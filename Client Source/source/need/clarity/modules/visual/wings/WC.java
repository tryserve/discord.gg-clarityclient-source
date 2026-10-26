package need.clarity.modules.visual.wings;

import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Prim;

final class WC {
   static final int WHITE = -1;

   private WC() {
   }

   static int a(int c) {
      return Col.a(c);
   }

   static int r(int c) {
      return Col.r(c);
   }

   static int g(int c) {
      return Col.g(c);
   }

   static int b(int c) {
      return Col.b(c);
   }

   static int argb(int a, int r, int g, int b) {
      return Col.argb(a, r, g, b);
   }

   static int clamp(int v) {
      return Col.clamp(v);
   }

   static double clamp01(double v) {
      return Col.clamp01(v);
   }

   static int mix(int c0, int c1, double t) {
      return Col.mix(c0, c1, t);
   }

   static int alpha(int c, double k) {
      return Col.alpha(c, k);
   }

   static int withA(int c, double a) {
      return Col.withA(c, a);
   }

   static int shade(int c, double k) {
      return Col.shade(c, k);
   }

   static int tint(int c, double t) {
      return Col.tint(c, t);
   }

   static int dark(int c, double t) {
      return Col.dark(c, t);
   }

   static double[] P(double u, double v, double w) {
      return Prim.P(u, v, w);
   }

   static double[] lerp(double[] p, double[] q, double t) {
      return Prim.lerp(p, q, t);
   }

   static double[] add(double[] p, double du, double dv, double dw) {
      return Prim.add(p, du, dv, dw);
   }

   static double[] bez(double[] p0, double[] p1, double[] p2, double t) {
      return Prim.bez(p0, p1, p2, t);
   }

   static double sm(double t) {
      return Prim.sm(t);
   }

   static double rnd(int seed) {
      return Prim.rnd(seed);
   }
}
