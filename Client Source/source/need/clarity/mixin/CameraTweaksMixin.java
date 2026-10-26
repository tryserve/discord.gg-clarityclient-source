package need.clarity.mixin;

import need.clarity.modules.visual.CameraTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraTweaksMixin {
   @Shadow
   private @Nullable Entity entity;

   @Shadow
   protected abstract void move(float var1, float var2, float var3);

   @Shadow
   protected abstract float getMaxZoom(float var1);

   @Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), cancellable = true)
   private void clarity$cameraTweaks(float partialTicks, CallbackInfo ci) {
      if (CameraTweaks.isActive()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && !mc.options.getCameraType().isFirstPerson()) {
            float scaleFactor = 1.0F;
            if (this.entity instanceof LivingEntity living) {
               scaleFactor = living.getScale() * (float)living.getAttributeValue(Attributes.CAMERA_DISTANCE) / 4.0F;
            }

            if (this.entity != null && this.entity.isPassenger() && this.entity.getVehicle() instanceof LivingEntity vehicle) {
               float d = vehicle.getScale() * (float)vehicle.getAttributeValue(Attributes.CAMERA_DISTANCE);
               if (d / 4.0F > scaleFactor) {
                  scaleFactor = d / 4.0F;
               }
            }

            float baseDistance = CameraTweaks.customDistanceEnabled() ? CameraTweaks.customDistance() : 4.0F;
            float distance = baseDistance * scaleFactor;
            boolean noClip = CameraTweaks.noCameraClip();
            this.move(noClip ? -distance : -this.getMaxZoom(distance), 0.0F, 0.0F);
            ci.cancel();
         }
      }
   }
}
