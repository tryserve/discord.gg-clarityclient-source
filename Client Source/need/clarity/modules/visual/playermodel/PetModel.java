package need.clarity.modules.visual.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface PetModel {
   int TORSO = 0;
   int HEAD = 1;
   int ARM_R = 2;
   int ARM_L = 3;
   int LEG_R = 4;
   int LEG_L = 5;
   int TAIL = 6;
   int PARTS = 7;

   void pose(double var1, double var3, boolean var5, double var6, double var8, double var10, double var12, double[][] var14);

   void draw(VertexConsumer var1, double[] var2, double[] var3, double[][] var4, float var5);

   default double petScale() {
      return 0.42;
   }

   default double eyeHeight() {
      return 0.95;
   }

   default double shadowRadius() {
      return 0.3;
   }

   default boolean canWave() {
      return true;
   }

   default double waveAngle() {
      return 1.95;
   }

   default double stride() {
      return 1.0;
   }

   static PetModel of(final RigModel r) {
      return new PetModel() {
         @Override
         public void pose(double walkPhase, double walkAmp, boolean sneaking, double headYaw, double headPitch, double time, double idle, double[][] out) {
            r.pose(walkPhase, walkAmp, 0.0, sneaking, headYaw, headPitch, out);
            Mat.identity(out[6]);
         }

         @Override
         public void draw(VertexConsumer vc, double[] world, double[] view, double[][] rig, float alpha) {
            RigRenderer.draw(r, vc, world, view, rig, alpha);
         }

         @Override
         public double waveAngle() {
            return r.waveAngle();
         }
      };
   }
}
