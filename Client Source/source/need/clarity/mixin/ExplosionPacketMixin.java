package need.clarity.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import need.clarity.modules.visual.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class ExplosionPacketMixin {
   @WrapWithCondition(
      method = "handleExplosion",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")
   )
   private boolean clarity$explosionEmitter(ClientLevel level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
      return !NoRender.hideExplosionParticlesAt(x, y, z);
   }
}
