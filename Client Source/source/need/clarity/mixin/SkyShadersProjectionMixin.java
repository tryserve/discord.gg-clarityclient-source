package need.clarity.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectionMatrixBuffer.class)
public abstract class SkyShadersProjectionMixin {
   @Unique
   private static final Matrix4f clarity$tmp = new Matrix4f();

   @Unique
   private boolean clarity$isLevelBuffer() {
      Minecraft mc = Minecraft.getInstance();
      return mc != null && mc.gameRenderer != null ? ((GameRendererSkyAccessor)mc.gameRenderer).clarity$getLevelProjectionMatrixBuffer() == (Object)this : false;
   }

   @Inject(method = "getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"))
   private void clarity$captureMatrix(Matrix4f matrix, CallbackInfoReturnable<GpuBufferSlice> cir) {
      if (matrix != null && this.clarity$isLevelBuffer()) {
         SkyShaderRenderer.captureProjection(matrix);
      }
   }

   @Inject(method = "getBuffer(Lnet/minecraft/client/renderer/Projection;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"))
   private void clarity$captureProjection(Projection projection, CallbackInfoReturnable<GpuBufferSlice> cir) {
      if (projection != null && this.clarity$isLevelBuffer()) {
         SkyShaderRenderer.captureProjection(projection.getMatrix(clarity$tmp));
      }
   }
}

