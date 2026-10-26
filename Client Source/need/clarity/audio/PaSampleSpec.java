package need.clarity.audio;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"format", "rate", "channels"})
public class PaSampleSpec extends Structure {
   public int format;
   public int rate;
   public byte channels;
}
