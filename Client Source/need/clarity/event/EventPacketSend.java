package need.clarity.event;

import net.minecraft.network.protocol.Packet;

public class EventPacketSend {
   private final Packet<?> packet;
   private boolean cancelled;

   public EventPacketSend(Packet<?> packet) {
      this.packet = packet;
      this.cancelled = false;
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
