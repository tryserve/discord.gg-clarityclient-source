package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.aura.AuraParams;
import need.clarity.modules.visual.cosmetics.aura.AuraReact;
import need.clarity.modules.visual.cosmetics.aura.CircleParams;
import need.clarity.modules.visual.cosmetics.aura.ElementParams;
import need.clarity.modules.visual.cosmetics.aura.ElementStyle;
import need.clarity.modules.visual.cosmetics.aura.EnergySphere;
import need.clarity.modules.visual.cosmetics.aura.LightningArcs;
import need.clarity.modules.visual.cosmetics.aura.LightningParams;
import need.clarity.modules.visual.cosmetics.aura.OrbitStyle;
import need.clarity.modules.visual.cosmetics.aura.Ripple;
import need.clarity.modules.visual.cosmetics.aura.RuneCircle;
import need.clarity.modules.visual.cosmetics.aura.SphereParams;
import need.clarity.render.worldfx.ShapeFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Aura extends ModuleBase {
   public static Aura INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private final long startNanos = System.nanoTime();
   private long lastNanos = 0L;
   private double orbitTurns = 0.0;
   private double auraClock = 0.0;
   private long speedNanos = 0L;
   private double speedX = 0.0;
   private double speedZ = 0.0;
   private double energy = 0.0;
   private static final int MAX_RIPPLES = 4;
   private static final double HIT_RANGE = 8.0;
   private final List<double[]> hits = new java.util.ArrayList<>();
   private int lastHurtTime = 0;
   static final String MODE_ORBIT = "Orbiting";
   static final String MODE_SPHERE = "Energy Sphere";
   static final String MODE_ARCS = "Lightning Arcs";
   static final String MODE_GROUND = "Ground Runes";
   static final String MODE_ELEMENT = "Elemental";

   private String mode() {
      String m = this.getSetting("Mode", "");
      if (ShapeFx.auraStyle(m) >= 0) {
         return m;
      } else if ("Orbiting".equals(m) || "Energy Sphere".equals(m) || "Lightning Arcs".equals(m) || "Ground Runes".equals(m) || "Elemental".equals(m)) {
         return m;
      } else if (this.getSetting("Energy Sphere", false)) {
         return "Energy Sphere";
      } else {
         return this.getSetting("Lightning Arcs", false) ? "Lightning Arcs" : "Orbiting";
      }
   }

   public Aura() {
      super("Aura", "Visual");
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
      this.hits.clear();
      this.lastHurtTime = 0;
      this.speedNanos = 0L;
      this.energy = 0.0;
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
            Player p = mc.player;
            double time = (System.nanoTime() - this.startNanos) / 1.0E9;
            this.trackHits(mc, p, time);
            this.trackSpeed(p, partialTick);
            boolean firstPerson = mc.options.getCameraType().isFirstPerson() && !Freecam.isActive();
            if (!firstPerson || !this.getSetting("Hide In First Person", false)) {
               String auraMode = this.mode();
               boolean orbiting = "Orbiting".equals(auraMode);
               boolean sphere = "Energy Sphere".equals(auraMode);
               boolean arcs = "Lightning Arcs".equals(auraMode);
               boolean ground = "Ground Runes".equals(auraMode);
               boolean element = "Elemental".equals(auraMode);
               OrbitStyle style = OrbitStyle.fromName(this.getSetting("Orbit Style", "Shards"));
               ElementStyle elementStyle = ElementStyle.fromName(this.getSetting("Element", "Flame"));
               float scale = this.getSetting("Scale", 1.0F);
               float opacity = clamp01(this.getSetting("Opacity", 1.0F));
               float height = this.getSetting("Height", 0.0F);
               float speed = this.getSetting("Speed", 0.25F);
               boolean animate = this.getSetting("Animate", true);
               boolean throughWalls = this.getSetting("Through Walls", false);
               int color = ThemeColors.resolveRgb(this.getSetting("Color", ""), -9443329, false) & 16777215;
               int color2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -5936129, true) & 16777215;
               if (element && this.getSetting("Natural Colors", true)) {
                  color = elementStyle.natural1;
                  color2 = elementStyle.natural2;
               }

               long now = System.nanoTime();
               double dt = this.lastNanos == 0L ? 0.0 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
               this.lastNanos = now;
               AuraReact.Reaction react = AuraReact.at(this.energy, this.getSetting("Speed Reaction", 1.0F).floatValue());
               if (animate) {
                  this.orbitTurns = this.orbitTurns + dt * speed * react.turn();
                  if (this.orbitTurns > 10000.0 || this.orbitTurns < -10000.0) {
                     this.orbitTurns %= 1.0;
                  }

                  this.auraClock = this.auraClock + dt * react.clock();
                  if (this.auraClock > 100000.0) {
                     this.auraClock %= 3600.0;
                  }
               }

               AuraParams params = new AuraParams(this.getSetting("Count", 5), this.getSetting("Radius", 1.0F).floatValue() * react.radius(), this.orbitTurns, speed);
               double x = p.xOld + (p.getX() - p.xOld) * partialTick;
               double y = p.yOld + (p.getY() - p.yOld) * partialTick;
               double z = p.zOld + (p.getZ() - p.zOld) * partialTick;
               float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
               float headYaw = Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot);
               float pitch = Mth.lerp(partialTick, p.xRotO, p.getXRot());
               Pose pose = p.getPose();
               Attach.Subject subject = new Attach.Subject(x, y, z, bodyYaw, headYaw, pitch, p.getBbHeight(), pose == Pose.CROUCHING, pose == Pose.SWIMMING, pose == Pose.FALL_FLYING, 1.0);
               int shaderAura = ShapeFx.auraStyle(auraMode);
               if (shaderAura >= 0) {
                  ShapeFx.aura(
                     shaderAura,
                     x,
                     y + height,
                     z,
                     p.getBbHeight(),
                     scale * (float)react.radius(),
                     this.getSetting("Radius", 1.0F),
                     this.getSetting("Count", 5),
                     ShapeFx.rgb(color),
                     ShapeFx.rgb(color2),
                     opacity,
                     (float)(this.auraClock % 3600.0),
                     throughWalls
                  );
               } else {
                  Attach anchor = !ground && !element ? Attach.CENTER : Attach.FEET;
                  Attach.Frame mount = anchor.frame(subject).shifted(0.0, height, 0.0);
                  int mode;
                  if (Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls() && !Chams.isHideSelf()) {
                     mode = 2;
                  } else {
                     mode = throughWalls ? 1 : 0;
                  }

                  ClarityRenderUtils.CosmeticBatch batch = ClarityRenderUtils.beginCosmetics(poseStack, mode);
                  Builder builder = new Builder(batch, color, color2, opacity, this.auraClock, animate);
                  Vec3 cam = ClarityRenderUtils.getCameraPos();
                  builder.view = new double[]{cam.x, cam.y, cam.z};
                  builder.frame(mount, scale);
                  if (orbiting) {
                     style.create(params).build(builder);
                  } else if (sphere) {
                     SphereParams sp = new SphereParams(
                        this.getSetting("Sphere Radius", 1.2F).floatValue(),
                        "Low".equalsIgnoreCase(this.getSetting("Sphere Detail", "High")) ? 2 : 3,
                        this.getSetting("Hit Ripple", true) ? this.localRipples(mount, time) : List.of()
                     );
                     new EnergySphere(sp).build(builder);
                  } else if (arcs) {
                     new LightningArcs(
                           new LightningParams(this.getSetting("Arc Count", 6), this.getSetting("Arc Reach", 1.2F).floatValue() * react.reach(), this.getSetting("Crackle Rate", 20).intValue(), 600)
                        )
                        .build(builder);
                  } else if (ground) {
                     new RuneCircle(new CircleParams(this.getSetting("Circle Radius", 1.5F).floatValue() * react.radius(), this.orbitTurns)).build(builder);
                  } else if (element) {
                     elementStyle.create(new ElementParams(this.getSetting("Radius", 1.0F).floatValue() * react.radius(), p.getBbHeight())).build(builder);
                  }

                  batch.end();
               }
            }
         }
      }
   }

   private void trackSpeed(Player p, float partialTick) {
      long now = System.nanoTime();
      double x = p.xOld + (p.getX() - p.xOld) * partialTick;
      double z = p.zOld + (p.getZ() - p.zOld) * partialTick;
      double dt = this.speedNanos == 0L ? 0.0 : (now - this.speedNanos) / 1.0E9;
      if (this.speedNanos == 0L || !(dt < 0.002)) {
         if (this.speedNanos != 0L && dt <= 0.25) {
            double d = Math.hypot(x - this.speedX, z - this.speedZ);
            if (d <= 10.0) {
               this.energy = AuraReact.follow(this.energy, AuraReact.energyFor(d / dt), dt);
            }
         }

         this.speedX = x;
         this.speedZ = z;
         this.speedNanos = now;
      }
   }

   private void trackHits(Minecraft mc, Player p, double time) {
      this.hits.removeIf(h -> time - h[3] > 1.3);
      int ht = p.hurtTime;
      boolean hurt = ht > this.lastHurtTime && ht > 0;
      this.lastHurtTime = ht;
      if (hurt && "Energy Sphere".equals(this.mode()) && this.getSetting("Hit Ripple", true)) {
         double cx = p.getX();
         double cy = p.getY() + p.getBbHeight() * 0.5;
         double cz = p.getZ();
         double best = 64.0;
         double dx = 0.0;
         double dy = 0.0;
         double dz = 0.0;
         boolean found = false;

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e != p && e instanceof LivingEntity le && !le.isRemoved()) {
               double ex = le.getX() - cx;
               double ey = le.getY() + le.getBbHeight() * 0.5 - cy;
               double ez = le.getZ() - cz;
               double d2 = ex * ex + ey * ey + ez * ez;
               if (d2 < best && d2 > 1.0E-6) {
                  best = d2;
                  dx = ex;
                  dy = ey;
                  dz = ez;
                  found = true;
               }
            }
         }

         if (!found) {
            double yaw = Math.toRadians(p.yBodyRot);
            dx = -Math.sin(yaw);
            dy = 0.25;
            dz = Math.cos(yaw);
         }

         double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (this.hits.size() >= 4) {
            this.hits.remove(0);
         }

         this.hits.add(new double[]{dx / l, dy / l, dz / l, time});
      }
   }

   private List<Ripple> localRipples(Attach.Frame f, double time) {
      List<Ripple> out = new java.util.ArrayList<>(this.hits.size());

      for (double[] h : this.hits) {
         out.add(new Ripple(h[0] * f.rx + h[1] * f.ry + h[2] * f.rz, h[0] * f.ux + h[1] * f.uy + h[2] * f.uz, h[0] * f.fx + h[1] * f.fy + h[2] * f.fz, time - h[3]));
      }

      return out;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
