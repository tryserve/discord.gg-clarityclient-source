package need.clarity.render.handshader;

import net.minecraft.world.item.ItemDisplayContext;

public class HandShaderConfig {
   public static boolean enabled = false;
   public static boolean applyToArm = true;
   public static boolean applyToItems = true;
   public static int color = -1727996673;
   public static double opacity = 0.6;
   public static boolean textured = true;
   public static boolean fullbright = false;
   public static boolean outline = true;
   public static int outlineColor = -16720641;
   public static double outlineWidth = 1.0;
   public static boolean outlineGlow = true;
   public static double glowSpread = 1.0;
   public static boolean antialiasing = true;

   public static int getArmColor() {
      int a = Math.max(1, Math.min(255, (int)Math.round(opacity * 255.0)));
      return a << 24 | color & 16777215;
   }

   public static int getEffectiveOutlineColor() {
      return outlineColor;
   }

   public static int getEffectiveLight(int originalLight) {
      return fullbright ? 15728880 : originalLight;
   }

   public static int getItemColor(int baseColor) {
      int a1 = baseColor >> 24 & 0xFF;
      int r1 = baseColor >> 16 & 0xFF;
      int g1 = baseColor >> 8 & 0xFF;
      int b1 = baseColor & 0xFF;
      int a2 = Math.max(1, Math.min(255, (int)Math.round(opacity * 255.0)));
      int customColor = color;
      int r2 = customColor >> 16 & 0xFF;
      int g2 = customColor >> 8 & 0xFF;
      int b2 = customColor & 0xFF;
      int a = a1 * a2 / 255;
      if (!textured) {
         return a << 24 | r2 << 16 | g2 << 8 | b2;
      }

      int r = r1 * r2 / 255;
      int g = g1 * g2 / 255;
      int b = b1 * b2 / 255;
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static boolean shouldApplyToItem(ItemDisplayContext context) {
      return enabled && applyToItems && context != null && context.firstPerson();
   }
}
