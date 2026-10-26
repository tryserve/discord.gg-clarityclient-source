package need.clarity.mixin;

import need.clarity.modules.movement.SpinBot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AvatarRenderer.class, priority = 5000)
public class SpinBotRenderMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$spinBotOverride(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      if (SpinBot.isClientMode()) {
         if (player == Minecraft.getInstance().player) {
            state.bodyRot = SpinBot.yaw(tickProgress);
            state.yRot = 0.0F;
         }
      }
   }
}
