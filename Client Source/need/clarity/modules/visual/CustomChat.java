package need.clarity.modules.visual;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import need.clarity.modules.ModuleBase;
import need.clarity.util.ThemeColors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.client.multiplayer.chat.GuiMessageTag.Icon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

public class CustomChat extends ModuleBase {
   public static CustomChat INSTANCE;
   private static final Map<GuiMessage, String> BASE = new WeakHashMap<>();
   private static final Map<GuiMessage, Integer> COUNT = new WeakHashMap<>();
   private static String pendingBase;
   private static int pendingCount = 1;
   private static Component pendingContent;
   public static Line currentLine;
   public static float currentAlpha = 1.0F;
   private static final Map<GuiMessage, Long> TIME = new WeakHashMap<>();

   public CustomChat() {
      super("Custom Chat", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static boolean customRenderer() {
      return active() && !"Vanilla".equals(INSTANCE.getSetting("Renderer", "Clarity"));
   }

   public static boolean bool(String k, boolean d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static float num(String k, float d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static String str(String k, String d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static int countOf(GuiMessage m) {
      return COUNT.getOrDefault(m, 1);
   }

   public static long timeOf(GuiMessage m) {
      return TIME.getOrDefault(m, 0L);
   }

   public static String formatTime(long at) {
      String pattern = switch (str("Time Format", "HH:mm")) {
         case "HH:mm:ss" -> "HH:mm:ss";
         case "h:mm a" -> "h:mm a";
         default -> "HH:mm";
      };
      return new SimpleDateFormat(pattern).format(new Date(at == 0L ? 1700000000000L : at));
   }

   public static float entryProgress(float ageTicks) {
      if ("None".equals(str("Animation", "Slide"))) {
         return 1.0F;
      }

      float dur = Math.max(1.0F, 8.0F / Math.max(0.2F, num("Animation Speed", 1.0F)));
      return ease(ageTicks / dur);
   }

   public static float slideOffset(float e) {
      return str("Animation", "Slide").startsWith("Slide") ? -(1.0F - e) * 40.0F : 0.0F;
   }

   public static float fadeFactor(float e) {
      String a = str("Animation", "Slide");
      return !a.contains("Fade") && !"Slide".equals(a) ? 1.0F : e;
   }

   public static Component onAdd(Component content, List<GuiMessage> all, List<Line> lines) {
      pendingContent = null;
      if (active() && content != null) {
         String base = content.getString();
         int count = 1;
         if (INSTANCE.getSetting("Anti Spam", true) && !base.isBlank()) {
            int range = Math.max(1, Math.min(50, INSTANCE.getSetting("Spam Range", 5)));

            for (int i = 0; i < Math.min(range, all.size()); i++) {
               GuiMessage old = all.get(i);
               String ob = BASE.getOrDefault(old, old.content().getString());
               if (ob.equals(base)) {
                  count = COUNT.getOrDefault(old, 1) + 1;
                  all.remove(i);
                  lines.removeIf(l -> l.parent() == old);
                  break;
               }
            }
         }

         pendingBase = base;
         pendingCount = count;
         if (customRenderer()) {
            pendingContent = content;
            return content;
         }

         MutableComponent out = Component.empty();
         if (INSTANCE.getSetting("Timestamps", false)) {
            String pattern = switch ((String)INSTANCE.getSetting("Time Format", "HH:mm")) {
               case "HH:mm:ss" -> "HH:mm:ss";
               case "h:mm a" -> "h:mm a";
               default -> "HH:mm";
            };
            String t = new SimpleDateFormat(pattern).format(new Date());
            out.append(Component.literal("[" + t + "] ").withStyle(ChatFormatting.DARK_GRAY));
         }

         out.append(content);
         if (count > 1) {
            out.append(Component.literal(" [x" + count + "]").withStyle(s -> s.withColor(0xFF000000 | ThemeColors.getAccentRgb())));
         }

         pendingBase = base;
         pendingCount = count;
         pendingContent = (Component)(out.getSiblings().size() == 1 ? content : out);
         return pendingContent;
      } else {
         return content;
      }
   }

   public static void afterAdd(List<GuiMessage> all) {
      if (pendingContent != null && !all.isEmpty()) {
         GuiMessage first = all.getFirst();
         if (first.content() == pendingContent || !BASE.containsKey(first) && first.addedTime() == Minecraft.getInstance().gui.getGuiTicks()) {
            BASE.put(first, pendingBase);
            COUNT.put(first, pendingCount);
            TIME.put(first, System.currentTimeMillis());
         }

         pendingContent = null;
      }
   }

   public static int historyLimit(int vanilla) {
      return active() && INSTANCE.getSetting("Longer History", true) ? 1000 : vanilla;
   }

   public static ChatGraphicsAccess decorate(ChatGraphicsAccess access) {
      return active() && !customRenderer() && access != null && !(access instanceof CustomChat.Decorated) ? new CustomChat.Decorated(access) : access;
   }

   private static float ease(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   private static float entry() {
      Line l = currentLine;
      if (l == null) {
         return 1.0F;
      }

      String anim = INSTANCE.getSetting("Animation", "Slide");
      if ("None".equals(anim)) {
         return 1.0F;
      }

      Minecraft mc = Minecraft.getInstance();
      float age = mc.gui.getGuiTicks() - l.addedTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      float dur = Math.max(1.0F, 8.0F / Math.max(0.2F, INSTANCE.getSetting("Animation Speed", 1.0F)));
      return ease(age / dur);
   }

   private static final class Decorated implements ChatGraphicsAccess {
      private final ChatGraphicsAccess d;

      Decorated(ChatGraphicsAccess d) {
         this.d = d;
      }

      private float slide(float e) {
         String anim = CustomChat.INSTANCE.getSetting("Animation", "Slide");
         return !anim.startsWith("Slide") ? 0.0F : -(1.0F - e) * 40.0F;
      }

      private float fade(float e) {
         String anim = CustomChat.INSTANCE.getSetting("Animation", "Slide");
         return !anim.contains("Fade") && !"Slide".equals(anim) ? 1.0F : e;
      }

      public void updatePose(Consumer<Matrix3x2f> c) {
         this.d.updatePose(c);
      }

      public void fill(int x0, int y0, int x1, int y1, int color) {
         if (x0 != -4) {
            this.d.fill(x0, y0, x1, y1, color);
         } else {
            float e = CustomChat.entry();
            int dx = Math.round(this.slide(e));
            String bg = CustomChat.INSTANCE.getSetting("Background", "Panel");
            int a = color >>> 24;
            if ("None".equals(bg)) {
               a = 0;
            } else {
               float op = Math.max(0.0F, Math.min(1.0F, CustomChat.INSTANCE.getSetting("Background Opacity", 0.45F)));
               a = Math.round(255.0F * op * Math.max(0.0F, Math.min(1.0F, CustomChat.currentAlpha)));
               color = ThemeColors.getSurfaceRgb() & 16777215;
            }

            a = Math.round(a * this.fade(e));
            if (a > 0) {
               this.d.fill(x0 + dx, y0, x1 + dx, y1, a << 24 | color & 16777215);
            }

            if (CustomChat.INSTANCE.getSetting("Accent Bar", false)) {
               int aa = Math.round(220.0F * Math.max(0.0F, Math.min(1.0F, CustomChat.currentAlpha)) * this.fade(e));
               if (aa > 0) {
                  this.d.fill(x0 + dx, y0, x0 + dx + 1, y1, aa << 24 | ThemeColors.getAccentRgb() & 16777215);
               }
            }
         }
      }

      public boolean handleMessage(int y, float alpha, FormattedCharSequence text) {
         float e = CustomChat.entry();
         float dx = this.slide(e);
         if (dx == 0.0F) {
            return this.d.handleMessage(y, alpha * this.fade(e), text);
         }

         this.d.updatePose(m -> m.translate(dx, 0.0F));

         try {
            return this.d.handleMessage(y, alpha * this.fade(e), text);
         } finally {
            this.d.updatePose(m -> m.translate(-dx, 0.0F));
         }
      }

      public void handleTag(int a, int b, int c, int e, float f, GuiMessageTag tag) {
         this.d.handleTag(a, b, c, e, f, tag);
      }

      public void handleTagIcon(int a, int b, boolean c, GuiMessageTag tag, Icon icon) {
         this.d.handleTagIcon(a, b, c, tag, icon);
      }
   }
}
