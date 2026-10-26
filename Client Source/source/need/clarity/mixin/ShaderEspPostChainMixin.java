package need.clarity.mixin;

import java.util.Set;
import need.clarity.render.esp.ShaderEspRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PostChain.class)
public abstract class ShaderEspPostChainMixin {
   @Inject(
      method = "load(Lnet/minecraft/client/renderer/PostChainConfig;Lnet/minecraft/client/renderer/texture/TextureManager;Ljava/util/Set;Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/renderer/Projection;Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;)Lnet/minecraft/client/renderer/PostChain;",
      at = @At("RETURN")
   )
   private static void clarity$shaderEspRememberOutlineChain(
      PostChainConfig config, TextureManager textureManager, Set<?> externalTargets, Identifier id, Projection projection, ProjectionMatrixBuffer buffer, CallbackInfoReturnable<PostChain> cir
   ) {
      PostChain chain = (PostChain)cir.getReturnValue();
      if (chain != null && id != null && "entity_outline".equals(id.getPath())) {
         ShaderEspRenderer.registerOutlinePasses(((PostChainAccessor)chain).clarity$getPasses());
      }
   }
}
