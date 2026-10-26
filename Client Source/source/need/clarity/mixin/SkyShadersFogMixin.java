package need.clarity.mixin;

import need.clarity.modules.visual.Ambience;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class SkyShadersFogMixin {
   @Inject(
      method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
      at = @At("RETURN")
   )
   private void clarity$skyFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float partialTick, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
      FogData fog = (FogData)cir.getReturnValue();
      if (fog != null && fog.color != null) {
         if (SkyShaders.matchFog() && SkyShaderRenderer.wasActiveLastFrame()) {
            if (!Ambience.isOverrideFogColourEnabled()) {
               String preset = SkyShaders.preset();
               float[][] c = SkyShaders.colors();
               float[] h = SkyShaders.horizon(preset, c[0], c[1]);
               fog.color.set(h[0], h[1], h[2], fog.color.w);
               if ("Fog".equals(preset)) {
                  float end = SkyShaders.fogDistance();
                  float start = end * 0.04F;
                  fog.environmentalStart = Math.min(fog.environmentalStart, start);
                  fog.environmentalEnd = Math.min(fog.environmentalEnd, end);
                  fog.renderDistanceStart = Math.min(fog.renderDistanceStart, start);
                  fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, end);
                  fog.cloudEnd = Math.min(fog.cloudEnd, end);
               }
            }
         }
      }
   }
}
