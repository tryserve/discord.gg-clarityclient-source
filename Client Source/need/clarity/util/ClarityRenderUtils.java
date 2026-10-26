package need.clarity.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import need.clarity.modules.visual.RenderAnimations;
import need.clarity.modules.visual.cosmetics.CosmeticSink;
import need.clarity.render.ChamsRenderer;
import need.clarity.render.ThroughWalls;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ClarityRenderUtils {
   private static final Snippet FOGLESS_LINES_SNIPPET = RenderPipeline.builder(new Snippet[]{RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
      .withVertexShader(Identifier.parse("clarity:core/fogless_lines"))
      .withFragmentShader(Identifier.parse("clarity:core/fogless_lines"))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
      .buildSnippet();
   public static final RenderPipeline ESP_FILLED_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.parse("clarity:pipeline/esp_filled"))
         .withDepthStencilState(Optional.empty())
         .withCull(true)
         .build()
   );
   public static final RenderPipeline ESP_LINES_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{FOGLESS_LINES_SNIPPET}).withLocation(Identifier.parse("clarity:pipeline/esp_lines")).build()
   );
   public static final RenderType ESP_QUADS = RenderType.create(
      "clarity:esp_quads",
      RenderSetup.builder(ESP_FILLED_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).sortOnUpload().createRenderSetup()
   );
   public static final RenderType ESP_OUTLINE = RenderType.create(
      "clarity:esp_outline", RenderSetup.builder(ESP_LINES_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup()
   );
   private static final DepthStencilState ESP_OCCLUDED_DEPTH = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false);
   public static final RenderPipeline ESP_FILLED_OCCLUDED_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.parse("clarity:pipeline/esp_filled_occluded"))
         .withDepthStencilState(ESP_OCCLUDED_DEPTH)
         .withCull(true)
         .build()
   );
   public static final RenderPipeline ESP_LINES_OCCLUDED_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{FOGLESS_LINES_SNIPPET}).withLocation(Identifier.parse("clarity:pipeline/esp_lines_occluded")).withDepthStencilState(ESP_OCCLUDED_DEPTH).build()
   );
   public static final RenderType ESP_QUADS_OCCLUDED = RenderType.create(
      "clarity:esp_quads_occluded",
      RenderSetup.builder(ESP_FILLED_OCCLUDED_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).sortOnUpload().createRenderSetup()
   );
   public static final RenderType ESP_OUTLINE_OCCLUDED = RenderType.create(
      "clarity:esp_outline_occluded",
      RenderSetup.builder(ESP_LINES_OCCLUDED_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup()
   );
   public static final int WING_OCCLUDED = 0;
   public static final int WING_THROUGH_WALLS = 1;
   public static final int WING_CHAMS_LAYERED = 2;
   public static final int COSMETIC_OCCLUDED = 0;
   public static final int COSMETIC_THROUGH_WALLS = 1;
   public static final int COSMETIC_CHAMS_LAYERED = 2;
   private static final RenderPipeline WINGS_OCCLUDED_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.parse("clarity:pipeline/wings_occluded"))
         .withDepthStencilState(ESP_OCCLUDED_DEPTH)
         .withCull(false)
         .build()
   );
   private static final RenderPipeline WINGS_WALLS_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.parse("clarity:pipeline/wings_walls"))
         .withDepthStencilState(Optional.empty())
         .withCull(false)
         .build()
   );
   private static final RenderType WINGS_OCCLUDED = makeWingType("wings_occluded", WINGS_OCCLUDED_PIPE);
   private static final RenderType WINGS_WALLS = makeWingType("wings_walls", WINGS_WALLS_PIPE);
   private static RenderPipeline wingsChamsPipe;
   private static RenderType wingsChams;
   private static final RenderType ORDERED_OCCLUDED = makeOrderedType("ordered_occluded", WINGS_OCCLUDED_PIPE);
   private static final RenderType ORDERED_WALLS = makeOrderedType("ordered_walls", WINGS_WALLS_PIPE);
   private static RenderType orderedChams;
   public static final RenderPipeline CHAMS_UV_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.parse("clarity:pipeline/chams_uv"))
         .withColorTargetState(new ColorTargetState(Optional.empty(), 15))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withCull(false)
         .build()
   );
   public static final RenderType CHAMS_UV = RenderType.create(
      "clarity:chams_uv",
      RenderSetup.builder(CHAMS_UV_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).sortOnUpload().createRenderSetup()
   );
   public static final RenderPipeline TRACER_LINES_PIPE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{FOGLESS_LINES_SNIPPET}).withLocation(Identifier.parse("clarity:pipeline/tracer_lines")).withDepthStencilState(DepthStencilState.DEFAULT).build()
   );
   public static final RenderType TRACER_LINES = RenderType.create(
      "clarity:tracer_lines",
      RenderSetup.builder(TRACER_LINES_PIPE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup()
   );
   private static final double TRACER_MIN_DIST = 2.0;

   private static RenderType makeWingType(String name, RenderPipeline pipe) {
      return RenderType.create(
         "clarity:" + name,
         RenderSetup.builder(pipe).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).sortOnUpload().createRenderSetup()
      );
   }

   private static RenderPipeline wingChamsPipe() {
      if (wingsChamsPipe == null) {
         DepthStencilState st = ChamsRenderer.throughWallsDepthState();
         Builder b = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET}).withLocation(Identifier.parse("clarity:pipeline/wings_chams")).withCull(false);
         if (st != null) {
            b.withDepthStencilState(st);
         } else {
            b.withDepthStencilState(Optional.empty());
         }

         wingsChamsPipe = RenderPipelines.register(b.build());
      }

      return wingsChamsPipe;
   }

   private static RenderType wingChamsType() {
      if (wingsChams == null) {
         wingsChams = makeWingType("wings_chams", wingChamsPipe());
      }

      return wingsChams;
   }

   private static RenderType makeOrderedType(String name, RenderPipeline pipe) {
      return RenderType.create(
         "clarity:" + name, RenderSetup.builder(pipe).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup()
      );
   }

   private static RenderType orderedType(int mode) {
      if (mode == 1) {
         return ORDERED_WALLS;
      }

      if (mode == 2) {
         if (orderedChams == null) {
            orderedChams = makeOrderedType("ordered_chams", wingChamsPipe());
         }

         return orderedChams;
      } else {
         return ORDERED_OCCLUDED;
      }
   }

   public static void drawWingQuad(PoseStack poseStack, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb, int mode) {
      Vec3 camOffset = getCameraPos().reverse();
      BufferSource vcp = getVCP();
      RenderType type = mode == 2 ? wingChamsType() : (mode == 1 ? WINGS_WALLS : WINGS_OCCLUDED);
      VertexConsumer vc = vcp.getBuffer(type);
      drawQuad(poseStack, vc, a.add(camOffset), b.add(camOffset), c.add(camOffset), d.add(camOffset), anim(argb));
      vcp.endBatch(type);
   }

   public static ClarityRenderUtils.OrderedBatch beginOrdered(PoseStack poseStack, int mode) {
      return new ClarityRenderUtils.OrderedBatch(poseStack, mode);
   }

   public static ClarityRenderUtils.CosmeticBatch beginCosmetics(PoseStack poseStack, int mode) {
      return new ClarityRenderUtils.CosmeticBatch(poseStack, mode);
   }

   public static ClarityRenderUtils.CosmeticBatch beginWings(PoseStack poseStack, int mode) {
      return beginCosmetics(poseStack, mode);
   }

   public static void drawEspMaskUV(PoseStack poseStack, List<ClarityRenderUtils.ESPEntry> boxes) {
      if (!boxes.isEmpty()) {
         Vec3 camOffset = getCameraPos().reverse();
         BufferSource vcp = getVCP();
         VertexConsumer vc = vcp.getBuffer(CHAMS_UV);

         for (ClarityRenderUtils.ESPEntry entry : boxes) {
            drawUVBox(poseStack, vc, entry.box().move(camOffset.x, camOffset.y, camOffset.z));
         }

         vcp.endBatch(CHAMS_UV);
      }
   }

   private static void drawUVBox(PoseStack ps, VertexConsumer vc, AABB b) {
      Pose e = ps.last();
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      uvQuad(vc, e, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
      uvQuad(vc, e, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
      uvQuad(vc, e, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
      uvQuad(vc, e, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
      uvQuad(vc, e, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
      uvQuad(vc, e, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
   }

   private static void uvQuad(VertexConsumer vc, Pose e, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
      vc.addVertex(e, ax, ay, az).setColor(uv(0, 0));
      vc.addVertex(e, bx, by, bz).setColor(uv(1, 0));
      vc.addVertex(e, cx, cy, cz).setColor(uv(1, 1));
      vc.addVertex(e, dx, dy, dz).setColor(uv(0, 1));
   }

   private static int uv(int u, int v) {
      return 0xFF000000 | (u & 0xFF) << 16 | (v & 0xFF) << 8;
   }

   public static ClarityRenderUtils.TexBatch beginTextured(PoseStack poseStack, Identifier texture) {
      return beginTextured(poseStack, texture, false);
   }

   public static ClarityRenderUtils.TexBatch beginTextured(PoseStack poseStack, Identifier texture, boolean chamsLayered) {
      RenderType type = RenderTypes.entityTranslucentCullItemTarget(texture);
      if (chamsLayered) {
         type = ThroughWalls.convert(type);
      }

      return new ClarityRenderUtils.TexBatch(getVCP(), type, poseStack, getCameraPos());
   }

   private ClarityRenderUtils() {
   }

   private static int anim(int argb) {
      float f = RenderAnimations.factor();
      if (f >= 0.999F) {
         return argb;
      }

      int a = Math.round((argb >>> 24 & 0xFF) * f);
      if (a < 0) {
         a = 0;
      } else if (a > 255) {
         a = 255;
      }

      return a << 24 | argb & 16777215;
   }

   public static Vec3 getCameraPos() {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      return camera == null ? Vec3.ZERO : camera.position();
   }

   private static BufferSource getVCP() {
      return Minecraft.getInstance().renderBuffers().bufferSource();
   }

   public static void drawBoxes(PoseStack poseStack, List<AABB> boxes, int argbFill, int argbLines, boolean doFill, boolean doLines) {
      if (!boxes.isEmpty()) {
         Vec3 camOffset = getCameraPos().reverse();
         BufferSource vcp = getVCP();
         if (doFill) {
            VertexConsumer fillVc = vcp.getBuffer(ESP_QUADS);

            for (AABB box : boxes) {
               drawFilledBox(poseStack, fillVc, box.move(camOffset.x, camOffset.y, camOffset.z), argbFill);
            }

            vcp.endBatch(ESP_QUADS);
         }

         if (doLines) {
            VertexConsumer lineVc = vcp.getBuffer(ESP_OUTLINE);

            for (AABB box : boxes) {
               drawOutlineBox(poseStack, lineVc, box.move(camOffset.x, camOffset.y, camOffset.z), argbLines);
            }

            vcp.endBatch(ESP_OUTLINE);
         }
      }
   }

   public static void drawBoxes(PoseStack poseStack, List<AABB> boxes, int argbFill, int argbLines) {
      drawBoxes(poseStack, boxes, argbFill, argbLines, true, true);
   }

   public static void drawEspMask(PoseStack poseStack, List<ClarityRenderUtils.ESPEntry> boxes, boolean throughWalls) {
      if (!boxes.isEmpty()) {
         Vec3 camOffset = getCameraPos().reverse();
         BufferSource vcp = getVCP();
         RenderType type = throughWalls ? ESP_QUADS : ESP_QUADS_OCCLUDED;
         VertexConsumer vc = vcp.getBuffer(type);

         for (ClarityRenderUtils.ESPEntry entry : boxes) {
            AABB box = entry.box().move(camOffset.x, camOffset.y, camOffset.z);
            drawFilledBox(poseStack, vc, box, entry.argb());
         }

         vcp.endBatch(type);
      }
   }

   public static void drawEspBoxes(PoseStack poseStack, List<ClarityRenderUtils.ESPEntry> boxes, boolean doFill, int fillArgb, boolean doLines, boolean throughWalls, boolean corners, float lineWidth) {
      if (!boxes.isEmpty()) {
         Vec3 camOffset = getCameraPos().reverse();
         BufferSource vcp = getVCP();
         if (doFill) {
            RenderType type = throughWalls ? ESP_QUADS : ESP_QUADS_OCCLUDED;
            VertexConsumer fillVc = vcp.getBuffer(type);
            int fill = anim(fillArgb);

            for (ClarityRenderUtils.ESPEntry entry : boxes) {
               AABB box = entry.box().move(camOffset.x, camOffset.y, camOffset.z);
               drawFilledBox(poseStack, fillVc, box, fill);
            }

            vcp.endBatch(type);
         }

         if (doLines) {
            RenderType type = throughWalls ? ESP_OUTLINE : ESP_OUTLINE_OCCLUDED;
            VertexConsumer lineVc = vcp.getBuffer(type);

            for (ClarityRenderUtils.ESPEntry entry : boxes) {
               AABB box = entry.box().move(camOffset.x, camOffset.y, camOffset.z);
               int line = anim(entry.argb());
               if (corners) {
                  drawCornerBox(poseStack, lineVc, box, line, lineWidth);
               } else {
                  drawOutlineBox(poseStack, lineVc, box, line, lineWidth);
               }
            }

            vcp.endBatch(type);
         }
      }
   }

   public static void drawFilledQuad(PoseStack poseStack, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb, boolean throughWalls) {
      Vec3 camOffset = getCameraPos().reverse();
      BufferSource vcp = getVCP();
      RenderType type = throughWalls ? ESP_QUADS : ESP_QUADS_OCCLUDED;
      VertexConsumer vc = vcp.getBuffer(type);
      drawQuad(poseStack, vc, a.add(camOffset), b.add(camOffset), c.add(camOffset), d.add(camOffset), anim(argb));
      vcp.endBatch(type);
   }

   private static void drawQuad(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
      Pose entry = ps.last();
      vc.addVertex(entry, (float)a.x, (float)a.y, (float)a.z).setColor(argb);
      vc.addVertex(entry, (float)b.x, (float)b.y, (float)b.z).setColor(argb);
      vc.addVertex(entry, (float)c.x, (float)c.y, (float)c.z).setColor(argb);
      vc.addVertex(entry, (float)d.x, (float)d.y, (float)d.z).setColor(argb);
   }

   public static void drawGroundRing(PoseStack poseStack, Vec3 origin, double radius, int argb, boolean throughWalls, int points) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null) {
         double size = 0.6;

         for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2) / points * i;
            double x = origin.x + Math.cos(a) * radius;
            double z = origin.z + Math.sin(a) * radius;
            int bx = (int)Math.floor(x);
            int bz = (int)Math.floor(z);
            int topY = mc.level.getHeight(Types.MOTION_BLOCKING, bx, bz);
            double y = topY + 0.02;
            Vec3 c = new Vec3(bx + 0.5, y, bz + 0.5);
            drawFilledQuad(poseStack, c.add(-size, 0.0, -size), c.add(size, 0.0, -size), c.add(size, 0.0, size), c.add(-size, 0.0, size), argb, throughWalls);
         }
      }
   }

   public static void drawFlatRing(PoseStack poseStack, Vec3 center, double radius, int argb, double size, int points) {
      for (int i = 0; i < points; i++) {
         double a = (Math.PI * 2) / points * i;
         double x = center.x + Math.cos(a) * radius;
         double z = center.z + Math.sin(a) * radius;
         drawFilledQuad(
            poseStack, new Vec3(x - size, center.y, z - size), new Vec3(x + size, center.y, z - size), new Vec3(x + size, center.y, z + size), new Vec3(x - size, center.y, z + size), argb, false
         );
      }
   }

   private static Vec3 clampTracerEnd(Vec3 camRelEnd) {
      double len = camRelEnd.length();
      if (len < 2.0) {
         return len < 1.0E-6 ? new Vec3(0.0, 0.0, -2.0) : camRelEnd.scale(2.0 / len);
      } else {
         return camRelEnd;
      }
   }

   public static void drawTracer(PoseStack poseStack, float partialTick, Vec3 worldTarget, int argb) {
      drawTracer(poseStack, partialTick, worldTarget, argb, 2.0F);
   }

   public static void drawTracer(PoseStack poseStack, float partialTick, Vec3 worldTarget, int argb, float lineWidth) {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (camera != null) {
         Vec3 camOffset = getCameraPos().reverse();
         Vec3 end = clampTracerEnd(worldTarget.add(camOffset));
         Vec3 start = new Vec3(
               Math.sin(-Math.toRadians(camera.yRot())) * Math.cos(Math.toRadians(camera.xRot())),
               -Math.sin(Math.toRadians(camera.xRot())),
               Math.cos(-Math.toRadians(camera.yRot())) * Math.cos(Math.toRadians(camera.xRot()))
            )
            .scale(10.0);
         BufferSource vcp = getVCP();
         VertexConsumer vc = vcp.getBuffer(ESP_OUTLINE);
         drawLine(poseStack, vc, start, end, argb, lineWidth);
         vcp.endBatch(ESP_OUTLINE);
      }
   }

   public static void drawWorldLine(PoseStack poseStack, Vec3 a, Vec3 b, int argb, float lineWidth) {
      drawWorldLine(poseStack, a, b, argb, lineWidth, true);
   }

   public static void drawWorldLine(PoseStack poseStack, Vec3 a, Vec3 b, int argb, float lineWidth, boolean throughWalls) {
      Vec3 camOffset = getCameraPos().reverse();
      BufferSource vcp = getVCP();
      RenderType type = throughWalls ? ESP_OUTLINE : ESP_OUTLINE_OCCLUDED;
      VertexConsumer vc = vcp.getBuffer(type);
      drawLine(poseStack, vc, a.add(camOffset), b.add(camOffset), anim(argb), lineWidth);
      vcp.endBatch(type);
   }

   public static void drawLineList(PoseStack poseStack, double[] segs, int[] argb, int count, float lineWidth, boolean throughWalls) {
      if (count > 0) {
         Vec3 cam = getCameraPos();
         BufferSource vcp = getVCP();
         RenderType type = throughWalls ? ESP_OUTLINE : ESP_OUTLINE_OCCLUDED;
         VertexConsumer vc = vcp.getBuffer(type);
         Pose entry = poseStack.last();

         for (int i = 0; i < count; i++) {
            int o = i * 6;
            float x1 = (float)(segs[o] - cam.x);
            float y1 = (float)(segs[o + 1] - cam.y);
            float z1 = (float)(segs[o + 2] - cam.z);
            float x2 = (float)(segs[o + 3] - cam.x);
            float y2 = (float)(segs[o + 4] - cam.y);
            float z2 = (float)(segs[o + 5] - cam.z);
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!(len < 1.0E-6F)) {
               int c = anim(argb[i]);
               line(vc, entry, x1, y1, z1, x2, y2, z2, dx / len, dy / len, dz / len, c, lineWidth);
            }
         }

         vcp.endBatch(type);
      }
   }

   public static void drawTracers(PoseStack poseStack, float partialTick, List<Vec3> worldTargets, int argb) {
      drawTracers(poseStack, partialTick, worldTargets, argb, 2.0F);
   }

   public static void drawTracers(PoseStack poseStack, float partialTick, List<Vec3> worldTargets, int argb, float lineWidth) {
      if (!worldTargets.isEmpty()) {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         if (camera != null) {
            Vec3 camOffset = getCameraPos().reverse();
            Vec3 start = new Vec3(
                  Math.sin(-Math.toRadians(camera.yRot())) * Math.cos(Math.toRadians(camera.xRot())),
                  -Math.sin(Math.toRadians(camera.xRot())),
                  Math.cos(-Math.toRadians(camera.yRot())) * Math.cos(Math.toRadians(camera.xRot()))
               )
               .scale(10.0);
            BufferSource vcp = getVCP();
            VertexConsumer vc = vcp.getBuffer(ESP_OUTLINE);

            for (Vec3 target : worldTargets) {
               Vec3 end = clampTracerEnd(target.add(camOffset));
               drawLine(poseStack, vc, start, end, anim(argb), lineWidth);
            }

            vcp.endBatch(ESP_OUTLINE);
         }
      }
   }

   private static void drawFilledBox(PoseStack ps, VertexConsumer vc, AABB b, int argb) {
      Pose entry = ps.last();
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      vc.addVertex(entry, x1, y1, z1).setColor(argb);
      vc.addVertex(entry, x2, y1, z1).setColor(argb);
      vc.addVertex(entry, x2, y1, z2).setColor(argb);
      vc.addVertex(entry, x1, y1, z2).setColor(argb);
      vc.addVertex(entry, x1, y2, z1).setColor(argb);
      vc.addVertex(entry, x1, y2, z2).setColor(argb);
      vc.addVertex(entry, x2, y2, z2).setColor(argb);
      vc.addVertex(entry, x2, y2, z1).setColor(argb);
      vc.addVertex(entry, x1, y1, z1).setColor(argb);
      vc.addVertex(entry, x1, y2, z1).setColor(argb);
      vc.addVertex(entry, x2, y2, z1).setColor(argb);
      vc.addVertex(entry, x2, y1, z1).setColor(argb);
      vc.addVertex(entry, x1, y1, z2).setColor(argb);
      vc.addVertex(entry, x2, y1, z2).setColor(argb);
      vc.addVertex(entry, x2, y2, z2).setColor(argb);
      vc.addVertex(entry, x1, y2, z2).setColor(argb);
      vc.addVertex(entry, x1, y1, z1).setColor(argb);
      vc.addVertex(entry, x1, y1, z2).setColor(argb);
      vc.addVertex(entry, x1, y2, z2).setColor(argb);
      vc.addVertex(entry, x1, y2, z1).setColor(argb);
      vc.addVertex(entry, x2, y1, z1).setColor(argb);
      vc.addVertex(entry, x2, y2, z1).setColor(argb);
      vc.addVertex(entry, x2, y2, z2).setColor(argb);
      vc.addVertex(entry, x2, y1, z2).setColor(argb);
   }

   private static void drawOutlineBox(PoseStack ps, VertexConsumer vc, AABB b, int argb) {
      drawOutlineBox(ps, vc, b, argb, 2.0F);
   }

   private static void drawOutlineBox(PoseStack ps, VertexConsumer vc, AABB b, int argb, float lineWidth) {
      Pose entry = ps.last();
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      line(vc, entry, x1, y1, z1, x2, y1, z1, 1.0F, 0.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x2, y1, z1, x2, y1, z2, 0.0F, 0.0F, 1.0F, argb, lineWidth);
      line(vc, entry, x2, y1, z2, x1, y1, z2, -1.0F, 0.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x1, y1, z2, x1, y1, z1, 0.0F, 0.0F, -1.0F, argb, lineWidth);
      line(vc, entry, x1, y2, z1, x2, y2, z1, 1.0F, 0.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x2, y2, z1, x2, y2, z2, 0.0F, 0.0F, 1.0F, argb, lineWidth);
      line(vc, entry, x2, y2, z2, x1, y2, z2, -1.0F, 0.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x1, y2, z2, x1, y2, z1, 0.0F, 0.0F, -1.0F, argb, lineWidth);
      line(vc, entry, x1, y1, z1, x1, y2, z1, 0.0F, 1.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x2, y1, z1, x2, y2, z1, 0.0F, 1.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x2, y1, z2, x2, y2, z2, 0.0F, 1.0F, 0.0F, argb, lineWidth);
      line(vc, entry, x1, y1, z2, x1, y2, z2, 0.0F, 1.0F, 0.0F, argb, lineWidth);
   }

   private static void drawCornerBox(PoseStack ps, VertexConsumer vc, AABB b, int argb, float lineWidth) {
      Pose entry = ps.last();
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float len = Math.max(0.1F, Math.min(Math.min(dx, dy), dz) * 0.25F);

      for (int xi = 0; xi < 2; xi++) {
         for (int yi = 0; yi < 2; yi++) {
            for (int zi = 0; zi < 2; zi++) {
               float cx = xi == 0 ? x1 : x2;
               float cy = yi == 0 ? y1 : y2;
               float cz = zi == 0 ? z1 : z2;
               float ddx = xi == 0 ? len : -len;
               float ddy = yi == 0 ? len : -len;
               float ddz = zi == 0 ? len : -len;
               line(vc, entry, cx, cy, cz, cx + ddx, cy, cz, ddx, 0.0F, 0.0F, argb, lineWidth);
               line(vc, entry, cx, cy, cz, cx, cy + ddy, cz, 0.0F, ddy, 0.0F, argb, lineWidth);
               line(vc, entry, cx, cy, cz, cx, cy, cz + ddz, 0.0F, 0.0F, ddz, argb, lineWidth);
            }
         }
      }
   }

   private static void drawLine(PoseStack ps, VertexConsumer vc, Vec3 start, Vec3 end, int argb) {
      drawLine(ps, vc, start, end, argb, 2.0F);
   }

   private static void drawLine(PoseStack ps, VertexConsumer vc, Vec3 start, Vec3 end, int argb, float lineWidth) {
      Pose entry = ps.last();
      float x1 = (float)start.x;
      float y1 = (float)start.y;
      float z1 = (float)start.z;
      float x2 = (float)end.x;
      float y2 = (float)end.y;
      float z2 = (float)end.z;
      Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
      vc.addVertex(entry, x1, y1, z1).setColor(argb).setNormal(entry, normal.x, normal.y, normal.z).setLineWidth(lineWidth);
      vc.addVertex(entry, x2, y2, z2).setColor(argb).setNormal(entry, normal.x, normal.y, normal.z).setLineWidth(lineWidth);
   }

   private static void line(VertexConsumer vc, Pose entry, float x1, float y1, float z1, float x2, float y2, float z2, float nx, float ny, float nz, int argb) {
      line(vc, entry, x1, y1, z1, x2, y2, z2, nx, ny, nz, argb, 2.0F);
   }

   private static void line(VertexConsumer vc, Pose entry, float x1, float y1, float z1, float x2, float y2, float z2, float nx, float ny, float nz, int argb, float lineWidth) {
      vc.addVertex(entry, x1, y1, z1).setColor(argb).setNormal(entry, nx, ny, nz).setLineWidth(lineWidth);
      vc.addVertex(entry, x2, y2, z2).setColor(argb).setNormal(entry, nx, ny, nz).setLineWidth(lineWidth);
   }

   public static final class CosmeticBatch implements CosmeticSink {
      private final BufferSource vcp;
      private final RenderType type;
      private final VertexConsumer vc;
      private final Pose pose;
      private final Vec3 cam = ClarityRenderUtils.getCameraPos();

      private CosmeticBatch(PoseStack poseStack, int mode) {
         this.vcp = ClarityRenderUtils.getVCP();
         this.type = mode == 2 ? ClarityRenderUtils.wingChamsType() : (mode == 1 ? ClarityRenderUtils.WINGS_WALLS : ClarityRenderUtils.WINGS_OCCLUDED);
         this.vc = this.vcp.getBuffer(this.type);
         this.pose = poseStack.last();
      }

      private void put(double[] p, int argb) {
         this.vc.addVertex(this.pose, (float)(p[0] - this.cam.x), (float)(p[1] - this.cam.y), (float)(p[2] - this.cam.z)).setColor(ClarityRenderUtils.anim(argb));
      }

      @Override
      public void quad(double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
         this.put(a, ca);
         this.put(b, cb);
         this.put(c, cc);
         this.put(d, cd);
      }

      public void end() {
         this.vcp.endBatch(this.type);
      }
   }

   public record ESPEntry(AABB box, int argb) {
   }

   public static final class OrderedBatch implements CosmeticSink {
      private static float[] pos = new float[12288];
      private static int[] col = new int[4096];
      private static long[] keys = new long[1024];
      private final BufferSource vcp;
      private final RenderType type;
      private final VertexConsumer vc;
      private final Pose pose;
      private final Vec3 cam = ClarityRenderUtils.getCameraPos();
      private int n;

      private OrderedBatch(PoseStack poseStack, int mode) {
         this.vcp = ClarityRenderUtils.getVCP();
         this.type = ClarityRenderUtils.orderedType(mode);
         this.vc = this.vcp.getBuffer(this.type);
         this.pose = poseStack.last();
      }

      @Override
      public void quad(double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
         if (this.n == keys.length) {
            keys = Arrays.copyOf(keys, this.n * 2);
            pos = Arrays.copyOf(pos, this.n * 24);
            col = Arrays.copyOf(col, this.n * 8);
         }

         int o = this.n * 12;
         double cx = 0.0;
         double cy = 0.0;
         double cz = 0.0;

         for (int i = 0; i < 4; i++) {
            double[] p = i == 0 ? a : (i == 1 ? b : (i == 2 ? c : d));
            float x = (float)(p[0] - this.cam.x);
            float y = (float)(p[1] - this.cam.y);
            float z = (float)(p[2] - this.cam.z);
            pos[o + i * 3] = x;
            pos[o + i * 3 + 1] = y;
            pos[o + i * 3 + 2] = z;
            cx += x;
            cy += y;
            cz += z;
         }

         cx *= 0.25;
         cy *= 0.25;
         cz *= 0.25;
         int q = this.n * 4;
         col[q] = ca;
         col[q + 1] = cb;
         col[q + 2] = cc;
         col[q + 3] = cd;
         float dist = (float)(cx * cx + cy * cy + cz * cz);
         keys[this.n] = (long)Float.floatToIntBits(dist) << 32 | this.n;
         this.n++;
      }

      public void nextLayer() {
         if (this.n != 0) {
            Arrays.sort(keys, 0, this.n);

            for (int k = this.n - 1; k >= 0; k--) {
               int i = (int)(keys[k] & 4294967295L);
               int o = i * 12;
               int q = i * 4;

               for (int j = 0; j < 4; j++) {
                  this.vc.addVertex(this.pose, pos[o + j * 3], pos[o + j * 3 + 1], pos[o + j * 3 + 2]).setColor(ClarityRenderUtils.anim(col[q + j]));
               }
            }

            this.n = 0;
         }
      }

      public void end() {
         this.nextLayer();
         this.vcp.endBatch(this.type);
      }
   }

   public static final class TexBatch {
      private final BufferSource vcp;
      private final RenderType type;
      private final VertexConsumer vc;
      private final Pose pose;
      private final Vec3 cam;

      private TexBatch(BufferSource vcp, RenderType type, PoseStack ps, Vec3 cam) {
         this.vcp = vcp;
         this.type = type;
         this.vc = vcp.getBuffer(type);
         this.pose = ps.last();
         this.cam = cam;
      }

      public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
         argb = ClarityRenderUtils.anim(argb);
         Vec3 n = b.subtract(a).cross(c.subtract(a));
         boolean front = n.dot(this.cam.subtract(a)) >= 0.0;
         if (front) {
            this.vert(a, 0.0F, 0.0F, argb);
            this.vert(b, 1.0F, 0.0F, argb);
            this.vert(c, 1.0F, 1.0F, argb);
            this.vert(d, 0.0F, 1.0F, argb);
         } else {
            this.vert(d, 0.0F, 1.0F, argb);
            this.vert(c, 1.0F, 1.0F, argb);
            this.vert(b, 1.0F, 0.0F, argb);
            this.vert(a, 0.0F, 0.0F, argb);
         }
      }

      public void billboard(Vec3 center, double halfW, double halfH, double angle, int argb) {
         Vec3 toCam = this.cam.subtract(center);
         double d = toCam.length();
         if (!(d < 1.0E-4)) {
            toCam = toCam.scale(1.0 / d);
            Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(toCam);
            double rl = right.length();
            right = rl < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.scale(1.0 / rl);
            Vec3 up = toCam.cross(right);
            if (angle != 0.0) {
               double cs = Math.cos(angle);
               double sn = Math.sin(angle);
               Vec3 r2 = right.scale(cs).add(up.scale(sn));
               Vec3 u2 = up.scale(cs).subtract(right.scale(sn));
               right = r2;
               up = u2;
            }

            Vec3 rw = right.scale(halfW);
            Vec3 uh = up.scale(halfH);
            this.quad(center.subtract(rw).add(uh), center.add(rw).add(uh), center.add(rw).subtract(uh), center.subtract(rw).subtract(uh), argb);
         }
      }

      public void flat(Vec3 c, double r, double angle, int argb) {
         double cs = Math.cos(angle) * r;
         double sn = Math.sin(angle) * r;
         Vec3 a = c.add(-cs + sn, 0.0, -sn - cs);
         Vec3 b = c.add(cs + sn, 0.0, sn - cs);
         Vec3 cc = c.add(cs - sn, 0.0, sn + cs);
         Vec3 d = c.add(-cs - sn, 0.0, -sn + cs);
         this.quad(a, b, cc, d, argb);
      }

      private void vert(Vec3 p, float u, float v, int argb) {
         this.vc
            .addVertex(this.pose, (float)(p.x - this.cam.x), (float)(p.y - this.cam.y), (float)(p.z - this.cam.z))
            .setColor(argb)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(this.pose, 0.0F, 1.0F, 0.0F);
      }

      public void end() {
         this.vcp.endBatch(this.type);
      }
   }
}
