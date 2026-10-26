package need.clarity.modules.visual.cosmetics.aura;

import java.util.List;

public record SphereParams(double radius, int frequency, List<Ripple> ripples) {
   public static SphereParams defaults() {
      return new SphereParams(1.2, 3, List.of());
   }

   public SphereParams withRipples(List<Ripple> r) {
      return new SphereParams(this.radius, this.frequency, r);
   }

   public SphereParams withFrequency(int f) {
      return new SphereParams(this.radius, f, this.ripples);
   }
}
