package need.clarity.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import need.clarity.config.ClarityConfig;

public final class ThemeColors {
   public static final int DEFAULT_ACCENT_RGB = 8037111;
   public static final int DEFAULT_ACCENT2_RGB = 4020641;

   private ThemeColors() {
   }

   private static JsonObject themeObject() {
      try {
         JsonElement el = ClarityConfig.getCurrent().get("__theme");
         if (el != null && el.isJsonObject()) {
            return el.getAsJsonObject();
         }
      } catch (Throwable var1) {
      }

      return null;
   }

   public static boolean isEnabled() {
      JsonObject t = themeObject();
      if (t == null) {
         return true;
      }

      try {
         JsonElement en = t.get("enabled");
         return en != null && en.isJsonPrimitive() ? en.getAsBoolean() : true;
      } catch (Throwable ignored) {
         return true;
      }
   }

   private static int readHex(String key, int fallback) {
      JsonObject t = themeObject();
      if (t == null) {
         return fallback;
      }

      try {
         JsonElement v = t.get(key);
         if (v != null && v.isJsonPrimitive()) {
            return parseHexRgb(v.getAsString(), fallback);
         }
      } catch (Throwable var5) {
      }

      return fallback;
   }

   public static int getAccentRgb() {
      return Chroma.active() ? Chroma.accent() : readHex("accent", 8037111);
   }

   public static int getAccent2Rgb() {
      return Chroma.active() ? Chroma.accent2() : readHex("accent2", 4020641);
   }

   public static int getAccentArgb() {
      return 0xFF000000 | getAccentRgb();
   }

   public static int getAccent2Argb() {
      return 0xFF000000 | getAccent2Rgb();
   }

   public static int getSurfaceRgb() {
      return readHex("surface", 1448742);
   }

   public static int getBorderRgb() {
      return readHex("border", 2765122);
   }

   public static int getTextRgb() {
      return readHex("text", 15592944);
   }

   public static int getMutedRgb() {
      return readHex("muted", 9145235);
   }

   public static boolean isLight() {
      int s = getSurfaceRgb();
      int r = s >> 16 & 0xFF;
      int g = s >> 8 & 0xFF;
      int b = s & 0xFF;
      return 0.299 * r + 0.587 * g + 0.114 * b > 140.0;
   }

   public static int resolveRgb(Object value, int fallback, boolean secondary) {
      int themed = themedRgb(value, secondary);
      return themed >= 0 ? themed : parseHexRgb(value, fallback);
   }

   public static int resolveArgb(Object value, int fallbackArgb, boolean secondary) {
      if (isThemeValue(value)) {
         String s = value.toString().trim();
         return s.equalsIgnoreCase("theme2") ? getAccent2Argb() : getAccentArgb();
      } else if (isEnabled()) {
         return secondary ? getAccent2Argb() : getAccentArgb();
      } else {
         return parseArgb(value, fallbackArgb);
      }
   }

   public static boolean isThemeValue(Object value) {
      if (value == null) {
         return false;
      }

      String s = value.toString().trim();
      return s.equalsIgnoreCase("theme") || s.equalsIgnoreCase("theme2");
   }

   private static int themedRgb(Object value, boolean secondary) {
      if (isThemeValue(value)) {
         String s = value.toString().trim();
         return s.equalsIgnoreCase("theme2") ? getAccent2Rgb() : getAccentRgb();
      } else if (isEnabled()) {
         return secondary ? getAccent2Rgb() : getAccentRgb();
      } else {
         return -1;
      }
   }

   private static boolean looksLikeHex(String s) {
      if (s == null) {
         return false;
      }

      String h = s.startsWith("#") ? s.substring(1) : s;
      if (h.length() != 6 && h.length() != 8) {
         return false;
      }

      for (int i = 0; i < h.length(); i++) {
         if (Character.digit(h.charAt(i), 16) < 0) {
            return false;
         }
      }

      return true;
   }

   public static int parseHexRgb(Object value, int fallback) {
      if (value == null) {
         return fallback;
      }

      String h = value.toString().trim();
      if (h.startsWith("#")) {
         h = h.substring(1);
      }

      try {
         if (h.length() == 6) {
            return (int)Long.parseLong(h, 16) & 16777215;
         }

         if (h.length() == 8) {
            return (int)Long.parseLong(h, 16) & 16777215;
         }
      } catch (NumberFormatException var4) {
      }

      return fallback;
   }

   public static int parseArgb(Object value, int fallback) {
      if (value == null) {
         return fallback;
      }

      String h = value.toString().trim();
      if (h.startsWith("#")) {
         h = h.substring(1);
      }

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
   }
}
