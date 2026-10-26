package need.clarity.modules.combat;

import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class Reach extends ModuleBase {
   public Reach() {
      super("Reach", "Combat");
   }

   public static float getBlockReach() {
      Reach m = (Reach)ModuleRegistry.get("Reach");
      return m != null && m.isEnabled() ? m.getSetting("Block Reach", 4.5F) : -1.0F;
   }

   public static float getEntityReach() {
      Reach m = (Reach)ModuleRegistry.get("Reach");
      return m != null && m.isEnabled() ? m.getSetting("Entity Reach", 3.0F) : -1.0F;
   }

   public static double vanillaBlockReach(Player p) {
      try {
         return p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      } catch (Throwable t) {
         return 4.5;
      }
   }

   public static double vanillaEntityReach(Player p) {
      try {
         return p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
      } catch (Throwable t) {
         return 3.0;
      }
   }
}
