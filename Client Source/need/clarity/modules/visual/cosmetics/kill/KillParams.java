package need.clarity.modules.visual.cosmetics.kill;

import need.clarity.modules.visual.cosmetics.Col;

public record KillParams(double width, double height, double age, double life, double reach, int segs, int seed, KillParams.Ground ground) {
   public static final KillParams.Ground FLAT = (u, w) -> 0.0;

   public KillParams {
      if (!(width > 0.0)) {
         width = 0.6;
      }

      if (!(height > 0.0)) {
         height = 1.8;
      }

      if (!(life > 0.05)) {
         life = 0.05;
      }

      if (!(reach > 0.2)) {
         reach = 0.2;
      }

      if (Double.isNaN(age)) {
         age = 0.0;
      }

      segs = Math.max(12, Math.min(128, segs));
      if (ground == null) {
         ground = FLAT;
      }
   }

   public static KillParams defaults() {
      return new KillParams(0.6, 1.8, 0.3, 0.625, 5.0, 64, 1, FLAT);
   }

   public KillParams withAge(double a) {
      return new KillParams(this.width, this.height, a, this.life, this.reach, this.segs, this.seed, this.ground);
   }

   public KillParams withSize(double w, double h) {
      return new KillParams(w, h, this.age, this.life, this.reach, this.segs, this.seed, this.ground);
   }

   public KillParams withLife(double l) {
      return new KillParams(this.width, this.height, this.age, l, this.reach, this.segs, this.seed, this.ground);
   }

   public KillParams withReach(double r) {
      return new KillParams(this.width, this.height, this.age, this.life, r, this.segs, this.seed, this.ground);
   }

   public KillParams withSegs(int s) {
      return new KillParams(this.width, this.height, this.age, this.life, this.reach, s, this.seed, this.ground);
   }

   public KillParams withSeed(int s) {
      return new KillParams(this.width, this.height, this.age, this.life, this.reach, this.segs, s, this.ground);
   }

   public KillParams withGround(KillParams.Ground g) {
      return new KillParams(this.width, this.height, this.age, this.life, this.reach, this.segs, this.seed, g);
   }

   public double t() {
      return Col.clamp01(this.age / this.life);
   }

   public double bulk() {
      return Math.max(1.0, Math.min(3.0, Math.pow(this.height / 1.8, 0.6)));
   }

   @FunctionalInterface
   public interface Ground {
      double at(double var1, double var3);
   }
}
