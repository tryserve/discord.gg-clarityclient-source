package need.clarity.bridge;

import need.clarity.modules.combat.CrystalAura;
import need.clarity.modules.movement.NoSlow;
import need.clarity.modules.utility.AntiGuiClose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GameplayBridge {
   private GameplayBridge() {
   }

   public static boolean antiGuiCloseSetScreen(Minecraft mc, Screen incoming) {
      Screen current = mc.screen;

      Screen result;
      try {
         result = AntiGuiClose.redirect(current, incoming);
      } catch (Throwable t) {
         return false;
      }

      if (result == incoming) {
         return false;
      }

      if (result == current) {
         return true;
      }

      AntiGuiClose.beginRestore();

      try {
         mc.setScreen(result);
      } finally {
         AntiGuiClose.endRestore();
      }

      return true;
   }

   public static void serverClose(LocalPlayer player) {
      boolean handled = false;

      try {
         handled = AntiGuiClose.onServerClose();
      } catch (Throwable var3) {
      }

      if (!handled) {
         player.clientSideCloseContainer();
      }
   }

   public static void openScreenStart() {
      try {
         if (Minecraft.getInstance().isSameThread()) {
            AntiGuiClose.serverOpening = true;
         }
      } catch (Throwable var1) {
      }
   }

   public static void openScreenEnd() {
      try {
         AntiGuiClose.serverOpening = false;
      } catch (Throwable var1) {
      }
   }

   public static void onEntityAdded(ClientboundAddEntityPacket packet) {
      try {
         CrystalAura.onEntitySpawn(packet.getId());
      } catch (Throwable var2) {
      }
   }

   private static boolean isLocalPlayer(Entity e) {
      Minecraft mc = Minecraft.getInstance();
      return mc != null && mc.player != null && e == mc.player;
   }

   private static boolean standingOn(Entity self, Block block) {
      try {
         if (self.level().getBlockState(self.blockPosition()).is(block)) {
            return true;
         }

         BlockPos below = BlockPos.containing(self.getX(), self.getY() - 0.5000001, self.getZ());
         return self.level().getBlockState(below).is(block);
      } catch (Throwable t) {
         return false;
      }
   }

   public static float noSlowSpeed(float f, Entity self) {
      try {
         if (f >= 1.0F || !isLocalPlayer(self)) {
            return f;
         }

         if (standingOn(self, Blocks.SOUL_SAND) && NoSlow.skipEnv("Soul Sand") || standingOn(self, Blocks.HONEY_BLOCK) && NoSlow.skipEnv("Honey")) {
            return 1.0F;
         }
      } catch (Throwable var3) {
      }

      return f;
   }

   public static float noSlowJump(float f, Entity self) {
      try {
         if (f >= 1.0F || !isLocalPlayer(self)) {
            return f;
         }

         if (standingOn(self, Blocks.HONEY_BLOCK) && NoSlow.skipEnv("Honey")) {
            return 1.0F;
         }
      } catch (Throwable var3) {
      }

      return f;
   }

   public static boolean noSlowStuck(Entity self, BlockState state) {
      try {
         return state != null && isLocalPlayer(self)
            ? state.is(Blocks.COBWEB) && NoSlow.skipEnv("Cobwebs")
               || state.is(Blocks.SWEET_BERRY_BUSH) && NoSlow.skipEnv("Berry Bushes")
               || state.is(Blocks.POWDER_SNOW) && NoSlow.skipEnv("Powder Snow")
            : false;
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean noSlowSlime(Entity entity) {
      try {
         return isLocalPlayer(entity) && NoSlow.skipEnv("Slime");
      } catch (Throwable t) {
         return false;
      }
   }
}
