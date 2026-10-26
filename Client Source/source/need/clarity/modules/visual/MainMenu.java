package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class MainMenu extends ModuleBase {
   public static MainMenu INSTANCE;
   public static final String DEFAULT_CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";

   public MainMenu() {
      super("Main Menu", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static String background() {
      String s = get("Background", "Silk");

      return switch (s) {
         case "Grain", "Dots", "Aurora", "Nebula", "Contours", "Rain", "Bokeh" -> s;
         default -> "Silk";
      };
   }

   public static String layout() {
      String s = get("Layout", "Center");

      return switch (s) {
         case "Left", "Minimal" -> s;
         default -> "Center";
      };
   }

   public static boolean inlineLists() {
      return get("Inline Lists", true);
   }

   public static float quality() {
      return switch ((String)get("Shader Quality", "High")) {
         case "Ultra" -> 1.0F;
         case "Balanced" -> 0.35F;
         default -> 0.5F;
      };
   }

   public static float glow() {
      return clamp(get("Logo Glow", 1.0F), 0.0F, 2.0F);
   }

   public static float speed() {
      return clamp(get("Shader Speed", 1.0F), 0.0F, 3.0F);
   }

   public static float spotlight() {
      return clamp(get("Cursor Light", 1.0F), 0.0F, 2.0F);
   }

   public static boolean parallax() {
      return get("Parallax", true);
   }

   public static boolean showAccount() {
      return get("Show Account", true);
   }

   public static String clientId() {
      String s = get("Azure Client ID", "");
      s = s == null ? "" : s.trim();
      return s.isEmpty() ? "54fd49e4-2103-4044-9603-2b028c814ec3" : s;
   }

   private static <T> T get(String key, T def) {
      MainMenu i = INSTANCE;
      if (i == null) {
         return def;
      } else {
         Object v = i.getSetting(key, def);
         if (def instanceof Float && v instanceof Number n) {
            return (T)Float.valueOf(n.floatValue());
         } else {
            return (T)(def.getClass().isInstance(v) ? v : def);
         }
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
   }
}

