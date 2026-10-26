package need.clarity.util;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.mixin.ChatComponentAccessor;
import need.clarity.mixin.KeyMappingAccessor;
import need.clarity.mixin.LocalPlayerSprintAccessor;
import need.clarity.mixin.MultiPlayerGameModeAccessor;
import need.clarity.mixin.PoseStackAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.player.LocalPlayer;

public final class AccessorCompat {
   private static final ConcurrentHashMap<String, Field> FIELDS = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Method> METHODS = new ConcurrentHashMap<>();
   private static final Field MISSING_F = missingField();
   private static final Method MISSING_M = missingMethod();

   private AccessorCompat() {
   }

   private static Field missingField() {
      try {
         return AccessorCompat.class.getDeclaredField("FIELDS");
      } catch (Throwable t) {
         return null;
      }
   }

   private static Method missingMethod() {
      try {
         return Object.class.getDeclaredMethod("hashCode");
      } catch (Throwable t) {
         return null;
      }
   }

   private static Field field(Class<?> owner, String name) {
      Field f = FIELDS.computeIfAbsent(owner.getName() + "#" + name, k -> {
         for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
               Field found = c.getDeclaredField(name);
               found.setAccessible(true);
               return found;
            } catch (Throwable ignored) {
            }
         }

         return MISSING_F;
      });
      return f == MISSING_F ? null : f;
   }

   private static Method method(Class<?> owner, String name) {
      Method m = METHODS.computeIfAbsent(owner.getName() + "#" + name, k -> {
         for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
               for (Method cand : c.getDeclaredMethods()) {
                  if (cand.getName().equals(name) && cand.getParameterCount() == 0) {
                     cand.setAccessible(true);
                     return cand;
                  }
               }
            } catch (Throwable var8) {
            }
         }

         return MISSING_M;
      });
      return m == MISSING_M ? null : m;
   }

   private static int getInt(Object o, String name, int def) {
      try {
         Field f = field(o.getClass(), name);
         return f == null ? def : f.getInt(o);
      } catch (Throwable t) {
         return def;
      }
   }

   private static void setInt(Object o, String name, int v) {
      try {
         Field f = field(o.getClass(), name);
         if (f != null) {
            f.setInt(o, v);
         }
      } catch (Throwable var4) {
      }
   }

   public static int getClickCount(KeyMapping k) {
      return k instanceof KeyMappingAccessor a ? a.getClickCount() : getInt(k, "clickCount", 0);
   }

   public static void setClickCount(KeyMapping k, int v) {
      if (k instanceof KeyMappingAccessor a) {
         a.setClickCount(v);
      } else {
         setInt(k, "clickCount", v);
      }
   }

   public static void addClick(KeyMapping k) {
      setClickCount(k, getClickCount(k) + 1);
   }

   public static Key getBoundKey(KeyMapping k) {
      if (k instanceof KeyMappingAccessor a) {
         return a.getBoundKey();
      } else {
         try {
            Field f = field(k.getClass(), "key");
            return f == null ? null : (Key)f.get(k);
         } catch (Throwable t) {
            return null;
         }
      }
   }

   public static void setDestroyDelay(MultiPlayerGameMode gm, int v) {
      if (gm instanceof MultiPlayerGameModeAccessor a) {
         a.setDestroyDelay(v);
      } else {
         setInt(gm, "destroyDelay", v);
      }
   }

   public static void ensureHasSentCarriedItem(MultiPlayerGameMode gm) {
      if (gm instanceof MultiPlayerGameModeAccessor a) {
         a.clarity$ensureHasSentCarriedItem();
      } else {
         try {
            Method m = method(gm.getClass(), "ensureHasSentCarriedItem");
            if (m != null) {
               m.invoke(gm);
            }
         } catch (Throwable var3) {
         }
      }
   }

   public static void setWasSprinting(LocalPlayer p, boolean v) {
      if (p instanceof LocalPlayerSprintAccessor a) {
         a.setWasSprinting(v);
      } else {
         try {
            Field f = field(p.getClass(), "wasSprinting");
            if (f != null) {
               f.setBoolean(p, v);
            }
         } catch (Throwable var4) {
         }
      }
   }

   public static List<Pose> poses(PoseStack ps) {
      if (ps instanceof PoseStackAccessor a) {
         return a.clarity$poses();
      } else {
         try {
            Field f = field(PoseStack.class, "poses");
            return f == null ? List.of() : (List)f.get(ps);
         } catch (Throwable t) {
            return List.of();
         }
      }
   }

   public static int lastIndex(PoseStack ps) {
      return ps instanceof PoseStackAccessor a ? a.clarity$lastIndex() : getInt(ps, "lastIndex", 0);
   }

   public static List<GuiMessage> chatMessages(ChatComponent c) {
      if (c instanceof ChatComponentAccessor a) {
         return a.clarity$allMessages();
      } else {
         try {
            Field f = field(ChatComponent.class, "allMessages");
            return f == null ? List.of() : (List)f.get(c);
         } catch (Throwable t) {
            return List.of();
         }
      }
   }

   public static int chatScroll(ChatComponent c) {
      return c instanceof ChatComponentAccessor a ? a.clarity$scroll() : getInt(c, "chatScrollbarPos", 0);
   }

   public static Object creativeSelectedTab() {
      try {
         Class<?> c = Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen");
         Field f = field(c, "selectedTab");
         return f == null ? null : f.get(null);
      } catch (Throwable t) {
         return null;
      }
   }
}
