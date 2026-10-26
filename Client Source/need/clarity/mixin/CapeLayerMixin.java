package need.clarity.mixin;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeLayer.class)
public class CapeLayerMixin {
   @Redirect(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isInvisible:Z", opcode = 180)
   )
   private boolean cl$overtickGhostCape(AvatarRenderState state) {
      if (!state.isInvisible) {
         return false;
      }

      Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
      return overtick == null || !overtick.isGhostState(state);
   }
}
