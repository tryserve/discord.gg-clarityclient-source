package need.clarity.mixin;

import need.clarity.modules.visual.Chams;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class ChamsAvatarMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void chams$tagState(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      Chams.updateState(player, state);
   }
}
