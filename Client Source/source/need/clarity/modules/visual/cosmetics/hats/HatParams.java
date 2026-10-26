package need.clarity.modules.visual.cosmetics.hats;

public record HatParams(int[] gradient, double centerAlpha, double edgeAlpha, double chinaSpin, double spin, boolean helmet, boolean bob) {
   public static HatParams defaults() {
      return new HatParams(new int[]{-16714844}, 1.0, 0.4, -0.2, 0.15, false, true);
   }
}
