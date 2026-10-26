package need.clarity.render.esp;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import need.clarity.render.RawShaderDrawUtil;
import need.clarity.render.Shader;
import need.clarity.render.ShaderHelper;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public final class SkeletonGlowRenderer {
   private static Shader maskShader;
   private static Shader blurShader;
   private static Shader compShader;
   private static boolean initDone = false;
   private static boolean initFailed = false;
   private static final SkeletonGlowRenderer.Target mask = new SkeletonGlowRenderer.Target();
   private static final SkeletonGlowRenderer.Target ga = new SkeletonGlowRenderer.Target();
   private static final SkeletonGlowRenderer.Target gb = new SkeletonGlowRenderer.Target();
   private static final Matrix4f proj = new Matrix4f();
   private static final Matrix4f vp = new Matrix4f();
   private static final Vector4f va = new Vector4f();
   private static final Vector4f vb = new Vector4f();
   private static final float NEAR_W = 0.05F;
   private static float[] quadPos = new float[3072];
   private static int[] quadCol = new int[256];

   private SkeletonGlowRenderer() {
   }

   public static void render(PoseStack poseStack, double[] segs, int[] argb, int count, boolean throughWalls, float lineWidth, float radius, float intensity, int overrideRgb) {
      if (count > 0 && !(intensity <= 0.0F)) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.getMainRenderTarget();
         if (main != null && main.width > 0 && main.height > 0) {
            if (ensureInit()) {
               if (SkyShaderRenderer.levelProjection(proj)) {
                  proj.mul(poseStack.last().pose(), vp);
                  int w = main.width;
                  int h = main.height;
                  float px = h / 1080.0F;
                  float halfWidthPx = Math.max(1.5F, lineWidth * 1.25F) * px;

                  try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
                     GlStateManager._activeTexture(33984);
                     int prevSampler = GL11.glGetInteger(35097);
                     GL33.glBindSampler(0, 0);

                     try {
                        if (ensure(mask, w, h) && ensure(ga, w / 2, h / 2) && ensure(gb, w / 2, h / 2)) {
                           boolean depthOk = false;
                           GL30.glBindFramebuffer(36160, mask.fbo);
                           if (!throughWalls) {
                              int depthId = RawFramebufferUtil.getDepthTextureId(main);
                              if (depthId != 0) {
                                 GL30.glFramebufferTexture2D(36160, 36096, 3553, depthId, 0);
                                 depthOk = GL30.glCheckFramebufferStatus(36160) == 36053;
                                 if (!depthOk) {
                                    GL30.glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
                                 }
                              }
                           }

                           GL11.glDrawBuffer(36064);
                           GlStateManager._viewport(0, 0, w, h);
                           GlStateManager._disableScissorTest();
                           GlStateManager._disableCull();
                           GL11.glColorMask(true, true, true, true);
                           GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                           GL11.glClear(16384);
                           if (depthOk) {
                              GlStateManager._enableDepthTest();
                              GL11.glDepthFunc(515);
                              GL11.glDepthMask(false);
                           } else {
                              GlStateManager._disableDepthTest();
                           }

                           GlStateManager._enableBlend();
                           GlStateManager._blendFuncSeparate(1, 1, 1, 1);
                           GL14.glBlendEquation(32776);
                           boolean any = buildAndDrawMask(segs, argb, count, w, h, halfWidthPx, overrideRgb);
                           GL14.glBlendEquation(32774);
                           GlStateManager._disableDepthTest();
                           if (depthOk) {
                              GL30.glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
                           }

                           if (!any) {
                              return;
                           }

                           GlStateManager._disableBlend();
                           GL30.glBindFramebuffer(36008, mask.fbo);
                           GL30.glBindFramebuffer(36009, ga.fbo);
                           GL30.glBlitFramebuffer(0, 0, mask.w, mask.h, 0, 0, ga.w, ga.h, 16384, 9729);
                           float sigma = Math.min(16.0F, Math.max(0.6F, radius * px / 2.0F));
                           blurShader.bind();
                           blurShader.setUniform1i("Source", 0);
                           blurShader.setUniform1f("sigma", sigma);
                           target(gb);
                           bind(ga.tex);
                           blurShader.setUniform2f("direction", 1.0F / ga.w, 0.0F);
                           ShaderHelper.drawFullScreenQuad();
                           target(ga);
                           bind(gb.tex);
                           blurShader.setUniform2f("direction", 0.0F, 1.0F / gb.h);
                           ShaderHelper.drawFullScreenQuad();
                           blurShader.unbind();
                           if (!RawFramebufferUtil.bindDirect(main)) {
                              return;
                           }

                           GlStateManager._disableDepthTest();
                           GlStateManager._enableBlend();
                           GlStateManager._blendFuncSeparate(1, 771, 1, 771);
                           GL14.glBlendEquation(32774);
                           bind(ga.tex);
                           compShader.bind();
                           compShader.setUniform1i("Glow", 0);
                           compShader.setUniform1f("intensity", intensity);
                           compShader.setUniform1i("useOverride", overrideRgb >= 0 ? 1 : 0);
                           if (overrideRgb >= 0) {
                              compShader.setUniform3f("overrideColor", (overrideRgb >> 16 & 0xFF) / 255.0F, (overrideRgb >> 8 & 0xFF) / 255.0F, (overrideRgb & 0xFF) / 255.0F);
                           }

                           ShaderHelper.drawFullScreenQuad();
                           compShader.unbind();
                           return;
                        }
                     } finally {
                        GL33.glBindSampler(0, prevSampler);
                        GlStateManager._activeTexture(33984);
                     }
                  } catch (Throwable var28) {
                  }
               }
            }
         }
      }
   }

   private static boolean buildAndDrawMask(double[] segs, int[] argb, int count, int w, int h, float halfPx, int overrideRgb) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      if (quadPos.length < count * 12) {
         quadPos = new float[count * 12];
         quadCol = new int[count];
      }

      int quads = 0;

      for (int i = 0; i < count; i++) {
         int o = i * 6;
         va.set((float)(segs[o] - cam.x), (float)(segs[o + 1] - cam.y), (float)(segs[o + 2] - cam.z), 1.0F);
         vb.set((float)(segs[o + 3] - cam.x), (float)(segs[o + 4] - cam.y), (float)(segs[o + 5] - cam.z), 1.0F);
         vp.transform(va);
         vp.transform(vb);
         boolean ina = va.w > 0.05F;
         boolean inb = vb.w > 0.05F;
         if (ina || inb) {
            if (!ina || !inb) {
               float t = (0.05F - va.w) / (vb.w - va.w);
               Vector4f out = ina ? vb : va;
               out.set(va.x + (vb.x - va.x) * t, va.y + (vb.y - va.y) * t, va.z + (vb.z - va.z) * t, 0.05F);
            }

            float ax = va.x / va.w;
            float ay = va.y / va.w;
            float az = va.z / va.w;
            float bx = vb.x / vb.w;
            float by = vb.y / vb.w;
            float bz = vb.z / vb.w;
            float dxp = (bx - ax) * w * 0.5F;
            float dyp = (by - ay) * h * 0.5F;
            float len = (float)Math.sqrt(dxp * dxp + dyp * dyp);
            float tx;
            float ty;
            if (len < 0.001F) {
               tx = 1.0F;
               ty = 0.0F;
            } else {
               tx = dxp / len;
               ty = dyp / len;
            }

            float nx = -ty * halfPx * 2.0F / w;
            float ny = tx * halfPx * 2.0F / h;
            float ex = tx * halfPx * 2.0F / w;
            float ey = ty * halfPx * 2.0F / h;
            int c = argb[i];
            int col = 0xFF000000 | (overrideRgb >= 0 ? overrideRgb : c & 16777215);
            float a = (c >>> 24 & 0xFF) / 255.0F;
            if (a < 0.999F) {
               int r = Math.round((col >> 16 & 0xFF) * a);
               int g = Math.round((col >> 8 & 0xFF) * a);
               int b = Math.round((col & 0xFF) * a);
               col = Math.round(a * 255.0F) << 24 | r << 16 | g << 8 | b;
            }

            float[] q = quadPos;
            int k = quads * 12;
            q[k] = ax - ex + nx;
            q[k + 1] = ay - ey + ny;
            q[k + 2] = az;
            q[k + 3] = ax - ex - nx;
            q[k + 4] = ay - ey - ny;
            q[k + 5] = az;
            q[k + 6] = bx + ex - nx;
            q[k + 7] = by + ey - ny;
            q[k + 8] = bz;
            q[k + 9] = bx + ex + nx;
            q[k + 10] = by + ey + ny;
            q[k + 11] = bz;
            quadCol[quads] = col;
            quads++;
         }
      }

      if (quads == 0) {
         return false;
      }

      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i < quads; i++) {
         int k = i * 12;
         int col = quadCol[i];

         for (int v = 0; v < 4; v++) {
            buf.addVertex(quadPos[k + v * 3], quadPos[k + v * 3 + 1], quadPos[k + v * 3 + 2]).setColor(col);
         }
      }

      maskShader.bind();
      RawShaderDrawUtil.draw(buf.buildOrThrow());
      maskShader.unbind();
      return true;
   }

   private static boolean ensureInit() {
      if (initDone) {
         return true;
      }

      if (initFailed) {
         return false;
      }

      try {
         maskShader = new Shader("esp", "skeleton_mask");
         blurShader = new Shader("hand", "fx_blur");
         compShader = new Shader("esp", "skeleton_glow");
         initDone = true;
      } catch (Exception e) {
         initFailed = true;
      }

      return initDone;
   }

   private static void target(SkeletonGlowRenderer.Target t) {
      GL30.glBindFramebuffer(36160, t.fbo);
      GL11.glDrawBuffer(36064);
      GlStateManager._viewport(0, 0, t.w, t.h);
   }

   private static void bind(int tex) {
      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(tex);
   }

   private static boolean ensure(SkeletonGlowRenderer.Target t, int w, int h) {
      w = Math.max(1, w);
      h = Math.max(1, h);
      if (t.fbo != 0 && t.w == w && t.h == h) {
         return true;
      }

      if (t.tex == 0) {
         t.tex = GL11.glGenTextures();
      }

      GlStateManager._activeTexture(33984);
      int prev = GL11.glGetInteger(32873);
      GlStateManager._bindTexture(t.tex);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexParameteri(3553, 33085, 0);
      int prevPbo = GL11.glGetInteger(35055);
      GL15.glBindBuffer(35052, 0);
      GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
      GL15.glBindBuffer(35052, prevPbo);
      GlStateManager._bindTexture(prev);
      if (t.fbo == 0) {
         t.fbo = GL30.glGenFramebuffers();
      }

      int prevFbo = GL11.glGetInteger(36006);
      GL30.glBindFramebuffer(36160, t.fbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, t.tex, 0);
      boolean ok = GL30.glCheckFramebufferStatus(36160) == 36053;
      GL30.glBindFramebuffer(36160, prevFbo);
      t.w = ok ? w : 0;
      t.h = ok ? h : 0;
      return ok;
   }

   private static final class Target {
      int tex;
      int fbo;
      int w;
      int h;
   }
}
