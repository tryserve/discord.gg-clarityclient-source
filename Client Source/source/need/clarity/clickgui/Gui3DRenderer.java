package need.clarity.clickgui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.Arrays;
import need.clarity.Clarity;
import need.clarity.nvg.ClarityFonts;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.render.RawGlStateCapture;
import need.clarity.render.Shader;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;

public final class Gui3DRenderer {
   private static long vg;
   private static NvgCanvas canvas;
   private static NVGLUFramebuffer fb;
   private static int fbW;
   private static int fbH;
   private static Shader shader;
   private static int vao;
   private static int sampler;
   private static boolean failed;
   private static double ax;
   private static double ay;
   private static double az;
   private static float[] dir = new float[]{0.0F, 0.0F, -1.0F};
   private static float vis;
   private static long lastNanos;
   private static float time;
   private static final Gui3DMath.Frame frame = new Gui3DMath.Frame();
   private static float[] invVp;
   private static float canvasW = 1.0F;
   private static float canvasH = 1.0F;
   private static double mouseX = -1.0;
   private static double mouseY = -1.0;
   private static boolean haveFrame;
   private static final Matrix4f VP = new Matrix4f();
   private static final float[] VP_ARR = new float[16];
   private static ClickGui gui;
   private static boolean active;
   private static boolean aiming;
   private static final boolean[] pressed = new boolean[8];
   static boolean cursorMode;
   private static float hoverX = -1.0F;
   private static float hoverY = -1.0F;

   private Gui3DRenderer() {
   }

   public static boolean hasFailed() {
      return failed;
   }

   public static boolean active() {
      return active && gui != null;
   }

   static ClickGui gui() {
      return gui;
   }

   public static void toggle() {
      if (active()) {
         close();
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && !failed) {
            GuiData data = GuiData.get();
            gui = new ClickGui(data, new ConfigStore(data));
            Vec3 look = mc.player.getViewVector(1.0F);
            dir = new float[]{(float)look.x, (float)look.y, (float)look.z};
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            ax = cam.x;
            ay = cam.y;
            az = cam.z;
            vis = 0.0F;
            lastNanos = 0L;
            haveFrame = false;
            aiming = false;
            Arrays.fill(pressed, false);
            mouseY = -1.0;
            mouseX = -1.0;
            active = true;
         }
      }
   }

   public static void close() {
      if (gui != null && !gui.closing) {
         gui.close();
      }
   }

   static boolean wantsKeyboard() {
      return gui != null && !gui.closing && (gui.focused != null || gui.listenModule != null || gui.listenSetting != null);
   }

   static void cursor(double x, double y) {
      mouseX = x;
      mouseY = y;
   }

   private static float[] pointerUv() {
      if (haveFrame && invVp != null) {
         float nx = 0.0F;
         float ny = 0.0F;
         if (cursorMode) {
            if (mouseX < 0.0) {
               return null;
            }

            Minecraft mc = Minecraft.getInstance();
            float w = Math.max(1, mc.getWindow().getWidth());
            float h = Math.max(1, mc.getWindow().getHeight());
            nx = (float)(mouseX / w * 2.0 - 1.0);
            ny = (float)(1.0 - mouseY / h * 2.0);
         }

         return Gui3DMath.pick(invVp, nx, ny, frame);
      } else {
         return null;
      }
   }

   static float[] toGui(boolean unbounded) {
      float[] uv = pointerUv();
      return uv != null && (unbounded || !(Math.abs(uv[0]) > 1.0F) && !(Math.abs(uv[1]) > 1.0F))
         ? new float[]{(uv[0] * 0.5F + 0.5F) * canvasW, (0.5F - uv[1] * 0.5F) * canvasH}
         : new float[]{-10000.0F, -10000.0F};
   }

   static float[] toGui() {
      return toGui(false);
   }

   public static boolean onMouseButton(int button, int action) {
      if (!active() || button < 0 || button >= pressed.length) {
         return false;
      }

      if (action == 1) {
         if (aiming && !gui.closing) {
            float[] g = toGui(false);
            gui.mouseMove(g[0], g[1]);
            gui.mouseDown(g[0], g[1], button);
            pressed[button] = true;
            return true;
         } else {
            return false;
         }
      } else if (action == 0 && pressed[button]) {
         pressed[button] = false;
         float[] g = toGui(true);
         gui.mouseUp(g[0], g[1], button);
         return true;
      } else {
         return false;
      }
   }

   public static boolean onScroll(double dy) {
      if (active() && aiming && dy != 0.0) {
         float[] g = toGui(false);
         gui.scroll(g[0], g[1], (float)dy);
         return true;
      } else {
         return false;
      }
   }

   static boolean closedOut(ClickGui gui) {
      return gui.closing && vis <= 0.01F;
   }

   public static void renderWorld() {
      if (!failed && active()) {
         ClickGui gui = Gui3DRenderer.gui;
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.getMainRenderTarget();
         if (main != null && main.width > 0 && main.height > 0) {
            if (mc.player != null && mc.level != null) {
               try {
                  long now = System.nanoTime();
                  float dt = lastNanos == 0L ? 0.016F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
                  lastNanos = now;
                  time += dt;
                  float target = gui.closing ? 0.0F : 1.0F;
                  vis = vis + (target - vis) * (1.0F - (float)Math.exp(-dt * (gui.closing ? 9.0F : 7.0F)));
                  if (Math.abs(vis - target) < 0.002F) {
                     vis = target;
                  }

                  if (!SkyShaderRenderer.levelViewProjection(VP)) {
                     return;
                  }

                  VP.get(VP_ARR);
                  invVp = Gui3DMath.invert(VP_ARR);
                  Vec3 cam = ClarityRenderUtils.getCameraPos();
                  if (gui.flag3D("3D Follow", false)) {
                     float k = 1.0F - (float)Math.exp(-dt * 4.0F);
                     ax = ax + (cam.x - ax) * k;
                     ay = ay + (cam.y - ay) * k;
                     az = az + (cam.z - az) * k;
                     Vec3 look = mc.player.getViewVector(1.0F);

                     for (int i = 0; i < 3; i++) {
                        dir[i] = dir[i] + ((float)(i == 0 ? look.x : (i == 1 ? look.y : look.z)) - dir[i]) * k * 0.5F;
                     }
                  }

                  float ds = ClickGuiRenderer.designScale(gui);
                  float[] cv = gui.canvas3D(main.width / ds, main.height / ds);
                  canvasW = cv[0];
                  canvasH = cv[1];
                  float px = Math.min(2.0F, 2560.0F / Math.max(canvasW, canvasH));
                  int w = Math.max(64, Math.round(canvasW * px));
                  int h = Math.max(64, Math.round(canvasH * px));
                  float dist = gui.setting3D("3D Distance", 2.2);
                  float halfH = gui.setting3D("3D Size", 1.9) * 0.5F;
                  float e = ease(vis);
                  boolean fl = gui.flag3D("3D Float", true);
                  float bob = fl ? (float)Math.sin(time * 1.1) * 0.012F : 0.0F;
                  float swing = fl ? (float)Math.sin(time * 0.7) * 0.6F : 0.0F;
                  Gui3DMath.place(frame, dir, dist, halfH * (0.9F + 0.1F * e), canvasW / canvasH, gui.setting3D("3D Tilt", 6.0), swing + (1.0F - e) * 14.0F, bob, (1.0F - e) * 0.45F);
                  float rx = (float)(ax - cam.x);
                  float ry = (float)(ay - cam.y);
                  float rz = (float)(az - cam.z);
                  frame.c[0] = frame.c[0] + rx;
                  frame.c[1] = frame.c[1] + ry;
                  frame.c[2] = frame.c[2] + rz;
                  haveFrame = true;
                  boolean dragging = false;

                  for (boolean b : pressed) {
                     dragging |= b;
                  }

                  float[] uv = pointerUv();
                  aiming = uv != null && Math.abs(uv[0]) <= 1.0F && Math.abs(uv[1]) <= 1.0F && vis > 0.5F;
                  float[] g = toGui(dragging);
                  gui.mouseMove(g[0], g[1]);
                  hoverX = aiming ? g[0] : -1.0F;
                  hoverY = aiming ? g[1] : -1.0F;

                  try (
                     RawGlStateCapture ignored = RawGlStateCapture.capture();
                     RawGlState ignored2 = RawGlState.capture();
                  ) {
                     drawMenu(mc, gui, w, h, px);
                     drawPanel(main, gui, e);
                  }

                  if (wantsKeyboard() && mc.screen == null) {
                     mc.execute(() -> {
                        if (mc.screen == null && wantsKeyboard()) {
                           mc.setScreen(new Gui3DScreen());
                        }
                     });
                  }

                  if (closedOut(gui)) {
                     active = false;
                     Gui3DRenderer.gui = null;
                     cursorMode = false;
                     if (mc.screen instanceof Gui3DScreen) {
                        mc.execute(() -> mc.setScreen(null));
                     }
                  }
               } catch (Throwable t) {
                  failed = true;
                  active = false;
                  Gui3DRenderer.gui = null;
                  Clarity.LOGGER.warn("[Clarity] 3D GUI failed to draw", t);
               }
            } else {
               active = false;
               Gui3DRenderer.gui = null;
            }
         }
      }
   }

   private static float ease(float t) {
      return 1.0F - (float)Math.pow(1.0F - Math.max(0.0F, Math.min(1.0F, t)), 3.0);
   }

   private static void drawMenu(Minecraft mc, ClickGui gui, int w, int h, float px) {
      ensure(w, h);
      if (gui.snapshotRequest() != 0) {
         gui.snapshotTaken(false);
      }

      NanoVGGL3.nvgluBindFramebuffer(vg, fb);
      GL33C.glViewport(0, 0, fbW, fbH);
      GL33C.glDisable(3089);
      GL33C.glDisable(2929);
      GL33C.glDisable(2884);
      GL33C.glColorMask(true, true, true, true);
      GL33C.glStencilMask(255);
      GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL33C.glClearStencil(0);
      GL33C.glClear(17408);
      GL33C.glBindBuffer(35052, 0);
      GL33C.glPixelStorei(3317, 1);
      GL33C.glPixelStorei(3314, 0);
      GL33C.glPixelStorei(3316, 0);
      GL33C.glPixelStorei(3315, 0);

      for (int u = 0; u < 4; u++) {
         GL33C.glBindSampler(u, 0);
      }

      GL33C.glActiveTexture(33984);
      canvas.setGlassSource(0, 0, 0);
      canvas.setCrisp(false);
      NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
      canvas.begin(fbW, fbH);
      NanoVG.nvgSave(vg);
      NanoVG.nvgScale(vg, px, px);
      gui.render(canvas, canvasW, canvasH, System.nanoTime());
      if (hoverX >= 0.0F && !cursorMode) {
         int acc = gui.theme().accent;
         canvas.circle(hoverX, hoverY, 3.2F, Theme.a(-16777216, 0.35F));
         canvas.ring(hoverX, hoverY, 4.2F, 1.6F, Theme.a(acc, 0.9F));
         canvas.circle(hoverX, hoverY, 1.4F, -1);
      }

      NanoVG.nvgRestore(vg);
      NanoVG.nvgEndFrame(vg);
      GL33C.glBindTexture(3553, fb.texture());
      GL33C.glGenerateMipmap(3553);
   }

   private static void drawPanel(RenderTarget main, ClickGui gui, float e) {
      if (shader == null) {
         shader = new Shader("clickgui", "gui3d");
      }

      if (vao == 0) {
         vao = GL33C.glGenVertexArrays();
      }

      if (sampler == 0) {
         sampler = GL33C.glGenSamplers();
         GL33C.glSamplerParameteri(sampler, 10241, 9987);
         GL33C.glSamplerParameteri(sampler, 10240, 9729);
         GL33C.glSamplerParameteri(sampler, 10242, 33071);
         GL33C.glSamplerParameteri(sampler, 10243, 33071);
      }

      if (RawFramebufferUtil.bindDirect(main)) {
         GL33C.glViewport(0, 0, main.width, main.height);
         GL33C.glDisable(3089);
         GL33C.glDisable(2884);
         GL33C.glDisable(2960);
         GL33C.glDepthMask(false);
         if (gui.flag3D("3D Always On Top", true)) {
            GL33C.glDisable(2929);
         } else {
            GL33C.glEnable(2929);
            GL33C.glDepthFunc(515);
         }

         GL33C.glEnable(3042);
         GL33C.glBlendFuncSeparate(1, 771, 1, 771);
         GL33C.glBlendEquation(32774);
         GL33C.glColorMask(true, true, true, true);
         shader.bind();
         float[] mat = new float[16];
         shader.setUniformMatrix4f("uViewProj", VP.get(mat));
         shader.setUniform3f("uCenter", frame.c[0], frame.c[1], frame.c[2]);
         shader.setUniform3f("uRight", frame.r[0], frame.r[1], frame.r[2]);
         shader.setUniform3f("uUp", frame.u[0], frame.u[1], frame.u[2]);
         shader.setUniform1i("uTex", 0);
         shader.setUniform1f("uAlpha", Math.min(1.0F, e * 1.15F));
         boolean shadow = gui.flag3D("3D Shadow", true);
         shader.setUniform2f("uShadowOff", 0.004F, 0.022F);
         shader.setUniform1f("uShadowRad", 0.012F);
         shader.setUniform1f("uShadowA", shadow ? 0.55F : 0.0F);
         shader.setUniform1f("uAspect", canvasW / canvasH);
         GL33C.glActiveTexture(33984);
         GL33C.glBindTexture(3553, fb.texture());
         GL33C.glBindSampler(0, sampler);
         GL33C.glBindVertexArray(vao);
         GL33C.glDrawArrays(5, 0, 4);
         GL33C.glBindVertexArray(0);
         GL33C.glBindSampler(0, 0);
         shader.unbind();
         GL33C.glDepthMask(true);
      }
   }

   private static void ensure(int w, int h) {
      if (vg == 0L) {
         vg = NanoVGGL3.nvgCreate(3);
         if (vg == 0L) {
            throw new IllegalStateException("nvgCreate failed");
         }

         ClarityFonts.loadInto(vg);
         canvas = new NvgCanvas(vg);
      }

      if (fb == null || fbW != w || fbH != h) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         canvas.forgetImages();
         fb = NanoVGGL3.nvgluCreateFramebuffer(vg, w, h, 0);
         if (fb == null) {
            throw new IllegalStateException("nvgluCreateFramebuffer failed");
         }

         fbW = w;
         fbH = h;
      }
   }

   public static void dispose() {
      if (shader != null) {
         shader.delete();
         shader = null;
      }

      if (vao != 0) {
         GL33C.glDeleteVertexArrays(vao);
         vao = 0;
      }

      if (sampler != 0) {
         GL33C.glDeleteSamplers(sampler);
         sampler = 0;
      }

      if (vg != 0L) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         NanoVGGL3.nvgDelete(vg);
         vg = 0L;
      }
   }
}
