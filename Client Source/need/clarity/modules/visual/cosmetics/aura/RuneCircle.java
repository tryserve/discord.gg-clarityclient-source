package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class RuneCircle implements Cosmetic {
   public static final int QUADS = 446;
   private static final double Y0 = 0.03;
   private static final double DY = 0.003;
   private static final int GLYPHS = 14;
   private static final int TICKS = 36;
   private static final int MOTES = 14;
   private static final double SHOCK_PERIOD = 3.4;
   private static final double[][][] SHAPES = new double[][][]{
      {{0.0, -1.0, 0.0, 1.0}, {0.0, 1.0, 0.7, 0.4}, {0.0, 0.2, 0.7, -0.2}},
      {{-0.6, -1.0, 0.0, 1.0}, {0.0, 1.0, 0.6, -1.0}, {-0.35, -0.15, 0.35, -0.15}},
      {{-0.6, 1.0, 0.6, 1.0}, {0.0, 1.0, 0.0, -1.0}, {-0.6, -1.0, 0.6, -1.0}},
      {{-0.6, -1.0, 0.6, 1.0}, {0.6, -1.0, -0.6, 1.0}},
      {{0.0, 1.0, 0.6, 0.0}, {0.6, 0.0, 0.0, -1.0}, {0.0, -1.0, -0.6, 0.0}, {-0.6, 0.0, 0.0, 1.0}},
      {{-0.5, 1.0, -0.5, -1.0}, {-0.5, 0.0, 0.5, 1.0}, {-0.5, 0.0, 0.5, -1.0}},
      {{-0.6, -0.6, 0.6, -0.6}, {0.0, -0.6, 0.0, 1.0}, {-0.4, 0.4, 0.4, 0.4}},
      {{-0.6, 1.0, 0.6, 0.2}, {0.6, 0.2, -0.6, -0.2}, {-0.6, -0.2, 0.6, -1.0}}
   };
   private final CircleParams p;

   public RuneCircle() {
      this(CircleParams.defaults());
   }

   public RuneCircle(CircleParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double R = this.p.radius();
      double t = b.animate ? b.time : 0.0;
      double turnBand = this.p.phase() * 0.35 * (Math.PI * 2);
      double turnStar = -this.p.phase() * 0.6 * (Math.PI * 2) + (Math.PI / 6);
      double sweep = b.animate ? t * 0.3 : 0.15;
      double breathe = AuraKit.wave(b, 1.6, 0.0);
      int lineA = Col.tint(b.c1, 0.25);
      int lineB = b.c2;
      int hot = Col.tint(b.c2, 0.6);
      pool(b, R, breathe);
      rings(b, R, sweep, lineA, lineB, hot);
      ticks(b, R, turnBand, lineB);
      this.glyphBand(b, R, turnBand, t, hot);
      this.star(b, R, turnStar, t, lineA, lineB, hot);
      if (b.animate) {
         shock(b, R, t, lineA);
      } else {
         shockAt(b, R, 0.5, lineA);
      }

      this.pillars(b, R, turnStar, lineA, hot);
      this.motes(b, R, t, hot);
   }

   private static void pool(Builder b, double R, double breathe) {
      double peak = 0.2 + 0.07 * breathe;
      Prim.disc(b, 0.03, R * 1.03, 36, (u, v) -> Col.withA(Col.mix(b.c2, b.c1, v), peak * Math.pow(1.0 - v, 1.3)));
   }

   private static Prim.Shade gleam(int base, int hot, double sweep, double lo, double hi) {
      return (u, v) -> {
         double g = Math.pow(1.0 - AuraKit.frac(u - sweep), 4.0);
         return Col.withA(Col.mix(base, hot, g), lo + (hi - lo) * g);
      };
   }

   private static void rings(Builder b, double R, double sweep, int lineA, int lineB, int hot) {
      Prim.ring(b, 0.033, R * 0.955, R * 1.0, 48, gleam(lineA, hot, sweep, 0.55, 1.0));
      Prim.ring(b, 0.033, R * 0.87, R * 0.885, 48, gleam(lineB, hot, -sweep * 0.7, 0.45, 0.85));
      Prim.ring(b, 0.033, R * 0.7, R * 0.715, 40, gleam(lineB, hot, sweep * 0.5, 0.45, 0.85));
   }

   private static void ticks(Builder b, double R, double turn, int line) {
      int dim = Col.withA(line, 0.55);
      int lit = Col.withA(Col.tint(line, 0.3), 0.85);

      for (int k = 0; k < 36; k++) {
         double a = turn + (Math.PI * 2) * k / 36.0;
         boolean major = k % 3 == 0;
         double r0 = R * (major ? 0.895 : 0.915);
         double r1 = R * (major ? 0.945 : 0.935);
         double c = Math.cos(a);
         double s = Math.sin(a);
         AuraKit.flatBar(b, r0 * c, r0 * s, r1 * c, r1 * s, R * (major ? 0.014 : 0.01), 0.036, major ? lit : dim, major ? lit : dim);
      }
   }

   private void glyphBand(Builder b, double R, double turn, double t, int hot) {
      double rMid = R * 0.7925;
      double gh = R * 0.052;
      double gw = R * 0.045;
      double width = R * 0.012;
      double y = 0.036;

      for (int k = 0; k < 14; k++) {
         double a = turn + (Math.PI * 2) * k / 14.0;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         double pulse = b.animate ? 0.5 + 0.5 * Math.sin(t * 2.0 + k * 0.9) : 0.5;
         int lo = Col.withA(Col.mix(b.c2, b.c1, 0.3), 0.85);
         int hi = Col.withA(Col.mix(b.c2, hot, 0.25 + 0.6 * pulse), 1.0);

         for (double[] s : SHAPES[k % SHAPES.length]) {
            double ax = rMid * ca + -sa * s[0] * gw + ca * s[1] * gh;
            double az = rMid * sa + ca * s[0] * gw + sa * s[1] * gh;
            double bx = rMid * ca + -sa * s[2] * gw + ca * s[3] * gh;
            double bz = rMid * sa + ca * s[2] * gw + sa * s[3] * gh;
            AuraKit.flatBar(b, ax, az, bx, bz, width, y, lo, hi);
         }

         double ad = a + (Math.PI / 14);
         double cd = Math.cos(ad);
         double sd = Math.sin(ad);
         AuraKit.flatBar(b, R * 0.73 * cd, R * 0.73 * sd, R * 0.855 * cd, R * 0.855 * sd, R * 0.006, y, Col.withA(b.c2, 0.45), Col.withA(b.c2, 0.45));
      }
   }

   private void star(Builder b, double R, double turn, double t, int lineA, int lineB, int hot) {
      double r = R * 0.61;
      double y = 0.036;
      double w = R * 0.013;
      double[][] v = new double[6][];

      for (int i = 0; i < 6; i++) {
         v[i] = new double[]{r * Math.cos(turn + i * Math.PI / 3.0), r * Math.sin(turn + i * Math.PI / 3.0)};
      }

      int ca = Col.withA(lineA, 0.95);
      int cb = Col.withA(lineB, 0.95);

      for (int tri = 0; tri < 2; tri++) {
         for (int e = 0; e < 3; e++) {
            double[] p0 = v[tri + 2 * e];
            double[] p1 = v[tri + 2 * ((e + 1) % 3)];
            AuraKit.flatBar(b, p0[0], p0[1], p1[0], p1[1], w, y, ca, cb);
         }
      }

      double pulse = AuraKit.wave(b, 2.4, 0.0);

      for (int i = 0; i < 6; i++) {
         AuraKit.fan(b, v[i][0], v[i][1], R * (0.03 + 0.006 * pulse), y + 0.003, 6, turn, Col.withA(-1, 0.95), Col.withA(hot, 0.85));
      }

      Prim.ring(b, y, R * 0.338, R * 0.354, 32, Prim.byUloop(Col.withA(lineB, 0.9), Col.withA(hot, 0.9)));
      Prim.ring(b, y, R * 0.09, R * 0.108, 20, Prim.solid(Col.withA(hot, 0.95)));
      AuraKit.fan(b, 0.0, 0.0, R * (0.045 + 0.012 * pulse), y + 0.003, 8, 0.0, Col.withA(-1, 0.95), Col.withA(hot, 0.8));
   }

   private static void shock(Builder b, double R, double t, int line) {
      shockAt(b, R, AuraKit.frac(t / 3.4), line);
   }

   private static void shockAt(Builder b, double R, double f, int line) {
      double r = R * (0.12 + 0.86 * Prim.sm(f));
      double a = 0.65 * Math.pow(1.0 - f, 1.5);
      if (!(a < 0.02)) {
         Prim.ring(b, 0.039, r - R * 0.012, r + R * 0.012, 40, Prim.solid(Col.withA(Col.tint(line, 0.4), a)));
      }
   }

   private void pillars(Builder b, double R, double turn, int lineA, int hot) {
      double r = R * 0.61;

      for (int i = 0; i < 6; i++) {
         double a = turn + i * Math.PI / 3.0;
         double wv = AuraKit.wave(b, 2.0, i * 1.1);
         double h = 0.32 + 0.26 * wv;
         AuraKit.streak(b, r * Math.cos(a), 0.03, r * Math.sin(a), h, 0.075, Col.withA(Col.mix(lineA, hot, 0.5), 0.55), Col.withA(lineA, 0.0));
      }
   }

   private void motes(Builder b, double R, double t, int hot) {
      double[] cam = b.viewLocal();

      for (int k = 0; k < 14; k++) {
         double a = (Math.PI * 2) * Prim.rnd(k * 3 + 1);
         double rr = R * (0.2 + 0.65 * Prim.rnd(k * 3 + 2));
         double period = 2.4 + 2.0 * Prim.rnd(k * 3 + 3);
         double u = b.animate ? AuraKit.frac(t / period + Prim.rnd(k * 7 + 5)) : Prim.rnd(k * 7 + 5);
         double x = rr * Math.cos(a);
         double z = rr * Math.sin(a);
         double y = 0.03 + u * 1.25;
         double env = Math.sin(Math.PI * u);
         double f = AuraKit.near(cam, x, y, z, b.scale());
         double al = 0.85 * env * f;
         if (!(al < 0.03)) {
            double len = 0.16 + 0.12 * Prim.rnd(k * 5 + 9);
            AuraKit.streak(b, x, y, z, len, 0.024, Col.withA(Col.tint(hot, 0.4), al), Col.withA(b.c2, 0.0));
         }
      }
   }
}
