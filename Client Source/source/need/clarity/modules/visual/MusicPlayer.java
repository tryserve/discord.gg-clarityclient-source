package need.clarity.modules.visual;

import need.clarity.keybind.KeybindHandler;
import need.clarity.media.MediaBridge;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MusicPlayer extends ModuleBase {
   public static MusicPlayer INSTANCE;
   private boolean nextWas;
   private boolean prevWas;
   private boolean pauseWas;

   public MusicPlayer() {
      super("Music Player", "HUD");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      MediaBridge.ensureStarted();
   }

   @Override
   public void onDisable() {
      MediaBridge.stop();
      this.nextWas = this.prevWas = this.pauseWas = false;
   }

   @Override
   public void onTick() {
      MediaBridge.ensureStarted();
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.screen == null) {
         boolean n = isBindDown(mc, this.nextKey());
         boolean p = isBindDown(mc, this.previousKey());
         boolean t = isBindDown(mc, this.pauseKey());
         if (n && !this.nextWas) {
            MediaBridge.next();
         }

         if (p && !this.prevWas) {
            MediaBridge.previous();
         }

         if (t && !this.pauseWas) {
            MediaBridge.playPause();
         }

         this.nextWas = n;
         this.prevWas = p;
         this.pauseWas = t;
      } else {
         this.nextWas = this.prevWas = this.pauseWas = true;
      }
   }

   private static boolean isBindDown(Minecraft mc, String name) {
      if (name != null && !name.isEmpty() && !"NONE".equalsIgnoreCase(name)) {
         long window = mc.getWindow().handle();
         int mouse = KeybindHandler.mouseButtonIndex(name);
         if (mouse >= 0) {
            return GLFW.glfwGetMouseButton(window, mouse) == 1;
         }

         int key = KeybindHandler.glfwKeyCode(name);
         return key == -1 ? false : GLFW.glfwGetKey(window, key) == 1;
      } else {
         return false;
      }
   }

   public String nextKey() {
      return this.getSetting("Next Key", "NONE");
   }

   public String previousKey() {
      return this.getSetting("Previous Key", "NONE");
   }

   public String pauseKey() {
      return this.getSetting("Play/Pause Key", "NONE");
   }

   public String anchor() {
      return this.getSetting("Anchor", "Top Left");
   }

   public int offsetX() {
      return this.getSetting("X Offset", 12);
   }

   public int offsetY() {
      return this.getSetting("Y Offset", 12);
   }

   public float scale() {
      return Math.max(0.5F, Math.min(2.0F, this.getSetting("Scale", 1.0F)));
   }

   public float opacity() {
      return Math.max(0.1F, Math.min(1.0F, this.getSetting("Opacity", 1.0F)));
   }

   public String layout() {
      return this.getSetting("Layout", "Card");
   }

   public float width() {
      return Math.max(160.0F, Math.min(420.0F, this.getSetting("Width", 260.0F)));
   }

   public float cornerRadius() {
      return Math.max(0.0F, this.getSetting("Corner Radius", 10.0F));
   }

   public boolean showArt() {
      return this.getSetting("Show Art", true);
   }

   public boolean showArtist() {
      return this.getSetting("Show Artist", true);
   }

   public boolean showProgress() {
      return this.getSetting("Show Progress Bar", true);
   }

   public boolean showTimes() {
      return this.getSetting("Show Times", true);
   }

   public String timeMode() {
      return this.getSetting("Time Mode", "Elapsed / Remaining");
   }

   public boolean scrollTitle() {
      return this.getSetting("Scroll Long Titles", true);
   }

   public boolean showApp() {
      return this.getSetting("Show Source", false);
   }

   public boolean hideWhenIdle() {
      return this.getSetting("Hide When Idle", true);
   }

   public boolean hideWhenPaused() {
      return this.getSetting("Hide When Paused", false);
   }

   public boolean dimWhenPaused() {
      return this.getSetting("Dim When Paused", true);
   }

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public float bgOpacity() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Background Opacity", 0.92F)));
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean highlight() {
      return this.getSetting("Highlight", true);
   }

   public boolean blur() {
      return this.getSetting("Blur", false);
   }

   public int blurStrength() {
      return Math.max(1, Math.min(4, this.getSetting("Blur Strength", 2)));
   }

   public float blurTint() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Blur Tint", 0.45F)));
   }

   public boolean useTheme() {
      return this.getSetting("Theme Colors", true);
   }

   public int accentColor() {
      return ThemeColors.resolveRgb(this.getSetting("Accent Color", ""), -14829228, false) & 16777215;
   }

   public int textColor() {
      return ThemeColors.resolveRgb(this.getSetting("Text Color", ""), -1184272, false) & 16777215;
   }

   public int bgColor() {
      return ThemeColors.resolveRgb(this.getSetting("Background Color", ""), -15328474, false) & 16777215;
   }
}
