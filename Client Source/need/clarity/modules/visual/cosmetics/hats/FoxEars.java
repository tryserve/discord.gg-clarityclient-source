package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class FoxEars implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private static final int TIP_BLACK = -15331057;
   private final HatParams p;

   public FoxEars() {
      this(HatParams.defaults());
   }

   public FoxEars(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         this.ear(b, t, side);
      }

      b.mirrorU(false);
   }

   private void ear(Builder b, double t, int side) {
      int c1 = b.c1;
      int c2 = b.c2;
      double swivel = b.animate ? Anim.pulse(t, 5.3 + side * 1.1, side * 0.55, 0.14) : 0.0;
      double sway = b.animate ? 0.04 * Math.sin(t * 0.9 + side * 2.3) : 0.0;
      b.push();
      b.translate(0.135, -0.012, -0.01);
      b.rotateZ(-0.14 + 0.34 * swivel + sway);
      b.rotateX(-0.1 * swivel);
      double[][] spine = Prim.bezier(6, Prim.P(0.0, 0.0, 0.0), Prim.P(0.03, 0.19, -0.02), Prim.P(0.085, 0.38, -0.055));
      int cream = Col.tint(c2, 0.55);
      Prim.Shade outer = (u, v) -> Col.shade(Col.mix(c1, -15331057, Prim.sm((v - 0.58) / 0.22)), 0.86 + 0.14 * Math.cos(u * (Math.PI * 2)));
      Prim.Shade inner = (u, v) -> Col.mix(Col.shade(cream, 0.85), Col.tint(cream, 0.4), Math.min(1.0, v * 1.3));
      EarKit.shell(b, spine, v -> 0.088 * Math.pow(1.0 - v, 0.85), v -> 0.024 * Math.pow(1.0 - v, 0.7), 8, outer, inner, 0.68, 0.5, 0.66);

      for (int k = 0; k < 3; k++) {
         int idx = 1 + k;
         double f = idx / 6.0;
         double[] c = spine[idx];

         for (int sgn = -1; sgn <= 1; sgn += 2) {
            double half = 0.088 * Math.pow(1.0 - f, 0.85) * 0.42;
            double[] base = Prim.add(c, sgn * half, 0.0, 0.018000000000000002);
            Prim.spike(b, base, Prim.add(base, -sgn * 0.006, 0.034, 0.008), 0.009, 3, Prim.byV(Col.shade(cream, 0.9), Col.tint(cream, 0.6)));
         }
      }

      b.pop();
   }
}
