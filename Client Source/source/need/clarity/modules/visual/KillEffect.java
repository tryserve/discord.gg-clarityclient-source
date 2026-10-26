package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import need.clarity.WorldRenderEvent;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.visual.cosmetics.Attach;
import need.clarity.modules.visual.cosmetics.Builder;
import need.clarity.modules.visual.cosmetics.kill.KillEffects;
import need.clarity.modules.visual.cosmetics.kill.KillParams;
import need.clarity.modules.visual.cosmetics.kill.KillStyle;
import need.clarity.render.worldfx.KillFx;
import need.clarity.render.worldfx.ScanFx;
import need.clarity.render.worldfx.ShapeFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class KillEffect extends ModuleBase {
   public static KillEffect INSTANCE;
   private final Map<Integer, Float> lastHealth = new HashMap<>();
   private final KillEffects effects = new KillEffects();
   private final Map<Long, Integer> heightCache = new HashMap<>();
   private Object lastLevel = null;
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private boolean listenerRegistered = false;

   public KillEffect() {
      super("Kill Effect", "Visual");
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
      this.lastHealth.clear();
      this.effects.clear();
      this.heightCache.clear();
      this.lastLevel = null;
      KillFx.clear();
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

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         if (mc.level != this.lastLevel) {
            this.lastLevel = mc.level;
            this.lastHealth.clear();
            this.effects.clear();
            ScanFx.clear();
         }

         double maxDistance = this.getSetting("Max Distance", 0.0F).floatValue();
         Vec3 camPos = ClarityRenderUtils.getCameraPos();
         this.lastHealth.keySet().removeIf(id -> mc.level.getEntity(id) == null);

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le) {
               float hp = le.getHealth();
               Float prev = this.lastHealth.put(le.getId(), hp);
               if (prev != null && prev > 0.0F && hp <= 0.0F && (!(maxDistance > 0.5) || !(camPos.distanceTo(le.position()) > maxDistance))) {
                  this.spawn(le);
               }
            }
         }
      }
   }

   private KillStyle style() {
      KillStyle s = KillStyle.find(this.getSetting("Effect", "Shockwave"));
      return s != null ? s : KillStyle.SHOCKWAVE;
   }

   private void spawn(LivingEntity le) {
      if ("Wireframe Scan".equals(this.getSetting("Effect", "Shockwave"))) {
         Vec3 c = le.getBoundingBox().getCenter();
         ScanFx.spawn(c.x, c.y, c.z, this.getSetting("Scan Range", 14.0F), this.getSetting("Scan Speed", 10.0F));
      } else {
         int shader = KillFx.style(this.getSetting("Effect", "Shockwave"));
         if (shader >= 0) {
            Vec3 p = le.position();
            KillFx.spawn(shader, p.x, p.y, p.z, le.getBbWidth(), le.getBbHeight());
         } else {
            KillStyle style = this.style();
            double maxRadius = this.getSetting("Max Radius", 5.0F).floatValue();
            double life;
            if (style == KillStyle.SHOCKWAVE) {
               double speed = Math.max(0.5, this.getSetting("Speed", 8.0F).floatValue());
               life = Math.max(0.1, maxRadius / speed);
            } else {
               life = Math.max(0.5, Math.min(10.0, this.getSetting("Duration", 2.5F).floatValue()));
            }

            int segs = this.getSetting("Points", 64);
            long nanos = System.nanoTime();
            Vec3 pos = le.position();
            this.effects
               .spawn(new KillEffects.Active(style, pos.x, pos.y, pos.z, le.getBbWidth(), le.getBbHeight(), nanos / 1.0E9, life, maxRadius, segs, KillEffects.seedFor(le.getId(), nanos / 1000000L)));
         }
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null && mc.player != null) {
            if (!ScanFx.isEmpty()) {
               int s1 = ThemeColors.resolveRgb(this.getSetting("Color", ""), -12845218, false) & 16777215;
               int s2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -3604522, true) & 16777215;
               float[] f1 = ShapeFx.rgb(s1);
               float[] f2 = ShapeFx.rgb(s2);

               for (int i = 0; i < 3; i++) {
                  f1[i] = f1[i] * 0.45F + 0.55F;
               }

               ScanFx.render(partialTick, clamp01(this.getSetting("Opacity", 0.9F)), this.getSetting("Through Walls", true), f1, f2);
            }

            int c1 = ThemeColors.resolveRgb(this.getSetting("Color", ""), -12845218, false) & 16777215;
            int c2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -3604522, true) & 16777215;
            KillFx.render(
               Math.max(0.1F, this.getSetting("Scale", 1.0F)),
               clamp01(this.getSetting("Opacity", 0.9F)),
               this.getSetting("Through Walls", true),
               this.getSetting("Natural Colors", true),
               ShapeFx.rgb(c1),
               ShapeFx.rgb(c2)
            );
            double now = System.nanoTime() / 1.0E9;
            this.effects.prune(now);
            if (!this.effects.isEmpty()) {
               float scale = Math.max(0.1F, this.getSetting("Scale", 1.0F));
               float opacity = clamp01(this.getSetting("Opacity", 0.9F));
               boolean throughWalls = this.getSetting("Through Walls", true);
               boolean natural = this.getSetting("Natural Colors", true);
               int color = ThemeColors.resolveRgb(this.getSetting("Color", ""), -12845218, false) & 16777215;
               int color2 = ThemeColors.resolveRgb(this.getSetting("Second Color", ""), -3604522, true) & 16777215;
               boolean chamsLayered = Chams.INSTANCE != null && Chams.INSTANCE.isEnabled() && Chams.isThroughWalls();
               int depthMode;
               if (chamsLayered) {
                  depthMode = 2;
               } else {
                  depthMode = throughWalls ? 1 : 0;
               }

               Vec3 cam = ClarityRenderUtils.getCameraPos();
               this.heightCache.clear();
               ClarityRenderUtils.CosmeticBatch batch = ClarityRenderUtils.beginCosmetics(poseStack, depthMode);

               for (KillEffects.Active a : this.effects.active()) {
                  Attach.Frame mount = Attach.FEET.frame(Attach.Subject.standing(a.x(), a.y(), a.z(), 0.0, a.height()));
                  KillParams params = a.params(now, this.groundFor(mc, mount, scale));
                  int c1x = color;
                  int c2x = color2;
                  if (a.style().hasNatural() && natural) {
                     c1x = a.style().natural1;
                     c2x = a.style().natural2;
                  }

                  Builder builder = new Builder(batch, c1x, c2x, opacity, params.age(), true);
                  builder.view = new double[]{cam.x, cam.y, cam.z};
                  builder.frame(mount, scale);
                  a.style().create(params).build(builder);
               }

               batch.end();
            }
         }
      }
   }

   private KillParams.Ground groundFor(Minecraft mc, Attach.Frame mount, double scale) {
      return (u, w) -> {
         double[] p = mount.map(u * scale, 0.0, w * scale);
         int bx = (int)Math.floor(p[0]);
         int bz = (int)Math.floor(p[2]);
         long key = (long)bx << 32 ^ bz & 4294967295L;
         Integer top = this.heightCache.get(key);
         if (top == null) {
            top = mc.level.getHeight(Types.MOTION_BLOCKING, bx, bz);
            this.heightCache.put(key, top);
         }

         double y = Math.abs(top.intValue() - mount.oy) > 2.0 ? mount.oy : top.intValue();
         return (y - mount.oy) / scale;
      };
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
