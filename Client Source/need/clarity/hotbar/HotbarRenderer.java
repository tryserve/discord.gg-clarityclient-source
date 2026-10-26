package need.clarity.hotbar;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import need.clarity.Clarity;
import need.clarity.clickgui.NvgCanvas;
import need.clarity.clickgui.RawGlState;
import need.clarity.clickgui.Theme;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.visual.CustomHotbar;
import need.clarity.nvg.ClarityFonts;
import need.clarity.render.GuiItems;
import need.clarity.render.Shader;
import need.clarity.util.ThemeColors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL33C;

public final class HotbarRenderer {
   private static final Identifier TEX_ID = Identifier.fromNamespaceAndPath("clarity", "custom_hotbar");
   private static final HotbarState STATE = new HotbarState();
   private static final HotbarPainter PAINTER = new HotbarPainter();
   private static long vg;
   private static NvgCanvas canvas;
   private static NVGLUFramebuffer fb;
   private static int fbW;
   private static int fbH;
   private static boolean valid;
   private static boolean failed;
   private static int lastSig;
   private static int lastCfg = Integer.MIN_VALUE;
   private static int lastTheme;
   private static boolean waypointsBroken;
   private static int shaderStyle;
   private static int accent2;
   private static float shaderIntensity = 0.6F;
   private static Shader dockShader;
   private static boolean shaderFailed;
   private static int sVao;
   private static int sVbo;
   private static final long START = System.nanoTime();

   private HotbarRenderer() {
   }

   public static void register() {
      HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, orig -> (g, dt) -> hotbar(g, dt, orig));

      for (Identifier id : new Identifier[]{VanillaHudElements.HEALTH_BAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH}) {
         HudElementRegistry.replaceElement(id, orig -> (g, dt) -> {
            if (!replacingStatus()) {
               orig.extractRenderState(g, dt);
            }
         });
      }

      for (Identifier id : new Identifier[]{VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL}) {
         HudElementRegistry.replaceElement(id, orig -> (g, dt) -> {
            if (!replacingXp()) {
               orig.extractRenderState(g, dt);
            }
         });
      }
   }

   private static boolean on() {
      return !failed && CustomHotbar.active();
   }

   private static boolean replacingStatus() {
      return on() && CustomHotbar.INSTANCE.statusBars();
   }

   private static boolean replacingXp() {
      if (on() && CustomHotbar.INSTANCE.experience()) {
         LocalPlayer p = Minecraft.getInstance().player;
         return p == null || !hasWaypoints(p);
      } else {
         return false;
      }
   }

   private static boolean hasWaypoints(LocalPlayer p) {
      if (waypointsBroken) {
         return false;
      }

      try {
         return p.connection.getWaypointManager().hasWaypoints();
      } catch (Throwable t) {
         waypointsBroken = true;
         return false;
      }
   }

   private static void hotbar(GuiGraphicsExtractor g, DeltaTracker dt, HudElement orig) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      CustomHotbar m = CustomHotbar.INSTANCE;
      if (on() && p != null) {
         boolean slots = m.hotbar() && !p.isSpectator() && GuiItems.available();
         boolean status = m.statusBars();
         boolean xp = m.experience() && !hasWaypoints(p);
         if (!slots) {
            orig.extractRenderState(g, dt);
         }

         if (slots || status || xp) {
            try {
               draw(g, dt, mc, p, m, slots, status, xp);
            } catch (Throwable t) {
               failed = true;
               Clarity.LOGGER.warn("[Clarity] Custom Hotbar disabled after an error", t);
            }
         }
      } else {
         orig.extractRenderState(g, dt);
      }
   }

   private static void draw(GuiGraphicsExtractor g, DeltaTracker dt, Minecraft mc, LocalPlayer p, CustomHotbar m, boolean slots, boolean status, boolean xp) {
      float partial = dt.getGameTimeDeltaPartialTick(true);
      fill(mc, p, partial, slots, status, xp);
      applySettings(m);
      int gs = Math.max(1, Math.round(mc.getWindow().getGuiScale()));
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      PAINTER.update(STATE, System.nanoTime());
      int sig = STATE.signature() * 31 + lastCfg * 17 + lastTheme;
      int w = 260 * gs;
      int h = 64 * gs * 2;
      if (!valid || sig != lastSig || PAINTER.animating() || shaderStyle != 0 && STATE.hotbar || w != fbW || h != fbH) {
         redraw(mc, gs, w, h);
         lastSig = sig;
      }

      int ox = sw / 2 - 130;
      int oy = sh - 64;
      int layerH = 64 * gs;
      blitLayer(g, gs, ox, oy, fbH, layerH);
      if (slots) {
         Inventory inv = p.getInventory();
         int iy = oy + HotbarPainter.itemY();

         for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty()) {
               item(g, p, st, ox + HotbarPainter.slotItemX(i), iy, i + 1, partial);
            }
         }

         ItemStack off = p.getOffhandItem();
         if (!off.isEmpty()) {
            item(g, p, off, ox + HotbarPainter.offhandItemX(STATE.offhandLeft), iy, 10, partial);
         }

         blitLayer(g, gs, ox, oy, layerH, layerH);
      }
   }

   private static void blitLayer(GuiGraphicsExtractor g, int gs, int ox, int oy, int v, int layerH) {
      g.pose().pushMatrix();
      g.pose().scale(1.0F / gs, 1.0F / gs);
      g.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, TEX_ID, ox * gs, oy * gs, 0.0F, v, fbW, layerH, fbW, -layerH, fbW, fbH);
      g.pose().popMatrix();
   }

   private static void item(GuiGraphicsExtractor g, LivingEntity p, ItemStack st, int x, int y, int seed, float partial) {
      float pop = st.getPopTime() - partial;
      boolean popping = pop > 0.0F;
      if (popping) {
         float s = 1.0F + pop / 5.0F;
         g.pose().pushMatrix();
         g.pose().translate(x + 8.0F, y + 12.0F);
         g.pose().scale(1.0F / s, (s + 1.0F) / 2.0F);
         g.pose().translate(-(x + 8.0F), -(y + 12.0F));
      }

      try {
         GuiItems.draw(g, p, st, x, y, seed);
      } finally {
         if (popping) {
            g.pose().popMatrix();
         }
      }
   }

   private static void fill(Minecraft mc, LocalPlayer p, float partial, boolean slots, boolean status, boolean xp) {
      HotbarState s = STATE;
      s.hotbar = slots;
      s.status = status;
      s.experience = xp;
      if (slots) {
         Inventory inv = p.getInventory();
         s.selected = Math.floorMod(inv.getSelectedSlot(), 9);

         for (int i = 0; i < 9; i++) {
            slot(s.slots[i], inv.getItem(i), p, partial);
         }

         slot(s.offhand, p.getOffhandItem(), p, partial);
         s.offhandLeft = p.getMainArm() == HumanoidArm.RIGHT;
      } else {
         s.clearSlots();
      }

      s.survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
      s.hasXp = mc.gameMode != null && mc.gameMode.hasExperience();
      s.health = p.getHealth();
      s.maxHealth = Math.max(p.getMaxHealth(), p.getHealth());
      s.absorption = p.getAbsorptionAmount();
      s.poison = p.hasEffect(MobEffects.POISON);
      s.wither = p.hasEffect(MobEffects.WITHER);
      s.regen = p.hasEffect(MobEffects.REGENERATION);
      s.hungerEffect = p.hasEffect(MobEffects.HUNGER);
      s.frozen = p.isFullyFrozen();
      s.hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
      s.armor = p.getArmorValue();
      FoodData fd = p.getFoodData();
      s.food = fd.getFoodLevel();
      s.saturation = fd.getSaturationLevel();
      s.maxAir = p.getMaxAirSupply();
      s.air = Math.min(p.getAirSupply(), s.maxAir);
      s.airShown = p.isEyeInFluid(FluidTags.WATER) || s.air < s.maxAir;
      s.xp = p.experienceProgress;
      s.level = p.experienceLevel;
      if (p.getVehicle() instanceof LivingEntity le && le.showVehicleHealth()) {
         s.mount = true;
         s.mountHealth = le.getHealth();
         s.mountMax = le.getMaxHealth();
      } else {
         s.mount = false;
         s.mountHealth = s.mountMax = 0.0F;
      }

      s.jump = p.jumpableVehicle() != null;
      s.jumpScale = s.jump ? p.getJumpRidingScale() : 0.0F;
   }

   private static void slot(HotbarState.Slot sl, ItemStack st, LocalPlayer p, float partial) {
      if (st.isEmpty()) {
         sl.present = false;
         sl.count = 0;
         sl.bar = -1.0F;
         sl.cooldown = 0.0F;
      } else {
         sl.present = true;
         sl.count = st.getCount();
         if (st.isBarVisible()) {
            sl.bar = Math.min(1.0F, st.getBarWidth() / 13.0F);
            sl.barColor = st.getBarColor();
         } else {
            sl.bar = -1.0F;
         }

         sl.cooldown = p.getCooldowns().getCooldownPercent(st, partial);
      }
   }

   private static void applySettings(CustomHotbar m) {
      int cfg = ClarityConfig.valuesHash("Custom Hotbar");
      int accent = m.themeColors() ? ThemeColors.getAccentRgb() : Theme.hex(m.accent(), 8037111);
      int surface = ThemeColors.getSurfaceRgb();
      int theme = accent * 31 + surface;
      if (cfg != lastCfg || theme != lastTheme) {
         lastCfg = cfg;
         lastTheme = theme;
         HotbarPainter pt = PAINTER;
         pt.icons = "Icons".equals(m.style());
         pt.accent = 0xFF000000 | accent;
         pt.surface = 0xFF000000 | surface;
         pt.bgOpacity = Math.max(0.0F, Math.min(1.0F, m.bgOpacity()));
         pt.rounding = Math.max(0.0F, m.rounding());
         pt.slotNumbers = m.slotNumbers();
         pt.numbers = m.numbers();
         pt.trail = m.damageTrail();
         pt.pulse = m.lowPulse();
         pt.smooth = m.smooth();
         pt.healthCol = 0xFF000000 | Theme.hex(m.healthColor(), 16731486);
         pt.absorbCol = 0xFF000000 | Theme.hex(m.absorbColor(), 16763200);
         pt.hungerCol = 0xFF000000 | Theme.hex(m.hungerColor(), 15901498);
         pt.armorCol = 0xFF000000 | Theme.hex(m.armorColor(), 11125992);
         pt.airCol = 0xFF000000 | Theme.hex(m.airColor(), 6015231);
         pt.xpCol = 0xFF000000 | Theme.hex(m.xpColor(), 8184667);

         shaderStyle = switch (m.shader()) {
            case "Flow" -> 1;
            case "Aurora" -> 2;
            case "Shimmer" -> 3;
            case "Edge Light" -> 4;
            default -> 0;
         };
         shaderIntensity = Math.max(0.0F, Math.min(1.0F, m.shaderIntensity()));
         accent2 = m.themeColors() ? ThemeColors.getAccent2Rgb() : accent;
      }
   }

   private static void redraw(Minecraft mc, int gs, int w, int h) {
      ensure(mc, w, h);

      try (RawGlState saved = RawGlState.capture()) {
         NanoVGGL3.nvgluBindFramebuffer(vg, fb);
         GL33C.glViewport(0, 0, fbW, fbH);
         GL33C.glDisable(3089);
         GL33C.glDisable(2929);
         GL33C.glColorMask(true, true, true, true);
         GL33C.glStencilMask(255);
         GL33C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL33C.glClearStencil(0);
         GL33C.glClear(17408);
         GL33C.glBindBuffer(35052, 0);
         GL33C.glPixelStorei(3317, 1);
         GL33C.glPixelStorei(3314, 0);
         GL33C.glPixelStorei(3316, 0);
         GL33C.glPixelStorei(3315, 0);
         GL33C.glBindSampler(0, 0);
         if (shaderStyle != 0 && STATE.hotbar) {
            NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
            canvas.begin(fbW, fbH);
            NanoVG.nvgSave(vg);
            NanoVG.nvgScale(vg, gs, gs);
            PAINTER.panels(canvas, STATE);
            NanoVG.nvgRestore(vg);
            NanoVG.nvgEndFrame(vg);
            drawShader(gs);
            NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
            canvas.begin(fbW, fbH);
            NanoVG.nvgSave(vg);
            NanoVG.nvgScale(vg, gs, gs);
         } else {
            NanoVG.nvgBeginFrame(vg, fbW, fbH, 1.0F);
            canvas.begin(fbW, fbH);
            NanoVG.nvgSave(vg);
            NanoVG.nvgScale(vg, gs, gs);
            PAINTER.panels(canvas, STATE);
         }

         PAINTER.base(canvas, STATE);
         NanoVG.nvgTranslate(vg, 0.0F, 64.0F);
         PAINTER.over(canvas, STATE);
         NanoVG.nvgRestore(vg);
         NanoVG.nvgEndFrame(vg);
      }

      valid = true;
   }

   private static void drawShader(int gs) {
      if (!shaderFailed) {
         try {
            if (dockShader == null) {
               dockShader = new Shader("hotbar", "dock");
            }
         } catch (Throwable t) {
            shaderFailed = true;
            Clarity.LOGGER.warn("[Clarity] Custom Hotbar shader failed to compile", t);
            return;
         }

         int layerH = 64 * gs;
         GL33C.glViewport(0, fbH - layerH, fbW, layerH);
         GL33C.glDisable(2929);
         GL33C.glDisable(2884);
         GL33C.glDisable(2960);
         GL33C.glDisable(3089);
         GL33C.glEnable(3042);
         GL33C.glBlendFunc(1, 771);
         dockShader.bind();
         float[] r = HotbarPainter.shaderRects(STATE);
         dockShader.setUniform4f("uRect0", r[0], r[1], r[2], r[3]);
         dockShader.setUniform4f("uRect1", r[4], r[5], r[6], r[7]);
         dockShader.setUniform1f("uRadius", PAINTER.rounding);
         dockShader.setUniform1f("uScale", gs);
         dockShader.setUniform1f("uFbH", fbH);
         dockShader.setUniform1f("uTime", (float)(System.nanoTime() - START) / 1.0E9F % 3600.0F);
         int a1 = PAINTER.accent;
         int a2 = accent2;
         dockShader.setUniform3f("uAccent", (a1 >> 16 & 0xFF) / 255.0F, (a1 >> 8 & 0xFF) / 255.0F, (a1 & 0xFF) / 255.0F);
         dockShader.setUniform3f("uAccent2", (a2 >> 16 & 0xFF) / 255.0F, (a2 >> 8 & 0xFF) / 255.0F, (a2 & 0xFF) / 255.0F);
         dockShader.setUniform1f("uIntensity", shaderIntensity);
         dockShader.setUniform1i("uStyle", shaderStyle);
         if (sVao == 0) {
            sVao = GL33C.glGenVertexArrays();
            sVbo = GL33C.glGenBuffers();
            GL33C.glBindVertexArray(sVao);
            GL33C.glBindBuffer(34962, sVbo);
            GL33C.glBufferData(34962, new float[]{-1.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, -1.0F, 3.0F, 0.0F}, 35044);
            GL33C.glEnableVertexAttribArray(0);
            GL33C.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
         }

         GL33C.glBindVertexArray(sVao);
         GL33C.glDrawArrays(4, 0, 3);
         GL33C.glBindVertexArray(0);
         dockShader.unbind();
         GL33C.glViewport(0, 0, fbW, fbH);
      }
   }

   private static void ensure(Minecraft mc, int w, int h) {
      if (vg == 0L) {
         vg = NanoVGGL3.nvgCreate(3);
         if (vg == 0L) {
            throw new IllegalStateException("nvgCreate failed");
         }

         ClarityFonts.loadInterInto(vg);
         canvas = new NvgCanvas(vg);
         canvas.setCrisp(true);
      }

      if (fb == null || fbW != w || fbH != h) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         fb = NanoVGGL3.nvgluCreateFramebuffer(vg, w, h, 0);
         if (fb == null) {
            throw new IllegalStateException("nvgluCreateFramebuffer failed");
         }

         fbW = w;
         fbH = h;
         valid = false;
         GlTexture tex = new HotbarRenderer.BridgeGlTexture(fb.texture(), fbW, fbH);
         GpuTextureView view = RenderSystem.getDevice().createTextureView(tex);
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
         mc.getTextureManager().register(TEX_ID, new HotbarRenderer.BridgeTexture(tex, view, sampler));
      }
   }

   public static void dispose() {
      if (dockShader != null) {
         dockShader.delete();
         dockShader = null;
      }

      if (sVao != 0) {
         GL33C.glDeleteVertexArrays(sVao);
         GL33C.glDeleteBuffers(sVbo);
         sVbo = 0;
         sVao = 0;
      }

      if (vg != 0L) {
         if (fb != null) {
            NanoVGGL3.nvgluDeleteFramebuffer(vg, fb);
            fb = null;
         }

         NanoVGGL3.nvgDelete(vg);
         vg = 0L;
      }

      valid = false;
   }

   private static final class BridgeGlTexture extends GlTexture {
      BridgeGlTexture(int glId, int w, int h) {
         super(12, "clarity-custom-hotbar", TextureFormat.RGBA8, w, h, 1, 1, glId);
      }

      public void close() {
      }
   }

   private static final class BridgeTexture extends AbstractTexture {
      BridgeTexture(GpuTexture tex, GpuTextureView view, GpuSampler sampler) {
         this.texture = tex;
         this.textureView = view;
         this.sampler = sampler;
      }

      public void close() {
      }
   }
}
