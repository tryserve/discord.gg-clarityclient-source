package need.clarity.render.handshader;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.FloatBuffer;
import need.clarity.audio.AudioDemand;
import need.clarity.modules.visual.HandShaders;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import need.clarity.render.Shader;
import need.clarity.render.ShaderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public final class HandFx {
   private static Shader maskShader;
   private static Shader blurShader;
   private static Shader glowShader;
   private static Shader glassShader;
   private static Shader holoShader;
   private static Shader plainBlurShader;
   private static Shader wispAdvect;
   private static Shader wispShader;
   private static Shader liquidShader;
   private static Shader beatShader;
   private static Shader trailAdvect;
   private static Shader trailShader;
   private static final HandFx.Target trA = new HandFx.Target();
   private static final HandFx.Target trB = new HandFx.Target();
   private static final HandFx.Target trMask = new HandFx.Target();
   private static final HandFx.Target wispA = new HandFx.Target();
   private static final HandFx.Target wispB = new HandFx.Target();
   private static boolean initDone = false;
   private static boolean initFailed = false;
   private static final HandFx.Target half1 = new HandFx.Target();
   private static final HandFx.Target half2 = new HandFx.Target();
   private static final HandFx.Target quarter1 = new HandFx.Target();
   private static final HandFx.Target quarter2 = new HandFx.Target();
   private static int samplerLinear = 0;
   private static int samplerNearest = 0;
   private static boolean bgValid = false;
   private static int bgDiv = 2;
   private static final AudioDemand.Frame audio = new AudioDemand.Frame();
   private static long wispLastNanos = 0L;
   private static float windX;
   private static float windY;
   private static float windVX;
   private static float windVY;
   private static float lastYaw;
   private static float lastPitch;
   private static boolean haveLook = false;
   private static long trailLastNanos = 0L;
   private static long lastEcho = 0L;
   private static final long START = System.nanoTime();

   private HandFx() {
   }

   public static boolean handles(String shader) {
      return "Glass".equals(shader)
         || "Glow".equals(shader)
         || "Hologram".equals(shader)
         || "Blur".equals(shader)
         || "Wisp".equals(shader)
         || "Liquid Glass".equals(shader)
         || "Beat Pulse".equals(shader)
         || "Trails".equals(shader);
   }

   private static boolean ensureInit() {
      if (initDone) {
         return true;
      }

      if (initFailed) {
         return false;
      }

      try {
         maskShader = new Shader("hand", "fx_mask");
         blurShader = new Shader("hand", "fx_blur");
         glowShader = new Shader("hand", "glowfx");
         glassShader = new Shader("hand", "glass");
         holoShader = new Shader("hand", "holo");
         plainBlurShader = new Shader("hand", "plainblur");
         wispAdvect = new Shader("hand", "wisp_advect");
         wispShader = new Shader("hand", "wisp");
         liquidShader = new Shader("hand", "liquidglass");
         beatShader = new Shader("hand", "beatpulse");
         trailAdvect = new Shader("hand", "trails_advect");
         trailShader = new Shader("hand", "trails");
         samplerLinear = makeSampler(9729);
         samplerNearest = makeSampler(9728);
         initDone = true;
      } catch (Exception e) {
         initFailed = true;
      }

      return initDone;
   }

   private static int makeSampler(int filter) {
      int s = GL33.glGenSamplers();
      GL33.glSamplerParameteri(s, 10241, filter);
      GL33.glSamplerParameteri(s, 10240, filter);
      GL33.glSamplerParameteri(s, 10242, 33071);
      GL33.glSamplerParameteri(s, 10243, 33071);
      return s;
   }

   private static int glassDiv(float px) {
      return HandShaders.getGlassBlur() * px <= 8.0F ? 2 : 4;
   }

   public static void captureBackground() {
      bgValid = false;
      String s = HandShaders.getShader();
      boolean blurBehind = "Blur".equals(s) && !HandShaders.getBlurHandItself();
      boolean liquid = "Liquid Glass".equals(s);
      if ("Glass".equals(s) || "Hologram".equals(s) || blurBehind || liquid) {
         RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
         if (main != null && main.width > 0 && main.height > 0 && ensureInit()) {
            try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
               int div = liquid ? 2 : (!"Glass".equals(s) && !blurBehind ? 2 : glassDiv(main.height / 1080.0F));
               HandFx.Target t = div == 2 ? half1 : quarter1;
               if (ensure(t, main.width / div, main.height / div)) {
                  if (RawFramebufferUtil.bindDirect(main)) {
                     GlStateManager._disableScissorTest();
                     GL30.glBindFramebuffer(36009, t.fbo);
                     GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, t.w, t.h, 16384, 9729);
                     bgValid = true;
                     bgDiv = div;
                  }
               }
            } catch (Throwable var9) {
            }
         }
      }
   }

   public static void render(String name, RenderTarget main, RenderTarget copy) {
      if (ensureInit()) {
         int colorId = RawFramebufferUtil.getColorTextureId(copy);
         int depthId = RawFramebufferUtil.getDepthTextureId(copy);
         if (colorId != 0 && depthId != 0) {
            int[] prevSampler = new int[4];

            for (int i = 0; i < 4; i++) {
               GlStateManager._activeTexture(33984 + i);
               prevSampler[i] = GL11.glGetInteger(35097);
            }

            GlStateManager._activeTexture(33987);
            int prevTex3 = GL11.glGetInteger(32873);

            try {
               GlStateManager._disableBlend();
               GlStateManager._disableDepthTest();
               GlStateManager._disableScissorTest();
               GlStateManager._disableCull();
               bind(0, colorId, samplerLinear);
               bind(1, depthId, samplerNearest);
               int w = main.width;
               int h = main.height;
               float px = h / 1080.0F;
               switch (name) {
                  case "Glow":
                     glow(main, w, h, px);
                     break;
                  case "Glass":
                     glass(main, w, h, px);
                     break;
                  case "Hologram":
                     hologram(main, w, h, px);
                     break;
                  case "Blur":
                     plainBlur(main, w, h, px);
                     break;
                  case "Wisp":
                     wisp(main, w, h);
                     break;
                  case "Liquid Glass":
                     liquidGlass(main, w, h, px);
                     break;
                  case "Beat Pulse":
                     beatPulse(main, w, h, px);
                     break;
                  case "Trails":
                     trails(main, w, h);
               }
            } finally {
               for (int i = 0; i < 4; i++) {
                  GL33.glBindSampler(i, prevSampler[i]);
               }

               GlStateManager._activeTexture(33987);
               GlStateManager._bindTexture(prevTex3);
               GlStateManager._activeTexture(33984);
               bgValid = false;
            }
         }
      }
   }

   private static void glow(RenderTarget main, int w, int h, float px) {
      float sigma = Math.max(0.5F, HandShaders.getGlowRadius() * px / 1.6F);
      int div = sigma <= 6.0F ? 2 : 4;
      HandFx.Target a = div == 2 ? half1 : quarter1;
      HandFx.Target b = div == 2 ? half2 : quarter2;
      if (ensure(a, w / div, h / div) && ensure(b, w / div, h / div)) {
         mask(a, w, h, div);
         blur(a, b, sigma / div);
         if (RawFramebufferUtil.bindDirect(main)) {
            bind(2, a.tex, samplerLinear);
            int c = HandShaders.getGlowFxColor();
            glowShader.bind();
            glowShader.setUniform1i("ColorTexture", 0);
            glowShader.setUniform1i("DepthTexture", 1);
            glowShader.setUniform1i("GlowTexture", 2);
            glowShader.setUniform3f("glowColor", r(c), g(c), b(c));
            glowShader.setUniform1f("intensity", HandShaders.getGlowIntensity());
            Shader var10000 = glowShader;

            var10000.setUniform1i("mode", switch (HandShaders.getGlowMode()) {
               case "Item Colors" -> 1;
               case "Chroma" -> 2;
               default -> 0;
            });
            glowShader.setUniform1f("time", time());
            glowShader.setUniform1f("pulse", HandShaders.getGlowPulse() ? 1.0F : 0.0F);
            ShaderHelper.drawFullScreenQuad();
            glowShader.unbind();
         }
      }
   }

   private static void plainBlur(RenderTarget main, int w, int h, float px) {
      boolean self = HandShaders.getBlurHandItself();
      if (!self) {
         if (!bgValid) {
            return;
         }

         int div = bgDiv;
         HandFx.Target a = div == 2 ? half1 : quarter1;
         HandFx.Target b = div == 2 ? half2 : quarter2;
         if (a.fbo == 0 || !ensure(b, a.w, a.h)) {
            return;
         }

         float sigma = HandShaders.getGlassBlur() * px / div;
         if (sigma > 0.3F) {
            blur(a, b, sigma);
         }

         if (!RawFramebufferUtil.bindDirect(main)) {
            return;
         }

         bind(2, a.tex, samplerLinear);
      } else if (!RawFramebufferUtil.bindDirect(main)) {
         return;
      }

      plainBlurShader.bind();
      plainBlurShader.setUniform1i("ColorTexture", 0);
      plainBlurShader.setUniform1i("DepthTexture", 1);
      plainBlurShader.setUniform1i("Background", 2);
      plainBlurShader.setUniform1i("selfBlur", self ? 1 : 0);
      plainBlurShader.setUniform2f("texel", 1.0F / w, 1.0F / h);
      plainBlurShader.setUniform1f("radius", HandShaders.getGlassBlur() * px);
      ShaderHelper.drawFullScreenQuad();
      plainBlurShader.unbind();
   }

   private static void glass(RenderTarget main, int w, int h, float px) {
      if (bgValid) {
         int div = bgDiv;
         HandFx.Target a = div == 2 ? half1 : quarter1;
         HandFx.Target b = div == 2 ? half2 : quarter2;
         if (a.fbo != 0 && ensure(b, a.w, a.h)) {
            float sigma = HandShaders.getGlassBlur() * px / div;
            if (sigma > 0.3F) {
               blur(a, b, sigma);
            }

            if (RawFramebufferUtil.bindDirect(main)) {
               bind(2, a.tex, samplerLinear);
               int t = HandShaders.getGlassTint();
               glassShader.bind();
               glassShader.setUniform1i("ColorTexture", 0);
               glassShader.setUniform1i("DepthTexture", 1);
               glassShader.setUniform1i("Background", 2);
               glassShader.setUniform2f("texel", 1.0F / w, 1.0F / h);
               glassShader.setUniform1f("pxScale", px);
               glassShader.setUniform3f("tint", r(t), g(t), b(t));
               glassShader.setUniform1f("tintAmount", HandShaders.getGlassTintAmount());
               glassShader.setUniform1f("refraction", HandShaders.getGlassRefraction());
               glassShader.setUniform1f("detail", HandShaders.getGlassDetail());
               glassShader.setUniform1f("rim", HandShaders.getGlassRim());
               ShaderHelper.drawFullScreenQuad();
               glassShader.unbind();
            }
         }
      }
   }

   private static void liquidGlass(RenderTarget main, int w, int h, float px) {
      if (bgValid && bgDiv == 2 && half1.fbo != 0) {
         if (ensure(half2, half1.w, half1.h)) {
            float frost = HandShaders.getLiquidFrost() * px / 2.0F;
            if (frost > 0.3F) {
               blur(half1, half2, frost);
            }

            int qw = Math.max(1, w / 4);
            int qh = Math.max(1, h / 4);
            if (ensure(quarter1, qw, qh) && ensure(quarter2, qw, qh)) {
               mask(quarter1, w, h, 4);
               blur(quarter1, quarter2, Math.max(0.6F, HandShaders.getLiquidThickness() * px / 4.0F));
               if (RawFramebufferUtil.bindDirect(main)) {
                  bind(2, half1.tex, samplerLinear);
                  bind(3, quarter1.tex, samplerLinear);
                  int t = HandShaders.getLiquidTint();
                  liquidShader.bind();
                  liquidShader.setUniform1i("ColorTexture", 0);
                  liquidShader.setUniform1i("DepthTexture", 1);
                  liquidShader.setUniform1i("Background", 2);
                  liquidShader.setUniform1i("Height", 3);
                  liquidShader.setUniform2f("texel", 1.0F / w, 1.0F / h);
                  liquidShader.setUniform2f("heightTexel", 1.0F / qw, 1.0F / qh);
                  liquidShader.setUniform1f("pxScale", px);
                  liquidShader.setUniform1f("refraction", HandShaders.getLiquidRefraction());
                  liquidShader.setUniform1f("dispersion", HandShaders.getLiquidDispersion());
                  liquidShader.setUniform3f("tint", r(t), g(t), b(t));
                  liquidShader.setUniform1f("tintAmount", HandShaders.getLiquidTintAmount());
                  liquidShader.setUniform1f("specular", HandShaders.getLiquidSpecular());
                  liquidShader.setUniform1f("detail", HandShaders.getLiquidDetail());
                  ShaderHelper.drawFullScreenQuad();
                  liquidShader.unbind();
               }
            }
         }
      }
   }

   private static void beatPulse(RenderTarget main, int w, int h, float px) {
      audio.reactivity = HandShaders.getPulseReactivity();
      AudioDemand.sample(audio, time(), HandShaders.getPulseIdleBeats() ? 0.6F : 0.0F);
      float radius = HandShaders.getPulseRadius();
      float sigma = Math.max(0.5F, radius * px / 1.6F);
      int div = sigma <= 6.0F ? 2 : 4;
      HandFx.Target a = div == 2 ? half1 : quarter1;
      HandFx.Target bb = div == 2 ? half2 : quarter2;
      if (ensure(a, w / div, h / div) && ensure(bb, w / div, h / div)) {
         mask(a, w, h, div);
         blur(a, bb, sigma / div);
         if (RawFramebufferUtil.bindDirect(main)) {
            bind(2, a.tex, samplerLinear);
            int c1 = HandShaders.getPulseColor();
            int c2 = HandShaders.getPulseColor2();
            beatShader.bind();
            beatShader.setUniform1i("ColorTexture", 0);
            beatShader.setUniform1i("DepthTexture", 1);
            beatShader.setUniform1i("GlowTexture", 2);
            beatShader.setUniform3f("color1", r(c1), g(c1), b(c1));
            beatShader.setUniform3f("color2", r(c2), g(c2), b(c2));
            beatShader.setUniform1i("mode", "Chroma".equals(HandShaders.getPulseMode()) ? 1 : 0);
            beatShader.setUniform1f("intensity", HandShaders.getPulseIntensity());
            beatShader.setUniform1f("time", time());
            beatShader.setUniform1f("aspect", (float)w / h);
            beatShader.setUniform1fv("uBands", audio.bands);
            beatShader.setUniform1f("uBass", audio.bass);
            beatShader.setUniform1fv("uBeatAge", audio.beatAges);
            beatShader.setUniform1f("uFlash", audio.flash);
            beatShader.setUniform1f("uLevel", audio.level);
            ShaderHelper.drawFullScreenQuad();
            beatShader.unbind();
         }
      }
   }

   private static void hologram(RenderTarget main, int w, int h, float px) {
      float haloAmt = HandShaders.getHoloHalo();
      if (haloAmt > 0.01F) {
         if (!ensure(quarter1, w / 4, h / 4) || !ensure(quarter2, w / 4, h / 4)) {
            return;
         }

         mask(quarter1, w, h, 4);
         blur(quarter1, quarter2, Math.max(0.5F, 9.0F * px / 4.0F));
      }

      boolean hasBg = bgValid && bgDiv == 2 && half1.fbo != 0;
      if (RawFramebufferUtil.bindDirect(main)) {
         bind(2, hasBg ? half1.tex : 0, samplerLinear);
         bind(3, haloAmt > 0.01F ? quarter1.tex : 0, samplerLinear);
         int c = HandShaders.getHoloColor();
         holoShader.bind();
         holoShader.setUniform1i("ColorTexture", 0);
         holoShader.setUniform1i("DepthTexture", 1);
         holoShader.setUniform1i("Background", 2);
         holoShader.setUniform1i("HaloTexture", 3);
         holoShader.setUniform2f("texel", 1.0F / w, 1.0F / h);
         holoShader.setUniform1f("pxScale", px);
         holoShader.setUniform3f("holoColor", r(c), g(c), b(c));
         holoShader.setUniform1f("opacity", HandShaders.getHoloOpacity());
         holoShader.setUniform1f("scanlines", HandShaders.getHoloScanlines() ? 1.0F : 0.0F);
         holoShader.setUniform1f("glitch", HandShaders.getHoloGlitch() ? 1.0F : 0.0F);
         holoShader.setUniform1f("halo", haloAmt);
         holoShader.setUniform1f("hasBackground", hasBg ? 1.0F : 0.0F);
         holoShader.setUniform1f("time", time());
         ShaderHelper.drawFullScreenQuad();
         holoShader.unbind();
      }
   }

   private static void wisp(RenderTarget main, int w, int h) {
      int qw = Math.max(1, w / 4);
      int qh = Math.max(1, h / 4);
      boolean resized = wispA.w != qw || wispA.h != qh;
      if (ensure(quarter1, qw, qh) && ensure(wispA, qw, qh) && ensure(wispB, qw, qh)) {
         long now = System.nanoTime();
         float dt = wispLastNanos == 0L ? 0.016666668F : (float)(now - wispLastNanos) / 1.0E9F;
         boolean stale = wispLastNanos == 0L || dt > 0.5F || resized;
         wispLastNanos = now;
         dt = Math.max(0.001F, Math.min(dt, 0.05F));
         if (stale) {
            clear(wispA);
            clear(wispB);
            windVY = 0.0F;
            windVX = 0.0F;
            windY = 0.0F;
            windX = 0.0F;
            haveLook = false;
         }

         updateWind(dt, (float)w / h);
         mask(quarter1, w, h, 4);
         float life = HandShaders.getWispTrail();
         float decay = (float)Math.exp(-dt / Math.max(0.03F, life / 3.0F));
         target(wispB);
         bind(2, wispA.tex, samplerLinear);
         bind(3, quarter1.tex, samplerLinear);
         wispAdvect.bind();
         wispAdvect.setUniform1i("History", 2);
         wispAdvect.setUniform1i("Mask", 3);
         wispAdvect.setUniform2f("wind", windX, windY);
         wispAdvect.setUniform1f("dt", dt);
         wispAdvect.setUniform1f("decay", decay);
         wispAdvect.setUniform1f("turbulence", HandShaders.getWispTurbulence());
         wispAdvect.setUniform1f("time", time());
         wispAdvect.setUniform1f("aspect", (float)w / h);
         wispAdvect.setUniform1f("rise", 0.05F * HandShaders.getWispTurbulence());
         wispAdvect.setUniform2f("texel", 1.0F / qw, 1.0F / qh);
         wispAdvect.setUniform1f("spread", Math.min(3.0F, 2.0F * (float)Math.sqrt(dt * 60.0F)));
         ShaderHelper.drawFullScreenQuad();
         wispAdvect.unbind();
         int t = wispA.tex;
         wispA.tex = wispB.tex;
         wispB.tex = t;
         int f = wispA.fbo;
         wispA.fbo = wispB.fbo;
         wispB.fbo = f;
         if (RawFramebufferUtil.bindDirect(main)) {
            bind(2, wispA.tex, samplerLinear);
            int c = HandShaders.getWispColor();
            wispShader.bind();
            wispShader.setUniform1i("ColorTexture", 0);
            wispShader.setUniform1i("DepthTexture", 1);
            wispShader.setUniform1i("WispTexture", 2);
            wispShader.setUniform3f("wispColor", r(c), g(c), b(c));
            wispShader.setUniform1f("intensity", HandShaders.getWispIntensity());
            Shader var10000 = wispShader;

            var10000.setUniform1i("mode", switch (HandShaders.getWispMode()) {
               case "Item Colors" -> 1;
               case "Chroma" -> 2;
               default -> 0;
            });
            wispShader.setUniform1f("time", time());
            wispShader.setUniform2f("texel", 1.0F / qw, 1.0F / qh);
            wispShader.setUniform1f("aspect", (float)w / h);
            ShaderHelper.drawFullScreenQuad();
            wispShader.unbind();
         }
      }
   }

   private static void trails(RenderTarget main, int w, int h) {
      int div = switch (HandShaders.getTrailResolution()) {
         case "Full" -> 1;
         case "Quarter" -> 4;
         default -> 2;
      };
      int tw = Math.max(1, w / div);
      int th = Math.max(1, h / div);
      boolean resized = trA.w != tw || trA.h != th;
      if (ensure(trMask, tw, th) && ensure(trA, tw, th) && ensure(trB, tw, th)) {
         long now = System.nanoTime();
         float dt = trailLastNanos == 0L ? 0.016666668F : (float)(now - trailLastNanos) / 1.0E9F;
         boolean stale = trailLastNanos == 0L || dt > 0.5F || resized;
         trailLastNanos = now;
         dt = Math.max(0.001F, Math.min(dt, 0.05F));
         if (stale) {
            clear(trA);
            clear(trB);
            windVY = 0.0F;
            windVX = 0.0F;
            windY = 0.0F;
            windX = 0.0F;
            haveLook = false;
            lastEcho = 0L;
         }

         updateWind(dt, (float)w / h, HandShaders.getTrailFollow());
         mask(trMask, w, h, Math.max(1, div));
         float length = Math.max(0.03F, HandShaders.getTrailLength());
         float decay = (float)Math.exp(-dt * 3.0 / length);
         float deposit = 1.0F;
         if (HandShaders.getTrailEcho()) {
            long spacing = (long)(Math.max(10.0F, HandShaders.getTrailEchoSpacing()) * 1000000.0F);
            if (lastEcho != 0L && now - lastEcho < spacing) {
               deposit = 0.0F;
            } else {
               lastEcho = now;
            }
         }

         deposit *= HandShaders.getTrailStrength();
         target(trB);
         bind(2, trA.tex, samplerLinear);
         bind(3, trMask.tex, samplerLinear);
         trailAdvect.bind();
         trailAdvect.setUniform1i("History", 2);
         trailAdvect.setUniform1i("Mask", 3);
         trailAdvect.setUniform2f("velocity", windX + HandShaders.getTrailDriftX(), windY + HandShaders.getTrailDriftY());
         trailAdvect.setUniform1f("dt", dt);
         trailAdvect.setUniform1f("decay", decay);
         trailAdvect.setUniform1f("deposit", deposit);
         trailAdvect.setUniform1f("turbulence", HandShaders.getTrailTurbulence());
         trailAdvect.setUniform1f("softness", HandShaders.getTrailSoftness() * Math.min(1.0F, (float)Math.sqrt(dt * 60.0F)));
         trailAdvect.setUniform1f("grow", HandShaders.getTrailGrow() * dt * 60.0F);
         trailAdvect.setUniform1f("time", time());
         trailAdvect.setUniform1f("aspect", (float)w / h);
         trailAdvect.setUniform2f("texel", 1.0F / tw, 1.0F / th);
         ShaderHelper.drawFullScreenQuad();
         trailAdvect.unbind();
         int t = trA.tex;
         trA.tex = trB.tex;
         trB.tex = t;
         int f = trA.fbo;
         trA.fbo = trB.fbo;
         trB.fbo = f;
         if (RawFramebufferUtil.bindDirect(main)) {
            bind(2, trA.tex, samplerLinear);
            int c1 = HandShaders.getTrailColor();
            int c2 = HandShaders.getTrailColor2();
            trailShader.bind();
            trailShader.setUniform1i("ColorTexture", 0);
            trailShader.setUniform1i("DepthTexture", 1);
            trailShader.setUniform1i("TrailTexture", 2);
            trailShader.setUniform2f("trailTexel", 1.0F / tw, 1.0F / th);
            Shader var29 = trailShader;

            var29.setUniform1i("mode", switch (HandShaders.getTrailMode()) {
               case "Color" -> 1;
               case "Gradient" -> 2;
               case "Chroma" -> 3;
               default -> 0;
            });
            trailShader.setUniform3f("color1", r(c1), g(c1), b(c1));
            trailShader.setUniform3f("color2", r(c2), g(c2), b(c2));
            trailShader.setUniform1f("opacity", HandShaders.getTrailOpacity());
            var29 = trailShader;

            var29.setUniform1i("fadeCurve", switch (HandShaders.getTrailFade()) {
               case "Smooth" -> 1;
               case "Sharp" -> 2;
               case "Hold" -> 3;
               default -> 0;
            });
            var29 = trailShader;

            var29.setUniform1i("blendMode", switch (HandShaders.getTrailBlend()) {
               case "Additive" -> 1;
               case "Screen" -> 2;
               default -> 0;
            });
            trailShader.setUniform1f("saturation", HandShaders.getTrailSaturation());
            trailShader.setUniform1f("brightness", HandShaders.getTrailBrightness());
            trailShader.setUniform1f("edgeOnly", HandShaders.getTrailEdgeOnly() ? 1.0F : 0.0F);
            trailShader.setUniform1f("overHand", HandShaders.getTrailOverHand() ? 1.0F : 0.0F);
            trailShader.setUniform1f("chromaSpeed", HandShaders.getTrailChromaSpeed());
            trailShader.setUniform1f("time", time());
            ShaderHelper.drawFullScreenQuad();
            trailShader.unbind();
         }
      }
   }

   private static void updateWind(float dt, float aspect) {
      updateWind(dt, aspect, HandShaders.getWispInertia());
   }

   private static void updateWind(float dt, float aspect, float inertia) {
      Minecraft mc = Minecraft.getInstance();
      float tx = 0.0F;
      float ty = 0.0F;
      if (mc.player != null) {
         float yaw = mc.player.getYRot();
         float pitch = mc.player.getXRot();
         if (haveLook) {
            float dYaw = Mth.wrapDegrees(yaw - lastYaw) / dt;
            float dPitch = (pitch - lastPitch) / dt;
            float f = 1.428F;
            tx -= (float)Math.toRadians(dYaw) * f * 0.5F / aspect;
            ty += (float)Math.toRadians(dPitch) * f * 0.5F;
         }

         lastYaw = yaw;
         lastPitch = pitch;
         haveLook = true;
         Vec3 v = mc.player.getDeltaMovement();
         double yr = Math.toRadians(yaw);
         double right = -Math.cos(yr) * v.x + -Math.sin(yr) * v.z;
         tx -= (float)right * 20.0F * 0.1F;
         ty -= (float)v.y * 20.0F * 0.06F;
      }

      float follow = 0.7F * inertia;
      tx *= follow;
      ty *= follow;
      float len = (float)Math.hypot(tx, ty);
      if (len > 1.6F) {
         tx *= 1.6F / len;
         ty *= 1.6F / len;
      }

      float k = 55.0F;
      float damp = 6.5F;
      int sub = Math.max(1, (int)Math.ceil(dt * 240.0F));
      float hstep = dt / sub;

      for (int i = 0; i < sub; i++) {
         windVX = windVX + ((tx - windX) * 55.0F - windVX * 6.5F) * hstep;
         windVY = windVY + ((ty - windY) * 55.0F - windVY * 6.5F) * hstep;
         windX = windX + windVX * hstep;
         windY = windY + windVY * hstep;
      }
   }

   private static void clear(HandFx.Target t) {
      target(t);
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClear(16384);
   }

   private static void mask(HandFx.Target dst, int w, int h, int div) {
      target(dst);
      maskShader.bind();
      maskShader.setUniform1i("ColorTexture", 0);
      maskShader.setUniform1i("DepthTexture", 1);
      maskShader.setUniform2f("srcTexel", 1.0F / w, 1.0F / h);
      maskShader.setUniform1f("spread", div == 2 ? 0.5F : 1.0F);
      ShaderHelper.drawFullScreenQuad();
      maskShader.unbind();
   }

   private static void blur(HandFx.Target a, HandFx.Target b, float sigma) {
      sigma = Math.min(sigma, 16.0F);
      blurShader.bind();
      blurShader.setUniform1i("Source", 2);
      blurShader.setUniform1f("sigma", sigma);
      target(b);
      bind(2, a.tex, samplerLinear);
      blurShader.setUniform2f("direction", 1.0F / a.w, 0.0F);
      ShaderHelper.drawFullScreenQuad();
      target(a);
      bind(2, b.tex, samplerLinear);
      blurShader.setUniform2f("direction", 0.0F, 1.0F / b.h);
      ShaderHelper.drawFullScreenQuad();
      blurShader.unbind();
   }

   private static void target(HandFx.Target t) {
      GL30.glBindFramebuffer(36160, t.fbo);
      GL11.glDrawBuffer(36064);
      GlStateManager._viewport(0, 0, t.w, t.h);
   }

   private static void bind(int unit, int tex, int sampler) {
      GlStateManager._activeTexture(33984 + unit);
      GlStateManager._bindTexture(tex);
      GL33.glBindSampler(unit, sampler);
      GlStateManager._activeTexture(33984);
   }

   private static boolean ensure(HandFx.Target t, int w, int h) {
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
      GL11.glTexImage2D(3553, 0, 34842, w, h, 0, 6408, 5126, (FloatBuffer)null);
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
      if (!ok) {
      }

      return ok;
   }

   private static float time() {
      return (float)((System.nanoTime() - START) / 1.0E9 % 3600.0);
   }

   private static float r(int c) {
      return (c >> 16 & 0xFF) / 255.0F;
   }

   private static float g(int c) {
      return (c >> 8 & 0xFF) / 255.0F;
   }

   private static float b(int c) {
      return (c & 0xFF) / 255.0F;
   }

   private static final class Target {
      int tex;
      int fbo;
      int w;
      int h;
   }
}
