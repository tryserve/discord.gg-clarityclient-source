package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Shockwave implements Cosmetic {
   private static final int STREAKS = 16;
   private static final int SPARKS = 12;
   private static final int FLASH_SEGS = 24;
   private final KillParams p;

   public static int maxQuads(int segs) {
      int s = Math.max(12, Math.min(128, segs));
      return 4 * s + 24 + 16 + 36;
   }

   public Shockwave() {
      this(KillParams.defaults());
   }

   public Shockwave(KillParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = this.p.t();
      double age = this.p.age();
      int segs = this.p.segs();
      double reach = this.p.reach();
      KillParams.Ground g = this.p.ground();
      double TAU = Math.PI * 2;
      double LIFT = 0.03;
      double k = KillKit.clamp(reach / 5.0, 0.6, 3.0);
      double R = reach * (1.0 - Math.pow(1.0 - t, 2.2));
      double fade = Math.pow(1.0 - t, 1.15) * Prim.sm(t / 0.03);
      int hot = Col.tint(b.c1, 0.55);
      int mid = b.c1;
      int cool = b.c2;
      double we = (0.07 + 0.1 * (1.0 - t)) * k;
      double rot = Prim.rnd(this.p.seed() * 977 + 3) * (Math.PI * 2);
      if (!(fade < 0.004) || !(t > 0.5)) {
         this.flash(b, g, t, k, R, hot, mid);
         if (!(R <= 0.04)) {
            double tw = Math.min(R, (0.9 + 0.7 * k) * (0.45 + 0.55 * (1.0 - t)));
            KillKit.ring(b, g, 0.03, R - tw, R, segs, (u, v) -> {
               double glint = 0.88 + 0.12 * Math.sin(u * (Math.PI * 2) * 9.0 + age * 18.0);
               return Col.withA(Col.mix(cool, mid, Prim.sm(v)), 0.6 * fade * Math.pow(v, 1.6) * glint);
            });
            double r2 = R * 0.66;
            if (r2 > 0.15) {
               double ew = we * 0.9;
               double ea = 0.5 * fade * Prim.sm(t / 0.14);
               KillKit.ring(b, g, 0.04, Math.max(0.0, r2 - ew), r2 + ew * 0.5, segs, (u, v) -> Col.withA(Col.tint(cool, 0.25), ea * Math.max(0.0, 1.0 - Math.abs(2.0 * v - 1.0))));
            }

            KillKit.ring(b, g, 0.05, Math.max(0.0, R - we * 1.6), R + we * 0.4, segs, (u, v) -> {
               double bell = Math.pow(Math.max(0.0, 1.0 - Math.abs(v - 0.75) / 0.75), 1.1);
               return Col.withA(Col.mix(mid, hot, Math.min(1.0, v * 1.2)), 0.95 * fade * bell);
            });
            this.wall(b, g, t, age, k, R, reach, segs, fade, mid, hot);
            if (R > 0.3) {
               this.streaks(b, g, t, k, R, rot, fade, hot, mid);
               this.sparks(b, g, t, k, R, fade, hot, mid);
            }
         }
      }
   }

   private void flash(Builder b, KillParams.Ground g, double t, double k, double R, int hot, int mid) {
      if (!(t >= 0.22)) {
         double tf = t / 0.22;
         double fr = 0.5 * k + Math.max(R, 0.3) * 0.35;
         double fa = 0.7 * (1.0 - Prim.sm(tf));
         KillKit.disc(b, g, 0.06, fr, 24, (u, v) -> Col.withA(Col.mix(hot, mid, v), fa * Math.pow(1.0 - v, 1.3)));
      }
   }

   private void wall(Builder b, KillParams.Ground g, double t, double age, double k, double R, double reach, int segs, double fade, int mid, int hot) {
      double hw = (0.3 + 0.16 * Math.min(reach, 12.0)) * Math.pow(1.0 - t, 0.9);
      if (!(hw < 0.02)) {
         double[] cam = b.viewLocal();
         double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
         double[][] lo = new double[segs + 1][];
         double[][] hi = new double[segs + 1][];
         double[] nf0 = new double[segs + 1];
         double[] nf1 = new double[segs + 1];

         for (int i = 0; i <= segs; i++) {
            double a = (double)(i % segs) / segs * (Math.PI * 2);
            double ragged = 0.85 + 0.15 * Math.sin(a * 7.0 + age * 10.0);
            double x = R * Math.cos(a);
            double z = R * Math.sin(a);
            double y0 = g.at(x, z) + 0.03;
            double rt = R + 0.12 * hw;
            lo[i] = Prim.P(x, y0, z);
            hi[i] = Prim.P(rt * Math.cos(a), y0 + hw * ragged, rt * Math.sin(a));
            nf0[i] = KillKit.near(cam, lo[i][0], lo[i][1], lo[i][2], scale);
            nf1[i] = KillKit.near(cam, hi[i][0], hi[i][1], hi[i][2], scale);
         }

         for (int i = 0; i < segs; i++) {
            int j = i + 1;
            double ba = 0.5 * fade;
            int b0 = Col.withA(mid, ba * nf0[i]);
            int b1 = Col.withA(mid, ba * nf0[j]);
            int t0 = Col.withA(hot, 0.0);
            int t1 = Col.withA(hot, 0.0);
            b.quad(lo[i], lo[j], hi[j], hi[i], b0, b1, t1, t0);
         }
      }
   }

   private void streaks(Builder b, KillParams.Ground g, double t, double k, double R, double rot, double fade, int hot, int mid) {
      for (int i = 0; i < 16; i++) {
         int sd = this.p.seed() * 131 + i * 7;
         double a = rot + (Math.PI * 2) * (i + 0.7 * (Prim.rnd(sd + 1) - 0.5)) / 16.0;
         double len = Math.min(R * 0.9, k * (0.5 + 1.4 * Prim.rnd(sd + 2)) * (0.35 + 0.65 * (1.0 - t)));
         double dw = 0.05 * k * (0.7 + 0.6 * Prim.rnd(sd + 3));
         double rb = R - len;
         double rt = R + 0.05 * k;
         double c = Math.cos(a);
         double s = Math.sin(a);
         double tx = -s;
         double tz = c;
         double[] l = KillKit.gp(g, 0.045, rb * c + tx * dw, rb * s + tz * dw);
         double[] r = KillKit.gp(g, 0.045, rb * c - tx * dw, rb * s - tz * dw);
         double[] tip = KillKit.gp(g, 0.045, rt * c, rt * s);
         int base = Col.withA(mid, 0.0);
         int tipc = Col.withA(hot, 0.85 * fade);
         b.tri(l, r, tip, base, base, tipc);
      }
   }

   private void sparks(Builder b, KillParams.Ground g, double t, double k, double R, double fade, int hot, int mid) {
      double lift = 1.0 - Math.pow(1.0 - t, 2.0);
      double rot = Prim.rnd(this.p.seed() * 313 + 9) * (Math.PI * 2);

      for (int i = 0; i < 12; i++) {
         int sd = this.p.seed() * 313 + i * 11;
         double a = rot + (Math.PI * 2) * Prim.rnd(sd + 1);
         double r = R * (0.55 + 0.45 * Prim.rnd(sd + 2));
         double x = r * Math.cos(a);
         double z = r * Math.sin(a);
         double rise = (0.15 + 0.9 * Prim.rnd(sd + 3)) * k * lift * (1.0 - 0.5 * t);
         double y0 = g.at(x, z) + 0.03 + rise;
         double len = 0.16 * k * (1.0 - t) + 0.05;
         Prim.spike(b, Prim.P(x, y0, z), Prim.P(x, y0 + len, z), 0.022 * k, 3, (u, v) -> Col.withA(Col.mix(hot, mid, v), 0.9 * fade * (1.0 - 0.7 * v)));
      }
   }
}
