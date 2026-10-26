package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Shatter implements Cosmetic {
   public static final int MAX_CUBES = 72;
   public static final int SHARDS = 30;
   public static final int CRACKS = 7;
   public static final int CRACK_SEGS = 4;
   public static final int POOL_SEGS = 20;
   public static final int FLASH_LON = 8;
   public static final int FLASH_LAT = 4;
   public static final int QUADS = 542;
   static final double NOMINAL = 2.2;
   private static final double HOLD = 0.1;
   private static final double BOUNCE = 0.36;
   private static final double DRAG = 1.8;
   private static final double SHARD_DRAG = 1.2;
   private final KillParams p;

   public Shatter() {
      this(KillParams.defaults());
   }

   public Shatter(KillParams p) {
      this.p = p;
   }

   public static int[] grid(double w, double h) {
      double cell = Math.cbrt(w * w * h / 64.0);

      for (int it = 0; it < 60; it++) {
         int nx = Math.max(2, (int)Math.round(w / cell));
         int ny = Math.max(2, (int)Math.round(h / cell));
         if (nx * nx * ny <= 72) {
            return new int[]{nx, ny};
         }

         cell *= 1.07;
      }

      return new int[]{2, 2};
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      double life = this.p.life();
      double w = this.p.width();
      double h = this.p.height();
      if (!(t >= 1.0)) {
         double vis = Prim.sm(age / 0.03) * (1.0 - Prim.sm((t - 0.58) / 0.42));
         if (!(vis < 0.004)) {
            double warp = KillKit.clamp(2.2 / life, 0.5, 2.5);
            double tau = age * warp;
            double tb = Math.max(0.0, tau - 0.1);
            double kv = KillKit.clamp(Math.pow(h / 1.8, 0.45), 0.6, 2.2);
            double g = 13.0 * kv;
            KillParams.Ground gr = this.p.ground();
            double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
            double[] cam = b.viewLocal();
            double rot = Prim.rnd(this.p.seed() * 41 + 7) * (Math.PI * 2);
            double heat = tau < 0.1 ? Prim.sm(tau / 0.1) : 1.0 - Prim.sm((tau - 0.1) / 0.35);
            this.pool(b, gr, kv, tb, vis);
            this.cracks(b, gr, kv, t, tb, rot);
            this.cubes(b, cam, gr, w, h, kv, g, scale, age, tau, tb, t, vis, heat);
            this.shards(b, gr, kv, g, tb, vis);
            this.flash(b, w, h, tb);
         }
      }
   }

   private void pool(Builder b, KillParams.Ground gr, double kv, double tb, double vis) {
      double a = 0.45 * Prim.sm(tb / 0.02) * (1.0 - Prim.sm(tb / 0.7)) * vis;
      if (!(tb <= 0.0) && !(a < 0.01)) {
         int hot = Col.tint(b.c2, 0.6);
         int c1 = b.c1;
         KillKit.disc(b, gr, 0.03, 0.55 + 0.8 * kv, 20, (u, v) -> Col.withA(Col.mix(hot, c1, v), a * Math.pow(1.0 - v, 1.3)));
      }
   }

   private void cracks(Builder b, KillParams.Ground gr, double kv, double t, double tb, double rot) {
      if (!(tb <= 0.0)) {
         double grow = 1.0 - Math.pow(1.0 - Math.min(1.0, tb / 0.28), 3.0);
         double a = 0.9 * Prim.sm(tb / 0.02) * (1.0 - Prim.sm((t - 0.5) / 0.5));
         if (!(a < 0.01)) {
            int hot = Col.tint(b.c2, 0.6);
            int c1 = b.c1;
            double cool = Prim.sm(t / 0.5);

            for (int c = 0; c < 7; c++) {
               int sd = this.p.seed() * 89 + c * 23;
               double ang = rot + (Math.PI * 2) * (c + 0.35 * (Prim.rnd(sd + 1) - 0.5)) / 7.0;
               double len = (0.9 + 1.4 * Prim.rnd(sd + 2)) * kv * grow;
               double ca = Math.cos(ang);
               double sa = Math.sin(ang);
               double[] xs = new double[5];
               double[] zs = new double[5];
               double[] ws = new double[5];

               for (int i = 0; i <= 4; i++) {
                  double d = len * i / 4.0;
                  double lat = i == 0 ? 0.0 : (2.0 * Prim.rnd(sd + 10 + i) - 1.0) * 0.16 * len;
                  xs[i] = d * ca - lat * sa;
                  zs[i] = d * sa + lat * ca;
                  ws[i] = 0.11 * kv * (1.0 - 0.8 * i / 4.0) * (0.6 + 0.4 * (1.0 - t));
               }

               KillKit.groundStrip(b, gr, 0.04, xs, zs, ws, (u, v) -> Col.withA(Col.mix(Col.mix(hot, c1, cool), Col.mix(c1, Col.dark(c1, 0.3), cool), v), a * (1.0 - 0.75 * v)));
            }
         }
      }
   }

   private static double contact(double b0, double vy, double g) {
      return (vy + Math.sqrt(vy * vy + 2.0 * g * Math.max(0.0, b0))) / g;
   }

   private static double arc(double tau, double b0, double vy, double g) {
      double t1 = contact(b0, vy, g);
      if (tau <= t1) {
         return Math.max(0.0, b0 + vy * tau - 0.5 * g * tau * tau);
      }

      double v = -(vy - g * t1) * 0.36;
      double rest = tau - t1;

      for (int k = 0; k < 4; k++) {
         if (v < 0.5) {
            return 0.0;
         }

         double d = 2.0 * v / g;
         if (rest < d) {
            return Math.max(0.0, v * rest - 0.5 * g * rest * rest);
         }

         rest -= d;
         v *= 0.36;
      }

      return 0.0;
   }

   private void cubes(Builder b, double[] cam, KillParams.Ground gr, double w, double h, double kv, double g, double scale, double age, double tau, double tb, double t, double vis, double heat) {
      int[] gd = grid(w, h);
      int nx = gd[0];
      int ny = gd[1];
      double cx = w / nx;
      double cy = h / ny;
      double s0 = 0.47 * Math.min(cx, cy);
      double s = s0 * (1.0 - 0.55 * Prim.sm((t - 0.7) / 0.3));
      double HALF_PI = Math.PI / 2;
      double shiver = tb <= 0.0 ? 0.06 * s0 * Prim.sm(tau / 0.1) : 0.0;

      for (int j = 0; j < ny; j++) {
         for (int k = 0; k < nx; k++) {
            for (int i = 0; i < nx; i++) {
               int n = (j * nx + k) * nx + i;
               int sd = this.p.seed() * 131 + n * 17;
               double ox = -0.5 * w + (i + 0.5) * cx;
               double oy = (j + 0.5) * cy;
               double oz = -0.5 * w + (k + 0.5) * cx;
               double dl = Math.hypot(ox, oz);
               double dy = oy - 0.5 * h;
               double ang = dl > 1.0E-4 ? Math.atan2(oz, ox) + (Prim.rnd(sd + 1) - 0.5) * 0.9 : Prim.rnd(sd + 1) * (Math.PI * 2);
               double hs = (1.3 + 2.4 * Prim.rnd(sd + 2)) * kv * (0.55 + 0.45 * Math.min(1.0, dl / (0.5 * w + 1.0E-6)));
               double vy = (2.6 + 3.4 * Prim.rnd(sd + 3) + 2.0 * dy / h) * kv;
               double vx = hs * Math.cos(ang);
               double vz = hs * Math.sin(ang);
               double ex = (1.0 - Math.exp(-1.8 * tb)) / 1.8;
               double px = ox + vx * ex;
               double pz = oz + vz * ex;
               double b0 = oy - s0;
               double t1 = contact(b0, vy, g);
               double e1 = (1.0 - Math.exp(-1.8 * t1)) / 1.8;
               double fl = gr.at(ox + vx * e1, oz + vz * e1);
               double b0r = Math.max(0.0, b0 - fl);
               double bottom;
               if (tb <= 0.0) {
                  bottom = b0;
               } else {
                  bottom = fl + arc(tb, b0r, vy, g);
                  bottom = Math.max(bottom, gr.at(px, pz));
               }

               double T = contact(b0r, vy, g) + 0.5;
               double x = Math.min(1.0, tb / T);
               double f = 1.0 - Math.pow(1.0 - x, 3.0);
               double sgn = Prim.rnd(sd + 7) < 0.5 ? -1.0 : 1.0;
               double ax = sgn * (2 + (int)(Prim.rnd(sd + 4) * 4.0)) * (Math.PI / 2) * f;
               double az = -sgn * (1 + (int)(Prim.rnd(sd + 5) * 4.0)) * (Math.PI / 2) * f;
               double ay = (2.0 * Prim.rnd(sd + 6) - 1.0) * Math.PI * 1.5 * f;
               double hyf = Math.abs(Math.cos(ax) * Math.sin(az)) + Math.abs(Math.cos(ax) * Math.cos(az)) + Math.abs(Math.sin(ax));
               double py = bottom + s * hyf;
               double jx = shiver * Math.sin(age * 71.0 + n * 1.7);
               double jz = shiver * Math.cos(age * 63.0 + n * 2.3);
               double a = 0.85 * vis * KillKit.near(cam, px, py, pz, scale);
               if (!(a < 0.01)) {
                  double hf = (j + 0.5) / ny;
                  int base = Col.mix(b.c1, b.c2, hf);
                  double glint = Math.pow(Math.max(0.0, Math.sin(age * (5.0 + 5.0 * Prim.rnd(sd + 9)) + (Math.PI * 2) * Prim.rnd(sd + 10))), 12.0);
                  double hot = heat * 0.55 + glint * 0.5;
                  int cb = Col.withA(Col.tint(Col.shade(base, 0.82), hot * 0.6), a);
                  int ct = Col.withA(Col.tint(base, 0.3 + hot), a);
                  b.push();
                  b.translate(px + jx, py, pz + jz).rotateY(ay).rotateX(ax).rotateZ(az);
                  Prim.box(b, -s, -s, -s, s, s, s, cb, ct);
                  b.pop();
               }
            }
         }
      }
   }

   private void shards(Builder b, KillParams.Ground gr, double kv, double g, double tb, double vis) {
      if (!(tb <= 0.0)) {
         double a = vis * Prim.sm(tb / 0.02) * (1.0 - Prim.sm(tb / 1.2));
         if (!(a < 0.01)) {
            double w = this.p.width();
            double h = this.p.height();
            int tip = Col.withA(Col.tint(b.c2, 0.7), 0.95 * a);
            int root = Col.withA(b.c1, 0.55 * a);

            for (int m = 0; m < 30; m++) {
               int sd = this.p.seed() * 337 + m * 29;
               double sx = (Prim.rnd(sd + 1) - 0.5) * w * 0.8;
               double sy = h * (0.2 + 0.7 * Prim.rnd(sd + 2));
               double sz = (Prim.rnd(sd + 3) - 0.5) * w * 0.8;
               double ang = Prim.rnd(sd + 4) * (Math.PI * 2);
               double el = 0.15 + 0.75 * Prim.rnd(sd + 5);
               double sp = (2.5 + 3.0 * Prim.rnd(sd + 6)) * kv;
               double vx = sp * Math.cos(el) * Math.cos(ang);
               double vz = sp * Math.cos(el) * Math.sin(ang);
               double vy = sp * Math.sin(el) + 1.0 * kv;
               double ex = (1.0 - Math.exp(-1.2 * tb)) / 1.2;
               double x = sx + vx * ex;
               double z = sz + vz * ex;
               double y = Math.max(sy + vy * tb - 0.5 * g * tb * tb, gr.at(x, z) + 0.03);
               double L = (0.16 + 0.22 * Prim.rnd(sd + 7)) * kv;
               double ws = 0.4 * L;
               double a1 = Prim.rnd(sd + 8) * (Math.PI * 2) + (4.0 + 6.0 * Prim.rnd(sd + 9)) * tb;
               double a2 = Prim.rnd(sd + 11) * (Math.PI * 2) + (3.0 + 5.0 * Prim.rnd(sd + 12)) * tb;
               b.push();
               b.translate(x, y, z).rotateY(a1).rotateX(a2);
               b.tri(Prim.P(0.0, L, 0.0), Prim.P(-ws, -0.4 * L, 0.0), Prim.P(ws, -0.4 * L, 0.0), tip, root, root);
               b.pop();
            }
         }
      }
   }

   private void flash(Builder b, double w, double h, double tb) {
      if (!(tb <= 0.0) && !(tb >= 0.24)) {
         double x = tb / 0.24;
         double R = 0.4 * Math.max(0.55 * h, w) * (0.35 + 1.5 * (1.0 - Math.pow(1.0 - x, 2.0)));
         double a = 0.55 * Math.pow(1.0 - x, 1.5);
         double cyc = Math.max(0.5 * h, R + 0.03);
         int hot = Col.tint(b.c2, 0.8);
         int c1 = b.c1;
         Prim.sphere(b, 0.0, cyc, 0.0, R, 8, 4, (u, v) -> {
            double fi = (-Math.PI / 2) + Math.PI * v;
            double an = u * (Math.PI * 2);
            double[] n = new double[]{Math.cos(fi) * Math.cos(an), Math.sin(fi), Math.cos(fi) * Math.sin(an)};
            double f = b.facing(Prim.P(R * n[0], cyc + R * n[1], R * n[2]), n);
            return Col.withA(Col.mix(c1, hot, f), a * (0.25 + 0.75 * Math.pow(f, 1.5)));
         });
      }
   }
}
