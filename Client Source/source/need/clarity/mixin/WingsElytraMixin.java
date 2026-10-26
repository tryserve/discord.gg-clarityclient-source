package need.clarity.mixin;

import need.clarity.modules.visual.Wings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class WingsElytraMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$replaceElytraWithWings(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      try {
         if (player != Minecraft.getInstance().player) {
            return;
         }

         if (!Wings.shouldReplaceElytra()) {
            return;
         }

         if (state.chestEquipment != null && state.chestEquipment.has(DataComponents.GLIDER)) {
            state.chestEquipment = ItemStack.EMPTY;
         }
      } catch (Throwable var6) {
      }
   }
}
