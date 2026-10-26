package need.clarity.nvg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import need.clarity.modules.visual.CustomChat;
import need.clarity.util.AccessorCompat;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.world.entity.player.ChatVisiblity;

public final class ChatHud {
   private static final Map<GuiMessage, ChatHud.Wrapped> CACHE = new WeakHashMap<>();
   private static final List<McText.Hit> HITS = new ArrayList<>();
   private static float panelH = 0.0F;
   private static float panelA = 0.0F;
   private static long lastNanos = 0L;
   private static float canvasW = 1.0F;
   private static float canvasH = 1.0F;

   private ChatHud() {
   }

   public static boolean active() {
      return CustomChat.customRenderer();
   }

   private static boolean focused() {
      return Minecraft.getInstance().screen instanceof ChatScreen;
   }

   public static boolean animating() {
      if (!active()) {
         return false;
      }

      if (focused()) {
         return true;
      }

      if (panelA > 0.003F) {
         return true;
      }

      Minecraft mc = Minecraft.getInstance();
      List<GuiMessage> all = messages(mc);
      return all.isEmpty() ? false : mc.gui.getGuiTicks() - all.getFirst().addedTime() < 205;
   }

   private static List<GuiMessage> messages(Minecraft mc) {
      try {
         return AccessorCompat.chatMessages(mc.gui.getChat());
      } catch (Throwable t) {
         return List.of();
      }
   }

   public static int blurStrength() {
      return active() && CustomChat.bool("Blur", false) && !(panelA <= 0.01F) ? Math.round(CustomChat.num("Blur Strength", 2.0F)) : 0;
   }

   public static void draw(long vg, float cw, float ch) {
      HITS.clear();
      canvasW = cw;
      canvasH = ch;
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.options.chatVisibility().get() != ChatVisiblity.HIDDEN) {
            int guiH = mc.getWindow().getGuiScaledHeight();
            if (guiH > 0) {
               float r = ch / guiH;
               double scale = (Double)mc.options.chatScale().get();
               boolean focused = focused();
               float custW = CustomChat.num("Width", 0.0F);
               float width = (custW > 1.0F ? custW : ChatComponent.getWidth((Double)mc.options.chatWidth().get()) * (float)scale) * r;
               float size = CustomChat.num("Text Size", 8.5F) * r * (float)Math.max(0.5, scale);
               float lineH = size * CustomChat.num("Line Height", 1.32F);
               float pad = 4.0F * r;
               float left = (2.0F + CustomChat.num("X Offset", 0.0F)) * r;
               float bottom = (guiH - 40 - CustomChat.num("Y Offset", 0.0F)) * r;
               float maxH = ChatComponent.getHeight(focused ? (Double)mc.options.chatHeightFocused().get() : (Double)mc.options.chatHeightUnfocused().get()) * (float)scale * r;
               String[] faces = McText.faces(CustomChat.str("Font", "Inter"));
               boolean shadow = CustomChat.bool("Text Shadow", false);
               boolean times = CustomChat.bool("Timestamps", false);
               String bgMode = CustomChat.str("Background", "Panel");
               float op = Math.max(0.0F, Math.min(1.0F, CustomChat.num("Background Opacity", 0.45F)));
               float radius = CustomChat.num("Corner Radius", 6.0F) * r;
               int accent = ThemeColors.getAccentRgb();
               int surface = ThemeColors.getSurfaceRgb();
               int muted = ThemeColors.getMutedRgb();
               float textOpacity = (float)((Double)mc.options.chatOpacity().get() * 0.9 + 0.1);
               long now = System.nanoTime();
               float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
               lastNanos = now;
               float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
               int ticks = mc.gui.getGuiTicks();
               float timeW = times ? ClarityDraw.textWidth(vg, faces[1], size * 0.82F, 0.0F, CustomChat.formatTime(0L)) + 6.0F * r : 0.0F;
               float textW = width - timeW;

               record Block(GuiMessage m, List<List<McText.Run>> lines, float alpha, float age, int count, long time) {
               }

               List<Block> blocks = new ArrayList<>();
               float used = 0.0F;
               int skipLines = focused ? Math.max(0, AccessorCompat.chatScroll(mc.gui.getChat())) : 0;

               for (GuiMessage m : messages(mc)) {
                  float age = ticks - m.addedTime() + partial;
                  float alpha;
                  if (focused) {
                     alpha = 1.0F;
                  } else {
                     if (age >= 200.0F) {
                        break;
                     }

                     float t = Math.max(0.0F, Math.min(1.0F, (1.0F - age / 200.0F) * 10.0F));
                     alpha = t * t;
                  }

                  List<List<McText.Run>> lines = wrapped(vg, m, faces, size, textW - pad);
                  if (skipLines > 0) {
                     skipLines -= lines.size();
                     if (skipLines >= 0) {
                        continue;
                     }
                  }

                  float bh = lines.size() * lineH;
                  if (used + bh > maxH + 0.5F && !blocks.isEmpty()) {
                     break;
                  }

                  used += bh + (bgMode.equals("Bubbles") ? 2.0F * r : 0.0F);
                  blocks.add(new Block(m, lines, alpha * textOpacity, age, CustomChat.countOf(m), CustomChat.timeOf(m)));
               }

               float k = 1.0F - (float)Math.exp(-dt * 14.0F);
               float targetH = blocks.isEmpty() ? 0.0F : used + pad * 2.0F;
               float maxA = 0.0F;

               for (Block b : blocks) {
                  maxA = Math.max(maxA, b.alpha);
               }

               panelH = panelH + (targetH - panelH) * (CustomChat.str("Animation", "Slide").equals("None") ? 1.0F : k);
               panelA = panelA + (maxA - panelA) * k;
               if (blocks.isEmpty() && panelA < 0.003F) {
                  panelH = 0.0F;
               } else {
                  if ("Panel".equals(bgMode) && panelH > 1.0F && panelA > 0.003F) {
                     float px = left;
                     float pw = width + pad * 2.0F;
                     float py = bottom - panelH;
                     float ph = panelH;
                     background(vg, px, py, pw, ph, radius, panelA, op, surface, accent);
                  }

                  float y = bottom - pad;

                  for (Block b : blocks) {
                     float bh = b.lines.size() * lineH;
                     float e = CustomChat.entryProgress(b.age);
                     float dx = CustomChat.slideOffset(e) * r;
                     float fa = b.alpha * CustomChat.fadeFactor(e);
                     float top = y - bh;
                     if (!"Bubbles".equals(bgMode)) {
                        if ("Lines".equals(bgMode)) {
                           ClarityDraw.fillRRect(vg, left + dx, top, width + pad * 2.0F, bh, 0.0F, tintA(surface, op * fa));
                        }
                     } else {
                        float lw = 0.0F;

                        for (List<McText.Run> l : b.lines) {
                           lw = Math.max(lw, McText.width(vg, faces, size, l));
                        }

                        float bw = Math.min(width + pad * 2.0F, timeW + lw + pad * 2.0F + (b.count > 1 ? 26.0F * r : 0.0F));
                        background(vg, left + dx, top - 1.0F * r, bw, bh + 2.0F * r, Math.min(radius, (bh + 2.0F * r) / 2.0F), fa, op, surface, accent);
                     }

                     if (CustomChat.bool("Accent Bar", false)) {
                        ClarityDraw.fillRRect(vg, left + dx + 1.0F * r, top + 1.0F * r, 1.5F * r, bh - 2.0F * r, 1.0F * r, tintA(accent, 0.9F * fa));
                     }

                     float tx = left + pad + dx;
                     if (times && b.time > 0L) {
                        ClarityDraw.text(vg, faces[1], size * 0.82F, 0.0F, tintA(muted, 0.85F * fa), 17, tx, top + lineH / 2.0F, CustomChat.formatTime(b.time));
                     }

                     float ly = top + lineH / 2.0F;
                     float endX = tx + timeW;

                     for (List<McText.Run> l : b.lines) {
                        endX = McText.draw(vg, faces, size, l, tx + timeW, ly, fa, shadow, focused ? HITS : null);
                        ly += lineH;
                     }

                     if (b.count > 1) {
                        String lbl = "x" + b.count;
                        float bs = size * 0.78F;
                        float bw = ClarityDraw.textWidth(vg, faces[2], bs, 0.0F, lbl) + 7.0F * r;
                        float bx = endX + 4.0F * r;
                        float cy = ly - lineH;
                        ClarityDraw.fillRRect(vg, bx, cy - bs * 0.68F, bw, bs * 1.36F, bs * 0.68F, tintA(accent, 0.28F * fa));
                        ClarityDraw.text(vg, faces[2], bs, 0.0F, tintA(accent, fa), 18, bx + bw / 2.0F, cy, lbl);
                     }

                     y = top - ("Bubbles".equals(bgMode) ? 2.0F * r : 0.0F);
                  }

                  if (focused) {
                     drawHover(vg, faces, size, r);
                  }
               }
            }
         }
      }
   }

   private static void background(long vg, float x, float y, float w, float h, float rad, float a, float op, int surface, int accent) {
      if (!LiquidGlass.panel(vg, "Custom Chat", x, y, w, h, rad, a)) {
         boolean blurred = CustomChat.bool("Blur", false) && HudElements.fillBlurRRect(vg, x, y, w, h, rad, a);
         ClarityDraw.fillRRect(vg, x, y, w, h, rad, tintA(surface, (blurred ? CustomChat.num("Blur Tint", 0.35F) : op) * a));
         HudFx.panel(vg, "Custom Chat", x, y, w, h, rad, a);
         if (CustomChat.bool("Border", false)) {
            ClarityDraw.strokeRRect(vg, x, y, w, h, rad, 1.0F, tintA(mix(ThemeColors.getBorderRgb(), accent, 0.15F), 0.8F * a));
         }
      }
   }

   private static List<List<McText.Run>> wrapped(long vg, GuiMessage m, String[] faces, float size, float w) {
      String key = faces[0] + "|" + Math.round(size * 10.0F) + "|" + Math.round(w);
      ChatHud.Wrapped c = CACHE.get(m);
      if (c != null && c.key.equals(key)) {
         return c.lines;
      }

      List<McText.Run> runs = McText.runs(m.content(), 16777215);
      List<List<McText.Run>> lines = McText.wrap(vg, faces, size, runs, Math.max(20.0F, w));
      CACHE.put(m, new ChatHud.Wrapped(key, lines));
      return lines;
   }

   private static float[] mouse() {
      Minecraft mc = Minecraft.getInstance();
      double gx = mc.mouseHandler.getScaledXPos(mc.getWindow());
      double gy = mc.mouseHandler.getScaledYPos(mc.getWindow());
      float r = canvasH / Math.max(1, mc.getWindow().getGuiScaledHeight());
      return new float[]{(float)gx * r, (float)gy * r};
   }

   private static Style styleAt(float x, float y) {
      for (McText.Hit h : HITS) {
         if (x >= h.x() && x <= h.x() + h.w() && y >= h.y() && y <= h.y() + h.h()) {
            return h.style();
         }
      }

      return null;
   }

   public static Style hovered() {
      if (active() && !HITS.isEmpty()) {
         float[] m = mouse();
         return styleAt(m[0], m[1]);
      } else {
         return null;
      }
   }

   private static void drawHover(long vg, String[] faces, float size, float r) {
      Style st = hovered();
      if (st != null && st.getHoverEvent() instanceof ShowText show) {
         float[] var20 = mouse();
         List<McText.Run> runs = McText.runs(show.value(), 16777215);
         List<List<McText.Run>> lines = McText.wrap(vg, faces, size, runs, 220.0F * r);
         float lh = size * 1.35F;
         float pad = 5.0F * r;
         float w = 0.0F;

         for (List<McText.Run> l : lines) {
            w = Math.max(w, McText.width(vg, faces, size, l));
         }

         float tw = w + pad * 2.0F;
         float th = lines.size() * lh + pad * 2.0F;
         float x = Math.min(var20[0] + 10.0F * r, canvasW - tw - 4.0F);
         float y = Math.max(4.0F, var20[1] - th - 6.0F * r);
         ClarityDraw.fillRRect(vg, x, y, tw, th, 6.0F * r, tintA(ThemeColors.getSurfaceRgb(), 0.95F));
         ClarityDraw.strokeRRect(vg, x, y, tw, th, 6.0F * r, 1.0F, tintA(ThemeColors.getAccentRgb(), 0.6F));
         float ly = y + pad + lh / 2.0F;

         for (List<McText.Run> l : lines) {
            McText.draw(vg, faces, size, l, x + pad, ly, 1.0F, false, null);
            ly += lh;
         }
      }
   }

   private static int tintA(int rgb, float a) {
      int al = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private record Wrapped(String key, List<List<McText.Run>> lines) {
   }
}
