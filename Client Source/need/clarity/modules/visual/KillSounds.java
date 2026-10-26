package need.clarity.modules.visual;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClaritySounds;
import need.clarity.util.CustomSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class KillSounds extends ModuleBase {
   public static KillSounds INSTANCE;
   private static final long KILL_CREDIT_NANOS = 6000000000L;
   private final Random random = new Random();
   private final Map<Integer, Float> lastHealth = new HashMap<>();
   private final Map<Integer, Long> lastHit = new ConcurrentHashMap<>();

   public KillSounds() {
      super("Kill Sounds", "Visual");
      INSTANCE = this;
      PerAttackEvents.register(this::onAttack);
      PerPacketEvents.register(this::onPacket);
   }

   @Override
   public void onDisable() {
      this.lastHealth.clear();
      this.lastHit.clear();
   }

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled()) {
         Entity target = event.getTarget();
         if (target != null) {
            this.lastHit.put(target.getId(), System.nanoTime());
         }
      }
   }

   private boolean recentlyHit() {
      long now = System.nanoTime();

      for (long t : this.lastHit.values()) {
         if (now - t <= 6000000000L) {
            return true;
         }
      }

      return false;
   }

   private void onPacket(EventPacketReceive event) {
      if (this.isEnabled() && this.getSetting("Mute Vanilla Sound", true)) {
         String path = ClaritySounds.soundPath(event.getPacket());
         if (path != null && path.endsWith(".death")) {
            if (!this.getSetting("Only My Kills", true) || this.recentlyHit()) {
               event.cancel();
            }
         }
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         long now = System.nanoTime();
         this.lastHit.values().removeIf(tx -> now - tx > 12000000000L);
         this.lastHealth.keySet().removeIf(id -> mc.level.getEntity(id) == null);
         boolean onlyMine = this.getSetting("Only My Kills", true);
         boolean onlyPlayers = this.getSetting("Only Players", false);
         double maxDistance = this.getSetting("Max Distance", 0.0F).floatValue();

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le && e != mc.player) {
               float hp = le.getHealth();
               Float prev = this.lastHealth.put(le.getId(), hp);
               if (prev != null && prev > 0.0F && hp <= 0.0F && (!onlyPlayers || le instanceof Player)) {
                  if (onlyMine) {
                     Long t = this.lastHit.get(le.getId());
                     if (t == null || now - t > 6000000000L) {
                        continue;
                     }
                  }

                  if (!(maxDistance > 0.5) || !(mc.player.distanceTo(le) > maxDistance)) {
                     this.play(mc);
                  }
               }
            }
         }
      }
   }

   private void play(Minecraft mc) {
      String chosen = this.getSetting("Sound", "Headshot");
      if (this.getSetting("Random Sound", false) || !CustomSounds.play(chosen, this.getSetting("Volume", 1.0F), this.getSetting("Pitch", 1.0F))) {
         SoundEvent sound;
         if (this.getSetting("Random Sound", false)) {
            List<SoundEvent> pool = new java.util.ArrayList<>();

            for (String label : ClaritySounds.KILL.keySet()) {
               pool.add(ClaritySounds.kill(label));
            }

            sound = pool.get(this.random.nextInt(pool.size()));
         } else {
            sound = ClaritySounds.kill(this.getSetting("Sound", "Headshot"));
         }

         if (sound != null) {
            float volume = this.getSetting("Volume", 1.0F);
            float pitch = this.getSetting("Pitch", 1.0F);
            mc.player.playSound(sound, volume, pitch);
         }
      }
   }
}
