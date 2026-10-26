package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class SpikedCrown implements Cosmetic {
   static final double R0 = 0.262;
   static final double R1 = 0.288;
   static final double BAND_H = 0.095;
   static final double HELMET_LIFT = 0.06;
   static final int SEG = 24;
   static final int MAIN = 9;
   static final double[] HEIGHTS = new double[]{0.27, 0.17, 0.21, 0.15, 0.19, 0.19, 0.15, 0.21, 0.17};
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public SpikedCrown() {
      this(HatParams.defaults());
   }

   public SpikedCrown(HatParams p) {
      this.p = p;
   }

   private static double bandR(double y) {
      return 0.262 + 0.025999999999999968 * y / 0.095;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int metal = b.c1;
      int accent = b.c2;
      int forged = Col.dark(metal, 0.22);
      int gemAlt = Col.tint(metal, 0.5);
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.06 : 0.0, 0.0);
      Prim.cylinder(b, 0.0, 0.095, 0.262, 0.288, 24, (u, v) -> GemKit.metal(forged, u, 0.25 + 0.6 * v, t, 0.1), false, false);
      Prim.Shade edge = (u, v) -> GemKit.metal(metal, u, 0.7 + 0.3 * v, t, 0.1);
      Prim.cylinder(b, 0.0, 0.018, 0.266, bandR(0.018) + 0.004, 24, edge, false, false);
      Prim.cylinder(b, 0.077, 0.095, bandR(0.077) + 0.004, 0.292, 24, edge, false, false);

      for (int k = 0; k < 18; k++) {
         double th = k * (Math.PI * 2) / 18.0;
         if (!nearStone(th)) {
            double y = 0.048;
            double r = bandR(y) + 0.002;
            double[] base = GemKit.ring(r, th, y);
            double[] tip = GemKit.ring(r + 0.012, th, y);
            double g = k / 18.0;
            Prim.spike(b, base, tip, 0.0105, 4, (u, v) -> GemKit.metal(metal, g, 0.55 + 0.4 * v, t, 0.1));
         }
      }

      double[] stoneAt = new double[]{Math.PI / 2, (Math.PI / 2) + Math.toRadians(40.0), (Math.PI / 2) - Math.toRadians(40.0)};

      for (int k = 0; k < 3; k++) {
         boolean big = k == 0;
         double rad = big ? 0.038 : 0.024;
         GemKit.mountOnWall(b, bandR(0.048) + 0.002, stoneAt[k], 0.048);
         Prim.ring(b, 0.004, rad * 0.98, rad * 1.34, 8, (u, v) -> GemKit.metal(metal, u, 0.85, t, 0.1));
         GemKit.gem(b, rad, big ? 0.026 : 0.017, big ? 0.012 : 0.008, 6, accent, gemAlt, k * 2.3);
         GemKit.glint(b, Prim.P(0.0, big ? 0.026 : 0.017, 0.0), big ? 0.07 : 0.05, Col.tint(accent, 0.6), GemKit.twinkle(b, 3.1, k * 0.31));
         b.pop();
      }

      for (int j = 0; j < 9; j++) {
         double th = (Math.PI / 2) + j * (Math.PI * 2) / 9.0;
         double h = HEIGHTS[j];
         double glow = 0.5 + 0.5 * Math.sin(t * 2.4 + j * 0.8);
         int hot = Col.tint(accent, 0.15 + 0.3 * glow);
         double[] base = GemKit.ring(0.27799999999999997, th, 0.089);
         double lean = 0.03 + 0.04 * h;
         double[] tip = Prim.add(base, lean * Math.cos(th), h, lean * Math.sin(th));
         int jj = j;
         Prim.spike(b, base, tip, 0.026 + 0.05 * h, 4, (u, v) -> Col.mix(GemKit.metal(metal, jj / 9.0, 0.35 + 0.4 * (1.0 - v), t, 0.1), hot, Prim.sm((v - 0.55) / 0.45)));
         if (h > 0.2) {
            GemKit.glint(b, Prim.add(tip, 0.0, 0.008, 0.0), 0.07, Col.tint(accent, 0.7), GemKit.twinkle(b, 3.7, j * 0.19));
         }
      }

      for (int j = 0; j < 9; j++) {
         double th = (Math.PI / 2) + (j + 0.5) * (Math.PI * 2) / 9.0;
         double[] base = GemKit.ring(0.27999999999999997, th, 0.089);
         double[] tip = Prim.add(base, 0.012 * Math.cos(th), 0.062, 0.012 * Math.sin(th));
         int jj = j;
         Prim.spike(b, base, tip, 0.022, 4, (u, v) -> GemKit.metal(metal, (jj + 0.5) / 9.0, 0.4 + 0.5 * (1.0 - v), t, 0.1));
      }

      b.pop();
   }

   private static boolean nearStone(double th) {
      double lim = Math.toRadians(12.0);

      for (double a : new double[]{Math.PI / 2, (Math.PI / 2) + Math.toRadians(40.0), (Math.PI / 2) - Math.toRadians(40.0)}) {
         double d = Math.abs(((th - a) % (Math.PI * 2) + (Math.PI * 2) + Math.PI) % (Math.PI * 2) - Math.PI);
         if (d < lim) {
            return true;
         }
      }

      return false;
   }
}
