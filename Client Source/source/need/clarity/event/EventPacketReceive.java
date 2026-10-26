package need.clarity.event;

import net.minecraft.network.protocol.Packet;

public final class EventPacketReceive {
   private final Packet<?> packet;
   private boolean cancelled;

   public EventPacketReceive(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }
}
