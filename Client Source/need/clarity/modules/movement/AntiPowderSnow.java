package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;

public class AntiPowderSnow extends ModuleBase {
   public static AntiPowderSnow INSTANCE;

   public AntiPowderSnow() {
      super("Anti Powder Snow", "Movement");
      INSTANCE = this;
   }
}
