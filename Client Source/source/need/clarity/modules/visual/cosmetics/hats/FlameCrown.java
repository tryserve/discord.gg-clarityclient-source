package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class FlameCrown implements Cosmetic {
   static final double R0 = 0.262;
   static final double R1 = 0.285;
   static final double BAND_H = 0.075;
   static final double HELMET_LIFT = 0.06;
   static final int SEG = 28;
   static final int PLUMES = 7;
   static final int STEPS = 5;
   static final int EMBERS = 12;
   static final double[] HEIGHTS = new double[]{0.34, 0.22, 0.29, 0.2, 0.2, 0.29, 0.22};
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public FlameCrown() {
      this(HatParams.defaults());
   }

   public FlameCrown(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int core = b.c1;
      int body = b.c2;
      int coal = Col.dark(body, 0.78);
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.06 : 0.0, 0.0);
      Prim.cylinder(b, 0.0, 0.075, 0.262, 0.285, 28, (u, v) -> {
         double crawl = 0.5 + 0.5 * Math.sin(u * (Math.PI * 2) * 7.0 + t * 1.6 + 1.7 * Math.sin(u * (Math.PI * 2) * 3.0));
         double heat = (0.1 + 0.75 * v * v) * (0.45 + 0.55 * crawl);
         return Col.mix(coal, Col.tint(body, 0.1), heat);
      }, false, false);
      Prim.cylinder(b, 0.062, 0.079, 0.283, 0.29, 28, (u, v) -> {
         double f = 0.5 + 0.5 * Math.sin(t * 5.0 + u * (Math.PI * 2) * 4.0);
         return Col.mix(body, core, 0.35 + 0.5 * f);
      }, false, false);
      Prim.cylinder(b, 0.0, 0.014, 0.265, 0.272, 28, (u, v) -> Col.dark(body, 0.68), false, false);

      for (int k = 0; k < 7; k++) {
         this.plume(b, k, (Math.PI / 2) + k * (Math.PI * 2) / 7.0, HEIGHTS[k], t, core, body);
      }

      for (int k = 0; k < 7; k++) {
         double th = (Math.PI / 2) + (k + 0.5) * (Math.PI * 2) / 7.0;
         double flick = 0.85 + 0.15 * Math.sin(t * 7.3 + k * 2.3);
         double[] base = GemKit.ring(0.27299999999999996, th, 0.071);
         double sway = 0.014 * Math.sin(t * 3.1 + k * 1.7);
         double[] tip = Prim.add(base, 0.028 * Math.cos(th) - sway * Math.sin(th), 0.095 * flick, 0.028 * Math.sin(th) + sway * Math.cos(th));
         Prim.spike(b, base, tip, 0.03, 3, (u, v) -> Col.withA(Col.mix(body, core, v * 0.7), 0.9 * (1.0 - Prim.sm((v - 0.3) / 0.7))));
      }

      for (int k = 0; k < 12; k++) {
         double ph = Prim.rnd(k * 7 + 3);
         double age = frac(t * (0.2 + 0.1 * Prim.rnd(k + 41)) + ph);
         double ang = (Math.PI / 2) + k * (Math.PI * 2) / 12.0 + t * 0.7 * (k % 2 == 0 ? 1 : -1) + age * 1.4;
         double rad = 0.07 + 0.19 * Prim.rnd(k * 5 + 11) + 0.03 * age;
         double y = 0.14500000000000002 + age * (0.3 + 0.24 * Prim.rnd(k * 3 + 17));
         double fade = Prim.sm(age * 7.0) * (1.0 - age * age);
         int c = Col.withA(Col.mix(Col.tint(core, 0.55), body, age), fade);
         GemKit.spark(b, GemKit.ring(rad, ang, y), 0.011 * (1.0 - 0.55 * age), c);
      }

      b.pop();
   }

   private static double frac(double x) {
      return x - Math.floor(x);
   }

   private void plume(Builder b, int k, double th, double h, double t, int core, int body) {
      double flick = 0.86 + 0.14 * Math.sin(t * 6.3 + k * 1.9) * Math.sin(t * 3.7 + k * 0.7) + 0.05 * Math.sin(t * 11.0 + k * 2.7);
      double hh = h * Math.max(0.55, flick);
      double lean = 0.035 + 0.05 * h;
      double sway = 0.03 * Math.sin(t * 2.1 + k * 1.3) + 0.02 * Math.sin(t * 3.6 + k * 0.9);
      double co = Math.cos(th);
      double so = Math.sin(th);
      double tx = -so;
      double tz = co;
      double[] base = GemKit.ring(0.26499999999999996, th, 0.071);
      double[][] spine = Prim.bezier(
         5,
         base,
         Prim.add(base, co * lean * 0.15 + tx * sway * 0.2, hh * 0.35, so * lean * 0.15 + tz * sway * 0.2),
         Prim.add(base, co * lean * 0.6 + tx * sway * 0.7, hh * 0.72, so * lean * 0.6 + tz * sway * 0.7),
         Prim.add(base, co * lean + tx * sway, hh, so * lean + tz * sway)
      );
      double[] wd = k % 2 == 0 ? Prim.P(tx, 0.0, tz) : Prim.P(co, 0.0, so);
      double wb = 0.058 + 0.05 * h;
      int kk = k;
      Prim.flatTube(
         b,
         spine,
         wd,
         v -> wb * Math.pow(1.0 - v, 1.15) * (0.75 + 0.55 * Math.sin(Math.PI * Math.pow(v, 0.8))),
         v -> 0.3 * wb * Math.pow(1.0 - v, 1.15) * (0.75 + 0.55 * Math.sin(Math.PI * Math.pow(v, 0.8))),
         5,
         (u, v) -> {
            int c = Col.mix(Col.shade(body, 0.8), body, Prim.sm(v / 0.35));
            c = Col.mix(c, Col.mix(body, core, 0.6), Prim.sm((v - 0.45) / 0.4));
            double soft = Math.abs(Math.cos(u * (Math.PI * 2)));
            double a = 0.9 * (1.0 - Prim.sm((v - 0.55) / 0.45)) * (1.0 - 0.5 * soft * soft);
            c = Col.shade(c, 1.0 + 0.14 * Math.sin(t * 9.0 + v * 6.0 + kk));
            return Col.withA(c, a);
         }
      );
      double[][] cs = new double[spine.length][];

      for (int i = 0; i < spine.length; i++) {
         cs[i] = Prim.add(base, (spine[i][0] - base[0]) * 0.6, (spine[i][1] - base[1]) * 0.6, (spine[i][2] - base[2]) * 0.6);
      }

      Prim.flatTube(b, cs, wd, v -> 0.5 * wb * (1.0 - v) * (0.8 + 0.4 * Math.sin(Math.PI * v)), v -> 0.36 * wb * (1.0 - 0.5 * v) * (1.0 - Math.pow(v, 4.0)), 4, (u, v) -> {
         int c = Col.mix(Col.tint(core, 0.3), Col.tint(core, 0.85), Prim.sm(v));
         double soft = Math.abs(Math.cos(u * (Math.PI * 2)));
         double a = (0.98 - 0.55 * Prim.sm((v - 0.5) / 0.5)) * (1.0 - 0.35 * soft * soft);
         return Col.withA(Col.shade(c, 1.0 + 0.12 * Math.sin(t * 12.0 + kk * 1.3)), a);
      });
   }
}
