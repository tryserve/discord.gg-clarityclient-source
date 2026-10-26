package need.clarity.modules.utility;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.KillAura;
import need.clarity.modules.combat.Reach;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AutoWeb extends ModuleBase {
   public static AutoWeb INSTANCE;
   private static final double FACE_MARGIN = 0.05;
   private static final int STALL_TICKS = 6;
   private static final Field RIGHT_CLICK_DELAY_FIELD;
   private int ticks = 0;
   private int nextActionTick = 0;
   private int savedSlot = -1;
   private int lastSetSlot = -1;
   private int swapBackAtTick = -1;
   private AutoWeb.Job job = null;
   private int placeAtTick = 0;
   private int jobStartedTick = 0;
   private boolean rotating = false;
   private float rotYaw;
   private float rotPitch;

   public AutoWeb() {
      super("Auto Web", "Utility");
      INSTANCE = this;
      PerFrameEvents.register(this::onFrame);
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.restoreSlotNow(Minecraft.getInstance());
      this.stopRotating();
      this.reset();
   }

   private void reset() {
      this.nextActionTick = 0;
      this.swapBackAtTick = -1;
      this.savedSlot = -1;
      this.lastSetSlot = -1;
      this.job = null;
      this.rotating = false;
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
         if (mc.screen != null) {
            this.stopRotating();
            this.job = null;
         } else {
            if (this.savedSlot != -1 && this.lastSetSlot != -1 && InventoryHelper.getSelected(mc.player.getInventory()) != this.lastSetSlot) {
               this.savedSlot = -1;
               this.swapBackAtTick = -1;
            }

            int slot = findCobwebSlot(mc);
            LivingEntity target = slot < 0 ? null : this.findTarget(mc);
            List<AutoWeb.Job> jobs = target == null ? List.of() : this.buildJobs(mc, target);
            if (jobs.isEmpty()) {
               this.job = null;
               this.stopRotating();
               this.scheduleSwapBack();
               this.tickSwapBack(mc);
            } else {
               this.swapBackAtTick = -1;
               if (this.ticks >= this.nextActionTick) {
                  if (this.job != null) {
                     AutoWeb.Job fresh = null;

                     for (AutoWeb.Job j : jobs) {
                        if (j.pos().equals(this.job.pos())) {
                           fresh = j;
                           break;
                        }
                     }

                     this.job = fresh != null && this.ticks - this.jobStartedTick <= 6 ? fresh : null;
                  }

                  if (this.job == null) {
                     this.job = jobs.get(0);
                     this.jobStartedTick = this.ticks;
                     this.placeAtTick = this.ticks + (this.silent() ? 1 : 0);
                  }

                  if (!this.selectSlot(mc, slot)) {
                     this.job = null;
                  } else if (!this.silent()) {
                     this.stopRotating();
                     this.place(mc, this.job.hit());
                  } else {
                     boolean wasRotating = this.rotating;
                     float sentYaw = this.rotYaw;
                     float sentPitch = this.rotPitch;
                     this.rotateTo(this.job.yaw(), this.job.pitch());
                     if (wasRotating && this.ticks >= this.placeAtTick) {
                        BlockHitResult ray = rayFromRotation(mc, sentYaw, sentPitch);
                        if (ray != null && ray.getBlockPos().equals(this.job.hit().getBlockPos()) && ray.getDirection() == this.job.hit().getDirection()) {
                           this.place(mc, new BlockHitResult(ray.getLocation(), ray.getDirection(), ray.getBlockPos(), false));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void place(Minecraft mc, BlockHitResult hit) {
      setRightClickDelay(mc, 0);
      InteractionResult res = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
      if (res.consumesAction()) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      }

      setRightClickDelay(mc, 4);
      this.job = null;
      this.nextActionTick = this.ticks + Math.max(1, this.getSetting("Place Delay", 0));
   }

   private List<AutoWeb.Job> buildJobs(Minecraft mc, LivingEntity target) {
      if (this.getSetting("Only Falling", false) && target.getDeltaMovement().y > -0.1 && target.getY() - target.yo > -0.05) {
         return List.of();
      }

      List<BlockPos> spots = new ArrayList<>();
      AABB bb = target.getBoundingBox();
      BlockPos feet = BlockPos.containing((bb.minX + bb.maxX) * 0.5, bb.minY + 0.01, (bb.minZ + bb.maxZ) * 0.5);
      spots.add(feet);
      if (this.getSetting("Head", false)) {
         spots.add(feet.above());
      }

      List<AutoWeb.Job> jobs = new ArrayList<>();

      for (BlockPos pos : spots) {
         BlockState st = mc.level.getBlockState(pos);
         if (!st.is(Blocks.COBWEB) && st.canBeReplaced() && st.getFluidState().isEmpty() && !new AABB(pos).intersects(mc.player.getBoundingBox())) {
            AutoWeb.Job j = this.findPlacement(mc, pos);
            if (j != null) {
               jobs.add(j);
            }
         }
      }

      return jobs;
   }

   private AutoWeb.Job findPlacement(Minecraft mc, BlockPos pos) {
      double reach = Reach.vanillaBlockReach(mc.player);
      double reachSq = reach * reach;
      Vec3 eyes = mc.player.getEyePosition();
      AutoWeb.Job best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         BlockPos support = pos.relative(dir);
         BlockState s = mc.level.getBlockState(support);
         if (s.isSolid() && !s.canBeReplaced() && !s.hasBlockEntity() && s.isCollisionShapeFullBlock(mc.level, support)) {
            Direction face = dir.getOpposite();
            Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 centre = Vec3.atCenterOf(support).add(n.scale(0.5));
            if (!(eyes.subtract(centre).dot(n) < 0.05)) {
               for (Vec3 p : facePoints(centre, face)) {
                  double d = eyes.distanceToSqr(p);
                  if (!(d > reachSq)) {
                     Vec3 end = p.add(p.subtract(eyes).normalize().scale(0.05));
                     BlockHitResult ray = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
                     if (ray.getType() == Type.BLOCK && ray.getBlockPos().equals(support) && ray.getDirection() == face) {
                        float[] rot = lookAngles(eyes, p);
                        double score = d + this.angleCost(mc, rot) * 0.01;
                        if (score < bestScore) {
                           bestScore = score;
                           best = new AutoWeb.Job(pos, new BlockHitResult(p, face, support, false), rot[0], rot[1]);
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static List<Vec3> facePoints(Vec3 c, Direction face) {
      List<Vec3> pts = new ArrayList<>(9);
      pts.add(c);
      double[] o = new double[]{-0.35, 0.0, 0.35};

      for (double a : o) {
         for (double b : o) {
            if (a != 0.0 || b != 0.0) {
               pts.add(switch (face.getAxis()) {
                  case X -> c.add(0.0, a, b);
                  case Y -> c.add(a, 0.0, b);
                  case Z -> c.add(a, b, 0.0);
                  default -> throw new MatchException(null, null);
               });
            }
         }
      }

      return pts;
   }

   private float angleCost(Minecraft mc, float[] rot) {
      float y = this.rotating ? this.rotYaw : mc.player.getYRot();
      float p = this.rotating ? this.rotPitch : mc.player.getXRot();
      float dy = ((rot[0] - y) % 360.0F + 540.0F) % 360.0F - 180.0F;
      return Math.abs(dy) + Math.abs(rot[1] - p);
   }

   private LivingEntity findTarget(Minecraft mc) {
      double range = this.getSetting("Range", 5.0);
      double rangeSq = range * range;
      LivingEntity ka = KillAura.isActive() ? KillAura.getTarget() : null;
      if (valid(mc, ka, rangeSq)) {
         return ka;
      }

      LivingEntity aa = AimAssist.getCurrentTarget();
      if (valid(mc, aa, rangeSq)) {
         return aa;
      }

      List<LivingEntity> c = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (valid(mc, p, rangeSq)) {
            c.add(p);
         }
      }

      return c.isEmpty() ? null : Targets.getTarget(c, mc.player);
   }

   private static boolean valid(Minecraft mc, LivingEntity e, double rangeSq) {
      if (e == null || !e.isAlive() || e.isRemoved()) {
         return false;
      } else {
         return !Targets.isTargetAllowed(e, mc.player) ? false : mc.player.distanceToSqr(e) <= rangeSq;
      }
   }

   private boolean silent() {
      return this.getSetting("Silent Aim", true);
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

   private static float wrapDegrees(float angle) {
      angle %= 360.0F;
      if (angle >= 180.0F) {
         angle -= 360.0F;
      }

      if (angle < -180.0F) {
         angle += 360.0F;
      }

      return angle;
   }

   private void stopRotating() {
      if (this.rotating) {
         AimAssist.clearSilentRotation();
      }

      this.rotating = false;
   }

   private static int findCobwebSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();
      int held = InventoryHelper.getSelected(inv);
      if (inv.getItem(held).is(Items.COBWEB)) {
         return held;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && s.is(Items.COBWEB)) {
            return i;
         }
      }

      return -1;
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
            int lo = this.getSetting("Swap Back Delay_lo", 50);
            int hi = this.getSetting("Swap Back Delay_hi", 150);
            int ms = lo >= hi ? lo : lo + (int)(Math.random() * (hi - lo + 1));
            this.swapBackAtTick = this.ticks + Math.max(1, (ms + 49) / 50);
         }
      }
   }

   private void tickSwapBack(Minecraft mc) {
      if (this.swapBackAtTick != -1 && this.ticks >= this.swapBackAtTick) {
         this.restoreSlotNow(mc);
         this.swapBackAtTick = -1;
      }
   }

   private void restoreSlotNow(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      this.lastSetSlot = -1;
   }

   private static BlockHitResult rayFromRotation(Minecraft mc, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 end = eyes.add(AimAssist.getRotationVector(pitch, yaw).scale(Reach.vanillaBlockReach(mc.player)));
      BlockHitResult ray = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return ray.getType() == Type.BLOCK ? ray : null;
   }

   private static float[] lookAngles(Vec3 eyes, Vec3 point) {
      Vec3 d = point.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      return new float[]{(float)(Math.atan2(d.x * -1.0, d.z) * 180.0 / Math.PI), (float)(Math.atan2(d.y, h) * -1.0 * 180.0 / Math.PI)};
   }

   private static void setRightClickDelay(Minecraft mc, int value) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var3) {
         }
      }
   }

   static {
      Field f = null;

      try {
         f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
      } catch (NoSuchFieldException var2) {
      }

      RIGHT_CLICK_DELAY_FIELD = f;
   }

   private record Job(BlockPos pos, BlockHitResult hit, float yaw, float pitch) {
   }
}
