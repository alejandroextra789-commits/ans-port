package com.ans.client.dev;

import com.ans.Ans;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.render.ReplacedMobRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import com.ans.AnsClientEvents;
import com.ans.AnsPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;

public final class CritterSelfTest {
   private static String world = "anstest";
   private static List<String> groups = List.of("sheep", "chicken", "rabbit", "goat", "llama");
   private static boolean opened;
   private static int ticks = -1;
   private static double ox;
   private static double oz;
   private static final double OY = 250.0;
   private static final Map<Integer, List<Runnable>> STEPS = new TreeMap<>();
   private static int end;

   private CritterSelfTest() {
   }

   public static void init() {
      Path flag = AnsPlatform.gameDir().resolve("ans_critter_selftest.txt");
      if (Files.isRegularFile(flag)) {
         try {
            List<String> lines = Files.readAllLines(flag).stream().map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#")).toList();
            if (!lines.isEmpty()) {
               world = lines.get(0);
            }

            if (lines.size() > 1) {
               groups = lines.subList(1, lines.size());
            }
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] critter selftest: could not read {}: {}", flag, e.toString());
         }

         Ans.LOGGER.info("[ans] critter selftest enabled: world '{}', groups {}", world, groups);
         ReplacedMobRenderer.debug = true;
         AnsClientEvents.registerEndTick(CritterSelfTest::tick);
      }
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         if (server != null) {
            ticks++;
            if (ticks == 30) {
               ox = Math.floor(mc.player.getX()) + 0.5;
               oz = Math.floor(mc.player.getZ()) + 0.5;
               if (!mc.gui.hud.isHidden()) {
                  mc.gui.hud.toggle();
               }

               plan(mc, server);
            }

            if (ticks >= 60) {
               run(server, "execute as @e[tag=ans_walk] at @s run tp @s ~0.075 ~ ~", "execute as @e[tag=ans_run] at @s run tp @s ~0.3 ~ ~");
               if (ticks % 60 == 0) {
                  run(server, "execute as @e[tag=ans_walk] at @s run tp @s ~-4.5 ~ ~", "execute as @e[tag=ans_run] at @s run tp @s ~-18 ~ ~");
               }
            }

            List<Runnable> steps = STEPS.get(ticks);
            if (steps != null) {
               steps.forEach(Runnable::run);
            }

            if (end > 0 && ticks == end) {
               Ans.LOGGER.info("[ans] critter selftest: done, stopping client");
               mc.stop();
            }
         }
      } else {
         if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            if (mc.getLevelSource().levelExists(world)) {
               mc.createWorldOpenFlows().openWorld(world, () -> Ans.LOGGER.warn("[ans] critter selftest: world open cancelled"));
            } else {
               Ans.LOGGER.warn("[ans] critter selftest: world '{}' not found", world);
            }
         }
      }
   }

   private static void at(int tick, Runnable r) {
      STEPS.computeIfAbsent(tick, var0x -> new ArrayList<>()).add(r);
      end = Math.max(end, tick + 10);
   }

   private static void plan(Minecraft mc, IntegratedServer server) {
      run(
         server,
         "gamemode creative @a",
         "time set noon",
         "weather clear",
         "gamerule advance_time false",
         "gamerule spawn_mobs false",
         "kill @e[type=!minecraft:player]",
         fill(-40, 0, -10, -15, 0, 24, "minecraft:grass_block"),
         fill(-14, 0, -10, 40, 0, 24, "minecraft:grass_block"),
         fill(41, 0, -10, 95, 0, 24, "minecraft:grass_block"),
         fill(96, 0, -10, 132, 0, 24, "minecraft:grass_block")
      );
      List<String> cmds = new ArrayList<>();
      int t = 60;
      if (groups.contains("sheep")) {
         double gx = 0.0;
         DyeColor[] colors = DyeColor.values();

         for (int i = 0; i < colors.length; i++) {
            double x = gx - 6.3 + i % 8 * 1.8;
            cmds.add(summon("minecraft:sheep", x, 0.0, i < 8 ? 9.0 : 5.0, 90.0F, "{NoAI:1b,Color:" + colors[i].getId() + "b,Tags:[\"ans_test\"]}"));
         }

         shotAt(t, gx, 3.6, -3.5, 0.0F, 22.0F, "sheep_colors");
         t += 20;
         shotAt(t, gx - 3.5, 1.9, 1.5, 0.0F, 16.0F, "sheep_colors_close");
         t += 20;
         gx = 24.0;
         double x = gx - 7.0;
         cmds.add(summon("minecraft:sheep", x, 0.0, 7.0, 90.0F, "{NoAI:1b,Sheared:1b,Tags:[\"ans_test\"]}"));
         double var40;
         cmds.add(summon("minecraft:sheep", var40 = x + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,Sheared:1b,Color:14b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:sheep", x = var40 + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,Sheared:1b,Color:15b,Tags:[\"ans_test\"]}"));
         double var42;
         cmds.add(summon("minecraft:sheep", var42 = x + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,CustomName:\"jeb_\",Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:sheep", x = var42 + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,CustomName:\"Snowy\",Tags:[\"ans_test\"]}"));
         double var44;
         cmds.add(summon("minecraft:sheep", var44 = x + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,CustomName:\"Lamb\",Color:6b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:sheep", x = var44 + 2.0, 0.0, 7.0, 90.0F, "{NoAI:1b,Color:11b,Tags:[\"ans_test\"]}"));
         x = gx - 5.0;
         cmds.add(summon("minecraft:sheep", x, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,Tags:[\"ans_test\"]}"));
         double var47;
         cmds.add(summon("minecraft:sheep", var47 = x + 2.0, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,Color:6b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:sheep", x = var47 + 2.0, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,Color:15b,Tags:[\"ans_test\"]}"));
         double var49;
         cmds.add(summon("minecraft:sheep", var49 = x + 2.0, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,CustomName:\"jeb_\",Tags:[\"ans_test\"]}"));
         shotAt(t, gx, 2.6, -2.0, 0.0F, 16.0F, "sheep_special");
         t += 20;
         shotAt(t, gx - 4.0, 1.7, 0.4, 0.0F, 18.0F, "sheep_special_close_left");
         t += 20;
         shotAt(t, gx + 3.0, 1.7, 0.4, 0.0F, 18.0F, "sheep_special_close_right");
         at(t + 4, () -> shot(mc, "sheep_jeb_later"));
         t += 24;
         double grazeX = 44.0;
         cmds.add(summon("minecraft:sheep", grazeX - 1.5, 0.0, 5.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_graze\"]}"));
         cmds.add(summon("minecraft:sheep", grazeX + 1.5, 0.0, 5.0, 0.0F, "{NoAI:1b,Color:4b,Tags:[\"ans_test\",\"ans_graze\"]}"));
         at(t, () -> camera(server, grazeX, 1.9, 0.5, 0.0F, 16.0F));
         at(t + 14, () -> server.execute(() -> {
            ServerLevel level = server.overworld();

            for (Entity e : level.getAllEntities()) {
               if (e.entityTags().contains("ans_graze")) {
                  level.broadcastEntityEvent(e, (byte)10);
               }
            }
         }));

         for (int s = 1; s <= 5; s++) {
            int n = s;
            at(t + 14 + s * 9, () -> shot(mc, "sheep_grazing_" + n));
         }

         t += 79;
         at(t - 4, () -> shot(mc, "sheep_grazing_after"));
      }

      if (groups.contains("chicken")) {
         double gx = 104.0;
         String[] variants = new String[]{"minecraft:temperate", "minecraft:warm", "minecraft:cold"};

         for (int v = 0; v < 3; v++) {
            cmds.add(summon("minecraft:chicken", gx + (v - 1) * 2.2, 0.0, 7.0, 90.0F, "{NoAI:1b,variant:\"" + variants[v] + "\",Tags:[\"ans_test\"]}"));
            cmds.add(
               summon("minecraft:chicken", gx + (v - 1) * 2.2, 0.0, 4.0, 90.0F, "{NoAI:1b,Age:-24000,variant:\"" + variants[v] + "\",Tags:[\"ans_test\"]}")
            );
         }

         cmds.add(summon("minecraft:chicken", gx + 5.0, 0.0, 7.0, 90.0F, "{NoAI:1b,CustomName:\"Chick\",Tags:[\"ans_test\"]}"));
         cmds.add(
            summon(
               "minecraft:chicken",
               gx - 5.0,
               0.0,
               7.0,
               90.0F,
               "{NoAI:1b,IsChickenJockey:1b,Passengers:[{id:\"minecraft:zombie\",IsBaby:1b,NoAI:1b,Tags:[\"ans_test\"]}],Tags:[\"ans_test\"]}"
            )
         );
         shotAt(t, gx, 2.2, -1.5, 0.0F, 16.0F, "chicken_variants");
         t += 20;
         shotAt(t, gx, 1.7, 1.2, 0.0F, 22.0F, "chicken_close");
         t += 20;
         shotAt(t, gx - 5.0, 1.8, 4.6, 0.0F, 20.0F, "chicken_jockey");
         t += 20;
      }

      if (groups.contains("rabbit")) {
         double gx = 124.0;
         String[] types = new String[]{"brown", "white", "black", "white_splotched", "gold", "salt"};

         for (int i = 0; i < types.length; i++) {
            cmds.add(summon("minecraft:rabbit", gx - 5.0 + i * 2, 0.0, 7.0, 90.0F, "{NoAI:1b,RabbitType:" + i + ",Tags:[\"ans_test\"]}"));
            cmds.add(summon("minecraft:rabbit", gx - 5.0 + i * 2, 0.0, 4.5, 90.0F, "{NoAI:1b,Age:-24000,RabbitType:" + i + ",Tags:[\"ans_test\"]}"));
         }

         String[] names = new String[]{"Toast", "Killer Bunny", "Punk", "Festive", "Cherry", "Bunny"};

         for (int i = 0; i < names.length; i++) {
            cmds.add(summon("minecraft:rabbit", gx - 5.0 + i * 2, 0.0, 10.0, 90.0F, "{NoAI:1b,CustomName:\"" + names[i] + "\",Tags:[\"ans_test\"]}"));
         }

         cmds.add(summon("minecraft:rabbit", gx + 7.0, 0.0, 7.0, 90.0F, "{NoAI:1b,RabbitType:99,Tags:[\"ans_test\"]}"));

         for (int i = 0; i < 3; i++) {
            cmds.add(summon("minecraft:rabbit", gx - 4.0 + i * 4, 0.0, 13.5, 90.0F, "{RabbitType:" + i + ",Tags:[\"ans_test\"]}"));
         }

         shotAt(t, gx, 2.4, -1.5, 0.0F, 18.0F, "rabbit_variants");
         t += 20;
         shotAt(t, gx - 2.0, 1.7, 2.2, 0.0F, 24.0F, "rabbit_close");
         t += 20;
         shotAt(t, gx, 1.8, 6.0, 0.0F, 18.0F, "rabbit_names");
         t += 20;
      }

      if (groups.contains("goat")) {
         double gx = -28.0;
         cmds.add(summon("minecraft:goat", gx - 4.5, 0.0, 7.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:goat", gx - 1.5, 0.0, 7.0, 90.0F, "{NoAI:1b,HasLeftHorn:0b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:goat", gx + 1.5, 0.0, 7.0, 90.0F, "{NoAI:1b,HasLeftHorn:0b,HasRightHorn:0b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:goat", gx + 4.5, 0.0, 7.0, 90.0F, "{NoAI:1b,IsScreamingGoat:1b,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:goat", gx - 2.0, 0.0, 3.5, 90.0F, "{NoAI:1b,Age:-24000,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:goat", gx + 2.0, 0.0, 3.5, 90.0F, "{NoAI:1b,CustomName:\"Kiddo\",Tags:[\"ans_test\"]}"));
         shotAt(t, gx, 2.6, -2.5, 0.0F, 16.0F, "goat_variants");
         t += 20;
         shotAt(t, gx - 3.0, 1.9, 2.5, 20.0F, 12.0F, "goat_close");
         t += 20;
      }

      if (groups.contains("llama")) {
         double gx = -28.0;

         for (int v = 0; v < 4; v++) {
            String carpet = v == 1
               ? ",equipment:{body:{id:\"minecraft:red_carpet\",count:1}}"
               : (v == 2 ? ",equipment:{body:{id:\"minecraft:light_blue_carpet\",count:1}}" : "");
            cmds.add(
               summon(
                  "minecraft:llama",
                  gx - 5.0 + v * 3.3,
                  0.0,
                  19.0,
                  90.0F,
                  "{NoAI:1b,Tame:1b,Variant:" + v + carpet + (v == 3 ? ",ChestedHorse:1b" : "") + ",Tags:[\"ans_test\"]}"
               )
            );
         }

         cmds.add(summon("minecraft:llama", gx - 2.0, 0.0, 15.5, 90.0F, "{NoAI:1b,Age:-24000,Variant:1,Tags:[\"ans_test\"]}"));
         cmds.add(summon("minecraft:llama", gx + 2.0, 0.0, 15.5, 90.0F, "{NoAI:1b,CustomName:\"Cria\",Variant:2,Tags:[\"ans_test\"]}"));
         shotAt(t, gx, 3.0, 9.0, 0.0F, 16.0F, "llama_variants");
         t += 20;
         shotAt(t, gx + 2.0, 2.2, 13.0, 30.0F, 12.0F, "llama_close");
         t += 20;
      }

      double wx = 64.0;
      int lane = 0;

      for (String mob : List.of("sheep", "chicken", "rabbit", "goat", "llama")) {
         if (groups.contains(mob)) {
            cmds.add(summon("minecraft:" + mob, wx - 3.0, 0.0, 3.0 + lane * 2.2, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
            cmds.add(summon("minecraft:" + mob, wx - 7.0, 0.0, 13.0 + lane * 2.5, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_run\"]}"));
            lane++;
         }
      }

      at(t, () -> camera(server, wx + 2.0, 3.2, -2.0, 0.0F, 22.0F));
      at(t + 14, () -> shot(mc, "walk_run_a"));
      at(t + 18, () -> shot(mc, "walk_run_b"));
      at(t + 22, () -> shot(mc, "walk_run_c"));
      t += 30;
      double ex = 84.0;
      lane = 0;

      for (String mob : List.of("sheep", "chicken", "rabbit", "goat", "llama")) {
         if (groups.contains(mob)) {
            cmds.add(summon("minecraft:" + mob, ex - 5.0 + lane * 2.5, 0.0, 5.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_hurt\"]}"));
            cmds.add(summon("minecraft:" + mob, ex - 5.0 + lane * 2.5, 0.0, 9.0, 90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_die\"]}"));
            lane++;
         }
      }

      at(t, () -> camera(server, ex - 2.0, 2.4, -1.5, 0.0F, 16.0F));
      at(t + 10, () -> run(server, "execute as @e[tag=ans_hurt] run damage @s 1"));
      at(t + 14, () -> shot(mc, "hurt"));
      at(t + 20, () -> run(server, "kill @e[tag=ans_die]"));
      at(t + 30, () -> shot(mc, "dying"));
      t += 40;
      List<String> fall = new ArrayList<>();
      lane = 0;

      for (String mob : List.of("sheep", "chicken", "rabbit", "goat", "llama")) {
         if (groups.contains(mob)) {
            fall.add(summon("minecraft:" + mob, ex - 5.0 + lane * 2.5, 9.0, 7.0, 90.0F, "{Tags:[\"ans_test\"]}"));
            lane++;
         }
      }

      at(t, () -> run(server, fall.toArray(new String[0])));
      at(t + 8, () -> shot(mc, "falling"));
      at(t + 14, () -> shot(mc, "falling_b"));
      at(t + 22, () -> shot(mc, "falling_c"));
      at(t + 60, () -> shot(mc, "landed"));
      t += 70;
      at(50, () -> run(server, cmds.toArray(new String[0])));
      at(56, () -> run(server, "kill @e[type=minecraft:item]"));
      at(t, () -> stats(mc, "final"));
   }

   private static void shotAt(int t, double x, double y, double z, float yaw, float pitch, String name) {
      Minecraft mc = Minecraft.getInstance();
      at(t, () -> camera(mc.getSingleplayerServer(), x, y, z, yaw, pitch));
      at(t + 14, () -> shot(mc, name));
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

   private static void camera(IntegratedServer server, double x, double y, double z, float yaw, float pitch) {
      run(server, String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.1f %.1f", ox + x, 251.0 + y - 1.62, oz + z, yaw, pitch));
   }

   private static void shot(Minecraft mc, String name) {
      stats(mc, name);
      Screenshot.grab(mc.gameDirectory, "ans_critter_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, var0x -> {});
   }

   private static void stats(Minecraft mc, String when) {
      if (mc.level != null && mc.player != null) {
         Ans.LOGGER.info("[ans] critter selftest ({})", when);

         for (Entity e : mc.level.entitiesForRendering()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null && !(e.distanceTo(mc.player) > 9.0F)) {
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
