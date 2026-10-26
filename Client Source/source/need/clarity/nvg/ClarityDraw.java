package need.clarity.nvg;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public final class ClarityDraw {
   public static final float[] BOUNDS = new float[4];

   private ClarityDraw() {
   }

   public static void fillRRect(long vg, float x, float y, float w, float h, float r, int argb) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         NanoVG.nvgBeginPath(vg);
         NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
         NanoVG.nvgFillColor(vg, ClarityNvg.argb(argb, c));
         NanoVG.nvgFill(vg);
      }
   }

   public static void fillRRect(long vg, float x, float y, float w, float h, float r, NVGColor col) {
      NanoVG.nvgBeginPath(vg);
      NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
      NanoVG.nvgFillColor(vg, col);
      NanoVG.nvgFill(vg);
   }

   public static void fillRRectVarying(long vg, float x, float y, float w, float h, float tl, float tr, float br, float bl, int argb) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         NanoVG.nvgBeginPath(vg);
         NanoVG.nvgRoundedRectVarying(vg, x, y, w, h, tl, tr, br, bl);
         NanoVG.nvgFillColor(vg, ClarityNvg.argb(argb, c));
         NanoVG.nvgFill(vg);
      }
   }

   public static void fillRRectVaryingLinear(long vg, float x, float y, float w, float h, float tl, float tr, float br, float bl, float ax, float ay, float bx, float by, int argb0, int argb1) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c0 = NVGColor.malloc(s);
         NVGColor c1 = NVGColor.malloc(s);
         NVGPaint p = NVGPaint.malloc(s);
         NanoVG.nvgBeginPath(vg);
         NanoVG.nvgRoundedRectVarying(vg, x, y, w, h, tl, tr, br, bl);
         NanoVG.nvgLinearGradient(vg, ax, ay, bx, by, ClarityNvg.argb(argb0, c0), ClarityNvg.argb(argb1, c1), p);
         NanoVG.nvgFillPaint(vg, p);
         NanoVG.nvgFill(vg);
      }
   }

   public static void fillRRectLinear(long vg, float x, float y, float w, float h, float r, float ax, float ay, float bx, float by, int argb0, int argb1) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c0 = NVGColor.malloc(s);
         NVGColor c1 = NVGColor.malloc(s);
         NVGPaint p = NVGPaint.malloc(s);
         NanoVG.nvgBeginPath(vg);
         NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
         NanoVG.nvgLinearGradient(vg, ax, ay, bx, by, ClarityNvg.argb(argb0, c0), ClarityNvg.argb(argb1, c1), p);
         NanoVG.nvgFillPaint(vg, p);
         NanoVG.nvgFill(vg);
      }
   }

   public static void strokeRRect(long vg, float x, float y, float w, float h, float r, float width, int argb) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         float i = width * 0.5F;
         NanoVG.nvgBeginPath(vg);
         NanoVG.nvgRoundedRect(vg, x + i, y + i, w - width, h - width, Math.max(0.0F, r - i));
         NanoVG.nvgStrokeColor(vg, ClarityNvg.argb(argb, c));
         NanoVG.nvgStrokeWidth(vg, width);
         NanoVG.nvgStroke(vg);
      }
   }

   public static void edgeHighlight(long vg, float x, float y, float w, float h, boolean rightSide, float alpha) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         NanoVG.nvgBeginPath(vg);
         if (rightSide) {
            NanoVG.nvgMoveTo(vg, x + 0.75F, y + 1.0F);
            NanoVG.nvgLineTo(vg, x + 0.75F, y + h - 1.0F);
         } else {
            NanoVG.nvgMoveTo(vg, x + w - 0.75F, y + 1.0F);
            NanoVG.nvgLineTo(vg, x + w - 0.75F, y + h - 1.0F);
         }

         NanoVG.nvgStrokeColor(vg, ClarityNvg.white(alpha, c));
         NanoVG.nvgStrokeWidth(vg, 1.0F);
         NanoVG.nvgStroke(vg);
      }
   }

   public static void textOutlined(long vg, String face, float size, float spacing, int align, float x, float y, String str, int fillArgb, int outlineArgb, float outlineWidth) {
      if (outlineWidth <= 0.0F) {
         text(vg, face, size, spacing, fillArgb, align, x, y, str);
      } else {
         try (MemoryStack s = MemoryStack.stackPush()) {
            NVGColor oc = NVGColor.malloc(s);
            NanoVG.nvgFontFace(vg, face);
            NanoVG.nvgFontSize(vg, size);
            NanoVG.nvgTextLetterSpacing(vg, spacing);
            NanoVG.nvgTextAlign(vg, align);
            NanoVG.nvgFillColor(vg, ClarityNvg.argb(outlineArgb, oc));

            for (int dx = -1; dx <= 1; dx++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (dx != 0 || dy != 0) {
                     NanoVG.nvgText(vg, x + dx * outlineWidth, y + dy * outlineWidth, str);
                  }
               }
            }
         }

         text(vg, face, size, spacing, fillArgb, align, x, y, str);
      }
   }

   public static void panel(long vg, float x, float y, float w, float h, float r, int topArgb, int bottomArgb, int borderArgb, float borderWidth) {
      fillRRectLinear(vg, x, y, w, h, r, x, y, x, y + h, topArgb, bottomArgb);
      if (borderWidth > 0.0F) {
         strokeRRect(vg, x, y, w, h, r, borderWidth, borderArgb);
      }
   }

   public static void topHighlight(long vg, float x, float y, float w, float h, float r, int argb) {
      float inset = Math.max(2.0F, r * 0.7F);
      fillRRect(vg, x + inset, y + 1.0F, Math.max(1.0F, w - inset * 2.0F), 1.0F, 0.5F, argb);
   }

   public static void bottomShadow(long vg, float x, float y, float w, float h, float r, int argb) {
      float inset = Math.max(2.0F, r * 0.7F);
      fillRRect(vg, x + inset, y + h - 2.0F, Math.max(1.0F, w - inset * 2.0F), 1.0F, 0.5F, argb);
   }

   public static float text(long vg, String face, float size, float spacing, NVGColor col, int align, float x, float y, String str) {
      NanoVG.nvgFontFace(vg, face);
      NanoVG.nvgFontSize(vg, size);
      NanoVG.nvgTextLetterSpacing(vg, spacing);
      NanoVG.nvgTextAlign(vg, align);
      NanoVG.nvgFillColor(vg, col);
      return draw(vg, str, x, y, align, size, col.a());
   }

   public static float text(long vg, String face, float size, float spacing, int argb, int align, float x, float y, String str) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         NanoVG.nvgFontFace(vg, face);
         NanoVG.nvgFontSize(vg, size);
         NanoVG.nvgTextLetterSpacing(vg, spacing);
         NanoVG.nvgTextAlign(vg, align);
         NanoVG.nvgFillColor(vg, ClarityNvg.argb(argb, c));
         return draw(vg, str, x, y, align, size, (argb >>> 24 & 0xFF) / 255.0F);
      }
   }

   public static float text(long vg, String face, float size, float spacing, int argb, int align, float x, float y, String str, float emojiAlpha) {
      try (MemoryStack s = MemoryStack.stackPush()) {
         NVGColor c = NVGColor.malloc(s);
         NanoVG.nvgFontFace(vg, face);
         NanoVG.nvgFontSize(vg, size);
         NanoVG.nvgTextLetterSpacing(vg, spacing);
         NanoVG.nvgTextAlign(vg, align);
         NanoVG.nvgFillColor(vg, ClarityNvg.argb(argb, c));
         return draw(vg, str, x, y, align, size, emojiAlpha);
      }
   }

   private static float draw(long vg, String str, float x, float y, int align, float size, float emojiAlpha) {
      Object p = RichText.prepare(str);
      if (p instanceof RichText.Layout l) {
         try {
            return RichText.draw(vg, l, x, y, align, size, emojiAlpha);
         } catch (Throwable t) {
            RichText.emojiFailed(t);
            NanoVG.nvgTextAlign(vg, align);
            p = RichText.prepare(str);
         }
      }

      return NanoVG.nvgText(vg, x, y, p instanceof String s ? s : str);
   }

   public static float textWidth(long vg, String face, float size, float spacing, String str) {
      NanoVG.nvgFontFace(vg, face);
      NanoVG.nvgFontSize(vg, size);
      NanoVG.nvgTextLetterSpacing(vg, spacing);
      NanoVG.nvgTextAlign(vg, 9);
      Object p = RichText.prepare(str);
      if (p instanceof RichText.Layout l) {
         try {
            float w = RichText.width(vg, l, size);
            BOUNDS[0] = 0.0F;
            BOUNDS[1] = 0.0F;
            BOUNDS[2] = w;
            BOUNDS[3] = size * 1.2F;
            return w;
         } catch (Throwable t) {
            RichText.emojiFailed(t);
            p = RichText.prepare(str);
         }
      }

      return NanoVG.nvgTextBounds(vg, 0.0F, 0.0F, p instanceof String s ? s : str, BOUNDS);
   }

   public static void hudText(long vg, float size, int argb, int align, float x, float y, String str) {
      text(vg, "inter-sb", size, 0.0F, argb, align, x, y, str);
   }
}
