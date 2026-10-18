package com.ans.client.attachable;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.molang.AttachableContext;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangActorRef;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangExpr;
import com.ans.client.molang.MolangScope;
import com.ans.client.molang.MolangVars;
import com.ans.client.render.UiRender;
import com.ans.client.settings.Experience;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeableLeatherItem;

public final class Attachables {
   public static final ResourceLocation INDEX = Ans.id("attachable/_index.json");
   public static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ans.attachables"));
   private static final Set<String> EXCLUDED = "false".equalsIgnoreCase(System.getProperty("ans.packElytra")) ? Set.of("minecraft:elytra") : Set.of();
   public static final Set<String> DEV_JAVA_ITEMS = ConcurrentHashMap.newKeySet();
   static final boolean DEBUG = Boolean.getBoolean("ans.debugAttachables") || Files.exists(Path.of("ans_debug_attachables.txt"));
   private static final Set<String> DEBUG_SEEN = ConcurrentHashMap.newKeySet();
   public static final Map<EquipmentSlot, String> SLOT_NAMES = Map.of(
      EquipmentSlot.MAINHAND,
      "main_hand",
      EquipmentSlot.OFFHAND,
      "off_hand",
      EquipmentSlot.HEAD,
      "head",
      EquipmentSlot.CHEST,
      "chest",
      EquipmentSlot.LEGS,
      "legs",
      EquipmentSlot.FEET,
      "feet"
   );
   private static final EquipmentSlot[] HAND_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
   private static final EquipmentSlot[] HAND_AND_HEAD_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD};
   private static final EquipmentSlot[] ALL_SLOTS = new EquipmentSlot[]{
      EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };
   private static Map<String, List<Attachables.Candidate>> index = Map.of();
   private static final Map<ResourceLocation, Optional<AttachableBundle>> BUNDLES = new HashMap<>();
   private static int seenGeneration = -1;
   private static String seenExperience = "";
   private static final MolangContext SCRATCH = new MolangContext(new MolangScope());
   private static final int V_IS_ENCHANTED = MolangVars.id("is_enchanted");
   private static final int V_HAS_TRIM = MolangVars.id("has_trim");
   private static final int V_TRIM_PATH = MolangVars.id("trim_path");
   private static final int V_IS_PAPERDOLL = MolangVars.id("is_paperdoll");
   private static final Map<Entity, Attachables.State> STATES = new WeakHashMap<>();
   private static final Pattern JAVA_LEATHER = Pattern.compile("textures/entity/equipment/([a-z_]+)/leather\\.png");
   private static final Map<ResourceLocation, Optional<ResourceLocation>> LEATHER_OVERLAYS = new ConcurrentHashMap<>();
   private static int leatherGeneration = -1;

   private Attachables() {
   }

   private static synchronized void ensureLoaded() {
      int generation = AnimationLookup.generation();
      String experience = Experience.current();
      if (generation != seenGeneration || !experience.equals(seenExperience)) {
         seenGeneration = generation;
         seenExperience = experience;
         BUNDLES.clear();
         STATES.clear();
         Map<String, List<Attachables.Candidate>> out = new HashMap<>();
         ResourceLocation indexFile = Experience.resolve(INDEX);
         Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(indexFile);
         if (res.isEmpty()) {
            Ans.LOGGER.warn("[ans] {} not found: run the converter (attachables.py); Java item/armour models are used", INDEX);
            index = out;
         } else {
            try (Reader reader = res.get().openAsReader()) {
               JsonObject items = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("items");

               for (Entry<String, JsonElement> e : items.entrySet()) {
                  if (!EXCLUDED.contains(e.getKey())) {
                     List<Attachables.Candidate> list = new ArrayList<>();

                     for (JsonElement c : e.getValue().getAsJsonArray()) {
                        JsonArray a = c.getAsJsonArray();
                        ResourceLocation id = ResourceLocation.tryParse(a.get(0).getAsString());
                        if (id != null) {
                           list.add(new Attachables.Candidate(id, a.size() > 1 && !a.get(1).isJsonNull() ? Molang.compile(a.get(1).getAsString()) : null));
                        }
                     }

                     out.put(e.getKey(), List.copyOf(list));
                  }
               }

               Ans.LOGGER.info("[ans] Attachables: {} Java items have a pack attachable", out.size());
            } catch (Exception ex) {
               Ans.LOGGER.error("[ans] Could not read {}", INDEX, ex);
            }

            index = out;
         }
      }
   }

   private static synchronized AttachableBundle bundle(ResourceLocation id) {
      return BUNDLES.computeIfAbsent(id, Attachables::load).orElse(null);
   }

   private static Optional<AttachableBundle> load(ResourceLocation id) {
      ResourceLocation file = id.withPath(p -> p + ".json");
      Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(file);
      if (res.isEmpty()) {
         Ans.LOGGER.warn("[ans] attachable bundle {} not found", file);
         return Optional.empty();
      }

      try (Reader reader = res.get().openAsReader()) {
         return Optional.of(AttachableBundle.parse(id, JsonParser.parseReader(reader).getAsJsonObject()));
      } catch (Exception e) {
         Ans.LOGGER.error("[ans] Could not read attachable bundle {}", file, e);
         return Optional.empty();
      }
   }

   public static String ownerResourceLocation(Entity owner, EntityRuntime runtime) {
      return runtime != null ? "minecraft:" + runtime.mobId() : BuiltInRegistries.ENTITY_TYPE.getKey(owner.getType()).toString();
   }

   public static AttachableBundle select(ItemStack stack, String slot, LivingEntity owner, String ownerId, float partialTick) {
      if (ENABLED && !stack.isEmpty()) {
         ensureLoaded();
         String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         List<Attachables.Candidate> list = index.get(itemId);
         if (list != null && (DEV_JAVA_ITEMS.isEmpty() || !DEV_JAVA_ITEMS.contains(itemId))) {
            Iterator var7 = list.iterator();

            AttachableBundle b;
            do {
               Attachables.Candidate c;
               boolean ok;
               do {
                  if (!var7.hasNext()) {
                     return null;
                  }

                  c = (Attachables.Candidate)var7.next();
                  if (c.condition == null) {
                     break;
                  }

                  MolangContext s = SCRATCH;
                  s.actor = owner;
                  s.entity = owner;
                  s.partialTick = partialTick;
                  s.attachable = new AttachableContext(slot, null, ownerId, false, false);

                  try {
                     ok = c.condition.test(s);
                  } finally {
                     s.actor = null;
                     s.entity = null;
                     s.attachable = null;
                  }
               } while (!ok);

               b = bundle(c.bundle);
            } while (b == null || !b.drawable() || b.wornOnly() && isHand(slot) || !b.itemAux.isEmpty() && !isHand(slot));

            return b;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean isHand(String slot) {
      return "main_hand".equals(slot) || "off_hand".equals(slot);
   }

   static boolean wearsArmor(LivingEntity owner) {
      return owner instanceof Player || owner instanceof ArmorStand || MobEquipment.get(owner.getType()).armor != null;
   }

   static synchronized Attachables.State state(Entity entity) {
      return STATES.computeIfAbsent(entity, var0x -> new Attachables.State());
   }

   static EntityRuntime ownerRuntime(Entity entity) {
      Attachables.State s;
      synchronized (Attachables.class) {
         s = STATES.get(entity);
      }

      return s == null ? null : s.owner;
   }

   public static EquipmentSlot[] slots(LivingEntity owner) {
      if (wearsArmor(owner)) {
         return ALL_SLOTS;
      } else {
         return MobEquipment.get(owner.getType()).headItems ? HAND_AND_HEAD_SLOTS : HAND_SLOTS;
      }
   }

   public static void installHook() {
      VanillaItemTextures.register();
      EntityRuntime.addPreAnimationHook((entity, runtime, ctx) -> {
         if (ENABLED && entity instanceof LivingEntity living) {
            Attachables.State st = state(entity);
            boolean fp = runtime.isFirstPerson();
            if (fp) {
               st.fpOwner = runtime;
            } else {
               st.owner = runtime;
            }

            String ownerId = ownerResourceLocation(entity, runtime);

            for (EquipmentSlot slot : slots(living)) {
               ItemStack stack = holderStack(living, slot);
               if (!stack.isEmpty()) {
                  String name = SLOT_NAMES.get(slot);
                  AttachableBundle b = select(stack, name, living, ownerId, ctx.partialTick);
                  if (b != null && slot == EquipmentSlot.MAINHAND && b.auxVariable != null) {
                     Integer aux = b.itemAux.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                     if (aux != null) {
                        runtime.variables().set(MolangVars.id(b.auxVariable), aux.intValue());
                     }
                  }

                  if (b != null && b.parentSetup != null) {
                     AttachableContext saved = ctx.attachable;
                     ctx.attachable = new AttachableContext(name, null, ownerId, fp, true);

                     try {
                        b.parentSetup.exec(ctx);
                     } finally {
                        ctx.attachable = saved;
                     }
                  }
               }
            }
         }
      });
   }

   static ItemStack holderStack(LivingEntity owner, EquipmentSlot slot) {
      return owner.getItemBySlot(slot);
   }

   public static Attachables.Frame tick(
      LivingEntity owner, EquipmentSlot slot, ItemStack stack, float partialTick, GeoRenderState renderState, boolean firstPerson
   ) {
      if (ENABLED && !stack.isEmpty()) {
         Attachables.State st = state(owner);
         EntityRuntime ownerRuntime = firstPerson && st.fpOwner != null ? st.fpOwner : st.owner;
         String name = SLOT_NAMES.get(slot);
         String ownerId = ownerResourceLocation(owner, ownerRuntime);
         AttachableBundle b = select(stack, name, owner, ownerId, partialTick);
         boolean ui = !firstPerson && owner instanceof Player && UiRender.active();
         String key = name + (firstPerson ? "#fp" : (ui ? "#ui" : ""));
         if (b == null) {
            st.slots.remove(key);
            return null;
         }

         Attachables.SlotRun run = st.slots.get(key);
         if (run == null || run.bundle != b || run.runtime.generation() != AnimationLookup.generation()) {
            run = new Attachables.SlotRun(b);
            run.runtime.ui(ui);
            st.slots.put(key, run);
         }

         MolangContext c = run.runtime.context();
         MolangActorRef ref = ownerRuntime == null ? null : new MolangActorRef(ownerRuntime.context(), owner, partialTick);
         c.attachable = new AttachableContext(name, ref, ownerId, firstPerson, false);
         MolangScope vars = run.runtime.variables();
         vars.set(V_IS_ENCHANTED, stack.hasFoil() ? 1.0 : 0.0);
         ResourceLocation trim = slot.getType() == Type.HUMANOID_ARMOR ? TrimTextures.texture(stack, slot, b.bundle.textures.get("default")) : null;
         vars.set(V_HAS_TRIM, trim != null ? 1.0 : 0.0);
         if (trim != null) {
            vars.set(V_TRIM_PATH, trim.toString());
         }

         vars.set(V_IS_PAPERDOLL, ui ? 1.0 : 0.0);
         FrameResult frame = leatherOverlay(itemColors(run.runtime.tick(owner, partialTick, renderState), stack));
         Map<ResourceLocation, Map<String, String>> binds = new HashMap<>();

         for (RenderLayer layer : frame.layers()) {
            if (!binds.containsKey(layer.model())) {
               Map<String, MolangExpr> exprs = b.bindings.get(layer.model());
               Map<String, String> out = new HashMap<>();
               if (exprs != null) {
                  c.actor = owner;
                  c.entity = owner;
                  c.partialTick = partialTick;
                  MolangContext previous = c.enter();

                  try {
                     for (Entry<String, MolangExpr> e : exprs.entrySet()) {
                        if (e.getValue().value(c) instanceof String s && !s.isEmpty()) {
                           out.put(e.getKey(), s.toLowerCase(Locale.ROOT));
                        }
                     }
                  } finally {
                     MolangContext.restore(previous);
                     c.actor = null;
                     c.entity = null;
                  }
               }

               binds.put(layer.model(), out);
            }
         }

         if (DEBUG && DEBUG_SEEN.add(owner.getId() + ":" + key + ":" + b.identifier + ":" + System.currentTimeMillis() / 3000L)) {
            Ans.LOGGER
               .info(
                  "[ans-attach] {} #{} {} -> {} layers={} binds={} state={} pose={}",
                  new Object[]{
                     ownerId,
                     owner.getId(),
                     key,
                     b.identifier,
                     frame.layers().stream().map(l -> l.model().getPath() + "|" + l.texture().getPath()).toList(),
                     binds,
                     run.runtime.describe(false),
                     frame.pose()
                  }
               );
         }

         return new Attachables.Frame(name, slot, b, run.runtime, frame, binds);
      } else {
         return null;
      }
   }

   public static void debugAnchors(LivingEntity owner, Attachables.Frame f, FrameResult holder) {
      if (DEBUG && DEBUG_SEEN.add("anchor:" + owner.getId() + ":" + f.slot() + ":" + f.bundle().identifier + ":" + System.currentTimeMillis() / 3000L)) {
         Ans.LOGGER.info("[ans-attach] anchors #{} {}:{}", new Object[]{owner.getId(), f.slot(), AttachableRenderer.describeAnchors(f, holder)});
      }
   }

   private static FrameResult itemColors(FrameResult frame, ItemStack stack) {
      boolean masked = false;

      for (RenderLayer l : frame.layers()) {
         masked |= l.changeColor() != 0;
      }

      if (!masked) {
         return frame;
      }

      int color = stack.getItem() instanceof DyeableLeatherItem leather
         ? leather.getColor(stack) | 0xFF000000
         : -1;
      List<RenderLayer> out = new ArrayList<>(frame.layers().size());

      for (RenderLayer l : frame.layers()) {
         out.add(
            l.changeColor() == 0
               ? l
               : new RenderLayer(
                  l.model(),
                  l.texture(),
                  l.materials(),
                  l.visibility(),
                  l.tint(),
                  l.hurtOverlay(),
                  l.ignoreLighting(),
                  l.uvOffsetU(),
                  l.uvOffsetV(),
                  color,
                  l.overlay(),
                  l.uvScaleU(),
                  l.uvScaleV()
               )
         );
      }

      return new FrameResult(frame.pose(), List.copyOf(out), frame.scaleX(), frame.scaleY(), frame.scaleZ());
   }

   private static FrameResult leatherOverlay(FrameResult frame) {
      int generation = AnimationLookup.generation();
      if (generation != leatherGeneration) {
         leatherGeneration = generation;
         LEATHER_OVERLAYS.clear();
      }

      List<RenderLayer> out = null;

      for (int i = 0; i < frame.layers().size(); i++) {
         RenderLayer l = frame.layers().get(i);
         ResourceLocation overlay = "minecraft".equals(l.texture().getNamespace()) && JAVA_LEATHER.matcher(l.texture().getPath()).matches()
            ? LEATHER_OVERLAYS.computeIfAbsent(l.texture(), t -> {
               ResourceLocation o = t.withPath(t.getPath().replace("/leather.png", "/leather_overlay.png"));
               return Minecraft.getInstance().getResourceManager().getResource(o).isPresent() ? Optional.of(o) : Optional.empty();
            }).orElse(null)
            : null;
         if (overlay == null) {
            if (out != null) {
               out.add(l);
            }
         } else {
            if (out == null) {
               out = new ArrayList<>(frame.layers().subList(0, i));
            }

            out.add(l);
            List<RenderLayer.MaterialRule> plain = new ArrayList<>(l.materials().size());

            for (RenderLayer.MaterialRule m : l.materials()) {
               plain.add(new RenderLayer.MaterialRule(m.pattern(), m.kind(), false, m.uvAnim(), false));
            }

            out.add(
               new RenderLayer(
                  l.model(),
                  overlay,
                  List.copyOf(plain),
                  l.visibility(),
                  l.tint(),
                  l.hurtOverlay(),
                  l.ignoreLighting(),
                  l.uvOffsetU(),
                  l.uvOffsetV(),
                  0,
                  l.overlay(),
                  l.uvScaleU(),
                  l.uvScaleV()
               )
            );
         }
      }

      return out == null ? frame : new FrameResult(frame.pose(), List.copyOf(out), frame.scaleX(), frame.scaleY(), frame.scaleZ());
   }

   public static String slotBone(EntityType<?> ownerType, String slot) {
      MobEquipment cfg = ownerType == null ? MobEquipment.DEFAULT : MobEquipment.get(ownerType);

      return switch (slot) {
         case "main_hand" -> cfg.rightItemBone;
         case "off_hand" -> cfg.leftItemBone;
         case "head" -> cfg.headBone;
         case "chest", "torso" -> cfg.bodyBone;
         case "legs" -> cfg.bodyBone;
         case "feet" -> cfg.rightLegBone;
         default -> slot;
      };
   }

   private record Candidate(ResourceLocation bundle, MolangExpr condition) {
   }

   public record Frame(
      String slot, EquipmentSlot javaSlot, AttachableBundle bundle, EntityRuntime runtime, FrameResult frame, Map<ResourceLocation, Map<String, String>> bindings
   ) {
   }

   public record FrameList(List<Attachables.Frame> frames) {
      public static final DataTicket<Attachables.FrameList> TICKET = DataTicket.create("ans_attachables", Attachables.FrameList.class);
   }

   static final class SlotRun {
      final AttachableBundle bundle;
      final EntityRuntime runtime;

      SlotRun(AttachableBundle bundle) {
         this.bundle = bundle;
         this.runtime = new EntityRuntime("attachable/" + bundle.identifier, bundle.bundle);
      }
   }

   static final class State {
      EntityRuntime owner;
      EntityRuntime fpOwner;
      final Map<String, Attachables.SlotRun> slots = new HashMap<>();
   }
}
