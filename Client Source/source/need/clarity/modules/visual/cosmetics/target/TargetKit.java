package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Prim;

final class TargetKit {
   static final double TAU = Math.PI * 2;
   private static final double NEAR_MIN = 0.3;
   private static final double NEAR_MAX = 0.85;

   private TargetKit() {
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

   static double wave(Builder b, double rate, double phase) {
      return b.animate ? 0.5 + 0.5 * Math.sin(b.time * rate + phase) : 0.5;
   }

   static double frac(double x) {
      return x - Math.floor(x);
   }

   static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   static double[][] perp(double[] axis) {
      double[] ref = Math.abs(axis[1]) < 0.9 ? new double[]{0.0, 1.0, 0.0} : new double[]{1.0, 0.0, 0.0};
      double[] e1 = Prim.unit(Prim.cross(axis, ref));
      return new double[][]{e1, Prim.cross(axis, e1)};
   }

   static void beam(Builder b, double[] p0, double[] p1, double half, int c0, int c1, boolean capEnd) {
      double[] axis = Prim.unit(Prim.sub(p1, p0));
      double[][] pp = perp(axis);
      double[][] a = new double[4][];
      double[][] e = new double[4][];
      double[] sx = new double[]{1.0, -1.0, -1.0, 1.0};
      double[] sy = new double[]{1.0, 1.0, -1.0, -1.0};

      for (int i = 0; i < 4; i++) {
         double ku = sx[i] * half;
         double kv = sy[i] * half;
         a[i] = Prim.P(p0[0] + pp[0][0] * ku + pp[1][0] * kv, p0[1] + pp[0][1] * ku + pp[1][1] * kv, p0[2] + pp[0][2] * ku + pp[1][2] * kv);
         e[i] = Prim.P(p1[0] + pp[0][0] * ku + pp[1][0] * kv, p1[1] + pp[0][1] * ku + pp[1][1] * kv, p1[2] + pp[0][2] * ku + pp[1][2] * kv);
      }

      double[] k = new double[]{1.0, 0.72, 0.58, 0.86};

      for (int i = 0; i < 4; i++) {
         int j = i + 1 & 3;
         int s0 = Col.shade(c0, k[i]);
         int s1 = Col.shade(c1, k[i]);
         b.quad(a[i], a[j], e[j], e[i], s0, s0, s1, s1);
      }

      if (capEnd) {
         int c = Col.shade(c1, 1.1);
         b.quad(e[0], e[1], e[2], e[3], c);
      }
   }
}
