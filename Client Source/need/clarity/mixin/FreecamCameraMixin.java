package need.clarity.mixin;

import need.clarity.modules.visual.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class FreecamCameraMixin {
   @Shadow
   private boolean detached;

   @Shadow
   protected abstract void setPosition(Vec3 var1);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = Shift.AFTER))
   private void clarity$freecamOverrideCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
      if (Freecam.isActive()) {
         this.detached = true;
         this.setPosition(Freecam.getCameraPos());
         this.setRotation(Freecam.cameraYaw, Freecam.cameraPitch);
      }
   }

   @Inject(method = "position", at = @At("HEAD"), cancellable = true)
   private void clarity$freecamPosition(CallbackInfoReturnable<Vec3> cir) {
      if (Freecam.isActive()) {
         cir.setReturnValue(Freecam.getCameraPos());
      }
   }
}
