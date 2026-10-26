package need.clarity.bridge;

import need.clarity.modules.visual.Ambience;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.SkyRenderState;

public final class SkyAmbienceBridge {
   private SkyAmbienceBridge() {
   }

   public static void onExtractSkyTail(Object level, float partialTick, Object camera, Object skyState) {
      try {
         SkyRenderState state = (SkyRenderState)skyState;
         if (Ambience.isOverrideTimeEnabled()) {
            long dayTime = Ambience.getOverriddenDayTime();
            float fraction = (float)dayTime / 24000.0F;
            float angleDeg = fraction * 360.0F;
            float angleRad = angleDeg * (float) (Math.PI / 180.0);
            state.sunAngle = angleRad;
            state.moonAngle = angleRad + (float) Math.PI;
            state.starAngle = angleRad;
         }

         if (!Ambience.isOverrideSkyColourEnabled()) {
            return;
         }

         float[] c1 = Ambience.getSkyColour();
         if (!Ambience.isSkyGradientEnabled()) {
            state.skyColor = toArgb(c1[0], c1[1], c1[2]);
            return;
         }

         float[] c2 = Ambience.getSkyGradientColour2();
         float pitch = ((Camera)camera).xRot();
         float t = (float)Math.max(0.0, Math.min(1.0, -pitch / 90.0));
         state.skyColor = toArgb(c1[0] + (c2[0] - c1[0]) * t, c1[1] + (c2[1] - c1[1]) * t, c1[2] + (c2[2] - c1[2]) * t);
      } catch (Throwable var10) {
      }
   }

   private static int toArgb(float r, float g, float b) {
      int ri = Math.max(0, Math.min(255, (int)(r * 255.0F)));
      int gi = Math.max(0, Math.min(255, (int)(g * 255.0F)));
      int bi = Math.max(0, Math.min(255, (int)(b * 255.0F)));
      return 0xFF000000 | ri << 16 | gi << 8 | bi;
   }
}
