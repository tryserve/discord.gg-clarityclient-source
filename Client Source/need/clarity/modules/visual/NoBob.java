package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class NoBob extends ModuleBase {
   public static NoBob INSTANCE;
   private Boolean previous;

   public NoBob() {
      super("No Bob", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.apply();
      }
   }

   @Override
   public void onEnable() {
      this.apply();
   }

   @Override
   public void onDisable() {
      this.restore();
   }

   private void apply() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null) {
         if (this.previous == null) {
            this.previous = (Boolean)mc.options.bobView().get();
         }

         mc.options.bobView().set(false);
      }
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null && this.previous != null) {
         mc.options.bobView().set(this.previous);
         this.previous = null;
      }
   }
}
