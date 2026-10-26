package need.clarity.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import need.clarity.Unload;
import need.clarity.util.ThemeColors;
import net.fabricmc.loader.api.FabricLoader;

public final class FriendManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Object LOCK = new Object();
   private static final List<Friend> friends = new ArrayList<>();
   private static final Map<String, Object> settings = new ConcurrentHashMap<>();
   private static volatile Map<String, Friend> index = Map.of();
   private static volatile List<Friend> snapshot = List.of();
   private static final AtomicLong revision = new AtomicLong();
   private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();
   private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "clarity-friends-writer");
      t.setDaemon(true);
      return t;
   });
   private static volatile ScheduledFuture<?> pendingWrite;
   private static volatile boolean loaded = false;
   private static final List<Runnable> settingListeners = new CopyOnWriteArrayList<>();

   private FriendManager() {
   }

   public static Path file() {
      Path d = FabricLoader.getInstance().getGameDir().resolve("clarity");

      try {
         Files.createDirectories(d);
      } catch (Exception var2) {
      }

      return d.resolve("friends.json");
   }

   public static void load() {
      synchronized (LOCK) {
         friends.clear();
         settings.clear();
         Path p = file();
         if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
               JsonElement el = JsonParser.parseReader(r);
               if (el.isJsonObject()) {
                  readJson(el.getAsJsonObject());
               }
            } catch (Exception e) {
               try {
                  Files.copy(p, p.resolveSibling("friends.broken.json"), StandardCopyOption.REPLACE_EXISTING);
               } catch (Exception var6) {
               }
            }
         }

         loaded = true;
         rebuild();
      }
   }

   private static void readJson(JsonObject root) {
      if (root.has("settings") && root.get("settings").isJsonObject()) {
         for (Entry<String, JsonElement> e : root.getAsJsonObject("settings").entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
               JsonPrimitive v = e.getValue().getAsJsonPrimitive();
               settings.put(e.getKey(), v.isBoolean() ? v.getAsBoolean() : (v.isNumber() ? v.getAsDouble() : v.getAsString()));
            }
         }
      }

      if (root.has("friends") && root.get("friends").isJsonArray()) {
         for (JsonElement fe : root.getAsJsonArray("friends")) {
            if (fe.isJsonObject()) {
               Friend f = Friend.fromJson(fe.getAsJsonObject());
               if (f != null && findLocked(f.uuid(), f.name()) == null) {
                  friends.add(f);
               }
            }
         }
      }
   }

   public static JsonObject toJson() {
      synchronized (LOCK) {
         JsonObject root = new JsonObject();
         root.addProperty("version", 1);
         JsonObject s = new JsonObject();

         for (Entry<String, Object> e : settings.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Boolean b) {
               s.addProperty(e.getKey(), b);
            } else if (v instanceof Number n) {
               s.addProperty(e.getKey(), n);
            } else {
               s.addProperty(e.getKey(), String.valueOf(v));
            }
         }

         root.add("settings", s);
         JsonArray arr = new JsonArray();

         for (Friend f : friends) {
            arr.add(f.toJson());
         }

         root.add("friends", arr);
         return root;
      }
   }

   private static void scheduleWrite() {
      ScheduledFuture<?> prev = pendingWrite;
      if (prev != null) {
         prev.cancel(false);
      }

      pendingWrite = WRITER.schedule(FriendManager::writeNow, 300L, TimeUnit.MILLISECONDS);
   }

   public static void shutdownWriter() {
      try {
         WRITER.shutdown();
      } catch (Throwable var1) {
      }
   }

   public static void flushSync() {
      ScheduledFuture<?> prev = pendingWrite;
      if (prev != null) {
         prev.cancel(false);
      }

      if (loaded) {
         writeNow();
      }
   }

   private static void writeNow() {
      try {
         Path p = file();
         Path tmp = p.resolveSibling("friends.json.tmp");
         Files.writeString(tmp, GSON.toJson(toJson()), StandardCharsets.UTF_8);

         try {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var4) {
      }
   }

   private static void rebuild() {
      Map<String, Friend> m = new HashMap<>();

      for (Friend f : friends) {
         if (f.hasUuid()) {
            m.put("u:" + f.uuid(), f);
         }

         if (!f.name().isEmpty()) {
            m.putIfAbsent("n:" + f.name().toLowerCase(Locale.ROOT), f);
         }
      }

      index = Map.copyOf(m);
      snapshot = List.copyOf(friends);
      revision.incrementAndGet();
   }

   private static void changed(boolean listChanged) {
      rebuild();
      scheduleWrite();
      if (listChanged) {
         for (Runnable r : listeners) {
            try {
               r.run();
            } catch (Throwable var4) {
            }
         }
      }
   }

   public static void addListener(Runnable r) {
      listeners.add(r);
   }

   public static long revision() {
      return revision.get();
   }

   private static Friend findLocked(String uuid, String name) {
      String u = Friend.normUuid(uuid);

      for (Friend f : friends) {
         if (u != null && u.equals(f.uuid())) {
            return f;
         }
      }

      if (name != null && !name.isBlank()) {
         for (Friend f : friends) {
            if (f.name().equalsIgnoreCase(name.trim()) && (u == null || !f.hasUuid())) {
               return f;
            }
         }
      }

      return null;
   }

   public static List<Friend> all() {
      return snapshot;
   }

   public static int count() {
      return snapshot.size();
   }

   public static Friend get(String uuid, String name) {
      Map<String, Friend> m = index;
      String u = Friend.normUuid(uuid);
      if (u != null) {
         Friend f = m.get("u:" + u);
         if (f != null) {
            return f;
         }
      }

      if (name != null && !name.isEmpty()) {
         Friend f = m.get("n:" + name.toLowerCase(Locale.ROOT));
         if (f == null) {
            return null;
         } else {
            return u != null && f.hasUuid() && !u.equals(f.uuid()) ? null : f;
         }
      } else {
         return null;
      }
   }

   public static Friend byKey(String key) {
      if (key == null) {
         return null;
      } else {
         return key.startsWith("name:") ? get(null, key.substring(5)) : get(key, null);
      }
   }

   public static boolean isFriend(String uuid, String name) {
      return enabled() && get(uuid, name) != null;
   }

   public static FriendManager.AddResult add(String uuid, String name) {
      if (uuid != null && !uuid.isBlank() || name != null && validName(name)) {
         synchronized (LOCK) {
            Friend existing = findLocked(uuid, name);
            if (existing != null) {
               boolean learned = false;
               if (!existing.hasUuid() && uuid != null && !uuid.isBlank()) {
                  existing.setUuid(uuid);
                  learned = true;
               }

               if (name != null && !name.isBlank() && !existing.name().equals(name)) {
                  existing.setName(name);
               }

               if (learned) {
                  changed(true);
               }

               return FriendManager.AddResult.ALREADY;
            } else {
               friends.add(new Friend(uuid, name, System.currentTimeMillis()));
               changed(true);
               return FriendManager.AddResult.ADDED;
            }
         }
      } else {
         return FriendManager.AddResult.INVALID;
      }
   }

   public static boolean remove(String uuid, String name) {
      synchronized (LOCK) {
         Friend f = findLocked(uuid, name);
         if (f == null) {
            return false;
         }

         friends.remove(f);
         changed(true);
         return true;
      }
   }

   public static boolean removeByKey(String key) {
      Friend f = byKey(key);
      return f != null && remove(f.uuid(), f.name());
   }

   public static boolean toggle(String uuid, String name) {
      if (get(uuid, name) != null) {
         remove(uuid, name);
         return false;
      } else {
         return add(uuid, name) != FriendManager.AddResult.INVALID;
      }
   }

   public static void clear() {
      synchronized (LOCK) {
         friends.clear();
         changed(true);
      }
   }

   public static void observe(String uuid, String name) {
      if (uuid != null && name != null && !name.isEmpty()) {
         Friend f = get(uuid, name);
         if (f != null) {
            boolean needUuid = !f.hasUuid();
            boolean renamed = f.hasUuid() && !f.name().equals(name);
            if (needUuid || renamed) {
               synchronized (LOCK) {
                  if (needUuid) {
                     f.setUuid(uuid);
                  }

                  f.setName(name);
                  changed(needUuid);
               }
            }
         }
      }
   }

   public static void setFriendOption(String key, String option, Object value) {
      Friend f = byKey(key);
      if (f != null) {
         synchronized (LOCK) {
            f.setOption(option, value);
            changed(false);
         }
      }
   }

   public static void resetFriend(String key) {
      Friend f = byKey(key);
      if (f != null) {
         synchronized (LOCK) {
            f.resetOptions();
            changed(false);
         }
      }
   }

   public static Object setting(String key) {
      Object v = settings.get(key);
      return v != null ? v : FriendSettings.globalDefault(key);
   }

   public static boolean bool(String key) {
      if (Unload.isUnloaded()) {
         return false;
      } else {
         Object v = setting(key);
         if (v instanceof Boolean b) {
            return b;
         } else if (v instanceof String s) {
            return Boolean.parseBoolean(s);
         } else {
            return v instanceof Number n ? n.doubleValue() != 0.0 : false;
         }
      }
   }

   public static double num(String key, double def) {
      Object v = setting(key);
      if (v instanceof Number n) {
         return n.doubleValue();
      } else {
         try {
            return Double.parseDouble(String.valueOf(v));
         } catch (Exception e) {
            return def;
         }
      }
   }

   public static String str(String key) {
      Object v = setting(key);
      return v == null ? "" : String.valueOf(v);
   }

   public static void setSetting(String key, Object value) {
      if (value == null) {
         settings.remove(key);
      } else {
         settings.put(key, value);
      }

      revision.incrementAndGet();
      scheduleWrite();

      for (Runnable r : settingListeners) {
         try {
            r.run();
         } catch (Throwable var5) {
         }
      }
   }

   public static void addSettingListener(Runnable r) {
      settingListeners.add(r);
   }

   public static boolean enabled() {
      return bool("Friends Enabled");
   }

   public static boolean isProtected(String uuid, String name) {
      if (enabled() && bool("Protect Friends")) {
         Friend f = get(uuid, name);
         return f != null && f.bool("Protect");
      } else {
         return false;
      }
   }

   public static boolean avoidExplosions(String uuid, String name) {
      if (enabled() && bool("Protect Friends") && bool("Explosion Safety")) {
         Friend f = get(uuid, name);
         return f != null && f.bool("Protect") && f.bool("Avoid Explosion Damage");
      } else {
         return false;
      }
   }

   public static boolean blockHits(String uuid, String name) {
      if (!enabled()) {
         return false;
      }

      Friend f = get(uuid, name);
      return f != null && f.bool("Block My Hits");
   }

   public static int nameColour(Friend f) {
      return f != null && enabled() && bool("Recolour Names") && f.bool("Colour Name") ? parseRgb(f.str("Name Colour"), 5636095) : -1;
   }

   public static int espColour(String uuid, String name) {
      if (enabled() && bool("ESP Colours")) {
         Friend f = get(uuid, name);
         return f != null && f.bool("Colour In ESP") ? parseRgb(f.str("Name Colour"), 5636095) : -1;
      } else {
         return -1;
      }
   }

   public static String tag(Friend f) {
      return f != null && f.bool("Show Tag") ? str("Friend Tag").trim() : "";
   }

   public static int parseRgb(String s, int fallback) {
      if (s == null) {
         return fallback;
      }

      String t = s.trim();
      if (!t.equalsIgnoreCase("theme") && !t.equalsIgnoreCase("theme2")) {
         if (t.startsWith("#")) {
            t = t.substring(1);
         }

         if (t.length() == 8) {
            t = t.substring(2);
         }

         if (t.length() == 3) {
            t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
         }

         try {
            return Integer.parseInt(t, 16) & 16777215;
         } catch (Exception e) {
            return fallback;
         }
      } else {
         try {
            return ThemeColors.resolveArgb(t, 0xFF000000 | fallback, t.equalsIgnoreCase("theme2")) & 16777215;
         } catch (Throwable e) {
            return fallback;
         }
      }
   }

   public static boolean validName(String name) {
      if (name == null) {
         return false;
      }

      String n = name.trim();
      return n.length() >= 1 && n.length() <= 16 && n.matches("[A-Za-z0-9_]+");
   }

   public static List<Friend> sorted(Predicate<Friend> online) {
      List<Friend> l = new ArrayList<>(snapshot);
      Comparator<Friend> byName = Comparator.comparing(f -> f.displayName().toLowerCase(Locale.ROOT));

      Comparator<Friend> cmp = switch (str("Sort By")) {
         case "Name" -> byName;
         case "Recently Added" -> Comparator.comparingLong(Friend::addedAt).reversed();
         case "Online First" -> Comparator.<Friend, Boolean>comparing(f -> !online.test(f)).thenComparing(byName);
         default -> Comparator.<Friend, Boolean>comparing(f -> !f.favourite()).thenComparing(f -> !online.test(f)).thenComparing(byName);
      };
      l.sort(cmp);
      return l;
   }

   public enum AddResult {
      ADDED,
      ALREADY,
      INVALID;
   }
}
