package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import need.clarity.modules.visual.Inspect;
import need.clarity.modules.visual.ViewModel;
import need.clarity.render.AnimationsRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ViewModelMixin {
   @Unique
   private boolean clarity$pushed = false;

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   private void clarity$vmHead(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      this.clarity$pushed = false;
      if (Inspect.hidesOffhand(hand)) {
         ci.cancel();
      } else {
         boolean inspect = Inspect.playing();
         if (ViewModel.isActive() || inspect) {
            poseStack.pushPose();
            this.clarity$pushed = true;
            if (inspect && player != null) {
               HumanoidArm mainArm = player.getMainArm();
               HumanoidArm a = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
               Inspect.applyHand(poseStack, hand, AnimationsRenderer.shouldAnimate(a) ? 0.0F : equipProgress);
            }

            if (ViewModel.isActive()) {
               ViewModel m = ViewModel.INSTANCE;
               poseStack.translate(m.getPosX(hand), m.getPosY(hand), m.getPosZ(hand));
               poseStack.mulPose(Axis.XP.rotationDegrees(m.getRotX(hand)));
               poseStack.mulPose(Axis.YP.rotationDegrees(m.getRotY(hand)));
               poseStack.mulPose(Axis.ZP.rotationDegrees(m.getRotZ(hand)));
               float scale = m.getScale(hand);
               if (player != null && scale != 1.0F) {
                  HumanoidArm main = player.getMainArm();
                  HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
                  float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                  float equip = AnimationsRenderer.shouldAnimate(arm) ? 0.0F : equipProgress;
                  float ay = -0.52F + equip * -0.6F;
                  poseStack.translate(side * 0.56F, ay, -0.72F);
                  poseStack.scale(scale, scale, scale);
                  poseStack.translate(-side * 0.56F, -ay, 0.72F);
               } else {
                  poseStack.scale(scale, scale, scale);
               }
            }
         }
      }
   }

   @Inject(method = "renderArmWithItem", at = @At("RETURN"))
   private void clarity$vmTail(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      if (this.clarity$pushed) {
         poseStack.popPose();
         this.clarity$pushed = false;
      }
   }
}
