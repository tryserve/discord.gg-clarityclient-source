package need.clarity.mixin;

import need.clarity.modules.utility.NameProtect;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public abstract class NameProtectTextMixin {
   @ModifyVariable(
      method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private static String cl$nameProtectText(String text) {
      try {
         return NameProtect.maybeReplace(text);
      } catch (Throwable t) {
         return text;
      }
   }
}
