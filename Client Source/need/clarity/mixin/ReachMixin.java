package need.clarity.mixin;

import need.clarity.modules.combat.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ReachMixin {
   @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
   private void reach$blockRange(CallbackInfoReturnable<Double> cir) {
      if ((Object)this == Minecraft.getInstance().player) {
         float r = Reach.getBlockReach();
         if (r > 0.0F) {
            cir.setReturnValue((double)r);
         }
      }
   }

   @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
   private void reach$entityRange(CallbackInfoReturnable<Double> cir) {
      if ((Object)this == Minecraft.getInstance().player) {
         float r = Reach.getEntityReach();
         if (r > 0.0F) {
            cir.setReturnValue((double)r);
         }
      }
   }
}
