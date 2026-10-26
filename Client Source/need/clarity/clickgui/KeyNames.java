package need.clarity.clickgui;

public final class KeyNames {
   private KeyNames() {
   }

   public static String name(int key) {
      if (key >= 65 && key <= 90) {
         return String.valueOf((char)key);
      }

      if (key >= 48 && key <= 57) {
         return String.valueOf((char)key);
      }

      if (key >= 290 && key <= 301) {
         return "F" + (key - 289);
      }

      return switch (key) {
         case 32 -> "Space";
         case 39 -> "'";
         case 44 -> ",";
         case 45 -> "-";
         case 46 -> ".";
         case 47 -> "/";
         case 59 -> ";";
         case 61 -> "=";
         case 91 -> "[";
         case 92 -> "\\";
         case 93 -> "]";
         case 96 -> "`";
         case 257, 335 -> "Enter";
         case 258 -> "Tab";
         case 259 -> "Backspace";
         case 260 -> "Insert";
         case 261 -> "Delete";
         case 262 -> "ArrowRight";
         case 263 -> "ArrowLeft";
         case 264 -> "ArrowDown";
         case 265 -> "ArrowUp";
         case 266 -> "PageUp";
         case 267 -> "PageDown";
         case 268 -> "Home";
         case 269 -> "End";
         case 280 -> "CapsLock";
         case 281 -> "ScrollLock";
         case 282 -> "NumLock";
         case 283 -> "PrintScreen";
         case 284 -> "Pause";
         case 340, 344 -> "Shift";
         case 341, 345 -> "Control";
         case 342, 346 -> "Alt";
         default -> "Key " + key;
      };
   }
}
