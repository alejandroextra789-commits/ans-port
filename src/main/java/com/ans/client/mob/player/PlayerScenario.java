package com.ans.client.mob.player;

import com.ans.Ans;
import com.ans.client.attachable.AtlasSources;
import com.ans.client.attachable.AttachableRenderer;
import com.ans.client.attachable.Attachables;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangScope;
import com.ans.client.molang.MolangVars;
import com.ans.client.molang.PackSettings;
import com.ans.client.render.ReplacedMobRenderer;
import com.ans.client.settings.AnsSettings;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.ans.AnsClientEvents;
import com.ans.AnsPlatform;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.parrot.Parrot.Variant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class PlayerScenario {
   private static final double OY = 250.0;
   private static String world = "anstest";
   private static final List<PlayerScenario.Step> STEPS = new ArrayList<>();
   private static final Map<String, RemotePlayer> FAKES = new HashMap<>();
   private static boolean opened;
   private static int ticks = -1;
   private static double ox;
   private static double oz;

   private PlayerScenario() {
   }

   public static void init() {
      Path file = AnsPlatform.gameDir().resolve("ans_player_scenario.txt");
      if (Files.isRegularFile(file)) {
         try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
               String line = raw.strip();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  if (line.startsWith("world ")) {
                     world = line.substring(6).strip();
                  } else if (line.startsWith("@")) {
                     int space = line.indexOf(32);
                     String when = line.substring(1, space);
                     String action = line.substring(space + 1).strip();
                     int every = 1;
                     if (when.contains("/")) {
                        every = Integer.parseInt(when.substring(when.indexOf(47) + 1));
                        when = when.substring(0, when.indexOf(47));
                     }

                     int from;
                     int to;
                     if (when.contains("-")) {
                        from = Integer.parseInt(when.substring(0, when.indexOf(45)));
                        to = Integer.parseInt(when.substring(when.indexOf(45) + 1));
                     } else {
                        from = to = Integer.parseInt(when);
                     }

                     STEPS.add(new PlayerScenario.Step(from, to, Math.max(1, every), action));
                  }
               }
            }
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] player scenario: could not read {}: {}", file, e.toString());
            return;
         }

         Ans.LOGGER.info("[ans] player scenario enabled: world '{}', {} steps", world, STEPS.size());
         ReplacedMobRenderer.debug = true;
         AnsClientEvents.registerEndTick(PlayerScenario::tick);
      }
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         if (server != null) {
            ticks++;
            if (ticks == 20) {
               ox = Math.floor(mc.player.getX()) + 0.5;
               oz = Math.floor(mc.player.getZ()) + 0.5;
               if (!mc.gui.hud.isHidden()) {
                  mc.gui.hud.toggle();
               }
            }

            if (ticks >= 20) {
               for (PlayerScenario.Step s : STEPS) {
                  if (ticks >= s.from && ticks <= s.to && (ticks - s.from) % s.every == 0) {
                     try {
                        run(mc, server, s.action);
                     } catch (RuntimeException e) {
                        Ans.LOGGER.warn("[ans] player scenario: '{}' failed: {}", s.action, e.toString());
                     }
                  }
               }
            }
         }
      } else {
         if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            if (mc.getLevelSource().levelExists(world)) {
               mc.createWorldOpenFlows().openWorld(world, () -> {});
            } else {
               Ans.LOGGER.warn("[ans] player scenario: world '{}' not found", world);
            }
         }
      }
   }

   private static KeyMapping key(Minecraft mc, String name) {
      return switch (name) {
         case "forward" -> mc.options.keyUp;
         case "back" -> mc.options.keyDown;
         case "left" -> mc.options.keyLeft;
         case "right" -> mc.options.keyRight;
         case "jump" -> mc.options.keyJump;
         case "sneak" -> mc.options.keyShift;
         case "sprint" -> mc.options.keySprint;
         case "use" -> mc.options.keyUse;
         case "attack" -> mc.options.keyAttack;
         default -> throw new IllegalArgumentException("unknown key " + name);
      };
   }

   private static void run(Minecraft mc, IntegratedServer server, String action) {
      String[] p = action.split("\\s+");
      switch (p[0]) {
         case "shot":
            String name = p[1].replace("%t", String.format(Locale.ROOT, "%04d", ticks));
            log(mc, name);
            Screenshot.grab(mc.gameDirectory, "ans_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, var0x -> {});
            break;
         case "armtrace":
            armTrace(mc, p);
            break;
         case "log":
            log(mc, p.length > 1 ? p[1] : "log");
            break;
         case "quit":
            KeyMapping.releaseAll();
            Ans.LOGGER.info("[ans] player scenario: done, stopping client");
            mc.stop();
            break;
         case "view":
            Options var55 = mc.options;

            var55.setCameraType(switch (p[1]) {
               case "front" -> CameraType.THIRD_PERSON_FRONT;
               case "first" -> CameraType.FIRST_PERSON;
               default -> CameraType.THIRD_PERSON_BACK;
            });
            break;
         case "key":
            key(mc, p[1]).setDown(p[2].equals("on"));
            break;
         case "swing":
            mc.player.swing(p.length > 1 && p[1].equals("off") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            break;
         case "look":
            mc.player.setYRot(Float.parseFloat(p[1]));
            mc.player.setXRot(Float.parseFloat(p[2]));
            mc.player.setYHeadRot(Float.parseFloat(p[1]));
            break;
         case "pos":
            command(
               server,
               String.format(
                  Locale.ROOT,
                  "tp @p %.3f %.3f %.3f %s %s",
                  ox + Double.parseDouble(p[1]),
                  250.0 + Double.parseDouble(p[2]),
                  oz + Double.parseDouble(p[3]),
                  p[4],
                  p[5]
               )
            );
            break;
         case "fake":
            fake(mc, p);
            break;
         case "setting":
            AnsSettings.scenario(mc, p);
            break;
         case "fakepose":
            RemotePlayer f = FAKES.get(p[1]);

            Pose pose = switch (p[2]) {
               case "crouching" -> Pose.CROUCHING;
               case "swimming" -> Pose.SWIMMING;
               case "sleeping" -> Pose.SLEEPING;
               case "fall_flying" -> Pose.FALL_FLYING;
               default -> Pose.STANDING;
            };
            f.setShiftKeyDown(pose == Pose.CROUCHING);
            f.setPose(pose);
            break;
         case "fakeitem": {
            RemotePlayer f = FAKES.get(p[1]);
            EquipmentSlot slot = EquipmentSlot.byName(p[2]);
            f.setItemSlot(slot, new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(new ResourceLocation(p[3]))));
            break;
         }
         case "fakesleep": {
            RemotePlayer f = FAKES.get(p[1]);
            BlockPos bed = BlockPos.containing(ox + Double.parseDouble(p[2]), 250.0 + Double.parseDouble(p[3]), oz + Double.parseDouble(p[4]));
            f.setPos(bed.getX() + 0.5, bed.getY() + 0.6875, bed.getZ() + 0.5);
            f.setSleepingPos(bed);
            f.setPose(Pose.SLEEPING);
            break;
         }
         case "cape":
            PlayerRenderer.TEST_CAPES.put(p[1].equals("@self") ? mc.player.getGameProfile().getName() : p[1], new ResourceLocation(p[2]));
            break;
         case "fakeskin":
            String who = p[1].equals("@self") ? mc.player.getGameProfile().getName() : p[1];
            String key = "anstest_" + who.toLowerCase(Locale.ROOT) + "_" + Integer.toHexString(p[2].hashCode());
            new SkinTextureDownloader(Proxy.NO_PROXY, mc.getTextureManager(), mc)
               .downloadAndRegisterSkin(
                  new ResourceLocation("minecraft", "skins/" + key), mc.gameDirectory.toPath().resolve(p[2]), "http://ans.test/skin/" + key, true
               )
               .thenAccept(tex -> PlayerRendering.TEST_SKINS.put(who, tex));
            break;
         case "fakeparrot": {
            RemotePlayer f = FAKES.get(p[1]);
            Optional<Variant> v = Optional.of(Variant.byId(Integer.parseInt(p[3])));
            if (p[2].equals("left")) {
               f.setShoulderParrotLeft(v);
            } else {
               f.setShoulderParrotRight(v);
            }
            break;
         }
         case "hud":
            if (mc.gui.hud.isHidden() == p[1].equals("on")) {
               mc.gui.hud.toggle();
            }
            break;
         case "eval": {
            MolangContext c = new MolangContext(new MolangScope());
            c.entity = mc.player;
            c.actor = mc.player;
            c.partialTick = 1.0F;
            String src = action.substring(5).strip();
            Ans.LOGGER.info("[ans] player scenario eval {} = {}", src, Molang.compile(src).value(c));
            break;
         }
         case "mobeval": {
            LivingEntity mob = null;
            double best = Double.MAX_VALUE;

            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof LivingEntity l
                  && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(p[1])
                  && e.distanceToSqr(mc.player) < best) {
                  best = e.distanceToSqr(mc.player);
                  mob = l;
               }
            }

            if (mob == null) {
               return;
            }

            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(mob.getId());
            MolangContext c = new MolangContext(rt != null ? rt.variables() : new MolangScope());
            c.entity = mob;
            c.actor = mob;
            c.partialTick = 1.0F;
            String src = action.substring(action.indexOf(p[1]) + p[1].length()).strip();
            Ans.LOGGER.info("[ans] player scenario mobeval {} t={} {} = {}", new Object[]{p[1], ticks, src, Molang.compile(src).value(c)});
            break;
         }
         case "inventory":
            mc.setScreen(p.length > 1 && p[1].equals("off") ? null : new InventoryScreen(mc.player));
            break;
         case "fakeswing":
            FAKES.get(p[1]).swing(InteractionHand.MAIN_HAND);
            break;
         case "fakelook": {
            RemotePlayer f = FAKES.get(p[1]);
            f.setYHeadRot(Float.parseFloat(p[2]));
            f.setXRot(Float.parseFloat(p[3]));
            break;
         }
         case "fakeglide": {
            RemotePlayer f = FAKES.get(p[1]);

            try {
               Method m = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
               m.setAccessible(true);
               m.invoke(f, 7, p[2].equals("on"));
            } catch (ReflectiveOperationException e) {
               Ans.LOGGER.warn("[ans] player scenario: fakeglide: {}", e.toString());
            }

            f.setPose(p[2].equals("on") ? Pose.FALL_FLYING : Pose.STANDING);
            break;
         }
         case "fakefly": {
            RemotePlayer f = FAKES.get(p[1]);
            f.setPos(f.getX() + Double.parseDouble(p[2]), f.getY() + Double.parseDouble(p[3]), f.getZ() + Double.parseDouble(p[4]));
            f.setOnGround(false);
            break;
         }
         case "fakemove": {
            RemotePlayer f = FAKES.get(p[1]);
            f.setPos(f.getX() + Double.parseDouble(p[2]), f.getY(), f.getZ() + Double.parseDouble(p[3]));
            f.setOnGround(true);
            break;
         }
         case "vars":
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(mc.player.getId());
            if (rt == null) {
               return;
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 1; i < p.length; i++) {
               String n = p[i].replaceFirst("^(v|variable)\\.", "");
               Object vx = rt.variables().getOrNull(MolangVars.id(n));
               sb.append(' ').append(n).append('=').append(vx instanceof Double d ? String.format(Locale.ROOT, "%.3f", d) : vx);
            }

            Ans.LOGGER.info("[ans] player scenario vars:{}", sb);
            break;
         case "fplog":
            Ans.LOGGER.info("[ans] player scenario fp ({}): {}", p.length > 1 ? p[1] : "", FirstPerson.describe(false));
            break;
         case "fpvars":
            EntityRuntime rt = FirstPerson.runtime();
            if (rt == null) {
               return;
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 1; i < p.length; i++) {
               String n = p[i].replaceFirst("^(v|variable)\\.", "");
               Object vx = rt.variables().getOrNull(MolangVars.id(n));
               sb.append(' ').append(n).append('=').append(vx instanceof Double d ? String.format(Locale.ROOT, "%.3f", d) : vx);
            }

            Ans.LOGGER.info("[ans] player scenario fp vars:{}", sb);
            break;
         case "firstperson":
            FirstPerson.enabled = p[1].equals("on");
            break;
         case "javaitems":
            Attachables.DEV_JAVA_ITEMS.clear();
            if (!p[1].equals("off")) {
               Attachables.DEV_JAVA_ITEMS.addAll(List.of(p[1].split(",")));
            }
            break;
         case "relscale":
            AttachableRenderer.relativeDropsParentScale = p[1].equals("on");
            break;
         case "relall":
            AttachableRenderer.relativeAllLayers = p[1].equals("on");
            break;
         case "atlassrc":
            AtlasSources.enabled = p[1].equals("on");
            mc.reloadResourcePacks();
            break;
         case "packset":
            PackSettings.set(p[1], p[2]);
            break;
         case "pack":
            PackRepository repo = mc.getResourcePackRepository();
            if (p[2].equals("on")) {
               repo.addPack(p[1]);
            } else {
               repo.removePack(p[1]);
            }

            mc.options.updateResourcePacks(repo);
            break;
         case "hotbar":
            mc.player.getInventory().setSelectedSlot(Integer.parseInt(p[1]));
            break;
         case "vnstyle":
            try {
               Class<?> styles = Class.forName("com.villagernews.client.ClientSettings$VillagerStyle");
               Object style = styles.getMethod("byId", String.class).invoke(null, p[1]);
               Class.forName("com.villagernews.client.ClientSettings").getMethod("setVillagerStyle", styles).invoke(null, style);
            } catch (ReflectiveOperationException e) {
               Ans.LOGGER.warn("[ans] player scenario: vnstyle: {}", e.toString());
            }
            break;
         case "turn":
            mc.player.turn(Double.parseDouble(p[1]) / 0.15, p.length > 2 ? Double.parseDouble(p[2]) / 0.15 : 0.0);
            break;
         case "pause":
            mc.pauseGame(false);
            break;
         case "resume":
            mc.setScreen(null);
            break;
         case "fly":
            mc.player.getAbilities().flying = p[1].equals("on");
            mc.player.onUpdateAbilities();
            break;
         default:
            command(server, String.format(Locale.ROOT, "execute positioned %.3f %.3f %.3f run %s", ox, 250.0, oz, action));
      }
   }

   private static void fake(Minecraft mc, String[] p) {
      String name = p[1];
      long seed = p.length > 6 ? Long.parseLong(p[6]) : name.hashCode();
      UUID uuid = new UUID(seed * -7046029254386353131L, seed);
      RemotePlayer f = new RemotePlayer(mc.level, new GameProfile(uuid, name));
      f.setId(2000000 + FAKES.size());
      float yaw = Float.parseFloat(p[5]);
      f.setPos(ox + Double.parseDouble(p[2]), 250.0 + Double.parseDouble(p[3]), oz + Double.parseDouble(p[4]));
      f.setOldPosAndRot();
      f.setOnGround(true);
      f.setYRot(yaw);
      f.setYHeadRot(yaw);
      f.setYBodyRot(yaw);
      f.yRotO = yaw;
      f.yHeadRotO = yaw;
      f.yBodyRotO = yaw;

      try {
         Field acc = Avatar.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
         acc.setAccessible(true);
         EntityDataAccessor<Byte> a = (EntityDataAccessor<Byte>)acc.get(null);
         f.getEntityData().set(a, (byte)127);
      } catch (ReflectiveOperationException e) {
         Ans.LOGGER.warn("[ans] player scenario: fake model parts: {}", e.toString());
      }

      mc.level.addEntity(f);
      FAKES.put(name, f);
      Ans.LOGGER.info("[ans] player scenario: fake player {} ({} skin, uuid {})", new Object[]{name, f.getModelName(), uuid});
   }

   private static void armTrace(Minecraft mc, String[] p) {
      if (p[1].equals("off")) {
         EntityRuntime.sampleTrace = null;
      } else {
         Set<String> bones = Set.of(p[1].split(","));
         int frames = p.length > 2 ? Integer.parseInt(p[2]) : 20;
         boolean fp = p.length > 3 && p[3].equals("fp");
         Path out = AnsPlatform.gameDir().resolve("ans_armtrace.txt");
         StringBuilder sb = new StringBuilder();
         long[] state = new long[]{-1L, 0L};
         int tick = ticks;
         EntityRuntime.sampleTrace = (rt, anim, bone, w, before, after) -> {
            EntityRuntime target = fp ? FirstPerson.runtime() : (mc.player == null ? null : ReplacedMobRenderer.DEBUG_RUNTIMES.get(mc.player.getId()));
            if (rt == target && bones.contains(bone) && state[1] <= frames) {
               if (rt.frameCount() != state[0]) {
                  if (state[1] >= frames) {
                     state[1]++;
                     EntityRuntime.sampleTrace = null;

                     try {
                        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                     } catch (IOException e) {
                        Ans.LOGGER.warn("[ans] armtrace: {}", e.toString());
                     }

                     return;
                  }

                  state[0] = rt.frameCount();
                  state[1]++;
                  sb.append(String.format(Locale.ROOT, "== tick %d frame %d t=%.3f %s%n", tick, rt.frameCount(), rt.clockSeconds(), rt.describe(false)));
               }

               sb.append(
                  String.format(
                     Locale.ROOT,
                     "  %-10s %-8s w=%.3f r(%.1f,%.1f,%.1f)->(%.1f,%.1f,%.1f) p(%.2f,%.2f,%.2f)->(%.2f,%.2f,%.2f)%n",
                     bone,
                     anim,
                     w,
                     -Math.toDegrees(before[0]),
                     -Math.toDegrees(before[1]),
                     Math.toDegrees(before[2]),
                     -Math.toDegrees(after[0]),
                     -Math.toDegrees(after[1]),
                     Math.toDegrees(after[2]),
                     before[3],
                     before[4],
                     before[5],
                     after[3],
                     after[4],
                     after[5]
                  )
               );
            }
         };
      }
   }

   private static void command(IntegratedServer server, String command) {
      server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
   }

   private static void log(Minecraft mc, String when) {
      if (mc.level != null && mc.player != null) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof AbstractClientPlayer) {
               EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
               if (rt != null) {
                  Ans.LOGGER
                     .info(
                        "[ans] player scenario ({}) {} {} at ({}, {}, {}): {}",
                        new Object[]{
                           when,
                           e.getName().getString(),
                           e.getPose(),
                           String.format(Locale.ROOT, "%.1f", e.getX() - ox),
                           String.format(Locale.ROOT, "%.1f", e.getY() - 250.0),
                           String.format(Locale.ROOT, "%.1f", e.getZ() - oz),
                           rt.describe(false)
                        }
                     );
                  StringBuilder sb = new StringBuilder();

                  for (String b : new String[]{"root", "waist", "body", "head", "rightleg", "leftleg", "rightarm", "leftarm", "cape"}) {
                     float[] v = rt.lastPose().get(b);
                     if (v != null) {
                        sb.append(
                           String.format(
                              Locale.ROOT,
                              " %s=r(%.0f,%.0f,%.0f)p(%.1f,%.1f,%.1f)",
                              b,
                              Math.toDegrees(v[0]),
                              Math.toDegrees(v[1]),
                              Math.toDegrees(v[2]),
                              v[3],
                              v[4],
                              v[5]
                           )
                        );
                     }
                  }

                  Ans.LOGGER.info("[ans]   pose {}:{}", e.getName().getString(), sb);
               }
            }
         }
      }
   }

   private record Step(int from, int to, int every, String action) {
   }
}
