package need.clarity.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.lang.reflect.Method;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class RawFramebufferUtil {
   private static int fbo = 0;
   private static boolean loggedBadTexture = false;
   private static boolean loggedIncomplete = false;

   private RawFramebufferUtil() {
   }

   public static void copy(RenderTarget src, RenderTarget dst, boolean copyDepth) {
      RenderSystem.assertOnRenderThread();
      if (isUsable(src) && isUsable(dst)) {
         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         int w = Math.min(src.width, dst.width);
         int h = Math.min(src.height, dst.height);
         GpuTexture srcColor = src.getColorTexture();
         GpuTexture dstColor = dst.getColorTexture();
         if (srcColor != null && dstColor != null) {
            encoder.copyTextureToTexture(srcColor, dstColor, 0, 0, 0, 0, 0, w, h);
         }

         if (copyDepth) {
            GpuTexture srcDepth = src.getDepthTexture();
            GpuTexture dstDepth = dst.getDepthTexture();
            if (srcDepth != null && dstDepth != null) {
               encoder.copyTextureToTexture(srcDepth, dstDepth, 0, 0, 0, 0, 0, w, h);
            }
         }
      }
   }

   public static void clear(RenderTarget target) {
      RenderSystem.assertOnRenderThread();
      if (isUsable(target)) {
         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         GpuTexture color = target.getColorTexture();
         if (color != null && !color.isClosed()) {
            encoder.clearColorTexture(color, 0);
         }
      }
   }

   public static void clearColorAndDepth(RenderTarget target) {
      RenderSystem.assertOnRenderThread();
      if (isUsable(target)) {
         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         GpuTexture color = target.getColorTexture();
         GpuTexture depth = target.getDepthTexture();
         if (color != null && !color.isClosed()) {
            if (depth != null && !depth.isClosed()) {
               encoder.clearColorAndDepthTextures(color, 0, depth, 1.0);
            } else {
               encoder.clearColorTexture(color, 0);
            }
         }
      }
   }

   public static boolean bindDirect(RenderTarget target) {
      RenderSystem.assertOnRenderThread();
      if (!isUsable(target)) {
         return false;
      }

      int prevBinding = GL11.glGetInteger(36006);
      GpuTexture color = target.getColorTexture();
      int colorId = getGlId(color);
      GpuTexture depth = target.getDepthTexture();
      int depthId = depth != null && !depth.isClosed() ? getGlId(depth) : 0;
      if (colorId == 0 || !GL11.glIsTexture(colorId) || depthId != 0 && !GL11.glIsTexture(depthId)) {
         if (!loggedBadTexture) {
            loggedBadTexture = true;
         }

         return false;
      } else {
         for (int attempt = 0; attempt < 2; attempt++) {
            ensureFbo();
            attach(colorId, depthId);
            int status = GL30.glCheckFramebufferStatus(36160);
            if (status == 36053) {
               GL11.glDrawBuffer(36064);
               GL11.glViewport(0, 0, target.width, target.height);
               return true;
            }

            if (!loggedIncomplete) {
               loggedIncomplete = true;
            }

            deleteFbo();
         }

         GL30.glBindFramebuffer(36160, GL30.glIsFramebuffer(prevBinding) ? prevBinding : 0);
         return false;
      }
   }

   private static void attach(int colorId, int depthId) {
      GL30.glBindFramebuffer(36160, fbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, colorId, 0);
      GL30.glFramebufferTexture2D(36160, 36096, 3553, depthId, 0);
   }

   public static void invalidate() {
      if (fbo != 0) {
         int prevDraw = GL11.glGetInteger(36006);
         int prevRead = GL11.glGetInteger(36010);
         GL30.glBindFramebuffer(36160, fbo);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
         GL30.glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
         GL30.glBindFramebuffer(36009, prevDraw == fbo ? 0 : prevDraw);
         GL30.glBindFramebuffer(36008, prevRead == fbo ? 0 : prevRead);
      }
   }

   public static void dispose() {
      deleteFbo();
   }

   private static void deleteFbo() {
      if (fbo != 0) {
         if (GL11.glGetInteger(36006) == fbo) {
            GL30.glBindFramebuffer(36009, 0);
         }

         if (GL11.glGetInteger(36010) == fbo) {
            GL30.glBindFramebuffer(36008, 0);
         }

         GL30.glDeleteFramebuffers(fbo);
         fbo = 0;
      }
   }

   public static void unbind() {
      GL30.glBindFramebuffer(36160, 0);
   }

   public static int getColorTextureId(RenderTarget target) {
      return target == null ? 0 : getGlId(target.getColorTexture());
   }

   public static int getDepthTextureId(RenderTarget target) {
      return target == null ? 0 : getGlId(target.getDepthTexture());
   }

   private static boolean isUsable(RenderTarget target) {
      return target != null && target.width > 0 && target.height > 0 && target.getColorTexture() != null;
   }

   private static void ensureFbo() {
      if (fbo == 0) {
         fbo = GL30.glGenFramebuffers();
      }
   }

   public static int getGlId(GpuTexture texture) {
      if (texture == null || texture.isClosed()) {
         return 0;
      } else if (texture instanceof GlTexture gt) {
         return gt.glId();
      } else {
         try {
            Method m = texture.getClass().getMethod("glId");
            return (Integer)m.invoke(texture);
         } catch (Exception e) {
            return 0;
         }
      }
   }
}
