package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Tiara implements Cosmetic {
   static final double R = 0.262;
   static final double PHI_MAX = Math.toRadians(112.0);
   static final double HELMET_LIFT = 0.05;
   static final int STEPS = 34;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public Tiara() {
      this(HatParams.defaults());
   }

   public Tiara(HatParams p) {
      this.p = p;
   }

   private static double wallH(double phi) {
      return 0.03 + 0.056 * Prim.sm(1.0 - Math.abs(phi) / 1.5);
   }

   private static double[] onWall(double phi, double y, double off) {
      double r = 0.262 + off;
      return Prim.P(r * Math.sin(phi), y, r * Math.cos(phi));
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int gold = b.c1;
      int accent = b.c2;
      int gemAlt = Col.tint(gold, 0.55);
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.05 : 0.0, 0.0);

      for (int i = 0; i < 34; i++) {
         double f0 = i / 34.0;
         double f1 = (i + 1) / 34.0;
         double a0 = -PHI_MAX + 2.0 * PHI_MAX * f0;
         double a1 = -PHI_MAX + 2.0 * PHI_MAX * f1;
         double h0 = wallH(a0);
         double h1 = wallH(a1);
         b.quad(
            onWall(a0, 0.0, 0.0),
            onWall(a1, 0.0, 0.0),
            onWall(a1, h1, 0.0),
            onWall(a0, h0, 0.0),
            GemKit.metal(gold, f0, 0.25, t, 0.1),
            GemKit.metal(gold, f1, 0.25, t, 0.1),
            GemKit.metal(gold, f1, 0.85, t, 0.1),
            GemKit.metal(gold, f0, 0.85, t, 0.1)
         );
         b.quad(
            onWall(a0, 0.0, 0.003),
            onWall(a1, 0.0, 0.003),
            onWall(a1, 0.013, 0.003),
            onWall(a0, 0.013, 0.003),
            GemKit.metal(Col.tint(gold, 0.2), f0, 0.8, t, 0.1),
            GemKit.metal(Col.tint(gold, 0.2), f1, 0.8, t, 0.1),
            GemKit.metal(Col.tint(gold, 0.2), f1, 1.0, t, 0.1),
            GemKit.metal(Col.tint(gold, 0.2), f0, 1.0, t, 0.1)
         );
      }

      double hw = wallH(0.0);
      double[][] prof = new double[][]{{-0.068, 0.0}, {-0.06, 0.05}, {-0.04, 0.102}, {-0.016, 0.152}, {0.0, 0.186}, {0.016, 0.152}, {0.04, 0.102}, {0.06, 0.05}, {0.068, 0.0}};
      double[] hub = onWall(0.0, hw - 0.012, 0.004);

      for (int i = 0; i < prof.length - 1; i++) {
         double[] q0 = onWall(prof[i][0] / 0.262, hw + prof[i][1], 0.004);
         double[] q1 = onWall(prof[i + 1][0] / 0.262, hw + prof[i + 1][1], 0.004);
         double g0 = (double)i / (prof.length - 1);
         double g1 = (double)(i + 1) / (prof.length - 1);
         b.tri(hub, q0, q1, GemKit.metal(gold, 0.5, 0.35, t, 0.1), GemKit.metal(gold, g0, 0.4 + 0.5 * prof[i][1] / 0.186, t, 0.1), GemKit.metal(gold, g1, 0.4 + 0.5 * prof[i + 1][1] / 0.186, t, 0.1));
      }

      stoneAt(b, 0.0, hw + 0.066, 0.026, 0.05, 0.028, 0.01, 0.0, accent, gemAlt, gold, t, true);

      for (int k = -1; k <= 1; k += 2) {
         stoneAt(b, 0.55 * k, 0.038, 0.019, 0.019, 0.013, 0.006, k > 0 ? 1.4 : 0.6, accent, gemAlt, gold, t, false);
      }

      double[] at = new double[]{0.62, 1.0, 1.38};
      double[] ph = new double[]{0.052, 0.042, 0.032};

      for (int i = 0; i < at.length; i++) {
         for (int k = -1; k <= 1; k += 2) {
            double phi = at[i] * k;
            double[] base = onWall(phi, wallH(phi) - 0.004, 0.0);
            double[] tip = Prim.add(base, 0.006 * Math.sin(phi), ph[i], 0.006 * Math.cos(phi));
            double g = 0.5 + 0.5 * phi / PHI_MAX;
            Prim.spike(b, base, tip, 0.0135, 4, (u, v) -> GemKit.metal(gold, g, 0.4 + 0.5 * v, t, 0.1));
            GemKit.pearl(b, tip[0], tip[1] + 0.007, tip[2], 0.0125, accent);
         }
      }

      for (int k = -1; k <= 1; k += 2) {
         int kk = k;
         int steps = 14;
         double[][] spine = new double[steps + 1][];

         for (int i = 0; i <= steps; i++) {
            double s = (double)i / steps;
            double rho = 0.026 * (1.0 - 0.8 * s);
            double th = Math.PI - s * (Math.PI * 2) * 1.9;
            spine[i] = onWall(k * (0.088 + rho * Math.cos(th)) / 0.262, 0.046 + rho * Math.sin(th), 0.005);
         }

         Prim.tube(b, spine, v -> 0.0048, 3, (u, v) -> GemKit.metal(Col.tint(gold, 0.1), 0.5 + 0.1 * kk, 0.6 + 0.3 * Math.cos(u * (Math.PI * 2)), t, 0.1), false, false);
      }
   }

   private static void stoneAt(Builder b, double phi, double y, double ru, double rw, double up, double down, double phase, int base, int alt, int gold, double t, boolean teardrop) {
      GemKit.mountOnWall(b, 0.267, (Math.PI / 2) - phi, y);
      if (!teardrop) {
         Prim.ring(b, 0.004, ru * 0.98, ru * 1.36, 8, (u, v) -> GemKit.metal(Col.tint(gold, 0.25), u, 0.9, t, 0.1));
      }

      GemKit.gem(b, ru, rw, up, down, 6, base, alt, phase);
      GemKit.glint(b, Prim.P(0.0, up, 0.0), teardrop ? 0.1 : 0.055, Col.tint(base, 0.6), GemKit.twinkle(b, teardrop ? 2.6 : 3.4, phase * 0.3));
      b.pop();
   }
}
