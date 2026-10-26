package need.clarity.modules.combat;

import java.lang.reflect.Field;
import java.util.Random;
import need.clarity.modules.ModuleBase;
import need.clarity.util.AccessorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.HitResult.Type;

public class AutoClicker extends ModuleBase {
   private static final Field MISS_TIME_FIELD;
   private final Random random = new Random();
   private long lastTickNanos = 0L;
   private long budgetNanos = 0L;
   private long intervalNanos = 0L;
   private boolean wasActive = false;

   public AutoClicker() {
      super("Auto Clicker", "Combat");
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (this.getSetting("Not in GUI", true) && mc.screen != null) {
            this.reset();
         } else {
            boolean physicalLeftHeld = mc.options.keyAttack.isDown();
            if (!physicalLeftHeld) {
               this.reset();
            } else if (this.getSetting("Not While Using", true) && mc.player.isUsingItem()) {
               this.reset();
            } else if (this.getSetting("Stop On Break", true) && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK && mc.gameMode != null && mc.gameMode.isDestroying()) {
               this.reset();
            } else {
               if (this.getSetting("Only Weapon", false)) {
                  ItemStack held = mc.player.getMainHandItem();
                  if (!this.isWeapon(held)) {
                     this.reset();
                     return;
                  }
               }

               setMissTime(mc, 0);
               long now = System.nanoTime();
               if (!this.wasActive) {
                  this.wasActive = true;
                  this.lastTickNanos = now;
                  this.budgetNanos = 0L;
                  this.intervalNanos = this.randomIntervalNanos();
               } else {
                  long elapsed = now - this.lastTickNanos;
                  this.lastTickNanos = now;
                  if (elapsed > 200000000L) {
                     elapsed = 50000000L;
                  }

                  for (this.budgetNanos += elapsed; this.budgetNanos >= this.intervalNanos; this.intervalNanos = this.randomIntervalNanos()) {
                     this.injectClick(mc);
                     this.budgetNanos = this.budgetNanos - this.intervalNanos;
                  }
               }
            }
         }
      }
   }

   private void injectClick(Minecraft mc) {
      AccessorCompat.addClick(mc.options.keyAttack);
   }

   private boolean isWeapon(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.is(ItemTags.SWORDS)) {
         return true;
      } else {
         return stack.is(ItemTags.AXES) ? true : stack.getItem() instanceof TridentItem;
      }
   }

   private long randomIntervalNanos() {
      int lo = this.getSetting("CPS Low", 9);
      int hi = this.getSetting("CPS High", 12);
      if (lo <= 0) {
         lo = 1;
      }

      if (hi <= 0) {
         hi = 1;
      }

      if (hi < lo) {
         hi = lo;
      }

      int cps = lo + (hi > lo ? this.random.nextInt(hi - lo + 1) : 0);
      return 1000000000L / cps;
   }

   private void reset() {
      this.wasActive = false;
      this.budgetNanos = 0L;
      this.lastTickNanos = 0L;
   }

   private static void setMissTime(Minecraft mc, int value) {
      if (MISS_TIME_FIELD != null) {
         try {
            MISS_TIME_FIELD.setInt(mc, value);
         } catch (IllegalAccessException var3) {
         }
      }
   }

   static {
      Field f = null;

      try {
         f = Minecraft.class.getDeclaredField("missTime");
         f.setAccessible(true);
      } catch (NoSuchFieldException var2) {
      }

      MISS_TIME_FIELD = f;
   }
}
