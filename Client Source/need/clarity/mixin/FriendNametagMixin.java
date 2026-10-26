package need.clarity.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 950)
public abstract class FriendNametagMixin {
   private static final Map<Class<?>, Field> CL_FRIEND_TAG_FIELD = new ConcurrentHashMap<>();

   private static Field cl$friendFindNameTagField(EntityRenderState state) {
      return CL_FRIEND_TAG_FIELD.computeIfAbsent(state.getClass(), cls -> {
         for (Class<?> c = (Class<?>)cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
               if (Component.class.isAssignableFrom(f.getType())) {
                  f.setAccessible(true);
                  return f;
               }
            }
         }

         return null;
      });
   }

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void cl$friendNametag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      if (entity instanceof Player player) {
         if (FriendManager.bool("Nametags")) {
            Friend friend = Friends.of(player);
            if (friend != null) {
               Field f = cl$friendFindNameTagField(state);
               if (f != null) {
                  try {
                     if (!(f.get(state) instanceof Component current)) {
                        return;
                     }

                     Component decorated = Friends.decorate(current, friend, Friends.nameOf(player));
                     if (decorated != current) {
                        f.set(state, decorated);
                     }
                  } catch (Exception var11) {
                  }
               }
            }
         }
      }
   }
}
