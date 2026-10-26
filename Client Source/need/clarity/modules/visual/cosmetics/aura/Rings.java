package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Rings implements Cosmetic {
   static final int MAX = 6;
   private static final double BAND = 0.018;
   private static final double WALL = 0.015;
   private final AuraParams p;

   public Rings() {
      this(AuraParams.defaults());
   }

   public Rings(AuraParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      int n = this.p.items(6);
      int d = this.p.detail(n);
      int segs = d == 2 ? 36 : (d == 1 ? 30 : 24);
      double t = b.animate ? b.time : 0.0;
      double turns = this.p.phase();
      double baseR = this.p.radius();

      for (int j = 0; j < n; j++) {
         double g = n == 1 ? 0.0 : (double)j / (n - 1);
         double radius = baseR * (0.8 + 0.22 * g);
         double sign = j % 2 == 0 ? 1.0 : -1.0;
         double az = j * 2.399 + (Math.PI * 2) * turns * 0.35 * sign;
         double tilt = j == 0 && n >= 3 ? 0.0 : 0.5 + 0.85 * g;
         double spin = (Math.PI * 2) * turns * (1.0 + 0.3 * j) * sign;
         double bob = b.animate ? 0.03 * Math.sin(t * 1.3 + j * 1.9) : 0.0;
         b.push();
         b.translate(0.0, bob, 0.0);
         b.rotateY(az);
         b.rotateX(tilt);
         b.rotateY(spin);
         ring(b, j, radius, segs, sign);
         b.pop();
      }
   }

   private static void ring(Builder b, int j, double radius, int segs, double sign) {
      int c1 = b.c1;
      int c2 = b.c2;
      int own = j % 2 == 0 ? c1 : c2;
      int other = j % 2 == 0 ? c2 : c1;
      double[] cam = b.viewLocal();
      double scale = b.scale();
      Prim.Shade band = (u, v) -> {
         double uu = sign > 0.0 ? u : 1.0 - u;
         double comet = Math.pow(1.0 - uu, 2.2);
         int base = Col.mix(other, Col.tint(own, 0.35), comet);
         double a = 0.28 + 0.72 * comet;
         double fx = AuraKit.near(cam, radius * Math.cos(u * (Math.PI * 2)), 0.0, radius * Math.sin(u * (Math.PI * 2)), scale);
         return Col.withA(base, a * fx * (0.55 + 0.45 * v));
      };
      Prim.ring(b, 0.0, radius - 0.018, radius + 0.018, segs, band);
      Prim.Shade wall = (u, v) -> {
         int c = band.at(u, 0.5);
         return Col.alpha(c, 0.75 + 0.25 * v);
      };
      Prim.cylinder(b, -0.015, 0.015, radius, radius, segs, wall, false, false);
      double f = AuraKit.near(cam, radius, 0.0, 0.0, scale);
      if (f > 0.02) {
         int core = Col.tint(own, 0.55);
         Prim.sphere(b, radius, 0.0, 0.0, 0.028, 6, 3, AuraKit.fade((u, v) -> Col.mix(Col.dark(core, 0.3), Col.tint(core, 0.7), v), f));
         Prim.sphere(b, radius, 0.0, 0.0, 0.06, 6, 3, AuraKit.fade((u, v) -> Col.withA(Col.tint(own, 0.3), 0.2), f));
      }
   }
}
