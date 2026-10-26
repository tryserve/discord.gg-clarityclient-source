package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class NoFOV extends ModuleBase {
   public static NoFOV INSTANCE;
   private Double previous;

   public NoFOV() {
      super("No FOV", "Visual");
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
            this.previous = (Double)mc.options.fovEffectScale().get();
         }

         mc.options.fovEffectScale().set(0.0);
      }
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null && this.previous != null) {
         mc.options.fovEffectScale().set(this.previous);
         this.previous = null;
      }
   }
}
