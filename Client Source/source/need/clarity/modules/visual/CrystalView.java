package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import need.clarity.PerPacketEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import need.clarity.render.CrystalRenderer;
import need.clarity.util.ClaritySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;

public class CrystalView extends ModuleBase {
   public static CrystalView INSTANCE;
   public final Map<Integer, CrystalView.Track> tracks = new HashMap<>();
   public final List<CrystalView.Pop> pops = new java.util.ArrayList<>();
   private final ConcurrentLinkedQueue<CrystalView.Boom> explosions = new ConcurrentLinkedQueue<>();
   private final List<CrystalView.Recent> recent = new java.util.ArrayList<>();
   private static final long RECENT_NANOS = 1000000000L;
   private static final long UNPOP_NANOS = 450000000L;
   private final WorldRenderEvent.Listener listener = this::onRender;
   private boolean registered = false;
   private boolean firstFrame = true;
   public static final Set<String> VANILLA_STYLES = Set.of("Energy Edges", "Hologram", "Galaxy", "Glint", "Aurora", "Molten", "Frost", "Prismatic", "Pulse", "Digital", "Void", "Electric", "Inferno");

   public CrystalView() {
      super("Crystal View", "Visual");
      INSTANCE = this;
      PerPacketEvents.register(this::onPacket);
      if (this.isEnabled()) {
         this.onEnable();
      }
   }

   public boolean vanillaModel() {
      return VANILLA_STYLES.contains(this.style());
   }

   public static boolean active() {
      CrystalView c = INSTANCE;
      return c != null && c.isEnabled() && !c.vanillaModel();
   }

   public static boolean isCrystalExplosion(double x, double y, double z) {
      CrystalView cv = INSTANCE;
      Minecraft mc = Minecraft.getInstance();
      if (cv != null && mc.level != null) {
         double r2 = 6.25;

         for (CrystalView.Track t : cv.tracks.values()) {
            if (t.entity.distanceToSqr(x, y, z) < 6.25) {
               return true;
            }
         }

         long now = System.nanoTime();
         int i = 0;

         for (int n = cv.recent.size(); i < n; i++) {
            CrystalView.Recent r = cv.recent.get(i);
            double dx = r.x - x;
            double dy = r.y - y;
            double dz = r.z - z;
            if (now - r.nanos < 800000000L && dx * dx + dy * dy + dz * dz < 6.25) {
               return true;
            }
         }

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof EndCrystal && e.distanceToSqr(x, y, z) < 6.25) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean hideParticlesAt(double x, double y, double z) {
      CrystalView cv = INSTANCE;
      return cv != null && cv.isEnabled() && cv.getSetting("Hide Vanilla Particles", true) ? isCrystalExplosion(x, y, z) : false;
   }

   @Override
   public void onEnable() {
      if (!this.registered) {
         WorldRenderEvent.register(this.listener);
         this.registered = true;
      }

      this.firstFrame = true;
   }

   @Override
   public void onDisable() {
      if (this.registered) {
         WorldRenderEvent.unregister(this.listener);
         this.registered = false;
      }

      this.tracks.clear();
      this.pops.clear();
      this.recent.clear();
      this.explosions.clear();
   }

   private void onPacket(EventPacketReceive e) {
      if (this.isEnabled()) {
         if (e.getPacket() instanceof ClientboundExplodePacket p) {
            Vec3 c = p.center();
            this.explosions.add(new CrystalView.Boom(c.x, c.y, c.z, System.nanoTime()));
         }
      }
   }

   private void onRender(PoseStack poseStack, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         long now = System.nanoTime();
         boolean animateIn = !this.firstFrame && this.getSetting("Spawn Animation", true);
         boolean bobOn = this.getSetting("Bob", true);
         float spinSpeed = this.getSetting("Spin Speed", 1.0F);

         for (CrystalView.Track t : this.tracks.values()) {
            t.seen = false;
         }

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof EndCrystal ec) {
               CrystalView.Track t = this.tracks.get(ec.getId());
               if (t == null || t.entity != ec) {
                  t = new CrystalView.Track(ec, now, animateIn);
                  this.tracks.put(ec.getId(), t);
               }

               t.seen = true;
               double x = Mth.lerp(partialTick, ec.xo, ec.getX());
               double y = Mth.lerp(partialTick, ec.yo, ec.getY());
               double z = Mth.lerp(partialTick, ec.zo, ec.getZ());
               float age = ec.time + partialTick;
               t.age = age;
               float f = Mth.sin(age * 0.2F) / 2.0F + 0.5F;
               float bob = bobOn ? (f * f + f) * 0.4F * 0.55F : 0.2F;
               t.fx = x;
               t.fy = y;
               t.fz = z;
               t.cx = x;
               t.cy = y + 1.2 + bob;
               t.cz = z;
               t.spin = age * 0.0523599F * spinSpeed + t.seed * 6.2832F;
               if (t.popped && !ec.isRemoved() && now - t.popNanos > 450000000L) {
                  t.popped = false;
               }
            }
         }

         this.firstFrame = false;

         CrystalView.Boom ex;
         while ((ex = this.explosions.poll()) != null) {
            if (!this.absorbByRecent(ex)) {
               CrystalView.Track best = null;
               double bestD = 1.0;

               for (CrystalView.Track t : this.tracks.values()) {
                  if (!t.popped && t.bornNanos <= ex.nanos()) {
                     double dx = t.fx - ex.x();
                     double dy = t.fy - ex.y();
                     double dz = t.fz - ex.z();
                     double d = dx * dx + dy * dy + dz * dz;
                     if (d < bestD) {
                        bestD = d;
                        best = t;
                     }
                  }
               }

               if (best != null) {
                  this.pop(best, now, true);
               }
            }
         }

         Iterator<CrystalView.Track> it = this.tracks.values().iterator();

         while (it.hasNext()) {
            CrystalView.Track t = it.next();
            if (!t.seen || t.entity.isRemoved()) {
               RemovalReason why = t.entity.getRemovalReason();
               boolean unloaded = why == RemovalReason.UNLOADED_TO_CHUNK || why == RemovalReason.UNLOADED_WITH_PLAYER || why == RemovalReason.CHANGED_DIMENSION;
               if (!t.popped && !unloaded && mc.player.distanceToSqr(t.fx, t.fy, t.fz) < 16384.0) {
                  this.pop(t, now, false);
               }

               it.remove();
            }
         }

         float dur = this.popDuration();
         long durNanos = (long)(dur * 1.0E9);
         this.pops.removeIf(p -> now - p.nanos > durNanos);
         this.recent.removeIf(r -> now - r.nanos > 1000000000L);
         CrystalRenderer.draw(this, now);
      } else {
         this.tracks.clear();
         this.pops.clear();
         this.recent.clear();
      }
   }

   private boolean absorbByRecent(CrystalView.Boom ex) {
      CrystalView.Recent best = null;
      double bestD = 1.0;
      int i = 0;

      for (int n = this.recent.size(); i < n; i++) {
         CrystalView.Recent r = this.recent.get(i);
         if (!r.claimed && r.nanos <= ex.nanos() + 50000000L) {
            double dx = r.x - ex.x();
            double dy = r.y - ex.y();
            double dz = r.z - ex.z();
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) {
               bestD = d;
               best = r;
            }
         }
      }

      if (best == null) {
         return false;
      }

      best.claimed = true;
      return true;
   }

   private void pop(CrystalView.Track t, long now, boolean fromExplosion) {
      t.popped = true;
      t.popNanos = now;
      this.recent.add(new CrystalView.Recent(t.fx, t.fy, t.fz, now, fromExplosion));
      if (this.getSetting("Pop Effect", true)) {
         this.pops.add(new CrystalView.Pop(t, now));
      }

      this.playPopSound(t.fx, t.fy, t.fz);
   }

   private void playPopSound(double x, double y, double z) {
      String s = this.getSetting("Pop Sound", "Shatter");

      SoundEvent ev = switch (s) {
         case "Shatter" -> ClaritySounds.raw("kill.shatter");
         case "Crystal" -> ClaritySounds.raw("hit.crystal");
         case "Glass" -> ClaritySounds.raw("hit.glass2");
         case "Kill Pop" -> ClaritySounds.raw("kill.killpop");
         case "Chime" -> SoundEvents.AMETHYST_BLOCK_RESONATE;
         case "Amethyst" -> SoundEvents.AMETHYST_CLUSTER_BREAK;
         default -> null;
      };
      if (ev != null) {
         Minecraft mc = Minecraft.getInstance();
         float vol = this.getSetting("Volume", 1.0F);
         float pitch = 0.94F + (float)Math.random() * 0.12F;
         mc.level.playLocalSound(x, y, z, ev, SoundSource.BLOCKS, vol, pitch, false);
      }
   }

   public static boolean shouldMute(SoundInstance sound) {
      CrystalView cv = INSTANCE;
      if (cv != null && cv.isEnabled() && cv.getSetting("Mute Vanilla Boom", false)) {
         try {
            return !"entity.generic.explode".equals(sound.getIdentifier().getPath()) ? false : isCrystalExplosion(sound.getX(), sound.getY(), sound.getZ());
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public String style() {
      return this.getSetting("Style", "Prism Gem");
   }

   public String colorMode() {
      return this.getSetting("Color Mode", "Theme");
   }

   public String color() {
      return this.getSetting("Color", "#7AA2F7");
   }

   public String color2() {
      return this.getSetting("Second Colour", "#3D59A1");
   }

   public float size() {
      return this.getSetting("Size", 1.0F);
   }

   public float glow() {
      return this.getSetting("Glow", 1.0F);
   }

   public boolean walls() {
      return this.getSetting("Through Walls", false);
   }

   public boolean groundRing() {
      return this.getSetting("Ground Ring", true);
   }

   public float popDuration() {
      return this.getSetting("Pop Duration", 0.7F);
   }

   public float popSize() {
      return this.getSetting("Pop Size", 1.0F);
   }

   public boolean explosionPop() {
      return "Explosion".equals(this.getSetting("Pop Style", "Afterimage"));
   }

   private record Boom(double x, double y, double z, long nanos) {
   }

   public static final class Pop {
      public final double cx;
      public final double cy;
      public final double cz;
      public final double fx;
      public final double fy;
      public final double fz;
      public final long nanos;
      public final float spin;
      public final float seed;
      public final float age;

      Pop(CrystalView.Track t, long now) {
         this.cx = t.cx;
         this.cy = t.cy;
         this.cz = t.cz;
         this.fx = t.fx;
         this.fy = t.fy;
         this.fz = t.fz;
         this.nanos = now;
         this.spin = t.spin;
         this.seed = t.seed;
         this.age = t.age;
      }
   }

   private static final class Recent {
      final double x;
      final double y;
      final double z;
      final long nanos;
      boolean claimed;

      Recent(double x, double y, double z, long nanos, boolean claimed) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.nanos = nanos;
         this.claimed = claimed;
      }
   }

   public static final class Track {
      public EndCrystal entity;
      public long spawnNanos;
      public double cx;
      public double cy;
      public double cz;
      public double fx;
      public double fy;
      public double fz;
      public float spin;
      public float age;
      public boolean popped;
      public boolean seen;
      public long popNanos;
      public final long bornNanos;
      public final float seed;

      Track(EndCrystal e, long now, boolean animateIn) {
         this.entity = e;
         this.bornNanos = now;
         this.spawnNanos = animateIn ? now : now - 10000000000L;
         this.seed = (float)(e.getId() * 0.618034 % 1.0);
      }
   }
}
