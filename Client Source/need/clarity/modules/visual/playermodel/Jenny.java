package need.clarity.modules.visual.playermodel;

public final class Jenny {
   private static final int DRESS = -6587414;
   private static final int DRESS_DARK = -8758325;
   private static final int COLLAR = -1;
   private static final int HAIR = -10864603;
   private static final int SKIN = -405080;
   private static final int BOW = -36957;
   private static final int BOW_KNOT = -1945978;
   private static final int BLUSH = -24912;
   private static final int SOCK = -723720;
   private static final int SHOE = -8769979;
   private static final int WHITE = -1;
   private static final int BLACK = -16119286;
   private static final int MOUTH = -5228465;
   private static final double T = 0.022;
   private static final double HIP = 0.27;
   private static final double LEG_X = 0.09;
   private static final double ARM_X = 0.19;
   private static final double SHOULDER = 0.31;
   private static final double NECK = 0.38;
   private static final double HEAD_Y = 0.3;
   private static final double LEAN = 0.35;
   private static final double HAND = 0.28;
   private static final double ROLL = 0.22;
   static final double FACE_MIN_Z = 0.22;
   private static final int M_TORSO = 0;
   private static final int M_HEAD = 1;
   private static final int M_FACE = 2;
   private static final int M_ARM = 3;
   private static final int M_LEG = 4;
   private static final int MESHES = 5;
   private static Mesh[] meshes;
   static final double[][] ANCHOR = new double[][]{{0.0, 0.42000000000000004, 0.0}, null, {0.19, 0.47000000000000003, 0.0}, {-0.19, 0.47000000000000003, 0.0}, {0.09, 0.13, 0.0}, {-0.09, 0.13, 0.0}};
   static final RigModel MODEL = new RigModel() {
      @Override
      public Mesh mesh(int part) {
         return Jenny.mesh(part);
      }

      @Override
      public Mesh face() {
         return Jenny.meshes()[2];
      }

      @Override
      public double[] anchor(int part) {
         return Jenny.ANCHOR[part];
      }

      @Override
      public double faceMinZ() {
         return 0.22;
      }

      @Override
      public void pose(double walkPhase, double walkAmp, double attack, boolean sneaking, double headYaw, double headPitch, double[][] out) {
         Jenny.pose(walkPhase, walkAmp, attack, sneaking, headYaw, headPitch, out);
      }

      @Override
      public double height() {
         return 1.36;
      }

      @Override
      public double waveAngle() {
         return 1.55;
      }
   };

   private Jenny() {
   }

   static Mesh[] meshes() {
      Mesh[] ms = meshes;
      if (ms != null) {
         return ms;
      }

      MeshBuilder b = new MeshBuilder();
      b.outlineColor(-16119286);
      ms = new Mesh[5];
      b.reset();
      b.outlined(0.022, () -> b.frustum(-0.1, 0.27, 0.34, 0.135, 20, -6587414));
      b.outlined(0.012, () -> b.ellipsoid(0.0, -0.075, 0.0, 0.275, 0.03, 0.275, 20, 6, -8758325));
      b.outlined(0.014, () -> b.ellipsoid(0.0, 0.335, 0.01, 0.16, 0.045, 0.15, 16, 8, -1));
      b.outlined(0.008, () -> b.sphere(0.0, 0.3, 0.145, 0.032, 8, 6, -36957));
      ms[0] = b.build();
      b.reset();
      b.translate(0.0, 0.3, 0.0);
      b.outlined(0.022, () -> b.ellipsoid(0.0, 0.0, -0.02, 0.33, 0.32, 0.31, 24, 14, -10864603));

      for (int s = -1; s <= 1; s += 2) {
         double x = s * 0.25;
         b.outlined(0.022, () -> b.ellipsoid(x, -0.12, 0.06, 0.1, 0.19, 0.15, 12, 8, -10864603));
      }

      bow(b);
      ms[1] = b.build();
      b.reset();
      b.translate(0.0, 0.3, 0.0);
      b.outlined(0.014, () -> b.ellipsoid(0.0, -0.06, 0.13, 0.215, 0.21, 0.18, 18, 12, -405080));
      b.outlined(0.012, () -> b.ellipsoid(0.0, 0.105, 0.16, 0.25, 0.085, 0.15, 18, 8, -10864603));

      for (int s = -1; s <= 1; s += 2) {
         double x = s * 0.09;
         b.ellipsoid(x, -0.075, 0.29, 0.034, 0.048, 0.03, 10, 6, -16119286);
         b.sphere(x - s * 0.01 + 0.004, -0.058, 0.318, 0.011, 6, 4, -1);
         b.ellipsoid(s * 0.135, -0.14, 0.255, 0.045, 0.022, 0.025, 10, 4, -24912);
      }

      b.ellipsoid(0.0, -0.18, 0.276, 0.028, 0.014, 0.02, 8, 4, -5228465);
      ms[2] = b.build();
      b.reset();
      b.outlined(0.022, () -> b.ellipsoid(0.0, -0.15, 0.0, 0.052, 0.13, 0.052, 10, 8, -405080));
      b.outlined(0.022, () -> b.sphere(0.0, -0.28, 0.0, 0.062, 10, 8, -405080));
      b.outlined(0.022, () -> b.ellipsoid(0.0, -0.035, 0.0, 0.088, 0.08, 0.088, 12, 8, -6587414));
      ms[3] = b.build();
      b.reset();
      b.outlined(0.022, () -> b.cylinder(-0.19, 0.02, 0.06, 12, -723720));
      b.outlined(0.022, () -> b.ellipsoid(0.0, -0.215, 0.035, 0.082, 0.05, 0.13, 12, 8, -8769979));
      ms[4] = b.build();
      meshes = ms;
      return ms;
   }

   private static void bow(MeshBuilder b) {
      for (int s = -1; s <= 1; s += 2) {
         b.resetTransform();
         b.translate(s * 0.115, 0.605, -0.04);
         b.rotZ(-s * 0.35);
         b.outlined(0.014, () -> b.ellipsoid(0.0, 0.0, 0.0, 0.11, 0.07, 0.05, 12, 8, -36957));
      }

      b.resetTransform();
      b.outlined(0.012, () -> b.sphere(0.0, 0.61, -0.03, 0.045, 10, 6, -1945978));
   }

   public static void pose(double walkPhase, double walkAmp, double attack, boolean sneaking, double headYaw, double headPitch, double[][] out) {
      double stride = Math.sin(walkPhase) * walkAmp * 0.6;
      double skip = Math.abs(Math.sin(walkPhase)) * walkAmp * 0.025;
      double lean = sneaking ? 0.35 : 0.0;
      double swing = Math.sin(attack * Math.PI);
      double[] torso = out[0];
      Mat.setTranslation(torso, 0.0, 0.27 + skip, 0.0);
      Mat.rotX(torso, lean);
      Mat.rotZ(torso, Math.sin(walkPhase) * walkAmp * 0.05);
      double[] head = out[1];
      Mat.setTranslation(head, 0.0, 0.27 + skip + 0.38 * Math.cos(lean), 0.38 * Math.sin(lean));
      Mat.rotY(head, headYaw);
      Mat.rotX(head, headPitch);
      Mat.rotZ(head, Math.sin(walkPhase * 0.5) * walkAmp * 0.06);
      arm(torso, out[2], 0.19, 0.22, -stride * 0.8 - swing * 1.6);
      arm(torso, out[3], -0.19, -0.22, stride * 0.8);
      Mat.setTranslation(out[4], 0.09, 0.27, 0.0);
      Mat.rotX(out[4], stride);
      Mat.setTranslation(out[5], -0.09, 0.27, 0.0);
      Mat.rotX(out[5], -stride);
   }

   private static void arm(double[] torso, double[] a, double u, double roll, double angle) {
      Mat.copy(torso, a);
      Mat.translate(a, u, 0.31, 0.0);
      Mat.rotZ(a, roll);
      Mat.rotX(a, angle);
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
}
