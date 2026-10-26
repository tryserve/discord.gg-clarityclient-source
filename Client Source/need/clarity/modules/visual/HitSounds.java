package need.clarity.modules.visual;

import java.util.List;
import java.util.Random;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClaritySounds;
import need.clarity.util.CustomSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HitSounds extends ModuleBase {
   private static final long MUTE_WINDOW_NANOS = 800000000L;
   private final Random random = new Random();
   private volatile long muteUntil = 0L;

   public HitSounds() {
      super("Hit Sounds", "Visual");
      PerAttackEvents.register(this::onAttack);
      PerPacketEvents.register(this::onPacket);
   }

   private void onPacket(EventPacketReceive event) {
      if (this.isEnabled() && this.getSetting("Mute Vanilla Sound", true)) {
         if (System.nanoTime() <= this.muteUntil) {
            String path = ClaritySounds.soundPath(event.getPacket());
            if (path != null && path.startsWith("entity.player.attack")) {
               event.cancel();
            }
         }
      }
   }

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled()) {
         Entity target = event.getTarget();
         if (target != null) {
            if (!this.getSetting("Only Players", false) || target instanceof Player) {
               if (!this.getSetting("Only Crit", false) || this.isCrit(target)) {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.player != null) {
                     String chosen = this.getSetting("Sound", "Attack Crit");
                     if (!this.getSetting("Random Sound", false) && CustomSounds.isCustom(chosen)) {
                        float v = this.getSetting("Volume", 1.0F);
                        float pt = this.getSetting("Pitch", 1.0F);
                        float rnd = this.getSetting("Pitch Random", 0.0F);
                        if (rnd > 0.0F) {
                           pt = Math.max(0.5F, Math.min(2.0F, pt + (this.random.nextFloat() - 0.5F) * 2.0F * rnd));
                        }

                        this.muteUntil = System.nanoTime() + 800000000L;
                        CustomSounds.play(chosen, v, pt);
                        this.playSecond(mc, v, pt);
                     } else {
                        SoundEvent sound;
                        if (this.getSetting("Random Sound", false)) {
                           List<SoundEvent> pool = new java.util.ArrayList<>(
                              List.of(SoundEvents.PLAYER_ATTACK_CRIT, SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_WEAK, SoundEvents.PLAYER_ATTACK_SWEEP)
                           );

                           for (String label : ClaritySounds.HIT.keySet()) {
                              pool.add(ClaritySounds.hit(label));
                           }

                           sound = pool.get(this.random.nextInt(pool.size()));
                        } else {
                           sound = resolveSound(this.getSetting("Sound", "Attack Crit"));
                        }

                        if (sound != null) {
                           float volume = this.getSetting("Volume", 1.0F);
                           float pitch = this.getSetting("Pitch", 1.0F);
                           float randomness = this.getSetting("Pitch Random", 0.0F);
                           if (randomness > 0.0F) {
                              pitch += (this.random.nextFloat() - 0.5F) * 2.0F * randomness;
                              if (pitch < 0.5F) {
                                 pitch = 0.5F;
                              }

                              if (pitch > 2.0F) {
                                 pitch = 2.0F;
                              }
                           }

                           this.muteUntil = System.nanoTime() + 800000000L;
                           mc.player.playSound(sound, volume, pitch);
                           this.playSecond(mc, volume, pitch);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void playSecond(Minecraft mc, float volume, float pitch) {
      String secondName = this.getSetting("Second Sound", "None");
      if (secondName != null && !"None".equals(secondName)) {
         if (!CustomSounds.play(secondName, volume * 0.6F, pitch)) {
            SoundEvent second = resolveSound(secondName);
            if (second != null) {
               mc.player.playSound(second, volume * 0.6F, pitch);
            }
         }
      }
   }

   private boolean isCrit(Entity target) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      } else {
         return !(target instanceof LivingEntity) ? false : mc.player.fallDistance > 0.0 && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isPassenger();
      }
   }

   public static SoundEvent resolveSound(String name) {
      if (name == null) {
         return SoundEvents.PLAYER_ATTACK_CRIT;
      }

      SoundEvent custom = ClaritySounds.hit(name);
      if (custom != null) {
         return custom;
      }

      switch (name) {
         case "Attack Strong":
            return SoundEvents.PLAYER_ATTACK_STRONG;
         case "Attack Weak":
            return SoundEvents.PLAYER_ATTACK_WEAK;
         case "Attack Sweep":
            return SoundEvents.PLAYER_ATTACK_SWEEP;
         case "Attack Knockback":
            return SoundEvents.PLAYER_ATTACK_KNOCKBACK;
         case "Orb Pickup":
            return SoundEvents.EXPERIENCE_ORB_PICKUP;
         case "Level Up":
            return SoundEvents.PLAYER_LEVELUP;
         case "Totem Use":
            return SoundEvents.TOTEM_USE;
         case "Anvil Land":
            return SoundEvents.ANVIL_LAND;
         case "Bell":
            return SoundEvents.BELL_BLOCK;
         case "Attack Crit":
         default:
            return SoundEvents.PLAYER_ATTACK_CRIT;
      }
   }
}
