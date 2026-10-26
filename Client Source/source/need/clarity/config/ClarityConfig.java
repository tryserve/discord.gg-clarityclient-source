package need.clarity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import need.clarity.Clarity;
import need.clarity.clickgui.GuiData;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.server.ClarityServer;
import net.fabricmc.loader.api.FabricLoader;

public class ClarityConfig {
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getGameDir().resolve("clarity_config.json");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static volatile JsonObject current = new JsonObject();
   private static volatile String cachedJson = "{}";
   private static final ConcurrentHashMap<String, Boolean> moduleEnabled = new ConcurrentHashMap<>();
   private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "clarity-config-writer");
      t.setDaemon(true);
      return t;
   });
   private static volatile ScheduledFuture<?> pendingWrite = null;
   private static final String[] GAME_OWNED = new String[]{"__activeProfile", "__rev"};
   private static final Map<String, Map<String, Object>> recentLayout = new HashMap<>();
   private static volatile long recentLayoutAt = 0L;
   private static final long LAYOUT_PROTECT_MS = 2000L;
   private static volatile boolean frozen = false;

   public static void load() {
      if (Files.exists(CONFIG_PATH)) {
         try (Reader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(r);
            if (el.isJsonObject()) {
               JsonObject loaded = el.getAsJsonObject();
               String before = GSON.toJson(loaded);
               LegacyConfigMigration.migrateChinaHat(loaded, true);
               ConfigSchema.normalizeRoot(loaded);
               current = loaded;
               rebuildLiveState();
               cachedJson = GSON.toJson(current);
               if (!cachedJson.equals(before)) {
                  scheduleWrite();
               }
            }
         } catch (Exception var6) {
         }
      }
   }

   private static void rebuildLiveState() {
      moduleEnabled.clear();

      for (Entry<String, JsonElement> entry : current.entrySet()) {
         String name = entry.getKey();
         if (!name.startsWith("__")) {
            JsonElement el = entry.getValue();
            if (el.isJsonObject()) {
               JsonElement en = el.getAsJsonObject().get("enabled");
               Boolean b = ConfigSchema.bool(en);
               moduleEnabled.put(name, b != null ? b : ConfigSchema.defaultEnabled(name));
            }
         }
      }
   }

   public static JsonObject getCurrent() {
      return current;
   }

   public static String getCurrentJson() {
      return cachedJson;
   }

   public static synchronized void update(String jsonBody) {
      try {
         JsonElement el = JsonParser.parseString(jsonBody);
         if (!el.isJsonObject()) {
            return;
         }

         JsonObject incoming = el.getAsJsonObject();
         JsonObject old = current;

         for (Entry<String, JsonElement> e : old.entrySet()) {
            String k = e.getKey();
            if (!k.startsWith("__") && !incoming.has(k)) {
               incoming.add(k, e.getValue().deepCopy());
            }
         }

         for (String k : GAME_OWNED) {
            if (!incoming.has(k) && old.has(k)) {
               incoming.add(k, old.get(k).deepCopy());
            }
         }

         LegacyConfigMigration.migrateChinaHat(incoming, true);
         ConfigSchema.normalizeRoot(incoming);
         current = incoming;
         reapplyRecentLayout(current);
         rebuildLiveState();
         cachedJson = GSON.toJson(current);
         scheduleWrite();

         try {
            ModuleRegistry.syncFromConfig();
         } catch (Throwable var8) {
         }

         guiModeMayHaveChanged();
      } catch (Exception var9) {
      }
   }

   public static synchronized void mutate(Consumer<JsonObject> edit) {
      try {
         edit.accept(current);
         current.addProperty("__rev", System.currentTimeMillis());
         rebuildLiveState();
         cachedJson = GSON.toJson(current);
         scheduleWrite();
      } catch (Exception var2) {
      }

      guiModeMayHaveChanged();
   }

   private static void guiModeMayHaveChanged() {
      try {
         ClarityServer.sync();
      } catch (Throwable var1) {
      }
   }

   public static boolean isEnabled(String moduleName) {
      Boolean b = moduleEnabled.get(moduleName);
      return b != null ? b : ConfigSchema.defaultEnabled(moduleName);
   }

   public static boolean webGuiActive() {
      return "Web".equals(getSetting("Click GUI", "GUI"));
   }

   public static void setModuleEnabled(String moduleName, boolean enabled) {
      moduleEnabled.put(moduleName, enabled);
      JsonElement el = current.get(moduleName);
      JsonObject mod;
      if (el != null && el.isJsonObject()) {
         mod = el.getAsJsonObject();
      } else {
         mod = ConfigSchema.defaultModule(moduleName);
         current.add(moduleName, mod);
      }

      mod.addProperty("enabled", enabled);
      cachedJson = GSON.toJson(current);
      scheduleWrite();
   }

   public static int valuesHash(String moduleName) {
      try {
         JsonElement modEl = current.get(moduleName);
         if (modEl != null && modEl.isJsonObject()) {
            JsonElement valsEl = modEl.getAsJsonObject().get("values");
            return valsEl == null ? 0 : valsEl.hashCode();
         } else {
            return 0;
         }
      } catch (Exception e) {
         return 0;
      }
   }

   public static Object getSetting(String moduleName, String key) {
      try {
         JsonElement modEl = current.get(moduleName);
         if (modEl != null && modEl.isJsonObject()) {
            JsonElement valsEl = modEl.getAsJsonObject().get("values");
            if (valsEl != null && valsEl.isJsonObject()) {
               JsonElement el = valsEl.getAsJsonObject().get(key);
               if (el != null && !el.isJsonNull()) {
                  JsonPrimitive p = el.getAsJsonPrimitive();
                  if (p.isBoolean()) {
                     return p.getAsBoolean();
                  } else {
                     return p.isNumber() ? p.getAsDouble() : p.getAsString();
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } catch (Exception e) {
         return null;
      }
   }

   public static String getString(String moduleName, String key, String defaultValue) {
      try {
         JsonObject mod = current.getAsJsonObject(moduleName);
         JsonObject vals = mod.getAsJsonObject("values");
         JsonElement v = vals.get(key);
         return v != null && v.isJsonPrimitive() ? v.getAsString() : defaultValue;
      } catch (Exception e) {
         return defaultValue;
      }
   }

   public static void putSetting(String moduleName, String key, Object value) {
      try {
         JsonElement modEl = current.get(moduleName);
         JsonObject mod;
         if (modEl != null && modEl.isJsonObject()) {
            mod = modEl.getAsJsonObject();
         } else {
            mod = ConfigSchema.defaultModule(moduleName);
            current.add(moduleName, mod);
         }

         JsonElement valsEl = mod.get("values");
         JsonObject vals;
         if (valsEl != null && valsEl.isJsonObject()) {
            vals = valsEl.getAsJsonObject();
         } else {
            vals = new JsonObject();
            mod.add("values", vals);
         }

         if (value instanceof Boolean b) {
            vals.addProperty(key, b);
         } else if (value instanceof Number n) {
            vals.addProperty(key, n);
         } else {
            vals.addProperty(key, value.toString());
         }

         cachedJson = GSON.toJson(current);
         scheduleWrite();
      } catch (Exception var9) {
      }
   }

   public static void putSettings(String moduleName, Map<String, Object> values, boolean layout) {
      if (values != null && !values.isEmpty()) {
         try {
            writeValues(current, moduleName, values);
            if (layout) {
               synchronized (recentLayout) {
                  recentLayout.computeIfAbsent(moduleName, k -> new HashMap<>()).putAll(values);
                  recentLayoutAt = System.currentTimeMillis();
               }
            }

            cachedJson = GSON.toJson(current);
            scheduleWrite();
         } catch (Exception var6) {
         }
      }
   }

   private static void writeValues(JsonObject root, String moduleName, Map<String, Object> values) {
      JsonElement modEl = root.get(moduleName);
      JsonObject mod;
      if (modEl != null && modEl.isJsonObject()) {
         mod = modEl.getAsJsonObject();
      } else {
         mod = ConfigSchema.defaultModule(moduleName);
         root.add(moduleName, mod);
      }

      JsonElement valsEl = mod.get("values");
      JsonObject vals;
      if (valsEl != null && valsEl.isJsonObject()) {
         vals = valsEl.getAsJsonObject();
      } else {
         vals = new JsonObject();
         mod.add("values", vals);
      }

      for (Entry<String, Object> e : values.entrySet()) {
         Object v = e.getValue();
         if (v != null) {
            if (v instanceof Boolean b) {
               vals.addProperty(e.getKey(), b);
            } else if (v instanceof Number n) {
               vals.addProperty(e.getKey(), n);
            } else {
               vals.addProperty(e.getKey(), v.toString());
            }
         }
      }
   }

   private static void reapplyRecentLayout(JsonObject root) {
      synchronized (recentLayout) {
         if (!recentLayout.isEmpty()) {
            if (System.currentTimeMillis() - recentLayoutAt > 2000L) {
               recentLayout.clear();
            } else {
               for (Entry<String, Map<String, Object>> e : recentLayout.entrySet()) {
                  writeValues(root, e.getKey(), e.getValue());
               }
            }
         }
      }
   }

   public static void setActiveProfile(String name) {
      try {
         current.addProperty("__activeProfile", name == null ? "" : name);
         cachedJson = GSON.toJson(current);
         scheduleWrite();
      } catch (Exception var2) {
      }
   }

   public static void applyProfileSnapshot(JsonObject snapshot) {
      if (snapshot != null) {
         try {
            snapshot = snapshot.deepCopy();
            LegacyConfigMigration.migrateChinaHat(snapshot, false);
            ConfigSchema.normalizeSnapshot(snapshot);

            for (String k : new String[]{"__customCapes", "__customSkins"}) {
               JsonElement a = snapshot.get(k);
               if (a != null && a.isJsonArray()) {
                  current.add(k, a.deepCopy());
               }
            }

            for (Entry<String, JsonElement> entry : snapshot.entrySet()) {
               String module = entry.getKey();
               if (module != null && !module.isEmpty() && !module.startsWith("__") && !"Click GUI".equals(module)) {
                  JsonElement el = entry.getValue();
                  if (el != null && el.isJsonObject()) {
                     JsonObject snap = el.getAsJsonObject();
                     JsonObject merged = new JsonObject();

                     for (Entry<String, JsonElement> se : snap.entrySet()) {
                        JsonElement v = se.getValue();
                        merged.add(se.getKey(), (JsonElement)(v == null ? JsonNull.INSTANCE : v.deepCopy()));
                     }

                     JsonElement curEl = current.get(module);
                     if (curEl != null && curEl.isJsonObject()) {
                        for (Entry<String, JsonElement> ce : curEl.getAsJsonObject().entrySet()) {
                           if (!merged.has(ce.getKey())) {
                              JsonElement v = ce.getValue();
                              merged.add(ce.getKey(), (JsonElement)(v == null ? JsonNull.INSTANCE : v.deepCopy()));
                           }
                        }
                     }

                     JsonElement en = snap.get("enabled");
                     if (en != null && en.isJsonPrimitive()) {
                        boolean enabled;
                        try {
                           enabled = en.getAsBoolean();
                        } catch (Exception ex) {
                           enabled = isEnabled(module);
                        }

                        moduleEnabled.put(module, enabled);
                        merged.addProperty("enabled", enabled);
                     } else if (!merged.has("enabled")) {
                        merged.addProperty("enabled", isEnabled(module));
                     }

                     current.add(module, merged);
                  }
               }
            }

            JsonObject snap = snapshot;
            Set<String> all = new LinkedHashSet<>();

            for (String k : current.keySet()) {
               if (!k.startsWith("__")) {
                  all.add(k);
               }
            }

            try {
               all.addAll(GuiData.get().modules.keySet());
            } catch (Throwable var11) {
            }

            try {
               for (ModuleBase m : ModuleRegistry.all()) {
                  all.add(m.getName());
               }
            } catch (Throwable var13) {
            }

            for (String module : all) {
               if (!"Click GUI".equals(module) && !snap.has(module)) {
                  JsonElement curEl = current.get(module);
                  JsonObject mod = curEl != null && curEl.isJsonObject() ? curEl.getAsJsonObject() : ConfigSchema.defaultModule(module);
                  mod.addProperty("enabled", false);
                  current.add(module, mod);
                  moduleEnabled.put(module, false);
               }
            }

            current.addProperty("__rev", System.currentTimeMillis());
            rebuildLiveState();
            cachedJson = GSON.toJson(current);
            scheduleWrite();
         } catch (Exception var14) {
         }
      }
   }

   public static JsonObject getBlockColors(String moduleName) {
      try {
         JsonElement modEl = current.get(moduleName);
         if (modEl != null && modEl.isJsonObject()) {
            JsonElement colorsEl = modEl.getAsJsonObject().get("blockColors");
            return colorsEl != null && colorsEl.isJsonObject() ? colorsEl.getAsJsonObject() : new JsonObject();
         } else {
            return new JsonObject();
         }
      } catch (Exception e) {
         return new JsonObject();
      }
   }

   public static Map<String, String> getKeybinds() {
      Map<String, String> binds = new HashMap<>();

      for (Entry<String, JsonElement> entry : current.entrySet()) {
         String name = entry.getKey();
         if (!name.startsWith("__")) {
            JsonElement el = entry.getValue();
            if (el.isJsonObject() && isRegisteredModule(name)) {
               JsonElement bindEl = el.getAsJsonObject().get("bind");
               if (bindEl != null && bindEl.isJsonPrimitive()) {
                  String bind = bindEl.getAsString();
                  if (bind != null && !bind.isEmpty() && !bind.equals("null")) {
                     binds.put(name, bind);
                  }
               }
            }
         }
      }

      return binds;
   }

   private static boolean isRegisteredModule(String name) {
      List<ModuleBase> all = ModuleRegistry.all();
      return all.isEmpty() || ModuleRegistry.get(name) != null;
   }

   public static void pruneStaleBinds() {
      if (!ModuleRegistry.all().isEmpty()) {
         List<String> stale = new ArrayList<>();

         for (Entry<String, JsonElement> e : current.entrySet()) {
            String name = e.getKey();
            if (!name.startsWith("__") && e.getValue().isJsonObject() && ModuleRegistry.get(name) == null) {
               JsonElement b = e.getValue().getAsJsonObject().get("bind");
               if (b != null && !b.isJsonNull()) {
                  stale.add(name);
               }
            }
         }

         if (!stale.isEmpty()) {
            mutate(root -> {
               for (String namex : stale) {
                  JsonElement el = root.get(namex);
                  if (el != null && el.isJsonObject()) {
                     el.getAsJsonObject().add("bind", JsonNull.INSTANCE);
                  }
               }
            });
            Clarity.LOGGER.info("[Clarity] Cleared binds of removed modules: {}", stale);
         }
      }
   }

   public static void freeze() {
      frozen = true;
      ScheduledFuture<?> p = pendingWrite;
      if (p != null) {
         p.cancel(false);
      }

      SCHEDULER.shutdownNow();
   }

   private static synchronized void scheduleWrite() {
      if (!frozen) {
         if (pendingWrite != null) {
            pendingWrite.cancel(false);
         }

         pendingWrite = SCHEDULER.schedule(ClarityConfig::writeToDisk, 300L, TimeUnit.MILLISECONDS);
      }
   }

   private static void writeToDisk() {
      if (!frozen) {
         try {
            String json = GSON.toJson(current);
            Files.writeString(CONFIG_PATH, json, StandardCharsets.UTF_8);
         } catch (Exception var1) {
         }
      }
   }

   public static void flushSync() {
      if (!frozen) {
         if (pendingWrite != null) {
            pendingWrite.cancel(false);
         }

         writeToDisk();
      }
   }

   public static String getCustomCapeUrl(String capeId) {
      return findCustomAsset("__customCapes", capeId);
   }

   public static String getCustomSkinUrl(String skinId) {
      return findCustomAsset("__customSkins", skinId);
   }

   private static String findCustomAsset(String key, String assetId) {
      try {
         JsonElement el = current.get(key);
         if (el == null || !el.isJsonArray()) {
            return null;
         }

         for (JsonElement item : el.getAsJsonArray()) {
            if (item.isJsonObject()) {
               JsonObject obj = item.getAsJsonObject();
               JsonElement id = obj.get("id");
               if (id != null && assetId.equals(id.getAsString())) {
                  JsonElement url = obj.get("url");
                  return url != null ? url.getAsString() : null;
               }
            }
         }
      } catch (Exception var8) {
      }

      return null;
   }
}
