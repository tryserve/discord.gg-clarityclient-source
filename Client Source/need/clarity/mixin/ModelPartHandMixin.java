package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.render.ModelHands;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartHandMixin {
   @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true)
   private void cl$modelHand(PoseStack poseStack, CallbackInfo ci) {
      if (ModelHands.active) {
         try {
            if (ModelHands.apply((ModelPart)(Object)this, poseStack)) {
               ci.cancel();
            }
         } catch (Throwable var4) {
         }
      }
   }
}
