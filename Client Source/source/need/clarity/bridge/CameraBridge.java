package need.clarity.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import need.clarity.modules.visual.Aspect;
import need.clarity.modules.visual.CameraTweaks;
import need.clarity.modules.visual.Freecam;
import need.clarity.modules.visual.Freelook;
import need.clarity.modules.visual.SmoothCamera;
import need.clarity.modules.visual.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class CameraBridge {
   private static Method MOVE;
   private static Method MAX_ZOOM;
   private static Method SET_ROTATION;

   private CameraBridge() {
   }

   public static boolean isFreecamActive() {
      try {
         return Freecam.isActive();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onExtractRenderStateTail(Object stateObj, float partialTick) {
      try {
         if (!Aspect.isActive()) {
            return;
         }

         CameraRenderState state = (CameraRenderState)stateObj;
         if (state.projectionMatrix == null) {
            return;
         }

         state.projectionMatrix.scale(Aspect.factor(), 1.0F, 1.0F);
      } catch (Throwable var3) {
      }
   }

   public static void onAfterAlignWithEntity(Object cameraObj) {
      try {
         if (!Freecam.isActive()) {
            return;
         }

         Camera camera = (Camera)cameraObj;
         Field detached = Camera.class.getDeclaredField("detached");
         detached.setAccessible(true);
         detached.setBoolean(camera, true);
         Method setPos = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
         setPos.setAccessible(true);
         setPos.invoke(camera, Freecam.getCameraPos());
         Method setRot = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
         setRot.setAccessible(true);
         setRot.invoke(camera, Freecam.cameraYaw, Freecam.cameraPitch);
      } catch (Throwable var5) {
      }
   }

   public static void onUpdateReturn(Object cameraObj, Object dtObj) {
      try {
         Camera camera = (Camera)cameraObj;
         DeltaTracker dt = (DeltaTracker)dtObj;
         Minecraft mc = Minecraft.getInstance();
         Entity e = camera.entity();
         if (mc == null || mc.level == null || mc.player == null || e == null || !camera.isDetached()) {
            SmoothCamera.smoothPosition(null, null, 0.0F);
            return;
         }

         Vec3 p = SmoothCamera.smoothPosition(camera.position(), e, camera.getCameraEntityPartialTicks(dt));
         if (p != null) {
            Method setPos = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            setPos.setAccessible(true);
            setPos.invoke(camera, p);
         }
      } catch (Throwable var8) {
      }
   }

   public static Vec3 getFreecamPosition() {
      try {
         return !Freecam.isActive() ? null : Freecam.getCameraPos();
      } catch (Throwable t) {
         return null;
      }
   }

   public static void onBeforeGetMaxZoom(Object cameraObj) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.level == null || mc.player == null) {
            return;
         }

         Camera camera = (Camera)cameraObj;
         float[] r = SmoothCamera.smooth(camera.yRot(), camera.xRot());
         if (r != null) {
            Method setRot = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            setRot.setAccessible(true);
            setRot.invoke(camera, r[0], r[1]);
         }
      } catch (Throwable var5) {
      }
   }

   public static boolean cameraTweaks(Object cameraObj) {
      try {
         if (!CameraTweaks.isActive()) {
            return false;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc != null && !mc.options.getCameraType().isFirstPerson()) {
            Camera camera = (Camera)cameraObj;
            Entity entity = camera.entity();
            float scaleFactor = 1.0F;
            if (entity instanceof LivingEntity living) {
               scaleFactor = living.getScale() * (float)living.getAttributeValue(Attributes.CAMERA_DISTANCE) / 4.0F;
            }

            if (entity != null && entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
               float d = vehicle.getScale() * (float)vehicle.getAttributeValue(Attributes.CAMERA_DISTANCE);
               if (d / 4.0F > scaleFactor) {
                  scaleFactor = d / 4.0F;
               }
            }

            float baseDistance = CameraTweaks.customDistanceEnabled() ? CameraTweaks.customDistance() : 4.0F;
            float distance = baseDistance * scaleFactor;
            if (MOVE == null) {
               MOVE = Camera.class.getDeclaredMethod("move", float.class, float.class, float.class);
               MOVE.setAccessible(true);
               MAX_ZOOM = Camera.class.getDeclaredMethod("getMaxZoom", float.class);
               MAX_ZOOM.setAccessible(true);
            }

            float back = CameraTweaks.noCameraClip() ? distance : (Float)MAX_ZOOM.invoke(camera, distance);
            MOVE.invoke(camera, -back, 0.0F, 0.0F);
            return true;
         } else {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   public static void freelookSetRotation(Camera camera, float yaw, float pitch) {
      try {
         if (Freelook.isActive()) {
            yaw = Freelook.yaw;
            pitch = Freelook.pitch;
         }
      } catch (Throwable var5) {
      }

      try {
         if (SET_ROTATION == null) {
            SET_ROTATION = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            SET_ROTATION.setAccessible(true);
         }

         SET_ROTATION.invoke(camera, yaw, pitch);
      } catch (Throwable var4) {
      }
   }

   public static float zoomFov(float fov) {
      try {
         return Zoom.applyFov(fov);
      } catch (Throwable t) {
         return fov;
      }
   }
}
