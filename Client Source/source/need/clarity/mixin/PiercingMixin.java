package need.clarity.mixin;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.Piercing;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class PiercingMixin {
   @Shadow
   public HitResult hitResult;
   @Shadow
   public Entity crosshairPickEntity;

   @Inject(method = "handleKeybinds", at = @At("HEAD"))
   private void cl$piercingHandleKeybinds(CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.player != null && mc.level != null) {
         Piercing piercing = (Piercing)ModuleRegistry.get("Piercing");
         if (piercing != null && piercing.isEnabled()) {
            piercing.onHandleInput(mc);
         }
      }
   }
}
