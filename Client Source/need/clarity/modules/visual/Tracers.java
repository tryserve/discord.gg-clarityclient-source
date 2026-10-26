package need.clarity.modules.visual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.List;
import need.clarity.WorldRenderEvent;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.render.worldfx.WorldFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Tracers extends ModuleBase {
   private static Tracers instance;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   public static final String STYLE_ARROWS = "Arrows";
   private static final String STYLE_ARROWS_LEGACY = "Crosshair Arrows";
   private static final int BATCH = 64;
   private final float[] posBuf = new float[256];
   private final float[] colBuf = new float[256];

   public Tracers() {
      super("Tracers", "Visual");
      instance = this;
      WorldRenderEvent.register(this.renderListener);
   }

   public float thickness() {
      return this.getSetting("Thickness", 2.0F);
   }

   public static boolean crosshairArrowsActive() {
      Tracers t = instance;
      if (t != null && t.isEnabled()) {
         String st = t.getSetting("Style", "Lines");
         return "Arrows".equals(st) || "Crosshair Arrows".equals(st);
      } else {
         return false;
      }
   }

   public String arrowImageName() {
      return switch ((String)this.getSetting("Arrow Image", "Smooth")) {
         case "Triangle 1" -> "arrow_triangle1";
         case "Triangle 2" -> "arrow_triangle2";
         default -> "arrow_smooth";
      };
   }

   public float arrowCentreBias() {
      return switch ((String)this.getSetting("Arrow Image", "Smooth")) {
         case "Triangle 1" -> 0.052F;
         case "Triangle 2" -> 0.071F;
         default -> 0.098F;
      };
   }

   public float arrowDistance() {
      return this.getSetting("Arrow Distance", 60.0F);
   }

   public float arrowSize() {
      return this.getSetting("Arrow Scale", 22.0F);
   }

   private Tracers.Buckets collect(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      int alpha = this.getSetting("Alpha", 200);
      int playerArgb = parseColor(this.getSetting("Player Colour", "#ff4444"), alpha, false);
      int mobArgb = parseColor(this.getSetting("Mob Colour", "#44ff44"), alpha, true);
      int baseArgb = parseColor(this.getSetting("Tracer Colour", "#ffb52e"), alpha, false);
      boolean trackPlayers = this.isTracking("Players");
      boolean trackMobs = this.isTracking("Mobs");
      boolean trackEntities = this.isTracking("Entities");
      List<Vec3> players = new java.util.ArrayList<>();
      List<Vec3> mobs = new java.util.ArrayList<>();
      List<Vec3> entities = new java.util.ArrayList<>();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e != mc.player && e.isAlive()) {
            Vec3 center = new Vec3(e.xOld + (e.getX() - e.xOld) * partialTick, e.yOld + (e.getY() - e.yOld) * partialTick + e.getBbHeight() * 0.5, e.zOld + (e.getZ() - e.zOld) * partialTick);
            if (trackPlayers && e instanceof Player) {
               players.add(center);
            } else if (trackMobs && e instanceof Mob) {
               mobs.add(center);
            } else if (trackEntities && !(e instanceof LivingEntity)) {
               entities.add(center);
            }
         }
      }

      return new Tracers.Buckets(players, mobs, entities, playerArgb, mobArgb, withAlpha(baseArgb, alpha / 2));
   }

   public List<Tracers.ArrowTarget> collectCrosshairTargets(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      List<Tracers.ArrowTarget> out = new java.util.ArrayList<>();
      if (mc.level != null && mc.player != null) {
         Tracers.Buckets b = this.collect(partialTick);

         for (Vec3 v : b.players()) {
            out.add(new Tracers.ArrowTarget(v, b.playerArgb()));
         }

         for (Vec3 v : b.mobs()) {
            out.add(new Tracers.ArrowTarget(v, b.mobArgb()));
         }

         for (Vec3 v : b.entities()) {
            out.add(new Tracers.ArrowTarget(v, b.entityArgb()));
         }

         Vec3 eye = ClarityRenderUtils.getCameraPos();
         out.sort(Comparator.<Tracers.ArrowTarget>comparingDouble(t -> t.pos().distanceToSqr(eye)).reversed());
         return out;
      } else {
         return out;
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         if (!crosshairArrowsActive()) {
            List<Tracers.ArrowTarget> targets = this.collectCrosshairTargets(partialTick);
            if (!targets.isEmpty()) {
               Vec3 cam = ClarityRenderUtils.getCameraPos();
               RenderTarget main = mc.getMainRenderTarget();
               Window win = mc.getWindow();
               if (main != null && win != null) {
                  float sw = main.width;
                  float sh = main.height;
                  double gs = win.getGuiScale();
                  int guiW = Math.max(1, win.getGuiScaledWidth());
                  int guiH = Math.max(1, win.getGuiScaledHeight());
                  float cxPix = (float)(((guiW - 15) / 2 + 7.5) * gs);
                  float cyPix = (float)(((guiH - 15) / 2 + 7.5) * gs);
                  float sx = cxPix / Math.max(1, win.getWidth()) * 2.0F - 1.0F;
                  float sy = 1.0F - cyPix / Math.max(1, win.getHeight()) * 2.0F;
                  float width = Math.max(0.25F, this.thickness());

                  for (int from = 0; from < targets.size(); from += 64) {
                     int n = Math.min(64, targets.size() - from);

                     for (int i = 0; i < n; i++) {
                        Tracers.ArrowTarget t = targets.get(from + i);
                        int k = i * 4;
                        int c = t.argb();
                        this.posBuf[k] = (float)(t.pos().x - cam.x);
                        this.posBuf[k + 1] = (float)(t.pos().y - cam.y);
                        this.posBuf[k + 2] = (float)(t.pos().z - cam.z);
                        this.posBuf[k + 3] = 0.0F;
                        this.colBuf[k] = (c >> 16 & 0xFF) / 255.0F;
                        this.colBuf[k + 1] = (c >> 8 & 0xFF) / 255.0F;
                        this.colBuf[k + 2] = (c & 0xFF) / 255.0F;
                        this.colBuf[k + 3] = (c >>> 24 & 0xFF) / 255.0F;
                     }

                     WorldFx.drawInstanced("tracer", n, true, 0, s -> {
                        s.setUniform2f("uStart", sx, sy);
                        s.setUniform2f("uScreen", sw, sh);
                        s.setUniform1f("uWidth", width);
                        s.setUniform4fv("uPos", this.posBuf);
                        s.setUniform4fv("uCol", this.colBuf);
                     });
                  }
               }
            }
         }
      }
   }

   private boolean isTracking(String category) {
      try {
         JsonObject modObj = ClarityConfig.getCurrent().getAsJsonObject("Tracers");
         if (modObj == null) {
            return true;
         }

         JsonObject values = modObj.getAsJsonObject("values");
         if (values == null) {
            return true;
         }

         JsonElement el = values.get("Tracer Mode");
         if (el != null && el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) {
               if (item.isJsonPrimitive() && item.getAsString().equals(category)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } catch (Exception e) {
         return true;
      }
   }

   private static int parseColor(String hex, int alpha, boolean secondary) {
      int rgb = ThemeColors.resolveRgb(hex, 16758062, secondary);
      return (alpha & 0xFF) << 24 | rgb & 16777215;
   }

   private static int withAlpha(int argb, int newAlpha) {
      return argb & 16777215 | (newAlpha & 0xFF) << 24;
   }

   public static Tracers getInstance() {
      return instance;
   }

   public record ArrowTarget(Vec3 pos, int argb) {
   }

   private record Buckets(List<Vec3> players, List<Vec3> mobs, List<Vec3> entities, int playerArgb, int mobArgb, int entityArgb) {
   }
}
