package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class Freelook extends ModuleBase {
   public static Freelook INSTANCE;
   public static float yaw;
   public static float pitch;
   private static CameraType previous;
   private static boolean on;

   public Freelook() {
      super("Freelook", "Visual");
      INSTANCE = this;
   }

   public static boolean isActive() {
      return on && INSTANCE != null && INSTANCE.isEnabled();
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         on = false;
      } else {
         yaw = mc.player.getYRot();
         pitch = mc.player.getXRot();
         previous = mc.options.getCameraType();
         String p = this.getSetting("Perspective", "Third Person");

         CameraType want = switch (p) {
            case "First Person" -> CameraType.FIRST_PERSON;
            case "Front" -> CameraType.THIRD_PERSON_FRONT;
            default -> CameraType.THIRD_PERSON_BACK;
         };
         mc.options.setCameraType(want);
         on = true;
      }
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (on && previous != null && mc.options != null) {
         mc.options.setCameraType(previous);
      }

      on = false;
      previous = null;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled() && mc.player == null) {
         this.setEnabled(false);
      }
   }

   public static void turn(double dYaw, double dPitch) {
      float sens = INSTANCE.getSetting("Sensitivity", 1.0F);
      yaw = Mth.wrapDegrees(yaw + (float)(dYaw * 0.15) * sens);
      float lim = INSTANCE.getSetting("Pitch Limit", 90.0F);
      pitch = Mth.clamp(pitch + (float)(dPitch * 0.15) * sens * (INSTANCE.getSetting("Invert Pitch", false) ? -1.0F : 1.0F), -lim, lim);
   }
}
