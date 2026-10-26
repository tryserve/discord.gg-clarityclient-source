package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class PotionHud extends ModuleBase {
   public static PotionHud INSTANCE;

   public PotionHud() {
      super("Potion HUD", "HUD");
      INSTANCE = this;
   }

   public String anchor() {
      return this.getSetting("Anchor", "Bottom Left");
   }

   public int offsetX() {
      return this.getSetting("X Offset", 12);
   }

   public int offsetY() {
      return this.getSetting("Y Offset", 12);
   }

   public float scale() {
      return Math.max(0.5F, Math.min(2.0F, this.getSetting("Scale", 1.0F)));
   }

   public float opacity() {
      return Math.max(0.1F, Math.min(1.0F, this.getSetting("Opacity", 1.0F)));
   }

   public String layout() {
      return this.getSetting("Layout", "List");
   }

   public String sort() {
      return this.getSetting("Sort", "Time Left");
   }

   public boolean showIcons() {
      return this.getSetting("Icons", true);
   }

   public boolean showLevel() {
      return this.getSetting("Level", true);
   }

   public boolean romanLevel() {
      return this.getSetting("Roman Numerals", true);
   }

   public boolean showTime() {
      return this.getSetting("Time", true);
   }

   public boolean showBar() {
      return this.getSetting("Duration Bar", true);
   }

   public boolean effectColors() {
      return this.getSetting("Effect Colors", true);
   }

   public boolean blinkEnding() {
      return this.getSetting("Blink When Ending", true);
   }

   public boolean hideAmbient() {
      return this.getSetting("Hide Beacon Effects", false);
   }

   public int maxRows() {
      return Math.max(1, this.getSetting("Max Rows", 12));
   }

   public float width() {
      return Math.max(110.0F, Math.min(320.0F, this.getSetting("Width", 170.0F)));
   }

   public float cornerRadius() {
      return Math.max(0.0F, this.getSetting("Corner Radius", 8.0F));
   }

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public float bgOpacity() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Background Opacity", 0.92F)));
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean highlight() {
      return this.getSetting("Highlight", true);
   }

   public boolean blur() {
      return this.getSetting("Blur", false);
   }

   public int blurStrength() {
      return Math.max(1, Math.min(4, this.getSetting("Blur Strength", 2)));
   }

   public float blurTint() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Blur Tint", 0.45F)));
   }
}
