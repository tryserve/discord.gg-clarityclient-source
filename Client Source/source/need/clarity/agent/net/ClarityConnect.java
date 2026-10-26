package need.clarity.agent.net;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import need.clarity.agent.AgentStatus;

public final class ClarityConnect {
   private static final String DOMAIN = "CLR1";
   private static final byte T_JSON = 1;
   private static final byte T_RAW = 2;
   private static final int GCM_TAG = 16;
   private static final int GCM_IV_LEN = 12;
   private static final byte[] SPKI_X25519 = ServerKey.hexBytes("302a300506032b656e032100");
   private static final long BEAT_INTERVAL_MS = 30000L;
   private static final long BEAT_TIMEOUT_MS = 90000L;
   private final String wsUrl;
   private final String licenceKey;
   private final String hwid;
   private final String uuid;
   private final String build;
   private final long pid;
   private final ClassLoader gameLoader;
   private final Instrumentation inst;
   private final ClarityConnect.Progress progress;
   private SecretKey kC2S;
   private SecretKey kS2C;
   private SecretKey kAck;
   private final AtomicLong txCtr = new AtomicLong(1L);
   private final AtomicLong rxCtr = new AtomicLong(0L);
   private Map<String, Object> manifest;
   private List<byte[]> stages;
   private int stageCount;
   private WebSocket ws;
   private final LinkedBlockingQueue<Object> inbox = new LinkedBlockingQueue<>();
   private final AtomicBoolean closed = new AtomicBoolean(false);
   private ScheduledExecutorService beater;
   private static final byte[] EMPTY_CHUNK = new byte[0];

   public static ClarityConnect.ConnectResult connect(
      String wsUrl, String licenceKey, String hwid, String uuid, String build, long pid, ClassLoader gameLoader, Instrumentation inst, ClarityConnect.Progress progress
   ) {
      try {
         return new ClarityConnect(wsUrl, licenceKey, hwid, uuid, build, pid, gameLoader, inst, progress).run();
      } catch (ClarityConnect.DenyException e) {
         return new ClarityConnect.ConnectResult(ClarityConnect.Result.DENIED, null);
      } catch (Exception e) {
         e.printStackTrace();
         return new ClarityConnect.ConnectResult(ClarityConnect.Result.ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
      }
   }

   private ClarityConnect(String wsUrl, String licenceKey, String hwid, String uuid, String build, long pid, ClassLoader gameLoader, Instrumentation inst, ClarityConnect.Progress progress) {
      this.wsUrl = wsUrl;
      this.licenceKey = licenceKey;
      this.hwid = hwid;
      this.uuid = uuid;
      this.build = build;
      this.pid = pid;
      this.gameLoader = gameLoader;
      this.inst = inst;
      this.progress = progress;
   }

   private ClarityConnect.ConnectResult run() throws Exception {
      this.progress.report(2, "Connecting to the licence server");
      this.openWebSocket();
      this.progress.report(5, "Verifying server identity");
      Map<String, String> hello = this.nextJson();
      if (!"hello".equals(hello.get("t"))) {
         throw new ClarityConnect.ProtocolException("expected hello");
      }

      byte[] ns = b64d(hello.get("ns"));
      byte[] spk = b64d(hello.get("spk"));
      byte[] sig = b64d(hello.get("sig"));
      PublicKey hsPub = ServerKey.ed25519("96de94e8da32afbc8d879671f1444edd9c3c98308739f901512bfdae94ba87da");
      byte[] sigMsg = concat(utf8("CLR1hello"), ns, spk);
      if (!verify(hsPub, sigMsg, sig)) {
         throw new SecurityException("Server identity verification failed");
      }

      this.progress.report(10, "Performing key exchange");
      KeyPair eph = genX25519();
      byte[] nc = randomBytes(32);
      byte[] cpk = rawPub(eph.getPublic());
      byte[] secret = x25519(eph.getPrivate(), spk);
      byte[] salt = concat(ns, nc);
      this.kC2S = hkdf(secret, salt, "clr1 c2s");
      this.kS2C = hkdf(secret, salt, "clr1 s2c");
      this.kAck = hkdf(secret, salt, "clr1 ack");
      byte[] transcript = sha256(concat(utf8("CLR1"), ns, spk, nc, cpk));
      String authJson = "{\"t\":\"auth\",\"cpk\":\"" + b64e(cpk) + "\",\"nc\":\"" + b64e(nc) + "\"}";
      this.ws.sendText(authJson, true);
      this.progress.report(15, "Sending licence key");
      String reqJson = "{\"t\":\"req\",\"key\":"
         + jsonStr(this.licenceKey)
         + ",\"hwid\":"
         + jsonStr(this.hwid)
         + ",\"uuid\":"
         + jsonStr(this.uuid)
         + ",\"build\":"
         + jsonStr(this.build)
         + ",\"pid\":"
         + this.pid
         + "}";
      this.sendEncrypted(reqJson);
      this.progress.report(20, "Waiting for licence verification");
      Map<String, String> grant = this.nextEncryptedJson();
      if (!"grant".equals(grant.get("t"))) {
         throw new ClarityConnect.DenyException();
      }

      byte[] tsig = b64d(grant.get("tsig"));
      if (!verify(hsPub, transcript, tsig)) {
         throw new SecurityException("Transcript signature invalid");
      }

      byte[] manifestBytes = b64d(grant.get("manifest"));
      byte[] msig = b64d(grant.get("msig"));
      PublicKey relPub = ServerKey.ed25519("174ed3cf53647a9c7559824aaf7959f8a0a7b87ee41db0ec5510b928df790537");
      if (!verify(relPub, manifestBytes, msig)) {
         throw new SecurityException("Payload manifest signature invalid");
      }

      this.manifest = parseManifest(manifestBytes);
      List<Map<String, Object>> stageMeta = (List<Map<String, Object>>)this.manifest.get("stages");
      this.stageCount = stageMeta.size();
      this.stages = new ArrayList<>(this.stageCount);

      for (int n = 0; n < this.stageCount; n++) {
         int startPct = 25 + (int)((double)n / this.stageCount * 55.0);
         int endPct = 25 + (int)((double)(n + 1) / this.stageCount * 55.0);
         this.progress.report(startPct, "Downloading stage " + (n + 1) + " of " + this.stageCount);
         byte[] stage = this.downloadStage(n, endPct, stageMeta.get(n));
         this.stages.add(stage);
         this.sendAck(n, stage, stageMeta.get(n));
      }

      this.progress.report(82, "Starting session heartbeat");
      this.startHeartbeat();
      this.progress.report(85, "Defining classes");
      this.bootstrapFromMemory();
      this.progress.report(100, "Done");
      return new ClarityConnect.ConnectResult(ClarityConnect.Result.OK, null);
   }

   private byte[] downloadStage(int n, int endPct, Map<String, Object> meta) throws Exception {
      this.sendEncrypted("{\"t\":\"stage\",\"n\":" + n + "}");
      Map<String, String> begin = this.nextEncryptedJson();
      if (!"stage_begin".equals(begin.get("t"))) {
         throw new ClarityConnect.ProtocolException("expected stage_begin, got " + begin.get("t"));
      }

      long size = Long.parseLong(begin.get("size"));
      String sha256 = begin.get("sha256");
      ByteArrayOutputStream buf = new ByteArrayOutputStream((int)Math.max(0L, size));
      long got = 0L;
      int basePct = 25 + (int)((double)n / Math.max(1, this.stageCount) * 55.0);

      while (got < size) {
         byte[] chunk = this.nextEncryptedRaw();
         if (chunk == EMPTY_CHUNK) {
            break;
         }

         buf.write(chunk);
         got += chunk.length;
         int pct = size > 0L ? (int)(basePct + (double)got / size * (endPct - basePct)) : endPct;
         this.progress.report(pct, "Downloading stage " + (n + 1) + " (" + got / 1024L + " / " + size / 1024L + " KB)");
      }

      Map<String, String> end = this.nextEncryptedJson();
      if (!"stage_end".equals(end.get("t"))) {
         throw new ClarityConnect.ProtocolException("expected stage_end, got " + end.get("t"));
      } else {
         byte[] data = buf.toByteArray();
         if (!ServerKey.hexString(sha256(data)).equals(sha256)) {
            throw new SecurityException("Stage " + n + " hash mismatch (received " + data.length + " of " + size + " bytes)");
         } else {
            return data;
         }
      }
   }

   private void sendAck(int n, byte[] stageData, Map<String, Object> meta) throws Exception {
      String sha256 = (String)meta.get("sha256");
      byte[] macKey = this.kAck.getEncoded();
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(macKey, "HmacSHA256"));
      mac.update(ServerKey.hexBytes(sha256));
      byte[] m = mac.doFinal();
      this.sendEncrypted("{\"t\":\"ack\",\"n\":" + n + ",\"mac\":\"" + b64e(m) + "\"}");
   }

   private void startHeartbeat() {
      this.beater = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread t = new Thread(r, "clarity-heartbeat");
         t.setDaemon(true);
         return t;
      });
      AtomicLong lastOk = new AtomicLong(System.currentTimeMillis());
      this.beater.scheduleWithFixedDelay(() -> {
         if (this.closed.get()) {
            this.beater.shutdownNow();
         } else {
            try {
               this.sendEncrypted("{\"t\":\"beat\"}");
               long deadline = System.currentTimeMillis() + 15000L;

               while (System.currentTimeMillis() < deadline) {
                  Object msg = this.inbox.poll(500L, TimeUnit.MILLISECONDS);
                  if (msg != null && msg instanceof String s) {
                     Map<String, String> m = parseJson(s);
                     if ("beat_ok".equals(m.get("t"))) {
                        lastOk.set(System.currentTimeMillis());
                        return;
                     }

                     if ("kill".equals(m.get("t"))) {
                        this.shutdown();
                        return;
                     }
                  }
               }
            } catch (Exception var7) {
            }

            if (System.currentTimeMillis() - lastOk.get() > 90000L) {
               this.shutdown();
            }
         }
      }, 30000L, 30000L, TimeUnit.MILLISECONDS);
   }

   private void shutdown() {
      if (this.closed.compareAndSet(false, true)) {
         try {
            this.ws.sendClose(1000, "bye");
         } catch (Exception var4) {
         }

         if (this.beater != null) {
            this.beater.shutdownNow();
         }

         try {
            Class<?> cb = Class.forName("need.clarity.ClarityBootstrap", false, this.gameLoader);
            Method m = cb.getMethod("disableAll");
            m.invoke(null);
         } catch (Exception var3) {
         }
      }
   }

   private void bootstrapFromMemory() throws Exception {
      byte[] agentStage = this.stages.get(0);
      extractNatives(agentStage);
      extractResources(agentStage, this.inst);
      ClarityConnect.InMemoryClassLoader memCl = new ClarityConnect.InMemoryClassLoader(agentStage, this.gameLoader);

      for (int i = 1; i < this.stages.size(); i++) {
         byte[] nanoStage = this.stages.get(i);
         new ClarityConnect.InMemoryClassLoader(nanoStage, memCl);
      }

      injectBridgesFromBytes(agentStage, this.gameLoader, this.inst);
      if (this.inst != null) {
         Class<?> tfClass = memCl.loadClass("need.clarity.agent.ClarityTransformer");
         ClassFileTransformer tf = (ClassFileTransformer)tfClass.getDeclaredConstructor().newInstance();
         this.inst.addTransformer(tf, true);
         retransformTargets(this.inst, tfClass);
      }

      Class<?> bootstrap = memCl.loadClass("need.clarity.agent.ClarityBootstrap");
      Method init = bootstrap.getMethod("initialize", ClassLoader.class);
      init.invoke(null, this.gameLoader);
      AgentStatus.markLoaded();
   }

   private static void injectBridgesFromBytes(byte[] jarBytes, ClassLoader targetLoader, Instrumentation inst) throws Exception {
      Map<String, byte[]> entries = new HashMap<>();
      ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(jarBytes));

      ZipEntry e;
      try {
         while ((e = z.getNextEntry()) != null) {
            if (!e.isDirectory()) {
               entries.put(e.getName(), z.readAllBytes());
            }
         }
      } catch (Throwable var20) {
         try {
            z.close();
         } catch (Throwable var17) {
            var20.addSuppressed(var17);
         }

         throw var20;
      }

      z.close();
      Module var21 = ClassLoader.class.getModule();
      Module var22 = ClarityConnect.class.getModule();
      if (!var21.isOpen("java.lang", var22)) {
         if (inst == null) {
            throw new IllegalStateException("Instrumentation unavailable: cannot open java.lang to define bridges");
         }

         inst.redefineModule(var21, Set.of(), Map.of(), Map.of("java.lang", Set.of(var22)), Set.of(), Map.of());
      }

      Method define = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
      define.setAccessible(true);
      List<String> pending = new ArrayList<>();

      for (String entry : new TreeSet<>(entries.keySet())) {
         if (entry.endsWith(".class") && (entry.startsWith("need/clarity/") && !entry.startsWith("need/clarity/agent/") || entry.startsWith("org/lwjgl/"))) {
            pending.add(entry.substring(0, entry.length() - ".class".length()));
         }
      }

      Map<String, Throwable> lastError = new LinkedHashMap<>();

      for (int pass = 0; pass < 6 && !pending.isEmpty(); pass++) {
         List<String> next = new ArrayList<>();

         for (String internalName : pending) {
            String binaryName = internalName.replace('/', '.');

            try {
               Class.forName(binaryName, false, targetLoader);
            } catch (ClassNotFoundException var19) {
               byte[] bytecode = entries.get(internalName + ".class");

               try {
                  define.invoke(targetLoader, binaryName, bytecode, 0, bytecode.length);
               } catch (InvocationTargetException ite) {
                  Throwable c = ite.getCause();
                  if (!(c instanceof LinkageError) || c.getMessage() == null || !c.getMessage().contains("duplicate")) {
                     lastError.put(internalName, c);
                     next.add(internalName);
                  }
               }
            }
         }

         if (next.size() == pending.size() && pass > 0) {
            pending = next;
            break;
         }

         pending = next;
      }

      if (!pending.isEmpty()) {
         StringBuilder sb = new StringBuilder("Could not define bridge classes in the game loader: ");

         for (String n : pending) {
            sb.append(n).append(" (").append(lastError.get(n)).append("); ");
         }

         throw new IOException(sb.toString());
      }
   }

   private static void extractResources(byte[] jarBytes, Instrumentation inst) throws IOException {
      File resJar = File.createTempFile("clarity-resources", ".jar");
      resJar.deleteOnExit();
      Set<String> seen = new HashSet<>();

      ZipEntry e;
      try (
         ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(jarBytes));
         ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(resJar)));
      ) {
         while ((e = zin.getNextEntry()) != null) {
            String name = e.getName();
            if (!e.isDirectory() && !name.endsWith(".class") && !name.startsWith("META-INF/") && seen.add(name)) {
               byte[] data = zin.readAllBytes();
               ZipEntry out = new ZipEntry(name);
               out.setSize(data.length);
               zout.putNextEntry(out);
               zout.write(data);
               zout.closeEntry();
            }
         }
      }

      JarFile jf;
      try {
         jf = new JarFile(resJar);
      } catch (IOException ex) {
         return;
      }

      if (inst != null) {
         inst.appendToSystemClassLoaderSearch(jf);
         inst.appendToBootstrapClassLoaderSearch(jf);
      } else {
         try {
            ClassLoader sysCl = ClassLoader.getSystemClassLoader();
            Method addUrl = sysCl.getClass().getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(sysCl, resJar.toURI().toURL());
         } catch (Exception var12) {
         }
      }

      System.setProperty("clarity.resources.jar", resJar.getAbsolutePath());
   }

   private static void extractNatives(byte[] jarBytes) throws IOException {
      String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String os = osName.contains("win") ? "windows" : (osName.contains("mac") ? "macos" : "linux");
      String archProp = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      String arch = !archProp.contains("aarch64") && !archProp.contains("arm64") ? "x64" : "arm64";
      String prefix = os + "/" + arch + "/";
      File dir = File.createTempFile("clarity-natives", "");
      if (dir.delete() && dir.mkdirs()) {
         dir.deleteOnExit();
         int extracted = 0;
         ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(jarBytes));

         ZipEntry e;
         try {
            while ((e = z.getNextEntry()) != null) {
               if (!e.isDirectory() && e.getName().startsWith(prefix)) {
                  String fileName = e.getName().substring(e.getName().lastIndexOf(47) + 1);
                  File out = new File(dir, fileName);

                  try (FileOutputStream fos = new FileOutputStream(out)) {
                     z.transferTo(fos);
                  }

                  out.deleteOnExit();
                  extracted++;
               }
            }
         } catch (Throwable var18) {
            try {
               z.close();
            } catch (Throwable var15) {
               var18.addSuppressed(var15);
            }

            throw var18;
         }

         z.close();
         if (extracted != 0) {
            String existing = System.getProperty("org.lwjgl.librarypath");
            System.setProperty("org.lwjgl.librarypath", existing != null && !existing.isEmpty() ? existing + File.pathSeparator + dir.getAbsolutePath() : dir.getAbsolutePath());
         }
      } else {
         throw new IOException("Could not create natives dir: " + dir);
      }
   }

   private static void retransformTargets(Instrumentation inst, Class<?> tfClass) {
      try {
         Method isTarget = tfClass.getMethod("isTargetClass", String.class);
         List<Class<?>> targets = new ArrayList<>();

         for (Class<?> c : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(c)) {
               String n = c.getName().replace('.', '/');

               try {
                  if (Boolean.TRUE.equals(isTarget.invoke(null, n))) {
                     targets.add(c);
                  }
               } catch (Exception var11) {
               }
            }
         }

         for (Class<?> c : targets) {
            try {
               inst.retransformClasses(c);
            } catch (Throwable var10) {
            }
         }
      } catch (Exception var12) {
      }
   }

   private void openWebSocket() throws Exception {
      HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
      CompletableFuture<WebSocket> fut = client.newWebSocketBuilder().buildAsync(URI.create(this.wsUrl), new Listener() {
         private final StringBuilder textBuf = new StringBuilder();
         private final ByteArrayOutputStream binBuf = new ByteArrayOutputStream();

         @Override
         public CompletionStage<?> onText(WebSocket ws, CharSequence d, boolean last) {
            this.textBuf.append(d);
            if (last) {
               ClarityConnect.this.inbox.add(this.textBuf.toString());
               this.textBuf.setLength(0);
            }

            ws.request(1L);
            return null;
         }

         @Override
         public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer d, boolean last) {
            byte[] part = new byte[d.remaining()];
            d.get(part);
            this.binBuf.write(part, 0, part.length);
            if (last) {
               byte[] frame = this.binBuf.toByteArray();
               this.binBuf.reset();
               Object decoded = ClarityConnect.this.decryptFrame(frame);
               if (decoded != null) {
                  ClarityConnect.this.inbox.add(decoded);
               }
            }

            ws.request(1L);
            return null;
         }

         @Override
         public void onError(WebSocket ws, Throwable e) {
            ClarityConnect.this.closed.set(true);
            ClarityConnect.this.inbox.add(e);
         }

         @Override
         public CompletionStage<?> onClose(WebSocket ws, int sc, String reason) {
            ClarityConnect.this.closed.set(true);
            ClarityConnect.this.inbox.add(new ClarityConnect.ClosedMarker(sc));
            return null;
         }
      });
      this.ws = fut.get(15L, TimeUnit.SECONDS);
      this.ws.request(2147483647L);
   }

   private Object decryptFrame(byte[] frame) {
      if (this.kS2C == null) {
         return null;
      }

      if (frame.length < 25) {
         return null;
      }

      long ctr = ByteBuffer.wrap(frame, 0, 8).getLong();
      if (ctr <= this.rxCtr.get()) {
         return null;
      }

      byte[] iv = new byte[12];
      ByteBuffer.wrap(frame, 0, 8).getLong();
      System.arraycopy(frame, 0, iv, 4, 8);

      try {
         Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
         c.init(2, this.kS2C, new GCMParameterSpec(128, iv));
         c.updateAAD("s2c".getBytes(StandardCharsets.UTF_8));
         byte[] ct = Arrays.copyOfRange(frame, 8, frame.length);
         byte[] pt = c.doFinal(ct);
         this.rxCtr.set(ctr);
         byte type = pt[0];
         byte[] body = Arrays.copyOfRange(pt, 1, pt.length);
         if (type == 1) {
            return new String(body, StandardCharsets.UTF_8);
         } else {
            return type == 2 ? body : null;
         }
      } catch (Exception e) {
         return null;
      }
   }

   private void sendEncrypted(String json) throws Exception {
      long ctr = this.txCtr.getAndIncrement();
      byte[] iv = new byte[12];
      ByteBuffer.wrap(iv).putLong(4, ctr);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(1, this.kC2S, new GCMParameterSpec(128, iv));
      c.updateAAD("c2s".getBytes(StandardCharsets.UTF_8));
      byte[] body = json.getBytes(StandardCharsets.UTF_8);
      byte[] plain = new byte[1 + body.length];
      plain[0] = 1;
      System.arraycopy(body, 0, plain, 1, body.length);
      byte[] ct = c.doFinal(plain);
      byte[] hdr = new byte[8];
      ByteBuffer.wrap(hdr).putLong(ctr);
      this.ws.sendBinary(ByteBuffer.wrap(concat(hdr, ct)), true).get(10L, TimeUnit.SECONDS);
   }

   private Map<String, String> nextJson() throws Exception {
      return parseJson(this.nextRawText());
   }

   private String nextRawText() throws Exception {
      Object o;
      do {
         o = this.inbox.poll(30L, TimeUnit.SECONDS);
         if (o == null) {
            throw new ClarityConnect.ProtocolException("Timeout waiting for server message");
         }

         if (o instanceof Throwable t) {
            throw new IOException("WebSocket error", t);
         }

         if (o instanceof ClarityConnect.ClosedMarker cm) {
            if (cm.code() == 4001) {
               throw new ClarityConnect.DenyException();
            }

            throw new ClarityConnect.ProtocolException("Socket closed: " + cm.code());
         }
      } while (!(o instanceof String));

      return (String)o;
   }

   private Map<String, String> nextEncryptedJson() throws Exception {
      Object o;
      do {
         o = this.inbox.poll(30L, TimeUnit.SECONDS);
         if (o == null) {
            throw new ClarityConnect.ProtocolException("Timeout");
         }

         if (o instanceof Throwable t) {
            throw new IOException("WebSocket error", t);
         }

         if (o instanceof ClarityConnect.ClosedMarker cm) {
            if (cm.code() == 4001) {
               throw new ClarityConnect.DenyException();
            }

            throw new ClarityConnect.ProtocolException("Socket closed: " + cm.code());
         }
      } while (!(o instanceof String));

      String s = (String)o;
      return parseJson(s);
   }

   private byte[] nextEncryptedRaw() throws Exception {
      while (true) {
         Object o = this.inbox.poll(60L, TimeUnit.SECONDS);
         if (o == null) {
            throw new ClarityConnect.ProtocolException("Timeout waiting for chunk");
         }

         if (o instanceof Throwable t) {
            throw new IOException("WebSocket error", t);
         }

         if (o instanceof ClarityConnect.ClosedMarker cm) {
            throw new ClarityConnect.ProtocolException("Socket closed: " + cm.code());
         }

         if (o instanceof byte[] b) {
            return b;
         }

         if (o instanceof String s) {
            Map<String, String> m = parseJson(s);
            String type = m.get("t");
            if ("stage_end".equals(type)) {
               this.inbox.add(s);
               return EMPTY_CHUNK;
            }

            if ("stage_begin".equals(type)) {
               throw new ClarityConnect.ProtocolException("unexpected stage_begin in chunk stream");
            }

            this.inbox.add(s);
         }
      }
   }

   private static KeyPair genX25519() throws Exception {
      return KeyPairGenerator.getInstance("XDH").generateKeyPair();
   }

   private static byte[] x25519(PrivateKey priv, byte[] peerRaw) throws Exception {
      byte[] der = new byte[SPKI_X25519.length + 32];
      System.arraycopy(SPKI_X25519, 0, der, 0, SPKI_X25519.length);
      System.arraycopy(peerRaw, 0, der, SPKI_X25519.length, 32);
      PublicKey pub = KeyFactory.getInstance("XDH").generatePublic(new X509EncodedKeySpec(der));
      KeyAgreement ka = KeyAgreement.getInstance("XDH");
      ka.init(priv);
      ka.doPhase(pub, true);
      return ka.generateSecret();
   }

   private static byte[] rawPub(PublicKey pub) throws Exception {
      byte[] spki = pub.getEncoded();
      return Arrays.copyOfRange(spki, spki.length - 32, spki.length);
   }

   private static SecretKey hkdf(byte[] ikm, byte[] salt, String info) throws Exception {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(salt.length == 0 ? new byte[32] : salt, "HmacSHA256"));
      byte[] prk = mac.doFinal(ikm);
      mac.init(new SecretKeySpec(prk, "HmacSHA256"));
      mac.update(info.getBytes(StandardCharsets.UTF_8));
      mac.update((byte)1);
      return new SecretKeySpec(mac.doFinal(), "AES");
   }

   private static boolean verify(PublicKey pub, byte[] msg, byte[] sig) throws Exception {
      Signature s = Signature.getInstance("Ed25519");
      s.initVerify(pub);
      s.update(msg);
      return s.verify(sig);
   }

   private static byte[] sha256(byte[] data) throws Exception {
      return MessageDigest.getInstance("SHA-256").digest(data);
   }

   private static byte[] randomBytes(int n) {
      byte[] b = new byte[n];
      new SecureRandom().nextBytes(b);
      return b;
   }

   private static byte[] b64d(String s) {
      return Base64.getDecoder().decode(s);
   }

   private static String b64e(byte[] b) {
      return Base64.getEncoder().encodeToString(b);
   }

   private static byte[] utf8(String s) {
      return s.getBytes(StandardCharsets.UTF_8);
   }

   private static byte[] concat(byte[]... arrays) {
      int total = 0;

      for (byte[] a : arrays) {
         total += a.length;
      }

      byte[] out = new byte[total];
      int off = 0;

      for (byte[] a : arrays) {
         System.arraycopy(a, 0, out, off, a.length);
         off += a.length;
      }

      return out;
   }

   private static Map<String, String> parseJson(String s) {
      Map<String, String> m = new LinkedHashMap<>();
      s = s.trim();
      if (s.startsWith("{")) {
         s = s.substring(1, s.lastIndexOf(125));
      }

      String[] tokens = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

      for (String tok : tokens) {
         int colon = tok.indexOf(58);
         if (colon >= 0) {
            String k = tok.substring(0, colon).trim().replaceAll("^\"|\"+$", "");
            String v = tok.substring(colon + 1).trim().replaceAll("^\"|\"+$", "");
            m.put(k, v);
         }
      }

      return m;
   }

   private static Map<String, Object> parseManifest(byte[] bytes) {
      String s = new String(bytes, StandardCharsets.UTF_8);
      Map<String, Object> out = new LinkedHashMap<>();
      List<Map<String, Object>> stageList = new ArrayList<>();
      int si = s.indexOf("\"stages\"");
      if (si >= 0) {
         int arr = s.indexOf(91, si);
         int depth = 0;
         int start = arr;

         int i;
         for (i = arr; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '[' || ch == '{') {
               depth++;
            } else if (ch == ']' || ch == '}') {
               if (--depth == 0) {
                  i++;
                  break;
               }
            }
         }

         String arrStr = s.substring(start + 1, i - 1).trim();

         for (String obj : splitTopLevel(arrStr)) {
            Map<String, Object> stage = new LinkedHashMap<>();

            for (String line : obj.replaceAll("^\\{|\\}$", "").split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
               int colon = line.indexOf(58);
               if (colon >= 0) {
                  String k = line.substring(0, colon).trim().replaceAll("^\"|\"$", "");
                  String v = line.substring(colon + 1).trim().replaceAll("^\"|\"$", "");
                  stage.put(k, v);
               }
            }

            stageList.add(stage);
         }
      }

      out.put("stages", stageList);
      return out;
   }

   private static List<String> splitTopLevel(String s) {
      List<String> out = new ArrayList<>();
      int depth = 0;
      int start = 0;

      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         if (ch == '{' || ch == '[') {
            depth++;
         } else if (ch == '}' || ch == ']') {
            depth--;
         } else if (ch == ',' && depth == 0) {
            out.add(s.substring(start, i).trim());
            start = i + 1;
         }
      }

      if (start < s.length()) {
         out.add(s.substring(start).trim());
      }

      return out;
   }

   private static String jsonStr(String v) {
      return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
   }

   private record ClosedMarker(int code) {
   }

   public static final class ConnectResult {
      public final ClarityConnect.Result result;
      public final String error;

      ConnectResult(ClarityConnect.Result r, String e) {
         this.result = r;
         this.error = e;
      }
   }

   private static final class DenyException extends RuntimeException {
      DenyException() {
         super("denied");
      }
   }

   private static final class InMemoryClassLoader extends ClassLoader {
      private final Map<String, byte[]> classes = new HashMap<>();
      private final Map<String, byte[]> resources = new HashMap<>();

      InMemoryClassLoader(byte[] jarBytes, ClassLoader parent) throws IOException {
         super(parent);

         ZipEntry e;
         try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            while ((e = z.getNextEntry()) != null) {
               if (!e.isDirectory()) {
                  byte[] data = z.readAllBytes();
                  if (e.getName().endsWith(".class")) {
                     String name = e.getName().replace('/', '.').replaceFirst("\\.class$", "");
                     this.classes.put(name, data);
                  }

                  this.resources.put(e.getName(), data);
               }
            }
         }
      }

      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
         byte[] b = this.classes.get(name);
         if (b == null) {
            throw new ClassNotFoundException(name);
         } else {
            return this.defineClass(name, b, 0, b.length);
         }
      }

      @Override
      public InputStream getResourceAsStream(String name) {
         String key = name.startsWith("/") ? name.substring(1) : name;
         byte[] b = this.resources.get(key);
         return b != null ? new ByteArrayInputStream(b) : super.getResourceAsStream(name);
      }
   }

   public interface Progress {
      void report(int var1, String var2);
   }

   private static final class ProtocolException extends IOException {
      ProtocolException(String m) {
         super(m);
      }
   }

   public enum Result {
      OK,
      DENIED,
      ERROR;
   }
}
