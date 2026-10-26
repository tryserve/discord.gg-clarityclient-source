package need.clarity.clickgui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GuiData {
   public final List<GuiData.Entry> blocks = new ArrayList<>();
   public final List<GuiData.Entry> allItems = new ArrayList<>();
   public final LinkedHashMap<String, List<GuiData.Module>> categories = new LinkedHashMap<>();
   public final Map<String, GuiData.Module> modules = new LinkedHashMap<>();
   public final LinkedHashMap<String, List<String>> themes = new LinkedHashMap<>();
   public final List<String> capes = new ArrayList<>();
   public final Set<String> defaultOn = new HashSet<>();
   private static GuiData cached;

   public static synchronized GuiData get() {
      if (cached == null) {
         cached = load(readHtml());
      }

      return cached;
   }

   public static String readHtml() {
      try (InputStream in = GuiData.class.getResourceAsStream("/assets/clarity/Clarity.html")) {
         return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception e) {
         return "";
      }
   }

   public static GuiData load(String html) {
      GuiData d = new GuiData();
      Object cats = JsLiteral.after(html, "const categories=");
      Object sets = JsLiteral.after(html, "const moduleSettings=");
      Object th = JsLiteral.after(html, "const themes=");
      Object cp = JsLiteral.after(html, "const CAPES=");
      Map<String, Object> setMap = (Map<String, Object>)(sets instanceof Map<?, ?> m ? m : Map.of());
      if (cats instanceof Map<?, ?> cm) {
         for (Map.Entry<?, ?> e : cm.entrySet()) {
            String cat = String.valueOf(e.getKey());
            List<GuiData.Module> list = new ArrayList<>();
            if (e.getValue() instanceof List<?> mods) {
               for (Object o : mods) {
                  if (o instanceof List<?> pair && !pair.isEmpty()) {
                     String name = String.valueOf(pair.get(0));
                     String desc = pair.size() > 1 ? String.valueOf(pair.get(1)) : "";
                     GuiData.Module mod = new GuiData.Module(name, desc, cat);
                     Object var21 = setMap.get(name);
                     if (var21 instanceof List) {
                        for (Object so : (List)var21) {
                           if (so instanceof List<?> sd && sd.size() >= 2) {
                              mod.settings.add(new GuiData.Setting((List<Object>)sd));
                           }
                        }
                     }

                     list.add(mod);
                     d.modules.put(name, mod);
                  }
               }
            }

            d.categories.put(cat, list);
         }
      }

      if (th instanceof Map<?, ?> tm) {
         for (Map.Entry<?, ?> e : tm.entrySet()) {
            List<String> cols = new ArrayList<>();
            Object var38 = e.getValue();
            if (var38 instanceof List) {
               for (Object o : (List)var38) {
                  cols.add(String.valueOf(o));
               }
            }

            if (cols.size() >= 5) {
               d.themes.put(String.valueOf(e.getKey()), cols);
            }
         }
      }

      Matcher dm = Pattern.compile("state\\[\"([^\"]+)\"\\]\\.enabled\\s*=\\s*true").matcher(html);

      while (dm.find()) {
         d.defaultOn.add(dm.group(1));
      }

      Object bl = JsLiteral.after(html, "const BLOCKS = ");
      Object it = JsLiteral.after(html, "const ITEMS = ");
      if (it instanceof List) {
         for (Object o : (List)it) {
            if (o instanceof Map<?, ?> m && m.get("id") != null) {
               d.allItems.add(new GuiData.Entry(String.valueOf(m.get("id")), String.valueOf(m.get("name")), String.valueOf(m.get("group"))));
            }
         }
      }

      if (bl instanceof List) {
         for (Object o : (List)bl) {
            if (o instanceof Map<?, ?> m && m.get("id") != null) {
               GuiData.Entry e = new GuiData.Entry(String.valueOf(m.get("id")), String.valueOf(m.get("name")), String.valueOf(m.get("group")));
               d.blocks.add(e);
               d.allItems.add(new GuiData.Entry(e.id(), e.name(), "Blocks / " + e.group()));
            }
         }
      }

      if (cp instanceof List) {
         for (Object o : (List)cp) {
            if (o instanceof Map<?, ?> c && c.get("id") != null) {
               d.capes.add(String.valueOf(c.get("id")));
            }
         }
      }

      return d;
   }

   static String str(List<Object> l, int k) {
      return l.size() > k && l.get(k) != null ? String.valueOf(l.get(k)) : "";
   }

   static Map<String, Object> cast(Map<?, ?> m) {
      return (Map<String, Object>)m;
   }

   public record Entry(String id, String name, String group) {
   }

   public static final class Module {
      public final String name;
      public final String description;
      public final String category;
      public final List<GuiData.Setting> settings = new ArrayList<>();

      Module(String name, String description, String category) {
         this.name = name;
         this.description = description;
         this.category = category;
      }
   }

   public static final class Setting {
      public final String name;
      public final String type;
      public final List<Object> raw;
      public final Map<String, Object> meta;

      public static GuiData.Setting of(List<Object> raw) {
         return new GuiData.Setting(raw);
      }

      Setting(List<Object> raw) {
         this.raw = raw;
         this.name = GuiData.str(raw, 0);
         this.type = GuiData.str(raw, 1);
         this.meta = (raw.isEmpty() ? null : raw.get(raw.size() - 1)) instanceof Map<?, ?> m && raw.size() > 2 ? GuiData.cast(m) : null;
      }

      public Object arg(int k) {
         return this.raw.size() > k ? this.raw.get(k) : null;
      }

      public double num(int k, double def) {
         return this.arg(2) instanceof List<?> l && l.size() > k && l.get(k) instanceof Number n ? n.doubleValue() : def;
      }

      public List<String> options() {
         Object a = this.arg(2);
         List<String> out = new ArrayList<>();
         if (a instanceof List) {
            for (Object o : (List)a) {
               if (o != null) {
                  out.add(String.valueOf(o));
               }
            }
         }

         return out;
      }

      public Map<String, List<String>> groups() {
         if (this.meta != null && this.meta.get("groups") instanceof Map<?, ?> g) {
            LinkedHashMap var9 = new LinkedHashMap();

            for (Map.Entry<?, ?> e : g.entrySet()) {
               List<String> l = new ArrayList<>();
               Object var7 = e.getValue();
               if (var7 instanceof List) {
                  for (Object o : (List)var7) {
                     l.add(String.valueOf(o));
                  }
               }

               var9.put(String.valueOf(e.getKey()), l);
            }

            return var9;
         } else {
            return null;
         }
      }

      public Object defaultValue() {
         return switch (this.type) {
            case "range" -> this.num(2, 0.0);
            case "check" -> this.arg(2) instanceof Boolean b ? b : Boolean.FALSE;
            case "select" -> this.arg(3) != null ? String.valueOf(this.arg(3)) : (this.options().isEmpty() ? "" : this.options().get(0));
            case "multiselect" -> this.arg(3) instanceof List<?> l ? new ArrayList(l) : new ArrayList();
            case "blockpicker", "itempicker" -> new ArrayList();
            default -> this.arg(2) != null && !(this.arg(2) instanceof Map) ? (this.arg(2) instanceof List ? "" : String.valueOf(this.arg(2))) : "";
         };
      }

      public String placeholder() {
         return this.arg(3) instanceof String p ? p : "";
      }
   }
}
