package need.clarity.nvg;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.Clarity;
import need.clarity.modules.utility.NameProtect;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public final class RichText {
   private static final int CACHE = 4096;
   private static final Map<String, Object> cache = new LinkedHashMap<String, Object>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, Object> e) {
         return this.size() > 4096;
      }
   };
   private static volatile boolean emojiOff = false;
   private static final float EMOJI_EM = 1.18F;
   private static final float EMOJI_ADV = 1.3F;
   private static final float[] B = new float[4];

   private RichText() {
   }

   public static void emojiFailed(Throwable t) {
      if (!emojiOff) {
         emojiOff = true;
         synchronized (cache) {
            cache.clear();
         }

         try {
            Clarity.LOGGER.error("[Clarity] Emoji drawing failed; emoji are shown as text from now on", t);
         } catch (Throwable var3) {
         }
      }
   }

   public static Object prepare(String s) {
      if (s != null && !s.isEmpty()) {
         if (NameProtect.active()) {
            s = NameProtect.maybeReplace(s);
         }

         if (simple(s)) {
            return s;
         }

         synchronized (cache) {
            Object hit = cache.get(s);
            if (hit != null) {
               return hit;
            }
         }

         Object p = build(s);
         synchronized (cache) {
            cache.put(s, p);
            return p;
         }
      } else {
         return s == null ? "" : s;
      }
   }

   private static boolean simple(String s) {
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c >= 592 || c == ':') {
            return false;
         }
      }

      return true;
   }

   private static Object build(String s) {
      List<Object> items = new ArrayList<>();
      StringBuilder text = new StringBuilder(s.length());
      boolean emoji = false;
      int i = 0;
      int n = s.length();

      while (i < n) {
         int cp = s.codePointAt(i);
         if (!emojiOff && cp == 58 && (i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1)))) {
            int end = shortcodeEnd(s, i);
            if (end > 0 && end + 1 < n && Character.isLetterOrDigit(s.charAt(end + 1))) {
               end = -1;
            }

            if (end > 0) {
               Emoji.Entry e = Emoji.byName(s.substring(i + 1, end));
               if (e != null) {
                  if (text.length() > 0) {
                     items.add(text.toString());
                     text.setLength(0);
                  }

                  items.add(e.index());
                  emoji = true;
                  i = end + 1;
                  continue;
               }
            }
         }

         boolean keycap = cp < 169 && i + 1 < n && (s.charAt(i + 1) == '️' || s.charAt(i + 1) == 8419);
         if (!emojiOff && (cp >= 169 || keycap) && Emoji.mayStart(cp)) {
            int[] m = Emoji.match(s, i);
            if (m != null) {
               if (text.length() > 0) {
                  items.add(text.toString());
                  text.setLength(0);
               }

               items.add(m[0]);
               emoji = true;
               i += m[1];
               if (i < n) {
                  int next = s.codePointAt(i);
                  if (next >= 127995 && next <= 127999) {
                     i += Character.charCount(next);
                  }
               }
               continue;
            }
         }

         int len = Character.charCount(cp);
         if (cp < 592 || Glyphs.covered(cp)) {
            text.appendCodePoint(cp);
         } else if (!Glyphs.invisible(cp)) {
            text.append(Glyphs.substitute(cp));
         }

         i += len;
      }

      if (!emoji) {
         return text.toString();
      }

      if (text.length() > 0) {
         items.add(text.toString());
      }

      return new RichText.Layout(items.toArray());
   }

   private static int shortcodeEnd(String s, int i) {
      int j = i + 1;

      for (int n = Math.min(s.length(), i + 42); j < n; j++) {
         char c = s.charAt(j);
         if (c == ':') {
            return j > i + 1 ? j : -1;
         }

         if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_' && c != '+' && c != '-') {
            return -1;
         }
      }

      return -1;
   }

   public static float width(long vg, RichText.Layout l, float size) {
      float w = 0.0F;

      for (Object o : l.items) {
         if (o instanceof String t) {
            w += NanoVG.nvgTextBounds(vg, 0.0F, 0.0F, t, B);
         } else {
            w += size * 1.3F;
         }
      }

      return w;
   }

   public static float draw(long vg, RichText.Layout l, float x, float y, int align, float size, float alpha) {
      float w = width(vg, l, size);
      float sx = x;
      if ((align & 2) != 0) {
         sx = x - w / 2.0F;
      } else if ((align & 4) != 0) {
         sx = x - w;
      }

      float baseline;
      try (MemoryStack st = MemoryStack.stackPush()) {
         FloatBuffer asc = st.floats(0.0F);
         FloatBuffer desc = st.floats(0.0F);
         FloatBuffer lh = st.floats(0.0F);
         NanoVG.nvgTextMetrics(vg, asc, desc, lh);
         float a = asc.get(0);
         float d = desc.get(0);
         if ((align & 8) != 0) {
            baseline = y + a;
         } else if ((align & 16) != 0) {
            baseline = y + (a + d) / 2.0F;
         } else if ((align & 32) != 0) {
            baseline = y + d;
         } else {
            baseline = y;
         }
      }

      float em = size * 1.18F;
      float top = baseline - size * 0.36F - em / 2.0F;
      int vAlign = align & 120;
      NanoVG.nvgTextAlign(vg, 1 | (vAlign == 0 ? 64 : vAlign));
      float cx = sx;

      for (Object o : l.items) {
         if (o instanceof String t) {
            cx = NanoVG.nvgText(vg, cx, y, t);
         } else {
            float adv = size * 1.3F;
            Emoji.draw(vg, (Integer)o, cx + (adv - em) / 2.0F, top, em, alpha);
            cx += adv;
         }
      }

      NanoVG.nvgTextAlign(vg, align);
      return cx;
   }

   public static final class Layout {
      final Object[] items;

      Layout(Object[] items) {
         this.items = items;
      }
   }
}
