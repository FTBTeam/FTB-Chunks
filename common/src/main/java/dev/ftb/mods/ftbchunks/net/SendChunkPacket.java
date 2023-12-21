package dev.ftb.mods.ftbchunks.net;

import com.google.common.primitives.Ints;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SendChunkPacket extends BaseS2CMessage {
	public final ResourceKey<Level> dimension;
	public final UUID teamId;
	public final SingleChunk chunk;

	public SendChunkPacket(ResourceKey<Level> dimension, UUID teamId, SingleChunk chunk) {
		this.dimension = dimension;
		this.teamId = teamId;
		this.chunk = chunk;
	}

	SendChunkPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
		teamId = buf.readUUID();
		chunk = new SingleChunk(buf, teamId);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_CHUNK;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeUUID(teamId);
		chunk.write(buf, teamId);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunksClient.INSTANCE.updateChunksFromServer(dimension, teamId, List.of(chunk));
	}

	public static class SingleChunk {
		// NOTE: relative times are sent in seconds rather than milliseconds
		//  (int rather than long for network sync efficiency)
		//  allows for offsets of up to ~2 billion seconds, which is 62 years - should be enough...
		//  (come back to me with a bug report in 2085 if not)
		private final int x, z;
		private final int relativeTimeClaimed;
		private final boolean forceLoaded;
		private final int relativeTimeForceLoaded;
		private final boolean expires;
		private final int relativeForceLoadExpiryTime;

		public SingleChunk(long now, int x, int z, @Nullable ClaimedChunk claimedChunk) {
			this.x = x;
			this.z = z;

			if (claimedChunk != null) {
				relativeTimeClaimed = millisToSeconds(now - claimedChunk.getTimeClaimed());
				forceLoaded = claimedChunk.isForceLoaded();
				expires = claimedChunk.getForceLoadExpiryTime() > 0L;
				relativeTimeForceLoaded = forceLoaded ? millisToSeconds(now - claimedChunk.getForceLoadedTime()) : 0;
				relativeForceLoadExpiryTime = expires ? millisToSeconds(claimedChunk.getForceLoadExpiryTime() - now) : 0;
			} else {
				relativeTimeClaimed = relativeTimeForceLoaded = relativeForceLoadExpiryTime = 0;
				forceLoaded = expires = false;
			}
		}

		private static int millisToSeconds(long ms) {
			return Ints.saturatedCast(ms / 1000L);
		}

		public int getX() {
			return x;
		}

		public int getZ() {
			return z;
		}

		public SingleChunk(FriendlyByteBuf buf, UUID teamId) {
			x = buf.readVarInt();
			z = buf.readVarInt();

			if (!teamId.equals(Util.NIL_UUID)) {
				relativeTimeClaimed = buf.readInt();
				byte b = buf.readByte();
				forceLoaded = (b & 0x01) != 0;
				expires = (b & 0x02) != 0;
				relativeTimeForceLoaded = forceLoaded ? buf.readInt() : 0;
				relativeForceLoadExpiryTime = expires ? buf.readInt() : 0;
			} else {
				relativeTimeClaimed = relativeTimeForceLoaded = relativeForceLoadExpiryTime = 0;
				forceLoaded = expires = false;
			}
		}

		public void write(FriendlyByteBuf buf, UUID teamId) {
			buf.writeVarInt(x);
			buf.writeVarInt(z);

			if (!teamId.equals(Util.NIL_UUID)) {
				byte b = 0x0;
				if (forceLoaded) b |= 0x1;
				if (expires) b |= 0x2;

				buf.writeInt(relativeTimeClaimed);
				buf.writeByte(b);
				if (forceLoaded) buf.writeInt(relativeTimeForceLoaded);
				if (expires) buf.writeInt(relativeForceLoadExpiryTime);
			}
		}

		public SingleChunk hidden() {
			return new SingleChunk(0L, x, z, null);
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