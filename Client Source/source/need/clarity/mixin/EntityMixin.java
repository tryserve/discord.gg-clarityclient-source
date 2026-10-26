package need.clarity.mixin;

import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
   @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
   private void cl$silentViewVector(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
      if ((Object)this == Minecraft.getInstance().player && AimAssist.isActivelyTracking()) {
         cir.setReturnValue(AimAssist.getRotationVector(AimAssist.getSilentServerPitch(), AimAssist.getSilentServerYaw()));
      }
   }

   @Inject(method = "getHeadLookAngle", at = @At("HEAD"), cancellable = true)
   private void cl$silentHeadLookAngle(CallbackInfoReturnable<Vec3> cir) {
      if ((Object)this == Minecraft.getInstance().player && AimAssist.isActivelyTracking()) {
         cir.setReturnValue(AimAssist.getRotationVector(AimAssist.getSilentServerPitch(), AimAssist.getSilentServerYaw()));
      }
   }

   @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
   private float cl$redirectMovementYaw(Entity instance) {
      return instance == Minecraft.getInstance().player && AimAssist.hasSilentRotation() && MoveFix.shouldApplyRotationFix() ? AimAssist.getSilentServerYaw() : instance.getYRot();
   }
}
