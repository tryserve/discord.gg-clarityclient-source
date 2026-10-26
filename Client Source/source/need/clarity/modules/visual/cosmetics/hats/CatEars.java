package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class CatEars implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public CatEars() {
      this(HatParams.defaults());
   }

   public CatEars(HatParams p) {
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
      double flick = b.animate ? Anim.pulse(t, 4.3, side * 0.37 + 0.1, 0.09) : 0.0;
      double sway = b.animate ? 0.03 * Math.sin(t * 1.1 + side * 1.7) : 0.0;
      b.push();
      b.translate(0.145, -0.012, 0.005);
      b.rotateZ(-0.16 + 0.3 * flick + sway);
      b.rotateX(0.06 * flick);
      double[][] spine = Prim.bezier(4, Prim.P(0.0, 0.0, 0.0), Prim.P(0.022, 0.12, 0.005), Prim.P(0.055, 0.26, -0.012));
      Prim.Shade outer = (u, v) -> Col.shade(Col.mix(c1, Col.dark(c1, 0.55), Prim.sm(v * 1.1)), 0.86 + 0.14 * Math.cos(u * (Math.PI * 2)));
      Prim.Shade inner = (u, v) -> Col.mix(Col.dark(c2, 0.25), Col.tint(c2, 0.3), Math.min(1.0, v * 1.2));
      EarKit.shell(b, spine, v -> 0.108 * Math.pow(1.0 - v, 0.9), v -> 0.03 * Math.pow(1.0 - v, 0.7), 8, outer, inner, 0.7, 0.5, 0.8);
      int fur = Col.tint(c2, 0.7);

      for (int k = -1; k <= 1; k++) {
         double[] base = Prim.P(0.016 * k, 0.012, 0.034);
         Prim.spike(b, base, Prim.add(base, 0.008 * k, 0.05 - 0.008 * Math.abs(k), 0.012), 0.008, 3, Prim.byV(Col.shade(fur, 0.8), fur));
      }

      b.pop();
   }
}
