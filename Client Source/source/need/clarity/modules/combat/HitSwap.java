package need.clarity.modules.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.Random;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.config.ClarityConfig;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class HitSwap extends ModuleBase {
   private static final double DEFAULT_DYNAMIC_THRESHOLD = -0.25;
   private HitSwap.Phase phase = HitSwap.Phase.IDLE;
   private long phaseStart = 0L;
   private int originalSlot = -1;
   private int maceSlot = -1;
   private LivingEntity slamTarget = null;
   private int slamTick = -1;
   private final Random rng = new Random();

   public HitSwap() {
      super("Hit Swap", "Combat");
      PerAttackEvents.register(this::onAttack);
   }

   @Override
   public void onEnable() {
      this.resetState(Minecraft.getInstance());
   }

   @Override
   public void onDisable() {
      this.resetState(Minecraft.getInstance());
   }

   private void resetState(Minecraft mc) {
      if (mc.player != null && this.originalSlot >= 0) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.originalSlot);
      }

      this.phase = HitSwap.Phase.IDLE;
      this.phaseStart = 0L;
      this.originalSlot = -1;
      this.maceSlot = -1;
      this.slamTarget = null;
   }

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled()) {
         if (this.phase == HitSwap.Phase.IDLE) {
            Entity target = event.getTarget();
            if (target != null) {
               if (this.isTargetTypeAllowed(target)) {
                  if (target instanceof LivingEntity living) {
                     if (!Friends.isProtected(living)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                           if (!this.getSetting("Only Weapon", false) || isWeapon(mc.player)) {
                              boolean doStunSlam = this.getSetting("Stun Slam", false) && isEntityShielding(living);
                              boolean checkShield = !doStunSlam && this.getSetting("Check Shield", false);
                              if (!checkShield || !isEntityShielding(living)) {
                                 this.originalSlot = InventoryHelper.getSelected(mc.player.getInventory());
                                 boolean wantDensity = this.wantsDensityMace(mc.player);
                                 this.maceSlot = this.findMaceSlot(mc.player.getInventory(), wantDensity);
                                 if (this.maceSlot >= 0) {
                                    if (doStunSlam) {
                                       int axeSlot = findAxeSlot(mc.player.getInventory());
                                       if (axeSlot < 0) {
                                          return;
                                       }

                                       this.slamTarget = living;
                                       this.slamTick = mc.player.tickCount;
                                       if (axeSlot != this.originalSlot) {
                                          InventoryHelper.setSelected(mc.player.getInventory(), axeSlot);
                                       }

                                       this.setPhase(HitSwap.Phase.SLAM_WAIT_TO_FIRE);
                                    } else {
                                       InventoryHelper.setSelected(mc.player.getInventory(), this.maceSlot);
                                       int swapLo = this.getSetting("Swap Delay_lo", 0);
                                       int swapHi = this.getSetting("Swap Delay_hi", 0);
                                       if (swapLo == 0 && swapHi == 0) {
                                          this.scheduleRestore();
                                       } else {
                                          this.setPhase(HitSwap.Phase.WAIT_SWAP);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            long now = System.currentTimeMillis();
            switch (this.phase) {
               case WAIT_SWAP:
                  int loxx = this.getSetting("Swap Delay_lo", 0);
                  int hixx = this.getSetting("Swap Delay_hi", 0);
                  if (now - this.phaseStart >= this.randomIn(loxx, hixx)) {
                     this.scheduleRestore();
                  }
                  break;
               case WAIT_RESTORE:
                  int lox = this.getSetting("Swap Back Delay_lo", 100);
                  int hix = this.getSetting("Swap Back Delay_hi", 150);
                  if (now - this.phaseStart >= this.randomIn(lox, hix)) {
                     this.restoreSlot(mc);
                  }
                  break;
               case SLAM_WAIT_TO_FIRE:
                  if (mc.player.tickCount > this.slamTick) {
                     if (this.maceSlot >= 0) {
                        InventoryHelper.setSelected(mc.player.getInventory(), this.maceSlot);
                     }

                     if (this.slamTarget != null && this.slamTarget.isAlive()) {
                        fireAttack(mc, this.slamTarget);
                     }

                     this.slamTarget = null;
                     boolean swapBack = this.getSetting("Swap Back", true);
                     if (swapBack && this.originalSlot >= 0) {
                        this.setPhase(HitSwap.Phase.SLAM_WAIT_RESTORE);
                     } else {
                        this.originalSlot = -1;
                        this.phase = HitSwap.Phase.IDLE;
                     }
                  }
                  break;
               case SLAM_WAIT_RESTORE:
                  int lo = this.getSetting("Swap Back Delay_lo", 100);
                  int hi = this.getSetting("Swap Back Delay_hi", 150);
                  if (now - this.phaseStart >= this.randomIn(lo, hi)) {
                     this.restoreSlot(mc);
                  }
            }
         }
      }
   }

   private void setPhase(HitSwap.Phase p) {
      this.phase = p;
      this.phaseStart = System.currentTimeMillis();
   }

   private void scheduleRestore() {
      boolean swapBack = this.getSetting("Swap Back", true);
      if (swapBack && this.originalSlot >= 0) {
         this.setPhase(HitSwap.Phase.WAIT_RESTORE);
      } else {
         this.originalSlot = -1;
         this.phase = HitSwap.Phase.IDLE;
      }
   }

   private void restoreSlot(Minecraft mc) {
      if (this.originalSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.originalSlot);
         this.originalSlot = -1;
      }

      this.phase = HitSwap.Phase.IDLE;
   }

   private static void fireAttack(Minecraft mc, LivingEntity target) {
      if (mc.player != null) {
         mc.gameMode.attack(mc.player, target);
         mc.player.swing(InteractionHand.MAIN_HAND);
         mc.player.resetAttackStrengthTicker();
      }
   }

   private static boolean isEntityShielding(LivingEntity living) {
      if (living.isBlocking()) {
         return true;
      }

      if (!living.isUsingItem()) {
         return false;
      }

      ItemStack useItem = living.getUseItem();
      return !useItem.isEmpty() && useItem.is(Items.SHIELD) ? living.getTicksUsingItem() >= 5 : false;
   }

   private boolean wantsDensityMace(LocalPlayer player) {
      String mode = this.getSetting("Mode", "Breach");

      return switch (mode) {
         case "Density" -> true;
         case "Dynamic" -> {
            double velY = player.getDeltaMovement().y;
            double threshold = this.getSetting("Dynamic Threshold", -0.25);
            yield velY < threshold;
         }
         default -> false;
      };
   }

   private int findMaceSlot(Inventory inv, boolean wantDensity) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return -1;
      }

      String mode = this.getSetting("Mode", "Breach");
      if ("Any".equals(mode)) {
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && stack.is(Items.MACE)) {
               return slot;
            }
         }

         return -1;
      } else {
         Registry<Enchantment> registry;
         try {
            registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
         } catch (Exception e) {
            return -1;
         }

         Holder<Enchantment> targetEnchant = resolveEnchant(registry, wantDensity);
         int fallback = -1;

         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && stack.is(Items.MACE)) {
               if (targetEnchant != null && EnchantmentHelper.getItemEnchantmentLevel(targetEnchant, stack) > 0) {
                  return slot;
               }

               if (fallback < 0) {
                  fallback = slot;
               }
            }
         }

         return fallback;
      }
   }

   private static int findAxeSlot(Inventory inv) {
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = inv.getItem(slot);
         if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
            return slot;
         }
      }

      return -1;
   }

   private static Holder<Enchantment> resolveEnchant(Registry<Enchantment> registry, boolean density) {
      try {
         Optional<? extends Holder<Enchantment>> opt = density ? registry.get(Enchantments.DENSITY) : registry.get(Enchantments.BREACH);
         return (Holder<Enchantment>)opt.orElse(null);
      } catch (Exception e) {
         return null;
      }
   }

   private static boolean isWeapon(LocalPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return stack.isEmpty() ? false : stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.MACE_ENCHANTABLE) || stack.getItem() instanceof TridentItem;
   }

   private boolean isTargetTypeAllowed(Entity entity) {
      boolean players = this.isTargeting("Players");
      boolean mobs = this.isTargeting("Mobs");
      boolean entities = this.isTargeting("Entities");
      if (entity instanceof Player) {
         return players;
      } else if (entity instanceof Mob) {
         return mobs;
      } else {
         return entity instanceof LivingEntity ? entities : entities;
      }
   }

   private boolean isTargeting(String category) {
      try {
         JsonObject modObj = ClarityConfig.getCurrent().getAsJsonObject("Hit Swap");
         if (modObj == null) {
            return true;
         }

         JsonObject values = modObj.getAsJsonObject("values");
         if (values == null) {
            return true;
         }

         JsonElement el = values.get("Targets");
         if (el != null && el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) {
               if (item.isJsonPrimitive() && item.getAsString().equals(category)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } catch (Exception e) {
         return true;
      }
   }

   private int randomIn(int lo, int hi) {
      return hi <= lo ? lo : lo + this.rng.nextInt(hi - lo + 1);
   }

   private enum Phase {
      IDLE,
      WAIT_SWAP,
      WAIT_RESTORE,
      SLAM_WAIT_TO_FIRE,
      SLAM_WAIT_RESTORE;
   }
}
