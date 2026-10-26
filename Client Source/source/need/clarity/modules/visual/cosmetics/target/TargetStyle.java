package need.clarity.modules.visual.cosmetics.target;

import java.util.function.Function;
import need.clarity.modules.visual.cosmetics.Cosmetic;

public enum TargetStyle {
   CORNER_BRACKETS("Corner Brackets", CornerBrackets::new),
   HEX_SHIELD("Hex Shield", HexShield::new),
   ROTATING_DIAMOND("Rotating Diamond", RotatingDiamond::new),
   CHEVRON_LOCK_ON("Chevron Lock-on", ChevronLockOn::new),
   ORBITING_TRIANGLES("Orbiting Triangles", OrbitingTriangles::new),
   SPINNING_RING("Spinning Ring", SpinningRing::new),
   LIGHT_PILLAR("Light Pillar", LightPillar::new),
   HOLOGRAM_SCAN("Hologram Scan", HologramScan::new),
   SOUL_FLAME("Soul Flame", SoulFlame::new, 11860223, 2788607);

   public final String label;
   public final int natural1;
   public final int natural2;
   private final Function<TargetParams, Cosmetic> factory;

   TargetStyle(String label, Function<TargetParams, Cosmetic> factory) {
      this(label, factory, 0, 0);
   }

   TargetStyle(String label, Function<TargetParams, Cosmetic> factory, int natural1, int natural2) {
      this.label = label;
      this.factory = factory;
      this.natural1 = natural1;
      this.natural2 = natural2;
   }

   public boolean hasNatural() {
      return this.natural1 != 0 || this.natural2 != 0;
   }

   public Cosmetic create(TargetParams p) {
      return this.factory.apply(p);
   }

   public static TargetStyle find(String name) {
      if (name == null) {
         return null;
      }

      for (TargetStyle s : values()) {
         if (s.label.equalsIgnoreCase(name)) {
            return s;
         }
      }

      return null;
   }
}
