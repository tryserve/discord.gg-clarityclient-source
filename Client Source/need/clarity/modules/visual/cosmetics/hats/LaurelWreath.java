package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class LaurelWreath implements Cosmetic {
   static final double R = 0.268;
   static final double HELMET_LIFT = 0.05;
   static final int NODES = 15;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public LaurelWreath() {
      this(HatParams.defaults());
   }

   public LaurelWreath(HatParams p) {
      this.p = p;
   }

   private static double[] stem(double s) {
      double phi = Math.toRadians(165.0 - 143.0 * s);
      double y = 0.036 + 0.024 * Math.cos(phi) + 0.006 * Math.sin(s * (Math.PI * 2) * 3.0);
      double r = 0.268 + 0.008 * Math.sin(s * (Math.PI * 2) * 2.5);
      return Prim.P(r * Math.sin(phi), y, r * Math.cos(phi));
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      int leaf = b.c1;
      int accent = b.c2;
      b.push();
      b.translate(0.0, this.p.helmet() ? 0.05 : 0.0, 0.0);

      for (int side = 0; side < 2; side++) {
         b.mirrorU(side == 1);
         this.branch(b, t, leaf, accent, side);
      }

      b.mirrorU(false);
      double ky = 0.028;
      double kz = -0.272;
      Prim.sphere(b, 0.0, ky, kz, 0.022, 5, 3, (u, v) -> Col.shade(accent, 0.75 + 0.35 * v));

      for (int side = -1; side <= 1; side += 2) {
         double[][] spine = Prim.bezier(6, Prim.P(0.006 * side, ky - 0.005, kz - 0.006), Prim.P(0.03 * side, ky - 0.08, kz - 0.012), Prim.P(0.022 * side, ky - 0.2, kz - 0.006));
         if (b.animate) {
            for (int i = 1; i < spine.length; i++) {
               spine[i][0] = spine[i][0] + side * 0.01 * (i / 6.0) * Math.sin(t * 1.7 + i * 0.6 + side);
            }
         }

         Prim.ribbon(b, spine, 0.046, 0.034, new double[]{1.0, 0.0, 0.0}, (u, v) -> Col.shade(Col.mix(accent, Col.tint(accent, 0.35), v), 0.82 + 0.3 * (1.0 - Math.abs(2.0 * u - 1.0))));
      }

      b.pop();
   }

   private static double[] rotY(double[] v, double a) {
      return new double[]{v[0] * Math.cos(a) + v[2] * Math.sin(a), v[1], -v[0] * Math.sin(a) + v[2] * Math.cos(a)};
   }

   private void branch(Builder b, double t, int leaf, int accent, int side) {
      double[][] spine = new double[16][];

      for (int i = 0; i <= 15; i++) {
         spine[i] = stem(i / 15.0);
      }

      int wood = Col.shade(leaf, 0.5);
      Prim.tube(b, spine, v -> 0.0105 * (1.0 - 0.35 * v), 3, (u, v) -> Col.shade(wood, 0.85 + 0.25 * Math.cos(u * (Math.PI * 2))), false, false);
      double[] up = new double[]{0.0, 1.0, 0.0};

      for (int i = 0; i < 15; i++) {
         double s = (i + 0.5) / 15.0;
         double[] c = stem(s);
         double ds = 0.03333333333333333;
         double[] tan = Prim.unit(Prim.sub(stem(Math.min(1.0, s + ds)), stem(Math.max(0.0, s - ds))));
         double phi = Math.toRadians(165.0 - 143.0 * s);
         double[] rad = new double[]{Math.sin(phi), 0.0, Math.cos(phi)};
         double len = 0.09 + 0.02 * Math.sin(Math.PI * s);
         double wid = 0.043;
         double sway = b.animate ? 0.07 * Math.sin(t * 1.5 + i * 0.9 + side * 1.3) : 0.0;
         double[] dOut = rotY(tan, Math.toRadians(36.0) + sway);
         double[] dIn = rotY(tan, Math.toRadians(-36.0) + sway);
         if (Prim.dot(dOut, rad) < Prim.dot(dIn, rad)) {
            double[] sw = dOut;
            dOut = dIn;
            dIn = sw;
         }

         for (int k = 0; k < 3; k++) {
            if (k != 2 || i % 2 != 1) {
               double[] d = k == 0 ? (double[])dOut.clone() : (k == 1 ? (double[])dIn.clone() : rotY(tan, sway * 0.6));
               d[1] += k == 2 ? 0.95 : 0.55;
               d = Prim.unit(d);
               double lk = k == 2 ? 0.78 : 1.0;
               double[] sd = Prim.unit(Prim.cross(d, up));
               double[] n = Prim.unit(Prim.cross(sd, d));
               if (n[1] < 0.0) {
                  n = new double[]{-n[0], -n[1], -n[2]};
               }

               double off = k == 0 ? 0.004 : (k == 1 ? -0.004 : 0.0);
               double[] base = Prim.add(c, rad[0] * off, 0.002, rad[2] * off);
               double ll = len * lk;
               double mid = 0.42 * ll;
               double w2 = wid * lk * 0.5;
               double fold = 0.26 * wid * lk;
               double[] rib = Prim.add(base, d[0] * mid, d[1] * mid, d[2] * mid);
               double[] l = Prim.add(rib, sd[0] * w2 + n[0] * fold, sd[1] * w2 + n[1] * fold, sd[2] * w2 + n[2] * fold);
               double[] r = Prim.add(rib, -sd[0] * w2 + n[0] * fold, -sd[1] * w2 + n[1] * fold, -sd[2] * w2 + n[2] * fold);
               double[] tip = Prim.add(base, d[0] * ll, d[1] * ll, d[2] * ll);
               double g = (side + s) / 2.0 + k * 0.03;
               int cb = GemKit.metal(leaf, g, 0.05, t, 0.1);
               int cm = GemKit.metal(leaf, g, 0.5, t, 0.1);
               int ct = GemKit.metal(leaf, g, 0.95, t, 0.1);
               b.quad(base, l, tip, r, cb, Col.shade(cm, 0.86), ct, Col.shade(cm, 1.12));
            }
         }

         if (i % 4 == 3) {
            double pulse = b.animate ? 0.9 + 0.1 * Math.sin(t * 2.0 + i + side) : 1.0;
            Prim.sphere(b, c[0] - rad[0] * 0.016, c[1] + 0.01, c[2] - rad[2] * 0.016, 0.0125, 5, 3, (u, v) -> Col.shade(accent, pulse * (0.75 + 0.45 * v)));
         }
      }
   }
}
