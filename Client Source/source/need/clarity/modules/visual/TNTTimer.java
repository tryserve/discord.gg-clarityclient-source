package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TNTTimer extends ModuleBase {
   public static TNTTimer INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public TNTTimer() {
      super("TNT Timer", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
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
         if (mc != null && mc.level != null) {
            double maxDistance = getMaxDistance();
            boolean throughWalls = isThroughWalls();
            int safeColor = getSafeColor();
            int dangerColor = getDangerColor();
            Vec3 camPos = ClarityRenderUtils.getCameraPos();
            List<ClarityRenderUtils.ESPEntry> entries = new java.util.ArrayList<>();

            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof PrimedTnt tnt && (!(maxDistance > 0.5) || !(camPos.distanceTo(tnt.position()) > maxDistance))) {
                  int fuse = tnt.getFuse();
                  float t = clamp01((fuse - partialTick) / 80.0F);
                  int rgb = lerp(dangerColor, safeColor, t);
                  int argb = 0xFF000000 | rgb & 16777215;
                  AABB box = tnt.getBoundingBox()
                     .inflate(0.1)
                     .move((tnt.xOld - tnt.getX()) * (1.0 - partialTick), (tnt.yOld - tnt.getY()) * (1.0 - partialTick), (tnt.zOld - tnt.getZ()) * (1.0 - partialTick));
                  entries.add(new ClarityRenderUtils.ESPEntry(box, argb));
               }
            }

            if (!entries.isEmpty()) {
               ClarityRenderUtils.drawEspBoxes(poseStack, entries, false, 0, true, throughWalls, false, getOutlineThickness());
            }
         }
      }
   }

   public static double getMaxDistance() {
      return INSTANCE.getSetting("Max Distance", 0.0F).floatValue();
   }

   public static boolean isThroughWalls() {
      return INSTANCE.getSetting("Through Walls", true);
   }

   public static float getOutlineThickness() {
      return INSTANCE.getSetting("Outline Thickness", 2.0F);
   }

   public static int getSafeColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Safe Color", ""), -12845218, false);
   }

   public static int getDangerColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Danger Color", ""), -50373, true);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static int lerp(int low, int high, float t) {
      int lr = low >> 16 & 0xFF;
      int lg = low >> 8 & 0xFF;
      int lb = low & 0xFF;
      int hr = high >> 16 & 0xFF;
      int hg = high >> 8 & 0xFF;
      int hb = high & 0xFF;
      return Math.round(lr + (hr - lr) * t) << 16 | Math.round(lg + (hg - lg) * t) << 8 | Math.round(lb + (hb - lb) * t);
   }
}
