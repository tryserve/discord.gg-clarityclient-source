package need.clarity.modules.utility.crystalhelpers;

import java.util.LinkedList;
import org.jetbrains.annotations.Nullable;

public final class OptOutCache {
   private static final int MAX_SERVERS = 10;
   private final OptOutCache.EvictingList<String> optedOutServers = new OptOutCache.EvictingList<>(10);
   private final OptOutCache.EvictingList<String> notifiedServers = new OptOutCache.EvictingList<>(10);
   private volatile boolean optedOut;

   public boolean isOptedOut() {
      return this.optedOut;
   }

   public void setOptedOut(boolean optedOut) {
      this.optedOut = optedOut;
   }

   public void markOptedOut(@Nullable String serverKey) {
      if (serverKey != null) {
         synchronized (this.optedOutServers) {
            if (!this.optedOutServers.contains(serverKey)) {
               this.optedOutServers.add(serverKey);
            }
         }

         this.optedOut = true;
      }
   }

   public boolean isServerOptedOut(@Nullable String serverKey) {
      if (serverKey == null) {
         return false;
      }

      synchronized (this.optedOutServers) {
         return this.optedOutServers.contains(serverKey);
      }
   }

   public boolean hasNotified(@Nullable String serverKey) {
      if (serverKey == null) {
         return false;
      }

      synchronized (this.notifiedServers) {
         return this.notifiedServers.contains(serverKey);
      }
   }

   public void markNotified(@Nullable String serverKey) {
      if (serverKey != null) {
         synchronized (this.notifiedServers) {
            if (!this.notifiedServers.contains(serverKey)) {
               this.notifiedServers.add(serverKey);
            }
         }
      }
   }

   public void clearCurrentSession() {
      this.optedOut = false;
   }

   private static final class EvictingList<T> extends LinkedList<T> {
      private final int maxSize;

      EvictingList(int maxSize) {
         this.maxSize = maxSize;
      }

      @Override
      public boolean add(T t) {
         if (this.size() >= this.maxSize && this.maxSize > 0) {
            this.removeFirst();
         }

         return super.add(t);
      }
   }
}
