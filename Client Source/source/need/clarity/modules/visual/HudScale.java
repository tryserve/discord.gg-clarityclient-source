package need.clarity.modules.visual;

import net.minecraft.client.Minecraft;

public final class HudScale {
   private HudScale() {
   }

   public static boolean auto() {
      HUD h = HUD.INSTANCE;
      return h == null || h.autoScale();
   }

   public static float manual() {
      HUD h = HUD.INSTANCE;
      float v = h != null ? h.overlayScale() : 1.0F;
      return Math.max(0.5F, Math.min(3.0F, v));
   }

   public static float factor(float physW, float physH) {
      if (!auto()) {
         return manual();
      }

      float f = physH / 720.0F;
      return Math.max(0.75F, Math.min(4.0F, f));
   }

   public static int supersample() {
      HUD h = HUD.INSTANCE;
      String s = h != null ? h.supersample() : "Auto";
      if ("Off".equals(s)) {
         return 1;
      }

      if ("3x".equals(s)) {
         return 3;
      }

      if ("2x".equals(s)) {
         return 2;
      }

      Minecraft mc = Minecraft.getInstance();
      int ph = mc != null && mc.getWindow() != null ? mc.getWindow().getHeight() : 1080;
      return ph < 1000 ? 2 : 1;
   }
}
