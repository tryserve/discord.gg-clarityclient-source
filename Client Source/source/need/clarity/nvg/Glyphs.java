package need.clarity.nvg;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;

public final class Glyphs {
   private static int[] starts;
   private static int[] ends;
   private static boolean loaded;

   private Glyphs() {
   }

   private static synchronized void load() {
      if (!loaded) {
         loaded = true;

         try (InputStream in = Glyphs.class.getResourceAsStream("/assets/clarity/fonts/coverage.bin")) {
            if (in != null) {
               DataInputStream d = new DataInputStream(new BufferedInputStream(in));
               int n = d.readInt();
               int[] s = new int[n];
               int[] e = new int[n];

               for (int i = 0; i < n; i++) {
                  s[i] = d.readInt();
                  e[i] = d.readInt();
               }

               starts = s;
               ends = e;
            }
         } catch (Throwable ignored) {
            starts = null;
            ends = null;
         }
      }
   }

   public static boolean covered(int cp) {
      if (cp < 592) {
         return true;
      }

      if (!loaded) {
         load();
      }

      int[] s = starts;
      int[] e = ends;
      if (s == null) {
         return true;
      }

      int lo = 0;
      int hi = s.length - 1;

      while (lo <= hi) {
         int mid = lo + hi >>> 1;
         if (cp < s[mid]) {
            hi = mid - 1;
         } else {
            if (cp <= e[mid]) {
               return true;
            }

            lo = mid + 1;
         }
      }

      return false;
   }

   public static boolean invisible(int cp) {
      if (cp == 8205 || cp == 8204 || cp == 8203 || cp == 8288 || cp == 65279) {
         return true;
      } else if (cp >= 65024 && cp <= 65039) {
         return true;
      } else if (cp >= 917504 && cp <= 917999) {
         return true;
      } else if (cp >= 8206 && cp <= 8207) {
         return true;
      } else if (cp >= 8234 && cp <= 8238) {
         return true;
      } else {
         return cp >= 8294 && cp <= 8297 ? true : Character.getType(cp) == 16 && cp != 173;
      }
   }

   public static String substitute(int cp) {
      String nf = Normalizer.normalize(new String(Character.toChars(cp)), Form.NFKC);
      boolean ok = !nf.isEmpty();
      int i = 0;

      while (ok && i < nf.length()) {
         int c = nf.codePointAt(i);
         if (!covered(c) && !invisible(c)) {
            ok = false;
         }

         i += Character.charCount(c);
      }

      if (ok) {
         return nf;
      }

      String base = Normalizer.normalize(nf, Form.NFD).replaceAll("\\p{M}+", "");
      return !base.isEmpty() && base.codePoints().allMatch(Glyphs::covered) ? base : "?";
   }
}
