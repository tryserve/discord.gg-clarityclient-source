package need.clarity.agent.loader;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class LoaderTheme {
   private float[] bg = rgb(LoaderTheme.Palette.DEFAULT.bg);
   private float[] surface = rgb(LoaderTheme.Palette.DEFAULT.surface);
   private float[] border = rgb(LoaderTheme.Palette.DEFAULT.border);
   private float[] text = rgb(LoaderTheme.Palette.DEFAULT.text);
   private float[] muted = rgb(LoaderTheme.Palette.DEFAULT.muted);
   private float[] accent = rgb(LoaderTheme.Palette.DEFAULT.accent);
   private float[] accent2 = rgb(LoaderTheme.Palette.DEFAULT.accent2);
   private LoaderTheme.Palette target = LoaderTheme.Palette.DEFAULT;

   LoaderTheme.Palette target() {
      return this.target;
   }

   void setTarget(LoaderTheme.Palette p) {
      this.target = p;
   }

   void snap() {
      this.update(1.0F, 0.0);
   }

   void update(float k, double timeSec) {
      LoaderTheme.Palette p = this.target;
      lerp(this.bg, p.bg, k);
      lerp(this.surface, p.surface, k);
      lerp(this.border, p.border, k);
      lerp(this.text, p.text, k);
      lerp(this.muted, p.muted, k);
      if (p.chroma) {
         double t = timeSec / 12.0;
         lerp(this.accent, hsv((float)(t % 1.0), 0.62F, 1.0F), Math.min(1.0F, k * 3.0F));
         lerp(this.accent2, hsv((float)((t + 0.07) % 1.0), 0.78F, 0.78F), Math.min(1.0F, k * 3.0F));
      } else {
         lerp(this.accent, p.accent, k);
         lerp(this.accent2, p.accent2, k);
      }
   }

   boolean settled() {
      LoaderTheme.Palette p = this.target;
      return p.chroma
         ? false
         : near(this.bg, p.bg)
            && near(this.surface, p.surface)
            && near(this.border, p.border)
            && near(this.text, p.text)
            && near(this.muted, p.muted)
            && near(this.accent, p.accent)
            && near(this.accent2, p.accent2);
   }

   private static boolean near(float[] cur, int target) {
      float[] t = rgb(target);

      for (int i = 0; i < 3; i++) {
         if (Math.abs(t[i] - cur[i]) > 0.5F) {
            return false;
         }
      }

      return true;
   }

   Color bg() {
      return c(this.bg, 255);
   }

   Color surface() {
      return c(this.surface, 255);
   }

   Color border() {
      return c(this.border, 255);
   }

   Color text() {
      return c(this.text, 255);
   }

   Color muted() {
      return c(this.muted, 255);
   }

   Color accent() {
      return c(this.accent, 255);
   }

   Color accent2() {
      return c(this.accent2, 255);
   }

   Color accent(int alpha) {
      return c(this.accent, alpha);
   }

   Color accent2(int alpha) {
      return c(this.accent2, alpha);
   }

   Color border(int alpha) {
      return c(this.border, alpha);
   }

   Color surface(int alpha) {
      return c(this.surface, alpha);
   }

   Color text(int alpha) {
      return c(this.text, alpha);
   }

   Color onAccent() {
      float l = (0.299F * this.accent[0] + 0.587F * this.accent[1] + 0.114F * this.accent[2]) / 255.0F;
      return l > 0.62F ? new Color(1053466) : Color.WHITE;
   }

   private static Color c(float[] v, int a) {
      return new Color(clamp(v[0]), clamp(v[1]), clamp(v[2]), a);
   }

   private static int clamp(float f) {
      return Math.max(0, Math.min(255, Math.round(f)));
   }

   private static float[] rgb(int v) {
      return new float[]{v >> 16 & 0xFF, v >> 8 & 0xFF, v & 0xFF};
   }

   private static void lerp(float[] cur, int target, float k) {
      float[] t = rgb(target);

      for (int i = 0; i < 3; i++) {
         cur[i] += (t[i] - cur[i]) * k;
      }
   }

   static int hsv(float h, float s, float v) {
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

   static String toPref(LoaderTheme.Palette p) {
      return p.name().replace('|', ' ') + "|" + p.bg() + "|" + p.surface() + "|" + p.border() + "|" + p.text() + "|" + p.muted() + "|" + p.accent() + "|" + p.accent2() + "|" + p.chroma();
   }

   static LoaderTheme.Palette fromPref(String s) {
      try {
         if (s != null && !s.isEmpty()) {
            String[] a = s.split("\\|");
            return a.length < 9
               ? null
               : new LoaderTheme.Palette(
                  a[0],
                  Integer.parseInt(a[1]),
                  Integer.parseInt(a[2]),
                  Integer.parseInt(a[3]),
                  Integer.parseInt(a[4]),
                  Integer.parseInt(a[5]),
                  Integer.parseInt(a[6]),
                  Integer.parseInt(a[7]),
                  Boolean.parseBoolean(a[8]),
                  true
               );
         } else {
            return null;
         }
      } catch (Exception e) {
         return null;
      }
   }

   static LoaderTheme.Palette read(Path gameDir) {
      if (gameDir == null) {
         return LoaderTheme.Palette.DEFAULT;
      }

      try {
         Path f = gameDir.resolve("clarity_config.json");
         if (!Files.isRegularFile(f)) {
            return LoaderTheme.Palette.DEFAULT;
         }

         Object root = JsonLite.parse(Files.readString(f, StandardCharsets.UTF_8));
         Map<String, Object> theme = JsonLite.obj(root, "__theme");
         Map<String, Object> gui = JsonLite.obj(root, "Click GUI");
         Map<String, Object> vals = JsonLite.obj(gui, "values");
         String name = JsonLite.str(vals, "Theme");
         if (theme == null) {
            return new LoaderTheme.Palette(
               name != null ? name : "TokyoNight",
               LoaderTheme.Palette.DEFAULT.bg,
               LoaderTheme.Palette.DEFAULT.surface,
               LoaderTheme.Palette.DEFAULT.border,
               LoaderTheme.Palette.DEFAULT.text,
               LoaderTheme.Palette.DEFAULT.muted,
               LoaderTheme.Palette.DEFAULT.accent,
               LoaderTheme.Palette.DEFAULT.accent2,
               false,
               true
            );
         }

         if (Boolean.FALSE.equals(JsonLite.bool(theme, "enabled"))) {
            return LoaderTheme.Palette.DEFAULT;
         }

         int accent = hex(JsonLite.str(theme, "accent"), LoaderTheme.Palette.DEFAULT.accent);
         int accent2 = hex(JsonLite.str(theme, "accent2"), LoaderTheme.Palette.DEFAULT.accent2);
         int surface = hex(JsonLite.str(theme, "surface"), LoaderTheme.Palette.DEFAULT.surface);
         int border = hex(JsonLite.str(theme, "border"), LoaderTheme.Palette.DEFAULT.border);
         boolean light = luma(surface) > 140;
         int derivedBg = light ? mix(surface, 16777215, 0.45F) : mix(surface, 0, 0.45F);
         int bg = theme.get("background") instanceof String s ? hex(s, derivedBg) : derivedBg;
         int text = hex(JsonLite.str(theme, "text"), light ? 1447965 : 15592944);
         int muted = hex(JsonLite.str(theme, "muted"), light ? 6120048 : 9145235);
         boolean chroma = Boolean.TRUE.equals(JsonLite.bool(theme, "chroma")) || "Chroma".equals(name);
         if (name == null) {
            name = chroma ? "Chroma" : "Custom";
         }

         return new LoaderTheme.Palette(name, bg, surface, border, text, muted, accent, accent2, chroma, true);
      } catch (Exception e) {
         return LoaderTheme.Palette.DEFAULT;
      }
   }

   static int hex(String s, int fallback) {
      if (s == null) {
         return fallback;
      }

      try {
         String t = s.trim();
         if (t.startsWith("#")) {
            t = t.substring(1);
         }

         if (t.length() == 8) {
            t = t.substring(2);
         }

         if (t.length() == 3) {
            t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
         }

         return Integer.parseInt(t, 16) & 16777215;
      } catch (Exception e) {
         return fallback;
      }
   }

   static int luma(int rgb) {
      return (int)(0.299 * (rgb >> 16 & 0xFF) + 0.587 * (rgb >> 8 & 0xFF) + 0.114 * (rgb & 0xFF));
   }

   static int mix(int a, int b, float t) {
      int r = Math.round((a >> 16 & 0xFF) * (1.0F - t) + (b >> 16 & 0xFF) * t);
      int g = Math.round((a >> 8 & 0xFF) * (1.0F - t) + (b >> 8 & 0xFF) * t);
      int bl = Math.round((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
      return r << 16 | g << 8 | bl;
   }

   record Palette(String name, int bg, int surface, int border, int text, int muted, int accent, int accent2, boolean chroma, boolean fromConfig) {
      static final LoaderTheme.Palette DEFAULT = new LoaderTheme.Palette("TokyoNight", 724243, 1448742, 2765122, 15592944, 9145235, 8037111, 4020641, false, false);
   }
}
