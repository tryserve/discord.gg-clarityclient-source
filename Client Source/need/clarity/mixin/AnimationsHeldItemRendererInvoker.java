package need.clarity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface AnimationsHeldItemRendererInvoker {
   @Invoker("applyItemArmTransform")
   void cl$applyItemArmTransform(PoseStack var1, HumanoidArm var2, float var3);

   @Invoker("swingArm")
   void cl$swingArm(float var1, PoseStack var2, int var3, HumanoidArm var4);
}
