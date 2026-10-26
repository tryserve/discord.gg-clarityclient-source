package need.clarity.modules.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.HashedPatchMap.HashGenerator;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryCleaner extends ModuleBase {
   private InventoryCleaner.Stage stage = InventoryCleaner.Stage.IDLE;
   private long stageReadyAt = 0L;
   private final List<Integer> pendingSlots = new ArrayList<>();
   private int throwIndex = 0;
   private long lastDropAt = 0L;
   private static final Random rng = new Random();

   public InventoryCleaner() {
      super("Inventory Cleaner", "utility");
   }

   @Override
   public void onDisable() {
      MoveFix.setMovementFrozen(false);
      this.resetSilentState();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null) {
            Set<Item> whitelist = this.resolveWhitelist();
            if (!whitelist.isEmpty()) {
               if (this.getSetting("Silent", false)) {
                  this.tickSilent(mc, whitelist);
               } else {
                  this.tickNormal(mc, whitelist);
               }
            }
         }
      }
   }

   private void tickNormal(Minecraft mc, Set<Item> whitelist) {
      if (mc.screen instanceof InventoryScreen) {
         long now = System.currentTimeMillis();
         int delay = this.pickDelay("Item Delay_lo", "Item Delay_hi");
         if (now - this.lastDropAt >= delay) {
            Inventory inv = mc.player.getInventory();

            for (int pass = 0; pass < 2; pass++) {
               int start = pass == 0 ? 9 : 0;
               int end = pass == 0 ? 36 : 9;

               for (int i = start; i < end; i++) {
                  ItemStack stack = inv.getItem(i);
                  if (!stack.isEmpty() && whitelist.contains(stack.getItem())) {
                     this.sendThrow(mc, i);
                     this.lastDropAt = now;
                     return;
                  }
               }
            }
         }
      }
   }

   private void tickSilent(Minecraft mc, Set<Item> whitelist) {
      if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) {
         if (this.stage != InventoryCleaner.Stage.IDLE) {
            this.abort(mc);
         }

         MoveFix.setMovementFrozen(false);
      } else {
         switch (this.stage) {
            case IDLE:
               this.tickSilentIdle(mc, whitelist);
               break;
            case WAIT_OPEN:
               this.tickSilentWaitOpen(mc, whitelist);
               break;
            case THROWING:
               this.tickSilentThrowing(mc, whitelist);
               break;
            case WAIT_CLOSE:
               this.tickSilentWaitClose(mc);
         }

         boolean freeze = this.getSetting("Bypass", true);
         MoveFix.setMovementFrozen(freeze && this.stage != InventoryCleaner.Stage.IDLE);
      }
   }

   private void tickSilentIdle(Minecraft mc, Set<Item> whitelist) {
      this.pendingSlots.clear();
      this.throwIndex = 0;
      Inventory inv = mc.player.getInventory();

      for (int pass = 0; pass < 2; pass++) {
         int start = pass == 0 ? 9 : 0;
         int end = pass == 0 ? 36 : 9;

         for (int i = start; i < end; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && whitelist.contains(stack.getItem())) {
               this.pendingSlots.add(i);
            }
         }
      }

      if (!this.pendingSlots.isEmpty()) {
         this.stageReadyAt = now() + this.pickDelay("Open Delay_lo", "Open Delay_hi");
         this.stage = InventoryCleaner.Stage.WAIT_OPEN;
      }
   }

   private void tickSilentWaitOpen(Minecraft mc, Set<Item> whitelist) {
      if (now() >= this.stageReadyAt) {
         this.advanceToValidSlot(mc, whitelist);
         if (this.throwIndex >= this.pendingSlots.size()) {
            this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
            this.stage = InventoryCleaner.Stage.WAIT_CLOSE;
         } else {
            this.sendThrow(mc, this.pendingSlots.get(this.throwIndex));
            this.throwIndex++;
            if (this.throwIndex < this.pendingSlots.size()) {
               this.stageReadyAt = now() + this.pickDelay("Silent Item Delay_lo", "Silent Item Delay_hi");
               this.stage = InventoryCleaner.Stage.THROWING;
            } else {
               this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
               this.stage = InventoryCleaner.Stage.WAIT_CLOSE;
            }
         }
      }
   }

   private void tickSilentThrowing(Minecraft mc, Set<Item> whitelist) {
      if (now() >= this.stageReadyAt) {
         this.advanceToValidSlot(mc, whitelist);
         if (this.throwIndex >= this.pendingSlots.size()) {
            this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
            this.stage = InventoryCleaner.Stage.WAIT_CLOSE;
         } else {
            this.sendThrow(mc, this.pendingSlots.get(this.throwIndex));
            this.throwIndex++;
            if (this.throwIndex < this.pendingSlots.size()) {
               this.stageReadyAt = now() + this.pickDelay("Silent Item Delay_lo", "Silent Item Delay_hi");
            } else {
               this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
               this.stage = InventoryCleaner.Stage.WAIT_CLOSE;
            }
         }
      }
   }

   private void advanceToValidSlot(Minecraft mc, Set<Item> whitelist) {
      while (this.throwIndex < this.pendingSlots.size()) {
         int invSlot = this.pendingSlots.get(this.throwIndex);
         ItemStack stack = mc.player.getInventory().getItem(invSlot);
         if (stack.isEmpty() || !whitelist.contains(stack.getItem())) {
            this.throwIndex++;
            continue;
         }
         break;
      }
   }

   private void tickSilentWaitClose(Minecraft mc) {
      if (now() >= this.stageReadyAt) {
         this.sendClose(mc);
         this.resetSilentState();
      }
   }

   private void sendThrow(Minecraft mc, int invSlot) {
      int containerSlot = invSlotToContainerSlot(invSlot);
      ItemStack stack = mc.player.getInventory().getItem(invSlot);
      if (!stack.isEmpty()) {
         int containerId = mc.player.inventoryMenu.containerId;
         int stateId = mc.player.inventoryMenu.getStateId();
         HashGenerator hasher = mc.player.connection.decoratedHashOpsGenenerator();
         Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectArrayMap();
         changedSlots.put(containerSlot, HashedStack.create(ItemStack.EMPTY, hasher));
         mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, (short)containerSlot, (byte)1, ContainerInput.THROW, changedSlots, HashedStack.EMPTY));
         mc.player.getInventory().setItem(invSlot, ItemStack.EMPTY);
      }
   }

   private void sendClose(Minecraft mc) {
      mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
   }

   private void abort(Minecraft mc) {
      if (this.stage != InventoryCleaner.Stage.IDLE) {
         this.sendClose(mc);
      }

      MoveFix.setMovementFrozen(false);
      this.resetSilentState();
   }

   private void resetSilentState() {
      this.stage = InventoryCleaner.Stage.IDLE;
      this.stageReadyAt = 0L;
      this.throwIndex = 0;
      this.pendingSlots.clear();
   }

   private Set<Item> resolveWhitelist() {
      try {
         JsonObject modObj = ClarityConfig.getCurrent().getAsJsonObject("Inventory Cleaner");
         if (modObj == null) {
            return Collections.emptySet();
         }

         JsonObject values = modObj.getAsJsonObject("values");
         if (values == null) {
            return Collections.emptySet();
         }

         JsonElement el = values.get("Items");
         if (el != null && el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() == 0) {
               return Collections.emptySet();
            }

            Set<Item> result = new HashSet<>();

            for (JsonElement entry : arr) {
               if (entry.isJsonPrimitive()) {
                  try {
                     String raw = entry.getAsString();
                     if (!raw.contains(":")) {
                        raw = "minecraft:" + raw;
                     }

                     Item item = (Item)BuiltInRegistries.ITEM.getValue(Identifier.parse(raw));
                     if (item != Items.AIR) {
                        result.add(item);
                     }
                  } catch (Exception var10) {
                  }
               }
            }

            return result;
         } else {
            return Collections.emptySet();
         }
      } catch (Exception e) {
         return Collections.emptySet();
      }
   }

   private static int invSlotToContainerSlot(int invSlot) {
      return invSlot < 9 ? invSlot + 36 : invSlot;
   }

   private int pickDelay(String loKey, String hiKey) {
      int lo = this.getSetting(loKey, 0);
      int hi = this.getSetting(hiKey, 0);
      return lo >= hi ? Math.max(0, lo) : lo + rng.nextInt(hi - lo + 1);
   }

   private static long now() {
      return System.currentTimeMillis();
   }

   private enum Stage {
      IDLE,
      WAIT_OPEN,
      THROWING,
      WAIT_CLOSE;
   }
}
