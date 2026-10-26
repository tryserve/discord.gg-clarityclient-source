package need.clarity.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Mixer.Info;

public final class BeatAnalyzer {
   public static final int BANDS = 32;
   public static final String DESKTOP = "Desktop Audio";
   private static final int N = 1024;
   private static final int HOP = 512;
   private static final float RATE = 44100.0F;
   private static final String[] LOOPBACK_HINTS = new String[]{"monitor", "stereo mix", "loopback", "what u hear", "wave out", "cable output", "blackhole", "soundflower", "mix"};
   private static final float[] bands = new float[32];
   private static final float[] peaks = new float[32];
   private static volatile float bass = 0.0F;
   private static final long[] beatTimes = new long[4];
   private static final long[] beatNums = new long[4];
   private static int beatIdx = 0;
   private static long beatCount = 0L;
   private static volatile boolean running = false;
   private static volatile boolean hasAudio = false;
   private static volatile String deviceName = "";
   private static volatile float sensitivity = 1.0F;
   private static Thread thread;
   private static String wantedDevice = null;

   private BeatAnalyzer() {
   }

   public static synchronized void start(String device) {
      String dev = device == null ? "" : device.trim();
      if (!running || !dev.equals(wantedDevice)) {
         stop();
         wantedDevice = dev;
         running = true;
         thread = new Thread(() -> loop(dev), "clarity-music-beats");
         thread.setDaemon(true);
         thread.start();
      }
   }

   public static synchronized void stop() {
      running = false;
      if (thread != null) {
         thread.interrupt();
         thread = null;
      }

      hasAudio = false;
   }

   public static void setSensitivity(float s) {
      sensitivity = Math.max(0.1F, s);
   }

   public static boolean hasAudio() {
      return hasAudio;
   }

   public static String device() {
      return deviceName;
   }

   public static float bass() {
      return bass;
   }

   public static void copyBands(float[] outBands, float[] outPeaks) {
      synchronized (bands) {
         System.arraycopy(bands, 0, outBands, 0, 32);
         System.arraycopy(peaks, 0, outPeaks, 0, 32);
      }
   }

   public static void beatAges(float[] out, long nowNanos) {
      synchronized (beatTimes) {
         for (int i = 0; i < 4; i++) {
            long t = beatTimes[(beatIdx - 1 - i + 8) % 4];
            out[i] = t == 0L ? -1.0F : (float)((nowNanos - t) / 1.0E9);
         }
      }
   }

   public static void beatIds(float[] out) {
      synchronized (beatTimes) {
         for (int i = 0; i < 4; i++) {
            out[i] = (float)(beatNums[(beatIdx - 1 - i + 8) % 4] % 65536L);
         }
      }
   }

   public static long beatCount() {
      synchronized (beatTimes) {
         return beatCount;
      }
   }

   private static void loop(String device) {
      TargetDataLine line = null;
      PulseCapture pulse = null;

      try {
         boolean wantDesktop = device.isEmpty() || device.equalsIgnoreCase("auto") || device.equalsIgnoreCase("Desktop Audio");
         if (wantDesktop) {
            pulse = PulseCapture.open("@DEFAULT_MONITOR@", 44100, 2, 2048);
            if (pulse != null) {
               deviceName = "Desktop Audio (what your speakers play)";
            } else if (device.equalsIgnoreCase("Desktop Audio")) {
            }
         }

         if (pulse == null) {
            line = open(device.equalsIgnoreCase("Desktop Audio") ? "" : device);
         }

         if (pulse != null || line != null) {
            int ch = pulse != null ? 2 : line.getFormat().getChannels();
            byte[] buf = new byte[1024 * ch];
            float[] ring = new float[1024];
            float[] re = new float[1024];
            float[] im = new float[1024];
            float[] win = new float[1024];

            for (int i = 0; i < 1024; i++) {
               win[i] = (float)(0.5 - 0.5 * Math.cos((Math.PI * 2) * i / 1023.0));
            }

            float[] gain = new float[32];
            Arrays.fill(gain, 0.001F);
            float bassAvg = 0.0F;
            float bassMax = 0.001F;
            long lastBeat = 0L;
            int filled = 0;
            hasAudio = true;

            while (running && !Thread.currentThread().isInterrupted()) {
               int got;
               if (pulse != null) {
                  got = pulse.read(buf) ? buf.length : -1;
               } else {
                  got = line.read(buf, 0, buf.length);
               }

               if (got < 0) {
                  return;
               }

               if (got > 0) {
                  int frames = got / (2 * ch);
                  System.arraycopy(ring, frames, ring, 0, 1024 - frames);

                  for (int f = 0; f < frames; f++) {
                     float s = 0.0F;

                     for (int c = 0; c < ch; c++) {
                        int idx = (f * ch + c) * 2;
                        s += (short)(buf[idx + 1] << 8 | buf[idx] & 0xFF) / 32768.0F;
                     }

                     ring[1024 - frames + f] = s / ch;
                  }

                  filled = Math.min(1024, filled + frames);
                  if (filled >= 1024) {
                     for (int i = 0; i < 1024; i++) {
                        re[i] = ring[i] * win[i];
                        im[i] = 0.0F;
                     }

                     fft(re, im);
                     float[] lvl = new float[32];
                     float bassE = 0.0F;

                     for (int b = 0; b < 32; b++) {
                        double f0 = 40.0 * Math.pow(400.0, b / 32.0);
                        double f1 = 40.0 * Math.pow(400.0, (b + 1) / 32.0);
                        int k0 = Math.max(1, (int)(f0 * 1024.0 / 44100.0));
                        int k1 = Math.max(k0 + 1, (int)(f1 * 1024.0 / 44100.0));
                        float e = 0.0F;

                        for (int k = k0; k < k1 && k < 512; k++) {
                           e = Math.max(e, re[k] * re[k] + im[k] * im[k]);
                        }

                        float mag = (float)Math.sqrt(e);
                        gain[b] = Math.max(Math.max(mag, 0.05F), gain[b] * 0.9985F);
                        float norm = mag / Math.max(gain[b], 1.0E-4F);
                        lvl[b] = (float)Math.pow(Math.min(1.0F, norm), 0.8) * Math.min(1.0F, gain[b] * 400.0F);
                        if (f1 < 160.0) {
                           bassE += mag;
                        }
                     }

                     float sens = sensitivity;
                     synchronized (bands) {
                        for (int b = 0; b < 32; b++) {
                           float v = Math.min(1.0F, lvl[b] * sens);
                           bands[b] = v > bands[b] ? bands[b] + (v - bands[b]) * 0.65F : bands[b] * 0.82F;
                           peaks[b] = Math.max(bands[b], peaks[b] - 0.012F);
                        }
                     }

                     bassMax = Math.max(bassE, bassMax * 0.999F);
                     bassAvg = bassAvg * 0.94F + bassE * 0.06F;
                     float bn = bassE / Math.max(bassMax, 1.0E-4F);
                     bass = bass * 0.7F + Math.min(1.0F, bn * sens) * 0.3F;
                     long now = System.nanoTime();
                     boolean loudEnough = bassE > 0.8F;
                     if (loudEnough && bassE > bassAvg * (1.35F / Math.max(0.3F, sens)) && bn > 0.3F && now - lastBeat > 180000000L) {
                        lastBeat = now;
                        synchronized (beatTimes) {
                           beatTimes[beatIdx] = now;
                           beatNums[beatIdx] = ++beatCount;
                           beatIdx = (beatIdx + 1) % 4;
                        }
                     }
                  }
               }
            }

            return;
         }
      } catch (Throwable var45) {
         return;
      } finally {
         hasAudio = false;
         if (line != null) {
            try {
               line.stop();
               line.close();
            } catch (Throwable var42) {
            }
         }

         if (pulse != null) {
            pulse.close();
         }
      }
   }

   private static TargetDataLine open(String wanted) throws Exception {
      AudioFormat[] formats = new AudioFormat[]{new AudioFormat(44100.0F, 16, 2, true, false), new AudioFormat(44100.0F, 16, 1, true, false)};
      List<Info> candidates = new ArrayList<>();

      for (Info mi : AudioSystem.getMixerInfo()) {
         Mixer m = AudioSystem.getMixer(mi);

         for (AudioFormat fmt : formats) {
            if (m.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, fmt))) {
               candidates.add(mi);
               break;
            }
         }
      }

      Info pick = null;
      String w = wanted.toLowerCase(Locale.ROOT);
      if (!w.isEmpty() && !w.equals("auto")) {
         for (Info mi : candidates) {
            if (mi.getName().equalsIgnoreCase(wanted)) {
               pick = mi;
               break;
            }
         }

         if (pick == null) {
            for (Info mi : candidates) {
               if (label(mi).toLowerCase(Locale.ROOT).contains(w)) {
                  pick = mi;
                  break;
               }
            }
         }

         if (pick == null) {
         }
      }

      if (pick == null) {
         label72:
         for (String hint : LOOPBACK_HINTS) {
            for (Info mi : candidates) {
               if (label(mi).toLowerCase(Locale.ROOT).contains(hint)) {
                  pick = mi;
                  break label72;
               }
            }
         }
      }

      for (AudioFormat fmt : formats) {
         try {
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(TargetDataLine.class, fmt);
            TargetDataLine line = pick != null ? (TargetDataLine)AudioSystem.getMixer(pick).getLine(info) : (TargetDataLine)AudioSystem.getLine(info);
            line.open(fmt, 4096 * fmt.getChannels());
            line.start();
            deviceName = pick != null ? label(pick) : "system default input";
            return line;
         } catch (Throwable var12) {
         }
      }

      return null;
   }

   private static String label(Info mi) {
      return mi.getName() + " (" + mi.getDescription() + ")";
   }

   public static List<String> deviceNames() {
      List<String> out = new ArrayList<>();
      if (PulseCapture.available()) {
         out.add("Desktop Audio");
      }

      AudioFormat st = new AudioFormat(44100.0F, 16, 2, true, false);
      AudioFormat mono = new AudioFormat(44100.0F, 16, 1, true, false);

      for (Info mi : AudioSystem.getMixerInfo()) {
         try {
            Mixer m = AudioSystem.getMixer(mi);
            if ((m.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, st)) || m.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, mono)))
               && !out.contains(mi.getName())) {
               out.add(mi.getName());
            }
         } catch (Throwable var8) {
         }
      }

      return out;
   }

   public static String listDevices() {
      List<String> out = new ArrayList<>();
      AudioFormat fmt = new AudioFormat(44100.0F, 16, 2, true, false);

      for (Info mi : AudioSystem.getMixerInfo()) {
         Mixer m = AudioSystem.getMixer(mi);
         if (m.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, fmt))
            || m.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, new AudioFormat(44100.0F, 16, 1, true, false)))) {
            out.add(mi.getName());
         }
      }

      return out.toString();
   }

   private static void fft(float[] re, float[] im) {
      int n = re.length;
      int i = 1;
      int j = 0;

      while (i < n) {
         int bit;
         for (bit = n >> 1; (j & bit) != 0; bit >>= 1) {
            j ^= bit;
         }

         j ^= bit;
         if (i < j) {
            float t = re[i];
            re[i] = re[j];
            re[j] = t;
            t = im[i];
            im[i] = im[j];
            im[j] = t;
         }

         i++;
      }

      for (int len = 2; len <= n; len <<= 1) {
         double ang = (-Math.PI * 2) / len;
         float wr = (float)Math.cos(ang);
         float wi = (float)Math.sin(ang);

         for (int ix = 0; ix < n; ix += len) {
            float cr = 1.0F;
            float ci = 0.0F;

            for (int jx = 0; jx < len / 2; jx++) {
               int a = ix + jx;
               int b = a + len / 2;
               float xr = re[b] * cr - im[b] * ci;
               float xi = re[b] * ci + im[b] * cr;
               re[b] = re[a] - xr;
               im[b] = im[a] - xi;
               re[a] += xr;
               im[a] += xi;
               float ncr = cr * wr - ci * wi;
               ci = cr * wi + ci * wr;
               cr = ncr;
            }
         }
      }
   }
}
