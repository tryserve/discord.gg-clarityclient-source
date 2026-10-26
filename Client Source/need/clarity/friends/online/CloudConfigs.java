package need.clarity.friends.online;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import need.clarity.clickgui.GuiData;
import need.clarity.config.ClarityConfig;
import need.clarity.config.ConfigSchema;
import need.clarity.config.LegacyConfigMigration;
import need.clarity.friends.FriendManager;
import need.clarity.modules.ModuleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class CloudConfigs {
   public static final List<String> VISIBILITIES = List.of("Private", "Unlisted", "Public");
   public static final List<String> SORTS = List.of("Most Liked", "Most Downloaded", "Newest", "Name");
   public static final List<String> SCOPES = List.of("Everyone", "Liked By Me");
   public static final CloudConfigs.View MINE = new CloudConfigs.View();
   public static final CloudConfigs.View BROWSE = new CloudConfigs.View();
   public static volatile String query = "";
   public static volatile String sort = "Most Liked";
   public static volatile String scope = "Everyone";
   public static volatile String author = "";
   private static final AtomicInteger reqIds = new AtomicInteger();
   private static final Map<Integer, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
   private static final AtomicLong revision = new AtomicLong();
   private static final Gson GSON = new Gson();
   private static final String BACKUP = "Before cloud load";
   private static Map<String, String> links;

   private CloudConfigs() {
   }

   public static long revision() {
      return revision.get();
   }

   public static boolean available() {
      return PresenceClient.serverHas("configs");
   }

   public static String unavailableReason() {
      if (!FriendManager.bool("Online Features")) {
         return "Online features are off";
      } else if (PresenceClient.state() != PresenceClient.State.CONNECTED) {
         return "Not connected to Clarity";
      } else {
         return available() ? null : "The Clarity server is out of date (redeploy it, see VPS_SETUP.md section 7)";
      }
   }

   private static CompletableFuture<JsonObject> request(String type, JsonObject body) {
      JsonObject o = new JsonObject();
      o.addProperty("t", type);
      int req = reqIds.incrementAndGet();
      o.addProperty("req", req);
      if (body != null) {
         for (Entry<String, JsonElement> e : body.entrySet()) {
            o.add(e.getKey(), e.getValue());
         }
      }

      if (!available()) {
         JsonObject err = new JsonObject();
         err.addProperty("error", unavailableReason());
         return CompletableFuture.completedFuture(err);
      } else {
         CompletableFuture<JsonObject> f = new CompletableFuture<>();
         pending.put(req, f);
         PresenceClient.sendRaw(o);
         return f.completeOnTimeout(timeout(), 20L, TimeUnit.SECONDS).whenComplete((r, t) -> pending.remove(req));
      }
   }

   private static JsonObject timeout() {
      JsonObject err = new JsonObject();
      err.addProperty("error", "the server didn't answer");
      return err;
   }

   static void onResult(JsonObject o) {
      int req;
      try {
         req = o.get("req").getAsInt();
      } catch (Exception e) {
         return;
      }

      CompletableFuture<JsonObject> f = pending.remove(req);
      if (f != null) {
         f.complete(o);
      }
   }

   static void onDisconnect() {
      for (CompletableFuture<JsonObject> f : pending.values()) {
         JsonObject err = new JsonObject();
         err.addProperty("error", "disconnected");
         f.complete(err);
      }

      pending.clear();
   }

   private static void onMain(Runnable r) {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         r.run();
      } else {
         mc.execute(() -> {
            try {
               r.run();
            } catch (Throwable var2) {
            }
         });
      }
   }

   private static String error(JsonObject o) {
      return o.has("error") && !o.get("error").isJsonNull() ? capital(o.get("error").getAsString()) : null;
   }

   private static String capital(String s) {
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   public static void refreshMine() {
      MINE.loading = true;
      JsonObject b = new JsonObject();
      b.addProperty("scope", "mine");
      b.addProperty("sort", "new");
      request("cfg_list", b).thenAccept(r -> onMain(() -> {
         MINE.loading = false;
         String err = error(r);
         MINE.error = err;
         if (err == null) {
            MINE.items = parseList(r);
            MINE.total = r.get("total").getAsInt();
            MINE.loadedAt = System.currentTimeMillis();
         }

         revision.incrementAndGet();
      }));
   }

   public static void refreshBrowse(boolean more) {
      if (!more || !BROWSE.loading && BROWSE.hasMore()) {
         int page = more ? BROWSE.page + 1 : 0;
         BROWSE.loading = true;
         JsonObject b = new JsonObject();
         b.addProperty("scope", "Liked By Me".equals(scope) ? "liked" : "public");

         b.addProperty("sort", switch (sort) {
            case "Most Downloaded" -> "downloads";
            case "Newest" -> "new";
            case "Name" -> "name";
            default -> "likes";
         });
         b.addProperty("q", query);
         b.addProperty("author", author);
         b.addProperty("page", page);
         String q = query;
         String s = sort;
         String sc = scope;
         String au = author;
         request("cfg_list", b).thenAccept(r -> onMain(() -> {
            if (q.equals(query) && s.equals(sort) && sc.equals(scope) && au.equals(author)) {
               BROWSE.loading = false;
               String err = error(r);
               BROWSE.error = err;
               if (err == null) {
                  List<CloudConfigs.Meta> got = parseList(r);
                  if (more) {
                     List<CloudConfigs.Meta> all = new ArrayList<>(BROWSE.items);
                     all.addAll(got);
                     BROWSE.items = all;
                  } else {
                     BROWSE.items = got;
                  }

                  BROWSE.total = r.get("total").getAsInt();
                  BROWSE.page = page;
                  BROWSE.loadedAt = System.currentTimeMillis();
               }

               revision.incrementAndGet();
            }
         }));
      }
   }

   private static List<CloudConfigs.Meta> parseList(JsonObject r) {
      List<CloudConfigs.Meta> out = new ArrayList<>();
      if (r.has("items") && r.get("items").isJsonArray()) {
         for (JsonElement e : r.getAsJsonArray("items")) {
            CloudConfigs.Meta m = parseMeta(e);
            if (m != null) {
               out.add(m);
            }
         }
      }

      return out;
   }

   private static CloudConfigs.Meta parseMeta(JsonElement el) {
      if (el != null && el.isJsonObject()) {
         JsonObject o = el.getAsJsonObject();

         try {
            JsonObject owner = o.getAsJsonObject("owner");
            return new CloudConfigs.Meta(
               s(o, "id"),
               s(o, "name"),
               s(o, "description"),
               s(o, "server"),
               s(o, "visibility"),
               s(owner, "uuid"),
               s(owner, "name"),
               l(o, "created"),
               l(o, "updated"),
               (int)l(o, "size"),
               (int)l(o, "modules"),
               (int)l(o, "enabled"),
               (int)l(o, "likes"),
               b(o, "liked"),
               (int)l(o, "downloads"),
               b(o, "mine")
            );
         } catch (Exception e) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String s(JsonObject o, String k) {
      return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
   }

   private static long l(JsonObject o, String k) {
      try {
         return o.get(k).getAsLong();
      } catch (Exception e) {
         return 0L;
      }
   }

   private static boolean b(JsonObject o, String k) {
      try {
         return o.get(k).getAsBoolean();
      } catch (Exception e) {
         return false;
      }
   }

   private static void replace(CloudConfigs.Meta m) {
      if (m != null) {
         for (CloudConfigs.View v : new CloudConfigs.View[]{MINE, BROWSE}) {
            List<CloudConfigs.Meta> l = new ArrayList<>(v.items);

            for (int i = 0; i < l.size(); i++) {
               if (l.get(i).id().equals(m.id())) {
                  l.set(i, m);
               }
            }

            v.items = l;
         }

         revision.incrementAndGet();
      }
   }

   private static void drop(String id) {
      for (CloudConfigs.View v : new CloudConfigs.View[]{MINE, BROWSE}) {
         List<CloudConfigs.Meta> l = new ArrayList<>(v.items);
         if (l.removeIf(x -> x.id().equals(id))) {
            v.items = l;
            v.total = Math.max(0, v.total - 1);
         }
      }

      revision.incrementAndGet();
   }

   public static JsonObject currentSnapshot() {
      Set<String> modules = new LinkedHashSet<>();

      try {
         modules.addAll(GuiData.get().modules.keySet());
      } catch (Throwable var2) {
      }

      return ConfigSchema.snapshot(ClarityConfig.getCurrent(), modules);
   }

   public static JsonObject profileSnapshot(String profile) {
      JsonElement p = ClarityConfig.getCurrent().get("__profiles");
      if (p != null && p.isJsonObject()) {
         JsonElement s = p.getAsJsonObject().get(profile);
         return s != null && s.isJsonObject() ? s.getAsJsonObject().deepCopy() : null;
      } else {
         return null;
      }
   }

   public static void upload(String name, String description, String visibility, String server, JsonObject snapshot, String linkProfile, String updateId, Consumer<CloudConfigs.Result> done) {
      if (snapshot == null) {
         done.accept(CloudConfigs.Result.fail("Nothing to upload"));
      } else {
         JsonObject b = new JsonObject();
         if (updateId != null) {
            b.addProperty("id", updateId);
         }

         if (name != null) {
            b.addProperty("name", name);
         }

         if (description != null) {
            b.addProperty("description", description);
         }

         if (server != null) {
            b.addProperty("server", server);
         }

         if (visibility != null) {
            b.addProperty("visibility", visibility.toLowerCase(Locale.ROOT));
         }

         b.add("data", snapshot);
         request("cfg_put", b)
            .thenAccept(
               r -> onMain(
                  () -> {
                     String err = error(r);
                     if (err != null) {
                        done.accept(CloudConfigs.Result.fail(err));
                     } else {
                        CloudConfigs.Meta m = parseMeta(r.get("meta"));
                        if (m != null && linkProfile != null && !linkProfile.isEmpty()) {
                           link(linkProfile, m.id());
                        }

                        refreshMine();
                        if (m != null && "public".equals(m.visibility())) {
                           refreshBrowse(false);
                        }

                        done.accept(
                           CloudConfigs.Result.ok(
                              updateId != null ? "Updated " + (m == null ? "config" : m.name()) : "Uploaded " + (m == null ? "config" : m.name()) + (m == null ? "" : " · code " + m.id()), m
                           )
                        );
                     }
                  }
               )
            );
      }
   }

   public static void setVisibility(CloudConfigs.Meta m, String visibility, Consumer<CloudConfigs.Result> done) {
      JsonObject b = new JsonObject();
      b.addProperty("id", m.id());
      b.addProperty("visibility", visibility.toLowerCase(Locale.ROOT));
      edit(b, done, m.name() + " is now " + visibility.toLowerCase(Locale.ROOT));
   }

   public static void rename(CloudConfigs.Meta m, String name, String description, String server, Consumer<CloudConfigs.Result> done) {
      JsonObject b = new JsonObject();
      b.addProperty("id", m.id());
      if (name != null) {
         b.addProperty("name", name);
      }

      if (description != null) {
         b.addProperty("description", description);
      }

      if (server != null) {
         b.addProperty("server", server);
      }

      edit(b, done, "Saved");
   }

   private static void edit(JsonObject b, Consumer<CloudConfigs.Result> done, String okMsg) {
      request("cfg_edit", b).thenAccept(r -> onMain(() -> {
         String err = error(r);
         if (err != null) {
            done.accept(CloudConfigs.Result.fail(err));
         } else {
            CloudConfigs.Meta m = parseMeta(r.get("meta"));
            replace(m);
            refreshBrowse(false);
            done.accept(CloudConfigs.Result.ok(okMsg, m));
         }
      }));
   }

   public static void like(CloudConfigs.Meta m, boolean on, Consumer<CloudConfigs.Result> done) {
      JsonObject b = new JsonObject();
      b.addProperty("id", m.id());
      b.addProperty("like", on);
      replace(
         new CloudConfigs.Meta(
            m.id(),
            m.name(),
            m.description(),
            m.server(),
            m.visibility(),
            m.ownerUuid(),
            m.ownerName(),
            m.created(),
            m.updated(),
            m.size(),
            m.modules(),
            m.enabled(),
            Math.max(0, m.likes() + (on && !m.liked() ? 1 : 0) - (!on && m.liked() ? 1 : 0)),
            on,
            m.downloads(),
            m.mine()
         )
      );
      request("cfg_like", b).thenAccept(r -> onMain(() -> {
         String err = error(r);
         if (err != null) {
            replace(m);
            done.accept(CloudConfigs.Result.fail(err));
         } else {
            CloudConfigs.Meta nm = parseMeta(r.get("meta"));
            replace(nm);
            done.accept(CloudConfigs.Result.ok(on ? "Liked " + m.name() : "Unliked " + m.name(), nm));
         }
      }));
   }

   public static void delete(CloudConfigs.Meta m, Consumer<CloudConfigs.Result> done) {
      JsonObject b = new JsonObject();
      b.addProperty("id", m.id());
      request("cfg_delete", b).thenAccept(r -> onMain(() -> {
         String err = error(r);
         if (err != null) {
            done.accept(CloudConfigs.Result.fail(err));
         } else {
            drop(m.id());
            unlinkId(m.id());
            done.accept(CloudConfigs.Result.ok("Deleted " + m.name(), null));
         }
      }));
   }

   public static void load(String code, boolean apply, Consumer<CloudConfigs.Result> done) {
      String id = code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
      if (id.isEmpty()) {
         done.accept(CloudConfigs.Result.fail("Type a config code"));
      } else {
         JsonObject b = new JsonObject();
         b.addProperty("id", id);
         request("cfg_get", b).thenAccept(r -> onMain(() -> {
            String err = error(r);
            if (err != null) {
               done.accept(CloudConfigs.Result.fail(err));
            } else {
               CloudConfigs.Meta m = parseMeta(r.get("meta"));
               if (m != null && r.has("data") && r.get("data").isJsonObject()) {
                  replace(m);
                  JsonObject data = r.getAsJsonObject("data");

                  String profile;
                  try {
                     profile = importSnapshot(m, data, apply);
                  } catch (Exception e) {
                     done.accept(CloudConfigs.Result.fail("Couldn't load it: " + e.getMessage()));
                     return;
                  }

                  done.accept(CloudConfigs.Result.ok((apply ? "Loaded " : "Saved as profile ") + profile, m));
               } else {
                  done.accept(CloudConfigs.Result.fail("Broken config"));
               }
            }
         }));
      }
   }

   private static String importSnapshot(CloudConfigs.Meta m, JsonObject data, boolean apply) {
      JsonObject snap = data.deepCopy();
      LegacyConfigMigration.migrateChinaHat(snap, false);
      ConfigSchema.normalizeSnapshot(snap);
      JsonObject root = ClarityConfig.getCurrent();
      JsonElement pe = root.get("__profiles");
      Set<String> taken = pe != null && pe.isJsonObject() ? new HashSet<>(pe.getAsJsonObject().keySet()) : new HashSet<>();
      String name = null;

      for (Entry<String, String> e : links().entrySet()) {
         if (e.getValue().equals(m.id()) && taken.contains(e.getKey())) {
            name = e.getKey();
            break;
         }
      }

      if (name == null) {
         String base = m.mine() ? m.name() : m.name() + " (" + m.ownerName() + ")";
         name = base;

         for (int i = 2; taken.contains(name); i++) {
            name = base + " (" + i + ")";
         }
      }

      if (apply && FriendManager.bool("Backup Before Loading")) {
         JsonObject backup = currentSnapshot();
         ClarityConfig.mutate(r -> obj(r, "__profiles").add("Before cloud load", backup));
      }

      String finalName = name;
      ClarityConfig.mutate(r -> obj(r, "__profiles").add(finalName, snap));
      if (m.mine()) {
         link(finalName, m.id());
      }

      if (apply) {
         ClarityConfig.applyProfileSnapshot(snap);
         ClarityConfig.setActiveProfile(finalName);
         ClarityConfig.mutate(r -> {});

         try {
            ModuleRegistry.syncFromConfig();
         } catch (Throwable var10) {
         }
      }

      return finalName;
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

   private static Path linksFile() {
      return FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("cloud.json");
   }

   public static synchronized Map<String, String> links() {
      if (links == null) {
         links = new LinkedHashMap<>();

         try {
            Path f = linksFile();
            if (Files.exists(f)) {
               JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
               if (o.has("links")) {
                  for (Entry<String, JsonElement> e : o.getAsJsonObject("links").entrySet()) {
                     links.put(e.getKey(), e.getValue().getAsString());
                  }
               }
            }
         } catch (Throwable var4) {
         }
      }

      return links;
   }

   public static String linkOf(String profile) {
      return profile == null ? null : links().get(profile);
   }

   public static String profileFor(String id) {
      for (Entry<String, String> e : links().entrySet()) {
         if (e.getValue().equals(id)) {
            return e.getKey();
         }
      }

      return null;
   }

   public static synchronized void link(String profile, String id) {
      links().entrySet().removeIf(e -> e.getValue().equals(id));
      links().put(profile, id);
      saveLinks();
   }

   public static synchronized void unlinkProfile(String profile) {
      if (links().remove(profile) != null) {
         saveLinks();
      }
   }

   private static synchronized void unlinkId(String id) {
      if (links().entrySet().removeIf(e -> e.getValue().equals(id))) {
         saveLinks();
      }
   }

   private static synchronized void saveLinks() {
      try {
         JsonObject o = new JsonObject();
         JsonObject l = new JsonObject();

         for (Entry<String, String> e : links.entrySet()) {
            l.addProperty(e.getKey(), e.getValue());
         }

         o.add("links", l);
         Path f = linksFile();
         Files.createDirectories(f.getParent());
         Files.writeString(f, GSON.toJson(o), StandardCharsets.UTF_8);
      } catch (Throwable var4) {
      }

      revision.incrementAndGet();
   }

   public static void onProfileSaved(String profile) {
      try {
         String id = linkOf(profile);
         if (id == null || !available() || !FriendManager.bool("Auto Sync Linked Profiles")) {
            return;
         }

         JsonObject snap = profileSnapshot(profile);
         if (snap == null) {
            return;
         }

         upload(null, null, null, null, snap, profile, id, r -> {
            if (r.error() != null && r.error().toLowerCase(Locale.ROOT).contains("no longer exists")) {
               unlinkProfile(profile);
            }
         });
      } catch (Throwable var3) {
      }
   }

   public static void onProfileDeleted(String profile) {
      try {
         unlinkProfile(profile);
      } catch (Throwable var2) {
      }
   }

   public record Meta(
      String id,
      String name,
      String description,
      String server,
      String visibility,
      String ownerUuid,
      String ownerName,
      long created,
      long updated,
      int size,
      int modules,
      int enabled,
      int likes,
      boolean liked,
      int downloads,
      boolean mine
   ) {
      public String visibilityLabel() {
         return switch (this.visibility) {
            case "public" -> "Public";
            case "unlisted" -> "Unlisted";
            default -> "Private";
         };
      }
   }

   public record Result(String error, String message, CloudConfigs.Meta meta) {
      static CloudConfigs.Result ok(String msg, CloudConfigs.Meta m) {
         return new CloudConfigs.Result(null, msg, m);
      }

      static CloudConfigs.Result fail(String err) {
         return new CloudConfigs.Result(err, null, null);
      }
   }

   public static final class View {
      public volatile List<CloudConfigs.Meta> items = List.of();
      public volatile boolean loading;
      public volatile String error;
      public volatile int total;
      public volatile int page;
      public volatile long loadedAt;

      public boolean hasMore() {
         return this.items.size() < this.total;
      }
   }
}
