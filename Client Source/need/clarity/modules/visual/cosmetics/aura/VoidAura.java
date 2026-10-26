package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class VoidAura implements Cosmetic {
   public static final int NATURAL_1 = 13073919;
   public static final int NATURAL_2 = 3805050;
   public static final int QUADS = 360;
   private static final int ARMS = 4;
   private static final int ARM_SEGS = 8;
   private static final int TENDRILS = 6;
   private static final int SHARDS = 8;
   private static final int MOTES = 22;
   private static final double Y_PIT = 0.012;
   private static final double Y_ARMS = 0.02;
   private static final double Y_RING = 0.028;
   private final ElementParams p;

   public VoidAura() {
      this(ElementParams.defaults());
   }

   public VoidAura(ElementParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double ring = 0.35 + 0.75 * this.p.radius();
      double body = this.p.bodyHeight();
      double[] cam = b.viewLocal();
      this.rift(b, cam, ring, t);

      for (int k = 0; k < 6; k++) {
         this.tendril(b, cam, ring, body, k, t);
      }

      for (int k = 0; k < 8; k++) {
         this.shard(b, cam, ring, body, k, t);
      }

      this.gravityRing(b, ring, body, t);
      this.motes(b, cam, ring, body, t);
   }

   private void rift(Builder b, double[] cam, double ring, double t) {
      double breathe = 1.0 + (b.animate ? 0.025 * Math.sin(t * 1.7) : 0.0);
      double R = ring * breathe;
      int core = Col.shade(b.c2, 0.16);
      int dusk = Col.shade(b.c2, 0.55);
      int hot = Col.tint(b.c1, 0.2);
      Prim.disc(b, 0.012, R * 1.15, 24, (u, v) -> Col.withA(Col.mix(core, dusk, v * v), 0.88 * (1.0 - Prim.sm((v - 0.72) / 0.28))));
      double t2 = t;
      Prim.ring(b, 0.028, R * 0.8, R * 0.9, 20, (u, v) -> Col.withA(Col.mix(b.c2, hot, v), horizon(u, t2) * v));
      Prim.ring(b, 0.028, R * 0.9, R * 1.0, 20, (u, v) -> Col.withA(Col.mix(hot, b.c1, v), horizon(u, t2) * (1.0 - v)));
      double spin = b.animate ? t * 0.7 : 0.0;

      for (int k = 0; k < 4; k++) {
         double a0 = (Math.PI * 2) * k / 4.0 + spin;
         double px = 0.0;
         double pz = 0.0;

         for (int i = 0; i <= 8; i++) {
            double s = i / 8.0;
            double r = R * (0.88 - 0.76 * s);
            double a = a0 + 2.0 * s;
            double x = r * Math.cos(a);
            double z = r * Math.sin(a);
            if (i > 0) {
               double s0 = (i - 1) / 8.0;
               int c0 = armColour(b, hot, s0);
               int c1 = armColour(b, hot, s);
               double w = 0.11 * (1.0 - 0.55 * s0);
               AuraKit.flatBar(b, px, pz, x, z, w, 0.02, c0, c1);
            }

            px = x;
            pz = z;
         }
      }

      double pin = 0.055 + (b.animate ? 0.012 * Math.sin(t * 4.0) : 0.0);
      AuraKit.fan(b, 0.0, 0.0, pin, 0.028, 8, 0.0, Col.withA(-1, 0.9), Col.withA(b.c1, 0.0));
   }

   private static double horizon(double u, double t) {
      double sweep = 0.5 + 0.5 * Math.cos((Math.PI * 2) * (u * 2.0 - t * 0.35));
      return 0.3 + 0.6 * sweep;
   }

   private static int armColour(Builder b, int hot, double s) {
      return Col.withA(Col.mix(hot, b.c1, s), 0.65 * Math.pow(1.0 - s, 0.8));
   }

   private void tendril(Builder b, double[] cam, double ring, double body, int k, double t) {
      double a = (Math.PI * 2) * (k + 0.3 * (Prim.rnd(k * 31 + 1) - 0.5)) / 6.0 + (b.animate ? t * 0.18 : 0.0);
      double r0 = ring * (0.8 + 0.12 * Prim.rnd(k * 31 + 2));
      double h = body * (0.5 + 0.42 * Prim.rnd(k * 31 + 3));
      double cx = Math.cos(a);
      double cz = Math.sin(a);
      double tx = -cz;
      double tz = cx;
      double sway = b.animate ? 0.1 * Math.sin(t * 1.3 + k * 2.2) : 0.0;
      double[] p0 = Prim.P(r0 * cx, 0.02, r0 * cz);
      double[] p1 = Prim.P((r0 + 0.05) * cx + tx * 0.25, h * 0.35, (r0 + 0.05) * cz + tz * 0.25);
      double[] p2 = Prim.P(r0 * 0.65 * cx + tx * (0.35 + sway), h * 0.72, r0 * 0.65 * cz + tz * (0.35 + sway));
      double[] p3 = Prim.P(r0 * 0.28 * cx + tx * 0.42, h, r0 * 0.28 * cz + tz * 0.42);
      int S = 5;
      double[][] spine = Prim.bezier(S, p0, p1, p2, p3);
      double f = AuraKit.near(cam, spine[S / 2][0], spine[S / 2][1], spine[S / 2][2], b.scale());
      if (!(f <= 0.03)) {
         double[] wp = new double[S + 1];

         for (int i = 0; i <= S; i++) {
            double s = (double)i / S;
            wp[i] = 0.22 * (1.0 - Math.pow(s, 0.9)) + 0.01;
         }

         this.strip(b, spine, wp, Prim.P(tx, 0.0, tz), f);
         this.strip(b, spine, wp, Prim.P(cx, 0.0, cz), f);
      }
   }

   private void strip(Builder b, double[][] sp, double[] w, double[] side, double f) {
      int n = sp.length;
      double[][] l = new double[n][];
      double[][] r = new double[n][];

      for (int i = 0; i < n; i++) {
         double[] tg = Prim.tangent(sp, i);
         double k = Prim.dot(side, tg);
         double[] sv = Prim.unit(new double[]{side[0] - tg[0] * k, side[1] - tg[1] * k, side[2] - tg[2] * k});
         double hw = w[i] * 0.5;
         l[i] = Prim.P(sp[i][0] + sv[0] * hw, sp[i][1] + sv[1] * hw, sp[i][2] + sv[2] * hw);
         r[i] = Prim.P(sp[i][0] - sv[0] * hw, sp[i][1] - sv[1] * hw, sp[i][2] - sv[2] * hw);
      }

      int body = Col.shade(b.c2, 0.22);
      int rim = Col.tint(b.c1, 0.1);

      for (int i = 0; i < n - 1; i++) {
         double s0 = i / (n - 1.0);
         double s1 = (i + 1) / (n - 1.0);
         double e0 = 1.0 - Math.pow(s0, 2.6);
         double e1 = 1.0 - Math.pow(s1, 2.6);
         int m0 = AuraKit.fade(Col.withA(body, 0.86 * e0), f);
         int m1 = AuraKit.fade(Col.withA(body, 0.86 * e1), f);
         int d0 = AuraKit.fade(Col.withA(rim, 0.5 * e0), f);
         int d1 = AuraKit.fade(Col.withA(rim, 0.5 * e1), f);
         b.quad(l[i], sp[i], sp[i + 1], l[i + 1], d0, m0, m1, d1);
         b.quad(sp[i], r[i], r[i + 1], sp[i + 1], m0, d0, d1, m1);
      }
   }

   private void shard(Builder b, double[] cam, double ring, double body, int k, double t) {
      double dir = k % 2 == 0 ? 1.0 : -1.0;
      double rate = 0.16 * dir * (0.7 + 0.6 * Prim.rnd(k * 37 + 1));
      double a = (Math.PI * 2) * (k + 0.3 * (Prim.rnd(k * 37 + 2) - 0.5)) / 8.0 + (b.animate ? t * rate : 0.0);
      double r = ring * (0.72 + 0.34 * Prim.rnd(k * 37 + 3));
      double y = body * (0.16 + 0.7 * Prim.rnd(k * 37 + 4)) + (b.animate ? 0.07 * Math.sin(t * 1.1 + k * 1.9) : 0.0);
      double len = 0.22 + 0.16 * Prim.rnd(k * 37 + 5);
      double rr = len * 0.34;
      double x = r * Math.cos(a);
      double z = r * Math.sin(a);
      double f = AuraKit.near(cam, x, y, z, b.scale());
      if (!(f <= 0.03)) {
         int dark = Col.withA(Col.shade(b.c2, 0.35), 0.92);
         int glow = Col.withA(Col.tint(b.c1, 0.15), 0.96);
         Prim.Shade shade = AuraKit.fade((u, v) -> Col.mix(dark, glow, Math.pow(v, 1.6)), f);
         b.push();
         b.translate(x, y, z);
         b.rotateY(b.animate ? t * 0.9 * dir + k : k);
         b.rotateZ((k % 3 - 1) * 0.35 + (b.animate ? 0.25 * Math.sin(t * 0.8 + k) : 0.0));
         Prim.spike(b, Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, len, 0.0), rr, 4, shade);
         Prim.spike(b, Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, -len * 0.75, 0.0), rr, 4, shade);
         b.pop();
      }
   }

   private void gravityRing(Builder b, double ring, double body, double t) {
      double R = ring * 0.92;
      b.push();
      b.translate(0.0, body * 0.5, 0.0);
      b.rotateY(b.animate ? t * 0.35 : 0.6);
      b.rotateZ(1.05 + (b.animate ? 0.08 * Math.sin(t * 0.6) : 0.0));
      double[] cam = b.viewLocal();
      double sc = b.scale();
      int hot = Col.tint(b.c1, 0.35);
      int c1 = b.c1;
      Prim.ring(b, 0.0, R, R + 0.1, 28, (u, v) -> {
         double a = (Math.PI * 2) * u;
         double sweep = Math.pow(0.5 + 0.5 * Math.cos((Math.PI * 2) * (u - t * 0.22)), 3.0);
         double al = (0.3 + 0.62 * sweep) * AuraKit.near(cam, R * Math.cos(a), 0.0, R * Math.sin(a), sc);
         return Col.withA(Col.mix(c1, hot, sweep), al);
      });
      b.pop();
   }

   private void motes(Builder b, double[] cam, double ring, double body, double t) {
      for (int k = 0; k < 22; k++) {
         double period = 2.5 + 2.0 * Prim.rnd(k * 41 + 1);
         double u = b.animate ? AuraKit.frac(t / period + Prim.rnd(k * 41 + 2)) : Prim.rnd(k * 41 + 2);
         double a = (Math.PI * 2) * Prim.rnd(k * 41 + 3) + u * 3.4;
         double r = ring * 1.5 * Math.pow(1.0 - u, 1.3) + 0.05;
         double x = r * Math.cos(a);
         double z = r * Math.sin(a);
         double y = 0.03 + u * u * body * 0.6;
         double al = 0.9 * Math.sin(Math.PI * Math.pow(u, 0.6)) * AuraKit.near(cam, x, y, z, b.scale());
         if (!(al < 0.04)) {
            double s = 0.026 * (0.8 + 0.5 * Prim.rnd(k * 41 + 4));
            int c = Col.withA(Col.mix(Col.tint(b.c1, 0.2), -1, u * u), al);
            b.quad(Prim.P(x - s, y, z), Prim.P(x, y - s * 1.6, z), Prim.P(x + s, y, z), Prim.P(x, y + s * 1.6, z), c);
            b.quad(Prim.P(x, y, z - s), Prim.P(x, y - s * 1.6, z), Prim.P(x, y, z + s), Prim.P(x, y + s * 1.6, z), c);
         }
      }
   }
}
