package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StorageESP extends ModuleBase {
   public static StorageESP INSTANCE;
   private static final int RESCAN_INTERVAL = 20;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private int tickCounter = 0;
   private List<ClarityRenderUtils.ESPEntry> cached = List.of();
   private final List<Vec3> tracerTargets = new java.util.ArrayList<>();
   private int cachedTracerArgb = -1;

   public StorageESP() {
      super("Storage ESP", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   @Override
   public void onEnable() {
      this.registerListener();
      this.tickCounter = 0;
      this.cached = List.of();
      this.tracerTargets.clear();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
      this.cached = List.of();
      this.tracerTargets.clear();
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

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null && mc.player != null) {
         if (++this.tickCounter >= 20) {
            this.tickCounter = 0;
            this.rescan(mc);
         }
      }
   }

   private void rescan(Minecraft mc) {
      double maxDistance = getMaxDistance();
      float fillOpacity = getFillOpacity();
      float lineOpacity = getOutlineOpacity();
      int chestColor = resolveColor("Chest Color", -19712, false);
      int shulkerColor = resolveColor("Shulker Color", -5157889, true);
      int furnaceColor = resolveColor("Furnace Color", -36797, true);
      int otherColor = resolveColor("Other Color", -11549705, true);
      boolean showChests = this.getSetting("Chests", true);
      boolean showShulkers = this.getSetting("Shulkers", true);
      boolean showFurnaces = this.getSetting("Furnaces", true);
      boolean showOther = this.getSetting("Other", true);
      Vec3 camPos = ClarityRenderUtils.getCameraPos();
      int renderDist = Math.min((Integer)mc.options.renderDistance().get(), 24);
      ChunkPos center = mc.player.chunkPosition();
      List<ClarityRenderUtils.ESPEntry> entries = new java.util.ArrayList<>();
      List<Vec3> tracers = new java.util.ArrayList<>();
      int tracerColor = 0;
      int tracerCount = 0;

      for (int dx = -renderDist; dx <= renderDist; dx++) {
         for (int dz = -renderDist; dz <= renderDist; dz++) {
            int cx = center.x() + dx;
            int cz = center.z() + dz;
            if (mc.level.hasChunk(cx, cz)) {
               LevelChunk chunk = mc.level.getChunk(cx, cz);

               for (BlockEntity be : chunk.getBlockEntities().values()) {
                  int rgb;
                  if (!(be instanceof ChestBlockEntity) && !(be instanceof BarrelBlockEntity) && !(be instanceof EnderChestBlockEntity)) {
                     if (be instanceof ShulkerBoxBlockEntity) {
                        if (!showShulkers) {
                           continue;
                        }

                        rgb = shulkerColor;
                     } else if (be instanceof AbstractFurnaceBlockEntity) {
                        if (!showFurnaces) {
                           continue;
                        }

                        rgb = furnaceColor;
                     } else {
                        if (!(be instanceof HopperBlockEntity) && !(be instanceof DispenserBlockEntity) && !(be instanceof DropperBlockEntity) && !(be instanceof BrewingStandBlockEntity)
                           || !showOther) {
                           continue;
                        }

                        rgb = otherColor;
                     }
                  } else {
                     if (!showChests) {
                        continue;
                     }

                     rgb = chestColor;
                  }

                  BlockPos pos = be.getBlockPos();
                  Vec3 centerVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                  if (!(maxDistance > 0.5) || !(camPos.distanceTo(centerVec) > maxDistance)) {
                     AABB box = new AABB(pos.getX() + 0.05, pos.getY() + 0.05, pos.getZ() + 0.05, pos.getX() + 0.95, pos.getY() + 0.95, pos.getZ() + 0.95);
                     int lineArgb = argb(rgb, lineOpacity);
                     entries.add(new ClarityRenderUtils.ESPEntry(box, lineArgb));
                     if (showTracers()) {
                        tracerColor = argb(rgb, 1.0F);
                        tracers.add(centerVec);
                        tracerCount++;
                     }
                  }
               }
            }
         }
      }

      this.cached = entries;
      this.tracerTargets.clear();
      this.tracerTargets.addAll(tracers);
      this.cachedTracerArgb = tracerColor;
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         if (!this.cached.isEmpty() || !this.tracerTargets.isEmpty()) {
            boolean throughWalls = isThroughWalls();
            boolean doFill = getFillEnabled();
            boolean doOutline = getOutlineEnabled();
            float lineWidth = getOutlineThickness();
            int fillArgb = argb(resolveColor("Fill Color", -1, false), getFillOpacity());
            if (!this.cached.isEmpty()) {
               ClarityRenderUtils.drawEspBoxes(poseStack, this.cached, doFill, fillArgb, doOutline, throughWalls, false, lineWidth);
            }

            if (showTracers() && !this.tracerTargets.isEmpty()) {
               ClarityRenderUtils.drawTracers(poseStack, partialTick, this.tracerTargets, this.cachedTracerArgb, 2.0F);
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

   public static boolean getFillEnabled() {
      return INSTANCE.getSetting("Fill", false);
   }

   public static float getFillOpacity() {
      return INSTANCE.getSetting("Fill Opacity", 0.15F);
   }

   public static boolean getOutlineEnabled() {
      return INSTANCE.getSetting("Outline", true);
   }

   public static float getOutlineOpacity() {
      return INSTANCE.getSetting("Outline Opacity", 0.85F);
   }

   public static float getOutlineThickness() {
      return INSTANCE.getSetting("Outline Thickness", 2.0F);
   }

   public static boolean showTracers() {
      return INSTANCE.getSetting("Tracers", false);
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
