package need.clarity.bridge;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.modules.utility.NameProtect;
import need.clarity.modules.visual.HealthTags;
import need.clarity.modules.visual.Nametags;
import need.clarity.render.esp.ShaderEspRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class EntityRendererBridge {
   private static final Map<Class<?>, Field> NAME_TAG_CACHE = new ConcurrentHashMap<>();

   private EntityRendererBridge() {
   }

   public static void onExtractRenderStateTail(Object entityObj, Object stateObj, float partialTick) {
      Entity entity = (Entity)entityObj;
      EntityRenderState state = (EntityRenderState)stateObj;

      try {
         if (entity instanceof Player player && FriendManager.bool("Nametags")) {
            Friend friend = Friends.of(player);
            Field f = friend == null ? null : findNameTagField(state);
            if (f != null && f.get(state) instanceof Component current) {
               Component decorated = Friends.decorate(current, friend, Friends.nameOf(player));
               if (decorated != current) {
                  f.set(state, decorated);
               }
            }
         }
      } catch (Throwable var18) {
      }

      try {
         if (Nametags.hidesVanilla(entity)) {
            state.nameTag = null;
            state.scoreText = null;
         }
      } catch (Throwable var17) {
      }

      try {
         ShaderEspRenderer.tagState(entity, state);
      } catch (Throwable var16) {
      }

      try {
         HealthTags ht = HealthTags.getInstance();
         if (ht != null && ht.isEnabled() && entity instanceof LivingEntity living) {
            Minecraft mc = Minecraft.getInstance();
            if (entity != mc.player) {
               Field f = findNameTagField(state);
               if (f != null && f.get(state) instanceof Component current) {
                  int hp = HealthTags.getDisplayHp(living);
                  int color = HealthTags.getHealthColor(living.getHealth(), living.getMaxHealth()) & 16777215;
                  MutableComponent suffix = Component.literal("  " + hp).setStyle(Style.EMPTY.withColor(color));
                  f.set(state, current.copy().append(suffix));
               }
            }
         }
      } catch (Throwable var15) {
      }

      try {
         NameProtect np = NameProtect.getInstance();
         if (np != null && np.isEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (entity == mc.player) {
               Field f = findNameTagField(state);
               if (f != null && f.get(state) instanceof Component current) {
                  Component replaced = NameProtect.replaceInTree(current);
                  if (replaced != current) {
                     f.set(state, replaced);
                  }
               }
            }
         }
      } catch (Throwable var14) {
      }
   }

   public static boolean shouldHideCorpse(Object entity) {
      try {
         return entity == null ? false : ShaderEspRenderer.isHidden(((Entity)entity).getId());
      } catch (Throwable t) {
         return false;
      }
   }

   private static Field findNameTagField(EntityRenderState state) {
      return NAME_TAG_CACHE.computeIfAbsent(state.getClass(), cls -> {
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
}
