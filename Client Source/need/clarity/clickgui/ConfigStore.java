package need.clarity.clickgui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import need.clarity.config.ClarityConfig;
import need.clarity.config.ConfigFiles;
import need.clarity.config.ConfigSchema;
import need.clarity.friends.online.CloudConfigs;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.visual.SkinChanger;
import need.clarity.server.ClarityServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ConfigStore implements GuiStore {
   private final GuiData data;
   private static final Map<String, String> THEME_ALIAS = Map.of(
      "OneDark", "Ocean", "Midnight", "Ocean", "Graphite", "Zinc", "Slate", "Zinc", "Fog", "Zinc", "Wheat", "Gold", "Gruvbox", "Gold", "Radioactive", "Lime"
   );
   private List<Path> filesCache = List.of();
   private long filesAt = 0L;

   public ConfigStore(GuiData data) {
      this.data = data;
   }

   private static JsonObject root() {
      return ClarityConfig.getCurrent();
   }

   private static JsonObject obj(JsonObject parent, String key) {
      JsonElement e = parent.get(key);
      if (e != null && e.isJsonObject()) {
         return e.getAsJsonObject();
      }

      JsonObject o = new JsonObject();
      parent.add(key, o);
      return o;
   }

   private static JsonObject peek(JsonObject parent, String key) {
      JsonElement e = parent == null ? null : parent.get(key);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
   }

   private static Object fromJson(JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         if (e.isJsonArray()) {
            List<String> l = new ArrayList<>();

            for (JsonElement x : e.getAsJsonArray()) {
               if (x.isJsonPrimitive()) {
                  l.add(x.getAsString());
               }
            }

            return l;
         } else if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
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
   }

   private static JsonElement toJson(Object v) {
      if (v == null) {
         return JsonNull.INSTANCE;
      } else if (v instanceof Boolean b) {
         return new JsonPrimitive(b);
      } else if (v instanceof Number n) {
         return new JsonPrimitive(n);
      } else if (!(v instanceof List<?> l)) {
         return new JsonPrimitive(String.valueOf(v));
      } else {
         JsonArray a = new JsonArray();

         for (Object o : l) {
            a.add(String.valueOf(o));
         }

         return a;
      }
   }

   @Override
   public boolean isEnabled(String m) {
      ModuleBase live = ModuleRegistry.get(m);
      return live != null ? live.isEnabled() : ClarityConfig.isEnabled(m);
   }

   @Override
   public void setEnabled(String m, boolean on) {
      ModuleBase live = ModuleRegistry.get(m);
      if (live != null) {
         live.setEnabled(on);
      } else {
         ClarityConfig.setModuleEnabled(m, on);
      }

      ClarityConfig.mutate(r -> {});
   }

   @Override
   public Object value(String m, String k) {
      JsonObject vals = peek(peek(root(), m), "values");
      return vals == null ? null : fromJson(vals.get(k));
   }

   @Override
   public void setValue(String m, String k, Object v) {
      ClarityConfig.mutate(r -> obj(obj(r, m), "values").add(k, toJson(v)));
   }

   @Override
   public void resetModule(String m) {
      ClarityConfig.mutate(r -> obj(r, m).add("values", new JsonObject()));
   }

   @Override
   public String bind(String m) {
      JsonObject o = peek(root(), m);
      JsonElement b = o == null ? null : o.get("bind");
      return b != null && !b.isJsonNull() ? b.getAsString() : null;
   }

   @Override
   public String bindMode(String m) {
      JsonObject o = peek(root(), m);
      JsonElement b = o == null ? null : o.get("bindMode");
      return b != null && !b.isJsonNull() ? b.getAsString() : "toggle";
   }

   @Override
   public void setBind(String m, String bind, String mode) {
      ClarityConfig.mutate(r -> {
         JsonObject o = obj(r, m);
         if (bind == null) {
            o.add("bind", JsonNull.INSTANCE);
         } else {
            o.addProperty("bind", bind);
         }

         o.addProperty("bindMode", mode == null ? "toggle" : mode);
      });
   }

   @Override
   public Object gui(String k) {
      return this.value("Click GUI", k);
   }

   @Override
   public void setGui(String k, Object v) {
      ClarityConfig.mutate(r -> {
         JsonObject o = obj(r, "Click GUI");
         if (!o.has("enabled")) {
            o.addProperty("enabled", true);
         }

         obj(o, "values").add(k, toJson(v));
      });
   }

   @Override
   public Map<String, String> theme() {
      Map<String, String> m = new HashMap<>();
      JsonObject t = peek(root(), "__theme");
      if (t != null && t.has("accent")) {
         for (String k : new String[]{"accent", "accent2", "background", "surface", "border", "text", "muted"}) {
            JsonElement e = t.get(k);
            if (e != null && e.isJsonPrimitive()) {
               m.put(k, e.getAsString());
            }
         }

         if (!m.containsKey("background") && m.containsKey("surface")) {
            int s = Theme.hex(m.get("surface"), 1448742);
            m.put("background", Theme.toHex(Theme.mix(0xFF000000 | s, -16777216, 0.45F)));
         }

         return m;
      } else {
         List<String> d = this.data.themes.getOrDefault("TokyoNight", this.data.themes.isEmpty() ? null : this.data.themes.values().iterator().next());
         if (d != null) {
            m.put("accent", d.get(0));
            m.put("accent2", d.get(1));
            m.put("background", d.get(2));
            m.put("surface", d.get(3));
            m.put("border", d.get(4));
         }

         return m;
      }
   }

   @Override
   public String themeName() {
      if (this.gui("Theme") instanceof String s0) {
         String s = THEME_ALIAS.getOrDefault(s0, s0);
         if (this.data.themes.containsKey(s)) {
            return s;
         }
      }

      String acc = this.theme().get("accent");
      if (acc != null) {
         for (Entry<String, List<String>> e : this.data.themes.entrySet()) {
            if (e.getValue().get(0).equalsIgnoreCase(acc)) {
               return e.getKey();
            }
         }
      }

      return "";
   }

   @Override
   public void applyTheme(String name, List<String> c) {
      ClarityConfig.mutate(r -> {
         JsonObject t = obj(r, "__theme");
         if (!t.has("enabled")) {
            t.addProperty("enabled", true);
         }

         t.addProperty("accent", c.get(0));
         t.addProperty("accent2", c.get(1));
         t.addProperty("background", c.get(2));
         t.addProperty("surface", c.get(3));
         t.addProperty("border", c.get(4));
         t.addProperty("chroma", "Chroma".equals(name));
         boolean light = Theme.luma(0xFF000000 | Theme.hex(c.get(3), 0)) > 0.55F;
         t.addProperty("text", light ? "#16181d" : "#ededf0");
         t.addProperty("muted", light ? "#5d6270" : "#8b8b93");
         JsonObject g = obj(r, "Click GUI");
         obj(g, "values").addProperty("Theme", name);
      });
   }

   @Override
   public boolean themeColorsEnabled() {
      JsonObject t = peek(root(), "__theme");
      JsonElement e = t == null ? null : t.get("enabled");
      return e == null || !e.isJsonPrimitive() || e.getAsBoolean();
   }

   @Override
   public void setThemeColorsEnabled(boolean on) {
      ClarityConfig.mutate(r -> obj(r, "__theme").addProperty("enabled", on));
   }

   @Override
   public List<String> profiles() {
      JsonObject p = peek(root(), "__profiles");
      return p == null ? new ArrayList<>() : new ArrayList<>(p.keySet());
   }

   @Override
   public String activeProfile() {
      JsonElement e = root().get("__activeProfile");
      return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
   }

   @Override
   public void loadProfile(String name) {
      JsonObject p = peek(root(), "__profiles");
      JsonObject snap = p == null ? null : peek(p, name);
      if (snap != null) {
         ClarityConfig.applyProfileSnapshot(snap);
         ClarityConfig.setActiveProfile(name);
         ClarityConfig.mutate(r -> {});

         try {
            ModuleRegistry.syncFromConfig();
         } catch (Throwable var5) {
         }
      }
   }

   @Override
   public void saveProfile(String name) {
      ClarityConfig.mutate(r -> {
         JsonObject snap = ConfigSchema.snapshot(r, this.data.modules.keySet());
         obj(r, "__profiles").add(name, snap);
         r.addProperty("__activeProfile", name);
      });
      CloudConfigs.onProfileSaved(name);
   }

   @Override
   public void deleteProfile(String name) {
      ClarityConfig.mutate(r -> {
         obj(r, "__profiles").remove(name);
         if (name.equals(this.activeProfile())) {
            r.addProperty("__activeProfile", "");
         }
      });
      CloudConfigs.onProfileDeleted(name);
   }

   private static JsonArray bindArr(JsonObject r) {
      JsonElement e = r.get("__binds");
      if (e != null && e.isJsonArray()) {
         return e.getAsJsonArray();
      }

      JsonArray a = new JsonArray();
      r.add("__binds", a);
      return a;
   }

   static String nameOf(JsonElement x) {
      JsonElement n = x.isJsonObject() ? x.getAsJsonObject().get("name") : null;
      return n != null && n.isJsonPrimitive() ? n.getAsString() : null;
   }

   @Override
   public List<String> bindSets() {
      List<String> out = new ArrayList<>();
      JsonElement e = root().get("__binds");
      if (e != null && e.isJsonArray()) {
         for (JsonElement x : e.getAsJsonArray()) {
            if (x.isJsonObject() && x.getAsJsonObject().has("name")) {
               out.add(x.getAsJsonObject().get("name").getAsString());
            }
         }
      }

      return out;
   }

   @Override
   public void loadBindSet(String name) {
      ClarityConfig.mutate(r -> {
         for (JsonElement x : bindArr(r)) {
            if (x.isJsonObject() && name.equals(nameOf(x))) {
               JsonObject binds = peek(x.getAsJsonObject(), "binds");
               if (binds == null) {
                  return;
               }

               for (Entry<String, JsonElement> b : binds.entrySet()) {
                  if (b.getValue().isJsonObject()) {
                     JsonObject mod = obj(r, b.getKey());
                     JsonObject bo = b.getValue().getAsJsonObject();
                     mod.add("bind", (JsonElement)(bo.has("bind") ? bo.get("bind").deepCopy() : JsonNull.INSTANCE));
                     mod.add("bindMode", (JsonElement)(bo.has("bindMode") ? bo.get("bindMode").deepCopy() : new JsonPrimitive("toggle")));
                  }
               }
            }
         }
      });
   }

   @Override
   public void saveBindSet(String name) {
      ClarityConfig.mutate(r -> {
         JsonObject binds = new JsonObject();

         for (String m : this.data.modules.keySet()) {
            JsonObject mod = peek(r, m);
            if (mod != null) {
               JsonObject b = new JsonObject();
               b.add("bind", (JsonElement)(mod.has("bind") ? mod.get("bind").deepCopy() : JsonNull.INSTANCE));
               b.add("bindMode", (JsonElement)(mod.has("bindMode") ? mod.get("bindMode").deepCopy() : new JsonPrimitive("toggle")));
               binds.add(m, b);
            }
         }

         JsonArray arr = bindArr(r);

         for (JsonElement x : arr) {
            if (x.isJsonObject() && name.equals(nameOf(x))) {
               x.getAsJsonObject().add("binds", binds);
               return;
            }
         }

         JsonObject set = new JsonObject();
         set.addProperty("id", "b_" + System.currentTimeMillis());
         set.addProperty("name", name);
         set.addProperty("locked", false);
         set.add("binds", binds);
         arr.add(set);
      });
   }

   @Override
   public void deleteBindSet(String name) {
      ClarityConfig.mutate(r -> {
         JsonArray arr = bindArr(r);
         JsonArray keep = new JsonArray();

         for (JsonElement x : arr) {
            if (!x.isJsonObject() || !name.equals(nameOf(x))) {
               keep.add(x);
            }
         }

         r.add("__binds", keep);
      });
   }

   @Override
   public Map<String, String> serverProfiles() {
      Map<String, String> m = new LinkedHashMap<>();
      JsonObject s = peek(root(), "__serverProfiles");
      if (s != null) {
         for (Entry<String, JsonElement> e : s.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
               m.put(e.getKey(), e.getValue().getAsString());
            }
         }
      }

      return m;
   }

   @Override
   public void mapServer(String a, String p) {
      ClarityConfig.mutate(r -> obj(r, "__serverProfiles").addProperty(a, p));
   }

   @Override
   public void unmapServer(String a) {
      ClarityConfig.mutate(r -> obj(r, "__serverProfiles").remove(a));
   }

   @Override
   public String currentServer() {
      try {
         ServerData sd = Minecraft.getInstance().getCurrentServer();
         if (sd != null && sd.ip != null) {
            return sd.ip;
         }
      } catch (Throwable var2) {
      }

      return "";
   }

   private static List<String> assetIds(String key) {
      List<String> out = new ArrayList<>();
      JsonElement e = root().get(key);
      if (e != null && e.isJsonArray()) {
         for (JsonElement x : e.getAsJsonArray()) {
            if (x.isJsonObject() && x.getAsJsonObject().has("id")) {
               out.add(x.getAsJsonObject().get("id").getAsString());
            }
         }
      }

      return out;
   }

   @Override
   public List<String> customSkins() {
      List<String> out = assetIds("__customSkins");

      try (Stream<Path> files = Files.list(ConfigFiles.skinsDir())) {
         files.filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).sorted().forEach(f -> out.add("file:" + f.getFileName()));
      } catch (Exception var7) {
      }

      return out;
   }

   @Override
   public String addCustomSkin(Path png) throws Exception {
      byte[] bytes = Files.readAllBytes(png);
      if (bytes.length > 2097152) {
         throw new IllegalArgumentException("That file is too big for a skin");
      } else {
         int[] size = SkinChanger.pngSize(bytes);
         if (size == null) {
            throw new IllegalArgumentException("That isn't a PNG image");
         } else if (size[0] == 64 && (size[1] == 64 || size[1] == 32)) {
            String id = "custom_" + System.currentTimeMillis();
            String name = png.getFileName().toString().replaceAll("\\.[^.]+$", "").replace('_', ' ');
            JsonObject asset = new JsonObject();
            asset.addProperty("id", id);
            asset.addProperty("name", name);
            asset.addProperty("url", "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
            asset.addProperty("category", "Custom");
            ClarityConfig.mutate(r -> {
               JsonElement a = r.get("__customSkins");
               JsonArray arr = a != null && a.isJsonArray() ? a.getAsJsonArray() : new JsonArray();
               arr.add(asset);
               r.add("__customSkins", arr);
            });
            return id;
         } else {
            throw new IllegalArgumentException("Skins must be 64x64 or 64x32 (that one is " + size[0] + "x" + size[1] + ")");
         }
      }
   }

   @Override
   public String assetName(String id) {
      if (id == null) {
         return null;
      }

      if (id.startsWith("file:")) {
         return id.substring(5).replaceAll("(?i)\\.png$", "");
      }

      for (String key : new String[]{"__customSkins", "__customCapes"}) {
         JsonElement e = root().get(key);
         if (e != null && e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
               if (x.isJsonObject() && x.getAsJsonObject().has("id") && id.equals(x.getAsJsonObject().get("id").getAsString()) && x.getAsJsonObject().has("name")) {
                  return x.getAsJsonObject().get("name").getAsString();
               }
            }
         }
      }

      return null;
   }

   @Override
   public List<String> customCapes() {
      return assetIds("__customCapes");
   }

   @Override
   public String exportProfile(String name) throws Exception {
      JsonObject p = peek(root(), "__profiles");
      JsonObject snap = p == null ? null : peek(p, name);
      if (snap == null) {
         throw new IllegalArgumentException("No profile called " + name);
      } else {
         return ConfigFiles.export(name, snap).getFileName().toString();
      }
   }

   @Override
   public String importProfile(Path file, boolean load) {
      ConfigFiles.Imported imp = ConfigFiles.read(file);
      JsonObject existing = peek(root(), "__profiles");
      String name = imp.name();
      boolean same = existing != null && existing.get(name) != null && existing.get(name).equals(imp.snapshot());
      if (!same) {
         name = ConfigFiles.uniqueName(existing == null ? Set.of() : existing.keySet(), name);
      }

      String finalName = name;
      if (!same) {
         ClarityConfig.mutate(r -> obj(r, "__profiles").add(finalName, imp.snapshot()));
      }

      if (load) {
         this.loadProfile(finalName);
      }

      this.filesAt = 0L;
      return finalName;
   }

   @Override
   public List<Path> configFiles() {
      long now = System.currentTimeMillis();
      if (now - this.filesAt > 1500L) {
         this.filesCache = ConfigFiles.list();
         this.filesAt = now;
      }

      return this.filesCache;
   }

   @Override
   public void openConfigFolder(Consumer<Boolean> done) {
      ConfigFiles.openFolder(done);
   }

   @Override
   public String configFolderPath() {
      return ConfigFiles.dir().toAbsolutePath().normalize().toString();
   }

   @Override
   public void setClipboard(String text) {
      try {
         Minecraft.getInstance().keyboardHandler.setClipboard(text);
      } catch (Throwable var3) {
      }
   }

   @Override
   public boolean pickConfigFile(Consumer<Path> onResult) {
      return ConfigFiles.pickFile(onResult);
   }

   @Override
   public boolean webMode() {
      return ClarityConfig.webGuiActive();
   }

   @Override
   public void switchToWebGui() {
      this.setGui("GUI", "Web");
      ClarityConfig.flushSync();
      this.openWebGui();
   }

   @Override
   public void openWebGui() {
      if (this.webMode()) {
         try {
            ClarityServer.sync();
         } catch (Throwable var2) {
         }

         ConfigFiles.openUrl("http://localhost:7420/");
      }
   }

   @Override
   public String clipboard() {
      try {
         return Minecraft.getInstance().keyboardHandler.getClipboard();
      } catch (Throwable t) {
         return "";
      }
   }
}
