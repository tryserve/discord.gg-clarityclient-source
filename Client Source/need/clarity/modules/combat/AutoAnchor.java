package need.clarity.modules.combat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.util.AccessorCompat;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public class AutoAnchor extends ModuleBase {
   private static final int VANILLA_RMB_COOLDOWN = 4;
   private static final double FACE_MARGIN = 0.05;
   private static final int STAGE_TIMEOUT_TICKS = 20;
   private static final float ANCHOR_EXPLOSION_POWER = 5.0F;
   private static final Field RIGHT_CLICK_DELAY_FIELD = resolveRightClickDelayField();
   private AutoAnchor.Stage stage = AutoAnchor.Stage.IDLE;
   private boolean silentSeq = false;
   private BlockPos anchorPos = null;
   private BlockPos shieldPos = null;
   private int savedSlot = -1;
   private int ticks = 0;
   private int stageStart = 0;
   private int chargedAt = -1;
   private boolean bindWasDown = false;
   private boolean aiming = false;
   private boolean aimSilent = true;
   private float aimYaw;
   private float aimPitch;

   private static Field resolveRightClickDelayField() {
      try {
         Field f = Minecraft.class.getDeclaredField("rightClickDelay");
         f.setAccessible(true);
         return f;
      } catch (NoSuchFieldException e) {
         return null;
      }
   }

   public AutoAnchor() {
      super("Auto Anchor", "Combat");
      PerFrameEvents.register(this::onFrame);
   }

   private void onFrame() {
      if (this.isEnabled() && this.aiming && this.aimSilent) {
         AimAssist.setSilentRotation(this.aimYaw, this.aimPitch);
      }
   }

   @Override
   public void onDisable() {
      this.restoreSlotNow(Minecraft.getInstance());
      this.stopAim();
      this.resetState();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         this.ticks++;
         if (mc.screen == null) {
            if ("Silent".equals(this.getSetting("Aim Mode", "None"))) {
               this.tickSilentMode(mc);
            } else {
               this.tickManualMode(mc);
            }
         }
      }
   }

   private void tickManualMode(Minecraft mc) {
      boolean rmb = mc.options.keyUse.isDown();
      if (this.stage == AutoAnchor.Stage.DONE) {
         this.suppressVanillaRmb(mc);
         if (!rmb) {
            this.stopAim();
            this.resetState();
         }
      } else if (!rmb) {
         if (this.stage != AutoAnchor.Stage.IDLE) {
            this.abort(mc);
         }
      } else {
         if (this.stage == AutoAnchor.Stage.IDLE) {
            BlockHitResult hit = crosshairBlock(mc);
            if (hit == null || !isAnchor(mc, hit.getBlockPos()) || charge(mc, hit.getBlockPos()) != 0) {
               return;
            }

            if (findSlot(mc, Items.GLOWSTONE) == -1) {
               return;
            }

            this.silentSeq = false;
            this.anchorPos = hit.getBlockPos();
            this.setStage(AutoAnchor.Stage.CHARGE);
         }

         this.suppressVanillaRmb(mc);
         this.runStage(mc);
      }
   }

   private void tickSilentMode(Minecraft mc) {
      boolean bindDown = isBindPressed(mc, this.getSetting("Silent Bind", "None"));
      boolean pressed = bindDown && !this.bindWasDown;
      this.bindWasDown = bindDown;
      if (this.stage == AutoAnchor.Stage.DONE) {
         this.stopAim();
         this.resetState();
      } else if (this.stage != AutoAnchor.Stage.IDLE || pressed && this.startSilentSequence(mc)) {
         this.runStage(mc);
      }
   }

   private boolean startSilentSequence(Minecraft mc) {
      LivingEntity target = this.findTarget(mc);
      if (target == null) {
         return false;
      }

      BlockPos best = this.findBestAnchorPos(mc, target);
      if (best == null) {
         return false;
      }

      this.silentSeq = true;
      this.anchorPos = best;
      this.setStage(isAnchor(mc, best) ? AutoAnchor.Stage.CHARGE : AutoAnchor.Stage.PLACE_ANCHOR);
      return true;
   }

   private void runStage(Minecraft mc) {
      if (this.anchorPos == null) {
         this.abort(mc);
      } else {
         int timeout = 20 + (this.stage == AutoAnchor.Stage.EXPLODE ? Math.max(0, this.getSetting("Explode Delay", 0)) : 0);
         if (this.ticks - this.stageStart > timeout) {
            this.abort(mc);
         } else {
            switch (this.stage) {
               case PLACE_ANCHOR:
                  this.tickPlaceAnchor(mc);
                  break;
               case CHARGE:
                  this.tickCharge(mc);
                  break;
               case SHIELD:
                  this.tickShield(mc);
                  break;
               case EXPLODE:
                  this.tickExplode(mc);
            }
         }
      }
   }

   private void tickPlaceAnchor(Minecraft mc) {
      if (isAnchor(mc, this.anchorPos)) {
         this.setStage(AutoAnchor.Stage.CHARGE);
      } else if (!mc.level.getBlockState(this.anchorPos).canBeReplaced()) {
         this.abort(mc);
      } else {
         int slot = findSlot(mc, Items.RESPAWN_ANCHOR);
         if (slot == -1) {
            this.abort(mc);
         } else {
            AutoAnchor.Click c = this.findPlacement(mc, this.anchorPos, null);
            if (c == null) {
               this.abort(mc);
            } else {
               this.swapTo(mc, slot);
               BlockHitResult hit = this.aimAndRay(mc, c, true);
               if (hit != null && matches(hit, c.hit())) {
                  if (this.useItemOn(mc, hit).consumesAction()) {
                     this.setStage(AutoAnchor.Stage.CHARGE);
                  }
               }
            }
         }
      }
   }

   private void tickCharge(Minecraft mc) {
      if (!isAnchor(mc, this.anchorPos)) {
         this.abort(mc);
      } else if (charge(mc, this.anchorPos) > 0) {
         this.onCharged(mc);
      } else {
         int slot = findSlot(mc, Items.GLOWSTONE);
         if (slot == -1) {
            this.abort(mc);
         } else {
            this.swapTo(mc, slot);
            if (mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
               BlockHitResult hit;
               if (!this.silentSeq && !this.aiming) {
                  hit = crosshairBlock(mc);
                  if (hit == null || !hit.getBlockPos().equals(this.anchorPos)) {
                     return;
                  }
               } else {
                  AutoAnchor.Click c = this.findAnchorClick(mc, this.anchorPos);
                  if (c == null) {
                     this.abort(mc);
                     return;
                  }

                  hit = this.aimAndRay(mc, c, true);
                  if (hit == null || !hit.getBlockPos().equals(this.anchorPos)) {
                     return;
                  }
               }

               if (this.useItemOn(mc, hit).consumesAction()) {
                  this.onCharged(mc);
               }
            }
         }
      }
   }

   private void onCharged(Minecraft mc) {
      this.chargedAt = this.ticks;
      if ("Safe".equals(this.getSetting("Anchor Mode", "Normal")) && this.pickShield(mc)) {
         this.setStage(AutoAnchor.Stage.SHIELD);
      } else {
         this.setStage(AutoAnchor.Stage.EXPLODE);
      }
   }

   private void tickShield(Minecraft mc) {
      if (this.shieldPos != null && mc.level.getBlockState(this.shieldPos).canBeReplaced()) {
         int slot = findSlot(mc, Items.GLOWSTONE);
         if (slot == -1) {
            this.setStage(AutoAnchor.Stage.EXPLODE);
         } else {
            AutoAnchor.Click c = this.findPlacement(mc, this.shieldPos, this.anchorPos);
            if (c == null) {
               this.setStage(AutoAnchor.Stage.EXPLODE);
            } else {
               this.swapTo(mc, slot);
               BlockHitResult hit = this.aimAndRay(mc, c, this.shieldSilent());
               if (hit != null && matches(hit, c.hit())) {
                  if (this.useItemOn(mc, hit).consumesAction()) {
                     this.setStage(AutoAnchor.Stage.EXPLODE);
                  }
               }
            }
         }
      } else {
         this.setStage(AutoAnchor.Stage.EXPLODE);
      }
   }

   private void tickExplode(Minecraft mc) {
      if (isAnchor(mc, this.anchorPos) && charge(mc, this.anchorPos) != 0) {
         if (this.ticks - this.chargedAt < Math.max(0, this.getSetting("Explode Delay", 0))) {
            if (this.aiming) {
               this.aimAndRay(mc, this.findAnchorClickOrKeep(mc), this.aimSilent);
            }
         } else {
            this.restoreSlotNow(mc);
            if (mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
               int other = findNonGlowstoneSlot(mc);
               if (other == -1) {
                  this.abort(mc);
                  return;
               }

               this.swapTo(mc, other);
            }

            BlockHitResult hit;
            if (!this.silentSeq && !this.aiming) {
               hit = crosshairBlock(mc);
               if (hit == null || !hit.getBlockPos().equals(this.anchorPos)) {
                  return;
               }
            } else {
               AutoAnchor.Click c = this.findAnchorClick(mc, this.anchorPos);
               if (c == null) {
                  this.abort(mc);
                  return;
               }

               hit = this.aimAndRay(mc, c, this.silentSeq || this.aimSilent);
               if (hit == null || !hit.getBlockPos().equals(this.anchorPos)) {
                  return;
               }
            }

            if (this.useItemOn(mc, hit).consumesAction()) {
               this.finish(mc);
            }
         }
      } else {
         this.finish(mc);
      }
   }

   private AutoAnchor.Click findAnchorClickOrKeep(Minecraft mc) {
      AutoAnchor.Click c = this.findAnchorClick(mc, this.anchorPos);
      return c != null ? c : new AutoAnchor.Click(null, this.aimYaw, this.aimPitch);
   }

   private void finish(Minecraft mc) {
      this.restoreSlotNow(mc);
      this.stopAim();
      this.stage = AutoAnchor.Stage.DONE;
   }

   private void abort(Minecraft mc) {
      this.restoreSlotNow(mc);
      this.stopAim();
      this.resetState();
   }

   private void setStage(AutoAnchor.Stage s) {
      this.stage = s;
      this.stageStart = this.ticks;
   }

   private BlockHitResult aimAndRay(Minecraft mc, AutoAnchor.Click c, boolean silent) {
      float[] sent = null;
      if (this.aiming && this.aimSilent == silent) {
         sent = silent ? new float[]{this.aimYaw, this.aimPitch} : new float[]{mc.player.getYRot(), mc.player.getXRot()};
      }

      float fromYaw;
      float fromPitch;
      if (this.aiming && this.aimSilent == silent) {
         fromYaw = this.aimYaw;
         fromPitch = this.aimPitch;
      } else if (silent && KillAura.isActive()) {
         fromYaw = AimAssist.getSilentServerYaw();
         fromPitch = AimAssist.getSilentServerPitch();
      } else {
         fromYaw = mc.player.getYRot();
         fromPitch = mc.player.getXRot();
      }

      float speed = Math.max(1.0F, this.getSetting("Rotation Speed", 180.0F));
      float newYaw = step(fromYaw, c.yaw(), speed);
      float newPitch = Math.max(-90.0F, Math.min(90.0F, step(fromPitch, c.pitch(), speed)));
      if (this.aiming && this.aimSilent && !silent) {
         AimAssist.clearSilentRotation();
      }

      this.aiming = true;
      this.aimSilent = silent;
      this.aimYaw = newYaw;
      this.aimPitch = newPitch;
      if (silent) {
         AimAssist.setSilentRotation(newYaw, newPitch);
      } else {
         mc.player.setYRot(newYaw);
         mc.player.setXRot(newPitch);
      }

      return sent == null ? null : rayFrom(mc, sent[0], sent[1]);
   }

   private void stopAim() {
      if (this.aiming && this.aimSilent) {
         AimAssist.clearSilentRotation();
      }

      this.aiming = false;
   }

   private boolean shieldSilent() {
      return this.silentSeq || "Silent".equals(this.getSetting("Safe Aim Mode", "Silent"));
   }

   private static float step(float from, float to, float maxStep) {
      float d = wrapDegrees(to - from);
      return Math.abs(d) <= maxStep ? from + d : from + Math.signum(d) * maxStep;
   }

   private static boolean matches(BlockHitResult a, BlockHitResult want) {
      return a.getBlockPos().equals(want.getBlockPos()) && a.getDirection() == want.getDirection();
   }

   private static BlockHitResult rayFrom(Minecraft mc, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 end = eyes.add(AimAssist.getRotationVector(pitch, yaw).scale(Reach.vanillaBlockReach(mc.player)));
      BlockHitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return r.getType() == Type.BLOCK ? r : null;
   }

   private AutoAnchor.Click findPlacement(Minecraft mc, BlockPos pos, BlockPos avoidSupport) {
      Vec3 eyes = mc.player.getEyePosition();
      double reachSq = sq(Reach.vanillaBlockReach(mc.player));
      AutoAnchor.Click best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         BlockPos support = pos.relative(dir);
         if (!support.equals(avoidSupport)) {
            BlockState s = mc.level.getBlockState(support);
            if (s.isSolid() && !s.canBeReplaced() && !s.hasBlockEntity() && !s.is(Blocks.RESPAWN_ANCHOR) && s.isCollisionShapeFullBlock(mc.level, support)) {
               Direction face = dir.getOpposite();
               AutoAnchor.Click c = bestVisiblePoint(mc, eyes, reachSq, support, face);
               if (c != null) {
                  double score = eyes.distanceToSqr(c.hit().getLocation()) + this.angleCost(mc, c) * 0.01;
                  if (score < bestScore) {
                     bestScore = score;
                     best = c;
                  }
               }
            }
         }
      }

      return best;
   }

   private AutoAnchor.Click findAnchorClick(Minecraft mc, BlockPos anchor) {
      Vec3 eyes = mc.player.getEyePosition();
      double reachSq = sq(Reach.vanillaBlockReach(mc.player));
      AutoAnchor.Click best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction face : Direction.values()) {
         AutoAnchor.Click c = bestVisiblePoint(mc, eyes, reachSq, anchor, face);
         if (c != null) {
            double score = this.angleCost(mc, c);
            if (score < bestScore) {
               bestScore = score;
               best = c;
            }
         }
      }

      return best;
   }

   private static AutoAnchor.Click bestVisiblePoint(Minecraft mc, Vec3 eyes, double reachSq, BlockPos block, Direction face) {
      Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
      Vec3 centre = Vec3.atCenterOf(block).add(n.scale(0.5));
      if (eyes.subtract(centre).dot(n) < 0.05) {
         return null;
      }

      for (Vec3 p : facePoints(centre, face)) {
         if (!(eyes.distanceToSqr(p) > reachSq)) {
            Vec3 end = p.add(p.subtract(eyes).normalize().scale(0.05));
            BlockHitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
            if (r.getType() == Type.BLOCK && r.getBlockPos().equals(block) && r.getDirection() == face) {
               float[] rot = lookAngles(eyes, p);
               return new AutoAnchor.Click(new BlockHitResult(p, face, block, false), rot[0], rot[1]);
            }
         }
      }

      return null;
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

   private float angleCost(Minecraft mc, AutoAnchor.Click c) {
      float y = this.aiming ? this.aimYaw : mc.player.getYRot();
      float p = this.aiming ? this.aimPitch : mc.player.getXRot();
      return Math.abs(wrapDegrees(c.yaw() - y)) + Math.abs(c.pitch() - p);
   }

   private boolean pickShield(Minecraft mc) {
      this.shieldPos = null;
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 centre = Vec3.atCenterOf(this.anchorPos);
      AABB me = mc.player.getBoundingBox();
      double best = Double.MAX_VALUE;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dy != 0 || dz != 0) {
                  BlockPos cand = this.anchorPos.offset(dx, dy, dz);
                  if (mc.level.getBlockState(cand).canBeReplaced() && !me.intersects(new AABB(cand)) && !new AABB(cand).clip(centre, eyes).isEmpty()) {
                     AutoAnchor.Click c = this.findPlacement(mc, cand, this.anchorPos);
                     if (c != null) {
                        double score = this.angleCost(mc, c);
                        if (score < best) {
                           best = score;
                           this.shieldPos = cand;
                        }
                     }
                  }
               }
            }
         }
      }

      return this.shieldPos != null;
   }

   private LivingEntity findTarget(Minecraft mc) {
      LivingEntity ka = KillAura.isActive() ? KillAura.getTarget() : null;
      if (ka != null && ka.isAlive() && mc.player.distanceToSqr(ka) < 64.0) {
         return ka;
      }

      List<LivingEntity> c = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (Targets.isTargetAllowed(p, mc.player) && mc.player.distanceToSqr(p) < 64.0) {
            c.add(p);
         }
      }

      return c.isEmpty() ? null : Targets.getTarget(c, mc.player);
   }

   private BlockPos findBestAnchorPos(Minecraft mc, LivingEntity target) {
      BlockPos origin = mc.player.blockPosition();
      int range = 5;
      BlockPos bestPos = null;
      double bestScore = -1.0;

      for (int dx = -range; dx <= range; dx++) {
         for (int dy = -range; dy <= range; dy++) {
            for (int dz = -range; dz <= range; dz++) {
               BlockPos cand = origin.offset(dx, dy, dz);
               BlockState st = mc.level.getBlockState(cand);
               if (st.canBeReplaced() && mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(cand), e -> true).isEmpty()) {
                  Vec3 centre = Vec3.atCenterOf(cand);
                  double dist = centre.distanceTo(target.getEyePosition());
                  if (!(dist >= 10.0)) {
                     double distToBody = centre.distanceTo(new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()));
                     if (!(distToBody > 3.0)) {
                        double exposure = this.computeExposure(mc, centre, target);
                        if (!(exposure <= 0.0)) {
                           double score = 5.0 * (1.0 - dist / 10.0) * exposure / Math.max(0.5, distToBody);
                           if (!(score <= bestScore) && this.findPlacement(mc, cand, null) != null) {
                              bestScore = score;
                              bestPos = cand;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return bestPos;
   }

   private double computeExposure(Minecraft mc, Vec3 centre, LivingEntity target) {
      AABB box = target.getBoundingBox();
      int hits = 0;
      int total = 0;

      for (double fx : new double[]{box.minX, box.maxX}) {
         for (double fy : new double[]{box.minY, box.maxY}) {
            for (double fz : new double[]{box.minZ, box.maxZ}) {
               HitResult r = mc.level.clip(new ClipContext(centre, new Vec3(fx, fy, fz), Block.COLLIDER, Fluid.NONE, target));
               if (r.getType() == Type.MISS) {
                  hits++;
               }

               total++;
            }
         }
      }

      return total == 0 ? 0.0 : (double)hits / total;
   }

   private static boolean isBindPressed(Minecraft mc, String name) {
      if (name != null && !name.isEmpty() && !"None".equalsIgnoreCase(name)) {
         long window = mc.getWindow().handle();

         return switch (name) {
            case "Mouse 1" -> GLFW.glfwGetMouseButton(window, 0) == 1;
            case "Mouse 2" -> GLFW.glfwGetMouseButton(window, 1) == 1;
            case "Mouse 3" -> GLFW.glfwGetMouseButton(window, 2) == 1;
            case "Mouse 4" -> GLFW.glfwGetMouseButton(window, 3) == 1;
            case "Mouse 5" -> GLFW.glfwGetMouseButton(window, 4) == 1;
            default -> {
               int key = guiNameToGlfwKey(name);
               yield key != -1 && GLFW.glfwGetKey(window, key) == 1;
            }
         };
      } else {
         return false;
      }
   }

   private static int guiNameToGlfwKey(String name) {
      return switch (name) {
         case "Space" -> 32;
         case "Esc" -> 256;
         case "Ctrl" -> 341;
         case "Shift" -> 340;
         case "Alt" -> 342;
         case "Up" -> 265;
         case "Down" -> 264;
         case "Left" -> 263;
         case "Right" -> 262;
         case "Caps Lock" -> 280;
         case "Backspace" -> 259;
         case "Enter" -> 257;
         case "Tab" -> 258;
         case "Delete" -> 261;
         case "Insert" -> 260;
         case "Home" -> 268;
         case "End" -> 269;
         case "Page Up" -> 266;
         case "Page Down" -> 267;
         case "F1" -> 290;
         case "F2" -> 291;
         case "F3" -> 292;
         case "F4" -> 293;
         case "F5" -> 294;
         case "F6" -> 295;
         case "F7" -> 296;
         case "F8" -> 297;
         case "F9" -> 298;
         case "F10" -> 299;
         case "F11" -> 300;
         case "F12" -> 301;
         default -> {
            label151: {
               if (name.length() == 1) {
                  char c = name.charAt(0);
                  if (c >= 'A' && c <= 'Z') {
                     yield 65 + (c - 'A');
                  }

                  if (c >= '0' && c <= '9') {
                     yield c == '0' ? 48 : 49 + (c - '1');
                  }
               }

               yield -1;
            }
         }
      };
   }

   private InteractionResult useItemOn(Minecraft mc, BlockHitResult hit) {
      InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
      if (result.consumesAction()) {
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.setRightClickDelay(mc, 4);
      }

      return result;
   }

   private void suppressVanillaRmb(Minecraft mc) {
      AccessorCompat.setClickCount(mc.options.keyUse, 0);
      this.setRightClickDelay(mc, 4);
   }

   private void setRightClickDelay(Minecraft mc, int value) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var4) {
         }
      }
   }

   private static BlockHitResult crosshairBlock(Minecraft mc) {
      return mc.hitResult instanceof BlockHitResult b && b.getType() == Type.BLOCK ? b : null;
   }

   private static boolean isAnchor(Minecraft mc, BlockPos pos) {
      return pos != null && mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR);
   }

   private static int charge(Minecraft mc, BlockPos pos) {
      return !isAnchor(mc, pos) ? 0 : (Integer)mc.level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE);
   }

   private void resetState() {
      this.stage = AutoAnchor.Stage.IDLE;
      this.silentSeq = false;
      this.anchorPos = null;
      this.shieldPos = null;
      this.chargedAt = -1;
      this.stageStart = this.ticks;
   }

   private void restoreSlotNow(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
   }

   private static int findSlot(Minecraft mc, Item item) {
      Inventory inv = mc.player.getInventory();
      int held = InventoryHelper.getSelected(inv);
      if (inv.getItem(held).is(item)) {
         return held;
      }

      for (int i = 0; i < 9; i++) {
         if (inv.getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private static int findNonGlowstoneSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (!inv.getItem(i).is(Items.GLOWSTONE)) {
            return i;
         }
      }

      return -1;
   }

   private void swapTo(Minecraft mc, int slot) {
      Inventory inv = mc.player.getInventory();
      int cur = InventoryHelper.getSelected(inv);
      if (cur != slot) {
         if (this.savedSlot == -1) {
            this.savedSlot = cur;
         }

         InventoryHelper.setSelected(inv, slot);
      }
   }

   private static float[] lookAngles(Vec3 eyes, Vec3 target) {
      Vec3 d = target.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      return new float[]{(float)(Math.atan2(d.x * -1.0, d.z) * 180.0 / Math.PI), (float)(Math.atan2(d.y, h) * -1.0 * 180.0 / Math.PI)};
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

   private static double sq(double v) {
      return v * v;
   }

   private record Click(BlockHitResult hit, float yaw, float pitch) {
   }

   private enum Stage {
      IDLE,
      PLACE_ANCHOR,
      CHARGE,
      SHIELD,
      EXPLODE,
      DONE;
   }
}
