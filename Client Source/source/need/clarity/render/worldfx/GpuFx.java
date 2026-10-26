package need.clarity.render.worldfx;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

public final class GpuFx {
   public static final String[] SKY = new String[]{
      "Snow", "Rain", "Sakura", "Embers", "Fireflies", "Starfall", "Leaves", "Hearts", "Bubbles", "Aurora", "Galaxy", "Plasma", "Butterflies", "Ash", "Matrix", "Crystals"
   };
   public static final String[] HIT = new String[]{"Sparks", "Stars", "Hearts", "Shards", "Blood", "Magic", "Nova", "Confetti"};
   private static final float[] HIT_LIFE = new float[]{0.6F, 0.9F, 1.2F, 0.9F, 0.8F, 1.1F, 0.7F, 1.4F};
   private static final int HM = 96;
   private static final float[] heights = new float[9216];
   private static int hmOriginX;
   private static int hmOriginZ;
   private static int hmBaseY;
   private static boolean hmValid = false;
   private static boolean hmDirty = false;
   private static long hmBuiltAt = 0L;
   private static int hmTex = 0;
   private static FloatBuffer hmBuf;
   private static final List<GpuFx.Burst> bursts = new ArrayList<>();
   private static final float[] uBurst = new float[128];
   private static final float[] uInfo = new float[128];
   private static int burstCounter = 0;

   private GpuFx() {
   }

   public static int skyStyle(String n) {
      return indexOf(SKY, n);
   }

   public static int hitStyle(String n) {
      return indexOf(HIT, n);
   }

   public static void tickHeightmap() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         int cx = (int)Math.floor(cam.x);
         int cz = (int)Math.floor(cam.z);
         long now = System.nanoTime();
         boolean far = Math.abs(cx - (hmOriginX + 48)) > 10 || Math.abs(cz - (hmOriginZ + 48)) > 10;
         if (!hmValid || far || now - hmBuiltAt >= 2000000000L) {
            hmOriginX = cx - 48;
            hmOriginZ = cz - 48;
            hmBaseY = (int)Math.floor(cam.y);

            for (int z = 0; z < 96; z++) {
               for (int x = 0; x < 96; x++) {
                  int h = mc.level.getHeight(Types.MOTION_BLOCKING, hmOriginX + x, hmOriginZ + z);
                  heights[z * 96 + x] = h - hmBaseY;
               }
            }

            hmValid = true;
            hmDirty = true;
            hmBuiltAt = now;
         }
      }
   }

   private static int heightTexture() {
      if (!hmValid) {
         return 0;
      }

      if (hmTex != 0 && !hmDirty) {
         return hmTex;
      }

      if (hmBuf == null) {
         hmBuf = MemoryUtil.memAllocFloat(9216);
      }

      hmBuf.clear();
      hmBuf.put(heights).flip();
      GlStateManager._activeTexture(33984);
      int prev = GL11.glGetInteger(32873);
      int prevPbo = GL11.glGetInteger(35055);
      int rowLen = GL11.glGetInteger(3314);
      int skipP = GL11.glGetInteger(3316);
      int skipR = GL11.glGetInteger(3315);
      int align = GL11.glGetInteger(3317);
      GL15.glBindBuffer(35052, 0);
      GL11.glPixelStorei(3314, 0);
      GL11.glPixelStorei(3316, 0);
      GL11.glPixelStorei(3315, 0);
      GL11.glPixelStorei(3317, 4);
      boolean create = hmTex == 0;
      if (create) {
         hmTex = GL11.glGenTextures();
      }

      GlStateManager._bindTexture(hmTex);
      if (create) {
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 33085, 0);
         GL11.glTexImage2D(3553, 0, 33326, 96, 96, 0, 6403, 5126, hmBuf);
      } else {
         GL11.glTexSubImage2D(3553, 0, 0, 0, 96, 96, 6403, 5126, hmBuf);
      }

      GL11.glPixelStorei(3314, rowLen);
      GL11.glPixelStorei(3316, skipP);
      GL11.glPixelStorei(3315, skipR);
      GL11.glPixelStorei(3317, align);
      GL15.glBindBuffer(35052, prevPbo);
      GlStateManager._bindTexture(prev);
      hmDirty = false;
      return hmTex;
   }

   public static void sky(int style, int count, float radius, float height, float speed, float size, float[] c1, float[] c2, boolean natural, float opacity, boolean landOnBlocks, float time) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      float W = Math.max(4.0F, Math.min(radius, 46.0F) * 2.0F);
      float H = Math.max(6.0F, height * 2.5F);
      float mx = (float)mod(cam.x, W);
      float my = (float)mod(cam.y, H);
      float mz = (float)mod(cam.z, W);
      int tex = landOnBlocks ? heightTexture() : 0;
      boolean useH = tex != 0;
      float offX = (float)(cam.x - hmOriginX);
      float offZ = (float)(cam.z - hmOriginZ);
      float offY = (float)(cam.y - hmBaseY);
      WorldFx.drawInstanced("weather", count, false, tex, s -> {
         s.setUniform1f("uTime", time);
         s.setUniform2f("uBox", W, H);
         s.setUniform3f("uCamMod", mx, my, mz);
         s.setUniform1i("uStyle", style);
         s.setUniform1f("uSize", size);
         s.setUniform1f("uSpeed", speed);
         s.setUniform1i("uHeight", 0);
         s.setUniform2f("uHmOff", offX, offZ);
         s.setUniform1f("uHmCamY", offY);
         s.setUniform1i("uUseHeight", useH ? 1 : 0);
         s.setUniform1i("uHmSize", 96);
         s.setUniform3f("uColor1", c1[0], c1[1], c1[2]);
         s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
         s.setUniform1i("uNatural", natural ? 1 : 0);
         s.setUniform1f("uOpacity", opacity);
      });
   }

   public static void addBurst(int style, double x, double y, double z, float scale) {
      float life = HIT_LIFE[Math.max(0, Math.min(HIT_LIFE.length - 1, style))];
      float seed = burstCounter++ * 0.6180339F % 1.0F;
      bursts.add(new GpuFx.Burst(x, y, z, System.nanoTime(), seed, scale, life));

      while (bursts.size() > 32) {
         bursts.remove(0);
      }
   }

   public static boolean hasBursts() {
      return !bursts.isEmpty();
   }

   public static void renderBursts(int style, int perBurst, float size, float[] c1, float[] c2, boolean natural, boolean throughWalls) {
      long now = System.nanoTime();
      bursts.removeIf(bx -> (now - bx.t) / 1.0E9 > bx.life);
      if (!bursts.isEmpty()) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         int n = bursts.size();
         Arrays.fill(uBurst, 0.0F);
         Arrays.fill(uInfo, 0.0F);

         for (int i = 0; i < n; i++) {
            GpuFx.Burst b = bursts.get(i);
            uBurst[i * 4] = (float)(b.x - cam.x);
            uBurst[i * 4 + 1] = (float)(b.y - cam.y);
            uBurst[i * 4 + 2] = (float)(b.z - cam.z);
            uBurst[i * 4 + 3] = (float)((now - b.t) / 1.0E9);
            uInfo[i * 4] = b.seed;
            uInfo[i * 4 + 1] = b.scale;
            uInfo[i * 4 + 2] = b.life;
         }

         int per = Math.max(4, Math.min(64, perBurst));
         WorldFx.drawInstanced("burst", n * per, throughWalls, 0, s -> {
            s.setUniform4fv("uBurst", uBurst);
            s.setUniform4fv("uInfo", uInfo);
            s.setUniform1i("uPer", per);
            s.setUniform1i("uStyle", style);
            s.setUniform1f("uSize", size);
            s.setUniform3f("uColor1", c1[0], c1[1], c1[2]);
            s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
            s.setUniform1i("uNatural", natural ? 1 : 0);
         });
      }
   }

   public static void auraParticles(
      int mode, double fx, double fy, double fz, float height, float radius, int count, float size, float[] c1, float[] c2, float opacity, float time, boolean throughWalls
   ) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      int tail = 14;
      float cx = (float)(fx - cam.x);
      float cy = (float)(fy - cam.y);
      float cz = (float)(fz - cam.z);
      int instances = mode == 0 ? count * 14 : count;
      WorldFx.drawInstanced("aurap", instances, throughWalls, 0, s -> {
         s.setUniform3f("uCenter", cx, cy, cz);
         s.setUniform1f("uHeight", height);
         s.setUniform1f("uRadius", radius);
         s.setUniform1f("uTime", time);
         s.setUniform1f("uSize", size);
         s.setUniform1i("uMode", mode);
         s.setUniform1i("uTail", 14);
         s.setUniform3f("uColor1", c1[0], c1[1], c1[2]);
         s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
         s.setUniform1f("uOpacity", opacity);
      });
   }

   private static double mod(double a, double m) {
      double r = a % m;
      return r < 0.0 ? r + m : r;
   }

   private static int indexOf(String[] arr, String v) {
      if (v != null) {
         for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(v)) {
               return i;
            }
         }
      }

      return -1;
   }

   private record Burst(double x, double y, double z, long t, float seed, float scale, float life) {
   }
}
