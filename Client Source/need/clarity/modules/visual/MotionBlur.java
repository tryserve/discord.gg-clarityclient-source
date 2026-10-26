package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class MotionBlur extends ModuleBase {
   public static MotionBlur INSTANCE;

   public MotionBlur() {
      super("Motion Blur", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      MotionBlur m = INSTANCE;
      return m != null && m.isEnabled();
   }

   public static float exposure() {
      float a = INSTANCE == null ? 40.0F : INSTANCE.getSetting("Amount", 40.0F);
      return Math.max(1.0F, Math.min(100.0F, a)) / 100.0F / 24.0F;
   }

   public static int samples() {
      String q = INSTANCE == null ? "Medium" : INSTANCE.getSetting("Quality", "Medium");

      return switch (q) {
         case "Low" -> 6;
         case "High" -> 16;
         default -> 10;
      };
   }

   public static float maxLength() {
      float m = INSTANCE == null ? 6.0F : INSTANCE.getSetting("Max Blur", 6.0F);
      return Math.max(1.0F, Math.min(20.0F, m)) / 100.0F;
   }
}
