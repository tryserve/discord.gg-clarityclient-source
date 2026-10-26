package need.clarity.modules.combat;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.util.RotDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class KillAura extends ModuleBase {
   private static volatile KillAura INSTANCE;
   private final Random rng = new Random();
   private long lastFrameTime = -1L;
   private boolean rotating = false;
   private LivingEntity humanTarget = null;
   private final double[] goal = new double[]{0.5, 0.65, 0.5};
   private final double[] point = new double[]{0.5, 0.65, 0.5};
   private final double[] pointVel = new double[]{0.0, 0.0, 0.0};
   private long nextPointAtMs = 0L;
   private long reactUntilMs = -1L;
   private float yawVel = 0.0F;
   private float pitchVel = 0.0F;
   private double speedMul = 1.0;
   private double speedMulGoal = 1.0;
   private long nextSpeedAtMs = 0L;
   private final double[] phases = new double[24];
   private volatile LivingEntity target = null;
   private boolean waiting = false;
   private long fireAtMs = -1L;

   public KillAura() {
      super("Kill Aura", "Combat");
      INSTANCE = this;

      for (int i = 0; i < this.phases.length; i++) {
         this.phases[i] = this.rng.nextDouble() * Math.PI * 2.0;
      }

      PerFrameEvents.register(this::onRenderFrame);
   }

   public static boolean isActive() {
      KillAura i = INSTANCE;
      return i != null && i.isEnabled();
   }

   public static LivingEntity getTarget() {
      KillAura i = INSTANCE;
      return i == null ? null : i.target;
   }

   @Override
   public void onEnable() {
      this.lastFrameTime = -1L;
      this.resetWait();
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.resetWait();
      AimAssist.releaseSharedTarget();
   }

   private void onRenderFrame() {
      Minecraft mc = Minecraft.getInstance();
      long now = System.currentTimeMillis();
      long deltaMs = this.lastFrameTime < 0L ? 1L : Math.min(now - this.lastFrameTime, 100L);
      this.lastFrameTime = now;
      if (mc.player == null || mc.level == null) {
         this.rotating = false;
         this.target = null;
         AimAssist.releaseSharedTarget();
      } else if (!this.isEnabled()) {
         this.target = null;
         this.humanTarget = null;
         AimAssist.releaseSharedTarget();
         if (this.rotating) {
            this.returnToCamera(mc, deltaMs);
         }
      } else if (!this.canRun(mc)) {
         this.target = null;
         this.humanTarget = null;
         AimAssist.releaseSharedTarget();
         if (this.rotating) {
            this.returnToCamera(mc, deltaMs);
         }
      } else if (!CrystalAura.yieldsKillAura()) {
         LivingEntity t = this.findTarget(mc);
         this.target = t;
         if (t == null) {
            this.humanTarget = null;
            AimAssist.releaseSharedTarget();
            if (this.rotating) {
               this.returnToCamera(mc, deltaMs);
            }
         } else {
            AimAssist.setCurrentTarget(t);
            boolean human = this.getSetting("Humanize", true);
            String aimMode = this.getSetting("Aim At", "Dynamic");
            double dt = Math.min(deltaMs, 50L) / 1000.0;
            double tSec = System.nanoTime() / 1.0E9;
            if (t != this.humanTarget) {
               boolean wasLocked = this.humanTarget != null && this.rotating;
               this.humanTarget = t;
               this.pickGoal(aimMode);
               this.nextPointAtMs = now + this.randRange("Point Change", 500, 1400);
               if (human) {
                  long react = this.randRange("Reaction Delay", 60, 180);
                  this.reactUntilMs = now + (wasLocked ? react / 2L : react);
               } else {
                  this.reactUntilMs = -1L;
               }

               if (!wasLocked) {
                  this.yawVel = 0.0F;
                  this.pitchVel = 0.0F;
               }
            }

            if (human && now < this.reactUntilMs) {
               if (this.rotating) {
                  AimAssist.setSilentRotation(AimAssist.getSilentServerYaw(), AimAssist.getSilentServerPitch());
               }
            } else {
               Vec3 eyes = mc.player.getEyePosition();
               Vec3 aimPos = human ? this.humanPoint(t, eyes, aimMode, now, dt, tSec) : this.aimPoint(t, eyes);
               Vec3 delta = aimPos.subtract(eyes);
               if (!(delta.lengthSqr() < 1.0E-6)) {
                  double hDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                  float currentYaw;
                  float currentPitch;
                  if (this.rotating) {
                     currentYaw = AimAssist.getSilentServerYaw();
                     currentPitch = AimAssist.getSilentServerPitch();
                  } else {
                     currentYaw = mc.player.getYRot();
                     currentPitch = mc.player.getXRot();
                  }

                  float maxPitch = this.getSetting("Max Pitch", 90.0F);
                  float targetYaw = hDist < 0.05 ? currentYaw : (float)(Math.atan2(delta.x * -1.0, delta.z) * 180.0 / Math.PI);
                  float targetPitch = (float)(Math.atan2(delta.y, Math.max(hDist, 1.0E-4)) * -1.0 * 180.0 / Math.PI);
                  float negMaxPitch = maxPitch * -1.0F;
                  targetPitch = Math.max(negMaxPitch, Math.min(maxPitch, targetPitch));
                  if (Float.isFinite(targetPitch)) {
                     if ((Math.abs(targetPitch) > 80.0F || Math.abs(currentPitch) > 80.0F) && RotDebug.enabled()) {
                        RotDebug.report(
                           "Kill Aura targetPitch="
                              + targetPitch
                              + " currentPitch="
                              + currentPitch
                              + " rotating="
                              + this.rotating
                              + " target="
                              + t.getName().getString()
                              + " type="
                              + t.getType().toShortString()
                              + " hDist="
                              + String.format("%.2f", hDist)
                              + " dy="
                              + String.format("%.2f", delta.y)
                              + " dist="
                              + String.format("%.2f", eyes.distanceTo(aimPos))
                        );
                     }

                     int hSpeed = this.getSetting("Horizontal Speed", 15);
                     int vSpeed = this.getSetting("Vertical Speed", 15);
                     boolean aimH = this.getSetting("Aim Horizontally", true);
                     boolean aimV = this.getSetting("Aim Vertically", true);
                     float newYaw;
                     float newPitch;
                     if (human) {
                        int jit = this.getSetting("Jitter", 30);
                        float jitAmp = jit / 100.0F * 0.8F;
                        targetYaw += (float)(this.noise(tSec, 0) * jitAmp);
                        targetPitch += (float)(this.noise(tSec, 1) * jitAmp * 0.6F);
                        int speedVar = this.getSetting("Speed Variation", 30);
                        if (now >= this.nextSpeedAtMs) {
                           this.nextSpeedAtMs = now + 250L + (long)(this.rng.nextFloat() * 700.0F);
                           this.speedMulGoal = 1.0 + (this.rng.nextDouble() * 2.0 - 1.0) * (speedVar / 100.0) * 0.5;
                        }

                        this.speedMul = this.speedMul + (this.speedMulGoal - this.speedMul) * Math.min(1.0, dt * 3.0);
                        float yawDiff = wrapDegrees(targetYaw - currentYaw);
                        float pitchDiff = targetPitch - currentPitch;
                        double dist = Math.sqrt((double)yawDiff * yawDiff + (double)pitchDiff * pitchDiff);
                        int over = this.getSetting("Overshoot", 25);
                        double zeta = 1.0 - over / 100.0 * 0.45;
                        if (dist < 1.5) {
                           zeta = 1.0;
                        } else if (dist > 35.0) {
                           zeta = Math.max(0.5, zeta - 0.08);
                        }

                        int maxTurnSetting = this.getSetting("Max Turn Speed", 800);
                        double maxTurn = maxTurnSetting * (0.85 + 0.3 * Math.abs(this.noise(tSec * 0.35, 2)));
                        double omegaH = hSpeed * 1.1 * this.speedMul;
                        double omegaV = vSpeed * 1.1 * this.speedMul;
                        int steps = Math.max(1, (int)Math.ceil(dt / 0.008));
                        double h = dt / steps;
                        double yd = yawDiff;
                        double pd = pitchDiff;
                        double yv = this.yawVel;
                        double pv = this.pitchVel;
                        double negMaxTurn = maxTurn * -1.0;

                        for (int i = 0; i < steps; i++) {
                           if (aimH) {
                              double var71 = yv + (omegaH * omegaH * yd - 2.0 * zeta * omegaH * yv) * h;
                              yv = Math.max(negMaxTurn, Math.min(maxTurn, var71));
                              yd -= yv * h;
                           } else {
                              yv = 0.0;
                           }

                           if (aimV) {
                              double var72 = pv + (omegaV * omegaV * pd - 2.0 * zeta * omegaV * pv) * h;
                              pv = Math.max(negMaxTurn, Math.min(maxTurn, var72));
                              pd -= pv * h;
                           } else {
                              pv = 0.0;
                           }
                        }

                        this.yawVel = (float)yv;
                        this.pitchVel = (float)pv;
                        newYaw = aimH ? (float)(currentYaw + (yawDiff - yd)) : currentYaw;
                        newPitch = aimV ? (float)(targetPitch - pd) : currentPitch;
                     } else {
                        float yawDiff = wrapDegrees(targetYaw - currentYaw);
                        float pitchDiff = wrapDegrees(targetPitch - currentPitch);
                        float yawStep = clampAbs(yawDiff * (hSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(yawDiff));
                        float pitchStep = clampAbs(pitchDiff * (vSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(pitchDiff));
                        newYaw = aimH ? currentYaw + yawStep : currentYaw;
                        newPitch = aimV ? currentPitch + pitchStep : currentPitch;
                     }

                     newPitch = Math.max(-90.0F, Math.min(90.0F, newPitch));
                     if (Float.isFinite(newYaw) && Float.isFinite(newPitch)) {
                        AimAssist.setSilentRotation(newYaw, newPitch);
                        this.rotating = true;
                     }
                  }
               }
            }
         }
      }
   }

   private long randRange(String name, int defLo, int defHi) {
      int lo = this.getSetting(name + "_lo", defLo);
      int hi = this.getSetting(name + "_hi", defHi);
      return hi <= lo ? Math.max(0, lo) : lo + (long)(this.rng.nextFloat() * (hi - lo));
   }

   private double noise(double t, int ch) {
      int b = ch * 3 % (this.phases.length - 2);
      return 0.5 * Math.sin(1.7 * t + this.phases[b]) + 0.3 * Math.sin(3.1 * t + this.phases[b + 1]) + 0.2 * Math.sin(7.3 * t + this.phases[b + 2]);
   }

   private static double clamp(double v, double lo, double hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private void pickGoal(String mode) {
      int rnd = this.getSetting("Randomization", 40);
      int mg = this.getSetting("Hitbox Margin", 15);
      double margin = Math.max(0.03, mg / 100.0);
      double vMargin = Math.max(0.04, margin * 0.4);
      double spread = 0.1 + 0.14 * (rnd / 100.0);
      double u = 0.5;
      double w = 0.5;
      double v;
      if ("Center".equals(mode)) {
         v = 0.5;
      } else {
         u = 0.5 + this.rng.nextGaussian() * spread;
         w = 0.5 + this.rng.nextGaussian() * spread;
         if ("Head".equals(mode)) {
            v = 0.8 + this.rng.nextDouble() * 0.17;
         } else if ("Chest".equals(mode)) {
            v = 0.55 + this.rng.nextDouble() * 0.25;
         } else if ("Legs".equals(mode)) {
            v = 0.08 + this.rng.nextDouble() * 0.37;
         } else if ("Random".equals(mode)) {
            v = 0.08 + this.rng.nextDouble() * 0.87;
         } else {
            int wh = Math.max(0, this.getSetting("Head Weight", 30));
            int wb = Math.max(0, this.getSetting("Body Weight", 50));
            int wl = Math.max(0, this.getSetting("Legs Weight", 20));
            int total = wh + wb + wl;
            int r = total <= 0 ? 0 : this.rng.nextInt(total);
            if (total <= 0) {
               v = 0.55 + this.rng.nextDouble() * 0.25;
            } else if (r < wh) {
               v = 0.8 + this.rng.nextDouble() * 0.17;
            } else if (r < wh + wb) {
               v = 0.5 + this.rng.nextDouble() * 0.3;
            } else {
               v = 0.08 + this.rng.nextDouble() * 0.4;
            }
         }
      }

      this.goal[0] = clamp(u, margin, 1.0 - margin);
      this.goal[1] = clamp(v, vMargin, 1.0 - vMargin);
      this.goal[2] = clamp(w, margin, 1.0 - margin);
   }

   private Vec3 humanPoint(LivingEntity t, Vec3 eyes, String mode, long nowMs, double dt, double tSec) {
      AABB box = t.getBoundingBox();
      if (nowMs >= this.nextPointAtMs) {
         this.pickGoal(mode);
         this.nextPointAtMs = nowMs + this.randRange("Point Change", 500, 1400);
      }

      int drift = this.getSetting("Point Drift", 40);
      double omega = 1.5 + drift / 100.0 * 9.0;
      int steps = Math.max(1, (int)Math.ceil(dt / 0.008));
      double h = dt / steps;

      for (int a = 0; a < 3; a++) {
         for (int i = 0; i < steps; i++) {
            this.pointVel[a] = this.pointVel[a] + (omega * omega * (this.goal[a] - this.point[a]) - 2.0 * omega * this.pointVel[a]) * h;
            this.point[a] = this.point[a] + this.pointVel[a] * h;
         }
      }

      int rnd = this.getSetting("Randomization", 40);
      double sway = 0.035 * (rnd / 100.0);
      double fx = this.point[0] + this.noise(tSec * 0.6, 2) * sway;
      double fy = this.point[1] + this.noise(tSec * 0.6, 3) * sway;
      double fz = this.point[2] + this.noise(tSec * 0.6, 4) * sway;
      fx = clamp(fx, 0.02, 0.98);
      fy = clamp(fy, 0.02, 0.98);
      fz = clamp(fz, 0.02, 0.98);
      if ("Closest".equals(mode)) {
         Vec3 c = this.aimPoint(t, eyes);
         double dx = (fx - 0.5) * (box.maxX - box.minX) * 0.5;
         double dy = (fy - 0.5) * (box.maxY - box.minY) * 0.25;
         double dz = (fz - 0.5) * (box.maxZ - box.minZ) * 0.5;
         return new Vec3(clamp(c.x + dx, box.minX, box.maxX), clamp(c.y + dy, box.minY, box.maxY), clamp(c.z + dz, box.minZ, box.maxZ));
      } else {
         return new Vec3(box.minX + fx * (box.maxX - box.minX), box.minY + fy * (box.maxY - box.minY), box.minZ + fz * (box.maxZ - box.minZ));
      }
   }

   private void returnToCamera(Minecraft mc, long deltaMs) {
      float visualYaw = mc.player.getYRot();
      float visualPitch = mc.player.getXRot();
      float curYaw = AimAssist.getSilentServerYaw();
      float curPitch = AimAssist.getSilentServerPitch();
      float yawDiff = wrapDegrees(visualYaw - curYaw);
      float pitchDiff = wrapDegrees(visualPitch - curPitch);
      if (Math.abs(yawDiff) < 0.5F && Math.abs(pitchDiff) < 0.5F) {
         this.rotating = false;
         this.yawVel = 0.0F;
         this.pitchVel = 0.0F;
         AimAssist.clearSilentRotation();
      } else {
         int hSpeed = this.getSetting("Horizontal Speed", 15);
         int vSpeed = this.getSetting("Vertical Speed", 15);
         float yawStep = clampAbs(yawDiff * (hSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(yawDiff));
         float pitchStep = clampAbs(pitchDiff * (vSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(pitchDiff));
         AimAssist.setSilentRotation(curYaw + yawStep, curPitch + pitchStep);
         AimAssist.clearSilentRotation();
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         this.resetWait();
      } else if (!this.canRun(mc)) {
         this.resetWait();
      } else if (CrystalAura.yieldsKillAura()) {
         this.resetWait();
      } else {
         LivingEntity t = this.target;
         if (t == null || !t.isAlive() || t.isRemoved()) {
            this.resetWait();
         } else if (this.getSetting("Check Shielding", true) && t.isBlocking()) {
            this.resetWait();
         } else {
            float coolLo = this.getSetting("Cooldown Threshold_lo", 0.9F);
            float coolHi = this.getSetting("Cooldown Threshold_hi", 1.0F);
            float needed = coolLo + this.rng.nextFloat() * Math.max(0.0F, coolHi - coolLo);
            if (mc.player.getAttackStrengthScale(0.0F) < needed) {
               this.resetWait();
            } else if (this.getSetting("Prioritize Criticals", true) && !this.canCrit(mc)) {
               this.resetWait();
            } else if (!this.canHit(mc, t)) {
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
                  this.attack(mc, t);
                  this.resetWait();
               }
            }
         }
      }
   }

   private boolean canHit(Minecraft mc, LivingEntity t) {
      float attackRangeF = this.getSetting("Attack Range", 3.0F);
      double attackRange = attackRangeF;
      Vec3 eyes = mc.player.getEyePosition();
      AABB box = t.getBoundingBox();
      Vec3 hitPoint;
      if (this.getSetting("Only When Aimed", true)) {
         if (!this.rotating) {
            return false;
         }

         Vec3 dir = AimAssist.getRotationVector(AimAssist.getSilentServerPitch(), AimAssist.getSilentServerYaw());
         Vec3 end = eyes.add(dir.scale(attackRange + 1.0));
         Optional<Vec3> clip = box.clip(eyes, end);
         if (clip.isEmpty()) {
            return false;
         }

         hitPoint = clip.get();
      } else {
         hitPoint = closestPoint(box, eyes);
      }

      if (eyes.distanceToSqr(hitPoint) > attackRange * attackRange) {
         return false;
      }

      if (this.getSetting("Attack Through Walls", false)) {
         return true;
      }

      BlockHitResult block = mc.level.clip(new ClipContext(eyes, hitPoint, Block.OUTLINE, Fluid.NONE, mc.player));
      return block.getType() == Type.MISS;
   }

   private void attack(Minecraft mc, LivingEntity t) {
      PerAttackEvents.fire(new EventStartAttack(mc.player, t));
      mc.gameMode.attack(mc.player, t);
      mc.player.swing(InteractionHand.MAIN_HAND);
      CrystalAura.onKillAuraAttack();
   }

   private boolean canRun(Minecraft mc) {
      if (mc.player == null || mc.level == null) {
         return false;
      } else if (this.getSetting("Not in GUI", true) && mc.screen != null) {
         return false;
      } else {
         return this.getSetting("Not While Using", true) && mc.player.isUsingItem() ? false : !this.getSetting("Only Weapon", false) || isWeapon(mc.player.getMainHandItem());
      }
   }

   private LivingEntity findTarget(Minecraft mc) {
      int range = this.getSetting("Range", 6);
      double rangeSq = (double)range * range;
      int fov = this.getSetting("FOV", 360);
      float visualYaw = mc.player.getYRot();
      float visualPitch = mc.player.getXRot();
      Vec3 eyes = mc.player.getEyePosition();
      boolean throughWalls = this.getSetting("Attack Through Walls", false);
      List<LivingEntity> candidates = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(
            e -> e instanceof Player
               && !AimAssist.isSelfLike(e)
               && e.isAlive()
               && !e.isSpectator()
               && e.distanceToSqr(mc.player) <= rangeSq
               && !AntiBot.isBot((LivingEntity)e)
               && (fov >= 360 || this.withinFov((LivingEntity)e, eyes, visualYaw, visualPitch, fov))
               && (throughWalls || isVisible(mc, eyes, (LivingEntity)e))
         )
         .map(e -> (LivingEntity)e)
         .collect(Collectors.toList());
      return candidates.isEmpty() ? null : Targets.getTarget(candidates, mc.player);
   }

   private static boolean isVisible(Minecraft mc, Vec3 eyes, LivingEntity e) {
      AABB b = e.getBoundingBox();
      double cx = (b.minX + b.maxX) * 0.5;
      double cz = (b.minZ + b.maxZ) * 0.5;
      double[] ys = new double[]{b.maxY - 0.15, (b.minY + b.maxY) * 0.5, b.minY + 0.25};

      for (double y : ys) {
         BlockHitResult hit = mc.level.clip(new ClipContext(eyes, new Vec3(cx, y, cz), Block.OUTLINE, Fluid.NONE, mc.player));
         if (hit.getType() == Type.MISS) {
            return true;
         }
      }

      return false;
   }

   private boolean withinFov(LivingEntity e, Vec3 eyes, float visualYaw, float visualPitch, int fov) {
      Vec3 aimPos = this.aimPoint(e, eyes);
      Vec3 d = aimPos.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      float yaw = (float)(Math.atan2(d.x * -1.0, d.z) * 180.0 / Math.PI);
      float pitch = (float)(Math.atan2(d.y, h) * -1.0 * 180.0 / Math.PI);
      float dy = wrapDegrees(yaw - visualYaw);
      float dp = wrapDegrees(pitch - visualPitch);
      return Math.sqrt(dy * dy + dp * dp) <= fov / 2.0F;
   }

   private Vec3 aimPoint(LivingEntity t, Vec3 eyes) {
      AABB box = t.getBoundingBox();
      if ("Center".equals(this.getSetting("Aim At", "Closest"))) {
         return new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
      }

      if (eyes.x > box.minX - 0.25 && eyes.x < box.maxX + 0.25 && eyes.z > box.minZ - 0.25 && eyes.z < box.maxZ + 0.25) {
         return new Vec3((box.minX + box.maxX) / 2.0, box.minY + (box.maxY - box.minY) * 0.6, (box.minZ + box.maxZ) / 2.0);
      }

      double mx = Math.min(0.08, (box.maxX - box.minX) * 0.25);
      double my = Math.min(0.08, (box.maxY - box.minY) * 0.25);
      double mz = Math.min(0.08, (box.maxZ - box.minZ) * 0.25);
      return new Vec3(Math.max(box.minX + mx, Math.min(eyes.x, box.maxX - mx)), Math.max(box.minY + my, Math.min(eyes.y, box.maxY - my)), Math.max(box.minZ + mz, Math.min(eyes.z, box.maxZ - mz)));
   }

   private static Vec3 closestPoint(AABB box, Vec3 p) {
      return new Vec3(Math.max(box.minX, Math.min(p.x, box.maxX)), Math.max(box.minY, Math.min(p.y, box.maxY)), Math.max(box.minZ, Math.min(p.z, box.maxZ)));
   }

   private boolean canCrit(Minecraft mc) {
      if (mc.player.onGround()) {
         return true;
      } else if (mc.player.isInWater() || mc.player.isInLava()) {
         return false;
      } else {
         return mc.player.isPassenger() ? false : mc.player.getDeltaMovement().y <= -0.1;
      }
   }

   private static boolean isWeapon(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() instanceof TridentItem || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.MACE_ENCHANTABLE);
   }

   private void resetWait() {
      this.waiting = false;
      this.fireAtMs = -1L;
   }

   private static float wrapDegrees(float angle) {
      angle %= 360.0F;
      if (angle >= 180.0F) {
         angle -= 360.0F;
      }

      if (angle < -180.0F) {
         angle += 360.0F;
      }

      return angle;
   }

   private static float clampAbs(float value, float maxAbs) {
      if (value > maxAbs) {
         return maxAbs;
      }

      float negMax = maxAbs * -1.0F;
      return value < negMax ? negMax : value;
   }
}
