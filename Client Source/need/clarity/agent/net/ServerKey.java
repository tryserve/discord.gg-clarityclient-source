package need.clarity.agent.net;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public final class ServerKey {
   public static final String HANDSHAKE_HEX = "96de94e8da32afbc8d879671f1444edd9c3c98308739f901512bfdae94ba87da";
   public static final String RELEASE_HEX = "174ed3cf53647a9c7559824aaf7959f8a0a7b87ee41db0ec5510b928df790537";
   private static final byte[] ED25519_SPKI_PREFIX = hexBytes("302a300506032b6570032100");

   private ServerKey() {
   }

   public static PublicKey ed25519(String hex) {
      byte[] raw = hexBytes(hex);
      if (raw.length != 32) {
         throw new IllegalArgumentException("expected 32-byte Ed25519 key, got " + raw.length);
      }

      byte[] der = new byte[ED25519_SPKI_PREFIX.length + 32];
      System.arraycopy(ED25519_SPKI_PREFIX, 0, der, 0, ED25519_SPKI_PREFIX.length);
      System.arraycopy(raw, 0, der, ED25519_SPKI_PREFIX.length, 32);

      try {
         return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
      } catch (Exception e) {
         throw new RuntimeException("Failed to decode Ed25519 key", e);
      }
   }

   public static byte[] hexBytes(String hex) {
      int len = hex.length();
      if ((len & 1) != 0) {
         throw new IllegalArgumentException("odd hex length");
      }

      byte[] out = new byte[len / 2];

      for (int i = 0; i < out.length; i++) {
         int hi = Character.digit(hex.charAt(i * 2), 16);
         int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
         if (hi < 0 || lo < 0) {
            throw new IllegalArgumentException("non-hex char");
         }

         out[i] = (byte)(hi << 4 | lo);
      }

      return out;
   }

   public static String hexString(byte[] b) {
      StringBuilder sb = new StringBuilder(b.length * 2);

      for (byte v : b) {
         sb.append(String.format("%02x", v & 255));
      }

      return sb.toString();
   }
}
