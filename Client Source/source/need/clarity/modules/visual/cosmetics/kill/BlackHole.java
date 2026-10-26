package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class BlackHole implements Cosmetic {
   public static final int NATURAL_1 = 10837247;
   public static final int NATURAL_2 = 16760952;
   private static final int HOLE_LON = 12;
   private static final int HOLE_LAT = 6;
   private static final int DISC_SEGS = 32;
   private static final int ARMS = 4;
   private static final int ARM_SEGS = 20;
   private static final int DEBRIS = 20;
   private static final int RING_SEGS = 28;
   private static final int HALO_SEGS = 16;
   private static final int RIPPLES = 2;
   private static final int RIPPLE_SEGS = 24;
   private static final int POP_SEGS = 24;
   private static final int POP_SPIKES = 8;
   private static final int VORT_ARMS = 6;
   private static final int VORT_SEGS = 14;
   private static final int FLOOR_SEGS = 20;
   public static final int QUADS = 500;
   private final KillParams p;

   public BlackHole() {
      this(KillParams.defaults());
   }

   public BlackHole(KillParams p) {
      this.p = p;
   }

   private static double[][] basis(double[] c) {
      double[] f = c == null ? new double[]{0.0, 0.0, -1.0} : Prim.unit(c);
      double[] up = Math.abs(f[1]) > 0.97 ? new double[]{1.0, 0.0, 0.0} : new double[]{0.0, 1.0, 0.0};
      double[] r = Prim.unit(Prim.cross(up, f));
      return new double[][]{r, Prim.cross(f, r), f};
   }

   private static double[] at(double[][] bs, double a, double rad) {
      double c = Math.cos(a) * rad;
      double s = Math.sin(a) * rad;
      return Prim.P(bs[0][0] * c + bs[1][0] * s, bs[0][1] * c + bs[1][1] * s, bs[0][2] * c + bs[1][2] * s);
   }

   private static void billboardRing(Builder b, double[][] bs, double rIn, double rOut, int segs, Prim.Shade s) {
      if (!(rOut <= rIn + 1.0E-6)) {
         for (int i = 0; i < segs; i++) {
            double t0 = (double)i / segs;
            double t1 = (double)(i + 1) / segs;
            double a0 = t0 * (Math.PI * 2);
            double a1 = t1 * (Math.PI * 2);
            b.quad(at(bs, a0, rIn), at(bs, a0, rOut), at(bs, a1, rOut), at(bs, a1, rIn), s.at(t0, 0.0), s.at(t0, 1.0), s.at(t1, 1.0), s.at(t1, 0.0));
         }
      }
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      if (!(t >= 1.0)) {
         double H = this.p.height();
         double sz = KillKit.clamp(Math.pow(H / 1.8, 0.6), 0.55, 2.6);
         double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
         KillParams.Ground gr = this.p.ground();
         double rot = Prim.rnd(this.p.seed() * 71 + 5) * (Math.PI * 2);
         double dir = Prim.rnd(this.p.seed() * 71 + 6) < 0.5 ? -1.0 : 1.0;
         double vis = Prim.sm(t / 0.06) * (1.0 - Prim.sm((t - 0.94) / 0.06));
         double grow = Prim.sm(t / 0.16);
         double coll = Prim.sm((t - 0.72) / 0.2);
         double rr = 0.42 * sz * grow * (1.0 - 0.92 * coll);
         double cy = 0.52 * H + 0.3 * sz * Prim.sm(t / 0.35);
         if (vis > 0.004) {
            this.floor(b, gr, sz, age, rot, dir, vis, grow);
         }

         if (vis > 0.004 && rr > 0.01) {
            b.push();
            b.translate(0.0, cy, 0.0).rotateY(rot + 0.35 * age).rotateX(0.42 + 0.1 * Math.sin(age * 0.8));
            this.hole(b, scale, sz, rr, age, t, rot, dir, vis, grow, coll);
            b.pop();
         }

         this.pop(b, cy, sz, t, rot);
      }
   }

   private void floor(Builder b, KillParams.Ground gr, double sz, double age, double rot, double dir, double vis, double grow) {
      int c1 = b.c1;
      int c2 = b.c2;
      int dark = Col.dark(b.c1, 0.9);
      double da = 0.3 * vis;
      KillKit.disc(b, gr, 0.03, 1.6 * sz * grow, 20, (u, v) -> Col.withA(dark, da * Math.pow(1.0 - v, 0.8)));
      double wa = 0.5 * vis;

      for (int k = 0; k < 6; k++) {
         double a0 = rot + (Math.PI * 2) * k / 6.0;
         double[] xs = new double[15];
         double[] zs = new double[15];
         double[] ws = new double[15];

         for (int i = 0; i <= 14; i++) {
            double s = i / 14.0;
            double r = 3.6 * sz * grow * (1.0 - 0.9 * s);
            double th = a0 + dir * (age * 0.8 + s * 2.2 * Math.PI);
            xs[i] = r * Math.cos(th);
            zs[i] = r * Math.sin(th);
            ws[i] = 0.1 * sz * (1.0 - 0.6 * s);
         }

         KillKit.groundStrip(b, gr, 0.04, xs, zs, ws, (u, v) -> Col.withA(Col.mix(c1, c2, v), wa * Math.sin(Math.PI * Math.pow(v, 0.6))));
      }
   }

   private void hole(Builder b, double scale, double sz, double rr, double age, double t, double rot, double dir, double vis, double grow, double coll) {
      double[] c = b.viewLocal();
      double[][] bs = basis(c);
      double fc = KillKit.near(c, 0.0, 0.0, 0.0, scale);
      int c1 = b.c1;
      int c2 = b.c2;
      int hot = Col.tint(b.c2, 0.75);
      double ha = 0.2 * vis * fc;
      double[][] hb = new double[][]{bs[0], bs[1], bs[2]};
      double off = -0.1 * rr;
      b.push();
      b.translate(bs[2][0] * off, bs[2][1] * off, bs[2][2] * off);
      double hr = 3.2 * rr;

      for (int i = 0; i < 16; i++) {
         double t0 = i / 16.0;
         double t1 = (i + 1) / 16.0;
         b.tri(Prim.P(0.0, 0.0, 0.0), at(hb, t0 * (Math.PI * 2), hr), at(hb, t1 * (Math.PI * 2), hr), Col.withA(c2, ha * 0.9), Col.withA(c1, 0.0), Col.withA(c1, 0.0));
      }

      b.pop();

      for (int k = 0; k < 2; k++) {
         double x = KillKit.frac(age * 0.55 + k * 0.5);
         double r = rr * (1.6 + 4.2 * x);
         double a = 0.28 * (1.0 - x) * Prim.sm(x / 0.1) * vis * fc * grow;
         if (!(a < 0.01)) {
            double wd = 0.06 * rr * (1.0 + x);
            billboardRing(b, bs, r - wd, r + wd, 24, (u, v) -> Col.withA(c1, a * (1.0 - Math.abs(2.0 * v - 1.0))));
         }
      }

      ha = 1.55 * rr;
      double ro = 3.9 * rr;
      double[] rf = new double[]{0.0, 0.35, 1.0};
      double[][][] pos = new double[3][33][];
      int[][] col = new int[3][33];
      double beam = rot * 1.7;

      for (int q = 0; q < 3; q++) {
         double rho = ha + (ro - ha) * rf[q];
         int base = rf[q] < 0.35 ? Col.mix(hot, c2, rf[q] / 0.35) : Col.mix(c2, c1, (rf[q] - 0.35) / 0.65);
         double ab = q == 0 ? 0.92 : (q == 1 ? 0.8 : 0.0);

         for (int i = 0; i <= 32; i++) {
            double th = i % 32 / 32.0 * (Math.PI * 2);
            pos[q][i] = Prim.P(rho * Math.cos(th), 0.0, rho * Math.sin(th));
            double arm = 0.5 + 0.5 * Math.sin(2.0 * th - dir * age * (8.0 / (0.5 + rf[q])) + 5.0 * rf[q]);
            double dop = 0.72 + 0.28 * Math.cos(th - beam);
            double nf = KillKit.near(c, pos[q][i][0], pos[q][i][1], pos[q][i][2], scale);
            col[q][i] = Col.withA(Col.shade(base, 0.65 + 0.65 * arm * dop), ab * (0.6 + 0.4 * arm) * vis * nf);
         }
      }

      boolean[] far = new boolean[32];

      for (int i = 0; i < 32; i++) {
         if (c == null) {
            far[i] = true;
         } else {
            double[] m = pos[1][i];
            far[i] = m[0] * c[0] + m[1] * c[1] + m[2] * c[2] < 0.0;
         }
      }

      this.discPass(b, pos, col, far, true);
      this.jets(b, c, sz, rr, age, t, vis, fc, true);
      int rim = Col.tint(c1, 0.2);
      Prim.sphere(b, 0.0, 0.0, 0.0, rr, 12, 6, (u, v) -> {
         double fi = (-Math.PI / 2) + Math.PI * v;
         double an = u * (Math.PI * 2);
         double[] n = new double[]{Math.cos(fi) * Math.cos(an), Math.sin(fi), Math.cos(fi) * Math.sin(an)};
         double[] pt = Prim.P(rr * n[0], rr * n[1], rr * n[2]);
         double f = b.facing(pt, n);
         double edge = Math.pow(1.0 - f, 2.5);
         return Col.withA(Col.mix(-16448760, rim, edge * 0.85), 0.97 * vis * KillKit.near(c, pt[0], pt[1], pt[2], scale));
      });
      double pa = 0.95 * vis * fc;
      billboardRing(b, bs, 1.1 * rr, 1.3 * rr, 28, (u, v) -> {
         double bell = Math.pow(Math.max(0.0, 1.0 - Math.abs(2.0 * v - 1.0)), 0.9);
         double shimmer = 0.75 + 0.25 * Math.sin(age * 2.0 + u * (Math.PI * 2) * 3.0);
         return Col.withA(Col.mix(hot, c1, v), pa * bell * shimmer);
      });
      this.jets(b, c, sz, rr, age, t, vis, fc, false);
      this.discPass(b, pos, col, far, false);
      this.arms(b, c, scale, sz, rr, ha, ro, age, rot, dir, vis, hot, c1);
      this.debris(b, c, scale, sz, ha, ro, age, dir, vis, hot, c1);
   }

   private void discPass(Builder b, double[][][] pos, int[][] col, boolean[] far, boolean wantFar) {
      for (int i = 0; i < 32; i++) {
         if (far[i] == wantFar) {
            for (int q = 0; q < 2; q++) {
               b.quad(pos[q][i], pos[q + 1][i], pos[q + 1][i + 1], pos[q][i + 1], col[q][i], col[q + 1][i], col[q + 1][i + 1], col[q][i + 1]);
            }
         }
      }
   }

   private void jets(Builder b, double[] c, double sz, double rr, double age, double t, double vis, double fc, boolean farHalf) {
      double jv = Prim.sm((t - 0.12) / 0.1) * (1.0 - Prim.sm((t - 0.7) / 0.12));
      if (!(jv * vis * fc < 0.01)) {
         int hot = Col.tint(b.c2, 0.5);
         int c1 = b.c1;

         for (int sgn = -1; sgn <= 1; sgn += 2) {
            double flick = 0.85 + 0.15 * Math.sin(age * 9.0 + sgn * 1.9) * Math.sin(age * 5.3);
            double L = (1.6 + 1.4 * flick) * sz * jv;
            boolean isFar = c != null && sgn * c[1] < 0.0;
            if (c == null) {
               isFar = sgn < 0;
            }

            if (isFar == farHalf) {
               double a = 0.55 * jv * vis * fc;
               double[] base = Prim.P(0.0, sgn * 0.9 * rr, 0.0);
               double[] tip = Prim.P(0.0, sgn * (0.9 * rr + L), 0.0);
               Prim.spike(b, base, tip, 0.28 * rr, 6, (u, v) -> Col.withA(Col.mix(hot, c1, v), a * (1.0 - v)));
               Prim.spike(b, base, Prim.P(0.0, sgn * (0.9 * rr + L * 1.15), 0.0), 0.11 * rr, 4, (u, v) -> Col.withA(Col.tint(hot, 0.5), Math.min(1.0, a * 1.4) * (1.0 - 0.6 * v)));
            }
         }
      }
   }

   private void arms(Builder b, double[] c, double scale, double sz, double rr, double ri, double ro, double age, double rot, double dir, double vis, int hot, int c1) {
      for (int k = 0; k < 4; k++) {
         double a0 = rot + (Math.PI * 2) * k / 4.0;
         double[][] sp = new double[21][];

         for (int i = 0; i <= 20; i++) {
            double s = i / 20.0;
            double rho = ro * 1.9 + (ri * 1.02 - ro * 1.9) * s;
            double th = a0 + dir * (age * 1.1 + 8.168140899333462 * Math.pow(s, 0.85));
            sp[i] = Prim.P(rho * Math.cos(th), 0.0, rho * Math.sin(th));
         }

         double[][] l = new double[21][];
         double[][] r = new double[21][];
         int[] cc = new int[21];

         for (int i = 0; i <= 20; i++) {
            double[] tg = Prim.tangent(sp, i);
            double[] sd = Prim.unit(Prim.cross(Prim.P(0.0, 1.0, 0.0), tg));
            double s = i / 20.0;
            double hw = 0.05 * sz * (1.0 - 0.5 * s) * (0.6 + 0.4 * grow(s));
            l[i] = Prim.P(sp[i][0] + sd[0] * hw, sp[i][1], sp[i][2] + sd[2] * hw);
            r[i] = Prim.P(sp[i][0] - sd[0] * hw, sp[i][1], sp[i][2] - sd[2] * hw);
            double al = 0.6 * vis * Math.sin(Math.PI * Math.pow(s, 0.7)) * KillKit.near(c, sp[i][0], sp[i][1], sp[i][2], scale);
            cc[i] = Col.withA(Col.mix(c1, hot, s), al);
         }

         for (int i = 0; i < 20; i++) {
            b.quad(l[i], r[i], r[i + 1], l[i + 1], cc[i], cc[i], cc[i + 1], cc[i + 1]);
         }
      }
   }

   private static double grow(double s) {
      return Math.min(1.0, s * 2.0);
   }

   private void debris(Builder b, double[] c, double scale, double sz, double ri, double ro, double age, double dir, double vis, int hot, int c1) {
      for (int k = 0; k < 20; k++) {
         int sd = this.p.seed() * 97 + k * 19;
         double rate = 0.35 + 0.45 * Prim.rnd(sd + 1);
         double ph = Prim.rnd(sd + 2);
         double th0 = Prim.rnd(sd + 3) * (Math.PI * 2);
         double s = KillKit.frac(age * rate + ph);
         double rho = ro * 1.8 + (ri * 1.05 - ro * 1.8) * Math.pow(s, 1.15);
         double th = th0 + dir * (age * 0.9 + s * 1.7 * Math.PI);
         double y = (Prim.rnd(sd + 4) - 0.5) * 0.25 * rho * (1.0 - s);
         double[] pos = Prim.P(rho * Math.cos(th), y, rho * Math.sin(th));
         double[] tg = Prim.P(-dir * Math.sin(th), 0.0, dir * Math.cos(th));
         double len = (0.05 + 0.32 * s) * sz;
         double wd = 0.02 * sz * (1.0 + s);
         double al = 0.9 * vis * Math.pow(Math.sin(Math.PI * s), 0.9) * KillKit.near(c, pos[0], pos[1], pos[2], scale);
         if (!(al < 0.03)) {
            double[] rad = Prim.P(Math.cos(th), 0.0, Math.sin(th));
            double[] tail = Prim.P(pos[0] - tg[0] * len, pos[1], pos[2] - tg[2] * len);
            double[] mid = Prim.P(pos[0] - tg[0] * len * 0.5, pos[1], pos[2] - tg[2] * len * 0.5);
            int head = Col.withA(hot, al);
            int body = Col.withA(Col.mix(c1, hot, 0.5), al * 0.6);
            int end = Col.withA(c1, 0.0);
            b.quad(tail, Prim.P(mid[0] + rad[0] * wd, mid[1], mid[2] + rad[2] * wd), pos, Prim.P(mid[0] - rad[0] * wd, mid[1], mid[2] - rad[2] * wd), end, body, head, body);
         }
      }
   }

   private void pop(Builder b, double cy, double sz, double t, double rot) {
      if (!(t <= 0.9)) {
         double pp = (t - 0.9) / 0.1;
         double a = 0.9 * Math.pow(1.0 - pp, 1.2) * Prim.sm(pp / 0.05);
         if (!(a < 0.01)) {
            b.push();
            b.translate(0.0, cy, 0.0);
            double[] c = b.viewLocal();
            double[][] bs = basis(c);
            double rp = (0.25 + 1.7 * (1.0 - Math.pow(1.0 - pp, 2.0))) * sz;
            double wd = 0.08 * sz * (1.0 - 0.5 * pp);
            int hot = Col.tint(b.c2, 0.85);
            int c1 = b.c1;
            billboardRing(b, bs, rp - wd, rp + wd, 24, (u, v) -> Col.withA(Col.mix(c1, hot, 1.0 - Math.abs(2.0 * v - 1.0)), a * (1.0 - 0.5 * Math.abs(2.0 * v - 1.0))));

            for (int k = 0; k < 8; k++) {
               int sd = this.p.seed() * 83 + k * 31;
               double az = Prim.rnd(sd + 1) * (Math.PI * 2);
               double el = Math.asin(2.0 * Prim.rnd(sd + 2) - 1.0);
               double[] d = new double[]{Math.cos(el) * Math.cos(az), Math.sin(el), Math.cos(el) * Math.sin(az)};
               double r0 = 0.3 * rp;
               double r1 = rp * (1.05 + 0.35 * Prim.rnd(sd + 3));
               Prim.spike(b, Prim.P(d[0] * r0, d[1] * r0, d[2] * r0), Prim.P(d[0] * r1, d[1] * r1, d[2] * r1), 0.05 * sz, 3, (u, v) -> Col.withA(Col.mix(hot, c1, v), 0.85 * a * (1.0 - 0.6 * v)));
            }

            b.pop();
         }
      }
   }
}
