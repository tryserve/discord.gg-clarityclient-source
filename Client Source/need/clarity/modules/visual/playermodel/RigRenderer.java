package need.clarity.modules.visual.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class RigRenderer {
   private static final double[] full = new double[12];
   private static final int[] order = new int[3];
   private static final double[] dist = new double[3];

   private RigRenderer() {
   }

   public static void draw(RigModel model, VertexConsumer vc, double[] world, double[] view, double[][] rig, float alpha) {
      int far = d2(model, world, 4) >= d2(model, world, 5) ? 4 : 5;
      part(model, vc, world, view, rig, far, alpha);
      part(model, vc, world, view, rig, far == 4 ? 5 : 4, alpha);
      int[] o = order;
      double[] d = dist;
      o[0] = 0;
      o[1] = 2;
      o[2] = 3;

      for (int i = 0; i < 3; i++) {
         d[i] = d2(model, world, o[i]);
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
         part(model, vc, world, view, rig, o[i], alpha);
      }

      part(model, vc, world, view, rig, 1, alpha);
      if (faceVisible(model, world, rig[1])) {
         Mat.mul(world, rig[1], full);
         Mat.mul(view, full, full);
         model.face().emit(vc, full, alpha);
      }
   }

   private static void part(RigModel model, VertexConsumer vc, double[] world, double[] view, double[][] rig, int p, float alpha) {
      Mat.mul(world, rig[p], full);
      Mat.mul(view, full, full);
      model.mesh(p).emit(vc, full, alpha);
   }

   private static double d2(RigModel model, double[] w, int p) {
      double[] a = model.anchor(p);
      double x = Mat.px(w, a[0], a[1], a[2]);
      double y = Mat.py(w, a[0], a[1], a[2]);
      double z = Mat.pz(w, a[0], a[1], a[2]);
      return x * x + y * y + z * z;
   }

   private static boolean faceVisible(RigModel model, double[] world, double[] head) {
      Mat.mul(world, head, full);
      double s2 = full[0] * full[0] + full[4] * full[4] + full[8] * full[8];
      if (s2 < 1.0E-12) {
         return true;
      }

      double z = -(full[2] * full[3] + full[6] * full[7] + full[10] * full[11]) / s2;
      return z >= model.faceMinZ();
   }
}
