package need.clarity.bridge;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.movement.AntiPush;
import need.clarity.modules.movement.MoveFix;
import need.clarity.modules.utility.Overtick;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class EntityBridge {
   private EntityBridge() {
   }

   public static boolean onPush(Entity entity) {
      try {
         if (!AntiPush.shouldCancel()) {
            return false;
         }

         Minecraft mc = Minecraft.getInstance();
         return mc.player == null ? false : entity == mc.player;
      } catch (Throwable t) {
         return false;
      }
   }

   public static Vec3 getViewVectorOverride(Entity entity, float tickDelta) {
      try {
         if (entity != Minecraft.getInstance().player) {
            return null;
         } else {
            return !AimAssist.isActivelyTracking() ? null : AimAssist.getRotationVector(AimAssist.getSilentServerPitch(), AimAssist.getSilentServerYaw());
         }
      } catch (Throwable t) {
         return null;
      }
   }

   public static Vec3 getHeadLookAngleOverride(Entity entity) {
      try {
         if (entity != Minecraft.getInstance().player) {
            return null;
         } else {
            return !AimAssist.isActivelyTracking() ? null : AimAssist.getRotationVector(AimAssist.getSilentServerPitch(), AimAssist.getSilentServerYaw());
         }
      } catch (Throwable t) {
         return null;
      }
   }

   public static float redirectMovementYaw(Entity entity) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (entity == mc.player && AimAssist.hasSilentRotation() && MoveFix.shouldApplyRotationFix()) {
            return AimAssist.getSilentServerYaw();
         }
      } catch (Throwable var2) {
      }

      return Float.NaN;
   }

   public static float getEyeHeightOverride(Entity entity) {
      try {
         if (entity != Minecraft.getInstance().player) {
            return -1.0F;
         }

         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         return overtick != null && overtick.isEnabled() && overtick.isElytraStopHeld() ? entity.getEyeHeight(Pose.STANDING) : -1.0F;
      } catch (Throwable t) {
         return -1.0F;
      }
   }
}
