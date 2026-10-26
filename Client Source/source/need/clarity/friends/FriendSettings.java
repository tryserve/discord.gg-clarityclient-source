package need.clarity.friends;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import need.clarity.clickgui.GuiData;

public final class FriendSettings {
   public static final String GLOBAL = "§friends";
   public static final String FRIEND_PREFIX = "§friend:";
   public static final String PROTECT = "Protect";
   public static final String AVOID_BLAST = "Avoid Explosion Damage";
   public static final String BLOCK_HITS = "Block My Hits";
   public static final String COLOUR_NAME = "Colour Name";
   public static final String NAME_COLOUR = "Name Colour";
   public static final String USE_NICKNAME = "Use Nickname";
   public static final String NICKNAME = "Nickname";
   public static final String SHOW_TAG = "Show Tag";
   public static final String ESP_COLOUR = "Colour In ESP";
   public static final String JOIN_ALERTS = "Join Alerts";
   public static final String FAVOURITE = "Favourite";
   public static final String NOTES = "Notes";
   public static final String G_ENABLED = "Friends Enabled";
   public static final String G_PROTECT = "Protect Friends";
   public static final String G_BLAST = "Explosion Safety";
   public static final String G_MAX_DAMAGE = "Max Friend Damage";
   public static final String G_RECOLOUR = "Recolour Names";
   public static final String G_NAMETAGS = "Nametags";
   public static final String G_TABLIST = "Tab List";
   public static final String G_TAG = "Friend Tag";
   public static final String G_ESP = "ESP Colours";
   public static final String G_JOIN_ALERTS = "Join Alerts";
   public static final String G_SORT = "Sort By";
   public static final String G_ONLINE = "Online Features";
   public static final String G_STATUS = "Status";
   public static final String G_SHOW_SERVER = "Show Server";
   public static final String G_SHARE_WITH = "Share With";
   public static final String G_REQUESTS = "Friend Requests";
   public static final String G_ONLINE_ALERT = "Online Alerts";
   public static final String G_USER_COUNT = "Show User Count";
   public static final String G_SERVER_URL = "Presence Server";
   public static final String G_CHAT = "Global Chat";
   public static final String G_CHAT_INGAME = "Show In Game Chat";
   public static final String G_CHAT_PREFIX = "Chat Prefix";
   public static final String G_CHAT_FROM = "Show Messages From";
   public static final String G_CHAT_MENTION = "Mention Alerts";
   public static final String G_CHAT_TIMES = "Show Times";
   public static final String G_CLOUD_SYNC = "Auto Sync Linked Profiles";
   public static final String G_CLOUD_BACKUP = "Backup Before Loading";
   public static final String G_DM_INGAME = "Show DMs In Game Chat";
   public static final String G_DM_ALERTS = "DM Notifications";
   public static final String G_DM_PREFIX = "DM Prefix";
   public static final String G_CONFIRMED = "Confirmed Friends";
   public static final String G_CHAT_MUTED = "Chat Hidden Users";
   public static final List<String> STATUSES = List.of("Online", "Away", "Busy", "Appear Offline", "Invisible");
   public static final List<String> SHARE = List.of("Mutual Friends", "Anyone On My List");
   public static final List<String> REQUESTS = List.of("Anyone", "Nobody");
   public static final List<String> SORTS = List.of("Favourites First", "Online First", "Name", "Recently Added");
   public static final List<String> CHAT_FROM = List.of("Everyone", "Friends Only");
   public static final List<GuiData.Setting> PER_FRIEND = List.of(
      def("Protect", "check", true, null, null),
      def("Avoid Explosion Damage", "check", true, null, showif("Protect", true)),
      def("Block My Hits", "check", false, null, null),
      def("Colour Name", "check", true, null, null),
      def("Name Colour", "color", "#55FFFF", null, showif("Colour Name", true)),
      def("Use Nickname", "check", true, null, null),
      def("Nickname", "text", "", "Shown instead of their name", showif("Use Nickname", true)),
      def("Show Tag", "check", true, null, null),
      def("Colour In ESP", "check", true, null, null),
      def("Join Alerts", "check", true, null, null),
      def("Favourite", "check", false, null, null),
      def("Notes", "text", "", "Anything you want to remember", null)
   );
   public static final LinkedHashMap<String, List<GuiData.Setting>> GLOBAL_SECTIONS = new LinkedHashMap<>();
   public static final List<GuiData.Setting> CLOUD = List.of(def("Auto Sync Linked Profiles", "check", true, null, null), def("Backup Before Loading", "check", true, null, null));
   private static final Map<String, Object> PER_DEFAULTS = new HashMap<>();
   private static final Map<String, Object> GLOBAL_DEFAULTS = new HashMap<>();

   private FriendSettings() {
   }

   public static Object perFriendDefault(String key) {
      return PER_DEFAULTS.get(key);
   }

   public static Object globalDefault(String key) {
      return GLOBAL_DEFAULTS.get(key);
   }

   public static boolean isFriendModule(String module) {
      return module != null && module.startsWith("§friend:");
   }

   public static boolean isGlobalModule(String module) {
      return "§friends".equals(module);
   }

   public static boolean isFriendsModule(String module) {
      return isGlobalModule(module) || isFriendModule(module);
   }

   public static String friendModule(Friend f) {
      return "§friend:" + f.key();
   }

   private static Map<String, Object> showif(String key, Object value) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("showif", List.of(key, (String)value));
      return m;
   }

   private static GuiData.Setting def(String name, String type, Object a2, Object a3, Map<String, Object> meta) {
      List<Object> l = new ArrayList<>(List.of(name, type));
      l.add(a2);
      if (a3 != null) {
         l.add(a3);
      } else if (meta != null && "text".equals(type)) {
         l.add("");
      }

      if (meta != null) {
         l.add(new LinkedHashMap<>(meta));
      }

      return GuiData.Setting.of(l);
   }

   static {
      GLOBAL_SECTIONS.put(
         "General",
         List.of(
            def("Friends Enabled", "check", true, null, null),
            def("Protect Friends", "check", true, null, null),
            def("Explosion Safety", "check", true, null, showif("Protect Friends", true)),
            def("Max Friend Damage", "range", List.of(0.0, 20.0, 4.0, 0.5), null, showif("Explosion Safety", true)),
            def("Sort By", "select", SORTS, "Favourites First", null)
         )
      );
      GLOBAL_SECTIONS.put(
         "Display",
         List.of(
            def("Recolour Names", "check", true, null, null),
            def("Nametags", "check", true, null, null),
            def("Tab List", "check", true, null, null),
            def("Friend Tag", "text", "★", "Prefix for friends' names", null),
            def("ESP Colours", "check", true, null, null),
            def("Join Alerts", "check", true, null, null)
         )
      );
      GLOBAL_SECTIONS.put(
         "Online",
         List.of(
            def("Online Features", "check", true, null, null),
            def("Status", "select", STATUSES, "Online", showif("Online Features", true)),
            def("Show Server", "check", true, null, showif("Online Features", true)),
            def("Share With", "select", SHARE, "Mutual Friends", showif("Online Features", true)),
            def("Friend Requests", "select", REQUESTS, "Anyone", showif("Online Features", true)),
            def("Online Alerts", "check", true, null, showif("Online Features", true)),
            def("Show User Count", "check", true, null, showif("Online Features", true)),
            def("Presence Server", "text", "", "Default: wss://104-156-154-148.sslip.io", showif("Online Features", true))
         )
      );
      GLOBAL_SECTIONS.put(
         "Messages",
         List.of(
            def("DM Notifications", "check", true, null, null),
            def("Show DMs In Game Chat", "check", true, null, null),
            def("DM Prefix", "text", "@", "Type @Name message in Minecraft chat to DM a friend", null)
         )
      );
      GLOBAL_SECTIONS.put(
         "Chat",
         List.of(
            def("Global Chat", "check", true, null, null),
            def("Show In Game Chat", "check", true, null, showif("Global Chat", true)),
            def("Chat Prefix", "text", "#", "Start a Minecraft chat message with this to send it to Clarity chat instead", showif("Global Chat", true)),
            def("Show Messages From", "select", CHAT_FROM, "Everyone", showif("Global Chat", true)),
            def("Mention Alerts", "check", true, null, showif("Global Chat", true)),
            def("Show Times", "check", true, null, showif("Global Chat", true))
         )
      );

      for (GuiData.Setting s : PER_FRIEND) {
         PER_DEFAULTS.put(s.name, s.defaultValue());
      }

      for (List<GuiData.Setting> l : GLOBAL_SECTIONS.values()) {
         for (GuiData.Setting s : l) {
            GLOBAL_DEFAULTS.put(s.name, s.defaultValue());
         }
      }

      for (GuiData.Setting s : CLOUD) {
         GLOBAL_DEFAULTS.put(s.name, s.defaultValue());
      }
   }
}
