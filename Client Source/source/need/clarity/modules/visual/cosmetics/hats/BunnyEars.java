package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class BunnyEars implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public BunnyEars() {
      this(HatParams.defaults());
   }

   public BunnyEars(HatParams p) {
      this.p = p;
   }

   private static double oval(double v) {
      return (0.7 + 0.3 * Prim.sm(v / 0.5)) * Math.sqrt(Math.max(0.0, 1.0 - Math.pow(v, 4.5)));
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
      double flop = b.animate ? Anim.pulse(t, 6.1, side * 0.5 + 0.2, 0.2) : 0.0;
      double sway = b.animate ? Math.sin(t * 1.2 + side * 1.9) * 0.012 : 0.0;
      b.push();
      b.translate(0.075, -0.012, -0.01);
      b.rotateZ(-0.1);
      double[][] spine = Prim.bezier(
         9, Prim.P(0.0, 0.0, 0.0), Prim.P(0.012 + sway, 0.2, -0.03), Prim.P(0.055 + sway + 0.05 * flop, 0.42, -0.05 - 0.02 * flop), Prim.P(0.115 + 0.03 * flop, 0.5 - 0.05 * flop, 0.0)
      );
      Prim.Shade outer = (u, v) -> Col.shade(Col.mix(c1, Col.tint(c1, 0.25), v), 0.86 + 0.14 * Math.cos(u * (Math.PI * 2)));
      Prim.Shade inner = (u, v) -> Col.mix(Col.dark(c2, 0.2), Col.tint(c2, 0.35), Prim.sm(v * 1.2));
      EarKit.shell(b, spine, v -> 0.07 * oval(v), v -> 0.018 * oval(v) + 0.004 * (1.0 - v), 8, outer, inner, 0.7, 0.5, 0.86);
      b.pop();
   }
}
