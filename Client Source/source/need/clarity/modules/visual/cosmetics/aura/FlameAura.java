package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class FlameAura implements Cosmetic {
   public static final int NATURAL_1 = 16761395;
   public static final int NATURAL_2 = 16726802;
   public static final int QUADS = 412;
   private static final int MAIN = 10;
   private static final int LICKS = 10;
   private static final int EMBERS = 16;
   private static final double BASE_Y = 0.02;
   private final ElementParams p;

   public FlameAura() {
      this(ElementParams.defaults());
   }

   public FlameAura(ElementParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double ring = 0.35 + 0.27 * this.p.radius();
      double body = this.p.bodyHeight();
      double[] cam = b.viewLocal();
      this.glow(b, ring, AuraKit.wave(b, 3.1, 0.0));

      for (int k = 0; k < 10; k++) {
         double a = (Math.PI * 2) * (k + 0.35 * (Prim.rnd(k * 13 + 1) - 0.5)) / 10.0;
         double rr = ring * (0.94 + 0.12 * Prim.rnd(k * 13 + 2));
         double h = body * (0.58 + 0.47 * Prim.rnd(k * 13 + 3));
         double w = 0.28 * (0.85 + 0.3 * Prim.rnd(k * 13 + 4)) * Math.sqrt(this.p.radius());
         this.tongue(b, cam, rr * Math.cos(a), rr * Math.sin(a), h, w, k, t, true);
      }

      for (int k = 0; k < 10; k++) {
         double a = (Math.PI * 2) * (k + 0.5) / 10.0;
         double rr = ring * 1.06;
         double h = body * (0.2 + 0.2 * Prim.rnd(k * 17 + 5));
         this.tongue(b, cam, rr * Math.cos(a), rr * Math.sin(a), h, 0.17 * Math.sqrt(this.p.radius()), k + 40, t, false);
      }

      this.embers(b, cam, ring, body, t);
   }

   private void glow(Builder b, double ring, double breathe) {
      double peak = 0.34 + 0.1 * breathe;
      int hot = Col.tint(b.c1, 0.2);
      int cool = b.c2;
      Prim.ring(b, 0.02, ring - 0.28, ring, 20, (u, v) -> Col.withA(Col.mix(cool, hot, v), peak * v * v));
      Prim.ring(b, 0.02, ring, ring + 0.32, 20, (u, v) -> Col.withA(Col.mix(hot, cool, v), peak * (1.0 - v) * (1.0 - v)));
   }

   private void tongue(Builder b, double[] cam, double bx, double bz, double height, double width, int seed, double t, boolean tall) {
      int S = tall ? 4 : 3;
      double rl = Math.sqrt(bx * bx + bz * bz);
      double rx = bx / rl;
      double rz = bz / rl;
      double tx = -rz;
      double tz = rx;
      double flick = b.animate ? 0.86 + 0.1 * Math.sin(t * 8.0 + seed * 2.1) + 0.06 * Math.sin(t * 13.7 + seed * 0.9) : 0.93;
      double h = height * flick;
      double f = AuraKit.near(cam, bx, h * 0.5, bz, b.scale());
      if (!(f <= 0.03)) {
         double[][] spine = new double[S + 1][];
         double[] wp = new double[S + 1];

         for (int i = 0; i <= S; i++) {
            double s = (double)i / S;
            double swirl = 0.2 * h * Math.pow(s, 1.7);
            double lean = -0.14 * h * Math.pow(s, 1.5);
            double swT = 0.0;
            double swR = 0.0;
            if (b.animate) {
               swT = 0.06 * s * Math.sin(t * 3.1 + seed * 1.7 - s * 2.6);
               swR = 0.05 * s * Math.sin(t * 2.3 + seed * 2.3 - s * 2.1);
            }

            double ox = tx * (swirl + swT) + rx * (lean + swR);
            double oz = tz * (swirl + swT) + rz * (lean + swR);
            spine[i] = Prim.P(bx + ox, 0.02 + h * s, bz + oz);
            wp[i] = width * profile(s);
         }

         double[] side1 = Prim.P(tx, 0.0, tz);
         double[] side2 = Prim.P(rx, 0.0, rz);
         this.soft(b, spine, wp, side1, f, 0.85);
         this.soft(b, spine, wp, side2, f, 0.85);
         if (tall) {
            int c = 3;
            double[][] core = new double[c + 1][];
            double[] cw = new double[c + 1];

            for (int i = 0; i <= c; i++) {
               double s = (double)i / c;
               core[i] = spine[(int)Math.round(s * 0.75 * S)];
               cw[i] = width * 0.42 * profile(s * 0.8);
            }

            this.hard(b, core, cw, side1, f);
            this.hard(b, core, cw, side2, f);
         }
      }
   }

   private static double profile(double s) {
      return s < 0.28 ? 0.6 + 0.4 * Math.sin(s / 0.28 * Math.PI / 2.0) : Math.pow(Math.max(0.0, (1.0 - s) / 0.72), 0.9);
   }

   private void soft(Builder b, double[][] sp, double[] w, double[] side, double f, double peak) {
      int n = sp.length;
      int hot = Col.tint(b.c1, 0.15);
      int cool = b.c2;
      double[][] l = new double[n][];
      double[][] r = new double[n][];
      edges(sp, w, side, l, r);

      for (int i = 0; i < n - 1; i++) {
         double s0 = i / (n - 1.0);
         double s1 = (i + 1) / (n - 1.0);
         int c0 = flameColour(hot, cool, s0, peak, f);
         int c1 = flameColour(hot, cool, s1, peak, f);
         int e0 = Col.alpha(c0, 0.22);
         int e1 = Col.alpha(c1, 0.22);
         b.quad(l[i], sp[i], sp[i + 1], l[i + 1], e0, c0, c1, e1);
         b.quad(sp[i], r[i], r[i + 1], sp[i + 1], c0, e0, e1, c1);
      }
   }

   private void hard(Builder b, double[][] sp, double[] w, double[] side, double f) {
      int n = sp.length;
      double[][] l = new double[n][];
      double[][] r = new double[n][];
      edges(sp, w, side, l, r);
      int white = Col.tint(b.c1, 0.75);
      int warm = Col.tint(b.c1, 0.25);

      for (int i = 0; i < n - 1; i++) {
         double s0 = i / (n - 1.0);
         double s1 = (i + 1) / (n - 1.0);
         int c0 = AuraKit.fade(Col.withA(Col.mix(white, warm, s0), 0.95 * (1.0 - Math.pow(s0, 1.4))), f);
         int c1 = AuraKit.fade(Col.withA(Col.mix(white, warm, s1), 0.95 * (1.0 - Math.pow(s1, 1.4))), f);
         b.quad(l[i], r[i], r[i + 1], l[i + 1], c0, c0, c1, c1);
      }
   }

   private static int flameColour(int hot, int cool, double s, double peak, double f) {
      double heat = Prim.sm((s - 0.06) / 0.62);
      return AuraKit.fade(Col.withA(Col.mix(hot, cool, heat), peak * (1.0 - Math.pow(s, 3.2))), f);
   }

   private static void edges(double[][] sp, double[] w, double[] side, double[][] l, double[][] r) {
      for (int i = 0; i < sp.length; i++) {
         double[] tg = Prim.tangent(sp, i);
         double k = Prim.dot(side, tg);
         double[] sv = new double[]{side[0] - tg[0] * k, side[1] - tg[1] * k, side[2] - tg[2] * k};
         sv = Prim.unit(sv);
         double hw = w[i] * 0.5;
         l[i] = Prim.P(sp[i][0] + sv[0] * hw, sp[i][1] + sv[1] * hw, sp[i][2] + sv[2] * hw);
         r[i] = Prim.P(sp[i][0] - sv[0] * hw, sp[i][1] - sv[1] * hw, sp[i][2] - sv[2] * hw);
      }
   }

   private void embers(Builder b, double[] cam, double ring, double body, double t) {
      for (int k = 0; k < 16; k++) {
         double period = 2.2 + 2.0 * Prim.rnd(k * 11 + 1);
         double u = b.animate ? AuraKit.frac(t / period + Prim.rnd(k * 11 + 2)) : Prim.rnd(k * 11 + 2);
         double a = (Math.PI * 2) * Prim.rnd(k * 11 + 3) + u * 1.4;
         double rr = ring * (0.55 + 0.55 * Prim.rnd(k * 11 + 4)) * (1.0 - 0.25 * u);
         double x = rr * Math.cos(a);
         double z = rr * Math.sin(a);
         double y = 0.02 + u * (body + 0.35);
         double al = 0.95 * Math.sin(Math.PI * Math.pow(u, 0.7)) * AuraKit.near(cam, x, y, z, b.scale());
         if (!(al < 0.04)) {
            double s = 0.028 * (1.0 - 0.6 * u) * (0.8 + 0.5 * Prim.rnd(k * 11 + 6));
            int c = Col.withA(Col.mix(Col.tint(b.c1, 0.5), b.c2, Prim.sm(u * 1.2)), al);
            b.quad(Prim.P(x - s, y, z), Prim.P(x, y - s * 1.6, z), Prim.P(x + s, y, z), Prim.P(x, y + s * 1.6, z), c);
            b.quad(Prim.P(x, y, z - s), Prim.P(x, y - s * 1.6, z), Prim.P(x, y, z + s), Prim.P(x, y + s * 1.6, z), c);
         }
      }
   }
}
