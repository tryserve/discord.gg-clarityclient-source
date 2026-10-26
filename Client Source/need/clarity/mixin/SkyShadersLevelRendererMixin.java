package need.clarity.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SkyShadersLevelRendererMixin {
   @Inject(
      method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
      at = @At("HEAD")
   )
   private void clarity$skyBeginFrame(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      CameraRenderState cameraState,
      Matrix4fc positionMatrix,
      GpuBufferSlice fog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      ChunkSectionsToRender sections,
      CallbackInfo ci
   ) {
      SkyShaderRenderer.beginFrame(positionMatrix, cameraState);
   }

   @Inject(method = "addCloudsPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/CloudStatus;Lnet/minecraft/world/phys/Vec3;JFIFI)V", at = @At("HEAD"), cancellable = true)
   private void clarity$hideClouds(CallbackInfo ci) {
      if (SkyShaders.hideClouds()) {
         ci.cancel();
      }
   }
}
