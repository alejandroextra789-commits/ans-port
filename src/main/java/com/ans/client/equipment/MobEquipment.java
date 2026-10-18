package com.ans.client.equipment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.world.entity.EntityType;

public final class MobEquipment {
   public static final MobEquipment DEFAULT = new MobEquipment.Builder().build();
   private static final Map<EntityType<?>, MobEquipment> BY_TYPE = new ConcurrentHashMap<>();
   public final String rightItemBone;
   public final String leftItemBone;
   public final String headBone;
   public final String bodyBone;
   public final String rightArmBone;
   public final String leftArmBone;
   public final String rightLegBone;
   public final String leftLegBone;
   public final ArmorModelSet<ModelLayerLocation> armor;
   public final ArmorModelSet<ModelLayerLocation> babyArmor;
   public final boolean headItems;
   public final float itemScale;
   public final boolean mainHandRight;

   private MobEquipment(MobEquipment.Builder b) {
      this.rightItemBone = b.rightItem;
      this.leftItemBone = b.leftItem;
      this.headBone = b.head;
      this.bodyBone = b.body;
      this.rightArmBone = b.rightArm;
      this.leftArmBone = b.leftArm;
      this.rightLegBone = b.rightLeg;
      this.leftLegBone = b.leftLeg;
      this.armor = b.armor;
      this.babyArmor = b.babyArmor;
      this.headItems = b.headItems;
      this.itemScale = b.itemScale;
      this.mainHandRight = b.mainHandRight;
   }

   public static MobEquipment get(EntityType<?> type) {
      return BY_TYPE.getOrDefault(type, DEFAULT);
   }

   public static MobEquipment.Builder builder() {
      return new MobEquipment.Builder();
   }

   public static void humanoid(EntityType<?> type, ArmorModelSet<ModelLayerLocation> armor, ArmorModelSet<ModelLayerLocation> babyArmor) {
      builder().armor(armor, babyArmor).register(type);
   }

   public static final class Builder {
      private String rightItem = "rightitem";
      private String leftItem = "leftitem";
      private String head = "head";
      private String body = "body";
      private String rightArm = "rightarm";
      private String leftArm = "leftarm";
      private String rightLeg = "rightleg";
      private String leftLeg = "leftleg";
      private ArmorModelSet<ModelLayerLocation> armor;
      private ArmorModelSet<ModelLayerLocation> babyArmor;
      private boolean headItems;
      private float itemScale = 1.0F;
      private boolean mainHandRight;

      public MobEquipment.Builder hands(String right, String left) {
         this.rightItem = right;
         this.leftItem = left;
         return this;
      }

      public MobEquipment.Builder bodyBones(String head, String body, String rightArm, String leftArm, String rightLeg, String leftLeg) {
         this.head = head;
         this.body = body;
         this.rightArm = rightArm;
         this.leftArm = leftArm;
         this.rightLeg = rightLeg;
         this.leftLeg = leftLeg;
         return this;
      }

      public MobEquipment.Builder armor(ArmorModelSet<ModelLayerLocation> adult, ArmorModelSet<ModelLayerLocation> baby) {
         this.armor = adult;
         this.babyArmor = baby;
         this.headItems = true;
         return this;
      }

      public MobEquipment.Builder headItems(boolean enabled) {
         this.headItems = enabled;
         return this;
      }

      public MobEquipment.Builder itemScale(float scale) {
         this.itemScale = scale;
         return this;
      }

      public MobEquipment.Builder mainHandAlwaysRight() {
         this.mainHandRight = true;
         return this;
      }

      public MobEquipment build() {
         return new MobEquipment(this);
      }

      public MobEquipment register(EntityType<?> type) {
         MobEquipment e = this.build();
         MobEquipment.BY_TYPE.put(type, e);
         return e;
      }
   }
}
