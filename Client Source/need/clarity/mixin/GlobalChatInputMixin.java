package need.clarity.mixin;

import need.clarity.Unload;
import need.clarity.friends.online.DirectMessages;
import need.clarity.friends.online.GlobalChat;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class GlobalChatInputMixin {
   @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true, require = 0)
   private void clarity$globalChat(String message, boolean addToRecentChat, CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         try {
            if (DirectMessages.intercept(message, addToRecentChat) || GlobalChat.intercept(message, addToRecentChat)) {
               ci.cancel();
            }
         } catch (Throwable var5) {
         }
      }
   }
}
