package need.clarity.bridge;

import need.clarity.modules.visual.Ambience;

public final class ClientLevelAmbienceBridge {
   private ClientLevelAmbienceBridge() {
   }

   public static long overrideClockTime(long original, boolean isClient) {
      try {
         return isClient && Ambience.isOverrideTimeEnabled() ? Ambience.getOverriddenDayTime() : original;
      } catch (Throwable t) {
         return original;
      }
   }

   public static float overrideRainLevel(float original, boolean isClient) {
      try {
         if (isClient && Ambience.isOverrideWeatherEnabled()) {
            return "Clear".equals(Ambience.getWeatherMode()) ? 0.0F : 1.0F;
         } else {
            return original;
         }
      } catch (Throwable t) {
         return original;
      }
   }

   public static float overrideThunderLevel(float original, boolean isClient) {
      try {
         if (isClient && Ambience.isOverrideWeatherEnabled()) {
            return "Thunder".equals(Ambience.getWeatherMode()) ? 1.0F : 0.0F;
         } else {
            return original;
         }
      } catch (Throwable t) {
         return original;
      }
   }
}
