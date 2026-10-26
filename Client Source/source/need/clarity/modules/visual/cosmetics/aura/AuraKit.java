package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Prim;

final class AuraKit {
   static final double TAU = Math.PI * 2;
   private static final double NEAR_MIN = 0.3;
   private static final double NEAR_MAX = 0.85;

   private AuraKit() {
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

   static double nearOrigin(Builder b) {
      return near(b.viewLocal(), 0.0, 0.0, 0.0, b.scale());
   }

   static int fade(int c, double f) {
      return f >= 0.999 ? c : Col.alpha(c, f);
   }

   static Prim.Shade fade(Prim.Shade s, double f) {
      return f >= 0.999 ? s : (u, v) -> Col.alpha(s.at(u, v), f);
   }

   static double wave(Builder b, double rate, double phase) {
      return b.animate ? 0.5 + 0.5 * Math.sin(b.time * rate + phase) : 0.5;
   }

   static double frac(double x) {
      return x - Math.floor(x);
   }

   static void flatBar(Builder b, double x0, double z0, double x1, double z1, double width, double y, int c0, int c1) {
      double dx = x1 - x0;
      double dz = z1 - z0;
      double len = Math.sqrt(dx * dx + dz * dz);
      if (!(len < 1.0E-9)) {
         double nx = -dz / len * width * 0.5;
         double nz = dx / len * width * 0.5;
         b.quad(Prim.P(x0 + nx, y, z0 + nz), Prim.P(x0 - nx, y, z0 - nz), Prim.P(x1 - nx, y, z1 - nz), Prim.P(x1 + nx, y, z1 + nz), c0, c0, c1, c1);
      }
   }

   static void fan(Builder b, double cx, double cz, double r, double y, int segs, double phase, int cc, int ce) {
      double[] o = Prim.P(cx, y, cz);

      for (int i = 0; i < segs; i++) {
         double a0 = phase + (Math.PI * 2) * i / segs;
         double a1 = phase + (Math.PI * 2) * (i + 1) / segs;
         b.tri(o, Prim.P(cx + r * Math.cos(a0), y, cz + r * Math.sin(a0)), Prim.P(cx + r * Math.cos(a1), y, cz + r * Math.sin(a1)), cc, ce, ce);
      }
   }

   static void streak(Builder b, double x, double y0, double z, double height, double width, int cb, int ct) {
      double h = width * 0.5;
      b.quad(Prim.P(x - h, y0, z), Prim.P(x + h, y0, z), Prim.P(x + h * 0.4, y0 + height, z), Prim.P(x - h * 0.4, y0 + height, z), cb, cb, ct, ct);
      b.quad(Prim.P(x, y0, z - h), Prim.P(x, y0, z + h), Prim.P(x, y0 + height, z + h * 0.4), Prim.P(x, y0 + height, z - h * 0.4), cb, cb, ct, ct);
   }
}
