package need.clarity.nvg;

import java.nio.ByteBuffer;
import need.clarity.Clarity;
import need.clarity.config.ClarityConfig;
import need.clarity.render.Shader;
import need.clarity.util.ThemeColors;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

public final class HudFx {
   public static final String[] STYLES = new String[]{"None", "Flow", "Aurora", "Waves", "Shimmer", "Plasma", "Stars", "Edge Light"};
   private static final int TEX_STYLES = 7;
   private static final int EDGE = 7;
   private static final int[] tex = new int[7];
   private static final int[] fbo = new int[7];
   private static final int[] img = new int[7];
   private static final long[] imgVg = new long[7];
   private static int texW;
   private static int texH;
   private static int fbW = 1;
   private static int fbH = 1;
   private static int usedLast;
   private static int usedNow;
   private static Shader shader;
   private static boolean failed;
   private static int vao;
   private static int vbo;
   private static final long START = System.nanoTime();
   private static final float[] XF = new float[6];
   private static final NVGColor C1 = NVGColor.create();
   private static final NVGColor C2 = NVGColor.create();

   private HudFx() {
   }

   public static int fbWidth() {
      return fbW;
   }

   public static int fbHeight() {
      return fbH;
   }

   public static int styleIndex(String s) {
      if (s == null) {
         return 0;
      }

      for (int i = 1; i < STYLES.length; i++) {
         if (STYLES[i].equals(s)) {
            return i;
         }
      }

      return 0;
   }

   public static int style(String module) {
      Object v = ClarityConfig.getSetting(module, "Shader");
      return v == null ? 0 : styleIndex(String.valueOf(v));
   }

   public static float opacity(String module) {
      float o = ClarityConfig.getSetting(module, "Shader Opacity") instanceof Number n ? n.floatValue() : 0.5F;
      return Math.max(0.0F, Math.min(1.0F, o));
   }

   public static boolean animating() {
      return (usedLast | usedNow) != 0;
   }

   public static void prepare(int w, int h) {
      fbW = Math.max(1, w);
      fbH = Math.max(1, h);
      usedLast = usedNow;
      usedNow = 0;
      int wanted = usedLast & -129;
      if (wanted != 0 && !failed) {
         try {
            if (shader == null) {
               shader = new Shader("hud", "fx");
            }

            int tw = Math.max(1, w / 2);
            int th = Math.max(1, h / 2);
            if (tw != texW || th != texH) {
               freeTextures();
               texW = tw;
               texH = th;
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

            GL33C.glDisable(3042);
            GL33C.glDisable(2929);
            GL33C.glDisable(3089);
            GL33C.glDisable(2960);
            GL33C.glDisable(2884);
            GL33C.glColorMask(true, true, true, true);
            GL33C.glViewport(0, 0, texW, texH);
            shader.bind();
            float time = (float)(System.nanoTime() - START) / 1.0E9F % 3600.0F;
            int a1 = ThemeColors.getAccentRgb();
            int a2 = ThemeColors.getAccent2Rgb();
            shader.setUniform1f("uTime", time);
            shader.setUniform2f("uRes", texW, texH);
            shader.setUniform3f("uAccent", (a1 >> 16 & 0xFF) / 255.0F, (a1 >> 8 & 0xFF) / 255.0F, (a1 & 0xFF) / 255.0F);
            shader.setUniform3f("uAccent2", (a2 >> 16 & 0xFF) / 255.0F, (a2 >> 8 & 0xFF) / 255.0F, (a2 & 0xFF) / 255.0F);
            GL33C.glBindVertexArray(vao);

            for (int s = 1; s < 7; s++) {
               if ((wanted & 1 << s) != 0) {
                  ensureTexture(s);
                  GL33C.glBindFramebuffer(36160, fbo[s]);
                  shader.setUniform1i("uStyle", s);
                  GL33C.glDrawArrays(4, 0, 3);
               }
            }

            GL33C.glBindVertexArray(0);
            shader.unbind();
         } catch (Throwable t) {
            failed = true;
            Clarity.LOGGER.warn("[Clarity] HUD shaders disabled after an error", t);
         }
      }
   }

   private static void ensureTexture(int s) {
      if (tex[s] == 0) {
         tex[s] = GL33C.glGenTextures();
         GL33C.glBindTexture(3553, tex[s]);
         GL33C.glTexImage2D(3553, 0, 32856, texW, texH, 0, 6408, 5121, (ByteBuffer)null);
         GL33C.glTexParameteri(3553, 10241, 9729);
         GL33C.glTexParameteri(3553, 10240, 9729);
         GL33C.glTexParameteri(3553, 10242, 33071);
         GL33C.glTexParameteri(3553, 10243, 33071);
         fbo[s] = GL33C.glGenFramebuffers();
         GL33C.glBindFramebuffer(36160, fbo[s]);
         GL33C.glFramebufferTexture2D(36160, 36064, 3553, tex[s], 0);
      }
   }

   private static void freeTextures() {
      for (int s = 0; s < 7; s++) {
         if (fbo[s] != 0) {
            GL33C.glDeleteFramebuffers(fbo[s]);
            fbo[s] = 0;
         }

         if (tex[s] != 0) {
            GL33C.glDeleteTextures(tex[s]);
            tex[s] = 0;
         }

         img[s] = 0;
      }
   }

   public static void onContextDestroyed() {
      for (int s = 0; s < 7; s++) {
         img[s] = 0;
         imgVg[s] = 0L;
      }
   }

   private static int image(long vg, int s) {
      if (tex[s] == 0) {
         return 0;
      }

      if (img[s] != 0 && imgVg[s] == vg) {
         return img[s];
      }

      img[s] = NanoVGGL3.nvglCreateImageFromHandle(vg, tex[s], texW, texH, 65544);
      imgVg[s] = vg;
      return img[s];
   }

   public static void panel(long vg, String module, float x, float y, float w, float h, float r, float alpha) {
      panel(vg, module, x, y, w, h, r, r, r, r, alpha);
   }

   public static void panel(long vg, String module, float x, float y, float w, float h, float tl, float tr, float br, float bl, float alpha) {
      int s = style(module);
      if (s != 0 && !(alpha <= 0.003F) && !(w <= 0.0F) && !(h <= 0.0F)) {
         usedNow |= 1 << s;
         float a = Math.min(1.0F, alpha * opacity(module));
         if (s == 7) {
            edge(vg, x, y, w, h, tl, tr, br, bl, a);
         } else {
            int im = image(vg, s);
            if (im > 0) {
               NanoVG.nvgCurrentTransform(vg, XF);
               float sx = XF[0];
               float sy = XF[3];
               float tx = XF[4];
               float ty = XF[5];
               if (!(Math.abs(sx) < 1.0E-6F) && !(Math.abs(sy) < 1.0E-6F)) {
                  try (MemoryStack ms = MemoryStack.stackPush()) {
                     NVGPaint p = NVGPaint.malloc(ms);
                     NanoVG.nvgImagePattern(vg, -tx / sx, -ty / sy, fbW / sx, fbH / sy, 0.0F, im, a, p);
                     NanoVG.nvgBeginPath(vg);
                     NanoVG.nvgRoundedRectVarying(vg, x, y, w, h, tl, tr, br, bl);
                     NanoVG.nvgFillPaint(vg, p);
                     NanoVG.nvgFill(vg);
                  }
               }
            }
         }
      }
   }

   private static void edge(long vg, float x, float y, float w, float h, float tl, float tr, float br, float bl, float a) {
      float t = (float)(System.nanoTime() - START) / 1.0E9F;
      float cx = x + w / 2.0F;
      float cy = y + h / 2.0F;
      float L = Math.max(w, h) * 0.5F;
      int a1 = ThemeColors.getAccentRgb();
      int a2 = ThemeColors.getAccent2Rgb();

      for (int k = 0; k < 2; k++) {
         double ang = t * 1.1 + k * Math.PI;
         float dx = (float)Math.cos(ang) * L;
         float dy = (float)Math.sin(ang) * L;
         int c = k == 0 ? a1 : a2;
         NanoVG.nvgRGBAf((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, a, C1);
         NanoVG.nvgRGBAf((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, 0.0F, C2);

         try (MemoryStack ms = MemoryStack.stackPush()) {
            NVGPaint p = NVGPaint.malloc(ms);
            NanoVG.nvgLinearGradient(vg, cx + dx, cy + dy, cx, cy, C1, C2, p);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRectVarying(vg, x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, tl, tr, br, bl);
            NanoVG.nvgStrokeWidth(vg, 1.3F);
            NanoVG.nvgStrokePaint(vg, p);
            NanoVG.nvgStroke(vg);
         }
      }
   }

   public static void dispose() {
      freeTextures();
      texH = 0;
      texW = 0;
      if (shader != null) {
         shader.delete();
         shader = null;
      }

      if (vao != 0) {
         GL33C.glDeleteVertexArrays(vao);
         GL33C.glDeleteBuffers(vbo);
         vbo = 0;
         vao = 0;
      }
   }
}
