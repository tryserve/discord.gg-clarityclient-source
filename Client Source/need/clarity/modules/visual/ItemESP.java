package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ItemESP extends ModuleBase {
   public static ItemESP INSTANCE;
   private static final double MAX_ENTRIES = 4096.0;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public ItemESP() {
      super("Item ESP", "Visual");
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

   public static boolean shaderActive() {
      ItemESP i = INSTANCE;
      return i != null && i.isEnabled() && "Shader Glow".equals(i.getSetting("Style", "Box"));
   }

   public static int shaderColor() {
      ItemESP i = INSTANCE;
      return i == null ? 16765503 : ThemeColors.resolveRgb(i.getSetting("Glow Color", "#FFD23F"), 16765503, false);
   }

   public static float shaderRadius() {
      ItemESP i = INSTANCE;
      return i == null ? 10.0F : Math.max(2.0F, Math.min(40.0F, i.getSetting("Glow Radius", 10.0F)));
   }

   public static boolean shaderInclude(Entity e) {
      ItemESP i = INSTANCE;
      if (i == null) {
         return false;
      }

      double max = getMaxDistance();
      return max <= 0.5 || ClarityRenderUtils.getCameraPos().distanceTo(e.position()) <= max;
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         if (!shaderActive()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null && mc.player != null) {
               boolean throughWalls = isThroughWalls();
               boolean doFill = getFillEnabled();
               boolean doOutline = getOutlineEnabled();
               double maxDistance = getMaxDistance();
               float lineWidth = getOutlineThickness();
               int fillArgb = argb(getFillColor(), getFillOpacity());
               int outlineArgb = argb(getOutlineColor(), getOutlineOpacity());
               Vec3 camPos = ClarityRenderUtils.getCameraPos();
               List<ClarityRenderUtils.ESPEntry> entries = new java.util.ArrayList<>();
               int added = 0;

               for (Entity e : mc.level.entitiesForRendering()) {
                  if (e instanceof ItemEntity) {
                     if (added >= 4096.0) {
                        break;
                     }

                     Vec3 pos = new Vec3(e.xOld + (e.getX() - e.xOld) * partialTick, e.yOld + (e.getY() - e.yOld) * partialTick, e.zOld + (e.getZ() - e.zOld) * partialTick);
                     double dist = camPos.distanceTo(pos);
                     if (!(maxDistance > 0.5) || !(dist > maxDistance)) {
                        float fade = 1.0F;
                        if (isDistanceFade() && maxDistance > 0.5) {
                           fade = (float)(1.0 - dist / maxDistance);
                           if (fade < 0.0F) {
                              fade = 0.0F;
                           }

                           if (fade > 1.0F) {
                              fade = 1.0F;
                           }
                        }

                        AABB box = e.getBoundingBox().move(pos.x - e.getX(), pos.y - e.getY(), pos.z - e.getZ()).inflate(0.05);
                        entries.add(new ClarityRenderUtils.ESPEntry(box, scaleAlpha(outlineArgb, fade)));
                        added++;
                     }
                  }
               }

               if (!entries.isEmpty()) {
                  ClarityRenderUtils.drawEspBoxes(poseStack, entries, doFill, fillArgb, doOutline, throughWalls, false, lineWidth);
               }
            }
         }
      }
   }

   public static boolean isThroughWalls() {
      return INSTANCE.getSetting("Through Walls", true);
   }

   public static boolean getFillEnabled() {
      return INSTANCE.getSetting("Fill", false);
   }

   public static int getFillColor() {
      return resolveColor("Fill Color", -16733441, true);
   }

   public static float getFillOpacity() {
      return INSTANCE.getSetting("Fill Opacity", 0.25F);
   }

   public static boolean getOutlineEnabled() {
      return INSTANCE.getSetting("Outline", true);
   }

   public static int getOutlineColor() {
      return resolveColor("Outline Color", -10934, false);
   }

   public static float getOutlineOpacity() {
      return INSTANCE.getSetting("Outline Opacity", 0.85F);
   }

   public static float getOutlineThickness() {
      return INSTANCE.getSetting("Outline Thickness", 2.0F);
   }

   public static float getMaxDistance() {
      return INSTANCE.getSetting("Max Distance", 0.0F);
   }

   public static boolean isDistanceFade() {
      return INSTANCE.getSetting("Distance Fade", false);
   }

   private static int scaleAlpha(int argb, float f) {
      int a = Math.round((argb >>> 24 & 0xFF) * f);
      if (a < 0) {
         a = 0;
      } else if (a > 255) {
         a = 255;
      }

      return a << 24 | argb & 16777215;
   }

   private static int resolveColor(String key, int fallback, boolean secondary) {
      return ThemeColors.resolveArgb(INSTANCE.getSetting(key, ""), fallback, secondary);
   }

   private static int argb(int rgb, float opacity) {
      int a = Math.round(clamp01(opacity) * 255.0F);
      return a << 24 | rgb & 16777215;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
