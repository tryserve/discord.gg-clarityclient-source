package need.clarity.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public final class RawShaderDrawUtil {
   private static int vertexArray = -1;
   private static int vertexBuffer = -1;
   private static int indexBuffer = -1;
   private static int indexedQuadCapacity = 0;

   private RawShaderDrawUtil() {
   }

   public static void draw(MeshData mesh) {
      RenderSystem.assertOnRenderThread();
      ensureBuffers();
      ByteBuffer vertData = mesh.vertexBuffer();
      DrawState state = mesh.drawState();
      VertexFormat format = state.format();
      Mode mode = state.mode();
      int vertexCount = state.vertexCount();
      GL30.glBindVertexArray(vertexArray);
      GL15.glBindBuffer(34962, vertexBuffer);
      GL15.glBufferData(34962, vertData, 35040);
      configureAttributes(format);
      drawArrays(mode, vertexCount);
      List<VertexFormatElement> elements = format.getElements();

      for (int i = 0; i < elements.size(); i++) {
         GL20.glDisableVertexAttribArray(i);
      }

      GL30.glBindVertexArray(0);
      GL15.glBindBuffer(34962, 0);
      mesh.close();
   }

   private static void ensureBuffers() {
      if (vertexArray == -1) {
         vertexArray = GL30.glGenVertexArrays();
         vertexBuffer = GL15.glGenBuffers();
         indexBuffer = GL15.glGenBuffers();
      }
   }

   private static void configureAttributes(VertexFormat format) {
      List<VertexFormatElement> elements = format.getElements();
      List<String> names = format.getElementAttributeNames();
      int stride = format.getVertexSize();
      int[] offsets = format.getOffsetsByElement();

      for (int i = 0; i < elements.size(); i++) {
         VertexFormatElement el = elements.get(i);
         String name = names.get(i);
         int location = resolveAttributeLocation(el, name);
         if (location >= 0) {
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, el.count(), toGlType(el.type()), shouldNormalize(el), stride, offsets[i]);
         }
      }
   }

   private static int resolveAttributeLocation(VertexFormatElement el, String name) {
      if (el == VertexFormatElement.POSITION) {
         return 0;
      } else if (el == VertexFormatElement.UV0) {
         return 1;
      } else if (el == VertexFormatElement.COLOR) {
         return 2;
      } else {
         return el == VertexFormatElement.NORMAL ? 3 : GL20.glGetAttribLocation(GL11.glGetInteger(35725), name);
      }
   }

   private static void drawArrays(Mode mode, int vertexCount) {
      if (mode == Mode.QUADS) {
         int quadCount = vertexCount / 4;
         ensureQuadIndices(quadCount);
         GL15.glBindBuffer(34963, indexBuffer);
         GL11.glDrawElements(4, quadCount * 6, 5125, 0L);
         GL15.glBindBuffer(34963, 0);
      } else {
         int glPrim = switch (mode) {
            case TRIANGLES -> 4;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
            case LINES -> 1;
            default -> 4;
         };
         GL11.glDrawArrays(glPrim, 0, vertexCount);
      }
   }

   private static void ensureQuadIndices(int quadCount) {
      if (quadCount > indexedQuadCapacity) {
         int newCapacity = Math.max(quadCount, indexedQuadCapacity * 2);
         IntBuffer buf = MemoryUtil.memAllocInt(newCapacity * 6);

         for (int i = 0; i < newCapacity; i++) {
            int base = i * 4;
            buf.put(base);
            buf.put(base + 1);
            buf.put(base + 2);
            buf.put(base + 2);
            buf.put(base + 3);
            buf.put(base);
         }

         buf.flip();
         GL15.glBindBuffer(34963, indexBuffer);
         GL15.glBufferData(34963, buf, 35044);
         GL15.glBindBuffer(34963, 0);
         MemoryUtil.memFree(buf);
         indexedQuadCapacity = newCapacity;
      }
   }

   private static int toGlType(Type type) {
      return switch (type) {
         case FLOAT -> 5126;
         case UBYTE, BYTE -> 5121;
         case USHORT, SHORT -> 5123;
         case UINT, INT -> 5125;
         default -> throw new MatchException(null, null);
      };
   }

   private static boolean shouldNormalize(VertexFormatElement el) {
      return el == VertexFormatElement.COLOR || el == VertexFormatElement.NORMAL;
   }
}
