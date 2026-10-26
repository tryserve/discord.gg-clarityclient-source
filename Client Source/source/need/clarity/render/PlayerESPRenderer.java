package need.clarity.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import need.clarity.modules.visual.PlayerESP;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;

public final class PlayerESPRenderer {
   private static Shader espOutlineShader;
   private static boolean initialized = false;
   private static boolean initFailed = false;

   private PlayerESPRenderer() {
   }

   public static boolean isInitialized() {
      return initialized;
   }

   public static void initIfNeeded() {
      if (!initialized && !initFailed) {
         try {
            espOutlineShader = new Shader("hand", "esp_outline");
            initialized = true;
         } catch (Exception e) {
            initFailed = true;
         }
      }
   }

   public static void render(PoseStack poseStack, List<ClarityRenderUtils.ESPEntry> entries, float partialTick) {
      if (!entries.isEmpty()) {
         boolean throughWalls = PlayerESP.isThroughWalls();
         boolean corners = "Corners".equals(PlayerESP.getBoxStyle());
         boolean drawFill = "3D".equals(PlayerESP.getMode()) || PlayerESP.getFillEnabled();
         boolean drawLines = PlayerESP.getOutlineEnabled();
         int fillColor = PlayerESP.getFillColor();
         float fillOpacity = PlayerESP.getFillOpacity();
         int fillArgb = Math.round(clamp01(fillOpacity) * 255.0F) << 24 | fillColor & 16777215;
         ClarityRenderUtils.drawEspBoxes(poseStack, entries, drawFill, fillArgb, drawLines, throughWalls, corners, PlayerESP.getOutlineThickness());
      }
   }

   private static void renderScreenOutline(PoseStack poseStack, List<ClarityRenderUtils.ESPEntry> entries, boolean throughWalls) {
      boolean outlineEnabled = PlayerESP.getOutlineEnabled();
      boolean fillEnabled = PlayerESP.getFillEnabled();
      if (outlineEnabled || fillEnabled) {
         if (!initialized) {
            if (initFailed) {
               return;
            }

            initIfNeeded();
            if (!initialized) {
               return;
            }
         }

         if (PlayerESPMaskCapture.ensureFramebuffers()) {
            try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
               PlayerESPMaskCapture.clearMask();
               PlayerESPMaskCapture.beginMaskOverride(throughWalls);

               try {
                  ClarityRenderUtils.drawEspMask(poseStack, entries, throughWalls);
               } finally {
                  PlayerESPMaskCapture.endMaskOverride();
               }

               renderOutline();
            } catch (Exception var15) {
            }
         }
      }
   }

   private static void renderOutline() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      TextureTarget maskFbo = PlayerESPMaskCapture.getMaskFbo();
      if (mainFbo != null && maskFbo != null && espOutlineShader != null) {
         if (RawFramebufferUtil.bindDirect(mainFbo)) {
            GLCompat.enableBlend();
            GLCompat.defaultBlendFunc();
            GLCompat.disableDepthTest();
            GLCompat.activeTexture(33984);
            GLCompat.bindTexture(maskFbo.getColorTexture());
            int fillColor = PlayerESP.getFillColor();
            espOutlineShader.bind();
            espOutlineShader.setUniform1i("PlayerMask", 0);
            espOutlineShader.setUniform1f("outlineThickness", PlayerESP.getOutlineThickness());
            espOutlineShader.setUniformBool("outlineEnabled", PlayerESP.getOutlineEnabled());
            espOutlineShader.setUniformBool("fillEnabled", PlayerESP.getFillEnabled());
            espOutlineShader.setUniform3f("fillColor", red(fillColor), green(fillColor), blue(fillColor));
            espOutlineShader.setUniform1f("fillOpacity", PlayerESP.getFillOpacity());
            drawFullScreenQuad();
            espOutlineShader.unbind();
         }
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float red(int argb) {
      return (argb >> 16 & 0xFF) / 255.0F;
   }

   private static float green(int argb) {
      return (argb >> 8 & 0xFF) / 255.0F;
   }

   private static float blue(int argb) {
      return (argb & 0xFF) / 255.0F;
   }

   private static void drawFullScreenQuad() {
      RenderSystem.assertOnRenderThread();
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      buf.addVertex(-1.0F, -1.0F, 0.0F);
      buf.addVertex(1.0F, -1.0F, 0.0F);
      buf.addVertex(1.0F, 1.0F, 0.0F);
      buf.addVertex(-1.0F, 1.0F, 0.0F);
      RawShaderDrawUtil.draw(buf.buildOrThrow());
   }
}
