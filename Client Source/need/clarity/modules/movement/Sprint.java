package need.clarity.modules.movement;

import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public class Sprint extends ModuleBase {
   public Sprint() {
      super("Sprint", "Movement");
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         Player player = mc.player;
         if (this.canSprint(mc, player)) {
            player.setSprinting(true);
         }
      }
   }

   private boolean canSprint(Minecraft mc, Player player) {
      MoveFix.EventMovementInput event = PerMovementEvents.lastEvent;
      boolean movingForward = event != null ? event.forward : mc.options.keyUp.isDown();
      if (!movingForward) {
         return false;
      } else {
         FoodData food = player.getFoodData();
         if (food.getFoodLevel() <= 6) {
            return false;
         } else if (player.isShiftKeyDown()) {
            return false;
         } else if (player.isBlocking()) {
            return false;
         } else if (player.isUsingItem()) {
            return false;
         } else {
            return player.hasEffect(MobEffects.BLINDNESS) ? false : player.onGround() || player.isSprinting();
         }
      }
   }
}
