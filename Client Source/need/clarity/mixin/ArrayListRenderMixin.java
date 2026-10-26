package need.clarity.mixin;

import need.clarity.Unload;
import need.clarity.nvg.ArrayListRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class ArrayListRenderMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void clarity$arrayListOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && !Unload.isUnloaded()) {
         ArrayListRenderer.render(context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.getWindow().getWidth(), mc.getWindow().getHeight());
      }
   }
}
