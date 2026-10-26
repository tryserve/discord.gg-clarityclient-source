package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Orbs implements Cosmetic {
   static final int MAX = 12;
   private static final double CORE = 0.055;
   private static final double WAVE = 0.2;
   private static final double ARC = 0.9;
   private final AuraParams p;

   public Orbs() {
      this(AuraParams.defaults());
   }

   public Orbs(AuraParams p) {
      this.p = p;
   }

   private static double pathY(double a, double t) {
      return 0.2 * Math.sin(3.0 * a + t * 0.9);
   }

   @Override
   public void build(Builder b) {
      int n = this.p.items(12);
      int d = this.p.detail(n);
      int lon = d == 2 ? 8 : (d == 1 ? 6 : 5);
      int lat = d == 2 ? 4 : 3;
      int trailSegs = d == 2 ? 6 : (d == 1 ? 4 : 3);
      int trailSides = d == 2 ? 4 : 3;
      double t = b.animate ? b.time : 0.0;
      double radius = this.p.radius();
      double dir = this.p.dir();

      for (int k = 0; k < n; k++) {
         int own = k % 2 == 0 ? b.c1 : b.c2;
         int other = k % 2 == 0 ? b.c2 : b.c1;
         double phi = this.p.angle(k, n);
         double[] centre = Prim.P(radius * Math.sin(phi), pathY(phi, t), radius * Math.cos(phi));
         double f = AuraKit.near(b.viewLocal(), centre[0], centre[1], centre[2], b.scale());
         if (!(f <= 0.02)) {
            double[][] pts = new double[trailSegs + 1][];

            for (int s = 0; s <= trailSegs; s++) {
               double a = phi - dir * 0.9 * s / trailSegs;
               pts[s] = Prim.P(radius * Math.sin(a), pathY(a, t), radius * Math.cos(a));
            }

            for (int s = 0; s < trailSegs; s++) {
               int seg = s;
               double r0 = 0.030250000000000003 * (1.0 - (double)s / trailSegs) + 0.003;
               double r1 = 0.030250000000000003 * (1.0 - (double)(s + 1) / trailSegs) + 0.003;
               Prim.taperedTube(b, pts[s], pts[s + 1], r0, r1, trailSides, (u, v) -> {
                  double g = (seg + v) / trailSegs;
                  return AuraKit.fade(Col.withA(Col.tint(Col.mix(own, other, g), 0.25), 0.75 * Math.pow(1.0 - g, 1.4)), f);
               }, false);
            }

            double pulse = b.animate ? 1.0 + 0.1 * Math.sin(t * 3.0 + k * 1.3) : 1.0;
            double rr = 0.055 * pulse;
            int core = Col.tint(own, 0.35);
            Prim.sphere(b, centre[0], centre[1], centre[2], rr, lon, lat, AuraKit.fade((u, v) -> Col.mix(Col.dark(core, 0.35), Col.tint(core, 0.65), v), f));
            double halo = 0.16 + 0.08 * AuraKit.wave(b, 2.2, k * 1.7);
            Prim.sphere(b, centre[0], centre[1], centre[2], rr * 2.1, lon, lat, AuraKit.fade((u, v) -> Col.withA(Col.tint(own, 0.3), halo), f));
         }
      }
   }
}
