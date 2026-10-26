package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Phoenix implements Cosmetic {
   public static final int NATURAL_1 = 16738842;
   public static final int NATURAL_2 = 16773280;
   public static final int TAIL = 7;
   static final int BODY_SIDES = 6;
   static final int BODY_PTS = 5;
   static final int NECK_SIDES = 5;
   static final int NECK_PTS = 4;
   static final int HEAD_LON = 6;
   static final int HEAD_LAT = 3;
   static final int BEAK_SIDES = 4;
   static final int CREST = 3;
   static final int CREST_SEGS = 2;
   static final int TAIL_SEGS = 7;
   static final int PRIMARIES = 8;
   static final int SECONDARIES = 7;
   static final int COVERTS = 6;
   static final int FEATHER_SEGS = 3;
   static final int ARM_SIDES = 4;
   static final int FLASH_SEGS = 16;
   static final int RING_SEGS = 28;
   static final int SCORCH_SEGS = 16;
   static final int HEAT_SEGS = 14;
   static final int AURA_SEGS = 14;
   static final int EMBERS = 22;
   static final int SPARKS = 8;
   private static final int WING_QUADS = 59;
   public static final int QUADS = 386;
   private final KillParams p;

   public static int beats(int seed) {
      return 2 + (Prim.rnd(seed * 67 + 4) < 0.5 ? 0 : 1);
   }

   public static double stroke(double t, int seed) {
      double win = KillKit.clamp((t - 0.12) / 0.78, 0.0, 1.0);
      double glide = Prim.sm((t - 0.8) / 0.15);
      return (0.12 + 0.62 * Math.cos((Math.PI * 2) * beats(seed) * win)) * (1.0 - glide) + 0.15 * glide;
   }

   public Phoenix() {
      this(KillParams.defaults());
   }

   public Phoenix(KillParams p) {
      this.p = p;
   }

   private static double[] mad(double[] a, double[] b, double k) {
      return new double[]{a[0] + b[0] * k, a[1] + b[1] * k, a[2] + b[2] * k};
   }

   private static double[] comb(double[] a, double ka, double[] b, double kb) {
      return new double[]{a[0] * ka + b[0] * kb, a[1] * ka + b[1] * kb, a[2] * ka + b[2] * kb};
   }

   private static double[] norm(double[] v, double[] fallback) {
      double l = Math.sqrt(Prim.dot(v, v));
      return l < 1.0E-9 ? fallback : new double[]{v[0] / l, v[1] / l, v[2] / l};
   }

   static double[] euler(double[] X, double[] Z) {
      double yaw = Math.atan2(Z[0], Z[2]);
      double pitch = Math.asin(KillKit.clamp(-Z[1], -1.0, 1.0));
      double cy = Math.cos(yaw);
      double sy = Math.sin(yaw);
      double cx = Math.cos(pitch);
      double sx = Math.sin(pitch);
      double x1 = cy * X[0] - sy * X[2];
      double y1 = X[1];
      double z1 = sy * X[0] + cy * X[2];
      double x2 = x1;
      double y2 = cx * y1 + sx * z1;
      return new double[]{yaw, pitch, Math.atan2(y2, x2)};
   }

   private double size() {
      return Math.max(0.1, 0.95 * this.p.height() + 0.25 * this.p.width());
   }

   private double[] pos(double t, double S, double floor0) {
      double h = this.p.height();
      double e = t * t * (1.7 - 0.7 * t);
      double rho = 1.15 * S * Math.pow(t, 1.4);
      double th = Prim.rnd(this.p.seed() * 67 + 1) * (Math.PI * 2) + (Prim.rnd(this.p.seed() * 67 + 2) < 0.5 ? -1.0 : 1.0) * 0.9 * Math.PI * t;
      return Prim.P(rho * Math.cos(th), floor0 + 0.5 * h + (2.6 * h + S) * e, rho * Math.sin(th));
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      if (!(t >= 1.0)) {
         double S0 = this.size();
         KillParams.Ground gr = this.p.ground();
         double floor0 = gr.at(0.0, 0.0);
         double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
         double[] cam = b.viewLocal();
         int hotW = Col.tint(b.c2, 0.8);
         int mid = b.c2;
         int tip = b.c1;
         double bird = Prim.sm(t / 0.04);
         this.floor(b, gr, S0, t, hotW, mid, tip);
         if (!(bird < 0.01)) {
            double S = S0 * (0.55 + 0.45 * Prim.sm(t / 0.12));
            double[] P0 = this.pos(t, S0, floor0);
            double unfurl = KillKit.clamp((P0[1] - floor0 - 0.3 * S) / (1.6 * S), 0.15, 1.0);
            double sdA = Prim.rnd(this.p.seed() * 67 + 3) * (Math.PI * 2);
            double[] seedH = Prim.P(Math.cos(sdA), 0.0, Math.sin(sdA));
            double[] awayH = seedH;
            double wA = 0.0;
            double[] toCam = Prim.P(-Math.cos(sdA), 0.25, -Math.sin(sdA));
            if (cam != null) {
               double dx = cam[0] - P0[0];
               double dy = cam[1] - P0[1];
               double dz = cam[2] - P0[2];
               double hl = Math.hypot(dx, dz);
               wA = KillKit.clamp(hl / 1.5, 0.0, 1.0);
               if (hl > 1.0E-6) {
                  awayH = Prim.P(-dx / hl, 0.0, -dz / hl);
               }

               toCam = norm(Prim.P(dx, dy, dz), toCam);
            }

            double[] P1 = this.pos(Math.min(1.0, t + 0.01), S0, floor0);
            double[] P2 = this.pos(Math.max(0.0, t - 0.01), S0, floor0);
            double[] tanH = norm(Prim.P(P1[0] - P2[0], 0.0, P1[2] - P2[2]), seedH);
            double[] hd = norm(comb(comb(awayH, wA, seedH, 1.0 - wA), 0.62, tanH, 0.38), seedH);
            double pit = 1.15 - 0.3 * Prim.sm(t);
            double[] Fn = Prim.P(hd[0] * Math.cos(pit), Math.sin(pit), hd[2] * Math.cos(pit));
            double[] up = Prim.P(0.0, 1.0, 0.0);
            double[] Xn = norm(Prim.cross(up, Fn), Prim.P(1.0, 0.0, 0.0));
            double[] Yn = Prim.cross(Fn, Xn);
            double[] Y = norm(comb(toCam, 0.8, Yn, 0.2), Yn);
            double[] Z = norm(mad(Fn, Y, -Prim.dot(Fn, Y)), norm(Prim.cross(Y, Prim.P(1.0, 0.0, 0.0)), Prim.P(0.0, 0.0, 1.0)));
            double[] X = norm(Prim.cross(Y, Z), Prim.P(1.0, 0.0, 0.0));
            double[] eu = euler(X, Z);
            double sp = Prim.sm((t - 0.02) / 0.16);
            double tailDip = 1.3 * S * unfurl * (Math.max(0.0, Z[1]) + 0.25);
            double wingDip = S * (0.35 + 0.8 * sp) * Math.abs(X[1]) + 0.25 * S * Math.abs(Y[1]) + 1.3 * S * Math.max(0.0, Z[1]);
            double lift = Math.max(0.0, floor0 + Math.max(tailDip, wingDip) + 0.03 - P0[1]);
            if (lift > 0.0) {
               P0 = Prim.P(P0[0], P0[1] + lift, P0[2]);
            }

            int nb = beats(this.p.seed());
            double s = Prim.sm((t - 0.02) / 0.16);
            double win = KillKit.clamp((t - 0.12) / 0.78, 0.0, 1.0);
            double ph = (Math.PI * 2) * nb * win;
            double glide = Prim.sm((t - 0.8) / 0.15);
            double beta = 0.95 * (1.0 - s) + stroke(t, this.p.seed()) * s;
            double vel = t > 0.12 && t < 0.9 ? -0.62 * Math.sin(ph) * (Math.PI * 2) * nb / 0.78 * s * (1.0 - glide) : 0.0;
            double bob = 0.03 * S * Math.cos(ph) * s * (1.0 - glide);
            double tipL = 1.3 - 1.6 * Prim.sm((t - 0.5) / 0.5);
            double bodyA = bird * (1.0 - Prim.sm((t - 0.8) / 0.2));
            double nearC = KillKit.near(cam, P0[0], P0[1], P0[2], scale);
            this.heat(b, P0, S, floor0, t, hotW, tip, nearC);
            this.aura(b, P0, S, floor0, toCam, t, bodyA * nearC, hotW, tip);
            b.push();
            b.translate(P0[0], P0[1] + bob, P0[2]).rotateY(eu[0]).rotateX(eu[1]).rotateZ(eu[2]);
            double[] camB = b.viewLocal();
            this.body(b, S, bodyA * nearC, hotW, mid, tip, camB, scale);
            this.tail(b, S, unfurl, age, bird * nearC, tipL, hotW, mid, tip, camB, scale);
            this.wing(b, 1.0, S, s, beta, vel, age, bird * nearC, tipL, hotW, mid, tip, camB, scale);
            this.wing(b, -1.0, S, s, beta, vel, age, bird * nearC, tipL, hotW, mid, tip, camB, scale);
            b.pop();
            this.embers(b, cam, gr, S0, floor0, age, t, hotW, tip, scale);
         }
      }
   }

   private static double wprof(double f) {
      return f < 0.3 ? 0.35 + 0.65 * Prim.sm(f / 0.3) : 1.0 - 0.97 * Math.pow((f - 0.3) / 0.7, 1.4);
   }

   private void feather(
      Builder b,
      double[] root,
      double[] dir,
      double len,
      double wid,
      double[] side,
      double[] bendDir,
      double bend,
      int segs,
      double a,
      double tipL,
      int cRoot,
      int cMid,
      int cTip,
      double[] camB,
      double scale
   ) {
      if (!(a < 0.01) && !(len < 1.0E-4)) {
         double[][] sp = new double[segs + 1][];

         for (int k = 0; k <= segs; k++) {
            double f = (double)k / segs;
            sp[k] = mad(mad(root, dir, len * f), bendDir, bend * len * f * f);
         }

         a *= KillKit.near(camB, sp[segs / 2][0], sp[segs / 2][1], sp[segs / 2][2], scale);
         if (!(a < 0.01)) {
            double[][] l = new double[segs + 1][];
            double[][] r = new double[segs + 1][];
            int[] col = new int[segs + 1];

            for (int k = 0; k <= segs; k++) {
               double f = (double)k / segs;
               double[] tg = norm(Prim.sub(sp[Math.min(k + 1, segs)], sp[Math.max(k - 1, 0)]), dir);
               double[] sv = norm(mad(side, tg, -Prim.dot(side, tg)), norm(Prim.cross(tg, Prim.P(0.0, 1.0, 0.0)), Prim.P(1.0, 0.0, 0.0)));
               double hw = 0.5 * wid * wprof(f);
               l[k] = mad(sp[k], sv, hw);
               r[k] = mad(sp[k], sv, -hw);
               int c = f < 0.25 ? Col.mix(cRoot, cMid, f / 0.25) : Col.mix(cMid, cTip, Prim.sm((f - 0.25) / 0.6));
               col[k] = Col.withA(c, a * Prim.sm((tipL - f) / 0.3));
            }

            for (int k = 0; k < segs; k++) {
               b.quad(l[k], r[k], r[k + 1], l[k + 1], col[k], col[k], col[k + 1], col[k + 1]);
            }
         }
      }
   }

   private double flick(int sd, double age) {
      return 0.5 + 0.5 * Math.sin(age * (7.0 + 7.0 * Prim.rnd(sd)) + (Math.PI * 2) * Prim.rnd(sd + 1));
   }

   private void body(Builder b, double S, double a, int hotW, int mid, int tip, double[] camB, double scale) {
      if (!(a < 0.01)) {
         double[][] sp = new double[5][];

         for (int i = 0; i < 5; i++) {
            double v = i / 4.0;
            sp[i] = Prim.P(0.0, S * (0.02 + 0.03 * Math.sin(Math.PI * v)), S * (-0.32 + 0.62 * v));
         }

         Prim.tube(b, sp, v -> S * 0.145 * Math.pow(Math.sin(Math.PI * (0.1 + 0.82 * v)), 0.7), 6, (u, v) -> {
            double top = 0.5 + 0.5 * Math.cos(u * (Math.PI * 2));
            int c = Col.mix(Col.mix(tip, mid, Prim.sm(v * 1.6)), hotW, 0.25 + 0.75 * top * Prim.sm(v * 1.3));
            return Col.withA(c, a * 0.92);
         }, true, true);
         double[][] nk = new double[][]{Prim.P(0.0, S * 0.03, S * 0.27), Prim.P(0.0, S * 0.11, S * 0.36), Prim.P(0.0, S * 0.2, S * 0.41), Prim.P(0.0, S * 0.25, S * 0.47)};
         Prim.tube(b, nk, v -> S * (0.09 - 0.036 * v), 5, (u, v) -> Col.withA(Col.mix(mid, hotW, 0.35 + 0.4 * v), a * 0.92), false, false);
         Prim.ellipsoid(b, 0.0, S * 0.26, S * 0.52, S * 0.058, S * 0.058, S * 0.072, 6, 3, (u, v) -> Col.withA(Col.mix(mid, hotW, 0.4 + 0.5 * v), a));
         Prim.spike(b, Prim.P(0.0, S * 0.255, S * 0.565), Prim.P(0.0, S * 0.232, S * 0.675), S * 0.028, 4, (u, v) -> Col.withA(Col.mix(mid, tip, 0.25 + 0.5 * v), a));

         for (int k = 0; k < 3; k++) {
            double lam = (k - 1) * 0.3;
            double[] d = norm(Prim.P(Math.sin(lam) * 0.5, 0.55, -0.8), Prim.P(0.0, 0.0, -1.0));
            double ln = S * (k == 1 ? 0.3 : 0.23) * (0.9 + 0.1 * this.flick(this.p.seed() * 71 + k * 5, this.p.age()));
            this.feather(b, Prim.P((k - 1) * 0.012 * S, S * 0.305, S * 0.5), d, ln, S * 0.035, Prim.P(1.0, 0.0, 0.0), Prim.P(0.0, 1.0, 0.0), 0.1, 2, a, 1.5, hotW, mid, tip, camB, scale);
         }
      }
   }

   private void tail(Builder b, double S, double unfurl, double age, double a, double tipL, int hotW, int mid, int tip, double[] camB, double scale) {
      double[] root = Prim.P(0.0, S * 0.01, S * -0.3);

      for (int k = 0; k < 7; k++) {
         int sd = this.p.seed() * 73 + k * 11;
         double c = k - 3.0;
         double lam = c * 0.13;
         double fl = this.flick(sd, age);
         double fl2 = this.flick(sd + 5, age);
         double len = S * unfurl * (1.3 - 0.22 * Math.abs(c)) * (0.88 + 0.12 * fl);
         double[][] sp = new double[8][];

         for (int i = 0; i <= 7; i++) {
            double f = i / 7.0;
            double wx = 0.1 * len * f * Math.sin(8.168140899333462 * f - age * 5.0 + k * 0.9);
            double wy = 0.08 * len * f * Math.sin((Math.PI * 11.0 / 5.0) * f - age * 4.0 + k * 0.7) - 0.15 * len * f * f;
            sp[i] = Prim.P(root[0] + Math.sin(lam) * len * f + wx, root[1] + wy, root[2] - Math.cos(lam) * len * f);
         }

         this.tailRibbon(b, sp, S * (0.075 - 0.01 * Math.abs(c)), a * (0.78 + 0.22 * fl2), tipL, hotW, mid, tip, camB, scale);
      }
   }

   private void tailRibbon(Builder b, double[][] sp, double wid, double a, double tipL, int hotW, int mid, int tip, double[] camB, double scale) {
      int n = sp.length;
      if (!(a < 0.01)) {
         a *= KillKit.near(camB, sp[n / 2][0], sp[n / 2][1], sp[n / 2][2], scale);
         if (!(a < 0.01)) {
            double[][] l = new double[n][];
            double[][] r = new double[n][];
            int[] col = new int[n];

            for (int k = 0; k < n; k++) {
               double f = k / (n - 1.0);
               double[] tg = norm(Prim.sub(sp[Math.min(k + 1, n - 1)], sp[Math.max(k - 1, 0)]), Prim.P(0.0, 0.0, -1.0));
               double[] sv = norm(mad(Prim.P(1.0, 0.0, 0.0), tg, -tg[0]), Prim.P(1.0, 0.0, 0.0));
               double hw = 0.5 * wid * wprof(f);
               l[k] = mad(sp[k], sv, hw);
               r[k] = mad(sp[k], sv, -hw);
               int c = f < 0.2 ? Col.mix(hotW, mid, f / 0.2) : Col.mix(mid, tip, Prim.sm((f - 0.2) / 0.65));
               col[k] = Col.withA(c, a * Prim.sm((tipL - f) / 0.3));
            }

            for (int k = 0; k < n - 1; k++) {
               b.quad(l[k], r[k], r[k + 1], l[k + 1], col[k], col[k], col[k + 1], col[k + 1]);
            }
         }
      }
   }

   private static double[] back(double sx, double[] a, double[] n) {
      double[] c = norm(Prim.cross(a, n), Prim.P(0.0, 0.0, -1.0));
      return Prim.P(-sx * c[0], -sx * c[1], -sx * c[2]);
   }

   private void wing(Builder b, double sx, double S, double spread, double beta, double vel, double age, double a, double tipL, int hotW, int mid, int tip, double[] camB, double scale) {
      if (!(a < 0.01)) {
         double gam = 1.25 * (1.0 - spread) + 0.3 * spread;
         double cb = Math.cos(beta);
         double sb = Math.sin(beta);
         double cg = Math.cos(gam);
         double sg = Math.sin(gam);
         double[] a1 = norm(Prim.P(sx * cg * cb, cg * sb, -sg), Prim.P(sx, 0.0, 0.0));
         double[] n = Prim.P(-sx * sb, cb, 0.0);
         double[] b1 = back(sx, a1, n);
         double dl = 0.3 * spread;
         double[] a2 = norm(comb(a1, Math.cos(dl), b1, Math.sin(dl)), a1);
         double[] b2 = back(sx, a2, n);
         double L1 = 0.3 * S;
         double L2 = 0.27 * S;
         double[] sh = Prim.P(sx * 0.09 * S, 0.03 * S, 0.1 * S);
         double[] wr = mad(sh, a1, L1);
         double[] tp = mad(wr, a2, L2);
         double bend0 = (-0.08 + KillKit.clamp(-0.03 * vel, -0.3, 0.3)) * (0.3 + 0.7 * spread);
         int base = this.p.seed() * 79 + (sx > 0.0 ? 0 : 500);
         int cSec = Col.mix(hotW, mid, 0.45);
         Prim.tube(b, new double[][]{sh, wr, tp}, v -> S * (0.034 - 0.02 * v), 4, (u, v) -> Col.withA(Col.mix(hotW, mid, 0.5 * v), a * 0.9 * Prim.sm(tipL / 0.6)), false, false);
         double fan = 0.12 + 0.88 * spread;

         for (int j = 0; j < 7; j++) {
            double q = j / 6.0;
            double fl = this.flick(base + j * 7 + 100, age);
            double phi = (1.32 - 0.22 * q) * fan;
            double[] d = comb(a1, Math.cos(phi), b1, Math.sin(phi));
            double[] root = mad(sh, a1, L1 * (0.12 + 0.84 * q));
            double[] side = Prim.cross(d, n);
            this.feather(b, root, d, S * (0.27 + 0.1 * q) * (0.9 + 0.1 * fl), S * 0.105, side, n, bend0, 3, a * (0.8 + 0.2 * this.flick(base + j * 7 + 101, age)), tipL, cSec, mid, tip, camB, scale);
         }

         for (int j = 0; j < 8; j++) {
            double q = j / 7.0;
            double fl = this.flick(base + j * 7, age);
            double phi = (0.06 + 0.78 * q) * fan;
            double[] d = comb(a2, Math.cos(phi), b2, Math.sin(phi));
            double[] root = mad(wr, a2, L2 * (1.0 - 0.85 * q));
            double[] side = Prim.cross(d, n);
            this.feather(
               b, root, d, S * (0.44 - 0.12 * q) * (0.88 + 0.12 * fl), S * 0.088, side, n, bend0 * 1.3, 3, a * (0.78 + 0.22 * this.flick(base + j * 7 + 1, age)), tipL, hotW, mid, tip, camB, scale
            );
         }

         for (int j = 0; j < 6; j++) {
            double q = j / 5.0;
            double phi = (1.32 - 0.22 * q) * fan;
            double[] d = comb(a1, Math.cos(phi), b1, Math.sin(phi));
            double[] root = mad(mad(sh, a1, L1 * (0.1 + 0.8 * q)), n, 0.012 * S);
            this.feather(b, root, d, S * 0.17, S * 0.09, Prim.cross(d, n), n, bend0, 1, a * 0.95, tipL, hotW, hotW, mid, camB, scale);
         }
      }
   }

   private void floor(Builder b, KillParams.Ground gr, double S, double t, int hotW, int mid, int tip) {
      double sa = 0.36 * Prim.sm(t / 0.05) * (1.0 - Prim.sm((t - 0.35) / 0.5));
      if (sa > 0.01) {
         int dark = Col.dark(tip, 0.9);
         KillKit.disc(b, gr, 0.03, 0.85 * S, 16, (u, v) -> Col.withA(dark, sa * Math.pow(1.0 - v, 0.8)));
      }

      if (t < 0.1) {
         double x = t / 0.1;
         double fa = 0.9 * (1.0 - Prim.sm(x)) * Prim.sm(t / 0.008);
         if (fa > 0.01) {
            KillKit.disc(b, gr, 0.06, S * (0.5 + 0.9 * x), 16, (u, v) -> Col.withA(Col.mix(hotW, tip, v), fa * Math.pow(1.0 - v, 1.1)));
         }
      }

      double x = KillKit.clamp(t / 0.34, 0.0, 1.0);
      double ra = 0.95 * Math.pow(1.0 - x, 1.2) * Prim.sm(t / 0.01);
      if (ra > 0.01) {
         double r = S * (0.25 + 1.45 * (1.0 - Math.pow(1.0 - x, 2.2)));
         double wd = S * (0.1 * (1.0 - 0.5 * x) + 0.03);
         KillKit.ring(b, gr, 0.04, Math.max(0.0, r - wd), r + wd * 0.5, 28, (u, v) -> {
            double bell = Math.pow(Math.max(0.0, 1.0 - Math.abs(v - 0.7) / 0.7), 1.1);
            return Col.withA(Col.mix(tip, mid, 0.2 + 0.8 * v), ra * bell);
         });
      }
   }

   private void heat(Builder b, double[] c, double S, double floor0, double t, int hotW, int tip, double nearC) {
      double a = 0.3 * Prim.sm(t / 0.05) * (1.0 - Prim.sm((t - 0.55) / 0.35)) * nearC;
      if (!(a < 0.01)) {
         double y = Math.max(c[1] - 0.3 * S, floor0 + 0.03 + 0.02);
         double r = 0.85 * S;
         b.push();
         b.translate(c[0], 0.0, c[2]);
         Prim.disc(b, y, r, 14, (u, v) -> Col.withA(Col.mix(hotW, tip, Math.pow(v, 0.7)), a * Math.pow(1.0 - v, 1.3)));
         b.pop();
      }
   }

   private void aura(Builder b, double[] c, double S, double floor0, double[] toCam, double t, double a, int hotW, int tip) {
      a *= 0.2 * (1.0 - Prim.sm((t - 0.6) / 0.35));
      if (!(a < 0.01)) {
         double[] f = toCam;
         double[] upv = Math.abs(f[1]) > 0.97 ? Prim.P(1.0, 0.0, 0.0) : Prim.P(0.0, 1.0, 0.0);
         double[] rr = norm(Prim.cross(upv, f), Prim.P(1.0, 0.0, 0.0));
         double[] uu = Prim.cross(f, rr);
         double rad = Math.min(0.95 * S, c[1] - floor0 - 0.05);
         if (!(rad < 0.05)) {
            double[] ctr = mad(c, f, -0.12 * Math.min(S, rad));
            double[][] rim = new double[15][];

            for (int i = 0; i <= 14; i++) {
               double an = i % 14 / 14.0 * (Math.PI * 2);
               rim[i] = mad(mad(ctr, rr, Math.cos(an) * rad), uu, Math.sin(an) * rad);
            }

            int c0 = Col.withA(hotW, a);
            int c1 = Col.withA(tip, 0.0);

            for (int i = 0; i < 14; i++) {
               b.tri(ctr, rim[i], rim[i + 1], c0, c1, c1);
            }
         }
      }
   }

   private void embers(Builder b, double[] cam, KillParams.Ground gr, double S, double floor0, double age, double t, int hotW, int tip, double scale) {
      double ks = S * 0.024;

      for (int k = 0; k < 22; k++) {
         int sd = this.p.seed() * 83 + k * 13;
         double born = 0.03 + 0.62 * Prim.rnd(sd + 1);
         double span = 0.2 + 0.25 * Prim.rnd(sd + 2);
         double x = (t - born) / span;
         if (!(x <= 0.0) && !(x >= 1.0)) {
            double px;
            double py;
            double pz;
            if (k % 3 == 0) {
               double an = Prim.rnd(sd + 3) * (Math.PI * 2);
               double rad = (0.15 + 0.85 * Prim.rnd(sd + 4)) * S * (0.9 + 0.4 * x);
               px = rad * Math.cos(an);
               pz = rad * Math.sin(an);
               py = gr.at(px, pz) + 0.15 * S + x * (0.9 + 1.6 * Prim.rnd(sd + 5)) * S;
            } else {
               double[] c = this.pos(born, S, floor0);
               px = c[0] + (Prim.rnd(sd + 3) - 0.5) * 0.9 * S + 0.25 * S * Math.sin(x * 5.0 + k);
               pz = c[2] + (Prim.rnd(sd + 4) - 0.5) * 0.9 * S + 0.25 * S * Math.cos(x * 5.0 + k * 1.3);
               py = c[1] + (Prim.rnd(sd + 6) - 0.5) * 0.5 * S + x * (0.5 + 1.1 * Prim.rnd(sd + 5)) * S;
               py = Math.max(py, gr.at(px, pz) + 0.03 + 1.7 * ks * 1.4);
            }

            double al = 0.95 * Math.pow(Math.sin(Math.PI * x), 0.8) * (1.0 - Prim.sm((t - 0.85) / 0.15)) * KillKit.near(cam, px, py, pz, scale);
            if (!(al < 0.04)) {
               double sz = ks * (0.6 + 0.8 * Prim.rnd(sd + 7)) * (1.0 - 0.5 * x);
               double sl = sz * 1.7;
               int c = Col.withA(Col.mix(hotW, tip, x), al);
               b.quad(Prim.P(px - sz, py, pz), Prim.P(px, py - sl, pz), Prim.P(px + sz, py, pz), Prim.P(px, py + sl, pz), c);
               b.quad(Prim.P(px, py, pz - sz), Prim.P(px, py - sl, pz), Prim.P(px, py, pz + sz), Prim.P(px, py + sl, pz), c);
            }
         }
      }

      for (int k = 0; k < 8; k++) {
         int sd = this.p.seed() * 89 + k * 19;
         double born = 0.1 + 0.55 * Prim.rnd(sd + 1);
         double span = 0.25 + 0.2 * Prim.rnd(sd + 2);
         double x = (t - born) / span;
         if (!(x <= 0.0) && !(x >= 1.0)) {
            double[] c = this.pos(born, S, floor0);
            double px = c[0] + (Prim.rnd(sd + 3) - 0.5) * 1.0 * S + 0.2 * S * Math.sin(x * 4.0 + k);
            double pz = c[2] + (Prim.rnd(sd + 4) - 0.5) * 1.0 * S + 0.2 * S * Math.cos(x * 4.0 + k);
            double py = c[1] - x * 0.7 * S + (Prim.rnd(sd + 5) - 0.5) * 0.4 * S;
            py = Math.max(py, gr.at(px, pz) + 0.03 + 0.075 * S);
            double al = 0.85 * Math.sin(Math.PI * x) * (1.0 - Prim.sm((t - 0.85) / 0.15)) * KillKit.near(cam, px, py, pz, scale);
            if (!(al < 0.04)) {
               double an = Prim.rnd(sd + 6) * (Math.PI * 2) + (3.0 + 4.0 * Prim.rnd(sd + 7)) * x;
               double el = (Prim.rnd(sd + 8) - 0.5) * 1.6;
               double[] d = Prim.P(Math.cos(an) * Math.cos(el), Math.sin(el), Math.sin(an) * Math.cos(el));
               double[] w = norm(Prim.cross(d, Prim.P(0.3, 1.0, 0.2)), Prim.P(1.0, 0.0, 0.0));
               double L = 0.075 * S;
               double W = 0.02 * S;
               int col = Col.withA(Col.mix(hotW, tip, 0.3 + 0.6 * x), al);
               int ct = Col.withA(tip, al * 0.6);
               b.quad(mad(Prim.P(px, py, pz), d, -L), mad(Prim.P(px, py, pz), w, W), mad(Prim.P(px, py, pz), d, L), mad(Prim.P(px, py, pz), w, -W), col, col, ct, col);
            }
         }
      }
   }
}
