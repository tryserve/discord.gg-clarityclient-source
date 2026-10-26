package need.clarity.modules.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import need.clarity.util.ContainerScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChestStealer extends ModuleBase {
   private static ChestStealer INSTANCE;
   private ChestStealer.Stage stage = ChestStealer.Stage.IDLE;
   private long stageReadyAt = 0L;
   private boolean pendingCapture = false;
   private AbstractContainerMenu activeMenu = null;
   private final List<Integer> pendingSlots = new ArrayList<>();
   private int collectIndex = 0;
   private long lastCollectAt = 0L;
   private boolean slotsReady = false;
   private static final Random rng = new Random();

   public ChestStealer() {
      super("Chest Stealer", "utility");
      INSTANCE = this;
   }

   public static boolean tryCaptureSilent() {
      ChestStealer cs = INSTANCE;
      if (cs == null || !cs.isEnabled()) {
         return false;
      }

      if (!"Silent".equals(cs.getSetting("Collection Mode", "Normal"))) {
         return false;
      }

      if (cs.stage != ChestStealer.Stage.IDLE) {
         return false;
      }

      cs.pendingCapture = true;
      return true;
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null) {
            if ("Silent".equals(this.getSetting("Collection Mode", "Normal"))) {
               this.tickSilent(mc);
            } else {
               this.tickNormal(mc);
            }
         }
      }
   }

   private void tickNormal(Minecraft mc) {
      if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
         this.resetState();
      } else {
         AbstractContainerMenu menu = screen.getMenu();
         if (!(menu instanceof ChestMenu)) {
            this.resetState();
         } else {
            if (!this.slotsReady) {
               this.buildSlotList(menu, this.resolveWhitelist());
               this.slotsReady = true;
            }

            this.advanceToNonEmpty(menu);
            if (this.collectIndex >= this.pendingSlots.size()) {
               this.resetState();
            } else {
               long now = System.currentTimeMillis();
               int delay = this.pickDelay();
               if (now - this.lastCollectAt >= delay) {
                  do {
                     this.advanceToNonEmpty(menu);
                     if (this.collectIndex >= this.pendingSlots.size()) {
                        this.resetState();
                        return;
                     }

                     ContainerScreenHelper.shiftClick(mc.gameMode, menu.containerId, this.pendingSlots.get(this.collectIndex), mc.player);
                     this.collectIndex++;
                  } while (delay == 0 && this.collectIndex < this.pendingSlots.size());

                  this.lastCollectAt = now;
               }
            }
         }
      }
   }

   private void tickSilent(Minecraft mc) {
      switch (this.stage) {
         case IDLE:
            this.tickSilentIdle(mc);
            break;
         case COLLECTING:
            this.tickSilentCollecting(mc);
            break;
         case WAIT_CLOSE:
            this.tickSilentWaitClose(mc);
      }

      boolean freeze = this.getSetting("Bypass", true);
      MoveFix.setMovementFrozen(freeze && this.stage != ChestStealer.Stage.IDLE);
   }

   private void tickSilentIdle(Minecraft mc) {
      if (this.pendingCapture) {
         AbstractContainerMenu menu = mc.player.containerMenu;
         if (!(menu instanceof ChestMenu)) {
            this.pendingCapture = false;
         } else {
            this.activeMenu = menu;
            this.buildSlotList(menu, this.resolveWhitelist());
            this.pendingCapture = false;
            if (this.pendingSlots.isEmpty()) {
               this.sendClose(mc);
               this.resetState();
            } else {
               this.stageReadyAt = now() + this.pickDelay();
               this.stage = ChestStealer.Stage.COLLECTING;
            }
         }
      }
   }

   private void tickSilentCollecting(Minecraft mc) {
      if (mc.player.containerMenu == mc.player.inventoryMenu) {
         this.resetState();
      } else if (now() >= this.stageReadyAt) {
         int delay = this.pickDelay();
         int liveId = mc.player.containerMenu.containerId;

         do {
            this.advanceToNonEmpty(this.activeMenu);
            if (this.collectIndex >= this.pendingSlots.size()) {
               this.stageReadyAt = now() + delay;
               this.stage = ChestStealer.Stage.WAIT_CLOSE;
               return;
            }

            ContainerScreenHelper.shiftClick(mc.gameMode, liveId, this.pendingSlots.get(this.collectIndex), mc.player);
            this.collectIndex++;
         } while (delay == 0);

         this.stageReadyAt = now() + delay;
      }
   }

   private void tickSilentWaitClose(Minecraft mc) {
      if (mc.player.containerMenu == mc.player.inventoryMenu) {
         this.resetState();
      } else if (now() >= this.stageReadyAt) {
         this.sendClose(mc);
         this.resetState();
      }
   }

   private void buildSlotList(AbstractContainerMenu menu, Set<Item> whitelist) {
      this.pendingSlots.clear();
      this.collectIndex = 0;
      boolean useWhitelist = "Whitelist".equals(this.getSetting("Item Mode", "All")) && !whitelist.isEmpty();
      int containerSize = menu.slots.size() - 36;

      for (int i = 0; i < containerSize; i++) {
         ItemStack stack = menu.getSlot(i).getItem();
         if (!stack.isEmpty() && (!useWhitelist || whitelist.contains(stack.getItem()))) {
            this.pendingSlots.add(i);
         }
      }

      if ("Random".equals(this.getSetting("Slot Order", "Normal"))) {
         Collections.shuffle(this.pendingSlots, rng);
      }
   }

   private void advanceToNonEmpty(AbstractContainerMenu menu) {
      if (menu != null) {
         while (this.collectIndex < this.pendingSlots.size()) {
            int slot = this.pendingSlots.get(this.collectIndex);
            if (slot < menu.slots.size() && !menu.getSlot(slot).getItem().isEmpty()) {
               break;
            }

            this.collectIndex++;
         }
      }
   }

   private void sendClose(Minecraft mc) {
      if (mc.player != null && this.activeMenu != null) {
         mc.player.connection.send(new ServerboundContainerClosePacket(this.activeMenu.containerId));
         mc.player.containerMenu = mc.player.inventoryMenu;
      }
   }

   private void resetState() {
      MoveFix.setMovementFrozen(false);
      this.stage = ChestStealer.Stage.IDLE;
      this.stageReadyAt = 0L;
      this.pendingCapture = false;
      this.activeMenu = null;
      this.pendingSlots.clear();
      this.collectIndex = 0;
      this.slotsReady = false;
      this.lastCollectAt = 0L;
   }

   private Set<Item> resolveWhitelist() {
      if (!"Whitelist".equals(this.getSetting("Item Mode", "All"))) {
         return Collections.emptySet();
      }

      try {
         JsonObject modObj = ClarityConfig.getCurrent().getAsJsonObject("Chest Stealer");
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

   private int pickDelay() {
      int lo = this.getSetting("Item Delay_lo", 50);
      int hi = this.getSetting("Item Delay_hi", 100);
      return lo >= hi ? Math.max(0, lo) : lo + rng.nextInt(hi - lo + 1);
   }

   private static long now() {
      return System.currentTimeMillis();
   }

   private enum Stage {
      IDLE,
      COLLECTING,
      WAIT_CLOSE;
   }
}
