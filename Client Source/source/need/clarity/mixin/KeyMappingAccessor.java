package need.clarity.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
   @Accessor("clickCount")
   int getClickCount();

   @Accessor("clickCount")
   void setClickCount(int var1);

   @Accessor("key")
   Key getBoundKey();
}
