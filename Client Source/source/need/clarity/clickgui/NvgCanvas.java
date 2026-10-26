package need.clarity.clickgui;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import need.clarity.nvg.ClarityFonts;
import need.clarity.nvg.RichText;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;

public final class NvgCanvas implements Canvas {
   private final long vg;
   private final NVGColor c1 = NVGColor.create();
   private final NVGColor c2 = NVGColor.create();
   private final NVGPaint paint = NVGPaint.create();
   private final Deque<Float> alphaStack = new ArrayDeque<>();
   private final float[] xf = new float[6];
   private float alpha = 1.0F;
   private int fbW;
   private int fbH;
   private int glassImg = 0;
   private int glassTex = 0;
   private int glassW = 0;
   private int glassH = 0;
   public Predicate<String> iconSource;
   private final List<NvgCanvas.Icon> icons = new ArrayList<>();
   private boolean crisp = true;
   private String famKey = "inter";
   private int snapImg = 0;
   private int snapTex = 0;
   private int snapW = 0;
   private int snapH = 0;
   private int glassSrcTex;
   private int glassSrcW;
   private int glassSrcH;
   private final Map<String, Integer> images = new HashMap<>();
   private final Map<String, int[]> byteImages = new HashMap<>();
   private final float[] bounds = new float[4];

   public NvgCanvas(long vg) {
      this.vg = vg;
   }

   public void begin(int fbW, int fbH) {
      this.fbW = fbW;
      this.fbH = fbH;
      this.alpha = 1.0F;
      this.alphaStack.clear();
      this.icons.clear();
   }

   public List<NvgCanvas.Icon> icons() {
      return this.icons;
   }

   public void recordStack(Object stack, String id, float x, float y, float size, int group) {
      NanoVG.nvgCurrentTransform(this.vg, this.xf);
      float sx = this.xf[0];
      float sy = this.xf[3];
      float tx = this.xf[4];
      float ty = this.xf[5];
      this.icons.add(new NvgCanvas.Icon(id, stack, sx * x + tx, sy * y + ty, size * sx, -100000.0F, -100000.0F, 100000.0F, 100000.0F, this.alpha, group));
   }

   public float[] toPixels(float x, float y, float w, float h) {
      NanoVG.nvgCurrentTransform(this.vg, this.xf);
      return new float[]{this.xf[0] * x + this.xf[4], this.xf[3] * y + this.xf[5], this.xf[0] * w, this.xf[3] * h};
   }

   @Override
   public boolean itemIcon(String id, float x, float y, float size, float clipX, float clipY, float clipW, float clipH) {
      Predicate<String> src = this.iconSource;
      if (src != null && src.test(id)) {
         NanoVG.nvgCurrentTransform(this.vg, this.xf);
         float sx = this.xf[0];
         float sy = this.xf[3];
         float tx = this.xf[4];
         float ty = this.xf[5];
         float px = sx * x + tx;
         float py = sy * y + ty;
         float cx0 = Math.max(-100000.0F, sx * clipX + tx);
         float cy0 = Math.max(-100000.0F, sy * clipY + ty);
         float cx1 = Math.min(100000.0F, sx * (clipX + clipW) + tx);
         float cy1 = Math.min(100000.0F, sy * (clipY + clipH) + ty);
         this.icons.add(new NvgCanvas.Icon(id, null, px, py, size * sx, cx0, cy0, cx1, cy1, this.alpha, -1));
         return true;
      } else {
         return false;
      }
   }

   public void setCrisp(boolean on) {
      this.crisp = on;
   }

   @Override
   public void setFontFamily(String family) {
      this.famKey = ClarityFonts.key(family);
   }

   private String face(String font) {
      if ("inter".equals(this.famKey)) {
         return font;
      } else if ("inter-med".equals(font)) {
         return this.famKey + "-med";
      } else if ("inter-sb".equals(font)) {
         return this.famKey + "-sb";
      } else {
         return "inter".equals(font) ? this.famKey : font;
      }
   }

   void setSnapshot(int tex, int w, int h) {
      if (this.snapImg == 0 || this.snapTex != tex || this.snapW != w || this.snapH != h) {
         if (this.snapImg != 0) {
            NanoVG.nvgDeleteImage(this.vg, this.snapImg);
         }

         this.snapImg = tex == 0 ? 0 : NanoVGGL3.nvglCreateImageFromHandle(this.vg, tex, w, h, 65560);
         this.snapTex = tex;
         this.snapW = w;
         this.snapH = h;
      }
   }

   void forgetSnapshot() {
      this.snapImg = 0;
      this.snapTex = 0;
   }

   @Override
   public boolean hasSnapshot() {
      return this.snapImg != 0;
   }

   @Override
   public void snapPiece(float[] xy, float sw, float sh, float a) {
      if (this.snapImg != 0 && xy.length >= 6) {
         NanoVG.nvgImagePattern(this.vg, 0.0F, 0.0F, sw, sh, 0.0F, this.snapImg, Math.max(0.0F, Math.min(1.0F, a * this.alpha)), this.paint);
         this.polyPath(xy);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
      }
   }

   @Override
   public void snapHole(float cx, float cy, float r, float sw, float sh, float a) {
      if (this.snapImg != 0) {
         NanoVG.nvgImagePattern(this.vg, 0.0F, 0.0F, sw, sh, 0.0F, this.snapImg, Math.max(0.0F, Math.min(1.0F, a * this.alpha)), this.paint);
         NanoVG.nvgBeginPath(this.vg);
         NanoVG.nvgRect(this.vg, 0.0F, 0.0F, sw, sh);
         NanoVG.nvgCircle(this.vg, cx, cy, r);
         NanoVG.nvgPathWinding(this.vg, 2);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
      }
   }

   @Override
   public void strokePoly(float[] xy, float width, int argb) {
      if (xy.length >= 6) {
         this.polyPath(xy);
         NanoVG.nvgStrokeWidth(this.vg, width);
         NanoVG.nvgStrokeColor(this.vg, this.col(argb, this.c1));
         NanoVG.nvgStroke(this.vg);
      }
   }

   @Override
   public void fillPoly(float[] xy, int argb) {
      if (xy.length >= 6) {
         this.polyPath(xy);
         NanoVG.nvgFillColor(this.vg, this.col(argb, this.c1));
         NanoVG.nvgFill(this.vg);
      }
   }

   private void polyPath(float[] xy) {
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgMoveTo(this.vg, xy[0], xy[1]);

      for (int i = 2; i + 1 < xy.length; i += 2) {
         NanoVG.nvgLineTo(this.vg, xy[i], xy[i + 1]);
      }

      NanoVG.nvgClosePath(this.vg);
   }

   @Override
   public void rotate(float radians) {
      NanoVG.nvgRotate(this.vg, radians);
   }

   @Override
   public void scale2(float sx, float sy) {
      NanoVG.nvgScale(this.vg, sx, sy);
   }

   @Override
   public boolean logo(float x, float y, float size, int tint, float a) {
      Integer id = this.images.get("clarity_logo");
      if (id == null) {
         this.image("clarity_logo", x, y, 0.0F, 0.0F, 0.0F);
         id = this.images.get("clarity_logo");
      }

      if (id != null && id > 0) {
         NanoVG.nvgImagePattern(this.vg, x, y, size, size, 0.0F, id, 1.0F, this.paint);
         this.paint.innerColor(this.col(Theme.a(tint, a), this.c1));
         this.paint.outerColor(this.col(Theme.a(tint, a), this.c2));
         NanoVG.nvgBeginPath(this.vg);
         NanoVG.nvgRect(this.vg, x, y, size, size);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
         return true;
      } else {
         return false;
      }
   }

   private NVGColor col(int argb, NVGColor out) {
      float a = (argb >>> 24 & 0xFF) / 255.0F * this.alpha;
      return NanoVG.nvgRGBAf((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, Math.max(0.0F, Math.min(1.0F, a)), out);
   }

   @Override
   public void save() {
      NanoVG.nvgSave(this.vg);
      this.alphaStack.push(this.alpha);
   }

   @Override
   public void restore() {
      NanoVG.nvgRestore(this.vg);
      this.alpha = this.alphaStack.isEmpty() ? 1.0F : this.alphaStack.pop();
   }

   @Override
   public void translate(float x, float y) {
      NanoVG.nvgTranslate(this.vg, x, y);
   }

   @Override
   public void scale(float s) {
      NanoVG.nvgScale(this.vg, s, s);
   }

   @Override
   public void alpha(float a) {
      this.alpha = this.alpha * Math.max(0.0F, Math.min(1.0F, a));
   }

   @Override
   public void scissor(float x, float y, float w, float h) {
      NanoVG.nvgIntersectScissor(this.vg, x, y, Math.max(0.0F, w), Math.max(0.0F, h));
   }

   private void path(float x, float y, float w, float h, float r) {
      NanoVG.nvgBeginPath(this.vg);
      r = Math.min(r, Math.min(w, h) / 2.0F);
      if (r <= 0.01F) {
         NanoVG.nvgRect(this.vg, x, y, w, h);
      } else {
         NanoVG.nvgRoundedRect(this.vg, x, y, w, h, r);
      }
   }

   @Override
   public void rect(float x, float y, float w, float h, float r, int argb) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         this.path(x, y, w, h, r);
         NanoVG.nvgFillColor(this.vg, this.col(argb, this.c1));
         NanoVG.nvgFill(this.vg);
      }
   }

   @Override
   public void gradient(float x, float y, float w, float h, float r, int a, int b, boolean horizontal) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         NanoVG.nvgLinearGradient(this.vg, x, y, horizontal ? x + w : x, horizontal ? y : y + h, this.col(a, this.c1), this.col(b, this.c2), this.paint);
         this.path(x, y, w, h, r);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
      }
   }

   @Override
   public void stroke(float x, float y, float w, float h, float r, float width, int argb) {
      this.path(x, y, w, h, r);
      NanoVG.nvgStrokeWidth(this.vg, width);
      NanoVG.nvgStrokeColor(this.vg, this.col(argb, this.c1));
      NanoVG.nvgStroke(this.vg);
   }

   @Override
   public void strokeGradient(float x, float y, float w, float h, float r, float width, int a, int b) {
      NanoVG.nvgLinearGradient(this.vg, x, y, x + w, y + h, this.col(a, this.c1), this.col(b, this.c2), this.paint);
      this.path(x, y, w, h, r);
      NanoVG.nvgStrokeWidth(this.vg, width);
      NanoVG.nvgStrokePaint(this.vg, this.paint);
      NanoVG.nvgStroke(this.vg);
   }

   @Override
   public void shadow(float x, float y, float w, float h, float r, float blur, int argb) {
      NanoVG.nvgBoxGradient(this.vg, x, y, w, h, r, blur, this.col(argb, this.c1), this.col(argb & 16777215, this.c2), this.paint);
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgRect(this.vg, x - blur * 1.5F, y - blur * 1.5F, w + blur * 3.0F, h + blur * 3.0F);
      NanoVG.nvgFillPaint(this.vg, this.paint);
      NanoVG.nvgFill(this.vg);
   }

   @Override
   public void glass(float x, float y, float w, float h, float r, float a) {
      int img = this.glassImage();
      if (img != 0) {
         NanoVG.nvgCurrentTransform(this.vg, this.xf);
         float sx = this.xf[0];
         float sy = this.xf[3];
         float tx = this.xf[4];
         float ty = this.xf[5];
         if (!(Math.abs(sx) < 1.0E-6F) && !(Math.abs(sy) < 1.0E-6F)) {
            NanoVG.nvgImagePattern(this.vg, -tx / sx, -ty / sy, this.fbW / sx, this.fbH / sy, 0.0F, img, Math.max(0.0F, Math.min(1.0F, a * this.alpha)), this.paint);
            this.path(x, y, w, h, r);
            NanoVG.nvgFillPaint(this.vg, this.paint);
            NanoVG.nvgFill(this.vg);
         }
      }
   }

   void setGlassSource(int tex, int w, int h) {
      this.glassSrcTex = tex;
      this.glassSrcW = w;
      this.glassSrcH = h;
   }

   private int glassImage() {
      int t = this.glassSrcTex;
      if (t == 0) {
         return 0;
      }

      int w = this.glassSrcW;
      int h = this.glassSrcH;
      if (this.glassImg != 0 && this.glassTex == t && this.glassW == w && this.glassH == h) {
         return this.glassImg;
      }

      if (this.glassImg != 0) {
         NanoVG.nvgDeleteImage(this.vg, this.glassImg);
      }

      this.glassImg = NanoVGGL3.nvglCreateImageFromHandle(this.vg, t, w, h, 65544);
      this.glassTex = t;
      this.glassW = w;
      this.glassH = h;
      return this.glassImg;
   }

   void forgetImages() {
      this.glassImg = 0;
      this.glassTex = 0;
      this.forgetSnapshot();
   }

   @Override
   public void image(String name, float x, float y, float w, float h, float a) {
      Integer id = this.images.get(name);
      if (id == null) {
         id = 0;

         try (InputStream in = NvgCanvas.class.getResourceAsStream("/assets/clarity/textures/fx/" + name + ".png")) {
            if (in != null) {
               byte[] bytes = in.readAllBytes();
               ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
               buf.put(bytes).flip();
               id = NanoVG.nvgCreateImageMem(this.vg, 1, buf);
            }
         } catch (Throwable var13) {
         }

         this.images.put(name, id);
      }

      if (id != null && id > 0 && !(w <= 0.0F) && !(h <= 0.0F)) {
         NanoVG.nvgImagePattern(this.vg, x, y, w, h, 0.0F, id, Math.max(0.0F, Math.min(1.0F, a * this.alpha)), this.paint);
         NanoVG.nvgBeginPath(this.vg);
         NanoVG.nvgRect(this.vg, x, y, w, h);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
      }
   }

   @Override
   public boolean imageBytes(String key, byte[] png, float x, float y, float w, float h, float radius, float a) {
      if (png != null && png.length != 0 && !(w <= 0.0F) && !(h <= 0.0F)) {
         int hash = Arrays.hashCode(png);
         int[] e = this.byteImages.get(key);
         if (e == null || e[1] != hash) {
            if (e != null && e[0] > 0) {
               NanoVG.nvgDeleteImage(this.vg, e[0]);
            }

            int id = 0;

            try {
               ByteBuffer buf = BufferUtils.createByteBuffer(png.length);
               buf.put(png).flip();
               id = NanoVG.nvgCreateImageMem(this.vg, 1, buf);
            } catch (Throwable var14) {
            }

            e = new int[]{id, hash};
            if (this.byteImages.size() > 256) {
               for (int[] old : this.byteImages.values()) {
                  if (old[0] > 0) {
                     NanoVG.nvgDeleteImage(this.vg, old[0]);
                  }
               }

               this.byteImages.clear();
            }

            this.byteImages.put(key, e);
         }

         if (e[0] <= 0) {
            return false;
         }

         NanoVG.nvgImagePattern(this.vg, x, y, w, h, 0.0F, e[0], Math.max(0.0F, Math.min(1.0F, a * this.alpha)), this.paint);
         NanoVG.nvgBeginPath(this.vg);
         NanoVG.nvgRoundedRect(this.vg, x, y, w, h, radius);
         NanoVG.nvgFillPaint(this.vg, this.paint);
         NanoVG.nvgFill(this.vg);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void circle(float cx, float cy, float r, int argb) {
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgCircle(this.vg, cx, cy, r);
      NanoVG.nvgFillColor(this.vg, this.col(argb, this.c1));
      NanoVG.nvgFill(this.vg);
   }

   @Override
   public void ring(float cx, float cy, float r, float width, int argb) {
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgCircle(this.vg, cx, cy, r);
      NanoVG.nvgStrokeWidth(this.vg, width);
      NanoVG.nvgStrokeColor(this.vg, this.col(argb, this.c1));
      NanoVG.nvgStroke(this.vg);
   }

   @Override
   public void arc(float cx, float cy, float r, float a0, float a1, float width, int argb) {
      if (!(width <= 0.05F)) {
         NanoVG.nvgBeginPath(this.vg);
         NanoVG.nvgArc(this.vg, cx, cy, r, a0, a1, 2);
         NanoVG.nvgLineCap(this.vg, 1);
         NanoVG.nvgStrokeWidth(this.vg, width);
         NanoVG.nvgStrokeColor(this.vg, this.col(argb, this.c1));
         NanoVG.nvgStroke(this.vg);
      }
   }

   @Override
   public void line(float x1, float y1, float x2, float y2, float width, int argb) {
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgMoveTo(this.vg, x1, y1);
      NanoVG.nvgLineTo(this.vg, x2, y2);
      NanoVG.nvgLineCap(this.vg, 1);
      NanoVG.nvgStrokeWidth(this.vg, width);
      NanoVG.nvgStrokeColor(this.vg, this.col(argb, this.c1));
      NanoVG.nvgStroke(this.vg);
   }

   @Override
   public void glow(float cx, float cy, float r, int argb) {
      NanoVG.nvgRadialGradient(this.vg, cx, cy, 0.0F, r, this.col(argb, this.c1), this.col(argb & 16777215, this.c2), this.paint);
      NanoVG.nvgBeginPath(this.vg);
      NanoVG.nvgCircle(this.vg, cx, cy, r);
      NanoVG.nvgFillPaint(this.vg, this.paint);
      NanoVG.nvgFill(this.vg);
   }

   @Override
   public float text(String font, float size, float x, float y, int align, int argb, String s) {
      if (s != null && !s.isEmpty()) {
         NanoVG.nvgFontFace(this.vg, this.face(font));
         NanoVG.nvgFontSize(this.vg, size);
         NanoVG.nvgTextAlign(this.vg, align);
         NanoVG.nvgFillColor(this.vg, this.col(argb, this.c1));
         if (this.crisp) {
            NanoVG.nvgCurrentTransform(this.vg, this.xf);
            if (Math.abs(this.xf[1]) < 1.0E-5F && Math.abs(this.xf[2]) < 1.0E-5F && Math.abs(this.xf[0]) > 1.0E-4F && Math.abs(this.xf[3]) > 1.0E-4F) {
               x = (Math.round(this.xf[0] * x + this.xf[4]) - this.xf[4]) / this.xf[0];
               y = (Math.round(this.xf[3] * y + this.xf[5]) - this.xf[5]) / this.xf[3];
            }
         }

         Object p = RichText.prepare(s);
         if (p instanceof RichText.Layout l) {
            try {
               return RichText.draw(this.vg, l, x, y, align, size, (argb >>> 24 & 0xFF) / 255.0F * this.alpha) - x;
            } catch (Throwable t) {
               RichText.emojiFailed(t);
               NanoVG.nvgFontFace(this.vg, this.face(font));
               NanoVG.nvgFontSize(this.vg, size);
               NanoVG.nvgTextAlign(this.vg, align);
               NanoVG.nvgFillColor(this.vg, this.col(argb, this.c1));
               p = RichText.prepare(s);
            }
         }

         float end = NanoVG.nvgText(this.vg, x, y, p instanceof String str ? str : s);
         return end - x;
      } else {
         return 0.0F;
      }
   }

   @Override
   public float textWidth(String font, float size, String s) {
      if (s != null && !s.isEmpty()) {
         NanoVG.nvgFontFace(this.vg, this.face(font));
         NanoVG.nvgFontSize(this.vg, size);
         NanoVG.nvgTextAlign(this.vg, 65);
         Object p = RichText.prepare(s);
         if (p instanceof RichText.Layout l) {
            try {
               return RichText.width(this.vg, l, size);
            } catch (Throwable t) {
               RichText.emojiFailed(t);
               p = RichText.prepare(s);
            }
         }

         return NanoVG.nvgTextBounds(this.vg, 0.0F, 0.0F, p instanceof String str ? str : s, this.bounds);
      } else {
         return 0.0F;
      }
   }

   public record Icon(String id, Object stack, float x, float y, float size, float cx0, float cy0, float cx1, float cy1, float alpha, int group) {
   }
}
