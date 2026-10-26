package need.clarity.bridge;

import need.clarity.clickgui.Gui3DRenderer;
import need.clarity.keybind.KeybindHandler;
import need.clarity.modules.utility.MiddleClickFriend;
import need.clarity.modules.visual.Freecam;
import need.clarity.modules.visual.Freelook;
import need.clarity.modules.visual.Zoom;
import need.clarity.nvg.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class MouseHandlerBridge {
   private MouseHandlerBridge() {
   }

   public static boolean onMouseButton(long windowPtr, MouseButtonInfo buttonInfo, int action) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return false;
         }

         int button = buttonInfo.button();
         if (HudLayout.isEditing()) {
            if (mc.screen == null) {
               HudLayout.onMouseButton(button, action);
               return true;
            }

            HudLayout.exitNoGrab();
         }

         if (mc.screen != null) {
            return false;
         }

         if (Gui3DRenderer.onMouseButton(button, action)) {
            return true;
         }

         if (action == 1 && button == 2 && MiddleClickFriend.onMiddleClick()) {
            KeybindHandler.onMouseEvent(button, true);
            return true;
         }

         if (action == 1) {
            KeybindHandler.onMouseEvent(button, true);
         } else if (action == 0) {
            KeybindHandler.onMouseEvent(button, false);
         }
      } catch (Throwable var6) {
      }

      return false;
   }

   public static boolean onScroll(long windowPtr, double xOffset, double yOffset) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            return false;
         }

         if (HudLayout.isEditing() && mc.screen == null) {
            HudLayout.onScroll(yOffset);
            return true;
         }

         if (mc.screen != null) {
            return false;
         }

         if (Gui3DRenderer.onScroll(yOffset)) {
            return true;
         }

         if (Zoom.consumesScroll() && yOffset != 0.0) {
            Zoom.scroll(yOffset);
            return true;
         }
      } catch (Throwable var7) {
      }

      return false;
   }

   public static void turnPlayer(LocalPlayer player, double deltaYaw, double deltaPitch) {
      try {
         if (Freelook.isActive() && !Freecam.isActive()) {
            Freelook.turn(deltaYaw, deltaPitch);
            return;
         }

         if (!Freecam.isActive()) {
            double k = Zoom.sensitivity();
            player.turn(deltaYaw * k, deltaPitch * k);
            return;
         }

         Freecam.cameraYaw = Mth.wrapDegrees(Freecam.cameraYaw + (float)(deltaYaw * 0.15));
         Freecam.cameraPitch = Mth.clamp(Freecam.cameraPitch + (float)(deltaPitch * 0.15), -90.0F, 90.0F);
      } catch (Throwable t) {
         try {
            player.turn(deltaYaw, deltaPitch);
         } catch (Throwable var7) {
         }
      }
   }
}
