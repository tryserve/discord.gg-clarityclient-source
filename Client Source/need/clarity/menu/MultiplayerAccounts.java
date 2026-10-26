package need.clarity.menu;

import need.clarity.Unload;
import need.clarity.modules.visual.MainMenu;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button.Plain;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

public final class MultiplayerAccounts {
   private static final int BTN_W = 150;
   private static final int BTN_H = 20;
   private static final int MARGIN = 6;

   private MultiplayerAccounts() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (!Unload.isUnloaded()) {
            if (screen instanceof JoinMultiplayerScreen) {
               if (MainMenu.active() && !MenuRenderer.hasFailed()) {
                  String name = client.getUser() != null ? client.getUser().getName() : "?";
                  Screens.getWidgets(screen).add(new MultiplayerAccounts.AccountsButton(screen, scaledWidth - 150 - 6, 6, name));
               }
            }
         }
      });
   }

   private static final class AccountsButton extends Plain {
      AccountsButton(Screen parent, int x, int y, String name) {
         super(x, y, 150, 20, Component.literal("Account: " + name), b -> Minecraft.getInstance().setScreen(new AltManagerScreen(parent)), DEFAULT_NARRATION);
      }
   }
}
