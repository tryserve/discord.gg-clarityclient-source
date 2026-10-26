package need.clarity.mixin;

import java.util.List;
import need.clarity.modules.visual.CustomChat;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class CustomChatMixin {
   @Shadow
   @Final
   private List<GuiMessage> allMessages;
   @Shadow
   @Final
   private List<Line> trimmedMessages;

   @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private Component cl$customChatAdd(Component content) {
      try {
         return CustomChat.onAdd(content, this.allMessages, this.trimmedMessages);
      } catch (Throwable t) {
         return content;
      }
   }

   @Inject(method = "addMessage", at = @At("TAIL"))
   private void cl$customChatAfter(CallbackInfo ci) {
      try {
         CustomChat.afterAdd(this.allMessages);
      } catch (Throwable var3) {
      }
   }

   @ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100))
   private int cl$customChatHistory(int v) {
      return CustomChat.historyLimit(v);
   }

   @ModifyVariable(
      method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private ChatGraphicsAccess cl$customChatDraw(ChatGraphicsAccess access) {
      try {
         return CustomChat.decorate(access);
      } catch (Throwable t) {
         return access;
      }
   }

   @Inject(method = "lambda$extractRenderState$1", at = @At("HEAD"), require = 0)
   private static void cl$customChatBgLine(int a, int b, ChatGraphicsAccess g, int c, float d, Line line, int idx, float alpha, CallbackInfo ci) {
      CustomChat.currentLine = line;
      CustomChat.currentAlpha = alpha;
   }

   @Inject(method = "lambda$extractRenderState$1", at = @At("RETURN"), require = 0)
   private static void cl$customChatBgLineEnd(int a, int b, ChatGraphicsAccess g, int c, float d, Line line, int idx, float alpha, CallbackInfo ci) {
      CustomChat.currentLine = null;
   }
}
