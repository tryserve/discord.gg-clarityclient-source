package need.clarity.alts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class AltStore {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final List<AltStore.Alt> ALTS = new ArrayList<>();
   private static boolean loaded = false;
   private static volatile int version = 0;
   private static List<AltStore.Alt> snapshot = List.of();

   private AltStore() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getGameDir().resolve("clarity").resolve("alts.json");
   }

   public static synchronized List<AltStore.Alt> all() {
      load();
      return snapshot;
   }

   public static int version() {
      return version;
   }

   private static void changed() {
      snapshot = List.copyOf(ALTS);
      version++;
   }

   public static synchronized void put(AltStore.Alt alt) {
      load();
      ALTS.removeIf(a -> a.uuid.equalsIgnoreCase(alt.uuid));
      ALTS.add(0, alt);
      save();
   }

   public static synchronized void remove(String uuid) {
      load();
      ALTS.removeIf(a -> a.uuid.equalsIgnoreCase(uuid));
      save();
   }

   public static synchronized void touch(AltStore.Alt alt) {
      alt.lastUsed = System.currentTimeMillis();
      save();
   }

   private static void load() {
      if (!loaded) {
         loaded = true;

         try {
            Path f = file();
            if (!Files.exists(f)) {
               return;
            }

            List<AltStore.Alt> list = (List<AltStore.Alt>)GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), (new TypeToken<List<AltStore.Alt>>() {}).getType());
            if (list != null) {
               for (AltStore.Alt a : list) {
                  if (a != null && a.uuid != null && !a.uuid.isEmpty()) {
                     ALTS.add(a);
                  }
               }
            }
         } catch (Throwable var4) {
         }

         changed();
      }
   }

   public static synchronized void save() {
      changed();

      try {
         Path f = file();
         Files.createDirectories(f.getParent());
         Path tmp = f.resolveSibling("alts.json.tmp");
         Files.writeString(tmp, GSON.toJson(ALTS), StandardCharsets.UTF_8);
         restrict(tmp);
         Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (Throwable var2) {
      }
   }

   private static void restrict(Path p) {
      try {
         Files.setPosixFilePermissions(p, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
      } catch (Throwable var2) {
      }
   }

   public static final class Alt {
      public String name = "";
      public String uuid = "";
      public String refreshToken = "";
      public String clientId = "";
      public String mcToken = "";
      public long mcTokenExpiry = 0L;
      public long lastUsed = 0L;

      public boolean hasFreshToken() {
         return !this.mcToken.isEmpty() && System.currentTimeMillis() < this.mcTokenExpiry - 300000L;
      }
   }
}
