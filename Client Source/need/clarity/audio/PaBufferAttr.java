package need.clarity.audio;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"maxlength", "tlength", "prebuf", "minreq", "fragsize"})
public class PaBufferAttr extends Structure {
   public int maxlength = -1;
   public int tlength = -1;
   public int prebuf = -1;
   public int minreq = -1;
   public int fragsize = -1;
}
