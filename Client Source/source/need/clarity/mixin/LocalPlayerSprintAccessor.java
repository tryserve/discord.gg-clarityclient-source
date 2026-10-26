package need.clarity.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerSprintAccessor {
   @Accessor("wasSprinting")
   boolean getWasSprinting();

   @Accessor("wasSprinting")
   void setWasSprinting(boolean var1);
}
