package need.clarity.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import need.clarity.PerFrameEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.Targets;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AutoMace extends ModuleBase {
   public static final String KILL_AURA = "Kill Aura";
   public static final String TRIGGERBOT = "Triggerbot";
   private final Random rng = new Random();
   private int savedSlot = -1;
   private int lastSetSlot = -1;
   private long swapBackAt = 0L;
   private LivingEntity slamTarget = null;
   private int slamTick = -1;
   private boolean aiming = false;
   private float aimYaw;
   private float aimPitch;
   private LivingEntity target = null;

   public AutoMace() {
      super("Auto Mace", "Combat");
      PerFrameEvents.register(this::onFrame);
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.restoreSlot(Minecraft.getInstance());
      this.stopAim();
      this.reset();
   }

   private void reset() {
      this.slamTarget = null;
      this.slamTick = -1;
      this.savedSlot = -1;
      this.lastSetSlot = -1;
      this.swapBackAt = 0L;
      this.target = null;
      this.aiming = false;
   }

   private void onFrame() {
      if (this.isEnabled() && this.aiming) {
         AimAssist.setSilentRotation(this.aimYaw, this.aimPitch);
      }
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         long now = System.currentTimeMillis();
         if (this.savedSlot != -1 && this.lastSetSlot != -1 && InventoryHelper.getSelected(mc.player.getInventory()) != this.lastSetSlot) {
            this.savedSlot = -1;
            this.swapBackAt = 0L;
         }

         if (this.slamTarget != null) {
            this.finishStunSlam(mc);
         } else {
            if (this.swapBackAt > 0L && now >= this.swapBackAt) {
               this.restoreSlot(mc);
            }

            if (mc.screen == null && !mc.player.isUsingItem() && this.fallReady(mc)) {
               int mace = this.findMaceSlot(mc);
               if (mace < 0) {
                  this.target = null;
                  this.stopAim();
               } else {
                  if ("Triggerbot".equals(this.getSetting("Mode", "Kill Aura"))) {
                     this.stopAim();
                     this.target = null;
                     this.tickTriggerbot(mc, mace);
                  } else {
                     this.tickKillAura(mc, mace);
                  }
               }
            } else {
               this.target = null;
               this.stopAim();
            }
         }
      }
   }

   private boolean fallReady(Minecraft mc) {
      if (!this.getSetting("Only Off Ground", true)) {
         return true;
      } else if (mc.player.onGround()) {
         return false;
      } else if (!mc.player.getAbilities().flying && !mc.player.isFallFlying()) {
         double fallen = mc.player.fallDistance;
         return fallen >= this.getSetting("Min Fall Distance", 1.5);
      } else {
         return false;
      }
   }

   private void tickTriggerbot(Minecraft mc, int mace) {
      if (mc.hitResult instanceof EntityHitResult ehr) {
         if (ehr.getEntity() instanceof LivingEntity t && validTarget(mc, t)) {
            if (this.readyToHit(mc, t)) {
               this.hit(mc, t, mace);
            }
         }
      }
   }

   private void tickKillAura(Minecraft mc, int mace) {
      double range = this.getSetting("Range", 4.0);
      LivingEntity t = this.findTarget(mc, range);
      this.target = t;
      if (t == null) {
         this.stopAim();
      } else {
         float[] sent = this.aiming ? new float[]{this.aimYaw, this.aimPitch} : null;
         float[] want = lookAt(mc.player.getEyePosition(), aimPoint(mc, t));
         this.stepAim(mc, want[0], want[1]);
         if (sent != null && this.readyToHit(mc, t)) {
            if (rayHits(mc, sent[0], sent[1], t)) {
               this.hit(mc, t, mace);
            }
         }
      }
   }

   private boolean readyToHit(Minecraft mc, LivingEntity t) {
      if (isShielding(t) && !this.canStunSlam(mc, t) && this.getSetting("Check Shield", true)) {
         return false;
      }

      float cd = this.getSetting("Cooldown Threshold", 0.9F);
      return mc.player.getAttackStrengthScale(0.0F) >= cd;
   }

   private void hit(Minecraft mc, LivingEntity t, int mace) {
      if (this.canStunSlam(mc, t)) {
         if (willStayAirborne(mc)) {
            int axe = findAxeSlot(mc);
            this.select(mc, axe);
            Targets.onHit(t);
            mc.gameMode.attack(mc.player, t);
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.slamTarget = t;
            this.slamTick = mc.player.tickCount;
            this.swapBackAt = 0L;
         }
      } else {
         this.select(mc, mace);
         Targets.onHit(t);
         mc.gameMode.attack(mc.player, t);
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.scheduleSwapBack();
      }
   }

   private void finishStunSlam(Minecraft mc) {
      if (mc.player.tickCount > this.slamTick) {
         LivingEntity t = this.slamTarget;
         this.slamTarget = null;
         int mace = this.findMaceSlot(mc);
         if (mace >= 0 && validTarget(mc, t) && inReach(mc, t)) {
            this.select(mc, mace);
            mc.gameMode.attack(mc.player, t);
            mc.player.swing(InteractionHand.MAIN_HAND);
         }

         this.scheduleSwapBack();
      }
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
   }

   private void scheduleSwapBack() {
      if (this.savedSlot != -1 && this.getSetting("Swap Back", true)) {
         this.swapBackAt = System.currentTimeMillis() + this.randomMs("Swap Back Delay", 50, 150);
      } else if (!this.getSetting("Swap Back", true)) {
         this.savedSlot = -1;
      }
   }

   private static boolean willStayAirborne(Minecraft mc) {
      LocalPlayer p = mc.player;
      if (p.onGround()) {
         return false;
      }

      double vy = p.getDeltaMovement().y;
      AABB next = p.getBoundingBox().move(0.0, Math.min(vy, 0.0) - 0.1, 0.0);
      return !mc.level.noCollision(p, next) ? false : p.fallDistance + Math.max(0.0, -vy) > 1.5;
   }

   private static boolean inReach(Minecraft mc, LivingEntity t) {
      double r = mc.player.entityInteractionRange();
      Vec3 eyes = mc.player.getEyePosition();
      AABB b = t.getBoundingBox();
      double x = Mth.clamp(eyes.x, b.minX, b.maxX);
      double y = Mth.clamp(eyes.y, b.minY, b.maxY);
      double z = Mth.clamp(eyes.z, b.minZ, b.maxZ);
      return eyes.distanceToSqr(x, y, z) <= r * r;
   }

   private boolean canStunSlam(Minecraft mc, LivingEntity t) {
      return this.getSetting("Stun Slam", true) && isShielding(t) && findAxeSlot(mc) >= 0;
   }

   private static boolean isShielding(LivingEntity t) {
      return t.isBlocking() ? true : t.isUsingItem() && t.getUseItem().is(Items.SHIELD) && t.getTicksUsingItem() >= 5;
   }

   private static int findAxeSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && s.is(ItemTags.AXES)) {
            return i;
         }
      }

      return -1;
   }

   private LivingEntity findTarget(Minecraft mc, double range) {
      double aimRangeSq = (range + 1.0) * (range + 1.0);
      List<LivingEntity> c = new ArrayList<>();

      for (Player p : mc.level.players()) {
         if (validTarget(mc, p) && mc.player.distanceToSqr(p) <= aimRangeSq) {
            c.add(p);
         }
      }

      if (this.target != null && c.contains(this.target)) {
         return this.target;
      } else {
         return c.isEmpty() ? null : Targets.getTarget(c, mc.player);
      }
   }

   private static boolean validTarget(Minecraft mc, LivingEntity e) {
      return e != null && e.isAlive() && !e.isRemoved() && Targets.isTargetAllowed(e, mc.player);
   }

   private static Vec3 aimPoint(Minecraft mc, LivingEntity t) {
      Vec3 eyes = mc.player.getEyePosition();
      AABB b = t.getBoundingBox().deflate(0.1);
      return new Vec3(Mth.clamp(eyes.x, b.minX, b.maxX), Mth.clamp(eyes.y, b.minY, b.maxY), Mth.clamp(eyes.z, b.minZ, b.maxZ));
   }

   private void stepAim(Minecraft mc, float tYaw, float tPitch) {
      float fromYaw;
      float fromPitch;
      if (this.aiming) {
         fromYaw = this.aimYaw;
         fromPitch = this.aimPitch;
      } else {
         fromYaw = mc.player.getYRot();
         fromPitch = mc.player.getXRot();
      }

      float speed = Math.max(1.0F, this.getSetting("Rotation Speed", 180.0F));
      this.aimYaw = step(fromYaw, tYaw, speed);
      this.aimPitch = Mth.clamp(step(fromPitch, tPitch, speed), -90.0F, 90.0F);
      this.aiming = true;
      AimAssist.setSilentRotation(this.aimYaw, this.aimPitch);
   }

   private void stopAim() {
      if (this.aiming) {
         AimAssist.clearSilentRotation();
      }

      this.aiming = false;
   }

   private static float step(float from, float to, float max) {
      float d = Mth.wrapDegrees(to - from);
      return Math.abs(d) <= max ? from + d : from + Math.signum(d) * max;
   }

   private static boolean rayHits(Minecraft mc, float yaw, float pitch, Entity t) {
      Vec3 eyes = mc.player.getEyePosition();
      double reach = mc.player.entityInteractionRange();
      Vec3 end = eyes.add(AimAssist.getRotationVector(pitch, yaw).scale(reach));
      Optional<Vec3> hit = t.getBoundingBox().inflate(t.getPickRadius()).clip(eyes, end);
      if (hit.isEmpty()) {
         return false;
      }

      double d = eyes.distanceToSqr(hit.get());
      BlockHitResult block = mc.level.clip(new ClipContext(eyes, hit.get(), Block.COLLIDER, Fluid.NONE, mc.player));
      return block.getType() == Type.MISS || eyes.distanceToSqr(block.getLocation()) >= d;
   }

   private static float[] lookAt(Vec3 eyes, Vec3 p) {
      Vec3 d = p.subtract(eyes);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      return new float[]{(float)(Math.atan2(-d.x, d.z) * 180.0 / Math.PI), (float)(Math.atan2(d.y, h) * -180.0 / Math.PI)};
   }

   private int findMaceSlot(Minecraft mc) {
      Inventory inv = mc.player.getInventory();
      String type = this.getSetting("Mace", "Dynamic");

      boolean wantDensity = switch (type) {
         case "Density" -> true;
         case "Dynamic" -> mc.player.fallDistance >= this.getSetting("Density Fall Distance", 5.0);
         default -> false;
      };
      Holder<Enchantment> want = null;
      if (!"Any".equals(type)) {
         try {
            Registry<Enchantment> reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            want = (Holder<Enchantment>)(wantDensity ? reg.get(Enchantments.DENSITY) : reg.get(Enchantments.BREACH)).orElse(null);
         } catch (Exception var9) {
         }
      }

      int fallback = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && s.is(Items.MACE)) {
            if (want == null) {
               return i;
            }

            if (EnchantmentHelper.getItemEnchantmentLevel(want, s) > 0) {
               return i;
            }

            if (fallback < 0) {
               fallback = i;
            }
         }
      }

      return fallback;
   }

   private void restoreSlot(Minecraft mc) {
      if (this.savedSlot >= 0 && mc.player != null) {
         InventoryHelper.setSelected(mc.player.getInventory(), this.savedSlot);
      }

      this.savedSlot = -1;
      this.lastSetSlot = -1;
      this.swapBackAt = 0L;
   }

   private long randomMs(String key, int loDef, int hiDef) {
      int lo = this.getSetting(key + "_lo", loDef);
      int hi = this.getSetting(key + "_hi", hiDef);
      return hi <= lo ? lo : lo + this.rng.nextInt(hi - lo + 1);
   }
}
