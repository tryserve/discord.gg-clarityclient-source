package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.wings.WingBuilder;
import need.clarity.modules.visual.wings.WingStyle;
import need.clarity.render.worldfx.CosmeticFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Wings extends ModuleBase {
   public static Wings INSTANCE;
   private static final double REST_ROLL = 0.1;
   private static final double REST_FOLD = 0.18;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private long lastNanos = 0L;
   private double flapPhase = 0.0;
   private double flapAmp = 0.0;
   private final long startNanos = System.nanoTime();

   public Wings() {
      super("Wings", "Visual");
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
            boolean throughWalls = this.getSetting("Through Walls", false);
            float scale = this.getSetting("Scale", 1.0F);
            float opacity = clamp01(this.getSetting("Opacity", 0.85F));
            boolean animate = this.getSetting("Animate", true);
            float flapSpeed = this.getSetting("Flap Speed", 1.0F);
            float yOffset = this.getSetting("Y Offset", 0.0F);
            WingStyle style = WingStyle.fromName(this.getSetting("Style", "Angel"));
            int color = ThemeColors.resolveRgb(this.getSetting("Color", ""), -16720641, false) & 16777215;
            int color2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -8769793, true) & 16777215;
            Player p = mc.player;
            Vec3 pos = new Vec3(p.xOld + (p.getX() - p.xOld) * partialTick, p.yOld + (p.getY() - p.yOld) * partialTick, p.zOld + (p.getZ() - p.zOld) * partialTick);
            int mode;
            if (Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls() && !Chams.isHideSelf()) {
               mode = 2;
            } else {
               mode = throughWalls ? 1 : 0;
            }

            boolean isSneaking = p.isCrouching();
            boolean isSwimming = p.isSwimming();
            boolean isFallFlying = p.isFallFlying();
            long now = System.nanoTime();
            double dt = this.lastNanos == 0L ? 0.016 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
            this.lastNanos = now;
            double time = (now - this.startNanos) / 1.0E9;
            double roll = 0.1;
            double fold = 0.18;
            if (animate) {
               double moved = Math.hypot(p.getX() - p.xOld, p.getZ() - p.zOld);
               boolean airborne = !p.onGround();
               double targetAmp;
               double freq;
               if (isFallFlying) {
                  targetAmp = 0.12 + Math.min(moved, 0.5) * 0.15;
                  freq = (0.4 + Math.min(moved, 0.5) * 1.2) * flapSpeed;
               } else if (isSwimming) {
                  targetAmp = 0.2 + Math.min(moved, 0.3) * 0.25;
                  freq = (1.0 + Math.min(moved, 0.3) * 2.0) * flapSpeed;
               } else {
                  targetAmp = airborne ? 0.36 : 0.05 + Math.min(moved, 0.3) * 0.35;
                  freq = (airborne ? 2.3 : 0.7 + Math.min(moved, 0.3) * 3.0) * flapSpeed;
               }

               this.flapAmp = this.flapAmp + (targetAmp - this.flapAmp) * Math.min(1.0, dt * 6.0);
               this.flapPhase += dt * freq * Math.PI * 2.0;
               if (this.flapPhase > 1000000.0) {
                  this.flapPhase -= 1000000.0;
               }

               roll = 0.1 + this.flapAmp * Math.sin(this.flapPhase);
               fold = Math.max(0.0, 0.18 - this.flapAmp * 0.35 * (0.5 + 0.5 * Math.sin(this.flapPhase)));
               if (isFallFlying) {
                  fold = Math.max(fold, 0.05);
                  roll *= 0.6;
               }
            }

            float bodyYaw = p.yBodyRotO + (p.yBodyRot - p.yBodyRotO) * partialTick;
            float headPitch = p.xRotO + (p.getXRot() - p.xRotO) * partialTick;
            Attach.Subject subject = new Attach.Subject(pos.x, pos.y, pos.z, bodyYaw, bodyYaw, headPitch, p.getBbHeight(), isSneaking, isSwimming, isFallFlying, 1.0);
            int shaderStyle = CosmeticFx.wingStyle(this.getSetting("Style", "Angel"));
            if (shaderStyle >= 0) {
               Attach.Frame mount = Attach.BACK.frame(subject);
               double[] base = new double[]{mount.ox, mount.oy + yOffset, mount.oz};
               CosmeticFx.wings(
                  shaderStyle, base, -mount.rx, -mount.rz, mount.fx, mount.fz, 1.1 * scale, roll, fold, CosmeticFx.rgb(color), CosmeticFx.rgb(color2), opacity, throughWalls, (float)(time % 3600.0)
               );
            } else {
               this.drawWings(poseStack, p, pos, partialTick, style, scale, color, color2, opacity, mode, animate, time, roll, fold, yOffset, subject);
            }
         }
      }
   }

   private void drawWings(
      PoseStack poseStack,
      Player p,
      Vec3 pos,
      float partialTick,
      WingStyle style,
      float scale,
      int color,
      int color2,
      float opacity,
      int mode,
      boolean animate,
      double time,
      double roll,
      double fold,
      float yOffset,
      Attach.Subject subject
   ) {
      Attach.Frame mount = Attach.BACK.frame(subject);
      double fx = mount.fx;
      double fz = mount.fz;
      double rx = -mount.rx;
      double rz = -mount.rz;
      double[] base = new double[]{mount.ox, mount.oy + yOffset, mount.oz};
      double unit = scale * style.unit;
      ClarityRenderUtils.CosmeticBatch batch = ClarityRenderUtils.beginCosmetics(poseStack, mode);
      WingBuilder builder = new WingBuilder(batch, color, color2, opacity, time, animate);
      builder.frame(base, rx, rz, fx, fz, unit, roll, fold, 0);
      style.build(builder);
      builder.frame(base, -rx, -rz, fx, fz, unit, roll, fold, 1);
      style.build(builder);
      batch.end();
   }

   public static boolean shouldReplaceElytra() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.getSetting("Replace Elytra", false);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
