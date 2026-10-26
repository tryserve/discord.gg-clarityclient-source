package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class TargetHud extends ModuleBase {
   public static TargetHud INSTANCE;

   public TargetHud() {
      super("Target Hud", "HUD");
      INSTANCE = this;
   }

   public int x() {
      return this.getSetting("X", 12);
   }

   public int y() {
      return this.getSetting("Y", 12);
   }

   public String anchor() {
      return this.getSetting("Anchor", "Bottom Right");
   }

   public int width() {
      return Math.max(120, this.getSetting("Width", 224));
   }

   public boolean showHealth() {
      return this.getSetting("Show Health", true);
   }

   public boolean showDist() {
      return this.getSetting("Show Distance", true);
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

   public boolean showGear() {
      return this.getSetting("Show Gear", true);
   }
}
