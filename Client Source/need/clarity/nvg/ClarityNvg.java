package need.clarity.nvg;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public final class ClarityNvg {
   private ClarityNvg() {
   }

   public static NVGColor argb(int argb, NVGColor out) {
      int a = argb >>> 24 & 0xFF;
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      return NanoVG.nvgRGBA((byte)r, (byte)g, (byte)b, (byte)a, out);
   }

   public static NVGColor rgb(int rgb, float alpha, NVGColor out) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      return NanoVG.nvgRGBA((byte)r, (byte)g, (byte)b, (byte)clamp255(alpha * 255.0F), out);
   }

   public static NVGColor white(float alpha, NVGColor out) {
      return NanoVG.nvgRGBA((byte)-1, (byte)-1, (byte)-1, (byte)clamp255(alpha * 255.0F), out);
   }

   public static NVGColor black(float alpha, NVGColor out) {
      return NanoVG.nvgRGBA((byte)0, (byte)0, (byte)0, (byte)clamp255(alpha * 255.0F), out);
   }

   public static int withAlpha(int rgb, float alpha) {
      return clamp255(alpha * 255.0F) << 24 | rgb & 16777215;
   }

   public static NVGColor color(MemoryStack stack) {
      return NVGColor.malloc(stack);
   }

   private static int clamp255(float v) {
      int i = Math.round(v);
      return i < 0 ? 0 : (i > 255 ? 255 : i);
   }
}
