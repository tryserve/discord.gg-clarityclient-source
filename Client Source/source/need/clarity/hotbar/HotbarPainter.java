package need.clarity.hotbar;

import java.util.ArrayList;
import need.clarity.clickgui.Canvas;

public final class HotbarPainter {
   public static final int RW = 260;
   public static final int RH = 64;
   public static final int CX = 130;
   private static final float DOCK_X = 39.0F;
   private static final float DOCK_W = 182.0F;
   private static final float DOCK_H = 22.0F;
   private static final float DOCK_Y = 40.0F;
   private static final float TILE = 18.0F;
   private static final float TILE_Y = 42.0F;
   public boolean icons = false;
   public int accent = -8740105;
   public int surface = -15328474;
   public float bgOpacity = 0.72F;
   public float rounding = 5.0F;
   public boolean slotNumbers = false;
   public boolean numbers = true;
   public boolean trail = true;
   public boolean pulse = true;
   public boolean smooth = true;
   public int healthCol = -45730;
   public int absorbCol = -14016;
   public int hungerCol = -875718;
   public int armorCol = -5651224;
   public int airCol = -10761985;
   public int xpCol = -8592549;
   public int mountCol = -1538214;
   private float time;
   private long lastNanos;
   private float selX = 0.0F;
   private int lastSelected = -1;
   private int selTo;
   private float selFrom;
   private long selStart;
   private int selFrames;
   private float dispHealth = -1.0F;
   private float trailHealth = -1.0F;
   private float trailHold;
   private float healFlash;
   private float lastHealth = -1.0F;
   private float dispAbs = -1.0F;
   private float dispFood = -1.0F;
   private float dispSat = -1.0F;
   private float dispXp = -1.0F;
   private float dispAir = -1.0F;
   private float dispMount = -1.0F;
   private float trailMount = -1.0F;
   private float armorA;
   private float airA;
   private float mountA;
   private float jumpA;
   private int lastLevel = -1;
   private float levelFlash;
   private boolean animating = true;
   private boolean unsettled;
   private boolean fading;
   private static final float BAR_W = 85.0F;
   private static final float BAR_H = 4.0F;
   private static final float BAR_Y = 30.5F;
   private static final float LABEL_Y = 26.0F;
   private static final float L_X = 39.0F;
   private static final float R_X = 136.0F;
   static final float[] HEART;
   static final float[] HEART_SHADE;
   static final float[] SHIELD;
   static final float[] SHIELD_HI;
   static final float[] MEAT;
   static final float[] MEAT_SHADE;

   public static int slotItemX(int i) {
      return 42 + 20 * i;
   }

   public static int itemY() {
      return 43;
   }

   public static int offhandItemX(boolean left) {
      return left ? 14 : 230;
   }

   private static float tileX(float i) {
      return 41.0F + 20.0F * i;
   }

   public void update(HotbarState s, long nowNanos) {
      float dt = this.lastNanos == 0L ? 0.016F : (float)(nowNanos - this.lastNanos) / 1.0E9F;
      if (!(dt > 0.0F) || Float.isInfinite(dt)) {
         dt = 0.016666668F;
      }

      dt = Math.min(0.1F, dt);
      this.lastNanos = nowNanos;
      this.time += dt;
      boolean anim = false;
      int sel = Math.max(0, Math.min(8, s.selected));
      if (this.lastSelected < 0 || !this.smooth) {
         this.lastSelected = sel;
         this.selFrom = sel;
         this.selTo = sel;
         this.selStart = nowNanos;
      } else if (sel != this.selTo) {
         this.selFrom = this.selX;
         this.selTo = sel;
         this.selStart = nowNanos;
         this.lastSelected = sel;
         this.selFrames = 0;
      }

      this.selFrames++;
      float k = Math.max(0.0F, Math.min(1.0F, (float)(nowNanos - this.selStart) / 1.4E8F));
      if (this.selFrames > 60) {
         k = 1.0F;
      }

      float e = 1.0F - (1.0F - k) * (1.0F - k) * (1.0F - k);
      this.selX = Math.max(0.0F, Math.min(8.0F, this.selFrom + (this.selTo - this.selFrom) * e));
      if (Float.isNaN(this.selX)) {
         this.selX = sel;
      }

      if (k < 1.0F) {
         anim = true;
      } else {
         this.selX = sel;
      }

      float h = s.health;
      if (this.dispHealth < 0.0F || !this.smooth) {
         this.dispHealth = h;
         this.trailHealth = h;
      }

      if (this.lastHealth >= 0.0F && h > this.lastHealth + 0.49F) {
         this.healFlash = 1.0F;
      }

      if (this.lastHealth >= 0.0F && h < this.lastHealth - 0.01F) {
         this.trailHold = 0.38F;
      }

      this.lastHealth = h;
      if (h < this.dispHealth) {
         this.dispHealth = h;
      } else {
         this.dispHealth = approach(this.dispHealth, h, dt, 12.0F);
      }

      if (this.trailHealth < this.dispHealth || !this.trail) {
         this.trailHealth = this.dispHealth;
      } else if (this.trailHold > 0.0F) {
         this.trailHold -= dt;
      } else {
         this.trailHealth = approach(this.trailHealth, this.dispHealth, dt, 5.0F);
      }

      if (!(Math.abs(this.dispHealth - h) > 0.01F) && !(this.trailHealth - this.dispHealth > 0.02F) && !(this.trailHold > 0.0F)) {
         this.dispHealth = h;
         this.trailHealth = h;
      } else {
         anim = true;
      }

      if (this.healFlash > 0.0F) {
         this.healFlash = Math.max(0.0F, this.healFlash - dt * 2.2F);
         anim = true;
      }

      this.dispAbs = this.settle(this.dispAbs, s.absorption, dt, 12.0F, 0.01F);
      this.dispFood = this.settle(this.dispFood, s.food, dt, 10.0F, 0.01F);
      this.dispSat = this.settle(this.dispSat, s.saturation, dt, 10.0F, 0.01F);
      this.dispAir = this.settle(this.dispAir, s.air, dt, 14.0F, 0.5F);
      this.dispMount = this.settle(this.dispMount, s.mount ? s.mountHealth : 0.0F, dt, 12.0F, 0.01F);
      if (!(this.trailMount < this.dispMount) && this.trail) {
         this.trailMount = approach(this.trailMount, this.dispMount, dt, 5.0F);
      } else {
         this.trailMount = this.dispMount;
      }

      if (s.level != this.lastLevel) {
         if (this.lastLevel >= 0 && s.level > this.lastLevel) {
            this.levelFlash = 1.0F;
         }

         this.dispXp = s.level > this.lastLevel && this.lastLevel >= 0 ? 0.0F : s.xp;
         this.lastLevel = s.level;
      }

      this.dispXp = this.settle(this.dispXp, s.xp, dt, 9.0F, 0.0015F);
      if (this.levelFlash > 0.0F) {
         this.levelFlash = Math.max(0.0F, this.levelFlash - dt * 1.6F);
         anim = true;
      }

      anim |= this.unsettled;
      this.unsettled = false;
      this.armorA = this.fade(this.armorA, s.armor > 0, dt);
      this.airA = this.fade(this.airA, s.airShown, dt);
      this.mountA = this.fade(this.mountA, s.mount, dt);
      this.jumpA = this.fade(this.jumpA, s.jump, dt);
      anim |= this.fading;
      this.fading = false;
      if (this.trailMount - this.dispMount > 0.02F) {
         anim = true;
      }

      if (s.status && s.survival) {
         if (this.pulse && lowHealth(s)) {
            anim = true;
         }

         if (s.regen) {
            anim = true;
         }
      }

      this.animating = anim;
   }

   public boolean animating() {
      return this.animating;
   }

   private float settle(float cur, float target, float dt, float rate, float eps) {
      if (!(cur < 0.0F) && this.smooth) {
         float v = approach(cur, target, dt, rate);
         if (Math.abs(v - target) <= eps) {
            return target;
         }

         this.unsettled = true;
         return v;
      } else {
         return target;
      }
   }

   private float fade(float cur, boolean on, float dt) {
      float target = on ? 1.0F : 0.0F;
      if (!this.smooth) {
         return target;
      }

      float v = approach(cur, target, dt, 10.0F);
      if (Math.abs(v - target) < 0.004F) {
         return target;
      }

      this.fading = true;
      return v;
   }

   private static float approach(float cur, float target, float dt, float rate) {
      float k = 1.0F - (float)Math.exp(-dt * rate);
      if (!(k >= 0.0F)) {
         k = 0.0F;
      }

      if (k > 1.0F) {
         k = 1.0F;
      }

      float v = cur + (target - cur) * k;
      return Float.isNaN(v) ? target : v;
   }

   private static boolean lowHealth(HotbarState s) {
      return s.health > 0.0F && (s.health <= 4.0F || s.health / Math.max(1.0F, s.maxHealth) <= 0.2F);
   }

   public void panels(Canvas c, HotbarState s) {
      if (s.hotbar) {
         this.panel(c, 39.0F, 40.0F, 182.0F, 22.0F, this.rounding);
         if (s.offhand.present) {
            this.panel(c, offhandBoxX(s.offhandLeft), 40.0F, 22.0F, 22.0F, this.rounding);
         }
      }
   }

   public void base(Canvas c, HotbarState s) {
      if (s.hotbar) {
         this.drawSlots(c, s);
      }

      if (s.status && s.survival) {
         if (this.icons) {
            this.drawIconStatus(c, s);
         } else {
            this.drawBarStatus(c, s);
         }
      }

      if (s.experience) {
         this.drawXp(c, s);
      }
   }

   public static float[] shaderRects(HotbarState s) {
      return new float[]{39.0F, 40.0F, 182.0F, 22.0F, offhandBoxX(s.offhandLeft), 40.0F, s.hotbar && s.offhand.present ? 22.0F : 0.0F, 22.0F};
   }

   private static float offhandBoxX(boolean left) {
      return left ? 11.0F : 227.0F;
   }

   private void panel(Canvas c, float x, float y, float w, float h, float r) {
      c.shadow(x, y + 1.0F, w, h, r, 4.0F, a(-16777216, 0.22F));
      c.rect(x, y, w, h, r, a(this.surface, this.bgOpacity));
      c.stroke(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, Math.max(0.0F, r - 0.5F), 1.0F, a(-1, 0.06F));
   }

   private void drawSlots(Canvas c, HotbarState s) {
      float tr = Math.max(0.0F, this.rounding - 2.0F);

      for (int i = 0; i < 9; i++) {
         float tx = tileX(i);
         c.rect(tx, 42.0F, 18.0F, 18.0F, tr, a(-1, 0.03F));
         if (this.slotNumbers) {
            c.text("inter-med", 4.4F, tx + 1.8F, 47.0F, 65, a(-1, i == s.selected ? 0.5F : 0.2F), String.valueOf(i + 1));
         }
      }

      float sx = tileX(this.selX);
      c.rect(sx - 0.5F, 41.5F, 19.0F, 19.0F, tr + 0.5F, a(this.accent, 0.14F));
      if (s.offhand.present) {
         c.rect(offhandBoxX(s.offhandLeft) + 2.0F, 42.0F, 18.0F, 18.0F, tr, a(-1, 0.03F));
      }
   }

   public void over(Canvas c, HotbarState s) {
      if (s.hotbar) {
         float tr = Math.max(0.0F, this.rounding - 2.0F);
         float sx = tileX(this.selX);
         c.stroke(sx - 0.5F, 41.5F, 19.0F, 19.0F, tr + 0.5F, 1.0F, a(this.accent, 0.85F));

         for (int i = 0; i < 9; i++) {
            this.decorate(c, s.slots[i], slotItemX(i), itemY());
         }

         if (s.offhand.present) {
            this.decorate(c, s.offhand, offhandItemX(s.offhandLeft), itemY());
         }
      }
   }

   private void decorate(Canvas c, HotbarState.Slot sl, float ix, float iy) {
      if (sl.present) {
         if (sl.cooldown > 0.0F) {
            float ch = 18.0F * sl.cooldown;
            c.save();
            c.scissor(ix - 1.0F, iy + 17.0F - ch, 18.0F, ch);
            c.rect(ix - 1.0F, iy - 1.0F, 18.0F, 18.0F, Math.max(0.0F, this.rounding - 2.0F), a(-1, 0.22F));
            c.restore();
         }

         if (sl.bar >= 0.0F) {
            float bw = 12.0F;
            c.rect(ix + 2.0F, iy + 14.3F, bw, 1.5F, 0.75F, a(-16777216, 0.7F));
            c.rect(ix + 2.0F, iy + 14.3F, Math.max(1.5F, bw * sl.bar), 1.5F, 0.75F, 0xFF000000 | sl.barColor);
         }

         if (sl.count > 1) {
            this.textShadow(c, "inter-sb", 6.6F, ix + 16.8F, iy + 16.4F, 68, -1, String.valueOf(sl.count));
         }
      }
   }

   private void drawBarStatus(Canvas c, HotbarState s) {
      float max = Math.max(1.0F, s.maxHealth);
      int hc = this.healthColour(s);
      float beat = this.pulse && lowHealth(s) ? heartbeat(this.time) : 0.0F;
      float fs = 6.4F;
      this.thinBar(c, 39.0F, 30.5F, 85.0F, this.dispHealth / max, this.trailHealth / max, hc, false, Math.max(0.0F, this.dispAbs) / max, this.absorbCol, beat, s.regen, this.healFlash);
      float lx = 40.0F;
      this.mark(c, HotbarPainter.Icon.HEART, lx + 3.0F, 26.0F, 6.0F * (1.0F + 0.12F * beat), hc, s.hardcore);
      lx += 8.0F;
      if (this.numbers) {
         lx += this.textShadow(c, "inter-sb", fs, lx, 26.0F, 17, -1, fmt(s.health)) + 2.0F;
         if (s.absorption > 0.01F) {
            lx += this.textShadow(c, "inter-sb", fs * 0.9F, lx, 26.0F, 17, this.absorbCol, "+" + fmt(s.absorption)) + 2.0F;
         }
      }

      if (this.armorA > 0.0F) {
         c.save();
         c.alpha(this.armorA);
         float ax = lx + 4.0F;
         this.mark(c, HotbarPainter.Icon.ARMOR, ax + 3.0F, 26.0F, 6.0F, this.armorCol, false);
         if (this.numbers) {
            this.textShadow(c, "inter-sb", fs, ax + 8.0F, 26.0F, 17, a(-1, 0.85F), String.valueOf(s.armor));
         }

         c.restore();
      }

      float rx = 220.0F;
      float labelEnd = rx - 10.0F;
      if (this.mountA < 1.0F) {
         c.save();
         c.alpha(1.0F - this.mountA);
         int fc = s.hungerEffect ? -7362486 : this.hungerCol;
         this.thinBar(c, 136.0F, 30.5F, 85.0F, this.dispFood / 20.0F, this.dispFood / 20.0F, fc, true, Math.max(0.0F, this.dispSat) / 20.0F, mix(fc, -3388, 0.6F), 0.0F, false, 0.0F);
         this.mark(c, HotbarPainter.Icon.FOOD, rx - 3.0F, 26.0F, 6.4F, fc, false);
         float tw = this.numbers ? this.textShadow(c, "inter-sb", fs, rx - 8.0F, 26.0F, 20, -1, String.valueOf(s.food)) : 0.0F;
         labelEnd = rx - 8.0F - tw - 2.0F;
         c.restore();
      }

      if (this.mountA > 0.0F) {
         float mm = Math.max(1.0F, s.mountMax);
         c.save();
         c.alpha(this.mountA);
         this.thinBar(c, 136.0F, 30.5F, 85.0F, this.dispMount / mm, this.trailMount / mm, this.mountCol, true, 0.0F, 0, 0.0F, false, 0.0F);
         this.mark(c, HotbarPainter.Icon.HEART, rx - 3.0F, 26.0F, 6.0F, this.mountCol, false);
         float tw = this.numbers ? this.textShadow(c, "inter-sb", fs, rx - 8.0F, 26.0F, 20, -1, fmt(s.mountHealth)) : 0.0F;
         labelEnd = Math.min(labelEnd, rx - 8.0F - tw - 2.0F);
         c.restore();
      }

      if (this.airA > 0.0F) {
         c.save();
         c.alpha(this.airA);
         float f = clamp01(Math.max(0.0F, this.dispAir) / Math.max(1.0F, s.maxAir));
         float bw = 22.0F;
         float bx = labelEnd - 4.0F - bw;
         this.mark(c, HotbarPainter.Icon.BUBBLE, bx - 4.5F, 26.0F, 6.0F, this.airCol, false);
         c.rect(bx, 25.0F, bw, 2.0F, 1.0F, a(-16777216, 0.4F));
         if (f > 0.0F) {
            c.rect(bx, 25.0F, Math.max(2.0F, bw * f), 2.0F, 1.0F, this.airCol);
         }

         c.restore();
      }
   }

   private void thinBar(Canvas c, float x, float y, float w, float frac, float trailFrac, int col, boolean mirror, float band, int bandCol, float beat, boolean sheen, float flash) {
      float h = 4.0F;
      frac = clamp01(frac);
      trailFrac = clamp01(Math.max(frac, trailFrac));
      float r = this.rounding > 0.0F ? h * 0.5F : 0.0F;
      c.rect(x, y, w, h, r, a(-16777216, 0.3F + 0.25F * this.bgOpacity));
      if (trailFrac > frac + 0.001F) {
         float tw = Math.max(h, w * trailFrac);
         c.rect(mirror ? x + w - tw : x, y, tw, h, r, a(mix(col, -1, 0.6F), 0.55F));
      }

      if (frac > 0.0F) {
         float fw = Math.max(h, w * frac);
         float fx = mirror ? x + w - fw : x;
         int dark = mix(col, -16777216, 0.18F);
         c.gradient(fx, y, fw, h, r, mirror ? col : dark, mirror ? dark : col, true);
         if (beat > 0.0F) {
            c.rect(fx, y, fw, h, r, a(-1, 0.22F * beat));
         }

         if (flash > 0.0F) {
            c.rect(fx, y, fw, h, r, a(-1, 0.3F * flash));
         }

         if (sheen) {
            float p = this.time * 0.5F % 1.4F - 0.2F;
            float bx = fx + p * fw;
            c.save();
            c.scissor(fx, y, fw, h);
            c.gradient(bx - 8.0F, y, 8.0F, h, 0.0F, 16777215, a(-1, 0.28F), true);
            c.gradient(bx, y, 8.0F, h, 0.0F, a(-1, 0.28F), 16777215, true);
            c.restore();
         }
      }

      if (band > 0.001F) {
         float bw = Math.max(h * 0.5F, w * clamp01(band));
         c.save();
         c.scissor(x, y, w, h);
         c.rect(mirror ? x + w - bw : x, y, bw, h * 0.5F, r * 0.5F, a(bandCol, 0.9F));
         c.restore();
      }
   }

   private void mark(Canvas c, HotbarPainter.Icon icon, float cx, float cy, float size, int col, boolean hardcore) {
      switch (icon) {
         case HEART:
            shape(c, HEART, cx + 0.25F, cy + 0.4F, size, a(-16777216, 0.4F));
            shape(c, HEART, cx, cy, size, col);
            c.circle(cx - size * 0.2F, cy - size * 0.16F, size * 0.1F, a(-1, 0.6F));
            if (hardcore) {
               c.circle(cx, cy - size * 0.32F, size * 0.09F, a(-16777216, 0.55F));
            }
            break;
         case FOOD:
            this.drumstick(c, cx + 0.25F, cy + 0.4F, size, a(-16777216, 0.4F), 1.0F);
            this.drumstick(c, cx, cy, size, col, 1.0F);
            break;
         case ARMOR:
            shape(c, SHIELD, cx + 0.25F, cy + 0.35F, size, a(-16777216, 0.4F));
            shape(c, SHIELD, cx, cy, size, col);
            shape(c, SHIELD_HI, cx, cy, size, a(-1, 0.3F));
            break;
         case BUBBLE:
            c.circle(cx + 0.25F, cy + 0.35F, size * 0.42F, a(-16777216, 0.35F));
            c.circle(cx, cy, size * 0.4F, a(col, 0.5F));
            c.ring(cx, cy, size * 0.36F, 0.7F, mix(col, -1, 0.4F));
            c.circle(cx - size * 0.13F, cy - size * 0.13F, size * 0.1F, -1);
      }
   }

   private void drawIconStatus(Canvas c, HotbarState s) {
      float max = Math.max(1.0F, s.maxHealth);
      int maxH = (int)Math.ceil(max / 2.0F);
      int absH = (int)Math.ceil(Math.max(0.0F, this.dispAbs) / 2.0F);
      int total = maxH + absH;
      int rows = Math.max(1, (total + 9) / 10);
      float rowH = Math.max(10 - (rows - 2), 3);
      float baseY = 27.5F;
      int hc = this.healthColour(s);
      boolean low = this.pulse && lowHealth(s);
      int regenIdx = s.regen ? (int)(this.time * 20.0F) % (maxH + 5) : -1;

      for (int i = total - 1; i >= 0; i--) {
         int row = i / 10;
         int col = i % 10;
         float cx = 39 + col * 8 + 4.5F;
         float cy = baseY - row * rowH;
         if (low) {
            cy += (float)Math.sin(this.time * 15.0F + i * 2.1F) * 0.7F;
         }

         if (i == regenIdx) {
            cy -= 2.0F;
         }

         float size = 9.0F;
         if (low) {
            size *= 1.0F + 0.1F * heartbeat(this.time - i * 0.035F);
         }

         this.heartContainer(c, cx, cy, size, s.hardcore);
         if (i < maxH) {
            float hp = this.dispHealth - i * 2.0F;
            float tr = this.trailHealth - i * 2.0F;
            if (tr > hp + 0.01F) {
               this.heartFill(c, cx, cy, size, clampHalf(tr), a(-1, 0.85F), false);
            }

            this.heartFill(c, cx, cy, size, clampHalf(hp), hc, s.hardcore);
            if (this.healFlash > 0.0F && hp > 0.0F) {
               this.heartFill(c, cx, cy, size, clampHalf(hp), a(-1, 0.5F * this.healFlash), false);
            }
         } else {
            float ab = this.dispAbs - (i - maxH) * 2.0F;
            this.heartFill(c, cx, cy, size, clampHalf(ab), this.absorbCol, false);
         }
      }

      float topY = baseY - (rows - 1) * rowH;
      if (this.armorA > 0.0F) {
         c.save();
         c.alpha(this.armorA);

         for (int i = 0; i < 10; i++) {
            float cx = 39 + i * 8 + 4.5F;
            float cy = topY - 10.0F;
            float v = clampHalf(s.armor - i * 2.0F);
            shape(c, SHIELD, cx, cy, 8.6F, a(-16777216, 0.5F));
            if (v > 0.0F) {
               c.save();
               if (v < 1.0F) {
                  c.scissor(cx - 4.5F, cy - 5.0F, 4.5F, 10.0F);
               }

               shape(c, SHIELD, cx, cy, 7.0F, this.armorCol);
               shape(c, SHIELD_HI, cx, cy, 7.0F, a(-1, 0.35F));
               c.restore();
            }
         }

         c.restore();
      }

      if (this.mountA < 1.0F) {
         c.save();
         c.alpha(1.0F - this.mountA);
         int fc = s.hungerEffect ? -7362486 : this.hungerCol;

         for (int i = 0; i < 10; i++) {
            float cx = 221 - i * 8 - 4.5F;
            float cy = baseY;
            float v = clampHalf(this.dispFood - i * 2.0F);
            this.drumstick(c, cx, cy, 9.4F, a(-16777216, 0.5F), 1.0F);
            if (v > 0.0F) {
               c.save();
               if (v < 1.0F) {
                  c.scissor(cx, cy - 6.0F, 6.0F, 12.0F);
               }

               this.drumstick(c, cx, cy, 8.2F, fc, 1.0F);
               c.restore();
            }

            if (this.dispSat - i * 2.0F >= 1.0F) {
               c.ring(cx + 1.2F, cy - 1.2F, 2.9F, 0.6F, a(mix(fc, -3388, 0.7F), 0.85F));
            }
         }

         c.restore();
      }

      float rightTop = baseY;
      if (this.mountA > 0.0F) {
         int mh = (int)Math.ceil(Math.max(1.0F, s.mountMax) / 2.0F);
         int mrows = (mh + 9) / 10;
         c.save();
         c.alpha(this.mountA);

         for (int i = 0; i < mh; i++) {
            int row = i / 10;
            int col = i % 10;
            float cx = 221 - col * 8 - 4.5F;
            float cy = baseY - row * 10.0F;
            this.heartContainer(c, cx, cy, 9.0F, false);
            this.heartFill(c, cx, cy, 9.0F, clampHalf(this.dispMount - i * 2.0F), this.mountCol, false);
         }

         c.restore();
         rightTop = baseY - (mrows - 1) * 10.0F * this.mountA;
      }

      if (this.airA > 0.0F) {
         c.save();
         c.alpha(this.airA);
         float v = Math.max(0.0F, this.dispAir) / Math.max(1.0F, s.maxAir) * 10.0F;

         for (int i = 0; i < 10; i++) {
            float cx = 221 - i * 8 - 4.5F;
            float cy = rightTop - 10.0F;
            float f = clamp01(v - (9 - i));
            if (!(f <= 0.0F)) {
               float r = 3.6F * (0.35F + 0.65F * f);
               c.circle(cx, cy, r + 0.6F, a(-16777216, 0.45F));
               c.circle(cx, cy, r, a(this.airCol, 0.55F));
               c.ring(cx, cy, r - 0.3F, 0.8F, mix(this.airCol, -1, 0.35F));
               c.circle(cx - r * 0.35F, cy - r * 0.38F, r * 0.28F, a(-1, 0.8F));
            }
         }

         c.restore();
      }
   }

   private void heartContainer(Canvas c, float cx, float cy, float size, boolean hardcore) {
      shape(c, HEART, cx, cy, size, a(-16777216, 0.55F));
      shape(c, HEART, cx, cy, size * 0.8F, a(mix(this.surface, -1, 0.1F), 0.75F));
   }

   private void heartFill(Canvas c, float cx, float cy, float size, float fill, int col, boolean hardcore) {
      if (!(fill <= 0.0F)) {
         c.save();
         if (fill < 1.0F) {
            c.scissor(cx - size, cy - size, size, size * 2.0F);
         }

         float s = size * 0.8F;
         shape(c, HEART, cx, cy, s, col);
         if (col >>> 24 >= 240) {
            shape(c, HEART_SHADE, cx, cy, s, a(-16777216, 0.2F));
            c.circle(cx - s * 0.2F, cy - s * 0.17F, s * 0.11F, a(-1, 0.75F));
            if (hardcore) {
               c.circle(cx, cy - s * 0.3F, s * 0.09F, a(-16777216, 0.5F));
            }
         }

         c.restore();
      }
   }

   private void drumstick(Canvas c, float cx, float cy, float size, int col, float grow) {
      float s = size * grow;
      c.save();
      c.translate(cx, cy);
      c.scale(s);
      int bone = col >>> 24 < 240 ? col : mix(-660515, col, 0.15F);
      c.line(-0.04F, 0.04F, -0.3F, 0.3F, 0.15F, bone);
      c.circle(-0.37F, 0.24F, 0.1F, bone);
      c.circle(-0.24F, 0.37F, 0.1F, bone);
      c.fillPoly(MEAT, col);
      if (col >>> 24 >= 240) {
         c.fillPoly(MEAT_SHADE, a(-16777216, 0.2F));
         c.circle(0.0F, -0.22F, 0.07F, a(-1, 0.6F));
      }

      c.restore();
   }

   private void drawXp(Canvas c, HotbarState s) {
      boolean jump = this.jumpA > 0.5F;
      if (s.hasXp || s.jump) {
         float x = 40.0F;
         float y = 36.2F;
         float w = 180.0F;
         float h = 2.0F;
         int col = jump ? mix(this.accent, -14016, 0.5F) : this.xpCol;
         float f = jump ? clamp01(s.jumpScale) : clamp01(Math.max(0.0F, this.dispXp));
         c.rect(x, y, w, h, h * 0.5F, a(-16777216, 0.4F));
         if (f > 0.0F) {
            float fw = Math.max(h, w * f);
            c.gradient(x, y, fw, h, h * 0.5F, mix(col, -16777216, 0.2F), col, true);
            if (this.levelFlash > 0.0F) {
               c.rect(x, y, fw, h, h * 0.5F, a(-1, 0.5F * this.levelFlash));
            }
         }

         if (!jump && s.level > 0) {
            float ty = this.icons ? 30.8F : 26.0F;
            int tc = mix(this.xpCol, -1, 0.1F + 0.6F * this.levelFlash);
            this.textShadow(c, "inter-sb", 6.6F, 130.0F, ty, 18, tc, String.valueOf(s.level));
         }
      }
   }

   private static void shape(Canvas c, float[] poly, float cx, float cy, float size, int col) {
      c.save();
      c.translate(cx, cy);
      c.scale(size);
      c.fillPoly(poly, col);
      c.restore();
   }

   private float textShadow(Canvas c, String font, float size, float x, float y, int align, int col, String s) {
      c.text(font, size, x + 0.45F, y + 0.55F, align, a(-16777216, 0.6F * ((col >>> 24) / 255.0F)), s);
      return c.text(font, size, x, y, align, col, s);
   }

   private int healthColour(HotbarState s) {
      if (s.wither) {
         return -10857120;
      } else if (s.poison) {
         return -7485889;
      } else {
         return s.frozen ? -7350017 : this.healthCol;
      }
   }

   private static float heartbeat(float t) {
      float p = t * 1.25F % 1.0F;
      float a = (float)Math.exp(-Math.pow((p - 0.08F) / 0.05F, 2.0));
      float b = 0.6F * (float)Math.exp(-Math.pow((p - 0.28F) / 0.06F, 2.0));
      return Math.min(1.0F, a + b);
   }

   private static String fmt(float v) {
      float r = Math.round(v * 2.0F) / 2.0F;
      return Math.abs(r - Math.round(r)) < 0.01F ? String.valueOf(Math.round(r)) : String.valueOf(r);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float clampHalf(float v) {
      return v >= 2.0F ? 1.0F : (v >= 1.0F ? 0.5F : 0.0F);
   }

   static int a(int argb, float alpha) {
      int al = Math.round((argb >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, alpha)));
      return al << 24 | argb & 16777215;
   }

   static int mix(int c1, int c2, float t) {
      int a1 = c1 >>> 24;
      int r1 = c1 >> 16 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int b1 = c1 & 0xFF;
      int a2 = c2 >>> 24;
      int r2 = c2 >> 16 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b2 = c2 & 0xFF;
      return Math.round(a1 + (a2 - a1) * t) << 24 | Math.round(r1 + (r2 - r1) * t) << 16 | Math.round(g1 + (g2 - g1) * t) << 8 | Math.round(b1 + (b2 - b1) * t);
   }

   private static float[] roundPoly(float[] p, float cut) {
      int n = p.length / 2;
      float[] out = new float[n * 4];

      for (int i = 0; i < n; i++) {
         float x = p[i * 2];
         float y = p[i * 2 + 1];
         float px = p[(i - 1 + n) % n * 2];
         float py = p[(i - 1 + n) % n * 2 + 1];
         float nx = p[(i + 1) % n * 2];
         float ny = p[(i + 1) % n * 2 + 1];
         float d1 = (float)Math.hypot(px - x, py - y);
         float d2 = (float)Math.hypot(nx - x, ny - y);
         float c1 = Math.min(cut, d1 * 0.45F);
         float c2 = Math.min(cut, d2 * 0.45F);
         out[i * 4] = x + (px - x) / d1 * c1;
         out[i * 4 + 1] = y + (py - y) / d1 * c1;
         out[i * 4 + 2] = x + (nx - x) / d2 * c2;
         out[i * 4 + 3] = y + (ny - y) / d2 * c2;
      }

      return out;
   }

   static {
      int n = 56;
      float[] raw = new float[n * 2];
      float minX = 1.0E9F;
      float maxX = -1.0E9F;
      float minY = 1.0E9F;
      float maxY = -1.0E9F;

      for (int i = 0; i < n; i++) {
         double t = i * 2 * Math.PI / n;
         float x = (float)(16.0 * Math.pow(Math.sin(t), 3.0));
         float y = (float)(-(13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t)));
         raw[i * 2] = x;
         raw[i * 2 + 1] = y;
         minX = Math.min(minX, x);
         maxX = Math.max(maxX, x);
         minY = Math.min(minY, y);
         maxY = Math.max(maxY, y);
      }

      float sc = 1.0F / Math.max(maxX - minX, maxY - minY);
      float mx = (minX + maxX) / 2.0F;
      float my = (minY + maxY) / 2.0F;
      HEART = new float[n * 2];

      for (int i = 0; i < n; i++) {
         HEART[i * 2] = (raw[i * 2] - mx) * sc;
         HEART[i * 2 + 1] = (raw[i * 2 + 1] - my) * sc;
      }

      ArrayList<Float> sh = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         float x = HEART[i * 2];
         float y = HEART[i * 2 + 1];
         if (x * 0.6F + y > 0.05F) {
            sh.add(x);
            sh.add(y);
         }
      }

      HEART_SHADE = new float[sh.size()];

      for (int i = 0; i < sh.size(); i++) {
         HEART_SHADE[i] = sh.get(i);
      }

      SHIELD = roundPoly(new float[]{-0.44F, -0.44F, -0.1F, -0.44F, 0.0F, -0.36F, 0.1F, -0.44F, 0.44F, -0.44F, 0.44F, 0.02F, 0.22F, 0.3F, 0.0F, 0.48F, -0.22F, 0.3F, -0.44F, 0.02F}, 0.06F);
      SHIELD_HI = new float[]{-0.36F, -0.36F, -0.12F, -0.36F, -0.12F, 0.2F, -0.36F, -0.02F};
      int m = 28;
      MEAT = new float[m * 2];

      for (int i = 0; i < m; i++) {
         double t = i * 2 * Math.PI / m;
         float r = 0.34F * (1.0F + 0.12F * (float)Math.cos(t + (Math.PI * 3.0 / 4.0)));
         MEAT[i * 2] = 0.1F + (float)Math.cos(t) * r;
         MEAT[i * 2 + 1] = -0.1F + (float)Math.sin(t) * r;
      }

      ArrayList<Float> ms = new ArrayList<>();

      for (int i = 0; i < m; i++) {
         if (MEAT[i * 2] - 0.1F + MEAT[i * 2 + 1] + 0.1F > 0.08F) {
            ms.add(MEAT[i * 2]);
            ms.add(MEAT[i * 2 + 1]);
         }
      }

      MEAT_SHADE = new float[ms.size()];

      for (int i = 0; i < ms.size(); i++) {
         MEAT_SHADE[i] = ms.get(i);
      }
   }

   private enum Icon {
      HEART,
      FOOD,
      ARMOR,
      BUBBLE;
   }
}
