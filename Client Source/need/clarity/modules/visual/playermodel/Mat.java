package need.clarity.modules.visual.playermodel;

public final class Mat {
   private static final double[] TMP = new double[12];

   private Mat() {
   }

   public static double[] identity() {
      return new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
   }

   public static void identity(double[] m) {
      m[0] = 1.0;
      m[1] = 0.0;
      m[2] = 0.0;
      m[3] = 0.0;
      m[4] = 0.0;
      m[5] = 1.0;
      m[6] = 0.0;
      m[7] = 0.0;
      m[8] = 0.0;
      m[9] = 0.0;
      m[10] = 1.0;
      m[11] = 0.0;
   }

   public static void copy(double[] src, double[] dst) {
      System.arraycopy(src, 0, dst, 0, 12);
   }

   public static void mul(double[] a, double[] b, double[] out) {
      double[] t = TMP;

      for (int r = 0; r < 3; r++) {
         int o = r * 4;
         double a0 = a[o];
         double a1 = a[o + 1];
         double a2 = a[o + 2];
         t[o] = a0 * b[0] + a1 * b[4] + a2 * b[8];
         t[o + 1] = a0 * b[1] + a1 * b[5] + a2 * b[9];
         t[o + 2] = a0 * b[2] + a1 * b[6] + a2 * b[10];
         t[o + 3] = a0 * b[3] + a1 * b[7] + a2 * b[11] + a[o + 3];
      }

      System.arraycopy(t, 0, out, 0, 12);
   }

   public static void translate(double[] m, double x, double y, double z) {
      m[3] += m[0] * x + m[1] * y + m[2] * z;
      m[7] += m[4] * x + m[5] * y + m[6] * z;
      m[11] += m[8] * x + m[9] * y + m[10] * z;
   }

   public static void scale(double[] m, double s) {
      m[0] *= s;
      m[1] *= s;
      m[2] *= s;
      m[4] *= s;
      m[5] *= s;
      m[6] *= s;
      m[8] *= s;
      m[9] *= s;
      m[10] *= s;
   }

   public static void rotX(double[] m, double a) {
      if (a != 0.0) {
         double c = Math.cos(a);
         double s = Math.sin(a);

         for (int r = 0; r < 12; r += 4) {
            double y = m[r + 1];
            double z = m[r + 2];
            m[r + 1] = y * c + z * s;
            m[r + 2] = -y * s + z * c;
         }
      }
   }

   public static void rotY(double[] m, double a) {
      if (a != 0.0) {
         double c = Math.cos(a);
         double s = Math.sin(a);

         for (int r = 0; r < 12; r += 4) {
            double x = m[r];
            double z = m[r + 2];
            m[r] = x * c - z * s;
            m[r + 2] = x * s + z * c;
         }
      }
   }

   public static void rotZ(double[] m, double a) {
      if (a != 0.0) {
         double c = Math.cos(a);
         double s = Math.sin(a);

         for (int r = 0; r < 12; r += 4) {
            double x = m[r];
            double y = m[r + 1];
            m[r] = x * c + y * s;
            m[r + 1] = -x * s + y * c;
         }
      }
   }

   public static void setTranslation(double[] m, double x, double y, double z) {
      identity(m);
      m[3] = x;
      m[7] = y;
      m[11] = z;
   }

   public static double px(double[] m, double x, double y, double z) {
      return m[0] * x + m[1] * y + m[2] * z + m[3];
   }

   public static double py(double[] m, double x, double y, double z) {
      return m[4] * x + m[5] * y + m[6] * z + m[7];
   }

   public static double pz(double[] m, double x, double y, double z) {
      return m[8] * x + m[9] * y + m[10] * z + m[11];
   }
}
