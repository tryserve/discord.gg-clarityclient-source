package need.clarity.modules.client;

import need.clarity.Unload;
import need.clarity.modules.ModuleBase;

public class SelfDestruct extends ModuleBase {
   public SelfDestruct() {
      super("Self Destruct", "Client");
      if (this.isEnabled()) {
         this.setEnabled(false);
      }
   }

   @Override
   public void onEnable() {
      Unload.request(this);
   }
}
