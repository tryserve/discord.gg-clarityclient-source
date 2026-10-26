package need.clarity.mixin;

import need.clarity.nvg.ChatHud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class CustomChatScreenMixin {
   @Shadow
   private boolean insertionClickMode() {
      throw new AssertionError();
   }

   @Shadow
   private boolean handleComponentClicked(Style style, boolean insert) {
      throw new AssertionError();
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void cl$customChatClick(MouseButtonEvent e, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (e.button() == 0 && ChatHud.active()) {
         Style st = ChatHud.hovered();
         if (st != null && this.handleComponentClicked(st, this.insertionClickMode())) {
            cir.setReturnValue(true);
         }
      }
   }
}
