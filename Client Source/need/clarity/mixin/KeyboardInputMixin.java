package need.clarity.mixin;

import need.clarity.PerMovementEvents;
import need.clarity.Unload;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
   @Shadow
   @Final
   private Options options;
   @Unique
   private boolean cl$originalForward;
   @Unique
   private boolean cl$originalBack;
   @Unique
   private boolean cl$originalLeft;
   @Unique
   private boolean cl$originalRight;
   @Unique
   private boolean cl$originalJump;
   @Unique
   private boolean cl$originalSneak;
   @Unique
   private boolean cl$originalSprint;

   @Inject(method = "tick", at = @At("HEAD"))
   private void cl$onTickHead(CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         this.cl$originalForward = this.options.keyUp.isDown();
         this.cl$originalBack = this.options.keyDown.isDown();
         this.cl$originalLeft = this.options.keyLeft.isDown();
         this.cl$originalRight = this.options.keyRight.isDown();
         this.cl$originalJump = this.options.keyJump.isDown();
         this.cl$originalSneak = this.options.keyShift.isDown();
         this.cl$originalSprint = this.options.keySprint.isDown();
         MoveFix.EventMovementInput event = new MoveFix.EventMovementInput(
            this.cl$originalForward, this.cl$originalBack, this.cl$originalLeft, this.cl$originalRight, this.cl$originalJump, this.cl$originalSneak, this.cl$originalSprint
         );
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

         this.options.keyUp.setDown(event.forward);
         this.options.keyDown.setDown(event.back);
         this.options.keyLeft.setDown(event.left);
         this.options.keyRight.setDown(event.right);
         this.options.keyJump.setDown(event.jump);
         this.cl$setKey(this.options.keyShift, event.sneak, (Boolean)this.options.toggleCrouch().get());
         this.cl$setKey(this.options.keySprint, event.sprint, (Boolean)this.options.toggleSprint().get());
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void cl$onTickReturn(CallbackInfo ci) {
      this.options.keyUp.setDown(this.cl$originalForward);
      this.options.keyDown.setDown(this.cl$originalBack);
      this.options.keyLeft.setDown(this.cl$originalLeft);
      this.options.keyRight.setDown(this.cl$originalRight);
      this.options.keyJump.setDown(this.cl$originalJump);
      this.cl$setKey(this.options.keyShift, this.cl$originalSneak, (Boolean)this.options.toggleCrouch().get());
      this.cl$setKey(this.options.keySprint, this.cl$originalSprint, (Boolean)this.options.toggleSprint().get());
      if (MoveFix.isMovementFrozen()) {
         ((ClientInput)(Object)this).keyPresses = Input.EMPTY;
      }
   }

   @Unique
   private void cl$setKey(KeyMapping key, boolean desired, boolean toggleEnabled) {
      if (key instanceof ToggleKeyMapping && toggleEnabled) {
         if (key.isDown() != desired) {
            key.setDown(true);
         }
      } else {
         key.setDown(desired);
      }
   }
}

