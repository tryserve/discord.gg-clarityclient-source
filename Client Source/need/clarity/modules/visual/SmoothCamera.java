package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SmoothCamera extends ModuleBase {
   private static SmoothCamera INSTANCE;
   private static boolean rotInit = false;
   private static boolean posInit = false;
   private static float yaw;
   private static float pitch;
   private static double px;
   private static double py;
   private static double pz;
   private static long lastRot = 0L;
   private static long lastPos = 0L;

   public SmoothCamera() {
      super("Smooth Camera", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      rotInit = false;
      posInit = false;
   }

   private static boolean on() {
      SmoothCamera m = INSTANCE;
      return m != null && m.isEnabled() && !Freecam.isActive();
   }

   public static float[] smooth(float targetYaw, float targetPitch) {
      if (!on()) {
         rotInit = false;
         return null;
      } else {
         long now = System.nanoTime();
         double dt = (now - lastRot) / 1.0E9;
         lastRot = now;
         if (rotInit && !(dt > 0.25) && !(dt <= 0.0)) {
            float speed = clamp(INSTANCE.getSetting("Follow Speed", 5.0F), 0.5F, 30.0F);
            float a = (float)(1.0 - Math.exp(-speed * dt));
            yaw = wrap(yaw + wrap(targetYaw - yaw) * a);
            pitch = INSTANCE.getSetting("Smooth Pitch", true) ? pitch + (targetPitch - pitch) * a : targetPitch;
            return new float[]{yaw, pitch};
         } else {
            yaw = targetYaw;
            pitch = targetPitch;
            rotInit = true;
            return null;
         }
      }
   }

   public static Vec3 smoothPosition(Vec3 target, Entity entity, float partialTick) {
      if (on() && target != null && entity != null) {
         if (!INSTANCE.getSetting("Lag Behind", true)) {
            posInit = false;
            return null;
         }

         long now = System.nanoTime();
         double dt = (now - lastPos) / 1.0E9;
         lastPos = now;
         double dx = target.x - px;
         double dy = target.y - py;
         double dz = target.z - pz;
         if (posInit && !(dt > 0.25) && !(dt <= 0.0) && !(dx * dx + dy * dy + dz * dz > 400.0)) {
            float speed = clamp(INSTANCE.getSetting("Position Speed", 4.0F), 0.5F, 30.0F);
            double a = 1.0 - Math.exp(-speed * dt);
            px += dx * a;
            py += dy * a;
            pz += dz * a;
            Vec3 pos = new Vec3(px, py, pz);
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
               Vec3 eye = entity.getEyePosition(partialTick);
               BlockHitResult hit = mc.level.clip(new ClipContext(eye, pos, Block.VISUAL, Fluid.NONE, entity));
               if (hit != null && hit.getType() != Type.MISS) {
                  Vec3 h = hit.getLocation();
                  Vec3 back = eye.subtract(h);
                  double len = back.length();
                  pos = len > 1.0E-4 ? h.add(back.scale(Math.min(0.2, len) / len)) : h;
               }
            }

            return pos;
         } else {
            px = target.x;
            py = target.y;
            pz = target.z;
            posInit = true;
            return null;
         }
      } else {
         posInit = false;
         rotInit = target == null ? false : rotInit;
         return null;
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
   }

   private static float wrap(float deg) {
      deg %= 360.0F;
      if (deg >= 180.0F) {
         deg -= 360.0F;
      }

      if (deg < -180.0F) {
         deg += 360.0F;
      }

      return deg;
   }
}
