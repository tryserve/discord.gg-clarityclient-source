package need.clarity.modules.visual;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import need.clarity.config.ClarityConfig;

public final class ArrayListModes {
   private static final Map<String, Supplier<String>> RESOLVERS = new HashMap<>();

   private ArrayListModes() {
   }

   public static String modeOf(String module) {
      Supplier<String> r = RESOLVERS.get(module);
      if (r == null) {
         return null;
      }

      try {
         String v = r.get();
         return v != null && !v.isEmpty() ? v : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private static void select(String module, String key, String def) {
      RESOLVERS.put(module, () -> str(module, key, def));
   }

   private static void flag(String module, String key, boolean def, String onLabel, String offLabel) {
      RESOLVERS.put(module, () -> bool(module, key, def) ? onLabel : offLabel);
   }

   private static String str(String module, String key, String def) {
      return ClarityConfig.getSetting(module, key) instanceof String s && !s.isEmpty() ? s : def;
   }

   private static boolean bool(String module, String key, boolean def) {
      return ClarityConfig.getSetting(module, key) instanceof Boolean b ? b : def;
   }

   static {
      select("Kill Aura", "Aim At", "Dynamic");
      select("Auto Anchor", "Anchor Mode", "Normal");
      select("Chest Stealer", "Collection Mode", "Normal");
      select("Overtick", "Release Mode", "Flow");
      select("Disabler", "Mode", "Spartan Trident");
      select("Client Spoofer", "Client", "Vanilla");
      select("Anti Cob", "Mode", "Water");
      select("WindCharge", "Mode", "Immediate");
      select("MoveFix", "Mode", "Silent");
      select("Auto Mace", "Mode", "Kill Aura");
      select("Fly", "Mode", "Creative");
      select("NoFall", "Mode", "Normal");
      select("Animations", "Mode", "Simple");
      select("Tracers", "Style", "Lines");
      select("Hats", "Hat", "China Hat");
      select("Hand Shaders", "Shader", "Chroma");
      select("Player ESP", "Mode", "Outline");
      select("Hit Particles", "Particle", "Crit");
      select("Hit Sounds", "Sound", "Attack Crit");
      select("Kill Sounds", "Sound", "Headshot");
      select("Chams", "Mode", "Color");
      select("Jump Effect", "Particle", "Cloud");
      select("Wings", "Style", "Angel");
      select("Target ESP", "Mode", "Box");
      select("Particles", "Particle", "Snowflake");
      select("Kill Effect", "Effect", "Shockwave");
      select("Player Model", "Model", "Kenny");
      select("Pets", "Pet", "Mini Kenny");
      flag("Aim Assist", "Silent Rotations", false, "Silent", "Normal");
      flag("Auto Armour", "Silent", false, "Silent", "Normal");
      flag("Inventory Cleaner", "Silent", false, "Silent", "Normal");
      flag("Silent Totem", "Bypass", true, "Bypass", "Normal");
      flag("Inventory Walk", "Grim", false, "Grim", "Normal");
      RESOLVERS.put("Aura", () -> {
         String mode = str("Aura", "Mode", "Orbiting");
         switch (mode) {
            case "Orbiting":
               return str("Aura", "Orbit Style", "Shards");
            case "Elemental":
               return str("Aura", "Element", "Flame");
            default:
               return mode;
         }
      });
   }
}
