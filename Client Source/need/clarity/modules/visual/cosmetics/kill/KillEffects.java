package need.clarity.modules.visual.cosmetics.kill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KillEffects {
   public static final int DEFAULT_MAX = 8;
   private final ArrayList<KillEffects.Active> list = new ArrayList<>();
   private final int max;

   public KillEffects() {
      this(8);
   }

   public KillEffects(int max) {
      this.max = Math.max(1, max);
   }

   public void spawn(KillEffects.Active a) {
      this.list.add(a);

      while (this.list.size() > this.max) {
         this.list.remove(0);
      }
   }

   public void prune(double now) {
      this.list.removeIf(a -> a.expired(now));
   }

   public void clear() {
      this.list.clear();
   }

   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public int size() {
      return this.list.size();
   }

   public List<KillEffects.Active> active() {
      return Collections.unmodifiableList(this.list);
   }

   public static int seedFor(int entityId, long stamp) {
      long x = entityId * -7046029254386353131L + stamp * -4658895280553007687L;
      x ^= x >>> 31;
      x *= -7723592293110705685L;
      x ^= x >>> 29;
      return (int)x;
   }

   public record Active(KillStyle style, double x, double y, double z, double width, double height, double start, double life, double reach, int segs, int seed) {
      public double age(double now) {
         return now - this.start;
      }

      public boolean expired(double now) {
         return now - this.start >= this.life;
      }

      public KillParams params(double now, KillParams.Ground ground) {
         return new KillParams(this.width, this.height, Math.max(0.0, this.age(now)), this.life, this.reach, this.segs, this.seed, ground);
      }
   }
}
