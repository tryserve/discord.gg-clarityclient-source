package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import need.clarity.WorldRenderEvent;
import need.clarity.audio.AudioDemand;
import need.clarity.audio.BeatAnalyzer;
import need.clarity.modules.ModuleBase;
import need.clarity.render.worldfx.WorldFx;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MusicBeats extends ModuleBase {
   private final WorldRenderEvent.Listener renderListener = this::onWorldRender;
   private final float[] bands = new float[32];
   private final float[] peaks = new float[32];
   private final float[] ages = new float[4];
   private final AudioDemand.Frame audioFrame = new AudioDemand.Frame();
   private double clock = 0.0;
   private long lastNanos = 0L;
   private boolean warned = false;

   public MusicBeats() {
      super("Music Beats", "Visual");
      if (this.isEnabled()) {
         WorldRenderEvent.register(this.renderListener);
      }
   }

   @Override
   public void onEnable() {
      WorldRenderEvent.register(this.renderListener);
      this.warned = false;
   }

   @Override
   public void onDisable() {
      WorldRenderEvent.unregister(this.renderListener);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         AudioDemand.want(this.getSetting("Sensitivity", 1.0F));
      }
   }

   private void onWorldRender(PoseStack poseStack, float partialTick) {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null) {
            boolean firstPerson = mc.options.getCameraType().isFirstPerson() && !Freecam.isActive();
            if (!firstPerson || !this.getSetting("Hide In First Person", false)) {
               long now = System.nanoTime();
               double dt = this.lastNanos == 0L ? 0.0 : Math.min(0.1, (now - this.lastNanos) / 1.0E9);
               this.lastNanos = now;
               this.clock = (this.clock + dt) % 3600.0;
               float bass;
               if (BeatAnalyzer.hasAudio()) {
                  this.audioFrame.reactivity = this.getSetting("Reactivity", 0.8F);
                  AudioDemand.sample(this.audioFrame, this.clock, 0.0F);
                  System.arraycopy(this.audioFrame.bands, 0, this.bands, 0, this.bands.length);

                  for (int i = 0; i < this.peaks.length; i++) {
                     this.peaks[i] = Math.max(this.bands[i], this.peaks[i] - 0.004F);
                  }

                  System.arraycopy(this.audioFrame.beatAges, 0, this.ages, 0, this.ages.length);
                  bass = this.audioFrame.bass;
               } else {
                  for (int i = 0; i < this.bands.length; i++) {
                     this.bands[i] = 0.08F + 0.06F * (float)Math.sin(this.clock * 2.0 + i * 0.5);
                     this.peaks[i] = this.bands[i];
                  }

                  Arrays.fill(this.ages, -1.0F);
                  bass = 0.1F;
                  if (!this.warned) {
                     this.warned = true;
                  }
               }

               Player p = mc.player;
               Vec3 cam = ClarityRenderUtils.getCameraPos();
               double x = p.xOld + (p.getX() - p.xOld) * partialTick;
               double y = p.yOld + (p.getY() - p.yOld) * partialTick;
               double z = p.zOld + (p.getZ() - p.zOld) * partialTick;
               float cx = (float)(x - cam.x);
               float cy = (float)(y - cam.y);
               float cz = (float)(z - cam.z);
               int n = Math.max(16, Math.min(128, this.getSetting("Bars", 64))) & -2;
               float radius = this.getSetting("Radius", 1.1F);
               float barH = this.getSetting("Bar Height", 1.0F);
               float spin = this.getSetting("Spin", 0.15F);
               int c1 = ThemeColors.resolveRgb(this.getSetting("Color", "#4AD6FF"), 4904703, false) & 16777215;
               int c2 = ThemeColors.resolveRgb(this.getSetting("Second Color", "#B45CFF"), 11820287, true) & 16777215;
               boolean rainbow = "Rainbow".equals(this.getSetting("Color Mode", "Gradient"));
               boolean ground = this.getSetting("Ground Ring", true);
               float opacity = Math.max(0.05F, Math.min(1.0F, this.getSetting("Opacity", 1.0F)));
               float t = (float)this.clock;
               float b = bass;
               WorldFx.drawInstanced("beats", n + 1, this.getSetting("Through Walls", false), 0, s -> {
                  s.setUniform3f("uCenter", cx, cy, cz);
                  s.setUniform1f("uRadius", radius);
                  s.setUniform1f("uBarH", barH);
                  s.setUniform1f("uTime", t);
                  s.setUniform1f("uSpin", spin);
                  s.setUniform1i("uN", n);
                  s.setUniform1fv("uBands", this.bands);
                  s.setUniform1fv("uPeaks", this.peaks);
                  s.setUniform1fv("uBeatAges", this.ages);
                  s.setUniform1f("uBass", b);
                  s.setUniform3f("uColor1", (c1 >> 16 & 0xFF) / 255.0F, (c1 >> 8 & 0xFF) / 255.0F, (c1 & 0xFF) / 255.0F);
                  s.setUniform3f("uColor2", (c2 >> 16 & 0xFF) / 255.0F, (c2 >> 8 & 0xFF) / 255.0F, (c2 & 0xFF) / 255.0F);
                  s.setUniform1i("uRainbow", rainbow ? 1 : 0);
                  s.setUniform1i("uGround", ground ? 1 : 0);
                  s.setUniform1f("uOpacity", opacity);
               });
            }
         }
      }
   }
}
