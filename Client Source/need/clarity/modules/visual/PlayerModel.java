package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.playermodel.ImportedModel;
import need.clarity.modules.visual.playermodel.ImportedModelRenderer;
import need.clarity.modules.visual.playermodel.Kenny;
import need.clarity.modules.visual.playermodel.KennyRenderer;
import need.clarity.render.KennyPipeline;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PlayerModel extends ModuleBase {
   public static PlayerModel INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private long lastNanos = 0L;
   private double walkPhase = 0.0;
   private double walkAmp = 0.0;
   private double airFactor = 0.0;
   private final float[] anim = new float[7];
   private final double[][] rig = new double[6][12];
   private final double[] world = new double[12];
   private final double[] view = new double[12];
   private static volatile LivingEntityRenderState modelState;
   private static final double[] HAND_R = new double[12];
   private static final double[] HAND_L = new double[12];
   private static volatile boolean handsValid = false;
   private long tAccum;
   private long tFrames;
   private long tLastFrame;
   private long tFrameAccum;
   private long tLastShow;
   private String lastStatus = "";

   public PlayerModel() {
      super("Player Model", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   public static void markModelState(LivingEntityRenderState state) {
      modelState = state;
   }

   public static void forget(LivingEntityRenderState state) {
      if (modelState == state) {
         modelState = null;
      }
   }

   public static boolean isModelState(LivingEntityRenderState state) {
      return state != null && state == modelState && isActive();
   }

   public static double[] handPose(boolean right) {
      if (handsValid && isActive()) {
         return right ? HAND_R : HAND_L;
      } else {
         return null;
      }
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   @Override
   public void onEnable() {
      this.registerListener();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
      handsValid = false;
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
         if (!this.getSetting("Show Timing", false)) {
            this.render(poseStack, partialTick);
         } else {
            long t0 = System.nanoTime();
            this.render(poseStack, partialTick);
            long t1 = System.nanoTime();
            this.tAccum += t1 - t0;
            if (this.tLastFrame != 0L) {
               this.tFrameAccum = this.tFrameAccum + (t1 - this.tLastFrame);
            }

            this.tLastFrame = t1;
            this.tFrames++;
            if (t1 - this.tLastShow > 1000000000L && this.tFrames > 1L) {
               Minecraft mc = Minecraft.getInstance();
               if (mc.player != null) {
                  mc.player
                     .sendOverlayMessage(
                        Component.literal(
                           String.format(
                              "Kenny: %.3f ms/frame  |  whole frame: %.1f ms (%.0f fps)",
                              this.tAccum / 1000000.0 / this.tFrames,
                              this.tFrameAccum / 1000000.0 / (this.tFrames - 1L),
                              1.0E9 * (this.tFrames - 1L) / Math.max(1L, this.tFrameAccum)
                           )
                        )
                     );
               }

               this.tAccum = this.tFrameAccum = this.tFrames = 0L;
               this.tLastShow = t1;
            }
         }
      }
   }

   private void reportModelStatus() {
      String s = ImportedModelRenderer.status();
      if (!s.equals(this.lastStatus) && !s.startsWith("Loading")) {
         this.lastStatus = s;
         boolean ok = s.contains("triangles");

         try {
            if (Notifications.INSTANCE != null && Notifications.INSTANCE.isEnabled()) {
               Notifications.push("Player Model", s, ok);
            } else if (Minecraft.getInstance().player != null) {
               Minecraft.getInstance().player.sendSystemMessage(Component.literal("§b[Player Model]§r " + s));
            }
         } catch (Throwable var4) {
         }
      }
   }

   private void render(PoseStack poseStack, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null && mc.player != null) {
         if (!mc.options.getCameraType().isFirstPerson() || Freecam.isActive()) {
            Player p = mc.player;
            double scale = this.getSetting("Scale", 1.0F).floatValue();
            boolean throughWalls = this.getSetting("Through Walls", false);
            long now = System.nanoTime();
            double dt = this.lastNanos == 0L ? 0.016 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
            this.lastNanos = now;
            double moved = Math.hypot(p.getX() - p.xOld, p.getZ() - p.zOld);
            double target = p.onGround() ? Math.min(1.0, moved / 0.2) : 0.25;
            this.walkAmp = this.walkAmp + (target - this.walkAmp) * Math.min(1.0, dt * 12.0);
            this.walkPhase += dt * moved * 20.0 * 3.6;
            if (this.walkPhase > 1000000.0) {
               this.walkPhase -= 1000000.0;
            }

            this.airFactor = this.airFactor + ((p.onGround() ? 0.0 : 1.0) - this.airFactor) * Math.min(1.0, dt * 10.0);
            double x = Mth.lerp(partialTick, p.xOld, p.getX());
            double y = Mth.lerp(partialTick, p.yOld, p.getY());
            double z = Mth.lerp(partialTick, p.zOld, p.getZ());
            float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
            float headYaw = Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot);
            float pitch = ServerRotations.shouldApply() && AimAssist.hasSilentRotation() ? ServerRotations.serverPitchLerp(partialTick) : Mth.lerp(partialTick, p.xRotO, p.getXRot());
            double hy = Math.toRadians(Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85.0F, 85.0F));
            double hp = Math.toRadians(Mth.clamp(pitch, -89.0F, 89.0F));
            if ("Custom".equals(this.getSetting("Model", "Kenny"))) {
               ImportedModel m = ImportedModelRenderer.get(this.getSetting("Custom Model", ""), this.getSetting("Z Up", false));
               this.reportModelStatus();
               if (m != null) {
                  handsValid = false;
                  Vec3 cam0 = ClarityRenderUtils.getCameraPos();
                  boolean crouch = p.getPose() == Pose.CROUCHING;
                  float height = (float)(Mth.clamp(this.getSetting("Model Height", 1.85F), 0.2F, 8.0F) * scale * (crouch ? 0.9 : 1.0));
                  boolean limbs = m.rigged && this.getSetting("Animate Limbs", true);
                  float[] an = null;
                  if (limbs) {
                     an = this.anim;
                     an[0] = (float)this.walkPhase;
                     an[1] = (float)this.walkAmp;
                     an[2] = (float)this.airFactor;
                     an[3] = crouch ? 1.0F : 0.0F;
                     an[4] = (float)hy;
                     an[5] = (float)hp;
                     an[6] = p.getAttackAnim(partialTick);
                  }

                  float roll = !limbs && this.getSetting("Walk Bob", true) ? (float)(Math.sin(this.walkPhase) * 0.06 * this.walkAmp) : 0.0F;
                  double bob = this.getSetting("Walk Bob", true) ? Math.abs(Math.cos(this.walkPhase)) * (limbs ? 0.02 : 0.04) * this.walkAmp * scale : 0.0;
                  double yawRad = Math.toRadians(bodyYaw + this.getSetting("Yaw Offset", 0.0F));
                  boolean walls0 = throughWalls || Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls() && !Chams.isHideSelf();
                  ImportedModelRenderer.draw(poseStack, m, x - cam0.x, y - cam0.y + bob, z - cam0.z, yawRad, height, roll, this.getSetting("Lighting", true), walls0, RenderAnimations.factor(), an);
                  return;
               }
            }

            Kenny.pose(this.walkPhase, this.walkAmp, p.getAttackAnim(partialTick), p.getPose() == Pose.CROUCHING, hy, hp, this.rig);
            Kenny.hand(this.rig[2], HAND_R);
            Kenny.hand(this.rig[3], HAND_L);

            for (int i = 0; i < 3; i++) {
               HAND_R[i] = HAND_R[i] * scale;
               HAND_L[i] = HAND_L[i] * scale;
            }

            handsValid = true;
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            double yaw = Math.toRadians(bodyYaw);
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            double[] w = this.world;
            w[0] = -fz * scale;
            w[1] = 0.0;
            w[2] = fx * scale;
            w[3] = x - cam.x;
            w[4] = 0.0;
            w[5] = scale;
            w[6] = 0.0;
            w[7] = y - cam.y;
            w[8] = fx * scale;
            w[9] = 0.0;
            w[10] = fz * scale;
            w[11] = z - cam.z;
            Matrix4f pm = poseStack.last().pose();
            double[] v = this.view;
            v[0] = pm.m00();
            v[1] = pm.m10();
            v[2] = pm.m20();
            v[3] = pm.m30();
            v[4] = pm.m01();
            v[5] = pm.m11();
            v[6] = pm.m21();
            v[7] = pm.m31();
            v[8] = pm.m02();
            v[9] = pm.m12();
            v[10] = pm.m22();
            v[11] = pm.m32();
            boolean walls = throughWalls || Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls() && !Chams.isHideSelf();
            RenderType type = walls ? KennyPipeline.throughWalls() : KennyPipeline.occluded();
            float alpha = RenderAnimations.factor();
            BufferSource vcp = mc.renderBuffers().bufferSource();
            VertexConsumer vc = vcp.getBuffer(type);
            KennyRenderer.draw(vc, w, v, this.rig, alpha);
            vcp.endBatch(type);
         }
      }
   }
}
