package need.clarity.modules.visual.cosmetics.hats;

import java.util.ArrayList;
import java.util.List;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class ReindeerAntlers implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public ReindeerAntlers() {
      this(HatParams.defaults());
   }

   public ReindeerAntlers(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      Prim.BranchSpec sp = new Prim.BranchSpec();
      sp.maxDepth = 1;
      sp.tines = 3;
      sp.steps = 9;
      sp.childSteps = 4;
      sp.sides = 5;
      sp.taper = 0.32;
      sp.tineFrom = 0.38;
      sp.tineTo = 0.88;
      sp.spread = 0.7;
      sp.childLen = 0.36;
      sp.childRadius = 0.8;
      sp.bend = 1.7;
      sp.bendDir = new double[]{0.0, 0.1, 1.0};
      sp.sideRef = new double[]{0.0, 0.0, 1.0};
      sp.phases = new double[]{0.0, 0.35, -0.35};
      sp.jitter = 0.2;
      sp.seed = 3;
      List<double[]> tips = new ArrayList<>();
      sp.onTip = (pos, depth) -> tips.add(pos);
      Prim.BranchShade wood = (depth, u, v) -> {
         double g = (depth + v) / 2.0;
         int col = Col.mix(Col.dark(c1, 0.55), Col.tint(c1, 0.15), Prim.sm(g * 1.3));
         return Col.shade(col, 0.86 + 0.14 * Math.cos(u * (Math.PI * 2)));
      };

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         tips.clear();
         Prim.branch(b, sp, Prim.P(0.12, -0.02, -0.03), new double[]{0.4, 0.85, -0.5}, 0.62, 0.036, wood);
         double[][] palm = Prim.bezier(6, Prim.P(0.115, -0.01, 0.05), Prim.P(0.13, 0.04, 0.16), Prim.P(0.13, 0.1, 0.24));
         Prim.flatTube(
            b,
            palm,
            new double[]{0.0, 1.0, 0.0},
            v -> 0.017 + 0.032 * Prim.sm(v * 1.5),
            v -> 0.009,
            6,
            (u, v) -> Col.shade(Col.mix(Col.dark(c1, 0.45), Col.tint(c1, 0.1), Prim.sm(v * 1.2)), 0.85 + 0.15 * Math.cos(u * (Math.PI * 2)))
         );
         double[] end = palm[6];

         for (int k = -1; k <= 1; k++) {
            double[] base = Prim.add(end, 0.0, k * 0.032, -0.01);
            double[] tip = Prim.add(base, 0.006 * k, 0.04 + (k == 0 ? 0.016 : 0.0), 0.052);
            Prim.spike(b, base, tip, 0.011, 4, (u, v) -> Col.mix(Col.dark(c1, 0.45), Col.tint(c1, 0.35), v));
         }

         for (int i = 0; i < tips.size(); i++) {
            double[] tp = tips.get(i);
            double tw = b.animate ? 0.5 + 0.5 * Math.sin(t * 3.0 + i * 1.7 + side * 0.9) : 1.0;
            int lamp = i % 2 == 0 ? Col.tint(c2, 0.25) : Col.tint(-7512, 0.2);
            double r = 0.02 * (0.85 + 0.3 * tw);
            Prim.sphere(b, tp[0], tp[1], tp[2], r, 5, 3, (u, v) -> Col.withA(Col.tint(lamp, 0.35 * tw), 0.65 + 0.35 * tw));
         }

         Prim.ellipsoid(b, 0.12, -0.005, -0.03, 0.05, 0.034, 0.05, 6, 3, (u, v) -> Col.shade(Col.dark(c1, 0.45), 0.8 + 0.3 * v));
      }

      b.mirrorU(false);
   }
}
