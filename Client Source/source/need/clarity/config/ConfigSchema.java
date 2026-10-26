package need.clarity.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.clickgui.GuiData;

public final class ConfigSchema {
   public static final String GUI_MODULE = "Click GUI";
   static final String[] SNAPSHOT_EXTRAS = new String[]{"__theme", "__customCapes", "__customSkins"};
   static final String[] NEVER_IN_SNAPSHOT = new String[]{"__profiles", "__binds", "__serverProfiles", "__activeProfile", "__rev", "__webPrefs"};

   private ConfigSchema() {
   }

   private static GuiData data() {
      try {
         return GuiData.get();
      } catch (Throwable t) {
         return null;
      }
   }

   public static boolean defaultEnabled(String module) {
      if ("Click GUI".equals(module)) {
         return true;
      }

      GuiData d = data();
      return d != null && d.defaultOn.contains(module);
   }

   public static JsonObject defaultModule(String module) {
      JsonObject o = new JsonObject();
      o.addProperty("enabled", defaultEnabled(module));
      o.add("values", new JsonObject());
      o.add("bind", JsonNull.INSTANCE);
      o.addProperty("bindMode", "toggle");
      return o;
   }

   public static void normalizeRoot(JsonObject root) {
      if (root != null) {
         normalizeModules(root);
         JsonElement p = root.get("__profiles");
         if (p != null) {
            if (!p.isJsonObject()) {
               root.remove("__profiles");
            } else {
               JsonObject profiles = p.getAsJsonObject();

               for (String name : new ArrayList<String>(profiles.keySet())) {
                  JsonElement snap = profiles.get(name);
                  if (!name.isBlank() && snap != null && snap.isJsonObject()) {
                     normalizeSnapshot(snap.getAsJsonObject());
                  } else {
                     profiles.remove(name);
                  }
               }
            }
         }

         JsonElement b = root.get("__binds");
         if (b != null) {
            if (!b.isJsonArray()) {
               root.remove("__binds");
            } else {
               JsonArray keep = new JsonArray();

               for (JsonElement x : b.getAsJsonArray()) {
                  if (x.isJsonObject()) {
                     JsonObject set = x.getAsJsonObject();
                     String name = str(set.get("name"));
                     if (name != null && !name.isBlank()) {
                        if (!(set.get("binds") instanceof JsonObject)) {
                           set.add("binds", new JsonObject());
                        }

                        keep.add(set);
                     }
                  }
               }

               root.add("__binds", keep);
            }
         }

         JsonElement s = root.get("__serverProfiles");
         if (s != null) {
            if (!s.isJsonObject()) {
               root.remove("__serverProfiles");
            } else {
               JsonObject map = s.getAsJsonObject();

               for (String k : new ArrayList<String>(map.keySet())) {
                  if (str(map.get(k)) == null) {
                     map.remove(k);
                  }
               }
            }
         }
      }
   }

   public static void normalizeSnapshot(JsonObject snap) {
      if (snap != null) {
         for (String k : NEVER_IN_SNAPSHOT) {
            snap.remove(k);
         }

         normalizeModules(snap);
      }
   }

   public static void normalizeModules(JsonObject root) {
      for (String name : new ArrayList<String>(root.keySet())) {
         if (!name.startsWith("__")) {
            if (name.isBlank()) {
               root.remove(name);
            } else {
               root.add(name, normalizeModule(name, root.get(name)));
            }
         }
      }
   }

   public static JsonObject normalizeModule(String name, JsonElement el) {
      JsonObject out;
      if (el != null && el.isJsonObject()) {
         out = el.getAsJsonObject();
      } else {
         out = new JsonObject();
         Boolean b = bool(el);
         if (b != null) {
            out.addProperty("enabled", b);
         }
      }

      Boolean en = bool(out.get("enabled"));
      out.addProperty("enabled", en != null ? en : defaultEnabled(name));
      JsonElement v = out.get("values");
      JsonObject values = v != null && v.isJsonObject() ? v.getAsJsonObject() : new JsonObject();
      coerceValues(name, values);
      out.add("values", values);
      String bind = str(out.get("bind"));
      if (bind != null && !bind.isBlank() && !"null".equals(bind)) {
         out.addProperty("bind", bind);
      } else {
         out.add("bind", JsonNull.INSTANCE);
      }

      String mode = str(out.get("bindMode"));
      out.addProperty("bindMode", "hold".equals(mode) ? "hold" : "toggle");
      return out;
   }

   private static void coerceValues(String module, JsonObject values) {
      GuiData d = data();
      GuiData.Module mod = d == null ? null : d.modules.get(module);
      Map<String, String> types = new HashMap<>();
      if (mod != null) {
         for (GuiData.Setting s : mod.settings) {
            types.put(s.name, s.type);
         }
      }

      for (String key : new ArrayList<String>(values.keySet())) {
         JsonElement e = values.get(key);
         if (e != null && !e.isJsonNull()) {
            String type = types.get(key);
            if (type != null) {
               switch (type) {
                  case "range":
                     if (!e.isJsonPrimitive()) {
                        values.remove(key);
                     } else {
                        JsonPrimitive p = e.getAsJsonPrimitive();
                        if (!p.isNumber()) {
                           try {
                              values.addProperty(key, Double.parseDouble(p.getAsString().trim()));
                           } catch (Exception ex) {
                              values.remove(key);
                           }
                        }
                     }
                     break;
                  case "check":
                     Boolean b = bool(e);
                     if (b == null) {
                        values.remove(key);
                     } else {
                        values.addProperty(key, b);
                     }
                     break;
                  case "select":
                     if (!e.isJsonPrimitive()) {
                        values.remove(key);
                     } else if (!e.getAsJsonPrimitive().isString()) {
                        values.addProperty(key, e.getAsString());
                     }
               }
            }
         } else {
            values.remove(key);
         }
      }
   }

   public static JsonObject snapshot(JsonObject root, Collection<String> knownModules) {
      JsonObject snap = new JsonObject();

      for (Entry<String, JsonElement> e : root.entrySet()) {
         if (!e.getKey().startsWith("__")) {
            snap.add(e.getKey(), e.getValue().deepCopy());
         }
      }

      if (knownModules != null) {
         for (String m : knownModules) {
            if (!snap.has(m)) {
               snap.add(m, defaultModule(m));
            }
         }
      }

      for (String k : SNAPSHOT_EXTRAS) {
         JsonElement e = root.get(k);
         if (e != null && !e.isJsonNull()) {
            snap.add(k, e.deepCopy());
         }
      }

      normalizeSnapshot(snap);
      return snap;
   }

   static Boolean bool(JsonElement e) {
      if (e != null && e.isJsonPrimitive()) {
         JsonPrimitive p = e.getAsJsonPrimitive();
         if (p.isBoolean()) {
            return p.getAsBoolean();
         } else if (p.isNumber()) {
            return p.getAsDouble() != 0.0;
         } else {
            String s = p.getAsString().trim();
            if (s.equalsIgnoreCase("true")) {
               return true;
            } else {
               return s.equalsIgnoreCase("false") ? false : null;
            }
         }
      } else {
         return null;
      }
   }

   static String str(JsonElement e) {
      return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
   }
}

