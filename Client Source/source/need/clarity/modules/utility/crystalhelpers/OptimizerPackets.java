package need.clarity.modules.utility.crystalhelpers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class OptimizerPackets {
   private static final String NAMESPACE = "marlowcrystal";

   private OptimizerPackets() {
   }

   static Identifier mkId(String path) {
      return Identifier.fromNamespaceAndPath("marlowcrystal", path);
   }

   public record Challenge(int id) implements CustomPacketPayload {
      public static final Type<OptimizerPackets.@NotNull Challenge> TYPE = new Type(OptimizerPackets.mkId("challenge"));
      public static final StreamCodec<@NotNull FriendlyByteBuf, OptimizerPackets.@NotNull Challenge> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, OptimizerPackets.Challenge>() {
         public void encode(FriendlyByteBuf buf, OptimizerPackets.Challenge v) {
            buf.writeInt(v.id());
         }

         @NotNull
         public OptimizerPackets.Challenge decode(FriendlyByteBuf buf) {
            return new OptimizerPackets.Challenge(buf.readInt());
         }
      };

      @NotNull
      public Type<OptimizerPackets.Challenge> type() {
         return TYPE;
      }
   }

   public record ChallengeResponse(int id) implements CustomPacketPayload {
      public static final Type<OptimizerPackets.@NotNull ChallengeResponse> TYPE = new Type(OptimizerPackets.mkId("challenge_response"));
      public static final StreamCodec<@NotNull FriendlyByteBuf, OptimizerPackets.@NotNull ChallengeResponse> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, OptimizerPackets.ChallengeResponse>() {
         public void encode(FriendlyByteBuf buf, OptimizerPackets.ChallengeResponse v) {
            buf.writeInt(v.id());
         }

         @NotNull
         public OptimizerPackets.ChallengeResponse decode(FriendlyByteBuf buf) {
            return new OptimizerPackets.ChallengeResponse(buf.readInt());
         }
      };

      @NotNull
      public Type<OptimizerPackets.ChallengeResponse> type() {
         return TYPE;
      }
   }

   public static final class OptOut implements CustomPacketPayload {
      public static final Type<OptimizerPackets.@NotNull OptOut> TYPE = new Type(OptimizerPackets.mkId("opt_out"));
      public static final OptimizerPackets.OptOut INSTANCE = new OptimizerPackets.OptOut();
      public static final StreamCodec<@NotNull FriendlyByteBuf, OptimizerPackets.@NotNull OptOut> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, OptimizerPackets.OptOut>() {
         public void encode(FriendlyByteBuf buf, OptimizerPackets.OptOut v) {
         }

         @NotNull
         public OptimizerPackets.OptOut decode(FriendlyByteBuf buf) {
            return OptimizerPackets.OptOut.INSTANCE;
         }
      };

      private OptOut() {
      }

      @NotNull
      public Type<OptimizerPackets.OptOut> type() {
         return TYPE;
      }
   }

   public static final class OptOutAck implements CustomPacketPayload {
      public static final Type<OptimizerPackets.@NotNull OptOutAck> TYPE = new Type(OptimizerPackets.mkId("opt_out_ack"));
      public static final OptimizerPackets.OptOutAck INSTANCE = new OptimizerPackets.OptOutAck();
      public static final StreamCodec<@NotNull FriendlyByteBuf, OptimizerPackets.@NotNull OptOutAck> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, OptimizerPackets.OptOutAck>() {
         public void encode(FriendlyByteBuf buf, OptimizerPackets.OptOutAck v) {
         }

         @NotNull
         public OptimizerPackets.OptOutAck decode(FriendlyByteBuf buf) {
            return OptimizerPackets.OptOutAck.INSTANCE;
         }
      };

      private OptOutAck() {
      }

      @NotNull
      public Type<OptimizerPackets.OptOutAck> type() {
         return TYPE;
      }
   }

   public record Version(int major, int minor, int patch, boolean snapshot) implements CustomPacketPayload {
      public static final Type<OptimizerPackets.Version> TYPE = new Type(OptimizerPackets.mkId("version"));
      public static final StreamCodec<FriendlyByteBuf, OptimizerPackets.Version> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, OptimizerPackets.Version>() {
         public void encode(FriendlyByteBuf buf, OptimizerPackets.Version v) {
            buf.writeVarInt(v.major());
            buf.writeVarInt(v.minor());
            buf.writeVarInt(v.patch());
            buf.writeBoolean(v.snapshot());
         }

         public OptimizerPackets.Version decode(FriendlyByteBuf buf) {
            return new OptimizerPackets.Version(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
         }
      };

      @NotNull
      public Type<OptimizerPackets.Version> type() {
         return TYPE;
      }
   }
}
