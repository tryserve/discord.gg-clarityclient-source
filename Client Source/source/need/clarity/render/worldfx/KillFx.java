package need.clarity.render.worldfx;

import java.util.ArrayList;
import java.util.List;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.world.phys.Vec3;

public final class KillFx {
   public static final String[] STYLES = new String[]{"Supernova", "Thunderbolt", "Soul Ascend", "Event Horizon", "Light Pillar", "Crystal Burst"};
   private static final float[] LIFE = new float[]{1.3F, 1.0F, 2.2F, 1.7F, 2.2F, 1.2F};
   private static final int[][] NATURAL = new int[][]{{16765562, 16738877}, {10475775, 5930239}, {6087167, 2780159}, {11820287, 3807884}, {16770442, 16777215}, {8382719, 13169919}};
   private static final List<KillFx.Kill> kills = new ArrayList<>();
   private static int counter = 0;
   private static final float[] R = new float[3];
   private static final float[] U = new float[3];
   private static final float[] F = new float[3];
   private static final float[] uBurst = new float[128];
   private static final float[] uInfo = new float[128];

   private KillFx() {
   }

   public static int style(String name) {
      if (name != null) {
         for (int i = 0; i < STYLES.length; i++) {
            if (STYLES[i].equalsIgnoreCase(name)) {
               return i;
            }
         }
      }

      return -1;
   }

   public static void spawn(int style, double x, double y, double z, float w, float h) {
      kills.add(new KillFx.Kill(style, x, y, z, w, h, System.nanoTime(), counter++ * 0.6180339F % 1.0F));

      while (kills.size() > 24) {
         kills.remove(0);
      }
   }

   public static void clear() {
      kills.clear();
   }

   public static void render(float scale, float opacity, boolean throughWalls, boolean natural, float[] c1, float[] c2) {
      long now = System.nanoTime();
      kills.removeIf(k -> (now - k.t) / 1.0E9 > LIFE[k.style]);
      if (!kills.isEmpty()) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         WorldFx.axes(R, U, F);
         float hx = R[0];
         float hz = R[2];
         float hl = (float)Math.sqrt(hx * hx + hz * hz);
         if (hl < 1.0E-4F) {
            hx = 1.0F;
            hz = 0.0F;
         } else {
            hx /= hl;
            hz /= hl;
         }

         float time = (float)(now / 1.0E9 % 3600.0);

         for (int style = 0; style < STYLES.length; style++) {
            float[] a = natural ? rgb(NATURAL[style][0]) : c1;
            float[] b = natural ? rgb(NATURAL[style][1]) : c2;
            WorldFx.begin();
            int shards = 0;

            for (KillFx.Kill k : kills) {
               if (k.style == style) {
                  float p = (float)Math.min(1.0, (now - k.t) / 1.0E9 / LIFE[style]);
                  float x = (float)(k.x - cam.x);
                  float y = (float)(k.y - cam.y);
                  float z = (float)(k.z - cam.z);
                  float cy = y + k.h * 0.5F;
                  float s = scale;
                  switch (style) {
                     case 0:
                        bb(x, cy, z, 2.2F * s, 1.3F, a, opacity, p, 0, k.seed);
                        ground(x, y, z, 4.0F * s, 1.25F, a, opacity, p, 1, k.seed);
                        break;
                     case 1:
                        upright(x, y, z, hx, hz, 1.6F * s, 30.0F, a, opacity, p, 0, k.seed);
                        ground(x, y, z, 2.0F * s, 1.25F, a, opacity, p, 1, k.seed);
                        break;
                     case 2:
                        upright(x, y, z, hx, hz, 0.9F * s, k.h * 2.6F * s, a, opacity, p, 0, k.seed);
                        break;
                     case 3:
                        bb(x, cy, z, 1.8F * s, 1.2F, a, opacity, p, 0, k.seed);
                        break;
                     case 4:
                        upright(x, y, z, hx, hz, 0.9F * s, 24.0F, a, opacity, p, 0, k.seed);
                        ground(x, y, z, 1.3F * s, 1.25F, a, opacity, p, 1, k.seed);
                        break;
                     default:
                        bb(x, cy, z, 1.4F * s, 1.0F, a, opacity, p, 0, k.seed);
                        if (shards < 32) {
                           int i = shards++;
                           uBurst[i * 4] = x;
                           uBurst[i * 4 + 1] = cy;
                           uBurst[i * 4 + 2] = z;
                           uBurst[i * 4 + 3] = (float)((now - k.t) / 1.0E9);
                           uInfo[i * 4] = k.seed;
                           uInfo[i * 4 + 1] = (0.8F + k.w) * s;
                           uInfo[i * 4 + 2] = LIFE[style];
                        }
                  }
               }
            }

            if (!WorldFx.isEmpty()) {
               int st = style;
               float[] fb = b;
               WorldFx.flush("killfx", throughWalls, sh -> {
                  sh.setUniform1f("uTime", time);
                  sh.setUniform1i("uStyle", st);
                  sh.setUniform3f("uColor2", fb[0], fb[1], fb[2]);
               });
               if (shards > 0) {
                  int n = shards;
                  float[] fa = a;

                  for (int i = n; i < 32; i++) {
                     uBurst[i * 4 + 3] = 99.0F;
                  }

                  WorldFx.drawInstanced("burst", n * 48, throughWalls, 0, sh -> {
                     sh.setUniform4fv("uBurst", uBurst);
                     sh.setUniform4fv("uInfo", uInfo);
                     sh.setUniform1i("uPer", 48);
                     sh.setUniform1i("uStyle", 3);
                     sh.setUniform1f("uSize", 1.5F * scale);
                     sh.setUniform3f("uColor1", fa[0], fa[1], fa[2]);
                     sh.setUniform3f("uColor2", fb[0], fb[1], fb[2]);
                     sh.setUniform1i("uNatural", 0);
                  });
               }
            }
         }
      }
   }

   private static void bb(float x, float y, float z, float half, float E, float[] c, float op, float p, int part, float seed) {
      quad(x, y, z, R[0], R[1], R[2], U[0], U[1], U[2], half * E, half * E, -E, -E, E, E, c, op, p, part + seed * 0.99F);
   }

   private static void ground(float x, float y, float z, float half, float E, float[] c, float op, float p, int part, float seed) {
      quad(x, y + 0.02F, z, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, half * E, half * E, -E, -E, E, E, c, op, p, part + seed * 0.99F);
   }

   private static void upright(float x, float y, float z, float hx, float hz, float halfW, float height, float[] c, float op, float p, int part, float seed) {
      quad(x, y + height * 0.5F, z, hx, 0.0F, hz, 0.0F, 1.0F, 0.0F, halfW, height * 0.5F, -1.0F, 0.0F, 1.0F, 1.0F, c, op, p, part + seed * 0.99F);
   }

   private static void quad(
      float cx, float cy, float cz, float ax, float ay, float az, float bx, float by, float bz, float ha, float hb, float u0, float v0, float u1, float v1, float[] c, float op, float p, float aux
   ) {
      float[][] uv = new float[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
      float[][] P = new float[4][];
      float um = (u0 + u1) * 0.5F;
      float vm = (v0 + v1) * 0.5F;
      float uh = (u1 - u0) * 0.5F;
      float vh = (v1 - v0) * 0.5F;

      for (int i = 0; i < 4; i++) {
         float su = (uv[i][0] - um) / uh;
         float sv = (uv[i][1] - vm) / vh;
         P[i] = new float[]{cx + ax * ha * su + bx * hb * sv, cy + ay * ha * su + by * hb * sv, cz + az * ha * su + bz * hb * sv};
      }

      int[] order = new int[]{0, 1, 2, 0, 2, 3};

      for (int i : order) {
         WorldFx.vertex(P[i][0], P[i][1], P[i][2], uv[i][0], uv[i][1], c[0], c[1], c[2], op, p, aux);
      }
   }

   private static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }

   private record Kill(int style, double x, double y, double z, float w, float h, long t, float seed) {
   }
}
