package need.clarity.modules.visual.cosmetics.aura;

public record LightningParams(int count, double reach, double rate, int budget) {
   public static LightningParams defaults() {
      return new LightningParams(6, 1.2, 20.0, 600);
   }

   public LightningParams withBudget(int b) {
      return new LightningParams(this.count, this.reach, this.rate, b);
   }

   public int lod() {
      int n = Math.max(1, Math.min(12, this.count));

      for (int l = 0; l < 4; l++) {
         if (LightningArcs.quadsPerBolt(l) * n <= this.budget) {
            return l;
         }
      }

      return 3;
   }

   public int bolts() {
      int n = Math.max(1, Math.min(12, this.count));
      int fit = this.budget / LightningArcs.quadsPerBolt(3);
      return Math.max(1, Math.min(n, fit));
   }
}
