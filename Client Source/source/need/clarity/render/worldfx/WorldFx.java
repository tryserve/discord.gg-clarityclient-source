package need.clarity.render.worldfx;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import need.clarity.render.Shader;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

public final class WorldFx {
   public static final String TRAIL = "trail";
   public static final String RING = "jumpring";
   public static final String WINGS = "wings";
   public static final String HATS = "hats";
   public static final String WEATHER = "weather";
   public static final String BURST = "burst";
   public static final String AURA_PARTICLES = "aurap";
   public static final String TARGET = "target";
   public static final String AURA = "aura";
   public static final String KILL = "killfx";
   public static final String BEATS = "beats";
   public static final String CORPSE = "corpse";
   public static final String PET_SHADOW = "petshadow";
   private static final int FLOATS = 11;
   private static FloatBuffer buf = MemoryUtil.memAllocFloat(67584);
   private static int verts = 0;
   private static final Map<String, Shader> shaders = new HashMap<>();
   private static final Set<String> failed = new HashSet<>();
   private static int vao = 0;
   private static int vbo = 0;
   private static final Matrix4f viewProj = new Matrix4f();
   private static final float[] mat = new float[16];
   private static final float[] axR = new float[3];
   private static final float[] axU = new float[3];
   private static final float[] axF = new float[3];
   private static int emptyVao = 0;
   private static int linearSampler = 0;

   private WorldFx() {
   }

   public static void begin() {
      buf.clear();
      verts = 0;
   }

   public static boolean isEmpty() {
      return verts == 0;
   }

   public static void vertex(float x, float y, float z, float u, float v, float r, float g, float b, float a, float ax, float ay) {
      if (buf.remaining() < 11) {
         buf = MemoryUtil.memRealloc(buf, buf.capacity() * 2);
      }

      buf.put(x).put(y).put(z).put(u).put(v).put(r).put(g).put(b).put(a).put(ax).put(ay);
      verts++;
   }

   public static void flushTextured(String shaderName, boolean throughWalls, int texture, Consumer<Shader> uniforms) {
      if (texture == 0) {
         begin();
      } else {
         int prevSampler = 0;

         try {
            GlStateManager._activeTexture(33984);
            prevSampler = GL11.glGetInteger(35097);
            int prevTex = GL11.glGetInteger(32873);
            if (linearSampler == 0) {
               linearSampler = GL33.glGenSamplers();
               GL33.glSamplerParameteri(linearSampler, 10241, 9729);
               GL33.glSamplerParameteri(linearSampler, 10240, 9729);
               GL33.glSamplerParameteri(linearSampler, 10242, 33071);
               GL33.glSamplerParameteri(linearSampler, 10243, 33071);
            }

            GlStateManager._bindTexture(texture);
            GL33.glBindSampler(0, linearSampler);
            flush(shaderName, throughWalls, uniforms);
            GlStateManager._activeTexture(33984);
            GlStateManager._bindTexture(prevTex);
         } finally {
            GL33.glBindSampler(0, prevSampler);
         }
      }
   }

   public static void flush(String shaderName, boolean throughWalls, Consumer<Shader> uniforms) {
      try {
         if (verts == 0) {
            return;
         }

         Shader shader = shader(shaderName);
         if (shader == null) {
            return;
         }

         if (!SkyShaderRenderer.levelViewProjection(viewProj)) {
            return;
         }

         RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
         if (main == null) {
            return;
         }

         try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
            if (RawFramebufferUtil.bindDirect(main)) {
               GlStateManager._disableScissorTest();
               GlStateManager._disableCull();
               GlStateManager._depthMask(false);
               if (throughWalls) {
                  GlStateManager._disableDepthTest();
               } else {
                  GlStateManager._enableDepthTest();
                  GlStateManager._depthFunc(515);
               }

               GlStateManager._enableBlend();
               GlStateManager._blendFuncSeparate(1, 771, 1, 771);
               GL14.glBlendEquation(32774);
               if (vao == 0) {
                  createVao();
               }

               buf.flip();
               GL30.glBindVertexArray(vao);
               GL15.glBindBuffer(34962, vbo);
               GL15.glBufferData(34962, buf, 35040);
               shader.bind();
               shader.setUniformMatrix4f("uViewProj", viewProj.get(mat));
               setAxes(shader);
               if (uniforms != null) {
                  uniforms.accept(shader);
               }

               GL11.glDrawArrays(4, 0, verts);
               shader.unbind();
               GL30.glBindVertexArray(0);
               return;
            }
         }
      } catch (Throwable var15) {
         return;
      } finally {
         buf.clear();
         verts = 0;
      }
   }

   public static void drawInstanced(String shaderName, int instances, boolean throughWalls, int texture0, Consumer<Shader> uniforms) {
      try {
         if (instances > 0) {
            Shader shader = shader(shaderName);
            if (shader != null) {
               if (SkyShaderRenderer.levelViewProjection(viewProj)) {
                  RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
                  if (main != null) {
                     try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
                        if (RawFramebufferUtil.bindDirect(main)) {
                           GlStateManager._disableScissorTest();
                           GlStateManager._disableCull();
                           GlStateManager._depthMask(false);
                           if (throughWalls) {
                              GlStateManager._disableDepthTest();
                           } else {
                              GlStateManager._enableDepthTest();
                              GlStateManager._depthFunc(515);
                           }

                           GlStateManager._enableBlend();
                           GlStateManager._blendFuncSeparate(1, 771, 1, 771);
                           GL14.glBlendEquation(32774);
                           if (emptyVao == 0) {
                              emptyVao = GL30.glGenVertexArrays();
                           }

                           GL30.glBindVertexArray(emptyVao);
                           int prevSampler = 0;
                           if (texture0 != 0) {
                              GlStateManager._activeTexture(33984);
                              prevSampler = GL11.glGetInteger(35097);
                              GlStateManager._bindTexture(texture0);
                              GL33.glBindSampler(0, 0);
                           }

                           shader.bind();
                           shader.setUniformMatrix4f("uViewProj", viewProj.get(mat));
                           setAxes(shader);
                           if (uniforms != null) {
                              uniforms.accept(shader);
                           }

                           GL31.glDrawArraysInstanced(5, 0, 4, instances);
                           shader.unbind();
                           GL30.glBindVertexArray(0);
                           if (texture0 != 0) {
                              GL33.glBindSampler(0, prevSampler);
                           }

                           return;
                        }
                     }
                  }
               }
            }
         }
      } catch (Throwable var12) {
      }
   }

   private static void setAxes(Shader s) {
      SkyShaderRenderer.cameraAxes(axR, axU, axF);
      s.setUniform3f("uRight", axR[0], axR[1], axR[2]);
      s.setUniform3f("uUp", axU[0], axU[1], axU[2]);
      s.setUniform3f("uFwd", axF[0], axF[1], axF[2]);
   }

   public static void axes(float[] right, float[] up, float[] fwd) {
      SkyShaderRenderer.cameraAxes(right, up, fwd);
   }

   private static void createVao() {
      vao = GL30.glGenVertexArrays();
      vbo = GL15.glGenBuffers();
      GL30.glBindVertexArray(vao);
      GL15.glBindBuffer(34962, vbo);
      int stride = 44;
      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 3, 5126, false, stride, 0L);
      GL20.glEnableVertexAttribArray(1);
      GL20.glVertexAttribPointer(1, 2, 5126, false, stride, 12L);
      GL20.glEnableVertexAttribArray(2);
      GL20.glVertexAttribPointer(2, 4, 5126, false, stride, 20L);
      GL20.glEnableVertexAttribArray(3);
      GL20.glVertexAttribPointer(3, 2, 5126, false, stride, 36L);
      GL30.glBindVertexArray(0);
   }

   private static Shader shader(String name) {
      Shader s = shaders.get(name);
      if (s == null && !failed.contains(name)) {
         try {
            int hash = name.indexOf(35);
            s = hash < 0 ? new Shader("worldfx", name) : new Shader("worldfx", name.substring(0, hash), "#define VARIANT " + name.substring(hash + 1));
            shaders.put(name, s);
         } catch (Exception e) {
            failed.add(name);
         }

         return s;
      } else {
         return s;
      }
   }
}
