package need.clarity.modules.visual;

import java.util.List;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ClaritySounds;
import need.clarity.util.CustomSounds;
import need.clarity.util.ThemeColors;
import need.clarity.util.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public class Notifications extends ModuleBase {
   public static Notifications INSTANCE;
   private static final List<Notifications.Entry> ACTIVE = new java.util.ArrayList<>();

   public Notifications() {
      super("Notifications", "HUD");
      INSTANCE = this;
   }

   public long durationMs() {
      return (long)(Math.max(0.5F, this.getSetting("Duration", 3.0F)) * 1000.0F);
   }

   public String position() {
      return this.getSetting("Position", "Bottom Right");
   }

   public int offsetX() {
      return this.getSetting("X Offset", 12);
   }

   public int offsetY() {
      return this.getSetting("Y Offset", 12);
   }

   public float scale() {
      return Math.max(0.5F, Math.min(2.0F, this.getSetting("Scale", 1.0F)));
   }

   public float opacity() {
      return Math.max(0.1F, Math.min(1.0F, this.getSetting("Opacity", 1.0F)));
   }

   public int maxVisible() {
      return Math.max(1, Math.min(10, this.getSetting("Max Visible", 6)));
   }

   public String animation() {
      return this.getSetting("Animation", "Slide");
   }

   public boolean showEnabled() {
      return this.getSetting("Show Enabled", true);
   }

   public boolean showDisabled() {
      return this.getSetting("Show Disabled", true);
   }

   public boolean compact() {
      return this.getSetting("Compact", false);
   }

   public String enabledSound() {
      return this.getSetting("Enabled Sound", "Chime Up");
   }

   public String disabledSound() {
      return this.getSetting("Disabled Sound", "Chime Down");
   }

   public float soundVolume() {
      return Math.max(0.0F, Math.min(2.0F, this.getSetting("Volume", 1.0F)));
   }

   public float soundPitch() {
      return Math.max(0.5F, Math.min(2.0F, this.getSetting("Pitch", 1.0F)));
   }

   public String style() {
      return this.getSetting("Style", "Modern");
   }

   public int blurStrength() {
      return Math.max(1, Math.min(4, this.getSetting("Blur Strength", 2)));
   }

   public float blurTint() {
      return Math.max(0.0F, Math.min(0.9F, this.getSetting("Blur Tint", 0.35F)));
   }

   public boolean statusBar() {
      return this.getSetting("Status Bar", true);
   }

   public boolean progressBar() {
      return this.getSetting("Progress Bar", false);
   }

   public boolean useThemeAccent() {
      return this.getSetting("Theme Colors", false);
   }

   public int enabledColor() {
      return ThemeColors.resolveRgb(this.getSetting("Enabled Color", ""), -12845218, false) & 16777215;
   }

   public int disabledColor() {
      return ThemeColors.resolveRgb(this.getSetting("Disabled Color", ""), -50373, true) & 16777215;
   }

   @Override
   public void onDisable() {
      synchronized (ACTIVE) {
         ACTIVE.clear();
      }
   }

   public static void moduleToggled(String moduleName, boolean enabled) {
      Notifications n = INSTANCE;
      if (n != null && n.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null) {
            if (!enabled || n.showEnabled()) {
               if (enabled || n.showDisabled()) {
                  push(moduleName, enabled ? "Enabled" : "Disabled", enabled);
               }
            }
         }
      }
   }

   public static void push(String title, String subtitle, boolean positive) {
      push(title, subtitle, positive, false);
   }

   public static void push(String title, String subtitle, boolean positive, boolean silent) {
      Notifications n = INSTANCE;
      if (n != null && n.isEnabled()) {
         Notifications.Entry e = new Notifications.Entry(title, subtitle, positive, n.durationMs());
         if (!silent && !UiSounds.toggledRecently()) {
            n.playSound(positive);
         }

         synchronized (ACTIVE) {
            ACTIVE.removeIf(o -> o.title.equals(title));
            ACTIVE.add(e);

            while (ACTIVE.size() > n.maxVisible()) {
               ACTIVE.remove(0);
            }
         }
      }
   }

   private void playSound(boolean positive) {
      String label = positive ? this.enabledSound() : this.disabledSound();
      if (label != null && !"None".equals(label)) {
         if (CustomSounds.isCustom(label)) {
            CustomSounds.play(label, this.soundVolume(), this.soundPitch());
         } else {
            SoundEvent custom = uiSound(label);
            SoundEvent sound = custom != null ? custom : HitSounds.resolveSound(label);
            if (sound != null) {
               float volume = this.soundVolume();
               float pitch = this.soundPitch();
               if (!(volume <= 0.0F)) {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc != null) {
                     mc.execute(() -> mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume)));
                  }
               }
            }
         }
      }
   }

   private static SoundEvent uiSound(String label) {
      String id = switch (label) {
         case "Chime Up" -> "ui.chime_up";
         case "Chime Down" -> "ui.chime_down";
         case "Pop" -> "ui.pop";
         case "Toggle On" -> "ui.toggle_on";
         case "Toggle Off" -> "ui.toggle_off";
         case "Success" -> "ui.success";
         case "Error" -> "ui.error";
         default -> null;
      };
      return id == null ? null : ClaritySounds.raw(id);
   }

   public static List<Notifications.Entry> snapshot() {
      long now = System.currentTimeMillis();
      synchronized (ACTIVE) {
         ACTIVE.removeIf(e -> now - e.bornMs > e.lifeMs);
         return new java.util.ArrayList<>(ACTIVE);
      }
   }

   public static List<Notifications.Entry> previewSnapshot() {
      Notifications n = INSTANCE;
      int count = Math.min(3, n != null ? n.maxVisible() : 3);
      long born = System.currentTimeMillis() - 1000L;
      String[] names = new String[]{"Kill Aura", "Reach", "Sprint"};
      List<Notifications.Entry> out = new java.util.ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         boolean on = i % 2 == 0;
         out.add(new Notifications.Entry(names[i], on ? "Enabled" : "Disabled", on, born, 3600000L));
      }

      return out;
   }

   public static boolean wantsBackdrop() {
      Notifications n = INSTANCE;
      return n != null && n.isEnabled() && "Blur".equals(n.style()) ? !snapshot().isEmpty() : false;
   }

   public static boolean hasActive() {
      Notifications n = INSTANCE;
      return n != null && n.isEnabled() ? !snapshot().isEmpty() : false;
   }

   public static final class Entry {
      public final String title;
      public final String subtitle;
      public final boolean positive;
      public final long bornMs;
      public final long lifeMs;

      Entry(String title, String subtitle, boolean positive, long lifeMs) {
         this(title, subtitle, positive, System.currentTimeMillis(), lifeMs);
      }

      Entry(String title, String subtitle, boolean positive, long bornMs, long lifeMs) {
         this.title = title;
         this.subtitle = subtitle;
         this.positive = positive;
         this.bornMs = bornMs;
         this.lifeMs = lifeMs;
      }
   }
}
