package need.clarity.mixin;

import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class FriendTabListMixin {
   @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
   private void cl$friendTabList(CallbackInfoReturnable<Component> cir) {
      if (FriendManager.enabled() && FriendManager.bool("Tab List")) {
         PlayerInfo self = (PlayerInfo)(Object)this;
         String name = self.getProfile().name();
         Friend friend = FriendManager.get(self.getProfile().id().toString(), name);
         if (friend != null) {
            Component current = (Component)cir.getReturnValue();
            if (current == null) {
               current = Component.literal(name);
            }

            Component decorated = Friends.decorate(current, friend, name);
            if (decorated != current) {
               cir.setReturnValue(decorated);
            }
         }
      }
   }
}
