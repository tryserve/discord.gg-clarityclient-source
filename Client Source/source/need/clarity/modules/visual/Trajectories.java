package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Trajectories extends ModuleBase {
   public static Trajectories INSTANCE;
   private static final int MAX_STEPS = 300;
   private static final double BOX_SIZE = 0.08;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public Trajectories() {
      super("Trajectories", "Visual");
      INSTANCE = this;
      if (this.isEnabled()) {
         this.registerListener();
      }
   }

   @Override
   public void onEnable() {
      this.registerListener();
   }

   @Override
   public void onDisable() {
      this.unregisterListener();
   }

   private void registerListener() {
      if (!this.listenerRegistered) {
         this.listenerRegistered = true;
         WorldRenderEvent.register(this.renderListener);
      }
   }

   private void unregisterListener() {
      if (this.listenerRegistered) {
         this.listenerRegistered = false;
         WorldRenderEvent.unregister(this.renderListener);
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            Player player = mc.player;
            Trajectories.Projectile proj = this.resolveProjectile(player, partialTick);
            if (proj != null) {
               List<Vec3> points = this.simulate(mc, proj);
               if (points.size() >= 2) {
                  String mode = getRenderMode();
                  boolean doBoxes = "Boxes".equals(mode) || "Both".equals(mode);
                  boolean doLine = "Line".equals(mode) || "Both".equals(mode);
                  float boxOpacity = getBoxOpacity();
                  float lineOpacity = getLineOpacity();
                  if (doBoxes) {
                     int boxArgb = argb(getBoxColor(), boxOpacity);
                     List<ClarityRenderUtils.ESPEntry> entries = new java.util.ArrayList<>();

                     for (Vec3 p : points) {
                        entries.add(new ClarityRenderUtils.ESPEntry(new AABB(p.x - 0.08, p.y - 0.08, p.z - 0.08, p.x + 0.08, p.y + 0.08, p.z + 0.08), boxArgb));
                     }

                     ClarityRenderUtils.drawEspBoxes(poseStack, entries, false, boxArgb, true, true, false, 1.0F);
                  }

                  if (doLine) {
                     int lineArgb = argb(getLineColor(), lineOpacity);
                     float width = getLineWidth();
                     Trajectories.MultiBufferSourceHelper.drawPolyline(poseStack, points, lineArgb, width);
                  }
               }
            }
         }
      }
   }

   private List<Vec3> simulate(Minecraft mc, Trajectories.Projectile proj) {
      List<Vec3> points = new java.util.ArrayList<>();
      Vec3 pos = proj.start;
      Vec3 vel = proj.dir.scale(proj.speed);
      points.add(pos);

      for (int i = 0; i < 300; i++) {
         pos = pos.add(vel);
         vel = vel.scale(proj.drag);
         vel = vel.add(0.0, -proj.gravity, 0.0);
         BlockPos bp = BlockPos.containing(pos);
         BlockState state = mc.level.getBlockState(bp);
         if (!state.isAir()) {
            points.add(pos);
            break;
         }

         if (i % 2 == 0) {
            points.add(pos);
         }
      }

      return points;
   }

   private Trajectories.Projectile resolveProjectile(Player player, float partialTick) {
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      ItemStack held = null;
      InteractionHand hand = InteractionHand.MAIN_HAND;
      if (this.isChargedProjectileWeapon(player, main)) {
         held = main;
      } else if (this.isChargedProjectileWeapon(player, off)) {
         held = off;
         hand = InteractionHand.OFF_HAND;
      } else if (isThrowable(main)) {
         held = main;
      } else if (isThrowable(off)) {
         held = off;
         hand = InteractionHand.OFF_HAND;
      }

      if (held == null) {
         return null;
      } else {
         Vec3 dir = player.getViewVector(partialTick).normalize();
         double eye = player.getEyeHeight();
         Vec3 start = new Vec3(
            player.xOld + (player.getX() - player.xOld) * partialTick, player.yOld + (player.getY() - player.yOld) * partialTick + eye - 0.1, player.zOld + (player.getZ() - player.zOld) * partialTick
         );
         if (held.is(Items.BOW)) {
            float charge = getBowCharge(player, hand);
            double speed = Math.min(charge, 1.0) * 3.0;
            return new Trajectories.Projectile(start, dir, speed, 0.05, 0.99);
         } else if (held.is(Items.CROSSBOW)) {
            return new Trajectories.Projectile(start, dir, 3.15, 0.05, 0.99);
         } else {
            return held.is(Items.TRIDENT) ? new Trajectories.Projectile(start, dir, 2.5, 0.05, 0.99) : new Trajectories.Projectile(start, dir, 1.5, 0.03, 0.99);
         }
      }
   }

   private boolean isChargedProjectileWeapon(Player player, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.is(Items.BOW) ? player.isUsingItem() : stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT);
      }
   }

   private static boolean isThrowable(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : stack.is(Items.SNOWBALL)
            || stack.is(Items.EGG)
            || stack.is(Items.ENDER_PEARL)
            || stack.is(Items.SPLASH_POTION)
            || stack.is(Items.LINGERING_POTION)
            || stack.is(Items.EXPERIENCE_BOTTLE)
            || stack.is(Items.WIND_CHARGE);
   }

   private static float getBowCharge(Player player, InteractionHand hand) {
      if (player.isUsingItem() && player.getUsedItemHand() == hand) {
         int useTicks = player.getTicksUsingItem();
         float charge = useTicks / 20.0F;
         charge = (charge * charge + charge * 2.0F) / 3.0F;
         return Math.min(charge, 1.0F);
      } else {
         return 0.15F;
      }
   }

   public static String getRenderMode() {
      return INSTANCE.getSetting("Render", "Both");
   }

   public static int getBoxColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Box Color", ""), -16733441, true);
   }

   public static float getBoxOpacity() {
      return INSTANCE.getSetting("Box Opacity", 1.0F);
   }

   public static int getLineColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Line Color", ""), -16733441, false);
   }

   public static float getLineOpacity() {
      return INSTANCE.getSetting("Line Opacity", 1.0F);
   }

   public static float getLineWidth() {
      return INSTANCE.getSetting("Line Width", 2.0F);
   }

   private static int argb(int rgb, float opacity) {
      int a = Math.round(clamp01(opacity) * 255.0F);
      return a << 24 | rgb & 16777215;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static final class MultiBufferSourceHelper {
      static void drawPolyline(PoseStack poseStack, List<Vec3> points, int argb, float width) {
         for (int i = 0; i < points.size() - 1; i++) {
            ClarityRenderUtils.drawWorldLine(poseStack, points.get(i), points.get(i + 1), argb, width, true);
         }
      }
   }

   private record Projectile(Vec3 start, Vec3 dir, double speed, double gravity, double drag) {
   }
}
