package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.visual.playermodel.Mat;
import need.clarity.modules.visual.playermodel.PetModel;
import need.clarity.modules.visual.playermodel.PetModels;
import need.clarity.render.KennyPipeline;
import need.clarity.render.worldfx.PetFx;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public class Pets extends ModuleBase {
   public static Pets INSTANCE;
   private static final double GRAVITY = 24.0;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private final long startNanos = System.nanoTime();
   private boolean placed = false;
   private ClientLevel lastLevel;
   private String lastPet = "";
   private double px;
   private double py;
   private double pz;
   private double vy;
   private double yaw;
   private double walkPhase;
   private double walkAmp;
   private double idle;
   private double waveT = -1.0;
   private double nextWave = 3.5;
   private long lastNanos;
   private final double[][] rig = new double[7][12];
   private final double[] world = new double[12];
   private final double[] view = new double[12];
   private final MutableBlockPos probe = new MutableBlockPos();

   public Pets() {
      super("Pets", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   @Override
   public void onEnable() {
      this.placed = false;
      this.registerListener();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
      this.placed = false;
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
            String pet = this.getSetting("Pet", "Mini Kenny");
            PetModel model = PetModels.get(pet);
            long now = System.nanoTime();
            double dt = this.lastNanos == 0L ? 0.016 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
            this.lastNanos = now;
            double time = (now - this.startNanos) / 1.0E9;
            Player p = mc.player;
            double x = Mth.lerp(partialTick, p.xOld, p.getX());
            double y = Mth.lerp(partialTick, p.yOld, p.getY());
            double z = Mth.lerp(partialTick, p.zOld, p.getZ());
            float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
            if (mc.level != this.lastLevel || !pet.equals(this.lastPet)) {
               this.placed = false;
               this.lastLevel = mc.level;
               this.lastPet = pet;
            }

            double scale = this.getSetting("Scale", 1.0F).floatValue();
            double dist = this.getSetting("Follow Distance", 1.2F).floatValue();
            String side = this.getSetting("Position", "Right");
            double sgn = "Left".equals(side) ? -1.0 : ("Behind".equals(side) ? 0.0 : 1.0);
            double by = Math.toRadians(bodyYaw);
            double fx = -Math.sin(by);
            double fz = Math.cos(by);
            double rx = -fz;
            double rz = fx;
            double back = sgn == 0.0 ? dist : dist * 0.45;
            double tx = x + rx * sgn * dist * 0.85 - fx * back;
            double tz = z + rz * sgn * dist * 0.85 - fz * back;
            double s = model.petScale() * scale;
            this.step(mc.level, p, model, dt, tx, tz, x, y, z, this.getSetting("Follow Speed", 1.0F).floatValue(), s);
            boolean firstPerson = mc.options.getCameraType().isFirstPerson() && !Freecam.isActive();
            if (!firstPerson || !this.getSetting("Hide In First Person", false)) {
               boolean through = this.getSetting("Through Walls", false);
               float fade = RenderAnimations.factor();
               if (this.getSetting("Shadow", true)) {
                  double g = this.groundOr(mc.level, this.px, this.pz, this.py, this.py);
                  float k = (float)Mth.clamp(1.0 - (this.py - g) * 1.5, 0.0, 1.0);
                  PetFx.shadow(this.px, g, this.pz, model.shadowRadius() * scale, 0.9F * fade * k, false);
               }

               this.draw(mc, poseStack, model, p, y + p.getEyeHeight(), s, through, fade, time);
            }
         }
      }
   }

   private void step(ClientLevel level, Player p, PetModel model, double dt, double tx, double tz, double x, double y, double z, double speedMul, double s) {
      if (!this.placed) {
         this.px = tx;
         this.pz = tz;
         this.py = this.groundOr(level, tx, tz, y + 0.5, y);
         this.vy = 0.0;
         this.yaw = p.getYRot();
         this.walkAmp = 0.0;
         this.idle = 0.0;
         this.waveT = -1.0;
         this.placed = true;
      }

      double dx = tx - this.px;
      double dz = tz - this.pz;
      double d = Math.hypot(dx, dz);
      double dyP = y - this.py;
      if (d > 14.0 || dyP < -6.0 || dyP > 6.0 && p.onGround()) {
         this.px = tx;
         this.pz = tz;
         this.py = this.groundOr(level, tx, tz, y + 0.5, y);
         this.vy = 0.0;
         d = 0.0;
         dz = 0.0;
         dx = 0.0;
      }

      double v = Math.min(9.0, Math.max(0.0, (d - 0.25) * 3.2)) * speedMul;
      double step = Math.min(v * dt, Math.max(0.0, d - 0.2));
      if (d > 1.0E-4 && step > 0.0) {
         this.px += dx / d * step;
         this.pz += dz / d * step;
      }

      double moveSpeed = step / Math.max(dt, 1.0E-4);
      double targetYaw = moveSpeed > 0.3 ? Math.toDegrees(Math.atan2(-dx, dz)) : Math.toDegrees(Math.atan2(-(x - this.px), z - this.pz));
      this.yaw = this.yaw + Mth.clamp(Mth.wrapDegrees(targetYaw - this.yaw), -540.0 * dt, 540.0 * dt);
      this.walkAmp = this.walkAmp + (Math.min(1.0, moveSpeed / 2.2) - this.walkAmp) * Math.min(1.0, dt * 10.0);
      this.walkPhase = this.walkPhase + dt * moveSpeed * 3.6 / Math.max(0.2, s * model.stride());
      if (this.walkPhase > 1000000.0) {
         this.walkPhase -= 1000000.0;
      }

      double ground = this.groundOr(level, this.px, this.pz, this.py + 1.1, Math.min(this.py, y));
      if (this.vy != 0.0 || this.py > ground + 0.001) {
         this.vy -= 24.0 * dt;
         this.py = this.py + this.vy * dt;
         if (this.vy <= 0.0 && this.py <= ground) {
            this.py = ground;
            this.vy = 0.0;
         }
      } else if (ground > this.py) {
         double h = ground - this.py;
         if (h > 0.3) {
            this.vy = Math.sqrt(48.0 * (h + 0.2));
         } else {
            this.py = Math.min(ground, this.py + dt * 4.0);
         }
      }

      if (!(moveSpeed > 0.3) && this.vy == 0.0) {
         this.idle += dt;
      } else {
         this.idle = 0.0;
         this.waveT = -1.0;
         this.nextWave = 3.5;
      }

      if (this.waveT >= 0.0) {
         this.waveT += dt;
      }
   }

   private void draw(Minecraft mc, PoseStack poseStack, PetModel model, Player p, double eyeY, double s, boolean through, float fade, double time) {
      double hx = p.getX() - this.px;
      double hz = p.getZ() - this.pz;
      double hy = 0.0;
      double hp = 0.0;
      if (this.getSetting("Look At You", true)) {
         double dirYaw = Math.toDegrees(Math.atan2(-hx, hz));
         hy = Math.toRadians(Mth.clamp(Mth.wrapDegrees(dirYaw - this.yaw), -70.0, 70.0));
         double headY = this.py + s * model.eyeHeight();
         hp = Mth.clamp(-Math.atan2(eyeY - headY, Math.max(0.3, Math.hypot(hx, hz))), -0.75, 0.5);
      }

      model.pose(this.walkPhase, this.walkAmp, p.getPose() == Pose.CROUCHING, hy, hp, time, this.idle, this.rig);
      if (model.canWave() && this.getSetting("Wave", true)) {
         if (this.waveT < 0.0 && this.idle > this.nextWave) {
            this.waveT = 0.0;
            this.nextWave = this.idle + 5.0 + 4.0 * Math.random();
         }

         if (this.waveT >= 0.0) {
            double env = Math.min(1.0, Math.min(this.waveT / 0.25, (1.8 - this.waveT) / 0.25));
            if (env <= 0.0 && this.waveT > 1.0) {
               this.waveT = -1.0;
            } else {
               Mat.rotZ(this.rig[2], Math.max(0.0, env) * (model.waveAngle() + 0.32 * Math.sin(this.waveT * 14.0)));
            }
         }
      }

      Vec3 cam = ClarityRenderUtils.getCameraPos();
      double yr = Math.toRadians(this.yaw);
      double fx = -Math.sin(yr);
      double fz = Math.cos(yr);
      double[] w = this.world;
      w[0] = -fz * s;
      w[1] = 0.0;
      w[2] = fx * s;
      w[3] = this.px - cam.x;
      w[4] = 0.0;
      w[5] = s;
      w[6] = 0.0;
      w[7] = this.py - cam.y;
      w[8] = fx * s;
      w[9] = 0.0;
      w[10] = fz * s;
      w[11] = this.pz - cam.z;
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
      RenderType type = through ? KennyPipeline.throughWalls() : KennyPipeline.occluded();
      BufferSource vcp = mc.renderBuffers().bufferSource();
      VertexConsumer vc = vcp.getBuffer(type);
      model.draw(vc, w, v, this.rig, fade);
      vcp.endBatch(type);
   }

   private double groundY(ClientLevel level, double x, double z, double fromY, double minY) {
      int bx = Mth.floor(x);
      int bz = Mth.floor(z);
      int top = Mth.floor(fromY);
      int bottom = Math.max(Mth.floor(minY), level.getMinY());

      for (int byy = top; byy >= bottom; byy--) {
         this.probe.set(bx, byy, bz);
         BlockState st = level.getBlockState(this.probe);
         if (!st.isAir()) {
            VoxelShape shape = st.getCollisionShape(level, this.probe);
            if (!shape.isEmpty()) {
               double h = byy + shape.bounds().maxY;
               if (h <= fromY + 0.001) {
                  return h;
               }
            }
         }
      }

      return Double.NaN;
   }

   private double groundOr(ClientLevel level, double x, double z, double fromY, double fallback) {
      double g = this.groundY(level, x, z, fromY, fromY - 6.0);
      return Double.isNaN(g) ? fallback : g;
   }
}
