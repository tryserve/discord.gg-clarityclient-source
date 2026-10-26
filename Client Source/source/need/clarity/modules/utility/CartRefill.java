package need.clarity.modules.utility;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.HashedStack;
import net.minecraft.network.HashedPatchMap.HashGenerator;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CartRefill extends ModuleBase {
   private static final Random rng = new Random();
   private CartRefill.Stage stage = CartRefill.Stage.IDLE;
   private long stageReadyAt = 0L;
   private long nextMoveAt = 0L;

   public CartRefill() {
      super("Cart Refill", "Utility");
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (this.stage != CartRefill.Stage.IDLE && mc.player != null && this.isSilent()) {
         this.sendClose(mc);
      }

      MoveFix.setMovementFrozen(false);
      this.stage = CartRefill.Stage.IDLE;
   }

   private boolean isSilent() {
      return "Silent".equals(this.getSetting("Mode", "Silent"));
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (this.isSilent()) {
            this.tickSilent(mc);
         } else {
            this.tickInventory(mc);
         }
      }
   }

   private void tickInventory(Minecraft mc) {
      MoveFix.setMovementFrozen(false);
      this.stage = CartRefill.Stage.IDLE;
      if (!(mc.screen instanceof InventoryScreen)) {
         this.nextMoveAt = 0L;
      } else {
         long now = now();
         if (this.nextMoveAt == 0L) {
            this.nextMoveAt = now + this.pickDelay("Open Delay_lo", "Open Delay_hi");
         } else if (now >= this.nextMoveAt) {
            int[] move = this.nextMove(mc);
            if (move != null) {
               this.sendSwap(mc, move[0], move[1]);
               this.nextMoveAt = now + this.pickDelay("Move Delay_lo", "Move Delay_hi");
            }
         }
      }
   }

   private void tickSilent(Minecraft mc) {
      if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) {
         if (this.stage != CartRefill.Stage.IDLE) {
            this.abort(mc);
         }
      } else {
         switch (this.stage) {
            case IDLE:
               if (this.nextMove(mc) != null) {
                  this.stageReadyAt = now() + this.pickDelay("Open Delay_lo", "Open Delay_hi");
                  this.stage = CartRefill.Stage.WAIT_OPEN;
               }
               break;
            case WAIT_OPEN:
               if (now() >= this.stageReadyAt) {
                  this.stage = CartRefill.Stage.MOVING;
                  this.doSilentMove(mc);
               }
               break;
            case MOVING:
               if (now() >= this.stageReadyAt) {
                  this.doSilentMove(mc);
               }
               break;
            case WAIT_CLOSE:
               if (now() >= this.stageReadyAt) {
                  this.sendClose(mc);
                  this.stage = CartRefill.Stage.IDLE;
               }
         }

         boolean freeze = this.getSetting("Bypass", true);
         MoveFix.setMovementFrozen(freeze && this.stage != CartRefill.Stage.IDLE);
      }
   }

   private void doSilentMove(Minecraft mc) {
      int[] move = this.nextMove(mc);
      if (move == null) {
         this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
         this.stage = CartRefill.Stage.WAIT_CLOSE;
      } else {
         this.sendSwap(mc, move[0], move[1]);
         if (this.nextMove(mc) == null) {
            this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
            this.stage = CartRefill.Stage.WAIT_CLOSE;
         } else {
            this.stageReadyAt = now() + this.pickDelay("Move Delay_lo", "Move Delay_hi");
         }
      }
   }

   private int[] nextMove(Minecraft mc) {
      Inventory inv = mc.player.getInventory();
      int src = findCartInMain(inv);
      if (src < 0) {
         return null;
      }

      if ("Slot".equals(this.getSetting("Target", "Empty Slots"))) {
         int h = Math.max(1, Math.min(9, this.getSetting("Slot", 1))) - 1;
         ItemStack cur = inv.getItem(h);
         if (cur.is(Items.TNT_MINECART)) {
            return null;
         } else {
            return !cur.isEmpty() && this.getSetting("Only If Empty", true) ? null : new int[]{src, h};
         }
      } else {
         for (int h = 0; h < 9; h++) {
            if (inv.getItem(h).isEmpty()) {
               return new int[]{src, h};
            }
         }

         return null;
      }
   }

   private static int findCartInMain(Inventory inv) {
      for (int i = 9; i < 36; i++) {
         if (inv.getItem(i).is(Items.TNT_MINECART)) {
            return i;
         }
      }

      return -1;
   }

   private void sendSwap(Minecraft mc, int invSlot, int hotbar) {
      int containerId = mc.player.inventoryMenu.containerId;
      int stateId = mc.player.inventoryMenu.getStateId();
      HashGenerator hasher = mc.player.connection.decoratedHashOpsGenenerator();
      Inventory inv = mc.player.getInventory();
      ItemStack cart = inv.getItem(invSlot);
      ItemStack old = inv.getItem(hotbar);
      Int2ObjectMap<HashedStack> changed = new Int2ObjectArrayMap();
      changed.put(invSlot, HashedStack.create(old.isEmpty() ? ItemStack.EMPTY : old, hasher));
      changed.put(36 + hotbar, HashedStack.create(cart, hasher));
      mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, (short)invSlot, (byte)hotbar, ContainerInput.SWAP, changed, HashedStack.EMPTY));
      inv.setItem(invSlot, old.isEmpty() ? ItemStack.EMPTY : old.copy());
      inv.setItem(hotbar, cart.copy());
   }

   private void sendClose(Minecraft mc) {
      mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
   }

   private void abort(Minecraft mc) {
      if (this.stage != CartRefill.Stage.IDLE && this.stage != CartRefill.Stage.WAIT_OPEN) {
         this.sendClose(mc);
      }

      MoveFix.setMovementFrozen(false);
      this.stage = CartRefill.Stage.IDLE;
   }

   private int pickDelay(String lowKey, String highKey) {
      int low = this.getSetting(lowKey, 0);
      int high = this.getSetting(highKey, 0);
      return low >= high ? Math.max(0, low) : low + rng.nextInt(high - low + 1);
   }

   private static long now() {
      return System.currentTimeMillis();
   }

   private enum Stage {
      IDLE,
      WAIT_OPEN,
      MOVING,
      WAIT_CLOSE;
   }
}
