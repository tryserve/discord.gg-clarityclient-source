package need.clarity.modules.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import need.clarity.PerFrameEvents;
import need.clarity.keybind.KeybindHandler;
import need.clarity.modules.ModuleBase;
import need.clarity.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class Inspect extends ModuleBase {
   public static Inspect INSTANCE;
   public static final String[] ANIMATIONS = new String[]{"Karambit", "Butterfly", "Flip", "Twirl", "Showcase", "Huntsman", "Toss", "Spin", "Gut Knife", "Bayonet"};
   private final Random random = new Random();
   private boolean keyWasDown = false;
   private long startNanos = 0L;
   private String playing = "";
   private float[][] keys;
   private float duration = 1.6F;
   private final float[] fadeFrom = new float[12];
   private long fadeStart = 0L;
   private ItemStack inspected = ItemStack.EMPTY;
   private int inspectedSlot = -1;
   private final float[] cur = new float[12];
   private long sampledAt = -1L;
   private int lastSlot = -1;
   private Item lastItem;
   private static final float FADE_S = 0.18F;

   public Inspect() {
      super("Inspect", "Visual");
      INSTANCE = this;
      PerFrameEvents.register(this::onFrame);
   }

   private static float[][] frames(String anim) {
      return switch (anim) {
         case "Karambit" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.14F, -0.1F, 0.1F, 0.04F, -10.0F, -25.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.22F, -0.11F, 0.11F, 0.04F, -12.0F, -28.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -90.0F},
            {0.5F, -0.12F, 0.12F, 0.04F, -12.0F, -30.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -540.0F},
            {0.62F, -0.12F, 0.1F, 0.04F, -8.0F, -30.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -720.0F},
            {0.78F, -0.06F, 0.06F, 0.02F, 12.0F, 35.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -720.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -720.0F}
         };
         case "Butterfly" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.1F, -0.08F, 0.08F, 0.0F, -5.0F, -15.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.2F, -0.08F, 0.09F, 0.0F, -5.0F, -15.0F, 14.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F, 0.0F},
            {0.3F, -0.08F, 0.08F, 0.0F, -5.0F, -15.0F, 6.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F, 0.0F},
            {0.4F, -0.08F, 0.09F, 0.0F, -5.0F, -10.0F, 14.0F, 0.0F, 0.0F, 0.0F, 360.0F, 180.0F, 0.0F},
            {0.5F, -0.08F, 0.08F, 0.0F, -5.0F, -10.0F, 6.0F, 0.0F, 0.0F, 0.0F, 540.0F, 180.0F, 0.0F},
            {0.6F, -0.08F, 0.09F, 0.0F, -5.0F, -10.0F, 14.0F, 0.0F, 0.0F, 0.0F, 720.0F, 360.0F, 0.0F},
            {0.72F, -0.1F, 0.08F, 0.02F, -10.0F, 30.0F, -8.0F, 0.0F, 0.0F, 0.0F, 720.0F, 360.0F, 0.0F},
            {0.86F, -0.04F, 0.04F, 0.0F, 5.0F, -20.0F, 10.0F, 0.0F, 0.0F, 0.0F, 720.0F, 360.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 720.0F, 360.0F, 0.0F}
         };
         case "Flip" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.18F, -0.04F, 0.02F, 0.0F, 15.0F, -10.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.3F, -0.04F, 0.08F, 0.0F, -10.0F, -10.0F, 5.0F, 0.0F, 0.1F, 0.0F, -180.0F, 0.0F, 0.0F},
            {0.42F, -0.04F, 0.04F, 0.0F, 0.0F, -10.0F, 5.0F, 0.0F, 0.02F, 0.0F, -360.0F, 0.0F, 0.0F},
            {0.7F, -0.1F, 0.08F, 0.03F, -15.0F, -35.0F, 15.0F, 0.0F, 0.0F, 0.0F, -360.0F, 0.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -360.0F, 0.0F, 0.0F}
         };
         case "Twirl" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.2F, -0.08F, 0.08F, 0.03F, -15.0F, -20.0F, 15.0F, 0.0F, 0.0F, 0.0F, 0.0F, 90.0F, 0.0F},
            {0.5F, -0.1F, 0.1F, 0.04F, -20.0F, -25.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
            {0.8F, -0.06F, 0.06F, 0.02F, -5.0F, 20.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 720.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 720.0F, 0.0F}
         };
         case "Showcase" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.22F, -0.18F, 0.06F, 0.06F, -8.0F, -40.0F, 22.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.45F, -0.19F, 0.07F, 0.06F, -10.0F, -42.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.68F, -0.05F, 0.1F, 0.04F, -28.0F, 30.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.85F, -0.04F, 0.09F, 0.03F, -30.0F, 32.0F, -14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}
         };
         case "Huntsman" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.25F, -0.1F, 0.1F, 0.05F, -30.0F, -15.0F, 40.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.5F, -0.1F, 0.1F, 0.05F, -30.0F, -15.0F, 40.0F, 0.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F},
            {0.75F, -0.08F, 0.08F, 0.03F, -15.0F, 20.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F}
         };
         case "Toss" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.15F, 0.0F, -0.05F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.25F, 0.0F, 0.03F, 0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.15F, 0.0F, -180.0F, 0.0F, 0.0F},
            {0.45F, 0.0F, -0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.55F, 0.0F, -540.0F, 90.0F, 0.0F},
            {0.65F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, -900.0F, 180.0F, 0.0F},
            {0.75F, 0.0F, -0.06F, 0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1080.0F, 180.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1080.0F, 360.0F, 0.0F}
         };
         case "Spin" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.12F, -0.08F, 0.06F, 0.02F, 0.0F, -10.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -60.0F},
            {0.88F, -0.08F, 0.06F, 0.02F, 0.0F, -10.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1020.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1080.0F}
         };
         case "Gut Knife" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.2F, -0.06F, 0.08F, 0.03F, -20.0F, -10.0F, 35.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.45F, -0.06F, 0.08F, 0.03F, -20.0F, 25.0F, -25.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.6F, -0.06F, 0.08F, 0.03F, -15.0F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F, 0.0F}
         };
         case "Bayonet" -> new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.18F, -0.08F, 0.1F, 0.02F, -10.0F, -30.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
            {0.4F, -0.08F, 0.12F, 0.02F, -10.0F, -30.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
            {0.62F, -0.14F, 0.06F, 0.08F, 30.0F, -50.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
            {0.8F, -0.1F, 0.06F, 0.05F, 20.0F, -40.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
            {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F}
         };
         default -> new float[][]{{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}, {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}};
      };
   }

   @Override
   public void onDisable() {
      this.startNanos = 0L;
      this.fadeStart = 0L;
   }

   public static boolean playing() {
      Inspect i = INSTANCE;
      return i != null && i.isEnabled() && (i.startNanos != 0L || i.fadeStart != 0L);
   }

   private void onFrame() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer p = mc.player;
         if (p != null && mc.level != null) {
            boolean down = mc.screen == null && isBindDown(mc, this.getSetting("Inspect Key", "Y"));
            boolean pressed = down && !this.keyWasDown;
            this.keyWasDown = down;
            int slot = InventoryHelper.getSelected(p.getInventory());
            ItemStack held = p.getMainHandItem();
            boolean switched = slot != this.lastSlot || this.lastItem != null && held.getItem() != this.lastItem;
            this.lastSlot = slot;
            this.lastItem = held.getItem();
            if (this.startNanos != 0L) {
               boolean cancel = slot != this.inspectedSlot
                  || held.getItem() != this.inspected.getItem()
                  || this.getSetting("Cancel On Attack", true) && (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown());
               if (cancel) {
                  this.cancel();
               }
            }

            if (pressed) {
               this.start(held, slot);
            } else if (switched && this.getSetting("Inspect On Switch", false) && !held.isEmpty()) {
               this.start(held, slot);
            } else if (this.startNanos != 0L && down && this.getSetting("Loop While Held", false) && this.progress() >= 1.0F) {
               this.start(held, slot);
            }
         } else {
            this.startNanos = 0L;
            this.fadeStart = 0L;
         }
      }
   }

   private void start(ItemStack held, int slot) {
      if (!held.isEmpty() || this.getSetting("Empty Hand", false)) {
         String a = this.getSetting("Animation", "Auto");
         if ("Random".equals(a)) {
            a = ANIMATIONS[this.random.nextInt(ANIMATIONS.length)];
         } else if ("Auto".equals(a)) {
            a = this.autoFor(held);
         }

         this.playing = a;
         this.keys = frames(a);
         this.duration = Math.max(0.3F, this.getSetting("Duration", 1.6F) / Math.max(0.1F, this.getSetting("Speed", 1.0F)));
         this.inspected = held.copy();
         this.inspectedSlot = slot;
         this.startNanos = System.nanoTime();
         this.fadeStart = 0L;
      }
   }

   private void cancel() {
      this.sample();

      for (int i = 0; i < 12; i++) {
         this.fadeFrom[i] = i < 9 && (i < 3 || i >= 6) ? this.cur[i] : wrap(this.cur[i]);
      }

      this.startNanos = 0L;
      this.fadeStart = System.nanoTime();
   }

   private String autoFor(ItemStack st) {
      String id = String.valueOf(BuiltInRegistries.ITEM.getKey(st.getItem())).toLowerCase(Locale.ROOT);
      if (id.contains("sword") || id.contains("spear") || id.contains("dagger") || id.contains("knife")) {
         return this.random.nextBoolean() ? "Karambit" : "Butterfly";
      } else if (id.contains("_axe") || id.contains("mace") || id.contains("trident")) {
         return this.random.nextBoolean() ? "Huntsman" : "Gut Knife";
      } else if (id.contains("bow") || id.contains("crossbow") || id.contains("shield")) {
         return "Showcase";
      } else if (id.contains("pickaxe") || id.contains("shovel") || id.contains("hoe")) {
         return "Twirl";
      } else {
         return st.getItem() instanceof BlockItem ? "Toss" : "Flip";
      }
   }

   private float progress() {
      return this.startNanos == 0L ? 1.0F : Math.min(1.0F, (float)(System.nanoTime() - this.startNanos) / 1.0E9F / this.duration);
   }

   private float[] sample() {
      long now = System.nanoTime();
      if (now == this.sampledAt) {
         return this.cur;
      }

      this.sampledAt = now;
      Arrays.fill(this.cur, 0.0F);
      if (this.startNanos != 0L && this.keys != null) {
         float t = this.progress();
         if (t >= 1.0F && (!this.getSetting("Loop While Held", false) || !this.keyWasDown)) {
            this.startNanos = 0L;
            return this.cur;
         }

         interpolate(this.keys, this.ease(t), this.cur);
      } else if (this.fadeStart != 0L) {
         float f = (float)(now - this.fadeStart) / 1.0E9F / 0.18F;
         if (f >= 1.0F) {
            this.fadeStart = 0L;
            return this.cur;
         }

         float w = 1.0F - smooth(f);

         for (int i = 0; i < 12; i++) {
            this.cur[i] = this.fadeFrom[i] * w;
         }
      }

      float amt = this.getSetting("Intensity", 1.0F);
      if (amt != 1.0F) {
         for (int i = 0; i < 6; i++) {
            this.cur[i] = this.cur[i] * amt;
         }
      }

      return this.cur;
   }

   private float ease(float t) {
      float s = Math.max(0.0F, Math.min(1.0F, this.getSetting("Smoothness", 0.5F)));
      return t * (1.0F - s) + smooth(t) * s;
   }

   private static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   private static void interpolate(float[][] k, float t, float[] out) {
      int n = k.length;
      int i = 0;

      while (i < n - 2 && t > k[i + 1][0]) {
         i++;
      }

      float t0 = k[i][0];
      float t1 = k[i + 1][0];
      float u = t1 > t0 ? Math.max(0.0F, Math.min(1.0F, (t - t0) / (t1 - t0))) : 1.0F;
      float[] a = k[i];
      float[] b = k[i + 1];
      float[] pa = i > 0 ? k[i - 1] : null;
      float[] nb = i + 2 < n ? k[i + 2] : null;
      float u2 = u * u;
      float u3 = u2 * u;
      float h00 = 2.0F * u3 - 3.0F * u2 + 1.0F;
      float h10 = u3 - 2.0F * u2 + u;
      float h01 = -2.0F * u3 + 3.0F * u2;
      float h11 = u3 - u2;
      float dt = t1 - t0;

      for (int c = 0; c < 12; c++) {
         float va = a[c + 1];
         float vb = b[c + 1];
         float ma = pa == null ? 0.0F : (vb - pa[c + 1]) / Math.max(1.0E-4F, b[0] - pa[0]) * dt;
         float mb = nb == null ? 0.0F : (nb[c + 1] - va) / Math.max(1.0E-4F, nb[0] - a[0]) * dt;
         out[c] = h00 * va + h10 * ma + h01 * vb + h11 * mb;
      }
   }

   private static float wrap(float deg) {
      float d = deg % 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private static boolean mainHand(InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND;
   }

   private static float side() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
   }

   public static void applyHand(PoseStack pose, InteractionHand hand, float equip) {
      Inspect m = INSTANCE;
      if (m != null && m.isEnabled() && mainHand(hand) && playing()) {
         float[] v = m.sample();
         if (!allZero(v, 0, 6)) {
            float s = side();
            float ax = s * 0.56F;
            float ay = -0.52F - equip * 0.6F;
            float az = -0.72F;
            pose.translate(ax + s * v[0], ay + v[1], az + v[2]);
            pose.mulPose(Axis.YP.rotationDegrees(s * v[4]));
            pose.mulPose(Axis.XP.rotationDegrees(v[3]));
            pose.mulPose(Axis.ZP.rotationDegrees(s * v[5]));
            pose.translate(-ax, -ay, -az);
         }
      }
   }

   public static void applyItem(PoseStack pose, InteractionHand hand) {
      Inspect m = INSTANCE;
      if (m != null && m.isEnabled() && mainHand(hand) && playing()) {
         float[] v = m.sample();
         if (!allZero(v, 6, 12)) {
            float s = side();
            pose.translate(s * v[6], v[7], v[8]);
            pose.mulPose(Axis.ZP.rotationDegrees(s * v[11]));
            pose.mulPose(Axis.XP.rotationDegrees(v[9]));
            pose.mulPose(Axis.YP.rotationDegrees(s * v[10]));
         }
      }
   }

   public static boolean hidesOffhand(InteractionHand hand) {
      Inspect m = INSTANCE;
      return m != null && hand == InteractionHand.OFF_HAND && playing() && m.getSetting("Hide Off Hand", true);
   }

   private static boolean allZero(float[] v, int from, int to) {
      for (int i = from; i < to; i++) {
         if (Math.abs(v[i]) > 1.0E-4F) {
            return false;
         }
      }

      return true;
   }

   private static boolean isBindDown(Minecraft mc, String name) {
      if (name != null && !name.isEmpty() && !"NONE".equalsIgnoreCase(name)) {
         long window = mc.getWindow().handle();
         int mouse = KeybindHandler.mouseButtonIndex(name);
         if (mouse >= 0) {
            return GLFW.glfwGetMouseButton(window, mouse) == 1;
         }

         int key = KeybindHandler.glfwKeyCode(name);
         return key == -1 ? false : GLFW.glfwGetKey(window, key) == 1;
      } else {
         return false;
      }
   }
}
