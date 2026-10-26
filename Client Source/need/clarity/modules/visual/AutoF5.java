package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class AutoF5 extends ModuleBase {
   public static AutoF5 INSTANCE;
   private CameraType previous;
   private boolean forced = false;

   public AutoF5() {
      super("Auto F5", "Visual");
      INSTANCE = this;
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled() && mc != null && mc.options != null) {
         this.previous = mc.options.getCameraType();
      }
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         this.previous = mc.options.getCameraType();
      }

      this.forced = false;
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && this.forced && this.previous != null) {
         mc.options.setCameraType(this.previous);
      }

      this.forced = false;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null) {
         boolean containerOpen = mc.screen instanceof AbstractContainerScreen;
         if (containerOpen) {
            if (!this.forced) {
               this.previous = mc.options.getCameraType();
               this.forced = true;
            }

            if (mc.options.getCameraType().isFirstPerson()) {
               mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
         } else if (this.forced) {
            if (this.previous != null) {
               mc.options.setCameraType(this.previous);
            }

            this.forced = false;
         }
      }
   }
}
