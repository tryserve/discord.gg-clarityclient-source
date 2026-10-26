package need.clarity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.modules.visual.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public final class ModelHands {
   public static volatile boolean active = false;
   public static volatile ModelPart right;
   public static volatile ModelPart left;
   private static final double MODEL_FEET = 1.501;

   private ModelHands() {
   }

   public static boolean apply(ModelPart part, PoseStack poseStack) {
      boolean isRight = part == right;
      if (!isRight && part != left) {
         return false;
      }

      double[] h = PlayerModel.handPose(isRight);
      if (h == null) {
         return false;
      }

      poseStack.translate(-h[0], 1.501 - h[1], -h[2]);
      Matrix3f m = new Matrix3f((float)h[3], (float)h[4], (float)h[5], (float)h[6], (float)h[7], (float)h[8], (float)h[9], (float)h[10], (float)h[11]);
      poseStack.mulPose(new Quaternionf().setFromNormalized(m));
      float side = isRight ? 1.0F : -1.0F;
      poseStack.translate(side / 16.0F, -0.625F, 0.125F);
      return true;
   }
}
