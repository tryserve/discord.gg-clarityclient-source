package need.clarity.modules.combat;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerPacketEvents;
import need.clarity.PerPacketSendEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.event.EventPacketReceive;
import need.clarity.event.EventPacketSend;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FakeLag extends ModuleBase {
   private static final int GHOST_ID = -1337900;
   private static final UUID GHOST_UUID = UUID.fromString("fa4e1a90-0000-4000-8000-000000000001");
   private static FakeLag INSTANCE;
   private final Random rng = new Random();
   private final Deque<Packet<?>> queue = new ArrayDeque<>();
   private FakeLag.Phase phase = FakeLag.Phase.HOLDING;
   private long phaseStart = 0L;
   private float holdDur = 0.0F;
   private float lastHealth = -1.0F;
   private volatile boolean releasing = false;
   private Vec3 serverPos = null;
   private float serverYaw = 0.0F;
   private float serverPitch = 0.0F;
   private RemotePlayer ghost;
   private ClientLevel ghostLevel;

   public FakeLag() {
      super("Fake Lag", "Combat");
      INSTANCE = this;
      PerPacketSendEvents.register(this::onPacketSend);
      PerPacketEvents.register(this::onPacketReceive);
      PerAttackEvents.register(this::onAttack);
      WorldRenderEvent.register(this::onWorldRender);
   }

   public static boolean isGhost(Entity e) {
      FakeLag f = INSTANCE;
      return f != null && f.ghost != null && e == f.ghost;
   }

   public static int ghostColor() {
      FakeLag f = INSTANCE;
      if (f == null) {
         return 3833804;
      }

      int rgb = ThemeColors.resolveRgb(f.getSetting("Render Color", "#3a7fcc"), 3833804, false);
      return rgb & 16777215;
   }

   @Override
   public void onEnable() {
      this.queue.clear();
      this.lastHealth = -1.0F;
      this.startHolding();
   }

   @Override
   public void onDisable() {
      this.flushAll();
      this.serverPos = null;
      this.despawnGhost();
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && !mc.isSingleplayer()) {
            float hp = mc.player.getHealth();
            if (this.lastHealth < 0.0F) {
               this.lastHealth = hp;
            }

            if (this.getSetting("Flush On Damage", true) && hp < this.lastHealth && !this.queue.isEmpty()) {
               this.flushAll();
               this.startHolding();
            }

            this.lastHealth = hp;
            if (this.getSetting("Flush On Eat", false) && mc.player.isUsingItem() && !this.queue.isEmpty()) {
               this.flushAll();
               this.startHolding();
            }

            long now = System.currentTimeMillis();
            switch (this.phase) {
               case HOLDING:
                  if ((float)(now - this.phaseStart) >= this.holdDur) {
                     this.phase = FakeLag.Phase.RELEASING;
                     this.releaseOneTick(mc);
                  } else {
                     this.updateGhost(mc);
                  }
                  break;
               case RELEASING:
                  this.releaseOneTick(mc);
                  break;
               case GAP:
                  float gapDur = this.getSetting("Pulse Mode", false) ? this.randomBetween("Pulse Duration") : 0.0F;
                  if ((float)(now - this.phaseStart) >= gapDur) {
                     this.startHolding();
                  }
            }
         }
      }
   }

   private void onPacketSend(EventPacketSend event) {
      if (this.isEnabled() && !this.releasing) {
         if (this.phase != FakeLag.Phase.GAP) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.isSingleplayer()) {
               Packet<?> packet = event.getPacket();
               if (packet != null) {
                  if (!(packet instanceof ServerboundKeepAlivePacket)) {
                     this.queue.addLast(packet);
                     event.cancel();
                  }
               }
            }
         }
      }
   }

   private void onPacketReceive(EventPacketReceive event) {
      if (this.isEnabled() && !this.queue.isEmpty()) {
         Packet<?> packet = event.getPacket();
         if (this.getSetting("Flush On Knockback", true)) {
            if (packet instanceof ClientboundSetEntityMotionPacket velPkt) {
               Minecraft mc = Minecraft.getInstance();
               if (mc.player != null && velPkt.id() == mc.player.getId()) {
                  this.flushAll();
                  this.startHolding();
                  return;
               }
            }

            if (packet instanceof ClientboundExplodePacket) {
               this.flushAll();
               this.startHolding();
               return;
            }
         }

         if (packet instanceof ClientboundPlayerPositionPacket) {
            this.flushAll();
            this.startHolding();
         }
      }
   }

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled() && !this.queue.isEmpty()) {
         if (this.getSetting("Flush On Hit", true)) {
            this.flushAll();
            this.startHolding();
         }
      }
   }

   private void startHolding() {
      this.phase = FakeLag.Phase.HOLDING;
      this.phaseStart = System.currentTimeMillis();
      this.holdDur = this.randomBetween("Delay");
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.serverPos = mc.player.position();
         this.serverYaw = mc.player.getYRot();
         this.serverPitch = mc.player.getXRot();
      }

      if (this.getSetting("Render", false) && "Shader".equals(this.getSetting("Render Mode", "Box"))) {
         this.spawnGhost(mc);
      }
   }

   private void releaseOneTick(Minecraft mc) {
      ClientPacketListener conn = mc.getConnection();
      if (conn == null) {
         this.queue.clear();
         this.enterGap();
      } else if (this.queue.isEmpty()) {
         this.enterGap();
      } else {
         this.releasing = true;

         try {
            while (!this.queue.isEmpty()) {
               Packet<?> pkt = this.queue.pollFirst();

               try {
                  conn.send(pkt);
               } catch (Throwable var8) {
               }

               if (this.isMovementPacket(pkt)) {
                  break;
               }
            }
         } finally {
            this.releasing = false;
         }

         if (this.queue.isEmpty()) {
            this.enterGap();
         }
      }
   }

   private void flushAll() {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener conn = mc.getConnection();
      if (conn == null) {
         this.queue.clear();
         this.despawnGhost();
      } else {
         this.releasing = true;

         Packet<?> pkt;
         try {
            while ((pkt = this.queue.pollFirst()) != null) {
               try {
                  conn.send(pkt);
               } catch (Throwable var8) {
               }
            }
         } finally {
            this.releasing = false;
         }

         this.despawnGhost();
         this.serverPos = null;
      }
   }

   private void enterGap() {
      this.despawnGhost();
      this.serverPos = null;
      this.phase = FakeLag.Phase.GAP;
      this.phaseStart = System.currentTimeMillis();
   }

   private void spawnGhost(Minecraft mc) {
      this.despawnGhost();
      if (mc.player != null && mc.level != null && this.serverPos != null) {
         LocalPlayer me = mc.player;
         RemotePlayer p = new RemotePlayer(mc.level, new GameProfile(GHOST_UUID, ""));
         p.setId(-1337900);
         p.snapTo(this.serverPos.x, this.serverPos.y, this.serverPos.z, this.serverYaw, this.serverPitch);
         p.yHeadRot = p.yHeadRotO = this.serverYaw;
         p.yBodyRot = p.yBodyRotO = this.serverYaw;
         p.setOldPosAndRot();

         for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
               p.setItemSlot(slot, me.getItemBySlot(slot).copy());
            } catch (Throwable var9) {
            }
         }

         p.setInvisible(true);
         mc.level.addEntity(p);
         this.ghost = p;
         this.ghostLevel = mc.level;
      }
   }

   private void despawnGhost() {
      Minecraft mc = Minecraft.getInstance();
      if (this.ghost != null && mc.level != null && mc.level == this.ghostLevel) {
         try {
            mc.level.removeEntity(-1337900, RemovalReason.DISCARDED);
         } catch (Throwable var3) {
         }
      }

      this.ghost = null;
      this.ghostLevel = null;
   }

   private void updateGhost(Minecraft mc) {
      if (this.ghost != null && !this.ghost.isRemoved()) {
         if (mc.level != this.ghostLevel) {
            this.despawnGhost();
         } else {
            if (mc.player != null) {
               for (EquipmentSlot slot : EquipmentSlot.values()) {
                  try {
                     this.ghost.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
                  } catch (Throwable var7) {
                  }
               }
            }
         }
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         if (this.phase == FakeLag.Phase.HOLDING || this.phase == FakeLag.Phase.RELEASING) {
            if (this.getSetting("Render", false)) {
               if ("Box".equals(this.getSetting("Render Mode", "Box"))) {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.player != null) {
                     Vec3 pos = this.serverPos;
                     if (pos != null) {
                        float w = mc.player.getBbWidth() / 2.0F;
                        float h = mc.player.getBbHeight();
                        AABB box = new AABB(pos.x - w, pos.y, pos.z - w, pos.x + w, pos.y + h, pos.z + w);
                        int fillArgb = parseColor(this.getSetting("Render Color", "#3a7fcc"), 60);
                        int outlineArgb = parseColor(this.getSetting("Render Color", "#3a7fcc"), 180);
                        ClarityRenderUtils.drawBoxes(poseStack, List.of(box), fillArgb, outlineArgb, true, true);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isMovementPacket(Packet<?> packet) {
      return packet instanceof ServerboundMovePlayerPacket
         || packet instanceof Pos
         || packet instanceof Rot
         || packet instanceof PosRot
         || packet instanceof StatusOnly
         || packet instanceof ServerboundMoveVehiclePacket;
   }

   private float randomBetween(String key) {
      float lo = this.getSetting(key + "_lo", 200.0F);
      float hi = this.getSetting(key + "_hi", 400.0F);
      return hi > lo ? lo + this.rng.nextFloat() * (hi - lo) : lo;
   }

   private static int parseColor(String hex, int alpha) {
      int rgb = ThemeColors.resolveRgb(hex, 3833804, false);
      return (alpha & 0xFF) << 24 | rgb & 16777215;
   }

   private enum Phase {
      HOLDING,
      RELEASING,
      GAP;
   }
}
