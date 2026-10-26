package need.clarity.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class MediaBridge {
   private static final MediaBridge.Snapshot EMPTY = new MediaBridge.Snapshot(false, false, "", "", "", "", 0.0, 0.0, 0L, null, 0, null);
   private static volatile MediaBridge.Snapshot current = EMPTY;
   private static volatile boolean wanted;
   private static Thread worker;
   private static Process proc;
   private static Path cmdFile;
   private static int artCounter;
   private static long nextRestartAtMs;
   private static final String WIN_SCRIPT = String.join(
      "\n",
      "param([string]$CmdFile)",
      "$ErrorActionPreference = 'SilentlyContinue'",
      "[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)",
      "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
      "$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]",
      "function Await($op, $type) {",
      "  $t = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))",
      "  $t.Wait(-1) | Out-Null",
      "  $t.Result",
      "}",
      "$mgrType   = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]",
      "$propsType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]",
      "$streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime]",
      "$mgr = Await ($mgrType::RequestAsync()) $mgrType",
      "if ($mgr -eq $null) { [Console]::Out.WriteLine('{\"error\":\"no media session manager\"}'); exit 1 }",
      "function Get-Thumb($props) {",
      "  try {",
      "    $ref = $props.Thumbnail",
      "    if ($ref -eq $null) { return $null }",
      "    $stream = Await ($ref.OpenReadAsync()) $streamType",
      "    if ($stream -eq $null) { return $null }",
      "    $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)",
      "    $ms = New-Object System.IO.MemoryStream",
      "    $net.CopyTo($ms)",
      "    $net.Dispose()",
      "    if ($ms.Length -lt 64) { return $null }",
      "    return [Convert]::ToBase64String($ms.ToArray())",
      "  } catch { return $null }",
      "}",
      "$lastKey = ''; $id = 0; $thumbSent = $false; $thumbTries = 0; $thumb = $null; $tick = 0",
      "while ($true) {",
      "  try {",
      "    if (Test-Path $CmdFile) {",
      "      $c = ([System.IO.File]::ReadAllText($CmdFile)).Trim()",
      "      Remove-Item $CmdFile -Force",
      "      $cs = $mgr.GetCurrentSession()",
      "      if ($cs -ne $null) {",
      "        if ($c -eq 'next')   { $null = $cs.TrySkipNextAsync() }",
      "        if ($c -eq 'prev')   { $null = $cs.TrySkipPreviousAsync() }",
      "        if ($c -eq 'toggle') { $null = $cs.TryTogglePlayPauseAsync() }",
      "      }",
      "    }",
      "    if ($tick % 2 -eq 0) {",
      "      $s = $mgr.GetCurrentSession()",
      "      if ($s -eq $null) {",
      "        [Console]::Out.WriteLine('{\"active\":false}')",
      "      } else {",
      "        $props = Await ($s.TryGetMediaPropertiesAsync()) $propsType",
      "        $tl = $s.GetTimelineProperties()",
      "        $status = $s.GetPlaybackInfo().PlaybackStatus.ToString()",
      "        $playing = ($status -eq 'Playing')",
      "        $start = $tl.StartTime.TotalSeconds; $end = $tl.EndTime.TotalSeconds",
      "        $dur = $end - $start; if ($dur -lt 0) { $dur = 0 }",
      "        $pos = $tl.Position.TotalSeconds - $start",
      "        if ($playing -and $tl.LastUpdatedTime.Year -gt 2000) { $pos += ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalSeconds }",
      "        if ($pos -lt 0) { $pos = 0 }; if ($dur -gt 0 -and $pos -gt $dur) { $pos = $dur }",
      "        $key = '' + $props.Title + '|' + $props.Artist + '|' + $props.AlbumTitle",
      "        if ($key -ne $lastKey) { $lastKey = $key; $id++; $thumb = $null; $thumbSent = $false; $thumbTries = 0 }",
      "        if (-not $thumbSent -and $thumb -eq $null -and $thumbTries -lt 8) { $thumb = Get-Thumb $props; $thumbTries++ }",
      "        $o = [ordered]@{ active = $true; id = $id; playing = $playing; title = [string]$props.Title; artist = [string]$props.Artist; album = [string]$props.AlbumTitle; app = [string]$s.SourceAppUserModelId; pos = [math]::Round($pos, 2); dur = [math]::Round($dur, 2) }",
      "        if (-not $thumbSent -and $thumb -ne $null) { $o['thumb'] = $thumb; $thumbSent = $true }",
      "        [Console]::Out.WriteLine(($o | ConvertTo-Json -Compress))",
      "      }",
      "      [Console]::Out.Flush()",
      "    }",
      "  } catch { }",
      "  $tick++",
      "  Start-Sleep -Milliseconds 250",
      "}"
   );
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4L)).build();

   private MediaBridge() {
   }

   public static MediaBridge.Snapshot get() {
      return current;
   }

   public static synchronized void ensureStarted() {
      wanted = true;
      if (worker == null || !worker.isAlive()) {
         if (System.currentTimeMillis() >= nextRestartAtMs) {
            nextRestartAtMs = System.currentTimeMillis() + 5000L;
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Runnable r;
            if (os.contains("win")) {
               r = MediaBridge::runWindows;
            } else if (os.contains("mac")) {
               r = MediaBridge::runMac;
            } else {
               r = MediaBridge::runLinux;
            }

            worker = new Thread(r, "Clarity-MediaBridge");
            worker.setDaemon(true);
            worker.start();
         }
      }
   }

   public static synchronized void stop() {
      wanted = false;
      Process p = proc;
      if (p != null) {
         p.destroyForcibly();
      }

      proc = null;
      Thread t = worker;
      if (t != null) {
         t.interrupt();
      }

      worker = null;
      current = EMPTY;
   }

   public static void next() {
      command("next");
   }

   public static void previous() {
      command("prev");
   }

   public static void playPause() {
      command("toggle");
   }

   private static void command(String cmd) {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      Thread t = new Thread(() -> {
         try {
            if (os.contains("win")) {
               Path f = cmdFile;
               if (f != null) {
                  Files.writeString(f, cmd, StandardCharsets.UTF_8);
               }
            } else if (os.contains("mac")) {
               String c = switch (cmd) {
                  case "next" -> "next";
                  case "prev" -> "previous";
                  default -> "togglePlayPause";
               };
               new ProcessBuilder("nowplaying-cli", c).redirectErrorStream(true).start().waitFor(3L, TimeUnit.SECONDS);
            } else {
               String c = switch (cmd) {
                  case "next" -> "next";
                  case "prev" -> "previous";
                  default -> "play-pause";
               };
               new ProcessBuilder("playerctl", c).redirectErrorStream(true).start().waitFor(3L, TimeUnit.SECONDS);
            }
         } catch (Throwable var5) {
         }
      }, "Clarity-MediaCmd");
      t.setDaemon(true);
      t.start();
   }

   private static void apply(boolean active, boolean playing, String title, String artist, String album, String app, double pos, double dur, byte[] newArt, boolean trackIdChanged) {
      MediaBridge.Snapshot old = current;
      if (!active) {
         int v = old.art != null ? ++artCounter + 1000 : old.artVersion;
         current = new MediaBridge.Snapshot(false, false, "", "", "", "", 0.0, 0.0, 0L, null, v, null);
      } else {
         boolean sameTrack = old.active && !trackIdChanged && eq(old.title, title) && eq(old.artist, artist);
         byte[] art = sameTrack ? old.art : null;
         int ver = old.artVersion;
         if (newArt != null) {
            art = newArt;
            ver = ++artCounter + 1000;
         } else if (!sameTrack && old.art != null) {
            ver = ++artCounter + 1000;
         }

         double usePos = pos;
         long stamp = System.nanoTime();
         if (sameTrack && old.playing == playing && Math.abs(old.durSec - dur) < 0.5) {
            double expected = old.position();
            if (Math.abs(expected - pos) < 1.25) {
               usePos = old.posSec;
               stamp = old.stampNanos;
            }
         }

         current = new MediaBridge.Snapshot(true, playing, title, artist, album, app, usePos, dur, stamp, art, ver, null);
      }
   }

   private static boolean eq(String a, String b) {
      return a == null ? b == null : a.equals(b);
   }

   private static void fail(String msg) {
      current = new MediaBridge.Snapshot(false, false, "", "", "", "", 0.0, 0.0, 0L, null, current.artVersion, msg);
   }

   private static void runWindows() {
      try {
         Path dir = Path.of(System.getProperty("java.io.tmpdir"), "clarity_media");
         Files.createDirectories(dir);
         Path script = dir.resolve("media.ps1");
         byte[] body = WIN_SCRIPT.getBytes(StandardCharsets.UTF_8);
         byte[] bom = new byte[]{-17, -69, -65};
         byte[] all = new byte[bom.length + body.length];
         System.arraycopy(bom, 0, all, 0, bom.length);
         System.arraycopy(body, 0, all, bom.length, body.length);
         Files.write(script, all);
         cmdFile = dir.resolve("cmd.txt");
         Files.deleteIfExists(cmdFile);
         ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(), cmdFile.toString()
         );
         pb.redirectErrorStream(false);
         pb.redirectError(Redirect.DISCARD);
         Process p = pb.start();
         proc = p;

         try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            int lastId = -1;

            String line;
            while (wanted && (line = br.readLine()) != null) {
               if (!line.isEmpty() && line.charAt(0) == '\ufeff') {
                  line = line.substring(1);
               }

               if (!line.isEmpty() && line.charAt(0) == '{') {
                  try {
                     JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                     if (o.has("error")) {
                        fail(o.get("error").getAsString());
                        break;
                     }

                     if (o.has("active") && o.get("active").getAsBoolean()) {
                        int id = o.get("id").getAsInt();
                        byte[] art = null;
                        if (o.has("thumb")) {
                           art = Base64.getDecoder().decode(o.get("thumb").getAsString());
                        }

                        apply(
                           true,
                           o.get("playing").getAsBoolean(),
                           str(o, "title"),
                           str(o, "artist"),
                           str(o, "album"),
                           friendlyApp(str(o, "app")),
                           o.get("pos").getAsDouble(),
                           o.get("dur").getAsDouble(),
                           art,
                           id != lastId
                        );
                        lastId = id;
                     } else {
                        apply(false, false, "", "", "", "", 0.0, 0.0, null, false);
                     }
                  } catch (Throwable var14) {
                  }
               }
            }
         }

         p.destroyForcibly();
      } catch (Throwable t) {
         fail("PowerShell unavailable");
      }
   }

   private static String str(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
   }

   private static String friendlyApp(String id) {
      String l = id == null ? "" : id.toLowerCase(Locale.ROOT);
      if (l.contains("spotify")) {
         return "Spotify";
      } else if (l.contains("chrome")) {
         return "Chrome";
      } else if (l.contains("msedge") || l.contains("edge")) {
         return "Edge";
      } else if (l.contains("firefox")) {
         return "Firefox";
      } else if (l.contains("brave")) {
         return "Brave";
      } else if (l.contains("opera")) {
         return "Opera";
      } else if (l.contains("vlc")) {
         return "VLC";
      } else {
         return !l.contains("music") && !l.contains("itunes") ? "" : "Music";
      }
   }

   private static void runLinux() {
      String artUrlDone = null;
      String lastKey = "";
      int id = 0;
      boolean warned = false;

      while (wanted && !Thread.currentThread().isInterrupted()) {
         try {
            String sep = "\u001f";
            Process p = new ProcessBuilder(
                  "playerctl",
                  "metadata",
                  "--format",
                  "{{status}}" + sep + "{{title}}" + sep + "{{artist}}" + sep + "{{album}}" + sep + "{{mpris:length}}" + sep + "{{position}}" + sep + "{{mpris:artUrl}}" + sep + "{{playerName}}"
               )
               .redirectErrorStream(false)
               .redirectError(Redirect.DISCARD)
               .start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));

            String out;
            try {
               out = br.readLine();
            } catch (Throwable var19) {
               try {
                  br.close();
               } catch (Throwable var18) {
                  var19.addSuppressed(var18);
               }

               throw var19;
            }

            br.close();
            p.waitFor(3L, TimeUnit.SECONDS);
            if (out != null && p.exitValue() == 0) {
               String[] f = out.split(sep, -1);
               if (f.length >= 8) {
                  boolean playing = "Playing".equalsIgnoreCase(f[0]);
                  double dur = parseMicros(f[4]);
                  double pos = parseMicros(f[5]);
                  String key = f[1] + "|" + f[2] + "|" + f[3];
                  boolean changed = !key.equals(lastKey);
                  if (changed) {
                     lastKey = key;
                     id++;
                     artUrlDone = null;
                  }

                  byte[] art = null;
                  String artUrl = f[6];
                  if (artUrl != null && !artUrl.isBlank() && !artUrl.equals(artUrlDone)) {
                     art = fetchArt(artUrl);
                     if (art != null) {
                        artUrlDone = artUrl;
                     }
                  }

                  apply(true, playing, f[1], f[2], f[3], cap(f[7]), pos, dur, art, changed);
               }
            } else {
               apply(false, false, "", "", "", "", 0.0, 0.0, null, false);
            }
         } catch (IOException e) {
            if (!warned) {
               fail("playerctl not found");
               warned = true;
            }
         } catch (InterruptedException e) {
            return;
         } catch (Throwable var22) {
         }

         try {
            Thread.sleep(1000L);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   private static double parseMicros(String s) {
      try {
         return Double.parseDouble(s.trim()) / 1000000.0;
      } catch (Throwable t) {
         return 0.0;
      }
   }

   private static String cap(String s) {
      if (s != null && !s.isEmpty()) {
         String l = s.toLowerCase(Locale.ROOT);
         if (l.startsWith("spotify")) {
            return "Spotify";
         } else if (l.startsWith("chrom")) {
            return "Chrome";
         } else {
            return l.startsWith("firefox") ? "Firefox" : Character.toUpperCase(l.charAt(0)) + l.substring(1);
         }
      } else {
         return "";
      }
   }

   private static byte[] fetchArt(String url) {
      try {
         if (url.startsWith("file://")) {
            return Files.readAllBytes(Path.of(URI.create(url)));
         }

         if (url.startsWith("http://") || url.startsWith("https://")) {
            HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6L)).GET().build(), BodyHandlers.ofByteArray());
            if (r.statusCode() == 200 && r.body().length > 64) {
               return r.body();
            }
         }
      } catch (Throwable var2) {
      }

      return null;
   }

   private static void runMac() {
      String lastKey = "";
      int id = 0;
      String lastArtHash = "";
      boolean warned = false;

      while (wanted && !Thread.currentThread().isInterrupted()) {
         try {
            Process p = new ProcessBuilder("nowplaying-cli", "get", "title", "artist", "album", "duration", "elapsedTime", "playbackRate", "artworkData")
               .redirectErrorStream(false)
               .redirectError(Redirect.DISCARD)
               .start();
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));

            String l;
            try {
               while ((l = br.readLine()) != null) {
                  lines.add(l);
               }
            } catch (Throwable var23) {
               try {
                  br.close();
               } catch (Throwable var21) {
                  var23.addSuppressed(var21);
               }

               throw var23;
            }

            br.close();
            p.waitFor(3L, TimeUnit.SECONDS);
            if (lines.size() >= 6 && !"(null)".equals(lines.get(0))) {
               String title = nn(lines.get(0));
               l = nn(lines.get(1));
               String album = nn(lines.get(2));
               double dur = num(lines.get(3));
               double pos = num(lines.get(4));
               double rate = num(lines.get(5));
               String key = title + "|" + l + "|" + album;
               boolean changed = !key.equals(lastKey);
               if (changed) {
                  lastKey = key;
                  id++;
                  lastArtHash = "";
               }

               byte[] art = null;
               if (lines.size() >= 7 && !"(null)".equals(lines.get(6)) && !lines.get(6).isBlank()) {
                  String h = Integer.toString(lines.get(6).hashCode());
                  if (!h.equals(lastArtHash)) {
                     try {
                        art = Base64.getDecoder().decode(lines.get(6).trim());
                        lastArtHash = h;
                     } catch (Throwable var22) {
                     }
                  }
               }

               apply(true, rate > 0.0, title, l, album, "", pos, dur, art, changed);
            } else {
               apply(false, false, "", "", "", "", 0.0, 0.0, null, false);
            }
         } catch (IOException e) {
            if (!warned) {
               fail("nowplaying-cli not found");
               warned = true;
            }
         } catch (InterruptedException e) {
            return;
         } catch (Throwable var26) {
         }

         try {
            Thread.sleep(1000L);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   private static String nn(String s) {
      return s != null && !"(null)".equals(s) ? s : "";
   }

   private static double num(String s) {
      try {
         return Double.parseDouble(s.trim());
      } catch (Throwable t) {
         return 0.0;
      }
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         Process p = proc;
         if (p != null) {
            p.destroyForcibly();
         }
      }, "Clarity-MediaBridge-Shutdown"));
   }

   public static final class Snapshot {
      public final boolean active;
      public final boolean playing;
      public final String title;
      public final String artist;
      public final String album;
      public final String app;
      public final double posSec;
      public final double durSec;
      public final long stampNanos;
      public final byte[] art;
      public final int artVersion;
      public final String error;

      Snapshot(boolean active, boolean playing, String title, String artist, String album, String app, double posSec, double durSec, long stampNanos, byte[] art, int artVersion, String error) {
         this.active = active;
         this.playing = playing;
         this.title = title;
         this.artist = artist;
         this.album = album;
         this.app = app;
         this.posSec = posSec;
         this.durSec = durSec;
         this.stampNanos = stampNanos;
         this.art = art;
         this.artVersion = artVersion;
         this.error = error;
      }

      public double position() {
         double p = this.posSec;
         if (this.playing) {
            p += (System.nanoTime() - this.stampNanos) / 1.0E9;
         }

         if (this.durSec > 0.0) {
            p = Math.min(p, this.durSec);
         }

         return Math.max(0.0, p);
      }

      public double remaining() {
         return this.durSec > 0.0 ? Math.max(0.0, this.durSec - this.position()) : -1.0;
      }
   }
}
