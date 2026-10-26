package need.clarity.modules.visual.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class Mesh {
   public static final Mesh EMPTY = new Mesh(new float[0], new int[0]);
   final int count;
   final float[] pos;
   final int[] col;

   Mesh(float[] pos, int[] col) {
      this.count = col.length;
      this.pos = pos;
      this.col = col;
   }

   public int vertexCount() {
      return this.count;
   }

   public void emit(VertexConsumer vc, double[] m, float alpha) {
      if (this.count != 0) {
         float m0 = (float)m[0];
         float m1 = (float)m[1];
         float m2 = (float)m[2];
         float m3 = (float)m[3];
         float m4 = (float)m[4];
         float m5 = (float)m[5];
         float m6 = (float)m[6];
         float m7 = (float)m[7];
         float m8 = (float)m[8];
         float m9 = (float)m[9];
         float m10 = (float)m[10];
         float m11 = (float)m[11];
         float[] p = this.pos;
         int[] c = this.col;
         boolean fade = alpha < 0.999F;
         int i = 0;

         for (int k = 0; i < this.count; k += 3) {
            float x = p[k];
            float y = p[k + 1];
            float z = p[k + 2];
            int argb = c[i];
            if (fade) {
               argb = (int)((argb >>> 24 & 0xFF) * alpha) << 24 | argb & 16777215;
            }

            vc.addVertex(m0 * x + m1 * y + m2 * z + m3, m4 * x + m5 * y + m6 * z + m7, m8 * x + m9 * y + m10 * z + m11).setColor(argb);
            i++;
         }
      }
   }
}
