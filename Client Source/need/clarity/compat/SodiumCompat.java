package need.clarity.compat;

public final class SodiumCompat {
   private static final boolean SODIUM_PRESENT = detect();

   private SodiumCompat() {
   }

   private static boolean detect() {
      String[] candidates = new String[]{"net.caffeinemc.mods.sodium.client.SodiumClientMod", "me.jellysquid.mods.sodium.client.SodiumClientMod"};

      for (String cls : candidates) {
         try {
            Class.forName(cls);
            return true;
         } catch (ClassNotFoundException var6) {
         }
      }

      return false;
   }

   public static boolean isSodiumPresent() {
      return SODIUM_PRESENT;
   }
}
