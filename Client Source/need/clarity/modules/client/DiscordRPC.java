package need.clarity.modules.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.util.DiscordIpc;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DiscordRPC extends ModuleBase {
   public static DiscordRPC INSTANCE;
   private final DiscordIpc ipc = new DiscordIpc();
   private long sessionStart = System.currentTimeMillis();
   private long serverStart = 0L;
   private String lastServer = "";
   private String lastSig = "";
   private int tick = 0;
   private DiscordIpc.State shownState = DiscordIpc.State.OFF;
   private static String builtIn;

   public DiscordRPC() {
      super("Discord RPC", "Client");
      INSTANCE = this;
   }

   private String appId() {
      String custom = this.getSetting("Custom App ID", this.getSetting("Application ID", "")).trim();
      return custom.matches("\\d{15,21}") ? custom : builtInId();
   }

   public static String builtInId() {
      if (builtIn != null) {
         return builtIn;
      }

      String id = "";

      try (InputStream in = DiscordRPC.class.getResourceAsStream("/assets/clarity/discord_app_id.txt")) {
         if (in != null) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
               String t = line.trim();
               if (t.matches("\\d{15,21}")) {
                  id = t;
                  break;
               }
            }
         }
      } catch (Exception var9) {
      }

      builtIn = id;
      return id;
   }

   @Override
   public void onEnable() {
      this.sessionStart = System.currentTimeMillis();
      this.lastSig = "";
      String id = this.appId();
      if (!id.matches("\\d{15,21}")) {
         note("Discord RPC: this build has no Application ID (see discord_app_id.txt)", false);
      } else {
         this.ipc.start(id);
      }
   }

   @Override
   public void onDisable() {
      this.ipc.stop();
      this.shownState = DiscordIpc.State.OFF;
   }

   public DiscordIpc.State state() {
      return this.ipc.state();
   }

   public String status() {
      return switch (this.ipc.state()) {
         case CONNECTED -> "Connected" + (this.ipc.user().isEmpty() ? "" : " as " + this.ipc.user());
         case CONNECTING -> "Connecting…";
         case NO_DISCORD -> this.ipc.lastError();
         case ERROR -> "Error: " + this.ipc.lastError();
         default -> "Off";
      };
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         DiscordIpc.State st = this.ipc.state();
         if (st != this.shownState) {
            if (st == DiscordIpc.State.CONNECTED) {
               note("Discord RPC connected" + (this.ipc.user().isEmpty() ? "" : " (" + this.ipc.user() + ")"), true);
            } else if (st == DiscordIpc.State.NO_DISCORD && this.shownState == DiscordIpc.State.CONNECTING) {
               note("Discord RPC: Discord not found", false);
            } else if (st == DiscordIpc.State.ERROR) {
               note("Discord RPC: " + this.ipc.lastError(), false);
            }

            this.shownState = st;
         }

         if (++this.tick % 20 == 0) {
            if (this.ipc.state() == DiscordIpc.State.OFF && this.appId().matches("\\d{15,21}")) {
               this.ipc.start(this.appId());
            }

            JsonObject activity = this.build(Minecraft.getInstance());
            String sig = activity.toString();
            if (!sig.equals(this.lastSig)) {
               this.lastSig = sig;
               this.ipc.setActivity(activity);
            }
         }
      }
   }

   private JsonObject build(Minecraft mc) {
      String server = this.serverKey(mc);
      if (!server.equals(this.lastServer)) {
         this.lastServer = server;
         this.serverStart = System.currentTimeMillis();
      }

      JsonObject a = new JsonObject();
      put(a, "details", this.fill(this.getSetting("Details", "{status}"), mc));
      put(a, "state", this.fill(this.getSetting("State", "{health} · {ping}"), mc));
      String timer = this.getSetting("Timer", "Session");
      if (!"Off".equals(timer)) {
         JsonObject ts = new JsonObject();
         ts.addProperty("start", "Server".equals(timer) && mc.level != null ? this.serverStart : this.sessionStart);
         a.add("timestamps", ts);
      }

      JsonObject assets = new JsonObject();
      put(assets, "large_image", this.getSetting("Large Image", "").trim());
      put(assets, "large_text", this.fill(this.getSetting("Large Text", "Clarity {version}"), mc));
      put(assets, "small_image", this.getSetting("Small Image", "").trim());
      put(assets, "small_text", this.fill(this.getSetting("Small Text", "{modules} modules on"), mc));
      if (assets.has("large_image") || assets.has("small_image")) {
         a.add("assets", assets);
      }

      JsonArray buttons = new JsonArray();

      for (int i = 1; i <= 2; i++) {
         String label = this.getSetting("Button " + i + " Label", "").trim();
         String url = this.getSetting("Button " + i + " URL", "").trim();
         if (!label.isEmpty() && url.matches("(?i)https?://\\S+")) {
            JsonObject b = new JsonObject();
            b.addProperty("label", cut(label, 32));
            b.addProperty("url", url);
            buttons.add(b);
         }
      }

      if (!buttons.isEmpty()) {
         a.add("buttons", buttons);
      }

      a.addProperty("instance", false);
      return a;
   }

   private String fill(String template, Minecraft mc) {
      if (template != null && !template.isEmpty()) {
         boolean inGame = mc.level != null && mc.player != null;
         String server = this.serverLabel(mc);
         String status = !inGame ? "In the main menu" : (mc.isLocalServer() ? "Singleplayer" : "Playing on " + server);
         String s = template.replace("{status}", status)
            .replace("{server}", inGame ? server : "Main menu")
            .replace("{players}", inGame && mc.getConnection() != null ? String.valueOf(mc.getConnection().getOnlinePlayers().size()) : "0")
            .replace("{health}", inGame ? String.format(Locale.ROOT, "%.0f♥", mc.player.getHealth() / 2.0F) : "")
            .replace("{hp}", inGame ? String.format(Locale.ROOT, "%.0f", mc.player.getHealth()) : "")
            .replace("{dimension}", inGame ? dimension(mc) : "")
            .replace("{ping}", inGame && !mc.isLocalServer() ? ping(mc) + "ms" : "")
            .replace("{fps}", String.valueOf(mc.getFps()))
            .replace("{username}", mc.getUser() != null ? mc.getUser().getName() : "")
            .replace("{modules}", String.valueOf(enabledModules()))
            .replace("{version}", version());
         return s.replaceAll("(\\s*·\\s*)+$", "").replaceAll("^(\\s*·\\s*)+", "").replaceAll("(\\s*·\\s*){2,}", " · ").trim();
      } else {
         return "";
      }
   }

   private String serverKey(Minecraft mc) {
      if (mc.level == null) {
         return "";
      }

      if (mc.isLocalServer()) {
         return "local";
      }

      ServerData sd = mc.getCurrentServer();
      return sd == null ? "?" : sd.ip;
   }

   private String serverLabel(Minecraft mc) {
      if (this.getSetting("Hide Server IP", false)) {
         return "Multiplayer";
      }

      ServerData sd = mc.getCurrentServer();
      return sd == null ? "Multiplayer" : sd.ip;
   }

   private static String dimension(Minecraft mc) {
      try {
         ResourceKey<Level> dim = mc.level.dimension();
         if (dim == Level.OVERWORLD) {
            return "Overworld";
         }

         if (dim == Level.NETHER) {
            return "Nether";
         }

         if (dim == Level.END) {
            return "The End";
         }
      } catch (Throwable var2) {
      }

      return "";
   }

   private static int ping(Minecraft mc) {
      try {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info == null ? 0 : info.getLatency();
      } catch (Throwable t) {
         return 0;
      }
   }

   private static int enabledModules() {
      int n = 0;

      try {
         for (ModuleBase m : ModuleRegistry.all()) {
            if (m.isEnabled()) {
               n++;
            }
         }
      } catch (Throwable var3) {
      }

      return n;
   }

   private static String version() {
      try {
         return FabricLoader.getInstance().getModContainer("clarity").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
      } catch (Throwable t) {
         return "";
      }
   }

   private static void put(JsonObject o, String k, String v) {
      if (v != null) {
         v = v.trim();
         if (v.length() >= 2) {
            o.addProperty(k, cut(v, 128));
         }
      }
   }

   private static String cut(String s, int max) {
      return s.length() <= max ? s : s.substring(0, max - 1) + "…";
   }

   private static void note(String text, boolean positive) {
      try {
         Friends.alert("Discord RPC", text.replace("Discord RPC: ", "").replace("Discord RPC ", ""), positive);
      } catch (Throwable var3) {
      }
   }
}
