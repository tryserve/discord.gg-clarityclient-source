package need.clarity.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerPacketEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.event.EventPacketReceive;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Backtrack extends ModuleBase {
   private final Queue<Backtrack.QueuedPacket> delayedPackets = new ConcurrentLinkedDeque<>();
   private final Random rng = new Random();
   private Entity target;
   private Vec3 trackedTargetPosition;
   private static volatile LivingEntity currentTarget = null;
   private VecDeltaCodec trackedTargetPositionCodec;
   private float activeDelayMs;

   public static LivingEntity getCurrentTarget() {
      return currentTarget;
   }

   public Backtrack() {
      super("Backtrack", "Combat");
      PerAttackEvents.register(this::onAttack);
      PerPacketEvents.register(this::onPacketReceive);
      WorldRenderEvent.register(this::onWorldRender);
   }

   @Override
   public void onEnable() {
      this.delayedPackets.clear();
      this.clearTrackedTargetPosition();
   }

   @Override
   public void onDisable() {
      this.flushPackets(false);
      this.clearTrackedTargetPosition();
   }

   private void onAttack(EventStartAttack event) {
      if (this.isEnabled()) {
         this.target = event.getTarget();
         if (Friends.isProtected(this.target)) {
            this.target = null;
         }

         currentTarget = this.target instanceof LivingEntity le ? le : null;
         this.clearTrackedTargetPosition();
      }
   }

   private void onPacketReceive(EventPacketReceive event) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (!mc.isSingleplayer()) {
               Packet<?> packet = event.getPacket();
               if (packet != null) {
                  if (!this.hasValidTarget(mc)) {
                     this.flushPackets(false);
                     this.clearTrackedTargetPosition();
                  } else if (!(packet instanceof ServerboundChatPacket) && !(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ServerboundChatCommandPacket)) {
                     if (!(packet instanceof ClientboundPlayerPositionPacket) && !(packet instanceof ClientboundDisconnectPacket)) {
                        if (packet instanceof ClientboundSetHealthPacket healthPkt && healthPkt.getHealth() <= 0.0F) {
                           this.flushPackets(false);
                           this.clearTrackedTargetPosition();
                        } else {
                           if (this.activeDelayMs <= 0.0F) {
                              float lo = this.getSetting("Delay_lo", 150.0F);
                              float hi = this.getSetting("Delay_hi", 250.0F);
                              this.activeDelayMs = hi > lo ? lo + this.rng.nextFloat() * (hi - lo) : lo;
                           }

                           float delay = this.activeDelayMs;
                           if (delay <= 0.0F) {
                              this.flushPackets(false);
                           } else {
                              this.flushPackets(true);
                              if (this.isServerPositionCloserThanClient(packet, mc)) {
                                 this.flushPackets(false);
                                 this.clearTrackedTargetPosition();
                              } else {
                                 this.trackTargetPosition(packet, mc);
                                 this.delayedPackets.add(new Backtrack.QueuedPacket(packet, System.currentTimeMillis()));
                                 event.cancel();
                                 Backtrack.QueuedPacket oldest = this.delayedPackets.peek();
                                 if (oldest != null && (float)(System.currentTimeMillis() - oldest.timestamp) > delay) {
                                    this.flushPackets(false);
                                 }
                              }
                           }
                        }
                     } else {
                        this.flushPackets(false);
                        this.clearTrackedTargetPosition();
                     }
                  }
               }
            }
         }
      }
   }

   private void flushPackets(boolean onlyExpired) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
         this.delayedPackets.clear();
         this.clearTrackedTargetPosition();
      } else {
         long now = System.currentTimeMillis();

         while (true) {
            Backtrack.QueuedPacket queued = this.delayedPackets.peek();
            if (queued == null || onlyExpired && (float)(now - queued.timestamp) < this.activeDelayMs) {
               if (!onlyExpired) {
                  this.delayedPackets.clear();
                  this.clearTrackedTargetPosition();
                  return;
               }

               if (this.delayedPackets.isEmpty()) {
                  this.clearTrackedTargetPosition();
               }

               return;
            }

            this.delayedPackets.poll();

            try {
               Packet pkt = queued.packet;
               pkt.handle(connection);
            } catch (Throwable var8) {
            }
         }
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         if (this.getSetting("Render", true)) {
            if (!this.hasValidTarget(Minecraft.getInstance())) {
               this.clearTrackedTargetPosition();
            } else {
               AABB box = this.getTrackedTargetBox();
               if (box != null) {
                  boolean doFill = this.getSetting("Fill", true);
                  boolean doOutline = this.getSetting("Outline", true);
                  if (doFill || doOutline) {
                     int fillArgb = doFill ? parseColor(this.getSetting("Fill Color", "#3a7fcc"), 80, false) : 0;
                     int outlineArgb = doOutline ? parseColor(this.getSetting("Outline Color", "#ffffff"), 255, true) : 0;
                     ClarityRenderUtils.drawBoxes(poseStack, List.of(box), fillArgb, outlineArgb, doFill, doOutline);
                  }
               }
            }
         }
      }
   }

   private boolean isServerPositionCloserThanClient(Packet<?> packet, Minecraft mc) {
      if (mc.player != null && mc.level != null && this.target != null) {
         Vec3 serverPos = null;
         if (packet instanceof ClientboundMoveEntityPacket movePacket) {
            if (!this.isTargetMovePacket(movePacket, mc)) {
               return false;
            }

            if (!movePacket.hasPosition()) {
               return false;
            }

            this.ensureTrackedTargetPosition();
            if (this.trackedTargetPositionCodec == null) {
               return false;
            }

            serverPos = this.trackedTargetPositionCodec.decode(movePacket.getXa(), movePacket.getYa(), movePacket.getZa());
         } else if (packet instanceof ClientboundEntityPositionSyncPacket syncPacket) {
            if (syncPacket.id() != this.target.getId()) {
               return false;
            }

            serverPos = syncPacket.values().position();
         } else if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            if (teleportPacket.id() != this.target.getId()) {
               return false;
            }

            Vec3 base = this.trackedTargetPosition != null ? this.trackedTargetPosition : this.target.position();
            Vec3 change = teleportPacket.change().position();
            double x = teleportPacket.relatives().contains(Relative.X) ? base.x + change.x : change.x;
            double y = teleportPacket.relatives().contains(Relative.Y) ? base.y + change.y : change.y;
            double z = teleportPacket.relatives().contains(Relative.Z) ? base.z + change.z : change.z;
            serverPos = new Vec3(x, y, z);
         }

         if (serverPos == null) {
            return false;
         }

         Vec3 playerPos = mc.player.position();
         double clientDistSq = playerPos.distanceToSqr(this.target.position());
         double serverDistSq = playerPos.distanceToSqr(serverPos);
         return serverDistSq < clientDistSq;
      } else {
         return false;
      }
   }

   private void trackTargetPosition(Packet<?> packet, Minecraft mc) {
      if (this.hasValidTarget(mc)) {
         if (mc.level != null) {
            if (packet instanceof ClientboundMoveEntityPacket movePacket) {
               if (this.isTargetMovePacket(movePacket, mc)) {
                  this.trackMovePacket(movePacket);
               }
            } else if (packet instanceof ClientboundEntityPositionSyncPacket syncPacket) {
               if (syncPacket.id() == this.target.getId()) {
                  this.setTrackedTargetPosition(syncPacket.values().position());
               }
            } else {
               if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
                  if (teleportPacket.id() != this.target.getId()) {
                     return;
                  }

                  this.trackTeleportPacket(teleportPacket);
               }
            }
         }
      }
   }

   private void trackMovePacket(ClientboundMoveEntityPacket packet) {
      this.ensureTrackedTargetPosition();
      if (this.trackedTargetPositionCodec != null) {
         if (packet.hasPosition()) {
            Vec3 actualPosition = this.trackedTargetPositionCodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
            this.trackedTargetPosition = actualPosition;
            this.trackedTargetPositionCodec.setBase(actualPosition);
         }
      }
   }

   private void trackTeleportPacket(ClientboundTeleportEntityPacket packet) {
      this.ensureTrackedTargetPosition();
      if (this.trackedTargetPosition != null) {
         Vec3 change = packet.change().position();
         double x = packet.relatives().contains(Relative.X) ? this.trackedTargetPosition.x + change.x : change.x;
         double y = packet.relatives().contains(Relative.Y) ? this.trackedTargetPosition.y + change.y : change.y;
         double z = packet.relatives().contains(Relative.Z) ? this.trackedTargetPosition.z + change.z : change.z;
         this.setTrackedTargetPosition(new Vec3(x, y, z));
      }
   }

   private void ensureTrackedTargetPosition() {
      if (this.trackedTargetPosition == null || this.trackedTargetPositionCodec == null) {
         if (this.target != null) {
            this.setTrackedTargetPosition(this.target.getPositionCodec().getBase());
         }
      }
   }

   private void setTrackedTargetPosition(Vec3 position) {
      this.trackedTargetPosition = position;
      this.trackedTargetPositionCodec = new VecDeltaCodec();
      this.trackedTargetPositionCodec.setBase(position);
   }

   private AABB getTrackedTargetBox() {
      if (this.trackedTargetPosition != null && this.target != null) {
         Vec3 offset = this.trackedTargetPosition.subtract(this.target.position());
         return this.target.getBoundingBox().move(offset);
      } else {
         return null;
      }
   }

   private boolean isTargetMovePacket(ClientboundMoveEntityPacket packet, Minecraft mc) {
      if (mc.level == null) {
         return false;
      }

      Entity moved = packet.getEntity(mc.level);
      return moved != null && moved.getId() == this.target.getId();
   }

   private void clearTrackedTargetPosition() {
      this.trackedTargetPosition = null;
      this.trackedTargetPositionCodec = null;
      this.activeDelayMs = 0.0F;
   }

   private boolean hasValidTarget(Minecraft mc) {
      if (this.target == null || mc.player == null) {
         return false;
      }

      if (!this.target.isAlive()) {
         return false;
      }

      int maxRange = this.getSetting("Range", 6);
      return mc.player.distanceToSqr(this.target) <= maxRange * maxRange;
   }

   private static int parseColor(String hex, int defaultAlpha, boolean secondary) {
      int rgb = ThemeColors.resolveRgb(hex, 3833804, secondary);
      return (defaultAlpha & 0xFF) << 24 | rgb & 16777215;
   }

   private static final class QueuedPacket {
      final Packet<?> packet;
      final long timestamp;

      QueuedPacket(Packet<?> packet, long timestamp) {
         this.packet = packet;
         this.timestamp = timestamp;
      }
   }
}
