package dev.ftb.mods.ftbchunks.net;

import com.google.common.primitives.Ints;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public record SendChunkPacket(ResourceKey<Level> dimension, UUID teamId, SingleChunk chunk) implements CustomPacketPayload {
	public static final Type<SendChunkPacket> TYPE = new Type<>(FTBChunksAPI.rl("send_chunk_packet"));

	public static final StreamCodec<FriendlyByteBuf, SendChunkPacket> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), SendChunkPacket::dimension,
			UUIDUtil.STREAM_CODEC, SendChunkPacket::teamId,
			SingleChunk.STREAM_CODEC, SendChunkPacket::chunk,
			SendChunkPacket::new
	);

	@Override
	public Type<SendChunkPacket> type() {
		return TYPE;
	}

	public static void handle(SendChunkPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.updateChunksFromServer(message.dimension, message.teamId, List.of(message.chunk)));
	}

	public record SingleChunk(int x, int z, boolean claimed, int relativeTimeClaimed, boolean forceLoaded,
							  int relativeTimeForceLoaded, boolean expires, int relativeForceLoadExpiryTime) {
		// NOTE: relative times are sent in seconds rather than milliseconds
		//  (int rather than long for network sync efficiency)
		//  allows for offsets of up to ~2 billion seconds, which is 62 years - should be enough...
		//  (come back to me with a bug report in 2085 if not)

		public static StreamCodec<FriendlyByteBuf, SingleChunk> STREAM_CODEC = new StreamCodec<>() {
			@Override
			public SingleChunk decode(FriendlyByteBuf buf) {
				int x = buf.readVarInt();
				int z = buf.readVarInt();

				if (buf.readBoolean()) {
					int relativeTimeClaimed = buf.readInt();
					byte b = buf.readByte();
					boolean forceLoaded = (b & 0x01) != 0;
					boolean expires = (b & 0x02) != 0;
					int relativeTimeForceLoaded = forceLoaded ? buf.readInt() : 0;
					int relativeForceLoadExpiryTime = expires ? buf.readInt() : 0;
					return new SingleChunk(x, z, true, relativeTimeClaimed, forceLoaded, relativeTimeForceLoaded, expires, relativeForceLoadExpiryTime);
				} else {
					return new SingleChunk(x, z, false, 0, false, 0, false, 0);
				}
			}

			@Override
			public void encode(FriendlyByteBuf buf, SingleChunk singleChunk) {
				buf.writeVarInt(singleChunk.x);
				buf.writeVarInt(singleChunk.z);

				buf.writeBoolean(singleChunk.claimed);
				if (singleChunk.claimed) {
					byte b = 0x0;
					if (singleChunk.forceLoaded) b |= 0x1;
					if (singleChunk.expires) b |= 0x2;

					buf.writeInt(singleChunk.relativeTimeClaimed);
					buf.writeByte(b);
					if (singleChunk.forceLoaded) buf.writeInt(singleChunk.relativeTimeForceLoaded);
					if (singleChunk.expires) buf.writeInt(singleChunk.relativeForceLoadExpiryTime);
				}
			}
		};

		public static SingleChunk create(long now, int x, int z, @Nullable ClaimedChunk claimedChunk) {
			if (claimedChunk != null) {
				boolean forceLoaded = claimedChunk.isForceLoaded();
				boolean expires = claimedChunk.getForceLoadExpiryTime() > 0L;
				return new SingleChunk(x, z, true,
						millisToSeconds(now - claimedChunk.getTimeClaimed()),
						forceLoaded,
						forceLoaded ? millisToSeconds(now - claimedChunk.getForceLoadedTime()) : 0,
						expires,
						expires ? millisToSeconds(claimedChunk.getForceLoadExpiryTime() - now) : 0);
			} else {
				return new SingleChunk(x, z, false, 0, false, 0, false, 0);
			}
		}

		public SingleChunk hidden() {
			return SingleChunk.create(0L, x, z, null);
		}

		private static int millisToSeconds(long ms) {
			return Ints.saturatedCast(ms / 1000L);
		}

		public MapChunk.DateInfo getDateInfo(boolean isClaimed, long now) {
			if (!isClaimed) {
				return MapChunk.NO_DATE_INFO;
			}

			Date claimed = new Date(now - relativeTimeClaimed * 1000L);
			Date forceLoaded = this.forceLoaded ? new Date(now - relativeTimeForceLoaded * 1000L) : null;
			Date expiry = this.forceLoaded && expires ? new Date(now + relativeForceLoadExpiryTime * 1000L) : null;
			return new MapChunk.DateInfo(claimed, forceLoaded, expiry);
		}
	}
}