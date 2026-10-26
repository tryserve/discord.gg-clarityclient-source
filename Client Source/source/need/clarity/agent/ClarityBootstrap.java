package need.clarity.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

public final class ClarityBootstrap {
   private static final String[] OPTIMIZER_PACKET_IDS = new String[]{
      "crystaloptimizer:version", "crystaloptimizer:optout", "crystaloptimizer:optoutack", "crystaloptimizer:challenge", "crystaloptimizer:challengeresponse"
   };

   private ClarityBootstrap() {
   }

   public static void initialize(ClassLoader gameLoader) {
      try {
         tryPreClearPayloadRegistry(gameLoader);
      } catch (Throwable var14) {
      }

      try {
         Class<?> clarityClass = Class.forName("need.clarity.Clarity", true, gameLoader);

         Object clarityInstance;
         try {
            Constructor<?> ctor = clarityClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            clarityInstance = ctor.newInstance();
         } catch (Exception e) {
            return;
         }

         Method onInitialize = clarityClass.getDeclaredMethod("onInitialize");
         onInitialize.setAccessible(true);

         try {
            onInitialize.invoke(clarityInstance);
         } catch (InvocationTargetException var10) {
         } catch (Exception var11) {
         }

         try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft", true, gameLoader);
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Field screenField = mcClass.getField("screen");
            Object currentScreen = screenField.get(mc);
            if (currentScreen != null) {
               String screenName = currentScreen.getClass().getName();
               if (screenName.contains("TitleScreen")) {
                  mcClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen", true, gameLoader)).invoke(mc, currentScreen);
               }
            }
         } catch (Throwable var9) {
         }

         scheduleSoundReload(gameLoader);
      } catch (Throwable var13) {
      }
   }

   private static void scheduleSoundReload(ClassLoader gameLoader) {
      try {
         Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft", true, gameLoader);
         Object mc = mcClass.getMethod("getInstance").invoke(null);
         if (mc == null) {
            return;
         }

         mcClass.getMethod("execute", Runnable.class).invoke(mc, (Runnable)() -> {
            try {
               reloadSounds(mcClass, mc, gameLoader);
            } catch (Throwable tx) {
               System.err.println("[Clarity] sound reload failed: " + tx);
            }
         });
      } catch (Throwable t) {
         System.err.println("[Clarity] could not schedule sound reload: " + t);
      }
   }

   private static void reloadSounds(Class<?> mcClass, Object mc, ClassLoader gameLoader) throws Exception {
      Object rm = mcClass.getMethod("getResourceManager").invoke(mc);
      if (rm != null) {
         if (!Proxy.isProxyClass(rm.getClass())) {
            try {
               Class<?> misc = Class.forName("need.clarity.bridge.MiscBridge", true, gameLoader);
               rm = misc.getMethod("wrapResourceManager", Object.class).invoke(null, rm);
            } catch (Throwable var5) {
            }
         }

         System.out.println("[Clarity] reload: resource manager wrapped=" + Proxy.isProxyClass(rm.getClass()) + " jar=" + System.getProperty("clarity.resources.jar"));
         reloadListener(mcClass, mc, "getSoundManager", rm, gameLoader);
         reloadListener(mcClass, mc, "getShaderManager", rm, gameLoader);
      }
   }

   private static void reloadListener(Class<?> mcClass, Object mc, String getter, Object rm, ClassLoader gameLoader) {
      try {
         Object listener = mcClass.getMethod(getter).invoke(mc);
         if (listener == null) {
            System.out.println("[Clarity] reload: " + getter + " returned null");
            return;
         }

         Method prepare = null;
         Method apply = null;

         for (Class<?> c = listener.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
               if (!m.isSynthetic() && !m.isBridge()) {
                  if (prepare == null && m.getName().equals("prepare") && m.getParameterCount() == 2) {
                     prepare = m;
                  }

                  if (apply == null && m.getName().equals("apply") && m.getParameterCount() == 3) {
                     apply = m;
                  }
               }
            }
         }

         if (prepare == null || apply == null) {
            System.out.println("[Clarity] reload: " + listener.getClass().getName() + " has no prepare/apply (prepare=" + prepare + ", apply=" + apply + ")");
            return;
         }

         prepare.setAccessible(true);
         apply.setAccessible(true);
         Object profiler = inactiveProfiler(prepare.getParameterTypes()[1], gameLoader);
         Object prepared = prepare.invoke(listener, rm, profiler);
         apply.invoke(listener, prepared, rm, profiler);
         System.out.println("[Clarity] reload: " + listener.getClass().getSimpleName() + " reloaded");
      } catch (Throwable t) {
         Throwable c = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
         System.out.println("[Clarity] reload: " + getter + " failed: " + c);
         c.printStackTrace(System.out);
      }
   }

   private static Object inactiveProfiler(Class<?> paramType, ClassLoader gameLoader) {
      try {
         Class<?> ip = Class.forName("net.minecraft.util.profiling.InactiveProfiler", true, gameLoader);
         Object inst = ip.getField("INSTANCE").get(null);
         if (paramType.isInstance(inst)) {
            return inst;
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   private static void tryPreClearPayloadRegistry(ClassLoader gameLoader) throws Exception {
      Class<?> regClass;
      try {
         regClass = Class.forName("net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry", true, gameLoader);
      } catch (ClassNotFoundException e) {
         return;
      }

      Method playC2S = regClass.getDeclaredMethod("playC2S");
      Method playS2C = regClass.getDeclaredMethod("playS2C");
      playC2S.setAccessible(true);
      playS2C.setAccessible(true);
      Object c2sReg = playC2S.invoke(null);
      Object s2cReg = playS2C.invoke(null);
      Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation", true, gameLoader);
      Method rlFactory = findResourceLocationFactory(rlClass);

      for (Object registry : new Object[]{c2sReg, s2cReg}) {
         removeKeys(registry, rlClass, rlFactory);
      }
   }

   private static Method findResourceLocationFactory(Class<?> rlClass) {
      for (String name : new String[]{"parse", "of", "tryParse", "fromNamespaceAndPath"}) {
         try {
            Method m;
            if ("fromNamespaceAndPath".equals(name)) {
               m = rlClass.getDeclaredMethod(name, String.class, String.class);
            } else {
               m = rlClass.getDeclaredMethod(name, String.class);
            }

            m.setAccessible(true);
            return m;
         } catch (NoSuchMethodException var6) {
         }
      }

      throw new IllegalStateException("Cannot find ResourceLocation factory method");
   }

   private static void removeKeys(Object registry, Class<?> rlClass, Method rlFactory) {
      for (Class<?> cls = registry.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
         for (Field field : cls.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType()) || Set.class.isAssignableFrom(field.getType())) {
               field.setAccessible(true);

               try {
                  Object collection = field.get(registry);
                  if (collection != null) {
                     for (String id : OPTIMIZER_PACKET_IDS) {
                        try {
                           Object rl = buildResourceLocation(rlFactory, id);
                           if (collection instanceof Map map) {
                              map.remove(rl);
                           } else if (collection instanceof Set set) {
                              set.remove(rl);
                           }
                        } catch (Exception var16) {
                        }
                     }
                  }
               } catch (Exception var17) {
               }
            }
         }
      }
   }

   private static Object buildResourceLocation(Method factory, String id) throws Exception {
      if (factory.getParameterCount() == 2) {
         String[] parts = id.split(":", 2);
         return factory.invoke(null, parts[0], parts.length > 1 ? parts[1] : id);
      } else {
         return factory.invoke(null, id);
      }
   }
}
