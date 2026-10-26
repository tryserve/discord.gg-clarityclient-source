package need.clarity.modules.utility;

import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.world.InteractionHand;

public class Disabler extends ModuleBase {
   private int tickCount = 0;

   public Disabler() {
      super("Disabler", "utility");
      PerPacketEvents.register(this::onPacketReceive);
   }

   @Override
   public void onEnable() {
      this.tickCount = 0;
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.player.connection != null) {
         String mode = this.getSetting("Mode", "Spartan Trident");
         if ("Spartan Trident".equals(mode)) {
            this.tickSpartanTrident(mc);
         }
      }
   }

   private void tickSpartanTrident(Minecraft mc) {
      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      this.tickCount++;
   }

   private void onPacketReceive(EventPacketReceive event) {
      if (this.isEnabled()) {
         if ("Grim NoFlag".equals(this.getSetting("Mode", "Spartan Trident"))) {
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket posPacket) {
               event.cancel();
               int teleportId = posPacket.id();
               Minecraft mc = Minecraft.getInstance();
               mc.execute(() -> {
                  if (mc.player != null && mc.player.connection != null) {
                     mc.player.connection.send(new ServerboundAcceptTeleportationPacket(teleportId));
                  }
               });
            }
         }
      }
   }
}
