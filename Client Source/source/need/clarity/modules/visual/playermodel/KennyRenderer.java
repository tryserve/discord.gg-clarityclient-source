package need.clarity.modules.visual.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class KennyRenderer {
   private static final double[] full = new double[12];
   private static final int[] order = new int[3];
   private static final double[] dist = new double[3];

   private KennyRenderer() {
   }

   public static void draw(VertexConsumer vc, double[] world, double[] view, double[][] rig, float alpha) {
      int far = d2(world, 4) >= d2(world, 5) ? 4 : 5;
      part(vc, world, view, rig, far, alpha);
      part(vc, world, view, rig, far == 4 ? 5 : 4, alpha);
      int[] o = order;
      double[] d = dist;
      o[0] = 0;
      o[1] = 2;
      o[2] = 3;

      for (int i = 0; i < 3; i++) {
         d[i] = d2(world, o[i]);
      }

      for (int i = 1; i < 3; i++) {
         int oi = o[i];
         double di = d[i];

         int j;
         for (j = i - 1; j >= 0 && d[j] < di; j--) {
            o[j + 1] = o[j];
            d[j + 1] = d[j];
         }

         o[j + 1] = oi;
         d[j + 1] = di;
      }

      for (int i = 0; i < 3; i++) {
         part(vc, world, view, rig, o[i], alpha);
      }

      part(vc, world, view, rig, 1, alpha);
      if (faceVisible(world, rig[1])) {
         Mat.mul(world, rig[1], full);
         Mat.mul(view, full, full);
         Kenny.face().emit(vc, full, alpha);
      }
   }

   private static void part(VertexConsumer vc, double[] world, double[] view, double[][] rig, int p, float alpha) {
      Mat.mul(world, rig[p], full);
      Mat.mul(view, full, full);
      Kenny.mesh(p).emit(vc, full, alpha);
   }

   private static double d2(double[] w, int p) {
      double[] a = Kenny.ANCHOR[p];
      double x = Mat.px(w, a[0], a[1], a[2]);
      double y = Mat.py(w, a[0], a[1], a[2]);
      double z = Mat.pz(w, a[0], a[1], a[2]);
      return x * x + y * y + z * z;
   }

   private static boolean faceVisible(double[] world, double[] head) {
      Mat.mul(world, head, full);
      double s2 = full[0] * full[0] + full[4] * full[4] + full[8] * full[8];
      if (s2 < 1.0E-12) {
         return true;
      }

      double z = -(full[2] * full[3] + full[6] * full[7] + full[10] * full[11]) / s2;
      return z >= 0.3;
   }
}
