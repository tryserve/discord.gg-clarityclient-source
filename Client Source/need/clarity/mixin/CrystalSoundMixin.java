package need.clarity.mixin;

import need.clarity.modules.visual.CrystalView;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class CrystalSoundMixin {
   @Inject(method = "play", at = @At("HEAD"), cancellable = true)
   private void cl$muteCrystalBoom(SoundInstance sound, CallbackInfoReturnable<PlayResult> cir) {
      if (CrystalView.shouldMute(sound)) {
         cir.setReturnValue(PlayResult.NOT_STARTED);
      }
   }
}
