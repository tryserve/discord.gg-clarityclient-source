package need.clarity.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;

public final class ThemeTextures {
   private static volatile boolean enabled = false;
   private static volatile float strength = 0.6F;
   private static volatile float tintR = 0.48F;
   private static volatile float tintG = 0.64F;
   private static volatile float tintB = 1.0F;

   private ThemeTextures() {
   }

   public static void set(boolean en, float st, boolean useTheme, int customRgb) {
      enabled = en;
      strength = Math.max(0.0F, Math.min(1.0F, st));
      setAccent(useTheme ? ThemeColors.getAccentRgb() : customRgb);
   }

   public static void refreshAccent() {
      setAccent(ThemeColors.getAccentRgb());
   }

   private static void setAccent(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      float m = Math.max(r, Math.max(g, b));
      if (m < 1.0F) {
         m = 1.0F;
      }

      tintR = r / m;
      tintG = g / m;
      tintB = b / m;
   }

   public static boolean enabled() {
      return enabled;
   }

   public static void reload() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            mc.execute(mc::reloadResourcePacks);
         }
      } catch (Throwable var1) {
      }
   }

   public static boolean shouldRecolor(String path) {
      if (path == null) {
         return false;
      } else {
         return !path.startsWith("block/") && !path.startsWith("item/") ? path.startsWith("textures/gui/") || path.contains("/gui/") : true;
      }
   }

   public static void recolor(NativeImage img) {
      if (enabled && img != null) {
         float s = strength;
         if (!(s <= 0.001F)) {
            float trBase = tintR;
            float tgBase = tintG;
            float tbBase = tintB;

            try {
               int w = img.getWidth();
               int h = img.getHeight();

               for (int y = 0; y < h; y++) {
                  for (int x = 0; x < w; x++) {
                     int p = img.getPixel(x, y);
                     int a = p >>> 24 & 0xFF;
                     if (a != 0) {
                        int r = p >>> 16 & 0xFF;
                        int g = p >>> 8 & 0xFF;
                        int b = p & 0xFF;
                        float lum = 0.299F * r + 0.587F * g + 0.114F * b;
                        int nr = (int)(r + (lum * trBase - r) * s);
                        int ng = (int)(g + (lum * tgBase - g) * s);
                        int nb = (int)(b + (lum * tbBase - b) * s);
                        img.setPixel(x, y, a << 24 | nr << 16 | ng << 8 | nb);
                     }
                  }
               }
            } catch (Throwable var18) {
            }
         }
      }
   }
}
