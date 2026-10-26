package need.clarity.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import need.clarity.audio.BeatAnalyzer;
import need.clarity.config.ClarityConfig;
import need.clarity.friends.FriendsWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

public class ClarityServer {
   private static final int PORT = 7420;
   private static final Object LOCK = new Object();
   private static HttpServer server;
   private static ExecutorService pool;
   private static volatile boolean client = false;
   private static final String LOCKED_PAGE = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Clarity</title>\n<link rel=\"icon\" href=\"/icon.png\">\n<style>\n  html,body{height:100%;margin:0;background:#0b0e17;color:#ededf0;font:15px/1.6 system-ui,-apple-system,Segoe UI,sans-serif}\n  body{display:flex;align-items:center;justify-content:center}\n  .box{max-width:440px;padding:32px 36px;border:1px solid #2a3142;border-radius:16px;background:#161b26;text-align:center}\n  h1{font-size:19px;margin:0 0 10px}\n  p{color:#8b8b93;margin:0}\n  b{color:#ededf0}\n</style></head><body><div class=\"box\">\n  <h1>The web GUI is turned off</h1>\n  <p>Clarity is set to use the in-game <b>ClickGUI</b>, so only one menu edits your config at a time.<br><br>\n  To use this page, open the ClickGUI (Right Shift) &rarr; <b>GUI</b> &rarr; <b>Switch to the web GUI</b>.\n  This tab reloads by itself when you do.</p>\n</div><script>\n  setInterval(function(){fetch(\"/status\",{cache:\"no-store\"}).then(function(r){return r.json()})\n    .then(function(s){if(s&&s.gui===\"Web\")location.reload()}).catch(function(){})},1000);\n</script></body></html>\n";

   public static void init() {
      client = true;
      sync();
   }

   public static void sync() {
      if (client) {
         if (ClarityConfig.webGuiActive()) {
            start();
         } else {
            stopSoon();
         }
      }
   }

   public static void shutdown() {
      client = false;
      stop();
   }

   public static boolean running() {
      synchronized (LOCK) {
         return server != null;
      }
   }

   public static void start() {
      synchronized (LOCK) {
         if (server == null) {
            try {
               HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 7420), 0);
               s.createContext("/", ClarityServer::handleRequest);
               pool = Executors.newCachedThreadPool(r -> {
                  Thread t = new Thread(r, "clarity-web");
                  t.setDaemon(true);
                  return t;
               });
               s.setExecutor(pool);
               s.start();
               server = s;
            } catch (IOException e) {
               retryLater();
            }
         }
      }
   }

   private static void retryLater() {
      Thread t = new Thread(() -> {
         for (int i = 0; i < 10; i++) {
            try {
               Thread.sleep(300L);
            } catch (InterruptedException ignored) {
               return;
            }

            if (!ClarityConfig.webGuiActive() || running()) {
               return;
            }

            synchronized (LOCK) {
               if (server != null) {
                  return;
               }

               try {
                  HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 7420), 0);
                  s.createContext("/", ClarityServer::handleRequest);
                  pool = Executors.newCachedThreadPool(r -> {
                     Thread x = new Thread(r, "clarity-web");
                     x.setDaemon(true);
                     return x;
                  });
                  s.setExecutor(pool);
                  s.start();
                  server = s;
               } catch (IOException var5) {
                  continue;
               }

               return;
            }
         }
      }, "clarity-web-retry");
      t.setDaemon(true);
      t.start();
   }

   private static void stopSoon() {
      if (running()) {
         Thread t = new Thread(() -> {
            try {
               Thread.sleep(250L);
            } catch (InterruptedException var1) {
            }

            if (!ClarityConfig.webGuiActive()) {
               stop();
            }
         }, "clarity-web-stop");
         t.setDaemon(true);
         t.start();
      }
   }

   public static void stop() {
      HttpServer s;
      ExecutorService p;
      synchronized (LOCK) {
         s = server;
         p = pool;
         server = null;
         pool = null;
      }

      if (s != null) {
         try {
            s.stop(0);
         } catch (Throwable var4) {
         }
      }

      if (p != null) {
         p.shutdownNow();
      }
   }

   private static void handleRequest(HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
      exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, *");
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
         exchange.sendResponseHeaders(204, -1L);
         exchange.close();
      } else {
         String path = exchange.getRequestURI().getPath();
         String method = exchange.getRequestMethod().toUpperCase();
         if (path.equals("/status") && method.equals("GET")) {
            String gui = ClarityConfig.webGuiActive() ? "Web" : "ClickGUI";
            byte[] body = ("{\"gui\":\"" + gui + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
               out.write(body);
            }
         } else if ((path.equals("/") || path.equals("/index.html")) && method.equals("GET")) {
            exchange.getResponseHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
            exchange.getResponseHeaders().add("Pragma", "no-cache");
            if (!ClarityConfig.webGuiActive()) {
               send(
                  exchange,
                  200,
                  "text/html; charset=utf-8",
                  "<!doctype html><html><head><meta charset=\"utf-8\"><title>Clarity</title>\n<link rel=\"icon\" href=\"/icon.png\">\n<style>\n  html,body{height:100%;margin:0;background:#0b0e17;color:#ededf0;font:15px/1.6 system-ui,-apple-system,Segoe UI,sans-serif}\n  body{display:flex;align-items:center;justify-content:center}\n  .box{max-width:440px;padding:32px 36px;border:1px solid #2a3142;border-radius:16px;background:#161b26;text-align:center}\n  h1{font-size:19px;margin:0 0 10px}\n  p{color:#8b8b93;margin:0}\n  b{color:#ededf0}\n</style></head><body><div class=\"box\">\n  <h1>The web GUI is turned off</h1>\n  <p>Clarity is set to use the in-game <b>ClickGUI</b>, so only one menu edits your config at a time.<br><br>\n  To use this page, open the ClickGUI (Right Shift) &rarr; <b>GUI</b> &rarr; <b>Switch to the web GUI</b>.\n  This tab reloads by itself when you do.</p>\n</div><script>\n  setInterval(function(){fetch(\"/status\",{cache:\"no-store\"}).then(function(r){return r.json()})\n    .then(function(s){if(s&&s.gui===\"Web\")location.reload()}).catch(function(){})},1000);\n</script></body></html>\n"
               );
            } else {
               serveResource(exchange, "assets/clarity/Clarity.html", "text/html; charset=utf-8");
            }
         } else if ((path.equals("/favicon.ico") || path.equals("/icon.png")) && method.equals("GET")) {
            serveResource(exchange, "assets/clarity/icon.png", "image/png");
         } else if (path.startsWith("/capes/") && method.equals("GET")) {
            String filename = path.substring("/capes/".length());
            if (!filename.contains("..") && !filename.contains("/") && !filename.contains("\\")) {
               serveResource(exchange, "assets/clarity/capes/" + filename, "image/png");
            } else {
               send(exchange, 400, "text/plain", "Bad request");
            }
         } else if (path.startsWith("/skins/") && method.equals("GET")) {
            String filename = path.substring("/skins/".length());
            if (!filename.contains("..") && !filename.contains("/") && !filename.contains("\\")) {
               serveResource(exchange, "assets/clarity/skins/" + filename, "image/png");
            } else {
               send(exchange, 400, "text/plain", "Bad request");
            }
         } else if (path.startsWith("/backgrounds/") && method.equals("GET")) {
            String filename = path.substring("/backgrounds/".length());
            if (!filename.contains("..") && !filename.contains("/") && !filename.contains("\\")) {
               String ct = filename.endsWith(".png") ? "image/png" : "image/jpeg";
               serveResource(exchange, "assets/clarity/backgrounds/" + filename, ct);
            } else {
               send(exchange, 400, "text/plain", "Bad request");
            }
         } else if (path.equals("/currentserver") && method.equals("GET")) {
            String ip = "";

            try {
               ServerData sd = Minecraft.getInstance().getCurrentServer();
               if (sd != null && sd.ip != null && !sd.ip.isEmpty()) {
                  ip = sd.ip;
               } else {
                  ClientPacketListener conn = Minecraft.getInstance().getConnection();
                  if (conn != null && conn.getConnection() != null && conn.getConnection().getRemoteAddress() instanceof InetSocketAddress isa) {
                     ip = isa.getHostString() + ":" + isa.getPort();
                  }
               }
            } catch (Throwable var18) {
            }

            byte[] body = ("{\"ip\":\"" + esc(ip) + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
               out.write(body);
            }
         } else if (path.equals("/audiodevices") && method.equals("GET")) {
            StringBuilder sb = new StringBuilder("[");

            try {
               for (String n : BeatAnalyzer.deviceNames()) {
                  if (sb.length() > 1) {
                     sb.append(',');
                  }

                  sb.append('"').append(esc(n)).append('"');
               }
            } catch (Throwable var19) {
            }

            sb.append(']');
            byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
               out.write(body);
            }
         } else if (path.equals("/servers") && method.equals("GET")) {
            StringBuilder sb = new StringBuilder("[");

            try {
               ServerList list = new ServerList(Minecraft.getInstance());
               list.load();

               for (int i = 0; i < list.size(); i++) {
                  ServerData d = list.get(i);
                  if (d != null) {
                     if (sb.length() > 1) {
                        sb.append(',');
                     }

                     sb.append("{\"name\":\"").append(esc(d.name)).append("\",\"ip\":\"").append(esc(d.ip)).append("\"}");
                  }
               }
            } catch (Throwable var20) {
            }

            sb.append(']');
            byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
               out.write(body);
            }
         } else if (!path.equals("/friends") || !method.equals("GET") && !method.equals("POST")) {
            if (path.equals("/config") && method.equals("GET")) {
               byte[] body = ClarityConfig.getCurrentJson().getBytes(StandardCharsets.UTF_8);
               exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
               exchange.getResponseHeaders().add("Cache-Control", "no-store");
               exchange.sendResponseHeaders(200, body.length);

               try (OutputStream out = exchange.getResponseBody()) {
                  out.write(body);
               }
            } else if (path.equals("/config") && method.equals("POST")) {
               String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
               if (!ClarityConfig.webGuiActive()) {
                  send(exchange, 423, "text/plain", "Web GUI is locked: GUI is set to ClickGUI");
               } else {
                  ClarityConfig.update(body);
                  send(exchange, 204, "text/plain", "");
               }
            } else {
               send404(exchange);
            }
         } else if (!ClarityConfig.webGuiActive()) {
            send(exchange, 423, "text/plain", "Web GUI is locked: GUI is set to ClickGUI");
         } else {
            String out;
            if (method.equals("POST")) {
               String origin = exchange.getRequestHeaders().getFirst("Origin");
               if (origin != null && !origin.equals("http://127.0.0.1:7420") && !origin.equals("http://localhost:7420")) {
                  send(exchange, 403, "text/plain", "Forbidden");
                  return;
               }

               String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
               out = FriendsWeb.apply(body);
            } else {
               out = FriendsWeb.model();
            }

            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            send(exchange, 200, "application/json; charset=utf-8", out);
         }
      }
   }

   private static String esc(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private static void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
      try (InputStream in = ClarityServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
         if (in == null) {
            send404(exchange);
            return;
         }

         byte[] data = in.readAllBytes();
         exchange.getResponseHeaders().add("Content-Type", contentType);
         exchange.getResponseHeaders().add("Cache-Control", "no-cache");
         exchange.sendResponseHeaders(200, data.length);

         try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
         }
      }
   }

   private static void send(HttpExchange exchange, int code, String contentType, String body) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", contentType);
      exchange.sendResponseHeaders(code, bytes.length > 0 ? bytes.length : -1L);
      if (bytes.length > 0) {
         try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
         }
      } else {
         exchange.close();
      }
   }

   private static void send404(HttpExchange exchange) throws IOException {
      send(exchange, 404, "text/plain", "Not found");
   }
}
