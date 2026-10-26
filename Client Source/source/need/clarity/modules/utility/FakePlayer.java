package need.clarity.modules.utility;

import com.mojang.authlib.GameProfile;
import java.util.Locale;
import java.util.UUID;
import need.clarity.PerPacketSendEvents;
import need.clarity.Unload;
import need.clarity.event.EventPacketSend;
import need.clarity.modules.ModuleBase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

public class FakePlayer extends ModuleBase {
   private static final int ENTITY_ID = -1337420;
   private static final UUID FAKE_UUID = UUID.fromString("c1a7f00d-0000-4000-8000-00000000fa4e");
   public static FakePlayer INSTANCE;
   private RemotePlayer fake;
   private ClientLevel spawnedIn;
   private double homeX;
   private double homeY;
   private double homeZ;
   private int ticks;
   private int deadTicks = -1;
   private double vx;
   private double vy;
   private double vz;
   private boolean onGround = true;
   private double fallStartY = Double.NaN;
   private int sinceHurt = 100;
   private float lastHurtDamage = 0.0F;
   private float absorption = 0.0F;
   private int totemsLeft = 0;
   private int regenTicks = 0;
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public FakePlayer() {
      super("Fake Player", "Utility");
      INSTANCE = this;
      PerPacketSendEvents.register(this::onPacketSend);
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)c -> {
         if (!Unload.isUnloaded()) {
            try {
               if (this.isEnabled()) {
                  this.simulate();
               }
            } catch (Throwable var3) {
            }
         }
      });
   }

   public static boolean isFake(Entity e) {
      FakePlayer f = INSTANCE;
      return f != null && f.fake != null && e == f.fake;
   }

   public static boolean isFakeId(int id) {
      FakePlayer f = INSTANCE;
      return f != null && f.fake != null && id == -1337420;
   }

   @Override
   public void onEnable() {
      this.spawn();
   }

   @Override
   public void onDisable() {
      this.despawn();
   }

   private void onPacketSend(EventPacketSend event) {
      if (this.fake != null) {
         try {
            Packet<?> p = event.getPacket();
            if (p instanceof ServerboundAttackPacket atk) {
               if (atk.entityId() == -1337420) {
                  event.cancel();
               }
            } else if (p instanceof ServerboundInteractPacket it && it.entityId() == -1337420) {
               event.cancel();
            }
         } catch (Throwable var5) {
         }
      }
   }

   private void spawn() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer me = mc.player;
      ClientLevel level = mc.level;
      if (me != null && level != null) {
         this.despawn();
         String name = this.getSetting("Name", "FakePlayer");
         if (name == null || name.isBlank()) {
            name = "FakePlayer";
         }

         if (name.length() > 16) {
            name = name.substring(0, 16);
         }

         RemotePlayer p = new FakePlayer.Body(level, new GameProfile(FAKE_UUID, name));
         p.setId(-1337420);
         this.homeX = me.getX();
         this.homeY = me.getY();
         this.homeZ = me.getZ();
         p.snapTo(this.homeX, this.homeY, this.homeZ, me.getYRot(), me.getXRot());
         p.yHeadRot = p.yHeadRotO = me.getYRot();
         p.yBodyRot = p.yBodyRotO = me.getYRot();
         p.setOldPosAndRot();
         if (this.getSetting("Copy Gear", true)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
               try {
                  p.setItemSlot(slot, me.getItemBySlot(slot).copy());
               } catch (Throwable var11) {
               }
            }
         }

         p.setHealth(p.getMaxHealth());
         level.addEntity(p);
         this.fake = p;
         this.spawnedIn = level;
         this.ticks = 0;
         this.deadTicks = -1;
         this.vx = this.vy = this.vz = 0.0;
         this.onGround = true;
         this.fallStartY = Double.NaN;
         this.sinceHurt = 100;
         this.lastHurtDamage = 0.0F;
         this.absorption = 0.0F;
         this.regenTicks = 0;
         this.refillTotems();
      }
   }

   private void refillTotems() {
      this.totemsLeft = (int)Math.round(Math.max(0.0, Math.min(64.0, this.getSetting("Totems", 0.0))));
      if (this.totemsLeft > 0 && this.fake != null) {
         this.fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
      }
   }

   private void despawn() {
      Minecraft mc = Minecraft.getInstance();
      if (this.fake != null && mc.level != null && mc.level == this.spawnedIn) {
         try {
            mc.level.removeEntity(-1337420, RemovalReason.DISCARDED);
         } catch (Throwable var3) {
         }
      }

      this.fake = null;
      this.spawnedIn = null;
   }

   private void simulate() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         this.fake = null;
         this.spawnedIn = null;
      } else if (this.fake != null && mc.level == this.spawnedIn && !this.fake.isRemoved()) {
         this.ticks++;
         this.sinceHurt++;
         if (this.deadTicks >= 0) {
            this.fake.deathTime = Math.min(20, this.deadTicks);
            if (++this.deadTicks > 30) {
               this.respawn();
            }
         } else {
            this.fake.setOldPosAndRot();
            this.fake.yBodyRotO = this.fake.yBodyRot;
            this.fake.yHeadRotO = this.fake.yHeadRot;
            double tx = this.homeX;
            double tz = this.homeZ;
            float speed = this.getSetting("Speed", 1.0F);
            double t = this.ticks * 0.05 * speed;
            String mode = String.valueOf(this.getSetting("Movement", "Still"));
            switch (mode) {
               case "Circle":
                  tx += Math.cos(t * 2.2) * 2.5;
                  tz += Math.sin(t * 2.2) * 2.5;
                  break;
               case "Strafe":
                  tx += Math.sin(t * 3.0) * 2.0;
                  break;
               case "Random":
                  tx += Math.sin(t * 2.1) * 1.6 + Math.sin(t * 5.3 + 1.7) * 0.6;
                  tz += Math.cos(t * 1.7) * 1.6 + Math.sin(t * 4.1 + 0.4) * 0.6;
            }

            double dx = tx - this.fake.getX();
            double dz = tz - this.fake.getZ();
            double dist = Math.hypot(dx, dz);
            double ix = 0.0;
            double iz = 0.0;
            boolean walk = !"Still".equals(mode) || this.getSetting("Return Home", true) && dist > 0.35;
            if (walk && dist > 0.05) {
               ix = dx / dist;
               iz = dz / dist;
            }

            boolean sprint = this.getSetting("Sprint", false) && (ix != 0.0 || iz != 0.0);
            double accel = this.onGround ? 0.1 * (sprint ? 1.3 : 1.0) * Math.min(1.5, speed) : 0.02;
            double slow = Math.min(1.0, dist / 0.6);
            this.vx += ix * accel * slow;
            this.vz += iz * accel * slow;
            if (this.getSetting("Jump", false) && this.onGround) {
               this.vy = 0.42;
            }

            double yBefore = this.fake.getY();
            this.fake.setDeltaMovement(this.vx, this.vy, this.vz);
            this.fake.move(MoverType.SELF, new Vec3(this.vx, this.vy, this.vz));
            Vec3 after = this.fake.getDeltaMovement();
            boolean wasGround = this.onGround;
            this.onGround = this.fake.onGround();
            if (this.fake.horizontalCollision) {
               if (Math.abs(after.x) < 1.0E-9) {
                  this.vx = 0.0;
               }

               if (Math.abs(after.z) < 1.0E-9) {
                  this.vz = 0.0;
               }
            }

            if (this.onGround && this.vy < 0.0) {
               this.vy = 0.0;
            }

            if (this.fake.verticalCollision && this.vy > 0.0) {
               this.vy = 0.0;
            }

            this.vy = (this.vy - 0.08) * 0.98;
            double fr = this.onGround ? 0.546 : 0.91;
            this.vx *= fr;
            this.vz *= fr;
            if (Math.abs(this.vx) < 0.003) {
               this.vx = 0.0;
            }

            if (Math.abs(this.vz) < 0.003) {
               this.vz = 0.0;
            }

            this.fake.setDeltaMovement(this.vx, this.vy, this.vz);
            if (!this.onGround) {
               if (Double.isNaN(this.fallStartY) || this.fake.getY() > this.fallStartY) {
                  this.fallStartY = Math.max(this.fake.getY(), yBefore);
               }
            } else if (!wasGround && !Double.isNaN(this.fallStartY)) {
               double fell = this.fallStartY - this.fake.getY();
               this.fallStartY = Double.NaN;
               int dmg = (int)Math.ceil(fell - 3.0);
               if (dmg > 0 && this.getSetting("Take Damage", true) && this.getSetting("Fall Damage", true)) {
                  this.damage(dmg, 0.0F, this.getSetting("Sounds", true) ? (dmg > 4 ? SoundEvents.PLAYER_BIG_FALL : SoundEvents.PLAYER_SMALL_FALL) : null);
               }
            } else {
               this.fallStartY = Double.NaN;
            }

            double mx = this.fake.getX() - this.fake.xo;
            double mz = this.fake.getZ() - this.fake.zo;
            if (this.getSetting("Face You", true)) {
               LocalPlayer me = mc.player;
               double lx = me.getX() - this.fake.getX();
               double lz = me.getZ() - this.fake.getZ();
               double ly = me.getEyeY() - this.fake.getEyeY();
               float yaw = (float)Math.toDegrees(Math.atan2(lz, lx)) - 90.0F;
               float pitch = (float)(-Math.toDegrees(Math.atan2(ly, Math.hypot(lx, lz))));
               this.fake.setYRot(yaw);
               this.fake.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
               this.fake.yHeadRot = yaw;
               this.fake.yBodyRot = Mth.approachDegrees(this.fake.yBodyRot, yaw, 20.0F);
            } else if (mx * mx + mz * mz > 1.0E-5) {
               float yaw = (float)Math.toDegrees(Math.atan2(mz, mx)) - 90.0F;
               this.fake.setYRot(yaw);
               this.fake.yHeadRot = yaw;
               this.fake.yBodyRot = Mth.approachDegrees(this.fake.yBodyRot, yaw, 25.0F);
            }

            this.fake.setSprinting(sprint);
            if (this.regenTicks > 0) {
               this.regenTicks--;
               if (this.regenTicks % 25 == 0) {
                  this.heal(1.0F);
               }
            } else if (this.getSetting("Regen", true) && this.ticks % 40 == 0) {
               this.heal(1.0F);
            }

            if (this.fake.getY() < mc.level.getMinY() - 64) {
               this.respawn();
            }
         }
      } else {
         this.spawn();
      }
   }

   private void heal(float hp) {
      if (this.fake.getHealth() < this.fake.getMaxHealth()) {
         this.fake.setHealth(Math.min(this.fake.getMaxHealth(), this.fake.getHealth() + hp));
      }
   }

   private void respawn() {
      this.deadTicks = -1;
      this.fake.deathTime = 0;
      this.fake.hurtTime = 0;
      this.fake.setHealth(this.fake.getMaxHealth());
      this.absorption = 0.0F;
      this.regenTicks = 0;
      this.vx = this.vy = this.vz = 0.0;
      this.fake.snapTo(this.homeX, this.homeY, this.homeZ, this.fake.getYRot(), this.fake.getXRot());
      this.fake.setOldPosAndRot();
      this.refillTotems();
   }

   public static boolean onLocalAttack(Object target) {
      FakePlayer f = INSTANCE;
      if (f != null && f.fake != null && target == f.fake) {
         if (f.isEnabled()) {
            try {
               f.attacked(Minecraft.getInstance().player);
            } catch (Throwable var3) {
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void attacked(LocalPlayer me) {
      if (me != null && this.deadTicks < 0) {
         Minecraft mc = Minecraft.getInstance();
         boolean sounds = this.getSetting("Sounds", true);
         float cooldown = me.getAttackStrengthScale(0.5F);
         float base = (float)me.getAttributeValue(Attributes.ATTACK_DAMAGE);
         ItemStack weapon = me.getMainHandItem();
         float ench = enchantLevel(mc, Enchantments.SHARPNESS, weapon) > 0 ? 0.5F * enchantLevel(mc, Enchantments.SHARPNESS, weapon) + 0.5F : 0.0F;
         float dmg = base * (0.2F + cooldown * cooldown * 0.8F);
         ench *= cooldown;
         boolean strong = cooldown > 0.9F;
         boolean sprintHit = me.isSprinting() && strong;
         boolean crit = strong && me.fallDistance > 0.0 && !me.onGround() && !me.onClimbable() && !me.isInWater() && !me.isPassenger() && !me.isSprinting() && !me.hasEffect(MobEffects.BLINDNESS);
         if (crit) {
            dmg *= 1.5F;
         }

         dmg += ench;
         float yaw = (float)Math.toDegrees(Math.atan2(me.getZ() - this.fake.getZ(), me.getX() - this.fake.getX())) - this.fake.getYRot();
         boolean hurt;
         if (this.getSetting("Take Damage", true)) {
            hurt = this.damage(this.afterArmor(mc, dmg), yaw, sounds ? SoundEvents.PLAYER_HURT : null);
         } else {
            hurt = this.sinceHurt >= 10;
            if (hurt) {
               this.sinceHurt = 0;
               this.fake.animateHurt(yaw);
               this.fake.hurtTime = this.fake.hurtDuration = 10;
            }
         }

         if (sounds) {
            SoundEvent s = !hurt
               ? SoundEvents.PLAYER_ATTACK_NODAMAGE
               : (crit ? SoundEvents.PLAYER_ATTACK_CRIT : (sprintHit ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : (strong ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK)));
            mc.level.playLocalSound(this.fake.getX(), this.fake.getY(), this.fake.getZ(), s, SoundSource.PLAYERS, 1.0F, 1.0F, false);
         }

         if (hurt) {
            try {
               if (crit) {
                  mc.particleEngine.createTrackingEmitter(this.fake, ParticleTypes.CRIT);
               }

               if (ench > 0.0F) {
                  mc.particleEngine.createTrackingEmitter(this.fake, ParticleTypes.ENCHANTED_HIT);
               }
            } catch (Throwable var19) {
            }
         }

         if (hurt && this.getSetting("Take KB", true)) {
            float mult = this.getSetting("KB Multiplier", 1.0F) * (1.0F - this.knockbackResistance());
            this.knockback(0.4 * mult, me.getX() - this.fake.getX(), me.getZ() - this.fake.getZ());
            int kbLevel = enchantLevel(mc, Enchantments.KNOCKBACK, weapon);
            float extra = (sprintHit ? 1.0F : 0.0F) + kbLevel;
            if (extra > 0.0F) {
               double r = Math.toRadians(me.getYRot());
               this.knockback(extra * 0.5 * mult, Math.sin(r), -Math.cos(r));
            }
         }
      }
   }

   private void knockback(double strength, double x, double z) {
      if (!(strength <= 0.0)) {
         double len = Math.hypot(x, z);
         if (len < 1.0E-4) {
            x = Math.random() - 0.5;
            z = Math.random() - 0.5;
            len = Math.hypot(x, z);
         }

         double nx = x / len * strength;
         double nz = z / len * strength;
         this.vx = this.vx / 2.0 - nx;
         this.vz = this.vz / 2.0 - nz;
         if (this.onGround) {
            this.vy = Math.min(0.4, this.vy / 2.0 + strength);
         }

         this.onGround = false;
      }
   }

   private boolean damage(float dmg, float yaw, SoundEvent sound) {
      if (dmg <= 0.0F) {
         return false;
      }

      boolean fresh = this.sinceHurt >= 10;
      float applied;
      if (!fresh) {
         if (dmg <= this.lastHurtDamage) {
            return false;
         }

         applied = dmg - this.lastHurtDamage;
         this.lastHurtDamage = dmg;
      } else {
         applied = dmg;
         this.lastHurtDamage = dmg;
         this.sinceHurt = 0;
         this.fake.animateHurt(yaw);
         this.fake.hurtTime = this.fake.hurtDuration = 10;
      }

      float fromAbs = Math.min(this.absorption, applied);
      this.absorption -= fromAbs;
      applied -= fromAbs;
      float hp = this.fake.getHealth() - applied;
      Minecraft mc = Minecraft.getInstance();
      if (hp <= 0.0F) {
         if (this.totemsLeft > 0) {
            this.popTotem();
            return true;
         }

         this.fake.setHealth(0.0F);
         this.deadTicks = 0;
         if (sound != null) {
            mc.level.playLocalSound(this.fake.getX(), this.fake.getY(), this.fake.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F, false);
         }
      } else {
         this.fake.setHealth(hp);
         if (sound != null && fresh) {
            mc.level.playLocalSound(this.fake.getX(), this.fake.getY(), this.fake.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F, false);
         }
      }

      return true;
   }

   private void popTotem() {
      Minecraft mc = Minecraft.getInstance();
      this.totemsLeft--;
      this.fake.setHealth(1.0F);
      this.absorption = 8.0F;
      this.regenTicks = 900;

      try {
         mc.particleEngine.createTrackingEmitter(this.fake, ParticleTypes.TOTEM_OF_UNDYING, 30);
      } catch (Throwable var3) {
      }

      mc.level.playLocalSound(this.fake.getX(), this.fake.getY(), this.fake.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F, false);
      if (this.totemsLeft <= 0) {
         this.fake.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
   }

   private float afterArmor(Minecraft mc, float dmg) {
      float armor = 0.0F;
      float tough = 0.0F;

      try {
         armor = (float)this.fake.getAttributeValue(Attributes.ARMOR);
         tough = (float)this.fake.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      } catch (Throwable var12) {
      }

      if (armor <= 0.0F) {
         float[] at = this.armorFromItems();
         armor = at[0];
         tough = at[1];
      }

      float f = 2.0F + tough / 4.0F;
      float g = Mth.clamp(armor - dmg / f, armor * 0.2F, 20.0F);
      dmg *= 1.0F - g / 25.0F;
      int epf = 0;

      for (EquipmentSlot s : ARMOR) {
         epf += enchantLevel(mc, Enchantments.PROTECTION, this.fake.getItemBySlot(s));
      }

      dmg *= 1.0F - Mth.clamp(epf, 0, 20) / 25.0F;
      return Math.max(0.0F, dmg);
   }

   private float[] armorFromItems() {
      float armor = 0.0F;
      float tough = 0.0F;

      for (int i = 0; i < 4; i++) {
         ItemStack st = this.fake.getItemBySlot(ARMOR[i]);
         if (!st.isEmpty()) {
            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(st.getItem())).toLowerCase(Locale.ROOT);
            float t = 0.0F;
            int[] pts;
            if (id.contains("netherite")) {
               pts = new int[]{3, 8, 6, 3};
               t = 3.0F;
            } else if (id.contains("diamond")) {
               pts = new int[]{3, 8, 6, 3};
               t = 2.0F;
            } else if (id.contains("iron")) {
               pts = new int[]{2, 6, 5, 2};
            } else if (id.contains("chainmail")) {
               pts = new int[]{2, 5, 4, 1};
            } else if (id.contains("golden")) {
               pts = new int[]{2, 5, 3, 1};
            } else if (id.contains("copper")) {
               pts = new int[]{2, 4, 3, 1};
            } else if (id.contains("turtle")) {
               pts = new int[]{2, 0, 0, 0};
            } else {
               if (!id.contains("leather")) {
                  continue;
               }

               pts = new int[]{1, 3, 2, 1};
            }

            armor += pts[i];
            tough += t;
         }
      }

      return new float[]{armor, tough};
   }

   private float knockbackResistance() {
      int pieces = 0;

      for (EquipmentSlot s : ARMOR) {
         ItemStack st = this.fake.getItemBySlot(s);
         if (!st.isEmpty() && String.valueOf(BuiltInRegistries.ITEM.getKey(st.getItem())).contains("netherite")) {
            pieces++;
         }
      }

      return Math.min(1.0F, pieces * 0.1F);
   }

   private static int enchantLevel(Minecraft mc, ResourceKey<Enchantment> key, ItemStack st) {
      if (st != null && !st.isEmpty() && mc.level != null) {
         try {
            Registry<Enchantment> reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> h = (Holder<Enchantment>)reg.get(key).orElse(null);
            return h == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(h, st);
         } catch (Throwable t) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private static final class Body extends RemotePlayer {
      Body(ClientLevel level, GameProfile profile) {
         super(level, profile);
      }

      public boolean isPushable() {
         return false;
      }

      public void push(Entity entity) {
      }
   }
}
