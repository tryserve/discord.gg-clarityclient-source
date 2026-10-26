package need.clarity.render;

import java.util.Arrays;

public final class SwingEngine {
   public static final int X = 0;
   public static final int Y = 1;
   public static final int Z = 2;
   public static final int YAW = 3;
   public static final int ROLL = 4;
   public static final int CHOP = 5;
   public static final int FLIP = 6;
   public static final int N = 7;
   public static final String[] MODES = new String[]{"Smooth", "Stab", "Slice", "Windmill", "Combo Slash", "Blade Dance", "Heavy Charge", "Momentum", "Whirlwind"};
   private static final float COMBO_WINDOW = 0.75F;
   private static final float RETURN_TIME = 0.35F;
   public static final float[] pose = new float[7];
   private static final float[] vel = new float[7];
   private static final float[] target = new float[7];
   private static final float[] from = new float[7];
   private static final float[] to = new float[7];
   private static String mode = "";
   private static float lastP = 0.0F;
   private static int combo = 0;
   private static float sinceEnd = 99.0F;
   private static boolean swinging = false;
   private static float momentum = 0.0F;
   private static float spin = 0.0F;
   private static float whirl = 0.0F;
   private static float time = 0.0F;

   private SwingEngine() {
   }

   public static void step(String m, float p, float dt, float str) {
      if (!m.equals(mode)) {
         reset();
         mode = m;
      }

      dt = Math.max(0.0F, Math.min(dt, 0.05F));
      time += dt;
      boolean hit = p > 0.001F && lastP <= 0.001F || p < lastP - 0.25F && p > 0.0F;
      if (hit) {
         boolean chain = swinging || sinceEnd < 0.75F;
         combo = chain ? combo + 1 : 0;
         swinging = true;
         System.arraycopy(pose, 0, from, 0, 7);
         if ("Momentum".equals(mode)) {
            momentum = Math.min(6.0F, momentum + 1.0F);
         }

         if ("Whirlwind".equals(mode)) {
            whirl += 90.0F;
         }
      }

      if (swinging && p <= 0.001F && !hit) {
         swinging = false;
         sinceEnd = 0.0F;
      }

      if (!swinging) {
         sinceEnd += dt;
      }

      lastP = p;
      compute(p, dt, 0.75F + 0.5F * str);

      for (int i = 5; i <= 6; i++) {
         while (pose[i] - target[i] > 180.0F) {
            pose[i] = pose[i] - 360.0F;
         }

         while (pose[i] - target[i] < -180.0F) {
            pose[i] = pose[i] + 360.0F;
         }
      }

      boolean smoothMode = "Smooth".equals(mode);
      float omega = smoothMode ? 20.0F : 34.0F;
      float zeta = smoothMode ? 0.55F : 0.82F;
      int sub = Math.max(1, (int)Math.ceil(dt * 240.0F));
      float h = dt / sub;

      for (int s = 0; s < sub; s++) {
         for (int i = 0; i < 7; i++) {
            float a = omega * omega * (target[i] - pose[i]) - 2.0F * zeta * omega * vel[i];
            vel[i] = vel[i] + a * h;
            pose[i] = pose[i] + vel[i] * h;
         }
      }
   }

   public static void reset() {
      Arrays.fill(pose, 0.0F);
      Arrays.fill(vel, 0.0F);
      Arrays.fill(target, 0.0F);
      Arrays.fill(from, 0.0F);
      Arrays.fill(to, 0.0F);
      combo = 0;
      sinceEnd = 99.0F;
      swinging = false;
      lastP = 0.0F;
      momentum = 0.0F;
      spin = 0.0F;
      whirl = 0.0F;
   }

   public static boolean handles(String m) {
      for (String s : MODES) {
         if (s.equals(m)) {
            return true;
         }
      }

      return false;
   }

   private static float clamp01(float t) {
      return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
   }

   private static float smooth(float t) {
      t = clamp01(t);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float outCubic(float t) {
      t = 1.0F - clamp01(t);
      return 1.0F - t * t * t;
   }

   private static float inOut(float t) {
      t = clamp01(t);
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   private static float outBack(float t) {
      t = clamp01(t) - 1.0F;
      float c = 1.9F;
      return 1.0F + (c + 1.0F) * t * t * t + c * t * t;
   }

   private static float bump(float t) {
      return (float)Math.sin(Math.PI * clamp01(t));
   }

   private static float sinD(float deg) {
      return (float)Math.sin(Math.toRadians(deg));
   }

   private static float cosD(float deg) {
      return (float)Math.cos(Math.toRadians(deg));
   }

   private static void set(float[] o, float x, float y, float z, float yaw, float roll, float chop, float flip) {
      o[0] = x;
      o[1] = y;
      o[2] = z;
      o[3] = yaw;
      o[4] = roll;
      o[5] = chop;
      o[6] = flip;
   }

   private static void blend(float e) {
      for (int i = 0; i < 7; i++) {
         target[i] = from[i] + (to[i] - from[i]) * e;
      }
   }

   private static boolean settle() {
      if (!swinging && !(sinceEnd < 0.75F)) {
         float k = smooth((sinceEnd - 0.75F) / 0.35F);

         for (int i = 0; i < 7; i++) {
            target[i] = to[i] * (1.0F - k);
         }

         if (k >= 1.0F) {
            combo = 0;
            Arrays.fill(to, 0.0F);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void compute(float p, float dt, float amp) {
      float sq = (float)Math.sqrt(p);
      float sp = swinging ? p : 1.0F;
      float b = bump(p);
      switch (mode) {
         case "Smooth":
            set(target, -0.32F * bump(sq) * amp, 0.2F * (float)Math.sin(sq * 2.0F * Math.PI), -0.2F * b, -20.0F * (float)Math.sin(p * p * Math.PI), -20.0F * bump(sq), -80.0F * bump(sq) * amp, 0.0F);
            break;
         case "Stab":
            float t = p < 0.35F ? outCubic(p / 0.35F) : 1.0F - smooth((p - 0.35F) / 0.65F);
            set(target, -0.08F * t, 0.06F * t, -0.42F * t * amp, -12.0F * t, 70.0F * t, 12.0F * t, 0.0F);
            break;
         case "Slice":
            float e = inOut(p);
            set(target, (0.15F - 0.55F * e) * b * amp, 0.05F * b, -0.18F * b, (55.0F - 120.0F * e) * b * amp, -45.0F * b, -35.0F * b, 0.0F);
            break;
         case "Windmill":
            set(target, -0.12F * b, 0.1F * b, -0.15F * b, 0.0F, -10.0F * b, -360.0F * inOut(p), 0.0F);
            break;
         case "Combo Slash":
            if (settle()) {
               return;
            }

            int st = combo % 3;
            switch (st) {
               case 0:
                  set(to, -0.26F * amp, -0.04F, -0.08F, -38.0F * amp, 32.0F, -68.0F * amp, 0.0F);
                  break;
               case 1:
                  set(to, 0.12F * amp, 0.1F, -0.06F, 42.0F * amp, -38.0F, -18.0F, 0.0F);
                  break;
               default:
                  set(to, -0.05F, -0.06F, -0.18F, 0.0F, 0.0F, -100.0F * amp, -360.0F);
            }

            blend(st == 2 ? inOut(sp) : outBack(sp));
            target[2] = target[2] - 0.1F * b;
            if (st == 2) {
               target[1] = target[1] + 0.12F * bump(Math.min(1.0F, p * 1.6F));
            }
            break;
         case "Blade Dance":
            if (settle()) {
               return;
            }

            switch (combo % 4) {
               case 0:
                  set(to, 0.02F, 0.04F, -0.04F, 15.0F, 10.0F, -25.0F, -360.0F);
                  blend(inOut(sp));
                  target[0] = target[0] - 0.12F * b;
                  return;
               case 1:
                  set(to, -0.05F, 0.04F, -0.1F, -8.0F, 80.0F, 10.0F, -360.0F);
                  blend(outBack(sp));
                  target[2] = target[2] - 0.3F * b * amp;
                  return;
               case 2:
                  set(to, -0.3F * amp, 0.0F, -0.08F, -50.0F * amp, 20.0F, -55.0F, -360.0F);
                  blend(outBack(sp));
                  return;
               default:
                  set(to, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -90.0F * amp, -1080.0F);
                  blend(inOut(sp));
                  target[1] = target[1] + 0.1F * b;
                  target[2] = target[2] - 0.15F * b;
                  return;
            }
         case "Heavy Charge":
            if (settle()) {
               return;
            }

            int st4 = combo % 4;
            if (st4 < 3) {
               float c = st4 + 1;
               set(to, -0.04F * c, 0.05F * c, 0.01F * c, -6.0F * c, 12.0F * c * amp, 0.0F, 0.0F);
               blend(outBack(sp));
               target[5] = target[5] - 14.0F * b;
               float shake = 0.0045F * c * c;
               target[0] = target[0] + shake * (float)Math.sin(time * 61.0F);
               target[1] = target[1] + shake * (float)Math.sin(time * 47.0F + 1.0F);
            } else {
               set(to, -0.04F, -0.16F, -0.25F, -6.0F, -12.0F, -115.0F * amp, -90.0F);
               blend(sp < 0.45F ? outCubic(sp / 0.45F) : 1.0F);
               float rec = smooth((sp - 0.55F) / 0.45F);

               for (int i = 0; i < 7; i++) {
                  target[i] = target[i] * (1.0F - 0.75F * rec);
               }
            }
            break;
         case "Momentum":
            momentum = Math.max(0.0F, momentum - dt * (swinging ? 0.4F : 1.3F));
            spin = spin - (float)Math.pow(momentum, 1.35) * 120.0F * dt;
            if (momentum < 0.05F) {
               spin = Math.round(spin / 360.0F) * 360.0F;
            }

            if (spin < -36000.0F) {
               spin += 36000.0F;
            }

            float m = momentum / 6.0F;
            set(target, -0.06F * m - 0.1F * b, 0.05F * m + 0.04F * b, -0.1F * m - 0.12F * b, -10.0F * m, 20.0F * m, -20.0F * m * amp - 30.0F * b, spin);
            break;
         case "Whirlwind":
            if (!swinging && sinceEnd > 0.95F) {
               whirl = Math.round(whirl / 360.0F) * 360.0F;
            }

            if (whirl > 36000.0F) {
               whirl -= 36000.0F;
            }

            float a = whirl;
            set(target, 0.18F * sinD(a) * amp - 0.08F * b, 0.06F * b, -0.06F * (1.0F - cosD(a)) - 0.1F * b, 30.0F * sinD(a), -15.0F * b + 25.0F * sinD(a), -50.0F * b * amp, 0.0F);
            break;
         default:
            Arrays.fill(target, 0.0F);
      }
   }
}

