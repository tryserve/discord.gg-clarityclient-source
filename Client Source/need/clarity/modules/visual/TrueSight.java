package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class TrueSight extends ModuleBase {
   public static TrueSight INSTANCE;

   public TrueSight() {
      super("True Sight", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null && mc.player != null) {
         boolean hideSelf = this.getSetting("Hide Self", true);

         for (Entity e : mc.level.entitiesForRendering()) {
            if ((e != mc.player || !hideSelf) && e.isInvisible()) {
               e.setInvisible(false);
            }
         }
      }
   }
}
