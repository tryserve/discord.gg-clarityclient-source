package need.clarity.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import need.clarity.LocalPlayerRotationState;
import need.clarity.PerFrameEvents;
import need.clarity.Unload;
import need.clarity.config.ClarityConfig;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.util.InventoryHelper;
import need.clarity.util.RotationGcd;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class CrystalAura extends ModuleBase {
   private static final double CRYSTAL_POWER_DIAMETER = 12.0;
   private static final float SELF_WEIGHT = 0.5F;
   private static final int SCAN_RADIUS = 3;
   private static final int EVAL_LIMIT = 12;
   private static final long HOLD_AIM_MS = 80L;
   private static final double PLACE_REACH = 3.0;
   private static final double PLAN_REACH = 2.85;
   private static final double MAX_SPOT_TARGET_DIST = 3.5;
   private static final float TURN_PENALTY_PER_DEG = 0.045F;
   private static final float MAX_YAW_TURN = 110.0F;
   private final Random rng = new Random();
   private static volatile CrystalAura INSTANCE;
   private volatile long tandemUntil = 0L;
   private long tandemStart = 0L;
   private String lastDebug = "";
   private volatile CrystalAura.Plan plan = null;
   private String planKey = null;
   private long planSince = 0L;
   private long planSeed = 0L;
   private long lastPlanAt = 0L;
   private long lastActionAt = 0L;
   private long engagedSince = 0L;
   private BlockPos stickyBase = null;
   private boolean releaseAim = false;
   private Player airTarget = null;
   private long targetAirAt = 0L;
   private static final long AIRBORNE_GRACE_MS = 300L;
   private long lastSuccessAt = 0L;
   private static final long GIVE_UP_MS = 1200L;
   private static final float HYST = 1.5F;
   private long reactUntil = 0L;
   private final Map<String, Long> blacklist = new HashMap<>();
   private final Map<Integer, Long> brokenIds = new HashMap<>();
   private final Map<BlockPos, Long> recentPlaces = new HashMap<>();
   private BlockPos pendingObsidian = null;
   private long pendingUntil = 0L;
   private volatile int lastActTick = -1000;
   private int lastInteractTick = -1000;
   private int aimConvergedTick = -1;
   private Vec3 lastExplosionPos = null;
   private long lastExplosionAt = 0L;
   private final Map<Integer, Vec3> seenCrystals = new HashMap<>();
   private Player checkTarget = null;
   private float checkHealth = 0.0F;
   private float checkEst = 0.0F;
   private long checkAt = 0L;
   private int noEffect = 0;
   private long suspendUntil = 0L;
   private BlockPos lastCrystalBase = null;
   private long lastCrystalAt = 0L;
   private long nextPlaceAt = 0L;
   private long nextBreakAt = 0L;
   private long nextObsidianAt = 0L;
   private long nextSwapAt = 0L;
   private long swapBackAt = 0L;
   private int savedSlot = -1;
   private boolean rotating = false;
   private float curYaw = 0.0F;
   private float curPitch = 0.0F;
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private long lastFrameNs = -1L;
   private double speedMul = 1.0;
   private double speedMulGoal = 1.0;
   private long nextSpeedAt = 0L;
   private CrystalAura.Plan pendingExec = null;
   private AABB hypoBox = null;

   public CrystalAura() {
      super("Crystal Aura", "Combat");
      INSTANCE = this;
      PerFrameEvents.register(this::onFrame);
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)c -> {
         if (!Unload.isUnloaded()) {
            try {
               this.onTickEnd();
            } catch (Throwable var3) {
            }
         }
      });
   }

   public static void onKillAuraAttack() {
      CrystalAura i = INSTANCE;
      if (i != null && i.isEnabled() && i.getSetting("Work With Kill Aura", true)) {
         long now = System.currentTimeMillis();
         Minecraft mcNow = Minecraft.getInstance();
         if (mcNow.player != null) {
            i.lastActTick = mcNow.player.tickCount;
         }

         i.tandemStart = now;
         i.tandemUntil = now + i.getSetting("Combo Duration", 1200).intValue();
         if (KillAura.getTarget() instanceof Player pl) {
            i.airTarget = pl;
            i.targetAirAt = now;
         }
      }
   }

   private void onTickEnd() {
      CrystalAura.Plan p = this.pendingExec;
      this.pendingExec = null;
      if (p != null && this.isEnabled() && p == this.plan) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && mc.gameMode != null && mc.screen == null) {
            if (this.rotating && AimAssist.hasSilentRotation()) {
               this.sentYaw = LocalPlayerRotationState.tickServerYaw;
               this.sentPitch = LocalPlayerRotationState.tickServerPitch;
            } else {
               this.sentYaw = mc.player.getYRot();
               this.sentPitch = mc.player.getXRot();
            }

            this.execute(mc, p, System.currentTimeMillis());
         }
      }
   }

   public static void onLocalAttack(Entity e) {
      CrystalAura i = INSTANCE;
      if (i != null && i.isEnabled()) {
         if (e instanceof Player pl && pl != Minecraft.getInstance().player) {
            i.airTarget = pl;
            i.targetAirAt = System.currentTimeMillis();
         }
      }
   }

   public static void onEntitySpawn(int id) {
      CrystalAura i = INSTANCE;
      if (i != null && i.isEnabled() && i.getSetting("Instant Break", true)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && mc.gameMode != null && mc.screen == null) {
            if (!mc.player.isUsingItem() && mc.player.isAlive()) {
               if (mc.level.getEntity(id) instanceof EndCrystal ec) {
                  try {
                     i.instantBreak(mc, ec);
                  } catch (Throwable var6) {
                  }
               }
            }
         }
      }
   }

   private void instantBreak(Minecraft mc, EndCrystal ec) {
      long now = System.currentTimeMillis();
      if (now >= this.suspendUntil && now >= this.nextBreakAt) {
         if (mc.player.tickCount > this.lastInteractTick) {
            if (!this.brokenIds.containsKey(ec.getId())) {
               BlockPos under = ec.blockPosition().below();
               if (this.lastCrystalBase != null && under.equals(this.lastCrystalBase)) {
                  CrystalAura.Plan last = this.plan;
                  Player target = last != null ? last.target : null;
                  if (target != null && target.isAlive() && !target.isRemoved()) {
                     Vec3 eyes = mc.player.getEyePosition();
                     float breakRange = this.getSetting("Break Range", 4.5F);
                     if (!(eyes.distanceToSqr(closestPoint(ec.getBoundingBox(), eyes)) > (double)breakRange * breakRange)) {
                        Vec3 center = ec.position();
                        float t = this.targetDamage(mc, center, target);
                        float s = this.damageAt(mc, center, mc.player, true);
                        float tHealth = target.getHealth() + target.getAbsorptionAmount();
                        float myHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                        if (this.valid(
                           t,
                           s,
                           tHealth,
                           myHp,
                           this.getSetting("Min Damage", 6.0F),
                           this.getSetting("Max Self Damage", 8.0F),
                           this.getSetting("Anti Suicide", true),
                           this.getSetting("Safety Margin", 2.0F),
                           true
                        )) {
                           if (!this.hurtsFriend(mc, center)) {
                              CrystalAura.Plan p = new CrystalAura.Plan();
                              p.type = CrystalAura.PlanType.BREAK;
                              p.key = "B:" + ec.getId();
                              p.crystal = ec;
                              p.target = target;
                              p.aim = ec.getBoundingBox().getCenter();
                              p.targetDmg = t;
                              p.selfDmg = s;
                              this.doBreak(mc, p, now);
                              if (this.brokenIds.containsKey(ec.getId())) {
                                 this.debug(mc, "instant break dmg=" + String.format("%.1f", t));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean yieldsKillAura() {
      CrystalAura i = INSTANCE;
      if (i != null && i.isEnabled() && i.getSetting("Work With Kill Aura", true)) {
         long now = System.currentTimeMillis();
         return now < i.tandemUntil && (i.plan != null || i.savedSlot >= 0) || i.savedSlot >= 0;
      } else {
         return false;
      }
   }

   private boolean killAuraEngaged(Minecraft mc) {
      if (!this.getSetting("Work With Kill Aura", true)) {
         return false;
      }

      if (!KillAura.isActive()) {
         return false;
      }

      LivingEntity t = KillAura.getTarget();
      if (t != null && t.isAlive()) {
         double atk = 3.0;

         try {
            if (ClarityConfig.getSetting("Kill Aura", "Attack Range") instanceof Number n) {
               atk = n.doubleValue();
            }
         } catch (Throwable var7) {
         }

         return mc.player.distanceTo(t) <= atk + 1.5;
      } else {
         return false;
      }
   }

   private void debug(Minecraft mc, String msg) {
      if (this.getSetting("Debug", false)) {
         if (!msg.equals(this.lastDebug)) {
            this.lastDebug = msg;
            if (mc.player != null) {
               mc.player.sendSystemMessage(Component.literal("[CrystalAura] " + msg));
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.resetAll();
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      this.plan = null;
      this.rotating = false;
      AimAssist.clearSilentRotation();
      this.restoreSlotNow(mc);
      this.resetAll();
   }

   private void resetAll() {
      this.plan = null;
      this.planKey = null;
      this.planSince = 0L;
      this.lastPlanAt = 0L;
      this.lastActionAt = 0L;
      this.reactUntil = 0L;
      this.engagedSince = 0L;
      this.lastSuccessAt = 0L;
      this.stickyBase = null;
      this.releaseAim = false;
      this.airTarget = null;
      this.targetAirAt = 0L;
      this.blacklist.clear();
      this.brokenIds.clear();
      this.recentPlaces.clear();
      this.pendingObsidian = null;
      this.pendingUntil = 0L;
      this.lastCrystalBase = null;
      this.lastCrystalAt = 0L;
      this.checkTarget = null;
      this.noEffect = 0;
      this.suspendUntil = 0L;
      this.lastActTick = -1000;
      this.lastInteractTick = -1000;
      this.aimConvergedTick = -1;
      this.lastExplosionPos = null;
      this.lastExplosionAt = 0L;
      this.seenCrystals.clear();
      this.nextPlaceAt = this.nextBreakAt = this.nextObsidianAt = this.nextSwapAt = this.swapBackAt = 0L;
      this.lastFrameNs = -1L;
      this.speedMul = this.speedMulGoal = 1.0;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (mc.player != null && mc.level != null && mc.gameMode != null) {
            long now = System.currentTimeMillis();
            if (this.rotating && AimAssist.hasSilentRotation()) {
               this.sentYaw = LocalPlayerRotationState.tickServerYaw;
               this.sentPitch = LocalPlayerRotationState.tickServerPitch;
            } else {
               this.sentYaw = mc.player.getYRot();
               this.sentPitch = mc.player.getXRot();
            }

            this.prune(now);
            if (mc.screen == null && !mc.player.isUsingItem() && mc.player.isAlive()) {
               this.evaluateEffect(mc, now);
               if (now < this.suspendUntil) {
                  this.setPlan(null, now);
                  this.handleSwapBack(mc, now);
               } else {
                  CrystalAura.Plan p = this.computePlan(mc, now);
                  if (p == null) {
                     this.debug(mc, "no good play (airborne/damage/safety/inventory)");
                  } else {
                     this.debug(mc, p.type + " dmg=" + String.format("%.1f", p.targetDmg) + " self=" + String.format("%.1f", p.selfDmg));
                  }

                  this.setPlan(p, now);
                  if (p == null) {
                     if (now - this.lastPlanAt >= 80L) {
                        this.handleSwapBack(mc, now);
                     }
                  } else {
                     this.swapBackAt = 0L;
                     this.lastPlanAt = now;
                     if (now - Math.max(this.engagedSince, this.lastSuccessAt) > 1200L) {
                        this.blacklist.put(p.key, now + 1200L);
                        this.stickyBase = null;
                        this.releaseAim = true;
                        this.suspendUntil = now + 400L;
                        this.debug(mc, "no successful place/break in 1200ms - dropping " + p.type + " " + p.key);
                        this.setPlan(null, now);
                     } else {
                        long ref = Math.max(this.planSince, this.lastActionAt);
                        boolean converged = this.aimHitsPlan(mc, p);
                        long limit = converged ? 250L : 500L;
                        if (now - ref > limit) {
                           if (p.type != CrystalAura.PlanType.BREAK) {
                              BlockHitResult r = this.rayBlock(mc, 3.0);
                              this.debug(
                                 mc,
                                 "stall detail: aimErr="
                                    + String.format("%.2f", this.aimError(mc, p.aim))
                                    + " ray="
                                    + (r == null ? "MISS" : r.getBlockPos().toShortString() + " " + r.getDirection())
                                    + " want="
                                    + p.pos.toShortString()
                                    + " aimDist="
                                    + String.format("%.2f", mc.player.getEyePosition().distanceTo(p.aim))
                              );
                           }

                           this.blacklist.put(p.key, now + (converged ? 700L : 1200L));
                           this.debug(mc, "stalled on " + p.type + (converged ? " (aimed but could not act)" : " (never got aimed)") + " - skipping");
                           this.setPlan(null, now);
                        } else {
                           if (p.type != CrystalAura.PlanType.BREAK && !this.aimHitsPlan(mc, p)) {
                              this.aimConvergedTick = -1;
                           } else {
                              if (this.aimConvergedTick < 0) {
                                 this.aimConvergedTick = mc.player.tickCount;
                              }

                              if (mc.player.tickCount > this.aimConvergedTick) {
                                 this.pendingExec = p;
                              }
                           }
                        }
                     }
                  }
               }
            } else {
               this.setPlan(null, now);
               this.handleSwapBack(mc, now);
            }
         } else {
            this.plan = null;
         }
      }
   }

   private void evaluateEffect(Minecraft mc, long now) {
      if (this.checkTarget != null) {
         if (now - this.checkAt >= this.crystalWaitMs() + 350L) {
            Player t = this.checkTarget;
            this.checkTarget = null;
            if (t.isAlive() && !t.isRemoved()) {
               float after = t.getHealth() + t.getAbsorptionAmount();
               float lost = this.checkHealth - after;
               if (!this.getSetting("Pause When Ineffective", false) || !(lost < Math.min(this.checkEst * 0.25F, 2.0F))) {
                  this.noEffect = 0;
               } else if (++this.noEffect >= 5) {
                  this.suspendUntil = now + 1500L;
                  this.noEffect = 0;
                  this.debug(mc, "explosions are not hurting the target - pausing");
               }
            } else {
               this.noEffect = 0;
            }
         }
      }
   }

   private void setPlan(CrystalAura.Plan p, long now) {
      if (p == null) {
         this.plan = null;
      } else {
         boolean wasIdle = this.plan == null && now - this.lastPlanAt > 80L;
         if (wasIdle) {
            this.engagedSince = now;
            this.planSeed = this.rng.nextLong();
            if (this.getSetting("Humanize", true)) {
               this.reactUntil = now < this.tandemUntil ? 0L : now + this.randMs("Reaction Delay", 40L, 120L);
            } else {
               this.reactUntil = 0L;
            }
         }

         this.releaseAim = false;
         this.stickyBase = p.type == CrystalAura.PlanType.BREAK && p.crystal != null ? p.crystal.blockPosition().below() : p.pos;
         if (!p.key.equals(this.planKey)) {
            this.planKey = p.key;
            this.planSince = now;
            this.aimConvergedTick = -1;
         }

         this.plan = p;
      }
   }

   private void prune(long now) {
      this.brokenIds.values().removeIf(t -> now - t > 300L);
      this.recentPlaces.values().removeIf(t -> now - t > 600L);
      this.blacklist.values().removeIf(t -> t < now);
      if (this.pendingObsidian != null && now > this.pendingUntil) {
         this.pendingObsidian = null;
      }
   }

   private CrystalAura.Plan computePlan(Minecraft mc, long now) {
      Player me = mc.player;
      Vec3 eyes = me.getEyePosition();
      Player target = this.pickTarget(mc);
      if (target == null) {
         this.loseTarget();
         return null;
      }

      float placeRange = 2.85F;
      float breakRange = this.getSetting("Break Range", 4.5F);
      float minDmg = this.getSetting("Min Damage", 6.0F);
      float maxSelf = this.getSetting("Max Self Damage", 8.0F);
      boolean antiSuicide = this.getSetting("Anti Suicide", true);
      float margin = this.getSetting("Safety Margin", 2.0F);
      boolean allowObs = this.getSetting("Place Obsidian", true);
      float tHealth = target.getHealth() + target.getAbsorptionAmount();
      float myHp = me.getHealth() + me.getAbsorptionAmount();
      List<Entity> ents = new ArrayList<>();
      List<EndCrystal> crystals = new ArrayList<>();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (!e.isRemoved() && (!(e.distanceToSqr(target) > 256.0) || !(e.distanceToSqr(me) > 256.0))) {
            ents.add(e);
            if (e instanceof EndCrystal ec && ec.isAlive()) {
               crystals.add(ec);
            }
         }
      }

      Set<Integer> ids = new HashSet<>();

      for (EndCrystal ec : crystals) {
         ids.add(ec.getId());
      }

      for (Entry<Integer, Vec3> en : this.seenCrystals.entrySet()) {
         if (!ids.contains(en.getKey())) {
            this.noteExplosion(en.getValue(), now);
         }
      }

      this.seenCrystals.clear();

      for (EndCrystal ec : crystals) {
         this.seenCrystals.put(ec.getId(), ec.position());
      }

      CrystalAura.Plan bestBreak = null;
      float bestBreakScore = -Float.MAX_VALUE;

      for (EndCrystal ec : crystals) {
         if (!this.brokenIds.containsKey(ec.getId())) {
            String key = "B:" + ec.getId();
            if (!this.blacklist.containsKey(key)) {
               AABB box = ec.getBoundingBox();
               Vec3 closest = closestPoint(box, eyes);
               if (!(eyes.distanceToSqr(closest) > (double)breakRange * breakRange)) {
                  Vec3 center = ec.position();
                  float t = this.targetDamage(mc, center, target);
                  if (!(t <= 0.0F)) {
                     float s = this.damageAt(mc, center, me, true);
                     boolean stickyB = this.sticky(ec.blockPosition().below());
                     if (this.valid(t, s, tHealth, myHp, minDmg, maxSelf, antiSuicide, margin, stickyB) && !this.hurtsFriend(mc, center)) {
                        BlockPos under = ec.blockPosition().below();
                        Vec3 aim = null;
                        if (eyes.y > under.getY() + 1.02) {
                           double[] off = this.aimOffsets("C:" + under.asLong());
                           Vec3 top = facePoint(under, Direction.UP, off[0], off[2]);
                           Vec3 dir = top.subtract(eyes);
                           double len = dir.length();
                           if (len > 1.0E-4) {
                              Vec3 far = top.add(dir.scale(0.3 / len));
                              if (box.clip(eyes, far).isPresent() && (this.visibleFace(mc, eyes, under, Direction.UP, top) || this.lineClear(mc, eyes, top))) {
                                 aim = top;
                              }
                           }
                        }

                        if (aim == null) {
                           aim = box.getCenter();
                           if (!this.lineClear(mc, eyes, aim)) {
                              continue;
                           }
                        }

                        float score = t - s * 0.5F;
                        if (score > bestBreakScore) {
                           bestBreakScore = score;
                           CrystalAura.Plan p = new CrystalAura.Plan();
                           p.type = CrystalAura.PlanType.BREAK;
                           p.key = key;
                           p.crystal = ec;
                           p.target = target;
                           p.aim = aim;
                           p.targetDmg = t;
                           p.selfDmg = s;
                           bestBreak = p;
                        }
                     }
                  }
               }
            }
         }
      }

      if (bestBreak != null) {
         return bestBreak;
      }

      boolean airborneOnly = this.getSetting("Airborne Only", true);
      if (target != this.airTarget) {
         this.airTarget = target;
         this.targetAirAt = 0L;
      }

      if (!target.onGround()) {
         this.targetAirAt = now;
      }

      boolean targetAirborne = now - this.targetAirAt <= 300L;
      if (me.distanceTo(target) > 7.5) {
         this.loseTarget();
         return null;
      }

      boolean haveCrystal = this.canUse(mc, Items.END_CRYSTAL);
      boolean haveObsidian = allowObs && this.canUse(mc, Items.OBSIDIAN);
      if (!haveCrystal) {
         return null;
      }

      if (this.lastCrystalBase != null) {
         if (now - this.lastCrystalAt < this.crystalWaitMs()) {
            boolean visible = false;

            for (EndCrystal ec : crystals) {
               if (ec.blockPosition().below().equals(this.lastCrystalBase)) {
                  visible = true;
                  break;
               }
            }

            if (!visible) {
               this.lastPlanAt = now;
               return null;
            }
         }

         this.lastCrystalBase = null;
      }

      boolean obsidianBusy = this.pendingObsidian != null && now <= this.pendingUntil;
      List<CrystalAura.Cand> existing = new ArrayList<>();
      List<CrystalAura.Cand> fresh = new ArrayList<>();
      BlockPos tp = target.blockPosition();
      Vec3 tpos = target.position();
      double reachSqr = 14.822499265670785;

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            for (int dy = -2; dy <= 0; dy++) {
               BlockPos base = tp.offset(dx, dy, dz);
               Vec3 center = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
               if (!(eyes.distanceToSqr(Vec3.atCenterOf(base)) > reachSqr) && !(center.distanceToSqr(tpos) > 12.25)) {
                  BlockState st = mc.level.getBlockState(base);
                  boolean isExisting = this.isBase(st);
                  boolean isFresh = !isExisting && (st.isAir() || st.canBeReplaced());
                  if ((isExisting || isFresh)
                     && (!airborneOnly || targetAirborne || !isFresh || this.sticky(base))
                     && (!isFresh || haveObsidian && !obsidianBusy)
                     && mc.level.getBlockState(base.above()).isAir()
                     && !this.crystalSpotBlocked(mc, ents, base, now)
                     && (!isFresh || !this.blockedByEntity(mc, ents, base))) {
                     String key = (isFresh ? "O:" : "C:") + base.asLong();
                     if (!this.blacklist.containsKey(key)) {
                        CrystalAura.Cand c = new CrystalAura.Cand();
                        c.base = base;
                        c.fresh = isFresh;
                        c.key = key;
                        c.ub = this.rawDamage(center, target);
                        if (!(c.ub < Math.min(minDmg, tHealth))) {
                           (isFresh ? fresh : existing).add(c);
                        }
                     }
                  }
               }
            }
         }
      }

      CrystalAura.Cand chosen = this.evaluate(mc, existing, eyes, target, 2.85F, tHealth, myHp, minDmg, maxSelf, antiSuicide, margin);
      if (chosen == null && !existing.isEmpty() && this.pendingObsidian != null && now <= this.pendingUntil) {
         return null;
      }

      if (chosen == null) {
         chosen = this.evaluate(mc, fresh, eyes, target, 2.85F, tHealth, myHp, minDmg, maxSelf, antiSuicide, margin);
      }

      if (chosen == null) {
         return null;
      }

      CrystalAura.Plan p = new CrystalAura.Plan();
      p.type = chosen.fresh ? CrystalAura.PlanType.OBSIDIAN : CrystalAura.PlanType.CRYSTAL;
      p.key = chosen.key;
      p.pos = chosen.base;
      p.clickPos = chosen.clickPos;
      p.face = chosen.face;
      p.aim = chosen.aim;
      p.target = target;
      p.targetDmg = chosen.t;
      p.selfDmg = chosen.s;
      return p;
   }

   private CrystalAura.Cand evaluate(
      Minecraft mc, List<CrystalAura.Cand> list, Vec3 eyes, Player target, float placeRange, float tHealth, float myHp, float minDmg, float maxSelf, boolean antiSuicide, float margin
   ) {
      if (list.isEmpty()) {
         return null;
      }

      float refYaw = this.rotating ? this.curYaw : mc.player.getYRot();
      float refPitch = this.rotating ? this.curPitch : mc.player.getXRot();
      List<CrystalAura.Cand> usable = new ArrayList<>();

      for (CrystalAura.Cand c : list) {
         if (this.pickFace(mc, c, eyes, placeRange, target)) {
            float[] a = angles(eyes, c.aim);
            c.turn = Math.abs(Mth.wrapDegrees(a[0] - refYaw)) + Math.abs(a[1] - refPitch) * 0.5F;
            c.yawTurn = Math.abs(Mth.wrapDegrees(a[0] - refYaw));
            usable.add(c);
         }
      }

      if (usable.isEmpty()) {
         return null;
      }

      boolean anyFront = false;

      for (CrystalAura.Cand c : usable) {
         if (c.yawTurn <= 110.0F) {
            anyFront = true;
            break;
         }
      }

      if (anyFront) {
         usable.removeIf(cx -> cx.yawTurn > 110.0F);
      }

      usable.sort(Comparator.comparingDouble(cx -> -(cx.ub - cx.turn * 0.045F + (this.sticky(cx.base) ? 1000.0F : 0.0F))));
      CrystalAura.Cand best = null;
      int evaluated = 0;

      for (CrystalAura.Cand c : usable) {
         if (evaluated >= 12) {
            break;
         }

         evaluated++;
         Vec3 center = new Vec3(c.base.getX() + 0.5, c.base.getY() + 1.0, c.base.getZ() + 0.5);
         this.hypoBox = c.fresh ? new AABB(c.base).deflate(0.001) : null;

         float t;
         float s;
         boolean friendHit;
         try {
            t = this.targetDamage(mc, center, target);
            s = !(t <= 0.0F) && !(t < Math.min(minDmg, tHealth)) ? this.damageAt(mc, center, mc.player, true) : 0.0F;
            friendHit = !(t <= 0.0F) && !(t < Math.min(minDmg, tHealth)) && this.hurtsFriend(mc, center);
         } finally {
            this.hypoBox = null;
         }

         if (!(t <= 0.0F) && !(t < Math.min(minDmg, tHealth)) && this.valid(t, s, tHealth, myHp, minDmg, maxSelf, antiSuicide, margin, this.sticky(c.base)) && !friendHit) {
            c.t = t;
            c.s = s;
            c.score = t - s * 0.5F - c.turn * 0.045F + (this.sticky(c.base) ? 6.0F : 0.0F);
            if (best == null || c.score > best.score) {
               best = c;
            }
         }
      }

      return best;
   }

   private void loseTarget() {
      this.stickyBase = null;
      this.planKey = null;
      this.releaseAim = true;
   }

   private boolean sticky(BlockPos base) {
      return this.stickyBase != null && this.stickyBase.equals(base);
   }

   private boolean valid(float t, float s, float tHealth, float myHp, float minDmg, float maxSelf, boolean antiSuicide, float margin, boolean sticky) {
      if (!sticky) {
         tHealth += 1.5F;
         minDmg += 1.5F;
         maxSelf -= 1.5F;
         margin += 1.5F;
      }

      boolean lethal = t >= tHealth;
      if (!lethal && t < minDmg) {
         return false;
      } else {
         return antiSuicide && s >= myHp - margin ? false : lethal || !(s > maxSelf);
      }
   }

   private Player pickTarget(Minecraft mc) {
      float range = this.getSetting("Target Range", 9.0F);
      List<Player> cands = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (p != mc.player && !AimAssist.isSelfLike(p) && p.isAlive() && !p.isSpectator() && !p.isCreative() && Targets.isTargetAllowed(p, mc.player) && !(mc.player.distanceTo(p) > range)) {
            cands.add(p);
         }
      }

      return Targets.getTarget(cands, mc.player) instanceof Player pl ? pl : null;
   }

   private boolean isBase(BlockState st) {
      return st.is(Blocks.OBSIDIAN) || st.is(Blocks.BEDROCK);
   }

   private boolean selfIn(Minecraft mc, AABB box) {
      if (mc.player == null) {
         return false;
      }

      AABB me = mc.player.getBoundingBox();
      if (me.intersects(box)) {
         return true;
      }

      Vec3 v = mc.player.getDeltaMovement();
      return me.move(v.x, v.y, v.z).intersects(box);
   }

   private boolean crystalSpotBlocked(Minecraft mc, List<Entity> ents, BlockPos base, long now) {
      Long recent = this.recentPlaces.get(base);
      if (recent != null && now - recent < this.crystalWaitMs()) {
         return true;
      }

      AABB spot = new AABB(base.getX(), base.getY() + 1.0, base.getZ(), base.getX() + 1.0, base.getY() + 3.0, base.getZ() + 1.0);
      if (this.selfIn(mc, spot)) {
         return true;
      }

      for (Entity e : ents) {
         if (!e.isRemoved() && e != mc.player && (!(e instanceof EndCrystal) || !this.brokenIds.containsKey(e.getId())) && e.getBoundingBox().intersects(spot)) {
            return true;
         }
      }

      return false;
   }

   private boolean blockedByEntity(Minecraft mc, List<Entity> ents, BlockPos pos) {
      AABB box = new AABB(pos);
      if (this.selfIn(mc, box)) {
         return true;
      }

      for (Entity e : ents) {
         if (!e.isRemoved() && !e.isSpectator() && e != mc.player && !(e instanceof ItemEntity) && e.getBoundingBox().intersects(box)) {
            return true;
         }
      }

      return false;
   }

   private static Vec3 closestPoint(AABB box, Vec3 p) {
      return new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY), Mth.clamp(p.z, box.minZ, box.maxZ));
   }

   private boolean lineClear(Minecraft mc, Vec3 from, Vec3 to) {
      HitResult r = mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player));
      return r.getType() == Type.MISS;
   }

   private boolean pickFace(Minecraft mc, CrystalAura.Cand c, Vec3 eyes, float reach, Player target) {
      double[] off = this.aimOffsets(c.key);
      float baseYaw = this.rotating ? this.curYaw : mc.player.getYRot();
      float basePitch = this.rotating ? this.curPitch : mc.player.getXRot();
      float bestScore = Float.MAX_VALUE;
      BlockPos bestClick = null;
      Direction bestFace = null;
      Vec3 bestAim = null;

      for (Direction d : Direction.values()) {
         BlockPos click;
         Direction face;
         if (c.fresh) {
            click = c.base.relative(d);
            BlockState ns = mc.level.getBlockState(click);
            if (ns.isAir()
               || ns.canBeReplaced()
               || ns.getBlock() instanceof LiquidBlock
               || this.unreliableSupport(click, ns)
               || click.equals(this.pendingObsidian) && System.currentTimeMillis() <= this.pendingUntil) {
               continue;
            }

            face = d.getOpposite();
         } else {
            click = c.base;
            face = d;
            if (mc.level.getBlockState(c.base.relative(d)).isSolid()) {
               continue;
            }
         }

         Vec3 point = facePoint(click, face, off[0], off[2]);
         if (!(eyes.distanceToSqr(point) > (double)reach * reach)) {
            if (!this.visibleFace(mc, eyes, click, face, point)) {
               point = facePoint(click, face, 0.0, 0.0);
               if (!this.visibleFace(mc, eyes, click, face, point) || eyes.distanceToSqr(point) > (double)reach * reach) {
                  continue;
               }
            }

            if (target != null) {
               AABB tbox = target.getBoundingBox().inflate(0.1);
               Optional<Vec3> entHit = tbox.clip(eyes, point);
               if (entHit.isPresent() && eyes.distanceToSqr(entHit.get()) < eyes.distanceToSqr(point)) {
                  continue;
               }
            }

            float[] a = angles(eyes, point);
            float score = Math.abs(Mth.wrapDegrees(a[0] - baseYaw)) + Math.abs(a[1] - basePitch);
            if (!c.fresh) {
               if (face == Direction.UP) {
                  score -= 10000.0F;
               }
            } else {
               score += 12.0F * (float)point.distanceTo(new Vec3(c.base.getX() + 0.5, c.base.getY() + 1.0, c.base.getZ() + 0.5));
            }

            if (score < bestScore) {
               bestScore = score;
               bestClick = click;
               bestFace = face;
               bestAim = point;
            }
         }
      }

      if (bestClick == null) {
         return false;
      }

      c.clickPos = bestClick;
      c.face = bestFace;
      c.aim = bestAim;
      return true;
   }

   private static Vec3 facePoint(BlockPos pos, Direction face, double a, double b) {
      double x = pos.getX() + 0.5 + face.getStepX() * 0.5;
      double y = pos.getY() + 0.5 + face.getStepY() * 0.5;
      double z = pos.getZ() + 0.5 + face.getStepZ() * 0.5;
      switch (face.getAxis()) {
         case X:
            y += a;
            z += b;
            break;
         case Y:
            x += a;
            z += b;
            break;
         case Z:
            x += a;
            y += b;
      }

      return new Vec3(x, y, z);
   }

   private boolean visibleFace(Minecraft mc, Vec3 eyes, BlockPos pos, Direction face, Vec3 point) {
      Vec3 dir = point.subtract(eyes);
      double len = dir.length();
      if (len < 1.0E-4) {
         return false;
      }

      Vec3 end = point.add(dir.scale(0.15 / len));
      HitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return r instanceof BlockHitResult b && b.getType() == Type.BLOCK && b.getBlockPos().equals(pos) && b.getDirection() == face;
   }

   private double[] aimOffsets(String key) {
      if (this.getSetting("Humanize", true) && this.getSetting("Randomize Aim Point", true)) {
         String spot = key.length() > 2 ? key.substring(2) : key;
         Random r = new Random(spot.hashCode() * 31L + this.planSeed);
         return new double[]{(r.nextDouble() * 2.0 - 1.0) * 0.15, (r.nextDouble() * 2.0 - 1.0) * 0.12, (r.nextDouble() * 2.0 - 1.0) * 0.15};
      } else {
         return new double[]{0.0, 0.0, 0.0};
      }
   }

   private static float[] angles(Vec3 eyes, Vec3 target) {
      Vec3 d = target.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      float yaw = (float)(Math.atan2(-d.x, d.z) * 180.0 / Math.PI);
      float pitch = (float)(-Math.atan2(d.y, h) * 180.0 / Math.PI);
      return new float[]{yaw, pitch};
   }

   private boolean hurtsFriend(Minecraft mc, Vec3 center) {
      List<Player> friends = Friends.explosionSensitive(mc, center, 12.0);
      if (friends.isEmpty()) {
         return false;
      }

      float max = (float)FriendManager.num("Max Friend Damage", 4.0);

      for (Player p : friends) {
         float d = this.damageAt(mc, center, p, false);
         if (!(d <= 0.0F) && (d > max || d >= p.getHealth() + p.getAbsorptionAmount())) {
            return true;
         }
      }

      return false;
   }

   private float rawDamage(Vec3 center, LivingEntity e) {
      double d = Math.sqrt(e.distanceToSqr(center)) / 12.0;
      if (d > 1.0) {
         return 0.0F;
      }

      double impact = 1.0 - d;
      return (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
   }

   private float damageAt(Minecraft mc, Vec3 center, LivingEntity e, boolean self) {
      return this.damageAt(mc, center, e, Vec3.ZERO, self);
   }

   private float damageAt(Minecraft mc, Vec3 center, LivingEntity e, Vec3 shift, boolean self) {
      double d = e.position().add(shift).distanceTo(center) / 12.0;
      if (d > 1.0) {
         return 0.0F;
      }

      double exposure = this.seenPercent(mc, center, e.getBoundingBox().move(shift), e);
      double impact = (1.0 - d) * exposure;
      float dmg = (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
      Difficulty diff = mc.level.getDifficulty();
      if (diff == Difficulty.PEACEFUL) {
         dmg = 0.0F;
      } else if (diff == Difficulty.EASY) {
         dmg = Math.min(dmg / 2.0F + 1.0F, dmg);
      } else if (diff == Difficulty.HARD) {
         dmg *= 1.5F;
      }

      float armor = (float)e.getAttributeValue(Attributes.ARMOR);
      float tough = (float)e.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float f = 2.0F + tough / 4.0F;
      float g = Mth.clamp(armor - dmg / f, armor * 0.2F, 20.0F);
      dmg *= 1.0F - g / 25.0F;
      dmg = this.applyProtection(mc, e, dmg);
      if (self) {
         dmg *= 1.1F;
      }

      return Math.max(0.0F, dmg);
   }

   private float applyProtection(Minecraft mc, LivingEntity e, float dmg) {
      try {
         for (MobEffectInstance inst : e.getActiveEffects()) {
            if (((MobEffect)inst.getEffect().value()).getDescriptionId().endsWith("resistance")) {
               int k = (inst.getAmplifier() + 1) * 5;
               dmg *= Math.max(0, 25 - k) / 25.0F;
            }
         }
      } catch (Throwable var15) {
      }

      try {
         Registry<Enchantment> reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
         Holder<Enchantment> prot = (Holder<Enchantment>)reg.get(Enchantments.PROTECTION).orElse(null);
         Holder<Enchantment> blast = (Holder<Enchantment>)reg.get(Enchantments.BLAST_PROTECTION).orElse(null);
         int epf = 0;
         EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

         for (EquipmentSlot slot : slots) {
            ItemStack st = e.getItemBySlot(slot);
            if (!st.isEmpty()) {
               if (prot != null) {
                  epf += EnchantmentHelper.getItemEnchantmentLevel(prot, st);
               }

               if (blast != null) {
                  epf += EnchantmentHelper.getItemEnchantmentLevel(blast, st) * 2;
               }
            }
         }

         dmg *= 1.0F - Mth.clamp(epf, 0, 20) / 25.0F;
      } catch (Throwable var14) {
      }

      return dmg;
   }

   private static Vec3 predictShift(LivingEntity e) {
      double sx = (e.getX() - e.xo) * 2.0;
      double sz = (e.getZ() - e.zo) * 2.0;
      double len = Math.sqrt(sx * sx + sz * sz);
      if (len > 0.7) {
         sx *= 0.7 / len;
         sz *= 0.7 / len;
      }

      return new Vec3(sx, 0.0, sz);
   }

   private float targetDamage(Minecraft mc, Vec3 center, Player target) {
      float now = this.damageAt(mc, center, target, Vec3.ZERO, false);
      if (now <= 0.0F) {
         return 0.0F;
      }

      Vec3 sh = predictShift(target);
      if (sh.lengthSqr() < 0.0025) {
         return now;
      }

      float pred = this.damageAt(mc, center, target, sh, false);
      return (now + pred) * 0.5F;
   }

   private double seenPercent(Minecraft mc, Vec3 center, AABB box, Entity e) {
      double dx = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double dy = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double dz = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      if (!(dx < 0.0) && !(dy < 0.0) && !(dz < 0.0)) {
         double xo = (1.0 - Math.floor(1.0 / dx) * dx) / 2.0;
         double zo = (1.0 - Math.floor(1.0 / dz) * dz) / 2.0;
         int hit = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += dx) {
            for (double y = 0.0; y <= 1.0; y += dy) {
               for (double z = 0.0; z <= 1.0; z += dz) {
                  Vec3 pt = new Vec3(Mth.lerp(x, box.minX, box.maxX) + xo, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + zo);
                  HitResult r = mc.level.clip(new ClipContext(pt, center, Block.COLLIDER, Fluid.NONE, e));
                  if (r.getType() == Type.MISS && (this.hypoBox == null || this.hypoBox.clip(pt, center).isEmpty())) {
                     hit++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0 : (double)hit / total;
      } else {
         return 0.0;
      }
   }

   private void execute(Minecraft mc, CrystalAura.Plan p, long now) {
      if (now >= this.reactUntil) {
         switch (p.type) {
            case BREAK:
               this.doBreak(mc, p, now);
               break;
            case CRYSTAL:
               this.doPlace(mc, p, now, Items.END_CRYSTAL);
               break;
            case OBSIDIAN:
               this.doPlace(mc, p, now, Items.OBSIDIAN);
         }
      }
   }

   private void noteExplosion(Vec3 pos, long now) {
      this.lastExplosionPos = pos;
      this.lastExplosionAt = now;
   }

   private static boolean blastProof(BlockState st) {
      return st.is(Blocks.OBSIDIAN) || st.is(Blocks.CRYING_OBSIDIAN) || st.is(Blocks.BEDROCK) || st.is(Blocks.NETHERITE_BLOCK) || st.is(Blocks.ANCIENT_DEBRIS) || st.is(Blocks.REINFORCED_DEEPSLATE);
   }

   private boolean unreliableSupport(BlockPos pos, BlockState st) {
      if (this.lastExplosionPos == null) {
         return false;
      } else {
         long now = System.currentTimeMillis();
         if (now - this.lastExplosionAt > this.crystalWaitMs() + 250L) {
            return false;
         } else {
            return blastProof(st) ? false : Vec3.atCenterOf(pos).distanceToSqr(this.lastExplosionPos) <= 16.0;
         }
      }
   }

   private boolean slotChangeAllowed(Minecraft mc) {
      return mc.player != null && mc.player.tickCount > this.lastActTick;
   }

   private long crystalWaitMs() {
      long ping = 50L;

      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
               ping = info.getLatency();
            }
         }
      } catch (Throwable var5) {
      }

      return Mth.clamp(ping + 25L, 50L, 300L);
   }

   private double aimError(Minecraft mc, Vec3 aim) {
      if (aim == null) {
         return 180.0;
      }

      Vec3 eyes = mc.player.getEyePosition();
      if (aim.subtract(eyes).lengthSqr() < 1.0E-6) {
         return 0.0;
      }

      float[] a = angles(eyes, aim);
      return Math.hypot(Mth.wrapDegrees(a[0] - this.sentYaw), a[1] - this.sentPitch);
   }

   private boolean aimHitsPlan(Minecraft mc, CrystalAura.Plan p) {
      if (p.type == CrystalAura.PlanType.BREAK) {
         return this.aimError(mc, p.aim) < 4.0;
      } else {
         BlockHitResult r = this.rayBlock(mc, 3.0);
         if (r == null) {
            return false;
         } else {
            return p.type == CrystalAura.PlanType.CRYSTAL ? r.getBlockPos().equals(p.pos) : r.getBlockPos().relative(r.getDirection()).equals(p.pos);
         }
      }
   }

   private Vec3 sentLook() {
      return AimAssist.getRotationVector(this.sentPitch, this.sentYaw);
   }

   private BlockHitResult rayBlock(Minecraft mc, double reach) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 end = eyes.add(this.sentLook().scale(reach));
      HitResult r = mc.level.clip(new ClipContext(eyes, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return r instanceof BlockHitResult b && b.getType() == Type.BLOCK ? b : null;
   }

   private void doBreak(Minecraft mc, CrystalAura.Plan p, long now) {
      if (now >= this.nextBreakAt) {
         if (mc.player.tickCount > this.lastInteractTick) {
            EndCrystal ec = p.crystal;
            if (ec != null && !ec.isRemoved() && ec.isAlive()) {
               float breakRange = this.getSetting("Break Range", 4.5F);
               Vec3 eyes = mc.player.getEyePosition();
               AABB cbox = ec.getBoundingBox().inflate(0.05);
               if (!cbox.contains(eyes)) {
                  Vec3 end = eyes.add(this.sentLook().scale(breakRange));
                  Optional<Vec3> hit = cbox.clip(eyes, end);
                  if (hit.isEmpty()) {
                     return;
                  }

                  if (!this.lineClear(mc, eyes, hit.get())) {
                     this.debug(mc, "break ray blocked by a block");
                     return;
                  }
               }

               mc.gameMode.attack(mc.player, ec);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.brokenIds.put(ec.getId(), now);
               BlockPos underBroken = ec.blockPosition().below();
               this.recentPlaces.remove(underBroken);
               if (underBroken.equals(this.lastCrystalBase)) {
                  this.lastCrystalBase = null;
               }

               this.lastActTick = mc.player.tickCount;
               this.lastInteractTick = mc.player.tickCount;
               this.noteExplosion(ec.position(), now);
               if (p.target != null && p.targetDmg >= 3.0F && this.checkTarget == null) {
                  this.checkTarget = p.target;
                  this.checkHealth = p.target.getHealth() + p.target.getAbsorptionAmount();
                  this.checkEst = p.targetDmg;
                  this.checkAt = now;
               }

               this.nextBreakAt = now + this.randMs("Break Delay", 0L, 20L);
               this.lastActionAt = now;
               this.lastSuccessAt = now;
               this.extendWindow(now);
            }
         }
      }
   }

   private void doPlace(Minecraft mc, CrystalAura.Plan p, long now, Item item) {
      boolean crystal = item == Items.END_CRYSTAL;
      if (now >= (crystal ? this.nextPlaceAt : this.nextObsidianAt)) {
         if (mc.player.tickCount > this.lastInteractTick) {
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (crystal && mc.player.getOffhandItem().is(Items.END_CRYSTAL) && !mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
               hand = InteractionHand.OFF_HAND;
            } else if (!mc.player.getMainHandItem().is(item)) {
               if (!this.getSetting("Auto Switch", true)) {
                  return;
               }

               int slot = this.findSlot(mc, item);
               if (slot == -1) {
                  return;
               }

               if (!this.slotChangeAllowed(mc)) {
                  return;
               }

               if (this.savedSlot == -1) {
                  this.savedSlot = InventoryHelper.getSelected(mc.player.getInventory());
               }

               InventoryHelper.setSelected(mc.player.getInventory(), slot);
               this.lastInteractTick = mc.player.tickCount;
               this.nextSwapAt = now + this.randMs("Swap Delay", 0L, 25L);
               this.lastActionAt = now;
               return;
            }

            if (now >= this.nextSwapAt) {
               boolean turning = this.aimError(mc, p.aim) > 3.0;
               BlockHitResult ray = this.rayBlock(mc, 3.0);
               if (ray == null) {
                  if (!turning) {
                     this.debug(mc, "aim ray hit nothing (out of reach?) for " + p.type);
                  }
               } else {
                  BlockPos hitPos = ray.getBlockPos();
                  boolean rightBlock = crystal ? hitPos.equals(p.pos) : hitPos.relative(ray.getDirection()).equals(p.pos);
                  if (!turning || rightBlock) {
                     BlockState hs = mc.level.getBlockState(hitPos);
                     if (!hs.isAir() && !(hs.getBlock() instanceof LiquidBlock)) {
                        if (crystal ? this.isBase(hs) : !hs.canBeReplaced() && !this.unreliableSupport(hitPos, hs)) {
                           if (crystal) {
                              if (!hitPos.equals(p.pos)) {
                                 this.debug(mc, "ray hits a different block than the base");
                                 return;
                              }
                           } else if (!hitPos.relative(ray.getDirection()).equals(p.pos)) {
                              this.debug(mc, "ray would put obsidian elsewhere");
                              return;
                           }

                           BlockHitResult hit = new BlockHitResult(ray.getLocation(), ray.getDirection(), hitPos, false);
                           InteractionResult res = mc.gameMode.useItemOn(mc.player, hand, hit);
                           if (!res.consumesAction()) {
                              this.debug(mc, "server/client refused " + p.type);
                           } else {
                              mc.player.swing(hand);
                              this.lastActTick = mc.player.tickCount;
                              this.lastInteractTick = mc.player.tickCount;
                              this.lastActionAt = now;
                              this.lastSuccessAt = now;
                              if (crystal) {
                                 this.recentPlaces.put(p.pos, now);
                                 this.lastCrystalBase = p.pos;
                                 this.lastCrystalAt = now;
                                 if (p.pos.equals(this.pendingObsidian)) {
                                    this.pendingObsidian = null;
                                 }

                                 this.nextPlaceAt = now + this.randMs("Place Delay", 0L, 25L);
                              } else {
                                 this.pendingObsidian = p.pos;
                                 this.pendingUntil = now + 900L;
                                 this.nextObsidianAt = now + this.randMs("Obsidian Delay", 0L, 25L);
                                 if (this.getSetting("Same Tick Crystal", true)) {
                                    this.chainCrystal(mc, p, now);
                                 }
                              }

                              this.extendWindow(now);
                           }
                        } else {
                           this.debug(mc, "ray hit an unusable block for " + p.type);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void chainCrystal(Minecraft mc, CrystalAura.Plan p, long now) {
      if (mc.player.tickCount > this.lastInteractTick) {
         if (mc.level.getBlockState(p.pos).is(Blocks.OBSIDIAN)) {
            if (mc.level.getBlockState(p.pos.above()).isAir()) {
               AABB spot = new AABB(p.pos.getX(), p.pos.getY() + 1.0, p.pos.getZ(), p.pos.getX() + 1.0, p.pos.getY() + 3.0, p.pos.getZ() + 1.0);
               if (!this.selfIn(mc, spot)) {
                  if (mc.level.getEntities((Entity)null, spot, e -> !e.isSpectator()).isEmpty()) {
                     BlockHitResult ray = this.rayBlock(mc, 3.0);
                     if (ray != null && ray.getBlockPos().equals(p.pos)) {
                        InteractionHand hand;
                        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) {
                           hand = InteractionHand.OFF_HAND;
                        } else {
                           if (!this.getSetting("Auto Switch", true)) {
                              return;
                           }

                           int slot = this.findSlot(mc, Items.END_CRYSTAL);
                           if (slot == -1) {
                              return;
                           }

                           InventoryHelper.setSelected(mc.player.getInventory(), slot);
                           hand = InteractionHand.MAIN_HAND;
                        }

                        BlockHitResult hit = new BlockHitResult(ray.getLocation(), ray.getDirection(), p.pos, false);
                        InteractionResult res = mc.gameMode.useItemOn(mc.player, hand, hit);
                        if (res.consumesAction()) {
                           mc.player.swing(hand);
                           this.recentPlaces.put(p.pos, now);
                           this.lastCrystalBase = p.pos;
                           this.lastCrystalAt = now;
                           this.pendingObsidian = null;
                           this.nextPlaceAt = now + this.randMs("Place Delay", 0L, 25L);
                           this.debug(mc, "obsidian + crystal same tick");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void extendWindow(long now) {
      if (this.tandemStart != 0L) {
         int dur = this.getSetting("Combo Duration", 1200);
         this.tandemUntil = Math.max(this.tandemUntil, Math.min(now + 400L, this.tandemStart + dur * 2L));
      }
   }

   private boolean canUse(Minecraft mc, Item item) {
      if (mc.player.getMainHandItem().is(item)) {
         return true;
      } else {
         return item == Items.END_CRYSTAL && mc.player.getOffhandItem().is(item) ? true : this.getSetting("Auto Switch", true) && this.findSlot(mc, item) != -1;
      }
   }

   private int findSlot(Minecraft mc, Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private void handleSwapBack(Minecraft mc, long now) {
      if (this.savedSlot >= 0 && mc.player != null) {
         if (!this.getSetting("Swap Back", true)) {
            this.savedSlot = -1;
         } else {
            if (this.swapBackAt == 0L) {
               this.swapBackAt = now + this.randMs("Swap Back Delay", 50L, 100L);
            }

            if (now >= this.swapBackAt && this.slotChangeAllowed(mc) && mc.player.tickCount > this.lastInteractTick) {
               this.lastInteractTick = mc.player.tickCount;
               this.restoreSlotNow(mc);
            }
         }
      }
   }

   private void restoreSlotNow(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      this.swapBackAt = 0L;
   }

   private void onFrame() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            long nowNs = System.nanoTime();
            double dtMs = this.lastFrameNs < 0L ? 16.0 : Math.min(100.0, (nowNs - this.lastFrameNs) / 1000000.0);
            this.lastFrameNs = nowNs;
            long now = System.currentTimeMillis();
            CrystalAura.Plan p = this.plan;
            if (p == null) {
               if (this.rotating) {
                  if (KillAura.isActive() && KillAura.getTarget() != null && now - this.lastPlanAt >= 80L) {
                     this.rotating = false;
                  } else if (!this.releaseAim && now - this.lastPlanAt < 80L) {
                     AimAssist.setSilentRotation(this.curYaw, this.curPitch);
                  } else {
                     this.returnToCamera(mc, dtMs);
                  }
               }
            } else {
               if (!this.rotating) {
                  this.curYaw = mc.player.getYRot();
                  this.curPitch = mc.player.getXRot();
                  this.rotating = true;
               }

               if (now < this.reactUntil) {
                  AimAssist.setSilentRotation(this.curYaw, this.curPitch);
               } else {
                  Vec3 dv = mc.player.getDeltaMovement();
                  Vec3 eyes = mc.player.getEyePosition().add(dv.x, mc.player.onGround() ? 0.0 : dv.y, dv.z);
                  Vec3 aim = p.aim;
                  if (aim != null) {
                     if (!(aim.subtract(eyes).lengthSqr() < 1.0E-6)) {
                        float[] ang = angles(eyes, aim);
                        float targetYaw = ang[0];
                        float targetPitch = Mth.clamp(ang[1], -90.0F, 90.0F);
                        boolean human = this.getSetting("Humanize", true);
                        double tSec = nowNs / 1.0E9;
                        if (human) {
                           int jit = this.getSetting("Jitter", 20);
                           float amp = Math.min(0.5F, jit / 100.0F * 0.5F);
                           targetYaw += (float)(noise(tSec, 0) * amp);
                           targetPitch += (float)(noise(tSec, 1) * amp * 0.6);
                           int spdVar = this.getSetting("Speed Variation", 25);
                           if (now >= this.nextSpeedAt) {
                              this.nextSpeedAt = now + 250L + (long)(this.rng.nextFloat() * 650.0F);
                              this.speedMulGoal = 1.0 + (this.rng.nextDouble() * 2.0 - 1.0) * (spdVar / 100.0) * 0.5;
                           }

                           this.speedMul = this.speedMul + (this.speedMulGoal - this.speedMul) * Math.min(1.0, dtMs / 1000.0 * 3.0);
                        } else {
                           this.speedMul = 1.0;
                        }

                        float yawDiff = Mth.wrapDegrees(targetYaw - this.curYaw);
                        float pitchDiff = targetPitch - this.curPitch;
                        int speed = this.getSetting("Rotation Speed", 16);
                        double factor = 1.0 - Math.exp(-(speed * 2.0) * (dtMs / 1000.0) * this.speedMul);
                        double minStep = 360.0 * (dtMs / 1000.0);
                        double stepY = yawDiff * factor;
                        double stepP = pitchDiff * factor;
                        double mag0 = Math.sqrt(stepY * stepY + stepP * stepP);
                        double full = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
                        if (mag0 < minStep && full > 1.0E-6) {
                           double k = Math.min(full, minStep) / full;
                           stepY = yawDiff * k;
                           stepP = pitchDiff * k;
                        }

                        int maxTurn = this.getSetting("Max Turn Speed", 900);
                        double maxStep = maxTurn * (dtMs / 1000.0);
                        double mag = Math.sqrt(stepY * stepY + stepP * stepP);
                        if (mag > maxStep && mag > 1.0E-6) {
                           stepY *= maxStep / mag;
                           stepP *= maxStep / mag;
                        }

                        if (Math.abs(yawDiff) < 0.05F) {
                           stepY = yawDiff;
                        }

                        if (Math.abs(pitchDiff) < 0.05F) {
                           stepP = pitchDiff;
                        }

                        float newYaw = this.curYaw + (float)stepY;
                        float newPitch = Mth.clamp(this.curPitch + (float)stepP, -90.0F, 90.0F);
                        if (this.getSetting("GCD Fix", true)) {
                           newYaw = RotationGcd.snapYaw(this.curYaw, newYaw);
                           newPitch = RotationGcd.snapPitch(this.curPitch, newPitch);
                        }

                        this.curYaw = Mth.wrapDegrees(newYaw);
                        this.curPitch = newPitch;
                        AimAssist.setSilentRotation(this.curYaw, this.curPitch);
                     }
                  }
               }
            }
         } else {
            this.rotating = false;
         }
      }
   }

   private void returnToCamera(Minecraft mc, double dtMs) {
      float visYaw = mc.player.getYRot();
      float visPitch = mc.player.getXRot();
      float yawDiff = Mth.wrapDegrees(visYaw - this.curYaw);
      float pitchDiff = Mth.wrapDegrees(visPitch - this.curPitch);
      if (Math.abs(yawDiff) < 0.5F && Math.abs(pitchDiff) < 0.5F) {
         this.rotating = false;
         AimAssist.clearSilentRotation();
      } else {
         int speed = this.getSetting("Rotation Speed", 16);
         double factor = 1.0 - Math.exp(-(speed * 2.0) * (dtMs / 1000.0));
         this.curYaw = Mth.wrapDegrees(this.curYaw + (float)(yawDiff * factor));
         this.curPitch = Mth.clamp(this.curPitch + (float)(pitchDiff * factor), -90.0F, 90.0F);
         AimAssist.setSilentRotation(this.curYaw, this.curPitch);
         AimAssist.clearSilentRotation();
      }
   }

   private static double noise(double t, int seed) {
      return 0.6 * Math.sin(t * 1.3 + seed * 7.1) + 0.4 * Math.sin(t * 2.9 + seed * 3.3 + 1.7);
   }

   private long randMs(String key, long loDefault, long hiDefault) {
      long lo = this.getSetting(key + "_lo", loDefault);
      long hi = this.getSetting(key + "_hi", hiDefault);
      if (hi < lo) {
         hi = lo;
      }

      return lo == hi ? lo : lo + (long)(this.rng.nextDouble() * (hi - lo + 1L));
   }

   private static final class Cand {
      BlockPos base;
      boolean fresh;
      BlockPos clickPos;
      Direction face;
      Vec3 aim;
      float ub;
      float t;
      float s;
      float score;
      float turn;
      float yawTurn;
      String key;
   }

   private static final class Plan {
      CrystalAura.PlanType type;
      String key;
      BlockPos pos;
      BlockPos clickPos;
      Direction face;
      EndCrystal crystal;
      Player target;
      Vec3 aim;
      float targetDmg;
      float selfDmg;
   }

   private enum PlanType {
      BREAK,
      CRYSTAL,
      OBSIDIAN;
   }
}
