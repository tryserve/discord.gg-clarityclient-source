package need.clarity.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class LegacyConfigMigration {
   static final String CHINA_HAT = "China Hat";
   static final String HATS = "Hats";
   static final String[] CHINA_HAT_KEYS = new String[]{"Color Mode", "Color Count", "Color 1", "Color 2", "Color 3", "Color 4", "Center Alpha", "Edge Alpha", "Rotation Speed"};

   private LegacyConfigMigration() {
   }

   public static boolean migrateChinaHat(JsonObject root, boolean ensureEnabledKey) {
      if (root == null) {
         return false;
      }

      JsonElement legacyEl = root.remove("China Hat");
      if (legacyEl == null) {
         return false;
      }

      if (!legacyEl.isJsonObject()) {
         return true;
      }

      JsonObject legacy = legacyEl.getAsJsonObject();
      boolean wasEnabled = isTrue(legacy.get("enabled"));
      JsonObject hats = childObject(root, "Hats");
      JsonObject vals = childObject(hats, "values");
      JsonElement oldValsEl = legacy.get("values");
      JsonObject oldVals = oldValsEl != null && oldValsEl.isJsonObject() ? oldValsEl.getAsJsonObject() : null;

      for (String key : CHINA_HAT_KEYS) {
         JsonElement v = oldVals == null ? null : oldVals.get(key);
         boolean stored = v != null && !v.isJsonNull();
         if (wasEnabled) {
            if (stored) {
               vals.add(key, v.deepCopy());
            } else {
               vals.remove(key);
            }
         } else if (stored && !vals.has(key)) {
            vals.add(key, v.deepCopy());
         }
      }

      if (wasEnabled) {
         vals.addProperty("Hat", "China Hat");
         hats.addProperty("enabled", true);
      } else if (ensureEnabledKey && !hats.has("enabled")) {
         hats.addProperty("enabled", false);
      }

      String bind = primitiveString(legacy.get("bind"));
      if (!isBlankBind(bind) && isBlankBind(primitiveString(hats.get("bind")))) {
         hats.addProperty("bind", bind);
      }

      return true;
   }

   private static JsonObject childObject(JsonObject parent, String name) {
      JsonElement el = parent.get(name);
      if (el != null && el.isJsonObject()) {
         return el.getAsJsonObject();
      }

      JsonObject created = new JsonObject();
      parent.add(name, created);
      return created;
   }

   private static boolean isTrue(JsonElement el) {
      if (el != null && el.isJsonPrimitive()) {
         try {
            return el.getAsBoolean();
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static String primitiveString(JsonElement el) {
      return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
   }

   private static boolean isBlankBind(String s) {
      return s == null || s.isEmpty() || "null".equals(s);
   }
}
