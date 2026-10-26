package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class IceCrown implements Cosmetic {
   static final double R0 = 0.262;
   static final double R1 = 0.286;
   static final double BAND_H = 0.06;
   static final double HELMET_LIFT = 0.06;
   static final int SITES = 6;
   static final int BEADS = 12;
   static final int MOTES = 8;
   static final double[] HEIGHTS = new double[]{0.3, 0.2, 0.26, 0.17, 0.26, 0.2};
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public IceCrown() {
      this(HatParams.defaults());
   }

   public IceCrown(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int deep = Col.shade(Col.mix(b.c1, b.c2, 0.6), 0.72);
      int pale = Col.tint(b.c1, 0.55);
      int frost = Col.tint(b.c1, 0.82);
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.06 : 0.0, 0.0);
      Prim.prism(b, 0.0, 0.06, 0.286, 14, 0.0, (u, v) -> {
         double s = shimmer(u, t, 0.0);
         return Col.tint(Col.mix(deep, pale, 0.25 + 0.6 * v), 0.45 * s);
      }, false);
      Prim.prism(b, 0.046, 0.066, 0.29, 14, 0.0, (u, v) -> Col.tint(frost, 0.4 * shimmer(u, t, 0.0)), false);

      for (int k = 0; k < 12; k++) {
         double th = (Math.PI / 2) + (k + 0.5) * (Math.PI * 2) / 12.0;
         double[] c = GemKit.ring(0.289, th, 0.064);
         GemKit.pearl(b, c[0], c[1], c[2], 0.013 + 0.004 * Prim.rnd(k + 5), frost);
      }

      for (int k = 0; k < 6; k++) {
         double th = (Math.PI / 2) + k * (Math.PI * 2) / 6.0;
         double h = HEIGHTS[k];
         double co = Math.cos(th);
         double so = Math.sin(th);
         double tx = -so;
         double tz = co;
         double grow = 1.0 + 0.025 * Math.sin(t * 1.3 + k * 1.1);
         double tilt = k == 0 ? 0.1 : 0.16 + 0.34 * (1.0 - h / 0.3);
         double[] base = GemKit.ring(0.27199999999999996, th, 0.054);
         double[] dir = new double[]{co * Math.sin(tilt), Math.cos(tilt), so * Math.sin(tilt)};
         crystal(b, base, dir, h * grow, 0.034 + 0.05 * h, deep, pale, k / 6.0, t, k);
         double th2 = th + 0.27;
         double[] base2 = GemKit.ring(0.27399999999999997, th2, 0.054);
         double[] dir2 = Prim.unit(Prim.P(Math.cos(th2) * 0.55 + tx * 0.3, 0.82, Math.sin(th2) * 0.55 + tz * 0.3));
         crystal(b, base2, dir2, (0.42 * h + 0.03) * grow, 0.02 + 0.03 * h, deep, pale, (k + 0.4) / 6.0, t, k + 7);
         double[] tip = Prim.add(base, dir[0] * h * grow, dir[1] * h * grow + 0.01, dir[2] * h * grow);
         GemKit.glint(b, tip, k == 0 ? 0.1 : 0.075, Col.tint(b.c1, 0.85), GemKit.twinkle(b, 3.3, k * 0.23));
      }

      for (int k = 0; k < 8; k++) {
         double age = frac(t * (0.1 + 0.06 * Prim.rnd(k + 71)) + Prim.rnd(k * 3 + 1));
         double ang = k * (Math.PI * 2) / 8.0 + t * 0.4 * (k % 2 == 0 ? 1 : -1) + 2.0 * age;
         double rad = 0.3 + 0.09 * Prim.rnd(k * 5 + 9) + 0.02 * Math.sin(age * (Math.PI * 2) * 2.0);
         double y = 0.36 - age * 0.4;
         double fade = Prim.sm(age * 6.0) * Prim.sm((1.0 - age) * 6.0);
         GemKit.spark(b, GemKit.ring(rad, ang, y), 0.01, Col.withA(Col.tint(b.c1, 0.9), 0.85 * fade));
      }

      b.pop();
   }

   private static double frac(double x) {
      return x - Math.floor(x);
   }

   private static double shimmer(double g, double t, double offset) {
      double d = frac(g - t * 0.16 - offset + 0.5) - 0.5;
      return Math.exp(-(d / 0.07) * (d / 0.07));
   }

   private static void crystal(Builder b, double[] base, double[] dir, double len, double r, int deep, int pale, double g, double t, int seed) {
      double[] a = Prim.unit(dir);
      double[] ref = Math.abs(a[1]) < 0.9 ? Prim.P(0.0, 1.0, 0.0) : Prim.P(1.0, 0.0, 0.0);
      double[] e1 = Prim.unit(Prim.cross(a, ref));
      double[] e2 = Prim.cross(a, e1);
      double rot = Prim.rnd(seed * 13 + 2) * (Math.PI * 2) / 6.0;
      double[] sh = Prim.add(base, a[0] * len * 0.62, a[1] * len * 0.62, a[2] * len * 0.62);
      double[] tip = Prim.add(base, a[0] * len, a[1] * len, a[2] * len);
      double rs = r * 0.96;

      for (int i = 0; i < 6; i++) {
         double a0 = rot + i * (Math.PI * 2) / 6.0;
         double a1 = rot + (i + 1) * (Math.PI * 2) / 6.0;
         double am = (a0 + a1) * 0.5;
         double[] lo0 = ringPt(base, e1, e2, r, a0);
         double[] lo1 = ringPt(base, e1, e2, r, a1);
         double[] hi0 = ringPt(sh, e1, e2, rs, a0);
         double[] hi1 = ringPt(sh, e1, e2, rs, a1);
         double[] nrm = Prim.add(Prim.P(0.0, 0.0, 0.0), e1[0] * Math.cos(am) + e2[0] * Math.sin(am), e1[1] * Math.cos(am) + e2[1] * Math.sin(am), e1[2] * Math.cos(am) + e2[2] * Math.sin(am));
         double[] mid = new double[]{(lo0[0] + hi1[0]) / 2.0, (lo0[1] + hi1[1]) / 2.0, (lo0[2] + hi1[2]) / 2.0};
         double lit = 0.55 + 0.6 * b.facing(mid, nrm);
         double sh0 = shimmer(g + i * 0.012, t, 0.0);
         int cLo = Col.tint(Col.shade(deep, lit), 0.3 * sh0);
         int cHi = Col.tint(Col.shade(pale, lit), 0.55 * sh0);
         b.quad(lo0, lo1, hi1, hi0, Col.withA(cLo, 0.9), Col.withA(cLo, 0.9), Col.withA(cHi, 0.8), Col.withA(cHi, 0.8));
         double[] rn = Prim.cross(Prim.sub(hi1, hi0), Prim.sub(tip, hi0));
         double[] rc = new double[]{(hi0[0] + hi1[0] + tip[0]) / 3.0, (hi0[1] + hi1[1] + tip[1]) / 3.0, (hi0[2] + hi1[2] + tip[2]) / 3.0};
         double rl = 0.55 + 0.65 * b.facing(rc, rn);
         double flash = Col.clamp01(0.75 * shimmer(g + i * 0.012, t, 0.0) + 0.5 * Prim.sm((b.facing(rc, rn) - 0.7) / 0.3) * (0.5 + 0.5 * Math.sin(t * 2.7 + seed * 2.1 + i * 1.9)));
         int rHi = Col.tint(Col.shade(pale, rl), 0.2 + 0.65 * flash);
         b.tri(hi0, hi1, tip, Col.withA(rHi, 0.78), Col.withA(rHi, 0.78), Col.withA(Col.tint(rHi, 0.5), 0.5));
      }
   }

   private static double[] ringPt(double[] c, double[] e1, double[] e2, double r, double ang) {
      double ca = Math.cos(ang) * r;
      double sa = Math.sin(ang) * r;
      return Prim.P(c[0] + e1[0] * ca + e2[0] * sa, c[1] + e1[1] * ca + e2[1] * sa, c[2] + e1[2] * ca + e2[2] * sa);
   }
}
