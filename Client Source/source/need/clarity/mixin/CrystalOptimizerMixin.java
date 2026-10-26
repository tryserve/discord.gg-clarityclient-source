package need.clarity.mixin;

import need.clarity.modules.utility.CrystalOptimizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
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
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class CrystalOptimizerMixin {
   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
   private void clarity$onPacketSend(Packet<?> packet, CallbackInfo ci) {
      if (packet instanceof ServerboundAttackPacket mc) {
         ServerboundAttackPacket var10000 = mc;
         int var12;

         try {
            var12 = var10000.entityId();
         } catch (Throwable var9) {
            throw new MatchException(var9.toString(), var9);
         }

         int target = var12;
         if (true) {
            int entityId = target;
            if (!CrystalOptimizer.isActive()) {
               return;
            }

            Minecraft mcx = Minecraft.getInstance();
            if (mcx.level == null) {
               return;
            }

            if (mcx.level.getEntity(entityId) instanceof EndCrystal crystal) {
               LocalPlayer player = mcx.player;
               if (player == null) {
                  return;
               }

               if (this.clarity$canDestroyCrystal(player)) {
                  this.clarity$destroyCrystal(crystal);
                  this.clarity$retargetCrosshair(mcx, crystal);
               }

               return;
            }

            return;
         }
      }
   }

   @Unique
   private boolean clarity$canDestroyCrystal(LocalPlayer player) {
      double damage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
      damage += this.clarity$getWeaponDamage(player.getMainHandItem());
      MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
      if (strength != null) {
         damage += 3.0 * (strength.getAmplifier() + 1);
      }

      MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
      if (weakness != null) {
         damage -= 4.0 * (weakness.getAmplifier() + 1);
      }

      return damage > 0.0;
   }

   @Unique
   private double clarity$getWeaponDamage(ItemStack item) {
      if (item.isEmpty()) {
         return 0.0;
      }

      double[] sum = new double[]{0.0};
      item.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
         if (Attributes.ATTACK_DAMAGE.equals(attribute)) {
            sum[0] += modifier.amount();
         }
      });
      return sum[0];
   }

   @Unique
   private void clarity$destroyCrystal(EndCrystal crystal) {
      crystal.remove(RemovalReason.KILLED);
      crystal.gameEvent(GameEvent.ENTITY_DIE);
   }

   @Unique
   private void clarity$retargetCrosshair(Minecraft mc, EndCrystal crystal) {
      LocalPlayer player = mc.player;
      if (player != null && mc.hitResult != null && mc.crosshairPickEntity == crystal) {
         HitResult retraced = player.pick(player.blockInteractionRange(), 1.0F, false);
         mc.crosshairPickEntity = null;
         mc.hitResult = retraced;
      }
   }
}
