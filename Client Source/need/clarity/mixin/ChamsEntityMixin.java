package need.clarity.mixin;

import need.clarity.modules.visual.Chams;
import need.clarity.render.ChamsCollector;
import need.clarity.render.ChamsRenderer;
import need.clarity.render.ThroughWalls;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntityRenderer.class, priority = 500)
public abstract class ChamsEntityMixin {
   @Shadow
   public abstract Identifier getTextureLocation(LivingEntityRenderState var1);

   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void chams$renderType(LivingEntityRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing, CallbackInfoReturnable<RenderType> cir) {
      Integer tint = Chams.getStateTint(state);
      boolean throughWalls = Chams.isThroughWalls();
      ChamsCollector.active = tint != null && throughWalls;
      ChamsCollector.hideArmour = tint != null && !Chams.isRenderArmour();
      ChamsCollector.armourThroughWalls = Chams.isArmourThroughWalls();
      if (tint != null) {
         if (Chams.isNormal()) {
            RenderType skin = RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state));
            cir.setReturnValue(throughWalls ? ThroughWalls.convert(skin) : skin);
         } else {
            cir.setReturnValue(ChamsRenderer.modelRenderType(throughWalls));
         }
      }
   }

   @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
   private void chams$modelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
      Integer tint = Chams.getStateTint(state);
      if (tint != null) {
         cir.setReturnValue(tint);
      }
   }

   @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true)
   private SubmitNodeCollector chams$wrapCollector(SubmitNodeCollector collector) {
      boolean chams = Chams.INSTANCE != null && Chams.INSTANCE.isEnabled();
      if (!chams) {
         return collector;
      } else {
         return !((Object)this instanceof AvatarRenderer) ? collector : ChamsCollector.wrap(collector);
      }
   }
}
