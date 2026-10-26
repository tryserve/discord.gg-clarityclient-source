package need.clarity.mixin;

import need.clarity.menu.MainMenuScreen;
import need.clarity.menu.MenuRenderer;
import need.clarity.modules.visual.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MainMenuRedirectMixin {
   @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
   private Screen clarity$redirectTitle(Screen screen) {
      return screen instanceof TitleScreen && MainMenu.active() && !MenuRenderer.hasFailed() ? new MainMenuScreen() : screen;
   }
}
