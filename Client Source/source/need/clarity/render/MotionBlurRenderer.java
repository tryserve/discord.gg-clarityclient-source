package need.clarity.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import need.clarity.modules.visual.MotionBlur;
import need.clarity.render.sky.SkyShaderRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public final class MotionBlurRenderer {
   private static final float CUT_ANGLE = (float)Math.toRadians(45.0);
   private static final Matrix4f view = new Matrix4f();
   private static final Matrix3f cur = new Matrix3f();
   private static final Matrix3f prev = new Matrix3f();
   private static final Matrix3f delta = new Matrix3f();
   private static final Matrix4f proj = new Matrix4f();
   private static final float[] m3 = new float[9];
   private static boolean havePrev = false;
   private static long lastNanos = 0L;
   private static int program = 0;
   private static int locScene;
   private static int locDelta;
   private static int locFocal;
   private static int locScale;
   private static int locMaxLen;
   private static int locSize;
   private static int locSamples;
   private static boolean failed = false;
   private static int vao = 0;
   private static int vbo = 0;
   private static int sampler = 0;
   private static int copyTex = 0;
   private static int copyFbo = 0;
   private static int readFbo = 0;
   private static int copyW = 0;
   private static int copyH = 0;
   private static int errors = 0;

   private MotionBlurRenderer() {
   }

   public static void apply() {
      if (MotionBlur.active() && !failed) {
         try {
            run();
         } catch (Throwable t) {
            if (++errors >= 30) {
               failed = true;
            }
         }
      } else {
         havePrev = false;
         lastNanos = 0L;
      }
   }

   private static void run() {
      RenderSystem.assertOnRenderThread();
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         long now = System.nanoTime();
         float dt = lastNanos == 0L ? 0.016666668F : (float)(now - lastNanos) / 1.0E9F;
         lastNanos = now;
         dt = Math.max(0.001F, Math.min(0.05F, dt));
         Camera camera = mc.gameRenderer.getMainCamera();
         camera.getViewRotationMatrix(view);
         cur.set(view);
         if (!havePrev) {
            prev.set(cur);
            havePrev = true;
         } else {
            cur.transpose(delta);
            prev.mul(delta, delta);
            prev.set(cur);
            float tr = delta.m00() + delta.m11() + delta.m22();
            float angle = (float)Math.acos(Math.max(-1.0F, Math.min(1.0F, (tr - 1.0F) * 0.5F)));
            if (!(angle > CUT_ANGLE) && Float.isFinite(angle)) {
               RenderTarget main = mc.getMainRenderTarget();
               if (main != null && main.width > 0 && main.height > 0) {
                  if (SkyShaderRenderer.levelProjection(proj)) {
                     float fx = Math.abs(proj.m00());
                     float fy = Math.abs(proj.m11());
                     if (!(fx < 1.0E-4F) && !(fy < 1.0E-4F)) {
                        float scale = MotionBlur.exposure() / dt;
                        float maxPx = angle * Math.max(fx, fy) * 0.5F * scale * Math.max(main.width, main.height);
                        if (!(maxPx < 0.75F)) {
                           int sceneId = RawFramebufferUtil.getColorTextureId(main);
                           if (sceneId != 0) {
                              try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
                                 if (!ensureResources(main.width, main.height)) {
                                    return;
                                 }

                                 GL30.glBindFramebuffer(36160, readFbo);
                                 GL30.glFramebufferTexture2D(36160, 36064, 3553, sceneId, 0);
                                 if (GL30.glCheckFramebufferStatus(36160) != 36053) {
                                    return;
                                 }

                                 GlStateManager._disableScissorTest();
                                 GL30.glBindFramebuffer(36008, readFbo);
                                 GL11.glReadBuffer(36064);
                                 GL30.glBindFramebuffer(36009, copyFbo);
                                 GL11.glDrawBuffer(36064);
                                 GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height, 16384, 9728);
                                 if (!RawFramebufferUtil.bindDirect(main)) {
                                    return;
                                 }

                                 GlStateManager._disableDepthTest();
                                 GlStateManager._depthMask(false);
                                 GlStateManager._disableBlend();
                                 GlStateManager._disableCull();
                                 GL11.glColorMask(true, true, true, true);
                                 GlStateManager._activeTexture(33984);
                                 int prevSampler = GL11.glGetInteger(35097);
                                 GlStateManager._bindTexture(copyTex);
                                 GL33.glBindSampler(0, sampler);
                                 GL20.glUseProgram(program);
                                 delta.get(m3);
                                 GL20.glUniform1i(locScene, 0);
                                 GL20.glUniformMatrix3fv(locDelta, false, m3);
                                 GL20.glUniform2f(locFocal, fx, fy);
                                 GL20.glUniform1f(locScale, scale);
                                 GL20.glUniform1f(locMaxLen, MotionBlur.maxLength());
                                 GL20.glUniform2f(locSize, main.width, main.height);
                                 GL20.glUniform1i(locSamples, MotionBlur.samples());
                                 GL30.glBindVertexArray(vao);
                                 GL11.glDrawArrays(4, 0, 3);
                                 GL30.glBindVertexArray(0);
                                 GL33.glBindSampler(0, prevSampler);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         havePrev = false;
      }
   }

   private static boolean ensureResources(int w, int h) {
      if (program == 0 && !buildProgram()) {
         return false;
      }

      if (vao == 0) {
         vao = GL30.glGenVertexArrays();
         vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(vao);
         GL15.glBindBuffer(34962, vbo);
         GL15.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
         GL30.glBindVertexArray(0);
      }

      if (sampler == 0) {
         sampler = GL33.glGenSamplers();
         GL33.glSamplerParameteri(sampler, 10241, 9729);
         GL33.glSamplerParameteri(sampler, 10240, 9729);
         GL33.glSamplerParameteri(sampler, 10242, 33071);
         GL33.glSamplerParameteri(sampler, 10243, 33071);
      }

      if (readFbo == 0) {
         readFbo = GL30.glGenFramebuffers();
      }

      if (copyFbo != 0 && copyW == w && copyH == h) {
         return true;
      }

      if (copyTex == 0) {
         copyTex = GL11.glGenTextures();
      }

      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(copyTex);
      GL11.glTexParameteri(3553, 33085, 0);
      int prevUnpack = GL11.glGetInteger(35055);
      GL15.glBindBuffer(35052, 0);
      GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
      GL15.glBindBuffer(35052, prevUnpack);
      if (copyFbo == 0) {
         copyFbo = GL30.glGenFramebuffers();
      }

      GL30.glBindFramebuffer(36160, copyFbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, copyTex, 0);
      boolean ok = GL30.glCheckFramebufferStatus(36160) == 36053;
      copyW = ok ? w : 0;
      copyH = ok ? h : 0;
      return ok;
   }

   private static boolean buildProgram() {
      try {
         int vs = compile(35633, read("blur.vsh"));
         int fs = compile(35632, read("blur.fsh"));
         int p = GL20.glCreateProgram();
         GL20.glAttachShader(p, vs);
         GL20.glAttachShader(p, fs);
         GL20.glBindAttribLocation(p, 0, "Position");
         GL20.glLinkProgram(p);
         GL20.glDetachShader(p, vs);
         GL20.glDetachShader(p, fs);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         if (GL20.glGetProgrami(p, 35714) == 0) {
            GL20.glDeleteProgram(p);
            failed = true;
            return false;
         } else {
            program = p;
            locScene = GL20.glGetUniformLocation(p, "uScene");
            locDelta = GL20.glGetUniformLocation(p, "uDelta");
            locFocal = GL20.glGetUniformLocation(p, "uFocal");
            locScale = GL20.glGetUniformLocation(p, "uScale");
            locMaxLen = GL20.glGetUniformLocation(p, "uMaxLen");
            locSize = GL20.glGetUniformLocation(p, "uSize");
            locSamples = GL20.glGetUniformLocation(p, "uSamples");
            return true;
         }
      } catch (Throwable t) {
         failed = true;
         return false;
      }
   }

   private static int compile(int type, String src) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, src);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         String log = GL20.glGetShaderInfoLog(s);
         GL20.glDeleteShader(s);
         throw new RuntimeException(log);
      } else {
         return s;
      }
   }

   private static String read(String name) throws IOException {
      try (InputStream in = MotionBlurRenderer.class.getResourceAsStream("/assets/clarity/shaders/motionblur/" + name)) {
         if (in == null) {
            throw new IOException("missing " + name);
         } else {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
         }
      }
   }
}
