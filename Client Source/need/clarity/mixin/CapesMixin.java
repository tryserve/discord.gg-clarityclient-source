package need.clarity.mixin;

import com.mojang.authlib.GameProfile;
import need.clarity.modules.visual.Capes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class CapesMixin {
   @Shadow
   @Final
   private GameProfile profile;

   @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
   private void clarity$overrideCape(CallbackInfoReturnable<PlayerSkin> cir) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null) {
         if (this.profile.id().equals(mc.player.getUUID())) {
            Capes module = Capes.INSTANCE;
            if (module != null && module.isEnabled()) {
               Identifier capeTex = module.getActiveTexture();
               if (capeTex != null) {
                  PlayerSkin original = (PlayerSkin)cir.getReturnValue();
                  Texture newCape = new ResourceTexture(capeTex, capeTex);
                  cir.setReturnValue(new PlayerSkin(original.body(), newCape, newCape, original.model(), original.secure()));
               }
            }
         }
      }
   }
}
