package need.clarity.mixin;

import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public class XCarryMixin {
   @Redirect(method = "closeContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
   private void xcarry$redirectSend(ClientPacketListener connection, Packet<?> packet) {
      LocalPlayer self = (LocalPlayer)(Object)this;
      ModuleBase xcarry = ModuleRegistry.get("XCarry");
      if (xcarry == null || !xcarry.isEnabled() || self.containerMenu.containerId != 0) {
         connection.send(packet);
      }
   }
}
