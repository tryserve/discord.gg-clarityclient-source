package need.clarity.mixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
   @Accessor("allMessages")
   List<GuiMessage> clarity$allMessages();

   @Accessor("chatScrollbarPos")
   int clarity$scroll();

   @Accessor("chatScrollbarPos")
   void clarity$setScroll(int var1);
}
