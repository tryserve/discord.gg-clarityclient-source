package need.clarity;

import need.clarity.clickgui.Gui3DRenderer;
import need.clarity.config.ClarityConfig;
import need.clarity.config.ServerProfileLoader;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.hotbar.HotbarRenderer;
import need.clarity.menu.MultiplayerAccounts;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.client.DiscordRPC;
import need.clarity.modules.client.Panic;
import need.clarity.modules.client.SelfDestruct;
import need.clarity.modules.combat.AimAssist;
import need.clarity.modules.combat.AntiBot;
import need.clarity.modules.combat.AutoAnchor;
import need.clarity.modules.combat.AutoClicker;
import need.clarity.modules.combat.AutoCrystal;
import need.clarity.modules.combat.AutoHitCrystal;
import need.clarity.modules.combat.AutoJumpReset;
import need.clarity.modules.combat.AutoLunge;
import need.clarity.modules.combat.AutoMace;
import need.clarity.modules.combat.AutoTrap;
import need.clarity.modules.combat.Backtrack;
import need.clarity.modules.combat.CartAssist;
import need.clarity.modules.combat.Criticals;
import need.clarity.modules.combat.CrystalAura;
import need.clarity.modules.combat.HitSwap;
import need.clarity.modules.combat.KillAura;
import need.clarity.modules.combat.Piercing;
import need.clarity.modules.combat.Reach;
import need.clarity.modules.combat.ShieldBreaker;
import need.clarity.modules.combat.SprintReset;
import need.clarity.modules.combat.TargetStrafe;
import need.clarity.modules.combat.Triggerbot;
import need.clarity.modules.movement.AntiHunger;
import need.clarity.modules.movement.AntiLevitation;
import need.clarity.modules.movement.AntiPowderSnow;
import need.clarity.modules.movement.AntiPush;
import need.clarity.modules.movement.ElytraJump;
import need.clarity.modules.movement.Fly;
import need.clarity.modules.movement.InventoryWalk;
import need.clarity.modules.movement.MoveFix;
import need.clarity.modules.movement.NoFall;
import need.clarity.modules.movement.NoJumpDelay;
import need.clarity.modules.movement.NoSlow;
import need.clarity.modules.movement.Parkour;
import need.clarity.modules.movement.ReverseStep;
import need.clarity.modules.movement.SnapTap;
import need.clarity.modules.movement.Speed;
import need.clarity.modules.movement.SpinBot;
import need.clarity.modules.movement.Sprint;
import need.clarity.modules.movement.Step;
import need.clarity.modules.movement.WindCharge;
import need.clarity.modules.utility.AntiCob;
import need.clarity.modules.utility.AntiGuiClose;
import need.clarity.modules.utility.AutoArmour;
import need.clarity.modules.utility.AutoTool;
import need.clarity.modules.utility.AutoWeb;
import need.clarity.modules.utility.CartRefill;
import need.clarity.modules.utility.ChestStealer;
import need.clarity.modules.utility.ClientSpoofer;
import need.clarity.modules.utility.CrystalOptimizer;
import need.clarity.modules.utility.Disabler;
import need.clarity.modules.utility.FakePlayer;
import need.clarity.modules.utility.FastPlace;
import need.clarity.modules.utility.GhostHand;
import need.clarity.modules.utility.HoverTotem;
import need.clarity.modules.utility.InventoryCleaner;
import need.clarity.modules.utility.KeyPearl;
import need.clarity.modules.utility.MLG;
import need.clarity.modules.utility.MiddleClickFriend;
import need.clarity.modules.utility.NameProtect;
import need.clarity.modules.utility.NoBreakDelay;
import need.clarity.modules.utility.Overtick;
import need.clarity.modules.utility.RightClicker;
import need.clarity.modules.utility.SilentTotem;
import need.clarity.modules.utility.Targets;
import need.clarity.modules.utility.XCarry;
import need.clarity.modules.visual.Ambience;
import need.clarity.modules.visual.Animations;
import need.clarity.modules.visual.ArrayList;
import need.clarity.modules.visual.Aspect;
import need.clarity.modules.visual.Aura;
import need.clarity.modules.visual.AutoF5;
import need.clarity.modules.visual.BpsCounter;
import need.clarity.modules.visual.CameraTweaks;
import need.clarity.modules.visual.Capes;
import need.clarity.modules.visual.Chams;
import need.clarity.modules.visual.ClarityIcon;
import need.clarity.modules.visual.CrystalView;
import need.clarity.modules.visual.CustomChat;
import need.clarity.modules.visual.CustomCrosshair;
import need.clarity.modules.visual.CustomHotbar;
import need.clarity.modules.visual.CustomScoreboard;
import need.clarity.modules.visual.Drugs;
import need.clarity.modules.visual.EnchantGlint;
import need.clarity.modules.visual.Freecam;
import need.clarity.modules.visual.Freelook;
import need.clarity.modules.visual.Fullbright;
import need.clarity.modules.visual.HUD;
import need.clarity.modules.visual.HandShaders;
import need.clarity.modules.visual.HandView;
import need.clarity.modules.visual.Hats;
import need.clarity.modules.visual.HealthTags;
import need.clarity.modules.visual.HitParticles;
import need.clarity.modules.visual.HitSounds;
import need.clarity.modules.visual.Inspect;
import need.clarity.modules.visual.ItemESP;
import need.clarity.modules.visual.ItemTags;
import need.clarity.modules.visual.JumpEffect;
import need.clarity.modules.visual.KeybindList;
import need.clarity.modules.visual.KillEffect;
import need.clarity.modules.visual.KillSounds;
import need.clarity.modules.visual.MainMenu;
import need.clarity.modules.visual.MotionBlur;
import need.clarity.modules.visual.MoveAnimations;
import need.clarity.modules.visual.MusicBeats;
import need.clarity.modules.visual.MusicPlayer;
import need.clarity.modules.visual.Nametags;
import need.clarity.modules.visual.NoBob;
import need.clarity.modules.visual.NoFOV;
import need.clarity.modules.visual.NoHurtCam;
import need.clarity.modules.visual.NoRender;
import need.clarity.modules.visual.NoSwing;
import need.clarity.modules.visual.Notifications;
import need.clarity.modules.visual.Particles;
import need.clarity.modules.visual.Pets;
import need.clarity.modules.visual.PlayerESP;
import need.clarity.modules.visual.PlayerModel;
import need.clarity.modules.visual.PlayerTrails;
import need.clarity.modules.visual.PotionHud;
import need.clarity.modules.visual.RenderAnimations;
import need.clarity.modules.visual.Search;
import need.clarity.modules.visual.ServerRotations;
import need.clarity.modules.visual.ShaderESP;
import need.clarity.modules.visual.SkeletonESP;
import need.clarity.modules.visual.SkinChanger;
import need.clarity.modules.visual.SkyShaders;
import need.clarity.modules.visual.SmoothCamera;
import need.clarity.modules.visual.StorageESP;
import need.clarity.modules.visual.TNTTimer;
import need.clarity.modules.visual.TargetESP;
import need.clarity.modules.visual.TargetHud;
import need.clarity.modules.visual.ThemeTexturesModule;
import need.clarity.modules.visual.Tracers;
import need.clarity.modules.visual.Trajectories;
import need.clarity.modules.visual.TrueSight;
import need.clarity.modules.visual.ViewModel;
import need.clarity.modules.visual.Wings;
import need.clarity.modules.visual.Zoom;
import need.clarity.nvg.ArrayListRenderer;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.server.ClarityServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Clarity implements ModInitializer {
   public static final String MOD_ID = "clarity";
   public static final Logger LOGGER = LoggerFactory.getLogger("clarity");
   private static boolean listenersRegistered = false;
   private static Thread shutdownHook;

   public static void removeShutdownHook() {
      Thread h = shutdownHook;
      if (h != null) {
         try {
            Runtime.getRuntime().removeShutdownHook(h);
         } catch (Throwable var2) {
         }

         shutdownHook = null;
      }
   }

   public void onInitialize() {
      ClarityConfig.load();
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
         ClarityServer.init();
      }

      new Overtick();
      new Triggerbot();
      new AimAssist();
      new KillAura();
      new ShieldBreaker();
      new SprintReset();
      new Criticals();
      new Piercing();
      new Reach();
      new Backtrack();
      new AntiBot();
      new HitSwap();
      new AutoCrystal();
      new CrystalAura();
      new AutoHitCrystal();
      new AutoAnchor();
      new AutoLunge();
      new AutoTrap();
      new TargetStrafe();
      new AutoMace();
      new CartAssist();
      new AutoJumpReset();
      new AutoClicker();
      new Panic();
      new SelfDestruct();
      new DiscordRPC();
      new ServerRotations();
      new Fullbright();
      new Animations();
      new Search();
      new Tracers();
      new Freecam();
      new HealthTags();
      new Nametags();
      new Capes();
      new NoRender();
      new CameraTweaks();
      new ViewModel();
      new HandShaders();
      new PlayerESP();
      new SkeletonESP();
      new MainMenu();
      new StorageESP();
      new ItemESP();
      new Chams();
      new Trajectories();
      new HitParticles();
      new HitSounds();
      new KillSounds();
      new PlayerTrails();
      new EnchantGlint();
      new ArrayList();
      new ThemeTexturesModule();
      new Ambience();
      new SkyShaders();
      new ShaderESP();
      new SmoothCamera();
      new MusicBeats();
      new RenderAnimations();
      new MoveFix();
      new WindCharge();
      new AutoWeb();
      new CartRefill();
      new AntiCob();
      new HUD();
      new TargetHud();
      new Notifications();
      new KeybindList();
      new PotionHud();
      new MusicPlayer();
      new TNTTimer();
      new JumpEffect();
      new NoBob();
      new NoFOV();
      new Wings();
      new Hats();
      new Aura();
      new TargetESP();
      new Particles();
      new KillEffect();
      new AutoF5();
      new ItemTags();
      new TrueSight();
      new NoSwing();
      new Zoom();
      new NoHurtCam();
      new Aspect();
      new SkinChanger();
      new PlayerModel();
      new Pets();
      new MotionBlur();
      new Drugs();
      new CustomCrosshair();
      new CrystalView();
      new CustomHotbar();
      new MoveAnimations();
      new HandView();
      new Inspect();
      new BpsCounter();
      new ClarityIcon();
      new Freelook();
      new CustomChat();
      new CustomScoreboard();
      new Sprint();
      new SpinBot();
      new Fly();
      new ElytraJump();
      new Speed();
      new InventoryWalk();
      new AntiPowderSnow();
      new AntiHunger();
      new AntiLevitation();
      new NoFall();
      new NoSlow();
      new AntiPush();
      new Step();
      new ReverseStep();
      new Parkour();
      new Targets();
      new RightClicker();
      new MLG();
      new AutoTool();
      new NoBreakDelay();
      new FastPlace();
      new XCarry();
      new GhostHand();
      new NameProtect();
      new AutoArmour();
      new ChestStealer();
      new FakePlayer();
      new InventoryCleaner();
      new HoverTotem();
      new SilentTotem();
      new SnapTap();
      new NoJumpDelay();
      new ClientSpoofer();
      new CrystalOptimizer();
      new Disabler();
      new KeyPearl();
      new MiddleClickFriend();
      new AntiGuiClose();
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
         ClarityConfig.pruneStaleBinds();
         ModuleRegistry.syncFromConfig();
      }

      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
         ModuleRegistry.registerTickEvent();
         HotbarRenderer.register();
         ServerProfileLoader.register();
         MultiplayerAccounts.register();
         Friends.init();
      }

      if (!listenersRegistered) {
         listenersRegistered = true;
         if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> {
               if (!Unload.isUnloaded()) {
                  ClarityConfig.flushSync();
                  Friends.shutdown();
                  ClarityServer.stop();
                  ArrayListRenderer.dispose();
                  HotbarRenderer.dispose();
                  Gui3DRenderer.dispose();
                  SkyShaderRenderer.dispose();
               }
            });
         }

         shutdownHook = new Thread(() -> {
            if (!Unload.isUnloaded()) {
               ClarityConfig.flushSync();

               try {
                  FriendManager.flushSync();
               } catch (Throwable var1) {
               }

               ClarityServer.stop();
               ArrayListRenderer.dispose();
            }
         }, "clarity-shutdown");
         Runtime.getRuntime().addShutdownHook(shutdownHook);
      }
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("clarity", path);
   }
}
