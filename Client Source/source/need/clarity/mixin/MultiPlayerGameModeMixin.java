package need.clarity.mixin;

import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.CrystalAura;
import need.clarity.modules.utility.FakePlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
   @Unique
   private boolean mpgm$swapActive = false;
   @Unique
   private float mpgm$savedYRot = 0.0F;
   @Unique
   private float mpgm$savedXRot = 0.0F;

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void mpgm$attackHead(Player player, Entity target, CallbackInfo ci) {
      CrystalAura.onLocalAttack(target);
      if (FakePlayer.onLocalAttack(target)) {
         player.resetAttackStrengthTicker();
         ci.cancel();
      }
   }

   @Inject(method = "useItem", at = @At("HEAD"))
   private void mpgm$useItemHead(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      if (!AimAssist.hasSilentRotation()) {
         this.mpgm$swapActive = false;
      } else {
         float silentYaw = AimAssist.getSilentServerYaw();
         float silentPitch = AimAssist.getSilentServerPitch();
         float visualYaw = player.getYRot();
         float visualPitch = player.getXRot();
         float yawDelta = wrapDeg(silentYaw - visualYaw);
         float pitchDelta = wrapDeg(silentPitch - visualPitch);
         if (Math.abs(yawDelta) < 0.05F && Math.abs(pitchDelta) < 0.05F) {
            this.mpgm$swapActive = false;
         } else {
            this.mpgm$savedYRot = visualYaw;
            this.mpgm$savedXRot = visualPitch;
            this.mpgm$swapActive = true;
            AimAssist.commitUseItemRotation(silentYaw, silentPitch);
            player.setYRot(silentYaw);
            player.setXRot(silentPitch);
         }
      }
   }

   @Inject(method = "useItem", at = @At("RETURN"))
   private void mpgm$useItemReturn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      if (this.mpgm$swapActive) {
         this.mpgm$swapActive = false;
         player.setYRot(this.mpgm$savedYRot);
         player.setXRot(this.mpgm$savedXRot);
      }
   }

   private static float wrapDeg(float a) {
      a %= 360.0F;
      if (a >= 180.0F) {
         a -= 360.0F;
      }

      if (a < -180.0F) {
         a += 360.0F;
      }

      return a;
   }
}
