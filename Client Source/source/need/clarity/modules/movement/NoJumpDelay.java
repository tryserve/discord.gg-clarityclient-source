package need.clarity.modules.movement;

import java.lang.reflect.Field;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public class NoJumpDelay extends ModuleBase {
   private static final Field NO_JUMP_DELAY_FIELD = resolveField();

   private static Field resolveField() {
      try {
         Field f = LivingEntity.class.getDeclaredField("noJumpDelay");
         f.setAccessible(true);
         return f;
      } catch (NoSuchFieldException e) {
         return null;
      }
   }

   public NoJumpDelay() {
      super("No Jump Delay", "Utility");
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (NO_JUMP_DELAY_FIELD != null) {
            try {
               NO_JUMP_DELAY_FIELD.setInt(mc.player, 0);
            } catch (IllegalAccessException var3) {
            }
         }
      }
   }
}
