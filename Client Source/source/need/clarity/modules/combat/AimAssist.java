package need.clarity.modules.combat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.util.RotDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends ModuleBase {
   private long lastFrameTime = -1L;
   private static float silentServerYaw = 0.0F;
   private static float silentServerPitch = 0.0F;
   private static boolean hasSilentRotation = false;
   private static boolean hadSilentRotationLastFrame = false;
   private static boolean returningToVisual = false;
   private static boolean ownsSilent = false;
   private static boolean isActivelyTracking = false;
   private static volatile LivingEntity currentTarget = null;
   public static float lastInjectedYaw = 0.0F;
   public static float lastInjectedPitch = 0.0F;
   private static boolean pendingUseItem = false;
   private static float pendingUseItemYaw = 0.0F;
   private static float pendingUseItemPitch = 0.0F;
   private static volatile AimAssist INSTANCE;

   public static LivingEntity getCurrentTarget() {
      LivingEntity t = currentTarget;
      if (t == null || t.isAlive() && !t.isRemoved()) {
         return t;
      }

      currentTarget = null;
      return null;
   }

   public static void releaseSharedTarget() {
      AimAssist i = INSTANCE;
      if (i == null || !i.isEnabled()) {
         currentTarget = null;
      }
   }

   public static void setCurrentTarget(LivingEntity target) {
      currentTarget = target != null && target.isAlive() ? target : null;
   }

   public static void clearCurrentTarget() {
      currentTarget = null;
   }

   public static void commitUseItemRotation(float yaw, float pitch) {
      pendingUseItem = true;
      pendingUseItemYaw = yaw;
      pendingUseItemPitch = pitch;
   }

   public static boolean hasPendingUseItem() {
      return pendingUseItem;
   }

   public static float getPendingUseItemYaw() {
      return pendingUseItemYaw;
   }

   public static float getPendingUseItemPitch() {
      return pendingUseItemPitch;
   }

   public static void clearPendingUseItem() {
      pendingUseItem = false;
   }

   public static boolean isSelfLike(Entity e) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer me = mc.player;
      if (me == null || e == null) {
         return false;
      } else if (e == me) {
         return true;
      } else {
         return e.getUUID().equals(me.getUUID()) ? true : me.getVehicle() == e || e.getVehicle() == me;
      }
   }

   public static boolean hasSilentRotation() {
      return hasSilentRotation;
   }

   public static boolean isActivelyTracking() {
      return isActivelyTracking;
   }

   public static float getSilentServerYaw() {
      return silentServerYaw;
   }

   public static float getSilentServerPitch() {
      return silentServerPitch;
   }

   public static Vec3 getRotationVector(float pitch, float yaw) {
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      float yawRad = yaw * (float) (-Math.PI / 180.0);
      float cosPitch = Mth.cos(pitchRad);
      float sinPitch = Mth.sin(pitchRad);
      float cosYaw = Mth.cos(yawRad);
      float sinYaw = Mth.sin(yawRad);
      return new Vec3(sinYaw * cosPitch, sinPitch * -1.0F, cosYaw * cosPitch);
   }

   public AimAssist() {
      super("Aim Assist", "Combat");
      INSTANCE = this;
      PerFrameEvents.register(this::onRenderFrame);
   }

   public static boolean isActive() {
      AimAssist i = INSTANCE;
      return i != null && i.isEnabled() || KillAura.isActive();
   }

   @Override
   public void onEnable() {
      this.lastFrameTime = -1L;
      returningToVisual = false;
   }

   @Override
   public void onDisable() {
      currentTarget = null;
      if (!hasSilentRotation && !hadSilentRotationLastFrame) {
         hasSilentRotation = false;
         hadSilentRotationLastFrame = false;
         returningToVisual = false;
         isActivelyTracking = false;
      } else {
         isActivelyTracking = false;
      }
   }

   private void onRenderFrame() {
      hadSilentRotationLastFrame = hasSilentRotation;
      hasSilentRotation = false;
      isActivelyTracking = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         long deltaMs = this.lastFrameTime < 0L ? 1L : Math.min(now - this.lastFrameTime, 100L);
         this.lastFrameTime = now;
         if (KillAura.isActive()) {
            returningToVisual = false;
            ownsSilent = false;
         } else {
            boolean silentRot = this.getSetting("Silent Rotations", false);
            boolean shouldAim = this.isEnabled()
               && (!this.getSetting("Not in GUI", true) || mc.screen == null)
               && (!this.getSetting("Not While Using", false) || !mc.player.isUsingItem())
               && (!this.getSetting("Only Weapon", false) || isWeapon(mc.player));
            if (!shouldAim) {
               currentTarget = null;
               if (silentRot && hadSilentRotationLastFrame && ownsSilent) {
                  this.runReturnPhase(mc, deltaMs);
               } else {
                  returningToVisual = false;
               }
            } else {
               int range = this.getSetting("Range", 5);
               double rangeSq = (double)range * range;
               List<LivingEntity> candidates = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
                  .filter(e -> e instanceof Player && !isSelfLike(e) && e.distanceToSqr(mc.player) <= rangeSq && !AntiBot.isBot((LivingEntity)e))
                  .map(e -> (LivingEntity)e)
                  .collect(Collectors.toList());
               LivingEntity target = candidates.isEmpty() ? null : Targets.getTarget(candidates, mc.player);
               if (target == null) {
                  currentTarget = null;
                  if (silentRot && hadSilentRotationLastFrame && ownsSilent) {
                     this.runReturnPhase(mc, deltaMs);
                  } else {
                     returningToVisual = false;
                  }
               } else {
                  currentTarget = target;
                  returningToVisual = false;
                  Vec3 eyes = mc.player.getEyePosition();
                  String aimAt = this.getSetting("Aim At", "Closest");
                  Vec3 aimPos;
                  if ("Center".equals(aimAt)) {
                     AABB box = target.getBoundingBox();
                     aimPos = new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
                  } else {
                     AABB box = target.getBoundingBox();
                     if (eyes.x > box.minX - 0.25 && eyes.x < box.maxX + 0.25 && eyes.z > box.minZ - 0.25 && eyes.z < box.maxZ + 0.25) {
                        aimPos = new Vec3((box.minX + box.maxX) / 2.0, box.minY + (box.maxY - box.minY) * 0.6, (box.minZ + box.maxZ) / 2.0);
                     } else {
                        aimPos = new Vec3(Math.max(box.minX, Math.min(eyes.x, box.maxX)), Math.max(box.minY, Math.min(eyes.y, box.maxY)), Math.max(box.minZ, Math.min(eyes.z, box.maxZ)));
                     }
                  }

                  Vec3 delta = aimPos.subtract(eyes);
                  if (!(delta.lengthSqr() < 1.0E-6)) {
                     double hDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                     if (hDist < 0.05) {
                        if (silentRot && hadSilentRotationLastFrame && ownsSilent) {
                           this.runReturnPhase(mc, deltaMs);
                        }
                     } else {
                        float targetYaw = (float)(Math.atan2(delta.x * -1.0, delta.z) * 180.0 / Math.PI);
                        float targetPitch = (float)(Math.atan2(delta.y, hDist) * -1.0 * 180.0 / Math.PI);
                        if (silentRot && !hadSilentRotationLastFrame) {
                           silentServerYaw = mc.player.getYRot();
                           silentServerPitch = mc.player.getXRot();
                        }

                        float currentYaw = silentRot && hadSilentRotationLastFrame ? silentServerYaw : mc.player.getYRot();
                        float currentPitch = silentRot && hadSilentRotationLastFrame ? silentServerPitch : mc.player.getXRot();
                        float yawDiff = wrapDegrees(targetYaw - currentYaw);
                        float pitchDiff = wrapDegrees(targetPitch - currentPitch);
                        int fov = this.getSetting("FOV", 90);
                        float visualYawForFov = mc.player.getYRot();
                        float visualPitchForFov = mc.player.getXRot();
                        float fovYawDiff = wrapDegrees(targetYaw - visualYawForFov);
                        float fovPitchDiff = wrapDegrees(targetPitch - visualPitchForFov);
                        float angularDist = (float)Math.sqrt(fovYawDiff * fovYawDiff + fovPitchDiff * fovPitchDiff);
                        if (angularDist > fov / 2.0F) {
                           if (silentRot && hadSilentRotationLastFrame && ownsSilent) {
                              this.runReturnPhase(mc, deltaMs);
                           }
                        } else {
                           int hSpeed = this.getSetting("Horizontal Speed", 10);
                           int vSpeed = this.getSetting("Vertical Speed", 10);
                           boolean aimH = this.getSetting("Aim Horizontally", true);
                           boolean aimV = this.getSetting("Aim Vertically", true);
                           float yawStep = yawDiff * (hSpeed / 20.0F) * ((float)deltaMs / 50.0F);
                           float pitchStep = pitchDiff * (vSpeed / 20.0F) * ((float)deltaMs / 50.0F);
                           yawStep = clampAbs(yawStep, Math.abs(yawDiff));
                           pitchStep = clampAbs(pitchStep, Math.abs(pitchDiff));
                           if (silentRot) {
                              float newYaw = aimH ? currentYaw + yawStep : currentYaw;
                              float newPitch = aimV ? currentPitch + pitchStep : currentPitch;
                              if (Math.abs(newPitch) > 80.0F && RotDebug.enabled()) {
                                 RotDebug.report("Aim Assist silent pitch=" + newPitch + " target=" + target.getName().getString() + " targetPitch=" + targetPitch + " hDist=" + hDist);
                              }

                              silentServerYaw = silentServerYaw + wrapDegrees(newYaw - silentServerYaw);
                              silentServerPitch = Math.max(-90.0F, Math.min(90.0F, newPitch));
                              hasSilentRotation = true;
                              isActivelyTracking = true;
                              ownsSilent = true;
                           } else {
                              if (aimH) {
                                 mc.player.setYRot(currentYaw + yawStep);
                              }

                              if (aimV) {
                                 mc.player.setXRot(Math.max(-90.0F, Math.min(90.0F, currentPitch + pitchStep)));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         returningToVisual = false;
         hadSilentRotationLastFrame = false;
      }
   }

   private void runReturnPhase(Minecraft mc, long deltaMs) {
      returningToVisual = true;
      float visualYaw = mc.player.getYRot();
      float visualPitch = mc.player.getXRot();
      float yawDiff = wrapDegrees(visualYaw - silentServerYaw);
      float pitchDiff = wrapDegrees(visualPitch - silentServerPitch);
      if (Math.abs(yawDiff) < 0.5F && Math.abs(pitchDiff) < 0.5F) {
         hasSilentRotation = false;
         hadSilentRotationLastFrame = false;
         returningToVisual = false;
         isActivelyTracking = false;
         ownsSilent = false;
         silentServerYaw = visualYaw;
         silentServerPitch = visualPitch;
         lastInjectedYaw = visualYaw;
         lastInjectedPitch = visualPitch;
      } else {
         int hSpeed = this.getSetting("Horizontal Speed", 10);
         int vSpeed = this.getSetting("Vertical Speed", 10);
         float yawStep = clampAbs(yawDiff * (hSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(yawDiff));
         float pitchStep = clampAbs(pitchDiff * (vSpeed / 20.0F) * ((float)deltaMs / 50.0F), Math.abs(pitchDiff));
         silentServerYaw += yawStep;
         silentServerPitch = Math.max(-90.0F, Math.min(90.0F, silentServerPitch + pitchStep));
         hasSilentRotation = true;
      }
   }

   public static void setSilentRotation(float yaw, float pitch) {
      if (Math.abs(pitch) > 80.0F && RotDebug.enabled()) {
         RotDebug.report("setSilentRotation pitch=" + pitch + " by " + RotDebug.callers(2, 3));
      }

      if (!hasSilentRotation && !hadSilentRotationLastFrame) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            float ref = mc.player.getYRot();
            silentServerYaw = ref + wrapDegrees(yaw - ref);
         } else {
            silentServerYaw = yaw;
         }
      } else {
         silentServerYaw = silentServerYaw + wrapDegrees(yaw - silentServerYaw);
      }

      silentServerPitch = Math.max(-90.0F, Math.min(90.0F, pitch));
      hasSilentRotation = true;
      isActivelyTracking = true;
      ownsSilent = false;
   }

   public static void commitSnappedRotation(float yaw, float pitch) {
      silentServerYaw = yaw;
      silentServerPitch = Math.max(-90.0F, Math.min(90.0F, pitch));
   }

   public static void clearSilentRotation() {
      isActivelyTracking = false;
   }

   private static boolean isWeapon(LocalPlayer player) {
      ItemStack stack = player.getMainHandItem();
      Item item = stack.getItem();
      return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.MACE_ENCHANTABLE) || item instanceof TridentItem;
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
      float negMax = maxAbs * -1.0F;
      if (value > maxAbs) {
         return maxAbs;
      } else {
         return value < negMax ? negMax : value;
      }
   }
}
