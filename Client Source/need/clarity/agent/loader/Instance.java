package need.clarity.agent.loader;

import java.nio.file.Path;

public final class Instance {
   public final String pid;
   public final String name;
   public final String version;
   public final String loader;
   public final String uuid;
   public final Path gameDir;
   public volatile boolean injected;
   public volatile boolean ready;
   public final long seenAt = System.currentTimeMillis();

   public Instance(String pid, String name, String version, String loader, String uuid, Path gameDir, boolean injected) {
      this.pid = pid;
      this.name = name;
      this.version = version;
      this.loader = loader;
      this.uuid = uuid;
      this.gameDir = gameDir;
      this.injected = injected;
      this.ready = injected;
   }

   public String subtitle() {
      StringBuilder b = new StringBuilder();
      if (!this.version.isEmpty()) {
         b.append(this.version);
      }

      if (!this.loader.isEmpty()) {
         b.append(b.length() > 0 ? " · " : "").append(this.loader);
      }

      b.append(b.length() > 0 ? " · " : "").append("PID ").append(this.pid);
      return b.toString();
   }
}
