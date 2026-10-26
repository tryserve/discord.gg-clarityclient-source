package need.clarity.bridge;

import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.CrystalAura;
import need.clarity.modules.utility.FakePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class MultiPlayerGameModeBridge {
   private MultiPlayerGameModeBridge() {
   }

   public static float[] useItemHead(Player player) {
      try {
         if (!AimAssist.hasSilentRotation()) {
            return null;
         }

         float silentYaw = AimAssist.getSilentServerYaw();
         float silentPitch = AimAssist.getSilentServerPitch();
         float visualYaw = player.getYRot();
         float visualPitch = player.getXRot();
         float yd = (silentYaw - visualYaw) % 360.0F;
         if (yd >= 180.0F) {
            yd -= 360.0F;
         }

         if (yd < -180.0F) {
            yd += 360.0F;
         }

         float pd = (silentPitch - visualPitch) % 360.0F;
         if (pd >= 180.0F) {
            pd -= 360.0F;
         }

         if (pd < -180.0F) {
            pd += 360.0F;
         }

         if (Math.abs(yd) < 0.05F && Math.abs(pd) < 0.05F) {
            return null;
         }

         AimAssist.commitUseItemRotation(silentYaw, silentPitch);
         player.setYRot(silentYaw);
         player.setXRot(silentPitch);
         return new float[]{visualYaw, visualPitch};
      } catch (Throwable t) {
         return null;
      }
   }

   public static void useItemReturn(Player player, float[] saved) {
      try {
         if (saved == null) {
            return;
         }

         player.setYRot(saved[0]);
         player.setXRot(saved[1]);
      } catch (Throwable var3) {
      }
   }

   public static boolean attackHead(Player player, Entity target) {
      try {
         CrystalAura.onLocalAttack(target);
         if (FakePlayer.onLocalAttack(target)) {
            player.resetAttackStrengthTicker();
            return true;
         }
      } catch (Throwable var3) {
      }

      return false;
   }
}
