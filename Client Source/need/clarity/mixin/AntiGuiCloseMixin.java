package need.clarity.mixin;

import need.clarity.modules.utility.AntiGuiClose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class AntiGuiCloseMixin {
   @Shadow
   public Screen screen;

   @Shadow
   public abstract void setScreen(Screen var1);

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void cl$antiGuiClose(Screen incoming, CallbackInfo ci) {
      Screen current = this.screen;

      Screen result;
      try {
         result = AntiGuiClose.redirect(current, incoming);
      } catch (Throwable t) {
         return;
      }

      if (result != incoming) {
         ci.cancel();
         if (result != current) {
            AntiGuiClose.beginRestore();

            try {
               this.setScreen(result);
            } finally {
               AntiGuiClose.endRestore();
            }
         }
      }
   }
}
