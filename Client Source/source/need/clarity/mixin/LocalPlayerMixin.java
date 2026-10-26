package need.clarity.mixin;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.ServerRotations;
import need.clarity.util.RotDebug;
import need.clarity.util.RotationGcd;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
   @Shadow
   public float yBob;
   @Shadow
   public float yBobO;
   @Shadow
   public float xBob;
   @Shadow
   public float xBobO;
   @Unique
   private boolean cl$silentApplied;
   @Unique
   private float cl$tickServerYaw;
   @Unique
   private float cl$tickServerPitch;
   @Unique
   private float cl$sYaw;
   @Unique
   private float cl$sPitch;
   @Unique
   private float cl$sRenderYaw;
   @Unique
   private float cl$sRenderPitch;
   @Unique
   private float cl$sHeadYaw;
   @Unique
   private float cl$sBodyYaw;
   @Unique
   private boolean cl$haveLast;
   @Unique
   private float cl$lastYaw;
   @Unique
   private float cl$lastPitch;
   @Unique
   private static final float cl$BODY_FOLLOW = 0.22F;
   @Unique
   private static final float cl$MAX_HEAD_YAW = 50.0F;

   @Inject(method = "tick", at = @At("HEAD"))
   private void cl$silentRotationHead(CallbackInfo ci) {
      this.cl$silentApplied = false;
      LocalPlayerRotationState.silentActive = false;
      LocalPlayer self = (LocalPlayer)(Object)this;
      if (!AimAssist.hasSilentRotation()) {
         this.cl$haveLast = true;
         this.cl$lastYaw = self.getYRot();
         this.cl$lastPitch = self.getXRot();
      } else {
         if (!this.cl$haveLast) {
            this.cl$lastYaw = self.getYRot();
            this.cl$lastPitch = self.getXRot();
            this.cl$haveLast = true;
         }

         float cl$wantYaw = AimAssist.getSilentServerYaw();
         float cl$wantPitch = Mth.clamp(AimAssist.getSilentServerPitch(), -90.0F, 90.0F);
         if (Float.isFinite(cl$wantYaw) && Float.isFinite(cl$wantPitch)) {
            if (RotationGcd.enabled()) {
               this.cl$tickServerYaw = RotationGcd.snapYaw(this.cl$lastYaw, cl$wantYaw);
               this.cl$tickServerPitch = RotationGcd.snapPitch(this.cl$lastPitch, cl$wantPitch);
            } else {
               this.cl$tickServerYaw = cl$wantYaw;
               this.cl$tickServerPitch = cl$wantPitch;
            }

            if (Math.abs(this.cl$tickServerPitch) > 80.0F && RotDebug.enabled()) {
               RotDebug.report("TICK applies server pitch=" + this.cl$tickServerPitch + " (wanted " + cl$wantPitch + ", last sent " + this.cl$lastPitch + ", camera " + self.getXRot() + ")");
            }

            this.cl$lastYaw = this.cl$tickServerYaw;
            this.cl$lastPitch = this.cl$tickServerPitch;
            AimAssist.commitSnappedRotation(this.cl$tickServerYaw, this.cl$tickServerPitch);
            this.cl$sYaw = self.getYRot();
            this.cl$sPitch = self.getXRot();
            this.cl$sRenderYaw = this.yBob;
            this.cl$sRenderPitch = this.xBob;
            this.cl$sHeadYaw = self.yHeadRot;
            this.cl$sBodyYaw = self.yBodyRot;
            LocalPlayerRotationState.tickServerYaw = this.cl$tickServerYaw;
            LocalPlayerRotationState.tickServerPitch = this.cl$tickServerPitch;
            LocalPlayerRotationState.realYaw = this.cl$sYaw;
            LocalPlayerRotationState.silentActive = true;
            ServerRotations.captureLastPitch(ServerRotations.serverPitchLerp(1.0F));
            self.setYRot(this.cl$tickServerYaw);
            self.setXRot(this.cl$tickServerPitch);
            ServerRotations.captureCurrentPitch(this.cl$tickServerPitch);
            this.cl$silentApplied = true;
         } else {
            this.cl$lastYaw = self.getYRot();
            this.cl$lastPitch = self.getXRot();
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void cl$silentRotationReturn(CallbackInfo ci) {
      LocalPlayerRotationState.silentActive = false;
      if (this.cl$silentApplied) {
         this.cl$silentApplied = false;
         LocalPlayer self = (LocalPlayer)(Object)this;
         self.setYRot(this.cl$sYaw);
         self.setXRot(this.cl$sPitch);
         self.yRotO = this.cl$sYaw;
         self.xRotO = this.cl$sPitch;
         float cl$modelYaw = ServerRotations.shouldApply() ? this.cl$tickServerYaw : this.cl$sYaw;
         self.yHeadRotO = this.cl$sHeadYaw;
         self.yHeadRot = cl$modelYaw;
         float cl$body = this.cl$sBodyYaw + Mth.wrapDegrees(cl$modelYaw - this.cl$sBodyYaw) * 0.22F;
         float cl$d = Mth.wrapDegrees(cl$modelYaw - cl$body);
         if (cl$d > 50.0F) {
            cl$body = cl$modelYaw - 50.0F;
         } else if (cl$d < -50.0F) {
            cl$body = cl$modelYaw + 50.0F;
         }

         self.yBodyRotO = this.cl$sBodyYaw;
         self.yBodyRot = cl$body;
         this.yBobO = this.cl$sRenderYaw;
         this.xBobO = this.cl$sRenderPitch;
         this.yBob = this.cl$sRenderYaw + (this.cl$sYaw - this.cl$sRenderYaw) * 0.5F;
         this.xBob = this.cl$sRenderPitch + (this.cl$sPitch - this.cl$sRenderPitch) * 0.5F;
      }
   }
}
