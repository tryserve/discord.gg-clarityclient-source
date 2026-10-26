package need.clarity.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import org.lwjgl.opengl.GL11;

public final class GLCompat {
   private static boolean forceColorMaskOff = false;

   private GLCompat() {
   }

   public static boolean isColorMaskForcedOff() {
      return forceColorMaskOff;
   }

   public static void forceColorMaskOff(boolean force) {
      forceColorMaskOff = force;
      GL11.glColorMask(!force, !force, !force, !force);
   }

   public static void enableBlend() {
      GlStateManager._enableBlend();
   }

   public static void disableBlend() {
      GlStateManager._disableBlend();
   }

   public static void defaultBlendFunc() {
      blendFuncSeparate(770, 771, 1, 0);
   }

   public static void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
      GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
   }

   public static void enableDepthTest() {
      GlStateManager._enableDepthTest();
   }

   public static void disableDepthTest() {
      GlStateManager._disableDepthTest();
   }

   public static void activeTexture(int texture) {
      GlStateManager._activeTexture(texture);
   }

   public static void bindTexture(int textureId) {
      GlStateManager._bindTexture(textureId);
   }

   public static void bindTexture(GpuTexture texture) {
      bindTexture(RawFramebufferUtil.getGlId(texture));
   }

   public static void clearColor(float red, float green, float blue, float alpha) {
      GL11.glClearColor(red, green, blue, alpha);
   }

   public static void clear(int mask) {
      GL11.glClear(mask);
   }
}
