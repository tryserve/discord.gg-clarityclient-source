package need.clarity.modules.utility;

import java.util.Optional;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostHand extends ModuleBase {
   private static GhostHand instance;
   private static final double STEP = 0.1;

   public GhostHand() {
      super("Ghost Hand", "Utility");
      instance = this;
   }

   public static GhostHand getInstance() {
      return instance;
   }

   public void onPick(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         if (mc.screen == null) {
            if (!this.isVanillaHitContainer(mc)) {
               double range = mc.player.blockInteractionRange();
               Vec3 start = mc.player.getEyePosition();
               Vec3 lookVec = mc.player.getLookAngle();
               Vec3 end = start.add(lookVec.scale(range));
               BlockHitResult ghostHit = this.findContainerThroughWalls(mc, start, end);
               if (ghostHit != null) {
                  mc.hitResult = ghostHit;
               }
            }
         }
      }
   }

   private BlockHitResult findContainerThroughWalls(Minecraft mc, Vec3 start, Vec3 end) {
      double dx = end.x - start.x;
      double dy = end.y - start.y;
      double dz = end.z - start.z;
      double totalLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (totalLen < 0.001) {
         return null;
      }

      int steps = (int)Math.ceil(totalLen / 0.1);
      BlockPos lastChecked = null;

      for (int i = 1; i <= steps; i++) {
         double t = (double)i / steps;
         BlockPos pos = BlockPos.containing(start.x + dx * t, start.y + dy * t, start.z + dz * t);
         if (!pos.equals(lastChecked)) {
            lastChecked = pos;
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir()) {
               BlockEntity be = mc.level.getBlockEntity(pos);
               if (be instanceof MenuProvider) {
                  Vec3 hitLoc = this.computeHitPoint(mc, pos, state, start, end);
                  Direction face = this.computeFace(hitLoc, pos);
                  return new BlockHitResult(hitLoc, face, pos, false);
               }
            }
         }
      }

      return null;
   }

   private boolean isVanillaHitContainer(Minecraft mc) {
      if (mc.hitResult instanceof BlockHitResult bhr) {
         if (bhr.getType() == Type.MISS) {
            return false;
         }

         BlockEntity be = mc.level.getBlockEntity(bhr.getBlockPos());
         return be instanceof MenuProvider;
      } else {
         return false;
      }
   }

   private Vec3 computeHitPoint(Minecraft mc, BlockPos pos, BlockState state, Vec3 start, Vec3 end) {
      VoxelShape shape = state.getShape(mc.level, pos);
      if (!shape.isEmpty()) {
         AABB bb = shape.bounds().move(pos);
         Optional<Vec3> clip = bb.clip(start, end);
         if (clip.isPresent()) {
            return clip.get();
         }
      }

      return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   private Direction computeFace(Vec3 hitPoint, BlockPos pos) {
      double relX = hitPoint.x - (pos.getX() + 0.5);
      double relY = hitPoint.y - (pos.getY() + 0.5);
      double relZ = hitPoint.z - (pos.getZ() + 0.5);
      double absX = Math.abs(relX);
      double absY = Math.abs(relY);
      double absZ = Math.abs(relZ);
      if (absX >= absY && absX >= absZ) {
         return relX > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (absY >= absX && absY >= absZ) {
         return relY > 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return relZ > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }
}
