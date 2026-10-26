package need.clarity.bridge;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.movement.SpinBot;
import need.clarity.modules.utility.Overtick;
import need.clarity.modules.visual.Capes;
import need.clarity.modules.visual.Chams;
import need.clarity.modules.visual.MoveAnimations;
import need.clarity.modules.visual.PlayerModel;
import need.clarity.modules.visual.Wings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

public final class AvatarRendererBridge {
   private AvatarRendererBridge() {
   }

   public static void onExtractRenderStateTail(Object playerObj, Object stateObj, float tickProgress) {
      Avatar player = (Avatar)playerObj;
      AvatarRenderState state = (AvatarRenderState)stateObj;

      try {
         Chams.updateState(player, state);
      } catch (Throwable var12) {
      }

      try {
         if (player == Minecraft.getInstance().player) {
            Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
            if (overtick != null && overtick.isEnabled() && overtick.isElytraStopHeld() && (state.isFallFlying || state.pose == Pose.FALL_FLYING)) {
               state.isFallFlying = false;
               state.pose = Pose.STANDING;
               state.fallFlyingTimeInTicks = 0.0F;
               state.shouldApplyFlyingYRot = false;
               state.flyingYRot = 0.0F;
            }
         }
      } catch (Throwable var14) {
      }

      try {
         if (player == Minecraft.getInstance().player) {
            Capes capes = Capes.INSTANCE;
            if (capes != null && capes.isEnabled()) {
               Identifier tex = capes.getActiveTexture();
               if (tex != null && state.skin != null) {
                  Texture cape = new ResourceTexture(tex, tex);
                  state.skin = new PlayerSkin(state.skin.body(), cape, cape, state.skin.model(), state.skin.secure());
                  state.showCape = true;
               }
            }
         }
      } catch (Throwable var11) {
      }

      try {
         if (player == Minecraft.getInstance().player && Wings.shouldReplaceElytra() && state.chestEquipment != null && state.chestEquipment.has(DataComponents.GLIDER)) {
            state.chestEquipment = ItemStack.EMPTY;
         }
      } catch (Throwable var10) {
      }

      try {
         MoveAnimations.onExtract(player, state);
      } catch (Throwable var9) {
      }

      try {
         if (player == Minecraft.getInstance().player && PlayerModel.isActive()) {
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
            state.headEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
            if (!state.chestEquipment.has(DataComponents.GLIDER)) {
               state.chestEquipment = ItemStack.EMPTY;
            }

            state.parrotOnLeftShoulder = null;
            state.parrotOnRightShoulder = null;
            PlayerModel.markModelState(state);
         } else {
            PlayerModel.forget(state);
         }
      } catch (Throwable var13) {
      }

      try {
         if (SpinBot.isClientMode() && player == Minecraft.getInstance().player) {
            state.bodyRot = SpinBot.yaw(tickProgress);
            state.yRot = 0.0F;
         }
      } catch (Throwable var8) {
      }
   }
}
