package need.clarity.mixin;

import need.clarity.nvg.ChatHud;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class CustomChatRenderMixin {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void cl$hideVanillaChat(GuiGraphicsExtractor g, Font font, int a, int b, int c, DisplayMode mode, boolean ins, CallbackInfo ci) {
      if (ChatHud.active()) {
         ci.cancel();
      }
   }

   @Inject(method = "captureClickableText", at = @At("HEAD"), cancellable = true)
   private void cl$noVanillaHits(ActiveTextCollector collector, int x, int y, DisplayMode mode, CallbackInfo ci) {
      if (ChatHud.active()) {
         ci.cancel();
      }
   }
}
