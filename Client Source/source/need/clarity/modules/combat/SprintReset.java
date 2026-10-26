package need.clarity.modules.combat;

import java.util.LinkedHashMap;
import java.util.Map;
import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.PerMovementEvents;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.combat.combohelpers.ComboHelper;
import need.clarity.modules.combat.combohelpers.ComboMode;
import need.clarity.modules.combat.combohelpers.STap;
import need.clarity.modules.combat.combohelpers.WTap;
import need.clarity.modules.movement.MoveFix;

public class SprintReset extends ModuleBase {
   private static final String W_TAP = "W-Tap";
   private static final String S_TAP = "S-Tap";
   private final Map<String, ComboMode> modes = new LinkedHashMap<>();

   public SprintReset() {
      super("Sprint Reset", "Combat");
      ComboHelper.instance = this;
      this.modes.put("W-Tap", new WTap());
      this.modes.put("S-Tap", new STap());
      PerAttackEvents.register(this::onAttack);
      PerMovementEvents.register(this::onMovement);
   }

   @Override
   public void onEnable() {
      this.modes.values().forEach(ComboMode::onEnable);
   }

   @Override
   public void onDisable() {
      this.modes.values().forEach(ComboMode::onDisable);
   }

   private void onAttack(EventStartAttack e) {
      if (this.isEnabled()) {
         ComboMode mode = this.getCurrentMode();
         if (mode != null) {
            mode.onAttack(e);
         }
      }
   }

   private void onMovement(MoveFix.EventMovementInput e) {
      if (this.isEnabled()) {
         ComboMode mode = this.getCurrentMode();
         if (mode != null) {
            mode.onMovement(e);
         }
      }
   }

   private ComboMode getCurrentMode() {
      String mode = this.getSetting("Mode", "W-Tap");
      return this.modes.get(mode);
   }

   @Override
   public <T> T getSetting(String key, T defaultValue) {
      return super.getSetting(key, defaultValue);
   }
}
