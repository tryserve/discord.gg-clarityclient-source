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
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class ShaderCapes {
   public static final String[] IDS = new String[]{"clarity_aurora", "clarity_nebula", "clarity_flow", "clarity_circuit", "clarity_prism"};
   public static final Identifier TEX_ID = Identifier.fromNamespaceAndPath("clarity", "capes/shader_cape");
   private static final int W = 512;
   private static final int H = 256;
   private static volatile int style = -1;
   private static int program = 0;
   private static int vao = 0;
   private static int vbo = 0;
   private static int tex = 0;
   private static int fbo = 0;
   private static int logoTex = 0;
   private static int maskTex = 0;
   private static int locRes;
   private static int locTime;
   private static int locStyle;
   private static int locA;
   private static int locB;
   private static int locLogo;
   private static int locMask;
   private static int locHasMask;
   private static boolean failed = false;
   private static boolean registered = false;
   private static long lastDraw = 0L;
   private static final long START = System.nanoTime();

   private ShaderCapes() {
   }

   public static boolean isShaderCape(String id) {
      return styleOf(id) >= 0;
   }

   public static int styleOf(String id) {
      for (int i = 0; i < IDS.length; i++) {
         if (IDS[i].equals(id)) {
            return i;
         }
      }

      return -1;
   }

   public static Identifier select(String id) {
      style = styleOf(id);
      return style >= 0 && !failed ? TEX_ID : null;
   }

   public static void frame() {
      if (style >= 0 && !failed) {
         long now = System.nanoTime();
         if (!registered || now - lastDraw >= 16000000L) {
            lastDraw = now;

            try {
               draw(now);
            } catch (Throwable t) {
               failed = true;
            }
         }
      }
   }

   private static void draw(long now) {
      RenderSystem.assertOnRenderThread();

      try (RawGlStateCapture ignored = RawGlStateCapture.capture()) {
         if (!ensure()) {
            return;
         }

         GL30.glBindFramebuffer(36160, fbo);
         GL11.glDrawBuffer(36064);
         GlStateManager._viewport(0, 0, 512, 256);
         GlStateManager._disableScissorTest();
         GlStateManager._disableDepthTest();
         GlStateManager._disableBlend();
         GlStateManager._disableCull();
         GL11.glColorMask(true, true, true, true);
         GlStateManager._activeTexture(33984);
         int prevSampler = GL11.glGetInteger(35097);
         GL33.glBindSampler(0, 0);
         GlStateManager._bindTexture(logoTex);
         GlStateManager._activeTexture(33985);
         int prevSampler1 = GL11.glGetInteger(35097);
         GL33.glBindSampler(1, 0);
         GlStateManager._bindTexture(maskTex);
         GlStateManager._activeTexture(33984);
         GL20.glUseProgram(program);
         int a = ThemeColors.getAccentRgb();
         int b = ThemeColors.getAccent2Rgb();
         GL20.glUniform2f(locRes, 512.0F, 256.0F);
         GL20.glUniform1f(locTime, (float)((now - START) / 1.0E9 % 3600.0));
         GL20.glUniform1i(locStyle, style);
         GL20.glUniform3f(locA, (a >> 16 & 0xFF) / 255.0F, (a >> 8 & 0xFF) / 255.0F, (a & 0xFF) / 255.0F);
         GL20.glUniform3f(locB, (b >> 16 & 0xFF) / 255.0F, (b >> 8 & 0xFF) / 255.0F, (b & 0xFF) / 255.0F);
         GL20.glUniform1i(locLogo, 0);
         GL20.glUniform1i(locMask, 1);
         GL20.glUniform1i(locHasMask, maskTex != 0 ? 1 : 0);
         GL30.glBindVertexArray(vao);
         GL11.glDrawArrays(4, 0, 3);
         GL30.glBindVertexArray(0);
         GL33.glBindSampler(0, prevSampler);
         GL33.glBindSampler(1, prevSampler1);
      }
   }

   private static boolean ensure() {
      if (program != 0 && registered) {
         return true;
      }

      if (program == 0) {
         program = link(read("shaders/cape/cape.vsh"), read("shaders/cape/cape.fsh"));
         locRes = GL20.glGetUniformLocation(program, "uRes");
         locTime = GL20.glGetUniformLocation(program, "uTime");
         locStyle = GL20.glGetUniformLocation(program, "uStyle");
         locA = GL20.glGetUniformLocation(program, "uA");
         locB = GL20.glGetUniformLocation(program, "uB");
         locLogo = GL20.glGetUniformLocation(program, "uLogo");
         locMask = GL20.glGetUniformLocation(program, "uMask");
         locHasMask = GL20.glGetUniformLocation(program, "uHasMask");
         vao = GL30.glGenVertexArrays();
         vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(vao);
         GL15.glBindBuffer(34962, vbo);
         GL15.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
         GL30.glBindVertexArray(0);
         logoTex = loadLogo();
         maskTex = loadElytraMask();
      }

      tex = GL11.glGenTextures();
      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(tex);
      GL11.glTexParameteri(3553, 33085, 0);
      int pbo = GL11.glGetInteger(35055);
      GL15.glBindBuffer(35052, 0);
      GL11.glTexImage2D(3553, 0, 32856, 512, 256, 0, 6408, 5121, (ByteBuffer)null);
      GL15.glBindBuffer(35052, pbo);
      fbo = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, fbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, tex, 0);
      if (GL30.glCheckFramebufferStatus(36160) != 36053) {
         failed = true;
         return false;
      } else {
         GlTexture gt = new ShaderCapes.BridgeGlTexture(tex);
         GpuTextureView view = RenderSystem.getDevice().createTextureView(gt);
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
         Minecraft.getInstance().getTextureManager().register(TEX_ID, new ShaderCapes.BridgeTexture(gt, view, sampler));
         registered = true;
         return true;
      }
   }

   private static int loadElytraMask() {
      for (String path : new String[]{"textures/entity/equipment/wings/elytra.png", "textures/entity/elytra.png"}) {
         try (InputStream in = Minecraft.getInstance().getResourceManager().open(Identifier.parse("minecraft:" + path))) {
            int id = upload(in.readAllBytes(), 9728);
            if (id != 0) {
               return id;
            }
         } catch (Throwable var9) {
         }
      }

      return 0;
   }

   private static int loadLogo() {
      byte[] png;
      try (InputStream in = ShaderCapes.class.getResourceAsStream("/assets/clarity/textures/fx/clarity_logo.png")) {
         if (in == null) {
            return 0;
         }

         png = in.readAllBytes();
      } catch (Exception e) {
         return 0;
      }

      return upload(png, 9729);
   }

   private static int upload(byte[] png, int filter) {
      ByteBuffer src = MemoryUtil.memAlloc(png.length);
      src.put(png).flip();

      try (MemoryStack st = MemoryStack.stackPush()) {
         IntBuffer w = st.mallocInt(1);
         IntBuffer h = st.mallocInt(1);
         IntBuffer c = st.mallocInt(1);
         ByteBuffer px = STBImage.stbi_load_from_memory(src, w, h, c, 4);
         if (px == null) {
            return 0;
         }

         try {
            int id = GL11.glGenTextures();
            GlStateManager._activeTexture(33984);
            GlStateManager._bindTexture(id);
            GL11.glTexParameteri(3553, 10241, filter);
            GL11.glTexParameteri(3553, 10240, filter);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexParameteri(3553, 33085, 0);
            int pbo = GL11.glGetInteger(35055);
            GL15.glBindBuffer(35052, 0);
            int align = GL11.glGetInteger(3317);
            int row = GL11.glGetInteger(3314);
            GL11.glPixelStorei(3317, 4);
            GL11.glPixelStorei(3314, 0);
            GL11.glTexImage2D(3553, 0, 32856, w.get(0), h.get(0), 0, 6408, 5121, px);
            GL11.glPixelStorei(3317, align);
            GL11.glPixelStorei(3314, row);
            GL15.glBindBuffer(35052, pbo);
            return id;
         } finally {
            STBImage.stbi_image_free(px);
         }
      } finally {
         MemoryUtil.memFree(src);
      }
   }

   private static int link(String vsh, String fsh) {
      int vs = compile(35633, vsh);
      int fs = compile(35632, fsh);
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
         throw new RuntimeException("cape shader link failed");
      } else {
         return p;
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

   private static String read(String path) {
      try (InputStream in = ShaderCapes.class.getResourceAsStream("/assets/clarity/" + path)) {
         if (in == null) {
            throw new RuntimeException("missing " + path);
         } else {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId) {
         super(12, "clarity-shader-cape", TextureFormat.RGBA8, 512, 256, 1, 1, glId);
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
