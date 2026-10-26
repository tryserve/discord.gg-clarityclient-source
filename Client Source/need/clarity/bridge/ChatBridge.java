package need.clarity.bridge;

import java.util.List;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.friends.online.DirectMessages;
import need.clarity.friends.online.GlobalChat;
import need.clarity.modules.utility.NameProtect;
import need.clarity.modules.visual.CustomChat;
import need.clarity.modules.visual.CustomScoreboard;
import need.clarity.nvg.ChatHud;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.Objective;

public final class ChatBridge {
   private ChatBridge() {
   }

   public static Component onAdd(Component content, List<GuiMessage> all, List<Line> lines) {
      try {
         return CustomChat.onAdd(content, all, lines);
      } catch (Throwable t) {
         return content;
      }
   }

   public static void afterAdd(List<GuiMessage> all) {
      try {
         CustomChat.afterAdd(all);
      } catch (Throwable var2) {
      }
   }

   public static int historyLimit(int vanilla) {
      try {
         return CustomChat.historyLimit(vanilla);
      } catch (Throwable t) {
         return vanilla;
      }
   }

   public static ChatGraphicsAccess decorate(ChatGraphicsAccess access) {
      try {
         return CustomChat.decorate(access);
      } catch (Throwable t) {
         return access;
      }
   }

   public static boolean hideVanillaChat() {
      try {
         return ChatHud.active();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void lineStart(Object line, float alpha) {
      try {
         CustomChat.currentLine = (Line)line;
         CustomChat.currentAlpha = alpha;
      } catch (Throwable var3) {
      }
   }

   public static void lineEnd() {
      try {
         CustomChat.currentLine = null;
      } catch (Throwable var1) {
      }
   }

   public static Style clickedStyle(MouseButtonEvent e) {
      try {
         return e.button() == 0 && ChatHud.active() ? ChatHud.hovered() : null;
      } catch (Throwable t) {
         return null;
      }
   }

   public static boolean interceptInput(String message, boolean addToRecent) {
      try {
         return DirectMessages.intercept(message, addToRecent) || GlobalChat.intercept(message, addToRecent);
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean customScoreboard(Object graphics, Object objective) {
      try {
         return CustomScoreboard.render(graphics, (Objective)objective);
      } catch (Throwable t) {
         return false;
      }
   }

   public static Component friendTabList(Component current, PlayerInfo self) {
      try {
         if (FriendManager.enabled() && FriendManager.bool("Tab List")) {
            String name = self.getProfile().name();
            Friend friend = FriendManager.get(self.getProfile().id().toString(), name);
            if (friend == null) {
               return current;
            }

            Component base = (Component)(current == null ? Component.literal(name) : current);
            return Friends.decorate(base, friend, name);
         } else {
            return current;
         }
      } catch (Throwable t) {
         return current;
      }
   }

   public static String nameProtectText(String text) {
      try {
         return NameProtect.maybeReplace(text);
      } catch (Throwable t) {
         return text;
      }
   }
}
