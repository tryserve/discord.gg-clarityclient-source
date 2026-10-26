package need.clarity.clickgui;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

public final class RawGlState implements AutoCloseable {
   private static final int UNITS = 4;
   private int program;
   private int vao;
   private int arrayBuffer;
   private int uniformBuffer;
   private int activeTexture;
   private int readFb;
   private int drawFb;
   private int pixelUnpackBuffer;
   private int pixelPackBuffer;
   private final int[] tex2d = new int[4];
   private final int[] samplers = new int[4];
   private int unpackAlignment;
   private int unpackRowLength;
   private int unpackSkipPixels;
   private int unpackSkipRows;
   private int vpX;
   private int vpY;
   private int vpW;
   private int vpH;
   private boolean blend;
   private boolean depth;
   private boolean cull;
   private boolean stencil;
   private boolean scissor;
   private int blendSrcRgb;
   private int blendDstRgb;
   private int blendSrcA;
   private int blendDstA;
   private int blendEqRgb;
   private int blendEqA;
   private int cullMode;
   private int frontFace;
   private int depthFunc;
   private boolean depthMask;
   private int stencilWriteMask;
   private int stencilFunc;
   private int stencilRef;
   private int stencilValueMask;
   private int stencilFail;
   private int stencilZFail;
   private int stencilZPass;
   private final byte[] colorMask = new byte[4];
   private int ubo0;
   private long ubo0Start;
   private long ubo0Size;
   private final float[] clearColor = new float[4];
   private int clearStencil;

   public static RawGlState capture() {
      RawGlState s = new RawGlState();
      s.program = GL33C.glGetInteger(35725);
      s.vao = GL33C.glGetInteger(34229);
      s.arrayBuffer = GL33C.glGetInteger(34964);
      s.uniformBuffer = GL33C.glGetInteger(35368);
      s.activeTexture = GL33C.glGetInteger(34016);
      s.readFb = GL33C.glGetInteger(36010);
      s.drawFb = GL33C.glGetInteger(36006);
      s.pixelUnpackBuffer = GL33C.glGetInteger(35055);
      s.pixelPackBuffer = GL33C.glGetInteger(35053);
      s.unpackAlignment = GL33C.glGetInteger(3317);
      s.unpackRowLength = GL33C.glGetInteger(3314);
      s.unpackSkipPixels = GL33C.glGetInteger(3316);
      s.unpackSkipRows = GL33C.glGetInteger(3315);

      for (int i = 0; i < 4; i++) {
         GL33C.glActiveTexture(33984 + i);
         s.tex2d[i] = GL33C.glGetInteger(32873);
         s.samplers[i] = GL33C.glGetInteger(35097);
      }

      GL33C.glActiveTexture(s.activeTexture);

      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer vp = stack.mallocInt(4);
         GL33C.glGetIntegerv(2978, vp);
         s.vpX = vp.get(0);
         s.vpY = vp.get(1);
         s.vpW = vp.get(2);
         s.vpH = vp.get(3);
         ByteBuffer cm = stack.malloc(4);
         GL33C.glGetBooleanv(3107, cm);
         cm.get(s.colorMask);
         ByteBuffer dm = stack.malloc(1);
         GL33C.glGetBooleanv(2930, dm);
         s.depthMask = dm.get(0) != 0;
      }

      s.ubo0 = GL33C.glGetIntegeri(35368, 0);
      s.ubo0Start = GL33C.glGetInteger64i(35369, 0);
      s.ubo0Size = GL33C.glGetInteger64i(35370, 0);
      GL33C.glGetFloatv(3106, s.clearColor);
      s.clearStencil = GL33C.glGetInteger(2961);
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
      s.depthFunc = GL33C.glGetInteger(2932);
      s.stencilWriteMask = GL33C.glGetInteger(2968);
      s.stencilFunc = GL33C.glGetInteger(2962);
      s.stencilRef = GL33C.glGetInteger(2967);
      s.stencilValueMask = GL33C.glGetInteger(2963);
      s.stencilFail = GL33C.glGetInteger(2964);
      s.stencilZFail = GL33C.glGetInteger(2965);
      s.stencilZPass = GL33C.glGetInteger(2966);
      return s;
   }

   @Override
   public void close() {
      GL33C.glBindFramebuffer(36008, this.readFb);
      GL33C.glBindFramebuffer(36009, this.drawFb);
      GL33C.glViewport(this.vpX, this.vpY, this.vpW, this.vpH);
      GL33C.glUseProgram(this.program);
      GL33C.glBindVertexArray(this.vao);
      GL33C.glBindBuffer(34962, this.arrayBuffer);
      if (this.ubo0 != 0 && this.ubo0Size > 0L) {
         GL33C.glBindBufferRange(35345, 0, this.ubo0, this.ubo0Start, this.ubo0Size);
      } else {
         GL33C.glBindBufferBase(35345, 0, this.ubo0);
      }

      GL33C.glBindBuffer(35345, this.uniformBuffer);

      for (int i = 0; i < 4; i++) {
         GL33C.glActiveTexture(33984 + i);
         GL33C.glBindTexture(3553, this.tex2d[i]);
         GL33C.glBindSampler(i, this.samplers[i]);
      }

      GL33C.glActiveTexture(this.activeTexture);
      GL33C.glBindBuffer(35052, this.pixelUnpackBuffer);
      GL33C.glBindBuffer(35051, this.pixelPackBuffer);
      GL33C.glPixelStorei(3317, this.unpackAlignment);
      GL33C.glPixelStorei(3314, this.unpackRowLength);
      GL33C.glPixelStorei(3316, this.unpackSkipPixels);
      GL33C.glPixelStorei(3315, this.unpackSkipRows);
      toggle(3042, this.blend);
      toggle(2929, this.depth);
      toggle(2884, this.cull);
      toggle(2960, this.stencil);
      toggle(3089, this.scissor);
      GL33C.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcA, this.blendDstA);
      GL33C.glBlendEquationSeparate(this.blendEqRgb, this.blendEqA);
      GL33C.glCullFace(this.cullMode);
      GL33C.glFrontFace(this.frontFace);
      GL33C.glDepthFunc(this.depthFunc);
      GL33C.glDepthMask(this.depthMask);
      GL33C.glStencilMask(this.stencilWriteMask);
      GL33C.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
      GL33C.glStencilOp(this.stencilFail, this.stencilZFail, this.stencilZPass);
      GL33C.glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
      GL33C.glClearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
      GL33C.glClearStencil(this.clearStencil);
   }

   private static void toggle(int cap, boolean on) {
      if (on) {
         GL33C.glEnable(cap);
      } else {
         GL33C.glDisable(cap);
      }
   }
}
