package need.clarity.clickgui;

import need.clarity.menu.MenuRenderer;
import need.clarity.menu.MenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
   private final ClickGui gui;
   private final long openedAt = System.nanoTime();
   private boolean shiftArmed = false;
   private final Screen parent;
   private float mx = -1.0F;
   private float my = -1.0F;

   public ClickGuiScreen() {
      this(null);
   }

   public ClickGuiScreen(Screen parent) {
      super(Component.literal("Clarity"));
      this.parent = parent instanceof MenuScreen ? parent : null;
      if (this.parent instanceof MenuScreen m) {
         this.mx = m.mouseDX();
         this.my = m.mouseDY();
      }

      GuiData data = GuiData.get();
      ClickGuiRenderer.resetFailure();
      this.gui = new ClickGui(data, new ConfigStore(data));
      ClickGuiRenderer.resetScene();
   }

   public ClickGui gui() {
      return this.gui;
   }

   private float dx(double guiX) {
      Minecraft mc = Minecraft.getInstance();
      return (float)(guiX * mc.getWindow().getGuiScale() / ClickGuiRenderer.designScale(this.gui));
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
      ClickGuiRenderer.overMenu = this.parent != null;
      if (this.parent != null) {
         MenuRenderer.render(ctx, this.mx, this.my, (c, ww, hh) -> {});
      }

      ClickGuiRenderer.render(ctx, this.gui);
      if (ClickGuiRenderer.hasFailed()) {
         Minecraft mc = Minecraft.getInstance();
         mc.execute(
            () -> {
               if (mc.screen == this) {
                  mc.setScreen(this.parent);
               }

               try {
                  new ConfigStore(GuiData.get()).switchToWebGui();
               } catch (Throwable var3x) {
               }

               if (mc.player != null) {
                  mc.player
                     .sendSystemMessage(
                        Component.literal(
                           "§c[Clarity] The ClickGUI failed to draw, so GUI was switched to Web and opened in your browser. The error is in logs/latest.log (search \"ClickGUI frame failed\")."
                        )
                     );
               }
            }
         );
      } else {
         if (this.gui.isClosed()) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
               if (mc.screen == this) {
                  mc.setScreen(this.parent);
               }
            });
         }
      }
   }

   public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
   }

   public void mouseMoved(double x, double y) {
      this.gui.mouseMove(this.dx(x), this.dx(y));
      this.mx = MenuRenderer.toDesign(x);
      this.my = MenuRenderer.toDesign(y);
   }

   public void removed() {
      ClickGuiRenderer.overMenu = false;
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      this.gui.mouseDown(this.dx(e.x()), this.dx(e.y()), e.button());
      return true;
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      this.gui.mouseUp(this.dx(e.x()), this.dx(e.y()), e.button());
      return true;
   }

   public boolean mouseDragged(MouseButtonEvent e, double ddx, double ddy) {
      this.gui.mouseMove(this.dx(e.x()), this.dx(e.y()));
      return true;
   }

   public boolean mouseScrolled(double x, double y, double h, double v) {
      this.gui.scroll(this.dx(x), this.dx(y), (float)v);
      return true;
   }

   public boolean keyPressed(KeyEvent e) {
      if (e.key() != 344 || this.shiftArmed && System.nanoTime() - this.openedAt >= 250000000L) {
         this.gui.key(e.key(), e.modifiers());
         return true;
      } else {
         return true;
      }
   }

   public boolean keyReleased(KeyEvent e) {
      if (e.key() == 344) {
         this.shiftArmed = true;
      }

      return true;
   }

   public boolean charTyped(CharacterEvent e) {
      this.gui.chars(e.codepointAsString());
      return true;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.gui.close();
   }
}
