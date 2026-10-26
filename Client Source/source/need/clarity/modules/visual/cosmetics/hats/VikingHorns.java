package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class VikingHorns implements Cosmetic {
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public VikingHorns() {
      this(HatParams.defaults());
   }

   public VikingHorns(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      int metalDark = Col.dark(c2, 0.45);
      int metalLight = Col.tint(c2, 0.2);
      Prim.Shade metal = (u, v) -> {
         double sheen = Math.pow(Math.max(0.0, Math.cos((u - t * 0.22) * (Math.PI * 2))), 5.0) * 0.55;
         return Col.tint(Col.mix(metalDark, metalLight, 0.35 + 0.35 * v), sheen);
      };
      Prim.prism(b, -0.105, -0.025, 0.345, 4, Math.PI / 4, metal, false);
      Prim.Shade rim = (u, v) -> Col.tint(metalLight, 0.25);
      Prim.prism(b, -0.113, -0.1, 0.352, 4, Math.PI / 4, rim, false);
      Prim.prism(b, -0.03, -0.017, 0.352, 4, Math.PI / 4, rim, false);
      Prim.tube(
         b,
         Prim.bezier(10, Prim.P(0.0, -0.03, -0.245), Prim.P(0.0, 0.13, 0.0), Prim.P(0.0, -0.03, 0.245)),
         v -> 0.018,
         5,
         (u, v) -> Col.shade(Col.mix(metalDark, metalLight, 0.5 + 0.5 * Math.cos(u * (Math.PI * 2))), 0.95),
         false,
         false
      );

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         Prim.taperedTube(
            b, Prim.P(0.236, -0.065, 0.0), Prim.P(0.3, -0.065, 0.0), 0.068, 0.056, 8, (u, v) -> Col.shade(Col.mix(metalDark, metalLight, 0.4 + 0.4 * Math.cos(u * (Math.PI * 2))), 0.95), true
         );
         double[][] spine = Prim.bezier(12, Prim.P(0.29, -0.065, 0.0), Prim.P(0.54, -0.09, 0.0), Prim.P(0.66, 0.05, 0.03), Prim.P(0.55, 0.34, 0.08));
         Prim.tube(b, spine, v -> 0.05 * Math.pow(1.0 - v, 0.75) + 0.005, 6, (u, v) -> {
            int bone = Col.mix(Col.dark(c1, 0.35), Col.tint(c1, 0.15), Prim.sm(v / 0.7));
            bone = Col.tint(bone, 0.45 * Prim.sm((v - 0.65) / 0.35) + 0.5 * Anim.sweep(v, Anim.frac(t * 0.3) * 1.5 - 0.25, 0.1));
            return Col.shade(bone, (0.9 + 0.1 * Math.cos(v * (Math.PI * 2) * 6.0)) * (0.88 + 0.12 * Math.cos(u * (Math.PI * 2))));
         }, false, true);
      }

      b.mirrorU(false);
   }
}
