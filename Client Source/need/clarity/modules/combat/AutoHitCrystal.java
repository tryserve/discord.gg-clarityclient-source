package need.clarity.modules.combat;

import java.lang.reflect.Field;
import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.util.AccessorCompat;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoHitCrystal extends ModuleBase {
   private AutoHitCrystal.Stage stage = AutoHitCrystal.Stage.NONE;
   private long stageReadyAt = 0L;
   private long placeReadyAt = 0L;
   private long breakReadyAt = 0L;
   private boolean placedObsidian = false;
   private BlockPos expectedPlacementPos = null;
   private BlockPos clickTargetPos = null;
   private int savedSlot = -1;
   private int lastAttackedCrystalId = -1;
   private final Random rng = new Random();
   private static final Field RIGHT_CLICK_DELAY_FIELD = resolveRightClickDelayField();

   private static Field resolveRightClickDelayField() {
      try {
         Field f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
         return f;
      } catch (NoSuchFieldException e) {
         return null;
      }
   }

   public AutoHitCrystal() {
      super("Auto Hit Crystal", "Combat");
   }

   @Override
   public void onDisable() {
      this.restoreSlotNow(Minecraft.getInstance());
      this.resetState();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null && mc.gameMode != null) {
            if (mc.screen == null) {
               if (!mc.player.isUsingItem()) {
                  long now = System.currentTimeMillis();
                  boolean rmbDown = mc.options.keyUse.isDown();
                  if (!rmbDown && this.stage != AutoHitCrystal.Stage.NONE) {
                     this.restoreSlotNow(mc);
                     this.resetState();
                  } else if (rmbDown) {
                     int guard = 0;

                     AutoHitCrystal.Stage prev;
                     do {
                        prev = this.stage;
                        switch (this.stage) {
                           case NONE:
                              this.tickNone(mc, now);
                              break;
                           case SWITCH_OBSIDIAN:
                              this.tickSwitchObsidian(mc, now);
                              break;
                           case PLACE_OBSIDIAN:
                              this.tickPlaceObsidian(mc, now);
                              break;
                           case SWITCH_CRYSTAL:
                              this.tickSwitchCrystal(mc, now);
                              break;
                           case DONE:
                              this.tickDone(mc, now);
                        }
                     } while (this.stage != prev && this.stage != AutoHitCrystal.Stage.NONE && this.stage != AutoHitCrystal.Stage.DONE && now >= this.stageReadyAt && ++guard < 10);
                  }
               }
            }
         }
      }
   }

   private void tickNone(Minecraft mc, long now) {
      if (this.workWithAllows(mc)) {
         if (this.canActivate(mc)) {
            this.activate(mc, now);
         }
      }
   }

   private boolean canActivate(Minecraft mc) {
      if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal) {
         return this.hasCrystalInHotbar(mc);
      } else if (mc.hitResult instanceof BlockHitResult blockHit) {
         BlockPos pos = blockHit.getBlockPos();
         BlockState state = mc.level.getBlockState(pos);
         if (state.isAir()) {
            return false;
         } else if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
            return this.hasCrystalInHotbar(mc);
         } else if (this.hasObsidianInHotbar(mc) && this.hasCrystalInHotbar(mc)) {
            Direction hitFace = blockHit.getDirection();
            BlockPos placementPos = state.canBeReplaced() ? pos : pos.relative(hitFace);
            BlockState ps = mc.level.getBlockState(placementPos);
            return ps.isAir() || ps.canBeReplaced();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void activate(Minecraft mc, long now) {
      if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal) {
         this.stageReadyAt = now;
         this.stage = AutoHitCrystal.Stage.SWITCH_CRYSTAL;
      } else if (mc.hitResult instanceof BlockHitResult blockHit) {
         BlockPos var12 = blockHit.getBlockPos();
         BlockState hitState = mc.level.getBlockState(var12);
         if (hitState.is(Blocks.OBSIDIAN) || hitState.is(Blocks.BEDROCK)) {
            this.stageReadyAt = now;
            this.stage = AutoHitCrystal.Stage.SWITCH_CRYSTAL;
         } else if (!hitState.isAir()) {
            Direction hitFace = blockHit.getDirection();
            BlockPos placementPos = hitState.canBeReplaced() ? var12 : var12.relative(hitFace);
            BlockState ps = mc.level.getBlockState(placementPos);
            if (ps.isAir() || ps.canBeReplaced()) {
               this.expectedPlacementPos = placementPos;
               this.clickTargetPos = placementPos.below();
               this.placedObsidian = false;
               this.stageReadyAt = now;
               this.stage = AutoHitCrystal.Stage.SWITCH_OBSIDIAN;
            }
         }
      }
   }

   private void tickSwitchObsidian(Minecraft mc, long now) {
      if (now >= this.stageReadyAt) {
         int slot = this.findSlot(mc, Items.OBSIDIAN);
         if (slot == -1) {
            slot = this.findSlot(mc, Items.BEDROCK);
         }

         if (slot == -1) {
            this.resetState();
         } else {
            this.swapTo(mc, slot);
            AccessorCompat.setClickCount(mc.options.keyUse, 0);
            this.setRightClickDelay(mc, 4);
            this.placedObsidian = false;
            this.stageReadyAt = now + this.placeObsidianDelayMs();
            this.stage = AutoHitCrystal.Stage.PLACE_OBSIDIAN;
         }
      }
   }

   private void tickPlaceObsidian(Minecraft mc, long now) {
      if (now >= this.stageReadyAt) {
         if (this.placedObsidian && this.expectedPlacementPos != null) {
            BlockState placed = mc.level.getBlockState(this.expectedPlacementPos);
            if (placed.is(Blocks.OBSIDIAN) || placed.is(Blocks.BEDROCK)) {
               this.placedObsidian = false;
               this.stageReadyAt = now;
               this.stage = AutoHitCrystal.Stage.SWITCH_CRYSTAL;
            }
         } else {
            if (this.expectedPlacementPos == null) {
               if (!(mc.hitResult instanceof BlockHitResult bhr)) {
                  this.resetState();
                  return;
               }

               this.expectedPlacementPos = this.resolvePlacementPos(mc, bhr);
               if (this.expectedPlacementPos == null) {
                  this.resetState();
                  return;
               }

               this.clickTargetPos = this.expectedPlacementPos.below();
            }

            BlockState ps = mc.level.getBlockState(this.expectedPlacementPos);
            if (ps.is(Blocks.OBSIDIAN) || ps.is(Blocks.BEDROCK)) {
               this.placedObsidian = false;
               this.expectedPlacementPos = null;
               this.clickTargetPos = null;
               this.stageReadyAt = now;
               this.stage = AutoHitCrystal.Stage.SWITCH_CRYSTAL;
            } else if (!ps.isAir() && !ps.canBeReplaced()) {
               this.resetState();
            } else {
               Vec3 hitVec = new Vec3(this.expectedPlacementPos.getX() + 0.5, this.expectedPlacementPos.getY(), this.expectedPlacementPos.getZ() + 0.5);
               BlockHitResult syntheticBhr = new BlockHitResult(hitVec, Direction.UP, this.clickTargetPos, false);
               InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, syntheticBhr);
               if (result.consumesAction()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  this.setRightClickDelay(mc, 4);
               }

               this.placedObsidian = true;
            }
         }
      }
   }

   private void tickSwitchCrystal(Minecraft mc, long now) {
      if (now >= this.stageReadyAt) {
         int slot = this.findSlot(mc, Items.END_CRYSTAL);
         if (slot == -1) {
            this.resetState();
         } else {
            this.swapTo(mc, slot);
            this.placeReadyAt = 0L;
            this.breakReadyAt = now + this.breakDelayMs();
            this.stageReadyAt = now;
            this.stage = AutoHitCrystal.Stage.DONE;
         }
      }
   }

   private void tickDone(Minecraft mc, long now) {
      if (!mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
         int slot = this.findSlot(mc, Items.END_CRYSTAL);
         if (slot == -1) {
            this.resetState();
         } else {
            this.swapTo(mc, slot);
         }
      } else {
         if (mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            boolean validBase = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
            if (validBase && now >= this.placeReadyAt) {
               if (this.lastAttackedCrystalId != -1) {
                  if (this.crystalIsAlive(mc, this.lastAttackedCrystalId)) {
                     return;
                  }

                  this.lastAttackedCrystalId = -1;
               }

               if (!this.hasCrystalOnBlock(mc, pos)) {
                  this.simulateUse(mc);
                  this.placeReadyAt = now + this.placeDelayMs();
               }
            }
         }

         if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal crystal && !crystal.isRemoved() && crystal.isAlive() && now >= this.breakReadyAt) {
            this.simulateAttack(mc);
            this.lastAttackedCrystalId = crystal.getId();
            this.breakReadyAt = now + this.breakDelayMs();
            this.handOffToAutoCrystal();
            this.resetState();
         }
      }
   }

   private void handOffToAutoCrystal() {
      AutoCrystal ac = (AutoCrystal)ModuleRegistry.get("Auto Crystal");
      if (ac != null && ac.isEnabled()) {
         ac.beginHandoff(this.savedSlot);
         this.savedSlot = -1;
      } else {
         this.restoreSlotNow(Minecraft.getInstance());
      }
   }

   private void resetState() {
      this.stage = AutoHitCrystal.Stage.NONE;
      this.stageReadyAt = 0L;
      this.placeReadyAt = 0L;
      this.breakReadyAt = 0L;
      this.placedObsidian = false;
      this.expectedPlacementPos = null;
      this.clickTargetPos = null;
      this.lastAttackedCrystalId = -1;
   }

   private void restoreSlotNow(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
   }

   private void simulateUse(Minecraft mc) {
      AccessorCompat.addClick(mc.options.keyUse);
   }

   private void simulateAttack(Minecraft mc) {
      if (mc.hitResult instanceof EntityHitResult ehr) {
         mc.gameMode.attack(mc.player, ehr.getEntity());
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private BlockPos resolvePlacementPos(Minecraft mc, BlockHitResult bhr) {
      try {
         BlockPos pos = bhr.getBlockPos();
         BlockState hitState = mc.level.getBlockState(pos);
         if (hitState.isAir()) {
            return null;
         }

         Direction hitFace = bhr.getDirection();
         return hitState.canBeReplaced() ? pos : pos.relative(hitFace);
      } catch (Exception e) {
         return null;
      }
   }

   private EndCrystal findCrystalAbove(Minecraft mc, BlockPos pos) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal ec && !ec.isRemoved()) {
            BlockPos below = BlockPos.containing(ec.position()).below();
            if (below.equals(pos)) {
               return ec;
            }
         }
      }

      return null;
   }

   private boolean hasCrystalOnBlock(Minecraft mc, BlockPos pos) {
      return this.findCrystalAbove(mc, pos) != null;
   }

   private boolean crystalIsAlive(Minecraft mc, int id) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity.getId() == id && entity instanceof EndCrystal ec) {
            return !ec.isRemoved() && ec.isAlive();
         }
      }

      return false;
   }

   private void setRightClickDelay(Minecraft mc, int value) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var4) {
         }
      }
   }

   private boolean workWithAllows(Minecraft mc) {
      boolean wCrystal = this.getSetting("Work with Crystal", true);
      boolean wObsidian = this.getSetting("Work with Obsidian", true);
      boolean wSword = this.getSetting("Work with Sword", true);
      boolean wTotem = this.getSetting("Work with Totem", true);
      boolean wFist = this.getSetting("Work with Fist", true);
      if (!wCrystal && !wObsidian && !wSword && !wTotem && !wFist) {
         return true;
      } else {
         ItemStack held = mc.player.getMainHandItem();
         if (wCrystal && held.is(Items.END_CRYSTAL)) {
            return true;
         } else if (wObsidian && held.is(Items.OBSIDIAN)) {
            return true;
         } else if (wSword && held.is(ItemTags.SWORDS)) {
            return true;
         } else {
            return wTotem && held.is(Items.TOTEM_OF_UNDYING) ? true : wFist && held.isEmpty();
         }
      }
   }

   private boolean hasCrystalInHotbar(Minecraft mc) {
      return this.findSlot(mc, Items.END_CRYSTAL) != -1;
   }

   private boolean hasObsidianInHotbar(Minecraft mc) {
      return this.findSlot(mc, Items.OBSIDIAN) != -1 || this.findSlot(mc, Items.BEDROCK) != -1;
   }

   private int findSlot(Minecraft mc, Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private void swapTo(Minecraft mc, int slot) {
      if (this.savedSlot == -1) {
         this.savedSlot = getSelected(mc.player.getInventory());
      }

      setSelected(mc.player.getInventory(), slot);
   }

   private static int getSelected(Inventory inv) {
      return InventoryHelper.getSelected(inv);
   }

   private static void setSelected(Inventory inv, int slot) {
      InventoryHelper.setSelected(inv, slot);
   }

   private long placeObsidianDelayMs() {
      return this.randomMs("Place Obsidian Delay", 0L, 25L);
   }

   private long placeDelayMs() {
      return this.randomMs("Place Delay", 0L, 25L);
   }

   private long breakDelayMs() {
      return this.randomMs("Break Delay", 0L, 25L);
   }

   private long randomMs(String key, long loDefault, long hiDefault) {
      long lo = toLong(this.getSetting(key + "_lo", loDefault));
      long hi = toLong(this.getSetting(key + "_hi", hiDefault));
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

   private enum Stage {
      NONE,
      SWITCH_OBSIDIAN,
      PLACE_OBSIDIAN,
      SWITCH_CRYSTAL,
      DONE;
   }
}
