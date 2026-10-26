package need.clarity.mixin;

import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.ServerRotations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
   private void cl$silentRotPitch(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
      if (entity instanceof LocalPlayer) {
         if (entity == Minecraft.getInstance().player) {
            if (ServerRotations.shouldApply()) {
               if (AimAssist.hasSilentRotation()) {
                  state.xRot = ServerRotations.serverPitchLerp(tickDelta);
               }
            }
         }
      }
   }
}
