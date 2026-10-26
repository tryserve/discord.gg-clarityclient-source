package need.clarity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PerFrameEvents {
   private static final List<Runnable> CALLBACKS = new CopyOnWriteArrayList<>();

   private PerFrameEvents() {
   }

   public static void register(Runnable callback) {
      if (!CALLBACKS.contains(callback)) {
         CALLBACKS.add(callback);
      }
   }

   public static void unregister(Runnable callback) {
      CALLBACKS.remove(callback);
   }

   public static void fire() {
      if (!Unload.isUnloaded()) {
         for (Runnable r : CALLBACKS) {
            try {
               r.run();
            } catch (Exception var3) {
            }
         }
      }
   }

   public static void clear() {
      CALLBACKS.clear();
   }
}
