package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class Animations extends ModuleBase {
   public static final String DEFAULT_MODE = "Simple";
   public static final int DEFAULT_STRENGTH = 20;
   public static final boolean DEFAULT_SWING_SPEED = false;
   public static final int DEFAULT_SWING_SPEED_V = 6;
   public static volatile Animations INSTANCE;

   public Animations() {
      super("Animations", "Visual");
      INSTANCE = this;
   }

   public String getMode() {
      Object raw = this.getSetting("Mode", "Simple");
      if (raw == null) {
         return "Simple";
      }

      String s = raw.toString();
      return "Simple".equals(s) ? "Mode 1" : s;
   }

   public int getStrength() {
      int v = this.getSetting("Strength", 20) instanceof Number n ? n.intValue() : 20;
      return Math.clamp(v, 20, 75);
   }

   public boolean isSwingSpeedEnabled() {
      return this.getSetting("Swing Speed", false) instanceof Boolean b ? b : false;
   }

   public int getSwingSpeedValue() {
      int v = this.getSetting("Swing Speed Value", 6) instanceof Number n ? n.intValue() : 6;
      return Math.clamp(v, 1, 20);
   }

   public static boolean shouldOverrideSwingSpeed() {
      Animations inst = INSTANCE;
      return inst != null && inst.isEnabled() && inst.isSwingSpeedEnabled();
   }

   public static int getConfiguredSwingSpeed() {
      Animations inst = INSTANCE;
      return inst != null ? inst.getSwingSpeedValue() : 6;
   }
}
