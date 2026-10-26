package need.clarity.modules.movement;

import need.clarity.PerFrameEvents;
import need.clarity.PerMovementEvents;
import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.utility.Overtick;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class WindCharge extends ModuleBase {
   public static WindCharge INSTANCE;
   private String state = "IDLE";
   private int ticks = 0;
   private int shotsDone = 0;
   private boolean upPhase = false;
   private float savedYaw;
   private float savedPitch;
   private float targetPitch;
   private float appliedYaw;
   private float appliedPitch;
   private boolean aimActive = false;
   private int windSlot = -1;
   private int prevSlot = -1;
   private int lastSentSlot = -1;
   private boolean shouldJump = false;
   private boolean startedOvertick = false;
   private int pendingFires = 0;
   private volatile boolean knockbackSeen = false;
   private boolean jumpQueued = false;
   private static final int EXPLODE_TIMEOUT = 20;

   public WindCharge() {
      super("WindCharge", "Utility");
      INSTANCE = this;
      PerFrameEvents.register(this::onFrame);
      PerMovementEvents.register(this::onMovementInput);
      PerPacketEvents.register(this::onPacketReceive);
   }

   private void onPacketReceive(EventPacketReceive event) {
      if (this.isEnabled()) {
         try {
            Packet<?> pkt = event.getPacket();
            boolean explode = pkt instanceof ClientboundExplodePacket;
            if (!explode && pkt instanceof ClientboundSetEntityMotionPacket motion) {
               Minecraft mc = Minecraft.getInstance();
               explode = mc.player != null && motion.id() == mc.player.getId();
            }

            if (explode) {
               this.knockbackSeen = true;
            }
         } catch (Throwable var6) {
         }
      }
   }

   private void onMovementInput(MoveFix.EventMovementInput event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (this.pendingFires > 0) {
            int n = this.pendingFires;
            this.pendingFires = 0;

            for (int i = 0; i < n; i++) {
               this.fire(mc);
            }
         }

         if (this.shouldJump && this.jump()) {
            if (mc.player.onGround()) {
               event.jump = true;
               this.shouldJump = false;
            }
         }
      }
   }

   private void onFrame() {
      if (this.isEnabled() && this.silent() && this.aimActive) {
         if (!"IDLE".equals(this.state) && !"DONE".equals(this.state)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               AimAssist.setSilentRotation(this.appliedYaw, this.appliedPitch);
            }
         } else {
            this.aimActive = false;
         }
      }
   }

   private boolean silent() {
      return this.getSetting("Silent Aim", true);
   }

   private float target() {
      return this.getSetting("Target Pitch", 90.0F);
   }

   private int aimTicks() {
      return Math.max(1, this.getSetting("Aim Ticks", 1));
   }

   private int restoreTicks() {
      return Math.max(1, this.getSetting("Restore Ticks", 1));
   }

   private int shotDelay() {
      return Math.max(0, this.getSetting("Shot Delay", 10));
   }

   private boolean combo() {
      return this.getSetting("Combo", true);
   }

   private int comboCount() {
      return Math.min(10, Math.max(2, this.getSetting("Combo Count", 3)));
   }

   private boolean jump() {
      return this.getSetting("Jump", true);
   }

   private int upRange() {
      return Math.max(1, this.getSetting("Up Range", 3));
   }

   private String mode() {
      return this.getSetting("Mode", "Immediate");
   }

   private int delay() {
      return Math.max(1, this.getSetting("Delay", 10));
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         this.setEnabled(false);
      } else {
         this.windSlot = this.findWindSlot();
         if (this.windSlot < 0) {
            this.setEnabled(false);
         } else {
            this.prevSlot = -1;
            this.savedYaw = mc.player.getYRot();
            this.savedPitch = mc.player.getXRot();
            this.upPhase = this.hasBlockOverhead();
            this.targetPitch = this.upPhase ? -90.0F : Math.max(-90.0F, Math.min(90.0F, this.target()));
            this.state = "AIM";
            this.ticks = 0;
            this.shotsDone = 0;
            this.knockbackSeen = false;
            this.jumpQueued = false;
            this.pendingFires = 0;
            this.enableOvertick();
         }
      }
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      this.aimActive = false;
      this.shouldJump = false;
      this.pendingFires = 0;
      this.disableOvertick();
      if (mc.player != null) {
         this.selectSlot(this.prevSlot);
         if (this.silent()) {
            AimAssist.clearSilentRotation();
         }
      }

      this.state = "IDLE";
   }

   private static Overtick overtick() {
      return ModuleRegistry.get("Overtick") instanceof Overtick o ? o : null;
   }

   private void enableOvertick() {
      Overtick ov = overtick();
      if (ov != null) {
         ov.setOverrideMask(17);
         this.startedOvertick = !ov.isEnabled();
         if (this.startedOvertick) {
            try {
               ov.setEnabled(true);
            } catch (Throwable var3) {
            }
         }
      }
   }

   private void disableOvertick() {
      if (this.startedOvertick) {
         Overtick ov = overtick();
         if (ov != null) {
            try {
               ov.releaseAll();
            } catch (Throwable var5) {
            }

            try {
               ov.clearOverrideMask();
            } catch (Throwable var4) {
            }

            if (ov.isEnabled()) {
               try {
                  ov.setEnabled(false);
               } catch (Throwable var3) {
               }
            }
         }

         this.startedOvertick = false;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         switch (this.state) {
            case "AIM":
               this.ticks++;
               float t = Math.min(1.0F, (float)this.ticks / this.aimTicks());
               float np = this.savedPitch + (this.targetPitch - this.savedPitch) * t;
               this.applyRotation(this.savedYaw, this.snapGcd(np));
               if (this.ticks >= this.aimTicks()) {
                  this.ticks = 0;
                  this.state = "FIRE";
               }
               break;
            case "FIRE":
               if (this.ticks == 0) {
                  if (this.shotsDone == 0) {
                     this.selectSlot(this.windSlot);
                  }

                  this.applyRotation(this.savedYaw, this.snapGcd(this.targetPitch));
                  this.pendingFires++;
                  this.shotsDone++;
               }

               if (this.combo() && this.shotsDone < this.comboCount()) {
                  this.ticks++;
                  if (this.ticks > this.shotDelay()) {
                     this.ticks = 0;
                  }
               } else {
                  this.ticks = 0;
                  if (!"Hold".equalsIgnoreCase(this.mode())) {
                     this.disableOvertick();
                  }

                  this.state = "WAIT";
               }
               break;
            case "WAIT":
               this.ticks++;
               boolean hold = "Hold".equalsIgnoreCase(this.mode());
               Overtick ov = overtick();
               if (this.knockbackSeen && !this.jumpQueued) {
                  this.shouldJump = true;
                  this.jumpQueued = true;
                  this.ticks = 0;
               } else if (hold) {
                  if (ov == null || !ov.hasQueued()) {
                     this.knockbackSeen = false;
                     this.jumpQueued = false;
                     this.disableOvertick();
                     this.ticks = 0;
                     this.state = "RESTORE";
                  }
               } else if (this.jumpQueued) {
                  this.knockbackSeen = false;
                  this.jumpQueued = false;
                  this.ticks = 0;
                  this.state = "RESTORE";
               }

               if (this.ticks > Math.max(20, this.delay()) && !this.knockbackSeen && !this.jumpQueued) {
                  this.disableOvertick();
                  this.ticks = 0;
                  this.state = this.jump() ? "JUMP" : "RESTORE";
               }
               break;
            case "JUMP":
               this.shouldJump = true;
               this.state = "RESTORE";
               break;
            case "RESTORE":
               if (this.ticks == 0) {
                  this.selectSlot(this.prevSlot);
               }

               this.ticks++;
               float rt = Math.min(1.0F, (float)this.ticks / this.restoreTicks());
               float rnp = this.targetPitch + (this.savedPitch - this.targetPitch) * rt;
               this.applyRotation(this.savedYaw, this.snapGcd(rnp));
               if (this.ticks >= this.restoreTicks()) {
                  this.applyRotation(this.savedYaw, this.savedPitch);
                  if (this.silent()) {
                     AimAssist.clearSilentRotation();
                  }

                  this.state = "DONE";
               }
               break;
            case "DONE":
               this.setEnabled(false);
         }
      } else {
         this.aimActive = false;
         if (this.isEnabled()) {
            this.setEnabled(false);
         }
      }
   }

   private void applyRotation(float yaw, float pitch) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.appliedYaw = yaw;
         this.appliedPitch = pitch;
         this.aimActive = true;
         if (this.silent()) {
            AimAssist.setSilentRotation(yaw, pitch);
         } else {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
         }
      }
   }

   private float snapGcd(float pitch) {
      Minecraft mc = Minecraft.getInstance();

      float g;
      try {
         double s = (Double)mc.options.sensitivity().get();
         float f = (float)s * 0.6F + 0.2F;
         g = f * f * f * 8.0F * 0.15F;
      } catch (Throwable t) {
         g = 0.15F;
      }

      return g < 1.0E-4F ? pitch : Math.round(pitch / g) * g;
   }

   private void fire(Minecraft mc) {
      try {
         LocalPlayer p = mc.player;
         if (p == null || mc.gameMode == null) {
            return;
         }

         float yaw = this.silent() ? AimAssist.getSilentServerYaw() : p.getYRot();
         float pitch = this.silent() ? AimAssist.getSilentServerPitch() : p.getXRot();
         float oY = p.getYRot();
         float oP = p.getXRot();
         p.setYRot(yaw);
         p.setXRot(pitch);
         if (p.isUsingItem()) {
            p.stopUsingItem();
         }

         mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
         p.setYRot(oY);
         p.setXRot(oP);
      } catch (Throwable var7) {
      }
   }

   private boolean hasBlockOverhead() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         BlockPos base = mc.player.blockPosition();

         for (int dy = 1; dy <= this.upRange(); dy++) {
            BlockState st = mc.level.getBlockState(base.above(dy));
            if (st != null && !st.isAir()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private int findWindSlot() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return -1;
      }

      Inventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && String.valueOf(s.getItem().getDescriptionId()).contains("wind_charge")) {
            return i;
         }
      }

      return -1;
   }

   private void selectSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            if (this.prevSlot == -1) {
               this.prevSlot = mc.player.getInventory().getSelectedSlot();
            }

            if (slot != this.lastSentSlot) {
               this.lastSentSlot = slot;

               try {
                  InventoryHelper.setSelected(mc.player.getInventory(), slot);
                  mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
               } catch (Throwable var4) {
               }
            }
         }
      }
   }
}
