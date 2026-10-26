package need.clarity.modules.visual;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public class Chams extends ModuleBase {
   public static final String[] MODES = new String[]{"Color", "Normal"};
   public static Chams INSTANCE;
   private static final Map<LivingEntityRenderState, Integer> chamsStates = Collections.synchronizedMap(new WeakHashMap<>());

   public static void updateState(Avatar avatar, AvatarRenderState state) {
      if (INSTANCE != null && INSTANCE.isEnabled() && shouldTarget(avatar)) {
         chamsStates.put(state, computeTint(avatar));
      } else {
         chamsStates.remove(state);
      }
   }

   public static Integer getStateTint(LivingEntityRenderState state) {
      return INSTANCE != null && INSTANCE.isEnabled() ? chamsStates.get(state) : null;
   }

   private static boolean shouldTarget(Avatar avatar) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && avatar instanceof Player) {
         Player self = mc.player;
         if (avatar == self && isHideSelf()) {
            return false;
         }

         if (!avatar.isAlive() || avatar.isSpectator()) {
            return false;
         }

         if (avatar != self && isHideTeammates() && isTeammate(avatar, self)) {
            return false;
         }

         float maxDist = getMaxDistance();
         return !(maxDist > 0.5F) || !(self.distanceTo(avatar) > maxDist);
      } else {
         return false;
      }
   }

   private static int computeTint(Avatar avatar) {
      if (isNormal()) {
         int alpha = Math.max(8, Math.round(clamp01(getNormalOpacity()) * 255.0F));
         return alpha << 24 | 16777215;
      }

      int rgb;
      if (isChroma()) {
         double time = System.nanoTime() / 1.0E9;
         float hue = (float)(time * getChromaSpeed() + avatar.getX() * 0.03 + avatar.getZ() * 0.03);
         rgb = hsvToRgb(hue);
      } else {
         rgb = getChamsColor() & 16777215;
      }

      int alpha = Math.max(8, Math.round(clamp01(getOpacity()) * 255.0F));
      return alpha << 24 | rgb;
   }

   public Chams() {
      super("Chams", "Visual");
      INSTANCE = this;
   }

   public static String getMode() {
      if (INSTANCE == null) {
         return "Color";
      }

      String m = INSTANCE.getSetting("Mode", "Color");

      for (String v : MODES) {
         if (v.equals(m)) {
            return m;
         }
      }

      return "Color";
   }

   public static boolean isNormal() {
      return "Normal".equals(getMode());
   }

   public static int getChamsColor() {
      if (INSTANCE == null) {
         return -43776;
      }

      String raw = INSTANCE.getSetting("Color", "");
      return ThemeColors.resolveArgb(raw, -43776, false);
   }

   public static float getOpacity() {
      return INSTANCE != null ? INSTANCE.getSetting("Opacity", 0.85F) : 0.85F;
   }

   public static float getNormalOpacity() {
      return INSTANCE != null ? INSTANCE.getSetting("Normal Opacity", 1.0F) : 1.0F;
   }

   public static boolean isRenderArmour() {
      return isNormal() || INSTANCE != null && INSTANCE.getSetting("Render Armour", false);
   }

   public static boolean isArmourThroughWalls() {
      return isNormal() || INSTANCE == null || INSTANCE.getSetting("Armour Through Walls", true);
   }

   public static boolean isThroughWalls() {
      return INSTANCE != null && INSTANCE.getSetting("Through Walls", true);
   }

   public static boolean isHideSelf() {
      return INSTANCE == null || INSTANCE.getSetting("Hide Self", true);
   }

   public static boolean isHideTeammates() {
      return INSTANCE != null && INSTANCE.getSetting("Hide Teammates", false);
   }

   public static boolean isChroma() {
      return INSTANCE != null && INSTANCE.getSetting("Chroma", false);
   }

   public static float getChromaSpeed() {
      return INSTANCE != null ? INSTANCE.getSetting("Chroma Speed", 0.3F) : 0.3F;
   }

   public static float getMaxDistance() {
      return INSTANCE != null ? INSTANCE.getSetting("Max Distance", 0.0F) : 0.0F;
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   private static boolean isTeammate(Entity a, Entity b) {
      PlayerTeam ta = a.getTeam();
      PlayerTeam tb = b.getTeam();
      if (ta != null && tb != null) {
         String na = ta.getName();
         return na != null && na.equals(tb.getName());
      } else {
         return false;
      }
   }

   private static int hsvToRgb(float hue) {
      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      int i = (int)h;
      float f = h - i;
      float v = 1.0F;
      float q = v * (1.0F - f);
      float t = v * (1.0F - (1.0F - f));
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = v;
            g = t;
            b = 0.0F;
            break;
         case 1:
            r = q;
            g = v;
            b = 0.0F;
            break;
         case 2:
            r = 0.0F;
            g = v;
            b = t;
            break;
         case 3:
            r = 0.0F;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = 0.0F;
            b = v;
            break;
         default:
            r = v;
            g = 0.0F;
            b = q;
      }

      return (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
   }
}
