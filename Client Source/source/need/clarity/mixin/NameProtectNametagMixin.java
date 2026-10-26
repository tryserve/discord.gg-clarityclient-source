package need.clarity.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.modules.utility.NameProtect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class NameProtectNametagMixin {
   private static final Map<Class<?>, Field> NP_NAME_TAG_FIELD = new ConcurrentHashMap<>();

   private static Field cl$npFindNameTagField(EntityRenderState state) {
      return NP_NAME_TAG_FIELD.computeIfAbsent(state.getClass(), cls -> {
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
   private void cl$nameProtectNametag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      NameProtect module = NameProtect.getInstance();
      if (module != null && module.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (entity == mc.player) {
            Field f = cl$npFindNameTagField(state);
            if (f != null) {
               try {
                  if (!(f.get(state) instanceof Component current)) {
                     return;
                  }

                  Component replaced = NameProtect.replaceInTree(current);
                  if (replaced != current) {
                     f.set(state, replaced);
                  }
               } catch (Exception var11) {
               }
            }
         }
      }
   }
}
