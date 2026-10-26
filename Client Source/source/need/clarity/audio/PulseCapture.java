package need.clarity.audio;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public final class PulseCapture {
   public static final String MONITOR = "@DEFAULT_MONITOR@";
   private static volatile Boolean available = null;
   private static PulseSimpleLib lib;
   private final Pointer handle;
   private final IntByReference err = new IntByReference();

   private PulseCapture(Pointer handle) {
      this.handle = handle;
   }

   public static synchronized boolean available() {
      if (available != null) {
         return available;
      }

      String os = System.getProperty("os.name", "").toLowerCase();
      if (!os.contains("linux") && !os.contains("bsd")) {
         available = false;
         return false;
      }

      for (String name : new String[]{"pulse-simple", "libpulse-simple.so.0"}) {
         try {
            lib = (PulseSimpleLib)Native.load(name, PulseSimpleLib.class);
            available = true;
            return true;
         } catch (Throwable var6) {
         }
      }

      available = false;
      return false;
   }

   public static PulseCapture open(String device, int rate, int channels, int fragmentBytes) {
      if (!available()) {
         return null;
      }

      PaSampleSpec ss = new PaSampleSpec();
      ss.format = 3;
      ss.rate = rate;
      ss.channels = (byte)channels;
      PaBufferAttr attr = new PaBufferAttr();
      attr.fragsize = fragmentBytes;
      IntByReference error = new IntByReference();
      Pointer h = lib.pa_simple_new(null, "Minecraft (Clarity Music Beats)", 2, device, "Music Beats", ss, null, attr, error);
      return h == null ? null : new PulseCapture(h);
   }

   public boolean read(byte[] buf) {
      return lib.pa_simple_read(this.handle, buf, new NativeLong(buf.length), this.err) >= 0;
   }

   public void close() {
      try {
         lib.pa_simple_free(this.handle);
      } catch (Throwable var2) {
      }
   }
}
