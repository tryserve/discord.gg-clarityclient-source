package need.clarity.mixin;

import com.mojang.authlib.GameProfile;
import need.clarity.modules.visual.SkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class SkinsMixin {
   @Shadow
   @Final
   private GameProfile profile;

   @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
   private void clarity$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && this.profile != null) {
         if (this.profile.id().equals(mc.player.getUUID())) {
            PlayerSkin s = SkinChanger.override((PlayerSkin)cir.getReturnValue());
            if (s != null) {
               cir.setReturnValue(s);
            }
         }
      }
   }
}
