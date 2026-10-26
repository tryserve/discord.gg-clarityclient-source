package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class LightPillar implements Cosmetic {
   private static final int OUTER_SEGS = 28;
   private static final int INNER_SEGS = 18;
   private static final int RING_SEGS = 28;
   private static final int DISC_SEGS = 24;
   private static final int CURTAINS = 6;
   private static final int MOTES = 18;
   private static final double PULSE = 1.7;
   private final TargetParams p;

   public LightPillar() {
      this(TargetParams.defaults());
   }

   public LightPillar(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double scale = b.scale() > 1.0E-6 ? b.scale() : 1.0;
      double spin = (Math.PI * 2) * this.p.phase();
      double r = this.p.hw() + 0.16 * bulk;
      double feet = -0.5 * this.p.height() / scale;
      double full = this.p.height() / scale + 2.6 * bulk;
      double rise = Prim.sm(intro);
      double height = full * (0.12 + 0.88 * rise);
      double top = feet + height;
      double vis = 0.35 + 0.65 * intro;
      double[] cam = b.viewLocal();
      boolean inside = cam != null && Math.hypot(cam[0], cam[2]) < r * 1.05 && cam[1] > feet - 0.2 && cam[1] < top;
      double thin = inside ? 0.3 : 1.0;
      int cBase = b.c1;
      int cTop = Col.mix(b.c2, Col.tint(b.c2, 0.4), 0.5);
      Prim.cylinder(b, feet, top, r, r * 0.86, 28, (u, v) -> {
         double streak = 0.68 + 0.32 * Math.sin((u * 7.0 + spin * 0.6) * (Math.PI * 2));
         double climb = 0.86 + 0.14 * Math.sin(v * 16.0 - t * 3.0);
         double ax = 0.36 * Math.pow(1.0 - v, 1.5) * streak * climb + 0.04 * (1.0 - v);
         return Col.withA(Col.mix(cBase, cTop, Math.pow(v, 0.8)), ax * vis * thin);
      }, false, false);
      Prim.cylinder(b, feet, top * 0.94 + feet * 0.06, r * 0.52, r * 0.4, 18, (u, v) -> {
         double streak = 0.72 + 0.28 * Math.sin((u * 5.0 - spin * 0.9) * (Math.PI * 2));
         double ax = 0.5 * Math.pow(1.0 - v, 1.2) * streak;
         return Col.withA(Col.tint(Col.mix(cBase, cTop, v), 0.25), ax * vis * thin);
      }, false, false);
      double coreHalf = 0.028 * bulk;
      TargetKit.beam(
         b,
         Prim.P(0.0, feet, 0.0),
         Prim.P(0.0, top * 0.93 + feet * 0.07, 0.0),
         coreHalf,
         Col.withA(Col.tint(cBase, 0.7), 0.85 * vis * (inside ? 0.4 : 1.0)),
         Col.withA(Col.tint(cTop, 0.5), 0.0),
         false
      );
      double twist = 0.9 * this.p.dir();

      for (int i = 0; i < 6; i++) {
         double a0 = spin * 0.45 + (Math.PI * 2) * i / 6.0;
         double a1 = a0 + twist;
         double w = 0.05 * bulk / Math.max(r, 0.2);
         double rt = r * 0.8;
         double hi = feet + height * 0.82;
         int cb = Col.withA(Col.tint(cBase, 0.35), 0.3 * vis * thin);
         int ct = Col.withA(cTop, 0.0);
         b.quad(
            Prim.P(r * 0.98 * Math.cos(a0 - w), feet, r * 0.98 * Math.sin(a0 - w)),
            Prim.P(r * 0.98 * Math.cos(a0 + w), feet, r * 0.98 * Math.sin(a0 + w)),
            Prim.P(rt * Math.cos(a1 + w), hi, rt * Math.sin(a1 + w)),
            Prim.P(rt * Math.cos(a1 - w), hi, rt * Math.sin(a1 - w)),
            cb,
            cb,
            ct,
            ct
         );
      }

      double lift = 0.02 / scale;
      double breathe = b.animate ? 0.85 + 0.15 * Math.sin(t * 2.6) : 1.0;
      Prim.disc(b, feet + lift, r * 1.05, 24, (u, v) -> Col.withA(Col.tint(cBase, 0.25 * (1.0 - v)), 0.62 * Math.pow(1.0 - v, 1.1) * breathe * vis));

      for (int k = 0; k < 2; k++) {
         double c = b.animate ? TargetKit.frac(t / 1.7 + 0.5 * k) : 0.25 + 0.5 * k;
         double rr = r * (0.7 + 1.5 * c);
         double wd = 0.07 * bulk * (1.0 - 0.5 * c);
         double a = 0.6 * Math.pow(1.0 - c, 1.6) * Prim.sm(c * 8.0) * vis;
         int col = Col.withA(Col.tint(cBase, 0.3), a);
         Prim.ring(b, feet + lift * 1.5, rr - wd, rr + wd, 28, (u, v) -> Col.withA(col, Col.a(col) / 255.0 * (1.0 - Math.abs(2.0 * v - 1.0) * 0.7)));
      }

      for (int k = 0; k < 2; k++) {
         double c = b.animate ? TargetKit.frac(t / 3.4 + 0.5 * k) : 0.3 + 0.4 * k;
         double y = feet + height * c;
         double a = 0.55 * Math.sin(c * Math.PI) * (1.0 - c) * vis * thin;
         int col = Col.withA(Col.tint(Col.mix(cBase, cTop, c), 0.45), a);
         Prim.ring(b, y, r * 1.01 - 0.04 * bulk, r * 1.01, 28, (u, v) -> Col.withA(col, Col.a(col) / 255.0 * (0.25 + 0.75 * v)));
      }

      for (int i = 0; i < 18; i++) {
         double life = b.animate ? TargetKit.frac(t * (0.2 + 0.28 * Prim.rnd(i + 40)) + Prim.rnd(i + 80)) : Prim.rnd(i + 80);
         double ang = (Math.PI * 2) * Prim.rnd(i) + spin * 0.3 + life * 1.5;
         double rad = r * (0.2 + 0.7 * Prim.rnd(i + 20));
         double x = rad * Math.cos(ang);
         double z = rad * Math.sin(ang);
         double y = feet + 0.05 + life * height * 0.92;
         double s = 0.028 * bulk * (1.0 - 0.45 * life) * (0.7 + 0.6 * Prim.rnd(i + 60));
         double a = Math.sin(life * Math.PI) * 0.85 * vis * TargetKit.near(cam, x, y, z, scale);
         if (!(a < 0.02)) {
            int c = Col.withA(Col.tint(Col.mix(cBase, cTop, life), 0.55), a);
            b.quad(Prim.P(x - s, y, z), Prim.P(x, y + s * 1.6, z), Prim.P(x + s, y, z), Prim.P(x, y - s * 1.6, z), c);
            b.quad(Prim.P(x, y, z - s), Prim.P(x, y + s * 1.6, z), Prim.P(x, y, z + s), Prim.P(x, y - s * 1.6, z), c);
         }
      }
   }
}
