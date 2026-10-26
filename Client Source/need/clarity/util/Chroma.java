package need.clarity.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import need.clarity.config.ClarityConfig;

public final class Chroma {
   private static boolean cached;
   private static long checkedAt;
   private static float speed = 1.0F;

   private Chroma() {
   }

   public static boolean active() {
      long now = System.nanoTime();
      if (now - checkedAt < 250000000L) {
         return cached;
      }

      checkedAt = now;
      boolean on = false;
      float sp = 1.0F;

      try {
         JsonObject root = ClarityConfig.getCurrent();
         JsonElement t = root.get("__theme");
         if (t != null && t.isJsonObject()) {
            JsonElement c = t.getAsJsonObject().get("chroma");
            if (c != null && c.isJsonPrimitive()) {
               on = c.getAsBoolean();
            }

            JsonElement en = t.getAsJsonObject().get("enabled");
            if (en != null && en.isJsonPrimitive() && !en.getAsBoolean()) {
               on = false;
            }
         }

         JsonElement g = root.get("Click GUI");
         if (g != null && g.isJsonObject()) {
            JsonElement v = g.getAsJsonObject().get("values");
            if (v != null && v.isJsonObject()) {
               JsonElement s = v.getAsJsonObject().get("Chroma Speed");
               if (s != null && s.isJsonPrimitive()) {
                  sp = s.getAsFloat();
               }

               JsonElement name = v.getAsJsonObject().get("Theme");
               if (name != null && name.isJsonPrimitive()) {
                  on = "Chroma".equals(name.getAsString()) && themeOn(root);
               }
            }
         }
      } catch (Throwable var10) {
      }

      speed = Math.max(0.05F, Math.min(5.0F, sp));
      cached = on;
      return on;
   }

   private static boolean themeOn(JsonObject root) {
      JsonElement t = root.get("__theme");
      if (t != null && t.isJsonObject()) {
         JsonElement en = t.getAsJsonObject().get("enabled");
         return en == null || !en.isJsonPrimitive() || en.getAsBoolean();
      } else {
         return true;
      }
   }

   public static float hue(float offset) {
      double t = System.nanoTime() / 1.0E9 * speed / 12.0;
      return (float)((t + offset) % 1.0);
   }

   public static int accent() {
      return hsv(hue(0.0F), 0.62F, 1.0F);
   }

   public static int accent2() {
      return hsv(hue(0.07F), 0.78F, 0.78F);
   }

   public static int hsv(float h, float s, float v) {
      h = (h % 1.0F + 1.0F) % 1.0F;
      int i = (int)(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return Math.round(r * 255.0F) << 16 | Math.round(g * 255.0F) << 8 | Math.round(b * 255.0F);
   }
}
