package need.clarity.menu;

import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import need.clarity.clickgui.Canvas;
import need.clarity.modules.visual.MainMenu;
import need.clarity.util.UiSounds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

public final class MainMenuScreen extends MenuScreen {
   private static final String[] LABELS = new String[]{"Singleplayer", "Multiplayer", "Options", "Alt Manager"};
   private static final float BTN_W = 300.0F;
   private static final float BTN_H = 50.0F;
   private static final float BTN_GAP = 10.0F;
   private static final float LOGO = 116.0F;
   private static final float ROW_H = 62.0F;
   private static final float ROW_GAP = 6.0F;
   private static final int HOME = 0;
   private static final int WORLDS = 1;
   private static final int SERVERS = 2;
   private final float[] hover = new float[LABELS.length];
   private float quitHover = 0.0F;
   private int page = 0;
   private int shownPage = 0;
   private float pageA = 0.0F;
   private String query = "";
   private float scroll = 0.0F;
   private float scrollTarget = 0.0F;
   private final Map<String, Float> rowHover = new HashMap<>();
   private final float[] footHover = new float[4];
   private final float[] footW = new float[4];
   private float closeHover = 0.0F;
   private String selected = null;
   private String confirmRemove = null;
   private long lastClickAt = 0L;
   private String lastClickKey = null;
   private volatile List<LevelSummary> worlds = null;
   private volatile String worldsError = null;
   private final Map<String, byte[]> worldIcons = new ConcurrentHashMap<>();
   private boolean worldsLoading = false;
   private ServerList servers;
   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(4, r -> {
      Thread t = new Thread(r, "clarity-menu-ping");
      t.setDaemon(true);
      return t;
   });
   private static final String VERSION_LABEL = "Clarity " + version();

   public MainMenuScreen() {
      super("Clarity");
   }

   private static int layout() {
      return switch (MainMenu.layout()) {
         case "Left" -> 1;
         case "Minimal" -> 2;
         default -> 0;
      };
   }

   private float stackTop() {
      float total = 172.0F + LABELS.length * 50.0F + (LABELS.length - 1) * 10.0F;
      return (this.h - total) / 2.0F - 10.0F;
   }

   private float buttonY(int i) {
      return layout() == 2 ? this.h - 118.0F : this.stackTop() + 116.0F + 56.0F + i * 60.0F;
   }

   private float buttonX(int i) {
      return switch (layout()) {
         case 1 -> this.leftX();
         case 2 -> this.w / 2.0F - (LABELS.length * 190.0F - 10.0F) / 2.0F + i * 190.0F;
         default -> (this.w - 300.0F) / 2.0F;
      };
   }

   private float buttonW() {
      return layout() == 2 ? 180.0F : (layout() == 1 ? 260.0F : 300.0F);
   }

   private float leftX() {
      return Math.max(56.0F, this.w * 0.07F);
   }

   private float panelW() {
      return layout() == 1 ? Math.min(700.0F, this.w - (this.leftX() + 260.0F + 70.0F) - 56.0F) : Math.min(700.0F, this.w - 120.0F);
   }

   private float panelX() {
      if (layout() == 1) {
         float l = this.leftX() + 260.0F + 70.0F;
         return l + Math.max(0.0F, (this.w - 56.0F - l - this.panelW()) / 2.0F);
      } else {
         return (this.w - this.panelW()) / 2.0F;
      }
   }

   private float panelY() {
      return layout() == 2 ? Math.max(48.0F, this.h * 0.08F) : Math.max(56.0F, this.h * 0.1F);
   }

   private float panelH() {
      return (layout() == 2 ? this.h - 150.0F : this.h - 70.0F) - this.panelY();
   }

   private float listTop() {
      return this.panelY() + 92.0F;
   }

   private float listBot() {
      return this.panelY() + this.panelH() - 70.0F;
   }

   @Override
   protected void draw(Canvas c, float w, float h) {
      if (this.page != 0) {
         this.shownPage = this.page;
      }

      this.pageA = this.approach(this.pageA, this.page != 0 ? 1.0F : 0.0F, 11.0F);
      if (this.page == 0 && this.pageA < 0.01F) {
         this.shownPage = 0;
      }

      if (this.servers != null) {
         try {
            this.pinger.tick();
         } catch (Throwable var5) {
         }
      }

      float homeA = layout() == 1 ? 1.0F : 1.0F - this.pageA;
      if (homeA > 0.01F) {
         this.drawHome(c, w, h, homeA);
      }

      if (this.shownPage != 0 && this.pageA > 0.01F) {
         this.drawPanel(c, easeOut(this.pageA));
      }

      this.drawFooter(c, w, h);
   }

   private void drawHome(Canvas c, float w, float h, float a) {
      float t = MenuRenderer.time();
      float age = this.age();
      int lay = layout();
      float shift = lay == 0 ? -50.0F * this.pageA : 0.0F;
      float logoIn = easeOut(age / 0.9F);
      float logoSize = lay == 2 ? 132.0F : 116.0F;
      float lx;
      float ly;
      if (lay == 1) {
         lx = this.leftX();
         ly = this.stackTop();
      } else if (lay == 2) {
         lx = w / 2.0F - logoSize / 2.0F;
         ly = h * 0.36F - logoSize / 2.0F;
      } else {
         lx = w / 2.0F - 58.0F;
         ly = this.stackTop();
      }

      lx += MenuRenderer.parallaxX(-6.0F) + shift;
      ly += MenuRenderer.parallaxY(-6.0F);
      float g = MainMenu.glow();
      float breathe = 0.82F + 0.18F * (float)Math.sin(t * 1.6F);
      float logoA = lay == 2 ? a * (1.0F - this.pageA) : a;
      c.save();
      c.alpha(logoIn * logoA);
      c.glow(lx + logoSize / 2.0F, ly + logoSize / 2.0F, logoSize * 1.55F, white(0.1F * g * breathe));
      c.glow(lx + logoSize / 2.0F, ly + logoSize / 2.0F, logoSize * 0.85F, white(0.16F * g * breathe));
      float s = 0.92F + 0.08F * logoIn;
      float ls = logoSize * s;
      c.image("clarity_logo", lx + (logoSize - ls) / 2.0F, ly + (logoSize - ls) / 2.0F, ls, ls, 1.0F);
      if (lay == 1) {
         c.text("inter-sb", 34.0F, lx + logoSize + 22.0F, ly + logoSize / 2.0F - 8.0F, 17, white(0.95F), "Clarity");
         c.text("inter", 13.0F, lx + logoSize + 23.0F, ly + logoSize / 2.0F + 20.0F, 17, white(0.4F), VERSION_LABEL);
      }

      c.restore();
      float bw = this.buttonW();

      for (int i = 0; i < LABELS.length; i++) {
         float bx = this.buttonX(i) + shift;
         float by = this.buttonY(i);
         float in = easeOut((age - 0.25F - i * 0.07F) / 0.6F);
         if (!(in <= 0.0F)) {
            boolean active = i == 0 && this.page == 1 || i == 1 && this.page == 2;
            boolean over = in > 0.95F && a > 0.9F && this.over(bx, by, bw, 50.0F);
            this.hover[i] = this.approach(this.hover[i], !over && !active ? 0.0F : 1.0F, 14.0F);
            float hv = this.hover[i];
            c.save();
            c.alpha(in * a);
            c.translate(0.0F, (1.0F - in) * 14.0F);
            if (hv > 0.01F) {
               c.glow(bx + bw / 2.0F, by + 25.0F, bw * 0.42F, white(0.05F * hv));
               c.rect(bx, by, bw, 50.0F, 12.0F, white(0.07F * hv));
               c.stroke(bx + 0.5F, by + 0.5F, bw - 1.0F, 49.0F, 12.0F, 1.0F, white(0.18F * hv));
            }

            float size = (lay == 2 ? 16.5F : 19.0F) + hv * 0.6F;
            if (lay == 1) {
               c.text("inter-med", size, bx + 22.0F + 4.0F * hv, by + 25.0F, 17, white(0.62F + 0.38F * hv), LABELS[i]);
               float uh = 18.0F * hv;
               if (uh > 0.5F) {
                  c.line(bx + 10.0F, by + 25.0F - uh / 2.0F, bx + 10.0F, by + 25.0F + uh / 2.0F, 2.0F, white(0.85F * hv));
               }
            } else {
               float cx = bx + bw / 2.0F;
               c.text("inter-med", size, cx, by + 25.0F, 18, white(0.62F + 0.38F * hv), LABELS[i]);
               float uw = 26.0F * hv;
               if (uw > 0.5F) {
                  c.line(cx - uw, by + 50.0F - 11.0F, cx + uw, by + 50.0F - 11.0F, 1.5F, white(0.85F * hv));
               }
            }

            c.restore();
         }
      }
   }

   private void drawFooter(Canvas c, float w, float h) {
      float foot = easeOut((this.age() - 0.6F) / 0.8F);
      c.save();
      c.alpha(foot);
      if (MainMenu.showAccount()) {
         Minecraft mc0 = Minecraft.getInstance();
         String name = mc0 != null && mc0.getUser() != null ? mc0.getUser().getName() : "Player";
         float ax = 32.0F;
         float ay = h - 56.0F;
         c.circle(ax + 12.0F, ay + 12.0F, 12.0F, white(0.1F));
         c.ring(ax + 12.0F, ay + 12.0F, 12.0F, 1.0F, white(0.22F));
         c.text("inter-sb", 12.0F, ax + 12.0F, ay + 12.5F, 18, white(0.9F), name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
         c.text("inter-med", 14.0F, ax + 34.0F, ay + 6.0F, 17, white(0.85F), name);
         c.text("inter", 11.5F, ax + 34.0F, ay + 21.0F, 17, white(0.38F), "Signed in");
      }

      if (layout() != 1) {
         c.text("inter", 12.0F, w - 32.0F, h - 38.0F, 20, white(0.28F), VERSION_LABEL);
      }

      float qx = w - 52.0F;
      float qy = 28.0F;
      float qs = 24.0F;
      this.quitHover = this.approach(this.quitHover, this.over(qx - 6.0F, qy - 6.0F, qs + 12.0F, qs + 12.0F) ? 1.0F : 0.0F, 14.0F);
      float qa = 0.35F + 0.55F * this.quitHover;
      float qcx = qx + qs / 2.0F;
      float qcy = qy + qs / 2.0F;
      c.arc(qcx, qcy, 8.0F, (float)Math.toRadians(-60.0), (float)Math.toRadians(240.0), 1.6F, white(qa));
      c.line(qcx, qcy - 10.0F, qcx, qcy - 2.0F, 1.6F, white(qa));
      c.restore();
   }

   private void drawPanel(Canvas c, float a) {
      float px = this.panelX();
      float py = this.panelY();
      float pw = this.panelW();
      float ph = this.panelH();
      c.save();
      c.alpha(a);
      c.translate(layout() == 1 ? (1.0F - a) * 30.0F : 0.0F, layout() == 1 ? 0.0F : (1.0F - a) * 18.0F);
      c.shadow(px, py, pw, ph, 20.0F, 40.0F, black(0.45F));
      c.rect(px, py, pw, ph, 20.0F, black(0.42F));
      c.glass(px, py, pw, ph, 20.0F, 0.6F);
      c.stroke(px + 0.5F, py + 0.5F, pw - 1.0F, ph - 1.0F, 20.0F, 1.0F, white(0.1F));
      boolean w0 = this.shownPage == 1;
      List<MainMenuScreen.Row> rows = this.rows();
      c.text("inter-sb", 26.0F, px + 28.0F, py + 40.0F, 17, white(0.97F), w0 ? "Singleplayer" : "Multiplayer");
      String sub = w0 ? (this.worlds == null ? (this.worldsError != null ? this.worldsError : "Loading worlds…") : count(rows.size(), "world")) : count(rows.size(), "server");
      c.text("inter", 13.0F, px + 28.0F, py + 66.0F, 17, white(0.42F), sub);
      float sw = Math.min(220.0F, pw * 0.36F);
      float sx = px + pw - 28.0F - 34.0F - 10.0F - sw;
      float sy = py + 26.0F;
      c.rect(sx, sy, sw, 30.0F, 15.0F, white(this.query.isEmpty() ? 0.05F : 0.09F));
      c.stroke(sx + 0.5F, sy + 0.5F, sw - 1.0F, 29.0F, 15.0F, 1.0F, white(this.query.isEmpty() ? 0.1F : 0.22F));
      c.ring(sx + 16.0F, sy + 14.0F, 5.0F, 1.4F, white(0.5F));
      c.line(sx + 19.5F, sy + 17.5F, sx + 23.0F, sy + 21.0F, 1.4F, white(0.5F));
      boolean caret = (System.currentTimeMillis() / 530L & 1L) == 0L;
      c.text(
         "inter",
         13.0F,
         sx + 30.0F,
         sy + 15.5F,
         17,
         white(this.query.isEmpty() ? 0.35F : 0.9F),
         this.query.isEmpty() ? "Type to search" : ellipsize(c, "inter", 13.0F, this.query, sw - 44.0F) + (caret ? "|" : "")
      );
      float cx = px + pw - 28.0F - 17.0F;
      float cy = py + 41.0F;
      this.closeHover = this.approach(this.closeHover, this.over(cx - 17.0F, cy - 17.0F, 34.0F, 34.0F) ? 1.0F : 0.0F, 14.0F);
      c.circle(cx, cy, 17.0F, white(0.04F + 0.08F * this.closeHover));
      float xa = 0.5F + 0.45F * this.closeHover;
      c.line(cx - 5.0F, cy - 5.0F, cx + 5.0F, cy + 5.0F, 1.6F, white(xa));
      c.line(cx + 5.0F, cy - 5.0F, cx - 5.0F, cy + 5.0F, 1.6F, white(xa));
      float top = this.listTop();
      float bot = this.listBot();
      float lx = px + 16.0F;
      float lw = pw - 32.0F;
      float content = rows.size() * 68.0F;
      float maxScroll = Math.max(0.0F, content - (bot - top));
      this.scrollTarget = Math.max(0.0F, Math.min(maxScroll, this.scrollTarget));
      this.scroll = this.approach(this.scroll, this.scrollTarget, 18.0F);
      c.save();
      c.scissor(px, top - 2.0F, pw, bot - top + 4.0F);
      if (rows.isEmpty()) {
         float my0 = (top + bot) / 2.0F;
         String msg = !this.query.isEmpty() ? "Nothing matches \"" + this.query + "\"" : (w0 ? (this.worlds == null ? "" : "No worlds yet") : "No servers yet");
         String hint = !this.query.isEmpty() ? "Backspace to clear the search" : (w0 ? (this.worlds == null ? "" : "Create one below to start playing.") : "Add one below, or use Direct Connect.");
         if (w0 && this.worlds == null && this.worldsError == null) {
            spinner(c, px + pw / 2.0F, my0, white(0.6F));
         }

         c.text("inter-med", 15.0F, px + pw / 2.0F, my0 - 8.0F, 18, white(0.7F), msg);
         c.text("inter", 12.5F, px + pw / 2.0F, my0 + 14.0F, 18, white(0.35F), hint);
      }

      for (int i = 0; i < rows.size(); i++) {
         float ry = top + i * 68.0F - this.scroll;
         if (!(ry + 62.0F < top - 4.0F) && !(ry > bot + 4.0F)) {
            this.drawRow(c, rows.get(i), lx, ry, lw, top, bot);
         }
      }

      c.restore();
      if (this.scroll > 1.0F) {
         c.gradient(px + 1.0F, top - 2.0F, pw - 2.0F, 18.0F, 0.0F, black(0.35F), black(0.0F), false);
      }

      if (this.scroll < maxScroll - 1.0F) {
         c.gradient(px + 1.0F, bot - 16.0F, pw - 2.0F, 18.0F, 0.0F, black(0.0F), black(0.35F), false);
      }

      String[] fb = this.footerLabels();
      float fy = py + ph - 52.0F;
      float fx = px + 28.0F;

      for (int i = 0; i < fb.length; i++) {
         float bw = this.footerW(c, fb[i], i);
         this.footW[i] = bw;
         boolean ov = this.over(fx, fy, bw, 34.0F);
         this.footHover[i] = this.approach(this.footHover[i], ov ? 1.0F : 0.0F, 14.0F);
         float hv = this.footHover[i];
         if (i == 0) {
            c.rect(fx, fy, bw, 34.0F, 11.0F, white(0.9F + 0.08F * hv));
            c.text("inter-sb", 13.5F, fx + bw / 2.0F, fy + 17.5F, 18, black(0.9F), fb[i]);
         } else {
            c.rect(fx, fy, bw, 34.0F, 11.0F, white(0.05F + 0.07F * hv));
            c.stroke(fx + 0.5F, fy + 0.5F, bw - 1.0F, 33.0F, 11.0F, 1.0F, white(0.12F + 0.12F * hv));
            c.text("inter-med", 13.5F, fx + bw / 2.0F, fy + 17.5F, 18, white(0.65F + 0.3F * hv), fb[i]);
         }

         fx += bw + 10.0F;
      }

      c.restore();
   }

   private float footerW(Canvas c, String label, int i) {
      return Math.max(i == 0 ? 130.0F : 96.0F, c.textWidth(i == 0 ? "inter-sb" : "inter-med", 13.5F, label) + 36.0F);
   }

   private String[] footerLabels() {
      return this.shownPage == 1 ? new String[]{"Create World", "All Worlds"} : new String[]{"Add Server", "Direct Connect", "Refresh", "All Servers"};
   }

   private void drawRow(Canvas c, MainMenuScreen.Row r, float x, float y, float rw, float top, float bot) {
      boolean ov = this.my >= top && this.my <= bot && this.over(x, y, rw, 62.0F);
      boolean sel = r.key.equals(this.selected);
      float hv = this.approach(this.rowHover.getOrDefault(r.key, 0.0F), ov ? 1.0F : (sel ? 0.55F : 0.0F), 14.0F);
      this.rowHover.put(r.key, hv);
      if (hv > 0.01F) {
         c.rect(x, y, rw, 62.0F, 14.0F, white(0.055F * hv));
         c.stroke(x + 0.5F, y + 0.5F, rw - 1.0F, 61.0F, 14.0F, 1.0F, white(0.11F * hv));
      }

      float is = 40.0F;
      float ix = x + 12.0F;
      float iy = y + (62.0F - is) / 2.0F;
      if (!c.imageBytes("menu:" + r.key, r.icon, ix, iy, is, is, 9.0F, 1.0F)) {
         c.rect(ix, iy, is, is, 9.0F, white(0.08F));
         String l = r.title.isEmpty() ? "?" : r.title.substring(0, 1).toUpperCase();
         c.text("inter-sb", 17.0F, ix + is / 2.0F, iy + is / 2.0F + 0.5F, 18, white(0.75F), l);
      }

      float bx = x + rw - 12.0F;
      float actionsW = 0.0F;
      if (hv > 0.02F && r.playable) {
         float pbw = 64.0F;
         float pbx = bx - pbw;
         boolean op = this.over(pbx, y + 15.0F, pbw, 32.0F);
         c.rect(pbx, y + 15.0F, pbw, 32.0F, 10.0F, white((op ? 0.98F : 0.88F) * hv));
         c.text("inter-sb", 13.0F, pbx + pbw / 2.0F, y + 31.5F, 18, black(0.9F * hv), this.shownPage == 1 ? "Play" : "Join");
         actionsW = pbw + 8.0F;
         if (this.shownPage == 2) {
            boolean confirm = r.key.equals(this.confirmRemove);
            float dx = pbx - 8.0F - (confirm ? 74.0F : 30.0F);
            boolean od = this.over(dx, y + 16.0F, confirm ? 74.0F : 30.0F, 30.0F);
            if (confirm) {
               c.rect(dx, y + 16.0F, 74.0F, 30.0F, 10.0F, Math.round(hv * 255.0F) << 24 | 15026253);
               c.text("inter-sb", 12.5F, dx + 37.0F, y + 31.5F, 18, white(hv), "Remove?");
            } else {
               c.rect(dx, y + 16.0F, 30.0F, 30.0F, 10.0F, white((od ? 0.12F : 0.05F) * hv));
               float tx = dx + 15.0F;
               float ty = y + 31.0F;
               c.line(tx - 5.0F, ty - 5.0F, tx + 5.0F, ty + 5.0F, 1.5F, white((od ? 0.95F : 0.55F) * hv));
               c.line(tx + 5.0F, ty - 5.0F, tx - 5.0F, ty + 5.0F, 1.5F, white((od ? 0.95F : 0.55F) * hv));
            }

            float ex = dx - 8.0F - 30.0F;
            boolean oe = this.over(ex, y + 16.0F, 30.0F, 30.0F);
            c.rect(ex, y + 16.0F, 30.0F, 30.0F, 10.0F, white((oe ? 0.12F : 0.05F) * hv));
            float ea = (oe ? 0.95F : 0.55F) * hv;
            c.line(ex + 10.0F, y + 36.0F, ex + 20.0F, y + 26.0F, 1.6F, white(ea));
            c.line(ex + 9.0F, y + 37.0F, ex + 10.5F, y + 35.5F, 2.2F, white(ea));
            actionsW += (confirm ? 74.0F : 30.0F) + 8.0F + 30.0F + 8.0F;
         }
      }

      float infoA = 1.0F - hv * (r.playable ? 1.0F : 0.0F);
      float textRight = bx - Math.max(actionsW * hv, 0.0F);
      if (infoA > 0.02F) {
         if (this.shownPage == 2) {
            this.drawPing(c, bx - 18.0F, y + 22.0F, r.status, infoA);
            if (!r.right.isEmpty()) {
               c.text("inter-med", 12.5F, bx - 30.0F, y + 22.5F, 20, white(0.62F * infoA), r.right);
            }

            if (!r.rightSub.isEmpty()) {
               c.text("inter", 11.5F, bx, y + 41.0F, 20, white(0.36F * infoA), r.rightSub);
            }
         } else {
            if (!r.right.isEmpty()) {
               c.text("inter-med", 12.0F, bx, y + 22.5F, 20, white(0.55F * infoA), r.right);
            }

            if (!r.rightSub.isEmpty()) {
               c.text("inter", 11.5F, bx, y + 41.0F, 20, white(0.34F * infoA), r.rightSub);
            }
         }
      }

      float infoW = 0.0F;
      if (infoA > 0.02F) {
         infoW = Math.max(c.textWidth("inter-med", 12.5F, r.right) + (this.shownPage == 2 ? 30.0F : 0.0F), c.textWidth("inter", 11.5F, r.rightSub));
      }

      float tx0 = ix + is + 14.0F;
      float maxW = Math.min(textRight, bx - infoW * infoA) - tx0 - 14.0F;
      c.text("inter-med", 15.0F, tx0, y + 22.5F, 17, white(r.playable ? 0.95F : 0.55F), ellipsize(c, "inter-med", 15.0F, r.title, maxW));
      c.text("inter", 12.5F, tx0, y + 41.0F, 17, white(0.42F), ellipsize(c, "inter", 12.5F, r.sub, maxW));
   }

   private void drawPing(Canvas c, float x, float y, int status, float a) {
      if (status != 0) {
         if (status < 0) {
            int col = status == -1 ? 15026253 : 16098596;
            c.circle(x + 5.0F, y, 3.5F, Math.round(a * 220.0F) << 24 | col);
         } else {
            for (int i = 0; i < 5; i++) {
               float bh = 3.0F + i * 2.0F;
               boolean on = i < status;
               c.rect(x - 4.0F + i * 3.4F, y + 5.0F - bh, 2.2F, bh, 1.0F, white((on ? 0.85F : 0.18F) * a));
            }
         }
      } else {
         float t = MenuRenderer.time() * 3.0F;

         for (int i = 0; i < 3; i++) {
            float k = 0.5F + 0.5F * (float)Math.sin(t - i * 0.7F);
            c.circle(x + i * 5.0F, y, 1.6F, white((0.25F + 0.5F * k) * a));
         }
      }
   }

   private List<MainMenuScreen.Row> rows() {
      List<MainMenuScreen.Row> out = new ArrayList<>();
      String q = this.query.toLowerCase(Locale.ROOT);
      if (this.shownPage == 1) {
         List<LevelSummary> ws = this.worlds;
         if (ws == null) {
            return out;
         }

         for (LevelSummary s : ws) {
            String name = s.getLevelName();
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q) || s.getLevelId().toLowerCase(Locale.ROOT).contains(q)) {
               String mode;
               try {
                  mode = s.isHardcore() ? "Hardcore" : s.getGameMode().getShortDisplayName().getString();
               } catch (Throwable t) {
                  mode = "";
               }

               String sub = mode + (s.hasCommands() ? "  ·  Cheats" : "");

               String ver;
               try {
                  ver = s.getWorldVersionName().getString();
               } catch (Throwable t) {
                  ver = "";
               }

               boolean playable = !s.isLocked() && !s.isDisabled() && s.primaryActionActive();
               if (!playable) {
                  try {
                     sub = s.getInfo().getString();
                  } catch (Throwable var12) {
                  }
               }

               out.add(new MainMenuScreen.Row("w:" + s.getLevelId(), name, sub, s.getLastPlayed() > 0L ? ago(s.getLastPlayed()) : "", ver, 0, this.worldIcons.get(s.getLevelId()), playable, s));
            }
         }
      } else if (this.servers != null) {
         for (int i = 0; i < this.servers.size(); i++) {
            ServerData d = this.servers.get(i);
            if (q.isEmpty() || d.name.toLowerCase(Locale.ROOT).contains(q) || d.ip.toLowerCase(Locale.ROOT).contains(q)) {
               this.ensurePinged(d);
               String right = "";
               String rightSub = "";
               int st;
               switch (d.state()) {
                  case SUCCESSFUL:
                     long p = d.ping;
                     st = p < 0L ? 1 : (p < 150L ? 5 : (p < 300L ? 4 : (p < 600L ? 3 : (p < 1000L ? 2 : 1))));
                     if (d.players != null) {
                        right = String.format(Locale.ROOT, "%,d/%,d", d.players.online(), d.players.max());
                     }

                     if (p >= 0L) {
                        rightSub = p + " ms";
                     }
                     break;
                  case UNREACHABLE:
                     st = -1;
                     rightSub = "Offline";
                     break;
                  case INCOMPATIBLE:
                     st = -2;
                     rightSub = d.version != null ? d.version.getString() : "Incompatible";
                     break;
                  default:
                     st = 0;
               }

               String motd = d.motd != null ? d.motd.getString().replace('\n', ' ').replaceAll("\\s{2,}", " ").trim() : "";
               if (motd.isEmpty()) {
                  motd = d.ip;
               }

               out.add(new MainMenuScreen.Row("s:" + i + ":" + d.ip, d.name, motd, right, rightSub, st, d.getIconBytes(), true, d));
            }
         }
      }

      return out;
   }

   private void openPage(int p) {
      if (this.page == p) {
         this.page = 0;
      } else {
         this.page = p;
         this.query = "";
         this.scroll = this.scrollTarget = 0.0F;
         this.selected = null;
         this.confirmRemove = null;
         if (p == 1) {
            this.loadWorlds();
         }

         if (p == 2) {
            this.loadServers();
         }
      }
   }

   private void loadWorlds() {
      if (!this.worldsLoading) {
         this.worldsLoading = true;
         this.worldsError = null;
         Minecraft mc = Minecraft.getInstance();

         try {
            LevelStorageSource source = mc.getLevelSource();
            source.loadLevelSummaries(source.findLevelCandidates()).whenComplete((list, err) -> {
               if (err == null && list != null) {
                  List<LevelSummary> sorted = new ArrayList<>(list);
                  Collections.sort(sorted);

                  for (LevelSummary s : sorted) {
                     try {
                        Path icon = s.getIcon();
                        if (icon != null && Files.isRegularFile(icon) && Files.size(icon) < 524288L) {
                           this.worldIcons.put(s.getLevelId(), Files.readAllBytes(icon));
                        }
                     } catch (Throwable var7) {
                     }
                  }

                  this.worlds = sorted;
               } else {
                  this.worldsError = "Couldn't read your saves folder";
                  this.worlds = List.of();
               }

               this.worldsLoading = false;
            });
         } catch (Throwable t) {
            this.worldsError = "Couldn't read your saves folder";
            this.worlds = List.of();
            this.worldsLoading = false;
         }
      }
   }

   private void loadServers() {
      try {
         this.pinger.removeAll();
      } catch (Throwable var2) {
      }

      this.servers = new ServerList(Minecraft.getInstance());
      this.servers.load();
   }

   private void ensurePinged(ServerData d) {
      if (d.state() == State.INITIAL) {
         d.setState(State.PINGING);
         d.motd = Component.empty();
         d.status = Component.empty();
         Minecraft mc = Minecraft.getInstance();
         ServerList list = this.servers;
         PING_POOL.submit(() -> {
            try {
               this.pinger.pingServer(d, () -> mc.execute(() -> {
                  if (list == this.servers) {
                     list.save();
                  }
               }), () -> {}, EventLoopGroupHolder.remote(mc.options.useNativeTransport()));
            } catch (UnknownHostException e) {
               d.setState(State.UNREACHABLE);
               d.motd = Component.literal("Can't resolve hostname");
            } catch (Throwable e) {
               d.setState(State.UNREACHABLE);
               d.motd = Component.literal("Can't connect to server");
            }
         });
      }
   }

   private void play(MainMenuScreen.Row r) {
      Minecraft mc = Minecraft.getInstance();
      playClick(mc);
      if (r.ref instanceof LevelSummary s) {
         if (!r.playable) {
            return;
         }

         if (s.shouldBackup() || s.requiresManualConversion() || s.isDowngrade() || !s.isCompatible() || s.requiresFileFixing()) {
            mc.setScreen(new SelectWorldScreen(this));
            return;
         }

         mc.createWorldOpenFlows().openWorld(s.getLevelId(), () -> mc.setScreen(this));
      } else if (r.ref instanceof ServerData d) {
         try {
            this.pinger.removeAll();
         } catch (Throwable var6) {
         }

         ConnectScreen.startConnecting(this, mc, ServerAddress.parseString(d.ip), d, false, null);
      }
   }

   private void footer(int i) {
      Minecraft mc = Minecraft.getInstance();
      playClick(mc);
      if (this.shownPage == 1) {
         if (i == 0) {
            CreateWorldScreen.openFresh(mc, () -> mc.setScreen(this));
         } else {
            mc.setScreen(new SelectWorldScreen(this));
         }

         this.worlds = null;
      } else {
         switch (i) {
            case 0: {
               ServerData d = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), "", Type.OTHER);
               mc.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), ok -> {
                  if (ok && this.servers != null) {
                     ServerData hidden = this.servers.unhide(d.ip);
                     if (hidden != null) {
                        hidden.copyNameIconFrom(d);
                     } else {
                        this.servers.add(d, false);
                     }

                     this.servers.save();
                  }

                  mc.setScreen(this);
               }, d));
               break;
            }
            case 1: {
               ServerData d = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), "", Type.OTHER);
               mc.setScreen(new DirectJoinServerScreen(this, ok -> {
                  if (ok) {
                     ConnectScreen.startConnecting(this, mc, ServerAddress.parseString(d.ip), d, false, null);
                  } else {
                     mc.setScreen(this);
                  }
               }, d));
               break;
            }
            case 2:
               this.loadServers();
               break;
            default:
               mc.setScreen(new JoinMultiplayerScreen(this));
         }
      }
   }

   private void editServer(ServerData d) {
      Minecraft mc = Minecraft.getInstance();
      ServerData edit = new ServerData(d.name, d.ip, Type.OTHER);
      edit.copyFrom(d);
      mc.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), ok -> {
         if (ok && this.servers != null) {
            d.name = edit.name;
            d.ip = edit.ip;
            d.copyFrom(edit);
            d.setState(State.INITIAL);
            this.servers.save();
         }

         mc.setScreen(this);
      }, edit));
   }

   @Override
   protected void click(float x, float y) {
      Minecraft mc = Minecraft.getInstance();
      if (layout() == 1 || this.pageA < 0.5F) {
         float bw = this.buttonW();

         for (int i = 0; i < LABELS.length; i++) {
            if (this.over(this.buttonX(i), this.buttonY(i), bw, 50.0F)) {
               playClick(mc);
               boolean inline = MainMenu.inlineLists();
               switch (i) {
                  case 0:
                     if (inline) {
                        this.openPage(1);
                     } else {
                        mc.setScreen(new SelectWorldScreen(this));
                     }
                     break;
                  case 1:
                     if (inline) {
                        this.openPage(2);
                     } else {
                        mc.setScreen(new JoinMultiplayerScreen(this));
                     }
                     break;
                  case 2:
                     mc.setScreen(new OptionsScreen(this, mc.options, false));
                     break;
                  case 3:
                     mc.setScreen(new AltManagerScreen(this));
               }

               return;
            }
         }
      }

      if (this.over(this.w - 58.0F, 22.0F, 36.0F, 36.0F)) {
         playClick(mc);
         mc.stop();
      } else if (this.page != 0 && !(this.pageA < 0.6F)) {
         float px = this.panelX();
         float py = this.panelY();
         float pw = this.panelW();
         float ph = this.panelH();
         if (this.over(px, py, pw, ph)) {
            float cx = px + pw - 28.0F - 17.0F;
            float cy = py + 41.0F;
            if (this.over(cx - 17.0F, cy - 17.0F, 34.0F, 34.0F)) {
               playClick(mc);
               this.page = 0;
            } else {
               String[] fb = this.footerLabels();
               float fy = py + ph - 52.0F;
               float fx = px + 28.0F;

               for (int i = 0; i < fb.length; i++) {
                  float bw = this.footW[i] > 0.0F ? this.footW[i] : 110.0F;
                  if (this.over(fx, fy, bw, 34.0F)) {
                     this.footer(i);
                     return;
                  }

                  fx += bw + 10.0F;
               }

               float top = this.listTop();
               float bot = this.listBot();
               if (!(y < top) && !(y > bot)) {
                  List<MainMenuScreen.Row> rows = this.rows();
                  float lx = px + 16.0F;
                  float lw = pw - 32.0F;
                  float bx = lx + lw - 12.0F;

                  for (int i = 0; i < rows.size(); i++) {
                     MainMenuScreen.Row r = rows.get(i);
                     float ry = top + i * 68.0F - this.scroll;
                     if (this.over(lx, ry, lw, 62.0F)) {
                        float pbx = bx - 64.0F;
                        if (r.playable && this.over(pbx, ry + 15.0F, 64.0F, 32.0F)) {
                           this.play(r);
                           return;
                        }

                        if (r.ref instanceof ServerData d) {
                           boolean confirm = r.key.equals(this.confirmRemove);
                           float dw = confirm ? 74.0F : 30.0F;
                           float dx = pbx - 8.0F - dw;
                           if (this.over(dx, ry + 16.0F, dw, 30.0F)) {
                              playClick(mc);
                              if (confirm) {
                                 this.servers.remove(d);
                                 this.servers.save();
                                 this.confirmRemove = null;
                              } else {
                                 this.confirmRemove = r.key;
                              }

                              return;
                           }

                           if (this.over(dx - 38.0F, ry + 16.0F, 30.0F, 30.0F)) {
                              playClick(mc);
                              this.editServer(d);
                              return;
                           }
                        }

                        this.confirmRemove = null;
                        long now = System.currentTimeMillis();
                        if (r.key.equals(this.lastClickKey) && now - this.lastClickAt < 350L) {
                           this.play(r);
                           this.lastClickKey = null;
                           return;
                        }

                        this.lastClickKey = r.key;
                        this.lastClickAt = now;
                        this.selected = r.key;
                        return;
                     }
                  }

                  this.confirmRemove = null;
               }
            }
         } else {
            if (layout() != 1 || x > this.leftX() + 300.0F) {
               this.page = 0;
            }
         }
      }
   }

   public boolean mouseScrolled(double x, double y, double hAmount, double vAmount) {
      if (this.page != 0) {
         this.scrollTarget -= (float)vAmount * 52.0F;
      }

      return true;
   }

   public boolean keyPressed(KeyEvent e) {
      int k = e.key();
      if (this.page == 0) {
         return super.keyPressed(e);
      }

      if (k == 256) {
         if (!this.query.isEmpty()) {
            this.query = "";
         } else {
            this.page = 0;
         }

         return true;
      } else if (k == 259) {
         if (!this.query.isEmpty()) {
            this.query = this.query.substring(0, this.query.length() - 1);
         }

         this.scrollTarget = 0.0F;
         return true;
      } else if (k != 257 && k != 335) {
         if (k != 264 && k != 265) {
            return true;
         }

         List<MainMenuScreen.Row> rows = this.rows();
         if (rows.isEmpty()) {
            return true;
         }

         int idx = -1;

         for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).key.equals(this.selected)) {
               idx = i;
            }
         }

         idx = Math.max(0, Math.min(rows.size() - 1, idx + (k == 264 ? 1 : -1)));
         this.selected = rows.get(idx).key;
         float ry = idx * 68.0F;
         float view = this.listBot() - this.listTop();
         if (ry < this.scrollTarget) {
            this.scrollTarget = ry;
         }

         if (ry + 62.0F > this.scrollTarget + view) {
            this.scrollTarget = ry + 62.0F - view;
         }

         return true;
      } else {
         List<MainMenuScreen.Row> rows = this.rows();
         MainMenuScreen.Row pick = null;

         for (MainMenuScreen.Row r : rows) {
            if (r.key.equals(this.selected)) {
               pick = r;
            }
         }

         if (pick == null && !rows.isEmpty()) {
            pick = rows.get(0);
         }

         if (pick != null && pick.playable) {
            this.play(pick);
         }

         return true;
      }
   }

   public boolean charTyped(CharacterEvent e) {
      if (this.page == 0) {
         return false;
      } else {
         String s = e.codepointAsString();
         if (!s.isEmpty() && !Character.isISOControl(s.charAt(0)) && this.query.length() < 40) {
            this.query = this.query + s;
            this.scrollTarget = 0.0F;
            return true;
         } else {
            return true;
         }
      }
   }

   public void removed() {
      try {
         this.pinger.removeAll();
      } catch (Throwable var2) {
      }
   }

   private static String count(int n, String what) {
      return n + " " + what + (n == 1 ? "" : "s");
   }

   private static String ellipsize(Canvas c, String font, float size, String s, float maxW) {
      if (s == null) {
         return "";
      }

      if (maxW <= 12.0F) {
         return "";
      }

      if (c.textWidth(font, size, s) <= maxW) {
         return s;
      }

      int lo = 0;
      int hi = s.length();

      while (lo < hi) {
         int mid = (lo + hi + 1) / 2;
         if (c.textWidth(font, size, s.substring(0, mid) + "…") <= maxW) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      return s.substring(0, lo).trim() + "…";
   }

   private static String ago(long millis) {
      long s = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
      if (s < 60L) {
         return "Just now";
      } else if (s < 3600L) {
         return s / 60L + "m ago";
      } else if (s < 86400L) {
         return s / 3600L + "h ago";
      } else {
         return s < 2592000L ? s / 86400L + "d ago" : new SimpleDateFormat("d MMM yyyy").format(new Date(millis));
      }
   }

   private static void spinner(Canvas c, float cx, float cy, int col) {
      float t = MenuRenderer.time() * 5.0F;
      c.arc(cx, cy - 34.0F, 9.0F, t, t + 4.2F, 1.8F, col);
   }

   private static String version() {
      try {
         return FabricLoader.getInstance().getModContainer("clarity").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
      } catch (Throwable t) {
         return "";
      }
   }

   private static void playClick(Minecraft mc) {
      if (UiSounds.uiOn()) {
         UiSounds.play(UiSounds.Sound.CLICK);
      } else {
         try {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         } catch (Throwable var2) {
         }
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private record Row(String key, String title, String sub, String right, String rightSub, int status, byte[] icon, boolean playable, Object ref) {
   }
}
