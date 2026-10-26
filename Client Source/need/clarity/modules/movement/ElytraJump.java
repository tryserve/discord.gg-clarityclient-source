package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import need.clarity.util.ContainerScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ElytraJump extends ModuleBase {
   private static final String AUTO_SETTING = "Auto Swap";
   private boolean hasFiredOnStart = false;

   public ElytraJump() {
      super("Elytra Jump", "Movement");
   }

   private int getItemSlot(Item item) {
      Minecraft mc = Minecraft.getInstance();
      int finalSlot = -1;

      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getItem(i).getItem() == item) {
            finalSlot = i;
            break;
         }
      }

      if (finalSlot < 9 && finalSlot != -1) {
         finalSlot += 36;
      }

      return finalSlot;
   }

   private int getChestPlateSlot() {
      Minecraft mc = Minecraft.getInstance();
      Item[] items = new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE};

      for (Item item : items) {
         for (int i = 0; i < 36; i++) {
            Item stack = mc.player.getInventory().getItem(i).getItem();
            if (stack == item) {
               if (i < 9) {
                  i += 36;
               }

               return i;
            }
         }
      }

      return -1;
   }

   private void swapToElytra() {
      Minecraft mc = Minecraft.getInstance();
      int elytraSlot = this.getItemSlot(Items.ELYTRA);
      if (elytraSlot != -1 && mc.gameMode != null) {
         int rawSlot = elytraSlot >= 36 ? elytraSlot - 36 : elytraSlot;
         int containerSlot = rawSlot < 9 ? rawSlot + 36 : rawSlot;
         ContainerScreenHelper.shiftClick(mc.gameMode, mc.player.inventoryMenu.containerId, containerSlot, mc.player);
      }
   }

   private void swapToChestplate() {
      Minecraft mc = Minecraft.getInstance();
      int chestplateSlot = this.getChestPlateSlot();
      if (chestplateSlot != -1 && mc.gameMode != null) {
         int rawSlot = chestplateSlot >= 36 ? chestplateSlot - 36 : chestplateSlot;
         int containerSlot = rawSlot < 9 ? rawSlot + 36 : rawSlot;
         ContainerScreenHelper.shiftClick(mc.gameMode, mc.player.inventoryMenu.containerId, containerSlot, mc.player);
      }
   }

   @Override
   public void onEnable() {
      this.hasFiredOnStart = false;
      if (this.getSetting("Auto Swap", false)) {
         this.swapToElytra();
      }

      super.onEnable();
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (this.getSetting("Auto Swap", false) && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            this.swapToChestplate();
         }

         super.onDisable();
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (!mc.player.getAbilities().flying
            && mc.player.onGround()
            && !mc.player.isInWater()
            && !mc.player.isInLava()
            && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
            && !mc.options.keyJump.isDown()) {
            mc.player.jumpFromGround();
         }

         if (!mc.player.getAbilities().flying && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isFallFlying() && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
            )
          {
            mc.player.startFallFlying();
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
         }

         if (mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava()) {
            this.hasFiredOnStart = false;
         }

         if (mc.player.hurtTime > 0 && this.getSetting("Auto Swap", false) && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            this.swapToChestplate();
         } else {
            if (mc.player.getInventory() != null && this.getItemSlot(Items.ELYTRA) == 38 && mc.options.keyJump.isDown() && !mc.player.isFallFlying()) {
               mc.options.keyJump.setDown(false);
            }

            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
               mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
               mc.options.keyJump.setDown(true);
               if (mc.player.isFallFlying()) {
                  float boost = 0.06F + (float)(Math.random() * 0.001F);
                  mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y + boost, mc.player.getDeltaMovement().z);
               }
            } else if (this.getSetting("Auto Swap", false)) {
               this.swapToElytra();
            }
         }
      }
   }
}
