package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.render.AnimationsRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class AnimationsHeldItemRendererMixin {
   @Redirect(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
         ordinal = 4
      )
   )
   private void cl$skipDefaultEquipOffset(ItemInHandRenderer instance, PoseStack matrices, HumanoidArm arm, float equipProgress) {
      if (!AnimationsRenderer.shouldAnimate(arm)) {
         ((AnimationsHeldItemRendererInvoker)instance).cl$applyItemArmTransform(matrices, arm, equipProgress);
      }
   }

   @Redirect(
      method = "renderArmWithItem",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V", ordinal = 2)
   )
   private void cl$replaceSwingArm(ItemInHandRenderer instance, float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
      if (!AnimationsRenderer.apply(matrices, arm, swingProgress)) {
         ((AnimationsHeldItemRendererInvoker)instance).cl$swingArm(swingProgress, matrices, armX, arm);
      }
   }
}
