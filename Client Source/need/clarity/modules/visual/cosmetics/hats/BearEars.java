package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class BearEars implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public BearEars() {
      this(HatParams.defaults());
   }

   public BearEars(HatParams p) {
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
      double tw = b.animate ? Anim.pulse(t, 4.7, side * 0.45, 0.1) : 0.0;
      b.push();
      b.translate(0.18, -0.01, -0.005);
      b.rotateZ(-0.35 + 0.22 * tw);
      b.scaleLocal(1.0 + 0.06 * tw);
      Prim.ellipsoid(b, 0.0, 0.024, 0.0, 0.125, 0.125, 0.06, 10, 5, (u, v) -> Col.shade(c1, 0.72 + 0.32 * v + 0.06 * Math.cos(u * (Math.PI * 2))));
      Prim.ellipsoid(b, 0.0, 0.02, 0.032, 0.076, 0.076, 0.046, 8, 4, (u, v) -> Col.mix(Col.dark(c2, 0.25), Col.tint(c2, 0.3), v));
      b.pop();
   }
}
