package need.clarity.modules.visual.wings;

import java.util.function.DoubleUnaryOperator;

final class WingStyles {
   private WingStyles() {
   }

   private static double T(WingBuilder b) {
      return b.animate ? b.time : 0.0;
   }

   static void angel(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double[] p0 = WC.P(0.03, 0.02, 0.0);
      double[] p1 = WC.P(0.3, 1.0, 0.0);
      double[] p2 = WC.P(1.04, 0.5, 0.0);
      double t = T(b);
      angelRow(b, p0, p1, p2, 11, 0.04, 1.0, 0.98, 0.0, 0.17, -0.2, WC.tint(c1, 0.55), WC.mix(WC.tint(c1, 0.45), c2, 0.45), WC.withA(c2, 0.85), t, 0);
      angelRow(b, p0, p1, p2, 10, 0.08, 0.95, 0.68, 0.035, 0.15, -0.16, WC.tint(c1, 0.7), WC.tint(c1, 0.5), WC.withA(WC.tint(c2, 0.35), 0.92), t, 1);
      angelRow(b, p0, p1, p2, 12, 0.02, 0.96, 0.36, 0.07, 0.11, -0.1, WC.tint(c1, 0.9), WC.tint(c1, 0.72), WC.tint(c1, 0.55), t, 2);
      double[] prev = arm(p0, p1, p2, 0.0);

      for (int i = 1; i <= 12; i++) {
         double[] cur = arm(p0, p1, p2, i / 12.0);
         double w0 = 0.075 * (1.0 - (i - 1) / 12.0 * 0.55);
         double w1 = 0.075 * (1.0 - i / 12.0 * 0.55);
         b.bar(prev, cur, w0, w1, WC.tint(c1, 0.85), WC.tint(c1, 0.85));
         prev = cur;
      }

      double[] j = arm(p0, p1, p2, 0.45);
      b.quad(WC.add(j, -0.05, 0.0, 0.03), WC.add(j, 0.0, 0.05, 0.03), WC.add(j, 0.05, 0.0, 0.03), WC.add(j, 0.0, -0.05, 0.03), -1);
   }

   private static double[] arm(double[] p0, double[] p1, double[] p2, double t) {
      double[] p = WC.bez(p0, p1, p2, t);
      p[2] = 0.11 * p[0] * p[0] + 0.012;
      return p;
   }

   private static void angelRow(
      WingBuilder b, double[] p0, double[] p1, double[] p2, int n, double t0, double t1, double lenScale, double layer, double wd, double curl, int cRoot, int cMid, int cTip, double time, int row
   ) {
      for (int i = 0; i < n; i++) {
         double t = t0 + (t1 - t0) * (n == 1 ? 0.0 : (double)i / (n - 1));
         double[] r = arm(p0, p1, p2, t);
         r[2] += layer;
         double ang = -1.68 + 0.84 * Math.pow(t, 0.9);
         ang += Math.sin(time * 1.7 + i * 0.45 + row + b.side * 0.7) * 0.03;
         double len = lenScale * (0.42 + 0.78 * Math.pow(t, 1.15));
         b.feather(r, ang, len, wd * (0.75 + 0.5 * t), curl, 0.1 + 0.12 * t, cRoot, cMid, cTip, row < 2);
      }
   }

   static void dragon(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      double[] S = WC.P(0.04, 0.0, 0.0);
      double[] E = WC.P(0.36, 0.62, 0.06);
      double[] Wr = WC.P(0.74, 0.86, 0.14);
      double[] B = WC.P(0.05, -0.74, 0.02);
      double[][] tips = new double[][]{WC.P(1.45, 0.68, 0.36), WC.P(1.32, 0.12, 0.34), WC.P(1.06, -0.42, 0.3), WC.P(0.72, -0.84, 0.24)};

      for (int i = 0; i < tips.length; i++) {
         tips[i][0] = tips[i][0] + Math.sin(t * 1.5 + i * 1.1) * 0.02;
         tips[i][1] = tips[i][1] + Math.sin(t * 1.9 + i * 0.8) * 0.025;
      }

      double[][] T = new double[][]{tips[0], tips[1], tips[2], tips[3], B};
      double[] bend = new double[]{0.09, 0.08, 0.07, 0.05, 0.0};
      int dark = WC.shade(c1, 0.32);
      int mid = WC.withA(c1, 0.86);
      int rim = WC.withA(WC.tint(c2, 0.25), 0.9);
      int SS = 8;
      int TT = 4;

      for (int f = 0; f < 4; f++) {
         int fi = f;

         for (int si = 0; si < 8; si++) {
            for (int ti = 0; ti < 4; ti++) {
               double s0 = si / 8.0;
               double s1 = (si + 1) / 8.0;
               double t0 = ti / 4.0;
               double t1 = (ti + 1) / 4.0;
               double[] a = membrane(Wr, T, bend, fi, s0, t0, t);
               double[] bb = membrane(Wr, T, bend, fi, s1, t0, t);
               double[] c = membrane(Wr, T, bend, fi, s1, t1, t);
               double[] d = membrane(Wr, T, bend, fi, s0, t1, t);
               b.quad(a, bb, c, d, dragonCol(dark, mid, rim, t0), dragonCol(dark, mid, rim, t0), dragonCol(dark, mid, rim, t1), dragonCol(dark, mid, rim, t1));
            }
         }

         double[] prev = membrane(Wr, T, bend, fi, 0.0, 1.0, t);

         for (int si = 1; si <= 16; si++) {
            double[] cur = membrane(Wr, T, bend, fi, si / 16.0, 1.0, t);
            b.bar(prev, cur, 0.028, 0.028, rim, rim);
            prev = cur;
         }

         double[] vEnd = membrane(Wr, T, bend, fi, 0.5, 1.0, t);
         b.bar(membrane(Wr, T, bend, fi, 0.5, 0.12, t), vEnd, 0.022, 0.008, WC.withA(WC.tint(c1, 0.3), 0.55), WC.withA(WC.tint(c2, 0.3), 0.55));

         for (double sv : new double[]{0.25, 0.75}) {
            b.bar(membrane(Wr, T, bend, fi, sv, 0.35, t), membrane(Wr, T, bend, fi, sv, 0.95, t), 0.014, 0.006, WC.withA(WC.tint(c1, 0.25), 0.4), WC.withA(WC.tint(c2, 0.25), 0.4));
         }
      }

      b.tri(S, E, Wr, dark, dark, mid);
      b.tri(S, Wr, B, dark, mid, dark);
      b.bar(S, B, 0.03, 0.03, rim, rim);
      b.lift = 0.035;
      int bone = WC.tint(c2, 0.45);
      int boneTip = WC.tint(c2, 0.75);
      b.bar(S, E, 0.11, 0.085, bone, bone);
      b.bar(E, Wr, 0.085, 0.06, bone, bone);

      for (int f = 0; f < 4; f++) {
         double[] prev = finger(Wr, T[f], bend[f], 0.0);

         for (int k = 1; k <= 8; k++) {
            double[] cur = finger(Wr, T[f], bend[f], k / 8.0);
            b.bar(prev, cur, 0.06 * (1.0 - (k - 1) / 8.0 * 0.8) + 0.012, 0.06 * (1.0 - k / 8.0 * 0.8) + 0.012, WC.mix(bone, boneTip, (k - 1) / 8.0), WC.mix(bone, boneTip, k / 8.0));
            prev = cur;
         }

         double[] tp = finger(Wr, T[f], bend[f], 1.0);
         double[] before = finger(Wr, T[f], bend[f], 0.92);
         double dx = tp[0] - before[0];
         double dy = tp[1] - before[1];
         double dl = Math.hypot(dx, dy);
         dx /= dl;
         dy /= dl;
         b.tri(WC.add(tp, -dy * 0.028, dx * 0.028, 0.0), WC.add(tp, dy * 0.028, -dx * 0.028, 0.0), WC.add(tp, dx * 0.17 - dy * 0.04, dy * 0.17 + dx * 0.04, 0.0), boneTip, boneTip, WC.withA(-1, 0.95));
      }

      b.tri(WC.add(Wr, -0.03, -0.03, 0.0), WC.add(Wr, 0.04, 0.03, 0.0), WC.add(Wr, 0.13, 0.26, 0.0), boneTip, boneTip, -1);

      for (double[] k : new double[][]{E, Wr}) {
         b.quad(WC.add(k, -0.05, 0.0, 0.045), WC.add(k, 0.0, 0.05, 0.045), WC.add(k, 0.05, 0.0, 0.045), WC.add(k, 0.0, -0.05, 0.045), boneTip);
      }

      b.lift = 0.0;
   }

   private static double[] finger(double[] Wr, double[] T, double bend, double t) {
      double dx = T[0] - Wr[0];
      double dy = T[1] - Wr[1];
      double dl = Math.hypot(dx, dy);
      double arc = bend * Math.sin(Math.PI * t);
      return WC.P(Wr[0] + dx * t - dy / dl * arc, Wr[1] + dy * t + dx / dl * arc, Wr[2] + (T[2] - Wr[2]) * t);
   }

   private static double[] membrane(double[] Wr, double[][] T, double[] bend, int f, double s, double t, double time) {
      double scallop = 1.0 - 0.21 * Math.sin(Math.PI * s);
      double tt = t * scallop;
      double[] a = finger(Wr, T[f], bend[f], tt);
      double[] c = finger(Wr, T[f + 1], bend[f + 1], tt);
      double[] p = WC.lerp(a, c, s);
      p[2] += Math.sin(time * 2.0 + tt * 4.0 + s * 3.0 + f) * 0.022 * tt;
      return p;
   }

   private static int dragonCol(int dark, int mid, int rim, double t) {
      return t < 0.85 ? WC.mix(dark, mid, t / 0.85) : WC.mix(mid, rim, (t - 0.85) / 0.15 * 0.8);
   }

   static void butterfly(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      double[] O = WC.P(0.04, 0.05, 0.0);
      int ink = WC.shade(c1, 0.1);
      DoubleUnaryOperator fore = thx -> (0.72 + 0.36 * Math.exp(-sq((thx - 0.62) / 0.5))) * (1.0 - 0.035 * (0.5 + 0.5 * Math.cos(thx * 18.0)));
      DoubleUnaryOperator hind = thx -> (0.52 + 0.26 * Math.exp(-sq((thx + 0.72) / 0.45)) + 0.42 * Math.exp(-sq((thx + 1.28) / 0.1))) * (1.0 - 0.035 * (0.5 + 0.5 * Math.cos(thx * 20.0)));
      lobe(b, O, 0.03, 1.62, fore, 30, ink, t, 0.0);
      lobe(b, O, -1.5, -0.02, hind, 30, ink, t, 0.012);

      for (int k = 0; k < 6; k++) {
         double th = 0.12 + k * 0.27;
         b.bar(WC.P(O[0], O[1], 0.02), pol(O, th, fore.applyAsDouble(th) * 0.93, 0.02), 0.016, 0.008, WC.withA(ink, 0.55), WC.withA(ink, 0.4));
      }

      for (int k = 0; k < 5; k++) {
         double th = -1.38 + k * 0.28;
         b.bar(WC.P(O[0], O[1], 0.03), pol(O, th, hind.applyAsDouble(th) * 0.9, 0.03), 0.014, 0.007, WC.withA(ink, 0.5), WC.withA(ink, 0.35));
      }

      eye(b, pol(O, 0.78, 0.55, 0.04), 0.13, c1, c2);
      eye(b, pol(O, 0.3, 0.78, 0.04), 0.07, c1, c2);
      eye(b, pol(O, -0.68, 0.38, 0.04), 0.095, c1, c2);
      eye(b, pol(O, -0.4, 0.54, 0.04), 0.055, c1, c2);

      for (int k = 0; k < 5; k++) {
         double th = -1.1 + k * 0.5;
         b.tri(
            WC.add(O, -0.02, -0.02, 0.015),
            pol(O, th - 0.1, 0.11, 0.015),
            pol(O, th + 0.1, 0.11, 0.015),
            WC.withA(WC.shade(c1, 0.25), 0.9),
            WC.withA(WC.shade(c1, 0.25), 0.0),
            WC.withA(WC.shade(c1, 0.25), 0.0)
         );
      }
   }

   private static double sq(double x) {
      return x * x;
   }

   private static double[] pol(double[] O, double th, double r, double w) {
      double u = O[0] + Math.cos(th) * r;
      double v = O[1] + Math.sin(th) * r;
      return WC.P(u, v, w + 0.12 * (r * r) * 0.6 + 0.04 * Math.abs(th) * 0.0);
   }

   private static void lobe(WingBuilder b, double[] O, double a0, double a1, DoubleUnaryOperator rf, int steps, int ink, double t, double layer) {
      double[] rings = new double[]{0.0, 0.12, 0.3, 0.5, 0.68, 0.76, 0.88, 0.95, 1.0};
      int[] rc = new int[rings.length];
      int body = WC.shade(b.c1, 0.28);
      rc[0] = body;
      rc[1] = body;
      rc[2] = WC.mix(b.c1, b.c2, 0.1);
      rc[3] = WC.mix(b.c1, b.c2, 0.4);
      rc[4] = WC.tint(WC.mix(b.c1, b.c2, 0.8), 0.12);
      rc[5] = WC.shade(WC.mix(b.c1, b.c2, 0.85), 0.45);
      rc[6] = ink;
      rc[7] = ink;
      rc[8] = ink;

      for (int i = 0; i < steps; i++) {
         double th0 = a0 + (a1 - a0) * i / steps;
         double th1 = a0 + (a1 - a0) * (i + 1) / steps;
         double r0 = rf.applyAsDouble(th0);
         double r1 = rf.applyAsDouble(th1);
         double flut0 = Math.sin(t * 2.2 + th0 * 3.0) * 0.012;
         double flut1 = Math.sin(t * 2.2 + th1 * 3.0) * 0.012;

         for (int k = 0; k < rings.length - 1; k++) {
            double[] a = pol(O, th0, r0 * rings[k], layer + flut0 * rings[k]);
            double[] bb = pol(O, th1, r1 * rings[k], layer + flut1 * rings[k]);
            double[] c = pol(O, th1, r1 * rings[k + 1], layer + flut1 * rings[k + 1]);
            double[] d = pol(O, th0, r0 * rings[k + 1], layer + flut0 * rings[k + 1]);
            int in = rc[k];
            int out = rc[k + 1];
            if (k == 7) {
               int dot = i % 2 == 0 ? WC.withA(WC.tint(b.c1, 0.95), 0.97) : ink;
               b.quad(a, bb, c, d, dot);
            } else {
               b.quad(a, bb, c, d, in, in, out, out);
            }
         }
      }
   }

   private static void eye(WingBuilder b, double[] c, double r, int c1, int c2) {
      double[] radii = new double[]{0.0, 0.28, 0.55, 0.8, 1.0};
      int[] col = new int[]{WC.shade(c1, 0.12), WC.tint(c2, 0.35), -1, WC.shade(c1, 0.12)};
      int n = 14;

      for (int k = 0; k < 4; k++) {
         for (int i = 0; i < n; i++) {
            double a0 = i * 2 * Math.PI / n;
            double a1 = (i + 1) * 2 * Math.PI / n;
            b.quad(
               WC.add(c, Math.cos(a0) * r * radii[k], Math.sin(a0) * r * radii[k], 0.0),
               WC.add(c, Math.cos(a1) * r * radii[k], Math.sin(a1) * r * radii[k], 0.0),
               WC.add(c, Math.cos(a1) * r * radii[k + 1], Math.sin(a1) * r * radii[k + 1], 0.0),
               WC.add(c, Math.cos(a0) * r * radii[k + 1], Math.sin(a0) * r * radii[k + 1], 0.0),
               col[k]
            );
         }
      }
   }

   static void phoenix(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      double[] a0 = WC.P(0.05, 0.04, 0.0);
      double[] a1 = WC.P(0.34, 0.78, 0.0);
      double[] a2 = WC.P(0.95, 0.62, 0.0);
      int n = 11;

      for (int i = 0; i < n; i++) {
         double k = (double)i / (n - 1);
         double[] root = WC.bez(a0, a1, a2, 0.04 + 0.92 * k);
         root[2] = 0.12 * root[0] * root[0];
         double deg = 104.0 - 96.0 * Math.pow(k, 0.9);
         double len = 0.95 + 0.7 * k + 0.15 * Math.sin(k * 3.14);
         plume(b, root, Math.toRadians(deg), len, 0.22, -0.14, 0.1 + 0.1 * k, i, t, WC.tint(c1, 0.35), c1, c2);
      }

      for (int i = 0; i < 6; i++) {
         double k = i / 5.0;
         double[] root = WC.P(0.07 + 0.05 * k, 0.02 - 0.04 * k, 0.03);
         plume(b, root, Math.toRadians(-6.0 - 62.0 * k), 1.05 - 0.48 * k, 0.19, 0.12, 0.06, i + 40, t, WC.tint(c1, 0.3), c1, c2);
      }

      for (int i = 0; i < 9; i++) {
         double k = i / 8.0;
         double[] root = WC.bez(a0, a1, a2, 0.1 + 0.78 * k);
         root[2] = 0.12 * root[0] * root[0] + 0.05;
         double deg = 96.0 - 84.0 * Math.pow(k, 0.9);
         plume(b, root, Math.toRadians(deg), 0.5 + 0.28 * k, 0.13, -0.08, 0.05, i + 20, t, WC.tint(c1, 0.85), WC.tint(c1, 0.5), WC.mix(c1, c2, 0.5));
      }

      for (int i = 0; i < 16; i++) {
         double k = WC.rnd(i * 7 + 1);
         double[] r = WC.bez(a0, a1, a2, 0.1 + 0.9 * k);
         double ph = b.animate ? (t * (0.26 + 0.16 * WC.rnd(i * 7 + 3)) + WC.rnd(i * 7 + 4)) % 1.0 : WC.rnd(i * 7 + 5);
         double u = r[0] + 0.35 * (1.0 - k) * 0.0 + Math.sin(t * 2.0 + i) * 0.05 + (WC.rnd(i * 7 + 2) - 0.3) * 0.5;
         double v = r[1] + 0.15 + ph * 0.65;
         double s = 0.05 * (1.0 - ph) + 0.008;
         int col = WC.withA(WC.mix(WC.tint(c1, 0.7), c2, ph), (1.0 - ph) * WC.sm(ph * 8.0) * 0.95);
         double[] c = WC.P(u, v, 0.12 + WC.rnd(i * 7 + 6) * 0.2);
         b.quad(WC.add(c, -s, 0.0, 0.0), WC.add(c, 0.0, s * 1.5, 0.0), WC.add(c, s, 0.0, 0.0), WC.add(c, 0.0, -s * 1.5, 0.0), col);
      }
   }

   private static void plume(WingBuilder b, double[] root, double ang, double len, double wd, double bow, double lift, int idx, double time, int cRoot, int cMid, int cTip) {
      int n = 9;
      double dx = Math.cos(ang);
      double dy = Math.sin(ang);
      double px = -dy;
      double py = dx;
      double[] pl = null;
      double[] pr = null;
      int pc = 0;
      double[] pc0 = null;

      for (int i = 0; i <= 9; i++) {
         double t = i / 9.0;
         double wave = Math.sin(time * 3.0 + idx * 0.8 + t * 5.0) * 0.045 * t * t;
         double off = bow * len * Math.sin(Math.PI * t) + wave;
         double u = root[0] + dx * len * t + px * off;
         double v = root[1] + dy * len * t + py * off + lift * t * t * t;
         double w = root[2] + 0.28 * t * t * len + Math.sin(time * 2.4 + idx + t * 3.0) * 0.02 * t;
         double prof = i == 9 ? 0.0 : Math.max(0.22, Math.sin(Math.PI * Math.pow(t, 0.65)));
         double hw = wd * prof * 0.5;
         double tx = dx;
         double ty = dy;
         if (i > 0) {
            tx = u - pc0[0];
            ty = v - pc0[1];
            double l = Math.hypot(tx, ty);
            tx /= l;
            ty /= l;
         }

         double nx = -ty * hw;
         double ny = tx * hw;
         double[] l = WC.P(u + nx, v + ny, w);
         double[] r = WC.P(u - nx, v - ny, w);
         int col = t < 0.35 ? WC.mix(cRoot, cMid, t / 0.35) : WC.mix(cMid, cTip, (t - 0.35) / 0.65);
         col = WC.withA(col, 0.98 - 0.78 * t * t);
         if (i > 0) {
            b.quad(pl, pr, r, l, pc, pc, col, col);
         }

         pl = l;
         pr = r;
         pc = col;
         pc0 = WC.P(u, v, w);
      }
   }

   static void crystal(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      double[] lens = new double[]{0.78, 1.02, 1.28, 1.34, 1.16, 0.94, 0.72};

      for (int i = 0; i < 7; i++) {
         double deg = 78.0 - i * 17.5;
         double a = Math.toRadians(deg);
         double[] root = WC.P(0.05 + 0.06 * Math.cos(a), 0.06 + 0.06 * Math.sin(a), 0.0 + i * 0.004);
         double m = i / 6.0;
         shard(b, root, a, lens[i], 0.17, 0.075, WC.mix(c1, c2, m * 0.7), 0.92);
      }

      for (int i = 0; i < 6; i++) {
         double deg = 70.0 - i * 17.5;
         double a = Math.toRadians(deg);
         double[] root = WC.P(0.06, 0.05, -0.045);
         shard(b, root, a, 0.55 + 0.12 * Math.sin(i * 1.7), 0.12, 0.05, WC.mix(c2, c1, i / 5.0 * 0.5), 0.8);
      }

      for (int i = 0; i < 8; i++) {
         double deg = 80 - i * 16 + (WC.rnd(i + 30) - 0.5) * 8.0;
         double a = Math.toRadians(deg);
         double dist = 1.05 + WC.rnd(i + 50) * 0.22 + 0.3 * Math.exp(-sq((deg - 20.0) / 40.0));
         double bob = Math.sin(t * 1.4 + i * 1.3) * 0.045;
         double[] root = WC.P(0.05 + Math.cos(a) * dist, 0.06 + Math.sin(a) * dist + bob, 0.05 + WC.rnd(i + 70) * 0.12);
         double sz = 0.13 + WC.rnd(i + 90) * 0.12;
         shard(b, root, a + (WC.rnd(i + 110) - 0.5) * 0.6 + t * 0.0, sz, sz * 0.38, sz * 0.22, WC.mix(c1, c2, WC.rnd(i + 130)), 0.9);
      }
   }

   private static void shard(WingBuilder b, double[] root, double ang, double len, double wd, double depth, int base, double alpha) {
      double dx = Math.cos(ang);
      double dy = Math.sin(ang);
      double px = -dy;
      double py = dx;
      double[] B = root;
      double[] L = WC.P(root[0] + dx * len * 0.38 + px * wd, root[1] + dy * len * 0.38 + py * wd, root[2] + depth * 0.3);
      double[] R = WC.P(root[0] + dx * len * 0.38 - px * wd, root[1] + dy * len * 0.38 - py * wd, root[2] + depth * 0.3);
      double[] C = WC.P(root[0] + dx * len * 0.48, root[1] + dy * len * 0.48, root[2] + depth);
      double[] Tt = WC.P(root[0] + dx * len, root[1] + dy * len, root[2] + depth * 0.25);
      int cA = WC.withA(WC.shade(WC.tint(base, 0.25), 1.15), alpha);
      int cB = WC.withA(WC.shade(base, 0.62), alpha);
      int cC = WC.withA(WC.shade(base, 0.95), alpha);
      int cD = WC.withA(WC.shade(base, 0.45), alpha);
      b.tri(B, L, C, cA);
      b.tri(B, C, R, cB);
      b.tri(L, Tt, C, cC);
      b.tri(C, Tt, R, cD);
      int edge = WC.withA(WC.tint(base, 0.8), 0.75);
      b.bar(WC.P(B[0], B[1], B[2] + 0.01), WC.P(Tt[0], Tt[1], Tt[2] + 0.01), len * 0.018, len * 0.006, edge, edge);
   }

   static void cyber(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      int N = 6;
      double[] lens = new double[]{1.38, 1.26, 1.12, 0.98, 0.84, 0.7};
      double[] degs = new double[]{20.0, 10.0, 0.0, -10.0, -20.0, -32.0};
      double[][] roots = new double[6][];
      double[][] mids = new double[6][];
      int fill = WC.withA(WC.shade(c1, 0.55), 0.6);
      int fillBright = WC.withA(c1, 0.84);
      double[] hub = WC.P(0.06, 0.04, 0.02);

      for (int i = 0; i < 6; i++) {
         double k = (5 - i) / 5.0;
         roots[i] = WC.P(hub[0] + 0.62 * k, hub[1] + 0.06 + 0.66 * k, 0.02 + 0.004 * i + sweep(0.62 * k) * 0.5);
      }

      for (int i = 0; i < 5; i++) {
         double[] q0 = along(roots[i], degs[i], lens[i] * 0.72);
         double[] q1 = along(roots[i + 1], degs[i + 1], lens[i + 1] * 0.72);
         b.quad(roots[i], q0, q1, roots[i + 1], WC.withA(c2, 0.34), WC.withA(c2, 0.04), WC.withA(c2, 0.04), WC.withA(c2, 0.34));
      }

      for (int i = 0; i < 6; i++) {
         double a = Math.toRadians(degs[i]);
         double L = lens[i];
         double dx = Math.cos(a);
         double dy = Math.sin(a);
         double px = -dy;
         double py = dx;
         double[] o = roots[i];
         double[][] poly = new double[][]{{0.0, 0.06}, {0.26 * L, 0.115}, {L, 0.02}, {0.86 * L, -0.125}, {0.26 * L, -0.105}, {0.0, -0.06}};
         double[][] pts = new double[6][];

         for (int k = 0; k < 6; k++) {
            double x = poly[k][0];
            double y = poly[k][1];
            pts[k] = WC.P(o[0] + dx * x + px * y, o[1] + dy * x + py * y, o[2] + sweep(x));
         }

         b.quad(pts[0], pts[1], pts[4], pts[5], fill, fill, fillBright, fill);
         b.quad(pts[1], pts[2], pts[3], pts[4], fillBright, WC.withA(c2, 0.92), WC.withA(c2, 0.72), fillBright);

         for (int k = 0; k < 5; k++) {
            double along = k / 5.0;
            double pulse = b.animate ? 0.35 + 0.65 * Math.pow(Math.max(0.0, Math.sin(6.283 * (t * 0.55 - along * 0.8 + i * 0.17))), 3.0) : 0.7;
            int col = WC.mix(WC.withA(c2, 0.9), -1, pulse * 0.85);
            b.bar(WC.P(pts[k][0], pts[k][1], pts[k][2] + 0.006), WC.P(pts[k + 1][0], pts[k + 1][1], pts[k + 1][2] + 0.006), 0.02 + 0.014 * pulse, 0.02 + 0.014 * pulse, col, col);
         }

         b.bar(
            WC.P(o[0] + dx * 0.16 * L, o[1] + dy * 0.16 * L, o[2] + sweep(0.16 * L) + 0.008),
            WC.P(o[0] + dx * 0.9 * L, o[1] + dy * 0.9 * L, o[2] + sweep(0.9 * L) + 0.008),
            0.024,
            0.008,
            WC.withA(WC.tint(c2, 0.7), 0.85),
            WC.withA(-1, 0.95)
         );

         for (int k = 1; k <= 2; k++) {
            double x = L * (0.3 + 0.22 * k);
            double[] c = WC.P(o[0] + dx * x, o[1] + dy * x, o[2] + sweep(x) + 0.01);
            b.quad(WC.add(c, -0.024, 0.0, 0.0), WC.add(c, 0.0, 0.024, 0.0), WC.add(c, 0.024, 0.0, 0.0), WC.add(c, 0.0, -0.024, 0.0), -1);
         }
      }

      b.lift = 0.02;

      for (int i = 0; i < 5; i++) {
         b.bar(roots[i], roots[i + 1], 0.05, 0.05, WC.withA(WC.tint(c2, 0.55), 0.95), WC.withA(WC.tint(c2, 0.55), 0.95));
      }

      b.bar(hub, roots[5], 0.05, 0.05, WC.withA(WC.tint(c2, 0.55), 0.95), WC.withA(WC.tint(c2, 0.55), 0.95));

      for (int i = 0; i < 6; i++) {
         double[] c = WC.add(roots[i], 0.0, 0.0, 0.03);
         b.quad(WC.add(c, -0.045, 0.0, 0.0), WC.add(c, 0.0, 0.045, 0.0), WC.add(c, 0.045, 0.0, 0.0), WC.add(c, 0.0, -0.045, 0.0), -1);
      }

      b.lift = 0.0;

      for (int i = 0; i < 6; i++) {
         double a0 = i * Math.PI / 3.0;
         double a1 = (i + 1) * Math.PI / 3.0;
         double[] c = WC.add(hub, 0.0, 0.0, 0.03);
         b.tri(
            c,
            WC.add(c, Math.cos(a0) * 0.15, Math.sin(a0) * 0.15, 0.0),
            WC.add(c, Math.cos(a1) * 0.15, Math.sin(a1) * 0.15, 0.0),
            WC.withA(WC.shade(c1, 0.3), 0.95),
            WC.withA(c2, 0.95),
            WC.withA(c2, 0.95)
         );
         b.tri(
            c,
            WC.add(c, Math.cos(a0) * 0.075, Math.sin(a0) * 0.075, 0.012),
            WC.add(c, Math.cos(a1) * 0.075, Math.sin(a1) * 0.075, 0.012),
            WC.withA(-1, 0.9),
            WC.withA(WC.tint(c2, 0.5), 0.9),
            WC.withA(WC.tint(c2, 0.5), 0.9)
         );
      }
   }

   private static double[] along(double[] o, double deg, double x) {
      double a = Math.toRadians(deg);
      return WC.P(o[0] + Math.cos(a) * x, o[1] + Math.sin(a) * x, o[2] + sweep(x));
   }

   private static double sweep(double x) {
      return 0.3 * x * x;
   }

   static void fairy(WingBuilder b) {
      int c1 = b.c1;
      int c2 = b.c2;
      double t = T(b);
      insectWing(b, WC.P(0.05, 0.06, 0.0), 1.02, 1.45, 0.32, 0.0, 0.0, t);
      insectWing(b, WC.P(0.05, 0.05, 0.03), 0.42, 1.3, 0.27, 0.03, 1.7, t);
      insectWing(b, WC.P(0.05, 0.04, 0.06), -0.28, 1.08, 0.24, 0.06, 3.1, t);
      insectWing(b, WC.P(0.05, 0.03, 0.09), -0.8, 0.82, 0.17, 0.09, 4.4, t);

      for (int i = 0; i < 12; i++) {
         double a = 1.0 - WC.rnd(i * 3 + 1) * 1.8;
         double r = 0.5 + WC.rnd(i * 3 + 2) * 1.05;
         double tw = b.animate ? 0.5 + 0.5 * Math.sin(t * 3.0 + i * 2.1) : 0.7;
         double s = 0.012 + 0.016 * tw;
         double[] c = WC.P(0.05 + Math.cos(a) * r, 0.05 + Math.sin(a) * r + Math.sin(t * 0.8 + i) * 0.03, 0.04 + WC.rnd(i * 3) * 0.12);
         int col = WC.withA(WC.tint(WC.mix(c1, c2, WC.rnd(i + 5)), 0.8), 0.35 + 0.6 * tw);
         b.quad(WC.add(c, -s, 0.0, 0.0), WC.add(c, 0.0, s * 2.2, 0.0), WC.add(c, s, 0.0, 0.0), WC.add(c, 0.0, -s * 2.2, 0.0), col);
         b.quad(WC.add(c, -s * 2.2, 0.0, 0.0), WC.add(c, 0.0, s, 0.0), WC.add(c, s * 2.2, 0.0, 0.0), WC.add(c, 0.0, -s, 0.0), col);
      }
   }

   private static void insectWing(WingBuilder b, double[] root, double ang, double len, double hw, double layer, double phase, double time) {
      double dx = Math.cos(ang);
      double dy = Math.sin(ang);
      double px = -dy;
      double py = dx;
      int TT = 9;
      double[] sv = new double[]{-1.0, -0.9, -0.45, 0.0, 0.45, 0.9, 1.0};

      for (int ti = 0; ti < 9; ti++) {
         double t0 = ti / 9.0;
         double t1 = (ti + 1) / 9.0;

         for (int si = 0; si < sv.length - 1; si++) {
            double[] a = ip(root, dx, dy, px, py, len, hw, t0, sv[si], layer);
            double[] bb = ip(root, dx, dy, px, py, len, hw, t0, sv[si + 1], layer);
            double[] c = ip(root, dx, dy, px, py, len, hw, t1, sv[si + 1], layer);
            double[] d = ip(root, dx, dy, px, py, len, hw, t1, sv[si], layer);
            b.quad(a, bb, c, d, ic(b, t0, sv[si], phase, time), ic(b, t0, sv[si + 1], phase, time), ic(b, t1, sv[si + 1], phase, time), ic(b, t1, sv[si], phase, time));
         }
      }

      int vein = WC.withA(WC.tint(WC.mix(b.c1, b.c2, 0.5), 0.75), 0.6);
      double[] r0 = ip(root, dx, dy, px, py, len, hw, 0.0, 0.0, layer + 0.006);
      double[] r1 = ip(root, dx, dy, px, py, len, hw, 0.96, 0.0, layer + 0.006);
      b.bar(r0, r1, 0.02, 0.007, vein, vein);

      for (int k = 1; k <= 6; k++) {
         double t = k / 7.0 * 0.92;

         for (int sgn = -1; sgn <= 1; sgn += 2) {
            double[] c = ip(root, dx, dy, px, py, len, hw, t, 0.0, layer + 0.006);
            double[] e = ip(root, dx, dy, px, py, len, hw, Math.min(0.97, t + 0.1), sgn * 0.93, layer + 0.006);
            b.bar(c, e, 0.011, 0.006, vein, WC.withA(vein, 0.35));
         }
      }

      double[] s0 = ip(root, dx, dy, px, py, len, hw, 0.74, 0.85, layer + 0.008);
      double[] s1 = ip(root, dx, dy, px, py, len, hw, 0.8, 0.85, layer + 0.008);
      b.bar(s0, s1, 0.03, 0.03, WC.withA(WC.shade(b.c1, 0.3), 0.85), WC.withA(WC.shade(b.c1, 0.3), 0.85));
   }

   private static double[] ip(double[] root, double dx, double dy, double px, double py, double len, double hw, double t, double s, double layer) {
      double prof = Math.pow(Math.sin(Math.PI * Math.pow(Math.min(1.0, Math.max(0.0, t)), 0.72)), 0.85);
      double x = len * t;
      double y = hw * prof * s;
      return WC.P(root[0] + dx * x + px * y, root[1] + dy * x + py * y, root[2] + layer + 0.1 * t * t * len);
   }

   private static int ic(WingBuilder b, double t, double s, double phase, double time) {
      double sh = 0.5 + 0.5 * Math.sin(t * 4.2 + s * 1.3 + time * 1.3 + phase);
      int c = WC.mix(b.c1, b.c2, sh);
      double a = 0.26 + 0.62 * WC.sm((Math.abs(s) - 0.72) / 0.28) + 0.1 * (1.0 - t);
      return WC.withA(WC.tint(c, 0.18 + 0.12 * sh), a);
   }

   static void classic(WingBuilder b) {
      int c1 = WC.withA(b.c1, 0.85);
      int c2 = WC.withA(b.c2, 0.6);
      b.quad(WC.P(0.05, 0.12, 0.0), WC.P(0.9, 0.34, 0.55), WC.P(0.6, -0.6, 0.4), WC.P(0.05, -0.4, 0.0), c1);
      b.quad(WC.P(0.05, 0.05, 0.0), WC.P(0.55, 0.16, 0.34), WC.P(0.4, -0.42, 0.28), WC.P(0.05, -0.28, 0.0), c2);
   }
}
