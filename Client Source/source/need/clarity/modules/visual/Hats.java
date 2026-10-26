package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.hats.HatParams;
import need.clarity.modules.visual.cosmetics.hats.HatStyle;
import need.clarity.render.worldfx.CosmeticFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Hats extends ModuleBase {
   public static Hats INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private final long startNanos = System.nanoTime();

   public Hats() {
      super("Hats", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   @Override
   public void onEnable() {
      this.registerListener();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
   }

   private void registerListener() {
      if (!this.listenerRegistered) {
         this.listenerRegistered = true;
         WorldRenderEvent.register(this.renderListener);
      }
   }

   private void unregisterListener() {
      if (this.listenerRegistered) {
         this.listenerRegistered = false;
         WorldRenderEvent.unregister(this.renderListener);
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            boolean firstPerson = mc.options.getCameraType().isFirstPerson() && !Freecam.isActive();
            if (!firstPerson || !this.getSetting("Hide In First Person", true)) {
               Player p = mc.player;
               String hatName = this.getSetting("Hat", "China Hat");
               int shaderHat = CosmeticFx.hatStyle(hatName);
               HatStyle style = HatStyle.fromName(hatName);
               float scale = this.getSetting("Scale", 1.0F);
               float opacity = clamp01(this.getSetting("Opacity", 1.0F));
               float height = this.getSetting("Height", 0.0F);
               boolean animate = this.getSetting("Animate", true);
               boolean throughWalls = this.getSetting("Through Walls", false);
               boolean followTilt = this.getSetting("Follow Head Tilt", true);
               boolean bob = this.getSetting("Bob", true);
               int color = ThemeColors.resolveRgb(this.getSetting("Color", ""), -8090, false) & 16777215;
               int color2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -26052, true) & 16777215;
               HatParams params = new HatParams(
                  this.chinaGradient(),
                  clamp01(this.getSetting("Center Alpha", 1.0F)),
                  clamp01(this.getSetting("Edge Alpha", 0.4F)),
                  this.getSetting("Rotation Speed", -0.2F).floatValue(),
                  this.getSetting("Spin", 0.15F).floatValue(),
                  !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                  bob
               );
               double x = p.xOld + (p.getX() - p.xOld) * partialTick;
               double y = p.yOld + (p.getY() - p.yOld) * partialTick;
               double z = p.zOld + (p.getZ() - p.zOld) * partialTick;
               float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
               float headYaw = Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot);
               float pitch = ServerRotations.shouldApply() && AimAssist.hasSilentRotation() ? ServerRotations.serverPitchLerp(partialTick) : Mth.lerp(partialTick, p.xRotO, p.getXRot());
               Pose pose = p.getPose();
               Attach.Subject subject = new Attach.Subject(
                  x, y, z, bodyYaw, headYaw, pitch, p.getBbHeight(), pose == Pose.CROUCHING, pose == Pose.SWIMMING, pose == Pose.FALL_FLYING, followTilt ? 1.0 : 0.0
               );
               Attach.Frame mount = Attach.HEAD.frame(subject).shifted(0.0, 0.234 + height, 0.0);
               if (shaderHat >= 0) {
                  double t = (System.nanoTime() - this.startNanos) / 1.0E9;
                  CosmeticFx.hat(
                     shaderHat, mount, scale, CosmeticFx.rgb(color), CosmeticFx.rgb(color2), opacity, throughWalls, (float)(animate ? t % 3600.0 : 0.0), this.getSetting("Spin", 0.15F), bob && animate
                  );
               } else {
                  int mode;
                  if (Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls() && !Chams.isHideSelf()) {
                     mode = 2;
                  } else {
                     mode = throughWalls ? 1 : 0;
                  }

                  double time = (System.nanoTime() - this.startNanos) / 1.0E9;
                  ClarityRenderUtils.CosmeticBatch batch = ClarityRenderUtils.beginCosmetics(poseStack, mode);
                  Builder builder = new Builder(batch, color, color2, opacity, time, animate);
                  Vec3 cam = ClarityRenderUtils.getCameraPos();
                  builder.view = new double[]{cam.x, cam.y, cam.z};
                  builder.frame(mount, scale);
                  style.create(params).build(builder);
                  batch.end();
               }
            }
         }
      }
   }

   private int[] chinaGradient() {
      if (!"Custom".equals(this.getSetting("Color Mode", "Sync"))) {
         return new int[]{ThemeColors.resolveArgb("", -16714844, false)};
      }

      return switch ((String)this.getSetting("Color Count", "Quartet")) {
         case "Solo" -> new int[]{this.hex("Color 1", "#FFE96B", false)};
         case "Duo" -> new int[]{this.hex("Color 1", "#FFE96B", false), this.hex("Color 2", "#3B5B30", true)};
         case "Triple" -> new int[]{this.hex("Color 1", "#FFE96B", false), this.hex("Color 2", "#3B5B30", true), this.hex("Color 3", "#9A4676", false)};
         default -> new int[]{this.hex("Color 1", "#FFE96B", false), this.hex("Color 2", "#3B5B30", true), this.hex("Color 3", "#9A4676", false), this.hex("Color 4", "#F9B9EB", true)};
      };
   }

   private int hex(String key, String def, boolean secondary) {
      return ThemeColors.resolveArgb(this.getSetting(key, def), -16714844, secondary);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
