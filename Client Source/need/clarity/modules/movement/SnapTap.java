package need.clarity.modules.movement;

import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;

public class SnapTap extends ModuleBase {
   private boolean wasLeftPressed = false;
   private boolean wasRightPressed = false;
   private boolean wasForwardPressed = false;
   private boolean wasBackPressed = false;
   private boolean lastHorizontal = false;
   private boolean lastVertical = false;

   public SnapTap() {
      super("Snap Tap", "Utility");
      PerMovementEvents.register(this::onMove);
   }

   @Override
   public void onDisable() {
      this.wasLeftPressed = false;
      this.wasRightPressed = false;
      this.wasForwardPressed = false;
      this.wasBackPressed = false;
   }

   private void onMove(MoveFix.EventMovementInput e) {
      if (this.isEnabled()) {
         if (Minecraft.getInstance().screen == null) {
            boolean leftJustPressed = e.left && !this.wasLeftPressed;
            boolean rightJustPressed = e.right && !this.wasRightPressed;
            if (leftJustPressed && !rightJustPressed) {
               this.lastHorizontal = false;
            }

            if (rightJustPressed && !leftJustPressed) {
               this.lastHorizontal = true;
            }

            if (e.left && e.right) {
               if (this.lastHorizontal) {
                  e.left = false;
               } else {
                  e.right = false;
               }
            }

            boolean forwardJustPressed = e.forward && !this.wasForwardPressed;
            boolean backJustPressed = e.back && !this.wasBackPressed;
            if (forwardJustPressed && !backJustPressed) {
               this.lastVertical = true;
            }

            if (backJustPressed && !forwardJustPressed) {
               this.lastVertical = false;
            }

            if (e.forward && e.back) {
               if (this.lastVertical) {
                  e.back = false;
               } else {
                  e.forward = false;
               }
            }

            this.wasLeftPressed = e.originalLeft;
            this.wasRightPressed = e.originalRight;
            this.wasForwardPressed = e.originalForward;
            this.wasBackPressed = e.originalBack;
         }
      }
   }
}
