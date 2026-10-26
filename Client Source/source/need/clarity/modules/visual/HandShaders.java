package need.clarity.modules.visual;

import need.clarity.audio.AudioDemand;
import need.clarity.modules.ModuleBase;
import need.clarity.render.ShaderHelper;
import need.clarity.util.ThemeColors;

public class HandShaders extends ModuleBase {
   public static final String[] SHADER_NAMES = new String[]{
      "None", "Chroma", "Invert", "Outline", "Glass", "Glow", "Hologram", "Prism", "Blur", "Pixelate", "Galaxy", "Neon", "Wisp", "Abyss", "Liquid Glass", "Beat Pulse", "Trails"
   };
   public static HandShaders INSTANCE;

   public HandShaders() {
      super("Hand Shaders", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onTick() {
      if ("Beat Pulse".equals(getShader())) {
         AudioDemand.want(this.getSetting("Audio Sensitivity", 1.0F));
      }
   }

   public void render() {
      ShaderHelper.render();
   }

   public static String getShader() {
      return INSTANCE != null ? INSTANCE.getSetting("Shader", "Chroma") : "None";
   }

   private static int parseColor(String hex, int fallback) {
      if (hex != null && !hex.isEmpty()) {
         String h = hex.startsWith("#") ? hex.substring(1) : hex;

         try {
            if (h.length() == 6) {
               return (int)(4278190080L | Long.parseLong(h, 16));
            }

            if (h.length() == 8) {
               return (int)Long.parseLong(h, 16);
            }
         } catch (NumberFormatException var4) {
         }

         return fallback;
      } else {
         return fallback;
      }
   }

   private static int getColorSetting(String key, int fallback) {
      String raw = INSTANCE.getSetting(key, "");
      return !raw.isEmpty() ? parseColor(raw, fallback) : fallback;
   }

   private static int getThemedColorSetting(String key, int fallback, boolean secondary) {
      String raw = INSTANCE.getSetting(key, "");
      return ThemeColors.resolveArgb(raw.isEmpty() ? null : raw, fallback, secondary);
   }

   public static float getChromaSpeed() {
      return INSTANCE.getSetting("Chroma Speed", 0.3F);
   }

   public static float getChromaSaturation() {
      return INSTANCE.getSetting("Chroma Saturation", 1.0F);
   }

   public static float getChromaBrightness() {
      return INSTANCE.getSetting("Chroma Brightness", 1.0F);
   }

   public static boolean getInvertHideHand() {
      return INSTANCE.getSetting("Hide Hand", true);
   }

   public static float getInvertIntensity() {
      return INSTANCE.getSetting("Invert Intensity", 1.0F);
   }

   public static boolean getInvertHue() {
      return INSTANCE.getSetting("Invert Hue", false);
   }

   public static boolean getInvertSaturation() {
      return INSTANCE.getSetting("Invert Saturation", false);
   }

   public static boolean getInvertBrightness() {
      return INSTANCE.getSetting("Invert Brightness", false);
   }

   public static float getInvertEdgeGlow() {
      return INSTANCE.getSetting("Edge Glow", 0.5F);
   }

   public static int getInvertGlowColor() {
      return getColorSetting("Invert Glow Color", -16776961);
   }

   public static boolean getGlowApplyToArm() {
      return INSTANCE.getSetting("Apply To Arm", true);
   }

   public static boolean getGlowApplyToItems() {
      return INSTANCE.getSetting("Apply To Items", true);
   }

   public static int getGlowColor() {
      return getColorSetting("Glow Color", -16720641);
   }

   public static double getGlowOpacity() {
      return INSTANCE.getSetting("Opacity", 0.6F).floatValue();
   }

   public static boolean getGlowTextured() {
      return INSTANCE.getSetting("Textured", true);
   }

   public static boolean getGlowFullbright() {
      return INSTANCE.getSetting("Fullbright", false);
   }

   public static boolean getGlowOutline() {
      return INSTANCE.getSetting("Outline", true);
   }

   public static int getGlowOutlineColor() {
      return getColorSetting("Glow Outline Color", -16720641);
   }

   public static double getGlowOutlineWidth() {
      return INSTANCE.getSetting("Outline Width", 1.0F).floatValue();
   }

   public static boolean getGlowBloom() {
      return INSTANCE.getSetting("Bloom", true);
   }

   public static double getGlowSpread() {
      return INSTANCE.getSetting("Glow Spread", 1.0F).floatValue();
   }

   public static boolean getGlowAntialiasing() {
      return INSTANCE.getSetting("Antialiasing", true);
   }

   public static int getOutlineColor() {
      return getThemedColorSetting("Outline Color", -1, false);
   }

   public static float getOutlineThickness() {
      return INSTANCE.getSetting("Outline Thickness", 1.5F);
   }

   public static boolean getTintEnabled() {
      return INSTANCE.getSetting("Tint", false);
   }

   public static int getTintColor() {
      return getThemedColorSetting("Tint Color", -65536, true);
   }

   public static float getTintAmount() {
      return INSTANCE.getSetting("Tint Amount", 0.4F);
   }

   public static boolean getBlurHandItself() {
      return "Hand Itself".equals(INSTANCE.getSetting("Blur Target", "Behind Hand"));
   }

   public static float getPixelSize() {
      return INSTANCE.getSetting("Pixel Size", 6.0F);
   }

   public static float getGlassBlur() {
      return INSTANCE.getSetting("Blur", 10.0F);
   }

   public static int getGlassTint() {
      return getThemedColorSetting("Glass Tint", -3151617, false);
   }

   public static float getGlassTintAmount() {
      return INSTANCE.getSetting("Glass Tint Amount", 0.2F);
   }

   public static float getGlassRefraction() {
      return INSTANCE.getSetting("Refraction", 1.0F);
   }

   public static float getGlassDetail() {
      return INSTANCE.getSetting("Detail", 0.35F);
   }

   public static float getGlassRim() {
      return INSTANCE.getSetting("Rim Light", 0.6F);
   }

   public static String getGlowMode() {
      return INSTANCE.getSetting("Glow Mode", "Custom");
   }

   public static int getGlowFxColor() {
      return getThemedColorSetting("Glow Color", -8740097, false);
   }

   public static float getGlowRadius() {
      return INSTANCE.getSetting("Glow Radius", 18.0F);
   }

   public static float getGlowIntensity() {
      return INSTANCE.getSetting("Glow Intensity", 1.0F);
   }

   public static boolean getGlowPulse() {
      return INSTANCE.getSetting("Glow Pulse", false);
   }

   public static String getWispMode() {
      return INSTANCE.getSetting("Wisp Mode", "Custom");
   }

   public static int getWispColor() {
      return getThemedColorSetting("Wisp Color", -7701249, false);
   }

   public static float getWispTrail() {
      return INSTANCE.getSetting("Trail Length", 0.45F);
   }

   public static float getWispTurbulence() {
      return INSTANCE.getSetting("Turbulence", 1.0F);
   }

   public static float getWispInertia() {
      return INSTANCE.getSetting("Inertia", 1.0F);
   }

   public static float getWispIntensity() {
      return INSTANCE.getSetting("Wisp Intensity", 1.0F);
   }

   public static int getAbyssLight() {
      return getThemedColorSetting("Abyss Light", -8394241, false);
   }

   public static int getAbyssDeep() {
      return getThemedColorSetting("Abyss Deep", -16501666, true);
   }

   public static float getAbyssCaustics() {
      return INSTANCE.getSetting("Caustics", 1.0F);
   }

   public static float getAbyssEdgeLine() {
      return INSTANCE.getSetting("Edge Line", 1.0F);
   }

   public static float getAbyssLineSpeed() {
      return INSTANCE.getSetting("Line Speed", 1.0F);
   }

   public static int getHoloColor() {
      return getThemedColorSetting("Holo Color", -13047553, false);
   }

   public static float getHoloOpacity() {
      return INSTANCE.getSetting("Holo Opacity", 0.55F);
   }

   public static boolean getHoloScanlines() {
      return INSTANCE.getSetting("Scanlines", true);
   }

   public static boolean getHoloGlitch() {
      return INSTANCE.getSetting("Glitch", true);
   }

   public static float getHoloHalo() {
      return INSTANCE.getSetting("Halo", 0.6F);
   }

   public static float getLiquidRefraction() {
      return INSTANCE.getSetting("Liquid Refraction", 1.0F);
   }

   public static float getLiquidThickness() {
      return INSTANCE.getSetting("Thickness", 14.0F);
   }

   public static float getLiquidDispersion() {
      return INSTANCE.getSetting("Dispersion", 0.25F);
   }

   public static float getLiquidFrost() {
      return INSTANCE.getSetting("Frost", 0.0F);
   }

   public static int getLiquidTint() {
      return getThemedColorSetting("Liquid Tint", -1510145, false);
   }

   public static float getLiquidTintAmount() {
      return INSTANCE.getSetting("Liquid Tint Amount", 0.12F);
   }

   public static float getLiquidSpecular() {
      return INSTANCE.getSetting("Specular", 1.0F);
   }

   public static float getLiquidDetail() {
      return INSTANCE.getSetting("Keep Detail", 0.3F);
   }

   public static String getPulseMode() {
      return INSTANCE.getSetting("Pulse Mode", "Gradient");
   }

   public static int getPulseColor() {
      return getThemedColorSetting("Pulse Color", -11872513, false);
   }

   public static int getPulseColor2() {
      return getThemedColorSetting("Pulse Second Color", -4956929, true);
   }

   public static float getPulseRadius() {
      return INSTANCE.getSetting("Pulse Radius", 20.0F);
   }

   public static float getPulseIntensity() {
      return INSTANCE.getSetting("Pulse Intensity", 1.0F);
   }

   public static String getTrailMode() {
      return INSTANCE.getSetting("Trail Mode", "Hand Colors");
   }

   public static int getTrailColor() {
      return getThemedColorSetting("Trail Color", -8740097, false);
   }

   public static int getTrailColor2() {
      return getThemedColorSetting("Trail Second Color", -38187, true);
   }

   public static float getTrailLength() {
      return INSTANCE.getSetting("Trail Duration", 0.6F);
   }

   public static float getTrailOpacity() {
      return INSTANCE.getSetting("Trail Opacity", 0.7F);
   }

   public static float getTrailStrength() {
      return INSTANCE.getSetting("Trail Strength", 1.0F);
   }

   public static String getTrailFade() {
      return INSTANCE.getSetting("Fade Curve", "Smooth");
   }

   public static String getTrailBlend() {
      return INSTANCE.getSetting("Trail Blend", "Normal");
   }

   public static boolean getTrailEcho() {
      return INSTANCE.getSetting("Echo", false);
   }

   public static float getTrailEchoSpacing() {
      return INSTANCE.getSetting("Echo Spacing", 70.0F);
   }

   public static float getTrailFollow() {
      return INSTANCE.getSetting("Camera Follow", 1.0F);
   }

   public static float getTrailDriftX() {
      return INSTANCE.getSetting("Drift X", 0.0F);
   }

   public static float getTrailDriftY() {
      return INSTANCE.getSetting("Drift Y", 0.0F);
   }

   public static float getTrailTurbulence() {
      return INSTANCE.getSetting("Trail Turbulence", 0.0F);
   }

   public static float getTrailSoftness() {
      return INSTANCE.getSetting("Softness", 0.5F);
   }

   public static float getTrailGrow() {
      return INSTANCE.getSetting("Grow", 0.0F);
   }

   public static float getTrailSaturation() {
      return INSTANCE.getSetting("Trail Saturation", 1.0F);
   }

   public static float getTrailBrightness() {
      return INSTANCE.getSetting("Trail Brightness", 1.0F);
   }

   public static boolean getTrailEdgeOnly() {
      return INSTANCE.getSetting("Edge Only", false);
   }

   public static boolean getTrailOverHand() {
      return INSTANCE.getSetting("Over Hand", false);
   }

   public static float getTrailChromaSpeed() {
      return INSTANCE.getSetting("Trail Chroma Speed", 1.0F);
   }

   public static String getTrailResolution() {
      return INSTANCE.getSetting("Trail Resolution", "Half");
   }

   public static float getPulseReactivity() {
      return INSTANCE.getSetting("Pulse Reactivity", 0.6F);
   }

   public static boolean getPulseIdleBeats() {
      return INSTANCE.getSetting("Idle Beats", true);
   }
}
