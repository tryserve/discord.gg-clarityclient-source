package need.clarity.mixin;

import need.clarity.modules.visual.Ambience;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapAmbienceMixin {
   @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
   private void ambience$tintLightmap(LightmapRenderState state, float partialTick, CallbackInfo ci) {
      if (Ambience.isOverrideLightmapEnabled()) {
         float[] tint = Ambience.getLightmapColour();
         float tr = tint[0];
         float tg = tint[1];
         float tb = tint[2];
         if (!(tr >= 1.0F) || !(tg >= 1.0F) || !(tb >= 1.0F)) {
            tintVec(state.blockLightTint, tr, tg, tb);
            tintVec(state.skyLightColor, tr, tg, tb);
            tintVec(state.ambientColor, tr, tg, tb);
         }
      }
   }

   private static void tintVec(Vector3fc vec, float tr, float tg, float tb) {
      if (vec instanceof Vector3f v) {
         v.mul(tr, tg, tb);
      }
   }
}
