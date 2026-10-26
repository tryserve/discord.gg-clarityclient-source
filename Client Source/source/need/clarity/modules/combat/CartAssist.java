package need.clarity.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import need.clarity.PerFrameEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.InventoryHelper;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class CartAssist extends ModuleBase {
   private static final double FACE_MARGIN = 0.05;
   private static final int MAX_SIM_TICKS = 200;
   private static final int STAGE_TIMEOUT = 30;
   private static final Item[] RAILS = new Item[]{Items.RAIL, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL};
   private static final Field RIGHT_CLICK_DELAY_FIELD;
   private final Random rng = new Random();
   private final Set<Integer> seenArrows = new HashSet<>();
   private final Map<Integer, Integer> arrowFirstSeen = new HashMap<>();
   private static final int FIRE_WAIT_TICKS = 6;
   private boolean wasUsingBow = false;
   private int bowTicks = 0;
   private boolean bowFlame = false;
   private CartAssist.Stage stage = CartAssist.Stage.IDLE;
   private BlockPos railPos = null;
   private int arrowId = -1;
   private int landTicks = 0;
   private int jobStart = 0;
   private int ticks = 0;
   private int stageStart = 0;
   private long actAfterMs = 0L;
   private CartAssist.Click click = null;
   private int savedSlot = -1;
   private int lastSetSlot = -1;
   private long swapBackAt = 0L;
   private boolean aiming = false;
   private boolean aimSilent = true;
   private float aimYaw;
   private float aimPitch;

   public CartAssist() {
      super("Cart Assist", "Combat");
      PerFrameEvents.register(this::onFrame);
      WorldRenderEvent.register(this::onWorldRender);
   }

   @Override
   public void onEnable() {
      this.reset();
      this.seenArrows.clear();
      this.arrowFirstSeen.clear();
      this.wasUsingBow = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (isArrow(e)) {
               this.seenArrows.add(e.getId());
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.restoreSlot(Minecraft.getInstance());
      this.stopAim();
      this.reset();
   }

   private void reset() {
      this.stage = CartAssist.Stage.IDLE;
      this.railPos = null;
      this.arrowId = -1;
      this.click = null;
      this.swapBackAt = 0L;
   }

   private void onFrame() {
      if (this.isEnabled() && this.aiming && this.aimSilent) {
         AimAssist.setSilentRotation(this.aimYaw, this.aimPitch);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         this.ticks++;
         long now = System.currentTimeMillis();
         if (this.savedSlot != -1 && this.lastSetSlot != -1 && InventoryHelper.getSelected(mc.player.getInventory()) != this.lastSetSlot) {
            this.savedSlot = -1;
            this.swapBackAt = 0L;
         }

         boolean usingBow = mc.player.isUsingItem() && mc.player.getUseItem().is(Items.BOW);
         boolean released = !usingBow && this.wasUsingBow;
         if (usingBow) {
            this.bowTicks = mc.player.getTicksUsingItem();
            this.bowFlame = hasFlame(mc, mc.player.getUseItem());
         }

         this.wasUsingBow = usingBow;
         if (this.stage != CartAssist.Stage.IDLE && this.stage != CartAssist.Stage.DONE) {
            this.scanArrows(mc, now);
         } else {
            if (this.swapBackAt > 0L && now >= this.swapBackAt) {
               this.restoreSlot(mc);
            }

            if (this.stage == CartAssist.Stage.DONE) {
               this.stopAim();
               this.stage = CartAssist.Stage.IDLE;
            }

            if (released && this.bowFlame) {
               this.onBowRelease(mc, now);
            }

            this.scanArrows(mc, now);
            if (this.stage == CartAssist.Stage.IDLE) {
               return;
            }
         }

         if (mc.screen == null) {
            if (this.ticks - this.stageStart > 30 || this.ticks - this.jobStart > this.landTicks + 3) {
               this.finish();
            } else if (now >= this.actAfterMs) {
               switch (this.stage) {
                  case RAIL:
                     this.tickRail(mc, now);
                     break;
                  case CART:
                     this.tickCart(mc, now);
               }
            }
         }
      } else {
         this.seenArrows.clear();
      }
   }

   private void scanArrows(Minecraft mc, long now) {
      Set<Integer> alive = new HashSet<>();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (isArrow(e)) {
            int id = e.getId();
            alive.add(id);
            if (!this.seenArrows.contains(id)) {
               this.arrowFirstSeen.putIfAbsent(id, this.ticks);
               if (!(e instanceof TraceableEntity te && te.getOwner() == mc.player)) {
                  this.seenArrows.add(id);
               } else if (!e.isOnFire()) {
                  if (this.ticks - this.arrowFirstSeen.get(id) > 6) {
                     this.seenArrows.add(id);
                  }
               } else {
                  this.seenArrows.add(id);
                  if (this.stage == CartAssist.Stage.IDLE) {
                     this.tryStart(mc, now, this.predict(mc, e.position(), e.getDeltaMovement(), e), 0);
                  }
               }
            }
         }
      }

      this.seenArrows.retainAll(alive);
      this.arrowFirstSeen.keySet().retainAll(alive);
   }

   private void onBowRelease(Minecraft mc, long now) {
      float f = this.bowTicks / 20.0F;
      f = (f * f + f * 2.0F) / 3.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      if (!(f < 0.1F)) {
         LocalPlayer p = mc.player;
         Vec3 start = new Vec3(p.getX(), p.getEyeY() - 0.1, p.getZ());
         Vec3 dir = AimAssist.getRotationVector(p.getXRot(), p.getYRot());
         Vec3 mv = p.getDeltaMovement();
         Vec3 vel = dir.scale(f * 3.0).add(mv.x, p.onGround() ? 0.0 : mv.y, mv.z);
         int pingTicks = 0;

         try {
            if (mc.getConnection() != null) {
               PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
               if (info != null) {
                  pingTicks = Math.max(0, info.getLatency() / 50);
               }
            }
         } catch (Throwable var12) {
         }

         this.tryStart(mc, now, this.predict(mc, start, vel, p), pingTicks);
      }
   }

   private void tryStart(Minecraft mc, long now, CartAssist.Landing land, int extraTicks) {
      if (land != null && land.ticks >= 1) {
         if (this.validRailSpot(mc, land.pos)) {
            if (findSlot(mc, Items.TNT_MINECART) >= 0) {
               if (!hasTntCart(mc, land.pos)) {
                  boolean hasRail = isRail(mc, land.pos);
                  if (hasRail || findRailSlot(mc) >= 0) {
                     if (hasRail || this.findPlacement(mc, land.pos) != null) {
                        if (!hasRail || this.railClick(mc, land.pos) != null) {
                           this.railPos = land.pos;
                           this.landTicks = land.ticks + extraTicks;
                           this.jobStart = this.ticks;
                           this.click = null;
                           this.setStage(hasRail ? CartAssist.Stage.CART : CartAssist.Stage.RAIL);
                           this.actAfterMs = now + (this.getSetting("Humanize", true) ? this.randomMs("Reaction Delay", 0, 30) : 0L);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean hasFlame(Minecraft mc, ItemStack bow) {
      try {
         Registry<Enchantment> reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
         Reference<Enchantment> flame = (Reference<Enchantment>)reg.get(Enchantments.FLAME).orElse(null);
         return flame != null && EnchantmentHelper.getItemEnchantmentLevel(flame, bow) > 0;
      } catch (Throwable t) {
         return false;
      }
   }

   private void tickRail(Minecraft mc, long now) {
      if (isRail(mc, this.railPos)) {
         this.setStage(CartAssist.Stage.CART);
         this.click = null;
      } else if (!mc.level.getBlockState(this.railPos).canBeReplaced()) {
         this.finish();
      } else {
         int slot = findRailSlot(mc);
         if (slot < 0) {
            this.finish();
         } else {
            if (this.click == null) {
               this.click = this.findPlacement(mc, this.railPos);
            }

            if (this.click == null) {
               this.finish();
            } else {
               this.select(mc, slot);
               BlockHitResult hit = this.aimAndRay(mc, this.click);
               if (hit != null) {
                  if (hit.getBlockPos().equals(this.click.hit().getBlockPos()) && hit.getDirection() == this.click.hit().getDirection()) {
                     if (this.use(mc, hit)) {
                        this.setStage(CartAssist.Stage.CART);
                        this.click = null;
                        this.actAfterMs = now + (this.getSetting("Humanize", true) ? this.randomMs("Place Delay", 0, 40) : this.randomMs("Place Delay", 0, 0));
                     }
                  }
               }
            }
         }
      }
   }

   private void tickCart(Minecraft mc, long now) {
      if (!isRail(mc, this.railPos)) {
         this.finish();
      } else if (hasTntCart(mc, this.railPos)) {
         this.finish();
      } else {
         int slot = findSlot(mc, Items.TNT_MINECART);
         if (slot < 0) {
            this.finish();
         } else {
            if (this.click == null) {
               this.click = this.railClick(mc, this.railPos);
            }

            if (this.click == null) {
               this.finish();
            } else {
               this.select(mc, slot);
               BlockHitResult hit = this.aimAndRay(mc, this.click);
               if (hit != null && hit.getBlockPos().equals(this.railPos)) {
                  if (this.use(mc, hit)) {
                     this.finish();
                  }
               }
            }
         }
      }
   }

   private void finish() {
      this.stage = CartAssist.Stage.DONE;
      this.click = null;
      if (this.savedSlot != -1 && this.getSetting("Swap Back", true)) {
         this.swapBackAt = System.currentTimeMillis() + this.randomMs("Swap Back Delay", 50, 150);
      } else if (!this.getSetting("Swap Back", true)) {
         this.savedSlot = -1;
      }
   }

   private void setStage(CartAssist.Stage s) {
      this.stage = s;
      this.stageStart = this.ticks;
   }

   private CartAssist.Landing predict(Minecraft mc, Vec3 start, Vec3 vel, Entity self) {
      Vec3 pos = start;
      Vec3 v = vel;

      for (int t = 1; t <= 200; t++) {
         Vec3 next = pos.add(v);
         BlockHitResult bh = mc.level.clip(new ClipContext(pos, next, Block.COLLIDER, Fluid.NONE, self));
         Vec3 end = bh.getType() == Type.BLOCK ? bh.getLocation() : next;
         LivingEntity hitEntity = null;
         double best = Double.MAX_VALUE;
         AABB sweep = new AABB(pos, end).inflate(1.0);

         for (Entity e : mc.level.getEntities(self, sweep, en -> en instanceof LivingEntity && en != mc.player && en.isAlive() && !Friends.isProtected(en))) {
            Optional<Vec3> c = e.getBoundingBox().inflate(0.3).clip(pos, end);
            if (!c.isEmpty()) {
               double d = pos.distanceToSqr(c.get());
               if (d < best) {
                  best = d;
                  hitEntity = (LivingEntity)e;
               }
            }
         }

         if (hitEntity != null) {
            BlockPos feet = BlockPos.containing(hitEntity.getX(), hitEntity.getY() + 0.01, hitEntity.getZ());
            return new CartAssist.Landing(feet, t);
         }

         if (bh.getType() == Type.BLOCK) {
            BlockPos at = spotFromHit(mc, bh);
            return at == null ? null : new CartAssist.Landing(at, t);
         }

         pos = next;
         boolean water = !mc.level.getFluidState(BlockPos.containing(pos)).isEmpty();
         v = v.scale(water ? 0.6 : 0.99).add(0.0, -0.05, 0.0);
         if (pos.y < mc.level.getMinY() - 8) {
            return null;
         }
      }

      return null;
   }

   private static BlockPos spotFromHit(Minecraft mc, BlockHitResult bh) {
      Direction face = bh.getDirection();
      if (face == Direction.DOWN) {
         return null;
      }

      if (face == Direction.UP) {
         return bh.getBlockPos().above();
      }

      BlockPos p = bh.getBlockPos().relative(face);

      for (int i = 0; i < 4; i++) {
         if (sturdyTop(mc, p.below())) {
            return p;
         }

         p = p.below();
      }

      return null;
   }

   private static boolean sturdyTop(Minecraft mc, BlockPos pos) {
      return mc.level.getBlockState(pos).isFaceSturdy(mc.level, pos, Direction.UP);
   }

   private static boolean isArrow(Entity e) {
      return e.getType() == EntityType.ARROW || e.getType() == EntityType.SPECTRAL_ARROW;
   }

   private static boolean isRail(Minecraft mc, BlockPos pos) {
      return mc.level.getBlockState(pos).getBlock() instanceof BaseRailBlock;
   }

   private boolean validRailSpot(Minecraft mc, BlockPos pos) {
      BlockState st = mc.level.getBlockState(pos);
      if (!(st.getBlock() instanceof BaseRailBlock)) {
         if (!st.canBeReplaced() || !st.getFluidState().isEmpty()) {
            return false;
         }

         if (!sturdyTop(mc, pos.below())) {
            return false;
         }
      }

      double reach = Reach.vanillaBlockReach(mc.player);
      return mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= (reach + 0.5) * (reach + 0.5);
   }

   private static boolean hasTntCart(Minecraft mc, BlockPos pos) {
      return !mc.level.getEntities((Entity)null, new AABB(pos).inflate(0.2), e -> e.getType() == EntityType.TNT_MINECART && e.isAlive()).isEmpty();
   }

   private CartAssist.Click findPlacement(Minecraft mc, BlockPos pos) {
      Vec3 eyes = mc.player.getEyePosition();
      double reachSq = sq(Reach.vanillaBlockReach(mc.player));
      CartAssist.Click best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         BlockPos support = pos.relative(dir);
         BlockState s = mc.level.getBlockState(support);
         if (s.isSolid() && !s.canBeReplaced() && !s.hasBlockEntity() && s.isCollisionShapeFullBlock(mc.level, support)) {
            CartAssist.Click c = this.visiblePoint(
               mc, eyes, reachSq, support, dir.getOpposite(), Vec3.atCenterOf(support).add(dir.getOpposite().getStepX() * 0.5, dir.getOpposite().getStepY() * 0.5, dir.getOpposite().getStepZ() * 0.5)
            );
            if (c != null) {
               double score = this.angleCost(mc, c) + (dir == Direction.DOWN ? 0 : 5);
               if (score < bestScore) {
                  bestScore = score;
                  best = c;
               }
            }
         }
      }

      return best;
   }

   private CartAssist.Click railClick(Minecraft mc, BlockPos rail) {
      Vec3 eyes = mc.player.getEyePosition();
      double reachSq = sq(Reach.vanillaBlockReach(mc.player));
      Vec3 top = new Vec3(rail.getX() + 0.5, rail.getY() + 0.0625, rail.getZ() + 0.5);
      return eyes.y <= top.y + 0.05 ? null : this.visiblePoint(mc, eyes, reachSq, rail, Direction.UP, top);
   }

   private CartAssist.Click visiblePoint(Minecraft mc, Vec3 eyes, double reachSq, BlockPos block, Direction face, Vec3 centre) {
      Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
      if (eyes.subtract(centre).dot(n) < 0.05) {
         return null;
      }

      double jitter = this.getSetting("Humanize", true) ? Math.max(0, Math.min(100, this.getSetting("Aim Jitter", 40))) / 100.0 * 0.35 : 0.0;
      List<Vec3> pts = new ArrayList<>();
      if (jitter > 0.0) {
         pts.add(offsetOnFace(centre, face, (this.rng.nextDouble() * 2.0 - 1.0) * jitter, (this.rng.nextDouble() * 2.0 - 1.0) * jitter));
      }

      pts.add(centre);
      double[] o = new double[]{-0.3, 0.0, 0.3};

      for (double a : o) {
         for (double b : o) {
            if (a != 0.0 || b != 0.0) {
               pts.add(offsetOnFace(centre, face, a, b));
            }
         }
      }

      for (Vec3 p : pts) {
         if (!(eyes.distanceToSqr(p) > reachSq)) {
            Vec3 end = p.add(p.subtract(eyes).normalize().scale(0.05));
            BlockHitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
            if (r.getType() == Type.BLOCK && r.getBlockPos().equals(block) && (face == Direction.UP && mc.level.getBlockState(block).getBlock() instanceof BaseRailBlock || r.getDirection() == face)) {
               float[] rot = lookAt(eyes, p);
               return new CartAssist.Click(new BlockHitResult(p, face, block, false), rot[0], rot[1]);
            }
         }
      }

      return null;
   }

   private static Vec3 offsetOnFace(Vec3 c, Direction face, double a, double b) {
      return switch (face.getAxis()) {
         case X -> c.add(0.0, a, b);
         case Y -> c.add(a, 0.0, b);
         case Z -> c.add(a, b, 0.0);
         default -> throw new MatchException(null, null);
      };
   }

   private BlockHitResult aimAndRay(Minecraft mc, CartAssist.Click c) {
      boolean silent = this.getSetting("Silent Rotations", true);
      float[] sent = null;
      if (this.aiming && this.aimSilent == silent) {
         sent = silent ? new float[]{this.aimYaw, this.aimPitch} : new float[]{mc.player.getYRot(), mc.player.getXRot()};
      }

      float fromYaw;
      float fromPitch;
      if (this.aiming && this.aimSilent == silent) {
         fromYaw = this.aimYaw;
         fromPitch = this.aimPitch;
      } else {
         fromYaw = mc.player.getYRot();
         fromPitch = mc.player.getXRot();
      }

      float speed = Math.max(1.0F, this.getSetting("Rotation Speed", 180.0F));
      if (this.getSetting("Humanize", true)) {
         float var = Math.max(0, Math.min(100, this.getSetting("Speed Variation", 20))) / 100.0F;
         speed *= 1.0F + (this.rng.nextFloat() * 2.0F - 1.0F) * var;
      }

      float newYaw = step(fromYaw, c.yaw(), speed);
      float newPitch = Mth.clamp(step(fromPitch, c.pitch(), speed), -90.0F, 90.0F);
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

   private static BlockHitResult rayFrom(Minecraft mc, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 end = eyes.add(AimAssist.getRotationVector(pitch, yaw).scale(Reach.vanillaBlockReach(mc.player)));
      BlockHitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return r.getType() == Type.BLOCK ? r : null;
   }

   private boolean use(Minecraft mc, BlockHitResult hit) {
      setRightClickDelay(mc, 0);
      InteractionResult res = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
      setRightClickDelay(mc, 4);
      if (!res.consumesAction()) {
         return false;
      }

      mc.player.swing(InteractionHand.MAIN_HAND);
      return true;
   }

   private void select(Minecraft mc, int slot) {
      Inventory inv = mc.player.getInventory();
      int cur = InventoryHelper.getSelected(inv);
      if (cur != slot) {
         if (this.savedSlot == -1) {
            this.savedSlot = cur;
         }

         InventoryHelper.setSelected(inv, slot);
      }

      this.lastSetSlot = slot;
      this.swapBackAt = 0L;
   }

   private void restoreSlot(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      this.lastSetSlot = -1;
      this.swapBackAt = 0L;
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

   private static int findRailSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();
      int held = InventoryHelper.getSelected(inv);

      for (Item r : RAILS) {
         if (inv.getItem(held).is(r)) {
            return held;
         }
      }

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);

         for (Item r : RAILS) {
            if (s.is(r)) {
               return i;
            }
         }
      }

      return -1;
   }

   private void onWorldRender(PoseStack ps, float partialTick) {
      if (this.isEnabled() && this.getSetting("Render", true)) {
         if (this.railPos != null && (this.stage == CartAssist.Stage.RAIL || this.stage == CartAssist.Stage.CART)) {
            int rgb = ThemeColors.resolveRgb(this.getSetting("Render Color", "#ff5533"), 16733491, false);
            AABB box = new AABB(this.railPos.getX(), this.railPos.getY(), this.railPos.getZ(), this.railPos.getX() + 1, this.railPos.getY() + 0.125, this.railPos.getZ() + 1);
            ClarityRenderUtils.drawBoxes(ps, List.of(box), 1426063360 | rgb & 16777215, -587202560 | rgb & 16777215, true, true);
         }
      }
   }

   private float angleCost(Minecraft mc, CartAssist.Click c) {
      float y = this.aiming ? this.aimYaw : mc.player.getYRot();
      float p = this.aiming ? this.aimPitch : mc.player.getXRot();
      return Math.abs(Mth.wrapDegrees(c.yaw() - y)) + Math.abs(c.pitch() - p);
   }

   private static float step(float from, float to, float max) {
      float d = Mth.wrapDegrees(to - from);
      return Math.abs(d) <= max ? from + d : from + Math.signum(d) * max;
   }

   private static float[] lookAt(Vec3 eyes, Vec3 p) {
      Vec3 d = p.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      return new float[]{(float)(Math.atan2(-d.x, d.z) * 180.0 / Math.PI), (float)(Math.atan2(d.y, h) * -180.0 / Math.PI)};
   }

   private long randomMs(String key, int loDef, int hiDef) {
      int lo = this.getSetting(key + "_lo", loDef);
      int hi = this.getSetting(key + "_hi", hiDef);
      return hi <= lo ? Math.max(0, lo) : lo + this.rng.nextInt(hi - lo + 1);
   }

   private static void setRightClickDelay(Minecraft mc, int v) {
      if (RIGHT_CLICK_DELAY_FIELD != null) {
         try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, v);
         } catch (IllegalAccessException var3) {
         }
      }
   }

   private static double sq(double v) {
      return v * v;
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

   private record Click(BlockHitResult hit, float yaw, float pitch) {
   }

   private record Landing(BlockPos pos, int ticks) {
   }

   private enum Stage {
      IDLE,
      RAIL,
      CART,
      DONE;
   }
}
