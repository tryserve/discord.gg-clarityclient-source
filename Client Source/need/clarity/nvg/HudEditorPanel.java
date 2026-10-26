package need.clarity.nvg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import need.clarity.clickgui.Anim;
import need.clarity.clickgui.Canvas;
import need.clarity.clickgui.ConfigStore;
import need.clarity.clickgui.GuiData;
import need.clarity.clickgui.NvgCanvas;
import need.clarity.util.ThemeColors;
import need.clarity.util.UiSounds;

public final class HudEditorPanel {
   private static final float WIN_W = 244.0F;
   private static final float HEADER_H = 40.0F;
   private static final float PAD = 12.0F;
   private static final float RADIUS = 12.0F;
   private static final float LIST_ROW = 28.0F;
   private static final float POP_ROW = 24.0F;
   private static final int POP_MAX_ROWS = 8;
   private static final int[] SWATCHES = new int[]{16777215, 16733296, 16752451, 16106818, 3921530, 3065343, 8037111, 11820287, 16743094, 1052688};
   private static NvgCanvas canvas;
   private static Canvas lastCanvas;
   private static long canvasVg;
   private static ConfigStore store;
   private static final Anim AN = new Anim();
   private static long lastNanos;
   private static String selected;
   private static float scroll = 0.0F;
   private static float scrollTarget = 0.0F;
   private static float contentH = 0.0F;
   private static float listScroll = 0.0F;
   private static float listScrollTarget = 0.0F;
   private static float listContentH = 0.0F;
   private static String focusText;
   private static String textBuf = "";
   private static String dragSlider;
   private static float dragX0;
   private static float dragW;
   private static double dragMin;
   private static double dragMax;
   private static double dragStep;
   private static boolean mouseDown;
   private static String confirmReset;
   private static long confirmAt;
   private static String openColor;
   private static float winX = Float.NaN;
   private static float winY = Float.NaN;
   private static float winH;
   private static boolean collapsed;
   private static boolean winDrag;
   private static float grabX;
   private static float grabY;
   private static boolean posLoaded;
   private static float bodyTop;
   private static float bodyBot;
   private static String popId;
   private static List<String> popOpts;
   private static String popCur;
   private static Consumer<String> popPick;
   private static float popX;
   private static float popY;
   private static float popW;
   private static float popH;
   private static float popScroll;
   private static final List<HudEditorPanel.Hit> HITS = new ArrayList<>();
   private static int accent;
   private static int accent2;
   private static int surface;
   private static int text;
   private static int muted;
   private static int border;
   private static int track;
   private static float labelTint = 0.0F;
   private static boolean syncOn;
   private static String flashText;
   private static int flashColor;
   private static long flashAt;
   private static final Map<String, Integer> SHARED = new HashMap<>();
   private static float lastW = 1280.0F;
   private static float lastH = 720.0F;
   private static float lastMx;
   private static float lastMy;

   private HudEditorPanel() {
   }

   public static boolean isHudModule(String module) {
      GuiData.Module m = GuiData.get().modules.get(module);
      return m != null && ("HUD".equals(m.category) || "Custom Chat".equals(module));
   }

   public static void select(String module) {
      if (module != null && isHudModule(module)) {
         if (!module.equals(selected)) {
            scrollTarget = 0.0F;
            scroll = 0.0F;
            closePop();
            openColor = null;
            AN.set("view", 0.0F);
         }

         selected = module;
         focusText = null;
      } else {
         if (selected != null) {
            commitText();
            AN.set("view", 0.0F);
         }

         selected = null;
         openColor = null;
         closePop();
      }
   }

   public static String selected() {
      return selected;
   }

   public static void reset() {
      selected = null;
      focusText = null;
      dragSlider = null;
      mouseDown = false;
      winDrag = false;
      openColor = null;
      closePop();
   }

   private static ConfigStore store() {
      if (store == null) {
         store = new ConfigStore(GuiData.get());
      }

      return store;
   }

   private static Canvas canvas(long vg) {
      if (canvas == null || canvasVg != vg) {
         canvas = new NvgCanvas(vg);
         canvasVg = vg;
      }

      canvas.begin(1, 1);
      return canvas;
   }

   private static int a(int rgb, float al) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, al)) * 255.0F) << 24 | rgb & 16777215;
   }

   private static int mix(int x, int y, float t) {
      int xr = x >> 16 & 0xFF;
      int xg = x >> 8 & 0xFF;
      int xb = x & 0xFF;
      int yr = y >> 16 & 0xFF;
      int yg = y >> 8 & 0xFF;
      int yb = y & 0xFF;
      return Math.round(xr + (yr - xr) * t) << 16 | Math.round(xg + (yg - xg) * t) << 8 | Math.round(xb + (yb - xb) * t);
   }

   private static float luma(int rgb) {
      return (0.299F * (rgb >> 16 & 0xFF) + 0.587F * (rgb >> 8 & 0xFF) + 0.114F * (rgb & 0xFF)) / 255.0F;
   }

   public static void draw(long vg, float W, float H, float mx, float my) {
      draw(canvas(vg), W, H, mx, my);
   }

   public static void draw(Canvas c, float W, float H, float mx, float my) {
      HITS.clear();
      lastCanvas = c;
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.016666668F : (float)(now - lastNanos) / 1.0E9F;
      lastNanos = now;
      AN.frame(dt, 1.0F);
      accent = ThemeColors.getAccentRgb();
      accent2 = ThemeColors.getAccent2Rgb();
      surface = ThemeColors.getSurfaceRgb();
      text = ThemeColors.getTextRgb();
      muted = ThemeColors.getMutedRgb();
      border = ThemeColors.getBorderRgb();
      track = mix(surface, text, 0.16F);
      if (mouseDown && dragSlider != null) {
         updateSlider(mx);
      }

      loadPos(W, H);
      if (winDrag && mouseDown) {
         winX = mx - grabX;
         winY = my - grabY;
      }

      GuiData d = GuiData.get();
      if (selected != null && d.modules.get(selected) == null) {
         selected = null;
      }

      drawHints(c, W, H);
      float ghost = AN.to("ghost", HudLayout.draggingElement() ? 1.0F : 0.0F, 14.0F);
      float maxBody = Math.max(80.0F, Math.min(H - 28.0F - 40.0F, Math.max(320.0F, H * 0.6F)));
      float bodyTarget;
      if (collapsed) {
         bodyTarget = 0.0F;
      } else if (selected == null) {
         bodyTarget = Math.min(maxBody, listContentH > 0.0F ? listContentH : 8.0F + hudModules(d).size() * 28.0F);
      } else {
         bodyTarget = Math.min(maxBody, Math.max(60.0F, contentH));
      }

      float bodyH = AN.to("bodyH", bodyTarget, 20.0F);
      winH = 40.0F + bodyH;
      winX = clamp(winX, 6.0F, Math.max(6.0F, W - 244.0F - 6.0F));
      winY = clamp(winY, 6.0F, Math.max(6.0F, H - winH - 42.0F));
      float x = winX;
      float y = winY;
      c.save();
      c.alpha(1.0F - 0.62F * ghost);
      c.shadow(x, y + 4.0F, 244.0F, winH, 12.0F, 28.0F, a(0, 0.5F));
      c.rect(x, y, 244.0F, winH, 12.0F, a(surface, 0.97F));
      c.gradient(x, y, 244.0F, Math.min(winH, 60.0F), 12.0F, a(accent, 0.07F), a(accent, 0.0F), false);
      c.stroke(x, y, 244.0F, winH, 12.0F, 1.0F, a(mix(border, accent, 0.18F), 0.95F));
      drawHeader(c, d, x, y, mx, my);
      if (bodyH > 1.0F) {
         bodyTop = y + 40.0F;
         bodyBot = y + winH - 1.0F;
         c.line(x + 1.0F, bodyTop, x + 244.0F - 1.0F, bodyTop, 1.0F, a(border, 0.8F));
         c.save();
         c.scissor(x, bodyTop, 244.0F, Math.max(0.0F, bodyBot - bodyTop));
         float vt = AN.to("view", 1.0F, 16.0F);
         c.alpha(vt);
         c.translate((1.0F - vt) * 14.0F * (selected == null ? -1.0F : 1.0F), 0.0F);
         if (selected == null) {
            drawList(c, d, x, mx, my);
         } else {
            drawSettings(c, d.modules.get(selected), x, mx, my);
         }

         c.restore();
      }

      c.restore();
      drawFlash(c, W, H);
      if (popId != null) {
         drawPop(c, mx, my);
      }
   }

   private static List<String> hudModules(GuiData d) {
      List<String> mods = new ArrayList<>();

      for (GuiData.Module m : d.categories.getOrDefault("HUD", List.of())) {
         mods.add(m.name);
      }

      if (d.modules.containsKey("Custom Chat")) {
         mods.add("Custom Chat");
      }

      return mods;
   }

   private static void drawHeader(Canvas c, GuiData d, float x, float y, float mx, float my) {
      HITS.add(new HudEditorPanel.Hit(x, y, 244.0F, 40.0F, () -> {
         winDrag = true;
         grabX = lastMx - winX;
         grabY = lastMy - winY;
      }, null));
      boolean hvHeader = in(mx, my, x, y, 244.0F, 40.0F);
      float gh = AN.to("gripHv", !hvHeader && !winDrag ? 0.0F : 1.0F, 16.0F);
      float gw = 26.0F + 8.0F * gh;
      c.rect(x + 122.0F - gw / 2.0F, y + 5.0F, gw, 3.0F, 1.5F, a(text, 0.14F + 0.16F * gh));
      float cy = y + 20.0F + 2.0F;
      float right = x + 244.0F - 12.0F;
      float cb = 22.0F;
      float cbx = right - cb;
      iconButton(c, cbx, cy - cb / 2.0F, cb, mx, my);
      float rot = AN.to("collapse", collapsed ? 1.0F : 0.0F, 18.0F);
      chevron(c, cbx + cb / 2.0F, cy, 7.0F, (float) Math.PI * (0.5F - rot * 1.0F), a(text, 0.8F));
      HITS.add(new HudEditorPanel.Hit(cbx, cy - cb / 2.0F, cb, cb, () -> {
         collapsed = !collapsed;
         closePop();
         saveState();
      }, null));
      right = cbx - 6.0F;
      if (selected == null) {
         List<String> mods = hudModules(d);
         int on = 0;

         for (String m : mods) {
            if (store().isEnabled(m)) {
               on++;
            }
         }

         dotLogo(c, x + 12.0F + 4.0F, cy);
         c.text("inter-sb", 13.0F, x + 12.0F + 14.0F, cy, 17, a(text, 1.0F), "HUD Editor");
         String count = on + "/" + mods.size();
         float tw = c.textWidth("inter-sb", 10.0F, count) + 12.0F;
         c.rect(right - tw, cy - 9.0F, tw, 18.0F, 9.0F, a(accent, 0.14F));
         c.text("inter-sb", 10.0F, right - tw / 2.0F, cy, 18, a(mix(accent, text, 0.35F), 1.0F), count);
      } else {
         String sm = selected;
         float bb = 22.0F;
         float bbx = x + 12.0F - 4.0F;
         iconButton(c, bbx, cy - bb / 2.0F, bb, mx, my);
         chevron(c, bbx + bb / 2.0F + 0.5F, cy, 7.0F, (float) Math.PI, a(text, 0.85F));
         HITS.add(new HudEditorPanel.Hit(bbx, cy - bb / 2.0F, bb, bb, HudEditorPanel::back, null));
         boolean on = store().isEnabled(sm);
         float sw = 28.0F;
         float sh = 16.0F;
         float sx = right - sw;
         drawSwitch(c, "hdr:" + sm, sx, cy, sw, sh, on);
         HITS.add(new HudEditorPanel.Hit(sx - 4.0F, cy - 12.0F, sw + 8.0F, 24.0F, () -> toggleEnabled(sm), null));
         right = sx - 6.0F;
         boolean confirming = ("reset:" + sm).equals(confirmReset) && System.currentTimeMillis() - confirmAt < 2500L;
         float rb = 22.0F;
         float rbx = right - rb;
         boolean rhv = in(mx, my, rbx, cy - rb / 2.0F, rb, rb);
         float ct = AN.to("confirm", confirming ? 1.0F : 0.0F, 18.0F);
         c.rect(rbx, cy - rb / 2.0F, rb, rb, 7.0F, a(mix(text, 16733296, ct), rhv ? 0.12F + 0.1F * ct : 0.05F + 0.15F * ct));
         resetIcon(c, rbx + rb / 2.0F, cy, a(mix(text, 16743054, ct), 0.85F));
         HITS.add(new HudEditorPanel.Hit(rbx, cy - rb / 2.0F, rb, rb, () -> {
            if (("reset:" + sm).equals(confirmReset) && System.currentTimeMillis() - confirmAt < 2500L) {
               store().resetModule(sm);
               confirmReset = null;
            } else {
               confirmReset = "reset:" + sm;
               confirmAt = System.currentTimeMillis();
            }
         }, null));
         right = rbx - 6.0F;
         float lb = 22.0F;
         float lbx = right - lb;
         boolean lhv = in(mx, my, lbx, cy - lb / 2.0F, lb, lb);
         float st = AN.to("sync", syncOn ? 1.0F : 0.0F, 18.0F);
         c.rect(lbx, cy - lb / 2.0F, lb, lb, 7.0F, a(mix(text, accent, st), (lhv ? 0.12F : 0.05F) + 0.14F * st));
         linkIcon(c, lbx + lb / 2.0F, cy, a(mix(text, accent, st), 0.6F + 0.4F * st));
         HITS.add(new HudEditorPanel.Hit(lbx, cy - lb / 2.0F, lb, lb, () -> {
            syncOn = !syncOn;
            flash(syncOn ? "Sync on: changes go to every HUD element" : "Sync off", syncOn ? accent : muted);
            saveState();
         }, null));
         right = lbx - 8.0F;
         float tx = bbx + bb + 6.0F;
         String title = confirming ? "Click again to reset" : (lhv ? (syncOn ? "Sync: on" : "Sync: off") : sm);
         int titleCol = confirming ? a(16743054, 1.0F) : (lhv ? a(mix(text, accent, 0.5F), 1.0F) : a(text, 1.0F));
         float tfs = !confirming && !lhv ? 13.0F : 11.5F;
         c.text("inter-sb", tfs, tx, cy, 17, titleCol, ellipsize(c, "inter-sb", tfs, title, right - tx));
      }
   }

   private static void back() {
      commitText();
      selected = null;
      openColor = null;
      closePop();
      AN.set("view", 0.0F);
   }

   private static void drawList(Canvas c, GuiData d, float x, float mx, float my) {
      List<String> mods = hudModules(d);
      float viewH = bodyBot - bodyTop;
      listContentH = 8.0F + mods.size() * 28.0F;
      listScrollTarget = clamp(listScrollTarget, 0.0F, Math.max(0.0F, listContentH - viewH));
      listScroll = listScroll + (listScrollTarget - listScroll) * 0.35F;
      float ry = bodyTop + 4.0F - listScroll;
      float rx = x + 6.0F;
      float rw = 232.0F;

      for (String m : mods) {
         if (ry + 28.0F > bodyTop && ry < bodyBot) {
            boolean on = store().isEnabled(m);
            boolean hv = in(mx, my, rx, ry, rw, 28.0F) && in(mx, my, rx, bodyTop, rw, viewH) && popId == null && !winDrag;
            float h = AN.to("lh:" + m, hv ? 1.0F : 0.0F, 18.0F);
            if (h > 0.01F) {
               c.rect(rx, ry + 1.0F, rw, 26.0F, 8.0F, a(text, 0.06F * h));
            }

            float dcx = rx + 12.0F;
            float dcy = ry + 14.0F;
            if (on) {
               c.glow(dcx, dcy, 7.0F, a(accent, 0.35F));
               c.circle(dcx, dcy, 3.0F, a(accent, 1.0F));
            } else {
               c.ring(dcx, dcy, 2.6F, 1.2F, a(muted, 0.7F));
            }

            c.text("inter-med", 12.0F, rx + 24.0F + 2.0F * h, dcy, 17, a(on ? text : muted, on ? 0.95F : 0.85F), m);
            float sw = 26.0F;
            float sh = 14.0F;
            float sx = rx + rw - 30.0F - sw;
            drawSwitch(c, "ls:" + m, sx, dcy, sw, sh, on);
            chevron(c, rx + rw - 13.0F, dcy, 6.0F, 0.0F, a(text, 0.25F + 0.45F * h));
            String mod = m;
            float hy0 = Math.max(ry, bodyTop);
            float hy1 = Math.min(ry + 28.0F, bodyBot);
            if (hy1 > hy0) {
               HITS.add(new HudEditorPanel.Hit(rx, hy0, rw, hy1 - hy0, () -> select(mod), () -> toggleEnabled(mod)));
               HITS.add(new HudEditorPanel.Hit(sx - 6.0F, hy0, sw + 12.0F, hy1 - hy0, () -> toggleEnabled(mod), null));
            }
         }

         ry += 28.0F;
      }

      scrollbar(c, x, listScroll, listContentH, viewH);
   }

   private static void drawSettings(Canvas c, GuiData.Module mod, float x, float mx, float my) {
      float viewH = bodyBot - bodyTop;
      scrollTarget = clamp(scrollTarget, 0.0F, Math.max(0.0F, contentH - viewH));
      scroll = scroll + (scrollTarget - scroll) * 0.35F;
      float y = bodyTop + 6.0F - scroll;
      float y0 = y;
      float lx = x + 12.0F + 2.0F;
      float cw = 216.0F;
      boolean any = false;

      for (GuiData.Setting s : mod.settings) {
         if (visible(selected, mod, s)) {
            any = true;
            boolean child = s.meta != null && (s.meta.containsKey("showif") || s.meta.containsKey("showif2") || s.meta.containsKey("showifOr"));
            float ix = child ? 10.0F : 0.0F;
            labelTint = syncOn && sharedCount(selected, s) > 0 ? 0.45F : 0.0F;
            float ny = drawSetting(c, selected, s, lx + ix, y, cw - ix, mx, my);
            labelTint = 0.0F;
            GuiData.Setting fs = s;
            String fm = selected;
            if (sharedCount(fm, fs) > 0) {
               hit(lx + ix - 6.0F, y, cw - ix + 12.0F, ny - y, null, () -> copyToAll(fm, fs));
            }

            if (child && y + (ny - y) > bodyTop && y < bodyBot) {
               c.rect(lx + 1.0F, y + 5.0F, 2.0F, ny - y - 10.0F, 1.0F, a(accent, 0.35F));
            }

            y = ny;
         }
      }

      if (!any) {
         c.text("inter-med", 11.5F, x + 122.0F, y + 22.0F, 18, a(muted, 1.0F), "Nothing to set up here.");
         y += 44.0F;
      }

      contentH = y - y0 + 8.0F;
      scrollbar(c, x, scroll, contentH, viewH);
      if (scroll > 1.0F) {
         c.gradient(x + 1.0F, bodyTop, 242.0F, 12.0F, 0.0F, a(surface, 0.95F), a(surface, 0.0F), false);
      }

      if (scroll < contentH - viewH - 1.0F) {
         c.gradient(x + 1.0F, bodyBot - 12.0F, 242.0F, 12.0F, 0.0F, a(surface, 0.0F), a(surface, 0.95F), false);
      }
   }

   private static void scrollbar(Canvas c, float x, float scr, float content, float view) {
      if (!(content <= view + 1.0F)) {
         float th = Math.max(20.0F, view * view / content);
         float ty = bodyTop + 3.0F + (view - 6.0F - th) * (scr / Math.max(1.0F, content - view));
         c.rect(x + 244.0F - 5.0F, ty, 3.0F, th, 1.5F, a(text, 0.16F));
      }
   }

   private static void hit(float x, float y, float w, float h, HudEditorPanel.Action left, HudEditorPanel.Action right) {
      float y0 = Math.max(y, bodyTop);
      float y1 = Math.min(y + h, bodyBot);
      if (!(y1 - y0 < 3.0F)) {
         HITS.add(new HudEditorPanel.Hit(x, y0, w, y1 - y0, left, right));
      }
   }

   private static float drawSetting(Canvas c, String module, GuiData.Setting s, float x, float y, float w, float mx, float my) {
      String key = module + "|" + s.name;
      Object cur = value(module, s);
      switch (s.type) {
         case "check": {
            float h = 28.0F;
            boolean on = Boolean.TRUE.equals(cur) || "true".equals(String.valueOf(cur));
            if (rowVisible(y, h)) {
               rowHover(c, key, x, y, w, h, mx, my);
               label(c, x, y + h / 2.0F, s.name, w - 40.0F);
               drawSwitch(c, "s:" + key, x + w - 26.0F, y + h / 2.0F, 26.0F, 14.0F, on);
               hit(x - 6.0F, y, w + 12.0F, h, () -> set(module, s.name, !on), null);
            }

            return y + h;
         }
         case "range": {
            float h = 38.0F;
            double min = s.num(0, 0.0);
            double max = s.num(1, 1.0);
            double step = s.num(3, 0.0);
            double v = cur instanceof Number n ? n.doubleValue() : s.num(2, min);
            if (rowVisible(y, h)) {
               boolean dragging = key.equals(dragSlider);
               float ty = y + 27.0F;
               boolean hv = in(mx, my, x - 6.0F, ty - 9.0F, w + 12.0F, 18.0F) && popId == null || dragging;
               float ht = AN.to("rh:" + key, hv ? 1.0F : 0.0F, 18.0F);
               String vs = fmt(v, step, min, max);
               label(c, x, y + 11.0F, s.name, w - 50.0F);
               c.text("inter-sb", 11.0F, x + w, y + 11.0F, 20, a(mix(muted, accent, ht), 1.0F), vs);
               float t = (float)((v - min) / Math.max(1.0E-9, max - min));
               t = AN.to("rv:" + key, Math.max(0.0F, Math.min(1.0F, t)), 26.0F);
               float th = 4.0F + 1.0F * ht;
               c.rect(x, ty - th / 2.0F, w, th, th / 2.0F, a(track, 1.0F));
               if (t > 0.002F) {
                  c.gradient(x, ty - th / 2.0F, Math.max(th, w * t), th, th / 2.0F, a(accent2, 1.0F), a(accent, 1.0F), true);
               }

               float kx = x + w * t;
               float kr = 5.0F + 1.5F * ht;
               if (ht > 0.01F) {
                  c.glow(kx, ty, kr * 2.6F, a(accent, 0.3F * ht));
               }

               c.circle(kx, ty + 0.6F, kr + 0.6F, a(0, 0.25F));
               c.circle(kx, ty, kr, a(16777215, 1.0F));
               c.circle(kx, ty, kr * 0.38F, a(accent, ht));
               hit(x - 6.0F, ty - 10.0F, w + 12.0F, 20.0F, () -> {
                  dragSlider = key;
                  dragX0 = x;
                  dragW = w;
                  dragMin = min;
                  dragMax = max;
                  dragStep = step;
                  updateSlider(lastMx);
               }, null);
            }

            return y + h;
         }
         case "select": {
            List<String> opts = s.options();
            String sv = cur == null ? String.valueOf(s.defaultValue()) : String.valueOf(cur);
            int idx = Math.max(0, opts.indexOf(sv));
            boolean segmented = opts.size() >= 2 && opts.size() <= 3 && segFits(c, opts, Math.min(132.0F, w * 0.6F));
            float h = 30.0F;
            if (rowVisible(y, h)) {
               float cy = y + h / 2.0F;
               if (segmented) {
                  float bw = Math.min(132.0F, w * 0.6F);
                  float bx = x + w - bw;
                  float bh = 20.0F;
                  label(c, x, cy, s.name, w - bw - 8.0F);
                  c.rect(bx, cy - bh / 2.0F, bw, bh, 7.0F, a(track, 0.8F));
                  float segW = bw / opts.size();
                  float pos = AN.to("seg:" + key, idx, 20.0F);
                  c.shadow(bx + 2.0F + segW * pos, cy - bh / 2.0F + 2.0F, segW - 4.0F, bh - 4.0F, 5.0F, 6.0F, a(0, 0.3F));
                  c.rect(bx + 2.0F + segW * pos, cy - bh / 2.0F + 2.0F, segW - 4.0F, bh - 4.0F, 5.0F, a(mix(surface, text, 0.1F), 1.0F));
                  c.stroke(bx + 2.0F + segW * pos, cy - bh / 2.0F + 2.0F, segW - 4.0F, bh - 4.0F, 5.0F, 1.0F, a(accent, 0.5F));

                  for (int i = 0; i < opts.size(); i++) {
                     boolean sel = i == idx;
                     c.text("inter-med", 10.5F, bx + segW * i + segW / 2.0F, cy, 18, a(sel ? text : muted, sel ? 1.0F : 0.9F), ellipsize(c, "inter-med", 10.5F, opts.get(i), segW - 6.0F));
                     String o = opts.get(i);
                     hit(bx + segW * i, cy - bh / 2.0F, segW, bh, () -> set(module, s.name, o), null);
                  }
               } else {
                  float bw = Math.min(118.0F, w * 0.56F);
                  float bx = x + w - bw;
                  float bh = 22.0F;
                  float by = cy - bh / 2.0F;
                  label(c, x, cy, s.name, w - bw - 8.0F);
                  boolean open = key.equals(popId);
                  boolean hv = in(mx, my, bx, by, bw, bh) || open;
                  float ht = AN.to("dd:" + key, hv ? 1.0F : 0.0F, 18.0F);
                  c.rect(bx, by, bw, bh, 7.0F, a(mix(track, text, 0.04F * ht), 0.85F));
                  c.stroke(bx, by, bw, bh, 7.0F, 1.0F, a(mix(border, accent, open ? 1.0F : 0.45F * ht), 1.0F));
                  c.text("inter-med", 11.0F, bx + 9.0F, cy, 17, a(text, 0.95F), ellipsize(c, "inter-med", 11.0F, sv, bw - 28.0F));
                  chevron(c, bx + bw - 11.0F, cy, 6.0F, (float) Math.PI * (open ? -0.5F : 0.5F), a(mix(muted, accent, open ? 1.0F : ht), 1.0F));
                  if (!opts.isEmpty()) {
                     float fbx = bx;
                     float fby = by;
                     float fbw = bw;
                     float fbh = bh;
                     hit(bx, by, bw, bh, () -> {
                        if (key.equals(popId)) {
                           closePop();
                        } else {
                           openPop(key, opts, sv, v -> set(module, s.name, v), fbx, fby, fbw, fbh);
                        }
                     }, () -> set(module, s.name, opts.get((idx + 1) % opts.size())));
                  }
               }
            }

            return y + h;
         }
         case "color": {
            String sv = cur == null ? String.valueOf(s.defaultValue()) : String.valueOf(cur);
            boolean open = key.equals(openColor);
            float ot = AN.to("co:" + key, open ? 1.0F : 0.0F, 18.0F);
            float h = 28.0F + 30.0F * ot;
            if (rowVisible(y, h)) {
               float cy = y + 14.0F;
               int col = resolve(sv);
               String lbl = "theme".equalsIgnoreCase(sv) ? "Theme" : ("theme2".equalsIgnoreCase(sv) ? "Theme 2" : sv.toUpperCase(Locale.ROOT));
               label(c, x, cy, s.name, w - 100.0F);
               float chipW = 34.0F;
               float chipX = x + w - chipW;
               boolean hv = in(mx, my, chipX, cy - 9.0F, chipW, 18.0F);
               c.text("inter-med", 10.5F, chipX - 7.0F, cy, 20, a(muted, 1.0F), lbl);
               c.shadow(chipX, cy - 9.0F, chipW, 18.0F, 6.0F, 8.0F, a(col, 0.4F));
               c.rect(chipX, cy - 9.0F, chipW, 18.0F, 6.0F, a(col, 1.0F));
               c.stroke(chipX, cy - 9.0F, chipW, 18.0F, 6.0F, 1.0F, a(16777215, !open && !hv ? 0.18F : 0.7F));
               hit(chipX - 2.0F, cy - 11.0F, chipW + 4.0F, 22.0F, () -> openColor = key.equals(openColor) ? null : key, null);
               if (ot > 0.02F) {
                  c.save();
                  c.alpha(ot);
                  List<Object[]> chips = new ArrayList<>();
                  chips.add(new Object[]{"theme", ThemeColors.getAccentRgb()});
                  chips.add(new Object[]{"theme2", ThemeColors.getAccent2Rgb()});

                  for (int sw : SWATCHES) {
                     chips.add(new Object[]{String.format("#%06X", sw), sw});
                  }

                  float r = 7.5F;
                  float gap = (w - chips.size() * r * 2.0F) / Math.max(1, chips.size() - 1);
                  float sx = x + r;
                  float sy = y + 28.0F + 13.0F;

                  for (Object[] ch : chips) {
                     String id = (String)ch[0];
                     int cc = (Integer)ch[1];
                     boolean selc = id.equalsIgnoreCase(sv);
                     boolean chv = in(mx, my, sx - r, sy - r, r * 2.0F, r * 2.0F);
                     float rr = r - (selc ? 1.5F : 0.0F) + (chv ? 0.8F : 0.0F);
                     c.circle(sx, sy, rr, a(cc, 1.0F));
                     if (luma(cc) < 0.15F) {
                        c.ring(sx, sy, rr, 1.0F, a(text, 0.25F));
                     }

                     if (id.startsWith("theme")) {
                        c.text("inter-sb", 7.5F, sx, sy, 18, a(luma(cc) > 0.6F ? 1119000 : 16777215, 0.95F), id.equals("theme") ? "T" : "T2");
                     }

                     if (selc) {
                        c.ring(sx, sy, r + 1.5F, 1.4F, a(text, 1.0F));
                     }

                     hit(sx - r - 1.0F, sy - r - 1.0F, r * 2.0F + 2.0F, r * 2.0F + 2.0F, () -> set(module, s.name, id), null);
                     sx += r * 2.0F + gap;
                  }

                  c.restore();
               }
            }

            return y + h;
         }
         case "text": {
            float h = 30.0F;
            boolean focused = key.equals(focusText);
            String sv = focused ? textBuf : (cur == null ? String.valueOf(s.defaultValue()) : String.valueOf(cur));
            if (rowVisible(y, h)) {
               float cy = y + h / 2.0F;
               float bw = Math.min(108.0F, w * 0.5F);
               float bx = x + w - bw;
               float bh = 22.0F;
               float by = cy - bh / 2.0F;
               label(c, x, cy, s.name, w - bw - 8.0F);
               float ft = AN.to("tf:" + key, focused ? 1.0F : 0.0F, 18.0F);
               c.rect(bx, by, bw, bh, 7.0F, a(track, 0.7F));
               c.stroke(bx, by, bw, bh, 7.0F, 1.0F, a(mix(border, accent, ft), 1.0F));
               if (ft > 0.01F) {
                  c.shadow(bx, by, bw, bh, 7.0F, 8.0F, a(accent, 0.25F * ft));
               }

               c.save();
               c.scissor(bx + 4.0F, by, bw - 8.0F, bh);
               String shown = sv;
               float tw = c.textWidth("inter", 11.0F, shown);
               float off = focused ? Math.max(0.0F, tw - (bw - 20.0F)) : 0.0F;
               String disp = focused ? shown : ellipsize(c, "inter", 11.0F, shown, bw - 16.0F);
               float end = bx + 8.0F - off + c.text("inter", 11.0F, bx + 8.0F - off, cy, 17, a(text, 0.95F), disp);
               if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
                  float cx = disp.isEmpty() ? bx + 8.0F : end + 1.0F;
                  c.rect(cx, cy - 6.5F, 1.2F, 13.0F, 0.6F, a(accent, 1.0F));
               }

               c.restore();
               hit(bx, by, bw, bh, () -> {
                  commitText();
                  focusText = key;
                  textBuf = sv;
               }, null);
            }

            return y + h;
         }
         default: {
            float h = 28.0F;
            if (rowVisible(y, h)) {
               label(c, x, y + h / 2.0F, s.name, w - 80.0F);
               c.text("inter", 10.5F, x + w, y + h / 2.0F, 20, a(muted, 0.8F), "in ClickGUI");
            }

            return y + h;
         }
      }
   }

   private static boolean rowVisible(float y, float h) {
      return y + h > bodyTop && y < bodyBot;
   }

   private static void rowHover(Canvas c, String key, float x, float y, float w, float h, float mx, float my) {
      boolean hv = in(mx, my, x - 6.0F, y, w + 12.0F, h) && in(mx, my, x - 6.0F, bodyTop, w + 12.0F, bodyBot - bodyTop) && popId == null && !winDrag;
      float t = AN.to("row:" + key, hv ? 1.0F : 0.0F, 18.0F);
      if (t > 0.01F) {
         c.rect(x - 6.0F, y + 1.0F, w + 12.0F, h - 2.0F, 7.0F, a(text, 0.045F * t));
      }
   }

   private static boolean segFits(Canvas c, List<String> opts, float bw) {
      float seg = bw / opts.size();

      for (String o : opts) {
         if (c.textWidth("inter-med", 10.5F, o) > seg - 8.0F) {
            return false;
         }
      }

      return true;
   }

   private static void label(Canvas c, float x, float cy, String s, float maxW) {
      c.text("inter-med", 11.5F, x, cy, 17, a(mix(text, accent, labelTint), 0.86F + 0.14F * labelTint), ellipsize(c, "inter-med", 11.5F, s, maxW));
   }

   private static void flash(String s, int rgb) {
      flashText = s;
      flashColor = rgb;
      flashAt = System.currentTimeMillis();
   }

   private static GuiData.Setting twin(GuiData.Module other, GuiData.Setting s) {
      for (GuiData.Setting o : other.settings) {
         if (o.name.equals(s.name) && o.type.equals(s.type)) {
            return o;
         }
      }

      return null;
   }

   private static int sharedCount(String module, GuiData.Setting s) {
      String k = module + "|" + s.name + "|" + s.type;
      Integer n = SHARED.get(k);
      if (n != null) {
         return n;
      }

      int count = 0;
      GuiData d = GuiData.get();
      if (!"keybind".equals(s.type)) {
         for (String m : hudModules(d)) {
            if (!m.equals(module)) {
               GuiData.Module om = d.modules.get(m);
               if (om != null && twin(om, s) != null) {
                  count++;
               }
            }
         }
      }

      SHARED.put(k, count);
      return count;
   }

   private static int propagate(String module, String key, Object v) {
      GuiData d = GuiData.get();
      GuiData.Module src = d.modules.get(module);
      GuiData.Setting s = null;
      if (src != null) {
         for (GuiData.Setting x : src.settings) {
            if (x.name.equals(key)) {
               s = x;
               break;
            }
         }
      }

      if (s != null && !"keybind".equals(s.type)) {
         int n = 0;

         for (String m : hudModules(d)) {
            if (!m.equals(module)) {
               GuiData.Module om = d.modules.get(m);
               GuiData.Setting o = om == null ? null : twin(om, s);
               if (o != null) {
                  Object val = v;
                  if ("range".equals(o.type) && v instanceof Number num) {
                     double min = o.num(0, 0.0);
                     double max = o.num(1, 1.0);
                     double step = o.num(3, 0.0);
                     double x = Math.max(min, Math.min(max, num.doubleValue()));
                     if (step > 0.0) {
                        x = Math.max(min, Math.min(max, Math.round((x - min) / step) * step + min));
                     }

                     val = Math.round(x * 1000000.0) / 1000000.0;
                  } else if ("select".equals(o.type) && !o.options().contains(String.valueOf(v))) {
                     continue;
                  }

                  store().setValue(m, key, val);
                  n++;
               }
            }
         }

         return n;
      } else {
         return 0;
      }
   }

   private static void copyToAll(String module, GuiData.Setting s) {
      int n = propagate(module, s.name, value(module, s));
      UiSounds.play(n > 0 ? UiSounds.Sound.SUCCESS : UiSounds.Sound.ERROR);
      flash(n > 0 ? s.name + " copied to " + n + (n == 1 ? " element" : " elements") : "No other element has " + s.name, n > 0 ? accent : 16743054);
   }

   private static void drawFlash(Canvas c, float W, float H) {
      if (flashText != null) {
         long age = System.currentTimeMillis() - flashAt;
         if (age > 2000L) {
            flashText = null;
         } else {
            float t = Math.min(1.0F, (float)age / 140.0F) * Math.min(1.0F, (float)(2000L - age) / 260.0F);
            float fs = 11.0F;
            float pw = c.textWidth("inter-med", fs, flashText) + 30.0F;
            float ph = 24.0F;
            float px = clamp(winX + 122.0F - pw / 2.0F, 6.0F, W - pw - 6.0F);
            float py = winY + winH + 8.0F;
            if (py + ph > H - 40.0F) {
               py = winY - ph - 8.0F;
            }

            c.save();
            c.alpha(t);
            c.translate(0.0F, (1.0F - t) * -4.0F);
            c.shadow(px, py + 3.0F, pw, ph, ph / 2.0F, 14.0F, a(0, 0.45F));
            c.rect(px, py, pw, ph, ph / 2.0F, a(surface, 0.97F));
            c.stroke(px, py, pw, ph, ph / 2.0F, 1.0F, a(mix(border, flashColor, 0.6F), 1.0F));
            c.circle(px + 12.0F, py + ph / 2.0F, 3.0F, a(flashColor, 1.0F));
            c.text("inter-med", fs, px + 21.0F, py + ph / 2.0F, 17, a(text, 0.95F), flashText);
            c.restore();
         }
      }
   }

   private static void linkIcon(Canvas c, float cx, float cy, int col) {
      c.save();
      c.translate(cx, cy);
      c.rotate((float) (-Math.PI / 4));
      c.stroke(-6.2F, -2.6F, 7.4F, 5.2F, 2.6F, 1.4F, col);
      c.stroke(-1.2F, -2.6F, 7.4F, 5.2F, 2.6F, 1.4F, col);
      c.restore();
   }

   private static void drawSwitch(Canvas c, String key, float x, float cy, float w, float h, boolean on) {
      float t = AN.spring(key, on ? 1.0F : 0.0F, 420.0F, 28.0F);
      float tc = Math.max(0.0F, Math.min(1.0F, t));
      float y = cy - h / 2.0F;
      c.rect(x, y, w, h, h / 2.0F, a(mix(track, accent, tc), 1.0F));
      if (tc > 0.01F) {
         c.gradient(x, y, w, h, h / 2.0F, a(accent2, 0.0F), a(accent2, 0.5F * tc), true);
      }

      float r = h / 2.0F - 2.0F;
      float kx = x + 2.0F + r + (w - 4.0F - 2.0F * r) * t;
      c.circle(kx, cy + 0.6F, r + 0.4F, a(0, 0.22F));
      int offKnob = luma(surface) > 0.5F ? 16777215 : mix(text, surface, 0.15F);
      c.circle(kx, cy, r, a(mix(offKnob, 16777215, tc), 1.0F));
   }

   private static void iconButton(Canvas c, float x, float y, float s, float mx, float my) {
      boolean hv = in(mx, my, x, y, s, s) && popId == null;
      float t = AN.to("ib:" + (int)x + ":" + (int)y, hv ? 1.0F : 0.0F, 18.0F);
      c.rect(x, y, s, s, 7.0F, a(text, 0.04F + 0.08F * t));
   }

   private static void chevron(Canvas c, float cx, float cy, float s, float angle, int col) {
      float cos = (float)Math.cos(angle);
      float sin = (float)Math.sin(angle);
      float tipX = cx + cos * s * 0.25F;
      float tipY = cy + sin * s * 0.25F;
      float bx = cx - cos * s * 0.25F;
      float by = cy - sin * s * 0.25F;
      float px = -sin * s * 0.5F;
      float py = cos * s * 0.5F;
      c.line(bx + px, by + py, tipX, tipY, 1.5F, col);
      c.line(tipX, tipY, bx - px, by - py, 1.5F, col);
   }

   private static void resetIcon(Canvas c, float cx, float cy, int col) {
      float r = 4.6F;
      c.arc(cx, cy, r, -1.727876F, 3.6128316F, 1.4F, col);
      float ax = cx + (float)Math.cos(-1.7278759594743864) * r;
      float ay = cy + (float)Math.sin(-1.7278759594743864) * r;
      c.line(ax, ay, ax - 3.2F, ay - 0.6F, 1.4F, col);
      c.line(ax, ay, ax - 0.8F, ay + 3.0F, 1.4F, col);
   }

   private static void dotLogo(Canvas c, float cx, float cy) {
      c.glow(cx, cy, 8.0F, a(accent, 0.45F));
      c.circle(cx, cy, 3.5F, a(accent, 1.0F));
   }

   private static void drawHints(Canvas c, float W, float H) {
      String[][] items = new String[][]{{"Drag", "move"}, {"Ctrl", "snap"}, {"Right-click", "select"}, {"Esc", "done"}};
      float fs = 10.5F;
      float kfs = 9.5F;
      float gap = 14.0F;
      float total = 0.0F;

      for (String[] it : items) {
         total += c.textWidth("inter-sb", kfs, it[0]) + 10.0F + 5.0F + c.textWidth("inter-med", fs, it[1]);
      }

      total += gap * (items.length - 1) + 20.0F;
      float h = 24.0F;
      float x = (W - total) / 2.0F;
      float y = H - h - 10.0F;
      c.shadow(x, y, total, h, h / 2.0F, 14.0F, a(0, 0.35F));
      c.rect(x, y, total, h, h / 2.0F, a(surface, 0.9F));
      c.stroke(x, y, total, h, h / 2.0F, 1.0F, a(border, 0.9F));
      float cx = x + 10.0F;
      float cy = y + h / 2.0F;

      for (String[] it : items) {
         float kw = c.textWidth("inter-sb", kfs, it[0]) + 10.0F;
         c.rect(cx, cy - 8.0F, kw, 16.0F, 4.0F, a(text, 0.1F));
         c.rect(cx, cy + 6.5F, kw, 1.5F, 1.0F, a(0, 0.25F));
         c.text("inter-sb", kfs, cx + kw / 2.0F, cy, 18, a(text, 0.9F), it[0]);
         cx += kw + 5.0F;
         cx += c.text("inter-med", fs, cx, cy, 17, a(muted, 1.0F), it[1]);
         cx += gap;
      }
   }

   private static void openPop(String id, List<String> opts, String cur, Consumer<String> pick, float bx, float by, float bw, float bh) {
      popId = id;
      popOpts = opts;
      popCur = cur;
      popPick = pick;
      float longest = 0.0F;
      if (lastCanvas != null) {
         for (String o : opts) {
            longest = Math.max(longest, lastCanvas.textWidth("inter-med", 11.0F, o));
         }
      }

      popW = Math.max(bw, Math.min(220.0F, longest + 42.0F));
      popH = Math.min(opts.size(), 8) * 24.0F + 8.0F;
      popX = clamp(bx, 6.0F, Math.max(6.0F, lastW - popW - 6.0F));
      popY = by + bh + 4.0F;
      if (popY + popH > lastH - 8.0F) {
         popY = by - 4.0F - popH;
      }

      popY = Math.max(6.0F, popY);
      int ci = opts.indexOf(cur);
      popScroll = opts.size() > 8 ? clamp((ci - 4.0F) * 24.0F, 0.0F, (opts.size() - 8) * 24.0F) : 0.0F;
      AN.set("pop", 0.0F);
   }

   private static void closePop() {
      popId = null;
      popOpts = null;
      popPick = null;
   }

   private static void drawPop(Canvas c, float mx, float my) {
      float t = AN.to("pop", 1.0F, 22.0F);
      c.save();
      c.alpha(t);
      c.translate(0.0F, (1.0F - t) * -4.0F);
      c.shadow(popX, popY + 4.0F, popW, popH, 9.0F, 22.0F, a(0, 0.55F));
      c.rect(popX, popY, popW, popH, 9.0F, a(mix(surface, text, 0.04F), 0.99F));
      c.stroke(popX, popY, popW, popH, 9.0F, 1.0F, a(mix(border, accent, 0.25F), 1.0F));
      c.save();
      c.scissor(popX, popY + 4.0F, popW, popH - 8.0F);
      float y = popY + 4.0F - popScroll;

      for (String o : popOpts) {
         if (y + 24.0F > popY && y < popY + popH) {
            boolean sel = o.equals(popCur);
            boolean hv = in(mx, my, popX + 4.0F, y, popW - 8.0F, 24.0F) && in(mx, my, popX, popY + 4.0F, popW, popH - 8.0F);
            if (sel || hv) {
               c.rect(popX + 4.0F, y + 1.0F, popW - 8.0F, 22.0F, 6.0F, a(sel ? accent : text, sel ? 0.18F : 0.07F));
            }

            c.text("inter-med", 11.0F, popX + 12.0F, y + 12.0F, 17, a(sel ? mix(accent, text, 0.3F) : text, sel ? 1.0F : 0.9F), ellipsize(c, "inter-med", 11.0F, o, popW - 34.0F));
            if (sel) {
               float kx = popX + popW - 16.0F;
               float ky = y + 12.0F;
               c.line(kx - 3.5F, ky, kx - 1.0F, ky + 2.5F, 1.5F, a(accent, 1.0F));
               c.line(kx - 1.0F, ky + 2.5F, kx + 3.5F, ky - 2.5F, 1.5F, a(accent, 1.0F));
            }

            float hy0 = Math.max(y, popY + 4.0F);
            float hy1 = Math.min(y + 24.0F, popY + popH - 4.0F);
            String fo = o;
            Consumer<String> pk = popPick;
            if (hy1 - hy0 > 2.0F) {
               HITS.add(new HudEditorPanel.Hit(popX, hy0, popW, hy1 - hy0, () -> {
                  pk.accept(fo);
                  closePop();
               }, null));
            }
         }

         y += 24.0F;
      }

      c.restore();
      float content = popOpts.size() * 24.0F;
      float view = popH - 8.0F;
      if (content > view + 1.0F) {
         float th = Math.max(18.0F, view * view / content);
         float ty = popY + 4.0F + (view - th) * (popScroll / (content - view));
         c.rect(popX + popW - 5.0F, ty, 3.0F, th, 1.5F, a(text, 0.2F));
      }

      c.restore();
   }

   private static Object value(String module, GuiData.Setting s) {
      Object v = store().value(module, s.name);
      return v != null ? v : s.defaultValue();
   }

   private static void set(String module, String key, Object v) {
      if (v instanceof Boolean b) {
         UiSounds.toggle(b);
      } else if (v instanceof Number num && dragSlider != null && !(store().value(module, key) instanceof Number o && !(Math.abs(o.doubleValue() - num.doubleValue()) > 1.0E-9))) {
         UiSounds.tick((float)((num.doubleValue() - dragMin) / Math.max(1.0E-9, dragMax - dragMin)));
      }

      store().setValue(module, key, v);
      if (syncOn) {
         propagate(module, key, v);
      }
   }

   private static void toggleEnabled(String module) {
      boolean now = !store().isEnabled(module);
      UiSounds.toggle(now);
      store().setEnabled(module, now);
   }

   private static String fmt(double v, double step, double min, double max) {
      if (!(step >= 1.0) && (step != 0.0 || !(max - min > 20.0))) {
         return step > 0.0 && step >= 0.1 && Math.abs(step * 10.0 - Math.round(step * 10.0)) < 1.0E-6 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
      } else {
         return String.valueOf(Math.round(v));
      }
   }

   private static int resolve(String v) {
      if ("theme".equalsIgnoreCase(v)) {
         return ThemeColors.getAccentRgb();
      }

      if ("theme2".equalsIgnoreCase(v)) {
         return ThemeColors.getAccent2Rgb();
      }

      try {
         String h = v.startsWith("#") ? v.substring(1) : v;
         if (h.length() == 8) {
            h = h.substring(2);
         }

         return Integer.parseInt(h, 16) & 16777215;
      } catch (Exception e) {
         return 16777215;
      }
   }

   private static boolean visible(String module, GuiData.Module mod, GuiData.Setting s) {
      Map<String, Object> m = s.meta;
      if (m == null) {
         return true;
      }

      if (m.get("showifOr") instanceof List<?> l && l.size() >= 2 && l.get(1) instanceof List<?> vals) {
         String cur = String.valueOf(cur(module, mod, String.valueOf(l.get(0))));

         for (Object v : vals) {
            if (cur.equals(js(v))) {
               return true;
            }
         }

         return false;
      } else if (m.get("showif2") instanceof List<?> l && l.size() >= 4) {
         return match(cur(module, mod, String.valueOf(l.get(0))), l.get(1)) && match(cur(module, mod, String.valueOf(l.get(2))), l.get(3));
      } else {
         return m.get("showif") instanceof List<?> l && l.size() >= 2 ? match(cur(module, mod, String.valueOf(l.get(0))), l.get(1)) : true;
      }
   }

   private static Object cur(String module, GuiData.Module mod, String key) {
      Object v = store().value(module, key);
      if (v != null) {
         return v;
      }

      for (GuiData.Setting s : mod.settings) {
         if (s.name.equals(key)) {
            return s.defaultValue();
         }
      }

      return null;
   }

   private static String js(Object v) {
      return v instanceof Double d && d == Math.floor(d) ? String.valueOf(d.longValue()) : String.valueOf(v);
   }

   private static boolean match(Object cur, Object expected) {
      if (expected instanceof Boolean b) {
         return cur instanceof Boolean cb ? cb == b : String.valueOf(b).equals(String.valueOf(cur));
      } else {
         return js(expected).equals(js(cur));
      }
   }

   private static void updateSlider(float mx) {
      String[] k = dragSlider.split("\\|", 2);
      double t = Math.max(0.0F, Math.min(1.0F, (mx - dragX0) / Math.max(1.0F, dragW)));
      double v = dragMin + t * (dragMax - dragMin);
      if (dragStep > 0.0) {
         v = Math.round((v - dragMin) / dragStep) * dragStep + dragMin;
      }

      v = Math.max(dragMin, Math.min(dragMax, Math.round(v * 1000000.0) / 1000000.0));
      set(k[0], k[1], v);
   }

   private static void commitText() {
      if (focusText != null) {
         String[] k = focusText.split("\\|", 2);
         set(k[0], k[1], textBuf);
         focusText = null;
      }
   }

   private static String ellipsize(Canvas c, String font, float size, String s, float maxW) {
      if (s == null) {
         return "";
      }

      if (maxW <= 8.0F) {
         return "";
      }

      if (c.textWidth(font, size, s) <= maxW) {
         return s;
      }

      String t = s;

      while (t.length() > 1 && c.textWidth(font, size, t + "…") > maxW) {
         t = t.substring(0, t.length() - 1);
      }

      return t + "…";
   }

   private static void loadPos(float W, float H) {
      lastW = W;
      lastH = H;
      if (!posLoaded) {
         posLoaded = true;

         try {
            if (store().gui("HUD Editor X") instanceof Number nx) {
               winX = nx.floatValue() * W;
            }

            if (store().gui("HUD Editor Y") instanceof Number ny) {
               winY = ny.floatValue() * H;
            }

            Object col = store().gui("HUD Editor Collapsed");
            collapsed = Boolean.TRUE.equals(col) || "true".equals(String.valueOf(col));
            Object sy = store().gui("HUD Editor Sync");
            syncOn = Boolean.TRUE.equals(sy) || "true".equals(String.valueOf(sy));
         } catch (Throwable var4) {
         }

         if (Float.isNaN(winX)) {
            winX = 14.0F;
         }

         if (Float.isNaN(winY)) {
            winY = 60.0F;
         }
      }
   }

   private static void saveState() {
      try {
         store().setGui("HUD Editor X", Math.round(winX / Math.max(1.0F, lastW) * 10000.0) / 10000.0);
         store().setGui("HUD Editor Y", Math.round(winY / Math.max(1.0F, lastH) * 10000.0) / 10000.0);
         store().setGui("HUD Editor Collapsed", collapsed);
         store().setGui("HUD Editor Sync", syncOn);
      } catch (Throwable var1) {
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static boolean in(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   public static boolean over(float mx, float my) {
      return popId != null && in(mx, my, popX, popY, popW, popH) ? true : !Float.isNaN(winX) && in(mx, my, winX, winY, 244.0F, winH);
   }

   public static boolean onMouse(int button, int action, float mx, float my) {
      lastMx = mx;
      lastMy = my;
      if (action == 0) {
         mouseDown = false;
         if (winDrag) {
            winDrag = false;
            saveState();
            return true;
         } else if (dragSlider != null) {
            updateSlider(mx);
            dragSlider = null;
            return true;
         } else {
            return over(mx, my);
         }
      } else {
         if (action != 1) {
            return false;
         }

         if (popId != null && !in(mx, my, popX, popY, popW, popH)) {
            closePop();
            return true;
         }

         boolean overUi = over(mx, my);
         if (!overUi) {
            commitText();
            openColor = null;
            return false;
         }

         mouseDown = button == 0;

         for (int i = HITS.size() - 1; i >= 0; i--) {
            HudEditorPanel.Hit h = HITS.get(i);
            if (in(mx, my, h.x, h.y, h.w, h.h)) {
               HudEditorPanel.Action act = button == 1 ? h.right : h.left;
               if (act != null) {
                  commitText();
                  UiSounds.beginClick();

                  try {
                     act.run();
                  } finally {
                     UiSounds.endClick(winDrag || dragSlider != null);
                  }

                  return true;
               }
            }
         }

         commitText();
         return true;
      }
   }

   public static boolean onScroll(float mx, float my, double amount) {
      if (popId != null && in(mx, my, popX, popY, popW, popH)) {
         float max = Math.max(0.0F, popOpts.size() * 24.0F - (popH - 8.0F));
         popScroll = clamp(popScroll - (float)amount * 24.0F * 2.0F, 0.0F, max);
         return true;
      }

      if (popId != null) {
         closePop();
      }

      if (over(mx, my)) {
         if (selected != null) {
            scrollTarget -= (float)amount * 36.0F;
         } else {
            listScrollTarget -= (float)amount * 36.0F;
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean onKey(int key) {
      if (popId != null && key == 256) {
         closePop();
         return true;
      }

      if (focusText == null) {
         return false;
      }

      switch (key) {
         case 256:
            focusText = null;
            break;
         case 257:
         case 335:
            commitText();
            break;
         case 259:
            if (!textBuf.isEmpty()) {
               textBuf = textBuf.substring(0, textBuf.length() - 1);
            }
      }

      return true;
   }

   public static boolean onChar(String s) {
      if (focusText != null && s != null && !s.isEmpty() && !Character.isISOControl(s.charAt(0))) {
         if (textBuf.length() < 120) {
            textBuf = textBuf + s;
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean typing() {
      return focusText != null;
   }

   private interface Action {
      void run();
   }

   private record Hit(float x, float y, float w, float h, HudEditorPanel.Action left, HudEditorPanel.Action right) {
   }
}
