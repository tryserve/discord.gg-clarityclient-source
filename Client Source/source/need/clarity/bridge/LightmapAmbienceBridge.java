package need.clarity.bridge;

import need.clarity.modules.visual.Ambience;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;

public final class LightmapAmbienceBridge {
   private LightmapAmbienceBridge() {
   }

   public static void onExtractTail(Object state) {
      try {
         if (!Ambience.isOverrideLightmapEnabled()) {
            return;
         }

         LightmapRenderState lrs = (LightmapRenderState)state;
         float[] tint = Ambience.getLightmapColour();
         float tr = tint[0];
         float tg = tint[1];
         float tb = tint[2];
         if (tr >= 1.0F && tg >= 1.0F && tb >= 1.0F) {
            return;
         }

         if (lrs.blockLightTint instanceof Vector3f v) {
            v.mul(tr, tg, tb);
         }

         if (lrs.skyLightColor instanceof Vector3f v) {
            v.mul(tr, tg, tb);
         }

         if (lrs.ambientColor instanceof Vector3f v) {
            v.mul(tr, tg, tb);
         }
      } catch (Throwable var8) {
      }
   }
}
