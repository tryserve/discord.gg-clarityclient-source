package need.clarity.modules.combat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.FakePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

public class AntiBot extends ModuleBase {
   private static AntiBot instance;
   private static final Set<UUID> manuallyFlagged = ConcurrentHashMap.newKeySet();

   public AntiBot() {
      super("Anti Bot", "Combat");
      instance = this;
   }

   public static boolean isBot(LivingEntity entity) {
      AntiBot inst = instance;
      if (inst == null || !inst.isEnabled()) {
         return false;
      }

      if (entity instanceof Player player) {
         if (entity == Minecraft.getInstance().player) {
            return false;
         }

         if (FakePlayer.isFake(entity)) {
            return false;
         }

         if (manuallyFlagged.contains(entity.getUUID())) {
            return true;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (inst.getSetting("Not in Tab List", true) && info == null) {
               return true;
            }

            if (info != null) {
               if (inst.getSetting("Invalid Ping", true)) {
                  int latency = info.getLatency();
                  if (latency == 0) {
                     return true;
                  }
               }

               if (inst.getSetting("No Skin", false)) {
                  try {
                     PlayerSkin skin = info.getSkin();
                     if (skin == null || skin.body() == null) {
                        return true;
                     }
                  } catch (Exception ignored) {
                     return true;
                  }
               }
            }

            if (inst.getSetting("Invalid UUID", true)) {
               UUID uuid = entity.getUUID();
               int version = (int)(uuid.getMostSignificantBits() >> 12 & 15L);
               if (version != 4) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void toggleManualFlag(UUID uuid) {
      if (!manuallyFlagged.remove(uuid)) {
         manuallyFlagged.add(uuid);
      }
   }

   public static void clearManualFlags() {
      manuallyFlagged.clear();
   }

   @Override
   public void onDisable() {
   }
}
