package need.clarity.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatScreen.class)
public abstract class NameProtectChatScreenMixin {
   @ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
   private String cl$nameProtectChatInput(String input) {
      return input;
   }
}
