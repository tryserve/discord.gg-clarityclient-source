package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Swords implements Cosmetic {
   static final int MAX = 12;
   private static final double BLADE = 0.58;
   private static final double HALF_W = 0.042;
   private static final double HALF_T = 0.011;
   private static final double CENTRE = 0.21;
   private final AuraParams p;

   public Swords() {
      this(AuraParams.defaults());
   }

   public Swords(AuraParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      int n = this.p.items(12);
      int d = this.p.detail(n);
      double t = b.animate ? b.time : 0.0;
      double radius = this.p.radius();
      double dir = this.p.dir();
      double shrink = Math.max(0.7, Math.min(1.0, (Math.PI * 2) * radius / n / 0.75));

      for (int k = 0; k < n; k++) {
         double phi = this.p.angle(k, n);
         double y = 0.05 * Math.sin(t * 1.6 + k * 1.1);
         double roll = 0.12 * Math.sin(t * 1.7 + k * 2.3);
         this.trail(b, phi, y, radius, dir, d, k);
         b.push();
         b.translate(radius * Math.sin(phi), y, radius * Math.cos(phi));
         b.rotateY(phi);
         b.rotateZ(-dir * Math.PI / 2.0 + roll);
         b.scaleLocal(shrink);
         b.translate(0.0, -0.21, 0.0);
         b.rotateY(0.8);
         double f = AuraKit.nearOrigin(b);
         if (f > 0.02) {
            sword(b, f, AuraKit.wave(b, 2.6, k * 1.4));
         }

         b.pop();
      }
   }

   private void trail(Builder b, double phi, double y, double radius, double dir, int d, int k) {
      int segs = d == 2 ? 4 : (d == 1 ? 3 : 3);
      double r = Math.max(radius, 0.3);
      double start = phi - dir * 0.24 / r;
      double arc = 0.6;
      double[][] spine = new double[segs + 1][];

      for (int i = 0; i <= segs; i++) {
         double a = start - dir * arc * i / segs;
         spine[i] = Prim.P(radius * Math.sin(a), y, radius * Math.cos(a));
      }

      int c1 = b.c1;
      int c2 = b.c2;
      Prim.Shade fade = (u, v) -> Col.withA(Col.mix(c2, c1, v), 0.34 * Math.pow(1.0 - v, 1.5));
      Prim.ribbon(b, spine, 0.08, 0.0, Prim.P(0.0, 1.0, 0.0), fade);
      if (d >= 1) {
         Prim.ribbon(b, spine, 0.06, 0.0, Prim.P(Math.sin(phi), 0.0, Math.cos(phi)), fade);
      }
   }

   private static void sword(Builder b, double f, double pulse) {
      int c1 = b.c1;
      int c2 = b.c2;
      int steel = Col.mix(Col.dark(c1, 0.35), Col.tint(c2, 0.55), 0.45);
      int hiltCol = Col.mix(c2, -1, 0.15);
      double[] vs = new double[]{0.0, 0.22039999999999998, 0.45239999999999997};
      double[] hw = new double[]{0.042, 0.04074, 0.033600000000000005};
      double[] ht = new double[]{0.011, 0.011, 0.0088};
      double[][][] ring = new double[3][4][];
      int[][] col = new int[3][4];

      for (int i = 0; i < 3; i++) {
         ring[i][0] = Prim.P(hw[i], vs[i], 0.0);
         ring[i][1] = Prim.P(0.0, vs[i], ht[i]);
         ring[i][2] = Prim.P(-hw[i], vs[i], 0.0);
         ring[i][3] = Prim.P(0.0, vs[i], -ht[i]);
         double g = vs[i] / 0.58;
         int face = Col.mix(Col.dark(steel, 0.3), steel, 0.4 + 0.6 * g);
         int edge = Col.tint(Col.mix(steel, c2, 0.5 * g), 0.55);
         col[i][0] = AuraKit.fade(edge, f);
         col[i][1] = AuraKit.fade(face, f);
         col[i][2] = AuraKit.fade(edge, f);
         col[i][3] = AuraKit.fade(Col.shade(face, 0.86), f);
      }

      for (int i = 0; i < 2; i++) {
         for (int s = 0; s < 4; s++) {
            int s1 = s + 1 & 3;
            b.quad(ring[i][s], ring[i][s1], ring[i + 1][s1], ring[i + 1][s], col[i][s], col[i][s1], col[i + 1][s1], col[i + 1][s]);
         }
      }

      double[] tip = Prim.P(0.0, 0.58, 0.0);
      int tipCol = AuraKit.fade(Col.tint(c2, 0.85), f);

      for (int s = 0; s < 4; s++) {
         int s1 = s + 1 & 3;
         b.tri(ring[2][s], ring[2][s1], tip, col[2][s], col[2][s1], tipCol);
      }

      int glow = AuraKit.fade(Col.mix(c2, -1, 0.25 + 0.5 * pulse), f);
      int glowLo = AuraKit.fade(Col.mix(c1, c2, 0.5 + 0.4 * pulse), f);

      for (int side = -1; side <= 1; side += 2) {
         double w = side * 0.0135;
         b.quad(
            Prim.P(-0.008, 0.028999999999999998, w), Prim.P(0.008, 0.028999999999999998, w), Prim.P(0.005, 0.41759999999999997, w), Prim.P(-0.005, 0.41759999999999997, w), glowLo, glowLo, glow, glow
         );
      }

      Prim.box(b, -0.095, -0.018, -0.014, 0.095, 0.005, 0.014, AuraKit.fade(Col.dark(hiltCol, 0.25), f), AuraKit.fade(hiltCol, f));
      int grip = Col.dark(c1, 0.55);
      Prim.taperedTube(b, Prim.P(0.0, -0.018, 0.0), Prim.P(0.0, -0.11, 0.0), 0.013, 0.013, 4, AuraKit.fade((u, v) -> Col.mix(grip, Col.dark(grip, 0.4), (int)(v * 6.0) % 2), f), false);
      Prim.sphere(b, 0.0, -0.128, 0.0, 0.022, 5, 3, AuraKit.fade((u, v) -> Col.mix(Col.dark(hiltCol, 0.3), Col.tint(hiltCol, 0.4), v), f));
   }
}
