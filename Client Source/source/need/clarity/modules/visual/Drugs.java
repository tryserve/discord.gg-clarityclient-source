package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;

public class Drugs extends ModuleBase {
   public static Drugs INSTANCE;
   public static final String[] STYLES = new String[]{"Shrooms", "Acid", "Weed", "Drunk", "DMT", "Ketamine", "Caffeine"};
   private static final long START = System.nanoTime();
   private long enabledAt = System.nanoTime();

   public Drugs() {
      super("Drugs", "Visual");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.enabledAt = System.nanoTime();
   }

   public static boolean active() {
      Drugs d = INSTANCE;
      return d != null && d.isEnabled();
   }

   public static int style() {
      String s = INSTANCE == null ? STYLES[0] : INSTANCE.getSetting("Drug", STYLES[0]);

      for (int i = 0; i < STYLES.length; i++) {
         if (STYLES[i].equals(s)) {
            return i;
         }
      }

      return 0;
   }

   public static float amount() {
      Drugs d = INSTANCE;
      if (d == null) {
         return 0.0F;
      }

      float a = Math.max(0.0F, Math.min(100.0F, d.getSetting("Intensity", 70.0F))) / 100.0F;
      float comeUp = Math.max(0.0F, d.getSetting("Come Up", 3.0F));
      if (comeUp > 0.0F) {
         float t = (float)(System.nanoTime() - d.enabledAt) / 1.0E9F / comeUp;
         if (t < 1.0F) {
            a *= t * t * (3.0F - 2.0F * t);
         }
      }

      return a;
   }

   public static float time() {
      float speed = INSTANCE == null ? 1.0F : Math.max(0.0F, INSTANCE.getSetting("Speed", 1.0F));
      return (float)((System.nanoTime() - START) / 1.0E9 * speed % 3600.0);
   }
}
