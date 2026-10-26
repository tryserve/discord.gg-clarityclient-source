package need.clarity.alts;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import sun.misc.Unsafe;

public final class SessionSwapper {
   private static User originalUser;
   private static UserApiService originalApi;
   private static boolean swapped;
   private static final Unsafe UNSAFE = loadUnsafe();

   private SessionSwapper() {
   }

   public static void restoreOriginal() throws Exception {
      if (swapped && originalUser != null) {
         Minecraft mc = Minecraft.getInstance();
         setFieldOfType(mc, User.class, originalUser);
         if (originalApi != null) {
            setFieldOfType(mc, UserApiService.class, originalApi);

            try {
               rebuildProfileKeys(mc, originalApi, originalUser);
            } catch (Throwable var2) {
            }
         }

         swapped = false;
      }
   }

   public static void apply(AltStore.Alt alt) throws Exception {
      Minecraft mc = Minecraft.getInstance();
      if (!swapped) {
         originalUser = getFieldOfType(mc, User.class);

         try {
            originalApi = getFieldOfType(mc, UserApiService.class);
         } catch (Throwable var7) {
         }

         swapped = true;
      }

      User user = newUser(alt.name, parseUuid(alt.uuid), alt.mcToken);
      if (!setFieldOfType(mc, User.class, user)) {
         throw new IllegalStateException("couldn't find the session field");
      }

      UserApiService api = null;

      try {
         YggdrasilAuthenticationService auth = getFieldOfType(mc, YggdrasilAuthenticationService.class);
         if (auth != null) {
            api = auth.createUserApiService(alt.mcToken);
            setFieldOfType(mc, UserApiService.class, api);
         }
      } catch (Throwable var6) {
      }

      if (api != null) {
         try {
            rebuildProfileKeys(mc, api, user);
         } catch (Throwable var5) {
         }
      }
   }

   private static User newUser(String name, UUID uuid, String token) throws Exception {
      Constructor<?>[] ctors = User.class.getDeclaredConstructors();
      Arrays.sort(ctors, Comparator.<Constructor<?>>comparingInt(cx -> cx.getParameterCount()).reversed());
      Exception last = null;

      for (Constructor<?> c : ctors) {
         try {
            Class<?>[] types = c.getParameterTypes();
            Object[] args = new Object[types.length];
            int strings = 0;
            boolean ok = true;

            for (int i = 0; i < types.length; i++) {
               Class<?> t = types[i];
               if (t == String.class) {
                  args[i] = strings++ == 0 ? name : token;
               } else if (t == UUID.class) {
                  args[i] = uuid;
               } else if (t == Optional.class) {
                  args[i] = Optional.empty();
               } else {
                  if (!t.isEnum()) {
                     ok = false;
                     break;
                  }

                  args[i] = msaConstant(t);
               }
            }

            if (ok && strings >= 2) {
               c.setAccessible(true);
               return (User)c.newInstance(args);
            }
         } catch (Exception e) {
            last = e;
         }
      }

      throw last != null ? last : new IllegalStateException("no usable User constructor");
   }

   private static Object msaConstant(Class<?> enumType) {
      Object[] cs = enumType.getEnumConstants();

      for (Object c : cs) {
         if (((Enum)c).name().equalsIgnoreCase("MSA")) {
            return c;
         }
      }

      for (Object c : cs) {
         for (Field f : enumType.getDeclaredFields()) {
            if (f.getType() == String.class && !Modifier.isStatic(f.getModifiers())) {
               try {
                  f.setAccessible(true);
                  if ("msa".equalsIgnoreCase((String)f.get(c))) {
                     return c;
                  }
               } catch (Throwable var11) {
               }
            }
         }
      }

      return cs[cs.length - 1];
   }

   private static void rebuildProfileKeys(Minecraft mc, UserApiService api, User user) throws Exception {
      Path gameDir = mc.gameDirectory.toPath();

      for (Class<?> k = mc.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
         for (Field f : k.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
               Class<?> t = f.getType();
               if (!t.isPrimitive() && !t.isArray() && !t.getName().startsWith("java.")) {
                  for (Method m : t.getDeclaredMethods()) {
                     if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == t) {
                        Class<?>[] p = m.getParameterTypes();
                        if (p.length == 3 && p[0] == UserApiService.class && p[1] == User.class && p[2] == Path.class) {
                           m.setAccessible(true);
                           Object fresh = m.invoke(null, api, user, gameDir);
                           setField(f, mc, fresh);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static <T> T getFieldOfType(Object owner, Class<T> type) throws Exception {
      for (Class<?> k = owner.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
         for (Field f : k.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && type.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               return (T)f.get(owner);
            }
         }
      }

      return null;
   }

   private static boolean setFieldOfType(Object owner, Class<?> type, Object value) throws Exception {
      for (Class<?> k = owner.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
         for (Field f : k.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == type) {
               setField(f, owner, value);
               return true;
            }
         }
      }

      return false;
   }

   private static void setField(Field f, Object owner, Object value) throws Exception {
      try {
         f.setAccessible(true);
         f.set(owner, value);
      } catch (IllegalAccessException e) {
         if (UNSAFE == null) {
            throw e;
         }

         long off = UNSAFE.objectFieldOffset(f);
         UNSAFE.putObjectVolatile(owner, off, value);
      }
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

   static UUID parseUuid(String s) {
      String h = s.replace("-", "");
      return h.length() != 32
         ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + s).getBytes())
         : UUID.fromString(h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16) + "-" + h.substring(16, 20) + "-" + h.substring(20));
   }
}
