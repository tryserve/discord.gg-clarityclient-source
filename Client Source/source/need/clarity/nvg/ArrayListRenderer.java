package need.clarity.nvg;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.visual.ArrayList;
import need.clarity.modules.visual.ArrayListModes;
import need.clarity.modules.visual.HudScale;
import need.clarity.render.GuiItems;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.util.Chroma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

public final class ArrayListRenderer {
   private static final ArrayListRenderer INSTANCE = new ArrayListRenderer();
   private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath("clarity", "arraylist-overlay");
   private long vg;
   private boolean disabled;
   private NVGLUFramebuffer fb;
   private int fbW;
   private int fbH;
   private GpuTextureView bridgeView;
   private boolean fbValid;
   private int lastEnabledHash;
   private boolean animationsSettled;

   private ArrayListRenderer() {
   }

   public static void render(GuiGraphicsExtractor ctx, float scaledW, float scaledH, float physW, float physH) {
      if (!INSTANCE.disabled) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            HudLayout.validate();
         }

         if (mc != null && !mc.options.hideGui) {
            ArrayList module = ArrayList.INSTANCE;
            boolean listOn = module != null && module.isEnabled();
            boolean hudOn = HudElements.enabled();
            if (!listOn && !hudOn) {
               INSTANCE.fbValid = false;
            } else {
               try {
                  INSTANCE.ensureContext();
                  int ss = HudScale.supersample();
                  int fbW = Math.max(1, (int)(physW * ss));
                  int fbH = Math.max(1, (int)(physH * ss));
                  INSTANCE.ensureFramebuffer(fbW, fbH, mc);
                  int blurStrength = HudElements.backdropStrength();
                  boolean needRender = !INSTANCE.fbValid
                     || !INSTANCE.animationsSettled
                     || hudOn && HudElements.needsRedraw()
                     || blurStrength > 0
                     || HudFx.animating()
                     || Chroma.active()
                     || LiquidGlass.active()
                     || INSTANCE.hasStateChanged(module, fbW, fbH);
                  if (needRender) {
                     ArrayListRenderer.GlState saved = ArrayListRenderer.GlState.capture();

                     try {
                        if (blurStrength > 0) {
                           RenderTarget main = mc.getMainRenderTarget();
                           BackdropBlur.capture(RawFramebufferUtil.getColorTextureId(main), main.width, main.height, blurStrength);
                        } else {
                           BackdropBlur.invalidate();
                        }

                        HudFx.prepare(INSTANCE.fbW, INSTANCE.fbH);
                        NanoVGGL3.nvgluBindFramebuffer(INSTANCE.vg, INSTANCE.fb);
                        GL33C.glViewport(0, 0, INSTANCE.fbW, INSTANCE.fbH);
                        GL33C.glDisable(3089);
                        GL33C.glColorMask(true, true, true, true);
                        GL33C.glStencilMask(255);
                        GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                        GL33C.glClearStencil(0);
                        GL33C.glClear(17408);
                        GL33C.glDisable(2929);
                        GL33C.glBindBuffer(35052, 0);
                        GL33C.glBindBuffer(35051, 0);
                        GL33C.glPixelStorei(3317, 1);
                        GL33C.glPixelStorei(3314, 0);
                        GL33C.glPixelStorei(3316, 0);
                        GL33C.glPixelStorei(3315, 0);
                        GL33C.glBindSampler(0, 0);
                        float overlayScale = HudScale.factor(physW, physH);
                        float drawScale = overlayScale * ss;
                        float designW = physW / overlayScale;
                        float designH = physH / overlayScale;
                        NanoVG.nvgBeginFrame(INSTANCE.vg, INSTANCE.fbW, INSTANCE.fbH, 1.0F);
                        NanoVG.nvgSave(INSTANCE.vg);
                        NanoVG.nvgScale(INSTANCE.vg, drawScale, drawScale);
                        HudLayout.beginFrame(designW, designH);
                        if (listOn) {
                           ArrayList.draw(INSTANCE.vg, designW, designH);
                        }

                        if (hudOn) {
                           HudElements.draw(INSTANCE.vg, designW, designH);
                        }

                        NanoVG.nvgRestore(INSTANCE.vg);
                        if (LiquidGlass.active()) {
                           LiquidGlass.render(RawFramebufferUtil.getColorTextureId(mc.getMainRenderTarget()));
                           NanoVGGL3.nvgluBindFramebuffer(INSTANCE.vg, INSTANCE.fb);
                           GL33C.glViewport(0, 0, INSTANCE.fbW, INSTANCE.fbH);
                        }

                        NanoVG.nvgEndFrame(INSTANCE.vg);
                     } finally {
                        saved.restore();
                     }

                     INSTANCE.fbValid = true;
                     INSTANCE.animationsSettled = listOn ? INSTANCE.checkAnimationsSettled(module) : true;
                     INSTANCE.lastEnabledHash = buildEnabledHash();
                  }

                  ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, OVERLAY_ID, 0, 0, 0.0F, INSTANCE.fbH, (int)scaledW, (int)scaledH, INSTANCE.fbW, -INSTANCE.fbH, INSTANCE.fbW, INSTANCE.fbH);
                  if (hudOn) {
                     GuiItems.drawIcons(ctx, HudElements.icons(), INSTANCE.fbW / Math.max(1.0F, scaledW));
                  }
               } catch (Throwable t) {
                  INSTANCE.disabled = true;
               }
            }
         }
      }
   }

   public static void dispose() {
      BackdropBlur.dispose();
      HudFx.dispose();
      HudFx.onContextDestroyed();
      LiquidGlass.dispose();
      LiquidGlass.onContextDestroyed();
      HudElements.onContextDestroyed();
      if (INSTANCE.vg != 0L) {
         if (INSTANCE.fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(INSTANCE.vg, INSTANCE.fb);
            INSTANCE.fb = null;
         }

         ClarityFonts.free();
         ClarityImages.clear();
         NanoVGGL3.nvgDelete(INSTANCE.vg);
         INSTANCE.vg = 0L;
      }

      INSTANCE.fbValid = false;
   }

   private boolean hasStateChanged(ArrayList module, float scaledW, float scaledH) {
      if ((int)scaledW == this.fbW && (int)scaledH == this.fbH) {
         int hash = buildEnabledHash();
         return hash != this.lastEnabledHash;
      } else {
         return true;
      }
   }

   private static int buildEnabledHash() {
      List<ModuleBase> all = ModuleRegistry.all();
      int h = 0;
      int idx = 0;

      for (int i = 0; i < all.size(); i++) {
         ModuleBase m = all.get(i);
         if (m.isEnabled() && !(m instanceof ArrayList)) {
            h ^= m.getName().hashCode() * 31 + idx;
            String mode = ArrayListModes.modeOf(m.getName());
            if (mode != null) {
               h ^= mode.hashCode() * 17 + idx * 7;
            }

            idx++;
         }
      }

      return h ^ ClarityConfig.valuesHash("ArrayList") * 131;
   }

   private boolean checkAnimationsSettled(ArrayList module) {
      return ArrayList.areAnimationsSettled();
   }

   private void ensureContext() {
      if (this.vg == 0L) {
         this.vg = NanoVGGL3.nvgCreate(3);
         if (this.vg == 0L) {
            throw new IllegalStateException("[clarity/ArrayList] nvgCreate failed");
         }

         ClarityFonts.loadInto(this.vg);
      }
   }

   private void ensureFramebuffer(int w, int h, Minecraft mc) {
      if (this.fb == null || this.fbW != w || this.fbH != h) {
         if (this.fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(this.vg, this.fb);
            this.fb = null;
         }

         this.fb = NanoVGGL3.nvgluCreateFramebuffer(this.vg, w, h, 0);
         if (this.fb == null) {
            throw new IllegalStateException("[clarity/ArrayList] nvgluCreateFramebuffer failed at " + w + "×" + h);
         }

         this.fbW = w;
         this.fbH = h;
         this.fbValid = false;
         int prev = GL33C.glGetInteger(36006);
         NanoVGGL3.nvgluBindFramebuffer(this.vg, this.fb);
         GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL33C.glClearStencil(0);
         GL33C.glClear(17408);
         GL33C.glBindFramebuffer(36160, prev);
         this.registerBridge(mc);
      }
   }

   private void registerBridge(Minecraft mc) {
      int glId = this.fb.texture();
      GlTexture tex = new ArrayListRenderer.BridgeGlTexture(glId, this.fbW, this.fbH);
      this.bridgeView = RenderSystem.getDevice().createTextureView(tex);
      GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      AbstractTexture at = new ArrayListRenderer.BridgeTexture(tex, this.bridgeView, sampler);
      mc.getTextureManager().register(OVERLAY_ID, at);
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId, int w, int h) {
         super(12, "clarity-arraylist-overlay", TextureFormat.RGBA8, w, h, 1, 1, glId);
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

   private static final class GlState {
      int program;
      int vao;
      int arrayBuffer;
      int uniformBuffer;
      int activeTexture;
      int texture2d;
      int framebuffer;
      int pixelUnpackBuffer;
      int pixelPackBuffer;
      int sampler0;
      int unpackAlignment;
      int unpackRowLength;
      int unpackSkipPixels;
      int unpackSkipRows;
      int vpX;
      int vpY;
      int vpW;
      int vpH;
      boolean blend;
      boolean depth;
      boolean cull;
      boolean stencil;
      boolean scissor;
      int blendSrcRgb;
      int blendDstRgb;
      int blendSrcA;
      int blendDstA;
      int blendEqRgb;
      int blendEqA;
      int cullMode;
      int frontFace;
      int stencilWriteMask;
      int stencilFunc;
      int stencilRef;
      int stencilValueMask;
      int stencilFail;
      int stencilZFail;
      int stencilZPass;
      final byte[] colorMask = new byte[4];

      static ArrayListRenderer.GlState capture() {
         ArrayListRenderer.GlState s = new ArrayListRenderer.GlState();
         s.program = GL33C.glGetInteger(35725);
         s.vao = GL33C.glGetInteger(34229);
         s.arrayBuffer = GL33C.glGetInteger(34964);
         s.uniformBuffer = GL33C.glGetInteger(35368);
         s.activeTexture = GL33C.glGetInteger(34016);
         s.texture2d = GL33C.glGetInteger(32873);
         s.framebuffer = GL33C.glGetInteger(36006);
         s.pixelUnpackBuffer = GL33C.glGetInteger(35055);
         s.pixelPackBuffer = GL33C.glGetInteger(35053);
         s.unpackAlignment = GL33C.glGetInteger(3317);
         s.unpackRowLength = GL33C.glGetInteger(3314);
         s.unpackSkipPixels = GL33C.glGetInteger(3316);
         s.unpackSkipRows = GL33C.glGetInteger(3315);
         GL33C.glActiveTexture(33984);
         s.sampler0 = GL33C.glGetInteger(35097);
         GL33C.glActiveTexture(s.activeTexture);

         try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            GL33C.glGetIntegerv(2978, vp);
            s.vpX = vp.get(0);
            s.vpY = vp.get(1);
            s.vpW = vp.get(2);
            s.vpH = vp.get(3);
         }

         s.blend = GL33C.glIsEnabled(3042);
         s.depth = GL33C.glIsEnabled(2929);
         s.cull = GL33C.glIsEnabled(2884);
         s.stencil = GL33C.glIsEnabled(2960);
         s.scissor = GL33C.glIsEnabled(3089);
         s.blendSrcRgb = GL33C.glGetInteger(32969);
         s.blendDstRgb = GL33C.glGetInteger(32968);
         s.blendSrcA = GL33C.glGetInteger(32971);
         s.blendDstA = GL33C.glGetInteger(32970);
         s.blendEqRgb = GL33C.glGetInteger(32777);
         s.blendEqA = GL33C.glGetInteger(34877);
         s.cullMode = GL33C.glGetInteger(2885);
         s.frontFace = GL33C.glGetInteger(2886);
         s.stencilWriteMask = GL33C.glGetInteger(2968);
         s.stencilFunc = GL33C.glGetInteger(2962);
         s.stencilRef = GL33C.glGetInteger(2967);
         s.stencilValueMask = GL33C.glGetInteger(2963);
         s.stencilFail = GL33C.glGetInteger(2964);
         s.stencilZFail = GL33C.glGetInteger(2965);
         s.stencilZPass = GL33C.glGetInteger(2966);

         try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer cm = stack.malloc(4);
            GL33C.glGetBooleanv(3107, cm);
            cm.get(s.colorMask);
         }

         return s;
      }

      void restore() {
         GL33C.glBindFramebuffer(36160, this.framebuffer);
         GL33C.glViewport(this.vpX, this.vpY, this.vpW, this.vpH);
         GL33C.glUseProgram(this.program);
         GL33C.glBindVertexArray(this.vao);
         GL33C.glBindBuffer(34962, this.arrayBuffer);
         GL33C.glBindBuffer(35345, this.uniformBuffer);
         GL33C.glActiveTexture(this.activeTexture);
         GL33C.glBindTexture(3553, this.texture2d);
         GL33C.glBindBuffer(35052, this.pixelUnpackBuffer);
         GL33C.glBindBuffer(35051, this.pixelPackBuffer);
         GL33C.glPixelStorei(3317, this.unpackAlignment);
         GL33C.glPixelStorei(3314, this.unpackRowLength);
         GL33C.glPixelStorei(3316, this.unpackSkipPixels);
         GL33C.glPixelStorei(3315, this.unpackSkipRows);
         GL33C.glBindSampler(0, this.sampler0);
         toggle(3042, this.blend);
         toggle(2929, this.depth);
         toggle(2884, this.cull);
         toggle(2960, this.stencil);
         toggle(3089, this.scissor);
         GL33C.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcA, this.blendDstA);
         GL33C.glBlendEquationSeparate(this.blendEqRgb, this.blendEqA);
         GL33C.glCullFace(this.cullMode);
         GL33C.glFrontFace(this.frontFace);
         GL33C.glStencilMask(this.stencilWriteMask);
         GL33C.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
         GL33C.glStencilOp(this.stencilFail, this.stencilZFail, this.stencilZPass);
         GL33C.glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
      }

      private static void toggle(int cap, boolean on) {
         if (on) {
            GL33C.glEnable(cap);
         } else {
            GL33C.glDisable(cap);
         }
      }
   }
}
