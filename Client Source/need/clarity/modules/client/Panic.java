package need.clarity.modules.client;

import need.clarity.clickgui.GuiData;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;

public class Panic extends ModuleBase {
   public static final String[] CATEGORIES = new String[]{"Combat", "Visual", "Utility", "Movement", "HUD"};
   private boolean releaseNextTick = false;

   public Panic() {
      super("Panic", "Client");
   }

   @Override
   public void onEnable() {
      for (ModuleBase m : ModuleRegistry.all()) {
         if (m != this && m.isEnabled() && !"Targets".equals(m.getName())) {
            String cat = categoryOf(m);
            if (cat != null && this.getSetting(cat, true)) {
               try {
                  m.setEnabled(false);
               } catch (Throwable var5) {
               }
            }
         }
      }

      this.releaseNextTick = true;
   }

   @Override
   public void onTick() {
      if (this.releaseNextTick) {
         this.releaseNextTick = false;
         this.setEnabled(false);
      }
   }

   private static String categoryOf(ModuleBase m) {
      try {
         GuiData.Module gm = GuiData.get().modules.get(m.getName());
         if (gm != null) {
            return match(gm.category);
         }
      } catch (Throwable var2) {
      }

      return match(m.getCategory());
   }

   private static String match(String cat) {
      if (cat == null) {
         return null;
      }

      for (String c : CATEGORIES) {
         if (c.equalsIgnoreCase(cat.trim())) {
            return c;
         }
      }

      return null;
   }
}
