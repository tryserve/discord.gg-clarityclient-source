package need.clarity.nvg;

import java.nio.ByteBuffer;
import java.util.Arrays;
import need.clarity.Clarity;
import need.clarity.config.ClarityConfig;
import need.clarity.render.Shader;
import need.clarity.util.ThemeColors;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

public final class LiquidGlass {
   private static final int MAX = 24;
   private static final float[] rects = new float[96];
   private static final float[] params = new float[96];
   private static int count = 0;
   private static boolean usedLast = false;
   private static int tex;
   private static int fbo;
   private static int texW;
   private static int texH;
   private static int img;
   private static int sampler;
   private static long imgVg;
   private static int vao;
   private static int vbo;
   private static Shader shader;
   private static boolean failed;
   private static final float[] XF = new float[6];

   private LiquidGlass() {
   }

   public static boolean on(String module) {
      if (failed) {
         return false;
      }

      Object v = ClarityConfig.getSetting(module, "Liquid Glass");
      return v instanceof Boolean b ? b : "true".equals(String.valueOf(v));
   }

   public static boolean active() {
      return usedLast || count > 0;
   }

   private static float num(String module, String key, float def) {
      return ClarityConfig.getSetting(module, key) instanceof Number n ? n.floatValue() : def;
   }

   public static boolean panel(long vg, String module, float x, float y, float w, float h, float r, float alpha) {
      if (on(module) && !(alpha <= 0.003F) && !(w <= 1.0F) && !(h <= 1.0F)) {
         int fbW = HudFx.fbWidth();
         int fbH = HudFx.fbHeight();
         if (!ensureTexture(fbW, fbH)) {
            return false;
         }

         int im = image(vg);
         if (im <= 0) {
            return false;
         }

         NanoVG.nvgCurrentTransform(vg, XF);
         float sx = XF[0];
         float sy = XF[3];
         float tx = XF[4];
         float ty = XF[5];
         if (!(Math.abs(sx) < 1.0E-6F) && !(Math.abs(sy) < 1.0E-6F)) {
            if (count < 24) {
               float px = x * sx + tx;
               float py = y * sy + ty;
               float pw = w * sx;
               float ph = h * sy;
               int i = count * 4;
               rects[i] = px + pw * 0.5F;
               rects[i + 1] = fbH - (py + ph * 0.5F);
               rects[i + 2] = pw * 0.5F;
               rects[i + 3] = ph * 0.5F;
               params[i] = r * sx;
               params[i + 1] = Math.max(0.0F, Math.min(2.5F, num(module, "Glass Refraction", 1.0F)));
               params[i + 2] = Math.max(0.0F, num(module, "Glass Frost", 1.5F)) * sx;
               params[i + 3] = 13.0F * sx;
               count++;
            }

            try (MemoryStack ms = MemoryStack.stackPush()) {
               NVGPaint p = NVGPaint.malloc(ms);
               NanoVG.nvgImagePattern(vg, -tx / sx, -ty / sy, fbW / sx, fbH / sy, 0.0F, im, Math.min(1.0F, alpha), p);
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
            }

            float tint = Math.max(0.0F, Math.min(1.0F, num(module, "Glass Tint", 0.22F)));
            if (tint > 0.003F) {
               int surface = ThemeColors.getSurfaceRgb();
               int a = Math.round(tint * alpha * 255.0F);
               ClarityDraw.fillRRect(vg, x, y, w, h, r, a << 24 | surface & 16777215);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void render(int sceneTex) {
      usedLast = count > 0;
      if (count != 0 && sceneTex != 0 && fbo != 0) {
         try {
            if (shader == null) {
               shader = new Shader("hud", "liquid");
            }

            if (vao == 0) {
               vao = GL33C.glGenVertexArrays();
               vbo = GL33C.glGenBuffers();
               GL33C.glBindVertexArray(vao);
               GL33C.glBindBuffer(34962, vbo);
               GL33C.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
               GL33C.glEnableVertexAttribArray(0);
               GL33C.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
            }

            if (sampler == 0) {
               sampler = GL33C.glGenSamplers();
               GL33C.glSamplerParameteri(sampler, 10241, 9729);
               GL33C.glSamplerParameteri(sampler, 10240, 9729);
               GL33C.glSamplerParameteri(sampler, 10242, 33071);
               GL33C.glSamplerParameteri(sampler, 10243, 33071);
            }

            GL33C.glDisable(3042);
            GL33C.glDisable(2929);
            GL33C.glDisable(3089);
            GL33C.glDisable(2960);
            GL33C.glDisable(2884);
            GL33C.glColorMask(true, true, true, true);
            GL33C.glBindFramebuffer(36160, fbo);
            GL33C.glViewport(0, 0, texW, texH);
            GL33C.glActiveTexture(33984);
            int prevTex = GL33C.glGetInteger(32873);
            GL33C.glBindTexture(3553, sceneTex);
            GL33C.glBindSampler(0, sampler);
            shader.bind();
            shader.setUniform1i("uScene", 0);
            shader.setUniform2f("uFb", texW, texH);
            shader.setUniform1i("uCount", count);
            shader.setUniform4fv("uRect", Arrays.copyOf(rects, count * 4));
            shader.setUniform4fv("uParam", Arrays.copyOf(params, count * 4));
            shader.setUniform1f("uDispersion", 0.35F);
            shader.setUniform1f("uHighlight", 1.0F);
            GL33C.glBindVertexArray(vao);
            GL33C.glDrawArrays(4, 0, 3);
            GL33C.glBindVertexArray(0);
            shader.unbind();
            GL33C.glBindSampler(0, 0);
            GL33C.glBindTexture(3553, prevTex);
         } catch (Throwable t) {
            failed = true;
            Clarity.LOGGER.warn("[Clarity] Liquid Glass HUD disabled after an error", t);
         } finally {
            count = 0;
         }
      } else {
         count = 0;
      }
   }

   private static boolean ensureTexture(int w, int h) {
      if (failed) {
         return false;
      }

      if (tex != 0 && texW == w && texH == h) {
         return true;
      }

      try {
         int prevTex = GL33C.glGetInteger(32873);
         if (tex == 0) {
            tex = GL33C.glGenTextures();
         }

         GL33C.glBindTexture(3553, tex);
         GL33C.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
         GL33C.glTexParameteri(3553, 10241, 9729);
         GL33C.glTexParameteri(3553, 10240, 9729);
         GL33C.glTexParameteri(3553, 10242, 33071);
         GL33C.glTexParameteri(3553, 10243, 33071);
         GL33C.glBindTexture(3553, prevTex);
         int prevFb = GL33C.glGetInteger(36006);
         if (fbo == 0) {
            fbo = GL33C.glGenFramebuffers();
         }

         GL33C.glBindFramebuffer(36160, fbo);
         GL33C.glFramebufferTexture2D(36160, 36064, 3553, tex, 0);
         boolean ok = GL33C.glCheckFramebufferStatus(36160) == 36053;
         GL33C.glBindFramebuffer(36160, prevFb);
         if (!ok) {
            failed = true;
            return false;
         } else {
            texW = w;
            texH = h;
            img = 0;
            return true;
         }
      } catch (Throwable t) {
         failed = true;
         return false;
      }
   }

   private static int image(long vg) {
      if (tex == 0) {
         return 0;
      }

      if (img != 0 && imgVg == vg) {
         return img;
      }

      img = NanoVGGL3.nvglCreateImageFromHandle(vg, tex, texW, texH, 65544);
      imgVg = vg;
      return img;
   }

   public static void onContextDestroyed() {
      img = 0;
      imgVg = 0L;
   }

   public static void dispose() {
      try {
         if (fbo != 0) {
            GL33C.glDeleteFramebuffers(fbo);
         }

         if (tex != 0) {
            GL33C.glDeleteTextures(tex);
         }

         if (sampler != 0) {
            GL33C.glDeleteSamplers(sampler);
         }

         if (vao != 0) {
            GL33C.glDeleteVertexArrays(vao);
            GL33C.glDeleteBuffers(vbo);
         }

         if (shader != null) {
            shader.delete();
         }
      } catch (Throwable var1) {
      }

      vbo = 0;
      vao = 0;
      sampler = 0;
      tex = 0;
      fbo = 0;
      shader = null;
      texH = 0;
      texW = 0;
      img = 0;
   }
}
