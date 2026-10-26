package need.clarity.modules.utility;

import java.lang.reflect.Field;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AntiCob extends ModuleBase {
   public static AntiCob INSTANCE;
   private static final Field RIGHT_CLICK_DELAY_FIELD;
   private int savedSlot = -1;
   private boolean placed = false;
   private int timer = 0;
   private BlockPos waterPos = null;

   public AntiCob() {
      super("Anti Cob", "Utility");
      INSTANCE = this;
   }

   private String mode() {
      return this.getSetting("Mode", "Water");
   }

   private boolean silent() {
      return this.getSetting("Silent Rotations", true);
   }

   private int pickUpDelay() {
      return Math.max(1, this.getSetting("Pick Up Delay", 10));
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      this.savedSlot = mc.player == null ? -1 : InventoryHelper.getSelected(mc.player.getInventory());
      this.placed = false;
      this.timer = 0;
      this.waterPos = null;
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && this.savedSlot != -1) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      if (this.silent()) {
         AimAssist.clearSilentRotation();
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         BlockPos feet = mc.player.blockPosition();
         boolean inCob = mc.level.getBlockState(feet).is(Blocks.COBWEB) || mc.level.getBlockState(feet.above()).is(Blocks.COBWEB);
         if (!inCob) {
            this.placed = false;
            this.timer = 0;
         } else if ("Teleport".equalsIgnoreCase(this.mode())) {
            try {
               mc.player.setPos(mc.player.getX(), mc.player.getY() + 1.2, mc.player.getZ());
            } catch (Throwable var7) {
            }
         } else {
            if (!this.placed) {
               int slot = findSlot(mc, Items.WATER_BUCKET);
               if (slot < 0) {
                  return;
               }

               if (this.savedSlot == -1) {
                  this.savedSlot = InventoryHelper.getSelected(mc.player.getInventory());
               }

               InventoryHelper.setSelected(mc.player.getInventory(), slot);
               BlockHitResult hit = findSupportHit(mc, feet);
               if (hit == null) {
                  return;
               }

               if (this.silent()) {
                  float[] a = lookAngles(mc.player.getEyePosition(), hit.getLocation());
                  AimAssist.setSilentRotation(a[0], a[1]);
               }

               setRightClickDelay(mc, 0);
               InteractionResult r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
               if (r.consumesAction()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  this.placed = true;
                  this.timer = 0;
                  this.waterPos = feet;
               }
            } else {
               this.timer++;
               if (this.timer >= this.pickUpDelay()) {
                  int empty = findSlot(mc, Items.BUCKET);
                  if (empty >= 0 && this.waterPos != null) {
                     InventoryHelper.setSelected(mc.player.getInventory(), empty);
                     BlockHitResult hit = findSupportHit(mc, this.waterPos);
                     if (hit != null) {
                        setRightClickDelay(mc, 0);
                        InteractionResult r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                        if (r.consumesAction()) {
                           mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                     }
                  }

                  this.placed = false;
                  this.timer = 0;
                  this.waterPos = null;
               }
            }
         }
      }
   }

   private static int findSlot(Minecraft mc, Item item) {
      Inventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && s.is(item)) {
            return i;
         }
      }

      return -1;
   }

   private static BlockHitResult findSupportHit(Minecraft mc, BlockPos target) {
      double reachSq = mc.player.blockInteractionRange() * mc.player.blockInteractionRange();
      Vec3 eyes = mc.player.getEyePosition();

      for (Direction dir : Direction.values()) {
         BlockPos support = target.relative(dir);
         if (mc.level.getBlockState(support).isSolid()) {
            Direction face = dir.getOpposite();
            Vec3 fn = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 hitVec = Vec3.atCenterOf(support).add(fn.scale(0.5));
            if (!(eyes.distanceToSqr(hitVec) > reachSq)) {
               return new BlockHitResult(hitVec, face, support, false);
            }
         }
      }

      return null;
   }

   private static float[] lookAngles(Vec3 eyes, Vec3 point) {
      Vec3 d = point.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      return new float[]{(float)(Math.atan2(d.x * -1.0, d.z) * 180.0 / Math.PI), (float)(Math.atan2(d.y, h) * -1.0 * 180.0 / Math.PI)};
   }

   private static void setRightClickDelay(Minecraft mc, int value) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var3) {
         }
      }
   }

   static {
      Field f = null;

      try {
         f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
      } catch (NoSuchFieldException var2) {
      }

      RIGHT_CLICK_DELAY_FIELD = f;
   }
}
