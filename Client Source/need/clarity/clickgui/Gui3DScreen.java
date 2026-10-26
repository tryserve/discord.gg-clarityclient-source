package need.clarity.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class Gui3DScreen extends Screen {
   public Gui3DScreen() {
      super(Component.literal("Clarity"));
      Gui3DRenderer.cursorMode = true;
      Minecraft mc = Minecraft.getInstance();
      Gui3DRenderer.cursor(mc.getWindow().getWidth() / 2.0, mc.getWindow().getHeight() / 2.0);
   }

   private static ClickGui gui() {
      return Gui3DRenderer.gui();
   }

   private static double px(double guiUnits) {
      return guiUnits * Minecraft.getInstance().getWindow().getGuiScale();
   }

   private static float[] at(double x, double y) {
      Gui3DRenderer.cursor(px(x), px(y));
      return Gui3DRenderer.toGui(true);
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (gui() == null || !Gui3DRenderer.wantsKeyboard()) {
         mc.execute(() -> {
            if (mc.screen == this) {
               mc.setScreen(null);
            }
         });
      }
   }

   public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
   }

   public void removed() {
      Gui3DRenderer.cursorMode = false;
   }

   public void mouseMoved(double x, double y) {
      ClickGui g = gui();
      if (g != null) {
         float[] p = at(x, y);
         g.mouseMove(p[0], p[1]);
      }
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      ClickGui g = gui();
      if (g == null) {
         return true;
      }

      float[] p = at(e.x(), e.y());
      g.mouseDown(p[0], p[1], e.button());
      return true;
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      ClickGui g = gui();
      if (g == null) {
         return true;
      }

      float[] p = at(e.x(), e.y());
      g.mouseUp(p[0], p[1], e.button());
      return true;
   }

   public boolean mouseDragged(MouseButtonEvent e, double ddx, double ddy) {
      ClickGui g = gui();
      if (g == null) {
         return true;
      }

      float[] p = at(e.x(), e.y());
      g.mouseMove(p[0], p[1]);
      return true;
   }

   public boolean mouseScrolled(double x, double y, double h, double v) {
      ClickGui g = gui();
      if (g == null) {
         return true;
      }

      float[] p = at(x, y);
      g.scroll(p[0], p[1], (float)v);
      return true;
   }

   public boolean keyPressed(KeyEvent e) {
      ClickGui g = gui();
      if (g != null) {
         g.key(e.key(), e.modifiers());
      }

      return true;
   }

   public boolean charTyped(CharacterEvent e) {
      ClickGui g = gui();
      if (g != null) {
         g.chars(e.codepointAsString());
      }

      return true;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }
}
