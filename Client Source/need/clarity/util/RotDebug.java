package need.clarity.util;

import need.clarity.config.ClarityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class RotDebug {
   public static final float LIMIT = 80.0F;
   private static long last = 0L;

   private RotDebug() {
   }

   public static boolean enabled() {
      try {
         return !(ClarityConfig.getSetting("Kill Aura", "Debug Pitch") instanceof Boolean bo && !bo);
      } catch (Throwable t) {
         return true;
      }
   }

   public static String callers(int skip, int count) {
      StackTraceElement[] st = new Throwable().getStackTrace();
      StringBuilder sb = new StringBuilder();

      for (int i = skip; i < st.length && i < skip + count; i++) {
         String cn = st[i].getClassName();
         cn = cn.substring(cn.lastIndexOf(46) + 1);
         if (sb.length() > 0) {
            sb.append(" <- ");
         }

         sb.append(cn).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
      }

      return sb.toString();
   }

   public static void report(String msg) {
      try {
         long now = System.currentTimeMillis();
         if (now - last < 1000L) {
            return;
         }

         last = now;
         String line = "[Clarity rot] " + msg;
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(line));
         }
      } catch (Throwable var5) {
      }
   }
}
