package need.clarity.nvg;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL33C;

public final class BackdropBlur {
   private static final String VERT = "#version 330 core\nout vec2 uv;\nvoid main() {\n    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n    uv = p;\n    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n";
   private static final String FRAG_DOWN = "#version 330 core\nuniform sampler2D uTex;\nuniform vec2 uTexel;\nin vec2 uv;\nout vec4 fragColor;\nvoid main() {\n    vec3 c = texture(uTex, uv + uTexel * vec2(-1.0, -1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2( 1.0, -1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2(-1.0,  1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2( 1.0,  1.0)).rgb;\n    fragColor = vec4(c * 0.25, 1.0);\n}\n";
   private static final String FRAG_BLUR = "#version 330 core\nuniform sampler2D uTex;\nuniform vec2 uDir;\nin vec2 uv;\nout vec4 fragColor;\nvoid main() {\n    const float w0 = 0.2270270270;\n    const float w1 = 0.1945945946;\n    const float w2 = 0.1216216216;\n    const float w3 = 0.0540540541;\n    const float w4 = 0.0162162162;\n    vec3 c = texture(uTex, uv).rgb * w0;\n    c += (texture(uTex, uv + uDir)       .rgb + texture(uTex, uv - uDir)       .rgb) * w1;\n    c += (texture(uTex, uv + uDir * 2.0) .rgb + texture(uTex, uv - uDir * 2.0) .rgb) * w2;\n    c += (texture(uTex, uv + uDir * 3.0) .rgb + texture(uTex, uv - uDir * 3.0) .rgb) * w3;\n    c += (texture(uTex, uv + uDir * 4.0) .rgb + texture(uTex, uv - uDir * 4.0) .rgb) * w4;\n    fragColor = vec4(c, 1.0);\n}\n";
   private static final int DOWNSCALE = 4;
   private static boolean failed;
   private static int progDown;
   private static int progBlur;
   private static int vao;
   private static int sampler;
   private static int uDownTex;
   private static int uDownTexel;
   private static int uBlurTex;
   private static int uBlurDir;
   private static final int[] fbo = new int[2];
   private static final int[] tex = new int[2];
   private static int bw;
   private static int bh;
   private static int resultTex;

   private BackdropBlur() {
   }

   public static int textureId() {
      return resultTex;
   }

   public static int width() {
      return bw;
   }

   public static int height() {
      return bh;
   }

   public static void invalidate() {
      resultTex = 0;
   }

   public static int capture(int srcColorTex, int srcW, int srcH, int strength) {
      resultTex = 0;
      if (!failed && srcColorTex != 0 && srcW > 0 && srcH > 0) {
         try {
            if (!init()) {
               return 0;
            }

            if (!ensureTargets(Math.max(1, srcW / 4), Math.max(1, srcH / 4))) {
               return 0;
            }

            GL33C.glActiveTexture(33984);
            int prevTex = GL33C.glGetInteger(32873);
            GL33C.glDisable(3042);
            GL33C.glDisable(2929);
            GL33C.glDisable(2884);
            GL33C.glDisable(3089);
            GL33C.glDisable(2960);
            GL33C.glColorMask(true, true, true, true);
            GL33C.glBindVertexArray(vao);
            GL33C.glBindSampler(0, sampler);
            GL33C.glBindFramebuffer(36160, fbo[0]);
            GL33C.glViewport(0, 0, bw, bh);
            GL33C.glUseProgram(progDown);
            GL33C.glUniform1i(uDownTex, 0);
            GL33C.glUniform2f(uDownTexel, 1.0F / srcW, 1.0F / srcH);
            GL33C.glBindTexture(3553, srcColorTex);
            GL33C.glDrawArrays(4, 0, 3);
            int iters = Math.max(1, Math.min(4, strength));
            GL33C.glUseProgram(progBlur);
            GL33C.glUniform1i(uBlurTex, 0);

            for (int i = 0; i < iters; i++) {
               float radius = 1.0F + i * 0.9F;
               GL33C.glBindFramebuffer(36160, fbo[1]);
               GL33C.glBindTexture(3553, tex[0]);
               GL33C.glUniform2f(uBlurDir, radius / bw, 0.0F);
               GL33C.glDrawArrays(4, 0, 3);
               GL33C.glBindFramebuffer(36160, fbo[0]);
               GL33C.glBindTexture(3553, tex[1]);
               GL33C.glUniform2f(uBlurDir, 0.0F, radius / bh);
               GL33C.glDrawArrays(4, 0, 3);
            }

            GL33C.glBindTexture(3553, prevTex);
            GL33C.glBindSampler(0, 0);
            resultTex = tex[0];
            return resultTex;
         } catch (Throwable t) {
            failed = true;
            resultTex = 0;
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static void dispose() {
      resultTex = 0;

      try {
         for (int i = 0; i < 2; i++) {
            if (fbo[i] != 0) {
               GL33C.glDeleteFramebuffers(fbo[i]);
            }

            if (tex[i] != 0) {
               GL33C.glDeleteTextures(tex[i]);
            }

            fbo[i] = 0;
            tex[i] = 0;
         }

         if (progDown != 0) {
            GL33C.glDeleteProgram(progDown);
         }

         if (progBlur != 0) {
            GL33C.glDeleteProgram(progBlur);
         }

         if (vao != 0) {
            GL33C.glDeleteVertexArrays(vao);
         }

         if (sampler != 0) {
            GL33C.glDeleteSamplers(sampler);
         }
      } catch (Throwable var1) {
      }

      sampler = 0;
      vao = 0;
      progBlur = 0;
      progDown = 0;
      bh = 0;
      bw = 0;
   }

   private static boolean init() {
      if (progDown != 0) {
         return true;
      }

      int vs = compile(
         35633, "#version 330 core\nout vec2 uv;\nvoid main() {\n    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n    uv = p;\n    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n"
      );
      int fsD = compile(
         35632,
         "#version 330 core\nuniform sampler2D uTex;\nuniform vec2 uTexel;\nin vec2 uv;\nout vec4 fragColor;\nvoid main() {\n    vec3 c = texture(uTex, uv + uTexel * vec2(-1.0, -1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2( 1.0, -1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2(-1.0,  1.0)).rgb\n           + texture(uTex, uv + uTexel * vec2( 1.0,  1.0)).rgb;\n    fragColor = vec4(c * 0.25, 1.0);\n}\n"
      );
      int fsB = compile(
         35632,
         "#version 330 core\nuniform sampler2D uTex;\nuniform vec2 uDir;\nin vec2 uv;\nout vec4 fragColor;\nvoid main() {\n    const float w0 = 0.2270270270;\n    const float w1 = 0.1945945946;\n    const float w2 = 0.1216216216;\n    const float w3 = 0.0540540541;\n    const float w4 = 0.0162162162;\n    vec3 c = texture(uTex, uv).rgb * w0;\n    c += (texture(uTex, uv + uDir)       .rgb + texture(uTex, uv - uDir)       .rgb) * w1;\n    c += (texture(uTex, uv + uDir * 2.0) .rgb + texture(uTex, uv - uDir * 2.0) .rgb) * w2;\n    c += (texture(uTex, uv + uDir * 3.0) .rgb + texture(uTex, uv - uDir * 3.0) .rgb) * w3;\n    c += (texture(uTex, uv + uDir * 4.0) .rgb + texture(uTex, uv - uDir * 4.0) .rgb) * w4;\n    fragColor = vec4(c, 1.0);\n}\n"
      );
      if (vs != 0 && fsD != 0 && fsB != 0) {
         progDown = link(vs, fsD);
         progBlur = link(vs, fsB);
         GL33C.glDeleteShader(vs);
         GL33C.glDeleteShader(fsD);
         GL33C.glDeleteShader(fsB);
         if (progDown != 0 && progBlur != 0) {
            uDownTex = GL33C.glGetUniformLocation(progDown, "uTex");
            uDownTexel = GL33C.glGetUniformLocation(progDown, "uTexel");
            uBlurTex = GL33C.glGetUniformLocation(progBlur, "uTex");
            uBlurDir = GL33C.glGetUniformLocation(progBlur, "uDir");
            vao = GL33C.glGenVertexArrays();
            sampler = GL33C.glGenSamplers();
            GL33C.glSamplerParameteri(sampler, 10241, 9729);
            GL33C.glSamplerParameteri(sampler, 10240, 9729);
            GL33C.glSamplerParameteri(sampler, 10242, 33071);
            GL33C.glSamplerParameteri(sampler, 10243, 33071);
            return true;
         } else {
            failed = true;
            return false;
         }
      } else {
         failed = true;
         return false;
      }
   }

   private static boolean ensureTargets(int w, int h) {
      if (bw == w && bh == h && fbo[0] != 0) {
         return true;
      }

      for (int i = 0; i < 2; i++) {
         if (fbo[i] != 0) {
            GL33C.glDeleteFramebuffers(fbo[i]);
         }

         if (tex[i] != 0) {
            GL33C.glDeleteTextures(tex[i]);
         }

         tex[i] = GL33C.glGenTextures();
         GL33C.glActiveTexture(33984);
         int prev = GL33C.glGetInteger(32873);
         GL33C.glBindTexture(3553, tex[i]);
         GL33C.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
         GL33C.glTexParameteri(3553, 10241, 9729);
         GL33C.glTexParameteri(3553, 10240, 9729);
         GL33C.glTexParameteri(3553, 10242, 33071);
         GL33C.glTexParameteri(3553, 10243, 33071);
         GL33C.glBindTexture(3553, prev);
         fbo[i] = GL33C.glGenFramebuffers();
         int prevFb = GL33C.glGetInteger(36006);
         GL33C.glBindFramebuffer(36160, fbo[i]);
         GL33C.glFramebufferTexture2D(36160, 36064, 3553, tex[i], 0);
         int status = GL33C.glCheckFramebufferStatus(36160);
         GL33C.glBindFramebuffer(36160, prevFb);
         if (status != 36053) {
            failed = true;
            return false;
         }
      }

      bw = w;
      bh = h;
      return true;
   }

   private static int compile(int type, String src) {
      int s = GL33C.glCreateShader(type);
      GL33C.glShaderSource(s, src);
      GL33C.glCompileShader(s);
      if (GL33C.glGetShaderi(s, 35713) == 0) {
         GL33C.glDeleteShader(s);
         return 0;
      } else {
         return s;
      }
   }

   private static int link(int vs, int fs) {
      int p = GL33C.glCreateProgram();
      GL33C.glAttachShader(p, vs);
      GL33C.glAttachShader(p, fs);
      GL33C.glLinkProgram(p);
      if (GL33C.glGetProgrami(p, 35714) == 0) {
         GL33C.glDeleteProgram(p);
         return 0;
      } else {
         return p;
      }
   }
}
