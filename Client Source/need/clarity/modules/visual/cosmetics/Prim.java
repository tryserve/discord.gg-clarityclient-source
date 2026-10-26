package need.clarity.modules.visual.cosmetics;

import java.util.function.BiConsumer;
import java.util.function.DoubleUnaryOperator;

public final class Prim {
   private static final double TAU = Math.PI * 2;

   private Prim() {
   }

   public static Prim.Shade solid(int c) {
      return (u, v) -> c;
   }

   public static Prim.Shade byV(int c0, int c1) {
      return (u, v) -> Col.mix(c0, c1, v);
   }

   public static Prim.Shade byUloop(int c0, int c1) {
      return (u, v) -> Col.mix(c0, c1, 1.0 - Math.abs(2.0 * u - 1.0));
   }

   public static double[] P(double u, double v, double w) {
      return new double[]{u, v, w};
   }

   public static double[] lerp(double[] p, double[] q, double t) {
      return new double[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t};
   }

   public static double[] add(double[] p, double du, double dv, double dw) {
      return new double[]{p[0] + du, p[1] + dv, p[2] + dw};
   }

   public static double[] bez(double[] p0, double[] p1, double[] p2, double t) {
      double s = 1.0 - t;
      return new double[]{s * s * p0[0] + 2.0 * s * t * p1[0] + t * t * p2[0], s * s * p0[1] + 2.0 * s * t * p1[1] + t * t * p2[1], s * s * p0[2] + 2.0 * s * t * p1[2] + t * t * p2[2]};
   }

   public static double sm(double t) {
      t = Col.clamp01(t);
      return t * t * (3.0 - 2.0 * t);
   }

   public static double rnd(int seed) {
      long x = seed * -7046029254386353131L;
      x ^= x >>> 29;
      x *= -4658895280553007687L;
      x ^= x >>> 32;
      return (x & 16777215L) / 1.6777216E7;
   }

   public static double[] sub(double[] p, double[] q) {
      return new double[]{p[0] - q[0], p[1] - q[1], p[2] - q[2]};
   }

   public static double dot(double[] p, double[] q) {
      return p[0] * q[0] + p[1] * q[1] + p[2] * q[2];
   }

   public static double[] cross(double[] p, double[] q) {
      return new double[]{p[1] * q[2] - p[2] * q[1], p[2] * q[0] - p[0] * q[2], p[0] * q[1] - p[1] * q[0]};
   }

   public static double[] unit(double[] p) {
      double l = Math.sqrt(dot(p, p));
      return l < 1.0E-12 ? new double[]{0.0, 1.0, 0.0} : new double[]{p[0] / l, p[1] / l, p[2] / l};
   }

   private static double[] madd(double[] o, double[] a, double ka, double[] b, double kb) {
      return new double[]{o[0] + a[0] * ka + b[0] * kb, o[1] + a[1] * ka + b[1] * kb, o[2] + a[2] * ka + b[2] * kb};
   }

   private static double[][] perp(double[] axis) {
      double[] ref = Math.abs(axis[1]) < 0.9 ? new double[]{0.0, 1.0, 0.0} : new double[]{1.0, 0.0, 0.0};
      double[] p1 = unit(cross(axis, ref));
      return new double[][]{p1, cross(axis, p1)};
   }

   public static void ring(Builder b, double y, double rIn, double rOut, int segs, Prim.Shade s) {
      for (int i = 0; i < segs; i++) {
         double t0 = (double)i / segs;
         double t1 = (double)(i + 1) / segs;
         double a0 = t0 * (Math.PI * 2);
         double a1 = t1 * (Math.PI * 2);
         double c0 = Math.cos(a0);
         double s0 = Math.sin(a0);
         double c1 = Math.cos(a1);
         double s1 = Math.sin(a1);
         b.quad(P(rIn * c0, y, rIn * s0), P(rOut * c0, y, rOut * s0), P(rOut * c1, y, rOut * s1), P(rIn * c1, y, rIn * s1), s.at(t0, 0.0), s.at(t0, 1.0), s.at(t1, 1.0), s.at(t1, 0.0));
      }
   }

   public static void disc(Builder b, double y, double r, int segs, Prim.Shade s) {
      double[] c = P(0.0, y, 0.0);

      for (int i = 0; i < segs; i++) {
         double t0 = (double)i / segs;
         double t1 = (double)(i + 1) / segs;
         b.tri(
            c,
            P(r * Math.cos(t0 * (Math.PI * 2)), y, r * Math.sin(t0 * (Math.PI * 2))),
            P(r * Math.cos(t1 * (Math.PI * 2)), y, r * Math.sin(t1 * (Math.PI * 2))),
            s.at(t0, 0.0),
            s.at(t0, 1.0),
            s.at(t1, 1.0)
         );
      }
   }

   public static void star(Builder b, double y, int points, double rOuter, double rInner, double phase, Prim.Shade s) {
      int n = points * 2;
      double[] c = P(0.0, y, 0.0);

      for (int k = 0; k < n; k++) {
         double a0 = phase + Math.PI * k / points;
         double a1 = phase + Math.PI * (k + 1) / points;
         double r0 = k % 2 == 0 ? rOuter : rInner;
         double r1 = (k + 1) % 2 == 0 ? rOuter : rInner;
         b.tri(
            c,
            P(r0 * Math.cos(a0), y, r0 * Math.sin(a0)),
            P(r1 * Math.cos(a1), y, r1 * Math.sin(a1)),
            s.at((double)k / n, 0.0),
            s.at((double)k / n, r0 / rOuter),
            s.at((double)(k + 1) / n, r1 / rOuter)
         );
      }
   }

   public static void cone(Builder b, double y0, double r, double h, int segs, Prim.Shade s, boolean cap) {
      double[] tip = P(0.0, y0 + h, 0.0);
      double[] ctr = P(0.0, y0, 0.0);

      for (int i = 0; i < segs; i++) {
         double t0 = (double)i / segs;
         double t1 = (double)(i + 1) / segs;
         double tm = (t0 + t1) * 0.5;
         double[] p0 = P(r * Math.cos(t0 * (Math.PI * 2)), y0, r * Math.sin(t0 * (Math.PI * 2)));
         double[] p1 = P(r * Math.cos(t1 * (Math.PI * 2)), y0, r * Math.sin(t1 * (Math.PI * 2)));
         b.tri(p0, p1, tip, s.at(t0, 0.0), s.at(t1, 0.0), s.at(tm, 1.0));
         if (cap) {
            b.tri(ctr, p1, p0, s.at(tm, 0.0), s.at(t1, 0.0), s.at(t0, 0.0));
         }
      }
   }

   public static void cylinder(Builder b, double y0, double y1, double r0, double r1, int segs, Prim.Shade s, boolean capBottom, boolean capTop) {
      for (int i = 0; i < segs; i++) {
         double t0 = (double)i / segs;
         double t1 = (double)(i + 1) / segs;
         double tm = (t0 + t1) * 0.5;
         double c0 = Math.cos(t0 * (Math.PI * 2));
         double s0 = Math.sin(t0 * (Math.PI * 2));
         double c1 = Math.cos(t1 * (Math.PI * 2));
         double s1 = Math.sin(t1 * (Math.PI * 2));
         b.quad(P(r0 * c0, y0, r0 * s0), P(r0 * c1, y0, r0 * s1), P(r1 * c1, y1, r1 * s1), P(r1 * c0, y1, r1 * s0), s.at(t0, 0.0), s.at(t1, 0.0), s.at(t1, 1.0), s.at(t0, 1.0));
         if (capBottom && r0 > 1.0E-9) {
            b.tri(P(0.0, y0, 0.0), P(r0 * c1, y0, r0 * s1), P(r0 * c0, y0, r0 * s0), s.at(tm, 0.0), s.at(t1, 0.0), s.at(t0, 0.0));
         }

         if (capTop && r1 > 1.0E-9) {
            b.tri(P(0.0, y1, 0.0), P(r1 * c0, y1, r1 * s0), P(r1 * c1, y1, r1 * s1), s.at(tm, 1.0), s.at(t0, 1.0), s.at(t1, 1.0));
         }
      }
   }

   public static void torus(Builder b, double y, double R, double r, int major, int minor, Prim.Shade s) {
      for (int i = 0; i < major; i++) {
         double t0 = (double)i / major;
         double t1 = (double)(i + 1) / major;
         double c0 = Math.cos(t0 * (Math.PI * 2));
         double s0 = Math.sin(t0 * (Math.PI * 2));
         double c1 = Math.cos(t1 * (Math.PI * 2));
         double s1 = Math.sin(t1 * (Math.PI * 2));

         for (int j = 0; j < minor; j++) {
            double v0 = (double)j / minor;
            double v1 = (double)(j + 1) / minor;
            double rad0 = R + r * Math.cos(v0 * (Math.PI * 2));
            double h0 = y + r * Math.sin(v0 * (Math.PI * 2));
            double rad1 = R + r * Math.cos(v1 * (Math.PI * 2));
            double h1 = y + r * Math.sin(v1 * (Math.PI * 2));
            b.quad(P(rad0 * c0, h0, rad0 * s0), P(rad0 * c1, h0, rad0 * s1), P(rad1 * c1, h1, rad1 * s1), P(rad1 * c0, h1, rad1 * s0), s.at(t0, v0), s.at(t1, v0), s.at(t1, v1), s.at(t0, v1));
         }
      }
   }

   public static void sphere(Builder b, double cx, double cy, double cz, double r, int lon, int lat, Prim.Shade s) {
      ellipsoid(b, cx, cy, cz, r, r, r, lon, lat, s);
   }

   public static void ellipsoid(Builder b, double cx, double cy, double cz, double rx, double ry, double rz, int lon, int lat, Prim.Shade s) {
      for (int k = 0; k < lat; k++) {
         double v0 = (double)k / lat;
         double v1 = (double)(k + 1) / lat;
         double f0 = (-Math.PI / 2) + Math.PI * v0;
         double f1 = (-Math.PI / 2) + Math.PI * v1;
         double cf0 = Math.cos(f0);
         double sf0 = Math.sin(f0);
         double cf1 = Math.cos(f1);
         double sf1 = Math.sin(f1);

         for (int i = 0; i < lon; i++) {
            double t0 = (double)i / lon;
            double t1 = (double)(i + 1) / lon;
            double c0 = Math.cos(t0 * (Math.PI * 2));
            double s0 = Math.sin(t0 * (Math.PI * 2));
            double c1 = Math.cos(t1 * (Math.PI * 2));
            double s1 = Math.sin(t1 * (Math.PI * 2));
            b.quad(
               P(cx + rx * cf0 * c0, cy + ry * sf0, cz + rz * cf0 * s0),
               P(cx + rx * cf0 * c1, cy + ry * sf0, cz + rz * cf0 * s1),
               P(cx + rx * cf1 * c1, cy + ry * sf1, cz + rz * cf1 * s1),
               P(cx + rx * cf1 * c0, cy + ry * sf1, cz + rz * cf1 * s0),
               s.at(t0, v0),
               s.at(t1, v0),
               s.at(t1, v1),
               s.at(t0, v1)
            );
         }
      }
   }

   public static void prism(Builder b, double y0, double y1, double r, int sides, double phase, Prim.Shade s, boolean caps) {
      for (int i = 0; i < sides; i++) {
         double t0 = (double)i / sides;
         double t1 = (double)(i + 1) / sides;
         double tm = (t0 + t1) * 0.5;
         double a0 = phase + t0 * (Math.PI * 2);
         double a1 = phase + t1 * (Math.PI * 2);
         double k = facet(phase + tm * (Math.PI * 2));
         double c0 = Math.cos(a0);
         double s0 = Math.sin(a0);
         double c1 = Math.cos(a1);
         double s1 = Math.sin(a1);
         b.quad(
            P(r * c0, y0, r * s0),
            P(r * c1, y0, r * s1),
            P(r * c1, y1, r * s1),
            P(r * c0, y1, r * s0),
            Col.shade(s.at(tm, 0.0), k),
            Col.shade(s.at(tm, 0.0), k),
            Col.shade(s.at(tm, 1.0), k),
            Col.shade(s.at(tm, 1.0), k)
         );
         if (caps) {
            b.tri(P(0.0, y0, 0.0), P(r * c1, y0, r * s1), P(r * c0, y0, r * s0), Col.shade(s.at(tm, 0.0), 0.7), Col.shade(s.at(tm, 0.0), 0.7), Col.shade(s.at(tm, 0.0), 0.7));
            b.tri(P(0.0, y1, 0.0), P(r * c0, y1, r * s0), P(r * c1, y1, r * s1), Col.shade(s.at(tm, 1.0), 1.1), Col.shade(s.at(tm, 1.0), 1.1), Col.shade(s.at(tm, 1.0), 1.1));
         }
      }
   }

   public static void box(Builder b, double x0, double y0, double z0, double x1, double y1, double z1, int cBottom, int cTop) {
      double[][] v = new double[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
      int[][] f = new int[][]{{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}};
      double[] k = new double[]{0.8, 0.8, 0.9, 0.9};

      for (int i = 0; i < 4; i++) {
         int[] q = f[i];
         int lo = Col.shade(cBottom, k[i]);
         int hi = Col.shade(cTop, k[i]);
         b.quad(v[q[0]], v[q[1]], v[q[2]], v[q[3]], lo, lo, hi, hi);
      }

      int t = Col.shade(cTop, 1.1);
      int d = Col.shade(cBottom, 0.6);
      b.quad(v[3], v[2], v[6], v[7], t);
      b.quad(v[0], v[4], v[5], v[1], d);
   }

   public static void taperedTube(Builder b, double[] p0, double[] p1, double r0, double r1, int segs, Prim.Shade s, boolean caps) {
      double[] axis = unit(sub(p1, p0));
      double[][] pp = perp(axis);

      for (int i = 0; i < segs; i++) {
         double t0 = (double)i / segs;
         double t1 = (double)(i + 1) / segs;
         double tm = (t0 + t1) * 0.5;
         double c0 = Math.cos(t0 * (Math.PI * 2));
         double s0 = Math.sin(t0 * (Math.PI * 2));
         double c1 = Math.cos(t1 * (Math.PI * 2));
         double s1 = Math.sin(t1 * (Math.PI * 2));
         double[] a0 = madd(p0, pp[0], r0 * c0, pp[1], r0 * s0);
         double[] a1 = madd(p0, pp[0], r0 * c1, pp[1], r0 * s1);
         double[] e0 = madd(p1, pp[0], r1 * c0, pp[1], r1 * s0);
         double[] e1 = madd(p1, pp[0], r1 * c1, pp[1], r1 * s1);
         b.quad(a0, a1, e1, e0, s.at(t0, 0.0), s.at(t1, 0.0), s.at(t1, 1.0), s.at(t0, 1.0));
         if (caps && r0 > 1.0E-9) {
            b.tri(p0, a1, a0, s.at(tm, 0.0), s.at(t1, 0.0), s.at(t0, 0.0));
         }

         if (caps && r1 > 1.0E-9) {
            b.tri(p1, e0, e1, s.at(tm, 1.0), s.at(t0, 1.0), s.at(t1, 1.0));
         }
      }
   }

   public static void spike(Builder b, double[] base, double[] tip, double r, int sides, Prim.Shade s) {
      double[][] pp = perp(unit(sub(tip, base)));

      for (int i = 0; i < sides; i++) {
         double t0 = (double)i / sides;
         double t1 = (double)(i + 1) / sides;
         double tm = (t0 + t1) * 0.5;
         double k = facet(tm * (Math.PI * 2));
         double[] a0 = madd(base, pp[0], r * Math.cos(t0 * (Math.PI * 2)), pp[1], r * Math.sin(t0 * (Math.PI * 2)));
         double[] a1 = madd(base, pp[0], r * Math.cos(t1 * (Math.PI * 2)), pp[1], r * Math.sin(t1 * (Math.PI * 2)));
         b.tri(a0, a1, tip, Col.shade(s.at(tm, 0.0), k), Col.shade(s.at(tm, 0.0), k), Col.shade(s.at(tm, 1.0), k));
      }
   }

   public static void ribbon(Builder b, double[][] spine, double w0, double w1, double[] side, Prim.Shade s) {
      int n = spine.length;
      if (n >= 2) {
         double[][] l = new double[n][];
         double[][] r = new double[n][];

         for (int i = 0; i < n; i++) {
            double[] t = unit(sub(spine[Math.min(i + 1, n - 1)], spine[Math.max(i - 1, 0)]));
            double[] sv = sub(side, new double[]{t[0] * dot(side, t), t[1] * dot(side, t), t[2] * dot(side, t)});
            sv = dot(sv, sv) < 1.0E-10 ? perp(t)[0] : unit(sv);
            double hw = (w0 + (w1 - w0) * i / (n - 1.0)) * 0.5;
            l[i] = madd(spine[i], sv, hw, sv, 0.0);
            r[i] = madd(spine[i], sv, -hw, sv, 0.0);
         }

         for (int i = 0; i < n - 1; i++) {
            double v0 = i / (n - 1.0);
            double v1 = (i + 1) / (n - 1.0);
            b.quad(l[i], r[i], r[i + 1], l[i + 1], s.at(0.0, v0), s.at(1.0, v0), s.at(1.0, v1), s.at(0.0, v1));
         }
      }
   }

   public static double[][] bezier(int steps, double[]... c) {
      if (c.length != 3 && c.length != 4) {
         throw new IllegalArgumentException("3 or 4 control points");
      }

      double[][] out = new double[steps + 1][];

      for (int i = 0; i <= steps; i++) {
         double t = (double)i / steps;
         double s = 1.0 - t;
         out[i] = c.length == 3
            ? bez(c[0], c[1], c[2], t)
            : new double[]{
               s * s * s * c[0][0] + 3.0 * s * s * t * c[1][0] + 3.0 * s * t * t * c[2][0] + t * t * t * c[3][0],
               s * s * s * c[0][1] + 3.0 * s * s * t * c[1][1] + 3.0 * s * t * t * c[2][1] + t * t * t * c[3][1],
               s * s * s * c[0][2] + 3.0 * s * s * t * c[1][2] + 3.0 * s * t * t * c[2][2] + t * t * t * c[3][2]
            };
      }

      return out;
   }

   public static double[] tangent(double[][] spine, int i) {
      return unit(sub(spine[Math.min(i + 1, spine.length - 1)], spine[Math.max(i - 1, 0)]));
   }

   public static void tube(Builder b, double[][] spine, DoubleUnaryOperator radius, int segs, Prim.Shade s, boolean capBase, boolean capTip) {
      int n = spine.length;
      if (n >= 2) {
         double[][] nn = new double[n][];
         double[][] bb = new double[n][];
         double[] cur = perp(tangent(spine, 0))[0];

         for (int i = 0; i < n; i++) {
            double[] t = tangent(spine, i);
            double k = dot(cur, t);
            cur = unit(new double[]{cur[0] - t[0] * k, cur[1] - t[1] * k, cur[2] - t[2] * k});
            nn[i] = cur;
            bb[i] = cross(t, cur);
         }

         for (int i = 0; i < n - 1; i++) {
            double v0 = i / (n - 1.0);
            double v1 = (i + 1) / (n - 1.0);
            double ra = radius.applyAsDouble(v0);
            double rb = radius.applyAsDouble(v1);

            for (int j = 0; j < segs; j++) {
               double t0 = (double)j / segs;
               double t1 = (double)(j + 1) / segs;
               double c0 = Math.cos(t0 * (Math.PI * 2));
               double s0 = Math.sin(t0 * (Math.PI * 2));
               double c1 = Math.cos(t1 * (Math.PI * 2));
               double s1 = Math.sin(t1 * (Math.PI * 2));
               b.quad(
                  madd(spine[i], nn[i], ra * c0, bb[i], ra * s0),
                  madd(spine[i], nn[i], ra * c1, bb[i], ra * s1),
                  madd(spine[i + 1], nn[i + 1], rb * c1, bb[i + 1], rb * s1),
                  madd(spine[i + 1], nn[i + 1], rb * c0, bb[i + 1], rb * s0),
                  s.at(t0, v0),
                  s.at(t1, v0),
                  s.at(t1, v1),
                  s.at(t0, v1)
               );
            }
         }

         if (capBase) {
            tubeCap(b, spine[0], nn[0], bb[0], radius.applyAsDouble(0.0), segs, s, 0.0);
         }

         if (capTip) {
            tubeCap(b, spine[n - 1], nn[n - 1], bb[n - 1], radius.applyAsDouble(1.0), segs, s, 1.0);
         }
      }
   }

   private static void tubeCap(Builder b, double[] c, double[] n, double[] bn, double r, int segs, Prim.Shade s, double v) {
      if (!(r < 1.0E-9)) {
         for (int j = 0; j < segs; j++) {
            double t0 = (double)j / segs;
            double t1 = (double)(j + 1) / segs;
            b.tri(
               c,
               madd(c, n, r * Math.cos(t0 * (Math.PI * 2)), bn, r * Math.sin(t0 * (Math.PI * 2))),
               madd(c, n, r * Math.cos(t1 * (Math.PI * 2)), bn, r * Math.sin(t1 * (Math.PI * 2))),
               s.at((t0 + t1) * 0.5, v),
               s.at(t0, v),
               s.at(t1, v)
            );
         }
      }
   }

   public static void taperedTube(Builder b, double[][] spine, double r0, double r1, int segs, Prim.Shade s, boolean caps) {
      tube(b, spine, v -> r0 + (r1 - r0) * v, segs, s, caps, caps);
   }

   public static void flatTube(Builder b, double[][] spine, double[] widthDir, DoubleUnaryOperator halfW, DoubleUnaryOperator halfD, DoubleUnaryOperator shift, int segs, Prim.Shade s) {
      int n = spine.length;
      if (n >= 2) {
         double[][] nn = new double[n][];
         double[][] bb = new double[n][];

         for (int i = 0; i < n; i++) {
            double[] t = tangent(spine, i);
            double k = dot(widthDir, t);
            double[] w = new double[]{widthDir[0] - t[0] * k, widthDir[1] - t[1] * k, widthDir[2] - t[2] * k};
            nn[i] = dot(w, w) < 1.0E-10 ? perp(t)[0] : unit(w);
            bb[i] = cross(nn[i], t);
         }

         for (int i = 0; i < n - 1; i++) {
            double v0 = i / (n - 1.0);
            double v1 = (i + 1) / (n - 1.0);
            double wa = halfW.applyAsDouble(v0);
            double da = halfD.applyAsDouble(v0);
            double sa = shift.applyAsDouble(v0);
            double wb = halfW.applyAsDouble(v1);
            double db = halfD.applyAsDouble(v1);
            double sb = shift.applyAsDouble(v1);

            for (int j = 0; j < segs; j++) {
               double t0 = (double)j / segs;
               double t1 = (double)(j + 1) / segs;
               double c0 = Math.cos(t0 * (Math.PI * 2));
               double s0 = Math.sin(t0 * (Math.PI * 2));
               double c1 = Math.cos(t1 * (Math.PI * 2));
               double s1 = Math.sin(t1 * (Math.PI * 2));
               b.quad(
                  madd(madd(spine[i], bb[i], sa, bb[i], 0.0), nn[i], wa * c0, bb[i], da * s0),
                  madd(madd(spine[i], bb[i], sa, bb[i], 0.0), nn[i], wa * c1, bb[i], da * s1),
                  madd(madd(spine[i + 1], bb[i + 1], sb, bb[i + 1], 0.0), nn[i + 1], wb * c1, bb[i + 1], db * s1),
                  madd(madd(spine[i + 1], bb[i + 1], sb, bb[i + 1], 0.0), nn[i + 1], wb * c0, bb[i + 1], db * s0),
                  s.at(t0, v0),
                  s.at(t1, v0),
                  s.at(t1, v1),
                  s.at(t0, v1)
               );
            }
         }
      }
   }

   public static void flatTube(Builder b, double[][] spine, double[] widthDir, DoubleUnaryOperator halfW, DoubleUnaryOperator halfD, int segs, Prim.Shade s) {
      flatTube(b, spine, widthDir, halfW, halfD, v -> 0.0, segs, s);
   }

   public static void spiralCone(Builder b, double y0, double r, double h, int ridges, double turns, double depth, int rings, int segs, Prim.Shade s) {
      for (int k = 0; k < rings; k++) {
         double v0 = (double)k / rings;
         double v1 = (double)(k + 1) / rings;

         for (int i = 0; i < segs; i++) {
            double t0 = (double)i / segs;
            double t1 = (double)(i + 1) / segs;
            b.quad(
               spiralPt(y0, r, h, ridges, turns, depth, t0, v0),
               spiralPt(y0, r, h, ridges, turns, depth, t1, v0),
               spiralPt(y0, r, h, ridges, turns, depth, t1, v1),
               spiralPt(y0, r, h, ridges, turns, depth, t0, v1),
               s.at(t0, v0),
               s.at(t1, v0),
               s.at(t1, v1),
               s.at(t0, v1)
            );
         }
      }
   }

   private static double[] spiralPt(double y0, double r, double h, int ridges, double turns, double depth, double u, double v) {
      double rad = r * (1.0 - v) * (1.0 + depth * Math.cos(ridges * u * (Math.PI * 2) - turns * (Math.PI * 2) * v));
      return P(rad * Math.cos(u * (Math.PI * 2)), y0 + h * v, rad * Math.sin(u * (Math.PI * 2)));
   }

   public static void branch(Builder b, Prim.BranchSpec spec, double[] base, double[] dir, double len, double r, Prim.BranchShade s) {
      limb(b, spec, base, unit(dir), len, r, 0, 1, s);
   }

   private static void limb(Builder b, Prim.BranchSpec sp, double[] base, double[] dir, double len, double r, int depth, int id, Prim.BranchShade s) {
      int steps = depth == 0 ? sp.steps : sp.childSteps;
      double step = len / steps;
      double[][] spine = new double[steps + 1][];
      double[][] tan = new double[steps + 1][];
      spine[0] = base;
      tan[0] = dir;
      double[] d = dir;

      for (int k = 1; k <= steps; k++) {
         d = unit(new double[]{d[0] + sp.bendDir[0] * sp.bend * step, d[1] + sp.bendDir[1] * sp.bend * step, d[2] + sp.bendDir[2] * sp.bend * step});
         tan[k] = d;
         spine[k] = madd(spine[k - 1], d, step, d, 0.0);
      }

      double rEnd = r * sp.taper;
      int sides = Math.max(3, sp.sides - depth);
      tube(b, spine, v -> r + (rEnd - r) * v, sides, (u, v) -> s.at(depth, u, v), false, true);
      boolean grows = depth < sp.maxDepth;
      double[] tEnd = tan[steps];
      if (grows) {
         for (int i = 0; i < sp.tines; i++) {
            double f = sp.tines == 1 ? (sp.tineFrom + sp.tineTo) * 0.5 : sp.tineFrom + (sp.tineTo - sp.tineFrom) * i / (sp.tines - 1.0);
            double pos = f * steps;
            int k0 = Math.min((int)pos, steps - 1);
            double fr = pos - k0;
            double[] p = lerp(spine[k0], spine[k0 + 1], fr);
            double[] t = unit(lerp(tan[k0], tan[k0 + 1], fr));
            double[][] nb = sideBasis(t, sp.sideRef);
            double jr = sp.jitter * (rnd(sp.seed * 977 + id * 31 + i * 7) - 0.5);
            double phi = sp.phases[(i + id) % sp.phases.length] + jr * 2.0;
            double[] cd = unit(madd(madd(new double[3], t, Math.cos(sp.spread), nb[0], Math.sin(sp.spread) * Math.cos(phi)), nb[1], Math.sin(sp.spread) * Math.sin(phi), nb[1], 0.0));
            double cl = len * sp.childLen * (1.0 + jr * 1.2);
            limb(b, sp, p, cd, cl, (r + (rEnd - r) * f) * sp.childRadius, depth + 1, id * 8 + i + 1, s);
         }

         if (sp.forkTip) {
            double[][] nb = sideBasis(tEnd, sp.sideRef);

            for (int q = -1; q <= 1; q += 2) {
               double[] cd = unit(madd(new double[3], tEnd, Math.cos(sp.tipSpread), nb[1], q * Math.sin(sp.tipSpread)));
               limb(b, sp, spine[steps], cd, len * sp.childLen * 0.9, rEnd * 0.95, depth + 1, id * 8 + 6 + (q + 1) / 2, s);
            }
         }
      }

      if ((!grows || !sp.forkTip) && sp.onTip != null) {
         sp.onTip.accept(spine[steps], depth);
      }
   }

   private static double[][] sideBasis(double[] t, double[] ref) {
      double k = dot(ref, t);
      double[] n = new double[]{ref[0] - t[0] * k, ref[1] - t[1] * k, ref[2] - t[2] * k};
      n = dot(n, n) < 1.0E-10 ? perp(t)[0] : unit(n);
      return new double[][]{n, cross(t, n)};
   }

   private static double facet(double ang) {
      return 0.78 + 0.22 * Math.cos(ang - 0.8);
   }

   @FunctionalInterface
   public interface BranchShade {
      int at(int var1, double var2, double var4);
   }

   public static final class BranchSpec {
      public int maxDepth = 1;
      public int tines = 3;
      public int steps = 6;
      public int childSteps = 4;
      public int sides = 5;
      public int seed = 1;
      public double tineFrom = 0.3;
      public double tineTo = 0.85;
      public double spread = 0.9;
      public double childLen = 0.55;
      public double childRadius = 0.72;
      public double taper = 0.3;
      public double bend = 0.0;
      public double tipSpread = 0.5;
      public double jitter = 0.0;
      public double[] bendDir = new double[]{0.0, 0.0, 0.0};
      public double[] sideRef = new double[]{0.0, 0.0, 1.0};
      public double[] phases = new double[]{0.0};
      public boolean forkTip = false;
      public BiConsumer<double[], Integer> onTip = null;
   }

   @FunctionalInterface
   public interface Shade {
      int at(double var1, double var3);
   }
}
