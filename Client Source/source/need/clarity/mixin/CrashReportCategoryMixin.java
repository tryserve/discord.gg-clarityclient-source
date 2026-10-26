package need.clarity.mixin;

import net.minecraft.CrashReportCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrashReportCategory.class)
public class CrashReportCategoryMixin {
   @Redirect(method = "validateStackTrace", at = @At(value = "INVOKE", target = "Ljava/lang/StackTraceElement;getFileName()Ljava/lang/String;"))
   private String clarity$safeFileName(StackTraceElement element) {
      String name = element.getFileName();
      return name == null ? "" : name;
   }
}
