package need.clarity.modules.visual.cosmetics.hats;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import need.clarity.modules.visual.cosmetics.Cosmetic;

public enum HatStyle {
   CAT_EARS("Ears", "Cat Ears", CatEars::new),
   FOX_EARS("Ears", "Fox Ears", FoxEars::new),
   BUNNY_EARS("Ears", "Bunny Ears", BunnyEars::new),
   BEAR_EARS("Ears", "Bear Ears", BearEars::new),
   DEMON_HORNS("Horns", "Demon Horns", DemonHorns::new),
   STAG_ANTLERS("Horns", "Stag Antlers", StagAntlers::new),
   REINDEER_ANTLERS("Horns", "Reindeer Antlers", ReindeerAntlers::new),
   UNICORN_HORN("Horns", "Unicorn Horn", UnicornHorn::new),
   VIKING_HORNS("Horns", "Viking Horns", VikingHorns::new),
   CLASSIC_RING("Halos", "Classic Ring", ClassicRing::new),
   DOUBLE_RING("Halos", "Double Ring", DoubleRing::new),
   THORN_HALO("Halos", "Thorn Halo", ThornHalo::new),
   ORBITING_ORBS("Halos", "Orbiting Orbs", OrbitingOrbs::new),
   RUNIC_HALO("Halos", "Runic Halo", RunicHalo::new),
   ROYAL_CROWN("Crowns", "Royal Crown", RoyalCrown::new),
   SPIKED_CROWN("Crowns", "Spiked Crown", SpikedCrown::new),
   LAUREL_WREATH("Crowns", "Laurel Wreath", LaurelWreath::new),
   TIARA("Crowns", "Tiara", Tiara::new),
   FLAME_CROWN("Crowns", "Flame Crown", FlameCrown::new),
   ICE_CROWN("Crowns", "Ice Crown", IceCrown::new),
   FLOATING_CROWN("Crowns", "Floating Crown", FloatingCrown::new),
   CHINA_HAT("Misc", "China Hat", ChinaHatCosmetic::new);

   public static final List<String> CATEGORIES = List.of("Ears", "Horns", "Halos", "Crowns", "Misc");
   public final String category;
   public final String label;
   private final Function<HatParams, Cosmetic> factory;

   HatStyle(String category, String label, Function<HatParams, Cosmetic> factory) {
      this.category = category;
      this.label = label;
      this.factory = factory;
   }

   public Cosmetic create(HatParams p) {
      return this.factory.apply(p);
   }

   public static List<HatStyle> inCategory(String category) {
      List<HatStyle> out = new ArrayList<>();

      for (HatStyle s : values()) {
         if (s.category.equals(category)) {
            out.add(s);
         }
      }

      return out;
   }

   public static HatStyle fromName(String name) {
      for (HatStyle s : values()) {
         if (s.label.equalsIgnoreCase(name)) {
            return s;
         }
      }

      return CHINA_HAT;
   }
}
