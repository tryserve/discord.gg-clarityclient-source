package need.clarity.modules.visual;

import need.clarity.audio.AudioDemand;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public class ShaderESP extends ModuleBase {
   public static final String[] MODES = new String[]{"Glow", "Blur", "Galaxy", "Prism", "Neon", "Scan", "Liquid Glass", "Outline", "Contour", "Cel", "Pulse"};
   private static ShaderESP INSTANCE;

   public ShaderESP() {
      super("Shader ESP", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      ShaderESP i = INSTANCE;
      return i != null && i.isEnabled();
   }

   public static String mode() {
      String m = get("Mode", "Glow");

      for (String s : MODES) {
         if (s.equals(m)) {
            return s;
         }
      }

      return "Glow";
   }

   public static String colorMode() {
      return get("Color Mode", "Custom");
   }

   public static boolean chroma() {
      return "Chroma".equals(colorMode());
   }

   public static float glowRadius() {
      return clamp(get("Glow Radius", 12.0F), 1.0F, 60.0F);
   }

   public static float intensity() {
      return clamp(get("Intensity", 1.0F), 0.0F, 3.0F);
   }

   public static float fill() {
      return clamp(get("Fill Opacity", 0.15F), 0.0F, 1.0F);
   }

   public static boolean outline() {
      return get("Outline", true);
   }

   public static float blur() {
      return clamp(get("Blur", 10.0F), 0.0F, 40.0F);
   }

   public static float tintAmount() {
      return clamp(get("Tint Amount", 0.35F), 0.0F, 1.0F);
   }

   public static float speed() {
      return clamp(get("Speed", 1.0F), 0.0F, 5.0F);
   }

   public static float outlineWidth() {
      return clamp(get("Outline Width", 2.5F), 0.5F, 12.0F);
   }

   public static boolean hairline() {
      return get("Hairline", true);
   }

   public static float thickness() {
      return clamp(get("Thickness", 14.0F), 4.0F, 40.0F);
   }

   public static float refraction() {
      return clamp(get("Refraction", 1.0F), 0.0F, 3.0F);
   }

   public static float dispersion() {
      return clamp(get("Dispersion", 0.25F), 0.0F, 1.0F);
   }

   public static float frost() {
      return clamp(get("Frost", 0.0F), 0.0F, 20.0F);
   }

   public static float glassTint() {
      return clamp(get("Glass Tint", 0.25F), 0.0F, 1.0F);
   }

   public static float lines() {
      return clamp(get("Lines", 6.0F), 2.0F, 16.0F);
   }

   public static float celOpacity() {
      return clamp(get("Cel Opacity", 0.9F), 0.0F, 1.0F);
   }

   public static float reactivity() {
      return clamp(get("Reactivity", 0.6F), 0.0F, 1.5F);
   }

   public static boolean idleBeats() {
      return get("Idle Beats", true);
   }

   @Override
   public void onTick() {
      if ("Pulse".equals(mode())) {
         AudioDemand.want(get("Audio Sensitivity", 1.0F));
      }
   }

   public static boolean hideSelf() {
      return !get("Self", false);
   }

   public static boolean hideTeammates() {
      return get("Hide Teammates", false);
   }

   public static boolean showInvisible() {
      return get("Show Invisible", true);
   }

   public static float maxDistance() {
      return Math.max(0.0F, get("Max Distance", 0.0F));
   }

   public static int customColor() {
      if (INSTANCE == null) {
         return 9075967;
      }

      Object raw = INSTANCE.getSetting("Color", null);
      return ThemeColors.resolveRgb(raw, 9075967, false);
   }

   public static boolean shouldTarget(Player p) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && p != null) {
         Player self = mc.player;
         if (p == self && hideSelf()) {
            return false;
         }

         if (!p.isAlive() || p.isSpectator()) {
            return false;
         }

         if (p.isInvisible() && !showInvisible()) {
            return false;
         }

         if (p != self && hideTeammates() && isTeammate(p, self)) {
            return false;
         }

         float max = maxDistance();
         return max <= 0.5F || self.distanceTo(p) <= max;
      } else {
         return false;
      }
   }

   public static int colorFor(Player p) {
      return switch (colorMode()) {
         case "Team" -> {
            int c = p.getTeamColor() & 16777215;
            yield c == 16777215 && p.getTeam() == null ? customColor() : c;
         }
         case "Health" -> {
            float f = p.getMaxHealth() > 0.0F ? clamp(p.getHealth() / p.getMaxHealth(), 0.0F, 1.0F) : 1.0F;
            int r = Math.round(255.0F * Math.min(1.0F, 2.0F - 2.0F * f));
            int g = Math.round(255.0F * Math.min(1.0F, 2.0F * f));
            yield r << 16 | g << 8 | 48;
         }
         default -> customColor();
      };
   }

   private static <T> T get(String key, T def) {
      ShaderESP i = INSTANCE;
      if (i == null) {
         return def;
      } else {
         Object v = i.getSetting(key, def);
         if (def instanceof Float && v instanceof Number n) {
            return (T)Float.valueOf(n.floatValue());
         } else if (def instanceof Boolean && !(v instanceof Boolean)) {
            return def;
         } else {
            return (T)(def instanceof String && !(v instanceof String) ? def : v);
         }
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
   }

   private static boolean isTeammate(Player a, Player b) {
      PlayerTeam ta = a.getTeam();
      PlayerTeam tb = b.getTeam();
      if (ta != null && tb != null) {
         String na = ta.getName();
         return na != null && na.equals(tb.getName());
      } else {
         return false;
      }
   }
}

