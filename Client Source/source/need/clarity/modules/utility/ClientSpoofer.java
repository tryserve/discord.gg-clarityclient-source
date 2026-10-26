package need.clarity.modules.utility;

import need.clarity.modules.ModuleBase;

public class ClientSpoofer extends ModuleBase {
   private static ClientSpoofer instance;

   public ClientSpoofer() {
      super("Client Spoofer", "Utility");
      instance = this;
   }

   public static ClientSpoofer getInstance() {
      return instance;
   }

   public String getBrand() {
      String mode = this.getSetting("Client", "Vanilla");

      return switch (mode) {
         case "Vanilla" -> "vanilla";
         case "Fabric" -> "fabric";
         case "Lunar Client" -> "lunarclient:v2.21.5-2540";
         case "Forge" -> "forge";
         case "NeoForge" -> "neoforge";
         case "Badlion" -> "badlion";
         case "Feather" -> "feather";
         case "LabyMod" -> "labymod";
         case "Sodium" -> "sodium";
         case "Quilt" -> "quilt";
         default -> "vanilla";
      };
   }
}
