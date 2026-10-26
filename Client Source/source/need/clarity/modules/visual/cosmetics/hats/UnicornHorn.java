package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class UnicornHorn implements Cosmetic {
   static final double LEN = 0.56;
   static final double R = 0.062;
   static final double LEAN = 0.32;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public UnicornHorn() {
      this(HatParams.defaults());
   }

   public UnicornHorn(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      double glow = 0.5 + 0.5 * Math.sin(t * 2.0);
      b.push();
      b.translate(0.0, -0.03, 0.17);
      b.rotateX(0.32);
      Prim.spiralCone(b, 0.0, 0.062, 0.56, 3, 2.2, 0.16, 9, 12, (u, v) -> {
         double stripe = 0.5 + 0.5 * Math.cos(3.0 * u * (Math.PI * 2) - 13.823007675795091 * v);
         int col = Col.mix(Col.mix(c2, Col.tint(c1, 0.35), stripe), Col.tint(c1, 0.7), 0.15);
         col = Col.tint(col, 0.55 * Prim.sm((v - 0.55) / 0.45) + 0.12 * glow * v);
         return Col.shade(col, 0.72 + 0.28 * stripe);
      });
      int gold = -1592772;
      Prim.taperedTube(b, Prim.P(0.0, -0.02, 0.0), Prim.P(0.0, 0.05, 0.0), 0.072, 0.066, 8, (u, v) -> Col.shade(gold, 0.8 + 0.25 * Math.cos(u * (Math.PI * 2)) + 0.1 * v), true);
      Prim.taperedTube(b, Prim.P(0.0, 0.05, 0.0), Prim.P(0.0, 0.062, 0.0), 0.074, 0.074, 8, (u, v) -> Col.shade(Col.tint(gold, 0.25), 0.9), true);
      Prim.sphere(b, 0.0, 0.56, 0.0, 0.017 * (1.0 + 0.2 * glow), 8, 4, (u, v) -> Col.withA(Col.tint(c1, 0.6), 0.35 + 0.25 * glow));
      double turns = t * this.p.spin() * 4.0;

      for (int k = 0; k < 3; k++) {
         double ph = Anim.frac(t * 0.33 + k / 3.0);
         double h = 0.06 + ph * 0.4600000000000001;
         double rad = 0.1 + 0.03 * Math.sin(ph * (Math.PI * 2) * 2.0 + k);
         double a = turns * (Math.PI * 2) + k * 2.1 + ph * 3.0;
         double size = 0.052 * Math.sin(Math.PI * ph);
         if (!(size < 0.004)) {
            int sp = Col.withA(Col.tint(k == 1 ? c2 : c1, 0.75), 0.95 * Math.sin(Math.PI * ph));
            double spin = t * 1.4 + k;

            for (int cross = 0; cross < 2; cross++) {
               b.push().translate(rad * Math.cos(a), h, rad * Math.sin(a)).rotateY(spin + cross * Math.PI / 2.0).rotateX(Math.PI / 2);
               Prim.star(b, 0.0, 4, size, size * 0.22, Math.PI / 4, Prim.solid(sp));
               b.pop();
            }
         }
      }

      b.pop();
   }
}
