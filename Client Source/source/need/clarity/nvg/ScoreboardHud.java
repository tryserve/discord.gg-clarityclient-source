package need.clarity.nvg;

import java.util.ArrayList;
import java.util.List;
import need.clarity.modules.visual.CustomScoreboard;
import need.clarity.util.ThemeColors;
import org.lwjgl.nanovg.NanoVG;

public final class ScoreboardHud {
   public static final String ID = "Custom Scoreboard";
   private static float shownH = -1.0F;
   private static float shownW = -1.0F;
   private static float alpha = 0.0F;
   private static long lastNanos = 0L;
   private static float targetH = 0.0F;
   private static CustomScoreboard.Snapshot last;

   private ScoreboardHud() {
   }

   public static boolean wanted() {
      return CustomScoreboard.active() && (CustomScoreboard.current() != null || alpha > 0.003F);
   }

   public static boolean animating() {
      return !CustomScoreboard.active() ? false : alpha > 0.003F && alpha < 0.997F || CustomScoreboard.current() != null && Math.abs(shownH - targetH) > 0.3F;
   }

   public static int blurStrength() {
      return CustomScoreboard.active() && CustomScoreboard.bool("Blur", false) && !(alpha <= 0.01F) ? Math.round(CustomScoreboard.num("Blur Strength", 2.0F)) : 0;
   }

   public static void draw(long vg, float cw, float ch) {
      if (!CustomScoreboard.active()) {
         alpha = 0.0F;
      } else {
         CustomScoreboard.Snapshot s = CustomScoreboard.current();
         long now = System.nanoTime();
         float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
         lastNanos = now;
         float k = 1.0F - (float)Math.exp(-dt * 12.0F);
         alpha = alpha + ((s != null ? 1.0F : 0.0F) - alpha) * (CustomScoreboard.bool("Animate", true) ? k : 1.0F);
         if (s != null) {
            last = s;
         }

         s = last;
         if (s != null && !(alpha < 0.003F)) {
            String[] faces = McText.faces(CustomScoreboard.str("Font", "Inter"));
            float size = CustomScoreboard.num("Text Size", 9.0F);
            float pad = CustomScoreboard.num("Padding", 7.0F);
            float lineH = size * (1.25F + CustomScoreboard.num("Line Spacing", 1.0F) * 0.12F);
            boolean numbers = !CustomScoreboard.bool("Hide Numbers", true);
            boolean shadow = CustomScoreboard.bool("Text Shadow", false);
            String accentStyle = CustomScoreboard.str("Accent", "Line");
            float radius = CustomScoreboard.num("Corner Radius", 7.0F);
            float op = Math.max(0.0F, Math.min(1.0F, CustomScoreboard.num("Opacity", 0.55F)));
            int accent = ThemeColors.getAccentRgb();
            int surface = ThemeColors.getSurfaceRgb();
            float titleSize = size * 1.08F;
            List<McText.Run> title = McText.runs(s.title(), 16777215);
            float titleW = McText.width(vg, new String[]{faces[2], faces[2], faces[2]}, titleSize, title);
            List<List<McText.Run>> names = new ArrayList<>();
            List<List<McText.Run>> scores = new ArrayList<>();
            float contentW = titleW;

            for (CustomScoreboard.Row r : s.rows()) {
               List<McText.Run> n = McText.runs(r.name(), 16777215);
               List<McText.Run> sc = numbers ? McText.runs(r.score(), 16733525) : List.of();
               names.add(n);
               scores.add(sc);
               float w = McText.width(vg, faces, size, n) + (numbers ? McText.width(vg, faces, size, sc) + 10.0F : 0.0F);
               contentW = Math.max(contentW, w);
            }

            float titleH = titleSize * 1.5F + pad * 0.6F;
            float w = contentW + pad * 2.0F;
            float h = titleH + s.rows().size() * lineH + pad * 0.9F;
            targetH = h;
            if (!(shownH < 0.0F) && CustomScoreboard.bool("Animate", true)) {
               shownH = shownH + (h - shownH) * k;
               shownW = shownW + (w - shownW) * k;
            } else {
               shownH = h;
               shownW = w;
            }

            float W = shownW;
            float H = shownH;
            boolean leftSide = "Left".equals(CustomScoreboard.str("Side", "Right"));
            float xo = CustomScoreboard.num("X Offset", 6.0F);
            float yo = CustomScoreboard.num("Y Offset", 0.0F);
            float x = Math.round(leftSide ? xo : cw - W - xo);
            float y = Math.round(ch / 2.0F - H / 2.0F + yo);
            HudLayout.report("Custom Scoreboard", x, y, W, H);
            float a = alpha;
            NanoVG.nvgSave(vg);
            NanoVG.nvgIntersectScissor(vg, x - 2.0F, y - 2.0F, W + 4.0F, H + 4.0F);
            if (!LiquidGlass.panel(vg, "Custom Scoreboard", x, y, W, H, radius, a)) {
               boolean blurred = CustomScoreboard.bool("Blur", false) && HudElements.fillBlurRRect(vg, x, y, W, H, radius, a);
               if (!"None".equals(CustomScoreboard.str("Background", "Panel")) || blurred) {
                  ClarityDraw.fillRRect(vg, x, y, W, H, radius, tintA(surface, (blurred ? CustomScoreboard.num("Blur Tint", 0.35F) : op) * a));
               }

               HudFx.panel(vg, "Custom Scoreboard", x, y, W, H, radius, a);
            }

            if (CustomScoreboard.bool("Border", true)) {
               ClarityDraw.strokeRRect(vg, x, y, W, H, radius, 1.0F, tintA(mix(ThemeColors.getBorderRgb(), accent, 0.15F), 0.85F * a));
            }

            if ("Bar".equals(accentStyle)) {
               NanoVG.nvgSave(vg);
               NanoVG.nvgIntersectScissor(vg, x, y, W, 2.5F);
               ClarityDraw.fillRRect(vg, x, y, W, radius * 2.0F + 3.0F, radius, tintA(accent, a));
               NanoVG.nvgRestore(vg);
            }

            boolean center = CustomScoreboard.bool("Center Title", true);
            float tx = center ? x + W / 2.0F - titleW / 2.0F : x + pad;
            McText.draw(vg, new String[]{faces[2], faces[2], faces[2]}, titleSize, title, tx, y + pad * 0.6F + titleSize * 0.75F, a, shadow, null);
            if ("Line".equals(accentStyle)) {
               ClarityDraw.fillRRect(vg, x + pad, y + titleH - 2.0F, W - pad * 2.0F, 1.0F, 0.5F, tintA(accent, 0.75F * a));
            }

            float ry = y + titleH + lineH / 2.0F;

            for (int i = 0; i < names.size() && !(ry + lineH / 2.0F > y + H); i++) {
               McText.draw(vg, faces, size, names.get(i), x + pad, ry, a, shadow, null);
               if (numbers) {
                  float sw = McText.width(vg, faces, size, scores.get(i));
                  McText.draw(vg, faces, size, scores.get(i), x + W - pad - sw, ry, a, shadow, null);
               }

               ry += lineH;
            }

            NanoVG.nvgRestore(vg);
         }
      }
   }

   private static int tintA(int rgb, float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }
}
