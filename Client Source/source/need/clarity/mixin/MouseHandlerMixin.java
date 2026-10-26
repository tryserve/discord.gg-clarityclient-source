package need.clarity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import need.clarity.Unload;
import need.clarity.clickgui.Gui3DRenderer;
import need.clarity.keybind.KeybindHandler;
import need.clarity.modules.utility.MiddleClickFriend;
import need.clarity.modules.visual.Freecam;
import need.clarity.modules.visual.Freelook;
import need.clarity.modules.visual.Zoom;
import need.clarity.nvg.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
   @WrapOperation(method = "turnPlayer(D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
   private void clarity$freecamRedirectTurn(LocalPlayer player, double deltaYaw, double deltaPitch, Operation<Void> original) {
      if (Freelook.isActive() && !Freecam.isActive()) {
         Freelook.turn(deltaYaw, deltaPitch);
      } else if (!Freecam.isActive()) {
         double k = Zoom.sensitivity();
         original.call(new Object[]{player, deltaYaw * k, deltaPitch * k});
      } else {
         Freecam.cameraYaw = Mth.wrapDegrees(Freecam.cameraYaw + (float)(deltaYaw * 0.15));
         Freecam.cameraPitch = Mth.clamp(Freecam.cameraPitch + (float)(deltaPitch * 0.15), -90.0F, 90.0F);
      }
   }

   @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
   private void clarity$zoomScroll(long windowPtr, double xOffset, double yOffset, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && !Unload.isUnloaded()) {
         if (HudLayout.isEditing() && mc.screen == null) {
            HudLayout.onScroll(yOffset);
            ci.cancel();
         } else if (mc.screen == null) {
            if (Gui3DRenderer.onScroll(yOffset)) {
               ci.cancel();
            } else {
               if (Zoom.consumesScroll() && yOffset != 0.0) {
                  Zoom.scroll(yOffset);
                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
   private void onMouseButton(long windowPtr, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !Unload.isUnloaded()) {
         int button = buttonInfo.button();
         if (HudLayout.isEditing()) {
            if (mc.screen == null) {
               HudLayout.onMouseButton(button, action);
               ci.cancel();
               return;
            }

            HudLayout.exitNoGrab();
         }

         if (mc.screen == null) {
            if (Gui3DRenderer.onMouseButton(button, action)) {
               ci.cancel();
            } else if (action == 1 && button == 2 && MiddleClickFriend.onMiddleClick()) {
               KeybindHandler.onMouseEvent(button, true);
               ci.cancel();
            } else {
               if (action == 1) {
                  KeybindHandler.onMouseEvent(button, true);
               } else if (action == 0) {
                  KeybindHandler.onMouseEvent(button, false);
               }
            }
         }
      }
   }
}
