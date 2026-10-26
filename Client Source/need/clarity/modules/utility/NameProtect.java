package need.clarity.modules.utility;

import java.util.ArrayList;
import java.util.List;
import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;

public class NameProtect extends ModuleBase {
   private static NameProtect instance;
   private static String cachedReal;

   public NameProtect() {
      super("Name Protect", "Utility");
      instance = this;
   }

   public static NameProtect getInstance() {
      return instance;
   }

   public String getSpoofName() {
      String v = this.getSetting("Spoof Name", "Player");
      return v != null && !v.isBlank() ? v : "Player";
   }

   private static String getRealName() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return cachedReal;
      }

      try {
         if (mc.player != null) {
            return cachedReal = mc.player.getGameProfile().name();
         }

         if (mc.getUser() != null) {
            return cachedReal = mc.getUser().getName();
         }
      } catch (Throwable var2) {
      }

      return cachedReal;
   }

   public static boolean isLocalPlayerName(String realName) {
      if (realName == null) {
         return false;
      }

      String real = getRealName();
      return real != null && realName.equals(real);
   }

   public static String maybeReplace(String text) {
      if (text != null && !text.isEmpty()) {
         NameProtect m = instance;
         if (m != null && m.isEnabled()) {
            String real = getRealName();
            return real != null && !real.isEmpty() && text.contains(real) ? text.replace(real, m.getSpoofName()) : text;
         } else {
            return text;
         }
      } else {
         return text;
      }
   }

   public static boolean active() {
      NameProtect m = instance;
      return m != null && m.isEnabled();
   }

   public static Component replaceInTree(Component component) {
      if (component == null) {
         return null;
      }

      NameProtect m = instance;
      if (m != null && m.isEnabled()) {
         String real = getRealName();
         if (real == null) {
            return component;
         }

         String spoof = m.getSpoofName();
         return replaceNode(component, real, spoof);
      } else {
         return component;
      }
   }

   private static Component replaceNode(Component component, String real, String spoof) {
      ComponentContents contents = component.getContents();
      List<Component> siblings = component.getSiblings();
      boolean changed = false;
      ComponentContents newContents = contents;
      if (contents instanceof LiteralContents literal) {
         String text = literal.text();
         if (text.contains(real)) {
            newContents = new LiteralContents(text.replace(real, spoof));
            changed = true;
         }
      } else if (contents instanceof TranslatableContents translatable) {
         Object[] args = translatable.getArgs();
         Object[] newArgs = null;

         for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component argComp) {
               Component replaced = replaceNode(argComp, real, spoof);
               if (replaced != argComp) {
                  if (newArgs == null) {
                     newArgs = (Object[])args.clone();
                  }

                  newArgs[i] = replaced;
                  changed = true;
               }
            } else if (arg instanceof String argStr && argStr.contains(real)) {
               if (newArgs == null) {
                  newArgs = (Object[])args.clone();
               }

               newArgs[i] = argStr.replace(real, spoof);
               changed = true;
            }
         }

         if (newArgs != null) {
            newContents = new TranslatableContents(translatable.getKey(), translatable.getFallback(), newArgs);
         }
      } else {
         String flat = component.getString();
         if (flat != null && flat.contains(real)) {
            newContents = new LiteralContents(flat.replace(real, spoof));
            changed = true;
            siblings = List.of();
         }
      }

      List<Component> newSiblings = siblings;
      if (!siblings.isEmpty()) {
         List<Component> rebuilt = null;

         for (int i = 0; i < siblings.size(); i++) {
            Component child = siblings.get(i);
            Component replaced = replaceNode(child, real, spoof);
            if (replaced != child) {
               if (rebuilt == null) {
                  rebuilt = new ArrayList<>(siblings);
               }

               rebuilt.set(i, replaced);
               changed = true;
            }
         }

         if (rebuilt != null) {
            newSiblings = rebuilt;
         }
      }

      if (!changed) {
         return component;
      }

      MutableComponent result = MutableComponent.create(newContents).withStyle(component.getStyle());

      for (Component sibling : newSiblings) {
         result.append(sibling);
      }

      return result;
   }
}
