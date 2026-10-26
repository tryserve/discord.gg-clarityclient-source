package need.clarity.modules.visual.cosmetics.aura;

public final class AuraReact {
   public static final double WALK = 4.3;
   public static final double SPRINT = 5.6;
   private static final double REST = 0.5;
   private static final double FULL = 8.5;
   private static final double RISE = 0.12;
   private static final double FALL = 0.6;
   public static final double TELEPORT = 10.0;
   public static final double MAX_STRENGTH = 2.0;
   public static final AuraReact.Reaction REST_REACTION = new AuraReact.Reaction(1.0, 1.0, 1.0, 1.0);

   private AuraReact() {
   }

   public static double energyFor(double blocksPerSecond) {
      return !(blocksPerSecond > 0.5) ? 0.0 : Math.min(1.0, (blocksPerSecond - 0.5) / 8.0);
   }

   public static double follow(double current, double target, double dt) {
      if (dt <= 0.0) {
         return current;
      }

      double tau = target > current ? 0.12 : 0.6;
      double next = current + (target - current) * (1.0 - Math.exp(-dt / tau));
      return next < 0.0 ? 0.0 : (next > 1.0 ? 1.0 : next);
   }

   public static AuraReact.Reaction at(double energy, double strength) {
      double s = Math.max(0.0, Math.min(2.0, strength));
      double e = Math.max(0.0, Math.min(1.0, energy)) * s;
      return e <= 0.0 ? REST_REACTION : new AuraReact.Reaction(1.0 + 1.5 * e, 1.0 + 0.6 * e, 1.0 + 0.1 * e, 1.0 + 0.12 * e);
   }

   public record Reaction(double turn, double clock, double radius, double reach) {
   }
}
