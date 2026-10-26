package need.clarity.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import need.clarity.PerPacketEvents;
import need.clarity.event.EventPacketReceive;
import need.clarity.modules.utility.CrystalOptimizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ConnectionBridge {
   private ConnectionBridge() {
   }

   public static boolean onPacketReceive(Packet<?> packet, PacketListener listener) {
      try {
         EventPacketReceive event = PerPacketEvents.fire(new EventPacketReceive(packet));
         return event.isCancelled();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onConnectionSend(Packet<?> packet) {
      try {
         if (!(packet instanceof ServerboundAttackPacket attackPacket)) {
            return;
         }

         if (!CrystalOptimizer.isActive()) {
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            return;
         }

         int entityId;
         try {
            Field f = attackPacket.getClass().getDeclaredField("entityId");
            f.setAccessible(true);
            entityId = f.getInt(attackPacket);
         } catch (Throwable t) {
            try {
               Method m = attackPacket.getClass().getMethod("entityId");
               entityId = (Integer)m.invoke(attackPacket);
            } catch (Throwable t2) {
               return;
            }
         }

         if (!(mc.level.getEntity(entityId) instanceof EndCrystal crystal)) {
            return;
         }

         LocalPlayer player = mc.player;
         if (player == null) {
            return;
         }

         double damage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
         ItemStack mainHand = player.getMainHandItem();
         if (!mainHand.isEmpty()) {
            double[] weaponDmg = new double[]{0.0};
            mainHand.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
               if (Attributes.ATTACK_DAMAGE.equals(attr)) {
                  weaponDmg[0] += mod.amount();
               }
            });
            damage += weaponDmg[0];
         }

         MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
         if (strength != null) {
            damage += 3.0 * (strength.getAmplifier() + 1);
         }

         MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
         if (weakness != null) {
            damage -= 4.0 * (weakness.getAmplifier() + 1);
         }

         if (damage <= 0.0) {
            return;
         }

         crystal.remove(RemovalReason.KILLED);
         crystal.gameEvent(GameEvent.ENTITY_DIE);
         if (mc.hitResult != null && mc.crosshairPickEntity == crystal) {
            mc.crosshairPickEntity = null;
            mc.hitResult = player.pick(player.blockInteractionRange(), 1.0F, false);
         }
      } catch (Throwable var14) {
      }
   }
}
