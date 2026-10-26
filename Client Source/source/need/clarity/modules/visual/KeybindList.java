package need.clarity.modules.visual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.util.ThemeColors;

public class KeybindList extends ModuleBase {
   public static KeybindList INSTANCE;

   public KeybindList() {
      super("Keybind List", "HUD");
      INSTANCE = this;
   }

   public String anchor() {
      return this.getSetting("Anchor", "Top Left");
   }

   public int offsetX() {
      return this.getSetting("X Offset", 12);
   }

   public int offsetY() {
      return this.getSetting("Y Offset", 48);
   }

   public float scale() {
      return Math.max(0.5F, Math.min(2.0F, this.getSetting("Scale", 1.0F)));
   }

   public float opacity() {
      return Math.max(0.1F, Math.min(1.0F, this.getSetting("Opacity", 1.0F)));
   }

   public float textSize() {
      return Math.max(8.0F, Math.min(18.0F, this.getSetting("Text Size", 12.0F)));
   }

   public float rowHeight() {
      return Math.max(12.0F, this.getSetting("Row Height", 20.0F));
   }

   public float rowGap() {
      return Math.max(0.0F, this.getSetting("Row Gap", 0.0F));
   }

   public float padding() {
      return Math.max(2.0F, this.getSetting("Padding", 10.0F));
   }

   public float minWidth() {
      return Math.max(0.0F, this.getSetting("Min Width", 0.0F));
   }

   public float cornerRadius() {
      return Math.max(0.0F, this.getSetting("Corner Radius", 8.0F));
   }

   public String sort() {
      return this.getSetting("Sort", "Name");
   }

   public boolean onlyEnabled() {
      return this.getSetting("Only Enabled", false);
   }

   public int maxRows() {
      return Math.max(1, this.getSetting("Max Rows", 20));
   }

   public boolean showTitle() {
      return this.getSetting("Show Title", true);
   }

   public String titleText() {
      return this.getSetting("Title Text", "Keybinds");
   }

   public boolean titleDivider() {
      return this.getSetting("Title Divider", true);
   }

   public String keyStyle() {
      return this.getSetting("Key Style", "Pill");
   }

   public boolean upperKeys() {
      return this.getSetting("Uppercase Keys", true);
   }

   public String keyAlign() {
      return this.getSetting("Key Align", "Right");
   }

   public boolean showMode() {
      return this.getSetting("Show Mode", false);
   }

   public boolean stateDot() {
      return this.getSetting("State Dot", true);
   }

   public boolean rowHighlight() {
      return this.getSetting("Row Highlight", false);
   }

   public boolean dimDisabled() {
      return this.getSetting("Dim Disabled", true);
   }

   public boolean background() {
      return this.getSetting("Background", true);
   }

   public float bgOpacity() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Background Opacity", 0.92F)));
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean highlight() {
      return this.getSetting("Highlight", true);
   }

   public boolean blur() {
      return this.getSetting("Blur", false);
   }

   public int blurStrength() {
      return Math.max(1, Math.min(4, this.getSetting("Blur Strength", 2)));
   }

   public float blurTint() {
      return Math.max(0.0F, Math.min(1.0F, this.getSetting("Blur Tint", 0.45F)));
   }

   public boolean useTheme() {
      return this.getSetting("Theme Colors", true);
   }

   public int nameColor() {
      return ThemeColors.resolveRgb(this.getSetting("Name Color", ""), -1184272, false) & 16777215;
   }

   public int keyColor() {
      return ThemeColors.resolveRgb(this.getSetting("Key Color", ""), -8740105, false) & 16777215;
   }

   public int bgColor() {
      return ThemeColors.resolveRgb(this.getSetting("Background Color", ""), -15328474, false) & 16777215;
   }

   public int enabledColor() {
      return ThemeColors.resolveRgb(this.getSetting("Enabled Color", ""), -12845218, false) & 16777215;
   }

   public int disabledColor() {
      return ThemeColors.resolveRgb(this.getSetting("Disabled Color", ""), -50373, false) & 16777215;
   }

   public List<KeybindList.Row> previewRows() {
      List<KeybindList.Row> out = new java.util.ArrayList<>();
      out.add(new KeybindList.Row("Kill Aura", "R", "toggle", true));
      out.add(new KeybindList.Row("Sprint", "G", "toggle", true));
      out.add(new KeybindList.Row("Zoom", "C", "hold", false));
      return out;
   }

   public List<KeybindList.Row> rows() {
      Map<String, String> binds = ClarityConfig.getKeybinds();
      boolean only = this.onlyEnabled();
      List<KeybindList.Row> out = new java.util.ArrayList<>();

      for (Entry<String, String> e : binds.entrySet()) {
         String name = e.getKey();
         ModuleBase m = ModuleRegistry.get(name);
         boolean on = m != null ? m.isEnabled() : ClarityConfig.isEnabled(name);
         if (!only || on) {
            out.add(new KeybindList.Row(name, e.getValue(), bindMode(name), on));
         }
      }

      String sort = this.sort();
      Comparator<KeybindList.Row> byName = (a, b) -> a.name.compareToIgnoreCase(b.name);
      switch (sort) {
         case "Key":
            out.sort((a, b) -> {
               int c = a.key.compareToIgnoreCase(b.key);
               return c != 0 ? c : byName.compare(a, b);
            });
            break;
         case "Length":
            out.sort((a, b) -> {
               int c = Integer.compare(b.name.length(), a.name.length());
               return c != 0 ? c : byName.compare(a, b);
            });
            break;
         case "Enabled First":
            out.sort((a, b) -> {
               int c = Boolean.compare(b.on, a.on);
               return c != 0 ? c : byName.compare(a, b);
            });
            break;
         default:
            out.sort(byName);
      }

      int max = this.maxRows();
      return out.size() > max ? new java.util.ArrayList<>(out.subList(0, max)) : out;
   }

   private static String bindMode(String moduleName) {
      try {
         JsonObject mod = ClarityConfig.getCurrent().getAsJsonObject(moduleName);
         if (mod == null) {
            return "toggle";
         }

         JsonElement el = mod.get("bindMode");
         if (el != null && el.isJsonPrimitive()) {
            return el.getAsString().toLowerCase(Locale.ROOT);
         }
      } catch (Exception var3) {
      }

      return "toggle";
   }

   public static final class Row {
      public final String name;
      public final String key;
      public final String mode;
      public final boolean on;

      Row(String name, String key, String mode, boolean on) {
         this.name = name;
         this.key = key;
         this.mode = mode;
         this.on = on;
      }
   }
}
