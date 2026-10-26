package need.clarity.modules.movement;

import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpinBot extends ModuleBase {
   public static SpinBot INSTANCE;
   private boolean started;
   private int startTick;
   private float baseYaw;

   public SpinBot() {
      super("Spin Bot", "Movement");
      INSTANCE = this;
      PerFrameEvents.register(this::onFrame);
   }

   @Override
   public void onEnable() {
      this.started = false;
   }

   @Override
   public void onDisable() {
      this.started = false;
   }

   public static boolean isClientMode() {
      SpinBot s = INSTANCE;
      return s != null && s.isEnabled() && "Client".equals(s.getSetting("Mode", "Server"));
   }

   public static float yaw(float partialTick) {
      SpinBot s = INSTANCE;
      Minecraft mc = Minecraft.getInstance();
      return s != null && mc.player != null ? s.compute(mc.player, partialTick) : 0.0F;
   }

   private float compute(LocalPlayer p, float partialTick) {
      if (!this.started) {
         this.started = true;
         this.startTick = p.tickCount;
         this.baseYaw = p.getYRot();
      }

      float speed = Math.max(1.0F, Math.min(180.0F, this.getSetting("Speed", 30.0F)));
      float dir = "Left".equals(this.getSetting("Direction", "Right")) ? -1.0F : 1.0F;
      double ticks = p.tickCount - this.startTick + partialTick;
      double turned = ticks * speed;
      if (turned > 360000.0) {
         int whole = (int)(360000.0 / speed);
         this.startTick += whole;
         ticks = p.tickCount - this.startTick + partialTick;
         turned = ticks * speed;
      }

      return this.baseYaw + dir * (float)turned;
   }

   private void onFrame() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer p = mc.player;
         if (p != null && mc.level != null) {
            float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float yaw = this.compute(p, pt);
            if ("Client".equals(this.getSetting("Mode", "Server"))) {
               p.yBodyRotO = yaw;
               p.yBodyRot = yaw;
               p.yHeadRotO = yaw;
               p.yHeadRot = yaw;
            } else if (!this.getSetting("Yield To Combat", true) || !AimAssist.hasSilentRotation()) {
               String pm = this.getSetting("Pitch", "Real");

               float pitch = switch (pm) {
                  case "Down" -> 90.0F;
                  case "Up" -> -90.0F;
                  case "Zero" -> 0.0F;
                  default -> p.getXRot();
               };
               AimAssist.setSilentRotation(yaw, pitch);
            }
         } else {
            this.started = false;
         }
      }
   }
}
