package need.clarity.mixin;

import need.clarity.modules.visual.SmoothCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 900)
public abstract class SmoothCameraMixin {
   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract void setPosition(Vec3 var1);

   @Shadow
   public abstract float yRot();

   @Shadow
   public abstract float xRot();

   @Shadow
   public abstract Vec3 position();

   @Shadow
   public abstract boolean isDetached();

   @Shadow
   public abstract Entity entity();

   @Shadow
   public abstract float getCameraEntityPartialTicks(DeltaTracker var1);

   @Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
   private void clarity$smoothCamera(float partialTicks, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null && mc.player != null) {
         try {
            float[] r = SmoothCamera.smooth(this.yRot(), this.xRot());
            if (r != null) {
               this.setRotation(r[0], r[1]);
            }
         } catch (Throwable var5) {
         }
      }
   }

   @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("RETURN"))
   private void clarity$smoothCameraPosition(DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      Entity e = this.entity();
      if (mc != null && mc.level != null && mc.player != null && e != null && this.isDetached()) {
         try {
            Vec3 p = SmoothCamera.smoothPosition(this.position(), e, this.getCameraEntityPartialTicks(deltaTracker));
            if (p != null) {
               this.setPosition(p);
            }
         } catch (Throwable t) {
            SmoothCamera.smoothPosition(null, null, 0.0F);
         }
      } else {
         SmoothCamera.smoothPosition(null, null, 0.0F);
      }
   }
}
