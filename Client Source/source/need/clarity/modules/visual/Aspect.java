package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class Aspect extends ModuleBase {
   public static Aspect INSTANCE;

   public Aspect() {
      super("Aspect", "Visual");
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static float factor() {
      float f = INSTANCE != null ? INSTANCE.getSetting("Aspect", 1.0F) : 1.0F;
      return Math.max(0.5F, Math.min(2.0F, f));
   }
}
