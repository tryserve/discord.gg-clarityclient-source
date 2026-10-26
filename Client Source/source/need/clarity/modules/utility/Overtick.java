package need.clarity.modules.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerPacketEvents;
import need.clarity.PerPacketSendEvents;
import need.clarity.WorldRenderEvent;
import need.clarity.event.EventPacketReceive;
import need.clarity.event.EventPacketSend;
import need.clarity.keybind.KeybindHandler;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Overtick extends ModuleBase {
   private static final int KIND_NONE = 0;
   private static final int KIND_CHAT = 1;
   private static final int KIND_PLAYERCHAT = 2;
   private static final int KIND_DISCONNECT = 3;
   private static final int KIND_COSMETIC = 4;
   private static final int KIND_KEEPALIVE = 5;
   private static final int KIND_RESPAWN = 6;
   private static final int KIND_TP = 7;
   private static final int KIND_ANIMATE = 8;
   private static final int CAT_TRANSACTION = 1;
   private static final int CAT_MOVE = 2;
   private static final int CAT_LIFECYCLE = 4;
   private static final int CAT_STATE = 8;
   private static final int CAT_VELOCITY = 16;
   private static final int CAT_BLOCKS = 32;
   private static final int CAT_GAMESTATE = 64;
   private static final int CAT_INVENTORY = 128;
   private static final int CAT_ENTITYEVENT = 256;
   private static final int CAT_HEALTH = 512;
   private static final int CAT_XP = 1024;
   private static final int CAT_COOLDOWN = 2048;
   private static final int CAT_HELD = 4096;
   private static final int CAT_CAMERA = 8192;
   private static final int CAT_COMBAT = 16384;
   private static final Map<Class<?>, Integer> CLASS_INFO = new HashMap<>();
   private final List<Overtick.Entry> queue = new ArrayList<>();
   private static final int GHOST_SWING_TICKS = 6;
   private static final float GHOST_SWING_DURATION = 6.0F;
   private final Object ghostLock = new Object();
   private final Map<Integer, Overtick.GhostPos> ghostPos = new HashMap<>();
   private final Map<Integer, double[]> lastKnownPos = new HashMap<>();
   private final Map<Long, Object> serverBlocks = new HashMap<>();
   private final Set<Integer> heldLifecycle = ConcurrentHashMap.newKeySet();
   private volatile boolean elytraStopHeld = false;
   private volatile Set<EntityRenderState> ghostStates = Collections.newSetFromMap(new IdentityHashMap<>());
   private volatile boolean flushing = false;
   private volatile boolean disabling = false;
   private volatile boolean ghostsActive = false;
   private boolean flowing = false;
   private long flowStartReal = 0L;
   private long flowStartQueue = 0L;
   private int flowSpeedOverride = 0;
   private boolean autoReachFlow = false;
   private int lastTargetId = -1;
   private int reachDebugTick = 0;
   private boolean tickingActive = false;
   private int pingCount = 0;
   private boolean inUpDownDelay = false;
   private int delayPings = 0;
   private boolean jetpackKeyHeld = false;
   private int jetpackPings = 0;
   private boolean steadyReleasing = false;
   private boolean releaseKeyWasDown = false;
   private String lastMode = "";
   private long lastKeyMs = 0L;
   private ClientPacketListener lastConnection = null;
   private ResourceKey<Level> lastDimension = null;
   private volatile int enabledMask = 0;
   private volatile int neverMask = 0;
   private volatile int overrideMask = 0;
   private volatile String releaseModeStr = "Flow";
   private volatile int selfId = -1;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   public static final int MASK_LAUNCH = 17;

   private static Class<?> safeClass(String name) {
      try {
         return Class.forName(name);
      } catch (Throwable t) {
         return null;
      }
   }

   private static void reg(Class<?> cls, int kind, int cat) {
      if (cls != null) {
         CLASS_INFO.put(cls, kind & 0xFF | cat << 8);
      }
   }

   public void setOverrideMask(int mask) {
      this.overrideMask = mask;
   }

   public void clearOverrideMask() {
      this.overrideMask = 0;
   }

   public Overtick() {
      super("Overtick", "utility");
      PerPacketEvents.register(this::onPacketReceive);
      PerPacketSendEvents.register(this::onPacketSend);
      PerAttackEvents.register(this::onStartAttack);
   }

   private String mode() {
      return this.getSetting("Release Mode", "Flow");
   }

   private int flowSpeed() {
      return this.getSetting("Flow Speed", 3);
   }

   private int releaseSpeed() {
      return this.getSetting("Release Speed", 2);
   }

   private int interval() {
      return this.getSetting("Interval", 5);
   }

   private int delay() {
      return this.getSetting("Delay", 10);
   }

   private boolean disableOnFlag() {
      return this.getSetting("Disable On Flag", true);
   }

   private boolean disableOnDeath() {
      return this.getSetting("Disable On Death", true);
   }

   private boolean disableOnConnect() {
      return this.getSetting("Disable On Connect", true);
   }

   private boolean disableOnDim() {
      return this.getSetting("Disable On Dimension Change", true);
   }

   private int queueTimeout() {
      return this.getSetting("Queue Timeout", 29);
   }

   private boolean flushAllOnTimeout() {
      return this.getSetting("Flush All On Timeout", false);
   }

   private boolean safeReleaseOnTimeout() {
      return this.getSetting("Safe Release On Timeout", true);
   }

   private boolean ghostEnabled() {
      return this.getSetting("Ghost Entities", true);
   }

   private boolean ghostEsp() {
      return this.getSetting("Ghost ESP", true);
   }

   private int ghostAlpha() {
      return this.getSetting("Ghost Alpha", 60);
   }

   private float ghostOpacity() {
      return this.getSetting("Ghost Opacity", 0.6F);
   }

   private String ghostColor() {
      return this.getSetting("Ghost Color", "#64b4ff");
   }

   private boolean advanced() {
      return this.getSetting("Advanced Packet Control", false);
   }

   private int getReleaseKey() {
      String s = this.getSetting("Release Key", "NONE");
      return s != null && !s.isEmpty() && !s.equals("NONE") ? KeybindHandler.glfwKeyCode(s) : -1;
   }

   private void refreshRuntime() {
      int m = 0;
      if (this.advanced()) {
         if (this.getSetting("Queue Transactions", true)) {
            m |= 1;
         }

         if (this.getSetting("Queue Entity Movement", true)) {
            m |= 2;
         }

         if (this.getSetting("Queue Entity Lifecycle", true)) {
            m |= 4;
         }

         if (this.getSetting("Queue Entity State", true)) {
            m |= 8;
         }

         if (this.getSetting("Queue Velocity", true)) {
            m |= 16;
         }

         if (this.getSetting("Queue Blocks", true)) {
            m |= 32;
         }

         if (this.getSetting("Queue Game State", true)) {
            m |= 64;
         }

         if (this.getSetting("Queue Inventory", true)) {
            m |= 128;
         }

         if (this.getSetting("Queue Entity Events", true)) {
            m |= 256;
         }

         if (this.getSetting("Queue Health", true)) {
            m |= 512;
         }

         if (this.getSetting("Queue Experience", true)) {
            m |= 1024;
         }

         if (this.getSetting("Queue Cooldowns", true)) {
            m |= 2048;
         }

         if (this.getSetting("Queue Held Slot", true)) {
            m |= 4096;
         }

         if (this.getSetting("Queue Camera", false)) {
            m |= 8192;
         }

         if (this.getSetting("Queue Combat Events", false)) {
            m |= 16384;
         }
      } else {
         m = 255;
      }

      int over = this.overrideMask;
      this.enabledMask = over != 0 ? over : m;
      int n = 0;
      if (this.advanced()) {
         if (this.getSetting("Never Queue Inventory", false)) {
            n |= 128;
         }

         if (this.getSetting("Never Queue Velocity", false)) {
            n |= 16;
         }

         if (this.getSetting("Never Queue Blocks", false)) {
            n |= 32;
         }
      }

      this.neverMask = n;
      this.releaseModeStr = this.mode();

      try {
         this.selfId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getId() : -1;
      } catch (Exception e) {
         this.selfId = -1;
      }
   }

   @Override
   public void onEnable() {
      synchronized (this.queue) {
         this.queue.clear();
      }

      this.resetGhostTracking();
      this.flushing = this.disabling = false;
      this.flowing = false;
      this.flowStartReal = 0L;
      this.flowStartQueue = 0L;
      this.flowSpeedOverride = 0;
      this.autoReachFlow = false;
      this.lastTargetId = -1;
      this.tickingActive = this.inUpDownDelay = this.steadyReleasing = false;
      this.pingCount = this.delayPings = this.jetpackPings = 0;
      this.jetpackKeyHeld = false;
      this.releaseKeyWasDown = false;
      this.lastMode = "";
      this.lastKeyMs = 0L;
      Minecraft mc = Minecraft.getInstance();

      try {
         this.lastConnection = mc != null ? mc.getConnection() : null;
      } catch (Exception ignored) {
         this.lastConnection = null;
      }

      try {
         this.lastDimension = mc != null && mc.level != null ? mc.level.dimension() : null;
      } catch (Exception ignored) {
         this.lastDimension = null;
      }

      this.refreshRuntime();
      WorldRenderEvent.register(this.renderListener);
   }

   @Override
   public void onDisable() {
      if (!this.disabling) {
         this.disabling = true;
         WorldRenderEvent.unregister(this.renderListener);

         try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               mc.player.noPhysics = false;
            }
         } catch (Exception var2) {
         }

         this.flushQueue();
         this.resetGhostTracking();
         this.flushing = false;
         this.flowing = false;
         this.steadyReleasing = false;
         this.flowSpeedOverride = 0;
         this.autoReachFlow = false;
         this.disabling = false;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.player.connection != null) {
         try {
            this.refreshRuntime();
            if (this.disableOnDeath()) {
               try {
                  if (mc.player.isDeadOrDying()) {
                     this.setEnabled(false);
                     return;
                  }
               } catch (Exception var26) {
               }
            }

            try {
               ClientPacketListener conn = mc.getConnection();
               if (conn != this.lastConnection) {
                  if (this.disableOnConnect() && this.lastConnection != null) {
                     this.setEnabled(false);
                     return;
                  }

                  this.lastConnection = conn;
               }
            } catch (Exception var25) {
            }

            try {
               ResourceKey<Level> dim = mc.level.dimension();
               if (!Objects.equals(dim, this.lastDimension)) {
                  if (this.disableOnDim() && this.lastDimension != null && dim != null) {
                     this.setEnabled(false);
                     return;
                  }

                  this.lastDimension = dim;
               }
            } catch (Exception var24) {
            }

            this.handleReleaseKey(mc);
            synchronized (this.queue) {
               ;
            }

            boolean blinkActive;
            synchronized (this.queue) {
               blinkActive = !this.queue.isEmpty();
            }

            this.ghostsActive = blinkActive && this.ghostEnabled();
            if (this.ghostsActive) {
               this.tickGhosts();
            }

            if ("Jetpack".equals(this.releaseModeStr) && this.getReleaseKey() != -1) {
               this.jetpackKeyHeld = this.releaseKeyWasDown;
            } else {
               this.jetpackKeyHeld = false;
            }

            if (this.steadyReleasing && "Steady".equals(this.releaseModeStr)) {
               int rate = Math.max(1, this.releaseSpeed());
               List<Overtick.Entry> drained = new ArrayList<>();
               synchronized (this.queue) {
                  for (int i = 0; i < rate && !this.queue.isEmpty(); i++) {
                     drained.add(this.queue.remove(0));
                  }
               }

               if (!drained.isEmpty()) {
                  this.flushing = true;

                  for (Overtick.Entry e : drained) {
                     this.replayPacket(e.pkt);
                  }

                  this.flushing = false;
                  synchronized (this.queue) {
                     if (this.queue.isEmpty()) {
                        this.steadyReleasing = false;
                        this.clearGhostTracking();
                     }
                  }
               }
            }

            if (this.flowing) {
               boolean empty;
               synchronized (this.queue) {
                  empty = this.queue.isEmpty();
               }

               if (empty) {
                  this.flowing = false;
                  this.flowSpeedOverride = 0;
                  this.clearGhostTracking();
               } else {
                  int speed = this.flowSpeedOverride > 0 ? this.flowSpeedOverride : Math.max(1, this.flowSpeed());
                  long now = System.currentTimeMillis();
                  long elapsed = (now - this.flowStartReal) * speed;
                  List<Overtick.Entry> out = new ArrayList<>();
                  synchronized (this.queue) {
                     while (!this.queue.isEmpty()) {
                        Overtick.Entry h = this.queue.get(0);
                        if (h.ts - this.flowStartQueue > elapsed) {
                           break;
                        }

                        out.add(this.queue.remove(0));
                     }
                  }

                  if (!out.isEmpty()) {
                     this.flushing = true;

                     for (Overtick.Entry e : out) {
                        this.replayPacket(e.pkt);
                     }

                     this.flushing = false;
                     synchronized (this.queue) {
                        if (this.queue.isEmpty()) {
                           this.flowing = false;
                           this.flowSpeedOverride = 0;
                           this.clearGhostTracking();
                        }
                     }
                  }
               }
            }

            int timeoutSecs = this.queueTimeout();
            if (timeoutSecs > 0 && blinkActive) {
               long now = System.currentTimeMillis();
               long thresh = timeoutSecs * 1000L;
               Overtick.Entry head;
               synchronized (this.queue) {
                  head = this.queue.isEmpty() ? null : this.queue.get(0);
               }

               if (head != null && now - head.ts >= thresh) {
                  if (this.safeReleaseOnTimeout()) {
                     if (!this.flowing) {
                        this.startFlow(1);
                     }
                  } else if (this.flushAllOnTimeout()) {
                     this.flushAll();
                  } else {
                     List<Overtick.Entry> out = new ArrayList<>();
                     synchronized (this.queue) {
                        while (!this.queue.isEmpty() && now - this.queue.get(0).ts >= thresh) {
                           out.add(this.queue.remove(0));
                        }
                     }

                     if (!out.isEmpty()) {
                        this.flushing = true;

                        for (Overtick.Entry e : out) {
                           this.replayPacket(e.pkt);
                        }

                        this.flushing = false;
                        synchronized (this.queue) {
                           if (this.queue.isEmpty()) {
                              this.clearGhostTracking();
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception e) {
            this.flowing = false;
            this.flowSpeedOverride = 0;
         }
      } else {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }
      }
   }

   private void handleReleaseKey(Minecraft mc) {
      int key = this.getReleaseKey();
      if (key > 0 && key != -1) {
         boolean down;
         try {
            down = GLFW.glfwGetKey(mc.getWindow().handle(), key) == 1;
         } catch (Exception e) {
            down = false;
         }

         boolean pressed = down && !this.releaseKeyWasDown;
         boolean released = !down && this.releaseKeyWasDown;
         this.releaseKeyWasDown = down;
         String m = this.releaseModeStr;
         if (!m.equals(this.lastMode)) {
            this.resetToggle();
            this.lastMode = m;
         }

         if (pressed) {
            switch (m) {
               case "Flow":
                  this.startFlow();
                  break;
               case "Steady":
                  this.steadyReleasing = true;
                  break;
               case "Jetpack":
                  this.flushAll();
                  break;
               case "Tick":
                  this.tickingActive = !this.tickingActive;
                  this.pingCount = 0;
                  break;
               case "One by One":
                  if (this.debounce()) {
                     if (!this.hasExplosion()) {
                        this.flushAll();
                     } else {
                        this.releaseOne();
                        if (!this.hasExplosion()) {
                           this.flushAll();
                        }
                     }
                  }
                  break;
               case "Up/Down":
                  if (this.debounce() && !this.inUpDownDelay) {
                     this.releaseUpPhase();
                     if (this.hasExplosion()) {
                        this.inUpDownDelay = true;
                        this.delayPings = 0;
                     } else {
                        this.flushAll();
                     }
                  }
                  break;
               default:
                  if (this.debounce()) {
                     this.flushAll();
                  }
            }
         }

         if (released) {
            if ("Flow".equals(m)) {
               this.flowing = false;
               this.flowSpeedOverride = 0;
               this.autoReachFlow = false;
            }

            if ("Steady".equals(m)) {
               this.steadyReleasing = false;
            }
         }
      } else {
         this.releaseKeyWasDown = false;
      }
   }

   private boolean debounce() {
      long now = System.currentTimeMillis();
      if (now - this.lastKeyMs < 250L) {
         return false;
      }

      this.lastKeyMs = now;
      return true;
   }

   private void startFlow() {
      this.startFlow(0);
   }

   private void startFlow(int speedOverride) {
      Overtick.Entry head;
      synchronized (this.queue) {
         head = this.queue.isEmpty() ? null : this.queue.get(0);
      }

      if (head == null) {
         this.flowing = false;
         this.flowSpeedOverride = 0;
      } else {
         this.flowing = true;
         this.flowSpeedOverride = Math.max(0, speedOverride);
         this.flowStartReal = System.currentTimeMillis();
         this.flowStartQueue = head.ts;
      }
   }

   private void resetToggle() {
      this.tickingActive = false;
      this.pingCount = 0;
      this.delayPings = 0;
      this.jetpackPings = 0;
      this.inUpDownDelay = false;
   }

   private Vec3 targetGhostPos(int id) {
      synchronized (this.ghostLock) {
         Overtick.GhostPos gp = this.ghostPos.get(id);
         if (gp != null) {
            return new Vec3(gp.x, gp.y, gp.z);
         }

         double[] last = this.lastKnownPos.get(id);
         return last != null ? new Vec3(last[0], last[1], last[2]) : null;
      }
   }

   private void onPacketReceive(EventPacketReceive event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (this.isEnabled() && !this.flushing && !this.disabling) {
            try {
               Packet<?> pkt = event.getPacket();
               if (pkt instanceof ClientboundBundlePacket bundle) {
                  boolean anyGated = false;
                  boolean flagged = false;

                  for (Packet<?> sub : bundle.subPackets()) {
                     Integer sinfo = CLASS_INFO.get(sub.getClass());
                     if (sinfo != null) {
                        if ((sinfo & 0xFF) == 7) {
                           flagged = true;
                           break;
                        }

                        int sk = sinfo & 0xFF;
                        int scat = sinfo >>> 8;
                        if (sk == 0 && scat != 0 && (scat & this.neverMask) == 0 && (scat & this.enabledMask) != 0) {
                           anyGated = true;
                           break;
                        }
                     }
                  }

                  if (flagged) {
                     this.handleFlag();
                     return;
                  }

                  if (!anyGated) {
                     return;
                  }

                  event.cancel();

                  for (Packet<?> sub : bundle.subPackets()) {
                     Integer sinfo = CLASS_INFO.get(sub.getClass());
                     if (sub instanceof ClientboundSetEntityDataPacket sd && sd.id() == this.selfId) {
                        this.noteSelfEntityData(sd);
                     }

                     if (sinfo == null) {
                        this.replayPacket(sub);
                     } else {
                        int sk = sinfo & 0xFF;
                        int scat = sinfo >>> 8;
                        boolean gated = sk == 0 && scat != 0 && (scat & this.neverMask) == 0 && (scat & this.enabledMask) != 0;
                        if (gated) {
                           if (sub instanceof ClientboundAddEntityPacket add) {
                              this.seedGhost(add);
                           } else if (sub instanceof ClientboundRemoveEntitiesPacket rem) {
                              this.dropGhosts(rem);
                           }

                           this.enqueue(sub);
                        } else {
                           this.replayPacket(sub);
                        }
                     }
                  }

                  return;
               }

               Integer info = CLASS_INFO.get(pkt.getClass());
               if (info == null) {
                  return;
               }

               int kind = info & 0xFF;
               if (kind != 0) {
                  switch (kind) {
                     case 1:
                        return;
                     case 2:
                     case 3:
                        return;
                     case 4:
                        return;
                     case 5:
                        return;
                     case 6:
                        this.flushAll();
                        return;
                     case 7:
                        this.handleFlag();
                        return;
                     case 8:
                        if (this.ghostEnabled() || this.blockDiff()) {
                           this.trackServerPosition(pkt);
                        }

                        return;
                     default:
                        return;
                  }
               }

               int cat = info >>> 8;
               if (cat == 0) {
                  return;
               }

               if ((cat & this.neverMask) != 0 || (cat & this.enabledMask) == 0) {
                  return;
               }

               if (pkt instanceof ClientboundSetEntityDataPacket data && data.id() == this.selfId) {
                  boolean hasPhysics = false;

                  try {
                     for (DataValue<?> item : data.packedItems()) {
                        int idx = item.id();
                        if (idx == 0 || idx == 6 || idx == 8) {
                           hasPhysics = true;
                        }
                     }
                  } catch (Exception ignored) {
                     hasPhysics = true;
                  }

                  this.noteSelfEntityData(data);
                  if (!hasPhysics) {
                     this.replayPacket(pkt);
                     return;
                  }
               }

               if (pkt instanceof ClientboundEntityEventPacket ev && mc.level != null) {
                  Entity ent = ev.getEntity(mc.level);
                  if (ent == mc.player && ev.getEventId() != 9) {
                     return;
                  }
               }

               if (pkt instanceof ClientboundAddEntityPacket add) {
                  this.seedGhost(add);
               } else if (pkt instanceof ClientboundRemoveEntitiesPacket rem) {
                  this.dropGhosts(rem);
               }

               event.cancel();
               this.enqueue(pkt);

               try {
                  if (this.ghostEnabled() || this.blockDiff()) {
                     this.trackServerPosition(pkt);
                  }
               } catch (Exception var15) {
               }

               if (pkt instanceof ClientboundPingPacket) {
                  this.onPingQueued();
               }
            } catch (Exception var17) {
            }
         }
      }
   }

   private void seedGhost(ClientboundAddEntityPacket add) {
      this.heldLifecycle.add(add.getId());
      if (this.ghostEnabled()) {
         synchronized (this.ghostLock) {
            this.ghostPos.put(add.getId(), new Overtick.GhostPos(add.getX(), add.getY(), add.getZ(), add.getYRot(), add.getXRot(), add.getYHeadRot()));
            this.lastKnownPos.put(add.getId(), new double[]{add.getX(), add.getY(), add.getZ()});
         }
      }
   }

   private void dropGhosts(ClientboundRemoveEntitiesPacket rem) {
      IntListIterator var2 = rem.getEntityIds().iterator();

      while (var2.hasNext()) {
         int id = (Integer)var2.next();
         this.heldLifecycle.add(id);
         synchronized (this.ghostLock) {
            this.ghostPos.remove(id);
         }
      }
   }

   private void noteSelfEntityData(ClientboundSetEntityDataPacket data) {
      Minecraft mc = Minecraft.getInstance();

      try {
         for (DataValue<?> item : data.packedItems()) {
            int idx = item.id();
            if (idx == 0 && item.value() instanceof Byte b) {
               this.elytraStopHeld = (b & 128) == 0;
            } else if (idx == 15 && mc.player != null && item.value() instanceof Number n) {
               mc.player.setAbsorptionAmount(n.floatValue());
            }
         }
      } catch (Exception var9) {
      }
   }

   private void handleFlag() {
      Minecraft mc = Minecraft.getInstance();

      try {
         this.flushAll();
      } catch (Throwable var3) {
      }

      if (this.disableOnFlag()) {
         try {
            if (mc != null && !mc.isSameThread()) {
               mc.execute(() -> this.setEnabled(false));
            } else {
               this.setEnabled(false);
            }
         } catch (Throwable var4) {
         }
      }
   }

   private void enqueue(Packet<?> pkt) {
      synchronized (this.queue) {
         this.queue.add(new Overtick.Entry(pkt));
      }
   }

   private void onPingQueued() {
      String m = this.releaseModeStr;
      if (!m.equals(this.lastMode)) {
         this.resetToggle();
         this.lastMode = m;
      }

      if ("Tick".equals(m) && this.tickingActive) {
         if (++this.pingCount >= Math.max(1, this.interval())) {
            this.pingCount = 0;
            if (!this.releaseOne()) {
               this.tickingActive = false;
               this.flushAll();
            }
         }
      } else if ("Jetpack".equals(m)) {
         if (this.jetpackKeyHeld) {
            if (++this.jetpackPings >= Math.max(1, this.interval())) {
               this.jetpackPings = 0;
               if (!this.releaseOne()) {
                  this.flushAll();
               }
            }
         } else {
            this.jetpackPings = 0;
         }
      } else if ("Up/Down".equals(m) && this.inUpDownDelay && ++this.delayPings >= Math.max(1, this.delay())) {
         this.inUpDownDelay = false;
         this.delayPings = 0;
         this.flushAll();
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            if (mc.isSameThread()) {
               this.setEnabled(false);
            } else {
               mc.execute(() -> this.setEnabled(false));
            }
         }
      }
   }

   private void onPacketSend(EventPacketSend event) {
      if (this.isEnabled()) {
         try {
            Packet<?> p = event.getPacket();
            if (p instanceof ServerboundAttackPacket atk) {
               if (atk.entityId() < 0) {
                  event.cancel();
                  return;
               }
            } else if (p instanceof ServerboundInteractPacket it && it.entityId() < 0) {
               event.cancel();
               return;
            }
         } catch (Throwable var5) {
         }
      }
   }

   private void onStartAttack(EventStartAttack event) {
      if (event != null) {
         try {
            Entity target = event.getTarget();
            if (target != null) {
               this.lastTargetId = target.getId();
            }
         } catch (Exception var3) {
         }
      }
   }

   private boolean hasExplosion() {
      synchronized (this.queue) {
         for (Overtick.Entry e : this.queue) {
            if (e.pkt instanceof ClientboundExplodePacket) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isPing(Packet<?> p) {
      return p instanceof ClientboundPingPacket;
   }

   private void replayPacket(Packet<?> packet) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.connection != null) {
         try {
            handleRaw(packet, mc);
         } catch (Exception var4) {
         }
      }
   }

   private static <T extends PacketListener> void handleRaw(Packet<?> packet, Minecraft mc) {
      ((Packet)packet).handle(mc.player.connection);
   }

   private void flushAll() {
      List<Overtick.Entry> snapshot;
      synchronized (this.queue) {
         if (this.queue.isEmpty()) {
            this.clearGhostTracking();
            return;
         }

         snapshot = new ArrayList<>(this.queue);
         this.queue.clear();
      }

      this.clearGhostTracking();
      this.inUpDownDelay = false;
      this.flowing = false;
      this.flowSpeedOverride = 0;
      this.autoReachFlow = false;
      this.steadyReleasing = false;
      this.flushing = true;

      for (Overtick.Entry e : snapshot) {
         this.replayPacket(e.pkt);
      }

      this.flushing = false;
   }

   public void releaseAll() {
      try {
         this.flushAll();
      } catch (Throwable var2) {
      }
   }

   public boolean hasQueued() {
      synchronized (this.queue) {
         return !this.queue.isEmpty();
      }
   }

   private void flushQueue() {
      List<Overtick.Entry> snapshot;
      synchronized (this.queue) {
         snapshot = new ArrayList<>(this.queue);
         this.queue.clear();
      }

      this.flushing = true;

      for (Overtick.Entry e : snapshot) {
         this.replayPacket(e.pkt);
      }

      this.flushing = false;
   }

   private boolean releaseOne() {
      List<Overtick.Entry> slice = new ArrayList<>();
      synchronized (this.queue) {
         int explodeIdx = -1;

         for (int i = 0; i < this.queue.size(); i++) {
            if (this.queue.get(i).pkt instanceof ClientboundExplodePacket) {
               explodeIdx = i;
               break;
            }
         }

         if (explodeIdx < 0) {
            return false;
         }

         int pingIdx = -1;

         for (int i = explodeIdx + 1; i < this.queue.size(); i++) {
            if (this.isPing(this.queue.get(i).pkt)) {
               pingIdx = i;
               break;
            }
         }

         if (pingIdx < 0) {
            return false;
         }

         slice.addAll(this.queue.subList(0, pingIdx + 1));
         this.queue.subList(0, pingIdx + 1).clear();
      }

      this.flushing = true;

      for (Overtick.Entry e : slice) {
         this.replayPacket(e.pkt);
      }

      this.flushing = false;
      synchronized (this.queue) {
         if (this.queue.isEmpty()) {
            this.clearGhostTracking();
         }

         return true;
      }
   }

   private void releaseUpPhase() {
      int total = 0;

      while (true) {
         List<Overtick.Entry> slice = new ArrayList<>();
         synchronized (this.queue) {
            int explodeIdx = -1;

            for (int i = 0; i < this.queue.size(); i++) {
               if (this.queue.get(i).pkt instanceof ClientboundExplodePacket) {
                  explodeIdx = i;
                  break;
               }
            }

            if (explodeIdx < 0) {
               break;
            }

            try {
               Optional<Vec3> kb = ((ClientboundExplodePacket)this.queue.get(explodeIdx).pkt).playerKnockback();
               if (kb.isEmpty() || kb.get().y <= 0.0) {
                  break;
               }
            } catch (Exception ignored) {
               break;
            }

            int pingIdx = -1;

            for (int i = explodeIdx + 1; i < this.queue.size(); i++) {
               if (this.isPing(this.queue.get(i).pkt)) {
                  pingIdx = i;
                  break;
               }
            }

            if (pingIdx >= 0) {
               slice.addAll(this.queue.subList(0, pingIdx + 1));
               this.queue.subList(0, pingIdx + 1).clear();
            } else {
               break;
            }
         }

         this.flushing = true;

         for (Overtick.Entry e : slice) {
            this.replayPacket(e.pkt);
         }

         this.flushing = false;
         total += slice.size();
      }

      if (total > 0) {
         this.clearGhostTracking();
      }
   }

   private void trackServerPosition(Packet<?> pkt) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         try {
            if (pkt instanceof ClientboundBlockUpdatePacket bu) {
               synchronized (this.ghostLock) {
                  this.serverBlocks.put(bu.getPos().asLong(), bu.getBlockState());
                  if (this.serverBlocks.size() > 20000) {
                     this.serverBlocks.clear();
                  }

                  return;
               }
            }

            if (pkt instanceof ClientboundSectionBlocksUpdatePacket sec) {
               synchronized (this.ghostLock) {
                  sec.runUpdates((pos, state) -> this.serverBlocks.put(pos.asLong(), state));
                  if (this.serverBlocks.size() > 20000) {
                     this.serverBlocks.clear();
                  }

                  return;
               }
            }

            if (pkt instanceof ClientboundRemoveEntitiesPacket rem) {
               synchronized (this.ghostLock) {
                  IntListIterator var51 = rem.getEntityIds().iterator();

                  while (var51.hasNext()) {
                     int id = (Integer)var51.next();
                     this.ghostPos.remove(id);
                  }

                  return;
               }
            }

            if (pkt instanceof ClientboundMoveEntityPacket move) {
               Entity e = move.getEntity(mc.level);
               if (e != null && e != mc.player) {
                  int id = e.getId();
                  synchronized (this.ghostLock) {
                     Overtick.GhostPos gp = this.shadowFor(id, e);
                     if (gp == null) {
                        return;
                     }

                     if (move.hasPosition()) {
                        gp.pendingX = gp.pendingX + move.getXa() / 4096.0;
                        gp.pendingY = gp.pendingY + move.getYa() / 4096.0;
                        gp.pendingZ = gp.pendingZ + move.getZa() / 4096.0;
                     }

                     if (move.hasRotation()) {
                        gp.pendingYRot = move.getYRot();
                        gp.pendingXRot = move.getXRot();
                        gp.pendingYHeadRot = move.getYRot();
                     }

                     return;
                  }
               }

               return;
            }

            if (pkt instanceof ClientboundEntityPositionSyncPacket sync) {
               int id = sync.id();
               if (id == this.selfId) {
                  return;
               }

               synchronized (this.ghostLock) {
                  Overtick.GhostPos gp = this.shadowFor(id, mc.level.getEntity(id));
                  if (gp == null) {
                     return;
                  }

                  PositionMoveRotation v = sync.values();
                  Vec3 p = v.position();
                  gp.pendingX = p.x;
                  gp.pendingY = p.y;
                  gp.pendingZ = p.z;
                  gp.pendingYRot = v.yRot();
                  gp.pendingXRot = v.xRot();
                  gp.pendingYHeadRot = v.yRot();
                  return;
               }
            }

            if (pkt instanceof ClientboundTeleportEntityPacket tp) {
               int id = tp.id();
               if (id == this.selfId) {
                  return;
               }

               synchronized (this.ghostLock) {
                  Overtick.GhostPos gp = this.shadowFor(id, mc.level.getEntity(id));
                  if (gp == null) {
                     return;
                  }

                  PositionMoveRotation cur = new PositionMoveRotation(new Vec3(gp.pendingX, gp.pendingY, gp.pendingZ), Vec3.ZERO, gp.pendingYRot, gp.pendingXRot);
                  PositionMoveRotation abs = PositionMoveRotation.calculateAbsolute(cur, tp.change(), tp.relatives());
                  Vec3 p = abs.position();
                  gp.pendingX = p.x;
                  gp.pendingY = p.y;
                  gp.pendingZ = p.z;
                  gp.pendingYRot = abs.yRot();
                  gp.pendingXRot = abs.xRot();
                  gp.pendingYHeadRot = abs.yRot();
                  return;
               }
            }

            if (pkt instanceof ClientboundRotateHeadPacket head) {
               Entity e = head.getEntity(mc.level);
               if (e != null && e != mc.player) {
                  synchronized (this.ghostLock) {
                     Overtick.GhostPos gp = this.shadowFor(e.getId(), e);
                     if (gp != null) {
                        gp.pendingYHeadRot = head.getYHeadRot();
                     }

                     return;
                  }
               }

               return;
            }

            if (pkt instanceof ClientboundAnimatePacket anim) {
               synchronized (this.ghostLock) {
                  Overtick.GhostPos gp = this.ghostPos.get(anim.getId());
                  if (gp == null) {
                     return;
                  }

                  int action = anim.getAction();
                  if (action == 0) {
                     this.startGhostSwing(gp, true);
                  } else if (action == 3) {
                     this.startGhostSwing(gp, false);
                  } else if (action == 1) {
                     gp.hurtTicks = 10;
                  }

                  return;
               }
            }

            if (pkt instanceof ClientboundHurtAnimationPacket hurt) {
               synchronized (this.ghostLock) {
                  Overtick.GhostPos gp = this.ghostPos.get(hurt.id());
                  if (gp != null) {
                     gp.hurtTicks = 10;
                  }

                  return;
               }
            }

            if (pkt instanceof ClientboundSetEntityDataPacket data) {
               if (data.id() == this.selfId) {
                  return;
               }

               synchronized (this.ghostLock) {
                  Overtick.GhostPos gp = this.ghostPos.get(data.id());
                  if (gp == null) {
                     return;
                  }

                  try {
                     for (DataValue<?> item : data.packedItems()) {
                        int idx = item.id();
                        if (idx == 0 && item.value() instanceof Byte b) {
                           byte flags = b;
                           gp.crouching = (flags & 2) != 0;
                           gp.sprinting = (flags & 8) != 0;
                           gp.swimming = (flags & 16) != 0;
                           gp.fallFlying = (flags & 128) != 0;
                        } else if (idx == 6 && item.value() instanceof Pose p) {
                           gp.pose = p;
                        }
                     }
                  } catch (Exception var24) {
                  }
               }
            }
         } catch (Exception var26) {
         }
      }
   }

   private void startGhostSwing(Overtick.GhostPos gp, boolean mainHand) {
      if (!gp.swinging || gp.swingTime >= 3 || gp.swingTime < 0) {
         gp.swingTime = -1;
         gp.swinging = true;
         gp.swingArm = mainHand ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
      }
   }

   private void tickGhosts() {
      synchronized (this.ghostLock) {
         for (Map.Entry<Integer, Overtick.GhostPos> e : this.ghostPos.entrySet()) {
            Overtick.GhostPos gp = e.getValue();
            gp.prevYRot = gp.yRot;
            gp.prevYHeadRot = gp.yHeadRot;
            gp.prevXRot = gp.xRot;
            gp.prevX = gp.x;
            gp.prevY = gp.y;
            gp.prevZ = gp.z;
            gp.prevBodyYaw = gp.bodyYaw;
            if (gp.pendingX != gp.lerpX || gp.pendingY != gp.lerpY || gp.pendingZ != gp.lerpZ) {
               gp.lerpX = gp.pendingX;
               gp.lerpY = gp.pendingY;
               gp.lerpZ = gp.pendingZ;
               gp.lerpSteps = 3;
            }

            if (gp.lerpSteps > 0) {
               gp.x = gp.x + (gp.lerpX - gp.x) / gp.lerpSteps;
               gp.y = gp.y + (gp.lerpY - gp.y) / gp.lerpSteps;
               gp.z = gp.z + (gp.lerpZ - gp.z) / gp.lerpSteps;
               gp.lerpSteps--;
            }

            if (gp.pendingYRot != gp.lerpYRot || gp.pendingXRot != gp.lerpXRot || gp.pendingYHeadRot != gp.lerpYHeadRot) {
               gp.lerpYRot = gp.pendingYRot;
               gp.lerpXRot = gp.pendingXRot;
               gp.lerpYHeadRot = gp.pendingYHeadRot;
               gp.lerpRotSteps = 3;
            }

            if (gp.lerpRotSteps > 0) {
               gp.yRot = gp.yRot + Mth.wrapDegrees(gp.lerpYRot - gp.yRot) / gp.lerpRotSteps;
               gp.xRot = gp.xRot + (gp.lerpXRot - gp.xRot) / gp.lerpRotSteps;
               gp.yHeadRot = gp.yHeadRot + Mth.wrapDegrees(gp.lerpYHeadRot - gp.yHeadRot) / gp.lerpRotSteps;
               gp.lerpRotSteps--;
            }

            float f = Mth.wrapDegrees(gp.yHeadRot - gp.bodyYaw);
            gp.bodyYaw += f * 0.3F;
            float g = Mth.wrapDegrees(gp.yHeadRot - gp.bodyYaw);
            float maxHead = 50.0F;
            if (Math.abs(g) > maxHead) {
               gp.bodyYaw = gp.bodyYaw + (g - Math.signum(g) * maxHead);
            }

            gp.bodyYaw = Mth.wrapDegrees(gp.bodyYaw);
            gp.commitNs = System.nanoTime();
            this.lastKnownPos.put(e.getKey(), new double[]{gp.x, gp.y, gp.z});
            double dx = gp.x - gp.prevX;
            double dz = gp.z - gp.prevZ;
            float distance = (float)Math.sqrt(dx * dx + dz * dz);
            gp.walkAnim.update(Math.min(distance * 4.0F, 1.0F), 0.4F, 1.0F);
            gp.oAttackAnim = gp.attackAnim;
            if (gp.swinging) {
               gp.swingTime++;
               if (gp.swingTime >= 6) {
                  gp.swingTime = 0;
                  gp.swinging = false;
               }
            } else {
               gp.swingTime = 0;
            }

            gp.attackAnim = gp.swingTime / 6.0F;
            if (gp.hurtTicks > 0) {
               gp.hurtTicks--;
            }

            gp.swimAmount = gp.swimAmount + ((gp.swimming ? 1.0F : 0.0F) - gp.swimAmount) * 0.1F;
            gp.tickCount++;
         }
      }
   }

   private Overtick.GhostPos shadowFor(int id, Entity entity) {
      Overtick.GhostPos gp = this.ghostPos.get(id);
      if (gp == null) {
         if (entity == null) {
            return null;
         }

         double[] last = this.lastKnownPos.get(id);
         if (last != null) {
            gp = new Overtick.GhostPos(last[0], last[1], last[2], entity.getYRot(), entity.getXRot(), entity.getYHeadRot());
         } else {
            gp = new Overtick.GhostPos(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), entity.getYHeadRot());
         }

         this.ghostPos.put(id, gp);
      }

      return gp;
   }

   private void clearGhostTracking() {
      this.heldLifecycle.clear();
      this.elytraStopHeld = false;
   }

   private void resetGhostTracking() {
      synchronized (this.ghostLock) {
         this.ghostPos.clear();
         this.lastKnownPos.clear();
         this.serverBlocks.clear();
      }

      this.heldLifecycle.clear();
      this.elytraStopHeld = false;
      this.ghostStates = Collections.newSetFromMap(new IdentityHashMap<>());
   }

   public boolean isElytraStopHeld() {
      return this.elytraStopHeld;
   }

   public boolean isGhostEntity(int id) {
      if (this.heldLifecycle.contains(id)) {
         return true;
      }

      synchronized (this.ghostLock) {
         return this.ghostPos.containsKey(id);
      }
   }

   public static boolean isGatedPacket(Packet<?> pkt) {
      if (pkt == null) {
         return false;
      } else {
         Integer info = CLASS_INFO.get(pkt.getClass());
         if (info == null) {
            return false;
         } else {
            int kind = info & 0xFF;
            int cat = info >>> 8;
            if (kind == 0 && cat != 0) {
               int considered = 255;
               return (cat & considered) != 0;
            } else {
               return false;
            }
         }
      }
   }

   public static boolean isGatedPacket(Packet<?> pkt, int mask) {
      if (pkt == null) {
         return false;
      }

      Integer info = CLASS_INFO.get(pkt.getClass());
      if (info == null) {
         return false;
      }

      int kind = info & 0xFF;
      int cat = info >>> 8;
      return kind == 0 && cat != 0 ? (cat & mask) != 0 : false;
   }

   public boolean isGhostState(Object state) {
      return state != null && this.ghostStates.contains(state);
   }

   public int ghostOpacityAlpha() {
      float o = this.ghostOpacity();
      if (o < 0.0F) {
         o = 0.0F;
      }

      if (o > 1.0F) {
         o = 1.0F;
      }

      return (int)(o * 255.0F + 0.5F);
   }

   private static float ghostPartial(Overtick.GhostPos gp) {
      long elapsed = System.nanoTime() - gp.commitNs;
      float f = (float)elapsed / 5.0E7F;
      if (f < 0.0F) {
         return 0.0F;
      } else {
         return f > 1.0F ? 1.0F : f;
      }
   }

   public void addGhostRenderStates(LevelRenderState levelState, float partialTick) {
      if (this.isEnabled() && this.ghostEnabled() && this.ghostsActive && levelState != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            List<Map.Entry<Integer, Overtick.GhostPos>> snap;
            synchronized (this.ghostLock) {
               if (this.ghostPos.isEmpty()) {
                  this.ghostStates = Collections.newSetFromMap(new IdentityHashMap<>());
                  return;
               }

               snap = new ArrayList<>(this.ghostPos.entrySet());
            }

            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            double camX = 0.0;
            double camY = 0.0;
            double camZ = 0.0;

            try {
               if (levelState.cameraRenderState != null && levelState.cameraRenderState.pos != null) {
                  Vec3 c = levelState.cameraRenderState.pos;
                  camX = c.x;
                  camY = c.y;
                  camZ = c.z;
               }
            } catch (Exception var29) {
            }

            Set<EntityRenderState> newSet = Collections.newSetFromMap(new IdentityHashMap<>());

            for (Map.Entry<Integer, Overtick.GhostPos> en : snap) {
               Entity real = mc.level.getEntity(en.getKey());
               if (real != null && real != mc.player) {
                  Overtick.GhostPos gp = en.getValue();

                  try {
                     EntityRenderState state = dispatcher.extractEntity(real, partialTick);
                     if (state != null) {
                        double ix = Mth.lerp(partialTick, gp.prevX, gp.x);
                        double iy = Mth.lerp(partialTick, gp.prevY, gp.y);
                        double iz = Mth.lerp(partialTick, gp.prevZ, gp.z);
                        state.x = ix;
                        state.y = iy;
                        state.z = iz;
                        state.nameTag = null;
                        state.scoreText = null;
                        state.shadowRadius = 0.0F;

                        try {
                           state.shadowPieces.clear();
                        } catch (Exception var27) {
                        }

                        state.distanceToCameraSq = (camX - ix) * (camX - ix) + (camY - iy) * (camY - iy) + (camZ - iz) * (camZ - iz);
                        if (state instanceof LivingEntityRenderState les) {
                           les.isInvisible = false;
                           les.isInvisibleToPlayer = false;
                           float bodyRot = Mth.rotLerp(partialTick, gp.prevBodyYaw, gp.bodyYaw);
                           float headRot = Mth.rotLerp(partialTick, gp.prevYHeadRot, gp.yHeadRot);
                           les.bodyRot = bodyRot;
                           les.yRot = Mth.wrapDegrees(headRot - bodyRot);
                           les.xRot = Mth.lerp(partialTick, gp.prevXRot, gp.xRot);
                           les.walkAnimationPos = gp.walkAnim.position(partialTick);
                           les.walkAnimationSpeed = gp.walkAnim.speed(partialTick);
                           les.ageInTicks = gp.tickCount + partialTick;
                           les.hasRedOverlay = gp.hurtTicks > 0;
                        }

                        if (state instanceof HumanoidRenderState hrs) {
                           hrs.isCrouching = gp.crouching;
                           hrs.isFallFlying = gp.fallFlying;
                           hrs.isVisuallySwimming = gp.swimming;
                           hrs.swimAmount = gp.swimAmount;
                           hrs.pose = gp.pose;
                        }

                        if (state instanceof ArmedEntityRenderState ars) {
                           float diff = gp.attackAnim - gp.oAttackAnim;
                           if (diff < 0.0F) {
                              diff++;
                           }

                           ars.attackTime = gp.oAttackAnim + diff * partialTick;
                           ars.attackArm = gp.swingArm;
                        }

                        newSet.add(state);
                        levelState.entityRenderStates.add(state);
                     }
                  } catch (Exception var28) {
                  }
               }
            }

            this.ghostStates = newSet;
         }
      }
   }

   private boolean blockDiff() {
      return this.getSetting("Block Diff", true);
   }

   private String blockDiffColor() {
      return this.getSetting("Block Diff Color", "#FF4D4D");
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.level != null) {
         if (this.blockDiff()) {
            try {
               List<Long> keys;
               synchronized (this.ghostLock) {
                  keys = new ArrayList<>(this.serverBlocks.keySet());
               }

               List<AABB> differ = new ArrayList<>();

               for (Long k : keys) {
                  Object so;
                  synchronized (this.ghostLock) {
                     so = this.serverBlocks.get(k);
                  }

                  if (so instanceof BlockState server) {
                     BlockPos bp = BlockPos.of(k);
                     if (mc.level.getBlockState(bp) != server) {
                        differ.add(new AABB(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 1, bp.getZ() + 1));
                     }
                  }
               }

               if (!differ.isEmpty()) {
                  int rgb2 = parseRgb(this.blockDiffColor());
                  ClarityRenderUtils.drawBoxes(poseStack, differ, 1845493760 | rgb2, 0xFF000000 | rgb2);
               }
            } catch (Exception var26) {
            }
         }

         if (this.ghostEnabled() && this.ghostEsp() && this.ghostsActive) {
            List<Map.Entry<Integer, Overtick.GhostPos>> snap;
            synchronized (this.ghostLock) {
               if (this.ghostPos.isEmpty()) {
                  return;
               }

               snap = new ArrayList<>(this.ghostPos.entrySet());
            }

            try {
               int rgb = parseRgb(this.ghostColor());
               int alpha = Math.max(0, Math.min(255, this.ghostAlpha()));
               int fill = Math.min(120, alpha) << 24 | rgb;
               int outline = Math.min(255, alpha + 140) << 24 | rgb;
               List<AABB> boxes = new ArrayList<>(snap.size());

               for (Map.Entry<Integer, Overtick.GhostPos> en : snap) {
                  Entity real = mc.level.getEntity(en.getKey());
                  if (real != null) {
                     Overtick.GhostPos gp = en.getValue();
                     double ix = Mth.lerp(partialTick, gp.prevX, gp.x);
                     double iy = Mth.lerp(partialTick, gp.prevY, gp.y);
                     double iz = Mth.lerp(partialTick, gp.prevZ, gp.z);
                     boxes.add(real.getBoundingBox().move(ix - real.getX(), iy - real.getY(), iz - real.getZ()));
                     double halfH = real.getBbHeight() * 0.5;
                     ClarityRenderUtils.drawWorldLine(poseStack, new Vec3(real.getX(), real.getY() + halfH, real.getZ()), new Vec3(ix, iy + halfH, iz), outline, 2.0F);
                  }
               }

               if (!boxes.isEmpty()) {
                  ClarityRenderUtils.drawBoxes(poseStack, boxes, fill, outline);
               }
            } catch (Exception var24) {
            }
         }
      }
   }

   private static int parseRgb(String hex) {
      return ThemeColors.resolveRgb(hex, 6599935, false);
   }

   static {
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSystemChatPacket"), 1, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundPlayerChatPacket"), 2, 0);
      reg(safeClass("net.minecraft.network.protocol.common.ClientboundDisconnectPacket"), 3, 0);
      reg(safeClass("net.minecraft.network.protocol.common.ClientboundKeepAlivePacket"), 5, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundRespawnPacket"), 6, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket"), 7, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundAnimatePacket"), 8, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSoundPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSoundEntityPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundStopSoundPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundLevelEventPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundBossEventPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundTabListPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundClearTitlesPacket"), 4, 0);
      reg(safeClass("net.minecraft.network.protocol.common.ClientboundPingPacket"), 0, 1);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Pos"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$PosRot"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket"), 0, 2);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundAddEntityPacket"), 0, 4);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket"), 0, 4);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket"), 0, 4);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket"), 0, 4);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket"), 0, 8);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket"), 0, 8);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket"), 0, 8);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket"), 0, 8);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket"), 0, 8);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket"), 0, 16);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundExplodePacket"), 0, 16);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket"), 0, 32);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket"), 0, 32);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket"), 0, 32);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundBlockEventPacket"), 0, 32);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket"), 0, 32);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundGameEventPacket"), 0, 64);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket"), 0, 64);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetCameraPacket"), 0, 8192);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket"), 0, 128);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket"), 0, 128);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket"), 0, 4096);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundCooldownPacket"), 0, 2048);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundEntityEventPacket"), 0, 256);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket"), 0, 256);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetHealthPacket"), 0, 512);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundSetExperiencePacket"), 0, 1024);
      reg(safeClass("net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket"), 0, 16384);
   }

   private static final class Entry {
      final Packet<?> pkt;
      final long ts;

      Entry(Packet<?> pkt) {
         this.pkt = pkt;
         this.ts = System.currentTimeMillis();
      }
   }

   private static final class GhostPos {
      double x;
      double y;
      double z;
      float yRot;
      float xRot;
      float yHeadRot;
      float prevYRot;
      float prevXRot;
      float prevYHeadRot;
      double prevX;
      double prevY;
      double prevZ;
      double pendingX;
      double pendingY;
      double pendingZ;
      float pendingYRot;
      float pendingXRot;
      float pendingYHeadRot;
      final WalkAnimationState walkAnim = new WalkAnimationState();
      boolean swinging;
      int swingTime;
      float attackAnim;
      float oAttackAnim;
      HumanoidArm swingArm = HumanoidArm.RIGHT;
      int hurtTicks;
      boolean crouching;
      boolean sprinting;
      boolean swimming;
      boolean fallFlying;
      Pose pose = Pose.STANDING;
      float swimAmount;
      int tickCount;
      long commitNs;
      double lerpX;
      double lerpY;
      double lerpZ;
      int lerpSteps;
      float lerpYRot;
      float lerpXRot;
      float lerpYHeadRot;
      int lerpRotSteps;
      float bodyYaw;
      float prevBodyYaw;
      double renderX;
      double renderY;
      double renderZ;
      long lastRenderNs;

      GhostPos(double x, double y, double z, float yRot, float xRot, float yHeadRot) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.yRot = yRot;
         this.xRot = xRot;
         this.yHeadRot = yHeadRot;
         this.prevX = x;
         this.prevY = y;
         this.prevZ = z;
         this.prevYRot = yRot;
         this.prevXRot = xRot;
         this.prevYHeadRot = yHeadRot;
         this.pendingX = x;
         this.pendingY = y;
         this.pendingZ = z;
         this.pendingYRot = yRot;
         this.pendingXRot = xRot;
         this.pendingYHeadRot = yHeadRot;
         this.renderX = x;
         this.renderY = y;
         this.renderZ = z;
         this.lastRenderNs = System.nanoTime();
         this.lerpX = x;
         this.lerpY = y;
         this.lerpZ = z;
         this.lerpYRot = yRot;
         this.lerpXRot = xRot;
         this.lerpYHeadRot = yHeadRot;
         this.bodyYaw = yRot;
         this.prevBodyYaw = yRot;
      }
   }
}
