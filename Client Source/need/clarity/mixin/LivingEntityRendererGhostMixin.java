package need.clarity.mixin;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGhostMixin {
   @Shadow
   public abstract Identifier getTextureLocation(LivingEntityRenderState var1);

   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void cl$ghostRenderType(LivingEntityRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing, CallbackInfoReturnable<RenderType> cir) {
      Overtick overtick = cl$overtick();
      if (overtick != null && overtick.isGhostState(state)) {
         cir.setReturnValue(RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state)));
      }
   }

   @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
   private void cl$ghostModelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
      Overtick overtick = cl$overtick();
      if (overtick != null && overtick.isGhostState(state)) {
         int alpha = overtick.ghostOpacityAlpha();
         cir.setReturnValue(alpha << 24 | 16777215);
      }
   }

   @Unique
   private static Overtick cl$overtick() {
      return (Overtick)ModuleRegistry.get("Overtick");
   }
}
