package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class HologramScan implements Cosmetic {
   private static final double PASS_RATE = 0.6;
   private final TargetParams p;

   public HologramScan() {
      this(TargetParams.defaults());
   }

   public HologramScan(TargetParams p) {
      this.p = p;
   }

   static double scanFraction(double phase) {
      double c = TargetKit.frac(phase * 0.6);
      double tri = 1.0 - Math.abs(2.0 * c - 1.0);
      return 0.5 - 0.5 * Math.cos(Math.PI * tri);
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
      double hh = this.p.hh();
      double H = 2.0 * hh;
      double gx = this.p.hw() * (0.55 + 0.45 * intro);
      double vis = 0.3 + 0.7 * intro;
      if (b.animate) {
         vis *= 0.94 + 0.06 * Math.sin(t * 31.0);
         if (TargetKit.frac(t * 0.29 + 0.13) < 0.025) {
            vis *= 0.55;
         }
      }

      double frac = scanFraction(this.p.phase());
      double sY = -hh + H * frac;
      double c = TargetKit.frac(this.p.phase() * 0.6);
      double travel = (c < 0.5 ? 1.0 : -1.0) * this.p.dir();
      int scan = Col.tint(b.c2, 0.55);
      double[] cam = b.viewLocal();
      double out = 0.006;
      double gxo = gx + out;
      double[][] corner = new double[][]{{gxo, gxo}, {-gxo, gxo}, {-gxo, -gxo}, {gxo, -gxo}};
      double band = 0.02 * bulk;
      double glow = 0.085 * bulk;
      double wash = 0.3 * H;
      double behind = -travel;

      for (int i = 0; i < 4; i++) {
         double[] a = corner[i];
         double[] z = corner[i + 1 & 3];
         double nr = TargetKit.near(cam, (a[0] + z[0]) * 0.5, sY, (a[1] + z[1]) * 0.5, scale);
         int bright = Col.withA(Col.mix(Col.tint(b.c1, 0.5), Col.tint(scan, 0.35), 0.8), 0.95 * vis * nr);
         b.quad(Prim.P(a[0], sY - band, a[1]), Prim.P(z[0], sY - band, z[1]), Prim.P(z[0], sY + band, z[1]), Prim.P(a[0], sY + band, a[1]), bright);
         int g0 = Col.withA(scan, 0.0);
         int g1 = Col.withA(scan, 0.45 * vis * nr);
         b.quad(Prim.P(a[0], sY + band, a[1]), Prim.P(z[0], sY + band, z[1]), Prim.P(z[0], sY + glow, z[1]), Prim.P(a[0], sY + glow, a[1]), g1, g1, g0, g0);
         b.quad(Prim.P(a[0], sY - band, a[1]), Prim.P(z[0], sY - band, z[1]), Prim.P(z[0], sY - glow, z[1]), Prim.P(a[0], sY - glow, a[1]), g1, g1, g0, g0);
         double yEnd = clampY(sY + behind * wash, hh);
         double ys = sY + behind * band;
         if (Math.abs(yEnd - ys) > 1.0E-4) {
            double f = Math.min(1.0, Math.abs(yEnd - ys) / wash);
            int w0 = Col.withA(scan, 0.26 * vis * nr);
            int w1 = Col.withA(scan, 0.26 * (1.0 - f) * vis * nr);
            b.quad(Prim.P(a[0], ys, a[1]), Prim.P(z[0], ys, z[1]), Prim.P(z[0], yEnd, z[1]), Prim.P(a[0], yEnd, a[1]), w0, w0, w1, w1);
         }
      }

      int fc = Col.withA(scan, 0.05 * vis);
      int fe = Col.withA(scan, 0.17 * vis);
      double[] ctr = Prim.P(0.0, sY, 0.0);

      for (int i = 0; i < 4; i++) {
         double[] a = corner[i];
         double[] z = corner[i + 1 & 3];
         b.tri(ctr, Prim.P(a[0], sY, a[1]), Prim.P(z[0], sY, z[1]), fc, fe, fe);
      }

      int gl = Col.withA(Col.tint(scan, 0.2), 0.34 * vis);
      double gw = 0.006 * bulk;

      for (int k = 0; k < 5; k++) {
         double s = -gx + 2.0 * gx * k / 4.0;
         b.quad(Prim.P(s - gw, sY, -gx), Prim.P(s + gw, sY, -gx), Prim.P(s + gw, sY, gx), Prim.P(s - gw, sY, gx), gl);
         b.quad(Prim.P(-gx, sY, s - gw), Prim.P(gx, sY, s - gw), Prim.P(gx, sY, s + gw), Prim.P(-gx, sY, s + gw), gl);
      }

      double tab = 0.1 * bulk;
      int tc = Col.withA(Col.tint(scan, 0.3), 0.9 * vis);

      for (int i = 0; i < 4; i++) {
         double sx = corner[i][0] > 0.0 ? 1.0 : -1.0;
         double sz = corner[i][1] > 0.0 ? 1.0 : -1.0;
         double cx = sx * gxo;
         double cz = sz * gxo;
         double w = 0.012 * bulk;
         b.quad(Prim.P(cx, sY, cz), Prim.P(cx + sx * tab, sY, cz), Prim.P(cx + sx * tab, sY, cz + sz * w * 2.0), Prim.P(cx, sY, cz + sz * w * 2.0), tc);
      }
   }

   private static double clampY(double y, double hh) {
      return y < -hh ? -hh : (y > hh ? hh : y);
   }
}
