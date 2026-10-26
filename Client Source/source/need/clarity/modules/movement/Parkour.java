package need.clarity.modules.movement;

import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Parkour extends ModuleBase {
   public static Parkour INSTANCE;
   private int cooldown = 0;
   private int sprintHold = 0;

   public Parkour() {
      super("Parkour", "Movement");
      INSTANCE = this;
      PerMovementEvents.register(this::onMovementInput);
   }

   private double edgeDistance() {
      return clamp(this.getSetting("Edge Distance", 0.0), 0.0, 1.0);
   }

   private double minDrop() {
      return clamp(this.getSetting("Min Drop", 1.1), 0.6, 6.0);
   }

   private double minSpeed() {
      return clamp(this.getSetting("Min Speed", 0.05), 0.0, 0.5);
   }

   private int cooldownTicks() {
      return (int)Math.round(clamp(this.getSetting("Cooldown", 3.0), 0.0, 20.0));
   }

   private boolean onlyForward() {
      return this.getSetting("Only Forward", true);
   }

   private boolean onlySprinting() {
      return this.getSetting("Only Sprinting", false);
   }

   private boolean notSneaking() {
      return this.getSetting("Not While Sneaking", true);
   }

   private boolean sprintJump() {
      return this.getSetting("Sprint Jump", false);
   }

   private boolean ignoreSmallGaps() {
      return this.getSetting("Skip Tiny Gaps", false);
   }

   @Override
   public void onDisable() {
      this.cooldown = 0;
      this.sprintHold = 0;
   }

   private void onMovementInput(MoveFix.EventMovementInput event) {
      if (this.isEnabled()) {
         if (this.cooldown > 0) {
            this.cooldown--;
         }

         Minecraft mc = Minecraft.getInstance();
         LocalPlayer p = mc.player;
         Level level = mc.level;
         if (p != null && level != null && mc.screen == null) {
            if (this.sprintHold > 0) {
               this.sprintHold--;
               event.sprint = true;
            }

            if (!event.jump) {
               if (p.onGround()) {
                  if (!p.isInWater() && !p.isInLava() && !p.onClimbable() && !p.isPassenger() && !p.getAbilities().flying && !p.isFallFlying()) {
                     if (!this.notSneaking() || !event.sneak) {
                        if (!this.onlyForward() || event.forward) {
                           if (!this.onlySprinting() || p.isSprinting() || event.sprint) {
                              if (this.cooldown <= 0) {
                                 double[] dir = predictMove(p, level, event);
                                 if (dir != null) {
                                    double speed = dir[2];
                                    if (!(speed < this.minSpeed())) {
                                       double look = speed + this.edgeDistance() + 0.03;
                                       AABB box = p.getBoundingBox();
                                       AABB ahead = box.move(dir[0] * look, 0.0, dir[1] * look);
                                       AABB half = box.move(dir[0] * look * 0.5, 0.0, dir[1] * look * 0.5);
                                       double drop = this.minDrop();
                                       if (groundBelow(level, p, box, drop)) {
                                          if (!groundBelow(level, p, ahead, drop) || !groundBelow(level, p, half, drop)) {
                                             if (!this.ignoreSmallGaps() || !landingRightThere(level, p, ahead, dir, drop)) {
                                                event.jump = true;
                                                if (this.sprintJump() && event.forward) {
                                                   event.sprint = true;
                                                   this.sprintHold = 3;
                                                }

                                                this.cooldown = this.cooldownTicks();
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static double[] predictMove(LocalPlayer p, Level level, MoveFix.EventMovementInput e) {
      Vec3 v = p.getDeltaMovement();
      double mx = v.x;
      double mz = v.z;
      float fwd = (e.forward ? 1 : 0) - (e.back ? 1 : 0);
      float side = (e.left ? 1 : 0) - (e.right ? 1 : 0);
      if (fwd != 0.0F || side != 0.0F) {
         double len = Math.sqrt(fwd * fwd + side * side);
         double f = fwd / len * 0.98;
         double sd = side / len * 0.98;
         if (e.sneak) {
            f *= 0.3;
            sd *= 0.3;
         }

         double yaw = Math.toRadians(p.getYRot());
         double sin = Math.sin(yaw);
         double cos = Math.cos(yaw);
         double ix = sd * cos - f * sin;
         double iz = f * cos + sd * sin;
         float friction = 0.6F;

         try {
            friction = level.getBlockState(BlockPos.containing(p.getX(), p.getBoundingBox().minY - 0.5, p.getZ())).getBlock().getFriction();
         } catch (Throwable var31) {
         }

         double speed = p.getSpeed();
         if ((e.sprint || p.isSprinting()) && fwd > 0.0F && !p.isSprinting()) {
            speed *= 1.3;
         }

         double accel = speed * (0.21600002 / (friction * friction * friction));
         mx += ix * accel;
         mz += iz * accel;
      }

      double sp = Math.sqrt(mx * mx + mz * mz);
      return sp < 1.0E-4 ? null : new double[]{mx / sp, mz / sp, sp};
   }

   private static boolean groundBelow(Level level, LocalPlayer p, AABB box, double depth) {
      AABB under = new AABB(box.minX + 0.001, box.minY - depth, box.minZ + 0.001, box.maxX - 0.001, box.minY - 0.001, box.maxZ - 0.001);
      return !level.noBlockCollision(p, under);
   }

   private static boolean landingRightThere(Level level, LocalPlayer p, AABB ahead, double[] dir, double depth) {
      AABB further = ahead.move(dir[0] * 0.5, 0.0, dir[1] * 0.5);
      AABB under = new AABB(further.minX, further.minY - 0.6, further.minZ, further.maxX, further.minY - 0.001, further.maxZ);
      return !level.noBlockCollision(p, under);
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
