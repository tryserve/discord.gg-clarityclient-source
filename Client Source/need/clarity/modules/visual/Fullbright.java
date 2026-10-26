package need.clarity.modules.visual;

import java.lang.reflect.Field;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends ModuleBase {
   private static final double FULLBRIGHT_GAMMA = 16.0;
   private double savedGamma = 1.0;
   private static Field optionValueField;

   public Fullbright() {
      super("Fullbright", "Visual");
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (this.isGammaMode(mc)) {
            if (mc.options != null) {
               this.savedGamma = (Double)mc.options.gamma().get();
               setGammaUnclamped(mc, 16.0);
            }
         } else {
            applyNightVision(mc);
         }
      }
   }

   @Override
   public void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (mc.options != null) {
            setGammaUnclamped(mc, this.savedGamma);
         }

         removeNightVision(mc);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (this.isGammaMode(mc)) {
            removeNightVision(mc);
            if (mc.options != null) {
               setGammaUnclamped(mc, 16.0);
            }
         } else {
            if (mc.options != null && (Double)mc.options.gamma().get() > 1.0) {
               setGammaUnclamped(mc, this.savedGamma);
            }

            applyNightVision(mc);
         }
      }
   }

   private boolean isGammaMode(Minecraft mc) {
      Object raw = this.getSetting("Mode", "Gamma");
      return raw == null || "Gamma".equals(raw.toString());
   }

   private static void applyNightVision(Minecraft mc) {
      if (mc.player != null) {
         Holder<MobEffect> nvEffect = MobEffects.NIGHT_VISION;
         MobEffectInstance existing = mc.player.getEffect(nvEffect);
         if (existing == null || existing.getDuration() < 40) {
            mc.player.addEffect(new MobEffectInstance(nvEffect, Integer.MAX_VALUE, 0, false, false, false));
         }
      }
   }

   private static void removeNightVision(Minecraft mc) {
      if (mc.player != null) {
         mc.player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   private static void setGammaUnclamped(Minecraft mc, double gamma) {
      if (mc.options != null) {
         if (optionValueField == null) {
            mc.options.gamma().set(Math.min(gamma, 1.0));
         } else {
            try {
               optionValueField.set(mc.options.gamma(), gamma);
            } catch (IllegalAccessException var4) {
            }
         }
      }
   }

   static {
      try {
         optionValueField = OptionInstance.class.getDeclaredField("value");
         optionValueField.setAccessible(true);
      } catch (NoSuchFieldException e) {
         try {
            optionValueField = OptionInstance.class.getDeclaredField("f_231548_");
            optionValueField.setAccessible(true);
         } catch (NoSuchFieldException var2) {
         }
      }
   }
}
