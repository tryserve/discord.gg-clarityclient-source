package need.clarity.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.modules.visual.HealthTags;
import need.clarity.modules.visual.Nametags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class HealthTagsMixin {
   private static final Map<Class<?>, Field> NAME_TAG_FIELD = new ConcurrentHashMap<>();

   private static Field findNameTagField(EntityRenderState state) {
      return NAME_TAG_FIELD.computeIfAbsent(state.getClass(), cls -> {
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
   private void clarity$hideForNametags(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      try {
         if (Nametags.hidesVanilla(entity)) {
            state.nameTag = null;
            state.scoreText = null;
         }
      } catch (Throwable var6) {
      }
   }

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void clarity$appendHealthTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      HealthTags module = HealthTags.getInstance();
      if (module != null && module.isEnabled()) {
         if (entity instanceof LivingEntity living) {
            Minecraft mc = Minecraft.getInstance();
            if (entity != mc.player) {
               Field f = findNameTagField(state);
               if (f != null) {
                  try {
                     if (!(f.get(state) instanceof Component current)) {
                        return;
                     }

                     int hp = HealthTags.getDisplayHp(living);
                     int color = HealthTags.getHealthColor(living.getHealth(), living.getMaxHealth());
                     int rgb = color & 16777215;
                     MutableComponent suffix = Component.literal("  " + hp).setStyle(Style.EMPTY.withColor(rgb));
                     f.set(state, current.copy().append(suffix));
                  } catch (Exception var15) {
                  }
               }
            }
         }
      }
   }
}
