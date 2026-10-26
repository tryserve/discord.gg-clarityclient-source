package need.clarity.mixin;

import need.clarity.modules.visual.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ExplosionParticleMixin {
   @Inject(method = "trackExplosionEffects", at = @At("HEAD"), cancellable = true)
   private void clarity$noTrackedExplosionParticles(Vec3 center, float radius, int blockCount, WeightedList<?> particles, CallbackInfo ci) {
      if (NoRender.hideExplosionParticlesAt(center.x, center.y, center.z)) {
         ci.cancel();
      }
   }

   @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
   private void clarity$noExplosionParticles(
      Entity source,
      DamageSource damageSource,
      ExplosionDamageCalculator calculator,
      double x,
      double y,
      double z,
      float power,
      boolean fire,
      ExplosionInteraction interaction,
      ParticleOptions smallParticle,
      ParticleOptions largeParticle,
      WeightedList<?> sounds,
      Holder<?> music,
      CallbackInfo ci
   ) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelExplosionParticles()) {
         ci.cancel();
      }
   }
}
