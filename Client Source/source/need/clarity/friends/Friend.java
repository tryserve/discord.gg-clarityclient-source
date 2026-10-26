package need.clarity.friends;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class Friend {
   private volatile String uuid;
   private volatile String name;
   private final long addedAt;
   private final Map<String, Object> options = new ConcurrentHashMap<>();

   Friend(String uuid, String name, long addedAt) {
      this.uuid = normUuid(uuid);
      this.name = name == null ? "" : name.trim();
      this.addedAt = addedAt;
   }

   public String uuid() {
      return this.uuid;
   }

   public String name() {
      return this.name;
   }

   public long addedAt() {
      return this.addedAt;
   }

   public boolean hasUuid() {
      return this.uuid != null && !this.uuid.isEmpty();
   }

   void setUuid(String u) {
      this.uuid = normUuid(u);
   }

   void setName(String n) {
      if (n != null && !n.isBlank()) {
         this.name = n.trim();
      }
   }

   public String key() {
      return this.hasUuid() ? this.uuid : "name:" + this.name.toLowerCase(Locale.ROOT);
   }

   public Object option(String key) {
      Object v = this.options.get(key);
      return v != null ? v : FriendSettings.perFriendDefault(key);
   }

   public boolean bool(String key) {
      Object v = this.option(key);
      if (v instanceof Boolean b) {
         return b;
      } else if (v instanceof String s) {
         return Boolean.parseBoolean(s);
      } else {
         return v instanceof Number n ? n.doubleValue() != 0.0 : false;
      }
   }

   public String str(String key) {
      Object v = this.option(key);
      return v == null ? "" : String.valueOf(v);
   }

   void setOption(String key, Object value) {
      if (value == null) {
         this.options.remove(key);
      } else {
         this.options.put(key, value);
      }
   }

   void resetOptions() {
      this.options.clear();
   }

   public String displayName() {
      String nick = this.str("Nickname").trim();
      return !nick.isEmpty() && this.bool("Use Nickname") ? nick : this.name;
   }

   public boolean favourite() {
      return this.bool("Favourite");
   }

   JsonObject toJson() {
      JsonObject o = new JsonObject();
      if (this.hasUuid()) {
         o.addProperty("uuid", this.uuid);
      }

      o.addProperty("name", this.name);
      o.addProperty("addedAt", this.addedAt);
      JsonObject opts = new JsonObject();

      for (Entry<String, Object> e : this.options.entrySet()) {
         Object v = e.getValue();
         if (v instanceof Boolean b) {
            opts.addProperty(e.getKey(), b);
         } else if (v instanceof Number n) {
            opts.addProperty(e.getKey(), n);
         } else {
            opts.addProperty(e.getKey(), String.valueOf(v));
         }
      }

      o.add("options", opts);
      return o;
   }

   static Friend fromJson(JsonObject o) {
      String uuid = o.has("uuid") && !o.get("uuid").isJsonNull() ? o.get("uuid").getAsString() : null;
      String name = o.has("name") ? o.get("name").getAsString() : "";
      long added = o.has("addedAt") ? o.get("addedAt").getAsLong() : System.currentTimeMillis();
      if ((uuid == null || uuid.isEmpty()) && name.isBlank()) {
         return null;
      }

      Friend f = new Friend(uuid, name, added);
      if (o.has("options") && o.get("options").isJsonObject()) {
         for (Entry<String, JsonElement> e : o.getAsJsonObject("options").entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
               JsonPrimitive p = e.getValue().getAsJsonPrimitive();
               Object v = p.isBoolean() ? p.getAsBoolean() : (p.isNumber() ? p.getAsDouble() : p.getAsString());
               f.options.put(e.getKey(), v);
            }
         }
      }

      return f;
   }

   static String normUuid(String u) {
      if (u == null) {
         return null;
      }

      String s = u.trim().toLowerCase(Locale.ROOT);
      if (s.isEmpty()) {
         return null;
      }

      if (s.length() == 32 && s.indexOf(45) < 0) {
         s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
      }

      return s;
   }
}
