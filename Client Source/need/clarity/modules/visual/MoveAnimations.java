package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Pose;

public class MoveAnimations extends ModuleBase {
   public static MoveAnimations INSTANCE;
   private static AvatarRenderState localState;
   private static float airBlend;
   private static float climbBlend;
   private static float sprintBlend;
   private static float fallBlend;
   private static long lastNanos;

   public MoveAnimations() {
      super("Move Animations", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   private static String mode() {
      String m = INSTANCE.getSetting("Style", "Ninja");

      return switch (m) {
         case "Levitate", "Zombie", "Robot", "Swagger" -> m;
         default -> "Ninja";
      };
   }

   public static void onExtract(Object entity, AvatarRenderState state) {
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         if (entity instanceof LocalPlayer p && p == mc.player) {
            localState = state;
            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
            lastNanos = now;
            float k = 1.0F - (float)Math.exp(-dt * 9.0F);
            boolean climbing = p.onClimbable() && !p.onGround();
            boolean air = !p.onGround() && !climbing && !p.isInWater();
            airBlend = airBlend + ((air ? 1.0F : 0.0F) - airBlend) * k;
            climbBlend = climbBlend + ((climbing ? 1.0F : 0.0F) - climbBlend) * k;
            sprintBlend = sprintBlend + ((p.isSprinting() ? 1.0F : 0.0F) - sprintBlend) * k;
            fallBlend = fallBlend + ((air && p.getDeltaMovement().y < -0.08 ? 1.0F : 0.0F) - fallBlend) * k;
         }
      }
   }

   public static void apply(HumanoidModel<?> m, HumanoidRenderState s) {
      if (active() && s instanceof AvatarRenderState) {
         boolean self = s == localState;
         if (self || INSTANCE.getSetting("Others Too", false)) {
            if (!s.isFallFlying && !s.isVisuallySwimming && !s.isPassenger && !s.isAutoSpinAttack && !s.hasPose(Pose.SLEEPING) && !(s.swimAmount > 0.05F) && !(s.deathTime > 0.0F)) {
               float pos = s.walkAnimationPos;
               float spd = Math.min(1.0F, s.walkAnimationSpeed);
               float age = s.ageInTicks;
               float air = self ? airBlend : 0.0F;
               float climb = self ? climbBlend : 0.0F;
               float sprint = self ? sprintBlend : (spd > 0.85F ? 1.0F : 0.0F);
               float fall = self ? fallBlend : 0.0F;
               boolean arms = s.attackTime <= 0.0F && !s.isUsingItem;
               float sw = (float)Math.cos(pos * 0.6662F);
               float strength = INSTANCE.getSetting("Strength", 1.0F);
               MoveAnimations.Pose3 t = new MoveAnimations.Pose3(m);
               switch (mode()) {
                  case "Levitate":
                     levitate(t, m, s, spd, age, air, climb);
                     break;
                  case "Zombie":
                     zombie(t, m, sw, spd, age, air, climb);
                     break;
                  case "Robot":
                     robot(t, m, pos, spd, age, air, climb);
                     break;
                  case "Swagger":
                     swagger(t, m, sw, spd, age, air, climb, sprint);
                     break;
                  default:
                     ninja(t, m, s, sw, spd, age, air, climb, sprint, fall);
               }

               t.write(m, Math.max(0.0F, Math.min(1.0F, strength)), arms);
            }
         }
      }
   }

   private static void ninja(MoveAnimations.Pose3 t, HumanoidModel<?> m, HumanoidRenderState s, float sw, float spd, float age, float air, float climb, float sprint, float fall) {
      float lean = s.isCrouching ? 0.0F : (0.1F + 0.32F * sprint) * spd * (1.0F - climb);
      t.lean(lean);
      float trail = 0.25F + 1.05F * sprint * spd;
      t.rArmX = trail + sw * 0.15F * spd;
      t.lArmX = trail - sw * 0.15F * spd;
      t.rArmZ = 0.1F + 0.12F * sprint;
      t.lArmZ = -0.1F - 0.12F * sprint;
      t.rLegX = sw * (1.2F + 0.5F * sprint) * spd;
      t.lLegX = -sw * (1.2F + 0.5F * sprint) * spd;
      t.rLegX = mix(t.rLegX, mix(-1.15F, -0.35F, fall), air);
      t.lLegX = mix(t.lLegX, mix(-0.35F, 0.25F, fall), air);
      t.rArmX = mix(t.rArmX, 1.35F, air);
      t.lArmX = mix(t.lArmX, 1.35F, air);
      t.rArmZ = mix(t.rArmZ, 0.3F, air);
      t.lArmZ = mix(t.lArmZ, -0.3F, air);
      climbPose(t, age, climb);
   }

   private static void levitate(MoveAnimations.Pose3 t, HumanoidModel<?> m, HumanoidRenderState s, float spd, float age, float air, float climb) {
      float bob = (float)Math.sin(age * 0.08F);
      t.lift = -1.3F - bob * 1.1F;
      t.lean(s.isCrouching ? 0.0F : 0.12F * spd);
      t.rLegX = 0.14F + 0.28F * spd + 0.05F * bob;
      t.lLegX = 0.2F + 0.28F * spd - 0.05F * bob;
      t.rLegZ = 0.035F;
      t.lLegZ = -0.035F;
      float open = 0.22F + 0.06F * (float)Math.sin(age * 0.1F) + 0.35F * air;
      t.rArmZ = open;
      t.lArmZ = -open;
      t.rArmX = 0.08F * spd + 0.05F * bob;
      t.lArmX = 0.08F * spd - 0.05F * bob;
      climbPose(t, age, climb * 0.6F);
   }

   private static void zombie(MoveAnimations.Pose3 t, HumanoidModel<?> m, float sw, float spd, float age, float air, float climb) {
      float sway = (float)Math.sin(age * 0.067F) * 0.05F;
      t.rArmX = -1.45F + sway + sw * 0.05F * spd;
      t.lArmX = -1.45F - sway - sw * 0.05F * spd;
      t.rArmZ = -0.06F;
      t.lArmZ = 0.06F;
      t.rLegX = sw * 0.7F * spd;
      t.lLegX = -sw * 0.7F * spd;
      t.headZ = (float)Math.sin(age * 0.05F) * 0.12F;
      t.rArmX = mix(t.rArmX, -2.0F, air);
      t.lArmX = mix(t.lArmX, -2.0F, air);
      climbPose(t, age, climb);
   }

   private static void robot(MoveAnimations.Pose3 t, HumanoidModel<?> m, float pos, float spd, float age, float air, float climb) {
      float q = Math.signum((float)Math.sin(pos * 0.6662F)) * Math.min(1.0F, spd * 3.0F);
      t.rArmX = -0.55F * q;
      t.lArmX = 0.55F * q;
      t.rLegX = 0.5F * q;
      t.lLegX = -0.5F * q;
      t.rArmZ = 0.0F;
      t.lArmZ = 0.0F;
      t.headSnap = 0.3927F;
      t.rArmX = mix(t.rArmX, -3.05F, air);
      t.lArmX = mix(t.lArmX, -3.05F, air);
      t.rLegX = mix(t.rLegX, 0.0F, air);
      t.lLegX = mix(t.lLegX, 0.0F, air);
      climbPose(t, age, climb);
   }

   private static void swagger(MoveAnimations.Pose3 t, HumanoidModel<?> m, float sw, float spd, float age, float air, float climb, float sprint) {
      float a = (1.0F + 0.3F * sprint) * spd;
      t.rArmX = sw * a;
      t.lArmX = -sw * a;
      t.rArmZ = 0.15F + 0.12F * Math.abs(sw) * spd;
      t.lArmZ = -0.15F - 0.12F * Math.abs(sw) * spd;
      t.rLegX = -sw * 1.1F * spd;
      t.lLegX = sw * 1.1F * spd;
      t.bodyZ = 0.07F * sw * spd;
      t.bodyY = 0.14F * sw * spd;
      t.headZ = -0.05F * sw * spd;
      t.rArmX = mix(t.rArmX, -0.6F, air);
      t.lArmX = mix(t.lArmX, 0.6F, air);
      climbPose(t, age, climb);
   }

   private static void climbPose(MoveAnimations.Pose3 t, float age, float climb) {
      if (!(climb <= 0.001F)) {
         float c = (float)Math.sin(age * 0.45F);
         t.rArmX = mix(t.rArmX, -2.55F + 0.45F * c, climb);
         t.lArmX = mix(t.lArmX, -2.55F - 0.45F * c, climb);
         t.rArmZ = mix(t.rArmZ, 0.12F, climb);
         t.lArmZ = mix(t.lArmZ, -0.12F, climb);
         t.rLegX = mix(t.rLegX, -0.55F - 0.45F * c, climb);
         t.lLegX = mix(t.lLegX, -0.55F + 0.45F * c, climb);
      }
   }

   private static float mix(float a, float b, float k) {
      return a + (b - a) * k;
   }

   private static final class Pose3 {
      float rArmX;
      float lArmX;
      float rArmZ;
      float lArmZ;
      float rLegX;
      float lLegX;
      float rLegZ;
      float lLegZ;
      float bodyZ;
      float bodyY;
      float headZ;
      float lift;
      float lean;
      float headSnap;

      Pose3(HumanoidModel<?> m) {
         this.rArmX = m.rightArm.xRot;
         this.lArmX = m.leftArm.xRot;
         this.rArmZ = m.rightArm.zRot;
         this.lArmZ = m.leftArm.zRot;
         this.rLegX = m.rightLeg.xRot;
         this.lLegX = m.leftLeg.xRot;
         this.rLegZ = m.rightLeg.zRot;
         this.lLegZ = m.leftLeg.zRot;
         this.bodyZ = m.body.zRot;
         this.bodyY = m.body.yRot;
         this.headZ = m.head.zRot;
      }

      void lean(float l) {
         this.lean = l;
      }

      void write(HumanoidModel<?> m, float k, boolean arms) {
         if (arms) {
            m.rightArm.xRot = MoveAnimations.mix(m.rightArm.xRot, this.rArmX, k);
            m.leftArm.xRot = MoveAnimations.mix(m.leftArm.xRot, this.lArmX, k);
            m.rightArm.zRot = MoveAnimations.mix(m.rightArm.zRot, this.rArmZ, k);
            m.leftArm.zRot = MoveAnimations.mix(m.leftArm.zRot, this.lArmZ, k);
         }

         m.rightLeg.xRot = MoveAnimations.mix(m.rightLeg.xRot, this.rLegX, k);
         m.leftLeg.xRot = MoveAnimations.mix(m.leftLeg.xRot, this.lLegX, k);
         m.rightLeg.zRot = MoveAnimations.mix(m.rightLeg.zRot, this.rLegZ, k);
         m.leftLeg.zRot = MoveAnimations.mix(m.leftLeg.zRot, this.lLegZ, k);
         m.body.zRot = MoveAnimations.mix(m.body.zRot, this.bodyZ, k);
         m.body.yRot = MoveAnimations.mix(m.body.yRot, this.bodyY, k);
         m.head.zRot = MoveAnimations.mix(m.head.zRot, this.headZ, k);
         if (this.headSnap > 0.0F) {
            float snapped = Math.round(m.head.yRot / this.headSnap) * this.headSnap;
            m.head.yRot = MoveAnimations.mix(m.head.yRot, snapped, k);
         }

         float l = this.lean * k;
         if (l > 0.001F) {
            m.body.xRot += l;
            m.head.xRot -= l * 0.6F;
            m.body.y += 6.4F * l;
            m.head.y += 8.4F * l;
            m.rightArm.y += 6.4F * l;
            m.leftArm.y += 6.4F * l;
            m.rightLeg.z += 8.0F * l;
            m.leftLeg.z += 8.0F * l;
            m.rightLeg.y += 0.4F * l;
            m.leftLeg.y += 0.4F * l;
         }

         float y = this.lift * k;
         if (y != 0.0F) {
            for (ModelPart p : new ModelPart[]{m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
               p.y += y;
            }
         }
      }
   }
}
