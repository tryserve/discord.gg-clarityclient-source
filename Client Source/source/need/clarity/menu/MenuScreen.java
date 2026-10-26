package need.clarity.menu;

import need.clarity.clickgui.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class MenuScreen extends Screen {
   protected float mx = -1.0F;
   protected float my = -1.0F;
   protected float w = 1920.0F;
   protected float h = 1080.0F;
   protected final long openedAt = System.nanoTime();
   private long lastFrame = 0L;
   protected float dt = 0.0F;
   private final MenuRenderer.Ui ui = (c, ww, hh) -> {
      this.w = ww;
      this.h = hh;
      this.draw(c, ww, hh);
   };

   protected MenuScreen(String title) {
      super(Component.literal(title));
   }

   protected abstract void draw(Canvas var1, float var2, float var3);

   protected abstract void click(float var1, float var2);

   public float mouseDX() {
      return this.mx;
   }

   public float mouseDY() {
      return this.my;
   }

   protected float age() {
      return (float)(System.nanoTime() - this.openedAt) / 1.0E9F;
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
      long now = System.nanoTime();
      this.dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      MenuRenderer.render(ctx, this.mx, this.my, this.ui);
      if (MenuRenderer.hasFailed()) {
         Minecraft mc = Minecraft.getInstance();
         mc.execute(() -> {
            if (mc.screen == this) {
               mc.setScreen(new TitleScreen());
            }
         });
      }
   }

   public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick) {
   }

   public void mouseMoved(double x, double y) {
      this.mx = MenuRenderer.toDesign(x);
      this.my = MenuRenderer.toDesign(y);
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      this.mx = MenuRenderer.toDesign(e.x());
      this.my = MenuRenderer.toDesign(e.y());
      if (e.button() == 0) {
         this.click(this.mx, this.my);
      }

      return true;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected boolean over(float x, float y, float bw, float bh) {
      return this.mx >= x && this.mx <= x + bw && this.my >= y && this.my <= y + bh;
   }

   protected float approach(float cur, float target, float speed) {
      return cur + (target - cur) * (1.0F - (float)Math.exp(-this.dt * speed));
   }

   protected static float easeOut(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - (float)Math.pow(1.0F - t, 3.0);
   }

   protected static int white(float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | 16777215;
   }

   protected static int black(float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24;
   }
}
