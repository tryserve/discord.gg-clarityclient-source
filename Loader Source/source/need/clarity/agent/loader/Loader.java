package need.clarity.agent.loader;

import java.awt.Color;
import java.awt.geom.RoundRectangle2D.Double;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import need.clarity.agent.Attach;

public final class Loader {
   private static LoaderPanel panel0;

   private Loader() {
   }

   public static void main(String[] args) throws Exception {
      if (args.length >= 1) {
         Attach.main(args);
      } else {
         System.setProperty("awt.useSystemAAFontSettings", "on");
         System.setProperty("swing.aatext", "true");
         SwingUtilities.invokeLater(Loader::show);
      }
   }

   private static void show() {
      JFrame f = new JFrame("Clarity");
      f.setUndecorated(true);
      f.setDefaultCloseOperation(0);
      f.setResizable(false);
      LoaderPanel panel = new LoaderPanel(() -> {
         panel0.stop();
         f.dispose();
         System.exit(0);
      }, () -> f.setState(1));
      panel0 = panel;
      f.setContentPane(panel);
      f.pack();
      f.setBackground(new Color(724243));
      f.getContentPane().setBackground(new Color(724243));

      try {
         f.setShape(new Double(14.0, 14.0, 532.0, 396.0, 22.0, 22.0));
      } catch (Throwable var4) {
      }

      try {
         f.setIconImage(ImageIO.read(Loader.class.getResourceAsStream("/assets/clarity/icon.png")));
      } catch (Exception var3) {
      }

      f.setLocationRelativeTo(null);
      f.setVisible(true);
      panel.start();
      panel.requestFocusInWindow();
   }
}
