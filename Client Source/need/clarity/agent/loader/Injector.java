package need.clarity.agent.loader;

import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class Injector {
   String error = "";

   public Injector.Result run(Instance inst, String wsUrl, String licKey, BiConsumer<Integer, String> status) {
      Path statusFile = null;

      try {
         status.accept(3, "Preparing licence credentials");
         String jar = selfJar();
         if (jar.isEmpty()) {
            return this.fail("Run the loader from the built jar (clarity-agent-*.jar)");
         }

         String hwid = computeHwid();
         String uuid = inst.uuid != null ? inst.uuid.replace("-", "") : "";
         if (uuid.isEmpty()) {
            uuid = "00000000000000000000000000000000";
         }

         status.accept(10, "Attaching to " + inst.name + " (PID " + inst.pid + ")");

         VirtualMachine vm;
         try {
            vm = VirtualMachine.attach(inst.pid);
         } catch (Throwable t) {
            return this.fail("Could not attach: " + t.getMessage() + " (is Minecraft still running, under the same user?)");
         }

         try {
            status.accept(22, "Checking for an existing injection");
            if ("true".equals(vm.getSystemProperties().getProperty("clarity.agent.loaded"))) {
               status.accept(100, "Already injected");
               return Injector.Result.ALREADY;
            }

            Path statusDir = inst.gameDir != null && Files.isDirectory(inst.gameDir) && Files.isWritable(inst.gameDir) ? inst.gameDir : null;
            statusFile = statusDir != null ? Files.createTempFile(statusDir, ".clr-", ".tmp") : Files.createTempFile("clarity-inject-", ".status");
            Path sf = statusFile;
            String build = "loader-1";
            String args = wsUrl + "\t" + licKey + "\t" + hwid + "\t" + uuid + "\t" + build + "\t" + sf;
            status.accept(30, "Loading the agent");
            Throwable[] loadErr = new Throwable[]{null};
            boolean[] finished = new boolean[]{false};
            VirtualMachine fvm = vm;
            String fArgs = args;
            Thread t = new Thread(() -> {
               try {
                  fvm.loadAgent(jar, fArgs);
               } catch (Throwable e) {
                  loadErr[0] = e;
               }

               finished[0] = true;
            }, "clarity-load-agent");
            t.setDaemon(true);
            t.start();
            int shown = 30;
            int lines = 0;
            long start = System.currentTimeMillis();
            boolean agentDone = false;
            String agentFail = null;

            while (!finished[0] || lines < countLines(sf)) {
               for (List<String> all = readLines(sf); lines < all.size(); lines++) {
                  String l = all.get(lines);
                  int bar = l.indexOf(124);
                  if (bar >= 0) {
                     int pct;
                     try {
                        pct = Integer.parseInt(l.substring(0, bar).trim());
                     } catch (Exception e) {
                        continue;
                     }

                     String msg = l.substring(bar + 1);
                     if (pct < 0) {
                        agentFail = msg;
                     } else if (msg.equals("DONE")) {
                        agentDone = true;
                     } else {
                        shown = Math.max(shown, 30 + (int)(pct * 0.69));
                        status.accept(shown, msg);
                     }
                  }
               }

               if (System.currentTimeMillis() - start > 120000L) {
                  return this.fail("Timed out waiting for the server to deliver Clarity");
               }

               sleep(60L);
            }

            if (loadErr[0] != null) {
               return this.fail("Agent failed to load: " + loadErr[0]);
            }

            if (agentFail != null) {
               return this.fail(agentFail);
            }

            if (!agentDone) {
               return this.fail(
                  lines == 0
                     ? "The agent loaded but never reported back (sandboxed launcher?). Check the admin panel's Events for the real result."
                     : "The agent stopped before finishing. Check the admin panel's Events for the reason."
               );
            }

            status.accept(100, "Injected");
            return Injector.Result.OK;
         } finally {
            try {
               vm.detach();
            } catch (Throwable var70) {
            }
         }
      } catch (Throwable t) {
         return this.fail(String.valueOf(t));
      } finally {
         if (statusFile != null) {
            try {
               Files.deleteIfExists(statusFile);
            } catch (Exception var69) {
            }
         }
      }
   }

   private static String computeHwid() {
      List<String> macs = new ArrayList<>();

      try {
         for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            byte[] hw = ni.getHardwareAddress();
            if (hw != null && hw.length != 0) {
               StringBuilder sb = new StringBuilder();

               for (byte b : hw) {
                  sb.append(String.format("%02x", b & 255));
               }

               if (!sb.isEmpty()) {
                  macs.add(sb.toString());
               }
            }
         }
      } catch (Exception var10) {
      }

      Collections.sort(macs);

      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");

         for (String mac : macs) {
            md.update(mac.getBytes(StandardCharsets.UTF_8));
         }

         byte[] digest = md.digest();
         StringBuilder sb = new StringBuilder();

         for (byte b : digest) {
            sb.append(String.format("%02x", b & 255));
         }

         return sb.toString();
      } catch (Exception e) {
         return "unknown";
      }
   }

   private Injector.Result fail(String msg) {
      this.error = msg;
      return Injector.Result.FAILED;
   }

   private static int countLines(Path f) {
      return readLines(f).size();
   }

   private static List<String> readLines(Path f) {
      try {
         return Files.readAllLines(f, StandardCharsets.UTF_8);
      } catch (Exception e) {
         return List.of();
      }
   }

   private static void sleep(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException var3) {
      }
   }

   static String selfJar() {
      try {
         File f = new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI());
         if (f.isFile() && f.getName().endsWith(".jar")) {
            return f.getAbsolutePath();
         }
      } catch (Exception var1) {
      }

      return "";
   }

   public enum Result {
      OK,
      ALREADY,
      FAILED;
   }
}
