package need.clarity.menu;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.Map;
import need.clarity.clickgui.Canvas;
import need.clarity.clickgui.ConfigStore;
import need.clarity.clickgui.GuiData;
import need.clarity.clickgui.NvgCanvas;
import need.clarity.clickgui.RawGlState;
import need.clarity.clickgui.Theme;
import need.clarity.modules.visual.MainMenu;
import need.clarity.nvg.ClarityFonts;
import need.clarity.render.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;

public final class MenuRenderer {
   private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath("clarity", "menu-overlay");
   private static final long START = System.nanoTime();
   private static long vg;
   private static NvgCanvas canvas;
   private static NVGLUFramebuffer fb;
   private static int fbW;
   private static int fbH;
   private static Shader bgShader;
   private static Shader compShader;
   private static boolean failed = false;
   private static boolean bgFailed = false;
   private static int vao = 0;
   private static int vbo = 0;
   private static final float BG_HZ = 30.0F;
   private static final float MARGIN = 0.03F;
   private static final int[] bgTex = new int[2];
   private static final int[] bgFbo = new int[2];
   private static int bgW = 0;
   private static int bgH = 0;
   private static int cur = 0;
   private static double t0 = 0.0;
   private static double t1 = 0.0;
   private static int lastStyle = -1;
   private static boolean bgValid = false;
   private static float parX = 0.0F;
   private static float parY = 0.0F;
   private static float spotX = 0.5F;
   private static float spotY = 0.5F;
   private static float spotA = 0.0F;
   private static long lastNanos = 0L;
   private static double shaderTime = 0.0;
   private static int[] tint = new int[]{8037111, 4020641};
   private static long tintAt = 0L;

   private MenuRenderer() {
   }

   public static boolean hasFailed() {
      return failed;
   }

   public static float designScale() {
      Minecraft mc = Minecraft.getInstance();
      return Math.max(0.5F, Math.min(3.0F, Math.max(1, mc.getWindow().getHeight()) / 1080.0F));
   }

   public static float toDesign(double guiCoord) {
      Minecraft mc = Minecraft.getInstance();
      return (float)(guiCoord * mc.getWindow().getGuiScale() / designScale());
   }

   public static float time() {
      return (float)((System.nanoTime() - START) / 1.0E9);
   }

   public static void render(GuiGraphicsExtractor ctx, float mouseDX, float mouseDY, MenuRenderer.Ui ui) {
      if (!failed) {
         Minecraft mc = Minecraft.getInstance();
         int physW = mc.getWindow().getWidth();
         int physH = mc.getWindow().getHeight();
         if (physW > 0 && physH > 0) {
            float ds = designScale();
            float dw = physW / ds;
            float dh = physH / ds;
            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
            lastNanos = now;
            shaderTime = shaderTime + dt * MainMenu.speed();
            float tx = MainMenu.parallax() ? mouseDX / dw * 2.0F - 1.0F : 0.0F;
            float ty = MainMenu.parallax() ? mouseDY / dh * 2.0F - 1.0F : 0.0F;
            float k = 1.0F - (float)Math.exp(-dt * 4.0F);
            parX = parX + (tx - parX) * k;
            parY = parY + (ty - parY) * k;
            boolean hasMouse = mouseDX >= 0.0F && mouseDY >= 0.0F;
            float ks = 1.0F - (float)Math.exp(-dt * 9.0F);
            if (hasMouse) {
               spotX = spotX + (mouseDX / dw - spotX) * ks;
               spotY = spotY + (1.0F - mouseDY / dh - spotY) * ks;
            }

            spotA = spotA + ((hasMouse ? 1.0F : 0.0F) - spotA) * k;

            try (RawGlState ignored = RawGlState.capture()) {
               ensure(mc, physW, physH);

               for (int u = 0; u < 4; u++) {
                  GL33C.glBindSampler(u, 0);
               }

               boolean bgOk = drawBackgroundPass(physW, physH);
               NanoVGGL3.nvgluBindFramebuffer(vg, fb);
               GL33C.glViewport(0, 0, fbW, fbH);
               GL33C.glDisable(3089);
               GL33C.glColorMask(true, true, true, true);
               GL33C.glStencilMask(255);
               GL33C.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
               GL33C.glClearStencil(0);
               GL33C.glClear(17408);
               GL33C.glDisable(2929);
               GL33C.glDisable(2884);
               GL33C.glBindBuffer(35052, 0);
               GL33C.glPixelStorei(3317, 1);
               GL33C.glPixelStorei(3314, 0);
               GL33C.glPixelStorei(3316, 0);
               GL33C.glPixelStorei(3315, 0);

               for (int u = 0; u < 4; u++) {
                  GL33C.glBindSampler(u, 0);
               }

               GL33C.glActiveTexture(33984);
               if (bgOk) {
                  composite(physW, physH);
               }

               NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
               canvas.begin(fbW, fbH);
               NanoVG.nvgSave(vg);
               NanoVG.nvgScale(vg, ds, ds);
               ui.draw(canvas, dw, dh);
               NanoVG.nvgRestore(vg);
               NanoVG.nvgEndFrame(vg);
            } catch (Throwable t) {
               failed = true;
               return;
            }

            float gs = mc.getWindow().getGuiScale();
            ctx.pose().pushMatrix();
            ctx.pose().scale(1.0F / gs, 1.0F / gs);
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, OVERLAY_ID, 0, 0, 0.0F, fbH, fbW, fbH, fbW, -fbH, fbW, fbH);
            ctx.pose().popMatrix();
         }
      }
   }

   private static int style() {
      return switch (MainMenu.background()) {
         case "Grain" -> 1;
         case "Dots" -> 2;
         case "Aurora" -> 3;
         case "Nebula" -> 4;
         case "Contours" -> 5;
         case "Rain" -> 6;
         case "Bokeh" -> 7;
         default -> 0;
      };
   }

   private static boolean drawBackgroundPass(int w, int h) {
      if (bgFailed) {
         return false;
      }

      try {
         if (bgShader == null) {
            bgShader = new Shader("menu", "bg");
         }

         if (compShader == null) {
            compShader = new Shader("menu", "composite");
         }
      } catch (Exception e) {
         bgFailed = true;
         return false;
      }

      int style = style();
      float scale = style != 2 && style != 5 && style != 6 ? MainMenu.quality() : 1.0F;
      int tw = Math.max(1, Math.round(w * scale * 1.06F));
      int th = Math.max(1, Math.round(h * scale * 1.06F));
      if (ensureBgTargets(tw, th) || style != lastStyle) {
         bgValid = false;
      }

      lastStyle = style;
      double step = Math.max(0.001, MainMenu.speed()) / 30.0;
      double now = shaderTime;
      if (!bgValid) {
         t0 = Math.floor(now / step) * step;
         t1 = t0 + step;
         renderBg(cur ^ 1, t0, w, h, style, tw, th);
         renderBg(cur, t1, w, h, style, tw, th);
         bgValid = true;
      } else if (now >= t1) {
         if (now >= t1 + step) {
            bgValid = false;
            return drawBackgroundPass(w, h);
         }

         cur ^= 1;
         t0 = t1;
         t1 = t0 + step;
         renderBg(cur, t1, w, h, style, tw, th);
      }

      return true;
   }

   private static void renderBg(int slot, double time, int w, int h, int style, int tw, int th) {
      GL33C.glBindFramebuffer(36160, bgFbo[slot]);
      GL33C.glViewport(0, 0, tw, th);
      GL33C.glDisable(3042);
      GL33C.glDisable(3089);
      bgShader.bind();
      bgShader.setUniform1f("uTime", (float)(time % 3600.0));
      bgShader.setUniform2f("uRes", w, h);
      bgShader.setUniform1f("uMargin", 0.03F);
      bgShader.setUniform1i("uStyle", style);
      fullscreenTriangle();
      bgShader.unbind();
   }

   private static void composite(int w, int h) {
      GL33C.glViewport(0, 0, w, h);
      GL33C.glDisable(3042);
      GL33C.glActiveTexture(33984);
      GL33C.glBindTexture(3553, bgTex[cur ^ 1]);
      GL33C.glActiveTexture(33985);
      GL33C.glBindTexture(3553, bgTex[cur]);
      GL33C.glActiveTexture(33984);
      float span = 1.06F;
      float aspect = (float)w / h;
      float mix = (float)Math.max(0.0, Math.min(1.0, (shaderTime - t0) / (t1 - t0)));
      compShader.bind();
      compShader.setUniform1i("uBgA", 0);
      compShader.setUniform1i("uBgB", 1);
      compShader.setUniform1f("uMix", mix);
      compShader.setUniform2f("uShift", parX * 0.025F / aspect / span, -parY * 0.025F / span);
      compShader.setUniform1f("uInvSpan", 1.0F / span);
      compShader.setUniform2f("uRes", w, h);
      compShader.setUniform1f("uTime", time());
      compShader.setUniform1f("uGrain", style() == 1 ? 0.09F : 0.018F);
      compShader.setUniform1f("uFade", Math.min(1.0F, time() / 1.2F));
      compShader.setUniform3f("uSpot", spotX, spotY, spotA * MainMenu.spotlight());
      int[] th = themeTint();
      compShader.setUniform3f("uTint", (th[0] >> 16 & 0xFF) / 255.0F, (th[0] >> 8 & 0xFF) / 255.0F, (th[0] & 0xFF) / 255.0F);
      compShader.setUniform3f("uTint2", (th[1] >> 16 & 0xFF) / 255.0F, (th[1] >> 8 & 0xFF) / 255.0F, (th[1] & 0xFF) / 255.0F);
      fullscreenTriangle();
      compShader.unbind();
      GL33C.glActiveTexture(33985);
      GL33C.glBindTexture(3553, 0);
      GL33C.glActiveTexture(33984);
      GL33C.glBindTexture(3553, 0);
   }

   private static int[] themeTint() {
      long now = System.nanoTime();
      if (now - tintAt > 250000000L) {
         tintAt = now;

         try {
            Map<String, String> m = new ConfigStore(GuiData.get()).theme();
            tint = new int[]{Theme.hex(m.get("accent"), 8037111), Theme.hex(m.get("accent2"), 4020641)};
         } catch (Throwable var3) {
         }
      }

      return tint;
   }

   private static void fullscreenTriangle() {
      if (vao == 0) {
         vao = GL33C.glGenVertexArrays();
         vbo = GL33C.glGenBuffers();
         GL33C.glBindVertexArray(vao);
         GL33C.glBindBuffer(34962, vbo);
         GL33C.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
         GL33C.glEnableVertexAttribArray(0);
         GL33C.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
      }

      GL33C.glBindVertexArray(vao);
      GL33C.glDrawArrays(4, 0, 3);
      GL33C.glBindVertexArray(0);
   }

   private static boolean ensureBgTargets(int w, int h) {
      if (bgFbo[0] != 0 && bgW == w && bgH == h) {
         return false;
      }

      GL33C.glActiveTexture(33984);
      GL33C.glBindBuffer(35052, 0);

      for (int i = 0; i < 2; i++) {
         if (bgTex[i] == 0) {
            bgTex[i] = GL33C.glGenTextures();
         }

         if (bgFbo[i] == 0) {
            bgFbo[i] = GL33C.glGenFramebuffers();
         }

         GL33C.glBindTexture(3553, bgTex[i]);
         GL33C.glTexImage2D(3553, 0, 33321, w, h, 0, 6403, 5121, (ByteBuffer)null);
         GL33C.glTexParameteri(3553, 10241, 9729);
         GL33C.glTexParameteri(3553, 10240, 9729);
         GL33C.glTexParameteri(3553, 10242, 33071);
         GL33C.glTexParameteri(3553, 10243, 33071);
         GL33C.glTexParameteri(3553, 33085, 0);
         GL33C.glBindFramebuffer(36160, bgFbo[i]);
         GL33C.glFramebufferTexture2D(36160, 36064, 3553, bgTex[i], 0);
      }

      GL33C.glBindTexture(3553, 0);
      bgW = w;
      bgH = h;
      return true;
   }

   public static float parallaxX(float depth) {
      return parX * depth;
   }

   public static float parallaxY(float depth) {
      return parY * depth;
   }

   private static void ensure(Minecraft mc, int w, int h) {
      if (vg == 0L) {
         vg = NanoVGGL3.nvgCreate(3);
         if (vg == 0L) {
            throw new IllegalStateException("nvgCreate failed");
         }

         ClarityFonts.loadInto(vg);
         canvas = new NvgCanvas(vg);
         canvas.setCrisp(true);
      }

      if (fb == null || fbW != w || fbH != h) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         fb = NanoVGGL3.nvgluCreateFramebuffer(vg, w, h, 0);
         if (fb == null) {
            throw new IllegalStateException("nvgluCreateFramebuffer failed");
         }

         fbW = w;
         fbH = h;
         GlTexture tex = new MenuRenderer.BridgeGlTexture(fb.texture(), fbW, fbH);
         GpuTextureView view = RenderSystem.getDevice().createTextureView(tex);
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
         mc.getTextureManager().register(OVERLAY_ID, new MenuRenderer.BridgeTexture(tex, view, sampler));
      }
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId, int w, int h) {
         super(12, "clarity-menu-overlay", TextureFormat.RGBA8, w, h, 1, 1, glId);
      }

      public void close() {
      }
   }

   private static final class BridgeTexture extends AbstractTexture {
      BridgeTexture(GpuTexture tex, GpuTextureView view, GpuSampler sampler) {
         this.texture = tex;
         this.textureView = view;
         this.sampler = sampler;
      }

      public void close() {
      }
   }

   public interface Ui {
      void draw(Canvas var1, float var2, float var3);
   }
}
