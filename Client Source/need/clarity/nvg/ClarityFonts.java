package need.clarity.nvg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;

public final class ClarityFonts {
   public static final String INTER = "inter";
   public static final String INTER_MEDIUM = "inter-med";
   public static final String INTER_SEMIBOLD = "inter-sb";
   public static final LinkedHashMap<String, String> FAMILIES = new LinkedHashMap<>();
   private static final List<ByteBuffer> RETAINED = new ArrayList<>();
   private static boolean loaded = false;
   private static final String[][] FALLBACKS = new String[][]{
      {"clarity-symbols", "ClaritySymbols"}, {"clarity-ext", "ClarityExtended"}, {"clarity-ext2", "ClarityExtended2"}, {"clarity-cjk", "ClarityCJK"}
   };
   private static final Map<Long, List<String>> CHAINS = new HashMap<>();
   public static final String SYMBOLS = "clarity-symbols";

   public static String key(String family) {
      String file = family == null ? null : FAMILIES.get(family);
      return file != null && !"Inter".equals(file) ? file.toLowerCase(Locale.ROOT) : "inter";
   }

   private ClarityFonts() {
   }

   public static void loadAll(long vg) {
      if (!loaded) {
         loadInterInto(vg);
         loaded = true;
      }
   }

   public static void loadInterInto(long vg) {
      tryLoad(vg, "inter", "Inter-Regular");
      tryLoad(vg, "inter-med", "Inter-Medium");
      tryLoad(vg, "inter-sb", "Inter-SemiBold");
      List<String> chain = loadFallbacks(vg);

      for (String f : new String[]{"inter", "inter-med", "inter-sb"}) {
         addChain(vg, f, chain);
      }
   }

   public static void loadInto(long vg) {
      loadInterInto(vg);
      List<String> chain = loadFallbacks(vg);

      for (Entry<String, String> e : FAMILIES.entrySet()) {
         if (!"Inter".equals(e.getKey())) {
            String k = key(e.getKey());
            String f = e.getValue();
            String[][] w = new String[][]{{k, f + "-Regular", "inter"}, {k + "-med", f + "-Medium", "inter-med"}, {k + "-sb", f + "-SemiBold", "inter-sb"}};

            for (String[] x : w) {
               if (tryLoad(vg, x[0], x[1])) {
                  NanoVG.nvgAddFallbackFont(vg, x[0], x[2]);
                  addChain(vg, x[0], chain);
               }
            }
         }
      }
   }

   private static List<String> loadFallbacks(long vg) {
      List<String> have = CHAINS.get(vg);
      if (have != null && NanoVG.nvgFindFont(vg, have.isEmpty() ? "clarity-symbols" : have.get(0)) >= 0) {
         return have;
      }

      List<String> out = new ArrayList<>();

      for (String[] f : FALLBACKS) {
         if (NanoVG.nvgFindFont(vg, f[0]) >= 0 || tryLoad(vg, f[0], f[1])) {
            out.add(f[0]);
         }
      }

      CHAINS.put(vg, out);
      return out;
   }

   private static void addChain(long vg, String font, List<String> chain) {
      if (NanoVG.nvgFindFont(vg, font) >= 0) {
         for (String fb : chain) {
            NanoVG.nvgAddFallbackFont(vg, font, fb);
         }
      }
   }

   public static boolean isLoaded() {
      return loaded;
   }

   public static void free() {
      for (ByteBuffer b : RETAINED) {
         MemoryUtil.memFree(b);
      }

      RETAINED.clear();
      loaded = false;
   }

   private static boolean tryLoad(long vg, String logicalName, String fileName) {
      ByteBuffer buf = readQuietly("/fonts/" + fileName + ".ttf");
      if (buf == null) {
         buf = readQuietly("/assets/clarity/fonts/" + fileName + ".ttf");
      }

      if (buf == null) {
         return false;
      }

      RETAINED.add(buf);
      return NanoVG.nvgCreateFontMem(vg, logicalName, buf, false) >= 0;
   }

   private static ByteBuffer readQuietly(String resourcePath) {
      try (InputStream is = ClarityFonts.class.getResourceAsStream(resourcePath)) {
         if (is == null) {
            return null;
         }

         byte[] bytes = is.readAllBytes();
         ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
         buf.put(bytes).flip();
         return buf;
      } catch (IOException e) {
         return null;
      }
   }

   static {
      FAMILIES.put("Inter", "Inter");
      FAMILIES.put("Roboto", "Roboto");
      FAMILIES.put("Poppins", "Poppins");
      FAMILIES.put("Montserrat", "Montserrat");
      FAMILIES.put("Outfit", "Outfit");
      FAMILIES.put("JetBrains Mono", "JetBrainsMono");
      FAMILIES.put("Pixelify Sans", "PixelifySans");
   }
}
