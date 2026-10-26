package need.clarity.mixin;

import need.clarity.render.esp.ShaderEspRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ShaderEspLevelRendererMixin {
   @Inject(
      method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
      at = @At("TAIL")
   )
   private void clarity$shaderEspExtract(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
      ShaderEspRenderer.afterExtract(levelRenderState);
   }

   @Inject(
      method = "lambda$addMainPass$0(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;ZLorg/joml/Matrix4fc;)V",
      at = @At("TAIL")
   )
   private void clarity$shaderEspMainPassEnd(CallbackInfo ci) {
      ShaderEspRenderer.captureMask();
   }

   @Inject(method = "doEntityOutline()V", at = @At("HEAD"), cancellable = true)
   private void clarity$shaderEspComposite(CallbackInfo ci) {
      if (ShaderEspRenderer.onDoEntityOutline()) {
         ci.cancel();
      }
   }
}
