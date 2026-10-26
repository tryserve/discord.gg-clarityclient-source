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

public class SilentTotem extends ModuleBase {
   private SilentTotem.Stage stage = SilentTotem.Stage.IDLE;
   private long stageReadyAt = 0L;
   private static final Random rng = new Random();

   public SilentTotem() {
      super("Silent Totem", "utility");
   }

   @Override
   public void onDisable() {
      MoveFix.setMovementFrozen(false);
      this.stage = SilentTotem.Stage.IDLE;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null) {
            if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) {
               if (this.stage != SilentTotem.Stage.IDLE) {
                  this.abort(mc);
               }
            } else {
               switch (this.stage) {
                  case IDLE:
                     this.tickIdle(mc);
                     break;
                  case WAIT_OPEN:
                     this.tickWaitOpen(mc);
                     break;
                  case WAIT_CLOSE:
                     this.tickWaitClose(mc);
               }

               boolean freeze = this.getSetting("Bypass", true);
               MoveFix.setMovementFrozen(freeze && this.stage != SilentTotem.Stage.IDLE);
            }
         }
      }
   }

   private void tickIdle(Minecraft mc) {
      if (!this.hasTotemInOffhand(mc)) {
         if (this.findTotemInvSlot(mc) != -1) {
            this.stageReadyAt = now() + this.pickDelay("Open Delay_lo", "Open Delay_hi");
            this.stage = SilentTotem.Stage.WAIT_OPEN;
         }
      }
   }

   private void tickWaitOpen(Minecraft mc) {
      if (now() >= this.stageReadyAt) {
         if (this.hasTotemInOffhand(mc)) {
            this.abort(mc);
         } else {
            int invSlot = this.findTotemInvSlot(mc);
            if (invSlot == -1) {
               this.stage = SilentTotem.Stage.IDLE;
            } else {
               int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
               this.sendSwapWithOffhand(mc, containerSlot, invSlot);
               this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
               this.stage = SilentTotem.Stage.WAIT_CLOSE;
            }
         }
      }
   }

   private void tickWaitClose(Minecraft mc) {
      if (now() >= this.stageReadyAt) {
         this.sendClose(mc);
         this.stage = SilentTotem.Stage.IDLE;
      }
   }

   private void sendSwapWithOffhand(Minecraft mc, int containerSlot, int invSlot) {
      int containerId = mc.player.inventoryMenu.containerId;
      int stateId = mc.player.inventoryMenu.getStateId();
      HashGenerator hasher = mc.player.connection.decoratedHashOpsGenenerator();
      ItemStack totemStack = mc.player.getInventory().getItem(invSlot);
      ItemStack offhandStack = mc.player.getOffhandItem();
      Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectArrayMap();
      changedSlots.put(containerSlot, HashedStack.create(offhandStack.isEmpty() ? ItemStack.EMPTY : offhandStack, hasher));
      changedSlots.put(45, HashedStack.create(totemStack, hasher));
      mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, (short)containerSlot, (byte)40, ContainerInput.SWAP, changedSlots, HashedStack.EMPTY));
      mc.player.getInventory().setItem(invSlot, offhandStack.isEmpty() ? ItemStack.EMPTY : offhandStack.copy());
      mc.player.getInventory().setItem(40, totemStack.copy());
   }

   private void sendClose(Minecraft mc) {
      mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
   }

   private void abort(Minecraft mc) {
      if (this.stage != SilentTotem.Stage.IDLE) {
         this.sendClose(mc);
      }

      MoveFix.setMovementFrozen(false);
      this.stage = SilentTotem.Stage.IDLE;
   }

   private boolean hasTotemInOffhand(Minecraft mc) {
      return mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
   }

   private int findTotemInvSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();

      for (int i = 9; i < 36; i++) {
         if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      for (int i = 0; i < 9; i++) {
         if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
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
      WAIT_CLOSE;
   }
}
