package need.clarity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import need.clarity.event.EventPacketReceive;

public final class PerPacketEvents {
   private static final List<Consumer<EventPacketReceive>> LISTENERS = new CopyOnWriteArrayList<>();

   private PerPacketEvents() {
   }

   public static void register(Consumer<EventPacketReceive> listener) {
      LISTENERS.add(listener);
   }

   public static void unregister(Consumer<EventPacketReceive> listener) {
      LISTENERS.remove(listener);
   }

   public static EventPacketReceive fire(EventPacketReceive event) {
      if (Unload.isUnloaded()) {
         return event;
      }

      for (Consumer<EventPacketReceive> l : LISTENERS) {
         try {
            l.accept(event);
         } catch (Exception var4) {
         }
      }

      return event;
   }

   public static void clear() {
      LISTENERS.clear();
   }
}
