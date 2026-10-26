package need.clarity.bridge;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import need.clarity.render.DrugsRenderer;
import need.clarity.render.MotionBlurRenderer;
import need.clarity.render.esp.ShaderEspRenderer;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;

public final class LevelRendererBridge {
   private LevelRendererBridge() {
   }

   public static void onRenderLevelReturn(DeltaTracker tickCounter, Matrix4fc positionMatrix) {
      try {
         PoseStack poseStack = new PoseStack();
         poseStack.mulPose(positionMatrix);
         float partialTick = tickCounter.getGameTimeDeltaPartialTick(false);
         WorldRenderEvent.fire(poseStack, partialTick);
      } catch (Throwable var6) {
      }

      try {
         MotionBlurRenderer.apply();
      } catch (Throwable var5) {
      }

      try {
         DrugsRenderer.apply();
      } catch (Throwable var4) {
      }
   }

   public static void onExtractVisibleEntitiesTail(Object levelRenderState, float partialTick) {
      try {
         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         if (overtick != null) {
            overtick.addGhostRenderStates((LevelRenderState)levelRenderState, partialTick);
         }
      } catch (Throwable var4) {
      }

      try {
         ShaderEspRenderer.afterExtract((LevelRenderState)levelRenderState);
      } catch (Throwable var3) {
      }
   }

   public static void onSkyBeginFrame(Object positionMatrix, Object cameraState) {
      try {
         SkyShaderRenderer.beginFrame((Matrix4fc)positionMatrix, (CameraRenderState)cameraState);
      } catch (Throwable var3) {
      }
   }
}
