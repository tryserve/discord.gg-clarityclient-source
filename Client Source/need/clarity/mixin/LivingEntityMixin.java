package need.clarity.mixin;

import need.clarity.LocalPlayerRotationState;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.movement.MoveFix;
import need.clarity.modules.movement.NoSlow;
import need.clarity.modules.visual.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
   @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
   private Vec3 cl$adjustSprintJumpVelocity(Vec3 original) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || (Object)(Object)this != mc.player) {
         return original;
      }

      if (LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix()) {
         double magnitude = Math.sqrt(original.x * original.x + original.z * original.z);
         if (magnitude < 1.0E-6) {
            return original;
         }

         float serverYawRad = LocalPlayerRotationState.tickServerYaw * (float) (Math.PI / 180.0);
         double sin = Mth.sin(serverYawRad) * -1.0 * magnitude;
         double cos = Mth.cos(serverYawRad) * magnitude;
         return new Vec3(sin, original.y, cos);
      } else {
         return original;
      }
   }

   @Redirect(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
   private float cl$redirectElytraPitch(LivingEntity instance) {
      Minecraft mc = Minecraft.getInstance();
      return instance == mc.player && LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix() ? LocalPlayerRotationState.tickServerPitch : instance.getXRot();
   }

   @Redirect(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 cl$redirectElytraLookVector(LivingEntity instance) {
      Minecraft mc = Minecraft.getInstance();
      return instance == mc.player && LocalPlayerRotationState.silentActive && MoveFix.shouldApplyRotationFix()
         ? AimAssist.getRotationVector(LocalPlayerRotationState.tickServerPitch, LocalPlayerRotationState.tickServerYaw)
         : instance.getLookAngle();
   }

   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void cl$overrideSwingDuration(CallbackInfoReturnable<Integer> cir) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && (Object)(Object)this == mc.player) {
         if (Animations.shouldOverrideSwingSpeed()) {
            cir.setReturnValue(Animations.getConfiguredSwingSpeed());
         }
      }
   }

   @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
   private Vec3 cl$noSlowTravel(Vec3 travelInput) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || (Object)(Object)this != mc.player) {
         return travelInput;
      } else {
         return !NoSlow.shouldSkipSlow(mc.player) ? travelInput : new Vec3(travelInput.x * 5.0, travelInput.y, travelInput.z * 5.0);
      }
   }
}
