package need.clarity.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ClaritySounds {
   public static final Map<String, String> HIT = new LinkedHashMap<>();
   public static final Map<String, String> KILL = new LinkedHashMap<>();
   private static final Map<String, SoundEvent> CACHE = new HashMap<>();

   private ClaritySounds() {
   }

   private static SoundEvent event(String key) {
      return CACHE.computeIfAbsent(key, k -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("clarity", k)));
   }

   public static SoundEvent raw(String key) {
      return key == null ? null : event(key);
   }

   public static SoundEvent hit(String label) {
      String id = label == null ? null : HIT.get(label);
      return id == null ? null : event("hit." + id);
   }

   public static SoundEvent kill(String label) {
      String id = label == null ? null : KILL.get(label);
      return id == null ? null : event("kill." + id);
   }

   public static String soundPath(Packet<?> packet) {
      try {
         SoundEvent se = null;
         if (packet instanceof ClientboundSoundPacket p) {
            se = (SoundEvent)p.getSound().value();
         } else if (packet instanceof ClientboundSoundEntityPacket p) {
            se = (SoundEvent)p.getSound().value();
         }

         if (se == null) {
            return null;
         }

         Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(se);
         return key == null ? null : key.getPath();
      } catch (Throwable t) {
         return null;
      }
   }

   static {
      HIT.put("Anime", "anime");
      HIT.put("Blade Ball", "bladeball");
      HIT.put("Chink", "chink");
      HIT.put("Click", "click");
      HIT.put("Copper Bell", "copperbell");
      HIT.put("Crystal", "crystal");
      HIT.put("Glass 1", "glass1");
      HIT.put("Glass 2", "glass2");
      HIT.put("Metal Bat", "metalbat");
      HIT.put("Neverlose", "neverlose");
      HIT.put("Punch", "punch");
      HIT.put("Slice", "slice");
      HIT.put("Tap", "tap");
      HIT.put("Water", "water");
      KILL.put("Critical Hit", "criticalhit");
      KILL.put("Dead", "dead");
      KILL.put("Gong", "gong");
      KILL.put("Headshot", "headshot");
      KILL.put("Kill Pop", "killpop");
      KILL.put("Phase Out", "phaseout");
      KILL.put("Re:Zero", "rezero");
      KILL.put("Roblox", "roblox");
      KILL.put("Shatter", "shatter");
      KILL.put("Super Smash Bros", "supersmashbros");
      KILL.put("TF2 Killstreak", "tf2killstreak");
      KILL.put("Undertale", "undertale");
      KILL.put("Valorant", "valorant");
   }
}
