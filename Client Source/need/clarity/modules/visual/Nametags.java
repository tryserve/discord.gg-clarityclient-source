package need.clarity.modules.visual;

import need.clarity.modules.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class Nametags extends ModuleBase {
   public static Nametags INSTANCE;

   public Nametags() {
      super("Nametags", "Visual");
      INSTANCE = this;
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static boolean hidesVanilla(Object entity) {
      Nametags n = INSTANCE;
      return n != null && n.isEnabled() && n.hideVanilla() ? entity instanceof Entity e && n.shows(e) : false;
   }

   public boolean shows(Entity e) {
      Minecraft mc = Minecraft.getInstance();
      if (!(e instanceof LivingEntity le && le.isAlive())) {
         return false;
      } else if (e == mc.player) {
         return this.showSelf() && !mc.options.getCameraType().isFirstPerson();
      } else if (e instanceof Player) {
         return !e.isInvisible() || this.showInvisible();
      } else if (!this.showMobs()) {
         return false;
      } else {
         return e.isInvisible() && !this.showInvisible() ? false : e.hasCustomName() || !this.onlyNamedMobs();
      }
   }

   public String healthMode() {
      return this.getSetting("Health", "Bar + Number");
   }

   public String healthFormat() {
      return this.getSetting("Health Format", "HP");
   }

   public boolean absorption() {
      return this.getSetting("Absorption", true);
   }

   public boolean damageTrail() {
      return this.getSetting("Damage Trail", true);
   }

   public boolean armor() {
      return this.getSetting("Armor", true);
   }

   public boolean heldItems() {
      return this.getSetting("Held Items", true);
   }

   public String durability() {
      return this.getSetting("Durability", "Percent");
   }

   public boolean enchants() {
      return this.getSetting("Enchants", false);
   }

   public int maxEnchants() {
      return (int)clamp(this.getSetting("Max Enchants", 4.0F), 1.0F, 12.0F);
   }

   public boolean distance() {
      return this.getSetting("Distance", true);
   }

   public boolean ping() {
      return this.getSetting("Ping", true);
   }

   public boolean friendMark() {
      return this.getSetting("Friend Marker", true);
   }

   public boolean gamemode() {
      return this.getSetting("Gamemode", false);
   }

   public String style() {
      return this.getSetting("Style", "Modern");
   }

   public String font() {
      return this.getSetting("Font", "Inter");
   }

   public String nameColor() {
      return this.getSetting("Name Color", "Team");
   }

   public float scale() {
      return clamp(this.getSetting("Scale", 1.0F), 0.4F, 3.0F);
   }

   public String sizeMode() {
      return this.getSetting("Size Mode", "Constant");
   }

   public float minScale() {
      return clamp(this.getSetting("Min Scale", 0.6F), 0.2F, 1.0F);
   }

   public float bgOpacity() {
      return clamp(this.getSetting("Background Opacity", 0.78F), 0.0F, 1.0F);
   }

   public float radius() {
      return clamp(this.getSetting("Corner Radius", 5.0F), 0.0F, 12.0F);
   }

   public boolean border() {
      return this.getSetting("Border", true);
   }

   public boolean accentLine() {
      return this.getSetting("Accent Line", true);
   }

   public boolean textShadow() {
      return this.getSetting("Text Shadow", false);
   }

   public boolean themeColors() {
      return this.getSetting("Theme Colors", true);
   }

   public Object bgColor() {
      return this.getSetting("Background Color", "#11141C");
   }

   public Object textColor() {
      return this.getSetting("Text Color", "#EDEDF0");
   }

   public String barColor() {
      return this.getSetting("Health Colors", "Gradient");
   }

   public float yOffset() {
      return clamp(this.getSetting("Y Offset", 0.35F), -1.0F, 3.0F);
   }

   public float maxDistance() {
      return clamp(this.getSetting("Max Distance", 96.0F), 4.0F, 512.0F);
   }

   public boolean throughWalls() {
      return this.getSetting("Through Walls", true);
   }

   public boolean showSelf() {
      return this.getSetting("Show Self", false);
   }

   public boolean showInvisible() {
      return this.getSetting("Show Invisible", false);
   }

   public boolean showMobs() {
      return this.getSetting("Show Mobs", false);
   }

   public boolean onlyNamedMobs() {
      return this.getSetting("Only Named Mobs", true);
   }

   public boolean hideVanilla() {
      return this.getSetting("Hide Vanilla", true);
   }

   public float smoothness() {
      return clamp(this.getSetting("Smoothness", 0.6F), 0.0F, 1.0F);
   }

   public boolean fadeDistance() {
      return this.getSetting("Fade Far", true);
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
