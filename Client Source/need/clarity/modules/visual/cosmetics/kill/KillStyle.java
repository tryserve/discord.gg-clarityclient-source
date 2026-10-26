package need.clarity.modules.visual.cosmetics.kill;

import java.util.function.Function;
import need.clarity.modules.visual.cosmetics.Cosmetic;

public enum KillStyle {
   SHOCKWAVE("Shockwave", Shockwave::new),
   SHATTER("Shatter", Shatter::new),
   LIGHTNING_STRIKE("Lightning Strike", LightningStrike::new, 7314431, 15135487),
   BLACK_HOLE("Black Hole", BlackHole::new, 10837247, 16760952),
   PHOENIX("Phoenix", Phoenix::new, 16738842, 16773280),
   ASCEND("Ascend", Ascend::new, 16763213, 16774876);

   public final String label;
   public final int natural1;
   public final int natural2;
   private final Function<KillParams, Cosmetic> factory;

   KillStyle(String label, Function<KillParams, Cosmetic> factory) {
      this(label, factory, 0, 0);
   }

   KillStyle(String label, Function<KillParams, Cosmetic> factory, int natural1, int natural2) {
      this.label = label;
      this.factory = factory;
      this.natural1 = natural1;
      this.natural2 = natural2;
   }

   public boolean hasNatural() {
      return this.natural1 != 0 || this.natural2 != 0;
   }

   public Cosmetic create(KillParams p) {
      return this.factory.apply(p);
   }

   public static KillStyle find(String name) {
      if (name == null) {
         return null;
      }

      for (KillStyle s : values()) {
         if (s.label.equalsIgnoreCase(name)) {
            return s;
         }
      }

      return null;
   }
}
