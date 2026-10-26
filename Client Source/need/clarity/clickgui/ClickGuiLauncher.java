package need.clarity.clickgui;

import need.clarity.Unload;
import need.clarity.config.ClarityConfig;
import need.clarity.config.ConfigFiles;
import need.clarity.server.ClarityServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClickGuiLauncher {
   private ClickGuiLauncher() {
   }

   public static void onRightShift() {
      onRightShift(null);
   }

   public static void onRightShift(Screen menu) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.screen == menu && !Unload.isUnloaded()) {
         Object mode = ClarityConfig.getSetting("Click GUI", "GUI");
         if ("Web".equals(mode)) {
            ClarityServer.sync();
            ConfigFiles.openUrl("http://localhost:7420/");
         } else if (menu == null && Gui3DRenderer.active()) {
            Gui3DRenderer.toggle();
         } else if (menu == null && mc.level != null && mc.player != null && Boolean.TRUE.equals(ClarityConfig.getSetting("Click GUI", "Open In 3D")) && !Gui3DRenderer.hasFailed()) {
            Gui3DRenderer.toggle();
         } else {
            mc.setScreen(new ClickGuiScreen(menu));
         }
      }
   }
}
