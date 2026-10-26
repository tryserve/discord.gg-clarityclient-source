package need.clarity.bridge;

import need.clarity.clickgui.ClickGuiLauncher;
import need.clarity.keybind.KeybindHandler;
import need.clarity.menu.MenuScreen;
import need.clarity.nvg.HudEditorPanel;
import need.clarity.nvg.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class KeyboardHandlerBridge {
   private KeyboardHandlerBridge() {
   }

   public static boolean onKeyPress(long windowPtr, int action, KeyEvent keyEvent) {
      try {
         Minecraft mc = Minecraft.getInstance();
         int key = keyEvent.key();
         if (mc.screen instanceof MenuScreen menu && action == 1 && key == 344) {
            try {
               ClickGuiLauncher.onRightShift(menu);
            } catch (Throwable var8) {
            }

            return true;
         }

         if (mc.player == null) {
            return false;
         }

         if (action == 0 && (mc.screen != null || HudLayout.isEditing())) {
            KeybindHandler.onKeyEvent(key, false);
         }

         if (HudLayout.isEditing()) {
            if (mc.screen == null) {
               if ((action == 1 || action == 2) && HudEditorPanel.onKey(key)) {
                  return true;
               }

               if (action == 1 && (key == 345 || key == 256)) {
                  HudLayout.exit();
               }

               return true;
            }

            HudLayout.exitNoGrab();
         }

         if (mc.screen != null) {
            return false;
         }

         if (action == 1 && key == 345) {
            HudLayout.enter();
            if (HudLayout.isEditing()) {
               return true;
            }
         }

         if (action == 1) {
            KeybindHandler.onKeyEvent(key, true);
         } else if (action == 0) {
            KeybindHandler.onKeyEvent(key, false);
         }
      } catch (Throwable var9) {
      }

      return false;
   }

   public static boolean onCharTyped(CharacterEvent e) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (HudLayout.isEditing() && mc.screen == null) {
            HudEditorPanel.onChar(e.codepointAsString());
            return true;
         }
      } catch (Throwable var2) {
      }

      return false;
   }
}
