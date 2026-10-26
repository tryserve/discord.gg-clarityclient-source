package need.clarity.mixin;

import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
   @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
   private static <T extends PacketListener> void cl$onPacketReceive(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      EventPacketReceive event = PerPacketEvents.fire(new EventPacketReceive(packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
