package need.clarity.bridge;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.movement.Fly;
import need.clarity.modules.movement.MoveFix;
import need.clarity.modules.movement.NoSlow;
import need.clarity.modules.visual.Animations;
import need.clarity.modules.visual.NoSwing;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class LivingEntityBridge {
   private LivingEntityBridge() {
   }

   public static boolean shouldCancelSwing(LivingEntity entity) {
      try {
         return !NoSwing.isActive() ? false : entity == Minecraft.getInstance().player;
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onTravelHead(LivingEntity entity, Vec3 travelVector) {
      try {
         if (!Fly.isActive()) {
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || entity != mc.player) {
            return;
         }

         double curY = entity.getDeltaMovement().y;
         if (curY < 0.0 && !Fly.isJumpDown(mc) && !Fly.isDescendDown(mc)) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0, entity.getDeltaMovement().z);
         }
      } catch (Throwable var5) {
      }
   }

   public static void onTravelReturn(LivingEntity entity) {
      try {
         if (!Fly.isActive() || !Fly.hasPending) {
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || entity != mc.player) {
            return;
         }

         String mode = Fly.getMode();
         if ("Full".equals(mode)) {
            entity.setDeltaMovement(Fly.pendingVelX, Fly.pendingVelY, Fly.pendingVelZ);
         } else {
            Vec3 cur = entity.getDeltaMovement();
            entity.setDeltaMovement(cur.x * 0.91 + Fly.pendingVelX, Fly.pendingVelY, cur.z * 0.91 + Fly.pendingVelZ);
         }

         Fly.hasPending = false;
      } catch (Throwable var4) {
      }
   }

   public static boolean shouldCancelFallDamage(LivingEntity entity) {
      try {
         return !Fly.isActive() ? false : entity == Minecraft.getInstance().player;
      } catch (Throwable t) {
         return false;
      }
   }

   public static int getSwingDurationOverride(LivingEntity entity) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || entity != mc.player) {
            return -1;
         } else {
            return !Animations.shouldOverrideSwingSpeed() ? -1 : Animations.getConfiguredSwingSpeed();
         }
      } catch (Throwable t) {
         return -1;
      }
   }

   public static float redirectElytraPitch(LivingEntity entity) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (entity == mc.player && LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix()) {
            return LocalPlayerRotationState.tickServerPitch;
         }
      } catch (Throwable var2) {
      }

      return Float.NaN;
   }

   public static Vec3 redirectElytraLookVector(LivingEntity entity) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (entity == mc.player && LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix()) {
            return AimAssist.getRotationVector(LocalPlayerRotationState.tickServerPitch, LocalPlayerRotationState.tickServerYaw);
         }
      } catch (Throwable var2) {
      }

      return null;
   }

   public static Vec3 noSlowTravel(LivingEntity self, Vec3 in) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || self != mc.player) {
            return in;
         } else {
            return !NoSlow.shouldSkipSlow(mc.player) ? in : new Vec3(in.x * 5.0, in.y, in.z * 5.0);
         }
      } catch (Throwable t) {
         return in;
      }
   }
}
