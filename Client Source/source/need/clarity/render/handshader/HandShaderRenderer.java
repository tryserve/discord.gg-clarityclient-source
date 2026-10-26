package need.clarity.render.handshader;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

public class HandShaderRenderer {
   public static boolean itemGlowContext = false;
   private static Identifier whiteTextureId = null;
   private static DynamicTexture whiteTexture = null;
   private static Identifier glowRampTextureId = null;
   private static DynamicTexture glowRampTexture = null;

   public static Identifier getWhiteTexture() {
      if (whiteTextureId == null) {
         whiteTextureId = Identifier.fromNamespaceAndPath("clarity", "textures/misc/white.png");

         try {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixel(0, 0, -1);
            whiteTexture = new DynamicTexture(() -> "hand_shader_white", img);
            Minecraft.getInstance().getTextureManager().register(whiteTextureId, whiteTexture);
         } catch (Throwable var1) {
         }
      }

      return whiteTextureId;
   }

   public static Identifier getGlowRampTexture() {
      if (glowRampTextureId == null) {
         glowRampTextureId = Identifier.fromNamespaceAndPath("clarity", "textures/misc/glow_ramp.png");

         try {
            int width = 64;
            int height = 4;
            NativeImage img = new NativeImage(width, height, false);

            for (int y = 0; y < height; y++) {
               for (int x = 0; x < width; x++) {
                  float t = (float)x / (width - 1);
                  float falloff = (float)(Math.exp(-t * t * 3.5) * (1.0 - t * t));
                  int alpha = Math.max(0, Math.min(255, Math.round(falloff * 255.0F)));
                  img.setPixel(x, y, alpha << 24 | 16777215);
               }
            }

            glowRampTexture = new DynamicTexture(() -> "hand_shader_glow_ramp", img);
            Minecraft.getInstance().getTextureManager().register(glowRampTextureId, glowRampTexture);
         } catch (Throwable var8) {
         }
      }

      return glowRampTextureId;
   }

   public static void renderCustomArm(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier texture, ModelPart armPart) {
      int effectiveLight = HandShaderConfig.getEffectiveLight(light);
      Identifier armTex = HandShaderConfig.textured ? texture : getWhiteTexture();
      RenderType baseRenderType = HandShaderConfig.fullbright ? RenderTypes.entityTranslucentEmissive(armTex) : RenderTypes.entityTranslucent(armTex);
      int color = HandShaderConfig.getArmColor();
      collector.submitModelPart(armPart, poseStack, baseRenderType, effectiveLight, OverlayTexture.NO_OVERLAY, null, false, false, color, null, 0);
      if (HandShaderConfig.outline) {
         int outCol = HandShaderConfig.getEffectiveOutlineColor();
         float coreWidth = (float)HandShaderConfig.outlineWidth;
         armPart.visit(poseStack, (pose, name, index, cube) -> {
            float x0 = cube.minX / 16.0F;
            float x1 = cube.maxX / 16.0F;
            float y0 = cube.minY / 16.0F;
            float y1 = cube.maxY / 16.0F;
            float z0 = cube.minZ / 16.0F;
            float z1 = cube.maxZ / 16.0F;
            if (HandShaderConfig.outlineGlow) {
               float spread = (float)HandShaderConfig.glowSpread;
               int r = outCol >> 16 & 0xFF;
               int g = outCol >> 8 & 0xFF;
               int b = outCol & 0xFF;
               int baseA = outCol >> 24 & 0xFF;
               int glowAlpha = Math.max(1, Math.min(255, Math.round(baseA * 0.55F)));
               int glowColor = glowAlpha << 24 | r << 16 | g << 8 | b;
               float radius = 0.04F + 0.035F * spread;
               renderSilhouetteGlow(collector, poseStack, x0, y0, z0, x1, y1, z1, glowColor, radius);
            }

            collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (p, consumer) -> drawBoxEdges(consumer, p, x0, y0, z0, x1, y1, z1, outCol, coreWidth, 7.0E-4F));
            if (HandShaderConfig.antialiasing) {
               int r = outCol >> 16 & 0xFF;
               int g = outCol >> 8 & 0xFF;
               int b = outCol & 0xFF;
               int baseA = outCol >> 24 & 0xFF;
               int aaA = Math.max(1, Math.round(baseA * 0.22F));
               int aaColor = aaA << 24 | r << 16 | g << 8 | b;
               float aaW = coreWidth + 0.6F;
               float ext = 7.0E-4F * (aaW / coreWidth);
               collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (p, consumer) -> drawBoxEdges(consumer, p, x0, y0, z0, x1, y1, z1, aaColor, aaW, ext));
            }
         });
      }
   }

   private static void drawBoxEdges(VertexConsumer consumer, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, float lineWidth, float ext) {
      float minX = x0 - ext;
      float maxX = x1 + ext;
      float minY = y0 - ext;
      float maxY = y1 + ext;
      float minZ = z0 - ext;
      float maxZ = z1 + ext;
      drawEdge(consumer, pose, minX, y0, z0, maxX, y0, z0, color, lineWidth);
      drawEdge(consumer, pose, x1, y0, minZ, x1, y0, maxZ, color, lineWidth);
      drawEdge(consumer, pose, maxX, y0, z1, minX, y0, z1, color, lineWidth);
      drawEdge(consumer, pose, x0, y0, maxZ, x0, y0, minZ, color, lineWidth);
      drawEdge(consumer, pose, minX, y1, z0, maxX, y1, z0, color, lineWidth);
      drawEdge(consumer, pose, x1, y1, minZ, x1, y1, maxZ, color, lineWidth);
      drawEdge(consumer, pose, maxX, y1, z1, minX, y1, z1, color, lineWidth);
      drawEdge(consumer, pose, x0, y1, maxZ, x0, y1, minZ, color, lineWidth);
      drawEdge(consumer, pose, x0, minY, z0, x0, maxY, z0, color, lineWidth);
      drawEdge(consumer, pose, x1, minY, z0, x1, maxY, z0, color, lineWidth);
      drawEdge(consumer, pose, x1, minY, z1, x1, maxY, z1, color, lineWidth);
      drawEdge(consumer, pose, x0, minY, z1, x0, maxY, z1, color, lineWidth);
   }

   private static void drawEdge(VertexConsumer consumer, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int color, float lineWidth) {
      float nx = x2 - x1;
      float ny = y2 - y1;
      float nz = z2 - z1;
      float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (len > 1.0E-5F) {
         nx /= len;
         ny /= len;
         nz /= len;
      }

      consumer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(lineWidth);
      consumer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(lineWidth);
   }

   private static void renderSilhouetteGlow(SubmitNodeCollector collector, PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, int glowColor, float radius) {
      collector.submitCustomGeometry(
         poseStack,
         RenderTypes.entityTranslucentEmissive(getWhiteTexture()),
         (pose, consumer) -> {
            float[][] verts = new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
            float invS2 = 0.70710677F;
            float[][] edges = new float[][]{
               {7.0F, 6.0F, 0.0F, invS2, invS2},
               {6.0F, 2.0F, invS2, invS2, 0.0F},
               {2.0F, 3.0F, 0.0F, invS2, -invS2},
               {3.0F, 7.0F, -invS2, invS2, 0.0F},
               {4.0F, 5.0F, 0.0F, -invS2, invS2},
               {5.0F, 1.0F, invS2, -invS2, 0.0F},
               {1.0F, 0.0F, 0.0F, -invS2, -invS2},
               {0.0F, 4.0F, -invS2, -invS2, 0.0F},
               {4.0F, 7.0F, -invS2, 0.0F, invS2},
               {5.0F, 6.0F, invS2, 0.0F, invS2},
               {1.0F, 2.0F, invS2, 0.0F, -invS2},
               {0.0F, 3.0F, -invS2, 0.0F, -invS2}
            };
            float ext = radius * invS2;

            for (float[] e : edges) {
               int iA = (int)e[0];
               int iB = (int)e[1];
               float nx = e[2];
               float ny = e[3];
               float nz = e[4];
               float ax = verts[iA][0];
               float ay = verts[iA][1];
               float az = verts[iA][2];
               float bx = verts[iB][0];
               float by = verts[iB][1];
               float bz = verts[iB][2];
               float dx = bx - ax;
               float dy = by - ay;
               float dz = bz - az;
               float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
               float tx = len > 1.0E-4F ? dx / len : 0.0F;
               float ty = len > 1.0E-4F ? dy / len : 0.0F;
               float tz = len > 1.0E-4F ? dz / len : 0.0F;
               float ox = nx * radius;
               float oy = ny * radius;
               float oz = nz * radius;
               float ex = tx * ext;
               float ey = ty * ext;
               float ez = tz * ext;
               float aOutX = ax + ox - ex;
               float aOutY = ay + oy - ey;
               float aOutZ = az + oz - ez;
               float bOutX = bx + ox + ex;
               float bOutY = by + oy + ey;
               float bOutZ = bz + oz + ez;
               emitGlowQuad(consumer, pose, ax, ay, az, bx, by, bz, bOutX, bOutY, bOutZ, aOutX, aOutY, aOutZ, nx, ny, nz, glowColor);
            }
         }
      );
   }

   private static void emitGlowQuad(
      VertexConsumer consumer,
      Pose pose,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float nx,
      float ny,
      float nz,
      int color
   ) {
      int tip = color & 16777215;
      consumer.addVertex(pose, x1, y1, z1).setColor(color).setUv(0.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
      consumer.addVertex(pose, x2, y2, z2).setColor(color).setUv(0.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
      consumer.addVertex(pose, x3, y3, z3).setColor(tip).setUv(1.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
      consumer.addVertex(pose, x4, y4, z4).setColor(tip).setUv(1.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
      consumer.addVertex(pose, x4, y4, z4).setColor(tip).setUv(1.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, -nx, -ny, -nz);
      consumer.addVertex(pose, x3, y3, z3).setColor(tip).setUv(1.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, -nx, -ny, -nz);
      consumer.addVertex(pose, x2, y2, z2).setColor(color).setUv(0.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, -nx, -ny, -nz);
      consumer.addVertex(pose, x1, y1, z1).setColor(color).setUv(0.0F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, -nx, -ny, -nz);
   }

   public static void renderItemGlowBox(PoseStack poseStack, SubmitNodeCollector collector, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (HandShaderConfig.outline) {
         int outCol = HandShaderConfig.getEffectiveOutlineColor();
         float coreWidth = (float)HandShaderConfig.outlineWidth;
         float x0 = (float)minX;
         float y0 = (float)minY;
         float z0 = (float)minZ;
         float x1 = (float)maxX;
         float y1 = (float)maxY;
         float z1 = (float)maxZ;
         if (HandShaderConfig.outlineGlow) {
            float spread = (float)HandShaderConfig.glowSpread;
            int r = outCol >> 16 & 0xFF;
            int g = outCol >> 8 & 0xFF;
            int b = outCol & 0xFF;
            int baseA = outCol >> 24 & 0xFF;
            int glowAlpha = Math.max(1, Math.min(255, Math.round(baseA * 0.55F)));
            int glowColor = glowAlpha << 24 | r << 16 | g << 8 | b;
            float radius = 0.04F + 0.035F * spread;
            renderSilhouetteGlow(collector, poseStack, x0, y0, z0, x1, y1, z1, glowColor, radius);
         }

         collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (p2, consumer) -> drawBoxEdges(consumer, p2, x0, y0, z0, x1, y1, z1, outCol, coreWidth, 7.0E-4F));
      }
   }

   public static void renderItemGlowQuads(PoseStack poseStack, SubmitNodeCollector collector, List<BakedQuad> quads) {
      if (HandShaderConfig.outline) {
         if (quads != null && !quads.isEmpty()) {
            int outCol = HandShaderConfig.getEffectiveOutlineColor();
            float coreWidth = (float)HandShaderConfig.outlineWidth;
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;

            for (BakedQuad q : quads) {
               for (int i = 0; i < 4; i++) {
                  Vector3fc p = q.position(i);
                  if (p != null) {
                     if (p.x() < minX) {
                        minX = p.x();
                     }

                     if (p.y() < minY) {
                        minY = p.y();
                     }

                     if (p.z() < minZ) {
                        minZ = p.z();
                     }

                     if (p.x() > maxX) {
                        maxX = p.x();
                     }

                     if (p.y() > maxY) {
                        maxY = p.y();
                     }

                     if (p.z() > maxZ) {
                        maxZ = p.z();
                     }
                  }
               }
            }

            float maxDim = (float)Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
            if (!(maxDim > 0.0F)) {
               maxDim = 1.0F;
            }

            collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (pose, consumer) -> {
               for (BakedQuad q : quads) {
                  Vector3fc a = q.position0();
                  Vector3fc b = q.position1();
                  Vector3fc c = q.position2();
                  Vector3fc d = q.position3();
                  drawEdge(consumer, pose, a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), outCol, coreWidth);
                  drawEdge(consumer, pose, b.x(), b.y(), b.z(), c.x(), c.y(), c.z(), outCol, coreWidth);
                  drawEdge(consumer, pose, c.x(), c.y(), c.z(), d.x(), d.y(), d.z(), outCol, coreWidth);
                  drawEdge(consumer, pose, d.x(), d.y(), d.z(), a.x(), a.y(), a.z(), outCol, coreWidth);
               }
            });
            if (HandShaderConfig.outlineGlow) {
               float spread = (float)HandShaderConfig.glowSpread;
               int r = outCol >> 16 & 0xFF;
               int g = outCol >> 8 & 0xFF;
               int b2 = outCol & 0xFF;
               int baseA = outCol >> 24 & 0xFF;
               int glowAlpha = Math.max(1, Math.min(255, Math.round(baseA * 0.55F)));
               int glowColor = glowAlpha << 24 | r << 16 | g << 8 | b2;
               float radius = maxDim * (0.045F + 0.04F * spread);
               collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(getWhiteTexture()), (pose, consumer) -> {
                  for (BakedQuad q : quads) {
                     Direction dir = q.direction();
                     float nx = dir.getStepX();
                     float ny = dir.getStepY();
                     float nz = dir.getStepZ();
                     Vector3fc a = q.position0();
                     Vector3fc b = q.position1();
                     Vector3fc c = q.position2();
                     Vector3fc d = q.position3();
                     drawEdgeGlow(consumer, pose, a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), nx, ny, nz, glowColor, radius);
                     drawEdgeGlow(consumer, pose, b.x(), b.y(), b.z(), c.x(), c.y(), c.z(), nx, ny, nz, glowColor, radius);
                     drawEdgeGlow(consumer, pose, c.x(), c.y(), c.z(), d.x(), d.y(), d.z(), nx, ny, nz, glowColor, radius);
                     drawEdgeGlow(consumer, pose, d.x(), d.y(), d.z(), a.x(), a.y(), a.z(), nx, ny, nz, glowColor, radius);
                  }
               });
            }
         }
      }
   }

   private static void drawEdgeGlow(VertexConsumer consumer, Pose pose, float ax, float ay, float az, float bx, float by, float bz, float nx, float ny, float nz, int glowColor, float radius) {
      float dx = bx - ax;
      float dy = by - ay;
      float dz = bz - az;
      float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      float tx = 0.0F;
      float ty = 0.0F;
      float tz = 0.0F;
      if (len > 1.0E-5F) {
         tx = dx / len;
         ty = dy / len;
         tz = dz / len;
      }

      float ext = radius * 0.70710677F;
      float ox = nx * radius;
      float oy = ny * radius;
      float oz = nz * radius;
      float ex = tx * ext;
      float ey = ty * ext;
      float ez = tz * ext;
      emitGlowQuad(consumer, pose, ax, ay, az, bx, by, bz, bx + ox + ex, by + oy + ey, bz + oz + ez, ax + ox - ex, ay + oy - ey, az + oz - ez, nx, ny, nz, glowColor);
   }
}
