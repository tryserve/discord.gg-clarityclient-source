package need.clarity.bridge;

import need.clarity.PerPacketSendEvents;
import need.clarity.event.EventPacketSend;
import need.clarity.modules.movement.AntiHunger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public final class PacketListenerBridge {
   private PacketListenerBridge() {
   }

   public static boolean onPacketSend(Packet<?> packet) {
      try {
         EventPacketSend event = PerPacketSendEvents.fire(new EventPacketSend(packet));
         return event.isCancelled();
      } catch (Throwable t) {
         return false;
      }
   }

   public static Packet<?> antiHungerModify(Packet<?> packet) {
      try {
         if (packet instanceof ServerboundMovePlayerPacket mp) {
            AntiHunger module = AntiHunger.INSTANCE;
            return (Packet<?>)(module != null && module.isEnabled() && module.shouldSpoof() ? modifyOnGround(mp, false) : packet);
         } else {
            return packet;
         }
      } catch (Throwable t) {
         return packet;
      }
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
