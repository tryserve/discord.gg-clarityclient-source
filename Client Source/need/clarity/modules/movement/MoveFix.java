package need.clarity.modules.movement;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.Freecam;
import net.minecraft.util.Mth;

public class MoveFix extends ModuleBase {
   private static MoveFix instance;
   private static volatile boolean movementFrozen = false;

   public MoveFix() {
      super("MoveFix", "Movement");
      instance = this;
   }

   public static void setMovementFrozen(boolean frozen) {
      movementFrozen = frozen;
   }

   public static boolean isMovementFrozen() {
      return movementFrozen || Freecam.isActive() && Freecam.shouldFreezePlayer();
   }

   public static boolean shouldApplyRotationFix() {
      if (instance == null || !instance.isEnabled()) {
         return false;
      }

      if (!AimAssist.hasSilentRotation()) {
         return false;
      }

      String mode = instance.getSetting("Mode", "Silent");
      return !"None".equals(mode);
   }

   public static boolean shouldCorrectMovementInput() {
      if (!shouldApplyRotationFix()) {
         return false;
      } else {
         return instance == null ? false : "Silent".equals(instance.getSetting("Mode", "Silent"));
      }
   }

   public static void onMovementInput(MoveFix.EventMovementInput event) {
      if (shouldCorrectMovementInput()) {
         if (LocalPlayerRotationState.silentActive) {
            event.syncFloatsFromBooleans();
            float forward = event.forwardAmount;
            float sideways = event.sidewaysAmount;
            if (forward != 0.0F || sideways != 0.0F) {
               float clientYaw = LocalPlayerRotationState.realYaw;
               float serverYaw = LocalPlayerRotationState.tickServerYaw;
               float intendedAngle = (float)Math.toDegrees(getDirection(clientYaw, forward, sideways));
               float[] best = bestInputFor(serverYaw, intendedAngle);
               float bestForward = best[0];
               float bestSideways = best[1];
               event.forwardAmount = bestForward;
               event.sidewaysAmount = bestSideways;
               event.syncBooleansFromFloats();
            }
         }
      }
   }

   public static float[] bestInputFor(float moveYaw, float wantedYaw) {
      float bestForward = 0.0F;
      float bestSideways = 0.0F;
      float bestDiff = Float.MAX_VALUE;

      for (float pf = -1.0F; pf <= 1.0F; pf++) {
         for (float ps = -1.0F; ps <= 1.0F; ps++) {
            if (pf != 0.0F || ps != 0.0F) {
               float angle = (float)Math.toDegrees(getDirection(moveYaw, pf, ps));
               float diff = Math.abs(Mth.wrapDegrees(wantedYaw - angle));
               if (!(diff >= bestDiff)) {
                  bestDiff = diff;
                  bestForward = pf;
                  bestSideways = ps;
               }
            }
         }
      }

      return new float[]{bestForward, bestSideways};
   }

   public static float yawOf(double dx, double dz) {
      return (float)Math.toDegrees(Math.atan2(-dx, dz));
   }

   public static double getDirection(float yaw, float forward, float sideways) {
      if (forward < 0.0F) {
         yaw += 180.0F;
      }

      float m = 1.0F;
      if (forward < 0.0F) {
         m = -0.5F;
      } else if (forward > 0.0F) {
         m = 0.5F;
      }

      if (sideways > 0.0F) {
         yaw -= 90.0F * m;
      }

      if (sideways < 0.0F) {
         yaw += 90.0F * m;
      }

      return Math.toRadians(yaw);
   }

   public static class EventMovementInput {
      public boolean forward;
      public boolean back;
      public boolean left;
      public boolean right;
      public boolean jump;
      public boolean sneak;
      public boolean sprint;
      public float forwardAmount;
      public float sidewaysAmount;
      public final boolean originalForward;
      public final boolean originalBack;
      public final boolean originalLeft;
      public final boolean originalRight;
      public final boolean originalJump;
      public final boolean originalSneak;
      public final boolean originalSprint;

      public EventMovementInput(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
         this.forward = forward;
         this.back = back;
         this.left = left;
         this.right = right;
         this.jump = jump;
         this.sneak = sneak;
         this.sprint = sprint;
         this.originalForward = forward;
         this.originalBack = back;
         this.originalLeft = left;
         this.originalRight = right;
         this.originalJump = jump;
         this.originalSneak = sneak;
         this.originalSprint = sprint;
         this.syncFloatsFromBooleans();
      }

      public void syncFloatsFromBooleans() {
         this.forwardAmount = this.forward ? 1.0F : (this.back ? -1.0F : 0.0F);
         this.sidewaysAmount = this.left ? 1.0F : (this.right ? -1.0F : 0.0F);
      }

      public void syncBooleansFromFloats() {
         this.forward = this.forwardAmount > 0.0F;
         this.back = this.forwardAmount < 0.0F;
         this.left = this.sidewaysAmount > 0.0F;
         this.right = this.sidewaysAmount < 0.0F;
      }
   }
}
