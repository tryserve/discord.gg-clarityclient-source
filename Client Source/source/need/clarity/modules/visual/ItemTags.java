package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class ItemTags extends ModuleBase {
   public static ItemTags INSTANCE;

   public ItemTags() {
      super("Item Tags", "Visual");
      INSTANCE = this;
   }

   public boolean showCount() {
      return this.getSetting("Show Count", true);
   }

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public float scale() {
      return Math.max(0.5F, Math.min(1.5F, this.getSetting("Scale", 1.0F)));
   }

   public float maxDistance() {
      return this.getSetting("Max Distance", 32.0F);
   }
}
