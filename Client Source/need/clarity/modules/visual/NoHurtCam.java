package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class NoHurtCam extends ModuleBase {
   public static NoHurtCam INSTANCE;

   public NoHurtCam() {
      super("No Hurt Cam", "Visual");
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }
}
