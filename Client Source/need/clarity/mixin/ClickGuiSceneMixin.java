package need.clarity.mixin;

import need.clarity.Unload;
import need.clarity.clickgui.ClickGuiRenderer;
import need.clarity.clickgui.ClickGuiScreen;
import need.clarity.clickgui.Gui3DRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class ClickGuiSceneMixin {
   @Inject(method = "renderLevel", at = @At("TAIL"))
   private void clarity$captureClickGuiScene(DeltaTracker tickCounter, CallbackInfo ci) {
      if (!Unload.isUnloaded()) {
         if (Minecraft.getInstance().screen instanceof ClickGuiScreen screen) {
            ClickGuiRenderer.captureScene(screen.gui());
         }

         Gui3DRenderer.renderWorld();
      }
   }
}
