package need.clarity.bridge;

import need.clarity.PerMovementEvents;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public final class KeyboardInputBridge {
   private static boolean origForward;
   private static boolean origBack;
   private static boolean origLeft;
   private static boolean origRight;
   private static boolean origJump;
   private static boolean origSneak;
   private static boolean origSprint;

   private KeyboardInputBridge() {
   }

   public static void onTickHead() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.options == null) {
            return;
         }

         Options options = mc.options;
         origForward = options.keyUp.isDown();
         origBack = options.keyDown.isDown();
         origLeft = options.keyLeft.isDown();
         origRight = options.keyRight.isDown();
         origJump = options.keyJump.isDown();
         origSneak = options.keyShift.isDown();
         origSprint = options.keySprint.isDown();
         MoveFix.EventMovementInput event = new MoveFix.EventMovementInput(origForward, origBack, origLeft, origRight, origJump, origSneak, origSprint);
         MoveFix.onMovementInput(event);
         PerMovementEvents.fire(event);
         if (MoveFix.isMovementFrozen()) {
            event.forward = false;
            event.back = false;
            event.left = false;
            event.right = false;
            event.jump = false;
            event.sneak = false;
            event.sprint = false;
         }

         options.keyUp.setDown(event.forward);
         options.keyDown.setDown(event.back);
         options.keyLeft.setDown(event.left);
         options.keyRight.setDown(event.right);
         options.keyJump.setDown(event.jump);
         setKey(options.keyShift, event.sneak, (Boolean)options.toggleCrouch().get());
         setKey(options.keySprint, event.sprint, (Boolean)options.toggleSprint().get());
      } catch (Throwable var3) {
      }
   }

   public static void onTickReturn() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.options == null) {
            return;
         }

         Options options = mc.options;
         options.keyUp.setDown(origForward);
         options.keyDown.setDown(origBack);
         options.keyLeft.setDown(origLeft);
         options.keyRight.setDown(origRight);
         options.keyJump.setDown(origJump);
         setKey(options.keyShift, origSneak, (Boolean)options.toggleCrouch().get());
         setKey(options.keySprint, origSprint, (Boolean)options.toggleSprint().get());
         if (MoveFix.isMovementFrozen() && mc.player != null && mc.player.input instanceof ClientInput ci) {
            ci.keyPresses = Input.EMPTY;
         }
      } catch (Throwable var4) {
      }
   }

   private static void setKey(KeyMapping key, boolean desired, boolean toggleEnabled) {
      try {
         if (key instanceof ToggleKeyMapping && toggleEnabled) {
            if (key.isDown() != desired) {
               key.setDown(true);
            }

            return;
         }

         key.setDown(desired);
      } catch (Throwable var4) {
      }
   }
}
