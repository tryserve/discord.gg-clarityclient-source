package need.clarity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.WorldRenderEvent;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class ImageParticles {
   public static final String GLOW = "particle_glow";
   public static final String SNOWFLAKE = "particle_snowflake";
   private static final int MAX = 3000;
   private static final List<ImageParticles.P> LIST = new ArrayList<>();
   private static final WorldRenderEvent.Listener LISTENER = ImageParticles::onRender;
   private static boolean registered = false;
   private static long lastNanos = 0L;

   private ImageParticles() {
   }

   public static String textureFor(String mode) {
      if (mode == null) {
         return null;
      }

      return switch (mode) {
         case "Image Glow" -> "particle_glow";
         case "Image Snowflake" -> "particle_snowflake";
         default -> null;
      };
   }

   public static void spawn(String tex, double x, double y, double z, double vx, double vy, double vz, float life, float size, float gravity, int rgb, boolean collide) {
      if (LIST.size() < 3000) {
         if (!registered) {
            registered = true;
            WorldRenderEvent.register(LISTENER);
         }

         ImageParticles.P p = new ImageParticles.P();
         p.x = x;
         p.y = y;
         p.z = z;
         p.vx = vx;
         p.vy = vy;
         p.vz = vz;
         p.life = Math.max(0.05F, life);
         p.size = size;
         p.gravity = gravity;
         p.rgb = rgb & 16777215;
         p.tex = tex;
         p.collide = collide;
         LIST.add(p);
      }
   }

   public static void clear() {
      LIST.clear();
   }

   private static void onRender(PoseStack poseStack, float partialTick) {
      if (LIST.isEmpty()) {
         lastNanos = 0L;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.0F : (float)(now - lastNanos) / 1.0E9F;
            lastNanos = now;
            if (dt > 0.1F) {
               dt = 0.1F;
            }

            for (int i = LIST.size() - 1; i >= 0; i--) {
               ImageParticles.P p = LIST.get(i);
               p.age += dt;
               if (p.age >= p.life) {
                  LIST.remove(i);
               } else {
                  p.vy = p.vy - p.gravity * dt;
                  p.x = p.x + p.vx * dt;
                  p.y = p.y + p.vy * dt;
                  p.z = p.z + p.vz * dt;
                  if (p.collide && !mc.level.getBlockState(BlockPos.containing(p.x, p.y, p.z)).isAir()) {
                     LIST.remove(i);
                  }
               }
            }

            if (!LIST.isEmpty()) {
               Vec3 cam = ClarityRenderUtils.getCameraPos();
               Map<String, List<ImageParticles.P>> byTex = new HashMap<>();

               for (ImageParticles.P p : LIST) {
                  byTex.computeIfAbsent(p.tex, k -> new ArrayList<>()).add(p);
               }

               for (Entry<String, List<ImageParticles.P>> e : byTex.entrySet()) {
                  Identifier id = FxTextures.get(e.getKey());
                  if (id != null) {
                     List<ImageParticles.P> ps = e.getValue();
                     ps.sort(Comparator.comparingDouble(px -> -distSq(px, cam)));
                     ClarityRenderUtils.TexBatch batch = ClarityRenderUtils.beginTextured(poseStack, id);

                     for (ImageParticles.P p : ps) {
                        float fadeIn = Math.min(1.0F, p.age / 0.15F);
                        float fadeOut = Math.min(1.0F, (p.life - p.age) / (p.life * 0.35F));
                        float a = Math.max(0.0F, Math.min(fadeIn, fadeOut));
                        int argb = Math.round(a * 255.0F) << 24 | p.rgb;
                        double h = p.size * 0.5;
                        batch.billboard(new Vec3(p.x, p.y, p.z), h, h, 0.0, argb);
                     }

                     batch.end();
                  }
               }
            }
         } else {
            LIST.clear();
         }
      }
   }

   private static double distSq(ImageParticles.P p, Vec3 cam) {
      double dx = p.x - cam.x;
      double dy = p.y - cam.y;
      double dz = p.z - cam.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static final class P {
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      float age;
      float life;
      float size;
      float gravity;
      int rgb;
      String tex;
      boolean collide;
   }
}
