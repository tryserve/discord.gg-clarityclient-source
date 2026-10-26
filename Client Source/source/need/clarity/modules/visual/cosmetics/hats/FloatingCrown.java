package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class FloatingCrown implements Cosmetic {
   static final double R0 = 0.24;
   static final double R1 = 0.262;
   static final double BAND_H = 0.05;
   static final double HOVER = 0.2;
   static final double HELMET_LIFT = 0.06;
   static final int SEG = 28;
   static final int TINES = 6;
   static final int BLADE_STEPS = 6;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public FloatingCrown() {
      this(HatParams.defaults());
   }

   public FloatingCrown(HatParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int metal = b.c1;
      int accent = b.c2;
      int gemAlt = Col.tint(metal, 0.55);
      int light = Col.tint(accent, 0.6);
      double floor = this.p.helmet() ? 0.06 : 0.0;
      double bob = b.animate && this.p.bob() ? 0.018 * Math.sin(t * 1.7) : 0.0;
      double pulse = 0.8 + 0.2 * Math.sin(t * 2.2);
      b.push();
      b.translate(0.0, floor, 0.0);
      Prim.cylinder(b, 0.01, 0.2 + bob, 0.285, 0.235, 24, (u, v) -> {
         double ray = 0.55 + 0.45 * (0.5 + 0.5 * Math.cos(u * (Math.PI * 2) * 6.0 - t * 0.8));
         return Col.withA(Col.mix(light, Col.tint(accent, 0.85), v), 0.24 * v * v * ray * pulse);
      }, false, false);
      Prim.disc(b, 0.008, 0.27, 24, (u, v) -> Col.withA(Col.mix(Col.tint(accent, 0.85), light, Prim.sm(v)), 0.3 * Math.pow(1.0 - v, 1.4) * pulse));

      for (int k = 0; k < 2; k++) {
         double age = frac(t * 0.45 + k * 0.5);
         double rIn = 0.28200000000000003 + 0.1 * age;
         double fade = (1.0 - age) * (1.0 - age) * Prim.sm(age * 8.0);
         Prim.ring(b, 0.2 + bob - 0.006, rIn, rIn + 0.03, 28, (u, v) -> Col.withA(light, 0.55 * fade * Math.sin(Math.PI * v)));
      }

      double wob = b.animate ? 1.0 : 0.0;
      b.push();
      b.translate(0.0, 0.2 + bob, 0.0);
      b.rotateX(0.045 * wob * Math.sin(t * 1.3));
      b.rotateZ(0.045 * wob * Math.cos(t * 1.1));
      b.rotateY(t * this.p.spin() * (Math.PI * 2));
      Prim.cylinder(b, 0.0, 0.05, 0.24, 0.262, 28, (u, v) -> GemKit.metal(metal, u, 0.3 + 0.45 * v, t, 0.12), false, false);
      Prim.Shade rim = (u, v) -> GemKit.metal(Col.tint(metal, 0.2), u, 0.85 + 0.15 * v, t, 0.12);
      Prim.cylinder(b, 0.0, 0.011, 0.243, 0.251, 28, rim, false, false);
      Prim.cylinder(b, 0.03900000000000001, 0.05, 0.254, 0.266, 28, rim, false, false);

      for (int k = 0; k < 6; k++) {
         double th = (Math.PI / 2) + k * (Math.PI * 2) / 6.0;
         double h = k % 2 == 0 ? 0.235 : 0.155;
         double co = Math.cos(th);
         double so = Math.sin(th);
         double top = 0.05;
         double[][] spine = Prim.bezier(
            6,
            Prim.P(0.252 * co, top - 0.004, 0.252 * so),
            Prim.P(0.276 * co, top + 0.4 * h, 0.276 * so),
            Prim.P(0.258 * co, top + 0.75 * h, 0.258 * so),
            Prim.P((0.262 - 0.07 * h / 0.235) * co, top + h, (0.262 - 0.07 * h / 0.235) * so)
         );
         int kk = k;
         Prim.flatTube(b, spine, Prim.P(-so, 0.0, co), v -> 0.046 * Math.pow(1.0 - v, 0.85) * (0.62 + 1.1 * v), v -> 0.0085 * Math.pow(1.0 - v, 0.6) + 0.001, 4, (u, v) -> {
            int cx = GemKit.metal(metal, (kk + v) / 6.0, 0.35 + 0.5 * (0.5 + 0.5 * Math.cos(u * (Math.PI * 2))), t, 0.12);
            return Col.tint(cx, 0.25 * Prim.sm((v - 0.6) / 0.4));
         });
         GemKit.mountOnWall(b, 0.264, th, 0.026);
         GemKit.gem(b, 0.021, 0.015, 0.008, 4, accent, gemAlt, k * 1.9);
         b.pop();
         if (k % 2 == 0) {
            double[] tip = spine[6];
            GemKit.glint(b, Prim.add(tip, 0.0, 0.006, 0.0), 0.07, light, GemKit.twinkle(b, 3.4, k * 0.17));
         }
      }

      for (int k = 0; k < 6; k++) {
         double th = (Math.PI / 2) + (k + 0.5) * (Math.PI * 2) / 6.0;
         double y = 0.175 + 0.018 * Math.sin(t * 1.9 + k * 1.3);
         double[] c = GemKit.ring(0.31, th, y);
         b.push();
         b.translate(c[0], c[1], c[2]);
         b.rotateY(t * 1.5 + k);
         GemKit.gem(b, 0.024, 0.05, 0.04, 4, accent, gemAlt, k * 2.3);
         b.pop();
         GemKit.glint(b, c, 0.055, light, GemKit.twinkle(b, 2.7, k * 0.21 + 0.4));
      }

      double cy = 0.2 + 0.02 * (b.animate ? Math.sin(t * 1.4) : 0.0);
      b.push();
      b.translate(0.0, cy, 0.0);
      b.rotateY(-t * 0.9);
      GemKit.gem(b, 0.052, 0.072, 0.062, 6, accent, gemAlt, 9.0);
      b.pop();
      GemKit.glint(b, Prim.P(0.02, cy + 0.03, 0.03), 0.11, Col.tint(accent, 0.7), GemKit.twinkle(b, 2.9, 0.05));
      b.push();
      b.translate(0.0, cy, 0.0);
      b.rotateX(0.9);
      b.rotateY(t * 0.8);
      Prim.ring(b, 0.0, 0.088, 0.097, 20, (u, v) -> GemKit.metal(Col.tint(metal, 0.15), u, 0.9, t, 0.12));
      b.pop();
      b.pop();
      b.pop();
   }

   private static double frac(double x) {
      return x - Math.floor(x);
   }
}
