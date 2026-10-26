package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class RotatingDiamond implements Cosmetic {
   private static final int SIDES = 6;
   private final TargetParams p;

   public RotatingDiamond() {
      this(TargetParams.defaults());
   }

   public RotatingDiamond(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double k = this.p.bulk();
      double r = 0.16 * k;
      double up = 0.3 * k;
      double down = 0.4 * k;
      double bob = b.animate ? 0.045 * k * Math.sin(t * 2.1) : 0.0;
      double home = this.p.hh() + 0.12 * k + down;
      double y0 = home + bob + (1.0 - intro) * 0.9 * k;
      double turn = (Math.PI * 2) * this.p.phase();
      double vis = intro;
      b.push();
      b.translate(0.0, y0, 0.0);
      b.rotateZ(b.animate ? 0.1 * Math.sin(t * 1.4) : 0.0);
      this.light(b, k, r, down, y0, vis);
      b.push();
      b.rotateY(turn);
      this.outer(b, r, up, down, vis);
      b.pop();
      b.push();
      b.rotateY(-turn * 1.7 + 0.5);
      this.core(b, r * 0.46, up * 0.5, down * 0.5, vis);
      b.pop();
      this.sparks(b, t, k, r, up, down, vis);
      b.pop();
   }

   private static double[][] ring(double r, double phase) {
      double[][] v = new double[6][];

      for (int i = 0; i < 6; i++) {
         double a = phase + (Math.PI * 2) * i / 6.0;
         v[i] = Prim.P(r * Math.cos(a), 0.0, r * Math.sin(a));
      }

      return v;
   }

   private void outer(Builder b, double r, double up, double down, double vis) {
      double[][] v = ring(r, 0.0);
      double[] top = Prim.P(0.0, up, 0.0);
      double[] bot = Prim.P(0.0, -down, 0.0);
      int cUp = b.c1;
      int cDn = Col.mix(b.c2, b.c1, 0.3);

      for (int i = 0; i < 6; i++) {
         int j = (i + 1) % 6;
         this.face(b, v[i], v[j], top, cUp, Col.tint(cUp, 0.55), i, vis, false);
         this.face(b, v[j], v[i], bot, cDn, Col.tint(cDn, 0.5), i, vis, true);
      }

      int edge = Col.withA(Col.tint(b.c1, 0.75), 0.92 * vis);
      int edgeLow = Col.withA(Col.tint(cDn, 0.65), 0.85 * vis);
      double e = 0.0065 * this.p.bulk();

      for (int i = 0; i < 6; i++) {
         int j = (i + 1) % 6;
         TargetKit.beam(b, v[i], top, e, edge, Col.withA(edge, 0.98 * vis), false);
         TargetKit.beam(b, v[i], bot, e, edgeLow, Col.withA(edgeLow, 0.98 * vis), false);
         TargetKit.beam(b, v[i], v[j], e * 1.2, edge, edge, false);
      }
   }

   private void face(Builder b, double[] a, double[] c, double[] apex, int base, int tip, int i, double vis, boolean lower) {
      double[] centroid = Prim.P((a[0] + c[0] + apex[0]) / 3.0, (a[1] + c[1] + apex[1]) / 3.0, (a[2] + c[2] + apex[2]) / 3.0);
      double[] n = Prim.cross(Prim.sub(c, a), Prim.sub(apex, a));
      double glint = Math.pow(b.facing(centroid, n), 6.0);
      double lit = 0.68 + 0.3 * ((i * 5 + (lower ? 2 : 0)) % 6) / 5.0;
      int body = Col.shade(Col.mix(base, tip, 0.15), lit);
      body = Col.mix(body, Col.tint(base, 0.85), 0.55 * glint);
      double alpha = (0.7 + 0.22 * glint) * vis;
      int cb = Col.withA(body, alpha);
      int ct = Col.withA(Col.mix(body, tip, 0.65), Math.min(1.0, alpha + 0.12));
      b.tri(a, c, apex, cb, cb, ct);
   }

   private void core(Builder b, double r, double up, double down, double vis) {
      double[][] v = new double[4][];

      for (int i = 0; i < 4; i++) {
         double a = (Math.PI * 2) * i / 4.0;
         v[i] = Prim.P(r * Math.cos(a), 0.0, r * Math.sin(a));
      }

      double[] top = Prim.P(0.0, up, 0.0);
      double[] bot = Prim.P(0.0, -down, 0.0);
      int hi = Col.withA(Col.tint(b.c1, 0.85), 0.95 * vis);
      int lo = Col.withA(Col.tint(b.c2, 0.75), 0.9 * vis);

      for (int i = 0; i < 4; i++) {
         int j = i + 1 & 3;
         double f = 0.75 + 0.25 * ((i & 1) == 0 ? 1.0 : 0.6);
         b.tri(v[i], v[j], top, Col.shade(hi, f), Col.shade(hi, f), Col.tint(hi, 0.4));
         b.tri(v[j], v[i], bot, Col.shade(lo, f), Col.shade(lo, f), Col.tint(lo, 0.4));
      }
   }

   private void light(Builder b, double k, double r, double down, double y0, double vis) {
      double top = -down;
      double bottom = -(y0 - this.p.hh()) + 0.02;
      if (!(bottom > top - 0.05)) {
         int c0 = Col.withA(Col.tint(b.c1, 0.4), 0.3 * vis);
         int c1 = Col.withA(b.c2, 0.0);
         double w0 = r * 0.1;
         double w1 = r * 0.95;
         b.quad(Prim.P(-w0, top, 0.0), Prim.P(w0, top, 0.0), Prim.P(w1, bottom, 0.0), Prim.P(-w1, bottom, 0.0), c0, c0, c1, c1);
         b.quad(Prim.P(0.0, top, -w0), Prim.P(0.0, top, w0), Prim.P(0.0, bottom, w1), Prim.P(0.0, bottom, -w1), c0, c0, c1, c1);
      }
   }

   private void sparks(Builder b, double t, double k, double r, double up, double down, double vis) {
      for (int i = 0; i < 4; i++) {
         double a = (Math.PI * 2) * (i / 4.0 + 0.11 * (b.animate ? t : 0.0) * this.p.dir()) + i * 0.7;
         double rad = r * 2.15;
         double y = (i % 2 == 0 ? 0.55 : -0.45) * (i % 2 == 0 ? up : down) + (b.animate ? 0.05 * k * Math.sin(t * 1.7 + i * 1.9) : 0.0);
         double s = 0.032 * k * (0.7 + 0.5 * TargetKit.wave(b, 3.1, i * 1.7));
         double x = rad * Math.cos(a);
         double z = rad * Math.sin(a);
         int c = Col.withA(Col.tint(i % 2 == 0 ? b.c1 : b.c2, 0.7), (0.55 + 0.4 * TargetKit.wave(b, 3.1, i * 1.7)) * vis);
         b.quad(Prim.P(x - s, y, z), Prim.P(x, y + s * 1.5, z), Prim.P(x + s, y, z), Prim.P(x, y - s * 1.5, z), c);
         b.quad(Prim.P(x, y, z - s), Prim.P(x, y + s * 1.5, z), Prim.P(x, y, z + s), Prim.P(x, y - s * 1.5, z), c);
      }
   }
}
