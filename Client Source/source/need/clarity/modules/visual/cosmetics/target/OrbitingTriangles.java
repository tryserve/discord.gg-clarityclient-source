package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class OrbitingTriangles implements Cosmetic {
   private static final int COUNT = 5;
   private static final int GHOSTS = 6;
   private static final double TRAIL_STEP = 0.13;
   private static final double SQRT3 = Math.sqrt(3.0);
   private final TargetParams p;

   public OrbitingTriangles() {
      this(TargetParams.defaults());
   }

   public OrbitingTriangles(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double slam = 1.0 - intro;
      double dir = this.p.dir();
      double orbit = (Math.PI * 2) * this.p.phase() * 0.6 - slam * 2.4 * dir;
      double side = TargetKit.clamp(0.42 * bulk, 0.22, 1.4);
      double r0 = this.p.hw() + 0.26 * bulk + slam * 1.2 * bulk;
      double span = this.p.hh() * 0.82;
      double[] cam = b.viewLocal();
      double scale = b.scale();

      for (int i = 0; i < 5; i++) {
         double f = (i + 0.5) / 5.0;
         double y = (f - 0.5) * 2.0 * span + (b.animate ? 0.05 * bulk * Math.sin(t * 1.5 + i * 1.7) : 0.0);
         double rad = r0 + (b.animate ? 0.04 * bulk * Math.sin(t * 1.1 + i * 2.3) : 0.0);
         double a = orbit + (Math.PI * 2) * i / 5.0;
         double spin = (Math.PI * 2) * this.p.phase() * 1.8 + i * 1.3;
         int col = Col.mix(b.c1, b.c2, f);

         for (int g = 6; g >= 0; g--) {
            double ag = a - g * 0.13 * dir;
            double x = rad * Math.cos(ag);
            double z = rad * Math.sin(ag);
            double vis = intro * TargetKit.near(cam, x, y, z, scale);
            if (!(vis < 0.01)) {
               b.push();
               b.translate(x, y, z);
               b.rotateY((Math.PI / 2) - ag + spin);
               b.rotateX(0.42);
               if (g == 0) {
                  this.frame(b, side, col, f, vis, i);
               } else {
                  ghost(b, side * (1.0 - 0.09 * g), col, 0.3 * (1.0 - g / 7.0) * vis);
               }

               b.pop();
            }
         }
      }
   }

   private static double[][] corners(double s, double sign) {
      double up = s / SQRT3;
      double dn = s / (2.0 * SQRT3);
      return new double[][]{Prim.P(0.0, sign * up, 0.0), Prim.P(-0.5 * s, -sign * dn, 0.0), Prim.P(0.5 * s, -sign * dn, 0.0)};
   }

   private static void ghost(Builder b, double s, int col, double alpha) {
      double[][] v = corners(s, 1.0);
      double[] c = Prim.P(0.0, 0.0, 0.0);
      int edge = Col.withA(col, alpha);
      int mid = Col.withA(col, alpha * 0.2);

      for (int k = 0; k < 3; k++) {
         b.tri(c, v[k], v[(k + 1) % 3], mid, edge, edge);
      }
   }

   private void frame(Builder b, double s, int col, double f, double vis, int i) {
      double[][] v = corners(s, 1.0);
      double[] c = Prim.P(0.0, 0.0, 0.0);
      double glint = Math.pow(b.facing(c, new double[]{0.0, 0.0, 1.0}), 3.0);
      double lit = 0.75 + 0.25 * TargetKit.wave(b, 2.4, i * 1.9) + 0.35 * glint;
      int corner = Col.withA(Col.tint(col, 0.35 * glint), Math.min(1.0, 0.4 * lit) * vis);
      int heart = Col.withA(col, 0.04 * vis);

      for (int k = 0; k < 3; k++) {
         b.tri(c, v[k], v[(k + 1) % 3], heart, corner, corner);
      }

      double half = 0.0105 * this.p.bulk();
      int hot = Col.withA(Col.tint(col, 0.3 + 0.45 * glint), Math.min(1.0, 0.92 * lit) * vis);
      int cold = Col.withA(Col.mix(col, b.c2, 0.35), Math.min(1.0, 0.78 * lit) * vis);

      for (int k = 0; k < 3; k++) {
         int j = (k + 1) % 3;
         TargetKit.beam(b, v[k], v[j], half, k == 0 ? hot : cold, j == 0 ? hot : cold, false);
      }

      double[][] core = corners(s * 0.44, -1.0);
      int cc = Col.withA(Col.tint(col, 0.65), Math.min(1.0, 0.85 * lit) * vis);
      b.tri(core[0], core[1], core[2], cc, Col.shade(cc, 0.8), Col.shade(cc, 0.8));
   }
}
