package need.clarity.mixin;

import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.utility.Overtick;
import need.clarity.modules.visual.Capes;
import need.clarity.modules.visual.PlayerModel;
import need.clarity.modules.visual.Wings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void cl$avatarRenderStateHead(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$hideElytraVisual(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      try {
         if (player != Minecraft.getInstance().player) {
            return;
         }

         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         if (overtick == null || !overtick.isEnabled()) {
            return;
         }

         if (!overtick.isElytraStopHeld()) {
            return;
         }

         if (!state.isFallFlying && state.pose != Pose.FALL_FLYING) {
            return;
         }

         state.isFallFlying = false;
         state.pose = Pose.STANDING;
         state.fallFlyingTimeInTicks = 0.0F;
         state.shouldApplyFlyingYRot = false;
         state.flyingYRot = 0.0F;
      } catch (Throwable var6) {
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$clarityCape(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      try {
         if (player != Minecraft.getInstance().player) {
            return;
         }

         Capes capes = Capes.INSTANCE;
         if (capes == null || !capes.isEnabled()) {
            return;
         }

         Identifier tex = capes.getActiveTexture();
         if (tex == null || state.skin == null) {
            return;
         }

         Texture cape = new ResourceTexture(tex, tex);
         state.skin = new PlayerSkin(state.skin.body(), cape, cape, state.skin.model(), state.skin.secure());
         state.showCape = true;
      } catch (Throwable var8) {
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$replaceElytraWithWings(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      try {
         if (player != Minecraft.getInstance().player) {
            return;
         }

         if (!Wings.shouldReplaceElytra()) {
            return;
         }

         if (state.chestEquipment != null && state.chestEquipment.has(DataComponents.GLIDER)) {
            state.chestEquipment = ItemStack.EMPTY;
         }
      } catch (Throwable var6) {
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void cl$playerModelHideBody(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      try {
         if (player != Minecraft.getInstance().player || !PlayerModel.isActive()) {
            PlayerModel.forget(state);
            return;
         }

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
      } catch (Throwable var6) {
      }
   }
}
