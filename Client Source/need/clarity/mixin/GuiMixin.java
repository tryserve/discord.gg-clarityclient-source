package need.clarity.mixin;

import need.clarity.Unload;
import need.clarity.modules.visual.NoRender;
import need.clarity.render.CrosshairRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
   @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
   private void clarity$customCrosshair(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (!Unload.isUnloaded() && CrosshairRenderer.render(context)) {
         ci.cancel();
      }
   }

   @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
   private void clarity$noNausea(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelNausea()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void clarity$noPortal(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelPortal()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
   private void clarity$noSpyglass(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelSpyglass()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void clarity$noScoreboard(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelScoreboard()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
   private void clarity$noVignette(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
      NoRender module = NoRender.INSTANCE;
      if (module != null && module.shouldCancelVignette()) {
         ci.cancel();
      }
   }
}
