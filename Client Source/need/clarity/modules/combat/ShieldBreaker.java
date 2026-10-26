package need.clarity.modules.combat;

import java.util.Random;
import java.util.WeakHashMap;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ShieldBreaker extends ModuleBase {
   private static final WeakHashMap<LivingEntity, Long> shieldBrokenAt = new WeakHashMap<>();
   private static final long SHIELD_BROKEN_SUPPRESS_MS = 600L;
   private ShieldBreaker.Phase phase = ShieldBreaker.Phase.IDLE;
   private long phaseStart = 0L;
   private int originalSlot = -1;
   private final Random random = new Random();

   public ShieldBreaker() {
      super("Shield Breaker", "Combat");
   }

   @Override
   public void onEnable() {
      this.resetState(Minecraft.getInstance());
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (this.originalSlot != -1 && mc.player != null) {
         setSelected(mc.player.getInventory(), this.originalSlot);
      }

      this.resetState(mc);
   }

   private void resetState(Minecraft mc) {
      if (this.originalSlot != -1 && mc.player != null) {
         setSelected(mc.player.getInventory(), this.originalSlot);
      }

      this.phase = ShieldBreaker.Phase.IDLE;
      this.phaseStart = 0L;
      this.originalSlot = -1;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            boolean notInGui = this.getSetting("Not in GUI", true);
            if (!notInGui || mc.screen == null) {
               boolean notWhileUsing = this.getSetting("Not While Using", true);
               if (notWhileUsing && player.isUsingItem()) {
                  if (this.phase != ShieldBreaker.Phase.IDLE) {
                     if (this.originalSlot != -1) {
                        setSelected(player.getInventory(), this.originalSlot);
                        this.originalSlot = -1;
                     }

                     this.phase = ShieldBreaker.Phase.IDLE;
                  }
               } else {
                  String mode = this.getSetting("Mode", "Normal");
                  boolean swapBack = this.getSetting("Swap Back", false);
                  int delayLo = this.getSetting("Delay_lo", 50);
                  int delayHi = this.getSetting("Delay_hi", 75);
                  int sbLo = this.getSetting("Swap Back Delay_lo", 50);
                  int sbHi = this.getSetting("Swap Back Delay_hi", 75);
                  long now = System.currentTimeMillis();
                  if (this.phase == ShieldBreaker.Phase.WAITING_TO_RESTORE) {
                     if (now - this.phaseStart >= this.randomDelay(sbLo, sbHi)) {
                        if (this.originalSlot != -1) {
                           setSelected(player.getInventory(), this.originalSlot);
                           this.originalSlot = -1;
                        }

                        this.phase = ShieldBreaker.Phase.IDLE;
                     }
                  } else if (this.phase != ShieldBreaker.Phase.WAITING_TO_ATTACK) {
                     LivingEntity target = getCrosshairTarget(mc);
                     if (target != null && isBlocking(target)) {
                        int axeSlot = findAxeSlot(player.getInventory());
                        if (axeSlot >= 0) {
                           this.originalSlot = getSelected(player.getInventory());
                           if (axeSlot == this.originalSlot) {
                              this.originalSlot = -1;
                              this.executeAttack(mc, player, target, mode, swapBack);
                           } else {
                              setSelected(player.getInventory(), axeSlot);
                              this.phase = ShieldBreaker.Phase.WAITING_TO_ATTACK;
                              this.phaseStart = now;
                           }
                        }
                     }
                  } else {
                     if (now - this.phaseStart >= this.randomDelay(delayLo, delayHi)) {
                        LivingEntity target = getCrosshairTarget(mc);
                        if (target == null || !isBlocking(target)) {
                           if (this.originalSlot != -1) {
                              setSelected(player.getInventory(), this.originalSlot);
                              this.originalSlot = -1;
                           }

                           this.phase = ShieldBreaker.Phase.IDLE;
                           return;
                        }

                        this.executeAttack(mc, player, target, mode, swapBack);
                     }
                  }
               }
            }
         }
      }
   }

   private void executeAttack(Minecraft mc, LocalPlayer player, LivingEntity target, String mode, boolean swapBack) {
      int axeSlot = findAxeSlot(player.getInventory());
      if (axeSlot < 0) {
         if (this.originalSlot != -1) {
            setSelected(player.getInventory(), this.originalSlot);
            this.originalSlot = -1;
         }

         this.phase = ShieldBreaker.Phase.IDLE;
      } else {
         mc.gameMode.attack(player, target);
         player.swing(InteractionHand.MAIN_HAND);
         player.resetAttackStrengthTicker();
         if ("Stun".equals(mode)) {
            mc.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            player.resetAttackStrengthTicker();
         }

         notifyShieldBroken(target);
         if (swapBack && this.originalSlot != -1) {
            this.phase = ShieldBreaker.Phase.WAITING_TO_RESTORE;
            this.phaseStart = System.currentTimeMillis();
         } else {
            this.originalSlot = -1;
            this.phase = ShieldBreaker.Phase.IDLE;
         }
      }
   }

   public static boolean isBlocking(LivingEntity entity) {
      if (entity == null) {
         return false;
      }

      Long brokenAt = shieldBrokenAt.get(entity);
      if (brokenAt != null) {
         long timeSince = System.currentTimeMillis() - brokenAt;
         if (timeSince < 600L) {
            if (entity.isUsingItem()) {
               return false;
            }

            shieldBrokenAt.remove(entity);
         } else {
            shieldBrokenAt.remove(entity);
         }
      }

      return entity.isBlocking();
   }

   public static void notifyShieldBroken(LivingEntity entity) {
      shieldBrokenAt.put(entity, System.currentTimeMillis());
   }

   private static LivingEntity getCrosshairTarget(Minecraft mc) {
      return mc.crosshairPickEntity instanceof LivingEntity living && !Friends.isProtected(living) ? living : null;
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

   private static int getSelected(Inventory inv) {
      return InventoryHelper.getSelected(inv);
   }

   private static void setSelected(Inventory inv, int slot) {
      InventoryHelper.setSelected(inv, slot);
   }

   private int randomDelay(int lo, int hi) {
      return hi <= lo ? lo : lo + this.random.nextInt(hi - lo + 1);
   }

   private enum Phase {
      IDLE,
      WAITING_TO_ATTACK,
      WAITING_TO_RESTORE;
   }
}
