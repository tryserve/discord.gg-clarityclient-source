package need.clarity.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
   @Accessor("destroyDelay")
   int getDestroyDelay();

   @Accessor("destroyDelay")
   void setDestroyDelay(int var1);

   @Accessor("destroyProgress")
   float getDestroyProgress();

   @Invoker("ensureHasSentCarriedItem")
   void clarity$ensureHasSentCarriedItem();
}
