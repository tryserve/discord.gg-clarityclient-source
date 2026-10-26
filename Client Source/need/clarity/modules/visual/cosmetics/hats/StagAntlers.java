package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class StagAntlers implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public StagAntlers() {
      this(HatParams.defaults());
   }

   public StagAntlers(HatParams p) {
      this.p = p;
   }

   static Prim.BranchSpec spec() {
      Prim.BranchSpec sp = new Prim.BranchSpec();
      sp.maxDepth = 1;
      sp.tines = 3;
      sp.steps = 8;
      sp.childSteps = 4;
      sp.sides = 6;
      sp.taper = 0.28;
      sp.tineFrom = 0.26;
      sp.tineTo = 0.76;
      sp.spread = 0.85;
      sp.childLen = 0.46;
      sp.childRadius = 0.74;
      sp.bend = 1.15;
      sp.bendDir = Prim.unit(new double[]{-0.42, 0.0, 0.9});
      sp.sideRef = new double[]{0.0, 0.0, 1.0};
      sp.phases = new double[]{0.0, 0.45, -0.35};
      sp.forkTip = true;
      sp.tipSpread = 0.45;
      sp.jitter = 0.3;
      sp.seed = 7;
      return sp;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double wave = Anim.frac(t * 0.3) * 1.4 - 0.2;
      int c1 = b.c1;
      int c2 = b.c2;
      Prim.BranchShade bone = (depth, u, v) -> {
         double g = (depth + v) / 2.0;
         int col = Col.mix(Col.dark(c1, 0.5), Col.tint(c1, 0.3), Prim.sm(g * 1.25));
         if (depth >= 1) {
            col = Col.mix(col, c2, 0.65 * Prim.sm((v - 0.55) / 0.45));
         }

         col = Col.tint(col, 0.45 * Anim.sweep(g, wave, 0.12));
         return Col.shade(col, 0.86 + 0.14 * Math.cos(u * (Math.PI * 2)));
      };

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         Prim.branch(b, spec(), Prim.P(0.12, -0.02, -0.03), new double[]{0.3, 1.0, -0.22}, 0.5, 0.036, bone);
         Prim.ellipsoid(b, 0.12, -0.005, -0.03, 0.052, 0.036, 0.052, 8, 4, (u, v) -> Col.shade(Col.dark(c1, 0.45), 0.8 + 0.3 * v));
      }

      b.mirrorU(false);
   }
}
