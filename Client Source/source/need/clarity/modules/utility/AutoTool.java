package need.clarity.modules.utility;

import java.util.Optional;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class AutoTool extends ModuleBase {
   private final Minecraft mc = Minecraft.getInstance();
   private boolean wasMining = false;
   private int prevSlot = -1;
   private long swapTimer = -1L;
   private long swapBackTimer = -1L;
   private boolean didSwap = false;

   public AutoTool() {
      super("Auto Tool", "Utility");
   }

   @Override
   public void onDisable() {
      this.wasMining = false;
      this.prevSlot = -1;
      this.swapTimer = -1L;
      this.swapBackTimer = -1L;
      this.didSwap = false;
   }

   @Override
   public void onTick() {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         boolean isMining = this.mc.screen == null && this.mc.options.keyAttack.isDown() && this.mc.hitResult != null && this.mc.hitResult.getType() == Type.BLOCK;
         long now = System.currentTimeMillis();
         if (isMining && !this.wasMining) {
            this.prevSlot = InventoryHelper.getSelected(this.mc.player.getInventory());
            this.swapBackTimer = -1L;
            this.didSwap = false;
            int swapDelay = this.getSetting("Swap Delay", 0);
            this.swapTimer = now + swapDelay;
         }

         if (isMining) {
            if (this.swapTimer != -1L && now >= this.swapTimer) {
               this.swapTimer = -1L;
               this.doSwap();
            }
         } else if (this.wasMining) {
            this.swapTimer = -1L;
            if (this.didSwap && this.getSetting("Swap Back", false) && this.prevSlot != -1) {
               int swapBackDelay = this.getSetting("Swap Back Delay", 0);
               this.swapBackTimer = now + swapBackDelay;
            } else {
               this.prevSlot = -1;
            }

            this.didSwap = false;
         }

         if (!isMining && this.swapBackTimer != -1L && now >= this.swapBackTimer) {
            if (this.prevSlot != -1) {
               InventoryHelper.setSelected(this.mc.player.getInventory(), this.prevSlot);
            }

            this.prevSlot = -1;
            this.swapBackTimer = -1L;
         }

         this.wasMining = isMining;
      }
   }

   private void doSwap() {
      if (this.mc.player != null && this.mc.level != null) {
         if (this.mc.hitResult != null && this.mc.hitResult.getType() == Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)this.mc.hitResult).getBlockPos();
            BlockState state = this.mc.level.getBlockState(pos);
            boolean onlyPickaxe = this.getSetting("Only Pickaxe", false);
            Inventory inv = this.mc.player.getInventory();
            int currentSlot = InventoryHelper.getSelected(inv);
            float bestSpeed = this.getMiningSpeed(inv.getItem(currentSlot), state);
            int bestSlot = -1;

            for (int slot = 0; slot < 9; slot++) {
               if (slot != currentSlot) {
                  ItemStack stack = inv.getItem(slot);
                  if (!stack.isEmpty() && (!onlyPickaxe || stack.is(ItemTags.PICKAXES))) {
                     float speed = this.getMiningSpeed(stack, state);
                     if (speed > bestSpeed) {
                        bestSpeed = speed;
                        bestSlot = slot;
                     }
                  }
               }
            }

            if (bestSlot != -1) {
               InventoryHelper.setSelected(inv, bestSlot);
               this.didSwap = true;
            }
         }
      }
   }

   private float getMiningSpeed(ItemStack stack, BlockState state) {
      if (stack.isEmpty()) {
         return 1.0F;
      }

      float speed = stack.getDestroySpeed(state);
      if (speed > 1.0F && this.mc.level != null) {
         try {
            Registry<Enchantment> registry = this.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Reference<Enchantment>> efficiency = registry.get(Enchantments.EFFICIENCY);
            int effLvl = efficiency.<Integer>map(entry -> EnchantmentHelper.getItemEnchantmentLevel(entry, stack)).orElse(0);
            if (effLvl > 0) {
               speed += effLvl * effLvl + 1;
            }
         } catch (Exception var7) {
         }
      }

      return speed;
   }
}
