package need.clarity.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DiscordIpc {
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private static final int OP_CLOSE = 2;
   private static final int OP_PING = 3;
   private static final int OP_PONG = 4;
   private static final long MIN_UPDATE_GAP_MS = 4500L;
   private volatile String clientId;
   private volatile JsonObject pending;
   private volatile boolean pendingSet;
   private volatile boolean running;
   private volatile DiscordIpc.State state = DiscordIpc.State.OFF;
   private volatile String lastError = "";
   private volatile String user = "";
   private Thread thread;
   private final Object lock = new Object();

   public DiscordIpc.State state() {
      return this.state;
   }

   public String lastError() {
      return this.lastError;
   }

   public String user() {
      return this.user;
   }

   public synchronized void start(String clientId) {
      this.clientId = clientId;
      if (!this.running) {
         this.running = true;
         this.thread = new Thread(this::loop, "Clarity Discord RPC");
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   public synchronized void stop() {
      this.running = false;
      synchronized (this.lock) {
         this.lock.notifyAll();
      }

      Thread t = this.thread;
      this.thread = null;
      if (t != null) {
         t.interrupt();
      }

      this.state = DiscordIpc.State.OFF;
   }

   public void setActivity(JsonObject activity) {
      this.pending = activity;
      this.pendingSet = true;
      synchronized (this.lock) {
         this.lock.notifyAll();
      }
   }

   private void loop() {
      for (long backoff = 2000L; this.running; backoff = Math.min(60000L, backoff * 2L)) {
         this.state = DiscordIpc.State.CONNECTING;

         try (DiscordIpc.Pipe pipe = DiscordIpc.Pipe.open()) {
            if (pipe == null) {
               this.state = DiscordIpc.State.NO_DISCORD;
               this.lastError = "Discord isn't running (or can't be reached)";
            } else {
               this.handshake(pipe);
               this.state = DiscordIpc.State.CONNECTED;
               this.lastError = "";
               backoff = 2000L;
               if (this.pending != null) {
                  this.pendingSet = true;
               }

               long lastSent = 0L;

               while (this.running) {
                  if (this.pendingSet) {
                     long wait = 4500L - (System.currentTimeMillis() - lastSent);
                     if (wait > 0L) {
                        sleep(wait);
                        continue;
                     }

                     this.pendingSet = false;
                     this.send(pipe, this.pending);
                     lastSent = System.currentTimeMillis();
                  }

                  synchronized (this.lock) {
                     if (this.running && !this.pendingSet) {
                        this.lock.wait(15000L);
                     }
                  }
               }

               try {
                  this.send(pipe, null);
               } catch (Throwable var10) {
               }
            }
         } catch (InterruptedException e) {
            break;
         } catch (Throwable t) {
            this.state = DiscordIpc.State.ERROR;
            this.lastError = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
         }

         if (!this.running) {
            break;
         }

         sleep(backoff);
      }

      this.state = DiscordIpc.State.OFF;
   }

   private void handshake(DiscordIpc.Pipe pipe) throws IOException {
      JsonObject hs = new JsonObject();
      hs.addProperty("v", 1);
      hs.addProperty("client_id", this.clientId);
      pipe.write(0, hs.toString());
      JsonObject ready = this.readReply(pipe, null);
      if (ready == null) {
         throw new IOException("Discord closed the connection");
      }

      if (ready.has("code") && ready.has("message") && !ready.has("cmd")) {
         throw new IOException("Discord: " + ready.get("message").getAsString());
      }

      try {
         JsonObject u = ready.getAsJsonObject("data").getAsJsonObject("user");
         this.user = u.has("global_name") && !u.get("global_name").isJsonNull() ? u.get("global_name").getAsString() : u.get("username").getAsString();
      } catch (Throwable var5) {
      }
   }

   private void send(DiscordIpc.Pipe pipe, JsonObject activity) throws IOException {
      String nonce = UUID.randomUUID().toString();
      JsonObject args = new JsonObject();
      args.addProperty("pid", ProcessHandle.current().pid());
      if (activity != null) {
         args.add("activity", activity);
      }

      JsonObject cmd = new JsonObject();
      cmd.addProperty("cmd", "SET_ACTIVITY");
      cmd.add("args", args);
      cmd.addProperty("nonce", nonce);
      pipe.write(1, cmd.toString());
      JsonObject reply = this.readReply(pipe, nonce);
      if (reply != null && "ERROR".equals(str(reply, "evt"))) {
         try {
            this.lastError = reply.getAsJsonObject("data").get("message").getAsString();
         } catch (Throwable var8) {
         }
      }
   }

   private JsonObject readReply(DiscordIpc.Pipe pipe, String nonce) throws IOException {
      for (int i = 0; i < 32; i++) {
         int[] op = new int[1];
         String json = pipe.read(op);
         if (op[0] == 3) {
            pipe.write(4, json);
         } else {
            if (op[0] == 2) {
               JsonObject o = parse(json);
               throw new IOException("Discord closed the connection" + (o != null && o.has("message") ? ": " + o.get("message").getAsString() : ""));
            }

            JsonObject o = parse(json);
            if (o != null && (nonce == null || nonce.equals(str(o, "nonce")))) {
               return o;
            }
         }
      }

      return null;
   }

   private static JsonObject parse(String s) {
      try {
         return JsonParser.parseString(s).getAsJsonObject();
      } catch (Throwable t) {
         return null;
      }
   }

   private static String str(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
   }

   private static void sleep(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   private abstract static class Pipe implements Closeable {
      abstract void writeBytes(byte[] var1) throws IOException;

      abstract void readFully(byte[] var1) throws IOException;

      void write(int op, String json) throws IOException {
         byte[] body = json.getBytes(StandardCharsets.UTF_8);
         ByteBuffer bb = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
         bb.putInt(op).putInt(body.length).put(body);
         this.writeBytes(bb.array());
      }

      String read(int[] opOut) throws IOException {
         byte[] head = new byte[8];
         this.readFully(head);
         ByteBuffer bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
         opOut[0] = bb.getInt();
         int len = bb.getInt();
         if (len >= 0 && len <= 1048576) {
            byte[] body = new byte[len];
            this.readFully(body);
            return new String(body, StandardCharsets.UTF_8);
         } else {
            throw new IOException("Bad frame from Discord");
         }
      }

      static DiscordIpc.Pipe open() {
         boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

         for (int i = 0; i < 10; i++) {
            try {
               if (windows) {
                  RandomAccessFile f = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                  return new DiscordIpc.WinPipe(f);
               }

               for (Path dir : unixDirs()) {
                  Path p = dir.resolve("discord-ipc-" + i);
                  if (Files.exists(p)) {
                     try {
                        SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                        ch.connect(UnixDomainSocketAddress.of(p));
                        return new DiscordIpc.UnixPipe(ch);
                     } catch (IOException var6) {
                     }
                  }
               }
            } catch (IOException var7) {
            }
         }

         return null;
      }

      private static List<Path> unixDirs() {
         List<Path> bases = new ArrayList<>();

         for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
            String v = System.getenv(env);
            if (v != null && !v.isEmpty()) {
               bases.add(Path.of(v));
            }
         }

         bases.add(Path.of("/tmp"));
         List<Path> out = new ArrayList<>();

         for (Path b : bases) {
            out.add(b);
            out.add(b.resolve("app/com.discordapp.Discord"));
            out.add(b.resolve("app/com.discordapp.DiscordCanary"));
            out.add(b.resolve("app/dev.vencord.Vesktop"));
            out.add(b.resolve(".flatpak/dev.vencord.Vesktop/xdg-run"));
            out.add(b.resolve("snap.discord"));
            out.add(b.resolve("snap.discord-canary"));
         }

         return out;
      }
   }

   public enum State {
      OFF,
      CONNECTING,
      CONNECTED,
      NO_DISCORD,
      ERROR;
   }

   private static final class UnixPipe extends DiscordIpc.Pipe {
      private final SocketChannel ch;

      UnixPipe(SocketChannel ch) {
         this.ch = ch;
      }

      @Override
      void writeBytes(byte[] b) throws IOException {
         ByteBuffer bb = ByteBuffer.wrap(b);

         while (bb.hasRemaining()) {
            this.ch.write(bb);
         }
      }

      @Override
      void readFully(byte[] b) throws IOException {
         ByteBuffer bb = ByteBuffer.wrap(b);

         while (bb.hasRemaining()) {
            if (this.ch.read(bb) < 0) {
               throw new IOException("Discord closed the connection");
            }
         }
      }

      @Override
      public void close() throws IOException {
         this.ch.close();
      }
   }

   private static final class WinPipe extends DiscordIpc.Pipe {
      private final RandomAccessFile f;

      WinPipe(RandomAccessFile f) {
         this.f = f;
      }

      @Override
      void writeBytes(byte[] b) throws IOException {
         this.f.write(b);
      }

      @Override
      void readFully(byte[] b) throws IOException {
         this.f.readFully(b);
      }

      @Override
      public void close() throws IOException {
         this.f.close();
      }
   }
}
