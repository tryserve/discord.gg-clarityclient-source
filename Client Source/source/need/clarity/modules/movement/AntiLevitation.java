package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

public class AntiLevitation extends ModuleBase {
   public AntiLevitation() {
      super("Anti Levitation", "Movement");
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.removeEffect(MobEffects.LEVITATION);
      }
   }
}
