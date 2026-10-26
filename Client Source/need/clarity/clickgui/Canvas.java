package need.clarity.clickgui;

public interface Canvas {
   int LEFT = 1;
   int CENTER = 2;
   int RIGHT = 4;
   int TOP = 8;
   int MIDDLE = 16;
   int BASELINE = 64;
   String REGULAR = "inter";
   String MEDIUM = "inter-med";
   String SEMIBOLD = "inter-sb";

   void save();

   void restore();

   void translate(float var1, float var2);

   void scale(float var1);

   void alpha(float var1);

   void scissor(float var1, float var2, float var3, float var4);

   void rect(float var1, float var2, float var3, float var4, float var5, int var6);

   void gradient(float var1, float var2, float var3, float var4, float var5, int var6, int var7, boolean var8);

   void stroke(float var1, float var2, float var3, float var4, float var5, float var6, int var7);

   void strokeGradient(float var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8);

   void shadow(float var1, float var2, float var3, float var4, float var5, float var6, int var7);

   void glass(float var1, float var2, float var3, float var4, float var5, float var6);

   void circle(float var1, float var2, float var3, int var4);

   void ring(float var1, float var2, float var3, float var4, int var5);

   void arc(float var1, float var2, float var3, float var4, float var5, float var6, int var7);

   void line(float var1, float var2, float var3, float var4, float var5, int var6);

   void glow(float var1, float var2, float var3, int var4);

   void image(String var1, float var2, float var3, float var4, float var5, float var6);

   float text(String var1, float var2, float var3, float var4, int var5, int var6, String var7);

   float textWidth(String var1, float var2, String var3);

   default void rotate(float radians) {
   }

   default void scale2(float sx, float sy) {
      this.scale((sx + sy) / 2.0F);
   }

   default boolean logo(float x, float y, float size, int tint, float alpha) {
      return false;
   }

   default boolean hasSnapshot() {
      return false;
   }

   default void snapPiece(float[] xy, float sw, float sh, float alpha) {
   }

   default void snapHole(float cx, float cy, float r, float sw, float sh, float alpha) {
   }

   default void strokePoly(float[] xy, float width, int argb) {
   }

   default boolean itemIcon(String id, float x, float y, float size, float clipX, float clipY, float clipW, float clipH) {
      return false;
   }

   default void fillPoly(float[] xy, int argb) {
   }

   default void setFontFamily(String family) {
   }

   default boolean imageBytes(String key, byte[] png, float x, float y, float w, float h, float radius, float alpha) {
      return false;
   }
}
