package need.clarity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import need.clarity.modules.movement.MoveFix;

public final class PerMovementEvents {
   private static final List<Consumer<MoveFix.EventMovementInput>> LISTENERS = new CopyOnWriteArrayList<>();
   public static MoveFix.EventMovementInput lastEvent = null;

   private PerMovementEvents() {
   }

   public static void register(Consumer<MoveFix.EventMovementInput> listener) {
      LISTENERS.add(listener);
   }

   public static void fire(MoveFix.EventMovementInput event) {
      if (!Unload.isUnloaded()) {
         lastEvent = event;

         for (Consumer<MoveFix.EventMovementInput> l : LISTENERS) {
            try {
               l.accept(event);
            } catch (Exception var4) {
            }
         }
      }
   }

   public static void clear() {
      LISTENERS.clear();
   }
}
