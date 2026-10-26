package need.clarity.mixin;

import need.clarity.modules.visual.Ambience;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyAmbienceMixin {
   @Inject(method = "extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V", at = @At("TAIL"))
   private void ambience$overrideSkyColor(ClientLevel level, float partialTick, Camera camera, SkyRenderState skyRenderState, CallbackInfo ci) {
      if (Ambience.isOverrideTimeEnabled()) {
         long dayTime = Ambience.getOverriddenDayTime();
         float fraction = (float)dayTime / 24000.0F;
         float angleDeg = fraction * 360.0F;
         float angleRad = angleDeg * (float) (Math.PI / 180.0);
         skyRenderState.sunAngle = angleRad;
         skyRenderState.moonAngle = angleRad + (float) Math.PI;
         skyRenderState.starAngle = angleRad;
      }

      if (Ambience.isOverrideSkyColourEnabled()) {
         float[] c1 = Ambience.getSkyColour();
         if (!Ambience.isSkyGradientEnabled()) {
            skyRenderState.skyColor = toArgb(c1[0], c1[1], c1[2]);
         } else {
            float[] c2 = Ambience.getSkyGradientColour2();
            float pitch = camera.xRot();
            float t = (float)Math.max(0.0, Math.min(1.0, -pitch / 90.0));
            skyRenderState.skyColor = toArgb(lerp(c1[0], c2[0], t), lerp(c1[1], c2[1], t), lerp(c1[2], c2[2], t));
         }
      }
   }

   private static int toArgb(float r, float g, float b) {
      int ri = Math.max(0, Math.min(255, (int)(r * 255.0F)));
      int gi = Math.max(0, Math.min(255, (int)(g * 255.0F)));
      int bi = Math.max(0, Math.min(255, (int)(b * 255.0F)));
      return 0xFF000000 | ri << 16 | gi << 8 | bi;
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }
}
