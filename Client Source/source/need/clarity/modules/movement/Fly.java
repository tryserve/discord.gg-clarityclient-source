package need.clarity.modules.movement;

import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Fly extends ModuleBase {
   private static Fly instance;
   public static volatile double pendingVelX = 0.0;
   public static volatile double pendingVelY = 0.0;
   public static volatile double pendingVelZ = 0.0;
   public static volatile boolean hasPending = false;

   public Fly() {
      super("Fly", "Movement");
      instance = this;
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled();
   }

   public static String getMode() {
      return instance == null ? "Creative" : instance.getSetting("Mode", "Creative");
   }

   public static float getSpeed() {
      return instance == null ? 5.0F : instance.getSetting("Speed", 5.0F);
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
         hasPending = false;
      }
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         hasPending = false;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         LocalPlayer player = mc.player;
         String mode = getMode();
         float speed = getSpeed();
         MoveFix.EventMovementInput event = PerMovementEvents.lastEvent;
         boolean forward = event != null ? event.forward : mc.options.keyUp.isDown();
         boolean back = event != null ? event.back : mc.options.keyDown.isDown();
         boolean left = event != null ? event.left : mc.options.keyLeft.isDown();
         boolean right = event != null ? event.right : mc.options.keyRight.isDown();
         boolean jump = event != null ? event.jump : mc.options.keyJump.isDown();
         boolean sneak = event != null ? event.sneak : mc.options.keyShift.isDown();
         if ("Full".equals(mode)) {
            computeFull(player, speed, forward, back, left, right, jump, sneak);
         } else {
            computeCreative(player, speed, forward, back, left, right, jump, sneak);
         }

         hasPending = true;
      }
   }

   public static boolean isVulcanGrounded() {
      return false;
   }

   public static boolean isVulcanAirWalk(int blockY) {
      return false;
   }

   public static double vulcanFloorFraction() {
      return 0.0;
   }

   private static Vec3 movementDirection(LocalPlayer player, boolean fwd, boolean back, boolean left, boolean right) {
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

   private static void computeFull(LocalPlayer player, float speed, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak) {
      double s = 0.05 * speed;
      float fwd = (forward ? 1.0F : 0.0F) - (back ? 1.0F : 0.0F);
      float str = (left ? 1.0F : 0.0F) - (right ? 1.0F : 0.0F);
      double moveX = 0.0;
      double moveZ = 0.0;
      if (fwd != 0.0F || str != 0.0F) {
         double len = Math.sqrt(fwd * fwd + str * str);
         fwd = (float)(fwd / len);
         str = (float)(str / len);
         double yawRad = Math.toRadians(player.getYRot());
         double sin = Math.sin(yawRad);
         double cos = Math.cos(yawRad);
         moveX = (fwd * -sin + str * cos) * s;
         moveZ = (fwd * cos + str * sin) * s;
      }

      double moveY = 0.0;
      if (jump) {
         moveY = s;
      }

      if (sneak) {
         moveY = -s;
      }

      pendingVelX = moveX;
      pendingVelY = moveY;
      pendingVelZ = moveZ;
   }

   private static void computeCreative(LocalPlayer player, float speed, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak) {
      double s = 0.05 * speed;
      float fwd = (forward ? 1.0F : 0.0F) - (back ? 1.0F : 0.0F);
      float str = (left ? 1.0F : 0.0F) - (right ? 1.0F : 0.0F);
      double velX = 0.0;
      double velZ = 0.0;
      if (fwd != 0.0F || str != 0.0F) {
         double len = Math.sqrt(fwd * fwd + str * str);
         fwd = (float)(fwd / len);
         str = (float)(str / len);
         double yawRad = Math.toRadians(player.getYRot());
         double sin = Math.sin(yawRad);
         double cos = Math.cos(yawRad);
         velX = (fwd * -sin + str * cos) * s;
         velZ = (fwd * cos + str * sin) * s;
      }

      double velY = 0.0;
      if (jump) {
         velY = s;
      }

      if (sneak) {
         velY = -s;
      }

      pendingVelX = velX;
      pendingVelY = velY;
      pendingVelZ = velZ;
   }

   public static boolean isJumpDown(Minecraft mc) {
      long win = mc.getWindow().handle();
      return GLFW.glfwGetKey(win, 32) == 1;
   }

   public static boolean isDescendDown(Minecraft mc) {
      MoveFix.EventMovementInput event = PerMovementEvents.lastEvent;
      return event != null ? event.sneak : mc.options.keyShift.isDown();
   }
}
