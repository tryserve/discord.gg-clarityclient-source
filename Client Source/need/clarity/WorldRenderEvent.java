package need.clarity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WorldRenderEvent {
   private static final List<WorldRenderEvent.Listener> LISTENERS = new CopyOnWriteArrayList<>();

   private WorldRenderEvent() {
   }

   public static void register(WorldRenderEvent.Listener l) {
      if (!LISTENERS.contains(l)) {
         LISTENERS.add(l);
      }
   }

   public static void unregister(WorldRenderEvent.Listener l) {
      LISTENERS.remove(l);
   }

   public static void fire(PoseStack poseStack, float partialTick) {
      if (!Unload.isUnloaded()) {
         for (WorldRenderEvent.Listener l : LISTENERS) {
            try {
               l.onWorldRender(poseStack, partialTick);
            } catch (Exception var5) {
            }
         }
      }
   }

   public static void clear() {
      LISTENERS.clear();
   }

   public interface Listener {
      void onWorldRender(PoseStack var1, float var2);
   }
}
