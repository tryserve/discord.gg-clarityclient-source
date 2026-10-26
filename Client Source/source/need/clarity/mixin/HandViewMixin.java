package need.clarity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.HandView;
import need.clarity.modules.visual.Inspect;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public class HandViewMixin {
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;

   @WrapOperation(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void cl$handView(
      ItemInHandRenderer self,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext ctx,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayer player,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true, ordinal = 2) float swingProgress,
      @Local(argsOnly = true, ordinal = 3) float equipProgress
   ) {
      HandView.Renderer itemRenderer = (e, st, c, p, col, l) -> original.call(new Object[]{self, e, st, c, p, col, l});
      boolean handled = false;

      try {
         handled = HandView.render(this.entityRenderDispatcher, itemRenderer, player, hand, stack, ctx, pose, collector, light, swingProgress, equipProgress);
      } catch (Throwable var17) {
      }

      if (!handled) {
         try {
            Inspect.applyItem(pose, hand);
         } catch (Throwable var16) {
         }

         original.call(new Object[]{self, entity, stack, ctx, pose, collector, light});
      }
   }
}
