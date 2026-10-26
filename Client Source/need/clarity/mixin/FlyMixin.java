package need.clarity.mixin;

import need.clarity.modules.movement.Fly;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class FlyMixin {
   @Inject(method = "travel", at = @At("HEAD"))
   private void cl$flyNoGravity(Vec3 travelVector, CallbackInfo ci) {
      if (Fly.isActive()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && (Object)(Object)this == mc.player) {
            LivingEntity self = (LivingEntity)(Object)this;
            double curY = self.getDeltaMovement().y;
            if (curY < 0.0 && !Fly.isJumpDown(mc) && !Fly.isDescendDown(mc)) {
               self.setDeltaMovement(self.getDeltaMovement().x, 0.0, self.getDeltaMovement().z);
            }
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"))
   private void cl$flyApplyVelocity(Vec3 travelVector, CallbackInfo ci) {
      if (Fly.isActive() && Fly.hasPending) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && (Object)(Object)this == mc.player) {
            LivingEntity self = (LivingEntity)(Object)this;
            String mode = Fly.getMode();
            if ("Full".equals(mode)) {
               self.setDeltaMovement(Fly.pendingVelX, Fly.pendingVelY, Fly.pendingVelZ);
            } else {
               Vec3 cur = self.getDeltaMovement();
               self.setDeltaMovement(cur.x * 0.91 + Fly.pendingVelX, Fly.pendingVelY, cur.z * 0.91 + Fly.pendingVelZ);
            }

            Fly.hasPending = false;
         }
      }
   }

   @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
   private void cl$flyNoFallDamage(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
      if (Fly.isActive()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && (Object)(Object)this == mc.player) {
            cir.setReturnValue(false);
         }
      }
   }
}
