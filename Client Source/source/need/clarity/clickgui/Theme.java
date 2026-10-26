package need.clarity.clickgui;

import java.util.Map;

public final class Theme {
   public int accent;
   public int accent2;
   public int bg;
   public int surface;
   public int border;
   public int text;
   public int muted;
   public boolean light;

   public static Theme from(Map<String, String> t) {
      Theme th = new Theme();
      th.accent = 0xFF000000 | hex(t.get("accent"), 8037111);
      th.accent2 = 0xFF000000 | hex(t.get("accent2"), 4020641);
      th.bg = 0xFF000000 | hex(t.get("background"), 724503);
      th.surface = 0xFF000000 | hex(t.get("surface"), 1448742);
      th.border = 0xFF000000 | hex(t.get("border"), 2765122);
      th.light = luma(th.surface) > 0.55F;
      th.text = 0xFF000000 | hex(t.get("text"), th.light ? 1447965 : 15592944);
      th.muted = 0xFF000000 | hex(t.get("muted"), th.light ? 6120048 : 9145235);
      if (th.light && luma(th.text) > 0.6F) {
         th.text = -15329251;
         th.muted = -10657168;
      }

      return th;
   }

   public static Theme lerp(Theme a, Theme b, float t) {
      Theme o = new Theme();
      o.accent = mix(a.accent, b.accent, t);
      o.accent2 = mix(a.accent2, b.accent2, t);
      o.bg = mix(a.bg, b.bg, t);
      o.surface = mix(a.surface, b.surface, t);
      o.border = mix(a.border, b.border, t);
      o.text = mix(a.text, b.text, t);
      o.muted = mix(a.muted, b.muted, t);
      o.light = t < 0.5F ? a.light : b.light;
      return o;
   }

   public static int hex(String s, int def) {
      if (s == null) {
         return def;
      }

      s = s.trim();
      if (s.startsWith("#")) {
         s = s.substring(1);
      }

      if (s.length() == 3) {
         s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
      }

      try {
         return Integer.parseInt(s.substring(0, Math.min(6, s.length())), 16) & 16777215;
      } catch (Exception e) {
         return def;
      }
   }

   public static String toHex(int rgb) {
      return String.format("#%06X", rgb & 16777215);
   }

   public static int a(int argb, float alpha) {
      int base = argb >>> 24 & 0xFF;
      int na = Math.round(base * Math.max(0.0F, Math.min(1.0F, alpha)));
      return na << 24 | argb & 16777215;
   }

   public static int mix(int c1, int c2, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int a = Math.round((c1 >>> 24 & 0xFF) + ((c2 >>> 24 & 0xFF) - (c1 >>> 24 & 0xFF)) * t);
      int r = Math.round((c1 >> 16 & 0xFF) + ((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * t);
      int g = Math.round((c1 >> 8 & 0xFF) + ((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * t);
      int b = Math.round((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static float luma(int c) {
      return ((c >> 16 & 0xFF) * 0.299F + (c >> 8 & 0xFF) * 0.587F + (c & 0xFF) * 0.114F) / 255.0F;
   }

   public static int onColor(int c) {
      return luma(c) > 0.62F ? -15658216 : -1;
   }

   public static float[] rgbToHsv(int rgb) {
      float r = (rgb >> 16 & 0xFF) / 255.0F;
      float g = (rgb >> 8 & 0xFF) / 255.0F;
      float b = (rgb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      float h = 0.0F;
      if (d > 1.0E-6F) {
         if (max == r) {
            h = (g - b) / d % 6.0F;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            h++;
         }
      }

      return new float[]{h, max <= 0.0F ? 0.0F : d / max, max};
   }

   public static int hsvToRgb(float h, float s, float v) {
      h = (h % 1.0F + 1.0F) % 1.0F;
      float c = v * s;
      float x = c * (1.0F - Math.abs(h * 6.0F % 2.0F - 1.0F));
      float m = v - c;
      int sec = (int)(h * 6.0F);
      float r;
      float g;
      float b;
      switch (sec) {
         case 0:
            r = c;
            g = x;
            b = 0.0F;
            break;
         case 1:
            r = x;
            g = c;
            b = 0.0F;
            break;
         case 2:
            r = 0.0F;
            g = c;
            b = x;
            break;
         case 3:
            r = 0.0F;
            g = x;
            b = c;
            break;
         case 4:
            r = x;
            g = 0.0F;
            b = c;
            break;
         default:
            r = c;
            g = 0.0F;
            b = x;
      }

      return Math.round((r + m) * 255.0F) << 16 | Math.round((g + m) * 255.0F) << 8 | Math.round((b + m) * 255.0F);
   }
}
