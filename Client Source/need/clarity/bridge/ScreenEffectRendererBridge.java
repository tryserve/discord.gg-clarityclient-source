package need.clarity.bridge;

import need.clarity.modules.visual.NoRender;

public final class ScreenEffectRendererBridge {
   private ScreenEffectRendererBridge() {
   }

   public static boolean shouldCancelFire() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelFire();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelTotemAnimation() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelTotemAnimation();
      } catch (Throwable t) {
         return false;
      }
   }
}
