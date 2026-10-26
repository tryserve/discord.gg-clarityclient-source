package need.clarity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import need.clarity.event.EventPacketSend;

public final class PerPacketSendEvents {
   private static final List<Consumer<EventPacketSend>> LISTENERS = new CopyOnWriteArrayList<>();

   private PerPacketSendEvents() {
   }

   public static void register(Consumer<EventPacketSend> listener) {
      LISTENERS.add(listener);
   }

   public static void unregister(Consumer<EventPacketSend> listener) {
      LISTENERS.remove(listener);
   }

   public static EventPacketSend fire(EventPacketSend event) {
      if (Unload.isUnloaded()) {
         return event;
      }

      for (Consumer<EventPacketSend> l : LISTENERS) {
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
