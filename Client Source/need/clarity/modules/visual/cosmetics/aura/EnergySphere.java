package need.clarity.modules.visual.cosmetics.aura;

import java.util.ArrayList;
import java.util.List;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.Col;
import need.clarity.modules.visual.cosmetics.Cosmetic;
import need.clarity.modules.visual.cosmetics.Prim;

public final class EnergySphere implements Cosmetic {
   public static final double RIPPLE_LIFE = 1.3;
   private static final double RIPPLE_SPEED = 3.2;
   private static final double SEAM_HALF = 0.013;
   private final SphereParams p;

   public EnergySphere() {
      this(SphereParams.defaults());
   }

   public EnergySphere(SphereParams p) {
      this.p = p;
   }

   public static int quads(int frequency) {
      return SphereLattice.get(frequency).quads();
   }

   public static int[] latticeReport(int frequency) {
      SphereLattice L = SphereLattice.get(frequency);
      int[] uses = new int[L.corner.length];
      int minS = 99;
      int maxS = 0;

      for (int[] c : L.cellCorner) {
         minS = Math.min(minS, c.length);
         maxS = Math.max(maxS, c.length);

         for (int k : c) {
            uses[k]++;
         }
      }

      int odd = 0;

      for (int u : uses) {
         if (u != 3) {
            odd++;
         }
      }

      return new int[]{L.cellCenter.length, L.corner.length, L.edge.length, L.pentagons(), minS, maxS, odd};
   }

   @Override
   public void build(Builder b) {
      SphereLattice L = SphereLattice.get(this.p.frequency());
      EnergySphere.Field f = new EnergySphere.Field(b, this.p);
      EnergySphere.Sample[] corner = new EnergySphere.Sample[L.corner.length];

      for (int i = 0; i < corner.length; i++) {
         corner[i] = f.at(L.corner[i], 1.004);
      }

      for (int[] e : L.edge) {
         EnergySphere.Sample sa = corner[e[0]];
         EnergySphere.Sample sb = corner[e[1]];
         double[] mid = Prim.unit(new double[]{L.corner[e[0]][0] + L.corner[e[1]][0], L.corner[e[0]][1] + L.corner[e[1]][1], L.corner[e[0]][2] + L.corner[e[1]][2]});
         double[] along = Prim.sub(sb.pos, sa.pos);
         double[] side = Prim.cross(mid, along);
         double sl = Math.sqrt(Prim.dot(side, side));
         if (!(sl < 1.0E-9)) {
            double k = 0.013 / sl;
            double[] s = new double[]{side[0] * k, side[1] * k, side[2] * k};
            int ca = f.seam(sa);
            int cb = f.seam(sb);
            b.quad(Prim.add(sa.pos, s[0], s[1], s[2]), Prim.add(sa.pos, -s[0], -s[1], -s[2]), Prim.add(sb.pos, -s[0], -s[1], -s[2]), Prim.add(sb.pos, s[0], s[1], s[2]), ca, ca, cb, cb);
         }
      }

      for (int c = 0; c < L.cellCenter.length; c++) {
         int[] cc = L.cellCorner[c];
         int m = cc.length;
         boolean pent = m == 5;
         double var = f.panelVariation(c, pent);
         EnergySphere.Sample centre = f.at(L.cellCenter[c], 0.998);
         int cCol = f.fill(centre, var, pent);
         EnergySphere.Sample[] v = new EnergySphere.Sample[m];
         int[] col = new int[m];

         for (int k = 0; k < m; k++) {
            v[k] = f.at(L.cellInset[c][k], 0.998);
            col[k] = f.fill(v[k], var, pent);
         }

         int k;
         for (k = 0; k + 2 <= m; k += 2) {
            int k1 = (k + 1) % m;
            int k2 = (k + 2) % m;
            b.quad(centre.pos, v[k].pos, v[k1].pos, v[k2].pos, cCol, col[k], col[k1], col[k2]);
         }

         if (k < m) {
            b.tri(centre.pos, v[k].pos, v[0].pos, cCol, col[k], col[0]);
         }
      }
   }

   private static final class Field {
      final int c1;
      final int c2;
      final int hit;
      final double R;
      final double t;
      final double scale;
      final double inside;
      final boolean animate;
      final double[] cam;
      final double[][] rdir;
      final double[] rage;

      Field(Builder b, SphereParams p) {
         this.c1 = b.c1;
         this.c2 = b.c2;
         this.hit = Col.tint(Col.mix(b.c1, b.c2, 0.5), 0.82);
         this.animate = b.animate;
         this.t = this.animate ? b.time : 0.0;
         this.scale = b.scale();
         this.R = p.radius() * (this.animate ? 1.0 + 0.012 * Math.sin(this.t * 1.6) : 1.0);
         this.cam = b.viewLocal();
         this.inside = this.cam != null && Math.sqrt(Prim.dot(this.cam, this.cam)) < p.radius() * 0.98 ? 0.55 : 1.0;
         List<double[]> dirs = new ArrayList<>();
         List<Double> ages = new ArrayList<>();

         for (Ripple r : p.ripples()) {
            double l = Math.sqrt(r.dx() * r.dx() + r.dy() * r.dy() + r.dz() * r.dz());
            if (!(l < 1.0E-9) && !(r.age() < 0.0) && !(r.age() > 1.3)) {
               dirs.add(new double[]{r.dx() / l, r.dy() / l, r.dz() / l});
               ages.add(r.age());
            }
         }

         this.rdir = dirs.toArray(new double[0][]);
         this.rage = ages.stream().mapToDouble(Double::doubleValue).toArray();
      }

      double ripple(double[] n) {
         double s = 0.0;

         for (int i = 0; i < this.rdir.length; i++) {
            double th = Math.acos(Math.max(-1.0, Math.min(1.0, Prim.dot(n, this.rdir[i]))));
            double age = this.rage[i];
            double life = age / 1.3;
            double front = 3.2 * age;
            double w = 0.4 + 0.25 * life;
            double d1 = (th - front) / w;
            double d2 = (th - front * 0.62) / w;
            double fade = Math.pow(1.0 - life, 1.0);
            double ring = Math.exp(-d1 * d1) + 0.45 * Math.exp(-d2 * d2);
            double core = Math.exp(-th * th / 0.3) * Math.max(0.0, 1.0 - age / 0.3);
            s += ring * fade * 1.2 + core;
         }

         return Math.min(1.3, s);
      }

      EnergySphere.Sample at(double[] n, double lift) {
         EnergySphere.Sample s = new EnergySphere.Sample();
         s.ny = n[1];
         s.rip = this.rdir.length == 0 ? 0.0 : this.ripple(n);
         double r = this.R * (lift + 0.035 * s.rip);
         s.pos = Prim.P(n[0] * r, n[1] * r, n[2] * r);
         if (this.cam == null) {
            s.fres = 0.5;
            s.back = false;
            s.near = 1.0;
         } else {
            double dx = this.cam[0] - s.pos[0];
            double dy = this.cam[1] - s.pos[1];
            double dz = this.cam[2] - s.pos[2];
            double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double c = dl < 1.0E-9 ? 0.5 : (dx * n[0] + dy * n[1] + dz * n[2]) / dl;
            s.fres = 1.0 - Math.abs(c);
            s.back = c < 0.0;
            s.near = AuraKit.near(this.cam, s.pos[0], s.pos[1], s.pos[2], this.scale);
         }

         s.scan = 0.0;
         if (this.animate) {
            double yb = -1.05 + 2.1 * (this.t * 0.22 % 1.0);
            double d = (n[1] - yb) / 0.16;
            s.scan = Math.exp(-d * d);
         }

         return s;
      }

      double panelVariation(int cell, boolean pent) {
         double v = 0.85 + 0.3 * Prim.rnd(cell * 7 + 1);
         if (this.animate) {
            double tw = 0.5 + 0.5 * Math.sin(this.t * (0.9 + 1.6 * Prim.rnd(cell * 7 + 2)) + 6.283 * Prim.rnd(cell * 7 + 3));
            v += 0.55 * Math.pow(tw, 9.0);
         }

         return pent ? v * 1.45 : v;
      }

      private int body(EnergySphere.Sample s, double tintAmount) {
         int base = Col.mix(this.c2, this.c1, 0.5 + 0.5 * s.ny);
         return Col.tint(base, tintAmount);
      }

      int fill(EnergySphere.Sample s, double var, boolean pent) {
         int base = this.body(s, 0.1 + 0.3 * s.fres);
         if (pent) {
            base = Col.mix(base, Col.tint(Col.mix(this.c1, this.c2, 0.5 - 0.5 * s.ny), 0.35), 0.5);
         }

         base = Col.mix(base, this.hit, Math.min(1.0, s.rip * 0.8));
         double a = (0.055 + 0.22 * s.fres * s.fres + 0.16 * s.scan + 0.7 * s.rip) * var;
         a *= (s.back ? 0.6 : 1.0) * this.inside * s.near;
         return Col.withA(base, Math.min(0.85, a));
      }

      int seam(EnergySphere.Sample s) {
         int base = this.body(s, 0.35 + 0.3 * s.fres);
         base = Col.mix(base, this.hit, Math.min(1.0, s.rip * 0.9));
         double a = 0.32 + 0.42 * Math.pow(s.fres, 1.5) + 0.45 * s.scan + 0.6 * s.rip;
         a *= (s.back ? 0.65 : 1.0) * this.inside * s.near;
         return Col.withA(base, Math.min(0.95, a));
      }
   }

   private static final class Sample {
      double[] pos;
      double ny;
      double fres;
      boolean back;
      double scan;
      double rip;
      double near;
   }
}
