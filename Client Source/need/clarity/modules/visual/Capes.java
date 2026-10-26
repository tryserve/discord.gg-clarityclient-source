package need.clarity.modules.visual;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.Clarity;
import need.clarity.PerFrameEvents;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.render.ShaderCapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class Capes extends ModuleBase {
   public static volatile Capes INSTANCE;
   private static final Map<String, Identifier> TEXTURE_CACHE = new ConcurrentHashMap<>();
   private volatile Identifier activeTexture;
   private volatile String loadedCapeId = "";
   private int retryTicks = 0;

   public Capes() {
      super("Capes", "Visual");
      INSTANCE = this;
      PerFrameEvents.register(ShaderCapes::frame);
   }

   @Override
   public void onEnable() {
      this.refreshTexture();
   }

   @Override
   public void onDisable() {
      this.activeTexture = null;
      this.loadedCapeId = "";
      ShaderCapes.select("none");
   }

   @Override
   public void onTick() {
      String id = this.getCapeId();
      boolean failedLoad = this.activeTexture == null && !"none".equals(id) && !id.isEmpty();
      if (!id.equals(this.loadedCapeId) || failedLoad && ++this.retryTicks >= 40) {
         this.retryTicks = 0;
         this.refreshTexture();
      }
   }

   public Identifier getActiveTexture() {
      return this.activeTexture;
   }

   private String getCapeId() {
      Object raw = this.getSetting("Cape", "none");
      return raw != null ? raw.toString() : "none";
   }

   private void refreshTexture() {
      String id = this.getCapeId();
      this.loadedCapeId = id;
      this.activeTexture = null;
      if ("none".equals(id) || id.isEmpty()) {
         ShaderCapes.select("none");
      } else if (ShaderCapes.isShaderCape(id)) {
         this.activeTexture = ShaderCapes.select(id);
      } else {
         ShaderCapes.select("none");
         Identifier cached = TEXTURE_CACHE.get(id);
         if (cached != null) {
            this.activeTexture = cached;
         } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
               if (id.startsWith("custom_")) {
                  this.loadCustomCape(mc, id);
               } else {
                  this.loadBundledCape(mc, id);
               }
            }
         }
      }
   }

   private void loadBundledCape(Minecraft mc, String id) {
      String resourcePath = "capes/" + id + ".png";
      Identifier texId = Clarity.id("capes/" + id);

      try {
         InputStream stream = mc.getResourceManager().open(Clarity.id(resourcePath));
         NativeImage img = NativeImage.read(stream);
         stream.close();
         DynamicTexture tex = new DynamicTexture(() -> id + "_cape", img);
         mc.getTextureManager().register(texId, tex);
         TEXTURE_CACHE.put(id, texId);
         this.activeTexture = texId;
      } catch (Throwable var8) {
      }
   }

   private void loadCustomCape(Minecraft mc, String id) {
      String dataUrl = ClarityConfig.getCustomCapeUrl(id);
      if (dataUrl != null && !dataUrl.isEmpty()) {
         try {
            String base64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(44) + 1) : dataUrl;
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            Identifier texId = Clarity.id("capes/" + id);
            DynamicTexture tex = new DynamicTexture(() -> id + "_custom_cape", img);
            mc.getTextureManager().register(texId, tex);
            TEXTURE_CACHE.put(id, texId);
            this.activeTexture = texId;
         } catch (Exception var9) {
         }
      }
   }

   public static void invalidateCache(String id) {
      TEXTURE_CACHE.remove(id);
   }
}
