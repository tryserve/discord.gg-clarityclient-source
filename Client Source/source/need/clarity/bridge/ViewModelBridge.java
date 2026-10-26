package need.clarity.bridge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Method;
import need.clarity.modules.visual.HandView;
import need.clarity.modules.visual.Inspect;
import need.clarity.modules.visual.ViewModel;
import need.clarity.render.AnimationsRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ViewModelBridge {
   private static boolean pushed = false;
   private static Method RENDER_ITEM;

   private ViewModelBridge() {
   }

   public static boolean onRenderArmWithItemHead(Object playerObj, InteractionHand hand, float equipProgress, PoseStack poseStack) {
      pushed = false;

      try {
         if (Inspect.hidesOffhand(hand)) {
            return true;
         }

         boolean inspect = Inspect.playing();
         boolean vm = ViewModel.isActive();
         if (!vm && !inspect) {
            return false;
         }

         AbstractClientPlayer player = (AbstractClientPlayer)playerObj;
         poseStack.pushPose();
         pushed = true;
         if (inspect && player != null) {
            HumanoidArm mainArm = player.getMainArm();
            HumanoidArm a = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
            Inspect.applyHand(poseStack, hand, AnimationsRenderer.shouldAnimate(a) ? 0.0F : equipProgress);
         }

         if (!vm) {
            return false;
         }

         ViewModel m = ViewModel.INSTANCE;
         if (m == null) {
            return false;
         }

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
      } catch (Throwable var14) {
      }

      return false;
   }

   public static void onRenderArmWithItemReturn(PoseStack poseStack) {
      if (pushed) {
         try {
            poseStack.popPose();
         } catch (Throwable var2) {
         }

         pushed = false;
      }
   }

   public static void handViewRenderItem(
      ItemInHandRenderer self,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext ctx,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      AbstractClientPlayer player,
      InteractionHand hand,
      float swingProgress,
      float equipProgress
   ) {
      boolean handled = false;

      try {
         HandView.Renderer items = (e, st, c, p, col, l) -> callRenderItem(self, e, st, c, p, col, l);
         handled = HandView.render(Minecraft.getInstance().getEntityRenderDispatcher(), items, player, hand, stack, ctx, pose, collector, light, swingProgress, equipProgress);
      } catch (Throwable var14) {
      }

      if (!handled) {
         try {
            Inspect.applyItem(pose, hand);
         } catch (Throwable var13) {
         }

         callRenderItem(self, entity, stack, ctx, pose, collector, light);
      }
   }

   private static void callRenderItem(ItemInHandRenderer self, LivingEntity e, ItemStack st, ItemDisplayContext c, PoseStack p, SubmitNodeCollector col, int l) {
      try {
         if (RENDER_ITEM == null) {
            RENDER_ITEM = ItemInHandRenderer.class
               .getDeclaredMethod("renderItem", LivingEntity.class, ItemStack.class, ItemDisplayContext.class, PoseStack.class, SubmitNodeCollector.class, int.class);
            RENDER_ITEM.setAccessible(true);
         }

         RENDER_ITEM.invoke(self, e, st, c, p, col, l);
      } catch (Throwable var8) {
      }
   }
}
