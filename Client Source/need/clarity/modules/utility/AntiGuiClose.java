package need.clarity.modules.utility;

import need.clarity.menu.MainMenuScreen;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;

public class AntiGuiClose extends ModuleBase {
   public static AntiGuiClose INSTANCE;
   private static Screen transferred;
   private static long transferredAt;
   private static Screen deferred;
   public static boolean serverOpening;
   private static boolean restoring;

   public AntiGuiClose() {
      super("Anti GUI Close", "Utility");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      transferred = null;
      deferred = null;
   }

   private static boolean on() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   private boolean opt(String key, boolean def) {
      return this.getSetting(key, def);
   }

   private static boolean isTransient(Screen s) {
      return s instanceof LevelLoadingScreen
         || s instanceof ServerReconfigScreen
         || s instanceof ProgressScreen
         || s instanceof ConnectScreen
         || s instanceof DisconnectedScreen
         || s instanceof GenericMessageScreen;
   }

   private static boolean isOwnMenu(Screen s) {
      return s != null && !(s instanceof AbstractContainerScreen) && !isTransient(s) && !(s instanceof DeathScreen) && !(s instanceof TitleScreen) && !(s instanceof MainMenuScreen);
   }

   public static boolean onServerClose() {
      if (!on()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return false;
         } else {
            Screen s = mc.screen;
            if (deferred != null) {
               deferred = null;
               mc.player.containerMenu = mc.player.inventoryMenu;
               return true;
            } else if (INSTANCE.opt("Menus", true) && isOwnMenu(s)) {
               mc.player.containerMenu = mc.player.inventoryMenu;
               return true;
            } else {
               return INSTANCE.opt("Inventory", false) && s instanceof InventoryScreen;
            }
         }
      }
   }

   public static Screen redirect(Screen current, Screen incoming) {
      if (!restoring && on()) {
         Minecraft mc = Minecraft.getInstance();
         if (serverOpening && incoming instanceof AbstractContainerScreen && INSTANCE.opt("Server Menus", true) && isOwnMenu(current)) {
            deferred = incoming;
            return current;
         }

         if ((incoming instanceof LevelLoadingScreen || incoming instanceof ServerReconfigScreen) && INSTANCE.opt("Server Transfer", true) && isOwnMenu(current) && !(current instanceof PauseScreen)) {
            transferred = current;
            transferredAt = System.currentTimeMillis();
            deferred = null;
            return incoming;
         }

         if (incoming == null && isTransient(current) && transferred != null && mc.level != null) {
            Screen back = transferred;
            transferred = null;
            if (System.currentTimeMillis() - transferredAt < 120000L) {
               return back;
            }
         }

         if (incoming == null && deferred != null && current != null && !isTransient(current)) {
            Screen d = deferred;
            deferred = null;
            if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
               return d;
            }
         }

         if (incoming instanceof TitleScreen || incoming instanceof DisconnectedScreen || incoming instanceof MainMenuScreen) {
            transferred = null;
            deferred = null;
         }

         return incoming;
      } else {
         return incoming;
      }
   }

   public static void beginRestore() {
      restoring = true;
   }

   public static void endRestore() {
      restoring = false;
   }
}
