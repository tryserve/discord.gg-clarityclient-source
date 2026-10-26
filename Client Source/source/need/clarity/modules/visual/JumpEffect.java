package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.render.worldfx.WorldFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class JumpEffect extends ModuleBase {
   public static JumpEffect INSTANCE;
   public static final String[] CIRCLES = new String[]{"Ripple", "Shockwave", "Rune", "Bloom", "Corona", "Vortex", "Spectrum"};
   private final List<JumpEffect.Circle> circles = new java.util.ArrayList<>();
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private boolean wasOnGround = true;
   private int airTicks = 0;
   private int circleCount = 0;

   public JumpEffect() {
      super("Jump Effect", "Visual");
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
      this.wasOnGround = true;
      this.circles.clear();
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

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         Player p = mc.player;
         boolean onGround = p.onGround();
         String trigger = this.getSetting("Trigger", "Jump");
         boolean jumped = !onGround && this.wasOnGround && p.getDeltaMovement().y > 0.0;
         boolean landed = onGround && !this.wasOnGround && this.airTicks > 3;
         this.airTicks = onGround ? 0 : this.airTicks + 1;
         this.wasOnGround = onGround;
         boolean fire = jumped && !"Land".equals(trigger) || landed && !"Jump".equals(trigger);
         if (fire) {
            if (!this.getSetting("Only Sprinting", false) || p.isSprinting() || landed) {
               if (this.circleStyle() >= 0) {
                  float seed = this.circleCount++ * 0.6180339F % 1.0F;
                  this.circles.add(new JumpEffect.Circle(new Vec3(p.getX(), p.getY(), p.getZ()), System.nanoTime(), seed));
                  if (this.circles.size() > 32) {
                     this.circles.remove(0);
                  }
               } else {
                  this.spawnRing(mc, p);
               }
            }
         }
      }
   }

   private int circleStyle() {
      String m = this.getSetting("Particle", "Ripple");
      if ("Jump Circle".equals(m)) {
         return 0;
      }

      for (int i = 0; i < CIRCLES.length; i++) {
         if (CIRCLES[i].equals(m)) {
            return i;
         }
      }

      return -1;
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled() && !this.circles.isEmpty()) {
         double ttl = Math.max(0.05, this.getSetting("Circle Duration", 0.9F).floatValue());
         float size = Math.max(0.1F, this.getSetting("Circle Size", 1.6F));
         int rgb = ThemeColors.resolveRgb(this.getSetting("Circle Colour", "#6FC3FF"), 7324671, false) & 16777215;
         int rgb2 = ThemeColors.resolveRgb(this.getSetting("Circle Colour 2", "#C77DFF"), 13073919, true) & 16777215;
         boolean through = this.getSetting("Circle Through Walls", false);
         int style = Math.max(0, this.circleStyle());
         long now = System.nanoTime();
         this.circles.removeIf(c -> (now - c.time()) / 1.0E9 > ttl);
         if (!this.circles.isEmpty()) {
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            float r = (rgb >> 16 & 0xFF) / 255.0F;
            float g = (rgb >> 8 & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;
            WorldFx.begin();

            for (JumpEffect.Circle c : this.circles) {
               float prog = (float)Math.min(1.0, (now - c.time()) / 1.0E9 / ttl);
               float x = (float)(c.origin().x - cam.x);
               float y = (float)(c.origin().y + 0.02 - cam.y);
               float z = (float)(c.origin().z - cam.z);
               float e = 1.25F;
               float s = size * 1.25F;
               WorldFx.vertex(x - s, y, z - s, -1.25F, -1.25F, r, g, b, 1.0F, prog, c.seed());
               WorldFx.vertex(x + s, y, z - s, 1.25F, -1.25F, r, g, b, 1.0F, prog, c.seed());
               WorldFx.vertex(x + s, y, z + s, 1.25F, 1.25F, r, g, b, 1.0F, prog, c.seed());
               WorldFx.vertex(x - s, y, z - s, -1.25F, -1.25F, r, g, b, 1.0F, prog, c.seed());
               WorldFx.vertex(x + s, y, z + s, 1.25F, 1.25F, r, g, b, 1.0F, prog, c.seed());
               WorldFx.vertex(x - s, y, z + s, -1.25F, 1.25F, r, g, b, 1.0F, prog, c.seed());
            }

            float time = (float)(now / 1.0E9 % 3600.0);
            float r2 = (rgb2 >> 16 & 0xFF) / 255.0F;
            float g2 = (rgb2 >> 8 & 0xFF) / 255.0F;
            float b2 = (rgb2 & 0xFF) / 255.0F;
            WorldFx.flush("jumpring", through, sh -> {
               sh.setUniform1f("uTime", time);
               sh.setUniform1i("uStyle", style);
               sh.setUniform3f("uColor2", r2, g2, b2);
            });
         }
      }
   }

   private void spawnRing(Minecraft mc, Player p) {
      ParticleOptions particle = this.resolveParticle();
      if (particle != null) {
         float radius = this.getSetting("Radius", 0.6F);
         int points = this.getSetting("Points", 16);
         double x = p.getX();
         double y = p.getY() + 0.05;
         double z = p.getZ();

         for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2) / points * i;
            double dx = Math.cos(a) * radius;
            double dz = Math.sin(a) * radius;
            mc.level.addParticle(particle, x + dx, y, z + dz, 0.0, 0.02, 0.0);
         }
      }
   }

   private ParticleOptions resolveParticle() {
      String name = this.getSetting("Particle", "Cloud");

      return switch (name) {
         case "End Rod" -> ParticleTypes.END_ROD;
         case "Crit" -> ParticleTypes.CRIT;
         case "Flame" -> ParticleTypes.FLAME;
         case "Soul Flame" -> ParticleTypes.SOUL_FIRE_FLAME;
         case "Happy Villager" -> ParticleTypes.HAPPY_VILLAGER;
         default -> ParticleTypes.CLOUD;
      };
   }

   private record Circle(Vec3 origin, long time, float seed) {
   }
}
