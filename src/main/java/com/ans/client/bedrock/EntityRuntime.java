package com.ans.client.bedrock;

import net.minecraft.util.Mth;
import com.ans.client.molang.EngineVariables;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangExpr;
import com.ans.client.molang.MolangScope;
import com.ans.client.particle.BundleParticles;
import com.ans.client.particle.RuntimeEffects;
import com.ans.client.sound.BundleSounds;
import com.ans.client.sound.LocalPlayerSounds;
import com.ans.client.sound.PackSounds;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import com.ans.client.util.ArgbCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public final class EntityRuntime implements MolangContext.BoneSource {
   private static final int MAX_DEPTH = 12;
   private final EntityBundle bundle;
   private final String mobId;
   private final MolangScope vars = new MolangScope();
   private final MolangContext ctx = new MolangContext(this.vars);
   private final int generation;
   private boolean initialized;
   private double lastAge = Double.NaN;
   private double clock;
   private long frame;
   private List<EntityRuntime.EntryRun> animate = List.of();
   private MobPose pose = new MobPose();
   private MobPose lastPose;
   private List<ResourceLocation> lastModels = List.of();
   private RuntimeEffects particles;
   private boolean relativeBones;
   private boolean clampWeights;
   private static final boolean SWAP_BLEND = !"false".equalsIgnoreCase(System.getProperty("ans.swapBlend", "true"));
   private static final double SWAP_WINDOW = 0.25;
   private Item lastMainItem;
   private Item lastOffItem;
   private double itemSwapAt = Double.NEGATIVE_INFINITY;
   private boolean firstPerson;
   private Consumer<MolangContext> engineHook;
   private boolean ui;
   private static final List<EntityRuntime.PreAnimationHook> PRE_ANIMATION_HOOKS = new CopyOnWriteArrayList<>();
   private static final boolean SCRIPT_TEMPS = !"false".equalsIgnoreCase(System.getProperty("ans.scriptTemps"));
   private static final double STEP_NUDGE = 2.5E-5;
   public static volatile EntityRuntime.SampleTrace sampleTrace;
   private static final List<EntityBundle.Rule> DEFAULT_MATERIAL_RULES = List.of(
      new EntityBundle.Rule(new BonePattern("*"), Molang.compile("material.default"))
   );
   private Function<String, double []> boneTrsSource;
   private static final float[] NO_REST = new float[3];
   private final Map<String, float[]> restRotations = new HashMap<>();
   private int restGeneration = -1;

   private boolean swapBlendActive() {
      return SWAP_BLEND && this.clock - this.itemSwapAt <= 0.25;
   }

   private void trackItemSwap(LivingEntity living) {
      if (living != null && !this.firstPerson && this.ctx.attachable == null) {
         Item main = living.getMainHandItem().getItem();
         Item off = living.getOffhandItem().getItem();
         if (this.lastMainItem != null && (main != this.lastMainItem || off != this.lastOffItem)) {
            this.itemSwapAt = this.clock;
         }

         this.lastMainItem = main;
         this.lastOffItem = off;
      }
   }

   public static void clampNegativeWeights(String mobId) {
   }

   public EntityRuntime firstPerson(boolean firstPerson, Consumer<MolangContext> engineHook) {
      this.firstPerson = firstPerson;
      this.engineHook = engineHook;
      return this;
   }

   public boolean isFirstPerson() {
      return this.firstPerson;
   }

   public EntityRuntime ui(boolean ui) {
      this.ui = ui;
      return this;
   }

   public boolean isUi() {
      return this.ui;
   }

   public EntityRuntime(String mobId, EntityBundle bundle) {
      this.mobId = mobId;
      this.bundle = bundle;
      this.generation = AnimationLookup.generation();
      this.ctx.owner = this;
   }

   public EntityBundle bundle() {
      return this.bundle;
   }

   public int generation() {
      return this.generation;
   }

   public MolangScope variables() {
      return this.vars;
   }

   public MolangContext context() {
      return this.ctx;
   }

   public String mobId() {
      return this.mobId;
   }

   public RuntimeEffects particles() {
      if (this.particles == null) {
         this.particles = new RuntimeEffects(this);
      }

      return this.particles;
   }

   public List<ResourceLocation> lastModels() {
      return this.lastModels;
   }

   private boolean particlesOn() {
      return !this.bundle.particles.isEmpty() && !this.firstPerson && !this.ui && (this.ctx.attachable == null || !this.ctx.attachable.firstPerson());
   }

   public static void addPreAnimationHook(EntityRuntime.PreAnimationHook hook) {
      PRE_ANIMATION_HOOKS.add(hook);
   }

   private static void execScript(List<MolangExpr> script, MolangContext c) {
      if (SCRIPT_TEMPS && script.size() >= 2) {
         boolean keep = c.keepTemps;
         c.temps.clear();
         c.keepTemps = true;

         try {
            for (MolangExpr s : script) {
               s.exec(c);
            }
         } finally {
            c.keepTemps = keep;
            c.temps.clear();
         }
      } else {
         for (MolangExpr s : script) {
            s.exec(c);
         }
      }
   }

   public FrameResult tick(Entity entity, float partialTick) {
      double age = entity.tickCount + partialTick;
      double dt = Double.isNaN(this.lastAge) ? 0.0 : Mth.clamp((age - this.lastAge) / 20.0, 0.0, 0.5);
      this.lastAge = age;
      this.clock += dt;
      this.frame++;
            if (entity instanceof LocalPlayer && "player".equals(this.mobId) && !this.firstPerson && !this.ui) {
         LocalPlayerSounds.ran(entity.getId());
      }

      this.relativeBones = RelativeBones.enabled(this.mobId);
      this.clampWeights = BedrockRules.clampNegativeWeights(this.mobId);
      MolangContext c = this.ctx;
      c.actor = entity;
      c.entity = entity instanceof LivingEntity living ? living : null;
      c.partialTick = partialTick;
      c.deltaTime = dt;
      c.clock = this.clock;
      c.frame = this.frame;
      c.animTime = 0.0;
      c.controller = null;
      c.arrays = null;
      c.frameCache.clear();
      c.firstPerson = this.firstPerson;
      this.trackItemSwap(c.entity);
      MolangContext previous = c.enter();

      try {
         EngineVariables.apply(entity, partialTick, this.vars);
         if (this.engineHook != null) {
            this.engineHook.accept(c);
         }

         if (!this.initialized) {
            this.initialized = true;
            execScript(this.bundle.initialize, c);
            this.animate = this.entries(this.bundle.animate, 0);
         }

         if (c.attachable == null) {
            for (EntityRuntime.PreAnimationHook h : PRE_ANIMATION_HOOKS) {
               h.run(entity, this, c);
            }
         }

         execScript(this.bundle.preAnimation, c);
         this.pose = new MobPose();
         this.pose.owner = this;
         if (this.particlesOn()) {
            this.particles().ticked(entity, c.attachable != null);
         }

         this.tickEntries(this.animate, 1.0);
         c.controller = null;
         c.animTime = 0.0;
         float s = (float)this.eval(this.bundle.scale, 1.0) * ActorScales.correction(entity);
         float sx = s * (float)this.eval(this.bundle.scaleX, 1.0);
         float sy = s * (float)this.eval(this.bundle.scaleY, 1.0);
         float sz = s * (float)this.eval(this.bundle.scaleZ, 1.0);
         List<RenderLayer> layers = this.renderLayers(entity);
         if (this.relativeBones && !layers.isEmpty() && c.attachable == null) {
            BakedGeoModel m = BoneTransforms.model(layers.get(0).model());
            if (m != null) {
               RelativeBones.resolve(this.pose, m);
            }
         }

         this.lastPose = this.pose;
         List<ResourceLocation> models = new ArrayList<>(layers.size());

         for (RenderLayer l : layers) {
            if (!models.contains(l.model())) {
               models.add(l.model());
            }
         }

         this.lastModels = models;
         return new FrameResult(this.pose, layers, sx, sy, sz);
      } finally {
         MolangContext.restore(previous);
         c.entity = null;
         c.actor = null;
               }
   }

   private double eval(MolangExpr e, double fallback) {
      if (e == null) {
         return fallback;
      }

      double v = e.eval(this.ctx);
      return Double.isFinite(v) ? v : fallback;
   }

   private void playSounds(List<String> events, String origin) {
      Entity actor = this.ctx.actor;
      if (actor != null && !events.isEmpty()) {
         if (!this.firstPerson && !this.ui && (this.ctx.attachable == null || !this.ctx.attachable.firstPerson())) {
            try {
               PackSounds.play(actor, this.ctx.partialTick, events, this.mobId, origin);
            } catch (RuntimeException e) {
               Molang.warn(this.mobId + ": could not play " + events + ": " + e);
            }
         }
      }
   }

   private void startParticles(EntityBundle.ControllerDef controller, EntityRuntime.StateRun run) {
      if (run.particles == null) {
         List<BundleParticles.Effect> effects = this.particlesOn() ? this.bundle.particles.state(controller.key(), run.def.name()) : List.of();
         run.particles = effects.isEmpty() ? List.of() : this.particles().start(this.ctx.actor, effects);
      }
   }

   private void stopParticles(EntityRuntime.StateRun run) {
      if (run != null) {
         if (run.particles != null && !run.particles.isEmpty()) {
            RuntimeEffects.stop(run.particles);
         }

         for (EntityRuntime.EntryRun e : run.entries) {
            if (e.player instanceof EntityRuntime.ControllerPlayer cp) {
               this.stopParticles(cp.current);
               this.stopParticles(cp.previous);

               for (EntityRuntime.Fade f : cp.older) {
                  this.stopParticles(f.run());
               }
            }
         }
      }
   }

   private List<EntityRuntime.EntryRun> entries(List<EntityBundle.Entry> defs, int depth) {
      List<EntityRuntime.EntryRun> out = new ArrayList<>(defs.size());

      for (EntityBundle.Entry def : defs) {
         EntityRuntime.Player p = this.create(def.ref(), depth);
         if (p != null) {
            out.add(new EntityRuntime.EntryRun(def, p));
         }
      }

      return out;
   }

   private EntityRuntime.Player create(String ref, int depth) {
      EntityBundle.AnimationDef a = this.bundle.animations.get(ref);
      if (a != null) {
         return new EntityRuntime.AnimationPlayer(a);
      }

      EntityBundle.ControllerDef c = this.bundle.controllers.get(ref);
      if (c != null) {
         if (depth >= 12) {
            Molang.warn(this.mobId + ": controller nesting too deep at '" + ref + "'");
            return null;
         } else {
            return new EntityRuntime.ControllerPlayer(c, depth + 1);
         }
      } else {
         Molang.warn(this.mobId + ": '" + ref + "' is referenced but not defined (animation missing from the pack?); skipped");
         return null;
      }
   }

   private void tickEntries(List<EntityRuntime.EntryRun> runs, double weight) {
      for (EntityRuntime.EntryRun run : runs) {
         MolangExpr w = run.def.weight();
         double v = w == null ? 1.0 : w.eval(this.ctx);
         if (v < 0.0) {
            BedrockRules.negativeWeight(this.mobId, run.def.ref(), v);
         }

         if (v != 0.0 && Double.isFinite(v) && (!this.clampWeights || !(v < 0.0))) {
            run.player.tick(weight * v);
         }
      }
   }

   private void sample(EntityRuntime.AnimationPlayer player, float weight) {
      Animation animation = player.baked;
      if (animation == null) {
         return;
      }
      boolean step = BedrockRules.stepKeyframes();
      double at = step ? player.time + 2.5E-5 : player.time;
      Map<String, BedrockSampler.BoneTransform> sampled = BedrockSampler.sample(animation, at, step);
      BoneAnimation[] bones = animation.boneAnimations();
      boolean override = player.def.overridePrevious();
      float keep = 1.0F - weight;
      if (this.relativeBones) {
         List<String> rel = this.bundle.relativeToEntity.get(player.def.key());
         if (rel != null) {
            for (String r : rel) {
               this.pose.markEntityRelative(r);
            }
         }
      }

      MolangContext c = this.ctx;
      boolean bedrockThis = BedrockRules.animationThis();
      EntityRuntime.SampleTrace trace = sampleTrace;

      try {
         for (BoneAnimation bone : bones) {
            BedrockSampler.BoneTransform t = sampled.get(bone.boneName());
            if (t == null) {
               continue;
            }
            float[] v = this.pose.bone(bone.boneName());
            float[] before = trace != null ? (float[])v.clone() : null;
            float[] rest = bedrockThis ? this.restRotation(bone.boneName()) : NO_REST;

            for (int a = 0; a < 3; a++) {
               if (hasChannel(bone.rotationKeyFrames(), a)) {
                  if (bedrockThis) {
                     c.thisValue = a == 2 ? Math.toDegrees(v[a] + rest[a]) : -Math.toDegrees(v[a] + rest[a]);
                  }
                  float x = a == 0 ? t.rotX() : a == 1 ? t.rotY() : t.rotZ();
                  v[a] = override ? v[a] * keep + weight * x : v[a] + weight * x;
               }

               if (hasChannel(bone.positionKeyFrames(), a)) {
                  if (bedrockThis) {
                     c.thisValue = v[3 + a];
                  }
                  float x = a == 0 ? t.posX() : a == 1 ? t.posY() : t.posZ();
                  v[3 + a] = override ? v[3 + a] * keep + weight * x : v[3 + a] + weight * x;
               }

               if (hasChannel(bone.scaleKeyFrames(), a)) {
                  if (bedrockThis) {
                     c.thisValue = v[6 + a];
                  }
                  float s = a == 0 ? t.scaleX() : a == 1 ? t.scaleY() : t.scaleZ();
                  v[6 + a] = override ? v[6 + a] * keep + weight * s : v[6 + a] * (1.0F + weight * (s - 1.0F));
               }
            }

            if (trace != null) {
               trace.sampled(this, player.def.key(), bone.boneName(), weight, before, v);
            }
         }
      } finally {
         c.thisValue = 0.0;
      }
   }

   /** True if the stack has keyframes on channel a (0 = x, 1 = y, 2 = z). */
   private static boolean hasChannel(KeyframeStack<?> stack, int a) {
      if (stack == null) {
         return false;
      }
      List<?> frames = a == 0 ? stack.xKeyframes() : a == 1 ? stack.yKeyframes() : stack.zKeyframes();
      return frames != null && !frames.isEmpty();
   }

   private List<RenderLayer> renderLayers(Entity entity) {
      MolangContext c = this.ctx;
      List<RenderLayer> out = new ArrayList<>(this.bundle.renderControllers.size());

      for (EntityBundle.RenderControllerDef rc : this.bundle.renderControllers) {
         if (rc.condition() == null || rc.condition().test(c)) {
            c.arrays = rc.arrays();

            try {
               ResourceLocation model = this.resource(rc.geometry(), "geometry.", this.bundle.geometry);
               ResourceLocation texture = rc.textures().isEmpty() ? null : this.resource(rc.textures().get(0), "texture.", this.bundle.textures);
               if (model != null && texture != null) {
                  List<EntityBundle.Rule> materialRules = rc.materials().isEmpty()
                        && this.bundle.materials.containsKey("default")
                        && BedrockRules.defaultMaterialFallback()
                     ? DEFAULT_MATERIAL_RULES
                     : rc.materials();
                  List<RenderLayer.MaterialRule> materials = new ArrayList<>(materialRules.size());
                  boolean colorMask = false;
                  boolean multiTexture = false;

                  for (EntityBundle.Rule r : materialRules) {
                     EntityBundle.Material m = r.expr().value(c) instanceof String s && s.startsWith("material.")
                        ? this.bundle.materials.get(s.substring(9))
                        : null;
                     boolean masked = m != null && (m.bedrock().contains("change_color") || m.colorMask());
                     materials.add(
                        new RenderLayer.MaterialRule(
                           r.pattern(),
                           m != null ? m.kind() : RenderKind.CUTOUT,
                           masked,
                           m != null && (m.uvAnim() || m.kind().textureMatrixOffset()),
                           m != null && m.glint()
                        )
                     );
                     if (masked) {
                        colorMask = true;
                     }

                     if (m != null
                        && (m.bedrock().contains("multitexture") || m.bedrock().endsWith("_masked") || m.bedrock().equals("llama") || m.multiTexture())) {
                        multiTexture = true;
                     }
                  }

                  List<RenderLayer.VisibilityRule> visibility = new ArrayList<>(rc.partVisibility().size());

                  for (EntityBundle.Rule r : rc.partVisibility()) {
                     visibility.add(new RenderLayer.VisibilityRule(r.pattern(), r.expr().test(c)));
                  }

                  float[] tint = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
                  float[] rcColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
                  this.applyColor(rc.color(), rcColor, false);
                  boolean colorOnLast = multiTexture && rc.color() != null && rc.textures().size() > 1;
                  if (!colorOnLast) {
                     for (int k = 0; k < 4; k++) {
                        tint[k] *= rcColor[k];
                     }
                  }

                  float[] mix = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
                  this.mixOverlay(rc.overlayColor(), mix);
                  if (entity.isOnFire()) {
                     this.mixOverlay(rc.onFireColor(), mix);
                  }

                  int overlayMix = mix[3] <= 0.001F ? 0 : ArgbCompat.colorFromFloat(mix[3], mix[0], mix[1], mix[2]);
                  boolean hurt = true;
                  if (rc.isHurtColor() != null) {
                     hurt = rc.isHurtColor().a().eval(c) > 0.001;
                     if (entity instanceof LivingEntity living && (living.hurtTime > 0 || living.deathTime > 0)) {
                        rc.isHurtColor().r().eval(c);
                        rc.isHurtColor().g().eval(c);
                        rc.isHurtColor().b().eval(c);
                     }
                  }

                  int argb = ArgbCompat.colorFromFloat(tint[3], tint[0], tint[1], tint[2]);
                  float uvU = 0.0F;
                  float uvV = 0.0F;
                  float uvSU = 1.0F;
                  float uvSV = 1.0F;
                  if (rc.uvAnim() != null) {
                     uvU = (float)(rc.uvAnim().offsetU().eval(c) % 1.0);
                     uvV = (float)(rc.uvAnim().offsetV().eval(c) % 1.0);
                     uvSU = (float)rc.uvAnim().scaleU().eval(c);
                     uvSV = (float)rc.uvAnim().scaleV().eval(c);
                     if (!Float.isFinite(uvSU)) {
                        uvSU = 1.0F;
                     }

                     if (!Float.isFinite(uvSV)) {
                        uvSV = 1.0F;
                     }

                     if (!Float.isFinite(uvU)) {
                        uvU = 0.0F;
                     }

                     if (!Float.isFinite(uvV)) {
                        uvV = 0.0F;
                     }
                  }

                  int changeColor = colorMask ? ChangeColors.get(entity, c.partialTick) : 0;
                  float pt = c.partialTick;
                  int first = multiTexture ? ArgbCompat.multiply(argb, ChangeColors.textureTint(entity, pt, 0)) : argb;
                  out.add(
                     new RenderLayer(
                        model,
                        texture,
                        List.copyOf(materials),
                        List.copyOf(visibility),
                        first,
                        hurt,
                        rc.ignoreLighting(),
                        uvU,
                        uvV,
                        changeColor,
                        overlayMix,
                        uvSU,
                        uvSV
                     )
                  );

                  for (int i = 1; multiTexture && i < rc.textures().size(); i++) {
                     ResourceLocation extra = this.resource(rc.textures().get(i), "texture.", this.bundle.textures);
                     int extraTint = ArgbCompat.multiply(argb, ChangeColors.textureTint(entity, pt, i));
                     if (colorOnLast && i == rc.textures().size() - 1) {
                        extraTint = ArgbCompat.multiply(extraTint, ArgbCompat.colorFromFloat(rcColor[3], rcColor[0], rcColor[1], rcColor[2]));
                     }

                     if (extra != null) {
                        out.add(
                           new RenderLayer(
                              model,
                              extra,
                              List.copyOf(materials),
                              List.copyOf(visibility),
                              extraTint,
                              hurt,
                              rc.ignoreLighting(),
                              uvU,
                              uvV,
                              0,
                              overlayMix,
                              uvSU,
                              uvSV
                           )
                        );
                     }
                  }
               }
            } finally {
               c.arrays = null;
            }
         }
      }

      return out;
   }

   private ResourceLocation resource(MolangExpr expr, String prefix, Map<String, ResourceLocation> map) {
      if (expr == null) {
         return null;
      }

      if (expr.value(this.ctx) instanceof String s) {
         String var8 = s.toLowerCase(Locale.ROOT);
         if ("texture.".equals(prefix) && this.ctx.attachable != null && var8.startsWith("ans:derived/trim/")) {
            return ResourceLocation.tryParse(var8);
         }

         if (!var8.startsWith(prefix)) {
            return null;
         }

         ResourceLocation override = this.ctx.attachable != null ? null : ResourceOverrides.resolve(this.ctx.actor, prefix, var8.substring(prefix.length()));
         if (override != null) {
            return override;
         }

         ResourceLocation id = map.get(var8.substring(prefix.length()));
         if (id == null) {
            Molang.warn(this.mobId + ": render controller references unknown " + var8);
         }

         return id;
      } else {
         return null;
      }
   }

   private void mixOverlay(EntityBundle.Color color, float[] mix) {
      if (color != null) {
         MolangContext c = this.ctx;
         float r = (float)color.r().eval(c);
         float g = (float)color.g().eval(c);
         float b = (float)color.b().eval(c);
         float a = (float)color.a().eval(c);
         if (Float.isFinite(a)) {
            a = Mth.clamp(a, 0.0F, 1.0F);
            if (!(a <= 0.001F)) {
               float a0 = mix[3];
               float total = 1.0F - (1.0F - a0) * (1.0F - a);
               float[] rgb = new float[]{
                  Mth.clamp(Float.isFinite(r) ? r : 1.0F, 0.0F, 1.0F),
                  Mth.clamp(Float.isFinite(g) ? g : 1.0F, 0.0F, 1.0F),
                  Mth.clamp(Float.isFinite(b) ? b : 1.0F, 0.0F, 1.0F)
               };

               for (int k = 0; k < 3; k++) {
                  mix[k] = (mix[k] * a0 * (1.0F - a) + rgb[k] * a) / total;
               }

               mix[3] = total;
            }
         }
      }
   }

   private void applyColor(EntityBundle.Color color, float[] tint, boolean overlay) {
      if (color != null) {
         MolangContext c = this.ctx;
         double savedThis = c.thisValue;
         if (!overlay) {
            c.thisValue = 1.0;
         }

         float r = (float)color.r().eval(c);
         float g = (float)color.g().eval(c);
         float b = (float)color.b().eval(c);
         float a = (float)color.a().eval(c);
         c.thisValue = savedThis;
         if (Float.isFinite(a)) {
            if (overlay) {
               a = Mth.clamp(a, 0.0F, 1.0F);
               if (a <= 0.001F) {
                  return;
               }

               tint[0] *= 1.0F + (Mth.clamp(r, 0.0F, 1.0F) - 1.0F) * a;
               tint[1] *= 1.0F + (Mth.clamp(g, 0.0F, 1.0F) - 1.0F) * a;
               tint[2] *= 1.0F + (Mth.clamp(b, 0.0F, 1.0F) - 1.0F) * a;
            } else {
               tint[0] *= Mth.clamp(r, 0.0F, 1.0F);
               tint[1] *= Mth.clamp(g, 0.0F, 1.0F);
               tint[2] *= Mth.clamp(b, 0.0F, 1.0F);
               tint[3] *= Mth.clamp(a, 0.0F, 1.0F);
            }
         }
      }
   }

   public void boneTrsSource(Function<String, double []> source) {
      this.boneTrsSource = source;
   }

   @Override
   public double [] boneOrientationTrs(String bone) {
      if (this.boneTrsSource != null) {
         double[] r = this.boneTrsSource.apply(bone);
         if (r != null) {
            return r;
         }
      }

      for (ResourceLocation id : this.lastModels) {
         BakedGeoModel model = BoneTransforms.model(id);
         GeoBone b = model == null ? null : BoneTransforms.resolve(model, id, bone);
         if (b != null) {
            return BoneTransforms.trs(b, this.lastPose);
         }
      }

      return null;
   }

   @Override
   public double [] boneOrigin(String bone) {
      List<ResourceLocation> ids = new ArrayList<>(this.lastModels);
      ResourceLocation def = this.bundle.geometry.get("default");
      if (def != null && !ids.contains(def)) {
         ids.add(def);
      }

      for (ResourceLocation id : ids) {
         BakedGeoModel model = BoneTransforms.model(id);
         GeoBone b = model == null ? null : BoneTransforms.resolve(model, id, bone);
         if (b != null) {
            return BoneTransforms.origin(b);
         }
      }

      return null;
   }

   private float[] restRotation(String bone) {
      if (!BedrockRules.animationThisRest()) {
         return NO_REST;
      }

      int generation = AnimationLookup.generation();
      if (generation != this.restGeneration) {
         this.restGeneration = generation;
         this.restRotations.clear();
      }

      float[] r = this.restRotations.get(bone);
      if (r != null) {
         return r;
      }

      r = NO_REST;
      List<ResourceLocation> ids = new ArrayList<>(this.lastModels);

      for (ResourceLocation id : this.bundle.geometry.values()) {
         if (!ids.contains(id)) {
            ids.add(id);
         }
      }

      for (ResourceLocation id : ids) {
         BakedGeoModel model = BoneTransforms.model(id);
         GeoBone gb = model == null ? null : (GeoBone)((Map)BoneLookup.of(model)).get(bone);
         if (gb != null && (gb.getInitialSnapshot().getRotX() != 0.0F || gb.getInitialSnapshot().getRotY() != 0.0F || gb.getInitialSnapshot().getRotZ() != 0.0F)) {
            r = new float[]{gb.getInitialSnapshot().getRotX(), gb.getInitialSnapshot().getRotY(), gb.getInitialSnapshot().getRotZ()};
            break;
         }
      }

      if (!this.lastModels.isEmpty()) {
         this.restRotations.put(bone, r);
      }

      return r;
   }

   public long frameCount() {
      return this.frame;
   }

   public double clockSeconds() {
      return this.clock;
   }

   public MobPose lastPose() {
      return this.pose;
   }

   public String describe(boolean variables) {
      StringBuilder sb = new StringBuilder();
      this.describe(this.animate, sb, 0);
      if (variables) {
         sb.append(" | ").append(this.vars.describe("v."));
      }

      sb.append(" | pose ").append(this.pose.bones().size()).append(" bones");
      return sb.toString();
   }

   private void describe(List<EntityRuntime.EntryRun> runs, StringBuilder sb, int depth) {
      if (depth <= 4) {
         for (EntityRuntime.EntryRun r : runs) {
            if (r.player instanceof EntityRuntime.ControllerPlayer cp) {
               sb.append(' ').append(cp.def.key()).append('=').append(cp.current.def.name());
               if (cp.previous != null) {
                  sb.append("(<-").append(cp.previous.def.name()).append(')');
               }

               sb.append('[');
               this.describe(cp.current.entries, sb, depth + 1);
               sb.append(']');
            } else if (r.player instanceof EntityRuntime.AnimationPlayer ap) {
               sb.append(' ').append(ap.def.key()).append(String.format(Locale.ROOT, "@%.2f", ap.time)).append(ap.finished ? "*" : "");
            }
         }
      }
   }

   @Override
   public String toString() {
      return "EntityRuntime[" + this.mobId + "]";
   }

   private final class AnimationPlayer extends EntityRuntime.Player {
      final EntityBundle.AnimationDef def;
      double time;
      double prevTime = -1.0E-9;
      boolean finished;
      boolean started;
      double startDelay;
      double loopWait;
      Animation baked;
      boolean resolved;

      AnimationPlayer(EntityBundle.AnimationDef def) {
         this.def = def;
      }

      @Override
      boolean finished() {
         return this.finished;
      }

      @Override
      boolean isAnimation() {
         return true;
      }

      double length() {
         if (!Double.isNaN(this.def.length())) {
            return this.def.length();
         }

         if (this.baked == null) {
            return 0.0;
         }

         double l = this.baked.length();
         return l >= 1.0E8 ? 0.0 : l;
      }

      @Override
      void tick(double weight) {
         MolangContext c = EntityRuntime.this.ctx;
         double dt = c.deltaTime;
         if (!this.resolved) {
            this.resolved = true;
            this.baked = this.def.animation() == null ? null : AnimationLookup.baked(EntityRuntime.this.bundle.animationFile, this.def.animation());
         }

         if (!this.started) {
            this.started = true;
            if (this.def.startDelay() != null) {
               c.animTime = 0.0;
               this.startDelay = this.def.startDelay().eval(c);
            }
         }

         if (this.startDelay > 0.0) {
            this.startDelay -= dt;
         } else {
            double length = this.length();
            EntityBundle.Loop loop = this.def.loop();
            if (this.loopWait > 0.0) {
               this.loopWait -= dt;
               if (this.loopWait > 0.0) {
                  this.contribute(weight, length);
                  return;
               }

               this.time = 0.0;
               this.prevTime = -1.0E-9;
            }

            c.animTime = this.time;
            double next = this.def.animTimeUpdate() != null ? this.def.animTimeUpdate().eval(c) : this.time + dt;
            if (!Double.isFinite(next)) {
               next = this.time;
            }

            boolean contributes = true;
            if (loop == EntityBundle.Loop.LOOP) {
               if (length <= 0.0) {
                  this.finished = true;
               } else {
                  double wrapped = (next % length + length) % length;
                  if (wrapped < this.prevTime - 1.0E-9) {
                     this.fireTimeline(this.prevTime, length);
                     this.finished = true;
                     if (this.def.loopDelay() != null) {
                        c.animTime = length;
                        double wait = this.def.loopDelay().eval(c);
                        if (wait > 0.0) {
                           this.loopWait = wait;
                           this.time = length;
                           this.prevTime = length;
                           this.contribute(weight, length);
                           return;
                        }
                     }

                     this.prevTime = -1.0E-9;
                  }

                  next = wrapped;
                  this.fireTimeline(this.prevTime, next);
               }
            } else {
               boolean wasFinished = this.finished;
               if (next >= length) {
                  next = length;
                  this.finished = true;
               }

               if (next < 0.0) {
                  next = 0.0;
               }

               this.fireTimeline(this.prevTime, next);
               if (loop == EntityBundle.Loop.ONCE && wasFinished) {
                  contributes = false;
               }
            }

            this.time = next;
            this.prevTime = next;
            if (contributes) {
               this.contribute(weight, length);
            }
         }
      }

      private void contribute(double weight, double length) {
         MolangContext c = EntityRuntime.this.ctx;
         c.animTime = this.time;
         double w = weight * (this.def.blendWeight() == null ? 1.0 : this.def.blendWeight().eval(c));
         if (w < 0.0) {
            BedrockRules.negativeWeight(EntityRuntime.this.mobId, this.def.key() + " (blend_weight)", w);
         }

         if (this.baked != null && w != 0.0 && Double.isFinite(w) && (!EntityRuntime.this.clampWeights || !(w < 0.0))) {
            EntityRuntime.this.sample(this, (float)w);
         }
      }

      private void fireTimeline(double prev, double next) {
         if (next >= prev) {
            for (BundleSounds.Key k : EntityRuntime.this.bundle.sounds.animation(this.def.key())) {
               if (k.time() > prev && k.time() <= next) {
                  EntityRuntime.this.playSounds(k.events(), "anim " + this.def.key() + "@" + k.time());
               }
            }

            if (EntityRuntime.this.particlesOn()) {
               for (BundleParticles.Key k : EntityRuntime.this.bundle.particles.animation(this.def.key())) {
                  if (k.time() > prev && k.time() <= next) {
                     EntityRuntime.this.particles().fire(EntityRuntime.this.ctx.actor, k.effects());
                  }
               }
            }
         }

         List<EntityBundle.TimelineEvent> events = this.def.timeline();
         if (!events.isEmpty() && !(next < prev)) {
            MolangContext c = EntityRuntime.this.ctx;

            for (EntityBundle.TimelineEvent e : events) {
               if (e.time() > prev && e.time() <= next) {
                  c.animTime = e.time();

                  for (MolangExpr s : e.scripts()) {
                     s.exec(c);
                  }
               }
            }
         }
      }
   }

   private final class ControllerPlayer extends EntityRuntime.Player {
      final EntityBundle.ControllerDef def;
      final int depth;
      EntityRuntime.StateRun current;
      EntityRuntime.StateRun previous;
      EntityBundle.Blend fade = EntityBundle.Blend.NONE;
      double fadeStart;
      final List<EntityRuntime.Fade> older = new ArrayList<>(2);

      ControllerPlayer(EntityBundle.ControllerDef def, int depth) {
         this.def = def;
         this.depth = depth;
         this.current = this.enter(def.states().get(def.initialState()));
      }

      private EntityRuntime.StateRun enter(EntityBundle.State state) {
         EntityRuntime.StateRun run = EntityRuntime.this.new StateRun(state, this.depth);
         MolangContext c = EntityRuntime.this.ctx;
         MolangContext.ControllerView saved = c.controller;
         c.controller = run;

         for (MolangExpr s : state.onEntry()) {
            s.exec(c);
         }

         c.controller = saved;
         return run;
      }

      @Override
      void tick(double weight) {
         MolangContext c = EntityRuntime.this.ctx;
         MolangContext.ControllerView saved = c.controller;
         EntityRuntime.this.startParticles(this.def, this.current);
         c.controller = this.current;
         c.animTime = 0.0;

         for (EntityBundle.Transition t : this.current.def.transitions()) {
            if (t.condition().test(c)) {
               EntityBundle.State target = this.def.states().get(t.target());
               if (target != null) {
                  for (MolangExpr s : this.current.def.onExit()) {
                     s.exec(c);
                  }

                  EntityRuntime.this.stopParticles(this.current);
                  EntityBundle.Blend blend = this.current.def.blend();
                  boolean swap = EntityRuntime.this.swapBlendActive();
                  if (blend.seconds() <= 0.0 && target.blend().seconds() > 0.0 && swap) {
                     blend = target.blend();
                  }

                  if (this.previous != null && swap && blend.seconds() > 0.0 && EntityRuntime.this.clock - this.fadeStart < this.fade.seconds()) {
                     this.older.add(new EntityRuntime.Fade(this.previous, this.fade, this.fadeStart));
                  } else if (this.previous != null) {
                     EntityRuntime.this.stopParticles(this.previous);
                     this.dropOlder(this.older.size());
                  }

                  this.previous = blend.seconds() > 0.0 ? this.current : null;
                  this.fade = blend;
                  this.fadeStart = EntityRuntime.this.clock;
                  this.current = this.enter(target);
                  EntityRuntime.this.startParticles(this.def, this.current);
                  List<String> sounds = EntityRuntime.this.bundle.sounds.state(this.def.key(), target.name());
                  if (!sounds.isEmpty()) {
                     EntityRuntime.this.playSounds(sounds, "state " + this.def.key() + "/" + target.name());
                  }
                  break;
               }

               Molang.warn(EntityRuntime.this.mobId + ": controller '" + this.def.key() + "' has no state '" + t.target() + "'");
            }
         }

         c.controller = saved;
         double fadeIn = 1.0;
         if (this.previous != null) {
            double t = EntityRuntime.this.clock - this.fadeStart;
            if (t >= this.fade.seconds()) {
               EntityRuntime.this.stopParticles(this.previous);
               this.previous = null;
               this.dropOlder(this.older.size());
            } else {
               double out = Mth.clamp(this.fade.outgoing(t), 0.0, 1.0);
               fadeIn = 1.0 - out;
               if (this.older.isEmpty()) {
                  this.previous.tick(weight * out);
               } else {
                  this.tickChain(weight, out);
               }
            }
         }

         this.current.tick(weight * fadeIn);
      }

      private void tickChain(double weight, double outNewest) {
         int n = this.older.size();
         double[] outs = new double[n];

         for (int i = 0; i < n; i++) {
            EntityRuntime.Fade f = this.older.get(i);
            double t = EntityRuntime.this.clock - f.start();
            outs[i] = t >= f.blend().seconds() ? 0.0 : Mth.clamp(f.blend().outgoing(t), 0.0, 1.0);
         }

         int expired = -1;

         for (int i = n - 1; i >= 0; i--) {
            if (outs[i] <= 0.0) {
               expired = i;
               break;
            }
         }

         if (expired >= 0) {
            this.dropOlder(expired + 1);
            if (this.older.isEmpty()) {
               this.previous.tick(weight * outNewest);
               return;
            }

            n = this.older.size();
            outs = Arrays.copyOfRange(outs, expired + 1, expired + 1 + n);
         }

         double[] w = new double[n];
         double prod = outNewest;

         for (int k = n - 1; k >= 0; k--) {
            prod *= outs[k];
            w[k] = prod * (k > 0 ? 1.0 - outs[k - 1] : 1.0);
         }

         for (int k = 0; k < n; k++) {
            if (w[k] > 0.0) {
               this.older.get(k).run().tick(weight * w[k]);
            }
         }

         this.previous.tick(weight * outNewest * (1.0 - outs[n - 1]));
      }

      private void dropOlder(int count) {
         for (int i = 0; i < count && !this.older.isEmpty(); i++) {
            EntityRuntime.this.stopParticles(this.older.remove(0).run());
         }
      }
   }

   private record EntryRun(EntityBundle.Entry def, EntityRuntime.Player player) {
   }

   private record Fade(EntityRuntime.StateRun run, EntityBundle.Blend blend, double start) {
   }

   private abstract static class Player {
      abstract void tick(double var1);

      boolean finished() {
         return false;
      }

      boolean isAnimation() {
         return false;
      }
   }

   @FunctionalInterface
   public interface PreAnimationHook {
      void run(Entity var1, EntityRuntime var2, MolangContext var3);
   }

   @FunctionalInterface
   public interface SampleTrace {
      void sampled(EntityRuntime var1, String var2, String var3, float var4, float[] var5, float[] var6);
   }

   private final class StateRun implements MolangContext.ControllerView {
      final EntityBundle.State def;
      final double enteredAt;
      final List<EntityRuntime.EntryRun> entries;
      List<?> particles;

      StateRun(EntityBundle.State def, int depth) {
         this.def = def;
         this.enteredAt = EntityRuntime.this.clock;
         this.entries = EntityRuntime.this.entries(def.animations(), depth);
      }

      @Override
      public double stateTime() {
         return EntityRuntime.this.clock - this.enteredAt;
      }

      @Override
      public boolean anyAnimationFinished() {
         for (EntityRuntime.EntryRun r : this.entries) {
            if (r.player.isAnimation() && r.player.finished()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean allAnimationsFinished() {
         for (EntityRuntime.EntryRun r : this.entries) {
            if (r.player.isAnimation() && !r.player.finished()) {
               return false;
            }
         }

         return true;
      }

      void tick(double weight) {
         MolangContext c = EntityRuntime.this.ctx;
         MolangContext.ControllerView saved = c.controller;
         c.controller = this;

         try {
            EntityRuntime.this.tickEntries(this.entries, weight);
         } finally {
            c.controller = saved;
         }
      }
   }
}
