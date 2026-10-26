package need.clarity.modules.utility;

import java.util.List;
import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ContainerScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class HoverTotem extends ModuleBase {
   private boolean pendingOffhand = false;
   private long offhandAt = -1L;
   private boolean pendingFill = false;
   private long fillAt = -1L;
   private int triggeredSlotIndex = -1;
   private final Random rng = new Random();

   public HoverTotem() {
      super("Hover Totem", "Utility");
   }

   @Override
   public void onDisable() {
      this.resetSwap();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.gameMode != null) {
         if (mc.screen instanceof AbstractContainerScreen<?> screen) {
            this.tickHoverSwap(mc, screen);
         } else {
            this.resetSwap();
         }
      }
   }

   private void tickHoverSwap(Minecraft mc, AbstractContainerScreen<?> screen) {
      Slot hovered = ContainerScreenHelper.getHoveredSlot(screen);
      boolean hoveringTotem = hovered != null && hovered.hasItem() && hovered.getItem().is(Items.TOTEM_OF_UNDYING);
      if (hoveringTotem) {
         int idx = menuIndex(screen, hovered);
         if (hovered.container instanceof Inventory && hovered.getContainerSlot() == 40) {
            this.resetSwap();
            return;
         }

         if (this.triggeredSlotIndex != -1 && idx != this.triggeredSlotIndex) {
            this.resetSwap();
         }

         long now = System.currentTimeMillis();
         if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && !this.pendingOffhand) {
            this.pendingOffhand = true;
            this.offhandAt = now + this.hoverDelayMs();
            this.triggeredSlotIndex = idx;
         }

         if (this.pendingOffhand && now >= this.offhandAt) {
            if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
               ContainerScreenHelper.swapToOffhand(mc.gameMode, screen.getMenu().containerId, idx, mc.player);
            }

            this.pendingOffhand = false;
            this.offhandAt = -1L;
         }

         if (!this.getSetting("Fill Hotbar", false)) {
            this.pendingFill = false;
            this.fillAt = -1L;
         } else if ("Empty".equals(this.getSetting("Fill Hotbar Mode", "Slot"))) {
            this.tickEmptyMode(mc, screen, idx, now);
         } else {
            this.tickSlotMode(mc, screen, idx, now);
         }
      } else {
         this.resetSwap();
      }
   }

   private void tickSlotMode(Minecraft mc, AbstractContainerScreen<?> screen, int slotIndex, long now) {
      if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
         this.pendingFill = false;
         this.fillAt = -1L;
      } else {
         int targetSlot = this.getSetting("Slot", 1) - 1;
         if (mc.player.getInventory().getItem(targetSlot).is(Items.TOTEM_OF_UNDYING)) {
            this.pendingFill = false;
            this.fillAt = -1L;
         } else {
            if (!this.pendingFill) {
               this.pendingFill = true;
               this.fillAt = now + this.hoverDelayMs();
            }

            if (now >= this.fillAt) {
               ContainerScreenHelper.swapToHotbar(mc.gameMode, screen.getMenu().containerId, slotIndex, targetSlot, mc.player);
               this.pendingFill = false;
               this.fillAt = -1L;
            }
         }
      }
   }

   private void tickEmptyMode(Minecraft mc, AbstractContainerScreen<?> screen, int slotIndex, long now) {
      if (!this.pendingFill) {
         this.pendingFill = true;
         this.fillAt = now + this.hoverDelayMs();
      }

      if (now >= this.fillAt) {
         ContainerScreenHelper.shiftClick(mc.gameMode, screen.getMenu().containerId, slotIndex, mc.player);
         this.pendingFill = false;
         this.fillAt = -1L;
      }
   }

   private static int menuIndex(AbstractContainerScreen<?> screen, Slot slot) {
      return screen.getMenu().slots.indexOf(slot);
   }

   private void resetSwap() {
      this.pendingOffhand = false;
      this.offhandAt = -1L;
      this.pendingFill = false;
      this.fillAt = -1L;
      this.triggeredSlotIndex = -1;
   }

   private long hoverDelayMs() {
      Object raw = this.getSetting("Hover Delay", null);
      long lo = 0L;
      long hi = 0L;
      if (raw instanceof List<?> list && list.size() >= 2) {
         lo = toLong(list.get(0));
         hi = toLong(list.get(1));
      } else {
         lo = toLong(raw);
         hi = lo;
      }

      if (hi < lo) {
         hi = lo;
      }

      return lo == hi ? lo : lo + (long)(this.rng.nextDouble() * (hi - lo + 1L));
   }

   private static long toLong(Object o) {
      if (o == null) {
         return 0L;
      } else if (o instanceof Number n) {
         return n.longValue();
      } else {
         try {
            return Long.parseLong(o.toString());
         } catch (NumberFormatException e) {
            return 0L;
         }
      }
   }
}
