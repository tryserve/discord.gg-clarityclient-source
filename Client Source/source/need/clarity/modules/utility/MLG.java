package need.clarity.modules.utility;

import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.util.AccessorCompat;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class MLG extends ModuleBase {
   private MLG.State state = MLG.State.IDLE;
   private int savedSlot = -1;
   private int swapBackTimer = 0;
   private boolean swapBackPending = false;
   private long swapBackStartedAt = 0L;
   private boolean placedWater = false;
   private int waterBucketSlot = -1;
   private int pickupWaitTicks = 0;
   private boolean frameShouldAim = false;
   private float silentYaw = 0.0F;
   private float silentPitch = 0.0F;
   private boolean silentInitialised = false;
   private float cachedDistToGround = 512.0F;

   public MLG() {
      super("MLG", "utility");
      PerFrameEvents.register(this::onFrame);
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.clearAim();
      this.fullReset();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (this.swapBackTimer > 0) {
            this.swapBackTimer--;
            if (this.swapBackTimer == 0 && this.savedSlot >= 0) {
               boolean swapBack = this.getSetting("Swap Back", false);
               if (swapBack) {
                  this.swapBackPending = true;
                  this.swapBackStartedAt = System.currentTimeMillis();
               } else {
                  InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
                  this.savedSlot = -1;
               }
            }
         }

         if (this.swapBackPending && this.savedSlot >= 0) {
            int delayMs = this.getSetting("Swap Back Delay", 0);
            if (System.currentTimeMillis() - this.swapBackStartedAt >= delayMs) {
               InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
               this.savedSlot = -1;
               this.swapBackPending = false;
            }
         }

         if (this.state == MLG.State.PICKUP) {
            this.tickPickup(mc);
         } else if (!mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
            double vy = mc.player.getDeltaMovement().y;
            if (vy >= 0.0) {
               if (this.state == MLG.State.DONE) {
                  this.frameShouldAim = false;
               }

               if (this.state == MLG.State.IDLE) {
                  return;
               }
            }

            this.cachedDistToGround = this.raycastDistToGround(mc);
            float threshold = this.getSetting("Fall Threshold", 5.0F);
            float fallDist = (float)mc.player.fallDistance;
            float totalFall = fallDist + this.cachedDistToGround;
            if (totalFall < threshold) {
               if (this.state == MLG.State.DONE) {
                  this.frameShouldAim = false;
               }

               if (this.state == MLG.State.IDLE) {
                  return;
               }
            }

            boolean onlyLethal = this.getSetting("Only Lethal Fall", false);
            if (onlyLethal && !this.wouldBeLethal(mc, fallDist)) {
               if (this.state == MLG.State.IDLE) {
                  return;
               }

               if (this.state == MLG.State.AIMING) {
                  this.resetState();
                  return;
               }
            }

            int targetSlot = this.findItemSlot(mc);
            if (targetSlot < 0) {
               this.frameShouldAim = false;
            } else {
               float ticksToImpact = this.estimateTicksToImpact(vy, this.cachedDistToGround);
               float placeAtTicks = 5.0F;
               switch (this.state) {
                  case IDLE:
                     this.state = MLG.State.AIMING;
                     this.silentInitialised = false;
                     this.frameShouldAim = true;
                     this.placedWater = false;
                     this.waterBucketSlot = -1;
                     break;
                  case AIMING:
                     this.frameShouldAim = true;
                     float pitchErr = Math.abs(90.0F - this.silentPitch);
                     float tolerance = ticksToImpact <= 1.0F ? 90.0F : (ticksToImpact <= 3.0F ? 30.0F : 5.0F);
                     boolean aimed = pitchErr <= tolerance;
                     boolean time = ticksToImpact <= placeAtTicks;
                     if (aimed && time || ticksToImpact <= 2.0F) {
                        this.state = MLG.State.PLACING;
                     }
                     break;
                  case PLACING:
                     this.frameShouldAim = true;
                     this.doPlace(mc, targetSlot);
                     this.state = MLG.State.WAITING;
                     break;
                  case WAITING:
                     this.frameShouldAim = true;
                     if (this.swapBackTimer <= 0) {
                        this.state = MLG.State.DONE;
                     }
                     break;
                  case DONE:
                     this.frameShouldAim = true;
               }
            }
         } else {
            if (this.state == MLG.State.DONE || this.state == MLG.State.WAITING || this.state == MLG.State.AIMING) {
               boolean pickUpWater = this.getSetting("Pick Up Water", false);
               boolean useWater = this.getSetting("Water Bucket", true);
               if (pickUpWater && useWater && this.placedWater && mc.player.isInWater()) {
                  this.clearAim();
                  this.frameShouldAim = false;
                  this.state = MLG.State.PICKUP;
                  int delayMs = this.getSetting("Pick Up Delay", 0);
                  this.pickupWaitTicks = delayMs / 50;
                  return;
               }
            }

            if (this.state != MLG.State.IDLE) {
               this.clearAim();
               this.resetState();
            }
         }
      }
   }

   private void tickPickup(Minecraft mc) {
      if (this.pickupWaitTicks > 0) {
         this.pickupWaitTicks--;
      } else if (!mc.player.isInWater()) {
         this.state = MLG.State.IDLE;
         this.placedWater = false;
      } else {
         int emptySlot = -1;
         if (this.waterBucketSlot >= 0 && this.waterBucketSlot < 9 && mc.player.getInventory().getItem(this.waterBucketSlot).is(Items.BUCKET)) {
            emptySlot = this.waterBucketSlot;
         }

         if (emptySlot < 0) {
            for (int i = 0; i < 9; i++) {
               if (mc.player.getInventory().getItem(i).is(Items.BUCKET)) {
                  emptySlot = i;
                  break;
               }
            }
         }

         if (emptySlot < 0) {
            this.state = MLG.State.IDLE;
            this.placedWater = false;
         } else {
            int cur = InventoryHelper.getSelected(mc.player.getInventory());
            if (this.savedSlot < 0) {
               this.savedSlot = cur;
            }

            InventoryHelper.setSelected(mc.player.getInventory(), emptySlot);
            boolean silentRot = this.getSetting("Silent Rotations", true);
            if (silentRot) {
               AimAssist.setSilentRotation(mc.player.getYRot(), 89.0F);
            }

            AccessorCompat.addClick(mc.options.keyUse);
            this.swapBackTimer = 2;
            this.state = MLG.State.IDLE;
            this.placedWater = false;
            this.waterBucketSlot = -1;
         }
      }
   }

   private void onFrame() {
      if (this.isEnabled() && this.frameShouldAim) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            boolean silentRot = this.getSetting("Silent Rotations", true);
            float aimSpeed = this.getSetting("Aim Speed", 10).intValue();
            if (!this.silentInitialised) {
               this.silentInitialised = true;
               if (silentRot && AimAssist.hasSilentRotation()) {
                  this.silentYaw = AimAssist.getSilentServerYaw();
                  this.silentPitch = AimAssist.getSilentServerPitch();
               } else {
                  this.silentYaw = mc.player.getYRot();
                  this.silentPitch = mc.player.getXRot();
               }
            }

            float targetPitch = 90.0F;
            float targetYaw = this.silentYaw;
            float degsPerFrame = aimSpeed / 3.0F;
            float pitchDiff = targetPitch - this.silentPitch;
            float pitchStep = Math.min(Math.abs(pitchDiff), degsPerFrame) * Math.signum(pitchDiff);
            this.silentPitch = Math.max(-90.0F, Math.min(90.0F, this.silentPitch + pitchStep));
            this.silentYaw = targetYaw;
            if (silentRot) {
               AimAssist.setSilentRotation(this.silentYaw, this.silentPitch);
            } else {
               mc.player.setYRot(this.silentYaw);
               mc.player.setXRot(this.silentPitch);
            }
         }
      }
   }

   private void doPlace(Minecraft mc, int slot) {
      int cur = InventoryHelper.getSelected(mc.player.getInventory());
      if (cur != slot) {
         this.savedSlot = cur;
         InventoryHelper.setSelected(mc.player.getInventory(), slot);
      } else {
         this.savedSlot = -1;
      }

      this.placedWater = mc.player.getInventory().getItem(slot).is(Items.WATER_BUCKET);
      this.waterBucketSlot = slot;
      if (this.cachedDistToGround <= 4.5F) {
         BlockHitResult groundHit = this.raycastDirectlyBelow(mc);
         if (groundHit != null) {
            mc.hitResult = groundHit;
         }
      }

      AccessorCompat.addClick(mc.options.keyUse);
      this.swapBackTimer = 2;
   }

   private float estimateTicksToImpact(double vy, float distToGround) {
      if (distToGround <= 0.0F) {
         return 0.0F;
      }

      double y = 0.0;
      double v = vy;
      int tick = 0;

      while (tick < 2000) {
         v = (v - 0.08) * 0.98;
         y += v;
         tick++;
         if (y <= distToGround * -1.0) {
            return tick;
         }
      }

      return 2000.0F;
   }

   private float raycastDistToGround(Minecraft mc) {
      Vec3 feet = mc.player.position();
      Vec3 castEnd = feet.add(0.0, -512.0, 0.0);
      BlockHitResult hit = mc.level.clip(new ClipContext(feet, castEnd, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS ? 512.0F : (float)(feet.y - hit.getLocation().y);
   }

   private BlockHitResult raycastDirectlyBelow(Minecraft mc) {
      Vec3 feet = mc.player.position();
      Vec3 castEnd = feet.add(0.0, -512.0, 0.0);
      BlockHitResult hit = mc.level.clip(new ClipContext(feet, castEnd, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS ? null : new BlockHitResult(hit.getLocation(), Direction.UP, hit.getBlockPos(), false);
   }

   private boolean wouldBeLethal(Minecraft mc, float fallDist) {
      float totalFall = fallDist + this.cachedDistToGround;
      float damage = Math.max(0.0F, totalFall - 3.0F);
      return damage >= mc.player.getHealth();
   }

   private int findItemSlot(Minecraft mc) {
      if (!this.getSetting("Water Bucket", true)) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).is(Items.WATER_BUCKET)) {
            return i;
         }
      }

      return -1;
   }

   private void clearAim() {
      boolean silentRot = this.getSetting("Silent Rotations", true);
      if (silentRot) {
         AimAssist.clearSilentRotation();
      }
   }

   private void resetState() {
      this.state = MLG.State.IDLE;
      this.frameShouldAim = false;
      this.silentInitialised = false;
      this.placedWater = false;
      this.waterBucketSlot = -1;
      this.pickupWaitTicks = 0;
      this.cachedDistToGround = 512.0F;
   }

   private void fullReset() {
      this.resetState();
      this.savedSlot = -1;
      this.swapBackTimer = 0;
      this.swapBackPending = false;
      this.swapBackStartedAt = 0L;
   }

   private enum State {
      IDLE,
      AIMING,
      PLACING,
      WAITING,
      DONE,
      PICKUP;
   }
}
