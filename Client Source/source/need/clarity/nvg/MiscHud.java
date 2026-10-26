package need.clarity.nvg;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import need.clarity.modules.visual.BpsCounter;
import need.clarity.modules.visual.ClarityIcon;
import need.clarity.util.Chroma;
import need.clarity.util.ThemeColors;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public final class MiscHud {
   public static final String BPS = "BPS";
   public static final String ICON = "Clarity Icon";
   private static int logoImg = 0;
   private static int glowImg = 0;
   private static long imgVg = 0L;

   private MiscHud() {
   }

   public static void drawBps(long vg, float cw, float ch) {
      BpsCounter m = BpsCounter.INSTANCE;
      if (m != null && m.isEnabled()) {
         float sc = m.scale();
         float op = m.opacity();
         int accent = ThemeColors.getAccentRgb();
         int text = m.themeColors() ? ThemeColors.getTextRgb() : ThemeColors.parseHexRgb(m.textColor(), 15592944);
         int line = m.themeColors() ? accent : ThemeColors.parseHexRgb(m.graphColor(), 8037111);
         int muted = ThemeColors.getMutedRgb();
         int surface = ThemeColors.getSurfaceRgb();
         float pad = 8.0F * sc;
         float valSz = 13.0F * sc;
         float labSz = 9.5F * sc;
         String val = String.format(Locale.ROOT, "%." + m.decimals() + "f", m.bps());
         String unit = " b/s";
         String lab = m.label() ? m.labelText() : "";
         float labW = lab.isEmpty() ? 0.0F : ClarityDraw.textWidth(vg, "inter-med", labSz, 0.3F, lab) + 6.0F * sc;
         float valW = ClarityDraw.textWidth(vg, "inter-sb", valSz, 0.0F, val);
         float unitW = ClarityDraw.textWidth(vg, "inter-med", labSz, 0.0F, unit);
         String peak = m.graph() && m.showPeak() ? String.format(Locale.ROOT, "max %.1f", m.peak()) : "";
         float peakW = peak.isEmpty() ? 0.0F : ClarityDraw.textWidth(vg, "inter-med", labSz * 0.9F, 0.0F, peak) + 8.0F * sc;
         float rowH = valSz * 1.3F;
         float gW = m.graph() ? m.graphWidth() * sc : 0.0F;
         float gH = m.graph() ? m.graphHeight() * sc : 0.0F;
         float w = Math.max(labW + valW + unitW + peakW, gW) + pad * 2.0F;
         float h = pad * 2.0F + rowH + (m.graph() ? gH + 5.0F * sc : 0.0F);
         float mx = m.offsetX();
         float my = m.offsetY();
         float x;
         float y;
         switch (m.anchor()) {
            case "Top Right":
               x = cw - w - mx;
               y = my;
               break;
            case "Bottom Left":
               x = mx;
               y = ch - h - my;
               break;
            case "Bottom Right":
               x = cw - w - mx;
               y = ch - h - my;
               break;
            default:
               x = mx;
               y = my;
         }

         x = Math.round(x);
         y = Math.round(y);
         HudLayout.report("BPS", x, y, w, h);
         float cr = m.cornerRadius() * sc;
         if (!LiquidGlass.panel(vg, "BPS", x, y, w, h, cr, op) && m.background()) {
            boolean blurred = m.blur() && HudElements.fillBlurRRect(vg, x, y, w, h, cr, op);
            ClarityDraw.fillRRect(vg, x, y, w, h, cr, argb(surface, (blurred ? m.blurTint() : m.bgOpacity()) * op));
            HudFx.panel(vg, "BPS", x, y, w, h, cr, op);
            if (m.border()) {
               ClarityDraw.strokeRRect(vg, x, y, w, h, cr, 1.0F, argb(mix(ThemeColors.getBorderRgb(), accent, 0.1F), op));
            }
         }

         float ty = y + pad + rowH * 0.5F;
         float tx = x + pad;
         if (!lab.isEmpty()) {
            ClarityDraw.text(vg, "inter-med", labSz, 0.3F, argb(line, op), 17, tx, ty, lab);
            tx += labW;
         }

         ClarityDraw.text(vg, "inter-sb", valSz, 0.0F, argb(text, op), 17, tx, ty, val);
         ClarityDraw.text(vg, "inter-med", labSz, 0.0F, argb(muted, op), 17, tx + valW, ty + 0.6F * sc, unit);
         if (!peak.isEmpty()) {
            ClarityDraw.text(vg, "inter-med", labSz * 0.9F, 0.0F, argb(muted, 0.85F * op), 20, x + w - pad, ty + 0.6F * sc, peak);
         }

         if (m.graph()) {
            drawGraph(vg, m, x + pad, y + pad + rowH + 5.0F * sc, w - pad * 2.0F, gH, line, op, sc);
         }
      }
   }

   private static void drawGraph(long vg, BpsCounter m, float gx, float gy, float gw, float gh, int col, float op, float sc) {
      int len = Math.min(m.graphLength(), 200);
      float top = Math.max(4.317F, m.peak() * 1.15F);
      ClarityDraw.fillRRect(vg, gx, gy + gh - 0.5F, gw, 1.0F, 0.0F, argb(col, 0.18F * op));
      float sprintY = gy + gh - gh * (5.612F / top);
      if (sprintY > gy + 2.0F) {
         ClarityDraw.fillRRect(vg, gx, sprintY, gw, 0.6F * sc, 0.0F, argb(col, 0.08F * op));
      }

      int n = Math.min(len, Math.max(2, m.samples()));
      if (n >= 2) {
         float step = gw / (len - 1);
         NanoVG.nvgSave(vg);
         NanoVG.nvgIntersectScissor(vg, gx, gy - 1.0F, gw, gh + 2.0F);

         try (MemoryStack s = MemoryStack.stackPush()) {
            NVGColor c1 = NVGColor.malloc(s);
            NVGColor c2 = NVGColor.malloc(s);
            if (m.graphFill()) {
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgMoveTo(vg, gx + gw, gy + gh);

               for (int i = 0; i < n; i++) {
                  NanoVG.nvgLineTo(vg, gx + gw - i * step, gy + gh - gh * Math.min(1.0F, m.sample(i) / top));
               }

               NanoVG.nvgLineTo(vg, gx + gw - (n - 1) * step, gy + gh);
               NanoVG.nvgClosePath(vg);
               NVGPaint p = NVGPaint.malloc(s);
               NanoVG.nvgLinearGradient(vg, gx, gy, gx, gy + gh, ClarityNvg.argb(argb(col, 0.35F * op), c1), ClarityNvg.argb(argb(col, 0.0F), c2), p);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
            }

            NanoVG.nvgBeginPath(vg);

            for (int i = 0; i < n; i++) {
               float px = gx + gw - i * step;
               float py = gy + gh - gh * Math.min(1.0F, m.sample(i) / top);
               if (i == 0) {
                  NanoVG.nvgMoveTo(vg, px, py);
               } else {
                  NanoVG.nvgLineTo(vg, px, py);
               }
            }

            NanoVG.nvgStrokeColor(vg, ClarityNvg.argb(argb(col, op), c1));
            NanoVG.nvgStrokeWidth(vg, Math.max(1.0F, 1.4F * sc));
            NanoVG.nvgLineJoin(vg, 1);
            NanoVG.nvgLineCap(vg, 1);
            NanoVG.nvgStroke(vg);
            float ny = gy + gh - gh * Math.min(1.0F, m.sample(0) / top);
            ClarityDraw.fillRRect(vg, gx + gw - 2.0F * sc, ny - 2.0F * sc, 4.0F * sc, 4.0F * sc, 2.0F * sc, argb(col, op));
         }

         NanoVG.nvgRestore(vg);
      }
   }

   public static void onContextDestroyed() {
      glowImg = 0;
      logoImg = 0;
      imgVg = 0L;
   }

   private static int load(long vg, String path) {
      try (InputStream in = MiscHud.class.getResourceAsStream(path)) {
         if (in == null) {
            return 0;
         }

         byte[] bytes = in.readAllBytes();
         ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
         buf.put(bytes).flip();
         return NanoVG.nvgCreateImageMem(vg, 1, buf);
      } catch (Throwable t) {
         return 0;
      }
   }

   public static void drawIcon(long vg, float cw, float ch) {
      ClarityIcon m = ClarityIcon.INSTANCE;
      if (m != null && m.isEnabled()) {
         if (imgVg != vg) {
            glowImg = 0;
            logoImg = 0;
            imgVg = vg;
         }

         if (logoImg == 0) {
            logoImg = load(vg, "/assets/clarity/textures/fx/clarity_logo_clean.png");
         }

         if (glowImg == 0) {
            glowImg = load(vg, "/assets/clarity/textures/fx/clarity_logo_glow.png");
         }

         if (logoImg > 0) {
            float s = m.size();
            float op = m.opacity();
            boolean bg = m.background();
            float pad = bg ? m.padding() : 0.0F;
            float box = s + pad * 2.0F;
            float mx = m.offsetX();
            float my = m.offsetY();
            float bx;
            float by;
            switch (m.anchor()) {
               case "Top Right":
                  bx = cw - box - mx;
                  by = my;
                  break;
               case "Bottom Left":
                  bx = mx;
                  by = ch - box - my;
                  break;
               case "Bottom Right":
                  bx = cw - box - mx;
                  by = ch - box - my;
                  break;
               default:
                  bx = mx;
                  by = my;
            }

            bx = Math.round(bx);
            by = Math.round(by);
            HudLayout.report("Clarity Icon", bx, by, box, box);
            if (bg) {
               float cr = m.circle() ? box / 2.0F : Math.min(box / 2.0F, m.cornerRadius());
               int accent = ThemeColors.getAccentRgb();
               int surface = m.themeBg() ? ThemeColors.getSurfaceRgb() : ThemeColors.parseHexRgb(m.bgColor(), 1448742);
               if (!LiquidGlass.panel(vg, "Clarity Icon", bx, by, box, box, cr, op)) {
                  boolean blurred = m.blur() && HudElements.fillBlurRRect(vg, bx, by, box, box, cr, op);
                  ClarityDraw.fillRRect(vg, bx, by, box, box, cr, argb(surface, (blurred ? m.blurTint() : m.bgOpacity()) * op));
                  HudFx.panel(vg, "Clarity Icon", bx, by, box, box, cr, op);
                  if (m.border()) {
                     ClarityDraw.strokeRRect(vg, bx, by, box, box, cr, 1.0F, argb(mix(ThemeColors.getBorderRgb(), accent, 0.1F), op));
                  }
               }
            }

            float x = bx + pad;
            float y = by + pad;
            double t = System.nanoTime() / 1.0E9;
            int col = colour(m.colorMode(), m.customColor(), 16777215, t, 0.0F);
            int gcol = colour(m.glowColorMode(), m.glowCustom(), ThemeColors.getAccentRgb(), t, 0.05F);
            float glow = m.glow();
            if (m.pulse()) {
               glow *= 0.65F + 0.35F * (float)Math.sin(t * m.pulseSpeed() * Math.PI);
            }

            float rot = (float)Math.toRadians(m.rotation() + (m.spin() ? t * m.spinSpeed() % 360.0 : 0.0));
            float cx = x + s / 2.0F;
            float cy = y + s / 2.0F;
            NanoVG.nvgSave(vg);
            NanoVG.nvgTranslate(vg, cx, cy);
            if (rot != 0.0F) {
               NanoVG.nvgRotate(vg, rot);
            }

            try (MemoryStack st = MemoryStack.stackPush()) {
               NVGPaint p = NVGPaint.malloc(st);
               NVGColor c = NVGColor.malloc(st);
               if (glow > 0.01F && glowImg > 0) {
                  float gs = s * 2.0F * m.glowSize();
                  int passes = (int)Math.ceil(glow);

                  for (int i = 0; i < passes; i++) {
                     float a = Math.min(1.0F, glow - i) * op;
                     if (a <= 0.01F) {
                        break;
                     }

                     NanoVG.nvgImagePattern(vg, -gs / 2.0F, -gs / 2.0F, gs, gs, 0.0F, glowImg, 1.0F, p);
                     p.innerColor(ClarityNvg.argb(argb(gcol, a), c));
                     p.outerColor(ClarityNvg.argb(argb(gcol, a), c));
                     NanoVG.nvgBeginPath(vg);
                     NanoVG.nvgRect(vg, -gs / 2.0F, -gs / 2.0F, gs, gs);
                     NanoVG.nvgFillPaint(vg, p);
                     NanoVG.nvgFill(vg);
                  }
               }

               NanoVG.nvgImagePattern(vg, -s / 2.0F, -s / 2.0F, s, s, 0.0F, logoImg, 1.0F, p);
               p.innerColor(ClarityNvg.argb(argb(col, op), c));
               p.outerColor(ClarityNvg.argb(argb(col, op), c));
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgRect(vg, -s / 2.0F, -s / 2.0F, s, s);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
            }

            NanoVG.nvgRestore(vg);
         }
      }
   }

   private static int colour(String mode, Object custom, int white, double t, float hueOffset) {
      return switch (mode) {
         case "Theme" -> ThemeColors.getAccentRgb();
         case "Theme 2" -> ThemeColors.getAccent2Rgb();
         case "White" -> 16777215;
         case "Rainbow" -> Chroma.hsv((float)((t / 6.0 + hueOffset) % 1.0), 0.6F, 1.0F);
         case "Custom" -> ThemeColors.parseHexRgb(custom, white);
         default -> white;
      };
   }

   private static int argb(int rgb, float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int a, int b, float t) {
      int r = (int)((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = (int)((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = (int)((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }
}
