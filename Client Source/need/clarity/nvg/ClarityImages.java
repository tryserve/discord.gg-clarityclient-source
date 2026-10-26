package need.clarity.nvg;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public final class ClarityImages {
   private static final Map<String, Integer> CACHE = new HashMap<>();

   private ClarityImages() {
   }

   public static int get(long vg, String name) {
      Integer cached = CACHE.get(name);
      if (cached != null) {
         return cached;
      }

      int id = 0;

      try (InputStream in = ClarityImages.class.getResourceAsStream("/assets/clarity/textures/fx/" + name + ".png")) {
         if (in != null) {
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            id = NanoVG.nvgCreateImageMem(vg, 1, buf);
         }
      } catch (Throwable var10) {
      }

      CACHE.put(name, id);
      return id;
   }

   public static void clear() {
      CACHE.clear();
   }

   public static void draw(long vg, int img, float cx, float cy, float size, float angleRad, int argb) {
      if (img > 0) {
         float a = (argb >>> 24 & 0xFF) / 255.0F;
         float r = (argb >>> 16 & 0xFF) / 255.0F;
         float g = (argb >>> 8 & 0xFF) / 255.0F;
         float b = (argb & 0xFF) / 255.0F;
         float h = size * 0.5F;

         try (MemoryStack s = MemoryStack.stackPush()) {
            NVGPaint p = NVGPaint.malloc(s);
            NanoVG.nvgSave(vg);
            NanoVG.nvgTranslate(vg, cx, cy);
            NanoVG.nvgRotate(vg, angleRad);
            NanoVG.nvgImagePattern(vg, -h, -h, size, size, 0.0F, img, 1.0F, p);
            p.innerColor().r(r).g(g).b(b).a(a);
            p.outerColor().r(r).g(g).b(b).a(a);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, -h, -h, size, size);
            NanoVG.nvgFillPaint(vg, p);
            NanoVG.nvgFill(vg);
            NanoVG.nvgRestore(vg);
         }
      }
   }
}
