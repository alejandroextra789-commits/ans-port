package com.ans.client.molang;


public record AttachableContext(String itemSlot, MolangActorRef owner, String ownerResourceLocation, boolean firstPerson, boolean parentSetup) {
   public Object value(String name) {
      return switch (name) {
         case "item_slot" -> this.itemSlot;
         case "owning_entity" -> this.owner;
         case "is_first_person" -> this.firstPerson ? 1.0 : 0.0;
         default -> null;
      };
   }
}
