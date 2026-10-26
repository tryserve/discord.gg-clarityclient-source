package need.clarity.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.Optional;
import need.clarity.util.ThemeTextures;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
   @Inject(
      method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V",
      at = @At("TAIL")
   )
   private void cl$themeRecolor(Identifier id, FrameSize frameSize, NativeImage image, Optional<?> animation, List<?> metadata, Optional<?> texture, CallbackInfo ci) {
      try {
         if (id != null && ThemeTextures.shouldRecolor(id.getPath())) {
            ThemeTextures.recolor(image);
         }
      } catch (Throwable var9) {
      }
   }
}
