package need.clarity.mixin;

import need.clarity.modules.visual.Ambience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ClientLevelAmbienceMixin {
   @Inject(method = "getOverworldClockTime()J", at = @At("RETURN"), cancellable = true)
   private void ambience$overrideOverworldClockTime(CallbackInfoReturnable<Long> cir) {
      if ((Object)this instanceof ClientLevel && Ambience.isOverrideTimeEnabled()) {
         cir.setReturnValue(Ambience.getOverriddenDayTime());
      }
   }

   @Inject(method = "getDefaultClockTime()J", at = @At("RETURN"), cancellable = true)
   private void ambience$overrideDefaultClockTime(CallbackInfoReturnable<Long> cir) {
      if ((Object)this instanceof ClientLevel && Ambience.isOverrideTimeEnabled()) {
         cir.setReturnValue(Ambience.getOverriddenDayTime());
      }
   }

   @Inject(method = "getRainLevel(F)F", at = @At("RETURN"), cancellable = true)
   private void ambience$overrideRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
      if ((Object)this instanceof ClientLevel) {
         if (Ambience.isOverrideWeatherEnabled()) {
            cir.setReturnValue("Clear".equals(Ambience.getWeatherMode()) ? 0.0F : 1.0F);
         }
      }
   }

   @Inject(method = "getThunderLevel(F)F", at = @At("RETURN"), cancellable = true)
   private void ambience$overrideThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
      if ((Object)this instanceof ClientLevel) {
         if (Ambience.isOverrideWeatherEnabled()) {
            cir.setReturnValue("Thunder".equals(Ambience.getWeatherMode()) ? 1.0F : 0.0F);
         }
      }
   }
}
