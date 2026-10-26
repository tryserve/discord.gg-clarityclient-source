package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.NoHurtCam;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoHurtCamMixin {
   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void clarity$noHurtCam(CameraRenderState state, PoseStack poseStack, CallbackInfo ci) {
      if (NoHurtCam.isActive()) {
         ci.cancel();
      }
   }
}
