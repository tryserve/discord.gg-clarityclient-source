package need.clarity.modules.visual.cosmetics.aura;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class Runes implements Cosmetic {
   static final int MAX = 12;
   private static final double R = 0.15;
   private static final double HALF_T = 0.02;
   private static final double GLOW = 0.25;
   private static final double[][][] GLYPHS = new double[][][]{
      {{0.0, -0.55, 0.0, 0.55}, {0.0, 0.55, 0.42, 0.22}, {0.0, 0.2, 0.42, -0.13}},
      {{0.0, 0.55, -0.42, -0.35}, {0.0, 0.55, 0.42, -0.35}, {-0.42, -0.35, 0.42, -0.35}},
      {{-0.42, -0.5, 0.42, 0.5}, {-0.42, 0.5, 0.42, -0.5}},
      {{0.0, 0.55, 0.4, 0.0}, {0.4, 0.0, 0.0, -0.55}, {0.0, -0.55, -0.4, 0.0}, {-0.4, 0.0, 0.0, 0.55}},
      {{-0.36, 0.52, 0.22, 0.16}, {0.22, 0.16, -0.22, -0.16}, {-0.22, -0.16, 0.36, -0.52}},
      {{0.0, -0.55, 0.0, 0.55}, {-0.38, 0.18, 0.38, 0.18}, {-0.26, -0.28, 0.26, -0.28}}
   };
   private final AuraParams p;

   public Runes() {
      this(AuraParams.defaults());
   }

   public Runes(AuraParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      int n = this.p.items(12);
      int d = this.p.detail(n);
      double t = b.animate ? b.time : 0.0;
      double radius = this.p.radius();

      for (int k = 0; k < n; k++) {
         double phi = this.p.angle(k, n);
         double y = (k % 2 == 0 ? 0.08 : -0.08) + 0.03 * Math.sin(t * 1.4 + k * 1.3);
         b.push();
         b.translate(radius * Math.sin(phi), y, radius * Math.cos(phi));
         b.rotateY(phi);
         b.rotateX(-0.1);
         double f = AuraKit.nearOrigin(b);
         if (f > 0.02) {
            tablet(b, k, d, f, AuraKit.wave(b, 2.2, k * 1.3));
         }

         b.pop();
      }
   }

   private static void tablet(Builder b, int k, int d, double f, double pulse) {
      int c1 = b.c1;
      int c2 = b.c2;
      int slab = Col.dark(Col.mix(c1, c2, 0.2), 0.45);
      int rim = Col.mix(c2, -1, 0.2 + 0.25 * pulse);
      int glyph = Col.mix(Col.tint(c2, 0.3), -1, 0.15 + 0.45 * pulse);
      b.push();
      b.rotateX(Math.PI / 2);
      Prim.prism(b, -0.02, 0.02, 0.15, 6, 0.0, AuraKit.fade((u, v) -> Col.mix(slab, Col.dark(slab, 0.2), v), f), true);
      Prim.Shade rimShade = AuraKit.fade((u, v) -> Col.mix(Col.dark(rim, 0.35), rim, v), f);
      Prim.ring(b, 0.022, 0.12, 0.1425, 6, rimShade);
      if (d >= 1) {
         Prim.ring(b, -0.022, 0.12, 0.1425, 6, rimShade);
      }

      b.pop();
      double[][] strokes = GLYPHS[k % GLYPHS.length];
      int lo = AuraKit.fade(Col.mix(c2, c1, 0.35), f);
      int hi = AuraKit.fade(glyph, f);

      for (double[] s : strokes) {
         stroke(b, s, 0.024, lo, hi);
         if (d >= 1) {
            stroke(b, s, -0.024, lo, hi);
         }
      }

      int centre = AuraKit.fade(Col.withA(Col.tint(c2, 0.3), 0.26 + 0.18 * pulse), f);
      int edge = Col.withA(c2, 0.0);
      halo(b, 0.25, 0.045, centre, edge);
      if (d >= 1) {
         halo(b, 0.25, -0.045, centre, edge);
      }
   }

   private static void stroke(Builder b, double[] s, double w, int c0, int c1) {
      double x0 = s[0] * 0.15;
      double y0 = s[1] * 0.15;
      double x1 = s[2] * 0.15;
      double y1 = s[3] * 0.15;
      double dx = x1 - x0;
      double dy = y1 - y0;
      double len = Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-9)) {
         double nx = -dy / len * 0.017;
         double ny = dx / len * 0.017;
         b.quad(Prim.P(x0 + nx, y0 + ny, w), Prim.P(x0 - nx, y0 - ny, w), Prim.P(x1 - nx, y1 - ny, w), Prim.P(x1 + nx, y1 + ny, w), c0, c0, c1, c1);
      }
   }

   private static void halo(Builder b, double r, double w, int c, int e) {
      double[] o = Prim.P(0.0, 0.0, w);

      for (int i = 0; i < 6; i++) {
         double a0 = i * Math.PI / 3.0;
         double a1 = (i + 1) * Math.PI / 3.0;
         b.tri(o, Prim.P(r * Math.cos(a0), r * Math.sin(a0), w), Prim.P(r * Math.cos(a1), r * Math.sin(a1), w), c, e, e);
      }
   }
}
