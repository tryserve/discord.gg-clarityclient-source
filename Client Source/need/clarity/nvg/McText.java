package need.clarity.nvg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class McText {
   private static final String OBF = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

   private McText() {
   }

   public static List<McText.Run> runs(Component c, int defaultRgb) {
      List<McText.Run> out = new ArrayList<>();
      if (c == null) {
         return out;
      }

      c.visit((style, s) -> {
         legacy(s, style, defaultRgb, out);
         return Optional.empty();
      }, Style.EMPTY);
      return out;
   }

   private static void legacy(String s, Style base, int defaultRgb, List<McText.Run> out) {
      Style st = base;
      StringBuilder b = new StringBuilder();

      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         if (ch == 167 && i + 1 < s.length()) {
            if (b.length() > 0) {
               out.add(run(b.toString(), st, defaultRgb));
               b.setLength(0);
            }

            ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(s.charAt(i + 1)));
            if (f != null) {
               st = f == ChatFormatting.RESET ? base : st.applyLegacyFormat(f);
            }

            i++;
         } else {
            b.append(ch);
         }
      }

      if (b.length() > 0) {
         out.add(run(b.toString(), st, defaultRgb));
      }
   }

   private static McText.Run run(String t, Style st, int defaultRgb) {
      TextColor col = st.getColor();
      int rgb = col != null ? col.getValue() & 16777215 : defaultRgb;
      return new McText.Run(t, rgb, st.isBold(), st.isItalic(), st.isUnderlined(), st.isStrikethrough(), st.isObfuscated(), st);
   }

   public static String plain(List<McText.Run> runs) {
      StringBuilder b = new StringBuilder();

      for (McText.Run r : runs) {
         b.append(r.text());
      }

      return b.toString();
   }

   public static String[] faces(String family) {
      String k = ClarityFonts.key(family);
      return "inter".equals(k) ? new String[]{"inter", "inter-med", "inter-sb"} : new String[]{k, k + "-med", k + "-sb"};
   }

   private static String shown(McText.Run r) {
      if (!r.obfuscated()) {
         return r.text();
      }

      StringBuilder b = new StringBuilder();
      long seed = System.nanoTime() / 60000000L;

      for (int i = 0; i < r.text().length(); i++) {
         char c = r.text().charAt(i);
         b.append(
            c == ' '
               ? ' '
               : "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                  .charAt(Math.floorMod(seed * 31L + i * 7919L, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".length()))
         );
      }

      return b.toString();
   }

   public static float width(long vg, String[] faces, float size, List<McText.Run> runs) {
      float w = 0.0F;

      for (McText.Run r : runs) {
         w += ClarityDraw.textWidth(vg, r.bold() ? faces[2] : faces[0], size, 0.0F, r.text());
      }

      return w;
   }

   public static float draw(long vg, String[] faces, float size, List<McText.Run> runs, float x, float y, float alpha, boolean shadow, List<McText.Hit> hits) {
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      if (a <= 0) {
         return x + width(vg, faces, size, runs);
      }

      for (McText.Run r : runs) {
         String face = r.bold() ? faces[2] : faces[0];
         String s = shown(r);
         float w = ClarityDraw.textWidth(vg, face, size, 0.0F, r.text());
         if (shadow) {
            ClarityDraw.text(vg, face, size, 0.0F, a * 3 / 5 << 24, 17, x + size * 0.07F, y + size * 0.07F, s, 0.0F);
         }

         ClarityDraw.text(vg, face, size, 0.0F, a << 24 | r.rgb(), 17, x, y, s);
         if (r.underline()) {
            ClarityDraw.fillRRect(vg, x, y + size * 0.45F, w, Math.max(0.8F, size * 0.07F), 0.0F, a << 24 | r.rgb());
         }

         if (r.strike()) {
            ClarityDraw.fillRRect(vg, x, y - size * 0.02F, w, Math.max(0.8F, size * 0.07F), 0.0F, a << 24 | r.rgb());
         }

         if (hits != null) {
            hits.add(new McText.Hit(x, y - size * 0.6F, w, size * 1.2F, r.style()));
         }

         x += w;
      }

      return x;
   }

   public static List<List<McText.Run>> wrap(long vg, String[] faces, float size, List<McText.Run> runs, float maxW) {
      List<List<McText.Run>> lines = new ArrayList<>();
      List<McText.Run> cur = new ArrayList<>();
      float cw = 0.0F;

      for (McText.Run r : runs) {
         String face = r.bold() ? faces[2] : faces[0];
         String[] parts = r.text().split("(?<=\\n)|(?<= )");

         for (String p : parts) {
            boolean newline = p.endsWith("\n");
            String piece = newline ? p.substring(0, p.length() - 1) : p;
            float pw = ClarityDraw.textWidth(vg, face, size, 0.0F, piece);
            if (cw + pw > maxW && cw > 0.0F) {
               lines.add(cur);
               cur = new ArrayList<>();
               cw = 0.0F;
               piece = piece.stripLeading();
               pw = ClarityDraw.textWidth(vg, face, size, 0.0F, piece);
            }

            while (pw > maxW && piece.length() > 1) {
               int n = piece.length();

               while (n > 1 && ClarityDraw.textWidth(vg, face, size, 0.0F, piece.substring(0, n)) > maxW - cw) {
                  n--;
               }

               cur.add(with(r, piece.substring(0, n)));
               lines.add(cur);
               cur = new ArrayList<>();
               cw = 0.0F;
               piece = piece.substring(n);
               pw = ClarityDraw.textWidth(vg, face, size, 0.0F, piece);
            }

            if (!piece.isEmpty()) {
               cur.add(with(r, piece));
               cw += pw;
            }

            if (newline) {
               lines.add(cur);
               cur = new ArrayList<>();
               cw = 0.0F;
            }
         }
      }

      if (!cur.isEmpty() || lines.isEmpty()) {
         lines.add(cur);
      }

      return lines;
   }

   private static McText.Run with(McText.Run r, String t) {
      return new McText.Run(t, r.rgb(), r.bold(), r.italic(), r.underline(), r.strike(), r.obfuscated(), r.style());
   }

   public record Hit(float x, float y, float w, float h, Style style) {
   }

   public record Run(String text, int rgb, boolean bold, boolean italic, boolean underline, boolean strike, boolean obfuscated, Style style) {
   }
}
