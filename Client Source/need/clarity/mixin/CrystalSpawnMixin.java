package need.clarity.mixin;

import need.clarity.modules.combat.CrystalAura;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class CrystalSpawnMixin {
   @Inject(method = "handleAddEntity", at = @At("TAIL"))
   private void clarity$onCrystalSpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
      CrystalAura.onEntitySpawn(packet.getId());
   }
}
