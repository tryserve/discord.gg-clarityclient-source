package need.clarity.modules.visual.wings;

public enum WingStyle {
   ANGEL("Angel", 0.6),
   DRAGON("Dragon", 0.64),
   BUTTERFLY("Butterfly", 1.05),
   PHOENIX("Phoenix", 0.42),
   CRYSTAL("Crystal", 0.62),
   CYBER("Cyber", 0.52),
   FAIRY("Fairy", 0.66),
   CLASSIC("Classic", 1.0);

   public final String label;
   public final double unit;

   WingStyle(String label, double unit) {
      this.label = label;
      this.unit = unit;
   }

   public static WingStyle fromName(String name) {
      if (name != null) {
         for (WingStyle s : values()) {
            if (s.label.equalsIgnoreCase(name)) {
               return s;
            }
         }
      }

      return ANGEL;
   }

   public void build(WingBuilder b) {
      switch (this) {
         case ANGEL:
            WingStyles.angel(b);
            break;
         case DRAGON:
            WingStyles.dragon(b);
            break;
         case BUTTERFLY:
            WingStyles.butterfly(b);
            break;
         case PHOENIX:
            WingStyles.phoenix(b);
            break;
         case CRYSTAL:
            WingStyles.crystal(b);
            break;
         case CYBER:
            WingStyles.cyber(b);
            break;
         case FAIRY:
            WingStyles.fairy(b);
            break;
         case CLASSIC:
            WingStyles.classic(b);
      }
   }
}
