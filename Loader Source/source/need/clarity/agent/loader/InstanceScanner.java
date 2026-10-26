package need.clarity.agent.loader;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstanceScanner {
   private static final String[] MC_KEYS = new String[]{"minecraft", "net.minecraft", "fabricmc", "fabriclauncher", "launchwrapper", "knotclassloader", "quilt"};
   private final Map<String, Instance> known = new HashMap<>();
   private final Set<String> notMinecraft = new HashSet<>();

   List<Instance> scan() {
      List<VirtualMachineDescriptor> vms;
      try {
         vms = VirtualMachine.list();
      } catch (Throwable t) {
         return new ArrayList<>(this.known.values());
      }

      String own = String.valueOf(ProcessHandle.current().pid());
      Set<String> alive = new HashSet<>();

      for (VirtualMachineDescriptor d : vms) {
         String pid = d.id();
         if (!pid.equals(own)) {
            alive.add(pid);
            if (!this.known.containsKey(pid) && !this.notMinecraft.contains(pid)) {
               boolean quick = matches(d.displayName());
               Instance inst = this.inspect(pid, quick);
               if (inst == null) {
                  this.notMinecraft.add(pid);
               } else {
                  this.known.put(pid, inst);
               }
            }
         }
      }

      this.known.keySet().retainAll(alive);

      for (Instance i : this.known.values()) {
         if (!i.ready) {
            i.ready = windowReady(i);
         }
      }

      this.notMinecraft.retainAll(alive);
      List<Instance> out = new ArrayList<>(this.known.values());
      out.sort((a, b) -> Long.compare(parse(a.pid), parse(b.pid)));
      return out;
   }

   static boolean windowReady(Instance inst) {
      if (inst.injected) {
         return true;
      }

      long now = System.currentTimeMillis();
      long startMs = -1L;

      try {
         long pid = Long.parseLong(inst.pid);
         startMs = ProcessHandle.of(pid).flatMap(h -> h.info().startInstant()).map(Instant::toEpochMilli).orElse(-1L);
      } catch (Exception var10) {
      }

      long upMs = now - (startMs >= 0L ? startMs : inst.seenAt);
      Path log = inst.gameDir == null ? null : inst.gameDir.resolve("logs").resolve("latest.log");
      boolean usable = false;

      try {
         if (log != null && Files.isRegularFile(log)) {
            usable = startMs < 0L || Files.getLastModifiedTime(log).toMillis() >= startMs - 3000L;
         }
      } catch (IOException var11) {
      }

      return !usable ? upMs > 60000L : hasMarker(log) || upMs > 180000L;
   }

   private static boolean hasMarker(Path log) {
      try (InputStream in = Files.newInputStream(log)) {
         String s = new String(in.readNBytes(400000), StandardCharsets.UTF_8);
         return s.contains("Reloading ResourceManager") || s.contains("OpenAL initialized") || s.contains("Sound engine started") || s.contains("Created: ");
      } catch (Exception e) {
         return false;
      }
   }

   static boolean isInjected(String pid) {
      try {
         VirtualMachine vm = VirtualMachine.attach(pid);

         try {
            return "true".equals(vm.getSystemProperties().getProperty("clarity.agent.loaded"));
         } finally {
            vm.detach();
         }
      } catch (Throwable t) {
         return false;
      }
   }

   private Instance inspect(String pid, boolean looksLikeMinecraft) {
      try {
         VirtualMachine vm = VirtualMachine.attach(pid);

         try {
            Properties p = vm.getSystemProperties();
            String cmd = p.getProperty("sun.java.command", "");
            String cp = p.getProperty("java.class.path", "");
            String cwd = p.getProperty("user.dir", "");
            if (!looksLikeMinecraft && !matches(cmd) && !matches(cp)) {
               return null;
            }

            String version = arg(cmd, "--version");
            if (version == null) {
               version = arg(cmd, "--fml.mcVersion");
            }

            if (version == null) {
               version = "";
            }

            Matcher m = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)$").matcher(version);
            if (m.find()) {
               version = m.group(1);
            }

            String lowAll = (cmd + " " + cp).toLowerCase(Locale.ROOT);
            String loader = lowAll.contains("quilt") ? "Quilt" : (lowAll.contains("neoforge") ? "NeoForge" : (lowAll.contains("fabric") ? "Fabric" : (lowAll.contains("forge") ? "Forge" : "")));
            String dirArg = arg(cmd, "--gameDir");
            Path gameDir = null;

            try {
               gameDir = Paths.get(dirArg != null ? dirArg : cwd).toAbsolutePath().normalize();
            } catch (Exception var21) {
            }

            String uuid = readPlayerUuid(gameDir);
            if (uuid.isEmpty()) {
               String cu = arg(cmd, "--uuid");
               if (cu != null && cu.replace("-", "").matches("[0-9a-fA-F]{32}")) {
                  uuid = cu.replace("-", "").toLowerCase(Locale.ROOT);
               }
            }

            boolean injected = "true".equals(p.getProperty("clarity.agent.loaded"));
            return new Instance(pid, friendlyName(gameDir), version, loader, uuid, gameDir, injected);
         } finally {
            vm.detach();
         }
      } catch (Throwable t) {
         return looksLikeMinecraft ? new Instance(pid, "Minecraft", "", "", "", null, false) : null;
      }
   }

   private static String readPlayerUuid(Path gameDir) {
      if (gameDir == null) {
         return "";
      }

      Path profilesFile = gameDir.resolve("launcher_profiles.json");
      if (!Files.exists(profilesFile)) {
         profilesFile = gameDir.getParent() != null ? gameDir.getParent().resolve(".minecraft").resolve("launcher_profiles.json") : null;
         if (profilesFile == null || !Files.exists(profilesFile)) {
            return "";
         }
      }

      try {
         String content = Files.readString(profilesFile);
         Matcher m = Pattern.compile("\"selectedUser\"\\s*:\\s*\\{[^}]*\"profile\"\\s*:\\s*\"([0-9a-f\\-]{32,36})\"").matcher(content);
         if (m.find()) {
            return m.group(1).replace("-", "");
         }

         Matcher m2 = Pattern.compile("\"(?:clientToken|uuid)\"\\s*:\\s*\"([0-9a-f\\-]{32,36})\"").matcher(content);
         if (m2.find()) {
            return m2.group(1).replace("-", "");
         }
      } catch (IOException var5) {
      }

      return "";
   }

   static String friendlyName(Path dir) {
      if (dir == null) {
         return "Minecraft";
      }

      List<String> parts = new ArrayList<>();

      for (Path p : dir) {
         parts.add(p.toString());
      }

      for (int i = parts.size() - 2; i >= 0; i--) {
         if (parts.get(i).equalsIgnoreCase("instances")) {
            return parts.get(i + 1);
         }
      }

      String last = parts.isEmpty() ? "" : parts.get(parts.size() - 1);
      if (!last.equalsIgnoreCase(".minecraft") && !last.equalsIgnoreCase("minecraft") && !last.isEmpty()) {
         return last;
      }

      String parent = parts.size() >= 2 ? parts.get(parts.size() - 2) : "";
      return !parent.isEmpty() && !last.equalsIgnoreCase(".minecraft") && !parent.equalsIgnoreCase("AppData") && !parent.equalsIgnoreCase("Roaming") ? parent : "Minecraft";
   }

   private static String arg(String cmd, String key) {
      String[] t = cmd.split("\\s+");

      for (int i = 0; i < t.length - 1; i++) {
         if (t[i].equals(key)) {
            String v = t[i + 1];
            return v.startsWith("\"") ? v.replace("\"", "") : v;
         }
      }

      return null;
   }

   static boolean matches(String s) {
      if (s == null) {
         return false;
      }

      String lo = s.toLowerCase(Locale.ROOT);

      for (String k : MC_KEYS) {
         if (lo.contains(k)) {
            return true;
         }
      }

      return false;
   }

   private static long parse(String s) {
      try {
         return Long.parseLong(s);
      } catch (Exception e) {
         return 0L;
      }
   }
}
