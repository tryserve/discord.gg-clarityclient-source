package need.clarity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TrapBoxRenderer {
   public static final String[] STYLES = new String[]{"Glow Edges", "Gradient", "Shimmer", "Pulse", "Neon"};

   private TrapBoxRenderer() {
   }

   public static int style(String name) {
      for (int i = 0; i < STYLES.length; i++) {
         if (STYLES[i].equals(name)) {
            return i;
         }
      }

      return -1;
   }

   public static void render(PoseStack ps, List<TrapBoxRenderer.Box> boxes, int style, int rgb1, int rgb2, float fill, float outline, float time, boolean throughWalls) {
      if (!boxes.isEmpty()) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         List<TrapBoxRenderer.Fill> fills = new ArrayList<>();
         List<TrapBoxRenderer.Line> lines = new ArrayList<>();

         for (TrapBoxRenderer.Box bx : boxes) {
            AABB b = bx.box().move(-cam.x, -cam.y, -cam.z).inflate(0.002);
            float f = bx.fade();
            float seed = bx.seed();
            switch (style) {
               case 0: {
                  int c = rgb1;
                  fills.add(new TrapBoxRenderer.Fill(b, argb(c, fill * 0.55F * f), argb(c, fill * 0.55F * f)));
                  lines.add(new TrapBoxRenderer.Line(b.inflate(0.035), argb(c, outline * 0.1F * f), argb(c, outline * 0.1F * f), 9.0F));
                  lines.add(new TrapBoxRenderer.Line(b.inflate(0.015), argb(c, outline * 0.22F * f), argb(c, outline * 0.22F * f), 5.0F));
                  lines.add(new TrapBoxRenderer.Line(b, argb(lighten(c, 0.35F), outline * f), argb(lighten(c, 0.35F), outline * f), 2.0F));
                  break;
               }
               case 1:
                  fills.add(new TrapBoxRenderer.Fill(b, argb(rgb2, fill * 0.35F * f), argb(rgb1, fill * f)));
                  lines.add(new TrapBoxRenderer.Line(b, argb(rgb2, outline * 0.6F * f), argb(rgb1, outline * f), 2.0F));
                  break;
               case 2:
                  fills.add(new TrapBoxRenderer.Fill(b, argb(rgb1, fill * 0.6F * f), argb(rgb1, fill * 0.6F * f)));
                  double h = b.maxY - b.minY;
                  double ph = frac(time * 0.9F + seed);
                  double cy = b.minY + ph * h;
                  double half = Math.min(0.09, h * 0.12);
                  AABB band = new AABB(b.minX, Math.max(b.minY, cy - half), b.minZ, b.maxX, Math.min(b.maxY, cy + half), b.maxZ).inflate(0.003, 0.0, 0.003);
                  int bright = lighten(rgb1, 0.6F);
                  float edge = (float)Math.sin(Math.PI * ph);
                  fills.add(new TrapBoxRenderer.Fill(band, argb(bright, 0.55F * edge * f), argb(bright, 0.55F * edge * f)));
                  lines.add(new TrapBoxRenderer.Line(band, argb(bright, 0.8F * edge * f), argb(bright, 0.8F * edge * f), 1.5F));
                  lines.add(new TrapBoxRenderer.Line(b, argb(rgb1, outline * f), argb(rgb1, outline * f), 2.0F));
                  break;
               case 3: {
                  float p = 0.5F + 0.5F * (float)Math.sin(time * 4.0 + seed * 6.2832);
                  int c = mix(rgb2, rgb1, p);
                  float fa = fill * (0.45F + 0.8F * p) * f;
                  fills.add(new TrapBoxRenderer.Fill(b, argb(c, fa), argb(c, fa)));
                  lines.add(new TrapBoxRenderer.Line(b.inflate(0.02 + 0.03 * p), argb(c, outline * 0.18F * p * f), argb(c, outline * 0.18F * p * f), 7.0F));
                  lines.add(new TrapBoxRenderer.Line(b, argb(c, outline * f), argb(c, outline * f), 2.0F));
                  break;
               }
               default:
                  float k = 0.5F + 0.5F * (float)Math.sin(time * 2.0 + seed * 6.2832);
                  int lo = mix(rgb1, rgb2, k);
                  int hi = mix(rgb2, rgb1, k);
                  fills.add(new TrapBoxRenderer.Fill(b, argb(lo, fill * 0.12F * f), argb(hi, fill * 0.12F * f)));
                  lines.add(new TrapBoxRenderer.Line(b.inflate(0.04), argb(lo, outline * 0.1F * f), argb(hi, outline * 0.1F * f), 11.0F));
                  lines.add(new TrapBoxRenderer.Line(b.inflate(0.018), argb(lo, outline * 0.25F * f), argb(hi, outline * 0.25F * f), 6.0F));
                  lines.add(new TrapBoxRenderer.Line(b, argb(lighten(lo, 0.5F), outline * f), argb(lighten(hi, 0.5F), outline * f), 2.5F));
            }
         }

         BufferSource vcp = Minecraft.getInstance().renderBuffers().bufferSource();
         Pose e = ps.last();
         RenderType quads = throughWalls ? ClarityRenderUtils.ESP_QUADS : ClarityRenderUtils.ESP_QUADS_OCCLUDED;
         VertexConsumer qv = vcp.getBuffer(quads);

         for (TrapBoxRenderer.Fill fl : fills) {
            fillBox(qv, e, fl.b(), fl.bottom(), fl.top());
         }

         vcp.endBatch(quads);
         RenderType lt = throughWalls ? ClarityRenderUtils.ESP_OUTLINE : ClarityRenderUtils.ESP_OUTLINE_OCCLUDED;
         VertexConsumer lv = vcp.getBuffer(lt);

         for (TrapBoxRenderer.Line ln : lines) {
            outlineBox(lv, e, ln.b(), ln.bottom(), ln.top(), ln.width());
         }

         vcp.endBatch(lt);
      }
   }

   private static void fillBox(VertexConsumer vc, Pose e, AABB b, int bot, int top) {
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      vc.addVertex(e, x1, y1, z1).setColor(bot);
      vc.addVertex(e, x2, y1, z1).setColor(bot);
      vc.addVertex(e, x2, y1, z2).setColor(bot);
      vc.addVertex(e, x1, y1, z2).setColor(bot);
      vc.addVertex(e, x1, y2, z1).setColor(top);
      vc.addVertex(e, x1, y2, z2).setColor(top);
      vc.addVertex(e, x2, y2, z2).setColor(top);
      vc.addVertex(e, x2, y2, z1).setColor(top);
      vc.addVertex(e, x1, y1, z1).setColor(bot);
      vc.addVertex(e, x1, y2, z1).setColor(top);
      vc.addVertex(e, x2, y2, z1).setColor(top);
      vc.addVertex(e, x2, y1, z1).setColor(bot);
      vc.addVertex(e, x1, y1, z2).setColor(bot);
      vc.addVertex(e, x2, y1, z2).setColor(bot);
      vc.addVertex(e, x2, y2, z2).setColor(top);
      vc.addVertex(e, x1, y2, z2).setColor(top);
      vc.addVertex(e, x1, y1, z1).setColor(bot);
      vc.addVertex(e, x1, y1, z2).setColor(bot);
      vc.addVertex(e, x1, y2, z2).setColor(top);
      vc.addVertex(e, x1, y2, z1).setColor(top);
      vc.addVertex(e, x2, y1, z1).setColor(bot);
      vc.addVertex(e, x2, y2, z1).setColor(top);
      vc.addVertex(e, x2, y2, z2).setColor(top);
      vc.addVertex(e, x2, y1, z2).setColor(bot);
   }

   private static void outlineBox(VertexConsumer vc, Pose e, AABB b, int bot, int top, float w) {
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      line(vc, e, x1, y1, z1, x2, y1, z1, 1.0F, 0.0F, 0.0F, bot, bot, w);
      line(vc, e, x2, y1, z1, x2, y1, z2, 0.0F, 0.0F, 1.0F, bot, bot, w);
      line(vc, e, x2, y1, z2, x1, y1, z2, -1.0F, 0.0F, 0.0F, bot, bot, w);
      line(vc, e, x1, y1, z2, x1, y1, z1, 0.0F, 0.0F, -1.0F, bot, bot, w);
      line(vc, e, x1, y2, z1, x2, y2, z1, 1.0F, 0.0F, 0.0F, top, top, w);
      line(vc, e, x2, y2, z1, x2, y2, z2, 0.0F, 0.0F, 1.0F, top, top, w);
      line(vc, e, x2, y2, z2, x1, y2, z2, -1.0F, 0.0F, 0.0F, top, top, w);
      line(vc, e, x1, y2, z2, x1, y2, z1, 0.0F, 0.0F, -1.0F, top, top, w);
      line(vc, e, x1, y1, z1, x1, y2, z1, 0.0F, 1.0F, 0.0F, bot, top, w);
      line(vc, e, x2, y1, z1, x2, y2, z1, 0.0F, 1.0F, 0.0F, bot, top, w);
      line(vc, e, x2, y1, z2, x2, y2, z2, 0.0F, 1.0F, 0.0F, bot, top, w);
      line(vc, e, x1, y1, z2, x1, y2, z2, 0.0F, 1.0F, 0.0F, bot, top, w);
   }

   private static void line(VertexConsumer vc, Pose e, float ax, float ay, float az, float bx, float by, float bz, float nx, float ny, float nz, int ca, int cb, float w) {
      vc.addVertex(e, ax, ay, az).setColor(ca).setNormal(e, nx, ny, nz).setLineWidth(w);
      vc.addVertex(e, bx, by, bz).setColor(cb).setNormal(e, nx, ny, nz).setLineWidth(w);
   }

   private static int argb(int rgb, float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int a, int b, float k) {
      int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * k);
      int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * k);
      int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
      return r << 16 | g << 8 | bl;
   }

   private static int lighten(int rgb, float k) {
      return mix(rgb, 16777215, k);
   }

   private static double frac(double v) {
      return v - Math.floor(v);
   }

   public record Box(AABB box, float fade, float seed) {
   }

   private record Fill(AABB b, int bottom, int top) {
   }

   private record Line(AABB b, int bottom, int top, float width) {
   }
}
