package need.clarity.modules.visual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import need.clarity.PerPacketEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.config.ClarityConfig;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Search extends ModuleBase {
   private static Search instance;
   private final Map<ChunkPos, Search.ChunkSearcher> searchers = new HashMap<>();
   private final Set<ChunkPos> chunksToRescan = Collections.synchronizedSet(new HashSet<>());
   private static final ExecutorService SCAN_POOL = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), r -> {
      Thread t = new Thread(r, "clarity-search-chunk");
      t.setDaemon(true);
      t.setPriority(1);
      return t;
   });
   private volatile Set<Block> targetBlocks = Collections.emptySet();
   private volatile boolean onlyVisible = false;
   private final Consumer<EventPacketReceive> packetListener = this::onPacket;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;

   public static void shutdownPool() {
      try {
         SCAN_POOL.shutdownNow();
      } catch (Throwable var1) {
      }
   }

   public Search() {
      super("Search", "Visual");
      instance = this;
   }

   @Override
   public void onEnable() {
      PerPacketEvents.register(this.packetListener);
      WorldRenderEvent.register(this.renderListener);
      this.targetBlocks = this.resolveTargetBlocks();
      this.rescheduleAll();
   }

   @Override
   public void onDisable() {
      PerPacketEvents.unregister(this.packetListener);
      WorldRenderEvent.unregister(this.renderListener);
      this.cancelAll();
      this.chunksToRescan.clear();
      this.targetBlocks = Collections.emptySet();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         boolean onlyVisible = this.getSetting("Only Visible Blocks", false);
         Set<Block> newTargets = this.resolveTargetBlocks();
         if (!newTargets.equals(this.targetBlocks) || onlyVisible != this.onlyVisible) {
            this.targetBlocks = newTargets;
            this.onlyVisible = onlyVisible;
            this.cancelAll();
         }

         if (!this.targetBlocks.isEmpty()) {
            HashSet<ChunkPos> flagged;
            synchronized (this.chunksToRescan) {
               flagged = new HashSet<>(this.chunksToRescan);
               this.chunksToRescan.clear();
            }

            for (ChunkPos cp : flagged) {
               Search.ChunkSearcher old = this.searchers.remove(cp);
               if (old != null) {
                  old.cancel();
               }
            }

            int renderDist = (Integer)mc.options.renderDistance().get();
            int keepDist = renderDist + 2;
            ChunkPos center = mc.player.chunkPosition();
            this.searchers.entrySet().removeIf(e -> {
               ChunkPos cpx = e.getKey();
               boolean outOfRange = Math.abs(cpx.x() - center.x()) > keepDist || Math.abs(cpx.z() - center.z()) > keepDist;
               boolean unloaded = !mc.level.hasChunk(cpx.x(), cpx.z());
               if (!outOfRange && !unloaded) {
                  return false;
               }

               e.getValue().cancel();
               return true;
            });

            for (int dx = -renderDist; dx <= renderDist; dx++) {
               for (int dz = -renderDist; dz <= renderDist; dz++) {
                  ChunkPos cp = new ChunkPos(center.x() + dx, center.z() + dz);
                  if (!this.searchers.containsKey(cp) && mc.level.hasChunk(cp.x(), cp.z())) {
                     ChunkAccess chunk = mc.level.getChunk(cp.x(), cp.z());
                     ChunkAccess chunkN = mc.level.hasChunk(cp.x(), cp.z() - 1) ? mc.level.getChunk(cp.x(), cp.z() - 1) : null;
                     ChunkAccess chunkS = mc.level.hasChunk(cp.x(), cp.z() + 1) ? mc.level.getChunk(cp.x(), cp.z() + 1) : null;
                     ChunkAccess chunkW = mc.level.hasChunk(cp.x() - 1, cp.z()) ? mc.level.getChunk(cp.x() - 1, cp.z()) : null;
                     ChunkAccess chunkE = mc.level.hasChunk(cp.x() + 1, cp.z()) ? mc.level.getChunk(cp.x() + 1, cp.z()) : null;
                     Search.ChunkSearcher searcher = new Search.ChunkSearcher(chunk, chunkN, chunkS, chunkW, chunkE, this.targetBlocks, onlyVisible);
                     this.searchers.put(cp, searcher);
                     searcher.start(SCAN_POOL);
                  }
               }
            }
         }
      }
   }

   private void onPacket(EventPacketReceive event) {
      ChunkPos cp = getAffectedChunk(event.getPacket());
      if (cp != null) {
         this.chunksToRescan.add(cp);
      }
   }

   private static ChunkPos getAffectedChunk(Packet<?> packet) {
      if (packet instanceof ClientboundLevelChunkWithLightPacket p) {
         return new ChunkPos(p.getX(), p.getZ());
      } else if (packet instanceof ClientboundBlockUpdatePacket p) {
         return ChunkPos.containing(p.getPos());
      } else if (packet instanceof ClientboundSectionBlocksUpdatePacket p) {
         ChunkPos[] found = new ChunkPos[]{null};
         p.runUpdates((pos, state) -> {
            if (found[0] == null) {
               found[0] = ChunkPos.containing(pos);
            }
         });
         return found[0];
      } else {
         return null;
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (!this.searchers.isEmpty()) {
         boolean showFill = this.getSetting("Show Fill", true);
         boolean showOutline = this.getSetting("Show Outline", true);
         int fillOpacity = this.getSetting("Fill Opacity", 80);
         int outlineOpacity = this.getSetting("Outline Opacity", 200);
         boolean showTracers = this.getSetting("Show Tracers", false);
         float tracerWidth = this.getSetting("Tracer Thickness", 2.0F);
         JsonObject colorsObj = ClarityConfig.getBlockColors("Search");
         Map<Integer, List<AABB>> byColor = new HashMap<>();
         Map<Integer, List<Vec3>> tracersByColor = showTracers ? new HashMap<>() : null;

         for (Search.ChunkSearcher searcher : this.searchers.values()) {
            for (Search.ChunkSearcher.Hit hit : searcher.getResults()) {
               int rgb = resolveColor(hit.block(), colorsObj);
               int fill = buildArgb(fillOpacity, rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
               byColor.computeIfAbsent(fill, k -> new java.util.ArrayList<>()).add(hit.box());
               if (showTracers && tracersByColor != null) {
                  int tracerColor = buildArgb(outlineOpacity, rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
                  AABB b = hit.box();
                  tracersByColor.computeIfAbsent(tracerColor, k -> new java.util.ArrayList<>()).add(new Vec3(b.minX + 0.5, b.minY + 0.5, b.minZ + 0.5));
               }
            }
         }

         for (Entry<Integer, List<AABB>> e : byColor.entrySet()) {
            int fill = e.getKey();
            int outline = withAlpha(fill, outlineOpacity);
            ClarityRenderUtils.drawBoxes(poseStack, e.getValue(), fill, outline, showFill, showOutline);
         }

         if (showTracers && tracersByColor != null) {
            for (Entry<Integer, List<Vec3>> e : tracersByColor.entrySet()) {
               ClarityRenderUtils.drawTracers(poseStack, partialTick, e.getValue(), e.getKey(), tracerWidth);
            }
         }
      }
   }

   private void cancelAll() {
      this.searchers.values().forEach(Search.ChunkSearcher::cancel);
      this.searchers.clear();
   }

   private void rescheduleAll() {
      this.cancelAll();
   }

   private Set<Block> resolveTargetBlocks() {
      try {
         JsonObject modObj = ClarityConfig.getCurrent().getAsJsonObject("Search");
         if (modObj == null) {
            return Collections.emptySet();
         }

         JsonObject values = modObj.getAsJsonObject("values");
         if (values == null) {
            return Collections.emptySet();
         }

         JsonElement el = values.get("Blocks");
         if (el != null && el.isJsonArray()) {
            Set<Block> result = new HashSet<>();

            for (JsonElement entry : el.getAsJsonArray()) {
               if (entry.isJsonPrimitive()) {
                  try {
                     String id = entry.getAsString();
                     if (!id.contains(":")) {
                        id = "minecraft:" + id;
                     }

                     Block b = (Block)BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
                     if (b != Blocks.AIR) {
                        result.add(b);
                     }
                  } catch (Exception var9) {
                  }
               }
            }

            return result;
         } else {
            return Collections.emptySet();
         }
      } catch (Exception e) {
         return Collections.emptySet();
      }
   }

   private static int resolveColor(Block block, JsonObject colorsObj) {
      String fullId = BuiltInRegistries.BLOCK.getKey(block).toString();
      String bareId = fullId.contains(":") ? fullId.split(":", 2)[1] : fullId;
      String hex = null;
      if (colorsObj.has(bareId)) {
         hex = colorsObj.get(bareId).getAsString();
      } else if (colorsObj.has(fullId)) {
         hex = colorsObj.get(fullId).getAsString();
      }

      if (hex == null) {
         return ThemeColors.isEnabled() ? 0xFF000000 | ThemeColors.getAccentRgb() : 16758062;
      } else {
         return ThemeColors.resolveArgb(hex, 16758062, false);
      }
   }

   private static int buildArgb(int a, int r, int g, int b) {
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   private static int withAlpha(int argb, int alpha) {
      return argb & 16777215 | (alpha & 0xFF) << 24;
   }

   public static Search getInstance() {
      return instance;
   }

   private static final class ChunkSearcher {
      private final ChunkAccess chunk;
      private final ChunkAccess chunkN;
      private final ChunkAccess chunkS;
      private final ChunkAccess chunkW;
      private final ChunkAccess chunkE;
      private final Set<Block> targetBlocks;
      private final boolean onlyVisible;
      private volatile List<Search.ChunkSearcher.Hit> results = Collections.emptyList();
      private CompletableFuture<Void> future = null;
      private volatile boolean cancelled = false;

      ChunkSearcher(ChunkAccess chunk, ChunkAccess chunkN, ChunkAccess chunkS, ChunkAccess chunkW, ChunkAccess chunkE, Set<Block> targetBlocks, boolean onlyVisible) {
         this.chunk = chunk;
         this.chunkN = chunkN;
         this.chunkS = chunkS;
         this.chunkW = chunkW;
         this.chunkE = chunkE;
         this.targetBlocks = targetBlocks;
         this.onlyVisible = onlyVisible;
      }

      void start(ExecutorService pool) {
         this.future = CompletableFuture.runAsync(this::scan, pool);
      }

      void cancel() {
         this.cancelled = true;
         if (this.future != null) {
            this.future.cancel(false);
         }
      }

      List<Search.ChunkSearcher.Hit> getResults() {
         return this.results;
      }

      private void scan() {
         List<Search.ChunkSearcher.Hit> found = new java.util.ArrayList<>();
         MutableBlockPos mpos = new MutableBlockPos();
         int minX = this.chunk.getPos().getMinBlockX();
         int minZ = this.chunk.getPos().getMinBlockZ();
         int minY = this.chunk.getMinY();
         int maxY = this.chunk.getMaxY();

         for (int x = minX; x < minX + 16 && !this.cancelled; x++) {
            for (int z = minZ; z < minZ + 16 && !this.cancelled; z++) {
               for (int y = minY; y < maxY && !this.cancelled; y++) {
                  mpos.set(x, y, z);
                  BlockState state = this.chunk.getBlockState(mpos);
                  if (this.targetBlocks.contains(state.getBlock()) && (!this.onlyVisible || this.isVisible(x, y, z, minX, minZ, minY, maxY))) {
                     found.add(new Search.ChunkSearcher.Hit(new AABB(x, y, z, x + 1, y + 1, z + 1), state.getBlock()));
                  }
               }
            }
         }

         if (!this.cancelled) {
            this.results = found;
         }
      }

      private boolean isVisible(int x, int y, int z, int minX, int minZ, int minY, int maxY) {
         if (y + 1 < maxY && isAir(this.chunk, x, y + 1, z)) {
            return true;
         }

         if (y - 1 >= minY && isAir(this.chunk, x, y - 1, z)) {
            return true;
         }

         if (y + 1 < maxY && y - 1 >= minY) {
            if (z - 1 >= minZ) {
               if (isAir(this.chunk, x, y, z - 1)) {
                  return true;
               }
            } else if (this.chunkN == null || isAir(this.chunkN, x, y, z - 1)) {
               return true;
            }

            if (z + 1 < minZ + 16) {
               if (isAir(this.chunk, x, y, z + 1)) {
                  return true;
               }
            } else if (this.chunkS == null || isAir(this.chunkS, x, y, z + 1)) {
               return true;
            }

            if (x - 1 >= minX) {
               if (isAir(this.chunk, x - 1, y, z)) {
                  return true;
               }
            } else if (this.chunkW == null || isAir(this.chunkW, x - 1, y, z)) {
               return true;
            }

            if (x + 1 < minX + 16) {
               if (isAir(this.chunk, x + 1, y, z)) {
                  return true;
               }
            } else if (this.chunkE == null || isAir(this.chunkE, x + 1, y, z)) {
               return true;
            }

            return false;
         } else {
            return true;
         }
      }

      private static boolean isAir(ChunkAccess c, int x, int y, int z) {
         try {
            return c.getBlockState(new BlockPos(x, y, z)).isAir();
         } catch (Exception e) {
            return true;
         }
      }

      record Hit(AABB box, Block block) {
      }
   }
}
