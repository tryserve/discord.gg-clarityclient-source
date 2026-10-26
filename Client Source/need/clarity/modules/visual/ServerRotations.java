package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.util.Mth;

public class ServerRotations extends ModuleBase {
   private static ServerRotations instance;
   private static float lastServerPitch;
   private static float currentServerPitch;

   public ServerRotations() {
      super("Server Rotations", "Visual");
      instance = this;
   }

   public static boolean shouldApply() {
      return instance != null && instance.isEnabled();
   }

   public static void captureLastPitch(float previousServerPitch) {
      lastServerPitch = previousServerPitch;
   }

   public static void captureCurrentPitch(float thisTickServerPitch) {
      currentServerPitch = thisTickServerPitch;
   }

   public static float serverPitchLerp(float tickDelta) {
      return Mth.lerp(Mth.clamp(tickDelta, 0.0F, 1.0F), lastServerPitch, currentServerPitch);
   }
}
