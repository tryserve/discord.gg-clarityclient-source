package need.clarity.modules.visual.cosmetics.hats;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class RunicHalo implements Cosmetic {
   static final double Y = 0.3;
   static final double R = 0.31;
   static final double GW = 0.07;
   static final double GH = 0.115;
   static final double STROKE = 0.013;
   static final int N = 12;
   private static final double TAU = Math.PI * 2;
   private static final double[][][] RUNES = new double[][][]{
      {{0.3, 0.0, 0.3, 1.0}, {0.3, 0.72, 0.85, 1.0}, {0.3, 0.42, 0.85, 0.7}},
      {{0.2, 0.0, 0.2, 1.0}, {0.2, 1.0, 0.78, 0.74}, {0.78, 0.74, 0.78, 0.0}},
      {{0.25, 0.0, 0.25, 1.0}, {0.25, 0.78, 0.78, 0.52}, {0.78, 0.52, 0.25, 0.26}},
      {{0.25, 0.0, 0.25, 1.0}, {0.25, 1.0, 0.8, 0.8}, {0.25, 0.66, 0.8, 0.46}},
      {{0.25, 0.0, 0.25, 1.0}, {0.25, 1.0, 0.78, 0.76}, {0.78, 0.76, 0.25, 0.52}, {0.25, 0.52, 0.82, 0.0}},
      {{0.72, 0.92, 0.25, 0.5}, {0.25, 0.5, 0.72, 0.08}},
      {{0.15, 0.0, 0.85, 1.0}, {0.15, 1.0, 0.85, 0.0}},
      {{0.25, 0.0, 0.25, 1.0}, {0.25, 1.0, 0.78, 0.76}, {0.78, 0.76, 0.25, 0.52}},
      {{0.2, 0.0, 0.2, 1.0}, {0.8, 0.0, 0.8, 1.0}, {0.2, 0.64, 0.8, 0.36}},
      {{0.72, 1.0, 0.28, 0.66}, {0.28, 0.66, 0.72, 0.34}, {0.72, 0.34, 0.28, 0.0}},
      {{0.5, 0.0, 0.5, 1.0}, {0.5, 1.0, 0.15, 0.7}, {0.5, 1.0, 0.85, 0.7}},
      {{0.25, 0.0, 0.25, 1.0}, {0.25, 1.0, 0.72, 0.76}, {0.72, 0.76, 0.25, 0.52}, {0.25, 0.52, 0.72, 0.26}, {0.72, 0.26, 0.25, 0.0}}
   };
   private final HatParams p;

   public RunicHalo() {
      this(HatParams.defaults());
   }

   public RunicHalo(HatParams p) {
      this.p = p;
   }

   private static void stroke(Builder b, double[] s, int cLo, int cHi) {
      double u0 = (s[0] - 0.5) * 0.07;
      double v0 = (s[1] - 0.5) * 0.115;
      double u1 = (s[2] - 0.5) * 0.07;
      double v1 = (s[3] - 0.5) * 0.115;
      double du = u1 - u0;
      double dv = v1 - v0;
      double len = Math.sqrt(du * du + dv * dv);
      double e = 0.0065 / len;
      b.bar(Prim.P(u0 - du * e, v0 - dv * e, 0.0), Prim.P(u1 + du * e, v1 + dv * e, 0.0), 0.013, 0.013, Col.mix(cLo, cHi, s[1]), Col.mix(cLo, cHi, s[3]));
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double bob = this.p.bob() && b.animate ? Math.sin(t * 1.7) * 0.014 : 0.0;
      double turn = b.animate ? t * this.p.spin() * (Math.PI * 2) : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      b.push();
      b.translate(0.0, 0.3 + bob, 0.0);
      b.rotateY(turn);

      for (int side = -1; side <= 1; side += 2) {
         double y0 = side * 0.0815 - 0.006;
         Prim.cylinder(b, y0, y0 + 0.012, 0.31, 0.31, 40, (u, v) -> {
            double band = Math.pow(Math.max(0.0, Math.cos((u - t * 0.24) * (Math.PI * 2))), 8.0);
            return Col.tint(Col.mix(c1, c2, 1.0 - Math.abs(2.0 * u - 1.0)), 0.2 + 0.55 * band);
         }, false, false);
      }

      for (int k = 0; k < 12; k++) {
         double th = k * (Math.PI * 2) / 12.0;
         double lit = b.animate ? Math.pow(Math.max(0.0, Math.cos(th - t * 1.5)), 2.0) : 0.35;
         int base = Col.mix(c1, c2, 1.0 - Math.abs(2.0 * k / 12.0 - 1.0));
         int lo = Col.shade(base, 0.8 + 0.2 * lit);
         int hi = Col.tint(base, 0.25 + 0.55 * lit);
         b.push();
         b.translate(0.31 * Math.cos(th), 0.0, 0.31 * Math.sin(th));
         b.rotateY((-Math.PI / 2) - th);

         for (double[] s : RUNES[k % RUNES.length]) {
            stroke(b, s, lo, hi);
         }

         int pl = Col.tint(base, 0.4);
         double pw = 0.052500000000000005;
         double ph = 0.0736;
         b.quad(
            Prim.P(-pw, -ph, 0.012),
            Prim.P(pw, -ph, 0.012),
            Prim.P(pw, ph, 0.012),
            Prim.P(-pw, ph, 0.012),
            Col.withA(pl, 0.05 + 0.06 * lit),
            Col.withA(pl, 0.05 + 0.06 * lit),
            Col.withA(pl, 0.1 + 0.14 * lit),
            Col.withA(pl, 0.1 + 0.14 * lit)
         );
         b.pop();
         double tm = th + (Math.PI / 12);
         int gem = Col.tint(k % 2 == 0 ? c2 : c1, 0.35);
         b.push();
         b.translate(0.31 * Math.cos(tm), 0.0, 0.31 * Math.sin(tm));
         Prim.Shade gs = (u, v) -> Col.mix(Col.shade(gem, 0.7), Col.tint(gem, 0.6), Prim.sm(v));
         Prim.spike(b, Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, 0.04, 0.0), 0.017, 4, gs);
         Prim.spike(b, Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, -0.04, 0.0), 0.017, 4, gs);
         b.pop();
      }

      Prim.ring(b, -0.0875, 0.29, 0.43, 36, (u, v) -> Col.withA(Col.tint(Col.mix(c1, c2, 1.0 - Math.abs(2.0 * u - 1.0)), 0.4), 0.3 * (1.0 - v) * (1.0 - v)));
      b.pop();
   }
}
