package need.clarity.nvg;

import com.mojang.blaze3d.platform.Window;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import need.clarity.clickgui.NvgCanvas;
import need.clarity.config.ClarityConfig;
import need.clarity.media.MediaBridge;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.visual.BpsCounter;
import need.clarity.modules.visual.ClarityIcon;
import need.clarity.modules.visual.CustomScoreboard;
import need.clarity.modules.visual.HUD;
import need.clarity.modules.visual.ItemTags;
import need.clarity.modules.visual.KeybindList;
import need.clarity.modules.visual.MusicPlayer;
import need.clarity.modules.visual.Nametags;
import need.clarity.modules.visual.Notifications;
import need.clarity.modules.visual.PotionHud;
import need.clarity.modules.visual.TNTTimer;
import need.clarity.modules.visual.TargetHud;
import need.clarity.modules.visual.Tracers;
import need.clarity.render.RawFramebufferUtil;
import need.clarity.util.Chroma;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public final class HudElements {
   private static final NVGColor COL = NVGColor.create();
   private static int lastSig = 0;
   private static long lastDrawNanos = 0L;
   private static boolean settling = true;
   private static NvgCanvas hudCanvas;
   private static long hudCanvasVg;
   private static final Map<String, Integer> POTION_MAX = new HashMap<>();
   private static final Map<String, Integer> EFFECT_IMG = new HashMap<>();
   private static float POTION_ANIM = 0.0F;
   private static int potionSig = 0;
   private static final Map<String, Float> HUD_ANIM = new HashMap<>();
   private static final Map<Integer, Integer> HEAD_IMAGE_CACHE = new HashMap<>();
   private static float CANVAS_W = 1.0F;
   private static float CANVAS_H = 1.0F;
   private static final Map<String, Float> KB_ANIM = new HashMap<>();
   private static float MUSIC_ANIM = 0.0F;
   private static int musicImg = 0;
   private static int musicImgVer = -1;
   private static int blurImg = 0;
   private static int blurImgTex = 0;
   private static int blurImgW = 0;
   private static int blurImgH = 0;
   private static float TGT_ANIM = 0.0F;
   private static String C_NAME = "";
   private static float C_RATIO = 1.0F;
   private static int C_DIST;
   private static String C_GEAR = "";
   private static boolean C_SHOWGEAR;
   private static boolean C_SHOWDIST;
   private static boolean C_SHOWHEALTH;

   private HudElements() {
   }

   public static boolean enabled() {
      HUD h = HUD.INSTANCE;
      TargetHud t = TargetHud.INSTANCE;
      ItemTags it = ItemTags.INSTANCE;
      TNTTimer tnt = TNTTimer.INSTANCE;
      if (HudLayout.isEditing()) {
         return true;
      } else if (Nametags.active()) {
         return true;
      } else if (BpsCounter.active() || ClarityIcon.active()) {
         return true;
      } else if (Notifications.hasActive()) {
         return true;
      } else if (KeybindList.INSTANCE != null && KeybindList.INSTANCE.isEnabled()) {
         return true;
      } else if (MusicPlayer.INSTANCE != null && MusicPlayer.INSTANCE.isEnabled()) {
         return true;
      } else if (PotionHud.INSTANCE != null && PotionHud.INSTANCE.isEnabled()) {
         return true;
      } else {
         return !ChatHud.active() && !ScoreboardHud.wanted()
            ? h != null && h.isEnabled() || t != null && t.isEnabled() || it != null && it.isEnabled() || tnt != null && tnt.isEnabled() || Tracers.crosshairArrowsActive()
            : true;
      }
   }

   public static boolean needsRedraw() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) {
         return true;
      }

      if (HudLayout.isEditing() || settling) {
         return true;
      }

      if (Nametags.active()) {
         return true;
      }

      if (Chroma.active()) {
         return true;
      }

      if (BpsCounter.active()) {
         return true;
      }

      if (ClarityIcon.active() && ClarityIcon.INSTANCE.animated()) {
         return true;
      }

      if (Notifications.hasActive() || Tracers.crosshairArrowsActive() || TNTTimer.active()) {
         return true;
      }

      if (HudFx.animating()) {
         return true;
      }

      if (ChatHud.animating() || ScoreboardHud.animating()) {
         return true;
      }

      if (ItemTags.INSTANCE != null && ItemTags.INSTANCE.isEnabled()) {
         return true;
      }

      if (MusicPlayer.INSTANCE != null && MusicPlayer.INSTANCE.isEnabled()) {
         return true;
      }

      if (potionsVisible(mc)) {
         return true;
      }

      TargetHud th = TargetHud.INSTANCE;
      if (th != null && th.isEnabled()) {
         try {
            if (TGT_ANIM > 0.005F || AimAssist.getCurrentTarget() != null) {
               return true;
            }
         } catch (Throwable t) {
            return true;
         }
      }

      return System.nanoTime() - lastDrawNanos > 250000000L ? true : signature(mc) != lastSig;
   }

   private static int signature(Minecraft mc) {
      int h = 17;
      HUD hud = HUD.INSTANCE;
      if (hud != null && hud.isEnabled()) {
         h = h * 31 + mc.getFps();
         h = h * 31 + ping(mc);
         h = h * 31 + (int)Math.floor(mc.player.getX());
         h = h * 31 + (int)Math.floor(mc.player.getY());
         h = h * 31 + (int)Math.floor(mc.player.getZ());
         h = h * 31 + ClarityConfig.valuesHash("HUD");
      }

      KeybindList kl = KeybindList.INSTANCE;
      if (kl != null && kl.isEnabled()) {
         for (KeybindList.Row r : kl.rows()) {
            h = h * 31 + r.name.hashCode() * (r.on ? 3 : 7) + (r.key == null ? 0 : r.key.hashCode());
         }

         h = h * 31 + ClarityConfig.valuesHash("Keybind List");
      }

      h = h * 31 + CustomScoreboard.signature();
      h = h * 31 + ThemeColors.getAccentRgb();
      h = h * 31 + ThemeColors.getTextRgb();
      return h * 31 + (hud != null && hud.isEnabled() ? 1 : 0) + (kl != null && kl.isEnabled() ? 2 : 0);
   }

   private static boolean animSettled(Map<String, Float> m) {
      for (float v : m.values()) {
         if (v > 0.005F && v < 0.995F) {
            return false;
         }
      }

      return true;
   }

   public static void draw(long vg, float w, float hgt) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null) {
         CANVAS_W = w;
         CANVAS_H = hgt;
         canvas(vg).begin(1, 1);

         try {
            NametagRenderer.draw(vg, canvas(vg), mc, w, hgt);
         } catch (Throwable var8) {
         }

         ChatHud.draw(vg, w, hgt);
         ScoreboardHud.draw(vg, w, hgt);
         drawInfo(vg, mc);
         drawKeybindList(vg, mc, w, hgt);

         try {
            MiscHud.drawBps(vg, w, hgt);
         } catch (Throwable var7) {
         }

         try {
            MiscHud.drawIcon(vg, w, hgt);
         } catch (Throwable var6) {
         }

         drawMusicPlayer(vg, mc, w, hgt);
         drawPotions(vg, mc, w, hgt);
         drawItemTags(vg, mc, w, hgt);
         drawTntTimers(vg, mc, w, hgt);
         drawTarget(vg, mc, w, hgt);
         drawNotifications(vg, mc, w, hgt);
         drawTracerArrows(vg, mc, w, hgt);
         HudLayout.drawOverlay(vg, w, hgt);
         settling = !animSettled(HUD_ANIM) || !animSettled(KB_ANIM);
         lastSig = signature(mc);
         lastDrawNanos = System.nanoTime();
      }
   }

   private static void drawTracerArrows(long vg, Minecraft mc, float w, float hgt) {
      if (Tracers.crosshairArrowsActive() && mc.level != null && mc.player != null) {
         Tracers t = Tracers.getInstance();
         int img = ClarityImages.get(vg, t.arrowImageName());
         if (img > 0) {
            float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Camera cam = mc.gameRenderer.getMainCamera();
            if (cam != null) {
               Vec3 eye = ClarityRenderUtils.getCameraPos();
               double yaw = Math.toRadians(cam.yRot());
               double pitch = Math.toRadians(cam.xRot());
               double cosP = Math.cos(pitch);
               double sinP = Math.sin(pitch);
               double cosY = Math.cos(yaw);
               double sinY = Math.sin(yaw);
               new Vec3(-sinY * cosP, -sinP, cosY * cosP);
               Vec3 r = new Vec3(-cosY, 0.0, -sinY);
               Vec3 u = new Vec3(-sinY * sinP, cosP, cosY * sinP).scale(1.0);
               Window win = mc.getWindow();
               int guiW = Math.max(1, win.getGuiScaledWidth());
               int guiH = Math.max(1, win.getGuiScaledHeight());
               float cx = ((guiW - 15) / 2 + 7.5F) / guiW * w;
               float cy = ((guiH - 15) / 2 + 7.5F) / guiH * hgt;
               float radius = t.arrowDistance();
               float bias = t.arrowCentreBias() * t.arrowSize();
               float size = t.arrowSize();

               for (Tracers.ArrowTarget at : t.collectCrosshairTargets(pt)) {
                  Vec3 ndc = null;

                  try {
                     ndc = mc.gameRenderer.projectPointToScreen(at.pos());
                  } catch (Throwable var50) {
                  }

                  double nx;
                  double ny;
                  if (ndc != null && ndc.z > 0.0 && ndc.z < 1.0 && Double.isFinite(ndc.x) && Double.isFinite(ndc.y)) {
                     double sx = (ndc.x * 0.5 + 0.5) * w;
                     double sy = (1.0 - (ndc.y * 0.5 + 0.5)) * hgt;
                     double vx = sx - cx;
                     double vy = sy - cy;
                     double vl = Math.hypot(vx, vy);
                     if (vl < 0.25) {
                        continue;
                     }

                     nx = vx / vl;
                     ny = vy / vl;
                  } else {
                     Vec3 d = at.pos().subtract(eye);
                     double dx = d.dot(r);
                     double dy = d.dot(u);
                     double len = Math.hypot(dx, dy);
                     if (len < 1.0E-6) {
                        continue;
                     }

                     nx = dx / len;
                     ny = -dy / len;
                  }

                  float angle = (float)Math.atan2(nx, -ny);
                  double ring = radius + bias;
                  ClarityImages.draw(vg, img, (float)(cx + nx * ring), (float)(cy + ny * ring), size, angle, at.argb());
               }
            }
         }
      }
   }

   private static void drawTntTimers(long vg, Minecraft mc, float w, float hgt) {
      if (TNTTimer.active() && mc.level != null) {
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof PrimedTnt tnt) {
               Vec3 ndc;
               try {
                  double ix = tnt.xOld + (tnt.getX() - tnt.xOld) * pt;
                  double iy = tnt.yOld + (tnt.getY() - tnt.yOld) * pt;
                  double iz = tnt.zOld + (tnt.getZ() - tnt.zOld) * pt;
                  ndc = mc.gameRenderer.projectPointToScreen(new Vec3(ix, iy + 1.1, iz));
               } catch (Throwable t) {
                  continue;
               }

               if (ndc != null && !(ndc.z <= 0.0) && !(ndc.z >= 1.0)) {
                  float sx = (float)((ndc.x * 0.5 + 0.5) * w);
                  float sy = (float)((1.0 - (ndc.y * 0.5 + 0.5)) * hgt);
                  String txt = String.format("%.1fs", Math.max(0.0F, tnt.getFuse() - pt) / 20.0F);
                  float tw = ClarityDraw.textWidth(vg, "inter-sb", 8.0F, 0.0F, txt);
                  ClarityDraw.fillRRect(vg, sx - tw / 2.0F - 4.0F, sy - 6.0F, tw + 8.0F, 12.0F, 3.0F, -687865856 | ThemeColors.getSurfaceRgb() & 16777215);
                  HudFx.panel(vg, "TNT Timer", sx - tw / 2.0F - 4.0F, sy - 6.0F, tw + 8.0F, 12.0F, 3.0F, 1.0F);
                  ClarityDraw.strokeRRect(vg, sx - tw / 2.0F - 4.0F, sy - 6.0F, tw + 8.0F, 12.0F, 3.0F, 1.0F, 0xFF000000 | ThemeColors.getBorderRgb());
                  ClarityDraw.text(vg, "inter-sb", 8.0F, 0.0F, 0xFF000000 | ThemeColors.getTextRgb(), 18, sx, sy, txt);
               }
            }
         }
      }
   }

   private static void drawItemTags(long vg, Minecraft mc, float w, float hgt) {
      ItemTags it = ItemTags.INSTANCE;
      if (it != null && it.isEnabled() && mc.level != null) {
         float scale = it.scale();
         NanoVG.nvgSave(vg);
         NanoVG.nvgScale(vg, scale, scale);

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ItemEntity ie && !(mc.player.distanceTo(ie) > it.maxDistance())) {
               Vec3 ndc;
               try {
                  ndc = mc.gameRenderer.projectPointToScreen(new Vec3(ie.getX(), ie.getY() + 0.4, ie.getZ()));
               } catch (Throwable t) {
                  continue;
               }

               if (ndc != null && !(ndc.z <= 0.0) && !(ndc.z >= 1.0)) {
                  float sx = (float)((ndc.x * 0.5 + 0.5) * w) / scale;
                  float sy = (float)((1.0 - (ndc.y * 0.5 + 0.5)) * hgt) / scale;
                  ItemStack stack = ie.getItem();
                  String name = stack.getHoverName().getString();
                  if (name.length() > 16) {
                     name = name.substring(0, 16);
                  }

                  if (it.showCount() && stack.getCount() > 1) {
                     name = name + " x" + stack.getCount();
                  }

                  float tw = ClarityDraw.textWidth(vg, "inter-med", 6.5F, 0.0F, name);
                  if (it.background()) {
                     setARGB(COL, ThemeColors.getSurfaceRgb(), 0.65F);
                     ClarityDraw.fillRRect(vg, sx - tw / 2.0F - 3.0F, sy - 5.0F, tw + 6.0F, 10.0F, 2.0F, COL);
                     HudFx.panel(vg, "Item Tags", sx - tw / 2.0F - 3.0F, sy - 5.0F, tw + 6.0F, 10.0F, 2.0F, 1.0F);
                     ClarityDraw.strokeRRect(vg, sx - tw / 2.0F - 3.0F, sy - 5.0F, tw + 6.0F, 10.0F, 2.0F, 1.0F, 0xFF000000 | ThemeColors.getBorderRgb());
                  }

                  setARGB(COL, ThemeColors.getTextRgb(), 0.95F);
                  ClarityDraw.text(vg, "inter-med", 6.5F, 0.0F, COL, 18, sx, sy, name);
               }
            }
         }

         NanoVG.nvgRestore(vg);
      }
   }

   private static NvgCanvas canvas(long vg) {
      if (hudCanvas == null || hudCanvasVg != vg) {
         hudCanvas = new NvgCanvas(vg);
         hudCanvas.setCrisp(false);
         hudCanvasVg = vg;
      }

      return hudCanvas;
   }

   public static List<NvgCanvas.Icon> icons() {
      return hudCanvas == null ? List.of() : hudCanvas.icons();
   }

   static boolean potionsVisible(Minecraft mc) {
      PotionHud ph = PotionHud.INSTANCE;
      return ph != null && ph.isEnabled() && mc.player != null ? HudLayout.isEditing() || !mc.player.getActiveEffects().isEmpty() || POTION_ANIM > 0.01F : false;
   }

   private static String fmtTicks(int ticks) {
      int s = Math.max(0, ticks / 20);
      return s >= 3600 ? s / 3600 + ":" + String.format("%02d:%02d", s / 60 % 60, s % 60) : s / 60 + ":" + String.format("%02d", s % 60);
   }

   private static String roman(int n) {
      String[] r = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
      return n >= 1 && n <= 10 ? r[n - 1] : String.valueOf(n);
   }

   private static int effectImage(long vg, String path) {
      Integer c = EFFECT_IMG.get(path);
      if (c != null) {
         return c;
      }

      int id = 0;

      try (InputStream in = Minecraft.getInstance().getResourceManager().open(Identifier.parse(path))) {
         byte[] bytes = in.readAllBytes();
         ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
         buf.put(bytes).flip();
         id = NanoVG.nvgCreateImageMem(vg, 32, buf);
      } catch (Throwable var10) {
      }

      EFFECT_IMG.put(path, id);
      return id;
   }

   private static List<HudElements.Fx> collectEffects(Minecraft mc, PotionHud ph) {
      List<HudElements.Fx> out = new ArrayList<>();
      Set<String> seen = new HashSet<>();

      for (MobEffectInstance inst : mc.player.getActiveEffects()) {
         try {
            if (!ph.hideAmbient() || !inst.isAmbient()) {
               MobEffect eff = (MobEffect)inst.getEffect().value();
               Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(eff);
               String key = id == null ? eff.getDescriptionId() : id.toString();
               String path = id == null ? null : id.getNamespace() + ":textures/mob_effect/" + id.getPath() + ".png";
               String name = Component.translatable(eff.getDescriptionId()).getString();
               int ticks = inst.getDuration();
               boolean inf = inst.isInfiniteDuration();
               int max = Math.max(ticks, POTION_MAX.getOrDefault(key, 0));
               POTION_MAX.put(key, max);
               seen.add(key);
               float frac = inf ? 1.0F : (max <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)ticks / max)));
               HudElements.Fx fx = new HudElements.Fx(path == null ? key : path, name, inst.getAmplifier() + 1, ticks, inf, eff.getColor() & 16777215, eff.isBeneficial(), frac);
               out.add(fx);
            }
         } catch (Throwable var16) {
         }
      }

      POTION_MAX.keySet().retainAll(seen);
      if (out.isEmpty() && HudLayout.isEditing()) {
         out.add(new HudElements.Fx("minecraft:textures/mob_effect/speed.png", "Speed", 2, 1800, false, 3402751, true, 0.75F));
         out.add(new HudElements.Fx("minecraft:textures/mob_effect/strength.png", "Strength", 1, 900, false, 16762624, true, 0.4F));
         out.add(new HudElements.Fx("minecraft:textures/mob_effect/poison.png", "Poison", 1, 160, false, 8889187, false, 0.12F));
      }

      switch (ph.sort()) {
         case "Name":
            out.sort(Comparator.comparing(f -> f.name));
            break;
         case "Type":
            out.sort(Comparator.<HudElements.Fx, Boolean>comparing(f -> !f.good).thenComparing(f -> f.name));
            break;
         default:
            out.sort(Comparator.<HudElements.Fx, Boolean>comparing(f -> f.infinite).thenComparingInt(f -> f.ticks));
      }

      if (out.size() > ph.maxRows()) {
         out = new ArrayList<>(out.subList(0, ph.maxRows()));
      }

      return out;
   }

   private static void drawPotions(long vg, Minecraft mc, float cw, float ch) {
      PotionHud ph = PotionHud.INSTANCE;
      if (ph != null && ph.isEnabled() && mc.player != null) {
         List<HudElements.Fx> list = collectEffects(mc, ph);
         POTION_ANIM = POTION_ANIM + ((list.isEmpty() ? 0.0F : 1.0F) - POTION_ANIM) * 0.18F;
         if (!(POTION_ANIM < 0.01F) && !list.isEmpty()) {
            float sc = ph.scale();
            float op = ph.opacity() * POTION_ANIM;
            boolean compact = "Compact".equals(ph.layout());
            int accent = ThemeColors.getAccentRgb();
            int text = ThemeColors.getTextRgb();
            int muted = ThemeColors.getMutedRgb();
            int surface = mix(ThemeColors.getSurfaceRgb(), accent, 0.02F);
            int borderCol = ThemeColors.getBorderRgb();
            float pad = 8.0F * sc;
            float icon = (compact ? 16.0F : 18.0F) * sc;
            float rowH = (compact ? 22.0F : 30.0F) * sc;
            float gap = 3.0F * sc;
            float nameSz = 12.0F * sc;
            float timeSz = 11.0F * sc;
            long nowMs = System.currentTimeMillis();
            float w;
            float h;
            if (compact) {
               float sum = 0.0F;

               for (HudElements.Fx f : list) {
                  sum += icon + 6.0F * sc + ClarityDraw.textWidth(vg, "inter-med", timeSz, 0.0F, f.time) + 12.0F * sc;
               }

               w = pad * 2.0F + sum - 6.0F * sc;
               h = pad * 2.0F + rowH;
            } else {
               w = ph.width() * sc;
               h = pad * 2.0F + list.size() * rowH + (list.size() - 1) * gap;
            }

            float mx = ph.offsetX();
            float my = ph.offsetY();
            float y;
            float x;
            switch (ph.anchor()) {
               case "Top Right":
                  x = cw - w - mx;
                  y = my;
                  break;
               case "Top Left":
                  x = mx;
                  y = my;
                  break;
               case "Bottom Right":
                  x = cw - w - mx;
                  y = ch - h - my;
                  break;
               default:
                  x = mx;
                  y = ch - h - my;
            }

            x = Math.round(x);
            y = Math.round(y);
            HudLayout.report("Potion HUD", x, y, w, h);
            float cr = ph.cornerRadius() * sc;
            if (!LiquidGlass.panel(vg, "Potion HUD", x, y, w, h, cr, op) && ph.background()) {
               boolean blurred = ph.blur() && fillBlurRRect(vg, x, y, w, h, cr, op);
               ClarityDraw.fillRRect(vg, x, y, w, h, cr, tintA(surface, (blurred ? ph.blurTint() : ph.bgOpacity()) * op));
               HudFx.panel(vg, "Potion HUD", x, y, w, h, cr, op);
               if (ph.border()) {
                  ClarityDraw.strokeRRect(vg, x, y, w, h, cr, 1.0F, tintA(mix(borderCol, accent, 0.1F), op));
               }

               if (ph.highlight()) {
                  int hl = mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.12F);
                  ClarityDraw.topHighlight(vg, x, y, w, h, cr, tintA(hl, 0.66F * op));
               }
            }

            float cx = x + pad;
            float cy = y + pad;

            for (HudElements.Fx f : list) {
               int col = ph.effectColors() ? brighten(f.rgb) : (f.good ? accent : 16733296);
               boolean ending = !f.infinite && f.ticks < 200;
               float blink = ending && ph.blinkEnding() ? 0.55F + 0.45F * (float)Math.cos(nowMs / 160.0) : 1.0F;
               float a = op * blink;
               if (compact) {
                  float iy = cy + (rowH - icon) * 0.5F;
                  if (ph.showIcons()) {
                     drawEffectIcon(vg, f, cx, iy, icon, col, a);
                  }

                  float tx = cx + (ph.showIcons() ? icon + 4.0F * sc : 0.0F);
                  ClarityDraw.text(vg, "inter-med", timeSz, 0.0F, tintA(ending ? col : text, a), 17, tx, cy + rowH * 0.5F + 0.5F, f.time);
                  cx = tx + ClarityDraw.textWidth(vg, "inter-med", timeSz, 0.0F, f.time) + 12.0F * sc;
               } else {
                  float ix = cx;
                  float iy = cy + (rowH - icon) * 0.5F - (ph.showBar() ? 2.0F * sc : 0.0F);
                  if (ph.showIcons()) {
                     drawEffectIcon(vg, f, ix, iy, icon, col, a);
                  }

                  float tx = ix + (ph.showIcons() ? icon + 8.0F * sc : 0.0F);
                  float right = x + w - pad;
                  float midY = iy + icon * 0.5F + 0.5F;
                  String label = f.name + (ph.showLevel() && f.amp > 1 ? " " + (ph.romanLevel() ? roman(f.amp) : String.valueOf(f.amp)) : "");
                  float timeW = ph.showTime() ? ClarityDraw.textWidth(vg, "inter-med", timeSz, 0.0F, f.time) : 0.0F;
                  label = potionEllipsize(vg, label, nameSz, right - tx - timeW - 8.0F * sc);
                  ClarityDraw.text(vg, "inter-sb", nameSz, 0.0F, tintA(text, a), 17, tx, midY, label);
                  if (ph.showTime()) {
                     ClarityDraw.text(vg, "inter-med", timeSz, 0.0F, tintA(ending ? col : muted, a), 20, right, midY, f.time);
                  }

                  if (ph.showBar()) {
                     float by = cy + rowH - 4.0F * sc;
                     float bw = right - tx;
                     float bh = 2.5F * sc;
                     ClarityDraw.fillRRect(vg, tx, by, bw, bh, bh * 0.5F, tintA(borderCol, 0.8F * op));
                     if (f.frac > 0.001F) {
                        ClarityDraw.fillRRect(vg, tx, by, Math.max(bh, bw * f.frac), bh, bh * 0.5F, tintA(col, a));
                     }
                  }

                  cy += rowH + gap;
               }
            }
         }
      } else {
         POTION_ANIM = 0.0F;
      }
   }

   private static void drawEffectIcon(long vg, HudElements.Fx f, float x, float y, float s, int col, float a) {
      int img = f.key.endsWith(".png") ? effectImage(vg, f.key) : 0;
      if (img > 0) {
         try (MemoryStack ms = MemoryStack.stackPush()) {
            NVGPaint p = NVGPaint.malloc(ms);
            NanoVG.nvgImagePattern(vg, x, y, s, s, 0.0F, img, a, p);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, x, y, s, s);
            NanoVG.nvgFillPaint(vg, p);
            NanoVG.nvgFill(vg);
         }
      } else {
         ClarityDraw.fillRRect(vg, x, y, s, s, s * 0.5F, tintA(col, 0.25F * a));
         ClarityDraw.strokeRRect(vg, x, y, s, s, s * 0.5F, 1.0F, tintA(col, a));
         String l = f.name.isEmpty() ? "?" : f.name.substring(0, 1);
         ClarityDraw.text(vg, "inter-sb", s * 0.55F, 0.0F, tintA(col, a), 18, x + s * 0.5F, y + s * 0.5F + 0.5F, l);
      }
   }

   private static int brighten(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int mx = Math.max(r, Math.max(g, b));
      if (mx < 150 && mx > 0) {
         float k = 150.0F / mx;
         r = Math.min(255, Math.round(r * k));
         g = Math.min(255, Math.round(g * k));
         b = Math.min(255, Math.round(b * k));
      }

      return r << 16 | g << 8 | b;
   }

   private static String potionEllipsize(long vg, String s, float size, float maxW) {
      if (maxW <= 0.0F) {
         return "";
      }

      if (ClarityDraw.textWidth(vg, "inter-sb", size, 0.0F, s) <= maxW) {
         return s;
      }

      String e = "…";
      int n = s.length();

      while (n > 0 && ClarityDraw.textWidth(vg, "inter-sb", size, 0.0F, s.substring(0, n) + e) > maxW) {
         n--;
      }

      return s.substring(0, Math.max(0, n)) + e;
   }

   private static void drawInfo(long vg, Minecraft mc) {
      HUD hud = HUD.INSTANCE;
      if (hud != null && hud.isEnabled()) {
         float scale = hud.scale();
         NanoVG.nvgSave(vg);
         NanoVG.nvgScale(vg, scale, scale);
         List<String[]> items = new ArrayList<>();
         if (hud.fps()) {
            items.add(new String[]{"FPS", String.valueOf(mc.getFps())});
         }

         if (hud.ping()) {
            items.add(new String[]{"Ping", ping(mc) + "ms"});
         }

         if (hud.coords()) {
            items.add(new String[]{"XYZ", String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ())});
         }

         boolean wm = hud.watermark();
         if (!wm && items.isEmpty()) {
            NanoVG.nvgRestore(vg);
         } else {
            int accent = ThemeColors.getAccentRgb();
            int text = ThemeColors.getTextRgb();
            int muted = ThemeColors.getMutedRgb();
            float sizeW = 13.0F;
            float sizeI = 12.0F;
            float space = 4.0F;
            float gap = hud.itemGap();
            float padH = hud.paddingH();
            float padV = hud.paddingV();
            float r = hud.cornerRadius();
            float wmW = wm ? ClarityDraw.textWidth(vg, "inter-sb", sizeW, 0.0F, hud.watermarkText()) : 0.0F;
            float[] lw = new float[items.size()];
            float[] vw = new float[items.size()];

            for (int i = 0; i < items.size(); i++) {
               lw[i] = ClarityDraw.textWidth(vg, "inter-med", sizeI, 0.0F, items.get(i)[0]);
               vw[i] = ClarityDraw.textWidth(vg, "inter-med", sizeI, 0.0F, items.get(i)[1]);
            }

            LinkedHashSet<String> present = new LinkedHashSet<>();
            if (wm) {
               present.add("wm");
            }

            for (String[] it : items) {
               present.add(it[0]);
            }

            LinkedHashSet<String> keys = new LinkedHashSet<>(HUD_ANIM.keySet());
            keys.addAll(present);

            for (String k : keys) {
               boolean on = present.contains(k);
               float cur = HUD_ANIM.getOrDefault(k, 0.0F);
               float next = cur + ((on ? 1.0F : 0.0F) - cur) * 0.12F;
               if (!on && next < 0.01F) {
                  HUD_ANIM.remove(k);
               } else {
                  HUD_ANIM.put(k, next);
               }
            }

            float animWm = wm ? HUD_ANIM.getOrDefault("wm", 0.0F) : 0.0F;
            float sumW = 0.0F;
            int visCount = 0;
            if (wm && animWm > 0.005F) {
               sumW += wmW * animWm;
               visCount++;
            }

            for (int i = 0; i < items.size(); i++) {
               float a = HUD_ANIM.getOrDefault(items.get(i)[0], 0.0F);
               if (a > 0.005F) {
                  sumW += (lw[i] + space + vw[i]) * a;
                  visCount++;
               }
            }

            float totalW = padH * 2.0F + sumW + Math.max(0, visCount - 1) * gap;
            float panelH = 30.0F;
            int px = Math.round(hud.x());
            int py = Math.round(hud.y());
            HudLayout.report("HUD", px * scale, py * scale, totalW * scale, panelH * scale);
            if (!LiquidGlass.panel(vg, "HUD", px, py, totalW, panelH, r, 1.0F) && hud.background()) {
               int hudBase = mix(ThemeColors.getSurfaceRgb(), accent, 0.02F);
               boolean hudBlurred = hud.blur() && fillBlurRRect(vg, px, py, totalW, panelH, r, r, r, r, 0.0F, 0.0F, CANVAS_W / scale, CANVAS_H / scale, 1.0F);
               ClarityDraw.fillRRect(vg, px, py, totalW, panelH, r, hudBlurred ? tintA(hudBase, hud.blurTint()) : 0xFF000000 | hudBase);
               HudFx.panel(vg, "HUD", px, py, totalW, panelH, r, 1.0F);
               if (hud.border()) {
                  ClarityDraw.strokeRRect(vg, px, py, totalW, panelH, r, 1.0F, 0xFF000000 | mix(ThemeColors.getBorderRgb(), accent, 0.1F));
               }

               if (hud.highlight()) {
                  int hl = mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.12F);
                  ClarityDraw.topHighlight(vg, px, py, totalW, panelH, r, 1711276032 | hl);
               }
            }

            float cy = Math.round(py + panelH * 0.5F + 1.0F);
            float x = px + padH;
            int sepArgb = -939524096 | mix(ThemeColors.getBorderRgb(), accent, 0.1F);
            boolean first = true;
            if (wm && animWm > 0.005F) {
               ClarityDraw.text(vg, "inter-sb", sizeW, 0.0F, tintA(accent, animWm), 17, x, cy, hud.watermarkText());
               x += wmW * animWm;
               first = false;
            }

            for (int i = 0; i < items.size(); i++) {
               float a = HUD_ANIM.getOrDefault(items.get(i)[0], 0.0F);
               if (!(a <= 0.005F)) {
                  if (!first) {
                     x += gap;
                     if (hud.separators()) {
                        ClarityDraw.fillRRect(vg, Math.round(x - gap * 0.5F), cy - 7.0F, 1.0F, 14.0F, 0.5F, sepArgb);
                     }
                  }

                  first = false;
                  ClarityDraw.text(vg, "inter-med", sizeI, 0.0F, tintA(text, a), 17, x, cy, items.get(i)[0]);
                  x += (lw[i] + space) * a;
                  ClarityDraw.text(vg, "inter-med", sizeI, 0.0F, tintA(muted, a), 17, x, cy, items.get(i)[1]);
                  x += vw[i] * a;
               }
            }

            NanoVG.nvgRestore(vg);
         }
      }
   }

   private static int tintA(int rgb, float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private static void drawPlayerHead(long vg, Minecraft mc, LivingEntity t, float hx, float hy, float hs) {
      if (t instanceof AbstractClientPlayer acp) {
         int img = 0;

         try {
            Texture body = acp.getSkin().body();
            if (body != null) {
               AbstractTexture at = mc.getTextureManager().getTexture(body.texturePath());
               if (at != null && at.getTexture() != null) {
                  int gl = RawFramebufferUtil.getGlId(at.getTexture());
                  if (gl > 0) {
                     try {
                        GL11.glBindTexture(3553, gl);
                        GL11.glTexParameteri(3553, 10241, 9728);
                        GL11.glTexParameteri(3553, 10240, 9728);
                     } catch (Throwable var14) {
                     }

                     Integer cached = HEAD_IMAGE_CACHE.get(gl);
                     if (cached != null) {
                        img = cached;
                     } else {
                        img = NanoVGGL3.nvglCreateImageFromHandle(vg, gl, 64, 64, 32);
                        HEAD_IMAGE_CACHE.put(gl, img);
                     }
                  }
               }
            }
         } catch (Throwable var15) {
         }

         if (img <= 0) {
            setARGB(COL, ThemeColors.getBorderRgb(), 1.0F);
            ClarityDraw.fillRRect(vg, hx, hy, hs, hs, hs * 0.22F, COL);
         } else {
            try (MemoryStack s = MemoryStack.stackPush()) {
               NVGPaint p = NVGPaint.malloc(s);
               NanoVG.nvgImagePattern(vg, hx - hs, hy - hs, hs * 8.0F, hs * 8.0F, 0.0F, img, 1.0F, p);
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgRoundedRect(vg, hx, hy, hs, hs, hs * 0.22F);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
               NanoVG.nvgImagePattern(vg, hx - hs * 5.0F, hy - hs, hs * 8.0F, hs * 8.0F, 0.0F, img, 1.0F, p);
               NanoVG.nvgBeginPath(vg);
               NanoVG.nvgRoundedRect(vg, hx, hy, hs, hs, hs * 0.22F);
               NanoVG.nvgFillPaint(vg, p);
               NanoVG.nvgFill(vg);
            }
         }
      }
   }

   public static boolean backdropReady() {
      return BackdropBlur.textureId() != 0;
   }

   public static int backdropStrength() {
      int s = 0;
      HUD h = HUD.INSTANCE;
      if (h != null && h.isEnabled() && h.background() && h.blur()) {
         s = Math.max(s, h.blurStrength());
      }

      TargetHud t = TargetHud.INSTANCE;
      if (t != null && t.isEnabled() && t.blur() && (TGT_ANIM > 0.01F || AimAssist.getCurrentTarget() != null)) {
         s = Math.max(s, t.blurStrength());
      }

      KeybindList k = KeybindList.INSTANCE;
      if (k != null && k.isEnabled() && k.background() && k.blur()) {
         s = Math.max(s, k.blurStrength());
      }

      BpsCounter bc = BpsCounter.INSTANCE;
      if (bc != null && bc.isEnabled() && bc.background() && bc.blur()) {
         s = Math.max(s, bc.blurStrength());
      }

      ClarityIcon ci = ClarityIcon.INSTANCE;
      if (ci != null && ci.isEnabled() && ci.background() && ci.blur()) {
         s = Math.max(s, ci.blurStrength());
      }

      MusicPlayer mp = MusicPlayer.INSTANCE;
      if (mp != null && mp.isEnabled() && mp.background() && mp.blur() && MUSIC_ANIM > 0.01F) {
         s = Math.max(s, mp.blurStrength());
      }

      if (Notifications.wantsBackdrop()) {
         s = Math.max(s, Notifications.INSTANCE.blurStrength());
      }

      PotionHud ph = PotionHud.INSTANCE;
      if (ph != null && ph.isEnabled() && ph.background() && ph.blur() && POTION_ANIM > 0.01F) {
         s = Math.max(s, ph.blurStrength());
      }

      s = Math.max(s, ChatHud.blurStrength());
      s = Math.max(s, ScoreboardHud.blurStrength());
      if (need.clarity.modules.visual.ArrayList.wantsBlur()) {
         s = Math.max(s, need.clarity.modules.visual.ArrayList.blurStrength());
      }

      return s;
   }

   public static boolean fillBlurRRect(long vg, float x, float y, float w, float h, float tl, float tr, float br, float bl, float ox, float oy, float ew, float eh, float alpha) {
      int img = backdropImage(vg);
      if (img > 0 && !(alpha <= 0.002F)) {
         try (MemoryStack ms = MemoryStack.stackPush()) {
            NVGPaint p = NVGPaint.malloc(ms);
            NanoVG.nvgImagePattern(vg, ox, oy, ew, eh, 0.0F, img, Math.min(1.0F, alpha), p);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRectVarying(vg, x, y, w, h, tl, tr, br, bl);
            NanoVG.nvgFillPaint(vg, p);
            NanoVG.nvgFill(vg);
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean fillBlurRRect(long vg, float x, float y, float w, float h, float r, float alpha) {
      return fillBlurRRect(vg, x, y, w, h, r, r, r, r, 0.0F, 0.0F, CANVAS_W, CANVAS_H, alpha);
   }

   private static String fmtTime(double sec) {
      int s = (int)Math.max(0.0, Math.floor(sec));
      int h = s / 3600;
      int m = s % 3600 / 60;
      int r = s % 60;
      return h > 0 ? String.format("%d:%02d:%02d", h, m, r) : String.format("%d:%02d", m, r);
   }

   private static String ellipsize(long vg, String face, float size, String str, float maxW) {
      if (ClarityDraw.textWidth(vg, face, size, 0.0F, str) <= maxW) {
         return str;
      }

      int end = str.length();

      while (end > 1) {
         String t = str.substring(0, --end).stripTrailing() + "…";
         if (ClarityDraw.textWidth(vg, face, size, 0.0F, t) <= maxW) {
            return t;
         }
      }

      return "…";
   }

   private static void drawScrollText(long vg, String face, float size, int argb, float x, float cy, float maxW, float rowH, String str, boolean scroll) {
      float tw = ClarityDraw.textWidth(vg, face, size, 0.0F, str);
      if (tw <= maxW) {
         ClarityDraw.text(vg, face, size, 0.0F, argb, 17, x, cy, str);
      } else if (!scroll) {
         ClarityDraw.text(vg, face, size, 0.0F, argb, 17, x, cy, ellipsize(vg, face, size, str, maxW));
      } else {
         double t = System.nanoTime() / 1.0E9;
         float travel = tw - maxW + 14.0F;
         float speed = 28.0F;
         float pause = 1.6F;
         float travelT = travel / speed;
         double period = pause * 2.0F + travelT * 2.0F;
         double ph = t % period;
         float off;
         if (ph < pause) {
            off = 0.0F;
         } else if (ph < pause + travelT) {
            off = (float)(ph - pause) * speed;
         } else if (ph < pause * 2.0F + travelT) {
            off = travel;
         } else {
            off = travel - (float)(ph - pause * 2.0F - travelT) * speed;
         }

         NanoVG.nvgSave(vg);
         NanoVG.nvgIntersectScissor(vg, x, cy - rowH * 0.5F, maxW, rowH);
         ClarityDraw.text(vg, face, size, 0.0F, argb, 17, x - off, cy, str);
         NanoVG.nvgRestore(vg);
      }
   }

   private static void fillCol(long vg, int argb) {
      NanoVG.nvgFillColor(vg, ClarityNvg.argb(argb, COL));
      NanoVG.nvgFill(vg);
   }

   private static void drawNote(long vg, float cx, float cy, float s, int argb) {
      NanoVG.nvgBeginPath(vg);
      NanoVG.nvgEllipse(vg, cx - s * 0.16F, cy + s * 0.28F, s * 0.2F, s * 0.15F);
      fillCol(vg, argb);
      ClarityDraw.fillRRect(vg, cx + s * 0.02F, cy - s * 0.36F, s * 0.07F, s * 0.66F, s * 0.03F, argb);
      NanoVG.nvgBeginPath(vg);
      NanoVG.nvgMoveTo(vg, cx + s * 0.09F, cy - s * 0.36F);
      NanoVG.nvgBezierTo(vg, cx + s * 0.36F, cy - s * 0.28F, cx + s * 0.4F, cy - s * 0.02F, cx + s * 0.26F, cy + s * 0.12F);
      NanoVG.nvgBezierTo(vg, cx + s * 0.31F, cy - s * 0.06F, cx + s * 0.22F, cy - s * 0.16F, cx + s * 0.09F, cy - s * 0.13F);
      NanoVG.nvgClosePath(vg);
      fillCol(vg, argb);
   }

   private static void drawMusicArt(long vg, float x, float y, float size, float r, MediaBridge.Snapshot snap, int surface, int accent, float alpha, boolean paused) {
      if (snap.active && snap.art != null && musicImg > 0) {
         float iw = 1.0F;
         float ih = 1.0F;

         try (MemoryStack ms = MemoryStack.stackPush()) {
            IntBuffer bw = ms.mallocInt(1);
            IntBuffer bh = ms.mallocInt(1);
            NanoVG.nvgImageSize(vg, musicImg, bw, bh);
            iw = Math.max(1, bw.get(0));
            ih = Math.max(1, bh.get(0));
         }

         float k = Math.max(size / iw, size / ih);
         float pw = iw * k;
         float ph = ih * k;

         try (MemoryStack ms = MemoryStack.stackPush()) {
            NVGPaint p = NVGPaint.malloc(ms);
            NanoVG.nvgImagePattern(vg, x - (pw - size) * 0.5F, y - (ph - size) * 0.5F, pw, ph, 0.0F, musicImg, Math.min(1.0F, alpha), p);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, size, size, r);
            NanoVG.nvgFillPaint(vg, p);
            NanoVG.nvgFill(vg);
         }
      } else {
         ClarityDraw.fillRRect(vg, x, y, size, size, r, tintA(mix(surface, accent, 0.22F), alpha));
         drawNote(vg, x + size * 0.5F, y + size * 0.5F, size * 0.5F, tintA(accent, 0.9F * alpha));
      }

      if (paused && snap.active) {
         float cx = x + size * 0.5F;
         float cy = y + size * 0.5F;
         float rr = size * 0.24F;
         ClarityDraw.fillRRect(vg, cx - rr, cy - rr, rr * 2.0F, rr * 2.0F, rr, tintA(0, 0.55F * alpha));
         float bw = rr * 0.28F;
         float bh = rr * 0.9F;
         ClarityDraw.fillRRect(vg, cx - bw * 1.45F, cy - bh * 0.5F, bw, bh, bw * 0.3F, tintA(16777215, 0.95F * alpha));
         ClarityDraw.fillRRect(vg, cx + bw * 0.45F, cy - bh * 0.5F, bw, bh, bw * 0.3F, tintA(16777215, 0.95F * alpha));
      }
   }

   private static void drawMusicPlayer(long vg, Minecraft mc, float cw, float ch) {
      MusicPlayer mp = MusicPlayer.INSTANCE;
      if (mp != null && mp.isEnabled()) {
         MediaBridge.Snapshot snap = MediaBridge.get();
         boolean idle = !snap.active;
         boolean visible = (!idle || !mp.hideWhenIdle()) && (idle || snap.playing || !mp.hideWhenPaused());
         if (HudLayout.isEditing()) {
            visible = true;
         }

         MUSIC_ANIM = MUSIC_ANIM + ((visible ? 1.0F : 0.0F) - MUSIC_ANIM) * 0.18F;
         if (!(MUSIC_ANIM < 0.01F)) {
            if (snap.artVersion != musicImgVer) {
               if (musicImg > 0) {
                  NanoVG.nvgDeleteImage(vg, musicImg);
               }

               musicImg = 0;
               musicImgVer = snap.artVersion;
               if (snap.art != null && snap.art.length > 0) {
                  try {
                     ByteBuffer buf = BufferUtils.createByteBuffer(snap.art.length);
                     buf.put(snap.art).flip();
                     int id = NanoVG.nvgCreateImageMem(vg, 1, buf);
                     musicImg = id > 0 ? id : 0;
                  } catch (Throwable ignored) {
                     musicImg = 0;
                  }
               }
            }

            float sc = mp.scale();
            float op = mp.opacity() * MUSIC_ANIM;
            boolean compact = "Compact".equals(mp.layout());
            boolean themed = mp.useTheme();
            boolean paused = !idle && !snap.playing;
            float dimA = paused && mp.dimWhenPaused() ? 0.62F : 1.0F;
            int accent = themed ? ThemeColors.getAccentRgb() : mp.accentColor();
            int textCol = themed ? ThemeColors.getTextRgb() : mp.textColor();
            int mutedCol = ThemeColors.getMutedRgb();
            int surface = themed ? mix(ThemeColors.getSurfaceRgb(), accent, 0.02F) : mp.bgColor();
            int borderCol = ThemeColors.getBorderRgb();
            String title;
            String artist;
            if (idle) {
               title = "Nothing playing";
               artist = snap.error != null ? snap.error : "Play something in Spotify or SoundCloud";
            } else {
               title = snap.title != null && !snap.title.isBlank() ? snap.title : "Unknown title";
               artist = snap.artist == null ? "" : snap.artist;
               if (mp.showApp() && snap.app != null && !snap.app.isEmpty()) {
                  artist = artist.isEmpty() ? snap.app : artist + " · " + snap.app;
               }
            }

            boolean showArtist = mp.showArtist() && !artist.isEmpty();
            boolean showBar = mp.showProgress() && !idle;
            boolean showTimes = mp.showTimes() && !idle;
            float pad = (compact ? 8.0F : 10.0F) * sc;
            float titleSz = (compact ? 12.0F : 13.0F) * sc;
            float artistSz = (compact ? 10.0F : 11.0F) * sc;
            float timeSz = 10.0F * sc;
            float titleRow = (compact ? 15.0F : 17.0F) * sc;
            float artistRow = (compact ? 13.0F : 14.0F) * sc;
            float barH = 3.0F * sc;
            float contentH;
            if (compact) {
               contentH = titleRow + (!showArtist && !showTimes ? 0.0F : artistRow);
            } else {
               contentH = titleRow + (showArtist ? artistRow : 0.0F);
               if (showBar || showTimes) {
                  contentH += 6.0F * sc + (showBar ? barH : 0.0F) + (showTimes ? (showBar ? 4.0F * sc : 0.0F) + 11.0F * sc : 0.0F);
               }
            }

            float artSize = mp.showArt() ? (compact ? Math.max(30.0F * sc, contentH) : Math.max(44.0F * sc, contentH)) : 0.0F;
            float bodyH = Math.max(artSize, contentH);
            float w = mp.width() * sc;
            float h = pad * 2.0F + bodyH + (compact && showBar ? 4.0F * sc : 0.0F);
            float mx = mp.offsetX();
            float my = mp.offsetY();
            float x;
            float y;
            switch (mp.anchor()) {
               case "Top Right":
                  x = cw - w - mx;
                  y = my;
                  break;
               case "Bottom Left":
                  x = mx;
                  y = ch - h - my;
                  break;
               case "Bottom Right":
                  x = cw - w - mx;
                  y = ch - h - my;
                  break;
               default:
                  x = mx;
                  y = my;
            }

            x = Math.round(x);
            y = Math.round(y);
            HudLayout.report("Music Player", x, y, w, h);
            float cr = mp.cornerRadius() * sc;
            if (!LiquidGlass.panel(vg, "Music Player", x, y, w, h, cr, op) && mp.background()) {
               boolean blurred = mp.blur() && fillBlurRRect(vg, x, y, w, h, cr, op);
               ClarityDraw.fillRRect(vg, x, y, w, h, cr, tintA(surface, (blurred ? mp.blurTint() : mp.bgOpacity()) * op));
               HudFx.panel(vg, "Music Player", x, y, w, h, cr, op);
               if (mp.border()) {
                  ClarityDraw.strokeRRect(vg, x, y, w, h, cr, 1.0F, tintA(mix(borderCol, accent, 0.1F), op));
               }

               if (mp.highlight()) {
                  int hl = mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.12F);
                  ClarityDraw.topHighlight(vg, x, y, w, h, cr, tintA(hl, 0.66F * op));
               }
            }

            float tx = x + pad;
            float bodyTop = y + pad;
            if (artSize > 0.0F) {
               float ar = Math.min(cr * 0.7F, artSize * 0.22F);
               drawMusicArt(vg, x + pad, bodyTop + (bodyH - artSize) * 0.5F, artSize, ar, snap, surface, accent, op * dimA, paused);
               tx += artSize + (compact ? 8.0F : 10.0F) * sc;
            }

            float textW = x + w - pad - tx;
            float textTop = bodyTop + (bodyH - contentH) * 0.5F;
            drawScrollText(vg, "inter-sb", titleSz, tintA(textCol, op * dimA), tx, textTop + titleRow * 0.5F, textW, titleRow, title, mp.scrollTitle());
            if (compact) {
               float ry = textTop + titleRow + artistRow * 0.5F;
               String timeStr = null;
               if (showTimes) {
                  String right = "Elapsed / Total".equals(mp.timeMode()) ? (snap.durSec > 0.0 ? fmtTime(snap.durSec) : "--:--") : (snap.durSec > 0.0 ? "-" + fmtTime(snap.remaining()) : "--:--");
                  timeStr = fmtTime(snap.position()) + " / " + right;
               }

               float timeW = timeStr != null ? ClarityDraw.textWidth(vg, "inter-med", timeSz, 0.0F, timeStr) : 0.0F;
               if (showArtist) {
                  float aw = Math.max(10.0F, textW - (timeW > 0.0F ? timeW + 8.0F * sc : 0.0F));
                  ClarityDraw.text(vg, "inter-med", artistSz, 0.0F, tintA(mutedCol, op), 17, tx, ry, ellipsize(vg, "inter-med", artistSz, artist, aw));
               }

               if (timeStr != null) {
                  ClarityDraw.text(vg, "inter-med", timeSz, 0.0F, tintA(mutedCol, op), 20, x + w - pad, ry, timeStr);
               }

               if (showBar) {
                  float bx = x + pad;
                  float bw = w - pad * 2.0F;
                  float by = y + h - pad * 0.5F - barH * 0.5F - 1.0F;
                  float frac = snap.durSec > 0.0 ? (float)Math.min(1.0, snap.position() / snap.durSec) : 0.0F;
                  ClarityDraw.fillRRect(vg, bx, by, bw, barH * 0.8F, barH * 0.4F, tintA(mix(borderCol, textCol, 0.15F), 0.9F * op));
                  if (frac > 0.0F) {
                     ClarityDraw.fillRRect(vg, bx, by, Math.max(barH, bw * frac), barH * 0.8F, barH * 0.4F, tintA(accent, op * dimA));
                  }
               }
            } else {
               float cy = textTop + titleRow;
               if (showArtist) {
                  ClarityDraw.text(vg, "inter-med", artistSz, 0.0F, tintA(mutedCol, op), 17, tx, cy + artistRow * 0.5F, ellipsize(vg, "inter-med", artistSz, artist, textW));
                  cy += artistRow;
               }

               if (showBar || showTimes) {
                  cy += 6.0F * sc;
                  if (showBar) {
                     float frac = snap.durSec > 0.0 ? (float)Math.min(1.0, snap.position() / snap.durSec) : 0.0F;
                     ClarityDraw.fillRRect(vg, tx, cy, textW, barH, barH * 0.5F, tintA(mix(borderCol, textCol, 0.15F), 0.9F * op));
                     if (frac > 0.0F) {
                        float fw = Math.max(barH, textW * frac);
                        ClarityDraw.fillRRect(vg, tx, cy, fw, barH, barH * 0.5F, tintA(accent, op * dimA));
                        float kr = barH * 0.95F;
                        ClarityDraw.fillRRect(vg, tx + fw - kr, cy + barH * 0.5F - kr, kr * 2.0F, kr * 2.0F, kr, tintA(textCol, 0.95F * op * dimA));
                     }

                     cy += barH + 4.0F * sc;
                  }

                  if (showTimes) {
                     String right = "Elapsed / Total".equals(mp.timeMode()) ? (snap.durSec > 0.0 ? fmtTime(snap.durSec) : "--:--") : (snap.durSec > 0.0 ? "-" + fmtTime(snap.remaining()) : "--:--");
                     float ty = cy + 5.5F * sc;
                     ClarityDraw.text(vg, "inter-med", timeSz, 0.0F, tintA(mutedCol, op), 17, tx, ty, fmtTime(snap.position()));
                     ClarityDraw.text(vg, "inter-med", timeSz, 0.0F, tintA(mutedCol, op), 20, tx + textW, ty, right);
                  }
               }
            }
         }
      } else {
         MUSIC_ANIM = 0.0F;
      }
   }

   private static void drawKeybindList(long vg, Minecraft mc, float cw, float ch) {
      KeybindList kl = KeybindList.INSTANCE;
      if (kl != null && kl.isEnabled()) {
         List<KeybindList.Row> rows = kl.rows();
         if (rows.isEmpty() && HudLayout.isEditing()) {
            rows = kl.previewRows();
         }

         int n = rows.size();
         if (n != 0) {
            float sc = kl.scale();
            float op = kl.opacity();
            float nameSz = kl.textSize() * sc;
            float keySz = Math.max(8.0F, kl.textSize() - 1.0F) * sc;
            float rowH = kl.rowHeight() * sc;
            float rowGap = kl.rowGap() * sc;
            float pad = kl.padding() * sc;
            boolean title = kl.showTitle();
            float titleSz = (kl.textSize() + 1.0F) * sc;
            float titleH = title ? titleSz + 12.0F * sc : 0.0F;
            String keyStyle = kl.keyStyle();
            boolean pill = "Pill".equals(keyStyle);
            boolean brackets = "Brackets".equals(keyStyle);
            boolean dot = kl.stateDot();
            boolean dim = kl.dimDisabled();
            boolean showMode = kl.showMode();
            boolean keysRight = !"Left".equals(kl.keyAlign());
            boolean themed = kl.useTheme();
            int accent = ThemeColors.getAccentRgb();
            int nameCol = themed ? ThemeColors.getTextRgb() : kl.nameColor();
            int keyCol = themed ? accent : kl.keyColor();
            int mutedCol = ThemeColors.getMutedRgb();
            int onCol = kl.enabledColor();
            int offCol = kl.disabledColor();
            int surface = themed ? mix(ThemeColors.getSurfaceRgb(), accent, 0.02F) : kl.bgColor();
            int borderCol = ThemeColors.getBorderRgb();
            String[] keyLabel = new String[n];
            float[] nameW = new float[n];
            float[] modeW = new float[n];
            float[] keyW = new float[n];
            float nameColW = 0.0F;
            float keyColW = 0.0F;
            float dotW = dot ? 12.0F * sc : 0.0F;

            for (int i = 0; i < n; i++) {
               KeybindList.Row r = rows.get(i);
               String k = kl.upperKeys() ? r.key.toUpperCase(Locale.ROOT) : r.key;
               keyLabel[i] = brackets ? "[" + k + "]" : k;
               nameW[i] = ClarityDraw.textWidth(vg, "inter-med", nameSz, 0.0F, r.name);
               modeW[i] = showMode && "hold".equals(r.mode) ? ClarityDraw.textWidth(vg, "inter-med", nameSz * 0.8F, 0.0F, " hold") : 0.0F;
               keyW[i] = ClarityDraw.textWidth(vg, "inter-med", keySz, 0.0F, keyLabel[i]) + (pill ? 12.0F * sc : 0.0F);
               nameColW = Math.max(nameColW, nameW[i] + modeW[i]);
               keyColW = Math.max(keyColW, keyW[i]);
            }

            float colGap = 16.0F * sc;
            String titleText = kl.titleText();
            float titleW = title ? ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, titleText) : 0.0F;
            float w = Math.max(kl.minWidth() * sc, Math.max(dotW + nameColW + colGap + keyColW, titleW) + pad * 2.0F);
            float h = pad * 2.0F + titleH + n * rowH + (n - 1) * rowGap;
            float mx = kl.offsetX();
            float my = kl.offsetY();
            float x;
            float y;
            switch (kl.anchor()) {
               case "Top Right":
                  x = cw - w - mx;
                  y = my;
                  break;
               case "Bottom Left":
                  x = mx;
                  y = ch - h - my;
                  break;
               case "Bottom Right":
                  x = cw - w - mx;
                  y = ch - h - my;
                  break;
               default:
                  x = mx;
                  y = my;
            }

            x = Math.round(x);
            y = Math.round(y);
            HudLayout.report("Keybind List", x, y, w, h);
            float cr = kl.cornerRadius() * sc;
            if (!LiquidGlass.panel(vg, "Keybind List", x, y, w, h, cr, op) && kl.background()) {
               boolean blurred = kl.blur() && fillBlurRRect(vg, x, y, w, h, cr, op);
               ClarityDraw.fillRRect(vg, x, y, w, h, cr, tintA(surface, (blurred ? kl.blurTint() : kl.bgOpacity()) * op));
               HudFx.panel(vg, "Keybind List", x, y, w, h, cr, op);
               if (kl.border()) {
                  ClarityDraw.strokeRRect(vg, x, y, w, h, cr, 1.0F, tintA(mix(borderCol, accent, 0.1F), op));
               }

               if (kl.highlight()) {
                  int hl = mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.12F);
                  ClarityDraw.topHighlight(vg, x, y, w, h, cr, tintA(hl, 0.66F * op));
               }
            }

            if (title) {
               float ty = y + pad + titleSz * 0.5F;
               ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(keyCol, op), 17, x + pad, ty, titleText);
               if (kl.titleDivider()) {
                  ClarityDraw.fillRRect(vg, x + pad, y + pad + titleH - 5.0F * sc, w - pad * 2.0F, 1.0F, 0.5F, tintA(mix(borderCol, accent, 0.1F), 0.9F * op));
               }
            }

            Set<String> present = new HashSet<>();

            for (int i = 0; i < n; i++) {
               KeybindList.Row r = rows.get(i);
               present.add(r.name);
               float cur = KB_ANIM.getOrDefault(r.name, r.on ? 1.0F : 0.0F);
               cur += ((r.on ? 1.0F : 0.0F) - cur) * 0.18F;
               KB_ANIM.put(r.name, cur);
               float ry = y + pad + titleH + i * (rowH + rowGap);
               float cy = ry + rowH * 0.5F + 0.5F;
               if (kl.rowHighlight() && cur > 0.02F) {
                  ClarityDraw.fillRRect(vg, x + pad * 0.5F, ry, w - pad, rowH, Math.min(cr, rowH * 0.4F), tintA(onCol, 0.1F * cur * op));
               }

               float tx = x + pad;
               if (dot) {
                  float dr = 3.5F * sc;
                  ClarityDraw.fillRRect(vg, tx + dr, cy - dr, dr * 2.0F, dr * 2.0F, dr, tintA(mix(offCol, onCol, cur), (0.55F + 0.45F * cur) * op));
               }

               tx += dotW;
               int nc = dim ? mix(mutedCol, nameCol, cur) : nameCol;
               ClarityDraw.text(vg, "inter-med", nameSz, 0.0F, tintA(nc, op), 17, tx, cy, r.name);
               if (modeW[i] > 0.0F) {
                  ClarityDraw.text(vg, "inter-med", nameSz * 0.8F, 0.0F, tintA(mutedCol, 0.8F * op), 17, tx + nameW[i], cy + 0.5F, " hold");
               }

               int kc = dim ? mix(mutedCol, keyCol, 0.45F + 0.55F * cur) : keyCol;
               float kRight = x + w - pad;
               float kLeft = x + pad + dotW + nameColW + colGap;
               float bx = keysRight ? kRight - keyW[i] : kLeft;
               if (pill) {
                  float bh = Math.min(rowH - 4.0F * sc, keySz + 8.0F * sc);
                  ClarityDraw.fillRRect(vg, bx, cy - bh * 0.5F, keyW[i], bh, Math.min(6.0F * sc, bh * 0.5F), tintA(kc, 0.14F * op));
                  ClarityDraw.strokeRRect(vg, bx, cy - bh * 0.5F, keyW[i], bh, Math.min(6.0F * sc, bh * 0.5F), 1.0F, tintA(kc, 0.35F * op));
                  ClarityDraw.text(vg, "inter-med", keySz, 0.0F, tintA(kc, op), 18, bx + keyW[i] * 0.5F, cy, keyLabel[i]);
               } else {
                  ClarityDraw.text(vg, "inter-med", keySz, 0.0F, tintA(kc, op), keysRight ? 20 : 17, keysRight ? kRight : kLeft, cy, keyLabel[i]);
               }
            }

            KB_ANIM.keySet().retainAll(present);
         }
      }
   }

   public static void onContextDestroyed() {
      MiscHud.onContextDestroyed();
      EFFECT_IMG.clear();
      blurImg = 0;
      blurImgH = 0;
      blurImgW = 0;
      blurImgTex = 0;
      musicImg = 0;
      musicImgVer = -1;
      hudCanvas = null;
      hudCanvasVg = 0L;
   }

   private static int backdropImage(long vg) {
      int t = BackdropBlur.textureId();
      if (t == 0) {
         return 0;
      }

      int w = BackdropBlur.width();
      int h = BackdropBlur.height();
      if (blurImg != 0 && blurImgTex == t && blurImgW == w && blurImgH == h) {
         return blurImg;
      }

      if (blurImg != 0) {
         NanoVG.nvgDeleteImage(vg, blurImg);
      }

      blurImg = NanoVGGL3.nvglCreateImageFromHandle(vg, t, w, h, 65544);
      blurImgTex = t;
      blurImgW = w;
      blurImgH = h;
      return blurImg;
   }

   private static void statusIcon(long vg, float cx, float cy, float d, boolean positive, int rgb, float a, float sc) {
      ClarityDraw.fillRRect(vg, cx - d / 2.0F, cy - d / 2.0F, d, d, d / 2.0F, tintA(rgb, 0.18F * a));
      NanoVG.nvgLineCap(vg, 1);
      NanoVG.nvgLineJoin(vg, 1);
      NanoVG.nvgBeginPath(vg);
      if (positive) {
         NanoVG.nvgMoveTo(vg, cx - d * 0.24F, cy + d * 0.02F);
         NanoVG.nvgLineTo(vg, cx - d * 0.06F, cy + d * 0.2F);
         NanoVG.nvgLineTo(vg, cx + d * 0.26F, cy - d * 0.16F);
      } else {
         NanoVG.nvgMoveTo(vg, cx - d * 0.18F, cy - d * 0.18F);
         NanoVG.nvgLineTo(vg, cx + d * 0.18F, cy + d * 0.18F);
         NanoVG.nvgMoveTo(vg, cx + d * 0.18F, cy - d * 0.18F);
         NanoVG.nvgLineTo(vg, cx - d * 0.18F, cy + d * 0.18F);
      }

      setARGB(COL, rgb, a);
      NanoVG.nvgStrokeColor(vg, COL);
      NanoVG.nvgStrokeWidth(vg, Math.max(1.4F, 1.9F * sc));
      NanoVG.nvgStroke(vg);
   }

   private static void drawNotifications(long vg, Minecraft mc, float canvasW, float canvasH) {
      Notifications nm = Notifications.INSTANCE;
      if (nm != null && nm.isEnabled()) {
         boolean editing = HudLayout.isEditing();
         List<Notifications.Entry> list = editing ? Notifications.previewSnapshot() : Notifications.snapshot();
         if (!list.isEmpty()) {
            float bbMinX = Float.MAX_VALUE;
            float bbMinY = Float.MAX_VALUE;
            float bbMaxX = -Float.MAX_VALUE;
            float bbMaxY = -Float.MAX_VALUE;
            String pos = nm.position();
            boolean centerX = pos.endsWith("Center");
            boolean right = pos.endsWith("Right");
            boolean bottom = pos.startsWith("Bottom");
            String style = nm.style();
            String anim = nm.animation();
            boolean compact = nm.compact();
            boolean bar = nm.statusBar();
            boolean progress = nm.progressBar();
            float sc = nm.scale();
            float op = nm.opacity();
            float mx = nm.offsetX();
            float my = nm.offsetY();
            float inMs = 220.0F;
            float outMs = 260.0F;
            int accent = ThemeColors.getAccentRgb();
            boolean themed = nm.useThemeAccent();
            int okCol = themed ? accent : nm.enabledColor();
            int offCol = themed ? mix(accent, 0, 0.45F) : nm.disabledColor();
            long now = System.currentTimeMillis();
            int surface = ThemeColors.getSurfaceRgb();
            int border = ThemeColors.getBorderRgb();
            int textCol = ThemeColors.getTextRgb();
            boolean light = ThemeColors.isLight();
            boolean classic = "Classic".equals(style);
            boolean minimal = "Minimal".equals(style);
            boolean modern = "Modern".equals(style);
            boolean sw = "Switch".equals(style);
            boolean blurStyle = "Blur".equals(style);
            int blurImage = blurStyle ? backdropImage(vg) : 0;
            float titleSz = (minimal ? 12.0F : 13.0F) * sc;
            float subSz = 11.0F * sc;
            float h;
            if (minimal) {
               h = 26.0F * sc;
            } else if (sw) {
               h = 34.0F * sc;
            } else if (classic) {
               h = (compact ? 28.0F : 40.0F) * sc;
            } else {
               h = (compact ? 30.0F : 42.0F) * sc;
            }

            float gap = (minimal ? 5.0F : 6.0F) * sc;
            int i = list.size() - 1;

            for (int slot = 0; i >= 0; slot++) {
               Notifications.Entry e = list.get(i);
               float age = (float)(now - e.bornMs);
               float a = Math.min(1.0F, age / 220.0F);
               float left = (float)e.lifeMs - age;
               if (left < 260.0F) {
                  a = Math.min(a, Math.max(0.0F, left / 260.0F));
               }

               float ease = 1.0F - (1.0F - a) * (1.0F - a);
               float alpha = ease * op;
               if (!(alpha <= 0.005F)) {
                  boolean pos1 = e.positive;
                  int status = pos1 ? okCol : offCol;
                  String line2 = e.subtitle;
                  float fullTitleW = ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, e.title);
                  float titleSubW = ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, e.title + "  " + line2);
                  float subW = ClarityDraw.textWidth(vg, "inter-med", subSz, 0.0F, line2);
                  boolean twoLine = !compact && !minimal && !sw;
                  float iconD = h - 16.0F * sc;
                  float w;
                  if (minimal) {
                     w = titleSubW + 38.0F * sc;
                  } else if (sw) {
                     w = fullTitleW + 14.0F * sc + 16.0F * sc + 32.0F * sc + 12.0F * sc;
                  } else if (classic) {
                     w = Math.max(compact ? 110.0F * sc : 150.0F * sc, (twoLine ? Math.max(fullTitleW, subW) : titleSubW) + (bar ? 24.0F : 12.0F) * sc + 14.0F * sc);
                  } else {
                     w = Math.max(compact ? 130.0F * sc : 160.0F * sc, (twoLine ? Math.max(fullTitleW, subW) : titleSubW) + 8.0F * sc + iconD + 9.0F * sc + 14.0F * sc);
                  }

                  float baseX = centerX ? (canvasW - w) * 0.5F : (right ? canvasW - w - mx : mx);
                  float slideAmt = "Slide".equals(anim) ? (1.0F - ease) * 40.0F * sc : 0.0F;
                  float x = Math.round(baseX + (centerX ? 0.0F : (right ? slideAmt : -slideAmt)));
                  float stackOff = slot * (h + gap);
                  float baseY = bottom ? canvasH - h - my - stackOff : my + stackOff;
                  float y = Math.round(baseY + (centerX ? (bottom ? slideAmt : -slideAmt) : 0.0F));
                  float cy = y + h * 0.5F;
                  if (editing) {
                     float rx = Math.round(baseX);
                     float ry = Math.round(baseY);
                     bbMinX = Math.min(bbMinX, rx);
                     bbMinY = Math.min(bbMinY, ry);
                     bbMaxX = Math.max(bbMaxX, rx + w);
                     bbMaxY = Math.max(bbMaxY, ry + h);
                  }

                  if (minimal) {
                     float r = h * 0.5F;
                     if (!LiquidGlass.panel(vg, "Notifications", x, y, w, h, r, alpha)) {
                        ClarityDraw.fillRRect(vg, x, y, w, h, r, tintA(mix(surface, 0, light ? 0.0F : 0.25F), 0.92F * alpha));
                        HudFx.panel(vg, "Notifications", x, y, w, h, r, alpha);
                        ClarityDraw.strokeRRect(vg, x, y, w, h, r, 1.0F, tintA(mix(border, status, 0.25F), alpha));
                     }

                     float dr = 4.0F * sc;
                     ClarityDraw.fillRRect(vg, x + 11.0F * sc - dr, cy - dr, dr * 2.0F, dr * 2.0F, dr, tintA(status, alpha));
                     float ty = cy - titleSz * 0.5F;
                     float tx = x + 22.0F * sc;
                     ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(textCol, alpha), 9, tx, ty, e.title);
                     float used = ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, e.title + "  ");
                     ClarityDraw.text(vg, "inter-med", titleSz, 0.0F, tintA(status, alpha), 9, tx + used, ty, line2);
                  } else {
                     float r = (sw ? 10.0F : (blurStyle ? 12.0F : 10.0F)) * sc;
                     if (blurStyle) {
                        if (blurImage > 0) {
                           try (MemoryStack ms = MemoryStack.stackPush()) {
                              NVGPaint p = NVGPaint.malloc(ms);
                              NanoVG.nvgImagePattern(vg, 0.0F, 0.0F, canvasW, canvasH, 0.0F, blurImage, alpha, p);
                              NanoVG.nvgBeginPath(vg);
                              NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
                              NanoVG.nvgFillPaint(vg, p);
                              NanoVG.nvgFill(vg);
                           }

                           ClarityDraw.fillRRect(vg, x, y, w, h, r, tintA(mix(surface, 0, light ? 0.0F : 0.35F), nm.blurTint() * alpha));
                        } else {
                           ClarityDraw.fillRRect(vg, x, y, w, h, r, tintA(mix(surface, 0, 0.2F), 0.72F * alpha));
                        }

                        ClarityDraw.strokeRRect(vg, x, y, w, h, r, 1.0F, tintA(16777215, 0.16F * alpha));
                        ClarityDraw.topHighlight(vg, x, y, w, h, r, tintA(16777215, 0.22F * alpha));
                        ClarityDraw.fillRRectLinear(vg, x, y, w, h, r, x, y, x + w * 0.5F, y, tintA(status, 0.16F * alpha), tintA(status, 0.0F));
                     } else if (modern) {
                        ClarityDraw.fillRRect(vg, x, y, w, h, r, tintA(mix(surface, status, 0.07F), alpha));
                        ClarityDraw.strokeRRect(vg, x, y, w, h, r, 1.0F, tintA(mix(border, status, 0.35F), alpha));
                        ClarityDraw.fillRRectLinear(vg, x, y, w, h, r, x, y, x + w * 0.55F, y, tintA(status, 0.2F * alpha), tintA(status, 0.0F));
                        int hl = mix(surface, light ? 0 : 16777215, 0.12F);
                        ClarityDraw.topHighlight(vg, x, y, w, h, r, tintA(hl, 0.5F * alpha));
                     } else if (!LiquidGlass.panel(vg, "Notifications", x, y, w, h, r, alpha)) {
                        ClarityDraw.fillRRect(vg, x, y, w, h, r, tintA(mix(surface, accent, 0.02F), alpha));
                        ClarityDraw.strokeRRect(vg, x, y, w, h, r, 1.0F, tintA(mix(border, accent, 0.1F), alpha));
                        int hl = mix(surface, light ? 0 : 16777215, 0.12F);
                        ClarityDraw.topHighlight(vg, x, y, w, h, r, tintA(hl, 0.66F * alpha));
                     }

                     HudFx.panel(vg, "Notifications", x, y, w, h, r, alpha);
                     if (classic) {
                        float padL = (bar ? 24.0F : 12.0F) * sc;
                        if (bar) {
                           float inset = compact ? 7.0F * sc : 10.0F * sc;
                           ClarityDraw.fillRRect(vg, x + 10.0F * sc, y + inset, 4.0F * sc, h - inset * 2.0F, 2.0F * sc, tintA(status, alpha));
                        }

                        if (compact) {
                           float ty = cy - titleSz * 0.5F;
                           ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(accent, alpha), 9, x + padL, ty, e.title);
                           float used = ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, e.title + "  ");
                           ClarityDraw.text(vg, "inter-med", titleSz, 0.0F, tintA(status, alpha), 9, x + padL + used, ty, line2);
                        } else {
                           ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(accent, alpha), 9, x + padL, y + 8.0F * sc, e.title);
                           ClarityDraw.text(vg, "inter-med", subSz, 0.0F, tintA(status, alpha), 9, x + padL, y + 23.0F * sc, line2);
                        }
                     } else if (sw) {
                        ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(textCol, alpha), 9, x + 14.0F * sc, cy - titleSz * 0.5F, e.title);
                        float tw = 32.0F * sc;
                        float th = 18.0F * sc;
                        float tx = x + w - tw - 12.0F * sc;
                        float ty = cy - th * 0.5F;
                        float t = Math.min(1.0F, age / 300.0F);
                        t = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
                        float on = pos1 ? t : 1.0F - t;
                        int track = mix(mix(border, 0, 0.1F), status, on);
                        ClarityDraw.fillRRect(vg, tx, ty, tw, th, th * 0.5F, tintA(track, alpha));
                        float kd = th - 4.0F * sc;
                        float kx = tx + 2.0F * sc + on * (tw - th);
                        ClarityDraw.fillRRect(vg, kx, ty + 2.0F * sc, kd, kd, kd * 0.5F, tintA(16777215, alpha));
                     } else {
                        float ix = x + 8.0F * sc + iconD * 0.5F;
                        statusIcon(vg, ix, cy, iconD, pos1, status, alpha, sc);
                        float tx = x + 8.0F * sc + iconD + 9.0F * sc;
                        if (compact) {
                           float ty = cy - titleSz * 0.5F;
                           ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(textCol, alpha), 9, tx, ty, e.title);
                           float used = ClarityDraw.textWidth(vg, "inter-sb", titleSz, 0.0F, e.title + "  ");
                           ClarityDraw.text(vg, "inter-med", titleSz, 0.0F, tintA(status, alpha), 9, tx + used, ty, line2);
                        } else {
                           ClarityDraw.text(vg, "inter-sb", titleSz, 0.0F, tintA(textCol, alpha), 9, tx, y + 8.0F * sc, e.title);
                           ClarityDraw.text(vg, "inter-med", subSz, 0.0F, tintA(status, alpha), 9, tx, y + 24.0F * sc, line2);
                        }
                     }

                     if (progress) {
                        float remain = Math.max(0.0F, Math.min(1.0F, left / (float)e.lifeMs));
                        float bh = 2.0F * sc;
                        float bx = x + r * 0.6F;
                        float bw = (w - r * 1.2F) * remain;
                        if (bw > 1.0F) {
                           ClarityDraw.fillRRect(vg, bx, y + h - bh - 2.0F * sc, bw, bh, bh * 0.5F, tintA(status, alpha * 0.85F));
                        }
                     }
                  }
               }

               i--;
            }

            if (editing && bbMinX < bbMaxX) {
               HudLayout.report("Notifications", bbMinX, bbMinY, bbMaxX - bbMinX, bbMaxY - bbMinY);
            }
         }
      }
   }

   static float bottomCenterClear(Minecraft mc, float canvasW) {
      float overlayScale = mc.getWindow().getWidth() / Math.max(1.0F, canvasW);
      float k = mc.getWindow().getGuiScale() / Math.max(0.01F, overlayScale);
      boolean survival = false;

      try {
         survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
      } catch (Throwable var6) {
      }

      return (22.0F + (survival ? 38.0F : 4.0F)) * k;
   }

   private static void drawTarget(long vg, Minecraft mc, float canvasW, float canvasH) {
      TargetHud th = TargetHud.INSTANCE;
      if (th != null && th.isEnabled() && mc.player != null) {
         LivingEntity t = null;

         try {
            t = AimAssist.getCurrentTarget();
         } catch (Throwable var43) {
         }

         boolean has = t != null;
         boolean editing = HudLayout.isEditing();
         TGT_ANIM = TGT_ANIM + ((!has && !editing ? 0.0F : 1.0F) - TGT_ANIM) * 0.12F;
         if (has || editing || !(TGT_ANIM < 0.01F)) {
            float a = editing ? 1.0F : Math.max(0.0F, Math.min(1.0F, TGT_ANIM));
            String name;
            float ratio;
            int dist;
            String gear;
            boolean showGear;
            boolean showDist;
            boolean showHealth;
            if (!has && editing) {
               name = "Target";
               ratio = 0.7F;
               dist = 4;
               gear = "A10  Netherite Sword";
               showGear = th.showGear();
               showDist = th.showDist();
               showHealth = th.showHealth();
            } else if (has) {
               name = t.getName().getString();
               float hp = Math.max(0.0F, t.getHealth());
               float maxHp = Math.max(1.0F, t.getMaxHealth());
               ratio = Math.max(0.0F, Math.min(1.0F, hp / maxHp));
               dist = (int)mc.player.distanceTo(t);
               int armor = 0;

               try {
                  armor = t.getArmorValue();
               } catch (Throwable var42) {
               }

               String held = t.getItemInHand(InteractionHand.MAIN_HAND).getHoverName().getString();
               if (held.length() > 14) {
                  held = held.substring(0, 14);
               }

               gear = "A" + armor + "  " + held;
               showGear = th.showGear();
               showDist = th.showDist();
               showHealth = th.showHealth();
               C_NAME = name;
               C_RATIO = ratio;
               C_DIST = dist;
               C_GEAR = gear;
               C_SHOWGEAR = showGear;
               C_SHOWDIST = showDist;
               C_SHOWHEALTH = showHealth;
            } else {
               name = C_NAME;
               ratio = C_RATIO;
               dist = C_DIST;
               gear = C_GEAR;
               showGear = C_SHOWGEAR;
               showDist = C_SHOWDIST;
               showHealth = C_SHOWHEALTH;
            }

            float w = th.width();
            float h = 65.0F;
            float mx = th.x();
            float my = th.y();
            String anchor = th.anchor();
            boolean center = "Bottom Center".equals(anchor);
            float bx0;
            float by0;
            switch (anchor) {
               case "Top Left":
                  bx0 = mx;
                  by0 = my;
                  break;
               case "Top Right":
                  bx0 = canvasW - w - mx;
                  by0 = my;
                  break;
               case "Bottom Left":
                  bx0 = mx;
                  by0 = canvasH - h - my;
                  break;
               case "Bottom Center":
                  bx0 = (canvasW - w) * 0.5F;
                  by0 = canvasH - h - bottomCenterClear(mc, canvasW) - my;
                  break;
               default:
                  bx0 = canvasW - w - mx;
                  by0 = canvasH - h - my;
            }

            boolean rightAnchor = !anchor.endsWith("Left");
            boolean bottomAnchor = anchor.startsWith("Bottom");
            HudLayout.report("Target Hud", Math.round(bx0), Math.round(by0), w, h);
            float x = Math.round(bx0 + (center ? 0.0F : (1.0F - a) * (rightAnchor ? 28.0F : -28.0F)));
            float y = Math.round(by0 + (1.0F - a) * (bottomAnchor ? 16.0F : -16.0F));
            int accent = ThemeColors.getAccentRgb();
            int tgtBase = mix(ThemeColors.getSurfaceRgb(), accent, 0.02F);
            if (!LiquidGlass.panel(vg, "Target Hud", x, y, w, h, 12.0F, a)) {
               boolean tgtBlurred = th.blur() && fillBlurRRect(vg, x, y, w, h, 12.0F, a);
               ClarityDraw.fillRRect(vg, x, y, w, h, 12.0F, tintA(tgtBase, tgtBlurred ? th.blurTint() * a : a));
               HudFx.panel(vg, "Target Hud", x, y, w, h, 12.0F, a);
               ClarityDraw.strokeRRect(vg, x, y, w, h, 12.0F, 1.0F, tintA(mix(ThemeColors.getBorderRgb(), accent, 0.1F), a));
               int hlT = mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.12F);
               ClarityDraw.topHighlight(vg, x, y, w, h, 12.0F, tintA(hlT, 0.66F * a));
            }

            float hs = 34.0F;
            float hx = x + 10.0F;
            float hy = Math.round(y + (h - hs) * 0.5F);
            if (has) {
               drawPlayerHead(vg, mc, t, hx, hy, hs);
            } else {
               ClarityDraw.fillRRect(vg, hx, hy, hs, hs, hs * 0.22F, tintA(1909293, a));
            }

            ClarityDraw.strokeRRect(vg, hx, hy, hs, hs, hs * 0.22F, 1.0F, tintA(ThemeColors.getBorderRgb(), a));
            float tx = hx + hs + 9.0F;
            float rightPad = 10.0F;
            ClarityDraw.text(vg, "inter-sb", 13.0F, 0.0F, tintA(accent, a), 9, tx, y + 10.0F, name);
            if (showDist) {
               ClarityDraw.text(vg, "inter-med", 12.0F, 0.0F, tintA(ThemeColors.getTextRgb(), a), 12, x + w - rightPad, y + 10.5F, dist + "m");
            }

            if (showGear) {
               ClarityDraw.text(vg, "inter-med", 11.0F, 0.0F, tintA(ThemeColors.getTextRgb(), a), 9, tx, y + 29.0F, gear);
            }

            if (showHealth) {
               float bx = tx;
               float by = y + h - 15.0F;
               float bw = Math.max(4.0F, x + w - rightPad - tx);
               float bh = 5.0F;
               setARGB(COL, ThemeColors.getBorderRgb(), a);
               ClarityDraw.fillRRect(vg, bx, by, bw, bh, 2.5F, COL);
               int r = Math.round(220.0F + -160.0F * ratio);
               int g = Math.round(60.0F + 160.0F * ratio);
               setARGB(COL, r << 16 | g << 8 | 60, a);
               ClarityDraw.fillRRect(vg, bx, by, bw * ratio, bh, 2.5F, COL);
               ClarityDraw.strokeRRect(vg, bx, by, bw, bh, 2.5F, 1.0F, tintA(mix(ThemeColors.getBorderRgb(), accent, 0.1F), a));
               ClarityDraw.topHighlight(vg, bx, by, bw, bh, 2.5F, tintA(mix(ThemeColors.getSurfaceRgb(), ThemeColors.isLight() ? 0 : 16777215, 0.2F), 0.66F * a));
            }
         }
      }
   }

   private static int ping(Minecraft mc) {
      try {
         ClientPacketListener c = mc.getConnection();
         if (c == null) {
            return 0;
         }

         PlayerInfo info = c.getPlayerInfo(mc.player.getUUID());
         return info == null ? 0 : info.getLatency();
      } catch (Throwable t) {
         return 0;
      }
   }

   private static void setARGB(NVGColor c, int rgb, float alpha) {
      NanoVG.nvgRGBAf((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, alpha, c);
   }

   private static final class Fx {
      final String key;
      final String name;
      final String time;
      final int amp;
      final int rgb;
      final boolean good;
      final boolean infinite;
      final float frac;
      final int ticks;

      Fx(String key, String name, int amp, int ticks, boolean infinite, int rgb, boolean good, float frac) {
         this.key = key;
         this.name = name;
         this.amp = amp;
         this.ticks = ticks;
         this.infinite = infinite;
         this.rgb = rgb;
         this.good = good;
         this.frac = frac;
         this.time = infinite ? "∞" : HudElements.fmtTicks(ticks);
      }
   }
}
