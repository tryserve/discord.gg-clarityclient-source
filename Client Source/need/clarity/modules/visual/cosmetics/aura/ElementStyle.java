package need.clarity.modules.visual.cosmetics.aura;

import java.util.function.Function;
import need.clarity.modules.visual.cosmetics.Cosmetic;

public enum ElementStyle {
   FLAME("Flame", 16761395, 16726802, FlameAura::new),
   FROST("Frost", 12907007, 4033791, FrostAura::new),
   VOID("Void", 13073919, 3805050, VoidAura::new),
   SAKURA("Sakura", 16763103, 16735902, SakuraAura::new),
   STORM("Storm", 13624319, 5661583, StormAura::new);

   public final String label;
   public final int natural1;
   public final int natural2;
   private final Function<ElementParams, Cosmetic> factory;

   ElementStyle(String label, int natural1, int natural2, Function<ElementParams, Cosmetic> factory) {
      this.label = label;
      this.natural1 = natural1;
      this.natural2 = natural2;
      this.factory = factory;
   }

   public Cosmetic create(ElementParams p) {
      return this.factory.apply(p);
   }

   public static ElementStyle fromName(String name) {
      for (ElementStyle s : values()) {
         if (s.label.equalsIgnoreCase(name)) {
            return s;
         }
      }

      return FLAME;
   }
}
