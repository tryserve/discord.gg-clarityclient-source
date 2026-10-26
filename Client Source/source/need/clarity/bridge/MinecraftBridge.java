package need.clarity.bridge;

import need.clarity.EventStartAttack;
import need.clarity.PerAttackEvents;
import need.clarity.friends.Friends;
import need.clarity.menu.MainMenuScreen;
import need.clarity.menu.MenuRenderer;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.Piercing;
import need.clarity.modules.utility.ChestStealer;
import need.clarity.modules.utility.GhostHand;
import need.clarity.modules.visual.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class MinecraftBridge {
   private MinecraftBridge() {
   }

   public static boolean onStartAttack(Minecraft mc) {
      try {
         if (mc.player == null) {
            return false;
         }

         Entity target = null;
         if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
            target = ((EntityHitResult)mc.hitResult).getEntity();
         }

         if (target != null && Friends.blockHits(target)) {
            return true;
         }

         EventStartAttack event = new EventStartAttack(mc.player, target);
         PerAttackEvents.fire(event);
         return event.isCancelled();
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onHandleKeybinds(Minecraft mc) {
      try {
         if (mc.player == null || mc.level == null) {
            return;
         }

         GhostHand module = GhostHand.getInstance();
         if (module != null && module.isEnabled()) {
            module.onPick(mc);
         }
      } catch (Throwable var3) {
      }

      try {
         if (mc.player == null || mc.level == null) {
            return;
         }

         Piercing piercing = (Piercing)ModuleRegistry.get("Piercing");
         if (piercing != null && piercing.isEnabled()) {
            piercing.onHandleInput(mc);
         }
      } catch (Throwable var2) {
      }
   }

   public static Screen redirectScreen(Screen screen) {
      try {
         if (screen instanceof TitleScreen && MainMenu.active() && !MenuRenderer.hasFailed()) {
            return new MainMenuScreen();
         }
      } catch (Throwable var2) {
      }

      return screen;
   }

   public static void onSetScreenReturn(Minecraft mc, Screen incomingScreen) {
      try {
         if (!(incomingScreen instanceof AbstractContainerScreen<?> cs)) {
            return;
         }

         if (!(cs.getMenu() instanceof ChestMenu)) {
            return;
         }

         if (!ChestStealer.tryCaptureSilent()) {
            return;
         }

         mc.screen = null;
         mc.mouseHandler.grabMouse();
      } catch (Throwable var3) {
      }
   }
}
