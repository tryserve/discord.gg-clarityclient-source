package need.clarity.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import need.clarity.alts.AltStore;
import need.clarity.alts.MicrosoftAuth;
import need.clarity.alts.SessionSwapper;
import need.clarity.clickgui.Canvas;
import need.clarity.modules.visual.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

public final class AltManagerScreen extends MenuScreen {
   private static final float PANEL_W = 560.0F;
   private static final float ROW_H = 64.0F;
   private static final float ROW_GAP = 8.0F;
   private final Screen parent;
   private volatile String status = "";
   private volatile boolean statusError = false;
   private volatile boolean busy = false;
   private volatile String busyUuid = null;
   private long statusAt = 0L;
   private float scroll = 0.0F;
   private float scrollTarget = 0.0F;
   private String confirmRemove = null;
   private final Map<String, float[]> rowHover = new HashMap<>();
   private int subVersion = -1;
   private String subCurrent = null;
   private String subtitle = "";
   private float addHover = 0.0F;
   private float backHover = 0.0F;

   public AltManagerScreen(Screen parent) {
      super("Alt Manager");
      this.parent = parent;
   }

   private float px() {
      return (this.w - 560.0F) / 2.0F;
   }

   private float headerY() {
      return Math.max(70.0F, this.h * 0.14F);
   }

   private float listTop() {
      return this.headerY() + 120.0F;
   }

   private float listBot() {
      return this.h - 170.0F;
   }

   private float addY() {
      return this.listBot() + 30.0F;
   }

   @Override
   protected void draw(Canvas c, float w, float h) {
      float in = easeOut(this.age() / 0.45F);
      c.save();
      c.alpha(in);
      c.translate(0.0F, (1.0F - in) * 10.0F);
      float x = this.px();
      float hy = this.headerY();
      String current = Minecraft.getInstance().getUser() != null ? Minecraft.getInstance().getUser().getName() : "";
      this.backHover = this.approach(this.backHover, this.over(x - 6.0F, hy - 4.0F, 90.0F, 32.0F) ? 1.0F : 0.0F, 14.0F);
      float ba = 0.45F + 0.5F * this.backHover;
      c.line(x + 2.0F, hy + 12.0F, x + 9.0F, hy + 5.0F, 1.6F, white(ba));
      c.line(x + 2.0F, hy + 12.0F, x + 9.0F, hy + 19.0F, 1.6F, white(ba));
      c.text("inter-med", 14.0F, x + 18.0F, hy + 12.5F, 17, white(ba), "Back");
      c.text("inter-sb", 30.0F, x, hy + 62.0F, 17, white(1.0F), "Accounts");
      List<AltStore.Alt> alts = AltStore.all();
      if (this.subVersion != AltStore.version() || !current.equals(this.subCurrent)) {
         this.subVersion = AltStore.version();
         this.subCurrent = current;
         this.subtitle = alts.size() + (alts.size() == 1 ? " saved account" : " saved accounts") + "  ·  playing as " + current;
      }

      c.text("inter", 13.5F, x, hy + 92.0F, 17, white(0.42F), this.subtitle);
      float top = this.listTop();
      float bot = this.listBot();
      float content = alts.size() * 72.0F;
      float maxScroll = Math.max(0.0F, content - (bot - top));
      this.scrollTarget = Math.max(0.0F, Math.min(maxScroll, this.scrollTarget));
      this.scroll = this.approach(this.scroll, this.scrollTarget, 18.0F);
      c.save();
      c.scissor(x - 20.0F, top - 2.0F, 600.0F, bot - top + 4.0F);
      if (alts.isEmpty()) {
         float cy = (top + bot) / 2.0F;
         c.ring(w / 2.0F, cy - 26.0F, 18.0F, 1.2F, white(0.18F));
         c.line(w / 2.0F - 7.0F, cy - 26.0F, w / 2.0F + 7.0F, cy - 26.0F, 1.4F, white(0.35F));
         c.line(w / 2.0F, cy - 33.0F, w / 2.0F, cy - 19.0F, 1.4F, white(0.35F));
         c.text("inter-med", 15.0F, w / 2.0F, cy + 14.0F, 18, white(0.7F), "No accounts yet");
         c.text("inter", 12.5F, w / 2.0F, cy + 36.0F, 18, white(0.35F), "Add one and it signs in through your browser.");
      }

      for (int i = 0; i < alts.size(); i++) {
         AltStore.Alt a = alts.get(i);
         float ry = top + i * 72.0F - this.scroll;
         if (!(ry + 64.0F < top - 4.0F) && !(ry > bot + 4.0F)) {
            this.drawRow(c, a, x, ry, a.name.equalsIgnoreCase(current));
         }
      }

      c.restore();
      if (this.scroll > 1.0F) {
         c.gradient(x - 20.0F, top - 2.0F, 600.0F, 22.0F, 0.0F, black(0.85F), black(0.0F), false);
      }

      if (this.scroll < maxScroll - 1.0F) {
         c.gradient(x - 20.0F, bot - 20.0F, 600.0F, 22.0F, 0.0F, black(0.0F), black(0.85F), false);
      }

      float ay = this.addY();
      boolean overAdd = this.over(x, ay, 560.0F, 52.0F);
      this.addHover = this.approach(this.addHover, overAdd ? 1.0F : 0.0F, 14.0F);
      boolean loggingIn = this.busy && this.busyUuid == null;
      if (loggingIn) {
         c.rect(x, ay, 560.0F, 52.0F, 14.0F, white(0.06F + 0.05F * this.addHover));
         c.stroke(x + 0.5F, ay + 0.5F, 559.0F, 51.0F, 14.0F, 1.0F, white(0.2F));
         c.text("inter-med", 15.5F, w / 2.0F, ay + 26.0F, 18, white(0.85F), "Cancel");
      } else {
         c.shadow(x, ay, 560.0F, 52.0F, 14.0F, 22.0F, white(0.1F + 0.1F * this.addHover));
         c.rect(x, ay, 560.0F, 52.0F, 14.0F, white(0.94F + 0.06F * this.addHover));
         c.line(w / 2.0F - 64.0F, ay + 26.0F, w / 2.0F - 52.0F, ay + 26.0F, 1.8F, black(0.9F));
         c.line(w / 2.0F - 58.0F, ay + 20.0F, w / 2.0F - 58.0F, ay + 32.0F, 1.8F, black(0.9F));
         c.text("inter-sb", 15.5F, w / 2.0F + 8.0F, ay + 26.5F, 18, black(0.92F), "Add account");
      }

      String s = this.status;
      if (!s.isEmpty()) {
         float sy = ay + 82.0F;
         float sa = this.statusError ? 1.0F : 0.55F;
         if (!this.busy && System.currentTimeMillis() - this.statusAt > 6000L) {
            sa *= Math.max(0.0F, 1.0F - (float)(System.currentTimeMillis() - this.statusAt - 6000L) / 800.0F);
         }

         float tw = c.textWidth("inter", 13.0F, s);
         if (this.busy) {
            spinner(c, w / 2.0F - tw / 2.0F - 14.0F, sy, white(0.7F));
         }

         c.text("inter", 13.0F, w / 2.0F + (this.busy ? 6.0F : 0.0F), sy, 18, white(sa), s);
      }

      c.restore();
   }

   private void drawRow(Canvas c, AltStore.Alt a, float x, float y, boolean active) {
      boolean overRow = this.over(x, y, 560.0F, 64.0F) && this.my >= this.listTop() && this.my <= this.listBot();
      float[] slot = this.rowHover.computeIfAbsent(a.uuid, k -> new float[1]);
      float hv = slot[0] = this.approach(slot[0], overRow ? 1.0F : 0.0F, 14.0F);
      c.rect(x, y, 560.0F, 64.0F, 14.0F, white(0.035F + 0.04F * hv + (active ? 0.025F : 0.0F)));
      c.stroke(x + 0.5F, y + 0.5F, 559.0F, 63.0F, 14.0F, 1.0F, white(active ? 0.28F : 0.07F + 0.1F * hv));
      float ax = x + 36.0F;
      float ay = y + 32.0F;
      c.circle(ax, ay, 18.0F, white(active ? 0.95F : 0.1F));
      c.text("inter-sb", 15.0F, ax, ay + 0.5F, 18, active ? black(0.9F) : white(0.85F), a.name.isEmpty() ? "?" : a.name.substring(0, 1).toUpperCase());
      c.text("inter-med", 16.0F, x + 68.0F, ay - 9.0F, 17, white(0.95F), a.name);
      String sub = active ? "Active" : (a.lastUsed > 0L ? "Used " + ago(a.lastUsed) : "Never used");
      c.text("inter", 12.0F, x + 68.0F, ay + 11.0F, 17, white(active ? 0.6F : 0.35F), sub);
      if (active) {
         c.glow(x + 68.0F + c.textWidth("inter", 12.0F, sub) + 10.0F, ay + 11.0F, 7.0F, white(0.8F));
      }

      float rx = x + 560.0F - 24.0F;
      if (this.busy && a.uuid.equals(this.busyUuid)) {
         spinner(c, rx - 6.0F, ay, white(0.8F));
      } else if (this.confirmRemove != null && this.confirmRemove.equals(a.uuid)) {
         float bw = 82.0F;
         c.rect(rx - bw + 6.0F, ay - 15.0F, bw, 30.0F, 9.0F, white(0.92F));
         c.text("inter-sb", 12.5F, rx - bw / 2.0F + 6.0F, ay + 0.5F, 18, black(0.9F), "Remove");
      } else if (hv > 0.02F) {
         boolean overX = this.over(rx - 16.0F, ay - 16.0F, 32.0F, 32.0F);
         float xa = (overX ? 0.9F : 0.4F) * hv;
         c.line(rx - 11.0F, ay - 5.0F, rx - 1.0F, ay + 5.0F, 1.5F, white(xa));
         c.line(rx - 1.0F, ay - 5.0F, rx - 11.0F, ay + 5.0F, 1.5F, white(xa));
         if (!active) {
            c.text("inter-med", 12.5F, rx - 36.0F, ay + 0.5F, 20, white(0.55F * hv), "Switch");
         }
      }
   }

   private static void spinner(Canvas c, float cx, float cy, int col) {
      float t = MenuRenderer.time() * 5.0F;
      c.arc(cx, cy, 7.0F, t, t + 4.2F, 1.6F, col);
   }

   private static String ago(long millis) {
      long s = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
      if (s < 60L) {
         return "just now";
      } else if (s < 3600L) {
         return s / 60L + "m ago";
      } else {
         return s < 86400L ? s / 3600L + "h ago" : s / 86400L + "d ago";
      }
   }

   private void setStatus(String s, boolean error) {
      this.status = s;
      this.statusError = error;
      this.statusAt = System.currentTimeMillis();
   }

   private void addAccount() {
      this.busy = true;
      this.busyUuid = null;
      this.setStatus("Opening browser…", false);
      MicrosoftAuth.browserLogin(MainMenu.clientId(), msg -> this.setStatus(msg, false)).whenComplete((alt, err) -> {
         if (err != null) {
            Throwable e = err.getCause() != null ? err.getCause() : err;
            if (!"Cancelled".equals(e.getMessage())) {
               this.busy = false;
               this.setStatus(e.getMessage() == null ? "Sign-in failed" : e.getMessage(), true);
            }
         } else {
            AltStore.put(alt);
            this.switchTo(alt, true);
         }
      });
   }

   private void switchTo(AltStore.Alt alt, boolean alreadyFresh) {
      this.busy = true;
      this.busyUuid = alt.uuid;
      Thread t = new Thread(() -> {
         try {
            AltStore.Alt fresh = alreadyFresh ? alt : MicrosoftAuth.ensureFresh(alt, m -> this.setStatus(m, false));
            fresh.lastUsed = System.currentTimeMillis();
            AltStore.put(fresh);
            Minecraft.getInstance().execute(() -> {
               try {
                  SessionSwapper.apply(fresh);
                  this.setStatus("Now playing as " + fresh.name, false);
               } catch (Throwable ex) {
                  this.setStatus("Couldn't switch session: " + ex.getMessage(), true);
               } finally {
                  this.busy = false;
                  this.busyUuid = null;
               }
            });
         } catch (Throwable e) {
            this.busy = false;
            this.busyUuid = null;
            this.setStatus(e.getMessage() == null ? "Couldn't refresh this account" : e.getMessage(), true);
         }
      }, "clarity-alt-switch");
      t.setDaemon(true);
      t.start();
   }

   @Override
   protected void click(float x, float y) {
      float px = this.px();
      float hy = this.headerY();
      if (this.over(px - 6.0F, hy - 4.0F, 90.0F, 32.0F)) {
         this.onClose();
      } else if (!this.over(px, this.addY(), 560.0F, 52.0F)) {
         if (!(y < this.listTop()) && !(y > this.listBot())) {
            List<AltStore.Alt> alts = AltStore.all();
            String current = Minecraft.getInstance().getUser() != null ? Minecraft.getInstance().getUser().getName() : "";

            for (int i = 0; i < alts.size(); i++) {
               AltStore.Alt a = alts.get(i);
               float ry = this.listTop() + i * 72.0F - this.scroll;
               if (this.over(px, ry, 560.0F, 64.0F)) {
                  float rx = px + 560.0F - 24.0F;
                  float ay = ry + 32.0F;
                  if (a.uuid.equals(this.confirmRemove)) {
                     if (this.over(rx - 76.0F, ay - 15.0F, 82.0F, 30.0F)) {
                        AltStore.remove(a.uuid);
                        this.setStatus("Removed " + a.name, false);
                     }

                     this.confirmRemove = null;
                     return;
                  }

                  if (this.over(rx - 16.0F, ay - 16.0F, 32.0F, 32.0F)) {
                     this.confirmRemove = a.uuid;
                     return;
                  }

                  this.confirmRemove = null;
                  if (!this.busy && !a.name.equalsIgnoreCase(current)) {
                     this.switchTo(a, false);
                     return;
                  }

                  return;
               }
            }

            this.confirmRemove = null;
         } else {
            this.confirmRemove = null;
         }
      } else {
         if (this.busy && this.busyUuid == null) {
            MicrosoftAuth.cancel();
            this.busy = false;
            this.setStatus("Cancelled", false);
         } else if (!this.busy) {
            this.addAccount();
         }
      }
   }

   public boolean mouseScrolled(double x, double y, double hAmount, double vAmount) {
      this.scrollTarget -= (float)vAmount * 48.0F;
      return true;
   }

   public boolean keyPressed(KeyEvent e) {
      if (e.key() == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(e);
      }
   }

   public void onClose() {
      if (this.busy && this.busyUuid == null) {
         MicrosoftAuth.cancel();
      }

      Minecraft.getInstance().setScreen(this.parent);
   }
}
