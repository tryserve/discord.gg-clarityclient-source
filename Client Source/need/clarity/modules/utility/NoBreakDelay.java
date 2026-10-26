package need.clarity.modules.utility;

import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.Minecraft;

public class NoBreakDelay extends ModuleBase {
   private final Minecraft mc = Minecraft.getInstance();

   public NoBreakDelay() {
      super("No Break Delay", "Utility");
   }

   @Override
   public void onTick() {
      if (this.mc.gameMode != null) {
         AccessorCompat.setDestroyDelay(this.mc.gameMode, 0);
      }
   }
}
