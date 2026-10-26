package need.clarity.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import need.clarity.Clarity;
import need.clarity.config.ConfigFiles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

public final class CustomSounds {
   public static final String PREFIX = "custom:";
   public static final String OPEN_FOLDER = "Open sounds folder…";
   private static final String[] EXT = new String[]{".ogg", ".oga", ".wav", ".wave", ".aif", ".aiff", ".au", ".mp3"};
   private static List<String> cachedList = List.of();
   private static long listedAt = 0L;
   private static final Map<String, CustomSounds.Pcm> CACHE = new HashMap<>();
   private static final Set<String> FAILED = new HashSet<>();
   private static final List<int[]> PLAYING = new ArrayList<>();

   private CustomSounds() {
   }

   public static Path folder() {
      Path d = FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("sounds");

      try {
         Files.createDirectories(d);
      } catch (Exception var2) {
      }

      return d;
   }

   public static boolean supported(Path p) {
      String n = p.getFileName().toString().toLowerCase(Locale.ROOT);

      for (String e : EXT) {
         if (n.endsWith(e)) {
            return true;
         }
      }

      return false;
   }

   public static List<String> options() {
      long now = System.currentTimeMillis();
      if (now - listedAt < 2000L) {
         return cachedList;
      }

      listedAt = now;
      Path dir = folder();
      List<String> out = new ArrayList<>();

      try (Stream<Path> s = Files.walk(dir, 2)) {
         s.filter(x$0 -> Files.isRegularFile(x$0)).filter(CustomSounds::supported).forEach(p -> out.add("custom:" + dir.relativize(p).toString().replace('\\', '/')));
      } catch (Exception var9) {
      }

      out.sort(String.CASE_INSENSITIVE_ORDER);
      cachedList = out;
      return out;
   }

   public static boolean isCustom(String value) {
      return value != null && value.startsWith("custom:");
   }

   public static String label(String value) {
      String f = value.substring("custom:".length());
      int slash = f.lastIndexOf(47);
      return "♪ " + (slash >= 0 ? f.substring(slash + 1) : f);
   }

   public static void openFolder() {
      try {
         ConfigFiles.openFolder(folder(), ok -> {});
      } catch (Throwable var1) {
      }
   }

   private static CustomSounds.Pcm pcm(String value) {
      String rel = value.substring("custom:".length());
      Path p = folder().resolve(rel).normalize();
      if (p.startsWith(folder()) && Files.isRegularFile(p)) {
         long mtime;
         try {
            mtime = Files.getLastModifiedTime(p).toMillis();
         } catch (Exception e) {
            return null;
         }

         synchronized (CACHE) {
            CustomSounds.Pcm c = CACHE.get(rel);
            if (c != null && c.mtime == mtime) {
               return c;
            }

            if (FAILED.contains(rel + "|" + mtime)) {
               return null;
            }
         }

         CustomSounds.Pcm out = null;

         try {
            byte[] bytes = Files.readAllBytes(p);
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".ogg") || n.endsWith(".oga")) {
               out = vorbis(bytes);
            } else if (n.endsWith(".mp3")) {
               out = CustomSounds.Mp3.decode(bytes);
            } else {
               out = javaSound(bytes);
            }
         } catch (Throwable t) {
            Clarity.LOGGER.warn("[Clarity] Couldn't read sound " + p.getFileName() + ": " + t);
         }

         synchronized (CACHE) {
            if (out != null && out.data.length != 0) {
               out.mtime = mtime;
               CACHE.put(rel, out);
               return out;
            } else {
               FAILED.add(rel + "|" + mtime);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static CustomSounds.Pcm vorbis(byte[] bytes) {
      ByteBuffer mem = MemoryUtil.memAlloc(bytes.length);

      try (MemoryStack st = MemoryStack.stackPush()) {
         mem.put(bytes).flip();
         IntBuffer ch = st.mallocInt(1);
         IntBuffer rate = st.mallocInt(1);
         ShortBuffer sb = STBVorbis.stb_vorbis_decode_memory(mem, ch, rate);
         if (sb == null) {
            return null;
         }

         try {
            CustomSounds.Pcm p = new CustomSounds.Pcm();
            p.channels = ch.get(0);
            p.rate = rate.get(0);
            p.data = new short[sb.remaining()];
            sb.get(p.data);
            return p;
         } finally {
            LibCStdlib.free(sb);
         }
      } finally {
         MemoryUtil.memFree(mem);
      }
   }

   private static CustomSounds.Pcm javaSound(byte[] bytes) throws Exception {
      AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
      AudioFormat src = in.getFormat();
      int ch = Math.max(1, Math.min(2, src.getChannels()));
      AudioFormat dst = new AudioFormat(Encoding.PCM_SIGNED, src.getSampleRate(), 16, ch, ch * 2, src.getSampleRate(), false);

      try (AudioInputStream pcm = AudioSystem.getAudioInputStream(dst, in)) {
         ByteArrayOutputStream bo = new ByteArrayOutputStream();
         pcm.transferTo(bo);
         byte[] b = bo.toByteArray();
         CustomSounds.Pcm p = new CustomSounds.Pcm();
         p.channels = ch;
         p.rate = Math.round(src.getSampleRate());
         p.data = new short[b.length / 2];

         for (int i = 0; i < p.data.length; i++) {
            p.data[i] = (short)(b[i * 2] & 0xFF | b[i * 2 + 1] << 8);
         }

         return p;
      }
   }

   public static boolean play(String value, float volume, float pitch) {
      if (!isCustom(value)) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return false;
      }

      CustomSounds.Pcm p = pcm(value);
      if (p == null) {
         return false;
      }

      float master = 1.0F;

      try {
         master = mc.options.getFinalSoundSourceVolume(SoundSource.MASTER);
      } catch (Throwable var8) {
      }

      float gain = Math.max(0.0F, volume) * master;
      float fp = Math.max(0.5F, Math.min(2.0F, pitch));
      if (gain <= 1.0E-4F) {
         return true;
      }

      mc.execute(() -> {
         try {
            start(p, gain, fp);
         } catch (Throwable var4x) {
         }
      });
      return true;
   }

   private static void start(CustomSounds.Pcm p, float gain, float pitch) {
      cleanup();
      int buf = AL10.alGenBuffers();
      ShortBuffer sb = MemoryUtil.memAllocShort(p.data.length);

      try {
         sb.put(p.data).flip();
         AL10.alBufferData(buf, p.channels == 2 ? 4355 : 4353, sb, p.rate);
      } finally {
         MemoryUtil.memFree(sb);
      }

      int src = AL10.alGenSources();
      AL10.alSourcei(src, 4105, buf);
      AL10.alSourcei(src, 514, 1);
      AL10.alSource3f(src, 4100, 0.0F, 0.0F, 0.0F);
      AL10.alSourcef(src, 4106, Math.min(4.0F, gain));
      AL10.alSourcef(src, 4099, pitch);
      AL10.alSourcePlay(src);
      synchronized (PLAYING) {
         PLAYING.add(new int[]{src, buf});
      }
   }

   private static void cleanup() {
      synchronized (PLAYING) {
         Iterator<int[]> it = PLAYING.iterator();

         while (it.hasNext()) {
            int[] e = it.next();
            boolean done = AL10.alGetSourcei(e[0], 4112) != 4114 || PLAYING.size() > 24;
            if (done) {
               AL10.alSourceStop(e[0]);
               AL10.alDeleteSources(e[0]);
               AL10.alDeleteBuffers(e[1]);
               it.remove();
            }
         }
      }
   }

   private static final class Mp3 {
      static CustomSounds.Pcm decode(byte[] bytes) throws Exception {
         Bitstream bs = new Bitstream(new ByteArrayInputStream(bytes));
         Decoder dec = new Decoder();
         short[] all = new short[65536];
         int n = 0;
         int channels = 0;
         int rate = 0;

         Header h;
         try {
            while ((h = bs.readFrame()) != null) {
               SampleBuffer out = (SampleBuffer)dec.decodeFrame(h, bs);
               channels = out.getChannelCount();
               rate = out.getSampleFrequency();
               int len = out.getBufferLength();
               if (n + len > all.length) {
                  all = Arrays.copyOf(all, Math.max(all.length * 2, n + len));
               }

               System.arraycopy(out.getBuffer(), 0, all, n, len);
               n += len;
               bs.closeFrame();
               if (n > 57600000) {
                  break;
               }
            }
         } finally {
            try {
               bs.close();
            } catch (Throwable var15) {
            }
         }

         if (channels != 0 && n != 0) {
            CustomSounds.Pcm p = new CustomSounds.Pcm();
            p.channels = channels;
            p.rate = rate;
            p.data = Arrays.copyOf(all, n);
            return p;
         } else {
            return null;
         }
      }
   }

   private static final class Pcm {
      short[] data;
      int channels;
      int rate;
      long mtime;
   }
}
