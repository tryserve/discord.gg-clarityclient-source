package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.render.esp.SkeletonGlowRenderer;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

public class SkeletonESP extends ModuleBase {
   public static SkeletonESP INSTANCE;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;
   private double[] segs = new double[1536];
   private int[] cols = new int[256];
   private int[] hidCols = new int[256];
   private int count = 0;
   private static final int CIRCLE_SEGMENTS = 14;
   private static final double PX = 0.05859375;

   public SkeletonESP() {
      super("Skeleton ESP", "Visual");
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
            Player self = mc.player;
            boolean firstPerson = mc.options.getCameraType().isFirstPerson() && !Freecam.isActive();
            float maxDist = getMaxDistance();
            boolean fadeOn = isDistanceFade();
            float fadeDist = maxDist > 0.5F ? maxDist : 48.0F;
            boolean visColors = isVisibilityColors();
            Vec3 cam = ClarityRenderUtils.getCameraPos();
            double time = System.nanoTime() / 1.0E9;
            this.count = 0;

            for (Player p : mc.level.players()) {
               if ((p != self || !isHideSelf() && !firstPerson)
                  && p.isAlive()
                  && !p.isSpectator()
                  && (!isHideInvisible() || !p.isInvisible())
                  && (p == self || !isHideTeammates() || !isTeammate(p, self))) {
                  Vec3 pos = isSmooth() ? p.getPosition(partialTick) : p.position();
                  double dist = cam.distanceTo(pos);
                  if (!(maxDist > 0.5F) || !(dist > maxDist)) {
                     float fade = 1.0F;
                     if (fadeOn) {
                        fade = clamp01((float)(1.0 - dist / fadeDist));
                     }

                     if (!(fade <= 0.01F)) {
                        int rgb = colorFor(p, time, pos);
                        int hidden = visColors ? getHiddenColor() & 16777215 : rgb;
                        int alpha = Math.round(255.0F * fade * getOpacity());
                        int start = this.count;
                        this.buildSkeleton(p, partialTick, pos, cam);
                        int argb = alpha << 24 | rgb & 16777215;
                        int hargb = alpha << 24 | hidden;

                        for (int i = start; i < this.count; i++) {
                           this.cols[i] = argb;
                           this.hidCols[i] = hargb;
                        }
                     }
                  }
               }
            }

            if (this.count != 0) {
               boolean throughWalls = isThroughWalls();
               float width = getLineWidth();
               if (isGlow()) {
                  int override = "Custom".equals(getGlowColorMode()) ? getGlowColor() & 16777215 : -1;
                  SkeletonGlowRenderer.render(poseStack, this.segs, this.cols, this.count, throughWalls, width, getGlowRadius(), getGlowIntensity(), override);
               }

               if (throughWalls && visColors) {
                  ClarityRenderUtils.drawLineList(poseStack, this.segs, this.hidCols, this.count, width, true);
                  ClarityRenderUtils.drawLineList(poseStack, this.segs, this.cols, this.count, width, false);
               } else {
                  ClarityRenderUtils.drawLineList(poseStack, this.segs, this.cols, this.count, width, throughWalls);
               }
            }
         }
      }
   }

   private void buildSkeleton(Player p, float pt, Vec3 pos, Vec3 cam) {
      float bodyYaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
      float headYaw = Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot);
      float pitch = Mth.lerp(pt, p.xRotO, p.getXRot());
      double walkPos = p.walkAnimation.position(pt);
      double walkSpd = Math.min(1.0, p.walkAnimation.speed(pt));
      double phase = walkPos * 0.6662;
      double swingA = Math.cos(phase) * walkSpd;
      double swingL = Math.cos(phase) * 1.4 * walkSpd;
      boolean crouch = p.getPose() == Pose.CROUCHING;
      boolean lying = p.isFallFlying() || p.isVisuallySwimming();
      boolean leftMain = p.getMainArm() == HumanoidArm.LEFT;
      float attack = p.getAttackAnim(pt);
      double atkSwing = attack > 0.0F ? Math.sin(Math.sqrt(attack) * Math.PI) * 1.3 : 0.0;
      double yaw = Math.toRadians(bodyYaw);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double rx = -fz;
      double rz = fx;
      double s = 0.05859375 * p.getScale();
      double lean = crouch ? -0.5 : 0.0;
      double pelvisY = crouch ? 10.0 : 12.0;
      double[] pelvis = new double[]{0.0, pelvisY, crouch ? -1.0 : 0.0};
      double[] neck = rotAbout(new double[]{0.0, pelvisY + 12.0, pelvis[2]}, pelvis, lean);
      double[] shR = rotAbout(new double[]{5.0, pelvisY + 11.0, pelvis[2]}, pelvis, lean);
      double[] shL = rotAbout(new double[]{-5.0, pelvisY + 11.0, pelvis[2]}, pelvis, lean);
      double hy = Math.toRadians(Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85.0F, 85.0F));
      double hp = Math.toRadians(Mth.clamp(pitch, -89.0F, 89.0F));
      double[] headC = offset(neck, yawVec(pitchVec(new double[]{0.0, 4.2, 0.0}, -hp), hy));
      double[] headT = offset(neck, yawVec(pitchVec(new double[]{0.0, 8.0, 0.0}, -hp), hy));
      double armR = swingA + (crouch ? 0.4 : 0.0) + (leftMain ? 0.0 : atkSwing);
      double armL = -swingA + (crouch ? 0.4 : 0.0) + (leftMain ? atkSwing : 0.0);
      double[] elR = offset(shR, pitchVec(new double[]{0.0, -5.5, 0.0}, armR));
      double[] haR = offset(shR, pitchVec(new double[]{0.0, -11.0, 0.0}, armR));
      double[] elL = offset(shL, pitchVec(new double[]{0.0, -5.5, 0.0}, armL));
      double[] haL = offset(shL, pitchVec(new double[]{0.0, -11.0, 0.0}, armL));
      double[] hipR = new double[]{2.0, pelvisY, pelvis[2]};
      double[] hipL = new double[]{-2.0, pelvisY, pelvis[2]};
      double kneeFwd = crouch ? 3.0 : 0.0;
      double thigh = crouch ? -pelvisY * 0.5 : -6.0;
      double[] knR = offset(hipR, pitchVec(new double[]{0.0, thigh, kneeFwd}, -swingL));
      double[] ftR = offset(hipR, pitchVec(new double[]{0.0, -pelvisY, 0.0}, -swingL));
      double[] knL = offset(hipL, pitchVec(new double[]{0.0, thigh, kneeFwd}, swingL));
      double[] ftL = offset(hipL, pitchVec(new double[]{0.0, -pelvisY, 0.0}, swingL));
      double[][] all = new double[][]{pelvis, neck, shR, shL, headC, headT, elR, haR, elL, haL, hipR, hipL, knR, ftR, knL, ftL};
      if (lying) {
         double tilt = -Math.toRadians(90.0 + pitch);
         double[] pivot = new double[]{0.0, 4.0, 0.0};

         for (double[] j : all) {
            double[] r = rotAbout(j, pivot, tilt);
            j[0] = r[0];
            j[1] = r[1];
            j[2] = r[2];
         }
      }

      this.bone(neck, pelvis, pos, rx, rz, fx, fz, s);
      this.bone(shL, shR, pos, rx, rz, fx, fz, s);
      this.bone(shR, elR, pos, rx, rz, fx, fz, s);
      this.bone(elR, haR, pos, rx, rz, fx, fz, s);
      this.bone(shL, elL, pos, rx, rz, fx, fz, s);
      this.bone(elL, haL, pos, rx, rz, fx, fz, s);
      this.bone(hipL, hipR, pos, rx, rz, fx, fz, s);
      this.bone(hipR, knR, pos, rx, rz, fx, fz, s);
      this.bone(knR, ftR, pos, rx, rz, fx, fz, s);
      this.bone(hipL, knL, pos, rx, rz, fx, fz, s);
      this.bone(knL, ftL, pos, rx, rz, fx, fz, s);
      if (isHeadCircle()) {
         double[] neckTop = offsetToward(neck, headC, 3.4);
         this.bone(neck, neckTop, pos, rx, rz, fx, fz, s);
         this.headCircle(this.toWorld(headC, pos, rx, rz, fx, fz, s), 3.4 * s, cam);
      } else {
         this.bone(neck, headT, pos, rx, rz, fx, fz, s);
      }
   }

   private void headCircle(double[] c, double radius, Vec3 cam) {
      double dx = c[0] - cam.x;
      double dy = c[1] - cam.y;
      double dz = c[2] - cam.z;
      double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(dl < 1.0E-4)) {
         dx /= dl;
         dy /= dl;
         dz /= dl;
         double ux = -dy * dx;
         double uy = 1.0 - dy * dy;
         double uz = -dy * dz;
         double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
         if (ul < 1.0E-4) {
            ux = 1.0;
            uy = 0.0;
            uz = 0.0;
            ul = 1.0;
         }

         ux /= ul;
         uy /= ul;
         uz /= ul;
         double vx = dy * uz - dz * uy;
         double vy = dz * ux - dx * uz;
         double vz = dx * uy - dy * ux;
         double px = 0.0;
         double py = 0.0;
         double pz = 0.0;

         for (int i = 0; i <= 14; i++) {
            double a = (Math.PI * 2) * i / 14.0;
            double ca = Math.cos(a) * radius;
            double sa = Math.sin(a) * radius;
            double x = c[0] + ux * ca + vx * sa;
            double y = c[1] + uy * ca + vy * sa;
            double z = c[2] + uz * ca + vz * sa;
            if (i > 0) {
               this.push(px, py, pz, x, y, z);
            }

            px = x;
            py = y;
            pz = z;
         }
      }
   }

   private void bone(double[] a, double[] b, Vec3 pos, double rx, double rz, double fx, double fz, double s) {
      double[] wa = this.toWorld(a, pos, rx, rz, fx, fz, s);
      double ax = wa[0];
      double ay = wa[1];
      double az = wa[2];
      double[] wb = this.toWorld(b, pos, rx, rz, fx, fz, s);
      if (!(Math.abs(ax - wb[0]) + Math.abs(ay - wb[1]) + Math.abs(az - wb[2]) < 1.0E-6)) {
         this.push(ax, ay, az, wb[0], wb[1], wb[2]);
      }
   }

   private double[] toWorld(double[] j, Vec3 pos, double rx, double rz, double fx, double fz, double s) {
      return new double[]{pos.x + (j[0] * rx + j[2] * fx) * s, pos.y + j[1] * s, pos.z + (j[0] * rz + j[2] * fz) * s};
   }

   private void push(double ax, double ay, double az, double bx, double by, double bz) {
      if (this.count >= this.cols.length) {
         int n = this.cols.length * 2;
         this.segs = Arrays.copyOf(this.segs, n * 6);
         this.cols = Arrays.copyOf(this.cols, n);
         this.hidCols = Arrays.copyOf(this.hidCols, n);
      }

      int o = this.count * 6;
      this.segs[o] = ax;
      this.segs[o + 1] = ay;
      this.segs[o + 2] = az;
      this.segs[o + 3] = bx;
      this.segs[o + 4] = by;
      this.segs[o + 5] = bz;
      this.count++;
   }

   private static double[] pitchVec(double[] v, double a) {
      double c = Math.cos(a);
      double sn = Math.sin(a);
      return new double[]{v[0], v[1] * c + v[2] * sn, -v[1] * sn + v[2] * c};
   }

   private static double[] yawVec(double[] v, double a) {
      double c = Math.cos(a);
      double sn = Math.sin(a);
      return new double[]{v[0] * c + v[2] * sn, v[1], -v[0] * sn + v[2] * c};
   }

   private static double[] rotAbout(double[] p, double[] pivot, double a) {
      double[] d = pitchVec(new double[]{p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2]}, a);
      return new double[]{pivot[0] + d[0], pivot[1] + d[1], pivot[2] + d[2]};
   }

   private static double[] offset(double[] base, double[] d) {
      return new double[]{base[0] + d[0], base[1] + d[1], base[2] + d[2]};
   }

   private static double[] offsetToward(double[] a, double[] b, double dist) {
      double dx = b[0] - a[0];
      double dy = b[1] - a[1];
      double dz = b[2] - a[2];
      double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(l < 1.0E-6) && !(dist <= 0.0)) {
         double k = Math.max(0.0, l - dist) / l;
         return new double[]{a[0] + dx * k, a[1] + dy * k, a[2] + dz * k};
      } else {
         return new double[]{b[0], b[1], b[2]};
      }
   }

   private static int colorFor(Player p, double time, Vec3 pos) {
      switch (getColorMode()) {
         case "Health":
            float f = p.getMaxHealth() > 0.0F ? clamp01(p.getHealth() / p.getMaxHealth()) : 1.0F;
            int r = Math.round(255.0F * Math.min(1.0F, 2.0F - 2.0F * f));
            int g = Math.round(255.0F * Math.min(1.0F, 2.0F * f));
            return r << 16 | g << 8;
         case "Team":
            int c = p.getTeamColor() & 16777215;
            return c == 16777215 && p.getTeam() == null ? getColor() & 16777215 : c;
         case "Chroma":
            float hue = (float)(time * getChromaSpeed() + pos.x * 0.03 + pos.z * 0.03);
            return hsv(hue);
         default:
            return getColor() & 16777215;
      }
   }

   private static int hsv(float hue) {
      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      int i = (int)h;
      float f = h - i;
      float q = 1.0F - f;
      float t = f;
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = 1.0F;
            g = t;
            b = 0.0F;
            break;
         case 1:
            r = q;
            g = 1.0F;
            b = 0.0F;
            break;
         case 2:
            r = 0.0F;
            g = 1.0F;
            b = t;
            break;
         case 3:
            r = 0.0F;
            g = q;
            b = 1.0F;
            break;
         case 4:
            r = t;
            g = 0.0F;
            b = 1.0F;
            break;
         default:
            r = 1.0F;
            g = 0.0F;
            b = q;
      }

      return Math.round(r * 255.0F) << 16 | Math.round(g * 255.0F) << 8 | Math.round(b * 255.0F);
   }

   private static boolean isTeammate(Player a, Player b) {
      PlayerTeam ta = a.getTeam();
      PlayerTeam tb = b.getTeam();
      if (ta != null && tb != null) {
         String na = ta.getName();
         return na != null && na.equals(tb.getName());
      } else {
         return false;
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   public static String getColorMode() {
      return INSTANCE.getSetting("Color Mode", "Custom");
   }

   public static int getColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Color", ""), -1, false);
   }

   public static float getChromaSpeed() {
      return INSTANCE.getSetting("Chroma Speed", 0.3F);
   }

   public static float getLineWidth() {
      return Math.max(0.5F, INSTANCE.getSetting("Line Width", 1.5F));
   }

   public static float getOpacity() {
      return clamp01(INSTANCE.getSetting("Opacity", 1.0F));
   }

   public static boolean isHeadCircle() {
      return INSTANCE.getSetting("Head Circle", true);
   }

   public static boolean isThroughWalls() {
      return INSTANCE.getSetting("Through Walls", true);
   }

   public static boolean isVisibilityColors() {
      return INSTANCE.getSetting("Visibility Colors", false);
   }

   public static int getHiddenColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Hidden Color", ""), -50373, true);
   }

   public static boolean isGlow() {
      return INSTANCE.getSetting("Glow", false);
   }

   public static String getGlowColorMode() {
      return INSTANCE.getSetting("Glow Color Mode", "Match Lines");
   }

   public static int getGlowColor() {
      return ThemeColors.resolveArgb(INSTANCE.getSetting("Glow Color", ""), -16733441, true);
   }

   public static float getGlowRadius() {
      return Math.max(1.0F, Math.min(40.0F, INSTANCE.getSetting("Glow Radius", 8.0F)));
   }

   public static float getGlowIntensity() {
      return Math.max(0.0F, Math.min(3.0F, INSTANCE.getSetting("Glow Intensity", 1.2F)));
   }

   public static float getMaxDistance() {
      return INSTANCE.getSetting("Max Distance", 0.0F);
   }

   public static boolean isDistanceFade() {
      return INSTANCE.getSetting("Distance Fade", false);
   }

   public static boolean isHideInvisible() {
      return INSTANCE.getSetting("Hide Invisible", true);
   }

   public static boolean isHideTeammates() {
      return INSTANCE.getSetting("Hide Teammates", false);
   }

   public static boolean isHideSelf() {
      return INSTANCE.getSetting("Hide Self", true);
   }

   public static boolean isSmooth() {
      return INSTANCE.getSetting("Smooth", true);
   }
}
