package need.clarity.modules.utility;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import need.clarity.Unload;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.utility.crystalhelpers.OptOutCache;
import need.clarity.modules.utility.crystalhelpers.OptimizerPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.util.Util;

public class CrystalOptimizer extends ModuleBase {
   private static CrystalOptimizer instance;
   private final OptOutCache optOutCache = new OptOutCache();
   private static boolean packetsRegistered = false;
   private static final Component PREFIX = Component.literal("[")
      .withStyle(ChatFormatting.GRAY)
      .append(Component.literal("Crystal Optimizer").withStyle(ChatFormatting.AQUA))
      .append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

   public static boolean isActive() {
      return instance != null && instance.isEnabled() && !instance.optOutCache.isOptedOut();
   }

   public CrystalOptimizer() {
      super("Crystal Optimizer", "utility");
      instance = this;
      if (!packetsRegistered) {
         packetsRegistered = true;
         registerPacketTypes();
      }

      this.registerPacketReceivers();
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         if (!Unload.isUnloaded()) {
            if (!client.isLocalServer()) {
               sender.sendPacket(new OptimizerPackets.Version(1, 0, 0, false));
               String key = serverKey(client);
               if (this.optOutCache.isServerOptedOut(key)) {
                  this.optOutCache.setOptedOut(true);
               }
            }
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         if (!Unload.isUnloaded()) {
            this.optOutCache.clearCurrentSession();
         }
      });
   }

   @Override
   public void onEnable() {
      this.optOutCache.clearCurrentSession();
   }

   @Override
   public void onDisable() {
   }

   private static void registerPacketTypes() {
      PayloadTypeRegistry.clientboundConfiguration().register(OptimizerPackets.OptOut.TYPE, OptimizerPackets.OptOut.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(OptimizerPackets.OptOut.TYPE, OptimizerPackets.OptOut.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(OptimizerPackets.Challenge.TYPE, OptimizerPackets.Challenge.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(OptimizerPackets.OptOutAck.TYPE, OptimizerPackets.OptOutAck.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(OptimizerPackets.Version.TYPE, OptimizerPackets.Version.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(OptimizerPackets.ChallengeResponse.TYPE, OptimizerPackets.ChallengeResponse.STREAM_CODEC);
   }

   private void registerPacketReceivers() {
      ClientPlayNetworking.registerGlobalReceiver(OptimizerPackets.OptOut.TYPE, (payload, context) -> {
         Minecraft client = context.client();
         String key = serverKey(client);
         if (key != null) {
            this.optOutCache.markOptedOut(key);
         } else {
            this.optOutCache.setOptedOut(true);
         }

         if (!this.optOutCache.hasNotified(key)) {
            CompletableFuture.delayedExecutor(2L, TimeUnit.SECONDS, Util.backgroundExecutor()).execute(() -> client.execute(() -> {
               if (client.player != null) {
                  if (!this.optOutCache.hasNotified(key)) {
                     this.optOutCache.markNotified(key);
                     client.player.sendSystemMessage(buildOptOutMessage());
                  }
               }
            }));
         }

         ClientPlayNetworking.send(OptimizerPackets.OptOutAck.INSTANCE);
      });
      ClientPlayNetworking.registerGlobalReceiver(OptimizerPackets.Challenge.TYPE, (payload, context) -> ClientPlayNetworking.send(new OptimizerPackets.ChallengeResponse(payload.id())));
   }

   private static String serverKey(Minecraft client) {
      return client.getCurrentServer() != null ? client.getCurrentServer().ip : null;
   }

   private static Component buildOptOutMessage() {
      Component hover = Component.empty()
         .append(Component.literal("Why is this disabled?\n").withStyle(ChatFormatting.AQUA))
         .append(Component.literal("• This server has requested Crystal Optimizer to be disabled.\n").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("• This may be to enforce server rules or avoid compatibility issues.\n").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("\nThis only applies while you are connected to this server.").withStyle(ChatFormatting.DARK_GRAY));
      Style hoverStyle = Style.EMPTY.withHoverEvent(new ShowText(hover));
      Component message = Component.literal("Crystal Optimizer disabled on this server.").withStyle(hoverStyle.withColor(ChatFormatting.RED));
      return PREFIX.copy().withStyle(hoverStyle).append(message);
   }
}
