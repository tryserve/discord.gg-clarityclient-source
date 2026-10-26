package need.clarity.render.sky;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

final class SkyProgram {
   private static final String DIR = "/assets/clarity/shaders/sky/";
   final int id;
   private final Map<String, Integer> locations = new HashMap<>();

   private SkyProgram(int id) {
      this.id = id;
   }

   static SkyProgram preset(String file) throws IOException {
      String frag = "#version 330 core\n" + read("common.glsl") + "\n" + read(file + ".fsh");
      return link(read("sky.vsh"), frag, file);
   }

   static SkyProgram upscale() throws IOException {
      return link(read("upscale.vsh"), read("upscale.fsh"), "upscale");
   }

   private static SkyProgram link(String vsh, String fsh, String name) {
      int vs = compile(35633, vsh, name + ".vsh");

      int fs;
      try {
         fs = compile(35632, fsh, name + ".fsh");
      } catch (RuntimeException e) {
         GL20.glDeleteShader(vs);
         throw e;
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, vs);
      GL20.glAttachShader(p, fs);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glLinkProgram(p);
      GL20.glDetachShader(p, vs);
      GL20.glDetachShader(p, fs);
      GL20.glDeleteShader(vs);
      GL20.glDeleteShader(fs);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         String log = GL20.glGetProgramInfoLog(p);
         GL20.glDeleteProgram(p);
         throw new RuntimeException("link failed (" + name + "): " + log);
      } else {
         return new SkyProgram(p);
      }
   }

   private static int compile(int type, String src, String name) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, src);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         String log = GL20.glGetShaderInfoLog(s);
         GL20.glDeleteShader(s);
         throw new RuntimeException("compile failed (" + name + "): " + log);
      } else {
         return s;
      }
   }

   private static String read(String file) throws IOException {
      try (InputStream in = SkyProgram.class.getResourceAsStream("/assets/clarity/shaders/sky/" + file)) {
         if (in == null) {
            throw new IOException("missing shader /assets/clarity/shaders/sky/" + file);
         } else {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
         }
      }
   }

   int loc(String name) {
      Integer l = this.locations.get(name);
      if (l == null) {
         l = GL20.glGetUniformLocation(this.id, name);
         this.locations.put(name, l);
      }

      return l;
   }

   void set1i(String n, int v) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniform1i(l, v);
      }
   }

   void set1f(String n, float v) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniform1f(l, v);
      }
   }

   void set3f(String n, float x, float y, float z) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniform3f(l, x, y, z);
      }
   }

   void set1fv(String n, float[] v) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniform1fv(l, v);
      }
   }

   void setMat3(String n, float[] m) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniformMatrix3fv(l, false, m);
      }
   }

   void setMat4(String n, float[] m) {
      int l = this.loc(n);
      if (l >= 0) {
         GL20.glUniformMatrix4fv(l, false, m);
      }
   }

   void delete() {
      GL20.glDeleteProgram(this.id);
   }
}
