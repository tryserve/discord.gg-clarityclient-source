package need.clarity.modules.visual.cosmetics;

import java.util.ArrayDeque;
import java.util.function.Predicate;

public class Builder {
   public final int c1;
   public final int c2;
   public final double time;
   public final boolean animate;
   public double lift;
   public double[] view;
   public Predicate<double[][]> quadFilter;
   protected final CosmeticSink sink;
   protected final double opacity;
   private Attach.Frame frame = Attach.Frame.identity();
   private double scale = 1.0;
   private boolean mirror = false;
   private double[] m = identity();
   private final ArrayDeque<double[]> stack = new ArrayDeque<>();

   public Builder(CosmeticSink sink, int c1, int c2, double opacity, double time, boolean animate) {
      this.sink = sink;
      this.c1 = c1 | 0xFF000000;
      this.c2 = c2 | 0xFF000000;
      this.opacity = opacity;
      this.time = time;
      this.animate = animate;
   }

   private static double[] identity() {
      return new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0};
   }

   public void frame(Attach.Frame f, double scale) {
      this.frame = f;
      this.scale = scale;
      this.m = identity();
      this.stack.clear();
      this.mirror = false;
   }

   public double scale() {
      return this.scale;
   }

   public Builder mirrorU(boolean on) {
      this.mirror = on;
      return this;
   }

   public Builder push() {
      this.stack.push((double[])this.m.clone());
      return this;
   }

   public Builder pop() {
      this.m = this.stack.pop();
      return this;
   }

   public Builder translate(double du, double dv, double dw) {
      this.m[9] = this.m[9] + (this.m[0] * du + this.m[1] * dv + this.m[2] * dw);
      this.m[10] = this.m[10] + (this.m[3] * du + this.m[4] * dv + this.m[5] * dw);
      this.m[11] = this.m[11] + (this.m[6] * du + this.m[7] * dv + this.m[8] * dw);
      return this;
   }

   public Builder scaleLocal(double k) {
      for (int i = 0; i < 9; i++) {
         this.m[i] = this.m[i] * k;
      }

      return this;
   }

   public Builder rotateY(double rad) {
      double c = Math.cos(rad);
      double s = Math.sin(rad);
      return this.rotate(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
   }

   public Builder rotateX(double rad) {
      double c = Math.cos(rad);
      double s = Math.sin(rad);
      return this.rotate(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
   }

   public Builder rotateZ(double rad) {
      double c = Math.cos(rad);
      double s = Math.sin(rad);
      return this.rotate(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0);
   }

   private Builder rotate(double... r) {
      double[] o = new double[12];

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            o[i * 3 + j] = this.m[i * 3] * r[j] + this.m[i * 3 + 1] * r[3 + j] + this.m[i * 3 + 2] * r[6 + j];
         }
      }

      o[9] = this.m[9];
      o[10] = this.m[10];
      o[11] = this.m[11];
      this.m = o;
      return this;
   }

   public double[] viewLocal() {
      if (this.view != null && this.scale != 0.0) {
         double dx = this.view[0] - this.frame.ox;
         double dy = this.view[1] - this.frame.oy;
         double dz = this.view[2] - this.frame.oz;
         double k = 1.0 / this.scale;
         double x = (this.frame.rx * dx + this.frame.ry * dy + this.frame.rz * dz) * k;
         double y = (this.frame.ux * dx + this.frame.uy * dy + this.frame.uz * dz) * k;
         double z = (this.frame.fx * dx + this.frame.fy * dy + this.frame.fz * dz) * k;
         if (this.mirror) {
            x = -x;
         }

         x -= this.m[9];
         y -= this.m[10];
         z -= this.m[11];
         double a = this.m[0];
         double b = this.m[1];
         double c = this.m[2];
         double d = this.m[3];
         double e = this.m[4];
         double f = this.m[5];
         double g = this.m[6];
         double h = this.m[7];
         double i = this.m[8];
         double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
         if (Math.abs(det) < 1.0E-12) {
            return null;
         }

         double id = 1.0 / det;
         return new double[]{
            ((e * i - f * h) * x + (c * h - b * i) * y + (b * f - c * e) * z) * id,
            ((f * g - d * i) * x + (a * i - c * g) * y + (c * d - a * f) * z) * id,
            ((d * h - e * g) * x + (b * g - a * h) * y + (a * e - b * d) * z) * id
         };
      } else {
         return null;
      }
   }

   public double facing(double[] p, double[] n) {
      double[] v = this.viewLocal();
      if (v == null) {
         return 0.5;
      }

      double dx = v[0] - p[0];
      double dy = v[1] - p[1];
      double dz = v[2] - p[2];
      double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double nl = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
      return !(dl < 1.0E-9) && !(nl < 1.0E-9) ? Math.abs((dx * n[0] + dy * n[1] + dz * n[2]) / (dl * nl)) : 0.5;
   }

   public double[] map(double[] p) {
      double x = this.m[0] * p[0] + this.m[1] * p[1] + this.m[2] * p[2] + this.m[9];
      double y = this.m[3] * p[0] + this.m[4] * p[1] + this.m[5] * p[2] + this.m[10];
      double z = this.m[6] * p[0] + this.m[7] * p[1] + this.m[8] * p[2] + this.m[11];
      if (this.mirror) {
         x = -x;
      }

      return this.frame.map(x * this.scale, y * this.scale, z * this.scale);
   }

   protected final int op(int c) {
      return this.opacity >= 0.999 ? c : Col.alpha(c, this.opacity);
   }

   public void quad(double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
      double[] A = this.map(a);
      double[] B = this.map(b);
      double[] C = this.map(c);
      double[] D = this.map(d);
      if (this.quadFilter == null || this.quadFilter.test(new double[][]{A, B, C, D})) {
         this.sink.quad(A, B, C, D, this.op(ca), this.op(cb), this.op(cc), this.op(cd));
      }
   }

   public void quad(double[] a, double[] b, double[] c, double[] d, int col) {
      this.quad(a, b, c, d, col, col, col, col);
   }

   public void tri(double[] a, double[] b, double[] c, int ca, int cb, int cc) {
      this.quad(a, b, c, c, ca, cb, cc, cc);
   }

   public void tri(double[] a, double[] b, double[] c, int col) {
      this.tri(a, b, c, col, col, col);
   }

   public void bar(double[] p0, double[] p1, double w0, double w1, int c0, int c1) {
      double du = p1[0] - p0[0];
      double dv = p1[1] - p0[1];
      double len = Math.sqrt(du * du + dv * dv);
      if (!(len < 1.0E-9)) {
         double nu = -dv / len;
         double nv = du / len;

         for (int s = -1; s <= 1; s += 2) {
            double e = this.lift + 0.005 * s;
            this.quad(
               Prim.P(p0[0] + nu * w0 * 0.5, p0[1] + nv * w0 * 0.5, p0[2] + e),
               Prim.P(p0[0] - nu * w0 * 0.5, p0[1] - nv * w0 * 0.5, p0[2] + e),
               Prim.P(p1[0] - nu * w1 * 0.5, p1[1] - nv * w1 * 0.5, p1[2] + e),
               Prim.P(p1[0] + nu * w1 * 0.5, p1[1] + nv * w1 * 0.5, p1[2] + e),
               c0,
               c0,
               c1,
               c1
            );
         }
      }
   }
}
