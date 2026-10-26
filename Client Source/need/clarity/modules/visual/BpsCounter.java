package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class BpsCounter extends ModuleBase {
   public static BpsCounter INSTANCE;
   public static final int HISTORY = 200;
   private final float[] history = new float[200];
   private int head = 0;
   private int filled = 0;
   private final float[] window = new float[20];
   private int wHead = 0;
   private int wFilled = 0;
   private float bps = 0.0F;
   private float peak = 0.0F;
   private double lx = Double.NaN;
   private double ly;
   private double lz;

   public BpsCounter() {
      super("BPS", "HUD");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   @Override
   public void onEnable() {
      this.head = this.filled = this.wHead = this.wFilled = 0;
      this.bps = this.peak = 0.0F;
      this.lx = Double.NaN;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p == null) {
         this.lx = Double.NaN;
      } else {
         double x = p.getX();
         double y = p.getY();
         double z = p.getZ();
         float d = 0.0F;
         if (!Double.isNaN(this.lx)) {
            double dx = x - this.lx;
            double dy = this.vertical() ? y - this.ly : 0.0;
            double dz = z - this.lz;
            d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > 10.0F) {
               d = 0.0F;
            }
         }

         this.lx = x;
         this.ly = y;
         this.lz = z;
         float inst = d * 20.0F;
         int n = Math.max(1, Math.min(20, this.smoothing()));
         this.window[this.wHead] = inst;
         this.wHead = (this.wHead + 1) % 20;
         this.wFilled = Math.min(20, this.wFilled + 1);
         float sum = 0.0F;
         int k = Math.min(n, this.wFilled);

         for (int i = 0; i < k; i++) {
            sum += this.window[(this.wHead - 1 - i + 20) % 20];
         }

         this.bps = k == 0 ? 0.0F : sum / k;
         this.history[this.head] = this.bps;
         this.head = (this.head + 1) % 200;
         this.filled = Math.min(200, this.filled + 1);
         this.peak = 0.0F;
         int len = this.graphLength();

         for (int i = 0; i < Math.min(len, this.filled); i++) {
            this.peak = Math.max(this.peak, this.history[(this.head - 1 - i + 200) % 200]);
         }
      }
   }

   public float bps() {
      return this.bps;
   }

   public float peak() {
      return this.peak;
   }

   public float sample(int i) {
      return i < this.filled ? this.history[(this.head - 1 - i + 200) % 200] : 0.0F;
   }

   public int samples() {
      return this.filled;
   }

   public String anchor() {
      return this.getSetting("Anchor", "Top Left");
   }

   public int offsetX() {
      return (int)this.getSetting("X Offset", 12.0F).floatValue();
   }

   public int offsetY() {
      return (int)this.getSetting("Y Offset", 120.0F).floatValue();
   }

   public float scale() {
      return clamp(this.getSetting("Scale", 1.0F), 0.5F, 3.0F);
   }

   public float opacity() {
      return clamp(this.getSetting("Opacity", 1.0F), 0.1F, 1.0F);
   }

   public boolean vertical() {
      return this.getSetting("Count Vertical", false);
   }

   public int smoothing() {
      return (int)this.getSetting("Smoothing", 5.0F).floatValue();
   }

   public int decimals() {
      return (int)clamp(this.getSetting("Decimals", 1.0F), 0.0F, 3.0F);
   }

   public boolean label() {
      return this.getSetting("Label", true);
   }

   public String labelText() {
      return this.getSetting("Label Text", "BPS");
   }

   public boolean graph() {
      return this.getSetting("Graph", true);
   }

   public int graphLength() {
      return (int)clamp(this.getSetting("Graph Length", 100.0F), 20.0F, 200.0F);
   }

   public float graphWidth() {
      return clamp(this.getSetting("Graph Width", 120.0F), 40.0F, 400.0F);
   }

   public float graphHeight() {
      return clamp(this.getSetting("Graph Height", 34.0F), 12.0F, 160.0F);
   }

   public boolean graphFill() {
      return this.getSetting("Graph Fill", true);
   }

   public boolean showPeak() {
      return this.getSetting("Show Peak", true);
   }

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public float bgOpacity() {
      return clamp(this.getSetting("Background Opacity", 0.92F), 0.0F, 1.0F);
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean blur() {
      return this.getSetting("Blur", false);
   }

   public int blurStrength() {
      return (int)clamp(this.getSetting("Blur Strength", 2.0F), 1.0F, 4.0F);
   }

   public float blurTint() {
      return clamp(this.getSetting("Blur Tint", 0.45F), 0.0F, 1.0F);
   }

   public float cornerRadius() {
      return clamp(this.getSetting("Corner Radius", 8.0F), 0.0F, 20.0F);
   }

   public boolean themeColors() {
      return this.getSetting("Theme Colors", true);
   }

   public Object textColor() {
      return this.getSetting("Text Color", "#EDEDF0");
   }

   public Object graphColor() {
      return this.getSetting("Graph Color", "#7AA2F7");
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
