package need.clarity.bridge;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.Reach;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PlayerBridge {
   private PlayerBridge() {
   }

   public static Vec3 redirectSwimmingLookVector(Player player) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (player == mc.player && LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix()) {
            return AimAssist.getRotationVector(LocalPlayerRotationState.tickServerPitch, LocalPlayerRotationState.tickServerYaw);
         }
      } catch (Throwable var2) {
      }

      return null;
   }

   public static double blockReach(Player player) {
      try {
         if (player != Minecraft.getInstance().player) {
            return -1.0;
         }

         float r = Reach.getBlockReach();
         return r > 0.0F ? r : -1.0;
      } catch (Throwable t) {
         return -1.0;
      }
   }

   public static double entityReach(Player player) {
      try {
         if (player != Minecraft.getInstance().player) {
            return -1.0;
         }

         float r = Reach.getEntityReach();
         return r > 0.0F ? r : -1.0;
      } catch (Throwable t) {
         return -1.0;
      }
   }
}
