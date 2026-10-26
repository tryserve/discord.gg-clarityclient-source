package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Prim;

final class KillKit {
   static final double TAU = Math.PI * 2;
   static final double LIFT = 0.03;
   private static final double NEAR_MIN = 0.3;
   private static final double NEAR_MAX = 0.85;

   private KillKit() {
   }

   static double near(double[] cam, double u, double v, double w, double scale) {
      if (cam == null) {
         return 1.0;
      }

      double dx = cam[0] - u;
      double dy = cam[1] - v;
      double dz = cam[2] - w;
      double d = Math.sqrt(dx * dx + dy * dy + dz * dz) * scale;
      return Prim.sm((d - 0.3) / 0.55);
   }

   static double frac(double x) {
      return x - Math.floor(x);
   }

   static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   static double[] gp(KillParams.Ground g, double lift, double u, double w) {
      return Prim.P(u, g.at(u, w) + lift, w);
   }

   static void ring(Builder b, KillParams.Ground g, double lift, double rIn, double rOut, int segs, Prim.Shade s) {
      if (!(rOut <= rIn + 1.0E-6)) {
         double[][] in = new double[segs + 1][];
         double[][] out = new double[segs + 1][];

         for (int i = 0; i <= segs; i++) {
            double a = (double)(i % segs) / segs * (Math.PI * 2);
            double c = Math.cos(a);
            double sn = Math.sin(a);
            in[i] = gp(g, lift, rIn * c, rIn * sn);
            out[i] = gp(g, lift, rOut * c, rOut * sn);
         }

         for (int i = 0; i < segs; i++) {
            double t0 = (double)i / segs;
            double t1 = (double)(i + 1) / segs;
            b.quad(in[i], out[i], out[i + 1], in[i + 1], s.at(t0, 0.0), s.at(t0, 1.0), s.at(t1, 1.0), s.at(t1, 0.0));
         }
      }
   }

   static void disc(Builder b, KillParams.Ground g, double lift, double r, int segs, Prim.Shade s) {
      if (!(r <= 1.0E-6)) {
         double[] c = gp(g, lift, 0.0, 0.0);
         double[][] rim = new double[segs + 1][];

         for (int i = 0; i <= segs; i++) {
            double a = (double)(i % segs) / segs * (Math.PI * 2);
            rim[i] = gp(g, lift, r * Math.cos(a), r * Math.sin(a));
         }

         for (int i = 0; i < segs; i++) {
            double t0 = (double)i / segs;
            double t1 = (double)(i + 1) / segs;
            b.tri(c, rim[i], rim[i + 1], s.at(t0, 0.0), s.at(t0, 1.0), s.at(t1, 1.0));
         }
      }
   }

   static double lerpArr(double[] arr, double v) {
      int n = arr.length;
      if (n == 1) {
         return arr[0];
      }

      double x = clamp(v, 0.0, 1.0) * (n - 1);
      int i = Math.min(n - 2, (int)x);
      double f = x - i;
      return arr[i] + (arr[i + 1] - arr[i]) * f;
   }

   static double[] nearAlong(double[] cam, double[][] pts, double scale) {
      double[] out = new double[pts.length];

      for (int i = 0; i < pts.length; i++) {
         out[i] = near(cam, pts[i][0], pts[i][1], pts[i][2], scale);
      }

      return out;
   }

   static void groundStrip(Builder b, KillParams.Ground g, double lift, double[] xs, double[] zs, double[] ws, Prim.Shade s) {
      int n = xs.length;
      if (n >= 2) {
         double[][] l = new double[n][];
         double[][] r = new double[n][];

         for (int i = 0; i < n; i++) {
            int a = Math.max(i - 1, 0);
            int c = Math.min(i + 1, n - 1);
            double dx = xs[c] - xs[a];
            double dz = zs[c] - zs[a];
            double len = Math.hypot(dx, dz);
            double nx = len < 1.0E-9 ? 0.0 : -dz / len;
            double nz = len < 1.0E-9 ? 1.0 : dx / len;
            double hw = 0.5 * ws[i];
            l[i] = gp(g, lift, xs[i] + nx * hw, zs[i] + nz * hw);
            r[i] = gp(g, lift, xs[i] - nx * hw, zs[i] - nz * hw);
         }

         for (int i = 0; i < n - 1; i++) {
            double v0 = i / (n - 1.0);
            double v1 = (i + 1) / (n - 1.0);
            b.quad(l[i], r[i], r[i + 1], l[i + 1], s.at(0.0, v0), s.at(1.0, v0), s.at(1.0, v1), s.at(0.0, v1));
         }
      }
   }
}
