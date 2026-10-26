package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.render.ImageParticles;
import need.clarity.render.worldfx.GpuFx;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public class Particles extends ModuleBase {
   public static Particles INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private final long startNanos = System.nanoTime();
   private double clock = 0.0;
   private long lastNanos = 0L;

   public Particles() {
      super("Particles", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         WorldRenderEvent.register(this.renderListener);
      }
   }

   @Override
   public void onEnable() {
      WorldRenderEvent.register(this.renderListener);
   }

   @Override
   public void onDisable() {
      WorldRenderEvent.unregister(this.renderListener);
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         int style = GpuFx.skyStyle(this.getSetting("Particle", "Snowflake"));
         if (style >= 0) {
            long now = System.nanoTime();
            double dt = this.lastNanos == 0L ? 0.0 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
            this.lastNanos = now;
            this.clock = (this.clock + dt) % 3600.0;
            float fallSpeed = this.getSetting("Fall Speed", 0.05F);
            int rgb1 = ThemeColors.resolveRgb(this.getSetting("Colour", "#8FD3FF"), 9425919, false) & 16777215;
            int rgb2 = ThemeColors.resolveRgb(this.getSetting("Second Colour", "#FF9AE6"), 16751334, true) & 16777215;
            GpuFx.sky(
               style,
               Math.max(100, Math.min(12000, this.getSetting("Density", 2500))),
               this.getSetting("Radius", 16.0F),
               this.getSetting("Height", 8.0F),
               fallSpeed / 0.05F,
               this.getSetting("Particle Size", 1.0F),
               rgb(rgb1),
               rgb(rgb2),
               this.getSetting("Natural Colors", true),
               Math.max(0.05F, Math.min(1.0F, this.getSetting("Opacity", 1.0F))),
               this.getSetting("Land On Blocks", true),
               (float)this.clock
            );
         }
      }
   }

   private static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         String mode = this.getSetting("Particle", "Snowflake");
         if (GpuFx.skyStyle(mode) >= 0) {
            if (this.getSetting("Land On Blocks", true)) {
               GpuFx.tickHeightmap();
            }
         } else {
            String imageTex = ImageParticles.textureFor(mode);
            ParticleOptions particle = imageTex == null ? this.resolveParticle() : null;
            if (imageTex != null || particle != null) {
               int rate = this.getSetting("Rate", 4);
               float radius = this.getSetting("Radius", 16.0F);
               float height = this.getSetting("Height", 8.0F);
               float speed = this.getSetting("Fall Speed", 0.05F);
               double px = mc.player.getX();
               double py = mc.player.getY();
               double pz = mc.player.getZ();
               float imgSize = this.getSetting("Image Size", 0.18F);
               int rgb = ThemeColors.resolveRgb(this.getSetting("Image Colour", "#FFFFFF"), 16777215, false) & 16777215;
               float life = (float)Math.max(0.5, Math.min(10.0, (height + 1.0) / Math.max(0.01, speed * 20.0)));

               for (int i = 0; i < rate; i++) {
                  double ox = (Math.random() - 0.5) * 2.0 * radius;
                  double oz = (Math.random() - 0.5) * 2.0 * radius;
                  double oy = py + Math.random() * height;
                  if (imageTex != null) {
                     double sway = 0.25;
                     ImageParticles.spawn(
                        imageTex,
                        px + ox,
                        oy,
                        pz + oz,
                        (Math.random() - 0.5) * sway,
                        -speed * 20.0,
                        (Math.random() - 0.5) * sway,
                        life,
                        imgSize * (0.7F + (float)Math.random() * 0.6F),
                        0.0F,
                        rgb,
                        true
                     );
                  } else {
                     mc.level.addParticle(particle, px + ox, oy, pz + oz, 0.0, -speed, 0.0);
                  }
               }
            }
         }
      }
   }

   private ParticleOptions resolveParticle() {
      return switch ((String)this.getSetting("Particle", "Snowflake")) {
         case "Dust" -> ParticleTypes.CRIT;
         case "Flame" -> ParticleTypes.FLAME;
         case "Soul Flame" -> ParticleTypes.SOUL_FIRE_FLAME;
         case "Heart" -> ParticleTypes.HEART;
         case "Cloud" -> ParticleTypes.CLOUD;
         case "Electric" -> ParticleTypes.ELECTRIC_SPARK;
         case "Lava" -> ParticleTypes.LAVA;
         case "Note" -> ParticleTypes.NOTE;
         case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
         case "End Rod" -> ParticleTypes.END_ROD;
         case "None" -> null;
         default -> ParticleTypes.SNOWFLAKE;
      };
   }
}
