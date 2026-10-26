package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Random;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.render.ImageParticles;
import need.clarity.render.worldfx.GpuFx;
import need.clarity.render.worldfx.ShapeFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class HitParticles extends ModuleBase {
   private final Random random = new Random();
   private final List<HitParticles.Ring> rings = new java.util.ArrayList<>();
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public HitParticles() {
      super("Hit Particles", "Visual");
      PerAttackEvents.register(this::onAttack);
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
      this.rings.clear();
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

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled()) {
         Entity target = event.getTarget();
         if (target != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
               String particleName = this.getSetting("Particle", "Crit");
               int shaderStyle = GpuFx.hitStyle(particleName);
               if (shaderStyle >= 0) {
                  float spreadS = this.getSetting("Spread", 0.3F);
                  GpuFx.addBurst(shaderStyle, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 0.55F + spreadS);
                  if (this.getSetting("Ring", false)) {
                     this.rings.add(new HitParticles.Ring(new Vec3(target.getX(), target.getY(), target.getZ()), System.nanoTime()));
                  }
               } else {
                  int count = this.getSetting("Count", 10);
                  float spread = this.getSetting("Spread", 0.3F);
                  float size = Math.max(0.5F, Math.min(3.0F, this.getSetting("Size", 1.0F)));
                  int rgb = ThemeColors.resolveRgb(this.getSetting("Colour", "#FF0000"), 16711680, false);
                  String secondName = this.getSetting("Second Particle", "None");
                  String tex1 = ImageParticles.textureFor(particleName);
                  String tex2 = ImageParticles.textureFor(secondName);
                  ParticleOptions particle = tex1 == null ? resolveParticle(particleName, rgb, size) : null;
                  if (tex1 != null || particle != null) {
                     ParticleOptions particle2 = tex2 == null ? resolveParticle(secondName, rgb, size) : null;
                     boolean hasSecond = tex2 != null || particle2 != null;
                     int imgRgb = ThemeColors.resolveRgb(this.getSetting("Image Colour", "#FFFFFF"), 16777215, false) & 16777215;
                     double x = target.getX();
                     double y = target.getY() + target.getBbHeight() * 0.5;
                     double z = target.getZ();
                     double scale = (spread + 0.2) * size;

                     for (int i = 0; i < count; i++) {
                        double ox = (this.random.nextDouble() - 0.5) * spread * 2.0;
                        double oy = (this.random.nextDouble() - 0.5) * spread * 2.0;
                        double oz = (this.random.nextDouble() - 0.5) * spread * 2.0;
                        double vx = (this.random.nextDouble() - 0.5) * 0.3 * scale;
                        double vy = this.random.nextDouble() * 0.2 * scale;
                        double vz = (this.random.nextDouble() - 0.5) * 0.3 * scale;
                        boolean useSecond = hasSecond && (i & 1) == 1;
                        String imgTex = useSecond ? tex2 : tex1;
                        if (imgTex != null) {
                           ImageParticles.spawn(
                              imgTex,
                              x + ox,
                              y + oy,
                              z + oz,
                              vx * 20.0,
                              vy * 20.0 + 0.4,
                              vz * 20.0,
                              0.45F + this.random.nextFloat() * 0.35F,
                              0.22F * size * (0.7F + this.random.nextFloat() * 0.6F),
                              3.0F,
                              imgRgb,
                              false
                           );
                        } else {
                           ParticleOptions use = useSecond ? particle2 : particle;
                           mc.level.addParticle(use, x + ox, y + oy, z + oz, vx, vy, vz);
                        }
                     }

                     if (this.getSetting("Ring", false)) {
                        this.rings.add(new HitParticles.Ring(new Vec3(target.getX(), target.getY(), target.getZ()), System.nanoTime()));
                     }
                  }
               }
            }
         }
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         if (GpuFx.hasBursts()) {
            int style = GpuFx.hitStyle(this.getSetting("Particle", "Crit"));
            if (style >= 0) {
               int c1 = ThemeColors.resolveRgb(this.getSetting("Colour", "#FF0000"), 16711680, false) & 16777215;
               int c2 = ThemeColors.resolveRgb(this.getSetting("Second Colour", "#FFD36B"), 16765803, true) & 16777215;
               GpuFx.renderBursts(
                  style,
                  this.getSetting("Count", 10) * 2,
                  Math.max(0.5F, Math.min(3.0F, this.getSetting("Size", 1.0F))),
                  ShapeFx.rgb(c1),
                  ShapeFx.rgb(c2),
                  this.getSetting("Natural Colors", true),
                  this.getSetting("Through Walls", false)
               );
            }
         }

         if (!this.rings.isEmpty()) {
            double ttl = this.getSetting("Ring TTL", 0.6F).floatValue();
            double speed = this.getSetting("Ring Speed", 7.0F).floatValue();
            double maxRadius = this.getSetting("Ring Radius", 2.5F).floatValue();
            int points = this.getSetting("Ring Points", 40);
            boolean ringThroughWalls = this.getSetting("Ring Through Walls", true);
            int rgb = ThemeColors.resolveRgb(this.getSetting("Ring Color", ""), -50373, false);
            long now = System.nanoTime();
            this.rings.removeIf(rx -> (now - rx.time()) / 1.0E9 > ttl);

            for (HitParticles.Ring r : this.rings) {
               double elapsed = (now - r.time()) / 1.0E9;
               double radius = speed * elapsed;
               if (!(radius > maxRadius)) {
                  float alpha = (float)(1.0 - radius / maxRadius);
                  int argb = Math.round(alpha * 220.0F) << 24 | rgb & 16777215;
                  ClarityRenderUtils.drawGroundRing(poseStack, r.origin(), radius, argb, ringThroughWalls, points);
               }
            }
         }
      }
   }

   private static ParticleOptions resolveParticle(String name, int rgb, float size) {
      if (name != null && !"None".equalsIgnoreCase(name)) {
         switch (name) {
            case "Enchanted":
               return ParticleTypes.ENCHANTED_HIT;
            case "Enchant":
               return ParticleTypes.ENCHANT;
            case "Flame":
               return ParticleTypes.FLAME;
            case "Soul Flame":
               return ParticleTypes.SOUL_FIRE_FLAME;
            case "Heart":
               return ParticleTypes.HEART;
            case "Smoke":
               return ParticleTypes.SMOKE;
            case "Large Smoke":
               return ParticleTypes.LARGE_SMOKE;
            case "Cloud":
               return ParticleTypes.CLOUD;
            case "Electric":
               return ParticleTypes.ELECTRIC_SPARK;
            case "Lava":
               return ParticleTypes.LAVA;
            case "Snowflake":
               return ParticleTypes.SNOWFLAKE;
            case "Totem":
               return ParticleTypes.TOTEM_OF_UNDYING;
            case "Note":
               return ParticleTypes.NOTE;
            case "Dust":
               return new DustParticleOptions(rgb & 16777215, size);
            case "Crit":
            default:
               return ParticleTypes.CRIT;
         }
      } else {
         return null;
      }
   }

   private record Ring(Vec3 origin, long time) {
   }
}
