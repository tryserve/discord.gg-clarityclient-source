package need.clarity.bridge;

import need.clarity.modules.visual.NoRender;
import need.clarity.nvg.ArrayListRenderer;
import need.clarity.render.CrosshairRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GuiBridge {
   private GuiBridge() {
   }

   public static boolean onExtractCrosshair(Object context) {
      try {
         return CrosshairRenderer.render((GuiGraphicsExtractor)context);
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelNausea() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelNausea();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelPortal() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelPortal();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelSpyglass() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelSpyglass();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelScoreboard() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelScoreboard();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelVignette() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelVignette();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onArrayListOverlay(Object context) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            return;
         }

         ArrayListRenderer.render((GuiGraphicsExtractor)context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.getWindow().getWidth(), mc.getWindow().getHeight());
      } catch (Throwable var2) {
      }
   }
}
