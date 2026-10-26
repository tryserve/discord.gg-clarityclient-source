package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class HexShield implements Cosmetic {
   static final int MAX_CELLS = 92;
   private static final double SQRT3 = Math.sqrt(3.0);
   private static final double INSET = 0.12;
   private static final double BARREL = 0.3;
   private static final double SEAM_HALF = 0.011;
   private static final int CAP_SEGS = 12;
   private final TargetParams p;

   public HexShield() {
      this(TargetParams.defaults());
   }

   public HexShield(TargetParams p) {
      this.p = p;
   }

   static HexShield.Layout layout(double r0, double h) {
      double circ = (Math.PI * 2) * r0;
      double w = Math.max(0.26, Math.sqrt(circ * h / 79.672));

      for (int guard = 0; guard < 60; guard++) {
         int n = Math.max(8, 2 * (int)Math.round(circ / w / 2.0));
         double cw = circ / n;
         double s = cw / SQRT3;
         int rows = Math.max(3, (int)Math.ceil(h / (1.5 * s)));
         if (n * rows <= 96) {
            return new HexShield.Layout(n, rows, cw);
         }

         w *= 1.06;
      }

      return new HexShield.Layout(8, 3, circ / 8.0);
   }

   private static double profile(double yn) {
      double a = Math.min(1.0, Math.abs(yn));
      return 1.0 - 0.3 * a * a * a;
   }

   @Override
   public void build(Builder b) {
      if (!b.animate) {
         double t = 0.0;
      }

      double intro = this.p.intro();
      double r0 = this.p.hw() + 0.3;
      double hs = this.p.hh() + 0.15;
      HexShield.Layout L = layout(r0, hs * 2.0);
      int n = L.columns();
      int rows = L.rows();
      double cw = L.cell();
      double s = cw / SQRT3;
      double rowStep = 2.0 * hs / rows;
      double fy = rowStep / (1.5 * s);
      HexShield.Field f = new HexShield.Field(b, r0, hs, intro);
      b.push();
      b.scaleLocal(0.78 + 0.22 * intro);
      b.rotateY((Math.PI * 2) * this.p.phase() * 0.35);
      double[] hx = new double[6];
      double[] hy = new double[6];

      for (int k = 0; k < 6; k++) {
         double a = (Math.PI / 2) + k * Math.PI / 3.0;
         hx[k] = s * Math.cos(a);
         hy[k] = s * Math.sin(a) * fy;
      }

      for (int j = 0; j < rows; j++) {
         double yc = -hs + (j + 0.5) * rowStep;

         for (int i = 0; i < n; i++) {
            double xc = (i + 0.5 * (j & 1)) * cw;
            int id = j * n + i;
            double[][] v = new double[6][];
            double[][] vi = new double[6][];

            for (int k = 0; k < 6; k++) {
               v[k] = wall(f, xc + hx[k], yc + hy[k], r0, hs, 1.004);
               vi[k] = wall(f, xc + hx[k] * 0.88, yc + hy[k] * 0.88, r0, hs, 1.0);
            }

            double[] centre = wall(f, xc, yc, r0, hs, 1.0);
            HexShield.Sample sc = f.sample(centre, id);
            int[] col = new int[6];

            for (int k = 0; k < 6; k++) {
               col[k] = f.fill(f.sample(vi[k], id), sc);
            }

            b.quad(vi[0], vi[1], vi[2], vi[3], col[0], col[1], col[2], col[3]);
            b.quad(vi[3], vi[4], vi[5], vi[0], col[3], col[4], col[5], col[0]);
            this.seam(b, f, v[0], v[1]);
            this.seam(b, f, v[5], v[0]);
            this.seam(b, f, v[4], v[5]);
            if (j == 0) {
               this.seam(b, f, v[2], v[3]);
               this.seam(b, f, v[3], v[4]);
            }
         }
      }

      this.cap(b, f, hs, r0);
      this.cap(b, f, -hs, r0);
      b.pop();
   }

   private static double[] wall(HexShield.Field f, double x, double y, double r0, double hs, double lift) {
      double yy = Math.max(-hs, Math.min(hs, y));
      double th = x / r0;
      double r = r0 * profile(yy / hs) * lift;
      return Prim.P(r * Math.cos(th), yy, r * Math.sin(th));
   }

   private void seam(Builder b, HexShield.Field f, double[] a, double[] c) {
      double[] mid = Prim.P((a[0] + c[0]) * 0.5, 0.0, (a[2] + c[2]) * 0.5);
      double[] along = Prim.sub(c, a);
      double[] side = Prim.cross(Prim.unit(mid), along);
      double sl = Math.sqrt(Prim.dot(side, side));
      if (!(sl < 1.0E-9)) {
         double k = 0.011 / sl;
         double[] sv = new double[]{side[0] * k, side[1] * k, side[2] * k};
         HexShield.Sample sa = f.sample(a, -1);
         HexShield.Sample sb = f.sample(c, -1);
         int ca = f.seam(sa);
         int cb = f.seam(sb);
         b.quad(Prim.add(a, sv[0], sv[1], sv[2]), Prim.add(a, -sv[0], -sv[1], -sv[2]), Prim.add(c, -sv[0], -sv[1], -sv[2]), Prim.add(c, sv[0], sv[1], sv[2]), ca, ca, cb, cb);
      }
   }

   private void cap(Builder b, HexShield.Field f, double y, double r0) {
      double r = r0 * profile(1.0) * 1.004;
      double[] rimPrev = null;
      HexShield.Sample sCentre = f.sample(Prim.P(0.0, y, 0.0), -1);
      int cc = f.capFill(sCentre, true);

      for (int i = 0; i <= 12; i++) {
         double a = (Math.PI * 2) * i / 12.0;
         double[] rim = Prim.P(r * Math.cos(a), y, r * Math.sin(a));
         if (rimPrev != null) {
            HexShield.Sample sr = f.sample(rim, -1);
            HexShield.Sample sp = f.sample(rimPrev, -1);
            b.tri(Prim.P(0.0, y, 0.0), rimPrev, rim, cc, f.capFill(sp, false), f.capFill(sr, false));
            this.seam(b, f, rimPrev, rim);
         }

         rimPrev = rim;
      }
   }

   private static final class Field {
      final int c1;
      final int c2;
      final double t;
      final double scale;
      final double hs;
      final double inside;
      final double intro;
      final boolean animate;
      final double[] cam;

      Field(Builder b, double r0, double hs, double intro) {
         this.c1 = b.c1;
         this.c2 = b.c2;
         this.animate = b.animate;
         this.t = this.animate ? b.time : 0.0;
         this.scale = b.scale();
         this.hs = hs;
         this.intro = intro;
         this.cam = b.viewLocal();
         this.inside = this.cam != null && Math.hypot(this.cam[0], this.cam[2]) < r0 * 0.98 ? 0.55 : 1.0;
      }

      HexShield.Sample sample(double[] pos, int cell) {
         HexShield.Sample s = new HexShield.Sample();
         s.id = cell;
         s.yn = pos[1] / this.hs;
         double len = Math.hypot(pos[0], pos[2]);
         double nx = len < 1.0E-9 ? 1.0 : pos[0] / len;
         double nz = len < 1.0E-9 ? 0.0 : pos[2] / len;
         if (this.cam == null) {
            s.fres = 0.5;
            s.back = false;
            s.near = 1.0;
         } else {
            double dx = this.cam[0] - pos[0];
            double dy = this.cam[1] - pos[1];
            double dz = this.cam[2] - pos[2];
            double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double c = dl < 1.0E-9 ? 0.5 : (dx * nx + dz * nz) / dl;
            s.fres = 1.0 - Math.abs(c);
            s.back = c < 0.0;
            s.near = TargetKit.near(this.cam, pos[0], pos[1], pos[2], this.scale);
         }

         if (this.animate) {
            double yb = -this.hs + 2.0 * this.hs * (this.t * 0.32 % 1.0);
            double d = (pos[1] - yb) / (0.16 * this.hs + 0.08);
            s.scan = Math.exp(-d * d);
            double ang = Math.atan2(pos[2], pos[0]);
            double behind = TargetKit.frac(this.t * 0.28 - ang / (Math.PI * 2));
            s.sweep = Math.exp(-behind / 0.12);
            if (cell >= 0) {
               double tw = 0.5 + 0.5 * Math.sin(this.t * (0.9 + 1.6 * Prim.rnd(cell * 7 + 2)) + 6.283 * Prim.rnd(cell * 7 + 3));
               s.twinkle = Math.pow(tw, 9.0);
            }
         }

         return s;
      }

      private int body(HexShield.Sample s, double tintAmount) {
         return Col.tint(Col.mix(this.c2, this.c1, 0.5 + 0.5 * s.yn), tintAmount);
      }

      int fill(HexShield.Sample s, HexShield.Sample centre) {
         int base = this.body(s, 0.1 + 0.3 * s.fres);
         double var = 0.85 + 0.3 * Prim.rnd(centre.id * 7 + 1);
         double a = (0.05 + 0.2 * s.fres * s.fres + 0.22 * s.scan + 0.24 * s.sweep + 0.4 * centre.twinkle) * var;
         a *= (s.back ? 0.6 : 1.0) * this.inside * s.near * this.intro;
         return Col.withA(Col.mix(base, Col.tint(this.c1, 0.6), Math.min(1.0, s.sweep * 0.5 + s.scan * 0.4)), Math.min(0.8, a));
      }

      int seam(HexShield.Sample s) {
         int base = this.body(s, 0.35 + 0.3 * s.fres);
         double a = 0.34 + 0.4 * Math.pow(s.fres, 1.5) + 0.4 * s.scan + 0.35 * s.sweep;
         a *= (s.back ? 0.65 : 1.0) * this.inside * s.near * this.intro;
         return Col.withA(base, Math.min(0.95, a));
      }

      int capFill(HexShield.Sample s, boolean centre) {
         int base = this.body(s, centre ? 0.35 : 0.15);
         double a = (centre ? 0.05 : 0.2) + 0.25 * s.scan * (Math.abs(s.yn) > 0.9 ? 0.0 : 1.0);
         a *= this.inside * s.near * this.intro;
         return Col.withA(base, Math.min(0.5, a));
      }
   }

   record Layout(int columns, int rows, double cell) {
      int cells() {
         return this.columns * this.rows;
      }
   }

   private static final class Sample {
      int id;
      double yn;
      double fres;
      boolean back;
      double scan;
      double sweep;
      double twinkle;
      double near;
   }
}
