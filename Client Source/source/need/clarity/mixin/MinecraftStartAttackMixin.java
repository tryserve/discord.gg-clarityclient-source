package need.clarity.mixin;

import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.Unload;
import need.clarity.friends.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftStartAttackMixin {
   @Shadow
   public LocalPlayer player;
   @Shadow
   public HitResult hitResult;

   @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
   private void cl$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
      if (this.player != null && !Unload.isUnloaded()) {
         Entity target = null;
         if (this.hitResult != null && this.hitResult.getType() == Type.ENTITY) {
            target = ((EntityHitResult)this.hitResult).getEntity();
         }

         if (target != null && Friends.blockHits(target)) {
            cir.setReturnValue(false);
         } else {
            EventStartAttack event = new EventStartAttack(this.player, target);
            PerAttackEvents.fire(event);
            if (event.isCancelled()) {
               cir.setReturnValue(false);
            }
         }
      }
   }
}

