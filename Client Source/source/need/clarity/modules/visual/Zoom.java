package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class Zoom extends ModuleBase {
   public static Zoom INSTANCE;
   private static float scrollMul = 1.0F;
   private static float current = 1.0F;
   private static long lastNanos = 0L;
   private static boolean smoothCamSet = false;
   private static boolean smoothCamPrev = false;

   public Zoom() {
      super("Zoom", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      scrollMul = 1.0F;
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null && this.getSetting("Cinematic", false)) {
         smoothCamPrev = mc.options.smoothCamera;
         mc.options.smoothCamera = true;
         smoothCamSet = true;
      }
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (smoothCamSet && mc != null && mc.options != null) {
         mc.options.smoothCamera = smoothCamPrev;
      }

      smoothCamSet = false;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   private static float target() {
      if (!isActive()) {
         return 1.0F;
      }

      float base = Math.max(1.1F, Math.min(50.0F, INSTANCE.getSetting("Zoom", 4.0F)));
      return Math.max(1.05F, Math.min(50.0F, base * scrollMul));
   }

   public static float applyFov(float fov) {
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
      lastNanos = now;
      float t = target();
      float smooth = INSTANCE == null ? 0.6F : Math.max(0.0F, Math.min(1.0F, INSTANCE.getSetting("Smoothness", 0.6F)));
      if (smooth <= 0.001F) {
         current = t;
      } else {
         float rate = 4.0F + (1.0F - smooth) * 26.0F;
         float k = 1.0F - (float)Math.exp(-dt * rate);
         float lc = (float)Math.log(current);
         float lt = (float)Math.log(t);
         current = (float)Math.exp(lc + (lt - lc) * k);
         if (Math.abs(current - t) < 5.0E-4F) {
            current = t;
         }
      }

      if (current <= 1.0005F) {
         current = 1.0F;
         return fov;
      } else {
         return fov / current;
      }
   }

   public static double sensitivity() {
      return !(current <= 1.0005F) && INSTANCE != null && INSTANCE.getSetting("Scale Sensitivity", true) ? 1.0 / current : 1.0;
   }

   public static void scroll(double dir) {
      if (isActive() && INSTANCE.getSetting("Scroll To Adjust", true)) {
         scrollMul = Math.max(0.25F, Math.min(6.0F, scrollMul * (float)Math.pow(1.15, dir)));
      }
   }

   public static boolean consumesScroll() {
      return isActive() && INSTANCE.getSetting("Scroll To Adjust", true);
   }
}
