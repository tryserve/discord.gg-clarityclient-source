package need.clarity.mixin;

import need.clarity.modules.utility.AntiGuiClose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class AntiGuiClosePacketMixin {
   @Redirect(method = "handleContainerClose", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;clientSideCloseContainer()V"))
   private void cl$serverClose(LocalPlayer player) {
      boolean handled = false;

      try {
         handled = AntiGuiClose.onServerClose();
      } catch (Throwable var4) {
      }

      if (!handled) {
         player.clientSideCloseContainer();
      }
   }

   @Inject(method = "handleOpenScreen", at = @At("HEAD"))
   private void cl$openStart(ClientboundOpenScreenPacket p, CallbackInfo ci) {
      if (Minecraft.getInstance().isSameThread()) {
         AntiGuiClose.serverOpening = true;
      }
   }

   @Inject(method = "handleOpenScreen", at = @At("RETURN"))
   private void cl$openEnd(ClientboundOpenScreenPacket p, CallbackInfo ci) {
      AntiGuiClose.serverOpening = false;
   }
}
