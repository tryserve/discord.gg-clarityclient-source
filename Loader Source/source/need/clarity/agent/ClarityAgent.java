package need.clarity.agent;

import java.lang.instrument.Instrumentation;
import need.clarity.agent.net.ClarityConnect;

public class ClarityAgent {
   public static void premain(String agentArgs, Instrumentation inst) throws Exception {
      agentmain(agentArgs, inst);
   }

   public static void agentmain(String agentArgs, Instrumentation inst) {
      String[] parts = agentArgs == null ? new String[0] : agentArgs.split("\t", -1);
      String wsUrl = parts.length > 0 ? parts[0].trim() : "";
      String licenceKey = parts.length > 1 ? parts[1].trim() : "";
      String hwid = parts.length > 2 ? parts[2].trim() : "";
      String uuid = parts.length > 3 ? parts[3].trim() : "";
      String build = parts.length > 4 ? parts[4].trim() : "unknown";
      String statusFile = parts.length > 5 ? parts[5].trim() : "";
      AgentStatus.init(statusFile);

      try {
         if (AgentStatus.alreadyLoaded()) {
            AgentStatus.report(100, "Clarity is already injected into this instance");
            AgentStatus.done();
            return;
         }

         if (wsUrl.isEmpty() || licenceKey.isEmpty()) {
            AgentStatus.fail("Missing server URL or licence key");
            return;
         }

         if ("96de94e8da32afbc8d879671f1444edd9c3c98308739f901512bfdae94ba87da".startsWith("REPLACE") || "174ed3cf53647a9c7559824aaf7959f8a0a7b87ee41db0ec5510b928df790537".startsWith("REPLACE")) {
            AgentStatus.fail("Server public keys not configured — rebuild with real key values in ServerKey.java");
            return;
         }

         AgentStatus.report(1, "Locating the game class loader");
         ClassLoader gameLoader = findGameClassLoader();
         if (gameLoader == null) {
            AgentStatus.fail("Could not find Minecraft's class loader");
            return;
         }

         long pid = ProcessHandle.current().pid();
         ClarityConnect.ConnectResult result = ClarityConnect.connect(wsUrl, licenceKey, hwid, uuid, build, pid, gameLoader, inst, (pct, msg) -> AgentStatus.report(pct, msg));
         if (result.result == ClarityConnect.Result.OK) {
            AgentStatus.done();
         } else if (result.result == ClarityConnect.Result.DENIED) {
            AgentStatus.fail("Licence denied — check your key, device limit, or contact support");
         } else {
            AgentStatus.fail(result.error != null ? result.error : "Unknown error");
         }
      } catch (Throwable e) {
         AgentStatus.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
         e.printStackTrace();
      }
   }

   private static ClassLoader findGameClassLoader() {
      ThreadGroup root = Thread.currentThread().getThreadGroup();

      while (root.getParent() != null) {
         root = root.getParent();
      }

      Thread[] threads = new Thread[root.activeCount() + 128];
      int n = root.enumerate(threads, true);

      for (int i = 0; i < n; i++) {
         if (threads[i] != null) {
            ClassLoader cl = threads[i].getContextClassLoader();
            if (cl != null && canLoadMinecraft(cl)) {
               return cl;
            }
         }
      }

      ClassLoader sysCl = ClassLoader.getSystemClassLoader();
      if (canLoadMinecraft(sysCl)) {
         return sysCl;
      }

      for (ClassLoader cl = ClarityAgent.class.getClassLoader(); cl != null; cl = cl.getParent()) {
         if (canLoadMinecraft(cl)) {
            return cl;
         }
      }

      return null;
   }

   private static boolean canLoadMinecraft(ClassLoader cl) {
      try {
         Class.forName("net.minecraft.client.Minecraft", false, cl);
         return true;
      } catch (ClassNotFoundException | LinkageError e) {
         return false;
      }
   }
}
