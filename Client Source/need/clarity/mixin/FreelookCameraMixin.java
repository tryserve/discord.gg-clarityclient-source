package need.clarity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import need.clarity.modules.visual.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class FreelookCameraMixin {
   @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
   private void cl$freelook(Camera camera, float yaw, float pitch, Operation<Void> original) {
      if (Freelook.isActive()) {
         original.call(new Object[]{camera, Freelook.yaw, Freelook.pitch});
      } else {
         original.call(new Object[]{camera, yaw, pitch});
      }
   }
}
