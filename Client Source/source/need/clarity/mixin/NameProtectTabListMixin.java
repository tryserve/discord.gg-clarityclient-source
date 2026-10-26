package need.clarity.mixin;

import need.clarity.modules.utility.NameProtect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class NameProtectTabListMixin {
   @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
   private void cl$nameProtectTabList(CallbackInfoReturnable<Component> cir) {
      NameProtect module = NameProtect.getInstance();
      if (module != null && module.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            PlayerInfo self = (PlayerInfo)(Object)this;
            String profileName = self.getProfile().name();
            if (profileName.equals(mc.player.getGameProfile().name())) {
               Component current = (Component)cir.getReturnValue();
               String spoof = module.getSpoofName();
               if (current != null) {
                  String raw = current.getString();
                  if (raw.contains(profileName)) {
                     cir.setReturnValue(Component.literal(raw.replace(profileName, spoof)).withStyle(current.getStyle()));
                  }
               } else {
                  cir.setReturnValue(Component.literal(spoof));
               }
            }
         }
      }
   }
}
