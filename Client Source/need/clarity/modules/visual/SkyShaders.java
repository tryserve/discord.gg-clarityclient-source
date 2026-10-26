package need.clarity.modules.visual;

import java.util.Locale;
import java.util.Set;
import need.clarity.audio.AudioDemand;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;

public class SkyShaders extends ModuleBase {
   public static final String[] PRESETS = new String[]{
      "Aurora",
      "Galaxy",
      "Black Hole",
      "Thunderstorm",
      "Nebula",
      "Synthwave",
      "Blood Moon",
      "Dreamscape",
      "Glow Lines",
      "Fog",
      "Music",
      "Sunset",
      "Abyss",
      "Glacier",
      "Lagoon",
      "Deep Sea",
      "Molten",
      "Beat Storm",
      "Pulse",
      "Beat Aurora",
      "Warp",
      "Prism Grid",
      "Flow Lines",
      "Cells",
      "Halo Rings"
   };
   private static SkyShaders INSTANCE;
   public static final Set<String> AUDIO_PRESETS = Set.of("Music", "Beat Storm", "Pulse", "Beat Aurora", "Warp");
   private static float[] prismA;
   private static float[] prismB;
   private static long prismAt = 0L;

   public SkyShaders() {
      super("Sky Shaders", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      SkyShaders i = INSTANCE;
      return i != null && i.isEnabled();
   }

   public static boolean isAudioPreset(String p) {
      return AUDIO_PRESETS.contains(p);
   }

   public static boolean usesAudio() {
      return active() && isAudioPreset(preset());
   }

   @Override
   public void onTick() {
      if (usesAudio()) {
         AudioDemand.want(get("Audio Sensitivity", 1.0F));
      }
   }

   public static float reactivity() {
      return clamp(get("Reactivity", 0.6F), 0.0F, 1.5F);
   }

   public static float idleBeat() {
      if (!get("Idle Beats", true)) {
         return 0.0F;
      }

      return switch (preset()) {
         case "Beat Storm" -> 1.9F;
         case "Pulse", "Beat Aurora", "Warp" -> 0.95F;
         default -> 0.0F;
      };
   }

   public static String preset() {
      String p = INSTANCE != null ? INSTANCE.getSetting("Preset", "Aurora") : "Aurora";
      if ("Prism".equals(p)) {
         return "Glow Lines";
      }

      for (String s : PRESETS) {
         if (s.equals(p)) {
            return s;
         }
      }

      return "Aurora";
   }

   public static String presetFile(String preset) {
      return preset.toLowerCase(Locale.ROOT).replace(" ", "");
   }

   public static float speed() {
      return clamp(get("Speed", 1.0F), 0.0F, 5.0F);
   }

   public static float brightness() {
      return clamp(get("Brightness", 1.0F), 0.0F, 3.0F);
   }

   public static float intensity() {
      return clamp(get("Intensity", 1.0F), 0.0F, 3.0F);
   }

   public static float scale() {
      return clamp(get("Scale", 1.0F), 0.25F, 3.0F);
   }

   public static float stars() {
      return clamp(get("Stars", 1.0F), 0.0F, 3.0F);
   }

   public static float rotation() {
      return clamp(get("Sky Rotation", 0.3F), -5.0F, 5.0F);
   }

   public static float horizonBlend() {
      return clamp(get("Horizon Blend", 0.5F), 0.0F, 1.0F);
   }

   public static boolean matchFog() {
      return get("Match Fog", true);
   }

   public static boolean keepSunMoon() {
      return get("Sun & Moon", false);
   }

   public static boolean hideClouds() {
      return active() && get("Hide Clouds", true);
   }

   public static boolean inTheEnd() {
      return get("In The End", true);
   }

   public static float fogDistance() {
      return clamp(get("Fog Distance", 48.0F), 8.0F, 256.0F);
   }

   public static float resolutionScale() {
      String r = INSTANCE != null ? INSTANCE.getSetting("Resolution", "Full") : "Full";

      return switch (r) {
         case "75%" -> 0.75F;
         case "50%" -> 0.5F;
         default -> 1.0F;
      };
   }

   private static int[] naturalColors(String preset) {
      return switch (preset) {
         case "Galaxy" -> new int[]{16763274, 9413375};
         case "Black Hole" -> new int[]{16767392, 16738847};
         case "Thunderstorm" -> new int[]{13227775, 5923712};
         case "Nebula" -> new int[]{16727450, 3065343};
         case "Synthwave" -> new int[]{16722541, 383464};
         case "Blood Moon" -> new int[]{16726831, 5900810};
         case "Dreamscape" -> new int[]{8300543, 16757703};
         case "Glow Lines" -> new int[]{ThemeColors.getAccentRgb(), ThemeColors.getAccent2Rgb()};
         case "Fog" -> new int[]{13161727, 6055538};
         case "Music" -> new int[]{4904703, 11820287};
         case "Sunset" -> new int[]{16747069, 2899578};
         case "Abyss" -> new int[]{8382975, 275550};
         case "Glacier" -> new int[]{12580607, 667466};
         case "Lagoon" -> new int[]{16773568, 1024952};
         case "Deep Sea" -> new int[]{4063184, 133658};
         case "Molten" -> new int[]{16747040, 3803652};
         case "Beat Storm" -> new int[]{14082303, 5134456};
         case "Pulse" -> new int[]{8031487, 3874414};
         case "Beat Aurora" -> new int[]{2883486, 11554303};
         case "Warp" -> new int[]{10479871, 16732120};
         case "Prism Grid" -> new int[]{4583679, 11820287};
         case "Flow Lines" -> new int[]{8191922, 4029439};
         case "Cells" -> new int[]{16735400, 6080767};
         case "Halo Rings" -> new int[]{16761707, 9067775};
         default -> new int[]{2883464, 9063935};
      };
   }

   public static float[][] colors() {
      String preset = preset();
      if ("Glow Lines".equals(preset) && get("Natural Colors", true)) {
         return themeBlend();
      }

      int[] nat = naturalColors(preset);
      int c1 = nat[0];
      int c2 = nat[1];
      if (!get("Natural Colors", true) && INSTANCE != null) {
         Object r1 = INSTANCE.getSetting("Color", null);
         Object r2 = INSTANCE.getSetting("Second Color", null);
         c1 = ThemeColors.resolveRgb(r1, c1, false);
         c2 = ThemeColors.resolveRgb(r2, c2, true);
      }

      return new float[][]{rgb(c1), rgb(c2)};
   }

   public static float[] horizon(String preset, float[] c1, float[] c2) {
      return switch (preset) {
         case "Galaxy" -> mix(rgb(790300), c2, 0.1F);
         case "Black Hole" -> mix(rgb(657936), c2, 0.06F);
         case "Thunderstorm" -> scale(c2, 0.45F);
         case "Nebula" -> mix(rgb(789014), mix(c1, c2, 0.5F), 0.08F);
         case "Synthwave" -> scale(c1, 0.8F);
         case "Blood Moon" -> scale(c2, 0.8F);
         case "Dreamscape" -> mix(c2, rgb(16769216), 0.25F);
         case "Glow Lines" -> mix(rgb(460815), mix(c1, c2, 0.5F), 0.14F);
         case "Fog" -> scale(c2, 0.78F);
         case "Music" -> mix(rgb(526351), mix(c1, c2, 0.5F), 0.1F);
         case "Sunset" -> scale(mix(c1, rgb(16763296), 0.35F), 0.85F);
         case "Abyss" -> scale(mix(c2, c1, 0.12F), 0.65F);
         case "Glacier" -> scale(mix(c2, c1, 0.1F), 0.6F);
         case "Lagoon" -> scale(c2, 0.8F);
         case "Deep Sea" -> scale(c2, 1.2F);
         case "Molten" -> scale(mix(c2, c1, 0.15F), 0.7F);
         case "Beat Storm" -> scale(c2, 0.45F);
         case "Pulse" -> mix(rgb(328969), c2, 0.1F);
         case "Beat Aurora" -> mix(rgb(726562), c1, 0.08F);
         case "Warp" -> mix(rgb(394764), c2, 0.1F);
         case "Prism Grid", "Flow Lines", "Cells", "Halo Rings" -> mix(rgb(329227), mix(c1, c2, 0.5F), 0.08F);
         default -> mix(rgb(726562), c1, 0.08F);
      };
   }

   public static synchronized float[][] themeBlend() {
      float[] ta = rgb(ThemeColors.getAccentRgb());
      float[] tb = rgb(ThemeColors.getAccent2Rgb());
      long now = System.nanoTime();
      if (prismA == null) {
         prismA = (float[])ta.clone();
         prismB = (float[])tb.clone();
      }

      float dt = prismAt == 0L ? 0.0F : Math.min(0.1F, (float)(now - prismAt) / 1.0E9F);
      prismAt = now;
      float k = 1.0F - (float)Math.exp(-dt * 3.2F);
      prismA = mix(prismA, ta, k);
      prismB = mix(prismB, tb, k);
      return new float[][]{(float[])prismA.clone(), (float[])prismB.clone()};
   }

   private static <T> T get(String key, T def) {
      SkyShaders i = INSTANCE;
      if (i == null) {
         return def;
      } else {
         Object v = i.getSetting(key, def);
         if (def instanceof Float && v instanceof Number n) {
            return (T)Float.valueOf(n.floatValue());
         } else {
            return (T)(def instanceof Boolean && !(v instanceof Boolean) ? def : v);
         }
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
   }

   private static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }

   private static float[] mix(float[] a, float[] b, float t) {
      return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
   }

   private static float[] scale(float[] a, float s) {
      return new float[]{a[0] * s, a[1] * s, a[2] * s};
   }
}

