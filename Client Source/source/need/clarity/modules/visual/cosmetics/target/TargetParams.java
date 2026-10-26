package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Prim;

public record TargetParams(double width, double height, double phase, double speed, double age) {
   public static final double MARGIN = 0.12;
   public static final double INTRO_SECONDS = 0.35;

   public static TargetParams defaults() {
      return new TargetParams(0.6, 1.8, 0.0, 0.25, 10.0);
   }

   public TargetParams withAge(double a) {
      return new TargetParams(this.width, this.height, this.phase, this.speed, a);
   }

   public TargetParams withSize(double w, double h) {
      return new TargetParams(w, h, this.phase, this.speed, this.age);
   }

   public TargetParams withPhase(double ph) {
      return new TargetParams(this.width, this.height, ph, this.speed, this.age);
   }

   public double hw() {
      return this.width * 0.5 + 0.12;
   }

   public double hh() {
      return this.height * 0.5 + 0.12;
   }

   public double dir() {
      return this.speed < 0.0 ? -1.0 : 1.0;
   }

   public double intro() {
      return Prim.sm(this.age / 0.35);
   }

   public double bulk() {
      return Math.max(1.0, Math.min(3.0, Math.pow(this.height / 1.8, 0.6)));
   }
}
