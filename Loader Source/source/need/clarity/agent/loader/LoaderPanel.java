package need.clarity.agent.loader;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Arc2D.Double;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class LoaderPanel extends JComponent {
   static final String SERVER_WS_URL = "wss://104-156-154-148.sslip.io/load";
   static final int W = 560;
   static final int H = 424;
   static final int M = 14;
   private static final Rectangle CARD = new Rectangle(14, 14, 532, 396);
   private static final Rectangle BTN_MIN = new Rectangle(490, 28, 16, 16);
   private static final Rectangle BTN_CLOSE = new Rectangle(514, 28, 16, 16);
   private static final Rectangle LOGO = new Rectangle(44, 56, 60, 60);
   private static final Rectangle SEL = new Rectangle(44, 164, 422, 64);
   private static final Rectangle REFRESH = new Rectangle(476, 164, 40, 64);
   private static final Rectangle AUTO = new Rectangle(412, 138, 104, 20);
   private static final Rectangle KEY_BOX = new Rectangle(44, 248, 472, 40);
   private static final Rectangle INJECT = new Rectangle(44, 298, 472, 48);
   private static final Rectangle BAR = new Rectangle(44, 366, 472, 6);
   private static final Rectangle LOG = new Rectangle(44, 404, 472, 34);
   private static final int ROW_H = 52;
   private static final Color OK = new Color(10407530);
   private static final Color BAD = new Color(16217742);
   final LoaderTheme theme = new LoaderTheme();
   private final Font fSemi;
   private final Font fReg;
   private final Font fMono;
   private List<Instance> instances = new ArrayList<>();
   private Instance selected;
   private LoaderPanel.Phase phase = LoaderPanel.Phase.IDLE;
   private float shownPct;
   private float targetPct;
   private String statusText = "Ready";
   private Color statusColor = null;
   private final Deque<String[]> log = new ArrayDeque<>();
   private boolean dropdownOpen;
   private float dropdownAnim;
   private boolean autoInject = false;
   private String themeLabel = "Default";
   private boolean hasRemembered;
   private boolean themeFromConfig;
   private final Set<String> autoTried = new HashSet<>();
   private final Map<String, Float> hover = new HashMap<>();
   private String hovered = "";
   private int hoverRow = -1;
   private Point dragFrom;
   private String licenceKey = "";
   private JPasswordField keyField;
   private boolean keyHover;
   private boolean dirty = true;
   private BufferedImage logoMask;
   private BufferedImage logoTinted;
   private int logoTintRgb = -1;
   private final long t0 = System.nanoTime();
   private final InstanceScanner scanner = new InstanceScanner();
   private final ScheduledExecutorService bg = Executors.newScheduledThreadPool(2, r -> {
      Thread t = new Thread(r, "clarity-loader-bg");
      t.setDaemon(true);
      return t;
   });
   private Timer frameTimer;
   private final Runnable onClose;
   private final Runnable onMinimize;
   private final Preferences prefs = Preferences.userRoot().node("need/clarity/loader");
   private int keyThemeKey;
   private BufferedImage cardLayer;
   private int cardKey;
   private final Map<Font, Map<Float, Font>> sizeCache = new HashMap<>();
   private final Map<Font, Font> trackCache = new HashMap<>();
   private int logoTintKey = -1;

   LoaderPanel(Runnable onClose, Runnable onMinimize) {
      this.onClose = onClose;
      this.onMinimize = onMinimize;
      this.fSemi = font("/assets/clarity/fonts/Inter-SemiBold.ttf", "SansSerif", 1);
      this.fReg = font("/assets/clarity/fonts/Inter-Regular.ttf", "SansSerif", 0);
      this.fMono = font("/assets/clarity/fonts/JetBrainsMono-Regular.ttf", "Monospaced", 0);
      this.logoMask = this.loadLogo();

      try {
         this.autoInject = this.prefs.getBoolean("autoInject", false);
      } catch (Exception var5) {
      }

      this.licenceKey = this.prefs.get("licenceKey", "");
      LoaderTheme.Palette remembered = LoaderTheme.fromPref(this.prefs.get("lastTheme", ""));
      if (remembered != null) {
         this.theme.setTarget(remembered);
         this.theme.snap();
         this.themeLabel = remembered.name();
         this.themeFromConfig = true;
         this.hasRemembered = true;
      }

      this.setPreferredSize(new Dimension(560, 424));
      this.setOpaque(true);
      this.setDoubleBuffered(true);
      this.setFocusable(true);
      this.setLayout(null);
      this.buildKeyField();
      MouseAdapter ma = new MouseAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            LoaderPanel.this.onMove(e.getPoint());
         }

         @Override
         public void mouseExited(MouseEvent e) {
            LoaderPanel.this.hovered = "";
            LoaderPanel.this.hoverRow = -1;
            LoaderPanel.this.dirty = true;
         }

         @Override
         public void mousePressed(MouseEvent e) {
            LoaderPanel.this.onPress(e);
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            LoaderPanel.this.onDrag(e);
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            LoaderPanel.this.dragFrom = null;
         }
      };
      this.addMouseListener(ma);
      this.addMouseMotionListener(ma);
   }

   private void buildKeyField() {
      this.keyField = new JPasswordField(this.licenceKey);
      this.styleTextField(this.keyField, true);
      this.keyField.setHorizontalAlignment(0);
      this.keyField.setEchoChar('•');
      this.keyField.setBounds(KEY_BOX.x + 1, KEY_BOX.y + 1, KEY_BOX.width - 2, KEY_BOX.height - 2);
      this.keyField.setToolTipText("CLR-XXXXX-XXXXX-XXXXX-XXXXX");
      this.keyField.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            LoaderPanel.this.syncKey();
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            LoaderPanel.this.syncKey();
         }

         @Override
         public void changedUpdate(DocumentEvent e) {
            LoaderPanel.this.syncKey();
         }
      });
      this.keyField.addActionListener(e -> this.requestFocusInWindow());
      this.keyField.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "leave");
      this.keyField.getActionMap().put("leave", new AbstractAction() {
         @Override
         public void actionPerformed(ActionEvent e) {
            LoaderPanel.this.requestFocusInWindow();
         }
      });
      this.keyField.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            LoaderPanel.this.keyHover = true;
            LoaderPanel.this.refreshKeyMask();
         }

         @Override
         public void mouseExited(MouseEvent e) {
            LoaderPanel.this.keyHover = false;
            LoaderPanel.this.refreshKeyMask();
         }
      });
      this.add(this.keyField);
   }

   private void applyKeyTheme() {
      Color fg = this.licenceKey.isEmpty() ? this.theme.muted() : this.theme.text();
      Color ac = this.theme.accent();
      int k = fg.getRGB() * 31 + ac.getRGB();
      if (k != this.keyThemeKey) {
         this.keyThemeKey = k;
         this.keyField.setForeground(fg);
         this.keyField.setCaretColor(ac);
         this.keyField.setSelectionColor(this.theme.accent(90));
         this.keyField.setSelectedTextColor(fg);
      }
   }

   private void refreshKeyMask() {
      boolean show = this.keyHover || this.keyField.isFocusOwner();
      this.keyField.setEchoChar((char)(show ? '\u0000' : '•'));
      this.dirty = true;
   }

   private void styleTextField(final JTextField f, boolean large) {
      f.setOpaque(false);
      f.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
      f.setForeground(new Color(12634869));
      f.setCaretColor(new Color(8037111));
      f.setFont(this.sz(this.fMono, large ? 13.5F : 12.0F));
      f.setBackground(new Color(0, 0, 0, 0));
      if (f.getDocument().getLength() == 0) {
         f.setForeground(new Color(5660553));
      }

      f.addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            if (f == LoaderPanel.this.keyField) {
               LoaderPanel.this.refreshKeyMask();
            }

            LoaderPanel.this.dirty = true;
         }

         @Override
         public void focusLost(FocusEvent e) {
            if (f == LoaderPanel.this.keyField) {
               LoaderPanel.this.refreshKeyMask();
            }

            LoaderPanel.this.dirty = true;
         }
      });
   }

   private void syncKey() {
      this.licenceKey = new String(this.keyField.getPassword()).trim();
      this.dirty = true;

      try {
         this.prefs.put("licenceKey", this.licenceKey);
      } catch (Exception var2) {
      }
   }

   void start() {
      this.addLog("Loader ready", "info");
      this.bg.execute(() -> {
         LoaderTheme.Palette p = discoverDefaultTheme();
         SwingUtilities.invokeLater(() -> this.applyDiscovered(p));
      });
      this.bg.scheduleWithFixedDelay(() -> {
         List<Instance> found = this.scanner.scan();
         SwingUtilities.invokeLater(() -> this.setInstances(found));
      }, 0L, 2L, TimeUnit.SECONDS);
      this.frameTimer = new Timer(16, e -> this.tick());
      this.frameTimer.start();
   }

   void stop() {
      if (this.frameTimer != null) {
         this.frameTimer.stop();
      }

      this.bg.shutdownNow();
   }

   private void tick() {
      double now = (System.nanoTime() - this.t0) / 1.0E9;
      boolean anim = !this.theme.settled();
      if (anim) {
         this.theme.update(0.14F, now);
      }

      if (this.shownPct != this.targetPct) {
         this.shownPct = this.shownPct + (this.targetPct - this.shownPct) * 0.12F;
         if (Math.abs(this.targetPct - this.shownPct) < 0.05F) {
            this.shownPct = this.targetPct;
         }

         anim = true;
      }

      float dropGoal = this.dropdownOpen ? 1.0F : 0.0F;
      if (this.dropdownAnim != dropGoal) {
         this.dropdownAnim = this.dropdownAnim + (dropGoal - this.dropdownAnim) * 0.3F;
         if (Math.abs(dropGoal - this.dropdownAnim) < 0.01F) {
            this.dropdownAnim = dropGoal;
         }

         anim = true;
      }

      for (Entry<String, Float> en : this.hover.entrySet()) {
         float goal = en.getKey().equals(this.hovered) ? 1.0F : 0.0F;
         float v = en.getValue();
         if (v != goal) {
            v += (goal - v) * 0.25F;
            if (Math.abs(goal - v) < 0.01F) {
               v = goal;
            }

            en.setValue(v);
            anim = true;
         }
      }

      if (this.phase == LoaderPanel.Phase.INJECTING) {
         anim = true;
      }

      if (anim || this.dirty) {
         this.dirty = false;
         this.repaint();
      }
   }

   void setInstances(List<Instance> list) {
      this.dirty = true;
      Set<String> before = new HashSet<>();

      for (Instance i : this.instances) {
         before.add(i.pid);
      }

      for (Instance i : list) {
         if (!before.contains(i.pid)) {
            this.addLog("Found " + i.name + (i.version.isEmpty() ? "" : " " + i.version) + " (PID " + i.pid + ")", "info");
         }
      }

      Set<String> now = new HashSet<>();

      for (Instance i : list) {
         now.add(i.pid);
      }

      for (String p : before) {
         if (!now.contains(p)) {
            this.addLog("Instance PID " + p + " closed", "info");
         }
      }

      this.instances = list;
      Instance keep = null;
      if (this.selected != null) {
         for (Instance i : list) {
            if (i.pid.equals(this.selected.pid)) {
               keep = i;
            }
         }
      }

      if (keep == null) {
         keep = list.isEmpty() ? null : list.get(0);
         if (this.phase != LoaderPanel.Phase.INJECTING) {
            this.phase = LoaderPanel.Phase.IDLE;
            this.setStatus("Ready", null, 0.0F);
         }
      }

      this.select(keep);
      if (this.autoInject && this.phase == LoaderPanel.Phase.IDLE && this.selected != null && this.selected.ready && !this.selected.injected && this.autoTried.add(this.selected.pid)) {
         this.addLog("Auto-inject enabled", "info");
         this.startInject();
      }
   }

   void select(Instance inst) {
      boolean changed = inst == null != (this.selected == null) || inst != null && !inst.pid.equals(this.selected.pid);
      this.selected = inst;
      if (changed) {
         if (inst == null) {
            this.bg.execute(() -> {
               LoaderTheme.Palette p = discoverDefaultTheme();
               SwingUtilities.invokeLater(() -> this.applyDiscovered(p));
            });
         } else {
            Path dir = inst.gameDir;
            this.bg.execute(() -> {
               LoaderTheme.Palette p = LoaderTheme.read(dir);
               SwingUtilities.invokeLater(() -> this.applyPalette(p, inst));
            });
         }
      }
   }

   private void applyDiscovered(LoaderTheme.Palette p) {
      if (p.fromConfig() || !this.hasRemembered) {
         this.applyPalette(p, null);
      }
   }

   void applyPalette(LoaderTheme.Palette p, Instance from) {
      if (p.fromConfig()) {
         this.hasRemembered = true;

         try {
            this.prefs.put("lastTheme", LoaderTheme.toPref(p));
         } catch (Exception var4) {
         }
      }

      this.dirty = true;
      this.theme.setTarget(p);
      this.themeLabel = p.name();
      this.themeFromConfig = p.fromConfig();
      if (from != null) {
         this.addLog(p.fromConfig() ? "Theme \"" + p.name() + "\" read from " + from.name + "'s config" : "No Clarity theme in " + from.name + " yet, using default", "info");
      }
   }

   private static LoaderTheme.Palette discoverDefaultTheme() {
      String home = System.getProperty("user.home", "");
      String appdata = System.getenv("APPDATA");
      List<Path> dirs = new ArrayList<>();
      if (appdata != null) {
         dirs.add(Paths.get(appdata, ".minecraft"));
      }

      dirs.add(Paths.get(home, ".minecraft"));
      dirs.add(Paths.get(home, "Library", "Application Support", "minecraft"));
      List<Path> instRoots = new ArrayList<>();

      for (String l : new String[]{"PrismLauncher", "MultiMC", "PolyMC", "ATLauncher"}) {
         if (appdata != null) {
            instRoots.add(Paths.get(appdata, l, l.equals("ATLauncher") ? "instances" : "instances"));
         }

         instRoots.add(Paths.get(home, ".local", "share", l, "instances"));
         instRoots.add(Paths.get(home, "Library", "Application Support", l, "instances"));
      }

      for (Path root : instRoots) {
         try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path inst : ds) {
               dirs.add(inst.resolve(".minecraft"));
               dirs.add(inst.resolve("minecraft"));
               dirs.add(inst);
            }
         } catch (Exception var15) {
         }
      }

      Path best = null;
      long bestT = -1L;

      for (Path d : dirs) {
         try {
            Path f = d.resolve("clarity_config.json");
            if (Files.isRegularFile(f)) {
               long t = Files.getLastModifiedTime(f).toMillis();
               if (t > bestT) {
                  bestT = t;
                  best = d;
               }
            }
         } catch (Exception var12) {
         }
      }

      if (best != null) {
         LoaderTheme.Palette p = LoaderTheme.read(best);
         if (p.fromConfig()) {
            return p;
         }
      }

      return LoaderTheme.Palette.DEFAULT;
   }

   void setStatus(String text, Color color, float pct) {
      this.statusText = text;
      this.statusColor = color;
      this.targetPct = pct;
      this.dirty = true;
   }

   void setPhase(LoaderPanel.Phase p) {
      this.phase = p;
      this.dirty = true;
   }

   void addLog(String text, String kind) {
      this.log.addLast(new String[]{LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), text, kind});

      while (this.log.size() > 40) {
         this.log.removeFirst();
      }

      this.dirty = true;
   }

   private void startInject() {
      Instance inst = this.selected;
      if (inst != null && this.phase != LoaderPanel.Phase.INJECTING && !inst.injected && inst.ready) {
         String key = this.licenceKey.trim();
         if (key.isEmpty()) {
            this.setStatus("Enter your licence key first", BAD, 0.0F);
            this.addLog("No licence key entered", "err");
            this.phase = LoaderPanel.Phase.FAILED;
         } else {
            this.phase = LoaderPanel.Phase.INJECTING;
            this.shownPct = 0.0F;
            this.targetPct = 2.0F;
            this.statusColor = null;
            this.addLog("Injecting", "info");
            this.bg.execute(() -> {
               Injector inj = new Injector();
               String[] last = new String[]{""};
               Injector.Result r = inj.run(inst, "wss://104-156-154-148.sslip.io/load", key, (pct, rawMsg) -> SwingUtilities.invokeLater(() -> {
                  String msg = generalize(rawMsg);
                  this.setStatus(msg, null, pct.intValue());
                  if (!msg.equals(last[0])) {
                     last[0] = msg;
                     this.addLog(msg, "step");
                  }
               }));
               SwingUtilities.invokeLater(() -> {
                  switch (r) {
                     case OK:
                        inst.injected = true;
                        this.phase = LoaderPanel.Phase.DONE;
                        this.setStatus("Injected successfully", OK, 100.0F);
                        this.addLog("Done. Press the Clarity key in-game to open the menu", "ok");
                        break;
                     case ALREADY:
                        inst.injected = true;
                        this.phase = LoaderPanel.Phase.DONE;
                        this.setStatus("Already injected", OK, 100.0F);
                        this.addLog("Already injected, nothing to do", "ok");
                        break;
                     default:
                        this.phase = LoaderPanel.Phase.FAILED;
                        this.setStatus(inj.error, BAD, 0.0F);
                        this.addLog(inj.error, "err");
                  }
               });
            });
         }
      }
   }

   static String generalize(String m) {
      String s = m == null ? "" : m.toLowerCase(Locale.ROOT);
      if (s.contains("already")) {
         return "Already injected";
      } else if (s.startsWith("injected") || s.equals("done")) {
         return "Injected";
      } else if (s.startsWith("download")) {
         return "Downloading";
      } else if (s.startsWith("connect") || s.startsWith("verif") || s.contains("key exchange") || s.startsWith("sending") || s.startsWith("waiting") || s.startsWith("prepar")) {
         return "Connecting";
      } else {
         return !s.startsWith("attach") && !s.startsWith("check") && !s.startsWith("locat") && !s.startsWith("loading") ? "Injecting" : "Attaching";
      }
   }

   private int rowAt(Point p) {
      if (!this.dropdownOpen) {
         return -1;
      } else {
         int y0 = SEL.y + SEL.height + 6;
         if (p.x >= SEL.x && p.x <= SEL.x + SEL.width) {
            int r = (p.y - y0) / 52;
            return p.y >= y0 && r >= 0 && r < Math.min(this.instances.size(), 4) ? r : -1;
         } else {
            return -1;
         }
      }
   }

   private void onMove(Point p) {
      int prevRow = this.hoverRow;
      this.hoverRow = this.rowAt(p);
      String h = "";
      if (this.hoverRow >= 0) {
         h = "row";
      } else if (this.dropdownOpen && p.y > SEL.y + SEL.height) {
         h = "";
      } else if (BTN_CLOSE.contains(p)) {
         h = "close";
      } else if (BTN_MIN.contains(p)) {
         h = "min";
      } else if (SEL.contains(p)) {
         h = "sel";
      } else if (REFRESH.contains(p)) {
         h = "refresh";
      } else if (AUTO.contains(p)) {
         h = "auto";
      } else if (INJECT.contains(p) && this.canInject()) {
         h = "inject";
      }

      if (!h.equals(this.hovered) || this.hoverRow != prevRow) {
         this.dirty = true;
      }

      this.hovered = h;
      this.hover.putIfAbsent(h, 0.0F);
      this.setCursor(Cursor.getPredefinedCursor(h.isEmpty() ? 0 : 12));
   }

   private void onPress(MouseEvent e) {
      if (this.keyField.isFocusOwner()) {
         this.requestFocusInWindow();
      }

      if (SwingUtilities.isLeftMouseButton(e)) {
         this.dirty = true;
         Point p = e.getPoint();
         int row = this.rowAt(p);
         if (row >= 0) {
            this.select(this.instances.get(row));
            if (this.phase != LoaderPanel.Phase.INJECTING) {
               this.phase = LoaderPanel.Phase.IDLE;
               this.setStatus("Ready", null, 0.0F);
            }

            this.dropdownOpen = false;
         } else {
            if (this.dropdownOpen) {
               this.dropdownOpen = false;
               if (SEL.contains(p)) {
                  return;
               }
            }

            if (BTN_CLOSE.contains(p)) {
               this.onClose.run();
            } else if (BTN_MIN.contains(p)) {
               this.onMinimize.run();
            } else if (SEL.contains(p)) {
               if (this.instances.size() > 1) {
                  this.dropdownOpen = true;
               }
            } else if (REFRESH.contains(p)) {
               this.addLog("Rescanning…", "info");
               this.bg.execute(() -> {
                  List<Instance> f = this.scanner.scan();
                  SwingUtilities.invokeLater(() -> this.setInstances(f));
               });
            } else if (AUTO.contains(p)) {
               this.autoInject = !this.autoInject;

               try {
                  this.prefs.putBoolean("autoInject", this.autoInject);
               } catch (Exception var5) {
               }
            } else if (INJECT.contains(p) && this.canInject()) {
               this.startInject();
            } else {
               if (p.y < 124) {
                  Window w = SwingUtilities.getWindowAncestor(this);
                  if (w != null) {
                     this.dragFrom = new Point(e.getXOnScreen() - w.getX(), e.getYOnScreen() - w.getY());
                  }
               }
            }
         }
      }
   }

   private void onDrag(MouseEvent e) {
      if (this.dragFrom != null) {
         Window w = SwingUtilities.getWindowAncestor(this);
         if (w != null) {
            w.setLocation(e.getXOnScreen() - this.dragFrom.x, e.getYOnScreen() - this.dragFrom.y);
         }
      }
   }

   private boolean canInject() {
      return this.selected != null && this.selected.ready && !this.selected.injected && this.phase != LoaderPanel.Phase.INJECTING && this.phase != LoaderPanel.Phase.DONE;
   }

   @Override
   protected void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D)g0.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      double now = (System.nanoTime() - this.t0) / 1.0E9;
      g.setComposite(AlphaComposite.Src);
      g.setColor(this.theme.bg());
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.setComposite(AlphaComposite.SrcOver);
      this.drawCard(g);
      this.drawWindowButtons(g);
      this.drawHeader(g, now);
      this.drawInstanceSection(g, now);
      this.drawKeySection(g);
      this.drawInjectButton(g, now);
      this.drawProgress(g, now);
      this.drawDropdown(g);
      this.applyKeyTheme();
      g.dispose();
   }

   private void drawCard(Graphics2D g) {
      Color bg = this.theme.bg();
      Color sf = this.theme.surface();
      Color ac = this.theme.accent();
      Color bd = this.theme.border(200);
      int key = ((bg.getRGB() * 31 + sf.getRGB()) * 31 + ac.getRGB()) * 31 + bd.getRGB();
      if (this.cardLayer == null || key != this.cardKey) {
         this.cardKey = key;
         if (this.cardLayer == null) {
            this.cardLayer = new BufferedImage(560, 424, 2);
         }

         Graphics2D c = this.cardLayer.createGraphics();
         c.setComposite(AlphaComposite.Clear);
         c.fillRect(0, 0, 560, 424);
         c.setComposite(AlphaComposite.SrcOver);
         c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         RoundRectangle2D card = new java.awt.geom.RoundRectangle2D.Float(CARD.x, CARD.y, CARD.width, CARD.height, 22.0F, 22.0F);
         c.setPaint(new GradientPaint(0.0F, CARD.y, bg, 0.0F, CARD.y + CARD.height, mixC(bg, sf, 0.55F)));
         c.fill(card);
         Shape oldClip = c.getClip();
         c.clip(card);
         c.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(CARD.x + 70, CARD.y + 60), 360.0F, new float[]{0.0F, 1.0F}, new Color[]{this.theme.accent(40), this.theme.accent(0)}));
         c.fillRect(CARD.x, CARD.y, CARD.width, CARD.height);
         c.setClip(oldClip);
         c.setColor(bd);
         c.setStroke(new BasicStroke(1.0F));
         c.draw(card);
         c.dispose();
      }

      g.drawImage(this.cardLayer, 0, 0, null);
   }

   private void drawWindowButtons(Graphics2D g) {
      float hc = this.hv("close");
      float hm = this.hv("min");
      g.setStroke(new BasicStroke(1.6F, 1, 1));
      Point c = center(BTN_CLOSE);
      Point m = center(BTN_MIN);
      if (hc > 0.02F) {
         g.setColor(withA(BAD, (int)(60.0F * hc)));
         g.fillOval(BTN_CLOSE.x - 2, BTN_CLOSE.y - 2, 20, 20);
      }

      g.setColor(mixC(this.theme.muted(), BAD, hc));
      g.drawLine(c.x - 4, c.y - 4, c.x + 4, c.y + 4);
      g.drawLine(c.x - 4, c.y + 4, c.x + 4, c.y - 4);
      if (hm > 0.02F) {
         g.setColor(this.theme.text((int)(28.0F * hm)));
         g.fillOval(BTN_MIN.x - 2, BTN_MIN.y - 2, 20, 20);
      }

      g.setColor(mixC(this.theme.muted(), this.theme.text(), hm));
      g.drawLine(m.x - 4, m.y, m.x + 4, m.y);
   }

   private void drawHeader(Graphics2D g, double now) {
      Font tf = this.sz(this.fSemi, 36.0F);
      int base = 92;
      int x = 46;
      double sc = g.getTransform().getScaleX();
      if (sc <= 0.0) {
         sc = 1.0;
      }

      Rectangle2D cb = tf.createGlyphVector(g.getFontRenderContext(), "C").getVisualBounds();
      int lh = (int)Math.round(cb.getHeight()) + 2;
      int lw = this.logoMask == null ? 0 : Math.round((float)lh * this.logoMask.getWidth() / this.logoMask.getHeight());
      BufferedImage logo = lw == 0 ? null : this.tintedLogo((int)Math.round(lw * sc), (int)Math.round(lh * sc));
      if (logo != null) {
         g.drawImage(logo, x, base - lh + 1, lw, lh, null);
         this.text(g, "larity", tf, this.theme.text(), x + lw + 3, base, false);
      } else {
         this.text(g, "Clarity", tf, this.theme.text(), x, base, false);
      }

      this.text(g, "Inject", this.sz(this.fReg, 13.0F), this.theme.muted(), x + 1, base + 22, false);
   }

   private void drawInstanceSection(Graphics2D g, double now) {
      this.text(g, "INSTANCE", this.sz(this.fSemi, 10.5F), this.theme.muted(), 46, 152, true);
      float ha = this.hv("auto");
      Font af = this.sz(this.fReg, 12.0F);
      this.text(g, "Auto-inject", af, mixC(this.theme.muted(), this.theme.text(), ha), AUTO.x, AUTO.y + 15, false);
      int tx = AUTO.x + AUTO.width - 34;
      int ty = AUTO.y + 1;
      int tw = 34;
      int th = 18;
      g.setColor(this.autoInject ? this.theme.accent(255) : this.theme.border(255));
      g.fill(new java.awt.geom.RoundRectangle2D.Float(tx, ty, tw, th, th, th));
      g.setColor(this.autoInject ? this.theme.onAccent() : this.theme.muted());
      g.fillOval(tx + (this.autoInject ? tw - th + 3 : 3), ty + 3, th - 6, th - 6);
      float hs = this.hv("sel");
      RoundRectangle2D sel = new java.awt.geom.RoundRectangle2D.Float(SEL.x, SEL.y, SEL.width, SEL.height, 16.0F, 16.0F);
      g.setColor(mixC(this.theme.surface(), this.theme.border(), 0.25F * hs));
      g.fill(sel);
      g.setColor(mixC(this.theme.border(), this.theme.accent(), 0.55F * hs + (this.dropdownOpen ? 0.45F : 0.0F)));
      g.draw(sel);
      if (this.selected == null) {
         this.text(g, "Waiting for Minecraft…", this.sz(this.fSemi, 15.0F), this.theme.text(), SEL.x + 24, SEL.y + 29, false);
         this.text(g, "Launch the game and it shows up here", this.sz(this.fReg, 12.0F), this.theme.muted(), SEL.x + 24, SEL.y + 48, false);
      } else {
         this.drawInstanceRow(g, this.selected, SEL.x, SEL.y, SEL.width, SEL.height, true);
         if (this.instances.size() > 1) {
            this.drawChevron(g, SEL.x + SEL.width - 28, SEL.y + SEL.height / 2, this.dropdownAnim);
         }
      }

      float hr = this.hv("refresh");
      RoundRectangle2D rf = new java.awt.geom.RoundRectangle2D.Float(REFRESH.x, REFRESH.y, REFRESH.width, REFRESH.height, 16.0F, 16.0F);
      g.setColor(mixC(this.theme.surface(), this.theme.border(), 0.25F * hr));
      g.fill(rf);
      g.setColor(mixC(this.theme.border(), this.theme.accent(), 0.55F * hr));
      g.draw(rf);
      Point rc = center(REFRESH);
      g.setColor(mixC(this.theme.muted(), this.theme.accent(), hr));
      g.setStroke(new BasicStroke(1.8F, 1, 1));
      double rot = hr * 90.0F;
      g.draw(new Double(rc.x - 7, rc.y - 7, 14.0, 14.0, 40.0 + rot, 270.0, 0));
      double a = Math.toRadians(40.0 + rot);
      double ax = rc.x + 7.0 * Math.cos(a);
      double ay = rc.y - 7.0 * Math.sin(a);
      Path2D head = new java.awt.geom.Path2D.Double();
      head.moveTo(ax + 4.0, ay - 1.0);
      head.lineTo(ax, ay + 4.0);
      head.lineTo(ax - 3.0, ay - 2.0);
      head.closePath();
      g.fill(head);
   }

   private void drawKeySection(Graphics2D g) {
      this.text(g, "LICENCE KEY", this.sz(this.fSemi, 10.5F), this.theme.muted(), 46, 242, true);
      RoundRectangle2D keyBox = new java.awt.geom.RoundRectangle2D.Float(KEY_BOX.x, KEY_BOX.y, KEY_BOX.width, KEY_BOX.height, 10.0F, 10.0F);
      boolean keyFocused = this.keyField.isFocusOwner();
      g.setColor(this.theme.surface(200));
      g.fill(keyBox);
      g.setColor(keyFocused ? this.theme.accent() : this.theme.border(200));
      g.setStroke(new BasicStroke(keyFocused ? 1.5F : 1.0F));
      g.draw(keyBox);
      if (this.licenceKey.isEmpty()) {
         this.text(g, "CLR-XXXXX-XXXXX-XXXXX-XXXXX", this.sz(this.fMono, 13.5F), this.theme.muted(), KEY_BOX.x + KEY_BOX.width / 2, KEY_BOX.y + 26, true, true);
      }
   }

   private void drawInstanceRow(Graphics2D g, Instance inst, int x, int y, int w, int h, boolean big) {
      int s = big ? 40 : 34;
      int ix = x + 16;
      int iy = y + (h - s) / 2;
      g.setPaint(new GradientPaint(ix, iy, this.theme.accent(), ix + s, iy + s, this.theme.accent2()));
      g.fill(new java.awt.geom.RoundRectangle2D.Float(ix, iy, s, s, 12.0F, 12.0F));
      g.setColor(this.theme.onAccent());
      g.setStroke(new BasicStroke(1.5F, 1, 1));
      int cx = ix + s / 2;
      int cy = iy + s / 2;
      int r = s / 4 + 1;
      Path2D hex = new java.awt.geom.Path2D.Double();

      for (int i = 0; i < 6; i++) {
         double an = (Math.PI / 6) + i * Math.PI / 3.0;
         double px = cx + r * 1.15 * Math.cos(an);
         double py = cy + r * 1.15 * Math.sin(an);
         if (i == 0) {
            hex.moveTo(px, py);
         } else {
            hex.lineTo(px, py);
         }
      }

      hex.closePath();
      g.draw(hex);
      g.drawLine(cx, cy, cx, cy - (int)(r * 1.15));
      g.drawLine(cx, cy, (int)(cx + r * 1.0), (int)(cy + r * 0.58));
      g.drawLine(cx, cy, (int)(cx - r * 1.0), (int)(cy + r * 0.58));
      int tx2 = ix + s + 14;
      int maxw = w - (tx2 - x) - 56;
      String name2 = this.ellipsize(g, inst.name, this.sz(this.fSemi, big ? 15.0F : 14.0F), maxw);
      this.text(g, name2, this.sz(this.fSemi, big ? 15.0F : 14.0F), this.theme.text(), tx2, y + h / 2 - 2, false);
      String sub = inst.subtitle() + (inst.injected ? " · injected" : (!inst.ready ? " · starting" : ""));
      this.text(g, this.ellipsize(g, sub, this.sz(this.fMono, 11.0F), maxw), this.sz(this.fMono, 11.0F), inst.injected ? OK : this.theme.muted(), tx2, y + h / 2 + 15, false);
   }

   private void drawChevron(Graphics2D g, int cx, int cy, float open) {
      g.setColor(this.theme.muted());
      g.setStroke(new BasicStroke(1.8F, 1, 1));
      Path2D p = new java.awt.geom.Path2D.Double();
      if (open > 0.5F) {
         p.moveTo(cx - 5, cy + 2);
         p.lineTo(cx, cy - 3);
         p.lineTo(cx + 5, cy + 2);
      } else {
         p.moveTo(cx - 5, cy - 2);
         p.lineTo(cx, cy + 3);
         p.lineTo(cx + 5, cy - 2);
      }

      g.draw(p);
   }

   private void drawInjectButton(Graphics2D g, double now) {
      boolean enabled = this.canInject();
      float h = this.hv("inject");
      Rectangle r = INJECT;
      RoundRectangle2D shape = new java.awt.geom.RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 16.0F, 16.0F);
      boolean busy = this.phase == LoaderPanel.Phase.INJECTING;
      boolean done = this.phase == LoaderPanel.Phase.DONE || this.selected != null && this.selected.injected;
      String label = this.selected == null
         ? "Waiting for Minecraft"
         : (busy ? "Injecting" : (done ? "Injected" : (!this.selected.ready ? "Waiting for game window" : (this.phase == LoaderPanel.Phase.FAILED ? "Retry" : "Inject"))));
      if (enabled) {
         for (int i = 8; i >= 1; i--) {
            g.setColor(this.theme.accent((int)((6.0F + 10.0F * h) * (9 - i) / 8.0F)));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x - i, r.y - i + 4, r.width + 2 * i, r.height + 2 * i, 16 + i, 16 + i));
         }

         g.setPaint(new GradientPaint(r.x, r.y, lighten(this.theme.accent(), 0.1F * h), r.x + r.width, r.y + r.height, this.theme.accent2()));
         g.fill(shape);
         g.setColor(new Color(255, 255, 255, (int)(24.0F + 26.0F * h)));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x, r.y, r.width, r.height / 2.0F, 16.0F, 16.0F));
         this.text(g, label, this.sz(this.fSemi, 16.0F), this.theme.onAccent(), r.x + r.width / 2, r.y + r.height / 2 + 6, true, true);
      } else if (busy) {
         g.setPaint(new GradientPaint(r.x, r.y, this.theme.accent(150), r.x + r.width, r.y, this.theme.accent2(150)));
         g.fill(shape);
         Shape old = g.getClip();
         g.clip(shape);
         float sx = (float)(now * 220.0 % (r.width + 160)) - 80.0F;
         g.setPaint(new GradientPaint(r.x + sx, r.y, new Color(255, 255, 255, 0), r.x + sx + 60.0F, r.y, new Color(255, 255, 255, 60)));
         g.fillRect(r.x, r.y, r.width, r.height);
         g.setClip(old);
         this.text(g, label + dots(now), this.sz(this.fSemi, 16.0F), this.theme.onAccent(), r.x + r.width / 2 - 8, r.y + r.height / 2 + 6, true, true);
      } else {
         g.setColor(done ? withA(OK, 38) : this.theme.surface());
         g.fill(shape);
         g.setColor(done ? withA(OK, 120) : this.theme.border());
         g.draw(shape);
         Color tc = done ? OK : this.theme.muted();
         if (done) {
            int ocx = r.x + r.width / 2 - 38;
            int ocy = r.y + r.height / 2;
            g.setColor(OK);
            g.setStroke(new BasicStroke(2.2F, 1, 1));
            g.drawLine(ocx - 6, ocy, ocx - 2, ocy + 4);
            g.drawLine(ocx - 2, ocy + 4, ocx + 6, ocy - 5);
            this.text(g, label, this.sz(this.fSemi, 16.0F), tc, ocx + 14, ocy + 6, false);
         } else {
            this.text(g, label, this.sz(this.fSemi, 15.0F), tc, r.x + r.width / 2, r.y + r.height / 2 + 5, true, true);
         }
      }
   }

   private void drawProgress(Graphics2D g, double now) {
      if (this.phase != LoaderPanel.Phase.IDLE) {
         Rectangle r = BAR;
         boolean showBar = this.phase == LoaderPanel.Phase.INJECTING || this.phase == LoaderPanel.Phase.DONE;
         if (showBar) {
            g.setColor(this.theme.border(255));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x, r.y, r.width, r.height, r.height, r.height));
         }

         float pct = Math.max(0.0F, Math.min(100.0F, this.shownPct));
         int filled = (int)(r.width * pct / 100.0F);
         if (showBar && filled > 0) {
            Color c1 = this.phase == LoaderPanel.Phase.FAILED ? BAD : this.theme.accent();
            Color c2 = this.phase == LoaderPanel.Phase.FAILED ? BAD : this.theme.accent2();

            for (int i = 3; i >= 1; i--) {
               g.setColor(withA(c1, 14));
               g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x, r.y - i * 1.5F, filled, r.height + i * 3, r.height + i * 3, r.height + i * 3));
            }

            g.setPaint(new GradientPaint(r.x, 0.0F, c2, r.x + Math.max(filled, 1), 0.0F, c1));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x, r.y, filled, r.height, r.height, r.height));
         }

         boolean busy = this.phase == LoaderPanel.Phase.INJECTING;
         int tx = r.x;
         if (busy) {
            g.setColor(this.theme.accent());
            g.setStroke(new BasicStroke(2.0F, 1, 1));
            double a = now * 300.0;
            g.draw(new Double(r.x, r.y + 12, 12.0, 12.0, -a, 260.0, 0));
            tx += 20;
         }

         Color sc = this.statusColor != null ? this.statusColor : this.theme.text();
         this.text(g, this.ellipsize(g, this.statusText, this.sz(this.fSemi, 13.0F), r.width - 70), this.sz(this.fSemi, 13.0F), sc, tx, r.y + 29, false);
         if (busy || this.phase == LoaderPanel.Phase.DONE) {
            this.text(
               g,
               Math.round(pct) + "%",
               this.sz(this.fMono, 12.0F),
               this.theme.muted(),
               r.x + r.width - g.getFontMetrics(this.sz(this.fMono, 12.0F)).stringWidth(Math.round(pct) + "%"),
               r.y + 29,
               false
            );
         }
      }
   }

   private void drawDropdown(Graphics2D g) {
      if (!(this.dropdownAnim < 0.03F) && this.instances.size() >= 2) {
         int count = Math.min(this.instances.size(), 4);
         int y0 = SEL.y + SEL.height + 6;
         float a = Math.min(1.0F, this.dropdownAnim);
         int h = count * 52 + 8;

         for (int i = 8; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, (int)(a * (4 + (8 - i) * 2))));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(SEL.x - i, y0 - i + 4, SEL.width + 2 * i, h + 2 * i, 16 + i, 16 + i));
         }

         RoundRectangle2D box = new java.awt.geom.RoundRectangle2D.Float(SEL.x, y0, SEL.width, h, 16.0F, 16.0F);
         g.setColor(mixC(this.theme.surface(), this.theme.bg(), 0.15F));
         g.fill(box);
         g.setColor(this.theme.border());
         g.draw(box);

         for (int i = 0; i < count; i++) {
            int ry = y0 + 4 + i * 52;
            Instance it = this.instances.get(i);
            boolean hot = i == this.hoverRow;
            boolean cur = this.selected != null && it.pid.equals(this.selected.pid);
            if (hot || cur) {
               g.setColor(hot ? this.theme.accent(34) : this.theme.accent(18));
               g.fill(new java.awt.geom.RoundRectangle2D.Float(SEL.x + 4, ry, SEL.width - 8, 52.0F, 12.0F, 12.0F));
            }

            this.drawInstanceRow(g, it, SEL.x, ry, SEL.width, 52, false);
         }
      }
   }

   private Font sz(Font base, float size) {
      return this.sizeCache.computeIfAbsent(base, k -> new HashMap<>()).computeIfAbsent(size, base::deriveFont);
   }

   private float hv(String k) {
      return this.hover.getOrDefault(k, 0.0F);
   }

   private static Point center(Rectangle r) {
      return new Point(r.x + r.width / 2, r.y + r.height / 2);
   }

   private void text(Graphics2D g, String s, Font f, Color c, int x, int y, boolean spaced) {
      this.text(g, s, f, c, x, y, false, false, spaced);
   }

   private void text(Graphics2D g, String s, Font f, Color c, int x, int y, boolean centered, boolean unused) {
      this.text(g, s, f, c, x, y, centered, false, false);
   }

   private void text(Graphics2D g, String s, Font f, Color c, int x, int y, boolean centered, boolean unused2, boolean spaced) {
      if (spaced) {
         Font src = f;
         f = this.trackCache.computeIfAbsent(src, k -> k.deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.12F)));
      }

      g.setFont(f);
      g.setColor(c);
      int w = g.getFontMetrics().stringWidth(s);
      g.drawString(s, centered ? x - w / 2 : x, y);
   }

   private String ellipsize(Graphics2D g, String s, Font f, int maxW) {
      FontMetrics fm = g.getFontMetrics(f);
      if (fm.stringWidth(s) <= maxW) {
         return s;
      }

      while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) {
         s = s.substring(0, s.length() - 1);
      }

      return s + "…";
   }

   private static String dots(double now) {
      return ".".repeat(1 + (int)(now * 2.5) % 3);
   }

   private static Color withA(Color c, int a) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
   }

   private static Color mixC(Color a, Color b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return new Color(
         Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
         Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
         Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
         Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
      );
   }

   private static Color lighten(Color c, float t) {
      return mixC(c, Color.WHITE, t);
   }

   private BufferedImage tintedLogo(int pxW, int pxH) {
      if (this.logoMask != null && pxW >= 1 && pxH >= 1) {
         Color a = this.theme.accent();
         int rgb = a.getRed() >> 2 << 16 | a.getGreen() >> 2 << 8 | a.getBlue() >> 2;
         int key = rgb * 31 + pxW * 7 + pxH;
         if (this.logoTinted != null && key == this.logoTintKey) {
            return this.logoTinted;
         }

         this.logoTintKey = key;
         Color tint = mixC(a, Color.WHITE, 0.35F);
         BufferedImage out = scaleSmooth(this.logoMask, pxW, pxH);
         Graphics2D gg = out.createGraphics();
         gg.setComposite(AlphaComposite.SrcIn);
         gg.setColor(tint);
         gg.fillRect(0, 0, out.getWidth(), out.getHeight());
         gg.dispose();
         this.logoTinted = out;
         return out;
      } else {
         return null;
      }
   }

   private static BufferedImage scaleSmooth(BufferedImage src, int w, int h) {
      BufferedImage cur = src;

      while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) {
         cur = scaleOnce(cur, cur.getWidth() / 2, cur.getHeight() / 2);
      }

      return scaleOnce(cur, w, h);
   }

   private static BufferedImage scaleOnce(BufferedImage src, int w, int h) {
      BufferedImage out = new BufferedImage(w, h, 2);
      Graphics2D gg = out.createGraphics();
      gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gg.drawImage(src, 0, 0, w, h, null);
      gg.dispose();
      return out;
   }

   private void drawFallbackMark(Graphics2D g, Rectangle r) {
      g.setPaint(new GradientPaint(r.x, r.y, this.theme.accent(), r.x + r.width, r.y + r.height, this.theme.accent2()));
      g.fill(new java.awt.geom.RoundRectangle2D.Float(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 18.0F, 18.0F));
      this.text(g, "C", this.sz(this.fSemi, 30.0F), this.theme.onAccent(), r.x + r.width / 2, r.y + r.height / 2 + 11, true, true);
   }

   private BufferedImage loadLogo() {
      for (String p : new String[]{"/assets/clarity/textures/fx/clarity_logo_clean.png", "/assets/clarity/textures/fx/clarity_logo.png", "/assets/clarity/icon.png"}) {
         try (InputStream in = LoaderPanel.class.getResourceAsStream(p)) {
            if (in != null) {
               BufferedImage src = ImageIO.read(in);
               if (src != null) {
                  int w = src.getWidth();
                  int h = src.getHeight();
                  int x0 = w;
                  int y0 = h;
                  int x1 = -1;
                  int y1 = -1;

                  for (int y = 0; y < h; y++) {
                     for (int x = 0; x < w; x++) {
                        if (src.getRGB(x, y) >>> 24 > 24) {
                           if (x < x0) {
                              x0 = x;
                           }

                           if (x > x1) {
                              x1 = x;
                           }

                           if (y < y0) {
                              y0 = y;
                           }

                           if (y > y1) {
                              y1 = y;
                           }
                        }
                     }
                  }

                  if (x1 >= 0) {
                     BufferedImage out = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, 2);
                     Graphics2D gg = out.createGraphics();
                     gg.drawImage(src, -x0, -y0, null);
                     gg.dispose();
                     return out;
                  }
               }
            }
         } catch (Exception var18) {
         }
      }

      return null;
   }

   private static Font font(String res, String fallback, int style) {
      try (InputStream in = LoaderPanel.class.getResourceAsStream(res)) {
         if (in != null) {
            return Font.createFont(0, in).deriveFont(13.0F);
         }
      } catch (Exception var8) {
      }

      return new Font(fallback, style, 13);
   }

   void debugSnap() {
      this.theme.snap();
      this.shownPct = this.targetPct;
      this.dropdownAnim = this.dropdownOpen ? 1.0F : 0.0F;
      this.dirty = true;
   }

   void debugOpenDropdown(boolean open) {
      this.dropdownOpen = open;
   }

   enum Phase {
      IDLE,
      INJECTING,
      DONE,
      FAILED;
   }
}
