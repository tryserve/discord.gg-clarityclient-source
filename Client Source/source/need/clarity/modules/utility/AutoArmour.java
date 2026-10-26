package need.clarity.modules.utility;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import need.clarity.util.ContainerScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.HashedStack;
import net.minecraft.network.HashedPatchMap.HashGenerator;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;

public class AutoArmour extends ModuleBase {
   private static final int[] ARMOUR_CONTAINER_SLOTS = new int[]{5, 6, 7, 8};
   private static final EquipmentSlot[] EQUIPMENT_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private AutoArmour.Stage stage = AutoArmour.Stage.IDLE;
   private long stageReadyAt = 0L;
   private final List<AutoArmour.SwapJob> pendingJobs = new ArrayList<>();
   private long lastSwapAt = 0L;
   private static final Random rng = new Random();

   public AutoArmour() {
      super("Auto Armour", "utility");
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
            if (this.getSetting("Silent", false)) {
               this.tickSilent(mc);
            } else {
               this.tickNormal(mc);
            }
         }
      }
   }

   private void tickNormal(Minecraft mc) {
      if (mc.screen instanceof InventoryScreen) {
         long now = System.currentTimeMillis();
         int delayMs = this.pickDelay("Delay_lo", "Delay_hi");
         if (now - this.lastSwapAt >= delayMs) {
            for (int i = 0; i < ARMOUR_CONTAINER_SLOTS.length; i++) {
               int bestInvSlot = this.findBestArmourInInventory(mc, i);
               if (bestInvSlot != -1 && mc.gameMode != null) {
                  ContainerScreenHelper.shiftClick(mc.gameMode, mc.player.inventoryMenu.containerId, this.invSlotToContainerSlot(bestInvSlot), mc.player);
                  this.lastSwapAt = now;
                  return;
               }
            }
         }
      }
   }

   private void tickSilent(Minecraft mc) {
      if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) {
         if (this.stage != AutoArmour.Stage.IDLE) {
            this.abort(mc);
         }

         MoveFix.setMovementFrozen(false);
      } else {
         switch (this.stage) {
            case IDLE:
               this.tickSilentIdle(mc);
               break;
            case WAIT_OPEN:
               this.tickSilentWaitOpen(mc);
               break;
            case WAIT_CLOSE:
               this.tickSilentWaitClose(mc);
         }

         boolean freeze = this.getSetting("Bypass", true);
         MoveFix.setMovementFrozen(freeze && this.stage != AutoArmour.Stage.IDLE);
      }
   }

   private void tickSilentIdle(Minecraft mc) {
      this.pendingJobs.clear();

      for (int i = 0; i < ARMOUR_CONTAINER_SLOTS.length; i++) {
         int best = this.findBestArmourInInventory(mc, i);
         if (best != -1) {
            this.pendingJobs.add(new AutoArmour.SwapJob(i, best));
         }
      }

      if (!this.pendingJobs.isEmpty()) {
         this.stageReadyAt = now() + this.pickDelay("Open Delay_lo", "Open Delay_hi");
         this.stage = AutoArmour.Stage.WAIT_OPEN;
      }
   }

   private void tickSilentWaitOpen(Minecraft mc) {
      if (now() >= this.stageReadyAt) {
         for (AutoArmour.SwapJob job : this.pendingJobs) {
            ItemStack planned = mc.player.getInventory().getItem(job.invSlot);
            if (!planned.isEmpty() && this.isArmourForSlot(planned, EQUIPMENT_SLOTS[job.armourIdx])) {
               EquipmentSlot eqSlot = EQUIPMENT_SLOTS[job.armourIdx];
               ItemStack current = mc.player.getItemBySlot(eqSlot);
               if (!current.isEmpty()) {
                  this.sendUnequip(mc, job.armourIdx);
               }

               this.sendEquip(mc, job.invSlot, job.armourIdx);
            }
         }

         this.pendingJobs.clear();
         this.stageReadyAt = now() + this.pickDelay("Close Delay_lo", "Close Delay_hi");
         this.stage = AutoArmour.Stage.WAIT_CLOSE;
      }
   }

   private void tickSilentWaitClose(Minecraft mc) {
      if (now() >= this.stageReadyAt) {
         this.sendClose(mc);
         this.resetSilentState();
      }
   }

   private int findBestArmourInInventory(Minecraft mc, int armourSlotIdx) {
      EquipmentSlot eqSlot = EQUIPMENT_SLOTS[armourSlotIdx];
      ItemStack equipped = mc.player.getItemBySlot(eqSlot);
      int bestInvSlot = -1;
      int bestScore = this.armourScoreEquipped(equipped);
      Inventory inv = mc.player.getInventory();

      for (int pass = 0; pass < 2; pass++) {
         int start = pass == 0 ? 9 : 0;
         int end = pass == 0 ? 36 : 9;

         for (int i = start; i < end; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && this.isArmourForSlot(stack, eqSlot)) {
               int score = this.armourScore(stack, eqSlot);
               if (score > bestScore) {
                  bestScore = score;
                  bestInvSlot = i;
               }
            }
         }
      }

      return bestInvSlot;
   }

   private boolean isArmourForSlot(ItemStack stack, EquipmentSlot eqSlot) {
      if (stack.isEmpty()) {
         return false;
      }

      return switch (eqSlot) {
         case HEAD -> stack.is(ItemTags.HEAD_ARMOR);
         case CHEST -> stack.is(ItemTags.CHEST_ARMOR);
         case LEGS -> stack.is(ItemTags.LEG_ARMOR);
         case FEET -> stack.is(ItemTags.FOOT_ARMOR);
         default -> false;
      };
   }

   private int armourScoreEquipped(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      }

      double armor = 0.0;
      double toughness = 0.0;
      double kbRes = 0.0;

      try {
         ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
         if (mods == null) {
            return 0;
         }

         for (Entry entry : mods.modifiers()) {
            Holder<Attribute> attr = entry.attribute();
            double amt = entry.modifier().amount();
            if (attr.is(Attributes.ARMOR)) {
               armor += amt;
            } else if (attr.is(Attributes.ARMOR_TOUGHNESS)) {
               toughness += amt;
            } else if (attr.is(Attributes.KNOCKBACK_RESISTANCE)) {
               kbRes += amt;
            }
         }
      } catch (Throwable ignored) {
         return 0;
      }

      return (int)(armor * 1000.0 + toughness * 100.0 + kbRes * 10.0);
   }

   private int armourScore(ItemStack stack, EquipmentSlot eqSlot) {
      if (stack.isEmpty()) {
         return 0;
      }

      double armor = 0.0;
      double toughness = 0.0;
      double kbRes = 0.0;

      try {
         ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
         if (mods == null) {
            return 0;
         }

         for (Entry entry : mods.modifiers()) {
            if (entry.slot().test(eqSlot)) {
               Holder<Attribute> attr = entry.attribute();
               double amt = entry.modifier().amount();
               if (attr.is(Attributes.ARMOR)) {
                  armor += amt;
               } else if (attr.is(Attributes.ARMOR_TOUGHNESS)) {
                  toughness += amt;
               } else if (attr.is(Attributes.KNOCKBACK_RESISTANCE)) {
                  kbRes += amt;
               }
            }
         }
      } catch (Throwable ignored) {
         return 0;
      }

      return (int)(armor * 1000.0 + toughness * 100.0 + kbRes * 10.0);
   }

   private void sendUnequip(Minecraft mc, int armourIdx) {
      EquipmentSlot eqSlot = EQUIPMENT_SLOTS[armourIdx];
      int armourSlot = ARMOUR_CONTAINER_SLOTS[armourIdx];
      int containerId = mc.player.inventoryMenu.containerId;
      int stateId = mc.player.inventoryMenu.getStateId();
      HashGenerator hasher = mc.player.connection.decoratedHashOpsGenenerator();
      Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectArrayMap();
      changedSlots.put(armourSlot, HashedStack.create(ItemStack.EMPTY, hasher));
      mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, (short)armourSlot, (byte)0, ContainerInput.QUICK_MOVE, changedSlots, HashedStack.EMPTY));
      mc.player.setItemSlot(eqSlot, ItemStack.EMPTY);
   }

   private void sendEquip(Minecraft mc, int invSlot, int armourIdx) {
      EquipmentSlot eqSlot = EQUIPMENT_SLOTS[armourIdx];
      int armourSlot = ARMOUR_CONTAINER_SLOTS[armourIdx];
      int srcSlot = this.invSlotToContainerSlot(invSlot);
      ItemStack newPiece = mc.player.getInventory().getItem(invSlot);
      int containerId = mc.player.inventoryMenu.containerId;
      int stateId = mc.player.inventoryMenu.getStateId();
      HashGenerator hasher = mc.player.connection.decoratedHashOpsGenenerator();
      Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectArrayMap();
      changedSlots.put(srcSlot, HashedStack.create(ItemStack.EMPTY, hasher));
      changedSlots.put(armourSlot, HashedStack.create(newPiece, hasher));
      mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, (short)srcSlot, (byte)0, ContainerInput.QUICK_MOVE, changedSlots, HashedStack.EMPTY));
      mc.player.getInventory().setItem(invSlot, ItemStack.EMPTY);
      mc.player.setItemSlot(eqSlot, newPiece.copy());
   }

   private void sendClose(Minecraft mc) {
      mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
   }

   private void abort(Minecraft mc) {
      if (this.stage != AutoArmour.Stage.IDLE) {
         this.sendClose(mc);
      }

      MoveFix.setMovementFrozen(false);
      this.resetSilentState();
   }

   private void resetSilentState() {
      this.stage = AutoArmour.Stage.IDLE;
      this.stageReadyAt = 0L;
      this.pendingJobs.clear();
   }

   private int invSlotToContainerSlot(int invSlot) {
      return invSlot < 9 ? invSlot + 36 : invSlot;
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

   private static final class SwapJob {
      final int armourIdx;
      final int invSlot;

      SwapJob(int armourIdx, int invSlot) {
         this.armourIdx = armourIdx;
         this.invSlot = invSlot;
      }
   }
}
