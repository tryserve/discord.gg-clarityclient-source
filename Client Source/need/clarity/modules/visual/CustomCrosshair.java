package need.clarity.modules.visual;

import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class CustomCrosshair extends ModuleBase {
   public static CustomCrosshair INSTANCE;
   private long hitNanos = -1L;
   private long killNanos = -1L;
   private LivingEntity lastTarget;
   private long lastTargetNanos = 0L;

   public CustomCrosshair() {
      super("Custom Crosshair", "HUD");
      INSTANCE = this;
      PerAttackEvents.register(this::onAttack);
   }

   public static boolean active() {
      CustomCrosshair c = INSTANCE;
      return c != null && c.isEnabled();
   }

   private void onAttack(EventStartAttack e) {
      if (this.isEnabled() && e.getTarget() instanceof LivingEntity le && le.isAlive()) {
         if (!this.getSetting("Players Only", true) || le instanceof Player) {
            this.hitNanos = System.nanoTime();
            this.lastTarget = le;
            this.lastTargetNanos = this.hitNanos;
         }
      }
   }

   public float hitAge(long now) {
      LivingEntity t = this.lastTarget;
      if (t != null) {
         if (now - this.lastTargetNanos > 1500000000L) {
            this.lastTarget = null;
         } else if (t.isDeadOrDying() || t.isRemoved() && t.getRemovalReason() != null && t.getRemovalReason().shouldDestroy()) {
            if (this.getSetting("Kill Effect", true)) {
               this.killNanos = now;
            }

            this.lastTarget = null;
         }
      }

      return this.hitNanos < 0L ? -1.0F : Math.min(5.0F, (float)(now - this.hitNanos) / 1.0E9F);
   }

   public float killAge(long now) {
      return this.killNanos < 0L ? -1.0F : Math.min(5.0F, (float)(now - this.killNanos) / 1.0E9F);
   }

   public String style() {
      return this.getSetting("Style", "Cross");
   }

   public String colorMode() {
      return this.getSetting("Color Mode", "Custom");
   }

   public String color() {
      return this.getSetting("Color", "#FFFFFF");
   }

   public String color2() {
      return this.getSetting("Second Colour", "#7A8CFF");
   }

   public String hitColor() {
      return this.getSetting("Hit Color", "#FF3B3B");
   }

   public String targetColor() {
      return this.getSetting("Target Colour", "#FF5050");
   }

   public float size() {
      return this.getSetting("Size", 6.0F);
   }

   public float gap() {
      return this.getSetting("Gap", 3.0F);
   }

   public float thickness() {
      return this.getSetting("Thickness", 2.0F);
   }

   public float dot() {
      return this.getSetting("Dot Size", 3.0F);
   }

   public float rotation() {
      return this.getSetting("Rotation", 0.0F);
   }

   public float spinSpeed() {
      return this.getSetting("Spin", 0.0F);
   }

   public boolean outline() {
      return this.getSetting("Outline", true);
   }

   public float outlineWidth() {
      return this.getSetting("Outline Thickness", 1.0F);
   }

   public float glow() {
      return this.getSetting("Glow", 0.6F);
   }

   public boolean dynamic() {
      return this.getSetting("Dynamic Spread", false);
   }

   public boolean hitmarker() {
      return this.getSetting("Hitmarker", true);
   }

   public boolean expand() {
      return this.getSetting("Hit Expand", true);
   }

   public boolean flash() {
      return this.getSetting("Hit Flash", true);
   }

   public boolean shockwave() {
      return this.getSetting("Shockwave", false);
   }

   public boolean cooldown() {
      return this.getSetting("Cooldown Ring", true);
   }

   public boolean targetTint() {
      return this.getSetting("Target Color", false);
   }

   public static Minecraft mc() {
      return Minecraft.getInstance();
   }
}
