package need.clarity.render.esp;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.audio.AudioDemand;
import need.clarity.modules.visual.ItemESP;
import need.clarity.modules.visual.ShaderESP;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import need.clarity.render.Shader;
import need.clarity.render.ShaderHelper;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.render.worldfx.CorpseFx;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public final class ShaderEspRenderer {
   private static final Map<EntityRenderState, Integer> tagged = new IdentityHashMap<>();
   private static boolean taggedThisFrame = false;
   private static long frame = 0L;
   private static long maskFrame = -1L;
   private static long compositeFrame = -1L;
   private static final Set<Object> outlinePasses = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private static Shader downShader;
   private static Shader blurShader;
   private static Shader glowShader;
   private static Shader blurModeShader;
   private static Shader galaxyShader;
   private static Shader prismShader;
   private static Shader neonShader;
   private static Shader scanShader;
   private static Shader liquidShader;
   private static Shader outlineShader;
   private static Shader contourShader;
   private static Shader celShader;
   private static Shader pulseShader;
   private static final AudioDemand.Frame audio = new AudioDemand.Frame();
   private static boolean initDone = false;
   private static boolean initFailed = false;
   private static final ShaderEspRenderer.Target mask = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target g2a = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target g2b = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target g4a = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target g4b = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target s2a = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target s2b = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target s4a = new ShaderEspRenderer.Target();
   private static final ShaderEspRenderer.Target s4b = new ShaderEspRenderer.Target();
   private static int samplerLinear = 0;
   private static final long START = System.nanoTime();
   private static double animTime = 0.0;
   private static long lastNanos = 0L;
   public static final int COMBO_TAG = 16646398;
   private static volatile int comboId = Integer.MIN_VALUE;
   private static volatile int comboTagColor = 16646398;
   private static volatile int pendingId = Integer.MIN_VALUE;
   private static volatile int pendingStyle = 0;
   private static volatile long pendingAt = 0L;
   private static final Map<Integer, Long> hidden = new ConcurrentHashMap<>();
   private static Shader capShader;

   private ShaderEspRenderer() {
   }

   public static void setComboTarget(int id) {
      if (pendingId == Integer.MIN_VALUE) {
         comboId = id;
      }
   }

   public static void requestCorpse(int id, int style) {
      pendingId = id;
      pendingStyle = style;
      pendingAt = System.nanoTime();
      comboId = id;
   }

   public static boolean isHidden(int id) {
      Long t = hidden.get(id);
      if (t == null) {
         return false;
      } else if (System.nanoTime() - t > 4000000000L) {
         hidden.remove(id);
         return false;
      } else {
         return true;
      }
   }

   private static boolean espOn() {
      return ShaderESP.active() || ItemESP.shaderActive();
   }

   private static boolean anyOn() {
      return espOn() || comboId != Integer.MIN_VALUE;
   }

   public static void tagState(Entity entity, EntityRenderState state) {
      int rgb = -1;
      if (entity instanceof Player p && ShaderESP.active() && ShaderESP.shouldTarget(p)) {
         rgb = ShaderESP.colorFor(p) & 16777215;
      } else if (entity instanceof ItemEntity && ItemESP.shaderActive() && ItemESP.shaderInclude(entity)) {
         rgb = ItemESP.shaderColor() & 16777215;
      }

      boolean combo = entity != null && entity.getId() == comboId;
      if (rgb >= 0 || combo) {
         if (rgb < 0) {
            rgb = 16646398;
         } else {
            int r = rgb >> 16 & 0xFF;
            int g = rgb >> 8 & 0xFF;
            int b = rgb & 0xFF;
            if (Math.max(r, Math.max(g, b)) < 8) {
               rgb = 526344;
            }

            if (rgb == 0) {
               rgb ^= 65793;
            }

            if (rgb == 16646398) {
               rgb ^= 131072;
            }
         }

         if (combo) {
            comboTagColor = rgb;
         }

         state.outlineColor = rgb;
         synchronized (tagged) {
            tagged.put(state, rgb);
         }
      }
   }

   public static void afterExtract(LevelRenderState level) {
      frame++;
      boolean any = false;
      synchronized (tagged) {
         if (!tagged.isEmpty() && anyOn() && level != null && level.entityRenderStates != null) {
            for (EntityRenderState s : level.entityRenderStates) {
               Integer c = tagged.get(s);
               if (c != null) {
                  s.outlineColor = c;
                  any = true;
               }
            }
         }

         tagged.clear();
      }

      taggedThisFrame = any;
      if (any) {
         level.haveGlowingEntities = true;
      }
   }

   public static void captureMask() {
      if (taggedThisFrame && anyOn()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.levelRenderer != null) {
            RenderTarget outline = mc.levelRenderer.entityOutlineTarget();
            if (outline != null && outline.width > 0 && outline.height > 0) {
               try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
                  if (ensure(mask, outline.width, outline.height, false)) {
                     if (RawFramebufferUtil.bindDirect(outline)) {
                        GlStateManager._disableScissorTest();
                        GL30.glBindFramebuffer(36009, mask.fbo);
                        GL30.glBlitFramebuffer(0, 0, outline.width, outline.height, 0, 0, mask.w, mask.h, 16384, 9728);
                        maskFrame = frame;
                     }
                  }
               } catch (Throwable var7) {
               }
            }
         }
      }
   }

   public static void registerOutlinePasses(List<?> passes) {
      if (passes != null) {
         for (Object p : passes) {
            if (p != null) {
               outlinePasses.add(p);
            }
         }
      }
   }

   public static boolean onOutlinePass(Object pass) {
      if (taggedThisFrame && anyOn() && outlinePasses.contains(pass)) {
         if (compositeFrame != frame) {
            captureMask();
            if (maskFrame == frame) {
               processCorpse();
               if (espOn()) {
                  drawComposite();
               }
            }

            compositeFrame = frame;
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean onDoEntityOutline() {
      if (taggedThisFrame && anyOn()) {
         if (compositeFrame != frame && maskFrame == frame) {
            processCorpse();
            if (espOn()) {
               drawComposite();
            }

            compositeFrame = frame;
         }

         return true;
      } else {
         return false;
      }
   }

   private static void drawComposite() {
      if (ensureInit()) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.getMainRenderTarget();
         if (main != null && main.width > 0 && main.height > 0 && mask.fbo != 0) {
            tick();

            try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
               int[] prevSampler = new int[3];

               for (int i = 0; i < 3; i++) {
                  GlStateManager._activeTexture(33984 + i);
                  prevSampler[i] = GL11.glGetInteger(35097);
               }

               try {
                  draw(main);
               } finally {
                  for (int i = 0; i < 3; i++) {
                     GL33.glBindSampler(i, prevSampler[i]);
                  }

                  GlStateManager._activeTexture(33984);
               }
            } catch (Throwable var14) {
            }
         }
      }
   }

   private static void processCorpse() {
      if (pendingId != Integer.MIN_VALUE) {
         int id = pendingId;
         if (System.nanoTime() - pendingAt > 1500000000L) {
            pendingId = Integer.MIN_VALUE;
            comboId = Integer.MIN_VALUE;
         } else {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(id);
            RenderTarget main = mc.getMainRenderTarget();
            if (e != null && main != null && mask.fbo != 0) {
               try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
                  if (capShader == null) {
                     capShader = new Shader("esp", "corpse_cap");
                  }

                  if (ensureInit()) {
                     Matrix4f vp = new Matrix4f();
                     if (SkyShaderRenderer.levelViewProjection(vp)) {
                        Vec3 cam = ClarityRenderUtils.getCameraPos();
                        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                        Vec3 lerp = e.getPosition(pt);
                        AABB bb = e.getBoundingBox().move(lerp.x - e.getX(), lerp.y - e.getY(), lerp.z - e.getZ());
                        double cx = (bb.minX + bb.maxX) * 0.5;
                        double cy = (bb.minY + bb.maxY) * 0.5;
                        double cz = (bb.minZ + bb.maxZ) * 0.5;
                        double half = Math.max(bb.getXsize(), bb.getYsize()) * 0.65 + 0.15;
                        double minX = 9.0;
                        double minY = 9.0;
                        double maxX = -9.0;
                        double maxY = -9.0;
                        Vector4f v = new Vector4f();

                        for (int i = 0; i < 8; i++) {
                           v.set((float)(cx + ((i & 1) == 0 ? -half : half) - cam.x), (float)(cy + ((i & 2) == 0 ? -half : half) - cam.y), (float)(cz + ((i & 4) == 0 ? -half : half) - cam.z), 1.0F);
                           vp.transform(v);
                           if (v.w <= 0.05F) {
                              pendingId = Integer.MIN_VALUE;
                              comboId = Integer.MIN_VALUE;
                              return;
                           }

                           minX = Math.min(minX, v.x / v.w);
                           maxX = Math.max(maxX, v.x / v.w);
                           minY = Math.min(minY, v.y / v.w);
                           maxY = Math.max(maxY, v.y / v.w);
                        }

                        minX = Math.max(-1.0, minX);
                        minY = Math.max(-1.0, minY);
                        maxX = Math.min(1.0, maxX);
                        maxY = Math.min(1.0, maxY);
                        if (!(maxX - minX < 0.005) && !(maxY - minY < 0.005)) {
                           double pw = (maxX - minX) * 0.5 * main.width;
                           double ph = (maxY - minY) * 0.5 * main.height;
                           double k = Math.min(1.0, 512.0 / Math.max(pw, ph));
                           int tw = Math.max(8, (int)Math.round(pw * k));
                           int th = Math.max(8, (int)Math.round(ph * k));
                           CorpseFx.Slot slot = CorpseFx.acquire(tw, th);
                           int[] prevSampler = new int[2];

                           for (int i = 0; i < 2; i++) {
                              GlStateManager._activeTexture(33984 + i);
                              prevSampler[i] = GL11.glGetInteger(35097);
                           }

                           try {
                              GlStateManager._disableBlend();
                              GlStateManager._disableDepthTest();
                              GlStateManager._disableScissorTest();
                              GlStateManager._disableCull();
                              GL30.glBindFramebuffer(36160, CorpseFx.fbo(slot));
                              GL11.glDrawBuffer(36064);
                              GlStateManager._viewport(0, 0, tw, th);
                              bind(0, mask.tex);
                              bind(1, RawFramebufferUtil.getColorTextureId(main));
                              capShader.bind();
                              capShader.setUniform1i("Mask", 0);
                              capShader.setUniform1i("Scene", 1);
                              capShader.setUniform4f("uRect", (float)(minX * 0.5 + 0.5), (float)(minY * 0.5 + 0.5), (float)(maxX * 0.5 + 0.5), (float)(maxY * 0.5 + 0.5));
                              int tc = comboTagColor;
                              capShader.setUniform3f("uTag", (tc >> 16 & 0xFF) / 255.0F, (tc >> 8 & 0xFF) / 255.0F, (tc & 0xFF) / 255.0F);
                              ShaderHelper.drawFullScreenQuad();
                              capShader.unbind();
                           } finally {
                              for (int i = 0; i < 2; i++) {
                                 GL33.glBindSampler(i, prevSampler[i]);
                              }

                              GlStateManager._activeTexture(33984);
                           }

                           float[] var84 = new float[3];
                           float[] U = new float[3];
                           float[] var85 = new float[3];
                           SkyShaderRenderer.cameraAxes(var84, U, var85);
                           double rx = cx - cam.x;
                           double ry = cy - cam.y;
                           double rz = cz - cam.z;
                           v.set((float)rx, (float)ry, (float)rz, 1.0F);
                           vp.transform(v);
                           double bx = v.x / v.w;
                           double by = v.y / v.w;
                           v.set((float)(rx + var84[0]), (float)(ry + var84[1]), (float)(rz + var84[2]), 1.0F);
                           vp.transform(v);
                           double kx = v.x / v.w - bx;
                           v.set((float)(rx + U[0]), (float)(ry + U[1]), (float)(rz + U[2]), 1.0F);
                           vp.transform(v);
                           double ky = v.y / v.w - by;
                           if (!(Math.abs(kx) < 1.0E-5) && !(Math.abs(ky) < 1.0E-5)) {
                              double W = (maxX - minX) / kx;
                              double H = (maxY - minY) / ky;
                              double offR = ((minX + maxX) * 0.5 - bx) / kx;
                              double offU = ((minY + maxY) * 0.5 - by) / ky;
                              CorpseFx.addAnchored(slot, new double[]{cx, cy, cz}, offR, offU, W, H, bb.minY, pendingStyle);
                              hidden.put(id, System.nanoTime());
                           }
                        } else {
                           pendingId = Integer.MIN_VALUE;
                           comboId = Integer.MIN_VALUE;
                        }
                     }
                  }
               } catch (Throwable var76) {
               } finally {
                  pendingId = Integer.MIN_VALUE;
                  comboId = Integer.MIN_VALUE;
               }
            } else {
               pendingId = Integer.MIN_VALUE;
               comboId = Integer.MIN_VALUE;
            }
         }
      }
   }

   private static double[] unproject(Matrix4f inv, double x, double y, float z, Vec3 cam) {
      Vector4f p = new Vector4f((float)x, (float)y, z, 1.0F);
      inv.transform(p);
      return new double[]{p.x / p.w + cam.x, p.y / p.w + cam.y, p.z / p.w + cam.z};
   }

   private static boolean draw(RenderTarget main) {
      int w = main.width;
      int h = main.height;
      float px = h / 1080.0F;
      boolean esp = ShaderESP.active();
      String mode = esp ? ShaderESP.mode() : "Glow";
      boolean blurMode = "Blur".equals(mode);
      boolean liquidMode = esp && "Liquid Glass".equals(mode);
      boolean pulseMode = esp && "Pulse".equals(mode);
      if (pulseMode) {
         audio.reactivity = ShaderESP.reactivity();
         AudioDemand.sample(audio, animTime, ShaderESP.idleBeats() ? 0.6F : 0.0F);
      }

      GlStateManager._disableBlend();
      GlStateManager._disableDepthTest();
      GlStateManager._disableScissorTest();
      GlStateManager._disableCull();
      float baseRadius = esp ? ShaderESP.glowRadius() : ItemESP.shaderRadius();
      float radius = blurMode ? Math.min(baseRadius, 8.0F) : baseRadius;
      float sigma = Math.max(0.5F, radius * px / 1.6F);
      if (esp) {
         switch (mode) {
            case "Outline":
               sigma = Math.max(0.75F, ShaderESP.outlineWidth() * px);
               break;
            case "Cel":
               sigma = Math.max(0.75F, ShaderESP.outlineWidth() * px);
               break;
            case "Liquid Glass":
               sigma = Math.max(1.0F, ShaderESP.thickness() * px * 0.6F);
               break;
            case "Contour":
               sigma = Math.max(2.0F, ShaderESP.glowRadius() * px);
               break;
            case "Pulse":
               sigma = Math.max(1.0F, ShaderESP.glowRadius() * px * 0.8F);
         }
      }

      int div = sigma <= 6.0F ? 2 : 4;
      ShaderEspRenderer.Target ga = div == 2 ? g2a : g4a;
      ShaderEspRenderer.Target gb = div == 2 ? g2b : g4b;
      if (ensure(ga, w / div, h / div, true) && ensure(gb, w / div, h / div, true)) {
         bind(0, mask.tex);
         target(ga);
         downShader.bind();
         downShader.setUniform1i("Mask", 0);
         downShader.setUniform3f("uSkip", 0.99607843F, 0.0F, 0.99607843F);
         downShader.setUniform2f("srcTexel", 1.0F / mask.w, 1.0F / mask.h);
         downShader.setUniform1f("spread", div == 2 ? 0.5F : 1.0F);
         ShaderHelper.drawFullScreenQuad();
         downShader.unbind();
         blur(ga, gb, sigma / div);
         ShaderEspRenderer.Target sa = null;
         if (blurMode || liquidMode) {
            float bs = liquidMode ? ShaderESP.frost() * px : ShaderESP.blur() * px;
            int sdiv = bs <= 8.0F ? 2 : 4;
            if (liquidMode) {
               sdiv = 2;
            }

            sa = sdiv == 2 ? s2a : s4a;
            ShaderEspRenderer.Target sb = sdiv == 2 ? s2b : s4b;
            if (!ensure(sa, w / sdiv, h / sdiv, true) || !ensure(sb, w / sdiv, h / sdiv, true)) {
               return false;
            }

            if (!RawFramebufferUtil.bindDirect(main)) {
               return false;
            }

            GL30.glBindFramebuffer(36009, sa.fbo);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, sa.w, sa.h, 16384, 9729);
            if (bs / sdiv > 0.3F) {
               blur(sa, sb, bs / sdiv);
            }
         }

         if (!RawFramebufferUtil.bindDirect(main)) {
            return false;
         }

         GlStateManager._enableBlend();
         GlStateManager._blendFuncSeparate(1, 771, 1, 771);
         GL14.glBlendEquation(32774);
         bind(0, mask.tex);
         bind(2, ga.tex);
         if (sa != null) {
            bind(1, sa.tex);
         }
         Shader s = switch (mode) {
            case "Blur" -> blurModeShader;
            case "Galaxy" -> galaxyShader;
            case "Prism" -> prismShader;
            case "Neon" -> neonShader;
            case "Scan" -> scanShader;
            case "Liquid Glass" -> liquidShader;
            case "Outline" -> outlineShader;
            case "Contour" -> contourShader;
            case "Cel" -> celShader;
            case "Pulse" -> pulseShader;
            default -> glowShader;
         };
         float time = (float)(animTime % 3600.0);
         s.bind();
         s.setUniform1i("Mask", 0);
         s.setUniform1i("Scene", 1);
         s.setUniform1i("Glow", 2);
         s.setUniform2f("texel", 1.0F / w, 1.0F / h);
         s.setUniform1f("pxScale", px);
         s.setUniform1f("time", time);
         s.setUniform1i("chroma", esp && ShaderESP.chroma() ? 1 : 0);
         s.setUniform1f("outline", esp && !ShaderESP.outline() ? 0.0F : 1.0F);
         s.setUniform1f("intensity", esp ? ShaderESP.intensity() : 1.0F);
         s.setUniform1f("fill", esp ? ShaderESP.fill() : 0.3F);
         s.setUniform3f("uSkip", 0.99607843F, 0.0F, 0.99607843F);
         s.setUniform1f("tintAmount", ShaderESP.tintAmount());
         s.setUniform1f("aspect", (float)w / h);
         s.setUniform2f("glowTexel", 1.0F / ga.w, 1.0F / ga.h);
         if (s == liquidShader) {
            s.setUniform1f("refraction", ShaderESP.refraction());
            s.setUniform1f("dispersion", ShaderESP.dispersion());
            s.setUniform1f("thickness", ShaderESP.thickness());
            s.setUniform1f("tintAmount", ShaderESP.glassTint());
         } else if (s == outlineShader) {
            s.setUniform1f("thinInner", ShaderESP.hairline() ? 1.0F : 0.0F);
         } else if (s == celShader) {
            s.setUniform1f("fill", ShaderESP.celOpacity());
         } else if (s == contourShader) {
            s.setUniform1f("lineCount", ShaderESP.lines());
         } else if (s == pulseShader) {
            s.setUniform1f("uBass", audio.bass);
            s.setUniform1fv("uBeatAge", audio.beatAges);
            s.setUniform1f("uFlash", audio.flash);
         }

         if (s == prismShader) {
            float[][] tc = SkyShaders.themeBlend();
            s.setUniform3f("uAccent", tc[0][0], tc[0][1], tc[0][2]);
            s.setUniform3f("uAccent2", tc[1][0], tc[1][1], tc[1][2]);
         }

         ShaderHelper.drawFullScreenQuad();
         s.unbind();
         return true;
      } else {
         return false;
      }
   }

   private static void blur(ShaderEspRenderer.Target a, ShaderEspRenderer.Target b, float sigma) {
      sigma = Math.min(sigma, 16.0F);
      blurShader.bind();
      blurShader.setUniform1i("Source", 2);
      blurShader.setUniform1f("sigma", sigma);
      target(b);
      bind(2, a.tex);
      blurShader.setUniform2f("direction", 1.0F / a.w, 0.0F);
      ShaderHelper.drawFullScreenQuad();
      target(a);
      bind(2, b.tex);
      blurShader.setUniform2f("direction", 0.0F, 1.0F / b.h);
      ShaderHelper.drawFullScreenQuad();
      blurShader.unbind();
   }

   private static void tick() {
      long now = System.nanoTime();
      double dt = lastNanos == 0L ? 0.0 : Math.min(0.1, (now - lastNanos) / 1.0E9);
      lastNanos = now;
      animTime = animTime + dt * (ShaderESP.active() ? ShaderESP.speed() : 1.0F);
   }

   private static boolean ensureInit() {
      if (initDone) {
         return true;
      }

      if (initFailed) {
         return false;
      }

      try {
         downShader = new Shader("esp", "esp_down");
         blurShader = new Shader("hand", "fx_blur");
         glowShader = new Shader("esp", "esp_glow");
         blurModeShader = new Shader("esp", "esp_blur");
         galaxyShader = new Shader("esp", "esp_galaxy");
         prismShader = new Shader("esp", "esp_prism");
         neonShader = new Shader("esp", "esp_neon");
         scanShader = new Shader("esp", "esp_scan");
         liquidShader = new Shader("esp", "esp_liquid");
         outlineShader = new Shader("esp", "esp_outline");
         contourShader = new Shader("esp", "esp_contour");
         celShader = new Shader("esp", "esp_cel");
         pulseShader = new Shader("esp", "esp_pulse");
         samplerLinear = GL33.glGenSamplers();
         GL33.glSamplerParameteri(samplerLinear, 10241, 9729);
         GL33.glSamplerParameteri(samplerLinear, 10240, 9729);
         GL33.glSamplerParameteri(samplerLinear, 10242, 33071);
         GL33.glSamplerParameteri(samplerLinear, 10243, 33071);
         initDone = true;
      } catch (Exception e) {
         initFailed = true;
      }

      return initDone;
   }

   private static void target(ShaderEspRenderer.Target t) {
      GL30.glBindFramebuffer(36160, t.fbo);
      GL11.glDrawBuffer(36064);
      GlStateManager._viewport(0, 0, t.w, t.h);
   }

   private static void bind(int unit, int tex) {
      GlStateManager._activeTexture(33984 + unit);
      GlStateManager._bindTexture(tex);
      GL33.glBindSampler(unit, samplerLinear);
      GlStateManager._activeTexture(33984);
   }

   private static boolean ensure(ShaderEspRenderer.Target t, int w, int h, boolean hdr) {
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
      if (hdr) {
         GL11.glTexImage2D(3553, 0, 34842, w, h, 0, 6408, 5126, (FloatBuffer)null);
      } else {
         GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
      }

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

   private static final class Target {
      int tex;
      int fbo;
      int w;
      int h;
   }
}
