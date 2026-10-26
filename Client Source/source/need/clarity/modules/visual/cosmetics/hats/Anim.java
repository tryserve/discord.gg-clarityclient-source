package need.clarity.modules.visual.cosmetics.hats;

final class Anim {
   static final double TAU = Math.PI * 2;

   private Anim() {
   }

   static double frac(double x) {
      return x - Math.floor(x);
   }

   static double pulse(double t, double period, double offset, double width) {
      double x = frac(t / period + offset);
      return x < width ? 0.5 - 0.5 * Math.cos(x / width * (Math.PI * 2)) : 0.0;
   }

   static double sweep(double g, double pos, double width) {
      double d = (g - pos) / width;
      return Math.exp(-d * d);
   }
}
