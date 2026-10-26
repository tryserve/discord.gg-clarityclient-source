package need.clarity.bridge;

import need.clarity.modules.visual.Ambience;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.renderer.fog.FogData;

public final class FogAmbienceBridge {
   private FogAmbienceBridge() {
   }

   public static void onSetupFogReturn(Object fogData) {
      try {
         if (fogData == null) {
            return;
         }

         FogData fog = (FogData)fogData;
         skyFog(fog);
         if (Ambience.isOverrideFogColourEnabled()) {
            float[] rgb = Ambience.getFogColour();
            fog.color.set(rgb[0], rgb[1], rgb[2], 1.0F);
         }

         if (Ambience.isOverrideFogDistanceEnabled()) {
            float endBlocks = Ambience.getFogDistanceChunks() * 16.0F;
            fog.environmentalStart = 0.0F;
            fog.environmentalEnd = endBlocks;
            fog.renderDistanceStart = 0.0F;
            fog.renderDistanceEnd = endBlocks;
            fog.skyEnd = endBlocks;
            fog.cloudEnd = endBlocks;
         }
      } catch (Throwable var3) {
      }
   }

   private static void skyFog(FogData fog) {
      try {
         if (fog.color == null) {
            return;
         }

         if (!SkyShaders.matchFog() || !SkyShaderRenderer.wasActiveLastFrame()) {
            return;
         }

         if (Ambience.isOverrideFogColourEnabled()) {
            return;
         }

         String preset = SkyShaders.preset();
         float[][] c = SkyShaders.colors();
         float[] h = SkyShaders.horizon(preset, c[0], c[1]);
         fog.color.set(h[0], h[1], h[2], fog.color.w);
         if ("Fog".equals(preset)) {
            float end = SkyShaders.fogDistance();
            float start = end * 0.04F;
            fog.environmentalStart = Math.min(fog.environmentalStart, start);
            fog.environmentalEnd = Math.min(fog.environmentalEnd, end);
            fog.renderDistanceStart = Math.min(fog.renderDistanceStart, start);
            fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, end);
            fog.cloudEnd = Math.min(fog.cloudEnd, end);
         }
      } catch (Throwable var6) {
      }
   }
}
