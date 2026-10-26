package need.clarity.modules.visual.playermodel;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PetModels {
   public static final String DEFAULT = "Mini Kenny";
   private static final Map<String, PetModel> MODELS = new LinkedHashMap<>();

   private PetModels() {
   }

   public static PetModel get(String name) {
      PetModel m = name == null ? null : MODELS.get(name);
      return m != null ? m : MODELS.get("Mini Kenny");
   }

   public static Iterable<String> names() {
      return MODELS.keySet();
   }

   static {
      MODELS.put("Mini Kenny", PetModel.of(RigModel.KENNY));
      MODELS.put("Mini Jenny", PetModel.of(RigModel.JENNY));
      MODELS.put("Mini Knight", new PetDesigns.Knight());
      MODELS.put("Mini Wizard", new PetDesigns.Wizard());
      MODELS.put("Mini Astronaut", new PetDesigns.Astronaut());
      MODELS.put("Mini Ninja", new PetDesigns.Ninja());
      MODELS.put("Dog", new PetDesigns.Dog());
      MODELS.put("Cat", new PetDesigns.Cat());
      MODELS.put("Bunny", new PetDesigns.Bunny());
      MODELS.put("Penguin", new PetDesigns.Penguin());
   }
}
