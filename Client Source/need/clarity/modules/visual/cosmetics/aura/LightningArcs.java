package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class LightningArcs implements Cosmetic {
   static final int MAX = 12;
   static final int LODS = 4;
   private static final int[] MAIN = new int[]{16, 13, 10, 7};
   private static final int[] FORKS = new int[]{3, 2, 2, 1};
   private static final int[] FORK_SEGS = new int[]{6, 6, 5, 4};
   private static final double GLOW = 0.095;
   private static final double CORE = 0.022;
   private static final double LIFT = 0.006;
   private static final double DUTY = 0.85;
   private final LightningParams p;

   public LightningArcs() {
      this(LightningParams.defaults());
   }

   public LightningArcs(LightningParams p) {
      this.p = p;
   }

   public static int quadsPerBolt(int lod) {
      return 2 * (MAIN[lod] + FORKS[lod] * FORK_SEGS[lod]) + 4;
   }

   public static int quads(LightningParams p) {
      return p.bolts() * quadsPerBolt(p.lod());
   }

   @Override
   public void build(Builder b) {
      int n = this.p.bolts();
      int lod = this.p.lod();
      double[] cam = b.viewLocal();
      double rate = Math.max(2.0, Math.min(60.0, this.p.rate()));

      for (int k = 0; k < n; k++) {
         int idx;
         double f;
         if (b.animate) {
            double ph = b.time * rate + k * 0.377;
            double fl = Math.floor(ph);
            idx = (int)(fl % 1.0E9);
            f = ph - fl;
         } else {
            idx = k;
            f = 0.16;
         }

         int seed = idx * 7919 + k * 104729 + 17;
         double intensity = intensity(f, seed, b.animate);
         if (!(intensity < 0.04)) {
            this.bolt(b, cam, lod, seed, intensity);
         }
      }
   }

   private static double intensity(double f, int seed, boolean animate) {
      if (!animate) {
         return 1.0;
      }

      if (r(seed, 7) > 0.85) {
         return 0.0;
      }

      double a = f < 0.15 ? Prim.sm(f / 0.15) : Math.pow(1.0 - (f - 0.15) / 0.85, 1.2);
      if (r(seed, 6) < 0.35 && f > 0.5 && f < 0.72) {
         a = Math.max(a, 0.75 * Math.sin(Math.PI * (f - 0.5) / 0.22));
      }

      return Col.clamp01(a);
   }

   private static double r(int seed, int i) {
      return Prim.rnd(seed * 16 + i);
   }

   private void bolt(Builder b, double[] cam, int lod, int seed, double intensity) {
      int S = MAIN[lod];
      boolean wrap = r(seed, 0) < 0.5;
      double reach = this.p.reach();
      double th = (Math.PI * 2) * r(seed, 1);
      double el = Math.asin((2.0 * r(seed, 2) - 1.0) * 0.9);
      double[] n0 = new double[]{Math.cos(el) * Math.sin(th), Math.sin(el), Math.cos(el) * Math.cos(th)};
      double[][] base = new double[S + 1][];
      double len;
      if (wrap) {
         double rs = reach * (0.86 + 0.12 * r(seed, 4));
         double arc = Math.toRadians(55.0 + 55.0 * r(seed, 5));
         double beta = (Math.PI * 2) * r(seed, 3);
         double[] e = Prim.unit(Prim.cross(Prim.P(0.0, 1.0, 0.0), n0));
         double[] nn = Prim.cross(n0, e);
         double[] dt = new double[]{Math.cos(beta) * e[0] + Math.sin(beta) * nn[0], Math.cos(beta) * e[1] + Math.sin(beta) * nn[1], Math.cos(beta) * e[2] + Math.sin(beta) * nn[2]};
         double[] n1 = new double[]{Math.cos(arc) * n0[0] + Math.sin(arc) * dt[0], Math.cos(arc) * n0[1] + Math.sin(arc) * dt[1], Math.cos(arc) * n0[2] + Math.sin(arc) * dt[2]};
         double sa = Math.sin(arc);

         for (int i = 0; i <= S; i++) {
            double t = (double)i / S;
            double w0 = Math.sin((1.0 - t) * arc) / sa;
            double w1 = Math.sin(t * arc) / sa;
            double rad = rs * (1.0 + 0.05 * Math.sin(Math.PI * t));
            base[i] = Prim.P((n0[0] * w0 + n1[0] * w1) * rad, (n0[1] * w0 + n1[1] * w1) * rad, (n0[2] * w0 + n1[2] * w1) * rad);
         }

         len = arc * rs;
      } else {
         double rr = 0.75 + 0.25 * r(seed, 3);
         double[] start = Prim.P(n0[0] * 0.32 * rr, n0[1] * 0.85 * rr, n0[2] * 0.2 * rr);
         double drift = 0.35;
         double[] n1 = Prim.unit(Prim.P(n0[0] + drift * (2.0 * r(seed, 4) - 1.0), n0[1] + drift * (2.0 * r(seed, 5) - 1.0), n0[2] + drift * (2.0 * r(seed, 8) - 1.0)));
         double tip = reach * (0.95 + 0.3 * r(seed, 9));
         double[] end = Prim.P(n1[0] * tip, n1[1] * tip, n1[2] * tip);

         for (int i = 0; i <= S; i++) {
            base[i] = Prim.lerp(start, end, (double)i / S);
         }

         len = Math.sqrt(Prim.dot(Prim.sub(end, start), Prim.sub(end, start)));
      }

      double[][] main = displace(base, 0.13 * len, seed * 31 + 1);
      double[] tipPoint = main[S];
      LightningArcs.Poly mp = poly(main, cam);
      double[] mt = new double[S + 1];
      double[] ma = new double[S + 1];
      double[] mw = new double[S + 1];

      for (int i = 0; i <= S; i++) {
         double t = (double)i / S;
         mt[i] = t;
         mw[i] = wrap ? 0.45 + 0.55 * Math.sin(Math.PI * t) : 1.0 - 0.45 * t;
         ma[i] = wrap ? 0.35 + 0.65 * Math.sin(Math.PI * t) : 1.0 - 0.2 * t;
      }

      this.draw(b, cam, mp, mt, ma, mw, intensity);
      int F = FORKS[lod];
      int FS = FORK_SEGS[lod];

      for (int j = 0; j < F; j++) {
         int fseed = seed * 31 + 100 + j;
         int at = (int)Math.round(S * (0.25 + 0.5 * (j + r(fseed, 0)) / F));
         at = Math.max(1, Math.min(S - 1, at));
         double[] o = main[at];
         double[] tan = Prim.unit(Prim.sub(main[at + 1], main[at - 1]));
         double[][] pp = perp(tan);
         double psi = (Math.PI * 2) * r(fseed, 1);
         double ang = 0.45 + 0.45 * r(fseed, 2);
         double[] pv = new double[]{Math.cos(psi) * pp[0][0] + Math.sin(psi) * pp[1][0], Math.cos(psi) * pp[0][1] + Math.sin(psi) * pp[1][1], Math.cos(psi) * pp[0][2] + Math.sin(psi) * pp[1][2]};
         double[] out = Prim.unit(o);
         double[] dir = Prim.unit(
            Prim.P(
               tan[0] * Math.cos(ang) + pv[0] * Math.sin(ang) + out[0] * 0.35,
               tan[1] * Math.cos(ang) + pv[1] * Math.sin(ang) + out[1] * 0.35,
               tan[2] * Math.cos(ang) + pv[2] * Math.sin(ang) + out[2] * 0.35
            )
         );
         double fl = reach * (0.22 + 0.22 * r(fseed, 3));
         double[][] fb = new double[FS + 1][];

         for (int i = 0; i <= FS; i++) {
            double t = (double)i / FS;
            fb[i] = Prim.P(o[0] + dir[0] * fl * t, o[1] + dir[1] * fl * t, o[2] + dir[2] * fl * t);
         }

         double[][] fp = displace(fb, 0.18 * fl, fseed);
         LightningArcs.Poly fpoly = poly(fp, cam);
         double g0 = mt[at];
         double[] ft = new double[FS + 1];
         double[] fa = new double[FS + 1];
         double[] fw = new double[FS + 1];

         for (int i = 0; i <= FS; i++) {
            double t = (double)i / FS;
            ft[i] = g0 + (1.0 - g0) * t * 0.5;
            fa[i] = 0.85 * (1.0 - t);
            fw[i] = mw[at] * 0.55 * (1.0 - 0.8 * t);
         }

         this.draw(b, cam, fpoly, ft, fa, fw, intensity * 0.8);
      }

      this.flare(b, cam, tipPoint, intensity, Col.mix(b.c1, b.c2, 1.0));
   }

   private static double[][] displace(double[][] base, double amp, int seed) {
      int n = base.length;
      double[][] out = new double[n][];
      out[0] = base[0];
      out[n - 1] = base[n - 1];

      for (int i = 1; i < n - 1; i++) {
         double t = i / (n - 1.0);
         double env = Math.pow(Math.sin(Math.PI * t), 0.6);
         double[] tan = Prim.unit(Prim.sub(base[i + 1], base[i - 1]));
         double[][] pp = perp(tan);
         double zig = i % 2 == 0 ? 1.0 : -1.0;
         double o1 = amp * env * (0.55 * zig * (0.4 + 0.6 * r(seed, 20 + i)) + 0.45 * (2.0 * r(seed, 40 + i) - 1.0));
         double o2 = amp * env * 0.7 * (2.0 * r(seed, 60 + i) - 1.0);
         out[i] = Prim.P(base[i][0] + pp[0][0] * o1 + pp[1][0] * o2, base[i][1] + pp[0][1] * o1 + pp[1][1] * o2, base[i][2] + pp[0][2] * o1 + pp[1][2] * o2);
      }

      return out;
   }

   private static double[][] perp(double[] axis) {
      double[] ref = Math.abs(axis[1]) < 0.9 ? Prim.P(0.0, 1.0, 0.0) : Prim.P(1.0, 0.0, 0.0);
      double[] p1 = Prim.unit(Prim.cross(axis, ref));
      return new double[][]{p1, Prim.cross(axis, p1)};
   }

   private static LightningArcs.Poly poly(double[][] pts, double[] cam) {
      int n = pts.length;
      LightningArcs.Poly q = new LightningArcs.Poly();
      q.pts = pts;
      q.side = new double[n][];
      q.lift = new double[n][];
      double[] prev = null;

      for (int i = 0; i < n; i++) {
         double[] tan = Prim.unit(Prim.sub(pts[Math.min(i + 1, n - 1)], pts[Math.max(i - 1, 0)]));
         double[] toCam = cam == null ? Prim.P(0.0, 0.0, 1.0) : Prim.unit(Prim.sub(cam, pts[i]));
         double[] s = Prim.cross(tan, toCam);
         double sl = Math.sqrt(Prim.dot(s, s));
         if (sl < 1.0E-4) {
            s = prev != null ? prev : Prim.cross(tan, Prim.P(1.0, 0.0, 0.0));
         }

         s = Prim.unit(s);
         prev = s;
         q.side[i] = s;
         q.lift[i] = Prim.P(toCam[0] * 0.006, toCam[1] * 0.006, toCam[2] * 0.006);
      }

      return q;
   }

   private void draw(Builder b, double[] cam, LightningArcs.Poly q, double[] t, double[] alpha, double[] wk, double intensity) {
      int n = q.pts.length;
      double glow = 0.095 * (0.45 + 0.55 * intensity);
      double core = 0.022 * (0.6 + 0.4 * intensity);
      double[][] gl = new double[n][];
      double[][] gr = new double[n][];
      double[][] cl = new double[n][];
      double[][] cr = new double[n][];
      int[] gc = new int[n];
      int[] cc = new int[n];
      double scale = b.scale();

      for (int i = 0; i < n; i++) {
         double[] pt = q.pts[i];
         double[] s = q.side[i];
         double[] lf = q.lift[i];
         double hg = glow * wk[i] * 0.5;
         double hc = core * wk[i] * 0.5;
         gl[i] = Prim.P(pt[0] + s[0] * hg, pt[1] + s[1] * hg, pt[2] + s[2] * hg);
         gr[i] = Prim.P(pt[0] - s[0] * hg, pt[1] - s[1] * hg, pt[2] - s[2] * hg);
         cl[i] = Prim.P(pt[0] + lf[0] + s[0] * hc, pt[1] + lf[1] + s[1] * hc, pt[2] + lf[2] + s[2] * hc);
         cr[i] = Prim.P(pt[0] + lf[0] - s[0] * hc, pt[1] + lf[1] - s[1] * hc, pt[2] + lf[2] - s[2] * hc);
         double f = AuraKit.near(cam, pt[0], pt[1], pt[2], scale);
         int base = Col.mix(b.c1, b.c2, t[i]);
         gc[i] = Col.withA(Col.tint(base, 0.05), Math.min(0.9, 0.55 * intensity * alpha[i] * f));
         cc[i] = Col.withA(Col.tint(base, 0.72), Math.min(1.0, intensity * alpha[i] * f));
      }

      for (int i = 0; i < n - 1; i++) {
         b.quad(gl[i], gr[i], gr[i + 1], gl[i + 1], gc[i], gc[i], gc[i + 1], gc[i + 1]);
         b.quad(cl[i], cr[i], cr[i + 1], cl[i + 1], cc[i], cc[i], cc[i + 1], cc[i + 1]);
      }
   }

   private void flare(Builder b, double[] cam, double[] at, double intensity, int rim) {
      double[] d = cam == null ? Prim.P(0.0, 0.0, 1.0) : Prim.unit(Prim.sub(cam, at));
      double[] rt = Prim.cross(Prim.P(0.0, 1.0, 0.0), d);
      rt = Math.sqrt(Prim.dot(rt, rt)) < 1.0E-4 ? Prim.P(1.0, 0.0, 0.0) : Prim.unit(rt);
      double[] up = Prim.cross(d, rt);
      double f = AuraKit.near(cam, at[0], at[1], at[2], b.scale());
      double sz = 0.11 * (0.5 + 0.5 * intensity);
      double hw = sz * 0.22;
      double[] c = Prim.P(at[0] + d[0] * 0.006 * 2.0, at[1] + d[1] * 0.006 * 2.0, at[2] + d[2] * 0.006 * 2.0);
      int mid = Col.withA(Col.tint(Col.mix(b.c1, b.c2, 0.6), 0.9), Math.min(1.0, 0.95 * intensity * f));
      int tipc = Col.withA(Col.tint(rim, 0.3), 0.0);
      double[][][] arms = new double[][][]{{rt, up}, {up, rt}};

      for (double[][] arm : arms) {
         double[] a = arm[0];
         double[] s = arm[1];
         double[] p0 = Prim.P(c[0] - a[0] * sz, c[1] - a[1] * sz, c[2] - a[2] * sz);
         double[] p1 = Prim.P(c[0] + a[0] * sz, c[1] + a[1] * sz, c[2] + a[2] * sz);
         double[] m0 = Prim.P(c[0] + s[0] * hw, c[1] + s[1] * hw, c[2] + s[2] * hw);
         double[] m1 = Prim.P(c[0] - s[0] * hw, c[1] - s[1] * hw, c[2] - s[2] * hw);
         b.tri(p0, m0, m1, tipc, mid, mid);
         b.tri(p1, m0, m1, tipc, mid, mid);
      }
   }

   private static final class Poly {
      double[][] pts;
      double[][] side;
      double[][] lift;
   }
}
