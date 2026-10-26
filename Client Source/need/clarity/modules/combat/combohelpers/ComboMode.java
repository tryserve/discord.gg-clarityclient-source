package need.clarity.modules.combat.combohelpers;

import need.clarity.EventStartAttack;
import need.clarity.friends.Friends;
import need.clarity.modules.movement.MoveFix;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;

public abstract class ComboMode {
   protected static final Minecraft mc = Minecraft.getInstance();
   protected int attackTick = -1;

   public void onEnable() {
      this.attackTick = -1;
   }

   public void onDisable() {
      this.attackTick = -1;
   }

   public void onAttack(EventStartAttack e) {
      if (mc.player != null && mc.level != null) {
         if (mc.screen == null) {
            if (this.validateAttack(e)) {
               if (ComboHelper.instance == null || !ComboHelper.instance.getSetting("Only Sword", false) || this.isSword()) {
                  if (mc.hitResult != null) {
                     if (mc.hitResult instanceof EntityHitResult hit) {
                        if (hit.getEntity() instanceof LivingEntity target) {
                           if (this.isEntityAllowed(target)) {
                              this.attackTick = mc.player.tickCount;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onMovement(MoveFix.EventMovementInput e) {
      if (mc.player != null && mc.level != null) {
         if (mc.screen == null) {
            if (!mc.player.isDeadOrDying()) {
               if (this.attackTick != -1) {
                  this.applyMovement(e);
               }
            }
         }
      }
   }

   protected boolean validateAttack(EventStartAttack e) {
      return true;
   }

   protected abstract void applyMovement(MoveFix.EventMovementInput var1);

   protected boolean isEntityAllowed(LivingEntity entity) {
      if (entity == null) {
         return false;
      } else {
         return Friends.isProtected(entity) ? false : entity.isAlive() && !entity.isRemoved() && !entity.isDeadOrDying();
      }
   }

   private boolean isSword() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.isEmpty()) {
            return false;
         } else {
            return stack.getItem() instanceof TridentItem ? true : stack.is(ItemTags.SWORDS);
         }
      }
   }
}
