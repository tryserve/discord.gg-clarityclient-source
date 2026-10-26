package need.clarity.util;

import java.lang.reflect.Field;
import need.clarity.mixin.InventoryAccessor;
import net.minecraft.world.entity.player.Inventory;

public final class InventoryHelper {
   private static Field selectedField = null;
   private static boolean reflectionFailed = false;

   private InventoryHelper() {
   }

   public static int getSelected(Inventory inv) {
      try {
         return ((InventoryAccessor)inv).getSelected();
      } catch (ClassCastException e) {
         return getSelectedReflection(inv);
      }
   }

   public static void setSelected(Inventory inv, int slot) {
      try {
         ((InventoryAccessor)inv).setSelected(slot);
      } catch (ClassCastException e) {
         setSelectedReflection(inv, slot);
      }
   }

   private static Field resolveField(Inventory inv) {
      if (selectedField != null) {
         return selectedField;
      }

      if (reflectionFailed) {
         return null;
      }

      String[] candidates = new String[]{"selected", "f_35978_", "field_7545"};

      for (String name : candidates) {
         try {
            Field f = Inventory.class.getDeclaredField(name);
            f.setAccessible(true);
            selectedField = f;
            return f;
         } catch (NoSuchFieldException var8) {
         }
      }

      for (Field f : Inventory.class.getDeclaredFields()) {
         if (f.getType() == int.class) {
            f.setAccessible(true);

            try {
               int val = (Integer)f.get(inv);
               if (val >= 0 && val <= 8) {
                  selectedField = f;
                  return f;
               }
            } catch (IllegalAccessException var7) {
            }
         }
      }

      reflectionFailed = true;
      return null;
   }

   private static int getSelectedReflection(Inventory inv) {
      Field f = resolveField(inv);
      if (f == null) {
         return 0;
      }

      try {
         return (Integer)f.get(inv);
      } catch (IllegalAccessException e) {
         return 0;
      }
   }

   private static void setSelectedReflection(Inventory inv, int slot) {
      Field f = resolveField(inv);
      if (f != null) {
         try {
            f.set(inv, slot);
         } catch (IllegalAccessException var4) {
         }
      }
   }
}
