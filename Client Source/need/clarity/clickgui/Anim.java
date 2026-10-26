package need.clarity.clickgui;

import java.util.HashMap;
import java.util.Map;

public final class Anim {
   private final Map<String, Anim.Val> vals = new HashMap<>();
   private float dt = 0.016666668F;
   private float speed = 1.0F;
   private boolean active;

   public void frame(float dt, float speedMul) {
      this.dt = Math.max(0.0F, Math.min(dt, 0.05F));
      this.speed = Math.max(0.1F, speedMul);
      this.active = false;
   }

   public boolean busy() {
      return this.active;
   }

   public float to(String key, float target, float rate) {
      Anim.Val s = this.vals.computeIfAbsent(key, kx -> new Anim.Val());
      if (!s.init) {
         s.v = target;
         s.init = true;
         return target;
      }

      float k = 1.0F - (float)Math.exp(-rate * this.speed * this.dt);
      s.v = s.v + (target - s.v) * k;
      if (Math.abs(target - s.v) < 0.001F) {
         s.v = target;
      } else {
         this.active = true;
      }

      return s.v;
   }

   public float from(String key, float from, float target, float rate) {
      Anim.Val s = this.vals.get(key);
      if (s == null) {
         s = new Anim.Val();
         s.v = from;
         s.init = true;
         this.vals.put(key, s);
      }

      return this.to(key, target, rate);
   }

   public float spring(String key, float target, float stiffness, float damping) {
      Anim.Val s = this.vals.computeIfAbsent(key, k -> new Anim.Val());
      if (!s.init) {
         s.v = target;
         s.init = true;
         return target;
      }

      float t = this.dt * this.speed;
      int steps = Math.max(1, (int)Math.ceil(t / 0.008F));
      float h = t / steps;

      for (int i = 0; i < steps; i++) {
         float f = (target - s.v) * stiffness - s.vel * damping;
         s.vel += f * h;
         s.v = s.v + s.vel * h;
      }

      if (Math.abs(target - s.v) < 0.001F && Math.abs(s.vel) < 0.001F) {
         s.v = target;
         s.vel = 0.0F;
      } else {
         this.active = true;
      }

      return s.v;
   }

   public void set(String key, float v) {
      Anim.Val s = this.vals.computeIfAbsent(key, k -> new Anim.Val());
      s.v = v;
      s.vel = 0.0F;
      s.init = true;
   }

   public void reset(String key) {
      this.vals.remove(key);
   }

   public void resetPrefix(String prefix) {
      this.vals.keySet().removeIf(k -> k.startsWith(prefix));
   }

   public static float ease(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   public static float easeOutBack(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(t - 1.0F, 3.0) + c1 * (float)Math.pow(t - 1.0F, 2.0);
   }

   public static float easeOutCubic(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - (float)Math.pow(1.0F - t, 3.0);
   }

   private static final class Val {
      float v;
      float vel;
      boolean init;
   }
}
