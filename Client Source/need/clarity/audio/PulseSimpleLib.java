package need.clarity.audio;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface PulseSimpleLib extends Library {
   int PA_STREAM_RECORD = 2;
   int PA_SAMPLE_S16LE = 3;

   Pointer pa_simple_new(String var1, String var2, int var3, String var4, String var5, PaSampleSpec var6, Pointer var7, PaBufferAttr var8, IntByReference var9);

   int pa_simple_read(Pointer var1, byte[] var2, NativeLong var3, IntByReference var4);

   void pa_simple_free(Pointer var1);
}
