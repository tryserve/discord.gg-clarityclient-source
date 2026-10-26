package need.clarity.hotbar;

public final class HotbarState {
   public boolean hotbar = true;
   public boolean status = true;
   public boolean experience = true;
   public int selected;
   public final HotbarState.Slot[] slots = new HotbarState.Slot[9];
   public final HotbarState.Slot offhand = new HotbarState.Slot();
   public boolean offhandLeft = true;
   public boolean survival = true;
   public boolean hasXp = true;
   public float health = 20.0F;
   public float maxHealth = 20.0F;
   public float absorption;
   public boolean poison;
   public boolean wither;
   public boolean frozen;
   public boolean regen;
   public boolean hardcore;
   public int armor;
   public int food = 20;
   public float saturation = 5.0F;
   public boolean hungerEffect;
   public int air = 300;
   public int maxAir = 300;
   public boolean airShown;
   public float xp;
   public int level;
   public boolean mount;
   public float mountHealth;
   public float mountMax;
   public boolean jump;
   public float jumpScale;

   public HotbarState() {
      for (int i = 0; i < 9; i++) {
         this.slots[i] = new HotbarState.Slot();
      }
   }

   public void clearSlots() {
      for (HotbarState.Slot s : this.slots) {
         s.clear();
      }

      this.offhand.clear();
   }

   public int signature() {
      int h = 17;
      h = h * 31
         + (this.hotbar ? 1 : 0)
         + (this.status ? 2 : 0)
         + (this.experience ? 4 : 0)
         + (this.survival ? 8 : 0)
         + (this.hasXp ? 16 : 0)
         + (this.offhandLeft ? 32 : 0)
         + (this.poison ? 64 : 0)
         + (this.wither ? 128 : 0)
         + (this.frozen ? 256 : 0)
         + (this.regen ? 512 : 0)
         + (this.hardcore ? 1024 : 0)
         + (this.hungerEffect ? 2048 : 0)
         + (this.airShown ? 4096 : 0)
         + (this.mount ? 8192 : 0)
         + (this.jump ? 16384 : 0);
      h = h * 31 + this.selected;

      for (HotbarState.Slot s : this.slots) {
         h = h * 31 + s.hash();
      }

      h = h * 31 + this.offhand.hash();
      h = h * 31 + Math.round(this.health * 20.0F);
      h = h * 31 + Math.round(this.maxHealth * 4.0F);
      h = h * 31 + Math.round(this.absorption * 20.0F);
      h = h * 31 + this.armor;
      h = h * 31 + this.food;
      h = h * 31 + Math.round(this.saturation * 4.0F);
      h = h * 31 + this.air / 3;
      h = h * 31 + this.maxAir;
      h = h * 31 + Math.round(this.xp * 512.0F);
      h = h * 31 + this.level;
      h = h * 31 + Math.round(this.mountHealth * 4.0F) + Math.round(this.mountMax * 64.0F);
      return h * 31 + Math.round(this.jumpScale * 256.0F);
   }

   public static final class Slot {
      public boolean present;
      public int count;
      public float bar = -1.0F;
      public int barColor = -11141291;
      public float cooldown;

      void clear() {
         this.present = false;
         this.count = 0;
         this.bar = -1.0F;
         this.cooldown = 0.0F;
      }

      int hash() {
         return !this.present ? 0 : this.count * 31 + (this.bar < 0.0F ? -7 : Math.round(this.bar * 13.0F)) * 131 + this.barColor * 7 + Math.round(this.cooldown * 96.0F) * 1031;
      }
   }
}
