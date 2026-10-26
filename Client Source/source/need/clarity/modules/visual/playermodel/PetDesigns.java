package need.clarity.modules.visual.playermodel;

final class PetDesigns {
   private static final int SKIN = -405080;
   private static final int PINK = -24912;

   private PetDesigns() {
   }

   private static void eyes(BlobModel.Kit k, double x, double y, double z, double rx, double ry, double rz) {
      for (int s = -1; s <= 1; s += 2) {
         k.at(0.0, 0.0, 0.0).ell(1, 0.012, s * x, y, z, rx, ry, rz, -16119286);
         k.ball(1, 0.0, s * x - s * rx * 0.3, y + ry * 0.35, z + rz * 0.75, Math.min(rx, ry) * 0.32, -1);
      }
   }

   static final class Astronaut extends BlobModel.Biped {
      private static final int SUIT = -986379;
      private static final int GREY = -6642769;
      private static final int GREY_L = -3551274;
      private static final int ORANGE = -34278;
      private static final int VISOR = -14800316;
      private static final int VISOR_HI = -7359233;
      private static final int RED = -1750963;
      private static final int BLUE = -12678145;

      Astronaut() {
         this.armX = 0.27;
         this.legX = 0.11;
      }

      @Override
      public double waveAngle() {
         return 1.9;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).cyl(4, 0.022, -0.17, 0.02, 0.075, -986379).cyl(5, 0.022, -0.17, 0.02, 0.075, -986379).ell2(4, 5, 0.022, 0.0, -0.21, 0.03, 0.09, 0.06, 0.13, -6642769);
         k.at(0.0, 0.0, 0.0).bias(0.05).ell2(4, 5, 0.012, 0.0, -0.09, 0.0, 0.082, 0.025, 0.082, -34278);
         k.at(0.0, 0.0, 0.0)
            .ell(0, 0.022, 0.0, 0.17, 0.0, 0.26, 0.25, 0.22, -986379)
            .ell(0, 0.022, 0.0, 0.2, -0.2, 0.2, 0.22, 0.1, -3551274)
            .ell(0, 0.012, 0.0, 0.2, 0.18, 0.11, 0.08, 0.05, -6642769)
            .ball(0, 0.0, -0.045, 0.21, 0.225, 0.018, -1750963)
            .ball(0, 0.0, 0.045, 0.21, 0.225, 0.018, -12678145);
         k.at(0.0, 0.0, 0.0).bias(0.06).ell(0, 0.012, 0.0, 0.02, 0.0, 0.212, 0.035, 0.182, -34278);
         k.at(0.0, 0.0, 0.0).ell2(2, 3, 0.022, 0.0, -0.13, 0.0, 0.07, 0.13, 0.07, -986379).ell2(2, 3, 0.022, 0.0, -0.28, 0.0, 0.07, 0.07, 0.07, -3551274);
         k.at(0.0, 0.0, 0.0).bias(0.05).ell2(2, 3, 0.012, 0.0, -0.22, 0.0, 0.076, 0.025, 0.076, -34278);
         k.at(0.0, 0.0, 0.0).ball(1, 0.022, 0.0, 0.3, 0.0, 0.36, -986379).ell(1, 0.022, 0.0, 0.29, 0.17, 0.27, 0.21, 0.2, -14800316).ell(1, 0.0, -0.1, 0.37, 0.345, 0.06, 0.035, 0.02, -7359233);

         for (int s = -1; s <= 1; s += 2) {
            k.ell(1, 0.0, s * 0.085, 0.28, 0.35, 0.032, 0.048, 0.02, -4656897);
            k.ball(1, 0.022, s * 0.355, 0.3, 0.0, 0.075, -34278);
         }

         k.at(0.18, 0.56, -0.05).rz(-0.3).cyl(1, 0.012, 0.0, 0.16, 0.013, -6642769);
         k.at(0.0, 0.0, 0.0).ball(1, 0.012, 0.227, 0.713, -0.05, 0.032, -1750963);
      }
   }

   static final class Bunny extends BlobModel.Quad {
      private static final int BUN = -658706;
      private static final int BUN_S = -1647149;

      Bunny() {
         this.legY = 0.26;
         this.frontZ = 0.17;
         this.backZ = -0.13;
         this.legX = 0.09;
         this.backLegX = 0.12;
         this.neckY = 0.5;
         this.neckZ = 0.16;
         this.tailY = 0.38;
         this.tailZ = -0.3;
         this.hop = true;
         this.wagIdle = 6.0;
         this.wagWalk = 4.0;
         this.wagAmp = 0.15;
      }

      @Override
      public double petScale() {
         return 0.5;
      }

      @Override
      public double eyeHeight() {
         return 0.62;
      }

      @Override
      public double stride() {
         return 1.6;
      }

      @Override
      public double shadowRadius() {
         return 0.28;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.36, -0.04, 0.21, 0.21, 0.27, -658706).ell(0, 0.022, 0.0, 0.33, 0.12, 0.14, 0.14, 0.12, -1647149);
         k.at(0.0, 0.0, 0.0)
            .ell2(2, 3, 0.022, 0.0, -0.11, 0.0, 0.042, 0.12, 0.042, -658706)
            .ell2(2, 3, 0.022, 0.0, -0.22, 0.015, 0.045, 0.045, 0.045, -658706)
            .ell2(4, 5, 0.022, 0.0, -0.04, 0.0, 0.085, 0.11, 0.13, -658706)
            .ell2(4, 5, 0.022, 0.0, -0.22, 0.07, 0.06, 0.04, 0.15, -658706);
         k.at(0.0, 0.0, 0.0).ell(1, 0.022, 0.0, 0.12, 0.05, 0.19, 0.17, 0.17, -658706).ell(1, 0.012, 0.0, 0.1, 0.222, 0.024, 0.017, 0.015, -24912);

         for (int s = -1; s <= 1; s += 2) {
            k.at(0.0, 0.0, 0.0).ell(1, 0.022, s * 0.085, 0.055, 0.15, 0.08, 0.065, 0.07, -658706);
            k.at(0.0, 0.0, 0.0).ell(1, 0.0, s * 0.11, 0.06, 0.21, 0.03, 0.015, 0.015, -24912);
            k.at(s * 0.07, 0.25, 0.0).rz(-s * 0.15).rx(-0.15).ell(1, 0.022, 0.0, 0.15, 0.0, 0.05, 0.17, 0.035, -658706).ell(1, 0.0, 0.0, 0.15, 0.022, 0.028, 0.12, 0.016, -24912);
         }

         PetDesigns.eyes(k, 0.085, 0.15, 0.195, 0.032, 0.04, 0.02);
         k.at(0.0, 0.0, 0.0).ball(6, 0.022, 0.0, 0.0, -0.02, 0.075, -1);
      }
   }

   static final class Cat extends BlobModel.Quad {
      private static final int GREY = -7564900;
      private static final int WHITE_F = -723722;
      private static final int EYE = -8662712;
      private static final int DARK = -10591379;

      Cat() {
         this.legY = 0.33;
         this.frontZ = 0.2;
         this.backZ = -0.2;
         this.legX = 0.1;
         this.neckY = 0.52;
         this.neckZ = 0.26;
         this.tailY = 0.5;
         this.tailZ = -0.33;
         this.wagIdle = 2.2;
         this.wagWalk = 3.0;
         this.wagAmp = 0.35;
      }

      @Override
      public double petScale() {
         return 0.5;
      }

      @Override
      public double eyeHeight() {
         return 0.7;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.43, -0.02, 0.175, 0.165, 0.34, -7564900).ell(0, 0.022, 0.0, 0.41, 0.25, 0.11, 0.12, 0.09, -723722);

         for (double z : new double[]{-0.12, 0.0, 0.12}) {
            k.at(0.0, 0.0, 0.0).ell(0, 0.0, 0.0, 0.585, z - 0.02, 0.1, 0.012, 0.03, -10591379);
         }

         k.at(0.0, 0.0, 0.0)
            .ell2(2, 3, 0.022, 0.0, -0.13, 0.0, 0.05, 0.15, 0.05, -7564900)
            .ell2(2, 3, 0.022, 0.0, -0.285, 0.015, 0.058, 0.04, 0.07, -723722)
            .ell2(4, 5, 0.022, 0.0, -0.05, 0.0, 0.075, 0.12, 0.09, -7564900)
            .ell2(4, 5, 0.022, 0.0, -0.17, 0.0, 0.048, 0.12, 0.048, -7564900)
            .ell2(4, 5, 0.022, 0.0, -0.285, 0.015, 0.058, 0.04, 0.07, -723722);
         k.at(0.0, 0.0, 0.0)
            .ell(1, 0.022, 0.0, 0.13, 0.06, 0.21, 0.18, 0.18, -7564900)
            .ell(1, 0.022, 0.0, 0.06, 0.2, 0.09, 0.06, 0.06, -723722)
            .ell(1, 0.012, 0.0, 0.095, 0.255, 0.025, 0.018, 0.015, -24912);

         for (int s = -1; s <= 1; s += 2) {
            k.at(s * 0.12, 0.26, 0.04).rz(-s * 0.35).cone(1, 0.022, 0.0, 0.075, 0.14, 0.006, -7564900);
            k.at(s * 0.12, 0.26, 0.065).rz(-s * 0.35).cone(1, 0.0, 0.01, 0.048, 0.11, 0.004, -24912);
            k.at(0.0, 0.0, 0.0)
               .ell(1, 0.012, s * 0.08, 0.15, 0.215, 0.042, 0.048, 0.025, -8662712)
               .ell(1, 0.0, s * 0.08, 0.15, 0.238, 0.012, 0.036, 0.01, -16119286)
               .ball(1, 0.0, s * 0.08 - s * 0.012, 0.17, 0.243, 0.009, -1);
            k.at(0.0, 0.0, 0.0).ell(1, 0.0, s * 0.045, 0.27, 0.12, 0.012, 0.035, 0.03, -10591379);
         }

         k.at(0.0, 0.0, 0.0).rx(0.5).ell(6, 0.022, 0.0, 0.0, -0.13, 0.04, 0.04, 0.14, -7564900);
         k.at(0.0, 0.129, -0.237).rx(1.3).ell(6, 0.022, 0.0, 0.0, -0.1, 0.038, 0.038, 0.11, -7564900);
      }
   }

   static final class Dog extends BlobModel.Quad {
      private static final int TAN = -2188722;
      private static final int CREAM = -530744;
      private static final int BROWN = -7711453;
      private static final int TONGUE = -34166;

      Dog() {
         this.tailLift = 0.0;
      }

      @Override
      public double petScale() {
         return 0.55;
      }

      @Override
      public double eyeHeight() {
         return 0.78;
      }

      @Override
      public double stride() {
         return 1.3;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.47, -0.02, 0.21, 0.19, 0.36, -2188722).ell(0, 0.022, 0.0, 0.45, 0.27, 0.14, 0.15, 0.1, -530744);
         k.at(0.0, 0.0, 0.0)
            .ell2(2, 3, 0.022, 0.0, -0.14, 0.0, 0.062, 0.16, 0.062, -2188722)
            .ell2(2, 3, 0.022, 0.0, -0.305, 0.02, 0.072, 0.045, 0.09, -530744)
            .ell2(4, 5, 0.022, 0.0, -0.06, 0.0, 0.085, 0.13, 0.1, -2188722)
            .ell2(4, 5, 0.022, 0.0, -0.18, 0.0, 0.058, 0.12, 0.058, -2188722)
            .ell2(4, 5, 0.022, 0.0, -0.305, 0.02, 0.072, 0.045, 0.09, -530744);
         k.at(0.0, 0.0, 0.0)
            .ell(1, 0.022, 0.0, 0.14, 0.08, 0.22, 0.2, 0.2, -2188722)
            .ell(1, 0.022, 0.0, 0.06, 0.25, 0.115, 0.085, 0.12, -530744)
            .ell(1, 0.012, 0.0, 0.1, 0.365, 0.045, 0.035, 0.03, -16119286)
            .ell(1, 0.012, 0.0, -0.025, 0.3, 0.04, 0.03, 0.035, -34166);
         PetDesigns.eyes(k, 0.09, 0.19, 0.255, 0.032, 0.04, 0.025);

         for (int s = -1; s <= 1; s += 2) {
            k.at(0.0, 0.0, 0.0).ell(1, 0.0, s * 0.09, 0.255, 0.235, 0.03, 0.018, 0.02, -530744);
            k.at(s * 0.17, 0.27, 0.05).rz(s * 0.5).ell(1, 0.022, 0.0, -0.08, 0.0, 0.05, 0.12, 0.09, -7711453);
         }

         k.at(0.0, 0.0, 0.0).rx(0.8).ell(6, 0.022, 0.0, 0.0, -0.12, 0.045, 0.045, 0.13, -2188722).ball(6, 0.022, 0.0, 0.0, -0.24, 0.05, -530744);
      }
   }

   static final class Knight extends BlobModel.Biped {
      private static final int STEEL = -4603185;
      private static final int STEEL_D = -8550506;
      private static final int CHAIN = -7301213;
      private static final int RED = -3657156;
      private static final int GOLD = -868018;
      private static final int LEATHER = -9747930;
      private static final int SLIT = -15460836;

      Knight() {
         this.armX = 0.25;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).cyl(4, 0.022, -0.19, 0.02, 0.066, -8550506).cyl(5, 0.022, -0.19, 0.02, 0.066, -8550506).ell2(4, 5, 0.022, 0.0, -0.215, 0.035, 0.088, 0.055, 0.13, -4603185);
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.17, 0.0, 0.24, 0.23, 0.2, -4603185).ell(0, 0.022, 0.0, 0.13, 0.115, 0.16, 0.2, 0.1, -3657156).ball(0, 0.012, 0.0, 0.19, 0.215, 0.035, -868018);
         k.at(0.0, 0.0, 0.0).bias(0.06).ell(0, 0.012, 0.0, 0.02, 0.0, 0.215, 0.04, 0.18, -9747930).ball(0, 0.012, 0.0, 0.02, 0.185, 0.03, -868018);
         k.at(0.0, 0.0, 0.0).ell2(2, 3, 0.022, 0.0, -0.14, 0.0, 0.055, 0.12, 0.055, -7301213).ell2(2, 3, 0.022, 0.0, -0.27, 0.0, 0.066, 0.066, 0.066, -8550506);
         k.at(0.0, 0.0, 0.0).bias(0.08).ell2(2, 3, 0.022, 0.0, -0.01, 0.0, 0.098, 0.09, 0.098, -4603185);
         k.at(0.0, 0.0, 0.0).ell(1, 0.022, 0.0, 0.3, 0.0, 0.32, 0.33, 0.31, -4603185).ell(1, 0.012, 0.0, 0.28, 0.255, 0.21, 0.05, 0.08, -15460836);

         for (int s = -1; s <= 1; s += 2) {
            k.ball(1, 0.0, s * 0.08, 0.28, 0.327, 0.022, -4200705);
         }

         k.at(0.0, 0.62, -0.04).rx(-0.5).ell(1, 0.012, 0.0, 0.08, 0.0, 0.05, 0.13, 0.11, -3657156);
         k.at(0.0, 0.58, -0.2).rx(-1.0).ell(1, 0.012, 0.0, 0.06, 0.0, 0.045, 0.1, 0.09, -3657156);
      }
   }

   static final class Ninja extends BlobModel.Biped {
      private static final int NINJA = -14013642;
      private static final int NINJA_D = -15000540;
      private static final int WRAP = -10657680;
      private static final int RED = -2737606;

      Ninja() {
         this.armX = 0.24;
         this.skip = 0.02;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).cyl(4, 0.022, -0.19, 0.02, 0.062, -14013642).cyl(5, 0.022, -0.19, 0.02, 0.062, -14013642).ell2(4, 5, 0.022, 0.0, -0.215, 0.035, 0.08, 0.05, 0.125, -15000540);
         k.at(0.0, 0.0, 0.0).bias(0.05).ell2(4, 5, 0.012, 0.0, -0.15, 0.0, 0.068, 0.03, 0.068, -10657680);
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.17, 0.0, 0.23, 0.23, 0.19, -14013642);
         k.at(0.0, 0.0, 0.0).bias(0.06).ell(0, 0.012, 0.0, 0.03, 0.0, 0.2, 0.045, 0.165, -2737606).ball(0, 0.012, 0.1, 0.03, 0.15, 0.04, -2737606);
         k.at(0.0, 0.0, 0.0).ell2(2, 3, 0.022, 0.0, -0.14, 0.0, 0.058, 0.13, 0.058, -14013642).ell2(2, 3, 0.022, 0.0, -0.27, 0.0, 0.06, 0.06, 0.06, -15000540);
         k.at(0.0, 0.0, 0.0).bias(0.05).ell2(2, 3, 0.012, 0.0, -0.2, 0.0, 0.064, 0.03, 0.064, -10657680);
         k.at(0.0, 0.0, 0.0).ball(1, 0.022, 0.0, 0.3, 0.0, 0.32, -14013642).ell(1, 0.012, 0.0, 0.3, 0.22, 0.22, 0.075, 0.12, -405080);
         PetDesigns.eyes(k, 0.085, 0.3, 0.322, 0.028, 0.04, 0.02);
         k.at(0.0, 0.0, 0.0).ring(1, 0.0, 0.42, 0.305, 0.295, 0.05, 0.035, 16, -2737606).ball(1, 0.012, 0.0, 0.42, -0.31, 0.045, -2737606);
         k.at(0.04, 0.42, -0.33).rx(0.9).rz(0.25).ell(1, 0.012, 0.0, -0.12, 0.0, 0.03, 0.13, 0.015, -2737606);
         k.at(-0.04, 0.42, -0.33).rx(0.7).rz(-0.15).ell(1, 0.012, 0.0, -0.12, 0.0, 0.03, 0.12, 0.015, -2737606);
      }
   }

   static final class Penguin extends BlobModel.Biped {
      private static final int PBLACK = -14276045;
      private static final int BELLY = -591878;
      private static final int ORANGE = -24786;

      Penguin() {
         this.hip = 0.09;
         this.legX = 0.09;
         this.armX = 0.25;
         this.shoulder = 0.34;
         this.neck = 0.52;
         this.roll = 0.35;
         this.strideAmt = 0.35;
         this.armSwing = 0.4;
         this.waddle = 0.16;
      }

      @Override
      public double petScale() {
         return 0.5;
      }

      @Override
      public double eyeHeight() {
         return 0.8;
      }

      @Override
      public double waveAngle() {
         return 1.5;
      }

      @Override
      public double stride() {
         return 0.7;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).ell2(4, 5, 0.022, 0.0, -0.065, 0.07, 0.075, 0.03, 0.11, -24786);
         k.at(0.0, 0.0, 0.0).ell(0, 0.022, 0.0, 0.25, 0.0, 0.27, 0.3, 0.24, -14276045).ell(0, 0.022, 0.0, 0.22, 0.09, 0.21, 0.25, 0.17, -591878);
         k.at(0.0, 0.0, 0.0).ell2(2, 3, 0.022, 0.0, -0.13, 0.0, 0.035, 0.16, 0.085, -14276045);
         k.at(0.0, 0.0, 0.0)
            .ell(1, 0.022, 0.0, 0.18, 0.0, 0.23, 0.21, 0.21, -14276045)
            .ell(1, 0.012, 0.0, 0.17, 0.1, 0.17, 0.14, 0.12, -591878)
            .ell(1, 0.012, 0.0, 0.14, 0.24, 0.06, 0.035, 0.08, -24786);
         PetDesigns.eyes(k, 0.07, 0.2, 0.205, 0.028, 0.034, 0.02);

         for (int s = -1; s <= 1; s += 2) {
            k.ell(1, 0.0, s * 0.11, 0.13, 0.19, 0.035, 0.018, 0.02, -24912);
         }
      }
   }

   static final class Wizard extends BlobModel.Biped {
      private static final int ROBE = -13351256;
      private static final int ROBE_D = -14208129;
      private static final int GOLD = -868018;
      private static final int BEARD = -855310;
      private static final int BOOT = -9747930;
      private static final int NOSE = -874358;

      Wizard() {
         this.armX = 0.2;
         this.roll = 0.2;
         this.strideAmt = 0.45;
      }

      @Override
      public double waveAngle() {
         return 1.7;
      }

      @Override
      protected void build(BlobModel.Kit k) {
         k.at(0.0, 0.0, 0.0).cyl(4, 0.022, -0.19, -0.05, 0.058, -9747930).cyl(5, 0.022, -0.19, -0.05, 0.058, -9747930).ell2(4, 5, 0.022, 0.0, -0.215, 0.04, 0.08, 0.05, 0.13, -9747930);
         k.at(0.0, 0.0, 0.0).cone(0, 0.022, -0.2, 0.3, 0.36, 0.14, -13351256).ball(0, 0.012, -0.1, 0.0, 0.22, 0.035, -868018).ball(0, 0.012, 0.12, 0.2, 0.14, 0.03, -868018);
         k.at(0.0, 0.0, 0.0).bias(0.06).ell(0, 0.012, 0.0, -0.18, 0.0, 0.305, 0.03, 0.305, -868018).ell(0, 0.012, 0.0, 0.12, 0.0, 0.222, 0.035, 0.222, -868018);
         k.at(0.0, 0.0, 0.0).blob(0.022, b -> b.frustum(-0.2, 0.1, 0.02, 0.07, 14, -13351256), 2, 3).ell2(2, 3, 0.022, 0.0, -0.25, 0.0, 0.06, 0.06, 0.06, -405080);
         k.at(0.0, 0.0, 0.0)
            .ball(1, 0.022, 0.0, 0.27, 0.0, 0.27, -405080)
            .ell(1, 0.022, 0.0, 0.25, -0.07, 0.265, 0.22, 0.24, -855310)
            .ell(1, 0.022, 0.0, 0.1, 0.17, 0.2, 0.24, 0.12, -855310)
            .ball(1, 0.012, 0.0, 0.25, 0.27, 0.05, -874358);
         PetDesigns.eyes(k, 0.09, 0.31, 0.24, 0.03, 0.035, 0.02);

         for (int s = -1; s <= 1; s += 2) {
            k.ell(1, 0.012, s * 0.095, 0.37, 0.225, 0.06, 0.025, 0.03, -855310);
         }

         k.at(0.0, 0.0, 0.0).bias(0.03).ell(1, 0.012, 0.0, 0.19, 0.255, 0.13, 0.04, 0.05, -855310);
         k.at(0.0, 0.0, 0.0).bias(0.05).ell(1, 0.022, 0.0, 0.46, 0.0, 0.44, 0.04, 0.44, -14208129);
         k.at(0.0, 0.46, -0.02).rx(-0.22).cone(1, 0.022, 0.0, 0.26, 0.36, 0.12, -14208129).bias(0.06).ell(1, 0.012, 0.0, 0.05, 0.0, 0.268, 0.045, 0.268, -868018);
         k.at(0.0, 0.46, -0.02).rx(-0.22).t(0.0, 0.36, 0.0).rx(-0.6).cone(1, 0.022, 0.0, 0.12, 0.24, 0.01, -14208129).ball(1, 0.012, 0.0, 0.25, 0.0, 0.045, -868018);
      }
   }
}
