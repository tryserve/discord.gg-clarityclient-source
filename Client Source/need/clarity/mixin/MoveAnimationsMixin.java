package need.clarity.mixin;

import need.clarity.modules.visual.MoveAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class MoveAnimationsMixin {
   @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
   private void cl$moveAnimations(HumanoidRenderState state, CallbackInfo ci) {
      try {
         MoveAnimations.apply((HumanoidModel<?>)(Object)this, state);
      } catch (Throwable var4) {
      }
   }
}
