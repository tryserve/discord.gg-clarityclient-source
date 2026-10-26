package need.clarity.modules.visual.cosmetics;

public final class Col {
   public static final int WHITE = -1;

   private Col() {
   }

   public static int a(int c) {
      return c >>> 24 & 0xFF;
   }

   public static int r(int c) {
      return c >>> 16 & 0xFF;
   }

   public static int g(int c) {
      return c >>> 8 & 0xFF;
   }

   public static int b(int c) {
      return c & 0xFF;
   }

   public static int argb(int a, int r, int g, int b) {
      return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   public static int clamp(int v) {
      return v < 0 ? 0 : Math.min(v, 255);
   }

   public static double clamp01(double v) {
      return v < 0.0 ? 0.0 : Math.min(v, 1.0);
   }

   public static int mix(int c0, int c1, double t) {
      t = clamp01(t);
      return argb((int)(a(c0) + (a(c1) - a(c0)) * t), (int)(r(c0) + (r(c1) - r(c0)) * t), (int)(g(c0) + (g(c1) - g(c0)) * t), (int)(b(c0) + (b(c1) - b(c0)) * t));
   }

   public static int alpha(int c, double k) {
      return argb((int)(a(c) * k), r(c), g(c), b(c));
   }

   public static int withA(int c, double a) {
      return argb((int)(255.0 * clamp01(a)), r(c), g(c), b(c));
   }

   public static int shade(int c, double k) {
      return argb(a(c), (int)(r(c) * k), (int)(g(c) * k), (int)(b(c) * k));
   }

   public static int tint(int c, double t) {
      return mix(c, c & 0xFF000000 | 16777215, t);
   }

   public static int dark(int c, double t) {
      return mix(c, c & 0xFF000000, t);
   }
}
