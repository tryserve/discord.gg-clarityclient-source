package need.clarity.clickgui;

public final class TextField {
   public String text = "";
   public int caret = 0;
   public boolean allSelected = false;
   public final int maxLen;

   public TextField(int maxLen) {
      this.maxLen = maxLen;
   }

   public void set(String s) {
      this.text = s == null ? "" : s;
      this.caret = this.text.length();
      this.allSelected = false;
   }

   public void type(String s) {
      if (s != null && !s.isEmpty()) {
         if (this.allSelected) {
            this.text = "";
            this.caret = 0;
            this.allSelected = false;
         }

         StringBuilder b = new StringBuilder();

         for (char c : s.toCharArray()) {
            if (c >= ' ' && c != 127) {
               b.append(c);
            }
         }

         String ins = b.toString();
         if (this.text.length() + ins.length() > this.maxLen) {
            ins = ins.substring(0, Math.max(0, this.maxLen - this.text.length()));
         }

         this.text = this.text.substring(0, this.caret) + ins + this.text.substring(this.caret);
         this.caret = this.caret + ins.length();
      }
   }

   public boolean key(int key, boolean ctrl, String clipboard) {
      switch (key) {
         case 65:
            if (ctrl) {
               this.allSelected = true;
               this.caret = this.text.length();
               return true;
            }

            return false;
         case 86:
            if (ctrl && clipboard != null) {
               this.type(clipboard.replace("\n", " "));
               return true;
            }

            return false;
         case 259:
            if (this.allSelected) {
               this.text = "";
               this.caret = 0;
               this.allSelected = false;
               return true;
            } else if (this.caret == 0) {
               return true;
            } else {
               int from = this.caret - 1;
               if (ctrl) {
                  from = this.caret;

                  while (from > 0 && this.text.charAt(from - 1) == ' ') {
                     from--;
                  }

                  while (from > 0 && this.text.charAt(from - 1) != ' ') {
                     from--;
                  }
               }

               this.text = this.text.substring(0, from) + this.text.substring(this.caret);
               this.caret = from;
               return true;
            }
         case 261:
            if (this.allSelected) {
               this.text = "";
               this.caret = 0;
               this.allSelected = false;
               return true;
            }

            if (this.caret < this.text.length()) {
               this.text = this.text.substring(0, this.caret) + this.text.substring(this.caret + 1);
            }

            return true;
         case 262:
            this.caret = Math.min(this.text.length(), this.caret + 1);
            this.allSelected = false;
            return true;
         case 263:
            this.caret = Math.max(0, this.caret - 1);
            this.allSelected = false;
            return true;
         case 268:
            this.caret = 0;
            this.allSelected = false;
            return true;
         case 269:
            this.caret = this.text.length();
            this.allSelected = false;
            return true;
         default:
            return false;
      }
   }
}
