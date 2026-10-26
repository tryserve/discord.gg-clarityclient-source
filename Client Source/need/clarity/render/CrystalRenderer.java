package need.clarity.render;

import java.awt.Color;
import need.clarity.modules.visual.CrystalView;
import need.clarity.render.worldfx.WorldFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.world.phys.Vec3;

public final class CrystalRenderer {
   public static final String CRYSTAL = "crystal";
   public static final String POP = "crystalpop";
   private static final float[] R = new float[3];
   private static final float[] U = new float[3];
   private static final float[] F = new float[3];
   private static final float[] A = new float[3];
   private static final float[] B = new float[3];
   private static final float[] FLAT_R = new float[]{1.0F, 0.0F, 0.0F};
   private static final float[] FLAT_U = new float[]{0.0F, 0.0F, 1.0F};
   private static final long START = System.nanoTime();
   private static final String[] SHAPE_SHADERS = variants("crystal", 4);
   private static final String[] SKIN_SHADERS = variants("crystalskin", 13);

   private CrystalRenderer() {
   }

   private static String[] variants(String base, int n) {
      String[] out = new String[n];

      for (int i = 0; i < n; i++) {
         out[i] = base + "#" + i;
      }

      return out;
   }

   private static int skinIndex(String style) {
      return switch (style) {
         case "Hologram" -> 1;
         case "Galaxy" -> 2;
         case "Glint" -> 3;
         case "Aurora" -> 4;
         case "Molten" -> 5;
         case "Frost" -> 6;
         case "Prismatic" -> 7;
         case "Pulse" -> 8;
         case "Digital" -> 9;
         case "Void" -> 10;
         case "Electric" -> 11;
         case "Inferno" -> 12;
         default -> 0;
      };
   }

   public static void draw(CrystalView cv, long now) {
      if (!cv.tracks.isEmpty() || !cv.pops.isEmpty()) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         WorldFx.axes(R, U, F);
         float time = (float)((now - START) / 1.0E9 % 3600.0);
         colours(cv, time, A, B);
         String styleName = cv.style();
         float size = cv.size();
         float dur = Math.max(0.1F, cv.popDuration());
         boolean explosion = cv.explosionPop();
         if (cv.vanillaModel()) {
            drawOverlay(cv, now, cam, time, A, B, dur, skinIndex(styleName), explosion);
         } else {
            int style = switch (styleName) {
               case "Plasma Orb" -> 1;
               case "Neon Cage" -> 2;
               case "Void Core" -> 3;
               default -> 0;
            };
            drawShapes(cv, now, cam, time, A, B, size, dur, style, explosion);
         }

         if (explosion) {
            drawPops(cv, now, cam, A, B, size, dur);
         }
      }
   }

   private static void drawOverlay(CrystalView cv, long now, Vec3 cam, float time, float[] a, float[] b, float dur, int st, boolean explosion) {
      int tex = CrystalModelFx.textureId();
      if (tex != 0) {
         float gl = Math.max(0.2F, cv.glow());
         float span = explosion ? dur * 0.45F : dur;
         float swell = explosion ? 0.45F : 0.1F;
         WorldFx.begin();

         for (CrystalView.Track t : cv.tracks.values()) {
            if (!t.popped) {
               float x = (float)(t.fx - cam.x);
               float y = (float)(t.fy - cam.y);
               float z = (float)(t.fz - cam.z);
               if (!behind(x, y + 1.0F, z)) {
                  float age = (float)(now - t.spawnNanos) / 1.0E9F;
                  float k = clamp01(age / 0.25F) * gl;
                  CrystalModelFx.emit(x, y, z, t.age, k, 1.012F, t.seed);
               }
            }
         }

         for (CrystalView.Pop p : cv.pops) {
            float k = clamp01((float)(now - p.nanos) / 1.0E9F / span);
            if (!(k >= 1.0F) && !behind((float)(p.fx - cam.x), (float)(p.fy - cam.y) + 1.0F, (float)(p.fz - cam.z))) {
               float e = 1.0F - (1.0F - k) * (1.0F - k);
               float fade = (1.0F - k) * (1.0F - k);
               CrystalModelFx.emit((float)(p.fx - cam.x), (float)(p.fy - cam.y), (float)(p.fz - cam.z), p.age, fade * gl, 1.012F + swell * e, p.seed);
            }
         }

         if (!WorldFx.isEmpty()) {
            WorldFx.flushTextured(SKIN_SHADERS[st], cv.walls(), tex, s -> {
               s.setUniform1f("uTime", time);
               s.setUniform3f("uA", a[0], a[1], a[2]);
               s.setUniform3f("uB", b[0], b[1], b[2]);
               s.setUniform3f("uCam", (float)cam.x, (float)cam.y, (float)cam.z);
               s.setUniform1i("uTex", 0);
            });
         }
      }
   }

   private static void drawShapes(CrystalView cv, long now, Vec3 cam, float time, float[] a, float[] b, float size, float dur, int style, boolean explosion) {
      WorldFx.begin();

      for (CrystalView.Track t : cv.tracks.values()) {
         if (!t.popped && !behind((float)(t.cx - cam.x), (float)(t.cy - cam.y), (float)(t.cz - cam.z))) {
            float age = (float)(now - t.spawnNanos) / 1.0E9F;
            float sc = outBack(age / 0.35F);
            float al = clamp01(age / 0.18F);
            billboard((float)(t.cx - cam.x), (float)(t.cy - cam.y), (float)(t.cz - cam.z), 0.95F * size * Math.max(sc, 1.0F), t.spin, 0.0F, size * sc, al, t.seed);
         }
      }

      for (CrystalView.Pop p : cv.pops) {
         if (!behind((float)(p.cx - cam.x), (float)(p.cy - cam.y), (float)(p.cz - cam.z))) {
            if (explosion) {
               float k = clamp01((float)(now - p.nanos) / 1.0E9F / dur / 0.4F);
               if (!(k >= 1.0F)) {
                  float sc = size * (1.0F + 0.22F * k);
                  billboard((float)(p.cx - cam.x), (float)(p.cy - cam.y), (float)(p.cz - cam.z), 0.95F * sc, p.spin + k * 2.0F, k, sc, 1.0F - k * k, p.seed);
               }
            } else {
               float k = clamp01((float)(now - p.nanos) / 1.0E9F / dur);
               if (!(k >= 1.0F)) {
                  float e = 1.0F - (1.0F - k) * (1.0F - k);
                  float sc = size * (1.0F + 0.12F * e);
                  float fade = (1.0F - k) * (1.0F - k);
                  billboard((float)(p.cx - cam.x), (float)(p.cy - cam.y) + 0.15F * e, (float)(p.cz - cam.z), 0.95F * sc, p.spin, -Math.max(0.35F, e), sc, fade, p.seed);
               }
            }
         }
      }

      if (!WorldFx.isEmpty()) {
         float glow = cv.glow();
         WorldFx.flush(SHAPE_SHADERS[style], cv.walls(), s -> {
            s.setUniform1f("uTime", time);
            s.setUniform3f("uA", a[0], a[1], a[2]);
            s.setUniform3f("uB", b[0], b[1], b[2]);
            s.setUniform1f("uGlow", glow);
         });
      }
   }

   private static void drawPops(CrystalView cv, long now, Vec3 cam, float[] a, float[] b, float size, float dur) {
      if (!cv.pops.isEmpty()) {
         WorldFx.begin();
         float ps = cv.popSize();
         boolean ring = cv.groundRing();

         for (CrystalView.Pop p : cv.pops) {
            float t = clamp01((float)(now - p.nanos) / 1.0E9F / dur);
            float x = (float)(p.cx - cam.x);
            float y = (float)(p.cy - cam.y);
            float z = (float)(p.cz - cam.z);
            quad(x, y, z, R, U, 3.2F * ps * size, t, p.seed);
            if (ring) {
               float gy = (float)(p.fy - cam.y) + 0.03F;
               quad((float)(p.fx - cam.x), gy, (float)(p.fz - cam.z), FLAT_R, FLAT_U, 3.5F * ps * size, t, 1.0F + p.seed);
            }
         }

         WorldFx.flush("crystalpop", cv.walls(), s -> {
            s.setUniform3f("uA", a[0], a[1], a[2]);
            s.setUniform3f("uB", b[0], b[1], b[2]);
            s.setUniform1f("uIntensity", 1.0F);
         });
      }
   }

   private static void billboard(float x, float y, float z, float rad, float spin, float dissolve, float scale, float alpha, float seed) {
      quadRaw(x, y, z, R, U, rad, spin, dissolve, scale, alpha, rad, seed);
   }

   private static void quad(float x, float y, float z, float[] r, float[] u, float rad, float progress, float mode) {
      quadRaw(x, y, z, r, u, rad, 1.0F, 1.0F, 1.0F, 1.0F, progress, mode);
   }

   private static void quadRaw(float x, float y, float z, float[] r, float[] u, float rad, float c0, float c1, float c2, float c3, float ax, float ay) {
      corner(x, y, z, r, u, rad, -1.0F, -1.0F, c0, c1, c2, c3, ax, ay);
      corner(x, y, z, r, u, rad, 1.0F, -1.0F, c0, c1, c2, c3, ax, ay);
      corner(x, y, z, r, u, rad, 1.0F, 1.0F, c0, c1, c2, c3, ax, ay);
      corner(x, y, z, r, u, rad, -1.0F, -1.0F, c0, c1, c2, c3, ax, ay);
      corner(x, y, z, r, u, rad, 1.0F, 1.0F, c0, c1, c2, c3, ax, ay);
      corner(x, y, z, r, u, rad, -1.0F, 1.0F, c0, c1, c2, c3, ax, ay);
   }

   private static void corner(float x, float y, float z, float[] r, float[] u, float rad, float sx, float sy, float c0, float c1, float c2, float c3, float ax, float ay) {
      WorldFx.vertex(x + (r[0] * sx + u[0] * sy) * rad, y + (r[1] * sx + u[1] * sy) * rad, z + (r[2] * sx + u[2] * sy) * rad, sx, sy, c0, c1, c2, c3, ax, ay);
   }

   private static void colours(CrystalView cv, float time, float[] a, float[] b) {
      int ca;
      int cb;
      switch (cv.colorMode()) {
         case "Custom":
            ca = ThemeColors.parseHexRgb(cv.color(), 8037111);
            cb = ThemeColors.parseHexRgb(cv.color2(), 4020641);
            break;
         case "Chroma":
            ca = Color.HSBtoRGB(time * 0.08F % 1.0F, 0.6F, 1.0F);
            cb = Color.HSBtoRGB((time * 0.08F + 0.33F) % 1.0F, 0.7F, 0.75F);
            break;
         default:
            ca = ThemeColors.getAccentRgb();
            cb = ThemeColors.getAccent2Rgb();
      }

      a[0] = (ca >> 16 & 0xFF) / 255.0F;
      a[1] = (ca >> 8 & 0xFF) / 255.0F;
      a[2] = (ca & 0xFF) / 255.0F;
      b[0] = (cb >> 16 & 0xFF) / 255.0F;
      b[1] = (cb >> 8 & 0xFF) / 255.0F;
      b[2] = (cb & 0xFF) / 255.0F;
   }

   private static boolean behind(float x, float y, float z) {
      return x * F[0] + y * F[1] + z * F[2] < -3.0F;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float outBack(float t) {
      t = clamp01(t) - 1.0F;
      float c = 1.7F;
      return 1.0F + (c + 1.0F) * t * t * t + c * t * t;
   }
}
