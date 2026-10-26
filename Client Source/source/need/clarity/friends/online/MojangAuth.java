package need.clarity.friends.online;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.client.Minecraft;

final class MojangAuth {
   private MojangAuth() {
   }

   static String profileId(Minecraft mc) {
      try {
         Object user = mc.getUser();
         if (call(user, "getProfileId") instanceof UUID u) {
            return u.toString();
         }

         Object uuid = call(user, "getUuid");
         if (uuid != null) {
            return parse(String.valueOf(uuid));
         }
      } catch (Throwable var5) {
      }

      try {
         if (mc.player != null) {
            return mc.player.getUUID().toString();
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   static String profileName(Minecraft mc) {
      try {
         Object n = call(mc.getUser(), "getName");
         if (n != null) {
            return String.valueOf(n);
         }
      } catch (Throwable var2) {
      }

      return null;
   }

   static String join(Minecraft mc, String serverId) {
      if (serverId != null && !serverId.isEmpty()) {
         try {
            Object user = mc.getUser();
            Object token = call(user, "getAccessToken");
            String id = profileId(mc);
            if (token != null && id != null) {
               Object session = sessionService(mc);
               if (session == null) {
                  return "session service not found";
               }

               Method join = null;

               for (Method m : session.getClass().getMethods()) {
                  if (m.getName().equals("joinServer") && m.getParameterCount() == 3) {
                     join = m;
                     break;
                  }
               }

               if (join == null) {
                  return "joinServer not found";
               }

               Class<?>[] p = join.getParameterTypes();
               Object first = p[0] == UUID.class ? UUID.fromString(id) : id;
               join.invoke(session, first, String.valueOf(token), serverId);
               return null;
            } else {
               return "no session";
            }
         } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            String m = c == null ? null : c.getMessage();
            return "Mojang rejected the login" + (m == null ? "" : ": " + m);
         } catch (Throwable t) {
            return "auth failed: " + t.getClass().getSimpleName();
         }
      } else {
         return "bad challenge";
      }
   }

   private static Object sessionService(Minecraft mc) {
      try {
         Object s = mc.services().sessionService();
         if (s != null) {
            return s;
         }
      } catch (Throwable var12) {
      }

      Class<?> type;
      try {
         type = Class.forName("com.mojang.authlib.minecraft.MinecraftSessionService");
      } catch (ClassNotFoundException e) {
         return null;
      }

      Object direct = findByType(mc, type);
      if (direct != null) {
         return direct;
      }

      for (Method m : mc.getClass().getMethods()) {
         if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()) {
            String n = m.getReturnType().getName();
            if (n.contains("Services") || n.contains("services")) {
               try {
                  Object holder = m.invoke(mc);
                  Object s = findByType(holder, type);
                  if (s != null) {
                     return s;
                  }
               } catch (Throwable var10) {
               }
            }
         }
      }

      return null;
   }

   private static Object findByType(Object owner, Class<?> type) {
      if (owner == null) {
         return null;
      }

      for (Method m : owner.getClass().getMethods()) {
         if (m.getParameterCount() == 0 && type.isAssignableFrom(m.getReturnType())) {
            try {
               Object v = m.invoke(owner);
               if (v != null) {
                  return v;
               }
            } catch (Throwable var7) {
            }
         }
      }

      return null;
   }

   private static Object call(Object o, String name) {
      if (o == null) {
         return null;
      }

      try {
         return o.getClass().getMethod(name).invoke(o);
      } catch (Throwable t) {
         return null;
      }
   }

   private static String parse(String s) {
      String t = s.replace("-", "");
      return t.length() != 32 ? s : t.substring(0, 8) + "-" + t.substring(8, 12) + "-" + t.substring(12, 16) + "-" + t.substring(16, 20) + "-" + t.substring(20);
   }
}
