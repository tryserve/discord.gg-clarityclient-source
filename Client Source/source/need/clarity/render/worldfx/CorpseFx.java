package need.clarity.render.worldfx;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

public final class CorpseFx {
   private static final float LIFE = 2.1F;
   private static final float CUT = 0.32F;
   private static final CorpseFx.Slot[] SLOTS = new CorpseFx.Slot[]{new CorpseFx.Slot(), new CorpseFx.Slot(), new CorpseFx.Slot(), new CorpseFx.Slot()};
   private static final List<CorpseFx.Corpse> corpses = new ArrayList<>();
   private static int counter = 0;

   private CorpseFx() {
   }

   public static CorpseFx.Slot acquire(int w, int h) {
      CorpseFx.Slot pick = null;

      for (CorpseFx.Slot s : SLOTS) {
         boolean busy = false;

         for (CorpseFx.Corpse c : corpses) {
            if (c.slot == s) {
               busy = true;
               break;
            }
         }

         if (!busy) {
            pick = s;
            break;
         }
      }

      if (pick == null) {
         pick = corpses.get(0).slot;
         corpses.remove(0);
      }

      if (pick.fbo == 0 || pick.w != w || pick.h != h) {
         if (pick.tex == 0) {
            pick.tex = GL11.glGenTextures();
         }

         GlStateManager._activeTexture(33984);
         int prev = GL11.glGetInteger(32873);
         GlStateManager._bindTexture(pick.tex);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexParameteri(3553, 33085, 0);
         int prevPbo = GL11.glGetInteger(35055);
         GL15.glBindBuffer(35052, 0);
         GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
         GL15.glBindBuffer(35052, prevPbo);
         GlStateManager._bindTexture(prev);
         if (pick.fbo == 0) {
            pick.fbo = GL30.glGenFramebuffers();
         }

         int prevFbo = GL11.glGetInteger(36006);
         GL30.glBindFramebuffer(36160, pick.fbo);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, pick.tex, 0);
         GL30.glBindFramebuffer(36160, prevFbo);
         pick.w = w;
         pick.h = h;
      }

      return pick;
   }

   public static int fbo(CorpseFx.Slot s) {
      return s.fbo;
   }

   public static void add(CorpseFx.Slot slot, double[] o, double[] u, double[] v, double groundY, int style) {
      corpses.add(new CorpseFx.Corpse(slot, o, u, v, groundY, System.nanoTime(), style, counter++ * 0.6180339F % 1.0F, null, null));
   }

   public static void addAnchored(CorpseFx.Slot slot, double[] anchor, double offR, double offU, double W, double H, double groundY, int style) {
      corpses.add(new CorpseFx.Corpse(slot, new double[3], new double[3], new double[3], groundY, System.nanoTime(), style, counter++ * 0.6180339F % 1.0F, anchor, new double[]{offR, offU, W, H}));
   }

   private static void place(CorpseFx.Corpse c) {
      if (c.anchor != null) {
         float[] R = new float[3];
         float[] U = new float[3];
         float[] F = new float[3];
         SkyShaderRenderer.cameraAxes(R, U, F);
         double offR = c.box[0];
         double offU = c.box[1];
         double W = c.box[2];
         double H = c.box[3];

         for (int i = 0; i < 3; i++) {
            c.o[i] = c.anchor[i] + R[i] * (offR - W / 2.0) + U[i] * (offU - H / 2.0);
            c.u[i] = R[i] * W;
            c.v[i] = U[i] * H;
         }
      }
   }

   public static boolean isEmpty() {
      return corpses.isEmpty();
   }

   public static void render(float[] c1, float[] c2, boolean throughWalls) {
      long now = System.nanoTime();
      corpses.removeIf(cx -> (now - cx.t) / 1.0E9 > 2.1F);
      if (!corpses.isEmpty()) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         float time = (float)(now / 1.0E9 % 3600.0);

         for (CorpseFx.Corpse c : corpses) {
            float t = (float)((now - c.t) / 1.0E9);
            place(c);
            WorldFx.begin();
            emit(c, t, cam);
            if (!WorldFx.isEmpty()) {
               WorldFx.flushTextured("corpse", throughWalls, c.slot.tex, s -> {
                  s.setUniform1i("uTex", 0);
                  s.setUniform3f("uColor1", c1[0], c1[1], c1[2]);
                  s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
                  s.setUniform1f("uTime", time);
               });
            }
         }
      }
   }

   private static void emit(CorpseFx.Corpse c, float t, Vec3 cam) {
      double[] U = c.u;
      double[] V = c.v;
      double[] N = norm(cross(U, V));
      double after = Math.max(0.0F, t - 0.32F);
      float alpha = (float)(1.0 - smooth(1.75, 2.1F, t));
      float dissolve = c.style == 2 ? (float)smooth(0.75, 1.6, t) : (float)smooth(1.0, 1.95, t);
      if (c.style == 2) {
         int cols = 3;
         int rows = 5;
         int k = 0;

         for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
               for (int tri = 0; tri < 2; k++) {
                  double u0 = (double)i / cols;
                  double u1 = (double)(i + 1) / cols;
                  double v0 = (double)j / rows;
                  double v1 = (double)(j + 1) / rows;
                  double[][] poly = (i + j + tri & 1) == 0
                     ? (tri == 0 ? new double[][]{{u0, v0}, {u1, v0}, {u1, v1}} : new double[][]{{u0, v0}, {u1, v1}, {u0, v1}})
                     : (tri == 0 ? new double[][]{{u0, v0}, {u1, v0}, {u0, v1}} : new double[][]{{u1, v0}, {u1, v1}, {u0, v1}});
                  double cu = (poly[0][0] + poly[1][0] + poly[2][0]) / 3.0;
                  double cv = (poly[0][1] + poly[1][1] + poly[2][1]) / 3.0;
                  double h1 = hash(k * 1.7 + c.seed * 13.0F);
                  double h2 = hash(k * 3.1 + c.seed * 7.0F);
                  double h3 = hash(k * 5.3 + 2.0);
                  double a = Math.max(0.0, t - 0.25);
                  double[] outDir = norm(add(mul(U, cu - 0.5), mul(V, cv - 0.5)));
                  double speed = 1.3 + 1.6 * h1;
                  double drag = (1.0 - Math.exp(-2.2 * a)) / 2.2;
                  double[] off = add(add(mul(outDir, speed * drag), mul(N, (h2 - 0.5) * 1.6 * drag)), new double[]{0.0, -3.5 * a * a, 0.0});
                  double ang = (h3 - 0.5) * 9.0 * a;
                  double shrink = 1.0 - 0.35 * smooth(0.6, 1.6, t);
                  poly(c, poly, off, ang, shrink, N, cam, alpha, dissolve, null, 0.0, true);
                  tri++;
               }
            }
         }

         if (t > 0.15 && t < 0.6) {
            float fa = (float)(1.0 - smooth(0.25, 0.6, t)) * (float)smooth(0.15, 0.25, t);
            streak(c, new double[]{0.15, 0.2}, new double[]{0.85, 0.8}, 0.05, fa, cam);
            streak(c, new double[]{0.85, 0.2}, new double[]{0.15, 0.8}, 0.05, fa, cam);
         }
      } else {
         double cutAng = c.style == 0 ? 0.55 + (c.seed - 0.5) * 0.4 : 0.0;
         double[] dir = new double[]{Math.cos(cutAng), Math.sin(cutAng)};
         double[] nrm = new double[]{-dir[1], dir[0]};
         double[] mid = new double[]{0.5, c.style == 0 ? 0.5 : 0.52};
         double[][] rect = new double[][]{{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0}};
         double sep = smooth(0.0, 0.55, after);
         boolean cut = t >= 0.32F;

         for (int side = -1; side <= 1; side += 2) {
            double[][] piece = clip(rect, nrm, mid, side);
            if (piece.length >= 3) {
               double[] inPlane;
               double ang;
               if (c.style == 0) {
                  inPlane = add(mul(U, nrm[0] * side * 0.16 * sep + dir[0] * side * 0.1 * sep), mul(V, nrm[1] * side * 0.16 * sep + dir[1] * side * 0.1 * sep));
                  ang = side * 0.55 * sep;
               } else {
                  double slide = side > 0 ? 0.45 * sep : 0.0;
                  inPlane = add(mul(U, slide), mul(V, side > 0 ? 0.02 * sep : -0.03 * sep));
                  ang = side > 0 ? -0.9 * sep : 0.0;
               }

               double fall = side <= 0 && c.style != 0 ? 1.0 * after * after : 3.25 * after * after;
               double[] off = add(inPlane, new double[]{0.0, -fall, 0.0});
               double low = Double.MAX_VALUE;

               for (double[] p : piece) {
                  double[] w = world(c, p);
                  low = Math.min(low, w[1]);
               }

               if (low + off[1] < c.ground) {
                  off[1] = c.ground - low;
               }

               poly(c, piece, off, ang, 1.0, N, cam, alpha, dissolve, cut ? nrm : null, dot2(nrm, mid), false);
            }
         }

         if (cut && after < 0.7) {
            for (int k = 0; k < 22; k++) {
               double h1 = hash(k * 2.3 + c.seed * 17.0F);
               double h2 = hash(k * 4.7 + c.seed * 5.0F);
               double h3 = hash(k * 7.9 + 1.0);
               double tt = after - h3 * 0.08;
               if (!(tt <= 0.0)) {
                  double s0 = (h1 - 0.5) * 1.1;
                  double side = h2 < 0.5 ? -1.0 : 1.0;
                  double sp = 0.6 + 0.9 * h2;
                  double vx = nrm[0] * side * sp + dir[0] * (h3 - 0.5) * 0.8;
                  double vy = nrm[1] * side * sp + dir[1] * (h3 - 0.5) * 0.8;
                  double px = mid[0] + dir[0] * s0 + vx * tt;
                  double py = mid[1] + dir[1] * s0 + vy * tt - 1.6 * tt * tt;
                  double qx = px - vx * 0.045;
                  double qy = py - (vy - 3.2 * tt) * 0.045;
                  float sa = (float)(1.0 - smooth(0.15, 0.7, after)) * alpha;
                  streak(c, new double[]{qx, qy}, new double[]{px, py}, 0.012, sa, cam);
               }
            }
         }

         if (t > 0.19999999284744263 && t < 0.6199999928474427) {
            float fa = (float)(smooth(0.19999999284744263, 0.32F, t) * (1.0 - smooth(0.3699999928474426, 0.6199999928474427, t)));
            double[] a = new double[]{mid[0] - dir[0] * 0.85, mid[1] - dir[1] * 0.85};
            double[] b = new double[]{mid[0] + dir[0] * 0.85, mid[1] + dir[1] * 0.85};
            streak(c, a, b, 0.045, fa, cam);
         }
      }
   }

   private static void poly(CorpseFx.Corpse c, double[][] uv, double[] off, double ang, double scale, double[] N, Vec3 cam, float alpha, float dissolve, double[] cutN, double cutD, boolean noCut) {
      int n = uv.length;
      double[][] w = new double[n][];
      double[] cen = new double[]{0.0, 0.0, 0.0};

      for (int i = 0; i < n; i++) {
         w[i] = world(c, uv[i]);
         cen = add(cen, w[i]);
      }

      cen = mul(cen, 1.0 / n);

      for (int i = 0; i < n; i++) {
         double[] r = mul(sub(w[i], cen), scale);
         w[i] = add(add(cen, rotate(r, N, ang)), off);
      }

      for (int i = 1; i < n - 1; i++) {
         v(w[0], uv[0], cam, alpha, dissolve, cutN, cutD, noCut);
         v(w[i], uv[i], cam, alpha, dissolve, cutN, cutD, noCut);
         v(w[i + 1], uv[i + 1], cam, alpha, dissolve, cutN, cutD, noCut);
      }
   }

   private static void v(double[] p, double[] uv, Vec3 cam, float alpha, float dissolve, double[] cutN, double cutD, boolean noCut) {
      float d = !noCut && cutN != null ? (float)(dot2(cutN, uv) - cutD) : 99.0F;
      WorldFx.vertex((float)(p[0] - cam.x), (float)(p[1] - cam.y), (float)(p[2] - cam.z), (float)uv[0], (float)uv[1], 1.0F, 1.0F, 1.0F, alpha, dissolve, d);
   }

   private static void streak(CorpseFx.Corpse c, double[] a, double[] b, double width, float alpha, Vec3 cam) {
      double[] wa = world(c, a);
      double[] wb = world(c, b);
      double[] along = sub(wb, wa);
      double[] N = norm(cross(c.u, c.v));
      double[] side = mul(norm(cross(along, N)), width * Math.sqrt(dot(c.u, c.u)));
      double[] lift = mul(N, -0.03);
      double[][] q = new double[][]{add(sub(wa, side), lift), add(sub(wb, side), lift), add(add(wb, side), lift), add(add(wa, side), lift)};
      double[][] uv = new double[][]{{-1.0, -1.0}, {1.0, -1.0}, {1.0, 1.0}, {-1.0, 1.0}};
      int[] order = new int[]{0, 1, 2, 0, 2, 3};

      for (int i : order) {
         WorldFx.vertex((float)(q[i][0] - cam.x), (float)(q[i][1] - cam.y), (float)(q[i][2] - cam.z), (float)uv[i][0], (float)uv[i][1], 1.0F, 1.0F, 1.0F, alpha, 0.0F, -100.0F);
      }
   }

   private static double[] world(CorpseFx.Corpse c, double[] uv) {
      return add(add(c.o, mul(c.u, uv[0])), mul(c.v, uv[1]));
   }

   private static double[][] clip(double[][] poly, double[] n, double[] m, int side) {
      List<double[]> out = new ArrayList<>();
      double cm = dot2(n, m);

      for (int i = 0; i < poly.length; i++) {
         double[] a = poly[i];
         double[] b = poly[(i + 1) % poly.length];
         double da = side * (dot2(n, a) - cm);
         double db = side * (dot2(n, b) - cm);
         if (da >= 0.0) {
            out.add(a);
         }

         if (da >= 0.0 != db >= 0.0) {
            double t = da / (da - db);
            out.add(new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
         }
      }

      return out.toArray(new double[0][]);
   }

   private static double[] rotate(double[] v, double[] k, double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      double[] kxv = cross(k, v);
      double kv = dot(k, v);
      return new double[]{v[0] * c + kxv[0] * s + k[0] * kv * (1.0 - c), v[1] * c + kxv[1] * s + k[1] * kv * (1.0 - c), v[2] * c + kxv[2] * s + k[2] * kv * (1.0 - c)};
   }

   private static double smooth(double e0, double e1, double x) {
      double t = Math.max(0.0, Math.min(1.0, (x - e0) / (e1 - e0)));
      return t * t * (3.0 - 2.0 * t);
   }

   private static double hash(double p) {
      double x = Math.sin(p * 127.1) * 43758.5453;
      return x - Math.floor(x);
   }

   private static double dot2(double[] a, double[] b) {
      return a[0] * b[0] + a[1] * b[1];
   }

   private static double dot(double[] a, double[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   private static double[] add(double[] a, double[] b) {
      return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
   }

   private static double[] sub(double[] a, double[] b) {
      return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
   }

   private static double[] mul(double[] a, double s) {
      return new double[]{a[0] * s, a[1] * s, a[2] * s};
   }

   private static double[] cross(double[] a, double[] b) {
      return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
   }

   private static double[] norm(double[] a) {
      double l = Math.sqrt(dot(a, a));
      return l < 1.0E-9 ? new double[]{0.0, 0.0, 1.0} : mul(a, 1.0 / l);
   }

   private record Corpse(CorpseFx.Slot slot, double[] o, double[] u, double[] v, double ground, long t, int style, float seed, double[] anchor, double[] box) {
   }

   public static final class Slot {
      int tex;
      int fbo;
      int w;
      int h;
   }
}
