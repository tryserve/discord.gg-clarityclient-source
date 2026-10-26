package need.clarity.clickgui;

final class Icons {
   private Icons() {
   }

   static void draw(Canvas c, String name, float cx, float cy, float s, int col) {
      float w = Math.max(1.2F, s * 0.11F);
      switch (name) {
         case "Combat":
            c.ring(cx, cy, s * 0.36F, w, col);
            c.line(cx - s * 0.5F, cy, cx - s * 0.18F, cy, w, col);
            c.line(cx + s * 0.18F, cy, cx + s * 0.5F, cy, w, col);
            c.line(cx, cy - s * 0.5F, cx, cy - s * 0.18F, w, col);
            c.line(cx, cy + s * 0.18F, cx, cy + s * 0.5F, w, col);
            c.circle(cx, cy, s * 0.07F, col);
            break;
         case "Utility":
            c.ring(cx, cy, s * 0.22F, w, col);

            for (int i = 0; i < 8; i++) {
               double a = i * Math.PI / 4.0;
               c.line(cx + (float)Math.cos(a) * s * 0.3F, cy + (float)Math.sin(a) * s * 0.3F, cx + (float)Math.cos(a) * s * 0.44F, cy + (float)Math.sin(a) * s * 0.44F, w * 1.3F, col);
            }
            break;
         case "Movement":
            for (int k = 0; k < 2; k++) {
               float ox = (k - 0.5F) * s * 0.28F;
               c.line(cx + ox - s * 0.14F, cy - s * 0.3F, cx + ox + s * 0.14F, cy, w, col);
               c.line(cx + ox + s * 0.14F, cy, cx + ox - s * 0.14F, cy + s * 0.3F, w, col);
            }
            break;
         case "Visual":
            c.arc(cx, cy + s * 0.32F, s * 0.48F, -2.576106F, -0.56548667F, w, col);
            c.arc(cx, cy - s * 0.32F, s * 0.48F, 0.56548667F, 2.576106F, w, col);
            c.circle(cx, cy, s * 0.14F, col);
            break;
         case "HUD":
            c.stroke(cx - s * 0.4F, cy - s * 0.3F, s * 0.8F, s * 0.6F, s * 0.1F, w, col);
            c.line(cx - s * 0.24F, cy + s * 0.14F, cx + s * 0.24F, cy + s * 0.14F, w * 1.2F, col);
            c.rect(cx - s * 0.26F, cy - s * 0.18F, s * 0.2F, s * 0.1F, s * 0.03F, Theme.a(col, 0.75F));
            break;
         case "Themes":
            c.circle(cx - s * 0.18F, cy - s * 0.12F, s * 0.13F, col);
            c.circle(cx + s * 0.18F, cy - s * 0.12F, s * 0.13F, Theme.a(col, 0.7F));
            c.circle(cx, cy + s * 0.2F, s * 0.13F, Theme.a(col, 0.45F));
            break;
         case "Profiles":
            c.stroke(cx - s * 0.32F, cy - s * 0.28F, s * 0.64F, s * 0.22F, s * 0.06F, w, col);
            c.stroke(cx - s * 0.32F, cy - s * 0.02F, s * 0.64F, s * 0.22F, s * 0.06F, w, Theme.a(col, 0.75F));
            c.stroke(cx - s * 0.32F, cy + s * 0.24F, s * 0.64F, s * 0.08F, s * 0.04F, w, Theme.a(col, 0.5F));
            break;
         case "Binds":
            c.stroke(cx - s * 0.36F, cy - s * 0.3F, s * 0.72F, s * 0.6F, s * 0.12F, w, col);
            c.line(cx - s * 0.16F, cy + s * 0.1F, cx + s * 0.16F, cy + s * 0.1F, w, col);
            break;
         case "Servers":
            c.ring(cx, cy, s * 0.38F, w, col);
            c.line(cx - s * 0.38F, cy, cx + s * 0.38F, cy, w, col);
            c.arc(cx, cy, s * 0.38F, (float) (-Math.PI / 2), (float) (Math.PI / 2), w * 0.01F, 0);
            c.stroke(cx - s * 0.16F, cy - s * 0.38F, s * 0.32F, s * 0.76F, s * 0.16F, w, col);
            break;
         case "Friends":
            c.ring(cx - s * 0.14F, cy - s * 0.16F, s * 0.13F, w, col);
            c.arc(cx - s * 0.14F, cy + s * 0.34F, s * 0.24F, (float) -Math.PI, 0.0F, w, col);
            c.ring(cx + s * 0.2F, cy - s * 0.1F, s * 0.1F, w, Theme.a(col, 0.7F));
            c.arc(cx + s * 0.2F, cy + s * 0.34F, s * 0.19F, -1.9477874F, 0.0F, w, Theme.a(col, 0.7F));
            break;
         case "star":
            float ro = s * 0.42F;
            float ri = s * 0.18F;
            float px = 0.0F;
            float py = 0.0F;

            for (int i = 0; i <= 10; i++) {
               double a = (-Math.PI / 2) + i * Math.PI / 5.0;
               float rx = i % 2 == 0 ? ro : ri;
               float x = cx + (float)Math.cos(a) * rx;
               float y = cy + (float)Math.sin(a) * rx;
               if (i > 0) {
                  c.line(px, py, x, y, w, col);
               }

               px = x;
               py = y;
            }
            break;
         case "GUI":
         case "Client":
            for (int k = -1; k <= 1; k++) {
               float y = cy + k * s * 0.24F;
               c.line(cx - s * 0.36F, y, cx + s * 0.36F, y, w, Theme.a(col, 0.55F));
               c.circle(cx + (k == 0 ? s * 0.14F : -s * 0.16F + k * s * 0.05F), y, s * 0.1F, col);
            }
            break;
         case "search":
            c.ring(cx - s * 0.08F, cy - s * 0.08F, s * 0.26F, w, col);
            c.line(cx + s * 0.12F, cy + s * 0.12F, cx + s * 0.34F, cy + s * 0.34F, w * 1.2F, col);
            break;
         case "chevron":
            c.line(cx - s * 0.12F, cy - s * 0.24F, cx + s * 0.12F, cy, w, col);
            c.line(cx + s * 0.12F, cy, cx - s * 0.12F, cy + s * 0.24F, w, col);
            break;
         case "down":
            c.line(cx - s * 0.24F, cy - s * 0.1F, cx, cy + s * 0.14F, w, col);
            c.line(cx, cy + s * 0.14F, cx + s * 0.24F, cy - s * 0.1F, w, col);
            break;
         case "close":
            c.line(cx - s * 0.22F, cy - s * 0.22F, cx + s * 0.22F, cy + s * 0.22F, w, col);
            c.line(cx + s * 0.22F, cy - s * 0.22F, cx - s * 0.22F, cy + s * 0.22F, w, col);
            break;
         case "check":
            c.line(cx - s * 0.26F, cy, cx - s * 0.06F, cy + s * 0.2F, w * 1.3F, col);
            c.line(cx - s * 0.06F, cy + s * 0.2F, cx + s * 0.28F, cy - s * 0.2F, w * 1.3F, col);
            break;
         case "reset":
            c.arc(cx, cy, s * 0.3F, (float) (-Math.PI * 3.0 / 5.0), (float) (Math.PI * 11.0 / 10.0), w, col);
            c.line(cx - s * 0.3F, cy - s * 0.28F, cx - s * 0.12F, cy - s * 0.26F, w, col);
            break;
         case "heart": {
            float r = s * 0.17F;
            c.circle(cx - r, cy - s * 0.08F, r * 1.05F, col);
            c.circle(cx + r, cy - s * 0.08F, r * 1.05F, col);
            c.fillPoly(new float[]{cx - s * 0.36F, cy - s * 0.02F, cx + s * 0.36F, cy - s * 0.02F, cx, cy + s * 0.36F}, col);
            break;
         }
         case "heartOutline": {
            float r = s * 0.17F;
            c.arc(cx - r, cy - s * 0.08F, r, (float) (Math.PI * 3.0 / 4.0), (float) (Math.PI * 2), w, col);
            c.arc(cx + r, cy - s * 0.08F, r, (float) Math.PI, (float) (Math.PI * 9.0 / 4.0), w, col);
            c.line(cx - s * 0.33F, cy + s * 0.02F, cx, cy + s * 0.34F, w, col);
            c.line(cx + s * 0.33F, cy + s * 0.02F, cx, cy + s * 0.34F, w, col);
            break;
         }
         case "cloud":
            c.arc(cx - s * 0.16F, cy + s * 0.02F, s * 0.17F, (float) (Math.PI / 2), (float) (Math.PI * 3.0 / 2.0), w, col);
            c.arc(cx + s * 0.02F, cy - s * 0.06F, s * 0.22F, (float) Math.PI, (float) (Math.PI * 2), w, col);
            c.arc(cx + s * 0.22F, cy + s * 0.04F, s * 0.15F, (float) (Math.PI * 3.0 / 2.0), (float) (Math.PI * 5.0 / 2.0), w, col);
            c.line(cx - s * 0.16F, cy + s * 0.19F, cx + s * 0.22F, cy + s * 0.19F, w, col);
            break;
         case "download":
            c.line(cx, cy - s * 0.32F, cx, cy + s * 0.12F, w, col);
            c.line(cx - s * 0.18F, cy - s * 0.04F, cx, cy + s * 0.14F, w, col);
            c.line(cx + s * 0.18F, cy - s * 0.04F, cx, cy + s * 0.14F, w, col);
            c.line(cx - s * 0.3F, cy + s * 0.32F, cx + s * 0.3F, cy + s * 0.32F, w, col);
            break;
         case "upload":
            c.line(cx, cy + s * 0.14F, cx, cy - s * 0.3F, w, col);
            c.line(cx - s * 0.18F, cy - s * 0.12F, cx, cy - s * 0.3F, w, col);
            c.line(cx + s * 0.18F, cy - s * 0.12F, cx, cy - s * 0.3F, w, col);
            c.line(cx - s * 0.3F, cy + s * 0.32F, cx + s * 0.3F, cy + s * 0.32F, w, col);
            break;
         case "Configs":
            c.stroke(cx - s * 0.3F, cy - s * 0.36F, s * 0.6F, s * 0.72F, s * 0.08F, w, col);
            c.line(cx - s * 0.16F, cy - s * 0.14F, cx + s * 0.16F, cy - s * 0.14F, w, col);
            c.line(cx - s * 0.16F, cy + s * 0.02F, cx + s * 0.16F, cy + s * 0.02F, w, col);
            c.line(cx - s * 0.16F, cy + s * 0.18F, cx + s * 0.06F, cy + s * 0.18F, w, col);
            break;
         case "send":
            float[] tri = new float[]{cx - s * 0.36F, cy - s * 0.32F, cx + s * 0.4F, cy, cx - s * 0.36F, cy + s * 0.32F, cx - s * 0.2F, cy};
            c.fillPoly(tri, col);
            break;
         case "plus":
            c.line(cx - s * 0.3F, cy, cx + s * 0.3F, cy, w * 1.25F, col);
            c.line(cx, cy - s * 0.3F, cx, cy + s * 0.3F, w * 1.25F, col);
            break;
         case "chat":
            c.stroke(cx - s * 0.38F, cy - s * 0.3F, s * 0.76F, s * 0.5F, s * 0.14F, w, col);
            c.line(cx - s * 0.18F, cy + s * 0.2F, cx - s * 0.26F, cy + s * 0.36F, w, col);
            c.line(cx - s * 0.26F, cy + s * 0.36F, cx - s * 0.02F, cy + s * 0.2F, w, col);
            break;
         default:
            c.circle(cx, cy, s * 0.16F, col);
      }
   }
}
