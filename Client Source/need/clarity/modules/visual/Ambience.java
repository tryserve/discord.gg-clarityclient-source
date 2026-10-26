package need.clarity.modules.visual;

import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;

public class Ambience extends ModuleBase {
   private static Ambience INSTANCE;
   private static final int FOG_DEFAULT = 12638463;
   private static final int LIGHTMAP_DEFAULT = 16777215;
   private static final int SKY_DEFAULT = 7907327;
   private static final int SKY_GRADIENT_DEFAULT = 1714796;

   public Ambience() {
      super("Ambience", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      String[] overrides = new String[]{"Override Time", "Override Weather", "Override Fog Colour", "Override Fog Distance", "Override Lightmap", "Override Sky Colour"};

      for (String k : overrides) {
         try {
            ClarityConfig.putSetting(this.getName(), k, true);
         } catch (Throwable var7) {
         }
      }
   }

   @Override
   public void onDisable() {
   }

   public static boolean isOverrideTimeEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Time", false);
   }

   public static long getOverriddenDayTime() {
      Ambience i = INSTANCE;
      int tod = i != null ? i.getSetting("Time of Day", 6000) : 6000;
      return Math.clamp(tod, 0, 23999);
   }

   public static float getOverriddenTimeOfDay(float original) {
      return !isOverrideTimeEnabled() ? original : (float)getOverriddenDayTime() / 24000.0F;
   }

   public static boolean isOverrideWeatherEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Weather", false);
   }

   public static String getWeatherMode() {
      Ambience i = INSTANCE;
      return i != null ? i.getSetting("Weather", "Clear") : "Clear";
   }

   public static boolean isOverrideFogColourEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Fog Colour", false);
   }

   public static float[] getFogColour() {
      Ambience i = INSTANCE;
      String hex = i != null ? i.getSetting("Fog Colour", "#c0d8ff") : "#c0d8ff";
      return hexToRgb(hex, 12638463, false);
   }

   public static boolean isOverrideFogDistanceEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Fog Distance", false);
   }

   public static int getFogDistanceChunks() {
      Ambience i = INSTANCE;
      int chunks = i != null ? i.getSetting("Fog Distance", 16) : 16;
      return Math.max(0, chunks);
   }

   public static boolean isOverrideLightmapEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Lightmap", false);
   }

   public static float[] getLightmapColour() {
      Ambience i = INSTANCE;
      String hex = i != null ? i.getSetting("Lightmap Colour", "#ffffff") : "#ffffff";
      int rgb = ThemeColors.resolveRgb(hex, 16777215, false);
      return new float[]{(rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
   }

   public static boolean isOverrideSkyColourEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Override Sky Colour", false);
   }

   public static float[] getSkyColour() {
      Ambience i = INSTANCE;
      String hex = i != null ? i.getSetting("Sky Colour", "#78a7ff") : "#78a7ff";
      return hexToRgb(hex, 7907327, false);
   }

   public static boolean isSkyGradientEnabled() {
      Ambience i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Sky Gradient", false);
   }

   public static float[] getSkyGradientColour2() {
      Ambience i = INSTANCE;
      String hex = i != null ? i.getSetting("Sky Gradient Colour 2", "#1a2a6c") : "#1a2a6c";
      return hexToRgb(hex, 1714796, true);
   }

   private static float[] hexToRgb(String hex, int fallbackRgb, boolean secondary) {
      int rgb = ThemeColors.resolveRgb(hex, fallbackRgb, secondary);
      return new float[]{(rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
   }
}
