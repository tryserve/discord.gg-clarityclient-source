package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class NoSwing extends ModuleBase {
   public static NoSwing INSTANCE;

   public NoSwing() {
      super("No Swing", "Visual");
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }
}
