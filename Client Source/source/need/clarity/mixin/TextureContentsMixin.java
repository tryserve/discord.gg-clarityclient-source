package need.clarity.mixin;

import need.clarity.util.ThemeTextures;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureContents.class)
public class TextureContentsMixin {
   @Inject(method = "load", at = @At("RETURN"))
   private static void cl$themeRecolor(ResourceManager manager, Identifier id, CallbackInfoReturnable<TextureContents> cir) {
      try {
         TextureContents contents = (TextureContents)cir.getReturnValue();
         if (contents != null && id != null && ThemeTextures.shouldRecolor(id.getPath())) {
            ThemeTextures.recolor(contents.image());
         }
      } catch (Throwable var4) {
      }
   }
}
