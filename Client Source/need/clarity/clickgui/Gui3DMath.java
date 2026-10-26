package need.clarity.clickgui;

public final class Gui3DMath {
   private Gui3DMath() {
   }

   public static void place(Gui3DMath.Frame f, float[] dir, float dist, float halfH, float aspect, float tilt, float swing, float lift, float push) {
      float[] d = norm((float[])dir.clone());
      float[] up = new float[]{0.0F, 1.0F, 0.0F};
      float[] right = cross(d, up);
      if (len(right) < 1.0E-4F) {
         right = new float[]{1.0F, 0.0F, 0.0F};
      }

      norm(right);
      float[] pu = norm(cross(right, d));
      double sw = Math.toRadians(swing);
      float cs = (float)Math.cos(sw);
      float sn = (float)Math.sin(sw);
      float[] r2 = add(scale(right, cs), scale(d, sn));
      float[] d2 = sub(scale(d, cs), scale(right, sn));
      double tl = Math.toRadians(tilt);
      float ct = (float)Math.cos(tl);
      float st = (float)Math.sin(tl);
      float[] u2 = add(scale(pu, ct), scale(d2, st));
      float D = dist + push;

      for (int i = 0; i < 3; i++) {
         f.c[i] = d[i] * D + pu[i] * lift;
         f.r[i] = r2[i] * halfH * aspect;
         f.u[i] = u2[i] * halfH;
      }
   }

   public static float[] pick(float[] invViewProj, float ndcX, float ndcY, Gui3DMath.Frame f) {
      float[] dir = unproject(invViewProj, ndcX, ndcY, -1.0F);
      if (dir == null) {
         return null;
      }

      float[] near = new float[]{0.0F, 0.0F, 0.0F};
      float[] n = cross(f.r, f.u);
      float denom = dot(dir, n);
      if (Math.abs(denom) < 1.0E-9F) {
         return null;
      }

      float t = dot(sub(f.c, near), n) / denom;
      if (t <= 0.0F) {
         return null;
      }

      float[] p = add(near, scale(dir, t));
      float[] q = sub(p, f.c);
      float rr = dot(f.r, f.r);
      float ru = dot(f.r, f.u);
      float uu = dot(f.u, f.u);
      float qr = dot(q, f.r);
      float qu = dot(q, f.u);
      float det = rr * uu - ru * ru;
      return Math.abs(det) < 1.0E-12F ? null : new float[]{(qr * uu - qu * ru) / det, (qu * rr - qr * ru) / det};
   }

   public static float[] project(float[] vp, float[] p) {
      float x = vp[0] * p[0] + vp[4] * p[1] + vp[8] * p[2] + vp[12];
      float y = vp[1] * p[0] + vp[5] * p[1] + vp[9] * p[2] + vp[13];
      float w = vp[3] * p[0] + vp[7] * p[1] + vp[11] * p[2] + vp[15];
      return w <= 1.0E-6F ? null : new float[]{x / w, y / w};
   }

   private static float[] unproject(float[] inv, float x, float y, float z) {
      float ox = inv[0] * x + inv[4] * y + inv[8] * z + inv[12];
      float oy = inv[1] * x + inv[5] * y + inv[9] * z + inv[13];
      float oz = inv[2] * x + inv[6] * y + inv[10] * z + inv[14];
      float ow = inv[3] * x + inv[7] * y + inv[11] * z + inv[15];
      return Math.abs(ow) < 1.0E-12F ? null : new float[]{ox / ow, oy / ow, oz / ow};
   }

   public static float[] invert(float[] m) {
      double[] a = new double[16];

      for (int i = 0; i < 16; i++) {
         a[i] = m[i];
      }

      double[] inv = new double[16];
      inv[0] = a[5] * a[10] * a[15] - a[5] * a[11] * a[14] - a[9] * a[6] * a[15] + a[9] * a[7] * a[14] + a[13] * a[6] * a[11] - a[13] * a[7] * a[10];
      inv[4] = -a[4] * a[10] * a[15] + a[4] * a[11] * a[14] + a[8] * a[6] * a[15] - a[8] * a[7] * a[14] - a[12] * a[6] * a[11] + a[12] * a[7] * a[10];
      inv[8] = a[4] * a[9] * a[15] - a[4] * a[11] * a[13] - a[8] * a[5] * a[15] + a[8] * a[7] * a[13] + a[12] * a[5] * a[11] - a[12] * a[7] * a[9];
      inv[12] = -a[4] * a[9] * a[14] + a[4] * a[10] * a[13] + a[8] * a[5] * a[14] - a[8] * a[6] * a[13] - a[12] * a[5] * a[10] + a[12] * a[6] * a[9];
      inv[1] = -a[1] * a[10] * a[15] + a[1] * a[11] * a[14] + a[9] * a[2] * a[15] - a[9] * a[3] * a[14] - a[13] * a[2] * a[11] + a[13] * a[3] * a[10];
      inv[5] = a[0] * a[10] * a[15] - a[0] * a[11] * a[14] - a[8] * a[2] * a[15] + a[8] * a[3] * a[14] + a[12] * a[2] * a[11] - a[12] * a[3] * a[10];
      inv[9] = -a[0] * a[9] * a[15] + a[0] * a[11] * a[13] + a[8] * a[1] * a[15] - a[8] * a[3] * a[13] - a[12] * a[1] * a[11] + a[12] * a[3] * a[9];
      inv[13] = a[0] * a[9] * a[14] - a[0] * a[10] * a[13] - a[8] * a[1] * a[14] + a[8] * a[2] * a[13] + a[12] * a[1] * a[10] - a[12] * a[2] * a[9];
      inv[2] = a[1] * a[6] * a[15] - a[1] * a[7] * a[14] - a[5] * a[2] * a[15] + a[5] * a[3] * a[14] + a[13] * a[2] * a[7] - a[13] * a[3] * a[6];
      inv[6] = -a[0] * a[6] * a[15] + a[0] * a[7] * a[14] + a[4] * a[2] * a[15] - a[4] * a[3] * a[14] - a[12] * a[2] * a[7] + a[12] * a[3] * a[6];
      inv[10] = a[0] * a[5] * a[15] - a[0] * a[7] * a[13] - a[4] * a[1] * a[15] + a[4] * a[3] * a[13] + a[12] * a[1] * a[7] - a[12] * a[3] * a[5];
      inv[14] = -a[0] * a[5] * a[14] + a[0] * a[6] * a[13] + a[4] * a[1] * a[14] - a[4] * a[2] * a[13] - a[12] * a[1] * a[6] + a[12] * a[2] * a[5];
      inv[3] = -a[1] * a[6] * a[11] + a[1] * a[7] * a[10] + a[5] * a[2] * a[11] - a[5] * a[3] * a[10] - a[9] * a[2] * a[7] + a[9] * a[3] * a[6];
      inv[7] = a[0] * a[6] * a[11] - a[0] * a[7] * a[10] - a[4] * a[2] * a[11] + a[4] * a[3] * a[10] + a[8] * a[2] * a[7] - a[8] * a[3] * a[6];
      inv[11] = -a[0] * a[5] * a[11] + a[0] * a[7] * a[9] + a[4] * a[1] * a[11] - a[4] * a[3] * a[9] - a[8] * a[1] * a[7] + a[8] * a[3] * a[5];
      inv[15] = a[0] * a[5] * a[10] - a[0] * a[6] * a[9] - a[4] * a[1] * a[10] + a[4] * a[2] * a[9] + a[8] * a[1] * a[6] - a[8] * a[2] * a[5];
      double det = a[0] * inv[0] + a[1] * inv[4] + a[2] * inv[8] + a[3] * inv[12];
      if (Math.abs(det) < 1.0E-30) {
         return null;
      }

      float[] out = new float[16];

      for (int i = 0; i < 16; i++) {
         out[i] = (float)(inv[i] / det);
      }

      return out;
   }

   static float[] cross(float[] a, float[] b) {
      return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
   }

   static float dot(float[] a, float[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   static float len(float[] a) {
      return (float)Math.sqrt(dot(a, a));
   }

   static float[] norm(float[] a) {
      float l = len(a);
      if (l > 1.0E-9F) {
         a[0] /= l;
         a[1] /= l;
         a[2] /= l;
      }

      return a;
   }

   static float[] add(float[] a, float[] b) {
      return new float[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
   }

   static float[] sub(float[] a, float[] b) {
      return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
   }

   static float[] scale(float[] a, float s) {
      return new float[]{a[0] * s, a[1] * s, a[2] * s};
   }

   public static final class Frame {
      public final float[] c = new float[3];
      public final float[] r = new float[3];
      public final float[] u = new float[3];
   }
}
