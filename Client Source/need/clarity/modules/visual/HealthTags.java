package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.world.entity.LivingEntity;

public class HealthTags extends ModuleBase {
   private static HealthTags instance;

   public HealthTags() {
      super("Health Tags", "Visual");
      instance = this;
   }

   public static HealthTags getInstance() {
      return instance;
   }

   public static int getHealthColor(float hp, float maxHp) {
      if (maxHp <= 0.0F) {
         return -1;
      } else {
         float ratio = hp / maxHp;
         if (ratio >= 0.8F) {
            return -11141291;
         } else if (ratio >= 0.5F) {
            return -171;
         } else {
            return ratio >= 0.25F ? -22016 : -43691;
         }
      }
   }

   public static int getDisplayHp(LivingEntity entity) {
      return Math.max(0, Math.round(entity.getHealth()));
   }
}
