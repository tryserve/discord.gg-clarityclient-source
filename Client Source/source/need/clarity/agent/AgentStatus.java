package need.clarity.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class AgentStatus {
   public static final String LOADED_PROPERTY = "clarity.agent.loaded";
   private static Path file;

   private AgentStatus() {
   }

   public static void init(String path) {
      file = null;
      if (path != null && !path.isBlank()) {
         try {
            file = Paths.get(path.trim());
         } catch (Exception var2) {
         }
      }
   }

   public static boolean alreadyLoaded() {
      return "true".equals(System.getProperty("clarity.agent.loaded"));
   }

   public static void markLoaded() {
      System.setProperty("clarity.agent.loaded", "true");
   }

   public static void report(int pct, String message) {
      write(pct + "|" + message);
   }

   public static void done() {
      write("100|DONE");
   }

   public static void fail(String reason) {
      write("-1|" + reason);
   }

   private static void write(String line) {
      Path f = file;
      if (f != null) {
         try {
            Files.writeString(f, line.replace('\n', ' ') + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (IOException var3) {
         }
      }
   }
}
