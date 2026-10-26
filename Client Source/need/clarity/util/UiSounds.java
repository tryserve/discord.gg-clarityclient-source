package need.clarity.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import need.clarity.clickgui.ConfigStore;
import need.clarity.clickgui.GuiData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class UiSounds {
   private static final Map<UiSounds.Sound, Long> LAST = new EnumMap<>(UiSounds.Sound.class);
   private static volatile long lastToggleAt;
   private static ConfigStore store;
   private static boolean inClick;
   private static boolean playedInClick;
   private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

   private UiSounds() {
   }

   private static ConfigStore store() {
      if (store == null) {
         store = new ConfigStore(GuiData.get());
      }

      return store;
   }

   private static Object gui(String key) {
      try {
         return store().gui(key);
      } catch (Throwable t) {
         return null;
      }
   }

   private static boolean flag(String key, boolean def) {
      Object v = gui(key);
      if (v instanceof Boolean b) {
         return b;
      } else {
         return v != null ? "true".equalsIgnoreCase(String.valueOf(v)) : def;
      }
   }

   private static float num(String key, float def) {
      Object v = gui(key);
      if (v instanceof Number n) {
         return n.floatValue();
      } else {
         try {
            return v == null ? def : Float.parseFloat(String.valueOf(v));
         } catch (NumberFormatException e) {
            return def;
         }
      }
   }

   public static boolean uiOn() {
      return flag("UI Sounds", true);
   }

   public static boolean socialOn() {
      return flag("Social Sounds", true);
   }

   public static boolean sliderTicks() {
      return flag("Slider Ticks", true);
   }

   public static boolean chatSounds() {
      return flag("Chat Message Sound", false);
   }

   private static SoundEvent event(UiSounds.Sound s) {
      synchronized (EVENTS) {
         return EVENTS.computeIfAbsent(s.id, ClaritySounds::raw);
      }
   }

   public static void play(UiSounds.Sound s) {
      play(s, 1.0F);
   }

   public static void play(UiSounds.Sound s, float pitch) {
      if (s != null) {
         if (s.social ? socialOn() : uiOn()) {
            long now = System.currentTimeMillis();
            synchronized (LAST) {
               Long last = LAST.get(s);
               if (last != null && now - last < s.minGapMs) {
                  if (inClick) {
                     playedInClick = true;
                  }

                  return;
               }

               LAST.put(s, now);
            }

            if (inClick) {
               playedInClick = true;
            }

            if (s == UiSounds.Sound.TOGGLE_ON || s == UiSounds.Sound.TOGGLE_OFF) {
               lastToggleAt = now;
            }

            float vol = Math.max(0.0F, Math.min(1.0F, s.social ? num("Social Volume", 0.8F) : num("UI Volume", 0.55F)));
            if (!(vol <= 0.001F)) {
               float p = pitch;
               if (s == UiSounds.Sound.CLICK || s == UiSounds.Sound.TICK || s == UiSounds.Sound.TAB) {
                  p *= 0.97F + ThreadLocalRandom.current().nextFloat() * 0.06F;
               }

               float fp = Math.max(0.5F, Math.min(2.0F, p));
               Minecraft mc = Minecraft.getInstance();
               if (mc != null) {
                  Runnable r = () -> {
                     try {
                        SoundEvent ev = event(s);
                        if (ev != null) {
                           mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, fp, vol));
                        }
                     } catch (Throwable var5x) {
                     }
                  };
                  mc.execute(r);
               }
            }
         }
      }
   }

   public static void tick(float fraction) {
      if (!sliderTicks()) {
         if (inClick) {
            playedInClick = true;
         }
      } else {
         play(UiSounds.Sound.TICK, 0.8F + 0.45F * Math.max(0.0F, Math.min(1.0F, fraction)));
      }
   }

   public static void toggle(boolean on) {
      play(on ? UiSounds.Sound.TOGGLE_ON : UiSounds.Sound.TOGGLE_OFF);
   }

   public static boolean toggledRecently() {
      return System.currentTimeMillis() - lastToggleAt < 180L;
   }

   public static void beginClick() {
      inClick = true;
      playedInClick = false;
   }

   public static void endClick(boolean silent) {
      boolean played = playedInClick;
      inClick = false;
      playedInClick = false;
      if (!played && !silent) {
         play(UiSounds.Sound.CLICK);
      }
   }

   public enum Sound {
      CLICK("ui.click", false, 25L),
      TICK("ui.tick", false, 32L),
      TAB("ui.tab", false, 40L),
      TOGGLE_ON("ui.toggle_on", false, 40L),
      TOGGLE_OFF("ui.toggle_off", false, 40L),
      OPEN("ui.open", false, 150L),
      CLOSE("ui.close", false, 150L),
      POP("ui.pop", false, 60L),
      ERROR("ui.error", false, 200L),
      SUCCESS("ui.success", false, 200L),
      CHIME_UP("ui.chime_up", false, 60L),
      CHIME_DOWN("ui.chime_down", false, 60L),
      REQUEST("social.request", true, 400L),
      ACCEPTED("social.accepted", true, 400L),
      ONLINE("social.online", true, 300L),
      MESSAGE("social.message", true, 250L),
      MENTION("social.mention", true, 300L),
      CHAT("social.chat", true, 120L);

      final String id;
      final boolean social;
      final long minGapMs;

      Sound(String id, boolean social, long minGapMs) {
         this.id = id;
         this.social = social;
         this.minGapMs = minGapMs;
      }
   }
}
