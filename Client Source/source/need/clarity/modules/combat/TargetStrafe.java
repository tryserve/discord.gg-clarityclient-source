package need.clarity.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import need.clarity.LocalPlayerRotationState;
import need.clarity.PerMovementEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.movement.MoveFix;
import need.clarity.modules.utility.Targets;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TargetStrafe extends ModuleBase {
   private static TargetStrafe instance;
   private static final int FLIP_COOLDOWN_TICKS = 8;
   private static final int RING_SEGMENTS = 72;
   private int direction = 1;
   private int flipCooldown = 0;
   private boolean strafingThisTick = false;
   private volatile LivingEntity renderTarget = null;

   public TargetStrafe() {
      super("Target Strafe", "Combat");
      instance = this;
      PerMovementEvents.register(this::onMovement);
      WorldRenderEvent.register(this::onWorldRender);
   }

   public static TargetStrafe getInstance() {
      return instance;
   }

   public static boolean isStrafing() {
      return instance != null && instance.isEnabled() && instance.strafingThisTick;
   }

   @Override
   public void onEnable() {
      this.flipCooldown = 0;
      this.strafingThisTick = false;
      String start = this.getSetting("Start Direction", "Left");
      this.direction = "Right".equals(start) ? -1 : 1;
   }

   @Override
   public void onDisable() {
      this.strafingThisTick = false;
      this.renderTarget = null;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (this.flipCooldown > 0) {
            this.flipCooldown--;
         }

         this.renderTarget = this.findTarget(mc);
         if (this.strafingThisTick && mc.player.horizontalCollision && this.getSetting("Switch On Collision", true)) {
            this.flip();
         }
      } else {
         this.renderTarget = null;
      }
   }

   private void onMovement(MoveFix.EventMovementInput e) {
      this.strafingThisTick = false;
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            if (!player.getAbilities().flying && !player.isFallFlying() && !player.isPassenger()) {
               if (this.activationHeld(e)) {
                  LivingEntity target = this.findTarget(mc);
                  this.renderTarget = target;
                  if (target != null) {
                     double radius = this.getSetting("Distance", 2.5);
                     radius = Math.max(0.1, radius);
                     Vec3 t = this.targetPosition(target);
                     double px = player.getX();
                     double pz = player.getZ();
                     double dx = px - t.x;
                     double dz = pz - t.z;
                     double dist = Math.sqrt(dx * dx + dz * dz);
                     if (dist < 1.0E-4) {
                        dx = 1.0E-4;
                        dz = 0.0;
                        dist = 1.0E-4;
                     }

                     double rx = dx / dist;
                     double rz = dz / dist;
                     if (this.getSetting("Manual Direction", true)) {
                        int leftSign = leftOrbitSign(rx, rz);
                        boolean l = e.originalLeft;
                        boolean r = e.originalRight;
                        if (l && !r) {
                           this.direction = leftSign;
                        } else if (r && !l) {
                           this.direction = -leftSign;
                        }
                     }

                     Vec3 want = carrotDirection(px, pz, t, rx, rz, radius, this.direction);
                     if (this.getSetting("Avoid Edges", true) && !isSafe(mc, player, want)) {
                        if (this.flipCooldown != 0) {
                           return;
                        }

                        Vec3 other = carrotDirection(px, pz, t, rx, rz, radius, -this.direction);
                        if (!isSafe(mc, player, other)) {
                           return;
                        }

                        this.flip();
                        want = other;
                     }

                     if (!(want.lengthSqr() < 1.0E-8)) {
                        float moveYaw = player.getYRot();
                        float wantYaw = MoveFix.yawOf(want.x, want.z);
                        float[] keys = MoveFix.bestInputFor(moveYaw, wantYaw);
                        e.forwardAmount = keys[0];
                        e.sidewaysAmount = keys[1];
                        e.syncBooleansFromFloats();
                        if (!e.forward) {
                           e.sprint = false;
                           if (player.isSprinting()) {
                              player.setSprinting(false);
                           }
                        }

                        if (this.getSetting("Auto Jump", false) && player.onGround()) {
                           e.jump = true;
                        }

                        this.strafingThisTick = true;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean activationHeld(MoveFix.EventMovementInput e) {
      String mode = this.getSetting("Activation", "Holding Forward");

      return switch (mode) {
         case "Always" -> true;
         case "Holding Jump" -> e.originalJump;
         default -> e.originalForward;
      };
   }

   private static Vec3 carrotDirection(double px, double pz, Vec3 t, double rx, double rz, double radius, int dir) {
      double step = Mth.clamp(0.9 / radius, 0.15, 1.2) * dir;
      double cos = Math.cos(step);
      double sin = Math.sin(step);
      double cx = rx * cos - rz * sin;
      double cz = rx * sin + rz * cos;
      double qx = t.x + cx * radius;
      double qz = t.z + cz * radius;
      return new Vec3(qx - px, 0.0, qz - pz);
   }

   private static int leftOrbitSign(double rx, double rz) {
      float realYaw = LocalPlayerRotationState.silentActive ? LocalPlayerRotationState.realYaw : Minecraft.getInstance().player.getYRot();
      double y = Math.toRadians(realYaw);
      double lx = Math.cos(y);
      double lz = Math.sin(y);
      double tx = -rz;
      double tz = rx;
      return tx * lx + tz * lz >= 0.0 ? 1 : -1;
   }

   private void flip() {
      if (this.flipCooldown <= 0) {
         this.direction = -this.direction;
         this.flipCooldown = 8;
      }
   }

   private static boolean isSafe(Minecraft mc, LocalPlayer player, Vec3 want) {
      double len = Math.sqrt(want.x * want.x + want.z * want.z);
      if (len < 1.0E-6) {
         return true;
      }

      double ax = player.getX() + want.x / len * 0.8;
      double az = player.getZ() + want.z / len * 0.8;
      BlockPos feet = BlockPos.containing(ax, player.getY() + 0.01, az);
      if (isHazard(mc.level.getBlockState(feet)) || isHazard(mc.level.getBlockState(feet.below()))) {
         return false;
      }

      if (!player.onGround()) {
         return true;
      }

      for (int i = 1; i <= 3; i++) {
         BlockState s = mc.level.getBlockState(feet.below(i));
         if (s.isSolid() || !s.getFluidState().isEmpty()) {
            return true;
         }

         if (isHazard(s)) {
            return false;
         }
      }

      return false;
   }

   private static boolean isHazard(BlockState s) {
      return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.COBWEB);
   }

   private LivingEntity findTarget(Minecraft mc) {
      double range = this.getSetting("Range", 6.0);
      double rangeSq = range * range;
      LivingEntity ka = KillAura.getTarget();
      if (KillAura.isActive() && valid(mc, ka, rangeSq)) {
         return ka;
      }

      if (this.getSetting("Only With Kill Aura", false)) {
         return null;
      }

      LivingEntity aa = AimAssist.getCurrentTarget();
      if (valid(mc, aa, rangeSq)) {
         return aa;
      }

      List<LivingEntity> candidates = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (valid(mc, p, rangeSq)) {
            candidates.add(p);
         }
      }

      return candidates.isEmpty() ? null : Targets.getTarget(candidates, mc.player);
   }

   private static boolean valid(Minecraft mc, LivingEntity e, double rangeSq) {
      if (e == null || !e.isAlive() || e.isRemoved()) {
         return false;
      } else {
         return !Targets.isTargetAllowed(e, mc.player) ? false : mc.player.distanceToSqr(e) <= rangeSq;
      }
   }

   private Vec3 targetPosition(LivingEntity target) {
      if (!this.getSetting("Prediction", true)) {
         return target.position();
      }

      double vx = target.getX() - target.xo;
      double vz = target.getZ() - target.zo;
      double ticks = this.getSetting("Prediction Ticks", 2.0);
      return new Vec3(target.getX() + vx * ticks, target.getY(), target.getZ() + vz * ticks);
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled() && this.getSetting("Render", true)) {
         LivingEntity t = this.renderTarget;
         if (t != null && t.isAlive()) {
            double radius = Math.max(0.1, this.getSetting("Distance", 2.5));
            double cx = Mth.lerp(partialTick, t.xo, t.getX());
            double cy = Mth.lerp(partialTick, t.yo, t.getY()) + 0.05;
            double cz = Mth.lerp(partialTick, t.zo, t.getZ());
            int rgb = ThemeColors.resolveRgb(this.getSetting("Render Color", "#3a7fcc"), 3833804, false);
            int alpha = this.strafingThisTick ? 200 : 110;
            int argb = alpha << 24 | rgb & 16777215;
            double h = 0.035;
            List<AABB> dots = new ArrayList<>(72);

            for (int i = 0; i < 72; i++) {
               double a = (Math.PI * 2) * i / 72.0;
               double x = cx + Math.cos(a) * radius;
               double z = cz + Math.sin(a) * radius;
               dots.add(new AABB(x - h, cy, z - h, x + h, cy + h, z + h));
            }

            ClarityRenderUtils.drawBoxes(poseStack, dots, argb, 0, true, false);
         }
      }
   }
}
