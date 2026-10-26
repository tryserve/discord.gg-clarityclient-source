package need.clarity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class EventStartAttack {
   private final Player player;
   private final Entity target;
   private boolean cancelled;

   public EventStartAttack(Player player, Entity target) {
      this.player = player;
      this.target = target;
   }

   public Player getPlayer() {
      return this.player;
   }

   public Entity getTarget() {
      return this.target;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }
}
