package need.clarity.clickgui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsLiteral {
   private final String s;
   private int i;

   private JsLiteral(String s, int start) {
      this.s = s;
      this.i = start;
   }

   public static Object after(String src, String marker) {
      int at = src.indexOf(marker);
      if (at < 0) {
         return null;
      }

      JsLiteral p = new JsLiteral(src, at + marker.length());
      return p.value();
   }

   private Object value() {
      this.ws();
      if (this.i >= this.s.length()) {
         return null;
      } else {
         char c = this.s.charAt(this.i);
         if (c == '{') {
            return this.object();
         } else if (c == '[') {
            return this.array();
         } else if (c == '"' || c == '\'' || c == '`') {
            return this.string();
         } else if (c == '-' || c == '.' || Character.isDigit(c)) {
            return this.number();
         } else if (this.s.startsWith("true", this.i)) {
            this.i += 4;
            return Boolean.TRUE;
         } else if (this.s.startsWith("false", this.i)) {
            this.i += 5;
            return Boolean.FALSE;
         } else if (this.s.startsWith("null", this.i)) {
            this.i += 4;
            return null;
         } else if (this.s.startsWith("undefined", this.i)) {
            this.i += 9;
            return null;
         } else {
            this.skipExpression();
            return null;
         }
      }
   }

   private Map<String, Object> object() {
      Map<String, Object> m = new LinkedHashMap<>();
      this.i++;

      while (true) {
         this.ws();
         if (this.i >= this.s.length()) {
            return m;
         }

         char c = this.s.charAt(this.i);
         if (c == '}') {
            this.i++;
            return m;
         }

         if (c == ',') {
            this.i++;
         } else {
            String key;
            if (c != '"' && c != '\'' && c != '`') {
               int st = this.i;

               while (this.i < this.s.length() && (Character.isLetterOrDigit(this.s.charAt(this.i)) || this.s.charAt(this.i) == '_' || this.s.charAt(this.i) == '$')) {
                  this.i++;
               }

               key = this.s.substring(st, this.i);
               if (key.isEmpty()) {
                  this.i++;
                  continue;
               }
            } else {
               key = this.string();
            }

            this.ws();
            if (this.i < this.s.length() && this.s.charAt(this.i) == ':') {
               this.i++;
            }

            m.put(key, this.value());
         }
      }
   }

   private List<Object> array() {
      List<Object> a = new ArrayList<>();
      this.i++;

      while (true) {
         this.ws();
         if (this.i >= this.s.length()) {
            return a;
         }

         char c = this.s.charAt(this.i);
         if (c == ']') {
            this.i++;
            return a;
         }

         if (c == ',') {
            this.i++;
         } else {
            a.add(this.value());
         }
      }
   }

   private String string() {
      char q = this.s.charAt(this.i++);
      StringBuilder b = new StringBuilder();

      while (this.i < this.s.length()) {
         char c = this.s.charAt(this.i++);
         if (c == q) {
            break;
         }

         if (c == '\\' && this.i < this.s.length()) {
            char e = this.s.charAt(this.i++);
            switch (e) {
               case 'n':
                  b.append('\n');
                  break;
               case 'o':
               case 'p':
               case 'q':
               case 's':
               default:
                  b.append(e);
                  break;
               case 'r':
                  b.append('\r');
                  break;
               case 't':
                  b.append('\t');
                  break;
               case 'u':
                  if (this.i + 4 <= this.s.length()) {
                     try {
                        b.append((char)Integer.parseInt(this.s.substring(this.i, this.i + 4), 16));
                     } catch (NumberFormatException var6) {
                     }

                     this.i += 4;
                  }
            }
         } else {
            b.append(c);
         }
      }

      return b.toString();
   }

   private Double number() {
      int st = this.i;

      while (this.i < this.s.length() && "+-.0123456789eExX".indexOf(this.s.charAt(this.i)) >= 0) {
         this.i++;
      }

      try {
         return Double.parseDouble(this.s.substring(st, this.i));
      } catch (NumberFormatException e) {
         return 0.0;
      }
   }

   private void ws() {
      while (this.i < this.s.length()) {
         char c = this.s.charAt(this.i);
         if (Character.isWhitespace(c)) {
            this.i++;
         } else if (c == '/' && this.i + 1 < this.s.length() && this.s.charAt(this.i + 1) == '/') {
            while (this.i < this.s.length() && this.s.charAt(this.i) != '\n') {
               this.i++;
            }
         } else {
            if (c == '/' && this.i + 1 < this.s.length() && this.s.charAt(this.i + 1) == '*') {
               int e = this.s.indexOf("*/", this.i + 2);
               this.i = e < 0 ? this.s.length() : e + 2;
               continue;
            }
            break;
         }
      }
   }

   private void skipExpression() {
      int depth = 0;

      while (this.i < this.s.length()) {
         char c = this.s.charAt(this.i);
         if (c != '"' && c != '\'' && c != '`') {
            if (c == '(' || c == '[' || c == '{') {
               depth++;
            } else if (c != ')' && c != ']' && c != '}') {
               if (c == ',' && depth == 0) {
                  return;
               }
            } else {
               if (depth == 0) {
                  return;
               }

               depth--;
            }

            this.i++;
         } else {
            this.string();
         }
      }
   }
}
