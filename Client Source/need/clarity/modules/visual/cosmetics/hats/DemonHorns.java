package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class DemonHorns implements Cosmetic {
   static final int STEPS = 10;
   static final int SIDES = 6;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public DemonHorns() {
      this(HatParams.defaults());
   }

   public DemonHorns(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double glow = 0.5 + 0.5 * Math.sin(t * 2.2);

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         this.horn(b, t, glow, side);
      }

      b.mirrorU(false);
   }

   private void horn(Builder b, double t, double glow, int side) {
      int c1 = b.c1;
      int c2 = b.c2;
      double[][] spine = Prim.bezier(10, Prim.P(0.13, -0.03, 0.03), Prim.P(0.2, 0.13, 0.02), Prim.P(0.37, 0.27, 0.05), Prim.P(0.22, 0.45, 0.13));
      double[] tip = spine[10];
      Prim.Shade horn = (u, v) -> {
         int root = Col.dark(c1, 0.55);
         int body = Col.mix(root, c1, Prim.sm(v / 0.55));
         int hot = Col.mix(body, Col.tint(c2, 0.15 + 0.25 * glow), Prim.sm((v - 0.55) / 0.45));
         double ridge = 0.84 + 0.16 * Math.cos(v * (Math.PI * 2) * 5.0);
         return Col.shade(hot, ridge * (0.88 + 0.12 * Math.cos(u * (Math.PI * 2))));
      };
      Prim.tube(b, spine, v -> 0.056 * Math.pow(1.0 - v, 0.85) + 0.004, 6, horn, false, true);
      double[] d0 = Prim.tangent(spine, 0);
      double[] lo = Prim.add(spine[0], -d0[0] * 0.05, -d0[1] * 0.05, -d0[2] * 0.05);
      double[] hi = Prim.add(spine[0], d0[0] * 0.045, d0[1] * 0.045, d0[2] * 0.045);
      int collar = Col.dark(c1, 0.6);
      Prim.taperedTube(b, lo, hi, 0.076, 0.062, 8, (u, v) -> Col.shade(collar, 0.82 + 0.18 * Math.cos(u * (Math.PI * 2))), true);
      double pulse = 1.0 + 0.2 * glow;
      Prim.sphere(b, tip[0], tip[1] + 0.006, tip[2], 0.026 * pulse, 8, 4, (u, v) -> Col.withA(Col.tint(c2, 0.45), 0.5 + 0.3 * glow));

      for (int k = 0; k < 3; k++) {
         double ph = Anim.frac(t * 0.55 + k / 3.0 + side * 0.17);
         double a = ph * 5.0 + k * 2.0;
         double alpha = (1.0 - ph) * Math.min(1.0, ph * 6.0) * 0.9;
         Prim.sphere(b, tip[0] + 0.02 * Math.sin(a), tip[1] + 0.03 + ph * 0.2, tip[2] + 0.02 * Math.cos(a), 0.011 * (1.0 - 0.5 * ph), 4, 2, (u, v) -> Col.withA(Col.tint(c2, 0.55), alpha));
      }
   }
}
