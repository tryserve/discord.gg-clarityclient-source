package need.clarity.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class ContainerScreenHelper {
   private static final Field HOVERED_SLOT;
   private static final Method HANDLE_INV_CLICK;
   private static final Object CLICK_TYPE_SWAP;
   private static final Object CLICK_TYPE_QUICK_MOVE;

   private ContainerScreenHelper() {
   }

   public static Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
      if (HOVERED_SLOT != null && screen != null) {
         try {
            return (Slot)HOVERED_SLOT.get(screen);
         } catch (Throwable e) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void swapToOffhand(MultiPlayerGameMode gameMode, int containerId, int slotId, LocalPlayer player) {
      invokeClick(gameMode, containerId, slotId, 40, CLICK_TYPE_SWAP, player);
   }

   public static void swapToHotbar(MultiPlayerGameMode gameMode, int containerId, int slotId, int hotbarIndex, LocalPlayer player) {
      invokeClick(gameMode, containerId, slotId, hotbarIndex, CLICK_TYPE_SWAP, player);
   }

   public static void shiftClick(MultiPlayerGameMode gameMode, int containerId, int slotId, LocalPlayer player) {
      invokeClick(gameMode, containerId, slotId, 0, CLICK_TYPE_QUICK_MOVE, player);
   }

   private static void invokeClick(MultiPlayerGameMode gameMode, int containerId, int slotId, int button, Object clickType, LocalPlayer player) {
      if (HANDLE_INV_CLICK != null && clickType != null && gameMode != null) {
         try {
            HANDLE_INV_CLICK.invoke(gameMode, containerId, slotId, button, clickType, player);
         } catch (Throwable var7) {
         }
      }
   }

   static {
      Field hoveredSlot = null;

      label62:
      for (Class<?> cls = AbstractContainerScreen.class; cls != null; cls = cls.getSuperclass()) {
         try {
            Field f = cls.getDeclaredField("hoveredSlot");
            f.setAccessible(true);
            hoveredSlot = f;
            break;
         } catch (NoSuchFieldException var12) {
            for (Field f : cls.getDeclaredFields()) {
               if (f.getType() == Slot.class) {
                  f.setAccessible(true);
                  hoveredSlot = f;
                  break label62;
               }
            }
         }
      }

      HOVERED_SLOT = hoveredSlot;
      Method invClick = null;
      Object swap = null;
      Object quickMove = null;

      try {
         for (Method m : MultiPlayerGameMode.class.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 5 && p[0] == int.class && p[1] == int.class && p[2] == int.class && p[3].isEnum() && p[4] == Player.class && m.getReturnType() == void.class) {
               m.setAccessible(true);
               invClick = m;
               Object[] consts = p[3].getEnumConstants();
               if (consts != null && consts.length > 2) {
                  quickMove = consts[1];
                  swap = consts[2];
               }
               break;
            }
         }
      } catch (Throwable var11) {
      }

      HANDLE_INV_CLICK = invClick;
      CLICK_TYPE_SWAP = swap;
      CLICK_TYPE_QUICK_MOVE = quickMove;
   }
}
