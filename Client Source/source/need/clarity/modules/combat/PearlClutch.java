package need.clarity.modules.combat;

import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PearlClutch extends ModuleBase {
   private final Minecraft mc = Minecraft.getInstance();
   private final Random rng = new Random();
   private PearlClutch.Phase phase = PearlClutch.Phase.IDLE;
   private long phaseStart = 0L;
   private int originalSlot = -1;

   public PearlClutch() {
      super("Pearl Clutch", "Combat");
   }

   @Override
   public void onEnable() {
      this.phase = PearlClutch.Phase.IDLE;
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         int pearlSlot = this.findPearlSlot(this.mc.player.getInventory());
         if (pearlSlot < 0) {
            this.disable();
         } else {
            this.originalSlot = InventoryHelper.getSelected(this.mc.player.getInventory());
            if (pearlSlot != this.originalSlot) {
               InventoryHelper.setSelected(this.mc.player.getInventory(), pearlSlot);
            }

            int swapLo = this.getSetting("Swap Delay_lo", 0);
            int swapHi = this.getSetting("Swap Delay_hi", 50);
            long delay = this.randomIn(swapLo, swapHi);
            if (delay <= 0L) {
               this.doThrow();
            } else {
               this.setPhase(PearlClutch.Phase.WAIT_THROW);
            }
         }
      } else {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      this.phase = PearlClutch.Phase.IDLE;
      this.originalSlot = -1;
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         if (this.mc.player == null) {
            this.disable();
         } else {
            long now = System.currentTimeMillis();
            switch (this.phase) {
               case WAIT_THROW:
                  int lox = this.getSetting("Swap Delay_lo", 0);
                  int hix = this.getSetting("Swap Delay_hi", 50);
                  if (now - this.phaseStart >= this.randomIn(lox, hix)) {
                     this.doThrow();
                  }
                  break;
               case WAIT_RESTORE:
                  int lo = this.getSetting("Throw Delay_lo", 50);
                  int hi = this.getSetting("Throw Delay_hi", 100);
                  if (now - this.phaseStart >= this.randomIn(lo, hi)) {
                     this.restoreSlot();
                     this.disable();
                  }
                  break;
               case DONE:
                  this.disable();
            }
         }
      }
   }

   private void doThrow() {
      if (this.mc.player != null && this.mc.gameMode != null) {
         this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
         if (this.getSetting("Anti Ghost", true)) {
            int sel = InventoryHelper.getSelected(this.mc.player.getInventory());
            this.mc.player.connection.send(new ServerboundSetCarriedItemPacket(sel));
         }

         boolean swapBack = this.getSetting("Swap Back", true);
         if (swapBack && this.originalSlot >= 0) {
            this.setPhase(PearlClutch.Phase.WAIT_RESTORE);
         } else if (this.getSetting("Auto Disable", true)) {
            this.phase = PearlClutch.Phase.DONE;
         }
      } else {
         this.disable();
      }
   }

   private void restoreSlot() {
      if (this.mc.player != null && this.originalSlot >= 0) {
         InventoryHelper.setSelected(this.mc.player.getInventory(), this.originalSlot);
      }

      this.originalSlot = -1;
   }

   private void disable() {
      if (this.getSetting("Auto Disable", true)) {
         this.setEnabled(false);
      }

      this.phase = PearlClutch.Phase.IDLE;
      this.originalSlot = -1;
   }

   private void setPhase(PearlClutch.Phase p) {
      this.phase = p;
      this.phaseStart = System.currentTimeMillis();
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
      WAIT_THROW,
      WAIT_RESTORE,
      DONE;
   }
}
