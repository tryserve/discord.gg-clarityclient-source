package need.clarity.nvg;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.visual.HUD;
import need.clarity.util.ThemeColors;
import need.clarity.util.UiSounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class HudLayout {
   public static final String NOTIFICATIONS = "Notifications";
   public static final String ARRAYLIST = "ArrayList";
   public static final String KEYBINDS = "Keybind List";
   public static final String MUSIC = "Music Player";
   public static final String TARGET = "Target Hud";
   public static final String INFO = "HUD";
   public static final String POTIONS = "Potion HUD";
   private static final float CENTER_BAND = 10.0F;
   private static final float GRID = 10.0F;
   private static final int GRID_MAJOR = 5;
   private static final Map<String, HudLayout.Rect> RECTS = new LinkedHashMap<>();
   private static long frame = 0L;
   private static float canvasW = 1.0F;
   private static float canvasH = 1.0F;
   private static boolean editing;
   private static String dragging;
   private static String hovered;
   private static float grabX;
   private static float grabY;
   private static boolean snappedCenter;
   private static boolean gridOn;
   private static String lastApplied = "";
   private static volatile String pendingOpen;
   private static final int NVG_ALIGN_LEFT_MIDDLE = 17;
   private static final int NVG_ALIGN_CENTER_MIDDLE = 18;

   private HudLayout() {
   }

   public static boolean isEditing() {
      return editing;
   }

   public static boolean draggingElement() {
      return editing && dragging != null;
   }

   public static void onScroll(double amount) {
      if (editing) {
         float[] c = cursor();
         if (c != null) {
            HudEditorPanel.onScroll(c[0], c[1], amount);
         }
      }
   }

   public static void openAfterGui(String module) {
      pendingOpen = module == null ? "" : module;
   }

   public static void enter() {
      Minecraft mc = Minecraft.getInstance();
      if (!editing && mc != null && mc.player != null && mc.screen == null) {
         editing = true;
         UiSounds.play(UiSounds.Sound.OPEN);
         dragging = null;
         hovered = null;
         lastApplied = "";

         try {
            KeyMapping.releaseAll();
         } catch (Throwable var2) {
         }

         mc.mouseHandler.releaseMouse();
      }
   }

   public static void toggle() {
      if (editing) {
         exit();
      } else {
         enter();
      }
   }

   public static void exit() {
      exit(true);
   }

   public static void exitNoGrab() {
      exit(false);
   }

   private static void exit(boolean regrab) {
      if (editing) {
         HudEditorPanel.reset();
         UiSounds.play(UiSounds.Sound.CLOSE);
         editing = false;
         dragging = null;
         hovered = null;
         RECTS.clear();
         Minecraft mc = Minecraft.getInstance();
         if (regrab && mc != null && mc.player != null && mc.screen == null) {
            mc.mouseHandler.grabMouse();
         }
      }
   }

   public static void validate() {
      if (pendingOpen != null) {
         Minecraft mc0 = Minecraft.getInstance();
         if (mc0 != null && mc0.screen == null && mc0.player != null) {
            String sel = pendingOpen;
            pendingOpen = null;
            enter();
            if (!sel.isEmpty()) {
               HudEditorPanel.select(sel);
            }
         }
      }

      if (editing) {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.player == null || mc.screen != null || mc.options.hideGui) {
            exitNoGrab();
         }
      }
   }

   public static void beginFrame(float designW, float designH) {
      frame++;
      canvasW = designW;
      canvasH = designH;
   }

   public static float[] rectOf(String id) {
      HudLayout.Rect r = RECTS.get(id);
      return r == null ? null : new float[]{r.x, r.y, r.w, r.h};
   }

   public static void report(String id, float x, float y, float w, float h) {
      if (editing && !(w <= 0.0F) && !(h <= 0.0F)) {
         HudLayout.Rect r = RECTS.computeIfAbsent(id, k -> new HudLayout.Rect());
         r.x = x;
         r.y = y;
         r.w = w;
         r.h = h;
         r.frame = frame;
      }
   }

   public static void onMouseButton(int button, int action) {
      if (editing) {
         float[] cur = cursor();
         if (cur != null && HudEditorPanel.onMouse(button, action, cur[0], cur[1])) {
            dragging = null;
         } else if (button == 1 && action == 1 && cur != null) {
            String hit = pick(cur[0], cur[1]);
            HudEditorPanel.select(hit);
         } else if (button == 0) {
            if (action == 1) {
               float[] c = cur;
               if (c == null) {
                  return;
               }

               String hit = pick(c[0], c[1]);
               if (hit != null) {
                  HudEditorPanel.select(hit);
               }

               if (hit != null) {
                  HudLayout.Rect r = RECTS.get(hit);
                  dragging = hit;
                  grabX = c[0] - r.x;
                  grabY = c[1] - r.y;
                  lastApplied = "";
               }
            } else if (action == 0) {
               dragging = null;
               snappedCenter = false;
            }
         }
      }
   }

   private static boolean gridKeyHeld() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return false;
      }

      try {
         return GLFW.glfwGetKey(mc.getWindow().handle(), 341) == 1;
      } catch (Throwable t) {
         return false;
      }
   }

   private static float snapAxis(float pos, float size, float total, float step) {
      float start = Math.round(pos / step) * step;
      float end = Math.round((pos + size) / step) * step - size;
      float s = Math.abs(start - pos) <= Math.abs(end - pos) ? start : end;
      return clamp(s, 0.0F, Math.max(0.0F, total - size));
   }

   private static float gridStep(String id) {
      return "HUD".equals(id) && HUD.INSTANCE != null ? 10.0F * HUD.INSTANCE.scale() : 10.0F;
   }

   private static float[] cursor() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return null;
      }

      try {
         long win = mc.getWindow().handle();
         double[] cx = new double[1];
         double[] cy = new double[1];
         int[] ww = new int[1];
         int[] wh = new int[1];
         GLFW.glfwGetCursorPos(win, cx, cy);
         GLFW.glfwGetWindowSize(win, ww, wh);
         return ww[0] > 0 && wh[0] > 0 ? new float[]{(float)(cx[0] / ww[0] * canvasW), (float)(cy[0] / wh[0] * canvasH)} : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private static String pick(float px, float py) {
      String hit = null;

      for (Entry<String, HudLayout.Rect> e : RECTS.entrySet()) {
         HudLayout.Rect r = e.getValue();
         if (r.frame == frame && px >= r.x && px <= r.x + r.w && py >= r.y && py <= r.y + r.h) {
            hit = e.getKey();
         }
      }

      return hit;
   }

   public static void drawOverlay(long vg, float w, float h) {
      if (editing) {
         gridOn = gridKeyHeld();
         float[] c = cursor();
         if (dragging != null && c != null) {
            HudLayout.Rect r = RECTS.get(dragging);
            if (r != null && r.frame == frame) {
               float nx = clamp(c[0] - grabX, 0.0F, Math.max(0.0F, w - r.w));
               float ny = clamp(c[1] - grabY, 0.0F, Math.max(0.0F, h - r.h));
               float step = gridStep(dragging);
               if (gridOn) {
                  nx = snapAxis(nx, r.w, w, step);
                  ny = snapAxis(ny, r.h, h, step);
               }

               float band = gridOn ? 5.0F : 10.0F;
               snappedCenter = false;
               if (Math.abs(nx + r.w * 0.5F - w * 0.5F) <= band) {
                  nx = (w - r.w) * 0.5F;
                  snappedCenter = true;
               }

               apply(dragging, nx, ny, r.w, r.h, w, h, snappedCenter);
               r.x = nx;
               r.y = ny;
            }
         }

         hovered = dragging == null && c != null && !HudEditorPanel.over(c[0], c[1]) ? pick(c[0], c[1]) : null;
         int accent = 0xFF000000 | ThemeColors.getAccentRgb();
         int surface = 0xFF000000 | ThemeColors.getSurfaceRgb();
         int text = 0xFF000000 | ThemeColors.getTextRgb();
         ClarityDraw.fillRRect(vg, 0.0F, 0.0F, w, h, 0.0F, 939524096);
         if (gridOn) {
            drawGrid(vg, w, h, dragging != null ? gridStep(dragging) : 10.0F, accent);
         }

         if (dragging != null && snappedCenter) {
            ClarityDraw.fillRRect(vg, w * 0.5F - 0.5F, 0.0F, 1.0F, h, 0.0F, withAlpha(accent, 0.65F));
         }

         String selected = HudEditorPanel.selected();
         int shown = 0;

         for (Entry<String, HudLayout.Rect> e : RECTS.entrySet()) {
            HudLayout.Rect r = e.getValue();
            if (r.frame == frame) {
               shown++;
               String id = e.getKey();
               boolean active = id.equals(dragging);
               boolean hov = id.equals(hovered);
               boolean sel = id.equals(selected);
               float pad = 3.0F;
               float x = r.x - pad;
               float y = r.y - pad;
               float bw = r.w + pad * 2.0F;
               float bh = r.h + pad * 2.0F;
               float fill = active ? 0.16F : (sel ? 0.1F : (hov ? 0.08F : 0.03F));
               float line = active ? 1.0F : (sel ? 0.9F : (hov ? 0.75F : 0.28F));
               ClarityDraw.fillRRect(vg, x, y, bw, bh, 7.0F, withAlpha(accent, fill));
               ClarityDraw.strokeRRect(vg, x, y, bw, bh, 7.0F, !active && !sel ? 1.0F : 1.5F, withAlpha(accent, line));
               if (active || hov || sel) {
                  float tw = ClarityDraw.textWidth(vg, "inter-sb", 9.5F, 0.0F, id);
                  float lw = tw + 14.0F;
                  float lh = 16.0F;
                  float lx = Math.max(2.0F, Math.min(w - lw - 2.0F, x));
                  float ly = y - lh - 4.0F;
                  if (ly < 2.0F) {
                     ly = y + bh + 4.0F;
                  }

                  ClarityDraw.fillRRect(vg, lx, ly, lw, lh, 8.0F, !active && !sel ? withAlpha(surface, 0.95F) : accent);
                  if (!active && !sel) {
                     ClarityDraw.strokeRRect(vg, lx, ly, lw, lh, 8.0F, 1.0F, withAlpha(accent, 0.6F));
                  }

                  int tc = !active && !sel ? text : onColor(accent);
                  ClarityDraw.text(vg, "inter-sb", 9.5F, 0.0F, tc, 18, lx + lw * 0.5F, ly + lh * 0.5F + 0.5F, id);
               }
            }
         }

         if (shown == 0) {
            String none = "No HUD elements are on. Switch some on in the editor window.";
            ClarityDraw.text(vg, "inter-med", 12.0F, 0.0F, withAlpha(text, 0.85F), 18, w * 0.5F, h * 0.5F, none);
         }

         HudEditorPanel.draw(vg, w, h, c == null ? -1.0F : c[0], c == null ? -1.0F : c[1]);
      }
   }

   private static int onColor(int argb) {
      float l = (0.299F * (argb >> 16 & 0xFF) + 0.587F * (argb >> 8 & 0xFF) + 0.114F * (argb & 0xFF)) / 255.0F;
      return l > 0.62F ? -15658216 : -1;
   }

   private static void drawGrid(long vg, float w, float h, float step, int accent) {
      int minor = withAlpha(accent, 0.12F);
      int major = withAlpha(accent, 0.24F);
      int i = 0;

      for (float x = 0.0F; x <= w; i++) {
         ClarityDraw.fillRRect(vg, x - 0.5F, 0.0F, 1.0F, h, 0.0F, i % 5 == 0 ? major : minor);
         x += step;
      }

      i = 0;

      for (float y = 0.0F; y <= h; i++) {
         ClarityDraw.fillRRect(vg, 0.0F, y - 0.5F, w, 1.0F, 0.0F, i % 5 == 0 ? major : minor);
         y += step;
      }
   }

   private static void apply(String id, float x, float y, float w, float h, float W, float H, boolean center) {
      float cx = x + w * 0.5F;
      float cy = y + h * 0.5F;
      boolean left = cx < W * 0.5F;
      boolean top = cy < H * 0.5F;
      float offX = left ? x : W - x - w;
      float offY = top ? y : H - y - h;
      Map<String, Object> m = new LinkedHashMap<>();
      switch (id) {
         case "Notifications":
            m.put("Position", (top ? "Top " : "Bottom ") + (center ? "Center" : (left ? "Left" : "Right")));
            m.put("X Offset", Math.max(0, Math.round(offX)));
            m.put("Y Offset", Math.max(0, Math.round(offY)));
            break;
         case "Keybind List":
         case "Music Player":
         case "Potion HUD":
         case "BPS":
         case "Clarity Icon":
            m.put("Anchor", (top ? "Top " : "Bottom ") + (left ? "Left" : "Right"));
            m.put("X Offset", Math.max(0, Math.round(offX)));
            m.put("Y Offset", Math.max(0, Math.round(offY)));
            break;
         case "Target Hud":
            if (!top && center) {
               Minecraft mc = Minecraft.getInstance();
               float clear = HudElements.bottomCenterClear(mc, W);
               m.put("Anchor", "Bottom Center");
               m.put("X", 0);
               m.put("Y", Math.round(H - h - clear - y));
            } else {
               m.put("Anchor", (top ? "Top " : "Bottom ") + (left ? "Left" : "Right"));
               m.put("X", Math.max(0, Math.round(offX)));
               m.put("Y", Math.max(0, Math.round(offY)));
            }
            break;
         case "Custom Scoreboard":
            m.put("Side", left ? "Left" : "Right");
            m.put("X Offset", Math.max(0, Math.round(offX)));
            m.put("Y Offset", Math.round(cy - H * 0.5F));
            break;
         case "ArrayList":
            m.put("Side", left ? "Left" : "Right");
            m.put("Margin X", Math.max(0, Math.round(offX)));
            m.put("Margin Y", Math.max(0, Math.round(y)));
            break;
         case "HUD":
            float s = HUD.INSTANCE != null ? HUD.INSTANCE.scale() : 1.0F;
            m.put("X", Math.max(0, Math.round(x / s)));
            m.put("Y", Math.max(0, Math.round(y / s)));
            break;
         default:
            return;
      }

      String sig = id + m;
      if (!sig.equals(lastApplied)) {
         lastApplied = sig;
         ClarityConfig.putSettings(id, m, true);
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static int withAlpha(int argb, float a) {
      int al = Math.round(clamp(a, 0.0F, 1.0F) * 255.0F);
      return al << 24 | argb & 16777215;
   }

   private static final class Rect {
      float x;
      float y;
      float w;
      float h;
      long frame;
   }
}
