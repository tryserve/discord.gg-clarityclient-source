package need.clarity.modules.visual.cosmetics.target;

import java.util.function.DoubleUnaryOperator;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class SpinningRing implements Cosmetic {
   private static final double OUT_TILT = 0.42;
   private static final double IN_TILT = 0.78;
   private static final double IN_TILT_MAX = 0.83;
   private static final int TEETH = 12;
   private final TargetParams p;

   public SpinningRing() {
      this(TargetParams.defaults());
   }

   public SpinningRing(TargetParams p) {
      this.p = p;
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double intro = this.p.intro();
      double bulk = this.p.bulk();
      double slam = 1.0 - intro;
      double spin = (Math.PI * 2) * this.p.phase();
      double dir = this.p.dir();
      double bob = b.animate ? 0.1 * this.p.hh() * Math.sin(t * 0.8) : 0.0;
      double rIn = Math.max(this.p.hw() * 1.1 / Math.cos(0.83), this.p.hw() + 0.12 * bulk);
      double rOut = Math.max(this.p.hw() + 0.3 * bulk, rIn + 0.16 * bulk);
      double grow = 1.0 + slam * 0.8;
      this.hoop(b, bob, rOut * grow, 0.026 * bulk, 0.42 + 0.06 * (b.animate ? Math.sin(t * 0.9) : 0.0) + slam * 1.1, spin * 0.12, spin, dir, b.c2, b.c1, true, 32, 4, intro);
      this.hoop(b, -bob, rIn * grow, 0.023 * bulk, 0.78 + 0.05 * (b.animate ? Math.sin(t * 1.3 + 1.0) : 0.0) + slam * 1.1, 1.2 - spin * 0.2, -spin * 1.35, -dir, b.c1, b.c2, false, 0, 3, intro);
   }

   private void hoop(Builder b, double y, double R, double tube, double tilt, double prec, double spin, double rate, int body, int flare, boolean gear, int glowMajor, int minor, double vis) {
      b.push();
      b.translate(0.0, y, 0.0);
      b.rotateY(prec);
      b.rotateX(tilt);
      b.rotateY(spin);
      double[] cam = b.viewLocal();
      double scale = b.scale();
      double bulk = this.p.bulk();
      DoubleUnaryOperator comet = ux -> {
         double d = rate > 0.0 ? ux : 1.0 - ux;
         double kx = Math.exp(-4.5 * d) + 0.55 * Math.exp(-4.5 * TargetKit.frac(d + 0.5));
         return Math.min(1.0, kx);
      };
      DoubleUnaryOperator near = ux -> {
         double ax = (Math.PI * 2) * ux;
         return TargetKit.near(cam, R * Math.cos(ax), 0.0, R * Math.sin(ax), scale);
      };
      Prim.torus(b, 0.0, R, tube, gear ? 40 : 32, minor, (ux, v) -> {
         double kx = comet.applyAsDouble(ux);
         int cx = Col.mix(body, Col.tint(flare, 0.45), kx);
         cx = Col.shade(cx, 0.72 + 0.28 * Math.cos((v - 0.25) * (Math.PI * 2)));
         return Col.withA(cx, (0.34 + 0.62 * kx) * near.applyAsDouble(ux) * vis);
      });
      if (gear) {
         Prim.torus(b, 0.0, R, tube * 3.0, glowMajor, 3, (ux, v) -> {
            double kx = comet.applyAsDouble(ux);
            return Col.withA(Col.mix(body, flare, kx), (0.05 + 0.13 * kx) * near.applyAsDouble(ux) * vis);
         });
         double len = 0.075 * bulk;
         double rb = tube * 0.95;

         for (int i = 0; i < 12; i++) {
            double u = i / 12.0;
            double a = (Math.PI * 2) * u;
            double k = comet.applyAsDouble(u);
            double nr = near.applyAsDouble(u);
            double[] base = Prim.P((R + tube * 0.8) * Math.cos(a), 0.0, (R + tube * 0.8) * Math.sin(a));
            double[] tip = Prim.P((R + tube + len) * Math.cos(a), 0.0, (R + tube + len) * Math.sin(a));
            int c = Col.mix(body, Col.tint(flare, 0.5), 0.4 + 0.6 * k);
            Prim.spike(b, base, tip, rb, 3, (uu, vv) -> Col.withA(c, (0.75 - 0.35 * vv) * nr * vis));
         }
      }

      b.pop();
   }
}
