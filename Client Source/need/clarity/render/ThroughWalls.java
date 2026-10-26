package need.clarity.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import sun.misc.Unsafe;

public final class ThroughWalls {
   private static final Map<RenderType, RenderType> CACHE = new ConcurrentHashMap<>();
   private static final Map<RenderType, Boolean> CONVERTED = new ConcurrentHashMap<>();
   private static final AtomicInteger COUNTER = new AtomicInteger();
   private static final Set<RenderType> FAILED_LOGGED = ConcurrentHashMap.newKeySet();
   private static final Unsafe UNSAFE = loadUnsafe();

   private ThroughWalls() {
   }

   private static Unsafe loadUnsafe() {
      try {
         Field f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         return (Unsafe)f.get(null);
      } catch (Throwable t) {
         return null;
      }
   }

   public static RenderType convert(RenderType type) {
      if (type == null) {
         return null;
      }

      if (!ChamsRenderer.isOwnType(type) && !CONVERTED.containsKey(type)) {
         RenderType cached = CACHE.get(type);
         if (cached != null) {
            return cached;
         }

         RenderType result;
         try {
            result = build(type);
         } catch (Throwable t) {
            if (FAILED_LOGGED.add(type)) {
            }

            result = type;
         }

         CACHE.put(type, result);
         if (result != type) {
            CONVERTED.put(result, Boolean.TRUE);
         }

         return result;
      } else {
         return type;
      }
   }

   private static RenderType build(RenderType type) throws Exception {
      if (UNSAFE == null) {
         throw new IllegalStateException("Unsafe unavailable");
      }

      Field setupField = findFieldOfType(type.getClass(), RenderSetup.class);
      if (setupField == null) {
         throw new IllegalStateException("RenderType has no RenderSetup field");
      }

      Object setup = setupField.get(type);
      Field pipeField = findFieldOfType(setup.getClass(), RenderPipeline.class);
      if (pipeField == null) {
         throw new IllegalStateException("RenderSetup has no RenderPipeline field");
      }

      Object pipeline = pipeField.get(setup);
      int n = COUNTER.incrementAndGet();
      Object newPipeline = shallowCopy(pipeline);
      setDepthState(newPipeline);

      for (Class<?> c = newPipeline.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == Identifier.class && f.getName().equalsIgnoreCase("location")) {
               f.setAccessible(true);
               f.set(newPipeline, Identifier.parse("clarity:chams_tw_" + n));
            }
         }
      }

      Object newSetup = shallowCopy(setup);
      pipeField.set(newSetup, newPipeline);
      return RenderType.create("clarity:chams_tw_" + n, (RenderSetup)newSetup);
   }

   private static void setDepthState(Object pipeline) throws Exception {
      DepthStencilState state = ChamsRenderer.throughWallsDepthState();
      Field depthField = null;

      for (Class<?> c = pipeline.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
               boolean depthType = f.getType() == DepthStencilState.class;
               boolean optional = f.getType() == Optional.class && f.getName().toLowerCase().contains("depth");
               if (depthType || optional) {
                  depthField = f;
                  break;
               }
            }
         }

         if (depthField != null) {
            break;
         }
      }

      if (depthField == null) {
         throw new IllegalStateException("RenderPipeline has no depth state field");
      }

      depthField.setAccessible(true);
      if (depthField.getType() == Optional.class) {
         depthField.set(pipeline, state != null ? Optional.of(state) : Optional.empty());
      } else {
         depthField.set(pipeline, state);
      }
   }

   private static Field findFieldOfType(Class<?> start, Class<?> wanted) {
      for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && wanted.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               return f;
            }
         }
      }

      return null;
   }

   private static Object shallowCopy(Object src) throws Exception {
      Object copy = UNSAFE.allocateInstance(src.getClass());

      for (Class<?> c = src.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
               f.setAccessible(true);
               f.set(copy, f.get(src));
            }
         }
      }

      return copy;
   }
}
