package need.clarity.modules.visual.playermodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ImportedModel {
   public final String name;
   public final List<ImportedModel.Part> parts = new ArrayList<>();
   public final Map<String, byte[]> images = new LinkedHashMap<>();
   public int triangles;
   public boolean rigged;
   public final float[] pivot = new float[18];
   public static final int MAX_TRIANGLES = 300000;
   public float widthRatio = 1.0F;

   private ImportedModel(String name) {
      this.name = name;
   }

   public static ImportedModel load(Path file, boolean zUp) throws IOException {
      String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
      ImportedModel m = new ImportedModel(file.getFileName().toString());
      if (n.endsWith(".obj")) {
         ImportedModel.ObjLoader.load(m, file);
      } else if (n.endsWith(".glb")) {
         ImportedModel.GltfLoader.loadGlb(m, file);
      } else if (n.endsWith(".gltf")) {
         ImportedModel.GltfLoader.loadGltf(m, file);
      } else {
         if (!n.endsWith(".bbmodel")) {
            throw new IOException("Unsupported file (use .bbmodel, .obj, .glb or .gltf)");
         }

         ImportedModel.BlockbenchLoader.load(m, file);
      }

      if (m.triangles == 0) {
         throw new IOException("The model has no triangles");
      }

      m.normalise(zUp);
      m.rig();
      return m;
   }

   public static boolean supported(Path p) {
      String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
      return n.endsWith(".obj") || n.endsWith(".glb") || n.endsWith(".gltf") || n.endsWith(".bbmodel");
   }

   static void flatNormals(float[] p, float[] nr, int i) {
      float ax = p[i * 3 + 3] - p[i * 3];
      float ay = p[i * 3 + 4] - p[i * 3 + 1];
      float az = p[i * 3 + 5] - p[i * 3 + 2];
      float bx = p[i * 3 + 6] - p[i * 3];
      float by = p[i * 3 + 7] - p[i * 3 + 1];
      float bz = p[i * 3 + 8] - p[i * 3 + 2];
      float nx = ay * bz - az * by;
      float ny = az * bx - ax * bz;
      float nz = ax * by - ay * bx;
      float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (l < 1.0E-12F) {
         nx = 0.0F;
         ny = 1.0F;
         nz = 0.0F;
      } else {
         nx /= l;
         ny /= l;
         nz /= l;
      }

      for (int k = 0; k < 3; k++) {
         nr[i * 3 + k * 3] = nx;
         nr[i * 3 + k * 3 + 1] = ny;
         nr[i * 3 + k * 3 + 2] = nz;
      }
   }

   private void normalise(boolean zUp) {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      float maxZ = -Float.MAX_VALUE;

      for (ImportedModel.Part p : this.parts) {
         if (zUp) {
            for (int i = 0; i < p.count; i++) {
               float y = p.pos[i * 3 + 1];
               float z = p.pos[i * 3 + 2];
               p.pos[i * 3 + 1] = z;
               p.pos[i * 3 + 2] = -y;
               float ny = p.nrm[i * 3 + 1];
               float nz = p.nrm[i * 3 + 2];
               p.nrm[i * 3 + 1] = nz;
               p.nrm[i * 3 + 2] = -ny;
            }
         }

         for (int i = 0; i < p.count; i++) {
            float x = p.pos[i * 3];
            float y = p.pos[i * 3 + 1];
            float z = p.pos[i * 3 + 2];
            if (x < minX) {
               minX = x;
            }

            if (x > maxX) {
               maxX = x;
            }

            if (y < minY) {
               minY = y;
            }

            if (y > maxY) {
               maxY = y;
            }

            if (z < minZ) {
               minZ = z;
            }

            if (z > maxZ) {
               maxZ = z;
            }
         }
      }

      float h = maxY - minY;
      if (!(h > 1.0E-9F)) {
         h = Math.max(maxX - minX, Math.max(maxZ - minZ, 1.0E-6F));
      }

      float s = 1.0F / h;
      float cx = (minX + maxX) / 2.0F;
      float cz = (minZ + maxZ) / 2.0F;

      for (ImportedModel.Part p : this.parts) {
         for (int i = 0; i < p.count; i++) {
            p.pos[i * 3] = (p.pos[i * 3] - cx) * s;
            p.pos[i * 3 + 1] = (p.pos[i * 3 + 1] - minY) * s;
            p.pos[i * 3 + 2] = (p.pos[i * 3 + 2] - cz) * s;
         }
      }

      this.widthRatio = Math.max(maxX - minX, maxZ - minZ) * s;
   }

   static byte kindOf(String... names) {
      for (String s : names) {
         if (s != null && !s.isEmpty()) {
            String t = s.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);

            for (String w : t.split("[^a-z]+")) {
               switch (w) {
                  case "head":
                  case "hair":
                  case "hat":
                  case "helmet":
                  case "face":
                  case "ear":
                  case "ears":
                  case "horn":
                  case "horns":
                  case "eye":
                  case "eyes":
                  case "nose":
                  case "mouth":
                  case "jaw":
                  case "beard":
                  case "snout":
                  case "skull":
                  case "antler":
                  case "antlers":
                  case "crown":
                     return 1;
                  case "arm":
                  case "arms":
                  case "hand":
                  case "hands":
                  case "shoulder":
                  case "forearm":
                  case "sleeve":
                  case "wrist":
                  case "elbow":
                  case "finger":
                  case "fingers":
                     return 2;
                  case "leg":
                  case "legs":
                  case "foot":
                  case "feet":
                  case "thigh":
                  case "shin":
                  case "calf":
                  case "knee":
                  case "boot":
                  case "boots":
                  case "shoe":
                  case "shoes":
                  case "sock":
                  case "toe":
                  case "toes":
                     return 3;
               }
            }
         }
      }

      return 0;
   }

   private void rig() {
      boolean anyLimb = false;
      boolean anyHead = false;

      for (ImportedModel.Part p : this.parts) {
         if (p.bone == null) {
            p.bone = new byte[p.count];
         }

         for (int i = 0; i < p.count; i++) {
            byte k = p.bone[i];
            if (k == 2 || k == 3) {
               anyLimb = true;
            } else if (k == 1) {
               anyHead = true;
            }
         }
      }

      if (!anyLimb && this.widthRatio <= 1.1F) {
         this.shapeClassify(!anyHead);
      }

      Map<Integer, float[]> span = new HashMap<>();

      for (ImportedModel.Part p : this.parts) {
         for (int t = 0; t + 2 < p.count; t += 3) {
            byte k = p.bone[t];
            int pid = p.piece == null ? -1 : p.piece[t];
            if ((k == 2 || k == 3) && pid >= 0) {
               float[] sp = span.computeIfAbsent(pid, q -> new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, 0.0F, 0.0F});

               for (int j = 0; j < 3; j++) {
                  float x = p.pos[(t + j) * 3];
                  sp[0] = Math.min(sp[0], x);
                  sp[1] = Math.max(sp[1], x);
                  sp[2] += x;
                  sp[3]++;
               }
            }
         }
      }

      float[] mn = new float[18];
      float[] mx = new float[18];
      Arrays.fill(mn, Float.MAX_VALUE);
      Arrays.fill(mx, -Float.MAX_VALUE);
      int[] cnt = new int[6];

      for (ImportedModel.Part p : this.parts) {
         for (int t = 0; t + 2 < p.count; t += 3) {
            byte k = p.bone[t];
            if (k != 0) {
               float cx = (p.pos[t * 3] + p.pos[t * 3 + 3] + p.pos[t * 3 + 6]) / 3.0F;
               float[] sp = k != 1 && p.piece != null && p.piece[t] >= 0 ? span.get(p.piece[t]) : null;
               boolean left = sp != null && (!(sp[0] < -0.02F) || !(sp[1] > 0.02F)) ? sp[2] / sp[3] > 0.0F : cx > 0.0F;
               int b = k == 1 ? 1 : (k == 2 ? (left ? 2 : 3) : (left ? 4 : 5));

               for (int j = 0; j < 3; j++) {
                  p.bone[t + j] = (byte)b;

                  for (int a = 0; a < 3; a++) {
                     float v = p.pos[(t + j) * 3 + a];
                     if (v < mn[b * 3 + a]) {
                        mn[b * 3 + a] = v;
                     }

                     if (v > mx[b * 3 + a]) {
                        mx[b * 3 + a] = v;
                     }
                  }

                  cnt[b]++;
               }
            }
         }
      }

      for (int b = 1; b < 6; b++) {
         if (cnt[b] != 0) {
            float h = mx[b * 3 + 1] - mn[b * 3 + 1];
            this.pivot[b * 3] = (mn[b * 3] + mx[b * 3]) / 2.0F;
            this.pivot[b * 3 + 2] = (mn[b * 3 + 2] + mx[b * 3 + 2]) / 2.0F;
            this.pivot[b * 3 + 1] = b == 1 ? mn[b * 3 + 1] + 0.1F * h : (b <= 3 ? mx[b * 3 + 1] - 0.1F * h : mx[b * 3 + 1] - 0.05F * h);
            this.rigged = true;
         }
      }
   }

   private void shapeClassify(boolean doHead) {
      float halfW = 0.0F;

      for (ImportedModel.Part p : this.parts) {
         for (int t = 0; t + 2 < p.count; t += 3) {
            float cy = (p.pos[t * 3 + 1] + p.pos[t * 3 + 4] + p.pos[t * 3 + 7]) / 3.0F;
            if (!(cy < 0.45F) && !(cy > 0.7F)) {
               for (int j = 0; j < 3; j++) {
                  halfW = Math.max(halfW, Math.abs(p.pos[(t + j) * 3]));
               }
            }
         }
      }

      for (ImportedModel.Part p : this.parts) {
         for (int t = 0; t + 2 < p.count; t += 3) {
            if (p.bone[t] == 0) {
               float cx = (p.pos[t * 3] + p.pos[t * 3 + 3] + p.pos[t * 3 + 6]) / 3.0F;
               float cy = (p.pos[t * 3 + 1] + p.pos[t * 3 + 4] + p.pos[t * 3 + 7]) / 3.0F;
               byte k = 0;
               if (doHead && cy > 0.77F) {
                  k = 1;
               } else if (cy < 0.4F) {
                  k = 3;
               } else if (cy <= 0.77F && halfW > 0.0F && Math.abs(cx) > 0.55F * halfW) {
                  k = 2;
               }

               if (k != 0) {
                  for (int j = 0; j < 3; j++) {
                     p.bone[t + j] = k;
                  }
               }
            }
         }
      }
   }

   static int rgba(float r, float g, float b, float a) {
      int R = Math.round(clamp01(r) * 255.0F);
      int G = Math.round(clamp01(g) * 255.0F);
      int B = Math.round(clamp01(b) * 255.0F);
      int A = Math.round(clamp01(a) * 255.0F);
      return A << 24 | R << 16 | G << 8 | B;
   }

   static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   static final class BlockbenchLoader {
      private static final String[] FACES = new String[]{"north", "east", "south", "west", "up", "down"};
      private final ImportedModel m;
      private final int nTex;
      private final String[] texKeys;
      private final String[] texIds;
      private final float[] texW;
      private final float[] texH;
      private final ImportedModel.PartBuilder[] builders;
      private final Map<String, JsonObject> byId = new HashMap<>();
      private final Set<String> used = new HashSet<>();
      private byte kind;
      private int piece;
      private int pieces;

      private BlockbenchLoader(ImportedModel m, int nTex) {
         this.m = m;
         this.nTex = nTex;
         this.texKeys = new String[nTex];
         this.texIds = new String[nTex];
         this.texW = new float[nTex];
         this.texH = new float[nTex];
         this.builders = new ImportedModel.PartBuilder[nTex + 1];
      }

      static void load(ImportedModel m, Path file) throws IOException {
         JsonObject root;
         try {
            root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
         } catch (RuntimeException e) {
            throw new IOException("Not a valid .bbmodel file");
         }

         JsonArray textures = arr(root, "textures");
         ImportedModel.BlockbenchLoader L = new ImportedModel.BlockbenchLoader(m, textures.size());
         float resW = 0.0F;
         float resH = 0.0F;
         JsonObject res = obj(root, "resolution");
         if (res != null) {
            resW = (float)num(res, "width", 0.0);
            resH = (float)num(res, "height", 0.0);
         }

         if (!(resW > 0.0F)) {
            resW = (float)num(root, "texture_width", 0.0);
         }

         if (!(resH > 0.0F)) {
            resH = (float)num(root, "texture_height", 0.0);
         }

         Path dir = file.toAbsolutePath().getParent();

         for (int i = 0; i < L.nTex; i++) {
            JsonObject t = textures.get(i).isJsonObject() ? textures.get(i).getAsJsonObject() : new JsonObject();
            L.texIds[i] = str(t, "id");
            float w = (float)num(t, "uv_width", 0.0);
            float h = (float)num(t, "uv_height", 0.0);
            if (!(w > 0.0F)) {
               w = resW > 0.0F ? resW : (float)num(t, "width", 16.0);
            }

            if (!(h > 0.0F)) {
               h = resH > 0.0F ? resH : (float)num(t, "height", 16.0);
            }

            L.texW[i] = Math.max(1.0F, w);
            L.texH[i] = Math.max(1.0F, h);
            byte[] data = null;
            String src = str(t, "source");
            int c = src == null ? -1 : src.indexOf("base64,");
            if (c >= 0) {
               try {
                  data = Base64.getMimeDecoder().decode(src.substring(c + 7));
               } catch (IllegalArgumentException var22) {
               }
            }

            if (data == null) {
               for (String key : new String[]{"relative_path", "path", "name"}) {
                  String s = str(t, key);
                  if (s != null && !s.isBlank()) {
                     try {
                        s = s.replace('\\', '/');
                        Path p = dir.resolve(s).normalize();
                        if (!Files.isRegularFile(p)) {
                           p = dir.resolve(s.substring(s.lastIndexOf(47) + 1));
                        }

                        if (Files.isRegularFile(p)) {
                           data = Files.readAllBytes(p);
                           break;
                        }
                     } catch (Exception var24) {
                     }
                  }
               }
            }

            if (data != null && data.length > 0) {
               L.texKeys[i] = "bb:" + i;
               m.images.put(L.texKeys[i], data);
            }
         }

         List<JsonObject> all = new ArrayList<>();

         for (JsonElement e : arr(root, "elements")) {
            if (e.isJsonObject()) {
               JsonObject o = e.getAsJsonObject();
               all.add(o);
               String id = str(o, "uuid");
               if (id != null) {
                  L.byId.put(id, o);
               }
            }
         }

         L.walk(arr(root, "outliner"), new ArrayList<>(), new ArrayList<>(), false);

         for (JsonObject o : all) {
            String id = str(o, "uuid");
            if (id == null || !L.used.contains(id)) {
               L.element(o, new ArrayList<>(), new ArrayList<>());
            }
         }

         for (ImportedModel.PartBuilder b : L.builders) {
            if (b != null) {
               b.finish(m);
            }
         }

         if (m.triangles > 300000) {
            throw new IOException("Too detailed (over 300000 triangles)");
         }
      }

      private void walk(JsonArray nodes, List<double[]> chain, List<String> names, boolean skip) {
         for (JsonElement n : nodes) {
            if (n.isJsonPrimitive()) {
               String id = n.getAsString();
               JsonObject el = this.byId.get(id);
               if (el != null && this.used.add(id) && !skip) {
                  this.element(el, chain, names);
               }
            } else if (n.isJsonObject()) {
               JsonObject g = n.getAsJsonObject();
               List<double[]> next = new ArrayList<>(chain);
               double[] o = vec3(g, "origin");
               double[] r = vec3(g, "rotation");
               next.add(new double[]{o[0], o[1], o[2], r[0], r[1], r[2]});
               List<String> nextNames = new ArrayList<>(names);
               nextNames.add(str(g, "name"));
               boolean hidden = !bool(g, "visibility", true) || !bool(g, "export", true);
               this.walk(arr(g, "children"), next, nextNames, skip || hidden);
            }
         }
      }

      private void element(JsonObject el, List<double[]> chain, List<String> names) {
         if (bool(el, "visibility", true) && bool(el, "export", true)) {
            String[] ns = new String[names.size() + 1];
            ns[0] = str(el, "name");

            for (int i = 0; i < names.size(); i++) {
               ns[i + 1] = names.get(names.size() - 1 - i);
            }

            this.kind = ImportedModel.kindOf(ns);
            this.piece = this.pieces++;
            String type = str(el, "type");
            if (type == null || type.equals("cube")) {
               this.cube(el, chain);
            } else if (type.equals("mesh")) {
               this.mesh(el, chain);
            }
         }
      }

      private void cube(JsonObject el, List<double[]> chain) {
         JsonArray fa = el.has("from") && el.get("from").isJsonArray() ? el.getAsJsonArray("from") : null;
         JsonArray ta = el.has("to") && el.get("to").isJsonArray() ? el.getAsJsonArray("to") : null;
         if (fa != null && ta != null && fa.size() >= 3 && ta.size() >= 3) {
            double inf = num(el, "inflate", 0.0);
            double x0 = fa.get(0).getAsDouble() - inf;
            double y0 = fa.get(1).getAsDouble() - inf;
            double z0 = fa.get(2).getAsDouble() - inf;
            double x1 = ta.get(0).getAsDouble() + inf;
            double y1 = ta.get(1).getAsDouble() + inf;
            double z1 = ta.get(2).getAsDouble() + inf;
            double[] org = vec3(el, "origin");
            double[] rot = vec3(el, "rotation");
            boolean box = bool(el, "box_uv", false);
            JsonArray off = el.has("uv_offset") && el.get("uv_offset").isJsonArray() ? el.getAsJsonArray("uv_offset") : null;
            JsonObject faces = obj(el, "faces");

            for (String f : FACES) {
               JsonObject fo = faces == null ? null : obj(faces, f);
               if (fo == null) {
                  if (!box) {
                     continue;
                  }

                  fo = new JsonObject();
               }

               int ti = this.textureIndex(fo);
               if (ti != -2) {
                  double[] uv = rect(fo, "uv");
                  if (uv == null && box && off != null && off.size() >= 2) {
                     uv = boxUv(f, off.get(0).getAsDouble(), off.get(1).getAsDouble(), x1 - x0, y1 - y0, z1 - z0);
                  }

                  if (uv != null) {
                     double[][] c = switch (f) {
                        case "north" -> new double[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
                        case "south" -> new double[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
                        case "east" -> new double[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
                        case "west" -> new double[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
                        case "up" -> new double[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
                        default -> new double[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
                     };
                     double[][] uvc = new double[][]{{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
                     int k = ((int)Math.round(num(fo, "rotation", 0.0)) / 90 % 4 + 4) % 4;
                     boolean textured = this.nTex > 0 && ti >= 0;
                     float tw = textured ? this.texW[ti] : 1.0F;
                     float th = textured ? this.texH[ti] : 1.0F;
                     float[][] p = new float[4][];
                     float[][] t = new float[4][];

                     for (int i = 0; i < 4; i++) {
                        p[i] = place(c[i], org, rot, chain);
                        double[] q = uvc[(i - k + 4) % 4];
                        t[i] = textured ? new float[]{(float)(q[0] / tw), (float)(q[1] / th)} : new float[]{0.0F, 0.0F};
                     }

                     this.quad(ti, p, t);
                  }
               }
            }
         }
      }

      private static double[] boxUv(String face, double ox, double oy, double w, double h, double d) {
         return switch (face) {
            case "east" -> new double[]{ox, oy + d, ox + d, oy + d + h};
            case "north" -> new double[]{ox + d, oy + d, ox + d + w, oy + d + h};
            case "west" -> new double[]{ox + d + w, oy + d, ox + 2.0 * d + w, oy + d + h};
            case "south" -> new double[]{ox + 2.0 * d + w, oy + d, ox + 2.0 * d + 2.0 * w, oy + d + h};
            case "up" -> new double[]{ox + d + w, oy + d, ox + d, oy};
            default -> new double[]{ox + d + 2.0 * w, oy, ox + d + w, oy + d};
         };
      }

      private void mesh(JsonObject el, List<double[]> chain) {
         JsonObject verts = obj(el, "vertices");
         JsonObject faces = obj(el, "faces");
         if (verts != null && faces != null) {
            double[] org = vec3(el, "origin");
            double[] rot = vec3(el, "rotation");

            for (Entry<String, JsonElement> fe : faces.entrySet()) {
               if (fe.getValue().isJsonObject()) {
                  JsonObject fo = fe.getValue().getAsJsonObject();
                  int ti = this.textureIndex(fo);
                  if (ti != -2) {
                     JsonArray ids = arr(fo, "vertices");
                     JsonObject uvs = obj(fo, "uv");
                     int n = ids.size();
                     if (n >= 3) {
                        float[][] p = new float[n][];
                        float[][] t = new float[n][];
                        boolean ok = true;

                        for (int i = 0; i < n && ok; i++) {
                           String id = ids.get(i).getAsString();
                           JsonElement ve = verts.get(id);
                           if (ve == null || !ve.isJsonArray() || ve.getAsJsonArray().size() < 3) {
                              ok = false;
                              break;
                           }

                           JsonArray va = ve.getAsJsonArray();
                           double[] local = new double[]{va.get(0).getAsDouble() + org[0], va.get(1).getAsDouble() + org[1], va.get(2).getAsDouble() + org[2]};
                           p[i] = place(local, org, rot, chain);
                           double u = 0.0;
                           double v = 0.0;
                           if (uvs != null && uvs.has(id) && uvs.get(id).isJsonArray() && uvs.getAsJsonArray(id).size() >= 2) {
                              u = uvs.getAsJsonArray(id).get(0).getAsDouble();
                              v = uvs.getAsJsonArray(id).get(1).getAsDouble();
                           }

                           t[i] = this.nTex != 0 && ti >= 0 ? new float[]{(float)(u / this.texW[ti]), (float)(v / this.texH[ti])} : new float[]{0.0F, 0.0F};
                        }

                        if (ok) {
                           ImportedModel.PartBuilder b = this.builder(ti);

                           for (int i = 1; i + 1 < n; i++) {
                              int start = b.n;
                              int[] tri = new int[]{0, i, i + 1};

                              for (int j : tri) {
                                 b.add(p[j][0], p[j][1], p[j][2], 0.0F, 0.0F, 0.0F, t[j][0], t[j][1], 0, false);
                              }

                              ImportedModel.flatNormals(b.pos, b.nrm, start);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private int textureIndex(JsonObject face) {
         JsonElement te = face.get("texture");
         if (te == null || te.isJsonNull()) {
            return this.nTex > 0 ? 0 : -1;
         }

         if (te.isJsonPrimitive() && te.getAsJsonPrimitive().isBoolean()) {
            return te.getAsBoolean() ? (this.nTex > 0 ? 0 : -1) : -2;
         }

         String s = te.getAsString();

         try {
            int i = Integer.parseInt(s);
            return i >= 0 && i < this.nTex ? i : (this.nTex > 0 ? 0 : -1);
         } catch (NumberFormatException e) {
            for (int i = 0; i < this.nTex; i++) {
               if (s.equals(this.texIds[i])) {
                  return i;
               }
            }

            return this.nTex > 0 ? 0 : -1;
         }
      }

      private ImportedModel.PartBuilder builder(int ti) {
         int slot = ti >= 0 && ti < this.nTex ? ti : this.nTex;
         if (this.builders[slot] == null) {
            this.builders[slot] = new ImportedModel.PartBuilder(slot < this.nTex ? new ImportedModel.Part(this.texKeys[slot], -1) : new ImportedModel.Part(null, -4934476));
         }

         this.builders[slot].curKind = this.kind;
         this.builders[slot].curPiece = this.piece;
         return this.builders[slot];
      }

      private void quad(int ti, float[][] p, float[][] t) {
         ImportedModel.PartBuilder b = this.builder(ti);
         int[][] tris = new int[][]{{0, 3, 2}, {0, 2, 1}};

         for (int[] tri : tris) {
            int start = b.n;

            for (int j : tri) {
               b.add(p[j][0], p[j][1], p[j][2], 0.0F, 0.0F, 0.0F, t[j][0], t[j][1], 0, false);
            }

            ImportedModel.flatNormals(b.pos, b.nrm, start);
         }
      }

      private static float[] place(double[] pt, double[] org, double[] rot, List<double[]> chain) {
         double[] v = rotate(pt, org, rot[0], rot[1], rot[2]);

         for (int i = chain.size() - 1; i >= 0; i--) {
            double[] g = chain.get(i);
            v = rotate(v, g, g[3], g[4], g[5]);
         }

         return new float[]{(float)(-v[0]), (float)v[1], (float)(-v[2])};
      }

      private static double[] rotate(double[] p, double[] pivot, double rx, double ry, double rz) {
         if (rx == 0.0 && ry == 0.0 && rz == 0.0) {
            return p;
         }

         double x = p[0] - pivot[0];
         double y = p[1] - pivot[1];
         double z = p[2] - pivot[2];
         if (rx != 0.0) {
            double c = Math.cos(Math.toRadians(rx));
            double s = Math.sin(Math.toRadians(rx));
            double t = y * c - z * s;
            z = y * s + z * c;
            y = t;
         }

         if (ry != 0.0) {
            double c = Math.cos(Math.toRadians(ry));
            double s = Math.sin(Math.toRadians(ry));
            double t = x * c + z * s;
            z = -x * s + z * c;
            x = t;
         }

         if (rz != 0.0) {
            double c = Math.cos(Math.toRadians(rz));
            double s = Math.sin(Math.toRadians(rz));
            double t = x * c - y * s;
            y = x * s + y * c;
            x = t;
         }

         return new double[]{x + pivot[0], y + pivot[1], z + pivot[2]};
      }

      private static JsonArray arr(JsonObject o, String k) {
         JsonElement e = o.get(k);
         return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
      }

      private static JsonObject obj(JsonObject o, String k) {
         JsonElement e = o.get(k);
         return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
      }

      private static String str(JsonObject o, String k) {
         JsonElement e = o.get(k);
         return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
      }

      private static double num(JsonObject o, String k, double def) {
         JsonElement e = o.get(k);

         try {
            return e != null && e.isJsonPrimitive() ? e.getAsDouble() : def;
         } catch (NumberFormatException ex) {
            return def;
         }
      }

      private static boolean bool(JsonObject o, String k, boolean def) {
         JsonElement e = o.get(k);
         return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
      }

      private static double[] vec3(JsonObject o, String k) {
         double[] v = new double[3];
         JsonElement e = o.get(k);
         if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();

            for (int i = 0; i < 3 && i < a.size(); i++) {
               try {
                  v[i] = a.get(i).getAsDouble();
               } catch (RuntimeException var7) {
               }
            }
         }

         return v;
      }

      private static double[] rect(JsonObject o, String k) {
         JsonElement e = o.get(k);
         if (e != null && e.isJsonArray() && e.getAsJsonArray().size() >= 4) {
            JsonArray a = e.getAsJsonArray();
            double[] r = new double[4];

            for (int i = 0; i < 4; i++) {
               try {
                  r[i] = a.get(i).getAsDouble();
               } catch (RuntimeException ex) {
                  return null;
               }
            }

            return r;
         } else {
            return null;
         }
      }
   }

   static final class GltfLoader {
      private static final int CHUNK_JSON = 1313821514;
      private static final int CHUNK_BIN = 5130562;

      static void loadGlb(ImportedModel m, Path file) throws IOException {
         ByteBuffer bb = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
         if (bb.remaining() >= 20 && bb.getInt(0) == 1179937895) {
            int pos = 12;
            JsonObject json = null;
            ByteBuffer bin = null;

            while (pos + 8 <= bb.limit()) {
               int len = bb.getInt(pos);
               int type = bb.getInt(pos + 4);
               if (len < 0 || pos + 8 + len > bb.limit()) {
                  break;
               }

               if (type == 1313821514) {
                  json = JsonParser.parseString(new String(bb.array(), pos + 8, len, StandardCharsets.UTF_8)).getAsJsonObject();
               } else if (type == 5130562 && bin == null) {
                  bin = ByteBuffer.wrap(bb.array(), pos + 8, len).slice().order(ByteOrder.LITTLE_ENDIAN);
               }

               pos += 8 + len;
            }

            if (json == null) {
               throw new IOException("Broken .glb (no JSON)");
            }

            load(m, json, bin, file.toAbsolutePath().getParent());
         } else {
            throw new IOException("Not a .glb file");
         }
      }

      static void loadGltf(ImportedModel m, Path file) throws IOException {
         JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
         load(m, json, null, file.toAbsolutePath().getParent());
      }

      private static void load(ImportedModel m, JsonObject g, ByteBuffer glbBin, Path dir) throws IOException {
         JsonArray extReq = g.getAsJsonArray("extensionsRequired");
         if (extReq != null) {
            for (JsonElement e : extReq) {
               String ext = e.getAsString();
               if (ext.contains("draco") || ext.contains("meshopt")) {
                  throw new IOException("Compressed glTF (" + ext + ") isn't supported; re-export without compression");
               }
            }
         }

         JsonArray bufs = arr(g, "buffers");
         ByteBuffer[] buffers = new ByteBuffer[bufs.size()];

         for (int i = 0; i < bufs.size(); i++) {
            JsonObject b = bufs.get(i).getAsJsonObject();
            String uri = b.has("uri") ? b.get("uri").getAsString() : null;
            if (uri == null) {
               buffers[i] = glbBin;
            } else {
               byte[] data = readUri(uri, dir);
               buffers[i] = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            }
         }

         ImportedModel.GltfLoader.Ctx c = new ImportedModel.GltfLoader.Ctx(g, buffers);
         JsonArray images = arr(g, "images");
         String[] imageKey = new String[images.size()];

         for (int i = 0; i < images.size(); i++) {
            JsonObject im = images.get(i).getAsJsonObject();

            try {
               byte[] bytes;
               if (im.has("bufferView")) {
                  bytes = c.view(im.get("bufferView").getAsInt());
               } else {
                  if (!im.has("uri")) {
                     continue;
                  }

                  bytes = readUri(im.get("uri").getAsString(), dir);
               }

               imageKey[i] = "gltf:" + i;
               m.images.put(imageKey[i], bytes);
            } catch (Exception var26) {
            }
         }

         JsonArray textures = arr(g, "textures");
         JsonArray materials = arr(g, "materials");
         int scene = g.has("scene") ? g.get("scene").getAsInt() : 0;
         JsonArray scenes = arr(g, "scenes");
         JsonArray nodes = arr(g, "nodes");
         List<Integer> roots = new ArrayList<>();
         if (scenes.size() > scene) {
            for (JsonElement e : arr(scenes.get(scene).getAsJsonObject(), "nodes")) {
               roots.add(e.getAsInt());
            }
         } else {
            for (int i = 0; i < nodes.size(); i++) {
               roots.add(i);
            }
         }

         Map<Integer, ImportedModel.PartBuilder> builders = new LinkedHashMap<>();
         Deque<Object[]> stack = new ArrayDeque<>();

         for (int r : roots) {
            stack.push(new Object[]{r, identity(), (byte)0});
         }

         int guard = 0;

         while (!stack.isEmpty() && guard++ < 100000) {
            Object[] it = stack.pop();
            int ni = (Integer)it[0];
            if (ni >= 0 && ni < nodes.size()) {
               JsonObject node = nodes.get(ni).getAsJsonObject();
               float[] world = mul((float[])it[1], local(node));
               byte kind = ImportedModel.kindOf(node.has("name") ? node.get("name").getAsString() : null);
               if (kind == 0 && node.has("mesh")) {
                  JsonArray ms = arr(g, "meshes");
                  int mi = node.get("mesh").getAsInt();
                  if (mi >= 0 && mi < ms.size() && ms.get(mi).getAsJsonObject().has("name")) {
                     kind = ImportedModel.kindOf(ms.get(mi).getAsJsonObject().get("name").getAsString());
                  }
               }

               if (kind == 0) {
                  kind = (Byte)it[2];
               }

               if (node.has("mesh")) {
                  mesh(m, c, g, node.get("mesh").getAsInt(), world, builders, materials, textures, imageKey, kind, ni);
               }

               for (JsonElement ch : arr(node, "children")) {
                  stack.push(new Object[]{ch.getAsInt(), world, kind});
               }
            }
         }

         for (ImportedModel.PartBuilder b : builders.values()) {
            b.finish(m);
         }
      }

      private static void mesh(
         ImportedModel m,
         ImportedModel.GltfLoader.Ctx c,
         JsonObject g,
         int meshIndex,
         float[] world,
         Map<Integer, ImportedModel.PartBuilder> builders,
         JsonArray materials,
         JsonArray textures,
         String[] imageKey,
         byte kind,
         int piece
      ) throws IOException {
         JsonArray meshes = arr(g, "meshes");
         if (meshIndex >= 0 && meshIndex < meshes.size()) {
            float[] nmat = normalMatrix(world);

            for (JsonElement pe : arr(meshes.get(meshIndex).getAsJsonObject(), "primitives")) {
               JsonObject prim = pe.getAsJsonObject();
               int mode = prim.has("mode") ? prim.get("mode").getAsInt() : 4;
               if (mode == 4 || mode == 5 || mode == 6) {
                  JsonObject at = prim.getAsJsonObject("attributes");
                  if (at != null && at.has("POSITION")) {
                     float[] P = c.floats(at.get("POSITION").getAsInt());
                     float[] N = at.has("NORMAL") ? c.floats(at.get("NORMAL").getAsInt()) : null;
                     float[] T = at.has("TEXCOORD_0") ? c.floats(at.get("TEXCOORD_0").getAsInt()) : null;
                     float[] C = at.has("COLOR_0") ? c.floats(at.get("COLOR_0").getAsInt()) : null;
                     int cComp = at.has("COLOR_0") ? c.components(at.get("COLOR_0").getAsInt()) : 0;
                     int vcount = P.length / 3;
                     int[] idx;
                     if (prim.has("indices")) {
                        idx = c.ints(prim.get("indices").getAsInt());
                     } else {
                        idx = new int[vcount];
                        int i = 0;

                        while (i < vcount) {
                           idx[i] = i++;
                        }
                     }

                     idx = triangles(idx, mode);
                     if (m.triangles + idx.length / 3 > 300000) {
                        throw new IOException("Too detailed (over 300000 triangles)");
                     }

                     int matIndex = prim.has("material") ? prim.get("material").getAsInt() : -1;
                     ImportedModel.PartBuilder b = builders.computeIfAbsent(matIndex, kx -> material(kx, materials, textures, imageKey));
                     b.curKind = kind;
                     b.curPiece = piece;

                     for (int t = 0; t + 2 < idx.length; t += 3) {
                        int start = b.n;
                        boolean noN = N == null;

                        for (int k = 0; k < 3; k++) {
                           int i = idx[t + k];
                           if (i < 0 || i >= vcount) {
                              i = 0;
                           }

                           float x = P[i * 3];
                           float y = P[i * 3 + 1];
                           float z = P[i * 3 + 2];
                           float wx = world[0] * x + world[4] * y + world[8] * z + world[12];
                           float wy = world[1] * x + world[5] * y + world[9] * z + world[13];
                           float wz = world[2] * x + world[6] * y + world[10] * z + world[14];
                           float nx = 0.0F;
                           float ny = 0.0F;
                           float nz = 0.0F;
                           if (!noN) {
                              float ox = N[i * 3];
                              float oy = N[i * 3 + 1];
                              float oz = N[i * 3 + 2];
                              nx = nmat[0] * ox + nmat[1] * oy + nmat[2] * oz;
                              ny = nmat[3] * ox + nmat[4] * oy + nmat[5] * oz;
                              nz = nmat[6] * ox + nmat[7] * oy + nmat[8] * oz;
                              float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                              if (l > 1.0E-12F) {
                                 nx /= l;
                                 ny /= l;
                                 nz /= l;
                              }
                           }

                           float u = T != null ? T[i * 2] : 0.0F;
                           float v = T != null ? T[i * 2 + 1] : 0.0F;
                           int col = -1;
                           if (C != null) {
                              float a = cComp == 4 ? C[i * 4 + 3] : 1.0F;
                              col = ImportedModel.rgba(C[i * cComp], C[i * cComp + 1], C[i * cComp + 2], a);
                           }

                           b.add(wx, wy, wz, nx, ny, nz, u, v, col, C != null);
                        }

                        if (noN) {
                           ImportedModel.flatNormals(b.pos, b.nrm, start);
                        }

                        if (det3(world) < 0.0F) {
                           swap(b, start + 1, start + 2);
                        }
                     }
                  }
               }
            }
         }
      }

      private static ImportedModel.PartBuilder material(int k, JsonArray materials, JsonArray textures, String[] imageKey) {
         int color = -1;
         String img = null;
         if (k >= 0 && k < materials.size()) {
            JsonObject mat = materials.get(k).getAsJsonObject();
            JsonObject pbr = mat.getAsJsonObject("pbrMetallicRoughness");
            if (pbr != null) {
               if (pbr.has("baseColorFactor")) {
                  JsonArray f = pbr.getAsJsonArray("baseColorFactor");
                  color = ImportedModel.rgba(f.get(0).getAsFloat(), f.get(1).getAsFloat(), f.get(2).getAsFloat(), f.size() > 3 ? f.get(3).getAsFloat() : 1.0F);
               }

               if (pbr.has("baseColorTexture")) {
                  img = texImage(pbr.getAsJsonObject("baseColorTexture"), textures, imageKey);
               }
            }

            JsonObject ext = mat.getAsJsonObject("extensions");
            if (img == null && ext != null && ext.has("KHR_materials_pbrSpecularGlossiness")) {
               JsonObject sg = ext.getAsJsonObject("KHR_materials_pbrSpecularGlossiness");
               if (sg.has("diffuseTexture")) {
                  img = texImage(sg.getAsJsonObject("diffuseTexture"), textures, imageKey);
               }
            }
         }

         return new ImportedModel.PartBuilder(new ImportedModel.Part(img, color));
      }

      private static String texImage(JsonObject texInfo, JsonArray textures, String[] imageKey) {
         int ti = texInfo.has("index") ? texInfo.get("index").getAsInt() : -1;
         if (ti >= 0 && ti < textures.size()) {
            JsonObject tex = textures.get(ti).getAsJsonObject();
            int src = tex.has("source") ? tex.get("source").getAsInt() : -1;
            JsonObject ext = tex.getAsJsonObject("extensions");
            if (src < 0 && ext != null) {
               for (String e : new String[]{"KHR_texture_basisu", "EXT_texture_webp"}) {
                  if (ext.has(e)) {
                     return null;
                  }
               }
            }

            return src >= 0 && src < imageKey.length ? imageKey[src] : null;
         } else {
            return null;
         }
      }

      private static void swap(ImportedModel.PartBuilder b, int i, int j) {
         for (int k = 0; k < 3; k++) {
            float t = b.pos[i * 3 + k];
            b.pos[i * 3 + k] = b.pos[j * 3 + k];
            b.pos[j * 3 + k] = t;
            t = b.nrm[i * 3 + k];
            b.nrm[i * 3 + k] = b.nrm[j * 3 + k];
            b.nrm[j * 3 + k] = t;
         }

         for (int k = 0; k < 2; k++) {
            float t = b.uv[i * 2 + k];
            b.uv[i * 2 + k] = b.uv[j * 2 + k];
            b.uv[j * 2 + k] = t;
         }

         if (b.col != null) {
            int t = b.col[i];
            b.col[i] = b.col[j];
            b.col[j] = t;
         }
      }

      private static int[] triangles(int[] idx, int mode) {
         if (mode == 4) {
            return idx;
         }

         if (idx.length < 3) {
            return new int[0];
         }

         int[] out = new int[(idx.length - 2) * 3];

         for (int i = 0; i + 2 < idx.length; i++) {
            if (mode == 5) {
               boolean even = (i & 1) == 0;
               out[i * 3] = idx[i];
               out[i * 3 + 1] = even ? idx[i + 1] : idx[i + 2];
               out[i * 3 + 2] = even ? idx[i + 2] : idx[i + 1];
            } else {
               out[i * 3] = idx[0];
               out[i * 3 + 1] = idx[i + 1];
               out[i * 3 + 2] = idx[i + 2];
            }
         }

         return out;
      }

      private static byte[] readUri(String uri, Path dir) throws IOException {
         if (uri.startsWith("data:")) {
            int comma = uri.indexOf(44);
            return Base64.getDecoder().decode(uri.substring(comma + 1));
         } else {
            String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            Path p = dir.resolve(path).normalize();
            if (!p.startsWith(dir.normalize())) {
               throw new IOException("Bad file reference in model: " + uri);
            } else {
               return Files.readAllBytes(p);
            }
         }
      }

      private static int comps(String t) {
         return switch (t) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> 1;
         };
      }

      private static int csize(int ct) {
         return switch (ct) {
            case 5120, 5121 -> 1;
            case 5122, 5123 -> 2;
            default -> 4;
         };
      }

      private static float[] identity() {
         return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
      }

      private static float[] local(JsonObject n) {
         if (!n.has("matrix")) {
            float tx = 0.0F;
            float ty = 0.0F;
            float tz = 0.0F;
            float qx = 0.0F;
            float qy = 0.0F;
            float qz = 0.0F;
            float qw = 1.0F;
            float sx = 1.0F;
            float sy = 1.0F;
            float sz = 1.0F;
            if (n.has("translation")) {
               JsonArray a = n.getAsJsonArray("translation");
               tx = a.get(0).getAsFloat();
               ty = a.get(1).getAsFloat();
               tz = a.get(2).getAsFloat();
            }

            if (n.has("rotation")) {
               JsonArray a = n.getAsJsonArray("rotation");
               qx = a.get(0).getAsFloat();
               qy = a.get(1).getAsFloat();
               qz = a.get(2).getAsFloat();
               qw = a.get(3).getAsFloat();
            }

            if (n.has("scale")) {
               JsonArray a = n.getAsJsonArray("scale");
               sx = a.get(0).getAsFloat();
               sy = a.get(1).getAsFloat();
               sz = a.get(2).getAsFloat();
            }

            float xx = qx * qx;
            float yy = qy * qy;
            float zz = qz * qz;
            float xy = qx * qy;
            float xz = qx * qz;
            float yz = qy * qz;
            float wx = qw * qx;
            float wy = qw * qy;
            float wz = qw * qz;
            return new float[]{
               (1.0F - 2.0F * (yy + zz)) * sx,
               2.0F * (xy + wz) * sx,
               2.0F * (xz - wy) * sx,
               0.0F,
               2.0F * (xy - wz) * sy,
               (1.0F - 2.0F * (xx + zz)) * sy,
               2.0F * (yz + wx) * sy,
               0.0F,
               2.0F * (xz + wy) * sz,
               2.0F * (yz - wx) * sz,
               (1.0F - 2.0F * (xx + yy)) * sz,
               0.0F,
               tx,
               ty,
               tz,
               1.0F
            };
         } else {
            JsonArray a = n.getAsJsonArray("matrix");
            float[] m = new float[16];

            for (int i = 0; i < 16; i++) {
               m[i] = a.get(i).getAsFloat();
            }

            return m;
         }
      }

      private static float[] mul(float[] a, float[] b) {
         float[] o = new float[16];

         for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
               o[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
            }
         }

         return o;
      }

      private static float det3(float[] m) {
         return m[0] * (m[5] * m[10] - m[9] * m[6]) - m[4] * (m[1] * m[10] - m[9] * m[2]) + m[8] * (m[1] * m[6] - m[5] * m[2]);
      }

      private static float[] normalMatrix(float[] m) {
         float a = m[0];
         float b = m[4];
         float c = m[8];
         float d = m[1];
         float e = m[5];
         float f = m[9];
         float g = m[2];
         float h = m[6];
         float i = m[10];
         float A = e * i - f * h;
         float B = -(d * i - f * g);
         float C = d * h - e * g;
         float det = a * A + b * B + c * C;
         if (Math.abs(det) < 1.0E-20F) {
            return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
         }

         float inv = 1.0F / det;
         return new float[]{A * inv, B * inv, C * inv, -(b * i - c * h) * inv, (a * i - c * g) * inv, -(a * h - b * g) * inv, (b * f - c * e) * inv, -(a * f - c * d) * inv, (a * e - b * d) * inv};
      }

      private static JsonArray arr(JsonObject o, String k) {
         return o != null && o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
      }

      private static final class Ctx {
         final JsonArray accessors;
         final JsonArray views;
         final ByteBuffer[] buffers;

         Ctx(JsonObject g, ByteBuffer[] buffers) {
            this.accessors = ImportedModel.GltfLoader.arr(g, "accessors");
            this.views = ImportedModel.GltfLoader.arr(g, "bufferViews");
            this.buffers = buffers;
         }

         byte[] view(int vi) {
            JsonObject v = this.views.get(vi).getAsJsonObject();
            ByteBuffer b = this.buffers[v.get("buffer").getAsInt()];
            int off = v.has("byteOffset") ? v.get("byteOffset").getAsInt() : 0;
            int len = v.get("byteLength").getAsInt();
            byte[] out = new byte[len];
            ByteBuffer d = b.duplicate();
            d.position(off);
            d.get(out);
            return out;
         }

         int components(int ai) {
            return ImportedModel.GltfLoader.comps(this.accessors.get(ai).getAsJsonObject().get("type").getAsString());
         }

         float[] floats(int ai) {
            JsonObject a = this.accessors.get(ai).getAsJsonObject();
            int count = a.get("count").getAsInt();
            int comps = ImportedModel.GltfLoader.comps(a.get("type").getAsString());
            int ct = a.get("componentType").getAsInt();
            boolean norm = a.has("normalized") && a.get("normalized").getAsBoolean();
            float[] out = new float[count * comps];
            if (!a.has("bufferView")) {
               return out;
            }

            JsonObject v = this.views.get(a.get("bufferView").getAsInt()).getAsJsonObject();
            ByteBuffer b = this.buffers[v.get("buffer").getAsInt()];
            int base = (v.has("byteOffset") ? v.get("byteOffset").getAsInt() : 0) + (a.has("byteOffset") ? a.get("byteOffset").getAsInt() : 0);
            int size = ImportedModel.GltfLoader.csize(ct);
            int stride = v.has("byteStride") ? v.get("byteStride").getAsInt() : comps * size;

            for (int i = 0; i < count; i++) {
               for (int k = 0; k < comps; k++) {
                  int p = base + i * stride + k * size;

                  out[i * comps + k] = switch (ct) {
                     case 5120 -> norm ? Math.max(b.get(p) / 127.0F, -1.0F) : b.get(p);
                     case 5121 -> norm ? (b.get(p) & 0xFF) / 255.0F : b.get(p) & 0xFF;
                     case 5122 -> norm ? Math.max(b.getShort(p) / 32767.0F, -1.0F) : b.getShort(p);
                     case 5123 -> norm ? (b.getShort(p) & 65535) / 65535.0F : b.getShort(p) & 65535;
                     default -> 0.0F;
                     case 5125 -> (float)(b.getInt(p) & 4294967295L);
                     case 5126 -> b.getFloat(p);
                  };
               }
            }

            return out;
         }

         int[] ints(int ai) {
            JsonObject a = this.accessors.get(ai).getAsJsonObject();
            int count = a.get("count").getAsInt();
            int ct = a.get("componentType").getAsInt();
            JsonObject v = this.views.get(a.get("bufferView").getAsInt()).getAsJsonObject();
            ByteBuffer b = this.buffers[v.get("buffer").getAsInt()];
            int base = (v.has("byteOffset") ? v.get("byteOffset").getAsInt() : 0) + (a.has("byteOffset") ? a.get("byteOffset").getAsInt() : 0);
            int size = ImportedModel.GltfLoader.csize(ct);
            int stride = v.has("byteStride") ? v.get("byteStride").getAsInt() : size;
            int[] out = new int[count];

            for (int i = 0; i < count; i++) {
               int p = base + i * stride;

               out[i] = switch (ct) {
                  case 5121 -> b.get(p) & 0xFF;
                  case 5123 -> b.getShort(p) & 65535;
                  default -> b.getInt(p);
               };
            }

            return out;
         }
      }
   }

   static final class ObjLoader {
      static void load(ImportedModel m, Path file) throws IOException {
         List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
         float[] v = new float[3072];
         float[] vc = null;
         float[] vt = new float[2048];
         float[] vn = new float[3072];
         int nv = 0;
         int nvt = 0;
         int nvn = 0;
         Map<String, ImportedModel.ObjLoader.Mtl> mtls = new HashMap<>();
         Map<String, ImportedModel.PartBuilder> builders = new LinkedHashMap<>();
         String current = "";
         byte objKind = 0;
         byte grpKind = 0;
         String objName = "";
         String grpName = "";
         Map<String, Integer> pieces = new HashMap<>();
         Path dir = file.toAbsolutePath().getParent();

         for (String raw : lines) {
            String l = raw.trim();
            if (l.startsWith("mtllib ")) {
               for (String lib : l.substring(7).trim().split("\\s+(?=\\S+\\.mtl)")) {
                  readMtl(dir, lib.trim(), mtls, m);
               }
            }
         }

         for (String raw : lines) {
            String l = raw.trim();
            if (!l.isEmpty() && l.charAt(0) != '#') {
               String[] t = l.split("\\s+");
               switch (t[0]) {
                  case "v":
                     if (nv * 3 + 3 > v.length) {
                        v = Arrays.copyOf(v, v.length * 2);
                     }

                     v[nv * 3] = f(t, 1);
                     v[nv * 3 + 1] = f(t, 2);
                     v[nv * 3 + 2] = f(t, 3);
                     if (t.length >= 7) {
                        if (vc == null) {
                           vc = new float[v.length];
                        }

                        if (vc.length < v.length) {
                           vc = Arrays.copyOf(vc, v.length);
                        }

                        vc[nv * 3] = f(t, 4);
                        vc[nv * 3 + 1] = f(t, 5);
                        vc[nv * 3 + 2] = f(t, 6);
                     }

                     nv++;
                     break;
                  case "vt":
                     if (nvt * 2 + 2 > vt.length) {
                        vt = Arrays.copyOf(vt, vt.length * 2);
                     }

                     vt[nvt * 2] = f(t, 1);
                     vt[nvt * 2 + 1] = t.length > 2 ? f(t, 2) : 0.0F;
                     nvt++;
                     break;
                  case "vn":
                     if (nvn * 3 + 3 > vn.length) {
                        vn = Arrays.copyOf(vn, vn.length * 2);
                     }

                     vn[nvn * 3] = f(t, 1);
                     vn[nvn * 3 + 1] = f(t, 2);
                     vn[nvn * 3 + 2] = f(t, 3);
                     nvn++;
                     break;
                  case "o":
                     objName = l.length() > 1 ? l.substring(1).trim() : "";
                     objKind = ImportedModel.kindOf(objName);
                     break;
                  case "g":
                     grpName = l.length() > 1 ? l.substring(1).trim() : "";
                     grpKind = ImportedModel.kindOf(grpName);
                     break;
                  case "usemtl":
                     current = t.length > 1 ? l.substring(7).trim() : "";
                     break;
                  case "f":
                     if (m.triangles + builders.values().stream().mapToInt(bx -> bx.n / 3).sum() > 300000) {
                        throw new IOException("Too detailed (over 300000 triangles)");
                     }

                     String key = current;
                     ImportedModel.PartBuilder b = builders.computeIfAbsent(key, k -> {
                        ImportedModel.ObjLoader.Mtl mt = mtls.get(k);
                        return new ImportedModel.PartBuilder(new ImportedModel.Part(mt == null ? null : mt.map, mt == null ? -1 : mt.color));
                     });
                     b.curKind = grpKind != 0 ? grpKind : objKind;
                     b.curPiece = pieces.computeIfAbsent(objName + "|" + grpName, q -> pieces.size());
                     int corners = t.length - 1;
                     int[][] idx = new int[corners][];

                     for (int i = 0; i < corners; i++) {
                        idx[i] = corner(t[i + 1], nv, nvt, nvn);
                     }

                     for (int i = 1; i + 1 < corners; i++) {
                        int start = b.n;
                        int[][] tri = new int[][]{idx[0], idx[i], idx[i + 1]};
                        boolean missingN = false;

                        for (int[] c : tri) {
                           int pi = c[0];
                           int ti = c[1];
                           int ni = c[2];
                           float u = 0.0F;
                           float vv = 0.0F;
                           float nx = 0.0F;
                           float ny = 0.0F;
                           float nz = 0.0F;
                           if (ti >= 0 && ti < nvt) {
                              u = vt[ti * 2];
                              vv = 1.0F - vt[ti * 2 + 1];
                           }

                           if (ni >= 0 && ni < nvn) {
                              nx = vn[ni * 3];
                              ny = vn[ni * 3 + 1];
                              nz = vn[ni * 3 + 2];
                           } else {
                              missingN = true;
                           }

                           boolean hc = vc != null && pi * 3 + 2 < vc.length;
                           int col = hc ? ImportedModel.rgba(vc[pi * 3], vc[pi * 3 + 1], vc[pi * 3 + 2], 1.0F) : -1;
                           b.add(v[pi * 3], v[pi * 3 + 1], v[pi * 3 + 2], nx, ny, nz, u, vv, col, hc);
                        }

                        if (missingN) {
                           ImportedModel.flatNormals(b.pos, b.nrm, start);
                        }
                     }
               }
            }
         }

         for (ImportedModel.PartBuilder b : builders.values()) {
            b.finish(m);
         }
      }

      private static int[] corner(String s, int nv, int nvt, int nvn) {
         String[] p = s.split("/", -1);
         int vi = idx(p[0], nv);
         int ti = p.length > 1 && !p[1].isEmpty() ? idx(p[1], nvt) : -1;
         int ni = p.length > 2 && !p[2].isEmpty() ? idx(p[2], nvn) : -1;
         if (vi >= 0 && vi < nv) {
            return new int[]{vi, ti, ni};
         } else {
            throw new IllegalArgumentException("Bad face index " + s);
         }
      }

      private static int idx(String s, int count) {
         int i = Integer.parseInt(s);
         return i < 0 ? count + i : i - 1;
      }

      private static float f(String[] t, int i) {
         return i < t.length ? Float.parseFloat(t[i]) : 0.0F;
      }

      private static void readMtl(Path dir, String name, Map<String, ImportedModel.ObjLoader.Mtl> out, ImportedModel m) {
         Path p = dir.resolve(name);
         if (Files.exists(p)) {
            try {
               ImportedModel.ObjLoader.Mtl cur = null;
               float kdR = 1.0F;
               float kdG = 1.0F;
               float kdB = 1.0F;
               float d = 1.0F;

               for (String raw : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                  String l = raw.trim();
                  if (!l.isEmpty() && l.charAt(0) != '#') {
                     String[] t = l.split("\\s+");
                     switch (t[0]) {
                        case "newmtl":
                           if (cur != null) {
                              cur.color = ImportedModel.rgba(kdR, kdG, kdB, d);
                           }

                           cur = new ImportedModel.ObjLoader.Mtl();
                           d = 1.0F;
                           kdB = 1.0F;
                           kdG = 1.0F;
                           kdR = 1.0F;
                           out.put(l.substring(6).trim(), cur);
                           break;
                        case "Kd":
                           if (t.length >= 4) {
                              kdR = Float.parseFloat(t[1]);
                              kdG = Float.parseFloat(t[2]);
                              kdB = Float.parseFloat(t[3]);
                           }
                           break;
                        case "d":
                           if (t.length >= 2) {
                              d = Float.parseFloat(t[1]);
                           }
                           break;
                        case "Tr":
                           if (t.length >= 2) {
                              d = 1.0F - Float.parseFloat(t[1]);
                           }
                           break;
                        case "map_Kd":
                           if (cur != null) {
                              String tex = t[t.length - 1];
                              Path tp = dir.resolve(tex.replace('\\', '/'));
                              if (!Files.exists(tp)) {
                                 tp = dir.resolve(tp.getFileName().toString());
                              }

                              if (Files.exists(tp)) {
                                 String key = "obj:" + tp.getFileName();
                                 if (!m.images.containsKey(key)) {
                                    m.images.put(key, Files.readAllBytes(tp));
                                 }

                                 cur.map = key;
                              }
                           }
                     }
                  }
               }

               if (cur != null) {
                  cur.color = ImportedModel.rgba(kdR, kdG, kdB, d);
               }
            } catch (Exception var19) {
            }
         }
      }

      private static final class Mtl {
         int color = -1;
         String map;
      }
   }

   public static final class Part {
      public final String image;
      public final int color;
      public float[] pos = new float[0];
      public float[] nrm = new float[0];
      public float[] uv = new float[0];
      public int[] vcol;
      public int count;
      int[] piece;
      public byte[] bone;

      Part(String image, int color) {
         this.image = image;
         this.color = color;
      }
   }

   static final class PartBuilder {
      final ImportedModel.Part part;
      float[] pos = new float[288];
      float[] nrm = new float[288];
      float[] uv = new float[192];
      int[] col;
      byte[] kd = new byte[96];
      byte curKind;
      int[] pc = new int[96];
      int curPiece = -1;
      int n;

      PartBuilder(ImportedModel.Part p) {
         this.part = p;
      }

      void add(float x, float y, float z, float nx, float ny, float nz, float u, float v, int c, boolean hasCol) {
         if (this.n * 3 + 3 > this.pos.length) {
            int cap = this.pos.length / 3 * 2;
            this.pos = Arrays.copyOf(this.pos, cap * 3);
            this.nrm = Arrays.copyOf(this.nrm, cap * 3);
            this.uv = Arrays.copyOf(this.uv, cap * 2);
            if (this.col != null) {
               this.col = Arrays.copyOf(this.col, cap);
            }

            this.kd = Arrays.copyOf(this.kd, cap);
            this.pc = Arrays.copyOf(this.pc, cap);
         }

         if (hasCol && this.col == null) {
            this.col = new int[this.pos.length / 3];
            Arrays.fill(this.col, 0, this.n, -1);
         }

         this.pos[this.n * 3] = x;
         this.pos[this.n * 3 + 1] = y;
         this.pos[this.n * 3 + 2] = z;
         this.nrm[this.n * 3] = nx;
         this.nrm[this.n * 3 + 1] = ny;
         this.nrm[this.n * 3 + 2] = nz;
         this.uv[this.n * 2] = u;
         this.uv[this.n * 2 + 1] = v;
         if (this.col != null) {
            this.col[this.n] = hasCol ? c : -1;
         }

         this.kd[this.n] = this.curKind;
         this.pc[this.n] = this.curPiece;
         this.n++;
      }

      void finish(ImportedModel m) {
         if (this.n != 0) {
            this.part.pos = Arrays.copyOf(this.pos, this.n * 3);
            this.part.nrm = Arrays.copyOf(this.nrm, this.n * 3);
            this.part.uv = Arrays.copyOf(this.uv, this.n * 2);
            if (this.col != null) {
               this.part.vcol = Arrays.copyOf(this.col, this.n);
            }

            this.part.bone = Arrays.copyOf(this.kd, this.n);
            this.part.piece = Arrays.copyOf(this.pc, this.n);
            this.part.count = this.n;
            m.parts.add(this.part);
            m.triangles = m.triangles + this.n / 3;
         }
      }
   }
}
