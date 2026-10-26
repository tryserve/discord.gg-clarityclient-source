package need.clarity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class ConfigFiles {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final Path NO_DIALOG = Paths.get("__clarity_no_dialog__");

   private ConfigFiles() {
   }

   public static Path dir() {
      Path d = FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("configs");

      try {
         Files.createDirectories(d);
      } catch (Exception var2) {
      }

      return d;
   }

   public static String fileName(String profile) {
      String s = profile == null ? "" : profile.replaceAll("[^A-Za-z0-9_\\- ]", "_").trim();
      return (s.isEmpty() ? "profile" : s) + ".json";
   }

   public static Path export(String name, JsonObject snapshot) throws Exception {
      JsonObject out = new JsonObject();
      out.addProperty("name", name);
      out.add("data", snapshot.deepCopy());
      Path p = dir().resolve(fileName(name));
      Files.writeString(p, GSON.toJson(out), StandardCharsets.UTF_8);
      return p;
   }

   public static List<Path> list() {
      List<Path> out = new ArrayList<>();

      try (Stream<Path> s = Files.list(dir())) {
         s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")).forEach(out::add);
      } catch (Exception var6) {
      }

      out.sort((a, b) -> {
         try {
            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
         } catch (Exception e) {
            return 0;
         }
      });
      return out;
   }

   public static ConfigFiles.Imported read(Path p) {
      JsonElement el;
      try {
         el = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
      } catch (Exception e) {
         throw new IllegalArgumentException("Not a valid JSON file");
      }

      if (!el.isJsonObject()) {
         throw new IllegalArgumentException("Not a Clarity config");
      }

      JsonObject o = el.getAsJsonObject();
      String base = p.getFileName().toString().replaceAll("(?i)\\.json$", "");
      String name = o.has("name") && o.get("name").isJsonPrimitive() ? o.get("name").getAsString().trim() : base;
      if (name.isEmpty()) {
         name = base;
      }

      if (o.has("binds") && !o.has("data")) {
         throw new IllegalArgumentException("That's a bind set file, not a profile");
      }

      JsonObject snap = o.has("data") && o.get("data").isJsonObject() ? o.getAsJsonObject("data").deepCopy() : o.deepCopy();
      boolean anyModule = false;

      for (String k : snap.keySet()) {
         if (!k.startsWith("__") && snap.get(k).isJsonObject()) {
            anyModule = true;
            break;
         }
      }

      if (!anyModule) {
         throw new IllegalArgumentException("No module settings in that file");
      }

      LegacyConfigMigration.migrateChinaHat(snap, false);
      ConfigSchema.normalizeSnapshot(snap);
      return new ConfigFiles.Imported(name, snap);
   }

   public static String uniqueName(Collection<String> taken, String base) {
      if (!taken.contains(base)) {
         return base;
      }

      int i = 2;

      while (true) {
         String n = base + " (" + i + ")";
         if (!taken.contains(n)) {
            return n;
         }

         i++;
      }
   }

   public static void openFolder(Consumer<Boolean> done) {
      openFolder(dir(), done);
   }

   public static void reveal(Path folder) {
      openFolder(folder, ok -> {});
   }

   public static void openFolder(Path folder, Consumer<Boolean> done) {
      Path d = folder.toAbsolutePath().normalize();
      String path = d.toString();
      Thread t = new Thread(() -> {
         boolean ok = false;

         for (List<String> cmd : openers(path)) {
            if (tryRun(cmd)) {
               ok = true;
               break;
            }
         }

         boolean result = ok;
         Minecraft.getInstance().execute(() -> done.accept(result));
      }, "clarity-open-folder");
      t.setDaemon(true);
      t.start();
   }

   public static void openUrl(String url) {
      Thread t = new Thread(() -> {
         String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
         List<List<String>> l = new ArrayList<>();
         if (os.contains("win")) {
            l.add(List.of("rundll32", "url.dll,FileProtocolHandler", url));
         } else if (os.contains("mac")) {
            l.add(List.of("open", url));
         } else {
            if (Files.exists(Paths.get("/.flatpak-info"))) {
               l.add(List.of("flatpak-spawn", "--host", "xdg-open", url));
            }

            l.add(List.of("xdg-open", url));
            l.add(List.of("gio", "open", url));
            l.add(List.of("kde-open", url));
            l.add(List.of("sensible-browser", url));
            l.add(List.of("x-www-browser", url));
            l.add(List.of("firefox", url));
            l.add(List.of("chromium", url));
            l.add(List.of("google-chrome", url));
         }

         for (List<String> cmd : l) {
            if (tryRun(cmd)) {
               return;
            }
         }
      }, "clarity-open-url");
      t.setDaemon(true);
      t.start();
   }

   private static List<List<String>> openers(String path) {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      List<List<String>> l = new ArrayList<>();
      if (os.contains("win")) {
         l.add(List.of("explorer.exe", path));
         l.add(List.of("cmd", "/c", "start", "", path));
      } else if (os.contains("mac")) {
         l.add(List.of("open", path));
      } else {
         String uri = Paths.get(path).toUri().toString();
         if (Files.exists(Paths.get("/.flatpak-info"))) {
            l.add(List.of("flatpak-spawn", "--host", "xdg-open", path));
         }

         l.add(List.of("xdg-open", path));
         l.add(List.of("gio", "open", path));
         l.add(List.of("kde-open", path));
         l.add(List.of("kde-open5", path));
         l.add(List.of("exo-open", "--launch", "FileManager", path));
         l.add(List.of("nautilus", path));
         l.add(List.of("dolphin", path));
         l.add(List.of("thunar", path));
         l.add(List.of("nemo", path));
         l.add(List.of("pcmanfm", path));
         l.add(
            List.of(
               "dbus-send",
               "--session",
               "--print-reply",
               "--dest=org.freedesktop.FileManager1",
               "/org/freedesktop/FileManager1",
               "org.freedesktop.FileManager1.ShowFolders",
               "array:string:" + uri,
               "string:"
            )
         );
      }

      return l;
   }

   private static boolean tryRun(List<String> cmd) {
      try {
         Process p = new ProcessBuilder(cmd).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).redirectInput(Redirect.PIPE).start();

         try {
            p.getOutputStream().close();
         } catch (Exception var3) {
         }

         return !p.waitFor(4L, TimeUnit.SECONDS) ? true : p.exitValue() == 0 || cmd.get(0).equals("explorer.exe");
      } catch (Exception e) {
         return false;
      }
   }

   public static Path skinsDir() {
      Path d = FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("skins");

      try {
         Files.createDirectories(d);
      } catch (Exception var2) {
      }

      return d;
   }

   public static boolean pickPng(String title, Consumer<Path> onResult) {
      try {
         Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
      } catch (Throwable t) {
         return false;
      }

      Thread t = new Thread(() -> {
         long started = System.nanoTime();
         String picked = null;

         try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filter = stack.mallocPointer(1);
            filter.put(stack.UTF8("*.png")).flip();
            picked = TinyFileDialogs.tinyfd_openFileDialog(title, skinsDir().toAbsolutePath() + File.separator, filter, "PNG image (*.png)", false);
         } catch (Throwable var10) {
         }

         Path result = picked != null && !picked.isBlank() ? Paths.get(picked) : null;
         boolean noDialog = result == null && System.nanoTime() - started < 400000000L;
         Minecraft.getInstance().execute(() -> onResult.accept(noDialog ? NO_DIALOG : result));
      }, "clarity-file-picker");
      t.setDaemon(true);
      t.start();
      return true;
   }

   public static boolean pickFile(Consumer<Path> onResult) {
      try {
         Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
      } catch (Throwable t) {
         return false;
      }

      Thread t = new Thread(() -> {
         long started = System.nanoTime();
         String picked = null;

         try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filter = stack.mallocPointer(1);
            filter.put(stack.UTF8("*.json")).flip();
            picked = TinyFileDialogs.tinyfd_openFileDialog("Load a Clarity config", dir().toAbsolutePath() + File.separator, filter, "Clarity config (*.json)", false);
         } catch (Throwable var9) {
         }

         Path result = picked != null && !picked.isBlank() ? Paths.get(picked) : null;
         boolean noDialog = result == null && System.nanoTime() - started < 400000000L;
         Minecraft.getInstance().execute(() -> onResult.accept(noDialog ? NO_DIALOG : result));
      }, "clarity-file-picker");
      t.setDaemon(true);
      t.start();
      return true;
   }

   public record Imported(String name, JsonObject snapshot) {
   }
}
