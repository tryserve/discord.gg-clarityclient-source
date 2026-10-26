package need.clarity.bridge;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.ServerRotations;
import need.clarity.util.RotationGcd;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class LocalPlayerBridge {
   private static boolean silentApplied;
   private static float tickServerYaw;
   private static float tickServerPitch;
   private static float sYaw;
   private static float sPitch;
   private static float sRenderYaw;
   private static float sRenderPitch;
   private static float sHeadYaw;
   private static float sBodyYaw;
   private static boolean haveLast;
   private static float lastYaw;
   private static float lastPitch;
   private static final float BODY_FOLLOW = 0.22F;
   private static final float MAX_HEAD_YAW = 50.0F;

   private LocalPlayerBridge() {
   }

   public static void onTickHead() {
      try {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer self = mc.player;
         if (self == null) {
            return;
         }

         silentApplied = false;
         LocalPlayerRotationState.silentActive = false;
         if (!AimAssist.hasSilentRotation()) {
            haveLast = true;
            lastYaw = self.getYRot();
            lastPitch = self.getXRot();
            return;
         }

         if (!haveLast) {
            lastYaw = self.getYRot();
            lastPitch = self.getXRot();
            haveLast = true;
         }

         float wantYaw = AimAssist.getSilentServerYaw();
         float wantPitch = Mth.clamp(AimAssist.getSilentServerPitch(), -90.0F, 90.0F);
         if (!Float.isFinite(wantYaw) || !Float.isFinite(wantPitch)) {
            lastYaw = self.getYRot();
            lastPitch = self.getXRot();
            return;
         }

         if (RotationGcd.enabled()) {
            tickServerYaw = RotationGcd.snapYaw(lastYaw, wantYaw);
            tickServerPitch = RotationGcd.snapPitch(lastPitch, wantPitch);
         } else {
            tickServerYaw = wantYaw;
            tickServerPitch = wantPitch;
         }

         lastYaw = tickServerYaw;
         lastPitch = tickServerPitch;
         AimAssist.commitSnappedRotation(tickServerYaw, tickServerPitch);
         sYaw = self.getYRot();
         sPitch = self.getXRot();
         sRenderYaw = self.yBob;
         sRenderPitch = self.xBob;
         sHeadYaw = self.yHeadRot;
         sBodyYaw = self.yBodyRot;
         LocalPlayerRotationState.tickServerYaw = tickServerYaw;
         LocalPlayerRotationState.tickServerPitch = tickServerPitch;
         LocalPlayerRotationState.realYaw = sYaw;
         LocalPlayerRotationState.silentActive = true;
         ServerRotations.captureLastPitch(ServerRotations.serverPitchLerp(1.0F));
         self.setYRot(tickServerYaw);
         self.setXRot(tickServerPitch);
         ServerRotations.captureCurrentPitch(tickServerPitch);
         silentApplied = true;
      } catch (Throwable var4) {
      }
   }

   public static void onTickReturn() {
      try {
         LocalPlayerRotationState.silentActive = false;
         if (!silentApplied) {
            return;
         }

         silentApplied = false;
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer self = mc.player;
         if (self == null) {
            return;
         }

         self.setYRot(sYaw);
         self.setXRot(sPitch);
         self.yRotO = sYaw;
         self.xRotO = sPitch;
         float modelYaw = ServerRotations.shouldApply() ? tickServerYaw : sYaw;
         self.yHeadRotO = sHeadYaw;
         self.yHeadRot = modelYaw;
         float body = sBodyYaw + Mth.wrapDegrees(modelYaw - sBodyYaw) * 0.22F;
         float d = Mth.wrapDegrees(modelYaw - body);
         if (d > 50.0F) {
            body = modelYaw - 50.0F;
         } else if (d < -50.0F) {
            body = modelYaw + 50.0F;
         }

         self.yBodyRotO = sBodyYaw;
         self.yBodyRot = body;
         self.yBobO = sRenderYaw;
         self.xBobO = sRenderPitch;
         self.yBob = sRenderYaw + (sYaw - sRenderYaw) * 0.5F;
         self.xBob = sRenderPitch + (sPitch - sRenderPitch) * 0.5F;
      } catch (Throwable var5) {
      }
   }
}
