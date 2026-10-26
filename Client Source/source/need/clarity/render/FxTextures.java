package need.clarity.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class FxTextures {
   private static final Map<String, Identifier> CACHE = new HashMap<>();

   private FxTextures() {
   }

   public static Identifier get(String name) {
      if (CACHE.containsKey(name)) {
         return CACHE.get(name);
      }

      Identifier id = Identifier.fromNamespaceAndPath("clarity", "fxdyn/" + name);

      try (InputStream in = FxTextures.class.getResourceAsStream("/assets/clarity/textures/fx/" + name + ".png")) {
         if (in == null) {
            CACHE.put(name, null);
            return null;
         } else {
            NativeImage img = NativeImage.read(in);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "clarity_fx_" + name, img));
            CACHE.put(name, id);
            return id;
         }
      } catch (Throwable t) {
         CACHE.put(name, null);
         return null;
      }
   }
}
