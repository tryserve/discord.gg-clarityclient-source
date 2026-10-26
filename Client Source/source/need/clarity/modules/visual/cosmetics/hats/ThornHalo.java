package need.clarity.modules.visual.cosmetics.hats;

import java.util.function.DoubleFunction;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class ThornHalo implements Cosmetic {
   static final double LIFT = 0.27;
   static final double R = 0.3;
   static final double VINE = 0.02;
   static final int PATH = 48;
   static final int SIDES = 5;
   static final int THORNS = 16;
   private static final double TAU = Math.PI * 2;
   private final HatParams p;

   public ThornHalo() {
      this(HatParams.defaults());
   }

   public ThornHalo(HatParams p) {
      this.p = p;
   }

   private static double[] vine(int i, double phase) {
      double th = i / 48.0 * (Math.PI * 2);
      double r = 0.3 + 0.014 * Math.sin(5.0 * th + phase);
      return Prim.P(r * Math.cos(th), 0.022 * Math.sin(3.0 * th + phase * 0.7), r * Math.sin(th));
   }

   private static double vineRadius(int i) {
      return 0.02 * (1.0 + 0.18 * Math.sin(7.0 * (i / 48.0) * (Math.PI * 2)));
   }

   @Override
   public void build(Builder b) {
      double t = b.animate ? b.time : 0.0;
      double bob = this.p.bob() && b.animate ? Math.sin(t * 1.8) * 0.016 : 0.0;
      double phase = b.animate ? t * 0.9 : 0.0;
      double turn = b.animate ? t * this.p.spin() * (Math.PI * 2) : 0.0;
      int c1 = b.c1;
      int c2 = b.c2;
      b.push();
      b.translate(0.0, 0.27 + bob, 0.0);
      b.rotateY(turn);
      DoubleFunction<Integer> vineCol = g -> Col.mix(c1, c2, 0.5 + 0.5 * Math.sin(g * (Math.PI * 2) * 2.0));

      for (int i = 0; i < 48; i++) {
         int seg = i;
         Prim.Shade s = (u, v) -> {
            double g = (seg + v) / 48.0;
            return Col.shade(vineCol.apply(g), 0.85 + 0.15 * Math.cos(u * (Math.PI * 2)));
         };
         Prim.taperedTube(b, vine(i, phase), vine(i + 1, phase), vineRadius(i), vineRadius(i + 1), 5, s, false);
      }

      for (int k = 0; k < 16; k++) {
         int idx = k * 48 / 16;
         double th = idx / 48.0 * (Math.PI * 2);
         boolean tall = k % 2 == 0;
         double elev = Math.toRadians(tall ? 42.0 : 12.0);
         double len = (tall ? 0.135 : 0.085) * (b.animate ? 1.0 + 0.1 * Math.sin(t * 2.4 + k * 0.9) : 1.0);
         double[] base = vine(idx, phase);
         double[] dir = new double[]{Math.cos(th) * Math.cos(elev), Math.sin(elev), Math.sin(th) * Math.cos(elev)};
         double[] tip = Prim.add(base, dir[0] * len, dir[1] * len, dir[2] * len);
         int root = vineCol.apply(idx / 48.0);
         int glow = Col.tint(c2, tall ? 0.65 : 0.5);
         Prim.spike(b, base, tip, 0.021, 4, (u, v) -> Col.mix(root, glow, Prim.sm(v)));
      }

      Prim.ring(b, -0.012, 0.27999999999999997, 0.44, 36, (u, v) -> Col.withA(Col.tint(vineCol.apply(u), 0.45), 0.34 * (1.0 - v) * (1.0 - v)));
      b.pop();
   }
}
