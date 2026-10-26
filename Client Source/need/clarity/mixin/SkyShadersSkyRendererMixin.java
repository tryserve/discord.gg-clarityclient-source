package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyShadersSkyRendererMixin {
   @Inject(method = "renderSkyDisc(I)V", at = @At("HEAD"), cancellable = true)
   private void clarity$skyDisc(int skyColor, CallbackInfo ci) {
      if (SkyShaderRenderer.tryRender(false)) {
         ci.cancel();
      }
   }

   @Inject(method = "renderEndSky()V", at = @At("HEAD"), cancellable = true)
   private void clarity$endSky(CallbackInfo ci) {
      if (SkyShaderRenderer.tryRender(true)) {
         ci.cancel();
      }
   }

   @Inject(method = "renderDarkDisc()V", at = @At("HEAD"), cancellable = true)
   private void clarity$darkDisc(CallbackInfo ci) {
      if (SkyShaderRenderer.isActiveThisFrame()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V", at = @At("HEAD"), cancellable = true)
   private void clarity$sunrise(PoseStack poseStack, float angle, int color, CallbackInfo ci) {
      if (SkyShaderRenderer.isActiveThisFrame()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V", at = @At("HEAD"), cancellable = true)
   private void clarity$sunMoonStars(PoseStack poseStack, float a, float b, float c, MoonPhase phase, float d, float e, CallbackInfo ci) {
      if (SkyShaderRenderer.isActiveThisFrame() && !SkyShaders.keepSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStars(FLcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"), cancellable = true)
   private void clarity$stars(float brightness, PoseStack poseStack, CallbackInfo ci) {
      if (SkyShaderRenderer.isActiveThisFrame()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("HEAD"), cancellable = true)
   private void clarity$endFlash(PoseStack poseStack, float a, float b, float c, CallbackInfo ci) {
      if (SkyShaderRenderer.isActiveThisFrame()) {
         ci.cancel();
      }
   }
}
