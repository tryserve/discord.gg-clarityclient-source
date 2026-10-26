package need.clarity.modules;

import need.clarity.Unload;
import need.clarity.config.ClarityConfig;
import need.clarity.modules.visual.Notifications;

public abstract class ModuleBase {
   private final String name;
   private final String category;
   private boolean enabled;

   protected ModuleBase(String name, String category) {
      this.name = name;
      this.category = category;
      this.enabled = ClarityConfig.isEnabled(name);
      ModuleRegistry.register(this);
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onTick() {
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         if (!enabled || !Unload.isUnloaded()) {
            this.enabled = enabled;
            ClarityConfig.setModuleEnabled(this.name, enabled);
            if (enabled) {
               this.onEnable();
            } else {
               this.onDisable();
            }

            if (!Unload.isUnloaded()) {
               try {
                  Notifications.moduleToggled(this.name, enabled);
               } catch (Throwable var3) {
               }
            }
         }
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public String getName() {
      return this.name;
   }

   public String getCategory() {
      return this.category;
   }

   protected <T> T getSetting(String key, T defaultValue) {
      Object val = ClarityConfig.getSetting(this.name, key);
      if (val != null && defaultValue != null) {
         try {
            if (val instanceof String s && !(defaultValue instanceof String)) {
               String t = s.trim();
               if (defaultValue instanceof Boolean) {
                  if (!t.equalsIgnoreCase("true") && !t.equalsIgnoreCase("false")) {
                     return defaultValue;
                  }

                  return (T)Boolean.valueOf(t);
               }

               if (defaultValue instanceof Number) {
                  val = Double.parseDouble(t);
               }
            }

            if (val instanceof Boolean b && defaultValue instanceof Number) {
               val = b ? 1.0 : 0.0;
            }

            if (val instanceof Number n) {
               if (defaultValue instanceof Integer) {
                  return (T)Integer.valueOf(n.intValue());
               }

               if (defaultValue instanceof Long) {
                  return (T)Long.valueOf(n.longValue());
               }

               if (defaultValue instanceof Float) {
                  return (T)Float.valueOf(n.floatValue());
               }

               if (defaultValue instanceof Double) {
                  return (T)Double.valueOf(n.doubleValue());
               }

               if (defaultValue instanceof Boolean) {
                  return (T)Boolean.valueOf(n.doubleValue() != 0.0);
               }
            }

            if (defaultValue instanceof String && !(val instanceof String)) {
               return (T)String.valueOf(val);
            } else {
               return (T)(defaultValue.getClass().isInstance(val) ? val : defaultValue);
            }
         } catch (RuntimeException e) {
            return defaultValue;
         }
      } else {
         return (T)(val == null ? defaultValue : val);
      }
   }

   protected void putSetting(String key, Object value) {
      ClarityConfig.putSetting(this.name, key, value);
   }
}
