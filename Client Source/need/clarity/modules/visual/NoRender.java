package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class NoRender extends ModuleBase {
   public static volatile NoRender INSTANCE;

   public NoRender() {
      super("NoRender", "Visual");
      INSTANCE = this;
   }

   public boolean shouldCancelFire() {
      return !this.isEnabled() ? false : this.getSetting("Fire Overlay", true);
   }

   public boolean shouldCancelNausea() {
      return !this.isEnabled() ? false : this.getSetting("Nausea Overlay", true);
   }

   public boolean shouldCancelPortal() {
      return !this.isEnabled() ? false : this.getSetting("Portal Overlay", true);
   }

   public boolean shouldCancelSpyglass() {
      return !this.isEnabled() ? false : this.getSetting("Spyglass Overlay", true);
   }

   public boolean shouldCancelScoreboard() {
      return !this.isEnabled() ? false : this.getSetting("Scoreboard", false);
   }

   public boolean shouldCancelVignette() {
      return !this.isEnabled() ? false : this.getSetting("Vignette", false);
   }

   public boolean shouldCancelTotemAnimation() {
      return !this.isEnabled() ? false : this.getSetting("Totem Animation", false);
   }

   public static boolean hideExplosionParticlesAt(double x, double y, double z) {
      NoRender nr = INSTANCE;
      return nr != null && nr.shouldCancelExplosionParticles() ? true : CrystalView.hideParticlesAt(x, y, z);
   }

   public boolean shouldCancelExplosionParticles() {
      return !this.isEnabled() ? false : this.getSetting("Explosion Particles", false);
   }
}
