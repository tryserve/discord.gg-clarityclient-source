package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;

public class AntiPush extends ModuleBase {
   public static AntiPush INSTANCE;

   public AntiPush() {
      super("Anti Push", "Movement");
      INSTANCE = this;
   }

   public static boolean shouldCancel() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }
}
