package need.clarity.mixin;

import need.clarity.modules.visual.CustomScoreboard;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class CustomScoreboardMixin {
   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void cl$customScoreboard(GuiGraphicsExtractor g, Objective objective, CallbackInfo ci) {
      boolean drew = false;

      try {
         drew = CustomScoreboard.render(g, objective);
      } catch (Throwable var6) {
      }

      if (drew) {
         ci.cancel();
      }
   }
}
