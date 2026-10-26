package need.clarity.modules.movement;

import java.util.List;
import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Speed extends ModuleBase {
   public Speed() {
      super("Speed", "Movement");
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         String mode = this.getSetting("Mode", "Normal");
         if ("Grim Collision".equals(mode)) {
            this.tickGrimCollision(mc);
         } else {
            this.tickNormal(mc);
         }
      }
   }

   private void applyGroundStrafe(LocalPlayer player, boolean fwd, boolean back, boolean left, boolean right) {
      Vec3 vel = player.getDeltaMovement();
      double speed = Math.hypot(vel.x, vel.z);
      if (!(speed <= 1.0E-5)) {
         Vec3 dir = this.movementDir(player, fwd, back, left, right);
         if (dir != null) {
            player.setDeltaMovement(dir.x * speed, vel.y, dir.z * speed);
         }
      }
   }

   private void addHorizontalSpeed(LocalPlayer player, double amount) {
      Vec3 vel = player.getDeltaMovement();
      double speed = Math.hypot(vel.x, vel.z);
      if (!(speed <= 1.0E-5)) {
         double factor = (speed + amount) / speed;
         player.setDeltaMovement(vel.x * factor, vel.y, vel.z * factor);
      }
   }

   private Vec3 movementDir(LocalPlayer player, boolean fwd, boolean back, boolean left, boolean right) {
      double f = (fwd ? 1.0 : 0.0) - (back ? 1.0 : 0.0);
      double s = (left ? 1.0 : 0.0) - (right ? 1.0 : 0.0);
      if (f == 0.0 && s == 0.0) {
         return null;
      }

      double yaw = Math.toRadians(player.getYRot());
      double sin = Math.sin(yaw);
      double cos = Math.cos(yaw);
      double x = s * cos - f * sin;
      double z = f * cos + s * sin;
      double len = Math.hypot(x, z);
      return len <= 1.0E-5 ? null : new Vec3(x / len, 0.0, z / len);
   }

   private void tickGrimCollision(Minecraft mc) {
      LocalPlayer player = mc.player;
      MoveFix.EventMovementInput event = PerMovementEvents.lastEvent;
      boolean anyKey = event != null
         ? event.forward || event.back || event.left || event.right
         : mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
      if (anyKey) {
         if (!(player.getHealth() <= 0.0F)) {
            float shrinkBox = this.getSetting("Shrink Box", 0.5F);
            float boostSpeed = this.getSetting("Boost Speed", 0.08F);
            AABB inflated = player.getBoundingBox().inflate(shrinkBox);
            List<Entity> nearby = mc.level.getEntities(player, inflated, e -> e instanceof LivingEntity && !(e instanceof ArmorStand));
            long collisions = nearby.stream().filter(e -> inflated.intersects(e.getBoundingBox())).count();
            if (collisions != 0L) {
               double yaw = Math.toRadians(this.resolveCollisionYaw(player));
               double boost = boostSpeed * (float)collisions;
               player.push(-Math.sin(yaw) * boost, 0.0, Math.cos(yaw) * boost);
            }
         }
      }
   }

   private float resolveCollisionYaw(LocalPlayer player) {
      String dir = this.getSetting("Collision Direction", "Silent Look");
      return "Silent Look".equals(dir) && AimAssist.hasSilentRotation() ? AimAssist.getSilentServerYaw() : player.getYRot();
   }

   private void tickNormal(Minecraft mc) {
      LocalPlayer player = mc.player;
      MoveFix.EventMovementInput event = PerMovementEvents.lastEvent;
      boolean forward = event != null ? event.forward : mc.options.keyUp.isDown();
      boolean back = event != null ? event.back : mc.options.keyDown.isDown();
      boolean left = event != null ? event.left : mc.options.keyLeft.isDown();
      boolean right = event != null ? event.right : mc.options.keyRight.isDown();
      if (!forward && !back && !left && !right) {
         player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
      } else {
         float speedBPS = this.getSetting("Speed", 4.0F);
         double vel = speedBPS / 20.0;
         double dir = movementAngle(player, forward, back, left, right);
         double dx = Math.sin(dir) * -1.0 * vel;
         double dz = Math.cos(dir) * vel;
         player.setDeltaMovement(dx, player.getDeltaMovement().y, dz);
      }
   }

   private static double movementAngle(LocalPlayer player, boolean forward, boolean back, boolean left, boolean right) {
      float yaw = player.getYRot();
      float offset = 0.0F;
      if (forward) {
         if (left && !right) {
            offset = -45.0F;
         } else if (right && !left) {
            offset = 45.0F;
         }
      } else if (back) {
         if (left && !right) {
            offset = -135.0F;
         } else if (right && !left) {
            offset = 135.0F;
         } else {
            offset = 180.0F;
         }
      } else if (left && !right) {
         offset = -90.0F;
      } else if (right && !left) {
         offset = 90.0F;
      }

      return Math.toRadians(yaw + offset);
   }
}
