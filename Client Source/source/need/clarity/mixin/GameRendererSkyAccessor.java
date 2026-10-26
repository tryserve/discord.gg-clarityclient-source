package need.clarity.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererSkyAccessor {
   @Accessor("levelProjectionMatrixBuffer")
   ProjectionMatrixBuffer clarity$getLevelProjectionMatrixBuffer();
}
