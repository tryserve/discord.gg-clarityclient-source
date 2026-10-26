package need.clarity.friends;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import need.clarity.Unload;
import need.clarity.friends.online.PresenceClient;
import need.clarity.modules.visual.Notifications;
import need.clarity.util.UiSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Friends {
   private static boolean registered = false;
   private static final Set<String> lastTab = new HashSet<>();
   private static final Set<String> lastNear = new HashSet<>();
   private static Object lastConnection;
   private static int tickCounter;

   private Friends() {
   }

   public static void init() {
      FriendManager.load();
      PresenceClient.init();
      if (!registered) {
         registered = true;
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
            if (!Unload.isUnloaded()) {
               try {
                  tick(mc);
               } catch (Throwable var2) {
               }
            }
         });
      }
   }

   public static void shutdown() {
      FriendManager.flushSync();
      PresenceClient.shutdown();
   }

   public static String uuidOf(Entity e) {
      try {
         return e.getUUID().toString();
      } catch (Throwable t) {
         return null;
      }
   }

   public static String nameOf(Player p) {
      try {
         return p.getGameProfile().name();
      } catch (Throwable t) {
         return null;
      }
   }

   public static Friend of(Entity e) {
      if (Unload.isUnloaded()) {
         return null;
      } else if (e instanceof Player p && FriendManager.enabled()) {
         Minecraft mc = Minecraft.getInstance();
         return mc != null && p == mc.player ? null : FriendManager.get(uuidOf(p), nameOf(p));
      } else {
         return null;
      }
   }

   public static boolean isFriend(Entity e) {
      return of(e) != null;
   }

   public static boolean isProtected(Entity e) {
      if (Unload.isUnloaded()) {
         return false;
      } else if (e instanceof Player p) {
         Minecraft mc = Minecraft.getInstance();
         return mc != null && p == mc.player ? false : FriendManager.isProtected(uuidOf(p), nameOf(p));
      } else {
         return false;
      }
   }

   public static boolean avoidExplosions(Entity e) {
      if (Unload.isUnloaded()) {
         return false;
      } else {
         return e instanceof Player p ? FriendManager.avoidExplosions(uuidOf(p), nameOf(p)) : false;
      }
   }

   public static boolean blockHits(Entity e) {
      if (Unload.isUnloaded()) {
         return false;
      } else {
         return e instanceof Player p ? FriendManager.blockHits(uuidOf(p), nameOf(p)) : false;
      }
   }

   public static List<Player> explosionSensitive(Minecraft mc, Vec3 from, double range) {
      if (mc.level != null && FriendManager.enabled() && FriendManager.count() != 0) {
         List<Player> out = null;
         double r2 = range * range;

         for (Player p : mc.level.players()) {
            if (p != mc.player && p.isAlive() && !(p.position().distanceToSqr(from) > r2) && avoidExplosions(p)) {
               if (out == null) {
                  out = new ArrayList<>(2);
               }

               out.add(p);
            }
         }

         return out == null ? List.of() : out;
      } else {
         return List.of();
      }
   }

   public static Component decorate(Component current, Friend f, String realName) {
      if (f != null && current != null) {
         int rgb = FriendManager.nameColour(f);
         String display = f.displayName();
         String tag = FriendManager.tag(f);
         boolean rename = !display.equals(realName);
         if (rgb < 0 && !rename && tag.isEmpty()) {
            return current;
         }

         String flat = current.getString();
         int at = realName != null && !realName.isEmpty() ? flat.indexOf(realName) : -1;
         Style base = current.getStyle();
         Style nameStyle = rgb >= 0 ? base.withColor(rgb) : base;
         Style tagStyle = Style.EMPTY.withColor(rgb >= 0 ? rgb : 5636095);
         MutableComponent out = Component.empty();
         if (!tag.isEmpty()) {
            out.append(Component.literal(tag + " ").setStyle(tagStyle));
         }

         if (at < 0) {
            if (tag.isEmpty() && rgb < 0) {
               return current;
            }

            out.append(rgb >= 0 && !rename ? current.copy().setStyle(nameStyle) : current.copy());
            return out;
         } else {
            if (at > 0) {
               out.append(Component.literal(flat.substring(0, at)).setStyle(base));
            }

            out.append(Component.literal(display).setStyle(nameStyle));
            String after = flat.substring(at + realName.length());
            if (!after.isEmpty()) {
               out.append(Component.literal(after).setStyle(base));
            }

            return out;
         }
      } else {
         return current;
      }
   }

   private static void tick(Minecraft mc) {
      PresenceClient.clientTick(mc);
      if (mc.player != null && mc.getConnection() != null && mc.level != null) {
         if (++tickCounter % 10 == 0) {
            if (FriendManager.enabled()) {
               boolean fresh = lastConnection != mc.getConnection();
               lastConnection = mc.getConnection();
               boolean alerts = FriendManager.bool("Join Alerts");
               Set<String> nowTab = new HashSet<>();

               for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                  String uuid = info.getProfile().id().toString();
                  String name = info.getProfile().name();
                  FriendManager.observe(uuid, name);
                  Friend f = FriendManager.get(uuid, name);
                  if (f != null && !uuid.equals(mc.player.getUUID().toString())) {
                     nowTab.add(f.key());
                     if (!fresh && alerts && f.bool("Join Alerts") && !lastTab.contains(f.key())) {
                        alert(f.displayName(), "joined the server", true);
                     }
                  }
               }

               if (!fresh && alerts) {
                  for (String k : lastTab) {
                     if (!nowTab.contains(k)) {
                        Friend f = FriendManager.byKey(k);
                        if (f != null && f.bool("Join Alerts")) {
                           alert(f.displayName(), "left the server", false);
                        }
                     }
                  }
               }

               lastTab.clear();
               lastTab.addAll(nowTab);
               Set<String> nowNear = new HashSet<>();

               for (Player p : mc.level.players()) {
                  if (p != mc.player) {
                     Friend f = FriendManager.get(uuidOf(p), nameOf(p));
                     if (f != null) {
                        nowNear.add(f.key());
                        if (!fresh && alerts && f.bool("Join Alerts") && !lastNear.contains(f.key())) {
                           alert(f.displayName(), "is nearby", true);
                        }
                     }
                  }
               }

               lastNear.clear();
               lastNear.addAll(nowNear);
            }
         }
      } else {
         lastTab.clear();
         lastNear.clear();
         lastConnection = null;
      }
   }

   public static boolean inCurrentServer(Friend f) {
      return f != null && lastTab.contains(f.key());
   }

   public static boolean nearby(Friend f) {
      return f != null && lastNear.contains(f.key());
   }

   public static void alert(String title, String text, boolean positive) {
      alert(title, text, positive, null);
   }

   public static void alert(String title, String text, boolean positive, UiSounds.Sound sound) {
      if (sound != null) {
         UiSounds.play(sound);
      }

      try {
         Notifications n = Notifications.INSTANCE;
         if (n != null && n.isEnabled()) {
            Notifications.push(title, text, positive, sound != null);
            return;
         }
      } catch (Throwable var5) {
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.sendSystemMessage(Component.literal("§b[Friends]§r " + title + " " + text));
            }
         });
      }
   }
}
