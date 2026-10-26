package need.clarity.modules.visual;

import java.util.Comparator;
import java.util.List;
import need.clarity.modules.ModuleBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class CustomScoreboard extends ModuleBase {
   public static CustomScoreboard INSTANCE;
   private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparing(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
   private static volatile CustomScoreboard.Snapshot snapshot;

   public CustomScoreboard() {
      super("Custom Scoreboard", "HUD");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static boolean bool(String k, boolean d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static float num(String k, float d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static String str(String k, String d) {
      return INSTANCE == null ? d : INSTANCE.getSetting(k, d);
   }

   public static CustomScoreboard.Snapshot current() {
      CustomScoreboard.Snapshot s = snapshot;
      return s != null && active() && System.currentTimeMillis() - s.at() <= 300L ? s : null;
   }

   public static boolean render(Object graphics, Objective objective) {
      if (active() && objective != null) {
         Scoreboard sb = objective.getScoreboard();
         NumberFormat fmt = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
         List<CustomScoreboard.Row> rows = new java.util.ArrayList<>();
         sb.listPlayerScores(objective).stream().filter(e -> !e.isHidden()).sorted(ORDER).limit(15L).forEach(e -> {
            PlayerTeam team = sb.getPlayersTeam(e.owner());
            rows.add(new CustomScoreboard.Row(PlayerTeam.formatNameForTeam(team, e.ownerName()), e.formatValue(fmt)));
         });
         snapshot = new CustomScoreboard.Snapshot(objective.getDisplayName(), rows, System.currentTimeMillis());
         return true;
      } else {
         return false;
      }
   }

   public static int signature() {
      CustomScoreboard.Snapshot s = current();
      if (s == null) {
         return 0;
      }

      int h = s.title().getString().hashCode();

      for (CustomScoreboard.Row r : s.rows()) {
         h = h * 31 + r.name().getString().hashCode() * 7 + r.score().getString().hashCode();
      }

      return h;
   }

   public record Row(Component name, Component score) {
   }

   public record Snapshot(Component title, List<CustomScoreboard.Row> rows, long at) {
   }
}
