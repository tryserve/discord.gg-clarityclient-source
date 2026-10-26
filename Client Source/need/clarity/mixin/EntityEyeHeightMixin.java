package need.clarity.mixin;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityEyeHeightMixin {
   @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
   private void cl$standingEyeWhileGlideHidden(CallbackInfoReturnable<Float> cir) {
      try {
         Entity self = (Entity)(Object)this;
         if (self != Minecraft.getInstance().player) {
            return;
         }

         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         if (overtick == null || !overtick.isEnabled() || !overtick.isElytraStopHeld()) {
            return;
         }

         cir.setReturnValue(self.getEyeHeight(Pose.STANDING));
      } catch (Throwable var4) {
      }
   }
}
