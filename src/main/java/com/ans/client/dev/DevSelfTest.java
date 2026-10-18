package com.ans.client.dev;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.render.ReplacedMobRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.ans.AnsClientEvents;
import com.ans.AnsPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class DevSelfTest {
   private static String world = "anstest";
   private static List<String> mobs = List.of("minecraft:cow", "minecraft:pig");
   private static boolean opened;
   private static int ticks = -1;
   private static double ox;
   private static double oz;
   private static final double OY = 250.0;
   private static final String[] VARIANTS = new String[]{"minecraft:temperate", "minecraft:warm", "minecraft:cold"};
   private static final double G_SPECIAL = 40.0;
   private static final double G_WALK = 60.0;
   private static final double G_EVENTS = 80.0;
   private static final double G_RIDE = 100.0;

   private DevSelfTest() {
   }

   public static void init() {
      Path flag = AnsPlatform.gameDir().resolve("ans_selftest.txt");
      if (Files.isRegularFile(flag)) {
         try {
            List<String> lines = Files.readAllLines(flag).stream().map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#")).toList();
            if (!lines.isEmpty()) {
               world = lines.get(0);
            }

            if (lines.size() > 1) {
               mobs = lines.subList(1, lines.size()).stream().map(s -> (String)(s.contains(":") ? s : "minecraft:" + s)).toList();
            }
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] selftest: could not read {}: {}", flag, e.toString());
         }

         Ans.LOGGER.info("[ans] selftest enabled: world '{}', mobs {}", world, mobs);
         ReplacedMobRenderer.debug = true;
         AnsClientEvents.registerEndTick(DevSelfTest::tick);
      }
   }

   private static double groupX(int mobIndex) {
      return mobIndex * 20;
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         if (server != null) {
            ticks++;
            if (ticks == 20) {
               loadAllBundles(mc);
            }

            if (ticks == 30) {
               ox = Math.floor(mc.player.getX()) + 0.5;
               oz = Math.floor(mc.player.getZ()) + 0.5;
               if (!mc.gui.hud.isHidden()) {
                  mc.gui.hud.toggle();
               }

               run(
                  server,
                  "gamemode creative @a",
                  "time set noon",
                  "weather clear",
                  "gamerule advance_time false",
                  "gamerule spawn_mobs false",
                  "kill @e[type=!minecraft:player]",
                  fill(-12, 0, -8, 116, 0, 20, "minecraft:grass_block"),
                  fill(101, -2, 3, 108, 0, 9, "minecraft:stone"),
                  fill(102, -1, 4, 107, 0, 8, "minecraft:water"),
                  fill(93, 1, 8, 93, 1, 8, "minecraft:oak_fence")
               );
               camera(server, 0.0, 1.8, -1.0, 0.0F, 12.0F);
            }

            if (ticks == 50) {
               summonAll(server);
            }

            if (ticks == 56 || ticks == 90) {
               run(server, "kill @e[type=minecraft:item]");
            }

            if (ticks >= 60) {
               run(server, "execute as @e[tag=ans_walk] at @s run tp @s ~0.075 ~ ~", "execute as @e[tag=ans_run] at @s run tp @s ~0.3 ~ ~");
               if (ticks % 60 == 0) {
                  int lane = 0;

                  for (String mob : mobs) {
                     run(
                        server,
                        tpAbs("@e[tag=ans_walk,type=" + mob + "]", 55.0, 3.0 + lane * 1.8),
                        tpAbs("@e[tag=ans_run,type=" + mob + "]", 53.0, 7.0 + lane * 2.2)
                     );
                     lane++;
                  }
               }
            }

            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof LivingEntity le && e.hasCustomName() && "Look".equals(e.getCustomName().getString())) {
                  le.yHeadRot = le.yHeadRotO = le.getYRot() + 45.0F;
                  le.setXRot(25.0F);
                  le.xRotO = 25.0F;
               }
            }

            int t = ticks;
            int m = 0;
            int base = 100;

            for (String mob : mobs) {
               double gx = groupX(m++);
               String name = BuiltInRegistries.ENTITY_TYPE.getKey((EntityType)BuiltInRegistries.ENTITY_TYPE.getValue(new ResourceLocation(mob))).getPath();
               if (t == base) {
                  camera(server, gx, 2.4, -0.5, 0.0F, 14.0F);
               }

               if (t == base + 14) {
                  shot(mc, name + "_variants");
               }

               if (t == base + 16) {
                  camera(server, gx, 1.7, 0.6, 0.0F, 24.0F);
               }

               if (t == base + 30) {
                  shot(mc, name + "_babies_close");
               }

               if (t == base + 32) {
                  camera(server, gx - 5.5, 2.4, 1.8, -50.0F, 14.0F);
               }

               if (t == base + 46) {
                  shot(mc, name + "_adults_3q");
               }

               base += 50;
            }

            if (t == base) {
               camera(server, 40.0, 2.4, -0.5, 0.0F, 14.0F);
            }

            if (t == base + 14) {
               shot(mc, "specials");
            }

            if (t == base + 16) {
               camera(server, 37.0, 1.8, 3.0, 0.0F, 16.0F);
            }

            if (t == base + 30) {
               shot(mc, "specials_close_left");
            }

            if (t == base + 32) {
               camera(server, 43.0, 1.8, 3.0, 0.0F, 16.0F);
            }

            if (t == base + 46) {
               shot(mc, "specials_close_right");
            }

            if (t == base + 47) {
               camera(server, 50.5, 1.9, 0.2, 0.0F, 14.0F);
            }

            if (t == base + 60) {
               shot(mc, "head_look_vs_vanilla_sheep");
            }

            if (t == base + 62) {
               camera(server, 43.6, 1.8, 3.9, 0.0F, 20.0F);
            }

            if (t == base + 76) {
               shot(mc, "snowy_cow_close");
            }

            if (t == base + 78) {
               camera(server, 35.0, 1.8, 4.0, 0.0F, 22.0F);
            }

            if (t == base + 92) {
               shot(mc, "saddle_close");
            }

            base += 96;
            if (t == base) {
               camera(server, 60.0, 2.2, -0.5, 0.0F, 12.0F);
            }

            if (t == base + 14) {
               shot(mc, "walk_run_a");
            }

            if (t == base + 18) {
               shot(mc, "walk_run_b");
            }

            if (t == base + 22) {
               shot(mc, "walk_run_c");
            }

            base += 30;
            if (t == base) {
               camera(server, 80.0, 2.2, 0.0, 0.0F, 14.0F);
            }

            if (t == base + 10) {
               run(server, "execute as @e[tag=ans_hurt] run damage @s 1");
            }

            if (t == base + 14) {
               shot(mc, "hurt");
            }

            if (t == base + 17) {
               shot(mc, "hurt_b");
            }

            if (t == base + 20) {
               run(server, "kill @e[tag=ans_die]");
            }

            if (t == base + 30) {
               shot(mc, "dying");
            }

            if (t == base + 36) {
               run(
                  server,
                  summon("minecraft:cow", 78.0, 9.0, 7.0, 90.0F, "{Tags:[\"ans_test\"]}"),
                  summon("minecraft:pig", 82.0, 9.0, 7.0, 90.0F, "{Tags:[\"ans_test\"]}")
               );
            }

            if (t == base + 48) {
               shot(mc, "falling");
            }

            if (t == base + 51) {
               shot(mc, "falling_b");
            }

            if (t == base + 75) {
               shot(mc, "landed");
            }

            base += 80;
            if (t == base) {
               camera(server, 100.0, 2.4, 0.0, 0.0F, 16.0F);
            }

            if (t == base + 16) {
               shot(mc, "riding_leash_swim");
            }

            if (t == base + 18) {
               camera(server, 96.0, 1.9, 2.5, 0.0F, 16.0F);
            }

            if (t == base + 32) {
               shot(mc, "riding_close");
            }

            if (t == base + 34) {
               camera(server, 104.5, 1.9, 1.5, 0.0F, 22.0F);
            }

            if (t == base + 48) {
               shot(mc, "swimming_close");
            }

            base += 50;
            if (t == base) {
               stats(mc, "final");
               Ans.LOGGER.info("[ans] selftest: done, stopping client");
               mc.stop();
            }
         }
      } else {
         if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            if (mc.getLevelSource().levelExists(world)) {
               Ans.LOGGER.info("[ans] selftest: opening world '{}'", world);
               mc.createWorldOpenFlows().openWorld(world, () -> Ans.LOGGER.warn("[ans] selftest: world open cancelled"));
            } else {
               Ans.LOGGER.warn("[ans] selftest: world '{}' not found in saves; stopping at the title screen", world);
            }
         }
      }
   }

   private static void loadAllBundles(Minecraft mc) {
      Map<ResourceLocation, Resource> found = mc.getResourceManager()
         .listResources(
            "entity",
            idx -> idx.getNamespace().equals("ans")
               && idx.getPath().endsWith(".json")
               && !idx.getPath().substring(idx.getPath().lastIndexOf(47) + 1).startsWith("_")
         );
      int ok = 0;
      int failed = 0;
      int animations = 0;
      int controllers = 0;

      for (ResourceLocation id : found.keySet()) {
         String name = id.getPath().substring(id.getPath().lastIndexOf(47) + 1).replace(".json", "");
         EntityBundle b = EntityBundles.get(name, id);
         if (b == null) {
            failed++;
         } else {
            ok++;
            animations += b.animations.size();
            controllers += b.controllers.size();
         }
      }

      Ans.LOGGER.info("[ans] selftest: parsed {} entity bundles ({} failed): {} animations, {} controllers", new Object[]{ok, failed, animations, controllers});
   }

   private static void summonAll(IntegratedServer server) {
      List<String> cmds = new ArrayList<>();
      int m = 0;

      for (String mob : mobs) {
         double gx = groupX(m++);

         for (int v = 0; v < VARIANTS.length; v++) {
            double x = gx + (v - 1) * 3.2;
            cmds.add(summon(mob, x, 0.0, 7.0, 90.0F, "{NoAI:1b,variant:\"" + VARIANTS[v] + "\",Tags:[\"ans_test\"]}"));
            cmds.add(summon(mob, x, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,variant:\"" + VARIANTS[v] + "\",Tags:[\"ans_test\"]}"));
         }
      }

      int w = 0;

      for (String mob : mobs) {
         cmds.add(summon(mob, 55.0, 0.0, 3.0 + w * 1.8, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
         cmds.add(summon(mob, 53.0, 0.0, 7.0 + w * 2.2, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_run\"]}"));
         w++;
      }

      double x = 35.0;
      if (mobs.contains("minecraft:pig")) {
         cmds.add(summon("minecraft:pig", x, 0.0, 6.0, 90.0F, "{NoAI:1b,equipment:{saddle:{id:\"minecraft:saddle\",count:1}},Tags:[\"ans_test\"]}"));
         double var13;
         cmds.add(summon("minecraft:pig", var13 = x + 2.0, 0.0, 6.0, 90.0F, "{NoAI:1b,variant:\"minecraft:cold\",CustomName:\"Snowy\",Tags:[\"ans_test\"]}"));
         double var14;
         cmds.add(summon("minecraft:pig", var14 = var13 + 2.0, 0.0, 6.0, 90.0F, "{NoAI:1b,CustomName:\"Cherry\",Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:pig", x = var14 + 2.0, 0.0, 6.0, 90.0F, "{NoAI:1b,CustomName:\"Muddy\",Tags:[\"ans_test\"]}"));
      }

      if (mobs.contains("minecraft:cow")) {
         double var15;
         cmds.add(summon("minecraft:cow", var15 = x + 2.6, 0.0, 6.0, 90.0F, "{NoAI:1b,variant:\"minecraft:cold\",CustomName:\"Snowy\",Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:cow", x = var15 + 2.8, 0.0, 6.0, 90.0F, "{NoAI:1b,CustomName:\"Baby\",Tags:[\"ans_test\"]}"));
      }

      int e = 0;

      for (String mob : mobs) {
         cmds.add(summon(mob, 76.0 + e * 5, 0.0, 5.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_hurt\"]}"));
         cmds.add(summon(mob, 78.5 + e * 5, 0.0, 8.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_die\"]}"));
         e++;
      }

      cmds.add(summon("minecraft:cow", 49.0, 0.0, 3.2, 90.0F, "{NoAI:1b,CustomName:\"Look\",CustomNameVisible:0b,Tags:[\"ans_test\"]}"));
      cmds.add(summon("minecraft:sheep", 52.0, 0.0, 3.2, 90.0F, "{NoAI:1b,CustomName:\"Look\",CustomNameVisible:0b,Tags:[\"ans_test\"]}"));
      String pig = mobs.contains("minecraft:pig") ? "minecraft:pig" : mobs.get(0);
      String cow = mobs.contains("minecraft:cow") ? "minecraft:cow" : mobs.get(0);
      cmds.add(summon("minecraft:oak_boat", 96.0, 0.0, 6.0, 90.0F, "{Passengers:[{id:\"" + pig + "\",NoAI:1b,Tags:[\"ans_test\"]}]}"));
      cmds.add(summon("minecraft:minecart", 98.5, 0.0, 6.0, 90.0F, "{Passengers:[{id:\"" + cow + "\",NoAI:1b,Tags:[\"ans_test\"]}]}"));
      cmds.add(
         summon(
            cow,
            94.0,
            0.0,
            10.5,
            90.0F,
            String.format(Locale.ROOT, "{NoAI:1b,leash:[I;%d,%d,%d],Tags:[\"ans_test\"]}", (int)Math.floor(ox + 100.0 - 7.0), 251, (int)Math.floor(oz + 8.0))
         )
      );
      cmds.add(summon(cow, 103.5, -1.0, 6.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\"]}"));
      cmds.add(summon(pig, 106.0, -1.0, 6.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\"]}"));
      run(server, cmds.toArray(new String[0]));
   }

   private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
      int bx = (int)Math.floor(ox);
      int bz = (int)Math.floor(oz);
      return String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", bx + x1, 250 + y1, bz + z1, bx + x2, 250 + y2, bz + z2, block);
   }

   private static String summon(String mob, double x, double y, double z, float yaw, String nbt) {
      String data = nbt.replaceFirst("^\\{", "{Rotation:[" + yaw + "f,0f],");
      return String.format(Locale.ROOT, "summon %s %.3f %.3f %.3f %s", mob, ox + x, 251.0 + y, oz + z, data);
   }

   private static String tpAbs(String target, double x, double z) {
      return String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f -90 0", target, ox + x, 251.0, oz + z);
   }

   private static void camera(IntegratedServer server, double x, double y, double z, float yaw, float pitch) {
      run(server, String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.1f %.1f", ox + x, 251.0 + y - 1.62, oz + z, yaw, pitch));
   }

   private static void shot(Minecraft mc, String name) {
      stats(mc, name);
      Screenshot.grab(mc.gameDirectory, "ans_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, var0x -> {});
   }

   private static void stats(Minecraft mc, String when) {
      Ans.LOGGER.info("[ans] selftest ({}): {} animated entities", when, ReplacedMobRenderer.DEBUG_RUNTIMES.size());
      if (mc.level != null && mc.player != null) {
         Map<String, Entity> nearest = new LinkedHashMap<>();

         for (Entity e : mc.level.entitiesForRendering()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null && !(e.distanceTo(mc.player) > 12.0F)) {
               Ans.LOGGER
                  .info(
                     "[ans]   {} #{} {} at ({}, {}): {}",
                     new Object[]{
                        BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(),
                        e.getId(),
                        e.hasCustomName() ? "'" + e.getCustomName().getString() + "'" : "",
                        String.format(Locale.ROOT, "%.1f", e.getX() - ox),
                        String.format(Locale.ROOT, "%.1f", e.getZ() - oz),
                        rt.describe(false)
                     }
                  );
               nearest.putIfAbsent(e.getType().toString(), e);
            }
         }

         for (Entity e : nearest.values()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null) {
               Ans.LOGGER.info("[ans]   pose/vars #{}: {}", e.getId(), rt.describe(true) + " " + rt.lastPose());
            }
         }
      }
   }

   private static void run(IntegratedServer server, String... commands) {
      server.execute(() -> {
         for (String command : commands) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
         }
      });
   }
}
