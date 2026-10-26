package need.clarity.mixin;

import need.clarity.modules.visual.Aspect;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class AspectMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void clarity$aspect(CameraRenderState state, float partialTick, CallbackInfo ci) {
      if (Aspect.isActive()) {
         if (state.projectionMatrix != null) {
            state.projectionMatrix.scale(Aspect.factor(), 1.0F, 1.0F);
         }
      }
   }
}
