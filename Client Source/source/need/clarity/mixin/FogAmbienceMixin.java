package need.clarity.mixin;

import need.clarity.modules.visual.Ambience;
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
public abstract class FogAmbienceMixin {
   @Inject(
      method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void ambience$overrideFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float partialTick, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
      FogData fog = (FogData)cir.getReturnValue();
      if (fog != null) {
         if (Ambience.isOverrideFogColourEnabled()) {
            float[] rgb = Ambience.getFogColour();
            fog.color.set(rgb[0], rgb[1], rgb[2], 1.0F);
         }

         if (Ambience.isOverrideFogDistanceEnabled()) {
            float endBlocks = Ambience.getFogDistanceChunks() * 16.0F;
            fog.environmentalStart = 0.0F;
            fog.environmentalEnd = endBlocks;
            fog.renderDistanceStart = 0.0F;
            fog.renderDistanceEnd = endBlocks;
            fog.skyEnd = endBlocks;
            fog.cloudEnd = endBlocks;
         }

         cir.setReturnValue(fog);
      }
   }
}
