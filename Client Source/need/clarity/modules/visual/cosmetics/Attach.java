package need.clarity.modules.visual.cosmetics;

public enum Attach {
   HEAD,
   BACK,
   FEET,
   CENTER,
   HAND;

   public static final double HEAD_HALF = 0.234;
   public static final double NECK_TO_EYE = 0.214;
   public static final double HEAD_TOP = 0.234;
   public static final double EYE_STAND = 1.62;
   public static final double EYE_SNEAK = 1.27;
   public static final double EYE_PRONE = 0.4;
   public static final double BACK_OFFSET = 0.14;
   public static final double BACK_HEIGHT = 0.8;

   public Attach.Frame frame(Attach.Subject s) {
      return switch (this) {
         case HEAD -> head(s);
         case BACK -> back(s);
         case FEET -> upright(s, s.x(), s.y(), s.z());
         case CENTER -> upright(s, s.x(), s.y() + s.height() * 0.5, s.z());
         case HAND -> hand(s);
      };
   }

   private static Attach.Frame upright(Attach.Subject s, double x, double y, double z) {
      double yaw = Math.toRadians(s.bodyYawDeg());
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      return new Attach.Frame(x, y, z, -fz, 0.0, fx, 0.0, 1.0, 0.0, fx, 0.0, fz);
   }

   private static Attach.Frame back(Attach.Subject s) {
      double yaw = Math.toRadians(s.bodyYawDeg());
      double sinY = -Math.sin(yaw);
      double cosY = Math.cos(yaw);
      if (!s.swimming() && !s.fallFlying()) {
         double heightMul = s.sneaking() ? 0.72 : 0.8;
         return new Attach.Frame(s.x() - sinY * 0.14, s.y() + s.height() * heightMul, s.z() - cosY * 0.14, -cosY, 0.0, sinY, 0.0, 1.0, 0.0, sinY, 0.0, cosY);
      } else {
         double pitch = Math.toRadians(Math.max(-89.0, Math.min(89.0, s.headPitchDeg())) * s.followTilt());
         double cp = Math.cos(pitch);
         double sp = Math.sin(pitch);
         double ffx = sinY * cp;
         double ffy = -sp;
         double ffz = cosY * cp;
         double rrx = -cosY;
         double rry = 0.0;
         double rrz = -sinY;
         double uux = rry * ffz - rrz * ffy;
         double uuy = rrz * ffx - rrx * ffz;
         double uuz = rrx * ffy - rry * ffx;
         double proneHeight = 0.4;
         double ox = s.x() - ffx * 0.14;
         double oy = s.y() + proneHeight + uuy * 0.15 - ffx * 0.0;
         double oz = s.z() - ffz * 0.14;
         return new Attach.Frame(ox, oy, oz, rrx, rry, rrz, uux, uuy, uuz, ffx, ffy, ffz);
      }
   }

   private static Attach.Frame hand(Attach.Subject s) {
      Attach.Frame f = upright(s, s.x(), s.y(), s.z());
      double side = 0.37;
      double up = s.height() * 0.42;
      return new Attach.Frame(f.ox + f.rx * side, f.oy + up, f.oz + f.rz * side, f.rx, f.ry, f.rz, f.ux, f.uy, f.uz, f.fx, f.fy, f.fz);
   }

   private static Attach.Frame head(Attach.Subject s) {
      double yaw = Math.toRadians(s.headYawDeg());
      double pitch = Math.toRadians(Math.max(-89.0, Math.min(89.0, s.headPitchDeg()))) * s.followTilt();
      double cp = Math.cos(pitch);
      double sp = Math.sin(pitch);
      double fx = -Math.sin(yaw) * cp;
      double fy = -sp;
      double fz = Math.cos(yaw) * cp;
      double rx = -Math.cos(yaw);
      double ry = 0.0;
      double rz = -Math.sin(yaw);
      double ux = ry * fz - rz * fy;
      double uy = rz * fx - rx * fz;
      double uz = rx * fy - ry * fx;
      double neckY = s.y() + s.eyeHeight() - 0.214;
      return new Attach.Frame(s.x() + ux * 0.234, neckY + uy * 0.234, s.z() + uz * 0.234, rx, ry, rz, ux, uy, uz, fx, fy, fz);
   }

   public static final class Frame {
      public final double ox;
      public final double oy;
      public final double oz;
      public final double rx;
      public final double ry;
      public final double rz;
      public final double ux;
      public final double uy;
      public final double uz;
      public final double fx;
      public final double fy;
      public final double fz;

      public Frame(double ox, double oy, double oz, double rx, double ry, double rz, double ux, double uy, double uz, double fx, double fy, double fz) {
         this.ox = ox;
         this.oy = oy;
         this.oz = oz;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
         this.ux = ux;
         this.uy = uy;
         this.uz = uz;
         this.fx = fx;
         this.fy = fy;
         this.fz = fz;
      }

      public static Attach.Frame identity() {
         return new Attach.Frame(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);
      }

      public double[] map(double u, double v, double w) {
         return new double[]{this.ox + this.rx * u + this.ux * v + this.fx * w, this.oy + this.ry * u + this.uy * v + this.fy * w, this.oz + this.rz * u + this.uz * v + this.fz * w};
      }

      public double[] map(double[] p) {
         return this.map(p[0], p[1], p[2]);
      }

      public Attach.Frame shifted(double du, double dv, double dw) {
         double[] o = this.map(du, dv, dw);
         return new Attach.Frame(o[0], o[1], o[2], this.rx, this.ry, this.rz, this.ux, this.uy, this.uz, this.fx, this.fy, this.fz);
      }
   }

   public record Subject(
      double x, double y, double z, double bodyYawDeg, double headYawDeg, double headPitchDeg, double height, boolean sneaking, boolean swimming, boolean fallFlying, double followTilt
   ) {
      public static Attach.Subject standing(double x, double y, double z, double bodyYawDeg, double height) {
         return new Attach.Subject(x, y, z, bodyYawDeg, bodyYawDeg, 0.0, height, false, false, false, 1.0);
      }

      public Attach.Subject withHead(double yawDeg, double pitchDeg) {
         return new Attach.Subject(this.x, this.y, this.z, this.bodyYawDeg, yawDeg, pitchDeg, this.height, this.sneaking, this.swimming, this.fallFlying, this.followTilt);
      }

      public Attach.Subject withPose(boolean sneak, boolean swim, boolean fly) {
         return new Attach.Subject(this.x, this.y, this.z, this.bodyYawDeg, this.headYawDeg, this.headPitchDeg, this.height, sneak, swim, fly, this.followTilt);
      }

      public Attach.Subject withTilt(double follow) {
         return new Attach.Subject(this.x, this.y, this.z, this.bodyYawDeg, this.headYawDeg, this.headPitchDeg, this.height, this.sneaking, this.swimming, this.fallFlying, follow);
      }

      public double eyeHeight() {
         return !this.swimming && !this.fallFlying ? (this.sneaking ? 1.27 : 1.62) : 0.4;
      }
   }
}
