package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class StormAura implements Cosmetic {
   public static final int NATURAL_1 = 13624319;
   public static final int NATURAL_2 = 5661583;
   public static final int QUADS = 384;
   private static final int PUFFS = 7;
   private static final int DROPS = 30;
   private static final int RIPPLES = 5;
   private static final int GUSTS = 3;
   private static final int GUST_SEGS = 10;
   private static final double CELL = 1.9;
   private static final double STRIKE_LEN = 0.3;
   private final ElementParams p;

   public StormAura() {
      this(ElementParams.defaults());
   }

   public StormAura(ElementParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double ring = 0.35 + 0.45 * this.p.radius();
      double body = this.p.bodyHeight();
      double cloudR = 0.55 + 0.3 * this.p.radius();
      double top = body + 0.55;
      double rainR = cloudR * 1.15;
      double[] cam = b.viewLocal();
      StormAura.Strike s = this.strike(b, t, ring);
      this.cloud(b, cam, cloudR, top, s.flash, t);

      for (int k = 0; k < 30; k++) {
         this.drop(b, cam, rainR, top, k, t);
      }

      for (int k = 0; k < 5; k++) {
         this.ripple(b, rainR, k, t);
      }

      for (int k = 0; k < 3; k++) {
         this.gust(b, cam, ring, body, k, t);
      }

      if (s.flash > 0.02) {
         this.bolt(b, s, cloudR, top);
      }
   }

   private StormAura.Strike strike(Builder b, double t, double ring) {
      int n = b.animate ? (int)Math.floor(t / 1.9) : 1;
      double start = Prim.rnd(n * 7 + 1) * 1.4999999999999998;
      double ph = b.animate ? t - n * 1.9 - start : 0.0;
      double flash = 0.0;
      if (b.animate) {
         if (ph >= 0.0 && ph < 0.3) {
            if (ph < 0.07) {
               flash = 1.0;
            } else if (ph < 0.13) {
               flash = 0.25;
            } else if (ph < 0.23) {
               flash = 0.9;
            } else {
               flash = 0.9 * (1.0 - (ph - 0.23) / 0.06999999999999998);
            }
         }
      } else {
         flash = 1.0;
      }

      double a = (Math.PI * 2) * Prim.rnd(n * 7 + 2);
      double r = Math.max(0.55, ring * (0.9 + 0.6 * Prim.rnd(n * 7 + 3)) + 0.25);
      return new StormAura.Strike(n, flash, r * Math.cos(a), r * Math.sin(a));
   }

   private void cloud(Builder b, double[] cam, double R, double top, double flash, double t) {
      for (int k = 0; k < 7; k++) {
         double cx;
         double cz;
         double cy;
         double rx;
         if (k == 0) {
            cx = 0.0;
            cz = 0.0;
            cy = top;
            rx = R * 0.62;
         } else if (k <= 5) {
            double a = (Math.PI * 2) * (k - 1) / 5.0 + (b.animate ? t * 0.1 : 0.0);
            double d = R * 0.58 * (0.9 + 0.2 * Prim.rnd(k * 59 + 1));
            cx = d * Math.cos(a);
            cz = d * Math.sin(a);
            cy = top + R * (-0.05 + 0.16 * (Prim.rnd(k * 59 + 2) - 0.5)) + (b.animate ? 0.03 * Math.sin(t * 0.9 + k * 1.7) : 0.0);
            rx = R * (0.34 + 0.14 * Prim.rnd(k * 59 + 3));
         } else {
            cx = R * 0.12 * Math.cos(t * 0.1);
            cz = R * 0.12 * Math.sin(t * 0.1);
            cy = top + R * 0.24;
            rx = R * 0.36;
         }

         double ry = rx * 0.72;
         double f = AuraKit.near(cam, cx, cy, cz, b.scale());
         if (!(f <= 0.03)) {
            double tone = 0.92 + 0.16 * Prim.rnd(k * 59 + 4);
            int dark = Col.shade(b.c2, 0.5 * tone);
            int light = Col.mix(b.c2, b.c1, 0.45);
            int lit = Col.tint(b.c1, 0.7);
            double seed = k * 1.9;
            Prim.ellipsoid(b, cx, cy, cz, rx, ry, rx, 7, 4, AuraKit.fade((u, v) -> {
               double lump = 0.94 + 0.06 * Math.cos(u * (Math.PI * 2) * 2.0 + seed);
               int base = Col.shade(Col.mix(dark, light, Math.pow(v, 0.9)), lump);
               return Col.withA(Col.mix(base, lit, flash * 0.85 * (1.0 - 0.5 * v)), 0.92);
            }, f));
         }
      }
   }

   private void drop(Builder b, double[] cam, double rainR, double top, int k, double t) {
      double rate = 0.9 + 0.5 * Prim.rnd(k * 61 + 1);
      double u = b.animate ? AuraKit.frac(t * rate + Prim.rnd(k * 61 + 2)) : Prim.rnd(k * 61 + 2);
      double a = (Math.PI * 2) * Prim.rnd(k * 61 + 3);
      double r = 0.35 + (rainR - 0.35) * Math.sqrt(Prim.rnd(k * 61 + 4));
      double startY = top - 0.2;
      double y = startY * (1.0 - u) + 0.03;
      double len = 0.3;
      double slant = 0.1;
      double x = r * Math.cos(a) + slant * (startY - y);
      double z = r * Math.sin(a);
      double fade = AuraKit.near(cam, x, y, z, b.scale()) * Prim.sm((y - 0.02) / 0.15);
      if (!(fade < 0.04)) {
         double h = 0.007;
         int head = Col.withA(Col.tint(b.c1, 0.5), 0.8 * fade);
         int tail = Col.withA(b.c1, 0.0);
         double dx = slant * len;
         b.quad(Prim.P(x - h, y, z), Prim.P(x + h, y, z), Prim.P(x + h + dx, y + len, z), Prim.P(x - h + dx, y + len, z), head, head, tail, tail);
         b.quad(Prim.P(x, y, z - h), Prim.P(x, y, z + h), Prim.P(x + dx, y + len, z + h), Prim.P(x + dx, y + len, z - h), head, head, tail, tail);
      }
   }

   private void ripple(Builder b, double rainR, int k, double t) {
      double period = 0.9 + 0.7 * Prim.rnd(k * 67 + 1);
      double u = b.animate ? AuraKit.frac(t / period + Prim.rnd(k * 67 + 2)) : 0.35;
      double a = (Math.PI * 2) * Prim.rnd(k * 67 + 3);
      double d = 0.35 + (rainR - 0.35) * Math.sqrt(Prim.rnd(k * 67 + 4));
      double r = 0.04 + 0.2 * u;
      double al = 0.55 * Math.pow(1.0 - u, 1.2);
      if (!(al < 0.03)) {
         int c = Col.withA(Col.tint(b.c1, 0.4), al);
         b.push();
         b.translate(d * Math.cos(a), 0.018, d * Math.sin(a));
         Prim.ring(b, 0.0, r - 0.018, r + 0.018, 8, (uu, vv) -> c);
         b.pop();
      }
   }

   private void gust(Builder b, double[] cam, double ring, double body, int k, double t) {
      double yc = body * (0.18 + 0.3 * k) + 0.1;
      double dir = k % 2 == 0 ? 1.0 : -1.0;
      double a0 = (Math.PI * 2) * Prim.rnd(k * 71 + 1) + (b.animate ? t * (2.0 + 0.5 * k) * dir : 0.0);
      double rg = ring * (0.95 + 0.1 * k);
      int cc = Col.tint(b.c1, 0.3);
      double[][] pts = new double[11][];

      for (int i = 0; i <= 10; i++) {
         double s = i / 10.0;
         double a = a0 + dir * s * (Math.PI * 2) * 0.55;
         pts[i] = Prim.P(rg * Math.cos(a), yc + 0.12 * (s - 0.5), rg * Math.sin(a));
      }

      for (int i = 0; i < 10; i++) {
         double s0 = i / 10.0;
         double s1 = (i + 1) / 10.0;
         double f = AuraKit.near(cam, pts[i][0], pts[i][1], pts[i][2], b.scale());
         if (!(f <= 0.03)) {
            double h0 = 0.055 * Math.pow(Math.sin(Math.PI * s0), 0.8) + 0.004;
            double h1 = 0.055 * Math.pow(Math.sin(Math.PI * s1), 0.8) + 0.004;
            int c0 = Col.withA(cc, 0.38 * Math.sin(Math.PI * s0) * f);
            int c1 = Col.withA(cc, 0.38 * Math.sin(Math.PI * s1) * f);
            double[] p0 = pts[i];
            double[] p1 = pts[i + 1];
            b.quad(Prim.P(p0[0], p0[1] + h0, p0[2]), Prim.P(p0[0], p0[1] - h0, p0[2]), Prim.P(p1[0], p1[1] - h1, p1[2]), Prim.P(p1[0], p1[1] + h1, p1[2]), c0, c0, c1, c1);
         }
      }
   }

   private void bolt(Builder b, StormAura.Strike s, double cloudR, double top) {
      int n = s.id();
      double fl = s.flash();
      double[] start = Prim.P(s.gx() * 0.35, top - 0.12, s.gz() * 0.35);
      double[] end = Prim.P(s.gx(), 0.03, s.gz());
      int seg = 8;
      double[][] pts = new double[seg + 1][];

      for (int i = 0; i <= seg; i++) {
         double q = (double)i / seg;
         double env = Math.sin(Math.PI * q) * 0.9 + 0.1 * (i > 0 && i < seg ? 1 : 0);
         double jx = (Prim.rnd(n * 53 + i * 2) - 0.5) * 0.36 * env;
         double jz = (Prim.rnd(n * 53 + i * 2 + 1) - 0.5) * 0.36 * env;
         pts[i] = Prim.P(start[0] + (end[0] - start[0]) * q + jx, start[1] + (end[1] - start[1]) * q, start[2] + (end[2] - start[2]) * q + jz);
      }

      this.limb(b, pts, 0.036, fl);
      double fa = (Math.PI * 2) * Prim.rnd(n * 53 + 40);
      double[][] fork = new double[5][];
      fork[0] = pts[3];

      for (int i = 1; i <= 4; i++) {
         double q = i / 4.0;
         fork[i] = Prim.P(
            pts[3][0] + Math.cos(fa) * 0.32 * q + (Prim.rnd(n * 53 + 41 + i) - 0.5) * 0.14,
            pts[3][1] - (pts[3][1] - 0.35) * q * 0.85,
            pts[3][2] + Math.sin(fa) * 0.32 * q + (Prim.rnd(n * 53 + 51 + i) - 0.5) * 0.14
         );
      }

      this.limb(b, fork, 0.022, fl);
      AuraKit.fan(b, s.gx(), s.gz(), 0.55, 0.03, 10, 0.0, Col.withA(-1, 0.55 * fl), Col.withA(b.c1, 0.0));
   }

   private void limb(Builder b, double[][] pts, double width, double fl) {
      int core = Col.withA(Col.mix(-1, b.c1, 0.2), fl);
      int glow = Col.withA(b.c1, 0.32 * fl);

      for (int i = 0; i < pts.length - 1; i++) {
         double[] p0 = pts[i];
         double[] p1 = pts[i + 1];
         double taper = 1.0 - 0.4 * i / (pts.length - 1.0);

         for (int pass = 0; pass < 2; pass++) {
            double h = (pass == 0 ? width * 2.0 : width * 0.5) * taper;
            int c = pass == 0 ? glow : core;
            b.quad(Prim.P(p0[0] - h, p0[1], p0[2]), Prim.P(p0[0] + h, p0[1], p0[2]), Prim.P(p1[0] + h, p1[1], p1[2]), Prim.P(p1[0] - h, p1[1], p1[2]), c);
            b.quad(Prim.P(p0[0], p0[1], p0[2] - h), Prim.P(p0[0], p0[1], p0[2] + h), Prim.P(p1[0], p1[1], p1[2] + h), Prim.P(p1[0], p1[1], p1[2] - h), c);
         }
      }
   }

   private record Strike(int id, double flash, double gx, double gz) {
   }
}
