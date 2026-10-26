package need.clarity.alts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import need.clarity.config.ConfigFiles;

public final class MicrosoftAuth {
   private static final String AUTHORIZE = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
   private static final String TOKEN = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
   private static final String XBL = "https://user.auth.xboxlive.com/user/authenticate";
   private static final String XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize";
   private static final String MC_LOGIN = "https://api.minecraftservices.com/authentication/login_with_xbox";
   private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
   private static final String REDIRECT_PATH = "/in_game_account_switcher_long_enough_uri_to_prevent_accidental_leaks_on_screensharing_even_if_you_have_like_extremely_big_screen_though_it_might_not_mork_but_we_will_try_it_anyway_to_prevent_funny_things_from_happening_or_something";
   private static final String SCOPE = "XboxLive.signin offline_access";
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private static final SecureRandom RNG = new SecureRandom();
   private static volatile HttpServer pending;
   private static volatile CompletableFuture<Map<String, String>> pendingCallback;
   public static final String CANCELLED = "Cancelled";

   private MicrosoftAuth() {
   }

   public static CompletableFuture<AltStore.Alt> browserLogin(String clientId, Consumer<String> status) {
      CompletableFuture<AltStore.Alt> out = new CompletableFuture<>();
      if (clientId != null && !clientId.isBlank()) {
         cancel();
         Thread t = new Thread(
            () -> {
               HttpServer server = null;

               try {
                  String verifier = randomUrlSafe(64);
                  String challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                  String state = randomUrlSafe(24);
                  server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                  pending = server;
                  int port = server.getAddress().getPort();
                  String redirect = "http://localhost:"
                     + port
                     + "/in_game_account_switcher_long_enough_uri_to_prevent_accidental_leaks_on_screensharing_even_if_you_have_like_extremely_big_screen_though_it_might_not_mork_but_we_will_try_it_anyway_to_prevent_funny_things_from_happening_or_something";
                  CompletableFuture<Map<String, String>> callback = new CompletableFuture<>();
                  pendingCallback = callback;
                  server.createContext("/", ex -> {
                     Map<String, String> qx = parseQuery(ex.getRequestURI().getRawQuery());
                     boolean ok = qx.containsKey("code") && state.equals(qx.get("state"));
                     byte[] body = page(ok).getBytes(StandardCharsets.UTF_8);
                     ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                     ex.sendResponseHeaders(200, body.length);

                     try (OutputStream os = ex.getResponseBody()) {
                        os.write(body);
                     }

                     if (qx.containsKey("code") || qx.containsKey("error")) {
                        callback.complete(qx);
                     }
                  });
                  server.start();
                  String url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id="
                     + enc(clientId)
                     + "&response_type=code&redirect_uri="
                     + enc(redirect)
                     + "&response_mode=query&scope="
                     + enc("XboxLive.signin offline_access")
                     + "&code_challenge="
                     + challenge
                     + "&code_challenge_method=S256&state="
                     + state
                     + "&prompt=select_account";
                  status.accept("Continue in your browser…");
                  ConfigFiles.openUrl(url);
                  Map<String, String> q = callback.get(5L, TimeUnit.MINUTES);
                  if (q.containsKey("error")) {
                     throw new MicrosoftAuth.AuthException(q.getOrDefault("error_description", q.get("error")));
                  }

                  if (!state.equals(q.get("state"))) {
                     throw new MicrosoftAuth.AuthException("Sign-in response didn't match this request");
                  }

                  status.accept("Signing in…");
                  JsonObject tok = postForm(
                     "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                     Map.of(
                        "client_id",
                        clientId,
                        "grant_type",
                        "authorization_code",
                        "code",
                        q.get("code"),
                        "redirect_uri",
                        redirect,
                        "code_verifier",
                        verifier,
                        "scope",
                        "XboxLive.signin offline_access"
                     )
                  );
                  AltStore.Alt alt = finish(clientId, tok, status);
                  out.complete(alt);
               } catch (ExecutionException e) {
                  Throwable c = e.getCause();
                  out.completeExceptionally(c instanceof MicrosoftAuth.AuthException ? c : new MicrosoftAuth.AuthException(friendly(c == null ? e : c)));
               } catch (TimeoutException e) {
                  out.completeExceptionally(new MicrosoftAuth.AuthException("Sign-in timed out"));
               } catch (Throwable e) {
                  out.completeExceptionally(e instanceof MicrosoftAuth.AuthException ? e : new MicrosoftAuth.AuthException(friendly(e)));
               } finally {
                  if (server != null) {
                     server.stop(0);
                  }

                  if (pending == server) {
                     pending = null;
                  }
               }
            },
            "clarity-ms-login"
         );
         t.setDaemon(true);
         t.start();
         return out;
      } else {
         out.completeExceptionally(new MicrosoftAuth.AuthException("No Azure Client ID configured (build has no default; set one in Main Menu settings)"));
         return out;
      }
   }

   public static void cancel() {
      HttpServer s = pending;
      pending = null;
      CompletableFuture<Map<String, String>> cb = pendingCallback;
      pendingCallback = null;
      if (cb != null) {
         cb.completeExceptionally(new MicrosoftAuth.AuthException("Cancelled"));
      }

      if (s != null) {
         try {
            s.stop(0);
         } catch (Throwable var3) {
         }
      }
   }

   public static AltStore.Alt ensureFresh(AltStore.Alt alt, Consumer<String> status) throws MicrosoftAuth.AuthException {
      if (alt.hasFreshToken()) {
         return alt;
      }

      if (!alt.refreshToken.isEmpty() && !alt.clientId.isEmpty()) {
         status.accept("Refreshing " + alt.name + "…");

         try {
            JsonObject tok = postForm(
               "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
               Map.of("client_id", alt.clientId, "grant_type", "refresh_token", "refresh_token", alt.refreshToken, "scope", "XboxLive.signin offline_access")
            );
            AltStore.Alt fresh = finish(alt.clientId, tok, status);
            fresh.lastUsed = alt.lastUsed;
            return fresh;
         } catch (MicrosoftAuth.AuthException e) {
            throw e;
         } catch (Throwable e) {
            throw new MicrosoftAuth.AuthException(friendly(e));
         }
      } else {
         throw new MicrosoftAuth.AuthException("Session expired, sign in again");
      }
   }

   private static AltStore.Alt finish(String clientId, JsonObject msToken, Consumer<String> status) throws Exception {
      if (!msToken.has("access_token")) {
         throw new MicrosoftAuth.AuthException(describeMsError(msToken));
      }

      String msAccess = msToken.get("access_token").getAsString();
      String refresh = msToken.has("refresh_token") ? msToken.get("refresh_token").getAsString() : "";
      status.accept("Xbox Live…");
      JsonObject xblReq = new JsonObject();
      JsonObject props = new JsonObject();
      props.addProperty("AuthMethod", "RPS");
      props.addProperty("SiteName", "user.auth.xboxlive.com");
      props.addProperty("RpsTicket", "d=" + msAccess);
      xblReq.add("Properties", props);
      xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
      xblReq.addProperty("TokenType", "JWT");
      JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", xblReq, null);
      if (!xbl.has("Token")) {
         throw new MicrosoftAuth.AuthException("Xbox Live sign-in failed");
      }

      String xblToken = xbl.get("Token").getAsString();
      String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
      JsonObject xstsReq = new JsonObject();
      JsonObject xp = new JsonObject();
      xp.addProperty("SandboxId", "RETAIL");
      JsonArray ut = new JsonArray();
      ut.add(xblToken);
      xp.add("UserTokens", ut);
      xstsReq.add("Properties", xp);
      xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
      xstsReq.addProperty("TokenType", "JWT");
      JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsReq, null);
      if (xsts.has("XErr")) {
         throw new MicrosoftAuth.AuthException(describeXErr(xsts.get("XErr").getAsLong()));
      }

      String xstsToken = xsts.get("Token").getAsString();
      status.accept("Minecraft…");
      JsonObject mcReq = new JsonObject();
      mcReq.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
      JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcReq, null);
      if (!mc.has("access_token")) {
         throw new MicrosoftAuth.AuthException(
            "Minecraft rejected the sign-in" + (mc.has("errorMessage") ? ": " + mc.get("errorMessage").getAsString() : " (is this Azure app approved for Minecraft?)")
         );
      }

      String mcToken = mc.get("access_token").getAsString();
      long expires = mc.has("expires_in") ? mc.get("expires_in").getAsLong() : 86400L;
      HttpResponse<String> pr = HTTP.send(
         HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile")).header("Authorization", "Bearer " + mcToken).timeout(Duration.ofSeconds(15L)).GET().build(),
         BodyHandlers.ofString()
      );
      if (pr.statusCode() == 404) {
         throw new MicrosoftAuth.AuthException("This account doesn't own Minecraft");
      }

      if (pr.statusCode() / 100 != 2) {
         throw new MicrosoftAuth.AuthException("Profile lookup failed (" + pr.statusCode() + ")");
      }

      JsonObject profile = JsonParser.parseString(pr.body()).getAsJsonObject();
      AltStore.Alt alt = new AltStore.Alt();
      alt.name = profile.get("name").getAsString();
      alt.uuid = profile.get("id").getAsString();
      alt.refreshToken = refresh;
      alt.clientId = clientId;
      alt.mcToken = mcToken;
      alt.mcTokenExpiry = System.currentTimeMillis() + expires * 1000L;
      return alt;
   }

   private static JsonObject postForm(String url, Map<String, String> form) throws Exception {
      StringBuilder b = new StringBuilder();

      for (Entry<String, String> e : form.entrySet()) {
         if (b.length() > 0) {
            b.append('&');
         }

         b.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
      }

      HttpResponse<String> r = HTTP.send(
         HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(20L))
            .POST(BodyPublishers.ofString(b.toString()))
            .build(),
         BodyHandlers.ofString()
      );
      return parse(r.body());
   }

   private static JsonObject postJson(String url, JsonObject body, String bearer) throws Exception {
      Builder rb = HttpRequest.newBuilder(URI.create(url))
         .header("Content-Type", "application/json")
         .header("Accept", "application/json")
         .timeout(Duration.ofSeconds(20L))
         .POST(BodyPublishers.ofString(body.toString()));
      if (bearer != null) {
         rb.header("Authorization", "Bearer " + bearer);
      }

      HttpResponse<String> r = HTTP.send(rb.build(), BodyHandlers.ofString());
      if (r.statusCode() == 403 && url.equals("https://api.minecraftservices.com/authentication/login_with_xbox")) {
         throw new MicrosoftAuth.AuthException("Minecraft refused this Azure app (403). New app IDs need Mojang's approval.");
      } else {
         return parse(r.body());
      }
   }

   private static JsonObject parse(String s) {
      try {
         return s != null && !s.isBlank() ? JsonParser.parseString(s).getAsJsonObject() : new JsonObject();
      } catch (Throwable t) {
         return new JsonObject();
      }
   }

   private static String describeMsError(JsonObject o) {
      String err = o.has("error") ? o.get("error").getAsString() : "unknown_error";
      if ("invalid_grant".equals(err)) {
         return "Session expired, sign in again";
      } else {
         return !"invalid_client".equals(err) && !"unauthorized_client".equals(err)
            ? "Microsoft sign-in failed (" + err + ")"
            : "Azure app rejected: check the Client ID and that public client flows are on";
      }
   }

   private static String describeXErr(long code) {
      if (code == 2148916233L) {
         return "This Microsoft account has no Xbox profile yet";
      } else if (code == 2148916235L) {
         return "Xbox Live isn't available in this account's country";
      } else if (code == 2148916236L || code == 2148916237L) {
         return "This account needs adult verification on xbox.com";
      } else {
         return code == 2148916238L ? "Child account: add it to a Microsoft family first" : "Xbox sign-in failed (" + code + ")";
      }
   }

   private static String friendly(Throwable e) {
      String m = e.getMessage();
      return m != null && !m.isBlank() ? m : e.getClass().getSimpleName();
   }

   private static Map<String, String> parseQuery(String raw) {
      Map<String, String> m = new HashMap<>();
      if (raw == null) {
         return m;
      }

      for (String part : raw.split("&")) {
         int i = part.indexOf(61);
         if (i > 0) {
            m.put(URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8));
         }
      }

      return m;
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }

   private static String randomUrlSafe(int bytes) {
      byte[] b = new byte[bytes];
      RNG.nextBytes(b);
      return base64Url(b);
   }

   private static String base64Url(byte[] b) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
   }

   private static String page(boolean ok) {
      String title = ok ? "Signed in" : "Sign-in cancelled";
      String sub = ok ? "You can close this tab and return to Minecraft." : "Head back to Minecraft and try again.";
      return "<!doctype html><html><head><meta charset=utf-8><title>Clarity</title><style>html,body{height:100%;margin:0;background:#000;color:#fff;font:15px/1.5 -apple-system,Segoe UI,Inter,sans-serif}body{display:flex;align-items:center;justify-content:center}.c{text-align:center}.d{width:10px;height:10px;border-radius:50%;background:#fff;margin:0 auto 22px;box-shadow:0 0 18px 4px rgba(255,255,255,.55)}h1{font-weight:600;font-size:22px;margin:0 0 6px}p{margin:0;color:rgba(255,255,255,.5)}</style></head><body><div class=c><div class=d></div><h1>"
         + title
         + "</h1><p>"
         + sub
         + "</p></div></body></html>";
   }

   public static final class AuthException extends Exception {
      public AuthException(String msg) {
         super(msg);
      }
   }
}
