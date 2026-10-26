package need.clarity.modules.visual.playermodel;

public interface RigModel {
   int TORSO = 0;
   int HEAD = 1;
   int ARM_R = 2;
   int ARM_L = 3;
   int LEG_R = 4;
   int LEG_L = 5;
   int PARTS = 6;
   RigModel KENNY = new RigModel() {
      @Override
      public Mesh mesh(int part) {
         return Kenny.mesh(part);
      }

      @Override
      public Mesh face() {
         return Kenny.face();
      }

      @Override
      public double[] anchor(int part) {
         return Kenny.ANCHOR[part];
      }

      @Override
      public double faceMinZ() {
         return 0.3;
      }

      @Override
      public void pose(double walkPhase, double walkAmp, double attack, boolean sneaking, double headYaw, double headPitch, double[][] out) {
         Kenny.pose(walkPhase, walkAmp, attack, sneaking, headYaw, headPitch, out);
      }

      @Override
      public double height() {
         return 1.32;
      }
   };
   RigModel JENNY = Jenny.MODEL;

   Mesh mesh(int var1);

   Mesh face();

   double[] anchor(int var1);

   double faceMinZ();

   void pose(double var1, double var3, double var5, boolean var7, double var8, double var10, double[][] var12);

   double height();

   default double waveAngle() {
      return 1.95;
   }
}
