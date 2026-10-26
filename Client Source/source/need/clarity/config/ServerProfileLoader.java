package need.clarity.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import need.clarity.Unload;
import need.clarity.modules.ModuleRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;

public final class ServerProfileLoader {
   private static final int DEFAULT_PORT = 25565;
   private static volatile boolean registered = false;

   private ServerProfileLoader() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
               if (!Unload.isUnloaded()) {
                  try {
                     applyForConnection(handler);
                  } catch (Throwable var4) {
                  }
               }
            });
         } catch (Throwable var1) {
         }
      }
   }

   private static void applyForConnection(ClientPacketListener handler) {
      if (handler != null) {
         List<String> candidates = candidates(resolveRawAddress(handler));
         if (!candidates.isEmpty()) {
            JsonObject config = ClarityConfig.getCurrent();
            if (config != null) {
               JsonObject serverProfiles = asObject(config.get("__serverProfiles"));
               if (serverProfiles != null && serverProfiles.size() != 0) {
                  String profileName = matchProfile(serverProfiles, candidates);
                  if (profileName != null && !profileName.isEmpty()) {
                     JsonObject profiles = asObject(config.get("__profiles"));
                     if (profiles != null) {
                        JsonObject profile = asObject(profiles.get(profileName));
                        if (profile != null) {
                           ClarityConfig.applyProfileSnapshot(profile);

                           try {
                              ClarityConfig.setActiveProfile(profileName);
                           } catch (Throwable var8) {
                           }

                           ModuleRegistry.syncFromConfig();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static String resolveRawAddress(ClientPacketListener handler) {
      try {
         ServerData data = handler.getServerData();
         if (data != null && data.ip != null && !data.ip.isBlank()) {
            return data.ip;
         }
      } catch (Throwable var4) {
      }

      try {
         Connection conn = handler.getConnection();
         if (conn != null) {
            SocketAddress addr = conn.getRemoteAddress();
            if (addr != null) {
               return addr.toString();
            }
         }
      } catch (Throwable var3) {
      }

      return null;
   }

   private static List<String> candidates(String raw) {
      List<String> out = new ArrayList<>(3);
      String normalised = normalise(raw);
      if (normalised == null) {
         return out;
      }

      addUnique(out, normalised);
      String host = bareHost(normalised);
      if (host != null && !host.isEmpty()) {
         addUnique(out, host);
         if (!hasPort(normalised)) {
            addUnique(out, host + ":25565");
         }
      }

      return out;
   }

   private static void addUnique(List<String> list, String value) {
      if (value != null && !value.isEmpty() && !list.contains(value)) {
         list.add(value);
      }
   }

   private static String normalise(String addr) {
      if (addr == null) {
         return null;
      }

      String s = addr.trim();
      if (s.isEmpty()) {
         return null;
      }

      int slash = s.lastIndexOf(47);
      if (slash >= 0 && slash < s.length() - 1) {
         s = s.substring(slash + 1);
      }

      return s.toLowerCase(Locale.ROOT);
   }

   private static String bareHost(String s) {
      if (s == null) {
         return null;
      }

      if (s.startsWith("[")) {
         int end = s.indexOf(93);
         if (end > 0) {
            return s.substring(0, end + 1);
         }
      }

      int colon = s.indexOf(58);
      return colon > 0 ? s.substring(0, colon) : s;
   }

   private static boolean hasPort(String s) {
      if (s == null) {
         return false;
      }

      if (!s.startsWith("[")) {
         return s.indexOf(58) >= 0;
      }

      int end = s.indexOf(93);
      return end >= 0 && end + 1 < s.length() && s.charAt(end + 1) == ':';
   }

   private static String matchProfile(JsonObject serverProfiles, List<String> candidates) {
      for (String candidate : candidates) {
         for (Entry<String, JsonElement> entry : serverProfiles.entrySet()) {
            String key = normalise(entry.getKey());
            if (key != null && key.equals(candidate)) {
               JsonElement value = entry.getValue();
               if (value != null && value.isJsonPrimitive()) {
                  return value.getAsString();
               }
            }
         }
      }

      return null;
   }

   private static JsonObject asObject(JsonElement el) {
      return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
   }
}
