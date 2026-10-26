package need.clarity.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;

public class Shader {
   private final int programId;
   private final String vertexPath;
   private final String fragmentPath;
   private final String defines;
   private final Map<String, Integer> uniformLocations = new HashMap<>();

   public Shader(String folder, String name) {
      this(folder, name, null);
   }

   public Shader(String folder, String name, String defines) {
      this.defines = defines;
      this.vertexPath = "/assets/clarity/shaders/" + folder + "/" + name + ".vsh";
      this.fragmentPath = "/assets/clarity/shaders/" + folder + "/" + name + ".fsh";
      int vertexId = this.createShader(this.vertexPath, 35633);
      int fragmentId = this.createShader(this.fragmentPath, 35632);
      this.programId = GL20.glCreateProgram();
      GL20.glAttachShader(this.programId, vertexId);
      GL20.glAttachShader(this.programId, fragmentId);
      GL20.glLinkProgram(this.programId);
      if (GL20.glGetProgrami(this.programId, 35714) == 0) {
         throw new RuntimeException("Failed to link shader program: " + GL20.glGetProgramInfoLog(this.programId));
      }

      GL20.glDeleteShader(vertexId);
      GL20.glDeleteShader(fragmentId);
   }

   private int createShader(String path, int type) {
      try {
         String source = this.injectDefines(this.readShaderFile(path));
         int shaderId = GL20.glCreateShader(type);
         GL20.glShaderSource(shaderId, source);
         GL20.glCompileShader(shaderId);
         if (GL20.glGetShaderi(shaderId, 35713) == 0) {
            throw new RuntimeException("Failed to compile shader: " + path + "\n" + GL20.glGetShaderInfoLog(shaderId));
         } else {
            return shaderId;
         }
      } catch (IOException e) {
         throw new RuntimeException("Failed to load shader: " + path, e);
      }
   }

   private String injectDefines(String src) {
      if (this.defines != null && !this.defines.isEmpty()) {
         int nl = src.startsWith("#version") ? src.indexOf(10) : -1;
         return nl < 0 ? this.defines + "\n" + src : src.substring(0, nl + 1) + this.defines + "\n" + src.substring(nl + 1);
      } else {
         return src;
      }
   }

   private String readShaderFile(String path) throws IOException {
      InputStream stream = Shader.class.getResourceAsStream(path);
      if (stream == null) {
         throw new IOException("Shader file not found: " + path);
      }

      StringBuilder source = new StringBuilder();

      String line;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
         while ((line = reader.readLine()) != null) {
            source.append(line).append("\n");
         }
      }

      return source.toString();
   }

   public void bind() {
      GL20.glUseProgram(this.programId);
   }

   public void unbind() {
      GL20.glUseProgram(0);
   }

   private int location(String name) {
      Integer cached = this.uniformLocations.get(name);
      if (cached != null) {
         return cached;
      }

      int loc = GL20.glGetUniformLocation(this.programId, name);
      this.uniformLocations.put(name, loc);
      return loc;
   }

   public void setUniform1i(String name, int value) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform1i(loc, value);
      }
   }

   public void setUniform1f(String name, float value) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform1f(loc, value);
      }
   }

   public void setUniform2f(String name, float x, float y) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform2f(loc, x, y);
      }
   }

   public void setUniform2f(String name, Vector2f v) {
      this.setUniform2f(name, v.x, v.y);
   }

   public void setUniform3f(String name, float x, float y, float z) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform3f(loc, x, y, z);
      }
   }

   public void setUniform3f(String name, Vector3f v) {
      this.setUniform3f(name, v.x, v.y, v.z);
   }

   public void setUniform4f(String name, float x, float y, float z, float w) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform4f(loc, x, y, z, w);
      }
   }

   public void setUniform1fv(String name, float[] values) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform1fv(loc, values);
      }
   }

   public void setUniform4fv(String name, float[] values) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniform4fv(loc, values);
      }
   }

   public void setUniformMatrix4f(String name, float[] columnMajor) {
      int loc = this.location(name);
      if (loc != -1) {
         GL20.glUniformMatrix4fv(loc, false, columnMajor);
      }
   }

   public void setUniformBool(String name, boolean value) {
      this.setUniform1i(name, value ? 1 : 0);
   }

   public void delete() {
      GL20.glDeleteProgram(this.programId);
   }
}
