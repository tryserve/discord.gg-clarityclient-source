package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class OrbitingOrbs implements Cosmetic {
   static final int ORBS = 5;
   static final double R = 0.33;
   static final double LIFT = 0.3;
   static final double ORB = 0.04;
   static final int TRAIL = 8;
   static final double TRAIL_ARC = 0.85;
   static final double PACE = 2.5;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public OrbitingOrbs() {
      this(HatParams.defaults());
   }

   public OrbitingOrbs(HatParams p) {
      this.p = p;
   }

   private static double heightAt(int k, double t) {
      return 0.045 * Math.sin(t * 1.7 + k * 1.26);
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double bob = this.p.bob() && b.animate ? Math.sin(t * 1.8) * 0.014 : 0.0;
      double rate = (Math.PI * 2) * this.p.spin() * 2.5;
      double dir = this.p.spin() < 0.0 ? -1.0 : 1.0;
      int c1 = b.c1;
      int c2 = b.c2;
      b.push();
      b.translate(0.0, 0.3 + bob, 0.0);
      Prim.ring(b, 0.0, 0.322, 0.338, 36, (u, v) -> Col.withA(Col.mix(c1, c2, 1.0 - Math.abs(2.0 * u - 1.0)), 0.2 * (1.0 - Math.abs(2.0 * v - 1.0))));

      for (int k = 0; k < 5; k++) {
         int own = k % 2 == 0 ? c1 : c2;
         int other = k % 2 == 0 ? c2 : c1;
         double th = k * (Math.PI * 2) / 5.0 + (b.animate ? t * rate : 0.0);
         double[][] pts = new double[9][];

         for (int s = 0; s <= 8; s++) {
            double f = s / 8.0;
            double a = th - dir * 0.85 * f;
            pts[s] = Prim.P(0.33 * Math.cos(a), heightAt(k, t - 0.5 * f), 0.33 * Math.sin(a));
         }

         for (int s = 0; s < 8; s++) {
            int seg = s;
            double r0 = 0.022000000000000002 * (1.0 - s / 8.0) + 0.003;
            double r1 = 0.022000000000000002 * (1.0 - (s + 1) / 8.0) + 0.003;
            Prim.Shade fade = (u, v) -> {
               double g = (seg + v) / 8.0;
               return Col.withA(Col.tint(Col.mix(own, other, g), 0.25), 0.75 * Math.pow(1.0 - g, 1.4));
            };
            Prim.taperedTube(b, pts[s], pts[s + 1], r0, r1, 4, fade, false);
         }

         double[] o = pts[0];
         double rr = 0.04 * (b.animate ? 1.0 + 0.1 * Math.sin(t * 3.0 + k * 1.3) : 1.0);
         int core = Col.tint(own, 0.35);
         Prim.sphere(b, o[0], o[1], o[2], rr, 8, 4, (u, v) -> Col.mix(Col.dark(core, 0.35), Col.tint(core, 0.65), v));
         Prim.sphere(b, o[0], o[1], o[2], rr * 2.0, 8, 4, (u, v) -> Col.withA(Col.tint(own, 0.3), 0.2));
      }

      b.pop();
   }
}
