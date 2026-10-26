package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class SoulFlame implements Cosmetic {
   public static final int NATURAL_1 = 11860223;
   public static final int NATURAL_2 = 2788607;
   public static final int QUADS = 420;
   private static final int TALL = 8;
   private static final int LICKS = 8;
   private static final int WISPS = 4;
   private static final int WISP_PTS = 8;
   private static final int EMBERS = 18;
   private static final int DISC_SEGS = 20;
   private static final int RING_SEGS = 24;
   private static final double LIFT = 0.02;
   private final TargetParams p;

   public SoulFlame() {
      this(TargetParams.defaults());
   }

   public SoulFlame(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
      double spin = (Math.PI * 2) * this.p.phase();
      double dir = this.p.dir();
      double H = this.p.height();
      double feet = -0.5 * H / scale;
      double body = Math.max(H, 0.9);
      double rr = this.p.hw() + 0.08 * bulk;
      double rise = Prim.sm(intro);
      double grow = 0.15 + 0.85 * rise;
      double vis = (0.4 + 0.6 * intro) * (b.animate ? 0.92 + 0.08 * Math.sin(t * 2.2) : 1.0);
      double[] cam = b.viewLocal();
      int hot = Col.tint(b.c1, 0.35);
      int cool = b.c2;
      this.ground(b, feet, rr, bulk, vis, intro, hot, cool, t);

      for (int k = 0; k < 8; k++) {
         double a = (Math.PI * 2) * (k + 0.3 * (Prim.rnd(k * 13 + 1) - 0.5)) / 8.0 + spin * 0.1;
         double r = rr * (0.94 + 0.12 * Prim.rnd(k * 13 + 2));
         double h = body * (0.74 + 0.34 * Prim.rnd(k * 13 + 3)) * grow;
         double w = 0.26 * (0.85 + 0.3 * Prim.rnd(k * 13 + 4)) * Math.sqrt(bulk);
         this.tongue(b, cam, scale, feet, r * Math.cos(a), r * Math.sin(a), h, w, k, t, dir, vis, hot, cool, true);
      }

      for (int k = 0; k < 8; k++) {
         double a = (Math.PI * 2) * (k + 0.5) / 8.0 + spin * 0.1;
         double r = rr * 1.08;
         double h = body * (0.18 + 0.18 * Prim.rnd(k * 17 + 5)) * grow;
         this.tongue(b, cam, scale, feet, r * Math.cos(a), r * Math.sin(a), h, 0.16 * Math.sqrt(bulk), k + 40, t, dir, vis, hot, cool, false);
      }

      for (int k = 0; k < 4; k++) {
         this.wisp(b, cam, scale, feet, rr, body, bulk, k, t, spin, dir, vis, hot, cool);
      }

      this.embers(b, cam, scale, feet, rr, body, bulk, t, spin, vis, hot, cool);
   }

   private void ground(Builder b, double feet, double rr, double bulk, double vis, double intro, int hot, int cool, double t) {
      double y = feet + 0.02;
      double breathe = b.animate ? 0.85 + 0.15 * Math.sin(t * 2.6) : 1.0;
      Prim.disc(b, y, rr * 1.1, 20, (u, v) -> Col.withA(Col.mix(hot, cool, v), 0.5 * Math.pow(1.0 - v, 1.2) * breathe * vis));
      double pulse = b.animate ? TargetKit.frac(t / 1.9) : 0.35;
      double pr = rr * (0.95 + 0.55 * pulse);
      double pw = 0.05 * bulk * (1.0 - 0.5 * pulse);
      double pa = 0.55 * Math.pow(1.0 - pulse, 1.5) * Prim.sm(pulse * 8.0) * vis;
      Prim.ring(b, y + 0.01, pr - pw, pr + pw, 24, (u, v) -> Col.withA(Col.tint(hot, 0.2), pa * (1.0 - Math.abs(2.0 * v - 1.0) * 0.7)));
      double slam = 1.0 - intro;
      if (slam > 0.01) {
         double fr = rr * (1.0 + 1.6 * intro);
         double fw = 0.09 * bulk;
         Prim.ring(b, y + 0.02, fr - fw, fr + fw, 24, (u, v) -> Col.withA(Col.tint(hot, 0.4), 0.7 * slam * (1.0 - Math.abs(2.0 * v - 1.0) * 0.6)));
      }
   }

   private void tongue(
      Builder b, double[] cam, double scale, double feet, double bx, double bz, double height, double width, int seed, double t, double dir, double vis, int hot, int cool, boolean tall
   ) {
      int S = tall ? 4 : 3;
      double rl = Math.sqrt(bx * bx + bz * bz);
      double rx = bx / rl;
      double rz = bz / rl;
      double tx = -rz;
      double tz = rx;
      double flick = b.animate ? 0.84 + 0.1 * Math.sin(t * 7.3 + seed * 2.1) + 0.06 * Math.sin(t * 12.1 + seed * 0.9) : 0.9;
      double h = height * flick;
      double f = TargetKit.near(cam, bx, feet + h * 0.5, bz, scale);
      if (!(f <= 0.03) && !(h < 0.001)) {
         double[][] spine = new double[S + 1][];
         double[] wp = new double[S + 1];

         for (int i = 0; i <= S; i++) {
            double s = (double)i / S;
            double swirl = dir * 0.22 * h * Math.pow(s, 1.8);
            double lean = -0.22 * h * Math.pow(s, 1.4);
            double swT = 0.0;
            double swR = 0.0;
            if (b.animate) {
               swT = 0.06 * s * Math.sin(t * 3.0 + seed * 1.7 - s * 2.4);
               swR = 0.05 * s * Math.sin(t * 2.4 + seed * 2.3 - s * 1.9 + Math.sin(s * Math.PI * 1.4));
            }

            spine[i] = Prim.P(bx + tx * (swirl + swT) + rx * (lean + swR), feet + 0.02 + h * s, bz + tz * (swirl + swT) + rz * (lean + swR));
            wp[i] = width * profile(s);
         }

         double[] side1 = Prim.P(tx, 0.0, tz);
         double[] side2 = Prim.P(rx, 0.0, rz);
         this.soft(b, spine, wp, side1, f, vis, hot, cool);
         if (tall) {
            this.soft(b, spine, wp, side2, f, vis, hot, cool);
            int c = 3;
            double[][] core = new double[c + 1][];
            double[] cw = new double[c + 1];

            for (int i = 0; i <= c; i++) {
               double s = (double)i / c;
               core[i] = spine[(int)Math.round(s * 0.7 * S)];
               cw[i] = width * 0.4 * profile(s * 0.75);
            }

            this.hard(b, core, cw, side1, f, vis, hot);
            this.hard(b, core, cw, side2, f, vis, hot);
         }
      }
   }

   private static double profile(double s) {
      return s < 0.22 ? 0.55 + 0.45 * Math.sin(s / 0.22 * Math.PI / 2.0) : Math.pow(Math.max(0.0, (1.0 - s) / 0.78), 1.3);
   }

   private void soft(Builder b, double[][] sp, double[] w, double[] side, double f, double vis, int hot, int cool) {
      int n = sp.length;
      double[][] l = new double[n][];
      double[][] r = new double[n][];
      edges(sp, w, side, l, r);

      for (int i = 0; i < n - 1; i++) {
         double s0 = i / (n - 1.0);
         double s1 = (i + 1) / (n - 1.0);
         int c0 = body(hot, cool, s0, f, vis);
         int c1 = body(hot, cool, s1, f, vis);
         int e0 = Col.alpha(c0, 0.22);
         int e1 = Col.alpha(c1, 0.22);
         b.quad(l[i], sp[i], sp[i + 1], l[i + 1], e0, c0, c1, e1);
         b.quad(sp[i], r[i], r[i + 1], sp[i + 1], c0, e0, e1, c1);
      }
   }

   private void hard(Builder b, double[][] sp, double[] w, double[] side, double f, double vis, int hot) {
      int n = sp.length;
      double[][] l = new double[n][];
      double[][] r = new double[n][];
      edges(sp, w, side, l, r);
      int white = Col.tint(hot, 0.65);
      int warm = Col.tint(hot, 0.15);

      for (int i = 0; i < n - 1; i++) {
         double s0 = i / (n - 1.0);
         double s1 = (i + 1) / (n - 1.0);
         int c0 = Col.withA(Col.mix(white, warm, s0), 0.92 * (1.0 - Math.pow(s0, 1.4)) * f * vis);
         int c1 = Col.withA(Col.mix(white, warm, s1), 0.92 * (1.0 - Math.pow(s1, 1.4)) * f * vis);
         b.quad(l[i], r[i], r[i + 1], l[i + 1], c0, c0, c1, c1);
      }
   }

   private static int body(int hot, int cool, double s, double f, double vis) {
      double heat = Prim.sm((s - 0.06) / 0.62);
      return Col.withA(Col.mix(hot, cool, heat), 0.8 * (1.0 - Math.pow(s, 2.6)) * f * vis);
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

   private void wisp(Builder b, double[] cam, double scale, double feet, double rr, double body, double bulk, int k, double t, double spin, double dir, double vis, int hot, int cool) {
      double tail = 0.34;
      double rate = 0.17 + 0.07 * Prim.rnd(k * 19 + 2);
      double sh = b.animate ? TargetKit.frac(t * rate + Prim.rnd(k * 19 + 1)) : 0.35 + 0.5 * Prim.rnd(k * 19 + 1);
      if (!(sh < 0.07)) {
         double top = 1.12 * body;
         double a0 = (Math.PI * 2) * (k / 4.0) + Prim.rnd(k * 19 + 3);
         double fade = Prim.sm((1.0 - sh) / 0.3) * Prim.sm(sh / 0.1);
         if (!(fade < 0.03)) {
            double len = Math.min(0.34, sh);
            double[][] spine = new double[8][];

            for (int i = 0; i < 8; i++) {
               double s = sh - len * (1.0 - i / 7.0);
               double ang = a0 + dir * s * 1.25 * (Math.PI * 2) + spin * 0.55;
               double rad = rr * (1.16 + 0.1 * Math.sin(s * 5.0 + k * 1.7 + (b.animate ? t * 0.9 : 0.0)));
               spine[i] = Prim.P(rad * Math.cos(ang), feet + 0.02 + s * top, rad * Math.sin(ang));
            }

            double[] head = spine[7];
            double f = TargetKit.near(cam, head[0], head[1], head[2], scale);
            if (!(f <= 0.03)) {
               double rHead = 0.03 * bulk * (0.85 + 0.3 * Prim.rnd(k * 19 + 4));
               int headCol = Col.tint(hot, 0.6);
               Prim.tube(b, spine, v -> rHead * (0.1 + 0.9 * Math.pow(v, 0.8)), 3, (u, v) -> Col.withA(Col.mix(cool, hot, Math.pow(v, 0.7)), 0.85 * Math.pow(v, 1.3) * fade * f * vis), false, false);
               double s = rHead * 2.0;
               double x = head[0];
               double y = head[1];
               double z = head[2];
               int c = Col.withA(headCol, 0.95 * fade * f * vis);
               b.quad(Prim.P(x - s, y, z), Prim.P(x, y - s * 1.5, z), Prim.P(x + s, y, z), Prim.P(x, y + s * 1.5, z), c);
               b.quad(Prim.P(x, y, z - s), Prim.P(x, y - s * 1.5, z), Prim.P(x, y, z + s), Prim.P(x, y + s * 1.5, z), c);
            }
         }
      }
   }

   private void embers(Builder b, double[] cam, double scale, double feet, double rr, double body, double bulk, double t, double spin, double vis, int hot, int cool) {
      for (int k = 0; k < 18; k++) {
         double period = 2.2 + 2.0 * Prim.rnd(k * 11 + 1);
         double u = b.animate ? TargetKit.frac(t / period + Prim.rnd(k * 11 + 2)) : Prim.rnd(k * 11 + 2);
         double a = (Math.PI * 2) * Prim.rnd(k * 11 + 3) + u * 1.4 + spin * 0.2;
         double rad = rr * (0.5 + 0.65 * Prim.rnd(k * 11 + 4)) * (1.0 - 0.3 * u);
         double x = rad * Math.cos(a);
         double z = rad * Math.sin(a);
         double y = feet + 0.02 + u * (body + 0.3);
         double al = 0.95 * Math.sin(Math.PI * Math.pow(u, 0.7)) * TargetKit.near(cam, x, y, z, scale) * vis;
         if (!(al < 0.04)) {
            double s = 0.026 * bulk * (1.0 - 0.6 * u) * (0.8 + 0.5 * Prim.rnd(k * 11 + 6));
            int c = Col.withA(Col.mix(Col.tint(hot, 0.5), cool, Prim.sm(u * 1.2)), al);
            b.quad(Prim.P(x - s, y, z), Prim.P(x, y - s * 1.6, z), Prim.P(x + s, y, z), Prim.P(x, y + s * 1.6, z), c);
            b.quad(Prim.P(x, y, z - s), Prim.P(x, y - s * 1.6, z), Prim.P(x, y, z + s), Prim.P(x, y + s * 1.6, z), c);
         }
      }
   }
}
