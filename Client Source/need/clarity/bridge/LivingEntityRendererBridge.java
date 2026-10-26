package need.clarity.bridge;

import java.lang.reflect.Method;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.utility.Overtick;
import need.clarity.modules.visual.Chams;
import need.clarity.modules.visual.ServerRotations;
import need.clarity.render.ChamsCollector;
import need.clarity.render.ChamsRenderer;
import need.clarity.render.ThroughWalls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class LivingEntityRendererBridge {
   private LivingEntityRendererBridge() {
   }

   public static void onExtractRenderStateTail(Object entity, Object state, float tickDelta) {
      try {
         if (!(entity instanceof LocalPlayer)) {
            return;
         }

         if (entity != Minecraft.getInstance().player) {
            return;
         }

         if (!(state instanceof LivingEntityRenderState lrs)) {
            return;
         }

         if (!ServerRotations.shouldApply()) {
            return;
         }

         if (AimAssist.hasSilentRotation()) {
            lrs.xRot = ServerRotations.serverPitchLerp(tickDelta);
         }
      } catch (Throwable var4) {
      }
   }

   public static Object overrideRenderType(Object renderer, Object stateObj, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
      try {
         LivingEntityRenderState state = (LivingEntityRenderState)stateObj;
         Integer tint = Chams.getStateTint(state);
         if (tint != null) {
            boolean throughWalls = Chams.isThroughWalls();
            ChamsCollector.active = throughWalls;
            ChamsCollector.hideArmour = !Chams.isRenderArmour();
            ChamsCollector.armourThroughWalls = Chams.isArmourThroughWalls();
            if (Chams.isNormal()) {
               try {
                  Method m = renderer.getClass().getMethod("getTextureLocation", LivingEntityRenderState.class);
                  m.setAccessible(true);
                  Object rl = m.invoke(renderer, state);
                  Object skin = RenderTypes.class.getMethod("entityTranslucentCullItemTarget", Identifier.class).invoke(null, rl);
                  if (throughWalls) {
                     return ThroughWalls.convert((RenderType)skin);
                  }

                  return skin;
               } catch (Throwable t) {
                  return null;
               }
            }

            return ChamsRenderer.modelRenderType(throughWalls);
         }

         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         if (overtick != null && overtick.isGhostState(state)) {
            try {
               Method m = renderer.getClass().getMethod("getTextureLocation", LivingEntityRenderState.class);
               m.setAccessible(true);
               Object rl = m.invoke(renderer, state);
               return RenderTypes.class.getMethod("entityTranslucentCullItemTarget", Identifier.class).invoke(null, rl);
            } catch (Throwable t) {
               return null;
            }
         }
      } catch (Throwable var13) {
      }

      return null;
   }

   public static int overrideModelTint(Object stateObj) {
      try {
         LivingEntityRenderState state = (LivingEntityRenderState)stateObj;
         Integer tint = Chams.getStateTint(state);
         if (tint != null) {
            return tint;
         }

         Overtick overtick = (Overtick)ModuleRegistry.get("Overtick");
         if (overtick != null && overtick.isGhostState(state)) {
            int alpha = overtick.ghostOpacityAlpha();
            return alpha << 24 | 16777215;
         }
      } catch (Throwable var5) {
      }

      return Integer.MIN_VALUE;
   }

   public static Object wrapCollector(Object rendererObj, Object collectorObj) {
      try {
         Chams chams = Chams.INSTANCE;
         if (chams == null || !chams.isEnabled()) {
            return collectorObj;
         } else {
            return !(rendererObj instanceof AvatarRenderer) ? collectorObj : ChamsCollector.wrap((SubmitNodeCollector)collectorObj);
         }
      } catch (Throwable t) {
         return collectorObj;
      }
   }
}
