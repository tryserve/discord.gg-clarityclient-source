package need.clarity.modules.visual;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.Clarity;
import need.clarity.config.ClarityConfig;
import need.clarity.config.ConfigFiles;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class SkinChanger extends ModuleBase {
   public static volatile SkinChanger INSTANCE;
   private static final Map<String, Identifier> TEXTURE_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, PlayerSkin> REMOTE = new ConcurrentHashMap<>();
   private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
   private static final Map<String, Long> FAILED_AT = new ConcurrentHashMap<>();
   private volatile Identifier activeTexture;
   private volatile String loadedSkinId = "";
   private int ticks = 0;
   private int retryAt = 0;
   private static volatile String lastError = "";

   public SkinChanger() {
      super("Skin Changer", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.loadedSkinId = "";
      this.retryAt = 0;
   }

   @Override
   public void onDisable() {
      this.activeTexture = null;
      this.loadedSkinId = "";
   }

   @Override
   public void onTick() {
      this.ticks++;
      String id = getSkinId();
      boolean wantsTexture = id.startsWith("custom_") || id.startsWith("file:");
      boolean failedBefore = wantsTexture && this.activeTexture == null && id.equals(this.loadedSkinId);
      if (!id.equals(this.loadedSkinId) || failedBefore && this.ticks >= this.retryAt) {
         this.loadedSkinId = id;
         this.activeTexture = null;
         if (wantsTexture) {
            try {
               this.loadCustomSkin(id);
            } catch (Throwable var5) {
            }

            if (this.activeTexture == null) {
               this.retryAt = this.ticks + 40;
            }
         }
      }

      String copy = copyPlayerName();
      if (!copy.isEmpty()) {
         requestRemote(copy);
      }
   }

   public static PlayerSkin override(PlayerSkin original) {
      SkinChanger m = INSTANCE;
      if (m != null && m.isEnabled() && original != null) {
         String copy = copyPlayerName();
         if (!copy.isEmpty()) {
            PlayerSkin s = m.copiedSkin(copy);
            if (s != null) {
               return s;
            }
         }

         String id = getSkinId();
         if ("default".equals(id)) {
            return DefaultPlayerSkin.getDefaultSkin();
         }

         Identifier tex = m.activeTexture;
         if (tex == null) {
            return null;
         }

         Texture body = new ResourceTexture(tex, tex);
         return new PlayerSkin(body, original.cape(), original.elytra(), m.modelFor(original.model()), original.secure());
      } else {
         return null;
      }
   }

   private PlayerSkin copiedSkin(String name) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(name);
         if (info != null && mc.player != null && !info.getProfile().id().equals(mc.player.getUUID())) {
            PlayerSkin s = info.getSkin();
            if (s != null) {
               return s;
            }
         }
      }

      return REMOTE.get(name.toLowerCase(Locale.ROOT));
   }

   private static void requestRemote(String name) {
      String key = name.toLowerCase(Locale.ROOT);
      if (!REMOTE.containsKey(key) && PENDING.add(key)) {
         Long failed = FAILED_AT.get(key);
         if (failed != null && System.currentTimeMillis() - failed < 30000L) {
            PENDING.remove(key);
         } else {
            Thread t = new Thread(() -> {
               try {
                  Minecraft mc = Minecraft.getInstance();
                  Optional<?> found = mc.services().profileResolver().fetchByName(name);
                  if (!(found.orElse(null) instanceof GameProfile basic)) {
                     throw new IllegalStateException("no such player");
                  }

                  UUID uuid = basic.id();
                  GameProfile full = basic;

                  try {
                     ProfileResult result = mc.services().sessionService().fetchProfile(uuid, true);
                     if (result != null && result.profile() != null) {
                        full = result.profile();
                     }
                  } catch (Throwable var18) {
                  }

                  Object r = mc.getSkinManager().get(full).get();
                  PlayerSkin skin = null;
                  if (r instanceof Optional<?> opt && opt.orElse(null) instanceof PlayerSkin ps) {
                     skin = ps;
                  } else if (r instanceof PlayerSkin ps) {
                     skin = ps;
                  }

                  if (skin == null) {
                     throw new IllegalStateException("no skin");
                  }

                  REMOTE.put(key, skin);
               } catch (Throwable t2) {
                  FAILED_AT.put(key, System.currentTimeMillis());
               } finally {
                  PENDING.remove(key);
               }
            }, "clarity-skin-fetch");
            t.setDaemon(true);
            t.start();
         }
      }
   }

   public static int[] pngSize(byte[] b) {
      if (b == null || b.length < 24) {
         return null;
      } else if ((b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71) {
         int w = (b[16] & 255) << 24 | (b[17] & 255) << 16 | (b[18] & 255) << 8 | b[19] & 255;
         int h = (b[20] & 255) << 24 | (b[21] & 255) << 16 | (b[22] & 255) << 8 | b[23] & 255;
         return new int[]{w, h};
      } else {
         return null;
      }
   }

   public static String lastError() {
      return lastError;
   }

   private void fail(String msg) {
      if (!msg.equals(lastError)) {
         lastError = msg;

         try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               mc.player.sendSystemMessage(Component.literal("§c[Clarity] Skin Changer: " + msg));
            }
         } catch (Throwable var3) {
         }
      }
   }

   private void loadCustomSkin(String id) {
      Identifier cached = TEXTURE_CACHE.get(id);
      if (cached != null) {
         this.activeTexture = cached;
      } else {
         byte[] bytes;
         if (id.startsWith("file:")) {
            Path f = ConfigFiles.skinsDir().resolve(id.substring(5)).normalize();
            if (!f.startsWith(ConfigFiles.skinsDir()) || !Files.isRegularFile(f)) {
               this.fail("skin file " + id.substring(5) + " is gone");
               return;
            }

            try {
               bytes = Files.readAllBytes(f);
            } catch (IOException e) {
               this.fail("couldn't read " + id.substring(5));
               return;
            }
         } else {
            String dataUrl = ClarityConfig.getCustomSkinUrl(id);
            if (dataUrl == null || dataUrl.isEmpty()) {
               this.fail("that skin isn't saved any more, pick it again");
               return;
            }

            String base64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(44) + 1) : dataUrl;

            try {
               bytes = Base64.getDecoder().decode(base64.trim());
            } catch (IllegalArgumentException e) {
               this.fail("that skin's data is damaged");
               return;
            }
         }

         int[] size = pngSize(bytes);
         if (size == null) {
            this.fail("that file isn't a PNG");
         } else if (size[0] == 64 && (size[1] == 64 || size[1] == 32)) {
            try (InputStream in = new ByteArrayInputStream(bytes)) {
               NativeImage img = NativeImage.read(in);
               if (img.getHeight() == 32 && img.getWidth() == 64) {
                  img = upgradeLegacy(img);
               }

               String path = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
               Identifier texId = Clarity.id("skins/" + path);
               DynamicTexture tex = new DynamicTexture(() -> id + "_custom_skin", img);
               Minecraft.getInstance().getTextureManager().register(texId, tex);
               TEXTURE_CACHE.put(id, texId);
               this.activeTexture = texId;
               lastError = "";
            } catch (Throwable e) {
               this.fail("couldn't load that skin");
            }
         } else {
            this.fail("skins must be 64x64 or 64x32");
         }
      }
   }

   private static NativeImage upgradeLegacy(NativeImage old) {
      NativeImage img = new NativeImage(64, 64, true);

      for (int y = 0; y < 32; y++) {
         for (int x = 0; x < 64; x++) {
            img.setPixel(x, y, old.getPixel(x, y));
         }
      }

      copyMirrored(img, 4, 16, 20, 48, 4, 4);
      copyMirrored(img, 8, 16, 24, 48, 4, 4);
      copyMirrored(img, 0, 20, 24, 52, 4, 12);
      copyMirrored(img, 4, 20, 20, 52, 4, 12);
      copyMirrored(img, 8, 20, 16, 52, 4, 12);
      copyMirrored(img, 12, 20, 28, 52, 4, 12);
      copyMirrored(img, 44, 16, 36, 48, 4, 4);
      copyMirrored(img, 48, 16, 40, 48, 4, 4);
      copyMirrored(img, 40, 20, 40, 52, 4, 12);
      copyMirrored(img, 44, 20, 36, 52, 4, 12);
      copyMirrored(img, 48, 20, 32, 52, 4, 12);
      copyMirrored(img, 52, 20, 44, 52, 4, 12);
      old.close();
      return img;
   }

   private static void copyMirrored(NativeImage img, int sx, int sy, int dx, int dy, int w, int h) {
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            img.setPixel(dx + w - 1 - x, dy + y, img.getPixel(sx + x, sy + y));
         }
      }
   }

   private PlayerModelType modelFor(PlayerModelType original) {
      String m = this.getSetting("Model", "Auto");
      if ("Slim".equals(m)) {
         return PlayerModelType.SLIM;
      } else {
         return "Wide".equals(m) ? PlayerModelType.WIDE : original;
      }
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public Identifier getActiveTexture() {
      return this.activeTexture;
   }

   public static String getSkinId() {
      if (INSTANCE == null) {
         return "none";
      }

      Object raw = INSTANCE.getSetting("Skin", "none");
      return raw != null ? raw.toString() : "none";
   }

   public static String copyPlayerName() {
      if (INSTANCE == null) {
         return "";
      }

      String v = INSTANCE.getSetting("Copy Player", "");
      return v == null ? "" : v.trim();
   }

   public static void invalidateCache(String id) {
      TEXTURE_CACHE.remove(id);
   }
}
