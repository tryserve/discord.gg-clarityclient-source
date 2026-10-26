package need.clarity.mixin;

import need.clarity.modules.visual.CustomChat;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public class CustomChatLineMixin {
   @Inject(method = "accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V", at = @At("HEAD"), require = 0)
   private void cl$line(Line line, int idx, float alpha, CallbackInfo ci) {
      CustomChat.currentLine = line;
      CustomChat.currentAlpha = alpha;
   }

   @Inject(method = "accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V", at = @At("RETURN"), require = 0)
   private void cl$lineEnd(Line line, int idx, float alpha, CallbackInfo ci) {
      CustomChat.currentLine = null;
   }
}
