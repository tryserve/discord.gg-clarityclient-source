package need.clarity.mixin;

import need.clarity.modules.utility.GhostHand;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class GhostHandMixin {
   @Inject(method = "handleKeybinds", at = @At("HEAD"))
   private void cl$ghostHandHandleKeybinds(CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.player != null && mc.level != null) {
         GhostHand module = GhostHand.getInstance();
         if (module != null && module.isEnabled()) {
            module.onPick(mc);
         }
      }
   }
}
