package need.clarity.modules.visual.cosmetics.target;

import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class ChevronLockOn implements Cosmetic {
   private static final int POINTS = 4;
   private static final int LAYERS = 3;
   private static final double SWEEP = 0.62;
   private static final double MARCH = 0.7;
   private final TargetParams p;

   public ChevronLockOn() {
      this(TargetParams.defaults());
   }

   public ChevronLockOn(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double slam = 1.0 - intro;
      double turn = (Math.PI * 2) * this.p.phase() * 0.25 - slam * 1.4 * this.p.dir();
      double bob = b.animate ? 0.03 * bulk * Math.sin(t * 1.6) : 0.0;
      double arm = 0.2 * bulk + 0.17 * Math.min(this.p.width(), 1.0);
      double wall = TargetKit.clamp(0.24 * this.p.height(), 0.14, 0.7) * bulk;
      double gap = 0.15 * bulk;
      double baseR = this.p.hw() + 0.16 * bulk;
      double bar = 0.017 * bulk;
      double post = 0.03 * bulk;
      double outX = Math.sin(0.62);
      double side = Math.cos(0.62);
      double[] cam = b.viewLocal();
      double scale = b.scale();
      double cycle = b.animate ? TargetKit.frac(t / 0.7) : 0.0;

      for (int i = 0; i < 4; i++) {
         double a = turn + (Math.PI * 2) * i / 4.0;
         double[] r = new double[]{Math.cos(a), 0.0, Math.sin(a)};
         double[] tg = new double[]{-Math.sin(a), 0.0, Math.cos(a)};

         for (int l = 0; l < 3; l++) {
            double peak = (2 - l) / 3.0;
            double d = TargetKit.frac(cycle - peak);
            double lit = b.animate ? Math.exp(-5.0 * d) : (l == 0 ? 1.0 : 0.3);
            double rad = baseR + l * gap - 0.05 * bulk * lit + slam * 1.6 * bulk;
            double len = arm;
            double x = len * outX;
            double y = len * side;
            double[] tip = new double[]{r[0] * rad, bob, r[2] * rad};
            double[] endA = new double[]{r[0] * (rad + x) + tg[0] * y, bob, r[2] * (rad + x) + tg[2] * y};
            double[] endB = new double[]{r[0] * (rad + x) - tg[0] * y, bob, r[2] * (rad + x) - tg[2] * y};
            double vis = intro * TargetKit.near(cam, tip[0], tip[1], tip[2], scale) * (1.0 - 0.14 * l);
            if (!(vis < 0.01)) {
               double mixL = l / 2.0;
               int base = Col.mix(b.c1, b.c2, 0.85 * mixL);
               int hot = Col.tint(base, 0.25 + 0.55 * lit);
               int tipC = Col.withA(hot, Math.min(1.0, 0.7 + 0.3 * lit) * vis);
               int endC = Col.withA(Col.mix(base, b.c2, 0.5), (0.62 + 0.25 * lit) * vis);
               int plateTip = Col.withA(base, (0.42 + 0.36 * lit) * vis);
               int plateEnd = Col.withA(base, 0.16 * vis);
               double h2 = wall * 0.5;

               for (double[] end : new double[][]{endA, endB}) {
                  double[] tl = new double[]{tip[0], tip[1] - h2, tip[2]};
                  double[] th = new double[]{tip[0], tip[1] + h2, tip[2]};
                  double[] el = new double[]{end[0], end[1] - h2, end[2]};
                  double[] eh = new double[]{end[0], end[1] + h2, end[2]};
                  b.quad(tl, el, eh, th, plateTip, plateEnd, plateEnd, plateTip);
                  TargetKit.beam(b, tl, el, bar, tipC, endC, false);
                  TargetKit.beam(b, th, eh, bar, tipC, endC, false);
               }

               TargetKit.beam(b, Prim.P(tip[0], tip[1] - h2, tip[2]), Prim.P(tip[0], tip[1] + h2, tip[2]), post, Col.shade(tipC, 0.9), Col.tint(tipC, 0.25), true);
            }
         }
      }
   }
}
