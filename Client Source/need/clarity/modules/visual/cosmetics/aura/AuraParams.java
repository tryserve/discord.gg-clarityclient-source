package need.clarity.modules.visual.cosmetics.aura;

public record AuraParams(int count, double radius, double phase, double speed) {
   public static AuraParams defaults() {
      return new AuraParams(5, 1.0, 0.0, 0.25);
   }

   public int items(int max) {
      return Math.max(1, Math.min(max, this.count));
   }

   public int detail(int n) {
      return n <= 5 ? 2 : (n <= 8 ? 1 : 0);
   }

   public double dir() {
      return this.speed < 0.0 ? -1.0 : 1.0;
   }

   public double angle(int k, int n) {
      return (Math.PI * 2) * (this.phase + (double)k / n);
   }
}
