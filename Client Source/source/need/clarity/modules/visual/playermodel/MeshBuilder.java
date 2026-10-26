package need.clarity.modules.visual.playermodel;

import java.util.Arrays;

final class MeshBuilder {
   private static final double TAU = Math.PI * 2;
   private double hull = 0.0;
   private int hullColor = -16119286;
   private final double[] m = Mat.identity();
   private float[] pos = new float[12288];
   private int[] col = new int[4096];
   private int n;
   private final double[] qp = new double[12];
   private final double[] qn = new double[12];
   private final int[] qc = new int[4];

   void reset() {
      this.n = 0;
      this.hull = 0.0;
      Mat.identity(this.m);
   }

   Mesh build() {
      Mesh mesh = this.n == 0 ? Mesh.EMPTY : new Mesh(Arrays.copyOf(this.pos, this.n * 3), Arrays.copyOf(this.col, this.n));
      this.n = 0;
      return mesh;
   }

   void outlineColor(int c) {
      this.hullColor = c;
   }

   void translate(double x, double y, double z) {
      Mat.translate(this.m, x, y, z);
   }

   void rotX(double a) {
      Mat.rotX(this.m, a);
   }

   void rotY(double a) {
      Mat.rotY(this.m, a);
   }

   void rotZ(double a) {
      Mat.rotZ(this.m, a);
   }

   void resetTransform() {
      Mat.identity(this.m);
   }

   void setTransform(double[] t) {
      Mat.copy(t, this.m);
   }

   void outlined(double t, Runnable shape) {
      this.hull = t;

      try {
         shape.run();
      } finally {
         this.hull = 0.0;
      }

      shape.run();
   }

   private void put(int i, double x, double y, double z, double nx, double ny, double nz, int c) {
      int o = i * 3;
      this.qp[o] = x;
      this.qp[o + 1] = y;
      this.qp[o + 2] = z;
      this.qn[o] = nx;
      this.qn[o + 1] = ny;
      this.qn[o + 2] = nz;
      this.qc[i] = c;
   }

   private void commit() {
      double ax = this.qp[6] - this.qp[0];
      double ay = this.qp[7] - this.qp[1];
      double az = this.qp[8] - this.qp[2];
      double bx = this.qp[9] - this.qp[3];
      double by = this.qp[10] - this.qp[4];
      double bz = this.qp[11] - this.qp[5];
      double fx = ay * bz - az * by;
      double fy = az * bx - ax * bz;
      double fz = ax * by - ay * bx;
      if (!(fx * fx + fy * fy + fz * fz < 1.0E-22)) {
         double sx = this.qn[0] + this.qn[3] + this.qn[6] + this.qn[9];
         double sy = this.qn[1] + this.qn[4] + this.qn[7] + this.qn[10];
         double sz = this.qn[2] + this.qn[5] + this.qn[8] + this.qn[11];
         boolean outward = fx * sx + fy * sy + fz * sz >= 0.0;
         boolean wantOutward = this.hull > 0.0;
         if (outward == wantOutward) {
            this.emit(0);
            this.emit(1);
            this.emit(2);
            this.emit(3);
         } else {
            this.emit(0);
            this.emit(3);
            this.emit(2);
            this.emit(1);
         }
      }
   }

   private void emit(int i) {
      if (this.n == this.col.length) {
         this.pos = Arrays.copyOf(this.pos, this.n * 6);
         this.col = Arrays.copyOf(this.col, this.n * 2);
      }

      int o = i * 3;
      double x = this.qp[o];
      double y = this.qp[o + 1];
      double z = this.qp[o + 2];
      this.pos[this.n * 3] = (float)Mat.px(this.m, x, y, z);
      this.pos[this.n * 3 + 1] = (float)Mat.py(this.m, x, y, z);
      this.pos[this.n * 3 + 2] = (float)Mat.pz(this.m, x, y, z);
      this.col[this.n] = this.hull > 0.0 ? this.hullColor : this.qc[i];
      this.n++;
   }

   void sphere(double cx, double cy, double cz, double r, int lon, int lat, int c) {
      this.ellipsoid(cx, cy, cz, r, r, r, lon, lat, c);
   }

   void ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz, int lon, int lat, int c) {
      double h = this.hull;
      rx += h;
      ry += h;
      rz += h;

      for (int k = 0; k < lat; k++) {
         double f0 = (-Math.PI / 2) + Math.PI * k / lat;
         double f1 = (-Math.PI / 2) + Math.PI * (k + 1) / lat;
         double cf0 = Math.cos(f0);
         double sf0 = Math.sin(f0);
         double cf1 = Math.cos(f1);
         double sf1 = Math.sin(f1);

         for (int i = 0; i < lon; i++) {
            double t0 = i * (Math.PI * 2) / lon;
            double t1 = (i + 1) * (Math.PI * 2) / lon;
            double c0 = Math.cos(t0);
            double s0 = Math.sin(t0);
            double c1 = Math.cos(t1);
            double s1 = Math.sin(t1);
            this.ePt(0, cx, cy, cz, rx, ry, rz, cf0, sf0, c0, s0, c);
            this.ePt(1, cx, cy, cz, rx, ry, rz, cf0, sf0, c1, s1, c);
            this.ePt(2, cx, cy, cz, rx, ry, rz, cf1, sf1, c1, s1, c);
            this.ePt(3, cx, cy, cz, rx, ry, rz, cf1, sf1, c0, s0, c);
            this.commit();
         }
      }
   }

   private void ePt(int i, double cx, double cy, double cz, double rx, double ry, double rz, double cf, double sf, double ct, double st, int c) {
      double ux = cf * ct;
      double uy = sf;
      double uz = cf * st;
      this.put(i, cx + rx * ux, cy + ry * uy, cz + rz * uz, ux / rx, uy / ry, uz / rz, c);
   }

   void cylinder(double y0, double y1, double r, int segs, int c) {
      double h = this.hull;
      y0 -= h;
      y1 += h;
      r += h;

      for (int i = 0; i < segs; i++) {
         double t0 = i * (Math.PI * 2) / segs;
         double t1 = (i + 1) * (Math.PI * 2) / segs;
         double c0 = Math.cos(t0);
         double s0 = Math.sin(t0);
         double c1 = Math.cos(t1);
         double s1 = Math.sin(t1);
         this.put(0, r * c0, y0, r * s0, c0, 0.0, s0, c);
         this.put(1, r * c1, y0, r * s1, c1, 0.0, s1, c);
         this.put(2, r * c1, y1, r * s1, c1, 0.0, s1, c);
         this.put(3, r * c0, y1, r * s0, c0, 0.0, s0, c);
         this.commit();
         this.put(0, 0.0, y0, 0.0, 0.0, -1.0, 0.0, c);
         this.put(1, r * c1, y0, r * s1, 0.0, -1.0, 0.0, c);
         this.put(2, r * c0, y0, r * s0, 0.0, -1.0, 0.0, c);
         this.put(3, r * c0, y0, r * s0, 0.0, -1.0, 0.0, c);
         this.commit();
         this.put(0, 0.0, y1, 0.0, 0.0, 1.0, 0.0, c);
         this.put(1, r * c0, y1, r * s0, 0.0, 1.0, 0.0, c);
         this.put(2, r * c1, y1, r * s1, 0.0, 1.0, 0.0, c);
         this.put(3, r * c1, y1, r * s1, 0.0, 1.0, 0.0, c);
         this.commit();
      }
   }

   void frustum(double y0, double r0, double y1, double r1, int segs, int c) {
      double h = this.hull;
      y0 -= h;
      y1 += h;
      r0 += h;
      r1 += h;
      double ny = (r0 - r1) / Math.max(1.0E-6, y1 - y0);

      for (int i = 0; i < segs; i++) {
         double t0 = i * (Math.PI * 2) / segs;
         double t1 = (i + 1) * (Math.PI * 2) / segs;
         double c0 = Math.cos(t0);
         double s0 = Math.sin(t0);
         double c1 = Math.cos(t1);
         double s1 = Math.sin(t1);
         this.put(0, r0 * c0, y0, r0 * s0, c0, ny, s0, c);
         this.put(1, r0 * c1, y0, r0 * s1, c1, ny, s1, c);
         this.put(2, r1 * c1, y1, r1 * s1, c1, ny, s1, c);
         this.put(3, r1 * c0, y1, r1 * s0, c0, ny, s0, c);
         this.commit();
         this.put(0, 0.0, y0, 0.0, 0.0, -1.0, 0.0, c);
         this.put(1, r0 * c1, y0, r0 * s1, 0.0, -1.0, 0.0, c);
         this.put(2, r0 * c0, y0, r0 * s0, 0.0, -1.0, 0.0, c);
         this.put(3, r0 * c0, y0, r0 * s0, 0.0, -1.0, 0.0, c);
         this.commit();
         this.put(0, 0.0, y1, 0.0, 0.0, 1.0, 0.0, c);
         this.put(1, r1 * c0, y1, r1 * s0, 0.0, 1.0, 0.0, c);
         this.put(2, r1 * c1, y1, r1 * s1, 0.0, 1.0, 0.0, c);
         this.put(3, r1 * c1, y1, r1 * s1, 0.0, 1.0, 0.0, c);
         this.commit();
      }
   }

   void strip(double[][] spine, double w, int c) {
      for (int i = 0; i + 1 < spine.length; i++) {
         double[] a = spine[i];
         double[] b = spine[i + 1];
         this.put(0, a[0] - w / 2.0, a[1], a[2], 0.0, 0.0, 1.0, c);
         this.put(1, a[0] + w / 2.0, a[1], a[2], 0.0, 0.0, 1.0, c);
         this.put(2, b[0] + w / 2.0, b[1], b[2], 0.0, 0.0, 1.0, c);
         this.put(3, b[0] - w / 2.0, b[1], b[2], 0.0, 0.0, 1.0, c);
         this.commit();
      }
   }
}
