package com.ans.client.mob;

import com.ans.Ans;
import com.ans.client.bedrock.ActorScales;
import com.ans.client.bedrock.ChangeColors;
import com.ans.client.molang.BedrockProperties;
import com.ans.client.molang.MolangQueries;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public final class AnimatedMob<E extends Entity> {
   private final EntityType<E> type;
   private final String id;
   private final ResourceLocation bundleFile;
   private final float shadowRadius;
   private final boolean vanillaDeathRotation;
   private final List<String> effectsOnly;
   private final AnimatedMob.RendererFactory<E> renderer;
   private final Function<E, ResourceLocation> bundleSelector;
   private final ToDoubleBiFunction<E, Float> yaw;
   private final boolean living;
   private final float deathRotation;

   private AnimatedMob(AnimatedMob.Builder<E> b) {
      this.type = b.type;
      this.id = b.id;
      this.bundleFile = b.bundleFile != null ? b.bundleFile : Ans.id("entity/" + b.id + ".json");
      this.shadowRadius = b.shadowRadius;
      this.vanillaDeathRotation = b.vanillaDeathRotation;
      this.deathRotation = b.deathRotation;
      this.bundleSelector = b.bundleSelector;
      this.yaw = b.yaw;
      this.living = b.living != null ? b.living : DefaultAttributes.hasSupplier(b.type);
      this.renderer = b.renderer;
      this.effectsOnly = b.effectsOnly;
   }

   public boolean effectsOnly() {
      return this.effectsOnly != null;
   }

   public List<String> hiddenBones() {
      return this.effectsOnly == null ? List.of() : this.effectsOnly;
   }

   public AnimatedMob.RendererFactory<E> renderer() {
      return this.renderer;
   }

   public boolean living() {
      return this.living;
   }

   public ResourceLocation bundleFor(Entity entity) {
      if (this.bundleSelector != null) {
         ResourceLocation id = this.bundleSelector.apply((E)entity);
         if (id != null) {
            return id;
         }
      }

      return this.bundleFile;
   }

   public float yaw(Entity entity, float partialTick) {
      return this.yaw == null ? 0.0F : (float)this.yaw.applyAsDouble((E)entity, partialTick);
   }

   public float deathRotation() {
      return this.deathRotation;
   }

   public static <E extends Entity> AnimatedMob.Builder<E> builder(EntityType<E> type) {
      return new AnimatedMob.Builder<>(type, BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
   }

   public static <E extends Entity> AnimatedMob.Builder<E> builder(EntityType<E> type, String id) {
      return new AnimatedMob.Builder<>(type, id);
   }

   public static ResourceLocation geoId(String name) {
      return Ans.id("entity/" + name);
   }

   public EntityType<E> type() {
      return this.type;
   }

   public String id() {
      return this.id;
   }

   public ResourceLocation bundleFile() {
      return this.bundleFile;
   }

   public float shadowRadius() {
      return this.shadowRadius;
   }

   public boolean vanillaDeathRotation() {
      return this.vanillaDeathRotation;
   }

   @Override
   public String toString() {
      return "AnimatedMob[" + this.id + "]";
   }

   public static final class Builder<E extends Entity> {
      private final EntityType<E> type;
      private final String id;
      private ResourceLocation bundleFile;
      private float shadowRadius = Float.NaN;
      private boolean vanillaDeathRotation = true;
      private Function<E, ResourceLocation> bundleSelector;
      private ToDoubleBiFunction<E, Float> yaw;
      private Boolean living;
      private AnimatedMob.RendererFactory<E> renderer;
      private List<String> effectsOnly;
      private float deathRotation = Float.NaN;

      private Builder(EntityType<E> type, String id) {
         this.type = type;
         this.id = id;
      }

      public AnimatedMob.Builder<E> bundle(ResourceLocation file) {
         this.bundleFile = file;
         return this;
      }

      public AnimatedMob.Builder<E> bundleFor(Function<E, ResourceLocation> selector) {
         this.bundleSelector = selector;
         return this;
      }

      public AnimatedMob.Builder<E> yaw(ToDoubleBiFunction<E, Float> yaw) {
         this.yaw = yaw;
         return this;
      }

      public AnimatedMob.Builder<E> living(boolean living) {
         this.living = living;
         return this;
      }

      public AnimatedMob.Builder<E> renderer(AnimatedMob.RendererFactory<E> factory) {
         this.renderer = factory;
         return this;
      }

      public AnimatedMob.Builder<E> shadow(float radius) {
         this.shadowRadius = radius;
         return this;
      }

      public AnimatedMob.Builder<E> effectsOnly(String... hiddenBones) {
         this.effectsOnly = List.of(hiddenBones);
         return this;
      }

      public AnimatedMob.Builder<E> vanillaDeathRotation(boolean enabled) {
         this.vanillaDeathRotation = enabled;
         return this;
      }

      public AnimatedMob.Builder<E> deathRotation(float degrees) {
         this.deathRotation = degrees;
         return this;
      }

      public AnimatedMob.Builder<E> query(String bedrockName, ToDoubleFunction<E> fn) {
         MolangQueries.registerFor(this.type, bedrockName.replaceFirst("^(query|q)\\.", ""), fn);
         return this;
      }

      public AnimatedMob.Builder<E> property(String bedrockName, Function<E, Object> provider) {
         BedrockProperties.register(this.type, bedrockName, provider);
         return this;
      }

      public AnimatedMob.Builder<E> changeColor(ToIntBiFunction<E, Float> provider) {
         ChangeColors.register(this.type, provider);
         return this;
      }

      public AnimatedMob.Builder<E> actorScale(ToDoubleFunction<E> scale) {
         ActorScales.register(this.type, scale);
         return this;
      }

      public AnimatedMob<E> build() {
         return new AnimatedMob<>(this);
      }

      public AnimatedMob<E> register() {
         return AnimatedMobs.register(this.build());
      }
   }

   @FunctionalInterface
   public interface RendererFactory<E extends Entity> {
      EntityRenderer<E> create(Context var1, AnimatedMob<E> var2, EntityRendererProvider<E> var3);
   }
}
