package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class ClarityIcon extends ModuleBase {
   public static ClarityIcon INSTANCE;

   public ClarityIcon() {
      super("Clarity Icon", "HUD");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public String anchor() {
      return this.getSetting("Anchor", "Top Left");
   }

   public int offsetX() {
      return (int)this.getSetting("X Offset", 10.0F).floatValue();
   }

   public int offsetY() {
      return (int)this.getSetting("Y Offset", 10.0F).floatValue();
   }

   public float size() {
      return clamp(this.getSetting("Size", 36.0F), 8.0F, 256.0F);
   }

   public float opacity() {
      return clamp(this.getSetting("Opacity", 1.0F), 0.05F, 1.0F);
   }

   public float rotation() {
      return this.getSetting("Rotation", 0.0F);
   }

   public String colorMode() {
      return this.getSetting("Color", "White");
   }

   public Object customColor() {
      return this.getSetting("Custom Color", "#FFFFFF");
   }

   public float glow() {
      return clamp(this.getSetting("Glow", 0.8F), 0.0F, 3.0F);
   }

   public float glowSize() {
      return clamp(this.getSetting("Glow Size", 1.0F), 0.3F, 3.0F);
   }

   public String glowColorMode() {
      return this.getSetting("Glow Color", "Theme");
   }

   public Object glowCustom() {
      return this.getSetting("Glow Custom Color", "#7AA2F7");
   }

   public boolean pulse() {
      return this.getSetting("Pulse", false);
   }

   public float pulseSpeed() {
      return clamp(this.getSetting("Pulse Speed", 1.0F), 0.1F, 5.0F);
   }

   public boolean spin() {
      return this.getSetting("Spin", false);
   }

   public float spinSpeed() {
      return this.getSetting("Spin Speed", 30.0F);
   }

   public boolean background() {
      return this.getSetting("Background", false);
   }

   public float padding() {
      return clamp(this.getSetting("Padding", 6.0F), 0.0F, 64.0F);
   }

   public float bgOpacity() {
      return clamp(this.getSetting("Background Opacity", 0.92F), 0.0F, 1.0F);
   }

   public float cornerRadius() {
      return clamp(this.getSetting("Corner Radius", 8.0F), 0.0F, 64.0F);
   }

   public boolean circle() {
      return this.getSetting("Circle", false);
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean blur() {
      return this.getSetting("Blur", false);
   }

   public int blurStrength() {
      return (int)clamp(this.getSetting("Blur Strength", 2.0F), 1.0F, 4.0F);
   }

   public float blurTint() {
      return clamp(this.getSetting("Blur Tint", 0.45F), 0.0F, 1.0F);
   }

   public boolean themeBg() {
      return this.getSetting("Theme Colors", true);
   }

   public Object bgColor() {
      return this.getSetting("Background Color", "#161B26");
   }

   public boolean animated() {
      return this.pulse() || this.spin() || "Rainbow".equals(this.colorMode()) || "Rainbow".equals(this.glowColorMode());
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
