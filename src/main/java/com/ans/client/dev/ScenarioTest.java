package com.ans.client.dev;

import com.ans.Ans;
import com.ans.client.bedrock.BedrockRules;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.render.ReplacedMobRenderer;
import com.ans.client.settings.AnsSettings;
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
import net.minecraft.world.entity.Entity;

public final class ScenarioTest {
   private static final double OY = 250.0;
   private static String world = "anstest";
   private static final List<ScenarioTest.Step> STEPS = new ArrayList<>();
   private static boolean opened;
   private static int ticks = -1;
   private static double ox;
   private static double oz;

   private ScenarioTest() {
   }

   public static void init() {
      Path file = AnsPlatform.gameDir().resolve("ans_scenario.txt");
      if (Files.isRegularFile(file)) {
         try {
            for (String raw : Files.readAllLines(file)) {
               String line = raw.strip();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  if (line.startsWith("world ")) {
                     world = line.substring(6).strip();
                  } else if (!line.startsWith("@")) {
                     Ans.LOGGER.warn("[ans] scenario: ignoring line '{}'", line);
                  } else {
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

                     STEPS.add(new ScenarioTest.Step(from, to, Math.max(1, every), action));
                  }
               }
            }
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] scenario: could not read {}: {}", file, e.toString());
            return;
         }

         Ans.LOGGER.info("[ans] scenario enabled: world '{}', {} steps", world, STEPS.size());
         ReplacedMobRenderer.debug = true;
         BedrockRules.logNegativeWeights = true;
         AnsClientEvents.registerEndTick(ScenarioTest::tick);
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
               for (ScenarioTest.Step s : STEPS) {
                  if (ticks >= s.from && ticks <= s.to && (ticks - s.from) % s.every == 0) {
                     run(mc, server, s.action);
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
               Ans.LOGGER.warn("[ans] scenario: world '{}' not found", world);
            }
         }
      }
   }

   private static void run(Minecraft mc, IntegratedServer server, String action) {
      String[] p = action.split("\\s+");
      switch (p[0]) {
         case "shot":
            log(mc, p[1]);
            Screenshot.grab(mc.gameDirectory, "ans_" + p[1] + ".png", mc.gameRenderer.mainRenderTarget(), 1, var0x -> {});
            break;
         case "log":
            log(mc, p.length > 1 ? p[1] : "log");
            break;
         case "origin":
            ox = ox + Double.parseDouble(p[1]);
            oz = oz + Double.parseDouble(p[2]);
            break;
         case "rules":
            BedrockRules.setLegacy("legacy".equals(p[1]));
            break;
         case "setting":
            AnsSettings.scenario(mc, p);
            break;
         case "quit":
            Ans.LOGGER.info("[ans] scenario: done, stopping client");
            mc.stop();
            break;
         case "camera":
            command(
               server,
               String.format(
                  Locale.ROOT,
                  "tp @p %.3f %.3f %.3f %s %s",
                  ox + Double.parseDouble(p[1]),
                  250.0 + Double.parseDouble(p[2]) - 1.62,
                  oz + Double.parseDouble(p[3]),
                  p[4],
                  p[5]
               )
            );
            break;
         default:
            command(server, String.format(Locale.ROOT, "execute positioned %.3f %.3f %.3f run %s", ox, 250.0, oz, action));
      }
   }

   private static void command(IntegratedServer server, String command) {
      server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
   }

   private static void log(Minecraft mc, String when) {
      if (mc.level != null && mc.player != null) {
         Map<String, Entity> first = new LinkedHashMap<>();

         for (Entity e : mc.level.entitiesForRendering()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null && !(e.distanceTo(mc.player) > 12.0F)) {
               Ans.LOGGER
                  .info(
                     "[ans] scenario ({}) {} #{} {} at ({}, {}): {}",
                     new Object[]{
                        when,
                        BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(),
                        e.getId(),
                        e.hasCustomName() ? "'" + e.getCustomName().getString() + "'" : "",
                        String.format(Locale.ROOT, "%.1f", e.getX() - ox),
                        String.format(Locale.ROOT, "%.1f", e.getZ() - oz),
                        rt.describe(false)
                     }
                  );
               first.putIfAbsent(e.getType().toString(), e);
            }
         }

         for (Entity e : first.values()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null) {
               Ans.LOGGER.info("[ans]   pose/vars #{}: {}", e.getId(), rt.describe(true) + " " + rt.lastPose());
            }
         }
      }
   }

   private record Step(int from, int to, int every, String action) {
   }
}
