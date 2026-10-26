package need.clarity.render.worldfx;

import need.clarity.util.ClarityRenderUtils;
import net.minecraft.world.phys.Vec3;

public final class ShapeFx {
   public static final String[] TARGET = new String[]{
      "Plasma Ring", "Reticle", "Vortex", "Energy Shield", "Marker", "Triangle Slice", "Reaper", "Shatter Lock", "Halo", "Ground Glow", "Brackets", "Comet Ring"
   };
   public static final String[] AURA = new String[]{"Plasma Shield", "Flame Aura", "Galaxy Ring", "Spirit Orbs", "Rising Sparks", "Soft Halo", "Silhouette Glow", "Drift Lines"};
   private static final float[] R = new float[3];
   private static final float[] U = new float[3];
   private static final float[] F = new float[3];
   private static float alpha = 1.0F;

   private ShapeFx() {
   }

   public static int comboStyle(String n) {
      int s = targetStyle(n);
      return s >= 5 && s <= 7 ? s - 5 : -1;
   }

   public static int targetStyle(String n) {
      return indexOf(TARGET, n);
   }

   public static int auraStyle(String n) {
      return indexOf(AURA, n);
   }

   public static void target(int style, double x, double y, double z, float bbW, float bbH, float[] c1, float[] c2, float time, float flash, float in, boolean throughWalls) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      WorldFx.axes(R, U, F);
      float fx = (float)(x - cam.x);
      float fy = (float)(y - cam.y);
      float fz = (float)(z - cam.z);
      float big = Math.max(bbH, bbW);
      WorldFx.begin();
      switch (style) {
         case 0: {
            float s = (bbW * 0.7F + 0.35F) * 1.25F;
            quad(fx, fy + 0.02F, fz, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, s, s, -1.25F, -1.25F, 1.25F, 1.25F, c1);
            break;
         }
         case 1: {
            float s = big * 0.62F * 1.3F * (1.0F + (1.0F - in) * 0.8F);
            quad(fx, fy + bbH * 0.5F, fz, R[0], R[1], R[2], U[0], U[1], U[2], s, s, -1.3F, -1.3F, 1.3F, 1.3F, c1);
            break;
         }
         case 2:
            float rad = bbW * 0.85F + 0.1F;
            float hgt = bbH * 1.2F;
            int n = 24;

            for (int i = 0; i < n; i++) {
               double a0 = (Math.PI * 2) * i / n;
               double a1 = (Math.PI * 2) * (i + 1) / n;
               float x0 = fx + (float)Math.cos(a0) * rad;
               float z0 = fz + (float)Math.sin(a0) * rad;
               float x1 = fx + (float)Math.cos(a1) * rad;
               float z1 = fz + (float)Math.sin(a1) * rad;
               float u0 = (float)i / n;
               float u1 = (float)(i + 1) / n;
               v(x0, fy, z0, u0, 0.0F, c1);
               v(x1, fy, z1, u1, 0.0F, c1);
               v(x1, fy + hgt, z1, u1, 1.0F, c1);
               v(x0, fy, z0, u0, 0.0F, c1);
               v(x1, fy + hgt, z1, u1, 1.0F, c1);
               v(x0, fy + hgt, z0, u0, 1.0F, c1);
            }
            break;
         case 3: {
            float s = big * 0.62F;
            quad(fx, fy + bbH * 0.5F, fz, R[0], R[1], R[2], U[0], U[1], U[2], s, s, -1.0F, -1.0F, 1.0F, 1.0F, c1);
            break;
         }
         case 4: {
            float s = 0.364F;
            quad(fx, fy + bbH + 0.55F, fz, R[0], R[1], R[2], U[0], U[1], U[2], s, s, -1.3F, -1.3F, 1.3F, 1.3F, c1);
            break;
         }
         case 5:
         case 6: {
            float hx = R[0];
            float hz = R[2];
            float hl = (float)Math.sqrt(hx * hx + hz * hz);
            if (hl < 1.0E-4F) {
               hx = 1.0F;
               hz = 0.0F;
            } else {
               hx /= hl;
               hz /= hl;
            }

            float fx2 = F[0];
            float fz2 = F[2];
            float fl = (float)Math.sqrt(fx2 * fx2 + fz2 * fz2);
            if (fl < 1.0E-4F) {
               fx2 = 0.0F;
               fz2 = 1.0F;
            } else {
               fx2 /= fl;
               fz2 /= fl;
            }

            float tilt = 0.6F;
            float ct = (float)Math.cos(tilt);
            float st = (float)Math.sin(tilt);
            float s = (bbW * 0.9F + 0.5F) * 1.3F;
            quad(fx, fy + bbH * 0.55F, fz, hx, 0.0F, hz, fx2 * ct, st, fz2 * ct, s, s, -1.3F, -1.3F, 1.3F, 1.3F, c1);
            break;
         }
         case 7: {
            float s = big * 0.62F * 1.3F;
            quad(fx, fy + bbH * 0.5F, fz, R[0], R[1], R[2], U[0], U[1], U[2], s, s, -1.3F, -1.3F, 1.3F, 1.3F, c1);
            break;
         }
         case 8:
         case 9:
         case 11: {
            float s = (bbW * 0.7F + 0.45F) * 1.25F;
            quad(fx, fy + 0.03F, fz, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, s, s, -1.25F, -1.25F, 1.25F, 1.25F, c1);
            break;
         }
         case 10:
            float hx = R[0];
            float hz = R[2];
            float hl = (float)Math.sqrt(hx * hx + hz * hz);
            if (hl < 1.0E-4F) {
               hx = 1.0F;
               hz = 0.0F;
            } else {
               hx /= hl;
               hz /= hl;
            }

            float w = (bbW * 0.5F + 0.22F) * 1.15F;
            float h = (bbH * 0.5F + 0.12F) * 1.15F;
            quad(fx, fy + bbH * 0.5F, fz, hx, 0.0F, hz, 0.0F, 1.0F, 0.0F, w, h, -1.15F, -1.15F, 1.15F, 1.15F, c1);
      }

      WorldFx.flush("target", throughWalls, sx -> {
         sx.setUniform1f("uTime", time);
         sx.setUniform1i("uStyle", style);
         sx.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
         sx.setUniform1f("uFlash", flash);
         sx.setUniform1f("uIn", in);
      });
   }

   public static void aura(int style, double x, double y, double z, float height, float scale, float radius, int count, float[] c1, float[] c2, float opacity, float time, boolean throughWalls) {
      if (style != 3 && style != 4) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         WorldFx.axes(R, U, F);
         float fx = (float)(x - cam.x);
         float fy = (float)(y - cam.y);
         float fz = (float)(z - cam.z);
         float[] col = new float[]{c1[0], c1[1], c1[2]};
         alpha = opacity;
         WorldFx.begin();
         switch (style) {
            case 0: {
               float s = 1.05F * scale;
               quad(fx, fy + height * 0.5F, fz, R[0], R[1], R[2], U[0], U[1], U[2], s, s, -1.0F, -1.0F, 1.0F, 1.0F, col);
               break;
            }
            case 1: {
               float hx = R[0];
               float hz = R[2];
               float l = (float)Math.sqrt(hx * hx + hz * hz);
               if (l < 1.0E-4F) {
                  hx = 1.0F;
                  hz = 0.0F;
                  l = 1.0F;
               }

               hx /= l;
               hz /= l;
               float w = 1.0F * scale;
               float h = 1.4F * scale;
               quad(fx, fy + h - 0.05F, fz, hx, 0.0F, hz, 0.0F, 1.0F, 0.0F, w, h, -1.0F, 0.0F, 1.0F, 1.0F, col);
               break;
            }
            case 2: {
               float s = 1.5F * scale * 1.2F;
               quad(fx, fy + height * 0.5F, fz, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, s, s, -1.2F, -1.2F, 1.2F, 1.2F, col);
            }
            case 3:
            case 4:
            default:
               break;
            case 5: {
               float s = 0.75F * scale * radius * 1.25F;
               quad(fx, fy + 0.03F, fz, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, s, s, -1.25F, -1.25F, 1.25F, 1.25F, col);
               break;
            }
            case 6: {
               float hx = R[0];
               float hz = R[2];
               float l = (float)Math.sqrt(hx * hx + hz * hz);
               if (l < 1.0E-4F) {
                  hx = 1.0F;
                  hz = 0.0F;
                  l = 1.0F;
               }

               hx /= l;
               hz /= l;
               float w = 0.62F * scale;
               float h = height * 0.5F + 0.22F;
               quad(fx, fy + height * 0.5F, fz, hx, 0.0F, hz, 0.0F, 1.0F, 0.0F, w, h, -1.0F, -1.0F, 1.0F, 1.0F, col);
               break;
            }
            case 7: {
               float rad = 0.55F * scale * radius;
               float hgt = height * 1.05F;
               int n = 28;

               for (int i = 0; i < n; i++) {
                  double a0 = (Math.PI * 2) * i / n;
                  double a1 = (Math.PI * 2) * (i + 1) / n;
                  float x0 = fx + (float)Math.cos(a0) * rad;
                  float z0 = fz + (float)Math.sin(a0) * rad;
                  float x1 = fx + (float)Math.cos(a1) * rad;
                  float z1 = fz + (float)Math.sin(a1) * rad;
                  float u0 = (float)i / n;
                  float u1 = (float)(i + 1) / n;
                  v(x0, fy, z0, u0, 0.0F, col);
                  v(x1, fy, z1, u1, 0.0F, col);
                  v(x1, fy + hgt, z1, u1, 1.0F, col);
                  v(x0, fy, z0, u0, 0.0F, col);
                  v(x1, fy + hgt, z1, u1, 1.0F, col);
                  v(x0, fy + hgt, z0, u0, 1.0F, col);
               }
            }
         }

         alpha = 1.0F;
         WorldFx.flush("aura", throughWalls, s -> {
            s.setUniform1f("uTime", time);
            s.setUniform1i("uStyle", style);
            s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
         });
      } else {
         GpuFx.auraParticles(style == 3 ? 0 : 1, x, y, z, height, radius * scale, style == 3 ? Math.max(1, count) : Math.round(140.0F * scale), scale, c1, c2, opacity, time, throughWalls);
      }
   }

   private static void quad(float cx, float cy, float cz, float ax, float ay, float az, float bx, float by, float bz, float ha, float hb, float u0, float v0, float u1, float v1, float[] col) {
      float um = (u0 + u1) * 0.5F;
      float vm = (v0 + v1) * 0.5F;
      float ox = cx;
      float oy = cy;
      float oz = cz;
      float[][] P = new float[4][];
      float[][] UV = new float[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};

      for (int i = 0; i < 4; i++) {
         float su = (UV[i][0] - um) / ((u1 - u0) * 0.5F);
         float sv = (UV[i][1] - vm) / ((v1 - v0) * 0.5F);
         P[i] = new float[]{ox + ax * ha * su + bx * hb * sv, oy + ay * ha * su + by * hb * sv, oz + az * ha * su + bz * hb * sv};
      }

      v(P[0][0], P[0][1], P[0][2], u0, v0, col);
      v(P[1][0], P[1][1], P[1][2], u1, v0, col);
      v(P[2][0], P[2][1], P[2][2], u1, v1, col);
      v(P[0][0], P[0][1], P[0][2], u0, v0, col);
      v(P[2][0], P[2][1], P[2][2], u1, v1, col);
      v(P[3][0], P[3][1], P[3][2], u0, v1, col);
   }

   private static void v(float x, float y, float z, float u, float vv, float[] c) {
      WorldFx.vertex(x, y, z, u, vv, c[0], c[1], c[2], alpha, 0.0F, 0.0F);
   }

   private static int indexOf(String[] arr, String v) {
      if (v != null) {
         for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(v)) {
               return i;
            }
         }
      }

      return -1;
   }

   public static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }
}
