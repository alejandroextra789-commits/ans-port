package com.ans.client.attachable;

import com.ans.client.molang.AttachableContext;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangQueries;
import java.util.Locale;
import net.minecraft.world.entity.EquipmentSlot;

public final class AttachableQueries {
   private AttachableQueries() {
   }

   public static void install() {
      MolangQueries.simple("is_attached", c -> c.attachable != null && !c.attachable.parentSetup() ? 1.0 : 0.0);
      MolangQueries.simple("is_attached_to_entity", c -> c.attachable != null && !c.attachable.parentSetup() ? 1.0 : 0.0);
      MolangQueries.simple("is_first_person", c -> (c.attachable != null ? !c.attachable.firstPerson() : !c.firstPerson) ? 0.0 : 1.0);
      MolangQueries.register("is_owner_identifier_any", a -> c -> {
         AttachableContext ac = c.attachable;
         if (ac == null) {
            return 0.0;
         }

         for (String s : MolangQueries.strings(a, 0, c)) {
            String id = s.contains(":") ? s : "minecraft:" + s;
            if (id.toLowerCase(Locale.ROOT).equals(ac.ownerResourceLocation())) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("armor_damage_slot", a -> c -> {
         if (c.entity != null && a.length != 0) {
            EquipmentSlot s = switch ((int)a[0].eval(c)) {
               case 0 -> EquipmentSlot.HEAD;
               case 1 -> EquipmentSlot.CHEST;
               case 2 -> EquipmentSlot.LEGS;
               case 3 -> EquipmentSlot.FEET;
               default -> EquipmentSlot.CHEST;
            };
            return c.entity.getItemBySlot(s).getDamageValue();
         } else {
            return 0.0;
         }
      });
      MolangQueries.registerString("item_slot_to_bone_name", a -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            return 0.0;
         }

         @Override
         public Object value(MolangContext c) {
            String slot = a.length > 0 ? MolangQueries.string(a[0], c) : "";
            return Attachables.slotBone(c.actor == null ? null : c.actor.getType(), slot.toLowerCase(Locale.ROOT));
         }
      });
   }
}
