package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class SakuraAura implements Cosmetic {
   public static final int NATURAL_1 = 16763103;
   public static final int NATURAL_2 = 16735902;
   public static final int QUADS = 284;
   private static final int STRANDS = 2;
   private static final int PER_STRAND = 22;
   private static final int BLOSSOMS = 4;
   private static final int FALLING = 14;
   private static final int GROUND = 28;
   private static final int BAND_SEGS = 15;
   private static final int HEART = 16771238;
   private final ElementParams p;

   public SakuraAura() {
      this(ElementParams.defaults());
   }

   public SakuraAura(ElementParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double ring = 0.45 + 0.5 * this.p.radius();
      double body = this.p.bodyHeight();
      double[] cam = b.viewLocal();
      this.glow(b, ring);

      for (int k = 0; k < 28; k++) {
         this.groundPetal(b, ring, k, t);
      }

      for (int s = 0; s < 2; s++) {
         this.band(b, ring, body, s, t);

         for (int j = 0; j < 22; j++) {
            this.helixPetal(b, cam, ring, body, s, j, t);
         }
      }

      for (int k = 0; k < 14; k++) {
         this.fallingPetal(b, cam, ring, body, k, t);
      }

      for (int k = 0; k < 4; k++) {
         this.blossom(b, cam, ring, body, k, t);
      }
   }

   private void glow(Builder b, double ring) {
      int c1 = b.c1;
      int c2 = b.c2;
      Prim.ring(b, 0.014, 0.1, ring * 1.2, 24, (u, v) -> Col.withA(Col.mix(c2, c1, v), 0.26 * Math.pow(1.0 - v, 1.3)));
   }

   private void petal(Builder b, double s, double k, double f) {
      int deep = AuraKit.fade(Col.withA(Col.shade(Col.mix(b.c2, b.c1, 0.15), k), 0.95), f);
      int mid = AuraKit.fade(Col.withA(Col.shade(Col.mix(b.c2, b.c1, 0.55), k), 0.95), f);
      int tip = AuraKit.fade(Col.withA(Col.shade(Col.tint(b.c1, 0.45), k), 0.93), f);
      int notch = AuraKit.fade(Col.withA(Col.shade(Col.tint(b.c1, 0.25), k), 0.9), f);
      double hw = 0.5 * s;
      double[] base = Prim.P(0.0, 0.0, 0.0);
      double[] l = Prim.P(-hw, 0.1 * s, 0.5 * s);
      double[] r = Prim.P(hw, 0.1 * s, 0.5 * s);
      double[] tl = Prim.P(-0.55 * hw, 0.18 * s, s);
      double[] tr = Prim.P(0.55 * hw, 0.18 * s, s);
      double[] n = Prim.P(0.0, 0.1 * s, 0.86 * s);
      b.quad(base, l, tl, n, deep, mid, tip, notch);
      b.quad(base, n, tr, r, deep, notch, tip, mid);
   }

   private double sheen(Builder b) {
      return 0.78 + 0.34 * b.facing(Prim.P(0.0, 0.0, 0.0), Prim.P(0.0, 1.0, 0.0));
   }

   private double helixU(Builder b, int s, int j, double t) {
      double u0 = (j + 0.5 * s) / 22.0;
      return b.animate ? AuraKit.frac(u0 + t * 0.075) : u0 % 1.0;
   }

   private double[] helixPoint(int s, double u, double t, double ring, double body, boolean animate) {
      double a = Math.PI * s + u * (Math.PI * 2) * 1.6 + (animate ? t * 0.5 : 0.0);
      double r = ring * (0.62 + 0.55 * u) * (1.0 + (animate ? 0.06 * Math.sin(u * 9.0 + t) : 0.0));
      return Prim.P(r * Math.cos(a), 0.03 + u * (body + 0.25), r * Math.sin(a));
   }

   private void helixPetal(Builder b, double[] cam, double ring, double body, int s, int j, double t) {
      double u = this.helixU(b, s, j, t);
      double[] c = this.helixPoint(s, u, t, ring, body, b.animate);
      double env = Prim.sm(u / 0.08) * Prim.sm((1.0 - u) / 0.15);
      double f = AuraKit.near(cam, c[0], c[1], c[2], b.scale()) * env;
      if (!(f <= 0.03)) {
         int id = s * 50 + j;
         double size = 0.13 + 0.05 * Prim.rnd(id * 17 + 1);
         double a = Math.atan2(c[2], c[0]);
         b.push();
         b.translate(c[0], c[1], c[2]);
         b.rotateY(-a + (Math.PI / 2) + (b.animate ? t * 1.4 : 0.0) + id);
         b.rotateX(0.6 + (b.animate ? 0.9 * Math.sin(t * 2.0 + id * 1.3) : 0.3));
         b.rotateZ(b.animate ? 0.6 * Math.sin(t * 1.5 + id * 0.7) : 0.2);
         b.translate(0.0, 0.0, -0.5 * size);
         this.petal(b, size, this.sheen(b), f);
         b.pop();
      }
   }

   private void band(Builder b, double ring, double body, int s, double t) {
      int n = 15;
      double[][] pts = new double[n + 1][];

      for (int i = 0; i <= n; i++) {
         pts[i] = this.helixPoint(s, 0.03 + 0.94 * i / n, t, ring, body, b.animate);
      }

      double hw = 0.07;
      int c1 = Col.tint(b.c1, 0.2);
      int c2 = b.c2;

      for (int i = 0; i < n; i++) {
         double v0 = (double)i / n;
         double v1 = (double)(i + 1) / n;
         int a0 = Col.withA(Col.mix(c2, c1, v0), 0.28 * Math.sin(Math.PI * v0));
         int a1 = Col.withA(Col.mix(c2, c1, v1), 0.28 * Math.sin(Math.PI * v1));
         double[] p0 = pts[i];
         double[] p1 = pts[i + 1];
         b.quad(Prim.P(p0[0], p0[1] + hw, p0[2]), Prim.P(p0[0], p0[1] - hw, p0[2]), Prim.P(p1[0], p1[1] - hw, p1[2]), Prim.P(p1[0], p1[1] + hw, p1[2]), a0, a0, a1, a1);
      }
   }

   private void fallingPetal(Builder b, double[] cam, double ring, double body, int k, double t) {
      double rate = 0.1 + 0.06 * Prim.rnd(k * 43 + 1);
      double u = b.animate ? AuraKit.frac(t * rate + Prim.rnd(k * 43 + 2)) : Prim.rnd(k * 43 + 2);
      double a0 = (Math.PI * 2) * Prim.rnd(k * 43 + 3);
      double r = Math.max(0.36, ring * 1.25 * Math.sqrt(Prim.rnd(k * 43 + 4)));
      double sway = b.animate ? 0.1 * Math.sin(t * 1.1 + k * 1.7) : 0.0;
      double x = r * Math.cos(a0) + sway;
      double z = r * Math.sin(a0) + (b.animate ? 0.08 * Math.cos(t * 0.9 + k) : 0.0);
      double y = (body + 0.6) * (1.0 - u) + 0.03;
      double env = Prim.sm(u / 0.1) * Prim.sm((1.0 - u) / 0.12);
      double f = AuraKit.near(cam, x, y, z, b.scale()) * env;
      if (!(f <= 0.03)) {
         double size = 0.09 + 0.05 * Prim.rnd(k * 43 + 5);
         b.push();
         b.translate(x, y, z);
         b.rotateY(a0 + (b.animate ? t * 1.1 : 0.0) + k);
         b.rotateX(0.8 + (b.animate ? 1.0 * Math.sin(t * 1.8 + k * 2.1) : 0.2));
         b.rotateZ(b.animate ? 0.7 * Math.sin(t * 1.3 + k) : 0.1);
         b.translate(0.0, 0.0, -0.5 * size);
         this.petal(b, size, this.sheen(b), f);
         b.pop();
      }
   }

   private void groundPetal(Builder b, double ring, int k, double t) {
      double r = ring * 1.3 * Math.sqrt(Prim.rnd(k * 41 + 1));
      if (r < 0.32) {
         r += 0.32;
      }

      double a = (Math.PI * 2) * Prim.rnd(k * 41 + 2);
      double size = 0.08 + 0.05 * Prim.rnd(k * 41 + 3);
      double stir = b.animate ? 0.05 * Math.sin(t * 0.7 + k * 1.3) : 0.0;
      b.push();
      b.translate(r * Math.cos(a), 0.016 + 0.004 * (k % 3), r * Math.sin(a));
      b.rotateY((Math.PI * 2) * Prim.rnd(k * 41 + 4) + stir);
      b.translate(0.0, 0.0, -0.5 * size);
      this.petal(b, size, 0.9, 1.0);
      b.pop();
   }

   private void blossom(Builder b, double[] cam, double ring, double body, int k, double t) {
      double a = (Math.PI * 2) * k / 4.0 + (b.animate ? t * 0.22 : 0.0);
      double r = ring * 1.05;
      double y = body * (0.24 + 0.52 * (k * 3 % 4) / 3.0) + (b.animate ? 0.06 * Math.sin(t * 1.2 + k * 1.9) : 0.0);
      double x = r * Math.cos(a);
      double z = r * Math.sin(a);
      double f = AuraKit.near(cam, x, y, z, b.scale());
      if (!(f <= 0.03)) {
         double size = 0.17 + 0.03 * Prim.rnd(k * 47 + 1);
         b.push();
         b.translate(x, y, z);
         b.rotateY(-a + (b.animate ? 0.6 * Math.sin(t * 0.8 + k) : 0.0) + k);
         b.rotateX(-0.9 + (b.animate ? 0.25 * Math.sin(t * 1.1 + k * 2.3) : 0.0));
         double bright = this.sheen(b);

         for (int i = 0; i < 5; i++) {
            b.push();
            b.rotateY((Math.PI * 2) * i / 5.0);
            b.rotateX(-0.35);
            this.petal(b, size, bright, f);
            b.pop();
         }

         int heart = AuraKit.fade(Col.withA(16771238, 0.95), f);
         int edge = AuraKit.fade(Col.withA(Col.mix(16771238, b.c2, 0.4), 0.9), f);
         AuraKit.fan(b, 0.0, 0.0, size * 0.22, 0.006, 5, 0.0, heart, edge);
         b.pop();
      }
   }
}
