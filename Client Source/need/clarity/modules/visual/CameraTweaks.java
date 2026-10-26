package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class CameraTweaks extends ModuleBase {
   private static CameraTweaks instance;

   public CameraTweaks() {
      super("Camera Tweaks", "Visual");
      instance = this;
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled();
   }

   public static boolean noCameraClip() {
      return instance == null ? false : instance.getSetting("No Camera Clip", true);
   }

   public static boolean customDistanceEnabled() {
      return instance == null ? false : instance.getSetting("Camera Distance", false);
   }

   public static float customDistance() {
      return instance == null ? 4.0F : instance.getSetting("Distance", 4.0F);
   }
}
