package need.clarity.render.sky;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import need.clarity.audio.AudioDemand;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

public final class SkyShaderRenderer {
   private static final Matrix4f levelProjection = new Matrix4f();
   private static boolean haveProjection = false;
   private static final Matrix4f viewRotation = new Matrix4f();
   private static CameraRenderState cameraState;
   private static boolean activeThisFrame = false;
   private static boolean activeLastFrame = false;
   private static final Map<String, SkyProgram> programs = new HashMap<>();
   private static final Set<String> failed = new HashSet<>();
   private static SkyProgram upscaleProgram;
   private static boolean upscaleFailed = false;
   private static int noiseTex = 0;
   private static int samplerRepeat = 0;
   private static int samplerClamp = 0;
   private static int vao = 0;
   private static int vbo = 0;
   private static int lowFbo = 0;
   private static int lowTex = 0;
   private static int lowW = 0;
   private static int lowH = 0;
   private static long lastNanos = 0L;
   private static double animTime = 0.0;
   private static double celestialAngle = 0.0;
   private static final Matrix4f tmpVP = new Matrix4f();
   private static final Matrix4f tmpInv = new Matrix4f();
   private static final Matrix3f tmpCel = new Matrix3f();
   private static final float[] mat4 = new float[16];
   private static final float[] mat3 = new float[9];
   private static final Vector3f CELESTIAL_AXIS = new Vector3f(0.0F, 0.766F, 0.643F);
   private static final AudioDemand.Frame audio = new AudioDemand.Frame();
   private static float dirSign = 1.0F;
   private static final Matrix4f projForPixel = new Matrix4f();
   private static final Vector4f tmpA = new Vector4f();
   private static final Vector4f tmpB = new Vector4f();

   private SkyShaderRenderer() {
   }

   public static void beginFrame(Matrix4fc view, CameraRenderState camera) {
      activeLastFrame = activeThisFrame;
      activeThisFrame = false;
      if (view != null) {
         viewRotation.set(view);
      }

      cameraState = camera;
   }

   public static void captureProjection(Matrix4fc projection) {
      levelProjection.set(projection);
      haveProjection = true;
   }

   public static boolean levelViewProjection(Matrix4f dest) {
      Matrix4f proj = haveProjection ? levelProjection : (cameraState != null ? cameraState.projectionMatrix : null);
      if (proj == null) {
         return false;
      }

      proj.mul(viewRotation, dest);
      return dest.isFinite();
   }

   public static boolean cameraViewProjection(Matrix4f dest) {
      CameraRenderState cam = cameraState;
      if (cam != null && cam.projectionMatrix != null) {
         cam.projectionMatrix.mul(viewRotation, dest);
         return dest.isFinite();
      } else {
         return false;
      }
   }

   public static boolean levelProjection(Matrix4f dest) {
      Matrix4f proj = haveProjection ? levelProjection : (cameraState != null ? cameraState.projectionMatrix : null);
      if (proj == null) {
         return false;
      }

      dest.set(proj);
      return dest.isFinite();
   }

   public static void cameraAxes(float[] right, float[] up, float[] fwd) {
      right[0] = viewRotation.m00();
      right[1] = viewRotation.m10();
      right[2] = viewRotation.m20();
      up[0] = viewRotation.m01();
      up[1] = viewRotation.m11();
      up[2] = viewRotation.m21();
      fwd[0] = -viewRotation.m02();
      fwd[1] = -viewRotation.m12();
      fwd[2] = -viewRotation.m22();
   }

   public static boolean isActiveThisFrame() {
      return activeThisFrame;
   }

   public static boolean wasActiveLastFrame() {
      return activeLastFrame && SkyShaders.active();
   }

   public static boolean tryRender(boolean endSky) {
      if (!SkyShaders.active()) {
         return false;
      }

      if (endSky && !SkyShaders.inTheEnd()) {
         return false;
      }

      CameraRenderState cam = cameraState;
      if (cam != null && cam.fogType != null && cam.fogType != FogType.NONE && cam.fogType != FogType.ATMOSPHERIC) {
         return false;
      }

      boolean ok;
      try {
         ok = draw();
      } catch (Throwable t) {
         ok = false;
      }

      activeThisFrame = ok;
      return ok;
   }

   private static boolean draw() {
      RenderSystem.assertOnRenderThread();
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.getMainRenderTarget();
      if (main != null && main.width > 0 && main.height > 0) {
         String preset = SkyShaders.preset();
         String file = SkyShaders.presetFile(preset);
         if (failed.contains(file)) {
            return false;
         }

         if (!computeMatrices()) {
            return false;
         }

         tickClocks();

         try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
            SkyProgram prog = program(file);
            if (prog != null && ensureCommon()) {
               float res = SkyShaders.resolutionScale();
               boolean scaled = res < 0.999F && !upscaleFailed;
               int w = main.width;
               int h = main.height;
               int rw = scaled ? Math.max(1, Math.round(w * res)) : w;
               int rh = scaled ? Math.max(1, Math.round(h * res)) : h;
               GlStateManager._disableDepthTest();
               GlStateManager._depthMask(false);
               GlStateManager._disableBlend();
               GlStateManager._disableCull();
               GlStateManager._disableScissorTest();
               GL11.glColorMask(true, true, true, true);
               if (scaled && !ensureLowTarget(rw, rh)) {
                  scaled = false;
                  rw = w;
                  rh = h;
               }

               if (scaled) {
                  GL30.glBindFramebuffer(36160, lowFbo);
                  GL11.glDrawBuffer(36064);
                  GlStateManager._viewport(0, 0, rw, rh);
               } else if (!RawFramebufferUtil.bindDirect(main)) {
                  return false;
               }

               int prevSampler = boundSampler0();
               GlStateManager._activeTexture(33984);
               GlStateManager._bindTexture(noiseTex);
               GL33.glBindSampler(0, samplerRepeat);
               GL20.glUseProgram(prog.id);
               setUniforms(prog, preset, rh);
               drawTriangle();
               if (scaled) {
                  SkyProgram up = upscale();
                  if (up != null && RawFramebufferUtil.bindDirect(main)) {
                     GlStateManager._bindTexture(lowTex);
                     GL33.glBindSampler(0, samplerClamp);
                     GL20.glUseProgram(up.id);
                     up.set1i("uSource", 0);
                     drawTriangle();
                  }
               }

               GL33.glBindSampler(0, prevSampler);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static void setUniforms(SkyProgram p, String preset, int renderHeight) {
      float[][] cols = SkyShaders.colors();
      float[] c1 = cols[0];
      float[] c2 = cols[1];
      float[] hz = SkyShaders.horizon(preset, c1, c2);
      tmpInv.get(mat4);
      p.setMat4("uInvViewProj", mat4);
      p.set1i("uNoise", 0);
      p.set1f("uTime", (float)(animTime % 10000.0));
      p.set3f("uColor1", lin(c1[0]), lin(c1[1]), lin(c1[2]));
      p.set3f("uColor2", lin(c2[0]), lin(c2[1]), lin(c2[2]));
      p.set1f("uIntensity", SkyShaders.intensity());
      p.set1f("uScale", SkyShaders.scale());
      p.set1f("uStars", SkyShaders.stars());
      p.set1f("uBrightness", SkyShaders.brightness());
      p.set3f("uHorizon", hz[0], hz[1], hz[2]);
      p.set1f("uHorizonFog", SkyShaders.horizonBlend());
      p.set1f("uDirSign", dirSign);
      tmpCel.rotation((float)celestialAngle, CELESTIAL_AXIS).get(mat3);
      p.setMat3("uCelestial", mat3);
      float m11 = Math.abs(projForPixel.m11());
      p.set1f("uPixel", m11 > 1.0E-4F ? 2.0F / (m11 * renderHeight) : 0.002F);
      if (SkyShaders.isAudioPreset(preset)) {
         setAudioUniforms(p);
      }
   }

   private static void setAudioUniforms(SkyProgram p) {
      audio.reactivity = SkyShaders.reactivity();
      AudioDemand.sample(audio, animTime, SkyShaders.idleBeat());
      p.set1fv("uBands", audio.bands);
      p.set1f("uBass", audio.bass);
      p.set1fv("uBeatAge", audio.beatAges);
      p.set1fv("uBeatId", audio.beatIds);
      p.set1f("uLevel", audio.level);
      p.set1f("uTreble", audio.treble);
      p.set1f("uFlash", audio.flash);
      p.set1f("uPhase", (float)(audio.phase % 10000.0));
      p.set1f("uReact", audio.reactivity);
   }

   private static boolean computeMatrices() {
      Matrix4f proj;
      if (haveProjection) {
         proj = levelProjection;
      } else {
         if (cameraState == null || cameraState.projectionMatrix == null) {
            return false;
         }

         proj = cameraState.projectionMatrix;
      }

      projForPixel.set(proj);
      proj.mul(viewRotation, tmpVP);
      if (tmpVP.isFinite() && !(Math.abs(tmpVP.determinant()) < 1.0E-12F)) {
         tmpVP.invert(tmpInv);
         if (!tmpInv.isFinite()) {
            return false;
         }

         tmpInv.transform(0.0F, 0.0F, -1.0F, 1.0F, tmpA);
         tmpInv.transform(0.0F, 0.0F, 1.0F, 1.0F, tmpB);
         float rx = tmpB.x * tmpA.w - tmpA.x * tmpB.w;
         float ry = tmpB.y * tmpA.w - tmpA.y * tmpB.w;
         float rz = tmpB.z * tmpA.w - tmpA.z * tmpB.w;
         float fx = -viewRotation.m02();
         float fy = -viewRotation.m12();
         float fz = -viewRotation.m22();
         dirSign = rx * fx + ry * fy + rz * fz < 0.0F ? -1.0F : 1.0F;
         return true;
      } else {
         return false;
      }
   }

   private static void tickClocks() {
      long now = System.nanoTime();
      double dt = lastNanos == 0L ? 0.0 : (now - lastNanos) / 1.0E9;
      lastNanos = now;
      dt = Math.max(0.0, Math.min(dt, 0.1));
      animTime = animTime + dt * SkyShaders.speed();
      celestialAngle = (celestialAngle + dt * SkyShaders.rotation() * 0.010471975511965976) % (Math.PI * 2);
   }

   private static float lin(float c) {
      return (float)Math.pow(Math.max(c, 0.0F), 2.2);
   }

   private static SkyProgram program(String file) {
      SkyProgram p = programs.get(file);
      if (p != null) {
         return p;
      }

      try {
         p = SkyProgram.preset(file);
         programs.put(file, p);
         return p;
      } catch (Throwable t) {
         failed.add(file);
         return null;
      }
   }

   private static SkyProgram upscale() {
      if (upscaleProgram == null && !upscaleFailed) {
         try {
            upscaleProgram = SkyProgram.upscale();
         } catch (Throwable t) {
            upscaleFailed = true;
         }

         return upscaleProgram;
      } else {
         return upscaleProgram;
      }
   }

   private static boolean ensureCommon() {
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

      if (samplerRepeat == 0) {
         samplerRepeat = makeSampler(10497);
         samplerClamp = makeSampler(33071);
      }

      if (noiseTex == 0) {
         noiseTex = buildNoise();
      }

      return noiseTex != 0;
   }

   private static int makeSampler(int wrap) {
      int s = GL33.glGenSamplers();
      GL33.glSamplerParameteri(s, 10241, 9729);
      GL33.glSamplerParameteri(s, 10240, 9729);
      GL33.glSamplerParameteri(s, 10242, wrap);
      GL33.glSamplerParameteri(s, 10243, wrap);
      return s;
   }

   private static int buildNoise() {
      int n = 256;
      byte[] r = new byte[65536];
      byte[] b = new byte[65536];
      byte[] a = new byte[65536];
      int s = -1640531527;

      for (int i = 0; i < 65536; i++) {
         int var16 = s ^ s << 13;
         int var17 = var16 ^ var16 >>> 17;
         int var18 = var17 ^ var17 << 5;
         r[i] = (byte)(var18 >>> 24);
         int var19 = var18 ^ var18 << 13;
         int var20 = var19 ^ var19 >>> 17;
         int var21 = var20 ^ var20 << 5;
         b[i] = (byte)(var21 >>> 24);
         int var22 = var21 ^ var21 << 13;
         int var23 = var22 ^ var22 >>> 17;
         s = var23 ^ var23 << 5;
         a[i] = (byte)(s >>> 24);
      }

      ByteBuffer buf = MemoryUtil.memAlloc(262144);

      try {
         for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
               int i = y * 256 + x;
               int j = (y - 17 & 0xFF) * 256 + (x - 37 & 0xFF);
               buf.put(r[i]).put(r[j]).put(b[i]).put(a[i]);
            }
         }

         buf.flip();
         int prevUnpackBuffer = GL11.glGetInteger(35055);
         GL15.glBindBuffer(35052, 0);
         int rowLen = GL11.glGetInteger(3314);
         int skipP = GL11.glGetInteger(3316);
         int skipR = GL11.glGetInteger(3315);
         int align = GL11.glGetInteger(3317);
         GL11.glPixelStorei(3314, 0);
         GL11.glPixelStorei(3316, 0);
         GL11.glPixelStorei(3315, 0);
         GL11.glPixelStorei(3317, 4);
         int tex = GL11.glGenTextures();
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(tex);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 10497);
         GL11.glTexParameteri(3553, 10243, 10497);
         GL11.glTexParameteri(3553, 33085, 0);
         GL11.glTexImage2D(3553, 0, 32856, 256, 256, 0, 6408, 5121, buf);
         GL11.glPixelStorei(3314, rowLen);
         GL11.glPixelStorei(3316, skipP);
         GL11.glPixelStorei(3315, skipR);
         GL11.glPixelStorei(3317, align);
         GL15.glBindBuffer(35052, prevUnpackBuffer);
         return tex;
      } finally {
         MemoryUtil.memFree(buf);
      }
   }

   private static boolean ensureLowTarget(int w, int h) {
      if (lowFbo != 0 && lowW == w && lowH == h) {
         return true;
      }

      if (lowTex == 0) {
         lowTex = GL11.glGenTextures();
      }

      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(lowTex);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 33085, 0);
      int prevUnpackBuffer = GL11.glGetInteger(35055);
      GL15.glBindBuffer(35052, 0);
      GL11.glTexImage2D(3553, 0, 34842, w, h, 0, 6408, 5126, (FloatBuffer)null);
      GL15.glBindBuffer(35052, prevUnpackBuffer);
      if (lowFbo == 0) {
         lowFbo = GL30.glGenFramebuffers();
      }

      GL30.glBindFramebuffer(36160, lowFbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, lowTex, 0);
      boolean ok = GL30.glCheckFramebufferStatus(36160) == 36053;
      lowW = ok ? w : 0;
      lowH = ok ? h : 0;
      if (!ok) {
      }

      return ok;
   }

   private static int boundSampler0() {
      GlStateManager._activeTexture(33984);
      return GL11.glGetInteger(35097);
   }

   private static void drawTriangle() {
      GL30.glBindVertexArray(vao);
      GL11.glDrawArrays(4, 0, 3);
      GL30.glBindVertexArray(0);
   }

   public static void dispose() {
      try {
         for (SkyProgram p : programs.values()) {
            p.delete();
         }

         programs.clear();
         if (upscaleProgram != null) {
            upscaleProgram.delete();
            upscaleProgram = null;
         }

         if (noiseTex != 0) {
            GL11.glDeleteTextures(noiseTex);
            noiseTex = 0;
         }

         if (lowTex != 0) {
            GL11.glDeleteTextures(lowTex);
            lowTex = 0;
         }

         if (lowFbo != 0) {
            GL30.glDeleteFramebuffers(lowFbo);
            lowFbo = 0;
         }

         if (samplerRepeat != 0) {
            GL33.glDeleteSamplers(samplerRepeat);
            samplerRepeat = 0;
         }

         if (samplerClamp != 0) {
            GL33.glDeleteSamplers(samplerClamp);
            samplerClamp = 0;
         }

         if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
         }

         if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
         }
      } catch (Throwable var2) {
      }
   }
}
