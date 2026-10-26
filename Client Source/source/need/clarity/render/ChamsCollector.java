package need.clarity.render;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class ChamsCollector implements InvocationHandler {
   public static volatile boolean active = false;
   public static volatile boolean hideArmour = false;
   public static volatile boolean armourThroughWalls = true;
   private static final Map<RenderType, Boolean> ARMOUR_TYPES = new ConcurrentHashMap<>();
   private final Object delegate;

   private ChamsCollector(Object delegate) {
      this.delegate = delegate;
   }

   public static SubmitNodeCollector wrap(SubmitNodeCollector collector) {
      if (collector == null || !SubmitNodeCollector.class.isInterface()) {
         return collector;
      }

      if (isWrapped(collector)) {
         return collector;
      }

      try {
         return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class[]{SubmitNodeCollector.class}, new ChamsCollector(collector));
      } catch (Throwable t) {
         return collector;
      }
   }

   private static boolean isWrapped(Object o) {
      return Proxy.isProxyClass(o.getClass()) && Proxy.getInvocationHandler(o) instanceof ChamsCollector;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object[] out = args;
      if (args != null && (active || hideArmour)) {
         for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RenderType rt) {
               boolean armour = isArmourType(rt);
               if (armour && hideArmour) {
                  return defaultValue(method.getReturnType());
               }

               if (active && (!armour || armourThroughWalls)) {
                  RenderType converted = ThroughWalls.convert(rt);
                  if (converted != rt) {
                     if (out == args) {
                        out = (Object[])args.clone();
                     }

                     out[i] = converted;
                  }
               }
            }
         }
      }

      Object result;
      try {
         result = method.invoke(this.delegate, out);
      } catch (InvocationTargetException e) {
         throw e.getCause() != null ? e.getCause() : e;
      }

      if (result != null && !isWrapped(result)) {
         Class<?> rt = method.getReturnType();
         if (rt.isInterface() && rt != Object.class && "order".equals(method.getName())) {
            try {
               return Proxy.newProxyInstance(rt.getClassLoader(), new Class[]{rt}, new ChamsCollector(result));
            } catch (Throwable var10) {
            }
         }
      }

      return result;
   }

   private static boolean isArmourType(RenderType type) {
      Boolean cached = ARMOUR_TYPES.get(type);
      if (cached != null) {
         return cached;
      }

      boolean result;
      try {
         String n = renderTypeName(type).toLowerCase(Locale.ROOT);
         result = n.contains("armor") || n.contains("armour");
      } catch (Throwable t) {
         result = false;
      }

      ARMOUR_TYPES.put(type, result);
      return result;
   }

   private static String renderTypeName(RenderType type) throws IllegalAccessException {
      for (Class<?> c = type.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == String.class && f.getName().equalsIgnoreCase("name")) {
               f.setAccessible(true);
               Object v = f.get(type);
               if (v != null) {
                  return (String)v;
               }
            }
         }
      }

      return String.valueOf(type);
   }

   private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive() || type == void.class) {
         return null;
      } else if (type == boolean.class) {
         return false;
      } else if (type == char.class) {
         return '\u0000';
      } else if (type == byte.class) {
         return (byte)0;
      } else if (type == short.class) {
         return (short)0;
      } else if (type == int.class) {
         return 0;
      } else if (type == long.class) {
         return 0L;
      } else {
         return type == float.class ? 0.0F : 0.0;
      }
   }
}
