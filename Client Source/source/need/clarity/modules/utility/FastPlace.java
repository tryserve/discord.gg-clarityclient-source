package need.clarity.modules.utility;

import java.lang.reflect.Field;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class FastPlace extends ModuleBase {
   private static final Field RIGHT_CLICK_DELAY_FIELD = resolveField();

   private static Field resolveField() {
      try {
         Field f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
         return f;
      } catch (NoSuchFieldException e) {
         return null;
      }
   }

   public FastPlace() {
      super("Fast Place", "Utility");
   }

   @Override
   public void onTick() {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            int targetDelay = this.getSetting("Delay", 0);
            if (targetDelay < 0) {
               targetDelay = 0;
            }

            if (targetDelay > 4) {
               targetDelay = 4;
            }

            try {
               int current = RIGHT_CLICK_DELAY_FIELD.getInt(mc);
               if (current > targetDelay) {
                  RIGHT_CLICK_DELAY_FIELD.setInt(mc, targetDelay);
               }
            } catch (IllegalAccessException var4) {
            }
         }
      }
   }
}
