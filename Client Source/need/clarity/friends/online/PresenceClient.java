package need.clarity.friends.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import need.clarity.Clarity;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.util.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class PresenceClient {
   public static final String DEFAULT_URL = "wss://104-156-154-148.sslip.io";
   public static final int PROTOCOL = 1;
   private static final Gson GSON = new Gson();
   private static volatile PresenceClient.State state = PresenceClient.State.DISABLED;
   private static volatile String lastError = "";
   private static volatile int userCount = -1;
   private static volatile String myUuid;
   private static volatile String myName;
   private static final Map<String, PresenceClient.Presence> presence = new ConcurrentHashMap<>();
   private static final Map<String, PresenceClient.Request> incoming = new ConcurrentHashMap<>();
   private static final Map<String, Long> outgoing = new ConcurrentHashMap<>();
   private static volatile WebSocket socket;
   private static volatile int generation = 0;
   private static volatile String connectedUrl;
   private static volatile long nextAttemptAt = 0L;
   private static volatile int failures = 0;
   private static volatile boolean dirtyStatus = true;
   private static volatile boolean dirtyFriends = true;
   private static volatile String lastServerSent = null;
   private static volatile String lastStatusSent = null;
   private static volatile long lastPing = 0L;
   private static volatile String phase = "";
   private static volatile long attemptStartedAt = 0L;
   private static volatile String lastLogged = "";
   private static final long ATTEMPT_TIMEOUT_MS = 25000L;
   private static final long MOJANG_TIMEOUT_MS = 12000L;
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "clarity-presence");
      t.setDaemon(true);
      return t;
   });
   private static final ExecutorService AUTH = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r, "clarity-presence-auth");
      t.setDaemon(true);
      return t;
   });
   private static volatile HttpClient http;
   private static boolean initialised = false;
   private static final Set<String> features = ConcurrentHashMap.newKeySet();
   private static final Set<String> quietRequests = ConcurrentHashMap.newKeySet();
   private static Set<String> knownFriendKeys = null;

   private PresenceClient() {
   }

   public static PresenceClient.State state() {
      return state;
   }

   public static String lastError() {
      return lastError;
   }

   public static int userCount() {
      return state == PresenceClient.State.CONNECTED ? userCount : -1;
   }

   public static PresenceClient.Presence presenceOf(Friend f) {
      return f != null && f.hasUuid() && confirmed(f) ? presence.get(f.uuid()) : null;
   }

   public static boolean onlineOnClarity(Friend f) {
      PresenceClient.Presence p = presenceOf(f);
      return p != null && p.online();
   }

   public static List<PresenceClient.Request> incomingRequests() {
      List<PresenceClient.Request> l = new ArrayList<>(incoming.values());
      l.sort(Comparator.comparingLong(PresenceClient.Request::at));
      return l;
   }

   public static Set<String> outgoingRequests() {
      return new TreeSet<>(outgoing.keySet());
   }

   public static boolean serverHas(String feature) {
      return state == PresenceClient.State.CONNECTED && features.contains(feature);
   }

   public static String myUuid() {
      return myUuid;
   }

   public static String myName() {
      return myName;
   }

   static void sendRaw(JsonObject o) {
      if (state == PresenceClient.State.CONNECTED) {
         send(o);
      }
   }

   public static String statusLine() {
      if (!FriendManager.bool("Online Features")) {
         return "Online features are off";
      }

      return switch (state) {
         case DISABLED -> wanted() ? "Starting…" : "Online features are off";
         case CONNECTING -> (phase.isEmpty() ? "Connecting" : Character.toUpperCase(phase.charAt(0)) + phase.substring(1))
            + "…"
            + (failures > 0 && !lastError.isEmpty() ? " (last try: " + lastError + ")" : "");
         case CONNECTED -> {
            int n = userCount;

            String you = switch (FriendManager.str("Status")) {
               case "Appear Offline" -> "you appear offline";
               case "Invisible" -> "you are invisible";
               default -> "you are " + FriendManager.str("Status").toLowerCase(Locale.ROOT);
            };
            String count = n >= 0 && FriendManager.bool("Show User Count") ? String.format(Locale.ROOT, "%,d using Clarity now", n) + " · " : "";
            yield "Connected · " + count + you;
         }
         case RETRYING -> {
            long in = Math.max(0L, (nextAttemptAt - System.currentTimeMillis() + 999L) / 1000L);
            yield "Not connected: " + (lastError.isEmpty() ? "can't reach the presence server" : lastError) + (in > 0L ? " · retrying in " + in + "s" : " · retrying");
         }
      };
   }

   public static String sendRequest(String name) {
      return sendRequest(name, null, false);
   }

   public static String sendRequest(String name, String uuid, boolean quiet) {
      if (name == null || !FriendManager.validName(name)) {
         return "That isn't a valid Minecraft name";
      }

      if (state != PresenceClient.State.CONNECTED) {
         return "Not connected to the presence server";
      }

      if (name.equalsIgnoreCase(myName)) {
         return "That's you";
      }

      outgoing.put(name.trim(), System.currentTimeMillis());
      if (quiet) {
         quietRequests.add(name.trim().toLowerCase(Locale.ROOT));
      }

      JsonObject o = msg("request");
      o.addProperty("to", name.trim());
      if (uuid != null && !uuid.isEmpty() && serverHas("request_uuid")) {
         o.addProperty("uuid", uuid);
      }

      send(o);
      return null;
   }

   public static boolean confirmed(Friend f) {
      return f != null && f.hasUuid() && confirmedSet().contains(f.uuid().toLowerCase(Locale.ROOT));
   }

   private static Set<String> confirmedSet() {
      Set<String> out = new HashSet<>();

      for (String p : FriendManager.str("Confirmed Friends").split(",")) {
         if (!p.isBlank()) {
            out.add(p.trim().toLowerCase(Locale.ROOT));
         }
      }

      return out;
   }

   static void confirm(String uuid) {
      if (uuid != null && !uuid.isEmpty()) {
         Set<String> set = confirmedSet();
         if (set.add(uuid.toLowerCase(Locale.ROOT))) {
            Set<String> keep = new LinkedHashSet<>();

            for (Friend f : FriendManager.all()) {
               if (f.hasUuid() && set.contains(f.uuid().toLowerCase(Locale.ROOT))) {
                  keep.add(f.uuid().toLowerCase(Locale.ROOT));
               }
            }

            keep.add(uuid.toLowerCase(Locale.ROOT));
            FriendManager.setSetting("Confirmed Friends", String.join(",", keep));
         }
      }
   }

   private static void autoRequestNewFriends() {
      Set<String> now = new HashSet<>();

      for (Friend f : FriendManager.all()) {
         now.add(f.key());
      }

      if (knownFriendKeys == null) {
         knownFriendKeys = now;
      } else {
         int sent = 0;

         for (Friend f : FriendManager.all()) {
            if (!knownFriendKeys.contains(f.key()) && !confirmed(f) && sent < 5 && !f.name().isEmpty() && !f.name().equalsIgnoreCase(myName)) {
               sendRequest(f.name(), f.hasUuid() ? f.uuid() : null, true);
               sent++;
            }
         }

         knownFriendKeys = now;
      }
   }

   public static void cancelRequest(String name) {
      if (outgoing.remove(name) != null) {
         JsonObject o = msg("cancel");
         o.addProperty("to", name);
         send(o);
      }
   }

   public static void respond(PresenceClient.Request r, boolean accept) {
      if (r != null && incoming.remove(r.uuid()) != null) {
         JsonObject o = msg("respond");
         o.addProperty("from", r.uuid());
         o.addProperty("accept", accept);
         send(o);
         if (accept) {
            FriendManager.add(r.uuid(), r.name());
            confirm(r.uuid());
            Friends.alert(r.name(), "is now your friend", true, UiSounds.Sound.ACCEPTED);
         }
      }
   }

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         FriendManager.addListener(() -> dirtyFriends = true);
         FriendManager.addSettingListener(() -> {
            dirtyStatus = true;
            String want = url();
            if (socket != null && connectedUrl != null && !connectedUrl.equals(want)) {
               disconnect("server changed");
            }

            if (!wanted()) {
               disconnect("turned off");
            }
         });
      }
   }

   public static void shutdown() {
      disconnect("shutting down");
      IO.shutdown();
      AUTH.shutdownNow();
   }

   private static boolean wanted() {
      return FriendManager.enabled() && FriendManager.bool("Online Features");
   }

   private static String url() {
      String u = FriendManager.str("Presence Server").trim();
      if (u.contains("169-128-191-166") || u.contains("169.128.191.166")) {
         u = "";
      }

      return u.isEmpty() ? "wss://104-156-154-148.sslip.io" : u;
   }

   public static void clientTick(Minecraft mc) {
      if (!wanted()) {
         if (socket != null) {
            disconnect("turned off");
         }

         state = PresenceClient.State.DISABLED;
      } else if (mc.getUser() != null) {
         long now = System.currentTimeMillis();
         if (socket == null && state != PresenceClient.State.CONNECTING && now >= nextAttemptAt) {
            connect(mc);
         }

         if (state == PresenceClient.State.CONNECTING && attemptStartedAt > 0L && now - attemptStartedAt > 25000L) {
            fail("timed out while " + (phase.isEmpty() ? "connecting" : phase));
         } else if (state == PresenceClient.State.CONNECTED) {
            String server = currentServer(mc);
            String status = FriendManager.str("Status");
            if (dirtyStatus || !Objects.equals(server, lastServerSent) || !Objects.equals(status, lastStatusSent)) {
               dirtyStatus = false;
               lastServerSent = server;
               lastStatusSent = status;
               send(statusMessage(server));
            }

            if (dirtyFriends) {
               dirtyFriends = false;
               send(friendsMessage());
               autoRequestNewFriends();
            }

            if (now - lastPing > 25000L) {
               lastPing = now;
               send(msg("ping"));
            }
         }
      }
   }

   private static void connect(Minecraft mc) {
      String u = url();

      URI uri;
      try {
         uri = URI.create(u);
         if (!"ws".equals(uri.getScheme()) && !"wss".equals(uri.getScheme())) {
            throw new IllegalArgumentException("use ws:// or wss://");
         }
      } catch (Exception e) {
         fail("bad address: " + e.getMessage());
         return;
      }

      myUuid = MojangAuth.profileId(mc);
      myName = MojangAuth.profileName(mc);
      if (myUuid != null && myName != null) {
         state = PresenceClient.State.CONNECTING;
         phase = "connecting to " + uri.getHost();
         attemptStartedAt = System.currentTimeMillis();
         connectedUrl = u;
         int gen = ++generation;

         try {
            if (http == null) {
               http = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10L)).build();
            }

            http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10L)).header("User-Agent", "Clarity-Presence/1").buildAsync(uri, new PresenceClient.Listener(gen)).whenComplete((ws, err) -> {
               if (gen != generation) {
                  if (ws != null) {
                     ws.abort();
                  }
               } else {
                  if (err != null) {
                     fail(rootMessage(err));
                  }
               }
            });
         } catch (Throwable t) {
            fail(rootMessage(t));
         }
      } else {
         fail("no account");
      }
   }

   private static void disconnect(String why) {
      generation++;
      WebSocket ws = socket;
      socket = null;
      connectedUrl = null;
      presence.clear();
      incoming.clear();
      outgoing.clear();
      userCount = -1;
      phase = "";
      attemptStartedAt = 0L;

      try {
         CloudConfigs.onDisconnect();
      } catch (Throwable var3) {
      }

      state = wanted() ? PresenceClient.State.RETRYING : PresenceClient.State.DISABLED;
      if (ws != null) {
         IO.execute(() -> {
            try {
               ws.sendClose(1000, why).get(3L, TimeUnit.SECONDS);
            } catch (Exception var3x) {
            }

            ws.abort();
         });
      }
   }

   private static void fail(String why) {
      generation++;
      WebSocket ws = socket;
      socket = null;
      if (ws != null) {
         try {
            ws.abort();
         } catch (Throwable var6) {
         }
      }

      presence.clear();
      userCount = -1;
      lastError = why == null ? "" : why;

      try {
         CloudConfigs.onDisconnect();
      } catch (Throwable var5) {
      }

      phase = "";
      attemptStartedAt = 0L;
      failures = Math.min(failures + 1, 8);
      if (!lastError.equals(lastLogged)) {
         lastLogged = lastError;

         try {
            Clarity.LOGGER.warn("[Clarity presence] not connected: {}", lastError);
         } catch (Throwable var4) {
         }
      }

      long backoff = Math.min(120000L, 2000L * (1L << failures - 1));
      backoff += ThreadLocalRandom.current().nextLong(1000L);
      nextAttemptAt = System.currentTimeMillis() + backoff;
      state = wanted() ? PresenceClient.State.RETRYING : PresenceClient.State.DISABLED;
   }

   private static JsonObject msg(String type) {
      JsonObject o = new JsonObject();
      o.addProperty("t", type);
      return o;
   }

   private static String statusCode() {
      return switch (FriendManager.str("Status")) {
         case "Away" -> "away";
         case "Busy" -> "busy";
         case "Appear Offline" -> "offline";
         case "Invisible" -> "invisible";
         default -> "online";
      };
   }

   private static JsonObject statusMessage(String server) {
      JsonObject o = msg("status");
      o.addProperty("status", statusCode());
      if (server != null && FriendManager.bool("Show Server")) {
         o.addProperty("server", server);
      }

      o.addProperty("share", "Anyone On My List".equals(FriendManager.str("Share With")) ? "list" : "mutual");
      o.addProperty("requests", "Nobody".equals(FriendManager.str("Friend Requests")) ? "nobody" : "anyone");
      o.addProperty("chat", FriendManager.bool("Global Chat"));
      return o;
   }

   private static JsonObject friendsMessage() {
      JsonObject o = msg("friends");
      JsonArray arr = new JsonArray();

      for (Friend f : FriendManager.all()) {
         if (f.hasUuid()) {
            arr.add(f.uuid());
         }
      }

      o.add("friends", arr);
      JsonArray names = new JsonArray();

      for (Friend f : FriendManager.all()) {
         if (!f.hasUuid()) {
            names.add(f.name());
         }
      }

      o.add("names", names);
      return o;
   }

   private static void send(JsonObject o) {
      WebSocket ws = socket;
      if (ws != null) {
         String text = GSON.toJson(o);
         IO.execute(() -> {
            try {
               ws.sendText(text, true).get(10L, TimeUnit.SECONDS);
            } catch (Exception e) {
               fail("send failed");
            }
         });
      }
   }

   private static String currentServer(Minecraft mc) {
      try {
         if (mc.player == null) {
            return null;
         }

         if (mc.isSingleplayer()) {
            return "Singleplayer";
         }

         ServerData sd = mc.getCurrentServer();
         if (sd != null && sd.ip != null && !sd.ip.isEmpty()) {
            return sd.ip.toLowerCase(Locale.ROOT);
         }
      } catch (Throwable var2) {
      }

      return null;
   }

   private static void handle(String text) {
      JsonObject o;
      try {
         o = JsonParser.parseString(text).getAsJsonObject();
      } catch (Exception e) {
         return;
      }

      String t = s(o, "t");
      switch (t) {
         case "challenge":
            String serverId = s(o, "serverId");
            boolean needAuth = !o.has("auth") || o.get("auth").getAsBoolean();
            String err = null;
            if (needAuth) {
               phase = "signing in with Mojang";
               int gen = generation;
               Future<String> f = AUTH.submit(() -> MojangAuth.join(Minecraft.getInstance(), serverId));

               try {
                  err = f.get(12000L, TimeUnit.MILLISECONDS);
               } catch (TimeoutException e) {
                  f.cancel(true);
                  err = "Mojang sign-in timed out";
               } catch (ExecutionException e) {
                  err = "Mojang sign-in failed: " + rootMessage(e);
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
               }

               if (gen != generation) {
                  return;
               }
            }

            if (err != null) {
               fail(err);
               return;
            }

            phase = needAuth ? "checking your account" : "joining";
            JsonObject h = msg("hello");
            h.addProperty("v", 1);
            h.addProperty("uuid", myUuid);
            h.addProperty("name", myName);
            h.addProperty("serverId", serverId);
            h.addProperty("client", "clarity");
            JsonObject st = statusMessage(currentServer(Minecraft.getInstance()));

            for (Entry<String, JsonElement> e : st.entrySet()) {
               if (!"t".equals(e.getKey())) {
                  h.add(e.getKey(), e.getValue());
               }
            }

            JsonObject fr = friendsMessage();
            h.add("friends", fr.get("friends"));
            h.add("names", fr.get("names"));
            send(h);
            break;
         case "welcome":
            state = PresenceClient.State.CONNECTED;
            phase = "";
            attemptStartedAt = 0L;
            if (!lastLogged.isEmpty()) {
               lastLogged = "";

               try {
                  Clarity.LOGGER.info("[Clarity presence] connected");
               } catch (Throwable var13) {
               }
            }

            failures = 0;
            lastError = "";
            if (o.has("count")) {
               userCount = o.get("count").getAsInt();
            }

            GlobalChat.onWelcome(o);
            features.clear();
            if (o.has("features") && o.get("features").isJsonArray()) {
               for (JsonElement f : o.getAsJsonArray("features")) {
                  try {
                     features.add(f.getAsString());
                  } catch (Exception var12) {
                  }
               }
            }

            dirtyStatus = false;
            dirtyFriends = false;
            Set<String> keys = new HashSet<>();

            for (Friend f : FriendManager.all()) {
               keys.add(f.key());
            }

            knownFriendKeys = keys;
            lastServerSent = currentServer(Minecraft.getInstance());
            lastStatusSent = FriendManager.str("Status");
            break;
         case "count":
            userCount = o.get("count").getAsInt();
            break;
         case "chat":
            GlobalChat.onMessage(o);
            break;
         case "chat_history":
            GlobalChat.onHistory(o);
            break;
         case "chat_delete":
            GlobalChat.onDelete(o);
            break;
         case "chat_error":
            GlobalChat.onError(s(o, "reason"));
            break;
         case "cfg_result":
            CloudConfigs.onResult(o);
            break;
         case "dm":
            DirectMessages.onDm(o);
            break;
         case "dm_sent":
            DirectMessages.onSent(o);
            break;
         case "presence":
            applyPresence(o, true);
            break;
         case "presence_bulk":
            if (o.has("list")) {
               for (JsonElement e : o.getAsJsonArray("list")) {
                  if (e.isJsonObject()) {
                     applyPresence(e.getAsJsonObject(), false);
                  }
               }
            }
            break;
         case "resolved": {
            String uuid = s(o, "uuid");
            String name = s(o, "name");
            if (!uuid.isEmpty() && !name.isEmpty()) {
               FriendManager.observe(uuid, name);
            }
            break;
         }
         case "request": {
            JsonObject from = o.getAsJsonObject("from");
            String uuid = s(from, "uuid");
            String name = s(from, "name");
            if (uuid.isEmpty()) {
               return;
            }

            if (FriendManager.get(uuid, name) != null) {
               respondRaw(uuid, true);
               FriendManager.observe(uuid, name);
               confirm(uuid);
               return;
            }

            incoming.put(uuid, new PresenceClient.Request(uuid, name, System.currentTimeMillis()));
            Friends.alert(name, "sent you a friend request", true, UiSounds.Sound.REQUEST);
            break;
         }
         case "request_cancelled":
            incoming.remove(s(o, "from"));
            break;
         case "request_result":
            String to = s(o, "to");
            boolean ok = o.has("ok") && o.get("ok").getAsBoolean();
            boolean quiet = quietRequests.remove(to.toLowerCase(Locale.ROOT));
            if (!ok) {
               outgoing.remove(to);
               if (!quiet) {
                  Friends.alert(to, "couldn't get your request (" + s(o, "reason") + ")", false, UiSounds.Sound.ERROR);
               }
            }
            break;
         case "accepted": {
            JsonObject by = o.getAsJsonObject("by");
            String uuid = s(by, "uuid");
            String name = s(by, "name");
            outgoing.keySet().removeIf(n -> n.equalsIgnoreCase(name));
            FriendManager.add(uuid, name);
            confirm(uuid);
            Friends.alert(name, "accepted your friend request", true, UiSounds.Sound.ACCEPTED);
            break;
         }
         case "declined": {
            JsonObject by = o.getAsJsonObject("by");
            String name = s(by, "name");
            outgoing.keySet().removeIf(n -> n.equalsIgnoreCase(name));
            break;
         }
         case "error":
            lastError = s(o, "msg");
      }
   }

   private static void respondRaw(String uuid, boolean accept) {
      JsonObject o = msg("respond");
      o.addProperty("from", uuid);
      o.addProperty("accept", accept);
      send(o);
   }

   private static void applyPresence(JsonObject o, boolean live) {
      String uuid = s(o, "uuid");
      if (!uuid.isEmpty()) {
         boolean online = o.has("online") && o.get("online").getAsBoolean();
         String name = s(o, "name");
         PresenceClient.Presence before = presence.get(uuid);
         PresenceClient.Presence now = new PresenceClient.Presence(
            uuid, name, online, s(o, "status"), o.has("server") && !o.get("server").isJsonNull() ? s(o, "server") : null, System.currentTimeMillis()
         );
         presence.put(uuid, now);
         if (!name.isEmpty()) {
            FriendManager.observe(uuid, name);
         }

         if (online) {
            confirm(uuid);
         }

         if (live && online && (before == null || !before.online()) && FriendManager.bool("Online Alerts")) {
            Friend f = FriendManager.get(uuid, name);
            if (f != null) {
               Friends.alert(f.displayName(), "is online" + (now.server() != null ? " on " + now.server() : ""), true, UiSounds.Sound.ONLINE);
            }
         }
      }
   }

   private static String s(JsonObject o, String k) {
      if (o != null && o.has(k) && !o.get(k).isJsonNull()) {
         try {
            return o.get(k).getAsString();
         } catch (Exception e) {
            return "";
         }
      } else {
         return "";
      }
   }

   static String closeReason(int code, String reason) {
      return switch (code) {
         case 4000 -> "this Clarity version is too old for the server";
         case 4001 -> "the server rejected the sign-in (" + (reason == null ? "bad identity" : reason) + ")";
         case 4002 -> "the server got no sign-in in time";
         case 4003 -> "Mojang couldn't confirm your account (restart the launcher and log in again)";
         default -> reason != null && !reason.isBlank() ? reason : "connection closed (" + code + ")";
         case 4005 -> "you signed in somewhere else";
         case 4006 -> "the presence server can't reach Mojang right now";
         case 4008 -> "rate limited";
         case 4009 -> "too many connections from your network";
      };
   }

   private static String rootMessage(Throwable t) {
      while (t.getCause() != null) {
         t = t.getCause();
      }

      String m = t.getMessage();
      return m != null && !m.isBlank() ? m : t.getClass().getSimpleName();
   }

   private static final class Listener implements WebSocket.Listener {
      private final StringBuilder buf = new StringBuilder();
      private final int gen;

      Listener(int gen) {
         this.gen = gen;
      }

      private boolean stale() {
         return this.gen != PresenceClient.generation;
      }

      @Override
      public void onOpen(WebSocket ws) {
         try {
            if (this.stale()) {
               ws.abort();
               return;
            }

            PresenceClient.socket = ws;
            PresenceClient.phase = "waiting for the server";
            ws.request(1L);
         } catch (Throwable t) {
            if (!this.stale()) {
               PresenceClient.fail("client error: " + PresenceClient.rootMessage(t));
            }
         }
      }

      @Override
      public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
         if (this.stale()) {
            ws.abort();
            return null;
         }

         this.buf.append(data);
         if (this.buf.length() > 1000000) {
            this.buf.setLength(0);
            PresenceClient.fail("message too large");
            return null;
         }

         if (last) {
            String text = this.buf.toString();
            this.buf.setLength(0);
            PresenceClient.IO.execute(() -> {
               try {
                  PresenceClient.handle(text);
               } catch (Throwable t) {
                  if (!this.stale()) {
                     PresenceClient.fail("client error: " + t.getClass().getSimpleName() + ": " + PresenceClient.rootMessage(t));
                  }
               }
            });
         }

         ws.request(1L);
         return null;
      }

      @Override
      public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
         if (!this.stale()) {
            PresenceClient.fail(PresenceClient.closeReason(code, reason));
         }

         return null;
      }

      @Override
      public void onError(WebSocket ws, Throwable error) {
         if (!this.stale()) {
            PresenceClient.fail(PresenceClient.rootMessage(error));
         }
      }
   }

   public record Presence(String uuid, String name, boolean online, String status, String server, long since) {
   }

   public record Request(String uuid, String name, long at) {
   }

   public enum State {
      DISABLED,
      CONNECTING,
      CONNECTED,
      RETRYING;
   }
}
