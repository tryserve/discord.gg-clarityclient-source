package need.clarity.friends.online;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.util.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;

public final class GlobalChat {
   public static final int MAX_LENGTH = 256;
   public static final String ADMIN_UUID = "00000000-0000-0000-0000-000000000000";
   private static final int KEEP = 200;
   private static final ArrayDeque<GlobalChat.Message> log = new ArrayDeque<>();
   private static final AtomicLong revision = new AtomicLong();
   private static final AtomicLong systemIds = new AtomicLong(-1L);
   private static volatile int unread = 0;
   private static volatile long lastSentAt = 0L;
   private static volatile boolean admin = false;

   private GlobalChat() {
   }

   public static boolean isAdmin(GlobalChat.Message m) {
      return m != null && "00000000-0000-0000-0000-000000000000".equals(m.uuid());
   }

   public static List<GlobalChat.Message> messages() {
      List<GlobalChat.Message> all;
      synchronized (log) {
         all = new ArrayList<>(log);
      }

      boolean friendsOnly = "Friends Only".equals(FriendManager.str("Show Messages From"));
      Set<String> hidden = hiddenUsers();
      List<GlobalChat.Message> out = new ArrayList<>(all.size());

      for (GlobalChat.Message m : all) {
         if (m.kind() != GlobalChat.Kind.USER || !hidden.contains(m.uuid()) && (!friendsOnly || FriendManager.get(m.uuid(), m.name()) != null)) {
            out.add(m);
         }
      }

      return out;
   }

   public static long revision() {
      return revision.get();
   }

   public static int unread() {
      return unread;
   }

   public static void markRead() {
      unread = 0;
   }

   public static boolean isAdmin() {
      return admin && PresenceClient.state() == PresenceClient.State.CONNECTED;
   }

   public static boolean enabled() {
      return FriendManager.bool("Online Features") && FriendManager.bool("Global Chat");
   }

   public static String blockedReason() {
      if (!FriendManager.bool("Online Features")) {
         return "Online features are off";
      } else if (!FriendManager.bool("Global Chat")) {
         return "Global Chat is off";
      } else if (PresenceClient.state() != PresenceClient.State.CONNECTED) {
         return "Not connected to Clarity";
      } else {
         return !PresenceClient.serverHas("chat") ? "The Clarity server is out of date (redeploy it)" : null;
      }
   }

   public static String send(String raw) {
      String text = clean(raw);
      if (text.isEmpty()) {
         return "Type a message first";
      }

      if (text.length() > 256) {
         return "Messages are limited to 256 characters";
      }

      String why = blockedReason();
      if (why != null) {
         return why;
      }

      long now = System.currentTimeMillis();
      if (now - lastSentAt < 600L) {
         return "Slow down";
      }

      lastSentAt = now;
      JsonObject o = new JsonObject();
      o.addProperty("t", "chat");
      o.addProperty("text", text);
      PresenceClient.sendRaw(o);
      return null;
   }

   public static void delete(GlobalChat.Message m) {
      if (m != null && m.kind() != GlobalChat.Kind.SYSTEM) {
         JsonObject o = new JsonObject();
         o.addProperty("t", "chat_delete");
         o.addProperty("id", m.id());
         PresenceClient.sendRaw(o);
      }
   }

   public static void mute(GlobalChat.Message m, int minutes) {
      if (m != null && m.uuid() != null) {
         JsonObject o = new JsonObject();
         o.addProperty("t", "chat_mute");
         o.addProperty("uuid", m.uuid());
         o.addProperty("minutes", minutes);
         PresenceClient.sendRaw(o);
      }
   }

   public static Map<String, String> hiddenEntries() {
      Map<String, String> out = new LinkedHashMap<>();

      for (String p : FriendManager.str("Chat Hidden Users").split(",")) {
         if (!p.isBlank()) {
            String[] f = p.trim().split("\\|", 2);
            out.put(f[0].toLowerCase(Locale.ROOT), f.length > 1 ? f[1] : f[0]);
         }
      }

      return out;
   }

   public static Set<String> hiddenUsers() {
      return hiddenEntries().keySet();
   }

   public static boolean isHidden(String uuid) {
      return uuid != null && hiddenUsers().contains(uuid.toLowerCase(Locale.ROOT));
   }

   public static void setHidden(String uuid, String name, boolean hide) {
      if (uuid != null) {
         Map<String, String> map = hiddenEntries();
         String k = uuid.toLowerCase(Locale.ROOT);
         if (hide) {
            if (map.containsKey(k)) {
               return;
            }

            map.put(k, name == null ? k : name);
         } else if (map.remove(k) == null) {
            return;
         }

         List<String> parts = new ArrayList<>();

         for (Entry<String, String> e : map.entrySet()) {
            parts.add(e.getKey() + "|" + e.getValue().replace(",", "").replace("|", ""));
         }

         FriendManager.setSetting("Chat Hidden Users", parts.isEmpty() ? null : String.join(",", parts));
         revision.incrementAndGet();
      }
   }

   public static boolean intercept(String input, boolean addToRecent) {
      if (input != null && FriendManager.bool("Online Features") && FriendManager.bool("Global Chat")) {
         String prefix = FriendManager.str("Chat Prefix").trim();
         if (!prefix.isEmpty() && !prefix.startsWith("/")) {
            String trimmed = input.trim();
            if (!trimmed.startsWith(prefix)) {
               return false;
            }

            Minecraft mc = Minecraft.getInstance();

            try {
               if (addToRecent) {
                  mc.gui.getChat().addRecentChat(trimmed);
               }
            } catch (Throwable var7) {
            }

            String body = trimmed.substring(prefix.length()).trim();
            if (body.isEmpty()) {
               return true;
            }

            String err = send(body);
            if (err != null) {
               showLocal(Component.literal("Couldn't send: " + err).withColor(16747146));
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void showInGame(GlobalChat.Message m) {
      if (FriendManager.bool("Show In Game Chat")) {
         MutableComponent line = Component.literal("");
         if (m.kind() == GlobalChat.Kind.SYSTEM) {
            line.append(Component.literal(m.text()).withColor(10528952));
            showLocal(line);
         } else {
            String prefix = FriendManager.str("Chat Prefix").trim();
            String time = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(m.at()));
            int nameRgb = nameColour(m);
            boolean adminMsg = isAdmin(m);
            MutableComponent name = Component.literal(m.name())
               .withStyle(
                  st -> st.withColor(nameRgb)
                     .withBold(adminMsg)
                     .withHoverEvent(new ShowText(Component.literal("Clarity chat · " + time + (prefix.isEmpty() ? "" : "\nClick to reply"))))
                     .withClickEvent(prefix.isEmpty() ? null : new SuggestCommand(prefix + " @" + m.name() + " "))
               );
            line.append(name);
            line.append(Component.literal(": ").withColor(9080728));
            line.append(Component.literal(m.text()).withColor(m.mention() ? 16769658 : 15264496));
            showLocal(line);
         }
      }
   }

   private static void showLocal(Component body) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         MutableComponent full = Component.literal("");
         full.append(Component.literal("[C] ").withColor(6211839));
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

   public static int nameColour(GlobalChat.Message m) {
      if (isAdmin(m)) {
         return 16728128;
      } else if (m.kind() == GlobalChat.Kind.SELF) {
         return 8377599;
      } else {
         Friend f = FriendManager.get(m.uuid(), m.name());
         if (f != null) {
            int rgb = FriendManager.nameColour(f);
            return rgb >= 0 ? rgb & 16777215 : 8188072;
         } else {
            int h = Math.abs((m.uuid() == null ? m.name() : m.uuid()).hashCode());
            return pastel(h % 360 / 360.0F);
         }
      }
   }

   private static int pastel(float hue) {
      float s = 0.38F;
      float v = 1.0F;
      float h6 = (hue - (float)Math.floor(hue)) * 6.0F;
      int i = (int)h6;
      float f = h6 - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - s * f);
      float t = v * (1.0F - s * (1.0F - f));
      float r;
      float g;
      float b;
      switch (i) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return Math.round(r * 255.0F) << 16 | Math.round(g * 255.0F) << 8 | Math.round(b * 255.0F);
   }

   static void onWelcome(JsonObject o) {
      admin = o.has("chatAdmin") && o.get("chatAdmin").getAsBoolean();
   }

   static void onMessage(JsonObject o) {
      GlobalChat.Message m = parse(o);
      if (m != null) {
         add(m);
         if (m.kind() == GlobalChat.Kind.USER) {
            if (isHidden(m.uuid())) {
               return;
            }

            if ("Friends Only".equals(FriendManager.str("Show Messages From")) && FriendManager.get(m.uuid(), m.name()) == null) {
               return;
            }

            unread++;
            if (m.mention() && FriendManager.bool("Mention Alerts")) {
               Friends.alert(m.name(), "mentioned you in Clarity chat", true, UiSounds.Sound.MENTION);
            } else if (m.mention()) {
               UiSounds.play(UiSounds.Sound.MENTION);
            } else if (UiSounds.chatSounds()) {
               UiSounds.play(UiSounds.Sound.CHAT);
            }
         }

         showInGame(m);
      }
   }

   static void onHistory(JsonObject o) {
      List<GlobalChat.Message> list = new ArrayList<>();
      if (o.has("list") && o.get("list").isJsonArray()) {
         for (JsonElement e : o.getAsJsonArray("list")) {
            if (e.isJsonObject()) {
               GlobalChat.Message m = parse(e.getAsJsonObject());
               if (m != null) {
                  list.add(m);
               }
            }
         }
      }

      synchronized (log) {
         log.removeIf(mx -> mx.kind() != GlobalChat.Kind.SYSTEM);
         List<GlobalChat.Message> merged = new ArrayList<>(log);
         merged.addAll(list);
         merged.sort(Comparator.comparingLong(GlobalChat.Message::at));
         log.clear();

         for (GlobalChat.Message m : merged) {
            log.addLast(m);
         }

         while (log.size() > 200) {
            log.removeFirst();
         }
      }

      revision.incrementAndGet();
   }

   static void onDelete(JsonObject o) {
      long id;
      try {
         id = o.get("id").getAsLong();
      } catch (Exception e) {
         return;
      }

      synchronized (log) {
         log.removeIf(m -> m.id() == id && m.kind() != GlobalChat.Kind.SYSTEM);
      }

      revision.incrementAndGet();
   }

   static void onError(String reason) {
      system("Not sent: " + reason);
   }

   static void system(String text) {
      GlobalChat.Message m = new GlobalChat.Message(systemIds.getAndDecrement(), null, "Clarity", text, System.currentTimeMillis(), GlobalChat.Kind.SYSTEM, false);
      add(m);
      showInGame(m);
   }

   private static void add(GlobalChat.Message m) {
      synchronized (log) {
         log.addLast(m);

         while (log.size() > 200) {
            log.removeFirst();
         }
      }

      revision.incrementAndGet();
   }

   private static GlobalChat.Message parse(JsonObject o) {
      try {
         long id = o.get("id").getAsLong();
         JsonObject from = o.getAsJsonObject("from");
         String uuid = from.get("uuid").getAsString();
         String name = from.get("name").getAsString();
         String text = clean(o.get("text").getAsString());
         long at = o.has("at") ? o.get("at").getAsLong() : System.currentTimeMillis();
         String me = PresenceClient.myUuid();
         boolean self = me != null && me.equalsIgnoreCase(uuid);
         return new GlobalChat.Message(id, uuid, name, text, at, self ? GlobalChat.Kind.SELF : GlobalChat.Kind.USER, !self && mentions(text));
      } catch (Exception e) {
         return null;
      }
   }

   private static boolean mentions(String text) {
      String me = PresenceClient.myName();
      return me != null && !me.isEmpty() ? Pattern.compile("(?i)(?<![A-Za-z0-9_])@?" + Pattern.quote(me) + "(?![A-Za-z0-9_])").matcher(text).find() : false;
   }

   static String clean(String s) {
      if (s == null) {
         return "";
      }

      StringBuilder b = new StringBuilder(s.length());
      boolean space = false;

      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         if (ch == 167) {
            i++;
         } else {
            if (ch < ' ' || ch >= 127 && ch <= 159 || ch >= 8203 && ch <= 8207 || ch >= 8232 && ch <= 8238 || ch >= 8294 && ch <= 8297) {
               if (ch != '\n' && ch != '\t') {
                  continue;
               }

               ch = ' ';
            }

            if (Character.isWhitespace(ch)) {
               if (!space && b.length() > 0) {
                  b.append(' ');
               }

               space = true;
            } else {
               space = false;
               b.append(ch);
            }
         }
      }

      return b.toString().trim();
   }

   public enum Kind {
      USER,
      SELF,
      SYSTEM;
   }

   public record Message(long id, String uuid, String name, String text, long at, GlobalChat.Kind kind, boolean mention) {
   }
}
