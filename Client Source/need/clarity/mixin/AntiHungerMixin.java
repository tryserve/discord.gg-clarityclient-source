package need.clarity.mixin;

import need.clarity.modules.movement.AntiHunger;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientCommonPacketListenerImpl.class)
public class AntiHungerMixin {
   @Redirect(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
   private void cl$antiHungerSend(Connection connection, Packet<?> packet) {
      if (packet instanceof ServerboundMovePlayerPacket mp) {
         AntiHunger module = AntiHunger.INSTANCE;
         if (module != null && module.isEnabled() && module.shouldSpoof()) {
            packet = modifyOnGround(mp, false);
         }
      }

      connection.send(packet);
   }

   private static ServerboundMovePlayerPacket modifyOnGround(ServerboundMovePlayerPacket packet, boolean onGround) {
      if (packet instanceof PosRot p) {
         return new PosRot(p.getX(0.0), p.getY(0.0), p.getZ(0.0), p.getYRot(0.0F), p.getXRot(0.0F), onGround, p.horizontalCollision());
      } else if (packet instanceof Pos p) {
         return new Pos(p.getX(0.0), p.getY(0.0), p.getZ(0.0), onGround, p.horizontalCollision());
      } else {
         return (ServerboundMovePlayerPacket)(packet instanceof Rot p
            ? new Rot(p.getYRot(0.0F), p.getXRot(0.0F), onGround, p.horizontalCollision())
            : new StatusOnly(onGround, packet.horizontalCollision()));
      }
   }
}
