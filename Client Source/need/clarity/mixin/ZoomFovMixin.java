package need.clarity.mixin;

import need.clarity.modules.visual.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class ZoomFovMixin {
   @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
   private void cl$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
      try {
         cir.setReturnValue(Zoom.applyFov(cir.getReturnValueF()));
      } catch (Throwable var4) {
      }
   }
}
