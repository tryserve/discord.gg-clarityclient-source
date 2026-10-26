package need.clarity.modules.movement;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class Step extends ModuleBase {
   public static Step INSTANCE;
   private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("clarity", "step_height");
   private static final float VANILLA_BASE = 0.6F;

   public Step() {
      super("Step", "Movement");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.apply();
   }

   @Override
   public void onTick() {
      this.apply();
   }

   @Override
   public void onDisable() {
      this.remove();
   }

   private void apply() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
         if (attr != null) {
            float desired = this.getSetting("Step Height", 1.0F);
            desired = Math.max(0.6F, Math.min(3.0F, desired));
            attr.removeModifier(MODIFIER_ID);
            attr.addPermanentModifier(new AttributeModifier(MODIFIER_ID, desired - 0.6F, Operation.ADD_VALUE));
         }
      }
   }

   private void remove() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
         if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
         }
      }
   }
}
