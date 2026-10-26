package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class CornerBrackets implements Cosmetic {
   private static final double BEAT = 2.4;
   private static final double CORE = 0.026;
   private static final double GLOW = 0.062;
   private static final double JOINT = 0.052;
   private final TargetParams p;

   public CornerBrackets() {
      this(TargetParams.defaults());
   }

   public CornerBrackets(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double beat = b.animate ? Math.exp(-7.0 * TargetKit.frac(t / 2.4)) : 0.0;
      double breathe = b.animate ? 0.01 * Math.sin(t * 1.9) : 0.0;
      double spread = breathe + 0.075 * beat + (1.0 - intro) * 0.55;
      double X = this.p.hw() + spread;
      double Y = this.p.hh() + spread;
      double Z = this.p.hw() + spread;
      double lx = clamp(0.72 * X, 0.14, 0.7 * bulk);
      double lz = clamp(0.72 * Z, 0.14, 0.7 * bulk);
      double ly = clamp(0.6 * Y, 0.16, 0.7 * bulk);
      double core = 0.026 * bulk;
      double glow = 0.062 * bulk;
      double joint = 0.052 * bulk;
      double[] cam = b.viewLocal();
      double scale = b.scale();
      int corner = 0;

      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sy = -1; sy <= 1; sy += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               double[] c = Prim.P(sx * X, sy * Y, sz * Z);
               double lit = 0.8 + 0.2 * TargetKit.wave(b, 2.3, corner * 0.9) + 0.3 * beat;
               double vis = intro * TargetKit.near(cam, c[0], c[1], c[2], scale);
               corner++;
               if (!(vis < 0.01)) {
                  int hot = Col.tint(b.c1, Math.min(1.0, 0.3 + 0.45 * beat));
                  int coreBase = Col.withA(Col.mix(b.c1, hot, 0.5), Math.min(1.0, 0.95 * lit) * vis);
                  int coreTip = Col.withA(b.c2, 0.6 * vis);
                  int glowBase = Col.withA(b.c1, 0.3 * lit * vis);
                  int glowTip = Col.withA(b.c2, 0.05 * vis);
                  double[][] tips = new double[][]{Prim.P(c[0] - sx * lx, c[1], c[2]), Prim.P(c[0], c[1] - sy * ly, c[2]), Prim.P(c[0], c[1], c[2] - sz * lz)};

                  for (double[] tip : tips) {
                     TargetKit.beam(b, c, tip, core, coreBase, coreTip, true);
                     TargetKit.beam(b, c, tip, glow, glowBase, glowTip, true);
                  }

                  int jc = Col.withA(Col.tint(b.c1, 0.6 + 0.3 * beat), Math.min(1.0, 0.95 * lit) * vis);
                  Prim.box(b, c[0] - joint, c[1] - joint, c[2] - joint, c[0] + joint, c[1] + joint, c[2] + joint, Col.shade(jc, 0.85), jc);
                  int halo = Col.withA(Col.tint(b.c1, 0.3), (0.18 + 0.25 * beat) * vis);
                  double h = joint * 1.9;
                  Prim.box(b, c[0] - h, c[1] - h, c[2] - h, c[0] + h, c[1] + h, c[2] + h, halo, halo);
               }
            }
         }
      }
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
