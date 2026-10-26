package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Shards implements Cosmetic {
   static final int MAX = 12;
   private final AuraParams p;

   public Shards() {
      this(AuraParams.defaults());
   }

   public Shards(AuraParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      int n = this.p.items(12);
      int d = this.p.detail(n);
      int sides = d == 2 ? 6 : (d == 1 ? 5 : 4);
      double t = b.animate ? b.time : 0.0;
      double radius = this.p.radius();

      for (int k = 0; k < n; k++) {
         double phi = this.p.angle(k, n);
         double size = 0.85 + 0.35 * Prim.rnd(k * 5 + 3);
         double y = 0.11 * Math.sin(t * 1.3 + k * 1.9) + (Prim.rnd(k * 7 + 11) - 0.5) * 0.26;
         double spin = t * (1.2 + 0.4 * (k % 3)) + k;
         double shimmer = 1.0 + 0.22 * Math.sin(t * 3.1 + k * 1.7);
         b.push();
         b.translate(radius * Math.sin(phi), y, radius * Math.cos(phi));
         b.rotateY(phi);
         b.rotateX(0.22);
         b.rotateY(spin);
         double f = AuraKit.nearOrigin(b);
         if (f > 0.02) {
            shard(b, size, shimmer, sides, f);
         }

         b.pop();
      }
   }

   private static void shard(Builder b, double size, double shimmer, int sides, double f) {
      double up = 0.4 * size;
      double down = 0.22 * size;
      double r = 0.068 * size;
      int c1 = b.c1;
      int c2 = b.c2;
      int mid = Col.shade(Col.tint(Col.mix(c1, c2, 0.3), 0.12), shimmer);
      int tip = Col.tint(c2, 0.3);
      int foot = Col.dark(c1, 0.45);
      double[] o = Prim.P(0.0, 0.0, 0.0);
      Prim.spike(b, o, Prim.P(0.0, up, 0.0), r, sides, AuraKit.fade((u, v) -> Col.mix(mid, tip, v), f));
      Prim.spike(b, o, Prim.P(0.0, -down, 0.0), r, sides, AuraKit.fade((u, v) -> Col.mix(mid, foot, v), f));
      int glow = Col.tint(c2, 0.3);
      Prim.Shade shell = AuraKit.fade((u, v) -> Col.withA(Col.mix(Col.tint(c1, 0.3), glow, 1.0 - v), 0.14), f);
      Prim.spike(b, Prim.P(0.0, -0.01, 0.0), Prim.P(0.0, up * 1.18, 0.0), r * 1.6, sides, shell);
      Prim.spike(b, Prim.P(0.0, -0.01, 0.0), Prim.P(0.0, -down * 1.18, 0.0), r * 1.6, sides, shell);
   }
}
