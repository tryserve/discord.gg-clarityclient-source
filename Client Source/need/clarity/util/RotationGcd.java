package need.clarity.util;

import need.clarity.config.ClarityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class RotationGcd {
   private RotationGcd() {
   }

   public static boolean enabled() {
      try {
         return !(ClarityConfig.getSetting("Kill Aura", "GCD Fix") instanceof Boolean bo && !bo);
      } catch (Throwable t) {
         return true;
      }
   }

   public static float step() {
      try {
         double s = (Double)Minecraft.getInstance().options.sensitivity().get();
         double f = s * 0.6 + 0.2;
         return (float)(f * f * f * 8.0 * 0.15);
      } catch (Throwable t) {
         return 0.15F;
      }
   }

   public static float snapYaw(float previous, float target) {
      float g = step();
      if (g < 1.0E-4F) {
         return target;
      }

      float delta = Mth.wrapDegrees(target - previous);
      return previous + Math.round(delta / g) * g;
   }

   public static float snapPitch(float previous, float target) {
      float g = step();
      if (g < 1.0E-4F) {
         return Mth.clamp(target, -90.0F, 90.0F);
      }

      float delta = target - previous;
      float out = previous + Math.round(delta / g) * g;

      while (out > 90.0F) {
         out -= g;
      }

      while (out < -90.0F) {
         out += g;
      }

      return out;
   }
}
