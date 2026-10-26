package need.clarity.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

public final class PlayerESPMaskCapture {
   private static TextureTarget maskFbo;

   private PlayerESPMaskCapture() {
   }

   public static boolean ensureFramebuffers() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         int w = mc.getWindow().getWidth();
         int h = mc.getWindow().getHeight();
         if (w > 0 && h > 0) {
            if (maskFbo == null || maskFbo.width != w || maskFbo.height != h) {
               if (maskFbo != null) {
                  RawFramebufferUtil.invalidate();
                  maskFbo.destroyBuffers();
               }

               maskFbo = new TextureTarget("clarity_esp_mask", w, h, false);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void clearMask() {
      if (maskFbo != null) {
         if (RawFramebufferUtil.bindDirect(maskFbo)) {
            GLCompat.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GLCompat.clear(16384);
         }
      }
   }

   public static void beginMaskOverride(boolean throughWalls) {
      if (maskFbo != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.renderBuffers() != null) {
            mc.renderBuffers().bufferSource().endBatch();
         }

         if (!throughWalls) {
            RenderTarget main = mc != null ? mc.getMainRenderTarget() : null;
            if (main != null) {
               RawFramebufferUtil.bindDirect(main);
            }
         }

         RenderSystem.outputColorTextureOverride = maskFbo.getColorTextureView();
         RenderSystem.outputDepthTextureOverride = null;
      }
   }

   public static void endMaskOverride() {
      RenderSystem.outputColorTextureOverride = null;
      RenderSystem.outputDepthTextureOverride = null;
   }

   public static TextureTarget getMaskFbo() {
      return maskFbo;
   }

   public static void dispose() {
      if (maskFbo != null) {
         RawFramebufferUtil.invalidate();
         maskFbo.destroyBuffers();
         maskFbo = null;
      }
   }
}
