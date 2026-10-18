package com.ans.client.mob.monster;

import com.ans.Ans;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.render.ReplacedMobRenderer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.ans.AnsClientEvents;
import com.ans.AnsPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;

public final class MonsterSelfTest {
   private static String world = "anstest";
   private static boolean opened;
   private static int ticks = -1;
   private static double ox;
   private static double oz;
   private static final double OY = 250.0;
   private static final double S_OVERVIEW = 0.0;
   private static final double S_CREEPER = 20.0;
   private static final double S_SPIDER = 40.0;
   private static final double S_ENDERMAN = 60.0;
   private static final double S_GOLEM = 80.0;
   private static final double S_EVENTS = 100.0;
   private static int swellAt = -1;
   private static int unswellAt = -1;
   private static int swingAt = -1;
   private static int teleportAt = -1;
   private static int golemAttackAt = -1;
   private static int golemFlowerAt = -1;
   private static EntityDataAccessor<Boolean> creepy;

   private MonsterSelfTest() {
   }

   public static void init() {
      Path flag = AnsPlatform.gameDir().resolve("ans_selftest_monster.txt");
      if (Files.isRegularFile(flag)) {
         try {
            List<String> lines = Files.readAllLines(flag).stream().map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#")).toList();
            if (!lines.isEmpty()) {
               world = lines.get(0);
            }
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] monster selftest: could not read {}: {}", flag, e.toString());
         }

         Ans.LOGGER.info("[ans] monster selftest enabled: world '{}'", world);
         ReplacedMobRenderer.debug = true;
         AnsClientEvents.registerEndTick(MonsterSelfTest::tick);
      }
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         if (server != null) {
            ticks++;
            int t = ticks;
            if (t == 30) {
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
                  "gamerule mob_griefing false",
                  "kill @e[type=!minecraft:player]",
                  fill(-12, -1, -8, 116, 0, 20, "minecraft:grass_block"),
                  fill(-12, 1, -8, 116, 12, 20, "minecraft:air"),
                  fill(41, 1, 13, 47, 8, 13, "minecraft:stone")
               );
               camera(server, 0.0, 2.0, -6.0, 0.0F, 10.0F);
            }

            if (t == 50) {
               summonAll(server);
            }

            if (t == 56 || t == 90) {
               run(server, "kill @e[type=minecraft:item]");
            }

            int b = 100;
            if (t == b) {
               camera(server, 0.0, 2.2, -2.5, 0.0F, 10.0F);
            }

            if (t == b + 15) {
               shot(mc, "overview");
            }

            if (t == b + 17) {
               camera(server, -7.0, 2.4, 1.0, -50.0F, 12.0F);
            }

            if (t == b + 30) {
               shot(mc, "overview_3q");
            }

            b += 35;
            if (t == b) {
               camera(server, 20.0, 2.0, -2.2, 0.0F, 8.0F);
            }

            if (t == b + 12) {
               shot(mc, "creepers_idle");
            }

            if (t == b + 14) {
               swellAt = t;
            }

            if (t == b + 22) {
               shot(mc, "creepers_swell_a");
            }

            if (t == b + 36) {
               shot(mc, "creepers_swell_b");
            }

            if (t == b + 44) {
               swellAt = -1;
               unswellAt = t;
            }

            if (t == b + 66) {
               shot(mc, "creepers_unswell");
            }

            if (t == b + 68) {
               camera(server, 21.5, 1.7, 1.6, 0.0F, 8.0F);
            }

            if (t == b + 80) {
               shot(mc, "creepers_charged_close");
            }

            if (t == b + 82) {
               camera(server, 20.0, 1.8, 3.8, 0.0F, 8.0F);
            }

            if (t == b + 96) {
               shot(mc, "creepers_names");
            }

            b += 100;
            if (t == b) {
               camera(server, 40.0, 2.3, -1.5, 0.0F, 14.0F);
            }

            if (t == b + 14) {
               shot(mc, "spiders_a");
            }

            if (t == b + 18) {
               shot(mc, "spiders_b");
            }

            if (t == b + 20) {
               camera(server, 44.0, 2.4, 7.2, 0.0F, 8.0F);
            }

            if (t == b + 34) {
               shot(mc, "spider_climb");
            }

            if (t == b + 44) {
               shot(mc, "spider_climb_b");
            }

            if (t == b + 46) {
               camera(server, 37.5, 1.2, 2.8, 0.0F, 12.0F);
            }

            if (t == b + 60) {
               shot(mc, "spider_eyes_close");
            }

            b += 64;
            if (t == b) {
               camera(server, 60.0, 2.6, -3.0, 0.0F, 6.0F);
            }

            if (t == b + 14) {
               shot(mc, "endermen");
            }

            if (t == b + 16) {
               camera(server, 56.0, 3.0, 3.2, 0.0F, 12.0F);
            }

            if (t == b + 29) {
               shot(mc, "enderman_carry_close");
            }

            if (t == b + 30) {
               camera(server, 52.8, 1.3, 6.0, -90.0F, 0.0F);
            }

            if (t == b + 33) {
               shot(mc, "enderman_carry_side");
            }

            if (t == b + 34) {
               camera(server, 59.0, 3.0, 3.2, 0.0F, 12.0F);
            }

            if (t == b + 46) {
               shot(mc, "enderman_angry_close");
            }

            if (t == b + 48) {
               swingAt = t;
               camera(server, 62.0, 2.6, 1.5, 0.0F, 6.0F);
            }

            if (t == b + 52) {
               shot(mc, "enderman_attack_a");
            }

            if (t == b + 56) {
               shot(mc, "enderman_attack_b");
            }

            if (t == b + 60) {
               teleportAt = t;
               camera(server, 66.0, 2.6, 0.5, 0.0F, 6.0F);
            }

            if (t == b + 62) {
               shot(mc, "enderman_teleport_a");
            }

            if (t == b + 65) {
               shot(mc, "enderman_teleport_b");
            }

            if (t == b + 72) {
               shot(mc, "enderman_teleport_c");
            }

            b += 76;
            if (t == b) {
               camera(server, 80.0, 2.6, -3.5, 0.0F, 8.0F);
            }

            if (t == b + 14) {
               shot(mc, "golems_cracks");
            }

            if (t == b + 16) {
               golemAttackAt = t;
               golemFlowerAt = t;
               camera(server, 79.0, 2.6, 4.2, 0.0F, 8.0F);
            }

            if (t == b + 19) {
               shot(mc, "golem_attack_a");
            }

            if (t == b + 23) {
               shot(mc, "golem_attack_b");
            }

            if (t == b + 50) {
               shot(mc, "golem_flower");
            }

            if (t == b + 52) {
               camera(server, 85.0, 2.4, 6.0, 0.0F, 8.0F);
            }

            if (t == b + 66) {
               shot(mc, "golem_walk");
            }

            b += 70;
            if (t == b) {
               camera(server, 100.0, 2.2, -1.5, 0.0F, 10.0F);
            }

            if (t == b + 10) {
               run(server, "execute as @e[tag=ans_hurt] run damage @s 1");
            }

            if (t == b + 13) {
               shot(mc, "hurt");
            }

            if (t == b + 20) {
               run(server, "kill @e[tag=ans_die]");
            }

            if (t == b + 28) {
               shot(mc, "dying_a");
            }

            if (t == b + 29) {
               camera(server, 98.5, 1.6, 5.8, 0.0F, 25.0F);
            }

            if (t == b + 34) {
               shot(mc, "dying_spiders_close");
            }

            if (t == b + 42) {
               shot(mc, "dying_b");
            }

            if (t == b + 44) {
               camera(server, 110.0, 2.2, 0.0, 0.0F, 10.0F);
               run(server, "data merge entity @e[tag=ans_boom,limit=1] {ignited:1b}");
            }

            if (t == b + 58) {
               shot(mc, "explosion_swell");
            }

            if (t == b + 76) {
               shot(mc, "explosion_after");
            }

            b += 80;
            if (t == b) {
               run(server, "time set midnight");
               camera(server, 0.0, 2.2, -2.5, 0.0F, 10.0F);
            }

            if (t == b + 20) {
               shot(mc, "overview_night");
            }

            if (t == b + 22) {
               camera(server, 37.5, 1.2, 2.8, 0.0F, 12.0F);
            }

            if (t == b + 36) {
               shot(mc, "spider_eyes_night");
            }

            b += 40;
            if (t >= 60) {
               harness(mc, server, t);
            }

            if (t == b) {
               stats(mc, "final");
               Ans.LOGGER.info("[ans] monster selftest: done, stopping client");
               mc.stop();
            }
         }
      } else {
         if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            if (mc.getLevelSource().levelExists(world)) {
               mc.createWorldOpenFlows().openWorld(world, () -> Ans.LOGGER.warn("[ans] monster selftest: world open cancelled"));
            } else {
               Ans.LOGGER.warn("[ans] monster selftest: world '{}' not found", world);
            }
         }
      }
   }

   private static void harness(Minecraft mc, IntegratedServer server, int t) {
      run(server, "execute as @e[tag=ans_walk] at @s run tp @s ~0.08 ~ ~");
      if (t % 80 == 0) {
         run(server, "execute as @e[tag=ans_walk] at @s run tp @s ~-6.4 ~ ~");
      }

      double climbY = t % 100 * 0.05;
      run(server, String.format(Locale.ROOT, "tp @e[tag=ans_climb] %.3f %.3f %.3f 0 0", ox + 40.0 + 4.0, 251.0 + climbY, oz + 11.75));
      if (teleportAt > 0 && t == teleportAt) {
         run(server, "execute as @e[tag=ans_tele] at @s run tp @s ~4 ~ ~");
      }

      for (Entity e : mc.level.entitiesForRendering()) {
         Set<String> tags = tags(server, e);
         if (!tags.isEmpty()) {
            if (e instanceof Spider s && tags.contains("ans_climb")) {
               s.setClimbing(true);
            }

            if (e instanceof Creeper c && tags.contains("ans_swell")) {
               if (swellAt > 0) {
                  c.setSwellDir(1);
               } else if (unswellAt > 0) {
                  c.setSwellDir(-1);
               }
            }

            if (e instanceof EnderMan m) {
               if (tags.contains("ans_angry")) {
                  setCreepy(m, true);
               }

               if (swingAt > 0 && t == swingAt && tags.contains("ans_attack")) {
                  m.swing(InteractionHand.MAIN_HAND);
               }
            }

            if (e instanceof IronGolem g) {
               if (golemAttackAt > 0 && t == golemAttackAt && tags.contains("ans_attack")) {
                  g.handleEntityEvent((byte)4);
               }

               if (golemFlowerAt > 0 && t == golemFlowerAt && tags.contains("ans_flower")) {
                  g.handleEntityEvent((byte)11);
               }
            }
         }
      }
   }

   private static Set<String> tags(IntegratedServer server, Entity e) {
      if (server == null) {
         return Set.of();
      }

      Entity twin = server.overworld().getEntity(e.getId());
      return twin == null ? Set.of() : Set.copyOf(twin.entityTags());
   }

   private static void setCreepy(EnderMan m, boolean value) {
      try {
         if (creepy == null) {
            Field f = EnderMan.class.getDeclaredField("DATA_CREEPY");
            f.setAccessible(true);
            creepy = (EntityDataAccessor<Boolean>)f.get(null);
         }

         m.getEntityData().set(creepy, value);
      } catch (ReflectiveOperationException | RuntimeException e) {
         Ans.LOGGER.warn("[ans] monster selftest: cannot set enderman anger: {}", e.toString());
      }
   }

   private static void summonAll(IntegratedServer server) {
      List<String> c = new ArrayList<>();
      String noAi = "NoAI:1b,Tags:[\"ans_test\"]";
      c.add(summon("creeper", -6.0, 6.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("spider", -3.0, 6.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("cave_spider", -0.3, 6.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("enderman", 2.5, 6.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("iron_golem", 6.0, 6.0, 180.0F, "{" + noAi + "}"));
      double x = 16.7;
      c.add(summon("creeper", x, 4.0, 180.0F, "{" + noAi + "}"));
      double var12;
      c.add(summon("creeper", var12 = x + 2.2, 4.0, 180.0F, "{powered:1b," + noAi + "}"));
      c.add(summon("creeper", x = var12 + 2.2, 4.0, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_swell\"]}"));
      double var14;
      c.add(summon("creeper", var14 = x + 2.2, 4.0, 180.0F, "{powered:1b,NoAI:1b,Tags:[\"ans_test\",\"ans_swell\"]}"));
      x = 14.4;

      for (String n : new String[]{"Pig", "Halloween", "Snowy", "Cherry", "Blossom", "Baby"}) {
         c.add(summon("creeper", x, 8.5, 180.0F, "{CustomName:\"" + n + "\"," + noAi + "}"));
         x += 2.2;
      }

      c.add(summon("spider", 37.0, 5.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("cave_spider", 39.2, 5.0, 180.0F, "{" + noAi + "}"));
      c.add(summon("spider", 41.6, 4.5, 180.0F, "{CustomName:\"Snowy\"," + noAi + "}"));
      c.add(summon("spider", 44.2, 4.5, 180.0F, "{CustomName:\"Cherry\"," + noAi + "}"));
      c.add(summon("spider", 46.5, 4.5, 180.0F, "{CustomName:\"Baby\"," + noAi + "}"));
      c.add(summon("spider", 36.0, 8.0, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
      c.add(summon("cave_spider", 36.0, 10.0, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
      c.add(summon("spider", 44.0, 11.75, 0.0F, "{NoAI:1b,NoGravity:1b,Tags:[\"ans_test\",\"ans_climb\"]}"));
      String grass = "carriedBlockState:{Name:\"minecraft:grass_block\"}";
      c.add(summon("enderman", 56.0, 6.0, 180.0F, "{" + grass + "," + noAi + "}"));
      c.add(summon("enderman", 59.0, 6.0, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_angry\"]}"));
      c.add(summon("enderman", 62.0, 5.0, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_attack\"]}"));
      c.add(summon("enderman", 64.5, 6.0, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_tele\"]}"));
      c.add(summon("enderman", 57.0, 10.0, -90.0F, "{carriedBlockState:{Name:\"minecraft:tnt\"},NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
      double gx = 74.6;

      for (float h : new float[]{100.0F, 70.0F, 45.0F, 20.0F}) {
         c.add(summon("iron_golem", gx, 6.0, 180.0F, String.format(Locale.ROOT, "{Health:%.0ff,%s}", h, noAi)));
         gx += 3.4;
      }

      c.add(summon("iron_golem", 77.5, 9.5, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_attack\"]}"));
      c.add(summon("iron_golem", 81.0, 9.5, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_flower\"]}"));
      c.add(summon("iron_golem", 82.0, 13.0, -90.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_walk\"]}"));
      String[] types = new String[]{"creeper", "spider", "cave_spider", "enderman", "iron_golem"};

      for (int i = 0; i < types.length; i++) {
         c.add(summon(types[i], 94.0 + i * 3, 4.5, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_hurt\"]}"));
         c.add(summon(types[i], 94.0 + i * 3, 8.5, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_die\"]}"));
      }

      c.add(summon("creeper", 110.0, 6.0, 180.0F, "{NoAI:1b,Tags:[\"ans_test\",\"ans_boom\"]}"));
      run(server, c.toArray(new String[0]));
   }

   private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
      int bx = (int)Math.floor(ox);
      int bz = (int)Math.floor(oz);
      return String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", bx + x1, 250 + y1, bz + z1, bx + x2, 250 + y2, bz + z2, block);
   }

   private static String summon(String mob, double x, double z, float yaw, String nbt) {
      String data = nbt.replaceFirst("^\\{", "{Rotation:[" + yaw + "f,0f],");
      return String.format(Locale.ROOT, "summon minecraft:%s %.3f %.3f %.3f %s", mob, ox + x, 251.0, oz + z, data);
   }

   private static void camera(IntegratedServer server, double x, double y, double z, float yaw, float pitch) {
      run(server, String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.1f %.1f", ox + x, 251.0 + y - 1.62, oz + z, yaw, pitch));
   }

   private static void shot(Minecraft mc, String name) {
      stats(mc, name);
      Screenshot.grab(mc.gameDirectory, "ans_monster_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, var0x -> {});
   }

   private static void stats(Minecraft mc, String when) {
      Ans.LOGGER.info("[ans] monster selftest ({}): {} animated entities", when, ReplacedMobRenderer.DEBUG_RUNTIMES.size());
      if (mc.level != null && mc.player != null) {
         Map<String, Entity> nearest = new LinkedHashMap<>();

         for (Entity e : mc.level.entitiesForRendering()) {
            EntityRuntime rt = ReplacedMobRenderer.DEBUG_RUNTIMES.get(e.getId());
            if (rt != null && !(e.distanceTo(mc.player) > 10.0F)) {
               Ans.LOGGER
                  .info(
                     "[ans]   {} #{} {}{} at ({}, {}, {}): {}",
                     new Object[]{
                        BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(),
                        e.getId(),
                        e.hasCustomName() ? "'" + e.getCustomName().getString() + "' " : "",
                        tags(mc.getSingleplayerServer(), e),
                        String.format(Locale.ROOT, "%.1f", e.getX() - ox),
                        String.format(Locale.ROOT, "%.1f", e.getY() - 250.0 - 1.0),
                        String.format(Locale.ROOT, "%.1f", e.getZ() - oz),
                        rt.describe(false)
                     }
                  );
               nearest.putIfAbsent(e.getType().toString() + tags(mc.getSingleplayerServer(), e), e);
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
