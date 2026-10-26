package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class RoyalCrown implements Cosmetic {
   static final double R0 = 0.255;
   static final double R1 = 0.298;
   static final double BAND_H = 0.125;
   static final double APEX_Y = 0.285;
   static final double HELMET_LIFT = 0.06;
   static final int SEG = 24;
   static final int LEG_STEPS = 6;
   static final int LEG_SIDES = 5;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public RoyalCrown() {
      this(HatParams.defaults());
   }

   public RoyalCrown(HatParams p) {
      this.p = p;
   }

   private static double bandR(double y) {
      return 0.255 + 0.04299999999999998 * y / 0.125;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int gold = b.c1;
      int accent = b.c2;
      int velvet = Col.dark(accent, 0.5);
      int gemAlt = Col.tint(gold, 0.55);
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.06 : 0.0, 0.0);
      Prim.cylinder(b, 0.0, 0.125, 0.255, 0.298, 24, (u, v) -> GemKit.metal(gold, u, 0.3 + 0.45 * v, t, 0.12), false, false);
      Prim.Shade rim = (u, v) -> GemKit.metal(Col.tint(gold, 0.18), u, 0.75 + 0.25 * v, t, 0.12);
      Prim.cylinder(b, 0.0, 0.026, 0.259, bandR(0.026) + 0.004, 24, rim, false, false);
      Prim.cylinder(b, 0.099, 0.125, bandR(0.099) + 0.004, 0.302, 24, rim, false, false);

      for (int k = 0; k < 4; k++) {
         double th = (Math.PI / 2) + k * Math.PI / 2.0;
         boolean front = k == 0;
         double y = 0.064;
         double r = bandR(y) + 0.002;
         GemKit.mountOnWall(b, r, th, y);
         Prim.ring(b, 0.004, (front ? 0.044 : 0.034) * 0.98, (front ? 0.044 : 0.034) * 1.32, 8, (u, v) -> GemKit.metal(Col.tint(gold, 0.25), u, 0.9, t, 0.12));
         GemKit.gem(b, front ? 0.044 : 0.034, front ? 0.03 : 0.022, front ? 0.014 : 0.01, 6, accent, gemAlt, k * 1.7);
         GemKit.glint(b, Prim.P(0.0, front ? 0.03 : 0.022, 0.0), front ? 0.075 : 0.055, Col.tint(accent, 0.6), GemKit.twinkle(b, 2.9, k * 0.27));
         b.pop();
         double fr = 0.29;
         double[] base = Prim.P(fr * Math.cos(th), 0.121, fr * Math.sin(th));
         double[] tip = Prim.add(base, 0.02 * Math.cos(th), 0.078, 0.02 * Math.sin(th));
         double gph = k / 4.0;
         Prim.spike(b, base, tip, 0.03, 4, (u, v) -> GemKit.metal(gold, gph + u * 0.1, 0.4 + 0.55 * v, t, 0.12));
         GemKit.pearl(b, tip[0], tip[1] + 0.008, tip[2], 0.015, accent);
      }

      dome(b, 0.12, 0.278, 0.135, 16, 4, (u, v) -> {
         int c = Col.mix(Col.dark(velvet, 0.45), velvet, Prim.sm(v * 1.2));
         return Col.shade(c, 0.9 + 0.1 * Math.cos(u * (Math.PI * 2) * 2.0) + 0.18 * v * v);
      });

      for (int k = 0; k < 4; k++) {
         double th = (Math.PI / 4) + k * Math.PI / 2.0;
         double ct = Math.cos(th);
         double st = Math.sin(th);
         double[][] spine = Prim.bezier(6, Prim.P(0.286 * ct, 0.119, 0.286 * st), Prim.P(0.292 * ct, 0.215, 0.292 * st), Prim.P(0.15 * ct, 0.285, 0.15 * st), Prim.P(0.0, 0.285, 0.0));
         int leg = k;
         Prim.tube(
            b, spine, v -> 0.0135 * (1.0 + 0.28 * Math.cos(v * (Math.PI * 2) * 4.0)), 5, (u, v) -> GemKit.metal(gold, (leg + v) / 4.0, 0.55 + 0.4 * Math.cos(u * (Math.PI * 2)), t, 0.12), false, false
         );
      }

      double oy = 0.31499999999999995;
      Prim.sphere(b, 0.0, oy, 0.0, 0.036, 8, 4, (u, v) -> GemKit.metal(gold, u, 0.4 + 0.6 * v, t, 0.12));
      GemKit.glint(b, Prim.P(0.012, oy + 0.022, 0.028), 0.07, Col.tint(gold, 0.7), GemKit.twinkle(b, 3.3, 0.5));
      double cy = oy + 0.032;
      int goldHi = Col.tint(gold, 0.15);
      Prim.box(b, -0.008, cy, -0.008, 0.008, cy + 0.085, 0.008, gold, goldHi);
      Prim.box(b, -0.034, cy + 0.048, -0.008, 0.034, cy + 0.064, 0.008, gold, goldHi);
      Prim.box(b, -0.008, cy + 0.048, -0.034, 0.008, cy + 0.064, 0.034, gold, goldHi);
      b.pop();
   }

   private static void dome(Builder b, double y0, double r, double h, int segs, int rings, Prim.Shade s) {
      for (int k = 0; k < rings; k++) {
         double v0 = (double)k / rings;
         double v1 = (double)(k + 1) / rings;
         double f0 = v0 * Math.PI / 2.0;
         double f1 = v1 * Math.PI / 2.0;
         double r0 = r * Math.cos(f0);
         double r1 = r * Math.cos(f1);
         double h0 = y0 + h * Math.sin(f0);
         double h1 = y0 + h * Math.sin(f1);

         for (int i = 0; i < segs; i++) {
            double t0 = (double)i / segs;
            double t1 = (double)(i + 1) / segs;
            double c0 = Math.cos(t0 * (Math.PI * 2));
            double s0 = Math.sin(t0 * (Math.PI * 2));
            double c1 = Math.cos(t1 * (Math.PI * 2));
            double s1 = Math.sin(t1 * (Math.PI * 2));
            b.quad(Prim.P(r0 * c0, h0, r0 * s0), Prim.P(r0 * c1, h0, r0 * s1), Prim.P(r1 * c1, h1, r1 * s1), Prim.P(r1 * c0, h1, r1 * s0), s.at(t0, v0), s.at(t1, v0), s.at(t1, v1), s.at(t0, v1));
         }
      }
   }
}
