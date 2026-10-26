package need.clarity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.visual.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class AnimationsRenderer {
   private static long lastNanos = 0L;

   private AnimationsRenderer() {
   }

   public static boolean shouldAnimate(HumanoidArm arm) {
      Animations module = getModule();
      if (module != null && module.isEnabled()) {
         Minecraft client = Minecraft.getInstance();
         return client.player != null && client.player.getMainArm() == arm;
      } else {
         return false;
      }
   }

   public static boolean apply(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      if (!shouldAnimate(arm)) {
         return false;
      }

      Animations module = getModule();
      if (module == null) {
         return false;
      }

      String mode = module.getMode();
      int strength = module.getStrength();
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      float anim = (float)Math.sin(swingProgress * Math.PI);
      float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
      float str = Mth.clamp(strength, 20, 75);
      if (SwingEngine.handles(mode)) {
         applyEngine(matrices, side, mode, swingProgress, str);
         return true;
      }

      switch (mode) {
         case "Mode 1":
            applyEquipOffset(matrices, arm);
            matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + anim * -20.0F)));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * g * -20.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
            break;
         case "Mode 2":
            applyEquipOffset(matrices, arm);
            matrices.mulPose(Axis.XP.rotationDegrees(50.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(side * -60.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * (110.0F + str * g)));
            break;
         case "Mode 3":
            applyEquipOffset(matrices, arm);
            matrices.mulPose(Axis.XP.rotationDegrees(50.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(side * (-30.0F * (1.0F - g) - 30.0F + (str - 20.0F) * g)));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * 110.0F));
            break;
         case "Mode 4":
            applyEquipOffset(matrices, arm);
            matrices.mulPose(Axis.YP.rotationDegrees(side * 90.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * -30.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(-90.0F - str * anim + 10.0F));
            break;
         case "Mode 5":
            applyEquipOffset(matrices, arm);
            matrices.mulPose(Axis.XP.rotationDegrees(swingProgress * -360.0F));
            break;
         default:
            return false;
      }

      return true;
   }

   private static void applyEngine(PoseStack m, float side, String mode, float p, float str) {
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.0F : (float)(now - lastNanos) / 1.0E9F;
      if (dt > 5.0E-4F || lastNanos == 0L) {
         lastNanos = now;
         SwingEngine.step(mode, p, dt, (str - 20.0F) / 55.0F);
      }

      float[] q = SwingEngine.pose;
      m.translate(side * (0.56F + q[0]), -0.52F + q[1], -0.72F + q[2]);
      if (q[6] != 0.0F) {
         m.translate(0.0F, 0.12F, 0.0F);
         m.mulPose(Axis.XP.rotationDegrees(q[6]));
         m.translate(0.0F, -0.12F, 0.0F);
      }

      m.mulPose(Axis.YP.rotationDegrees(side * (45.0F + q[3])));
      m.mulPose(Axis.ZP.rotationDegrees(side * q[4]));
      m.mulPose(Axis.XP.rotationDegrees(q[5]));
      m.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
   }

   private static void applyEquipOffset(PoseStack matrices, HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      matrices.translate(side * 0.56F, -0.52F, -0.72F);
   }

   private static Animations getModule() {
      return ModuleRegistry.get("Animations") instanceof Animations a ? a : null;
   }
}
