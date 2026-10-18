package com.ans.client.equipment;

import com.ans.Ans;
import com.ans.client.attachable.AttachableRenderer;
import com.ans.client.attachable.Attachables;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.render.MobAnimatable;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoArmorRenderer.ArmorSegment;
import com.geckolib.renderer.GeoArmorRenderer.RenderData;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.RenderPassInfo.BoneUpdater;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.service.GeckoLibClient;
import com.geckolib.util.RenderStateUtil;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.Vector3f;

public final class EquipmentLayer<E extends LivingEntity> extends GeoRenderLayer<MobAnimatable, E, LivingEntityRenderState> {
   public static final DataTicket<EquipmentLayer.Data> DATA = DataTicket.create("ans_equipment", EquipmentLayer.Data.class);
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final float ITEM_Y = 0.1875F;
   private static final float ITEM_Z = -0.1875F;
   private static final EquipmentLayer.Data NONE = new EquipmentLayer.Data(null, null, null, new ItemStack[0], false, 0, null);
   private final MobEquipment config;
   private final ItemModelResolver itemModels;
   private final EquipmentLayerRenderer equipmentRenderer;
   private final Map<EquipmentSlot, List<EquipmentLayer.Segment>> armor;
   private final Map<EquipmentSlot, List<EquipmentLayer.Segment>> babyArmor;
   private static final Set<Item> GEO_FAILED = ConcurrentHashMap.newKeySet();

   private static float[] firstBox(ModelPart part) {
      float[] out = new float[6];
      boolean[] found = new boolean[]{false};
      part.visit(new PoseStack(), (var2x, var3, var4, cube) -> {
         if (!found[0]) {
            found[0] = true;
            out[0] = Math.min(cube.minX, cube.maxX);
            out[1] = Math.min(cube.minY, cube.maxY);
            out[2] = Math.min(cube.minZ, cube.maxZ);
            out[3] = Math.max(cube.minX, cube.maxX);
            out[4] = Math.max(cube.minY, cube.maxY);
            out[5] = Math.max(cube.minZ, cube.maxZ);
         }
      });
      return out;
   }

   public EquipmentLayer(GeoRenderer<MobAnimatable, E, LivingEntityRenderState> renderer, Context context, MobEquipment config) {
      super(renderer);
      this.config = config;
      this.itemModels = context.getItemModelResolver();
      this.equipmentRenderer = context.getEquipmentRenderer();
      this.armor = config.armor == null ? null : this.segments(ArmorModelSet.bake(config.armor, context.getModelSet(), HumanoidModel::new));
      this.babyArmor = config.babyArmor == null ? null : this.segments(ArmorModelSet.bake(config.babyArmor, context.getModelSet(), HumanoidModel::new));
   }

   public EquipmentLayerRenderer equipmentRenderer() {
      return this.equipmentRenderer;
   }

   private Map<EquipmentSlot, List<EquipmentLayer.Segment>> segments(ArmorModelSet<HumanoidModel<HumanoidRenderState>> set) {
      MobEquipment c = this.config;
      Map<EquipmentSlot, List<EquipmentLayer.Segment>> out = new EnumMap<>(EquipmentSlot.class);
      HumanoidModel<?> head = (HumanoidModel<?>)set.head();
      HumanoidModel<?> chest = (HumanoidModel<?>)set.chest();
      HumanoidModel<?> legs = (HumanoidModel<?>)set.legs();
      HumanoidModel<?> feet = (HumanoidModel<?>)set.feet();
      out.put(EquipmentSlot.HEAD, List.of(new EquipmentLayer.Segment(c.headBone, head.head)));
      out.put(
         EquipmentSlot.CHEST,
         List.of(
            new EquipmentLayer.Segment(c.bodyBone, chest.body),
            new EquipmentLayer.Segment(c.rightArmBone, chest.rightArm),
            new EquipmentLayer.Segment(c.leftArmBone, chest.leftArm)
         )
      );
      out.put(
         EquipmentSlot.LEGS,
         List.of(
            new EquipmentLayer.Segment(c.bodyBone, legs.body),
            new EquipmentLayer.Segment(c.rightLegBone, legs.rightLeg),
            new EquipmentLayer.Segment(c.leftLegBone, legs.leftLeg)
         )
      );
      out.put(EquipmentSlot.FEET, List.of(new EquipmentLayer.Segment(c.rightLegBone, feet.rightLeg), new EquipmentLayer.Segment(c.leftLegBone, feet.leftLeg)));
      return out;
   }

   public void addRenderData(MobAnimatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
      if (entity != null) {
         EnumSet<EquipmentSlot> attached = this.attachables(entity, renderState, partialTick);
         boolean leftHanded = !this.config.mainHandRight && entity.getMainArm() == HumanoidArm.LEFT;
         ItemStack main = attached.contains(EquipmentSlot.MAINHAND) ? ItemStack.EMPTY : entity.getMainHandItem();
         ItemStack off = attached.contains(EquipmentSlot.OFFHAND) ? ItemStack.EMPTY : entity.getOffhandItem();
         ItemStackRenderState right = this.item(entity, leftHanded ? off : main, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         ItemStackRenderState left = this.item(entity, leftHanded ? main : off, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         ItemStack[] worn = new ItemStack[ARMOR_SLOTS.length];
         EquipmentLayer.GeoArmor[] geo = null;
         ItemStackRenderState head = null;
         boolean any = right != null || left != null;
         if (this.armor != null || this.config.headItems) {
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
               ItemStack s = attached.contains(ARMOR_SLOTS[i]) ? ItemStack.EMPTY : entity.getItemBySlot(ARMOR_SLOTS[i]);
               worn[i] = ItemStack.EMPTY;
               if (!s.isEmpty()) {
                  if (HumanoidArmorLayer.shouldRender(s, ARMOR_SLOTS[i])) {
                     if (this.armor != null) {
                        EquipmentLayer.GeoArmor g = geoArmor(entity, s, ARMOR_SLOTS[i], renderState, partialTick);
                        if (g != null) {
                           if (geo == null) {
                              geo = new EquipmentLayer.GeoArmor[ARMOR_SLOTS.length];
                           }

                           geo[i] = g;
                        } else {
                           worn[i] = s;
                        }

                        any = true;
                     }
                  } else if (ARMOR_SLOTS[i] == EquipmentSlot.HEAD && this.config.headItems) {
                     head = this.item(entity, s, ItemDisplayContext.HEAD);
                     any |= head != null;
                  }
               }
            }
         }

         if (any) {
            renderState.addGeckolibData(DATA, new EquipmentLayer.Data(right, left, head, worn, entity.isBaby(), renderState.outlineColor, geo));
         } else if (renderState.hasGeckolibData(DATA)) {
            renderState.addGeckolibData(DATA, NONE);
         }
      }
   }

   private static EquipmentLayer.GeoArmor geoArmor(
      LivingEntity entity, ItemStack stack, EquipmentSlot slot, LivingEntityRenderState base, float partialTick
   ) {
      GeoRenderProvider provider = GeoRenderProvider.of(stack);
      if (provider != GeoRenderProvider.DEFAULT && !GEO_FAILED.contains(stack.getItem())) {
         try {
            GeoArmorRenderer renderer = provider.getGeoArmorRenderer(stack, slot);
            if (renderer == null) {
               return null;
            }

            HumanoidRenderState state = RenderStateUtil.makeMinimalArmorRenderingClone(new HumanoidRenderState(), base);
            HumanoidModel<?> baseModel = (HumanoidModel<?>)((ArmorModelSet)GeckoLibClient.HUMANOID_ARMOR_MODEL.get()).get(slot);
            renderer.fillRenderState((GeoItem)stack.getItem(), new RenderData(stack, slot, entity, baseModel), state, partialTick);
            return new EquipmentLayer.GeoArmor(renderer, state);
         } catch (RuntimeException e) {
            if (GEO_FAILED.add(stack.getItem())) {
               Ans.LOGGER.error("[ans] GeckoLib armour of {} failed; drawing the equipment model", stack, e);
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private EnumSet<EquipmentSlot> attachables(E entity, LivingEntityRenderState renderState, float partialTick) {
      EnumSet<EquipmentSlot> covered = EnumSet.noneOf((Class<E>)EquipmentSlot.class);
      FrameResult holder = (FrameResult)renderState.getGeckolibData(FrameResult.TICKET);
      List<Attachables.Frame> frames = new ArrayList<>();
      if (holder != null && Attachables.ENABLED) {
         for (EquipmentSlot slot : Attachables.slots(entity)) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
               Attachables.Frame f;
               try {
                  f = Attachables.tick(entity, slot, stack, partialTick, renderState, false);
               } catch (RuntimeException e) {
                  Ans.LOGGER.error("[ans] attachable of {} in {} failed; Java's model is drawn", new Object[]{stack, slot, e});
                  f = null;
               }

               if (f != null) {
                  AttachableRenderer.resolveRelative(f, holder);
                  Attachables.debugAnchors(entity, f, holder);
                  frames.add(f);
                  covered.add((E)slot);
               }
            }
         }
      }

      if (!frames.isEmpty() || renderState.hasGeckolibData(Attachables.FrameList.TICKET)) {
         renderState.addGeckolibData(Attachables.FrameList.TICKET, new Attachables.FrameList(List.copyOf(frames)));
      }

      return covered;
   }

   private ItemStackRenderState item(LivingEntity entity, ItemStack stack, ItemDisplayContext context) {
      if (stack.isEmpty()) {
         return null;
      }

      ItemStackRenderState state = new ItemStackRenderState();
      this.itemModels.updateForLiving(state, stack, context, entity);
      return state.isEmpty() ? null : state;
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      EquipmentLayer.Data data = (EquipmentLayer.Data)info.getGeckolibData(DATA);
      FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
      Attachables.FrameList attached = (Attachables.FrameList)info.getGeckolibData(Attachables.FrameList.TICKET);
      if (attached != null && frame != null && !attached.frames().isEmpty()) {
         PoseStack aps = info.poseStack();
         aps.pushPose();
         aps.last().set(info.getModelRenderMatrixPose());
         AttachableRenderer.submit(this.getGeoModel(), info, collector, frame, attached.frames());
         aps.popPose();
      }

      if (data != null && frame != null) {
         int light = info.packedLight();
         PoseStack ps = info.poseStack();
         float eq = data.baby && frame.scaleY() > 0.0F ? 1.0F / frame.scaleY() : 1.0F;
         if (data.right != null) {
            this.heldItem(info, collector, frame, this.config.rightItemBone, data.right, light, data.outline, eq);
         }

         if (data.left != null) {
            this.heldItem(info, collector, frame, this.config.leftItemBone, data.left, light, data.outline, eq);
         }

         Map<EquipmentSlot, List<EquipmentLayer.Segment>> segments = data.baby && this.babyArmor != null ? this.babyArmor : this.armor;
         if (segments != null) {
            LayerType babyType = data.baby && this.babyArmor != null ? LayerType.HUMANOID_BABY : null;

            for (int i = 0; i < data.armor.length; i++) {
               ItemStack stack = data.armor[i];
               if (stack != null && !stack.isEmpty()) {
                  Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
                  Optional<ResourceKey<EquipmentAsset>> asset = equippable == null ? Optional.empty() : equippable.assetId();
                  if (!asset.isEmpty()) {
                     EquipmentSlot slot = ARMOR_SLOTS[i];
                     LayerType type = babyType != null ? babyType : (slot == EquipmentSlot.LEGS ? LayerType.HUMANOID_LEGGINGS : LayerType.HUMANOID);

                     for (EquipmentLayer.Segment seg : segments.get(slot)) {
                        GeoBone bone = this.bone(frame, seg.bone);
                        if (bone != null) {
                           ps.pushPose();
                           ps.last().set(info.getModelRenderMatrixPose());
                           toBone(ps, bone, frame.pose());
                           ps.scale(-1.0F, -1.0F, 1.0F);
                           if (data.baby) {
                              fit(ps, bone, seg.box);
                           }

                           this.equipmentRenderer
                              .renderLayers(
                                 type, asset.get(), seg.model, (LivingEntityRenderState)info.renderState(), stack, ps, collector, light, data.outline
                              );
                           ps.popPose();
                        }
                     }
                  }
               }
            }

            if (data.geo != null) {
               for (int i = 0; i < data.geo.length; i++) {
                  if (data.geo[i] != null) {
                     this.geoArmor(info, collector, frame, data.geo[i], ARMOR_SLOTS[i], segments.get(ARMOR_SLOTS[i]), data, light);
                  }
               }
            }
         }

         if (data.head != null) {
            GeoBone bone = this.bone(frame, this.config.headBone);
            if (bone != null) {
               ps.pushPose();
               ps.last().set(info.getModelRenderMatrixPose());
               toBone(ps, bone, frame.pose());
               ps.scale(-eq, -eq, eq);
               ps.translate(0.0F, -0.25F, 0.0F);
               ps.mulPose(Axis.YP.rotationDegrees(180.0F));
               ps.scale(0.625F, -0.625F, -0.625F);
               data.head.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, data.outline);
               ps.popPose();
            }
         }
      }
   }

   private void geoArmor(
      RenderPassInfo<LivingEntityRenderState> info,
      SubmitNodeCollector collector,
      FrameResult frame,
      EquipmentLayer.GeoArmor g,
      EquipmentSlot slot,
      List<EquipmentLayer.Segment> vanillaSegments,
      EquipmentLayer.Data data,
      int light
   ) {
      GeoArmorRenderer renderer = g.renderer();
      HumanoidRenderState state = g.state();
      state.lightCoords = light;
      state.outlineColor = data.outline;
      List<ArmorSegment> parts = renderer.getSegmentsForSlot(state, slot);

      for (ArmorSegment part : parts) {
         String boneName = switch (part) {
            case HEAD -> this.config.headBone;
            case CHEST -> this.config.bodyBone;
            case LEFT_ARM -> this.config.leftArmBone;
            case RIGHT_ARM -> this.config.rightArmBone;
            case LEFT_LEG, LEFT_FOOT -> this.config.leftLegBone;
            case RIGHT_LEG, RIGHT_FOOT -> this.config.rightLegBone;
            default -> throw new IllegalStateException("Unexpected enum value");
         };
         GeoBone bone = this.bone(frame, boneName);
         if (bone != null) {
            PoseStack ps = info.poseStack();
            ps.pushPose();
            ps.last().set(info.getModelRenderMatrixPose());
            toBone(ps, bone, frame.pose());
            ps.scale(-1.0F, -1.0F, 1.0F);
            if (data.baby && vanillaSegments != null) {
               for (EquipmentLayer.Segment seg : vanillaSegments) {
                  if (seg.bone.equals(boneName)) {
                     fit(ps, bone, seg.box);
                     break;
                  }
               }
            }

            BoneUpdater<HumanoidRenderState> place = (pass, snapshots) -> {
               for (ArmorSegment p : parts) {
                  snapshots.get(renderer.getBoneNameForSegment(state, p)).ifPresent(s -> {
                     if (p != part) {
                        s.skipRender(true).skipChildrenRender(true);
                     } else {
                        Vector3f t = p.modelPartMatcher.apply(new Vector3f());
                        s.setRotX(0.0F).setRotY(0.0F).setRotZ(0.0F).setTranslateX(t.x).setTranslateY(t.y).setTranslateZ(t.z);
                     }
                  });
               }
            };
            renderer.performRenderPass(state, ps, collector, info.cameraState(), List.of(place));
            ps.popPose();
         }
      }
   }

   private void heldItem(
      RenderPassInfo<LivingEntityRenderState> info,
      SubmitNodeCollector collector,
      FrameResult frame,
      String boneName,
      ItemStackRenderState item,
      int light,
      int outline,
      float eq
   ) {
      GeoBone bone = this.bone(frame, boneName);
      if (bone != null) {
         PoseStack ps = info.poseStack();
         ps.pushPose();
         ps.last().set(info.getModelRenderMatrixPose());
         toBone(ps, bone, frame.pose());
         if (eq != 1.0F) {
            ps.scale(eq, eq, eq);
         }

         ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
         ps.translate(0.0F, 0.1875F, -0.1875F);
         if (this.config.itemScale != 1.0F) {
            ps.scale(this.config.itemScale, this.config.itemScale, this.config.itemScale);
         }

         item.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, outline);
         ps.popPose();
      }
   }

   private static void fit(PoseStack ps, GeoBone bone, float[] box) {
      if (bone instanceof CuboidGeoBone c && c.cubes.length != 0) {
         float[] b = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

         for (GeoQuad q : c.cubes[0].quads()) {
            for (GeoVertex v : q.vertices()) {
               float x = -(v.posX() * 16.0F - bone.pivotX());
               float y = -(v.posY() * 16.0F - bone.pivotY());
               float z = v.posZ() * 16.0F - bone.pivotZ();
               b[0] = Math.min(b[0], x);
               b[1] = Math.min(b[1], y);
               b[2] = Math.min(b[2], z);
               b[3] = Math.max(b[3], x);
               b[4] = Math.max(b[4], y);
               b[5] = Math.max(b[5], z);
            }
         }

         float sx = (b[3] - b[0]) / (box[3] - box[0]);
         float sy = (b[4] - b[1]) / (box[4] - box[1]);
         float sz = (b[5] - b[2]) / (box[5] - box[2]);
         if (sx > 0.0F && sy > 0.0F && sz > 0.0F && Float.isFinite(sx + sy + sz)) {
            ps.translate((b[0] + b[3]) / 32.0F, (b[1] + b[4]) / 32.0F, (b[2] + b[5]) / 32.0F);
            ps.scale(sx, sy, sz);
            ps.translate(-(box[0] + box[3]) / 32.0F, -(box[1] + box[4]) / 32.0F, -(box[2] + box[5]) / 32.0F);
         }
      }
   }

   private GeoBone bone(FrameResult frame, String name) {
      for (RenderLayer layer : frame.layers()) {
         BakedGeoModel model = this.getGeoModel().getBakedModel(layer.model());
         if (!model.isMissingno()) {
            Optional<GeoBone> bone = model.getBone(name);
            if (bone.isPresent()) {
               return bone.get();
            }
         }
      }

      return null;
   }

   public static void toBone(PoseStack ps, GeoBone bone, MobPose pose) {
      List<BoneSnapshot> applied = new ArrayList<>();

      try {
         for (GeoBone b = bone; b != null; b = b.parent()) {
            BoneSnapshot s = BoneSnapshot.create(b);
            float[] v = pose.get(b.name());
            if (v != null) {
               s.setRotation(finite(v[0], 0.0F), finite(v[1], 0.0F), finite(v[2], 0.0F));
               s.setTranslation(finite(v[3], 0.0F), finite(v[4], 0.0F), finite(v[5], 0.0F));
               s.setScale(finite(v[6], 1.0F), finite(v[7], 1.0F), finite(v[8], 1.0F));
            }

            s.apply();
            applied.add(s);
         }

         RenderUtil.transformToBone(ps, bone);
      } finally {
         for (BoneSnapshot s : applied) {
            s.cleanup();
         }
      }
   }

   private static float finite(float f, float fallback) {
      return Float.isFinite(f) ? f : fallback;
   }

   public record Data(
      ItemStackRenderState right,
      ItemStackRenderState left,
      ItemStackRenderState head,
      ItemStack[] armor,
      boolean baby,
      int outline,
      EquipmentLayer.GeoArmor [] geo
   ) {
   }

   public record GeoArmor(GeoArmorRenderer<?, ?> renderer, HumanoidRenderState state) {
   }

   private static final class PartModel extends Model<Object> {
      PartModel(ModelPart part) {
         super(part, RenderTypes::armorCutoutNoCull);
      }

      public void setupAnim(Object state) {
         this.resetPose();
         this.root.x = this.root.y = this.root.z = 0.0F;
         this.root.xRot = this.root.yRot = this.root.zRot = 0.0F;
         this.root.visible = true;
      }
   }

   private record Segment(String bone, EquipmentLayer.PartModel model, float[] box) {
      Segment(String bone, ModelPart part) {
         this(bone, new EquipmentLayer.PartModel(part), EquipmentLayer.firstBox(part));
      }
   }
}
