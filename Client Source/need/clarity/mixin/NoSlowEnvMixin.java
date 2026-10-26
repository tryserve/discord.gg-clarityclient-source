package need.clarity.mixin;

import need.clarity.modules.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class NoSlowEnvMixin {
   private boolean cl$isLocalPlayer() {
      Minecraft mc = Minecraft.getInstance();
      return mc != null && mc.player != null && (Object)(Object)this == mc.player;
   }

   private boolean cl$standingOn(Block block) {
      Entity self = (Entity)(Object)this;

      try {
         BlockState in = self.level().getBlockState(self.blockPosition());
         if (in.is(block)) {
            return true;
         }

         BlockPos below = BlockPos.containing(self.getX(), self.getY() - 0.5000001, self.getZ());
         return self.level().getBlockState(below).is(block);
      } catch (Throwable t) {
         return false;
      }
   }

   @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true, require = 0)
   private void cl$noSlowBlockSpeed(CallbackInfoReturnable<Float> cir) {
      Float f = (Float)cir.getReturnValue();
      if (f != null && !(f >= 1.0F) && this.cl$isLocalPlayer()) {
         if (this.cl$standingOn(Blocks.SOUL_SAND) && NoSlow.skipEnv("Soul Sand") || this.cl$standingOn(Blocks.HONEY_BLOCK) && NoSlow.skipEnv("Honey")) {
            cir.setReturnValue(1.0F);
         }
      }
   }

   @Inject(method = "getBlockJumpFactor", at = @At("RETURN"), cancellable = true, require = 0)
   private void cl$noSlowBlockJump(CallbackInfoReturnable<Float> cir) {
      Float f = (Float)cir.getReturnValue();
      if (f != null && !(f >= 1.0F) && this.cl$isLocalPlayer()) {
         if (this.cl$standingOn(Blocks.HONEY_BLOCK) && NoSlow.skipEnv("Honey")) {
            cir.setReturnValue(1.0F);
         }
      }
   }

   @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true, require = 0)
   private void cl$noSlowStuck(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
      if (state != null && this.cl$isLocalPlayer()) {
         if (state.is(Blocks.COBWEB) && NoSlow.skipEnv("Cobwebs")
            || state.is(Blocks.SWEET_BERRY_BUSH) && NoSlow.skipEnv("Berry Bushes")
            || state.is(Blocks.POWDER_SNOW) && NoSlow.skipEnv("Powder Snow")) {
            ci.cancel();
         }
      }
   }
}
