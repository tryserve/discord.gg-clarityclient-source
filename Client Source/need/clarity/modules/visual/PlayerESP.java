package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.render.PlayerESPRenderer;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

public class PlayerESP extends ModuleBase {
   public static PlayerESP INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public PlayerESP() {
      super("Player ESP", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   @Override
   public void onEnable() {
      this.registerListener();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
   }

   private void registerListener() {
      if (!this.listenerRegistered) {
         this.listenerRegistered = true;
         WorldRenderEvent.register(this.renderListener);
      }
   }

   private void unregisterListener() {
      if (this.listenerRegistered) {
         this.listenerRegistered = false;
         WorldRenderEvent.unregister(this.renderListener);
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            boolean hideSelf = isHideSelf();
            boolean hideInvisible = isHideInvisible();
            boolean hideTeammates = isHideTeammates();
            boolean smooth = isSmooth();
            boolean healthTint = isHealthColor();
            boolean chroma = getChromaEnabled();
            float chromaSpeed = getChromaSpeed();
            float maxDistance = getMaxDistance();
            boolean distanceFade = isDistanceFade();
            float fadeDistance = maxDistance > 0.5F ? maxDistance : 32.0F;
            int baseColor = getOutlineColor();
            int healthLow = getHealthLowColor();
            int healthHigh = getHealthHighColor();
            double time = System.nanoTime() / 1.0E9;
            Vec3 camPos = ClarityRenderUtils.getCameraPos();
            Player self = mc.player;
            List<ClarityRenderUtils.ESPEntry> entries = new java.util.ArrayList<>();

            for (Player player : mc.level.players()) {
               if ((player != self || !hideSelf) && player.isAlive() && !player.isSpectator() && (!hideInvisible || !player.isInvisible()) && (!hideTeammates || !isTeammate(player, self))) {
                  Vec3 pos = smooth ? player.getPosition(partialTick) : player.position();
                  AABB box = player.getBoundingBox().move(pos.x - player.getX(), pos.y - player.getY(), pos.z - player.getZ());
                  double dist = camPos.distanceTo(pos);
                  if (!(maxDistance > 0.5F) || !(dist > maxDistance)) {
                     float fade = 1.0F;
                     if (distanceFade) {
                        fade = (float)(1.0 - dist / fadeDistance);
                        if (fade < 0.0F) {
                           fade = 0.0F;
                        }

                        if (fade > 1.0F) {
                           fade = 1.0F;
                        }
                     }

                     int rgb;
                     if (chroma) {
                        float hue = (float)(time * chromaSpeed + pos.x * 0.03 + pos.y * 0.02 + pos.z * 0.03);
                        rgb = hsvColor(hue, 1.0F, 1.0F);
                     } else if (healthTint) {
                        float hp = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
                        rgb = lerpColor(healthLow, healthHigh, clamp01(hp));
                     } else {
                        rgb = baseColor & 16777215;
                     }

                     int friendRgb = Friends.isFriend(player) ? FriendManager.espColour(player.getUUID().toString(), Friends.nameOf(player)) : -1;
                     if (friendRgb >= 0) {
                        rgb = friendRgb;
                     }

                     int alpha = Math.round(255.0F * fade);
                     int argb = alpha << 24 | rgb & 16777215;
                     entries.add(new ClarityRenderUtils.ESPEntry(box, argb));
                  }
               }
            }

            if (!entries.isEmpty()) {
               PlayerESPRenderer.render(poseStack, entries, partialTick);
            }
         }
      }
   }

   private static boolean isTeammate(Player a, Player b) {
      PlayerTeam ta = a.getTeam();
      PlayerTeam tb = b.getTeam();
      if (ta != null && tb != null) {
         String na = ta.getName();
         String nb = tb.getName();
         return na != null && na.equals(nb);
      } else {
         return false;
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static int lerpColor(int low, int high, float t) {
      float lr = (low >> 16 & 0xFF) / 255.0F;
      float lg = (low >> 8 & 0xFF) / 255.0F;
      float lb = (low & 0xFF) / 255.0F;
      float hr = (high >> 16 & 0xFF) / 255.0F;
      float hg = (high >> 8 & 0xFF) / 255.0F;
      float hb = (high & 0xFF) / 255.0F;
      int r = Math.round((lr + (hr - lr) * t) * 255.0F);
      int g = Math.round((lg + (hg - lg) * t) * 255.0F);
      int b = Math.round((lb + (hb - lb) * t) * 255.0F);
      return r << 16 | g << 8 | b;
   }

   private static int hsvColor(float hue, float sat, float val) {
      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      int i = (int)h;
      float f = h - i;
      float p = val * (1.0F - sat);
      float q = val * (1.0F - sat * f);
      float t = val * (1.0F - sat * (1.0F - f));
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = val;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = val;
            b = p;
            break;
         case 2:
            r = p;
            g = val;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = val;
            break;
         case 4:
            r = t;
            g = p;
            b = val;
            break;
         default:
            r = val;
            g = p;
            b = q;
      }

      int ri = Math.round(r * 255.0F);
      int gi = Math.round(g * 255.0F);
      int bi = Math.round(b * 255.0F);
      return ri << 16 | gi << 8 | bi;
   }

   public static String getMode() {
      String mode = INSTANCE != null ? INSTANCE.getSetting("Mode", "Outline") : "Outline";
      return !"Box".equals(mode) && !"3D".equals(mode) && !"Outline".equals(mode) ? "Outline" : mode;
   }

   public static boolean getOutlineEnabled() {
      return INSTANCE.getSetting("Outline", true);
   }

   public static int getOutlineColor() {
      return resolveColor(INSTANCE.getSetting("Outline Color", ""), -16733441, false);
   }

   public static float getOutlineThickness() {
      return INSTANCE.getSetting("Outline Thickness", 2.0F);
   }

   public static boolean getFillEnabled() {
      return INSTANCE.getSetting("Fill", false);
   }

   public static int getFillColor() {
      return resolveColor(INSTANCE.getSetting("Fill Color", ""), -16733441, true);
   }

   public static float getFillOpacity() {
      return INSTANCE.getSetting("Fill Opacity", 0.25F);
   }

   public static String getBoxStyle() {
      return INSTANCE.getSetting("Box Style", "Full");
   }

   public static boolean getChromaEnabled() {
      return INSTANCE.getSetting("Chroma", false);
   }

   public static float getChromaSpeed() {
      return INSTANCE.getSetting("Chroma Speed", 0.3F);
   }

   public static boolean isHealthColor() {
      return INSTANCE.getSetting("Health Color", false);
   }

   public static int getHealthLowColor() {
      return resolveColor(INSTANCE.getSetting("Health Low", ""), -50373, true);
   }

   public static int getHealthHighColor() {
      return resolveColor(INSTANCE.getSetting("Health High", ""), -12845218, false);
   }

   public static float getMaxDistance() {
      return INSTANCE.getSetting("Max Distance", 0.0F);
   }

   public static boolean isDistanceFade() {
      return INSTANCE.getSetting("Distance Fade", false);
   }

   public static boolean isThroughWalls() {
      return INSTANCE.getSetting("Through Walls", true);
   }

   public static boolean isHideInvisible() {
      return INSTANCE.getSetting("Hide Invisible", true);
   }

   public static boolean isHideTeammates() {
      return INSTANCE.getSetting("Hide Teammates", false);
   }

   public static boolean isHideSelf() {
      return INSTANCE.getSetting("Hide Self", true);
   }

   public static boolean isSmooth() {
      return INSTANCE.getSetting("Smooth", true);
   }

   private static int resolveColor(String hex, int fallback, boolean secondary) {
      return ThemeColors.resolveArgb(hex, fallback, secondary);
   }
}
