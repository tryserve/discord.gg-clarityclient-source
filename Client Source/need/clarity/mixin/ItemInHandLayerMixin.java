package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.PlayerModel;
import need.clarity.render.ModelHands;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
   private static final String SUBMIT = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V";

   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
      at = @At("HEAD")
   )
   private void cl$heldItemsBegin(PoseStack poseStack, SubmitNodeCollector collector, int light, ArmedEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
      ModelHands.active = false;

      try {
         if (!PlayerModel.isModelState(state)) {
            return;
         }

         Model model = ((RenderLayer)(Object)this).getParentModel();
         ModelPart root = model.root();
         ModelHands.right = root.getChild("right_arm");
         ModelHands.left = root.getChild("left_arm");
         ModelHands.active = true;
      } catch (Throwable var10) {
      }
   }

   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
      at = @At("RETURN")
   )
   private void cl$heldItemsEnd(PoseStack poseStack, SubmitNodeCollector collector, int light, ArmedEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
      ModelHands.active = false;
   }
}
