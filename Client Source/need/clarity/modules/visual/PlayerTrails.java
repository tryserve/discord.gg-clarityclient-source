package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.render.worldfx.WorldFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

public class PlayerTrails extends ModuleBase {
   public static final String[] STYLES = new String[]{"Energy", "Ribbon", "Lightning", "Comet", "Wind", "Aurora", "Silk", "Ethereal"};
   private static final int SUB = 4;
   private static final float SIDE_PAD = 1.4F;
   private static final long START = System.nanoTime();
   private final Map<Integer, ArrayDeque<PlayerTrails.Pt>> trails = new HashMap<>();
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private double[] px = new double[64];
   private double[] py = new double[64];
   private double[] pz = new double[64];
   private double[] pa = new double[64];
   private float[] sx = new float[256];
   private float[] sy = new float[256];
   private float[] sz = new float[256];
   private float[] sa = new float[256];

   public PlayerTrails() {
      super("Player Trails", "Visual");
      if (this.isEnabled()) {
         WorldRenderEvent.register(this.renderListener);
      }
   }

   @Override
   public void onEnable() {
      WorldRenderEvent.register(this.renderListener);
   }

   @Override
   public void onDisable() {
      WorldRenderEvent.unregister(this.renderListener);
      this.trails.clear();
   }

   private float lifetime() {
      return Math.max(5, this.getSetting("Length", 30)) / 20.0F;
   }

   private double height() {
      return this.getSetting("Height", 0.1F).floatValue();
   }

   private boolean include(AbstractClientPlayer p, Minecraft mc) {
      if (p.isAlive() && !p.isSpectator()) {
         return p == mc.player ? this.getSetting("Self", true) : this.getSetting("Others", true);
      } else {
         return false;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         long now = System.nanoTime();
         long life = (long)(this.lifetime() * 1.0E9);
         double h = this.height();
         Set<Integer> active = new HashSet<>();

         for (AbstractClientPlayer player : mc.level.players()) {
            if (this.include(player, mc)) {
               int id = player.getId();
               active.add(id);
               ArrayDeque<PlayerTrails.Pt> trail = this.trails.computeIfAbsent(id, k -> new ArrayDeque<>());
               double x = player.getX();
               double y = player.getY() + h;
               double z = player.getZ();
               PlayerTrails.Pt last = trail.peekFirst();
               if (last == null || sq(last.x - x) + sq(last.y - y) + sq(last.z - z) > 9.0E-4) {
                  trail.addFirst(new PlayerTrails.Pt(x, y, z, now));
               }

               while (!trail.isEmpty() && now - trail.peekLast().t > life) {
                  trail.removeLast();
               }

               while (trail.size() > 120) {
                  trail.removeLast();
               }
            }
         }

         this.trails.keySet().removeIf(idx -> !active.contains(idx));
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled() && !this.trails.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            String style = this.getSetting("Style", "Energy");

            int styleId = switch (style) {
               case "Ribbon" -> 1;
               case "Lightning" -> 2;
               case "Comet" -> 3;
               case "Wind" -> 4;
               case "Aurora" -> 5;
               case "Silk" -> 6;
               case "Ethereal" -> 7;
               default -> 0;
            };
            String colorMode = this.getSetting("Color Mode", "Custom");
            float[] c1 = rgb(ThemeColors.resolveRgb(this.getSetting("Colour", "#00AAFF"), 43775, false));
            float[] c2 = rgb(ThemeColors.resolveRgb(this.getSetting("Second Colour", "#B45CFF"), 11820287, true));
            float width = Math.max(0.02F, this.getSetting("Trail Width", 0.3F));
            if (styleId == 2) {
               width *= 1.6F;
            }

            if (styleId == 4 || styleId == 7) {
               width *= 1.8F;
            }

            float opacity = clamp(this.getSetting("Opacity", 0.9F), 0.0F, 1.0F);
            float speed = Math.max(0.0F, this.getSetting("Speed", 1.0F));
            boolean through = this.getSetting("Through Walls", false);
            float time = (float)((System.nanoTime() - START) / 1.0E9 * speed % 3600.0);
            float life = this.lifetime();
            long now = System.nanoTime();
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            double h = this.height();
            WorldFx.begin();

            for (AbstractClientPlayer player : mc.level.players()) {
               ArrayDeque<PlayerTrails.Pt> trail = this.trails.get(player.getId());
               if (trail != null && !trail.isEmpty() && this.include(player, mc)) {
                  int n = 0;
                  this.ensureP(trail.size() + 1);
                  double hx = player.xo + (player.getX() - player.xo) * partialTick;
                  double hy = player.yo + (player.getY() - player.yo) * partialTick + h;
                  double hz = player.zo + (player.getZ() - player.zo) * partialTick;
                  this.px[n] = hx - cam.x;
                  this.py[n] = hy - cam.y;
                  this.pz[n] = hz - cam.z;
                  this.pa[n] = 0.0;
                  n++;

                  for (PlayerTrails.Pt p : trail) {
                     double age = (now - p.t) / 1.0E9 / life;
                     if (age >= 1.0) {
                        break;
                     }

                     double x = p.x - cam.x;
                     double y = p.y - cam.y;
                     double z = p.z - cam.z;
                     if (n != 1 || !(sq(x - this.px[0]) + sq(y - this.py[0]) + sq(z - this.pz[0]) < 4.0E-4)) {
                        this.px[n] = x;
                        this.py[n] = y;
                        this.pz[n] = z;
                        this.pa[n] = Math.max(age, this.pa[n - 1] + 1.0E-4);
                        n++;
                     }
                  }

                  if (n >= 2) {
                     int m = this.smooth(n);
                     this.emitRibbon(m, width, styleId, colorMode, c1, c2, time, seed(player.getId()));
                  }
               }
            }

            if (!WorldFx.isEmpty()) {
               int sid = styleId;
               float t = time;
               float op = opacity;
               WorldFx.flush("trail", through, s -> {
                  s.setUniform1f("uTime", t);
                  s.setUniform1i("uStyle", sid);
                  s.setUniform1f("uOpacity", op);
               });
            }
         }
      }
   }

   private int smooth(int n) {
      int need = (n - 1) * 4 + 1;
      if (this.sx.length < need) {
         this.sx = new float[need * 2];
         this.sy = new float[need * 2];
         this.sz = new float[need * 2];
         this.sa = new float[need * 2];
      }

      int m = 0;

      for (int i = 0; i < n - 1; i++) {
         int i0 = Math.max(i - 1, 0);
         int i2 = i + 1;
         int i3 = Math.min(i + 2, n - 1);

         for (int s = 0; s < 4; s++) {
            double u = s / 4.0;
            double u2 = u * u;
            double u3 = u2 * u;
            this.sx[m] = (float)cr(this.px[i0], this.px[i], this.px[i2], this.px[i3], u, u2, u3);
            this.sy[m] = (float)cr(this.py[i0], this.py[i], this.py[i2], this.py[i3], u, u2, u3);
            this.sz[m] = (float)cr(this.pz[i0], this.pz[i], this.pz[i2], this.pz[i3], u, u2, u3);
            this.sa[m] = (float)(this.pa[i] + (this.pa[i2] - this.pa[i]) * u);
            m++;
         }
      }

      this.sx[m] = (float)this.px[n - 1];
      this.sy[m] = (float)this.py[n - 1];
      this.sz[m] = (float)this.pz[n - 1];
      this.sa[m] = (float)this.pa[n - 1];
      return m + 1;
   }

   private static double cr(double p0, double p1, double p2, double p3, double u, double u2, double u3) {
      return 0.5 * (2.0 * p1 + (-p0 + p2) * u + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * u2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * u3);
   }

   private void emitRibbon(int m, float width, int style, String colorMode, float[] c1, float[] c2, float time, float seed) {
      float prevSx = 0.0F;
      float prevSy = 1.0F;
      float prevSz = 0.0F;
      float d = 0.0F;
      float[] la = new float[3];
      float[] lb = new float[3];
      float[] ra = new float[3];
      float[] rb = new float[3];
      float[] ca = new float[3];
      float[] cb = new float[3];
      float dPrev = 0.0F;

      for (int k = 0; k < m; k++) {
         if (k > 0) {
            d += (float)Math.sqrt(sq(this.sx[k] - this.sx[k - 1]) + sq(this.sy[k] - this.sy[k - 1]) + sq(this.sz[k] - this.sz[k - 1]));
         }

         int a = Math.max(k - 1, 0);
         int b = Math.min(k + 1, m - 1);
         float tx = this.sx[b] - this.sx[a];
         float ty = this.sy[b] - this.sy[a];
         float tz = this.sz[b] - this.sz[a];
         float vx = -this.sx[k];
         float vy = -this.sy[k];
         float vz = -this.sz[k];
         float qx = ty * vz - tz * vy;
         float qy = tz * vx - tx * vz;
         float qz = tx * vy - ty * vx;
         float ql = (float)Math.sqrt(qx * qx + qy * qy + qz * qz);
         if (ql > 1.0E-6F) {
            qx /= ql;
            qy /= ql;
            qz /= ql;
            prevSx = qx;
            prevSy = qy;
            prevSz = qz;
         } else {
            qx = prevSx;
            qy = prevSy;
            qz = prevSz;
         }

         float age = this.sa[k];
         float w = width * 0.5F * (1.0F - 0.55F * age) * 1.4F;
         ra[0] = this.sx[k] + qx * w;
         ra[1] = this.sy[k] + qy * w;
         ra[2] = this.sz[k] + qz * w;
         rb[0] = this.sx[k] - qx * w;
         rb[1] = this.sy[k] - qy * w;
         rb[2] = this.sz[k] - qz * w;
         color(colorMode, c1, c2, age, d, time, seed, cb);
         if (k > 0) {
            float ageA = this.sa[k - 1];
            float P = 1.4F;
            quadVertex(la, ageA, 1.4F, ca, dPrev, seed);
            quadVertex(lb, ageA, -1.4F, ca, dPrev, seed);
            quadVertex(ra, age, 1.4F, cb, d, seed);
            quadVertex(lb, ageA, -1.4F, ca, dPrev, seed);
            quadVertex(rb, age, -1.4F, cb, d, seed);
            quadVertex(ra, age, 1.4F, cb, d, seed);
         }

         System.arraycopy(ra, 0, la, 0, 3);
         System.arraycopy(rb, 0, lb, 0, 3);
         System.arraycopy(cb, 0, ca, 0, 3);
         dPrev = d;
      }
   }

   private static void quadVertex(float[] p, float age, float side, float[] c, float d, float seed) {
      WorldFx.vertex(p[0], p[1], p[2], age, side, c[0], c[1], c[2], 1.0F, d, seed);
   }

   private static void color(String mode, float[] c1, float[] c2, float age, float d, float time, float seed, float[] out) {
      switch (mode) {
         case "Gradient":
            for (int i = 0; i < 3; i++) {
               out[i] = c1[i] + (c2[i] - c1[i]) * age;
            }
            break;
         case "Rainbow":
            hsv(d * 0.07F - time * 0.25F + seed, 0.75F, 1.0F, out);
            break;
         default:
            out[0] = c1[0];
            out[1] = c1[1];
            out[2] = c1[2];
      }
   }

   private void ensureP(int n) {
      if (this.px.length < n) {
         int c = n * 2;
         this.px = new double[c];
         this.py = new double[c];
         this.pz = new double[c];
         this.pa = new double[c];
      }
   }

   private static float seed(int id) {
      float f = id * 0.6180339F;
      return f - (float)Math.floor(f);
   }

   private static double sq(double v) {
      return v * v;
   }

   private static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }

   private static void hsv(float h, float s, float v, float[] out) {
      h -= (float)Math.floor(h);
      float r = Math.abs(h * 6.0F - 3.0F) - 1.0F;
      float g = 2.0F - Math.abs(h * 6.0F - 2.0F);
      float b = 2.0F - Math.abs(h * 6.0F - 4.0F);
      out[0] = v * (1.0F - s + s * clamp(r, 0.0F, 1.0F));
      out[1] = v * (1.0F - s + s * clamp(g, 0.0F, 1.0F));
      out[2] = v * (1.0F - s + s * clamp(b, 0.0F, 1.0F));
   }

   private record Pt(double x, double y, double z, long t) {
   }
}
