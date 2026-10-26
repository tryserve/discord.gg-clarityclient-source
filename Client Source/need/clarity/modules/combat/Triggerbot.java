package need.clarity.modules.combat;

import java.util.Random;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public class Triggerbot extends ModuleBase {
   private final Minecraft mc = Minecraft.getInstance();
   private final Random rng = new Random();
   private boolean waiting = false;
   private long fireAtMs = -1L;

   public Triggerbot() {
      super("Triggerbot", "Combat");
   }

   @Override
   public void onDisable() {
      this.waiting = false;
      this.fireAtMs = -1L;
      if (!AimAssist.isActive()) {
         AimAssist.clearCurrentTarget();
      }
   }

   @Override
   public void onTick() {
      if (this.mc.player != null && this.mc.level != null) {
         if (!this.getSetting("Not in GUI", true) || this.mc.screen == null) {
            if (!this.getSetting("Not While Using", true) || !this.mc.player.isUsingItem()) {
               if (this.getSetting("Require Mousedown", false)) {
                  boolean mouseHeld = GLFW.glfwGetMouseButton(this.mc.getWindow().handle(), 0) == 1;
                  if (!mouseHeld) {
                     this.resetWait();
                     return;
                  }
               }

               HitResult hit = this.mc.hitResult;
               if (hit != null && hit.getType() == Type.ENTITY) {
                  if (((EntityHitResult)hit).getEntity() instanceof LivingEntity living) {
                     if (living != this.mc.player && living.isAlive() && !Friends.isProtected(living)) {
                        if (!AimAssist.isActive()) {
                           AimAssist.setCurrentTarget(living);
                        }

                        if (this.getSetting("Only Weapon", false) && !this.isWeapon(this.mc.player.getMainHandItem())) {
                           this.resetWait();
                        } else if (this.getSetting("Check Shielding", true) && living.isBlocking()) {
                           this.resetWait();
                        } else {
                           float coolLo = this.getSetting("Cooldown Threshold_lo", 0.9F);
                           float coolHi = this.getSetting("Cooldown Threshold_hi", 1.0F);
                           float needed = coolLo + this.rng.nextFloat() * Math.max(0.0F, coolHi - coolLo);
                           if (this.mc.player.getAttackStrengthScale(0.0F) < needed) {
                              this.resetWait();
                           } else if (this.getSetting("Prioritize Criticals", true) && !this.canCrit()) {
                              this.resetWait();
                           } else {
                              int delayLo = this.getSetting("Extra Delay_lo", 0);
                              int delayHi = this.getSetting("Extra Delay_hi", 0);
                              long now = System.currentTimeMillis();
                              if (!this.waiting) {
                                 long extra = delayHi > delayLo ? (long)(this.rng.nextFloat() * (delayHi - delayLo)) + delayLo : delayLo;
                                 this.fireAtMs = now + extra;
                                 this.waiting = true;
                              }

                              if (now >= this.fireAtMs) {
                                 HitResult reHit = this.mc.hitResult;
                                 if (reHit != null && reHit.getType() == Type.ENTITY) {
                                    Entity e = ((EntityHitResult)reHit).getEntity();
                                    if (e instanceof LivingEntity && e.isAlive() && e != this.mc.player && !Friends.isProtected(e)) {
                                       this.attack(e);
                                    }
                                 }

                                 this.waiting = false;
                                 this.fireAtMs = -1L;
                              }
                           }
                        }
                     } else {
                        this.resetWait();
                        if (!AimAssist.isActive()) {
                           AimAssist.clearCurrentTarget();
                        }
                     }
                  } else {
                     this.resetWait();
                     if (!AimAssist.isActive()) {
                        AimAssist.clearCurrentTarget();
                     }
                  }
               } else {
                  this.resetWait();
                  if (!AimAssist.isActive()) {
                     AimAssist.clearCurrentTarget();
                  }
               }
            }
         }
      }
   }

   private boolean canCrit() {
      if (this.mc.player == null) {
         return false;
      } else if (this.mc.player.onGround()) {
         return true;
      } else if (this.mc.player.isInWater() || this.mc.player.isInLava()) {
         return false;
      } else {
         return this.mc.player.isPassenger() ? false : this.mc.player.getDeltaMovement().y <= -0.1;
      }
   }

   private boolean isWeapon(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         Item item = stack.getItem();
         if (item instanceof TridentItem) {
            return true;
         } else {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (stack.is(ItemTags.SWORDS)) {
               return true;
            } else {
               return stack.is(ItemTags.AXES) ? true : stack.is(ItemTags.MACE_ENCHANTABLE);
            }
         }
      }
   }

   private void attack(Entity target) {
      if (this.mc.gameMode != null && this.mc.player != null) {
         PerAttackEvents.fire(new EventStartAttack(this.mc.player, target));
         this.mc.gameMode.attack(this.mc.player, target);
         this.mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private void resetWait() {
      this.waiting = false;
      this.fireAtMs = -1L;
   }
}
