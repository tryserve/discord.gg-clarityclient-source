package need.clarity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class PerAttackEvents {
   private static final List<Consumer<EventStartAttack>> LISTENERS = new CopyOnWriteArrayList<>();

   private PerAttackEvents() {
   }

   public static void register(Consumer<EventStartAttack> listener) {
      LISTENERS.add(listener);
   }

   public static void fire(EventStartAttack event) {
      if (!Unload.isUnloaded()) {
         for (Consumer<EventStartAttack> l : LISTENERS) {
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
