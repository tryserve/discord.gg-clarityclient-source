package need.clarity.agent;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D.Double;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class Attach {
   private static final Color C_ACCENT = new Color(94, 168, 255);
   private static final Color C_BG = new Color(10, 10, 10);
   private static final Color C_BORDER = new Color(32, 48, 58);
   private static final Color C_TEXT_MUT = new Color(72, 88, 106);
   private static final String[] MC_KEYS = new String[]{"minecraft", "net.minecraft", "fabricmc", "fabriclauncher", "launchwrapper", "knotclassloader"};
   private JFrame frame;
   private JProgressBar bar;
   private Timer barTimer;
   private Timer pollTimer;
   private volatile boolean injecting = false;
   private volatile int barTarget = 0;

   public static void main(String[] args) throws Exception {
      if (args.length >= 1) {
         headlessAttach(args[0]);
      } else {
         SwingUtilities.invokeLater(new Attach()::launch);
      }
   }

   private void launch() {
      try {
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception var6) {
      }

      this.frame = new JFrame("Clarity");
      this.frame.setUndecorated(true);
      this.frame.setDefaultCloseOperation(3);
      this.frame.setSize(360, 200);
      this.frame.setResizable(false);
      this.frame.setLocationRelativeTo(null);
      this.frame.getContentPane().setBackground(C_BG);
      JPanel root = new JPanel(new GridBagLayout());
      root.setBackground(C_BG);
      JPanel inner = new JPanel();
      inner.setBackground(C_BG);
      inner.setLayout(new BoxLayout(inner, 1));
      JLabel title = new JLabel("Clarity");
      title.setFont(new Font("Inter", 1, 32));
      title.setForeground(C_ACCENT);
      title.setAlignmentX(0.5F);
      this.bar = new JProgressBar(0, 100);
      this.bar.setValue(0);
      this.bar.setStringPainted(false);
      this.bar.setOpaque(false);
      this.bar.setBorderPainted(false);
      this.bar.setMaximumSize(new Dimension(260, 10));
      this.bar.setPreferredSize(new Dimension(260, 10));
      this.bar.setMinimumSize(new Dimension(260, 10));
      this.bar.setAlignmentX(0.5F);
      this.bar.setUI(new BasicProgressBarUI() {
         @Override
         protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth();
            int h = c.getHeight();
            int barH = 4;
            int barY = (h - barH) / 2;
            double pct = (double)Attach.this.bar.getValue() / Attach.this.bar.getMaximum();
            int filled = (int)(w * pct);
            g2.setColor(Attach.C_BORDER);
            g2.fillRoundRect(0, barY, w, barH, barH, barH);
            if (filled > 0) {
               g2.setColor(new Color(94, 168, 255, 28));
               g2.fillRoundRect(0, barY - 3, filled, barH + 6, barH + 6, barH + 6);
               g2.setColor(new Color(94, 168, 255, 50));
               g2.fillRoundRect(0, barY - 1, filled, barH + 2, barH + 2, barH + 2);
               g2.setColor(Attach.C_ACCENT);
               g2.fillRoundRect(0, barY, filled, barH, barH, barH);
            }

            g2.dispose();
         }

         @Override
         protected void paintIndeterminate(Graphics g, JComponent c) {
            this.paintDeterminate(g, c);
         }
      });
      inner.add(title);
      inner.add(Box.createVerticalStrut(22));
      inner.add(this.bar);
      root.add(inner);
      this.frame.setContentPane(root);
      this.frame.setVisible(true);

      try {
         this.frame.setShape(new Double(0.0, 0.0, 360.0, 200.0, 18.0, 18.0));
      } catch (Exception var5) {
      }

      this.barTimer = new Timer(16, e -> {
         int cur = this.bar.getValue();
         int tgt = this.barTarget;
         if (cur < tgt) {
            int step = Math.max(1, (tgt - cur) / 5);
            int next = Math.min(cur + step, tgt);
            this.bar.setValue(next);
            if (next >= 100) {
               this.barTimer.stop();
               new Timer(600, ev -> this.frame.dispose()) {
                  {
                     this.setRepeats(false);
                     this.start();
                  }
               };
            }
         }
      });
      this.pollTimer = new Timer(1000, e -> this.pollAndInject());
      this.pollTimer.setInitialDelay(0);
      this.pollTimer.start();
   }

   private void pollAndInject() {
      if (!this.injecting) {
         new Thread(() -> {
            List<Map<String, String>> found = scanJvms();
            if (!found.isEmpty()) {
               this.injecting = true;
               String pid = found.get(0).get("pid");
               String jar = selfJar();
               SwingUtilities.invokeLater(() -> {
                  this.pollTimer.stop();
                  this.barTarget = 60;
                  this.bar.setValue(0);
                  this.barTimer.start();
               });
               boolean ok = false;

               try {
                  Thread.sleep(400L);
                  VirtualMachine vm = VirtualMachine.attach(pid);
                  vm.loadAgent(jar);
                  vm.detach();
                  ok = true;
               } catch (Exception var6) {
               }

               boolean success = ok;
               SwingUtilities.invokeLater(() -> {
                  if (success) {
                     this.barTarget = 100;
                  } else {
                     this.barTimer.stop();
                     this.bar.setValue(0);
                     this.barTarget = 0;
                     this.injecting = false;
                     this.pollTimer.start();
                  }
               });
            }
         }, "clarity-inject").start();
      }
   }

   private static List<Map<String, String>> scanJvms() {
      List<Map<String, String>> out = new ArrayList<>();
      String own = String.valueOf(ProcessHandle.current().pid());

      List<VirtualMachineDescriptor> vms;
      try {
         vms = VirtualMachine.list();
      } catch (Exception e) {
         return out;
      }

      for (VirtualMachineDescriptor d : vms) {
         if (!d.id().equals(own) && mcMatch(d.displayName())) {
            out.add(row(d.id(), d.displayName()));
         }
      }

      if (!out.isEmpty()) {
         return out;
      }

      for (VirtualMachineDescriptor d : vms) {
         if (!d.id().equals(own)) {
            try {
               VirtualMachine vm = VirtualMachine.attach(d.id());

               try {
                  Properties p = vm.getSystemProperties();
                  String cp = p.getProperty("java.class.path", "");
                  String dir = p.getProperty("user.dir", "");
                  if (mcMatch(cp) || mcMatch(dir) || mcMatch(d.displayName())) {
                     out.add(row(d.id(), d.displayName().isEmpty() ? cp : d.displayName()));
                  }
               } finally {
                  vm.detach();
               }
            } catch (Exception var15) {
            }
         }
      }

      return out;
   }

   private static boolean mcMatch(String s) {
      if (s == null) {
         return false;
      }

      String lo = s.toLowerCase(Locale.ROOT);

      for (String k : MC_KEYS) {
         if (lo.contains(k)) {
            return true;
         }
      }

      return false;
   }

   private static Map<String, String> row(String pid, String display) {
      Map<String, String> m = new LinkedHashMap<>();
      m.put("pid", pid);
      m.put("display", display);
      return m;
   }

   private static String selfJar() {
      try {
         File f = new File(Attach.class.getProtectionDomain().getCodeSource().getLocation().toURI());
         if (f.isFile() && f.getName().endsWith(".jar")) {
            return f.getAbsolutePath();
         }
      } catch (URISyntaxException | NullPointerException var1) {
      }

      return "";
   }

   private static void headlessAttach(String pid) throws Exception {
      String jar = selfJar();
      if (jar.isEmpty()) {
         System.err.println("[Clarity] Could not locate agent JAR.");
         System.exit(1);
      }

      VirtualMachine vm = VirtualMachine.attach(pid);

      try {
         vm.loadAgent(jar);
      } finally {
         vm.detach();
      }
   }
}
