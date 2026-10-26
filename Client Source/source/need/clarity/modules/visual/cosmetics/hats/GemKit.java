package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Prim;

final class GemKit {
   private static final double TAU = Math.PI * 2;

   private GemKit() {
   }

   static void gem(Builder b, double ru, double rw, double up, double down, int sides, int base, int alt, double phase) {
      double t = b.animate ? b.time : 0.0;
      double[] top = Prim.P(0.0, up, 0.0);
      double[] bot = Prim.P(0.0, -down, 0.0);

      for (int i = 0; i < sides; i++) {
         double a0 = (double)i / sides * (Math.PI * 2);
         double a1 = (double)(i + 1) / sides * (Math.PI * 2);
         double[] g0 = Prim.P(ru * Math.cos(a0), 0.0, rw * Math.sin(a0));
         double[] g1 = Prim.P(ru * Math.cos(a1), 0.0, rw * Math.sin(a1));

         for (int s = 0; s < 2; s++) {
            double[] apex = s == 0 ? top : bot;
            double[] n = Prim.cross(Prim.sub(g1, g0), Prim.sub(apex, g0));
            double[] c = new double[]{(g0[0] + g1[0] + apex[0]) / 3.0, (g0[1] + g1[1] + apex[1]) / 3.0, (g0[2] + g1[2] + apex[2]) / 3.0};
            int col = facet(b, c, n, base, alt, t, phase, i + s * sides);
            b.tri(g0, g1, apex, Col.shade(col, 0.88), Col.shade(col, 0.88), Col.shade(col, s == 0 ? 1.18 : 1.04));
         }
      }
   }

   static void gem(Builder b, double r, double up, double down, int sides, int base, int alt, double phase) {
      gem(b, r, r, up, down, sides, base, alt, phase);
   }

   private static int facet(Builder b, double[] p, double[] n, int base, int alt, double t, double phase, int k) {
      double f = b.facing(p, n);
      double body = 0.5 + 0.65 * f;
      double rim = (1.0 - f) * (1.0 - f);
      int col = Col.mix(Col.shade(base, body), Col.tint(alt, 0.35), 0.5 * rim);
      double spec = Prim.sm((f - 0.45) / 0.4) * (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * 3.1 + phase + k * 2.1)));
      return Col.tint(col, Col.clamp01(spec * 0.85));
   }

   static void glint(Builder b, double[] c, double size, int col, double k) {
      if (!(k < 0.04)) {
         double[] e1 = new double[]{1.0, 0.0, 0.0};
         double[] e2 = new double[]{0.0, 1.0, 0.0};
         double[] v = b.viewLocal();
         if (v != null) {
            double[] d = Prim.unit(Prim.sub(v, c));
            double[] up = Math.abs(d[1]) > 0.95 ? new double[]{1.0, 0.0, 0.0} : new double[]{0.0, 1.0, 0.0};
            e1 = Prim.unit(Prim.cross(up, d));
            e2 = Prim.cross(d, e1);
            c = Prim.add(c, d[0] * size * 0.2, d[1] * size * 0.2, d[2] * size * 0.2);
         }

         double s = size * (0.35 + 0.65 * k);
         double w = s * 0.13;
         int hot = Col.withA(Col.tint(col, 0.85), Col.clamp01(k));
         int tip = Col.withA(col, 0.0);
         sliver(b, c, e1, e2, s, w, hot, tip);
         sliver(b, c, e2, e1, s * 0.8, w, hot, tip);
      }
   }

   private static void sliver(Builder b, double[] c, double[] along, double[] across, double s, double w, int hot, int tip) {
      b.quad(
         Prim.add(c, along[0] * s, along[1] * s, along[2] * s),
         Prim.add(c, across[0] * w, across[1] * w, across[2] * w),
         Prim.add(c, -along[0] * s, -along[1] * s, -along[2] * s),
         Prim.add(c, -across[0] * w, -across[1] * w, -across[2] * w),
         tip,
         hot,
         tip,
         hot
      );
   }

   static void spark(Builder b, double[] c, double size, int col) {
      if (Col.a(col) >= 6 && !(size <= 0.0)) {
         double[] e1 = new double[]{1.0, 0.0, 0.0};
         double[] e2 = new double[]{0.0, 1.0, 0.0};
         double[] v = b.viewLocal();
         if (v != null) {
            double[] d = Prim.unit(Prim.sub(v, c));
            double[] up = Math.abs(d[1]) > 0.95 ? new double[]{1.0, 0.0, 0.0} : new double[]{0.0, 1.0, 0.0};
            e1 = Prim.unit(Prim.cross(up, d));
            e2 = Prim.cross(d, e1);
         }

         double h = size * 1.35;
         b.quad(
            Prim.add(c, e2[0] * h, e2[1] * h, e2[2] * h),
            Prim.add(c, e1[0] * size, e1[1] * size, e1[2] * size),
            Prim.add(c, -e2[0] * h, -e2[1] * h, -e2[2] * h),
            Prim.add(c, -e1[0] * size, -e1[1] * size, -e1[2] * size),
            col
         );
      }
   }

   static double twinkle(Builder b, double period, double phase) {
      return b.animate ? Anim.pulse(b.time, period, phase, 0.16) : 0.0;
   }

   static void pearl(Builder b, double cu, double cv, double cw, double r, int tint) {
      int lo = Col.tint(tint, 0.62);
      int hi = Col.tint(tint, 0.93);
      Prim.sphere(b, cu, cv, cw, r, 5, 3, (u, v) -> Col.mix(lo, hi, v));
   }

   static int metal(int c, double g, double v, double t, double speed) {
      double d = Anim.frac(g - t * speed + 0.5) - 0.5;
      double shine = Math.exp(-(d / 0.09) * (d / 0.09));
      return Col.tint(Col.shade(c, 0.74 + 0.3 * v), 0.42 * shine);
   }

   static double[] ring(double r, double th, double y) {
      return Prim.P(r * Math.cos(th), y, r * Math.sin(th));
   }

   static void mountOnWall(Builder b, double r, double th, double y) {
      double[] p = ring(r, th, y);
      b.push();
      b.translate(p[0], p[1], p[2]);
      b.rotateY((Math.PI / 2) - th);
      b.rotateX(Math.PI / 2);
   }
}
