package need.clarity.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import need.clarity.PerFrameEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.render.TrapBoxRenderer;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.InventoryHelper;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AutoTrap extends ModuleBase {
   private static AutoTrap instance;
   private static final int LEGIT_STALL_TICKS = 6;
   private static final double FACE_MARGIN = 0.05;
   private final Map<BlockPos, Integer> pendingSupports = new HashMap<>();
   private final List<AutoTrap.RenderEntry> renderEntries = new CopyOnWriteArrayList<>();
   private int ticks = 0;
   private int nextActionTick = 0;
   private int savedSlot = -1;
   private int lastSetSlot = -1;
   private int swapBackAtTick = -1;
   private AutoTrap.PlacementJob legitJob = null;
   private int legitPlaceAtTick = 0;
   private int legitStartedTick = 0;
   private boolean rotating = false;
   private float rotYaw;
   private float rotPitch;
   private static final Field RIGHT_CLICK_DELAY_FIELD;
   private static final long RENDER_START_NS;

   public AutoTrap() {
      super("Auto Trap", "Combat");
      instance = this;
      WorldRenderEvent.register(this::onWorldRender);
      PerFrameEvents.register(this::onFrame);
   }

   public static AutoTrap getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      this.hardReset();
   }

   @Override
   public void onDisable() {
      this.restoreSlotNow(Minecraft.getInstance());
      this.stopRotating();
      this.hardReset();
   }

   private void hardReset() {
      this.nextActionTick = 0;
      this.swapBackAtTick = -1;
      this.legitJob = null;
      this.rotating = false;
      this.savedSlot = -1;
      this.lastSetSlot = -1;
      this.renderEntries.clear();
      this.pendingSupports.clear();
   }

   private void onFrame() {
      if (this.isEnabled() && this.rotating) {
         AimAssist.setSilentRotation(this.rotYaw, this.rotPitch);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         this.ticks++;
         if (!this.pendingSupports.isEmpty()) {
            int keep = this.confirmTicks() + 20;
            this.pendingSupports.values().removeIf(t -> this.ticks - t > keep);
         }

         if (mc.screen != null) {
            this.stopRotating();
            this.legitJob = null;
         } else {
            if (this.savedSlot != -1 && this.playerChangedSlot(mc)) {
               this.savedSlot = -1;
               this.swapBackAtTick = -1;
            }

            Player target = this.findTarget(mc);
            List<AutoTrap.PlacementJob> jobs = target == null ? List.of() : this.buildJobs(mc, target, Set.of());
            if (target != null && !jobs.isEmpty()) {
               this.swapBackAtTick = -1;
               if (this.ticks >= this.nextActionTick) {
                  if ("Legit".equals(this.getSetting("Mode", "Instant"))) {
                     this.tickLegit(mc, target, jobs);
                  } else {
                     this.tickInstant(mc, target, jobs);
                  }
               }
            } else {
               this.legitJob = null;
               this.stopRotating();
               this.scheduleSwapBack();
               this.tickSwapBack(mc);
            }
         }
      }
   }

   private void tickInstant(Minecraft mc, Player target, List<AutoTrap.PlacementJob> jobs) {
      this.stopRotating();
      int slot = this.acquireBlockSlot(mc);
      if (slot != -1) {
         int perTick = Math.max(1, this.getSetting("Blocks Per Tick", 4));
         Set<BlockPos> failed = new HashSet<>();
         int placed = 0;

         while (placed < perTick && !jobs.isEmpty()) {
            AutoTrap.PlacementJob job = jobs.get(0);
            if (!this.selectSlot(mc, slot)) {
               return;
            }

            if (this.executePlace(mc, job)) {
               placed++;
            } else {
               failed.add(job.pos());
            }

            jobs = this.buildJobs(mc, target, failed);
         }

         if (placed > 0) {
            this.nextActionTick = this.ticks + Math.max(1, msToTicks(this.getSetting("Place Delay", 0)));
         }
      }
   }

   private void tickLegit(Minecraft mc, Player target, List<AutoTrap.PlacementJob> jobs) {
      if (this.legitJob != null) {
         AutoTrap.PlacementJob fresh = null;

         for (AutoTrap.PlacementJob j : jobs) {
            if (j.pos().equals(this.legitJob.pos())) {
               fresh = j;
               break;
            }
         }

         if (fresh != null && this.ticks - this.legitStartedTick <= 6) {
            this.legitJob = fresh;
         } else {
            this.legitJob = null;
         }
      }

      if (this.legitJob == null) {
         this.legitJob = jobs.get(0);
         this.legitStartedTick = this.ticks;
         int swapDelay = msToTicks(randomInRange(this.getSetting("Swap Delay_lo", 0), this.getSetting("Swap Delay_hi", 0)));
         this.legitPlaceAtTick = this.ticks + Math.max(1, swapDelay);
      }

      int slot = this.acquireBlockSlot(mc);
      if (slot == -1) {
         this.legitJob = null;
         this.stopRotating();
      } else {
         boolean wasRotating = this.rotating;
         float sentYaw = this.rotYaw;
         float sentPitch = this.rotPitch;
         this.rotateTo(this.legitJob.yaw(), this.legitJob.pitch());
         if (this.selectSlot(mc, slot)) {
            if (wasRotating && this.ticks >= this.legitPlaceAtTick) {
               BlockHitResult hit = this.legitJob.hit();
               if (!this.getSetting("Piercing", false)) {
                  BlockHitResult ray = rayFromRotation(mc, sentYaw, sentPitch);
                  if (ray == null || !ray.getBlockPos().equals(hit.getBlockPos()) || ray.getDirection() != hit.getDirection()) {
                     return;
                  }

                  hit = new BlockHitResult(ray.getLocation(), ray.getDirection(), ray.getBlockPos(), false);
               }

               this.executePlace(mc, new AutoTrap.PlacementJob(this.legitJob.pos(), hit, this.legitJob.yaw(), this.legitJob.pitch()));
               this.legitJob = null;
               this.nextActionTick = this.ticks + Math.max(1, msToTicks(this.getSetting("Place Delay", 0)));
            }
         }
      }
   }

   private List<BlockPos> buildCage(Player target) {
      AABB bb = target.getBoundingBox();
      int feetY = (int)Math.floor(bb.minY + 0.01);
      int height = Math.max(1, (int)Math.ceil(bb.maxY - feetY - 0.01));
      int minX = (int)Math.floor(bb.minX + 0.001);
      int maxX = (int)Math.floor(bb.maxX - 0.001);
      int minZ = (int)Math.floor(bb.minZ + 0.001);
      int maxZ = (int)Math.floor(bb.maxZ - 0.001);
      Set<Long> columns = new HashSet<>();

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            columns.add(colKey(x, z));
         }
      }

      LinkedHashSet<BlockPos> cage = new LinkedHashSet<>();
      if (this.getSetting("Floor", true)) {
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               cage.add(new BlockPos(x, feetY - 1, z));
            }
         }
      }

      for (int dy = 0; dy < height; dy++) {
         int y = feetY + dy;

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               for (Direction d : Plane.HORIZONTAL) {
                  int nx = x + d.getStepX();
                  int nz = z + d.getStepZ();
                  if (!columns.contains(colKey(nx, nz))) {
                     cage.add(new BlockPos(nx, y, nz));
                  }
               }
            }
         }
      }

      if (this.getSetting("Roof", true)) {
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               cage.add(new BlockPos(x, feetY + height, z));
            }
         }
      }

      return new ArrayList<>(cage);
   }

   private List<AutoTrap.PlacementJob> buildJobs(Minecraft mc, Player target, Set<BlockPos> skip) {
      List<AutoTrap.PlacementJob> jobs = new ArrayList<>();
      boolean piercing = this.getSetting("Piercing", false);
      List<BlockPos> cage = this.buildCage(target);
      AABB targetBox = target.getBoundingBox();

      for (BlockPos pos : cage) {
         if (!skip.contains(pos) && mc.level.getBlockState(pos).canBeReplaced() && !blockedByEntity(mc, pos)) {
            AutoTrap.PlacementJob job = this.findPlacement(mc, pos, piercing);
            if (job != null) {
               jobs.add(job);
            }
         }
      }

      if (jobs.isEmpty() && this.getSetting("Roof", true)) {
         int roofY = cage.get(cage.size() - 1).getY();

         for (BlockPos roof : cage) {
            if (roof.getY() == roofY && mc.level.getBlockState(roof).canBeReplaced()) {
               for (Direction d : Plane.HORIZONTAL) {
                  BlockPos helper = roof.relative(d);
                  if (!skip.contains(helper)
                     && helper.getY() == roofY
                     && !new AABB(helper).intersects(targetBox)
                     && mc.level.getBlockState(helper).canBeReplaced()
                     && mc.level.getBlockState(helper.below()).isSolid()
                     && !blockedByEntity(mc, helper)) {
                     AutoTrap.PlacementJob job = this.findPlacement(mc, helper, piercing);
                     if (job != null) {
                        jobs.add(job);
                     }
                  }
               }
            }
         }

         jobs.sort(Comparator.comparingDouble(j -> mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(j.pos()))));
      }

      return jobs;
   }

   private static long colKey(int x, int z) {
      return (long)x << 32 ^ z & 4294967295L;
   }

   private static boolean blockedByEntity(Minecraft mc, BlockPos pos) {
      AABB box = new AABB(pos);

      for (Entity e : mc.level.getEntities((Entity)null, box)) {
         if (!e.isSpectator() && e.isAlive() && e.blocksBuilding) {
            return true;
         }
      }

      return false;
   }

   private AutoTrap.PlacementJob findPlacement(Minecraft mc, BlockPos pos, boolean piercing) {
      double reach = Reach.vanillaBlockReach(mc.player);
      double reachSq = reach * reach;
      Vec3 eyes = mc.player.getEyePosition();
      AutoTrap.PlacementJob best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         BlockPos support = pos.relative(dir);
         BlockState supportState = mc.level.getBlockState(support);
         if (supportState.isSolid() && !supportState.canBeReplaced() && !isInteractable(supportState) && supportState.isCollisionShapeFullBlock(mc.level, support) && this.isConfirmed(support)) {
            Direction face = dir.getOpposite();
            Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 faceCentre = Vec3.atCenterOf(support).add(n.scale(0.5));
            if (!(eyes.subtract(faceCentre).dot(n) < 0.05)) {
               for (Vec3 point : facePoints(faceCentre, face)) {
                  double distSq = eyes.distanceToSqr(point);
                  if (!(distSq > reachSq)) {
                     if (!piercing) {
                        Vec3 end = point.add(point.subtract(eyes).normalize().scale(0.05));
                        BlockHitResult ray = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
                        if (ray.getType() != Type.BLOCK || !ray.getBlockPos().equals(support) || ray.getDirection() != face) {
                           continue;
                        }
                     }

                     float[] rot = lookAngles(eyes, point);
                     double score = distSq + this.rotationCost(mc, rot) * 0.01;
                     if (score < bestScore) {
                        bestScore = score;
                        best = new AutoTrap.PlacementJob(pos, new BlockHitResult(point, face, support, false), rot[0], rot[1]);
                     }

                     if (piercing) {
                        break;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static List<Vec3> facePoints(Vec3 centre, Direction face) {
      List<Vec3> pts = new ArrayList<>(10);
      pts.add(centre);
      Axis axis = face.getAxis();
      double[] offs = new double[]{-0.35, 0.0, 0.35};

      for (double a : offs) {
         for (double b : offs) {
            if (a != 0.0 || b != 0.0) {
               pts.add(switch (axis) {
                  case X -> centre.add(0.0, a, b);
                  case Y -> centre.add(a, 0.0, b);
                  case Z -> centre.add(a, b, 0.0);
                  default -> throw new MatchException(null, null);
               });
            }
         }
      }

      return pts;
   }

   private float rotationCost(Minecraft mc, float[] rot) {
      float yaw = this.rotating ? this.rotYaw : mc.player.getYRot();
      float pitch = this.rotating ? this.rotPitch : mc.player.getXRot();
      return Math.abs(wrapDegrees(rot[0] - yaw)) + Math.abs(rot[1] - pitch);
   }

   private static boolean isInteractable(BlockState s) {
      if (s.hasBlockEntity()) {
         return true;
      }

      net.minecraft.world.level.block.Block b = s.getBlock();
      return b instanceof CraftingTableBlock
         || b instanceof DoorBlock
         || b instanceof TrapDoorBlock
         || b instanceof FenceGateBlock
         || b instanceof ButtonBlock
         || b instanceof LeverBlock
         || b instanceof AnvilBlock
         || b == Blocks.NOTE_BLOCK
         || b == Blocks.RESPAWN_ANCHOR;
   }

   private static BlockHitResult rayFromRotation(Minecraft mc, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 dir = AimAssist.getRotationVector(pitch, yaw);
      Vec3 end = eyes.add(dir.scale(Reach.vanillaBlockReach(mc.player)));
      BlockHitResult ray = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return ray.getType() == Type.BLOCK ? ray : null;
   }

   private boolean executePlace(Minecraft mc, AutoTrap.PlacementJob job) {
      InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, job.hit());
      if (!result.consumesAction()) {
         return false;
      }

      mc.player.swing(InteractionHand.MAIN_HAND);
      this.setRightClickDelay(mc, 4);
      this.pendingSupports.put(job.pos().immutable(), this.ticks);
      if (this.getSetting("Render", false)) {
         this.renderEntries.add(new AutoTrap.RenderEntry(job.pos(), System.currentTimeMillis(), (float)Math.random()));
      }

      return true;
   }

   private boolean isConfirmed(BlockPos pos) {
      Integer at = this.pendingSupports.get(pos);
      return at == null ? true : this.ticks - at >= this.confirmTicks();
   }

   private int confirmTicks() {
      int ping = 0;

      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
               ping = info.getLatency();
            }
         }
      } catch (Throwable var4) {
      }

      return Math.max(2, (ping + 49) / 50 + 1);
   }

   private void rotateTo(float yaw, float pitch) {
      if (this.rotating) {
         this.rotYaw = this.rotYaw + wrapDegrees(yaw - this.rotYaw);
      } else {
         Minecraft mc = Minecraft.getInstance();
         float ref = mc.player != null ? mc.player.getYRot() : yaw;
         this.rotYaw = ref + wrapDegrees(yaw - ref);
      }

      this.rotPitch = Math.max(-90.0F, Math.min(90.0F, pitch));
      this.rotating = true;
      AimAssist.setSilentRotation(this.rotYaw, this.rotPitch);
   }

   private void stopRotating() {
      if (this.rotating) {
         AimAssist.clearSilentRotation();
      }

      this.rotating = false;
   }

   private int acquireBlockSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();
      boolean useWhitelist = this.getSetting("Block Whitelist", false);
      Set<String> whitelist = useWhitelist ? this.buildWhitelistSet() : Collections.emptySet();
      int held = InventoryHelper.getSelected(inv);
      if (isUsableBlock(inv.getItem(held), useWhitelist, whitelist)) {
         return held;
      }

      if (!this.getSetting("Swap", true)) {
         return -1;
      }

      int fallback = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = inv.getItem(i);
         if (isUsableBlock(stack, useWhitelist, whitelist)) {
            net.minecraft.world.level.block.Block b = ((BlockItem)stack.getItem()).getBlock();
            if (b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN) {
               return i;
            }

            if (fallback == -1) {
               fallback = i;
            }
         }
      }

      return fallback;
   }

   private static boolean isUsableBlock(ItemStack stack, boolean useWhitelist, Set<String> whitelist) {
      if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
         net.minecraft.world.level.block.Block var6 = bi.getBlock();
         if (useWhitelist && !whitelist.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return whitelist.contains(id);
         }

         if (var6 instanceof FallingBlock) {
            return false;
         }

         BlockState s = var6.defaultBlockState();
         return s.isSolid() && !isInteractable(s);
      } else {
         return false;
      }
   }

   private boolean selectSlot(Minecraft mc, int slot) {
      int cur = InventoryHelper.getSelected(mc.player.getInventory());
      if (cur == slot) {
         this.lastSetSlot = slot;
         return true;
      }

      if (!this.getSetting("Swap", true)) {
         return false;
      }

      if (this.savedSlot == -1) {
         this.savedSlot = cur;
      }

      InventoryHelper.setSelected(mc.player.getInventory(), slot);
      this.lastSetSlot = slot;
      return true;
   }

   private void scheduleSwapBack() {
      if (this.savedSlot != -1 && this.swapBackAtTick == -1) {
         if (!this.getSetting("Swap Back", true)) {
            this.savedSlot = -1;
            this.lastSetSlot = -1;
         } else {
            int delay = msToTicks(randomInRange(this.getSetting("Swap Back Delay_lo", 50), this.getSetting("Swap Back Delay_hi", 150)));
            this.swapBackAtTick = this.ticks + Math.max(1, delay);
         }
      }
   }

   private void tickSwapBack(Minecraft mc) {
      if (this.swapBackAtTick != -1 && this.ticks >= this.swapBackAtTick) {
         this.restoreSlotNow(mc);
         this.swapBackAtTick = -1;
      }
   }

   private boolean playerChangedSlot(Minecraft mc) {
      return this.lastSetSlot != -1 && mc.player != null ? InventoryHelper.getSelected(mc.player.getInventory()) != this.lastSetSlot : false;
   }

   private void restoreSlotNow(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      this.lastSetSlot = -1;
   }

   private Set<String> buildWhitelistSet() {
      List<?> raw = this.getSetting("Whitelist Blocks", new ArrayList());
      Set<String> set = new HashSet<>();

      for (Object entry : raw) {
         if (entry instanceof String s) {
            set.add(s.contains(":") ? s.substring(s.indexOf(58) + 1) : s);
         } else if (entry instanceof Map<?, ?> m) {
            Object id = m.get("id");
            if (id != null) {
               String s = id.toString();
               set.add(s.contains(":") ? s.substring(s.indexOf(58) + 1) : s);
            }
         }
      }

      return set;
   }

   private Player findTarget(Minecraft mc) {
      double range = this.getSetting("Range", 5.0);
      double rangeSq = range * range;
      List<Player> candidates = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (Targets.isTargetAllowed(p, mc.player) && !(mc.player.distanceToSqr(p) > rangeSq)) {
            candidates.add(p);
         }
      }

      if (candidates.isEmpty()) {
         return null;
      } else {
         return Targets.getTarget(candidates, mc.player) instanceof Player p ? p : candidates.get(0);
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled() && this.getSetting("Render", false)) {
         long now = System.currentTimeMillis();
         long fadeMs = Math.max(100, this.getSetting("Fade Time", 750));
         this.renderEntries.removeIf(e -> now - e.placedAtMs() > fadeMs);
         if (!this.renderEntries.isEmpty()) {
            int rgb1 = parseRgb(this.getSetting("Render Color", "#3a7fcc"));
            int rgb2 = parseRgb(this.getSetting("Second Color", "#9b6bff"));
            float fillOp = clamp01(this.getSetting("Fill Opacity", 0.35F));
            float lineOp = clamp01(this.getSetting("Outline Opacity", 0.9F));
            boolean grow = this.getSetting("Grow Animation", true);
            boolean walls = this.getSetting("Through Walls", false);
            int style = TrapBoxRenderer.style(this.getSetting("Render Mode", "Glow Edges"));
            if (style < 0) {
               for (AutoTrap.RenderEntry entry : this.renderEntries) {
                  float frac = 1.0F - (float)(now - entry.placedAtMs()) / (float)fadeMs;
                  if (!(frac <= 0.0F)) {
                     int alpha = Math.round(frac * fillOp * 255.0F) & 0xFF;
                     int argb = alpha << 24 | rgb1 & 16777215;
                     ClarityRenderUtils.drawBoxes(poseStack, List.of(entryBox(entry, now, fadeMs, grow)), argb, 0, true, false);
                  }
               }
            } else {
               List<TrapBoxRenderer.Box> boxes = new ArrayList<>();

               for (AutoTrap.RenderEntry entry : this.renderEntries) {
                  float frac = 1.0F - (float)(now - entry.placedAtMs()) / (float)fadeMs;
                  if (!(frac <= 0.0F)) {
                     float fade = frac < 0.35F ? frac / 0.35F : 1.0F;
                     fade = fade * fade * (3.0F - 2.0F * fade);
                     boxes.add(new TrapBoxRenderer.Box(entryBox(entry, now, fadeMs, grow), fade, entry.seed()));
                  }
               }

               float time = (float)((System.nanoTime() - RENDER_START_NS) / 1.0E9 % 3600.0);
               TrapBoxRenderer.render(poseStack, boxes, style, rgb1 & 16777215, rgb2 & 16777215, fillOp, lineOp, time, walls);
            }
         }
      }
   }

   private static AABB entryBox(AutoTrap.RenderEntry e, long now, long fadeMs, boolean grow) {
      BlockPos p = e.pos();
      double s = 1.0;
      if (grow) {
         double t = Math.min(1.0, (now - e.placedAtMs()) / 150.0);
         double k = 1.0 - Math.pow(1.0 - t, 3.0);
         s = 0.55 + 0.45 * k;
      }

      double h = 0.5 * s;
      return new AABB(p.getX() + 0.5 - h, p.getY() + 0.5 - h, p.getZ() + 0.5 - h, p.getX() + 0.5 + h, p.getY() + 0.5 + h, p.getZ() + 0.5 + h);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float[] lookAngles(Vec3 eyes, Vec3 target) {
      Vec3 d = target.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      float yaw = (float)(Math.atan2(d.x * -1.0, d.z) * 180.0 / Math.PI);
      float pitch = (float)(Math.atan2(d.y, h) * -1.0 * 180.0 / Math.PI);
      return new float[]{yaw, pitch};
   }

   private static float wrapDegrees(float a) {
      a %= 360.0F;
      if (a >= 180.0F) {
         a -= 360.0F;
      }

      if (a < -180.0F) {
         a += 360.0F;
      }

      return a;
   }

   private void setRightClickDelay(Minecraft mc, int value) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var4) {
         }
      }
   }

   private static int randomInRange(int lo, int hi) {
      return lo >= hi ? lo : lo + (int)(Math.random() * (hi - lo + 1));
   }

   private static int msToTicks(int ms) {
      return Math.max(0, (ms + 49) / 50);
   }

   private static int parseRgb(String hex) {
      return ThemeColors.resolveRgb(hex, 3833804, false);
   }

   static {
      Field f = null;

      try {
         f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
      } catch (NoSuchFieldException var2) {
      }

      RIGHT_CLICK_DELAY_FIELD = f;
      RENDER_START_NS = System.nanoTime();
   }

   private record PlacementJob(BlockPos pos, BlockHitResult hit, float yaw, float pitch) {
   }

   private record RenderEntry(BlockPos pos, long placedAtMs, float seed) {
   }
}
