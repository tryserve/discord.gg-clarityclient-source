package need.clarity.nvg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class Emoji {
   private static final Map<String, Emoji.Entry> BY_NAME = new HashMap<>();
   private static final Map<String, Emoji.Entry> BY_SEQ = new HashMap<>();
   private static final Set<Integer> STARTS = new HashSet<>();
   private static final List<Emoji.Entry> ALL = new ArrayList<>();
   private static int cell = 48;
   private static int pad = 4;
   private static int cols = 1;
   private static int rows = 1;
   private static int maxSeq = 1;
   private static boolean loaded;
   private static boolean ok;
   private static byte[] sheetBytes;
   private static final Map<Long, Integer> IMAGES = new HashMap<>();

   private Emoji() {
   }

   private static synchronized void load() {
      if (!loaded) {
         loaded = true;

         try (InputStream in = Emoji.class.getResourceAsStream("/assets/clarity/emoji/emoji.txt")) {
            if (in != null) {
               BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

               String line;
               while ((line = r.readLine()) != null) {
                  if (!line.isEmpty()) {
                     String[] f = line.split("\t");
                     if (f[0].startsWith("#")) {
                        cell = Integer.parseInt(f[1]);
                        pad = Integer.parseInt(f[2]);
                        cols = Integer.parseInt(f[3]);
                        rows = Integer.parseInt(f[4]);
                     } else {
                        int idx = Integer.parseInt(f[0]);
                        String[] seqs = f[1].split(",");
                        List<String> names = f.length > 2 && !f[2].isEmpty() ? List.of(f[2].split(",")) : List.of();
                        String main = chars(seqs[0]);
                        Emoji.Entry e = new Emoji.Entry(idx, main, names.isEmpty() ? "" : names.get(0), names);
                        ALL.add(e);

                        for (String n : names) {
                           BY_NAME.putIfAbsent(n, e);
                        }

                        for (int i = 0; i < seqs.length; i++) {
                           String s = chars(seqs[i]);
                           int first = s.codePointAt(0);
                           if (i <= 0 || first >= 126976) {
                              BY_SEQ.putIfAbsent(s, e);
                              STARTS.add(first);
                              maxSeq = Math.max(maxSeq, s.codePointCount(0, s.length()));
                           }
                        }
                     }
                  }
               }

               ok = !ALL.isEmpty();
            }
         } catch (Throwable t) {
            ok = false;
         }
      }
   }

   private static String chars(String hexSeq) {
      StringBuilder b = new StringBuilder();

      for (String h : hexSeq.split("-")) {
         b.appendCodePoint(Integer.parseInt(h, 16));
      }

      return b.toString();
   }

   public static boolean available() {
      if (!loaded) {
         load();
      }

      return ok;
   }

   public static Emoji.Entry byName(String name) {
      if (!loaded) {
         load();
      }

      return name == null ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
   }

   static boolean mayStart(int cp) {
      if (!loaded) {
         load();
      }

      return STARTS.contains(cp);
   }

   static int[] match(String s, int i) {
      if (!loaded) {
         load();
      }

      int best = -1;
      int bestEnd = -1;
      int j = i;
      StringBuilder b = new StringBuilder();

      for (int k = 0; k < maxSeq && j < s.length(); k++) {
         int cp = s.codePointAt(j);
         b.appendCodePoint(cp);
         j += Character.charCount(cp);
         Emoji.Entry e = BY_SEQ.get(b.toString());
         if (e != null) {
            best = e.index();
            bestEnd = j;
         }
      }

      if (best < 0) {
         return null;
      }

      if (bestEnd < s.length() && s.charAt(bestEnd) == '️') {
         bestEnd++;
      }

      return new int[]{best, bestEnd - i};
   }

   public static String partialAtEnd(String text) {
      if (text == null) {
         return null;
      }

      int colon = text.lastIndexOf(58);
      if (colon < 0 || colon == text.length() - 1) {
         return null;
      }

      if (colon > 0 && !Character.isWhitespace(text.charAt(colon - 1))) {
         return null;
      }

      String q = text.substring(colon + 1);
      if (q.length() >= 2 && q.length() <= 32) {
         for (int i = 0; i < q.length(); i++) {
            char ch = q.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '+' && ch != '-') {
               return null;
            }
         }

         return q.toLowerCase(Locale.ROOT);
      } else {
         return null;
      }
   }

   public static List<Emoji.Entry> search(String q, int max) {
      if (!loaded) {
         load();
      }

      List<Emoji.Entry> out = new ArrayList<>();
      if (q != null && !q.isEmpty()) {
         q = q.toLowerCase(Locale.ROOT);
         Set<Emoji.Entry> seen = Collections.newSetFromMap(new IdentityHashMap<>());
         Emoji.Entry exact = BY_NAME.get(q);
         if (exact != null) {
            out.add(exact);
            seen.add(exact);
         }

         for (int pass = 0; pass < 2 && out.size() < max; pass++) {
            label52:
            for (Emoji.Entry e : ALL) {
               if (out.size() < max) {
                  if (!seen.contains(e)) {
                     for (String n : e.names()) {
                        boolean hit = pass == 0 ? n.startsWith(q) : n.contains(q);
                        if (hit) {
                           out.add(e);
                           seen.add(e);
                           continue label52;
                        }
                     }
                  }
                  continue;
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static String bestName(Emoji.Entry e, String q) {
      for (String n : e.names()) {
         if (n.startsWith(q)) {
            return n;
         }
      }

      for (String n : e.names()) {
         if (n.contains(q)) {
            return n;
         }
      }

      return e.name();
   }

   private static int image(long vg) {
      Integer id = IMAGES.get(vg);
      if (id != null && id > 0) {
         try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer w = st.ints(0);
            IntBuffer h = st.ints(0);
            NanoVG.nvgImageSize(vg, id, w, h);
            if (w.get(0) == sheetW() && h.get(0) == sheetH()) {
               return id;
            }
         }
      }

      if (id != null && id < 0) {
         return -1;
      }

      try {
         if (sheetBytes == null) {
            try (InputStream in = Emoji.class.getResourceAsStream("/assets/clarity/emoji/sheet.png")) {
               sheetBytes = in == null ? new byte[0] : in.readAllBytes();
            }
         }

         if (sheetBytes.length == 0) {
            IMAGES.put(vg, -1);
            return -1;
         }

         ByteBuffer buf = MemoryUtil.memAlloc(sheetBytes.length);

         try {
            buf.put(sheetBytes).flip();
            int img = NanoVG.nvgCreateImageMem(vg, 1, buf);
            IMAGES.put(vg, img > 0 ? img : -1);
            return img > 0 ? img : -1;
         } finally {
            MemoryUtil.memFree(buf);
         }
      } catch (Throwable t) {
         IMAGES.put(vg, -1);
         return -1;
      }
   }

   public static void forget(long vg) {
      IMAGES.remove(vg);
   }

   private static int stride() {
      return cell + pad * 2;
   }

   private static int sheetW() {
      return cols * stride();
   }

   private static int sheetH() {
      return rows * stride();
   }

   static void draw(long vg, int index, float x, float y, float size, float alpha) {
      if (!(alpha <= 0.003F)) {
         int img = image(vg);
         if (img > 0) {
            float scale = size / cell;
            float sx = index % cols * stride() + pad;
            float sy = index / cols * stride() + pad;
            NanoVG.nvgSave(vg);

            try (MemoryStack st = MemoryStack.stackPush()) {
               NVGPaint p = NVGPaint.malloc(st);
               NanoVG.nvgImagePattern(vg, x - sx * scale, y - sy * scale, sheetW() * scale, sheetH() * scale, 0.0F, img, Math.min(1.0F, alpha), p);
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgRect(vg, x, y, size, size);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
            } finally {
               NanoVG.nvgRestore(vg);
            }
         }
      }
   }

   public static final class Entry {
      private final int index;
      private final String chars;
      private final String name;
      private final List<String> names;

      Entry(int index, String chars, String name, List<String> names) {
         this.index = index;
         this.chars = chars;
         this.name = name;
         this.names = names;
      }

      public int index() {
         return this.index;
      }

      public String chars() {
         return this.chars;
      }

      public String name() {
         return this.name;
      }

      public List<String> names() {
         return this.names;
      }
   }
}
