package need.clarity.agent.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonLite {
   private final String s;
   private int i;

   private JsonLite(String s) {
      this.s = s;
   }

   static Object parse(String text) {
      try {
         JsonLite p = new JsonLite(text);
         p.ws();
         return p.value();
      } catch (RuntimeException e) {
         return null;
      }
   }

   static Map<String, Object> obj(Object o, String key) {
      return (Map<String, Object>)(o instanceof Map<?, ?> m && m.get(key) instanceof Map<?, ?> r ? r : null);
   }

   static String str(Map<String, Object> m, String key) {
      return m != null && m.get(key) instanceof String v ? v : null;
   }

   static Boolean bool(Map<String, Object> m, String key) {
      return m != null && m.get(key) instanceof Boolean v ? v : null;
   }

   private void ws() {
      while (this.i < this.s.length() && Character.isWhitespace(this.s.charAt(this.i))) {
         this.i++;
      }
   }

   private Object value() {
      char c = this.s.charAt(this.i);
      switch (c) {
         case '"':
            return this.string();
         case '[':
            return this.array();
         case 'f':
            this.i += 5;
            return Boolean.FALSE;
         case 'n':
            this.i += 4;
            return null;
         case 't':
            this.i += 4;
            return Boolean.TRUE;
         case '{':
            return this.object();
         default:
            return this.number();
      }
   }

   private Map<String, Object> object() {
      Map<String, Object> m = new LinkedHashMap<>();
      this.i++;
      this.ws();
      if (this.s.charAt(this.i) == '}') {
         this.i++;
         return m;
      }

      char c;
      do {
         this.ws();
         String k = this.string();
         this.ws();
         this.expect(':');
         this.ws();
         m.put(k, this.value());
         this.ws();
         c = this.s.charAt(this.i++);
         if (c == '}') {
            return m;
         }
      } while (c == ',');

      throw new IllegalStateException();
   }

   private List<Object> array() {
      List<Object> l = new ArrayList<>();
      this.i++;
      this.ws();
      if (this.s.charAt(this.i) == ']') {
         this.i++;
         return l;
      }

      char c;
      do {
         this.ws();
         l.add(this.value());
         this.ws();
         c = this.s.charAt(this.i++);
         if (c == ']') {
            return l;
         }
      } while (c == ',');

      throw new IllegalStateException();
   }

   private String string() {
      this.expect('"');
      StringBuilder b = new StringBuilder();

      while (true) {
         char c = this.s.charAt(this.i++);
         if (c == '"') {
            return b.toString();
         }

         if (c != '\\') {
            b.append(c);
         } else {
            char e = this.s.charAt(this.i++);
            switch (e) {
               case 'b':
                  b.append('\b');
                  break;
               case 'c':
               case 'd':
               case 'e':
               case 'g':
               case 'h':
               case 'i':
               case 'j':
               case 'k':
               case 'l':
               case 'm':
               case 'o':
               case 'p':
               case 'q':
               case 's':
               default:
                  b.append(e);
                  break;
               case 'f':
                  b.append('\f');
                  break;
               case 'n':
                  b.append('\n');
                  break;
               case 'r':
                  b.append('\r');
                  break;
               case 't':
                  b.append('\t');
                  break;
               case 'u':
                  b.append((char)Integer.parseInt(this.s.substring(this.i, this.i + 4), 16));
                  this.i += 4;
            }
         }
      }
   }

   private Double number() {
      int st = this.i;

      while (this.i < this.s.length() && "+-0123456789.eE".indexOf(this.s.charAt(this.i)) >= 0) {
         this.i++;
      }

      if (st == this.i) {
         throw new IllegalStateException();
      } else {
         return Double.valueOf(this.s.substring(st, this.i));
      }
   }

   private void expect(char c) {
      if (this.s.charAt(this.i++) != c) {
         throw new IllegalStateException();
      }
   }
}
