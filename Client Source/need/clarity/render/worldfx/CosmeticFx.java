package need.clarity.render.worldfx;

import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.wings.WingBuilder;
import need.clarity.util.ClarityRenderUtils;
import net.minecraft.world.phys.Vec3;

public final class CosmeticFx {
   public static final String[] WING_STYLES = new String[]{"Seraph", "Void", "Circuit", "Inferno", "Prism"};
   public static final String[] HAT_STYLES = new String[]{"Divine Halo", "Orbit Rings", "Nebula Crown", "Neon Ears"};
   private static final double WING_U0 = -0.1;
   private static final double WING_U1 = 1.25;
   private static final double WING_V0 = -0.45;
   private static final double WING_V1 = 1.28;
   private static final int NU = 12;
   private static final int NV = 12;
   private static final double HAT_PAD = 1.35;
   private static final float[] gx = new float[169];
   private static final float[] gy = new float[gx.length];
   private static final float[] gz = new float[gx.length];
   private static final WingBuilder MAPPER = new WingBuilder((a, b, c, d, ca, cb, cc, cd) -> {}, 0, 0, 1.0, 0.0, false);

   private CosmeticFx() {
   }

   public static int wingStyle(String name) {
      return indexOf(WING_STYLES, name);
   }

   public static int hatStyle(String name) {
      return indexOf(HAT_STYLES, name);
   }

   public static void wings(
      int style, double[] base, double sideX, double sideZ, double fwdX, double fwdZ, double span, double roll, double fold, float[] c1, float[] c2, float opacity, boolean throughWalls, float time
   ) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      WorldFx.begin();

      for (int side = 0; side < 2; side++) {
         double sx = side == 0 ? sideX : -sideX;
         double sz = side == 0 ? sideZ : -sideZ;
         MAPPER.frame(base, sx, sz, fwdX, fwdZ, 1.0, roll, fold, side);

         for (int j = 0; j <= 12; j++) {
            for (int i = 0; i <= 12; i++) {
               double u = -0.1 + 1.35 * i / 12.0;
               double v = -0.45 + 1.73 * j / 12.0;
               double U = u * span;
               double V = (v - 0.62) * span;
               double Wb = span * (0.22 * u * u + 0.04 * u * (v - 0.62));
               double[] p = MAPPER.map(new double[]{U, V, Wb});
               int k = j * 13 + i;
               gx[k] = (float)(p[0] - cam.x);
               gy[k] = (float)(p[1] - cam.y);
               gz[k] = (float)(p[2] - cam.z);
            }
         }

         for (int j = 0; j < 12; j++) {
            for (int i = 0; i < 12; i++) {
               int k00 = j * 13 + i;
               int k10 = k00 + 1;
               int k01 = k00 + 12 + 1;
               int k11 = k01 + 1;
               float u0 = (float)(-0.1 + 1.35 * i / 12.0);
               float u1 = (float)(-0.1 + 1.35 * (i + 1) / 12.0);
               float v0 = (float)(-0.45 + 1.73 * j / 12.0);
               float v1 = (float)(-0.45 + 1.73 * (j + 1) / 12.0);
               v(k00, u0, v0, c1, opacity, side);
               v(k10, u1, v0, c1, opacity, side);
               v(k11, u1, v1, c1, opacity, side);
               v(k00, u0, v0, c1, opacity, side);
               v(k11, u1, v1, c1, opacity, side);
               v(k01, u0, v1, c1, opacity, side);
            }
         }
      }

      flush("wings", throughWalls, style, c2, time, 0.0F);
   }

   private static void v(int k, float u, float vv, float[] c, float op, int side) {
      WorldFx.vertex(gx[k], gy[k], gz[k], u, vv, c[0], c[1], c[2], op, side, 0.37F);
   }

   public static void hat(int style, Attach.Frame m, double scale, float[] c1, float[] c2, float opacity, boolean throughWalls, float time, float spin, boolean bob) {
      Vec3 cam = ClarityRenderUtils.getCameraPos();
      double[] r = new double[]{m.rx, m.ry, m.rz};
      double[] up = new double[]{m.ux, m.uy, m.uz};
      double[] f = new double[]{m.fx, m.fy, m.fz};
      double bobY = bob ? 0.03 * Math.sin(time * 2.0) * scale : 0.0;
      WorldFx.begin();
      switch (style) {
         case 0: {
            double[] c = m.map(0.0, 0.3 * scale + bobY, 0.0);
            double s = 0.36 * scale;
            quad(cam, c, r, f, s, s, 0.0F, 0.0F, c1, opacity);
            break;
         }
         case 1: {
            double[] c = m.map(0.0, -0.234, 0.0);
            double s = 0.52 * scale;

            for (int k = 0; k < 2; k++) {
               double phi = time * 0.35 + k * Math.PI * 0.5;
               double alpha = k == 0 ? 0.55 : -0.55;
               double[] a = add(mul(r, Math.cos(phi)), mul(f, Math.sin(phi)));
               double[] b = add(mul(r, -Math.sin(phi)), mul(f, Math.cos(phi)));
               double[] at = add(mul(a, Math.cos(alpha)), mul(up, Math.sin(alpha)));
               quad(cam, c, at, b, s, s, k, k == 0 ? 1.0F : -1.0F, c1, opacity);
            }
            break;
         }
         case 2: {
            double[] c = m.map(0.0, -0.05 * scale, 0.0);
            double R = 0.25 * scale;
            int n = 24;

            for (int i = 0; i < n; i++) {
               double a0 = (Math.PI * 2) * i / n;
               double a1 = (Math.PI * 2) * (i + 1) / n;
               double[] d0 = add(mul(r, Math.cos(a0)), mul(f, Math.sin(a0)));
               double[] d1 = add(mul(r, Math.cos(a1)), mul(f, Math.sin(a1)));
               double[] b0 = add(c, mul(d0, R));
               double[] b1 = add(c, mul(d1, R));
               double vt = 1.15;
               double[] t0 = add(add(c, mul(d0, R * 1.138)), mul(up, 0.24 * scale * 1.15));
               double[] t1 = add(add(c, mul(d1, R * 1.138)), mul(up, 0.24 * scale * 1.15));
               float u0 = (float)i / n;
               float u1 = (float)(i + 1) / n;
               tri(cam, b0, u0, 0.0F, b1, u1, 0.0F, t1, u1, 1.15F, c1, opacity, 0.0F);
               tri(cam, b0, u0, 0.0F, t1, u1, 1.15F, t0, u0, 1.15F, c1, opacity, 0.0F);
            }
            break;
         }
         case 3: {
            for (int s = -1; s <= 1; s += 2) {
               double tilt = 0.28 * s;
               double[] uu = add(mul(up, Math.cos(tilt)), mul(r, Math.sin(tilt)));
               double[] rr = add(mul(r, Math.cos(tilt)), mul(up, -Math.sin(tilt)));
               double[] cx = add(m.map(s * 0.15 * scale, 0.0, -0.02 * scale), mul(uu, 0.12 * scale));
               double[] rs = mul(rr, s);
               quad(cam, cx, rs, uu, 0.12 * scale, 0.13 * scale, 0.0F, 0.0F, c1, opacity);
            }
         }
      }

      flush("hats", throughWalls, style, c2, time, spin);
   }

   private static void flush(String shader, boolean through, int style, float[] c2, float time, float spin) {
      WorldFx.flush(shader, through, s -> {
         s.setUniform1f("uTime", time);
         s.setUniform1i("uStyle", style);
         s.setUniform3f("uColor2", c2[0], c2[1], c2[2]);
         s.setUniform1f("uSpin", spin);
      });
   }

   private static void quad(Vec3 cam, double[] c, double[] a, double[] b, double ha, double hb, float part, float dir, float[] col, float op) {
      double E = 1.35;
      ha *= E;
      hb *= E;
      float e = (float)E;
      double[] p00 = add(add(c, mul(a, -ha)), mul(b, -hb));
      double[] p10 = add(add(c, mul(a, ha)), mul(b, -hb));
      double[] p11 = add(add(c, mul(a, ha)), mul(b, hb));
      double[] p01 = add(add(c, mul(a, -ha)), mul(b, hb));
      tri(cam, p00, -e, -e, p10, e, -e, p11, e, e, col, op, part, dir);
      tri(cam, p00, -e, -e, p11, e, e, p01, -e, e, col, op, part, dir);
   }

   private static void tri(Vec3 cam, double[] a, float ua, float va, double[] b, float ub, float vb, double[] c, float uc, float vc, float[] col, float op, float part) {
      tri(cam, a, ua, va, b, ub, vb, c, uc, vc, col, op, part, 0.0F);
   }

   private static void tri(Vec3 cam, double[] a, float ua, float va, double[] b, float ub, float vb, double[] c, float uc, float vc, float[] col, float op, float part, float dir) {
      vert(cam, a, ua, va, col, op, part, dir);
      vert(cam, b, ub, vb, col, op, part, dir);
      vert(cam, c, uc, vc, col, op, part, dir);
   }

   private static void vert(Vec3 cam, double[] p, float u, float v, float[] col, float op, float part, float dir) {
      WorldFx.vertex((float)(p[0] - cam.x), (float)(p[1] - cam.y), (float)(p[2] - cam.z), u, v, col[0], col[1], col[2], op, part, dir);
   }

   private static double[] add(double[] a, double[] b) {
      return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
   }

   private static double[] mul(double[] a, double s) {
      return new double[]{a[0] * s, a[1] * s, a[2] * s};
   }

   private static int indexOf(String[] arr, String v) {
      if (v != null) {
         for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(v)) {
               return i;
            }
         }
      }

      return -1;
   }

   public static float[] rgb(int c) {
      return new float[]{(c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
   }
}
