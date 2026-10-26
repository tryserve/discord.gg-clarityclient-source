package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public class NoFall extends ModuleBase {
   private boolean wasOnGround = false;

   public NoFall() {
      super("NoFall", "Movement");
   }

   @Override
   public void onEnable() {
      this.wasOnGround = false;
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (!mc.player.getAbilities().invulnerable) {
            String mode = this.getSetting("Mode", "Normal");
            if ("Grim".equals(mode)) {
               this.tickGrim(mc);
            } else {
               this.tickNormal(mc);
            }

            this.wasOnGround = mc.player.onGround();
         }
      }
   }

   private void tickNormal(Minecraft mc) {
      float fallDistance = (float)mc.player.fallDistance;
      if (!(fallDistance <= 0.0F)) {
         boolean onlyLethal = this.getSetting("Only Lethal", true);
         if (onlyLethal) {
            float potentialDamage = fallDistance - 3.0F;
            if (potentialDamage < mc.player.getHealth()) {
               return;
            }
         }

         mc.player.connection.send(new StatusOnly(true, mc.player.horizontalCollision));
      }
   }

   private void tickGrim(Minecraft mc) {
      boolean onGround = mc.player.onGround();
      float fallDistance = (float)mc.player.fallDistance;
      if (!this.wasOnGround && onGround && fallDistance > 0.0F) {
         boolean onlyLethal = this.getSetting("Only Lethal", true);
         if (onlyLethal) {
            float potentialDamage = fallDistance - 3.0F;
            if (potentialDamage < mc.player.getHealth()) {
               return;
            }
         }

         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();
         mc.player.connection.send(new Pos(x, y + 1.0E-7, z, false, mc.player.horizontalCollision));
         mc.player.fallDistance = 0.0;
      }
   }
}
