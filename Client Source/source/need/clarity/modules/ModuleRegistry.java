package need.clarity.modules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import need.clarity.Unload;
import need.clarity.audio.AudioDemand;
import need.clarity.config.ClarityConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;

public final class ModuleRegistry {
   private static final Map<String, ModuleBase> modules = new LinkedHashMap<>();
   private static volatile boolean tickRegistered = false;

   private ModuleRegistry() {
   }

   public static void register(ModuleBase module) {
      modules.put(module.getName(), module);
   }

   public static ModuleBase get(String name) {
      return modules.get(name);
   }

   public static List<ModuleBase> all() {
      return List.copyOf(modules.values());
   }

   public static void registerTickEvent() {
      if (!tickRegistered) {
         tickRegistered = true;
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> {
            if (!Unload.isUnloaded()) {
               for (ModuleBase m : all()) {
                  if (m.isEnabled()) {
                     try {
                        m.onTick();
                     } catch (Throwable var5) {
                     }
                  }
               }

               try {
                  AudioDemand.endTick();
               } catch (Throwable var4) {
               }
            }
         });
      }
   }

   public static void syncFromConfig() {
      if (!Unload.isUnloaded()) {
         for (ModuleBase m : all()) {
            boolean want = ClarityConfig.isEnabled(m.getName());
            m.setEnabled(want);
         }
      }
   }
}
