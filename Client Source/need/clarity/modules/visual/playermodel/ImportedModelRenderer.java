package need.clarity.modules.visual.playermodel;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import need.clarity.Clarity;
import need.clarity.render.ThroughWalls;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class ImportedModelRenderer {
   private static volatile ImportedModel model;
   private static volatile String modelKey = "";
   private static volatile String loadingKey = "";
   private static volatile String error = "";
   private static final List<Identifier> textures = new ArrayList<>();
   private static Identifier white;
   private static ImportedModel uploadedFor;
   private static final float[][] RIG = new float[6][9];

   private ImportedModelRenderer() {
   }

   public static Path folder() {
      Path d = FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("models");

      try {
         Files.createDirectories(d);
      } catch (Exception var2) {
      }

      return d;
   }

   public static List<String> files() {
      Path dir = folder();
      List<String> out = new ArrayList<>();

      try (Stream<Path> s = Files.walk(dir, 2)) {
         s.filter(x$0 -> Files.isRegularFile(x$0)).filter(ImportedModel::supported).forEach(p -> out.add(dir.relativize(p).toString().replace('\\', '/')));
      } catch (Exception var7) {
      }

      out.sort(String.CASE_INSENSITIVE_ORDER);
      return out;
   }

   public static String status() {
      if (!error.isEmpty()) {
         return error;
      } else {
         ImportedModel m = model;
         if (!loadingKey.isEmpty() && !loadingKey.equals(modelKey)) {
            return "Loading…";
         } else {
            return m == null ? "No model" : m.name + " · " + m.triangles + " triangles";
         }
      }
   }

   public static void reload() {
      modelKey = "";
      loadingKey = "";
      error = "";
   }

   public static ImportedModel get(String file, boolean zUp) {
      if (file != null && !file.isBlank()) {
         Path p = folder().resolve(file.trim()).normalize();
         if (!p.startsWith(folder())) {
            error = "The model must be in the models folder";
            return null;
         }

         long mtime;
         try {
            mtime = Files.getLastModifiedTime(p).toMillis();
         } catch (Exception e) {
            error = "Can't find " + file.trim();
            return null;
         }

         String key = p + "|" + mtime + "|" + zUp;
         if (key.equals(modelKey)) {
            return model;
         }

         if (!key.equals(loadingKey)) {
            loadingKey = key;
            error = "";
            Thread t = new Thread(() -> {
               try {
                  ImportedModel m = ImportedModel.load(p, zUp);
                  if (key.equals(loadingKey)) {
                     model = m;
                     modelKey = key;
                     error = "";
                  }
               } catch (Throwable ex) {
                  if (key.equals(loadingKey)) {
                     model = null;
                     modelKey = key;
                     error = "Couldn't load " + p.getFileName() + ": " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                     Clarity.LOGGER.warn("[Clarity] " + error, ex);
                  }
               }
            }, "Clarity model loader");
            t.setDaemon(true);
            t.start();
         }

         return key.equals(modelKey) ? model : null;
      } else {
         error = "Pick a model file";
         return null;
      }
   }

   private static void upload(ImportedModel m) {
      if (m != uploadedFor) {
         Minecraft mc = Minecraft.getInstance();
         Map<String, Identifier> byImage = new HashMap<>();
         int slot = 0;

         for (Entry<String, byte[]> e : m.images.entrySet()) {
            try {
               NativeImage img = readImage(e.getValue());
               Identifier id = Clarity.id("model/tex_" + slot++);
               mc.getTextureManager().register(id, new DynamicTexture(() -> "clarity_model_texture", img));
               byImage.put(e.getKey(), id);
            } catch (Throwable ex) {
               Clarity.LOGGER.warn("[Clarity] Model texture " + e.getKey() + " couldn't be read", ex);
            }
         }

         if (white == null) {
            try {
               NativeImage img = new NativeImage(1, 1, false);
               img.setPixel(0, 0, -1);
               white = Clarity.id("model/white");
               mc.getTextureManager().register(white, new DynamicTexture(() -> "clarity_model_white", img));
            } catch (Throwable var8) {
            }
         }

         textures.clear();

         for (ImportedModel.Part p : m.parts) {
            textures.add(p.image != null && byImage.containsKey(p.image) ? byImage.get(p.image) : white);
         }

         uploadedFor = m;
      }
   }

   private static NativeImage readImage(byte[] bytes) throws Exception {
      boolean png = bytes.length > 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71;
      if (!png) {
         BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
         if (bi == null) {
            throw new IllegalArgumentException("unsupported image format");
         }

         ByteArrayOutputStream out = new ByteArrayOutputStream();
         ImageIO.write(bi, "png", out);
         bytes = out.toByteArray();
      }

      return NativeImage.read(new ByteArrayInputStream(bytes));
   }

   public static void draw(PoseStack poseStack, ImportedModel m, double ox, double oy, double oz, double yaw, float height, float roll, boolean lighting, boolean walls, float alpha, float[] anim) {
      upload(m);
      float[][] rig = anim != null && m.rigged ? pose(anim) : null;
      Minecraft mc = Minecraft.getInstance();
      BufferSource vcp = mc.renderBuffers().bufferSource();
      Pose pose = poseStack.last();
      float fx = (float)(-Math.sin(yaw));
      float fz = (float)Math.cos(yaw);
      float cr = (float)Math.cos(roll);
      float sr = (float)Math.sin(roll);
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      int light = 15728880;

      for (int pi = 0; pi < m.parts.size(); pi++) {
         ImportedModel.Part p = m.parts.get(pi);
         Identifier tex = pi < textures.size() ? textures.get(pi) : white;
         if (tex != null && p.count != 0) {
            RenderType type = RenderTypes.entityTranslucentCullItemTarget(tex);
            if (walls) {
               type = ThroughWalls.convert(type);
            }

            VertexConsumer vc = vcp.getBuffer(type);
            int mc8 = p.color;
            float[] P = p.pos;
            float[] N = p.nrm;
            float[] U = p.uv;
            int[] VC = p.vcol;
            byte[] BN = p.bone;

            for (int t = 0; t + 2 < p.count; t += 3) {
               for (int k = 0; k < 4; k++) {
                  int i = t + Math.min(k, 2);
                  float mx = P[i * 3];
                  float my = P[i * 3 + 1];
                  float mz = P[i * 3 + 2];
                  float na = N[i * 3];
                  float nb = N[i * 3 + 1];
                  float nc = N[i * 3 + 2];
                  int bi = rig != null && BN != null ? BN[i] : 0;
                  if (bi != 0) {
                     float[] R = rig[bi];
                     float dx = mx - m.pivot[bi * 3];
                     float dy = my - m.pivot[bi * 3 + 1];
                     float dz = mz - m.pivot[bi * 3 + 2];
                     mx = R[0] * dx + R[1] * dy + R[2] * dz + m.pivot[bi * 3];
                     my = R[3] * dx + R[4] * dy + R[5] * dz + m.pivot[bi * 3 + 1];
                     mz = R[6] * dx + R[7] * dy + R[8] * dz + m.pivot[bi * 3 + 2];
                     float ra = R[0] * na + R[1] * nb + R[2] * nc;
                     float rb = R[3] * na + R[4] * nb + R[5] * nc;
                     float rc = R[6] * na + R[7] * nb + R[8] * nc;
                     na = ra;
                     nb = rb;
                     nc = rc;
                  }

                  float lx = mx * height;
                  float ly = my * height;
                  float lz = mz * height;
                  float rx = lx * cr - ly * sr;
                  float ry = lx * sr + ly * cr;
                  float wx = fz * rx + fx * lz;
                  float wz = -fx * rx + fz * lz;
                  float nx = 0.0F;
                  float ny = 1.0F;
                  float nz = 0.0F;
                  if (lighting) {
                     float ax = na;
                     float ay = nb;
                     float az = nc;
                     float bx = ax * cr - ay * sr;
                     float by = ax * sr + ay * cr;
                     nx = fz * bx + fx * az;
                     ny = by;
                     nz = -fx * bx + fz * az;
                  }

                  int col = VC != null ? mulColor(VC[i], mc8) : mc8;
                  col = (col >>> 24 & 0xFF) * a / 255 << 24 | col & 16777215;
                  vc.addVertex(pose, (float)(ox + wx), (float)(oy + ry), (float)(oz + wz))
                     .setColor(col)
                     .setUv(U[i * 2], U[i * 2 + 1])
                     .setOverlay(OverlayTexture.NO_OVERLAY)
                     .setLight(15728880)
                     .setNormal(pose, nx, ny, nz);
               }
            }

            vcp.endBatch(type);
         }
      }
   }

   private static float[][] pose(float[] a) {
      float phase = a[0];
      float amp = a[1];
      float air = a[2];
      float crouch = a[3];
      float hyaw = a[4];
      float hpitch = a[5];
      float attack = a[6];
      float s = (float)Math.sin(phase) * amp;
      float legSwing = 0.75F * s;
      float armSwing = 0.6F * s;
      float swing = attack > 0.0F ? (float)Math.sin(attack * Math.PI) : 0.0F;
      euler(RIG[1], hpitch + 0.15F * crouch, -hyaw, 0.0F);
      euler(RIG[2], armSwing, 0.0F, 0.45F * air);
      euler(RIG[3], -armSwing - 1.4F * swing, 0.0F, -0.45F * air);
      euler(RIG[4], -legSwing - 0.4F * air, 0.0F, 0.0F);
      euler(RIG[5], legSwing + 0.4F * air, 0.0F, 0.0F);
      return RIG;
   }

   private static void euler(float[] m, float rx, float ry, float rz) {
      float cx = (float)Math.cos(rx);
      float sx = (float)Math.sin(rx);
      float cy = (float)Math.cos(ry);
      float sy = (float)Math.sin(ry);
      float cz = (float)Math.cos(rz);
      float sz = (float)Math.sin(rz);
      float a00 = cz;
      float a01 = -sz * cx;
      float a02 = sz * sx;
      float a10 = sz;
      float a11 = cz * cx;
      float a12 = -cz * sx;
      float a20 = 0.0F;
      float a21 = sx;
      float a22 = cx;
      m[0] = cy * a00 + sy * a20;
      m[1] = cy * a01 + sy * a21;
      m[2] = cy * a02 + sy * a22;
      m[3] = a10;
      m[4] = a11;
      m[5] = a12;
      m[6] = -sy * a00 + cy * a20;
      m[7] = -sy * a01 + cy * a21;
      m[8] = -sy * a02 + cy * a22;
   }

   private static int mulColor(int x, int y) {
      int a = (x >>> 24 & 0xFF) * (y >>> 24 & 0xFF) / 255;
      int r = (x >> 16 & 0xFF) * (y >> 16 & 0xFF) / 255;
      int g = (x >> 8 & 0xFF) * (y >> 8 & 0xFF) / 255;
      int b = (x & 0xFF) * (y & 0xFF) / 255;
      return a << 24 | r << 16 | g << 8 | b;
   }
}
