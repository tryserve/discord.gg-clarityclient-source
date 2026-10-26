package need.clarity.mixin;

import need.clarity.modules.visual.Freecam;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompiledSectionMesh.class)
public class SectionOcclusionGraphMixin {
   @Inject(method = "facesCanSeeEachother", at = @At("HEAD"), cancellable = true)
   private void clarity$bypassOcclusionCulling(Direction face1, Direction face2, CallbackInfoReturnable<Boolean> cir) {
      if (Freecam.isActive()) {
         cir.setReturnValue(true);
      }
   }
}
