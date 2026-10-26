package need.clarity.mixin;

import need.clarity.PerFrameEvents;
import need.clarity.Unload;
import need.clarity.modules.visual.HandShaders;
import need.clarity.render.GLCompat;
import need.clarity.render.ShaderHelper;
import need.clarity.render.handshader.HandShaderConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 1001)
public class GameRendererMixin {
   @Inject(method = "renderLevel", at = @At("HEAD"))
   public void clarity$firePerFrame(DeltaTracker tickCounter, CallbackInfo ci) {
      PerFrameEvents.fire();
   }

   @Inject(
      method = "renderLevel",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V")
   )
   public void beforeRenderHand(DeltaTracker tickCounter, CallbackInfo ci) {
      if (Unload.isUnloaded()) {
         HandShaderConfig.enabled = false;
      } else {
         if (!ShaderHelper.isInitialized()) {
            ShaderHelper.initShadersIfNeeded();
         }

         HandShaders hands = HandShaders.INSTANCE;
         if (hands != null && hands.isEnabled()) {
            boolean isGlow = false;
            HandShaderConfig.enabled = isGlow;
            HandShaderConfig.applyToArm = isGlow && HandShaders.getGlowApplyToArm();
            HandShaderConfig.applyToItems = isGlow && HandShaders.getGlowApplyToItems();
            HandShaderConfig.color = HandShaders.getGlowColor();
            HandShaderConfig.opacity = HandShaders.getGlowOpacity();
            HandShaderConfig.textured = HandShaders.getGlowTextured();
            HandShaderConfig.fullbright = HandShaders.getGlowFullbright();
            HandShaderConfig.outline = HandShaders.getGlowOutline();
            HandShaderConfig.outlineColor = HandShaders.getGlowOutlineColor();
            HandShaderConfig.outlineWidth = HandShaders.getGlowOutlineWidth();
            HandShaderConfig.outlineGlow = HandShaders.getGlowBloom();
            HandShaderConfig.glowSpread = HandShaders.getGlowSpread();
            HandShaderConfig.antialiasing = HandShaders.getGlowAntialiasing();
            if ("Invert".equals(HandShaders.getShader()) && HandShaders.getInvertHideHand()) {
               GLCompat.forceColorMaskOff(true);
            }

            ShaderHelper.captureBackground();
         } else {
            HandShaderConfig.enabled = false;
         }
      }
   }

   @Inject(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
         shift = Shift.AFTER
      )
   )
   public void afterRenderHand(DeltaTracker tickCounter, CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         HandShaders hands = HandShaders.INSTANCE;
         if (hands != null && hands.isEnabled()) {
            if (GLCompat.isColorMaskForcedOff()) {
               GLCompat.forceColorMaskOff(false);
            }

            ShaderHelper.checkFramebuffers();
            hands.render();
         }
      }
   }
}
