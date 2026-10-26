package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class AntiHunger extends ModuleBase {
   public static AntiHunger INSTANCE;

   public AntiHunger() {
      super("Anti Hunger", "Movement");
      INSTANCE = this;
   }

   public boolean shouldSpoof() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      } else if (!mc.player.onGround()) {
         return false;
      } else {
         return mc.player.fallDistance > 0.5 ? false : mc.gameMode == null || !mc.gameMode.isDestroying();
      }
   }
}
