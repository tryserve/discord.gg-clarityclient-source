package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ReverseStep extends ModuleBase {
   public static ReverseStep INSTANCE;

   public ReverseStep() {
      super("Reverse Step", "Movement");
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static float getStepDownHeight() {
      return INSTANCE == null ? 1.0F : INSTANCE.getSetting("Step Down Height", 1.0F);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         LocalPlayer player = mc.player;
         if (player.onGround()) {
            double hSpeedSq = player.getDeltaMovement().x * player.getDeltaMovement().x + player.getDeltaMovement().z * player.getDeltaMovement().z;
            if (!(hSpeedSq < 1.0E-6)) {
               float stepDown = Math.max(1.0F, Math.min(3.0F, getStepDownHeight()));
               Level level = mc.level;
               AABB playerBox = player.getBoundingBox();

               for (int i = 1; i <= (int)stepDown; i++) {
                  AABB loweredBox = playerBox.move(0.0, -i, 0.0);
                  boolean spaceIsClear = level.noBlockCollision(player, loweredBox);
                  if (!spaceIsClear) {
                     if (i == 1) {
                        return;
                     }

                     applyStepDown(player, i - 1);
                     return;
                  }

                  BlockPos below = BlockPos.containing(player.getX(), player.getY() - i - 0.1, player.getZ());
                  if (!level.getBlockState(below).isAir()) {
                     applyStepDown(player, i);
                     return;
                  }
               }
            }
         }
      }
   }

   private static void applyStepDown(LocalPlayer player, int blocks) {
      double targetY = Math.floor(player.getY()) - blocks;
      player.setPos(player.getX(), targetY, player.getZ());
      player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
      player.resetFallDistance();
   }
}
