package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class LightningStrike implements Cosmetic {
   public static final int NATURAL_1 = 7314431;
   public static final int NATURAL_2 = 15135487;
   static final int MAIN_PTS = 17;
   static final int BRANCHES = 5;
   static final int BR_PTS = 6;
   static final int ARCS = 9;
   static final int ARC_PTS = 4;
   static final int RING_SEGS = 24;
   static final int POOL_SEGS = 20;
   static final int FLASH_SEGS = 16;
   static final int COLUMN_SIDES = 12;
   static final int EMBERS = 10;
   public static final int QUADS = 347;
   private final KillParams p;

   public LightningStrike() {
      this(KillParams.defaults());
   }

   public LightningStrike(KillParams p) {
      this.p = p;
   }

   static double envelope(double age, double s) {
      if (!(age <= 0.0) && !(age >= s)) {
         double first = Prim.sm(age / 0.02) * (1.0 - Prim.sm((age - 0.06) / 0.05));
         double second = 0.85 * Prim.sm((age - 0.11) / 0.015) * (1.0 - Prim.sm((age - 0.14) / (s - 0.14)));
         return Math.max(first, second);
      } else {
         return 0.0;
      }
   }

   private static double[][] path(double[] a, double[] c, int n, double amp, int sStable, int sFine) {
      double[][] pts = new double[n][];

      for (int i = 0; i < n; i++) {
         double s = i / (n - 1.0);
         double e = Math.pow(Math.sin(Math.PI * s), 0.7);
         double ox = (0.6 * (2.0 * Prim.rnd(sStable + i * 2) - 1.0) + 0.4 * (2.0 * Prim.rnd(sFine + i * 2) - 1.0)) * amp * e;
         double oz = (0.6 * (2.0 * Prim.rnd(sStable + i * 2 + 1) - 1.0) + 0.4 * (2.0 * Prim.rnd(sFine + i * 2 + 1) - 1.0)) * amp * e;
         pts[i] = Prim.P(a[0] + (c[0] - a[0]) * s + ox, a[1] + (c[1] - a[1]) * s, a[2] + (c[2] - a[2]) * s + oz);
      }

      return pts;
   }

   private static void layer(Builder b, double[][] spine, double[] nearA, double w0, double w1, int col, double alpha, boolean topFade, double[] sideA, double[] sideB) {
      if (!(alpha < 0.004)) {
         Prim.Shade sh = (u, v) -> Col.withA(col, alpha * KillKit.lerpArr(nearA, v) * (topFade ? Prim.sm(v / 0.22) : 1.0));
         Prim.ribbon(b, spine, w0, w1, sideA, sh);
         Prim.ribbon(b, spine, w0, w1, sideB, sh);
      }
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      double life = this.p.life();
      if (!(t >= 1.0)) {
         double h = this.p.height();
         double bulk = this.p.bulk();
         KillParams.Ground gr = this.p.ground();
         double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
         double[] cam = b.viewLocal();
         double kb = KillKit.clamp(Math.pow(h / 1.8, 0.5), 0.6, 2.2);
         double S = Math.min(0.42, 0.45 * life);
         double I = envelope(age, S);
         double endFade = 1.0 - Prim.sm((t - 0.72) / 0.28);
         double rot = Prim.rnd(this.p.seed() * 61 + 3) * (Math.PI * 2);
         int f = (int)Math.floor(Math.min(Math.max(age, 0.0), S) * 24.0);
         int sStable = this.p.seed() * 7 + 1000;
         int sFine = this.p.seed() * 13 + f * 1013 + 77;
         int outer = b.c1;
         int body = Col.mix(b.c1, b.c2, 0.55);
         int core = Col.tint(b.c2, 0.7);
         int white = Col.tint(b.c2, 0.85);
         double H = 13.0 + 4.0 * bulk;
         double th = Prim.rnd(this.p.seed() * 61 + 1) * (Math.PI * 2);
         double slant = 1.6 + 1.6 * Prim.rnd(this.p.seed() * 61 + 2);
         double floor0 = gr.at(0.0, 0.0);
         double[] top = Prim.P(slant * Math.cos(th), floor0 + H, slant * Math.sin(th));
         double[] bot = Prim.P(0.0, floor0 + 0.03, 0.0);
         double[][] main = path(top, bot, 17, 0.62 * (0.8 + 0.2 * bulk), sStable, sFine);
         double[] nearA = KillKit.nearAlong(cam, main, scale);
         double[] X = Prim.P(1.0, 0.0, 0.0);
         double[] Z = Prim.P(0.0, 0.0, 1.0);
         this.scorch(b, gr, kb, age, t);
         this.column(b, cam, scale, h, kb, I);
         this.ring(b, gr, kb, age, endFade, white);
         this.arcs(b, gr, kb, age, rot, I, endFade, sFine, core);
         this.flash(b, gr, kb, age, white);
         double ai = 0.28 * Prim.sm((age - 0.06) / 0.12) * (1.0 - Prim.sm((age - S) / 0.5)) * endFade;
         double kw = KillKit.clamp(0.85 + 0.2 * bulk, 0.9, 1.5);
         layer(b, main, nearA, 0.05 * kw, 0.16 * kw, body, ai, true, X, Z);
         if (I > 0.02) {
            layer(b, main, nearA, 0.2 * kw, 0.62 * kw, outer, 0.15 * I, true, X, Z);
            layer(b, main, nearA, 0.07 * kw, 0.24 * kw, body, 0.5 * I, true, X, Z);
            layer(b, main, nearA, 0.022 * kw, 0.08 * kw, white, 1.0 * I, true, X, Z);
            this.branches(b, gr, main, kb, I, sFine, body, white, cam, scale);
         }

         this.embers(b, gr, kb, age, endFade, rot, white, body, cam, scale);
      }
   }

   private void branches(Builder b, KillParams.Ground gr, double[][] main, double kb, double I, int sFine, int body, int white, double[] cam, double scale) {
      for (int k = 0; k < 5; k++) {
         int sd = this.p.seed() * 43 + k * 19;
         if (!(Prim.rnd(sFine + k * 31 + 5) > 0.8)) {
            int bi = 3 + 2 * k;
            double[] start = main[bi];
            double phi = Prim.rnd(sd + 1) * (Math.PI * 2);
            double psi = 0.45 + 0.55 * Prim.rnd(sd + 2);
            double[] dir = new double[]{Math.cos(phi) * Math.sin(psi), -Math.cos(psi), Math.sin(phi) * Math.sin(psi)};
            double len = (2.5 + 3.5 * Prim.rnd(sd + 3)) * kb;
            double floorY = gr.at(start[0], start[2]) + 0.05;
            double y1 = start[1] + dir[1] * len;
            if (y1 < floorY) {
               len *= Math.max(0.05, (start[1] - floorY) / (start[1] - y1));
            }

            double[] end = Prim.P(start[0] + dir[0] * len, start[1] + dir[1] * len, start[2] + dir[2] * len);
            double[][] br = path(start, end, 6, 0.3 * len / 3.0, this.p.seed() * 5 + k * 101, sFine + k * 7919);
            double[] sideA = Prim.unit(Prim.cross(dir, Prim.P(0.0, 1.0, 0.0)));
            double[] sideB = Prim.unit(Prim.cross(dir, sideA));
            double[] nearA = KillKit.nearAlong(cam, br, scale);
            double[] fade = new double[br.length];

            for (int i = 0; i < br.length; i++) {
               fade[i] = nearA[i] * Math.pow(1.0 - i / (br.length - 1.0), 0.8);
            }

            layer(b, br, fade, 0.12, 0.02, body, 0.55 * I, false, sideA, sideB);
            layer(b, br, fade, 0.04, 0.008, white, 1.0 * I, false, sideA, sideB);
         }
      }
   }

   private void scorch(Builder b, KillParams.Ground gr, double kb, double age, double t) {
      double a = 0.38 * Prim.sm(age / 0.06) * (1.0 - Prim.sm((t - 0.3) / 0.7));
      if (!(a < 0.01)) {
         int dark = Col.dark(b.c1, 0.9);
         KillKit.disc(b, gr, 0.03, 0.75 + 0.5 * kb, 20, (u, v) -> Col.withA(dark, a * Math.pow(1.0 - v, 0.8)));
      }
   }

   private void column(Builder b, double[] cam, double scale, double h, double kb, double I) {
      double a = 0.32 * I * KillKit.near(cam, 0.0, 0.5 * h, 0.0, scale);
      if (!(a < 0.01)) {
         int c1 = b.c1;
         int c2 = b.c2;
         Prim.cylinder(b, 0.03, 1.4 * h + 0.5, 0.45 * kb, 0.3 * kb, 12, (u, v) -> Col.withA(Col.mix(c2, c1, v), a * Math.pow(1.0 - v, 1.2)), false, false);
      }
   }

   private void ring(Builder b, KillParams.Ground gr, double kb, double age, double endFade, int white) {
      double x = KillKit.clamp(age / 0.55, 0.0, 1.0);
      double a = 0.75 * Math.pow(1.0 - x, 1.2) * Prim.sm(age / 0.01) * endFade;
      if (!(a < 0.01)) {
         double r = 0.3 + (2.6 * kb - 0.3) * (1.0 - Math.pow(1.0 - x, 2.2));
         double wd = 0.09 * kb + 0.05 * (1.0 - x);
         int c1 = b.c1;
         KillKit.ring(b, gr, 0.04, Math.max(0.0, r - wd), r + wd * 0.4, 24, (u, v) -> {
            double bell = Math.pow(Math.max(0.0, 1.0 - Math.abs(v - 0.75) / 0.75), 1.1);
            return Col.withA(Col.mix(c1, white, v), a * bell);
         });
      }
   }

   private void arcs(Builder b, KillParams.Ground gr, double kb, double age, double rot, double I, double endFade, int sFine, int hot) {
      double a = 0.85 * Math.max(I, 0.4 * (1.0 - Prim.sm(age / 0.9))) * Prim.sm(age / 0.015) * endFade;
      if (!(a < 0.01)) {
         double grow = 1.0 - Math.pow(1.0 - Math.min(1.0, age / 0.1), 2.0);
         int c1 = b.c1;

         for (int k = 0; k < 9; k++) {
            int sd = this.p.seed() * 29 + k * 13;
            double ang = rot + (Math.PI * 2) * (k + 0.4 * (Prim.rnd(sd + 1) - 0.5)) / 9.0;
            double len = (1.3 + 1.7 * Prim.rnd(sd + 2)) * kb * grow;
            double ca = Math.cos(ang);
            double sa = Math.sin(ang);
            double[] xs = new double[4];
            double[] zs = new double[4];
            double[] ws = new double[4];

            for (int i = 0; i < 4; i++) {
               double d = len * i / 3.0;
               double lat = i == 0 ? 0.0 : (2.0 * Prim.rnd(sFine + 200 + k * 7 + i) - 1.0) * 0.28 * len / 1.5;
               xs[i] = d * ca - lat * sa;
               zs[i] = d * sa + lat * ca;
               ws[i] = 0.085 * kb * (1.0 - 0.7 * i / 3.0);
            }

            KillKit.groundStrip(b, gr, 0.05, xs, zs, ws, (u, v) -> Col.withA(Col.mix(hot, c1, v), a * (1.0 - 0.8 * v)));
         }
      }
   }

   private void flash(Builder b, KillParams.Ground gr, double kb, double age, int white) {
      if (!(age >= 0.22)) {
         double a = 0.85 * (1.0 - Prim.sm(age / 0.22)) * Prim.sm(age / 0.01);
         int c1 = b.c1;
         KillKit.disc(b, gr, 0.06, 0.9 + 0.6 * kb, 16, (u, v) -> Col.withA(Col.mix(white, c1, v), a * Math.pow(1.0 - v, 1.2)));
      }
   }

   private void embers(Builder b, KillParams.Ground gr, double kb, double age, double endFade, double rot, int white, int body, double[] cam, double scale) {
      for (int k = 0; k < 10; k++) {
         int sd = this.p.seed() * 53 + k * 13;
         double born = 0.06 + 0.3 * Prim.rnd(sd + 1);
         double span = 0.5 + 0.7 * Prim.rnd(sd + 2);
         double u = (age - born) / span;
         if (!(u <= 0.0) && !(u >= 1.0)) {
            double a = rot + (Math.PI * 2) * Prim.rnd(sd + 3);
            double rad = (0.2 + 0.9 * Prim.rnd(sd + 4)) * kb * (1.0 + 0.5 * u);
            double x = rad * Math.cos(a);
            double z = rad * Math.sin(a);
            double y = gr.at(x, z) + 0.2 + u * (1.0 + 2.0 * Prim.rnd(sd + 5));
            double al = 0.9 * Math.pow(Math.sin(Math.PI * u), 0.8) * endFade * KillKit.near(cam, x, y, z, scale);
            if (!(al < 0.04)) {
               double s = 0.035 * kb * (1.0 - 0.5 * u);
               int c = Col.withA(Col.mix(white, body, u), al);
               b.quad(Prim.P(x - s, y, z), Prim.P(x, y - s * 1.6, z), Prim.P(x + s, y, z), Prim.P(x, y + s * 1.6, z), c);
               b.quad(Prim.P(x, y, z - s), Prim.P(x, y - s * 1.6, z), Prim.P(x, y, z + s), Prim.P(x, y + s * 1.6, z), c);
            }
         }
      }
   }
}
