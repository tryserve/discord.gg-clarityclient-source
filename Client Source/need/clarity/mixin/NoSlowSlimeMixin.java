package need.clarity.mixin;

import need.clarity.modules.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public abstract class NoSlowSlimeMixin {
   @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true, require = 0)
   private void cl$noSlowSlime(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && entity == mc.player) {
         if (NoSlow.skipEnv("Slime")) {
            ci.cancel();
         }
      }
   }
}
