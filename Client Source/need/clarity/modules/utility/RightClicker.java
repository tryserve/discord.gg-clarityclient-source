package need.clarity.modules.utility;

import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RightClicker extends ModuleBase {
   private final Random random = new Random();
   private long lastTickNanos = 0L;
   private long budgetNanos = 0L;
   private long intervalNanos = 0L;
   private boolean wasActive = false;

   public RightClicker() {
      super("Right Clicker", "utility");
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         boolean physicalRightHeld = mc.options.keyUse.isDown();
         if (!physicalRightHeld) {
            this.wasActive = false;
            this.budgetNanos = 0L;
            this.lastTickNanos = 0L;
         } else if (!this.itemAllowed(mc)) {
            this.wasActive = false;
            this.budgetNanos = 0L;
            this.lastTickNanos = 0L;
         } else {
            long now = System.nanoTime();
            if (!this.wasActive) {
               this.wasActive = true;
               this.lastTickNanos = now;
               this.budgetNanos = 0L;
               this.intervalNanos = this.randomIntervalNanos();
            } else {
               long elapsed = now - this.lastTickNanos;
               this.lastTickNanos = now;
               if (elapsed > 200000000L) {
                  elapsed = 50000000L;
               }

               for (this.budgetNanos += elapsed; this.budgetNanos >= this.intervalNanos; this.intervalNanos = this.randomIntervalNanos()) {
                  this.injectClick(mc);
                  this.budgetNanos = this.budgetNanos - this.intervalNanos;
               }
            }
         }
      }
   }

   private void injectClick(Minecraft mc) {
      AccessorCompat.addClick(mc.options.keyUse);
   }

   private boolean itemAllowed(Minecraft mc) {
      boolean all = this.getSetting("All", false);
      boolean blocks = this.getSetting("Blocks", true);
      boolean expBot = this.getSetting("Experience Bottles", true);
      if (all) {
         return true;
      } else {
         ItemStack held = mc.player.getMainHandItem();
         if (held.isEmpty()) {
            return false;
         } else {
            return blocks && held.getItem() instanceof BlockItem ? true : expBot && held.is(Items.EXPERIENCE_BOTTLE);
         }
      }
   }

   private long randomIntervalNanos() {
      int lo = this.getSetting("CPS Low", 15);
      int hi = this.getSetting("CPS High", 18);
      if (lo <= 0) {
         lo = 1;
      }

      if (hi <= 0) {
         hi = 1;
      }

      if (hi < lo) {
         hi = lo;
      }

      int cps = lo + (hi > lo ? this.random.nextInt(hi - lo + 1) : 0);
      return 1000000000L / cps;
   }
}
