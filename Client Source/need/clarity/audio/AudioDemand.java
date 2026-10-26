package need.clarity.audio;

import java.util.Arrays;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;

public final class AudioDemand {
   private static float wantedSensitivity = 0.0F;
   private static boolean wantedThisTick = false;

   private AudioDemand() {
   }

   public static void want(float sensitivity) {
      wantedThisTick = true;
      wantedSensitivity = Math.max(wantedSensitivity, sensitivity);
   }

   public static void endTick() {
      if (wantedThisTick) {
         BeatAnalyzer.setSensitivity(wantedSensitivity <= 0.0F ? 1.0F : wantedSensitivity);
         Object dev = ClarityConfig.getSetting("Music Beats", "Audio Device");
         BeatAnalyzer.start(dev == null ? "" : String.valueOf(dev));
      } else {
         BeatAnalyzer.stop();
      }

      wantedThisTick = false;
      wantedSensitivity = 0.0F;
   }

   public static boolean musicBeatsOn() {
      ModuleBase mb = ModuleRegistry.get("Music Beats");
      return mb != null && mb.isEnabled();
   }

   private static float approach(float cur, float target, float dt, float rise, float fall) {
      float tau = target > cur ? rise : fall;
      return cur + (target - cur) * (1.0F - (float)Math.exp(-dt / tau));
   }

   public static float envelope(float age) {
      if (age < 0.0F) {
         return 0.0F;
      }

      float in = Math.min(1.0F, age / 0.1F);
      in = in * in * (3.0F - 2.0F * in);
      return in * (float)Math.exp(-Math.max(0.0F, age - 0.1F) * 2.4F);
   }

   public static void sample(AudioDemand.Frame f, double clockSeconds, float idleBeatSeconds) {
      long now = System.nanoTime();
      float dt = f.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - f.lastNanos) / 1.0E9F);
      f.lastNanos = now;
      float rawBass;
      if (BeatAnalyzer.hasAudio()) {
         BeatAnalyzer.copyBands(f.raw, f.peaks);
         BeatAnalyzer.beatAges(f.beatAges, now);
         BeatAnalyzer.beatIds(f.beatIds);
         rawBass = BeatAnalyzer.bass();
         f.live = true;
      } else {
         float t = (float)clockSeconds;

         for (int i = 0; i < f.raw.length; i++) {
            f.raw[i] = 0.1F + 0.07F * (float)Math.sin(t * 1.6 + i * 0.45) + 0.04F * (float)Math.sin(t * 0.7 - i * 0.2);
            f.peaks[i] = f.raw[i];
         }

         if (!(idleBeatSeconds > 0.0F)) {
            Arrays.fill(f.beatAges, -1.0F);
            Arrays.fill(f.beatIds, 0.0F);
            rawBass = 0.15F + 0.05F * (float)Math.sin(t * 1.3);
         } else {
            double period = idleBeatSeconds;
            long n = (long)Math.floor(clockSeconds / period);

            for (int i = 0; i < 4; i++) {
               long k = n - i;
               f.beatAges[i] = k < 0L ? -1.0F : (float)(clockSeconds - k * period);
               f.beatIds[i] = k < 0L ? 0.0F : (float)(k % 65536L);
            }

            float age = f.beatAges[0] < 0.0F ? 9.0F : f.beatAges[0];
            rawBass = 0.12F + 0.4F * envelope(age);
         }

         f.live = false;
      }

      boolean first = dt == 0.0F;
      float r = Math.max(0.0F, Math.min(1.5F, f.reactivity));
      float lvl = 0.0F;
      float tr = 0.0F;

      for (int i = 0; i < f.raw.length; i++) {
         f.smooth[i] = first ? f.raw[i] : approach(f.smooth[i], f.raw[i], dt, 0.1F, 0.45F);
         f.bands[i] = Math.min(1.0F, f.smooth[i] * r);
         lvl += f.bands[i];
         if (i >= 22) {
            tr += f.bands[i];
         }
      }

      f.sBass = first ? rawBass : approach(f.sBass, rawBass, dt, 0.12F, 0.5F);
      f.bass = Math.min(1.0F, f.sBass * r);
      f.level = lvl / f.bands.length;
      f.treble = tr / 10.0F;
      float env = envelope(f.beatAges[0]);
      f.sFlash = first ? env : approach(f.sFlash, env, dt, 0.05F, 0.25F);
      f.flash = Math.min(1.0F, f.sFlash * r);
      f.phase = f.phase + dt * (0.5 + 0.9 * f.bass);
   }

   public static final class Frame {
      public final float[] bands = new float[32];
      public final float[] peaks = new float[32];
      public final float[] beatAges = new float[4];
      public final float[] beatIds = new float[4];
      public float bass;
      public float level;
      public float treble;
      public float flash;
      public double phase;
      public boolean live;
      public float reactivity = 1.0F;
      private final float[] raw = new float[32];
      private final float[] smooth = new float[32];
      private float sBass;
      private float sFlash;
      private long lastNanos;
   }
}
