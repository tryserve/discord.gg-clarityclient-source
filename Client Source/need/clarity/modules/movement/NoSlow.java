package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemUseAnimation;

public class NoSlow extends ModuleBase {
   private static NoSlow instance;
   private boolean consuming;
   private boolean fullSpeed;
   private LocalPlayer sessionPlayer;
   private int lastTick = Integer.MIN_VALUE;
   private int lastUseTime;
   private int envLastTick = Integer.MIN_VALUE;
   private boolean envFullSpeed;
   private LocalPlayer envPlayer;

   public NoSlow() {
      super("NoSlow", "Movement");
      instance = this;
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         this.reset();
      } else {
         if (!this.isEnabled() || !isConsuming(mc.player)) {
            this.reset();
         }
      }
   }

   public static boolean skipEnv(String key) {
      NoSlow n = instance;
      if (n != null && n.isEnabled() && n.getSetting(key, true)) {
         LocalPlayer p = Minecraft.getInstance().player;
         return p != null && n.isEnvFullTick(p);
      } else {
         return false;
      }
   }

   private boolean isEnvFullTick(LocalPlayer player) {
      int now = player.tickCount;
      if (now == this.envLastTick && this.envPlayer == player) {
         return this.envFullSpeed;
      }

      if (this.envPlayer == player && now == this.envLastTick + 1) {
         this.envFullSpeed = !this.envFullSpeed;
      } else {
         this.envFullSpeed = false;
      }

      this.envPlayer = player;
      this.envLastTick = now;
      return this.envFullSpeed;
   }

   public static boolean shouldSkipSlow(LocalPlayer player) {
      if (instance != null && instance.isEnabled()) {
         if (!instance.getSetting("Items", true)) {
            return false;
         } else if (!isConsuming(player)) {
            instance.reset();
            return false;
         } else {
            return instance.isFullTick(player);
         }
      } else {
         return false;
      }
   }

   private boolean isFullTick(LocalPlayer player) {
      int useTime = player.getTicksUsingItem();
      if (!this.consuming || this.sessionPlayer != player || useTime < this.lastUseTime) {
         this.consuming = true;
         this.sessionPlayer = player;
         this.fullSpeed = false;
         this.lastTick = player.tickCount;
      } else if (player.tickCount != this.lastTick) {
         this.lastTick = player.tickCount;
         this.fullSpeed = !this.fullSpeed;
      }

      this.lastUseTime = useTime;
      return this.fullSpeed;
   }

   private static boolean isConsuming(LocalPlayer player) {
      if (player != null && player.isUsingItem() && !player.isPassenger()) {
         ItemUseAnimation action = player.getUseItem().getUseAnimation();
         return action == ItemUseAnimation.EAT || action == ItemUseAnimation.DRINK;
      } else {
         return false;
      }
   }

   private void reset() {
      this.consuming = false;
      this.fullSpeed = false;
      this.sessionPlayer = null;
      this.lastTick = Integer.MIN_VALUE;
      this.lastUseTime = 0;
   }
}
