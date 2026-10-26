package need.clarity.render;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import need.clarity.Clarity;
import need.clarity.clickgui.NvgCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GuiItems {
   private static MethodHandle itemMh;
   private static MethodHandle scissorOn;
   private static MethodHandle scissorOff;
   private static boolean searched;
   private static final Map<String, ItemStack> STACKS = new HashMap<>();

   private GuiItems() {
   }

   public static boolean available() {
      search();
      return itemMh != null;
   }

   private static void search() {
      if (!searched) {
         searched = true;
         Method best = null;
         Method on = null;
         Method off = null;

         for (Method mt : GuiGraphicsExtractor.class.getMethods()) {
            if (!Modifier.isStatic(mt.getModifiers()) && mt.getReturnType() == void.class) {
               Class<?>[] t = mt.getParameterTypes();
               if (t.length == 5
                  && t[0] == LivingEntity.class
                  && t[1] == ItemStack.class
                  && t[2] == int.class
                  && t[3] == int.class
                  && t[4] == int.class
                  && (best == null || mt.getName().toLowerCase(Locale.ROOT).contains("item"))) {
                  best = mt;
               }

               String n = mt.getName().toLowerCase(Locale.ROOT);
               if (n.contains("scissor")) {
                  if (n.contains("enable") && t.length == 4 && t[0] == int.class && t[1] == int.class && t[2] == int.class && t[3] == int.class) {
                     on = mt;
                  }

                  if (n.contains("disable") && t.length == 0) {
                     off = mt;
                  }
               }
            }
         }

         try {
            if (best != null) {
               itemMh = MethodHandles.publicLookup().unreflect(best);
            }

            if (on != null && off != null) {
               scissorOn = MethodHandles.publicLookup().unreflect(on);
               scissorOff = MethodHandles.publicLookup().unreflect(off);
            }
         } catch (Throwable var9) {
         }

         if (itemMh == null) {
            Clarity.LOGGER.warn("[Clarity] No GUI item renderer found; item icons fall back");
         }
      }
   }

   public static void draw(GuiGraphicsExtractor g, LivingEntity entity, ItemStack stack, int x, int y, int seed) {
      try {
         itemMh.invoke((GuiGraphicsExtractor)g, (LivingEntity)entity, (ItemStack)stack, (int)x, (int)y, (int)seed);
      } catch (RuntimeException | Error e) {
         throw e;
      } catch (Throwable t) {
         throw new RuntimeException(t);
      }
   }

   public static ItemStack stack(String id) {
      ItemStack s = STACKS.get(id);
      if (s != null) {
         return s;
      }

      try {
         Item it = (Item)BuiltInRegistries.ITEM.getValue(Identifier.parse(id.indexOf(58) >= 0 ? id : "minecraft:" + id));
         s = it != null && it != Items.AIR ? new ItemStack(it) : ItemStack.EMPTY;
      } catch (Throwable t) {
         s = ItemStack.EMPTY;
      }

      STACKS.put(id, s);
      return s;
   }

   public static boolean has(String id) {
      return available() && !stack(id).isEmpty();
   }

   public static void drawIcons(GuiGraphicsExtractor g, List<NvgCanvas.Icon> icons, float pxPerGui) {
      float gs = pxPerGui;
      if (!icons.isEmpty() && available()) {
         LivingEntity who = Minecraft.getInstance().player;
         g.pose().pushMatrix();
         g.pose().scale(1.0F / gs, 1.0F / gs);

         try {
            for (NvgCanvas.Icon ic : icons) {
               if (!(ic.alpha() < 0.9F)) {
                  float x0 = ic.x();
                  float y0 = ic.y();
                  float x1 = x0 + ic.size();
                  float y1 = y0 + ic.size();
                  if (!(x1 <= ic.cx0()) && !(y1 <= ic.cy0()) && !(x0 >= ic.cx1()) && !(y0 >= ic.cy1())) {
                     boolean cut = x0 < ic.cx0() || y0 < ic.cy0() || x1 > ic.cx1() || y1 > ic.cy1();
                     if (!cut || scissorOn != null) {
                        ItemStack st = ic.stack() instanceof ItemStack is ? is : stack(ic.id());
                        if (!st.isEmpty()) {
                           if (cut) {
                              scissorOn.invoke(
                                 (GuiGraphicsExtractor)g, (int)((int)Math.floor(ic.cx0())), (int)((int)Math.floor(ic.cy0())), (int)((int)Math.ceil(ic.cx1())), (int)((int)Math.ceil(ic.cy1()))
                              );
                           }

                           g.pose().pushMatrix();
                           g.pose().translate(x0, y0);
                           g.pose().scale(ic.size() / 16.0F, ic.size() / 16.0F);
                           draw(g, who, st, 0, 0, 0);
                           g.pose().popMatrix();
                           if (cut) {
                              scissorOff.invoke((GuiGraphicsExtractor)g);
                           }
                        }
                     }
                  }
               }
            }
         } catch (Throwable t) {
            Clarity.LOGGER.warn("[Clarity] Item icons disabled after an error", t);
            itemMh = null;
         } finally {
            g.pose().popMatrix();
         }
      }
   }
}
