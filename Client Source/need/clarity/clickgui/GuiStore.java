package need.clarity.clickgui;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface GuiStore {
   boolean isEnabled(String var1);

   void setEnabled(String var1, boolean var2);

   Object value(String var1, String var2);

   void setValue(String var1, String var2, Object var3);

   void resetModule(String var1);

   String bind(String var1);

   String bindMode(String var1);

   void setBind(String var1, String var2, String var3);

   Object gui(String var1);

   void setGui(String var1, Object var2);

   Map<String, String> theme();

   String themeName();

   void applyTheme(String var1, List<String> var2);

   boolean themeColorsEnabled();

   void setThemeColorsEnabled(boolean var1);

   List<String> profiles();

   String activeProfile();

   void loadProfile(String var1);

   void saveProfile(String var1);

   void deleteProfile(String var1);

   List<String> bindSets();

   void loadBindSet(String var1);

   void saveBindSet(String var1);

   void deleteBindSet(String var1);

   Map<String, String> serverProfiles();

   void mapServer(String var1, String var2);

   void unmapServer(String var1);

   String currentServer();

   List<String> customSkins();

   default String addCustomSkin(Path png) throws Exception {
      throw new UnsupportedOperationException("Not supported");
   }

   default String assetName(String id) {
      return null;
   }

   List<String> customCapes();

   String exportProfile(String var1) throws Exception;

   String importProfile(Path var1, boolean var2);

   List<Path> configFiles();

   void openConfigFolder(Consumer<Boolean> var1);

   String configFolderPath();

   void setClipboard(String var1);

   boolean pickConfigFile(Consumer<Path> var1);

   boolean webMode();

   void switchToWebGui();

   void openWebGui();

   String clipboard();
}
