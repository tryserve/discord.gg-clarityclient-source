package need.clarity.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import need.clarity.modules.visual.CustomCrosshair;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class CrosshairRenderer {
   private static final Identifier TEX_ID = Identifier.fromNamespaceAndPath("clarity", "crosshair_overlay");
   private static int program = 0;
   private static int vao = 0;
   private static int vbo = 0;
   private static int tex = 0;
   private static int fbo = 0;
   private static int size = 0;
   private static boolean failed = false;
   private static final Map<String, Integer> loc = new HashMap<>();
   private static float spread = 0.0F;
   private static float target = 0.0F;
   private static float spinAngle = 0.0F;
   private static long lastNanos = 0L;
   private static final long START = System.nanoTime();

   private CrosshairRenderer() {
   }

   public static boolean render(GuiGraphicsExtractor ctx) {
      CustomCrosshair cc = CustomCrosshair.INSTANCE;
      if (cc != null && cc.isEnabled() && !failed) {
         try {
            return draw(ctx, cc);
         } catch (Throwable t) {
            failed = true;
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean draw(GuiGraphicsExtractor ctx, CustomCrosshair cc) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer pl = mc.player;
      if (pl == null) {
         return true;
      }

      if (mc.options.getCameraType().isFirstPerson() && !pl.isSpectator()) {
         long now = System.nanoTime();
         float dt = lastNanos == 0L ? 0.016F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
         lastNanos = now;
         int physW = mc.getWindow().getWidth();
         int physH = mc.getWindow().getHeight();
         double scale = mc.getWindow().getGuiScale();
         float k = Math.max(0.75F, physH / 1080.0F);
         float targetSpread = 0.0F;
         if (cc.dynamic()) {
            double sp = Math.hypot(pl.getDeltaMovement().x, pl.getDeltaMovement().z) * 20.0;
            targetSpread = (float)Math.min(4.0, sp * 0.45) + (pl.onGround() ? 0.0F : 2.5F);
         }

         spread = spread + (targetSpread - spread) * Math.min(1.0F, dt * 12.0F);
         boolean onEntity = mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY;
         target = target + ((cc.targetTint() && onEntity ? 1.0F : 0.0F) - target) * Math.min(1.0F, dt * 14.0F);
         spinAngle = (spinAngle + cc.spinSpeed() * 90.0F * dt) % 360.0F;
         float cooldown = cc.cooldown() ? pl.getAttackStrengthScale(0.0F) : 1.0F;
         float hitAge = cc.hitAge(now);
         float killAge = cc.killAge(now);
         float reach = (cc.gap() + cc.size() + 22.0F + cc.glow() * 8.0F + 4.0F) * k;
         int gs = (int)Math.ceil(2.0F * reach / scale);
         int S = (int)Math.round(gs * scale);
         if (!ensure(S)) {
            return false;
         }

         try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
            GL30.glBindFramebuffer(36160, fbo);
            GL11.glDrawBuffer(36064);
            GlStateManager._viewport(0, 0, S, S);
            GlStateManager._disableScissorTest();
            GlStateManager._disableDepthTest();
            GlStateManager._disableBlend();
            GlStateManager._disableCull();
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClear(16384);
            GL20.glUseProgram(program);
            int guiW = mc.getWindow().getGuiScaledWidth();
            int guiH = mc.getWindow().getGuiScaledHeight();
            int gx = (guiW - gs) / 2;
            int gy = (guiH - gs) / 2;
            float ox = (float)(physW / 2.0 - (gx * scale + S / 2.0));
            float oy = (float)(-(physH / 2.0 - (gy * scale + S / 2.0)));
            u2("uRes", S, S);
            u2("uOffset", ox, oy);
            u1("uK", k);
            u1i("uStyle", styleIndex(cc.style()));
            u1("uLen", cc.size());
            u1("uGap", cc.gap());
            u1("uThick", cc.thickness());
            u1("uDot", cc.dot());
            u1("uRot", cc.rotation() + spinAngle);
            u1("uOutline", cc.outline() ? cc.outlineWidth() : 0.0F);
            u1("uGlow", cc.glow());
            u1("uTime", (float)((now - START) / 1.0E9 % 3600.0));
            u1("uSpread", spread);

            u1i("uColorMode", switch (cc.colorMode()) {
               case "Gradient" -> 1;
               case "Chroma" -> 2;
               default -> 0;
            });
            argb("uCol1", ThemeColors.resolveArgb(cc.color(), -1, false));
            argb("uCol2", ThemeColors.resolveArgb(cc.color2(), -8745729, true));
            GL20.glUniform3f(loc("uOutCol"), 0.02F, 0.02F, 0.03F);
            rgb("uHitCol", cc.hitColor());
            rgb("uTargetCol", cc.targetColor());
            if (ThemeColors.isThemeValue(cc.hitColor())) {
               argb("uHitCol", ThemeColors.resolveArgb(cc.hitColor(), -50373, false));
            }

            if (ThemeColors.isThemeValue(cc.targetColor())) {
               argb("uTargetCol", ThemeColors.resolveArgb(cc.targetColor(), -44976, true));
            }

            u1("uTarget", target);
            u1("uHitAge", hitAge);
            u1("uKillAge", killAge);
            u1i("uHitmarker", cc.hitmarker() ? 1 : 0);
            u1i("uExpand", cc.expand() ? 1 : 0);
            u1i("uFlash", cc.flash() ? 1 : 0);
            u1i("uShock", cc.shockwave() ? 1 : 0);
            u1("uCooldown", cooldown);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(4, 0, 3);
            GL30.glBindVertexArray(0);
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, TEX_ID, gx, gy, 0.0F, S, gs, gs, S, -S, S, S);
         }

         return true;
      } else {
         return true;
      }
   }

   private static int styleIndex(String s) {
      return switch (s) {
         case "Dot" -> 1;
         case "Circle" -> 2;
         case "Cross + Dot" -> 3;
         case "Segments" -> 4;
         case "Chevron" -> 5;
         case "Diamond" -> 6;
         case "Brackets" -> 7;
         default -> 0;
      };
   }

   private static int loc(String n) {
      return loc.computeIfAbsent(n, x -> GL20.glGetUniformLocation(program, x));
   }

   private static void u1(String n, float v) {
      GL20.glUniform1f(loc(n), v);
   }

   private static void u1i(String n, int v) {
      GL20.glUniform1i(loc(n), v);
   }

   private static void u2(String n, float a, float b) {
      GL20.glUniform2f(loc(n), a, b);
   }

   private static void argb(String n, int c) {
      GL20.glUniform3f(loc(n), (c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
   }

   private static void rgb(String n, String hex) {
      int c = 16777215;

      try {
         String h = hex.startsWith("#") ? hex.substring(1) : hex;
         c = (int)Long.parseLong(h.length() > 6 ? h.substring(h.length() - 6) : h, 16);
      } catch (Throwable var4) {
      }

      GL20.glUniform3f(loc(n), (c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
   }

   private static boolean ensure(int S) {
      if (program == 0 && !build()) {
         return false;
      }

      if (tex != 0 && size == S) {
         return true;
      }

      if (tex == 0) {
         tex = GL11.glGenTextures();
      }

      GlStateManager._activeTexture(33984);
      int prev = GL11.glGetInteger(32873);
      GlStateManager._bindTexture(tex);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 33085, 0);
      int pbo = GL11.glGetInteger(35055);
      GL15.glBindBuffer(35052, 0);
      GL11.glTexImage2D(3553, 0, 32856, S, S, 0, 6408, 5121, (ByteBuffer)null);
      GL15.glBindBuffer(35052, pbo);
      GlStateManager._bindTexture(prev);
      if (fbo == 0) {
         fbo = GL30.glGenFramebuffers();
      }

      int prevFbo = GL11.glGetInteger(36006);
      GL30.glBindFramebuffer(36160, fbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, tex, 0);
      boolean ok = GL30.glCheckFramebufferStatus(36160) == 36053;
      GL30.glBindFramebuffer(36160, prevFbo);
      if (!ok) {
         return false;
      }

      size = S;
      GlTexture gt = new CrosshairRenderer.BridgeGlTexture(tex, S);
      GpuTextureView view = RenderSystem.getDevice().createTextureView(gt);
      GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      Minecraft.getInstance().getTextureManager().register(TEX_ID, new CrosshairRenderer.BridgeTexture(gt, view, sampler));
      return true;
   }

   private static boolean build() {
      try {
         int vs = compile(35633, read("crosshair.vsh"));
         int fs = compile(35632, read("crosshair.fsh"));
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
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            int prevVao = GL11.glGetInteger(34229);
            int prevBuf = GL11.glGetInteger(34964);
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(34962, vbo);
            GL15.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
            GL30.glBindVertexArray(prevVao);
            GL15.glBindBuffer(34962, prevBuf);
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
      try (InputStream in = CrosshairRenderer.class.getResourceAsStream("/assets/clarity/shaders/crosshair/" + name)) {
         if (in == null) {
            throw new IOException("missing " + name);
         } else {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
         }
      }
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId, int s) {
         super(12, "clarity-crosshair", TextureFormat.RGBA8, s, s, 1, 1, glId);
      }

      public void close() {
      }
   }

   private static final class BridgeTexture extends AbstractTexture {
      BridgeTexture(GpuTexture t, GpuTextureView v, GpuSampler s) {
         this.texture = t;
         this.textureView = v;
         this.sampler = s;
      }

      public void close() {
      }
   }
}
