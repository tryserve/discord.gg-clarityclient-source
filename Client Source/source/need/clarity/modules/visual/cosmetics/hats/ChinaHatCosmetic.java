package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class ChinaHatCosmetic implements Cosmetic {
   static final double MODEL = 0.9375;
   static final double RADIUS = 0.65;
   static final double DROP = 0.3;
   static final double APEX = 0.22;
   static final double APEX_HELMET = 0.28;
   static final int SEGS = 40;
   static final double OUTLINE_R = 0.012;
   private final HatParams p;
   public boolean outline = true;
   public boolean darkLayer = true;

   public ChinaHatCosmetic() {
      this(HatParams.defaults());
   }

   public ChinaHatCosmetic(HatParams p) {
      this.p = p;
   }

   static int gradient(double angleDeg, int[] colors) {
      int n = colors.length;
      if (n == 1) {
         return colors[0];
      }

      double norm = (angleDeg % 360.0 + 360.0) % 360.0;
      double per = 360.0 / n;
      int i1 = (int)(norm / per) % n;
      int i2 = (i1 + 1) % n;
      return Col.mix(colors[i1], colors[i2], norm % per / per);
   }

   private static double oldAngleDeg(double u) {
      return (u - 0.25) * 360.0;
   }

   @Override
   public void build(Builder b) {
      double apex = this.p.helmet() ? 0.28 : 0.22;
      double spin = b.animate ? Math.toRadians(b.time * this.p.chinaSpin() * 360.0) : 0.0;
      int[] g = this.p.gradient();
      double ea = Col.clamp01(this.p.edgeAlpha());
      double ca = Col.clamp01(this.p.centerAlpha());
      b.push();
      b.scaleLocal(0.9375);
      b.translate(0.0, apex - 0.3, 0.0);
      b.rotateY(spin);
      Prim.cone(b, 0.0, 0.65, 0.3, 40, (u, v) -> Col.withA(gradient(oldAngleDeg(u), g), ea + (ca - ea) * v), false);
      if (this.darkLayer) {
         Prim.cone(b, -0.0015, 0.6474, 0.3, 40, (u, v) -> {
            int base = gradient(oldAngleDeg(u), g);
            return Col.withA(Col.shade(base, 1.0 - 0.75 * (1.0 - v)), ea + (ca - ea) * v);
         }, false);
      }

      if (this.outline) {
         Prim.torus(b, 0.0, 0.65, 0.012, 40, 4, (u, v) -> Col.withA(Col.shade(gradient(oldAngleDeg(u), g), 0.5), 1.0));
      }

      b.pop();
   }
}
