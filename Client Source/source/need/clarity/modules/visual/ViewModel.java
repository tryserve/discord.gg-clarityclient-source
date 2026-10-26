package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.world.InteractionHand;

public class ViewModel extends ModuleBase {
   public static volatile ViewModel INSTANCE;

   public ViewModel() {
      super("ViewModel", "Visual");
      INSTANCE = this;
   }

   public boolean isMirror() {
      return this.getSetting("Mirror", false);
   }

   public float getPosX(InteractionHand hand) {
      if (this.isMirror()) {
         float v = this.getSetting("PosX", 0.0F);
         return hand == InteractionHand.MAIN_HAND ? v : v * -1.0F;
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main PosX", 0.0F) : this.getSetting("Off PosX", 0.0F);
      }
   }

   public float getPosY(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("PosY", 0.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main PosY", 0.0F) : this.getSetting("Off PosY", 0.0F);
      }
   }

   public float getPosZ(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("PosZ", 0.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main PosZ", 0.0F) : this.getSetting("Off PosZ", 0.0F);
      }
   }

   public float getRotX(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("RotX", 0.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main RotX", 0.0F) : this.getSetting("Off RotX", 0.0F);
      }
   }

   public float getRotY(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("RotY", 0.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main RotY", 0.0F) : this.getSetting("Off RotY", 0.0F);
      }
   }

   public float getRotZ(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("RotZ", 0.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main RotZ", 0.0F) : this.getSetting("Off RotZ", 0.0F);
      }
   }

   public float getScale(InteractionHand hand) {
      if (this.isMirror()) {
         return this.getSetting("Scale", 1.0F);
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.getSetting("Main Scale", 1.0F) : this.getSetting("Off Scale", 1.0F);
      }
   }

   public static boolean isActive() {
      ViewModel inst = INSTANCE;
      return inst != null && inst.isEnabled();
   }
}
