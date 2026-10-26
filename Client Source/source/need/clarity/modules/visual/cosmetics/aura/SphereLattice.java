package need.clarity.modules.visual.cosmetics.aura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SphereLattice {
   private static final double TAU = Math.PI * 2;
   static final double INSET = 0.13;
   final int freq;
   final double[][] cellCenter;
   final int[][] cellCorner;
   final double[][][] cellInset;
   final double[][] corner;
   final int[][] edge;
   private static final SphereLattice[] CACHE = new SphereLattice[6];

   static synchronized SphereLattice get(int freq) {
      int f = Math.max(1, Math.min(5, freq));
      if (CACHE[f] == null) {
         CACHE[f] = new SphereLattice(f);
      }

      return CACHE[f];
   }

   int quads() {
      return this.cellCenter.length * 3 + this.edge.length;
   }

   private SphereLattice(int f) {
      this.freq = f;
      double t = (1.0 + Math.sqrt(5.0)) / 2.0;
      double[][] iv = new double[][]{
         {-1.0, t, 0.0}, {1.0, t, 0.0}, {-1.0, -t, 0.0}, {1.0, -t, 0.0}, {0.0, -1.0, t}, {0.0, 1.0, t}, {0.0, -1.0, -t}, {0.0, 1.0, -t}, {t, 0.0, -1.0}, {t, 0.0, 1.0}, {-t, 0.0, -1.0}, {-t, 0.0, 1.0}
      };
      int[][] fc = new int[][]{
         {0, 11, 5},
         {0, 5, 1},
         {0, 1, 7},
         {0, 7, 10},
         {0, 10, 11},
         {1, 5, 9},
         {5, 11, 4},
         {11, 10, 2},
         {10, 7, 6},
         {7, 1, 8},
         {3, 9, 4},
         {3, 4, 2},
         {3, 2, 6},
         {3, 6, 8},
         {3, 8, 9},
         {4, 9, 5},
         {2, 4, 11},
         {6, 2, 10},
         {8, 6, 7},
         {9, 8, 1}
      };

      for (int i = 0; i < iv.length; i++) {
         iv[i] = unit(iv[i]);
      }

      double[] up = iv[0];
      double[] ex = unit(cross(up, iv[5]));
      double[] ez = cross(ex, up);

      for (int i = 0; i < iv.length; i++) {
         iv[i] = new double[]{dot(iv[i], ex), dot(iv[i], up), dot(iv[i], ez)};
      }

      Map<String, Integer> index = new HashMap<>();
      List<double[]> verts = new ArrayList<>();
      List<int[]> tris = new ArrayList<>();

      for (int[] face : fc) {
         int[][] grid = new int[f + 1][];

         for (int i = 0; i <= f; i++) {
            grid[i] = new int[f + 1 - i];

            for (int j = 0; j <= f - i; j++) {
               double a = (double)(f - i - j) / f;
               double b = (double)i / f;
               double c = (double)j / f;
               double[] p = unit(
                  new double[]{
                     iv[face[0]][0] * a + iv[face[1]][0] * b + iv[face[2]][0] * c,
                     iv[face[0]][1] * a + iv[face[1]][1] * b + iv[face[2]][1] * c,
                     iv[face[0]][2] * a + iv[face[1]][2] * b + iv[face[2]][2] * c
                  }
               );
               String key = Math.round(p[0] * 100000.0) + "," + Math.round(p[1] * 100000.0) + "," + Math.round(p[2] * 100000.0);
               Integer k = index.get(key);
               if (k == null) {
                  k = verts.size();
                  verts.add(p);
                  index.put(key, k);
               }

               grid[i][j] = k;
            }
         }

         for (int i = 0; i < f; i++) {
            for (int j = 0; j < f - i; j++) {
               tris.add(new int[]{grid[i][j], grid[i + 1][j], grid[i][j + 1]});
               if (i + j < f - 1) {
                  tris.add(new int[]{grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]});
               }
            }
         }
      }

      int nT = tris.size();
      int nV = verts.size();
      this.corner = new double[nT][];
      List<List<Integer>> around = new ArrayList<>();

      for (int i = 0; i < nV; i++) {
         around.add(new ArrayList<>());
      }

      Map<Long, int[]> edgeTris = new HashMap<>();

      for (int ti = 0; ti < nT; ti++) {
         int[] tr = tris.get(ti);
         double[] c = new double[3];

         for (int v : tr) {
            c[0] += ((double[])verts.get(v))[0];
            c[1] += ((double[])verts.get(v))[1];
            c[2] += ((double[])verts.get(v))[2];
            around.get(v).add(ti);
         }

         this.corner[ti] = unit(c);

         for (int e = 0; e < 3; e++) {
            int a = tr[e];
            int b = tr[(e + 1) % 3];
            long key = (long)Math.min(a, b) << 32 | Math.max(a, b);
            int[] pair = edgeTris.get(key);
            if (pair == null) {
               edgeTris.put(key, new int[]{ti, -1});
            } else {
               pair[1] = ti;
            }
         }
      }

      List<int[]> es = new ArrayList<>();

      for (int[] pair : edgeTris.values()) {
         if (pair[1] >= 0) {
            es.add(pair);
         }
      }

      es.sort((p, q) -> p[0] != q[0] ? Integer.compare(p[0], q[0]) : Integer.compare(p[1], q[1]));
      this.edge = es.toArray(new int[0][]);
      this.cellCenter = new double[nV][];
      this.cellCorner = new int[nV][];
      this.cellInset = new double[nV][][];

      for (int v = 0; v < nV; v++) {
         double[] n = verts.get(v);
         this.cellCenter[v] = n;
         double[] ref = Math.abs(n[1]) < 0.9 ? new double[]{0.0, 1.0, 0.0} : new double[]{1.0, 0.0, 0.0};
         double[] e1 = unit(cross(n, ref));
         double[] e2 = cross(n, e1);
         List<Integer> ring = new ArrayList<>(around.get(v));
         double[] fn = n;
         ring.sort((a, b) -> Double.compare(ang(this.corner[a], fn, e1, e2), ang(this.corner[b], fn, e1, e2)));
         this.cellCorner[v] = ring.stream().mapToInt(Integer::intValue).toArray();
         this.cellInset[v] = new double[this.cellCorner[v].length][];

         for (int k = 0; k < this.cellCorner[v].length; k++) {
            double[] c = this.corner[this.cellCorner[v][k]];
            this.cellInset[v][k] = unit(new double[]{c[0] * 0.87 + n[0] * 0.13, c[1] * 0.87 + n[1] * 0.13, c[2] * 0.87 + n[2] * 0.13});
         }
      }
   }

   private static double ang(double[] c, double[] n, double[] e1, double[] e2) {
      double a = Math.atan2(dot(c, e2), dot(c, e1));
      return a < 0.0 ? a + (Math.PI * 2) : a;
   }

   private static double dot(double[] a, double[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   private static double[] cross(double[] a, double[] b) {
      return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
   }

   private static double[] unit(double[] p) {
      double l = Math.sqrt(dot(p, p));
      return l < 1.0E-12 ? new double[]{0.0, 1.0, 0.0} : new double[]{p[0] / l, p[1] / l, p[2] / l};
   }

   int pentagons() {
      return (int)Arrays.stream(this.cellCorner).filter(c -> c.length == 5).count();
   }
}
