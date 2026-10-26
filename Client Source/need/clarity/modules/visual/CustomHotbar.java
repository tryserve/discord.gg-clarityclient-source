package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class CustomHotbar extends ModuleBase {
   public static CustomHotbar INSTANCE;

   public CustomHotbar() {
      super("Custom Hotbar", "HUD");
      INSTANCE = this;
   }

   public static boolean active() {
      CustomHotbar m = INSTANCE;
      return m != null && m.isEnabled();
   }

   public String style() {
      return this.getSetting("Style", "Bars");
   }

   public boolean hotbar() {
      return this.getSetting("Hotbar", true);
   }

   public boolean statusBars() {
      return this.getSetting("Status Bars", true);
   }

   public boolean experience() {
      return this.getSetting("Experience", true);
   }

   public boolean themeColors() {
      return this.getSetting("Theme Colors", true);
   }

   public String accent() {
      return this.getSetting("Accent", "#7AA2F7");
   }

   public float bgOpacity() {
      return this.getSetting("Background Opacity", 0.72F);
   }

   public float rounding() {
      return this.getSetting("Rounding", 5.0F);
   }

   public boolean slotNumbers() {
      return this.getSetting("Slot Numbers", false);
   }

   public boolean numbers() {
      return this.getSetting("Numbers", true);
   }

   public boolean damageTrail() {
      return this.getSetting("Damage Trail", true);
   }

   public boolean lowPulse() {
      return this.getSetting("Low Health Pulse", true);
   }

   public boolean smooth() {
      return this.getSetting("Smooth Animations", true);
   }

   public String shader() {
      return this.getSetting("Shader", "Off");
   }

   public float shaderIntensity() {
      return this.getSetting("Shader Intensity", 0.6F);
   }

   public String healthColor() {
      return this.getSetting("Health Color", "#FF4D5E");
   }

   public String absorbColor() {
      return this.getSetting("Absorption Color", "#FFC940");
   }

   public String hungerColor() {
      return this.getSetting("Hunger Color", "#F2A33A");
   }

   public String armorColor() {
      return this.getSetting("Armor Color", "#A9C4E8");
   }

   public String airColor() {
      return this.getSetting("Air Color", "#5BC8FF");
   }

   public String xpColor() {
      return this.getSetting("XP Color", "#7CE35B");
   }
}
