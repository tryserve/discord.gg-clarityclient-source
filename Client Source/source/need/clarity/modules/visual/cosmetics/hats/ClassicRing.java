package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class ClassicRing implements Cosmetic {
   static final double LIFT = 0.3;
   static final double R = 0.3;
   static final double TUBE = 0.022;
   static final int MAJOR = 36;
   static final int MINOR = 6;
   private final HatParams p;

   public ClassicRing() {
      this(HatParams.defaults());
   }

   public ClassicRing(HatParams p) {
      this.p = p;
   }

   private static double frac(double x) {
      return x - Math.floor(x);
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double bob = this.p.bob() && b.animate ? Math.sin(t * 2.0) * 0.018 : 0.0;
      double breathe = b.animate ? 1.0 + 0.02 * Math.sin(t * 1.6) : 1.0;
      double drift = b.animate ? t * 0.1 : 0.0;
      double turn = b.animate ? t * this.p.spin() * Math.PI * 2.0 : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      b.push();
      b.translate(0.0, 0.3 + bob, 0.0);
      b.rotateY(turn);
      b.scaleLocal(breathe);
      Prim.Shade ringCol = (u, v) -> {
         double x = frac(u + drift);
         int base = Col.mix(c1, c2, 1.0 - Math.abs(2.0 * x - 1.0));
         double band = Math.pow(Math.max(0.0, Math.cos((u - t * 0.35) * Math.PI * 2.0)), 6.0);
         double top = Math.max(0.0, Math.sin(v * Math.PI * 2.0));
         double under = Math.max(0.0, -Math.sin(v * Math.PI * 2.0));
         return Col.shade(Col.tint(base, 0.3 * top + 0.55 * band), 1.0 - 0.22 * under);
      };
      Prim.torus(b, 0.0, 0.3, 0.022, 36, 6, ringCol);
      Prim.Shade inner = (u, v) -> Col.withA(Col.tint(ringCol.at(u, 0.25), 0.45), 0.55 * v * v);
      Prim.Shade outer = (u, v) -> Col.withA(Col.tint(ringCol.at(u, 0.25), 0.45), 0.55 * (1.0 - v) * (1.0 - v));
      Prim.ring(b, 0.0, 0.18, 0.3, 36, inner);
      Prim.ring(b, 0.0, 0.3, 0.42, 36, outer);
      Prim.torus(b, 0.0, 0.3, 0.0572, 28, 6, (u, v) -> Col.withA(ringCol.at(u, 0.25), 0.15));
      b.pop();
   }
}
