package need.clarity.keybind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import need.clarity.Unload;
import need.clarity.clickgui.ClickGuiLauncher;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;

public class KeybindHandler {
   private static final Set<Integer> heldKeys = new HashSet<>();

   public static void onKeyEvent(int glfwKey, boolean pressed) {
      if (!Unload.isUnloaded()) {
         Map<String, String> binds = ClarityConfig.getKeybinds();
         boolean freshPress = pressed && !heldKeys.contains(glfwKey);
         if (pressed) {
            heldKeys.add(glfwKey);
         } else {
            heldKeys.remove(glfwKey);
         }

         if (freshPress && glfwKey == 344) {
            try {
               ClickGuiLauncher.onRightShift();
            } catch (Throwable var10) {
            }
         }

         for (Entry<String, String> entry : binds.entrySet()) {
            String moduleName = entry.getKey();
            String bindStr = entry.getValue();
            if (mouseButtonIndex(bindStr) < 0) {
               int mappedKey = glfwKeyCode(bindStr);
               if (mappedKey != -1 && mappedKey == glfwKey) {
                  String mode = getBindMode(moduleName);
                  applyBind(moduleName, mode, pressed, freshPress);
               }
            }
         }
      }
   }

   public static void onMouseEvent(int glfwButton, boolean pressed) {
      if (!Unload.isUnloaded()) {
         Map<String, String> binds = ClarityConfig.getKeybinds();
         int code = glfwButton + 10000;
         boolean freshPress = pressed && !heldKeys.contains(code);
         if (pressed) {
            heldKeys.add(code);
         } else {
            heldKeys.remove(code);
         }

         for (Entry<String, String> entry : binds.entrySet()) {
            String moduleName = entry.getKey();
            String bindStr = entry.getValue();
            int mouseBtn = mouseButtonIndex(bindStr);
            if (mouseBtn >= 0 && mouseBtn == glfwButton) {
               String mode = getBindMode(moduleName);
               applyBind(moduleName, mode, pressed, freshPress);
            }
         }
      }
   }

   private static void applyBind(String moduleName, String mode, boolean pressed, boolean freshPress) {
      if ("hold".equals(mode)) {
         if (pressed && freshPress) {
            applyEnabled(moduleName, true);
         } else if (!pressed) {
            applyEnabled(moduleName, false);
         }
      } else if (pressed && freshPress) {
         boolean cur = ClarityConfig.isEnabled(moduleName);
         applyEnabled(moduleName, !cur);
      }
   }

   private static void applyEnabled(String moduleName, boolean enabled) {
      ModuleBase live = ModuleRegistry.get(moduleName);
      if (live != null) {
         live.setEnabled(enabled);
      }
   }

   private static String getBindMode(String moduleName) {
      try {
         JsonObject mod = ClarityConfig.getCurrent().getAsJsonObject(moduleName);
         if (mod == null) {
            return "toggle";
         }

         JsonElement modeEl = mod.get("bindMode");
         if (modeEl != null && modeEl.isJsonPrimitive()) {
            return modeEl.getAsString();
         }
      } catch (Exception var3) {
      }

      return "toggle";
   }

   public static int mouseButtonIndex(String bind) {
      return switch (bind) {
         case "Left Click" -> 0;
         case "Right Click" -> 1;
         case "Middle Click" -> 2;
         case "Mouse 4" -> 3;
         case "Mouse 5" -> 4;
         case "Mouse 6" -> 5;
         case "Mouse 7" -> 6;
         case "Mouse 8" -> 7;
         default -> -1;
      };
   }

   public static int glfwKeyCode(String bind) {
      if (bind != null && !bind.isEmpty()) {
         if (bind.length() == 1) {
            char c = Character.toUpperCase(bind.charAt(0));
            if (c >= 'A' && c <= 'Z') {
               return c;
            }

            if (c >= '0' && c <= '9') {
               return c;
            }

            return switch (c) {
               case ' ' -> 32;
               case '\'' -> 39;
               case ',' -> 44;
               case '-' -> 45;
               case '.' -> 46;
               case '/' -> 47;
               case ';' -> 59;
               case '=' -> 61;
               case '[' -> 91;
               case '\\' -> 92;
               case ']' -> 93;
               case '`' -> 96;
               default -> -1;
            };
         } else {
            return switch (bind) {
               case "Escape" -> 256;
               case "Enter" -> 257;
               case "Tab" -> 258;
               case "Backspace" -> 259;
               case "Delete" -> 261;
               case "Insert" -> 260;
               case "Home" -> 268;
               case "End" -> 269;
               case "PageUp" -> 266;
               case "PageDown" -> 267;
               case "ArrowUp" -> 265;
               case "ArrowDown" -> 264;
               case "ArrowLeft" -> 263;
               case "ArrowRight" -> 262;
               case "Shift" -> 340;
               case "Control" -> 341;
               case "Alt" -> 342;
               case "CapsLock" -> 280;
               case "NumLock" -> 282;
               case "ScrollLock" -> 281;
               case "PrintScreen" -> 283;
               case "Pause" -> 284;
               case "Space" -> 32;
               case "F1" -> 290;
               case "F2" -> 291;
               case "F3" -> 292;
               case "F4" -> 293;
               case "F5" -> 294;
               case "F6" -> 295;
               case "F7" -> 296;
               case "F8" -> 297;
               case "F9" -> 298;
               case "F10" -> 299;
               case "F11" -> 300;
               case "F12" -> 301;
               default -> -1;
            };
         }
      } else {
         return -1;
      }
   }
}
