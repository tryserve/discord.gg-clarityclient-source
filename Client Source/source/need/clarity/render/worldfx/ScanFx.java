package need.clarity.render.worldfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ScanFx {
   private static final int MAX_BLOCKS = 14000;
   private static final int MAX_DRAWN = 2600;
   private static final float TRAIL = 1.6F;
   private static final List<ScanFx.Scan> scans = new ArrayList<>();

   private ScanFx() {
   }

   public static void clear() {
      scans.clear();
   }

   public static void spawn(double x, double y, double z, float range, float speed) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null) {
         range = Math.max(2.0F, Math.min(40.0F, range));
         int R = (int)Math.ceil(range);
         int cx = (int)Math.floor(x);
         int cy = (int)Math.floor(y);
         int cz = (int)Math.floor(z);
         List<float[]> found = new ArrayList<>();
         MutableBlockPos p = new MutableBlockPos();
         MutableBlockPos q = new MutableBlockPos();
         int[][] nb = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
         double r2 = (double)range * range;

         for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
               for (int dz = -R; dz <= R; dz++) {
                  double d2 = (dx + 0.5) * (dx + 0.5) + (dy + 0.5) * (dy + 0.5) + (dz + 0.5) * (dz + 0.5);
                  if (!(d2 > r2)) {
                     p.set(cx + dx, cy + dy, cz + dz);
                     BlockState st = level.getBlockState(p);
                     if (!st.isAir()) {
                        int mask = 0;

                        for (int f = 0; f < 6; f++) {
                           int[] o = nb[f];
                           q.set(p.getX() + o[0], p.getY() + o[1], p.getZ() + o[2]);
                           if (!level.getBlockState(q).canOcclude()) {
                              mask |= 1 << f;
                           }
                        }

                        if (mask != 0) {
                           VoxelShape shape = st.getShape(level, p);
                           if (!shape.isEmpty()) {
                              AABB b = shape.bounds();
                              float bxMin = (float)(p.getX() + b.minX);
                              float byMin = (float)(p.getY() + b.minY);
                              float bzMin = (float)(p.getZ() + b.minZ);
                              float bxMax = (float)(p.getX() + b.maxX);
                              float byMax = (float)(p.getY() + b.maxY);
                              float bzMax = (float)(p.getZ() + b.maxZ);
                              double ddx = (bxMin + bxMax) * 0.5 - x;
                              double ddy = (byMin + byMax) * 0.5 - y;
                              double ddz = (bzMin + bzMax) * 0.5 - z;
                              found.add(new float[]{bxMin, byMin, bzMin, bxMax, byMax, bzMax, (float)Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz), mask});
                           }
                        }
                     }
                  }
               }
            }
         }

         found.sort((a, bxx) -> Float.compare(a[6], bxx[6]));
         int n = Math.min(found.size(), 14000);
         float[] bx = new float[n * 7];
         float[] dist = new float[n];

         for (int i = 0; i < n; i++) {
            float[] f = found.get(i);
            System.arraycopy(f, 0, bx, i * 7, 6);
            bx[i * 7 + 6] = f[7];
            dist[i] = f[6];
         }

         scans.add(new ScanFx.Scan(x, y, z, range, Math.max(1.0F, speed), System.nanoTime(), bx, dist, n));
      }
   }

   public static boolean isEmpty() {
      return scans.isEmpty();
   }

   public static void render(float partialTick, float opacity, boolean through, float[] c1, float[] c2) {
      if (!scans.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            scans.clear();
         } else {
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            long now = System.nanoTime();
            float maxRange = 0.0F;
            WorldFx.begin();
            Iterator<ScanFx.Scan> it = scans.iterator();

            while (it.hasNext()) {
               ScanFx.Scan s = it.next();
               float age = (float)(now - s.t) / 1.0E9F;
               float front = age * s.speed;
               if (front > s.range + 8.0F) {
                  it.remove();
               } else {
                  maxRange = Math.max(maxRange, s.range);
                  float cx = (float)(s.x - cam.x);
                  float cy = (float)(s.y - cam.y);
                  float cz = (float)(s.z - cam.z);
                  int lo = lowerBound(s.dist, s.n, front - 8.0F);
                  int drawn = 0;

                  for (int i = lo; i < s.n && s.dist[i] <= front + 1.2F && drawn < 2600 && !(s.dist[i] > s.range); i++) {
                     int o = i * 7;
                     faces(s.bx[o], s.bx[o + 1], s.bx[o + 2], s.bx[o + 3], s.bx[o + 4], s.bx[o + 5], (int)s.bx[o + 6], 0.004F, cam, cx, cy, cz, front);
                     drawn++;
                  }

                  AABB area = new AABB(s.x - s.range, s.y - s.range, s.z - s.range, s.x + s.range, s.y + s.range, s.z + s.range);

                  for (Entity e : mc.level.getEntities((Entity)null, area, en -> en instanceof LivingEntity && en != mc.player && en.isAlive())) {
                     Vec3 lp = e.getPosition(partialTick);
                     AABB b = e.getBoundingBox().move(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ()).inflate(0.04);
                     faces((float)b.minX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ, 63, 0.0F, cam, cx, cy, cz, front);
                  }
               }
            }

            if (!WorldFx.isEmpty()) {
               float op = opacity;
               float rng = maxRange;
               float tm = (float)(now / 1.0E9 % 3600.0);
               WorldFx.flush("scan", through, sh -> {
                  sh.setUniform3f("uColor1", c1[0], c1[1], c1[2]);
                  sh.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
                  sh.setUniform1f("uRange", rng);
                  sh.setUniform1f("uTrail", 1.6F);
                  sh.setUniform1f("uOpacity", op);
                  sh.setUniform1f("uTime", tm);
               });
            }
         }
      }
   }

   static void faces(float x0, float y0, float z0, float x1, float y1, float z1, int mask, float eps, Vec3 cam, float r, float g, float b, float a) {
      float ox = (float)cam.x;
      float oy = (float)cam.y;
      float oz = (float)cam.z;
      x0 -= ox;
      x1 -= ox;
      y0 -= oy;
      y1 -= oy;
      z0 -= oz;
      z1 -= oz;
      float w = x1 - x0;
      float h = y1 - y0;
      float d = z1 - z0;
      if ((mask & 1) != 0) {
         quad(x1 + eps, y0, z0, 0.0F, h, 0.0F, 0.0F, 0.0F, d, h, d, r, g, b, a);
      }

      if ((mask & 2) != 0) {
         quad(x0 - eps, y0, z0, 0.0F, h, 0.0F, 0.0F, 0.0F, d, h, d, r, g, b, a);
      }

      if ((mask & 4) != 0) {
         quad(x0, y1 + eps, z0, w, 0.0F, 0.0F, 0.0F, 0.0F, d, w, d, r, g, b, a);
      }

      if ((mask & 8) != 0) {
         quad(x0, y0 - eps, z0, w, 0.0F, 0.0F, 0.0F, 0.0F, d, w, d, r, g, b, a);
      }

      if ((mask & 16) != 0) {
         quad(x0, y0, z1 + eps, w, 0.0F, 0.0F, 0.0F, h, 0.0F, w, h, r, g, b, a);
      }

      if ((mask & 32) != 0) {
         quad(x0, y0, z0 - eps, w, 0.0F, 0.0F, 0.0F, h, 0.0F, w, h, r, g, b, a);
      }
   }

   static void quad(float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz, float su, float sv, float r, float g, float b, float a) {
      WorldFx.vertex(ox, oy, oz, 0.0F, 0.0F, r, g, b, a, su, sv);
      WorldFx.vertex(ox + ux, oy + uy, oz + uz, 1.0F, 0.0F, r, g, b, a, su, sv);
      WorldFx.vertex(ox + ux + vx, oy + uy + vy, oz + uz + vz, 1.0F, 1.0F, r, g, b, a, su, sv);
      WorldFx.vertex(ox, oy, oz, 0.0F, 0.0F, r, g, b, a, su, sv);
      WorldFx.vertex(ox + ux + vx, oy + uy + vy, oz + uz + vz, 1.0F, 1.0F, r, g, b, a, su, sv);
      WorldFx.vertex(ox + vx, oy + vy, oz + vz, 0.0F, 1.0F, r, g, b, a, su, sv);
   }

   private static int lowerBound(float[] a, int n, float v) {
      int lo = 0;
      int hi = n;

      while (lo < hi) {
         int m = lo + hi >>> 1;
         if (a[m] < v) {
            lo = m + 1;
         } else {
            hi = m;
         }
      }

      return lo;
   }

   private record Scan(double x, double y, double z, float range, float speed, long t, float[] bx, float[] dist, int n) {
   }
}
