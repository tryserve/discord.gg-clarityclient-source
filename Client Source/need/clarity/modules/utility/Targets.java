package need.clarity.modules.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AntiBot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

public class Targets extends ModuleBase {
   private static Targets instance;
   private static final Map<LivingEntity, Long> lastHitTime = new WeakHashMap<>();
   public static final int ALLOW_PLAYERS = 1;
   public static final int ALLOW_HOSTILE = 2;
   public static final int ALLOW_PASSIVE = 4;

   public static void onHit(LivingEntity entity) {
      lastHitTime.put(entity, System.currentTimeMillis());
   }

   public Targets() {
      super("Targets", "Utility");
      instance = this;
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   public static boolean isTargetAllowed(LivingEntity entity, Player self) {
      if (entity == self) {
         return false;
      }

      if (entity.isRemoved()) {
         return false;
      }

      if (AntiBot.isBot(entity)) {
         return false;
      }

      if (Friends.isProtected(entity)) {
         return false;
      }

      boolean skipDead = getBool("Don't Target Dead", true);
      return skipDead && entity.getHealth() <= 0.0F ? false : isTypeAllowed(entity);
   }

   public static boolean isTypeAllowed(LivingEntity entity) {
      return entity instanceof Player;
   }

   public static boolean isTypeAllowed(LivingEntity entity, int typeMask) {
      if (entity instanceof Player && (typeMask & 1) != 0) {
         return true;
      } else {
         return entity instanceof Monster && (typeMask & 2) != 0
            ? true
            : !(entity instanceof Player) && !(entity instanceof Monster) && (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof Mob) && (typeMask & 4) != 0;
      }
   }

   public static LivingEntity getTarget(List<? extends LivingEntity> candidates, Player self) {
      if (candidates != null && !candidates.isEmpty()) {
         boolean skipDead = getBool("Don't Target Dead", true);
         List<LivingEntity> filtered = new ArrayList<>();

         for (LivingEntity e : candidates) {
            if ((!skipDead || !(e.getHealth() <= 0.0F)) && !Friends.isProtected(e)) {
               filtered.add(e);
            }
         }

         if (filtered.isEmpty()) {
            return null;
         }

         String mode = getStr("Mode", "Nearest");

         return switch (mode) {
            case "Lowest HP" -> (LivingEntity)filtered.stream().min(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            case "Last Hit" -> (LivingEntity)filtered.stream().max(Comparator.comparingLong(ex -> lastHitTime.getOrDefault(ex, 0L))).orElse(null);
            default -> (LivingEntity)filtered.stream().min(Comparator.comparingDouble(ex -> ex.distanceToSqr(self))).orElse(null);
         };
      } else {
         return null;
      }
   }

   private static String getStr(String key, String def) {
      return instance == null ? def : instance.getSetting(key, def);
   }

   private static boolean getBool(String key, boolean def) {
      return instance == null ? def : instance.getSetting(key, def);
   }
}
