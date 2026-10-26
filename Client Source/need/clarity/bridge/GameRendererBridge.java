package need.clarity.bridge;

import need.clarity.PerFrameEvents;
import need.clarity.clickgui.ClickGuiRenderer;
import need.clarity.clickgui.ClickGuiScreen;
import need.clarity.clickgui.Gui3DRenderer;
import need.clarity.modules.visual.HandShaders;
import need.clarity.modules.visual.NoHurtCam;
import need.clarity.render.GLCompat;
import need.clarity.render.ShaderHelper;
import need.clarity.render.handshader.HandShaderConfig;
import net.minecraft.client.Minecraft;

public final class GameRendererBridge {
   private GameRendererBridge() {
   }

   public static void onRenderLevelHead() {
      try {
         PerFrameEvents.fire();
      } catch (Throwable var1) {
      }
   }

   public static void beforeRenderHand() {
      try {
         if (!ShaderHelper.isInitialized()) {
            ShaderHelper.initShadersIfNeeded();
         }

         HandShaders hands = HandShaders.INSTANCE;
         if (hands != null && hands.isEnabled()) {
            HandShaderConfig.enabled = false;
            ShaderHelper.captureBackground();
         } else {
            HandShaderConfig.enabled = false;
         }
      } catch (Throwable var1) {
      }
   }

   public static void afterRenderHand() {
      try {
         HandShaders hands = HandShaders.INSTANCE;
         if (hands != null && hands.isEnabled()) {
            if (GLCompat.isColorMaskForcedOff()) {
               GLCompat.forceColorMaskOff(false);
            }

            ShaderHelper.checkFramebuffers();
            hands.render();
         }
      } catch (Throwable var1) {
      }
   }

   public static boolean shouldCancelBobHurt() {
      try {
         return NoHurtCam.isActive();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void captureClickGuiScene() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.screen instanceof ClickGuiScreen screen) {
            ClickGuiRenderer.captureScene(screen.gui());
         }
      } catch (Throwable var4) {
      }

      try {
         Gui3DRenderer.renderWorld();
      } catch (Throwable var3) {
      }
   }
}
