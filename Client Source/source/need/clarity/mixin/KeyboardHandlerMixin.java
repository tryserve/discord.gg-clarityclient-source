package need.clarity.mixin;

import need.clarity.Unload;
import need.clarity.clickgui.ClickGuiLauncher;
import need.clarity.keybind.KeybindHandler;
import need.clarity.menu.MenuScreen;
import need.clarity.nvg.HudEditorPanel;
import need.clarity.nvg.HudLayout;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void clarity$hudEditorChar(long window, CharacterEvent e, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (!Unload.isUnloaded()) {
         if (HudLayout.isEditing() && mc.screen == null) {
            HudEditorPanel.onChar(e.codepointAsString());
            ci.cancel();
         }
      }
   }

   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void onKeyPress(long windowPtr, int action, KeyEvent keyEvent, CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.screen instanceof MenuScreen menu && action == 1 && keyEvent.key() == 344) {
            try {
               ClickGuiLauncher.onRightShift(menu);
            } catch (Throwable var9) {
            }

            ci.cancel();
         } else if (mc.player != null) {
            int key = keyEvent.key();
            if (action == 0 && (mc.screen != null || HudLayout.isEditing())) {
               KeybindHandler.onKeyEvent(key, false);
            }

            if (HudLayout.isEditing()) {
               if (mc.screen == null) {
                  if ((action == 1 || action == 2) && HudEditorPanel.onKey(key)) {
                     ci.cancel();
                     return;
                  }

                  if (action == 1 && (key == 345 || key == 256)) {
                     HudLayout.exit();
                  }

                  ci.cancel();
                  return;
               }

               HudLayout.exitNoGrab();
            }

            if (mc.screen == null) {
               if (action == 1 && key == 345) {
                  HudLayout.enter();
                  if (HudLayout.isEditing()) {
                     ci.cancel();
                     return;
                  }
               }

               if (action == 1) {
                  KeybindHandler.onKeyEvent(key, true);
               } else if (action == 0) {
                  KeybindHandler.onKeyEvent(key, false);
               }
            }
         }
      }
   }
}
