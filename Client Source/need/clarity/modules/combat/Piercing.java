package need.clarity.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Piercing extends ModuleBase {
   private static Piercing instance;
   private static final double DEFAULT_ATTACK_RANGE = 3.0;

   public Piercing() {
      super("Piercing", "Combat");
      instance = this;
   }

   public static Piercing getInstance() {
      return instance;
   }

   public void onHandleInput(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         if (mc.hitResult != null) {
            Piercing.Trace trace = this.buildTrace(mc);
            boolean attackPending = mc.options.keyAttack.isDown();
            if (attackPending) {
               Piercing.TargetHit hit = this.findTargetHit(mc, trace);
               if (hit != null) {
                  this.setCrosshairTarget(mc, hit);
               }
            } else {
               boolean interact = this.getSetting("Interact", false);
               if (interact && mc.options.keyUse.isDown() && this.getHoldingBlockHand(mc) != null) {
                  BlockHitResult blockHit = this.raycastBlock(mc);
                  if (blockHit.getType() == Type.BLOCK) {
                     mc.hitResult = blockHit;
                     return;
                  }
               }

               this.restoreBlockHit(mc, trace);
            }
         }
      }
   }

   public LivingEntity findPiercingTarget(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         Piercing.TargetHit hit = this.findTargetHit(mc, this.buildTrace(mc));
         return hit != null ? hit.target() : null;
      } else {
         return null;
      }
   }

   private Piercing.Trace buildTrace(Minecraft mc) {
      Player player = mc.player;
      float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      Vec3 start = player.getEyePosition(pt);
      double maxRange = this.getMaxRange(mc);
      Vec3 end = start.add(player.getViewVector(pt).scale(maxRange));
      return new Piercing.Trace(player, start, end);
   }

   private double getMaxRange(Minecraft mc) {
      Player p = mc.player;
      double attackRange = this.getAttackRange(p);
      return Math.max(attackRange, p.blockInteractionRange());
   }

   private double getAttackRange(Player player) {
      try {
         return player.entityInteractionRange();
      } catch (Throwable var3) {
         return 3.0;
      }
   }

   private double getEntityRangeSq(Minecraft mc, Piercing.Trace trace) {
      double attackRange = this.getAttackRange(mc.player);
      double rangeSq = attackRange * attackRange;
      boolean throughBlocks = this.getSetting("Blocks", true);
      if (throughBlocks) {
         return rangeSq;
      }

      BlockHitResult blockHit = this.clipBlocks(mc, trace);
      if (blockHit.getType() == Type.MISS) {
         return rangeSq;
      }

      double blockDistSq = trace.start().distanceToSqr(blockHit.getLocation());
      return Math.min(blockDistSq, rangeSq);
   }

   private Piercing.TargetHit findTargetHit(Minecraft mc, Piercing.Trace trace) {
      double maxRangeSq = this.getEntityRangeSq(mc, trace);
      List<Piercing.TargetHit> candidates = this.collectTargets(mc, trace, maxRangeSq);
      return candidates.isEmpty() ? null : this.selectBest(candidates);
   }

   private List<Piercing.TargetHit> collectTargets(Minecraft mc, Piercing.Trace trace, double maxRangeSq) {
      List<Piercing.TargetHit> targets = new ArrayList<>();
      int typeMask = this.buildTypeMask();
      AABB searchBox = mc.player.getBoundingBox().expandTowards(trace.end().subtract(trace.start())).inflate(1.0, 1.0, 1.0);

      for (Entity entity : mc.level.getEntities(mc.player, searchBox, EntitySelector.CAN_BE_PICKED)) {
         if (entity instanceof LivingEntity living && Targets.isTypeAllowed(living, typeMask) && Targets.isTargetAllowed(living, mc.player)) {
            AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> clip = hitbox.clip(trace.start(), trace.end());
            if (!clip.isEmpty()) {
               Vec3 hitLoc = clip.get();
               double distSq = trace.start().distanceToSqr(hitLoc);
               if (!(distSq > maxRangeSq)) {
                  targets.add(new Piercing.TargetHit(living, hitLoc));
               }
            }
         }
      }

      return targets;
   }

   private Piercing.TargetHit selectBest(List<Piercing.TargetHit> targets) {
      if (targets.size() == 1) {
         return targets.get(0);
      }

      String sort = this.getSetting("Sort Targets By", "Hurt Time");
      if ("Health".equals(sort)) {
         targets.sort(Comparator.comparingDouble(t -> t.target().getHealth()));
      } else {
         targets.sort(Comparator.comparingInt(t -> t.target().hurtTime));
      }

      return targets.get(0);
   }

   private int buildTypeMask() {
      int mask = 0;
      if (this.getSetting("Players", true)) {
         mask |= 1;
      }

      if (this.getSetting("Hostile Mobs", false)) {
         mask |= 2;
      }

      if (this.getSetting("Passive Mobs", false)) {
         mask |= 4;
      }

      if (mask == 0) {
         mask = 1;
      }

      return mask;
   }

   private BlockHitResult clipBlocks(Minecraft mc, Piercing.Trace trace) {
      return mc.level.clip(new ClipContext(trace.start(), trace.end(), Block.OUTLINE, Fluid.NONE, mc.player));
   }

   private BlockHitResult raycastBlock(Minecraft mc) {
      Vec3 start = mc.player.getEyePosition();
      double yawRad = Math.toRadians(mc.player.getYRot() * -1.0);
      double pitchRad = Math.toRadians(mc.player.getXRot() * -1.0);
      Vec3 dir = new Vec3(Math.sin(yawRad) * Math.cos(pitchRad), Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
      double range = mc.player.blockInteractionRange();
      Vec3 end = start.add(dir.scale(range));
      return this.clipBlocks(mc, new Piercing.Trace(mc.player, start, end));
   }

   private void setCrosshairTarget(Minecraft mc, Piercing.TargetHit hit) {
      mc.hitResult = new EntityHitResult(hit.target(), hit.location());
      mc.crosshairPickEntity = hit.target();
   }

   private void restoreBlockHit(Minecraft mc, Piercing.Trace trace) {
      if (mc.hitResult instanceof EntityHitResult entityHit) {
         if (!(entityHit.getEntity() instanceof LivingEntity living && Targets.isTargetAllowed(living, mc.player))) {
            BlockHitResult blockHit = this.clipBlocks(mc, trace);
            double blockInteractRangeSq = mc.player.blockInteractionRange() * mc.player.blockInteractionRange();
            if (blockHit.getType() != Type.MISS && blockHit.getLocation().distanceToSqr(trace.start()) <= blockInteractRangeSq) {
               mc.hitResult = blockHit;
            }

            mc.crosshairPickEntity = null;
         }
      }
   }

   private InteractionHand getHoldingBlockHand(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (main.getItem() instanceof BlockItem) {
         return InteractionHand.MAIN_HAND;
      }

      ItemStack off = mc.player.getOffhandItem();
      return off.getItem() instanceof BlockItem ? InteractionHand.OFF_HAND : null;
   }

   private record TargetHit(LivingEntity target, Vec3 location) {
   }

   private record Trace(Player player, Vec3 start, Vec3 end) {
   }
}
