package need.clarity.clickgui;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import need.clarity.Clarity;
import need.clarity.nvg.BackdropBlur;
import need.clarity.nvg.ClarityFonts;
import need.clarity.render.GuiItems;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;

public final class ClickGuiRenderer {
   private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath("clarity", "clickgui-overlay");
   private static long vg;
   private static NvgCanvas canvas;
   private static NVGLUFramebuffer fb;
   private static NVGLUFramebuffer snapFb;
   private static int fbW;
   private static int fbH;
   private static Shader bgShader;
   private static boolean failed = false;
   private static boolean bgFailed = false;
   private static final long START = System.nanoTime();
   private static int bgVao = 0;
   private static int bgVbo = 0;
   private static int sceneCopyTex = 0;
   private static int sceneCopyFbo = 0;
   private static int sceneReadFbo = 0;
   private static int sceneW = 0;
   private static int sceneH = 0;
   private static boolean sceneValid = false;
   private static int failStreak = 0;
   private static final int GIVE_UP_AFTER = 5;
   public static boolean overMenu = false;

   private ClickGuiRenderer() {
   }

   public static boolean hasFailed() {
      return failed;
   }

   public static void resetFailure() {
      failed = false;
      failStreak = 0;
   }

   public static void resetScene() {
      sceneValid = false;
   }

   public static void captureScene(ClickGui gui) {
      if (!failed) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.getMainRenderTarget();
         if (main != null && main.width > 0 && main.height > 0) {
            if (!gui.wantsScene()) {
               sceneValid = false;
            } else {
               try (RawGlState ignored = RawGlState.capture()) {
                  int mainTex = RawFramebufferUtil.getColorTextureId(main);
                  int blurred;
                  int w;
                  int h;
                  if (gui.blur() <= 0) {
                     blurred = mainTex;
                     w = main.width;
                     h = main.height;
                  } else {
                     blurred = BackdropBlur.capture(mainTex, main.width, main.height, gui.blur());
                     w = BackdropBlur.width();
                     h = BackdropBlur.height();
                  }

                  if (blurred == 0) {
                     sceneValid = false;
                  } else if (!ensureSceneCopy(w, h)) {
                     sceneValid = false;
                  } else {
                     GL33C.glBindFramebuffer(36008, sceneReadFbo);
                     GL33C.glFramebufferTexture2D(36008, 36064, 3553, blurred, 0);
                     GL33C.glBindFramebuffer(36009, sceneCopyFbo);
                     GL33C.glDisable(3089);
                     GL33C.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, 16384, 9728);
                     sceneValid = true;
                  }
               } catch (Throwable t) {
                  sceneValid = false;
               }
            }
         }
      }
   }

   private static boolean ensureSceneCopy(int w, int h) {
      if (sceneCopyFbo != 0 && sceneW == w && sceneH == h) {
         return true;
      }

      if (sceneCopyTex == 0) {
         sceneCopyTex = GL33C.glGenTextures();
      }

      if (sceneCopyFbo == 0) {
         sceneCopyFbo = GL33C.glGenFramebuffers();
      }

      if (sceneReadFbo == 0) {
         sceneReadFbo = GL33C.glGenFramebuffers();
      }

      GL33C.glActiveTexture(33984);
      GL33C.glBindBuffer(35052, 0);
      GL33C.glBindTexture(3553, sceneCopyTex);
      GL33C.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
      GL33C.glTexParameteri(3553, 10241, 9729);
      GL33C.glTexParameteri(3553, 10240, 9729);
      GL33C.glTexParameteri(3553, 10242, 33071);
      GL33C.glTexParameteri(3553, 10243, 33071);
      GL33C.glTexParameteri(3553, 33085, 0);
      GL33C.glBindFramebuffer(36160, sceneCopyFbo);
      GL33C.glFramebufferTexture2D(36160, 36064, 3553, sceneCopyTex, 0);
      if (GL33C.glCheckFramebufferStatus(36160) != 36053) {
         sceneH = 0;
         sceneW = 0;
         return false;
      } else {
         sceneW = w;
         sceneH = h;
         return true;
      }
   }

   public static float designScale(ClickGui gui) {
      Minecraft mc = Minecraft.getInstance();
      float h = Math.max(1, mc.getWindow().getHeight());
      return Math.max(0.55F, Math.min(2.4F, h / 900.0F)) * Math.max(0.5F, Math.min(1.6F, gui.scale()));
   }

   public static void render(GuiGraphicsExtractor ctx, ClickGui gui) {
      if (!failed) {
         Minecraft mc = Minecraft.getInstance();
         int physW = mc.getWindow().getWidth();
         int physH = mc.getWindow().getHeight();
         if (physW > 0 && physH > 0) {
            try (RawGlState ignored = RawGlState.capture()) {
               ensure(mc, physW, physH);
               int sceneTex = sceneValid ? sceneCopyTex : 0;
               canvas.setGlassSource(sceneTex, sceneW, sceneH);
               canvas.setCrisp(gui.crispText());
               float ds = designScale(gui);
               if (gui.snapshotRequest() != 0) {
                  boolean ok = false;

                  try {
                     NanoVGGL3.nvgluBindFramebuffer(vg, snapFb);
                     GL33C.glViewport(0, 0, fbW, fbH);
                     GL33C.glDisable(3089);
                     GL33C.glColorMask(true, true, true, true);
                     GL33C.glStencilMask(255);
                     GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                     GL33C.glClearStencil(0);
                     GL33C.glClear(17408);
                     NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
                     canvas.begin(fbW, fbH);
                     NanoVG.nvgSave(vg);
                     NanoVG.nvgScale(vg, ds, ds);
                     gui.renderSnapshot(canvas, physW / ds, physH / ds);
                     NanoVG.nvgRestore(vg);
                     NanoVG.nvgEndFrame(vg);
                     canvas.setSnapshot(snapFb.texture(), fbW, fbH);
                     ok = true;
                  } catch (Throwable var12) {
                  }

                  gui.snapshotTaken(ok);
               }

               NanoVGGL3.nvgluBindFramebuffer(vg, fb);
               GL33C.glViewport(0, 0, fbW, fbH);
               GL33C.glDisable(3089);
               GL33C.glColorMask(true, true, true, true);
               GL33C.glStencilMask(255);
               GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GL33C.glClearStencil(0);
               GL33C.glClear(17408);
               GL33C.glDisable(2929);
               GL33C.glDisable(2884);
               GL33C.glBindBuffer(35052, 0);
               GL33C.glPixelStorei(3317, 1);
               GL33C.glPixelStorei(3314, 0);
               GL33C.glPixelStorei(3316, 0);
               GL33C.glPixelStorei(3315, 0);

               for (int u = 0; u < 4; u++) {
                  GL33C.glBindSampler(u, 0);
               }

               GL33C.glActiveTexture(33984);
               if (gui.backgroundOn() && !overMenu) {
                  drawBackground(gui, sceneTex, physW, physH);
               }

               NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
               canvas.begin(fbW, fbH);
               NanoVG.nvgSave(vg);
               NanoVG.nvgScale(vg, ds, ds);
               gui.render(canvas, physW / ds, physH / ds, System.nanoTime());
               NanoVG.nvgRestore(vg);
               NanoVG.nvgEndFrame(vg);
               failStreak = 0;
            } catch (Throwable t) {
               try {
                  if (vg != 0L) {
                     NanoVG.nvgCancelFrame(vg);
                  }
               } catch (Throwable var10) {
               }

               failStreak++;
               if (failStreak == 1 || failStreak == 5) {
                  Clarity.LOGGER.error("[Clarity] ClickGUI frame failed to draw (" + failStreak + " in a row)", t);
               }

               if (failStreak >= 5) {
                  failed = true;
               }

               return;
            }

            float gs = mc.getWindow().getGuiScale();
            ctx.pose().pushMatrix();
            ctx.pose().scale(1.0F / gs, 1.0F / gs);
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, OVERLAY_ID, 0, 0, 0.0F, fbH, fbW, fbH, fbW, -fbH, fbW, fbH);
            ctx.pose().popMatrix();
            GuiItems.drawIcons(ctx, canvas.icons(), gs);
         }
      }
   }

   private static void drawBackground(ClickGui gui, int sceneTex, int w, int h) {
      if (!bgFailed) {
         try {
            if (bgShader == null) {
               bgShader = new Shader("clickgui", "bg");
            }
         } catch (Exception e) {
            bgFailed = true;
            return;
         }

         GL33C.glDisable(3042);
         GL33C.glActiveTexture(33984);
         GL33C.glBindTexture(3553, sceneTex);
         bgShader.bind();
         bgShader.setUniform1i("uScene", 0);
         bgShader.setUniform1f("uTime", (float)((System.nanoTime() - START) / 1.0E9 % 3600.0));
         bgShader.setUniform1f("uOpen", gui.openProgress());
         bgShader.setUniform1f("uAspect", (float)w / h);
         bgShader.setUniform1f("uDim", gui.backgroundDim());
         bgShader.setUniform1i("uHasScene", sceneTex != 0 && !gui.blurWindowOnly() ? 1 : 0);
         int a1 = gui.bgAccent();
         int a2 = gui.bgAccent2();
         bgShader.setUniform3f("uAccent", (a1 >> 16 & 0xFF) / 255.0F, (a1 >> 8 & 0xFF) / 255.0F, (a1 & 0xFF) / 255.0F);
         bgShader.setUniform3f("uAccent2", (a2 >> 16 & 0xFF) / 255.0F, (a2 >> 8 & 0xFF) / 255.0F, (a2 & 0xFF) / 255.0F);
         Shader var10000 = bgShader;

         var10000.setUniform1i("uStyle", switch (gui.background()) {
            case "Nebula" -> 1;
            case "Waves" -> 2;
            case "Solid" -> 3;
            default -> 0;
         });
         if (bgVao == 0) {
            bgVao = GL33C.glGenVertexArrays();
            bgVbo = GL33C.glGenBuffers();
            GL33C.glBindVertexArray(bgVao);
            GL33C.glBindBuffer(34962, bgVbo);
            GL33C.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
            GL33C.glEnableVertexAttribArray(0);
            GL33C.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
         }

         GL33C.glBindVertexArray(bgVao);
         GL33C.glDrawArrays(4, 0, 3);
         GL33C.glBindVertexArray(0);
         bgShader.unbind();
      }
   }

   private static void ensure(Minecraft mc, int w, int h) {
      if (vg == 0L) {
         vg = NanoVGGL3.nvgCreate(3);
         if (vg == 0L) {
            throw new IllegalStateException("nvgCreate failed");
         }

         ClarityFonts.loadInto(vg);
         canvas = new NvgCanvas(vg);
         canvas.iconSource = GuiItems::has;
      }

      if (fb == null || fbW != w || fbH != h) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         if (snapFb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, snapFb);
            snapFb = null;
         }

         canvas.forgetImages();
         fb = NanoVGGL3.nvgluCreateFramebuffer(vg, w, h, 0);
         if (fb == null) {
            throw new IllegalStateException("nvgluCreateFramebuffer failed");
         }

         snapFb = NanoVGGL3.nvgluCreateFramebuffer(vg, w, h, 0);
         if (snapFb == null) {
            throw new IllegalStateException("nvgluCreateFramebuffer (snapshot) failed");
         }

         fbW = w;
         fbH = h;
         GlTexture tex = new ClickGuiRenderer.BridgeGlTexture(fb.texture(), fbW, fbH);
         GpuTextureView view = RenderSystem.getDevice().createTextureView(tex);
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
         mc.getTextureManager().register(OVERLAY_ID, new ClickGuiRenderer.BridgeTexture(tex, view, sampler));
      }
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId, int w, int h) {
         super(12, "clarity-clickgui-overlay", TextureFormat.RGBA8, w, h, 1, 1, glId);
      }

      public void close() {
      }
   }

   private static final class BridgeTexture extends AbstractTexture {
      BridgeTexture(GpuTexture tex, GpuTextureView view, GpuSampler sampler) {
         this.texture = tex;
         this.textureView = view;
         this.sampler = sampler;
      }

      public void close() {
      }
   }
}
