package need.clarity.modules.utility;

import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KeyPearl extends ModuleBase {
   private final Minecraft mc = Minecraft.getInstance();
   private final Random rng = new Random();
   private KeyPearl.Phase phase = KeyPearl.Phase.IDLE;
   private long phaseStart = 0L;
   private long phaseDelay = 0L;
   private int phaseTicks = 0;
   private int originalSlot = -1;
   private int pearlSlot = -1;

   public KeyPearl() {
      super("Key Pearl", "Utility");
   }

   @Override
   public void onEnable() {
      this.reset();
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         this.phase = KeyPearl.Phase.PENDING;
      } else {
         this.finish();
      }
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         if (this.mc.player != null && this.mc.gameMode != null) {
            this.phaseTicks++;
            long now = System.currentTimeMillis();
            Inventory inv = this.mc.player.getInventory();
            switch (this.phase) {
               case PENDING:
                  this.pearlSlot = this.findPearlSlot(inv);
                  if (this.pearlSlot < 0) {
                     this.finish();
                     return;
                  }

                  this.originalSlot = InventoryHelper.getSelected(inv);
                  if (this.pearlSlot != this.originalSlot) {
                     InventoryHelper.setSelected(inv, this.pearlSlot);
                  }

                  this.setPhase(KeyPearl.Phase.WAIT_THROW, this.randomIn(this.getSetting("Swap Delay_lo", 0), this.getSetting("Swap Delay_hi", 50)));
                  if (this.phaseDelay <= 0L) {
                     this.doThrow();
                  }
                  break;
               case WAIT_THROW:
                  if (now - this.phaseStart >= this.phaseDelay) {
                     this.doThrow();
                  }
                  break;
               case WAIT_RESTORE:
                  if (this.phaseTicks >= 1 && now - this.phaseStart >= this.phaseDelay) {
                     this.restoreSlot();
                     this.finish();
                  }
            }
         } else {
            this.finish();
         }
      }
   }

   private void doThrow() {
      Inventory inv = this.mc.player.getInventory();
      if (InventoryHelper.getSelected(inv) == this.pearlSlot && inv.getItem(this.pearlSlot).is(Items.ENDER_PEARL)) {
         this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
         if (this.getSetting("Swap Back", true) && this.originalSlot >= 0 && this.originalSlot != this.pearlSlot) {
            this.setPhase(KeyPearl.Phase.WAIT_RESTORE, this.randomIn(this.getSetting("Throw Delay_lo", 50), this.getSetting("Throw Delay_hi", 100)));
         } else {
            this.originalSlot = -1;
            this.finish();
         }
      } else {
         this.restoreSlot();
         this.finish();
      }
   }

   private void restoreSlot() {
      if (this.mc.player != null && this.originalSlot >= 0 && InventoryHelper.getSelected(this.mc.player.getInventory()) != this.originalSlot) {
         InventoryHelper.setSelected(this.mc.player.getInventory(), this.originalSlot);
      }

      this.originalSlot = -1;
   }

   private void finish() {
      this.reset();
      if (this.getSetting("Auto Disable", true)) {
         this.setEnabled(false);
      }
   }

   private void reset() {
      this.phase = KeyPearl.Phase.IDLE;
      this.phaseTicks = 0;
      this.phaseDelay = 0L;
      this.originalSlot = -1;
      this.pearlSlot = -1;
   }

   private void setPhase(KeyPearl.Phase p, long delayMs) {
      this.phase = p;
      this.phaseStart = System.currentTimeMillis();
      this.phaseDelay = delayMs;
      this.phaseTicks = 0;
   }

   private int findPearlSlot(Inventory inv) {
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = inv.getItem(slot);
         if (!stack.isEmpty() && stack.is(Items.ENDER_PEARL)) {
            return slot;
         }
      }

      return -1;
   }

   private int randomIn(int lo, int hi) {
      return hi <= lo ? lo : lo + this.rng.nextInt(hi - lo + 1);
   }

   private enum Phase {
      IDLE,
      PENDING,
      WAIT_THROW,
      WAIT_RESTORE;
   }
}
