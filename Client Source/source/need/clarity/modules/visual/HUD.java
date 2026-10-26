package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class HUD extends ModuleBase {
   public static HUD INSTANCE;

   public HUD() {
      super("HUD", "HUD");
      INSTANCE = this;
   }

   public boolean watermark() {
      return this.getSetting("Watermark", true);
   }

   public String watermarkText() {
      return this.getSetting("Watermark Text", "Clarity");
   }

   public boolean fps() {
      return this.getSetting("FPS", true);
   }

   public boolean ping() {
      return this.getSetting("Ping", true);
   }

   public boolean coords() {
      return this.getSetting("Coords", true);
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

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean separators() {
      return this.getSetting("Separators", true);
   }

   public boolean highlight() {
      return this.getSetting("Highlight", true);
   }

   public int x() {
      return this.getSetting("X", 12);
   }

   public int y() {
      return this.getSetting("Y", 12);
   }

   public float paddingH() {
      return this.getSetting("Padding X", 13.0F);
   }

   public float paddingV() {
      return this.getSetting("Padding Y", 6.0F);
   }

   public float itemGap() {
      return this.getSetting("Item Gap", 13.0F);
   }

   public float cornerRadius() {
      return this.getSetting("Corner Radius", 5.0F);
   }

   public boolean autoScale() {
      return this.getSetting("Auto Scale", true);
   }

   public float overlayScale() {
      return this.getSetting("Overlay Scale", 1.0F);
   }

   public String supersample() {
      return this.getSetting("Supersample", "Auto");
   }

   public float scale() {
      return Math.max(0.5F, Math.min(1.5F, this.getSetting("Scale", 1.0F)));
   }
}
