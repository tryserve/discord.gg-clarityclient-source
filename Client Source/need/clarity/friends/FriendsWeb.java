package need.clarity.friends;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map.Entry;
import need.clarity.clickgui.GuiData;
import need.clarity.friends.online.PresenceClient;

public final class FriendsWeb {
   private static final Gson GSON = new Gson();

   private FriendsWeb() {
   }

   public static String model() {
      JsonObject o = new JsonObject();
      o.addProperty("rev", FriendManager.revision());
      JsonObject pr = new JsonObject();
      pr.addProperty("state", PresenceClient.state().name());
      pr.addProperty("line", PresenceClient.statusLine());
      pr.addProperty("count", PresenceClient.userCount());
      pr.addProperty("connected", PresenceClient.state() == PresenceClient.State.CONNECTED);
      o.add("presence", pr);
      JsonObject settings = new JsonObject();
      JsonArray sections = new JsonArray();

      for (Entry<String, List<GuiData.Setting>> sec : FriendSettings.GLOBAL_SECTIONS.entrySet()) {
         JsonArray pair = new JsonArray();
         pair.add(sec.getKey());
         JsonArray list = new JsonArray();

         for (GuiData.Setting s : sec.getValue()) {
            list.add(GSON.toJsonTree(s.raw));
            settings.add(s.name, GSON.toJsonTree(FriendManager.setting(s.name)));
         }

         pair.add(list);
         sections.add(pair);
      }

      o.add("settings", settings);
      JsonObject schema = new JsonObject();
      schema.add("global", sections);
      JsonArray per = new JsonArray();

      for (GuiData.Setting s : FriendSettings.PER_FRIEND) {
         per.add(GSON.toJsonTree(s.raw));
      }

      schema.add("friend", per);
      o.add("schema", schema);
      JsonArray friends = new JsonArray();

      for (Friend f : FriendManager.sorted(FriendsWeb::online)) {
         JsonObject fo = new JsonObject();
         fo.addProperty("key", f.key());
         fo.addProperty("uuid", f.uuid());
         fo.addProperty("name", f.name());
         fo.addProperty("display", f.displayName());
         int rgb = FriendManager.nameColour(f);
         fo.addProperty("color", rgb < 0 ? null : String.format("#%06x", rgb));
         fo.addProperty("status", statusText(f));
         fo.addProperty("statusColor", String.format("#%06x", statusRgb(f)));
         fo.addProperty("online", online(f));
         fo.addProperty("favourite", f.favourite());
         fo.addProperty("addedAt", f.addedAt());
         JsonObject opts = new JsonObject();

         for (GuiData.Setting s : FriendSettings.PER_FRIEND) {
            opts.add(s.name, GSON.toJsonTree(f.option(s.name)));
         }

         fo.add("options", opts);
         friends.add(fo);
      }

      o.add("friends", friends);
      JsonArray reqs = new JsonArray();

      for (PresenceClient.Request r : PresenceClient.incomingRequests()) {
         JsonObject ro = new JsonObject();
         ro.addProperty("uuid", r.uuid());
         ro.addProperty("name", r.name());
         reqs.add(ro);
      }

      o.add("requests", reqs);
      JsonArray out = new JsonArray();

      for (String n : PresenceClient.outgoingRequests()) {
         out.add(n);
      }

      o.add("outgoing", out);
      return GSON.toJson(o);
   }

   public static String apply(String body) {
      String msg = null;
      boolean ok = true;

      try {
         JsonObject a = JsonParser.parseString(body).getAsJsonObject();
         String action = str(a, "action");
         label97:
         switch (action) {
            case "add":
               String name = str(a, "name").trim();
               switch (FriendManager.add(null, name)) {
                  case ADDED:
                     msg = "Added " + name;
                     break label97;
                  case ALREADY:
                     msg = name + " is already a friend";
                     break label97;
                  default:
                     ok = false;
                     msg = "That isn't a valid Minecraft name";
                     break label97;
               }
            case "remove":
               ok = FriendManager.removeByKey(str(a, "key"));
               msg = ok ? "Removed" : "Not found";
               break;
            case "set":
               FriendManager.setFriendOption(str(a, "key"), str(a, "option"), value(a.get("value")));
               break;
            case "global":
               FriendManager.setSetting(str(a, "option"), value(a.get("value")));
               break;
            case "reset":
               FriendManager.resetFriend(str(a, "key"));
               break;
            case "clear":
               FriendManager.clear();
               msg = "Friends list cleared";
               break;
            case "request":
               msg = PresenceClient.sendRequest(str(a, "name"));
               ok = msg == null;
               if (ok) {
                  msg = "Request sent to " + str(a, "name");
               }
               break;
            case "respond":
               String uuid = str(a, "uuid");
               boolean accept = a.has("accept") && a.get("accept").getAsBoolean();

               for (PresenceClient.Request r : PresenceClient.incomingRequests()) {
                  if (r.uuid().equals(uuid)) {
                     PresenceClient.respond(r, accept);
                     msg = accept ? r.name() + " is now your friend" : "Declined " + r.name();
                  }
               }
               break;
            case "cancel":
               PresenceClient.cancelRequest(str(a, "name"));
               break;
            default:
               ok = false;
               msg = "Unknown action";
         }
      } catch (Exception e) {
         ok = false;
         msg = "Bad request";
      }

      JsonObject res = new JsonObject();
      res.addProperty("ok", ok);
      if (msg != null) {
         res.addProperty("msg", msg);
      }

      res.add("model", JsonParser.parseString(model()));
      return GSON.toJson(res);
   }

   static boolean online(Friend f) {
      return !PresenceClient.confirmed(f) ? false : PresenceClient.onlineOnClarity(f) || Friends.inCurrentServer(f);
   }

   static String statusText(Friend f) {
      if (!PresenceClient.confirmed(f)) {
         return "Waiting for them to add you back";
      }

      if (Friends.nearby(f)) {
         return "Nearby";
      }

      if (Friends.inCurrentServer(f)) {
         return "In your server";
      }

      PresenceClient.Presence p = PresenceClient.presenceOf(f);
      if (p != null && p.online()) {
         String st = switch (p.status()) {
            case "away" -> "Away";
            case "busy" -> "Busy";
            default -> "Online";
         };
         return p.server() != null && !p.server().isEmpty() ? st + " on " + p.server() : st + " on Clarity";
      } else {
         return f.hasUuid() ? "Offline" : "Not seen yet";
      }
   }

   static int statusRgb(Friend f) {
      if (!PresenceClient.confirmed(f)) {
         return 5922664;
      }

      if (!Friends.nearby(f) && !Friends.inCurrentServer(f)) {
         PresenceClient.Presence p = PresenceClient.presenceOf(f);
         if (p != null && p.online()) {
            return switch (p.status()) {
               case "away" -> 16103485;
               case "busy" -> 16733296;
               default -> 3921530;
            };
         } else {
            return 9080728;
         }
      } else {
         return 3921530;
      }
   }

   private static Object value(JsonElement e) {
      if (e != null && !e.isJsonNull() && e.isJsonPrimitive()) {
         JsonPrimitive p = e.getAsJsonPrimitive();
         if (p.isBoolean()) {
            return p.getAsBoolean();
         } else {
            return p.isNumber() ? p.getAsDouble() : p.getAsString();
         }
      } else {
         return null;
      }
   }

   private static String str(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
   }
}
