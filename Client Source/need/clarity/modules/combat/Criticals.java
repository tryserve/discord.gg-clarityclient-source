package need.clarity.modules.combat;

import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerMovementEvents;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class Criticals extends ModuleBase {
   private static final String NORMAL = "Normal";
   private static final String STOP_SPRINT = "Stop Sprint";
   private boolean suppressSprint = false;
   private LivingEntity pendingTarget = null;
   private boolean justAttacked = false;

   public Criticals() {
      super("Criticals", "Combat");
      PerAttackEvents.register(this::onAttack);
      PerMovementEvents.register(this::onMovement);
   }

   @Override
   public void onEnable() {
      this.suppressSprint = false;
      this.pendingTarget = null;
      this.justAttacked = false;
   }

   @Override
   public void onDisable() {
      this.suppressSprint = false;
      this.pendingTarget = null;
      this.justAttacked = false;
   }

   @Override
   public void onTick() {
      this.justAttacked = false;
      if (this.pendingTarget != null) {
         Minecraft mc = Minecraft.getInstance();
         LivingEntity t = this.pendingTarget;
         this.pendingTarget = null;
         LocalPlayer p = mc.player;
         if (p != null && mc.level != null && mc.gameMode != null && mc.screen == null) {
            if (t.isAlive() && !t.isRemoved()) {
               if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() == t) {
                  mc.gameMode.attack(p, t);
                  p.swing(InteractionHand.MAIN_HAND);
                  this.justAttacked = true;
               }
            }
         }
      }
   }

   private void onMovement(MoveFix.EventMovementInput e) {
      if (this.suppressSprint) {
         this.suppressSprint = false;
         if (this.isEnabled()) {
            e.sprint = false;
         }
      }
   }

   private void onAttack(EventStartAttack e) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer p = mc.player;
         if (p != null && mc.level != null && mc.screen == null) {
            if (this.justAttacked) {
               e.cancel();
            } else if (this.pendingTarget != null) {
               e.cancel();
            } else if (e.getTarget() instanceof LivingEntity living && living.isAlive()) {
               if (!Friends.isProtected(living)) {
                  if (this.canCritHere(p)) {
                     String mode = this.getSetting("Mode", "Normal");
                     if ("Stop Sprint".equals(mode)) {
                        this.stopSprintMode(p, e, living);
                     } else {
                        this.normalMode(p);
                     }
                  }
               }
            }
         }
      }
   }

   private void stopSprintMode(LocalPlayer p, EventStartAttack e, LivingEntity target) {
      if (!p.onGround() && !(p.fallDistance <= 0.0)) {
         if (p.isSprinting()) {
            e.cancel();
            this.pendingTarget = target;
            p.setSprinting(false);
            this.suppressSprint = true;
         }
      }
   }

   private void normalMode(LocalPlayer p) {
      this.dropSprint(p);
      if (p.onGround()) {
         double x = p.getX();
         double y = p.getY();
         double z = p.getZ();
         boolean hc = p.horizontalCollision;
         p.connection.send(new Pos(x, y + 0.0625, z, false, hc));
         p.connection.send(new Pos(x, y, z, false, hc));
      }
   }

   private void dropSprint(LocalPlayer p) {
      if (p.isSprinting()) {
         p.setSprinting(false);
         this.suppressSprint = true;
         p.connection.send(new ServerboundPlayerCommandPacket(p, Action.STOP_SPRINTING));
         AccessorCompat.setWasSprinting(p, false);
      }
   }

   private boolean canCritHere(LocalPlayer p) {
      if (p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) {
         return false;
      } else {
         return p.isInWater() || p.isInLava() || p.onClimbable() ? false : !p.hasEffect(MobEffects.BLINDNESS);
      }
   }
}
