package need.clarity.modules.visual.wings;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.CosmeticSink;

public final class WingBuilder extends Builder {
   private double bx;
   private double by;
   private double bz;
   private double sx;
   private double sz;
   private double fx;
   private double fz;
   private double scale;
   private double cr = 1.0;
   private double sr = 0.0;
   private double cf = 1.0;
   private double sf = 0.0;
   public int side;
   private static final double[] FEATHER_PROFILE = new double[]{0.55, 0.92, 1.0, 0.72, 0.0};

   public WingBuilder(CosmeticSink sink, int c1, int c2, double opacity, double time, boolean animate) {
      super(sink, c1, c2, opacity, time, animate);
   }

   public void frame(double[] base, double sideX, double sideZ, double fwdX, double fwdZ, double scale, double roll, double fold, int side) {
      this.bx = base[0];
      this.by = base[1];
      this.bz = base[2];
      this.sx = sideX;
      this.sz = sideZ;
      this.fx = fwdX;
      this.fz = fwdZ;
      this.scale = scale;
      this.side = side;
      this.cr = Math.cos(roll);
      this.sr = Math.sin(roll);
      this.cf = Math.cos(fold);
      this.sf = Math.sin(fold);
   }

   @Override
   public double[] map(double[] p) {
      double u = p[0];
      double v = p[1];
      double w = p[2];
      double u1 = u * this.cr - v * this.sr;
      double v1 = u * this.sr + v * this.cr;
      double u2 = u1 * this.cf;
      double w2 = w + u1 * this.sf;
      return new double[]{this.bx + (this.sx * u2 - this.fx * w2) * this.scale, this.by + v1 * this.scale, this.bz + (this.sz * u2 - this.fz * w2) * this.scale};
   }

   public void feather(double[] root, double ang, double len, double wd, double curl, double sweep, int cRoot, int cMid, int cTip, boolean rachis) {
      int n = FEATHER_PROFILE.length - 1;
      double seg = len / n;
      double u = root[0];
      double v = root[1];
      double a = ang;
      double[] pl = null;
      double[] pr = null;
      int pc = cRoot;
      double[] tip = root;

      for (int i = 0; i <= n; i++) {
         double t = (double)i / n;
         double w = root[2] + sweep * t * t;
         double nu = -Math.sin(a);
         double nv = Math.cos(a);
         double hw = wd * FEATHER_PROFILE[i] * 0.5;
         double[] l = WC.P(u + nu * hw, v + nv * hw, w);
         double[] r = WC.P(u - nu * hw, v - nv * hw, w);
         int col = t < 0.5 ? WC.mix(cRoot, cMid, t * 2.0) : WC.mix(cMid, cTip, (t - 0.5) * 2.0);
         if (i > 0) {
            this.quad(pl, pr, r, l, pc, pc, col, col);
         }

         pl = l;
         pr = r;
         pc = col;
         tip = WC.P(u, v, w);
         u += Math.cos(a) * seg;
         v += Math.sin(a) * seg;
         a += curl * seg;
      }

      if (rachis) {
         double[] s0 = WC.P(root[0], root[1], root[2] + 0.004);
         this.bar(s0, WC.P(tip[0], tip[1], tip[2] + 0.004), wd * 0.09, wd * 0.02, WC.tint(cMid, 0.6), WC.alpha(WC.tint(cTip, 0.5), 0.8));
      }
   }
}
