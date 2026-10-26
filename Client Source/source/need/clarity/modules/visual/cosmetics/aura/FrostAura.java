package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class FrostAura implements Cosmetic {
   public static final int NATURAL_1 = 12907007;
   public static final int NATURAL_2 = 4033791;
   public static final int QUADS = 432;
   private static final int CRYSTALS = 9;
   private static final int FLAKES = 7;
   private static final int SPECKS = 20;
   private final ElementParams p;

   public FrostAura() {
      this(ElementParams.defaults());
   }

   public FrostAura(ElementParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double ring = 0.35 + 0.6 * this.p.radius();
      double body = this.p.bodyHeight();
      double[] cam = b.viewLocal();
      this.mist(b, ring);

      for (int k = 0; k < 9; k++) {
         this.crystal(b, cam, ring, body, k, t);
      }

      for (int k = 0; k < 7; k++) {
         this.flake(b, cam, ring, body, k, t);
      }

      this.specks(b, cam, ring, body, t);
   }

   private void mist(Builder b, double ring) {
      int c1 = b.c1;
      int c2 = b.c2;
      Prim.ring(b, 0.02, 0.1, ring * 1.2, 28, (u, v) -> Col.withA(Col.mix(c2, c1, v), 0.3 * Math.pow(1.0 - v, 1.3)));
   }

   private void crystal(Builder b, double[] cam, double ring, double body, int k, double t) {
      double a = (Math.PI * 2) * (k + 0.4 * (Prim.rnd(k * 19 + 1) - 0.5)) / 9.0;
      double r = ring * (0.9 + 0.2 * Prim.rnd(k * 19 + 2));
      double h = body * (0.24 + 0.36 * Prim.rnd(k * 19 + 3));
      double lean = 0.1 + 0.32 * Prim.rnd(k * 19 + 4);
      double rr = 0.075 + 0.045 * Prim.rnd(k * 19 + 5);
      double f = AuraKit.near(cam, r * Math.sin(a), h * 0.5, r * Math.cos(a), b.scale());
      if (!(f <= 0.03)) {
         int deep = Col.withA(Col.shade(b.c2, 0.85), 0.78);
         int pale = Col.withA(Col.tint(b.c1, 0.35), 0.86);
         int tipC = Col.withA(Col.tint(b.c1, 0.85), 0.94);
         Prim.Shade body6 = AuraKit.fade((u, v) -> Col.mix(deep, pale, Math.pow(v, 0.8)), f);
         Prim.Shade head = AuraKit.fade((u, v) -> Col.mix(pale, tipC, v), f);
         b.push();
         b.translate(r * Math.sin(a), 0.0, r * Math.cos(a));
         b.rotateY(a);
         b.rotateX(lean);
         double neck = h * 0.72;
         Prim.prism(b, 0.0, neck, rr, 6, Math.PI / 6, body6, false);
         Prim.spike(b, Prim.P(0.0, neck, 0.0), Prim.P(0.0, h, 0.0), rr, 6, head);
         Prim.Shade shard = AuraKit.fade((u, v) -> Col.mix(deep, tipC, v), f);
         Prim.spike(b, Prim.P(rr * 1.7, 0.0, rr * 0.4), Prim.P(rr * 3.0, h * 0.36, rr * 1.5), rr * 0.75, 5, shard);
         Prim.spike(b, Prim.P(-rr * 1.6, 0.0, -rr * 0.5), Prim.P(-rr * 2.6, h * 0.28, -rr * 1.9), rr * 0.65, 5, shard);
         double tw = b.animate ? Math.pow(Math.max(0.0, Math.sin(t * 2.3 + k * 2.1)), 10.0) : (k % 3 == 0 ? 1 : 0);
         if (tw > 0.05) {
            double s = 0.05 + 0.07 * tw;
            double y = h + 0.03;
            int c = AuraKit.fade(Col.withA(-1, 0.9 * tw), f);
            int e = Col.withA(b.c1, 0.0);
            b.quad(Prim.P(0.0, y - s * 1.9, 0.0), Prim.P(s * 0.5, y, 0.0), Prim.P(0.0, y + s * 1.9, 0.0), Prim.P(-s * 0.5, y, 0.0), e, c, e, c);
            b.quad(Prim.P(0.0, y - s * 1.9, 0.0), Prim.P(0.0, y, s * 0.5), Prim.P(0.0, y + s * 1.9, 0.0), Prim.P(0.0, y, -s * 0.5), e, c, e, c);
         }

         b.pop();
      }
   }

   private void flake(Builder b, double[] cam, double ring, double body, int k, double t) {
      double rate = 0.1 * (0.7 + 0.6 * Prim.rnd(k * 23 + 1));
      double u = b.animate ? AuraKit.frac(t * rate + Prim.rnd(k * 23 + 2)) : (k + 0.5) / 7.0;
      double fall = body + 0.3;
      double y = fall * (1.0 - u) - 0.05;
      double dirn = k % 2 == 0 ? 1.0 : -1.0;
      double a = (Math.PI * 2) * (k / 7.0 + Prim.rnd(k * 23 + 3) * 0.3) + (b.animate ? t * 0.12 * dirn * (0.6 + 0.8 * Prim.rnd(k * 23 + 4)) : 0.0);
      double r = ring * (0.75 + 0.5 * Prim.rnd(k * 23 + 5));
      double x = r * Math.sin(a);
      double z = r * Math.cos(a);
      double env = Prim.sm(u / 0.12) * Prim.sm((1.0 - u) / 0.15);
      double f = AuraKit.near(cam, x, y, z, b.scale()) * env;
      if (!(f <= 0.03)) {
         double s = 0.17 + 0.09 * Prim.rnd(k * 23 + 6);
         int arm = AuraKit.fade(Col.withA(Col.tint(b.c1, 0.65), 0.92), f);
         int tipC = AuraKit.fade(Col.withA(-1, 0.95), f);
         b.push();
         b.translate(x, y, z);
         b.rotateY(b.animate ? t * 1.3 * dirn + k : k);
         b.rotateX(b.animate ? 0.55 * Math.sin(t * 0.9 + k * 1.7) + 0.4 : 0.5);
         double w = 0.026;

         for (int j = 0; j < 6; j++) {
            double aj = j * Math.PI / 3.0;
            double c = Math.cos(aj);
            double sn = Math.sin(aj);
            AuraKit.flatBar(b, c * s * 0.12, sn * s * 0.12, c * s, sn * s, w, 0.0, arm, tipC);
            double bx = c * s * 0.58;
            double bz = sn * s * 0.58;
            double bl = s * 0.32;

            for (int side = -1; side <= 1; side += 2) {
               double ab = aj + side * 0.6;
               AuraKit.flatBar(b, bx, bz, bx + Math.cos(ab) * bl, bz + Math.sin(ab) * bl, w * 0.8, 0.0, arm, tipC);
            }
         }

         AuraKit.fan(b, 0.0, 0.0, s * 0.17, 0.002, 6, 0.0, tipC, arm);
         b.pop();
      }
   }

   private void specks(Builder b, double[] cam, double ring, double body, double t) {
      for (int k = 0; k < 20; k++) {
         double rate = 0.06 + 0.06 * Prim.rnd(k * 29 + 1);
         double u = b.animate ? AuraKit.frac(t * rate + Prim.rnd(k * 29 + 2)) : Prim.rnd(k * 29 + 2);
         double a = (Math.PI * 2) * Prim.rnd(k * 29 + 3) + (b.animate ? t * 0.08 : 0.0);
         double r = ring * (0.35 + 0.95 * Prim.rnd(k * 29 + 4));
         double x = r * Math.cos(a) + (b.animate ? 0.05 * Math.sin(t * 0.8 + k) : 0.0);
         double z = r * Math.sin(a);
         double y = (body + 0.3) * (1.0 - u);
         double env = Prim.sm(u / 0.1) * Prim.sm((1.0 - u) / 0.12);
         double al = 0.9 * env * AuraKit.near(cam, x, y, z, b.scale());
         double turn = b.animate ? t * (1.5 + Prim.rnd(k * 29 + 5) * 2.0) + k : k;
         al *= 0.25 + 0.75 * Math.abs(Math.sin(turn));
         if (!(al < 0.04)) {
            double s = 0.018 + 0.014 * Prim.rnd(k * 29 + 6);
            b.push();
            b.translate(x, y, z);
            b.rotateY(turn);
            b.rotateX(turn * 0.7 + k);
            int c = Col.withA(Col.mix(-1, b.c1, 0.4), al);
            b.quad(Prim.P(s, 0.0, 0.0), Prim.P(0.0, 0.0, s * 0.6), Prim.P(-s, 0.0, 0.0), Prim.P(0.0, 0.0, -s * 0.6), c);
            b.pop();
         }
      }
   }
}
