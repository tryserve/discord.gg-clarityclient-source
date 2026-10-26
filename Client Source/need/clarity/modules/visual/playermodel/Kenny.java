package need.clarity.modules.visual.playermodel;

public final class Kenny {
   private static final int ORANGE = -423139;
   private static final int ORANGE_RING = -2594292;
   private static final int BROWN = -9551068;
   private static final int SKIN = -405080;
   private static final int WHITE = -1;
   private static final int BLACK = -16119286;
   private static final double T = 0.024;
   private static final double HIP = 0.27;
   private static final double LEG_X = 0.1;
   private static final double ARM_X = 0.27;
   private static final double SHOULDER = 0.3;
   private static final double NECK = 0.4;
   private static final double HOOD_Y = 0.28;
   private static final double LEAN = 0.4;
   private static final double MITTEN = 0.3;
   private static final double ROLL = 0.28;
   public static final int TORSO = 0;
   public static final int HEAD = 1;
   public static final int ARM_R = 2;
   public static final int ARM_L = 3;
   public static final int LEG_R = 4;
   public static final int LEG_L = 5;
   public static final int PARTS = 6;
   private static final int M_TORSO = 0;
   private static final int M_HOOD = 1;
   private static final int M_FACE = 2;
   private static final int M_ARM = 3;
   private static final int M_LEG = 4;
   private static final int MESHES = 5;
   static final double FACE_MIN_Z = 0.3;
   static final double HOOD_LIFT = 0.28;
   private static Mesh[] meshes;
   static final double[][] ANCHOR = new double[][]{{0.0, 0.47000000000000003, 0.0}, null, {0.27, 0.47000000000000003, 0.0}, {-0.27, 0.47000000000000003, 0.0}, {0.1, 0.13, 0.0}, {-0.1, 0.13, 0.0}};

   private Kenny() {
   }

   public static Mesh[] meshes() {
      Mesh[] ms = meshes;
      if (ms != null) {
         return ms;
      }

      MeshBuilder b = new MeshBuilder();
      b.outlineColor(-16119286);
      ms = new Mesh[5];
      b.reset();
      double cy = 0.19;
      double ry = 0.24;
      double rz = 0.21;
      b.outlined(0.024, () -> b.ellipsoid(0.0, cy, 0.0, 0.27, ry, rz, 20, 12, -423139));
      int n = 8;
      double[][] spine = new double[n + 1][];

      for (int i = 0; i <= n; i++) {
         double y = cy - 0.2 + 0.36 * i / n;
         double k = (y - cy) / ry;
         spine[i] = new double[]{0.0, y, rz * Math.sqrt(Math.max(0.0, 1.0 - k * k)) + 0.006};
      }

      b.strip(spine, 0.014, -16119286);
      ms[0] = b.build();
      b.reset();
      b.translate(0.0, 0.28, 0.0);
      b.outlined(0.024, () -> b.ellipsoid(0.0, 0.0, 0.0, 0.38, 0.36, 0.36, 24, 14, -423139));
      ms[1] = b.build();
      b.reset();
      b.translate(0.0, 0.28, 0.0);
      double fy = -0.01;
      b.outlined(0.016, () -> b.ellipsoid(0.0, fy, 0.27, 0.255, 0.175, 0.095, 18, 10, -2594292));
      b.outlined(0.014, () -> b.ellipsoid(0.0, fy, 0.29, 0.185, 0.12, 0.09, 16, 10, -405080));

      for (int s = -1; s <= 1; s += 2) {
         double x = s * 0.088;
         b.outlined(0.01, () -> b.ellipsoid(x, fy + 0.005, 0.31, 0.082, 0.088, 0.07, 12, 8, -1));
      }

      for (int s = -1; s <= 1; s += 2) {
         b.sphere(s * 0.068, fy + 0.005, 0.376, 0.02, 8, 5, -16119286);
      }

      ms[2] = b.build();
      b.reset();
      b.outlined(0.024, () -> b.ellipsoid(0.0, -0.13, 0.0, 0.075, 0.155, 0.075, 12, 8, -423139));
      b.outlined(0.024, () -> b.sphere(0.0, -0.3, 0.0, 0.085, 12, 8, -9551068));
      ms[3] = b.build();
      b.reset();
      b.outlined(0.024, () -> b.cylinder(-0.19, 0.02, 0.09, 12, -423139));
      b.outlined(0.024, () -> b.ellipsoid(0.0, -0.215, 0.04, 0.1, 0.055, 0.15, 12, 8, -9551068));
      ms[4] = b.build();
      meshes = ms;
      return ms;
   }

   public static void pose(double walkPhase, double walkAmp, double attack, boolean sneaking, double headYaw, double headPitch, double[][] out) {
      double stride = Math.sin(walkPhase) * walkAmp * 0.7;
      double lean = sneaking ? 0.4 : 0.0;
      double swing = Math.sin(attack * Math.PI);
      double[] torso = out[0];
      Mat.setTranslation(torso, 0.0, 0.27, 0.0);
      Mat.rotX(torso, lean);
      double[] head = out[1];
      Mat.setTranslation(head, 0.0, 0.27 + 0.4 * Math.cos(lean), 0.4 * Math.sin(lean));
      Mat.rotY(head, headYaw);
      Mat.rotX(head, headPitch);
      arm(torso, out[2], 0.27, 0.28, -stride - swing * 1.6);
      arm(torso, out[3], -0.27, -0.28, stride);
      Mat.setTranslation(out[4], 0.1, 0.27, 0.0);
      Mat.rotX(out[4], stride);
      Mat.setTranslation(out[5], -0.1, 0.27, 0.0);
      Mat.rotX(out[5], -stride);
   }

   private static void arm(double[] torso, double[] a, double u, double roll, double angle) {
      Mat.copy(torso, a);
      Mat.translate(a, u, 0.3, 0.0);
      Mat.rotZ(a, roll);
      Mat.rotX(a, angle);
   }

   public static void hand(double[] arm, double[] out) {
      out[0] = Mat.px(arm, 0.0, -0.3, 0.0);
      out[1] = Mat.py(arm, 0.0, -0.3, 0.0);
      out[2] = Mat.pz(arm, 0.0, -0.3, 0.0);
      out[3] = arm[0];
      out[4] = arm[4];
      out[5] = arm[8];
      out[6] = arm[1];
      out[7] = arm[5];
      out[8] = arm[9];
      out[9] = arm[2];
      out[10] = arm[6];
      out[11] = arm[10];
   }

   static Mesh mesh(int part) {
      Mesh[] ms = meshes();

      return switch (part) {
         case 0 -> ms[0];
         case 1 -> ms[1];
         case 2, 3 -> ms[3];
         default -> ms[4];
      };
   }

   static Mesh face() {
      return meshes()[2];
   }
}
