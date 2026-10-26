package need.clarity.friends.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.util.UiSounds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;

public final class DirectMessages {
   private static final int KEEP = 200;
   private static final Map<String, ArrayDeque<GlobalChat.Message>> convos = new ConcurrentHashMap<>();
   private static final Map<String, String> names = new ConcurrentHashMap<>();
   private static final Map<String, Integer> unread = new ConcurrentHashMap<>();
   private static final Map<Integer, String> inFlight = new ConcurrentHashMap<>();
   private static final AtomicInteger cids = new AtomicInteger();
   private static final AtomicLong localIds = new AtomicLong(-1000000L);
   private static final AtomicLong revision = new AtomicLong();
   private static boolean loaded = false;
   private static volatile long dirtyAt = 0L;

   private DirectMessages() {
   }

   public static long revision() {
      return revision.get();
   }

   public static List<GlobalChat.Message> messages(String uuid) {
      load();
      ArrayDeque<GlobalChat.Message> d = convos.get(key(uuid));
      if (d == null) {
         return List.of();
      }

      synchronized (d) {
         return new ArrayList<>(d);
      }
   }

   public static int unread(String uuid) {
      return unread.getOrDefault(key(uuid), 0);
   }

   public static int totalUnread() {
      int n = 0;

      for (int v : unread.values()) {
         n += v;
      }

      return n;
   }

   public static void markRead(String uuid) {
      if (uuid != null && unread.remove(key(uuid)) != null) {
         revision.incrementAndGet();
      }
   }

   public static List<DirectMessages.Conversation> conversations() {
      load();
      Map<String, DirectMessages.Conversation> out = new LinkedHashMap<>();

      for (Entry<String, ArrayDeque<GlobalChat.Message>> e : convos.entrySet()) {
         GlobalChat.Message last;
         synchronized ((ArrayDeque)e.getValue()) {
            last = e.getValue().peekLast();
         }

         String nm = displayName(e.getKey());
         out.put(e.getKey(), new DirectMessages.Conversation(e.getKey(), nm, unread(e.getKey()), last == null ? 0L : last.at(), last == null ? "" : last.text()));
      }

      for (Friend f : FriendManager.all()) {
         if (f.hasUuid() && PresenceClient.confirmed(f)) {
            String k = key(f.uuid());
            if (!out.containsKey(k)) {
               out.put(k, new DirectMessages.Conversation(k, f.displayName(), 0, 0L, ""));
            }
         }
      }

      List<DirectMessages.Conversation> l = new ArrayList<>(out.values());
      l.sort((a, b) -> {
         if (a.unread() > 0 != b.unread() > 0) {
            return a.unread() > 0 ? -1 : 1;
         } else {
            return a.lastAt() != b.lastAt() ? Long.compare(b.lastAt(), a.lastAt()) : a.name().compareToIgnoreCase(b.name());
         }
      });
      return l;
   }

   public static String displayName(String uuid) {
      Friend f = FriendManager.get(uuid, null);
      return f != null ? f.displayName() : names.getOrDefault(key(uuid), uuid);
   }

   public static String blockedReason(String uuid) {
      if (!FriendManager.bool("Online Features")) {
         return "Online features are off";
      } else if (PresenceClient.state() != PresenceClient.State.CONNECTED) {
         return "Not connected to Clarity";
      } else if (!PresenceClient.serverHas("dm")) {
         return "The Clarity server is out of date (redeploy it)";
      } else {
         Friend f = FriendManager.get(uuid, null);
         if (f == null) {
            return "They're not on your friends list";
         } else {
            return !PresenceClient.confirmed(f) ? "You can message them once they add you back" : null;
         }
      }
   }

   public static String send(String uuid, String raw) {
      String text = GlobalChat.clean(raw);
      if (text.isEmpty()) {
         return "Type a message first";
      }

      if (text.length() > 256) {
         return "Messages are limited to 256 characters";
      }

      String why = blockedReason(uuid);
      if (why != null) {
         return why;
      }

      int cid = cids.incrementAndGet();
      inFlight.put(cid, key(uuid));
      JsonObject o = new JsonObject();
      o.addProperty("t", "dm");
      o.addProperty("to", uuid);
      o.addProperty("text", text);
      o.addProperty("cid", cid);
      PresenceClient.sendRaw(o);
      String me = PresenceClient.myUuid();
      add(uuid, new GlobalChat.Message(localIds.decrementAndGet(), me, PresenceClient.myName(), text, System.currentTimeMillis(), GlobalChat.Kind.SELF, false));
      return null;
   }

   public static boolean intercept(String input, boolean addToRecent) {
      if (input != null && FriendManager.bool("Online Features")) {
         String prefix = FriendManager.str("DM Prefix").trim();
         if (!prefix.isEmpty() && !prefix.startsWith("/")) {
            String t = input.trim();
            if (!t.startsWith(prefix)) {
               return false;
            }

            String rest = t.substring(prefix.length());
            int sp = rest.indexOf(32);
            String who = sp < 0 ? rest : rest.substring(0, sp);
            if (who.isEmpty()) {
               return false;
            }

            Friend target = null;

            for (Friend f : FriendManager.all()) {
               if (f.name().equalsIgnoreCase(who) || f.displayName().equalsIgnoreCase(who)) {
                  target = f;
                  break;
               }
            }

            if (target != null && target.hasUuid()) {
               Minecraft mc = Minecraft.getInstance();

               try {
                  if (addToRecent) {
                     mc.gui.getChat().addRecentChat(t);
                  }
               } catch (Throwable var12) {
               }

               String body = sp < 0 ? "" : rest.substring(sp + 1).trim();
               if (body.isEmpty()) {
                  return true;
               }

               String err = send(target.uuid(), body);
               if (err != null) {
                  local(Component.literal("Couldn't send to " + target.displayName() + ": " + err).withColor(16747146));
               } else if (FriendManager.bool("Show DMs In Game Chat")) {
                  MutableComponent line = Component.literal("");
                  line.append(Component.literal("you → " + target.displayName()).withColor(8377599));
                  line.append(Component.literal(": ").withColor(9080728));
                  line.append(Component.literal(GlobalChat.clean(body)).withColor(15264496));
                  local(line);
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static void onDm(JsonObject o) {
      try {
         JsonObject from = o.getAsJsonObject("from");
         String uuid = from.get("uuid").getAsString();
         String name = from.get("name").getAsString();
         String text = GlobalChat.clean(o.get("text").getAsString());
         long at = o.has("at") ? o.get("at").getAsLong() : System.currentTimeMillis();
         long id = o.has("id") ? o.get("id").getAsLong() : localIds.decrementAndGet();
         names.put(key(uuid), name);
         if (FriendManager.get(uuid, name) == null) {
            return;
         }

         PresenceClient.confirm(uuid);
         add(uuid, new GlobalChat.Message(id, uuid, name, text, at, GlobalChat.Kind.USER, false));
         unread.merge(key(uuid), 1, Integer::sum);
         revision.incrementAndGet();
         String shown = displayName(uuid);
         if (FriendManager.bool("DM Notifications")) {
            Friends.alert(shown, "» " + (text.length() > 60 ? text.substring(0, 57) + "…" : text), true, UiSounds.Sound.MESSAGE);
         } else {
            UiSounds.play(UiSounds.Sound.MESSAGE);
         }

         if (FriendManager.bool("Show DMs In Game Chat")) {
            String prefix = FriendManager.str("DM Prefix").trim();
            MutableComponent nm = Component.literal(shown + " → you")
               .withStyle(
                  st -> st.withColor(16758891)
                     .withHoverEvent(new ShowText(Component.literal("Direct message" + (prefix.isEmpty() ? "" : "\nClick to reply"))))
                     .withClickEvent(prefix.isEmpty() ? null : new SuggestCommand(prefix + name + " "))
               );
            MutableComponent line = Component.literal("");
            line.append(nm);
            line.append(Component.literal(": ").withColor(9080728));
            line.append(Component.literal(text).withColor(15918808));
            local(line);
         }
      } catch (Exception var13) {
      }
   }

   static void onSent(JsonObject o) {
      int cid;
      try {
         cid = o.get("cid").getAsInt();
      } catch (Exception e) {
         return;
      }

      String uuid = inFlight.remove(cid);
      if (uuid != null) {
         if (o.has("error") && !o.get("error").isJsonNull()) {
            add(uuid, new GlobalChat.Message(localIds.decrementAndGet(), null, "Clarity", "Not sent: " + o.get("error").getAsString(), System.currentTimeMillis(), GlobalChat.Kind.SYSTEM, false));
         }
      }
   }

   private static String key(String uuid) {
      return uuid == null ? "" : uuid.toLowerCase(Locale.ROOT);
   }

   private static void add(String uuid, GlobalChat.Message m) {
      load();
      ArrayDeque<GlobalChat.Message> d = convos.computeIfAbsent(key(uuid), k -> new ArrayDeque<>());
      synchronized (d) {
         d.addLast(m);

         while (d.size() > 200) {
            d.removeFirst();
         }
      }

      revision.incrementAndGet();
      if (m.kind() != GlobalChat.Kind.SYSTEM) {
         save();
      }
   }

   private static void local(Component body) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         MutableComponent full = Component.literal("");
         full.append(Component.literal("[DM] ").withColor(16758891));
         full.append(body);
         mc.execute(() -> {
            try {
               if (mc.gui != null && mc.level != null) {
                  mc.gui.getChat().addClientSystemMessage(full);
               }
            } catch (Throwable var3) {
            }
         });
      }
   }

   private static Path file() {
      return FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("dms.json");
   }

   private static synchronized void load() {
      if (!loaded) {
         loaded = true;

         try {
            Path f = file();
            if (!Files.exists(f)) {
               return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();

            for (Entry<String, JsonElement> e : root.getAsJsonObject("convos").entrySet()) {
               JsonObject c = e.getValue().getAsJsonObject();
               String k = key(e.getKey());
               if (c.has("name")) {
                  names.put(k, c.get("name").getAsString());
               }

               ArrayDeque<GlobalChat.Message> d = new ArrayDeque<>();

               for (JsonElement me : c.getAsJsonArray("msgs")) {
                  JsonObject m = me.getAsJsonObject();
                  boolean self = m.has("self") && m.get("self").getAsBoolean();
                  d.addLast(
                     new GlobalChat.Message(
                        localIds.decrementAndGet(),
                        self ? PresenceClient.myUuid() : k,
                        self ? "you" : names.getOrDefault(k, k),
                        m.get("text").getAsString(),
                        m.get("at").getAsLong(),
                        self ? GlobalChat.Kind.SELF : GlobalChat.Kind.USER,
                        false
                     )
                  );
               }

               convos.put(k, d);
            }
         } catch (Throwable var11) {
         }
      }
   }

   private static void save() {
      long stamp = System.nanoTime();
      dirtyAt = stamp;
      Thread t = new Thread(() -> {
         try {
            Thread.sleep(1500L);
         } catch (InterruptedException var3) {
         }

         if (dirtyAt == stamp) {
            writeNow();
         }
      }, "clarity-dm-save");
      t.setDaemon(true);
      t.start();
   }

   public static synchronized void writeNow() {
      try {
         JsonObject root = new JsonObject();
         JsonObject cs = new JsonObject();

         for (Entry<String, ArrayDeque<GlobalChat.Message>> e : convos.entrySet()) {
            JsonObject c = new JsonObject();
            c.addProperty("name", names.getOrDefault(e.getKey(), displayName(e.getKey())));
            JsonArray arr = new JsonArray();
            synchronized ((ArrayDeque)e.getValue()) {
               for (GlobalChat.Message m : e.getValue()) {
                  if (m.kind() != GlobalChat.Kind.SYSTEM) {
                     JsonObject mo = new JsonObject();
                     mo.addProperty("text", m.text());
                     mo.addProperty("at", m.at());
                     if (m.kind() == GlobalChat.Kind.SELF) {
                        mo.addProperty("self", true);
                     }

                     arr.add(mo);
                  }
               }
            }

            c.add("msgs", arr);
            cs.add(e.getKey(), c);
         }

         root.add("convos", cs);
         Path f = file();
         Files.createDirectories(f.getParent());
         Path tmp = f.resolveSibling("dms.json.tmp");
         Files.writeString(tmp, new Gson().toJson(root), StandardCharsets.UTF_8);
         Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (Throwable var12) {
      }
   }

   public static void clear(String uuid) {
      convos.remove(key(uuid));
      unread.remove(key(uuid));
      revision.incrementAndGet();
      save();
   }

   public record Conversation(String uuid, String name, int unread, long lastAt, String lastText) {
   }
}
