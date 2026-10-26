package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Ascend implements Cosmetic {
   public static final int NATURAL_1 = 16763213;
   public static final int NATURAL_2 = 16774876;
   static final int RINGS = 4;
   static final int RING_SEGS = 24;
   static final int HAZE_SEGS = 14;
   static final int COLUMN_SIDES = 12;
   static final int MOTES = 20;
   static final int FLOOR_SEGS = 24;
   static final int POOL_SEGS = 16;
   public static final int QUADS = 456;
   static final double[] START = new double[]{0.8, 0.64, 0.48, 0.32};
   static final double[] FINAL = new double[]{3.0, 2.83, 2.67, 2.5};
   private final KillParams p;

   public Ascend() {
      this(KillParams.defaults());
   }

   public Ascend(KillParams p) {
      this.p = p;
   }

   static double departs(int i) {
      return 0.12 + 0.07 * i;
   }

   public static double climb(int i, double t) {
      return KillKit.clamp((t - departs(i)) / (1.0 - departs(i)), 0.0, 1.0);
   }

   private static double rise(int i, double u) {
      return 1.0 - Math.pow(1.0 - u, 2.4 - 0.25 * i);
   }

   private static double formed(int i, double t) {
      return Prim.sm((t - 0.015 * i) / 0.09);
   }

   static double baseRadius(double w, double h) {
      return 0.5 * w + 0.15 * h + 0.1;
   }

   private double[] state(int i, double t, double u, double R0, double floor0) {
      double h = this.p.height();
      double e = rise(i, u);
      double y = floor0 + (START[i] + (FINAL[i] - START[i]) * e) * h;
      double f = formed(i, t);
      double grow = 1.0 - Math.pow(1.0 - f, 3.0);
      double R = R0 * (1.0 - 0.07 * i) * (0.3 + 0.7 * grow) * (1.0 + 0.1 * Math.sin(Math.PI * Math.min(1.0, f))) * (1.0 - 0.62 * Prim.sm((u - 0.25) / 0.75));
      double phi = this.precession(i, t);
      double drift = 0.1 * R0 * e;
      return new double[]{y, R, drift * Math.cos(phi + 1.0), drift * Math.sin(phi + 1.0)};
   }

   private double precession(int i, double t) {
      int sd = this.p.seed() * 47 + i * 13;
      double dir = Prim.rnd(sd + 2) < 0.5 ? -1.0 : 1.0;
      return Prim.rnd(sd + 1) * (Math.PI * 2) + dir * (0.8 + 0.6 * Prim.rnd(sd + 3)) * (Math.PI * 2) * t;
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      if (!(t >= 1.0)) {
         double w = this.p.width();
         double h = this.p.height();
         KillParams.Ground gr = this.p.ground();
         double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
         double[] cam = b.viewLocal();
         double R0 = baseRadius(w, h);
         double floor0 = gr.at(0.0, 0.0);
         int hot = Col.tint(b.c2, 0.55);
         int outer = b.c1;
         this.floor(b, gr, R0, t, hot, outer);
         double topY = floor0;

         for (int i = 0; i < 4; i++) {
            double u = climb(i, t);
            double f = formed(i, t);
            double a = f * (1.0 - Prim.sm((u - 0.45) / 0.55));
            double[] s = this.state(i, t, u, R0, floor0);
            if (i == 0 && a > 0.02) {
               topY = s[0];
            }

            if (!(a < 0.01) && !(s[1] < 0.001)) {
               int sd = this.p.seed() * 47 + i * 13;
               double sign = Prim.rnd(sd + 4) < 0.5 ? -1.0 : 1.0;
               double tilt = sign * (0.12 + 0.22 * Prim.rnd(sd + 5)) * Prim.sm(u / 0.5);
               double spin = Prim.rnd(sd + 6) * (Math.PI * 2) + (1.5 + Prim.rnd(sd + 7)) * (Math.PI * 2) * t;
               b.push();
               b.translate(s[2], s[0], s[3]).rotateY(this.precession(i, t)).rotateZ(tilt);
               this.ring(b, s[1], a, spin, scale, s[0] - floor0 - 0.03, hot, outer);
               b.pop();
            }
         }

         this.column(b, cam, scale, R0, floor0, topY, t, hot, outer);
         this.motes(b, cam, scale, R0, floor0, age, t, hot);
      }
   }

   private static double[][] basis(double[] c) {
      double[] f = c == null ? new double[]{0.0, 0.0, -1.0} : Prim.unit(c);
      double[] up = Math.abs(f[1]) > 0.97 ? new double[]{1.0, 0.0, 0.0} : new double[]{0.0, 1.0, 0.0};
      double[] r = Prim.unit(Prim.cross(up, f));
      return new double[][]{r, Prim.cross(f, r), f};
   }

   private void ring(Builder b, double R, double a, double spin, double scale, double clear, int hot, int outer) {
      double[] camL = b.viewLocal();
      double wc = 0.05 * R + 0.01;
      double gw = 0.22 * R + 0.03;
      double bh = 0.7 * wc + 0.012;
      double rIn = Math.max(0.0, R - wc);
      double rOut = R + wc;
      Prim.ring(b, 0.0, rOut, rOut + gw, 24, (u, v) -> {
         double r = rOut + v * gw;
         double an = u * (Math.PI * 2);
         double al = a * 0.34 * Math.pow(1.0 - v, 1.6) * KillKit.near(camL, r * Math.cos(an), 0.0, r * Math.sin(an), scale);
         return Col.withA(Col.mix(outer, hot, 0.25 * (1.0 - v)), al);
      });
      Prim.ring(b, 0.0, rIn, rOut, 24, (u, v) -> {
         double r = rIn + v * (rOut - rIn);
         double an = u * (Math.PI * 2);
         double sector = 0.62 + 0.38 * Math.pow(0.5 + 0.5 * Math.cos(an - spin), 1.5);
         double al = a * sector * (0.62 + 0.38 * (1.0 - v)) * KillKit.near(camL, r * Math.cos(an), 0.0, r * Math.sin(an), scale);
         return Col.withA(Col.mix(hot, outer, Prim.sm(v * 1.1)), al);
      });
      double rb = R + 0.4 * wc;
      Prim.cylinder(b, -bh, bh, rb, rb, 24, (u, v) -> {
         double an = u * (Math.PI * 2);
         double sector = 0.62 + 0.38 * Math.pow(0.5 + 0.5 * Math.cos(an - spin), 1.5);
         double al = a * 0.55 * sector * KillKit.near(camL, rb * Math.cos(an), 0.0, rb * Math.sin(an), scale);
         return Col.withA(Col.mix(hot, outer, 0.45), al);
      }, false, false);
      double edge = 1.0 - b.facing(Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, 1.0, 0.0));
      double ha = a * (0.07 + 0.2 * edge * edge) * KillKit.near(camL, 0.0, 0.0, 0.0, scale);
      if (ha > 0.01 && clear > 0.05) {
         double[][] bs = basis(camL);
         double rh = Math.min(1.05 * R, clear);
         double[][] rim = new double[15][];

         for (int i = 0; i <= 14; i++) {
            double an = i % 14 / 14.0 * (Math.PI * 2);
            double cs = Math.cos(an) * rh;
            double sn = Math.sin(an) * rh;
            rim[i] = Prim.P(bs[0][0] * cs + bs[1][0] * sn, bs[0][1] * cs + bs[1][1] * sn, bs[0][2] * cs + bs[1][2] * sn);
         }

         int c0 = Col.withA(hot, ha);
         int c1 = Col.withA(outer, 0.0);
         double[] ctr = Prim.P(0.0, 0.0, 0.0);

         for (int i = 0; i < 14; i++) {
            b.tri(ctr, rim[i], rim[i + 1], c0, c1, c1);
         }
      }
   }

   private void column(Builder b, double[] cam, double scale, double R0, double floor0, double topY, double t, int hot, int outer) {
      double a = 0.85 * Prim.sm(t / 0.06) * (1.0 - Prim.sm((t - 0.62) / 0.33)) * KillKit.near(cam, 0.0, 0.5 * this.p.height(), 0.0, scale);
      double y0 = floor0 + 0.03;
      double y1 = topY;
      if (!(a < 0.01) && !(y1 <= y0 + 0.1)) {
         int cb = Col.withA(Col.mix(hot, outer, 0.45), 0.2 * a);
         int ct = Col.withA(outer, 0.035 * a);
         Prim.cylinder(b, y0, y1, 0.7 * R0, 0.45 * R0, 12, (u, v) -> Col.mix(cb, ct, v), false, false);
      }
   }

   private void floor(Builder b, KillParams.Ground gr, double R0, double t, int hot, int outer) {
      double a = Prim.sm(t / 0.04) * (1.0 - Prim.sm((t - 0.3) / 0.35));
      if (!(a < 0.01)) {
         double sw = 1.0 - Math.pow(1.0 - Math.min(1.0, t / 0.4), 2.0);
         double r = R0 * (0.85 + 0.45 * sw);
         double wd = 0.05 * R0 + 0.02;
         KillKit.ring(b, gr, 0.04, Math.max(0.0, r - wd), r + wd, 24, (u, v) -> {
            double bell = Math.pow(Math.max(0.0, 1.0 - Math.abs(v - 0.5) / 0.5), 0.9);
            return Col.withA(Col.mix(hot, outer, v), 0.85 * a * bell);
         });
         KillKit.disc(b, gr, 0.03, 1.25 * R0, 16, (u, v) -> Col.withA(Col.mix(hot, outer, v), 0.34 * a * Math.pow(1.0 - v, 1.4)));
      }
   }

   private void motes(Builder b, double[] cam, double scale, double R0, double floor0, double age, double t, int hot) {
      double h = this.p.height();
      double ks = KillKit.clamp(Math.sqrt(R0 / 0.67), 0.6, 3.5);

      for (int k = 0; k < 20; k++) {
         int sd = this.p.seed() * 53 + k * 17;
         int ri = k % 4;
         double uk = climb(ri, t) - (0.03 + 0.3 * Prim.rnd(sd + 1));
         if (!(uk <= 0.0)) {
            double[] s = this.state(ri, t, uk, R0, floor0);
            double ang = Prim.rnd(sd + 2) * (Math.PI * 2) + (Prim.rnd(sd + 3) < 0.5 ? -1.0 : 1.0) * (0.4 + 0.4 * Prim.rnd(sd + 4)) * (Math.PI * 2) * t;
            double rad = (0.25 + 0.8 * Prim.rnd(sd + 5)) * s[1];
            double x = s[2] + rad * Math.cos(ang);
            double z = s[3] + rad * Math.sin(ang);
            double y = s[0] + (Prim.rnd(sd + 6) - 0.65) * 0.1 * h;
            double tw = Math.pow(0.5 + 0.5 * Math.sin(age * (5.0 + 6.0 * Prim.rnd(sd + 7)) + (Math.PI * 2) * Prim.rnd(sd + 8)), 2.0);
            double al = 0.95 * (0.25 + 0.75 * tw) * Prim.sm(uk / 0.1) * (1.0 - Prim.sm((uk - 0.6) / 0.4)) * formed(ri, t) * (1.0 - Prim.sm((t - 0.8) / 0.2)) * KillKit.near(cam, x, y, z, scale);
            if (!(al < 0.04)) {
               double sz = 0.045 * ks * (0.6 + 0.8 * Prim.rnd(sd + 9)) * (1.0 - 0.4 * uk);
               double sl = sz * 1.7;
               int c = Col.withA(Col.mix(hot, b.c1, 0.25 * Prim.rnd(sd + 10)), al);
               b.quad(Prim.P(x - sz, y, z), Prim.P(x, y - sl, z), Prim.P(x + sz, y, z), Prim.P(x, y + sl, z), c);
               b.quad(Prim.P(x, y, z - sz), Prim.P(x, y - sl, z), Prim.P(x, y, z + sz), Prim.P(x, y + sl, z), c);
               b.quad(Prim.P(x - sl, y, z), Prim.P(x, y, z - sz), Prim.P(x + sl, y, z), Prim.P(x, y, z + sz), Col.alpha(c, 0.6));
            }
         }
      }
   }
}
