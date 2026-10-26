package need.clarity.modules.visual.cosmetics.aura;

import java.util.function.Function;
import need.clarity.modules.visual.cosmetics.Cosmetic;

public enum OrbitStyle {
   SHARDS("Shards", Shards::new),
   SWORDS("Swords", Swords::new),
   ORBS("Orbs", Orbs::new),
   RUNES("Runes", Runes::new),
   RINGS("Rings", Rings::new);

   public final String label;
   private final Function<AuraParams, Cosmetic> factory;

   OrbitStyle(String label, Function<AuraParams, Cosmetic> factory) {
      this.label = label;
      this.factory = factory;
   }

   public Cosmetic create(AuraParams p) {
      return this.factory.apply(p);
   }

   public static OrbitStyle fromName(String name) {
      for (OrbitStyle s : values()) {
         if (s.label.equalsIgnoreCase(name)) {
            return s;
         }
      }

      return SHARDS;
   }
}
