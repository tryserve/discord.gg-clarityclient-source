package need.clarity.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.Unload;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import need.clarity.render.DrugsRenderer;
import need.clarity.render.MotionBlurRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
   @Inject(
      method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
      at = @At("RETURN")
   )
   private void clarity$onRenderReturn(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      CameraRenderState cameraState,
      Matrix4fc positionMatrix,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean shouldRenderSky,
      ChunkSectionsToRender chunkSectionsToRender,
      CallbackInfo ci
   ) {
      if (!Unload.isUnloaded()) {
         PoseStack poseStack = new PoseStack();
         poseStack.mulPose(positionMatrix);
         float partialTick = tickCounter.getGameTimeDeltaPartialTick(false);
         WorldRenderEvent.fire(poseStack, partialTick);
         MotionBlurRenderer.apply();
         DrugsRenderer.apply();
      }
   }

   @Inject(
      method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
      at = @At("TAIL")
   )
   private void clarity$overtickGhostEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         try {
            Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
            if (overtick == null) {
               return;
            }

            overtick.addGhostRenderStates(levelRenderState, deltaTracker.getGameTimeDeltaPartialTick(false));
         } catch (Throwable var7) {
         }
      }
   }
}
