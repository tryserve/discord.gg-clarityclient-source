package need.clarity.modules.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import need.clarity.PerFrameEvents;
import need.clarity.keybind.KeybindHandler;
import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;

public class AutoLunge extends ModuleBase {
   private static final ResourceKey<Enchantment> LUNGE_KEY = ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("lunge"));
   private AutoLunge.Phase phase = AutoLunge.Phase.IDLE;
   private long phaseStart = 0L;
   private long phaseDelay = 0L;
   private int originalSlot = -1;
   private boolean bindWasDown = false;
   private boolean looping = false;
   private int grimRestoreSlot = -1;
   private int grimSpearSlot = -1;
   private boolean grimPending = false;
   private boolean grimHeld = false;
   private long lastGrimFire = 0L;
   private long grimInterval = 50L;
   private final Random random = new Random();
   private int grimPresses = 0;
   private int normalPresses = 0;
   private float grimTicker = 100.0F;
   private float lastScale = 1.0F;
   private final Map<Item, Float> spearDelay = new HashMap<>();
   private int heldSlotLast = -1;
   private int heldSlotTicks = 0;
   private long grimQueuedAt = 0L;
   private static final long GRIM_QUEUE_TIMEOUT_MS = 2500L;
   private static final float UNKNOWN_SPEAR_DELAY = 20.0F;

   public AutoLunge() {
      super("Auto Lunge", "combat");
      PerFrameEvents.register(this::onFrame);
   }

   @Override
   public void onEnable() {
      this.phase = AutoLunge.Phase.IDLE;
      this.phaseStart = 0L;
      this.originalSlot = -1;
      this.bindWasDown = false;
      this.looping = false;
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      this.restoreGrimSlot(mc);
      if (mc.player != null && this.originalSlot != -1) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.originalSlot);
      }

      this.phase = AutoLunge.Phase.IDLE;
      this.originalSlot = -1;
      this.bindWasDown = false;
      this.looping = false;
   }

   private void onFrame() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null) {
            if (mc.screen == null) {
               LocalPlayer player = mc.player;
               long now = System.currentTimeMillis();
               if (this.phase == AutoLunge.Phase.IDLE && "Grim".equals(this.getSetting("Mode", "Normal"))) {
                  this.pollGrimBind(mc);
               } else {
                  if (this.phase != AutoLunge.Phase.IDLE && !this.getSetting("Bind Hold Loop", false)) {
                     boolean down = isBindDown(mc, this.getSetting("Lunge Bind", "NONE"));
                     if (down && !this.bindWasDown) {
                        this.normalPresses = Math.min(this.normalPresses + 1, 8);
                     }

                     this.bindWasDown = down;
                  }

                  if (this.phase == AutoLunge.Phase.SWAP_BACK_WAITING) {
                     boolean clickConsumed = AccessorCompat.getClickCount(mc.options.keyAttack) == 0;
                     boolean elapsed = now - this.phaseStart >= this.phaseDelay;
                     boolean timedOut = now - this.phaseStart >= this.phaseDelay + 500L;
                     if (clickConsumed && elapsed || timedOut) {
                        if (this.originalSlot != -1) {
                           InventoryHelper.setSelected(player.getInventory(), this.originalSlot);
                           this.originalSlot = -1;
                        }

                        this.phase = AutoLunge.Phase.IDLE;
                        this.looping = true;
                        this.phaseStart = now;
                     }
                  } else if (this.phase == AutoLunge.Phase.HOLD_LOOP_WAITING) {
                     String hlBind = this.getSetting("Lunge Bind", "NONE");
                     if (!isBindDown(mc, hlBind)) {
                        this.phase = AutoLunge.Phase.IDLE;
                        this.looping = false;
                     } else {
                        if (now - this.phaseStart >= this.phaseDelay) {
                           if (player.getAttackStrengthScale(0.0F) >= this.cooldownThreshold()) {
                              this.executeLunge(mc, player);
                           } else {
                              this.phase = AutoLunge.Phase.IDLE;
                              this.looping = false;
                           }
                        }
                     }
                  } else if (this.phase == AutoLunge.Phase.SWAP_WAITING) {
                     if (now - this.phaseStart >= this.phaseDelay) {
                        this.executeLunge(mc, player);
                     }
                  } else {
                     String bind = this.getSetting("Lunge Bind", "NONE");
                     boolean bindDown = isBindDown(mc, bind);
                     boolean holdLoop = this.getSetting("Bind Hold Loop", false);
                     if (holdLoop) {
                        if (!bindDown) {
                           this.bindWasDown = false;
                           this.looping = false;
                           return;
                        }

                        this.looping = true;
                        this.triggerLunge(mc, player, now);
                     } else {
                        boolean risingEdge = bindDown && !this.bindWasDown;
                        this.bindWasDown = bindDown;
                        if (!risingEdge) {
                           if (this.normalPresses <= 0) {
                              return;
                           }

                           this.normalPresses--;
                        }

                        this.triggerLunge(mc, player, now);
                     }
                  }
               }
            }
         }
      }
   }

   private void pollGrimBind(Minecraft mc) {
      boolean bindDown = isBindDown(mc, this.getSetting("Lunge Bind", "NONE"));
      boolean holdLoop = this.getSetting("Bind Hold Loop", false);
      if (holdLoop) {
         this.grimHeld = bindDown;
         if (!bindDown) {
            this.bindWasDown = false;
         }
      } else {
         if (bindDown && !this.bindWasDown) {
            this.grimPresses = Math.min(this.grimPresses + 1, 8);
         }

         this.bindWasDown = bindDown;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      boolean grim = this.isEnabled() && "Grim".equals(this.getSetting("Mode", "Normal"));
      LocalPlayer player = mc.player;
      if (player != null) {
         this.trackCharge(player);
      }

      boolean fired = false;
      if (grim && player != null && mc.level != null && mc.screen == null) {
         long now = System.currentTimeMillis();
         boolean holdLoop = this.getSetting("Bind Hold Loop", false);
         if (holdLoop) {
            if (this.grimHeld && this.grimQueuedAt == 0L && now - this.lastGrimFire >= this.grimInterval) {
               this.grimQueuedAt = now;
            }

            if (!this.grimHeld) {
               this.grimQueuedAt = 0L;
            }
         } else if (this.grimPresses > 0 && this.grimQueuedAt == 0L) {
            this.grimPresses--;
            this.grimQueuedAt = now;
         }

         if (this.grimQueuedAt != 0L) {
            if (now - this.grimQueuedAt > 2500L) {
               this.grimQueuedAt = 0L;
            } else if (this.grimReady(player)) {
               this.grimQueuedAt = 0L;
               this.lastGrimFire = now;
               this.grimInterval = Math.max(50, this.randomDelay(this.getSetting("Hold Loop Delay_lo", 0), this.getSetting("Hold Loop Delay_hi", 100)));
               fired = this.sendGrimLunge(mc, player);
            }
         }
      } else {
         this.grimPending = false;
         this.grimPresses = 0;
         this.grimQueuedAt = 0L;
         if (!grim) {
            this.grimHeld = false;
         }
      }

      if (this.grimRestoreSlot != -1 && !fired) {
         this.restoreGrimSlot(mc);
      }
   }

   private void trackCharge(LocalPlayer player) {
      float scale = player.getAttackStrengthScale(0.0F);
      float delayNow = Math.max(1.0F, player.getCurrentItemAttackStrengthDelay());
      if (scale + 1.0E-4F < this.lastScale) {
         this.grimTicker = scale * delayNow;
      } else if (scale < 0.999F) {
         this.grimTicker = scale * delayNow;
      } else {
         this.grimTicker++;
      }

      this.lastScale = scale;
      int sel = InventoryHelper.getSelected(player.getInventory());
      this.heldSlotTicks = sel == this.heldSlotLast ? this.heldSlotTicks + 1 : 0;
      this.heldSlotLast = sel;
      ItemStack held = player.getInventory().getItem(sel);
      if (this.heldSlotTicks >= 1 && !held.isEmpty() && this.getLungeLevel(held, player) > 0) {
         this.spearDelay.put(held.getItem(), delayNow);
      }
   }

   private boolean grimReady(LocalPlayer player) {
      if (!player.isInWater() && !player.isFallFlying() && !player.isPassenger()) {
         int slot = this.findBestSpearSlot(player);
         if (slot < 0) {
            return false;
         }

         Float d = this.spearDelay.get(player.getInventory().getItem(slot).getItem());
         float delay = d != null ? d : 20.0F;
         float charge = Math.clamp(this.getSetting("Charge %", 95).intValue(), 1, 100) / 100.0F;
         return this.grimTicker >= delay * charge + 1.0F;
      } else {
         return false;
      }
   }

   private boolean sendGrimLunge(Minecraft mc, LocalPlayer player) {
      int spearSlot = this.findBestSpearSlot(player);
      if (spearSlot < 0) {
         return false;
      }

      Inventory inv = player.getInventory();
      int selected = InventoryHelper.getSelected(inv);
      int home = this.grimRestoreSlot != -1 ? this.grimRestoreSlot : selected;
      boolean swapBack = this.getSetting("Swap Back", true);
      if (spearSlot != selected) {
         InventoryHelper.setSelected(inv, spearSlot);
         this.syncCarriedItem(mc, spearSlot);
      }

      int stabs = Math.clamp(this.getSetting("Stabs Per Swap", 1).intValue(), 1, 10);

      for (int i = 0; i < stabs; i++) {
         player.connection.send(new ServerboundPlayerActionPacket(Action.STAB, BlockPos.ZERO, Direction.DOWN));
         player.swing(InteractionHand.MAIN_HAND);
      }

      player.resetAttackStrengthTicker();
      this.grimTicker = 0.0F;
      this.lastScale = 0.0F;
      if (spearSlot != home && swapBack) {
         this.grimRestoreSlot = home;
         this.grimSpearSlot = spearSlot;
      } else {
         this.grimRestoreSlot = -1;
      }

      return true;
   }

   private void syncCarriedItem(Minecraft mc, int slot) {
      if (mc.gameMode != null) {
         try {
            AccessorCompat.ensureHasSentCarriedItem(mc.gameMode);
            return;
         } catch (Throwable var4) {
         }
      }

      mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
   }

   private void restoreGrimSlot(Minecraft mc) {
      int slot = this.grimRestoreSlot;
      this.grimRestoreSlot = -1;
      if (slot >= 0 && mc.player != null) {
         Inventory inv = mc.player.getInventory();
         int selected = InventoryHelper.getSelected(inv);
         if (selected == this.grimSpearSlot && selected != slot) {
            InventoryHelper.setSelected(inv, slot);
            this.syncCarriedItem(mc, slot);
         }
      }
   }

   private float cooldownThreshold() {
      int pct = this.getSetting("Cooldown %", 85);
      return Math.clamp(pct, 0, 100) / 100.0F;
   }

   private void triggerLunge(Minecraft mc, LocalPlayer player, long now) {
      int spearSlot = this.findBestSpearSlot(player);
      if (spearSlot >= 0) {
         boolean holdLoop = this.getSetting("Bind Hold Loop", false);
         if (holdLoop && this.looping) {
            if (player.getAttackStrengthScale(0.0F) >= this.cooldownThreshold() && this.phase == AutoLunge.Phase.IDLE) {
               this.originalSlot = InventoryHelper.getSelected(player.getInventory());
               this.phase = AutoLunge.Phase.HOLD_LOOP_WAITING;
               this.phaseStart = now;
               this.phaseDelay = this.randomDelay(this.getSetting("Hold Loop Delay_lo", 0), this.getSetting("Hold Loop Delay_hi", 100));
            }
         } else if (!(player.getAttackStrengthScale(0.0F) < this.cooldownThreshold())) {
            this.originalSlot = InventoryHelper.getSelected(player.getInventory());
            long sd = this.randomDelay(this.getSetting("Swap Delay_lo", 0), this.getSetting("Swap Delay_hi", 0));
            if (sd == 0L) {
               this.executeLunge(mc, player);
            } else {
               this.phase = AutoLunge.Phase.SWAP_WAITING;
               this.phaseStart = now;
               this.phaseDelay = sd;
            }
         }
      }
   }

   private void executeLunge(Minecraft mc, LocalPlayer player) {
      Inventory inv = player.getInventory();
      boolean swapBack = this.getSetting("Swap Back", true);
      int spearSlot = this.findBestSpearSlot(player);
      if (spearSlot < 0) {
         if (this.originalSlot != -1) {
            InventoryHelper.setSelected(inv, this.originalSlot);
            this.originalSlot = -1;
         }

         this.phase = AutoLunge.Phase.IDLE;
         this.looping = false;
      } else {
         InventoryHelper.setSelected(inv, spearSlot);
         AccessorCompat.addClick(mc.options.keyAttack);
         if (swapBack && this.originalSlot != -1 && this.originalSlot != spearSlot) {
            this.phase = AutoLunge.Phase.SWAP_BACK_WAITING;
            this.phaseStart = System.currentTimeMillis();
            this.phaseDelay = this.randomDelay(this.getSetting("Swap Back Delay_lo", 100), this.getSetting("Swap Back Delay_hi", 100));
         } else {
            this.originalSlot = -1;
            this.phase = AutoLunge.Phase.IDLE;
            this.looping = true;
            this.phaseStart = System.currentTimeMillis();
         }
      }
   }

   private int findBestSpearSlot(LocalPlayer player) {
      Inventory inv = player.getInventory();
      int bestSlot = -1;
      int bestLevel = 0;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = inv.getItem(slot);
         if (!stack.isEmpty()) {
            int level = this.getLungeLevel(stack, player);
            if (level > bestLevel) {
               bestLevel = level;
               bestSlot = slot;
            }
         }
      }

      return bestSlot;
   }

   private int getLungeLevel(ItemStack stack, LocalPlayer player) {
      try {
         RegistryAccess registryAccess = player.level().registryAccess();
         Registry<Enchantment> enchantRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
         Optional<Reference<Enchantment>> holderOpt = enchantRegistry.get(LUNGE_KEY);
         if (holderOpt.isEmpty()) {
            return 0;
         }

         Holder<Enchantment> holder = (Holder<Enchantment>)holderOpt.get();
         ItemEnchantments enchantments = (ItemEnchantments)stack.get(DataComponents.ENCHANTMENTS);
         return enchantments == null ? 0 : enchantments.getLevel(holder);
      } catch (Exception e) {
         return 0;
      }
   }

   private static boolean isBindDown(Minecraft mc, String name) {
      if (name != null && !name.isEmpty() && !"NONE".equalsIgnoreCase(name) && !"None".equalsIgnoreCase(name)) {
         long window = mc.getWindow().handle();
         int mouse = KeybindHandler.mouseButtonIndex(name);
         if (mouse >= 0) {
            return GLFW.glfwGetMouseButton(window, mouse) == 1;
         }

         int glfwKey = KeybindHandler.glfwKeyCode(name);
         return glfwKey == -1 ? false : GLFW.glfwGetKey(window, glfwKey) == 1;
      } else {
         return false;
      }
   }

   private int randomDelay(int low, int high) {
      return high <= low ? low : low + this.random.nextInt(high - low + 1);
   }

   private enum Phase {
      IDLE,
      HOLD_LOOP_WAITING,
      SWAP_WAITING,
      SWAP_BACK_WAITING;
   }
}
