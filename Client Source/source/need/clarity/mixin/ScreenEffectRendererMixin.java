package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.NoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
   @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
   private static void clarity$noFire(PoseStack matrices, MultiBufferSource vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelFire()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
   private void clarity$noTotemAnimation(PoseStack matrices, float tickProgress, SubmitNodeCollector queue, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelTotemAnimation()) {
         ci.cancel();
      }
   }
}
