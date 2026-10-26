package need.clarity.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Arrays;
import need.clarity.render.worldfx.WorldFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CrystalModelFx {
   public static final String SHADER = "crystalskin";
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
   private static EndCrystalModel model;
   private static final EndCrystalRenderState STATE = new EndCrystalRenderState();
   private static final PoseStack PS = new PoseStack();
   private static final Vector3f V = new Vector3f();
   private static final int[] TRI = new int[]{0, 1, 2, 0, 2, 3};
   private static final float[] LU = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
   private static final float[] LV = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
   private static final float[] PX = new float[4];
   private static final float[] PY = new float[4];
   private static final float[] PZ = new float[4];
   private static float[] cubeClass = new float[0];
   private static int visitIdx;
   private static float curIntensity;
   private static float curInflate;
   private static float curSeed;

   private CrystalModelFx() {
   }

   private static void visitCube(Pose pose, String path, int idx, Cube cube) {
      int i = visitIdx++;
      if (i >= cubeClass.length) {
         float[] grown = Arrays.copyOf(cubeClass, i + 1);
         grown[i] = path.contains("base") ? -1.0F : (path.contains("cube") ? 2.0F : (path.contains("inner") ? 1.0F : 0.0F));
         cubeClass = grown;
      }

      float ci = cubeClass[i];
      if (!(ci < 0.0F)) {
         float inflate = curInflate;
         float cx = (cube.minX + cube.maxX) * 0.5F;
         float cy = (cube.minY + cube.maxY) * 0.5F;
         float cz = (cube.minZ + cube.maxZ) * 0.5F;
         Matrix4f mat = pose.pose();

         for (Polygon poly : cube.polygons) {
            Vertex[] vs = poly.vertices();
            if (vs.length == 4) {
               for (int c = 0; c < 4; c++) {
                  Vertex v = vs[c];
                  V.set((cx + (v.x() - cx) * inflate) * 0.0625F, (cy + (v.y() - cy) * inflate) * 0.0625F, (cz + (v.z() - cz) * inflate) * 0.0625F);
                  mat.transformPosition(V);
                  PX[c] = V.x;
                  PY[c] = V.y;
                  PZ[c] = V.z;
               }

               for (int t = 0; t < 6; t++) {
                  int c = TRI[t];
                  Vertex v = vs[c];
                  WorldFx.vertex(PX[c], PY[c], PZ[c], LU[c], LV[c], v.u(), v.v(), ci, curIntensity, 0.0F, curSeed);
               }
            }
         }
      }
   }

   private static EndCrystalModel model() {
      if (model == null) {
         model = new EndCrystalModel(EndCrystalModel.createBodyLayer().bakeRoot());
      }

      return model;
   }

   public static int textureId() {
      try {
         AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
         return t.getTexture() instanceof GlTexture gt ? gt.glId() : 0;
      } catch (Throwable e) {
         return 0;
      }
   }

   public static void emit(float x, float y, float z, float age, float intensity, float inflate, float seed) {
      if (!(intensity <= 0.002F)) {
         EndCrystalModel m = model();
         STATE.ageInTicks = age;
         STATE.showsBottom = false;
         m.setupAnim(STATE);
         curIntensity = intensity;
         curInflate = inflate;
         curSeed = seed;
         visitIdx = 0;
         PS.pushPose();

         try {
            PS.translate(x, y, z);
            PS.scale(2.0F, 2.0F, 2.0F);
            PS.translate(0.0F, -0.5F, 0.0F);
            m.root().visit(PS, CrystalModelFx::visitCube);
         } finally {
            PS.popPose();
         }
      }
   }
}
