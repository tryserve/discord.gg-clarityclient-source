package need.clarity.modules.utility;

import java.util.Optional;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.modules.ModuleBase;
import need.clarity.util.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class MiddleClickFriend extends ModuleBase {
   private static MiddleClickFriend instance;

   public MiddleClickFriend() {
      super("Middle Click Friend", "Utility");
      instance = this;
   }

   public static boolean onMiddleClick() {
      MiddleClickFriend m = instance;
      if (m != null && m.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && mc.screen == null) {
            Player target = m.pick(mc);
            if (target == null) {
               return false;
            }

            String uuid = Friends.uuidOf(target);
            String name = Friends.nameOf(target);
            String mode = m.getSetting("Action", "Toggle");
            boolean already = FriendManager.get(uuid, name) != null;
            if (already && "Toggle".equals(mode)) {
               FriendManager.remove(uuid, name);
               if (m.getSetting("Notify", true)) {
                  Friends.alert(name, "removed from friends", false, UiSounds.Sound.CHIME_DOWN);
               }
            } else if (!already) {
               FriendManager.add(uuid, name);
               if (m.getSetting("Notify", true)) {
                  Friends.alert(name, "added to friends", true, UiSounds.Sound.SUCCESS);
               }
            } else if (m.getSetting("Notify", true)) {
               Friends.alert(name, "is already a friend", true);
            }

            return m.getSetting("Cancel Pick Block", true);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private Player pick(Minecraft mc) {
      double range = this.getSetting("Range", 64.0);
      boolean walls = this.getSetting("Through Walls", false);
      if (mc.crosshairPickEntity instanceof Player p && p != mc.player) {
         return p;
      } else {
         Vec3 eyes = mc.player.getEyePosition();
         Vec3 dir = mc.player.getViewVector(1.0F);
         Vec3 end = eyes.add(dir.scale(range));
         if (!walls) {
            BlockHitResult block = mc.level.clip(new ClipContext(eyes, end, Block.COLLIDER, Fluid.NONE, mc.player));
            if (block != null && block.getType() != Type.MISS) {
               end = block.getLocation();
            }
         }

         Player best = null;
         double bestD = Double.MAX_VALUE;

         for (Player p : mc.level.players()) {
            if (p != mc.player && p.isAlive() && !p.isSpectator() && !(p.distanceToSqr(mc.player) > (range + 2.0) * (range + 2.0))) {
               double pad = 0.1 + Math.min(0.6, eyes.distanceTo(p.position()) * 0.01);
               AABB box = p.getBoundingBox().inflate(pad);
               Optional<Vec3> hit = box.clip(eyes, end);
               if (!hit.isEmpty()) {
                  double d = eyes.distanceToSqr(hit.get());
                  if (d < bestD) {
                     bestD = d;
                     best = p;
                  }
               }
            }
         }

         return best;
      }
   }
}
