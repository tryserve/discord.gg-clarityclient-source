package need.clarity.render.worldfx;

import need.clarity.util.ClarityRenderUtils;
import net.minecraft.world.phys.Vec3;

public final class PetFx {
   private PetFx() {
   }

   public static void shadow(double x, double groundY, double z, double radius, float strength, boolean throughWalls) {
      if (!(strength <= 0.01F)) {
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         float cx = (float)(x - cam.x);
         float cy = (float)(groundY + 0.015 - cam.y);
         float cz = (float)(z - cam.z);
         float r = (float)radius;
         WorldFx.begin();
         v(cx - r, cy, cz - r, -1.0F, -1.0F, strength);
         v(cx + r, cy, cz - r, 1.0F, -1.0F, strength);
         v(cx + r, cy, cz + r, 1.0F, 1.0F, strength);
         v(cx - r, cy, cz - r, -1.0F, -1.0F, strength);
         v(cx + r, cy, cz + r, 1.0F, 1.0F, strength);
         v(cx - r, cy, cz + r, -1.0F, 1.0F, strength);
         WorldFx.flush("petshadow", throughWalls, null);
      }
   }

   private static void v(float x, float y, float z, float u, float vv, float strength) {
      WorldFx.vertex(x, y, z, u, vv, 0.0F, 0.0F, 0.0F, strength, 0.0F, 0.0F);
   }
}
