package need.clarity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import need.clarity.alts.SessionSwapper;
import need.clarity.audio.BeatAnalyzer;
import need.clarity.clickgui.Gui3DRenderer;
import need.clarity.config.ClarityConfig;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.media.MediaBridge;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.AntiBot;
import need.clarity.modules.utility.crystalhelpers.OptimizerPackets;
import need.clarity.modules.visual.Search;
import need.clarity.nvg.ClarityImages;
import need.clarity.nvg.HudEditorPanel;
import need.clarity.nvg.HudLayout;
import need.clarity.render.ImageParticles;
import need.clarity.render.PlayerESPMaskCapture;
import need.clarity.render.SwingEngine;
import need.clarity.render.worldfx.KillFx;
import need.clarity.render.worldfx.ScanFx;
import need.clarity.server.ClarityServer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

public final class Unload {
   private static volatile boolean unloaded = false;
   private static final AtomicBoolean started = new AtomicBoolean(false);
   private static final String[] RESOURCE_OWNERS = new String[]{
      "need.clarity.nvg.ArrayListRenderer",
      "need.clarity.nvg.HudFx",
      "need.clarity.nvg.LiquidGlass",
      "need.clarity.nvg.BackdropBlur",
      "need.clarity.nvg.ClarityFonts",
      "need.clarity.nvg.HudElements",
      "need.clarity.nvg.MiscHud",
      "need.clarity.nvg.NametagRenderer",
      "need.clarity.nvg.ChatHud",
      "need.clarity.nvg.ScoreboardHud",
      "need.clarity.nvg.Emoji",
      "need.clarity.hotbar.HotbarRenderer",
      "need.clarity.render.RawFramebufferUtil",
      "need.clarity.render.PlayerESPMaskCapture",
      "need.clarity.render.ChamsRenderer",
      "need.clarity.render.ShaderCapes",
      "need.clarity.render.MotionBlurRenderer",
      "need.clarity.render.DrugsRenderer",
      "need.clarity.render.CrosshairRenderer",
      "need.clarity.render.ShaderHelper",
      "need.clarity.render.esp.ShaderEspRenderer",
      "need.clarity.render.esp.SkeletonGlowRenderer",
      "need.clarity.render.sky.SkyShaderRenderer",
      "need.clarity.render.handshader.HandShaderRenderer",
      "need.clarity.clickgui.Gui3DRenderer",
      "need.clarity.clickgui.ClickGuiRenderer",
      "need.clarity.menu.MenuRenderer"
   };
   private static final String[] CLEANUP_NAMES = new String[]{"dispose", "freeTextures", "free", "destroy", "cleanup", "release", "shutdown", "close"};

   private Unload() {
   }

   public static boolean isUnloaded() {
      return unloaded;
   }

   public static void request(ModuleBase self) {
      if (started.compareAndSet(false, true)) {
         unloaded = true;
         step(() -> {
            if (self != null) {
               self.setEnabled(false);
            }
         });
         step(ClarityConfig::flushSync);
         step(FriendManager::flushSync);
         step(ClarityConfig::freeze);
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            stageOne();
            stageTwo();
         } else {
            if (mc.isSameThread()) {
               stageOne();
            } else {
               mc.execute(Unload::stageOne);
            }
         }
      }
   }

   private static void stageOne() {
      for (int pass = 0; pass < 2; pass++) {
         for (ModuleBase m : ModuleRegistry.all()) {
            if (m.isEnabled()) {
               step(() -> m.setEnabled(false));
            }
         }
      }

      step(PerFrameEvents::clear);
      step(PerPacketEvents::clear);
      step(PerPacketSendEvents::clear);
      step(PerAttackEvents::clear);
      step(PerMovementEvents::clear);
      step(() -> PerMovementEvents.lastEvent = null);
      step(WorldRenderEvent::clear);
      step(AimAssist::releaseSharedTarget);
      step(AimAssist::clearCurrentTarget);
      step(AimAssist::clearSilentRotation);
      step(AntiBot::clearManualFlags);
      step(() -> LocalPlayerRotationState.silentActive = false);
      step(() -> LocalPlayerRotationState.tickServerYaw = LocalPlayerRotationState.realYaw);
      step(KillFx::clear);
      step(ScanFx::clear);
      step(ImageParticles::clear);
      step(SwingEngine::reset);
      step(HudEditorPanel::reset);
      step(ClarityImages::clear);
      step(PlayerESPMaskCapture::clearMask);
      step(() -> {
         if (HudLayout.isEditing()) {
            HudLayout.exitNoGrab();
         }
      });
      step(Gui3DRenderer::close);
      step(Unload::closeClarityScreens);
      step(ClarityServer::shutdown);
      step(Friends::shutdown);
      step(FriendManager::shutdownWriter);
      step(Unload::unregisterNetworking);
      step(MediaBridge::stop);
      step(BeatAnalyzer::stop);
      step(Search::shutdownPool);
      step(SessionSwapper::restoreOriginal);
      step(Clarity::removeShutdownHook);
      Thread t = new Thread(() -> {
         try {
            Thread.sleep(400L);
         } catch (InterruptedException var1) {
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            mc.execute(Unload::stageTwo);
         } else {
            stageTwo();
         }
      }, "clarity-unload");
      t.setDaemon(true);
      t.start();
   }

   private static void stageTwo() {
      for (String cn : RESOURCE_OWNERS) {
         step(() -> runCleanups(cn));
      }

      step(Unload::releaseClarityTextures);
      step(Clarity::removeShutdownHook);
      step(() -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            Method r = Minecraft.class.getDeclaredMethod("resizeDisplay");
            r.setAccessible(true);
            r.invoke(mc);
         }
      });
   }

   private static void runCleanups(String className) throws Exception {
      Class<?> c;
      try {
         c = Class.forName(className, false, Unload.class.getClassLoader());
      } catch (ClassNotFoundException e) {
         return;
      }

      for (String name : CLEANUP_NAMES) {
         Method m;
         try {
            m = c.getDeclaredMethod(name);
         } catch (NoSuchMethodException e) {
            continue;
         }

         if (Modifier.isStatic(m.getModifiers())) {
            m.setAccessible(true);
            Method target = m;
            step(() -> target.invoke(null));
         }
      }
   }

   private static boolean isOurs(Object o) {
      return o != null && o.getClass().getName().startsWith("need.clarity.");
   }

   private static void closeClarityScreens() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         Screen s = mc.screen;
         if (isOurs(s)) {
            mc.setScreen(mc.level == null ? new TitleScreen() : null);
         }
      }
   }

   private static void releaseClarityTextures() throws Exception {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         Object tm = mc.getTextureManager();
         List<Object> ids = new ArrayList<>();

         for (Class<?> k = tm.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
               if (!Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
                  f.setAccessible(true);
                  Map<Object, Object> map = (Map<Object, Object>)f.get(tm);
                  if (map != null) {
                     for (Object key : new ArrayList<>(map.keySet())) {
                        if (key instanceof Identifier id && "clarity".equals(id.getNamespace())) {
                           ids.add(id);
                        }
                     }
                  }
               }
            }
         }

         for (Object id : ids) {
            step(() -> mc.getTextureManager().release((Identifier)id));
         }
      }
   }

   private static void unregisterNetworking() {
      try {
         ClientPlayNetworking.unregisterGlobalReceiver(OptimizerPackets.OptOut.TYPE.id());
      } catch (Throwable var2) {
      }

      try {
         ClientPlayNetworking.unregisterGlobalReceiver(OptimizerPackets.Challenge.TYPE.id());
      } catch (Throwable var1) {
      }
   }

   private static void step(Unload.Step s) {
      try {
         s.run();
      } catch (Throwable var2) {
      }
   }

   @FunctionalInterface
   private interface Step {
      void run() throws Throwable;
   }
}
