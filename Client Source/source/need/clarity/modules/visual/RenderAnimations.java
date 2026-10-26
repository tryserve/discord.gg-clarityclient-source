package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class RenderAnimations extends ModuleBase {
   public static RenderAnimations INSTANCE;

   public RenderAnimations() {
      super("Render Animations", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static float factor() {
      RenderAnimations m = INSTANCE;
      if (m != null && m.isEnabled()) {
         String mode = m.mode();
         if ("None".equals(mode)) {
            return 1.0F;
         }

         double t = System.nanoTime() / 1.0E9;
         float pulse = 0.5F + 0.5F * (float)Math.sin(t * Math.PI * 2.0 * m.speed());

         return switch (mode) {
            case "Pulse" -> 0.45F + 0.55F * pulse;
            case "Breath" -> 0.7F + 0.3F * pulse;
            default -> 1.0F;
         };
      } else {
         return 1.0F;
      }
   }

   public String mode() {
      return this.getSetting("Mode", "Pulse");
   }

   public float speed() {
      return this.getSetting("Speed", 0.5F);
   }
}
