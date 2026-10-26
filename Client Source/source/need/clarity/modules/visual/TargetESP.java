package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.target.TargetParams;
import need.clarity.modules.visual.cosmetics.target.TargetStyle;
import need.clarity.render.FxTextures;
import need.clarity.render.esp.ShaderEspRenderer;
import need.clarity.render.worldfx.CorpseFx;
import need.clarity.render.worldfx.ShapeFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class TargetESP extends ModuleBase {
   public static TargetESP INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private long lastNanos = 0L;
   private double spinTurns = 0.0;
   private double modelClock = 0.0;
   private int trackedId = Integer.MIN_VALUE;
   private long acquiredNanos = 0L;
   private int flashTargetId = Integer.MIN_VALUE;
   private int lastHurtTime = 0;
   private long flashStartNanos = 0L;
   private double flashAmt = 0.0;
   private int flashRgb = 16711680;
   private int comboEntity = Integer.MIN_VALUE;

   public TargetESP() {
      super("Target ESP", "Visual");
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
      this.lastNanos = 0L;
      this.trackedId = Integer.MIN_VALUE;
      this.resetFlash();
      ShaderEspRenderer.setComboTarget(Integer.MIN_VALUE);
      this.comboEntity = Integer.MIN_VALUE;
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
            LivingEntity target = null;

            try {
               target = AimAssist.getCurrentTarget();
            } catch (Throwable var20) {
            }

            int combo = ShapeFx.comboStyle(this.getSetting("Mode", "Box"));
            int cc1 = ThemeColors.resolveRgb(this.getSetting("Color", ""), -50373, false) & 16777215;
            int cc2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -19635, true) & 16777215;
            CorpseFx.render(ShapeFx.rgb(cc1), ShapeFx.rgb(cc2), this.getSetting("Through Walls", true));
            if (combo >= 0) {
               if (target != null && target.isDeadOrDying()) {
                  ShaderEspRenderer.requestCorpse(target.getId(), combo);
                  this.comboEntity = Integer.MIN_VALUE;
               } else if (target != null && target.isAlive()) {
                  this.comboEntity = target.getId();
                  ShaderEspRenderer.setComboTarget(this.comboEntity);
               } else if (this.comboEntity != Integer.MIN_VALUE) {
                  if (mc.level.getEntity(this.comboEntity) instanceof LivingEntity le && le.isDeadOrDying()) {
                     ShaderEspRenderer.requestCorpse(this.comboEntity, combo);
                  } else {
                     ShaderEspRenderer.setComboTarget(Integer.MIN_VALUE);
                  }

                  this.comboEntity = Integer.MIN_VALUE;
               }
            } else if (this.comboEntity != Integer.MIN_VALUE) {
               ShaderEspRenderer.setComboTarget(Integer.MIN_VALUE);
               this.comboEntity = Integer.MIN_VALUE;
            }

            if (target != null && target.isAlive()) {
               this.updateHitFlash(target);
               String mode = this.getSetting("Mode", "Box");
               int shaderStyle = ShapeFx.targetStyle(mode);
               if (shaderStyle >= 0) {
                  this.drawShader(partialTick, target, shaderStyle);
               } else if ("Image".equals(mode)) {
                  this.drawImage(poseStack, partialTick, target);
               } else {
                  TargetStyle style = TargetStyle.find(mode);
                  if (style != null) {
                     this.drawModel(poseStack, partialTick, target, style);
                  } else {
                     boolean doFill = this.getSetting("Fill", false);
                     boolean throughWalls = this.getSetting("Through Walls", true);
                     int lineRgb = this.flash(ThemeColors.resolveRgb(this.getSetting("Color", ""), -50373, false));
                     int fillRgb = this.flash(ThemeColors.resolveRgb(this.getSetting("Fill Color", ""), -50373, true));
                     float lineOpacity = this.getSetting("Outline Opacity", 0.9F);
                     float fillOpacity = this.getSetting("Fill Opacity", 0.2F);
                     int lineArgb = Math.round(clamp01(lineOpacity) * 255.0F) << 24 | lineRgb & 16777215;
                     int fillArgb = Math.round(clamp01(fillOpacity) * 255.0F) << 24 | fillRgb & 16777215;
                     ClarityRenderUtils.ESPEntry entry = new ClarityRenderUtils.ESPEntry(target.getBoundingBox().inflate(0.12), lineArgb);
                     ClarityRenderUtils.drawEspBoxes(poseStack, List.of(entry), doFill, fillArgb, true, throughWalls, false, this.getSetting("Outline Thickness", 2.0F));
                  }
               }
            } else {
               this.trackedId = Integer.MIN_VALUE;
               this.lastNanos = 0L;
               this.resetFlash();
            }
         }
      }
   }

   private void drawShader(float partialTick, LivingEntity target, int style) {
      long now = System.nanoTime();
      if (target.getId() != this.trackedId) {
         this.trackedId = target.getId();
         this.acquiredNanos = now;
      }

      double age = (now - this.acquiredNanos) / 1.0E9;
      float in = (float)Math.min(1.0, age / 0.35);
      float speed = this.getSetting("Speed", 0.4F);
      float time = (float)(now / 1.0E9 * Math.max(0.1, Math.abs(speed) * 2.5) % 3600.0);
      int c1 = this.flash(ThemeColors.resolveRgb(this.getSetting("Color", ""), -50373, false)) & 16777215;
      int c2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -19635, true) & 16777215;
      float scale = this.getSetting("Scale", 1.0F);
      double x = target.xOld + (target.getX() - target.xOld) * partialTick;
      double y = target.yOld + (target.getY() - target.yOld) * partialTick;
      double z = target.zOld + (target.getZ() - target.zOld) * partialTick;
      ShapeFx.target(
         style, x, y, z, target.getBbWidth() * scale, target.getBbHeight() * scale, ShapeFx.rgb(c1), ShapeFx.rgb(c2), time, (float)this.flashAmt, in, this.getSetting("Through Walls", true)
      );
   }

   private void drawModel(PoseStack poseStack, float partialTick, LivingEntity target, TargetStyle style) {
      long now = System.nanoTime();
      double dt = this.lastNanos == 0L ? 0.0 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
      this.lastNanos = now;
      if (target.getId() != this.trackedId) {
         this.trackedId = target.getId();
         this.acquiredNanos = now;
      }

      double age = (now - this.acquiredNanos) / 1.0E9;
      boolean animate = this.getSetting("Animate", true);
      float speed = this.getSetting("Speed", 0.4F);
      if (animate) {
         this.spinTurns += dt * speed;
         if (this.spinTurns > 10000.0 || this.spinTurns < -10000.0) {
            this.spinTurns %= 1.0;
         }

         this.modelClock += dt;
         if (this.modelClock > 100000.0) {
            this.modelClock %= 3600.0;
         }
      }

      float scale = this.getSetting("Scale", 1.0F);
      float opacity = clamp01(this.getSetting("Opacity", 0.9F));
      boolean throughWalls = this.getSetting("Through Walls", true);
      int color = ThemeColors.resolveRgb(this.getSetting("Color", ""), -50373, false) & 16777215;
      int color2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -24005, true) & 16777215;
      if (style.hasNatural() && this.getSetting("Natural Colors", true)) {
         color = style.natural1;
         color2 = style.natural2;
      }

      color = this.flash(color);
      color2 = this.flash(color2);
      double x = target.xOld + (target.getX() - target.xOld) * partialTick;
      double y = target.yOld + (target.getY() - target.yOld) * partialTick;
      double z = target.zOld + (target.getZ() - target.zOld) * partialTick;
      Attach.Subject subject = Attach.Subject.standing(x, y, z, 0.0, target.getBbHeight());
      Attach.Frame mount = Attach.CENTER.frame(subject);
      TargetParams params = new TargetParams(target.getBbWidth(), target.getBbHeight(), this.spinTurns, speed, age);
      boolean chamsLayered = Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls();
      int depthMode;
      if (chamsLayered) {
         depthMode = 2;
      } else {
         depthMode = throughWalls ? 1 : 0;
      }

      ClarityRenderUtils.CosmeticBatch batch = ClarityRenderUtils.beginCosmetics(poseStack, depthMode);
      Builder builder = new Builder(batch, color, color2, opacity, this.modelClock, animate);
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      builder.view = new double[]{cam.x, cam.y, cam.z};
      builder.frame(mount, scale);
      style.create(params).build(builder);
      batch.end();
   }

   private void drawImage(PoseStack poseStack, float partialTick, LivingEntity target) {
      Identifier tex = FxTextures.get("target");
      if (tex != null) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         Vec3 center = new Vec3(
            target.xOld + (target.getX() - target.xOld) * partialTick,
            target.yOld + (target.getY() - target.yOld) * partialTick + target.getBbHeight() * 0.5,
            target.zOld + (target.getZ() - target.zOld) * partialTick
         );
         double dist = cam.distanceTo(center);
         if (!(dist < 0.5)) {
            double push = Math.min(dist - 0.1, target.getBbWidth() * 0.5 + 0.35);
            Vec3 toCam = cam.subtract(center).scale(1.0 / dist);
            Vec3 pos = center.add(toCam.scale(push));
            double perspective = dist / Math.max(0.1, dist - push);
            double edge = target.getBbHeight() / 3.0;
            double half = edge * 0.5 * clampD(this.getSetting("Image Size", 1.0F).floatValue(), 0.2, 4.0) * perspective;
            double spinDegPerSec = this.getSetting("Image Spin", 45.0F).floatValue();
            double angle = Math.toRadians(System.nanoTime() / 1.0E9 * spinDegPerSec % 360.0);
            int rgb = this.flash(ThemeColors.resolveRgb(this.getSetting("Image Colour", ""), -50373, false)) & 16777215;
            int alpha = Math.round(clamp01(this.getSetting("Image Opacity", 0.9F)) * 255.0F);
            boolean chamsLayered = Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls();
            ClarityRenderUtils.TexBatch batch = ClarityRenderUtils.beginTextured(poseStack, tex, chamsLayered);
            batch.billboard(pos, half, half, angle, alpha << 24 | rgb);
            batch.end();
         }
      }
   }

   private void resetFlash() {
      this.flashTargetId = Integer.MIN_VALUE;
      this.lastHurtTime = 0;
      this.flashStartNanos = 0L;
      this.flashAmt = 0.0;
   }

   private void updateHitFlash(LivingEntity target) {
      if (!this.getSetting("Hit Flash", false)) {
         this.resetFlash();
      } else {
         int hurt = target.hurtTime;
         if (target.getId() != this.flashTargetId) {
            this.flashTargetId = target.getId();
            this.lastHurtTime = hurt;
            this.flashStartNanos = 0L;
         } else if (hurt > this.lastHurtTime) {
            this.flashStartNanos = System.nanoTime();
         }

         this.lastHurtTime = hurt;
         this.flashRgb = ThemeColors.resolveRgb(this.getSetting("Hit Flash Color", "#FF0000"), -65536, false) & 16777215;
         if (this.flashStartNanos == 0L) {
            this.flashAmt = 0.0;
         } else {
            double duration = Math.max(0.05, this.getSetting("Hit Flash Duration", 0.35F).floatValue());
            double t = (System.nanoTime() - this.flashStartNanos) / 1.0E9 / duration;
            if (t >= 1.0) {
               this.flashAmt = 0.0;
               this.flashStartNanos = 0L;
            } else {
               this.flashAmt = 1.0 - t * t;
            }
         }
      }
   }

   private int flash(int rgb) {
      if (this.flashAmt <= 0.0) {
         return rgb;
      }

      int mixed = Col.mix(0xFF000000 | rgb & 16777215, 0xFF000000 | this.flashRgb, this.flashAmt);
      return mixed & 16777215 | rgb & 0xFF000000;
   }

   private static double clampD(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
