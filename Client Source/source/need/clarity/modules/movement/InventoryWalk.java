package need.clarity.modules.movement;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import need.clarity.PerMovementEvents;
import need.clarity.PerPacketSendEvents;
import need.clarity.event.EventPacketSend;
import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.CreativeModeTabs;
import org.lwjgl.glfw.GLFW;

public class InventoryWalk extends ModuleBase {
   private final Queue<Packet<?>> queuedPackets = new ConcurrentLinkedDeque<>();
   private boolean flushing = false;
   private boolean waitingToFlush = false;
   private int ticksStopped = 0;

   public InventoryWalk() {
      super("Inventory Walk", "Movement");
      PerMovementEvents.register(this::onMovementInput);
      PerPacketSendEvents.register(this::onPacketSend);
   }

   @Override
   public void onDisable() {
      this.flushPackets();
      this.waitingToFlush = false;
      this.ticksStopped = 0;
   }

   @Override
   public void onEnable() {
      this.queuedPackets.clear();
      this.waitingToFlush = false;
      this.ticksStopped = 0;
   }

   @Override
   public void onTick() {
      if (this.isEnabled() && this.isGrim()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (!this.queuedPackets.isEmpty() && !this.waitingToFlush && !(mc.screen instanceof AbstractContainerScreen)) {
               this.waitingToFlush = true;
               this.ticksStopped = 0;
            }
         }
      }
   }

   private void onMovementInput(MoveFix.EventMovementInput event) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (this.isGrim() && this.waitingToFlush) {
               event.forward = false;
               event.back = false;
               event.left = false;
               event.right = false;
               event.sprint = false;
            } else {
               Screen screen = mc.screen;
               if (screen != null && this.isAllowedScreen(screen)) {
                  long win = mc.getWindow().handle();
                  event.forward = this.isKeyDown(win, mc.options.keyUp);
                  event.back = this.isKeyDown(win, mc.options.keyDown);
                  event.left = this.isKeyDown(win, mc.options.keyLeft);
                  event.right = this.isKeyDown(win, mc.options.keyRight);
                  event.jump = this.isKeyDown(win, mc.options.keyJump);
                  event.sneak = this.isKeyDown(win, mc.options.keyShift);
                  event.sprint = this.isKeyDown(win, mc.options.keySprint);
               }
            }
         }
      }
   }

   private void onPacketSend(EventPacketSend event) {
      if (this.isEnabled() && this.isGrim()) {
         if (!this.flushing) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
               Packet<?> packet = event.getPacket();
               if (!(mc.screen instanceof AbstractContainerScreen)
                  || !(packet instanceof ServerboundContainerClickPacket)
                     && !(packet instanceof ServerboundContainerClosePacket)
                     && !(packet instanceof ServerboundChatPacket)
                     && !(packet instanceof ServerboundChatCommandPacket)) {
                  if (this.waitingToFlush && packet instanceof ServerboundMovePlayerPacket) {
                     this.ticksStopped++;
                     if (this.ticksStopped >= 1) {
                        this.flushPackets();
                        this.waitingToFlush = false;
                        this.ticksStopped = 0;
                     }
                  }
               } else {
                  this.queuedPackets.add(packet);
                  event.cancel();
                  this.waitingToFlush = true;
                  this.ticksStopped = 0;
               }
            }
         }
      }
   }

   private void flushPackets() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         this.queuedPackets.clear();
      } else {
         ClientPacketListener connection = mc.getConnection();
         if (connection == null) {
            this.queuedPackets.clear();
         } else {
            this.flushing = true;

            while (!this.queuedPackets.isEmpty()) {
               Packet<?> pkt = this.queuedPackets.poll();
               if (pkt != null) {
                  connection.send(pkt);
               }
            }

            this.flushing = false;
         }
      }
   }

   private boolean isAllowedScreen(Screen screen) {
      if (screen instanceof InventoryScreen) {
         return true;
      } else if (screen instanceof CreativeModeInventoryScreen) {
         return !this.isCreativeSearchTabOpen();
      } else {
         return screen instanceof AbstractContainerScreen ? !this.hasEditBox(screen) : false;
      }
   }

   private boolean isCreativeSearchTabOpen() {
      return AccessorCompat.creativeSelectedTab() == CreativeModeTabs.searchTab();
   }

   private boolean hasEditBox(Screen screen) {
      return screen.children().stream().anyMatch(EditBox.class::isInstance);
   }

   private boolean isKeyDown(long win, KeyMapping key) {
      Key bound = AccessorCompat.getBoundKey(key);
      if (bound == null) {
         return false;
      }

      int code = bound.getValue();
      return bound.getType() == Type.MOUSE ? GLFW.glfwGetMouseButton(win, code) == 1 : GLFW.glfwGetKey(win, code) == 1;
   }

   private boolean isGrim() {
      return this.getSetting("Grim", false);
   }
}
