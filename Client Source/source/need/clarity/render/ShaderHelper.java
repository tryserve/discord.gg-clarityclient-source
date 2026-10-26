package need.clarity.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.function.Consumer;
import need.clarity.modules.visual.HandShaders;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.handshader.HandFx;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

public final class ShaderHelper {
   private static Shader chromaShader;
   private static Shader prismShader;
   private static Shader pixelShader;
   private static Shader galaxyShader;
   private static Shader neonShader;
   private static Shader abyssShader;
   private static Shader invertShader;
   private static Shader edgeGlowShader;
   private static Shader outlineShader;
   private static Shader jfaInitShader;
   private static Shader jfaStepShader;
   private static Shader jfaCompositeShader;
   private static TextureTarget copyFbo;
   private static TextureTarget jfaFboA;
   private static TextureTarget jfaFboB;
   private static boolean initialized = false;

   private ShaderHelper() {
   }

   public static boolean isInitialized() {
      return initialized;
   }

   public static void initShadersIfNeeded() {
      if (!initialized) {
         try {
            chromaShader = new Shader("hand", "chroma");
            prismShader = new Shader("hand", "prism");
            pixelShader = new Shader("hand", "pixelate");
            galaxyShader = new Shader("hand", "galaxy");
            neonShader = new Shader("hand", "neon");

            try {
               abyssShader = new Shader("hand", "abyss");
            } catch (Exception ignored) {
               abyssShader = null;
            }

            invertShader = new Shader("hand", "invert");
            outlineShader = new Shader("hand", "outline");
            jfaInitShader = new Shader("hand", "jfa_init");
            jfaStepShader = new Shader("hand", "jfa_step");
            jfaCompositeShader = new Shader("hand", "jfa_composite");
            initialized = true;
         } catch (Exception var2) {
         }
      }
   }

   public static void checkFramebuffers() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         int w = mc.getWindow().getWidth();
         int h = mc.getWindow().getHeight();
         boolean stale = copyFbo != null && (copyFbo.width != w || copyFbo.height != h)
            || jfaFboA != null && (jfaFboA.width != w || jfaFboA.height != h)
            || jfaFboB != null && (jfaFboB.width != w || jfaFboB.height != h);
         if (stale) {
            RawFramebufferUtil.invalidate();
         }

         if (copyFbo == null || copyFbo.width != w || copyFbo.height != h) {
            if (copyFbo != null) {
               copyFbo.destroyBuffers();
            }

            copyFbo = new TextureTarget("clarity_hand_copy", w, h, true);
         }

         if (jfaFboA == null || jfaFboA.width != w || jfaFboA.height != h) {
            if (jfaFboA != null) {
               jfaFboA.destroyBuffers();
            }

            jfaFboA = new TextureTarget("clarity_jfa_a", w, h, false);
         }

         if (jfaFboB == null || jfaFboB.width != w || jfaFboB.height != h) {
            if (jfaFboB != null) {
               jfaFboB.destroyBuffers();
            }

            jfaFboB = new TextureTarget("clarity_jfa_b", w, h, false);
         }
      }
   }

   public static void render() {
      if (initialized) {
         try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
            String shaderName = HandShaders.getShader();
            switch (shaderName) {
               case "Chroma":
                  renderChroma();
                  break;
               case "Prism":
                  renderPrism();
                  break;
               case "Pixelate":
                  renderSimple(pixelShader, sh -> sh.setUniform1f("pixelSize", HandShaders.getPixelSize()));
                  break;
               case "Galaxy":
                  renderSimple(galaxyShader, ShaderHelper::themeUniforms);
                  break;
               case "Neon":
                  renderSimple(neonShader, ShaderHelper::themeUniforms);
                  break;
               case "Abyss":
                  renderSimple(abyssShader, sh -> {
                     sh.setUniform3f("uLight", colorToVec(HandShaders.getAbyssLight()));
                     sh.setUniform3f("uDeep", colorToVec(HandShaders.getAbyssDeep()));
                     sh.setUniform1f("uCaustics", HandShaders.getAbyssCaustics());
                     sh.setUniform1f("uEdgeLine", HandShaders.getAbyssEdgeLine());
                     sh.setUniform1f("uLineSpeed", HandShaders.getAbyssLineSpeed());
                  });
                  break;
               case "Invert":
                  renderInvert();
                  break;
               case "Outline":
                  renderOutline();
                  break;
               case "Glass":
               case "Glow":
               case "Hologram":
               case "Blur":
               case "Wisp":
               case "Liquid Glass":
               case "Beat Pulse":
               case "Trails":
                  renderFx(shaderName);
            }
         } catch (Exception var6) {
         }
      }
   }

   public static void captureBackground() {
      HandFx.captureBackground();
   }

   private static void renderFx(String name) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            HandFx.render(name, mainFbo, copyFbo);
            cleanupShaderRender();
         }
      }
   }

   private static boolean prepareShaderRender(RenderTarget mainFbo, TextureTarget fbo) {
      RawFramebufferUtil.clear(fbo);
      if (!RawFramebufferUtil.bindDirect(fbo)) {
         return false;
      }

      GLCompat.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GLCompat.clear(16640);
      RawFramebufferUtil.copy(mainFbo, fbo, true);
      if (!RawFramebufferUtil.bindDirect(mainFbo)) {
         return false;
      }

      GLCompat.enableBlend();
      GLCompat.defaultBlendFunc();
      GLCompat.disableDepthTest();
      return true;
   }

   private static void bindShaderTextures(TextureTarget fbo) {
      GLCompat.activeTexture(33984);
      GLCompat.bindTexture(fbo.getColorTexture());
      GLCompat.activeTexture(33985);
      GLCompat.bindTexture(fbo.getDepthTexture());
   }

   private static void cleanupShaderRender() {
      GLCompat.activeTexture(33984);
      GLCompat.disableBlend();
      GLCompat.enableDepthTest();
      Minecraft mc = Minecraft.getInstance();
      if (mc.getMainRenderTarget() != null) {
         RawFramebufferUtil.bindDirect(mc.getMainRenderTarget());
      }
   }

   private static Vector3f colorToVec(int argb) {
      Color c = new Color(argb, true);
      return new Vector3f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
   }

   private static void renderChroma() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && chromaShader != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            bindShaderTextures(copyFbo);
            chromaShader.bind();
            chromaShader.setUniform1i("ColorTexture", 0);
            chromaShader.setUniform1i("DepthTexture", 1);
            chromaShader.setUniform1f("time", (float)System.nanoTime() / 1.0E9F);
            chromaShader.setUniform1f("chromaSpeed", HandShaders.getChromaSpeed());
            chromaShader.setUniform1f("chromaSaturation", HandShaders.getChromaSaturation());
            chromaShader.setUniform1f("chromaBrightness", HandShaders.getChromaBrightness());
            drawFullScreenQuad();
            chromaShader.unbind();
            cleanupShaderRender();
         }
      }
   }

   private static void renderPrism() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && prismShader != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            bindShaderTextures(copyFbo);
            float[][] tc = SkyShaders.themeBlend();
            prismShader.bind();
            prismShader.setUniform1i("ColorTexture", 0);
            prismShader.setUniform1i("DepthTexture", 1);
            prismShader.setUniform1f("time", (float)(System.nanoTime() / 1.0E9 % 3600.0));
            prismShader.setUniform1f("aspect", (float)mainFbo.width / Math.max(1, mainFbo.height));
            prismShader.setUniform3f("uAccent", tc[0][0], tc[0][1], tc[0][2]);
            prismShader.setUniform3f("uAccent2", tc[1][0], tc[1][1], tc[1][2]);
            drawFullScreenQuad();
            prismShader.unbind();
            cleanupShaderRender();
         }
      }
   }

   private static void themeUniforms(Shader sh) {
      float[][] tc = SkyShaders.themeBlend();
      sh.setUniform3f("uAccent", tc[0][0], tc[0][1], tc[0][2]);
      sh.setUniform3f("uAccent2", tc[1][0], tc[1][1], tc[1][2]);
   }

   private static void renderSimple(Shader sh, Consumer<Shader> uniforms) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && sh != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            bindShaderTextures(copyFbo);
            sh.bind();
            sh.setUniform1i("ColorTexture", 0);
            sh.setUniform1i("DepthTexture", 1);
            sh.setUniform1f("time", (float)(System.nanoTime() / 1.0E9 % 3600.0));
            sh.setUniform1f("aspect", (float)mainFbo.width / Math.max(1, mainFbo.height));
            sh.setUniform2f("texel", 1.0F / Math.max(1, mainFbo.width), 1.0F / Math.max(1, mainFbo.height));
            sh.setUniform1f("pxScale", mainFbo.height / 1080.0F);
            uniforms.accept(sh);
            drawFullScreenQuad();
            sh.unbind();
            cleanupShaderRender();
         }
      }
   }

   private static void renderInvert() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && invertShader != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            bindShaderTextures(copyFbo);
            invertShader.bind();
            invertShader.setUniform1i("ColorTexture", 0);
            invertShader.setUniform1i("DepthTexture", 1);
            invertShader.setUniform1f("time", (float)System.nanoTime() / 1.0E9F);
            invertShader.setUniformBool("hideHand", HandShaders.getInvertHideHand());
            invertShader.setUniform1f("invertIntensity", HandShaders.getInvertIntensity());
            invertShader.setUniformBool("invertHue", HandShaders.getInvertHue());
            invertShader.setUniformBool("invertSaturation", HandShaders.getInvertSaturation());
            invertShader.setUniformBool("invertBrightness", HandShaders.getInvertBrightness());
            invertShader.setUniform1f("edgeGlow", HandShaders.getInvertEdgeGlow());
            invertShader.setUniform3f("glowColor", colorToVec(HandShaders.getInvertGlowColor()));
            drawFullScreenQuad();
            invertShader.unbind();
            cleanupShaderRender();
         }
      }
   }

   private static void renderEdgeGlow() {
   }

   private static void renderOutline() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainFbo = mc.getMainRenderTarget();
      if (mainFbo != null && outlineShader != null && copyFbo != null) {
         if (prepareShaderRender(mainFbo, copyFbo)) {
            bindShaderTextures(copyFbo);
            outlineShader.bind();
            outlineShader.setUniform1i("ColorTexture", 0);
            outlineShader.setUniform1i("DepthTexture", 1);
            outlineShader.setUniform3f("outlineColor", colorToVec(HandShaders.getOutlineColor()));
            outlineShader.setUniform1f("outlineThickness", HandShaders.getOutlineThickness());
            outlineShader.setUniformBool("tintEnabled", HandShaders.getTintEnabled());
            outlineShader.setUniform3f("tintColor", colorToVec(HandShaders.getTintColor()));
            outlineShader.setUniform1f("tintAmount", HandShaders.getTintAmount());
            drawFullScreenQuad();
            outlineShader.unbind();
            cleanupShaderRender();
         }
      }
   }

   public static void drawFullScreenQuad() {
      RenderSystem.assertOnRenderThread();
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      buf.addVertex(-1.0F, -1.0F, 0.0F);
      buf.addVertex(1.0F, -1.0F, 0.0F);
      buf.addVertex(1.0F, 1.0F, 0.0F);
      buf.addVertex(-1.0F, 1.0F, 0.0F);
      RawShaderDrawUtil.draw(buf.buildOrThrow());
   }
}
