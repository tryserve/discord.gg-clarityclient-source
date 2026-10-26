package need.clarity.modules.visual;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public class EnchantGlint extends ModuleBase {
   private static final String DEFAULT_COLOR = "#8040FF";
   private static final Identifier[] GLINTS = new Identifier[]{
      Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
      Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png"),
      Identifier.withDefaultNamespace("textures/misc/enchanted_glint_entity.png")
   };
   public static volatile EnchantGlint INSTANCE;
   private int lastColor = Integer.MIN_VALUE;
   private final DynamicTexture[] ours = new DynamicTexture[GLINTS.length];
   private boolean applied = false;
   private int retryTicks = 0;

   public EnchantGlint() {
      super("Enchant Glint", "Visual");
      INSTANCE = this;

      try {
         PerFrameEvents.register(this::onFrame);
      } catch (Throwable var2) {
      }
   }

   private void onFrame() {
      if (!this.isEnabled() && this.applied) {
         this.restore();
      }
   }

   @Override
   public void onEnable() {
      this.applied = false;
      this.lastColor = Integer.MIN_VALUE;
      this.retryTicks = 0;
   }

   @Override
   public void onDisable() {
      if (this.applied) {
         this.restore();
      }
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getTextureManager() != null) {
            int c = this.resolveColor();
            boolean replaced = this.applied && !this.stillOurs(mc);
            if (!this.applied || c != this.lastColor || replaced) {
               if (this.retryTicks > 0) {
                  this.retryTicks--;
                  return;
               }

               if (!this.refresh(c)) {
                  this.retryTicks = 40;
               }
            }
         }
      }
   }

   private boolean stillOurs(Minecraft mc) {
      for (int i = 0; i < GLINTS.length; i++) {
         if (this.ours[i] != null && mc.getTextureManager().getTexture(GLINTS[i]) != this.ours[i]) {
            return false;
         }
      }

      return true;
   }

   private int resolveColor() {
      String hex = this.getSetting("Glint Color", "#8040FF");
      return ThemeColors.resolveRgb(hex, 8405247, false) & 16777215;
   }

   private boolean refresh(int rgb) {
      boolean any = false;

      for (int i = 0; i < GLINTS.length; i++) {
         DynamicTexture t = this.tinted(GLINTS[i], rgb);
         this.ours[i] = t;
         if (t != null) {
            any = true;
         }
      }

      this.applied = any;
      this.lastColor = any ? rgb : Integer.MIN_VALUE;
      return any;
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();

      for (int i = 0; i < GLINTS.length; i++) {
         if (this.ours[i] != null) {
            NativeImage img = this.load(GLINTS[i]);
            if (img != null) {
               try {
                  mc.getTextureManager().register(GLINTS[i], new DynamicTexture(() -> "glint", img));
               } catch (Throwable t) {
                  try {
                     img.close();
                  } catch (Throwable var6) {
                  }
               }
            }

            this.ours[i] = null;
         }
      }

      this.applied = false;
      this.lastColor = Integer.MIN_VALUE;
   }

   private DynamicTexture tinted(Identifier id, int rgb) {
      NativeImage img = this.load(id);
      if (img == null) {
         return null;
      }

      try {
         float cr = (rgb >> 16 & 0xFF) / 255.0F;
         float cg = (rgb >> 8 & 0xFF) / 255.0F;
         float cb = (rgb & 0xFF) / 255.0F;

         for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
               int p = img.getPixel(x, y);
               int a = p >>> 24 & 0xFF;
               int r = p >>> 16 & 0xFF;
               int g = p >>> 8 & 0xFF;
               int b = p & 0xFF;
               float lum = Math.max(r, Math.max(g, b)) / 255.0F * 1.15F;
               int nr = Math.min(255, Math.round(lum * cr * 255.0F));
               int ng = Math.min(255, Math.round(lum * cg * 255.0F));
               int nb = Math.min(255, Math.round(lum * cb * 255.0F));
               img.setPixel(x, y, a << 24 | nr << 16 | ng << 8 | nb);
            }
         }

         DynamicTexture tex = new DynamicTexture(() -> "clarity_glint", img);
         Minecraft.getInstance().getTextureManager().register(id, tex);
         return tex;
      } catch (Throwable t) {
         try {
            img.close();
         } catch (Throwable var18) {
         }

         return null;
      }
   }

   private NativeImage load(Identifier id) {
      try (InputStream in = ((Resource)Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow()).open()) {
         return NativeImage.read(in);
      } catch (Throwable t) {
         return null;
      }
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static float[] getActiveGlintColor() {
      EnchantGlint g = INSTANCE;
      int c = g != null ? g.resolveColor() : 8405247;
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }
}
