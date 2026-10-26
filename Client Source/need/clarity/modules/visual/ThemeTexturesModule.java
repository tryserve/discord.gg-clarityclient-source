package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import need.clarity.util.ThemeTextures;

public class ThemeTexturesModule extends ModuleBase {
   private float lastStrength = -1.0F;
   private int lastTintRgb = Integer.MIN_VALUE;
   private long pendingReloadMs = 0L;

   public ThemeTexturesModule() {
      super("Theme Textures", "Visual");
      this.sync();
   }

   private float strength() {
      return this.getSetting("Strength", 0.6F);
   }

   private String colorMode() {
      return this.getSetting("Color Mode", "Theme");
   }

   private boolean useTheme() {
      return !"Custom".equalsIgnoreCase(this.colorMode());
   }

   private int desiredTint() {
      return this.useTheme() ? ThemeColors.getAccentRgb() : ThemeColors.parseHexRgb(this.getSetting("Color", "#7AA2F7"), 8037111);
   }

   private void sync() {
      int rgb = this.desiredTint();
      ThemeTextures.set(this.isEnabled(), this.strength(), this.useTheme(), rgb);
      this.lastStrength = this.strength();
      this.lastTintRgb = rgb;
   }

   @Override
   public void onEnable() {
      this.sync();
      ThemeTextures.reload();
   }

   @Override
   public void onDisable() {
      this.sync();
      ThemeTextures.reload();
   }

   @Override
   public void onTick() {
      boolean changed = Math.abs(this.strength() - this.lastStrength) > 0.001F || this.desiredTint() != this.lastTintRgb;
      if (changed) {
         this.sync();
         this.pendingReloadMs = System.currentTimeMillis();
      }

      if (this.pendingReloadMs != 0L && System.currentTimeMillis() - this.pendingReloadMs > 400L) {
         this.pendingReloadMs = 0L;
         ThemeTextures.reload();
      }
   }
}
