package com.ans.client.molang;

import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class MolangActorRef {
   private final MolangContext context;
   private final Entity entity;
   private final float partialTick;

   public MolangActorRef(MolangContext context, Entity entity, float partialTick) {
      this.context = context;
      this.entity = entity;
      this.partialTick = partialTick;
   }

   public Entity entity() {
      return this.entity;
   }

   <T> T in(Function<MolangContext, T> body) {
      MolangContext c = this.context;
      Entity savedActor = c.actor;
      LivingEntity savedEntity = c.entity;
      float savedPartial = c.partialTick;
      boolean set = savedActor == null;
      if (set) {
         c.actor = this.entity;
         c.entity = this.entity instanceof LivingEntity l ? l : null;
         c.partialTick = this.partialTick;
      }

      try {
         return body.apply(c);
      } finally {
         if (set) {
            c.actor = savedActor;
            c.entity = savedEntity;
            c.partialTick = savedPartial;
         }
      }
   }

   double eval(Node node) {
      return this.in(node::eval);
   }

   Object value(Node node) {
      return this.in(node::value);
   }

   Object valueOrNull(Node node) {
      return this.in(node::valueOrNull);
   }

   @Override
   public String toString() {
      return "ref(" + this.entity + ")";
   }
}
