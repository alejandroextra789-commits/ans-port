package com.ans.client.molang;

import com.ans.client.bedrock.BedrockRules;
import com.ans.client.bedrock.BedrockWalk;
import com.ans.client.equipment.EquipmentQueries;
import com.ans.client.mob.player.ExpressionSkins;
import com.geckolib.util.ClientUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

public final class BedrockQueries {
   public static String graphicsMode = "fancy";
   private static final boolean BEDROCK_IN_WATER = !"false".equalsIgnoreCase(System.getProperty("ans.bedrockInWater", "true"));
   private static final boolean CAMERA_FROM_POSITION = !"false".equalsIgnoreCase(System.getProperty("ans.cameraFromPosition", "true"));
   private static final boolean FP_TARGET_YAW_JAVA = "java".equalsIgnoreCase(System.getProperty("ans.fpTargetYaw"));
   private static final Map<String, String> BIOMES = new HashMap<>();
   private static final Map<String, Predicate<Holder<Biome>>> BIOME_TAGS = new HashMap<>();

   private BedrockQueries() {
   }

   public static void install() {
      time();
      controller();
      movement();
      rotation();
      state();
      life();
      names();
      world();
      items();
      context();
      helpers();
      EngineQueries.install();
   }

   private static double lifeTime(MolangContext c) {
      return c.actor == null ? 0.0 : (c.actor.tickCount + c.partialTick) / 20.0;
   }

   public static double bedrockPositionOffset(Entity e) {
      return e instanceof Player ? 1.62 : 0.0;
   }

   private static Vec3 cameraOrigin(Entity e, float partialTick, boolean rotation) {
      Vec3 p = e.getPosition(partialTick);
      if (CAMERA_FROM_POSITION) {
         return p.add(0.0, bedrockPositionOffset(e), 0.0);
      } else {
         return rotation ? p.add(0.0, e.getEyeHeight(), 0.0) : p;
      }
   }

   private static double axis(Vec3 v, double axis) {
      int a = (int)axis;
      return a == 0 ? v.x : (a == 1 ? v.y : v.z);
   }

   private static double arg(Node[] a, int i, MolangContext c) {
      return i < a.length ? a[i].eval(c) : 0.0;
   }

   private static Vec3 cameraPos() {
      Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
      return camera.position();
   }

   private static double horizontalSpeed(Entity e) {
      double dx = e.getX() - e.xo;
      double dz = e.getZ() - e.zo;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static double bodyYaw(LivingEntity e, float pt) {
      return Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
   }

   private static double headYawRelative(LivingEntity e, float pt) {
      return Mth.wrapDegrees(Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot) - Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot));
   }

   private static boolean firstPersonHeadFrame(MolangContext c) {
      return !FP_TARGET_YAW_JAVA && (c.firstPerson || c.attachable != null && c.attachable.firstPerson()) && !c.entity.isPassenger();
   }

   private static void time() {
      MolangQueries.simple("anim_time", c -> c.animTime);
      MolangQueries.simple("delta_time", c -> c.deltaTime);
      MolangQueries.simple("life_time", BedrockQueries::lifeTime);
      MolangQueries.simple("frame_alpha", c -> c.partialTick);
      MolangQueries.simple("time_stamp", c -> c.actor == null ? 0.0 : c.actor.level().getGameTime());
      MolangQueries.simple("day", c -> c.actor == null ? 0.0 : c.actor.level().getGameTime() / 24000.0);
      MolangQueries.simple("time_of_day", c -> c.actor == null ? 0.0 : (c.actor.level().getDefaultClockTime() % 24000L / 24000.0 + 0.25) % 1.0);
      MolangQueries.simple("moon_phase", var0 -> ClientUtil.getClientMoonPhase().index());
   }

   private static void controller() {
      MolangQueries.simple("state_time", c -> c.controller == null ? 0.0 : c.controller.stateTime());
      MolangQueries.simple("any_animation_finished", c -> c.controller != null && c.controller.anyAnimationFinished() ? 1.0 : 0.0);
      MolangQueries.simple("all_animations_finished", c -> c.controller != null && c.controller.allAnimationsFinished() ? 1.0 : 0.0);
   }

   private static void movement() {
      MolangQueries.simple("modified_move_speed", c -> c.entity == null ? 0.0 : BedrockWalk.of(c.entity).speed(c.partialTick));
      MolangQueries.simple("modified_distance_moved", c -> c.entity == null ? 0.0 : BedrockWalk.of(c.entity).position(c.partialTick));
      MolangQueries.simple("walk_distance", c -> c.entity == null ? 0.0 : BedrockWalk.of(c.entity).position(c.partialTick));
      MolangQueries.actor("ground_speed", e -> horizontalSpeed(e) * 20.0);
      MolangQueries.actor("vertical_speed", e -> (e.getY() - e.yo) * 20.0);
      MolangQueries.actor("yaw_speed", e -> Mth.wrapDegrees(e.getYRot() - e.yRotO));
      MolangQueries.actorFlag("is_moving", e -> horizontalSpeed(e) > 0.0025 || Math.abs(e.getY() - e.yo) > 0.0025);
      MolangQueries.actorFlag("is_on_ground", Entity::onGround);
      MolangQueries.actorFlag("is_in_water", e -> e.isInWater() && (!BEDROCK_IN_WATER || e.getFluidHeight(FluidTags.WATER) >= 0.4));
      MolangQueries.actorFlag("is_in_water_or_rain", Entity::isInWaterOrRain);
      MolangQueries.actorFlag("is_in_contact_with_water", Entity::isInWaterOrRain);
      MolangQueries.actorFlag("is_in_lava", Entity::isInLava);
      MolangQueries.flag("is_swimming", e -> e.isSwimming() || e.isVisuallySwimming());
      MolangQueries.flag("is_sneaking", Entity::isCrouching);
      MolangQueries.flag("is_sprinting", Entity::isSprinting);
      MolangQueries.actorFlag("is_riding", Entity::isPassenger);
      MolangQueries.actorFlag("has_rider", Entity::isVehicle);
      MolangQueries.actorFlag("has_player_rider", e -> e.hasPassenger(p -> p instanceof Player));
      MolangQueries.flag("is_gliding", LivingEntity::isFallFlying);
      MolangQueries.flag("is_wall_climbing", LivingEntity::onClimbable);
      MolangQueries.flag("is_jumping", e -> !e.onGround() && e.getY() - e.yo > 0.01 && !e.isInWater());
      MolangQueries.flag("is_crawling", e -> e.getPose() == Pose.SWIMMING && !e.isInWater());
      MolangQueries.register(
         "position", a -> c -> c.actor == null ? 0.0 : axis(c.actor.getPosition(c.partialTick).add(0.0, bedrockPositionOffset(c.actor), 0.0), arg(a, 0, c))
      );
      MolangQueries.register(
         "position_delta",
         a -> c -> c.actor == null ? 0.0 : axis(new Vec3(c.actor.getX() - c.actor.xo, c.actor.getY() - c.actor.yo, c.actor.getZ() - c.actor.zo), arg(a, 0, c))
      );
      MolangQueries.register("movement_direction", a -> c -> {
         if (c.actor == null) {
            return 0.0;
         }

         Vec3 d = new Vec3(c.actor.getX() - c.actor.xo, c.actor.getY() - c.actor.yo, c.actor.getZ() - c.actor.zo);
         return d.lengthSqr() < 1.0E-8 ? 0.0 : axis(d.normalize(), arg(a, 0, c));
      });
      MolangQueries.simple("ride_body_y_rotation", c -> c.entity != null && c.entity.getVehicle() instanceof LivingEntity v ? bodyYaw(v, c.partialTick) : 0.0);
   }

   private static void rotation() {
      MolangQueries.simple(
         "body_y_rotation", c -> c.entity != null ? bodyYaw(c.entity, c.partialTick) : (c.actor == null ? 0.0 : EntityRotation.yaw(c.actor, c.partialTick))
      );
      MolangQueries.simple("body_x_rotation", var0 -> 0.0);
      MolangQueries.simple("target_x_rotation", c -> c.actor == null ? 0.0 : EntityRotation.pitch(c.actor, c.partialTick));
      MolangQueries.simple(
         "target_y_rotation",
         c -> c.entity != null
            ? (firstPersonHeadFrame(c) ? 0.0 : headYawRelative(c.entity, c.partialTick))
            : (c.actor == null ? 0.0 : EntityRotation.yaw(c.actor, c.partialTick))
      );
      MolangQueries.register("head_x_rotation", var0 -> c -> c.actor == null ? 0.0 : EntityRotation.pitch(c.actor, c.partialTick));
      MolangQueries.register(
         "head_y_rotation",
         var0 -> c -> c.entity != null
            ? (
               BedrockRules.absoluteHeadYaw()
                  ? Mth.wrapDegrees(Mth.rotLerp(c.partialTick, c.entity.yHeadRotO, c.entity.yHeadRot))
                  : headYawRelative(c.entity, c.partialTick)
            )
            : (c.actor == null ? 0.0 : EntityRotation.yaw(c.actor, c.partialTick))
      );
      MolangQueries.simple("distance_from_camera", c -> c.actor == null ? 0.0 : cameraPos().distanceTo(cameraOrigin(c.actor, c.partialTick, false)));
      MolangQueries.register("camera_rotation", a -> c -> {
         Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
         return arg(a, 0, c) == 0.0 ? camera.xRot() : camera.yRot();
      });
      MolangQueries.register("rotation_to_camera", a -> c -> {
         if (c.actor == null) {
            return 0.0;
         }

         Vec3 d = cameraPos().subtract(cameraOrigin(c.actor, c.partialTick, true));
         return arg(a, 0, c) == 0.0 ? -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))) : Math.toDegrees(Math.atan2(-d.x, d.z));
      });
   }

   private static void state() {
      MolangQueries.flag("is_baby", LivingEntity::isBaby);
      MolangQueries.flag("is_sleeping", LivingEntity::isSleeping);
      MolangQueries.actorFlag("is_on_fire", Entity::isOnFire);
      MolangQueries.actorFlag("is_invisible", Entity::isInvisible);
      MolangQueries.flag("is_saddled", e -> e instanceof Mob m && m.isSaddled());
      MolangQueries.actorFlag("is_leashed", e -> e instanceof Mob m && m.isLeashed());
      MolangQueries.flag("is_tamed", e -> e instanceof TamableAnimal t && t.isTame() || e instanceof AbstractHorse h && h.isTamed());
      MolangQueries.flag("is_sitting", e -> e instanceof TamableAnimal t && t.isInSittingPose() || e.getPose() == Pose.SITTING);
      MolangQueries.flag("is_angry", e -> e instanceof NeutralMob n && n.isAngry());
      MolangQueries.flag("is_in_love", e -> e instanceof Animal a && a.isInLove());
      MolangQueries.flag("is_chested", e -> e instanceof AbstractChestedHorse h && h.hasChest());
      MolangQueries.flag("is_sheared", e -> e instanceof Sheep s && s.isSheared());
      MolangQueries.flag("is_grazing", e -> e instanceof Sheep s && s.getHeadEatPositionScale(0.0F) > 0.0F || e instanceof AbstractHorse h && h.isEating());
      MolangQueries.flag("is_eating", e -> e instanceof AbstractHorse h && h.isEating());
      MolangQueries.flag("is_using_item", LivingEntity::isUsingItem);
      MolangQueries.flag("blocking", LivingEntity::isBlocking);
      MolangQueries.actorFlag("is_silent", Entity::isSilent);
      MolangQueries.actorFlag("has_gravity", e -> !e.isNoGravity());
      MolangQueries.flag("is_breathing", e -> e.getAirSupply() >= e.getMaxAirSupply());
      MolangQueries.flag("is_local_player", e -> e == Minecraft.getInstance().player);
      MolangQueries.flag("is_spectator", e -> e instanceof Player p && p.isSpectator());
      MolangQueries.flag("is_aggressive", e -> e instanceof Mob m && m.isAggressive());
      MolangQueries.flag("has_target", e -> e instanceof Mob m && m.isAggressive());
      MolangQueries.living("scale", LivingEntity::getScale);
      MolangQueries.living("model_scale", LivingEntity::getScale);
      MolangQueries.living("equipment_count", e -> {
         int n = 0;

         for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.isArmor() && !e.getItemBySlot(s).isEmpty()) {
               n++;
            }
         }

         return n;
      });
      MolangQueries.flag("has_head_gear", e -> !e.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
      MolangQueries.simple("variant", var0 -> 0.0);
      MolangQueries.simple("mark_variant", var0 -> 0.0);
      MolangQueries.simple("skin_id", var0 -> 0.0);
   }

   private static void life() {
      MolangQueries.actorFlag("is_alive", e -> e.isAlive() && !(e instanceof LivingEntity l && l.isDeadOrDying()));
      MolangQueries.living("health", LivingEntity::getHealth);
      MolangQueries.living("max_health", LivingEntity::getMaxHealth);
      MolangQueries.simple("hurt_time", c -> {
         int t = c.entity != null ? c.entity.hurtTime : (VehicleCompatibility.hurtTime(c.actor));
         return t <= 0 ? 0.0 : Math.max(0.0F, t - c.partialTick);
      });
      MolangQueries.simple("death_ticks", c -> c.entity != null && c.entity.deathTime > 0 ? c.entity.deathTime + c.partialTick : 0.0);
      MolangQueries.living("invulnerable_ticks", e -> e.invulnerableTime);
      MolangQueries.actor("hurt_direction", e -> VehicleCompatibility.hurtDirection(e));
      MolangQueries.simple("attack_time", c -> c.entity == null ? 0.0 : c.entity.getAttackAnim(c.partialTick));
   }

   private static void names() {
      MolangQueries.register("is_name_any", a -> {
         String[] constant = MolangQueries.constantStrings(a, 0);
         Set<String> set = constant == null ? null : Set.of(dedupe(constant));
         return c -> {
            if (c.actor != null && c.actor.hasCustomName()) {
               String name = c.actor.getCustomName().getString();
               if (set != null) {
                  return set.contains(name) ? 1.0 : 0.0;
               }

               for (String s : MolangQueries.strings(a, 0, c)) {
                  if (s.equals(name)) {
                     return 1.0;
                  }
               }

               return 0.0;
            } else {
               return 0.0;
            }
         };
      });
      MolangQueries.registerString("get_name", var0 -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            return 0.0;
         }

         @Override
         public Object value(MolangContext c) {
            return c.actor != null && c.actor.hasCustomName() ? c.actor.getCustomName().getString() : "";
         }
      });
      MolangQueries.registerString("property", a -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            Object v = this.get(c);
            return v instanceof Number n ? n.doubleValue() : (v instanceof Boolean b ? (b ? 1 : 0) : 0.0);
         }

         @Override
         public Object value(MolangContext c) {
            Object v = this.get(c);
            return v instanceof String || v instanceof Double ? v : (v instanceof Number n ? n.doubleValue() : v instanceof Boolean b ? (b ? 1.0 : 0.0) : 0.0);
         }

         private Object get(MolangContext c) {
            return a.length == 0 ? null : BedrockProperties.get(c.entity, MolangQueries.string(a[0], c));
         }
      });
      MolangQueries.register("has_property", a -> c -> a.length > 0 && BedrockProperties.get(c.entity, MolangQueries.string(a[0], c)) != null ? 1.0 : 0.0);
   }

   private static String[] dedupe(String[] a) {
      return new HashSet<>(Arrays.asList(a)).toArray(new String[0]);
   }

   private static Holder<Biome> biome(MolangContext c) {
      if (c.actor == null) {
         return null;
      }

      Entity e = c.actor;
      return c.cached("ans:biome", () -> e.level().getBiome(BlockPos.containing(e.getX(), e.getY() + 0.1, e.getZ())));
   }

   private static String javaBiome(String bedrock) {
      String id = bedrock.contains(":") ? bedrock : "minecraft:" + bedrock;
      return BIOMES.getOrDefault(id, id);
   }

   private static boolean typeMatches(Entity vehicle, String bedrockId) {
      ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
      String path = key.getPath();
      String id = bedrockId.contains(":") ? bedrockId : "minecraft:" + bedrockId;

      return switch (id) {
         case "minecraft:boat" -> (path.endsWith("_boat") || path.endsWith("_raft")) && !path.contains("chest");
         case "minecraft:chest_boat" -> path.endsWith("_chest_boat") || path.endsWith("_chest_raft");
         default -> key.toString().equals(id);
      };
   }

   private static void world() {
      MolangQueries.register("entity_biome_has_any_identifier", a -> {
         String[] constant = MolangQueries.constantStrings(a, 0);
         Set<String> set = new HashSet<>();
         if (constant != null) {
            for (String s : constant) {
               set.add(javaBiome(s));
            }
         }

         return c -> {
            Holder<Biome> b = biome(c);
            if (b == null) {
               return 0.0;
            }

            String id = b.unwrapKey().map(k -> k.location().toString()).orElse("");
            if (constant != null) {
               return set.contains(id) ? 1.0 : 0.0;
            }

            for (String sx : MolangQueries.strings(a, 0, c)) {
               if (javaBiome(sx).equals(id)) {
                  return 1.0;
               }
            }

            return 0.0;
         };
      });
      MolangQueries.register("entity_biome_has_any_tags", a -> c -> {
         Holder<Biome> b = biome(c);
         if (b == null) {
            return 0.0;
         }

         for (String s : MolangQueries.strings(a, 0, c)) {
            Predicate<Holder<Biome>> p = BIOME_TAGS.get(s.toLowerCase(Locale.ROOT).replace("minecraft:", ""));
            if (p != null && p.test(b)) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("is_riding_any_entity_of_type", a -> c -> {
         if (c.actor != null && c.actor.getVehicle() != null) {
            for (String s : MolangQueries.strings(a, 0, c)) {
               if (typeMatches(c.actor.getVehicle(), s)) {
                  return 1.0;
               }
            }

            return 0.0;
         } else {
            return 0.0;
         }
      });
      MolangQueries.register("has_any_leashed_entity_of_type", var0 -> var0x -> 0.0);
      BlockTagQueries.install();
      MolangQueries.register(
         "is_pack_setting_selected", a -> c -> a.length >= 2 && PackSettings.selected(MolangQueries.string(a[0], c), MolangQueries.string(a[1], c)) ? 1.0 : 0.0
      );
      MolangQueries.register("is_pack_setting_enabled", a -> c -> {
         if (a.length < 1) {
            return 0.0;
         }

         String id = MolangQueries.string(a[0], c);
         return PackSettings.enabled(id) && ExpressionSkins.allows(c.actor, id) ? 1.0 : 0.0;
      });
      MolangQueries.register("graphics_mode_is_any", a -> c -> {
         for (String s : MolangQueries.strings(a, 0, c)) {
            if (s.equalsIgnoreCase(graphicsMode)) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.simple("actor_count", var0 -> ClientUtil.getVisibleEntityCount());
   }

   private static void items() {
      EquipmentQueries.install();
   }

   private static void context() {
      MolangQueries.simple("is_in_ui", var0 -> 0.0);
      MolangQueries.simple("is_attached", var0 -> 0.0);
      MolangQueries.simple("is_attached_to_entity", var0 -> 0.0);
      MolangQueries.simple("is_emoting", var0 -> 0.0);
      MolangQueries.simple("is_persona_or_premium_skin", var0 -> 0.0);
      MolangQueries.simple("is_first_person", var0 -> 0.0);
      MolangQueries.simple("is_on_screen", var0 -> 1.0);
      MolangQueries.simple("is_ghost", var0 -> 0.0);
      MolangQueries.register("is_owner_identifier_any", var0 -> var0x -> 0.0);
   }

   private static void helpers() {
      MolangQueries.register("in_range", a -> c -> {
         double x = arg(a, 0, c);
         return x >= arg(a, 1, c) && x <= arg(a, 2, c) ? 1.0 : 0.0;
      });
      MolangQueries.register("any", a -> c -> {
         if (a.length == 0) {
            return 0.0;
         }

         Object x = a[0].value(c);

         for (int i = 1; i < a.length; i++) {
            if (Nodes.equal(x, a[i].value(c))) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("all", a -> c -> {
         if (a.length < 2) {
            return 0.0;
         }

         Object x = a[0].value(c);

         for (int i = 1; i < a.length; i++) {
            if (!Nodes.equal(x, a[i].value(c))) {
               return 0.0;
            }
         }

         return 1.0;
      });
      MolangQueries.register("approx_eq", a -> c -> {
         if (a.length < 2) {
            return 1.0;
         }

         double x = arg(a, 0, c);

         for (int i = 1; i < a.length; i++) {
            if (Math.abs(a[i].eval(c) - x) > 1.0E-7) {
               return 0.0;
            }
         }

         return 1.0;
      });
   }

   static {
      BIOMES.put("minecraft:cold_beach", "minecraft:snowy_beach");
      BIOMES.put("minecraft:cold_taiga", "minecraft:snowy_taiga");
      BIOMES.put("minecraft:cold_taiga_hills", "minecraft:snowy_taiga");
      BIOMES.put("minecraft:cold_taiga_mutated", "minecraft:snowy_taiga");
      BIOMES.put("minecraft:ice_plains", "minecraft:snowy_plains");
      BIOMES.put("minecraft:ice_mountains", "minecraft:snowy_plains");
      BIOMES.put("minecraft:ice_plains_spikes", "minecraft:ice_spikes");
      BIOMES.put("minecraft:legacy_frozen_ocean", "minecraft:frozen_ocean");
      BIOMES.put("minecraft:extreme_hills", "minecraft:windswept_hills");
      BIOMES.put("minecraft:extreme_hills_plus_trees", "minecraft:windswept_forest");
      BIOMES.put("minecraft:mesa", "minecraft:badlands");
      BIOMES.put("minecraft:mesa_plateau_stone", "minecraft:wooded_badlands");
      BIOMES.put("minecraft:roofed_forest", "minecraft:dark_forest");
      BIOMES.put("minecraft:mega_taiga", "minecraft:old_growth_pine_taiga");
      BIOMES.put("minecraft:redwood_taiga_mutated", "minecraft:old_growth_spruce_taiga");
      BIOMES.put("minecraft:birch_forest_mutated", "minecraft:old_growth_birch_forest");
      BIOMES.put("minecraft:savanna_mutated", "minecraft:windswept_savanna");
      BIOMES.put("minecraft:sunflower_plains", "minecraft:sunflower_plains");
      BIOMES.put("minecraft:hell", "minecraft:nether_wastes");
      BIOMES.put("minecraft:the_end", "minecraft:the_end");
      BIOMES.put("minecraft:mushroom_island", "minecraft:mushroom_fields");
      BIOMES.put("minecraft:stone_beach", "minecraft:stony_shore");
      BIOMES.put("minecraft:jungle_edge", "minecraft:sparse_jungle");
      BIOMES.put("minecraft:bamboo_jungle", "minecraft:bamboo_jungle");
      BIOME_TAGS.put("jungle", b -> b.is(BiomeTags.IS_JUNGLE));
      BIOME_TAGS.put("mesa", b -> b.is(BiomeTags.IS_BADLANDS));
      BIOME_TAGS.put("nether", b -> b.is(BiomeTags.IS_NETHER));
      BIOME_TAGS.put("savanna", b -> b.is(BiomeTags.IS_SAVANNA));
      BIOME_TAGS.put("taiga", b -> b.is(BiomeTags.IS_TAIGA));
      BIOME_TAGS.put("ocean", b -> b.is(BiomeTags.IS_OCEAN));
      BIOME_TAGS.put("forest", b -> b.is(BiomeTags.IS_FOREST));
      BIOME_TAGS.put("mountain", b -> b.is(BiomeTags.IS_MOUNTAIN));
      BIOME_TAGS.put("desert", b -> b.is(ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "desert"))));
      BIOME_TAGS.put("deep_dark", b -> b.is(ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "deep_dark"))));
   }
}
