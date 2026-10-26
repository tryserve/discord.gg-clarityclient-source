package need.clarity.clickgui;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import need.clarity.config.ConfigFiles;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.FriendSettings;
import need.clarity.friends.Friends;
import need.clarity.friends.online.CloudConfigs;
import need.clarity.friends.online.DirectMessages;
import need.clarity.friends.online.GlobalChat;
import need.clarity.friends.online.PresenceClient;
import need.clarity.modules.visual.playermodel.ImportedModelRenderer;
import need.clarity.nvg.Emoji;
import need.clarity.nvg.HudEditorPanel;
import need.clarity.nvg.HudLayout;
import need.clarity.util.Chroma;
import need.clarity.util.CustomSounds;
import need.clarity.util.UiSounds;
import net.minecraft.client.Minecraft;

public final class ClickGui {
   public static final String GUI_MODULE = "Click GUI";
   static final String[] SPECIAL = new String[]{"Friends", "Themes", "Profiles", "Binds", "Servers", "GUI"};
   static final LinkedHashMap<String, List<GuiData.Setting>> GUI_SECTIONS = new LinkedHashMap<>();
   static final List<GuiData.Setting> GUI_SETTINGS = new ArrayList<>();
   static final Map<String, Object> GUI_DEFAULTS = new HashMap<>();
   static final String[] BG_ON = new String[]{"Aurora", "Nebula", "Waves", "Solid"};
   static final String[] FONTS = new String[]{"Inter", "Roboto", "Poppins", "Montserrat", "Outfit", "JetBrains Mono", "Pixelify Sans"};
   private static String lastPage;
   final GuiData data;
   final GuiStore store;
   final Anim an = new Anim();
   Theme th = Theme.from(Map.of());
   float sw;
   float sh;
   float mx = -9999.0F;
   float my = -9999.0F;
   float wx;
   float wy;
   float ww;
   float wh;
   float winScale = 1.0F;
   float winCx;
   float winCy;
   float wmx;
   float wmy;
   long lastNanos = 0L;
   float time = 0.0F;
   float open = 0.0F;
   boolean closing = false;
   boolean closed = false;
   String page;
   float pageSwitchAt = 0.0F;
   final TextField search = new TextField(48);
   TextField focused;
   Runnable focusCommit;
   String listenModule;
   String listenSetting;
   final Set<String> expanded = new HashSet<>();
   final Map<String, Float> measured = new HashMap<>();
   final Map<String, float[]> scroll = new HashMap<>();
   Consumer<Float> drag;
   Consumer<float[]> drag2;
   ClickGui.Pop pop;
   final List<ClickGui.Hit> hits = new ArrayList<>();
   final List<ClickGui.Toast> toasts = new ArrayList<>();
   final List<ClickGui.Ripple> ripples = new ArrayList<>();
   String hoverModule;
   float hoverSince;
   String confirmId;
   float confirmAt;
   final TextField nameField = new TextField(32);
   final TextField addField = new TextField(64);
   String addFieldOwner;
   float clipX = -1000000.0F;
   float clipY = -1000000.0F;
   float clipW = 2000000.0F;
   float clipH = 2000000.0F;
   int layer = 0;
   static final int SNAP_NONE = 0;
   static final int SNAP_CLOSE = 1;
   static final int SNAP_THEME = 2;
   int snapRequest = 0;
   boolean haveSnapshot = false;
   boolean snapPass = false;
   float closeStart = -1.0F;
   String closeStyle = "Fade";
   float[][] shards;
   String pendingThemeName;
   List<String> pendingThemeCols;
   float themeAt = -99.0F;
   float themeOx;
   float themeOy;
   String themeStyle = "Ripple";
   Theme oldTheme;
   float sideW = 210.0F;
   boolean pageSlide = true;
   private final Map<String, Float> pendingScroll = new HashMap<>();
   private final Map<String, Float> pendingPanelScroll = new HashMap<>();
   private int restoreFrames = 0;
   String dragId;
   String focusId;
   final TextField chatField = new TextField(256);
   final Map<String, float[]> chatScroll = new HashMap<>();
   private final Map<String, List<String>> chatWrapCache = new HashMap<>();
   private Consumer<Float> chatWheel;
   String friendsTab = "Friends";
   String pFriendsTab = "Friends";
   private static final String[] PAGE_TABS = new String[]{"Friends", "Chat", "Messages"};
   private static final String[] PANEL_TABS = new String[]{"Friends", "Chat", "DMs", "Settings"};
   final TextField dmField = new TextField(256);
   String dmTarget;
   private List<Emoji.Entry> emojiSug = List.of();
   private TextField emojiSugField;
   private String emojiSugQ;
   private String emojiSugDismissed;
   private int emojiSugSel;
   final TextField cfgName = new TextField(40);
   final TextField cfgDesc = new TextField(300);
   final TextField cfgServer = new TextField(60);
   final TextField cfgSearch = new TextField(60);
   final TextField cfgCode = new TextField(12);
   String profilesTab = "Local";
   String pConfigsTab = "Local";
   String uploadSource = "Current setup";
   String uploadVisibility = "Private";
   private String searchSeen = "";
   private float searchEditAt = 0.0F;
   private static final String[] PROFILE_TABS = new String[]{"Local", "My Cloud", "Browse"};
   private static final String[] PROFILE_TAB_ICONS = new String[]{"Profiles", "cloud", "search"};
   static final String CONFIGS_PANEL = "Configs";
   static final String FRIENDS_PANEL = "Friends";
   static final String CLIENT = "Client";
   static final String[] CLIENT_SECTIONS = new String[]{"General", "Panels", "Background", "Animations", "3D"};
   static final Set<String> WINDOW_ONLY = Set.of("Page Title", "Page Transition");
   final Map<String, ClickGui.Panel> panels = new LinkedHashMap<>();
   final List<String> panelOrder = new ArrayList<>();
   boolean panelsLoaded = false;
   String draggingPanel;
   String hoverPanel;
   Runnable nameEnter;
   private Map<String, String> idNames;
   private String tipText;
   private String tipKey;
   private float sliderFrac = 0.5F;
   static final String ADD_SKIN = "__add_skin__";
   static GuiStore displayStore;
   private static final String MODEL_OPEN = "Open models folder…";
   private static final String MODEL_RELOAD = "Reload model";
   private static final String MODEL_NONE = "No file selected";
   private List<String> modelFilesCache = List.of();
   private long modelFilesAt;
   static final String TABS_SETTINGS = "GUI";
   static final String TABS_PANEL = "§tabs";
   private static final Map<String, String> TABBOX_STATE = new HashMap<>();
   final ClickGui.Panel tabsPanel = new ClickGui.Panel("§tabs", 0.0F, 0.0F);
   int tMain;
   int tBg;
   int tOutline;
   int tBlack;
   int tFont;
   int tMuted;
   int tAccent;
   int tAccentDark;
   boolean tShadow = true;
   float tabsFx = Float.NaN;
   float tabsFy = Float.NaN;
   boolean tabsDragging = false;
   final List<float[]> tColRects = new ArrayList<>();
   final Map<String, Integer> tAssign = new HashMap<>();
   String tAssignKey = "";
   int tAssignFrames = 0;
   Set<String> tFolded;
   static final float TCP_W = 214.0F;

   private static List<String> tabsFontOptions() {
      List<String> f = new ArrayList<>(List.of("Match GUI"));
      f.addAll(List.of(FONTS));
      return f;
   }

   private static void section(String name, GuiData.Setting... settings) {
      GUI_SECTIONS.put(name, List.of(settings));
      GUI_SETTINGS.addAll(List.of(settings));

      for (GuiData.Setting st : settings) {
         GUI_DEFAULTS.putIfAbsent(st.name, st.defaultValue());
      }
   }

   private static GuiData.Setting def(String name, String type, Object a2, Object a3) {
      return def(name, type, a2, a3, null);
   }

   private static GuiData.Setting def(String name, String type, Object a2, Object a3, Map<String, Object> meta) {
      List<Object> l = new ArrayList<>(List.of(name, type));
      l.add(a2);
      if (a3 != null) {
         l.add(a3);
      }

      if (meta != null) {
         l.add(new LinkedHashMap<>(meta));
      }

      return GuiData.Setting.of(l);
   }

   public ClickGui(GuiData data, GuiStore store) {
      this.data = data;
      this.store = store;
      displayStore = store;
      String remembered = lastPage;
      if (remembered == null && store.gui("Last Page") instanceof String sp) {
         remembered = sp;
      }

      if (remembered == null || !data.categories.containsKey(remembered) && !Arrays.asList(SPECIAL).contains(remembered)) {
         this.page = data.categories.isEmpty() ? "GUI" : data.categories.keySet().iterator().next();
      } else {
         this.page = remembered;
      }

      this.pageSwitchAt = -1.0F;
      if (store.gui("Friends Tab") instanceof String ft && Arrays.asList(PANEL_TABS).contains(ft)) {
         this.pFriendsTab = ft;
      }

      if (store.gui("Configs Tab") instanceof String ct && List.of("Local", "Cloud", "Browse").contains(ct)) {
         this.pConfigsTab = ct;
      }

      UiSounds.play(UiSounds.Sound.OPEN);

      try {
         if (this.gbool("Save State", false)) {
            this.restoreState();
         }
      } catch (Throwable var6) {
      }
   }

   public float openProgress() {
      return this.closing && this.closeStart >= 0.0F ? 1.0F - Anim.ease(this.closeT() * 1.15F) : this.open;
   }

   public boolean isClosed() {
      return this.closed;
   }

   public void close() {
      if (!this.closing) {
         UiSounds.play(UiSounds.Sound.CLOSE);
         this.closing = true;
         this.pop = null;
         this.focused = null;
         this.listenModule = null;
         this.listenSetting = null;
         this.drag = null;
         this.drag2 = null;
         this.closeStyle = this.gstr("Close Animation", "Fade");
         this.closeStart = -1.0F;
         if (this.panelMode() && !"Match Open/Close".equals(this.gstr("Panel Animation", "Match Open/Close"))) {
            this.closeStyle = "Panel";
            this.closeStart = this.time;
         } else if (!"Fade".equals(this.closeStyle)) {
            this.snapRequest = 1;
         } else {
            this.closeStart = this.time;
         }

         lastPage = this.page;

         try {
            this.store.setGui("Last Page", this.page);
         } catch (Throwable var3) {
         }

         try {
            this.saveState();
         } catch (Throwable var2) {
         }
      }
   }

   public float scale() {
      return (float)num(this.store.gui("Scale"), 1.0);
   }

   public String background() {
      Object b = this.store.gui("Background");
      return b == null ? "Aurora" : String.valueOf(b);
   }

   public boolean backgroundOn() {
      return !"None".equals(this.background());
   }

   public float backgroundDim() {
      return (float)num(this.get("Click GUI", "Background Dim"), 0.42);
   }

   public boolean blurWindowOnly() {
      return "Window Only".equals(this.gstr("Blur Area", "Everywhere"));
   }

   public boolean frosted() {
      return bool(this.get("Click GUI", "Frosted Panels"), true);
   }

   public boolean crispText() {
      return bool(this.get("Click GUI", "Smooth Text"), true);
   }

   public boolean wantsScene() {
      return this.backgroundOn() || this.frosted();
   }

   public int blur() {
      return (int)Math.round(num(this.store.gui("Blur"), 3.0));
   }

   public Theme theme() {
      return this.th;
   }

   public int bgAccent() {
      return this.oldTheme != null && !"Fade".equals(this.themeStyle) ? Theme.mix(this.oldTheme.accent, this.th.accent, this.themeP()) : this.th.accent;
   }

   public int bgAccent2() {
      return this.oldTheme != null && !"Fade".equals(this.themeStyle) ? Theme.mix(this.oldTheme.accent2, this.th.accent2, this.themeP()) : this.th.accent2;
   }

   public int snapshotRequest() {
      return this.snapRequest;
   }

   public void renderSnapshot(Canvas c, float sw, float sh) {
      this.sw = sw;
      this.sh = sh;
      this.an.frame(0.0F, this.an_speed());
      c.setFontFamily(this.gstr("Font", "Inter"));
      if (this.tabsMode()) {
         this.applyTabsFont(c);
      }

      this.layoutWindow(1.0F);
      this.hits.clear();
      this.layer = 0;
      this.noClip();
      this.snapPass = true;

      try {
         c.save();
         this.drawMain(c);
         c.restore();
      } finally {
         this.snapPass = false;
      }
   }

   public void snapshotTaken(boolean ok) {
      int req = this.snapRequest;
      this.snapRequest = 0;
      this.haveSnapshot = ok;
      if (req == 1) {
         if (!ok) {
            this.closeStyle = "Fade";
         }

         this.closeStart = this.time;
         this.buildShards();
      } else if (req == 2) {
         this.oldTheme = this.th;
         if (this.pendingThemeName != null) {
            this.store.applyTheme(this.pendingThemeName, this.pendingThemeCols);
         }

         if (this.pendingThemeName != null) {
            this.toast(this.pendingThemeName + " applied");
         }

         this.pendingThemeName = null;
         this.pendingThemeCols = null;
         this.themeAt = ok ? this.time : -99.0F;
         if (!ok) {
            this.oldTheme = null;
         }
      }
   }

   public boolean animating() {
      return this.an.busy() || this.closing || !this.toasts.isEmpty() || this.drag != null;
   }

   public void mouseMove(float x, float y) {
      this.mx = x;
      this.my = y;
      this.toWindow();
      if (this.drag != null) {
         this.drag.accept(this.wmx);
      }

      if (this.drag2 != null) {
         this.drag2.accept(new float[]{this.wmx, this.wmy});
      }
   }

   public void mouseDown(float x, float y, int button) {
      this.mouseMove(x, y);
      if (!this.closing) {
         if (this.listenModule == null && this.listenSetting == null) {
            ClickGui.Hit h = this.hitAt(this.wmx, this.wmy);
            if (this.pop == null || h != null && h.layer != 0) {
               if (this.focused != null) {
                  Runnable commit = this.focusCommit;
                  this.focused = null;
                  this.focusCommit = null;
                  if (commit != null) {
                     commit.run();
                  }
               }

               if (this.panelMode() && this.pop == null) {
                  ClickGui.Panel top = this.panelAt(this.wmx, this.wmy);
                  if (top != null) {
                     this.bringToFront(top);
                  }
               }

               if (h != null) {
                  Runnable act = button == 1 ? h.right : (button == 0 ? h.click : null);
                  if (act != null) {
                     UiSounds.beginClick();

                     try {
                        act.run();
                     } finally {
                        UiSounds.endClick(this.drag != null || this.drag2 != null || this.draggingPanel != null);
                     }
                  }
               }
            } else {
               this.pop = null;
               this.focused = null;
            }
         } else if (button == 0) {
            this.listenModule = null;
            this.listenSetting = null;
         } else {
            this.applyListen(switch (button) {
               case 1 -> "Right Click";
               case 2 -> "Middle Click";
               default -> "Mouse " + (button + 1);
            });
         }
      }
   }

   public void mouseUp(float x, float y, int button) {
      this.mouseMove(x, y);
      this.drag = null;
      this.drag2 = null;
      if (this.draggingPanel != null) {
         this.draggingPanel = null;
         this.savePanels();
      }

      if (this.tabsDragging) {
         this.tabsDragging = false;
         this.saveTabsPos();
      }
   }

   public void scroll(float x, float y, float delta) {
      this.mouseMove(x, y);
      if (this.pop != null && !this.pop.color && this.inPop(this.wmx, this.wmy)) {
         this.pop.scrollT = this.pop.scrollT - delta * (this.compact() ? this.setRowH() * 2.0F : 26.0F);
      } else if (this.tabsMode()) {
         this.tabsScroll(delta);
      } else if (this.panelMode()) {
         ClickGui.Hit wh_ = this.hitAt(this.wmx, this.wmy);
         if (wh_ != null && wh_.wheel != null) {
            wh_.wheel.accept(delta);
         } else {
            ClickGui.Panel p = this.panelAt(this.wmx, this.wmy);
            if (p != null) {
               p.scrollT = Math.max(0.0F, Math.min(p.maxScroll, p.scrollT - delta * this.rowH() * 2.5F));
            }
         }
      } else {
         ClickGui.Hit h = this.hitAt(this.wmx, this.wmy);
         if (h != null && h.wheel != null) {
            h.wheel.accept(delta);
         } else {
            float[] s = this.scroll.computeIfAbsent(this.pageKey(), k -> new float[3]);
            s[0] = Math.max(0.0F, Math.min(s[2], s[0] - delta * 52.0F));
         }
      }
   }

   public boolean key(int key, int mods) {
      if (this.closing) {
         return true;
      }

      boolean ctrl = (mods & 2) != 0;
      if (this.listenModule == null && this.listenSetting == null) {
         if (this.pop != null && this.pop.ids && key == 256 && !this.pop.filter.text.isEmpty()) {
            this.pop.filter.set("");
            return true;
         }

         if (this.pop != null && this.pop.ids && (key == 257 || key == 335) && this.focused == this.pop.filter && !this.pop.filter.text.isEmpty()) {
            List<GuiData.Entry> v = this.idView(this.pop, this.idSelection(this.pop.module, this.pop.key));
            if (!v.isEmpty()) {
               this.idToggle(this.pop, v.get(0).id());
            }

            return true;
         } else {
            if (this.focused != null && (key == 257 || key == 335)) {
               if (this.focused == this.cfgCode) {
                  this.focused = null;
                  this.loadCode();
                  return true;
               }

               if (this.focused == this.cfgSearch) {
                  this.focused = null;
                  CloudConfigs.query = this.cfgSearch.text.trim();
                  this.searchSeen = CloudConfigs.query;
                  CloudConfigs.refreshBrowse(false);
                  return true;
               }

               if (this.focused == this.cfgName || this.focused == this.cfgDesc || this.focused == this.cfgServer) {
                  this.focused = null;
                  return true;
               }
            }

            if (this.focused != null && (this.focused == this.chatField || this.focused == this.dmField) && this.emojiKey(key)) {
               return true;
            }

            if (this.focused != null && this.focused == this.dmField) {
               if (key == 257 || key == 335) {
                  this.sendDmField();
                  return true;
               }

               if (key == 256) {
                  this.focused = null;
                  return true;
               }
            }

            if (this.focused != null && this.focused == this.chatField) {
               if (key == 257 || key == 335) {
                  this.sendChatField();
                  return true;
               }

               if (key == 256) {
                  this.focused = null;
                  return true;
               }
            }

            if (this.focused != null && this.focused == this.nameField && this.nameEnter != null) {
               if (key == 257 || key == 335) {
                  this.nameEnter.run();
                  return true;
               }

               if (key == 256) {
                  this.nameField.set("");
                  this.focused = null;
                  this.nameEnter = null;
                  return true;
               }
            }

            if (this.focused == null) {
               if (key == 256) {
                  if (this.pop != null) {
                     this.pop = null;
                     return true;
                  } else if (!this.search.text.isEmpty()) {
                     this.search.set("");
                     return true;
                  } else {
                     this.close();
                     return true;
                  }
               } else if (key == 344) {
                  this.close();
                  return true;
               } else if (ctrl && key == 70) {
                  this.focus(this.search, null);
                  return true;
               } else {
                  return false;
               }
            } else if (key != 256 && key != 257 && key != 335) {
               if (this.focused.key(key, ctrl, this.store.clipboard())) {
                  if (this.focused == this.search) {
                     this.resetScroll();
                  }

                  if (this.focusCommit != null && this.focused != this.search && !this.typingField(this.focused) && (this.pop == null || this.focused != this.pop.filter)) {
                     this.focusCommit.run();
                  }

                  return true;
               } else {
                  return true;
               }
            } else {
               if (this.focusCommit != null) {
                  this.focusCommit.run();
               }

               if (this.focused == this.search && key == 256) {
                  this.search.set("");
               }

               this.focused = null;
               return true;
            }
         }
      } else {
         this.applyListen(key == 256 ? "NONE" : KeyNames.name(key));
         return true;
      }
   }

   public void chars(String s) {
      if (!this.closing) {
         if (this.focused == null && this.pop != null && !this.pop.color) {
            this.focus(this.pop.filter, null);
         }

         if (this.focused != null) {
            this.focused.type(s);
            if (this.focused == this.search) {
               this.resetScroll();
            }

            if (this.focusCommit != null && !this.typingField(this.focused) && (this.pop == null || this.focused != this.pop.filter)) {
               this.focusCommit.run();
            }
         }
      }
   }

   public void render(Canvas c, float sw, float sh, long nanos) {
      float dt = this.lastNanos == 0L ? 0.0F : (float)(nanos - this.lastNanos) / 1.0E9F;
      this.lastNanos = nanos;
      this.time += dt;
      this.sw = sw;
      this.sh = sh;
      this.an.frame(dt, this.an_speed());
      c.setFontFamily(this.gstr("Font", "Inter"));
      if (this.tabsMode()) {
         this.applyTabsFont(c);
      }

      this.th = Theme.from(this.store.theme());
      if (Chroma.active()) {
         this.th.accent = 0xFF000000 | Chroma.accent();
         this.th.accent2 = 0xFF000000 | Chroma.accent2();
      }

      if (this.oldTheme != null && this.themeP() >= 1.0F) {
         this.oldTheme = null;
         this.themeAt = -99.0F;
      }

      if (this.oldTheme != null && "Fade".equals(this.themeStyle)) {
         this.th = Theme.lerp(this.oldTheme, this.th, Anim.ease(this.themeP()));
      }

      this.ripples.removeIf(r -> this.time - r.at() > 1.0F);
      if (this.closing) {
         if (this.closeStart < 0.0F) {
            this.closeStart = this.time;
            this.closeStyle = "Fade";
            this.haveSnapshot = false;
         }

         float t = this.closeT();
         if (t >= 1.0F) {
            this.closed = true;
         } else {
            this.hits.clear();
            this.layer = 0;
            this.noClip();
            if ("Panel".equals(this.closeStyle)) {
               this.layoutWindow(1.0F);
               c.save();
               this.drawPanels(c);
               c.restore();
               this.drawToasts(c);
            } else {
               this.drawClose(c, t);
               this.drawToasts(c);
            }
         }
      } else {
         this.open = this.an.to("open", 1.0F, 7.5F);
         this.layoutWindow(this.open);
         this.toWindow();
         this.hits.clear();
         this.layer = 0;
         this.noClip();
         if (this.panelMode()) {
            c.save();
            this.drawPanels(c);
            c.restore();
            this.drawThemeTransition(c);
            this.drawToasts(c);
            this.drawTooltip(c);
         } else {
            String oa = this.gstr("Open Animation", "Fade");
            boolean crt = "CRT".equals(oa);
            float crtV = 1.0F;
            c.save();
            if (crt) {
               float p1 = Anim.easeOutCubic(Math.min(1.0F, this.open / 0.4F));
               crtV = Anim.easeOutCubic(Math.max(0.0F, (this.open - 0.35F) / 0.65F));
               c.alpha(Math.min(1.0F, this.open * 4.0F));
               c.translate(this.winCx, this.winCy);
               c.scale2(Math.max(0.003F, p1), 0.01F + 0.99F * crtV);
               c.translate(-this.winCx, -this.winCy);
            } else {
               c.alpha("Slide".equals(oa) ? Math.min(1.0F, this.open * 2.0F) : Math.min(1.0F, this.open * 1.15F));
               c.translate(this.winCx, this.winCy);
               c.scale(this.winScale);
               c.translate(-this.winCx, -this.winCy);
            }

            if (this.tabsMode()) {
               this.drawTabs(c);
            } else {
               this.drawWindow(c);
            }

            if (crt && crtV < 0.999F) {
               c.rect(this.wx, this.wy, this.ww, this.wh, this.winR(), Theme.a(-1, (1.0F - crtV) * 0.85F));
            }

            c.restore();
            this.drawThemeTransition(c);
            this.drawToasts(c);
            this.drawTooltip(c);
         }
      }
   }

   private void layoutWindow(float o) {
      if (this.panelMode()) {
         this.wx = 0.0F;
         this.wy = 0.0F;
         this.ww = this.sw;
         this.wh = this.sh;
         this.winScale = 1.0F;
         this.winCx = this.sw / 2.0F;
         this.winCy = this.sh / 2.0F;
      } else if (this.tabsMode()) {
         this.layoutTabs(o);
      } else {
         float W = (float)num(this.get("Click GUI", "Window Width"), 1000.0);
         float H = (float)num(this.get("Click GUI", "Window Height"), 650.0);
         this.ww = Math.min(W, this.sw - 48.0F);
         this.wh = Math.min(H, this.sh - 48.0F);
         this.sideW = Math.max(150.0F, Math.min((float)num(this.get("Click GUI", "Sidebar Width"), 210.0), this.ww * 0.4F));
         this.wx = (this.sw - this.ww) / 2.0F;
         this.wy = (this.sh - this.wh) / 2.0F;
         this.winScale = 1.0F;
         if (!this.snapPass) {
            switch (this.gstr("Open Animation", "Fade")) {
               case "Pop":
                  this.wy = this.wy + (1.0F - Anim.easeOutCubic(o)) * 26.0F;
                  this.winScale = 0.9F + 0.1F * Anim.easeOutBack(Math.min(1.0F, o * 1.02F));
                  break;
               case "Slide":
                  this.wy = this.wy + (1.0F - Anim.easeOutCubic(o)) * this.sh * 0.4F;
                  break;
               case "Zoom":
                  this.winScale = 0.55F + 0.45F * Anim.easeOutCubic(o);
            }
         }

         this.winCx = this.wx + this.ww / 2.0F;
         this.winCy = this.wy + this.wh / 2.0F;
      }
   }

   private float winR() {
      return this.tabsMode() ? 0.0F : (float)num(this.get("Click GUI", "Rounding"), 16.0);
   }

   private void drawMain(Canvas c) {
      if (this.panelMode()) {
         this.drawPanels(c);
      } else if (this.tabsMode()) {
         this.drawTabs(c);
      } else {
         this.drawWindow(c);
      }
   }

   private void drawWindow(Canvas c) {
      float R = this.winR();
      float panelA = (float)num(this.get("Click GUI", "Panel Opacity"), 0.82);
      boolean glowOn = bool(this.get("Click GUI", "Accent Glow"), true);
      float bw = (float)num(this.get("Click GUI", "Border Width"), 1.2);
      float bo = (float)num(this.get("Click GUI", "Border Opacity"), 0.55);
      float shadowK = (float)num(this.get("Click GUI", "Shadow"), 1.0);
      if (shadowK > 0.01F) {
         c.shadow(this.wx, this.wy + 18.0F * shadowK, this.ww, this.wh, R, 30.0F + 40.0F * shadowK, Theme.a(-16777216, Math.min(0.9F, 0.6F * shadowK)));
      }

      if (glowOn) {
         float pulse = 0.18F + 0.06F * (float)Math.sin(this.time * 1.6F);
         c.shadow(this.wx, this.wy, this.ww, this.wh, R, 46.0F, Theme.a(this.th.accent, pulse));
      }

      if (this.frosted()) {
         c.glass(this.wx, this.wy, this.ww, this.wh, R, 1.0F);
      }

      c.rect(this.wx, this.wy, this.ww, this.wh, R, Theme.a(this.th.surface, panelA * 0.88F));
      c.gradient(this.wx, this.wy, this.ww, this.wh * 0.5F, R, Theme.a(this.th.accent, 0.07F), Theme.a(this.th.accent, 0.0F), false);
      if (bw > 0.01F && bo > 0.01F) {
         c.strokeGradient(this.wx + bw / 2.0F, this.wy + bw / 2.0F, this.ww - bw, this.wh - bw, R, bw, Theme.a(this.th.accent, bo), Theme.a(this.th.border, bo * 0.9F));
      }

      this.drawSidebar(c, R);
      this.drawContent(c);
      if (this.pop != null && !this.snapPass) {
         this.drawPop(c);
      }
   }

   private void toWindow() {
      this.wmx = this.winCx + (this.mx - this.winCx) / this.winScale;
      this.wmy = this.winCy + (this.my - this.winCy) / this.winScale;
   }

   private void drawSidebar(Canvas c, float R) {
      float sx = this.wx;
      float sy = this.wy;
      float sW = this.sideW;
      c.rect(sx, sy, sW, this.wh, R, Theme.a(this.th.bg, 0.35F));
      c.line(sx + sW, sy + 18.0F, sx + sW, sy + this.wh - 18.0F, 1.0F, Theme.a(this.th.border, 0.6F));
      String tintMode = this.gstr("Logo Tint", "Accent");

      int tint = switch (tintMode) {
         case "Accent 2" -> this.th.accent2;
         case "Text" -> this.th.text;
         default -> this.th.accent;
      };
      boolean original = "Original".equals(tintMode);
      float S = 38.0F;
      float base = sy + 47.0F;
      float lx = sx + 20.0F - 0.25F * S;
      float ly = base - 0.762F * S;
      if (bool(this.get("Click GUI", "Logo Glow"), true)) {
         float breathe = 0.5F + 0.5F * (float)Math.sin(this.time * 1.8F);
         c.glow(lx + 0.5F * S, ly + 0.5F * S, 22.0F + 4.0F * breathe, Theme.a(original ? -1 : tint, 0.2F + 0.1F * breathe));
      }

      if (!c.logo(lx, ly, S, original ? -1 : tint, 1.0F)) {
         c.text("inter-sb", 22.0F, sx + 20.0F, base, 65, tint, "C");
      }

      c.text("inter-sb", 22.0F, lx + 0.746F * S + 3.5F, base, 65, this.th.text, "larity");
      float fx = sx + 16.0F;
      float fy = sy + 70.0F;
      float fw = sW - 32.0F;
      float fh = 32.0F;
      float cr = this.ctlR() + 1.0F;
      boolean sf = this.focused == this.search;
      float sfT = this.an.to("searchFocus", sf ? 1.0F : 0.0F, 14.0F);
      c.rect(fx, fy, fw, fh, cr, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.04F), 0.7F));
      c.stroke(fx, fy, fw, fh, cr, 1.0F, Theme.mix(Theme.a(this.th.border, 0.8F), this.th.accent, sfT));
      if (sfT > 0.01F) {
         c.shadow(fx, fy, fw, fh, cr, 12.0F, Theme.a(this.th.accent, 0.25F * sfT));
      }

      Icons.draw(c, "search", fx + 16.0F, fy + fh / 2.0F, 14.0F, Theme.mix(this.th.muted, this.th.accent, sfT));
      String st = this.search.text;
      c.save();
      c.scissor(fx + 28.0F, fy, fw - 36.0F, fh);
      if (st.isEmpty() && !sf) {
         c.text("inter", 12.5F, fx + 30.0F, fy + fh / 2.0F, 17, this.th.muted, "Search modules…");
      } else {
         c.text("inter", 12.5F, fx + 30.0F, fy + fh / 2.0F, 17, this.th.text, st);
         if (sf) {
            this.caret(c, "inter", 12.5F, fx + 30.0F, fy + fh / 2.0F, this.search);
         }
      }

      c.restore();
      this.hit(fx, fy, fw, fh, () -> this.focus(this.search, null), null);
      float ny = fy + fh + 22.0F;
      List<String> nav = new ArrayList<>();
      c.text("inter-sb", 9.5F, sx + 22.0F, ny, 17, Theme.a(this.th.muted, 0.8F), "MODULES");
      ny += 12.0F;
      float itemH = 34.0F;
      float activeY = -1.0F;
      int idx = 0;
      Map<String, Float> itemY = new LinkedHashMap<>();

      for (String cat : this.data.categories.keySet()) {
         if (!"Client".equals(cat)) {
            itemY.put(cat, ny);
            ny += itemH + 2.0F;
            nav.add(cat);
         }
      }

      ny += 14.0F;
      c.text("inter-sb", 9.5F, sx + 22.0F, ny, 17, Theme.a(this.th.muted, 0.8F), "CLIENT");
      ny += 12.0F;
      if (this.data.categories.containsKey("Client")) {
         itemY.put("Client", ny);
         ny += itemH + 2.0F;
         nav.add("Client");
      }

      for (String sp : SPECIAL) {
         itemY.put(sp, ny);
         ny += itemH + 2.0F;
         nav.add(sp);
      }

      String activePage = this.search.text.isEmpty() ? this.page : null;
      if (activePage != null && itemY.containsKey(activePage)) {
         activeY = itemY.get(activePage);
      }

      float pr = Math.min(10.0F, this.ctlR() + 1.0F);
      if (activeY >= 0.0F) {
         float py = this.an.spring("pillY", activeY, 260.0F, 22.0F);
         float ph = this.an.spring("pillH", itemH, 300.0F, 20.0F);
         float pa = this.an.to("pillA", 1.0F, 10.0F);
         c.shadow(sx + 12.0F, py, sW - 24.0F, ph, pr, 18.0F, Theme.a(this.th.accent, 0.22F * pa));
         c.gradient(sx + 12.0F, py, sW - 24.0F, ph, pr, Theme.a(this.th.accent, 0.26F * pa), Theme.a(this.th.accent2, 0.08F * pa), true);
         c.stroke(sx + 12.0F, py, sW - 24.0F, ph, pr, 1.0F, Theme.a(this.th.accent, 0.35F * pa));
      } else {
         this.an.to("pillA", 0.0F, 10.0F);
      }

      for (String item : nav) {
         float y = itemY.get(item);
         boolean active = item.equals(activePage);
         float stagger = Anim.easeOutCubic((this.time * this.an_speed() - 0.03F * idx) / 0.35F);
         idx++;
         float hov = this.an.to("navHover:" + item, !active && this.in(sx + 12.0F, y, sW - 24.0F, itemH) ? 1.0F : 0.0F, 16.0F);
         if (hov > 0.01F) {
            c.rect(sx + 12.0F, y, sW - 24.0F, itemH, pr, Theme.a(this.th.text, 0.05F * hov));
         }

         float slide = (1.0F - stagger) * -14.0F;
         c.save();
         c.alpha(stagger);
         c.translate(slide, 0.0F);
         int col = active ? this.th.text : Theme.mix(this.th.muted, this.th.text, hov * 0.7F);
         Icons.draw(c, item, sx + 34.0F, y + itemH / 2.0F, 16.0F, active ? this.th.accent : col);
         c.text("inter-med", 13.0F, sx + 52.0F, y + itemH / 2.0F, 17, col, item);
         c.restore();
         String target = item;
         this.hit(sx + 12.0F, y, sW - 24.0F, itemH, () -> this.switchPage(target), null);
      }
   }

   private String gstr(String key, String def) {
      return str(this.get("Click GUI", key), def);
   }

   private float gnum(String key, double def) {
      return (float)num(this.get("Click GUI", key), def);
   }

   private boolean gbool(String key, boolean def) {
      return bool(this.get("Click GUI", key), def);
   }

   private float ctlR() {
      return this.gnum("Control Rounding", 9.0);
   }

   private float an_speed() {
      return (float)num(this.store.gui("Animation Speed"), 1.0);
   }

   private void saveState() {
      if (!this.gbool("Save State", false)) {
         this.store.setGui("Saved State", new ArrayList());
      } else {
         List<String> out = new ArrayList<>();
         out.add("page|" + this.page);
         if (!this.search.text.isEmpty()) {
            out.add("search|" + this.search.text);
         }

         out.add("tab|friends|" + this.friendsTab);
         out.add("tab|pfriends|" + this.pFriendsTab);
         out.add("tab|pconfigs|" + this.pConfigsTab);
         if (this.dmTarget != null) {
            out.add("dm|" + this.dmTarget);
         }

         for (String e : this.expanded) {
            out.add("exp|" + e);
         }

         for (Entry<String, float[]> e : this.scroll.entrySet()) {
            if (e.getValue()[0] > 0.5F) {
               out.add("scroll|" + e.getKey() + "|" + Math.round(e.getValue()[0]));
            }
         }

         for (ClickGui.Panel pn : this.panels.values()) {
            if (pn.scrollT > 0.5F) {
               out.add("pscroll|" + pn.name + "|" + Math.round(pn.scrollT));
            }
         }

         int n = 0;

         for (Entry<String, Float> e : this.measured.entrySet()) {
            if (n++ > 400) {
               break;
            }

            out.add("meas|" + e.getKey() + "|" + Math.round(e.getValue()));
         }

         this.store.setGui("Saved State", out);
      }
   }

   private void restoreState() {
      List<String> lines = list(this.store.gui("Saved State"), List.of());
      if (!lines.isEmpty()) {
         for (String line : lines) {
            int bar = line.indexOf(124);
            if (bar >= 0) {
               String kind = line.substring(0, bar);
               String rest = line.substring(bar + 1);
               int last = rest.lastIndexOf(124);
               String key = last < 0 ? rest : rest.substring(0, last);
               String val = last < 0 ? "" : rest.substring(last + 1);
               switch (kind) {
                  case "page":
                     if (this.data.categories.containsKey(rest) || Arrays.asList(SPECIAL).contains(rest)) {
                        this.page = rest;
                        lastPage = rest;
                     }
                     break;
                  case "search":
                     this.search.set(rest);
                     break;
                  case "tab":
                     switch (key) {
                        case "friends":
                           if (Arrays.asList(PAGE_TABS).contains(val)) {
                              this.friendsTab = val;
                           }
                           continue;
                        case "pfriends":
                           if (Arrays.asList(PANEL_TABS).contains(val)) {
                              this.pFriendsTab = val;
                           }
                           continue;
                        case "pconfigs":
                           if (List.of("Local", "Cloud", "Browse").contains(val)) {
                              this.pConfigsTab = val;
                           }
                        default:
                           continue;
                     }
                  case "dm":
                     this.dmTarget = rest;
                     break;
                  case "exp":
                     this.expanded.add(rest);
                     break;
                  case "scroll":
                     try {
                        this.pendingScroll.put(key, Float.parseFloat(val));
                     } catch (NumberFormatException var16) {
                     }
                     break;
                  case "pscroll":
                     try {
                        this.pendingPanelScroll.put(key, Float.parseFloat(val));
                     } catch (NumberFormatException var15) {
                     }
                     break;
                  case "meas":
                     try {
                        this.measured.put(key, Float.parseFloat(val));
                     } catch (NumberFormatException var14) {
                     }
               }
            }
         }

         this.pageSwitchAt = -99.0F;

         for (Entry<String, Float> e : this.pendingScroll.entrySet()) {
            this.scroll.put(e.getKey(), new float[]{e.getValue(), e.getValue(), e.getValue()});
            this.an.set("scroll:" + e.getKey(), e.getValue());
         }
      }
   }

   private void switchPage(String p) {
      if (!this.search.text.isEmpty()) {
         this.search.set("");
      }

      if (!p.equals(this.page)) {
         UiSounds.play(UiSounds.Sound.TAB);
         this.page = p;
         lastPage = p;
         this.pageSwitchAt = this.time;
         this.pop = null;
         if (this.focused != this.search) {
            this.focused = null;
         }
      }
   }

   private void resetScroll() {
      float[] s = this.scroll.computeIfAbsent(this.pageKey(), k -> new float[3]);
      s[0] = 0.0F;
      this.pageSwitchAt = this.time - 1.0F;
   }

   private String pageKey() {
      return this.search.text.isEmpty() ? this.page : "§search";
   }

   private void drawContent(Canvas c) {
      float cx = this.wx + this.sideW;
      float cy = this.wy;
      float cw = this.ww - this.sideW;
      float ch = this.wh;
      String pt = this.gstr("Page Transition", "Slide");
      float since = "None".equals(pt) ? 99.0F : (this.time - this.pageSwitchAt) * this.an_speed();
      float pageT = Anim.easeOutCubic(since / 0.38F);
      boolean slide = "Slide".equals(pt);
      this.pageSlide = slide;
      boolean searching = !this.search.text.isEmpty();
      List<GuiData.Module> list = null;
      String title;
      String sub;
      if (searching) {
         String q = this.search.text.toLowerCase(Locale.ROOT);
         list = new ArrayList<>();

         for (GuiData.Module m : this.data.modules.values()) {
            if (!m.name.toLowerCase(Locale.ROOT).contains(q) && !m.description.toLowerCase(Locale.ROOT).contains(q)) {
               for (GuiData.Setting s : m.settings) {
                  if (s.name.toLowerCase(Locale.ROOT).contains(q)) {
                     list.add(m);
                     break;
                  }
               }
            } else {
               list.add(m);
            }
         }

         title = "Search";
         sub = list.size() + " result" + (list.size() == 1 ? "" : "s") + " for “" + this.search.text + "”";
      } else if (this.data.categories.containsKey(this.page)) {
         list = this.data.categories.get(this.page);
         title = this.page;
         sub = null;
      } else {
         title = this.page;

         sub = switch (this.page) {
            case "Themes" -> "Pick a look. Applies to both menus.";
            case "Profiles" -> "Save, sync and share whole setups, and browse everyone else's.";
            case "Binds" -> "Save every keybind as a named set.";
            case "Servers" -> "Auto-apply a profile when you join a server.";
            case "Friends" -> "Protect friends from combat modules, style their names and see who's online.";
            default -> "Customise this menu.";
         };
      }

      boolean showTitle = this.gbool("Page Title", true);
      float top;
      if (showTitle) {
         c.save();
         c.alpha(pageT);
         if (slide) {
            c.translate((1.0F - pageT) * 18.0F, 0.0F);
         }

         if (sub == null) {
            c.text("inter-sb", 24.0F, cx + 28.0F, cy + 44.0F, 17, this.th.text, title);
            top = cy + 72.0F;
         } else {
            c.text("inter-sb", 24.0F, cx + 28.0F, cy + 38.0F, 17, this.th.text, title);
            c.text("inter", 12.0F, cx + 28.0F, cy + 62.0F, 17, this.th.muted, sub);
            top = cy + 84.0F;
         }

         c.restore();
      } else {
         top = cy + 14.0F;
      }

      float ax = cx + 14.0F;
      float ay = top;
      float aw = cw - 28.0F;
      float ah = ch - (top - cy) - 12.0F;
      float[] s = this.scroll.computeIfAbsent(this.pageKey(), k -> new float[3]);
      s[1] = this.an.to("scroll:" + this.pageKey(), s[0], 16.0F);
      float off = s[1];
      c.save();
      c.scissor(ax, ay, aw, ah);
      this.clip(ax, ay, aw, ah);
      float y0 = ay + 6.0F - off;
      float contentH;
      if (list != null) {
         contentH = this.drawModuleList(c, list, ax + 8.0F, y0, aw - 22.0F, since, searching) - y0;
      } else {
         contentH = switch (this.page) {
            case "Themes" -> this.drawThemes(c, ax + 8.0F, y0, aw - 22.0F, since);
            case "Profiles" -> this.drawProfilesPage(c, ax + 8.0F, y0, aw - 22.0F, since);
            case "Binds" -> this.drawBindSets(c, ax + 8.0F, y0, aw - 22.0F, since);
            case "Servers" -> this.drawServers(c, ax + 8.0F, y0, aw - 22.0F, since);
            case "Friends" -> this.drawFriends(c, ax + 8.0F, y0, aw - 22.0F, since);
            default -> this.drawGuiPage(c, ax + 8.0F, y0, aw - 22.0F, since);
         } - y0;
      }

      this.noClip();
      c.restore();
      float max = Math.max(0.0F, contentH - ah + 16.0F);
      s[2] = max;
      if (s[0] > max) {
         s[0] = max;
      }

      Float want = this.pendingScroll.get(this.pageKey());
      if (want != null) {
         if (!(max + 0.5F >= want) && ++this.restoreFrames <= 40) {
            s[0] = want;
         } else {
            s[0] = Math.min(want, max);
            this.pendingScroll.remove(this.pageKey());
         }

         this.an.set("scroll:" + this.pageKey(), s[0]);
      }

      if (max > 1.0F) {
         float barH = Math.max(36.0F, ah * ah / (ah + max));
         float barY = ay + (ah - barH) * (off / max);
         float hv = this.an.to("sbHover", this.in(ax + aw - 10.0F, ay, 10.0F, ah) ? 1.0F : 0.0F, 12.0F);
         c.rect(ax + aw - 6.0F - hv * 1.5F, barY, 3.0F + hv * 3.0F, barH, 3.0F, Theme.a(this.th.text, 0.18F + 0.2F * hv));
      }
   }

   private float drawModuleList(Canvas c, List<GuiData.Module> list, float x, float y, float w, float since, boolean searching) {
      boolean showDesc = this.gbool("Show Descriptions", true);
      float gap = this.gnum("Card Spacing", 10.0);
      int i = 0;

      for (GuiData.Module m : list) {
         float appear = Anim.easeOutCubic((since - 0.022F * Math.min(i, 18)) / 0.34F);
         y = this.drawModuleCard(c, m, x, y, w, appear, showDesc, searching) + gap;
         i++;
      }

      if (list.isEmpty()) {
         c.text("inter-med", 13.0F, x + w / 2.0F, y + 60.0F, 18, this.th.muted, "Nothing matches.");
         y += 120.0F;
      }

      return y + 10.0F;
   }

   private float drawModuleCard(Canvas c, GuiData.Module m, float x, float y, float w, float appear, boolean showDesc, boolean searching) {
      String name = m.name;
      boolean on = this.store.isEnabled(name);
      float H = showDesc ? 58.0F : 46.0F;
      float expT = this.an.to("exp:" + name, this.expanded.contains(name) ? 1.0F : 0.0F, 11.0F);
      float setH = this.measured.getOrDefault(name, 0.0F);
      float h = H + setH * Anim.ease(expT);
      float enT = this.an.to("en:" + name, on ? 1.0F : 0.0F, 12.0F);
      boolean visible = y + h > this.clipY - 4.0F && y < this.clipY + this.clipH + 4.0F;
      float ry = y + (this.pageSlide ? (1.0F - appear) * 16.0F : 0.0F);
      if (visible) {
         float hov = this.an.to("cardHover:" + name, this.in(x, ry, w, H) && this.pop == null ? 1.0F : 0.0F, 14.0F);
         float CR = this.gnum("Card Rounding", 14.0);
         float cardA = this.gnum("Card Opacity", 0.62);
         String style = this.gstr("Card Style", "Filled");
         String enStyle = this.gstr("Enabled Style", "Glow");
         boolean lift = this.gbool("Hover Lift", true);
         float liftY = lift ? -1.5F * hov : 0.0F;
         ry += liftY;
         c.save();
         c.alpha(appear);
         if (lift && hov > 0.01F && !"Flat".equals(style)) {
            c.shadow(x, ry + 3.0F, w, h, CR, 16.0F, Theme.a(-16777216, 0.22F * hov));
         }

         if ("Glow".equals(enStyle) && enT > 0.01F) {
            c.shadow(x, ry, w, h, CR, 18.0F, Theme.a(this.th.accent, 0.14F * enT));
         }

         int fill = Theme.mix(this.th.bg, this.th.text, 0.035F + 0.025F * hov);
         switch (style) {
            case "Outline":
               if (hov > 0.01F) {
                  c.rect(x, ry, w, h, CR, Theme.a(this.th.text, 0.035F * hov));
               }
               break;
            case "Flat":
               c.rect(x, ry, w, h, CR, Theme.a(fill, cardA * 0.75F));
               break;
            default:
               c.rect(x, ry, w, h, CR, Theme.a(fill, cardA));
         }

         if (enT > 0.01F && ("Glow".equals(enStyle) || "Tint".equals(enStyle))) {
            c.gradient(x, ry, w * 0.7F, h, CR, Theme.a(this.th.accent, ("Tint".equals(enStyle) ? 0.22F : 0.15F) * enT), Theme.a(this.th.accent, 0.0F), true);
         }

         if (!"Flat".equals(style)) {
            float accentMix = !"None".equals(enStyle) && !"Text".equals(enStyle) ? enT * 0.8F : 0.0F;
            c.stroke(x, ry, w, h, CR, 1.0F, Theme.mix(Theme.a(this.th.border, ("Outline".equals(style) ? 0.85F : 0.55F) + 0.25F * hov), Theme.a(this.th.accent, 0.55F), accentMix));
         }

         if (this.gbool("Ripple", true)) {
            c.save();
            c.scissor(x, ry, w, h);

            for (ClickGui.Ripple rp : this.ripples) {
               if (rp.key.equals(name)) {
                  float t = (this.time - rp.at) / 0.65F;
                  if (!(t > 1.0F)) {
                     c.circle(rp.x, rp.y + (ry - y), 20.0F + Anim.easeOutCubic(t) * w * 0.9F, Theme.a(this.th.accent, 0.22F * (1.0F - t)));
                  }
               }
            }

            c.restore();
         }

         float tx = x + 20.0F;
         int nameCol = "Text".equals(enStyle) ? Theme.mix(this.th.text, this.th.accent, enT) : this.th.text;
         c.text("inter-sb", 14.5F, tx, ry + (showDesc ? 22.0F : H / 2.0F), 17, nameCol, name);
         if (searching) {
            float nw = c.textWidth("inter-sb", 14.5F, name);
            String cat = m.category;
            float bw = c.textWidth("inter-med", 9.5F, cat) + 12.0F;
            c.rect(tx + nw + 8.0F, ry + (showDesc ? 22.0F : H / 2.0F) - 8.0F, bw, 16.0F, 8.0F, Theme.a(this.th.accent, 0.14F));
            c.text("inter-med", 9.5F, tx + nw + 8.0F + bw / 2.0F, ry + (showDesc ? 22.0F : H / 2.0F), 18, this.th.accent, cat);
         }

         float fry = ry;
         this.hit(x, ry, w, H, () -> {
            this.ripples.add(new ClickGui.Ripple(name, this.wmx, this.wmy - (fry - y), this.time));
            this.toggleModule(m);
         }, () -> this.toggleExpand(name));
         float swx = x + w - 20.0F - 42.0F;
         float swy = ry + H / 2.0F - 12.0F;
         this.drawSwitch(c, "msw:" + name, swx, swy, 42.0F, 24.0F, on);
         this.hit(swx - 4.0F, swy - 4.0F, 50.0F, 32.0F, () -> this.toggleModule(m), null);
         String bind = this.store.bind(name);
         boolean listening = name.equals(this.listenModule) && this.listenSetting == null;
         String bl = listening ? "Press a key…" : (bind != null && !bind.isEmpty() && !"null".equals(bind) ? bind : "Bind");
         boolean hold = "hold".equals(this.store.bindMode(name));
         float chipW = c.textWidth("inter-med", 11.0F, bl) + 18.0F + (hold && !listening ? 34 : 0);
         float chx = swx - 12.0F - chipW;
         float chy = ry + H / 2.0F - 12.0F;
         float lt = listening ? 0.5F + 0.5F * (float)Math.sin(this.time * 8.0F) : 0.0F;
         float chHov = this.an.to("chipHover:" + name, this.in(chx, chy, chipW, 24.0F) ? 1.0F : 0.0F, 16.0F);
         c.rect(chx, chy, chipW, 24.0F, this.ctlR() - 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.accent, listening ? 0.25F : 0.05F + 0.07F * chHov), 0.8F));
         c.stroke(chx, chy, chipW, 24.0F, this.ctlR() - 1.0F, 1.0F, listening ? Theme.a(this.th.accent, 0.5F + 0.5F * lt) : Theme.a(this.th.border, 0.8F));
         boolean unset = bind == null || bind.isEmpty() || "null".equals(bind);
         c.text("inter-med", 11.0F, chx + 9.0F, chy + 12.0F, 17, listening ? this.th.accent : (unset ? this.th.muted : this.th.text), bl);
         if (hold && !listening) {
            float hx = chx + chipW - 38.0F;
            c.rect(hx, chy + 5.0F, 30.0F, 14.0F, 7.0F, Theme.a(this.th.accent, 0.2F));
            c.text("inter-sb", 8.0F, hx + 15.0F, chy + 12.0F, 18, this.th.accent, "HOLD");
         }

         this.hit(chx, chy, chipW, 24.0F, () -> {
            this.listenModule = name;
            this.listenSetting = null;
         }, () -> {
            this.store.setBind(name, this.store.bind(name), hold ? "toggle" : "hold");
            this.toast(name + ": " + (hold ? "toggle" : "hold") + " mode");
         });
         float descRight = chx - 16.0F;
         if (!m.settings.isEmpty()) {
            float chevX = chx - 22.0F;
            float rot = Anim.ease(expT);
            c.save();
            c.translate(chevX, ry + H / 2.0F);
            Icons.draw(c, rot > 0.5F ? "down" : "chevron", 0.0F, 0.0F, 16.0F, Theme.mix(this.th.muted, this.th.accent, rot));
            c.restore();
            this.hit(chevX - 12.0F, ry + H / 2.0F - 12.0F, 24.0F, 24.0F, () -> this.toggleExpand(name), null);
            descRight = chevX - 18.0F;
         }

         if (showDesc) {
            String d = this.ellipsize(c, "inter", 11.5F, m.description, descRight - tx);
            c.text("inter", 11.5F, tx, ry + 40.0F, 17, this.th.muted, d);
            if (!d.equals(m.description) && this.in(tx, ry + 31.0F, descRight - tx, 18.0F)) {
               this.hoverTip(name, m.description);
            }
         }

         if (expT > 0.002F || setH == 0.0F) {
            float sy0 = ry + H;
            c.save();
            float vis = setH * Anim.ease(expT);
            c.scissor(x, sy0, w, Math.max(0.0F, vis));
            float savedY = this.clipY;
            float savedH = this.clipH;
            float savedX = this.clipX;
            float savedW = this.clipW;
            this.intersectClip(x, sy0, w, vis);
            c.alpha(Anim.ease(Math.min(1.0F, expT * 1.4F)));
            c.line(x + 16.0F, sy0, x + w - 16.0F, sy0, 1.0F, Theme.a(this.th.border, 0.5F));
            float end = this.drawSettings(c, name, m.settings, x + 20.0F, sy0 + 8.0F, w - 40.0F);
            float resetY = end + 2.0F;
            float rbw = 76.0F;
            float rh = this.an.to("resetHover:" + name, this.in(x + w - 20.0F - rbw, resetY, rbw, 24.0F) ? 1.0F : 0.0F, 14.0F);
            c.rect(x + w - 20.0F - rbw, resetY, rbw, 24.0F, this.ctlR() - 1.0F, Theme.a(this.th.text, 0.04F + 0.05F * rh));
            Icons.draw(c, "reset", x + w - 20.0F - rbw + 16.0F, resetY + 12.0F, 12.0F, Theme.mix(this.th.muted, this.th.text, rh));
            c.text("inter-med", 11.0F, x + w - 20.0F - rbw + 28.0F, resetY + 12.0F, 17, Theme.mix(this.th.muted, this.th.text, rh), "Reset");
            this.hit(x + w - 20.0F - rbw, resetY, rbw, 24.0F, () -> this.confirm("reset:" + name, () -> {
               this.store.resetModule(name);
               this.toast(name + " reset to defaults");
            }), null);
            if ("reset:".concat(name).equals(this.confirmId) && this.time - this.confirmAt < 2.5F) {
               c.text("inter-med", 11.0F, x + w - 28.0F - rbw, resetY + 12.0F, 20, this.th.accent, "Click again to reset");
            }

            this.clipX = savedX;
            this.clipY = savedY;
            this.clipW = savedW;
            this.clipH = savedH;
            this.measured.put(name, end - sy0 + 36.0F);
            c.restore();
         }

         c.restore();
      } else {
         this.hoverSinceReset(name);
      }

      return y + h;
   }

   private void openHudEditor(String module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         this.toast("Join a world to use the HUD editor");
      } else {
         HudLayout.openAfterGui(module);
         this.close();
      }
   }

   private void toggleModule(GuiData.Module m) {
      boolean now = !this.store.isEnabled(m.name);
      UiSounds.toggle(now);
      this.store.setEnabled(m.name, now);
   }

   private void toggleExpand(String name) {
      if (!this.expanded.remove(name)) {
         this.expanded.add(name);
      }
   }

   float drawSettings(Canvas c, String module, List<GuiData.Setting> settings, float x, float y, float w) {
      if (HudEditorPanel.isHudModule(module)) {
         c.text("inter", 11.5F, x, y + 14.0F, 17, this.th.muted, "HUD elements are set up in the HUD editor (Right Ctrl in game).");
         this.button(c, "hudedit:" + module, x, y + 28.0F, 190.0F, 30.0F, "Open in HUD editor", true, () -> this.openHudEditor(module));
         return y + 68.0F;
      }

      int row = 0;

      for (GuiData.Setting s : settings) {
         if (this.visible(module, s)) {
            Map<String, List<String>> groups = "select".equals(s.type) ? s.groups() : null;
            if (groups != null) {
               String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
               String cat = null;

               for (Entry<String, List<String>> e : groups.entrySet()) {
                  if (e.getValue().contains(cur)) {
                     cat = e.getKey();
                     break;
                  }
               }

               if (cat == null) {
                  cat = groups.keySet().iterator().next();
               }

               String fcat = cat;
               y = this.row(c, module, s.name + " Type", x, y, w, row++, (cxx, cy, cw) -> {
                  this.dropdown(c, module + "/" + s.name + "/type", cxx, cy, cw, new ArrayList<>(groups.keySet()), fcat, v -> {
                     List<String> entriesx = groups.get(v);
                     if (entriesx != null && !entriesx.contains(str(this.get(module, s.name), "")) && !entriesx.isEmpty()) {
                        this.set(module, s.name, entriesx.get(0));
                     }
                  });
                  return 28.0F;
               });
               List<String> entries = groups.getOrDefault(cat, List.of());
               y = this.row(c, module, s.name, x, y, w, row++, (cxx, cy, cw) -> {
                  this.dropdown(c, module + "/" + s.name, cxx, cy, cw, entries, entries.contains(cur) ? cur : (entries.isEmpty() ? cur : entries.get(0)), v -> this.set(module, s.name, v));
                  return 28.0F;
               });
            } else {
               switch (s.type) {
                  case "multiselect":
                     y = this.drawMulti(c, module, s, x, y, w, row++);
                     break;
                  case "blockpicker":
                  case "itempicker":
                     y = this.drawIdList(c, module, s, x, y, w, row++);
                     break;
                  default:
                     y = this.row(c, module, s.name, x, y, w, row++, (cxx, cy, cw) -> this.control(c, module, s, cxx, cy, cw));
               }
            }
         }
      }

      return y;
   }

   private float row(Canvas c, String module, String label, float x, float y, float w, int idx, ClickGui.Ctl ctl) {
      float rowH = 38.0F;
      float cw = Math.min(270.0F, w * 0.5F);
      float hv = this.an.to("rowHover:" + module + "/" + label, this.in(x - 8.0F, y, w + 16.0F, rowH) && this.pop == null ? 1.0F : 0.0F, 16.0F);
      if (hv > 0.01F) {
         c.rect(x - 8.0F, y + 2.0F, w + 16.0F, rowH - 4.0F, 9.0F, Theme.a(this.th.text, 0.035F * hv));
      }

      String l = this.ellipsize(c, "inter-med", 12.5F, label, w - cw - 16.0F);
      c.text("inter-med", 12.5F, x, y + rowH / 2.0F, 17, Theme.a(this.th.text, 0.88F), l);
      float ch = ctl.draw(x + w - cw, y + rowH / 2.0F, cw);
      return y + Math.max(rowH, ch + 10.0F);
   }

   private float control(Canvas c, String module, GuiData.Setting s, float cx, float cy, float cw) {
      String id = module + "/" + s.name;
      if (isModelFile(module, s)) {
         this.dropdown(c, id, cx, cy, cw, this.modelFileOptions(), this.modelFileValue(module, s), v -> this.pickModelFile(module, s.name, v));
         return 28.0F;
      }

      if (isSoundSelect(module, s)) {
         this.dropdown(c, id, cx, cy, cw, this.soundOptions(s), str(this.get(module, s.name), String.valueOf(s.defaultValue())), v -> this.pickSound(module, s.name, v));
         return 28.0F;
      }

      switch (s.type) {
         case "check":
            boolean v = bool(this.get(module, s.name), (Boolean)s.defaultValue());
            float sx = cx + cw - 38.0F;
            this.drawSwitch(c, "sw:" + id, sx, cy - 11.0F, 38.0F, 22.0F, v);
            this.hit(sx - 4.0F, cy - 15.0F, 46.0F, 30.0F, () -> this.set(module, s.name, !v), null);
            return 22.0F;
         case "range":
            this.slider(c, id, module, s, cx, cy, cw);
            return 22.0F;
         case "minmax":
            this.minmax(c, id, module, s, cx, cy, cw);
            return 22.0F;
         case "select": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            this.dropdown(c, id, cx, cy, cw, s.options(), cur, vx -> this.set(module, s.name, vx));
            return 28.0F;
         }
         case "skinselect":
         case "capeselect": {
            List<String> opts = new ArrayList<>(List.of("none"));
            if ("skinselect".equals(s.type)) {
               opts.add("default");
               opts.addAll(this.store.customSkins());
               opts.add("__add_skin__");
            } else {
               opts.addAll(this.data.capes);
               opts.addAll(this.store.customCapes());
            }

            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            this.dropdown(c, id, cx, cy, cw, opts, cur, vx -> this.pickAsset(module, s.name, vx));
            return 28.0F;
         }
         case "color":
            this.colorSwatch(c, id, module, s, cx, cy, cw);
            return 24.0F;
         case "text":
            this.textInput(c, id, cx, cy, cw, str(this.get(module, s.name), String.valueOf(s.defaultValue())), s.placeholder(), vx -> this.set(module, s.name, vx));
            return 28.0F;
         case "keybind": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            boolean listening = module.equals(this.listenModule) && s.name.equals(this.listenSetting);
            String lbl = listening ? "Press a key…" : (!"NONE".equals(cur) && !cur.isEmpty() ? cur : "None");
            float bw = Math.max(90.0F, c.textWidth("inter-med", 11.5F, lbl) + 24.0F);
            float bx = cx + cw - bw;
            float lt = listening ? 0.5F + 0.5F * (float)Math.sin(this.time * 8.0F) : 0.0F;
            c.rect(bx, cy - 13.0F, bw, 26.0F, this.ctlR() - 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.accent, listening ? 0.25F : 0.06F), 0.85F));
            c.stroke(bx, cy - 13.0F, bw, 26.0F, this.ctlR() - 1.0F, 1.0F, listening ? Theme.a(this.th.accent, 0.5F + 0.5F * lt) : Theme.a(this.th.border, 0.9F));
            c.text("inter-med", 11.5F, bx + bw / 2.0F, cy, 18, listening ? this.th.accent : this.th.text, lbl);
            this.hit(bx, cy - 13.0F, bw, 26.0F, () -> {
               this.listenModule = module;
               this.listenSetting = s.name;
            }, () -> this.set(module, s.name, "NONE"));
            return 26.0F;
         }
         default:
            c.text("inter", 11.0F, cx + cw, cy, 20, this.th.muted, "Edit in the web GUI");
            return 20.0F;
      }
   }

   private void drawSwitch(Canvas c, String key, float x, float y, float w, float h, boolean on) {
      float t = this.an.spring(key, on ? 1.0F : 0.0F, 340.0F, 24.0F);
      float tc = Math.max(0.0F, Math.min(1.0F, t));
      float hov = this.in(x, y, w, h) ? 1.0F : 0.0F;
      String style = this.gstr("Switch Style", "Pill");
      int off = Theme.a(Theme.mix(this.th.bg, this.th.text, 0.12F), 0.9F);
      switch (style) {
         case "Square": {
            float r = 4.0F;
            if (tc > 0.01F) {
               c.shadow(x, y, w, h, r, 8.0F, Theme.a(this.th.accent, 0.3F * tc));
            }

            c.rect(x, y, w, h, r, Theme.mix(off, this.th.accent, tc));
            float k = h - 6.0F;
            float kx = x + 3.0F + (w - 6.0F - k) * t;
            c.shadow(kx, y + 3.0F, k, k, 3.0F, 5.0F, Theme.a(-16777216, 0.3F));
            c.rect(kx, y + 3.0F, k, k, 3.0F, -1);
            break;
         }
         case "Minimal": {
            float th2 = Math.max(4.0F, h * 0.38F);
            c.rect(x + 2.0F, y + (h - th2) / 2.0F, w - 4.0F, th2, th2 / 2.0F, Theme.mix(off, Theme.a(this.th.accent, 0.55F), tc));
            float r = h / 2.0F - 1.0F;
            float kx = x + r + (w - 2.0F * r) * t;
            c.shadow(kx - r, y + 1.0F, 2.0F * r, 2.0F * r, r, 6.0F, Theme.a(-16777216, 0.3F));
            c.circle(kx, y + h / 2.0F, r, Theme.mix(-1, this.th.accent, tc));
            c.circle(kx, y + h / 2.0F, r * 0.42F * (1.0F + 0.15F * hov), Theme.mix(Theme.a(this.th.bg, 0.6F), -1, tc));
            break;
         }
         default: {
            int track = Theme.mix(off, this.th.accent, tc);
            if (tc > 0.01F) {
               c.shadow(x, y, w, h, h / 2.0F, 10.0F, Theme.a(this.th.accent, 0.35F * tc));
            }

            c.rect(x, y, w, h, h / 2.0F, track);
            if (tc > 0.01F) {
               c.gradient(x, y, w, h, h / 2.0F, Theme.a(this.th.accent2, 0.0F), Theme.a(this.th.accent2, 0.55F * tc), true);
            }

            float r = h / 2.0F - 3.0F;
            float kx = x + 3.0F + r + (w - 6.0F - 2.0F * r) * t;
            float squash = 1.0F + 0.18F * hov;
            c.shadow(kx - r, y + 3.0F, 2.0F * r, 2.0F * r, r, 6.0F, Theme.a(-16777216, 0.35F));
            c.circle(kx, y + h / 2.0F, r * (0.96F + 0.04F * squash), -1);
            if (tc > 0.5F) {
               Icons.draw(c, "check", kx, y + h / 2.0F, r * 1.1F, Theme.a(this.th.accent, (tc - 0.5F) * 2.0F));
            }
         }
      }
   }

   private void slider(Canvas c, String id, String module, GuiData.Setting s, float cx, float cy, float cw) {
      double min = s.num(0, 0.0);
      double max = s.num(1, 1.0);
      double def = s.num(2, min);
      double step = s.num(3, 0.0);
      double v = num(this.get(module, s.name), def);
      String style = this.gstr("Slider Style", "Pill");
      float boxW = "Classic".equals(style) ? 50.0F : 46.0F;
      float tx = cx;
      float tw = cw - boxW - 12.0F;
      float frac = (float)((v - min) / Math.max(1.0E-9, max - min));
      float shown = this.an.to("sl:" + id, frac, 22.0F);
      boolean dragging = this.drag != null && id.equals(this.dragId);
      float hv = this.an.to("slh:" + id, !this.in(tx - 6.0F, cy - 12.0F, tw + 12.0F, 24.0F) && !dragging ? 0.0F : 1.0F, 16.0F);
      String txt = fmt(v, step);
      int steps = step > 0.0 ? (int)Math.round((max - min) / step) : 0;
      switch (style) {
         case "Minimal": {
            float hh = 3.0F + hv;
            c.rect(tx, cy - hh / 2.0F, tw, hh, hh / 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.16F), 0.9F));
            c.rect(tx, cy - hh / 2.0F, Math.max(hh, tw * shown), hh, hh / 2.0F, this.th.accent);
            float kx = tx + tw * shown;
            float kr = 5.0F + 1.5F * hv;
            c.circle(kx, cy, kr, this.th.bg | 0xFF000000);
            c.ring(kx, cy, kr, 2.0F, this.th.accent);
            break;
         }
         case "Classic": {
            c.rect(tx, cy - 3.0F, tw, 6.0F, 3.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.14F), 0.9F));
            c.gradient(tx, cy - 3.0F, Math.max(6.0F, tw * shown), 6.0F, 3.0F, this.th.accent2, this.th.accent, true);
            if (hv > 0.01F || dragging) {
               c.shadow(tx, cy - 3.0F, tw * shown, 6.0F, 3.0F, 10.0F, Theme.a(this.th.accent, 0.4F * hv));
            }

            float kx = tx + tw * shown;
            float kr = 7.0F + 2.0F * hv;
            c.glow(kx, cy, kr * 2.4F, Theme.a(this.th.accent, 0.35F * hv));
            c.circle(kx, cy, kr, -1);
            c.ring(kx, cy, kr - 1.0F, 2.0F, this.th.accent);
            break;
         }
         default: {
            float hh = 10.0F + 2.0F * hv;
            float r = hh / 2.0F;
            float top = cy - hh / 2.0F;
            c.rect(tx, top, tw, hh, r, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.1F), 0.92F));
            c.stroke(tx, top, tw, hh, r, 1.0F, Theme.a(this.th.border, 0.5F));
            float fw = Math.max(hh, tw * shown);
            if (hv > 0.01F) {
               c.shadow(tx, top, fw, hh, r, 10.0F, Theme.a(this.th.accent, 0.35F * hv));
            }

            c.gradient(tx, top, fw, hh, r, this.th.accent2, this.th.accent, true);
            c.gradient(tx, top, fw, hh / 2.0F, r, Theme.a(-1, 0.22F), Theme.a(-1, 0.0F), false);
            if (steps > 1 && steps <= 12) {
               for (int k = 1; k < steps; k++) {
                  float px = tx + tw * k / steps;
                  boolean onFill = px < tx + fw - r;
                  c.circle(px, cy, 1.2F, onFill ? Theme.a(-1, 0.45F) : Theme.a(this.th.text, 0.22F));
               }
            }

            float kx = tx + fw - r;
            c.circle(kx, cy, r - 2.5F, -1);
            if (hv > 0.02F) {
               float bw = c.textWidth("inter-sb", 10.5F, txt) + 14.0F;
               float bh = 18.0F;
               float by = top - 6.0F - bh - 3.0F * hv;
               float bx = Math.max(tx - 4.0F, Math.min(tx + tw - bw + 4.0F, kx - bw / 2.0F));
               c.save();
               c.alpha(hv);
               c.shadow(bx, by + 2.0F, bw, bh, 9.0F, 8.0F, Theme.a(-16777216, 0.35F));
               c.rect(bx, by, bw, bh, 9.0F, this.th.accent);
               c.text("inter-sb", 10.5F, bx + bw / 2.0F, by + bh / 2.0F, 18, Theme.onColor(this.th.accent), txt);
               c.restore();
            }
         }
      }

      float bx = cx + cw - boxW;
      boolean editing = this.focused != null && id.equals(this.focusId);
      if (!"Classic".equals(style) && !editing) {
         float vh = this.an.to("slv:" + id, this.in(bx, cy - 11.0F, boxW, 22.0F) ? 1.0F : 0.0F, 16.0F);
         c.text("inter-sb", 11.5F, bx + boxW, cy, 20, Theme.mix(Theme.a(this.th.text, 0.85F), this.th.accent, Math.max(vh, hv * 0.6F)), txt);
      } else {
         c.rect(bx, cy - 11.0F, boxW, 22.0F, Math.max(0.0F, this.ctlR() - 2.0F), Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F), 0.85F));
         c.stroke(bx, cy - 11.0F, boxW, 22.0F, Math.max(0.0F, this.ctlR() - 2.0F), 1.0F, editing ? this.th.accent : Theme.a(this.th.border, 0.8F));
         c.text("inter-med", 11.0F, bx + boxW / 2.0F, cy, 18, this.th.text, editing ? this.focused.text : txt);
      }

      this.hit(bx, cy - 11.0F, boxW, 22.0F, () -> {
         TextField f = new TextField(12);
         f.set(txt);
         f.allSelected = true;
         this.focusId = id;
         this.focus(f, () -> {
            try {
               double nv = Double.parseDouble(f.text.trim());
               this.set(module, s.name, snap(Math.max(min, Math.min(max, nv)), step, min));
            } catch (NumberFormatException var12x) {
            }
         });
      }, null);
      Consumer<Float> apply = px -> {
         double t = Math.max(0.0F, Math.min(1.0F, (px - tx) / tw));
         this.sliderFrac = (float)t;
         this.set(module, s.name, snap(min + t * (max - min), step, min));
      };
      this.hit(tx - 8.0F, cy - 12.0F, tw + 16.0F, 24.0F, () -> {
         this.dragId = id;
         this.drag = apply;
         apply.accept(this.wmx);
      }, null);
   }

   private void minmax(Canvas c, String id, String module, GuiData.Setting s, float cx, float cy, float cw) {
      double min = s.num(0, 0.0);
      double max = s.num(1, 1.0);
      double step = s.num(4, 0.0);
      double lo = num(this.get(module, s.name + "_lo"), s.num(2, min));
      double hi = num(this.get(module, s.name + "_hi"), s.num(3, max));
      float labelW = 74.0F;
      float tx = cx;
      float tw = cw - labelW - 10.0F;
      float a = (float)((lo - min) / Math.max(1.0E-9, max - min));
      float b = (float)((hi - min) / Math.max(1.0E-9, max - min));
      float sa = this.an.to("mmA:" + id, a, 22.0F);
      float sb = this.an.to("mmB:" + id, b, 22.0F);
      if ("Classic".equals(this.gstr("Slider Style", "Pill"))) {
         c.rect(tx, cy - 3.0F, tw, 6.0F, 3.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.14F), 0.9F));
         c.gradient(tx + tw * sa, cy - 3.0F, Math.max(4.0F, tw * (sb - sa)), 6.0F, 3.0F, this.th.accent2, this.th.accent, true);

         for (float k : new float[]{sa, sb}) {
            float kx = tx + tw * k;
            c.circle(kx, cy, 7.0F, -1);
            c.ring(kx, cy, 6.0F, 2.0F, this.th.accent);
         }
      } else {
         boolean minimal = "Minimal".equals(this.gstr("Slider Style", "Pill"));
         float hh = minimal ? 3.0F : 10.0F;
         float r = hh / 2.0F;
         c.rect(tx, cy - r, tw, hh, r, Theme.a(Theme.mix(this.th.bg, this.th.text, minimal ? 0.16F : 0.1F), 0.92F));
         float x0 = tx + tw * sa;
         float x1 = tx + tw * sb;
         if (minimal) {
            c.rect(x0, cy - r, Math.max(hh, x1 - x0), hh, r, this.th.accent);
         } else {
            c.gradient(x0 - r, cy - r, Math.max(hh, x1 - x0 + hh), hh, r, this.th.accent2, this.th.accent, true);
            c.gradient(x0 - r, cy - r, Math.max(hh, x1 - x0 + hh), r, r, Theme.a(-1, 0.22F), Theme.a(-1, 0.0F), false);
         }

         for (float kx : new float[]{x0, x1}) {
            if (minimal) {
               c.circle(kx, cy, 5.0F, this.th.bg | 0xFF000000);
               c.ring(kx, cy, 5.0F, 2.0F, this.th.accent);
            } else {
               c.circle(kx, cy, r - 2.5F, -1);
            }
         }
      }

      c.text("inter-med", 11.0F, cx + cw, cy, 20, this.th.text, fmt(lo, step) + " – " + fmt(hi, step));
      this.hit(tx - 8.0F, cy - 12.0F, tw + 16.0F, 24.0F, () -> {
         double t0 = Math.max(0.0F, Math.min(1.0F, (this.wmx - tx) / tw));
         int[] grab = new int[]{minmaxGrab(t0, a, b)};
         this.dragId = id;
         this.drag = px -> {
            double t = Math.max(0.0F, Math.min(1.0F, (px - tx) / tw));
            this.sliderFrac = (float)t;
            double v = snap(min + t * (max - min), step, min);
            double curLo = num(this.get(module, s.name + "_lo"), s.num(2, min));
            double curHi = num(this.get(module, s.name + "_hi"), s.num(3, max));
            grab[0] = this.minmaxApply(module, s.name, v, curLo, curHi, grab[0]);
         };
         this.drag.accept(this.wmx);
      }, null);
   }

   private static int minmaxGrab(double t, double a, double b) {
      if (Math.abs(a - b) < 1.0E-6) {
         if (t > b + 1.0E-6) {
            return 1;
         } else {
            return t < a - 1.0E-6 ? -1 : 0;
         }
      } else {
         return Math.abs(t - a) <= Math.abs(t - b) ? -1 : 1;
      }
   }

   private int minmaxApply(String module, String name, double v, double lo, double hi, int grab) {
      if (grab == 0) {
         if (v > hi) {
            grab = 1;
         } else {
            if (!(v < lo)) {
               return 0;
            }

            grab = -1;
         }
      }

      if (grab < 0) {
         if (v > hi) {
            this.set(module, name + "_lo", hi);
            this.set(module, name + "_hi", v);
            return 1;
         }

         this.set(module, name + "_lo", v);
      } else {
         if (v < lo) {
            this.set(module, name + "_hi", lo);
            this.set(module, name + "_lo", v);
            return -1;
         }

         this.set(module, name + "_hi", v);
      }

      return grab;
   }

   private void dropdown(Canvas c, String id, float cx, float cy, float cw, List<String> options, String cur, Consumer<String> pick) {
      float h = 28.0F;
      float x = cx;
      float y = cy - h / 2.0F;
      boolean isOpen = this.pop != null && !this.pop.color && id.equals(this.pop.id);
      float hv = this.an.to("ddh:" + id, !this.in(x, y, cw, h) && !isOpen ? 0.0F : 1.0F, 16.0F);
      c.rect(x, y, cw, h, this.ctlR(), Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F + 0.04F * hv), 0.85F));
      c.stroke(x, y, cw, h, this.ctlR(), 1.0F, isOpen ? this.th.accent : Theme.mix(Theme.a(this.th.border, 0.9F), Theme.a(this.th.accent, 0.6F), hv));
      c.text("inter-med", 12.0F, x + 12.0F, cy, 17, this.th.text, this.ellipsize(c, "inter-med", 12.0F, display(cur), cw - 36.0F));
      float rot = this.an.to("ddr:" + id, isOpen ? 1.0F : 0.0F, 14.0F);
      c.save();
      c.translate(x + cw - 16.0F, cy);
      Icons.draw(c, rot > 0.5F ? "down" : "down", 0.0F, (rot - 0.5F) * -2.0F, 12.0F, Theme.mix(this.th.muted, this.th.accent, rot));
      c.restore();
      this.hit(x, y, cw, h, () -> {
         if (isOpen) {
            this.pop = null;
         } else {
            this.openList(id, x, y, cw, h, 170.0F, options, cur, pick);
         }
      }, null);
   }

   private void openList(String id, float x, float y, float cw, float h, float minW, List<String> options, String cur, Consumer<String> pick) {
      ClickGui.Pop p = new ClickGui.Pop(id, false);
      p.options = options;
      p.current = cur;
      p.pick = pick;
      if (this.compact()) {
         float rh = this.setRowH();
         int longest = 0;

         for (String o : options) {
            longest = Math.max(longest, display(o).length());
         }

         float est = longest * (this.pFont() - 1.0F) * 0.56F + 30.0F;
         p.w = Math.max(cw, Math.min(230.0F, est));
         p.x = Math.max(this.wx + 4.0F, Math.min(x, this.wx + this.ww - p.w - 4.0F));
         boolean filter = options.size() > 10;
         p.h = Math.min(options.size(), 10) * rh + 4.0F + (filter ? rh + 4.0F : 0.0F);
         p.y = y + h + p.h < this.wy + this.wh - 4.0F ? y + h : Math.max(this.wy + 4.0F, y - p.h);
         int ci = options.indexOf(cur);
         if (ci > 5 && options.size() > 10) {
            p.scrollT = (ci - 5) * rh;
            p.scrollY = p.scrollT;
         }

         this.pop = p;
         if (filter) {
            this.focus(p.filter, null);
         }
      } else {
         p.w = Math.max(cw, minW);
         p.x = Math.max(this.wx + 6.0F, Math.min(x + cw - p.w, this.wx + this.ww - p.w - 6.0F));
         float listH = Math.min(options.size(), 9) * 28 + 10 + (options.size() > 9 ? 36 : 0);
         p.h = listH;
         float below = this.wy + this.wh - (y + h + 6.0F);
         p.y = !(below > listH) && !(y - this.wy < listH) ? y - 6.0F - listH : y + h + 6.0F;
         int ci = options.indexOf(cur);
         if (ci > 4 && options.size() > 9) {
            p.scrollT = (ci - 4) * 28;
            p.scrollY = p.scrollT;
         }

         this.pop = p;
         if (options.size() > 9) {
            this.focus(p.filter, null);
         }
      }
   }

   private void colorSwatch(Canvas c, String id, String module, GuiData.Setting s, float cx, float cy, float cw) {
      String v = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
      int col = this.resolveColor(v);
      float sw_ = 44.0F;
      float x = cx + cw - sw_;
      boolean isOpen = this.pop != null && this.pop.color && id.equals(this.pop.id);
      c.shadow(x, cy - 11.0F, sw_, 22.0F, 7.0F, 8.0F, Theme.a(col | 0xFF000000, 0.45F));
      c.rect(x, cy - 11.0F, sw_, 22.0F, 7.0F, 0xFF000000 | col);
      c.stroke(x, cy - 11.0F, sw_, 22.0F, 7.0F, 1.2F, isOpen ? -1 : Theme.a(-1, 0.25F));
      String label = "theme".equals(v) ? "Theme accent" : ("theme2".equals(v) ? "Theme accent 2" : v.toUpperCase(Locale.ROOT));
      c.text("inter-med", 11.0F, x - 10.0F, cy, 20, this.th.muted, label);
      this.hit(x, cy - 11.0F, sw_, 22.0F, () -> {
         if (isOpen) {
            this.pop = null;
         } else {
            this.openColor(id, module, s.name, x + sw_, cy, 16.0F, col);
         }
      }, null);
   }

   private void openColor(String id, String module, String key, float right, float cy, float gap, int col) {
      ClickGui.Pop p = new ClickGui.Pop(id, true);
      p.module = module;
      p.key = key;
      p.w = 236.0F;
      p.h = 282.0F;
      if (this.tabsMode()) {
         p.w = 214.0F;
         p.h = this.tcpHeight();
      }

      p.x = Math.max(this.wx + 6.0F, Math.min(right - p.w, this.wx + this.ww - p.w - 6.0F));
      p.y = cy + gap + p.h < this.wy + this.wh - 8.0F ? cy + gap : Math.max(this.wy + 6.0F, cy - gap - p.h);
      p.hsv = Theme.rgbToHsv(col);
      p.filter.set(Theme.toHex(col));
      this.pop = p;
   }

   private void textInput(Canvas c, String id, float cx, float cy, float cw, String value, String placeholder, Consumer<String> commit) {
      float h = 28.0F;
      float x = cx;
      float y = cy - h / 2.0F;
      boolean editing = this.focused != null && id.equals(this.focusId);
      float ft = this.an.to("tf:" + id, editing ? 1.0F : 0.0F, 14.0F);
      c.rect(x, y, cw, h, this.ctlR(), Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.85F));
      c.stroke(x, y, cw, h, this.ctlR(), 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, ft));
      if (ft > 0.01F) {
         c.shadow(x, y, cw, h, this.ctlR(), 10.0F, Theme.a(this.th.accent, 0.25F * ft));
      }

      c.save();
      c.scissor(x + 8.0F, y, cw - 16.0F, h);
      String t = editing ? this.focused.text : value;
      if (t.isEmpty() && !editing) {
         c.text("inter", 12.0F, x + 11.0F, cy, 17, this.th.muted, placeholder.isEmpty() ? "Type…" : placeholder);
      } else {
         c.text("inter", 12.0F, x + 11.0F, cy, 17, this.th.text, t);
      }

      if (editing) {
         this.caret(c, "inter", 12.0F, x + 11.0F, cy, this.focused);
      }

      c.restore();
      this.hit(x, y, cw, h, () -> {
         TextField f = new TextField(64);
         f.set(value);
         this.focusId = id;
         this.focus(f, () -> commit.accept(f.text));
      }, null);
   }

   private float drawMulti(Canvas c, String module, GuiData.Setting s, float x, float y, float w, int idx) {
      List<String> opts = s.options();
      List<String> cur = list(this.get(module, s.name), s.defaultValue());
      c.text("inter-med", 12.5F, x, y + 18.0F, 17, Theme.a(this.th.text, 0.88F), s.name);
      float cxp = x;
      float cyp = y + 34.0F;

      for (String o : opts) {
         float bw = c.textWidth("inter-med", 11.5F, o) + 26.0F;
         if (cxp + bw > x + w) {
            cxp = x;
            cyp += 32.0F;
         }

         boolean on = cur.contains(o);
         float t = this.an.to("ms:" + module + "/" + s.name + "/" + o, on ? 1.0F : 0.0F, 14.0F);
         float hv = this.in(cxp, cyp, bw, 26.0F) ? 1.0F : 0.0F;
         c.rect(cxp, cyp, bw, 26.0F, 13.0F, Theme.mix(Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F + 0.04F * hv), 0.85F), Theme.a(this.th.accent, 0.25F), t));
         c.stroke(cxp, cyp, bw, 26.0F, 13.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, t));
         c.text("inter-med", 11.5F, cxp + bw / 2.0F, cyp + 13.0F, 18, Theme.mix(this.th.muted, this.th.text, t), o);
         this.hit(cxp, cyp, bw, 26.0F, () -> {
            List<String> next = new ArrayList<>(list(this.get(module, s.name), s.defaultValue()));
            if (!next.remove(o)) {
               next.add(o);
            }

            this.set(module, s.name, next);
         }, null);
         cxp += bw + 8.0F;
      }

      return cyp + 36.0F;
   }

   private float drawIdList(Canvas c, String module, GuiData.Setting s, float x, float y, float w, int idx) {
      return this.row(c, module, s.name, x, y, w, idx, (cxx, cy, cw) -> {
         this.idDropdown(c, module, s, cxx, cy, cw);
         return 28.0F;
      });
   }

   private void idDropdown(Canvas c, String module, GuiData.Setting s, float cx, float cy, float cw) {
      String id = "ids:" + module + "/" + s.name;
      float h = 28.0F;
      float x = cx;
      float y = cy - h / 2.0F;
      boolean isOpen = this.pop != null && this.pop.ids && id.equals(this.pop.id);
      float hv = this.an.to("ddh:" + id, !this.in(x, y, cw, h) && !isOpen ? 0.0F : 1.0F, 16.0F);
      c.rect(x, y, cw, h, this.ctlR(), Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F + 0.04F * hv), 0.85F));
      c.stroke(x, y, cw, h, this.ctlR(), 1.0F, isOpen ? this.th.accent : Theme.mix(Theme.a(this.th.border, 0.9F), Theme.a(this.th.accent, 0.6F), hv));
      List<String> cur = list(this.get(module, s.name), List.of());
      float tx = x + 10.0F;
      int shown = 0;

      for (String v : cur) {
         if (shown == 4) {
            break;
         }

         this.itemIcon(c, bare(v), tx, cy - 8.0F, 16.0F);
         tx += 19.0F;
         shown++;
      }

      if (shown > 0) {
         tx += 3.0F;
      }

      String label = cur.isEmpty() ? "None" : (shown < cur.size() ? "+" + (cur.size() - shown) : (cur.size() == 1 ? this.displayId(cur.get(0)) : ""));
      if (!label.isEmpty()) {
         c.text("inter-med", 12.0F, tx, cy, 17, cur.isEmpty() ? this.th.muted : this.th.text, this.ellipsize(c, "inter-med", 12.0F, label, x + cw - 30.0F - tx));
      }

      float rot = this.an.to("ddr:" + id, isOpen ? 1.0F : 0.0F, 14.0F);
      c.save();
      c.translate(x + cw - 16.0F, cy);
      Icons.draw(c, "down", 0.0F, (rot - 0.5F) * -2.0F, 12.0F, Theme.mix(this.th.muted, this.th.accent, rot));
      c.restore();
      this.hit(x, y, cw, h, () -> {
         if (isOpen) {
            this.pop = null;
            this.focused = null;
         } else {
            this.openIds(id, module, s, x, y, cw, h);
         }
      }, null);
   }

   private void drawPop(Canvas c) {
      if (this.tabsMode()) {
         this.drawTabsPop(c);
      } else if (this.panelMode()) {
         this.drawPanelPop(c);
      } else {
         ClickGui.Pop p = this.pop;
         float t = Anim.easeOutBack(Math.min(1.0F, (this.time - p.openedAt) * this.an_speed() / 0.22F));
         this.layer = 1;
         this.noClip();
         c.save();
         float ox = p.x + p.w / 2.0F;
         float oy = p.y;
         c.alpha(Math.min(1.0F, t * 1.4F));
         c.translate(ox, oy);
         c.scale(0.92F + 0.08F * t);
         c.translate(-ox, -oy + (1.0F - t) * -6.0F);
         c.shadow(p.x, p.y + 8.0F, p.w, p.h, 12.0F, 30.0F, Theme.a(-16777216, 0.55F));
         c.glass(p.x, p.y, p.w, p.h, 12.0F, 1.0F);
         c.rect(p.x, p.y, p.w, p.h, 12.0F, Theme.a(Theme.mix(this.th.surface, this.th.bg, 0.3F), 0.94F));
         c.stroke(p.x, p.y, p.w, p.h, 12.0F, 1.0F, Theme.a(this.th.accent, 0.45F));
         this.hit(p.x, p.y, p.w, p.h, () -> {}, () -> {});
         if (p.color) {
            this.drawColorPop(c, p);
         } else if (p.ids) {
            this.drawIdsPop(c, p);
         } else {
            this.drawListPop(c, p);
         }

         c.restore();
         this.layer = 0;
      }
   }

   private void drawPanelPop(Canvas c) {
      ClickGui.Pop p = this.pop;
      float t = Anim.easeOutCubic(Math.min(1.0F, (this.time - p.openedAt) * this.an_speed() / 0.16F));
      this.layer = 1;
      this.noClip();
      float R = Math.min(this.gnum("Panel Rounding", 3.0), 6.0F);
      c.save();
      c.alpha(Math.min(1.0F, t * 1.6F));
      c.scissor(p.x - 30.0F, p.y - 30.0F, p.w + 60.0F, 30.0F + p.h * t + (t >= 1.0F ? 30 : 0));
      if (this.gbool("Panel Shadow", true)) {
         c.shadow(p.x, p.y + 3.0F, p.w, p.h, R, 14.0F, Theme.a(-16777216, 0.55F));
      }

      c.rect(p.x, p.y, p.w, p.h, R, Theme.mix(this.th.bg, -16777216, 0.3F) | 0xFF000000);
      c.rect(p.x, p.y, p.w, 1.5F, 0.0F, this.th.accent);
      c.stroke(p.x, p.y, p.w, p.h, R, 1.0F, Theme.a(this.th.accent, 0.25F));
      this.hit(p.x, p.y, p.w, p.h, () -> {}, () -> {});
      if (p.color) {
         this.drawColorPop(c, p);
      } else if (p.ids) {
         this.drawIdsPop(c, p);
      } else {
         this.drawPanelListPop(c, p);
      }

      c.restore();
      this.layer = 0;
   }

   private void drawPanelListPop(Canvas c, ClickGui.Pop p) {
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      List<String> opts = p.options;
      float top = p.y + 2.0F;
      if (opts.size() > 10) {
         float fy = p.y + 3.0F;
         boolean f = this.focused == p.filter;
         c.rect(p.x + 3.0F, fy, p.w - 6.0F, rh, 2.0F, Theme.a(this.th.text, f ? 0.08F : 0.05F));
         Icons.draw(c, "search", p.x + 11.0F, fy + rh / 2.0F, Math.min(10.0F, rh - 5.0F), f ? this.th.accent : this.th.muted);
         String ft = p.filter.text;
         c.text("inter", fs, p.x + 20.0F, fy + rh / 2.0F, 17, ft.isEmpty() ? this.th.muted : this.th.text, ft.isEmpty() ? "Filter…" : ft);
         if (f) {
            this.caret(c, "inter", fs, p.x + 20.0F, fy + rh / 2.0F, p.filter);
         }

         this.hit(p.x + 3.0F, fy, p.w - 6.0F, rh, () -> this.focus(p.filter, null), null);
         top = fy + rh + 3.0F;
         String q = ft.toLowerCase(Locale.ROOT);
         if (!q.isEmpty()) {
            List<String> fl = new ArrayList<>();

            for (String o : opts) {
               if (display(o).toLowerCase(Locale.ROOT).contains(q)) {
                  fl.add(o);
               }
            }

            opts = fl;
         }
      }

      float viewH = p.y + p.h - 2.0F - top;
      float maxScroll = Math.max(0.0F, opts.size() * rh - viewH);
      p.scrollT = Math.max(0.0F, Math.min(maxScroll, p.scrollT));
      p.scrollY = p.scrollY + (p.scrollT - p.scrollY) * 0.35F;
      c.save();
      c.scissor(p.x, top, p.w, viewH);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sH = this.clipH;
      this.clip(p.x, top, p.w, viewH);
      float y = top - p.scrollY;

      for (String o : opts) {
         if (y + rh >= top && y <= top + viewH) {
            boolean sel = o.equals(p.current);
            float hv = this.an.to("ppo:" + p.id + "/" + o, this.in(p.x, y, p.w, rh) ? 1.0F : 0.0F, 22.0F);
            if (hv > 0.01F) {
               c.rect(p.x + 1.0F, y, p.w - 2.0F, rh, 0.0F, Theme.a(this.th.text, 0.06F * hv));
            }

            if (sel) {
               c.rect(p.x + 2.0F, y + 3.0F, 2.0F, rh - 6.0F, 1.0F, this.th.accent);
            }

            if (this.gbool("Row Dividers", true)) {
               c.line(p.x + 4.0F, y + rh, p.x + p.w - 4.0F, y + rh, 1.0F, Theme.a(this.th.text, 0.04F));
            }

            c.text(
               sel ? "inter-sb" : "inter-med",
               fs,
               p.x + 9.0F + 1.5F * hv,
               y + rh / 2.0F,
               17,
               sel ? this.th.accent : Theme.a(this.th.text, 0.78F + 0.22F * hv),
               this.ellipsize(c, "inter-med", fs, display(o), p.w - 18.0F)
            );
            String fo = o;
            this.hit(p.x, y, p.w, rh, () -> {
               p.pick.accept(fo);
               this.pop = null;
               this.focused = null;
            }, null);
         }

         y += rh;
      }

      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sH;
      c.restore();
      if (maxScroll > 0.0F) {
         float barH = Math.max(12.0F, viewH * viewH / (viewH + maxScroll));
         c.rect(p.x + p.w - 3.0F, top + (viewH - barH) * (p.scrollY / maxScroll), 2.0F, barH, 1.0F, Theme.a(this.th.accent, 0.6F));
      }

      if (opts.isEmpty()) {
         c.text("inter", fs, p.x + p.w / 2.0F, top + rh / 2.0F + 2.0F, 18, this.th.muted, "No matches");
      }
   }

   private void drawListPop(Canvas c, ClickGui.Pop p) {
      List<String> opts = p.options;
      float top = p.y + 5.0F;
      if (opts.size() > 9) {
         float fx = p.x + 8.0F;
         float fy = p.y + 8.0F;
         float fw = p.w - 16.0F;
         c.rect(fx, fy, fw, 26.0F, 8.0F, Theme.a(this.th.bg, 0.6F));
         Icons.draw(c, "search", fx + 13.0F, fy + 13.0F, 11.0F, this.th.muted);
         String ft = p.filter.text;
         c.text("inter", 11.5F, fx + 24.0F, fy + 13.0F, 17, ft.isEmpty() ? this.th.muted : this.th.text, ft.isEmpty() ? "Filter…" : ft);
         if (this.focused == p.filter) {
            this.caret(c, "inter", 11.5F, fx + 24.0F, fy + 13.0F, p.filter);
         }

         this.hit(fx, fy, fw, 26.0F, () -> this.focus(p.filter, null), null);
         top = p.y + 40.0F;
         String q = ft.toLowerCase(Locale.ROOT);
         if (!q.isEmpty()) {
            List<String> f = new ArrayList<>();

            for (String o : opts) {
               if (display(o).toLowerCase(Locale.ROOT).contains(q)) {
                  f.add(o);
               }
            }

            opts = f;
         }
      }

      float viewH = p.y + p.h - 5.0F - top;
      float maxScroll = Math.max(0.0F, opts.size() * 28 - viewH);
      p.scrollT = Math.max(0.0F, Math.min(maxScroll, p.scrollT));
      p.scrollY = p.scrollY + (p.scrollT - p.scrollY) * 0.35F;
      c.save();
      c.scissor(p.x, top, p.w, viewH);
      float savedX = this.clipX;
      float savedY = this.clipY;
      float savedW = this.clipW;
      float savedH = this.clipH;
      this.clip(p.x, top, p.w, viewH);
      float y = top - p.scrollY;
      int i = 0;

      for (String o : opts) {
         if (y + 28.0F >= top && y <= top + viewH) {
            boolean sel = o.equals(p.current);
            float appear = Anim.easeOutCubic(((this.time - p.openedAt) * this.an_speed() - i * 0.012F) / 0.2F);
            float hv = this.an.to("po:" + p.id + "/" + o, this.in(p.x + 5.0F, y, p.w - 10.0F, 28.0F) ? 1.0F : 0.0F, 20.0F);
            c.save();
            c.alpha(appear);
            c.translate(0.0F, (1.0F - appear) * 4.0F);
            if (sel) {
               c.rect(p.x + 5.0F, y + 2.0F, p.w - 10.0F, 24.0F, 7.0F, Theme.a(this.th.accent, 0.2F));
            }

            if (hv > 0.01F) {
               c.rect(p.x + 5.0F, y + 2.0F, p.w - 10.0F, 24.0F, 7.0F, Theme.a(this.th.text, 0.07F * hv));
            }

            c.text(
               sel ? "inter-sb" : "inter-med",
               12.0F,
               p.x + 16.0F + hv * 3.0F,
               y + 14.0F,
               17,
               sel ? this.th.text : Theme.mix(this.th.muted, this.th.text, 0.4F + 0.6F * hv),
               this.ellipsize(c, "inter-med", 12.0F, display(o), p.w - 48.0F)
            );
            if (sel) {
               Icons.draw(c, "check", p.x + p.w - 20.0F, y + 14.0F, 12.0F, this.th.accent);
            }

            c.restore();
            String fo = o;
            this.hit(p.x + 5.0F, y, p.w - 10.0F, 28.0F, () -> {
               p.pick.accept(fo);
               this.pop = null;
               this.focused = null;
            }, null);
         }

         y += 28.0F;
         i++;
      }

      this.clipX = savedX;
      this.clipY = savedY;
      this.clipW = savedW;
      this.clipH = savedH;
      c.restore();
      if (maxScroll > 0.0F) {
         float barH = Math.max(20.0F, viewH * viewH / (viewH + maxScroll));
         c.rect(p.x + p.w - 5.0F, top + (viewH - barH) * (p.scrollY / maxScroll), 3.0F, barH, 1.5F, Theme.a(this.th.text, 0.25F));
      }

      if (opts.isEmpty()) {
         c.text("inter", 11.5F, p.x + p.w / 2.0F, top + 20.0F, 18, this.th.muted, "No matches");
      }
   }

   private void drawColorPop(Canvas c, ClickGui.Pop p) {
      float x = p.x + 14.0F;
      float y = p.y + 14.0F;
      float w = p.w - 28.0F;
      float[] hsv = p.hsv;
      int hue = 0xFF000000 | Theme.hsvToRgb(hsv[0], 1.0F, 1.0F);
      float sqH = 130.0F;
      c.gradient(x, y, w, sqH, 8.0F, -1, hue, true);
      c.gradient(x, y, w, sqH, 8.0F, 0, -16777216, false);
      float kx = x + hsv[1] * w;
      float ky = y + (1.0F - hsv[2]) * sqH;
      c.ring(kx, ky, 7.0F, 2.5F, -1);
      c.ring(kx, ky, 8.5F, 1.0F, 1711276032);
      this.hit(x, y, w, sqH, () -> {
         this.drag2 = pt -> {
            hsv[1] = Math.max(0.0F, Math.min(1.0F, (pt[0] - x) / w));
            hsv[2] = Math.max(0.0F, Math.min(1.0F, 1.0F - (pt[1] - y) / sqH));
            this.commitColor(p);
         };
         this.drag2.accept(new float[]{this.wmx, this.wmy});
      }, null);
      float hy = y + sqH + 12.0F;
      float hh = 12.0F;

      for (int k = 0; k < 6; k++) {
         int a = 0xFF000000 | Theme.hsvToRgb(k / 6.0F, 1.0F, 1.0F);
         int b = 0xFF000000 | Theme.hsvToRgb((k + 1) / 6.0F, 1.0F, 1.0F);
         float sx = x + w * k / 6.0F;
         c.gradient(sx - (k > 0 ? 0.5F : 0.0F), hy, w / 6.0F + (k > 0 ? 0.5F : 0.0F), hh, k != 0 && k != 5 ? 0.0F : 6.0F, a, b, true);
      }

      float hx = x + hsv[0] * w;
      c.rect(hx - 3.0F, hy - 3.0F, 6.0F, hh + 6.0F, 3.0F, -1);
      c.stroke(hx - 3.0F, hy - 3.0F, 6.0F, hh + 6.0F, 3.0F, 1.0F, 1711276032);
      this.hit(x, hy - 4.0F, w, hh + 8.0F, () -> {
         this.drag = pxx -> {
            hsv[0] = Math.max(0.0F, Math.min(0.9999F, (pxx - x) / w));
            this.commitColor(p);
         };
         this.drag.accept(this.wmx);
      }, null);
      float fy = hy + hh + 14.0F;
      int cur = Theme.hsvToRgb(hsv[0], hsv[1], hsv[2]);
      c.rect(x, fy, 30.0F, 26.0F, 7.0F, 0xFF000000 | cur);
      boolean editing = this.focused == p.filter;
      c.rect(x + 38.0F, fy, 92.0F, 26.0F, 7.0F, Theme.a(this.th.bg, 0.6F));
      c.stroke(x + 38.0F, fy, 92.0F, 26.0F, 7.0F, 1.0F, editing ? this.th.accent : Theme.a(this.th.border, 0.9F));
      c.text("inter-med", 11.5F, x + 48.0F, fy + 13.0F, 17, this.th.text, editing ? p.filter.text : Theme.toHex(cur));
      if (editing) {
         this.caret(c, "inter-med", 11.5F, x + 48.0F, fy + 13.0F, p.filter);
      }

      this.hit(x + 38.0F, fy, 92.0F, 26.0F, () -> {
         p.filter.set(Theme.toHex(cur));
         p.filter.allSelected = true;
         this.focus(p.filter, () -> {
            int rgb = Theme.hex(p.filter.text, -1);
            if (rgb >= 0 && p.filter.text.replace("#", "").length() == 6) {
               float[] n = Theme.rgbToHsv(rgb);
               hsv[0] = n[0];
               hsv[1] = n[1];
               hsv[2] = n[2];
               this.set(p.module, p.key, Theme.toHex(rgb));
            }
         });
      }, null);
      String[][] themed = new String[][]{{"theme", "Follow theme accent"}, {"theme2", "Follow theme accent 2"}};

      for (int k = 0; k < 2; k++) {
         String tv = themed[k][0];
         float by = fy + 34.0F + k * 32;
         boolean active = tv.equals(str(this.get(p.module, p.key), ""));
         int tc = k == 0 ? this.th.accent : this.th.accent2;
         float hv = this.an.to("cpb:" + k, this.in(x, by, w, 26.0F) ? 1.0F : 0.0F, 16.0F);
         c.rect(x, by, w, 26.0F, 7.0F, Theme.a(tc, (active ? 0.32F : 0.12F) + 0.08F * hv));
         c.stroke(x, by, w, 26.0F, 7.0F, 1.0F, Theme.a(tc, active ? 0.9F : 0.35F + 0.3F * hv));
         c.circle(x + 14.0F, by + 13.0F, 5.0F, tc);
         c.text("inter-med", 11.0F, x + 26.0F, by + 13.0F, 17, this.th.text, themed[k][1]);
         if (active) {
            Icons.draw(c, "check", x + w - 14.0F, by + 13.0F, 11.0F, tc);
         }

         this.hit(x, by, w, 26.0F, () -> {
            this.set(p.module, p.key, tv);
            this.toast("Colour follows the theme");
         }, null);
      }
   }

   private void commitColor(ClickGui.Pop p) {
      this.set(p.module, p.key, Theme.toHex(Theme.hsvToRgb(p.hsv[0], p.hsv[1], p.hsv[2])));
   }

   private boolean inPop(float x, float y) {
      return this.pop != null && x >= this.pop.x && y >= this.pop.y && x <= this.pop.x + this.pop.w && y <= this.pop.y + this.pop.h;
   }

   private float drawThemes(Canvas c, float x, float y, float w, float since) {
      y = this.row(c, "§theme", "Theme colours for visuals", x + 10.0F, y, w - 20.0F, 0, (cxx, cyx, cw) -> {
         boolean v = this.store.themeColorsEnabled();
         float sx = cxx + cw - 38.0F;
         this.drawSwitch(c, "sw:themecolors", sx, cyx - 11.0F, 38.0F, 22.0F, v);
         this.hit(sx - 4.0F, cyx - 15.0F, 46.0F, 30.0F, () -> this.store.setThemeColorsEnabled(!v), null);
         return 22.0F;
      }) + 8.0F;
      float cardW = 172.0F;
      float cardH = 96.0F;
      float gap = 12.0F;
      int cols = Math.max(1, (int)((w + gap) / (cardW + gap)));
      cardW = (w - gap * (cols - 1)) / cols;
      String active = this.store.themeName();
      int i = 0;

      for (Entry<String, List<String>> e : this.data.themes.entrySet()) {
         String name = e.getKey();
         List<String> cols5 = e.getValue();
         int col = i % cols;
         int rowN = i / cols;
         float cx = x + col * (cardW + gap);
         float cy = y + rowN * (cardH + gap);
         float appear = Anim.easeOutCubic((since - 0.012F * i) / 0.32F);
         boolean act = name.equals(active);
         float hv = this.an.to("th:" + name, this.in(cx, cy, cardW, cardH) ? 1.0F : 0.0F, 14.0F);
         int acc = 0xFF000000 | Theme.hex(cols5.get(0), 8947848);
         int acc2 = 0xFF000000 | Theme.hex(cols5.get(1), 6710886);
         int bg = 0xFF000000 | Theme.hex(cols5.get(2), 1118481);
         int sur = 0xFF000000 | Theme.hex(cols5.get(3), 2236962);
         int bor = 0xFF000000 | Theme.hex(cols5.get(4), 3355443);
         float lift = -3.0F * hv;
         c.save();
         c.alpha(appear);
         c.translate(0.0F, (1.0F - appear) * 14.0F + lift);
         c.shadow(cx, cy + 4.0F, cardW, cardH, 12.0F, 16.0F + 8.0F * hv, Theme.a(act ? acc : -16777216, act ? 0.45F : 0.3F + 0.1F * hv));
         c.gradient(cx, cy, cardW, cardH, 12.0F, sur, bg, false);
         c.glow(cx + cardW - 26.0F, cy + 24.0F, 42.0F, Theme.a(acc, 0.35F + 0.25F * hv));
         c.stroke(cx, cy, cardW, cardH, 12.0F, act ? 2.0F : 1.0F, act ? acc : Theme.a(bor, 0.9F));
         int tcol = Theme.luma(sur) > 0.55F ? -15329251 : -1184272;
         c.text("inter-sb", 13.0F, cx + 14.0F, cy + 22.0F, 17, tcol, name);

         for (int k = 0; k < 5; k++) {
            int sc = 0xFF000000 | Theme.hex(cols5.get(k), 8947848);
            c.circle(cx + 22.0F + k * 22, cy + cardH - 24.0F, 8.0F, sc);
            c.ring(cx + 22.0F + k * 22, cy + cardH - 24.0F, 8.0F, 1.0F, Theme.a(-1, 0.18F));
         }

         c.gradient(cx + 14.0F, cy + 38.0F, cardW - 28.0F, 5.0F, 2.5F, acc, acc2, true);
         if (act) {
            c.circle(cx + cardW - 18.0F, cy + 20.0F, 9.0F, acc);
            Icons.draw(c, "check", cx + cardW - 18.0F, cy + 20.0F, 10.0F, Theme.onColor(acc));
         }

         c.restore();
         this.hit(cx, cy, cardW, cardH, () -> this.changeTheme(name, cols5), null);
         i++;
      }

      int rows = (this.data.themes.size() + cols - 1) / cols;
      return y + rows * (cardH + gap) + 10.0F;
   }

   private float drawProfiles(Canvas c, float x, float y, float w, float since) {
      y = this.nameBar(c, x, y, w, "New profile name…", "Save current", n -> {
         this.store.saveProfile(n);
         this.toast("Saved profile " + n);
      });
      String active = this.store.activeProfile();
      List<String> profs = this.store.profiles();
      float bx = x;
      String[][] fileBtns = new String[][]{{"fimport", "Load from file…"}, {"ffolder", "Open configs folder"}};

      for (String[] b : fileBtns) {
         float bw = c.textWidth("inter-med", 12.0F, b[1]) + 30.0F;
         Runnable r = "fimport".equals(b[0]) ? this::pickAndImport : () -> this.openFolder(null);
         this.button(c, b[0], bx, y, bw, 30.0F, b[1], false, r);
         bx += bw + 10.0F;
      }

      y += 46.0F;
      int i = 0;

      for (String p : profs) {
         boolean act = p.equals(active);
         String cloudTag = CloudConfigs.linkOf(p) != null ? "SYNCED" : null;
         String tag = act ? (cloudTag != null ? "ACTIVE · SYNCED" : "ACTIVE") : cloudTag;
         y = this.listRow(c, "prof:" + p, x, y, w, i++, since, p, tag, new String[]{"Load", "Overwrite", "Export", "Upload", "Delete"}, k -> {
            switch (k) {
               case 0:
                  this.store.loadProfile(p);
                  this.toast("Loaded " + p);
                  break;
               case 1:
                  this.confirm("ow:" + p, () -> {
                     this.store.saveProfile(p);
                     this.toast("Overwrote " + p);
                  });
                  break;
               case 2:
                  try {
                     this.toast("Saved configs/" + this.store.exportProfile(p));
                  } catch (Exception e) {
                     this.toast("Export failed: " + e.getMessage());
                  }
                  break;
               case 3:
                  this.startUpload(p);
                  break;
               default:
                  this.confirm("del:" + p, () -> {
                     this.store.deleteProfile(p);
                     this.toast("Deleted " + p);
                  });
            }
         }, act);
      }

      if (profs.isEmpty()) {
         y = this.empty(c, x, y, w, "No profiles yet. Save your current setup above.");
      }

      y += 8.0F;
      c.text("inter-sb", 10.0F, x + 6.0F, y + 10.0F, 17, Theme.a(this.th.accent, 0.9F), "FILES IN CLARITY/CONFIGS");
      y += 26.0F;
      List<Path> files = this.store.configFiles();
      int j = 0;

      for (Path f : files) {
         String fn = f.getFileName().toString();
         y = this.listRow(c, "file:" + fn, x, y, w, i + j++, since, fn, null, new String[]{"Load", "Import"}, k -> this.importFile(f, k == 0), false);
      }

      if (files.isEmpty()) {
         y = this.empty(c, x, y, w, "Exported profiles and .json configs you put in this folder show up here.");
      }

      return y + 10.0F;
   }

   private void importFile(Path f, boolean load) {
      try {
         String n = this.store.importProfile(f, load);
         this.toast((load ? "Loaded " : "Imported ") + n);
      } catch (Exception e) {
         this.toast("Can't load " + f.getFileName() + ": " + e.getMessage());
      }
   }

   private void pickAndImport() {
      boolean ok = this.store.pickConfigFile(f -> {
         if (f != null) {
            if (f == ConfigFiles.NO_DIALOG) {
               this.openFolder("No file picker here: drop the .json in this folder, then press Load below");
            } else {
               this.importFile(f, true);
            }
         }
      });
      if (!ok) {
         this.openFolder("No file picker here: drop the .json in this folder, then press Load below");
      }
   }

   private void openFolder(String hint) {
      this.store.openConfigFolder(opened -> {
         if (opened) {
            this.toast(hint != null ? hint : "Opened the configs folder");
         } else {
            this.store.setClipboard(this.store.configFolderPath());
            this.toast("Couldn't open a file manager. Folder path copied to clipboard");
         }
      });
   }

   private float drawBindSets(Canvas c, float x, float y, float w, float since) {
      y = this.nameBar(c, x, y, w, "Bind set name…", "Save current binds", n -> {
         this.store.saveBindSet(n);
         this.toast("Saved bind set " + n);
      });
      List<String> sets = this.store.bindSets();
      int i = 0;

      for (String b : sets) {
         y = this.listRow(c, "bind:" + b, x, y, w, i++, since, b, null, new String[]{"Load", "Overwrite", "Delete"}, k -> {
            switch (k) {
               case 0:
                  this.store.loadBindSet(b);
                  this.toast("Loaded binds " + b);
                  break;
               case 1:
                  this.confirm("bow:" + b, () -> {
                     this.store.saveBindSet(b);
                     this.toast("Overwrote " + b);
                  });
                  break;
               default:
                  this.confirm("bdel:" + b, () -> {
                     this.store.deleteBindSet(b);
                     this.toast("Deleted " + b);
                  });
            }
         }, false);
      }

      if (sets.isEmpty()) {
         y = this.empty(c, x, y, w, "No bind sets yet. Save your current keybinds above.");
      }

      return y + 10.0F;
   }

   private float drawServers(Canvas c, float x, float y, float w, float since) {
      String cur = this.store.currentServer();
      String active = this.store.activeProfile();
      float h = 56.0F;
      c.rect(x, y, w, h, 12.0F, Theme.a(Theme.mix(this.th.bg, this.th.accent, 0.08F), 0.7F));
      c.stroke(x, y, w, h, 12.0F, 1.0F, Theme.a(this.th.accent, 0.35F));
      c.text("inter-med", 11.0F, x + 16.0F, y + 19.0F, 17, this.th.muted, "Current server");
      c.text("inter-sb", 14.0F, x + 16.0F, y + 38.0F, 17, this.th.text, cur != null && !cur.isEmpty() ? cur : "Not connected");
      if (cur != null && !cur.isEmpty() && active != null && !active.isEmpty()) {
         String lbl = "Auto-apply “" + active + "” here";
         float bw = c.textWidth("inter-sb", 11.5F, lbl) + 28.0F;
         this.button(c, "srvmap", x + w - 16.0F - bw, y + h / 2.0F - 15.0F, bw, 30.0F, lbl, true, () -> {
            this.store.mapServer(cur, active);
            this.toast("Mapped " + cur);
         });
      }

      y += h + 14.0F;
      int i = 0;

      for (Entry<String, String> e : this.store.serverProfiles().entrySet()) {
         String addr = e.getKey();
         y = this.listRow(c, "srv:" + addr, x, y, w, i++, since, addr, e.getValue(), new String[]{"Remove"}, k -> this.confirm("srvdel:" + addr, () -> {
            this.store.unmapServer(addr);
            this.toast("Removed " + addr);
         }), false);
      }

      if (this.store.serverProfiles().isEmpty()) {
         y = this.empty(c, x, y, w, "No server mappings yet.");
      }

      return y + 10.0F;
   }

   private float drawGuiPage(Canvas c, float x, float y, float w, float since) {
      float CR = this.gnum("Card Rounding", 14.0);
      int i = 0;

      for (Entry<String, List<GuiData.Setting>> sec : GUI_SECTIONS.entrySet()) {
         String name = sec.getKey();
         float appear = Anim.easeOutCubic((since - 0.04F * i) / 0.34F);
         c.save();
         c.alpha(appear);
         c.translate(0.0F, this.pageSlide ? (1.0F - appear) * 12.0F : 0.0F);
         c.text("inter-sb", 10.0F, x + 6.0F, y + 10.0F, 17, Theme.a(this.th.accent, 0.9F), name.toUpperCase(Locale.ROOT));
         y += 22.0F;
         float top = y;
         float est = this.measured.getOrDefault("§gui:" + name, 120.0F);
         c.rect(x, y, w, est, CR, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F), this.gnum("Card Opacity", 0.62)));
         c.stroke(x, y, w, est, CR, 1.0F, Theme.a(this.th.border, 0.6F));
         float end = this.drawSettings(c, "Click GUI", sec.getValue(), x + 20.0F, y + 6.0F, w - 40.0F);
         if ("General".equals(name)) {
            boolean web = this.store.webMode();
            String lbl = web ? "Open the web GUI" : "Switch to the web GUI";
            float bw = c.textWidth("inter-med", 12.0F, lbl) + 34.0F;
            this.button(c, "openweb", x + w - 20.0F - bw, end + 4.0F, bw, 30.0F, lbl, false, web ? this.store::openWebGui : () -> {
               this.store.switchToWebGui();
               this.close();
            });
            end += 42.0F;
         }

         end += 6.0F;
         this.measured.put("§gui:" + name, end - top);
         c.restore();
         y = end + 18.0F;
         i++;
      }

      return y;
   }

   private Object friendGet(String module, String key) {
      if (FriendSettings.isGlobalModule(module)) {
         return FriendManager.setting(key);
      }

      Friend f = FriendManager.byKey(module.substring("§friend:".length()));
      return f == null ? FriendSettings.perFriendDefault(key) : f.option(key);
   }

   private void friendSet(String module, String key, Object v) {
      if (FriendSettings.isGlobalModule(module)) {
         FriendManager.setSetting(key, v);
         if ("Online Features".equals(key) && Boolean.FALSE.equals(v)) {
            this.toast("Online features off: nobody can see you");
         }

         if ("Status".equals(key) && "Invisible".equals(v)) {
            this.toast("Invisible: not shown, not counted, no requests");
         }
      } else {
         FriendManager.setFriendOption(module.substring("§friend:".length()), key, v);
      }
   }

   private static boolean friendOnline(Friend f) {
      return !PresenceClient.confirmed(f) ? false : PresenceClient.onlineOnClarity(f) || Friends.inCurrentServer(f);
   }

   private String friendStatus(Friend f) {
      if (!PresenceClient.confirmed(f)) {
         return PresenceClient.outgoingRequests().stream().anyMatch(n -> n.equalsIgnoreCase(f.name())) ? "Request sent · waiting for them to accept" : "Waiting for them to add you back";
      }

      if (Friends.nearby(f)) {
         return "Nearby";
      }

      if (Friends.inCurrentServer(f)) {
         return "In your server";
      }

      PresenceClient.Presence p = PresenceClient.presenceOf(f);
      if (p != null && p.online()) {
         String st = switch (p.status()) {
            case "away" -> "Away";
            case "busy" -> "Busy";
            default -> "Online";
         };
         return p.server() != null && !p.server().isEmpty() ? st + " on " + p.server() : st + " on Clarity";
      } else {
         return f.hasUuid() ? "Offline" : "Not seen yet";
      }
   }

   private int friendStatusColour(Friend f) {
      if (!PresenceClient.confirmed(f)) {
         return Theme.a(this.th.muted, 0.4F);
      }

      if (!Friends.nearby(f) && !Friends.inCurrentServer(f)) {
         PresenceClient.Presence p = PresenceClient.presenceOf(f);
         if (p != null && p.online()) {
            return switch (p.status()) {
               case "away" -> -673731;
               case "busy" -> -43920;
               default -> -12855686;
            };
         } else {
            return Theme.a(this.th.muted, 0.55F);
         }
      } else {
         return -12855686;
      }
   }

   private float friendLabel(Canvas c, float x, float y, String left, String right) {
      c.text("inter-sb", 10.0F, x + 6.0F, y + 10.0F, 17, Theme.a(this.th.accent, 0.9F), left);
      if (right != null) {
         float lw = c.textWidth("inter-sb", 10.0F, left);
         c.text("inter-med", 10.0F, x + 14.0F + lw, y + 10.0F, 17, this.th.muted, right);
      }

      return y + 26.0F;
   }

   private void friendAdd(boolean request) {
      String n = this.nameField.text.trim();
      if (n.isEmpty()) {
         this.toast("Type a name first");
      } else {
         if (request) {
            String err = PresenceClient.sendRequest(n);
            if (err != null) {
               this.toast(err);
               return;
            }

            this.toast("Request sent to " + n);
         } else {
            switch (FriendManager.add(null, n)) {
               case ADDED:
                  this.toast("Added " + n + " to friends");
                  break;
               case ALREADY:
                  this.toast(n + " is already a friend");
                  break;
               default:
                  this.toast("That isn't a valid Minecraft name");
                  return;
            }
         }

         this.nameField.set("");
         this.focused = null;
         this.nameEnter = null;
      }
   }

   private float drawFriends(Canvas c, float x, float y, float w, float since) {
      boolean onlineOn = FriendManager.bool("Online Features");
      PresenceClient.State ps = PresenceClient.state();
      boolean connected = ps == PresenceClient.State.CONNECTED;
      float h = 56.0F;
      c.rect(x, y, w, h, 12.0F, Theme.a(Theme.mix(this.th.bg, this.th.accent, 0.08F), 0.7F));
      c.stroke(x, y, w, h, 12.0F, 1.0F, Theme.a(this.th.accent, 0.35F));
      float ctlW = 150.0F;
      c.text("inter-med", 11.0F, x + 18.0F, y + 19.0F, 17, this.th.muted, "Clarity network");
      c.text("inter-sb", 13.0F, x + 18.0F, y + 38.0F, 17, this.th.text, this.ellipsize(c, "inter-sb", 13.0F, PresenceClient.statusLine(), w - ctlW - 50.0F));
      if (onlineOn) {
         String cur = str(this.friendGet("§friends", "Status"), "Online");
         this.dropdown(c, "friends/status", x + w - 16.0F - ctlW, y + h / 2.0F, ctlW, FriendSettings.STATUSES, cur, v -> this.friendSet("§friends", "Status", v));
      } else {
         this.button(c, "friends/on", x + w - 16.0F - ctlW, y + h / 2.0F - 15.0F, ctlW, 30.0F, "Turn on", true, () -> this.friendSet("§friends", "Online Features", true));
      }

      y += h + 14.0F;
      y = this.drawFriendsPageTabs(c, x, y, w) + 14.0F;
      if ("Messages".equals(this.friendsTab)) {
         float avail = this.clipH < 100000.0F ? this.clipY + this.clipH - y - 16.0F : 460.0F;
         float mh = Math.max(300.0F, avail);
         this.drawMessagesPage(c, x, y, w, mh);
         return y + mh;
      }

      if ("Chat".equals(this.friendsTab)) {
         float avail = this.clipH < 100000.0F ? this.clipY + this.clipH - y - 16.0F : 460.0F;
         float chH = Math.max(280.0F, avail);
         this.drawChatBox(c, null, "page", x, y, w, chH, 13.0F);
         return y + chH;
      }

      String addLbl = "Add friend";
      String reqLbl = "Send request";
      float bw1 = c.textWidth("inter-sb", 12.0F, addLbl) + 32.0F;
      float bw2 = connected ? c.textWidth("inter-med", 12.0F, reqLbl) + 30.0F : 0.0F;
      float fw = Math.max(140.0F, Math.min(340.0F, w - bw1 - bw2 - 24.0F));
      boolean editing = this.focused == this.nameField;
      float ft = this.an.to("friendField", editing ? 1.0F : 0.0F, 14.0F);
      c.rect(x, y, fw, 34.0F, this.ctlR() + 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.85F));
      c.stroke(x, y, fw, 34.0F, this.ctlR() + 1.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, ft));
      String t = this.nameField.text;
      c.save();
      c.scissor(x + 8.0F, y, fw - 16.0F, 34.0F);
      c.text("inter", 12.5F, x + 13.0F, y + 17.0F, 17, t.isEmpty() && !editing ? this.th.muted : this.th.text, t.isEmpty() && !editing ? "Minecraft name…" : t);
      if (editing) {
         this.caret(c, "inter", 12.5F, x + 13.0F, y + 17.0F, this.nameField);
      }

      c.restore();
      this.hit(x, y, fw, 34.0F, () -> {
         this.focus(this.nameField, null);
         this.nameEnter = () -> this.friendAdd(false);
      }, null);
      this.button(c, "friends/add", x + fw + 10.0F, y + 2.0F, bw1, 30.0F, addLbl, true, () -> this.friendAdd(false));
      if (connected) {
         this.button(c, "friends/req", x + fw + 20.0F + bw1, y + 2.0F, bw2, 30.0F, reqLbl, false, () -> this.friendAdd(true));
      }

      y += 50.0F;
      int i = 0;
      List<PresenceClient.Request> reqs = PresenceClient.incomingRequests();
      if (!reqs.isEmpty()) {
         y = this.friendLabel(c, x, y, "FRIEND REQUESTS", String.valueOf(reqs.size()));

         for (PresenceClient.Request r : reqs) {
            y = this.listRow(c, "freq:" + r.uuid(), x, y, w, i++, since, r.name(), "WANTS TO BE FRIENDS", new String[]{"Accept", "Decline"}, k -> {
               PresenceClient.respond(r, k == 0);
               this.toast(k == 0 ? r.name() + " is now your friend" : "Declined " + r.name());
            }, true);
         }
      }

      Set<String> sent = PresenceClient.outgoingRequests();
      if (!sent.isEmpty()) {
         y = this.friendLabel(c, x, y, "SENT REQUESTS", String.valueOf(sent.size()));

         for (String n : sent) {
            y = this.listRow(c, "fout:" + n, x, y, w, i++, since, n, "PENDING", new String[]{"Cancel"}, k -> {
               PresenceClient.cancelRequest(n);
               this.toast("Cancelled request to " + n);
            }, false);
         }
      }

      List<Friend> list = FriendManager.sorted(ClickGui::friendOnline);
      int online = 0;

      for (Friend f : list) {
         if (friendOnline(f)) {
            online++;
         }
      }

      y = this.friendLabel(c, x, y, "FRIENDS · " + list.size(), list.isEmpty() ? null : online + " online");
      float gap = this.gnum("Card Spacing", 10.0);
      int j = 0;

      for (Friend f : list) {
         float appear = Anim.easeOutCubic((since - 0.022F * Math.min(j++, 18)) / 0.34F);
         y = this.drawFriendCard(c, f, x, y, w, appear) + gap;
      }

      if (list.isEmpty()) {
         y = this.empty(c, x, y, w, "No friends yet. Type a name above, or middle click a player in game.");
      }

      float CR = this.gnum("Card Rounding", 14.0);
      int si = 0;

      for (Entry<String, List<GuiData.Setting>> sec : FriendSettings.GLOBAL_SECTIONS.entrySet()) {
         String name = sec.getKey();
         float appear = Anim.easeOutCubic((since - 0.04F * si++) / 0.34F);
         c.save();
         c.alpha(appear);
         c.translate(0.0F, this.pageSlide ? (1.0F - appear) * 12.0F : 0.0F);
         y = this.friendLabel(c, x, y + 4.0F, name.toUpperCase(Locale.ROOT) + " SETTINGS", null) - 4.0F;
         float top = y;
         float est = this.measured.getOrDefault("§friends:" + name, 120.0F);
         c.rect(x, y, w, est, CR, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F), this.gnum("Card Opacity", 0.62)));
         c.stroke(x, y, w, est, CR, 1.0F, Theme.a(this.th.border, 0.6F));
         float end = this.drawSettings(c, "§friends", sec.getValue(), x + 20.0F, y + 6.0F, w - 40.0F);
         if ("General".equals(name) && !list.isEmpty()) {
            String lbl = "frclear".equals(this.confirmId) && this.time - this.confirmAt < 2.5F ? "Click again to remove everyone" : "Remove all friends";
            float bw = c.textWidth("inter-med", 12.0F, lbl) + 34.0F;
            this.button(c, "friends/clear", x + w - 20.0F - bw, end + 4.0F, bw, 30.0F, lbl, false, () -> this.confirm("frclear", () -> {
               FriendManager.clear();
               this.toast("Friends list cleared");
            }));
            end += 42.0F;
         }

         end += 6.0F;
         this.measured.put("§friends:" + name, end - top);
         c.restore();
         y = end + 18.0F;
      }

      return y;
   }

   private float drawFriendCard(Canvas c, Friend f, float x, float y, float w, float appear) {
      String fkey = f.key();
      String key = "fr:" + fkey;
      String module = FriendSettings.friendModule(f);
      float H = 58.0F;
      float expT = this.an.to("exp:" + key, this.expanded.contains(key) ? 1.0F : 0.0F, 11.0F);
      float setH = this.measured.getOrDefault(key, 0.0F);
      float h = H + setH * Anim.ease(expT);
      boolean visible = y + h > this.clipY - 4.0F && y < this.clipY + this.clipH + 4.0F;
      if (!visible) {
         return y + h;
      }

      float ry = y + (this.pageSlide ? (1.0F - appear) * 16.0F : 0.0F);
      float hov = this.an.to("frHover:" + key, this.in(x, ry, w, H) && this.pop == null ? 1.0F : 0.0F, 14.0F);
      float CR = this.gnum("Card Rounding", 14.0);
      boolean onlineNow = friendOnline(f);
      c.save();
      c.alpha(appear);
      if (this.gbool("Hover Lift", true) && hov > 0.01F) {
         c.shadow(x, ry + 3.0F, w, h, CR, 16.0F, Theme.a(-16777216, 0.22F * hov));
      }

      int fill = Theme.mix(this.th.bg, this.th.text, 0.035F + 0.025F * hov);
      c.rect(x, ry, w, h, CR, Theme.a(fill, this.gnum("Card Opacity", 0.62)));
      if (f.favourite()) {
         c.gradient(x, ry, w * 0.6F, h, CR, Theme.a(-670398, 0.08F), Theme.a(-670398, 0.0F), true);
      }

      c.stroke(x, ry, w, h, CR, 1.0F, Theme.a(this.th.border, 0.55F + 0.25F * hov));
      int rgb = FriendManager.nameColour(f);
      int nc = rgb >= 0 ? 0xFF000000 | rgb : this.th.text;
      float ax = x + 30.0F;
      float ay = ry + H / 2.0F;
      String disp = f.displayName();
      String initial = disp.isEmpty() ? "?" : disp.substring(0, 1).toUpperCase(Locale.ROOT);
      c.circle(ax, ay, 15.0F, Theme.a(nc, 0.16F));
      c.ring(ax, ay, 15.0F, 1.2F, Theme.a(nc, 0.55F));
      c.text("inter-sb", 13.0F, ax, ay, 18, nc, initial);
      int sc = this.friendStatusColour(f);
      c.circle(ax + 11.0F, ay + 11.0F, 5.0F, 0xFF000000 | this.th.bg);
      if (onlineNow) {
         c.glow(ax + 11.0F, ay + 11.0F, 7.0F, Theme.a(sc, 0.5F));
      }

      c.circle(ax + 11.0F, ay + 11.0F, 3.4F, sc);
      float tx = x + 58.0F;
      float rightEdge = x + w - 20.0F;
      float starX = rightEdge - 40.0F;
      float chevX = rightEdge - 8.0F;
      float maxNameW = starX - 20.0F - tx;
      String dn = this.ellipsize(c, "inter-sb", 14.5F, disp, maxNameW * 0.6F);
      c.text("inter-sb", 14.5F, tx, ry + 22.0F, 17, nc, dn);
      float nx = tx + c.textWidth("inter-sb", 14.5F, dn) + 8.0F;
      if (!disp.equals(f.name()) && !f.name().isEmpty()) {
         c.text("inter", 11.5F, nx, ry + 22.0F, 17, this.th.muted, f.name());
         nx += c.textWidth("inter", 11.5F, f.name()) + 8.0F;
      }

      if (f.bool("Protect") && FriendManager.bool("Protect Friends")) {
         String tag = "PROTECTED";
         float bw = c.textWidth("inter-sb", 9.0F, tag) + 12.0F;
         if (nx + bw < starX - 8.0F) {
            c.rect(nx, ry + 22.0F - 8.0F, bw, 16.0F, 8.0F, Theme.a(this.th.accent, 0.14F));
            c.text("inter-sb", 9.0F, nx + bw / 2.0F, ry + 22.0F, 18, this.th.accent, tag);
         }
      }

      String sub = this.friendStatus(f);
      String notes = f.str("Notes").trim();
      if (!notes.isEmpty()) {
         sub = sub + "  ·  " + notes;
      }

      c.text("inter", 11.5F, tx, ry + 40.0F, 17, onlineNow ? Theme.mix(this.th.muted, sc, 0.55F) : this.th.muted, this.ellipsize(c, "inter", 11.5F, sub, starX - 16.0F - tx));
      this.hit(x, ry, w, H, () -> this.toggleExpand(key), () -> this.toggleExpand(key));
      boolean fav = f.favourite();
      float starH = this.an.to("frStar:" + key, this.in(starX - 12.0F, ay - 12.0F, 24.0F, 24.0F) ? 1.0F : 0.0F, 16.0F);
      Icons.draw(c, "star", starX, ay, 16.0F + 2.0F * starH, fav ? -670398 : Theme.a(this.th.muted, 0.5F + 0.5F * starH));
      if (fav) {
         c.glow(starX, ay, 10.0F, Theme.a(-670398, 0.25F));
      }

      this.hit(starX - 13.0F, ay - 13.0F, 26.0F, 26.0F, () -> this.friendSet(module, "Favourite", !fav), null);
      float rot = Anim.ease(expT);
      c.save();
      c.translate(chevX, ay);
      Icons.draw(c, rot > 0.5F ? "down" : "chevron", 0.0F, 0.0F, 16.0F, Theme.mix(this.th.muted, this.th.accent, rot));
      c.restore();
      if (expT > 0.002F || setH == 0.0F) {
         float sy0 = ry + H;
         c.save();
         float vis = setH * Anim.ease(expT);
         c.scissor(x, sy0, w, Math.max(0.0F, vis));
         float savedY = this.clipY;
         float savedH = this.clipH;
         float savedX = this.clipX;
         float savedW = this.clipW;
         this.intersectClip(x, sy0, w, vis);
         c.alpha(Anim.ease(Math.min(1.0F, expT * 1.4F)));
         c.line(x + 16.0F, sy0, x + w - 16.0F, sy0, 1.0F, Theme.a(this.th.border, 0.5F));
         float end = this.drawSettings(c, module, FriendSettings.PER_FRIEND, x + 20.0F, sy0 + 8.0F, w - 40.0F);
         float by = end + 4.0F;
         float bx = x + 20.0F;
         boolean confirmingDel = ("frdel:" + fkey).equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
         String delLbl = confirmingDel ? "Click again to remove" : "Remove friend";
         float dw = c.textWidth("inter-med", 12.0F, delLbl) + 30.0F;
         float dh = this.an.to("frDel:" + key, this.in(bx, by, dw, 30.0F) ? 1.0F : 0.0F, 16.0F);
         c.rect(bx, by, dw, 30.0F, this.ctlR() + 1.0F, Theme.a(-43920, (confirmingDel ? 0.3F : 0.1F) + 0.12F * dh));
         c.stroke(bx, by, dw, 30.0F, this.ctlR() + 1.0F, 1.0F, Theme.a(-43920, 0.3F + 0.4F * dh));
         c.text("inter-med", 12.0F, bx + dw / 2.0F, by + 15.0F, 18, Theme.mix(-43920, this.th.text, 0.25F), delLbl);
         this.hit(bx, by, dw, 30.0F, () -> this.confirm("frdel:" + fkey, () -> {
            FriendManager.removeByKey(fkey);
            this.expanded.remove(key);
            this.toast("Removed " + f.name());
         }), null);
         bx += dw + 10.0F;
         String rl = "Reset options";
         float rw = c.textWidth("inter-med", 12.0F, rl) + 30.0F;
         this.button(c, "frreset:" + fkey, bx, by, rw, 30.0F, rl, false, () -> {
            FriendManager.resetFriend(fkey);
            this.toast(f.name() + " reset");
         });
         bx += rw + 10.0F;
         if (PresenceClient.confirmed(f)) {
            String ml = "Message";
            float mw = c.textWidth("inter-med", 12.0F, ml) + 30.0F;
            this.button(c, "frmsg:" + fkey, bx, by, mw, 30.0F, ml, true, () -> this.openDm(f));
            bx += mw + 10.0F;
         } else if (PresenceClient.state() == PresenceClient.State.CONNECTED) {
            String ml = "Send request";
            float mw = c.textWidth("inter-med", 12.0F, ml) + 30.0F;
            this.button(c, "frreq2:" + fkey, bx, by, mw, 30.0F, ml, false, () -> {
               String e = PresenceClient.sendRequest(f.name(), f.hasUuid() ? f.uuid() : null, false);
               this.toast(e == null ? "Request sent to " + f.name() : e);
            });
            bx += mw + 10.0F;
         }

         String cl = "Copy name";
         float cw = c.textWidth("inter-med", 12.0F, cl) + 30.0F;
         this.button(c, "frcopy:" + fkey, bx, by, cw, 30.0F, cl, false, () -> {
            this.store.setClipboard(f.name());
            this.toast("Copied " + f.name());
         });
         if (f.hasUuid()) {
            String added = new SimpleDateFormat("d MMM yyyy", Locale.ROOT).format(new Date(f.addedAt()));
            c.text("inter", 10.5F, x + w - 20.0F, by + 15.0F, 20, Theme.a(this.th.muted, 0.8F), "Added " + added);
         }

         end = by + 30.0F;
         this.clipX = savedX;
         this.clipY = savedY;
         this.clipW = savedW;
         this.clipH = savedH;
         this.measured.put(key, end - sy0 + 14.0F);
         c.restore();
      }

      c.restore();
      return y + h;
   }

   private void sendDmField() {
      if (this.dmTarget != null) {
         String err = DirectMessages.send(this.dmTarget, this.dmField.text);
         if (err != null) {
            this.toast(err);
         } else {
            this.dmField.set("");

            for (Entry<String, float[]> e : this.chatScroll.entrySet()) {
               if (e.getKey().startsWith("dm")) {
                  e.getValue()[1] = 0.0F;
               }
            }
         }
      }
   }

   private void openDm(Friend f) {
      if (f != null && f.hasUuid()) {
         this.dmTarget = f.uuid().toLowerCase(Locale.ROOT);
         this.friendsTab = "Messages";
         this.pFriendsTab = "DMs";
         this.resetScroll();
         this.focus(this.dmField, null);
      }
   }

   private void drawEmojiSuggestions(Canvas c, TextField field, float fx, float fy, float fieldW, float fs, boolean panel) {
      String q = Emoji.partialAtEnd(field.text);
      if (q != null && !field.text.equals(this.emojiSugDismissed)) {
         if (!q.equals(this.emojiSugQ) || this.emojiSugField != field) {
            this.emojiSugSel = 0;
            this.emojiSugQ = q;
         }

         this.emojiSugField = field;
         this.emojiSug = Emoji.search(q, panel ? 4 : 6);
         if (!this.emojiSug.isEmpty()) {
            this.emojiSugSel = Math.min(this.emojiSugSel, this.emojiSug.size() - 1);
            float tfs = fs - 0.5F;
            float ch = tfs + (panel ? 9 : 13);
            float gap = 3.0F;
            float[] widths = new float[this.emojiSug.size()];
            float total = 6.0F;
            int shown = 0;

            for (int i = 0; i < this.emojiSug.size(); i++) {
               String nm = Emoji.bestName(this.emojiSug.get(i), q);
               widths[i] = c.textWidth("inter", tfs, ":" + nm + ":") + c.textWidth("inter-med", tfs, nm) + 18.0F;
               if (total + widths[i] + gap > fieldW && shown > 0) {
                  break;
               }

               total += widths[i] + gap;
               shown++;
            }

            float sx = fx;
            float sy = fy - ch - 8.0F;
            float sw = total + 3.0F;
            float t = this.an.to("emojiSug", 1.0F, 20.0F);
            c.save();
            c.alpha(t);
            c.shadow(sx, sy + 2.0F, sw, ch + 6.0F, 9.0F, 14.0F, Theme.a(-16777216, 0.45F));
            c.rect(sx, sy, sw, ch + 6.0F, 9.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F), 0.97F));
            c.stroke(sx, sy, sw, ch + 6.0F, 9.0F, 1.0F, Theme.a(Theme.mix(this.th.border, this.th.accent, 0.3F), 0.9F));
            float cx = sx + 6.0F;

            for (int i = 0; i < shown; i++) {
               Emoji.Entry e = this.emojiSug.get(i);
               String nm = Emoji.bestName(e, q);
               float w = widths[i];
               boolean sel = i == this.emojiSugSel;
               boolean hv = this.in(cx, sy + 3.0F, w, ch);
               if (sel || hv) {
                  c.rect(cx, sy + 3.0F, w, ch, 7.0F, Theme.a(sel ? this.th.accent : this.th.text, sel ? 0.2F : 0.07F));
               }

               float ew = c.text("inter", tfs, cx + 6.0F, sy + 3.0F + ch / 2.0F, 17, this.th.text, ":" + nm + ":");
               c.text("inter-med", tfs, cx + 10.0F + ew, sy + 3.0F + ch / 2.0F, 17, sel ? this.th.text : Theme.a(this.th.text, 0.75F), nm);
               Emoji.Entry fe = e;
               this.hit(cx, sy + 3.0F, w, ch, () -> this.completeEmoji(field, fe), null);
               cx += w + gap;
            }

            c.restore();
         }
      } else {
         if (this.emojiSugField == field) {
            this.emojiSug = List.of();
         }
      }
   }

   private void completeEmoji(TextField field, Emoji.Entry e) {
      String q = Emoji.partialAtEnd(field.text);
      if (q != null && e != null) {
         String head = field.text.substring(0, field.text.length() - q.length() - 1);
         field.set(head + ":" + Emoji.bestName(e, q) + ": ");
         this.emojiSug = List.of();
         this.focus(field, null);
      }
   }

   private boolean emojiKey(int key) {
      if (this.emojiSug.isEmpty() || this.focused == null || this.emojiSugField != this.focused) {
         return false;
      }

      if (Emoji.partialAtEnd(this.focused.text) == null) {
         this.emojiSug = List.of();
         return false;
      }

      switch (key) {
         case 256:
            this.emojiSugDismissed = this.focused.text;
            this.emojiSug = List.of();
            return true;
         case 257:
         case 258:
         case 335:
            this.completeEmoji(this.focused, this.emojiSug.get(Math.min(this.emojiSugSel, this.emojiSug.size() - 1)));
            return true;
         case 264:
            this.emojiSugSel = (this.emojiSugSel + 1) % this.emojiSug.size();
            return true;
         case 265:
            this.emojiSugSel = (this.emojiSugSel + this.emojiSug.size() - 1) % this.emojiSug.size();
            return true;
         default:
            return false;
      }
   }

   private void sendChatField() {
      String err = GlobalChat.send(this.chatField.text);
      if (err != null) {
         this.toast(err);
      } else {
         this.chatField.set("");

         for (float[] s : this.chatScroll.values()) {
            s[1] = 0.0F;
         }
      }
   }

   private void mentionInChat(String name) {
      String t = this.chatField.text;
      String add = "@" + name + " ";
      if (!t.contains(add)) {
         this.chatField.set(!t.isEmpty() && !t.endsWith(" ") ? t + " " + add : t + add);
      }

      this.focus(this.chatField, null);
   }

   private static String chatTime(long at) {
      return new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(at));
   }

   private boolean chatIn(ClickGui.Panel p, float x, float y, float w, float h) {
      return p != null ? this.pin(p, x, y, w, h) : this.in(x, y, w, h);
   }

   private void chatHit(float x, float y, float w, float h, Runnable click, Runnable right) {
      this.hit(x, y, w, h, click, right, this.chatWheel);
   }

   private List<String> chatWrap(Canvas c, String font, float size, String s, float maxW) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : s.split(" ")) {
         while (word.length() > 1 && c.textWidth(font, size, word) > maxW) {
            if (line.length() > 0) {
               out.add(line.toString());
               line.setLength(0);
            }

            int lo = 1;
            int hi = word.length() - 1;

            while (lo < hi) {
               int mid = (lo + hi + 1) / 2;
               if (c.textWidth(font, size, word.substring(0, mid)) <= maxW) {
                  lo = mid;
               } else {
                  hi = mid - 1;
               }
            }

            out.add(word.substring(0, lo));
            word = word.substring(lo);
         }

         String t = line.length() == 0 ? word : line + " " + word;
         if (line.length() > 0 && c.textWidth(font, size, t) > maxW) {
            out.add(line.toString());
            line.setLength(0);
            line.append(word);
         } else {
            line.setLength(0);
            line.append(t);
         }
      }

      if (line.length() > 0 || out.isEmpty()) {
         out.add(line.toString());
      }

      return out;
   }

   private List<String> chatLines(Canvas c, GlobalChat.Message m, String font, float size, float maxW) {
      String k = m.id() + "|" + m.at() + "|" + Math.round(maxW) + "|" + size + "|" + font + "|" + this.gstr("Font", "Inter");
      List<String> l = this.chatWrapCache.get(k);
      if (l == null) {
         if (this.chatWrapCache.size() > 900) {
            this.chatWrapCache.clear();
         }

         l = this.chatWrap(c, font, size, m.text(), maxW);
         this.chatWrapCache.put(k, l);
      }

      return l;
   }

   private void drawChatBox(Canvas c, ClickGui.Panel p, String id, float x, float y, float w, float h, float fs) {
      this.drawChatBox(c, p, id, null, x, y, w, h, fs);
   }

   private void drawChatBox(Canvas c, ClickGui.Panel p, String id, String dm, float x, float y, float w, float h, float fs) {
      boolean panel = p != null;
      TextField field = dm == null ? this.chatField : this.dmField;
      if (dm == null) {
         GlobalChat.markRead();
      } else {
         DirectMessages.markRead(dm);
      }

      float R = panel ? Math.max(2.0F, this.gnum("Panel Rounding", 3.0)) : 12.0F;
      float pad = panel ? 5.0F : 12.0F;
      float inH = panel ? Math.max(18.0F, this.setRowH() + 3.0F) : 38.0F;
      float lineH = fs * 1.4F;
      c.rect(x, y, w, h, R, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.03F), panel ? 0.55F : 0.62F));
      c.stroke(x, y, w, h, R, 1.0F, Theme.a(this.th.border, 0.55F));
      float top = y + pad * 0.6F;
      float bottom = y + h - inH - pad * 1.6F;
      float viewH = Math.max(10.0F, bottom - top);
      float[] sc = this.chatScroll.computeIfAbsent(id, k -> new float[3]);
      sc[1] = Math.max(0.0F, Math.min(sc[2], sc[1]));
      sc[0] = this.an.to("chatSc:" + id, sc[1], 18.0F);
      this.chatWheel = d -> sc[1] = Math.max(0.0F, Math.min(sc[2], sc[1] + d * lineH * 3.0F));
      List<GlobalChat.Message> msgs = dm == null ? GlobalChat.messages() : DirectMessages.messages(dm);
      String blocked = dm == null ? GlobalChat.blockedReason() : DirectMessages.blockedReason(dm);
      boolean times = FriendManager.bool("Show Times");
      float tx = x + pad + 3.0F;
      float tw = w - pad * 2.0F - 8.0F;
      c.save();
      c.scissor(x, top, w, viewH);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sH = this.clipH;
      this.intersectClip(x, top, w, viewH);
      this.chatHit(x, top, w, viewH, null, null);
      float yb = bottom + sc[0];
      float total = 0.0F;

      for (int i = msgs.size() - 1; i >= 0; i--) {
         GlobalChat.Message m = msgs.get(i);
         GlobalChat.Message older = i > 0 ? msgs.get(i - 1) : null;
         boolean sys = m.kind() == GlobalChat.Kind.SYSTEM;
         boolean header = !sys && (older == null || older.kind() == GlobalChat.Kind.SYSTEM || !Objects.equals(older.uuid(), m.uuid()) || m.at() - older.at() > 180000L);
         float textFs = sys ? fs - 1.0F : fs;
         List<String> lines = this.chatLines(c, m, sys ? "inter-med" : "inter", textFs, sys ? tw - 6.0F : tw);
         float bh = lines.size() * lineH + (header ? lineH + 4.0F : 1.0F) + (sys ? 4 : 0);
         float by = yb - bh;
         total += bh;
         if (by < bottom + 1.0F && yb > top - 1.0F) {
            this.drawChatMessage(c, p, id, m, lines, header, x + 3.0F, tx, by, w - 6.0F, bh, lineH, fs, textFs, times, dm != null);
         }

         yb = by;
      }

      sc[2] = Math.max(0.0F, total - viewH + 6.0F);
      if (msgs.isEmpty()) {
         String msg = blocked != null ? blocked : (dm != null ? "No messages with " + DirectMessages.displayName(dm) + " yet." : "No messages yet. Say hi!");
         c.text("inter-med", fs, x + w / 2.0F, top + viewH / 2.0F - (panel ? 0 : 8), 18, this.th.muted, this.ellipsize(c, "inter-med", fs, msg, w - 16.0F));
         if (!panel && blocked == null) {
            String prefix = FriendManager.str(dm == null ? "Chat Prefix" : "DM Prefix").trim();
            if (!prefix.isEmpty()) {
               c.text(
                  "inter",
                  fs - 1.5F,
                  x + w / 2.0F,
                  top + viewH / 2.0F + 12.0F,
                  18,
                  Theme.a(this.th.muted, 0.8F),
                  dm == null
                     ? "You can also type " + prefix + "message in Minecraft chat"
                     : "You can also type " + prefix + DirectMessages.displayName(dm).replace(' ', '_') + " message in Minecraft chat"
               );
            }
         }
      }

      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sH;
      c.restore();
      if (sc[1] > lineH) {
         String lbl = "Jump to latest";
         float pfs = fs - 1.0F;
         float pw = c.textWidth("inter-med", pfs, lbl) + 22.0F;
         float ph = pfs + 9.0F;
         float px = x + w / 2.0F - pw / 2.0F;
         float py = bottom - ph - 4.0F;
         boolean hv = this.chatIn(p, px, py, pw, ph);
         c.shadow(px, py + 2.0F, pw, ph, ph / 2.0F, 10.0F, Theme.a(-16777216, 0.4F));
         c.rect(px, py, pw, ph, ph / 2.0F, Theme.a(this.th.accent, hv ? 1.0F : 0.88F));
         Icons.draw(c, "down", px + 10.0F, py + ph / 2.0F, pfs, Theme.onColor(this.th.accent));
         c.text("inter-med", pfs, px + pw / 2.0F + 5.0F, py + ph / 2.0F, 18, Theme.onColor(this.th.accent), lbl);
         this.hit(px, py, pw, ph, () -> sc[1] = 0.0F, null);
      }

      float fx = x + pad;
      float fy = y + h - inH - pad;
      float fw = w - pad * 2.0F;
      float fr = panel ? 3.0F : this.ctlR() + 1.0F;
      if (blocked != null) {
         boolean canFix = !FriendManager.bool("Online Features") || dm == null && !FriendManager.bool("Global Chat");
         boolean hv = this.chatIn(p, fx, fy, fw, inH);
         c.rect(fx, fy, fw, inH, fr, Theme.a(Theme.mix(this.th.bg, this.th.text, canFix && hv ? 0.09F : 0.05F), 0.8F));
         c.stroke(fx, fy, fw, inH, fr, 1.0F, Theme.a(canFix ? this.th.accent : this.th.border, canFix ? 0.4F + 0.3F * (hv ? 1 : 0) : 0.6F));
         String lbl = canFix ? blocked + " · click to turn on" : (PresenceClient.state() == PresenceClient.State.CONNECTED ? blocked : PresenceClient.statusLine());
         c.text("inter-med", fs - 0.5F, fx + fw / 2.0F, fy + inH / 2.0F, 18, canFix ? this.th.accent : this.th.muted, this.ellipsize(c, "inter-med", fs - 0.5F, lbl, fw - 12.0F));
         if (canFix) {
            this.hit(fx, fy, fw, inH, () -> {
               FriendManager.setSetting("Online Features", true);
               FriendManager.setSetting("Global Chat", true);
               this.toast("Global Chat on");
            }, null);
         }
      } else {
         float bsz = inH;
         float fieldW = fw - bsz - 4.0F;
         boolean editing = this.focused == field;
         float ft = this.an.to("chatF:" + id, editing ? 1.0F : 0.0F, 14.0F);
         c.rect(fx, fy, fieldW, inH, fr, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.88F));
         c.stroke(fx, fy, fieldW, inH, fr, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, ft));
         if (!panel && ft > 0.01F) {
            c.shadow(fx, fy, fieldW, inH, fr, 12.0F, Theme.a(this.th.accent, 0.2F * ft));
         }

         String t = field.text;
         int left = 256 - t.length();
         String counter = left <= 40 ? String.valueOf(left) : null;
         float avail = fieldW - 16.0F - (counter != null ? c.textWidth("inter-med", fs - 1.5F, counter) + 6.0F : 0.0F);
         float tWidth = c.textWidth("inter", fs, t);
         float tx0 = fx + 8.0F - (editing ? Math.max(0.0F, tWidth - avail) : 0.0F);
         c.save();
         c.scissor(fx + 4.0F, fy, avail + 6.0F, inH);
         if (t.isEmpty() && !editing) {
            c.text(
               "inter",
               fs,
               fx + 8.0F,
               fy + inH / 2.0F,
               17,
               this.th.muted,
               this.ellipsize(c, "inter", fs, dm != null ? "Message " + DirectMessages.displayName(dm) + "…" : (panel ? "Message…" : "Message everyone on Clarity…"), avail)
            );
         } else {
            c.text("inter", fs, tx0, fy + inH / 2.0F, 17, this.th.text, t);
            if (editing) {
               this.caret(c, "inter", fs, tx0, fy + inH / 2.0F, field);
            }
         }

         c.restore();
         if (counter != null) {
            c.text("inter-med", fs - 1.5F, fx + fieldW - 6.0F, fy + inH / 2.0F, 20, left < 10 ? -43920 : this.th.muted, counter);
         }

         this.hit(fx, fy, fieldW, inH, () -> {
            if (dm != null) {
               this.dmTarget = dm;
            }

            this.focus(field, null);
         }, null);
         if (editing) {
            this.drawEmojiSuggestions(c, field, fx, fy, fieldW, fs, panel);
         } else if (this.emojiSugField == field) {
            this.emojiSug = List.of();
         }

         float bx = fx + fieldW + 4.0F;
         boolean can = !t.isBlank();
         boolean bhv = this.chatIn(p, bx, fy, bsz, inH);
         float bt = this.an.to("chatSend:" + id, can ? 1.0F : 0.0F, 14.0F);
         c.rect(bx, fy, bsz, inH, fr, Theme.mix(Theme.a(Theme.mix(this.th.bg, this.th.text, 0.07F), 0.88F), Theme.a(this.th.accent, bhv ? 1.0F : 0.88F), bt));
         Icons.draw(c, "send", bx + bsz / 2.0F + 1.0F, fy + inH / 2.0F, Math.min(bsz * 0.52F, 15.0F), Theme.mix(this.th.muted, Theme.onColor(this.th.accent), bt));
         this.hit(bx, fy, bsz, inH, () -> {
            if (dm != null) {
               this.dmTarget = dm;
               this.sendDmField();
            } else {
               this.sendChatField();
            }
         }, null);
      }
   }

   private void drawChatMessage(
      Canvas c,
      ClickGui.Panel p,
      String boxId,
      GlobalChat.Message m,
      List<String> lines,
      boolean header,
      float rx,
      float tx,
      float y,
      float rw,
      float bh,
      float lineH,
      float fs,
      float textFs,
      boolean times,
      boolean isDm
   ) {
      if (m.kind() == GlobalChat.Kind.SYSTEM) {
         float yy = y + 2.0F + lineH / 2.0F;

         for (String l : lines) {
            c.text("inter-med", textFs, rx + rw / 2.0F, yy, 18, Theme.a(this.th.muted, 0.9F), l);
            yy += lineH;
         }
      } else {
         String menuId = "chatmsg:" + boxId + ":" + m.id();
         boolean open = this.pop != null && menuId.equals(this.pop.id);
         boolean hv = this.chatIn(p, rx, y, rw, bh);
         if (m.mention()) {
            c.rect(rx, y, rw, bh, 3.0F, Theme.a(-670398, hv ? 0.14F : 0.09F));
            c.rect(rx, y + 1.0F, 2.0F, bh - 2.0F, 1.0F, Theme.a(-670398, 0.85F));
         } else if (hv || open) {
            c.rect(rx, y, rw, bh, 3.0F, Theme.a(this.th.text, open ? 0.07F : 0.04F));
         }

         this.chatHit(rx, y, rw, bh, null, () -> {
            if (isDm) {
               this.dmMenu(menuId, m, rx, y, rw, bh);
            } else {
               this.openChatMenu(menuId, m, rx, y, rw, bh);
            }
         });
         float yy = y + (header ? 4 : 0);
         if (header) {
            int nc = 0xFF000000 | GlobalChat.nameColour(m);
            String nm = m.name();
            float nameW = c.textWidth("inter-sb", fs, nm);
            boolean nhv = this.chatIn(p, tx - 2.0F, yy, nameW + 4.0F, lineH);
            c.text("inter-sb", fs, tx, yy + lineH / 2.0F, 17, nc, nm);
            if (GlobalChat.isAdmin(m)) {
               c.text("inter-sb", fs, tx + 0.6F, yy + lineH / 2.0F, 17, nc, nm);
            }

            if (nhv) {
               c.line(tx, yy + lineH / 2.0F + fs * 0.62F, tx + nameW, yy + lineH / 2.0F + fs * 0.62F, 1.0F, Theme.a(nc, 0.6F));
            }

            float nx = tx + nameW + 5.0F;
            boolean friend = !isDm && m.kind() == GlobalChat.Kind.USER && FriendManager.get(m.uuid(), m.name()) != null;
            if (m.kind() == GlobalChat.Kind.SELF || friend) {
               String tag = m.kind() == GlobalChat.Kind.SELF ? "you" : "friend";
               float tfs = Math.max(7.0F, fs - 3.0F);
               float tgw = c.textWidth("inter-sb", tfs, tag) + 8.0F;
               float tgh = tfs + 4.0F;
               c.rect(nx, yy + lineH / 2.0F - tgh / 2.0F, tgw, tgh, tgh / 2.0F, Theme.a(nc, 0.16F));
               c.text("inter-sb", tfs, nx + tgw / 2.0F, yy + lineH / 2.0F, 18, nc, tag);
               nx += tgw + 5.0F;
            }

            if (times) {
               c.text("inter", fs - 2.0F, nx, yy + lineH / 2.0F + 0.5F, 17, Theme.a(this.th.muted, 0.75F), chatTime(m.at()));
            }

            if (m.kind() != GlobalChat.Kind.SELF && !isDm) {
               this.chatHit(tx - 2.0F, yy, nameW + 4.0F, lineH, () -> this.openChatMenu(menuId, m, rx, y, rw, bh), () -> this.openChatMenu(menuId, m, rx, y, rw, bh));
            }

            yy += lineH;
         } else if (hv && times) {
            c.text("inter", fs - 2.0F, rx + rw - 4.0F, yy + lineH / 2.0F, 20, Theme.a(this.th.muted, 0.7F), chatTime(m.at()));
         }

         int col = m.kind() == GlobalChat.Kind.SELF ? Theme.a(this.th.text, 0.9F) : this.th.text;

         for (String l : lines) {
            c.text("inter", textFs, tx, yy + lineH / 2.0F, 17, col, l);
            yy += lineH;
         }
      }
   }

   private void dmMenu(String menuId, GlobalChat.Message m, float x, float y, float w, float h) {
      if (this.pop != null && menuId.equals(this.pop.id)) {
         this.pop = null;
      } else {
         float mw = this.compact() ? Math.min(w, 140.0F) : 180.0F;
         this.openList(menuId, x + w - mw, y, mw, h, mw, List.of("Copy message"), "", v -> {
            this.store.setClipboard(m.text());
            this.toast("Copied");
         });
      }
   }

   private void openChatMenu(String menuId, GlobalChat.Message m, float x, float y, float w, float h) {
      if (this.pop != null && menuId.equals(this.pop.id)) {
         this.pop = null;
      } else {
         boolean self = m.kind() == GlobalChat.Kind.SELF;
         Friend fr = self ? null : FriendManager.get(m.uuid(), m.name());
         boolean confirmed = fr != null && PresenceClient.confirmed(fr);
         boolean online = PresenceClient.state() == PresenceClient.State.CONNECTED;
         List<String> opts = new ArrayList<>();
         if (!self) {
            if (confirmed) {
               opts.add("Open in DM");
            } else if (fr != null) {
               opts.add("DM (once they add you back)");
            }

            if (fr == null) {
               opts.add("Add friend");
            } else if (!confirmed && online) {
               opts.add("Resend friend request");
            }

            opts.add("Mention");
            opts.add("Hide their messages");
         }

         opts.add("Copy message");
         if (!self) {
            opts.add("Copy name");
         }

         if (GlobalChat.isAdmin()) {
            opts.add("Delete (mod)");
            if (!self) {
               opts.add("Mute 10 min (mod)");
               opts.add("Mute 1 day (mod)");
            }
         }

         float mw = this.compact() ? Math.min(Math.max(w, 120.0F), 170.0F) : 220.0F;
         this.openList(menuId, x + w - mw, y, mw, h, mw, opts, "", v -> {
            switch (v) {
               case "Open in DM":
                  this.openDm(FriendManager.get(m.uuid(), m.name()));
                  break;
               case "DM (once they add you back)":
                  this.toast(m.name() + " has to add you back before you can DM them");
                  break;
               case "Add friend": {
                  FriendManager.add(m.uuid(), m.name());
                  String e = online ? PresenceClient.sendRequest(m.name(), m.uuid(), true) : null;
                  this.toast(e == null ? "Added " + m.name() + (online ? ", request sent" : "") : "Added " + m.name() + " (" + e + ")");
                  break;
               }
               case "Resend friend request": {
                  String e = PresenceClient.sendRequest(m.name(), m.uuid(), false);
                  this.toast(e == null ? "Request sent to " + m.name() : e);
                  break;
               }
               case "Mention":
                  this.mentionInChat(m.name());
                  break;
               case "Hide their messages":
                  GlobalChat.setHidden(m.uuid(), m.name(), true);
                  this.toast("Hid " + m.name() + " (undo in chat settings)");
                  break;
               case "Copy message":
                  this.store.setClipboard(m.text());
                  this.toast("Copied");
                  break;
               case "Copy name":
                  this.store.setClipboard(m.name());
                  this.toast("Copied " + m.name());
                  break;
               case "Delete (mod)":
                  GlobalChat.delete(m);
                  break;
               case "Mute 10 min (mod)":
                  GlobalChat.mute(m, 10);
                  break;
               case "Mute 1 day (mod)":
                  GlobalChat.mute(m, 1440);
            }
         });
      }
   }

   private float drawFriendsPageTabs(Canvas c, float x, float y, float w) {
      float h = 34.0F;
      float segW = 128.0F;
      float tw = segW * PAGE_TABS.length;
      c.rect(x, y, tw, h, h / 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.8F));
      c.stroke(x, y, tw, h, h / 2.0F, 1.0F, Theme.a(this.th.border, 0.6F));
      int idx = Math.max(0, Arrays.asList(PAGE_TABS).indexOf(this.friendsTab));
      float sx = this.an.to("frPageTab", x + idx * segW, 16.0F);
      c.shadow(sx + 3.0F, y + 3.0F, segW - 6.0F, h - 6.0F, (h - 6.0F) / 2.0F, 10.0F, Theme.a(this.th.accent, 0.3F));
      c.gradient(sx + 3.0F, y + 3.0F, segW - 6.0F, h - 6.0F, (h - 6.0F) / 2.0F, this.th.accent, this.th.accent2, true);

      for (int i = 0; i < PAGE_TABS.length; i++) {
         String t = PAGE_TABS[i];
         float sxi = x + i * segW;
         boolean act = i == idx;
         float hv = this.an.to("frPageTabH:" + t, !act && this.in(sxi, y, segW, h) ? 1.0F : 0.0F, 16.0F);
         int col = act ? Theme.onColor(this.th.accent) : Theme.mix(this.th.muted, this.th.text, hv);
         Icons.draw(c, "Chat".equals(t) ? "chat" : ("Messages".equals(t) ? "send" : "Friends"), sxi + 26.0F, y + h / 2.0F, 14.0F, col);
         c.text("inter-sb", 12.5F, sxi + 40.0F, y + h / 2.0F, 17, col, t);
         int badge = "Chat".equals(t) ? (act ? 0 : GlobalChat.unread()) : ("Messages".equals(t) ? DirectMessages.totalUnread() : PresenceClient.incomingRequests().size());
         if (badge > 0) {
            this.badge(c, sxi + 44.0F + c.textWidth("inter-sb", 12.5F, t), y + h / 2.0F, 10.0F, badge);
         }

         this.hit(sxi, y, segW, h, () -> {
            if (!t.equals(this.friendsTab)) {
               this.friendsTab = t;
               this.resetScroll();
            }
         }, null);
      }

      if ("Chat".equals(this.friendsTab)) {
         String prefix = FriendManager.str("Chat Prefix").trim();
         String hint = prefix.isEmpty() ? "Everyone on Clarity, in one room" : "In game: start a chat message with " + prefix;
         c.text("inter", 11.5F, x + w, y + h / 2.0F, 20, this.th.muted, this.ellipsize(c, "inter", 11.5F, hint, w - tw - 20.0F));
      }

      return y + h;
   }

   private boolean dmValid(String uuid) {
      if (uuid == null) {
         return false;
      }

      for (DirectMessages.Conversation cv : DirectMessages.conversations()) {
         if (cv.uuid().equals(uuid)) {
            return true;
         }
      }

      return false;
   }

   private int nameColourOf(String uuid) {
      Friend f = FriendManager.get(uuid, null);
      int rgb = f == null ? -1 : FriendManager.nameColour(f);
      return rgb >= 0 ? 0xFF000000 | rgb : this.th.text;
   }

   private void drawMessagesPage(Canvas c, float x, float y, float w, float h) {
      List<DirectMessages.Conversation> convs = DirectMessages.conversations();
      if (!this.dmValid(this.dmTarget)) {
         this.dmTarget = convs.isEmpty() ? null : convs.get(0).uuid();
      }

      float lw = Math.min(250.0F, w * 0.32F);
      c.rect(x, y, lw, h, 12.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.03F), 0.62F));
      c.stroke(x, y, lw, h, 12.0F, 1.0F, Theme.a(this.th.border, 0.55F));
      c.save();
      c.scissor(x, y, lw, h);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sH = this.clipH;
      this.intersectClip(x, y, lw, h);
      float ry = y + 6.0F;
      float rh = 52.0F;
      if (convs.isEmpty()) {
         for (String l : this.wrap(c, "inter-med", 12.0F, "No one to message yet. You can DM friends once they've added you back.", lw - 30.0F)) {
            c.text("inter-med", 12.0F, x + lw / 2.0F, ry + 30.0F, 18, this.th.muted, l);
            ry += 18.0F;
         }
      }

      for (DirectMessages.Conversation cv : convs) {
         boolean sel = cv.uuid().equals(this.dmTarget);
         float hv = this.an.to("dmc:" + cv.uuid(), this.in(x + 6.0F, ry, lw - 12.0F, rh) && !sel ? 1.0F : 0.0F, 16.0F);
         if (sel) {
            c.gradient(x + 6.0F, ry, lw - 12.0F, rh, 10.0F, Theme.a(this.th.accent, 0.22F), Theme.a(this.th.accent2, 0.08F), true);
         } else if (hv > 0.01F) {
            c.rect(x + 6.0F, ry, lw - 12.0F, rh, 10.0F, Theme.a(this.th.text, 0.05F * hv));
         }

         int nc = this.nameColourOf(cv.uuid());
         float ax = x + 30.0F;
         float ay = ry + rh / 2.0F;
         c.circle(ax, ay, 15.0F, Theme.a(nc, 0.16F));
         c.ring(ax, ay, 15.0F, 1.2F, Theme.a(nc, 0.55F));
         c.text("inter-sb", 13.0F, ax, ay, 18, nc, cv.name().isEmpty() ? "?" : cv.name().substring(0, 1).toUpperCase(Locale.ROOT));
         Friend f = FriendManager.get(cv.uuid(), null);
         if (f != null) {
            int sc = this.friendStatusColour(f);
            c.circle(ax + 11.0F, ay + 11.0F, 5.0F, 0xFF000000 | this.th.bg);
            c.circle(ax + 11.0F, ay + 11.0F, 3.4F, sc);
         }

         float tx = x + 54.0F;
         float tw = lw - 54.0F - 14.0F - (cv.unread() > 0 ? 26 : 0);
         c.text("inter-sb", 13.0F, tx, ry + 19.0F, 17, sel ? this.th.text : nc, this.ellipsize(c, "inter-sb", 13.0F, cv.name(), tw));
         String sub = cv.lastText().isEmpty() ? (f != null ? this.friendStatus(f) : "") : cv.lastText();
         c.text("inter", 11.5F, tx, ry + 36.0F, 17, cv.unread() > 0 ? this.th.text : this.th.muted, this.ellipsize(c, "inter", 11.5F, sub, tw));
         if (cv.unread() > 0) {
            this.badge(c, x + lw - 38.0F, ry + rh / 2.0F, 10.0F, cv.unread());
         }

         String u = cv.uuid();
         this.hit(x + 6.0F, ry, lw - 12.0F, rh, () -> {
            this.dmTarget = u;
            this.focus(this.dmField, null);
         }, null);
         ry += rh + 4.0F;
      }

      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sH;
      c.restore();
      float cx = x + lw + 14.0F;
      float cw = w - lw - 14.0F;
      if (this.dmTarget == null) {
         c.rect(cx, y, cw, h, 12.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.03F), 0.62F));
         c.stroke(cx, y, cw, h, 12.0F, 1.0F, Theme.a(this.th.border, 0.55F));
         c.text("inter-med", 13.0F, cx + cw / 2.0F, y + h / 2.0F, 18, this.th.muted, "Pick a friend to message");
      } else {
         Friend f = FriendManager.get(this.dmTarget, null);
         int nc = this.nameColourOf(this.dmTarget);
         c.text("inter-sb", 15.0F, cx + 4.0F, y + 13.0F, 17, nc, DirectMessages.displayName(this.dmTarget));
         if (f != null) {
            float nw = c.textWidth("inter-sb", 15.0F, DirectMessages.displayName(this.dmTarget));
            c.circle(cx + 14.0F + nw, y + 13.0F, 3.4F, this.friendStatusColour(f));
            c.text("inter", 11.5F, cx + 22.0F + nw, y + 13.0F, 17, this.th.muted, this.ellipsize(c, "inter", 11.5F, this.friendStatus(f), cw - nw - 40.0F));
         }

         this.drawChatBox(c, null, "dm:page", this.dmTarget, cx, y + 30.0F, cw, h - 30.0F, 13.0F);
      }
   }

   private float drawPanelDms(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      List<DirectMessages.Conversation> convs = DirectMessages.conversations();
      if (this.dmTarget != null && !this.dmValid(this.dmTarget)) {
         this.dmTarget = null;
      }

      if (this.dmTarget != null) {
         boolean hv = this.pin(p, x - 2.0F, y, w + 4.0F, rh);
         if (hv) {
            c.rect(x - 2.0F, y, w + 4.0F, rh, 3.0F, Theme.a(this.th.text, 0.05F));
         }

         c.text("inter-med", fs - 1.5F, x + 2.0F, y + rh / 2.0F, 17, this.th.muted, "‹");
         int nc = this.nameColourOf(this.dmTarget);
         c.text("inter-sb", fs, x + 12.0F, y + rh / 2.0F, 17, nc, this.ellipsize(c, "inter-sb", fs, DirectMessages.displayName(this.dmTarget), w - 30.0F));
         Friend f = FriendManager.get(this.dmTarget, null);
         if (f != null) {
            c.circle(x + w - 4.0F, y + rh / 2.0F, 2.6F, this.friendStatusColour(f));
         }

         this.hit(x - 2.0F, y, w + 4.0F, rh, () -> {
            this.dmTarget = null;
            this.focused = null;
         }, null);
         y += rh + 2.0F;
         float ch = Math.max(140.0F, this.gnum("Panel Chat Height", 270.0));
         this.drawChatBox(c, p, "dm:panel", this.dmTarget, x - 2.0F, y, w + 4.0F, ch, this.pFont() - 0.5F);
         return y + ch + 4.0F;
      } else if (!convs.isEmpty()) {
         for (DirectMessages.Conversation cv : convs) {
            float H = Math.max(24.0F, rh * 1.7F);
            boolean hv = this.pin(p, x - 2.0F, y, w + 4.0F, H);
            if (hv) {
               c.rect(x - 2.0F, y, w + 4.0F, H, 3.0F, Theme.a(this.th.text, 0.05F));
            }

            int nc = this.nameColourOf(cv.uuid());
            float ar = H * 0.32F;
            float ax = x + ar + 1.0F;
            float ay = y + H / 2.0F;
            c.circle(ax, ay, ar, Theme.a(nc, 0.16F));
            c.ring(ax, ay, ar, 1.0F, Theme.a(nc, 0.5F));
            c.text("inter-sb", ar * 1.05F, ax, ay, 18, nc, cv.name().isEmpty() ? "?" : cv.name().substring(0, 1).toUpperCase(Locale.ROOT));
            float tx = ax + ar + 6.0F;
            float maxW = w - (tx - x) - (cv.unread() > 0 ? 20 : 2);
            c.text("inter-sb", fs, tx, y + H * 0.34F, 17, nc, this.ellipsize(c, "inter-sb", fs, cv.name(), maxW));
            Friend f = FriendManager.get(cv.uuid(), null);
            String sub = cv.lastText().isEmpty() ? (f != null ? this.friendStatus(f) : "") : cv.lastText();
            c.text("inter", fs - 1.5F, tx, y + H * 0.7F, 17, cv.unread() > 0 ? this.th.text : this.th.muted, this.ellipsize(c, "inter", fs - 1.5F, sub, maxW));
            if (cv.unread() > 0) {
               this.badge(c, x + w - 16.0F, ay, Math.max(6.0F, fs - 2.5F), cv.unread());
            }

            String u = cv.uuid();
            this.hit(x - 2.0F, y, w + 4.0F, H, () -> {
               this.dmTarget = u;
               this.focus(this.dmField, null);
            }, null);
            y += H + 2.0F;
         }

         return y + 2.0F;
      } else {
         for (String l : this.wrap(c, "inter", fs - 0.5F, "You can DM friends once they've added you back.", w - 4.0F)) {
            c.text("inter", fs - 0.5F, x + w / 2.0F, y + rh / 2.0F, 18, this.th.muted, l);
            y += rh - 2.0F;
         }

         return y + 4.0F;
      }
   }

   private void badge(Canvas c, float x, float cy, float fs, int n) {
      String b = n > 99 ? "99+" : String.valueOf(n);
      float bh = fs + 4.0F;
      float bw = Math.max(bh, c.textWidth("inter-sb", fs, b) + 8.0F);
      c.rect(x, cy - bh / 2.0F, bw, bh, bh / 2.0F, -43920);
      c.text("inter-sb", fs, x + bw / 2.0F, cy, 18, -1, b);
   }

   private boolean typingField(TextField f) {
      return f == this.nameField
         || f == this.addField
         || f == this.chatField
         || f == this.dmField
         || f == this.cfgName
         || f == this.cfgDesc
         || f == this.cfgServer
         || f == this.cfgCode
         || f == this.cfgSearch;
   }

   private static String ago(long at) {
      long s = Math.max(0L, (System.currentTimeMillis() - at) / 1000L);
      if (s < 60L) {
         return "just now";
      } else if (s < 3600L) {
         return s / 60L + "m ago";
      } else if (s < 86400L) {
         return s / 3600L + "h ago";
      } else {
         return s < 2592000L ? s / 86400L + "d ago" : new SimpleDateFormat("d MMM yyyy", Locale.ROOT).format(new Date(at));
      }
   }

   private static String compact(int n) {
      if (n < 1000) {
         return String.valueOf(n);
      } else {
         return n < 10000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0F) : n / 1000 + "k";
      }
   }

   private void cloudToast(CloudConfigs.Result r) {
      this.toast(r.error() != null ? r.error() : r.message());
   }

   private void cloudTick(boolean mine, boolean browse) {
      if (CloudConfigs.available()) {
         long now = System.currentTimeMillis();
         if (mine && !CloudConfigs.MINE.loading && (CloudConfigs.MINE.loadedAt == 0L || now - CloudConfigs.MINE.loadedAt > 60000L)) {
            CloudConfigs.refreshMine();
         }

         if (browse) {
            String t = this.cfgSearch.text.trim();
            if (!t.equals(this.searchSeen)) {
               this.searchSeen = t;
               this.searchEditAt = this.time;
            }

            if (!t.equals(CloudConfigs.query) && this.time - this.searchEditAt > 0.35F) {
               CloudConfigs.query = t;
               CloudConfigs.refreshBrowse(false);
            } else if (!CloudConfigs.BROWSE.loading && (CloudConfigs.BROWSE.loadedAt == 0L || now - CloudConfigs.BROWSE.loadedAt > 60000L)) {
               CloudConfigs.refreshBrowse(false);
            }
         }
      }
   }

   private List<String> uploadSources() {
      List<String> l = new ArrayList<>();
      l.add("Current setup");
      l.addAll(this.store.profiles());
      return l;
   }

   private void doUpload() {
      String src = this.uploadSources().contains(this.uploadSource) ? this.uploadSource : "Current setup";
      boolean current = "Current setup".equals(src);
      String name = this.cfgName.text.trim();
      if (name.isEmpty()) {
         name = current ? (this.store.activeProfile().isEmpty() ? "" : this.store.activeProfile()) : src;
      }

      if (name.isEmpty()) {
         this.toast("Give the config a name");
         this.focus(this.cfgName, null);
      } else {
         JsonObject snap = current ? CloudConfigs.currentSnapshot() : CloudConfigs.profileSnapshot(src);
         String link = current ? (this.store.activeProfile().isEmpty() ? null : this.store.activeProfile()) : src;
         String n = name;
         CloudConfigs.upload(n, this.cfgDesc.text.trim(), this.uploadVisibility, this.cfgServer.text.trim(), snap, link, null, r -> {
            this.cloudToast(r);
            if (r.error() == null) {
               this.cfgName.set("");
               this.cfgDesc.set("");
               this.cfgServer.set("");
            }
         });
         this.toast("Uploading " + n + "…");
      }
   }

   private void updateCloud(CloudConfigs.Meta m) {
      String prof = CloudConfigs.profileFor(m.id());
      JsonObject snap = prof != null ? CloudConfigs.profileSnapshot(prof) : null;
      if (snap == null) {
         snap = CloudConfigs.currentSnapshot();
         prof = this.store.activeProfile().isEmpty() ? null : this.store.activeProfile();
      }

      CloudConfigs.upload(null, null, null, null, snap, prof, m.id(), this::cloudToast);
   }

   private void loadCode() {
      String code = this.cfgCode.text.trim();
      if (code.isEmpty()) {
         this.toast("Type a config code");
         this.focus(this.cfgCode, null);
      } else {
         CloudConfigs.load(code, true, r -> {
            this.cloudToast(r);
            if (r.error() == null) {
               this.cfgCode.set("");
            }
         });
      }
   }

   private void startUpload(String profile) {
      this.uploadSource = profile;
      this.cfgName.set(profile);
      this.profilesTab = "My Cloud";
      this.pConfigsTab = "Cloud";
      this.resetScroll();
      this.toast("Pick a visibility, then Upload");
   }

   private void copyCode(CloudConfigs.Meta m) {
      this.store.setClipboard(m.id());
      this.toast("Copied code " + m.id());
   }

   private float pillTabs(Canvas c, String id, float x, float y, String[] tabs, String[] icons, String current, int[] badges, Consumer<String> pick) {
      float h = 34.0F;
      float[] segs = new float[tabs.length];
      float tw = 0.0F;

      for (int i = 0; i < tabs.length; i++) {
         segs[i] = c.textWidth("inter-sb", 12.5F, tabs[i]) + 62.0F;
         tw += segs[i];
      }

      c.rect(x, y, tw, h, h / 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.8F));
      c.stroke(x, y, tw, h, h / 2.0F, 1.0F, Theme.a(this.th.border, 0.6F));
      int idx = Math.max(0, Arrays.asList(tabs).indexOf(current));
      float sx0 = x;

      for (int i = 0; i < idx; i++) {
         sx0 += segs[i];
      }

      float sx = this.an.to(id + ":x", sx0, 16.0F);
      float sw_ = this.an.to(id + ":w", segs[idx], 16.0F);
      c.shadow(sx + 3.0F, y + 3.0F, sw_ - 6.0F, h - 6.0F, (h - 6.0F) / 2.0F, 10.0F, Theme.a(this.th.accent, 0.3F));
      c.gradient(sx + 3.0F, y + 3.0F, sw_ - 6.0F, h - 6.0F, (h - 6.0F) / 2.0F, this.th.accent, this.th.accent2, true);
      float px = x;

      for (int i = 0; i < tabs.length; i++) {
         String t = tabs[i];
         boolean act = i == idx;
         float hv = this.an.to(id + ":h:" + t, !act && this.in(px, y, segs[i], h) ? 1.0F : 0.0F, 16.0F);
         int col = act ? Theme.onColor(this.th.accent) : Theme.mix(this.th.muted, this.th.text, hv);
         Icons.draw(c, icons[i], px + 24.0F, y + h / 2.0F, 14.0F, col);
         c.text("inter-sb", 12.5F, px + 38.0F, y + h / 2.0F, 17, col, t);
         if (badges != null && badges[i] > 0) {
            this.badge(c, px + 42.0F + c.textWidth("inter-sb", 12.5F, t), y + h / 2.0F, 10.0F, badges[i]);
         }

         String target = t;
         this.hit(px, y, segs[i], h, () -> pick.accept(target), null);
         px += segs[i];
      }

      return y + h;
   }

   private void inputBox(Canvas c, String id, TextField f, float x, float y, float w, float h, String placeholder) {
      boolean editing = this.focused == f;
      float ft = this.an.to("ib:" + id, editing ? 1.0F : 0.0F, 14.0F);
      c.rect(x, y, w, h, this.ctlR() + 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.85F));
      c.stroke(x, y, w, h, this.ctlR() + 1.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, ft));
      String t = f.text;
      float avail = w - 22.0F;
      float tWidth = c.textWidth("inter", 12.5F, t);
      float tx0 = x + 12.0F - (editing ? Math.max(0.0F, tWidth - avail) : 0.0F);
      c.save();
      c.scissor(x + 6.0F, y, w - 12.0F, h);
      if (t.isEmpty() && !editing) {
         c.text("inter", 12.5F, x + 12.0F, y + h / 2.0F, 17, this.th.muted, this.ellipsize(c, "inter", 12.5F, placeholder, avail));
      } else {
         c.text("inter", 12.5F, tx0, y + h / 2.0F, 17, this.th.text, t);
         if (editing) {
            this.caret(c, "inter", 12.5F, tx0, y + h / 2.0F, f);
         }
      }

      c.restore();
      this.hit(x, y, w, h, () -> this.focus(f, null), null);
   }

   private float cloudUnavailable(Canvas c, float x, float y, float w) {
      String why = CloudConfigs.unavailableReason();
      float h = 64.0F;
      c.rect(x, y, w, h, 12.0F, Theme.a(Theme.mix(this.th.bg, this.th.accent, 0.06F), 0.7F));
      c.stroke(x, y, w, h, 12.0F, 1.0F, Theme.a(this.th.border, 0.6F));
      Icons.draw(c, "cloud", x + 30.0F, y + h / 2.0F, 20.0F, this.th.muted);
      c.text("inter-sb", 13.5F, x + 54.0F, y + 24.0F, 17, this.th.text, this.ellipsize(c, "inter-sb", 13.5F, why == null ? "" : why, w - 200.0F));
      c.text("inter", 11.5F, x + 54.0F, y + 43.0F, 17, this.th.muted, this.ellipsize(c, "inter", 11.5F, "Online configs use your Clarity connection. " + PresenceClient.statusLine(), w - 200.0F));
      if (!FriendManager.bool("Online Features")) {
         this.button(c, "cloud/on", x + w - 136.0F, y + h / 2.0F - 15.0F, 120.0F, 30.0F, "Turn on", true, () -> FriendManager.setSetting("Online Features", true));
      }

      return y + h + 14.0F;
   }

   private float sectionLabel(Canvas c, float x, float y, String left, String right) {
      return this.friendLabel(c, x, y, left, right);
   }

   private float drawProfilesPage(Canvas c, float x, float y, float w, float since) {
      int[] badges = new int[]{0, 0, 0};
      y = this.pillTabs(c, "profTabs", x, y, PROFILE_TABS, PROFILE_TAB_ICONS, this.profilesTab, badges, t -> {
         if (!t.equals(this.profilesTab)) {
            this.profilesTab = t;
            this.resetScroll();
         }
      });
      if ("Browse".equals(this.profilesTab)) {
         String q = this.cfgSearch.text.trim();
         String info = CloudConfigs.BROWSE.loading ? "Searching…" : CloudConfigs.BROWSE.total + " config" + (CloudConfigs.BROWSE.total == 1 ? "" : "s");
         c.text("inter", 11.5F, x + w, y - 17.0F, 20, this.th.muted, q.isEmpty() ? info : info + " for “" + q + "”");
      }

      y += 16.0F;

      return switch (this.profilesTab) {
         case "My Cloud" -> this.drawMyCloud(c, x, y, w, since);
         case "Browse" -> this.drawBrowse(c, x, y, w, since);
         default -> this.drawProfiles(c, x, y, w, since);
      };
   }

   private float drawMyCloud(Canvas c, float x, float y, float w, float since) {
      if (!CloudConfigs.available()) {
         return this.cloudUnavailable(c, x, y, w);
      }

      this.cloudTick(true, false);
      float CR = this.gnum("Card Rounding", 14.0);
      float top = y;
      float est = this.measured.getOrDefault("§cloud:upload", 170.0F);
      c.rect(x, y, w, est, CR, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F), this.gnum("Card Opacity", 0.62)));
      c.stroke(x, y, w, est, CR, 1.0F, Theme.a(this.th.border, 0.6F));
      float ix = x + 18.0F;
      float iw = w - 36.0F;
      y += 14.0F;
      c.text("inter-sb", 13.5F, ix, y + 8.0F, 17, this.th.text, "Upload a config");
      c.text("inter", 11.5F, ix + c.textWidth("inter-sb", 13.5F, "Upload a config") + 10.0F, y + 8.0F, 17, this.th.muted, "Everything a profile saves: modules, settings, binds and theme");
      y += 24.0F;
      float ddW = 170.0F;
      float gap = 10.0F;
      float nameW = iw - ddW * 2.0F - gap * 2.0F;
      String active = this.store.activeProfile();
      boolean current = "Current setup".equals(this.uploadSource) || !this.uploadSources().contains(this.uploadSource);
      String namePh = current ? (active.isEmpty() ? "Config name" : active) : this.uploadSource;
      this.inputBox(c, "cfgName", this.cfgName, ix, y, nameW, 32.0F, namePh);
      this.dropdown(c, "cloud/src", ix + nameW + gap, y + 16.0F, ddW, this.uploadSources(), current ? "Current setup" : this.uploadSource, vx -> this.uploadSource = vx);
      this.dropdown(c, "cloud/vis", ix + nameW + gap * 2.0F + ddW, y + 16.0F, ddW, CloudConfigs.VISIBILITIES, this.uploadVisibility, vx -> this.uploadVisibility = vx);
      y += 42.0F;
      float srvW = Math.min(240.0F, iw * 0.3F);
      this.inputBox(c, "cfgDesc", this.cfgDesc, ix, y, iw - srvW - gap, 32.0F, "Description (optional): what's it for, what's special");
      this.inputBox(c, "cfgServer", this.cfgServer, ix + iw - srvW, y, srvW, 32.0F, "Made for server (optional)");
      y += 44.0F;

      String vinfo = switch (this.uploadVisibility) {
         case "Public" -> "Public: listed in Browse, anyone can find, like and load it.";
         case "Unlisted" -> "Unlisted: only people you give the code to can load it.";
         default -> "Private: only you, on any PC you sign in on.";
      };
      c.text("inter", 11.5F, ix, y + 15.0F, 17, this.th.muted, this.ellipsize(c, "inter", 11.5F, vinfo, iw - 160.0F));
      this.button(c, "cloud/upload", ix + iw - 140.0F, y, 140.0F, 30.0F, "Upload", true, this::doUpload);
      y += 44.0F;
      this.measured.put("§cloud:upload", y - top);
      y += 10.0F;
      float otop = y;
      float oest = this.measured.getOrDefault("§cloud:opts", 90.0F);
      c.rect(x, y, w, oest, CR, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F), this.gnum("Card Opacity", 0.62)));
      c.stroke(x, y, w, oest, CR, 1.0F, Theme.a(this.th.border, 0.6F));
      float end = this.drawSettings(c, "§friends", FriendSettings.CLOUD, x + 20.0F, y + 6.0F, w - 40.0F) + 6.0F;
      this.measured.put("§cloud:opts", end - otop);
      y = end + 18.0F;
      CloudConfigs.View v = CloudConfigs.MINE;
      y = this.sectionLabel(c, x, y, "YOUR CLOUD CONFIGS · " + v.items.size() + "/25", v.loading ? "loading…" : null);
      if (v.error != null) {
         y = this.empty(c, x, y, w, v.error);
      } else if (v.items.isEmpty() && !v.loading) {
         y = this.empty(c, x, y, w, "Nothing uploaded yet. Upload your setup above to back it up or share it.");
      }

      int i = 0;

      for (CloudConfigs.Meta m : v.items) {
         float appear = Anim.easeOutCubic((since - 0.022F * Math.min(i++, 18)) / 0.34F);
         y = this.configCard(c, m, x, y, w, appear, true) + this.gnum("Card Spacing", 10.0);
      }

      return y + 10.0F;
   }

   private float drawBrowse(Canvas c, float x, float y, float w, float since) {
      if (!CloudConfigs.available()) {
         return this.cloudUnavailable(c, x, y, w);
      }

      this.cloudTick(false, true);
      float gap = 10.0F;
      float ddW = 170.0F;
      float sW = w - ddW * 2.0F - gap * 2.0F;
      this.inputBox(c, "cfgSearch", this.cfgSearch, x, y, sW, 34.0F, "Search configs, authors, servers…");
      Icons.draw(c, "search", x + sW - 18.0F, y + 17.0F, 13.0F, this.th.muted);
      this.dropdown(c, "cloud/sort", x + sW + gap, y + 17.0F, ddW, CloudConfigs.SORTS, CloudConfigs.sort, vx -> {
         CloudConfigs.sort = vx;
         CloudConfigs.refreshBrowse(false);
      });
      this.dropdown(c, "cloud/scope", x + sW + gap * 2.0F + ddW, y + 17.0F, ddW, CloudConfigs.SCOPES, CloudConfigs.scope, vx -> {
         CloudConfigs.scope = vx;
         CloudConfigs.refreshBrowse(false);
      });
      y += 44.0F;
      float codeW = 200.0F;
      this.inputBox(c, "cfgCode", this.cfgCode, x, y, codeW, 32.0F, "Have a code? e.g. JXXHJCYJ");
      this.button(c, "cloud/code", x + codeW + 8.0F, y + 1.0F, 90.0F, 30.0F, "Load", false, this::loadCode);
      if (!CloudConfigs.author.isEmpty()) {
         String chip = "by " + CloudConfigs.author + "  ✕";
         float cw = c.textWidth("inter-med", 12.0F, chip) + 26.0F;
         float cx0 = x + codeW + 110.0F;
         boolean hv = this.in(cx0, y + 3.0F, cw, 26.0F);
         c.rect(cx0, y + 3.0F, cw, 26.0F, 13.0F, Theme.a(this.th.accent, hv ? 0.3F : 0.18F));
         c.text("inter-med", 12.0F, cx0 + cw / 2.0F, y + 16.0F, 18, this.th.text, chip);
         this.hit(cx0, y + 3.0F, cw, 26.0F, () -> {
            CloudConfigs.author = "";
            CloudConfigs.refreshBrowse(false);
         }, null);
      }

      y += 46.0F;
      CloudConfigs.View v = CloudConfigs.BROWSE;
      if (v.error != null) {
         return this.empty(c, x, y, w, v.error);
      }

      if (v.items.isEmpty()) {
         String msg = v.loading
            ? "Loading…"
            : (
               "Liked By Me".equals(CloudConfigs.scope)
                  ? "You haven't liked any configs yet."
                  : (CloudConfigs.query.isEmpty() ? "No public configs yet. Be the first: My Cloud → Upload → Public." : "Nothing matches that search.")
            );
         return this.empty(c, x, y, w, msg);
      }

      int i = 0;

      for (CloudConfigs.Meta m : v.items) {
         float appear = Anim.easeOutCubic((since - 0.018F * Math.min(i++, 20)) / 0.32F);
         y = this.configCard(c, m, x, y, w, appear, false) + this.gnum("Card Spacing", 10.0);
      }

      if (v.hasMore()) {
         float bw = 160.0F;
         this.button(c, "cloud/more", x + w / 2.0F - bw / 2.0F, y + 4.0F, bw, 32.0F, v.loading ? "Loading…" : "Load more", false, () -> CloudConfigs.refreshBrowse(true));
         y += 46.0F;
      }

      return y + 10.0F;
   }

   private float configCard(Canvas c, CloudConfigs.Meta m, float x, float y, float w, float appear, boolean mineView) {
      float CR = this.gnum("Card Rounding", 14.0);
      boolean hasDesc = !m.description().isEmpty();
      List<String> desc = hasDesc ? this.wrap(c, "inter", 12.0F, m.description(), w - 260.0F) : List.of();
      if (desc.size() > 2) {
         desc = new ArrayList<>(desc.subList(0, 2));
         desc.set(1, this.ellipsize(c, "inter", 12.0F, desc.get(1) + "…", w - 260.0F));
      }

      float H = 64 + desc.size() * 16;
      if (!(y + H < this.clipY - 4.0F) && !(y > this.clipY + this.clipH + 4.0F)) {
         float ry = y + (this.pageSlide ? (1.0F - appear) * 14.0F : 0.0F);
         String key = "cfg:" + m.id();
         float hov = this.an.to(key + ":h", this.in(x, ry, w, H) && this.pop == null ? 1.0F : 0.0F, 14.0F);
         c.save();
         c.alpha(appear);
         if (this.gbool("Hover Lift", true) && hov > 0.01F) {
            c.shadow(x, ry + 3.0F, w, H, CR, 16.0F, Theme.a(-16777216, 0.22F * hov));
         }

         c.rect(x, ry, w, H, CR, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F + 0.025F * hov), this.gnum("Card Opacity", 0.62)));
         c.stroke(x, ry, w, H, CR, 1.0F, Theme.a(this.th.border, 0.55F + 0.25F * hov));
         float tx = x + 20.0F;
         float ty = ry + 22.0F;
         String nm = this.ellipsize(c, "inter-sb", 14.5F, m.name(), w * 0.45F);
         c.text("inter-sb", 14.5F, tx, ty, 17, this.th.text, nm);
         float nx = tx + c.textWidth("inter-sb", 14.5F, nm) + 10.0F;
         List<String> tags = new ArrayList<>();
         if (mineView || m.mine()) {
            tags.add(m.visibilityLabel().toUpperCase(Locale.ROOT));
         }

         if (!m.server().isEmpty()) {
            tags.add(m.server());
         }

         String linked = mineView ? CloudConfigs.profileFor(m.id()) : null;
         if (linked != null) {
            tags.add("SYNCED · " + linked);
         }

         for (String tg : tags) {
            float tfs = 9.5F;
            float tw = c.textWidth("inter-sb", tfs, tg) + 14.0F;
            if (nx + tw > x + w - 250.0F) {
               break;
            }

            boolean pub = tg.equals("PUBLIC");
            c.rect(nx, ty - 9.0F, tw, 18.0F, 9.0F, Theme.a(pub ? -12855686 : this.th.accent, 0.16F));
            c.text("inter-sb", tfs, nx + tw / 2.0F, ty, 18, pub ? -12855686 : this.th.accent, tg);
            nx += tw + 6.0F;
         }

         float ly = ry + 42.0F;
         String by = mineView ? "Code " + m.id() : "by " + m.ownerName();
         int byCol = mineView ? this.th.muted : Theme.mix(this.th.muted, this.th.accent, 0.6F);
         c.text("inter-med", 11.5F, tx, ly, 17, byCol, by);
         float bw0 = c.textWidth("inter-med", 11.5F, by);
         if (!mineView) {
            boolean ahv = this.in(tx - 2.0F, ly - 8.0F, bw0 + 4.0F, 16.0F);
            if (ahv) {
               c.line(tx, ly + 7.0F, tx + bw0, ly + 7.0F, 1.0F, Theme.a(this.th.accent, 0.6F));
            }

            this.hit(tx - 2.0F, ly - 8.0F, bw0 + 4.0F, 16.0F, () -> {
               CloudConfigs.author = m.ownerName();
               CloudConfigs.refreshBrowse(false);
               this.resetScroll();
            }, null);
            if (ahv) {
               this.hoverTip("cfgby:" + m.id(), "Show all configs by " + m.ownerName());
            } else {
               this.hoverSinceReset("cfgby:" + m.id());
            }
         } else {
            this.hit(tx - 2.0F, ly - 8.0F, bw0 + 4.0F, 16.0F, () -> this.copyCode(m), null);
         }

         float sx = tx + bw0 + 14.0F;
         String stats = "·  " + compact(m.downloads()) + " downloads  ·  " + m.enabled() + " modules on  ·  updated " + ago(m.updated());
         c.text("inter", 11.5F, sx, ly, 17, this.th.muted, this.ellipsize(c, "inter", 11.5F, stats, x + w - 250.0F - sx));
         float dy = ry + 62.0F;

         for (String l : desc) {
            c.text("inter", 12.0F, tx, dy, 17, Theme.a(this.th.text, 0.8F), l);
            dy += 16.0F;
         }

         float bx = x + w - 16.0F;
         float cy = ry + H / 2.0F;
         if (!mineView && !m.mine()) {
            String lc = compact(m.likes());
            float lw = c.textWidth("inter-sb", 12.0F, lc) + 40.0F;
            bx -= lw;
            boolean lhv = this.in(bx, cy - 15.0F, lw, 30.0F);
            float lt = this.an.to(key + ":lk", m.liked() ? 1.0F : 0.0F, 14.0F);
            c.rect(bx, cy - 15.0F, lw, 30.0F, 15.0F, Theme.a(-43920, 0.08F + 0.14F * lt + (lhv ? 0.08F : 0.0F)));
            c.stroke(bx, cy - 15.0F, lw, 30.0F, 15.0F, 1.0F, Theme.a(-43920, 0.25F + 0.4F * lt));
            Icons.draw(c, m.liked() ? "heart" : "heartOutline", bx + 15.0F, cy, 14.0F, Theme.mix(this.th.muted, -43920, Math.max(lt, lhv ? 0.6F : 0.0F)));
            c.text("inter-sb", 12.0F, bx + 27.0F, cy, 17, m.liked() ? Theme.mix(-43920, this.th.text, 0.3F) : this.th.text, lc);
            this.hit(bx, cy - 15.0F, lw, 30.0F, () -> CloudConfigs.like(m, !m.liked(), rx -> {
               if (rx.error() != null) {
                  this.toast(rx.error());
               }
            }), null);
            bx -= 8.0F;
         } else {
            String lc = "♥ " + compact(m.likes());
            c.text("inter-sb", 12.0F, bx, cy, 20, Theme.a(-43920, m.likes() > 0 ? 0.9F : 0.45F), lc);
            bx -= c.textWidth("inter-sb", 12.0F, lc) + 14.0F;
         }

         String[] acts = mineView ? new String[]{"Load", "Update", "Delete"} : new String[]{"Load", "Save"};

         for (int k = acts.length - 1; k >= 0; k--) {
            String a = acts[k];
            boolean danger = a.equals("Delete");
            String cid = "cfgdel:" + m.id();
            boolean confirming = danger && cid.equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
            String lbl = confirming ? "Sure?" : a;
            float bw = c.textWidth("inter-med", 11.5F, lbl) + 24.0F;
            bx -= bw;
            float bh = this.an.to(key + ":b:" + a, this.in(bx, cy - 14.0F, bw, 28.0F) ? 1.0F : 0.0F, 16.0F);
            int base = danger ? -43920 : (k == 0 ? this.th.accent : this.th.text);
            c.rect(bx, cy - 14.0F, bw, 28.0F, this.ctlR(), Theme.a(base, (confirming ? 0.3F : 0.08F) + 0.12F * bh));
            c.stroke(bx, cy - 14.0F, bw, 28.0F, this.ctlR(), 1.0F, Theme.a(base, 0.25F + 0.4F * bh));
            c.text("inter-med", 11.5F, bx + bw / 2.0F, cy, 18, !danger && k != 0 ? this.th.text : Theme.mix(base, this.th.text, 0.25F), lbl);

            Runnable r = switch (a) {
               case "Load" -> () -> {
                  this.toast("Loading " + m.name() + "…");
                  CloudConfigs.load(m.id(), true, this::cloudToast);
               };
               case "Save" -> () -> CloudConfigs.load(m.id(), false, this::cloudToast);
               case "Update" -> () -> this.updateCloud(m);
               default -> () -> this.confirm(cid, () -> CloudConfigs.delete(m, this::cloudToast));
            };
            this.hit(bx, cy - 14.0F, bw, 28.0F, r, null);
            bx -= 8.0F;
         }

         if (mineView) {
            String menuId = "cfgmenu:" + m.id();
            this.hit(x, ry, w, H, null, () -> this.cloudMenu(menuId, m, x, ry, w, H));
         } else {
            this.hit(x, ry, w, H, null, () -> this.cloudMenu("cfgmenu:" + m.id(), m, x, ry, w, H));
         }

         if (this.in(x, ry, w, H) && this.pop == null) {
            this.hoverTip("cfgcard:" + m.id(), mineView ? "Right click: visibility, copy code, rename" : "Right click: copy code, more by " + m.ownerName());
         } else {
            this.hoverSinceReset("cfgcard:" + m.id());
         }

         c.restore();
         return y + H;
      } else {
         return y + H;
      }
   }

   private void cloudMenu(String menuId, CloudConfigs.Meta m, float x, float y, float w, float h) {
      if (this.pop != null && menuId.equals(this.pop.id)) {
         this.pop = null;
      } else {
         List<String> opts = new ArrayList<>();
         opts.add("Copy code");
         if (m.mine()) {
            for (String v : CloudConfigs.VISIBILITIES) {
               if (!v.equalsIgnoreCase(m.visibility())) {
                  opts.add("Make " + v.toLowerCase(Locale.ROOT));
               }
            }

            opts.add("Update from current setup");
            if (CloudConfigs.profileFor(m.id()) != null) {
               opts.add("Stop syncing");
            }

            opts.add("Delete");
         } else {
            opts.add(m.liked() ? "Unlike" : "Like");
            opts.add("More by " + m.ownerName());
            opts.add("Save as profile");
            if (GlobalChat.isAdmin()) {
               opts.add("Remove (mod)");
            }
         }

         float mw = this.compact() ? Math.min(w, 170.0F) : 220.0F;
         this.openList(menuId, x + w - mw, y, mw, h, mw, opts, "", vx -> {
            if (vx.equals("Copy code")) {
               this.copyCode(m);
            } else if (vx.startsWith("Make ")) {
               CloudConfigs.setVisibility(m, vx.substring(5), this::cloudToast);
            } else if (vx.equals("Update from current setup")) {
               CloudConfigs.upload(null, null, null, null, CloudConfigs.currentSnapshot(), this.store.activeProfile().isEmpty() ? null : this.store.activeProfile(), m.id(), this::cloudToast);
            } else if (vx.equals("Stop syncing")) {
               String pr = CloudConfigs.profileFor(m.id());
               if (pr != null) {
                  CloudConfigs.unlinkProfile(pr);
               }

               this.toast("No longer synced with " + pr);
            } else if (vx.equals("Delete") || vx.equals("Remove (mod)")) {
               String cid = "cfgdel:" + m.id();
               boolean armed = cid.equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
               this.confirm(cid, () -> CloudConfigs.delete(m, this::cloudToast));
               if (!armed) {
                  this.toast("Pick " + vx + " again to confirm");
               }
            } else if (vx.equals("Like") || vx.equals("Unlike")) {
               CloudConfigs.like(m, vx.equals("Like"), this::cloudToast);
            } else if (vx.startsWith("More by ")) {
               CloudConfigs.author = m.ownerName();
               CloudConfigs.refreshBrowse(false);
               this.profilesTab = "Browse";
               this.pConfigsTab = "Browse";
            } else if (vx.equals("Save as profile")) {
               CloudConfigs.load(m.id(), false, this::cloudToast);
            }
         });
      }
   }

   private float pInput(Canvas c, ClickGui.Panel p, String id, TextField f, float x, float y, float w, String placeholder) {
      float fh = this.setRowH() + 2.0F;
      float fs = this.pFont() - 1.0F;
      boolean editing = this.focused == f;
      float ft = this.an.to("pin:" + id, editing ? 1.0F : 0.0F, 16.0F);
      if (this.tabsMode()) {
         this.tFrame(c, x, y, w, fh, editing || this.pin(p, x, y, w, fh), false);
      } else {
         c.rect(x, y, w, fh, 3.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F), 0.9F));
         c.stroke(x, y, w, fh, 3.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.8F), this.th.accent, ft));
      }

      String t = f.text;
      float avail = w - 12.0F;
      float tWidth = c.textWidth("inter", fs, t);
      float tx0 = x + 6.0F - (editing ? Math.max(0.0F, tWidth - avail) : 0.0F);
      c.save();
      c.scissor(x + 3.0F, y, w - 6.0F, fh);
      if (t.isEmpty() && !editing) {
         c.text("inter", fs, x + 6.0F, y + fh / 2.0F, 17, this.th.muted, this.ellipsize(c, "inter", fs, placeholder, avail));
      } else {
         c.text("inter", fs, tx0, y + fh / 2.0F, 17, this.th.text, t);
         if (editing) {
            this.caret(c, "inter", fs, tx0, y + fh / 2.0F, f);
         }
      }

      c.restore();
      this.hit(x, y, w, fh, () -> this.focus(f, null), null);
      return y + fh;
   }

   private float panelTabs(Canvas c, ClickGui.Panel p, String id, float ix, float y, float iw, String[] tabs, String current, int[] badges, Consumer<String> pick) {
      if (this.tabsMode()) {
         float th_ = this.setRowH() + 1.0F;
         float segW = iw / tabs.length;
         float tfs_ = this.pFont() - 1.5F;
         int idx = Math.max(0, Arrays.asList(tabs).indexOf(current));

         for (int i = 0; i < tabs.length; i++) {
            float tx = ix + i * segW;
            boolean act = i == idx;
            boolean hv = this.pin(p, tx, y, segW, th_);
            this.tFrame(c, tx, y, segW + (i < tabs.length - 1 ? 1 : 0), th_, hv, !act);
            if (act) {
               c.rect(tx + 1.0F, y + 1.0F, segW - 1.0F, 2.0F, 0.0F, this.tAccent);
            }

            this.tText(
               c,
               "inter",
               tfs_,
               tx + segW / 2.0F,
               y + th_ / 2.0F + (act ? 1 : 0),
               18,
               act ? this.tFont : Theme.mix(this.tFont, this.tMuted, hv ? 0.1F : 0.4F),
               this.ellipsize(c, "inter", tfs_, tabs[i], segW - 4.0F)
            );
            if (badges != null && badges[i] > 0) {
               this.badge(c, tx + segW / 2.0F + c.textWidth("inter", tfs_, tabs[i]) / 2.0F + 2.0F, y + 3.0F, Math.max(6.0F, tfs_ - 3.0F), badges[i]);
            }

            String target = tabs[i];
            this.hit(tx, y, segW, th_, () -> pick.accept(target), null);
         }

         return y + th_;
      } else {
         float rh = this.setRowH();
         float fs = this.pFont();
         float th_ = rh + 1.0F;
         float segW = iw / tabs.length;
         c.rect(ix, y, iw, th_, th_ / 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.07F), 0.9F));
         int idx = Math.max(0, Arrays.asList(tabs).indexOf(current));
         float sx = this.an.to(id, ix + idx * segW, 20.0F);
         c.rect(sx + 1.0F, y + 1.0F, segW - 2.0F, th_ - 2.0F, (th_ - 2.0F) / 2.0F, this.th.accent);

         for (int i = 0; i < tabs.length; i++) {
            String t = tabs[i];
            float tx = ix + i * segW;
            boolean act = i == idx;
            boolean hv = this.pin(p, tx, y, segW, th_);
            float tfs = fs - 1.5F;
            int col = act ? Theme.onColor(this.th.accent) : (hv ? this.th.text : this.th.muted);
            c.text(act ? "inter-sb" : "inter-med", tfs, tx + segW / 2.0F, y + th_ / 2.0F, 18, col, this.ellipsize(c, "inter-med", tfs, t, segW - 4.0F));
            if (badges != null && badges[i] > 0) {
               this.badge(c, tx + segW / 2.0F + c.textWidth("inter-sb", tfs, t) / 2.0F + 2.0F, y + 3.0F, Math.max(6.0F, tfs - 3.0F), badges[i]);
            }

            String target = t;
            this.hit(tx, y, segW, th_, () -> pick.accept(target), null);
         }

         return y + th_;
      }
   }

   private float drawConfigsPanel(Canvas c, ClickGui.Panel p, float x, float y, float W) {
      float pad = 7.0F;
      float ix = x + pad;
      float iw = W - pad * 2.0F;
      y += 5.0F;
      y = this.panelTabs(c, p, "pcfgTab", ix, y, iw, new String[]{"Local", "Cloud", "Browse"}, this.pConfigsTab, null, t -> {
         this.pConfigsTab = t;

         try {
            this.store.setGui("Configs Tab", t);
         } catch (Throwable var3x) {
         }
      }) + 6.0F;

      return switch (this.pConfigsTab) {
         case "Cloud" -> this.drawPanelCloud(c, p, ix, y, iw);
         case "Browse" -> this.drawPanelBrowse(c, p, ix, y, iw);
         default -> this.drawPanelProfiles(c, p, ix, y, iw);
      } + 4.0F;
   }

   private float panelUnavailable(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      String why = CloudConfigs.unavailableReason();

      for (String l : this.wrap(c, "inter-med", fs, (why == null ? "" : why) + ". Online configs need your Clarity connection.", w - 4.0F)) {
         c.text("inter-med", fs, x + w / 2.0F, y + rh / 2.0F, 18, this.th.muted, l);
         y += rh - 2.0F;
      }

      if (!FriendManager.bool("Online Features")) {
         y = this.pAction(c, p, "pcfg/on", "Turn on online features", x, y + 4.0F, w, () -> FriendManager.setSetting("Online Features", true));
      }

      return y + 4.0F;
   }

   private float drawPanelCloud(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      if (!CloudConfigs.available()) {
         return this.panelUnavailable(c, p, x, y, w);
      }

      this.cloudTick(true, false);
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      y = this.pLabel(c, x, y, "UPLOAD");
      String active = this.store.activeProfile();
      boolean current = "Current setup".equals(this.uploadSource) || !this.uploadSources().contains(this.uploadSource);
      y = this.pInput(c, p, "pcfgName", this.cfgName, x, y, w, current ? (active.isEmpty() ? "Config name" : active) : this.uploadSource) + 3.0F;
      y = this.pInput(c, p, "pcfgDesc", this.cfgDesc, x, y, w, "Description (optional)") + 2.0F;
      y = this.pSelect(c, p, "pcfg/src", "From", x, y, w, this.uploadSources(), current ? "Current setup" : this.uploadSource, vx -> this.uploadSource = vx);
      y = this.pSelect(c, p, "pcfg/vis", "Visibility", x, y, w, CloudConfigs.VISIBILITIES, this.uploadVisibility, vx -> this.uploadVisibility = vx);
      y = this.pAction(c, p, "pcfg/upload", "Upload", x, y + 2.0F, w, this::doUpload) + 4.0F;
      y = this.drawPanelSettings(c, p, "§friends", FriendSettings.CLOUD, x, y, w) + 4.0F;
      CloudConfigs.View v = CloudConfigs.MINE;
      y = this.pLabel(c, x, y, "YOURS · " + v.items.size() + "/25" + (v.loading ? " · LOADING" : ""));
      if (v.error != null || v.items.isEmpty() && !v.loading) {
         String msg = v.error != null ? v.error : "Nothing uploaded yet.";
         c.text("inter", fs - 0.5F, x + w / 2.0F, y + rh / 2.0F, 18, this.th.muted, this.ellipsize(c, "inter", fs - 0.5F, msg, w));
         y += rh;
      }

      for (CloudConfigs.Meta m : v.items) {
         y = this.drawPanelConfigRow(c, p, m, x, y, w, true);
      }

      return y;
   }

   private float drawPanelBrowse(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      if (!CloudConfigs.available()) {
         return this.panelUnavailable(c, p, x, y, w);
      }

      this.cloudTick(false, true);
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      y = this.pInput(c, p, "pcfgSearch", this.cfgSearch, x, y, w, "Search configs, authors…") + 2.0F;
      y = this.pSelect(c, p, "pcfg/sort", "Sort", x, y, w, CloudConfigs.SORTS, CloudConfigs.sort, vx -> {
         CloudConfigs.sort = vx;
         CloudConfigs.refreshBrowse(false);
      });
      y = this.pSelect(c, p, "pcfg/scope", "Show", x, y, w, CloudConfigs.SCOPES, CloudConfigs.scope, vx -> {
         CloudConfigs.scope = vx;
         CloudConfigs.refreshBrowse(false);
      });
      if (!CloudConfigs.author.isEmpty()) {
         y = this.pAction(c, p, "pcfg/author", "By " + CloudConfigs.author + "  ✕", x, y + 1.0F, w, () -> {
            CloudConfigs.author = "";
            CloudConfigs.refreshBrowse(false);
         });
      }

      float bw = rh + 2.0F;
      this.pInput(c, p, "pcfgCode", this.cfgCode, x, y + 2.0F, w - bw - 3.0F, "Load by code…");
      this.iconButton(c, p, "pcfg/code", "download", x + w - bw, y + 2.0F, bw, rh + 2.0F, true, "Load this code", this::loadCode);
      y += rh + 8.0F;
      CloudConfigs.View v = CloudConfigs.BROWSE;
      y = this.pLabel(c, x, y, v.loading ? "LOADING" : v.total + " CONFIG" + (v.total == 1 ? "" : "S"));
      if (v.error != null || v.items.isEmpty() && !v.loading) {
         String msg = v.error != null ? v.error : (CloudConfigs.query.isEmpty() ? "No public configs yet." : "Nothing matches.");
         c.text("inter", fs - 0.5F, x + w / 2.0F, y + rh / 2.0F, 18, this.th.muted, this.ellipsize(c, "inter", fs - 0.5F, msg, w));
         y += rh;
      }

      for (CloudConfigs.Meta m : v.items) {
         y = this.drawPanelConfigRow(c, p, m, x, y, w, false);
      }

      if (v.hasMore()) {
         y = this.pAction(c, p, "pcfg/more", v.loading ? "Loading…" : "Load more", x, y + 2.0F, w, () -> CloudConfigs.refreshBrowse(true));
      }

      return y;
   }

   private float drawPanelConfigRow(Canvas c, ClickGui.Panel p, CloudConfigs.Meta m, float x, float y, float w, boolean mineView) {
      float fs = this.pFont() - 1.0F;
      float H = Math.max(24.0F, this.setRowH() * 1.7F);
      String key = "pcfg:" + (mineView ? "m:" : "b:") + m.id();
      boolean exp = this.expanded.contains(key);
      boolean hv = this.pin(p, x - 2.0F, y, w + 4.0F, H);
      float hT = this.an.to(key + ":h", hv ? 1.0F : 0.0F, 18.0F);
      if (y + H > this.clipY && y < this.clipY + this.clipH) {
         if (hT > 0.01F || exp) {
            c.rect(x - 2.0F, y, w + 4.0F, H, 3.0F, Theme.a(exp ? this.th.accent : this.th.text, exp ? 0.1F : 0.05F * hT));
         }

         float likeW = 0.0F;
         float cy = y + H / 2.0F;
         if (!mineView && !m.mine()) {
            String lc = compact(m.likes());
            likeW = c.textWidth("inter-sb", fs - 1.0F, lc) + 16.0F;
            float lx = x + w - likeW;
            boolean lhv = this.pin(p, lx - 2.0F, cy - 7.0F, likeW + 4.0F, 14.0F);
            Icons.draw(c, m.liked() ? "heart" : "heartOutline", lx + 5.0F, cy, 10.0F, !m.liked() && !lhv ? this.th.muted : -43920);
            c.text("inter-sb", fs - 1.0F, lx + 12.0F, cy, 17, m.liked() ? -43920 : this.th.muted, lc);
         } else {
            String vis = m.visibilityLabel().toUpperCase(Locale.ROOT);
            float tfs = Math.max(6.5F, fs - 3.0F);
            likeW = c.textWidth("inter-sb", tfs, vis) + 8.0F;
            boolean pub = "public".equals(m.visibility());
            c.rect(x + w - likeW, cy - (tfs + 4.0F) / 2.0F, likeW, tfs + 4.0F, (tfs + 4.0F) / 2.0F, Theme.a(pub ? -12855686 : this.th.accent, 0.18F));
            c.text("inter-sb", tfs, x + w - likeW / 2.0F, cy, 18, pub ? -12855686 : this.th.accent, vis);
         }

         float maxW = w - likeW - 6.0F;
         c.text("inter-sb", fs, x + 2.0F, y + H * 0.34F, 17, this.th.text, this.ellipsize(c, "inter-sb", fs, m.name(), maxW));
         String sub = mineView ? m.id() + " · ♥ " + compact(m.likes()) + " · ↓ " + compact(m.downloads()) : "by " + m.ownerName() + " · ↓ " + compact(m.downloads()) + " · " + ago(m.updated());
         c.text("inter", fs - 1.5F, x + 2.0F, y + H * 0.7F, 17, this.th.muted, this.ellipsize(c, "inter", fs - 1.5F, sub, maxW));
         float ry = y;
         this.hit(x - 2.0F, y, w + 4.0F, H, () -> this.toggleExpand(key), () -> this.cloudMenu("pcfgmenu:" + key, m, x - 2.0F, ry, w + 4.0F, H));
         if (!mineView && !m.mine()) {
            float lx = x + w - likeW;
            this.hit(lx - 2.0F, cy - 7.0F, likeW + 4.0F, 14.0F, () -> CloudConfigs.like(m, !m.liked(), r -> {
               if (r.error() != null) {
                  this.toast(r.error());
               }
            }), null);
         }
      }

      y += H;
      float expT = this.an.to("pexp:" + key, exp ? 1.0F : 0.0F, 12.0F);
      if (expT > 0.002F) {
         float setH = this.measured.getOrDefault(key, 0.0F);
         float vis = setH * Anim.ease(expT);
         c.save();
         c.scissor(x - 2.0F, y, w + 4.0F, Math.max(0.0F, vis));
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH2 = this.clipH;
         this.intersectClip(x - 2.0F, y, w + 4.0F, vis);
         c.rect(x - 2.0F, y, w + 4.0F, vis, 0.0F, Theme.a(-16777216, 0.22F));
         c.rect(x - 2.0F, y, 1.5F, vis, 0.0F, Theme.a(this.th.accent, 0.85F));
         float rh = this.setRowH();
         float yy = y + 3.0F;
         if (!m.description().isEmpty()) {
            for (String l : this.wrap(c, "inter", fs - 0.5F, m.description(), w - 8.0F)) {
               c.text("inter", fs - 0.5F, x + 4.0F, yy + rh / 2.0F - 2.0F, 17, Theme.a(this.th.text, 0.85F), l);
               yy += rh - 4.0F;
            }

            yy += 3.0F;
         }

         String info = m.enabled() + " modules on" + (m.server().isEmpty() ? "" : " · " + m.server()) + " · " + ago(m.updated());
         c.text("inter", fs - 1.5F, x + 4.0F, yy + rh / 2.0F - 2.0F, 17, this.th.muted, this.ellipsize(c, "inter", fs - 1.5F, info, w - 8.0F));
         yy += rh - 2.0F;
         if (mineView) {
            String linked = CloudConfigs.profileFor(m.id());
            if (linked != null) {
               c.text("inter-med", fs - 1.5F, x + 4.0F, yy + rh / 2.0F - 2.0F, 17, this.th.accent, this.ellipsize(c, "inter-med", fs - 1.5F, "Synced with profile " + linked, w - 8.0F));
               yy += rh - 2.0F;
            }

            yy = this.pSelect(c, p, key + "/vis", "Visibility", x + 4.0F, yy, w - 6.0F, CloudConfigs.VISIBILITIES, m.visibilityLabel(), v -> CloudConfigs.setVisibility(m, v, this::cloudToast)) + 2.0F;
         }

         float bh = rh;
         float n = mineView ? 4.0F : 3.0F;
         float bw = (w - 6.0F - 3.0F * (n - 1.0F)) / n;
         float bx = x + 4.0F;
         this.pButton(c, p, key + "/load", "Load", bx, yy, bw, bh, this.th.accent, () -> {
            this.toast("Loading " + m.name() + "…");
            CloudConfigs.load(m.id(), true, this::cloudToast);
         });
         bx += bw + 3.0F;
         if (!mineView) {
            this.pButton(c, p, key + "/save", "Save", bx, yy, bw, bh, this.th.text, () -> CloudConfigs.load(m.id(), false, this::cloudToast));
            bx += bw + 3.0F;
            this.pButton(c, p, key + "/by", "More by", bx, yy, bw, bh, this.th.text, () -> {
               CloudConfigs.author = m.ownerName();
               CloudConfigs.refreshBrowse(false);
            });
         } else {
            this.pButton(c, p, key + "/upd", "Update", bx, yy, bw, bh, this.th.text, () -> this.updateCloud(m));
            bx += bw + 3.0F;
            this.pButton(c, p, key + "/code", "Code", bx, yy, bw, bh, this.th.text, () -> this.copyCode(m));
            bx += bw + 3.0F;
            String cid = "cfgdel:" + m.id();
            boolean confirming = cid.equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
            this.pButton(c, p, key + "/del", confirming ? "Sure?" : "Delete", bx, yy, bw, bh, -43920, () -> this.confirm(cid, () -> CloudConfigs.delete(m, this::cloudToast)));
         }

         yy += bh + 5.0F;
         this.measured.put(key, yy - y);
         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH2;
         c.restore();
         y += vis;
      }

      return y + 2.0F;
   }

   private float friendsPanelW() {
      return Math.max(this.panelW(), this.gnum("Wide Panel Width", 210.0));
   }

   private float pw(ClickGui.Panel p) {
      return p == null || !"Friends".equals(p.name) && !"Configs".equals(p.name) ? this.panelW() : this.friendsPanelW();
   }

   private float pLabel(Canvas c, float x, float y, String text) {
      float fs = Math.max(7.0F, this.pFont() - 2.5F);
      c.text("inter-sb", fs, x + 1.0F, y + 6.0F, 17, Theme.a(this.th.accent, 0.85F), text);
      return y + 13.0F;
   }

   private void iconButton(Canvas c, ClickGui.Panel p, String id, String icon, float x, float y, float w, float h, boolean primary, String tip, Runnable r) {
      boolean hv = this.chatIn(p, x, y, w, h);
      float t = this.an.to("ib:" + id, hv ? 1.0F : 0.0F, 18.0F);
      if (this.tabsMode()) {
         this.tFrame(c, x, y, w, h, hv, true);
         if (primary) {
            c.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, Theme.a(this.tAccent, 0.75F + 0.25F * t));
         }
      } else if (primary) {
         c.rect(x, y, w, h, 3.0F, Theme.a(this.th.accent, 0.78F + 0.22F * t));
      } else {
         c.rect(x, y, w, h, 3.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.08F + 0.05F * t), 0.9F));
         c.stroke(x, y, w, h, 3.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.8F), this.th.accent, t));
      }

      Icons.draw(c, icon, x + w / 2.0F, y + h / 2.0F, Math.min(w, h) * 0.62F, primary ? Theme.onColor(this.th.accent) : Theme.mix(this.th.muted, this.th.text, t));
      this.hit(x, y, w, h, r, null);
      if (tip != null) {
         if (hv) {
            this.hoverTip("ib:" + id, tip);
         } else {
            this.hoverSinceReset("ib:" + id);
         }
      }
   }

   private void pButton(Canvas c, ClickGui.Panel p, String id, String label, float x, float y, float w, float h, int tint, Runnable r) {
      boolean hv = this.pin(p, x, y, w, h);
      if (this.tabsMode()) {
         float fs = this.pFont() - 1.5F;
         this.tFrame(c, x, y, w, h, hv, true);
         int col = (tint | 0xFF000000) == (this.th.text | 0xFF000000) ? this.tFont : Theme.mix(tint | 0xFF000000, this.tFont, 0.3F);
         this.tText(c, "inter", fs, x + w / 2.0F, y + h / 2.0F, 18, col, this.ellipsize(c, "inter", fs, label, w - 4.0F));
         this.hit(x, y, w, h, r, null);
      } else {
         float t = this.an.to("pb:" + id, hv ? 1.0F : 0.0F, 18.0F);
         float fs = this.pFont() - 1.5F;
         c.rect(x, y, w, h, 3.0F, Theme.a(tint, 0.1F + 0.14F * t));
         c.stroke(x, y, w, h, 3.0F, 1.0F, Theme.a(tint, 0.28F + 0.35F * t));
         c.text("inter-med", fs, x + w / 2.0F, y + h / 2.0F, 18, Theme.mix(tint, this.th.text, 0.45F), this.ellipsize(c, "inter-med", fs, label, w - 4.0F));
         this.hit(x, y, w, h, r, null);
      }
   }

   private float drawFriendsPanel(Canvas c, ClickGui.Panel p, float x, float y, float W) {
      float pad = 7.0F;
      float ix = x + pad;
      float iw = W - pad * 2.0F;
      float fs = this.pFont();
      float rh = this.setRowH();
      y += 4.0F;
      boolean onlineOn = FriendManager.bool("Online Features");
      boolean connected = PresenceClient.state() == PresenceClient.State.CONNECTED;
      String line = PresenceClient.statusLine();
      c.text("inter-med", fs - 1.5F, ix + 1.0F, y + rh / 2.0F, 17, Theme.a(this.th.text, 0.78F), this.ellipsize(c, "inter-med", fs - 1.5F, line, iw - 2.0F));
      if (this.pin(p, ix, y, iw, rh) && c.textWidth("inter-med", fs - 1.5F, line) > iw - 2.0F) {
         this.hoverTip("pfr:line", line);
      } else {
         this.hoverSinceReset("pfr:line");
      }

      y += rh;
      if (onlineOn) {
         y = this.pSelect(c, p, "pfr/status", "Status", ix, y, iw, FriendSettings.STATUSES, str(this.friendGet("§friends", "Status"), "Online"), v -> this.friendSet("§friends", "Status", v));
      } else {
         y = this.pAction(c, p, "pfr/on", "Turn on online features", ix, y, iw, () -> this.friendSet("§friends", "Online Features", true));
      }

      y += 4.0F;
      if (this.tabsMode()) {
         int[] badges = new int[PANEL_TABS.length];

         for (int i = 0; i < PANEL_TABS.length; i++) {
            String t = PANEL_TABS[i];
            badges[i] = "Chat".equals(t)
               ? (t.equals(this.pFriendsTab) ? 0 : GlobalChat.unread())
               : ("DMs".equals(t) ? DirectMessages.totalUnread() : ("Friends".equals(t) ? PresenceClient.incomingRequests().size() : 0));
         }

         y = this.panelTabs(c, p, "pfrTab", ix, y, iw, PANEL_TABS, this.pFriendsTab, badges, txx -> {
            this.pFriendsTab = txx;

            try {
               this.store.setGui("Friends Tab", txx);
            } catch (Throwable var3x) {
            }
         }) + 6.0F;
      } else {
         float th_ = rh + 1.0F;
         float segW = iw / PANEL_TABS.length;
         c.rect(ix, y, iw, th_, th_ / 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.07F), 0.9F));
         int idx = Math.max(0, Arrays.asList(PANEL_TABS).indexOf(this.pFriendsTab));
         float sx = this.an.to("pfrTab", ix + idx * segW, 20.0F);
         c.rect(sx + 1.0F, y + 1.0F, segW - 2.0F, th_ - 2.0F, (th_ - 2.0F) / 2.0F, this.th.accent);

         for (int i = 0; i < PANEL_TABS.length; i++) {
            String t = PANEL_TABS[i];
            float tx = ix + i * segW;
            boolean act = i == idx;
            boolean hv = this.pin(p, tx, y, segW, th_);
            float tfs = fs - 1.5F;
            int col = act ? Theme.onColor(this.th.accent) : (hv ? this.th.text : this.th.muted);
            c.text(act ? "inter-sb" : "inter-med", tfs, tx + segW / 2.0F, y + th_ / 2.0F, 18, col, t);
            int b = "Chat".equals(t) ? (act ? 0 : GlobalChat.unread()) : ("DMs".equals(t) ? DirectMessages.totalUnread() : ("Friends".equals(t) ? PresenceClient.incomingRequests().size() : 0));
            if (b > 0) {
               this.badge(c, tx + segW / 2.0F + c.textWidth("inter-sb", tfs, t) / 2.0F + 2.0F, y + 3.0F, Math.max(6.0F, tfs - 3.0F), b);
            }

            this.hit(tx, y, segW, th_, () -> {
               this.pFriendsTab = t;

               try {
                  this.store.setGui("Friends Tab", t);
               } catch (Throwable var3x) {
               }
            }, null);
         }

         y += th_ + 6.0F;
      }
      return switch (this.pFriendsTab) {
         case "Chat" -> {
            float ch = Math.max(140.0F, this.gnum("Panel Chat Height", 270.0));
            this.drawChatBox(c, p, "panel", ix - 2.0F, y, iw + 4.0F, ch, fs - 0.5F);
            yield y + (ch + 4.0F);
         }
         case "Settings" -> this.drawPanelFriendSettings(c, p, ix, y, iw);
         case "DMs" -> this.drawPanelDms(c, p, ix, y, iw);
         default -> this.drawPanelFriendList(c, p, ix, y, iw);
      } + 4.0F;
   }

   private float drawPanelFriendList(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      float rh = this.setRowH();
      float fs = this.pFont() - 1.0F;
      float fh = rh + 2.0F;
      boolean connected = PresenceClient.state() == PresenceClient.State.CONNECTED;
      int nb = connected ? 2 : 1;
      float bsz = fh;
      float fw = w - (bsz + 3.0F) * nb;
      boolean editing = this.focused == this.nameField && this.nameEnter != null;
      float ft = this.an.to("pfrField", editing ? 1.0F : 0.0F, 16.0F);
      if (this.tabsMode()) {
         this.tFrame(c, x, y, fw, fh, editing || this.pin(p, x, y, fw, fh), false);
      } else {
         c.rect(x, y, fw, fh, 3.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F), 0.9F));
         c.stroke(x, y, fw, fh, 3.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.8F), this.th.accent, ft));
      }

      String t = this.nameField.text;
      c.save();
      c.scissor(x + 3.0F, y, fw - 6.0F, fh);
      if (t.isEmpty() && !editing) {
         c.text("inter", fs, x + 6.0F, y + fh / 2.0F, 17, this.th.muted, "Add a friend…");
      } else {
         c.text("inter", fs, x + 6.0F, y + fh / 2.0F, 17, this.th.text, t);
         if (editing) {
            this.caret(c, "inter", fs, x + 6.0F, y + fh / 2.0F, this.nameField);
         }
      }

      c.restore();
      this.hit(x, y, fw, fh, () -> {
         this.focus(this.nameField, null);
         this.nameEnter = () -> this.friendAdd(false);
      }, null);
      float bx = x + fw + 3.0F;
      this.iconButton(c, p, "pfr/add", "plus", bx, y, bsz, fh, true, "Add to your friends list", () -> {
         if (this.nameField.text.isBlank()) {
            this.focus(this.nameField, null);
            this.nameEnter = () -> this.friendAdd(false);
         } else {
            this.friendAdd(false);
         }
      });
      if (connected) {
         bx += bsz + 3.0F;
         this.iconButton(c, p, "pfr/req", "send", bx, y, bsz, fh, false, "Send a friend request (they get to accept)", () -> {
            if (this.nameField.text.isBlank()) {
               this.focus(this.nameField, null);
               this.nameEnter = () -> this.friendAdd(true);
            } else {
               this.friendAdd(true);
            }
         });
      }

      y += fh + 6.0F;
      List<PresenceClient.Request> reqs = PresenceClient.incomingRequests();
      if (!reqs.isEmpty()) {
         y = this.pLabel(c, x, y, "REQUESTS · " + reqs.size());

         for (PresenceClient.Request r : reqs) {
            float h = rh + 4.0F;
            float b = h - 4.0F;
            c.rect(x - 2.0F, y, w + 4.0F, h, 3.0F, Theme.a(this.th.accent, 0.12F));
            c.text("inter-sb", fs, x + 3.0F, y + h / 2.0F, 17, this.th.text, this.ellipsize(c, "inter-sb", fs, r.name(), w - (b + 3.0F) * 2.0F - 8.0F));
            float bx2 = x + w - b;
            this.iconButton(c, p, "pfr/dec:" + r.uuid(), "close", bx2, y + 2.0F, b, b, false, "Decline", () -> {
               PresenceClient.respond(r, false);
               this.toast("Declined " + r.name());
            });
            this.iconButton(c, p, "pfr/acc:" + r.uuid(), "check", bx2 - b - 3.0F, y + 2.0F, b, b, true, "Accept", () -> {
               PresenceClient.respond(r, true);
               this.toast(r.name() + " is now your friend");
            });
            y += h + 3.0F;
         }

         y += 3.0F;
      }

      Set<String> sent = PresenceClient.outgoingRequests();
      if (!sent.isEmpty()) {
         y = this.pLabel(c, x, y, "SENT");

         for (String n : sent) {
            float h = rh;
            c.text("inter-med", fs, x + 3.0F, y + h / 2.0F, 17, this.th.muted, this.ellipsize(c, "inter-med", fs, n + " · waiting", w - h - 6.0F));
            this.iconButton(c, p, "pfr/cancel:" + n, "close", x + w - h + 2.0F, y + 1.0F, h - 2.0F, h - 2.0F, false, "Cancel request", () -> {
               PresenceClient.cancelRequest(n);
               this.toast("Cancelled request to " + n);
            });
            y += h + 2.0F;
         }

         y += 3.0F;
      }

      List<Friend> list = FriendManager.sorted(ClickGui::friendOnline);
      int online = 0;

      for (Friend f : list) {
         if (friendOnline(f)) {
            online++;
         }
      }

      y = this.pLabel(c, x, y, "FRIENDS · " + list.size() + (list.isEmpty() ? "" : "  ·  " + online + " ONLINE"));
      if (list.isEmpty()) {
         for (String l : this.wrap(c, "inter", fs - 0.5F, "No friends yet. Type a name above, or middle click a player in game.", w - 4.0F)) {
            c.text("inter", fs - 0.5F, x + w / 2.0F, y + rh / 2.0F, 18, this.th.muted, l);
            y += rh - 2.0F;
         }

         y += 4.0F;
      }

      for (Friend f : list) {
         y = this.drawPanelFriendRow(c, p, f, x, y, w);
      }

      return y;
   }

   private float drawPanelFriendRow(Canvas c, ClickGui.Panel p, Friend f, float x, float y, float w) {
      float fs = this.pFont() - 1.0F;
      float H = Math.max(24.0F, this.setRowH() * 1.7F);
      String fkey = f.key();
      String key = "pfr:" + fkey;
      String module = FriendSettings.friendModule(f);
      boolean exp = this.expanded.contains(key);
      boolean hv = this.pin(p, x - 2.0F, y, w + 4.0F, H);
      float hT = this.an.to("pfrh:" + fkey, hv ? 1.0F : 0.0F, 18.0F);
      if (y + H > this.clipY && y < this.clipY + this.clipH) {
         if (hT > 0.01F || exp) {
            c.rect(x - 2.0F, y, w + 4.0F, H, 3.0F, Theme.a(exp ? this.th.accent : this.th.text, exp ? 0.1F : 0.05F * hT));
         }

         if (f.favourite()) {
            c.gradient(x - 2.0F, y, (w + 4.0F) * 0.55F, H, 3.0F, Theme.a(-670398, 0.08F), Theme.a(-670398, 0.0F), true);
         }

         int rgb = FriendManager.nameColour(f);
         int nc = rgb >= 0 ? 0xFF000000 | rgb : this.th.text;
         float ar = H * 0.32F;
         float ax = x + ar + 1.0F;
         float ay = y + H / 2.0F;
         c.circle(ax, ay, ar, Theme.a(nc, 0.16F));
         c.ring(ax, ay, ar, 1.0F, Theme.a(nc, 0.5F));
         String disp = f.displayName();
         c.text("inter-sb", ar * 1.05F, ax, ay, 18, nc, disp.isEmpty() ? "?" : disp.substring(0, 1).toUpperCase(Locale.ROOT));
         int sc = this.friendStatusColour(f);
         boolean on = friendOnline(f);
         float dx = ax + ar * 0.72F;
         float dy = ay + ar * 0.72F;
         c.circle(dx, dy, ar * 0.44F, 0xFF000000 | this.th.bg);
         if (on) {
            c.glow(dx, dy, ar * 0.6F, Theme.a(sc, 0.45F));
         }

         c.circle(dx, dy, ar * 0.3F, sc);
         float tx = ax + ar + 6.0F;
         float starX = x + w - 6.0F;
         float maxW = starX - 10.0F - tx;
         c.text("inter-sb", fs, tx, y + H * 0.34F, 17, nc, this.ellipsize(c, "inter-sb", fs, disp, maxW));
         c.text("inter", fs - 1.5F, tx, y + H * 0.7F, 17, on ? Theme.mix(this.th.muted, sc, 0.5F) : this.th.muted, this.ellipsize(c, "inter", fs - 1.5F, this.friendStatus(f), maxW));
         String menuId = "fract:" + fkey;
         float ry = y;
         this.hit(x - 2.0F, y, w + 4.0F, H, () -> this.toggleExpand(key), () -> this.openFriendMenu(menuId, f, module, x - 2.0F, ry, w + 4.0F, H));
         boolean fav = f.favourite();
         boolean shv = this.pin(p, starX - 7.0F, ay - 7.0F, 14.0F, 14.0F);
         float sH = this.an.to("pfrs:" + fkey, shv ? 1.0F : 0.0F, 18.0F);
         float vis = fav ? 1.0F : Math.max(hT, sH);
         if (vis > 0.01F) {
            Icons.draw(c, "star", starX, ay, 10.0F + 2.0F * sH, fav ? -670398 : Theme.a(this.th.muted, (0.45F + 0.5F * sH) * vis));
         }

         this.hit(starX - 7.0F, ay - 7.0F, 14.0F, 14.0F, () -> this.friendSet(module, "Favourite", !fav), null);
         if (shv) {
            this.hoverTip("pfrs:" + fkey, fav ? "Unfavourite" : "Favourite");
         } else {
            this.hoverSinceReset("pfrs:" + fkey);
         }
      }

      y += H;
      float expT = this.an.to("pexp:" + key, exp ? 1.0F : 0.0F, 12.0F);
      if (expT > 0.002F) {
         float setH = this.measured.getOrDefault(key, 0.0F);
         float vis = setH * Anim.ease(expT);
         c.save();
         c.scissor(x - 2.0F, y, w + 4.0F, Math.max(0.0F, vis));
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH2 = this.clipH;
         this.intersectClip(x - 2.0F, y, w + 4.0F, vis);
         c.rect(x - 2.0F, y, w + 4.0F, vis, 0.0F, Theme.a(-16777216, 0.22F));
         c.rect(x - 2.0F, y, 1.5F, vis, 0.0F, Theme.a(this.th.accent, 0.85F));
         float end = this.drawPanelSettings(c, p, module, FriendSettings.PER_FRIEND, x + 4.0F, y + 2.0F, w - 6.0F);
         end += 3.0F;
         float bh = this.setRowH();
         float bw = (w - 6.0F - 6.0F) / 3.0F;
         boolean confirming = ("frdel:" + fkey).equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
         if (PresenceClient.confirmed(f)) {
            this.pButton(c, p, "pfr/msg:" + fkey, "Message", x + 4.0F, end, w - 6.0F, bh, this.th.accent, () -> this.openDm(f));
            end += bh + 3.0F;
         } else if (PresenceClient.state() == PresenceClient.State.CONNECTED) {
            this.pButton(c, p, "pfr/req:" + fkey, "Send friend request", x + 4.0F, end, w - 6.0F, bh, this.th.accent, () -> {
               String e = PresenceClient.sendRequest(f.name(), f.hasUuid() ? f.uuid() : null, false);
               this.toast(e == null ? "Request sent to " + f.name() : e);
            });
            end += bh + 3.0F;
         }

         this.pButton(c, p, "pfr/copy:" + fkey, "Copy name", x + 4.0F, end, bw, bh, this.th.text, () -> {
            this.store.setClipboard(f.name());
            this.toast("Copied " + f.name());
         });
         this.pButton(c, p, "pfr/reset:" + fkey, "Reset", x + 4.0F + bw + 3.0F, end, bw, bh, this.th.text, () -> {
            FriendManager.resetFriend(fkey);
            this.toast(f.name() + " reset");
         });
         this.pButton(c, p, "pfr/del:" + fkey, confirming ? "Sure?" : "Remove", x + 4.0F + (bw + 3.0F) * 2.0F, end, bw, bh, -43920, () -> this.confirm("frdel:" + fkey, () -> {
            FriendManager.removeByKey(fkey);
            this.expanded.remove(key);
            this.toast("Removed " + f.name());
         }));
         end += bh + 5.0F;
         this.measured.put(key, end - y);
         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH2;
         c.restore();
         y += vis;
      }

      return y + 2.0F;
   }

   private void openFriendMenu(String menuId, Friend f, String module, float x, float y, float w, float h) {
      if (this.pop != null && menuId.equals(this.pop.id)) {
         this.pop = null;
      } else {
         String fkey = f.key();
         List<String> opts = new ArrayList<>(List.of(f.favourite() ? "Unfavourite" : "Favourite", "Copy name"));
         PresenceClient.Presence pr = PresenceClient.presenceOf(f);
         if (GlobalChat.enabled() && pr != null && pr.online()) {
            opts.add("Mention in chat");
         }

         opts.add("Remove");
         this.openList(menuId, x, y, w, h, 130.0F, opts, "", v -> {
            switch (v) {
               case "Remove":
                  if (!("frdel:" + fkey).equals(this.confirmId) || this.time - this.confirmAt >= 2.5F) {
                     this.toast("Pick Remove again to confirm");
                  }

                  this.confirm("frdel:" + fkey, () -> {
                     FriendManager.removeByKey(fkey);
                     this.toast("Removed " + f.name());
                  });
                  break;
               case "Copy name":
                  this.store.setClipboard(f.name());
                  this.toast("Copied " + f.name());
                  break;
               case "Mention in chat":
                  this.pFriendsTab = "Chat";
                  this.friendsTab = "Chat";
                  this.mentionInChat(f.name());
                  break;
               default:
                  this.friendSet(module, "Favourite", !f.favourite());
            }
         });
      }
   }

   private float drawPanelFriendSettings(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      for (Entry<String, List<GuiData.Setting>> sec : FriendSettings.GLOBAL_SECTIONS.entrySet()) {
         y = this.pLabel(c, x, y, sec.getKey().toUpperCase(Locale.ROOT));
         y = this.drawPanelSettings(c, p, "§friends", sec.getValue(), x, y, w);
         if ("Chat".equals(sec.getKey())) {
            Map<String, String> hidden = GlobalChat.hiddenEntries();
            if (!hidden.isEmpty()) {
               float rh = this.setRowH();
               float fs = this.pFont() - 1.0F;
               y += 2.0F;
               c.text("inter-med", fs - 0.5F, x, y + rh / 2.0F, 17, this.th.muted, "Hidden in chat");
               y += rh;

               for (Entry<String, String> e : hidden.entrySet()) {
                  float bw = c.textWidth("inter-med", fs - 1.5F, "Unhide") + 12.0F;
                  c.text("inter-med", fs, x + 4.0F, y + rh / 2.0F, 17, Theme.a(this.th.text, 0.85F), this.ellipsize(c, "inter-med", fs, e.getValue(), w - bw - 10.0F));
                  this.pButton(c, p, "pfr/unhide:" + e.getKey(), "Unhide", x + w - bw, y + 1.0F, bw, rh - 2.0F, this.th.accent, () -> {
                     GlobalChat.setHidden(e.getKey(), e.getValue(), false);
                     this.toast("Showing " + e.getValue() + " again");
                  });
                  y += rh;
               }
            }
         }

         y += 5.0F;
      }

      if (FriendManager.count() > 0) {
         boolean confirming = "frclear".equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
         y = this.pAction(c, p, "pfr/clear", confirming ? "Click again to remove everyone" : "Remove all friends", x, y, w, () -> this.confirm("frclear", () -> {
            FriendManager.clear();
            this.toast("Friends list cleared");
         }));
      }

      return y;
   }

   boolean panelMode() {
      return "Panels".equals(this.gstr("GUI", "ClickGUI"));
   }

   public float setting3D(String key, double def) {
      return this.gnum(key, def);
   }

   public boolean flag3D(String key, boolean def) {
      return this.gbool(key, def);
   }

   public float[] canvas3D(float flatW, float flatH) {
      if (this.panelMode()) {
         return new float[]{flatW, flatH};
      }

      if (this.tabsMode()) {
         return new float[]{this.gnum("Tabs Width", 640.0) + 90.0F, this.gnum("Tabs Height", 560.0) + 90.0F};
      }

      float W = this.gnum("Window Width", 1000.0);
      float H = this.gnum("Window Height", 650.0);
      return new float[]{W + 90.0F, H + 90.0F};
   }

   private float panelW() {
      return this.gnum("Panel Width", 118.0);
   }

   private float rowH() {
      return this.tabsMode() ? this.tfs() + 9.0F : this.gnum("Row Height", 18.0);
   }

   private float headH() {
      return this.gnum("Header Height", 15.0);
   }

   private float pFont() {
      return this.tabsMode() ? this.tfs() + 1.0F : Math.max(8.5F, Math.min(14.0F, this.rowH() * 0.58F));
   }

   private float setRowH() {
      return Math.max(14.0F, this.rowH() - 2.0F);
   }

   private void ensurePanels() {
      if (!this.panelsLoaded || this.panels.isEmpty()) {
         this.panelsLoaded = true;
         this.panels.clear();
         this.panelOrder.clear();
         List<String> names = new ArrayList<>(this.data.categories.keySet());
         if (!names.contains("Client")) {
            names.add("Client");
         }

         if (!names.contains("Configs")) {
            names.add("Configs");
         }

         if (!names.contains("Friends")) {
            names.add("Friends");
         }

         float gap = 14.0F;
         float x = 20.0F;
         float y = 22.0F;

         for (String n : names) {
            float W = !"Friends".equals(n) && !"Configs".equals(n) ? this.panelW() : this.friendsPanelW();
            if (x + W > this.sw - 10.0F && x > 20.0F) {
               x = 20.0F;
               y += 280.0F;
            }

            this.panels.put(n, new ClickGui.Panel(n, x, y));
            this.panelOrder.add(n);
            x += W + gap;
         }

         List<String> saved = list(this.store.gui("Panel Layout"), List.of());
         List<String> order = new ArrayList<>();

         for (String e : saved) {
            String[] f = e.split("\\|");
            ClickGui.Panel p = f.length >= 4 ? this.panels.get(f[0]) : null;
            if (p != null) {
               try {
                  p.x = Float.parseFloat(f[1]);
                  p.y = Float.parseFloat(f[2]);
               } catch (NumberFormatException var15) {
               }

               p.open = !"false".equals(f[3]);
               order.add(p.name);
            }
         }

         if (!order.isEmpty()) {
            List<String> rest = new ArrayList<>(this.panelOrder);
            rest.removeAll(order);
            float right = 0.0F;

            for (String n : order) {
               right = Math.max(right, this.panels.get(n).x + this.pw(this.panels.get(n)));
            }

            float nx = right + gap;
            float ny = 22.0F;

            for (String n : rest) {
               ClickGui.Panel p = this.panels.get(n);
               float W = this.pw(p);
               if (nx + W > this.sw - 10.0F) {
                  nx = 20.0F;
                  ny += 280.0F;
               }

               p.x = nx;
               p.y = ny;
               nx += W + gap;
            }

            this.panelOrder.clear();
            this.panelOrder.addAll(rest);
            this.panelOrder.addAll(order);
         }
      }
   }

   private void savePanels() {
      List<String> out = new ArrayList<>();

      for (String n : this.panelOrder) {
         ClickGui.Panel p = this.panels.get(n);
         out.add(n + "|" + Math.round(p.x) + "|" + Math.round(p.y) + "|" + p.open);
      }

      try {
         this.store.setGui("Panel Layout", out);
      } catch (Throwable var5) {
      }
   }

   private void resetPanels() {
      try {
         this.store.setGui("Panel Layout", new ArrayList());
      } catch (Throwable var2) {
      }

      this.panelsLoaded = false;
      this.panels.clear();
      this.an.resetPrefix("pOpen:");
      this.toast("Panel layout reset");
   }

   private void bringToFront(ClickGui.Panel p) {
      if (!this.panelOrder.isEmpty() && !this.panelOrder.get(this.panelOrder.size() - 1).equals(p.name)) {
         this.panelOrder.remove(p.name);
         this.panelOrder.add(p.name);
         this.savePanels();
      }
   }

   private ClickGui.Panel panelAt(float x, float y) {
      ClickGui.Panel top = null;

      for (String n : this.panelOrder) {
         ClickGui.Panel p = this.panels.get(n);
         if (p != null && x >= p.x && x <= p.x + this.pw(p) && y >= p.y && y <= p.y + p.h) {
            top = p;
         }
      }

      return top;
   }

   private boolean pin(ClickGui.Panel p, float x, float y, float w, float h) {
      return p.name.equals(this.hoverPanel) && this.draggingPanel == null && this.in(x, y, w, h);
   }

   private void drawPanels(Canvas c) {
      this.ensurePanels();

      for (ClickGui.Panel p : this.panels.values()) {
         p.x = Math.max(30.0F - this.pw(p), Math.min(this.sw - 30.0F, p.x));
         p.y = Math.max(0.0F, Math.min(this.sh - this.headH(), p.y));
      }

      ClickGui.Panel top = this.pop == null ? this.panelAt(this.wmx, this.wmy) : null;
      this.hoverPanel = top == null ? null : top.name;
      this.drawGrid(c);
      int i = 0;

      for (String n : new ArrayList<>(this.panelOrder)) {
         this.drawPanel(c, this.panels.get(n), this.pw(this.panels.get(n)), i++);
      }

      this.drawPanelSearch(c);
      if (this.pop != null && !this.snapPass) {
         this.drawPop(c);
      }
   }

   private void drawPanel(Canvas c, ClickGui.Panel p, float W, int idx) {
      float realX = p.x;
      float realY = p.y;
      p.x = this.an.to("pX:" + p.name, realX, 38.0F);
      p.y = this.an.to("pY:" + p.name, realY, 38.0F);

      try {
         this.drawPanelAt(c, p, W, idx);
      } finally {
         p.x = realX;
         p.y = realY;
      }
   }

   private void drawPanelAt(Canvas c, ClickGui.Panel p, float W, int idx) {
      float hh = this.headH();
      float fs = this.pFont();
      float R = this.gnum("Panel Rounding", 3.0);
      float st = this.snapPass ? 1.0F : Anim.easeOutCubic((this.time * this.an_speed() - 0.045F * idx) / 0.38F);
      String pa = this.snapPass ? "Match Open/Close" : this.gstr("Panel Animation", "Match Open/Close");
      boolean custom = !"Match Open/Close".equals(pa);
      float unfold = 1.0F;
      if (custom) {
         int n = Math.max(1, this.panelOrder.size() - 1);
         float order = Math.min(1.0F, (float)idx / n);
         float raw;
         if (this.closing && "Panel".equals(this.closeStyle)) {
            float ct = this.closeT();
            raw = 1.0F - Math.max(0.0F, Math.min(1.0F, ct * 1.35F - 0.35F * (1.0F - order)));
         } else {
            float tt = this.time * this.an_speed();

            float lag = switch (pa) {
               case "Cascade" -> 0.055F * idx;
               case "Unfold" -> 0.04F * idx;
               case "Drift" -> 0.035F * idx;
               case "Spread" -> 0.02F * idx;
               default -> 0.03F * idx;
            };
            raw = Math.max(0.0F, Math.min(1.0F, (tt - lag) / ("Unfold".equals(pa) ? 0.55F : 0.42F)));
         }

         st = raw;
         if (st <= 0.001F) {
            p.h = hh;
            return;
         }
      } else if (st <= 0.001F) {
         p.h = hh;
         return;
      }

      c.save();
      String oa = custom ? "Custom:" + pa : this.gstr("Open Animation", "Fade");
      float pcx = p.x + W / 2.0F;
      float pcy = p.y + hh / 2.0F;
      switch (oa) {
         case "Custom:Cascade": {
            float e = Anim.easeOutCubic(st);
            c.alpha(Math.min(1.0F, st * 1.6F));
            c.translate(0.0F, -(1.0F - e) * 26.0F);
            c.translate(pcx, pcy);
            c.scale(0.96F + 0.04F * e);
            c.translate(-pcx, -pcy);
            break;
         }
         case "Custom:Unfold":
            float head = Anim.easeOutCubic(Math.min(1.0F, st / 0.45F));
            unfold = Anim.ease(Math.max(0.0F, (st - 0.35F) / 0.65F));
            c.alpha(Math.min(1.0F, head * 1.4F));
            c.translate(pcx, p.y);
            c.scale2(0.55F + 0.45F * head, 1.0F);
            c.translate(-pcx, -p.y);
            break;
         case "Custom:Drift": {
            float e = Anim.easeOutCubic(st);
            float from = p.x + W / 2.0F < this.sw / 2.0F ? -(p.x + W + 40.0F) : this.sw - p.x + 40.0F;
            c.alpha(Math.min(1.0F, st * 1.3F));
            c.translate(from * (1.0F - e), 0.0F);
            break;
         }
         case "Custom:Spread": {
            float e = Anim.easeOutBack(Math.min(1.0F, st));
            float dx = this.sw / 2.0F - pcx;
            float dy = this.sh / 2.0F - pcy;
            c.alpha(Math.min(1.0F, st * 2.0F));
            c.translate(dx * (1.0F - e), dy * (1.0F - e));
            c.translate(pcx, pcy);
            c.scale(0.85F + 0.15F * Math.min(1.0F, e));
            c.translate(-pcx, -pcy);
            break;
         }
         case "Custom:Soft Focus": {
            float e = Anim.ease(st);
            c.alpha(e);
            c.translate(pcx, pcy);
            c.scale(1.06F - 0.06F * e);
            c.translate(-pcx, -pcy);
            c.translate(0.0F, (1.0F - e) * 6.0F);
            break;
         }
         case "Pop":
            c.alpha(Math.min(1.0F, st * 1.4F));
            c.translate(pcx, pcy);
            c.scale(0.82F + 0.18F * Anim.easeOutBack(st));
            c.translate(-pcx, -pcy);
            break;
         case "Slide":
            c.alpha(Math.min(1.0F, st * 2.0F));
            c.translate(0.0F, -(1.0F - st) * (p.y + 160.0F));
            break;
         case "Zoom":
            c.alpha(st);
            c.translate(this.sw / 2.0F, this.sh / 2.0F);
            c.scale(0.6F + 0.4F * st);
            c.translate(-this.sw / 2.0F, -this.sh / 2.0F);
            break;
         case "CRT":
            c.alpha(Math.min(1.0F, st * 3.0F));
            c.translate(pcx, p.y);
            c.scale2(1.0F, 0.02F + 0.98F * st);
            c.translate(-pcx, -p.y);
            break;
         default:
            c.alpha(st);
            c.translate(0.0F, (1.0F - st) * -10.0F);
      }

      if (p.contentH <= 1.0F) {
         p.contentH = !"Friends".equals(p.name) && !"Configs".equals(p.name)
            ? (("Client".equals(p.name) ? 2 + CLIENT_SECTIONS.length : 0) + this.data.categories.getOrDefault(p.name, List.of()).size()) * this.rowH() + 2.0F
            : 200.0F;
      }

      float maxBody = Math.max(60.0F, Math.min(this.gnum("Panel Height", 760.0), this.sh - p.y - hh - 10.0F));
      float viewH = Math.min(p.contentH, maxBody);
      float ot = this.an.to("pOpen:" + p.name, p.open ? 1.0F : 0.0F, 12.0F);
      float bodyH = viewH * Anim.ease(ot) * unfold;
      p.h = hh + bodyH;
      p.maxScroll = Math.max(0.0F, p.contentH - viewH);
      p.scrollT = Math.max(0.0F, Math.min(p.maxScroll, p.scrollT));
      Float pWant = this.pendingPanelScroll.get(p.name);
      if (pWant != null) {
         if (!(p.maxScroll + 0.5F >= pWant) && ++this.restoreFrames <= 40) {
            p.scrollT = pWant;
         } else {
            p.scrollT = Math.min(pWant, p.maxScroll);
            this.pendingPanelScroll.remove(p.name);
         }

         this.an.set("pScroll:" + p.name, p.scrollT);
      }

      p.scroll = this.an.to("pScroll:" + p.name, p.scrollT, 16.0F);
      boolean dragging = p.name.equals(this.draggingPanel);
      int body = Theme.mix(this.th.bg, -16777216, 0.3F);
      if (this.gbool("Panel Shadow", true)) {
         c.shadow(p.x, p.y + 3.0F, W, p.h, R, dragging ? 22.0F : 14.0F, Theme.a(-16777216, dragging ? 0.6F : 0.45F));
      }

      if (this.frosted() && bodyH > 1.0F) {
         c.glass(p.x, p.y, W, p.h, R, 1.0F);
      }

      c.rect(p.x, p.y, W, p.h, R, Theme.a(body, this.gnum("Body Opacity", 0.92)));
      String hs = this.gstr("Header Style", "Filled");

      int headText = switch (hs) {
         case "Gradient" -> {
            c.gradient(p.x, p.y, W, hh, R, this.th.accent, this.th.accent2, true);
            yield Theme.onColor(this.th.accent);
         }
         case "Line" -> {
            c.rect(p.x, p.y, W, hh, R, Theme.a(Theme.mix(this.th.surface, -16777216, 0.2F), 0.95F));
            c.rect(p.x, p.y + hh - 1.5F, W, 1.5F, 0.0F, this.th.accent);
            yield this.th.text;
         }
         default -> {
            c.rect(p.x, p.y, W, hh, R, this.th.accent);
            yield Theme.onColor(this.th.accent);
         }
      };
      if (bodyH > 0.5F && R > 0.0F && !"Line".equals(hs)) {
         if ("Gradient".equals(hs)) {
            c.gradient(p.x, p.y + hh - R, W, R, 0.0F, this.th.accent, this.th.accent2, true);
         } else {
            c.rect(p.x, p.y + hh - R, W, R, 0.0F, this.th.accent);
         }
      }

      boolean hHead = this.pin(p, p.x, p.y, W, hh);
      if (hHead && !"Line".equals(hs)) {
         c.rect(p.x, p.y, W, hh, R, Theme.a(-1, 0.06F));
      }

      float hfs = Math.max(7.0F, Math.min(fs + 0.5F, hh * 0.7F));
      c.text("inter-sb", hfs, p.x + 7.0F, p.y + hh / 2.0F, 17, headText, this.ellipsize(c, "inter-sb", hfs, p.name, W - 30.0F));
      Icons.draw(c, p.name, p.x + W - 11.0F, p.y + hh / 2.0F, Math.max(7.0F, Math.min(13.0F, hh - 5.0F)), Theme.a(headText, p.open ? 0.9F : 0.55F));
      if ("Friends".equals(p.name)) {
         int n = PresenceClient.incomingRequests().size() + ("Chat".equals(this.pFriendsTab) && p.open ? 0 : GlobalChat.unread()) + DirectMessages.totalUnread();
         if (n > 0) {
            this.badge(c, p.x + W - 22.0F - Math.max(hh - 4.0F, c.textWidth("inter-sb", Math.max(6.0F, hfs - 3.0F), String.valueOf(n)) + 8.0F), p.y + hh / 2.0F, Math.max(6.0F, hfs - 3.0F), n);
         }
      }

      this.hit(p.x, p.y, W, hh, () -> {
         this.bringToFront(p);
         this.draggingPanel = p.name;
         float ox = this.wmx - p.x;
         float oy = this.wmy - p.y;
         this.drag2 = pt -> {
            float nx = pt[0] - ox;
            float ny = pt[1] - oy;
            if (this.gbool("Grid Snap", true)) {
               float g = Math.max(2.0F, this.gnum("Grid Size", 8.0));
               nx = Math.round(nx / g) * g;
               ny = Math.round(ny / g) * g;
            }

            p.x = nx;
            p.y = ny;
         };
      }, () -> {
         p.open = !p.open;
         this.savePanels();
      });
      if (bodyH > 0.5F) {
         float top = p.y + hh;
         this.hit(p.x, top, W, bodyH, () -> {}, () -> {});
         c.save();
         c.scissor(p.x, top, W, bodyH);
         this.clip(p.x, top, W, bodyH);
         float y0 = top - p.scroll;
         float y = y0;
         if ("Client".equals(p.name)) {
            y = this.drawClientPanel(c, p, p.x, y, W);
         } else if ("Friends".equals(p.name)) {
            y = this.drawFriendsPanel(c, p, p.x, y, W);
         } else if ("Configs".equals(p.name)) {
            y = this.drawConfigsPanel(c, p, p.x, y, W);
         } else {
            List<GuiData.Module> mods = this.data.categories.getOrDefault(p.name, List.of());
            boolean any = false;

            for (GuiData.Module m : mods) {
               if (this.matchesSearch(m)) {
                  any = true;
                  y = this.drawPanelModule(c, p, m, p.x, y, W);
               }
            }

            if (!any) {
               c.text("inter", fs - 0.5F, p.x + W / 2.0F, y + this.rowH() / 2.0F + 2.0F, 18, this.th.muted, "No matches");
               y += this.rowH() + 4.0F;
            }
         }

         p.contentH = y - y0 + 2.0F;
         this.noClip();
         c.restore();
         if (p.maxScroll > 1.0F && bodyH > 20.0F) {
            float barH = Math.max(14.0F, bodyH * bodyH / (bodyH + p.maxScroll));
            float barY = top + (bodyH - barH) * (p.scroll / p.maxScroll);
            c.rect(p.x + W - 3.0F, barY, 2.0F, barH, 1.0F, Theme.a(this.th.accent, 0.55F));
         }
      }

      c.restore();
   }

   private void drawGrid(Canvas c) {
      boolean on = this.draggingPanel != null && this.gbool("Grid Snap", true) && this.gbool("Show Grid", true) && !this.snapPass;
      float a = this.an.to("gridA", on ? 1.0F : 0.0F, 10.0F);
      if (!(a < 0.01F)) {
         float g = Math.max(2.0F, this.gnum("Grid Size", 8.0));
         float step = g * (float)Math.max(1.0, Math.ceil(12.0F / g));
         int col = Theme.a(this.th.text, 0.07F * a);

         for (float x = 0.0F; x <= this.sw; x += step) {
            c.line(x, 0.0F, x, this.sh, 1.0F, col);
         }

         for (float y = 0.0F; y <= this.sh; y += step) {
            c.line(0.0F, y, this.sw, y, 1.0F, col);
         }

         ClickGui.Panel p = this.draggingPanel == null ? null : this.panels.get(this.draggingPanel);
         if (p != null) {
            c.stroke(p.x, p.y, this.pw(p), p.h, this.gnum("Panel Rounding", 3.0), 1.2F, Theme.a(this.th.accent, 0.7F * a));
         }
      }
   }

   private boolean matchesSearch(GuiData.Module m) {
      if (this.search.text.isEmpty()) {
         return true;
      }

      String q = this.search.text.toLowerCase(Locale.ROOT);
      if (!m.name.toLowerCase(Locale.ROOT).contains(q) && !m.description.toLowerCase(Locale.ROOT).contains(q)) {
         for (GuiData.Setting s : m.settings) {
            if (s.name.toLowerCase(Locale.ROOT).contains(q)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void drawPanelSearch(Canvas c) {
      boolean sf = this.focused == this.search;
      float t = this.an.to("pSearch", !sf && this.search.text.isEmpty() ? 0.0F : 1.0F, 14.0F);
      if (!(t < 0.01F)) {
         float w = 240.0F;
         float h = 26.0F;
         float x = this.sw / 2.0F - w / 2.0F;
         float y = this.sh - 52.0F + (1.0F - t) * 20.0F;
         c.save();
         c.alpha(t);
         c.shadow(x, y + 2.0F, w, h, 6.0F, 14.0F, Theme.a(-16777216, 0.5F));
         c.rect(x, y, w, h, 6.0F, Theme.a(Theme.mix(this.th.bg, -16777216, 0.3F), 0.95F));
         c.stroke(x, y, w, h, 6.0F, 1.0F, sf ? this.th.accent : Theme.a(this.th.border, 0.9F));
         Icons.draw(c, "search", x + 14.0F, y + h / 2.0F, 12.0F, sf ? this.th.accent : this.th.muted);
         boolean empty = this.search.text.isEmpty();
         c.text("inter", 11.5F, x + 26.0F, y + h / 2.0F, 17, empty ? this.th.muted : this.th.text, empty ? "Search modules…" : this.search.text);
         if (sf) {
            this.caret(c, "inter", 11.5F, x + 26.0F, y + h / 2.0F, this.search);
         }

         c.restore();
         this.hit(x, y, w, h, () -> this.focus(this.search, null), () -> this.search.set(""));
      }
   }

   private float drawPanelModule(Canvas c, ClickGui.Panel p, GuiData.Module m, float x, float y, float W) {
      String name = m.name;
      float rh = this.rowH();
      float fs = this.pFont();
      boolean on = this.store.isEnabled(name);
      float enT = this.an.to("pen:" + name, on ? 1.0F : 0.0F, 14.0F);
      if (y + rh > this.clipY && y < this.clipY + this.clipH) {
         boolean hv = this.pin(p, x, y, W, rh);
         float hT = this.an.to("phv:" + name, hv ? 1.0F : 0.0F, 18.0F);
         String hl = this.gstr("Enabled Highlight", "Text");
         if (this.gbool("Row Dividers", true)) {
            c.line(x + 4.0F, y + rh, x + W - 4.0F, y + rh, 1.0F, Theme.a(this.th.text, 0.045F));
         }

         if (hT > 0.01F) {
            c.rect(x, y, W, rh, 0.0F, Theme.a(this.th.text, 0.06F * hT));
         }
         int vis = switch (hl) {
            case "Fill" -> {
               if (enT > 0.01F) {
                  c.rect(x + 1.5F, y + 1.0F, W - 3.0F, rh - 2.0F, 1.5F, Theme.a(this.th.accent, 0.85F * enT));
               }

               yield Theme.mix(Theme.a(this.th.text, 0.8F + 0.2F * hT), Theme.onColor(this.th.accent), enT);
            }
            case "Bar" -> {
               if (enT > 0.01F) {
                  c.rect(x + 1.5F, y + 3.0F, 2.0F, rh - 6.0F, 1.0F, Theme.a(this.th.accent, enT));
               }

               yield Theme.mix(Theme.a(this.th.muted, 0.9F + 0.1F * hT), this.th.text, Math.max(enT, 0.45F * hT));
            }
            default -> Theme.mix(Theme.a(this.th.text, 0.78F + 0.22F * hT), this.th.accent, enT);
         };
         float right = x + W - 6.0F;
         boolean exp = this.expanded.contains(name);
         String bind = this.store.bind(name);
         boolean listening = name.equals(this.listenModule) && this.listenSetting == null;
         if (!m.settings.isEmpty() || exp) {
            int dc = Theme.a(exp ? this.th.accent : this.th.muted, exp ? 1.0F : 0.5F + 0.4F * hT);

            for (int k = 0; k < 3; k++) {
               c.circle(right - 1.0F - k * 3.2F, y + rh / 2.0F, 0.95F, "Fill".equals(hl) && enT > 0.5F ? Theme.a(Theme.onColor(this.th.accent), 0.7F) : dc);
            }

            right -= 12.0F;
         }

         if (listening || bind != null && !bind.isEmpty() && !"null".equals(bind)) {
            String bl = listening ? "…" : bind;
            float bw = c.textWidth("inter-med", fs - 1.5F, bl);
            c.text(
               "inter-med",
               fs - 1.5F,
               right,
               y + rh / 2.0F,
               20,
               "Fill".equals(hl) && enT > 0.5F ? Theme.a(Theme.onColor(this.th.accent), 0.7F) : Theme.a(listening ? this.th.accent : this.th.muted, 0.85F),
               bl
            );
            right -= bw + 5.0F;
         }

         c.text("inter-med", fs, x + 7.0F + 1.5F * hT, y + rh / 2.0F, 17, vis, this.ellipsize(c, "inter-med", fs, name, right - x - 9.0F));
         this.hit(x, y, W, rh, () -> this.toggleModule(m), () -> this.toggleExpand(name));
         if (hv && this.gbool("Show Descriptions", true) && !m.description.isEmpty()) {
            this.hoverTip("p:" + name, m.description);
         } else {
            this.hoverSinceReset("p:" + name);
         }
      }

      y += rh;
      String mk = "p:" + name;
      float expT = this.an.to("pexp:" + name, this.expanded.contains(name) ? 1.0F : 0.0F, 12.0F);
      if (expT > 0.002F) {
         float setH = this.measured.getOrDefault(mk, 0.0F);
         float vis = setH * Anim.ease(expT);
         c.save();
         c.scissor(x, y, W, Math.max(0.0F, vis));
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH = this.clipH;
         this.intersectClip(x, y, W, vis);
         c.rect(x, y, W, vis, 0.0F, Theme.a(-16777216, 0.28F));
         c.rect(x, y, 1.5F, vis, 0.0F, Theme.a(this.th.accent, 0.85F));
         float end = this.drawPanelBindRow(c, p, name, x + 6.0F, y + 2.0F, W - 10.0F);
         end = this.drawPanelSettings(c, p, name, m.settings, x + 6.0F, end, W - 10.0F);
         end = this.drawPanelResetRow(c, p, name, x + 6.0F, end, W - 10.0F);
         this.measured.put(mk, end - y + 3.0F);
         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH;
         c.restore();
         y += vis;
      }

      return y;
   }

   private float drawPanelBindRow(Canvas c, ClickGui.Panel p, String module, float x, float y, float w) {
      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      boolean listening = module.equals(this.listenModule) && this.listenSetting == null;
      String bind = this.store.bind(module);
      boolean unset = bind == null || bind.isEmpty() || "null".equals(bind);
      boolean hold = "hold".equals(this.store.bindMode(module));
      String v = listening ? "Press a key…" : (unset ? "None" : bind) + (hold && !listening ? " · Hold" : "");
      boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      if (hv) {
         c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
      }

      c.text("inter-med", fs, x, y + h / 2.0F, 17, Theme.a(this.th.text, 0.85F), "Bind");
      float lt = listening ? 0.5F + 0.5F * (float)Math.sin(this.time * 8.0F) : 0.0F;
      c.text(
         "inter-med", fs, x + w, y + h / 2.0F, 20, listening ? Theme.a(this.th.accent, 0.5F + 0.5F * lt) : (unset ? this.th.muted : this.th.accent), this.ellipsize(c, "inter-med", fs, v, w - 30.0F)
      );
      this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
         this.listenModule = module;
         this.listenSetting = null;
      }, () -> {
         this.store.setBind(module, unset ? null : bind, hold ? "toggle" : "hold");
         this.toast(module + ": " + (hold ? "toggle" : "hold") + " mode");
      });
      return y + h;
   }

   private float drawPanelResetRow(Canvas c, ClickGui.Panel p, String module, float x, float y, float w) {
      float h = this.setRowH();
      float fs = this.pFont() - 1.5F;
      String id = "reset:" + module;
      boolean confirming = id.equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
      boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      c.text(
         "inter-med",
         fs,
         x + w / 2.0F,
         y + h / 2.0F,
         18,
         confirming ? this.th.accent : Theme.mix(this.th.muted, this.th.text, hv ? 0.7F : 0.0F),
         confirming ? "Click again to reset" : "Reset settings"
      );
      this.hit(x - 4.0F, y, w + 8.0F, h, () -> this.confirm(id, () -> {
         this.store.resetModule(module);
         this.toast(module + " reset to defaults");
      }), null);
      return y + h;
   }

   private float drawPanelSettings(Canvas c, ClickGui.Panel p, String module, List<GuiData.Setting> settings, float x, float y, float w) {
      if (HudEditorPanel.isHudModule(module)) {
         return this.pAction(c, p, "phudedit:" + module, "Open in HUD editor", x, y, w, () -> this.openHudEditor(module));
      }

      for (GuiData.Setting s : settings) {
         if (this.visible(module, s)) {
            Map<String, List<String>> groups = "select".equals(s.type) ? s.groups() : null;
            if (groups != null) {
               String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
               String cat = null;

               for (Entry<String, List<String>> e : groups.entrySet()) {
                  if (e.getValue().contains(cur)) {
                     cat = e.getKey();
                     break;
                  }
               }

               if (cat == null) {
                  cat = groups.keySet().iterator().next();
               }

               y = this.pSelect(c, p, module + "/" + s.name + "/type", s.name + " Type", x, y, w, new ArrayList<>(groups.keySet()), cat, v -> {
                  List<String> entries = groups.get(v);
                  if (entries != null && !entries.isEmpty() && !entries.contains(str(this.get(module, s.name), ""))) {
                     this.set(module, s.name, entries.get(0));
                  }
               });
               List<String> entries = groups.getOrDefault(cat, List.of());
               y = this.pSelect(c, p, module + "/" + s.name, s.name, x, y, w, entries, !entries.contains(cur) && !entries.isEmpty() ? entries.get(0) : cur, v -> this.set(module, s.name, v));
            } else {
               y = this.pControl(c, p, module, s, x, y, w);
            }
         }
      }

      return y;
   }

   private float pControl(Canvas c, ClickGui.Panel p, String module, GuiData.Setting s, float x, float y, float w) {
      String id = module + "/" + s.name;
      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      int label = Theme.a(this.th.text, 0.85F);
      if (isModelFile(module, s)) {
         return this.pSelect(c, p, id, s.name, x, y, w, this.modelFileOptions(), this.modelFileValue(module, s), vx -> this.pickModelFile(module, s.name, vx));
      }

      if (isSoundSelect(module, s)) {
         return this.pSelect(c, p, id, s.name, x, y, w, this.soundOptions(s), str(this.get(module, s.name), String.valueOf(s.defaultValue())), vx -> this.pickSound(module, s.name, vx));
      }

      switch (s.type) {
         case "check": {
            boolean v = bool(this.get(module, s.name), (Boolean)s.defaultValue());
            float t = this.an.to("pck:" + id, v ? 1.0F : 0.0F, 18.0F);
            if (hv) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
            }

            c.text("inter-med", fs, x, y + h / 2.0F, 17, label, this.ellipsize(c, "inter-med", fs, s.name, w - 16.0F));
            float b = Math.min(9.0F, h - 6.0F);
            float bx = x + w - b;
            float by = y + (h - b) / 2.0F;
            c.rect(bx, by, b, b, 2.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.12F), 0.95F));
            if (t > 0.01F) {
               c.rect(bx, by, b, b, 2.0F, Theme.a(this.th.accent, t));
            }

            if (t > 0.5F) {
               Icons.draw(c, "check", bx + b / 2.0F, by + b / 2.0F, b * 0.95F, Theme.a(Theme.onColor(this.th.accent), (t - 0.5F) * 2.0F));
            }

            this.hit(x - 4.0F, y, w + 8.0F, h, () -> this.set(module, s.name, !v), null);
            return y + h;
         }
         case "range": {
            double min = s.num(0, 0.0);
            double max = s.num(1, 1.0);
            double def = s.num(2, min);
            double step = s.num(3, 0.0);
            double v = num(this.get(module, s.name), def);
            float frac = (float)((v - min) / Math.max(1.0E-9, max - min));
            float shown = this.an.to("psl:" + id, frac, 22.0F);
            boolean dragging = this.drag != null && id.equals(this.dragId);
            boolean editing = this.focused != null && id.equals(this.focusId);
            float sy = y + 1.0F;
            float shh = h - 2.0F;
            c.rect(x - 2.0F, sy, w + 4.0F, shh, 2.0F, Theme.a(this.th.text, 0.05F + (hv ? 0.03F : 0.0F)));
            c.rect(x - 2.0F, sy, Math.max(0.0F, (w + 4.0F) * shown), shh, 2.0F, Theme.a(this.th.accent, dragging ? 0.75F : 0.55F + (hv ? 0.1F : 0.0F)));
            c.text("inter-med", fs, x + 1.0F, y + h / 2.0F, 17, this.th.text, this.ellipsize(c, "inter-med", fs, s.name, w * 0.62F));
            String txt = editing ? this.focused.text : fmt(v, step);
            c.text("inter-sb", fs, x + w - 1.0F, y + h / 2.0F, 20, editing ? this.th.accent : this.th.text, txt);
            if (editing) {
               this.caret(c, "inter-sb", fs, x + w - 1.0F - c.textWidth("inter-sb", fs, txt), y + h / 2.0F, this.focused);
            }

            Consumer<Float> apply = px -> {
               double tt = Math.max(0.0F, Math.min(1.0F, (px - x) / w));
               this.set(module, s.name, snap(min + tt * (max - min), step, min));
            };
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               this.dragId = id;
               this.drag = apply;
               apply.accept(this.wmx);
            }, () -> {
               TextField f = new TextField(12);
               f.set(fmt(v, step));
               f.allSelected = true;
               this.focusId = id;
               this.focus(f, () -> {
                  try {
                     this.set(module, s.name, snap(Math.max(min, Math.min(max, Double.parseDouble(f.text.trim()))), step, min));
                  } catch (NumberFormatException var11x) {
                  }
               });
            });
            return y + h;
         }
         case "minmax": {
            double min = s.num(0, 0.0);
            double max = s.num(1, 1.0);
            double step = s.num(4, 0.0);
            double lo = num(this.get(module, s.name + "_lo"), s.num(2, min));
            double hi = num(this.get(module, s.name + "_hi"), s.num(3, max));
            float a = (float)((lo - min) / Math.max(1.0E-9, max - min));
            float b = (float)((hi - min) / Math.max(1.0E-9, max - min));
            float sa = this.an.to("pmA:" + id, a, 22.0F);
            float sb = this.an.to("pmB:" + id, b, 22.0F);
            c.rect(x - 2.0F, y + 1.0F, w + 4.0F, h - 2.0F, 2.0F, Theme.a(this.th.text, 0.05F + (hv ? 0.03F : 0.0F)));
            c.rect(x - 2.0F + (w + 4.0F) * sa, y + 1.0F, Math.max(2.0F, (w + 4.0F) * (sb - sa)), h - 2.0F, 2.0F, Theme.a(this.th.accent, 0.55F + (hv ? 0.1F : 0.0F)));
            c.text("inter-med", fs, x + 1.0F, y + h / 2.0F, 17, this.th.text, this.ellipsize(c, "inter-med", fs, s.name, w * 0.45F));
            c.text("inter-sb", fs, x + w - 1.0F, y + h / 2.0F, 20, this.th.text, fmt(lo, step) + "-" + fmt(hi, step));
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               double t0 = Math.max(0.0F, Math.min(1.0F, (this.wmx - x) / w));
               int[] grab = new int[]{minmaxGrab(t0, a, b)};
               this.dragId = id;
               this.drag = px -> {
                  double t = Math.max(0.0F, Math.min(1.0F, (px - x) / w));
                  double vx = snap(min + t * (max - min), step, min);
                  double curLo = num(this.get(module, s.name + "_lo"), s.num(2, min));
                  double curHi = num(this.get(module, s.name + "_hi"), s.num(3, max));
                  grab[0] = this.minmaxApply(module, s.name, vx, curLo, curHi, grab[0]);
               };
               this.drag.accept(this.wmx);
            }, null);
            return y + h;
         }
         case "select": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            return this.pSelect(c, p, id, s.name, x, y, w, s.options(), cur, vx -> this.set(module, s.name, vx));
         }
         case "skinselect":
         case "capeselect": {
            List<String> opts = new ArrayList<>(List.of("none"));
            if ("skinselect".equals(s.type)) {
               opts.add("default");
               opts.addAll(this.store.customSkins());
               opts.add("__add_skin__");
            } else {
               opts.addAll(this.data.capes);
               opts.addAll(this.store.customCapes());
            }

            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            return this.pSelect(c, p, id, s.name, x, y, w, opts, cur, vx -> this.pickAsset(module, s.name, vx));
         }
         case "color":
            String v = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            int col = this.resolveColor(v);
            boolean isOpen = this.pop != null && this.pop.color && id.equals(this.pop.id);
            if (hv) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
            }

            c.text("inter-med", fs, x, y + h / 2.0F, 17, label, this.ellipsize(c, "inter-med", fs, s.name, w - 26.0F));
            float sw2 = 18.0F;
            float sh2 = Math.min(9.0F, h - 6.0F);
            float sx = x + w - sw2;
            float sy = y + (h - sh2) / 2.0F;
            c.rect(sx, sy, sw2, sh2, 2.0F, 0xFF000000 | col);
            c.stroke(sx, sy, sw2, sh2, 2.0F, 1.0F, isOpen ? -1 : Theme.a(-1, 0.25F));
            float fy = y;
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               if (isOpen) {
                  this.pop = null;
               } else {
                  this.openColor(id, module, s.name, x + w, fy + h / 2.0F, h / 2.0F + 4.0F, col);
               }
            }, null);
            return y + h;
         case "text":
            String value = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            boolean editing = this.focused != null && id.equals(this.focusId);
            if (hv || editing) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(editing ? this.th.accent : this.th.text, editing ? 0.12F : 0.05F));
            }

            c.save();
            c.scissor(x, y, w, h);
            if (editing) {
               c.text("inter", fs, x, y + h / 2.0F, 17, this.th.text, this.focused.text);
               this.caret(c, "inter", fs, x, y + h / 2.0F, this.focused);
            } else {
               c.text("inter-med", fs, x, y + h / 2.0F, 17, label, s.name);
               float lw = c.textWidth("inter-med", fs, s.name) + 6.0F;
               String shown = value.isEmpty() ? (s.placeholder().isEmpty() ? "…" : s.placeholder()) : value;
               c.text("inter", fs, x + w, y + h / 2.0F, 20, value.isEmpty() ? this.th.muted : this.th.accent, this.ellipsize(c, "inter", fs, shown, w - lw));
            }

            c.restore();
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               TextField f = new TextField(64);
               f.set(value);
               this.focusId = id;
               this.focus(f, () -> this.set(module, s.name, f.text));
            }, null);
            return y + h;
         case "keybind": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            boolean listening = module.equals(this.listenModule) && s.name.equals(this.listenSetting);
            String lbl = listening ? "Press…" : (!"NONE".equals(cur) && !cur.isEmpty() ? cur : "None");
            if (hv) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
            }

            float lw = c.textWidth("inter-med", fs, lbl);
            c.text("inter-med", fs, x, y + h / 2.0F, 17, label, this.ellipsize(c, "inter-med", fs, s.name, w - lw - 6.0F));
            c.text("inter-med", fs, x + w, y + h / 2.0F, 20, listening ? this.th.accent : this.th.muted, lbl);
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               this.listenModule = module;
               this.listenSetting = s.name;
            }, () -> this.set(module, s.name, "NONE"));
            return y + h;
         }
         case "multiselect": {
            List<String> cur = list(this.get(module, s.name), s.defaultValue());
            c.text("inter-sb", fs - 0.5F, x, y + h / 2.0F, 17, this.th.muted, this.ellipsize(c, "inter-sb", fs - 0.5F, s.name, w));
            y += h;

            for (String o : s.options()) {
               boolean on = cur.contains(o);
               boolean ohv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
               if (ohv) {
                  c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
               }

               c.text("inter-med", fs, x + 6.0F, y + h / 2.0F, 17, on ? this.th.accent : label, this.ellipsize(c, "inter-med", fs, o, w - 22.0F));
               float b = Math.min(9.0F, h - 6.0F);
               float bx = x + w - b;
               float by = y + (h - b) / 2.0F;
               c.rect(bx, by, b, b, 2.0F, on ? this.th.accent : Theme.a(Theme.mix(this.th.bg, this.th.text, 0.12F), 0.95F));
               if (on) {
                  Icons.draw(c, "check", bx + b / 2.0F, by + b / 2.0F, b * 0.95F, Theme.onColor(this.th.accent));
               }

               this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
                  List<String> next = new ArrayList<>(list(this.get(module, s.name), s.defaultValue()));
                  if (!next.remove(o)) {
                     next.add(o);
                  }

                  this.set(module, s.name, next);
               }, null);
               y += h;
            }

            return y;
         }
         case "blockpicker":
         case "itempicker": {
            String pid = "ids:" + module + "/" + s.name;
            boolean idsOpen = this.pop != null && this.pop.ids && pid.equals(this.pop.id);
            boolean phv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
            if (phv || idsOpen) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(idsOpen ? this.th.accent : this.th.text, idsOpen ? 0.12F : 0.05F));
            }

            List<String> cur = list(this.get(module, s.name), List.of());
            String cnt = cur.isEmpty() ? "None" : String.valueOf(cur.size());
            float cw = c.textWidth("inter-med", fs, cnt);
            float is = Math.min(h - 4.0F, 12.0F);
            float ix = x + w - cw - 4.0F;
            int shown = 0;

            for (String item : cur) {
               if (shown == 3 || ix - is < x + w * 0.45F) {
                  break;
               }

               ix -= is + 1.0F;
               this.itemIcon(c, bare(item), ix, y + (h - is) / 2.0F, is);
               shown++;
            }

            c.text("inter-med", fs, x, y + h / 2.0F, 17, Theme.a(this.th.text, 0.85F), this.ellipsize(c, "inter-med", fs, s.name, ix - x - 4.0F));
            c.text("inter-med", fs, x + w, y + h / 2.0F, 20, cur.isEmpty() ? this.th.muted : this.th.accent, cnt);
            float fy0 = y;
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
               if (idsOpen) {
                  this.pop = null;
                  this.focused = null;
               } else {
                  this.openIds(pid, module, s, x - 4.0F, fy0, w + 8.0F, h);
               }
            }, null);
            return y + h;
         }
         default:
            c.text("inter-med", fs, x, y + h / 2.0F, 17, label, this.ellipsize(c, "inter-med", fs, s.name, w * 0.5F));
            c.text("inter", fs - 1.0F, x + w, y + h / 2.0F, 20, this.th.muted, "Web GUI");
            return y + h;
      }
   }

   private float pSelect(Canvas c, ClickGui.Panel p, String id, String label, float x, float y, float w, List<String> options, String cur, Consumer<String> pick) {
      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      boolean isOpen = this.pop != null && !this.pop.color && id.equals(this.pop.id);
      boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      if (hv || isOpen) {
         c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(isOpen ? this.th.accent : this.th.text, isOpen ? 0.12F : 0.05F));
      }

      String val = display(cur);
      float vw = Math.min(c.textWidth("inter-med", fs, val), w * 0.6F);
      c.text("inter-med", fs, x, y + h / 2.0F, 17, Theme.a(this.th.text, 0.85F), this.ellipsize(c, "inter-med", fs, label, w - vw - 6.0F));
      c.text("inter-med", fs, x + w, y + h / 2.0F, 20, this.th.accent, this.ellipsize(c, "inter-med", fs, val, w * 0.6F));
      this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
         if (isOpen) {
            this.pop = null;
         } else {
            if (!options.isEmpty()) {
               this.openList(id, x - 4.0F, y, w + 8.0F, h, 150.0F, options, cur, pick);
            }
         }
      }, () -> {
         if (!options.isEmpty()) {
            int i = options.indexOf(cur);
            pick.accept(options.get((i + 1) % options.size()));
         }
      });
      return y + h;
   }

   private float drawClientPanel(Canvas c, ClickGui.Panel p, float x, float y, float W) {
      for (GuiData.Module m : this.data.categories.getOrDefault("Client", List.of())) {
         if (this.matchesSearch(m)) {
            y = this.drawPanelModule(c, p, m, p.x, y, W);
         }
      }

      List<String> entries = new ArrayList<>(List.of("Themes"));
      entries.addAll(List.of(CLIENT_SECTIONS));
      String q = this.search.text.toLowerCase(Locale.ROOT);

      for (String e : entries) {
         if (q.isEmpty() || e.toLowerCase(Locale.ROOT).contains(q) || "client".contains(q)) {
            y = this.drawClientEntry(c, p, e, x, y, W);
         }
      }

      return y;
   }

   private float drawClientEntry(Canvas c, ClickGui.Panel p, String entry, float x, float y, float W) {
      String key = "§client:" + entry;
      float rh = this.rowH();
      float fs = this.pFont();
      boolean exp = this.expanded.contains(key);
      if (y + rh > this.clipY && y < this.clipY + this.clipH) {
         boolean hv = this.pin(p, x, y, W, rh);
         float hT = this.an.to("phv:" + key, hv ? 1.0F : 0.0F, 18.0F);
         if (this.gbool("Row Dividers", true)) {
            c.line(x + 4.0F, y + rh, x + W - 4.0F, y + rh, 1.0F, Theme.a(this.th.text, 0.045F));
         }

         if (hT > 0.01F) {
            c.rect(x, y, W, rh, 0.0F, Theme.a(this.th.text, 0.06F * hT));
         }

         c.text("inter-med", fs, x + 7.0F + 1.5F * hT, y + rh / 2.0F, 17, exp ? this.th.accent : Theme.a(this.th.text, 0.78F + 0.22F * hT), entry);
         float right = x + W - 6.0F;

         for (int k = 0; k < 3; k++) {
            c.circle(right - 1.0F - k * 3.2F, y + rh / 2.0F, 0.95F, Theme.a(exp ? this.th.accent : this.th.muted, exp ? 1.0F : 0.5F + 0.4F * hT));
         }

         if ("Themes".equals(entry)) {
            c.text("inter", fs - 1.5F, right - 12.0F, y + rh / 2.0F, 20, this.th.muted, this.ellipsize(c, "inter", fs - 1.5F, this.store.themeName() == null ? "" : this.store.themeName(), W - 70.0F));
         }

         this.hit(x, y, W, rh, () -> this.toggleExpand(key), () -> this.toggleExpand(key));
      }

      y += rh;
      float expT = this.an.to("pexp:" + key, exp ? 1.0F : 0.0F, 12.0F);
      if (expT > 0.002F) {
         float setH = this.measured.getOrDefault(key, 0.0F);
         float vis = setH * Anim.ease(expT);
         c.save();
         c.scissor(x, y, W, Math.max(0.0F, vis));
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH = this.clipH;
         this.intersectClip(x, y, W, vis);
         c.rect(x, y, W, vis, 0.0F, Theme.a(-16777216, 0.28F));
         c.rect(x, y, 1.5F, vis, 0.0F, Theme.a(this.th.accent, 0.85F));
         float ix = x + 6.0F;
         float iw = W - 10.0F;
         float end = y + 2.0F;
         String var21 = entry;
         byte var22 = -1;
         switch (var21.hashCode()) {
            case -1790955542:
               if (var21.equals("Themes")) {
                  var22 = 0;
               }
               break;
            case -937619446:
               if (var21.equals("Profiles")) {
                  var22 = 1;
               }
         }

         label115: {
            List<GuiData.Setting> list;
            switch (var22) {
               case 0:
                  end = this.drawPanelThemes(c, p, ix, end, iw);
                  break label115;
               case 1:
                  end = this.drawPanelProfiles(c, p, ix, end, iw);
                  break label115;
               default:
                  list = new ArrayList<>();
            }

            for (GuiData.Setting st : GUI_SECTIONS.getOrDefault(entry, List.of())) {
               if (!WINDOW_ONLY.contains(st.name)) {
                  list.add(st);
               }
            }

            end = this.drawPanelSettings(c, p, "Click GUI", list, ix, end, iw);
            if ("General".equals(entry)) {
               boolean web = this.store.webMode();
               end = this.pAction(c, p, "pOpenWeb", web ? "Open the web GUI" : "Switch to the web GUI", ix, end, iw, web ? this.store::openWebGui : () -> {
                  this.store.switchToWebGui();
                  this.close();
               });
            }

            if ("Panels".equals(entry)) {
               end = this.pAction(c, p, "pResetLayout", "Reset layout", ix, end, iw, this::resetPanels);
            }
         }

         this.measured.put(key, end - y + 3.0F);
         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH;
         c.restore();
         y += vis;
      }

      return y;
   }

   private float pAction(Canvas c, ClickGui.Panel p, String id, String label, float x, float y, float w, Runnable r) {
      if (this.tabsMode()) {
         return this.tButton(c, "pa:" + id, label, x, y + 1.0F, w, r) - 1.0F;
      }

      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      c.rect(x - 2.0F, y + 1.5F, w + 4.0F, h - 3.0F, 2.0F, Theme.a(this.th.accent, hv ? 0.32F : 0.16F));
      c.text("inter-med", fs, x + w / 2.0F, y + h / 2.0F, 18, hv ? this.th.text : Theme.a(this.th.text, 0.85F), this.ellipsize(c, "inter-med", fs, label, w));
      this.hit(x - 4.0F, y, w + 8.0F, h, r, null);
      return y + h + 1.0F;
   }

   private float drawPanelThemes(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      boolean tc = this.store.themeColorsEnabled();
      boolean hv0 = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      if (hv0) {
         c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
      }

      c.text("inter-med", fs, x, y + h / 2.0F, 17, Theme.a(this.th.text, 0.85F), this.ellipsize(c, "inter-med", fs, "Theme colours for visuals", w - 16.0F));
      float b = Math.min(9.0F, h - 6.0F);
      float bx = x + w - b;
      float by = y + (h - b) / 2.0F;
      c.rect(bx, by, b, b, 2.0F, tc ? this.th.accent : Theme.a(Theme.mix(this.th.bg, this.th.text, 0.12F), 0.95F));
      if (tc) {
         Icons.draw(c, "check", bx + b / 2.0F, by + b / 2.0F, b * 0.95F, Theme.onColor(this.th.accent));
      }

      this.hit(x - 4.0F, y, w + 8.0F, h, () -> this.store.setThemeColorsEnabled(!tc), null);
      y += h + 2.0F;
      String active = this.store.themeName();

      for (Entry<String, List<String>> e : this.data.themes.entrySet()) {
         String name = e.getKey();
         List<String> cols = e.getValue();
         boolean act = name.equals(active);
         boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
         if (hv) {
            c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
         }

         for (int k = 0; k < 3; k++) {
            int sc = 0xFF000000 | Theme.hex(cols.get(k == 2 ? 3 : k), 8947848);
            c.circle(x + 4.0F + k * 7, y + h / 2.0F, 3.0F, sc);
         }

         c.text(act ? "inter-sb" : "inter-med", fs, x + 24.0F, y + h / 2.0F, 17, act ? this.th.accent : Theme.a(this.th.text, hv ? 1.0F : 0.8F), this.ellipsize(c, "inter-med", fs, name, w - 38.0F));
         if (act) {
            Icons.draw(c, "check", x + w - 4.0F, y + h / 2.0F, 9.0F, this.th.accent);
         }

         this.hit(x - 4.0F, y, w + 8.0F, h, () -> this.changeTheme(name, cols), null);
         y += h;
      }

      return y;
   }

   private float drawPanelProfiles(Canvas c, ClickGui.Panel p, float x, float y, float w) {
      float h = this.setRowH();
      float fs = this.pFont() - 1.0F;
      String active = this.store.activeProfile();
      boolean editing = this.focused == this.nameField && this.nameEnter != null;
      boolean hv0 = this.pin(p, x - 4.0F, y, w + 8.0F, h);
      if (hv0 || editing) {
         c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(editing ? this.th.accent : this.th.text, editing ? 0.12F : 0.05F));
      }

      if (editing) {
         String tx = this.nameField.text;
         c.text("inter", fs, x, y + h / 2.0F, 17, tx.isEmpty() ? this.th.muted : this.th.text, tx.isEmpty() ? "Name, then Enter" : tx);
         if (!tx.isEmpty()) {
            this.caret(c, "inter", fs, x, y + h / 2.0F, this.nameField);
         }
      } else {
         c.text("inter-med", fs, x, y + h / 2.0F, 17, this.th.accent, "+ Save current as…");
      }

      this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
         this.nameField.set("");
         this.focus(this.nameField, null);
         this.nameEnter = () -> {
            String n = this.nameField.text.trim();
            if (n.isEmpty()) {
               this.toast("Type a name first");
            } else {
               this.store.saveProfile(n);
               this.toast("Saved profile " + n);
               this.nameField.set("");
               this.focused = null;
               this.nameEnter = null;
            }
         };
      }, null);
      y += h;
      List<String> profs = this.store.profiles();

      for (String name : profs) {
         boolean act = name.equals(active);
         String menuId = "profact:" + name;
         boolean open = this.pop != null && menuId.equals(this.pop.id);
         boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
         if (hv || open) {
            c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(open ? this.th.accent : this.th.text, open ? 0.12F : 0.05F));
         }

         if (act) {
            c.rect(x - 3.0F, y + 3.0F, 2.0F, h - 6.0F, 1.0F, this.th.accent);
         }

         c.text(act ? "inter-sb" : "inter-med", fs, x + 2.0F, y + h / 2.0F, 17, act ? this.th.accent : Theme.a(this.th.text, hv ? 1.0F : 0.8F), this.ellipsize(c, "inter-med", fs, name, w - 26.0F));

         for (int k = 0; k < 3; k++) {
            c.circle(x + w - 1.0F - k * 3.2F, y + h / 2.0F, 0.9F, Theme.a(this.th.muted, hv ? 0.9F : 0.5F));
         }

         if (CloudConfigs.linkOf(name) != null) {
            Icons.draw(c, "cloud", x + w - 16.0F, y + h / 2.0F, Math.min(11.0F, h - 4.0F), Theme.a(this.th.accent, 0.85F));
            if (this.pin(p, x + w - 22.0F, y, 12.0F, h)) {
               this.hoverTip("pcloudlink:" + name, "Synced to the cloud (" + CloudConfigs.linkOf(name) + ")");
            }
         }

         float fy = y;
         this.hit(x - 4.0F, y, w + 8.0F, h, () -> {
            this.store.loadProfile(name);
            this.toast("Loaded " + name);
         }, () -> {
            if (open) {
               this.pop = null;
            } else {
               this.openList(menuId, x - 4.0F, fy, w + 8.0F, h, 120.0F, List.of("Load", "Overwrite", "Export", "Upload to cloud", "Delete"), "", v -> this.profileAction(name, v));
            }
         });
         y += h;
      }

      if (profs.isEmpty()) {
         c.text("inter", fs - 0.5F, x, y + h / 2.0F, 17, this.th.muted, "No profiles yet");
         y += h;
      }

      y = this.pAction(c, p, "pLoadFile", "Load from file…", x, y + 2.0F, w, this::pickAndImport);
      y = this.pAction(c, p, "pOpenFolder", "Open configs folder", x, y, w, () -> this.openFolder(null));
      List<Path> files = this.store.configFiles();
      if (!files.isEmpty()) {
         c.text("inter-sb", fs - 1.0F, x, y + h / 2.0F + 1.0F, 17, this.th.muted, "FILES (click load · right click import)");
         y += h;

         for (Path f : files) {
            String fn = f.getFileName().toString();
            boolean hv = this.pin(p, x - 4.0F, y, w + 8.0F, h);
            if (hv) {
               c.rect(x - 4.0F, y, w + 8.0F, h, 2.0F, Theme.a(this.th.text, 0.05F));
            }

            c.text("inter", fs, x + 2.0F, y + h / 2.0F, 17, Theme.a(this.th.text, hv ? 1.0F : 0.75F), this.ellipsize(c, "inter", fs, fn, w - 4.0F));
            this.hit(x - 4.0F, y, w + 8.0F, h, () -> this.importFile(f, true), () -> this.importFile(f, false));
            y += h;
         }
      }

      return y;
   }

   private void profileAction(String name, String action) {
      switch (action) {
         case "Load":
            this.store.loadProfile(name);
            this.toast("Loaded " + name);
            break;
         case "Overwrite":
            if (!("ow:" + name).equals(this.confirmId) || this.time - this.confirmAt >= 2.5F) {
               this.toast("Pick Overwrite again to confirm");
            }

            this.confirm("ow:" + name, () -> {
               this.store.saveProfile(name);
               this.toast("Overwrote " + name);
            });
            break;
         case "Export":
            try {
               this.toast("Saved configs/" + this.store.exportProfile(name));
            } catch (Exception e) {
               this.toast("Export failed: " + e.getMessage());
            }
            break;
         case "Upload to cloud":
            this.startUpload(name);
            break;
         case "Delete":
            if (!("del:" + name).equals(this.confirmId) || this.time - this.confirmAt >= 2.5F) {
               this.toast("Pick Delete again to confirm");
            }

            this.confirm("del:" + name, () -> {
               this.store.deleteProfile(name);
               this.toast("Deleted " + name);
            });
      }
   }

   float idRowH() {
      return this.compact() ? Math.max(this.setRowH() + 3.0F, 17.0F) : 30.0F;
   }

   void openIds(String id, String module, GuiData.Setting s, float x, float y, float cw, float h) {
      ClickGui.Pop p = new ClickGui.Pop(id, false);
      p.ids = true;
      p.module = module;
      p.key = s.name;
      p.blocks = "blockpicker".equals(s.type);
      float rh = this.idRowH();
      if (this.compact()) {
         p.w = Math.max(cw, 190.0F);
         p.x = Math.max(this.wx + 4.0F, Math.min(x, this.wx + this.ww - p.w - 4.0F));
         p.h = 3.0F + this.setRowH() + 3.0F + 10.0F * rh + 3.0F;
         p.y = y + h + p.h < this.wy + this.wh - 4.0F ? y + h : Math.max(this.wy + 4.0F, y - p.h);
      } else {
         p.w = Math.max(cw, 290.0F);
         p.x = Math.max(this.wx + 6.0F, Math.min(x + cw - p.w, this.wx + this.ww - p.w - 6.0F));
         p.h = 42.0F + 8.0F * rh + 6.0F;
         float below = this.wy + this.wh - (y + h + 6.0F);
         p.y = !(below > p.h) && !(y - this.wy < p.h) ? y - 6.0F - p.h : y + h + 6.0F;
      }

      this.pop = p;
      this.focus(p.filter, null);
   }

   private List<GuiData.Entry> catalog(boolean blocks) {
      return blocks ? this.data.blocks : this.data.allItems;
   }

   static String bare(String id) {
      return id.startsWith("minecraft:") ? id.substring(10) : id;
   }

   private String displayId(String id) {
      if (this.idNames == null) {
         this.idNames = new HashMap<>();

         for (GuiData.Entry e : this.data.allItems) {
            this.idNames.putIfAbsent(e.id(), e.name());
         }
      }

      String b = bare(id);
      String n = this.idNames.get(b);
      if (n != null) {
         return n;
      }

      StringBuilder sb = new StringBuilder();

      for (String part : b.substring(b.indexOf(58) + 1).split("_")) {
         if (!part.isEmpty()) {
            if (sb.length() > 0) {
               sb.append(' ');
            }

            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
         }
      }

      return sb.toString();
   }

   Set<String> idSelection(String module, String key) {
      Set<String> out = new LinkedHashSet<>();

      for (String v : list(this.get(module, key), List.of())) {
         out.add(bare(v));
      }

      return out;
   }

   private void idSet(ClickGui.Pop p, Set<String> sel) {
      List<String> ordered = new ArrayList<>();
      Set<String> done = new HashSet<>();

      for (GuiData.Entry e : this.catalog(p.blocks)) {
         if (sel.contains(e.id()) && done.add(e.id())) {
            ordered.add(e.id());
         }
      }

      for (String v : sel) {
         if (done.add(v)) {
            ordered.add(v);
         }
      }

      this.set(p.module, p.key, ordered);
   }

   private void idToggle(ClickGui.Pop p, String id) {
      Set<String> sel = this.idSelection(p.module, p.key);
      if (!sel.remove(id)) {
         sel.add(id);
      }

      this.idSet(p, sel);
   }

   List<GuiData.Entry> idView(ClickGui.Pop p, Set<String> sel) {
      String q = p.filter.text.trim().toLowerCase(Locale.ROOT);
      if (q.equals(p.vQuery) && p.onlySel == p.vOnly) {
         return p.view;
      }

      String qid = q.replace(' ', '_');
      List<GuiData.Entry> first = new ArrayList<>();
      List<GuiData.Entry> rest = new ArrayList<>();
      Set<String> seen = new HashSet<>();

      for (GuiData.Entry e : this.catalog(p.blocks)) {
         if (seen.add(e.id()) && (q.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(q) || e.id().contains(qid))) {
            if (sel.contains(e.id())) {
               first.add(e);
            } else if (!p.onlySel) {
               rest.add(e);
            }
         }
      }

      for (String v : sel) {
         if (!seen.contains(v)) {
            String n = this.displayId(v);
            if (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q) || v.contains(qid)) {
               first.add(new GuiData.Entry(v, n, "Other"));
            }
         }
      }

      first.addAll(rest);
      p.vQuery = q;
      p.vOnly = p.onlySel;
      p.view = first;
      return first;
   }

   private void itemIcon(Canvas c, String id, float x, float y, float s) {
      if (!c.itemIcon(id, x, y, s, this.clipX, this.clipY, this.clipW, this.clipH)) {
         int hue = Math.floorMod(id.hashCode(), 360);
         int col = Theme.hsvToRgb(hue / 360.0F, 0.42F, 0.72F);
         c.rect(x + s * 0.08F, y + s * 0.08F, s * 0.84F, s * 0.84F, s * 0.22F, Theme.a(0xFF000000 | col, 0.85F));
         String l = this.displayId(id);
         c.text("inter-sb", s * 0.52F, x + s / 2.0F, y + s / 2.0F, 18, -1, l.isEmpty() ? "?" : l.substring(0, 1));
      }
   }

   private void drawIdsPop(Canvas c, ClickGui.Pop p) {
      boolean pm = this.compact();
      float fs = pm ? this.pFont() - 1.0F : 12.0F;
      float pad = pm ? 3.0F : 8.0F;
      float sH = pm ? this.setRowH() : 28.0F;
      float rh = this.idRowH();
      float R = pm ? 2.0F : 8.0F;
      Set<String> sel = this.idSelection(p.module, p.key);
      String cnt = String.valueOf(sel.size());
      float chipW = c.textWidth("inter-sb", fs - 0.5F, cnt) + (pm ? 18.0F : 26.0F);
      float fx = p.x + pad;
      float fy = p.y + pad;
      float fw = p.w - pad * 2.0F - chipW - (pm ? 3.0F : 6.0F);
      boolean f = this.focused == p.filter;
      c.rect(fx, fy, fw, sH, R, Theme.a(pm ? this.th.text : this.th.bg, pm ? (f ? 0.08F : 0.05F) : 0.6F));
      if (!pm && f) {
         c.stroke(fx, fy, fw, sH, R, 1.0F, Theme.a(this.th.accent, 0.7F));
      }

      Icons.draw(c, "search", fx + (pm ? 8.0F : 13.0F), fy + sH / 2.0F, pm ? Math.min(10.0F, sH - 5.0F) : 11.0F, f ? this.th.accent : this.th.muted);
      float tx = fx + (pm ? 17.0F : 25.0F);
      String ft = p.filter.text;
      c.text(
         "inter",
         fs,
         tx,
         fy + sH / 2.0F,
         17,
         ft.isEmpty() ? this.th.muted : this.th.text,
         ft.isEmpty() ? (p.blocks ? "Search blocks…" : "Search items…") : this.ellipsize(c, "inter", fs, ft, fw - (tx - fx) - 6.0F)
      );
      if (f) {
         this.caret(c, "inter", fs, tx, fy + sH / 2.0F, p.filter);
      }

      this.hit(fx, fy, fw, sH, () -> this.focus(p.filter, null), () -> p.filter.set(""));
      float cx = fx + fw + (pm ? 3.0F : 6.0F);
      boolean chv = this.in(cx, fy, chipW, sH);
      c.rect(cx, fy, chipW, sH, R, p.onlySel ? this.th.accent : Theme.a(this.th.accent, chv ? 0.24F : 0.14F));
      int cc = p.onlySel ? Theme.onColor(this.th.accent) : this.th.accent;
      float ck = pm ? 8.0F : 10.0F;
      Icons.draw(c, "check", cx + (pm ? 7.0F : 10.0F), fy + sH / 2.0F, ck, cc);
      c.text("inter-sb", fs - 0.5F, cx + chipW - (pm ? 4.0F : 7.0F), fy + sH / 2.0F, 20, cc, cnt);
      this.hit(cx, fy, chipW, sH, () -> {
         p.onlySel = !p.onlySel;
         p.scrollT = 0.0F;
         p.scrollY = 0.0F;
      }, null);
      List<GuiData.Entry> view = this.idView(p, sel);
      float top = fy + sH + (pm ? 3.0F : 6.0F);
      float viewH = p.y + p.h - (pm ? 3.0F : 6.0F) - top;
      float maxScroll = Math.max(0.0F, view.size() * rh - viewH);
      p.scrollT = Math.max(0.0F, Math.min(maxScroll, p.scrollT));
      p.scrollY = p.scrollY + (p.scrollT - p.scrollY) * 0.35F;
      boolean iconsReady = (this.time - p.openedAt) * Math.max(0.05F, this.an_speed()) > 0.2F;
      c.save();
      c.scissor(p.x, top, p.w, viewH);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sHc = this.clipH;
      this.clip(p.x, top, p.w, viewH);
      int i0 = Math.max(0, (int)(p.scrollY / rh));
      int i1 = Math.min(view.size(), (int)((p.scrollY + viewH) / rh) + 1);
      float is = pm ? Math.min(rh - 4.0F, 14.0F) : 18.0F;
      float b = pm ? Math.min(9.0F, rh - 7.0F) : 14.0F;

      for (int i = i0; i < i1; i++) {
         GuiData.Entry e = view.get(i);
         float ey = top + i * rh - p.scrollY;
         boolean on = sel.contains(e.id());
         boolean hv = this.in(p.x + 1.0F, ey, p.w - 2.0F, rh);
         float rx = p.x + (pm ? 1.0F : 5.0F);
         float rw = p.w - (pm ? 2.0F : 10.0F);
         if (on) {
            c.rect(rx, ey + (pm ? 0.0F : 1.0F), rw, rh - (pm ? 0.0F : 2.0F), pm ? 0.0F : 7.0F, Theme.a(this.th.accent, hv ? 0.2F : 0.12F));
         } else if (hv) {
            c.rect(rx, ey + (pm ? 0.0F : 1.0F), rw, rh - (pm ? 0.0F : 2.0F), pm ? 0.0F : 7.0F, Theme.a(this.th.text, 0.06F));
         }

         if (pm && on) {
            c.rect(p.x + 2.0F, ey + 3.0F, 2.0F, rh - 6.0F, 1.0F, this.th.accent);
         }

         float ix = p.x + (pm ? 7.0F : 13.0F);
         if (iconsReady) {
            this.itemIcon(c, e.id(), ix, ey + (rh - is) / 2.0F, is);
         }

         float nx = ix + is + (pm ? 5.0F : 9.0F);
         float bx = p.x + p.w - (pm ? 6.0F : 14.0F) - b;
         c.text(on ? "inter-sb" : "inter-med", fs, nx, ey + rh / 2.0F, 17, on ? this.th.text : Theme.a(this.th.text, hv ? 1.0F : 0.8F), this.ellipsize(c, "inter-med", fs, e.name(), bx - nx - 6.0F));
         float by = ey + (rh - b) / 2.0F;
         c.rect(bx, by, b, b, pm ? 2.0F : 4.0F, on ? this.th.accent : Theme.a(Theme.mix(this.th.bg, this.th.text, 0.14F), 0.95F));
         if (on) {
            Icons.draw(c, "check", bx + b / 2.0F, by + b / 2.0F, b * 0.9F, Theme.onColor(this.th.accent));
         }

         String fid = e.id();
         this.hit(p.x, ey, p.w, rh, () -> this.idToggle(p, fid), null);
      }

      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sHc;
      c.restore();
      if (maxScroll > 0.0F) {
         float barH = Math.max(pm ? 12.0F : 20.0F, viewH * viewH / (viewH + maxScroll));
         c.rect(p.x + p.w - (pm ? 3.0F : 5.0F), top + (viewH - barH) * (p.scrollY / maxScroll), pm ? 2.0F : 3.0F, barH, 1.5F, pm ? Theme.a(this.th.accent, 0.6F) : Theme.a(this.th.text, 0.25F));
      }

      if (view.isEmpty()) {
         c.text("inter", fs, p.x + p.w / 2.0F, top + rh, 18, this.th.muted, p.onlySel && ft.isEmpty() ? "Nothing picked yet" : "No matches");
      }
   }

   private void changeTheme(String name, List<String> cols) {
      if (!name.equals(this.store.themeName())) {
         this.themeStyle = this.gstr("Theme Transition", "Fade");
         if ("None".equals(this.themeStyle)) {
            this.store.applyTheme(name, cols);
            this.toast(name + " applied");
         } else if ("Fade".equals(this.themeStyle)) {
            this.oldTheme = this.th;
            this.store.applyTheme(name, cols);
            this.themeAt = this.time;
            this.toast(name + " applied");
         } else {
            this.pendingThemeName = name;
            this.pendingThemeCols = cols;
            this.themeOx = this.mx;
            this.themeOy = this.my;
            this.snapRequest = 2;
         }
      }
   }

   float themeP() {
      return this.themeAt < -50.0F ? 1.0F : Math.max(0.0F, Math.min(1.0F, (this.time - this.themeAt) * this.an_speed() / 0.95F));
   }

   private void drawThemeTransition(Canvas c) {
      if (this.oldTheme != null && !(this.themeAt < -50.0F) && this.haveSnapshot && !"Fade".equals(this.themeStyle)) {
         float p = this.themeP();
         if (!(p >= 1.0F)) {
            float e = p < 0.5F ? 4.0F * p * p * p : 1.0F - (float)Math.pow(-2.0F * p + 2.0F, 3.0) / 2.0F;
            switch (this.themeStyle) {
               case "Wipe": {
                  float slope = 0.35F;
                  float L = -this.sh * slope - 40.0F + e * (this.sw + this.sh * slope + 80.0F);
                  c.snapPiece(new float[]{L, 0.0F, this.sw, 0.0F, this.sw, this.sh, L + this.sh * slope, this.sh}, this.sw, this.sh, 1.0F);
                  float fade = (float)Math.sin(Math.PI * p);
                  c.line(L, 0.0F, L + this.sh * slope, this.sh, 22.0F, Theme.a(this.th.accent, 0.18F * fade));
                  c.line(L, 0.0F, L + this.sh * slope, this.sh, 3.0F, Theme.a(Theme.mix(this.th.accent, -1, 0.4F), 0.9F * fade));
                  break;
               }
               case "Fade":
                  c.snapPiece(new float[]{0.0F, 0.0F, this.sw, 0.0F, this.sw, this.sh, 0.0F, this.sh}, this.sw, this.sh, 1.0F - Anim.ease(p));
                  break;
               default: {
                  float ox = this.themeOx;
                  float oy = this.themeOy;
                  float maxR = 0.0F;

                  for (float[] k : new float[][]{{0.0F, 0.0F}, {this.sw, 0.0F}, {0.0F, this.sh}, {this.sw, this.sh}}) {
                     maxR = Math.max(maxR, (float)Math.hypot(k[0] - ox, k[1] - oy));
                  }

                  float r = Math.max(0.5F, e * maxR);
                  c.snapHole(ox, oy, r, this.sw, this.sh, 1.0F);
                  float fade = 1.0F - Anim.ease(p);
                  c.ring(ox, oy, r, 26.0F, Theme.a(this.th.accent, 0.1F * fade));
                  c.ring(ox, oy, r, 2.5F, Theme.a(Theme.mix(this.th.accent, -1, 0.35F), 0.95F * fade));
                  c.ring(ox, oy, Math.max(0.5F, r * 0.9F), 1.2F, Theme.a(this.th.accent2, 0.5F * fade));

                  for (int k = 0; k < 36; k++) {
                     double a = k * (Math.PI / 18) + hashf(k) * 0.3;
                     float rr = r + (hashf(k + 50) - 0.5F) * 18.0F;
                     float sz = 1.2F + 2.0F * hashf(k + 99);
                     c.circle(ox + (float)Math.cos(a) * rr, oy + (float)Math.sin(a) * rr, sz, Theme.a(k % 2 == 0 ? this.th.accent : this.th.accent2, fade * (0.5F + 0.5F * hashf(k + 7))));
                  }

                  if (p < 0.25F) {
                     c.glow(ox, oy, 30.0F + 120.0F * p, Theme.a(this.th.accent, 0.6F * (1.0F - p / 0.25F)));
                  }
               }
            }
         }
      }
   }

   private static float hashf(int k) {
      double x = Math.sin(k * 127.1 + 311.7) * 43758.5453;
      return (float)(x - Math.floor(x));
   }

   private float closeDur() {
      return switch (this.closeStyle) {
         case "Shatter" -> 1.0F;
         case "Crumble" -> 1.15F;
         case "Slice" -> 0.7F;
         case "Implode" -> 0.8F;
         case "CRT" -> 0.62F;
         case "Panel" -> 0.42F;
         default -> 0.28F;
      };
   }

   float closeT() {
      return this.closeStart < 0.0F ? 0.0F : Math.max(0.0F, (this.time - this.closeStart) * this.an_speed() / this.closeDur());
   }

   private void buildShards() {
      this.layoutWindow(1.0F);
      Random rnd = new Random((long)(this.time * 1000.0F) ^ 24301L);
      List<float[]> out = new ArrayList<>();
      switch (this.closeStyle) {
         case "Shatter":
         case "Crumble":
         case "Implode":
            int cols = "Shatter".equals(this.closeStyle) ? 8 : ("Crumble".equals(this.closeStyle) ? 11 : 8);
            int rows = "Shatter".equals(this.closeStyle) ? 5 : ("Crumble".equals(this.closeStyle) ? 7 : 5);
            float jit = "Crumble".equals(this.closeStyle) ? 0.22F : 0.38F;
            float[][][] P = new float[cols + 1][rows + 1][];

            for (int i = 0; i <= cols; i++) {
               for (int j = 0; j <= rows; j++) {
                  float jx = i > 0 && i < cols ? (rnd.nextFloat() - 0.5F) * jit : 0.0F;
                  float jy = j > 0 && j < rows ? (rnd.nextFloat() - 0.5F) * jit : 0.0F;
                  P[i][j] = new float[]{this.wx + this.ww * (i + jx) / cols, this.wy + this.wh * (j + jy) / rows};
               }
            }

            for (int i = 0; i < cols; i++) {
               for (int j = 0; j < rows; j++) {
                  float[] a = P[i][j];
                  float[] b = P[i + 1][j];
                  float[] cc = P[i + 1][j + 1];
                  float[] d = P[i][j + 1];
                  if ("Shatter".equals(this.closeStyle)) {
                     if (rnd.nextBoolean()) {
                        out.add(piece(rnd, j, rows, a, b, cc));
                        out.add(piece(rnd, j, rows, a, cc, d));
                     } else {
                        out.add(piece(rnd, j, rows, a, b, d));
                        out.add(piece(rnd, j, rows, b, cc, d));
                     }
                  } else {
                     out.add(piece(rnd, j, rows, a, b, cc, d));
                  }
               }
            }
            break;
         case "Slice":
            int n = 14;

            for (int j = 0; j < n; j++) {
               float y0 = this.wy + this.wh * j / n;
               float y1 = this.wy + this.wh * (j + 1) / n;
               out.add(piece(rnd, j, n, new float[]{this.wx - 60.0F, y0}, new float[]{this.wx + this.ww + 60.0F, y0}, new float[]{this.wx + this.ww + 60.0F, y1}, new float[]{this.wx - 60.0F, y1}));
            }
            break;
         default:
            float m = 70.0F;
            out.add(
               piece(
                  rnd,
                  0,
                  1,
                  new float[]{this.wx - m, this.wy - m},
                  new float[]{this.wx + this.ww + m, this.wy - m},
                  new float[]{this.wx + this.ww + m, this.wy + this.wh + m},
                  new float[]{this.wx - m, this.wy + this.wh + m}
               )
            );
      }

      this.shards = out.toArray(new float[0][]);
   }

   private static float[] piece(Random rnd, int row, int rows, float[]... pts) {
      int n = pts.length;
      float[] f = new float[1 + 2 * n + 6];
      f[0] = n;
      float cx = 0.0F;
      float cy = 0.0F;

      for (int k = 0; k < n; k++) {
         f[1 + 2 * k] = pts[k][0];
         f[2 + 2 * k] = pts[k][1];
         cx += pts[k][0];
         cy += pts[k][1];
      }

      int o = 1 + 2 * n;
      f[o] = cx / n;
      f[o + 1] = cy / n;
      f[o + 2] = rnd.nextFloat();
      f[o + 3] = rnd.nextFloat();
      f[o + 4] = rnd.nextFloat();
      f[o + 5] = rows <= 1 ? 0.0F : (float)row / (rows - 1);
      return f;
   }

   private static float[] poly(float[] f) {
      int n = (int)f[0];
      float[] p = new float[2 * n];
      System.arraycopy(f, 1, p, 0, 2 * n);
      return p;
   }

   private void drawClose(Canvas c, float T) {
      float dur = this.closeDur();
      float sec = T * dur;
      this.layoutWindow(1.0F);
      if (this.shards != null && this.haveSnapshot && c.hasSnapshot()) {
         float fadeEnd = 1.0F - Anim.ease((T - 0.6F) / 0.4F);
         switch (this.closeStyle) {
            case "Shatter":
               float ix = this.mx > this.wx && this.mx < this.wx + this.ww && this.my > this.wy && this.my < this.wy + this.wh ? this.mx : this.winCx;
               float iy = this.mx > this.wx && this.mx < this.wx + this.ww && this.my > this.wy && this.my < this.wy + this.wh ? this.my : this.winCy;
               float maxD = (float)Math.hypot(this.ww, this.wh);
               if (!this.panelMode()) {
                  c.shadow(this.wx, this.wy + 18.0F, this.ww, this.wh, this.winR(), 60.0F, Theme.a(-16777216, 0.5F * (1.0F - Anim.ease(T * 3.0F))));
               }

               for (float[] f : this.shards) {
                  int o = 1 + 2 * (int)f[0];
                  float cx = f[o];
                  float cy = f[o + 1];
                  float r1 = f[o + 2];
                  float r2 = f[o + 3];
                  float r3 = f[o + 4];
                  float dx = cx - ix;
                  float dy = cy - iy;
                  float dist = (float)Math.hypot(dx, dy);
                  if (dist < 0.001F) {
                     dx = r1 - 0.5F;
                     dy = r2 - 0.5F;
                     dist = (float)Math.hypot(dx, dy) + 0.001F;
                  }

                  float dn = dist / maxD;
                  float tt = Math.max(0.0F, sec - 0.07F - 0.16F * dn);
                  float speed = (220.0F + 380.0F * r1) * (1.25F - dn);
                  float ox = dx / dist * speed * tt;
                  float oy = dy / dist * speed * tt + 850.0F * tt * tt;
                  float rot = (r3 - 0.5F) * 8.0F * tt;
                  float sc = 1.0F + (r2 - 0.4F) * 0.35F * Math.min(1.0F, tt * 2.0F);
                  c.save();
                  c.alpha(fadeEnd);
                  c.translate(cx + ox, cy + oy);
                  c.rotate(rot);
                  c.scale(sc);
                  c.translate(-cx, -cy);
                  c.snapPiece(poly(f), this.sw, this.sh, 1.0F);
                  if (sec < 0.35F) {
                     c.strokePoly(poly(f), 1.2F, Theme.a(Theme.mix(this.th.accent, -1, 0.6F), 0.9F * (1.0F - sec / 0.35F) * Math.min(1.0F, sec * 30.0F)));
                  }

                  c.restore();
               }

               if (sec < 0.4F) {
                  c.glow(ix, iy, 40.0F + 500.0F * sec, Theme.a(Theme.mix(this.th.accent, -1, 0.5F), 0.55F * (1.0F - sec / 0.4F)));
               }
               break;
            case "Crumble":
               for (float[] f : this.shards) {
                  int o = 1 + 2 * (int)f[0];
                  float cx = f[o];
                  float cy = f[o + 1];
                  float r1 = f[o + 2];
                  float r2 = f[o + 3];
                  float r3 = f[o + 4];
                  float row = f[o + 5];
                  float delay = (1.0F - row) * 0.42F + r1 * 0.14F;
                  float tt = Math.max(0.0F, sec - delay);
                  float shake = tt == 0.0F && sec > delay - 0.12F ? (float)Math.sin(this.time * 70.0F + r2 * 20.0F) * 1.6F : 0.0F;
                  float ox = (r2 - 0.5F) * 70.0F * tt + shake;
                  float oy = 1200.0F * tt * tt;
                  float rot = (r3 - 0.5F) * 3.2F * tt;
                  c.save();
                  c.alpha(Math.min(fadeEnd, 1.0F - Anim.ease((tt - 0.35F) / 0.35F)));
                  c.translate(cx + ox, cy + oy);
                  c.rotate(rot);
                  c.translate(-cx, -cy);
                  c.snapPiece(poly(f), this.sw, this.sh, 1.0F);
                  c.restore();
               }
               break;
            case "Slice":
               int i = 0;

               for (float[] f : this.shards) {
                  int o = 1 + 2 * (int)f[0];
                  float dir = i++ % 2 == 0 ? -1.0F : 1.0F;
                  float tt = Math.max(0.0F, sec - f[o + 5] * 0.16F);
                  float ox = dir * (2600.0F * tt * tt + 40.0F * tt);
                  c.save();
                  c.alpha(1.0F - Anim.ease(T * 1.1F - 0.1F));
                  c.translate(ox, 0.0F);
                  c.snapPiece(poly(f), this.sw, this.sh, 1.0F);
                  c.restore();
               }

               if (T < 0.3F) {
                  for (int k = 1; k < 14; k++) {
                     float y = this.wy + this.wh * k / 14.0F;
                     c.line(this.wx, y, this.wx + this.ww, y, 1.2F, Theme.a(this.th.accent, 0.7F * (1.0F - T / 0.3F)));
                  }
               }
               break;
            case "Implode":
               float glowR = 0.0F;

               for (float[] f : this.shards) {
                  int o = 1 + 2 * (int)f[0];
                  float cx = f[o];
                  float cy = f[o + 1];
                  float r1 = f[o + 2];
                  float r3 = f[o + 4];
                  float k = Anim.ease(Math.min(1.0F, Math.max(0.0F, (T - r1 * 0.15F) / 0.7F)));
                  float kk = k * k;
                  float px = cx + (this.winCx - cx) * kk;
                  float py = cy + (this.winCy - cy) * kk;
                  float rot = (2.5F + 3.0F * r3) * kk * (r1 < 0.5F ? -1 : 1);
                  c.save();
                  c.alpha(1.0F - Anim.ease((k - 0.75F) / 0.25F));
                  c.translate(px, py);
                  c.rotate(rot);
                  c.scale(Math.max(0.01F, 1.0F - k));
                  c.translate(-cx, -cy);
                  c.snapPiece(poly(f), this.sw, this.sh, 1.0F);
                  c.restore();
                  glowR += k;
               }

               glowR /= Math.max(1, this.shards.length);
               float pop = Math.max(0.0F, (T - 0.82F) / 0.18F);
               c.glow(this.winCx, this.winCy, 20.0F + 70.0F * glowR + 260.0F * pop, Theme.a(Theme.mix(this.th.accent, -1, 0.3F), 0.7F * glowR * (1.0F - pop)));
               if (pop > 0.0F) {
                  c.ring(this.winCx, this.winCy, 10.0F + 300.0F * pop, 2.0F, Theme.a(this.th.accent, 0.8F * (1.0F - pop)));
               }
               break;
            case "CRT":
               float[] f = this.shards[0];
               float p1 = Anim.ease(Math.min(1.0F, T / 0.5F));
               float p2 = Anim.ease(Math.max(0.0F, (T - 0.5F) / 0.32F));
               float sy = Math.max(0.006F, 1.0F - p1);
               float sx = Math.max(0.004F, (1.0F + 0.06F * p1) * (1.0F - p2));
               c.save();
               c.translate(this.winCx, this.winCy);
               c.scale2(sx, sy);
               c.translate(-this.winCx, -this.winCy);
               if (T < 0.82F) {
                  c.snapPiece(poly(f), this.sw, this.sh, 1.0F);
                  if (!this.panelMode()) {
                     c.rect(this.wx, this.wy, this.ww, this.wh, this.winR(), Theme.a(-1, 0.9F * p1));
                  }
               }

               c.restore();
               if (T > 0.4F && T < 0.85F) {
                  c.line(this.winCx - this.ww / 2.0F * sx, this.winCy, this.winCx + this.ww / 2.0F * sx, this.winCy, 3.0F, Theme.a(-1, 0.9F));
               }

               float dot = Math.max(0.0F, (T - 0.7F) / 0.3F);
               if (T > 0.6F) {
                  c.glow(this.winCx, this.winCy, 26.0F * (1.0F - dot) + 6.0F, Theme.a(-1, 0.9F * (1.0F - dot)));
               }
               break;
            default:
               c.save();
               c.alpha(1.0F - Anim.ease(T));
               c.translate(this.winCx, this.winCy);
               c.scale(1.0F - 0.06F * Anim.ease(T));
               c.translate(-this.winCx, -this.winCy);
               c.snapPiece(poly(this.shards[0]), this.sw, this.sh, 1.0F);
               c.restore();
         }
      } else {
         c.save();
         c.alpha(1.0F - Anim.ease(T));
         c.translate(this.winCx, this.winCy);
         c.scale(1.0F - 0.06F * Anim.ease(T));
         c.translate(-this.winCx, -this.winCy);
         this.drawMain(c);
         c.restore();
      }
   }

   private float nameBar(Canvas c, float x, float y, float w, String placeholder, String action, Consumer<String> onSave) {
      float fw = Math.min(320.0F, w - 200.0F);
      boolean editing = this.focused == this.nameField;
      float ft = this.an.to("nameField", editing ? 1.0F : 0.0F, 14.0F);
      c.rect(x, y, fw, 34.0F, this.ctlR() + 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.05F), 0.85F));
      c.stroke(x, y, fw, 34.0F, this.ctlR() + 1.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, ft));
      String t = this.nameField.text;
      c.text("inter", 12.5F, x + 13.0F, y + 17.0F, 17, t.isEmpty() && !editing ? this.th.muted : this.th.text, t.isEmpty() && !editing ? placeholder : t);
      if (editing) {
         this.caret(c, "inter", 12.5F, x + 13.0F, y + 17.0F, this.nameField);
      }

      Runnable save = () -> {
         String n = this.nameField.text.trim();
         if (n.isEmpty()) {
            this.toast("Type a name first");
         } else {
            onSave.accept(n);
            this.nameField.set("");
            this.focused = null;
         }
      };
      this.hit(x, y, fw, 34.0F, () -> this.focus(this.nameField, null), null);
      float bw = c.textWidth("inter-sb", 12.0F, action) + 32.0F;
      this.button(c, "namebar:" + action, x + fw + 10.0F, y + 2.0F, bw, 30.0F, action, true, save);
      if (editing) {
         this.focusCommit = null;
      }

      return y + 50.0F;
   }

   private void button(Canvas c, String id, float x, float y, float w, float h, String label, boolean primary, Runnable onClick) {
      float hv = this.an.to("btn:" + id, this.in(x, y, w, h) ? 1.0F : 0.0F, 16.0F);
      if (primary) {
         c.shadow(x, y + 2.0F, w, h, this.ctlR() + 1.0F, 12.0F + 6.0F * hv, Theme.a(this.th.accent, 0.35F + 0.2F * hv));
         c.gradient(x, y, w, h, this.ctlR() + 1.0F, Theme.mix(this.th.accent, -1, 0.08F * hv), this.th.accent2, true);
         c.text("inter-sb", 12.0F, x + w / 2.0F, y + h / 2.0F, 18, Theme.onColor(this.th.accent), label);
      } else {
         c.rect(x, y, w, h, this.ctlR() + 1.0F, Theme.a(Theme.mix(this.th.bg, this.th.text, 0.06F + 0.05F * hv), 0.85F));
         c.stroke(x, y, w, h, this.ctlR() + 1.0F, 1.0F, Theme.mix(Theme.a(this.th.border, 0.9F), this.th.accent, hv));
         c.text("inter-med", 12.0F, x + w / 2.0F, y + h / 2.0F, 18, this.th.text, label);
      }

      this.hit(x, y, w, h, onClick, null);
   }

   private float listRow(Canvas c, String id, float x, float y, float w, int i, float since, String title, String tag, String[] actions, ClickGui.IntAction act, boolean highlighted) {
      float h = 52.0F;
      float appear = Anim.easeOutCubic((since - 0.025F * Math.min(i, 14)) / 0.32F);
      float hv = this.an.to("lr:" + id, this.in(x, y, w, h) ? 1.0F : 0.0F, 14.0F);
      c.save();
      c.alpha(appear);
      c.translate((1.0F - appear) * 16.0F, 0.0F);
      if (highlighted) {
         c.shadow(x, y, w, h, this.gnum("Card Rounding", 14.0), 16.0F, Theme.a(this.th.accent, 0.2F));
      }

      c.rect(x, y, w, h, this.gnum("Card Rounding", 14.0), Theme.a(Theme.mix(this.th.bg, this.th.text, 0.035F + 0.025F * hv), 0.62F));
      if (highlighted) {
         c.gradient(x, y, w * 0.5F, h, this.gnum("Card Rounding", 14.0), Theme.a(this.th.accent, 0.16F), Theme.a(this.th.accent, 0.0F), true);
      }

      c.stroke(x, y, w, h, this.gnum("Card Rounding", 14.0), 1.0F, highlighted ? Theme.a(this.th.accent, 0.55F) : Theme.a(this.th.border, 0.55F + 0.2F * hv));
      c.text("inter-sb", 13.5F, x + 18.0F, y + h / 2.0F, 17, this.th.text, title);
      if (tag != null) {
         float tw = c.textWidth("inter-sb", 9.5F, tag) + 14.0F;
         float tx = x + 26.0F + c.textWidth("inter-sb", 13.5F, title);
         c.rect(tx, y + h / 2.0F - 9.0F, tw, 18.0F, 9.0F, Theme.a(this.th.accent, 0.2F));
         c.text("inter-sb", 9.5F, tx + tw / 2.0F, y + h / 2.0F, 18, this.th.accent, tag);
      }

      float bx = x + w - 14.0F;

      for (int k = actions.length - 1; k >= 0; k--) {
         String a = actions[k];
         boolean danger = a.equals("Delete") || a.equals("Remove");
         String key = danger
            ? (id.startsWith("srv:") ? "srvdel:" + id.substring(4) : (id.startsWith("bind:") ? "bdel:" + id.substring(5) : "del:" + id.substring(5)))
            : (a.equals("Overwrite") ? (id.startsWith("bind:") ? "bow:" + id.substring(5) : "ow:" + id.substring(5)) : "");
         boolean confirming = key.equals(this.confirmId) && this.time - this.confirmAt < 2.5F;
         String lbl = confirming ? "Sure?" : a;
         float bw = c.textWidth("inter-med", 11.5F, lbl) + 24.0F;
         bx -= bw;
         float bh = this.an.to("lrb:" + id + a, this.in(bx, y + h / 2.0F - 14.0F, bw, 28.0F) ? 1.0F : 0.0F, 16.0F);
         int base = danger ? -43920 : (k == 0 ? this.th.accent : this.th.text);
         c.rect(bx, y + h / 2.0F - 14.0F, bw, 28.0F, this.ctlR(), Theme.a(base, (confirming ? 0.3F : 0.08F) + 0.12F * bh));
         c.stroke(bx, y + h / 2.0F - 14.0F, bw, 28.0F, this.ctlR(), 1.0F, Theme.a(base, 0.25F + 0.4F * bh));
         c.text("inter-med", 11.5F, bx + bw / 2.0F, y + h / 2.0F, 18, !danger && k != 0 ? this.th.text : Theme.mix(base, this.th.text, 0.25F), lbl);
         int fk = k;
         this.hit(bx, y + h / 2.0F - 14.0F, bw, 28.0F, () -> act.run(fk), null);
         bx -= 8.0F;
      }

      c.restore();
      return y + h + 10.0F;
   }

   private float empty(Canvas c, float x, float y, float w, String msg) {
      c.text("inter-med", 12.5F, x + w / 2.0F, y + 40.0F, 18, this.th.muted, msg);
      return y + 90.0F;
   }

   private void confirm(String id, Runnable r) {
      if (id.equals(this.confirmId) && this.time - this.confirmAt < 2.5F) {
         this.confirmId = null;
         r.run();
      } else {
         this.confirmId = id;
         this.confirmAt = this.time;
      }
   }

   void toast(String s) {
      this.toasts.add(new ClickGui.Toast(s, this.time));
      if (this.toasts.size() > 4) {
         this.toasts.remove(0);
      }
   }

   private void drawToasts(Canvas c) {
      if (this.tabsMode()) {
         this.drawTabsToasts(c);
      } else {
         this.toasts.removeIf(tx -> this.time - tx.at > 2.6F);
         float y = this.sh - 40.0F;

         for (int i = this.toasts.size() - 1; i >= 0; i--) {
            ClickGui.Toast t = this.toasts.get(i);
            float age = this.time - t.at;
            float in = Anim.easeOutBack(Math.min(1.0F, age / 0.3F));
            float out = 1.0F - Anim.ease((age - 2.2F) / 0.4F);
            float a = Math.min(in, out);
            float tw = c.textWidth("inter-med", 12.5F, t.text) + 46.0F;
            float x = this.sw / 2.0F - tw / 2.0F;
            float yy = y + (1.0F - in) * 20.0F;
            c.save();
            c.alpha(Math.max(0.0F, a));
            c.shadow(x, yy, tw, 32.0F, 16.0F, 18.0F, Theme.a(-16777216, 0.5F));
            c.glass(x, yy, tw, 32.0F, 16.0F, 1.0F);
            c.rect(x, yy, tw, 32.0F, 16.0F, Theme.a(this.th.surface, 0.9F));
            c.stroke(x, yy, tw, 32.0F, 16.0F, 1.0F, Theme.a(this.th.accent, 0.5F));
            c.glow(x + 18.0F, yy + 16.0F, 12.0F, Theme.a(this.th.accent, 0.6F));
            c.circle(x + 18.0F, yy + 16.0F, 4.0F, this.th.accent);
            c.text("inter-med", 12.5F, x + 30.0F, yy + 16.0F, 17, this.th.text, t.text);
            c.restore();
            y -= 40.0F * a;
         }
      }
   }

   private void hoverTip(String key, String text) {
      if (!key.equals(this.tipKey)) {
         this.tipKey = key;
         this.hoverSince = this.time;
      }

      this.tipText = text;
   }

   private void hoverSinceReset(String key) {
      if (key.equals(this.tipKey)) {
         this.tipKey = null;
      }
   }

   private void drawTooltip(Canvas c) {
      if (this.tipKey != null && this.tipText != null && this.pop == null) {
         float age = this.time - this.hoverSince;
         String text = this.tipText;
         this.tipText = null;
         if (!(age < 0.55F)) {
            float a = Anim.ease((age - 0.55F) / 0.2F);
            if (this.tabsMode()) {
               this.tabsPalette();
               this.drawTabsTooltip(c, this.wrap(c, "inter", this.tfs(), text, 300.0F), a);
            } else {
               float maxW = 300.0F;
               List<String> lines = this.wrap(c, "inter", 11.5F, text, maxW);
               float w = 0.0F;

               for (String l : lines) {
                  w = Math.max(w, c.textWidth("inter", 11.5F, l));
               }

               w += 22.0F;
               float h = lines.size() * 16 + 14;
               float x = Math.min(this.mx + 14.0F, this.sw - w - 8.0F);
               float y = Math.min(this.my + 18.0F, this.sh - h - 8.0F);
               c.save();
               c.alpha(a);
               c.shadow(x, y, w, h, 9.0F, 14.0F, Theme.a(-16777216, 0.5F));
               c.rect(x, y, w, h, 9.0F, Theme.a(Theme.mix(this.th.surface, this.th.bg, 0.4F), 0.97F));
               c.stroke(x, y, w, h, 9.0F, 1.0F, Theme.a(this.th.border, 0.9F));

               for (int i = 0; i < lines.size(); i++) {
                  c.text("inter", 11.5F, x + 11.0F, y + 15.0F + i * 16, 17, this.th.text, lines.get(i));
               }

               c.restore();
            }
         }
      } else {
         this.tipKey = null;
      }
   }

   private void focus(TextField f, Runnable commit) {
      this.nameEnter = null;
      this.focused = f;
      this.focusCommit = commit;
      if (f == this.search || this.typingField(f) || this.pop != null && f == this.pop.filter && !this.pop.color) {
         this.focusId = null;
      }
   }

   private void applyListen(String name) {
      if (this.listenSetting != null) {
         this.set(this.listenModule, this.listenSetting, name);
      } else if (this.listenModule != null) {
         String mode = this.store.bindMode(this.listenModule);
         this.store.setBind(this.listenModule, "NONE".equals(name) ? null : name, mode == null ? "toggle" : mode);
         this.toast("NONE".equals(name) ? this.listenModule + " unbound" : this.listenModule + " → " + name);
      }

      this.listenModule = null;
      this.listenSetting = null;
   }

   private void caret(Canvas c, String font, float size, float x, float cy, TextField f) {
      if ((int)(this.time * 2.2F) % 2 != 1) {
         float cxp = x + c.textWidth(font, size, f.text.substring(0, Math.min(f.caret, f.text.length())));
         if (f.allSelected && !f.text.isEmpty()) {
            float tw = c.textWidth(font, size, f.text);
            c.rect(x - 1.0F, cy - size * 0.7F, tw + 2.0F, size * 1.4F, 3.0F, Theme.a(this.th.accent, 0.3F));
         } else {
            c.rect(cxp, cy - size * 0.62F, 1.4F, size * 1.24F, 0.7F, this.th.accent);
         }
      }
   }

   private void hit(float x, float y, float w, float h, Runnable click, Runnable right) {
      this.hit(x, y, w, h, click, right, null);
   }

   private void hit(float x, float y, float w, float h, Runnable click, Runnable right, Consumer<Float> wheel) {
      this.hits.add(new ClickGui.Hit(x, y, w, h, this.clipX, this.clipY, this.clipW, this.clipH, this.layer, click, right, wheel));
   }

   private ClickGui.Hit hitAt(float x, float y) {
      ClickGui.Hit best = null;

      for (ClickGui.Hit h : this.hits) {
         if (h.contains(x, y) && (best == null || h.layer >= best.layer)) {
            best = h;
         }
      }

      return best;
   }

   private void clip(float x, float y, float w, float h) {
      this.clipX = x;
      this.clipY = y;
      this.clipW = w;
      this.clipH = h;
   }

   private void intersectClip(float x, float y, float w, float h) {
      float nx = Math.max(this.clipX, x);
      float ny = Math.max(this.clipY, y);
      float nx2 = Math.min(this.clipX + this.clipW, x + w);
      float ny2 = Math.min(this.clipY + this.clipH, y + h);
      this.clip(nx, ny, Math.max(0.0F, nx2 - nx), Math.max(0.0F, ny2 - ny));
   }

   private void noClip() {
      this.clip(-1000000.0F, -1000000.0F, 2000000.0F, 2000000.0F);
   }

   private boolean in(float x, float y, float w, float h) {
      return this.layer == 0 && this.inPop(this.wmx, this.wmy)
         ? false
         : this.wmx >= x
            && this.wmy >= y
            && this.wmx <= x + w
            && this.wmy <= y + h
            && this.wmx >= this.clipX
            && this.wmy >= this.clipY
            && this.wmx <= this.clipX + this.clipW
            && this.wmy <= this.clipY + this.clipH;
   }

   private Object get(String module, String key) {
      if (FriendSettings.isFriendsModule(module)) {
         return this.friendGet(module, key);
      }

      Object v = "Click GUI".equals(module) ? this.store.gui(key) : this.store.value(module, key);
      return v == null && "Click GUI".equals(module) ? GUI_DEFAULTS.get(key) : v;
   }

   private void setSound(String module, String key, Object v) {
      try {
         if (v instanceof Boolean b) {
            UiSounds.toggle(b);
         } else if (v instanceof Number n && this.drag != null && !(this.get(module, key) instanceof Number o && !(Math.abs(o.doubleValue() - n.doubleValue()) > 1.0E-9))) {
            UiSounds.tick(this.sliderFrac);
         }
      } catch (Throwable var8) {
      }
   }

   private void set(String module, String key, Object v) {
      this.setSound(module, key, v);
      if (FriendSettings.isFriendsModule(module)) {
         this.friendSet(module, key, v);
      } else {
         if ("Click GUI".equals(module)) {
            this.store.setGui(key, v);
            if ("GUI".equals(key) && "Web".equals(v)) {
               this.toast("Right Shift now opens the web GUI");
            }

            if ("GUI".equals(key) && "ClickGUI".equals(v)) {
               this.toast("Web GUI locked while the ClickGUI is in use");
            }

            if ("GUI".equals(key) && "Panels".equals(v)) {
               this.pop = null;
               this.toast("Panels: left click toggles, right click opens settings");
            }

            if ("GUI".equals(key) && "Tabs".equals(v)) {
               this.pop = null;
               this.toast("Tabs: click a group title to fold it, right click for more");
            }
         } else {
            this.store.setValue(module, key, v);
         }
      }
   }

   private boolean visible(String module, GuiData.Setting s) {
      Map<String, Object> m = s.meta;
      if (m == null) {
         return true;
      }

      if (m.get("showifOr") instanceof List<?> l && l.size() >= 2 && l.get(1) instanceof List<?> vals) {
         String cur = String.valueOf(this.curOrDefault(module, String.valueOf(l.get(0))));

         for (Object v : vals) {
            if (cur.equals(jsStr(v))) {
               return true;
            }
         }

         return false;
      } else if (m.get("showif2") instanceof List<?> l && l.size() >= 4) {
         return match(this.curOrDefault(module, String.valueOf(l.get(0))), l.get(1)) && match(this.curOrDefault(module, String.valueOf(l.get(2))), l.get(3));
      } else {
         return m.get("showif") instanceof List<?> l && l.size() >= 2 ? match(this.curOrDefault(module, String.valueOf(l.get(0))), l.get(1)) : true;
      }
   }

   private Object curOrDefault(String module, String key) {
      Object v = this.get(module, key);
      if (v != null) {
         return v;
      }

      GuiData.Module mod = this.data.modules.get(module);
      List<GuiData.Setting> settings = mod != null ? mod.settings : ("Click GUI".equals(module) ? GUI_SETTINGS : List.of());

      for (GuiData.Setting s : settings) {
         if (s.name.equals(key)) {
            return s.defaultValue();
         }
      }

      return null;
   }

   private static boolean match(Object cur, Object expected) {
      if (expected instanceof Boolean b) {
         return cur instanceof Boolean cb ? cb == b : String.valueOf(b).equals(String.valueOf(cur));
      } else {
         return jsStr(expected).equals(jsStr(cur));
      }
   }

   private static String jsStr(Object o) {
      return o instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d) ? String.valueOf(d.longValue()) : String.valueOf(o);
   }

   private int resolveColor(String v) {
      if ("theme".equals(v)) {
         return this.th.accent & 16777215;
      } else {
         return "theme2".equals(v) ? this.th.accent2 & 16777215 : Theme.hex(v, 16777215);
      }
   }

   private static boolean isSoundSelect(String module, GuiData.Setting s) {
      return !"select".equals(s.type)
         ? false
         : "Hit Sounds".equals(module) && ("Sound".equals(s.name) || "Second Sound".equals(s.name))
            || "Kill Sounds".equals(module) && "Sound".equals(s.name)
            || "Notifications".equals(module) && ("Enabled Sound".equals(s.name) || "Disabled Sound".equals(s.name));
   }

   private List<String> soundOptions(GuiData.Setting s) {
      List<String> out = new ArrayList<>(s.options());

      try {
         out.addAll(CustomSounds.options());
      } catch (Throwable var4) {
      }

      out.add("Open sounds folder…");
      return out;
   }

   private void pickSound(String module, String key, String v) {
      if ("Open sounds folder…".equals(v)) {
         CustomSounds.openFolder();
      } else {
         this.set(module, key, v);
         if (CustomSounds.isCustom(v)) {
            CustomSounds.play(v, 0.8F, 1.0F);
         }
      }
   }

   private static boolean isModelFile(String module, GuiData.Setting s) {
      return "Player Model".equals(module) && "Custom Model".equals(s.name);
   }

   private List<String> modelFileOptions() {
      long now = System.currentTimeMillis();
      if (now - this.modelFilesAt > 2000L) {
         this.modelFilesAt = now;

         try {
            this.modelFilesCache = ImportedModelRenderer.files();
         } catch (Throwable t) {
            this.modelFilesCache = List.of();
         }
      }

      List<String> out = new ArrayList<>(this.modelFilesCache);
      out.add("Reload model");
      out.add("Open models folder…");
      return out;
   }

   private String modelFileValue(String module, GuiData.Setting s) {
      String v = str(this.get(module, s.name), "");
      return v.isEmpty() ? "No file selected" : v;
   }

   private void pickModelFile(String module, String key, String v) {
      if ("Open models folder…".equals(v)) {
         try {
            ConfigFiles.openFolder(ImportedModelRenderer.folder(), ok -> {});
         } catch (Throwable var5) {
         }
      } else if ("Reload model".equals(v)) {
         ImportedModelRenderer.reload();
         this.modelFilesAt = 0L;
      } else {
         this.set(module, key, v);
         if (!"Custom".equals(str(this.get(module, "Model"), "Kenny"))) {
            this.set(module, "Model", "Custom");
         }
      }
   }

   private void pickAsset(String module, String key, String v) {
      if (!"__add_skin__".equals(v)) {
         this.set(module, key, v);
      } else {
         boolean ok = ConfigFiles.pickPng("Pick a skin (64x64 PNG)", f -> {
            if (f != null) {
               if (f == ConfigFiles.NO_DIALOG) {
                  this.toast("No file picker here: drop PNGs into .minecraft/clarity/skins");
                  ConfigFiles.reveal(ConfigFiles.skinsDir());
               } else {
                  try {
                     String nid = this.store.addCustomSkin(f);
                     this.set(module, key, nid);
                     this.toast("Skin added");
                  } catch (Exception e) {
                     this.toast(e.getMessage() == null ? "Couldn't add that skin" : e.getMessage());
                  }
               }
            }
         });
         if (!ok) {
            this.toast("Drop PNG skins into .minecraft/clarity/skins");
            ConfigFiles.reveal(ConfigFiles.skinsDir());
         }
      }
   }

   static String display(String o) {
      if (o == null) {
         return "";
      }

      if (CustomSounds.isCustom(o)) {
         return CustomSounds.label(o);
      }

      if ("none".equals(o)) {
         return "None";
      }

      if ("default".equals(o)) {
         return "Default";
      }

      if ("__add_skin__".equals(o)) {
         return "+ Add skin from file…";
      }

      if (displayStore != null && (o.startsWith("custom_") || o.startsWith("file:"))) {
         try {
            String n = displayStore.assetName(o);
            if (n != null && !n.isBlank()) {
               return n;
            }
         } catch (Throwable var2) {
         }
      }

      if (o.startsWith("file:")) {
         return o.substring(5);
      } else {
         return o.startsWith("custom_") ? "Custom " + o.substring(7, Math.min(o.length(), 13)) : o;
      }
   }

   static double snap(double v, double step, double min) {
      if (step <= 0.0) {
         return v;
      }

      double s = Math.round((v - min) / step) * step + min;
      return Math.round(s * 1000000.0) / 1000000.0;
   }

   static String fmt(double v, double step) {
      if (!(step >= 1.0) && (!(step <= 0.0) || v != Math.rint(v))) {
         int dec = step <= 0.0 ? 2 : Math.max(0, Math.min(4, (int)Math.ceil(-Math.log10(step) - 1.0E-9)));
         return String.format(Locale.ROOT, "%." + dec + "f", v);
      } else {
         return String.valueOf(Math.round(v));
      }
   }

   static double num(Object o, double def) {
      if (o instanceof Number n) {
         return n.doubleValue();
      } else {
         if (o instanceof String s) {
            try {
               return Double.parseDouble(s);
            } catch (NumberFormatException var5) {
            }
         }

         return def;
      }
   }

   static boolean bool(Object o, boolean def) {
      if (o instanceof Boolean b) {
         return b;
      } else {
         return o instanceof String s ? "true".equalsIgnoreCase(s) : def;
      }
   }

   static String str(Object o, String def) {
      return o == null ? def : jsStr(o);
   }

   static List<String> list(Object o, Object def) {
      Object src = o != null ? o : def;
      List<String> out = new ArrayList<>();
      if (src instanceof List) {
         for (Object x : (List)src) {
            if (x != null) {
               out.add(String.valueOf(x));
            }
         }
      }

      return out;
   }

   private String ellipsize(Canvas c, String font, float size, String s, float maxW) {
      if (s == null) {
         return "";
      }

      if (maxW <= 10.0F) {
         return "";
      }

      if (c.textWidth(font, size, s) <= maxW) {
         return s;
      }

      int lo = 0;
      int hi = s.length();

      while (lo < hi) {
         int mid = (lo + hi + 1) / 2;
         if (c.textWidth(font, size, s.substring(0, mid) + "…") <= maxW) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      return s.substring(0, lo).trim() + "…";
   }

   private List<String> wrap(Canvas c, String font, float size, String s, float maxW) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : s.split(" ")) {
         String t = line.length() == 0 ? word : line + " " + word;
         if (c.textWidth(font, size, t) > maxW && line.length() > 0) {
            out.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(t);
         }

         if (out.size() >= 8) {
            break;
         }
      }

      if (line.length() > 0 && out.size() < 9) {
         out.add(line.toString());
      }

      return out;
   }

   boolean tabsMode() {
      return "Tabs".equals(this.gstr("GUI", "ClickGUI"));
   }

   boolean compact() {
      return this.panelMode() || this.tabsMode();
   }

   private float tfs() {
      return this.gnum("Tabs Font Size", 12.0);
   }

   private float tRow() {
      return this.tfs() + 8.0F;
   }

   private float tLabelH() {
      return this.tfs() + 3.0F;
   }

   private float tBoxH() {
      return Math.round(this.tfs() * 1.55F);
   }

   private float tBarH() {
      return this.tfs() + 2.0F;
   }

   private float tToggleS() {
      return this.tfs() + 1.0F;
   }

   void applyTabsFont(Canvas c) {
      String f = this.gstr("Tabs Font", "JetBrains Mono");
      if (!"Match GUI".equals(f)) {
         c.setFontFamily(f);
      }
   }

   private void tabsPalette() {
      this.tAccent = this.th.accent | 0xFF000000;
      this.tAccentDark = Theme.mix(this.tAccent, -16777216, 0.45F);
      if ("Theme".equals(this.gstr("Tabs Palette", "Classic"))) {
         this.tBg = this.th.bg | 0xFF000000;
         this.tMain = Theme.mix(this.th.surface, this.th.bg, 0.35F) | 0xFF000000;
         this.tOutline = this.th.border | 0xFF000000;
         this.tBlack = Theme.mix(this.th.bg, -16777216, 0.7F) | 0xFF000000;
         this.tFont = this.th.text | 0xFF000000;
         this.tMuted = this.th.muted | 0xFF000000;
      } else {
         this.tMain = -14935012;
         this.tBg = -15461356;
         this.tOutline = -13487566;
         this.tBlack = -16777216;
         this.tFont = -1;
         this.tMuted = -7566196;
      }

      this.tShadow = this.gbool("Tabs Text Shadow", true);
   }

   private void layoutTabs(float o) {
      float W = this.gnum("Tabs Width", 640.0);
      float H = this.gnum("Tabs Height", 560.0);
      this.ww = Math.max(200.0F, Math.min(W, this.sw - 16.0F));
      this.wh = Math.max(160.0F, Math.min(H, this.sh - 16.0F));
      if (Float.isNaN(this.tabsFx) || Float.isNaN(this.tabsFy)) {
         this.tabsFx = 0.5F;
         this.tabsFy = 0.5F;
         String[] f = str(this.store.gui("Tabs Position"), "").split("\\|");
         if (f.length == 2) {
            try {
               float a = Float.parseFloat(f[0]);
               float b = Float.parseFloat(f[1]);
               if (Float.isFinite(a) && Float.isFinite(b)) {
                  this.tabsFx = a;
                  this.tabsFy = b;
               }
            } catch (NumberFormatException var7) {
            }
         }
      }

      this.wx = Math.max(0.0F, Math.min(this.sw - this.ww, this.tabsFx * this.sw - this.ww / 2.0F));
      this.wy = Math.max(0.0F, Math.min(this.sh - this.wh, this.tabsFy * this.sh - this.wh / 2.0F));
      this.winScale = 1.0F;
      if (!this.snapPass) {
         switch (this.gstr("Open Animation", "Fade")) {
            case "Pop":
               this.wy = this.wy + (1.0F - Anim.easeOutCubic(o)) * 26.0F;
               this.winScale = 0.9F + 0.1F * Anim.easeOutBack(Math.min(1.0F, o * 1.02F));
               break;
            case "Slide":
               this.wy = this.wy + (1.0F - Anim.easeOutCubic(o)) * this.sh * 0.4F;
               break;
            case "Zoom":
               this.winScale = 0.55F + 0.45F * Anim.easeOutCubic(o);
         }
      }

      this.winCx = this.wx + this.ww / 2.0F;
      this.winCy = this.wy + this.wh / 2.0F;
   }

   private void moveTabsTo(float x, float y) {
      if (!(this.sw <= 0.0F) && !(this.sh <= 0.0F)) {
         this.tabsFx = (Math.max(0.0F, Math.min(this.sw - this.ww, x)) + this.ww / 2.0F) / this.sw;
         this.tabsFy = (Math.max(0.0F, Math.min(this.sh - this.wh, y)) + this.wh / 2.0F) / this.sh;
      }
   }

   private void saveTabsPos() {
      try {
         this.store.setGui("Tabs Position", String.format(Locale.ROOT, "%.4f|%.4f", this.tabsFx, this.tabsFy));
      } catch (Throwable var2) {
      }
   }

   private void centerTabs() {
      this.tabsFx = 0.5F;
      this.tabsFy = 0.5F;
      this.saveTabsPos();
      this.toast("Window centred");
   }

   private Set<String> folded() {
      if (this.tFolded == null) {
         this.tFolded = new LinkedHashSet<>(list(this.store.gui("Tabs Folded"), List.of()));
      }

      return this.tFolded;
   }

   private void toggleFold(String id) {
      Set<String> f = this.folded();
      if (!f.remove(id)) {
         f.add(id);
      }

      try {
         this.store.setGui("Tabs Folded", new ArrayList<>(f));
      } catch (Throwable var4) {
      }
   }

   private List<String> tabKeys() {
      List<String> out = new ArrayList<>();

      for (String cat : this.data.categories.keySet()) {
         if (!"Client".equals(cat)) {
            out.add(cat);
         }
      }

      out.add("Friends");
      out.add("GUI");
      return out;
   }

   private static String tabLabel(String key) {
      return "GUI".equals(key) ? "Settings" : key;
   }

   private String tabsPage(List<String> keys) {
      if (keys.contains(this.page)) {
         return this.page;
      } else if (!"Client".equals(this.page) && !Arrays.asList(SPECIAL).contains(this.page)) {
         return keys.isEmpty() ? "GUI" : keys.get(0);
      } else {
         return "GUI";
      }
   }

   private void drawTabs(Canvas c) {
      this.tabsPalette();
      this.tColRects.clear();
      float fs = this.tfs();
      float x = this.wx;
      float y = this.wy;
      float w = this.ww;
      float h = this.wh;
      if (this.gbool("Tabs Window Shadow", true)) {
         c.shadow(x, y + 4.0F, w, h, 0.0F, 24.0F, Theme.a(-16777216, 0.6F));
      }

      c.rect(x, y, w, h, 0.0F, this.tBlack);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, 0.0F, this.gbool("Tabs Accent Border", true) ? this.tAccent : this.tOutline);
      c.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, this.tMain);
      float titleH = Math.max(22.0F, fs + 13.0F);
      this.hit(x, y, w, titleH, () -> {
         float ox = this.wmx - this.wx;
         float oy = this.wmy - this.wy;
         this.tabsDragging = true;
         this.drag2 = pt -> this.moveTabsTo(pt[0] - ox, pt[1] - oy);
      }, null);
      String srv = null;

      try {
         srv = this.store.currentServer();
      } catch (Throwable var40) {
      }

      String sub = srv != null && !srv.isBlank() ? srv : "Minecraft";
      float tw = this.tText(c, "inter-med", fs, x + 8.0F, y + titleH / 2.0F, 17, this.tFont, "Clarity");
      this.tText(c, "inter", fs, x + 8.0F + tw, y + titleH / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, " | " + sub, w - tw - 190.0F));
      this.drawTabsSearch(c, x + w - 8.0F - 160.0F, y + (titleH - this.tBoxH() + 2.0F) / 2.0F, 160.0F, this.tBoxH() - 2.0F);
      float msx = x + 8.0F;
      float msy = y + titleH;
      float msw = w - 16.0F;
      float msh = h - titleH - 8.0F;
      c.rect(msx, msy, msw, msh, 0.0F, this.tOutline);
      c.rect(msx + 1.0F, msy + 1.0F, msw - 2.0F, msh - 2.0F, 0.0F, this.tBlack);
      c.rect(msx + 2.0F, msy + 2.0F, msw - 4.0F, msh - 4.0F, 0.0F, this.tBg);
      List<String> keys = this.tabKeys();
      String cur = this.tabsPage(keys);
      boolean searching = !this.search.text.isEmpty();
      float tabH = fs + 9.0F;
      float tax = msx + 8.0F;
      float tay = msy + 8.0F;
      float taw = msw - 16.0F;
      float cx = tax;
      float cy = tay + tabH + 1.0F;
      float cw = taw;
      float ch = msy + msh - 8.0F - cy;
      c.rect(cx - 1.0F, cy - 1.0F, cw + 2.0F, ch + 2.0F, 0.0F, this.tOutline);
      c.rect(cx, cy, cw, ch, 0.0F, this.tMain);
      float natural = 0.0F;

      for (String k : keys) {
         natural += c.textWidth("inter", fs, tabLabel(k)) + 12.0F + 1.0F;
      }

      float squeeze = natural > taw ? taw / natural : 1.0F;
      float tx = tax;

      for (String k : keys) {
         String label = tabLabel(k);
         float tbw = Math.max(18.0F, (c.textWidth("inter", fs, label) + 12.0F) * squeeze);
         boolean act = !searching && k.equals(cur);
         boolean hv = this.in(tx, tay, tbw, tabH);
         c.rect(tx - 1.0F, tay - 1.0F, tbw + 2.0F, tabH + 2.0F, 0.0F, this.tOutline);
         c.rect(tx, tay, tbw, tabH, 0.0F, act ? this.tMain : Theme.mix(this.tBg, this.tMain, hv ? 0.6F : 0.0F));
         if (act) {
            c.rect(tx, tay + tabH, tbw, 1.2F, 0.0F, this.tMain);
         }

         this.tText(c, "inter", fs, tx + tbw / 2.0F, tay + tabH / 2.0F, 18, !act && !hv ? Theme.mix(this.tFont, this.tMuted, 0.35F) : this.tFont, this.ellipsize(c, "inter", fs, label, tbw - 4.0F));
         String target = k;
         this.hit(tx, tay, tbw, tabH, () -> {
            if (!this.search.text.isEmpty()) {
               this.search.set("");
            }

            this.switchPage(target);
         }, null);
         tx += tbw + 1.0F;
      }

      String pt = this.gstr("Page Transition", "Slide");
      float since = "None".equals(pt) ? 99.0F : (this.time - this.pageSwitchAt) * this.an_speed();
      float pageT = Anim.easeOutCubic(since / 0.25F);
      this.hoverPanel = this.pop == null && this.draggingPanel == null ? "§tabs" : null;
      c.save();
      c.scissor(cx, cy, cw, ch);
      if (pageT < 1.0F) {
         c.alpha(0.25F + 0.75F * pageT);
         if ("Slide".equals(pt)) {
            c.translate(0.0F, (1.0F - pageT) * 6.0F);
         }
      }

      List<ClickGui.TBox> boxes = searching ? this.searchBoxes() : this.boxesFor(cur);
      int n = "Friends".equals(cur) && !searching ? 1 : Math.max(1, Math.min(4, Math.round(this.gnum("Tabs Columns", 2.0))));
      if (boxes.isEmpty()) {
         this.tText(c, "inter", fs, cx + cw / 2.0F, cy + 40.0F, 18, this.tMuted, searching ? "No modules match \"" + this.search.text + "\"" : "Nothing here");
      } else {
         this.drawColumns(c, boxes, n, cx, cy, cw, ch);
      }

      c.restore();
      float g = 10.0F;
      float gx = x + w - g - 2.0F;
      float gy = y + h - g - 2.0F;
      boolean ghv = this.in(gx, gy, g, g);
      int gc = ghv ? this.tAccent : Theme.a(this.tFont, 0.25F);

      for (int k = 1; k <= 3; k++) {
         c.line(gx + g - k * 3.0F, gy + g, gx + g, gy + g - k * 3.0F, 1.0F, gc);
      }

      this.hit(gx, gy, g + 2.0F, g + 2.0F, () -> {
         float ox = this.wmx;
         float oy = this.wmy;
         float W0 = this.ww;
         float H0 = this.wh;
         this.drag2 = pt2 -> {
            float nw = Math.round((W0 + pt2[0] - ox) / 10.0F) * 10.0F;
            float nh = Math.round((H0 + pt2[1] - oy) / 10.0F) * 10.0F;
            this.store.setGui("Tabs Width", (double)Math.max(460.0F, Math.min(1200.0F, nw)));
            this.store.setGui("Tabs Height", (double)Math.max(340.0F, Math.min(900.0F, nh)));
         };
      }, null);
      if (this.pop != null && !this.snapPass) {
         this.drawPop(c);
      }
   }

   private void drawTabsSearch(Canvas c, float x, float y, float w, float h) {
      boolean sf = this.focused == this.search;
      boolean hv = this.in(x, y, w, h);
      this.tFrame(c, x, y, w, h, hv || sf, false);
      float fs = this.tfs() - 0.5F;
      boolean empty = this.search.text.isEmpty();
      c.save();
      c.scissor(x + 3.0F, y, w - 6.0F, h);
      this.tText(c, "inter", fs, x + 6.0F, y + h / 2.0F, 17, empty ? this.tMuted : this.tFont, empty ? (sf ? "" : "Search…") : this.search.text);
      if (sf) {
         this.caret(c, "inter", fs, x + 6.0F, y + h / 2.0F, this.search);
      }

      c.restore();
      this.hit(x, y, w, h, () -> this.focus(this.search, null), () -> this.search.set(""));
   }

   private String colKey(int i) {
      return this.pageKey() + "#c" + i;
   }

   private void drawColumns(Canvas c, List<ClickGui.TBox> boxes, int n, float cx, float cy, float cw, float ch) {
      float pad = 8.0F;
      float gap = 8.0F;
      float colW = (cw - pad * 2.0F - gap * (n - 1)) / n;
      float top = cy + pad;
      float colH = ch - pad;
      StringBuilder sig = new StringBuilder(this.pageKey()).append('|').append(n);

      for (ClickGui.TBox b : boxes) {
         sig.append('|').append(b.id());
      }

      String key = sig.toString();
      if (!key.equals(this.tAssignKey)) {
         if (!this.search.text.isEmpty() && !this.tAssignKey.startsWith("§search")) {
            for (int i = 0; i < n; i++) {
               this.scroll.remove(this.colKey(i));
            }
         }

         this.tAssignKey = key;
         this.tAssignFrames = 0;
      }

      if (this.tAssignFrames++ < 4) {
         float[] hgt = new float[n];

         for (ClickGui.TBox b : boxes) {
            int best = 0;

            for (int i = 1; i < n; i++) {
               if (hgt[i] < hgt[best] - 0.5F) {
                  best = i;
               }
            }

            this.tAssign.put(b.id(), best);
            hgt[best] += this.measured.getOrDefault("tbh:" + b.id(), b.est()) + gap;
         }
      }

      for (int i = 0; i < n; i++) {
         float colX = cx + pad + i * (colW + gap);
         float[] st = this.scroll.computeIfAbsent(this.colKey(i), k -> new float[3]);
         st[0] = Math.max(0.0F, Math.min(st[2], st[0]));
         float sy = this.an.to("tsc:" + this.colKey(i), st[0], 16.0F);
         this.tColRects.add(new float[]{colX, top, colW, colH, i});
         c.save();
         c.scissor(colX - 1.0F, top - 1.0F, colW + 2.0F, colH + 1.0F);
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH = this.clipH;
         this.intersectClip(colX - 1.0F, top - 1.0F, colW + 2.0F, colH + 1.0F);
         float y0 = top - sy;
         float y = y0;
         boolean any = false;

         for (ClickGui.TBox b : boxes) {
            if (this.tAssign.getOrDefault(b.id(), 0) == i) {
               if (any) {
                  y += gap;
               }

               any = true;
               Float known = this.measured.get("tbh:" + b.id());
               if (known == null || !(y > top + colH + 2.0F) && !(y + known < top - 2.0F)) {
                  float end = b.draw().draw(c, colX, y, colW);
                  this.measured.put("tbh:" + b.id(), end - y);
                  y = end;
               } else {
                  y += known;
               }
            }
         }

         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH;
         c.restore();
         st[2] = Math.max(0.0F, y - y0 - colH + 4.0F);
         if (st[2] > 1.0F) {
            float barH = Math.max(16.0F, colH * colH / (colH + st[2]));
            float barY = top + (colH - barH) * (sy / st[2]);
            c.rect(colX + colW + (n > 1 && i < n - 1 ? gap / 2.0F - 1.0F : 3.0F), barY, 2.0F, barH, 0.0F, Theme.a(this.tAccent, 0.75F));
         }
      }
   }

   private void tabsScroll(float delta) {
      ClickGui.Hit h = this.hitAt(this.wmx, this.wmy);
      if (h != null && h.wheel != null) {
         h.wheel.accept(delta);
      } else {
         for (float[] r : this.tColRects) {
            if (this.wmx >= r[0] - 4.0F && this.wmx <= r[0] + r[2] + 4.0F && this.wmy >= r[1] && this.wmy <= r[1] + r[3]) {
               float[] st = this.scroll.computeIfAbsent(this.colKey((int)r[4]), k -> new float[3]);
               st[0] = Math.max(0.0F, Math.min(st[2], st[0] - delta * this.tRow() * 2.2F));
               return;
            }
         }
      }
   }

   private List<ClickGui.TBox> boxesFor(String tab) {
      List<ClickGui.TBox> out = new ArrayList<>();
      if ("Friends".equals(tab)) {
         out.add(
            new ClickGui.TBox(
               "friends",
               300.0F,
               (c, x, y, w) -> this.tGroup(c, "friends", "Friends", null, x, y, w, false, (cc, ix, iy, iw) -> this.drawFriendsPanel(cc, this.tabsPanel, ix - 6.0F, iy, iw + 12.0F), null)
            )
         );
         return out;
      }

      if ("GUI".equals(tab)) {
         out.add(
            new ClickGui.TBox("menu", 260.0F, (c, x, y, w) -> this.tTabbox(c, "menu", x, y, w, new String[]{"General", "Tabs", "Sounds"}, (cc, t, ix, iy, iw) -> this.guiSection(cc, t, ix, iy, iw)))
         );
         out.add(new ClickGui.TBox("themes", 90.0F, (c, x, y, w) -> this.tGroup(c, "themes", "Themes", null, x, y, w, true, this::tThemesBody, null)));
         out.add(new ClickGui.TBox("configs", 220.0F, (c, x, y, w) -> this.tTabbox(c, "configs", x, y, w, new String[]{"Local", "Cloud", "Browse"}, (cc, t, ix, iy, iw) -> {
            return switch (t) {
               case "Cloud" -> this.drawPanelCloud(cc, this.tabsPanel, ix, iy, iw);
               case "Browse" -> this.drawPanelBrowse(cc, this.tabsPanel, ix, iy, iw);
               default -> this.drawPanelProfiles(cc, this.tabsPanel, ix, iy, iw);
            };
         })));
         out.add(
            new ClickGui.TBox(
               "effects", 200.0F, (c, x, y, w) -> this.tTabbox(c, "effects", x, y, w, new String[]{"Background", "Animations", "3D"}, (cc, t, ix, iy, iw) -> this.guiSection(cc, t, ix, iy, iw))
            )
         );

         for (GuiData.Module m : this.data.categories.getOrDefault("Client", List.of())) {
            out.add(this.moduleBox(m));
         }

         return out;
      } else {
         for (GuiData.Module m : this.data.categories.getOrDefault(tab, List.of())) {
            out.add(this.moduleBox(m));
         }

         return out;
      }
   }

   private List<ClickGui.TBox> searchBoxes() {
      List<ClickGui.TBox> out = new ArrayList<>();

      for (List<GuiData.Module> mods : this.data.categories.values()) {
         for (GuiData.Module m : mods) {
            if (this.matchesSearch(m)) {
               out.add(this.moduleBox(m));
            }
         }
      }

      return out;
   }

   private ClickGui.TBox moduleBox(GuiData.Module m) {
      String id = "m:" + m.name;
      float est = 24.0F + this.tRow() + Math.min(14, m.settings.size()) * (this.tLabelH() + this.tBarH() + 6.0F);
      return new ClickGui.TBox(
         id,
         est,
         (c, x, y, w) -> this.tGroup(
            c, id, m.name, this.gbool("Show Descriptions", true) ? m.description : null, x, y, w, true, (cc, ix, iy, iw) -> this.tModuleBody(cc, m, ix, iy, iw), () -> this.moduleMenu(id, m, x, y, w)
         )
      );
   }

   private void moduleMenu(String id, GuiData.Module m, float x, float y, float w) {
      boolean isFolded = this.folded().contains(id);
      boolean hold = "hold".equals(this.store.bindMode(m.name));
      List<String> opts = List.of(isFolded ? "Expand" : "Collapse", hold ? "Bind mode: Toggle" : "Bind mode: Hold", "Reset settings");
      this.openList(id + "#menu", x, y, w, this.tfs() + 10.0F, 120.0F, opts, "", v -> {
         switch (v) {
            case "Expand":
            case "Collapse":
               this.toggleFold(id);
               break;
            case "Reset settings":
               if (!("reset:" + m.name).equals(this.confirmId) || this.time - this.confirmAt >= 2.5F) {
                  this.toast("Pick Reset settings again to confirm");
               }

               this.confirm("reset:" + m.name, () -> {
                  this.store.resetModule(m.name);
                  this.toast(m.name + " reset to defaults");
               });
               break;
            default:
               String bind = this.store.bind(m.name);
               this.store.setBind(m.name, bind != null && !bind.isEmpty() && !"null".equals(bind) ? bind : null, hold ? "toggle" : "hold");
               this.toast(m.name + ": " + (hold ? "toggle" : "hold") + " mode");
         }
      });
   }

   private float guiSection(Canvas c, String section, float x, float y, float w) {
      List<GuiData.Setting> list = new ArrayList<>();

      for (GuiData.Setting st : GUI_SECTIONS.getOrDefault(section, List.of())) {
         if (!"Page Title".equals(st.name)) {
            list.add(st);
         }
      }

      y = this.tSettings(c, "Click GUI", list, x, y, w);
      if ("General".equals(section)) {
         boolean web = this.store.webMode();
         y = this.tButton(c, "tOpenWeb", web ? "Open the web GUI" : "Switch to the web GUI", x, y, w, web ? this.store::openWebGui : () -> {
            this.store.switchToWebGui();
            this.close();
         });
      }

      if ("Tabs".equals(section)) {
         y = this.tButton(c, "tCenter", "Centre window", x, y, w, this::centerTabs);
      }

      return y;
   }

   private float tThemesBody(Canvas c, float x, float y, float w) {
      boolean tc = this.store.themeColorsEnabled();
      y = this.tToggle(c, "tthemecols", "Theme colours for visuals", tc, x, y, w, () -> this.store.setThemeColorsEnabled(!tc));
      String active = this.store.themeName() == null ? "" : this.store.themeName();
      List<String> names = new ArrayList<>(this.data.themes.keySet());
      y = this.tDropdown(c, "tthemes", "Theme", x, y, w, names, active, v -> {
         List<String> colsx = this.data.themes.get(v);
         if (colsx != null) {
            this.changeTheme(v, colsx);
         }
      });
      List<String> cols = this.data.themes.get(active);
      if (cols != null) {
         float sz = this.tToggleS();
         float sx = x;

         for (int k = 0; k < Math.min(5, cols.size()); k++) {
            int col = 0xFF000000 | Theme.hex(cols.get(k), 8947848);
            c.rect(sx, y, sz * 2.0F, sz, 0.0F, this.tBlack);
            c.rect(sx + 1.0F, y + 1.0F, sz * 2.0F - 2.0F, sz - 2.0F, 0.0F, col);
            sx += sz * 2.0F + 4.0F;
         }

         y += sz + 6.0F;
      }

      return y;
   }

   private float tGroup(Canvas c, String id, String title, String tip, float x, float y, float w, boolean foldable, ClickGui.TDraw body, Runnable menu) {
      float fs = this.tfs();
      float headH = fs + 9.0F;
      boolean isFolded = foldable && this.folded().contains(id);
      float open = this.an.to("tfold:" + id, isFolded ? 0.0F : 1.0F, 14.0F);
      float bodyH = this.measured.getOrDefault("tbc:" + id, 40.0F) * Anim.ease(open);
      float boxH = headH + bodyH + 3.0F;
      c.rect(x, y, w, boxH, 0.0F, this.tOutline);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, boxH - 2.0F, 0.0F, this.tBlack);
      c.rect(x + 2.0F, y + 2.0F, w - 4.0F, boxH - 4.0F, 0.0F, this.tBg);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, 2.0F, 0.0F, this.tAccent);
      boolean hv = this.in(x, y, w, headH);
      this.tText(c, "inter", fs, x + 6.0F, y + 3.0F + (headH - 3.0F) / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, title, w - 26.0F));
      if (foldable && (hv || isFolded)) {
         this.tText(c, "inter", fs, x + w - 7.0F, y + 3.0F + (headH - 3.0F) / 2.0F, 20, hv ? this.tAccent : this.tMuted, isFolded ? "+" : "-");
      }

      this.hit(x, y, w, headH, foldable ? () -> this.toggleFold(id) : () -> {}, menu);
      if (tip != null && !tip.isEmpty() && hv) {
         this.hoverTip("tg:" + id, tip);
      } else {
         this.hoverSinceReset("tg:" + id);
      }

      if (bodyH > 0.5F) {
         float top = y + headH;
         c.save();
         c.scissor(x + 2.0F, top, w - 4.0F, bodyH);
         float sX = this.clipX;
         float sY = this.clipY;
         float sW = this.clipW;
         float sH = this.clipH;
         this.intersectClip(x + 2.0F, top, w - 4.0F, bodyH);
         float end = body.draw(c, x + 6.0F, top, w - 12.0F);
         this.measured.put("tbc:" + id, end - top + 2.0F);
         this.clipX = sX;
         this.clipY = sY;
         this.clipW = sW;
         this.clipH = sH;
         c.restore();
      } else if (!this.measured.containsKey("tbc:" + id)) {
         this.measured.put("tbc:" + id, 40.0F);
      }

      return y + boxH;
   }

   private float tTabbox(Canvas c, String id, float x, float y, float w, String[] tabs, ClickGui.TTabBody body) {
      float fs = this.tfs();
      float headH = fs + 9.0F;
      String cur = TABBOX_STATE.getOrDefault(id, tabs[0]);
      if (!Arrays.asList(tabs).contains(cur)) {
         cur = tabs[0];
      }

      float bodyH = this.measured.getOrDefault("tbc:" + id + "/" + cur, 60.0F);
      bodyH = this.an.to("tbxh:" + id, bodyH, 18.0F);
      float boxH = 1.0F + headH + 4.0F + bodyH + 3.0F;
      c.rect(x, y, w, boxH, 0.0F, this.tOutline);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, boxH - 2.0F, 0.0F, this.tBlack);
      c.rect(x + 2.0F, y + 2.0F, w - 4.0F, boxH - 4.0F, 0.0F, this.tBg);
      float bx = x + 1.0F;
      float bw = (w - 2.0F) / tabs.length;

      for (int i = 0; i < tabs.length; i++) {
         String t = tabs[i];
         boolean act = t.equals(cur);
         float tx = bx + i * bw;
         boolean hv = this.in(tx, y + 1.0F, bw, headH);
         if (!act) {
            c.rect(tx, y + 1.0F, bw, headH, 0.0F, Theme.mix(this.tMain, this.tFont, hv ? 0.05F : 0.0F));
            c.rect(tx, y + 1.0F + headH - 1.0F, bw, 1.0F, 0.0F, this.tOutline);
         } else {
            c.rect(tx, y + 1.0F, bw, 2.0F, 0.0F, this.tAccent);
         }

         if (i > 0) {
            c.rect(tx, y + 1.0F, 1.0F, headH, 0.0F, this.tOutline);
         }

         this.tText(
            c,
            "inter",
            fs,
            tx + bw / 2.0F,
            y + 1.0F + headH / 2.0F + (act ? 1 : 0),
            18,
            act ? this.tFont : Theme.mix(this.tFont, this.tMuted, hv ? 0.1F : 0.4F),
            this.ellipsize(c, "inter", fs, t, bw - 6.0F)
         );
         String target = t;
         this.hit(tx, y + 1.0F, bw, headH, () -> TABBOX_STATE.put(id, target), null);
      }

      float top = y + 1.0F + headH + 4.0F;
      c.save();
      c.scissor(x + 2.0F, top - 2.0F, w - 4.0F, bodyH + 2.0F);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sH = this.clipH;
      this.intersectClip(x + 2.0F, top - 2.0F, w - 4.0F, bodyH + 2.0F);
      float end = body.draw(c, cur, x + 6.0F, top, w - 12.0F);
      this.measured.put("tbc:" + id + "/" + cur, end - top + 2.0F);
      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sH;
      c.restore();
      return y + boxH;
   }

   private float tModuleBody(Canvas c, GuiData.Module m, float x, float y, float w) {
      String name = m.name;
      boolean on = this.store.isEnabled(name);
      float rowTop = y;
      y = this.tToggle(c, "en:" + name, "Enabled", on, x, y, w, () -> this.toggleModule(m));
      String bind = this.store.bind(name);
      boolean unset = bind == null || bind.isEmpty() || "null".equals(bind) || "NONE".equals(bind);
      boolean listening = name.equals(this.listenModule) && this.listenSetting == null;
      boolean hold = "hold".equals(this.store.bindMode(name));
      String tag = listening ? "[...]" : "[" + (unset ? "None" : bind) + "]" + (hold && !unset ? " hold" : "");
      float fs = this.tfs() - 0.5F;
      float kw = c.textWidth("inter", fs, tag) + 4.0F;
      float rh = this.tRow();
      boolean khv = this.in(x + w - kw, rowTop, kw, rh);
      float pulse = listening ? 0.55F + 0.45F * (float)Math.sin(this.time * 8.0F) : 1.0F;
      this.tText(c, "inter", fs, x + w, rowTop + rh / 2.0F, 20, listening ? Theme.a(this.tAccent, pulse) : (khv ? this.tAccent : (unset ? this.tMuted : this.tFont)), tag);
      this.hit(x + w - kw, rowTop, kw, rh, () -> {
         this.listenModule = name;
         this.listenSetting = null;
      }, () -> {
         List<String> modes = List.of("Toggle", "Hold", "Clear");
         this.openList("tbind:" + name, x + w - 90.0F, rowTop, 90.0F, rh, 90.0F, modes, hold ? "Hold" : "Toggle", v -> {
            String b = unset ? null : bind;
            switch (v) {
               case "Clear":
                  this.store.setBind(name, null, hold ? "hold" : "toggle");
                  this.toast(name + " unbound");
                  break;
               case "Hold":
                  this.store.setBind(name, b, "hold");
                  this.toast(name + ": hold mode");
                  break;
               default:
                  this.store.setBind(name, b, "toggle");
                  this.toast(name + ": toggle mode");
            }
         });
      });
      if (!on && "When Enabled".equals(this.gstr("Tabs Show Settings", "Always"))) {
         return y;
      } else {
         return HudEditorPanel.isHudModule(name) ? this.tButton(c, "thud:" + name, "Open in HUD editor", x, y, w, () -> this.openHudEditor(name)) : this.tSettings(c, name, m.settings, x, y, w);
      }
   }

   private float tSettings(Canvas c, String module, List<GuiData.Setting> settings, float x, float y, float w) {
      for (GuiData.Setting s : settings) {
         if (this.visible(module, s)) {
            Map<String, List<String>> groups = "select".equals(s.type) ? s.groups() : null;
            if (groups != null && !groups.isEmpty()) {
               String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
               String cat = null;

               for (Entry<String, List<String>> e : groups.entrySet()) {
                  if (e.getValue().contains(cur)) {
                     cat = e.getKey();
                     break;
                  }
               }

               if (cat == null) {
                  cat = groups.keySet().iterator().next();
               }

               y = this.tDropdown(c, module + "/" + s.name + "/type", s.name + " Type", x, y, w, new ArrayList<>(groups.keySet()), cat, v -> {
                  List<String> entries = groups.get(v);
                  if (entries != null && !entries.isEmpty() && !entries.contains(str(this.get(module, s.name), ""))) {
                     this.set(module, s.name, entries.get(0));
                  }
               });
               List<String> entries = groups.getOrDefault(cat, List.of());
               y = this.tDropdown(c, module + "/" + s.name, s.name, x, y, w, entries, !entries.contains(cur) && !entries.isEmpty() ? entries.get(0) : cur, v -> this.set(module, s.name, v));
            } else {
               y = this.tControl(c, module, s, x, y, w);
            }
         }
      }

      return y;
   }

   private float tControl(Canvas c, String module, GuiData.Setting s, float x, float y, float w) {
      String id = module + "/" + s.name;
      float fs = this.tfs();
      if (isModelFile(module, s)) {
         return this.tDropdown(c, id, s.name, x, y, w, this.modelFileOptions(), this.modelFileValue(module, s), vx -> this.pickModelFile(module, s.name, vx));
      }

      if (isSoundSelect(module, s)) {
         return this.tDropdown(c, id, s.name, x, y, w, this.soundOptions(s), str(this.get(module, s.name), String.valueOf(s.defaultValue())), vx -> this.pickSound(module, s.name, vx));
      }

      switch (s.type) {
         case "check": {
            boolean v = bool(this.get(module, s.name), (Boolean)s.defaultValue());
            return this.tToggle(c, "ck:" + id, s.name, v, x, y, w, () -> this.set(module, s.name, !v));
         }
         case "range":
            return this.tSlider(c, module, s, x, y, w);
         case "minmax":
            return this.tMinMax(c, module, s, x, y, w);
         case "select": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            return this.tDropdown(c, id, s.name, x, y, w, s.options(), cur, vx -> this.set(module, s.name, vx));
         }
         case "skinselect":
         case "capeselect": {
            List<String> opts = new ArrayList<>(List.of("none"));
            if ("skinselect".equals(s.type)) {
               opts.add("default");
               opts.addAll(this.store.customSkins());
               opts.add("__add_skin__");
            } else {
               opts.addAll(this.data.capes);
               opts.addAll(this.store.customCapes());
            }

            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            return this.tDropdown(c, id, s.name, x, y, w, opts, cur, vx -> this.pickAsset(module, s.name, vx));
         }
         case "multiselect": {
            List<String> cur = list(this.get(module, s.name), s.defaultValue());
            return this.tMultiDropdown(c, id, s.name, x, y, w, s.options(), cur, o -> {
               List<String> next = new ArrayList<>(list(this.get(module, s.name), s.defaultValue()));
               if (!next.remove(o)) {
                  next.add(o);
               }

               this.set(module, s.name, next);
            }, () -> list(this.get(module, s.name), s.defaultValue()));
         }
         case "color": {
            String v = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            int col = this.resolveColor(v);
            float rh = this.tRow();
            boolean isOpen = this.pop != null && this.pop.color && id.equals(this.pop.id);
            float swW = fs * 2.3F;
            float swH = this.tToggleS();
            float sx = x + w - swW;
            float sy = y + (rh - swH) / 2.0F;
            boolean hv = this.in(x, y, w, rh);
            this.tText(c, "inter", fs, x, y + rh / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w - swW - 6.0F));
            c.rect(sx, sy, swW, swH, 0.0F, !isOpen && !hv ? Theme.mix(0xFF000000 | col, -16777216, 0.55F) : this.tAccent);
            c.rect(sx + 1.0F, sy + 1.0F, swW - 2.0F, swH - 2.0F, 0.0F, 0xFF000000 | col);
            float fy = y;
            this.hit(x, y, w, rh, () -> {
               if (isOpen) {
                  this.pop = null;
               } else {
                  this.openColor(id, module, s.name, x + w, fy + rh / 2.0F, rh / 2.0F + 2.0F, col);
               }
            }, null);
            return y + rh;
         }
         case "text":
            String value = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            return this.tInput(c, id, s.name, x, y, w, value, s.placeholder(), vx -> this.set(module, s.name, vx));
         case "keybind": {
            String cur = str(this.get(module, s.name), String.valueOf(s.defaultValue()));
            boolean listening = module.equals(this.listenModule) && s.name.equals(this.listenSetting);
            String tag = listening ? "[...]" : "[" + (!"NONE".equals(cur) && !cur.isEmpty() ? cur : "None") + "]";
            float rh = this.tRow();
            float tw = c.textWidth("inter", fs, tag);
            boolean hv = this.in(x, y, w, rh);
            this.tText(c, "inter", fs, x, y + rh / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w - tw - 8.0F));
            this.tText(c, "inter", fs, x + w, y + rh / 2.0F, 20, !listening && !hv ? this.tMuted : this.tAccent, tag);
            this.hit(x, y, w, rh, () -> {
               this.listenModule = module;
               this.listenSetting = s.name;
            }, () -> this.set(module, s.name, "NONE"));
            return y + rh;
         }
         case "blockpicker":
         case "itempicker": {
            String pid = "ids:" + module + "/" + s.name;
            boolean isOpen = this.pop != null && this.pop.ids && pid.equals(this.pop.id);
            List<String> cur = list(this.get(module, s.name), List.of());
            this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w));
            float by = y + this.tLabelH();
            float bh = this.tBoxH();
            boolean hv = this.in(x, by, w, bh);
            this.tFrame(c, x, by, w, bh, hv || isOpen, true);
            float is = bh - 5.0F;
            float ix = x + 5.0F;
            int shown = 0;

            for (String v : cur) {
               if (shown == 6 || ix + is > x + w - 40.0F) {
                  break;
               }

               this.itemIcon(c, bare(v), ix, by + 2.5F, is);
               ix += is + 2.0F;
               shown++;
            }

            String label = cur.isEmpty() ? "None" : (shown < cur.size() ? "+" + (cur.size() - shown) : (cur.size() == 1 ? this.displayId(cur.get(0)) : ""));
            if (!label.isEmpty()) {
               this.tText(c, "inter", fs, ix + (shown > 0 ? 3 : 0), by + bh / 2.0F, 17, cur.isEmpty() ? this.tMuted : this.tFont, this.ellipsize(c, "inter", fs, label, x + w - 18.0F - ix));
            }

            this.tArrow(c, x + w - 9.0F, by + bh / 2.0F, isOpen);
            this.hit(x, by, w, bh, () -> {
               if (isOpen) {
                  this.pop = null;
                  this.focused = null;
               } else {
                  this.openIds(pid, module, s, x, by, w, bh);
               }
            }, null);
            return by + bh + 5.0F;
         }
         default: {
            float rh = this.tRow();
            this.tText(c, "inter", fs, x, y + rh / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w * 0.6F));
            this.tText(c, "inter", fs - 1.0F, x + w, y + rh / 2.0F, 20, this.tMuted, "Web GUI");
            return y + rh;
         }
      }
   }

   private float tText(Canvas c, String font, float size, float x, float y, int align, int col, String s) {
      if (s != null && !s.isEmpty()) {
         if (this.tShadow) {
            c.text(font, size, x + 1.0F, y + 1.0F, align, Theme.a(-16777216, (col >>> 24 & 0xFF) / 255.0F * 0.85F), s);
         }

         return c.text(font, size, x, y, align, col, s);
      } else {
         return 0.0F;
      }
   }

   private void tFrame(Canvas c, float x, float y, float w, float h, boolean hot, boolean shade) {
      c.rect(x, y, w, h, 0.0F, hot ? this.tAccent : this.tBlack);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, 0.0F, this.tOutline);
      if (shade) {
         c.gradient(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, Theme.mix(this.tMain, -1, 0.04F), Theme.mix(this.tMain, -16777216, 0.25F), false);
      } else {
         c.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, this.tMain);
      }
   }

   private void tArrow(Canvas c, float cx, float cy, boolean up) {
      float s = Math.max(3.0F, this.tfs() * 0.3F);
      float[] xy = up ? new float[]{cx - s, cy + s * 0.5F, cx + s, cy + s * 0.5F, cx, cy - s * 0.6F} : new float[]{cx - s, cy - s * 0.5F, cx + s, cy - s * 0.5F, cx, cy + s * 0.6F};
      c.fillPoly(xy, this.tFont);
   }

   private float tToggle(Canvas c, String id, String label, boolean v, float x, float y, float w, Runnable click) {
      float rh = this.tRow();
      float b = this.tToggleS();
      float fs = this.tfs();
      float t = this.an.to("ttg:" + id, v ? 1.0F : 0.0F, 18.0F);
      float bx = x;
      float by = y + (rh - b) / 2.0F;
      boolean hv = this.in(x, y, w, rh);
      c.rect(bx, by, b, b, 0.0F, hv ? this.tAccent : this.tBlack);
      c.rect(bx + 1.0F, by + 1.0F, b - 2.0F, b - 2.0F, 0.0F, Theme.mix(this.tOutline, this.tAccentDark, t));
      c.rect(bx + 2.0F, by + 2.0F, b - 4.0F, b - 4.0F, 0.0F, Theme.mix(this.tMain, this.tAccent, t));
      this.tText(
         c,
         "inter",
         fs,
         bx + b + 6.0F,
         y + rh / 2.0F,
         17,
         Theme.mix(Theme.mix(this.tFont, this.tMuted, 0.2F), this.tFont, Math.max(t, hv ? 1.0F : 0.0F)),
         this.ellipsize(c, "inter", fs, label, w - b - 8.0F)
      );
      this.hit(x, y, w, rh, click, null);
      return y + rh;
   }

   private float tSlider(Canvas c, String module, GuiData.Setting s, float x, float y, float w) {
      String id = module + "/" + s.name;
      double min = s.num(0, 0.0);
      double max = s.num(1, 1.0);
      double def = s.num(2, min);
      double step = s.num(3, 0.0);
      double v = num(this.get(module, s.name), def);
      float frac = (float)Math.max(0.0, Math.min(1.0, (v - min) / Math.max(1.0E-9, max - min)));
      float shown = this.an.to("tsl:" + id, frac, 22.0F);
      boolean inside = "Inside".equals(this.gstr("Tabs Slider Labels", "Above"));
      float fs = this.tfs();
      float bh = this.tBarH();
      if (!inside) {
         this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w));
         y += this.tLabelH();
      }

      boolean dragging = this.drag != null && id.equals(this.dragId);
      boolean editing = this.focused != null && id.equals(this.focusId);
      boolean hv = this.in(x, y, w, bh);
      this.tFrame(c, x, y, w, bh, hv || dragging || editing, false);
      float fw = (w - 4.0F) * shown;
      if (fw > 0.5F) {
         c.rect(x + 2.0F, y + 2.0F, fw, bh - 4.0F, 0.0F, this.tAccentDark);
         if (fw > 2.0F) {
            c.rect(x + 3.0F, y + 3.0F, fw - 2.0F, bh - 6.0F, 0.0F, this.tAccent);
         }
      }

      String val = editing ? this.focused.text : fmt(v, step) + "/" + fmt(max, step);
      String txt = inside && !editing ? s.name + ": " + val : val;
      float tfs2 = fs - 0.5F;
      this.tText(c, "inter", tfs2, x + w / 2.0F, y + bh / 2.0F, 18, this.tFont, this.ellipsize(c, "inter", tfs2, txt, w - 6.0F));
      if (editing) {
         this.caret(c, "inter", tfs2, x + w / 2.0F - c.textWidth("inter", tfs2, txt) / 2.0F, y + bh / 2.0F, this.focused);
      }

      Consumer<Float> apply = px -> {
         double tt = Math.max(0.0F, Math.min(1.0F, (px - x - 2.0F) / (w - 4.0F)));
         this.sliderFrac = (float)tt;
         this.set(module, s.name, snap(min + tt * (max - min), step, min));
      };
      this.hit(x, y, w, bh, () -> {
         this.dragId = id;
         this.drag = apply;
         apply.accept(this.wmx);
      }, () -> {
         TextField f = new TextField(12);
         f.set(fmt(v, step));
         f.allSelected = true;
         this.focusId = id;
         this.focus(f, () -> {
            try {
               this.set(module, s.name, snap(Math.max(min, Math.min(max, Double.parseDouble(f.text.trim()))), step, min));
            } catch (NumberFormatException var11) {
            }
         });
      });
      return y + bh + 6.0F;
   }

   private float tMinMax(Canvas c, String module, GuiData.Setting s, float x, float y, float w) {
      String id = module + "/" + s.name;
      double min = s.num(0, 0.0);
      double max = s.num(1, 1.0);
      double step = s.num(4, 0.0);
      double lo = num(this.get(module, s.name + "_lo"), s.num(2, min));
      double hi = num(this.get(module, s.name + "_hi"), s.num(3, max));
      float a = (float)((lo - min) / Math.max(1.0E-9, max - min));
      float b = (float)((hi - min) / Math.max(1.0E-9, max - min));
      float sa = this.an.to("tmA:" + id, a, 22.0F);
      float sb = this.an.to("tmB:" + id, b, 22.0F);
      boolean inside = "Inside".equals(this.gstr("Tabs Slider Labels", "Above"));
      float fs = this.tfs();
      float bh = this.tBarH();
      if (!inside) {
         this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, s.name, w));
         y += this.tLabelH();
      }

      boolean hv = this.in(x, y, w, bh);
      boolean dragging = this.drag != null && id.equals(this.dragId);
      this.tFrame(c, x, y, w, bh, hv || dragging, false);
      float fx = x + 2.0F + (w - 4.0F) * sa;
      float fw = Math.max(2.0F, (w - 4.0F) * (sb - sa));
      c.rect(fx, y + 2.0F, fw, bh - 4.0F, 0.0F, this.tAccentDark);
      if (fw > 2.0F) {
         c.rect(fx + 1.0F, y + 3.0F, fw - 2.0F, bh - 6.0F, 0.0F, this.tAccent);
      }

      String val = fmt(lo, step) + " - " + fmt(hi, step);
      float tfs2 = fs - 0.5F;
      this.tText(c, "inter", tfs2, x + w / 2.0F, y + bh / 2.0F, 18, this.tFont, this.ellipsize(c, "inter", tfs2, inside ? s.name + ": " + val : val, w - 6.0F));
      float by = y;
      this.hit(x, by, w, bh, () -> {
         double t0 = Math.max(0.0F, Math.min(1.0F, (this.wmx - x - 2.0F) / (w - 4.0F)));
         int[] grab = new int[]{minmaxGrab(t0, a, b)};
         this.dragId = id;
         this.drag = px -> {
            double t = Math.max(0.0F, Math.min(1.0F, (px - x - 2.0F) / (w - 4.0F)));
            this.sliderFrac = (float)t;
            double nv = snap(min + t * (max - min), step, min);
            double curLo = num(this.get(module, s.name + "_lo"), s.num(2, min));
            double curHi = num(this.get(module, s.name + "_hi"), s.num(3, max));
            grab[0] = this.minmaxApply(module, s.name, nv, curLo, curHi, grab[0]);
         };
         this.drag.accept(this.wmx);
      }, null);
      return y + bh + 6.0F;
   }

   private float tDropdown(Canvas c, String id, String label, float x, float y, float w, List<String> options, String cur, Consumer<String> pick) {
      float fs = this.tfs();
      float bh = this.tBoxH();
      if (label != null) {
         this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, label, w));
         y += this.tLabelH();
      }

      boolean isOpen = this.pop != null && !this.pop.color && id.equals(this.pop.id);
      boolean hv = this.in(x, y, w, bh);
      this.tFrame(c, x, y, w, bh, hv || isOpen, true);
      this.tText(c, "inter", fs, x + 6.0F, y + bh / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, display(cur), w - 24.0F));
      this.tArrow(c, x + w - 9.0F, y + bh / 2.0F, isOpen);
      float by = y;
      this.hit(x, y, w, bh, () -> {
         if (isOpen) {
            this.pop = null;
         } else {
            if (!options.isEmpty()) {
               this.openList(id, x, by, w, bh, w, options, cur, pick);
            }
         }
      }, () -> {
         if (!options.isEmpty()) {
            int i = options.indexOf(cur);
            pick.accept(options.get((i + 1) % options.size()));
         }
      });
      return y + bh + 5.0F;
   }

   private float tMultiDropdown(Canvas c, String id, String label, float x, float y, float w, List<String> options, List<String> cur, Consumer<String> toggle, Supplier<List<String>> live) {
      float fs = this.tfs();
      float bh = this.tBoxH();
      this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, label, w));
      y += this.tLabelH();
      boolean isOpen = this.pop != null && !this.pop.color && id.equals(this.pop.id);
      boolean hv = this.in(x, y, w, bh);
      this.tFrame(c, x, y, w, bh, hv || isOpen, true);
      List<String> ordered = new ArrayList<>();

      for (String o : options) {
         if (cur.contains(o)) {
            ordered.add(o);
         }
      }

      String txt = ordered.isEmpty() ? "None" : String.join(", ", ordered);
      this.tText(c, "inter", fs, x + 6.0F, y + bh / 2.0F, 17, ordered.isEmpty() ? this.tMuted : this.tFont, this.ellipsize(c, "inter", fs, txt, w - 24.0F));
      this.tArrow(c, x + w - 9.0F, y + bh / 2.0F, isOpen);
      float by = y;
      this.hit(x, y, w, bh, () -> {
         if (isOpen) {
            this.pop = null;
         } else if (!options.isEmpty()) {
            this.openList(id, x, by, w, bh, w, options, "", toggle);
            if (this.pop != null) {
               this.pop.multi = true;
               this.pop.liveSel = live;
            }
         }
      }, null);
      return y + bh + 5.0F;
   }

   private float tInput(Canvas c, String id, String label, float x, float y, float w, String value, String placeholder, Consumer<String> commit) {
      float fs = this.tfs();
      float bh = this.tBoxH();
      this.tText(c, "inter", fs, x, y + this.tLabelH() / 2.0F, 17, this.tFont, this.ellipsize(c, "inter", fs, label, w));
      y += this.tLabelH();
      boolean editing = this.focused != null && id.equals(this.focusId);
      boolean hv = this.in(x, y, w, bh);
      this.tFrame(c, x, y, w, bh, hv || editing, false);
      String t = editing ? this.focused.text : value;
      float avail = w - 12.0F;
      float tWidth = c.textWidth("inter", fs, t);
      float tx0 = x + 6.0F - (editing ? Math.max(0.0F, tWidth - avail) : 0.0F);
      c.save();
      c.scissor(x + 3.0F, y, w - 6.0F, bh);
      if (t.isEmpty() && !editing) {
         this.tText(c, "inter", fs, x + 6.0F, y + bh / 2.0F, 17, this.tMuted, placeholder.isEmpty() ? "…" : placeholder);
      } else {
         this.tText(c, "inter", fs, tx0, y + bh / 2.0F, 17, this.tFont, t);
      }

      if (editing) {
         this.caret(c, "inter", fs, tx0, y + bh / 2.0F, this.focused);
      }

      c.restore();
      this.hit(x, y, w, bh, () -> {
         TextField f = new TextField(64);
         f.set(value);
         this.focusId = id;
         this.focus(f, () -> commit.accept(f.text));
      }, null);
      return y + bh + 5.0F;
   }

   private float tButton(Canvas c, String id, String label, float x, float y, float w, Runnable r) {
      float fs = this.tfs();
      float bh = this.tBoxH();
      boolean hv = this.in(x, y, w, bh);
      this.tFrame(c, x, y, w, bh, hv, true);
      this.tText(c, "inter", fs, x + w / 2.0F, y + bh / 2.0F, 18, hv ? this.tFont : Theme.mix(this.tFont, this.tMuted, 0.15F), this.ellipsize(c, "inter", fs, label, w - 8.0F));
      this.hit(x, y, w, bh, r, null);
      return y + bh + 5.0F;
   }

   private void drawTabsPop(Canvas c) {
      ClickGui.Pop p = this.pop;
      this.tabsPalette();
      float t = Anim.easeOutCubic(Math.min(1.0F, (this.time - p.openedAt) * this.an_speed() / 0.12F));
      this.layer = 1;
      this.noClip();
      c.save();
      c.alpha(Math.min(1.0F, t * 1.8F));
      c.scissor(p.x - 30.0F, p.y - 30.0F, p.w + 60.0F, 30.0F + p.h * t + (t >= 1.0F ? 30 : 0));
      c.shadow(p.x, p.y + 3.0F, p.w, p.h, 0.0F, 12.0F, Theme.a(-16777216, 0.55F));
      c.rect(p.x, p.y, p.w, p.h, 0.0F, this.tBlack);
      c.rect(p.x + 1.0F, p.y + 1.0F, p.w - 2.0F, p.h - 2.0F, 0.0F, this.tOutline);
      c.rect(p.x + 2.0F, p.y + 2.0F, p.w - 4.0F, p.h - 4.0F, 0.0F, this.tMain);
      this.hit(p.x, p.y, p.w, p.h, () -> {}, () -> {});
      if (p.color) {
         this.drawTabsColorPop(c, p);
      } else if (p.ids) {
         this.drawIdsPop(c, p);
      } else {
         this.drawTabsListPop(c, p);
      }

      c.restore();
      this.layer = 0;
   }

   private void drawTabsListPop(Canvas c, ClickGui.Pop p) {
      float rh = this.setRowH();
      float fs = this.tfs();
      List<String> opts = p.options;
      float top = p.y + 2.0F;
      if (opts.size() > 10) {
         float fy = p.y + 3.0F;
         boolean f = this.focused == p.filter;
         this.tFrame(c, p.x + 3.0F, fy, p.w - 6.0F, rh, f, false);
         String ft = p.filter.text;
         this.tText(c, "inter", fs, p.x + 9.0F, fy + rh / 2.0F, 17, ft.isEmpty() ? this.tMuted : this.tFont, ft.isEmpty() ? (f ? "" : "Filter…") : ft);
         if (f) {
            this.caret(c, "inter", fs, p.x + 9.0F, fy + rh / 2.0F, p.filter);
         }

         this.hit(p.x + 3.0F, fy, p.w - 6.0F, rh, () -> this.focus(p.filter, null), null);
         top = fy + rh + 3.0F;
         String q = ft.toLowerCase(Locale.ROOT);
         if (!q.isEmpty()) {
            List<String> fl = new ArrayList<>();

            for (String o : opts) {
               if (display(o).toLowerCase(Locale.ROOT).contains(q)) {
                  fl.add(o);
               }
            }

            opts = fl;
         }
      }

      Collection<String> sel = p.multi && p.liveSel != null ? p.liveSel.get() : List.of(p.current == null ? "" : p.current);
      float viewH = p.y + p.h - 2.0F - top;
      float maxScroll = Math.max(0.0F, opts.size() * rh - viewH);
      p.scrollT = Math.max(0.0F, Math.min(maxScroll, p.scrollT));
      p.scrollY = p.scrollY + (p.scrollT - p.scrollY) * 0.35F;
      c.save();
      c.scissor(p.x + 2.0F, top, p.w - 4.0F, viewH);
      float sX = this.clipX;
      float sY = this.clipY;
      float sW = this.clipW;
      float sH = this.clipH;
      this.clip(p.x + 2.0F, top, p.w - 4.0F, viewH);
      float y = top - p.scrollY;

      for (String o : opts) {
         if (y + rh >= top && y <= top + viewH) {
            boolean on = sel.contains(o);
            boolean hv = this.in(p.x, y, p.w, rh);
            if (hv) {
               c.rect(p.x + 2.0F, y, p.w - 4.0F, rh, 0.0F, Theme.mix(this.tMain, this.tFont, 0.06F));
            }

            this.tText(
               c,
               "inter",
               fs,
               p.x + 7.0F,
               y + rh / 2.0F,
               17,
               on ? this.tAccent : (hv ? this.tFont : Theme.mix(this.tFont, this.tMuted, 0.25F)),
               this.ellipsize(c, "inter", fs, display(o), p.w - 16.0F)
            );
            String fo = o;
            this.hit(p.x, y, p.w, rh, () -> {
               p.pick.accept(fo);
               if (!p.multi) {
                  this.pop = null;
                  this.focused = null;
               }
            }, null);
         }

         y += rh;
      }

      this.clipX = sX;
      this.clipY = sY;
      this.clipW = sW;
      this.clipH = sH;
      c.restore();
      if (maxScroll > 0.0F) {
         float barH = Math.max(12.0F, viewH * viewH / (viewH + maxScroll));
         c.rect(p.x + p.w - 4.0F, top + (viewH - barH) * (p.scrollY / maxScroll), 2.0F, barH, 0.0F, this.tAccent);
      }

      if (opts.isEmpty()) {
         this.tText(c, "inter", fs, p.x + p.w / 2.0F, top + rh / 2.0F + 2.0F, 18, this.tMuted, "No matches");
      }
   }

   static float tcpSquare() {
      return 182.0F;
   }

   float tcpHeight() {
      return 6.0F + tcpSquare() + 6.0F + this.tBoxH() + 4.0F + this.tBoxH() + 6.0F;
   }

   private void drawTabsColorPop(Canvas c, ClickGui.Pop p) {
      float x = p.x + 6.0F;
      float y = p.y + 6.0F;
      float sq = tcpSquare();
      float bh = this.tBoxH();
      float fs = this.tfs();
      float[] hsv = p.hsv;
      int hue = 0xFF000000 | Theme.hsvToRgb(hsv[0], 1.0F, 1.0F);
      c.rect(x - 1.0F, y - 1.0F, sq + 2.0F, sq + 2.0F, 0.0F, this.tBlack);
      c.gradient(x, y, sq, sq, 0.0F, -1, hue, true);
      c.gradient(x, y, sq, sq, 0.0F, 0, -16777216, false);
      float kx = x + hsv[1] * sq;
      float ky = y + (1.0F - hsv[2]) * sq;
      c.rect(kx - 3.0F, ky - 3.0F, 6.0F, 6.0F, 0.0F, -16777216);
      c.rect(kx - 2.0F, ky - 2.0F, 4.0F, 4.0F, 0.0F, -1);
      this.hit(x, y, sq, sq, () -> {
         this.drag2 = pt -> {
            hsv[1] = Math.max(0.0F, Math.min(1.0F, (pt[0] - x) / sq));
            hsv[2] = Math.max(0.0F, Math.min(1.0F, 1.0F - (pt[1] - y) / sq));
            this.commitColor(p);
         };
         this.drag2.accept(new float[]{this.wmx, this.wmy});
      }, null);
      float hx = x + sq + 4.0F;
      float hw = 16.0F;
      c.rect(hx - 1.0F, y - 1.0F, hw + 2.0F, sq + 2.0F, 0.0F, this.tBlack);

      for (int k = 0; k < 6; k++) {
         int a = 0xFF000000 | Theme.hsvToRgb(k / 6.0F, 1.0F, 1.0F);
         int b = 0xFF000000 | Theme.hsvToRgb((k + 1) / 6.0F, 1.0F, 1.0F);
         float sy = y + sq * k / 6.0F;
         c.gradient(hx, sy - (k > 0 ? 0.5F : 0.0F), hw, sq / 6.0F + (k > 0 ? 0.5F : 0.0F), 0.0F, a, b, false);
      }

      float hy = y + hsv[0] * sq;
      c.rect(hx - 1.0F, hy - 2.0F, hw + 2.0F, 4.0F, 0.0F, -16777216);
      c.rect(hx, hy - 1.0F, hw, 2.0F, 0.0F, -1);
      this.hit(hx - 2.0F, y, hw + 4.0F, sq, () -> {
         this.drag2 = pt -> {
            hsv[0] = Math.max(0.0F, Math.min(0.9999F, (pt[1] - y) / sq));
            this.commitColor(p);
         };
         this.drag2.accept(new float[]{this.wmx, this.wmy});
      }, null);
      float fy = y + sq + 6.0F;
      int cur = Theme.hsvToRgb(hsv[0], hsv[1], hsv[2]);
      float full = sq + 4.0F + hw;
      c.rect(x, fy, bh * 1.6F, bh, 0.0F, this.tBlack);
      c.rect(x + 1.0F, fy + 1.0F, bh * 1.6F - 2.0F, bh - 2.0F, 0.0F, 0xFF000000 | cur);
      boolean editing = this.focused == p.filter;
      float ix = x + bh * 1.6F + 4.0F;
      float iw = full - bh * 1.6F - 4.0F;
      this.tFrame(c, ix, fy, iw, bh, editing || this.in(ix, fy, iw, bh), false);
      this.tText(c, "inter", fs, ix + 6.0F, fy + bh / 2.0F, 17, this.tFont, editing ? p.filter.text : Theme.toHex(cur));
      if (editing) {
         this.caret(c, "inter", fs, ix + 6.0F, fy + bh / 2.0F, p.filter);
      }

      this.hit(ix, fy, iw, bh, () -> {
         p.filter.set(Theme.toHex(cur));
         p.filter.allSelected = true;
         this.focus(p.filter, () -> {
            int rgb = Theme.hex(p.filter.text, -1);
            if (rgb >= 0 && p.filter.text.replace("#", "").length() == 6) {
               float[] n = Theme.rgbToHsv(rgb);
               hsv[0] = n[0];
               hsv[1] = n[1];
               hsv[2] = n[2];
               this.set(p.module, p.key, Theme.toHex(rgb));
            }
         });
      }, null);
      float by = fy + bh + 4.0F;
      float bw = (full - 4.0F) / 2.0F;
      String[][] themed = new String[][]{{"theme", "Accent"}, {"theme2", "Accent 2"}};

      for (int k = 0; k < 2; k++) {
         String tv = themed[k][0];
         float bx = x + k * (bw + 4.0F);
         boolean active = tv.equals(str(this.get(p.module, p.key), ""));
         boolean hv = this.in(bx, by, bw, bh);
         this.tFrame(c, bx, by, bw, bh, hv || active, true);
         int tc = k == 0 ? this.th.accent : this.th.accent2;
         c.rect(bx + 6.0F, by + bh / 2.0F - 3.0F, 6.0F, 6.0F, 0.0F, 0xFF000000 | tc);
         this.tText(c, "inter", fs - 0.5F, bx + 16.0F, by + bh / 2.0F, 17, active ? this.tAccent : this.tFont, themed[k][1]);
         this.hit(bx, by, bw, bh, () -> {
            this.set(p.module, p.key, tv);
            this.toast("Colour follows the theme");
         }, null);
      }
   }

   private void drawTabsToasts(Canvas c) {
      this.tabsPalette();
      this.toasts.removeIf(tx -> this.time - tx.at > 2.6F);
      float fs = this.tfs();
      float h = fs + 10.0F;
      float y = 12.0F;

      for (ClickGui.Toast t : this.toasts) {
         float age = this.time - t.at;
         float in = Anim.easeOutCubic(Math.min(1.0F, age / 0.25F));
         float out = 1.0F - Anim.ease((age - 2.2F) / 0.4F);
         float a = Math.max(0.0F, Math.min(in, out));
         float w = c.textWidth("inter", fs, t.text) + 18.0F;
         float x = 12.0F - (1.0F - in) * (w + 20.0F);
         c.save();
         c.alpha(a);
         c.rect(x, y, w, h, 0.0F, this.tBlack);
         c.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, 0.0F, this.tOutline);
         c.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, this.tMain);
         c.rect(x + 1.0F, y + 1.0F, 2.0F, h - 2.0F, 0.0F, this.tAccent);
         this.tText(c, "inter", fs, x + 9.0F, y + h / 2.0F, 17, this.tFont, t.text);
         c.restore();
         y += (h + 4.0F) * a;
      }
   }

   private void drawTabsTooltip(Canvas c, List<String> lines, float a) {
      float fs = this.tfs();
      float lh = fs + 4.0F;
      float w = 0.0F;

      for (String l : lines) {
         w = Math.max(w, c.textWidth("inter", fs, l));
      }

      w += 14.0F;
      float h = lines.size() * lh + 8.0F;
      float x = Math.min(this.mx + 14.0F, this.sw - w - 8.0F);
      float y = Math.min(this.my + 18.0F, this.sh - h - 8.0F);
      c.save();
      c.alpha(a);
      c.rect(x, y, w, h, 0.0F, this.tBlack);
      c.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, 0.0F, this.tOutline);
      c.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, 0.0F, this.tMain);

      for (int i = 0; i < lines.size(); i++) {
         this.tText(c, "inter", fs, x + 7.0F, y + 4.0F + lh / 2.0F + i * lh, 17, this.tFont, lines.get(i));
      }

      c.restore();
   }

   static {
      section(
         "General",
         def("GUI", "select", List.of("Web", "ClickGUI", "Panels", "Tabs"), "ClickGUI"),
         def("Save State", "check", false, null),
         def("Scale", "range", List.of(0.7, 1.4, 1.0, 0.05), null),
         def("Font", "select", List.of(FONTS), "Inter"),
         def("Animation Speed", "range", List.of(0.5, 2.0, 1.0, 0.05), null),
         def("Show Descriptions", "check", true, null),
         def("Page Title", "check", true, null),
         def("Smooth Text", "check", true, null)
      );
      section(
         "Window",
         def("Window Width", "range", List.of(700.0, 1400.0, 1000.0, 10.0), null),
         def("Window Height", "range", List.of(440.0, 900.0, 650.0, 10.0), null),
         def("Rounding", "range", List.of(0.0, 28.0, 16.0, 1.0), null),
         def("Sidebar Width", "range", List.of(170.0, 280.0, 210.0, 2.0), null),
         def("Panel Opacity", "range", List.of(0.3, 1.0, 0.82, 0.01), null),
         def("Border Width", "range", List.of(0.0, 3.0, 1.2, 0.1), null),
         def("Border Opacity", "range", List.of(0.0, 1.0, 0.55, 0.05), null),
         def("Shadow", "range", List.of(0.0, 1.5, 1.0, 0.05), null),
         def("Accent Glow", "check", true, null),
         def("Frosted Panels", "check", true, null)
      );
      section("Logo", def("Logo Tint", "select", List.of("Accent", "Accent 2", "Text", "Original"), "Accent"), def("Logo Glow", "check", true, null));
      section(
         "Modules",
         def("Card Style", "select", List.of("Filled", "Outline", "Flat"), "Filled"),
         def("Card Rounding", "range", List.of(0.0, 24.0, 14.0, 1.0), null),
         def("Card Spacing", "range", List.of(0.0, 24.0, 10.0, 1.0), null),
         def("Card Opacity", "range", List.of(0.0, 1.0, 0.62, 0.01), null),
         def("Enabled Style", "select", List.of("Glow", "Tint", "Text", "None"), "Glow"),
         def("Ripple", "check", true, null),
         def("Hover Lift", "check", true, null)
      );
      section(
         "Controls",
         def("Control Rounding", "range", List.of(0.0, 14.0, 9.0, 1.0), null),
         def("Switch Style", "select", List.of("Pill", "Square", "Minimal"), "Pill"),
         def("Slider Style", "select", List.of("Pill", "Minimal", "Classic"), "Pill")
      );
      section(
         "Background",
         def("Background", "select", List.of("Aurora", "Nebula", "Waves", "Solid", "None"), "Aurora"),
         def("Blur", "range", List.of(0.0, 4.0, 3.0, 1.0), null),
         def("Blur Area", "select", List.of("Everywhere", "Window Only"), "Everywhere"),
         def("Background Dim", "range", List.of(0.0, 0.9, 0.42, 0.01), null, Map.of("showifOr", List.of("Background", List.<String>of(BG_ON))))
      );
      section(
         "Animations",
         def("Open Animation", "select", List.of("Fade", "Pop", "Slide", "Zoom", "CRT"), "Fade"),
         def("Close Animation", "select", List.of("Fade", "Shatter", "Crumble", "Slice", "Implode", "CRT"), "Fade"),
         def("Theme Transition", "select", List.of("Fade", "Ripple", "Wipe", "None"), "Fade"),
         def("Chroma Speed", "range", List.of(0.1, 4.0, 1.0, 0.05), null),
         def("Page Transition", "select", List.of("Slide", "Fade", "None"), "Slide")
      );
      section(
         "Sounds",
         def("UI Sounds", "check", true, null),
         def("UI Volume", "range", List.of(0.0, 1.0, 0.55, 0.05), null, Map.of("showif", List.of("UI Sounds", true))),
         def("Slider Ticks", "check", true, null, Map.of("showif", List.of("UI Sounds", true))),
         def("Social Sounds", "check", true, null),
         def("Social Volume", "range", List.of(0.0, 1.0, 0.8, 0.05), null, Map.of("showif", List.of("Social Sounds", true))),
         def("Chat Message Sound", "check", false, null, Map.of("showif", List.of("Social Sounds", true)))
      );
      section(
         "Panels",
         def("Panel Width", "range", List.of(90.0, 200.0, 118.0, 1.0), null),
         def("Wide Panel Width", "range", List.of(160.0, 360.0, 210.0, 2.0), null),
         def("Panel Chat Height", "range", List.of(140.0, 520.0, 270.0, 10.0), null),
         def("Panel Height", "range", List.of(150.0, 1000.0, 760.0, 10.0), null),
         def("Header Height", "range", List.of(10.0, 28.0, 15.0, 1.0), null),
         def("Row Height", "range", List.of(14.0, 28.0, 18.0, 1.0), null),
         def("Panel Rounding", "range", List.of(0.0, 12.0, 3.0, 1.0), null),
         def("Panel Animation", "select", List.of("Match Open/Close", "Cascade", "Unfold", "Drift", "Spread", "Soft Focus"), "Match Open/Close"),
         def("Body Opacity", "range", List.of(0.3, 1.0, 0.92, 0.01), null),
         def("Header Style", "select", List.of("Filled", "Gradient", "Line"), "Filled"),
         def("Enabled Highlight", "select", List.of("Text", "Fill", "Bar"), "Text"),
         def("Row Dividers", "check", true, null),
         def("Panel Shadow", "check", true, null),
         def("Grid Snap", "check", true, null),
         def("Grid Size", "range", List.of(2.0, 40.0, 8.0, 1.0), null, Map.of("showif", List.of("Grid Snap", true))),
         def("Show Grid", "check", true, null, Map.of("showif", List.of("Grid Snap", true)))
      );
      section(
         "Tabs",
         def("Tabs Width", "range", List.of(460.0, 1200.0, 640.0, 10.0), null),
         def("Tabs Height", "range", List.of(340.0, 900.0, 560.0, 10.0), null),
         def("Tabs Columns", "range", List.of(1.0, 4.0, 2.0, 1.0), null),
         def("Tabs Font", "select", tabsFontOptions(), "JetBrains Mono"),
         def("Tabs Font Size", "range", List.of(9.0, 16.0, 12.0, 0.5), null),
         def("Tabs Palette", "select", List.of("Classic", "Theme"), "Classic"),
         def("Tabs Accent Border", "check", true, null),
         def("Tabs Text Shadow", "check", true, null),
         def("Tabs Window Shadow", "check", true, null),
         def("Tabs Slider Labels", "select", List.of("Above", "Inside"), "Above"),
         def("Tabs Show Settings", "select", List.of("Always", "When Enabled"), "Always")
      );
      section(
         "3D",
         def("Open In 3D", "check", false, null),
         def("3D Distance", "range", List.of(1.2, 4.0, 2.2, 0.05), null),
         def("3D Size", "range", List.of(1.0, 4.0, 1.9, 0.05), null),
         def("3D Tilt", "range", List.of(0.0, 30.0, 6.0, 1.0), null),
         def("3D Follow", "check", false, null),
         def("3D Float", "check", true, null),
         def("3D Shadow", "check", true, null),
         def("3D Always On Top", "check", true, null)
      );
   }

   interface Ctl {
      float draw(float var1, float var2, float var3);
   }

   record Hit(float x, float y, float w, float h, float cx, float cy, float cw, float ch, int layer, Runnable click, Runnable right, Consumer<Float> wheel) {
      boolean contains(float px, float py) {
         return px >= this.x && py >= this.y && px <= this.x + this.w && py <= this.y + this.h && px >= this.cx && py >= this.cy && px <= this.cx + this.cw && py <= this.cy + this.ch;
      }
   }

   interface IntAction {
      void run(int var1);
   }

   final class Panel {
      final String name;
      float x;
      float y;
      float h;
      float scroll;
      float scrollT;
      float maxScroll;
      float contentH;
      boolean open = true;

      Panel(String name, float x, float y) {
         this.name = name;
         this.x = x;
         this.y = y;
      }
   }

   final class Pop {
      final String id;
      final boolean color;
      float x;
      float y;
      float w;
      float h;
      List<String> options;
      String current;
      Consumer<String> pick;
      String module;
      String key;
      float scrollY = 0.0F;
      float scrollT = 0.0F;
      final float openedAt = ClickGui.this.time;
      final TextField filter = new TextField(24);
      float[] hsv;
      boolean ids;
      boolean blocks;
      boolean onlySel;
      String vQuery;
      boolean vOnly;
      List<GuiData.Entry> view = List.of();
      boolean multi;
      Supplier<List<String>> liveSel;

      Pop(String id, boolean color) {
         this.id = id;
         this.color = color;
      }
   }

   record Ripple(String key, float x, float y, float at) {
   }

   private record TBox(String id, float est, ClickGui.TDraw draw) {
   }

   private interface TDraw {
      float draw(Canvas var1, float var2, float var3, float var4);
   }

   private interface TTabBody {
      float draw(Canvas var1, String var2, float var3, float var4, float var5);
   }

   record Toast(String text, float at) {
   }
}
