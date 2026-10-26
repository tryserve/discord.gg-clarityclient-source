package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.List;
import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

public class HandView extends ModuleBase {
   public static HandView INSTANCE;

   public HandView() {
      super("Hand View", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static boolean render(
      EntityRenderDispatcher dispatcher,
      HandView.Renderer items,
      AbstractClientPlayer player,
      InteractionHand hand,
      ItemStack item,
      ItemDisplayContext ctx,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      float swing,
      float equip
   ) {
      if (active() && player != null && item != null && !item.isEmpty() && !player.isInvisible()) {
         if (hand == InteractionHand.OFF_HAND && !INSTANCE.getSetting("Off Hand", true)) {
            return false;
         }

         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         boolean right = arm == HumanoidArm.RIGHT;
         boolean using = player.isUsingItem() && player.getUsedItemHand() == hand;
         boolean special = item.getItem() instanceof CrossbowItem || item.getItem() instanceof BowItem || item.getItem() instanceof TridentItem;
         boolean grip = !"Overlay".equals(INSTANCE.getSetting("Style", "Grip")) && !using && !special;
         if (!grip) {
            overlayArm(dispatcher, player, right, pose, collector, light);
            return false;
         }

         List<Pose> poses = AccessorCompat.poses(pose);
         int last = AccessorCompat.lastIndex(pose);
         if (last < 1) {
            return false;
         }

         Pose base = poses.get(last - 1);
         float s = right ? 1.0F : -1.0F;
         pose.pushPose();

         try {
            pose.last().set(base);
            pose.translate(s * INSTANCE.getSetting("Offset X", 0.0F), INSTANCE.getSetting("Offset Y", 0.0F), INSTANCE.getSetting("Offset Z", 0.0F));
            float g = (float)Math.sqrt(swing);
            float h = -0.3F * (float)Math.sin(g * Math.PI);
            float i = 0.4F * (float)Math.sin(g * Math.PI * 2.0);
            float j = -0.4F * (float)Math.sin(swing * Math.PI);
            pose.translate(s * (h + 0.64000005F), i - 0.6F + equip * -0.6F, j - 0.71999997F);
            pose.mulPose(Axis.YP.rotationDegrees(s * 45.0F));
            float k = (float)Math.sin(swing * swing * Math.PI);
            float l = (float)Math.sin(g * Math.PI);
            pose.mulPose(Axis.YP.rotationDegrees(s * l * 70.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(s * k * -20.0F));
            float rot = INSTANCE.getSetting("Rotation", 0.0F);
            if (rot != 0.0F) {
               pose.mulPose(Axis.YP.rotationDegrees(s * rot));
            }

            pose.translate(s * -1.0F, 3.6F, 3.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(s * 120.0F));
            pose.mulPose(Axis.XP.rotationDegrees(200.0F));
            pose.mulPose(Axis.YP.rotationDegrees(s * -135.0F));
            pose.translate(s * 5.6F, 0.0F, 0.0F);
            AvatarRenderer<AbstractClientPlayer> r = dispatcher.getPlayerRenderer(player);
            Identifier skin = player.getSkin().body().texturePath();
            boolean sleeve = INSTANCE.getSetting("Sleeve", true);
            if (right) {
               r.renderRightHand(pose, collector, light, skin, sleeve && player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
            } else {
               r.renderLeftHand(pose, collector, light, skin, sleeve && player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            }

            HumanoidModel<?> model = (HumanoidModel<?>)r.getModel();
            ModelPart part = right ? model.rightArm : model.leftArm;
            part.translateAndRotate(pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.translate((right ? 1.0F : -1.0F) / 16.0F, 0.125F, -0.625F);
            if (Inspect.playing()) {
               pose.mulPose(Axis.XP.rotationDegrees(90.0F));
               Inspect.applyItem(pose, hand);
               pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }

            items.render(player, item, right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, pose, collector, light);
         } finally {
            pose.popPose();
         }

         return true;
      } else {
         return false;
      }
   }

   private static void overlayArm(EntityRenderDispatcher dispatcher, AbstractClientPlayer player, boolean right, PoseStack pose, SubmitNodeCollector collector, int light) {
      float s = right ? 1.0F : -1.0F;
      float ox = INSTANCE.getSetting("Offset X", 0.0F);
      float oy = INSTANCE.getSetting("Offset Y", 0.0F);
      float oz = INSTANCE.getSetting("Offset Z", 0.0F);
      float rot = INSTANCE.getSetting("Rotation", 0.0F);
      boolean sleeve = INSTANCE.getSetting("Sleeve", true);
      pose.pushPose();

      try {
         pose.translate(s * (0.08F + ox), -0.08F + oy, oz);
         if (rot != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(s * rot));
         }

         pose.mulPose(Axis.YP.rotationDegrees(s * 45.0F));
         pose.translate(s * -1.0F, 3.6F, 3.5F);
         pose.mulPose(Axis.ZP.rotationDegrees(s * 120.0F));
         pose.mulPose(Axis.XP.rotationDegrees(200.0F));
         pose.mulPose(Axis.YP.rotationDegrees(s * -135.0F));
         pose.translate(s * 5.6F, 0.0F, 0.0F);
         AvatarRenderer<AbstractClientPlayer> r = dispatcher.getPlayerRenderer(player);
         Identifier skin = player.getSkin().body().texturePath();
         if (right) {
            r.renderRightHand(pose, collector, light, skin, sleeve && player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
         } else {
            r.renderLeftHand(pose, collector, light, skin, sleeve && player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
         }
      } catch (Throwable var17) {
      } finally {
         pose.popPose();
      }
   }

   public interface Renderer {
      void render(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, PoseStack var4, SubmitNodeCollector var5, int var6);
   }
}
