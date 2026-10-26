package need.clarity.modules.visual;

import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Freecam extends ModuleBase {
   private static Freecam instance;
   public static volatile Vec3 cameraPos = Vec3.ZERO;
   public static volatile float cameraYaw = 0.0F;
   public static volatile float cameraPitch = 0.0F;
   private static volatile Vec3 frozenPlayerPos = Vec3.ZERO;
   private static volatile float frozenYaw = 0.0F;
   private static volatile float frozenPitch = 0.0F;
   private static volatile boolean active = false;
   private long lastFrameNanos = -1L;
   private ClientLevel activeLevel;
   private LocalPlayer activePlayer;
   private final Runnable frameCallback = this::onRenderFrame;

   public Freecam() {
      super("Freecam", "Visual");
      instance = this;
   }

   public static boolean isActive() {
      return active;
   }

   public static Vec3 getCameraPos() {
      return cameraPos;
   }

   public static Vec3 getFrozenPlayerPos() {
      return frozenPlayerPos;
   }

   public static float getFrozenYaw() {
      return frozenYaw;
   }

   public static float getFrozenPitch() {
      return frozenPitch;
   }

   private static Freecam self() {
      return instance;
   }

   public static boolean shouldFreezePlayer() {
      Freecam f = self();
      return f == null || f.getSetting("Freeze Player", true);
   }

   public static boolean noClip() {
      Freecam f = self();
      return f == null || f.getSetting("No Clip", true);
   }

   public static float speed() {
      Freecam f = self();
      return f == null ? 5.0F : f.getSetting("Speed", 5.0F);
   }

   public static float verticalSpeed() {
      Freecam f = self();
      return f == null ? 5.0F : f.getSetting("Vertical Speed", 5.0F);
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         this.activate(mc);
      }
   }

   @Override
   public void onDisable() {
      active = false;
      this.lastFrameNanos = -1L;
      this.activeLevel = null;
      this.activePlayer = null;
      PerFrameEvents.unregister(this.frameCallback);
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.setYRot(frozenYaw);
         mc.player.setXRot(frozenPitch);
         mc.player.yRotO = frozenYaw;
         mc.player.xRotO = frozenPitch;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (!active) {
            this.activate(mc);
         } else if (mc.player != this.activePlayer || mc.level != this.activeLevel) {
            this.setEnabled(false);
         } else if (mc.player.isRemoved() || mc.player.isDeadOrDying()) {
            this.setEnabled(false);
         } else if (shouldFreezePlayer()) {
            mc.player.setYRot(frozenYaw);
            mc.player.setXRot(frozenPitch);
            mc.player.yRotO = frozenYaw;
            mc.player.xRotO = frozenPitch;
         }
      } else {
         if (active) {
            this.setEnabled(false);
         }
      }
   }

   private void activate(Minecraft mc) {
      if (!active) {
         cameraPos = mc.player.getEyePosition(1.0F);
         frozenPlayerPos = mc.player.position();
         frozenYaw = mc.player.getYRot();
         frozenPitch = mc.player.getXRot();
         cameraYaw = frozenYaw;
         cameraPitch = frozenPitch;
         active = true;
         this.lastFrameNanos = System.nanoTime();
         this.activeLevel = mc.level;
         this.activePlayer = mc.player;
         PerFrameEvents.register(this.frameCallback);
      }
   }

   private void onRenderFrame() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            long now = System.nanoTime();
            double dtSec = (now - this.lastFrameNanos) / 1.0E9;
            this.lastFrameNanos = now;
            if (!(dtSec <= 0.0)) {
               if (dtSec > 0.1) {
                  dtSec = 0.1;
               }

               if (mc.screen == null && !mc.isPaused()) {
                  boolean forward = mc.options.keyUp.isDown();
                  boolean back = mc.options.keyDown.isDown();
                  boolean left = mc.options.keyLeft.isDown();
                  boolean right = mc.options.keyRight.isDown();
                  boolean up = mc.options.keyJump.isDown();
                  boolean downK = mc.options.keyShift.isDown();
                  boolean sprint = mc.options.keySprint.isDown();
                  if (forward || back || left || right || up || downK) {
                     double speed = speed() * 2.0 * dtSec;
                     double vSpeed = verticalSpeed() * 2.0 * dtSec;
                     if (sprint) {
                        speed *= 2.0;
                     }

                     double yawRad = Math.toRadians(cameraYaw);
                     double sinYaw = Math.sin(yawRad);
                     double cosYaw = Math.cos(yawRad);
                     double fwdX = -sinYaw;
                     double fwdZ = cosYaw;
                     double rightX = -cosYaw;
                     double rightZ = -sinYaw;
                     double dx = 0.0;
                     double dy = 0.0;
                     double dz = 0.0;
                     if (forward) {
                        dx += fwdX;
                        dz += fwdZ;
                     }

                     if (back) {
                        dx -= fwdX;
                        dz -= fwdZ;
                     }

                     if (left) {
                        dx -= rightX;
                        dz -= rightZ;
                     }

                     if (right) {
                        dx += rightX;
                        dz += rightZ;
                     }

                     double hLen = Math.sqrt(dx * dx + dz * dz);
                     if (hLen > 1.0) {
                        dx /= hLen;
                        dz /= hLen;
                     }

                     dx *= speed;
                     dz *= speed;
                     if (up) {
                        dy += vSpeed;
                     }

                     if (downK) {
                        dy -= vSpeed;
                     }

                     Vec3 delta = new Vec3(dx, dy, dz);
                     cameraPos = noClip() ? cameraPos.add(delta) : collide(mc, cameraPos, delta);
                  }
               }
            }
         }
      }
   }

   private static Vec3 collide(Minecraft mc, Vec3 from, Vec3 delta) {
      Vec3 result = from;
      result = clipAxis(mc, result, new Vec3(delta.x, 0.0, 0.0));
      result = clipAxis(mc, result, new Vec3(0.0, delta.y, 0.0));
      return clipAxis(mc, result, new Vec3(0.0, 0.0, delta.z));
   }

   private static Vec3 clipAxis(Minecraft mc, Vec3 from, Vec3 delta) {
      if (delta.lengthSqr() < 1.0E-8) {
         return from;
      }

      Vec3 to = from.add(delta);
      BlockHitResult hit = mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player));
      if (hit.getType() == Type.MISS) {
         return to;
      }

      Vec3 dir = delta.normalize();
      return hit.getLocation().subtract(dir.scale(0.01));
   }
}
