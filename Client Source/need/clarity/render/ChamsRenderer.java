package need.clarity.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.lang.reflect.Constructor;
import java.util.Optional;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class ChamsRenderer {
   private static final float BIAS_SCALE = 0.0F;
   private static final float BIAS_CONSTANT = -1100000.0F;
   private static final DepthStencilState THROUGH_WALLS_STATE = createThroughWallsState();
   private static final RenderType CHAMS_WALLS = RenderType.create("clarity:chams_walls", RenderSetup.builder(buildFlat("clarity/chams_walls", true)).sortOnUpload().createRenderSetup());
   private static final RenderType CHAMS_DEPTH = RenderType.create("clarity:chams_depth", RenderSetup.builder(buildFlat("clarity/chams_depth", false)).sortOnUpload().createRenderSetup());

   private static DepthStencilState createThroughWallsState() {
      try {
         Constructor<DepthStencilState> c = DepthStencilState.class.getDeclaredConstructor(CompareOp.class, boolean.class, float.class, float.class);
         c.setAccessible(true);
         return c.newInstance(CompareOp.LESS_THAN_OR_EQUAL, true, 0.0F, -1100000.0F);
      } catch (Throwable t) {
         return null;
      }
   }

   public static DepthStencilState throughWallsDepthState() {
      return THROUGH_WALLS_STATE;
   }

   private static RenderPipeline buildFlat(String location, boolean throughWalls) {
      Builder b = RenderPipeline.builder(new Snippet[0])
         .withLocation(location)
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.ENTITY, Mode.QUADS);
      if (!throughWalls) {
         b.withDepthStencilState(DepthStencilState.DEFAULT);
      } else if (THROUGH_WALLS_STATE != null) {
         b.withDepthStencilState(THROUGH_WALLS_STATE);
      } else {
         b.withDepthStencilState(Optional.empty());
      }

      return b.build();
   }

   private ChamsRenderer() {
   }

   public static RenderType modelRenderType(boolean throughWalls) {
      return throughWalls ? CHAMS_WALLS : CHAMS_DEPTH;
   }

   public static boolean isOwnType(RenderType type) {
      return type == CHAMS_WALLS || type == CHAMS_DEPTH;
   }

   public static void dispose() {
   }
}
